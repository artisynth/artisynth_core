package artisynth.demos.test;

import java.io.*;
import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import artisynth.core.probes.*;
import maspack.geometry.*;
import maspack.interpolation.Interpolation;
import maspack.geometry.io.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.PathFinder;
import maspack.render.Renderer.Shading;
import maspack.properties.*;

public class MeshBodyTest extends RootModel {

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


      String outerOld = new String (
         "v  1  1  0.5\n" +
         "v  0  3  0.5\n" +
         "v -1  1  0.5\n" +
         "v -2 -1  0.5\n" +
         "v  0 -1  0.5\n" +
         "v  2 -1  0.5\n" +
         "v  1  1 -0.5\n" +
         "v  0  3 -0.5\n" +
         "v -1  1 -0.5\n" +
         "v -2 -1 -0.5\n" +
         "v  0 -1 -0.5\n" +
         "v  2 -1 -0.5\n" +

         "f 0 2 4\n" + 
         "f 0 1 2\n" + 
         "f 2 3 4\n" + 
         "f 0 4 5\n" + 

         "f 3 10 4\n" +
         "f 3 9 10\n" +
         "f 5 4 10\n" +
         "f 5 10 11\n" +

         "f 2 1 8\n" +
         "f 1 7 8\n" +
         "f 3 2 8\n" +
         "f 3 8 9\n" +

         "f 0 5 6\n" +
         "f 5 11 6\n" +
         "f 0 6 1\n" +
         "f 1 6 7\n" +

         "f 6 8 7\n" + 
         "f 6 10 8\n" + 
         "f 8 10 9\n" + 
         "f 6 11 10\n");

      String tallInner = new String (
         "v  1  1  1.5\n" + 
         "v -1  1  1.5\n" + 
         "v  0 -1  1.5\n" + 
         "v  1  1  0.5\n" + 
         "v -1  1  0.5\n" + 
         "v  0 -1  0.5\n" + 
         "v  1  1 -0.5\n" + 
         "v -1  1 -0.5\n" + 
         "v  0 -1 -0.5\n" + 
         "v  1  1 -1.5\n" + 
         "v -1  1 -1.5\n" + 
         "v  0 -1 -1.5\n" + 
         "f 0 1 2\n" + 

         "f 2 1 4\n" + 
         "f 2 4 5\n" + 
         "f 0 2 5\n" + 
         "f 0 5 3\n" + 
         "f 1 0 3\n" + 
         "f 1 3 4\n" + 

         "f 5 4 7\n" + 
         "f 5 7 8\n" + 
         "f 3 5 8\n" + 
         "f 3 8 6\n" + 
         "f 4 3 6\n" + 
         "f 4 6 7\n" + 

         "f 8 7 10\n" + 
         "f 8 10 11\n" + 
         "f 6 8 11\n" + 
         "f 6 11 9\n" + 
         "f 7 6 9\n" + 
         "f 7 9 10\n" + 

         "f 9 11 10\n");

      String outerDense = new String(
            "v  2  3  0.5\n" + 
            "v  0  3  0.5\n" + 
            "v -2  3  0.5\n" + 
            "v -2 -1  0.5\n" + 
            "v  0 -3  0.5\n" + 
            "v  2 -1  0.5\n" + 
            "v  1  1  0.5\n" + 
            "v -1  1  0.5\n" + 
            "v  0 -1  0.5\n" + 
            "v  2  3 -0.5\n" + 
            "v  0  3 -0.5\n" + 
            "v -2  3 -0.5\n" + 
            "v -2 -1 -0.5\n" + 
            "v  0 -3 -0.5\n" + 
            "v  2 -1 -0.5\n" + 
            "v  1  1 -0.5\n" + 
            "v -1  1 -0.5\n" + 
            "v  0 -1 -0.5\n" + 

            "f 0 1 6\n" +
            "f 0 6 5\n" +
            "f 1 2 7\n" +
            "f 2 3 7\n" +
            "f 5 8 4\n" +
            "f 8 3 4\n" +
            "f 1 7 6\n" +
            "f 6 7 8\n" +
            "f 7 3 8\n" +
            "f 6 8 5\n" +

            "f 1 0 9\n" +
            "f 1 9 10\n" +
            "f 2 1 10\n" +
            "f 2 10 11\n" +
            "f 3 2 11\n" +
            "f 3 11 12\n" +
            "f 4 3 12\n" +
            "f 4 12 13\n" +
            "f 5 4 13\n" +
            "f 5 13 14\n" +
            "f 0 5 14\n" +
            "f 0 14 9\n" +
               
            "f 10 9 15\n" +
            "f 15 9 14\n" +
            "f 12 11 16\n" +
            "f 16 11 10\n" +
            "f 10 15 16\n" +
            "f 16 17 12\n" +
            "f 16 15 17\n" +
            "f 15 14 17\n" +
            "f 12 17 13\n" +
            "f 17 14 13\n");

      String outerSparse = new String(
            "v  2  3  0.5\n" + 
            "v  0  3  0.5\n" + 
            "v -2  3  0.5\n" + 
            "v -2 -1  0.5\n" + 
            "v  0 -3  0.5\n" + 
            "v  2 -1  0.5\n" + 
            "v  2  3 -0.5\n" + 
            "v  0  3 -0.5\n" + 
            "v -2  3 -0.5\n" + 
            "v -2 -1 -0.5\n" + 
            "v  0 -3 -0.5\n" + 
            "v  2 -1 -0.5\n" + 

            "f 0 1 5\n" +
            "f 1 2 3\n" +
            "f 5 3 4\n" +
            "f 1 3 5\n" +

            "f 1 0 6\n" +
            "f 1 6 7\n" +
            "f 2 1 7\n" +
            "f 2 7 8\n" +
            "f 3 2 8\n" +
            "f 3 8 9\n" +
            "f 4 3 9\n" +
            "f 4 9 10\n" +
            "f 5 4 10\n" +
            "f 5 10 11\n" +
            "f 0 5 11\n" +
            "f 0 11 6\n" +
               
            "f 7 6 11\n" +
            "f 9 8 7\n" +
            "f 9 11 10\n" +
            "f 7 11 9\n");
      if (false) {
         mesh = WavefrontReader.readFromString (outerDense, /*zeroIndexed=*/true);
      }
      
      if (false) {
         mesh = MeshFactory.createRoundedBox (
            4.0, 2.0, 1.0, 4, 2, 1, 20);
         mesh.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));
      }
      
      if (true) {
         String filePath = PathFinder.getSourceRelativePath (
            this, "geometry/OsCoxaeLeft_Red4.obj");
         try {
            mesh = new PolygonalMesh (filePath);
         }
         catch (Exception e) {
         }
      }
      
      if (false) {
         mesh = MeshFactory.createBox (
            3.0, 2.0, 1.0, Point3d.ZERO, 5, 4, 3, false,
            MeshFactory.FaceType.ALT_TRI);
      }
      
      if (false) {
         try {
            mesh = new PolygonalMesh (
"/home/lloyd/Shared2/phuman/boneData/skeletonJ-19/ScapulaLeft_Red1.obj");
         }
         catch (Exception e) {
            e.printStackTrace(); 
         }
      }

      RenderProps.setDrawEdges (mesh, true);
      RenderProps.setEdgeColor (mesh, Color.WHITE);
      RenderProps.setShading (mesh, Shading.SMOOTH);

      try {
         mesh.write (
            new File("mesh.obj"), "%20.12f", true);
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
     
      FixedMeshBody body = new FixedMeshBody (mesh);
      mech.addMeshBody (body);

      addBodyProbe (body);
   }

   public void addBodyProbe (FixedMeshBody body) {
      NumericInputProbe inprobe =
         new NumericInputProbe (body, "pose", 0, 10);
      inprobe.addData (0, new RigidTransform3d());
      inprobe.addData (2, new RigidTransform3d(0.2, 0.1, 0, .1, .2, .3));
      inprobe.addData (4, new double[] {2, 0, 0, 0,  0, 2, 0, 0,  0, 0, 1, 0,  0, 0, 0, 1});
      inprobe.setInterpolationOrder (Interpolation.Order.SphericalLinear);
      addInputProbe (inprobe);
   }


}



