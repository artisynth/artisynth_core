/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import maspack.matrix.Point3d;
import maspack.util.ReaderTokenizer;

public class MayaToWavefront {

   private static String getMayaName(String line) {
      int iPos = line.indexOf("-n");
      if (iPos >= 0) {
         line = line.substring(iPos);
         int startQuot = line.indexOf('"');
         int endQuot = line.indexOf('"', startQuot + 1);

         if ((startQuot > -1) && (endQuot > -1)) {
            return line.substring(startQuot + 1, endQuot);
         }
      }
      return null;
   }

   public static void doFiberExport(String inputFile, String outputFile) {

      PrintStream out;
      ReaderTokenizer rtok;

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
      }

      // input stream
      try {
         rtok = new ReaderTokenizer(new FileReader(inputFile));
         rtok.eolIsSignificant(true);
      }
      catch (FileNotFoundException e) {
         e.printStackTrace();
         return;
      }

      Date date = new Date();
      DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
      out.println("# Exported OBJ file from '" + inputFile + "', "
         + dateFormat.format(date));
      out.println();

      int nVertices = 0;

      try {

         while (rtok.ttype != ReaderTokenizer.TT_EOF) {

            String line = readLine(rtok);

            // find nurbsCurve
            if (line.contains("createNode nurbsCurve")) {

               String curveName = getMayaName(line);

               // throw away lines starting with setAttr
               rtok.nextToken();
               while (rtok.ttype == ReaderTokenizer.TT_WORD
                  || rtok.ttype == ReaderTokenizer.TT_EOL) {
                  toEOL(rtok);
                  if (rtok.ttype == ReaderTokenizer.TT_EOL) {
                     rtok.nextToken();
                  }
               }
               // throw away next line (form: 1 17 0 no 3)
               toEOL(rtok);

               // read indexing
               int nSkip = (int)(rtok.scanNumber());
               int skipped = 0;

               // throw away numbers
               while (skipped < nSkip) {
                  rtok.nextToken();
                  if (rtok.ttype == ReaderTokenizer.TT_NUMBER) {
                     skipped++;
                  }
                  else if (rtok.ttype != ReaderTokenizer.TT_EOL) {
                     System.err.println("Error: unexpected token "
                        + rtok.tokenName() + " on line " + rtok.lineno());
                     return;
                  }
               }

               // read entire curve
               int nPoints = (int)(scanNumber(rtok));
               ArrayList<Point3d> fiber = new ArrayList<Point3d>();
               double pnt[] = new double[3];
               for (int i = 0; i < nPoints; i++) {

                  int nRead = scanNumbers(rtok, pnt, 3);
                  if (nRead < 3) {
                     System.err
                        .println("Error: cannot read coordinate on line "
                           + rtok.lineno());
                     return;
                  }

                  fiber.add(new Point3d(pnt));

               }

               // print fiber to file
               String fiberVertices = "l";
               if (curveName != null) {
                  out.println("# " + curveName);
               }
               for (Point3d vtx : fiber) {
                  nVertices++;
                  fiberVertices += " " + nVertices;
                  out.printf("v %f %f %f\n", vtx.x, vtx.y, vtx.z);
               }
               out.println(fiberVertices);
               out.println();

            }

         }
      }
      catch (IOException e) {
         e.printStackTrace();
         out.close();
         return;
      }

      if (out != System.out) {
         out.close();
      }

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

   // read numbers, skipping over newlines
   public static int scanNumbers(ReaderTokenizer rtok, double val[],
      int maxCount) throws IOException {

      int readCount = 0;
      while (true) {
         rtok.nextToken();
         if (rtok.ttype == ReaderTokenizer.TT_NUMBER) {
            val[readCount] = rtok.nval;
            readCount++;

            // if anything else other than number or EOL, then break
         }
         else if (rtok.ttype != ReaderTokenizer.TT_EOL) {
            break;
         }
         if (readCount == maxCount) {
            break;
         }
      }
      return readCount;
   }

   protected static int nextToken(ReaderTokenizer rtok) throws IOException {
      rtok.nextToken();
      return rtok.ttype;
   }

   private static void toEOL(ReaderTokenizer rtok) throws IOException {
      while ((rtok.ttype != ReaderTokenizer.TT_EOL)
         && (rtok.ttype != ReaderTokenizer.TT_EOF)) {
         nextToken(rtok);
      }
   }

   protected static String readLine(ReaderTokenizer rtok) throws IOException {

      Reader rtokReader = rtok.getReader();
      String line = "";
      int c;
      while (true) {
         c = rtokReader.read();

         if (c < 0) {
            rtok.ttype = ReaderTokenizer.TT_EOF;
            return line;
         }
         else if (c == '\n') {
            rtok.setLineno(rtok.lineno() + 1); // increase line number
            rtok.ttype = ReaderTokenizer.TT_EOL;
            break;
         }
         line += (char)c;
      }

      return line;
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

      doFiberExport(args[0], outfile);

   }

}
