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

public class XyzToXyzb extends MeshConverter {
   private static final long serialVersionUID = 1L;

   public XyzToXyzb () {
      super ("xyz", "xyzb");
   }

   public void convert (File inputFile, File outputFile) throws IOException {

      XyzReader reader = new XyzReader(inputFile);
      MeshBase mesh = reader.readMesh ();
      XyzbWriter writer = new XyzbWriter(outputFile);
      writer.writeMesh (mesh);
   }

   public static void main (String[] args) {

      XyzToXyzb converter = new XyzToXyzb();
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
