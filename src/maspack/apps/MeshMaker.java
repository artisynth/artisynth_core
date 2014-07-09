/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import argparser.ArgParser;
import argparser.DoubleHolder;
import argparser.IntHolder;
import argparser.StringHolder;

/**
 * An application front end to MeshFactory.
 */
public class MeshMaker {
   static IntHolder nslices = new IntHolder (16);
   static DoubleHolder radius = new DoubleHolder (1.0);
   static DoubleHolder wx = new DoubleHolder (1.0);
   static DoubleHolder wy = new DoubleHolder (1.0);
   static DoubleHolder wz = new DoubleHolder (1.0);
   static StringHolder fileName = new StringHolder ("mesh.obj");

   public static void main (String[] args) {

      ArgParser parser = new ArgParser ("java maspack.geometry.MeshMaker");
      parser.addOption ("-file %s #mesh file name", fileName);
      parser.addOption ("-radius %f #radius", radius);
      parser.addOption ("-wx %f #x width", wx);
      parser.addOption ("-wy %f #y width", wy);
      parser.addOption ("-wz %f #z width", wz);
      parser.addOption ("-nslices %d #number of slices", nslices);

      args = parser.matchAllArgs (args, 0, ArgParser.EXIT_ON_ERROR);

      PolygonalMesh mesh = null;
      if (args[0].equals ("sphere")) {
         mesh = MeshFactory.createQuadSphere (radius.value, nslices.value);
      }
      else if (args[0].equals ("box")) {
         mesh = MeshFactory.createQuadBox (wx.value, wy.value, wz.value);
      }
      else if (args[0].equals ("cylinder")) {
         mesh =
            MeshFactory.createCylinder (radius.value, wz.value, nslices.value);
      }
      else if (args[0].equals ("roundedCylinder")) {
         mesh =
            MeshFactory.createQuadRoundedCylinder (
               radius.value, wz.value, nslices.value, 
               /*nsegs=*/1, /*flatBottom=*/false);
      }
      else {
         System.err.println ("Unknown mesh type: " + args[0]);
         System.exit (1);
      }
      try {
         mesh.write (new PrintWriter (new BufferedWriter (
            new FileWriter (fileName.value))), "%10.5f");
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
   }
}
