package artisynth.demos.opensim;

import java.io.IOException;
import java.util.ArrayList;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.GmshWriter;
import artisynth.core.femmodels.NodeNumberWriter;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;

/**
 * Generates the data for OpenSimFemElbow.
 */
public class OpenSimFemElbowGen extends OpenSimArm26 {

   public static boolean omitFromMenu = true;

   double frameAttachDist = 0.005;

   ArrayList<FemNode3d> findRadialNodes (FemModel3d fem, double rdist) {
      ArrayList<FemNode3d> nodes = new ArrayList<>();
      for (FemNode3d n : fem.getNodes()) {
         Point3d pos = n.getPosition();
         double r = Math.hypot (pos.x, pos.y);
         if (Math.abs(r-rdist) < 1e-6) {
            nodes.add (n);
         }
      }
      return nodes;
   }

   public void build (String[] args) throws IOException {
      // create OpenSimArm26
      super.build (args);

      String datadir = getSourceRelativePath ("geometry/");

      // create full and partial hex tubes to simulate the cartilage of the
      // elbow. femH is attached to the humerus, and femL is attached to
      // the lower arm.
      double rinnerH = 0.0075;
      double routerH = 0.0100;
      double rinnerL = 0.0102;
      double routerL = 0.013;

      double lenH = 0.04;
      double lenL = 0.045;

      FemModel3d femH = FemFactory.createHexTube (
         null, lenH, rinnerH, routerH, /*nt*/20, /*nl*/10, /*nr*/2);
      ArrayList<FemNode3d> nodesH = findRadialNodes (femH, rinnerH);
      femH.transformGeometry (new RigidTransform3d (0, 0, 0.005));

      NodeNumberWriter.write (datadir+"innerCartAttach.txt", nodesH);
      GmshWriter.write (datadir+"innerCart.gmsh", femH);

      FemModel3d femL = FemFactory.createPartialHexTube (
         null, lenL, rinnerL, routerL, 3*Math.PI/2, /*nl*/11, /*nr+1*/3,/*nt*/17);
      ArrayList<FemNode3d> nodesL = findRadialNodes (femL, routerL);
      femL.transformGeometry (new RigidTransform3d (0, 0, 0.005));

      NodeNumberWriter.write (datadir+"outerCartAttach.txt", nodesL);
      GmshWriter.write (datadir+"outerCart.gmsh", femL);
   }
}
