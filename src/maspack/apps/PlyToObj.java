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

public class PlyToObj extends MeshConverter {
   private static final long serialVersionUID = 1L;

   public PlyToObj () {
      super ("ply", "obj");
   }

   public void convert (File inputFile, File outputFile) throws IOException {

      PlyReader reader = new PlyReader(inputFile);
      MeshBase mesh = reader.readMesh ();
      WavefrontWriter writer = new WavefrontWriter(outputFile);
      writer.writeMesh (mesh);
   }

   public static void main (String[] args) {

      PlyToObj converter = new PlyToObj();
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
