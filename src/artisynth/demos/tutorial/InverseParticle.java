package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.inverse.InverseManager;
import artisynth.core.inverse.InverseManager.ProbeID;
import artisynth.core.inverse.TrackingController;
import artisynth.core.inverse.TargetPoint;
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
import maspack.render.RenderProps;

/**
 * Demo of a particle controlled by a set of surrounding muscles
 */
public class InverseParticle extends RootModel {

   int numMuscles = 16; // num radial muscles surrounding the dynamic particle
   double muscleStiffness = 200; // passive muscle stiffness
   double muscleDamping = 1.0; // passive muscle damping
   double muscleFmax = 1000; // max active force at excitation = 1   
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

      // create and add particle whose position is to be controlled
      Particle part = new Particle ("center", /*mass=*/0.1, /*x,y,z=*/0, 0, 0);
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
      // set all muscles to be "exciters" for the controller to control 
      for (AxialSpring s : mech.axialSprings()) {
         if (s instanceof Muscle) {
            tcon.addExciter((Muscle)s);
         }
      }
      // set the center dynamic particle to be the component that is tracked
      TargetPoint target = tcon.addPointTarget(part);
      // add an L-2 regularization term, since there are more muscles than
      // target degrees-of-freedom
      tcon.setL2Regularization(/*weight=*/0.1);   

      // Render properties: make points gray spheres, central particle white,
      // and muscles as blue cylinders. 
      RenderProps.setSphericalPoints (this, dist/25, Color.LIGHT_GRAY);
      RenderProps.setPointColor (part, Color.WHITE);
      RenderProps.setCylindricalLines (mech, dist/50, Color.BLUE.darker ());      

      // add an input probe to control the position of the target:
      NumericInputProbe targetprobe = new NumericInputProbe (
         target, "position", /*startTime=*/0, /*stopTime=*/1);
      targetprobe.setName ("target positions");
      targetprobe.addData (
         new double[] {0d,0d,0d, 0.5,0d,0.5, 0d,0d,0d}, // three knot points
         /*timestep=*/0.5);
      targetprobe.setInterpolationOrder (Interpolation.Order.Cubic);
      addInputProbe (targetprobe);

      // add an output probe to record the excitations:
      NumericOutputProbe exprobe = InverseManager.createOutputProbe (
         tcon, ProbeID.COMPUTED_EXCITATIONS, /*fileName=*/null,
         /*startTime=*/0, /*stopTime=*/1, /*interval=*/-1);
      addOutputProbe (exprobe);
   }
}
