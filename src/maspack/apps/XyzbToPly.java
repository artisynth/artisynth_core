/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.io.*;

import maspack.geometry.*;
import maspack.geometry.io.*;
import maspack.geometry.io.MeshWriter.DataFormat;
import maspack.geometry.io.PlyWriter.DataType;

public class XyzbToPly extends MeshConverter {
   private static final long serialVersionUID = 1L;

   public XyzbToPly () {
      super ("xyzb", "ply");
   }

   public void convert (File inputFile, File outputFile) throws IOException {

      XyzbReader reader = new XyzbReader (inputFile);
      MeshBase mesh = reader.readMesh();
      PlyWriter writer = new PlyWriter (outputFile);
      writer.setDataFormat (DataFormat.BINARY_LITTLE_ENDIAN);
      writer.setFloatType (DataType.FLOAT);
      writer.writeMesh (mesh);
   }

   public static void main (String[] args) {

      XyzbToPly converter = new XyzbToPly();
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
