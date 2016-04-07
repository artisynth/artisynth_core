package maspack.render.GL.test;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;

import artisynth.core.mechmodels.FixedMeshBody;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.io.WavefrontReader;
import maspack.render.RenderProps;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.Shading;
import maspack.widgets.LabeledComponentBase;
import maspack.widgets.PropertyWidget;

public class TextureHeartTest extends GL2vsGL3Tester {
   
   @Override
   protected void addContent (MultiViewer mv) {
   
      JFrame frame = new JFrame();
      JPanel controls = new JPanel ();
      frame.add (controls);
      
      String heartObjFilename = "src/maspack/render/GL/test/data/heart/HumanHeart.obj";
      WavefrontReader reader = null;
      try {
         reader = new WavefrontReader (new File(heartObjFilename));
         PolygonalMesh mesh = reader.readMesh ();
         
         RenderProps rprops = mesh.getRenderProps ();
         if (rprops == null) {
            rprops = new RenderProps();
         }
         rprops.setShading (Shading.SMOOTH);
         rprops.setFaceColor (new Color(0.8f,0.8f,0.8f));
         rprops.getTextureMapProps ().setTextureColorMixing (ColorMixing.MODULATE);
         rprops.setSpecular (new Color(0.4f, 0.4f, 0.4f));
         rprops.getBumpMapProps ().setBumpScale (0.5f);
         rprops.setShininess (128);
         mesh.setRenderProps(rprops);
         
         FixedMeshBody fm = new FixedMeshBody (mesh);
         fm.setRenderProps (mesh.getRenderProps ());
         LabeledComponentBase base = PropertyWidget.create ("Map", fm.getRenderProps ().getTextureMapProps (), "enabled");
         controls.add(base);
         base = PropertyWidget.create ("Bump map", fm.getRenderProps ().getBumpMapProps (), "enabled");
         controls.add(base);
         base = PropertyWidget.create ("Lighting", fm.getRenderProps (), "shading");
         controls.add(base);
         
         base = PropertyWidget.create ("Specular", fm.getRenderProps ().getTextureMapProps (), "specularColoring");
         controls.add (base);
         
         base = PropertyWidget.create ("Bump map scale", fm.getRenderProps ().getBumpMapProps (), "bumpScale");
         controls.add (base);
         
         mv.addRenderable (fm);
      } catch (IOException e) {
         e.printStackTrace();
      }
      
      if (reader != null) {
         reader.close();
      }
      
      
      mv.autoFitViewers ();
      frame.pack ();
      frame.setVisible (true);
      
   }
   
   public static void main (String[] args) {
      TextureHeartTest tester = new TextureHeartTest();
      tester.run ();
   }

}
