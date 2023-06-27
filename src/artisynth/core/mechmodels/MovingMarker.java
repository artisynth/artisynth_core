package artisynth.core.mechmodels;

/**
 * Describes a marker point whose location with respect to its master
 * components varies with the model configration.
 */
public interface MovingMarker {
   
   /**
    * Updates the location of this marker with respect to the dynamic 
    * components it is attached to.
    */  
   public void updateMarkerLocation();

}
