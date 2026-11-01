import java.util.*;

/**
 * Weighted graph representing Sudar town's road network.
 * Stores intersections and facilities as nodes, and roads with travel times
 * (in minutes) as edges.
 *
 * Implements Dijkstra's Algorithm with a PriorityQueue (Min-Heap)
 * to compute the fastest response route between any station/unit and incident site.
 */
public class RoadNetwork {

    public static final String[] TOWN_LOCATIONS = {
        "Station-A", "Station-B", "Junction-1", "Junction-2", "Market",
        "Hospital", "Industrial-Estate", "River-Side", "School", "Bus-Stand",
        "Residential-Area", "Highway", "Warehouse", "Park", "College", "Other"
    };

    // Adjacency list: fromNode -> (toNode -> travelTimeMinutes)
    private final Map<String, Map<String, Integer>> graph = new HashMap<>();
    // Track closed roads for simulation: "nodeA<->nodeB" -> originalWeight
    private final Map<String, Integer> closedRoads = new HashMap<>();

    public RoadNetwork() {
        initDefaultRoads();
    }

    public synchronized void addLocation(String loc) {
        graph.putIfAbsent(loc, new HashMap<>());
    }

    public synchronized void addRoad(String u, String v, int travelTime) {
        addLocation(u);
        addLocation(v);
        graph.get(u).put(v, travelTime);
        graph.get(v).put(u, travelTime);
    }

    public synchronized boolean closeRoad(String u, String v) {
        if (graph.containsKey(u) && graph.get(u).containsKey(v)) {
            int weight = graph.get(u).get(v);
            graph.get(u).remove(v);
            graph.get(v).remove(u);
            String key = edgeKey(u, v);
            closedRoads.put(key, weight);
            return true;
        }
        return false;
    }

    public synchronized boolean openRoad(String u, String v) {
        String key = edgeKey(u, v);
        if (closedRoads.containsKey(key)) {
            int weight = closedRoads.remove(key);
            addRoad(u, v, weight);
            return true;
        }
        return false;
    }

    public synchronized boolean isRoadClosed(String u, String v) {
        return closedRoads.containsKey(edgeKey(u, v));
    }

    private String edgeKey(String u, String v) {
        return u.compareTo(v) < 0 ? u + "<->" + v : v + "<->" + u;
    }

    /**
     * Dijkstra's Algorithm implementation using Min-Heap PriorityQueue.
     * Computes the minimum travel time route from start to destination.
     */
    public synchronized RouteResult findShortestPath(String start, String destination) {
        if (start == null || destination == null) {
            return new RouteResult(Collections.emptyList(), Integer.MAX_VALUE, false);
        }
        if (start.equals(destination)) {
            return new RouteResult(Collections.singletonList(start), 0, true);
        }
        if (!graph.containsKey(start) || !graph.containsKey(destination)) {
            return new RouteResult(Collections.emptyList(), Integer.MAX_VALUE, false);
        }

        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<DijkstraNode> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.travelTime));

        for (String node : graph.keySet()) {
            dist.put(node, Integer.MAX_VALUE);
        }
        dist.put(start, 0);
        pq.add(new DijkstraNode(start, 0));

        while (!pq.isEmpty()) {
            DijkstraNode curr = pq.poll();
            if (curr.travelTime > dist.get(curr.name)) continue;
            if (curr.name.equals(destination)) break;

            Map<String, Integer> neighbors = graph.getOrDefault(curr.name, Collections.emptyMap());
            for (Map.Entry<String, Integer> edge : neighbors.entrySet()) {
                String next = edge.getKey();
                int weight = edge.getValue();
                int newDist = curr.travelTime + weight;

                if (newDist < dist.getOrDefault(next, Integer.MAX_VALUE)) {
                    dist.put(next, newDist);
                    prev.put(next, curr.name);
                    pq.add(new DijkstraNode(next, newDist));
                }
            }
        }

        if (dist.get(destination) == Integer.MAX_VALUE || !prev.containsKey(destination)) {
            return new RouteResult(Collections.emptyList(), Integer.MAX_VALUE, false);
        }

        LinkedList<String> path = new LinkedList<>();
        String step = destination;
        while (step != null) {
            path.addFirst(step);
            step = prev.get(step);
        }

        return new RouteResult(path, dist.get(destination), true);
    }

    public synchronized Set<String> getLocations() {
        return Collections.unmodifiableSet(graph.keySet());
    }

    public synchronized Map<String, Map<String, Integer>> getGraphCopy() {
        Map<String, Map<String, Integer>> copy = new HashMap<>();
        for (var entry : graph.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return copy;
    }

    public synchronized Map<String, Integer> getClosedRoads() {
        return Collections.unmodifiableMap(closedRoads);
    }

    /**
     * Initializes Sudar town road topology (15 locations, realistic travel times in minutes).
     */
    private void initDefaultRoads() {
        // Station connections
        addRoad("Station-A", "Junction-1", 4);
        addRoad("Station-A", "Market", 7);
        addRoad("Station-B", "Junction-2", 3);
        addRoad("Station-B", "Highway", 6);

        // Central grid & facilities
        addRoad("Junction-1", "Junction-2", 5);
        addRoad("Junction-1", "Industrial-Estate", 6);
        addRoad("Junction-2", "Hospital", 4);
        addRoad("Junction-2", "River-Side", 7);

        // Community corridors
        addRoad("Market", "Hospital", 5);
        addRoad("Market", "School", 4);
        addRoad("Market", "Bus-Stand", 3);
        addRoad("Hospital", "Bus-Stand", 6);
        addRoad("Hospital", "Industrial-Estate", 8);

        // Residential, education, recreation
        addRoad("Bus-Stand", "Residential-Area", 4);
        addRoad("Residential-Area", "College", 5);
        addRoad("College", "School", 3);
        addRoad("School", "River-Side", 6);
        addRoad("River-Side", "Park", 5);
        addRoad("Park", "College", 4);

        // Logistics & industrial outskirts
        addRoad("Highway", "Warehouse", 5);
        addRoad("Warehouse", "Industrial-Estate", 4);
        addRoad("Junction-1", "Market", 4);
        addRoad("Other", "Junction-1", 4);
        addRoad("Other", "Residential-Area", 4);
    }

    private static class DijkstraNode {
        final String name;
        final int travelTime;

        DijkstraNode(String name, int travelTime) {
            this.name = name;
            this.travelTime = travelTime;
        }
    }

    /**
     * Value object encapsulating calculated Dijkstra route result.
     */
    public static class RouteResult {
        private final List<String> path;
        private final int travelTimeMinutes;
        private final boolean reachable;

        public RouteResult(List<String> path, int travelTimeMinutes, boolean reachable) {
            this.path = path;
            this.travelTimeMinutes = travelTimeMinutes;
            this.reachable = reachable;
        }

        public List<String> getPath() { return path; }
        public int getTravelTimeMinutes() { return travelTimeMinutes; }
        public boolean isReachable() { return reachable; }

        public String getPathString() {
            return path.isEmpty() ? "No Route" : String.join(" -> ", path);
        }

        @Override
        public String toString() {
            return reachable ? String.format("%s (%d mins)", getPathString(), travelTimeMinutes) : "Unreachable";
        }
    }
}
