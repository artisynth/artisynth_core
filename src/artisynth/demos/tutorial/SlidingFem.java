package artisynth.demos.tutorial;

import java.awt.Color;
import java.util.*;

import artisynth.core.modelbase.*;
import artisynth.core.materials.*;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.*;
import artisynth.core.mechmodels.CollisionBehavior.ColorMapType;
import artisynth.core.mechmodels.CollisionManager.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.gui.ControlPanel;
import artisynth.core.driver.Main;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.matrix.*;
import maspack.properties.*;
import artisynth.core.util.ScalarRange;

public class SlidingFem extends RootModel {

   double myAngle = 30.0; // angle of the inclined plane

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      double feml = 0.1;    // x length of the FEM model
      double femh = 0.05;   // z height of the FEM model
      double planeh = 0.01; // height of the inclined plane

      // create an inclined plane for the FEM to slide on
      RigidBody plane = RigidBody.createBox (
         "plane", 3.2*feml, 3*femh, planeh, 1000);
      plane.setPose (new RigidTransform3d (feml, 0, -(femh+planeh)/2));
      plane.setDynamic (false);
      mech.addRigidBody (plane);

      // create the FEM to slide on the plane
      FemModel3d fem;
      fem = FemFactory.createHexGrid (
         null, /*xwidth=*/feml, /*ywidth=*/femh, /*zwidth=*/femh,
         /*nx=*/10, /*ny=*/10, /*nz=*/6);
      fem.setName("fem");
      fem.setDensity(1000);
      fem.setMaterial (new LinearMaterial (500000.0, 0.33));
      mech.addModel (fem);

      // rotate the entire MechModel by the angle of the inclined plane
      mech.transformGeometry (
         new RigidTransform3d (0, 0, 0, 0, 1, 0, Math.toRadians (myAngle)));

      // enable contact between the FEM and the plane
      mech.setFriction (0.45);
      mech.setCollisionBehavior (fem, plane, true, -1);
      mech.setUseImplicitFriction (true);

      // rendering: draw contact and friction forces as blue arrows
      CollisionManager cm = mech.getCollisionManager();
      cm.setDrawFrictionForces(true);
      cm.setDrawContactForces(true);
      cm.setContactForceLenScale(1.5);
      RenderProps.setSolidArrowLines (cm, 0.0007, Color.CYAN);
      RenderProps.setVisible (cm, true); 

      // rendering: draw FEM using a wireframe surface mesh, so we can see
      // through it
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setLineColor (fem, new Color (.5f, .5f, .5f));
      RenderProps.setVisible (fem.getElements(), false);
      RenderProps.setVisible (fem.getNodes(), false);
      RenderProps.setFaceStyle (fem, FaceStyle.NONE);
      RenderProps.setDrawEdges (fem, true);
      RenderProps.setShading (fem, Shading.NONE);
      RenderProps.setFaceColor (plane, new Color (.4f, .4f, .6f));

      // create a panel to control use of implicit friction and contact force
      // rendering
      ControlPanel panel = new ControlPanel("controls");
      panel.addWidget (mech, "useImplicitFriction");
      panel.addWidget (cm, "friction");
      panel.addWidget (cm, "bilateralVertexContact");
      panel.addWidget (cm, "contactForceLenScale");
      panel.addWidget (cm, "drawContactForces");
      panel.addWidget (cm, "drawFrictionForces");
      addControlPanel (panel);
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      MechModel mech = (MechModel)findComponent ("models/0");
      if (t1 == 0.02) {
         //mech.getSolver().myMurtyVelSolveRebuild = true;
      }
      StepAdjustment sa = super.advance (t0, t1, flags);
      return sa;
   }

}
