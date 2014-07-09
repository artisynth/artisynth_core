/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.io.*;
import java.util.*;
import argparser.*;
import maspack.util.*;

/** 
 * Class to convert .kaj files (which contain muscle fiber information)
 * into .obj files.
 *
 * <p>The .kaj format is a special format created by Anne Auger's group at the
 * U of T Anatomy Lab.
 */
public class KajToObj {

   class FiberTag {
      String muscleName;
      String partName;
      int layer;
      int number;

      FiberTag (String m, String p, int l, int n) {
         muscleName = m;
         partName = p;
         layer = l;
         number = n;
      }

      boolean equals (FiberTag tag) {
         return (muscleName.equals (tag.muscleName) &&
                 partName.equals (tag.partName) &&
                 layer == tag.layer &&
                 number == tag.number);
      }

      String getName () {
         return partName + "Layer" + layer + "Fiber" + number;
      }
   }

   private void printFiberLine (PrintWriter pw, int startIdx, int endIdx) {
      pw.print ("l");
      for (int i=startIdx; i<=endIdx; i++) {
         pw.print (" " + i);
      }
      pw.println ("");         
   }

   public void convert (ReaderTokenizer rtok, PrintWriter pw) throws IOException {

      LinkedList<FiberTag> tagList = new LinkedList<FiberTag>();

      FiberTag currentTag = null;
      int vertexIdx = 0;
      int startIdx = -1;

      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         rtok.pushBack();
         String muscleName = rtok.scanWord();
         rtok.scanToken ('|');
         String partName = rtok.scanWord();
         rtok.scanToken ('|');
         int layer = rtok.scanInteger();
         rtok.scanToken ('|');
         int number = rtok.scanInteger();
         rtok.scanToken ('|');

         double x = rtok.scanNumber();
         double y = rtok.scanNumber();
         double z = rtok.scanNumber();

         vertexIdx++;
         FiberTag tag = new FiberTag (muscleName, partName, layer, number);
         if (currentTag != null && !currentTag.equals (tag)) {
            printFiberLine (pw, startIdx, vertexIdx-1);
         }
         
         if (currentTag == null || !currentTag.equals (tag)) {
            pw.println ("");
            pw.println ("# " + tag.getName());
            currentTag = tag;
            startIdx = vertexIdx;
         }
         pw.println ("v " + x + " " + y + " " + z);
      }
      if (currentTag != null) {
         printFiberLine (pw, startIdx, vertexIdx-1);
      }

      rtok.close();
      pw.close();
   }

   public static void main (String[] args) {

      String kajFileName = null;
      String objFileName = null;

      ArgParser parser = new ArgParser (
         "java maspack.geometry.KajToObj <kajFile> <objFile>");
      //parser.addOption ("-file %s #mesh file names", meshFileList);
      int idx = 0;
      while (idx < args.length) {
         try {
            idx = parser.matchArg (args, idx);
            if (parser.getUnmatchedArgument() != null) {
               if (kajFileName == null) {
                  kajFileName = parser.getUnmatchedArgument();
               }
               else if (objFileName == null) {
                  objFileName = parser.getUnmatchedArgument();
               }
               else {
                  System.err.println (parser.getSynopsisString());
                  System.exit(1);
               }
            }
         }
         catch (Exception e) {
            // malformed or erroneous argument
            parser.printErrorAndExit (e.getMessage());
         }
      }

      if (kajFileName == null || objFileName == null) { 
         System.err.println (parser.getSynopsisString());
         System.exit(1);
     }

      ReaderTokenizer rtok = null;
      PrintWriter pw = null;

      try {
         if (kajFileName.equals ("-")) {
            rtok = new ReaderTokenizer (
               new BufferedReader (new InputStreamReader (System.in)));
         }
         else {
            rtok = new ReaderTokenizer (
               new BufferedReader (new FileReader (kajFileName)));
         }
         if (objFileName.equals ("-")) {
            pw = new PrintWriter (
               new BufferedWriter (new OutputStreamWriter (System.out)));
         }
         else {
            pw = new PrintWriter (
               new BufferedWriter (new FileWriter (objFileName)));
         }
         KajToObj converter = new KajToObj();
         Calendar calendar = Calendar.getInstance();
         pw.println (
            "# Exported OBJ file from '"+kajFileName+"', "+calendar.getTime());
         converter.convert (rtok, pw);
      }
      catch (Exception e) {
         e.printStackTrace(); 
         System.exit(1); 
      }
   }
}
