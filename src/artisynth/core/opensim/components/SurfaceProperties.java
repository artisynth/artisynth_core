package artisynth.core.opensim.components;

public class SurfaceProperties extends OpenSimObject {
   
   public static enum Representation {
      POINTS(1), WIRE(2), SHADED(3);
      
      private int id;
      private Representation(int id) {
         this.id = id;
      }
      
      public static Representation get(int id) {
         for (Representation rep : values()) {
            if (rep.id == id) {
               return rep;
            }
         }
         return null;
      }
   }
   
   Representation representation;  // (1:Points, 2:Wire, 3:Shaded) 
   String texture;      // rendering hint
   
   public SurfaceProperties() {
      representation = null;
      texture = null;
   }
   
   public Representation getRepresentation() {
      return representation;
   }
   
   public void setRepresentation(Representation rep) {
      representation = rep;
   }
   
   public String getTexture() {
      return texture;
   }
   
   public void setTexture(String tex) {
      texture = tex;
   }
   
   @Override
   public SurfaceProperties clone () {
      return (SurfaceProperties)super.clone ();
   }
   

}
