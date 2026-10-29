package artisynth.demos.test;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;

import maspack.matrix.*;
import maspack.geometry.*;

/**
 * Model used to generate simple unconstrained FE solve data for an
 * unstructured tetrahedral mesh. Consists of a sphere, tessellated using
 * tetgen, which is anchored either along one side (the default) or along the
 * top.
 */
public class BigSphere extends RootModel {

   public void build (String[] args) {

      double radius = 0.5;
      int divisions = 3;     // subdivision level of the icosahedral surface
      double quality = 1.5;  // tetgen radius-edge quality bound

      String size = "small"; // problem size
      boolean fixTop = false; // fix nodes along the top instead of the side
      boolean writeData = false; // write solve data to file
      boolean murtySolve = false; // use the MurtySolver
      boolean quad = false; // use quadratic elements
      boolean unknownArgs = false;

      for (int i=0; i<args.length; i++) {
         switch (args[i]) {
            case "-small": {
               divisions = 3;
               size = "small";
               break;
            }
            case "-medium": {
               divisions = 4;
               size = "medium";
               break;
            }
            case "-large": {
               divisions = 5;
               size = "large";
               break;
            }
            case "-fixSide": {
               fixTop = false;
               break;
            }
            case "-fixTop": {
               fixTop = true;
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

      if (quad) {
         // Converting to quadratic tets increases the number of nodes by
         // roughly a factor of 6, so use one fewer surface subdivision (which
         // reduces the number of linear nodes by roughly a factor of 4).
         divisions--;
      }
      PolygonalMesh surface =
         MeshFactory.createIcosahedralSphere (radius, divisions);
      FemModel3d fem = FemFactory.createFromMesh (null, surface, quality);
      if (quad) {
         fem = FemFactory.createQuadraticModel (null, fem);
      }

      // fix the nodes within a cap of the sphere, either on the -x side or
      // on the top (+z)
      double capLimit = 0.9*radius;
      for (FemNode3d n : fem.getNodes()) {
         Point3d p = n.getPosition();
         if (fixTop ? (p.z >= capLimit) : (p.x <= -capLimit)) {
            n.setDynamic (false);
         }
      }
      mech.addModel (fem);

      System.out.println (
         size + " problem size, "+fem.numNodes()+" nodes, "+
         fem.numElements()+(quad ? " quadratic" : "")+" elements");

      if (writeData) {
         mech.getSolver().setCrsFileName (
            "sphere_"+size+(quad ? "_quad" : "")+".txt");
         addBreakPoint (1.0);
      }
      if (murtySolve) {
          mech.setUseImplicitFriction (true);
      }
   }

   private void printOptions() {
      System.out.println (
         "Options for BigSphere:\n" +
         "  -small       small problem size (default)\n" +
         "  -medium      medium problem size\n" +
         "  -large       large problem size\n" +
         "  -quad        use quadratic tet elements, with the resolution\n" +
         "               reduced to give a roughly similar number of nodes\n" +
         "  -fixSide     fix nodes in a cap on the -x side (default)\n" +
         "  -fixTop      fix nodes in a cap on the top\n" +
         "  -writeData   write solve matrix data to a file\n" +
         "  -murtySolve  use implicit friction (MurtySolver)");
   }

}
