package maspack.test.GL;

import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JPanel;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.BumpMapProps;
import maspack.render.IsRenderable;
import maspack.render.ColorMapProps;
import maspack.render.NormalMapProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.widgets.LabeledComponentBase;
import maspack.widgets.PropertyWidget;
import maspack.util.PathFinder;

public class TextureEgyptianTest extends GL3Tester {
   
   @Override
   protected void addContent (MultiViewer mv) {
      
      JFrame frame = new JFrame();
      JPanel controls = new JPanel ();
      frame.add (controls);

      PolygonalMesh plane = 
         MeshFactory.createRectangle (4000, 400, 32, 8, /*texture=*/true);
      
      RenderProps rprops = plane.getRenderProps ();
      if (rprops == null) {
         rprops = new RenderProps();
      }
      
      rprops.setShading (Shading.SMOOTH);
      rprops.setShininess (20);
      rprops.setFaceColor (new Color(155,196,30));
      rprops.setSpecular (new Color(255, 113, 0));
      rprops.setFaceStyle (FaceStyle.FRONT_AND_BACK);
      plane.transform (new RigidTransform3d(Vector3d.ZERO, AxisAngle.ROT_X_90));

      String srcDir = PathFinder.findSourceDir (this);
      
      ColorMapProps dprops = new ColorMapProps ();
      dprops.setFileName (srcDir + "/data/specular_map.jpg");
      dprops.setColorMixing (ColorMixing.MODULATE);
      dprops.setEnabled (true);
      
      NormalMapProps normalProps = new NormalMapProps ();
      normalProps.setFileName (srcDir + "/data/foil_normal_map2.png");
      normalProps.setScaling (0.3f);
      normalProps.setEnabled (true);
      
      BumpMapProps bumpProps = new BumpMapProps ();
      bumpProps.setFileName (srcDir + "/data/egyptian_friz_2.png");
      bumpProps.setScaling (2.5f);
      bumpProps.setEnabled (true);
      
      rprops.setColorMap (dprops);
      rprops.setNormalMap (normalProps);
      rprops.setBumpMap (bumpProps);
      
      //FixedMeshBody fm = new FixedMeshBody(plane);
      //fm.setRenderProps(rprops);
      
      mv.addRenderable (plane);
      
      if (false) {
         
      mv.addRenderable (new IsRenderable() {
         @Override
         public void updateBounds (Vector3d pmin, Vector3d pmax) {
            Point3d.X_UNIT.updateBounds (pmin, pmax);
            Point3d.Y_UNIT.updateBounds (pmin, pmax);
            Point3d.Z_UNIT.updateBounds (pmin, pmax);
            
            Point3d.NEG_X_UNIT.updateBounds (pmin, pmax);
            Point3d.NEG_Y_UNIT.updateBounds (pmin, pmax);
            Point3d.NEG_Z_UNIT.updateBounds (pmin, pmax);
         }
         
         @Override
         public void render (Renderer renderer, int flags) {
            renderer.setColor (Color.CYAN);
            renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK);
            renderer.drawSphere (Point3d.ZERO, 1);
         }
         
         @Override
         public void prerender (RenderList list) {
            // TODO Auto-generated method stub
            
         }
         
         @Override
         public int getRenderHints () {
            // TODO Auto-generated method stub
            return 0;
         }
      });
      }
      

      mv.autoFitViewers ();
      
      LabeledComponentBase base = PropertyWidget.create ("Color texture", rprops.getColorMap (), "enabled");
      controls.add(base);
      base = PropertyWidget.create ("Normal map", rprops.getNormalMap (), "enabled");
      controls.add(base);
      base = PropertyWidget.create ("Bump map", rprops.getBumpMap (), "enabled");
      controls.add(base);
      
      base = PropertyWidget.create ("Specular", rprops.getColorMap (), "specularColoring");
      controls.add (base);
      
      base = PropertyWidget.create ("Bump map scale", rprops.getBumpMap (), "scaling");
      controls.add (base);
      
      base = PropertyWidget.create ("Normal map scale", rprops.getNormalMap (), "scaling");
      controls.add (base);
      
      
      
      frame.pack ();
      frame.setVisible (true);
      
   }
   
   public static void main (String[] args) {
      TextureEgyptianTest tester = new TextureEgyptianTest();
      tester.run ();
   }

}
