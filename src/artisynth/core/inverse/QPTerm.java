package artisynth.core.inverse;

import artisynth.core.modelbase.*;

/**
 * Base interface for the different terms used by the QP solver in the tracking
 * controller.
 */
public interface QPTerm extends ModelComponent {
   
   public enum Type {
      /**
       * Cost term
       */
      COST,
      
      /**
       * Inequality term
       */
      INEQUALITY,

      /**
       * Equality term
       */
      EQUALITY,
   };

   /**
    * Queries whether this is a cost, inequality, or equality term.
    *
    * @return type for this term.
    */
   public Type getType();
   
   /**
    * Sets the weight for this term.
    * 
    * @param w weight for this term
    */
   public void setWeight (double w);
   
   /**
    * Queries the weight for this term.
    * 
    * @return weight for this term
    */
   public double getWeight();
   
   /**
    * Returns the controller using this term. 
    * 
    * @return controller using this term
    */
   public TrackingController getController();
   
   /**
    * Queries the enabled status of this term. Terms that are not enabled are
    * ignored.
    *
    * @return enabled status of this term
    */
   public boolean isEnabled();
}
