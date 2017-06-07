package artisynth.core.modelbase;


public interface OrientedTransformableGeometry extends TransformableGeometry {

   /**
    * Flag indicating that transform should flip orientation
    */
   public static final int OTG_AUTOFLIP = 0x0100;
   
   public void flipOrientation(int flags);
   
}
