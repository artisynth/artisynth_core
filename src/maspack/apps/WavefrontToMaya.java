/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import maspack.geometry.io.WavefrontReader;
import maspack.matrix.Point3d;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public class WavefrontToMaya {
   
   static String requiresMayaVersion = "2012";

   
   public static void doFiberExport(String inputFile, String outputFile) {
      doFiberExport(inputFile, outputFile, new NumberFormat());
   }
   
   public static void doFiberExport(String inputFile, String outputFile, NumberFormat fmt) {

      PrintStream out;
      WavefrontReader wfr;
      String outputFileName = "unknown";
      
      // output stream
      if (outputFile == null) {
         out = System.out;
      }
      else {
         try {
            out = new PrintStream(outputFile);
         }
         catch (IOException e) {
            e.printStackTrace();
            return;
         }
         outputFileName = outputFile.substring(outputFile.lastIndexOf(File.separator)+1);
      }

      // input stream
      
      try {
         wfr = new WavefrontReader(inputFile);
         wfr.parse();
         wfr.close();
      }
      catch (IOException e) {
         e.printStackTrace();
         return;
      }
      
      Date date = new Date();
      DateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy HH:mm:ss z");
      
      // maya header
      out.println("//Maya ASCII 2012 scene");
      out.println("//Name: " + outputFileName);
      out.println("//Last modified: " + dateFormat.format(date));
      out.println("//Codeset: 1252");
      out.println("requires maya \""+requiresMayaVersion + "\";");

      int lineNum = 0;
      
      for (String groupName : wfr.getPolylineGroupNames()) {
         wfr.setGroup (groupName);
         ArrayList<Point3d> vtxList = new ArrayList<Point3d>();
         int[][] indices;
         try {
            indices = wfr.getLocalLineIndicesAndVertices (vtxList);
         } catch (IOException e) {
            e.printStackTrace();
            continue;
         }
         
         for (int i=0; i<indices.length; i++) {
            lineNum++;
            String lineStr = stringifyPolyline(indices[i], vtxList, "curve" + lineNum, fmt);
            out.print(lineStr);
         }
      }
      
      // maya footer
      out.println("// End of " + outputFileName);
      
      if (out != System.out) {
         out.flush();
         out.close();
      }

   }
   
   private static String stringifyPolyline(int [] vtxIndices, ArrayList<Point3d> vtxList, String name, NumberFormat fmt) {
      
      if (fmt == null) {
         fmt = new NumberFormat();
      }
      
      String lineStr = "";
      String transname = name + "trans";
      
      lineStr += "createNode transform -n \"" + transname + "\";\n";
      lineStr += "createNode nurbsCurve -n \"" + name + "\";\n";
      lineStr += "\tsetAttr -k off \".v\";\n";
      lineStr += "\tsetAttr \".cc\" -type \"nurbsCurve\"\n";
      
      int nverts = vtxIndices.length;
      
      lineStr += "\t\t1 " + (nverts-1) + " 0 no 3\n";
      lineStr += "\t\t" + nverts;
      for (int i=0; i<nverts; i++) {
         lineStr += " " + i;
      }
      lineStr += "\n\t\t" + nverts + "\n";
      
      // vertex locations
      for (int i=0; i<nverts; i++) {
         Point3d pos = vtxList.get(vtxIndices[i]);
         lineStr += "\t\t" + fmt.format(pos.x) + " " + fmt.format(pos.y) + " " + fmt.format(pos.z) + "\n";
      }
      lineStr += "\t\t;\n";
      
      return lineStr;
   }

   // read numbers, skipping over newlines
   public static double scanNumber(ReaderTokenizer rtok) throws IOException {

      while (rtok.nextToken() == ReaderTokenizer.TT_EOL) {
      }
      if (rtok.ttype != ReaderTokenizer.TT_NUMBER) {
         throw new IOException("expected a number, got " + rtok.tokenName()
            + ", line " + rtok.lineno());
      }

      return rtok.nval;
   }

   public static void main(String[] args) {

      String outfile = null;
      String fmt = "%g";
      
      if (args.length < 1) {
         System.out.println("arguments: input_file [output_file] [number_format]");
         return;
      }
      if (args.length > 1) {
         outfile = args[1];
      }
      if (args.length > 2) {
         fmt = args[2];
      }

      doFiberExport(args[0], outfile, new NumberFormat(fmt));

   }

}
