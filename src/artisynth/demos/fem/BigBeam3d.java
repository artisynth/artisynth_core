package artisynth.demos.fem;

import java.awt.Color;
import java.io.*;

import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;

import artisynth.core.probes.*;
import maspack.matrix.*;
import maspack.render.*;

import java.util.*;

/**
 * Creates an arbitrarily sized FEM beam model for use in testing the
 * FEM-related computational and memory performance of ArtiSynth.
 *
 * <p> The default size of the beam is 50 x 25 x 25 elements, but the number
 * of elements in the x and y+z directions can be specified with the arguments
 * -nx and -ny.
 *
 * <p> Profiling is enabled by default, so that the computation time for
 * required for each step is printed.
 *
 * <p> To run the program in "batch" mode, one can do the following:
 *
 * <pre>
 * artisynth -model artisynth.demos.fem.BigBeam3d [ -nx 90 -ny 60 ]
 *    -noGui -playFor 0.05
 *</pre>
 *
 * <p> This will create a 90 x 60 x 60 beam, and run it for 0.05 seconds.
 * 
 * <p> To disable hybrid solving, one can specify the argument
 * -disableHybridSolves.
 *
 * <p> To override the default number of threads, one should set the
 * environment variable OMP_NUM_THREADS.
 *
 * ===========================================================================
 *
 * Results on a core i7 with 16 Gbyte memory, with 11.4 Gbyte free:
 * 
 * 50x25x25 = 31250 elements, 34K nodes
 *
 *   mem: VIRT 10.8 G, RES 1.6 G, java 0.5 G, free 9.7 G
 *   cpu: direct, 1 thread:   7   sec
 *   cpu: direct, 4 thread:   3.5 sec
 *   cpu: hybrid, 4 thread:   1.1 sec
 *
 * 70*30*30 = 63000 elements, 67K nodes
 *
 *   mem: VIRT 11.4 G, RES 3.4 G, Java 1 G, free: 7.8 G
 *   cpu: direct, 1 thread:  23 sec
 *   cpu: direct, 4 thread:   9.7 sec
 *   cpu: hybrid, 4 thread:   2.6 sec
 *
 * 80x40x40 = 128000 elements, 135K nodes
 *
 *   mem: VIRT 14.9 G, RES 7.5 G, Java 2 G, free: 3.3 G
 *   cpu: direct, 1 thread:  90 sec
 *   cpu: direct, 4 thread:  41 sec
 *   cpu: hybrid, 4 thread:   6 sec
 * 
 * 80x50x50 = 200000 elements, 208K nodes
 *
 *   mem: VIRT 19.2 G, RES 12 G, Java 3 G, free: .3 G
 *   cpu: direct, 1 thread:  210 sec
 *   cpu: direct, 4 thread:   96 sec
 *   cpu: hybrid, 4 thread:   12 sec
 */
public class BigBeam3d extends FemBeam3d {

   protected int parseInt (String[] args, int i, int defaultValue) {
      int value = defaultValue;
      if (++i >= args.length) {
         System.out.println (
            "Warning: argument "+args[i]+" reguires an extra integer argument");
      }
      else {
         try {
            value = Integer.valueOf (args[i]);
         }
         catch (Exception e) {
            System.out.println (
               "Warning: integer argument expected for "+args[i]);
         }
      }
      return value;
   }

   public void build (String[] args) {

      int nx = 50;
      int ny = 25;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-nx")) {
            nx = parseInt (args, i++, nx);
         }
         else if (args[i].equals ("-ny")) {
            ny = parseInt (args, i++, ny);
         }
         else {
            System.out.println (
               "Warning: unrecognied argument "+args[i]);
         }
      }

      super.build ("hex", 30.0, 15.0, nx, ny, /*options=*/0);
      //super.build ("hex", 30.0, 15.0, 70, 30, /*options=*/0);
      //super.build ("hex", 30.0, 15.0, 80, 40, /*options=*/0);
      //super (name, "hex", 1.0, 0.2, 10, 5, 0);
      myFemMod.setSurfaceRendering (FemModel3d.SurfaceRender.None);

      MooneyRivlinMaterial mat = new MooneyRivlinMaterial ();
      myFemMod.setMaterial (mat);
      myMechMod.setProfiling (true);
      RenderProps.setVisible (myFemMod.getNodes(), false);
      System.out.println (
         "Running BigBeam "+nx+" X "+ny+" X "+ny+
         ", num dynamic nodes=" + (myFemMod.numNodes()-(ny+1)*(ny+1)));
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      // SolveMatrixTest tester = new SolveMatrixTest();
      // System.out.println ("error=" + tester.testStiffness (myMechMod, 1e-8));

      return super.advance (t0, t1, flags);
   }
}

