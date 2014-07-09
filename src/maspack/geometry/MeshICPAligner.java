/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.geometry.MeshICP.AlignmentType;

public class MeshICPAligner {

   public static void usage() {
      System.out.println("Usage:  java " + MeshICPAligner.class.getName() + " <target mesh> <input mesh> [<output mesh>]");
   }
  
   public void doAlignment(File base, File in, OutputStream out) {
     
      PolygonalMesh mesh1, mesh2, mesh3;
      try {
         mesh1 = new PolygonalMesh(base);
         mesh2 = new PolygonalMesh(in);
         mesh3 = new PolygonalMesh();
      } catch (IOException e) {
         e.printStackTrace();
         return;
      }
      
      // do alignment
      AffineTransform3d trans = MeshICP.align(mesh1, mesh2, 
         AlignmentType.RIGID_WITH_SCALING, 1e-12, 100, mesh3);
      
      try {
         // mesh3.write(new PrintWriter(out), "%g");
         trans.write(new PrintWriter(out), "%g", null);
         out.flush();
      } catch (IOException e) {
         e.printStackTrace();
      }  
      
      if (out != System.out) {
         try {
            out.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }
   
   public static void main(String[] args) {    
      
      if (args.length<2) {
         usage();
         return;
      }
      
      File file1 = new File(args[0]);
      File file2 = new File(args[1]);
      OutputStream out = System.out;
      if (args.length > 2 ) {
         try {
            out = new FileOutputStream(new File(args[2]));
         } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
         }
      }
      
      MeshICPAligner aligner = new MeshICPAligner();
      aligner.doAlignment(file1, file2, out);
      
   }
}
