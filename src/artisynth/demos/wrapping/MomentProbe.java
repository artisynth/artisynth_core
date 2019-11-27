package artisynth.demos.wrapping;

import artisynth.core.mechmodels.Frame;
import artisynth.core.probes.NumericMonitorProbe;
import maspack.matrix.VectorNd;
import maspack.spatialmotion.Wrench;

/**
 * Probe to record the moment of a frame.
 */
public class MomentProbe extends NumericMonitorProbe {
   
   Frame myFrame;

   public MomentProbe (
      Frame frame, String fileName,
      double startTime, double stopTime, double interval) {

      super (3, fileName, startTime, stopTime, interval);
      myFrame = frame;
   }

   public void generateData (VectorNd vec, double t, double trel) {
      Wrench wr = myFrame.getForce();

      vec.set (0, wr.m.x);
      vec.set (1, wr.m.y);
      vec.set (2, wr.m.z);
   }

}
