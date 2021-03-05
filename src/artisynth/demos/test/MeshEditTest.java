package artisynth.demos.test;

import java.io.*;
import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.modelbase.*;
import artisynth.core.renderables.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.geometry.*;
import maspack.geometry.io.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.properties.*;

public class MeshEditTest extends RootModel {

   public static String rbpath =
      ArtisynthPath.getHomeRelativePath (
         "src/maspack/geometry/sampleData/", ".");

   public void build (String[] args) {

      MechModel mech = new MechModel ("msmod");
      addModel (mech);

      // PolygonalMesh mesh = MeshFactory.createSkylineMesh (
      //    2.0, 1.0, 0.5, 5, 5,
      //    "11111",
      //    "1   1",
      //    "11121",
      //    "14141",
      //    "11131");

      PolygonalMesh mesh = MeshFactory.createPrism (
         new double[] { 1.0, 1.0, -1.0, 1.0, 0.0, -1.0 }, 1.0);

      if (false) {
         mesh = MeshFactory.createBox (
            3.0, 2.0, 1.0, Point3d.ZERO, 2, 2, 1, false,
            MeshFactory.FaceType.QUAD);
      }
      if (true) {
         double size = 10;
         mesh = MeshFactory.createPointedCylinder (size, size, size/10, 4);
      }
      

      if (args.length > 0) {
         try {
            mesh = new PolygonalMesh (args[0]);
            // WavefrontReader reader = new WavefrontReader (args[0]);
            // reader.setZeroIndexed (true);
            // mesh = reader.readMesh();
         }
         catch (Exception e) {
            System.out.println ("Can't read mesh: " + e);
         }
         System.out.println ("args[0]=" + args[0]);
      }
      

      //mesh = WavefrontReader.readFromString (outerDense, /*zeroIndexed=*/true);

      EditablePolygonalMeshComp editMesh =
         new EditablePolygonalMeshComp (mesh);

      RenderProps.setSphericalPoints (
         editMesh, 0.02*RenderableUtils.getRadius(editMesh), Color.RED);
      RenderProps.setDrawEdges (editMesh, true);
      RenderProps.setEdgeColor (editMesh, Color.CYAN);
      mech.addRenderable (editMesh);
   }
}
