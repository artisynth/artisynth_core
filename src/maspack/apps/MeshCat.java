/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.io.File;
import java.util.ArrayList;

import argparser.ArgParser;
import argparser.StringHolder;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.PolylineMesh;
import maspack.geometry.io.GenericMeshReader;
import maspack.geometry.io.GenericMeshWriter;
import maspack.geometry.io.MeshReader;
import maspack.geometry.io.MeshWriter;
import maspack.geometry.io.WavefrontReader;
import maspack.geometry.io.WavefrontWriter;

/**
 * Simple program to concatenate a bunch of meshes into a single
 * file.  
 * 
 * If the output file is of type OBJ, then each input mesh 
 * is added as a separate named object "o ..." within the OBJ file.
 * If the input meshes are named, then these are transfered over
 * to the output file.  Otherwise, the filename is used as the
 * object name.
 * 
 */
public class MeshCat {
   static StringHolder formatStr = new StringHolder ("%.8g");
   static ArrayList<String> inFileNames = new ArrayList<String>();
   static StringHolder outFileName = new StringHolder (null);

   public static void main (String[] args) {
      ArgParser parser = new ArgParser ("[options] <infileNames> ...");
      //parser.addOption ("-inFile %s #input file name", inFileName);
      parser.addOption ("-out %s #name for output file", outFileName);
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
      
      // store list of meshes separately, each with own name
      ArrayList<MeshBase> meshes =new ArrayList<>();
      
      try {

         // read all meshes
         for (int i=0; i<inFileNames.size(); i++) {
            File file = new File (inFileNames.get(i));
            if (!file.canRead()) {
               System.out.println (
                  "Warning: "
                  + "mesh file " + file.getName() +
                  " not found or not reeadable");
            }            
            else {
               
               String fn = inFileNames.get (i);
               File mfile = new File(fn);
               
               MeshReader reader = GenericMeshReader.createReader (fn);
               
               if (reader instanceof WavefrontReader) {
                  WavefrontReader wreader = (WavefrontReader)reader;
                  
                  wreader.parse ();
                  wreader.close ();
                  
                  String[] polyGroups = wreader.getPolyhedralGroupNames ();
                  for (String str : polyGroups) {
                     wreader.setGroup (str);
                     PolygonalMesh mesh = new PolygonalMesh();
                     wreader.readMesh (mesh);
                     if (str == null) {
                        mesh.setName (mfile.getName ());
                     } else {
                        mesh.setName (str);
                     }
                     meshes.add (mesh);
                  }
                  
                  String[] lineGroups = wreader.getPolylineGroupNames ();
                  for (String str : lineGroups) {
                     wreader.setGroup (str);
                     PolylineMesh lmesh = new PolylineMesh ();
                     wreader.readMesh (lmesh);
                     if (str == null) {
                        lmesh.setName (mfile.getName ());
                     } else {
                        lmesh.setName (str);
                     }
                     meshes.add (lmesh);
                  }
                  
               } else {
                  
                  MeshBase mesh = reader.readMesh (null);
                  mesh.setName (mfile.getName ());
                  meshes.add (mesh);
               }
            }
         }
         
         // write mesh list
         MeshWriter writer = GenericMeshWriter.createWriter (new File(outFileName.value));
         writer.setFormat (formatStr.value);

         // if OBJ, write as separate objects
         if (writer instanceof WavefrontWriter) {
            WavefrontWriter wwriter =(WavefrontWriter)writer;
            for (MeshBase mesh : meshes) {
               wwriter.writeString ("o " + mesh.getName () + "\n");
               wwriter.writeMesh (mesh);
            }
            
         } else {
            for (MeshBase mesh : meshes) {
               writer.writeMesh (mesh);
            }
         }
         
         writer.close ();

      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
   }
}
