package artisynth.demos.fem;

import java.awt.Color;
import java.util.*;

import artisynth.core.modelbase.*;
import artisynth.core.materials.*;
import artisynth.core.femmodels.*;
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

public class FemFrictionBeam extends RootModel {

   //double myAngle = 0.0;
   double myAngle = 30.0;
   //double myMu = 0.49;
   double myMu = 0.45;
   double myForce = 0.0;

   public enum FrictionMethod {
      IMPLICIT_G,
      IMPLICIT_N,
      DEFAULT
   };

   FrictionMethod myMethod = FrictionMethod.DEFAULT;

   //ArrayList<FemNode3d> myBottomNodes = new ArrayList<FemNode3d>();

   public static PropertyList myProps =
      new PropertyList (FemFrictionBeam.class, RootModel.class);

   static {
      myProps.add ("friction", "friction coefficient", 0);
      myProps.add ("angle", "inclination angle", 0);
      myProps.add ("profileSolve", "enable profiling of the solver", false);
      myProps.add (
         "frictionMethod",
         "method used to compute friction", FrictionMethod.DEFAULT);
      
   }

   public void setFrictionMethod (FrictionMethod method) {
      MechModel mechMod = (MechModel)findComponent ("models/0");
      if (mechMod != null) {
         switch (method) {
            case DEFAULT: {
               mechMod.setUseImplicitFriction (false);
               mechMod.getCollisionManager().setBilateralVertexContact(true);
               break;
            }
            case IMPLICIT_G: {
               mechMod.setUseImplicitFriction (true);
               mechMod.getCollisionManager().setBilateralVertexContact(true);
               break;
            }
            case IMPLICIT_N: {
               mechMod.setUseImplicitFriction (true);
               mechMod.getCollisionManager().setBilateralVertexContact(false);
               break;
            }
         }
      }
      myMethod = method;
   }

   public FrictionMethod getFrictionMethod() {
      return myMethod;
   }

   public void setFriction (double mu) {
      myMu = mu;
      if (models().size() > 0) {
         MechModel mechMod = (MechModel)models().get(0);
         mechMod.setFriction (mu);
      }
   }

   public double getFriction() {
      return myMu;
   }

   // public void setExplicitForce (double force) {
   //    myForce = force;
   //    Vector3d fvec = new Vector3d();
   //    double s = Math.sin(Math.toRadians(getAngle()));
   //    double c = Math.cos(Math.toRadians(getAngle()));

   //    fvec.set (-force*c, 0, force*s);
   //    if (models().size() > 0) {
   //       for (FemNode3d n : myBottomNodes) {
   //          n.setExternalForce (fvec);
   //       }
   //    }
   // }

   // public double getExplicitForce() {
   //    return myForce;
   // }

   public void setAngle (double ang) {
      if (models().size() > 0) {
         MechModel mechMod = (MechModel)models().get(0);      
         double delAng = ang-myAngle;
         mechMod.transformGeometry (
            new RigidTransform3d (0, 0, 0, 0, 1, 0, Math.toRadians(delAng)));
      }
      myAngle = ang;
   }

   public double getAngle () {
      return myAngle;
   }

   public void setProfileSolve (boolean enable) {
      if (models().size() > 0) {
         MechModel mechMod = (MechModel)models().get(0);      
         mechMod.getSolver().profileConstrainedBE = enable;
      }
   }

   public boolean getProfileSolve () {
      if (models().size() > 0) {
         MechModel mechMod = (MechModel)models().get(0);   
         return mechMod.getSolver().profileConstrainedBE;
      }
      return false;
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void build (String[] args) {

      double feml = 0.1;
      double femw = 0.05;
      double femh = 0.05;

      double fx = 0; //1.225; // external force along x
      double mz = 1.5;   // external moment about z

      double plateh = 0.01;

      MechModel mech = new MechModel ("mech");
      FemModel3d fem;
      //fem = FemFactory.createHexGrid (null, feml, femw, femh, 6, 3, 3);
      fem = FemFactory.createHexGrid (null,feml,femw,femh, 1, 1, 1);
      //fem = FemFactory.createHexGrid (null, feml, femw, femh, 10, 10, 6);
      fem.setName("fem");
      fem.setDensity(1000);


      // double EPS = 1e-8;
      // for (FemNode3d n : fem.getNodes()) {
      //    if (n.getPosition().z <= -femw/2+EPS) {
      //       myBottomNodes.add (n);
      //    }
      // }
           
      mech.addModel (fem);
      RigidBody plate = RigidBody.createBox (
         "plate", 6*feml, 3*feml, plateh, 1000);

      mech.addRigidBody (plate);

      plate.setDynamic (false);

      plate.setPose (new RigidTransform3d (0, 0, -(femh+plateh)/2));

      RenderProps.setPointStyle (fem, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (fem, 0.001);
      RenderProps.setPointColor (fem, Color.GREEN);

      mech.transformGeometry (
         new RigidTransform3d (0, 0, 0, 0, 1, 0, Math.toRadians (myAngle)));

      addModel (mech);

      setFriction (myMu);
      CollisionManager cm = mech.getCollisionManager();
      cm.setDrawFrictionForces(true);
      cm.setDrawContactForces(true);
      cm.setContactForceLenScale(1.0);
      // cm.setColliderType (ColliderType.AJL_CONTOUR);
      RenderProps.setLineRadius (cm, 0.001);
      RenderProps.setLineColor (cm, Color.CYAN);
      RenderProps.setLineStyle (cm, LineStyle.SOLID_ARROW);
      RenderProps.setVisible (cm, true);  
      CollisionBehavior behav = new CollisionBehavior (true, -1);
      // behav.setDrawColorMap (ColorMapType.CONTACT_PRESSURE); 
      // behav.setRenderingCollidable (0); // show color map on collidable 0 (fem);
      // behav.setColorMapRange (new ScalarRange(0, 1.0));
      mech.setCollisionBehavior (fem, plate, behav);
      //mech.setStabilization (PosStabilization.GlobalStiffness);

      // double Iz = 0;
      // for (FemNode3d n : fem.getNodes()) {
      //    Iz += n.getMass()*n.getPosition().norm();
      // }

      // for (FemNode3d n : fem.getNodes()) {
      //    Point3d pos = n.getPosition();
      //    Vector3d u = new Vector3d (-pos.y, pos.x, 0);
      //    double r = pos.norm();
      //    if (r != 0) {
      //       u.scale (1/r);
      //    }
      //    else {
      //       u.setZero();
      //    }
      //    double m = n.getMass();
      //    n.setExternalForce (
      //       new Vector3d(fx*m/fem.getMass()+mz*m*r*u.x/Iz, mz*m*r*u.y/Iz, 0));
      // }
      
      //mech.setProfiling (true);
      //RenderProps.setVisible (fem.getNodes(), false);

      //CollisionHandler.preventBilateralSeparation = true;
      fem.setMaterial (new LinearMaterial (500000.0, 0.33));
      setFrictionMethod (FrictionMethod.IMPLICIT_N);
      //setFrictionMethod (FrictionMethod.OLD_IMPLICIT);

      ControlPanel panel = new ControlPanel("controls");
      panel.addWidget (this, "friction");
      panel.addWidget (this, "frictionMethod");
      panel.addWidget (this, "angle");
      panel.addWidget (mech, "integrator");
      panel.addWidget (mech, "maxStepSize");
      panel.addWidget (cm, "contactForceLenScale");
      panel.addWidget (cm, "drawContactForces");
      panel.addWidget (cm, "drawFrictionForces");
      panel.addWidget (cm, "stictionCompliance");
      panel.addWidget (cm, "stictionCreep");
      panel.addWidget (fem, "material");
      panel.addWidget (this, "profileSolve");
      addControlPanel (panel);

      Main.getMain().arrangeControlPanels (this);

      //addBreakPoint (0.02);
   }

   public void attach (DriverInterface di) {
      super.attach (di);
      setFrictionMethod (getFrictionMethod());
   }
   
   public StepAdjustment advance (double t0, double t1, int flags) {
      StepAdjustment sa = super.advance (t0, t1, flags);

      MechModel mech = (MechModel)findComponent("models/0");
      FemNode3d node = (FemNode3d)mech.findComponent("models/fem/nodes/8");

      // if (t1 < 0.02) {
      //    node.setExternalForce (
      //       new Vector3d (Math.sqrt(3)/2, 0, -0.5));
      // }
      // else {
      //    node.setExternalForce (new Vector3d());
      // }
      
      
      return sa;
   }

}
