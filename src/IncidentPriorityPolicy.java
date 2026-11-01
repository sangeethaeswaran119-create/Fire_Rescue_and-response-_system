import java.util.Comparator;

/**
 * Strategy pattern interface for Incident prioritization.
 * Encapsulates the ranking criteria defined in the project specification.
 */
public interface IncidentPriorityPolicy {

    Comparator<Incident> comparator();

    /**
     * Default ranking implementation:
     * 1. Reported Intensity (descending: 5 down to 1)
     * 2. Critical Infrastructure Flag (descending: true before false)
     * 3. Population Exposure (descending: 3 down to 1)
     * 4. Arrival Order (ascending: FIFO for deterministic tie-breaking)
     */
    class DefaultPolicy implements IncidentPriorityPolicy {
        @Override
        public Comparator<Incident> comparator() {
            return Comparator
                    .comparingInt(Incident::getIntensity).reversed()
                    .thenComparing(Comparator.comparing(Incident::isCriticalInfrastructure).reversed())
                    .thenComparing(Comparator.comparingInt(Incident::getPopulationExposure).reversed())
                    .thenComparingInt(Incident::getArrivalOrder);
        }
    }
}
