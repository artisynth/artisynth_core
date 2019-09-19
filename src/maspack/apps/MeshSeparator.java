/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.io.*;

import maspack.geometry.PolygonalMesh;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.util.NumberFormat;
import maspack.util.ArraySort;
import maspack.render.RenderableUtils;
import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.DoubleHolder;
import argparser.StringHolder;

/**
 * Simple program to read in a mesh and separate it into disconnected parts.
 * Each part is then written out as a separate mesh file.  If there are no
 * disconnected parts, the program does nothing.
 */
public class MeshSeparator {
   static StringHolder formatStr = new StringHolder ("%.8g");
   static StringHolder inFileName = new StringHolder (null);
   static StringHolder outFileName = new StringHolder (null);

   public static void main (String[] args) {
      ArgParser parser = new ArgParser ("[options] <infileName>");
      //parser.addOption ("-inFile %s #input file name", inFileName);
      parser.addOption ("-out %s #base name for output file", outFileName);
      parser.addOption (
         "-format %s #printf-syle format string for vertex output", formatStr);

      int idx = 0;
      while (idx < args.length) {
         try {
            idx = parser.matchArg (args, idx);
            if (parser.getUnmatchedArgument() != null) {
               String fileName = parser.getUnmatchedArgument();
               if (inFileName.value == null) {
                  inFileName.value = fileName;
               }
               else {
                  System.out.println ("Ignoring extra input file "+fileName);
               }
            }
         }
         catch (Exception e) {
            // malformed or erroneous argument
            parser.printErrorAndExit (e.getMessage());
         }
      }      

      if (inFileName.value == null) {
         parser.printErrorAndExit ("input file name missing");
      }
      if (outFileName.value == null) {
         int dotIdx = inFileName.value.lastIndexOf ('.');
         if (dotIdx == -1) {
            outFileName.value = inFileName.value;
         }
         else {
            outFileName.value = inFileName.value.substring (0, dotIdx);
         }
      }

      try {
         File meshFile = new File (inFileName.value);

         if (!meshFile.exists()) {
            System.out.println (
               "Error: mesh file " + meshFile.getName() + " not found");
            meshFile = null;
            System.exit (1);
         }

         PolygonalMesh mesh = new PolygonalMesh (meshFile);

         PolygonalMesh[] meshes = mesh.partitionIntoConnectedMeshes();
         if (meshes == null) {
            System.out.println ("Mesh "+meshFile+" has no separate components");
            System.exit(0);
         }

         double numv[] = new double[meshes.length];
         int idxs[] = new int[meshes.length];
         for (int i=0; i<meshes.length; i++) {
            numv[i] = meshes[i].numVertices();
            idxs[i] = i;
         }
         ArraySort.quickSort (numv, idxs);
         
         for (int i=0; i<meshes.length; i++) {
            NumberFormat fmt = new NumberFormat ("%03d");
            File outFile = new File (
               outFileName.value + "_" + fmt.format(meshes.length-1-i) + ".obj");
            meshes[idxs[i]].write (outFile, formatStr.value, false);
         }
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
   }
}
