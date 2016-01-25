/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.*;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;

public class MeshInfo {
   private static final long serialVersionUID = 1L;

   private static void printUsageAndExit() {
      System.out.println ("Arguments: [options] <meshFile.obj> ...");
      System.out.println (" where options are");
      System.out.println ("-help: prints this message");
      System.out.println ("-test: test that mesh is clean and well formed");
      System.out.println ("-numFaces: print the number of faces");
      System.out.println ("-numVertices: print the number of vertices");
      System.exit(1);
   }

   private static void doPrintBounds (MeshBase mesh) {
      double inf = Double.POSITIVE_INFINITY;
      Point3d max = new Point3d (-inf, -inf, -inf);
      Point3d min = new Point3d ( inf,  inf,  inf);
      Point3d tmp = new Point3d();
      mesh.updateBounds (min, max);
      tmp.sub (max, min);
      System.out.println ("boundingBoxWidths= " + tmp.toString ("%8.3f"));
      System.out.println ("boundingBoxDiameter= " + tmp.norm());
      tmp.add (max, min);
      tmp.scale (0.5);
      System.out.println ("boundingBoxCenter= " + tmp.toString ("%8.3f"));
      System.out.println ("minPoint= " + min.toString ("%.8f"));
      System.out.println ("maxPoint= " + max.toString ("%.8f"));
   }

   private String getExtension (String fileName) {
      int dotIdx = fileName.lastIndexOf ('.');
      if (dotIdx != -1) {
         return fileName.substring (dotIdx+1);
      }
      else {
         return "";
      }
   }

   private int numParts (PolygonalMesh mesh) {
      PolygonalMesh[] parts = mesh.partitionIntoConnectedMeshes();
      if (parts == null) {
         return 0;
      }
      else {
         return parts.length;
      }
   }

   public PolygonalMesh getMesh (File inputFile) throws IOException {

      String suffix = getExtension (inputFile.getName());

      if (suffix.equals ("obj")) {
         return new PolygonalMesh (inputFile);
      }
      else {
         System.out.println ("Unrecognized file extension: '"+suffix+"'");
         return null;
      }
   }

   public void testMesh (File inputFile) throws IOException {

      String suffix = getExtension (inputFile.getName());

      if (suffix.equals ("obj")) {
         PolygonalMesh mesh = new PolygonalMesh (inputFile);
         int nump;
         int numd;

         double vol = mesh.computeVolume();

         if (!mesh.isManifold()) {
            System.out.println ("FAILED: not manifold");
         }
         else if (!mesh.isClosed()) {
            System.out.println ("FAILED: not closed");
         }
         else if ((nump = numParts(mesh)) > 0) {
            System.out.println ("FAILED: has "+nump+" disconnected parts");
         }
         else if ((numd=mesh.numDegenerateFaces()) > 0) {
            System.out.println ("FAILED: has "+numd+" degenerate faces");
         }
         else {
            System.out.println ("PASSED");
         }
      }
      else {
         System.out.println ("Unrecognized file extension: '"+suffix+"'");
      }
   }

   public void printInfo (File inputFile) throws IOException {
      
      String suffix = getExtension (inputFile.getName());

      if (suffix.equals ("xyzb")) {
         PointMesh mesh = new PointMesh();
         ((PointMesh)mesh).readBinary (inputFile);
         System.out.println ("meshType= PointMesh");
         System.out.println ("numVertices= " + mesh.numVertices());
         doPrintBounds (mesh);
      }
      else if (suffix.equals ("obj")) {
         PolygonalMesh mesh = new PolygonalMesh (inputFile);
         System.out.println ("meshType= PolygonalMesh");
         System.out.println ("numVertices= " + mesh.numVertices());
         System.out.println ("numFaces= " + mesh.numFaces());
         System.out.println ("isClosed= " + mesh.isClosed());
         System.out.println ("isManifold= " + mesh.isManifold());
         System.out.println ("volume= " + mesh.computeVolume());
         System.out.println ("area= " + mesh.computeArea());
         System.out.println ("averageEdgeLength= " +
                             mesh.computeAverageEdgeLength());
         doPrintBounds (mesh);
      }
      else {
         System.out.println ("Unrecognized file extension: '"+suffix+"'");
      }
   }

   private enum Command {
      PrintInfo,
      RunTest,
      PrintFaces,
      PrintVertices 
   };

   public static void main (String[] args) {

      ArrayList<String> inputNames = new ArrayList<String>();
      Command cmd = Command.PrintInfo;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-help")) {
            printUsageAndExit();
         }
         else if (!args[i].startsWith ("-")) {
            inputNames.add (args[i]);
         }
         else if (args[i].equals ("-test")) {
            cmd = Command.RunTest;
         }
         else if (args[i].equals ("-numFaces")) {
            cmd = Command.PrintFaces;
         }
         else if (args[i].equals ("-numVertices")) {
            cmd = Command.PrintVertices;
         }
         else {
            printUsageAndExit();
         }
      }
      if (inputNames.size() == 0) {
         System.out.println ("Error: no input files specified");
         printUsageAndExit();
      }
      
      for (int i=0; i<inputNames.size(); i++) {
         String fileName = inputNames.get(i);
         File inputFile = new File (fileName);
         if (!inputFile.canRead()) {
            System.out.println ("Can't read file " + inputFile);
            continue;
         }
         MeshInfo info = new MeshInfo();

         try {
            switch (cmd) {
               case PrintInfo: {
                  System.out.println (
                     "Info for mesh "+fileName+":");
                  info.printInfo (inputFile);
                  break;
               }
               case RunTest: {
                  System.out.println (
                     "Testing mesh "+fileName+":");
                  info.testMesh (inputFile);
                  break;
               }
               case PrintFaces: {
                  PolygonalMesh mesh;
                  if ((mesh = info.getMesh (inputFile)) != null) {
                     System.out.println (mesh.numFaces());
                  }
                  break;
               }
               case PrintVertices: {
                  PolygonalMesh mesh;
                  if ((mesh = info.getMesh (inputFile)) != null) {
                     System.out.println (mesh.numVertices());
                  }
                  break;
               }
               default: {
                  throw new InternalErrorException (
                     "Unimplemented command " + cmd);
               }
            }
         }
         catch (Exception e) {
            System.out.println ("Conversion failed:");
            e.printStackTrace(); 
         }
      }
   }
}
