package maspack.apps;

import java.io.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;

import maspack.geometry.io.*;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.render.*;
import maspack.render.Renderer.Shading;
import maspack.properties.*;

import quickhull3d.QuickHull3D;

public class CreateConvexHull {

   void printUsageAndExit (int status) {
      System.out.println (
         "Usage: java "+getClass()+" -input <infile> [ -output <outfile>]");
      System.exit(status);
   }

   public static void main (String[] args) {

      CreateConvexHull creator = new CreateConvexHull();

      String inputFile = null;
      String outputFile = null;
      
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-input")) {
            if (++i == args.length) {
               System.out.println (
                  "WARNING: option '-input' needs an additional argument");
            }
            else {
               inputFile = args[i];
            }
         }
         else if (args[i].equals ("-output")) {
            if (++i == args.length) {
               System.out.println (
                  "WARNING: option '-output' needs an additional argument");
            }
            else {
               outputFile = args[i];
            }
         }
         else if (args[i].equals ("-help")) {
            creator.printUsageAndExit (0);
         }
         else {
            creator.printUsageAndExit (1);
         }
      }

      if (inputFile == null) {
         creator.printUsageAndExit (1);
      }
      MeshBase mesh = null;
      try {
         GenericMeshReader reader = new GenericMeshReader(inputFile);
         mesh = reader.readMesh();
      }
      catch (IOException e) {
         System.out.println ("Can't read mesh from '"+inputFile+"':");
         e.printStackTrace(); 
         System.exit(1); 
      }

      PolygonalMesh hull = MeshFactory.createConvexHull (mesh);
      PrintWriter pw;
      try {
         if (outputFile != null) {
            pw = new PrintWriter (
               new BufferedWriter (new FileWriter (outputFile)));
         }
         else {
            pw =  new PrintWriter (
               new BufferedWriter (new OutputStreamWriter (System.out)));
         }
         hull.write (pw, "%g");
      }
      catch (IOException e) {
         System.out.println ("Can't write hull mesh:");
         e.printStackTrace(); 
      }
   }
}



