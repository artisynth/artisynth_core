/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.util.*;
import java.io.*;

import argparser.ArgParser;
import argparser.StringHolder;
import argparser.BooleanHolder;

import maspack.matrix.*;
import maspack.geometry.*;
import maspack.geometry.io.*;

/**
 * Base class for mesh conversion applications.
 */
public class FlipMeshNormals {

   protected ArgParser myArgParser;
   protected StringHolder myOutputName;
   protected StringHolder myInputName;
   protected BooleanHolder myPointMesh = new BooleanHolder (false);

   public FlipMeshNormals () {

      myOutputName = new StringHolder();
      myInputName = new StringHolder();
      String synopsis = "[options] <inputFile>";
      myArgParser = new ArgParser (synopsis);
      myOutputName = new StringHolder();
      myArgParser.addOption ("-out %s #output file name", myOutputName);    
      myArgParser.addOption (
         "-pointMesh %v #input is a point mesh", myPointMesh); 
   }

   public void convert () throws IOException {
      GenericMeshReader reader = new GenericMeshReader(myInputName.value);
      MeshBase mesh = null;
      if (myPointMesh.value) {
         mesh = new PointMesh();
      }
      mesh = reader.readMesh(mesh);
      if (mesh.hasExplicitNormals()) {
         ArrayList<Vector3d> normals = mesh.getNormals();      
         if (normals == null || normals.size() == 0) {
            System.out.println (
            "Warning: mesh does not have normals; no conversion performed");
            System.exit(0);
         }
         for (int i=0; i<normals.size(); i++) {
            normals.get(i).negate();
         }
      }
      GenericMeshWriter writer = new GenericMeshWriter(myOutputName.value);
      writer.setFormat (reader);
      writer.writeMesh (mesh);
      writer.close();
   }

   public void parseArgs (String[] args) {

      int idx = 0;
      while (idx < args.length) {
         try {
            idx = myArgParser.matchArg (args, idx);
            if (myArgParser.getUnmatchedArgument() != null) {
               String fileName = myArgParser.getUnmatchedArgument();
               if (myInputName.value == null) {
                  myInputName.value = fileName;
               }
               else {
                  System.out.println ("Ignoring extra argument "+fileName);
               }
            }
         }
         catch (Exception e) {
            // malformed or erroneous argument
            myArgParser.printErrorAndExit (e.getMessage());
         }
      } 
      if (myInputName.value == null) {
         myArgParser.printErrorAndExit ("Error: no input file specified");
      }
      if (myOutputName.value == null) {
         myOutputName.value = (new File(myInputName.value)).getName();
      }
   }

   public static void main (String[] args) {

      FlipMeshNormals converter = new FlipMeshNormals();

      converter.parseArgs (args);
      try {
         converter.convert();
      }
      catch (Exception e) {
         e.printStackTrace(); 
         System.exit(1);
      }
   }

}
