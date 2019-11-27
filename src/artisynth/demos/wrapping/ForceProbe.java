package artisynth.demos.wrapping;

import artisynth.core.mechmodels.Frame;
import artisynth.core.probes.NumericMonitorProbe;
import maspack.matrix.VectorNd;
import maspack.spatialmotion.Wrench;

/**
 * Probe to record the translational force of a frame
 * 
 * @author John E. Lloyd
 */
public class ForceProbe extends NumericMonitorProbe {
   
   Frame myFrame;
   VectorNd myCheckVals;

   public ForceProbe (
      Frame frame, String fileName,
      double startTime, double stopTime, double interval) {

      super (3, fileName, startTime, stopTime, interval);
      myFrame = frame;
   }

   public void generateData (VectorNd vec, double t, double trel) {
      Wrench wr = myFrame.getForce();

      vec.set (0, wr.f.x);
      vec.set (1, wr.f.y);
      vec.set (2, wr.f.z);
   }

}
