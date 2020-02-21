package artisynth.core.opensim.components;

import java.awt.Color;

import maspack.render.ColorMapProps;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;

public class Appearance extends OpenSimObject {
   
   private boolean visible;
   private boolean visibleExplicit;
   private double opacity;
   private Color color;
   private SurfaceProperties surfaceProperties;
   
   public Appearance() {
      visible = true;
      visibleExplicit = false;
      opacity = -1;
      color = null;
      surfaceProperties = null;
   }

   public boolean isVisible () {
      return visible;
   }

   public void setVisible (boolean visible) {
      this.visible = visible;
      this.visibleExplicit = true;
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
   
   public void updateRenderProps(RenderProps rprops) {
      if (visibleExplicit) {
         rprops.setVisible (visible);
      }
      if (opacity >= 0) {
         rprops.setAlpha (opacity);
      }
      if (color != null) {
         rprops.setFaceColor (color);
         rprops.setLineColor (color);
         rprops.setPointColor (color);
      }
      if (surfaceProperties != null) {
         SurfaceProperties.Representation type = surfaceProperties.getRepresentation ();
         if (type != null) {
            switch (type) {
               case SHADED:
                  rprops.setShading (Shading.SMOOTH);
                  break;
               case POINTS:
               case WIRE:
                  rprops.setDrawEdges (true);
                  rprops.setFaceStyle (FaceStyle.NONE);
                  break;               
            }
         }
         String texture = surfaceProperties.getTexture ();
         if (texture != null) {
            ColorMapProps tprops = new ColorMapProps ();
            tprops.setFileName (texture);
            rprops.setColorMap (tprops);
         }
      }
   }
   
   public RenderProps createRenderProps() {
      RenderProps rprops = new RenderProps();
      updateRenderProps(rprops);
      return rprops;
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
