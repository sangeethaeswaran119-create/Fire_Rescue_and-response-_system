import java.util.Set;

/**
 * Concrete subclass of ResponseUnit specialized in fire suppression.
 * Carries high-capacity pumps, hoses, foam equipment, and ladders.
 */
public class FireEngine extends ResponseUnit {

    public FireEngine(int id, String unitCode, String stationName,
                      String currentLocation, boolean available, Set<String> equipment) {
        super(id, unitCode, "Fire Engine", stationName, currentLocation, available, equipment);
    }

    public FireEngine(String unitCode, String stationName, String currentLocation) {
        super(unitCode, "Fire Engine", stationName, currentLocation);
    }

    @Override
    public boolean supportsRequirement(String requirement) {
        if (requirement == null || requirement.isBlank()) return false;
        String req = requirement.trim().toUpperCase();
        // Fire engines specialize in water/foam suppression and elevated rescue
        return equipment.contains(req);
    }
}
