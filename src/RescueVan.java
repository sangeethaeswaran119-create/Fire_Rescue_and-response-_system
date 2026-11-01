import java.util.Set;

/**
 * Concrete subclass of ResponseUnit specialized in technical rescue,
 * hydraulic cutting (vehicle extrication), medical support, and flood/boat rescue.
 */
public class RescueVan extends ResponseUnit {

    public RescueVan(int id, String unitCode, String stationName,
                     String currentLocation, boolean available, Set<String> equipment) {
        super(id, unitCode, "Rescue Van", stationName, currentLocation, available, equipment);
    }

    public RescueVan(String unitCode, String stationName, String currentLocation) {
        super(unitCode, "Rescue Van", stationName, currentLocation);
    }

    @Override
    public boolean supportsRequirement(String requirement) {
        if (requirement == null || requirement.isBlank()) return false;
        String req = requirement.trim().toUpperCase();
        // Rescue vans specialize in medical, cutters, rescue kits, and boats
        return equipment.contains(req);
    }
}
