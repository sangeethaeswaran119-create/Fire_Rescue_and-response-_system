import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class representing an emergency response vehicle.
 * Demonstrates OOP Abstraction and Encapsulation:
 * - availability state protected through assignIncident() and completeMission()
 * - equipment capabilities stored in a HashSet for fast O(1) compatibility checks.
 */
public abstract class ResponseUnit {

    protected int id;
    protected String unitCode;
    protected String unitType;
    protected String stationName;
    protected String currentLocation;
    protected boolean available;
    protected Set<String> equipment;

    public ResponseUnit(int id, String unitCode, String unitType, String stationName,
                        String currentLocation, boolean available, Set<String> equipment) {
        this.id = id;
        this.unitCode = unitCode;
        this.unitType = unitType;
        this.stationName = stationName;
        this.currentLocation = currentLocation;
        this.available = available;
        this.equipment = equipment != null ? new HashSet<>(equipment) : new HashSet<>();
    }

    public ResponseUnit(String unitCode, String unitType, String stationName, String currentLocation) {
        this(0, unitCode, unitType, stationName, currentLocation, true, new HashSet<>());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUnitCode() { return unitCode; }
    public String getUnitType() { return unitType; }
    public String getStationName() { return stationName; }
    public String getCurrentLocation() { return currentLocation; }
    public boolean isAvailable() { return available; }
    public Set<String> getEquipment() { return Collections.unmodifiableSet(equipment); }

    public void addEquipment(String item) {
        if (item != null && !item.isBlank()) {
            this.equipment.add(item.trim().toUpperCase());
        }
    }

    public boolean hasEquipment(String item) {
        return item != null && equipment.contains(item.trim().toUpperCase());
    }

    /**
     * Polymorphic method to verify if this unit supports a required equipment.
     * Concrete subclasses provide specialized validation.
     */
    public abstract boolean supportsRequirement(String requirement);

    /**
     * Assigns this unit to an incident, marking it BUSY.
     */
    public void assignIncident() {
        this.available = false;
    }

    /**
     * Completes active mission, marks unit AVAILABLE, and updates position.
     */
    public void completeMission(String newLocation) {
        this.available = true;
        if (newLocation != null && !newLocation.isBlank()) {
            this.currentLocation = newLocation;
        }
    }

    public void updateLocation(String newLocation) {
        if (newLocation != null && !newLocation.isBlank()) {
            this.currentLocation = newLocation;
        }
    }

    public String getEquipmentCsv() {
        return String.join(",", equipment);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) @ %s | Avail: %b | Eq: %s",
                unitCode, unitType, stationName, currentLocation, available, equipment);
    }
}
