package artisynth.core.opensim.components;

import java.awt.Color;

import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;

public abstract class VisibleBase extends OpenSimObject {
   
   public static enum DisplayPreference {
      NONE(0), WIRE_FRAME(1), SOLID_FILL(2), FLAT_SHADED(3), GOURAUD_SHADED(4);
      
      public final int id;
      public static final DisplayPreference DEFAULT = FLAT_SHADED;
      
      private DisplayPreference(int id) {
         this.id = id;
      }
      
      public static DisplayPreference get(int id) {
         for (DisplayPreference pref : values ()) {
            if (pref.id == id) {
               return pref;
            }
         }
         return null;
      }
   };
   
   private Color color;
   private DisplayPreference display_preference;
   private double opacity;

   public VisibleBase() {
      color = null;
      display_preference = null;
      opacity = -1.0;
   }
   
   public void setColor(Color c) {
      color = c;
   }
   
   public Color getColor () {
      return color;
   }

   public void setDisplayPreference(DisplayPreference dp) {
      display_preference = dp;
   }
   
   public DisplayPreference getDisplayPreference () {
      return display_preference;
   }
   
   public void setOpacity(double a) {
      opacity = a;
   }

   public double getOpacity () {
      return opacity;
   }
   
   @Override
   public VisibleBase clone () {
      return (VisibleBase)super.clone ();
   }
   
   
   /**
    * Updates the render properties with any explicit values set in this object
    * @param rprops
    */
   protected void updateRenderProps(RenderProps rprops) {
     
      if (opacity >= 0) {
         rprops.setAlpha (opacity);
      }
      if (color != null) {
         rprops.setFaceColor (color);
      }
      
      if (display_preference != null) {
         switch(display_preference) {
            case FLAT_SHADED:
               rprops.setVisible (true);
               rprops.setShading (Shading.FLAT);
               break;
            case GOURAUD_SHADED:
               rprops.setVisible (true);
               rprops.setShading (Shading.SMOOTH);
               break;
            case NONE:
               rprops.setVisible (false);
               break;
            case SOLID_FILL:
               rprops.setVisible (true);
               rprops.setFaceStyle (FaceStyle.FRONT_AND_BACK);
               rprops.setShading (Shading.SMOOTH);
               break;
            case WIRE_FRAME:
               rprops.setDrawEdges (true);
               rprops.setFaceStyle (FaceStyle.NONE);
               break;
            default:
               break;
            
         }
      }
   }
   
   /**
    * Gets render properties from this object, ignoring the mesh details.  If no
    * render props are explicity set, will return null
    * 
    * @return new render props
    */
   public RenderProps createRenderProps() {
      RenderProps rprops = new RenderProps();
      updateRenderProps (rprops);
      return rprops;
   }

}
