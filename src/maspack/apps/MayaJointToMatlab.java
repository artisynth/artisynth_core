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

public class MayaJointToMatlab {
   
   public class MayaAttribute {
      public String name;
      public String type;
      public String attr;
      public boolean dashed;
   }

   private static String getMayaName(String line) {
      return getMayaProp(line, "-n", true);
   }
   
   private static String getMayaParent(String line) {
      return getMayaProp(line,"-p", true);
   }
   
   private static String getQuotedString(String str, int startIndex) {
      int start = str.indexOf('"', startIndex);
      int end = str.indexOf('"', start + 1);
      
      if ((start > -1) && (end > -1)) {
         return str.substring(start + 1, end);
      }
      
      return null;
   }
   
   private static String getMayaProp(String line, String propName, boolean quoted) {
      int iPos = line.indexOf(propName);
      if (iPos >= 0) {
         line = line.substring(iPos + propName.length());
        
         if (quoted) {
           return getQuotedString(line, 0);
         } else {
            return line;
         }
      }
      return null;
   }
   
   private static void getAttribute(String line, MayaAttribute attrib) {
      
      int idx = line.indexOf("setAttr ");
      line = line.substring(idx+8).trim();
      
      if (line.startsWith("\"")){
	 attrib.dashed = false;
	 attrib.name = getQuotedString(line, 0);
	 if (attrib.name.startsWith(".")) {
	    attrib.name = attrib.name.substring(1);  // remove period
	 }
      } else {
	 attrib.dashed = true;
	 idx = line.indexOf(' ',1);
	 attrib.name = line.substring(1, idx);
      }
      idx = line.indexOf(attrib.name) + attrib.name.length()+1;
      line = line.substring(idx);
      
      // remove type
      if (line.contains("-type")) {
	attrib.type = getQuotedString(line, line.indexOf("-type")+6);
	line = line.replace("-type \"" + attrib.type + "\"", "");
      } else {
	 attrib.type = "";
      }
      line = line.replace(";","");
      
      attrib.attr = line.trim();
      attrib.attr = attrib.attr.replace("no", "false");
      attrib.attr = attrib.attr.replace("yes", "true");
      attrib.name = attrib.name.trim();
      attrib.type = attrib.type.trim();
      
      if (attrib.attr.contains(" ") || attrib.attr.contains(",")) {
	 attrib.attr="[" + attrib.attr + "]";
      }
      
   }
   
   
   public void doJointExport(String inputFile, String outputFile) {

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
      out.println("% Exported matlab file from '" + inputFile + "', "
         + dateFormat.format(date));

      int iJoint=0;
      
      try {

         String line = readLine(rtok);
         
         while (rtok.ttype != ReaderTokenizer.TT_EOF) {

            // find joint
            if (line.contains("createNode joint")) {
               String jointName = getMayaName(line);
               String jointParent = getMayaParent(line);
               
               out.println();
               out.println(jointName + ".parent = " + jointParent + ";");
               
               line = readLine(rtok);
               while (!line.contains("createNode") && rtok.ttype != ReaderTokenizer.TT_EOF) {
        	  if (line.contains("setAttr")) {
        	     MayaAttribute attr = new MayaAttribute(); 
        	     getAttribute(line, attr);
        	     
        	     if (!attr.dashed) {
        		out.println(jointName + "." + attr.name + "= " + attr.attr + ";");
        	     }
        	  }
        	  line = readLine(rtok);
               }
               
               iJoint++;
               out.println("JointArray("+ iJoint + ").joint="+jointName+";");
               out.println("JointArray("+ iJoint + ").name=\'"+jointName+"\';");
               
            } else {
               line = readLine(rtok);
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

      MayaJointToMatlab j2m = new MayaJointToMatlab();
      j2m.doJointExport(args[0], outfile);

   }

}
