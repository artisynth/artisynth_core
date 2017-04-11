/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import maspack.geometry.PolygonalMesh;
import maspack.util.NumberFormat;

public class ObjToMdl {

   public static void main (String[] args) {
      // TODO Auto-generated method stub
      
      if (args.length != 2) {
         System.out.println("usage: java artisynth.models.badin.ObjToMdl inputObjMeshName outputMdlMeshName");
         return;
      }
      
      try {
         PolygonalMesh mesh = new PolygonalMesh (new File(args[0])); 
         PrintWriter pw = new PrintWriter (new File(args[1]));
         MDLMeshIO.write (mesh, args[1], pw, new NumberFormat ("%g"));
         pw.close ();
      }
      catch (IOException e) {
         e.printStackTrace ();
      }

   }

}
