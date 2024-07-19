package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.inverse.ForceEffectorTarget;
import artisynth.core.inverse.InverseManager;
import artisynth.core.inverse.InverseManager.ProbeID;
import artisynth.core.inverse.TrackingController;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.Particle;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.workspace.RootModel;
import maspack.interpolation.Interpolation;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.Property;
import maspack.render.RenderProps;

/**
 * Demo using the tracking controller to control the tension in a spring.
 */
public class InverseSpringForce extends RootModel {

   int numMuscles = 3; // num radial muscles surrounding the dynamic particle
   double muscleStiffness = 200; // passive muscle stiffness
   double muscleDamping = 0.1; // passive muscle damping
   double muscleFmax = 200; // max active force at excitation = 1   
   double dist = 1.0; // distance of anchor point from world origin

   /**
    * Create a muscle excitable spring extending out from the origin
    * {@code part} at an angle {@code ang}.
    */
   void createMuscle (MechModel mech, Particle part, double ang) {
      // create and add non-dynamic particle as fixed end point for muscle
      Particle fixed = new Particle(
         /*mass=*/1d, new Point3d(dist*Math.sin(ang),0.0,dist*Math.cos(ang)));
      fixed.setDynamic(false);
      mech.addParticle(fixed);
      // create muscle and set its material
      Muscle muscle = new Muscle (fixed, part);
      muscle.setName (  // name the muscle using its angle
         "muscle_"+Integer.toString((int)Math.round(Math.toDegrees(ang))));
      muscle.setMaterial (
         new SimpleAxialMuscle (muscleStiffness, muscleDamping, muscleFmax));
      mech.addAxialSpring (muscle);

      // make muscles red when activated
      muscle.setExcitationColor (Color.RED);
      muscle.setMaxColoredExcitation (0.5);
      muscle.setRestLength (muscle.getLength());
   }

   public void build (String[] args) {
      // create MechModel and add to RootModel
      MechModel mech = new MechModel ("mech");
      addModel (mech);
      mech.setGravity (Vector3d.ZERO); // disable gravity      

      // create and add center particle
      Particle part = new Particle ("center", /*mass=*/0.1, /*x,y,z=*/0, 0, 0.33);
      part.setPointDamping (0.1); // add damping
      mech.addParticle(part);
      
      // create radial muscles connected to center particle
      for (int i = 0; i < numMuscles; i++) {
         double angle = 2*Math.PI*((double)i/numMuscles);
         createMuscle (mech, part, angle);
      }
      
      // create the tracking controller and add it to the root model
      TrackingController tcon = new TrackingController(mech, "tcon");
      addController(tcon);      
      // set all muscles but the first to be "exciters" for the controller
      for (int i=1; i<numMuscles; i++) {
         tcon.addExciter((Muscle)mech.axialSprings().get(i));
      }
      // set the first muscle to be the force effector target. This
      // will be unactivated and will simple serve as a passive spring
      AxialSpring passiveSpring = mech.axialSprings().get(0);
      ForceEffectorTarget target =
         tcon.addForceEffectorTarget(passiveSpring);
      // add an L-2 regularization term, since there are more muscles than
      // target degrees-of-freedom
      tcon.setL2Regularization(/*weight=*/0.1);   

      // Render properties: make points gray spheres, central particle white,
      // muscles as blue spindles, and passive spring as a cyan spindle.
      RenderProps.setSphericalPoints (this, dist/25, Color.LIGHT_GRAY);
      RenderProps.setPointColor (part, Color.WHITE);
      RenderProps.setSpindleLines (mech, dist/25, Color.BLUE.darker ());      
      RenderProps.setLineColor (passiveSpring, Color.CYAN);

      // add an input probe to control the desired target tension:
      NumericInputProbe targetprobe = new NumericInputProbe (
         target, "targetForce", /*startTime=*/0, /*stopTime=*/1);
      targetprobe.setName ("target tension");
      targetprobe.addData (
         new double[] {0d, 120d, 0d}, // three knot points
         /*timestep=*/0.5);
      targetprobe.setInterpolationOrder (Interpolation.Order.Cubic);
      addInputProbe (targetprobe);

      // add an output probe to show both the target tension ("targetForce"
      // of target) and actual tension ("forceNorm" of passiveSpring)
      Property[] props = new Property[] {
         target.getProperty ("targetForce"),
         passiveSpring.getProperty ("forceNorm"),
      };
      NumericOutputProbe trackingProbe = 
         new NumericOutputProbe (props, /*interval=*/-1);
      trackingProbe.setName ("target and source tension");
      trackingProbe.setStartTime (0);
      trackingProbe.setStopTime (1);
      addOutputProbe (trackingProbe);

      // add an output probe to record the excitations:
      NumericOutputProbe exprobe = InverseManager.createOutputProbe (
         tcon, ProbeID.COMPUTED_EXCITATIONS, /*fileName*/null,
         /*startTime*/0, /*stopTime*/1, /*interval*/-1);
      addOutputProbe (exprobe);
   }
}
