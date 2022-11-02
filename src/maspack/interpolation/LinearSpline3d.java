package maspack.interpolation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import maspack.matrix.Vector3d;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.*;
import maspack.util.IntHolder;
import maspack.util.ReaderTokenizer;
import maspack.util.Scannable;

/**
 * Implements three dimensional linear spline interpolation.  The spline is
 * defined by a sequence of knots delimiting the boundaries between linear
 * interpolation segements.
 */
public class LinearSpline3d
   implements Scannable, Iterable<LinearSpline3d.Knot> {

   public static class Knot {
      double myS0;   // s value at knot
      Vector3d myA0; // x value at knot
      int myIndex;

      public Knot() {
         myA0 = new Vector3d();
         myIndex = -1;
      }

      public Knot (double s0, Vector3d a0) {
         this();
         set (s0, a0);
      }

      public double getS0() {
         return myS0;
      }
      
      public Vector3d getA0() {
         return myA0;
      }

      public void set (double s0, Vector3d a0) {
         myS0 = s0;
         myA0.set (a0);
      }

      public void eval (Vector3d x, double s, Knot next) {
         double dels = next.myS0 - myS0;
         if (dels == 0) {
            // shouldn't happen but just in case
            x.set (myA0);
         }
         else {
            double t = (s-myS0)/dels; // normalize s to the interval
            x.combine (1-t, myA0, t, next.myA0);
         }
      }

      public void evalDx (Vector3d x, double s, Knot next) {
         double dels = next.myS0 - myS0;
         if (dels == 0) {
            // shouldn't happen but just in case
            x.setZero();
         }
         else {
            x.sub (next.myA0, myA0);
            x.scale (1/dels);
         }
      }

      /**
       * Creates a copy of this knot. All coefficients are copied except the
       * index.
       */
      public Knot copy() {
         Knot knot = new Knot();
         knot.myS0 = myS0;
         knot.myA0.set (myA0);
         return knot;
      }
      
      public int getIndex() {
         return myIndex;
      }

      public void setIndex(int idx) {
         myIndex = idx;
      }

      public boolean equals (Knot knot) {
         return (myS0 == knot.myS0 && myA0.equals(knot.myA0));
      }
   }

   ArrayList<Knot> myKnots;

   /**
    * Creates a empty spline
    */
   public LinearSpline3d() {
      myKnots = new ArrayList<>();
   }

   /**
    * Creates a spline from a set of values, withthe interval between each knot
    * point assumed to have a length of 1. 
    *
    * @param xVals x values for each knot point
    * @param dxdsVals dxds values for each knot point
    */
   public LinearSpline3d (
      ArrayList<? extends Vector3d> xVals, ArrayList<Vector3d> dxdsVals) {
      this();
      double[] sVals = new double[xVals.size()];
      for (int i=0; i<sVals.length; i++) {
         sVals[i] = i;
      }
      set (sVals, xVals);
   }

   /**
    * Creates a spline from data containing the values of s and x at each
    * knot point. The values of s should be in strictly ascending order.
    *
    * @param sVals s values at each knot point
    * @param xVals x values for each knot point
    */
   public LinearSpline3d (
      double[] sVals, ArrayList<? extends Vector3d> xVals) {
      this();
      set (sVals, xVals);
   }

   /**
    * Creates a spline from a copy of an existing one.
    *
    * @param spline spline to copy
    */
   public LinearSpline3d (LinearSpline3d spline) {
      this();
      set (spline);
   }

   /**
    * Sets this spline from specified values of s and x at each knot point. The
    * values of s should be in strictly ascending order.
    *
    * @param sVals s values at each knot point
    * @param xVals x values for each knot point
    */
   public void set (
      double[] sVals, ArrayList<? extends Vector3d> xVals) {
      if (sVals.length != xVals.size()) {
         throw new IllegalArgumentException (
            "sVals and xVals have different lengths");
      }
      for (int i=0; i<sVals.length-1; i++) {
         if (sVals[i] > sVals[i+1]) {
            throw new IllegalArgumentException (
               "sVals not in strictly ascending order");
         }
      }
      clearKnots();
      for (int i=0; i<xVals.size(); i++) {
         addKnot (sVals[i], xVals.get(i));
      }
   }

   /**
    * Sets this spline from a specified set of values, using the distance
    * between each value as the interpolating parameter. Adjacent values are
    * ignored.
    *
    * @param xVals x values for each knot point
    */
   public <T extends Vector3d> void setUsingDistance (ArrayList<T> xVals) {
      clearKnots();
      if (xVals.size() > 0) {
         DynamicDoubleArray distances = new DynamicDoubleArray();
         ArrayList<T> culledVals = new ArrayList<>(xVals.size());
         culledVals.add (xVals.get(0));
         distances.add (0);
         double dist = 0;
         for (int i=0; i<xVals.size()-1; i++) {
            double d = xVals.get(i).distance (xVals.get(i+1));
            if (d > 0) {
               dist += d;
               distances.add (dist);
               culledVals.add (xVals.get(i+1));
            }
         }
         set (distances.getArray(), culledVals);
      }
   }

   /**
    * Returns an array giving the values of s at each knot.
    *
    * @return array of s values
    */
   public double[] getSValues() {
      double[] svals = new double[numKnots()];
      int idx = 0;
      for (Knot knot : myKnots) {
         svals[idx++] = knot.myS0;
      }
      return svals;
   }

   /**
    * Adds another knot point to this spline at a specified s coordinate.
    *
    * @param s parameter value at which the knot should be added
    * @param x value at the knot
    */
   public Knot addKnot (double s, Vector3d x) {
      Knot knot = new Knot (s, x);
      Knot prev;
      Knot last = getLastKnot();
      if (last != null && last.myS0 <= s) {
         prev = last;
      }
      else {
         prev = getPreceedingKnot (s);
      }
      if (prev == null) {
         doAddKnot (0, knot);
      }
      else {
         doAddKnot (prev.getIndex()+1, knot);
      }
      return knot;
   }

   // reindex knots starting at idx
   private void reindexFrom (int idx) {
      while (idx < myKnots.size()) {
         myKnots.get(idx).setIndex(idx);
         idx++;
      }
      for (int k=0; k<myKnots.size(); k++) {
         if (myKnots.get(k).getIndex() != k) {
            throw new InternalErrorException (
               "bad index at "+k+": "+ myKnots.get(k).getIndex());
         }
      }
   }

   private void doAddKnot (int idx, Knot knot) {
      myKnots.add (idx, knot);
      reindexFrom (idx);
   }

   protected void addKnot (int idx, Knot knot) {
      doAddKnot (idx, knot);
   }

   /**
    * Removes a knot from this spline.
    *
    * @param knot knot to remove
    */
   public boolean removeKnot (Knot knot) {
      int idx = myKnots.indexOf (knot);
      if (idx != -1) {
         myKnots.remove (idx);
         reindexFrom (idx);
         knot.setIndex (-1);
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Removes all knots from from this spline.
    */
   public void clearKnots() {
      myKnots.clear();
   }

   /**
    * Find the knot immediately preceeding s. Specifically, find the nearest
    * knot for which {@code knot.getS0() <= s}. If no such knot exists, {@code
    * null} is returned.
    *
    * @param s value for which preceeding knot is sought
    */
   public Knot getPreceedingKnot (double s) {
      return getPreceedingKnot (s, null);
   }

   /**
    * Find the knot immediately preceeding s. Specifically, find the nearest
    * knot for which {@code knot.getS0() <= s}. If no such knot exists, {@code
    * null} is returned.
    *
    * <p>The optional parameter {@code lastIdx}, described below, can be used
    * to significantly reduce the search speed when queries are made with
    * increasing values of s.
    *
    * @param s value for which preceeding knot is sought
    * @param lastIdx if non-null, specifies a index value giving a hint on
    * where to start the knot search. On output, this value is set to the index
    * of the returned knot, or -1 if no knot is found.
    <*/
   public Knot getPreceedingKnot (double s, IntHolder lastIdx) {
      if (myKnots.size() == 0) {
         return null;
      }
      Knot prev = null;
      int k0 = 0;
      if (lastIdx != null) {
         int idx = lastIdx.value;
         if (idx > 0 && idx < myKnots.size() && myKnots.get(idx).myS0 <= s) {
            k0 = idx;
         }
      }
      for (int k=k0; k<myKnots.size(); k++) {
         if (myKnots.get(k).myS0 > s) {
            break;
         }
         prev = myKnots.get(k);
      }
      if (lastIdx != null) {
         lastIdx.value = (prev != null ? prev.myIndex : -1);
      }
      return prev;
   }

   /**
    * Find the knot immediately following s. Specifically, find the nearest
    * knot for which {@code knot.x0 > s}. If no such knot exists, {@code null}
    * is returned.
    *
    * @param s value for which next knot is sought
    */
   public Knot getNextKnot (double s) {
      if (myKnots.size() == 0) {
         return null;
      }
      Knot next = null;
      for (int k=myKnots.size()-1; k >= 0; k--) {
         if (s >= myKnots.get(k).myS0) {
            break;
         }
         next = myKnots.get(k);
      }
      return next;
   }

   private Knot getNextKnot (Knot prev) {
      if (prev.myIndex < numKnots()-1) {
         return myKnots.get(prev.myIndex+1);
      }
      else {
         return null;
      }
   }

   private Knot getPrevKnot (Knot prev) {
      if (prev.myIndex > 0) {
         return myKnots.get(prev.myIndex-1);
      }
      else {
         return null;
      }
   }

   /**
    * Evaluates the value of this spline for a specific parameter value. For
    * parameter values outside the knot range, the value is lineraly
    * extrapolated based on derivatives at the first or last knots. An empty
    * spline evaluates to 0.
    *
    * @param s parameter for which the value should be computed
    */
   public Vector3d eval (double s) {
      return eval (s, null);
   }

   /**
    * Evaluates the value of this spline for a specific parameter value. For
    * parameter values outside the knot range, the value is lineraly
    * extrapolated based on derivatives at the first or last knots. An empty
    * spline evaluates to 0.
    *
    * <p>The optional parameter {@code lastIdx}, described below, can be used
    * to significantly reduce the search speed when queries are made with
    * increasing values of s.
    *
    * @param s parameter for which the value should be computed
    * @param lastIdx if non-null, specifies a index value giving a hint on
    * where to search for the knot preceeding s. On output, this value is set
    * to the index of the preceeding knot, or -1 if no preceeding knot is
    * found.
    */
   public Vector3d eval (double s, IntHolder lastIdx) {
      Vector3d x = new Vector3d();
      if (numKnots() > 0) {
         Knot prev = getPreceedingKnot (s, lastIdx);
         if (prev == null) {
            prev = myKnots.get(0);
         }
         Knot next = getNextKnot (prev);
         if (next == null) {
            Knot pprev = getPrevKnot (prev);
            if (pprev == null) {
               x.set (prev.getA0());
               return x;
            }
            else {
               next = prev;
               prev = pprev;
            }
         }
         prev.eval (x, s, next);
      }
      return x;
   }

   /**
    * Evaluates the derivative (with respect to the parameter) of this spline
    * for a specific parameter value. For parameter values outside the knot
    * range, the value is set to the derivative at either the first or last
    * knot. An empty spline evaluates to 0.
    *
    * @param s parameter for which the derivative should be computed
    */
   public Vector3d evalDx (double s) {
      return evalDx (s, null);
   }

   /**
    * Evaluates the derivative (with respect to the parameter) of this spline
    * for a specific parameter value. For parameter values outside the knot
    * range, the value is set to the derivative at either the first or last
    * knot. An empty spline evaluates to 0.
    *
    * <p>The optional parameter {@code lastIdx}, described below, can be used
    * to significantly reduce the search speed when queries are made with
    * increasing values of s.
    *
    * @param s parameter for which the derivative should be computed
    * @param lastIdx if non-null, specifies a index value giving a hint on
    * where to search for the knot preceeding s. On output, this value is set
    * to the index of the preceeding knot, or -1 if no preceeding knot is
    * found.
    */
   public Vector3d evalDx (double s, IntHolder lastIdx) {
      Vector3d dx = new Vector3d();
      if (numKnots() > 0) {
         Knot prev = getPreceedingKnot (s, lastIdx);
         if (prev == null) {
            prev = myKnots.get(0);
         }
         Knot next = getNextKnot (prev);
         if (next == null) {
            Knot pprev = getPrevKnot (prev);
            if (pprev == null) {
               dx.setZero();
               return dx;
            }
            else {
               next = prev;
               prev = pprev;
            }
         }
         prev.evalDx (dx, s, next);
      }
      return dx;
   }
   
   /**
    * Scans this spline from a ReaderTokenizer. The expected format consists of
    * an opening square bracket, followed by {@code 4 n} numeric values giving
    * the s and x values for each of the {@code n} knots, and terminated
    * with a closing square bracket. Whitespace characters are ignored. For
    * example,
    * <pre>
    *  [  0.0
    *     0.0 4.0 1.0
    *     1.0
    *     0.0 5.0 1.2
    *  ]
    * </pre>
    * describes a spline with two knots, with s and x  equal to {@code 0},
    * and {@code (0, 4, 1)})} for the first knot and {@code
    * 1} and {@code (0, 5, 1.2)} for the second knot.
    * 
    * @param rtok
    * Tokenizer from which to scan the spline
    * @param ref
    * optional reference object (not used)
    * @throws IOException
    * if an I/O or formatting error occured
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      clearKnots();
      rtok.scanToken ('[');
      Vector3d a0 = new Vector3d();
      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsNumber()) {
            throw new IOException ("knot s0 value expected, "+rtok);
         }
         double s0 = rtok.nval;
         try {
            a0.scan (rtok);
         }
         catch (IOException e) {
            throw new IOException ("error scanning knot x value, "+rtok);
         }
         addKnot (s0, a0);
      }
   }

   /**
    * Writes a text description of this spline to a PrintWriter. The format
    * is the same as that described for {@link #scan scan}.
    * 
    * @param pw
    * writer to which the spline should be written
    * @param fmt
    * describes how the numbers should be formatted
    * @param ref
    * optional reference object (not used)
    * @throws IOException
    * if an I/O error occured
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      if (myKnots.size() == 0) {
         pw.println ("[ ]");
      }
      else {
         pw.println ("[");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (Knot knot : myKnots) {
            pw.println (fmt.format(knot.myS0));
            pw.println (knot.myA0.toString(fmt));
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isWritable() {
      return true;
   }

   /**
    * Returns the number of knots in this spline.
    *
    * @return number of knots
    */
   public int numKnots() {
      return myKnots.size();
   }

   /**
    * Returns the {@code idx}-th knot in this spline.
    *
    * @param idx index of the requested knot 
    * @return {@code idx}-th knot
    */
   public Knot getKnot (int idx) {
      return myKnots.get (idx);
   }

   /**
    * Returns the first knot in this spline, or {@code null} if there
    * are no knots.
    *
    * @return first knot
    */
   public Knot getFirstKnot () {
      if (myKnots.size() > 0) {
         return myKnots.get (0);
      }
      else {
         return null;
      }
   }

   /**
    * Returns the last knot in this spline, or {@code null} if there
    * are no knots.
    *
    * @return last knot
    */
   public Knot getLastKnot () {
      if (myKnots.size() > 0) {
         return myKnots.get (numKnots()-1);
      }
      else {
         return null;
      }
   }

   /**
    * Returns the start value of the parameter controlling this spline.
    *
    * @return starting parameter value
    */
   public double getS0() { 
      if (myKnots.size() > 0) {
         return myKnots.get(0).getS0();
      }
      else {
         return 0;
      }
   }

   /**
    * Returns the last value of the parameter controlling this spline.
    *
    * @return last parameter value
    */
   public double getSLast() { 
      if (myKnots.size() > 0) {
         return myKnots.get(numKnots()-1).getS0();
      }
      else {
         return 0;
      }
   }

   /**
    * Returns the length of the parameter interval controlling this spline.
    *
    * @return parameter interval length
    */
   public double getSLength() { 
      return getSLast() - getS0();
   }

   /**
    * Returns an iterator for the knots in this spline.
    *
    * @return spline knot iterator
    */
   public Iterator<Knot> iterator() {
      return myKnots.iterator(); //new MyIterator();
   }

   /**
    * Sets this spline to be a copy of another spline.
    *
    * @param spline spline to copy
    */
   public void set (LinearSpline3d spline) {
      clearKnots();
      for (Knot knot : spline) {
         doAddKnot (myKnots.size(), knot.copy());
      }
   }

   /**
    * Creates a copy of this spline.
    */
   public LinearSpline3d copy () {
      LinearSpline3d spline = new LinearSpline3d();
      spline.set (this);
      return spline;
   }

   /**
    * Queries whether this spline is equal to another spline.
    *
    * @param spline spline to compare to this one
    * @return {@code true} if the splines are equal
    */
   public boolean equals (LinearSpline3d spline) {
      if (numKnots() != spline.numKnots()) {
         return false;
      }
      for (int i=0; i<numKnots(); i++) {
         if (!getKnot(i).equals (spline.getKnot(i))) {
            return false;
         }
      }
      return true;
   }
   

}
