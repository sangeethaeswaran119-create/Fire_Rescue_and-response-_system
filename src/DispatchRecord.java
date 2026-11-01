import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Encapsulates an active or past mission dispatch record.
 * Tracks assigned unit, target incident, calculated Dijkstra route, travel time, and status.
 */
public class DispatchRecord {

    private int id;
    private int incidentId;
    private String incidentCode;
    private String unitCode;
    private String startLocation;
    private String targetLocation;
    private String routeText;
    private int travelTimeMins;
    private String status; // DISPATCHED, COMPLETED
    private String createdAt;
    private long dispatchedTimestamp;

    public DispatchRecord(int id, int incidentId, String incidentCode, String unitCode,
                          String startLocation, String targetLocation, String routeText,
                          int travelTimeMins, String status, String createdAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.incidentCode = incidentCode;
        this.unitCode = unitCode;
        this.startLocation = startLocation;
        this.targetLocation = targetLocation;
        this.routeText = routeText;
        this.travelTimeMins = travelTimeMins;
        this.status = status == null ? "DISPATCHED" : status;
        this.createdAt = createdAt != null ? createdAt :
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.dispatchedTimestamp = System.currentTimeMillis();
    }

    public DispatchRecord(int incidentId, String incidentCode, String unitCode,
                          String startLocation, String targetLocation, String routeText,
                          int travelTimeMins) {
        this(0, incidentId, incidentCode, unitCode, startLocation, targetLocation,
                routeText, travelTimeMins, "DISPATCHED", null);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIncidentId() { return incidentId; }
    public String getIncidentCode() { return incidentCode; }
    public String getUnitCode() { return unitCode; }
    public String getStartLocation() { return startLocation; }
    public String getTargetLocation() { return targetLocation; }
    public String getRouteText() { return routeText; }
    public String getRoute() { return routeText; }
    public int getTravelTimeMins() { return travelTimeMins; }
    public int getTravelTimeMinutes() { return travelTimeMins; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public long getDispatchedTimestamp() { return dispatchedTimestamp; }

    @Override
    public String toString() {
        return String.format("Mission #%d: [%s -> %s] via %s (%d mins) | %s",
                incidentId, unitCode, targetLocation, routeText, travelTimeMins, status);
    }
}
