package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.inverse.TrackingController;
import artisynth.core.inverse.InverseManager;
import artisynth.core.inverse.InverseManager.ProbeID;
import artisynth.core.materials.ConstantAxialMuscle;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MotionTargetComponent;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.Particle;
import artisynth.core.workspace.RootModel;
import artisynth.core.probes.NumericInputProbe;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

/**
 * Demo of a particle controlled by a set of surrounding muscles
 */
public class InverseParticle extends RootModel {

   public void build (String[] args) {
      
      int numMuscles = 16; // num radial muscles surrounding the dynamic particle
      double len = 1.0; // length of the radial muscles

      // create MechModel and add to RootModel
      MechModel mech = new MechModel ("mech");
      addModel (mech);
      
      // disable gravity for the 2D demo
      mech.setGravity (Vector3d.ZERO);

      // create central dynamic particle
      Particle dynamicPart =
         new Particle ("center", /*mass=*/0.1, /*x,y,z=*/0, 0, 0);
      dynamicPart.setPointDamping (0.1); // add damping to point
      
      // set render properties for the component
      RenderProps.setSphericalPoints (dynamicPart, len/25, Color.WHITE);
      
      // add component to much model
      mech.addParticle(dynamicPart);
      
      // create radial muscles connected to center particle
      for (int i = 0; i < numMuscles; i++)
      {
         double a = 2*Math.PI*((double)i/numMuscles);
     
         // create non-dynamic particle as fixed end point for muscle
         Particle fixedPnt =
            new Particle(/*mass=*/1d,
                         new Point3d(len*Math.sin(a),0.0,len*Math.cos(a)));
         fixedPnt.setDynamic(false);
         RenderProps.setSphericalPoints (fixedPnt, len/25, Color.LIGHT_GRAY);
         
         // create passive spring to provide resistance
         AxialSpring spring =
            new AxialSpring (
               /*stiffness=*/100d, /*damping=*/1d, /*restlen=*/len/2);
         spring.setPoints (fixedPnt, dynamicPart);
                  
         // create muscle with force = fmax * activation
         Muscle muscle = new Muscle (fixedPnt, dynamicPart);
         muscle.setName (
            "muscle_"+Integer.toString (
               Math.round ((float)Math.toDegrees (a)))+"_deg");
         muscle.setMaterial (new ConstantAxialMuscle (/*fmax=*/1d));

         // make muscles red when activated
         RenderProps.setCylindricalLines (muscle, len/50, Color.BLUE.darker ());
         muscle.setExcitationColor (Color.RED);
         muscle.setMaxColoredExcitation (0.5);

         // add components to mech model
         mech.addParticle(fixedPnt);
         mech.addAxialSpring (spring);
         mech.addAxialSpring (muscle);
      }
      
      // create the tracking controller
      TrackingController myTrackingController =
         new TrackingController(mech, "tcon");
      
      // set all muscles to be "exciters" for the controller to control 
      for (AxialSpring s : mech.axialSprings()) {
         if (s instanceof Muscle) {
            myTrackingController.addExciter((Muscle)s);
         }
      }
      
      // set the center dynamic particle to be the component that is tracked
      MotionTargetComponent target =
         myTrackingController.addMotionTarget(dynamicPart);
    
      // add an l-2 regularization term, since there are more muscles than
      // target degrees-of-freedom
      myTrackingController.addL2RegularizationTerm(/*weight=*/0.1);   

      // add a default set of probes and the inverse control panel for this demo
      myTrackingController.createProbesAndPanel (this);
      InverseManager.setInputProbeData (
         this,
         ProbeID.TARGET_POSITIONS,
         new double[] {0d, 0d, 0d, 0.5, 0d, 0.5, 0d, 0d, 0d}, 
         /*timestep=*/0.5);
      
      // add controller component to the root model
      addController(myTrackingController);
   }
}
