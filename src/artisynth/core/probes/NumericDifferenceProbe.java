package artisynth.core.probes;

import maspack.matrix.*;

public class NumericDifferenceProbe extends NumericMonitorProbe {

   NumericProbeBase myProbe0;
   NumericProbeBase myProbe1;

   public NumericDifferenceProbe() {
      super();
   }

   public NumericDifferenceProbe (
      NumericProbeBase probe0, NumericProbeBase probe1, double interval) {
      super (probe0.getVsize(), interval);
      setProbes (probe0, probe1);
   }

   public NumericDifferenceProbe (
      NumericProbeBase probe0, NumericProbeBase probe1, String fileName,
      double startTime, double stopTime, double interval) {
      super (probe0.getVsize(), fileName, startTime, stopTime, interval);
      setProbes (probe0, probe1);
   }

   public void setProbes (NumericProbeBase probe0, NumericProbeBase probe1) {
      if (probe0.getVsize() != probe1.getVsize()) {
         throw new IllegalArgumentException (
            "Probes have different vector sizes: " +
            probe0.getVsize() + " vs. " + probe1.getVsize());
      }
      setVsize (probe0.getVsize());
      myProbe0 = probe0;
      myProbe1 = probe1;
   }

   public void generateData (VectorNd vec, double t, double trel) {
      double trel0 = (t-myProbe0.getStartTime())/myProbe0.myScale;
      int vsize0 = myProbe0.getVsize();
      VectorNd vec0 = new VectorNd (vsize0);
      myProbe0.myNumericList.interpolate (vec0, trel0);

      double trel1 = (t-myProbe1.getStartTime())/myProbe1.myScale;
      int vsize1 = myProbe1.getVsize();
      VectorNd vec1 = new VectorNd (vsize1);
      myProbe1.myNumericList.interpolate (vec1, trel1);

      vec.setZero();
      for (int i=0; i<Math.min(vsize0,vsize1); i++) {
         vec.set (i, vec1.get(i) - vec0.get(i));
      }
   }

}
