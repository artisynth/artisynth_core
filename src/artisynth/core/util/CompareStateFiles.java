/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import java.io.*;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;

/** 
 * This program is used to two compare files describing the state (i.e.,
 * velocity and position) trajectory of a sequence of ArtiSynth simulations:
 * <pre>
 * java artisynth.core.util.CompareStateFiles file1 file2
 * </pre>
 * The reason for doing this is mainly regression testing: We record the state
 * trajectories for a series of simulations, save this in a file (usually named
 * something like XXXTestData.orig), and then after we have made changes to the
 * system, record the state trajectories again, save these in another file
 * (usually XXXTestData.out), and then compare with the original.
 *
 * Because of numeric round-off error, we can't expect the numbers to always be
 * exactly the same - without introducing error, refactorization may change the
 * order of arithmetic operations, leading to small differences in
 * output. Hence we can't simply 'diff' the files, and so we use this program
 * instead.
 *
 * Each file consists of a series of sections, arranged like this:
 *
 * <pre>
 * # comment line describing the section
 * t=0.00:
 * v: xxx xxx xxx xxx
 * x: xxx xxx xxx xxx xxx
 * t=0.01:
 * v: xxx xxx xxx xxx
 * x: xxx xxx xxx xxx xxx
 * ...
 * </pre>

 * where <code>t</code>, <code>v</code>, <code>x</code> denote time, velocity,
 * and position. Note that velocity and position do not necessarily have the
 * same number of state variables.  Between the two files, each section must
 * have the same comment line and the same number of time entries. This program
 * then compares the maximum error between the velocity and position
 * trajectories for each section, and outputs the maximum overall. With the
 * <code>-a</code> option, the errors for each section are also output.
 */
public class CompareStateFiles {

   NumberFormat fmt = new NumberFormat ("%.6g");

   VectorNd scanVector (ReaderTokenizer rtok) throws IOException {
      ArrayList<Double> values = new ArrayList<Double>();
      while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
         values.add (rtok.nval);
      }
      rtok.pushBack();
      VectorNd vec = new VectorNd (values.size());
      for (int i = 0; i < values.size(); i++) {
         vec.set (i, values.get (i));
      }
      return vec;
   }

   double scanTime (ReaderTokenizer rtok, String lastComment) 
      throws IOException {

      if (rtok.nextToken() == ReaderTokenizer.TT_EOF) {
         return -1;
      }
      if (lastComment != null && !commentsEqual(rtok.lastCommentLine(), lastComment)) {
         rtok.pushBack();
         return -1;
      }
      rtok.pushBack();
      rtok.scanWord ("t");
      rtok.scanToken ('=');
      double t = rtok.scanNumber();
      rtok.scanToken (':');
      return t;
   }

   double scanTimes (
      ReaderTokenizer rtok1, ReaderTokenizer rtok2, String lastComment)
      throws IOException {
      
      double t1 = scanTime (rtok1, lastComment);
      double t2 = scanTime (rtok2, lastComment);
      
      String com1 = rtok1.lastCommentLine();
      String com2 = rtok2.lastCommentLine();
      
      if (!commentsEqual (com1, com2)) {
         throw new IOException (
            "Inconsistent sections: '"+com1+"' vs. '"+com2+
            "', line "+rtok1.lineno());
      }
      if (t1 != t2) {
         if (t1 != -1 && t2 != -1) {
            throw new IOException (
               "Inconsistent time data, "+t1+" vs. "+t2+
               ", line "+rtok1.lineno());
         }
         return -1;
      }
      return t1;
   }

   VectorNd scanVelocity (ReaderTokenizer rtok) throws IOException {
      rtok.scanWord ("v");
      rtok.scanToken (':');
      return scanVector (rtok);
   }

   VectorNd scanPosition (ReaderTokenizer rtok) throws IOException {
      rtok.scanWord ("x");
      rtok.scanToken (':');
      return scanVector (rtok);
   }

   double myMaxVelErr = 0;
   double myMaxVelErrTime = 0;
   String myMaxVelErrComment = null;

   double myMaxPosErr = 0;
   double myMaxPosErrTime = 0;
   String myMaxPosErrComment = null;

   private boolean commentsEqual (String s1, String s2) {
      if ((s1 == null) != (s2 == null)) {
         return false;
      }
      else if (s1 != null) {
         s1 = s1.replace("\r", "");
         s2 = s2.replace("\r", "");
         return s1.equals (s2);
      }
      else {
         return true;
      }
   }

   public boolean compareSections (
      ReaderTokenizer rtok1, ReaderTokenizer rtok2, int showLevel) 
      throws IOException {

      VectorNd vel1, vel2;
      VectorNd pos1, pos2;

      double maxVelMag = 0;
      double maxPosMag = 0;
      double maxVelErr = 0;
      double maxPosErr = 0;
      double maxVelErrTime = 0;
      double maxPosErrTime = 0;

      double time;
      String comment = null;
      int cnt = 0;

      while ((time = scanTimes (rtok1, rtok2, comment)) != -1) {

         // trim any white space after the comment
         // e.g. '\r', which was interfering with output
         comment = rtok1.lastCommentLine();
         if (comment != null) {
            comment = comment.trim();
         }
         
         cnt++;

         vel1 = scanVelocity (rtok1);
         vel2 = scanVelocity (rtok2);
         if (vel1.size() != vel2.size()) {
            throw new IOException ("different velocity sizes: " + vel1.size()
            + " vs. " + vel2.size() + ", line " + rtok1.lineno());
         }
         maxVelMag = Math.max (maxVelMag, vel1.norm());
         vel1.sub (vel2);
         if (vel1.norm() > maxVelErr) {
            maxVelErr = vel1.norm();
            maxVelErrTime = time;
         }

         pos1 = scanPosition (rtok1);
         pos2 = scanPosition (rtok2);
         if (pos1.size() != pos2.size()) {
            throw new IOException ("different position sizes: " + pos1.size()
            + " vs. " + pos2.size() + ", line " + rtok1.lineno());
         }
         maxPosMag = Math.max (maxPosMag, pos1.norm());
         pos1.sub (pos2);
         if (pos1.norm() > maxPosErr) {
            maxPosErr = pos1.norm();
            maxPosErrTime = time;
         }

         if (showLevel > 1) {
            vel1.absolute();
            pos1.absolute();
            double maxVel = vel1.maxElement();
            int maxVelIdx = vel1.maxIndex();
            double maxPos = pos1.maxElement();
            int maxPosIdx = pos1.maxIndex();
            System.out.println (
               "time " + time + 
               ": verr="+fmt.format(maxVel)+" (at "+maxVelIdx+")" + 
               " perr="+fmt.format(maxPos)+" (at "+maxPosIdx+")");
         }
         
      }
      if ((rtok1.ttype == ReaderTokenizer.TT_EOF) !=
          (rtok2.ttype == ReaderTokenizer.TT_EOF)) {

         if (rtok1.ttype != ReaderTokenizer.TT_EOF) {
            System.out.println ("Warning: second file ended prematurely");
         }
         else if (rtok2.ttype != ReaderTokenizer.TT_EOF) {
            System.out.println ("Warning: first file ended prematurely");
         }    
      }
      if (cnt == 0) {
         return false;
      }
      
      maxVelErr /= maxVelMag;
      maxPosErr /= maxPosMag;

      if (showLevel > 0) {
         System.out.println (comment + ":");
         System.out.println ("vel error=" + fmt.format(maxVelErr));
         System.out.println ("pos error=" + fmt.format(maxPosErr));
      }

      if (maxVelErr > myMaxVelErr) {
         myMaxVelErr = maxVelErr;
         myMaxVelErrTime = maxVelErrTime;
         myMaxVelErrComment = comment;
      }
      if (maxPosErr > myMaxPosErr) {
         myMaxPosErr = maxPosErr;
         myMaxPosErrTime = maxPosErrTime;
         myMaxPosErrComment = comment;
      }
      return true;
   }

   public double getMaxVelErr() {
      return myMaxVelErr;
   }

   public double getMaxPosErr() {
      return myMaxPosErr;
   }
   
   public double getMaxVelErrTime() {
      return myMaxVelErrTime;
   }

   public double getMaxPosErrTime() {
      return myMaxPosErrTime;
   }
   
   public boolean compareFiles (
      String fileName1, String fileName2, int showLevel)
      throws IOException {

      ReaderTokenizer rtok1 = ArtisynthIO.newReaderTokenizer (fileName1);
      ReaderTokenizer rtok2 = ArtisynthIO.newReaderTokenizer (fileName2);
      rtok1.whitespaceChar('\r');
      rtok2.whitespaceChar('\r');
      
      boolean status = compareFiles (rtok1, rtok2, showLevel);
      rtok1.close();
      rtok2.close();
      return status;
   }

   public boolean compareFiles (
      ReaderTokenizer rtok1, ReaderTokenizer rtok2, int showLevel)
      throws IOException {

      myMaxVelErr = 0;
      myMaxVelErrTime = 0;
      myMaxVelErrComment = null;
      myMaxPosErr = 0;
      myMaxPosErrTime = 0;
      myMaxPosErrComment = null;
      
      while (compareSections (rtok1, rtok2, showLevel))
         ;
      if (showLevel > -1) {
         printMaxErrors ();
      }
      return (myMaxVelErr == 0 && myMaxPosErr == 0);
   }
   
   public void printMaxErrors () {
      printMaxErrors (new PrintWriter (System.out));
   }
   
   public void printMaxErrors (PrintWriter pw) {
      pw.println (
         "max vel error="+fmt.format(myMaxVelErr)+" at '"+
         myMaxVelErrComment+"', time "+myMaxVelErrTime);
      pw.println (
         "max pos error="+fmt.format(myMaxPosErr)+" at '"+
         myMaxPosErrComment+"', time "+myMaxPosErrTime); 
      pw.flush();
   }

   private static void printUsageAndExit () {
      System.out.println ("Usage: [options] <stateFile1> <stateFile2>");
      System.out.println ("");
      System.out.println ("  -help: print this message");
      System.out.println ("  -a:    display error data for each section");
      System.out.println ("  -A:    display error data for each time");
      System.out.println ("");
      System.exit(1);
   }

   public static void main (String[] args) {

      String fileName1 = null;
      String fileName2 = null;
      int showLevel = 0;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-help")) {
            printUsageAndExit();
         }
         else if (args[i].equals ("-a")) {
            showLevel = 1;
         }
         else if (args[i].equals ("-A")) {
            showLevel = 2;
         }
         else if (args[i].startsWith ("-")) {
            printUsageAndExit();
         }
         else if (fileName1 == null) {
            fileName1 = args[i];
         }
         else if (fileName2 == null) {
            fileName2 = args[i];
         }
         else {
            printUsageAndExit();
         }
      }

      if (fileName1 == null || fileName2 == null) {
         printUsageAndExit();
      }

      try {
         CompareStateFiles compare = new CompareStateFiles();
         compare.compareFiles (fileName1, fileName2, showLevel);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }
}
