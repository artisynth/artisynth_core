package artisynth.core.inverse;

/**
 * Base implementation for a QPConstraintTerm.
 */
public abstract class QPConstraintTermBase extends QPTermBase 
   implements QPConstraintTerm {

   public Type getType() {
      return Type.INEQUALITY;
   }
   
   public QPConstraintTermBase() {
      this(DEFAULT_WEIGHT);
   }
   
   public QPConstraintTermBase(double weight) {
      super (weight);
   }
}
