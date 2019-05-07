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
 * <p>Because of numeric round-off error, we can't expect the numbers to always
 * be exactly the same - without introducing error, refactorization may change
 * the order of arithmetic operations, leading to small differences in
 * output. Hence we can't simply 'diff' the files, and so we use this program
 * instead.
 *
 * <p>Each file consists of a series of sections, arranged like this:
 *
 * <pre>
 * TEST "string describing the section"
 * comps: [ P P F ]
 * t=0.00:
 * v: xxx xxx xxx xxx xxx xxx xxx xxx xxx xxx xxx xxx
 * x: xxx xxx xxx xxx xxx xxx xxx xxx xxx xxx xxx xxx xxx
 * t=0.01:
 * v: xxx xxx xxx xxx xxx xxx xxx xxx xxx xxx xxx xxx
 * x: xxx xxx xxx xxx xxx xxx xxx xxx xxx xxx xxx xxx xxx
 * ...
 * </pre>
 * The section begins with the keyword {@code TEST}, followed by a quoted
 * string describing the section.
 *
 * <p>The next line, beginning with {@code comps:}, lists a sequence of letter
 * codes, delimited by square brackets, describing the dynamic components that
 * make up the state. The following letter codes are defined: 'P' denotes a
 * particle, 'F' denotes a frame, and 'RXX' denotes a reduced model where 'XX'
 * is an integer giving the number of coordinates.
 *
 * <p>The following lines give the simulation time (after {@code t=}), velocity
 * state (after {@code v:}, and position state (after {@code x:}), and these
 * lines repeat until the end of the section.
 *
 * <p>Between the two files, each section must have the description string and
 * the same number of time entries. This program then compares the maximum
 * error between the velocity and position trajectories for each section (in a
 * manner appropriate to the components), and outputs the maximum 
 * overall. With the <code>-a</code> option, the errors for each section are
 * also output.
 */
public class CompareStateFiles {

   NumberFormat fmt = new NumberFormat ("%.6g");

   private static class ErrorDesc {
      double err;
      double mag;
      double time;
      CompDesc comp;

      double normalizedError() {
         if (mag != 0) {
            return err/mag;
         }
         else {
            return err;
         }
      }
   }

   // component descriptor
   private abstract class CompDesc {
      ErrorDesc myVelErr;
      ErrorDesc myPosErr;

      int myVelSize;
      int myPosSize;
      String mySymbol;

      CompDesc (String symbol, int velSize, int posSize) {
         mySymbol = symbol;
         myVelSize = velSize;
         myPosSize = posSize;
      }

      String getSymbol() {
         return mySymbol;
      }

      int computeError (
         ErrorDesc edesc, VectorNd vec1, VectorNd vec2,
         double time, int size, int idx) {
         VectorNd v1 = new VectorNd (size);
         VectorNd v2 = new VectorNd (size);
         VectorNd err = new VectorNd (size);
         vec1.getSubVector (idx, v1);
         vec2.getSubVector (idx, v2);
         err.sub (v1, v2);
         if (err.norm() > edesc.err) {
            edesc.err = err.norm();
            edesc.time = time;
            edesc.comp = this;
         }
         double mag = Math.max (v1.norm(), v2.norm());
         if (mag > edesc.mag) {
            edesc.mag = mag;
         }
         return idx + size;
      }

      abstract int computeVelError (
         VectorNd vel1, VectorNd vel2, double time, int idx);

      abstract int computePosError (
         VectorNd pos1, VectorNd pos2, double time, int idx);

      int getVelSize() {
         return myVelSize;
      }

      int getPosSize() {
         return myPosSize;
      }

      void setErrDescs (ErrorDesc velErr, ErrorDesc posErr) {
         myVelErr = velErr;
         myPosErr = posErr;
      }
   }

   private class ParticleDesc extends CompDesc {

      ParticleDesc() {
         super ("P", 3, 3);
      }

      int computeVelError (VectorNd vel1, VectorNd vel2, double time, int idx) {
         idx = computeError (myVelErr, vel1, vel2, time, 3, idx);
         return idx;
      }

      int computePosError (VectorNd pos1, VectorNd pos2, double time, int idx) {
         idx = computeError (myPosErr, pos1, pos2, time, 3, idx);
         return idx;
      }
   }
      
   private class FrameDesc extends CompDesc {

      ErrorDesc myAngVelErr;
      ErrorDesc myAngPosErr;

      FrameDesc() {
         super ("F", 6, 7);
      }

      int computeVelError (VectorNd vel1, VectorNd vel2, double time, int idx) {
         idx = computeError (myVelErr, vel1, vel2, time, 3, idx);
         idx = computeError (myAngVelErr, vel1, vel2, time, 3, idx);
         return idx;
      }

      int computePosError (VectorNd pos1, VectorNd pos2, double time, int idx) {
         idx = computeError (myPosErr, pos1, pos2, time, 3, idx);
         idx = computeQuaternionError (pos1, pos2, time, idx);
         return idx;
      }

      int computeQuaternionError (
         VectorNd pos1, VectorNd pos2, double time, int idx) {
         Quaternion q1 = new Quaternion();
         Quaternion q2 = new Quaternion();
         pos1.getSubVector (idx, q1);
         pos2.getSubVector (idx, q2);
         double err = Math.abs(q1.rotationAngle(q2))/Math.PI;
         if (err > myAngPosErr.err) {
            myAngPosErr.err = err;
            myAngPosErr.time = time;
            myAngPosErr.comp = this;
         }
         return idx + 4;
      }

      void setAngErrDescs (ErrorDesc angVelErr, ErrorDesc angPosErr) {
         myAngVelErr = angVelErr;
         myAngPosErr = angPosErr;
         myAngPosErr.mag = 1.0;
      }
   }
      
   private class ReducedDesc extends CompDesc {

      int myNumCoords = 0;

      ReducedDesc (int numCoords) {
         super ("R", numCoords, numCoords);
         myNumCoords = numCoords;
      }

      int computeVelError (
         VectorNd vel1, VectorNd vel2, double time, int idx) {
         idx = computeError (myVelErr, vel1, vel2, time, myNumCoords, idx);
         return idx;
      }

      int computePosError (
         VectorNd pos1, VectorNd pos2, double time, int idx) {
         idx = computeError (myPosErr, pos1, pos2, time, myNumCoords, idx);
         return idx;
      }
   }
      

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

   double scanTime (ReaderTokenizer rtok) 
      throws IOException {

      if (rtok.nextToken() == ReaderTokenizer.TT_EOF) {
         return -1;
      }
      if (rtok.tokenIsWord() && rtok.sval.equals ("TEST")) {
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

   ArrayList<CompDesc> scanComps (ReaderTokenizer rtok) throws IOException {
      ArrayList<CompDesc> comps = new ArrayList<CompDesc>();
      rtok.scanWord ("comps");
      rtok.scanToken (':');
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         String symbol = rtok.scanWord();
         if (symbol.equals ("P")) {
            comps.add (new ParticleDesc());
         }
         else if (symbol.equals ("F")) {
            comps.add (new FrameDesc());
         }
         else if (symbol.startsWith ("R")) {
            int ncoords = -1;
            try {
               ncoords = Integer.valueOf (symbol.substring (1));
            }
            catch (Exception e) {
               throw new IOException (
                  "Component symbol R does not have an integer suffix:" + rtok);
            }
            comps.add (new ReducedDesc(ncoords));
         }
         else {
            throw new IOException (
               "Unexpected component symbol:" + rtok);
         }
      }
      return comps;
   }                               

   String scanSectionStart (
      ArrayList<CompDesc> comps, ReaderTokenizer rtok1, ReaderTokenizer rtok2) 
      throws IOException {

      rtok1.nextToken();
      rtok2.nextToken();

      if (rtok1.ttype != rtok2.ttype) {
         throw new IOException (
            "Inconsistent inputs: " + rtok1 + " vs. " + rtok2);
      }
      if (rtok1.ttype == ReaderTokenizer.TT_EOF) {
         return null;
      }
      else if (rtok1.tokenIsWord()) {
         if (!rtok1.sval.equals ("TEST")) {
            throw new IOException (
               "First file, expected TEST keyword, got " + rtok1);
         }
         if (!rtok2.sval.equals ("TEST")) {
            throw new IOException (
               "Second file, expected TEST keyword, got " + rtok2);
         }
      }
      else {
         throw new IOException (
            "First file, Expected TEST keyword, got " + rtok1);
      }

      String desc1 = rtok1.scanQuotedString('"');
      String desc2 = rtok2.scanQuotedString('"');

      if (!desc1.equals (desc2)) {
         throw new IOException (
            "Inconsistent sections: '"+desc1+"' vs. '"+desc2+
            "', line "+rtok1.lineno());
      }
      ArrayList<CompDesc> comps1 = scanComps (rtok1);
      ArrayList<CompDesc> comps2 = scanComps (rtok2);
      if (comps1.size() != comps2.size()) {
         throw new IOException (
            "Inconsistent component counts: "+
            comps1.size()+" vs. "+comps2.size()+", line "+rtok1.lineno());
      }
      for (int i=0; i<comps1.size(); i++) {
         CompDesc c1 = comps1.get(i);
         CompDesc c2 = comps2.get(i);
         if (!c1.getSymbol().equals (c2.getSymbol())) {
            throw new IOException (
               "Inconsistent component "+i+": "+
               c1.getSymbol()+" vs. "+c2.getSymbol()+", line "+rtok1.lineno());
         }
      }
      comps.clear();
      comps.addAll (comps1);
      return desc1;
   }

   double scanTimes (
      ReaderTokenizer rtok1, ReaderTokenizer rtok2)
      throws IOException {
      
      double t1 = scanTime (rtok1);
      double t2 = scanTime (rtok2);

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

   VectorNd scanVelocity (ReaderTokenizer rtok, int velSize) throws IOException {
      rtok.scanWord ("v");
      rtok.scanToken (':');
      VectorNd vec = scanVector (rtok);
      if (vec.size() != velSize) {
         throw new IOException (
            "Read "+vec.size()+" velocity values, expected " + velSize);
      }
      return vec;
   }

   VectorNd scanPosition (ReaderTokenizer rtok, int posSize) throws IOException {
      rtok.scanWord ("x");
      rtok.scanToken (':');
      VectorNd vec = scanVector (rtok);
      if (vec.size() != posSize) {
         throw new IOException (
            "Read "+vec.size()+" position values, expected " + posSize);
      }
      return vec;
   }

   double myMaxVelErr = 0;
   double myMaxVelErrTime = 0;
   String myMaxVelErrDescription = null;

   double myMaxPosErr = 0;
   double myMaxPosErrTime = 0;
   String myMaxPosErrDescription = null;

   ErrorDesc findMaxError (ArrayList<ErrorDesc> edescs) {
      ErrorDesc maxdesc = null;
      double maxerr = 0;
      for (ErrorDesc ed : edescs) {
         if (maxdesc == null || ed.normalizedError() > maxerr) {
            maxdesc = ed;
            maxerr = ed.normalizedError();
         }
      }
      return maxdesc;
   }

   ArrayList<CompDesc> myComps;

   public boolean compareSections (
      ReaderTokenizer rtok1, ReaderTokenizer rtok2, int showLevel) 
      throws IOException {

      VectorNd vel1, vel2;
      VectorNd pos1, pos2;

      ArrayList<ErrorDesc> vdescs = new ArrayList<ErrorDesc>();
      ErrorDesc maxPntVelErr = new ErrorDesc();
      ErrorDesc maxAngVelErr = new ErrorDesc();
      ErrorDesc maxRedVelErr = new ErrorDesc();
      vdescs.add (maxPntVelErr);
      vdescs.add (maxAngVelErr);
      vdescs.add (maxRedVelErr);

      ArrayList<ErrorDesc> pdescs = new ArrayList<ErrorDesc>();
      ErrorDesc maxPntPosErr = new ErrorDesc();
      ErrorDesc maxAngPosErr = new ErrorDesc();
      ErrorDesc maxRedPosErr = new ErrorDesc();
      pdescs.add (maxPntPosErr);
      pdescs.add (maxAngPosErr);
      pdescs.add (maxRedPosErr);

      ArrayList<CompDesc> comps = new ArrayList<CompDesc>();
      String description = scanSectionStart (comps, rtok1, rtok2);
      myComps = comps;
      if (description == null) {
         // EOF
         return false;
      }
      int posSize = 0;
      int velSize = 0;
      for (CompDesc comp : comps) {
         velSize += comp.getVelSize();
         posSize += comp.getPosSize();
      }

      for (CompDesc comp : comps) {
         if (comp instanceof ParticleDesc) {
            ParticleDesc pcomp = (ParticleDesc)comp;
            pcomp.setErrDescs (maxPntVelErr, maxPntPosErr);
         }
         else if (comp instanceof FrameDesc) {
            FrameDesc fcomp = (FrameDesc)comp;
            fcomp.setErrDescs (maxPntVelErr, maxPntPosErr);
            fcomp.setAngErrDescs (maxAngVelErr, maxAngPosErr);
         }
         else if (comp instanceof ReducedDesc) {
            ReducedDesc rcomp = (ReducedDesc)comp;
            rcomp.setErrDescs (maxRedVelErr, maxRedPosErr);
         }
         else {
            throw new UnsupportedOperationException (
               "Unknown component type " + comp.getClass());
         }
      }

      double time;
      while ((time = scanTimes (rtok1, rtok2)) != -1) {
         vel1 = scanVelocity (rtok1, velSize);
         vel2 = scanVelocity (rtok2, velSize);
         if (vel1.size() != vel2.size()) {
            throw new IOException ("different velocity sizes: " + vel1.size()
            + " vs. " + vel2.size() + ", line " + rtok1.lineno());
         }
         int idx = 0;
         for (CompDesc c : comps) {
            idx = c.computeVelError (vel1, vel2, time, idx);
         }

         pos1 = scanPosition (rtok1, posSize);
         pos2 = scanPosition (rtok2, posSize);
         if (pos1.size() != pos2.size()) {
            throw new IOException ("different position sizes: " + pos1.size()
            + " vs. " + pos2.size() + ", line " + rtok1.lineno());
         }
         idx = 0;
         for (CompDesc c : comps) {
            idx = c.computePosError (pos1, pos2, time, idx);
         }

         if (showLevel > 1) {
            ErrorDesc maxVelErr = findMaxError (vdescs);
            ErrorDesc maxPosErr = findMaxError (pdescs);
            double verr = maxVelErr.normalizedError();
            String vmsg = " verr=" + fmt.format(verr);
            if (verr != 0) {
               vmsg += " (comp "+comps.indexOf (maxVelErr.comp)+")";
            }
            double perr = maxPosErr.normalizedError();
            String pmsg = " perr=" + fmt.format(perr);
            if (perr != 0) {
               pmsg += " (comp "+comps.indexOf (maxPosErr.comp)+")";
            }
            System.out.println ("time " + time + ":" + vmsg + "," + pmsg);
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
      ErrorDesc maxVelErr = findMaxError (vdescs);
      ErrorDesc maxPosErr = findMaxError (pdescs);

      if (showLevel > 0) {
         System.out.println (description + ":");
         System.out.println (
            "vel error=" + fmt.format(maxVelErr.normalizedError()));
         System.out.println (
            "pos error=" + fmt.format(maxPosErr.normalizedError()));
      }

      if (maxVelErr.normalizedError() > myMaxVelErr) {
         myMaxVelErr = maxVelErr.normalizedError();
         myMaxVelErrTime = maxVelErr.time;
         myMaxVelErrDescription = description;
      }
      if (maxPosErr.normalizedError() > myMaxPosErr) {
         myMaxPosErr = maxPosErr.normalizedError();
         myMaxPosErrTime = maxPosErr.time;
         myMaxPosErrDescription = description;
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
      myMaxVelErrDescription = null;
      myMaxPosErr = 0;
      myMaxPosErrTime = 0;
      myMaxPosErrDescription = null;
      
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
         myMaxVelErrDescription+"', time "+myMaxVelErrTime);
      pw.println (
         "max pos error="+fmt.format(myMaxPosErr)+" at '"+
         myMaxPosErrDescription+"', time "+myMaxPosErrTime); 
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
