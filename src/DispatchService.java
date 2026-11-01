import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core dispatch coordinator for the Smart Fire & Rescue Response System.
 *
 * Implements:
 * - PriorityQueue<Incident> for urgency ranking (Intensity -> Critical -> Pop -> FIFO).
 * - HashMap for O(1) indexing of units, stations, and incidents.
 * - HashSet for equipment compatibility validation.
 * - Dijkstra's algorithm for minimum-travel-time route computation.
 * - Encapsulated state updates (assignIncident, completeMission).
 */
public class DispatchService {

    private final RoadNetwork roadNetwork;
    private final DatabaseManager dbManager;

    // Core Data Structures
    private final PriorityQueue<Incident> waitingQueue;
    private final Map<Integer, Incident> incidentMap = new LinkedHashMap<>();
    private final Map<String, ResponseUnit> unitMap = new LinkedHashMap<>();
    private final Map<String, FireStation> stationMap = new LinkedHashMap<>();
    private final List<DispatchRecord> missionHistory = new ArrayList<>();

    private final AtomicInteger arrivalSequence = new AtomicInteger(100);

    public DispatchService(RoadNetwork roadNetwork, DatabaseManager dbManager) {
        this.roadNetwork = roadNetwork;
        this.dbManager = dbManager;
        this.waitingQueue = new PriorityQueue<>(new IncidentPriorityPolicy.DefaultPolicy().comparator());

        initSystemData();
    }

    private synchronized void initSystemData() {
        // 1. Try loading from MySQL
        if (dbManager.isConnected()) {
            dbManager.seedDefaultDataIfEmpty();
            List<FireStation> dbStations = dbManager.loadStations();
            List<ResponseUnit> dbUnits = dbManager.loadResponseUnits();
            List<Incident> dbIncidents = dbManager.loadIncidents();
            List<DispatchRecord> dbHistory = dbManager.loadMissionHistory();

            for (FireStation stn : dbStations) stationMap.put(stn.getStationCode(), stn);
            for (ResponseUnit u : dbUnits) {
                unitMap.put(u.getUnitCode(), u);
                // Associate with station if matching
                for (FireStation stn : stationMap.values()) {
                    if (stn.getStationName().equalsIgnoreCase(u.getStationName())) {
                        stn.addUnit(u);
                    }
                }
            }
            for (Incident inc : dbIncidents) {
                incidentMap.put(inc.getId(), inc);
                if ("PENDING".equalsIgnoreCase(inc.getStatus())) {
                    waitingQueue.offer(inc);
                }
            }
            missionHistory.addAll(dbHistory);
        }

        // 2. Fallback / default seed if database was offline or returned empty
        if (stationMap.isEmpty()) {
            FireStation stnA = new FireStation(1, "STN-A", "Central Fire Station", "Station-A");
            FireStation stnB = new FireStation(2, "STN-B", "North Fire Station", "Station-B");
            stationMap.put(stnA.getStationCode(), stnA);
            stationMap.put(stnB.getStationCode(), stnB);
        }

        if (unitMap.isEmpty()) {
            FireEngine fe1 = new FireEngine(1, "FE-01", "Central Fire Station", "Station-A", true,
                    new HashSet<>(Arrays.asList("HOSE", "FOAM", "LADDER", "WATERPUMP")));
            RescueVan rv1 = new RescueVan(2, "RV-01", "Central Fire Station", "Station-A", true,
                    new HashSet<>(Arrays.asList("MEDICAL", "CUTTER", "RESCUEKIT")));
            FireEngine fe2 = new FireEngine(3, "FE-02", "North Fire Station", "Station-B", true,
                    new HashSet<>(Arrays.asList("HOSE", "FOAM", "WATERPUMP")));
            RescueVan rv2 = new RescueVan(4, "RV-02", "North Fire Station", "Station-B", true,
                    new HashSet<>(Arrays.asList("MEDICAL", "CUTTER", "BOAT")));

            unitMap.put(fe1.getUnitCode(), fe1);
            unitMap.put(rv1.getUnitCode(), rv1);
            unitMap.put(fe2.getUnitCode(), fe2);
            unitMap.put(rv2.getUnitCode(), rv2);

            FireStation stnA = stationMap.get("STN-A");
            if (stnA != null) { stnA.addUnit(fe1); stnA.addUnit(rv1); }
            FireStation stnB = stationMap.get("STN-B");
            if (stnB != null) { stnB.addUnit(fe2); stnB.addUnit(rv2); }
        }

        if (incidentMap.isEmpty()) {
            seedSampleIncidents();
        }
    }

    private void seedSampleIncidents() {
        Object[][] samples = {
            {"Hospital", "ICU Electrical Fire", 5, true, 3, "FOAM"},
            {"School", "Chemistry Lab Explosion", 4, true, 3, "FOAM"},
            {"Industrial-Estate", "Chemical Storage Tank Blaze", 5, true, 2, "FOAM"},
            {"Market", "Commercial Complex Fire", 4, false, 3, "HOSE"},
            {"River-Side", "Boat Overturn & Flash Flood Trapped", 3, false, 2, "BOAT"},
            {"Warehouse", "Timber Yard Fire", 4, false, 1, "LADDER"},
            {"Highway", "Multi-Vehicle Pileup with Fuel Spill", 4, true, 2, "CUTTER"},
            {"Residential-Area", "High-Rise Apartment Balcony Fire", 3, false, 3, "LADDER"},
            {"Bus-Stand", "Commuter Bus Engine Fire", 3, false, 3, "HOSE"},
            {"College", "Auditorium Ceiling Collapse & Fire", 3, true, 2, "MEDICAL"},
            {"Park", "Community Gazebo Fire & Smoke", 2, false, 2, "HOSE"},
            {"Junction-1", "Traffic Signal Transformer Blast", 3, true, 2, "WATERPUMP"},
            {"Junction-2", "Fuel Tanker Leak Emergency", 4, true, 2, "FOAM"},
            {"Residential-Area", "Kitchen Gas Cylinder Leakage", 2, false, 2, "MEDICAL"},
            {"Industrial-Estate", "Machinery Jam with Trapped Worker", 3, true, 1, "CUTTER"}
        };

        for (int i = 0; i < samples.length; i++) {
            int num = 101 + i;
            String code = "INC-" + num;
            String loc = (String) samples[i][0];
            String type = (String) samples[i][1];
            int intensity = (Integer) samples[i][2];
            boolean crit = (Boolean) samples[i][3];
            int pop = (Integer) samples[i][4];
            String eq = (String) samples[i][5];

            Incident inc = new Incident(num, code, loc, type, intensity, crit, pop, eq,
                    arrivalSequence.incrementAndGet(), "PENDING", null);
            incidentMap.put(inc.getId(), inc);
            waitingQueue.offer(inc);
        }
    }

    /**
     * Registers a new incident report and enqueues it into the PriorityQueue.
     */
    public synchronized Incident registerIncident(String location, String type, int intensity,
                                                  boolean critical, int population, String equipment) {
        int order = arrivalSequence.incrementAndGet();
        int nextId = incidentMap.keySet().stream().max(Integer::compareTo).orElse(100) + 1;
        String code = "INC-" + nextId;

        Incident inc = new Incident(nextId, code, location, type, intensity, critical,
                population, equipment, order, "PENDING", null);

        // Persist to MySQL if available
        if (dbManager.isConnected()) {
            int dbId = dbManager.saveIncident(inc);
            if (dbId > 0) inc.setId(dbId);
        }

        incidentMap.put(inc.getId(), inc);
        waitingQueue.offer(inc);
        return inc;
    }

    /**
     * Core Dispatch Algorithm:
     * 1. Inspects highest-priority incident from PriorityQueue.
     * 2. Evaluates all available units for equipment compatibility (HashSet).
     * 3. Calculates shortest path via Dijkstra's Algorithm (Min-Heap).
     * 4. Selects reachable unit with minimal travel time.
     * 5. Encapsulates state updates (unit becomes BUSY, incident becomes DISPATCHED).
     */
    public synchronized DispatchResult dispatchNextIncident() {
        if (waitingQueue.isEmpty()) {
            return new DispatchResult(false, "No pending incidents in the queue.", null, null, null, 0);
        }

        // Peek highest-urgency incident
        Incident incident = waitingQueue.peek();

        ResponseUnit selectedUnit = null;
        RoadNetwork.RouteResult selectedRoute = null;
        int shortestTravelTime = Integer.MAX_VALUE;

        // Check compatibility & travel times across all response units
        for (ResponseUnit unit : unitMap.values()) {
            if (!unit.isAvailable()) continue;
            if (!unit.supportsRequirement(incident.getRequiredEquipment())) continue;

            RoadNetwork.RouteResult route = roadNetwork.findShortestPath(unit.getCurrentLocation(), incident.getLocation());
            if (route.isReachable() && route.getTravelTimeMinutes() < shortestTravelTime) {
                shortestTravelTime = route.getTravelTimeMinutes();
                selectedUnit = unit;
                selectedRoute = route;
            }
        }

        if (selectedUnit == null) {
            // Check if any unit supports the equipment at all
            boolean anyCompatible = unitMap.values().stream()
                    .anyMatch(u -> u.supportsRequirement(incident.getRequiredEquipment()));

            String reason = anyCompatible
                    ? "All units supporting " + incident.getRequiredEquipment() + " are currently BUSY. Incident remains PENDING."
                    : "No response unit in the fleet is equipped with " + incident.getRequiredEquipment() + ". Incident remains PENDING.";

            return new DispatchResult(false, reason, incident, null, null, 0);
        }

        // Successfully allocate unit
        waitingQueue.poll(); // Remove from queue
        selectedUnit.assignIncident();
        incident.setStatus("DISPATCHED");

        // Record mission
        DispatchRecord record = new DispatchRecord(
                incident.getId(),
                incident.getIncidentCode(),
                selectedUnit.getUnitCode(),
                selectedUnit.getCurrentLocation(),
                incident.getLocation(),
                selectedRoute.getPathString(),
                selectedRoute.getTravelTimeMinutes()
        );

        if (dbManager.isConnected()) {
            dbManager.updateIncidentStatus(incident.getId(), "DISPATCHED");
            dbManager.updateUnitStatusAndLocation(selectedUnit.getUnitCode(), "BUSY", selectedUnit.getCurrentLocation());
            dbManager.saveMissionRecord(record);
        }

        missionHistory.add(0, record);

        return new DispatchResult(
                true,
                "Dispatched unit " + selectedUnit.getUnitCode() + " to " + incident.getLocation(),
                incident,
                selectedUnit,
                selectedRoute,
                selectedRoute.getTravelTimeMinutes()
        );
    }

    /**
     * Marks an active mission as completed.
     * Releases unit back to AVAILABLE and updates unit position to incident location.
     */
    public synchronized boolean completeMission(int incidentId) {
        Incident inc = incidentMap.get(incidentId);
        if (inc == null) return false;

        // Find active mission for this incident
        DispatchRecord activeRecord = null;
        for (DispatchRecord rec : missionHistory) {
            if (rec.getIncidentId() == incidentId && "DISPATCHED".equalsIgnoreCase(rec.getStatus())) {
                activeRecord = rec;
                break;
            }
        }

        if (activeRecord != null) {
            activeRecord.setStatus("COMPLETED");
            ResponseUnit unit = unitMap.get(activeRecord.getUnitCode());
            if (unit != null) {
                unit.completeMission(activeRecord.getTargetLocation());
                if (dbManager.isConnected()) {
                    dbManager.updateUnitStatusAndLocation(unit.getUnitCode(), "AVAILABLE", unit.getCurrentLocation());
                }
            }
            if (dbManager.isConnected()) {
                dbManager.updateMissionStatus(incidentId, "COMPLETED");
            }
        }

        inc.setStatus("COMPLETED");
        if (dbManager.isConnected()) {
            dbManager.updateIncidentStatus(inc.getId(), "COMPLETED");
        }
        return true;
    }

    public synchronized List<Incident> getAllIncidentsSorted() {
        List<Incident> list = new ArrayList<>(incidentMap.values());
        list.sort(new IncidentPriorityPolicy.DefaultPolicy().comparator());
        return list;
    }

    public synchronized List<ResponseUnit> getUnits() {
        return new ArrayList<>(unitMap.values());
    }

    public synchronized String getActiveMissionTarget(String unitCode) {
        for (DispatchRecord rec : missionHistory) {
            if (rec.getUnitCode().equalsIgnoreCase(unitCode) && "DISPATCHED".equalsIgnoreCase(rec.getStatus())) {
                return rec.getTargetLocation();
            }
        }
        return null;
    }

    public synchronized List<FireStation> getStations() {
        return new ArrayList<>(stationMap.values());
    }

    public synchronized List<DispatchRecord> getMissionHistory() {
        return new ArrayList<>(missionHistory);
    }

    public synchronized Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        int total = incidentMap.size();
        long pending = waitingQueue.size();
        long dispatched = incidentMap.values().stream().filter(i -> "DISPATCHED".equalsIgnoreCase(i.getStatus())).count();
        long completed = incidentMap.values().stream().filter(i -> "COMPLETED".equalsIgnoreCase(i.getStatus())).count();
        long availableUnits = unitMap.values().stream().filter(ResponseUnit::isAvailable).count();

        stats.put("totalIncidents", total);
        stats.put("pendingIncidents", pending);
        stats.put("dispatchedIncidents", dispatched);
        stats.put("completedIncidents", completed);
        stats.put("availableUnits", availableUnits);
        stats.put("totalUnits", unitMap.size());
        stats.put("totalStations", stationMap.size());
        stats.put("dbConnected", dbManager.isConnected());
        stats.put("dbMessage", dbManager.getStatusMessage());
        return stats;
    }

    /**
     * Viva / Jury Feature 1: Algorithmic Emergency Surge Stress Test.
     * Demonstrates dynamic heap reorganization in O(k log N) time when 3 concurrent
     * emergencies of varying priorities arrive simultaneously.
     */
    public synchronized Map<String, Object> simulateEmergencySurge() {
        Map<String, Object> result = new LinkedHashMap<>();

        Incident topBefore = waitingQueue.peek();

        Incident i1 = registerIncident("Industrial-Estate", "Chemical Storage Tank Blaze", 5, true, 3, "FOAM");
        Incident i2 = registerIncident("Highway", "Multi-Vehicle Pileup with Fuel Spill", 4, true, 2, "CUTTER");
        Incident i3 = registerIncident("Residential-Area", "Multi-Family Apartment Fire", 3, false, 2, "HOSE");

        Incident topAfter = waitingQueue.peek();

        result.put("success", true);
        result.put("message", "Simulated surge of 3 concurrent emergencies enqueued into Max-Heap PriorityQueue.");
        result.put("previousTopCode", topBefore != null ? topBefore.getIncidentCode() : "NONE");
        result.put("previousTopPriority", topBefore != null ? topBefore.getPriority() : 0);
        result.put("newTopCode", topAfter != null ? topAfter.getIncidentCode() : "NONE");
        result.put("newTopPriority", topAfter != null ? topAfter.getPriority() : 0);
        result.put("surgeCount", 3);
        result.put("totalPendingNow", waitingQueue.size());

        List<Map<String, Object>> incidentSummaries = new ArrayList<>();
        for (Incident inc : Arrays.asList(i1, i2, i3)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", inc.getIncidentCode());
            m.put("location", inc.getLocation());
            m.put("priority", inc.getPriority());
            m.put("equipment", inc.getRequiredEquipment());
            incidentSummaries.add(m);
        }
        result.put("surgeIncidents", incidentSummaries);

        return result;
    }

    public RoadNetwork getRoadNetwork() {
        return roadNetwork;
    }

    public DatabaseManager getDbManager() {
        return dbManager;
    }

    /**
     * Dispatch response value object.
     */
    public static class DispatchResult {
        public final boolean success;
        public final String message;
        public final Incident incident;
        public final ResponseUnit unit;
        public final RoadNetwork.RouteResult route;
        public final int travelTimeMinutes;

        public DispatchResult(boolean success, String message, Incident incident,
                              ResponseUnit unit, RoadNetwork.RouteResult route, int travelTimeMinutes) {
            this.success = success;
            this.message = message;
            this.incident = incident;
            this.unit = unit;
            this.route = route;
            this.travelTimeMinutes = travelTimeMinutes;
        }
    }
}
