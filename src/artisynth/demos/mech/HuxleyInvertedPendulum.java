/**
 * Copyright (c) 2026, by the Authors: Jeff M. Barrett
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.demos.mech;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.HuxleyAxialMuscle;
import artisynth.core.materials.LinearAxialMaterial;
import artisynth.core.materials.MaxwellAxialMaterial;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.interpolation.Interpolation;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.spatialmotion.SpatialInertia;

/**
 * An inverted pendulum, stabilized by a pair of antagonistic muscles, that is
 * perturbed by a brief torque pulse. The muscles can be modeled using a
 * Huxley-type cross-bridge material ({@link HuxleyAxialMuscle}), or for
 * comparison, using linear springs, damped linear springs, or standard linear
 * solids.
 *
 * <p>The pendulum consists of a point mass {@code m} at a height {@code L}
 * above a hinge. The muscles connect the ground to the pendulum at a height
 * {@code b}, with horizontal offsets of {@code +/-a}, so that in the upright
 * position each muscle is vertical with length {@code b}. For small
 * rotations, the pendulum is stable if each muscle has a stiffness greater
 * than the critical value
 * <pre>
 *   k_crit = m g L / (2 a^2).
 * </pre>
 * The short-range stiffness of a Huxley muscle is proportional to its
 * activation, so that it stabilizes the pendulum only if its activation
 * exceeds {@code k_crit / K_max}, where {@code K_max} is its maximum
 * isometric stiffness.
 *
 * <p>The model accepts the following arguments:
 * <dl>
 * <dt>{@code -type <huxley|spring|damped|sls>}</dt>
 * <dd>muscle type (default: {@code huxley})</dd>
 * <dt>{@code -activation <a>}</dt>
 * <dd>activation of the Huxley muscles (default: 1.0)</dd>
 * <dt>{@code -stiffness <k>}</dt>
 * <dd>stiffness of the springs, in N/m, for the spring, damped and sls
 * types. The default is the short-range stiffness of the Huxley muscles at
 * the specified activation, so that the models can be compared.</dd>
 * <dt>{@code -perturbation <torque>}</dt>
 * <dd>peak perturbation torque, in N-m (default: -5.0)</dd>
 * <dt>{@code -duration <time>}</dt>
 * <dd>duration of the output probes, in seconds (default: 10)</dd>
 * </dl>
 */
public class HuxleyInvertedPendulum extends RootModel {

   /**
    * Describes how the muscles are modeled.
    */
   public enum MuscleType {
      /** Huxley cross-bridge muscles, using {@link HuxleyAxialMuscle} */
      HUXLEY,
      /** linear springs */
      SPRING,
      /** linear springs with damping */
      DAMPED,
      /**
       * standard linear solids, implemented using a linear spring in parallel
       * with a Maxwell element
       */
      SLS
   };

   // gravity
   public static double GRAVITY = 9.81;

   // pendulum geometry and mass
   public static double PENDULUM_LENGTH = 0.3052; // height L of the mass
   public static double PENDULUM_MASS = 30.0;     // mass m
   public static double MUSCLE_OFFSET = 0.05;     // horizontal offset a
   public static double INSERTION_HEIGHT = 0.142; // insertion height b

   // Huxley muscle properties
   public static double FIBRE_LENGTH = 0.142;
   public static double CROSS_SECTIONAL_AREA = 24e-4;
   public static double MUSCLE_DAMPING = 40.0;

   // damping for the damped springs, and viscosity for the SLS elements
   public static double SPRING_DAMPING = 10000.0;
   // stiffness of the SLS Maxwell elements, as a multiple of the spring
   // stiffness
   public static double SLS_STIFFNESS_RATIO = 1000.0*10.0/9.0;

   // perturbation properties
   public static double PERTURBATION_START = 1.0;
   public static double PERTURBATION_DURATION = 0.05;
   public static double PERTURBATION_RAMP_TIME = 0.001;

   MuscleType myMuscleType = MuscleType.HUXLEY;
   double myActivation = 1.0;
   double myStiffness = -1; // if < 0, use the Huxley short-range stiffness
   double myPerturbationTorque = -5.0;
   double myDuration = 10.0;

   /**
    * Returns the critical stiffness that each muscle must exceed for the
    * upright position to be stable.
    *
    * @return critical stiffness
    */
   public static double getCriticalStiffness() {
      double a = MUSCLE_OFFSET;
      return PENDULUM_MASS*GRAVITY*PENDULUM_LENGTH/(2*a*a);
   }

   protected void parseArgs (String[] args) {
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-type") && i+1 < args.length) {
            myMuscleType = MuscleType.valueOf (args[++i].toUpperCase());
         }
         else if (args[i].equals ("-activation") && i+1 < args.length) {
            myActivation = Double.parseDouble (args[++i]);
         }
         else if (args[i].equals ("-stiffness") && i+1 < args.length) {
            myStiffness = Double.parseDouble (args[++i]);
         }
         else if (args[i].equals ("-perturbation") && i+1 < args.length) {
            myPerturbationTorque = Double.parseDouble (args[++i]);
         }
         else if (args[i].equals ("-duration") && i+1 < args.length) {
            myDuration = Double.parseDouble (args[++i]);
         }
         else {
            System.out.println (
               "WARNING: HuxleyInvertedPendulum: unrecognized argument " +
               args[i]);
         }
      }
   }

   public void build (String[] args) throws IOException {
      parseArgs (args);

      MechModel mech = new MechModel ("mech");
      addModel (mech);
      mech.setGravity (0, -GRAVITY, 0);
      mech.setIntegrator (Integrator.Trapezoidal);
      mech.setMaxStepSize (1e-4);

      double a = MUSCLE_OFFSET;
      double b = INSERTION_HEIGHT;
      double L = PENDULUM_LENGTH;

      // fixed floor
      RigidBody floor = RigidBody.createBox (
         "floor", 0.6, 0.0075, 0.15, /*density=*/1000);
      floor.setDynamic (false);
      mech.addRigidBody (floor);

      // pendulum, with its frame origin at the hinge, and all of its mass
      // concentrated at the top
      RigidBody pendulum = new RigidBody ("pendulum");
      pendulum.setSurfaceMesh (createPendulumMesh (a, b, L));
      SpatialInertia inertia = new SpatialInertia ();
      inertia.addPointMass (PENDULUM_MASS, new Vector3d (0, L, 0));
      pendulum.setInertia (inertia);
      mech.addRigidBody (pendulum);

      // hinge about the z axis
      HingeJoint hinge = new HingeJoint (
         floor, pendulum, new Point3d(), new Vector3d (0, 0, 1));
      hinge.setName ("hinge");
      hinge.setThetaRange (-45.0, 45.0);
      mech.addBodyConnector (hinge);

      // muscle attachment points
      FrameMarker flexorOrigin = new FrameMarker (floor, new Point3d (a, 0, 0));
      FrameMarker extensorOrigin = new FrameMarker (floor, new Point3d (-a, 0, 0));
      FrameMarker flexorInsertion = new FrameMarker (pendulum, new Point3d (a, b, 0));
      FrameMarker extensorInsertion = new FrameMarker (pendulum, new Point3d (-a, b, 0));
      mech.addFrameMarker (flexorOrigin);
      mech.addFrameMarker (extensorOrigin);
      mech.addFrameMarker (flexorInsertion);
      mech.addFrameMarker (extensorInsertion);

      AxialSpring flexor;
      AxialSpring extensor;
      double kspring = myStiffness;
      if (kspring < 0) {
         // match the short-range stiffness of the Huxley muscles
         kspring = myActivation*createHuxleyMaterial().getMaxIsoStiffness();
      }
      switch (myMuscleType) {
         case HUXLEY: {
            flexor = createHuxleyMuscle ("flexor");
            extensor = createHuxleyMuscle ("extensor");
            break;
         }
         case SPRING: {
            flexor = new AxialSpring ("flexor", b);
            extensor = new AxialSpring ("extensor", b);
            flexor.setMaterial (new LinearAxialMaterial (kspring, 0));
            extensor.setMaterial (new LinearAxialMaterial (kspring, 0));
            break;
         }
         case DAMPED: {
            flexor = new AxialSpring ("flexor", b);
            extensor = new AxialSpring ("extensor", b);
            flexor.setMaterial (
               new LinearAxialMaterial (kspring, SPRING_DAMPING));
            extensor.setMaterial (
               new LinearAxialMaterial (kspring, SPRING_DAMPING));
            break;
         }
         case SLS: {
            // a standard linear solid is formed from a linear spring in
            // parallel with a Maxwell element
            flexor = new AxialSpring ("flexor", b);
            extensor = new AxialSpring ("extensor", b);
            flexor.setMaterial (new LinearAxialMaterial (kspring, 0));
            extensor.setMaterial (new LinearAxialMaterial (kspring, 0));
            double kmax = SLS_STIFFNESS_RATIO*kspring;
            AxialSpring flexorMaxwell = new AxialSpring ("flexorMaxwell", b);
            AxialSpring extensorMaxwell = new AxialSpring ("extensorMaxwell", b);
            flexorMaxwell.setMaterial (
               new MaxwellAxialMaterial (kmax, SPRING_DAMPING, b));
            extensorMaxwell.setMaterial (
               new MaxwellAxialMaterial (kmax, SPRING_DAMPING, b));
            mech.attachAxialSpring (
               flexorOrigin, flexorInsertion, flexorMaxwell);
            mech.attachAxialSpring (
               extensorOrigin, extensorInsertion, extensorMaxwell);
            RenderProps.setVisible (flexorMaxwell, false);
            RenderProps.setVisible (extensorMaxwell, false);
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Muscle type " + myMuscleType + " not implemented");
         }
      }
      mech.attachAxialSpring (flexorOrigin, flexorInsertion, flexor);
      mech.attachAxialSpring (extensorOrigin, extensorInsertion, extensor);

      addPerturbationProbe (pendulum);
      addOutputProbes (hinge, flexor, extensor);
      if (myMuscleType == MuscleType.HUXLEY) {
         createControlPanel (flexor, extensor);
      }

      // render properties
      Color color;
      switch (myMuscleType) {
         case HUXLEY: color = Color.RED; break;
         case SLS: color = Color.GREEN; break;
         default: color = Color.BLUE; break;
      }
      RenderProps.setSpindleLines (mech, 0.02, color);
      RenderProps.setSphericalPoints (mech, 0.008, Color.WHITE);
      RenderProps.setFaceColor (floor, new Color (0.6f, 0.6f, 0.6f));
      RenderProps.setFaceColor (pendulum, new Color (0.8f, 0.8f, 1.0f));
      setDefaultViewOrientation (AxisAlignedRotation.X_Y);
   }

   /**
    * Creates the material for the Huxley muscles.
    */
   protected HuxleyAxialMuscle createHuxleyMaterial() {
      return new HuxleyAxialMuscle (
         FIBRE_LENGTH, CROSS_SECTIONAL_AREA, MUSCLE_DAMPING);
   }

   /**
    * Creates a Huxley muscle with the specified name.
    */
   protected Muscle createHuxleyMuscle (String name) {
      Muscle mus = new Muscle (name);
      mus.setMaterial (createHuxleyMaterial());
      // the cross-bridge state is initialized to the isometric steady state
      // for this excitation at the first force evaluation
      mus.setExcitation (myActivation);
      return mus;
   }

   /**
    * Creates a mesh for the pendulum, consisting of a rod with a sphere at
    * the top (representing the mass) and a cross bar at the muscle
    * insertions.
    */
   protected PolygonalMesh createPendulumMesh (double a, double b, double L) {
      double rodRadius = 0.01;
      PolygonalMesh mesh =
         MeshFactory.createCylinder (rodRadius, L, /*nslices=*/24);
      // cylinder is centered on the origin and aligned with z
      mesh.transform (new RigidTransform3d (0, L/2, 0, 0, 0, Math.PI/2));
      PolygonalMesh mass = MeshFactory.createSphere (0.05, /*nslices=*/24);
      mass.transform (new RigidTransform3d (0, L, 0));
      mesh.addMesh (mass);
      PolygonalMesh bar = MeshFactory.createBox (2.4*a, 0.015, 0.03);
      bar.transform (new RigidTransform3d (0, b, 0));
      mesh.addMesh (bar);
      return mesh;
   }

   /**
    * Adds a probe that applies a brief torque pulse to the pendulum, with
    * smooth onset and offset ramps.
    */
   protected void addPerturbationProbe (RigidBody pendulum) {
      double t0 = PERTURBATION_START;
      double t1 = PERTURBATION_START + PERTURBATION_DURATION;
      double dt = PERTURBATION_RAMP_TIME;
      double T = myPerturbationTorque;
      NumericInputProbe probe = new NumericInputProbe (
         pendulum, "externalForce", 0, myDuration);
      // knot points: time, then the wrench (force, moment)
      probe.addData (
         new double[] {
            0.0,        0, 0, 0, 0, 0, 0,
            t0-dt,      0, 0, 0, 0, 0, 0,
            t0+dt,      0, 0, 0, 0, 0, T,
            t1-2*dt,    0, 0, 0, 0, 0, T,
            t1+2*dt,    0, 0, 0, 0, 0, 0,
            myDuration, 0, 0, 0, 0, 0, 0 },
         NumericInputProbe.EXPLICIT_TIME);
      // cubic Hermite interpolation with zero slopes at the knots
      probe.setInterpolationOrder (Interpolation.Order.CubicStep);
      probe.setName ("perturbation");
      addInputProbe (probe);
   }

   /**
    * Adds probes to record the pendulum angle, and the muscle forces (for
    * Huxley muscles) or lengths (for springs).
    */
   protected void addOutputProbes (
      HingeJoint hinge, AxialSpring flexor, AxialSpring extensor) {
      NumericOutputProbe probe;
      probe = new NumericOutputProbe (hinge, "theta", 0, myDuration, -1);
      probe.setName ("pendulum angle");
      addOutputProbe (probe);

      if (flexor instanceof Muscle) {
         addSpringProbe (flexor, "forceNorm", "flexor force");
         addSpringProbe (extensor, "forceNorm", "extensor force");
         addSpringProbe (flexor, "material.stiffness", "flexor stiffness");
      }
      else {
         // forces are not available as properties for plain springs
         addSpringProbe (flexor, "length", "flexor length");
         addSpringProbe (extensor, "length", "extensor length");
      }
   }

   private void addSpringProbe (
      AxialSpring spr, String propName, String probeName) {
      NumericOutputProbe probe =
         new NumericOutputProbe (spr, propName, 0, myDuration, -1);
      probe.setName (probeName);
      addOutputProbe (probe);
   }

   /**
    * Adds a control panel for adjusting the muscle excitations and
    * material properties.
    */
   protected void createControlPanel (
      AxialSpring flexor, AxialSpring extensor) {
      ControlPanel panel = new ControlPanel ("controls");
      panel.addWidget ("excitation", flexor, extensor);
      panel.addWidget ("material.damping", flexor, extensor);
      panel.addWidget ("material.attachmentRate", flexor, extensor);
      panel.addWidget ("material.detachmentRate", flexor, extensor);
      panel.addWidget ("material.crossBridgeStiffness", flexor, extensor);
      panel.addWidget ("material.maxIsoForce", flexor);
      panel.addWidget ("material.maxIsoStiffness", flexor);
      addControlPanel (panel);
   }
}
