package artisynth.core.opensim.components;

import java.awt.Color;

public class Appearance extends OpenSimObject {
   
   private boolean visible;
   private double opacity;
   private Color color;
   private SurfaceProperties surfaceProperties;
   
   public Appearance() {
      visible = true;
      opacity = -1;
      color = null;
      surfaceProperties = null;
   }

   public boolean isVisible () {
      return visible;
   }

   public void setVisible (boolean visible) {
      this.visible = visible;
   }

   public double getOpacity () {
      return opacity;
   }

   public void setOpacity (double opacity) {
      this.opacity = opacity;
   }

   public Color getColor () {
      return color;
   }

   public void setColor (Color color) {
      this.color = color;
   }

   public SurfaceProperties getSurfaceProperties () {
      return surfaceProperties;
   }

   public void setSurfaceProperties (SurfaceProperties surfaceProperties) {
      this.surfaceProperties = surfaceProperties;
      this.surfaceProperties.setParent (this);
   }
   
   @Override
   public Appearance clone () {
      Appearance app = (Appearance)super.clone ();
      if (surfaceProperties != null) {
         app.setSurfaceProperties (surfaceProperties.clone ());
      }
      return app;
   }
   
}
