import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a physical Fire Station in Sudar town.
 * Demonstrates OOP Composition: a station contains multiple response units.
 */
public class FireStation {

    private int id;
    private String stationCode;
    private String stationName;
    private String locationName;
    private final List<ResponseUnit> units;

    public FireStation(int id, String stationCode, String stationName, String locationName) {
        this.id = id;
        this.stationCode = stationCode;
        this.stationName = stationName;
        this.locationName = locationName;
        this.units = new ArrayList<>();
    }

    public FireStation(String stationCode, String stationName, String locationName) {
        this(0, stationCode, stationName, locationName);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStationCode() { return stationCode; }
    public String getStationName() { return stationName; }
    public String getLocationName() { return locationName; }

    public void addUnit(ResponseUnit unit) {
        if (unit != null && !units.contains(unit)) {
            units.add(unit);
        }
    }

    public List<ResponseUnit> getUnits() {
        return Collections.unmodifiableList(units);
    }

    public List<ResponseUnit> getAvailableUnits() {
        List<ResponseUnit> list = new ArrayList<>();
        for (ResponseUnit u : units) {
            if (u.isAvailable()) list.add(u);
        }
        return list;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s at %s (Total Units: %d)",
                stationCode, stationName, locationName, units.size());
    }
}
