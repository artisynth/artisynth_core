package artisynth.demos.tutorial;

import maspack.matrix.RigidTransform3d;
import maspack.matrix.VectorNd;
import maspack.util.Clonable;
import maspack.interpolation.Interpolation;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.Frame;
import artisynth.core.workspace.RootModel;
import artisynth.core.probes.NumericControlProbe;
import artisynth.core.probes.DataFunction;

/**
 * Simple demo using a NumericControlProbe to spin a Frame about the z
 * axis.
 */
public class SpinControlProbe extends RootModel {

   // Define the DataFunction that spins the body
   class SpinFunction implements DataFunction, Clonable {
      
      Frame myFrame; 
      RigidTransform3d myTFW0; // initial frame to world transform

      SpinFunction (Frame frame) {
         myFrame = frame;
         myTFW0 = new RigidTransform3d (frame.getPose());
      }

      public void eval (VectorNd vec, double t, double trel) {
         // vec should have size == 1, giving the current spin angle
         double ang = Math.toRadians(vec.get(0));
         RigidTransform3d TFW = new RigidTransform3d();
         TFW.R.mulRpy (ang, 0, 0);
         myFrame.setPose (TFW);
      }

      public Object clone() throws CloneNotSupportedException {
         return super.clone();
      }
   }

   public void build (String[] args) {

      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // Create a parametrically controlled rigid body to spin:
      RigidBody body = RigidBody.createBox ("box", 1.0, 1.0, 0.5, 1000.0);
      mech.addRigidBody (body);
      body.setDynamic (false);

      // Create a NumericControlProbe with size 1, initial spin data
      // with time step 2.0, start time 0, and stop time 8.
      NumericControlProbe spinProbe =
         new NumericControlProbe (
            /*vsize=*/1,
            new double[] { 0.0, 90.0, 0.0, -90.0, 0.0 },
            2.0, 0.0, 8.0);
      // set cubic interpolation for a smoother result
      spinProbe.setInterpolationOrder (Interpolation.Order.Cubic);
      // then set the data function:
      spinProbe.setDataFunction (new SpinFunction(body));
      addInputProbe (spinProbe);
   }
}
