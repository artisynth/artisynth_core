/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.io.VtkXmlReader;
import maspack.util.NumberFormat;

public class VtkMeshToWavefrontConverter {

   public static NumberFormat DEFAULT_NUMBER_FORMAT = new NumberFormat("%.8g");

   public static void convert(String input, String output) {
      convert(input, output, new NumberFormat(DEFAULT_NUMBER_FORMAT));
   }

   public static void convert(String input, String output, NumberFormat fmt) {

      PrintStream out = null;
      // output stream
      if (output == null) {
         out = System.out;
      }
      else {
         try {
            out = new PrintStream(output);
         } catch (IOException e) {
            e.printStackTrace();
            return;
         }
      }

      PolygonalMesh mesh;
      try {
         mesh = (PolygonalMesh)VtkXmlReader.read(input);
      } catch (IOException e1) {
         e1.printStackTrace();
         return;
      }

      Date date = new Date();
      DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
      out.println("# Exported OBJ file from '" + input + "', "
         + dateFormat.format(date));
      out.println();
      
      if (mesh != null) {
         try {
            mesh.write(new PrintWriter(out), fmt, false);
         } catch (IOException e) {
            e.printStackTrace();
            return;
         }
      }

   }

   public static void main(String[] args) {

      String outfile = null;
      if (args.length < 1) {
         System.out.println("arguments: input_file [output_file]");
         return;
      }
      if (args.length > 1) {
         outfile = args[1];
      }

      convert(args[0], outfile);

   }

}
