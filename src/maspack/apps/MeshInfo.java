/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.io.*;
import java.util.*;

import maspack.util.*;
import maspack.geometry.MeshBase;
import maspack.geometry.PointMesh;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.OBB;
import maspack.geometry.io.*;
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
      System.out.println ("-edgeLength: print average edge length");
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
      OBB obb = new OBB();
      obb.set (mesh, 0, OBB.Method.Points);
      obb.getWidths(tmp);
      System.out.println ("obbWidths= " + tmp.toString ("%8.3f"));
      System.out.println (
         "obbCenter= " + obb.getTransform().p.toString ("%8.3f"));
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

   public MeshBase getMesh (File inputFile) throws IOException {

      MeshBase mesh = null;
      try {
         mesh = GenericMeshReader.readMesh (inputFile);
      }
      catch (UnsupportedOperationException e) {
         System.out.println (
            "Extension for mesh "+inputFile.getName()+" not recognized");
      }
      return mesh;
   }   

   public void testMesh (File inputFile) throws IOException {

      String suffix = getExtension (inputFile.getName());

      if (suffix.equals ("obj")) {
         PolygonalMesh mesh = new PolygonalMesh (inputFile);
         int nump;
         int numd;

         double vol = mesh.computeVolume();

         String errMsg = null;
         if (!mesh.isManifold()) {
            errMsg = "not manifold";
         }
         else if (!mesh.isClosed()) {
            errMsg = "not closed";
         }
         else if ((nump = numParts(mesh)) > 0) {
            errMsg = "has "+nump+" disconnected parts";
         }
         else if ((numd=mesh.numDegenerateFaces()) > 0) {
            errMsg = "has "+numd+" degenerate faces";
         }
         if (errMsg != null) {
            System.out.println ("FAILED: "+inputFile.getName()+" "+errMsg);
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
      
      MeshBase mesh = getMesh (inputFile);
      if (mesh instanceof PolygonalMesh) {
         PolygonalMesh pmesh = (PolygonalMesh)mesh;

         System.out.println ("meshType= " + mesh.getClass());
         System.out.println ("numVertices= " + pmesh.numVertices());
         System.out.println ("numFaces= " + pmesh.numFaces());
         System.out.println ("isClosed= " + pmesh.isClosed());
         System.out.println ("isManifold= " + pmesh.isManifold());
         System.out.println ("volume= " + pmesh.computeVolume());
         System.out.println ("area= " + pmesh.computeArea());
         System.out.println ("averageEdgeLength= " +
                             pmesh.computeAverageEdgeLength());
         doPrintBounds (pmesh);
      }
      else if (mesh != null) {
         System.out.println ("meshType= " + mesh.getClass());
         System.out.println ("numVertices= " + mesh.numVertices());
         doPrintBounds (mesh);
      }
   }

   private enum Command {
      PrintInfo,
      RunTest,
      PrintFaces,
      PrintVertices,
      PrintEdgeLength
   };

   public static void main (String[] args) {

      ArrayList<String> inputNames = new ArrayList<String>();
      ArrayList<Command> cmds = new ArrayList<Command>();

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-help")) {
            printUsageAndExit();
         }
         else if (!args[i].startsWith ("-")) {
            inputNames.add (args[i]);
         }
         else if (args[i].equals ("-test")) {
            cmds.add (Command.RunTest);
         }
         else if (args[i].equals ("-numFaces")) {
            cmds.add (Command.PrintFaces);
         }
         else if (args[i].equals ("-numVertices")) {
            cmds.add (Command.PrintVertices);
         }
         else if (args[i].equals ("-edgeLength")) {
            cmds.add (Command.PrintEdgeLength);
         }
         else {
            printUsageAndExit();
         }
      }
      if (inputNames.size() == 0) {
         System.out.println ("Error: no input files specified");
         printUsageAndExit();
      }
      if (cmds.size() == 0) {
         cmds.add (Command.PrintInfo);
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
            for (Command cmd : cmds) {
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
                     MeshBase mesh;
                     if ((mesh = info.getMesh (inputFile)) != null) {
                        if (mesh instanceof PolygonalMesh) {
                           System.out.println (((PolygonalMesh)mesh).numFaces());
                        }
                        else {
                           System.out.println ("Mesh is a " + mesh.getClass());
                        }
                     }
                     break;
                  }
                  case PrintVertices: {
                     MeshBase mesh;
                     if ((mesh = info.getMesh (inputFile)) != null) {
                        System.out.println (mesh.numVertices());
                     }
                     break;
                  }
                  case PrintEdgeLength: {
                     MeshBase mesh;
                     if ((mesh = info.getMesh (inputFile)) != null) {
                        if (mesh instanceof PolygonalMesh) {
                           System.out.println (
                              ((PolygonalMesh)mesh).computeAverageEdgeLength());
                        }
                        else {
                           System.out.println ("Mesh is a " + mesh.getClass());
                        }
                     }
                     break;
                  }
                  default: {
                     throw new InternalErrorException (
                        "Unimplemented command " + cmd);
                  }
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
