package artisynth.core.gui;

import java.awt.Color;
import maspack.util.*;
import maspack.matrix.*;
import maspack.widgets.*;
import maspack.interpolation.*;
import maspack.render.color.HSL;
import java.io.*;
import java.util.*;

/**
 * Standalone application to test the large probe display. Creates a numeric
 * list with a given vector size, populates it with data, and the builds
 * the display around it.
 */
public class NumericDisplayTest {

   void setRandomList (NumericList nlist, int nknots) {
      VectorNd vec = new VectorNd(nlist.getVectorSize());
      VectorNd del = new VectorNd(nlist.getVectorSize());
      VectorNd mean = new VectorNd(nlist.getVectorSize());
      nlist.add (vec, 0);
      for (int k=1; k<nknots; k++) {
         double t = k/(double)(nknots-1);
         del.setRandom (-0.1, 0.1);
         vec.add (del);
         mean.add (vec);
         nlist.add (vec, t);
      }
      mean.scale (1/(double)(nknots-1));
      for (int k=0; k<nknots; k++) {
         nlist.getKnot(k).v.sub (mean);
      }
      
   }

   void createDisplay (int ntracks, int nknots, boolean input) {
      NumericList nlist = new NumericList (ntracks);
      setRandomList (nlist, nknots);
      
      String title = "test display for " + (input ? "input" : "output");

      NumericProbeDisplayLarge display =
         new NumericProbeDisplayLarge (title, nlist, input);
      display.pack();
      display.setVisible(true);
   }

   public static void printUsageAndExit() {
      String className = NumericDisplayTest.class.getName();
      System.out.println (
         "Usage: java "+className+" [-display]");
      System.exit(1);
   }

   public static void main (String[] args) {

      RandomGenerator.setSeed (0x1234);
      boolean input = false;
      int ntracks = 30;
      int nknots = 500;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-input")) {
            input = true;
         }
         else if (args[i].equals ("-ntracks")) {
            if (++i == args.length) {
               System.out.println ("-ntracks needs an additional argument");
               printUsageAndExit();
            }
            ntracks = Integer.valueOf (args[i]);
         }
         else if (args[i].equals ("-nknots")) {
            if (++i == args.length) {
               System.out.println ("-nknots needs an additional argument");
               printUsageAndExit();
            }
            nknots = Integer.valueOf (args[i]);
         }
         else {
            printUsageAndExit();
         }
      }

      NumericDisplayTest test = new NumericDisplayTest();
      test.createDisplay (ntracks, nknots, input);
   }
}
