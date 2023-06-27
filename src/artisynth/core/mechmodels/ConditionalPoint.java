package artisynth.core.mechmodels;

/**
 * Describes a point, usually belonging to a MultiPointSpring, which is only
 * active for particular model configugration.
 */
public interface ConditionalPoint {
   
   public boolean isPointActive();

}
