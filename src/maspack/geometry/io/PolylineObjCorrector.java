package maspack.geometry.io;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * 
 * @author antonio
 * 
 * Fixes the Polyline OBJ files that were sent from Benjamin to a true OBJ
 * format, re-assembling lines if possible
 * 
 */
public class PolylineObjCorrector {

   private static class Line {
      ArrayList<Integer> vtxInds = new ArrayList<Integer>();

      public Line (int a, int b) {
         vtxInds.add(a);
         vtxInds.add(b);
      }

      @SuppressWarnings("unused")
      public boolean canAttach(int a, int b) {
         if (vtxInds.get(0) == a || vtxInds.get(vtxInds.size() - 1) == a
            || vtxInds.get(0) == b || vtxInds.get(vtxInds.size() - 1) == b) {
            return true;
         }
         return false;
      }

      public boolean attach(int a, int b) {

         if (vtxInds.get(0) == a) {
            vtxInds.add(0, b);
         } else if (vtxInds.get(vtxInds.size() - 1) == a) {
            vtxInds.add(b);
         } else if (vtxInds.get(0) == b) {
            vtxInds.add(0, a);
         } else if (vtxInds.get(vtxInds.size() - 1) == b) {
            vtxInds.add(a);
         } else {
            return false;
         }

         return true;
      }

      public ArrayList<Integer> getVertexIndices() {
         return vtxInds;
      }

      @SuppressWarnings("unused")
      public void clearVertexIndices() {
         vtxInds.clear();
      }

   }

   public static void fixFile(String inFilename, String outFilename) {

      File inFile = new File(inFilename);

      if (inFilename.equals(outFilename)) {
         System.out.println("Creating backup file '" + inFilename + ".bak'");
         inFile.renameTo(new File(inFilename + ".bak"));
         inFile = new File(inFilename + ".bak");
      }

      try {

         FileInputStream fstream = new FileInputStream(inFile);
         DataInputStream in = new DataInputStream(fstream);
         BufferedReader fin = new BufferedReader(new InputStreamReader(in));
         PrintStream out = null;

         // output stream
         if (outFilename == null) {
            out = System.out;
         } else {
            try {
               out = new PrintStream(outFilename);
            } catch (IOException e) {
               e.printStackTrace();
               return;
            } finally {
               fin.close();
            }
         } 

         String strLine;
         ArrayList<Line> myLines = new ArrayList<Line>();

         while ((strLine = fin.readLine()) != null) {
            boolean lineLine = false;
            if (strLine.trim().startsWith("f")) {
               int[] faceIdxs = parseIntArray(strLine);
               if (faceIdxs.length == 2) {
                  // try to build a line
                  addToLineSet(faceIdxs, myLines);
                  lineLine = true;
               }
            }

            if (!lineLine) {
               out.println(strLine);
            }

         }

         // now print all lines
         for (Line line : myLines) {
            strLine = "l";
            for (Integer i : line.getVertexIndices()) {
               strLine += " " + i;
            }
            out.println(strLine);
         }

         fin.close();
         if (out != System.out) {
            out.close();
         }

      } catch (Exception e) {
         System.err.println("Error: " + e.getMessage());
      }
   }

   private static void addToLineSet(int[] idxs, ArrayList<Line> lineList) {

      for (Line line : lineList) {
         if (line.attach(idxs[0], idxs[1])) {
            return;
         }
      }

      // no line can attach the points
      Line newLine = new Line(idxs[0], idxs[1]);
      lineList.add(newLine);

   }

   public static int[] parseIntArray(String str) {

      String[] strArray = str.split("\\s+");
      int countInts = 0;
      for (int i = 0; i < strArray.length; i++) {
         try {
            Integer.parseInt(strArray[i]);
            countInts++;
         } catch (NumberFormatException e) {
         }
      }

      int[] out = new int[countInts];
      countInts = 0;
      for (int i = 0; i < strArray.length; i++) {
         try {
            out[countInts] = Integer.parseInt(strArray[i]);
            countInts++;
         } catch (NumberFormatException e) {
         }
      }

      return out;

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

      fixFile(args[0], outfile);

   }

}
