package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.EquilibriumAxialMuscle;
import artisynth.core.materials.Millard2012AxialMuscle;
import artisynth.core.materials.Millard2012AxialTendon;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.Particle;
import artisynth.core.modelbase.ControllerBase;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

/**
 * Demo showing the forces generated the length of a Millard2012 muscle,
 * implemented using both separate muscle and tendon components, and a combined
 * muscle/tendon material.
 */
public class EquilibriumMuscleDemo extends RootModel {

   Particle pr0, pr1; // right end point particles; used by controller
   
   // default muscle parameter settings
   private double myOptPennationAng = Math.toRadians(20.0);
   private double myMaxIsoForce = 10.0;
   private double myTendonSlackLen = 0.5;
   private double myOptFibreLen = 0.5;

   // initial total length of the muscles:
   private double len0 = 0.25 + myTendonSlackLen;

   public void build (String[] args) {
      // create a mech model with zero gravity
      MechModel mech = new MechModel ("mech");
      addModel (mech);
      mech.setGravity (0, 0, 0);

      // build first muscle, consisting of a tendonless muscle, attached to a
      // tendon via a connecting particle pc0 with a small mass.
      Particle pl0 = new Particle ("pl0", 1.0, 0.0, 0, 0); // left end point
      pl0.setDynamic (false); // point is fixed
      mech.addParticle (pl0);

      // create connecting particle. x coordinate will be set later.
      Particle pc0 = new Particle ("pc0", /*mass=*/1e-5, 0, 0, 0);
      mech.addParticle (pc0);

      pr0 = new Particle ("pr0", 1.0, len0, 0, 0); // right end point
      pr0.setDynamic (false); // point will be positioned by length controller
      mech.addParticle (pr0);

      // create muscle and attach it between pl0 and pc0
      Muscle mus0 = new Muscle("mus0"); // muscle 
      Millard2012AxialMuscle mat0 = new Millard2012AxialMuscle (
         myMaxIsoForce, myOptFibreLen, myTendonSlackLen, myOptPennationAng);
      mat0.setRigidTendon (true); // set muscle to rigid tendon with zero length
      mat0.setTendonSlackLength (0);
      mus0.setMaterial (mat0); 
      mech.attachAxialSpring (pl0, pc0, mus0);

      // create explicit tendon and attach it between pc0 and pr0
      AxialSpring tendon = new AxialSpring(); // tendon
      tendon.setMaterial (
         new Millard2012AxialTendon (myMaxIsoForce, myTendonSlackLen));
      mech.attachAxialSpring (pc0, pr0, tendon);

      // build second muscle, using combined muscle/tendom material, and attach
      // it between pl1 and pr1.
      Particle pl1 = new Particle (1.0, 0, 0, -0.5); // left end point
      pl1.setDynamic (false);
      mech.addParticle (pl1);

      pr1 = new Particle ("pr1", 1.0, len0, 0, -0.5); // right end point
      pr1.setDynamic (false);
      mech.addParticle (pr1);

      Muscle mus1 = new Muscle("mus1");
      Millard2012AxialMuscle mat1 = new Millard2012AxialMuscle (
         myMaxIsoForce, myOptFibreLen, myTendonSlackLen, myOptPennationAng);
      mus1.setMaterial (mat1);
      mech.attachAxialSpring (pl1, pr1, mus1);

      // initialize both muscle excitations to 1, and then adjust the muscle
      // lengths to the corresponding (zero velocity) equilibrium position
      mus0.setExcitation (1);
      mus1.setExcitation (1);
      // compute equilibrium muscle length with for 0 velocity
      double lm = mat1.computeLmWithConstantVm (
         len0, /*vel=*/0, /*excitation=*/1);
      // set muscle length of mat1 and x coord of pc0 to muscle length:
      mat1.setMuscleLength (lm);         
      pc0.setPosition (new Point3d (lm, 0, 0));

      // set render properties:
      // render markers as white spheres, and muscles as red spindles
      RenderProps.setSphericalPoints (mech, 0.03, Color.WHITE);
      RenderProps.setSpindleLines (mech, 0.02, Color.RED);
      // render tendon in blue and the juntion point in cyan
      RenderProps.setLineColor (tendon, Color.BLUE);
      RenderProps.setPointColor (pc0, Color.CYAN);

      // create a control panel to adjust material parameters and excitation
      ControlPanel panel = new ControlPanel();
      panel.addWidget ("material.optPennationAngle", mus0, mus1);
      panel.addWidget ("material.fibreDamping", mus0, mus1);
      panel.addWidget ("material.ignoreForceVelocity", mus0, mus1);
      panel.addWidget ("excitation", mus0, mus1);
      addControlPanel (panel);

      // add a controller to extend/contract the muscle end points, and probes
      // to record both muscle forces
      addController (new LengthController());
      addForceProbe ("muscle/tendon force", mus0, 2);
      addForceProbe ("equilibrium force", mus1, 2);
   }

   /**
    * A controller to extend and the contract the muscle length by moving the
    * rightmost muscle end points.
    */
   public class LengthController extends ControllerBase {
      
      double myRunTime = 1.5; // total extensions/contraction time
      double mySpeed = 1.0;   // speed of the end point motion

      public LengthController() { 
         // need null args constructor if this model is read from a file
      }

      public void apply (double t0, double t1) {
         double xlen = len0; // x position of the end points
         double xvel = 0; // x velocity of the end points
         if (t1 <= myRunTime/2) { // extend
            xlen += mySpeed*t1; 
            xvel = mySpeed;
         }
         else if (t1 <= myRunTime) { // contract
            xlen += mySpeed*(2*myRunTime/2 - t1);
            xvel = -mySpeed;
         }
         // update end point positions and velocities
         pr0.setPosition (new Point3d (xlen, 0, 0));
         pr1.setPosition (new Point3d (xlen, 0, -0.5));
         pr0.setVelocity (new Vector3d (xvel, 0, 0));
         pr1.setVelocity (new Vector3d (xvel, 0, 0));
      }
   }

   // Create and add an output probe to record the tension force of a muscle
   void addForceProbe (String name, Muscle mus, double stopTime) {
      try {
         NumericOutputProbe probe =
            new NumericOutputProbe (mus, "forceNorm", 0, stopTime, -1);
         probe.setName (name);
         addOutputProbe (probe);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Method that is called if this model is read from a saved file.
    * Reinitializes the global member references to pr0, pr1 and pc0.
    */
   public void postscanInitialize() {
      if (pr0 == null) {
         MechModel mech = (MechModel)findComponent("models/mech");
         pr0 = mech.particles().get("pr0");
         pr1 = mech.particles().get("pr1");
      }
   }
}
