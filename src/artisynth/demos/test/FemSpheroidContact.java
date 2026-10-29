package artisynth.demos.test;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.demos.tutorial.ElasticFoundationContact;
import artisynth.core.materials.LinearElasticContact;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.*;
import artisynth.core.materials.*;
import artisynth.core.mechmodels.CollisionBehavior.ColorMapType;
import artisynth.core.mechmodels.CollisionBehavior.Method;
import artisynth.core.mechmodels.CollisionBehavior.VertexPenetrations;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.MechSystemSolver.PosStabilization;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.Face.FaceFilter;
import maspack.geometry.*;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.*;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.util.PathFinder;

public class FemSpheroidContact extends RootModel {

   double radius = 0.5;

   public void build (String[] args) throws IOException {
      MechModel mech = new MechModel ("mech");
      mech.setGravity (0, 0, -45);
      mech.setStabilization (PosStabilization.GlobalStiffness);
      addModel (mech);
      
      // default fem material and damping properties
      double massDamping = 1.0;
      double stiffnessDamping = 0.0;

      double E = 2000000;
      FemMaterial mat = null;
      mat = new LinearMaterial (E, 0.49);
      mat = new MooneyRivlinMaterial (0.75*E/3, 0.25*E/3, 0, 0, 0, 100*E/5);

      // create the ball
      FemModel3d ball = FemFactory.createHexSphere (null, radius, /*nt*/30);
      ball.setMaterial (mat);
      ball.transformGeometry (AffineTransform3d.createScaling (1.0, 1.0, 0.6));
      ball.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (ball, new Color (0.7f, 0.7f, 1f));
      mech.addModel (ball);

      // create the plate for it to collide with
      double wx = 3;
      double wy = 1.5;
      double wz = 0.6;
      RigidBody plate = RigidBody.createBox (
         "plate", wx, wy, wz, /*density*/1000);
      plate.setDynamic (false);
      double tilt = -Math.PI/8;
      plate.setPose (
         new RigidTransform3d (0, 0, -1.5*radius-0.2/Math.cos(tilt), 0, tilt, 0));
      mech.addRigidBody (plate);

      CollisionBehavior cb = mech.setCollisionBehavior (ball, plate, true);
      cb.setBilateralVertexContact (false);

      ControlPanel panel = new ControlPanel();
      panel.addWidget (ball, "material");
      panel.addWidget (ball, "stiffnessDamping");
      panel.addWidget (ball, "particleDamping");
      panel.addWidget (mech, "stiffnessStabilizeMatrix");
      panel.addWidget (mech, "gravity");
         
      addControlPanel(panel);
   }

// may need to add this the build() method:
//    if (mech.getUseImplicitFriction()) {
//       mech.setCompliantContact();
//    }
}


