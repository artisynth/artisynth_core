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

public class GtsToObj extends MeshConverter {
   private static final long serialVersionUID = 1L;

   public GtsToObj () {
      super ("gts", "obj");
   }

   public void convert (File inputFile, File outputFile) throws IOException {

      GtsReader reader = new GtsReader (inputFile);
      MeshBase mesh = reader.readMesh ();
      WavefrontWriter writer = new WavefrontWriter (outputFile);
      writer.writeMesh (mesh);
   }

   public static void main (String[] args) {

      GtsToObj converter = new GtsToObj();
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
