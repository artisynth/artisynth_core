package artisynth.core.probes;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.interpolation.*;
import artisynth.core.probes.Probe.ExportProps;

/**
 * Provides testing for NumericProbes. At present, used only for testing import
 * and export methods.
 */
public class NumericProbeTest extends UnitTest {

   private NumericProbeBase createProbe (
      int vsize, boolean input, double stopTime) {
      
      NumericProbeBase probe;
      if (input) {
         NumericInputProbe iprobe = new NumericInputProbe();
         probe = iprobe;
      }
      else {
         NumericOutputProbe oprobe = new NumericOutputProbe();
         probe = oprobe;
      }
      probe.initVsize (vsize);
      probe.createNumericList();
      probe.setStartTime (0);
      probe.setStopTime (stopTime);
      return probe;
   }

   private void setRandomData (
      NumericProbeBase probe, int numk, double timeStep) {
      int vsize = probe.getVsize();
      NumericList nlist = probe.getNumericList();
      for (int k=0; k<numk; k++) {
         NumericListKnot knot = new NumericListKnot (vsize);
         knot.v.setRandom();
         knot.t = k*timeStep;
         nlist.add (knot);
      }
   }

   boolean probeDataEqual (
      NumericProbeBase probe0, NumericProbeBase probe1) {
      
      NumericList nlist0 = probe0.getNumericList();
      NumericList nlist1 = probe1.getNumericList();
      boolean equals = nlist0.equals (nlist1);
      if (!equals) {
         System.out.println ("probe0:");
         for (int k=0; k<nlist0.getNumKnots(); k++) {
            NumericListKnot knot = nlist0.getKnot(k);
            System.out.println (" " + knot.t + " " + knot.v);
         }
         System.out.println ("probe1:");
         for (int k=0; k<nlist1.getNumKnots(); k++) {
            NumericListKnot knot = nlist1.getKnot(k);
            System.out.println (" " + knot.t + " " + knot.v);
         }
      }
      return equals;
   }

   public void testImportExport (
      int numk, int vsize, double timeStep, boolean input) {

      double stopTime = (numk-1)*timeStep;
      NumericProbeBase probe0 = createProbe (vsize, input, stopTime);
      NumericProbeBase probe1 = createProbe (vsize, input, stopTime);
      setRandomData (probe0, numk, timeStep);

      String[] exts = new String[] { "csv", "txt" };
      File testFile = null;
      try {
         for (String ext : exts) {
            TextExportProps eprops =
               new TextExportProps((TextExportProps)probe0.getExportProps(ext));
            testFile = new File ("testImportExport." + ext);
            eprops.setIncludeTime (true);
            probe0.exportData (testFile, eprops);
            probe1.importData (testFile, -1);
            if (!probeDataEqual (probe0, probe1)) {
               throw new TestException (
                  "imported probe != exported probe, time included, ext=" + ext);
            }
            eprops.setIncludeTime (false);
            probe0.exportData (testFile, eprops);
            probe1.importData (testFile, timeStep);
            if (!probeDataEqual (probe0, probe1)) {
               throw new TestException (
                  "imported probe != exported probe, time excluded, ext=" + ext);
            }
            // test extension-specific methods
            if (ext.equals ("csv")) {
               probe0.exportCsvData (testFile);
               probe1.importCsvData (testFile, -1);
               if (!probeDataEqual (probe0, probe1)) {
                  throw new TestException (
                     "imported probe != exported probe, CSV, time included");
               }
               probe0.exportCsvData (testFile, "%g", /*timeIncluded*/false);
               probe1.importCsvData (testFile, timeStep);
               if (!probeDataEqual (probe0, probe1)) {
                  throw new TestException (
                     "imported probe != exported probe, CSV, time excluded");
               }
            }
            else {
               probe0.exportTextData (testFile);
               probe1.importTextData (testFile, -1);
               if (!probeDataEqual (probe0, probe1)) {
                  throw new TestException (
                     "imported probe != exported probe, text, time included");
               }
               probe0.exportTextData (testFile, "%g", /*timeIncluded*/false);
               probe1.importTextData (testFile, timeStep);
               if (!probeDataEqual (probe0, probe1)) {
                  throw new TestException (
                     "imported probe != exported probe, text, time excluded");
               }
            }
         }
      }
      catch (IOException e) {
         throw new TestException ("I/O error during import export test", e);
      }
      finally {
         if (testFile != null) {
            testFile.delete();
         }
      }
   }

   public void testImportExport() {
      testImportExport (/*numk*/1, /*vsize*/5, /*timeStep*/0.1, /*input*/true);
      testImportExport (/*numk*/2, /*vsize*/5, /*timeStep*/0.1, /*input*/true);
      testImportExport (/*numk*/10, /*vsize*/5, /*timeStep*/0.1, /*input*/true);

      testImportExport (/*numk*/1, /*vsize*/1, /*timeStep*/0.1, /*input*/true);
      testImportExport (/*numk*/2, /*vsize*/1, /*timeStep*/0.1, /*input*/true);
      testImportExport (/*numk*/10, /*vsize*/1, /*timeStep*/0.1, /*input*/true);

      // weird edge case with vsize=0, but test anyway
      testImportExport (/*numk*/1, /*vsize*/0, /*timeStep*/0.1, /*input*/true);
      testImportExport (/*numk*/2, /*vsize*/0, /*timeStep*/0.1, /*input*/true);
      testImportExport (/*numk*/10, /*vsize*/0, /*timeStep*/0.1, /*input*/true);
   }

   public void test() {
      testImportExport();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      NumericProbeTest tester = new NumericProbeTest();
      tester.runtest();
   }

}
