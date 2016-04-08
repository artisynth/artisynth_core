package maspack.render.GL.test;

import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JPanel;

import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.util.ArtisynthPath;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.BumpMapProps;
import maspack.render.TextureMapProps;
import maspack.render.NormalMapProps;
import maspack.render.RenderProps;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.widgets.LabeledComponentBase;
import maspack.widgets.PropertyWidget;

public class TextureEgyptianTest extends GL2vsGL3Tester {
   
   @Override
   protected void addContent (MultiViewer mv) {
      
      JFrame frame = new JFrame();
      JPanel controls = new JPanel ();
      frame.add (controls);

      PolygonalMesh plane = MeshFactory.createPlane (4000, 400, 32, 8);
      
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
      
      TextureMapProps dprops = new TextureMapProps ();
      dprops.setFileName (ArtisynthPath.getSrcRelativePath (this, "/data/specular_map.jpg"));
      dprops.setTextureColorMixing (ColorMixing.MODULATE);
      dprops.setEnabled (true);
      
      NormalMapProps normalProps = new NormalMapProps ();
      normalProps.setFileName (ArtisynthPath.getSrcRelativePath (this, "/data/foil_normal_map2.png"));
      normalProps.setNormalScale (0.3f);
      normalProps.setEnabled (true);
      
      BumpMapProps bumpProps = new BumpMapProps ();
      bumpProps.setFileName (ArtisynthPath.getSrcRelativePath (this, "/data/egyptian_friz_2.png"));
      bumpProps.setBumpScale (2.5f);
      bumpProps.setEnabled (true);
      
      rprops.setTextureMapProps (dprops);
      rprops.setNormalMapProps (normalProps);
      rprops.setBumpMapProps (bumpProps);
      
      FixedMeshBody fm = new FixedMeshBody(plane);
      fm.setRenderProps(rprops);
      mv.addRenderable (fm);
      
      mv.autoFitViewers ();
      
      LabeledComponentBase base = PropertyWidget.create ("Color texture", fm.getRenderProps ().getTextureMapProps (), "enabled");
      controls.add(base);
      base = PropertyWidget.create ("Normal map", fm.getRenderProps ().getNormalMapProps (), "enabled");
      controls.add(base);
      base = PropertyWidget.create ("Bump map", fm.getRenderProps ().getBumpMapProps (), "enabled");
      controls.add(base);
      
      base = PropertyWidget.create ("Specular", fm.getRenderProps ().getTextureMapProps (), "specularColoring");
      controls.add (base);
      
      base = PropertyWidget.create ("Bump map scale", fm.getRenderProps ().getBumpMapProps (), "bumpScale");
      controls.add (base);
      
      base = PropertyWidget.create ("Normal map scale", fm.getRenderProps ().getNormalMapProps (), "normalScale");
      controls.add (base);
      
      frame.pack ();
      frame.setVisible (true);
      
   }
   
   public static void main (String[] args) {
      TextureEgyptianTest tester = new TextureEgyptianTest();
      tester.run ();
   }

}
