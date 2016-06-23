package artisynth.demos.test;

import artisynth.core.gui.ControlPanel;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.BumpMapProps;
import maspack.render.NormalMapProps;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.render.ColorMapProps;
import maspack.util.PathFinder;

public class MappingDemo extends RootModel {

   public static class DrawMappings extends RenderableComponentBase {

      ColorMapProps myTextureProps = null;
      NormalMapProps myNormalProps = null;
      BumpMapProps myBumpProps = null;

      boolean myTextureMapEnabled = true;
      boolean myNormalMapEnabled = true;
      boolean myBumpMapEnabled = true;

      RenderObject myRenderObj;

      public static PropertyList myProps =
         new PropertyList (DrawMappings.class, RenderableComponentBase.class);

      static {
         myProps.add ("renderProps", "render properties", null);
         myProps.add ("textureMap", "texture map enabled", true);
         myProps.add ("normalMap", "normal map enabled", true);
         myProps.add ("bumpMap", "bump map enabled", true);
      }

      public void setTextureMap (boolean enable) {
         myTextureMapEnabled = enable;
      }

      public boolean getTextureMap () {
         return myTextureMapEnabled;
      }

      public void setNormalMap (boolean enable) {
         myNormalMapEnabled = enable;
      }

      public boolean getNormalMap () {
         return myNormalMapEnabled;
      }

      public void setBumpMap (boolean enable) {
         myBumpMapEnabled = enable;
      }

      public boolean getBumpMap () {
         return myBumpMapEnabled;
      }

      public PropertyList getAllPropertyInfo() {
         return myProps;
      }

      public DrawMappings() {
         setRenderProps (createRenderProps());
         createTextureProps();
         createNormalProps();
         createBumpProps();
      }

      public void updateBounds (Point3d min, Vector3d max) {
         (new Point3d(-3, -1, 0)).updateBounds (min, max);
         (new Point3d( 3,  1, 0)).updateBounds (min, max);
      }

      String getDataFolder() {
         return PathFinder.expand ("${srcdir MappingDemo}/data");
      }

      public void createTextureProps() {
         // create texture mapping
         myTextureProps = new ColorMapProps ();
         myTextureProps.setFileName (getDataFolder()+"/texture_map.jpg");
         myTextureProps.setEnabled (true);         
      }

      public void createNormalProps() {
         // create normal mapping
         myNormalProps = new NormalMapProps ();
         myNormalProps.setFileName (getDataFolder()+"/foil_normal_map.png");
         myNormalProps.setScaling (1f);
         myNormalProps.setEnabled (true);         
      }

      public void createBumpProps() {
         // create normal mapping
         myBumpProps = new BumpMapProps ();
         myBumpProps.setFileName (getDataFolder()+"/egyptian_friz.png");
         myBumpProps.setScaling (2.5f);
         myBumpProps.setEnabled (true);         
      }

      public void prerender (RenderList list) {

         // create render object if necessary. This is a simple 6 x 2 plane,
         // centered on the origin, created from two triangles, with texture
         // coordinates assigned to each vertex.
         if (myRenderObj == null) {
            RenderObject robj = new RenderObject();

            robj.addNormal (0, 0, 1f);
            robj.addTextureCoord (0, 0);
            robj.vertex (-3f, -1f, 0f);
            robj.addTextureCoord (1, 0);
            robj.vertex ( 3f, -1f, 0);
            robj.addTextureCoord (1, 1);
            robj.vertex ( 3f,  1f, 0);
            robj.addTextureCoord (0, 1);
            robj.vertex (-3f,  1f, 0);

            robj.addTriangle (0, 1, 2);
            robj.addTriangle (0, 2, 3);

            myRenderObj = robj;
         }
      }           

      public void render (Renderer renderer, int flags) {

         float[] greenGold = new float[] {0.61f, 0.77f, 0.12f};
         float[] yellowGold = new float[] {1f, 0.44f, 0f};

         renderer.setShininess (20);                       // increase shininess
         renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK); // see both sides
         renderer.setColor (greenGold);                    // base color
         renderer.setSpecular (yellowGold);                // reflected color

         renderer.setShading (Shading.SMOOTH);
         // set texture, normal and bump mappings if their properties are

         if (myTextureMapEnabled) {
            renderer.setColorMap (myTextureProps); 
         }
         if (myNormalMapEnabled) {
            renderer.setNormalMap (myNormalProps); 
         }
         if (myBumpMapEnabled) {
            renderer.setBumpMap (myBumpProps); 
         }

         if (false) {
            
            renderer.beginDraw (DrawMode.TRIANGLES);

            renderer.setNormal (0, 0, 1f);

            // first triangle
            renderer.setTextureCoord (0, 0);
            renderer.addVertex (-3f, -1f, 0f);
            renderer.setTextureCoord (1, 0);
            renderer.addVertex ( 3f, -1f, 0);
            renderer.setTextureCoord (1, 1);
            renderer.addVertex ( 3f,  1f, 0);

            // second triangle
            renderer.setTextureCoord (0, 0);
            renderer.addVertex (-3f, -1f, 0f);
            renderer.setTextureCoord (1, 1);
            renderer.addVertex ( 3f,  1f, 0);
            renderer.setTextureCoord (0, 1);
            renderer.addVertex (-3f,  1f, 0);

            renderer.endDraw();
         }
         else {
            renderer.drawTriangles (myRenderObj);
         }
         

         renderer.setColorMap (null);
         renderer.setNormalMap (null);
         renderer.setBumpMap (null);
      }

      public ControlPanel createControlPanel() {
         ControlPanel panel = new ControlPanel();
         panel.addWidget (this, "textureMap");
         panel.addWidget (this, "normalMap");
         panel.addWidget (this, "bumpMap");
         return panel;
      }

   }

   public void build (String[] args) {
      DrawMappings r = new DrawMappings();
      addRenderable (r);
      addControlPanel (r.createControlPanel());      
   }
   
   @Override
   public void attach (DriverInterface driver) {
      super.attach (driver);
      driver.getViewer ().setAxialView (AxisAlignedRotation.X_Y);
   }
}
