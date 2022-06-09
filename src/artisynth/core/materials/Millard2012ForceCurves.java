package artisynth.core.materials;

import java.io.PrintWriter;
import java.io.IOException;

import maspack.interpolation.*;
import maspack.interpolation.CubicHermiteSpline1d.Knot;
import maspack.util.*;
import maspack.matrix.*;

public class Millard2012ForceCurves {

   void printCurve (CubicHermiteSpline1d curve, double yscale) {
      PrintWriter pw = new IndentingPrintWriter (System.out);
      curve.scaleY (yscale);
      try {
         curve.write (pw, new NumberFormat("%g"), null);
      }
      catch (IOException e) {
         e.printStackTrace(); 
      }
      pw.flush();
   }

   void printActiveForceLengthCurve (double yscale) {
      CubicHermiteSpline1d curve =
         Millard2012AxialMuscle.getDefaultActiveForceLengthCurve();
      printCurve (curve, yscale);
   }

   void printForceVelocityCurve (double yscale) {
      CubicHermiteSpline1d curve =
         Millard2012AxialMuscle.getDefaultForceVelocityCurve();
      printCurve (curve, yscale);
   }

   void printTendonForceLengthCurve (double yscale) { 
      CubicHermiteSpline1d curve =
         Millard2012AxialMuscle.getDefaultTendonForceLengthCurve();
      printCurve (curve, yscale);
   }
      
   void printPassiveForceLengthCurve (double yscale) { 
      CubicHermiteSpline1d curve =
         Millard2012AxialMuscle.getDefaultPassiveForceLengthCurve();
      printCurve (curve, yscale);
   }

   public static void main (String[] args) {
      Millard2012ForceCurves curves = new Millard2012ForceCurves();
      curves.printTendonForceLengthCurve(0.1);
      //curves.printActiveForceLengthCurve(1.0);
      //curves.printPassiveForceLengthCurve(1.0);
      //curves.printForceVelocityCurve(1.0);
   }

}
