package artisynth.core.probes;

import java.io.*;
import java.util.*;

import artisynth.core.mechmodels.Point;
import maspack.util.*;
import maspack.matrix.*;

public class TRCReaderWriterTest extends UnitTest {

   public void test (
      int nframes, int npnts, double start, double stop) throws IOException {
      
      ArrayList<Point> points = new ArrayList<>();
      for (int i=0; i<npnts; i++) {
         points.add (new Point("pnt"+i, new Point3d()));
      }
      PositionInputProbe probe0 =
         new PositionInputProbe ("probe0", points, null, false, start, stop);
      probe0.clearData();
      for (int k=0; k<nframes; k++) {
         VectorNd v = new VectorNd(3*npnts);
         v.setRandom(-100,100);
         double t = (nframes == 1 ? 0.0 : k*(stop-start)/(nframes-1));
         probe0.addData (t, v);
      }
      File testfile = new File (
         PathFinder.getSourceRelativePath (this, "testfile.trc"));

      TRCWriter writer = new TRCWriter (testfile);
      writer.setNumberFormat ("%g");
      writer.writeData (probe0);

      TRCReader reader = new TRCReader (testfile);
      reader.readData();
      PositionInputProbe probe1 =
         reader.createInputProbe ("probe1", points, false, start, stop);
      checkEquals (
         "probe0,1 list equal", probesEqual (probe0, probe1), true);

      PositionInputProbe probe2 =
         reader.createInputProbeFromLabels ("probe2", points, false, start, stop);
      checkEquals (
         "probe0,2 list equal", probesEqual (probe0, probe2), true);

      if (npnts > 1) {
         ArrayList<Point> subpnts = new ArrayList<>();
         for (int i=0; i<npnts; i++) {
            if ((i%2) != 0) {
               subpnts.add (points.get(i));
            }
         }
         PositionInputProbe probe3 =
            reader.createInputProbeFromLabels ("probe3", subpnts, false, start, stop);

         writer.writeData (probe3);
         reader = new TRCReader (testfile);
         reader.readData();
         PositionInputProbe probe4 =
            reader.createInputProbe ("probe4", subpnts, false, start, stop);
         checkEquals (
            "probe3,4 list equal", probesEqual (probe3, probe4), true);
      }
      
      testfile.delete();
   }

   private boolean probesEqual (
      NumericInputProbe probe0, NumericInputProbe probe1) {
      if ((probe0.getStartTime() != probe1.getStartTime()) ||
          (probe0.getStopTime() != probe1.getStopTime())) {
         return false;
      }
      if (!probe0.getNumericList().equals (probe1.getNumericList())) {
         return false;
      }
      return true;
   }      

   public void test() throws IOException {
      test (10, 5, 0.0, 3.0);
      test (12, 1, 0.0, 3.0);
      test (1, 5, 1.0, 4.0);
      test (0, 5, 1.0, 4.0);
   }

   public static void main (String[] args) {
      TRCReaderWriterTest tester = new TRCReaderWriterTest();
      tester.runtest();
   }

}
