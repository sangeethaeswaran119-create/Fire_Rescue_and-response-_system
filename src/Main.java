import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;


public class Main {

    private static RoadNetwork roadNetwork;
    private static DatabaseManager dbManager;
    private static DispatchService dispatchService;

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================");
        System.out.println("    SMART FIRE AND RESCUE RESPONSE SYSTEM (SUDAR TOWN)");
        System.out.println("===============================================================");

        
        roadNetwork = new RoadNetwork();
        System.out.println("[INIT] Road network initialized with 15 town locations & road segments.");

        
        dbManager = new DatabaseManager();
        if (dbManager.isConnected()) {
            System.out.println("[DB] MySQL connection established successfully.");
        } else {
            System.out.println("[DB] Running with in-memory persistence fallback.");
            System.out.println("     Reason: " + dbManager.getLastErrorMessage());
            System.out.println("     To enable MySQL: update password in src/Config.java.");
        }

        // 3. Initialize Dispatch Service & PriorityQueue
        dispatchService = new DispatchService(roadNetwork, dbManager);
        System.out.println("[INIT] DispatchService loaded with PriorityQueue & response units.");

       
        int port = Config.HTTP_PORT;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

       
        server.createContext("/api/status", Main::handleStatus);
        server.createContext("/api/incidents", Main::handleIncidents);
        server.createContext("/api/units", Main::handleUnits);
        server.createContext("/api/dispatch", Main::handleDispatch);
        server.createContext("/api/complete", Main::handleComplete);
        server.createContext("/api/history", Main::handleHistory);
        server.createContext("/api/roads", Main::handleRoads);
        server.createContext("/api/road/toggle", Main::handleRoadToggle);
        server.createContext("/api/simulate/surge", Main::handleSimulateSurge);

        
        server.createContext("/", Main::handleWebFiles);

        server.start();

        System.out.println("---------------------------------------------------------------");
        System.out.println("Server is running!");
        System.out.println("Open your browser at: http://localhost:" + port);
        System.out.println("Press Ctrl+C to terminate the server.");
        System.out.println("---------------------------------------------------------------");
    }

    
    private static void handleStatus(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }

        Map<String, Object> stats = dispatchService.getSystemStats();
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"totalIncidents\":").append(stats.get("totalIncidents")).append(",");
        sb.append("\"pendingIncidents\":").append(stats.get("pendingIncidents")).append(",");
        sb.append("\"dispatchedIncidents\":").append(stats.get("dispatchedIncidents")).append(",");
        sb.append("\"completedIncidents\":").append(stats.get("completedIncidents")).append(",");
        sb.append("\"availableUnits\":").append(stats.get("availableUnits")).append(",");
        sb.append("\"totalUnits\":").append(stats.get("totalUnits")).append(",");
        sb.append("\"totalStations\":").append(stats.get("totalStations")).append(",");
        sb.append("\"dbConnected\":").append(stats.get("dbConnected")).append(",");
        sb.append("\"dbMessage\":").append(jsonEscape(String.valueOf(stats.get("dbMessage"))));
        sb.append("}");

        sendResponse(exchange, 200, sb.toString(), "application/json");
    }

    private static void handleIncidents(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            List<Incident> list = dispatchService.getAllIncidentsSorted();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                Incident inc = list.get(i);
                sb.append("{")
                  .append("\"id\":").append(inc.getId()).append(",")
                  .append("\"code\":").append(jsonEscape(inc.getIncidentCode())).append(",")
                  .append("\"location\":").append(jsonEscape(inc.getLocation())).append(",")
                  .append("\"type\":").append(jsonEscape(inc.getType())).append(",")
                  .append("\"intensity\":").append(inc.getIntensity()).append(",")
                  .append("\"critical\":").append(inc.isCriticalInfrastructure()).append(",")
                  .append("\"population\":").append(inc.getPopulationExposure()).append(",")
                  .append("\"equipment\":").append(jsonEscape(inc.getRequiredEquipment())).append(",")
                  .append("\"priority\":").append(inc.calculatePriority()).append(",")
                  .append("\"status\":").append(jsonEscape(inc.getStatus())).append(",")
                  .append("\"createdAt\":").append(jsonEscape(inc.getCreatedAt()))
                  .append("}");
            }
            sb.append("]");
            sendResponse(exchange, 200, sb.toString(), "application/json");
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseRequestBody(body);

                String location = normalizeLocation(requiredParam(params, "location"));
                String type = params.getOrDefault("type", "Emergency Incident");
                if (type == null || type.isBlank()) type = "Emergency Incident";
                String equipment = params.getOrDefault("equipment", "HOSE").trim().toUpperCase();
                if (equipment.isBlank()) equipment = "HOSE";
                int intensity = Integer.parseInt(params.getOrDefault("intensity", "3"));
                int population = Integer.parseInt(params.getOrDefault("population", "1"));
                boolean critical = "true".equalsIgnoreCase(params.get("critical")) || "on".equalsIgnoreCase(params.get("critical"));

                // Validate location
                boolean validLoc = Arrays.asList(RoadNetwork.TOWN_LOCATIONS).contains(location);
                if (!validLoc) {
                    throw new IllegalArgumentException("Unknown location: " + location);
                }

                Incident registered = dispatchService.registerIncident(location, type, intensity, critical, population, equipment);

                String json = String.format("{\"success\":true,\"message\":\"Incident registered successfully\",\"incidentId\":%d,\"code\":%s,\"priority\":%d}",
                        registered.getId(), jsonEscape(registered.getIncidentCode()), registered.calculatePriority());
                sendResponse(exchange, 201, json, "application/json");
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"success\":false,\"error\":" + jsonEscape(e.getMessage()) + "}", "application/json");
            }
            return;
        }

        sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
    }

    private static void handleUnits(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }

        List<ResponseUnit> units = dispatchService.getUnits();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < units.size(); i++) {
            if (i > 0) sb.append(",");
            ResponseUnit u = units.get(i);
            String target = dispatchService.getActiveMissionTarget(u.getUnitCode());
            String displayPos = u.getCurrentLocation();
            if (target != null && !u.isAvailable()) {
                displayPos = u.getCurrentLocation() + " -> " + target;
            }
            sb.append("{")
              .append("\"id\":").append(u.getId()).append(",")
              .append("\"code\":").append(jsonEscape(u.getUnitCode())).append(",")
              .append("\"type\":").append(jsonEscape(u.getUnitType())).append(",")
              .append("\"station\":").append(jsonEscape(u.getStationName())).append(",")
              .append("\"location\":").append(jsonEscape(displayPos)).append(",")
              .append("\"target\":").append(jsonEscape(target == null ? "" : target)).append(",")
              .append("\"available\":").append(u.isAvailable()).append(",")
              .append("\"status\":").append(jsonEscape(u.isAvailable() ? "AVAILABLE" : "BUSY")).append(",")
              .append("\"equipment\":").append(jsonEscape(u.getEquipmentCsv()))
              .append("}");
        }
        sb.append("]");
        sendResponse(exchange, 200, sb.toString(), "application/json");
    }

    private static void handleDispatch(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }

        DispatchService.DispatchResult result = dispatchService.dispatchNextIncident();

        if (result.success) {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"success\":true,");
            sb.append("\"message\":").append(jsonEscape(result.message)).append(",");
            sb.append("\"incidentId\":").append(result.incident.getId()).append(",");
            sb.append("\"incidentCode\":").append(jsonEscape(result.incident.getIncidentCode())).append(",");
            sb.append("\"location\":").append(jsonEscape(result.incident.getLocation())).append(",");
            sb.append("\"unitCode\":").append(jsonEscape(result.unit.getUnitCode())).append(",");
            sb.append("\"unitType\":").append(jsonEscape(result.unit.getUnitType())).append(",");
            sb.append("\"station\":").append(jsonEscape(result.unit.getStationName())).append(",");
            sb.append("\"travelTimeMinutes\":").append(result.travelTimeMinutes).append(",");
            sb.append("\"route\":").append(jsonEscape(result.route.getPathString())).append(",");
            sb.append("\"path\":[");
            for (int i = 0; i < result.route.getPath().size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(jsonEscape(result.route.getPath().get(i)));
            }
            sb.append("],\"dispatchedTimestamp\":").append(System.currentTimeMillis()).append("}");
            sendResponse(exchange, 200, sb.toString(), "application/json");
        } else {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"success\":false,");
            sb.append("\"message\":").append(jsonEscape(result.message));
            if (result.incident != null) {
                sb.append(",\"incidentId\":").append(result.incident.getId());
                sb.append(",\"incidentCode\":").append(jsonEscape(result.incident.getIncidentCode()));
            }
            sb.append("}");
            sendResponse(exchange, 409, sb.toString(), "application/json");
        }
    }

    private static void handleComplete(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseRequestBody(body);
            int incidentId = Integer.parseInt(requiredParam(params, "incidentId"));

            boolean completed = dispatchService.completeMission(incidentId);
            if (completed) {
                sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Mission completed successfully. Response unit released to AVAILABLE.\"}", "application/json");
            } else {
                sendResponse(exchange, 404, "{\"success\":false,\"error\":\"Active incident not found\"}", "application/json");
            }
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":" + jsonEscape(e.getMessage()) + "}", "application/json");
        }
    }

    private static void handleHistory(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }

        List<DispatchRecord> list = dispatchService.getMissionHistory();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            DispatchRecord rec = list.get(i);
            sb.append("{")
              .append("\"id\":").append(rec.getId()).append(",")
              .append("\"incidentId\":").append(rec.getIncidentId()).append(",")
              .append("\"incidentCode\":").append(jsonEscape(rec.getIncidentCode())).append(",")
              .append("\"unitCode\":").append(jsonEscape(rec.getUnitCode())).append(",")
              .append("\"startLocation\":").append(jsonEscape(rec.getStartLocation())).append(",")
              .append("\"targetLocation\":").append(jsonEscape(rec.getTargetLocation())).append(",")
              .append("\"route\":").append(jsonEscape(rec.getRouteText())).append(",")
              .append("\"travelTimeMinutes\":").append(rec.getTravelTimeMins()).append(",")
              .append("\"status\":").append(jsonEscape(rec.getStatus())).append(",")
              .append("\"createdAt\":").append(jsonEscape(rec.getCreatedAt())).append(",")
              .append("\"dispatchedTimestamp\":").append(rec.getDispatchedTimestamp())
              .append("}");
        }
        sb.append("]");
        sendResponse(exchange, 200, sb.toString(), "application/json");
    }

    private static void handleSimulateSurge(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }

        Map<String, Object> surge = dispatchService.simulateEmergencySurge();
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"success\":true,");
        sb.append("\"message\":").append(jsonEscape(String.valueOf(surge.get("message")))).append(",");
        sb.append("\"previousTopCode\":").append(jsonEscape(String.valueOf(surge.get("previousTopCode")))).append(",");
        sb.append("\"previousTopPriority\":").append(surge.get("previousTopPriority")).append(",");
        sb.append("\"newTopCode\":").append(jsonEscape(String.valueOf(surge.get("newTopCode")))).append(",");
        sb.append("\"newTopPriority\":").append(surge.get("newTopPriority")).append(",");
        sb.append("\"surgeCount\":").append(surge.get("surgeCount")).append(",");
        sb.append("\"totalPendingNow\":").append(surge.get("totalPendingNow"));
        sb.append("}");

        sendResponse(exchange, 200, sb.toString(), "application/json");
    }

    private static void handleRoads(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"locations\":[");
        for (int i = 0; i < RoadNetwork.TOWN_LOCATIONS.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(jsonEscape(RoadNetwork.TOWN_LOCATIONS[i]));
        }
        sb.append("],\"roads\":[");

        Map<String, Map<String, Integer>> graph = roadNetwork.getGraphCopy();
        Set<String> seenEdges = new HashSet<>();
        boolean first = true;
        for (var fromEntry : graph.entrySet()) {
            String u = fromEntry.getKey();
            for (var toEntry : fromEntry.getValue().entrySet()) {
                String v = toEntry.getKey();
                String edgeKey = u.compareTo(v) < 0 ? u + "--" + v : v + "--" + u;
                if (seenEdges.add(edgeKey)) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"from\":").append(jsonEscape(u))
                      .append(",\"to\":").append(jsonEscape(v))
                      .append(",\"time\":").append(toEntry.getValue())
                      .append(",\"closed\":false}");
                }
            }
        }
        for (var closed : roadNetwork.getClosedRoads().entrySet()) {
            String[] parts = closed.getKey().split("<->");
            if (parts.length == 2) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"from\":").append(jsonEscape(parts[0]))
                  .append(",\"to\":").append(jsonEscape(parts[1]))
                  .append(",\"time\":").append(closed.getValue())
                  .append(",\"closed\":true}");
            }
        }
        sb.append("]}");
        sendResponse(exchange, 200, sb.toString(), "application/json");
    }

    private static void handleRoadToggle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseRequestBody(body);
            String u = requiredParam(params, "from");
            String v = requiredParam(params, "to");

            boolean nowClosed;
            if (roadNetwork.isRoadClosed(u, v)) {
                roadNetwork.openRoad(u, v);
                nowClosed = false;
            } else {
                roadNetwork.closeRoad(u, v);
                nowClosed = true;
            }

            String msg = nowClosed ? "Road closed: " + u + " <-> " + v : "Road reopened: " + u + " <-> " + v;
            sendResponse(exchange, 200, "{\"success\":true,\"closed\":" + nowClosed + ",\"message\":" + jsonEscape(msg) + "}", "application/json");
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":" + jsonEscape(e.getMessage()) + "}", "application/json");
        }
    }

    // ====================================================================
    // STATIC FILE HANDLER
    // ====================================================================

    private static void handleWebFiles(HttpExchange exchange) throws IOException {
        String pathStr = exchange.getRequestURI().getPath();
        if ("/".equals(pathStr) || pathStr.isEmpty()) {
            pathStr = "/index.html";
        }

        Path webRoot = Paths.get("web").toAbsolutePath().normalize();
        if (!Files.isDirectory(webRoot)) {
            Path fallback = Paths.get("D:\\My Documents\\Downloads\\Smart_Fire_Rescue_No_Maven\\web").toAbsolutePath().normalize();
            if (Files.isDirectory(fallback)) {
                webRoot = fallback;
            }
        }

        Path targetFile = webRoot.resolve(pathStr.substring(1)).normalize();

        if (!targetFile.startsWith(webRoot) || !Files.isRegularFile(targetFile)) {
            sendResponse(exchange, 404, "404 Not Found: " + pathStr, "text/plain");
            return;
        }

        String mimeType = "text/html; charset=UTF-8";
        String fn = targetFile.getFileName().toString().toLowerCase();
        if (fn.endsWith(".css")) mimeType = "text/css; charset=UTF-8";
        else if (fn.endsWith(".js")) mimeType = "application/javascript; charset=UTF-8";
        else if (fn.endsWith(".json")) mimeType = "application/json; charset=UTF-8";
        else if (fn.endsWith(".png")) mimeType = "image/png";
        else if (fn.endsWith(".svg")) mimeType = "image/svg+xml";

        byte[] bytes = Files.readAllBytes(targetFile);
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    
    private static void sendResponse(HttpExchange exchange, int statusCode, String content, String contentType) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseRequestBody(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isBlank()) return map;

        // Try JSON parsing if body starts with '{'
        String trimmed = body.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
            for (String pair : trimmed.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String k = kv[0].trim().replaceAll("^\"|\"$", "");
                    String v = kv[1].trim().replaceAll("^\"|\"$", "");
                    map.put(k, v);
                }
            }
            return map;
        }

        
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String v = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                map.put(k, v);
            }
        }
        return map;
    }

    private static String requiredParam(Map<String, String> params, String key) {
        String val = params.get(key);
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return val.trim();
    }

    private static String normalizeLocation(String loc) {
        if (loc == null || loc.isBlank()) return "Residential-Area";
        loc = loc.trim();
        for (String node : RoadNetwork.TOWN_LOCATIONS) {
            if (node.equalsIgnoreCase(loc)) return node;
        }
        String lower = loc.toLowerCase();
        if (lower.contains("residential")) return "Residential-Area";
        if (lower.contains("hospital")) return "Hospital";
        if (lower.contains("school")) return "School";
        if (lower.contains("market")) return "Market";
        if (lower.contains("industrial")) return "Industrial-Estate";
        if (lower.contains("river")) return "River-Side";
        if (lower.contains("bus")) return "Bus-Stand";
        if (lower.contains("highway")) return "Highway";
        if (lower.contains("warehouse")) return "Warehouse";
        if (lower.contains("park")) return "Park";
        if (lower.contains("college")) return "College";
        if (lower.contains("junction-1") || lower.contains("junction 1") || lower.contains("west junction")) return "Junction-1";
        if (lower.contains("junction-2") || lower.contains("junction 2") || lower.contains("east junction")) return "Junction-2";
        if (lower.contains("station-a") || lower.contains("station a")) return "Station-A";
        if (lower.contains("station-b") || lower.contains("station b")) return "Station-B";
        if (lower.contains("other")) return "Other";
        return loc;
    }

    private static String jsonEscape(String str) {
        if (str == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
