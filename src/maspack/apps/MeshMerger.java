/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.io.*;
import java.util.ArrayList;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshBase;
import maspack.geometry.PointMesh;
import maspack.geometry.io.*;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderableUtils;
import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.DoubleHolder;
import argparser.StringHolder;

/**
 * Simple program to read in a bunch of meshes and combine them
 * into a single mesh.
 */
public class MeshMerger {
   static StringHolder formatStr = new StringHolder ("%.8g");
   static ArrayList<String> inFileNames = new ArrayList<String>();
   static StringHolder outFileName = new StringHolder (null);
   static BooleanHolder pointMesh = new BooleanHolder (false);

   public static void main (String[] args) {
      ArgParser parser = new ArgParser ("[options] <infileNames> ...");
      //parser.addOption ("-inFile %s #input file name", inFileName);
      parser.addOption ("-out %s #name for output file", outFileName);
      parser.addOption (
         "-pointMesh %v #meshes are assumed to be point meshes", pointMesh);
      parser.addOption (
         "-format %s #printf-syle format string for vertex output", formatStr);

      int idx = 0;
      while (idx < args.length) {
         try {
            idx = parser.matchArg (args, idx);
            if (parser.getUnmatchedArgument() != null) {
               inFileNames.add (parser.getUnmatchedArgument());
            }
         }
         catch (Exception e) {
            // malformed or erroneous argument
            parser.printErrorAndExit (e.getMessage());
         }
      }      

      if (inFileNames.size() == 0) {
         parser.printErrorAndExit ("input file name(s) missing");
      }
      if (outFileName.value == null) {
         parser.printErrorAndExit ("out file name missing");
      }
      
      try {
         MeshBase mesh = null;
         if (pointMesh.value) {
            mesh = new PointMesh();
         }
         else {
            mesh = new PolygonalMesh ();
         }

         for (int i=0; i<inFileNames.size(); i++) {
            File file = new File (inFileNames.get(i));
            if (!file.canRead()) {
               System.out.println (
                  "Warning: mesh file " + file.getName() +
                  " not found or not reeadable");
            }            
            else {
               if (pointMesh.value) {
                  PointMesh m =
                     (PointMesh)GenericMeshReader.readMesh (
                        file, new PointMesh());
                  ((PointMesh)mesh).addMesh (m);
               }
               else {
                  PolygonalMesh m = new PolygonalMesh (file);
                  ((PolygonalMesh)mesh).addMesh (m);
               }
            }
         }
         if (pointMesh.value) {
            GenericMeshWriter.writeMesh (new File(outFileName.value), mesh);
         }
         else {
            ((PolygonalMesh)mesh).write (
               new File(outFileName.value), formatStr.value, false);
         }

      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
   }
}
