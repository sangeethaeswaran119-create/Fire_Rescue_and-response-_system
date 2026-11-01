import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class Incident {

    private int id;
    private String incidentCode;
    private String location;
    private String type;
    private int intensity;             
    private boolean criticalInfrastructure; 
    private int populationExposure;    
    private String requiredEquipment;  
    private int arrivalOrder;          
    private String status;             
    private String createdAt;

    public Incident(int id, String incidentCode, String location, String type,
                    int intensity, boolean criticalInfrastructure,
                    int populationExposure, String requiredEquipment,
                    int arrivalOrder, String status, String createdAt) {
        this.id = id;
        this.incidentCode = incidentCode;
        this.location = location;
        this.type = type;
        this.intensity = Math.max(1, Math.min(5, intensity));
        this.criticalInfrastructure = criticalInfrastructure;
        this.populationExposure = Math.max(1, Math.min(3, populationExposure));
        this.requiredEquipment = requiredEquipment == null ? "HOSE" : requiredEquipment.trim().toUpperCase();
        this.arrivalOrder = arrivalOrder;
        this.status = status == null ? "PENDING" : status;
        this.createdAt = createdAt != null ? createdAt :
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public Incident(String incidentCode, String location, String type,
                    int intensity, boolean criticalInfrastructure,
                    int populationExposure, String requiredEquipment, int arrivalOrder) {
        this(0, incidentCode, location, type, intensity, criticalInfrastructure,
                populationExposure, requiredEquipment, arrivalOrder, "PENDING", null);
    }

    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getIncidentCode() { return incidentCode; }
    public void setIncidentCode(String incidentCode) { this.incidentCode = incidentCode; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getIntensity() { return intensity; }
    public void setIntensity(int intensity) { this.intensity = intensity; }

    public boolean isCriticalInfrastructure() { return criticalInfrastructure; }
    public void setCriticalInfrastructure(boolean criticalInfrastructure) {
        this.criticalInfrastructure = criticalInfrastructure;
    }

    public int getPopulationExposure() { return populationExposure; }
    public void setPopulationExposure(int populationExposure) {
        this.populationExposure = populationExposure;
    }

    public String getRequiredEquipment() { return requiredEquipment; }
    public void setRequiredEquipment(String requiredEquipment) {
        this.requiredEquipment = requiredEquipment.toUpperCase();
    }

    public int getArrivalOrder() { return arrivalOrder; }
    public void setArrivalOrder(int arrivalOrder) { this.arrivalOrder = arrivalOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /**
     * Priority score calculated based on urgency criteria:
     * - Fire Intensity (10x multiplier)
     * - Critical Infrastructure Flag (+25 bonus)
     * - Population Exposure (5x multiplier)
     *
     * Higher score = higher priority in the PriorityQueue.
     */
    public int calculatePriority() {
        int score = (intensity * 10);
        if (criticalInfrastructure) {
            score += 25;
        }
        score += (populationExposure * 5);
        return score;
    }

    public int getPriority() {
        return calculatePriority();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s at %s | Int: %d | Crit: %b | Pop: %d | Eq: %s | Prio: %d | %s",
                incidentCode, type, location, intensity, criticalInfrastructure,
                populationExposure, requiredEquipment, calculatePriority(), status);
    }
}
