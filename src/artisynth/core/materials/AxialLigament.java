package artisynth.core.materials;

import java.io.*;
import java.util.*;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;

import maspack.properties.PropertyList;
import maspack.function.*;
import maspack.interpolation.*;
import maspack.util.*;

/** 
 * Ligmament material based on : Blankevoort, L., and Huiskes,
 * R. (1991). Ligament-bone interaction in a three-dimensional model of the
 * knee.  Journal of Biomechanical Engineering, 113(3), 263-269.
 * 
 * implementation adapted from UW SimTk plugin: 
 * https://simtk.org/home/uwmodels
 * 
z * Notes:
 * - the restLength property in the associated spring is used as
 * the referenceLength in the UW formulation, where referenceLength
 * is the length of the ligament when the joint is in the reference 
 * position (full extension).
 * 
 */
public class AxialLigament extends AxialMaterial {

   public static double DEFAULT_MAX_FORCE = 1.0;
   public static double DEFAULT_DAMPING = 0.0; // typ constant
   public static double DEFAULT_SLACK_LENGTH = 0.0; // typ zero
   
   protected double myMaxForce = DEFAULT_MAX_FORCE;
   protected double myDamping = DEFAULT_DAMPING;
   protected double mySlackLength = DEFAULT_SLACK_LENGTH;

   Diff1Function1x1Base myForceLengthCurve;
   
   public static PropertyList myProps =
      new PropertyList (AxialLigament.class, AxialMaterial.class);

   
   protected static Diff1Function1x1Base myDefaultForceLengthCurve =
      createDefaultForceLengthCurve();

   protected static Diff1Function1x1Base createDefaultForceLengthCurve() {
      // a smoother version of Peter Loan's OpenSim default ligament
      double[] knotVals = new double[] {
         0.0, 0.0, 0.0,
         1.0, 0.0, 0.0,
         1.4539706497188747, 0.7673424569487259, 4.243209002494207,
         1.577742160990428, 1.8298974089730466, 2.912517275627977,
         1.7486933577192536, 2.0, 0.0,
         2.0, 2.0, 0.0
      };
      // double[] x = new double[] {
      //    -5.00000000,  0.99800000,  0.99900000,  1.00000000,  1.10000000,
      //    1.20000000,  1.30000000,  1.40000000,  1.50000000,  1.60000000,
      //    1.60100000,  1.60200000,  5.00000000};
      // double[] y = new double[] {
      //    0.00000000,  0.00000000,  0.00000000,  0.00000000,  0.03500000,
      //    0.12000000,  0.26000000,  0.55000000,  1.17000000,  2.00000000,
      //    2.00000000,  2.00000000,  2.00000000};
      CubicHermiteSpline1d flc = new CubicHermiteSpline1d(knotVals);
      return flc;
   }

   static {
      myProps.add (
         "maxForce",
         "scale factor for the force length curve",
         DEFAULT_MAX_FORCE);
      myProps.add ("damping", "damping coefficient", DEFAULT_DAMPING);
      myProps.add ("slackLength", "slack length", DEFAULT_SLACK_LENGTH);
   }

   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public AxialLigament () {
      myForceLengthCurve = myDefaultForceLengthCurve;
   }

   public AxialLigament (double fmax, double slackLen, double damping) {
      myMaxForce = fmax;
      mySlackLength = slackLen;
      myDamping = damping;
      myForceLengthCurve = myDefaultForceLengthCurve;
   }

   public AxialLigament (
      double fmax, double slackLen, double damping, Diff1Function1x1Base flc) {
      this (fmax, slackLen, damping);
      setForceLengthCurve (flc);
   }

   @Override
   public double computeF (
      double l, double ldot, double l0, double excitation) {

      double ln = l/mySlackLength;

      double fk = myMaxForce*myForceLengthCurve.eval (ln);
      if (fk < 0) {
         fk = 0;
      }
      return fk + myDamping*ldot/mySlackLength;
   }

   @Override
   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {

      double ln = l/mySlackLength;
      double fk = myMaxForce*myForceLengthCurve.eval (ln);
      if (fk < 0) {
         return 0;
      }
      else {
         return myMaxForce*myForceLengthCurve.evalDeriv (ln)/mySlackLength;
      }
   }

   @Override
   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {

      return myDamping/mySlackLength;
   }

   @Override
   public boolean isDFdldotZero () {
      return myDamping == 0;
   }

   public Diff1Function1x1 getForceLengthCurve() {
      return myForceLengthCurve;
   }

   public void setForceLengthCurve (Diff1Function1x1Base flc) {
      if (flc == null) {
         myForceLengthCurve = myDefaultForceLengthCurve;
      }
      else {
         myForceLengthCurve = flc.clone();
      }
   }

   /* property methods */
   
   public double getMaxForce () {
      return myMaxForce;
   }

   public void setMaxForce (double maxForce) {
      this.myMaxForce = maxForce;
   }

   public double getDamping () {
      return myDamping;
   }

   public void setDamping (double d) {
      this.myDamping = d;
   }

   public double getSlackLength () {
      return mySlackLength;
   }

   public void setSlackLength (double sl) {
      this.mySlackLength = sl;
   }

   public boolean equals(AxialMaterial mat) {
      if (!(mat instanceof AxialLigament)) {
         return false;
      }
      AxialLigament lmat = (AxialLigament)mat;
      if (myMaxForce != lmat.myMaxForce ||
          mySlackLength != lmat.mySlackLength ||
          myDamping != lmat.myDamping) {
         return false;
      }
      else {
         return super.equals(mat);
      }
   }

   public AxialLigament clone() {
      AxialLigament mat = (AxialLigament)super.clone();
      if (myForceLengthCurve != myDefaultForceLengthCurve) {
         mat.myForceLengthCurve = myForceLengthCurve.clone();
      }
      return mat;
   }

   public void scaleDistance(double s) {
      super.scaleDistance(s);
      mySlackLength *= s;
      myMaxForce *= s;
   }

   public void scaleMass(double s) {
      super.scaleMass(s);
      myMaxForce *= s;
      myDamping *= s;
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      if (myForceLengthCurve != myDefaultForceLengthCurve) {
         pw.print ("forceLengthCurve=");
         FunctionUtils.write (pw, myForceLengthCurve, fmt);
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      // if keyword is a property name, try scanning that
      rtok.nextToken();
      if (ScanWriteUtils.scanAttributeName (
             rtok, "forceLengthCurve")) {
         myForceLengthCurve =
            FunctionUtils.scan (rtok, Diff1Function1x1Base.class);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public void writeForceLengthCurve (
      String fileName, double x0, double x1,
      int npnts, String fmtStr) throws IOException {
      FunctionUtils.writeValues (
         new File(fileName), myForceLengthCurve, x0, x1, npnts, fmtStr);
   }


   public static void main (String[] args) throws IOException {
      AxialLigament lig = new AxialLigament();
      lig.writeForceLengthCurve ("axialLigament.txt", 0, 2, 100, "%g");
   }

}
