package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.gui.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

/**
 * Model used to generate simple unconstrained FE solve data for analyzing
 * solver performance. Consists of a simple slab anchored at the left side.
 */
public class BigSlab extends RootModel {

   public void build (String[] args) {

      int nx = 15;
      int ny = 15;
      int nz = 2;
      double wx = 1.0;
      double wy = 1.0;
      double wz = 0.2;

      String size = "small"; // problem size
      boolean writeData = false; // write solve data to file
      boolean murtySolve = false; // use the MurtySolver
      boolean quad = false; // use quadratic elements
      boolean incompressible = false; // enable FEM incompressibility
      boolean unknownArgs = false;

      for (int i=0; i<args.length; i++) {
         switch (args[i]) {
            case "-small": {
               size = "small";
               break;
            }
            case "-medium": {
               nx = 30;
               ny = 30;
               nz = 3;
               size = "medium";
               break;
            }
            case "-large": {
               nx = 60;
               ny = 60;
               nz = 4;
               size = "large";
               break;
            }
            case "-writeData": {
               writeData = true;
               break;
            }
            case "-murtySolve": {
               murtySolve = true;
               break;
            }
            case "-quad": {
               quad = true;
               break;
            }
            case "-incompressible": {
               incompressible = true;
               break;
            }
            default: {
               System.out.println (
                  "WARNING: ignoring unknown argument '"+args[i]+"'");
               unknownArgs = true;
            }
         }
      }
      if (unknownArgs) {
         printOptions();
      }

      MechModel mech = new MechModel ("mech");
      addModel (mech);

      FemModel3d fem;
      if (quad) {
         // choose the grid resolution so that the number of nodes is
         // approximately the same as for the linear grid
         int mz = Math.max (1, (nz+1)/2);
         int numNodes = (nx+1)*(ny+1)*(nz+1);
         int mx = 1;
         int my = 1;
         int bestErr = Integer.MAX_VALUE;
         for (int kx=1; kx<=nx; kx++) {
            int ky = Math.max (1, (int)Math.round (kx*ny/(double)nx));
            int err = Math.abs (numQuadhexGridNodes (kx, ky, mz) - numNodes);
            if (err < bestErr) {
               bestErr = err;
               mx = kx;
               my = ky;
            }
         }
         fem = FemFactory.createQuadhexGrid (null, wx, wy, wz, mx, my, mz);
      }
      else {
         fem = FemFactory.createHexGrid (null, wx, wy, wz, nx, ny, nz);
      }

      for (FemNode3d n : fem.getNodes()) {
         Point3d p = n.getPosition();
         if (Math.abs(p.x-(-wx/2)) < 1e-8) {
            n.setDynamic (false);
         }
      }
      if (incompressible) {
         fem.setIncompressible (FemModel.IncompMethod.AUTO);
      }
      mech.addModel (fem);

      System.out.println (
         size + " problem size, "+fem.numNodes()+" nodes, "+
         fem.numElements()+(quad ? " quadratic" : "")+" elements");

      if (writeData) {
         mech.getSolver().setCrsFileName (
            "slab_"+size+(quad ? "_quad" : "")+".txt");
         addBreakPoint (1.0);
      }
      if (murtySolve) {
          mech.setUseImplicitFriction (true);
      }
   }

   /**
    * Number of nodes in a grid of 20-node quadratic hexes: corner nodes plus
    * mid-edge nodes along x, y and z.
    */
   private int numQuadhexGridNodes (int mx, int my, int mz) {
      return ((mx+1)*(my+1)*(mz+1) + mx*(my+1)*(mz+1) +
              (mx+1)*my*(mz+1) + (mx+1)*(my+1)*mz);
   }

   private void printOptions() {
      System.out.println (
         "Options for BigSlab:\n" +
         "  -small       small problem size (default)\n" +
         "  -medium      medium problem size\n" +
         "  -large       large problem size\n" +
         "  -quad        use quadratic hex elements, with the resolution\n" +
         "               reduced to give a similar number of nodes\n" +
         "  -writeData   write solve matrix data to a file\n" +
         "  -murtySolve  use implicit friction (MurtySolver)");
   }

}
