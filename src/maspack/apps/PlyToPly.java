/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.io.*;

import argparser.*;
import maspack.geometry.*;
import maspack.geometry.io.*;
import maspack.geometry.io.MeshWriter.DataFormat;
import maspack.geometry.io.PlyWriter.DataType;

public class PlyToPly extends MeshConverter {
   private static final long serialVersionUID = 1L;

   BooleanHolder myPointMesh = new BooleanHolder (false);
   BooleanHolder myBinary = new BooleanHolder (false);
   BooleanHolder myFloat = new BooleanHolder (false);

   public PlyToPly () {
      super ("ply", "ply");
      myArgParser.addOption (
         "-pointMesh %v #input is a point mesh", myPointMesh); 
      myArgParser.addOption (
         "-binary %v #output should be binary", myBinary); 
      myArgParser.addOption (
         "-float %v #output should use 'float' rather than 'double'", myFloat); 
   }

   public void convert (File inputFile, File outputFile) throws IOException {

      PlyReader reader = new PlyReader(inputFile);
      MeshBase mesh;
      if (myPointMesh.value) {
         mesh = reader.readMesh (new PointMesh());
      }
      else {
         mesh = reader.readMesh ();
      }
      PlyWriter writer = new PlyWriter(outputFile);
      if (myBinary.value) {
         writer.setDataFormat (PlyWriter.DataFormat.BINARY_LITTLE_ENDIAN);
      }
      if (myFloat.value) {
         writer.setFloatType (PlyWriter.DataType.FLOAT);
      }
      writer.writeMesh (mesh);
   }

   public static void main (String[] args) {

      PlyToPly converter = new PlyToPly();

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
