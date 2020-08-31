package artisynth.demos.test;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import maspack.matrix.*;
import maspack.util.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.geometry.*;
import maspack.properties.*;
import maspack.interpolation.Interpolation.Order;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;
import artisynth.core.gui.*;
import artisynth.core.inverse.*;
import artisynth.core.workspace.*;

/**
 * Model to test the use of inverse control force minimization applied to a
 * FrameSpring
 */
public class FrameSpringPendulum extends RootModel {

   FrameSpring mySpring;
   MechModel myMech;
   StabilityTerm myStabilizer;

   static final double DEFAULT_PASSIVE_STIFFNESS = 1000.0;
   double myPassiveStiffness = DEFAULT_PASSIVE_STIFFNESS;

   static final double DEFAULT_MAX_COLORED_EXCITATION = 1.0;
   double myMaxColoredExcitation = DEFAULT_MAX_COLORED_EXCITATION;

   class StabilityMonitor extends MonitorBase {

      MechModel myMech;

      public StabilityMonitor (MechModel mech) {
         myMech = mech;
      }
      
      public void apply (double t0, double t1) {
         EigenDecomposition ed = new EigenDecomposition();
         ArrayList<SolveMatrixModifier> modifiers =
            new ArrayList<SolveMatrixModifier>();
         modifiers.add (new StiffnessMatrixScaler (0.00001, 1.0));
         MatrixNd H = new MatrixNd(myMech.getYPRStiffnessMatrix(modifiers));
         H.negate();
         ed.factor (H);
         VectorNd eigs = new VectorNd(ed.getEigReal());
         // sort the eigenvalues
         ArraySort.quickSort (eigs.getBuffer());
         System.out.println ("eigs: " + eigs.toString("%16.8f"));
         System.out.println ("det: " + ed.determinant());
      }
   }

   public void addTrackingController (MechModel mech) {
      TrackingController tcon = new TrackingController(mech, "tcon");
      
      // set all muscles to be "exciters" for the controller to control 
      for (AxialSpring spr : mech.axialSprings()) {
         tcon.addExciter((Muscle)spr);
      }
      myStabilizer = new StabilityTerm();
      myStabilizer.addStiffnessModifier (
         new StiffnessMatrixScaler (0.00001, 1.0));
      //myStabilizer.setIgnorePosition (true);
      myStabilizer.setDetTarget (0.001);
      tcon.addL2RegularizationTerm (1.0);
      tcon.addConstraintTerm (myStabilizer);      
      addController (tcon);
   }

   public static PropertyList myProps =
      new PropertyList (FrameSpringPendulum.class, RootModel.class);
   
   static {
      myProps.add (
         "stiffness", "translational spring stiffness", 1e5);
      myProps.add (
         "rotStiffness", "rotational spring stiffness", 1e5);
      myProps.add (
         "passiveStiffness",
         "passive stiffness in the springs", DEFAULT_PASSIVE_STIFFNESS);
      myProps.add (
         "maxColoredExcitation",
         "saturation value for excitation coloring",
         DEFAULT_MAX_COLORED_EXCITATION);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /* --- Begin property accessor methods --- */

   FrameSpring getSpring() {
      if (mySpring == null) {
         reinitMembers();
      }
      return mySpring;
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

   public double getPassiveStiffness() {
      return myPassiveStiffness;
   }

   public void setPassiveStiffness (double k) {
      for (AxialSpring spr : myMech.axialSprings()) {
         AxialMaterial mat = spr.getMaterial();
         if (mat instanceof SimpleAxialMuscle) {
            ((SimpleAxialMuscle)mat).setStiffness (k);
         }
      }
      myPassiveStiffness = k;
   }

   public double getMaxColoredExcitation() {
      return myMaxColoredExcitation;
   }

   public void setMaxColoredExcitation (double maxe) {
      for (AxialSpring spr : myMech.axialSprings()) {
         if (spr instanceof Muscle) {
            ((Muscle)spr).setMaxColoredExcitation (maxe);
         }
      }
      myMaxColoredExcitation = maxe;
   }

   protected void reinitMembers() {
      if (getControllers().size() > 0) {
         myMech = (MechModel)models().get(0);
         mySpring = myMech.frameSprings().get(0);
      }
   }

   public void build (String[] args) {

      int nsprings = 0;
      boolean stabilize = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-nsprings")) {
            if (++i == args.length) {
               System.out.println (
                  "WARNING: option -nsprings required another argument");
            }
            else {
               nsprings = Integer.valueOf (args[i]);
            }
         }
         else if (args[i].equals ("-stabilize")) {
            stabilize = true;
            if (nsprings == 0) {
               nsprings = 4;
            }
         }
         else {
            System.out.println (
               "WARNING: unrecognized option '"+args[i]+"'");
         }
      }

      // underlying mech model
      myMech = new MechModel ("mech");
      addModel (myMech);

      // add rigid body damping terms
      //myMech.setFrameDamping (10000.0);
      //myMech.setRotaryDamping (10000.0);

      double h = 2.0;
      double r = h/8;
      double density = 1000.0;

      // create rigid body from a rounded cylinder, with a marker at the top
      PolygonalMesh mesh =
         MeshFactory.createRoundedCylinder (r, h, 20, 20, /*flat=*/false);
      double zeq = -0.04401; // z offset for (un)stable equilibrium
      RigidBody rod = RigidBody.createFromMesh ("rod", mesh, density, 1.0);
      rod.setPose (new RigidTransform3d (0, 0, -h/2));
      myMech.addRigidBody (rod);

      // create circle of points and "muscle" actuators which anchored to them
      for (int i=0; i<nsprings; i++) {
         double ang = 2*Math.PI*i/nsprings;
         Particle p = new Particle (
            1, h*Math.cos(ang), h*Math.sin(ang), 0);
         FrameMarker mkr = myMech.addFrameMarker (
            rod, new Point3d (r*Math.cos(ang), r*Math.sin(ang), h/2));
         RenderProps.setSphericalPoints (mkr, r/5, Color.CYAN);
         p.setDynamic (false);
         myMech.addParticle (p);

         Muscle mus = new Muscle();
         mus.setMaterial (new SimpleAxialMuscle (myPassiveStiffness, 0, 50000));
         mus.setExcitationColor (Color.RED);
         myMech.attachAxialSpring (mkr, p, mus);
      }
      myMech.setMaxColoredExcitation(1.0);

      // create and add the frame spring at the base of the rigid body
      mySpring = new FrameSpring (null);
      mySpring.setMaterial (
         new LinearFrameMaterial (
            /*ktrans=*/1.0e5, /*krot=*/0, /*dtrans=*/0, /*drot=*/0));
      mySpring.setFrames (rod, null, new RigidTransform3d (0, 0, -(h+r)));
      myMech.addFrameSpring (mySpring);

      rod.setPose (new RigidTransform3d (0, 0, -h/2+zeq));

      if (stabilize) {
         addTrackingController (myMech);
      }
      else {
         addMonitor (new StabilityMonitor (myMech));
      }

      // create and add a control panel 
      addControlPanel (myMech);

      // set render properties for the model
      RenderProps.setSphericalPoints (myMech, r/5, Color.WHITE);
      RenderProps.setLineStyle (myMech.axialSprings(), LineStyle.SPINDLE);
      RenderProps.setLineRadius (myMech.axialSprings(), r/5);
      RenderProps.setLineColor (myMech.axialSprings(), Color.WHITE);

      RenderProps.setLineColor (mySpring, Color.RED);
      RenderProps.setLineWidth (mySpring, 3);
      mySpring.setAxisLength (2*r); 
   }

   private void addControlPanel (MechModel mech ) {
      ControlPanel panel = new ControlPanel ("options", "");

      panel.addWidget (this, "stiffness");
      panel.addWidget (this, "rotStiffness");

      if (mech.axialSprings().size() > 0) {
         panel.addWidget (this, "passiveStiffness");
         panel.addWidget (this, "maxColoredExcitation");
      }
      int k=0;
      for (AxialSpring spr : mech.axialSprings()) {
         panel.addWidget ("excitation"+k, spr, "excitation");
         k++;
      }
      if (myStabilizer != null) {
         panel.addWidget (myStabilizer, "det");
         panel.addWidget (myStabilizer, "detTarget");
         panel.addWidget ("stabilize", myStabilizer, "enabled");
      }
      addControlPanel (panel);
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      SolveMatrixTest tester = new SolveMatrixTest();
      tester.setYPRStiffness (true);
      System.out.println ("error=" + tester.testStiffness (myMech, 1e-8));
      return super.advance (t0, t1, flags);
   }

   public void render (Renderer r, int flags) {
      super.render (r, flags);
      if (myStabilizer != null) {
         NumberFormat fmt = new NumberFormat ("%7.4f");
         double emSize =  0.07*r.getScreenHeight();
         String str = "Det: " + fmt.format (myStabilizer.getDet());
         double margin = 0.02*r.getScreenWidth();
         Rectangle2D bounds = r.getTextBounds (r.getDefaultFont(), str, emSize);
         Vector3d pos =
            new Vector3d (r.getScreenWidth()-bounds.getWidth()-margin, margin, 0);
         r.begin2DRendering();
         r.setColor (Color.WHITE);
         r.drawText (
            "Det: " + fmt.format (myStabilizer.getDet()), pos, emSize);
         r.end2DRendering();
      }
   }

}
