import java.sql.*;
import java.util.*;

/**
 * Handles all MySQL JDBC database operations using PreparedStatements.
 * Implements persistence for stations, response units, incidents, and mission logs.
 *
 * If MySQL is temporarily offline or password is not yet configured,
 * it captures the exact error and provides clear diagnostic feedback.
 */
public class DatabaseManager {

    private boolean connected = false;
    private String connectionStatusMessage = "Not connected";
    private String lastErrorMessage = "";

    public DatabaseManager() {
        checkAndInitializeConnection();
    }

    public synchronized boolean isConnected() {
        return connected;
    }

    public synchronized String getStatusMessage() {
        return connectionStatusMessage;
    }

    public synchronized String getLastErrorMessage() {
        return lastErrorMessage;
    }

    /**
     * Verifies MySQL connection and verifies schema tables exist.
     */
    public synchronized boolean checkAndInitializeConnection() {
        try (Connection conn = Config.getConnection()) {
            connected = true;
            connectionStatusMessage = "Connected to MySQL database (" + Config.URL + ")";
            lastErrorMessage = "";
            ensureTablesExist(conn);
            return true;
        } catch (SQLException e) {
            connected = false;
            lastErrorMessage = e.getMessage();
            connectionStatusMessage = "MySQL unavailable: " + e.getMessage() +
                    "\n(Check src/Config.java, verify port 3307 or 3306, and enter your MySQL root password)";
            System.err.println("----------------------------------------------------------------");
            System.err.println("[DATABASE WARNING] Could not connect to MySQL database.");
            System.err.println("Reason: " + e.getMessage());
            System.err.println("Target URL: " + Config.URL + " | User: " + Config.USER);
            System.err.println("Setup Step: Please verify MySQL is running and set your password in src/Config.java");
            System.err.println("----------------------------------------------------------------");
            return false;
        }
    }

    private void ensureTablesExist(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS stations (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "station_code VARCHAR(30) UNIQUE NOT NULL," +
                    "station_name VARCHAR(100) NOT NULL," +
                    "location_name VARCHAR(100) NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS response_units (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "unit_code VARCHAR(30) UNIQUE NOT NULL," +
                    "unit_type VARCHAR(50) NOT NULL," +
                    "station_name VARCHAR(100) NOT NULL," +
                    "current_location VARCHAR(100) NOT NULL," +
                    "equipment VARCHAR(255) NOT NULL," +
                    "status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE')");

            stmt.execute("CREATE TABLE IF NOT EXISTS incidents (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "incident_code VARCHAR(30) UNIQUE NOT NULL," +
                    "location_name VARCHAR(100) NOT NULL," +
                    "incident_type VARCHAR(100) NOT NULL," +
                    "intensity INT NOT NULL," +
                    "critical BOOLEAN NOT NULL DEFAULT FALSE," +
                    "population_exposure INT NOT NULL DEFAULT 1," +
                    "required_equipment VARCHAR(50) NOT NULL," +
                    "status VARCHAR(30) NOT NULL DEFAULT 'PENDING'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS mission_history (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "incident_id INT NOT NULL," +
                    "unit_code VARCHAR(30) NOT NULL," +
                    "start_location VARCHAR(100) NOT NULL," +
                    "target_location VARCHAR(100) NOT NULL," +
                    "route_text VARCHAR(500) NOT NULL," +
                    "travel_time_mins INT NOT NULL DEFAULT 0," +
                    "status VARCHAR(30) NOT NULL DEFAULT 'DISPATCHED'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    public synchronized List<FireStation> loadStations() {
        List<FireStation> list = new ArrayList<>();
        if (!connected) return list;

        String sql = "SELECT id, station_code, station_name, location_name FROM stations ORDER BY id";
        try (Connection conn = Config.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new FireStation(
                        rs.getInt("id"),
                        rs.getString("station_code"),
                        rs.getString("station_name"),
                        rs.getString("location_name")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading stations: " + e.getMessage());
        }
        return list;
    }

    public synchronized List<ResponseUnit> loadResponseUnits() {
        List<ResponseUnit> list = new ArrayList<>();
        if (!connected) return list;

        String sql = "SELECT id, unit_code, unit_type, station_name, current_location, equipment, status FROM response_units ORDER BY id";
        try (Connection conn = Config.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String code = rs.getString("unit_code");
                String type = rs.getString("unit_type");
                String stn = rs.getString("station_name");
                String loc = rs.getString("current_location");
                String eqStr = rs.getString("equipment");
                boolean avail = "AVAILABLE".equalsIgnoreCase(rs.getString("status"));

                Set<String> eqSet = new HashSet<>();
                if (eqStr != null) {
                    for (String part : eqStr.split(",")) {
                        if (!part.isBlank()) eqSet.add(part.trim().toUpperCase());
                    }
                }

                if ("Rescue Van".equalsIgnoreCase(type)) {
                    list.add(new RescueVan(id, code, stn, loc, avail, eqSet));
                } else {
                    list.add(new FireEngine(id, code, stn, loc, avail, eqSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading response units: " + e.getMessage());
        }
        return list;
    }

    public synchronized List<Incident> loadIncidents() {
        List<Incident> list = new ArrayList<>();
        if (!connected) return list;

        String sql = "SELECT id, incident_code, location_name, incident_type, intensity, " +
                "critical, population_exposure, required_equipment, status, created_at " +
                "FROM incidents ORDER BY id";
        try (Connection conn = Config.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int arrivalOrder = 1;
            while (rs.next()) {
                list.add(new Incident(
                        rs.getInt("id"),
                        rs.getString("incident_code"),
                        rs.getString("location_name"),
                        rs.getString("incident_type"),
                        rs.getInt("intensity"),
                        rs.getBoolean("critical"),
                        rs.getInt("population_exposure"),
                        rs.getString("required_equipment"),
                        arrivalOrder++,
                        rs.getString("status"),
                        rs.getString("created_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading incidents: " + e.getMessage());
        }
        return list;
    }

    public synchronized List<DispatchRecord> loadMissionHistory() {
        List<DispatchRecord> list = new ArrayList<>();
        if (!connected) return list;

        String sql = "SELECT id, incident_id, unit_code, start_location, target_location, " +
                "route_text, travel_time_mins, status, created_at " +
                "FROM mission_history ORDER BY id DESC";
        try (Connection conn = Config.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DispatchRecord(
                        rs.getInt("id"),
                        rs.getInt("incident_id"),
                        "INC-" + rs.getInt("incident_id"),
                        rs.getString("unit_code"),
                        rs.getString("start_location"),
                        rs.getString("target_location"),
                        rs.getString("route_text"),
                        rs.getInt("travel_time_mins"),
                        rs.getString("status"),
                        rs.getString("created_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading mission history: " + e.getMessage());
        }
        return list;
    }

    public synchronized int saveIncident(Incident incident) {
        if (!connected) return 0;

        String sql = "INSERT INTO incidents (incident_code, location_name, incident_type, " +
                "intensity, critical, population_exposure, required_equipment, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Config.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, incident.getIncidentCode());
            ps.setString(2, incident.getLocation());
            ps.setString(3, incident.getType());
            ps.setInt(4, incident.getIntensity());
            ps.setBoolean(5, incident.isCriticalInfrastructure());
            ps.setInt(6, incident.getPopulationExposure());
            ps.setString(7, incident.getRequiredEquipment());
            ps.setString(8, incident.getStatus());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int genId = rs.getInt(1);
                    incident.setId(genId);
                    return genId;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving incident to MySQL: " + e.getMessage());
        }
        return 0;
    }

    public synchronized boolean updateIncidentStatus(int incidentId, String status) {
        if (!connected) return false;

        String sql = "UPDATE incidents SET status = ? WHERE id = ?";
        try (Connection conn = Config.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, incidentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating incident status: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean updateUnitStatusAndLocation(String unitCode, String status, String location) {
        if (!connected) return false;

        String sql = "UPDATE response_units SET status = ?, current_location = ? WHERE unit_code = ?";
        try (Connection conn = Config.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, location);
            ps.setString(3, unitCode);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating unit status/location: " + e.getMessage());
            return false;
        }
    }

    public synchronized int saveMissionRecord(DispatchRecord record) {
        if (!connected) return 0;

        String sql = "INSERT INTO mission_history (incident_id, unit_code, start_location, " +
                "target_location, route_text, travel_time_mins, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Config.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, record.getIncidentId());
            ps.setString(2, record.getUnitCode());
            ps.setString(3, record.getStartLocation());
            ps.setString(4, record.getTargetLocation());
            ps.setString(5, record.getRouteText());
            ps.setInt(6, record.getTravelTimeMins());
            ps.setString(7, record.getStatus());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    record.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving mission record: " + e.getMessage());
        }
        return 0;
    }

    public synchronized boolean updateMissionStatus(int incidentId, String status) {
        if (!connected) return false;

        String sql = "UPDATE mission_history SET status = ? WHERE incident_id = ? AND status = 'DISPATCHED'";
        try (Connection conn = Config.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, incidentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating mission status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Seeds default database records if tables are empty.
     * Uses INSERT IGNORE / ON DUPLICATE KEY UPDATE so it never duplicates records.
     */
    public synchronized void seedDefaultDataIfEmpty() {
        if (!connected) return;

        try (Connection conn = Config.getConnection();
             Statement stmt = conn.createStatement()) {

            // Check if stations empty
            ResultSet rsStn = stmt.executeQuery("SELECT COUNT(*) FROM stations");
            if (rsStn.next() && rsStn.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO stations (station_code, station_name, location_name) VALUES " +
                        "('STN-A', 'Central Fire Station', 'Station-A'), " +
                        "('STN-B', 'North Fire Station', 'Station-B')");
            }

            // Check if units empty
            ResultSet rsUnit = stmt.executeQuery("SELECT COUNT(*) FROM response_units");
            if (rsUnit.next() && rsUnit.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO response_units (unit_code, unit_type, station_name, current_location, equipment, status) VALUES " +
                        "('FE-01', 'Fire Engine', 'Central Fire Station', 'Station-A', 'HOSE,FOAM,LADDER,WATERPUMP', 'AVAILABLE'), " +
                        "('RV-01', 'Rescue Van', 'Central Fire Station', 'Station-A', 'MEDICAL,CUTTER,RESCUEKIT', 'AVAILABLE'), " +
                        "('FE-02', 'Fire Engine', 'North Fire Station', 'Station-B', 'HOSE,FOAM,WATERPUMP', 'AVAILABLE'), " +
                        "('RV-02', 'Rescue Van', 'North Fire Station', 'Station-B', 'MEDICAL,CUTTER,BOAT', 'AVAILABLE')");
            }

            // Check if incidents empty
            ResultSet rsInc = stmt.executeQuery("SELECT COUNT(*) FROM incidents");
            if (rsInc.next() && rsInc.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO incidents (incident_code, location_name, incident_type, intensity, critical, population_exposure, required_equipment, status) VALUES " +
                        "('INC-101', 'Hospital', 'ICU Electrical Fire', 5, TRUE, 3, 'FOAM', 'PENDING'), " +
                        "('INC-102', 'School', 'Chemistry Lab Explosion', 4, TRUE, 3, 'FOAM', 'PENDING'), " +
                        "('INC-103', 'Industrial-Estate', 'Chemical Storage Tank Blaze', 5, TRUE, 2, 'FOAM', 'PENDING'), " +
                        "('INC-104', 'Market', 'Commercial Complex Fire', 4, FALSE, 3, 'HOSE', 'PENDING'), " +
                        "('INC-105', 'River-Side', 'Boat Overturn & Flash Flood Trapped', 3, FALSE, 2, 'BOAT', 'PENDING'), " +
                        "('INC-106', 'Warehouse', 'Timber Yard Fire', 4, FALSE, 1, 'LADDER', 'PENDING'), " +
                        "('INC-107', 'Highway', 'Multi-Vehicle Pileup with Fuel Spill', 4, TRUE, 2, 'CUTTER', 'PENDING'), " +
                        "('INC-108', 'Residential-Area', 'High-Rise Apartment Balcony Fire', 3, FALSE, 3, 'LADDER', 'PENDING'), " +
                        "('INC-109', 'Bus-Stand', 'Commuter Bus Engine Fire', 3, FALSE, 3, 'HOSE', 'PENDING'), " +
                        "('INC-110', 'College', 'Auditorium Ceiling Collapse & Fire', 3, TRUE, 2, 'MEDICAL', 'PENDING'), " +
                        "('INC-111', 'Park', 'Community Gazebo Fire & Smoke', 2, FALSE, 2, 'HOSE', 'PENDING'), " +
                        "('INC-112', 'Junction-1', 'Traffic Signal Transformer Blast', 3, TRUE, 2, 'WATERPUMP', 'PENDING'), " +
                        "('INC-113', 'Junction-2', 'Fuel Tanker Leak Emergency', 4, TRUE, 2, 'FOAM', 'PENDING'), " +
                        "('INC-114', 'Residential-Area', 'Kitchen Gas Cylinder Leakage', 2, FALSE, 2, 'MEDICAL', 'PENDING'), " +
                        "('INC-115', 'Industrial-Estate', 'Machinery Jam with Trapped Worker', 3, TRUE, 1, 'CUTTER', 'PENDING')");
            }
        } catch (SQLException e) {
            System.err.println("Error seeding default data in MySQL: " + e.getMessage());
        }
    }
}
