/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.File;
import java.io.IOException;

import maspack.matrix.ScaledRigidTransform3d;

public class CPDRigidAligner {
   
   public static void main(String[] args) {    
      
      String file1 = args[0];
      String file2 = args[1];
      PolygonalMesh mesh1, mesh2;
      try {
         mesh1 = new PolygonalMesh(new File(file1));
         mesh2 = new PolygonalMesh(new File(file2));
      } catch (IOException e) {
         e.printStackTrace();
         return;
      }
      
      ScaledRigidTransform3d trans = CPD.rigid(mesh1, mesh2, 0, 0, 100, false);
      System.out.println(trans.toString());

   }
   
}
