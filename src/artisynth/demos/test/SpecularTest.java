package artisynth.demos.test;

import java.awt.Color;

import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.renderables.LightComponent;
import artisynth.core.workspace.DriverInterface;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.RigidTransform3d;
import maspack.render.Light;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.render.GL.GLViewer;
import maspack.render.Light.LightType;

public class SpecularTest extends MeshTestBase {


   FixedMeshBody createMesh (MechModel mech, double z) {
      PolygonalMesh mesh = 
         MeshFactory.createRectangle (3, 1, 20, 20, /*texture=*/true);
      FixedMeshBody body = new FixedMeshBody (mesh);
      mech.addMeshBody (body);
      body.setPose (new RigidTransform3d (0, 0, z, 0, 0, Math.PI/2));

      return body;
   }

   public void build (String[] args) {

      MechModel mech = new MechModel ("mech");
      addModel (mech);

      RenderProps.setShininess (mech, 128);
      RenderProps.setFaceStyle (mech, FaceStyle.FRONT_AND_BACK);
      RenderProps.setFaceColor (mech, Color.GRAY.darker ().darker ());
      RenderProps.setSpecular (mech, Color.WHITE);

      // mech.getRenderProps().setColorMap (createTextureProps());
      // mech.getRenderProps().setNormalMap (createNormalProps());
      // mech.getRenderProps().setBumpMap (createBumpProps());

      FixedMeshBody body0 = createMesh (mech, 0);
      //FixedMeshBody body1 = createMesh (mech, 2);


   }
   
   @Override
   public void attach (DriverInterface driver) {
      super.attach (driver);
      
      GLViewer viewer = driver.getViewer ();
     
      for (int i=viewer.numLights (); i-->0;) {
         viewer.removeLight (i);
      }
      
      Light light = new Light ();
      light.setAmbient (0, 0, 0, 0);
      light.setDiffuse (0, 0, 0, 0);
      light.setSpecular (1, 1, 1, 1);
      light.setPosition (0, 0, 1);
      light.setDirection (0, 0, -1);
      light.setType (LightType.POINT);
      
      viewer.addLight (light);
      
      LightComponent lc = new LightComponent (light);
      ComponentList<LightComponent> lights = new ComponentList<> (LightComponent.class, "lights");
      lights.add (lc);
      add(lights);
   }
   
   @Override
   public void detach (DriverInterface driver) {
      super.detach (driver);
      
      GLViewer viewer = driver.getViewer ();
      viewer.setDefaultLights ();
   }
}
