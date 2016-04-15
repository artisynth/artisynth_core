package artisynth.demos.test;

import java.io.*;
import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.render.*;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.properties.*;

public class MappingTest extends MeshTestBase {

   static String datafolder = PathFinder.expand ("${srcdir MappingTest}/data/");

   FixedMeshBody createMesh (MechModel mech, double z) {
      PolygonalMesh mesh = MeshFactory.createPlane (3, 1, 10, 10);
      FixedMeshBody body = new FixedMeshBody (mesh);
      mech.addMeshBody (body);
      body.setPose (new RigidTransform3d (0, 0, z, 0, 0, Math.PI/2));

      body.getRenderProps().setTextureMapProps (new TextureMapProps());
      body.getRenderProps().setNormalMapProps (new NormalMapProps());
      body.getRenderProps().setBumpMapProps (new BumpMapProps());

      return body;
   }

   public TextureMapProps createTextureProps() {
      // create texture mapping
      TextureMapProps props = new TextureMapProps ();
      props.setFileName (datafolder+"texture_map.jpg");
      props.setEnabled (true);         
      return props;
   }

   public NormalMapProps createNormalProps() {
      // create normal mapping
      NormalMapProps props = new NormalMapProps ();
      props.setFileName (datafolder+"foil_normal_map.png");
      props.setNormalScale (1f);
      props.setEnabled (true);         
      return props;
   }

   public BumpMapProps createBumpProps() {
      // create normal mapping
      BumpMapProps props = new BumpMapProps ();
      props.setFileName (datafolder+"egyptian_friz.png");
      props.setBumpScale (2.5f);
      props.setEnabled (true);         
      return props;
   }

   public void build (String[] args) {

      MechModel mech = new MechModel ("mech");
      addModel (mech);

      float[] greenGold = new float[] {0.61f, 0.77f, 0.12f};
      float[] yellowGold = new float[] {1f, 0.44f, 0f};

      RenderProps.setShininess (mech, 20);
      RenderProps.setFaceStyle (mech, FaceStyle.FRONT_AND_BACK);
      RenderProps.setFaceColor (mech, greenGold);
      RenderProps.setSpecular (mech, yellowGold);

      mech.getRenderProps().setTextureMapProps (createTextureProps());
      mech.getRenderProps().setNormalMapProps (createNormalProps());
      mech.getRenderProps().setBumpMapProps (createBumpProps());

      FixedMeshBody body0 = createMesh (mech, 0);
      FixedMeshBody body1 = createMesh (mech, 2);
   }

}
