package artisynth.demos.test;

import java.awt.Color;

import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.geometry.*;
import maspack.properties.*;
import maspack.interpolation.Interpolation.Order;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;
import artisynth.core.gui.*;
import artisynth.core.inverse.*;
import artisynth.core.workspace.*;

/**
 * Model to test the use of inverse control force minimization applied to a
 * FrameSpring
 */
public class MinimizeFrameSpringForce extends RootModel {

   TrackingController myController = null; // inverse controller
   double myForceTermWeight = 0.1;         // overall force minimization weight
   ForceEffectorTerm myForceTerm;          // force minimization term
   FrameSpring mySpring;
   MechModel myMech;

   public static PropertyList myProps =
      new PropertyList (MinimizeFrameSpringForce.class, RootModel.class);

   static VectorNd DEFAULT_TARGET_WEIGHTS = new VectorNd (6);
   
   static {
      myProps.add (
         "forceTermWeight", "overall weighting for force minimization", 1.0);
      myProps.add (
         "transWeight", "translational weighting for force minimization", 1.0);
      myProps.add (
         "rotWeight", "rotational weighting for force minimization", 1.0);
      myProps.addReadOnly (
         "springForce", "frame spring force");
      myProps.addReadOnly (
         "forceTermNorm", "frame spring force with weights applied");
      myProps.add (
         "stiffness", "translational spring stiffness", 1e5);
      myProps.add (
         "rotStiffness", "rotational spring stiffness", 1e5);
      myProps.add (
         "damping", "translational spring damping", 0.0);
      myProps.add (
         "rotDamping", "rotational spring damping", 0.0);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /* --- Begin property accessor methods --- */

   public double getForceTermWeight() {
      return myForceTermWeight;
   }

   public void setForceTermWeight (double w) {
      myForceTermWeight = w;
      if (getForceTerm() != null) {
         myForceTerm.setWeight (w);
      }
   }

   public VectorNd getSpringForce() {
      VectorNd minf = new VectorNd(6);
      if (getForceTerm() != null) {
         myForceTerm.getForceError (minf);
      }
      return minf;
   }

   public double getForceTermNorm() {
      VectorNd minf = new VectorNd(6);
      if (getForceTerm() != null) {
         myForceTerm.getForceError (minf);
         double tw = getTransWeight();
         double rw = getRotWeight();
         minf.set (0, tw*minf.get(0));
         minf.set (1, tw*minf.get(1));
         minf.set (2, tw*minf.get(2));
         minf.set (3, rw*minf.get(3));
         minf.set (4, rw*minf.get(4));
         minf.set (5, rw*minf.get(5));
      }
      return minf.norm();
   }

   FrameSpring getSpring() {
      if (mySpring == null) {
         reinitMembers();
      }
      return mySpring;
   }

   ForceEffectorTerm getForceTerm() {
      if (myForceTerm == null) {
         reinitMembers();
      }
      return myForceTerm;
   }

   public double getStiffness() {
      if (getSpring() != null) {
         LinearFrameMaterial mat = (LinearFrameMaterial)mySpring.getMaterial();
         return mat.getStiffness().x;
      }
      else {
         return 0;
      }
   }

   public void setStiffness (double k) {
      if (getSpring() != null) {
         LinearFrameMaterial mat = (LinearFrameMaterial)mySpring.getMaterial();
         mat.setStiffness(new Vector3d (k, k, k));
      }
   }

   public double getRotStiffness() {
      if (getSpring() != null) {
         LinearFrameMaterial mat = (LinearFrameMaterial)mySpring.getMaterial();
         return mat.getRotaryStiffness().x;
      }
      else {
         return 0;
      }
   }

   public void setRotStiffness (double k) {
      if (getSpring() != null) {
         LinearFrameMaterial mat = (LinearFrameMaterial)mySpring.getMaterial();
         mat.setRotaryStiffness(new Vector3d (k, k, k));
      }
   }

   public double getDamping() {
      if (getSpring() != null) {
         LinearFrameMaterial mat = (LinearFrameMaterial)mySpring.getMaterial();
         return mat.getDamping().x;
      }
      else {
         return 0;
      }
   }

   public void setDamping (double d) {
      if (getSpring() != null) {
         LinearFrameMaterial mat = (LinearFrameMaterial)mySpring.getMaterial();
         mat.setDamping(new Vector3d (d, d, d));
      }
   }

   public double getRotDamping() {
      if (getSpring() != null) {
         LinearFrameMaterial mat = (LinearFrameMaterial)mySpring.getMaterial();
         return mat.getRotaryDamping().x;
      }
      else {
         return 0;
      }
   }

   public void setRotDamping (double d) {
      if (getSpring() != null) {
         LinearFrameMaterial mat = (LinearFrameMaterial)mySpring.getMaterial();
         mat.setRotaryDamping(new Vector3d (d, d, d));
      }
   }

   public double getTransWeight () {
      if (getForceTerm() != null) {
         VectorNd weights = myForceTerm.getTargetWeights(0);
         return weights.get(0);
      }
      else {
         return 0;
      }
   }

   public void setTransWeight (double w) {
      if (getForceTerm() != null) {
         VectorNd weights = myForceTerm.getTargetWeights(0);
         weights.set (0, w);
         weights.set (1, w);
         weights.set (2, w);
         myForceTerm.setTargetWeights(0, weights);
      }
   }

   public double getRotWeight () {
      if (getForceTerm() != null) {
         VectorNd weights = myForceTerm.getTargetWeights(0);
         return weights.get(3);
      }
      else {
         return 0;
      }
   }

   public void setRotWeight (double w) {
      if (getForceTerm() != null) {
         VectorNd weights = myForceTerm.getTargetWeights(0);
         weights.set (3, w);
         weights.set (4, w);
         weights.set (5, w);
         myForceTerm.setTargetWeights(0, weights);
      }
   }

   public void setTargetWeights (VectorNd wgts) {
      if (getForceTerm() != null) {
         myForceTerm.setTargetWeights (0, wgts);
      }
   }

   protected void reinitMembers() {
      if (getControllers().size() > 0) {
         TrackingController tcon =
            (TrackingController)getControllers().get(0);
         myForceTerm = tcon.getForceEffectorTerm();
         myMech = (MechModel)models().get(0);
         mySpring = myMech.frameSprings().get(0);
      }
   }

   public void build (String[] args) {
      // underly mech model
      myMech = new MechModel ("mech");
      addModel (myMech);

      // add rigid body damping terms
      myMech.setFrameDamping (10000.0);
      myMech.setRotaryDamping (10000.0);

      double r = 1.0;
      double density = 1000.0;

      // create rigid body from a rounded cylinder, with a marker at the top
      PolygonalMesh mesh =
         MeshFactory.createRoundedCylinder (r/4, 2*r, 20, 20, /*flat=*/false);
      RigidBody rod = RigidBody.createFromMesh ("rod", mesh, density, 1.0);
      rod.setPose (new RigidTransform3d (0, 0, -r-r/4));
      myMech.addRigidBody (rod);

      FrameMarker mkr = myMech.addFrameMarker (rod, new Point3d (0, 0, r+r/4));

      // create circle of points and "muscle" actuators which anchored to them
      int npnts = 6;
      for (int i=0; i<npnts; i++) {
         double ang = 2*Math.PI*i/npnts;
         Particle p = new Particle (1, 2*r*Math.cos(ang), 0, 2*r*Math.sin(ang));
         p.setDynamic (false);
         myMech.addParticle (p);

         Muscle mus = new Muscle();
         mus.setMaterial (new SimpleAxialMuscle (1000.0, 0, 20000));
         myMech.attachAxialSpring (mkr, p, mus);
      }

      // create and add the frame spring at the base of the rigid body
      mySpring = new FrameSpring (null);
      mySpring.setMaterial (
         new LinearFrameMaterial (
            /*ktrans=*/1.0e5, /*krot=*/1.0e5, /*dtrans=*/0, /*drot=*/0));
      mySpring.setFrames (rod, null, new RigidTransform3d (0, 0, -2.5*r));
      myMech.addFrameSpring (mySpring);

      // create the tracking controller and add the force minimization terms
      myController = new TrackingController(myMech, "tcon");
      for (AxialSpring s : myMech.axialSprings()) {
         if (s instanceof Muscle) {
            myController.addExciter((Muscle)s);
         }
      }
      myController.addL2RegularizationTerm();
      MotionTargetComponent target = myController.addMotionTarget(mkr);
      RenderProps.setSphericalPoints ((Renderable)target, r/20, Color.GREEN);
      myForceTerm = myController.addForceEffectorTerm ();
      myForceTerm.setWeight (myForceTermWeight);
      myForceTerm.addForce (mySpring, 0.0002, false);
      myForceTerm.debugHf = false;
      addController (myController);

      // add input probe for the target
      NumericInputProbe inprobe =
         new NumericInputProbe (target, "position", 0, 1);
      inprobe.addData (
         new double[] { 0,   0, 0, 0,
                        0.5, 1, 0, 0,
                        0,   0, 0, 0 },
         NumericInputProbe.EXPLICIT_TIME);
      inprobe.setInterpolationOrder (Order.Cubic);
      addInputProbe (inprobe);

      // create and add a control panel 
      addControlPanel ();

      // create and add output probe(s)
      addProbes ();

      // set render properties for the model
      RenderProps.setSphericalPoints (myMech, r/20, Color.WHITE);
      RenderProps.setSphericalPoints (mkr, r/20, Color.CYAN);
      RenderProps.setLineStyle (myMech.axialSprings(), LineStyle.SPINDLE);
      RenderProps.setLineRadius (myMech.axialSprings(), r/20);
      RenderProps.setLineColor (myMech.axialSprings(), Color.RED);

      RenderProps.setLineColor (mySpring, Color.RED);
      RenderProps.setLineWidth (mySpring, 3);
      mySpring.setAxisLength (r/2); 
   }

   private void addControlPanel() {
      ControlPanel panel = new ControlPanel ("options", "");

      panel.addWidget (myMech, "integrator");
      panel.addWidget (this, "forceTermWeight");
      panel.addWidget (this, "transWeight");
      panel.addWidget (this, "rotWeight");
      panel.addWidget (this, "springForce");
      panel.addWidget ("controllerEnabled", myController, "active");
      panel.addWidget (myController, "computeIncrementally");
      panel.addWidget (this, "stiffness");
      panel.addWidget (this, "rotStiffness");
      panel.addWidget (this, "damping");
      panel.addWidget (this, "rotDamping");
      addControlPanel (panel);
   }

   private void addProbes() {
      NumericOutputProbe oprobe =
         new NumericOutputProbe (this, "forceTermNorm", 0, 10, -1);
      addOutputProbe (oprobe);

      InverseManager.addProbes (this, myController, 5.0, 0.01);
   }


}
