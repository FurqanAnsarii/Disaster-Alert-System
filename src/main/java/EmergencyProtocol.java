/**
 * Abstraction: Using an Interface to define standard emergency behaviors.
 * All disaster types must implement this protocol.
 */
public interface EmergencyProtocol {
    String generateAlert();
    String getEvacuationPlan();
}
