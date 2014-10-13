package artisynth.demos.tutorial;

import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;

public class NetDemoWithPan extends NetDemo {

   public void build (String[] args) {
      super.build (args);
      addPanProbe ();
   }

   private double[] computePanData (
      double y0, double z0, double totalDegrees, double time, int nsegs) {

      double[] data = new double[4*(nsegs+1)];
      for (int i=0; i<=nsegs; i++) {
         double ang = i*Math.toRadians(totalDegrees)/(nsegs-1);
         double s = Math.sin(ang);
         double c = Math.cos(ang);
         data[i*4+0] = i*(time/nsegs);
         data[i*4+1] = s*y0;
         data[i*4+2] = c*y0;
         data[i*4+3] = z0;
      }
      return data;
   }


   public void addPanProbe () {
      try {
         NumericInputProbe inprobe =
            new NumericInputProbe (
               this, "viewerEye", 0, 6);
         double z = 0.8;
         inprobe.addData (
            computePanData (-38, 24, 180.0, 6.0, 16),
            NumericInputProbe.EXPLICIT_TIME);
         addInputProbe (inprobe);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }
}
