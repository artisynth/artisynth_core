package artisynth.demos.tutorial;

import java.awt.Color;
import java.util.LinkedList;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.properties.*;
import maspack.spatialmotion.SpatialInertia;

public class RollingFem extends RootModel {
   public static boolean debug = false;

   static double myDensity = 1000;

   public void build (String[] args) {

      MechModel mech = new MechModel ("mech");
      addModel (mech);

      double radius = 0.1;

      mech.setFriction (0.2);
      mech.setDefaultCollisionBehavior (
         Collidable.Deformable, Collidable.Rigid, true, -1);
      mech.setUseImplicitFriction (true);

      RigidBody plate = RigidBody.createBox (
         "plate", 1.0, 0.5, 0.1, 1000.0);
      plate.setPose (
         new RigidTransform3d (0, 0, -0.25, 0, Math.toRadians(25), 0));
      //new RigidTransform3d (0, 0, -0.20, 0, Math.toRadians(25), 0));
      plate.setDynamic (false);         
      mech.addRigidBody (plate);

      FemModel3d fem = FemFactory.createHexCylinder (
         null, 0.25, radius, 5, 20);
      //FemModel3d fem = FemFactory.createHexCylinder (
      //   null, 0.5, radius, 1, 10);
      fem.transformGeometry (new RigidTransform3d (0, 0, 0, 0, 0, Math.PI/2));
      mech.addModel (fem);

      RenderProps.setLineColor (fem.getElements(), Color.BLUE);
      RenderProps.setPointStyle (fem.getNodes(), Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (fem.getNodes(), 0.005);

      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (fem, new Color (0f, 0.7f, 0.7f));

      RenderProps.setLineColor (
         fem.getElements(), new Color (0f, 0.2f, 0.2f));
      
      // rendering: draw contact and friction forces as blue arrows
      CollisionManager cm = mech.getCollisionManager();
      cm.setDrawFrictionForces(true);
      cm.setDrawContactForces(true);
      cm.setContactForceLenScale(0.02);
      cm.setBilateralVertexContact (false);
      RenderProps.setSolidArrowLines (cm, 0.005, Color.CYAN);
      RenderProps.setVisible (cm, true); 


      addControlPanel (mech, fem);
   }

   protected void addControlPanel (MechModel mech, FemModel3d fem) {
      CollisionManager cm = mech.getCollisionManager();
      ControlPanel panel = new ControlPanel();
      panel = new ControlPanel ("options", "");
      panel.addWidget (mech, "integrator");
      panel.addWidget (mech, "maxStepSize");
      panel.addWidget (cm, "friction");
      panel.addWidget (cm, "bilateralVertexContact");
      panel.addWidget (cm, "contactForceLenScale");
      panel.addWidget (cm, "drawContactForces");
      panel.addWidget (cm, "drawFrictionForces");
      panel.addWidget (mech, "useImplicitFriction");
      panel.addWidget (fem, "material");

      addControlPanel (panel);
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "simple demo of a 3d fem";
   }
}
