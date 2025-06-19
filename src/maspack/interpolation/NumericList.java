/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.interpolation;

import java.util.Iterator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.io.*;

import maspack.interpolation.Interpolation.Order;
import maspack.numerics.PolynomialFit;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.util.*;

/**
 * A linked list of vector values arranged in order with respect to a parameter
 * t. Can be interpolated to produce a continuous vector function.
 *
 * <p>A numeric list can be constructed to contain a set of subvectors
 * representing rotations, using a representation specified by a RotationRep.
 * For Linear, Cubic and CubicStep interpolation, rotation subvectors are
 * interpolated as curves on the surface of SO(3), as originally described by
 * Ken Shoemake in his SIGGRAPH 1985 paper "Animating Rotation with Quaternion
 * Curves".
 */
public class NumericList
   implements Cloneable, Iterable<NumericListKnot>, Scannable {
   
   public boolean debug = false;

   private static final double RTOD = 180.0/Math.PI;
   private static final double DTOR = Math.PI/180.0;
   private static final double INF = Double.POSITIVE_INFINITY;

   // Can be used to identify subvectors within this list's numeric vector that
   // represent rotations, according to the representation described by
   // myRotationRep. The length of each subvector will be given by
   // myRotationRep.size(), and the starting offsets of the subvectors
   // are given by myRotationSubvecOffsets.
   private int[] myRotationSubvecOffsets = new int[0];
   private RotationRep myRotationRep = null;

   private int myVsize;
   protected NumericListKnot myHead;
   protected NumericListKnot myTail;
   protected NumericListKnot myLast;
   // protected boolean myExtendValuesP = false;

   protected Interpolation myInterpolation;
   protected static Interpolation defaultInterpolation =
      new Interpolation (Order.Step, false);

   // coefficient vectors for cubic-catmull interpolation. Initialized to
   // zero-size and their size will be enlarged on demand
   protected VectorNd a1 = new VectorNd (0);
   protected VectorNd a2 = new VectorNd (0);
   protected VectorNd a3 = new VectorNd (0);

   private boolean myMinMaxValid = false;
   private double myMaxValue; // max value across all vectors and elements
   private VectorNd myMaxValues = new VectorNd(); // max for each vector element
   private double myMinValue; // min value across all vectors and elements
   private VectorNd myMinValues = new VectorNd(); // min for each vector element

   private VectorNd myTmp0;
   private VectorNd myTmp1;

   private void allocateTmps (int size) {
      if (myTmp0 == null) {
         myTmp0 = new VectorNd (size);
      }
      else if (size > myTmp0.size()) {
         myTmp0.setSize (size);
      }
      if (myTmp1 == null) {
         myTmp1 = new VectorNd (size);
      }
      else if (size > myTmp1.size()) {
         myTmp1.setSize (size);
      }
   }

   /**
    * Sets the interpolation method for this list. The default is
    * <code>Step</code> with no end data extension.
    * 
    * @param method
    * new interpolation method.
    */
   public void setInterpolation (Interpolation method) {
      myInterpolation = new Interpolation (method);
   }

   /**
    * Sets the interpolation order for this list. The default is
    * <code>Step</code>.
    * 
    * @param order
    * new interpolation order;
    */
   public void setInterpolationOrder (Interpolation.Order order) {
      myInterpolation.setOrder (order);
   }

   /**
    * Returns the interpolation method for this list.
    * 
    * @return interpolation method
    * @see #setInterpolation
    */
   public Interpolation getInterpolation() {
      return myInterpolation;
   }

   /**
    * Returns the interpolation order for this list.
    * 
    * @return interpolation order
    * @see #setInterpolationOrder
    */
   public Interpolation.Order getInterpolationOrder() {
       return myInterpolation.getOrder();
   }

   /**
    * Creates an empty numeric list for holding vectors of a prescribed size.
    * 
    * @param vsize
    * size of the vectors that will form this list
    */
   public NumericList (int vsize) {
      setInterpolation (defaultInterpolation);
      myVsize = vsize;
   }

   /**
    * Creates an empty numeric list containing rotation subvectors with
    * specified representation and offsets.
    * 
    * @param vsize
    * size of the vectors that will form this list
    * @param rotRep
    * how rotations are represented with the subvectors
    * @param offsets
    * offsets for the rotation subvectors
    */
   public NumericList (int vsize, RotationRep rotRep, int[] offsets) {
      setInterpolation (defaultInterpolation);
      myVsize = vsize;
      myRotationRep = rotRep;
      setRotationSubvecOffsets (offsets);
   }

   /**
    * Returns the size of the vectors associated with this list.
    */
   public int getVectorSize() {
      return myVsize;
   }

   /**
    * Returns the size of the derivative vector associated with this list.
    * This will be different from the list's vector size if the list contains
    * rotation subvectors using a representation whose numeric size is not
    * equal to three.
    *
    * @return derivative vector size
    */
   public int getDerivSize() {
      if (numRotationSubvecs() > 0) {
         return myVsize - (numRotationSubvecs()*(myRotationRep.size()-3));
      }
      else {
         Order order = myInterpolation.getOrder();
         if ((order == Order.SphericalLinear || order == Order.SphericalCubic) &&
             (myVsize == 4 || myVsize == 7 || myVsize == 16)) {
            return myVsize == 4 ? 3 : 6;
         }
         else {
            return myVsize;
         }
      }
   }

   /**
    * Returns the representation associated with rotation subvectors of this
    * list, or null if there are no rotation subvectors.
    *
    * @return rotation subvector representation
    */
   public RotationRep getRotationRep() {
      return myRotationRep;
   }

   /**
    * Returns an array giving the offsets of any rotation subvectors associated
    * with this list. The array will have length 0 if there are no rotation
    * subvectors.
    *
    * @return offsets for the rotation subvectors
    */
   public int[] getRotationSubvecOffsets() {
      return Arrays.copyOf (
         myRotationSubvecOffsets, myRotationSubvecOffsets.length);
   }

   /**
    * Checks a set of specified rotation subvector offsets to ensure that
    * they do not overlap and all fit within the overall vector.
    *
    * @param offsets offset values to be checked
    * @param rotRep rotation representation associated with the subvectors
    * @param vsize overall vector size
    */
   public static void checkRotationSubvecOffsets (
      int[] offsets, RotationRep rotRep, int vsize) {
      int rsize = rotRep.size();
      for (int i=0; i<offsets.length; i++) {
         int off = offsets[i];
         if (i > 0 && off < offsets[i-1]+rsize) {
            throw new IllegalArgumentException (
               "Rotational subvector "+i+" overlaps with previous subvector");
         }
         if (off+rsize > vsize) { 
            throw new IllegalArgumentException (
               "Rotational subvector "+i+" extends beyond end of vector");
         }
      }
   }

   /**
    * Used internal to set rotation subvector offsets for this list.
    */
   void setRotationSubvecOffsets (int[] offsets) {
      checkRotationSubvecOffsets (offsets, myRotationRep, myVsize);
      myRotationSubvecOffsets = Arrays.copyOf (offsets, offsets.length);
   }

   /**
    * Queries the number of rotation subvectors.
    *
    * @return number of rotation subvectors
    */
   public int numRotationSubvecs() {
      return myRotationSubvecOffsets.length;
   }

   /**
    * Adds a knot into this numeric list. The knot will be added at the proper
    * location so that all t values are monotonically increasing. Any existing
    * knot which has the same t value as the new knot will be removed and
    * returned.
    * 
    * @param knot
    * knot to add to the list
    * @return existing knot with the same t value, if any
    * @throws IllegalArgumentException
    * if the knot's vector has a size not equal to the vector size for this
    * list.
    * @see #getVectorSize
    */
   public synchronized NumericListKnot add (NumericListKnot knot) {
      NumericListKnot existing = add (knot, myLast);
      myLast = knot;
      return existing;
   }

   /**
    * Adds a knot into this numeric list, and adjust the value of any rotation
    * subvector to the (redundant) representation "closest" to that in
    * the nearest existing knot.  The knot will be added at the proper location
    * so that all t values are monotonically increasing. Any existing knot
    * which has the same t value as the new knot will be removed and returned.
    * 
    * @param knot
    * knot to add to the list
    * @return existing knot with the same t value, if any
    * @throws IllegalArgumentException
    * if the knot's vector has a size not equal to the vector size for this
    * list.
    * @see #getVectorSize
    */
   public synchronized NumericListKnot addAndAdjustRotations (
      NumericListKnot knot) {
      adjustRotationValues (knot.v, knot.v, knot.t);
      NumericListKnot existing = add (knot, myLast);
      myLast = knot;
      return existing;
   }

   /**
    * Creates a knot with the specified values and time and adds it into this
    * numeric list. The knot will be added at the proper location so that all t
    * values are monotonically increasing. Any existing knot which has the same
    * t value as the new knot will be removed and returned.
    * 
    * @param vals
    * values for the knot
    * @param t
    * time at which the knot should be added
    * @return knot that was added
    * @throws IllegalArgumentException
    * if <code>values</code> has a size less than the vector size for this
    * list.
    * @see #getVectorSize
    */
   public synchronized NumericListKnot add (Vector vals, double t) {
      if (vals.size() < getVectorSize()) {
         throw new IllegalArgumentException (
            "Insufficinet number of values specified for knot point");
      }
      NumericListKnot knot = new NumericListKnot (getVectorSize());
      for (int i = 0; i < getVectorSize(); i++) {
         knot.v.set (i, vals.get (i));
      }
      knot.t = t;
      add (knot);
      return knot;
   }

   /**
    * Creates a knot with the specified values and time and adds it into this
    * numeric list. The knot will be added at the proper location so that all t
    * values are monotonically increasing. Any existing knot which has the same
    * t value as the new knot will be removed and returned.
    * 
    * @param t
    * time at which the knot should be added
    * @param vals
    * values for the knot; the number must be {@code >=} the list's vector
    * size
    * @return knot that was added
    * @throws IllegalArgumentException
    * if <code>values</code> has a size less than the vector size for this
    * list.
    * @see #getVectorSize
    */
   public synchronized NumericListKnot add (double t, double... vals) {
      if (vals.length < getVectorSize()) {
         throw new IllegalArgumentException (
            "Insufficinet number of values specified for knot point");
      }
      NumericListKnot knot = new NumericListKnot (getVectorSize());
      for (int i = 0; i < getVectorSize(); i++) {
         knot.v.set (i, vals[i]);
      }
      knot.t = t;
      add (knot);
      return knot;
   }

   public synchronized NumericListKnot add (
      NumericListKnot knot, NumericListKnot last) {
      NumericListKnot existing = null;
      if (knot.v.size() != myVsize) {
         throw new IllegalArgumentException (
            "Knot vector has size "+knot.v.size() + ", expecting " + myVsize);
      }
      if (myHead == null) { // list is empty
         knot.next = null;
         knot.prev = null;
         myHead = knot;
         myTail = knot;
      }
      else {
         NumericListKnot anchor = findKnotAtOrBefore (knot.t, last);
         if (anchor.t < knot.t) { // anchor is before knot, so add knot
                                    // immediately after it
            knot.prev = anchor;
            knot.next = anchor.next;
            if (anchor.next == null) {
               myTail = knot;
            }
            else {
               anchor.next.prev = knot;
            }
            anchor.next = knot;

         }
         else if (anchor.t > knot.t) { // anchor is after knot, so add knot to
                                       // the beginning of list
            knot.next = anchor;
            knot.prev = null;
            myHead = knot;
            anchor.prev = knot;
         }
         else { // anchor.t == knot.t, so delete it and replace it with knot
            knot.next = anchor.next;
            knot.prev = anchor.prev;
            if (anchor.prev == null) {
               myHead = knot;
            }
            else {
               anchor.prev.next = knot;
            }
            if (anchor.next == null) {
               myTail = knot;
            }
            else {
               anchor.next.prev = knot;
            }
            existing = anchor;
         }
      }
      myMinMaxValid = false;
      knot.myList = this;
      return existing;
   }
   
   public void shiftTime(double t) {
      NumericListKnot nlk = myHead; 
      do {
         nlk.t += t;
         nlk = nlk.next;
      } while (nlk != myTail);
      nlk.t += t;
   }

   /**
    * Returns a vector giving the minimum values across all knots in this list.
    * Should not be modified.
    */
   public VectorNd getMinValues() {
      if (!myMinMaxValid) {
         updateMinMaxValues();
      }      
      return myMinValues;
   }

   /**
    * Returns a vector giving the maximum values across all knots in this list.
    * Should not be modified.
    */
   public VectorNd getMaxValues() {
      if (!myMinMaxValid) {
         updateMinMaxValues();
      }
      return myMaxValues;
   }

   public void getMinMaxValues (double[] minMax) {
      if (!myMinMaxValid) {
         updateMinMaxValues();
      }
      minMax[0] = myMinValue;
      minMax[1] = myMaxValue;
   }

   public void getMinMaxValues (VectorNd min, VectorNd max) {
      if (!myMinMaxValid) {
         updateMinMaxValues();
      }
      min.set (myMinValues);
      max.set (myMaxValues);
   }

   private void updateMinMaxValues (NumericListKnot knot) {
      double[] vbuf = knot.v.getBuffer();
      double[] vmin = myMinValues.getBuffer();
      double[] vmax = myMaxValues.getBuffer();
      for (int i = 0; i < myVsize; i++) {
         double x = vbuf[i];
         if (x > vmax[i]) {
            vmax[i] = x;
         }
         if (x < vmin[i]) {
            vmin[i] = x;
         }
      }
   }

   private void updateMinMaxValues() {
      myMinValues.setSize (myVsize);
      myMaxValues.setSize (myVsize);
      if (myHead != null) {
         myMinValues.setAll (INF);
         myMaxValues.setAll (-INF);
         for (NumericListKnot knot = myHead; knot != null; knot = knot.next) {
            updateMinMaxValues (knot);
         }
         myMinValue = myMinValues.minElement();
         myMaxValue = myMaxValues.maxElement();
      }
      else {
         myMinValues.setZero();
         myMaxValues.setZero();
         myMinValue = 0;
         myMaxValue = 0;
      }
      myMinMaxValid = true;
   }

   /**
    * Returns the sum of the square of each vector element over all the knots.
    *
    * @param sumsqr returns the mean square values
    */
   public void getSumSquaredValues (VectorNd sumsqr) {
      sumsqr.setSize (getVectorSize());
      sumsqr.setZero();
      for (NumericListKnot knot = myHead; knot != null; knot = knot.next) {
         for (int i=0; i<getVectorSize(); i++) {
            double x = knot.v.get(i);
            sumsqr.add (i, x*x);
         }
      }
   }

//   /**
//    * Queries the minimum and maximum values of each vector
//    * element over all the knots.
//    *
//    * @param min returns the minimum values
//    * @param max returns the maximum values
//    */
//   public void getMinMaxValues (VectorNd min, VectorNd max) {
//      min.setSize (getVectorSize());
//      max.setSize (getVectorSize());
//      for (NumericListKnot knot = myHead; knot != null; knot = knot.next) {
//         if (knot == myHead) {
//            min.set (knot.v);
//            max.set (knot.v);
//         }
//         else {
//            for (int i=0; i<getVectorSize(); i++) {
//               double x = knot.v.get(i);
//               if (x > max.get(i)) {
//                  max.set (i, x);
//               }
//               else if (x < min.get(i)) {
//                  min.set (i, x);
//               }
//            }
//         }
//      }
//   }
//
   /**
    * Removes and return the knot at time t, if any.
    * 
    * @param t
    * time of the knot to remove
    * @return removed knot, if any
    */
   public NumericListKnot remove (double t) {
      NumericListKnot knot = findKnotClosest (t);
      if (knot != null && knot.t == t) {
         remove (knot);
         // see if min/max value will need updating
         boolean minMaxUpdateNeeded = false;
         double[] vbuf = knot.v.getBuffer();
         for (int i = 0; i < myVsize; i++) {
            double x = vbuf[i];
            if (x >= myMaxValues.get(i)) // using >= for paranoid reasons
            {
               minMaxUpdateNeeded = true;
               break;
            }
            if (x <= myMinValues.get(i)) {
               minMaxUpdateNeeded = true;
               break;
            }
         }
         if (minMaxUpdateNeeded) {
            updateMinMaxValues();
         }
         return knot;
      }
      else {
         return null;
      }
   }

   /**
    * Removes a knot from this numeric list. It is up to the application to
    * ensure that the knot is currently a member of this list.
    * 
    * @param knot
    * knot to remove
    */
   public void remove (NumericListKnot knot) {
      if (knot.prev == null) {
         myHead = knot.next;
      }
      else {
         knot.prev.next = knot.next;
      }
      if (knot.next == null) {
         myTail = knot.prev;
      }
      else {
         knot.next.prev = knot.prev;
      }
      knot.myList = null;
      myMinMaxValid = false;
   }

   /**
    * Removes all knots after a specific knot in this list. It is up to the
    * application to ensure that the specified knot is a member of the list.
    * 
    * @param knot
    * all knots following this one will be removed
    */
   public synchronized void clearAfter (NumericListKnot knot) {
      for (NumericListKnot knotx = knot.next; knotx != null; knotx = knotx.next) {
         knotx.myList = null;
      }
      knot.next = null;
      myTail = knot;
      myMinMaxValid = false;
   }

   /**
    * Finds the knot whose t value is closest to, and if possible less or equal
    * to, a specified value.
    * 
    * @param t
    * specified value
    * @return nearest knot less than t
    */
   public NumericListKnot findKnotAtOrBefore (double t, NumericListKnot last) {
      if (last == null || last.myList != this) { // no hint from last knot
                                                   // point.
         if (myHead == null) {
            return null; // list is empty, so knot must be null
         }
         else { // start search at Head or Tail, whichever is closer to t
            double tmid = (myHead.t + myTail.t) / 2;
            last = (t <= tmid ? myHead : myTail);
         }
      }
      if (last.t > t) { // last is above t; try to move closer
         while (last.t > t && last.prev != null) {
            last = last.prev;
         }
      }
      else {
         while (last.next != null && last.next.t <= t) {
            last = last.next;
         }
      }
      return last;
   }

   /**
    * Finds the knot whose t value is closest to a specified value.
    * 
    * @param t
    * specified value
    * @return knot closest to t
    */
   public NumericListKnot findKnotClosest (double t) {
      NumericListKnot knot = findKnotClosest (t, myLast);
      myLast = knot;
      return knot;
   }

   public synchronized NumericListKnot findKnotClosest (
      double t, NumericListKnot last) {
      if (myHead == null) {
         return null;
      }
      NumericListKnot knot = findKnotAtOrBefore (t, last);
      if (knot.t >= t) { // t lies on or before any knot, so this knot must
         // be the closest
         return knot;
      }
      else if (knot.next == null) { // t lies after the end of the list, so this
         // knot is again the closest
         return knot;
      }
      else {
         NumericListKnot next = knot.next;
         if (Math.abs (knot.t - t) < Math.abs (next.t - t)) {
            return knot;
         }
         else {
            last = next;
            return next;
         }
      }
   }

   /**
    * Interpolates the value associated with a particular value of t, based on
    * the current contents of this list. The interpolation is done using the
    * list's interpolation method, as returned by {@link #getInterpolation}.
    * If t lies before the start or after the end of the list, the
    * interpolation value is set to the first or last knot value.
    * 
    * @param v
    * stores the interpolation result
    * @param t
    * value to interpolate for
    */
   public void interpolate (VectorNd v, double t) {
      myLast = interpolate (v, t, myInterpolation, myLast);
   }

   /**
    * Finds the derivative associated with a particular value of t, by
    * differentiating the function implied by the knot points and the list's
    * interpolation method, as returned by {@link #getInterpolation}. If t lies
    * before the start or after the end of the list, the derivative will be
    * zero.
    * 
    * @param v
    * stores the derivative result
    * @param t value to find the derivative at
    */
   public void interpolateDeriv (VectorNd v, double t) {
      myLast = interpolateDeriv (v, t, myInterpolation, myLast);
   }

   /**
    * Finds the derivative associated with a particular value of t, based on
    * numerical differentation of the current knot points. This is done by
    * linearly interpolating numerical derivatives computed at the mid-points
    * between knots. If t lies before the start or after the end of the list,
    * the derivative will be zero.
    * 
    * @param v
    * stores the derivative result
    * @param t
    * value to find the derivative for
    */
   public void numericalDeriv (VectorNd v, double t) {
      myLast = numericalDeriv (v, t, myLast);
   }

   /**
    * Copies the values {@code v} into {@code vr}, while adjusting the value of
    * any rotation subvector to the (redundant) representation "closest" to
    * that in the knot nearest to {@code t}.
    * 
    * @param vr
    * stores the result
    * @param v
    * values to copy and adjust if needed
    * @param t
    * time associated with the values
    */
   public void adjustRotationValues (VectorNd vr, VectorNd v, double t) {
      myLast = adjustRotationValues (vr, v, t, myLast);
   }

   /**
    * Copies the values {@code v} into {@code vr}, while adjusting the value of
    * any rotation subvector to the (redundant) representation "closest" to
    * that in the knot nearest to {@code t}.
    *
    * <p>The method returns the knot that is nearest to {@code t}.  By using
    * this returned value as the {@code last} argument in subsequent method
    * calls, one can traverse forward or backward (in time) along the list with
    * O(1) complexity.
    * 
    * @param vr
    * stores the result
    * @param v
    * values to copy and adjust if needed
    * @param t
    * time associated with the values
    * @param last
    * knot point used to start the search for the knot containing {@code t}
    * @return nearest knot to {@code t}
    */
   public NumericListKnot adjustRotationValues (
      VectorNd vr, VectorNd v, double t, NumericListKnot last) {
      if (v.size() != myVsize) {
         throw new IllegalArgumentException (
            "v has size "+v.size()+"; list has vector size "+myVsize);
      }
      NumericListKnot near = null;
      if (numRotationSubvecs() > 0) {
         near = findKnotClosest (t, last);
      }
      if (near == null) {
         if (vr != v) {
            vr.set (v);
         }
      }
      else {
         double[] rbuf = near.v.getBuffer();
         double[] obuf = vr.getBuffer();
         double[] vbuf = v.getBuffer();

         Quaternion qv = new Quaternion();
         int i=0;
         int k = 0;
         int off = myRotationSubvecOffsets[0];
         while (i<myVsize) {
            if (off == i) {
               qv.set (vbuf, off, myRotationRep, /*scale*/1);
               qv.get (obuf, rbuf, off, myRotationRep, /*scale*/1);
               i += myRotationRep.size();
               if (++k < numRotationSubvecs()) {
                  off = myRotationSubvecOffsets[k];
               }
               else {
                  off = -1;
               }
            }
            else {
               obuf[i] = vbuf[i];
               i++;
            }
         }         
      }
      return near;
   }

   public void interpolateCubic (VectorNd v, NumericListKnot prev, double t) {
      // resize interpolation vectors if necessary
      if (a1.size() != myVsize) {
         a1.setSize (myVsize);
         a2.setSize (myVsize);
         a3.setSize (myVsize);
      }
      NumericListKnot next = prev.next;

      // TODO Chad: fill in a3, a2, a1

      double s = (t - prev.t) / (next.t - prev.t);
      v.scaledAdd (s, a3, a2);
      v.scaledAdd (s, v, a1);
      v.scaledAdd (s, v, prev.v);
   }

   public synchronized NumericListKnot interpolate (
      VectorNd v, double t, Interpolation method, NumericListKnot last) {
      return interpolate (v, t, method.myOrder, method.myDataExtendedP, last);
   }

   public synchronized NumericListKnot interpolateDeriv (
      VectorNd v, double t, Interpolation method, NumericListKnot last) {
      return interpolateDeriv (v, t, method.myOrder, last);
   }

   private void interpLinear (NumericListKnot prev, VectorNd v, double t) {
      NumericListKnot next = prev.next; // next is not null by assumption
      double s = (t-prev.t)/(next.t-prev.t);

      double[] pbuf = prev.v.getBuffer();
      double[] nbuf = next.v.getBuffer();
      double[] vbuf = v.getBuffer();
      // rbuf gives reference values for resolving rotation representation
      double[] rbuf = (s < 0.5 ? pbuf : nbuf);     
      if (numRotationSubvecs() == 0) {
         for (int i=0; i<myVsize; i++) {
            vbuf[i] = (1-s)*pbuf[i] + s*nbuf[i];
         }
      }
      else {
         Quaternion qp = new Quaternion();
         Quaternion qn = new Quaternion();
         Quaternion qv = new Quaternion();
         int i=0;
         int k = 0;
         int off = myRotationSubvecOffsets[0];
         while (i<myVsize) {
            if (off == i) {
               qp.set (pbuf, off, myRotationRep, /*scale*/1);
               qn.set (nbuf, off, myRotationRep, /*scale*/1);
               //rvec.getQuaternion (qp, pbuf);
               //rvec.getQuaternion (qn, nbuf);
               qv.sphericalInterpolate (qp, s, qn);
               qv.get (vbuf, rbuf, off, myRotationRep, /*scale*/1);
               if (debug) {
                  double pang = pbuf[off+3];
                  double nang = nbuf[off+3];
                  AxisAngle axisAng = new AxisAngle();
                  Quaternion qd = new Quaternion();
                  qd.mulInverseLeft (qp, qn);
                  qd.getAxisAngle (axisAng);
                  System.out.printf (
                     "t=%g s=%g ang=%g del=%g chk2=%g\n", 
                     t, s, vbuf[off+3], RTOD*axisAng.angle,
                     (1-s)*pang+s*nang);
                  // System.out.println ("    nv=" + next.v);
                  // System.out.println ("    qp=" + qp);
                  // System.out.println ("    qn=" + qn);
                  // System.out.println ("    qd=" + qd);
               }
               //rvec.setRotation (vbuf, rbuf, qv);
               i += myRotationRep.size();
               if (++k < numRotationSubvecs()) {
                  off = myRotationSubvecOffsets[k];
               }
               else {
                  off = -1;
               }
            }
            else {
               vbuf[i] = (1-s)*pbuf[i] + s*nbuf[i];
               i++;
            }
         }
      }
   }

   private void interpLinearDeriv (NumericListKnot prev, VectorNd v, double t) {
      NumericListKnot next = prev.next; // next is not null by assumption

      double h = next.t-prev.t; // time difference

      double[] pbuf = prev.v.getBuffer();
      double[] nbuf = next.v.getBuffer();
      double[] vbuf = v.getBuffer();

      if (numRotationSubvecs() == 0) {
         for (int i=0; i<v.size(); i++) {
            vbuf[i] = (nbuf[i]-pbuf[i])/h;
         }
      }
      else {
         Quaternion qp = new Quaternion();
         Quaternion qn = new Quaternion();
         Vector3d w = new Vector3d();
         int i=0; // index into knot buffers
         int j=0; // index into vbuf, which will be <= i
         int k = 0;
         int off = myRotationSubvecOffsets[k];
         while (i<myVsize) {
            if (off == i) {
               qp.set (pbuf, off, myRotationRep, /*scale*/1);
               qn.set (nbuf, off, myRotationRep, /*scale*/1);
               //rvec.getQuaternion (qp, pbuf);
               //rvec.getQuaternion (qn, nbuf);
               qp.sphericalVelocity (w, qn, h);
               w.get (vbuf, j);
               i += myRotationRep.size();
               j += 3;
               if (++k < numRotationSubvecs()) {
                  off = myRotationSubvecOffsets[k];
               }
               else {
                  off = -1;
               }
            }
            else {
               vbuf[j] = (nbuf[i]-pbuf[i])/h;
               j++;
               i++;
            }
         }
      }
   }

   private void numericalDeriv (
      VectorNd v, double t,
      NumericListKnot knot0, NumericListKnot knot1, NumericListKnot knot2) {

      double[] buf0 = knot0.v.getBuffer();
      double[] buf1 = knot1.v.getBuffer();
      double[] buf2 = knot2.v.getBuffer();
      double[] vbuf = v.getBuffer();

      double h0 = knot1.t-knot0.t; // time difference between knot 1 and 0
      double h1 = knot2.t-knot1.t; // time difference between knot 2 and 1

      double s = (2*t - (knot0.t+knot1.t))/(h0 + h1);
      double a0 = (s-1)/h0;
      double a1 = (1-s)/h0 - s/h1;
      double a2 = s/h1;

      if (numRotationSubvecs() == 0) {
         for (int i=0; i<v.size(); i++) {
            vbuf[i] = a0*buf0[i] + a1*buf1[i] + a2*buf2[i];
         }
      }
      else {
         Quaternion q0 = new Quaternion();
         Quaternion q1 = new Quaternion();
         Quaternion q2 = new Quaternion();
         Vector3d w0 = new Vector3d();
         Vector3d w1 = new Vector3d();
         Vector3d wr = new Vector3d();
         int i=0; // index into knot buffers
         int j=0; // index into vbuf, which will be <= i
         int k = 0;
         int off = myRotationSubvecOffsets[k];
         while (i<myVsize) {
            if (off == i) {
               q0.set (buf0, off, myRotationRep, /*scale*/1);
               q1.set (buf1, off, myRotationRep, /*scale*/1);
               q2.set (buf2, off, myRotationRep, /*scale*/1);
               q0.sphericalVelocity (w0, q1, h0);
               q1.sphericalVelocity (w1, q2, h1);
               wr.combine (1-s, w0, s, w1);
               wr.get (vbuf, j);
               i += myRotationRep.size();
               j += 3;
               if (++k < numRotationSubvecs()) {
                  off = myRotationSubvecOffsets[k];
               }
               else {
                  off = -1;
               }
            }
            else {
               vbuf[j] = a0*buf0[i] + a1*buf1[i] + a2*buf2[i];
               j++;
               i++;
            }
         }
      }      
   }

   private void interpParabolic (NumericListKnot prev, VectorNd v, double t) {
      NumericListKnot prevprev = prev.prev;
      if (prevprev == null)
         prevprev = prev;
      NumericListKnot next = prev.next;
      if (next == null)
         next = prev;

      ParabolicInterpolation para = new ParabolicInterpolation();
      para.setTimeofKnots (prevprev.t, prev.t, next.t);
      int VectorSize = prev.v.size();
      v.setSize (VectorSize);
      double holder;
      double vValues[] = new double[3];
      for (int i = 0; i < VectorSize; i++) {
         vValues[0] = prevprev.v.get (i);
         vValues[1] = prev.v.get (i);
         vValues[2] = next.v.get (i);
         holder = para.interpolate (t, vValues);
         if (Double.isNaN (holder))
            holder = 0;
         v.set (i, holder);
      }
   }

   private void getRotation (Quaternion qr, VectorNd v) {
      double[] buf = v.getBuffer();
      if (v.size() == 4) {
         // axis angle
         Vector3d axis = new Vector3d (buf[0], buf[1], buf[2]);
         double ang = Math.toRadians (buf[3]);
         axis.scale (Math.sin(ang/2));
         qr.set (Math.cos(ang/2), axis);
      }
      else if (v.size() == 7) {
          // axis angle
          Vector3d axis = new Vector3d (buf[3], buf[4], buf[5]);
          double ang = Math.toRadians (buf[6]);
          axis.scale (Math.sin(ang/2));
          qr.set (Math.cos(ang/2), axis);
       }
      else if (v.size() == 16) {
         RotationMatrix3d R = new RotationMatrix3d();
         R.set (buf[0], buf[1], buf[2],
                buf[4], buf[5], buf[6],
                buf[8], buf[9], buf[10]);
         qr.set (R);
      }
   }

   static void getAngularVelocity (
      Vector3d w, Quaternion q0, Quaternion q1, double h) {
      Quaternion qd = new Quaternion();
      qd.mulInverseLeft (q0, q1);
      qd.log (w);
      w.scale (2/h);
      // transform velocity into global coordinates
      q0.transform (w, w);
   }

   /**
    * Get angular velocity from three quaternions instead of two, to make sure
    * that the velocity from q0 to q2 is consistent with the velocities from q0
    * to q1 and q1 to q2. This is necessary in case the default "route" from q0
    * to q2 is different than the route q0-q1-q2.
    */
   static void getAngularVelocity (
      Vector3d w, Quaternion q0, Quaternion q1, Quaternion q2, double h) {
      Vector3d wtmp= new Vector3d();
      Quaternion qd = new Quaternion();
      qd.mulInverseLeft (q0, q1);
      qd.log (w);
      qd.mulInverseLeft (q1, q2);
      qd.log (wtmp);
      w.add (wtmp);
      w.scale (2/h);
      // transform velocity into global coordinates
      q0.transform (w, w);
   }

   private void getPositionVelocity (
      Vector3d vr, Vector3d p0, Vector3d p1, double h) {
      vr.sub (p1, p0);
      vr.scale (1/h);
   }

   private void setRotation (VectorNd v, Quaternion q) {
      double[] buf = v.getBuffer();
      if (v.size() == 4) {
         AxisAngle axisAng = new AxisAngle();
         q.getAxisAngle (axisAng);
         buf[0] = axisAng.axis.x;
         buf[1] = axisAng.axis.y;
         buf[2] = axisAng.axis.z;
         buf[3] = Math.toDegrees (axisAng.angle);
      }
      else if (v.size() == 7) {
          AxisAngle axisAng = new AxisAngle();
          q.getAxisAngle (axisAng);
          buf[3] = axisAng.axis.x;
          buf[4] = axisAng.axis.y;
          buf[5] = axisAng.axis.z;
          buf[6] = Math.toDegrees (axisAng.angle);
      }
      else if (v.size() == 16) {
         RotationMatrix3d R = new RotationMatrix3d();
         R.set (q);
         buf[0] = R.m00; buf[1] = R.m01; buf[2]  = R.m02;
         buf[4] = R.m10; buf[5] = R.m11; buf[6]  = R.m12;
         buf[8] = R.m20; buf[9] = R.m21; buf[10] = R.m22;
         buf[12] = 0; buf[13] = 0; buf[14] = 0; buf[15] = 1;
      }
   }

   private void setAngularVelocity (VectorNd v, Vector3d w) {
      if (v.size() == 3) {
         v.set (w);
      }
      else {
         if (v.size() < 6) {
            System.out.println ("vsize=" + v.size());
         }
         v.setSubVector (3, w);
      }
   }

   private void getPosition (VectorNd v, Vector3d pos) {
      double[] buf = v.getBuffer();
      if (v.size() == 7) {
          pos.set (buf[0], buf[1], buf[2]);
      }
      else if (v.size() == 16) {
         pos.set (buf[3], buf[7], buf[11]);
      }
   }

   private void setPosition (VectorNd v, Vector3d pos) {
      double[] buf = v.getBuffer();
      if (v.size() == 7) {
          buf[0] = pos.x;
          buf[1] = pos.y;
          buf[2] = pos.z;
      }
      else if (v.size() == 16) {
         buf[3] = pos.x;
         buf[7] = pos.y;
         buf[11] = pos.z;
      }
   }

   private void interpLinearRotation (
      NumericListKnot prev, VectorNd v, double t) {

      NumericListKnot next = prev.next;
      double s = (t-prev.t)/(next.t-prev.t);
      Quaternion q0 = new Quaternion();
      Quaternion q1 = new Quaternion();
      getRotation (q0, prev.v);
      getRotation (q1, next.v);
      q0.sphericalInterpolate (q0, s, q1);
      setRotation (v, q0);
   }

   private void interpLinearRotationDeriv (
      NumericListKnot prev, VectorNd v, double t) {

      NumericListKnot next = prev.next;
      double h = (next.t-prev.t);
      Quaternion q0 = new Quaternion();
      Quaternion q1 = new Quaternion();
      Vector3d wr = new Vector3d();
      getRotation (q0, prev.v);
      getRotation (q1, next.v);
      q0.sphericalVelocity (wr, q1, h);
      setAngularVelocity (v, wr);
   }

   private void numericalRotationDeriv (
      VectorNd v, double t,
      NumericListKnot knot0, NumericListKnot knot1, NumericListKnot knot2) {
      
      double h0 = (knot1.t-knot0.t);
      double h1 = (knot2.t-knot1.t);
      double s = (2*t - (knot0.t+knot1.t))/(h0 + h1);
      Quaternion q0 = new Quaternion();
      Quaternion q1 = new Quaternion();
      Quaternion q2 = new Quaternion();
      Vector3d w0 = new Vector3d();
      Vector3d w1 = new Vector3d();
      Vector3d wr = new Vector3d();
      getRotation (q0, knot0.v);
      getRotation (q1, knot1.v);
      getRotation (q2, knot2.v);
      q0.sphericalVelocity (w0, q1, h0);
      q1.sphericalVelocity (w1, q2, h1);
      wr.combine (1-s, w0, s, w1);
      setAngularVelocity (v, wr);
   }

   private void interpLinearPosition (
      NumericListKnot prev, VectorNd v, double t) {

      NumericListKnot next = prev.next;
      double s = (t-prev.t)/(next.t-prev.t);
      Vector3d p0 = new Vector3d();
      Vector3d p1 = new Vector3d();
      getPosition (prev.v, p0);
      getPosition (next.v, p1);
      p1.combine (1-s, p0, s, p1);
      setPosition (v, p1);
   }

   private void interpLinearPositionDeriv (
      NumericListKnot prev, VectorNd v, double t) {

      NumericListKnot next = prev.next;
      double h = (next.t-prev.t);
      Vector3d p0 = new Vector3d();
      Vector3d p1 = new Vector3d();
      getPosition (prev.v, p0);
      getPosition (next.v, p1);
      Vector3d vel = new Vector3d();
      vel.sub (p1, p0);
      vel.scale (1/h);
      v.setSubVector (0, vel);
   }

   private void numericalPositionDeriv (
      VectorNd v, double t,
      NumericListKnot knot0, NumericListKnot knot1, NumericListKnot knot2) {

      double h0 = (knot1.t-knot0.t);
      double h1 = (knot2.t-knot1.t);
      double s = (2*t - (knot0.t+knot1.t))/(h0 + h1);
      double a0 = (s-1)/h0;
      double a1 = (1-s)/h0 - s/h1;
      double a2 = s/h1;

      Vector3d p0 = new Vector3d();
      Vector3d p1 = new Vector3d();
      Vector3d p2 = new Vector3d();
      getPosition (knot0.v, p0);
      getPosition (knot1.v, p1);
      getPosition (knot2.v, p2);
      Vector3d vr = new Vector3d();
      vr.scaledAdd (a0, p0);
      vr.scaledAdd (a1, p1);
      vr.scaledAdd (a2, p2);
      v.setSubVector (0, vr);
   }

   private void interpCubicRotation (
      NumericListKnot prev, VectorNd v, double t) {

      NumericListKnot next = prev.next;
      double s = (t-prev.t)/(next.t-prev.t);
      NumericListKnot prevprev = prev.prev;
      NumericListKnot nextnext = next.next;

      Quaternion q0 = new Quaternion();
      Quaternion q1 = new Quaternion();
      Quaternion qr = new Quaternion();
      Vector3d w0 = new Vector3d();
      Vector3d w1 = new Vector3d();
      getRotation (q0, prev.v);
      getRotation (q1, next.v);
      if (prevprev != null) {
         getRotation (qr, prevprev.v);
         getAngularVelocity (w0, qr, q0, q1, next.t-prevprev.t);
      }
      else {
         getAngularVelocity (w0, q0, q1, next.t-prev.t);
      }
      if (nextnext != null) {
         getRotation (qr, nextnext.v);
         getAngularVelocity (w1, q0, q1, qr, nextnext.t-prev.t);
      }
      else {
         getAngularVelocity (w1, q0, q1, next.t-prev.t);
      }
      Quaternion.sphericalHermiteGlobal (
         qr, null, q0, w0, q1, w1, s, next.t-prev.t);
      setRotation (v, qr);
   }

   private void interpCubicRotationDeriv (
      NumericListKnot prev, VectorNd v, double t) {

      NumericListKnot next = prev.next;
      double s = (t-prev.t)/(next.t-prev.t);
      NumericListKnot prevprev = prev.prev;
      NumericListKnot nextnext = next.next;

      Quaternion q0 = new Quaternion();
      Quaternion q1 = new Quaternion();
      Quaternion qr = new Quaternion();
      Vector3d w0 = new Vector3d();
      Vector3d w1 = new Vector3d();
      Vector3d wr = new Vector3d();
      getRotation (q0, prev.v);
      getRotation (q1, next.v);
      if (prevprev != null) {
         getRotation (qr, prevprev.v);
         getAngularVelocity (w0, qr, q0, q1, next.t-prevprev.t);
      }
      else {
         getAngularVelocity (w0, q0, q1, next.t-prev.t);
      }
      if (nextnext != null) {
         getRotation (qr, nextnext.v);
         getAngularVelocity (w1, q0, q1, qr, nextnext.t-prev.t);
      }
      else {
         getAngularVelocity (w1, q0, q1, next.t-prev.t);
      }
      Quaternion.sphericalHermiteGlobal (
         qr, wr, q0, w0, q1, w1, s, next.t-prev.t);
      setAngularVelocity (v, wr);
   }

   private void interpCubicPosition (
      NumericListKnot prev, VectorNd v, double t) {

      NumericListKnot next = prev.next;
      double s = (t-prev.t)/(next.t-prev.t);
      NumericListKnot prevprev = prev.prev;
      NumericListKnot nextnext = next.next;

      Vector3d p0 = new Vector3d();
      Vector3d p1 = new Vector3d();
      Vector3d pr = new Vector3d();
      Vector3d v0 = new Vector3d();
      Vector3d v1 = new Vector3d();
      getPosition (prev.v, p0);
      getPosition (next.v, p1);
      if (prevprev != null) {
         getPosition (prevprev.v, pr);
         getPositionVelocity (v0, pr, p1, next.t-prevprev.t);
      }
      else {
         getPositionVelocity (v0, p0, p1, next.t-prev.t);
      }
      if (nextnext != null) {
         getPosition (nextnext.v, pr);
         getPositionVelocity (v1, p0, pr, nextnext.t-prev.t);
      }
      else {
         getPositionVelocity (v1, p0, p1, next.t-prev.t);
      }
      Vector3d.hermiteInterpolate (pr, p0, v0, p1, v1, s, next.t-prev.t);
      setPosition (v, pr);
   }

   private void interpCubicPositionDeriv (
      NumericListKnot prev, VectorNd v, double t) {

      NumericListKnot next = prev.next;
      double s = (t-prev.t)/(next.t-prev.t);
      NumericListKnot prevprev = prev.prev;
      NumericListKnot nextnext = next.next;

      Vector3d p0 = new Vector3d();
      Vector3d p1 = new Vector3d();
      Vector3d pr = new Vector3d();
      Vector3d v0 = new Vector3d();
      Vector3d v1 = new Vector3d();
      Vector3d vr = new Vector3d();
      getPosition (prev.v, p0);
      getPosition (next.v, p1);
      if (prevprev != null) {
         getPosition (prevprev.v, pr);
         getPositionVelocity (v0, pr, p1, next.t-prevprev.t);
      }
      else {
         getPositionVelocity (v0, p0, p1, next.t-prev.t);
      }
      if (nextnext != null) {
         getPosition (nextnext.v, pr);
         getPositionVelocity (v1, p0, pr, nextnext.t-prev.t);
      }
      else {
         getPositionVelocity (v1, p0, p1, next.t-prev.t);
      }
      Vector3d.hermiteVelocity (vr, p0, v0, p1, v1, s, next.t-prev.t);
      v.setSubVector (0, vr);
   }

   private void interpCubicStep (
      NumericListKnot prev, VectorNd v, double t) {

      NumericListKnot next = prev.next;
      double h = (next.t-prev.t);
      double s = (t-prev.t)/(next.t-prev.t);

      double[] pbuf = prev.v.getBuffer();
      double[] nbuf = next.v.getBuffer();
      double[] vbuf = v.getBuffer();
      // rbuf gives reference values for resolving rotation representation
      double[] rbuf = (s < 0.5 ? pbuf : nbuf);      
      
      double b1 = (2*s-3)*s*s;

      if (numRotationSubvecs() == 0) { 
         for (int i=0; i<myVsize; i++) {
            vbuf[i] = b1*(pbuf[i]-nbuf[i]) + pbuf[i];
         }
      }
      else {
         Quaternion q0 = new Quaternion();
         Quaternion q1 = new Quaternion();
         Quaternion qr = new Quaternion();
         Vector3d w0 = new Vector3d(); // leave at zero
         Vector3d w1 = new Vector3d(); // leave at zero
         int i=0;
         int k = 0;
         int off = myRotationSubvecOffsets[k];
         while (i<myVsize) {
            if (off == i) {
               q0.set (pbuf, off, myRotationRep, /*scale*/1);
               q1.set (nbuf, off, myRotationRep, /*scale*/1);
               //rvec.getQuaternion (q0, pbuf); 
               //rvec.getQuaternion (q1, nbuf);
               Quaternion.sphericalHermiteGlobal (
                  qr, null, q0, w0, q1, w1, s, h);
               qr.get (vbuf, rbuf, off, myRotationRep, /*scale*/1);
               //rvec.setRotation (vbuf, rbuf, qr);
               i += myRotationRep.size();
               if (++k < numRotationSubvecs()) {
                  off = myRotationSubvecOffsets[k];
               }
               else {
                  off = -1;
               }
            }
            else {
               vbuf[i] = b1*(pbuf[i]-nbuf[i]) + pbuf[i];
               i++;
            }
         }         
      }
   }
   
   private void interpCubicStepDeriv (
      NumericListKnot prev, VectorNd v, double t) {

      NumericListKnot next = prev.next;
      double h = (next.t-prev.t);
      double s = (t-prev.t)/(next.t-prev.t);

      double[] pbuf = prev.v.getBuffer();
      double[] nbuf = next.v.getBuffer();
      double[] vbuf = v.getBuffer();
      
      double c1 = 6*s*(s-1)/h;

      if (numRotationSubvecs() == 0) { 
         for (int i=0; i<myVsize; i++) {
            vbuf[i] = c1*(pbuf[i]-nbuf[i]);
         }
      }
      else {
         Quaternion q0 = new Quaternion();
         Quaternion q1 = new Quaternion();
         Quaternion qr = new Quaternion();
         Vector3d w0 = new Vector3d(); // leave at zero
         Vector3d w1 = new Vector3d(); // leave at zero
         Vector3d wr = new Vector3d();
         int i=0; // index into knot buffers
         int j=0; // index into vbuf, which will be <= i
         int k = 0;
         int off = myRotationSubvecOffsets[k];
         while (i<myVsize) {
            if (off == i) {
               q0.set (pbuf, off, myRotationRep, /*scale*/1);
               q1.set (nbuf, off, myRotationRep, /*scale*/1);
               //rvec.getQuaternion (q0, pbuf); 
               //rvec.getQuaternion (q1, nbuf);
               Quaternion.sphericalHermiteGlobal (
                  qr, wr, q0, w0, q1, w1, s, h);
               wr.get (vbuf, j);
               i += myRotationRep.size();
               j += 3;
               if (++k < numRotationSubvecs()) {
                  off = myRotationSubvecOffsets[k];
               }
               else {
                  off = -1;
               }
            }
            else {
               vbuf[j] = c1*(pbuf[i]-nbuf[i]);
               j++;
               i++;
            }
         }         
      }
   }
   
   private void interpCubic (
      NumericListKnot prev, VectorNd v, double t) {

      if (numRotationSubvecs() == 0) {
         interpCubicOld (prev, v, t);
         return;
      }
      
      // next is not null by assumption
      NumericListKnot next = prev.next;
      double s = (t-prev.t)/(next.t-prev.t);
      double h = (next.t-prev.t);
      NumericListKnot prevprev = prev.prev;
      NumericListKnot nextnext = next.next;
      
      double[] pbuf = prev.v.getBuffer();
      double[] ppbuf = null;
      double[] nbuf = next.v.getBuffer();
      double[] nnbuf = null;
      double[] vbuf = v.getBuffer();
      // rbuf gives reference values for resolving rotation representation
      double[] rbuf = (s < 0.5 ? pbuf : nbuf);

      double hp;
      double hn;
      if (prevprev != null) {
         ppbuf = prevprev.v.getBuffer();
         hp = next.t-prevprev.t;
      }
      else {
         ppbuf = pbuf;
         hp = h;
      }
      if (nextnext != null) {
         nnbuf = nextnext.v.getBuffer();
         hn = nextnext.t-prev.t;
      }
      else {
         nnbuf = nbuf;
         hn = h;
      }

      double b1 = (2*s-3)*s*s;
      double b2 = ((s-2)*s+1)*s*h;
      double b3 = (s-1)*s*s*h;
      
      if (numRotationSubvecs() == 0) {
         for (int i=0; i<myVsize; i++) {
            double vp = (nbuf[i]-ppbuf[i])/hp;
            double vn = (nnbuf[i]-pbuf[i])/hn;
            vbuf[i] = b1*(pbuf[i]-nbuf[i]) + b2*vp + b3*vn + pbuf[i];
         }
      }
      else {
         Quaternion q0 = new Quaternion();
         Quaternion q1 = new Quaternion();
         Quaternion qr = new Quaternion();
         Vector3d w0 = new Vector3d();
         Vector3d w1 = new Vector3d();
         int i=0;
         int k = 0;
         int off = myRotationSubvecOffsets[k];
         while (i<myVsize) {
            if (off == i) {
               q0.set (pbuf, off, myRotationRep, /*scale*/1);
               //rvec.getQuaternion (q0, pbuf); 
               q1.set (nbuf, off, myRotationRep, /*scale*/1);
               //rvec.getQuaternion (q1, nbuf);
               qr.set (ppbuf, off, myRotationRep, /*scale*/1);
               //rvec.getQuaternion (qr, ppbuf);
               getAngularVelocity (w0, qr, q0, q1, hp);
               qr.set (nnbuf, off, myRotationRep, /*scale*/1);
               //rvec.getQuaternion (qr, nnbuf);
               getAngularVelocity (w1, q0, q1, qr, hn);

               Quaternion.sphericalHermiteGlobal (
                  qr, null, q0, w0, q1, w1, s, h);
               qr.get (vbuf, rbuf, off, myRotationRep, /*scale*/1);

               //rvec.setRotation (vbuf, rbuf, qr);
               i += myRotationRep.size();
               if (++k < numRotationSubvecs()) {
                  off = myRotationSubvecOffsets[k];
               }
               else {
                  off = -1;
               }
            }
            else {
               double vp = (nbuf[i]-ppbuf[i])/hp;
               double vn = (nnbuf[i]-pbuf[i])/hn;
               vbuf[i] = b1*(pbuf[i]-nbuf[i]) + b2*vp + b3*vn + pbuf[i];
               i++;
            }
         }         
      }
   }

   private void interpCubicDeriv (
      NumericListKnot prev, VectorNd v, double t) {
      // next is not null by assumption
      NumericListKnot next = prev.next;
      double s = (t-prev.t)/(next.t-prev.t);
      double h = (next.t-prev.t);
      NumericListKnot prevprev = prev.prev;
      NumericListKnot nextnext = next.next;
      
      double[] pbuf = prev.v.getBuffer();
      double[] ppbuf = null;
      double[] nbuf = next.v.getBuffer();
      double[] nnbuf = null;
      double[] vbuf = v.getBuffer();

      double hp;
      double hn;
      if (prevprev != null) {
         ppbuf = prevprev.v.getBuffer();
         hp = next.t-prevprev.t;
      }
      else {
         ppbuf = pbuf;
         hp = h;
      }
      if (nextnext != null) {
         nnbuf = nextnext.v.getBuffer();
         hn = nextnext.t-prev.t;
      }
      else {
         nnbuf = nbuf;
         hn = h;
      }

      double c1 = 6*s*(s-1)/h;
      double c2 = (3*s-4)*s + 1;
      double c3 = s*(3*s-2);
      
      if (numRotationSubvecs() == 0) {
         for (int i=0; i<v.size(); i++) {
            double vp = (nbuf[i]-ppbuf[i])/hp;
            double vn = (nnbuf[i]-pbuf[i])/hn;
            vbuf[i] = c1*(pbuf[i]-nbuf[i]) + c2*vp + c3*vn;
         }
      }
      else {
         Quaternion q0 = new Quaternion();
         Quaternion q1 = new Quaternion();
         Quaternion qr = new Quaternion();
         Vector3d w0 = new Vector3d();
         Vector3d w1 = new Vector3d();
         Vector3d wr = new Vector3d();
         int i=0; // index into knot buffers
         int j=0; // index into vbuf, which will be <= i
         int k = 0;
         int off = myRotationSubvecOffsets[k];
         while (i<myVsize) {
            if (off == i) {
               q0.set (pbuf, off, myRotationRep, /*scale*/1);
               //rvec.getQuaternion (q0, pbuf); 
               q1.set (nbuf, off, myRotationRep, /*scale*/1);
               //rvec.getQuaternion (q1, nbuf);
               qr.set (ppbuf, off, myRotationRep, /*scale*/1);
               //rvec.getQuaternion (qr, ppbuf);
               getAngularVelocity (w0, qr, q0, q1, hp);
               qr.set (nnbuf, off, myRotationRep, /*scale*/1);
               //rvec.getQuaternion (qr, nnbuf);
               getAngularVelocity (w1, q0, q1, qr, hn);

               Quaternion.sphericalHermiteGlobal (
                  qr, wr, q0, w0, q1, w1, s, h);
               wr.get (vbuf, j);
               i += myRotationRep.size();
               j += 3;
               if (++k < numRotationSubvecs()) {
                  off = myRotationSubvecOffsets[k];
               }
               else {
                  off = -1;
               }
            }
            else {
               double vp = (nbuf[i]-ppbuf[i])/hp;
               double vn = (nnbuf[i]-pbuf[i])/hn;
               vbuf[j] = c1*(pbuf[i]-nbuf[i]) + c2*vp + c3*vn;
               j++;
               i++;
            }
         }         
      }
   }

   private void interpCubicOld (
      NumericListKnot prev, VectorNd v, double t) {

      NumericListKnot next = prev.next;
      double s = (t-prev.t)/(next.t-prev.t);
      NumericListKnot prevprev = prev.prev;
      NumericListKnot nextnext = next.next;

      allocateTmps (v.size());
      VectorNd v0 = myTmp0;
      VectorNd v1 = myTmp1;

      if (prevprev != null) {
         v0.sub (next.v, prevprev.v);
         v0.scale (1/(next.t-prevprev.t));
      }
      else {
         v0.sub (next.v, prev.v);
         v0.scale (1/(next.t-prev.t));
      }
      if (nextnext != null) {
         v1.sub (nextnext.v, prev.v);
         v1.scale (1/(nextnext.t-prev.t));
      }
      else {
         v1.sub (next.v, prev.v);
         v1.scale (1/(next.t-prev.t));
      }
      VectorNd.hermiteInterpolate (v, prev.v, v0, next.v, v1, s, next.t-prev.t);
   }

   /**
    * Interpolates the value associated with a particular value of t, based on
    * the current contents of this list. The interpolation order is specified
    * by {@code order}. If t lies before the start or after the end of the
    * list, the interpolation value is set to either the first or last knot
    * value, or zero, depending on whether {@code extendData} is {@code true}
    * or {@code false}.
    *
    * <p>The method returns the nearest knot that is at or before {@code t}.
    * By using this returned value as the {@code last} argument in subsequent
    * method calls, one can traverse forward or backward (in time) along the
    * list with O(1) complexity.
    * 
    * @param v
    * stores the interpolation result
    * @param t
    * value to interpolate for
    * @param order
    * interpolation order
    * @param extendData
    * if {@code true}, data is extended beyond the start and end of the list
    * based on the first and last knot values
    * @param last
    * knot point used to start the search for the knot containing {@code t}
    * @return nearest knot at or before {@code t}
    */
   public synchronized NumericListKnot interpolate (
      VectorNd v, double t, Order order, boolean extendData,
      NumericListKnot last) {

      v.setSize (myVsize);
      if (myHead == null) {
         v.setZero();
         return null;
      }
      // try to find knots that bracket the t value

      NumericListKnot prev = findKnotAtOrBefore (t, last);
      NumericListKnot next = prev.next;

      if (t <= prev.t) { // before start of list
         if (extendData || prev.t == t) {
            v.set (prev.v);
         }
         else {
            v.setZero();
         }
         return prev;
      }
      else if (next == null) { // after end of list
         if (extendData || prev.t == t) {
            v.set (prev.v);
         }
         else {
            v.setZero();
         }
         return prev;
      }

      int size = v.size();
      boolean hasRotSubvecs = (numRotationSubvecs() > 0);

      // change interpolation if necessary to make it compatible with the data
      switch (order) {
         case SphericalLinear: {
            if (hasRotSubvecs || (size != 4 && size != 7 && size != 16)) {
               order = Order.Linear; 
            }
            break;
         }
         case Cubic: {
            if (prev.prev == null && next.next == null) {
               order = Order.Linear;
            }
            break;
         }
         case SphericalCubic: {
            if (prev.prev == null && next.next == null) {
               if (hasRotSubvecs || (size != 4 && size != 7 && size != 16)) {
                  order = Order.Linear; 
               }
               else {
                  order = Order.SphericalLinear;
               }
            }
            else {
               if (hasRotSubvecs || (size != 4 && size != 7 && size != 16)) {
                  order = Order.Cubic;
               }
            }          
            break;
         }
      }

      switch (order) {
         case Step: {
            v.set (prev.v);
            break;
         }
         case Linear: {
            interpLinear (prev, v, t);
            break;
         }
         case SphericalLinear: {
            interpLinearRotation (prev, v, t);
            if (size == 7 || size == 16) {
               interpLinearPosition (prev, v, t);
            }
            break;
         }
         case Parabolic: {
            interpParabolic (prev, v, t);
            break;
         }
         case Cubic: {
            interpCubic (prev, v, t);
            break;
         }
         case SphericalCubic: {
            interpCubicRotation (prev, v, t);
            if (size == 7 || size == 16) {
               interpCubicPosition (prev, v, t);
            }
            break;
         }
         case CubicStep: {
            interpCubicStep (prev, v, t);
            break;
         }
         default: {
            throw new InternalErrorException (
               "interpolation method " + order + " not implemented");
         }
      }
      return prev;
   }

   /**
    * Finds the derivative associated with a particular value of t, by
    * differentiating the function implied by the knot points and the
    * interpolation order specified by {@code order}. If t lies before the
    * start or after the end of the list, the derivative will be zero.
    * 
    * <p>The method returns the nearest knot that is at or before {@code t}.
    * By using this returned value as the {@code last} argument in subsequent
    * method calls, one can traverse forward or backward (in time) along the
    * list with O(1) complexity.
    *
    * @param v
    * stores the derivative result
    * @param t value to find the derivative at
    * @param order
    * interpolation order
    * @param last
    * knot point used to start the search for the knot containing {@code t}
    * @return nearest knot at or before {@code t}
    */
   public synchronized NumericListKnot interpolateDeriv (
      VectorNd v, double t, Order order, NumericListKnot last) {

      v.setSize (getDerivSize());
      if (myHead == null) {
         v.setZero();
         return null;
      }
      // try to find knots that bracket the t value

      NumericListKnot prev = findKnotAtOrBefore (t, last);
      NumericListKnot next = prev.next;

      if (prev.t > t) { // before the start of list
         v.setZero();
         return prev;
      }
      else if (next == null) { // at or beyond end of list
         if (prev.t == t && prev.prev != null) {
            next = prev;
            prev = prev.prev;
         }
         else {
            v.setZero();
            return prev;
         }
      }

      int size = getVectorSize();
      boolean hasRotSubvecs = (numRotationSubvecs() > 0);

      // change interpolation if necessary to make it compatible with the data
      switch (order) {
         case SphericalLinear: {
            if (hasRotSubvecs || (size != 4 && size != 7 && size != 16)) {
               order = Order.Linear; 
            }
            break;
         }
         case Cubic: {
            if (prev.prev == null && next.next == null) {
               order = Order.Linear;
            }
            break;
         }
         case SphericalCubic: {
            if (prev.prev == null && next.next == null) {
               if (hasRotSubvecs || (size != 4 && size != 7 && size != 16)) {
                  order = Order.Linear; 
               }
               else {
                  order = Order.SphericalLinear;
               }
            }
            else {
               if (hasRotSubvecs || (size != 4 && size != 7 && size != 16)) {
                  order = Order.Cubic;
               }
            }          
            break;
         }
      }

      switch (order) {
         case Step: {
            v.setZero();
            break;
         }
         case Linear: {
            interpLinearDeriv (prev, v, t);
            break;
         }
         case SphericalLinear: {
            interpLinearRotationDeriv (prev, v, t);
            if (size == 7 || size == 16) {
               interpLinearPositionDeriv (prev, v, t);
            }
            break;
         }
         case Parabolic: {
            throw new UnsupportedOperationException (
               "Parabolic interpolation not supported for interpDeriv()");
         }
         case Cubic: {
            interpCubicDeriv (prev, v, t);
            break;
         }
         case SphericalCubic: {
            interpCubicRotationDeriv (prev, v, t);
            if (size == 7 || size == 16) {
               interpCubicPositionDeriv (prev, v, t);
            }
            break;
         }
         case CubicStep: {
            interpCubicStepDeriv (prev, v, t);
            break;
         }
         default: {
            throw new InternalErrorException (
               "interpolation method " + order + " not implemented");
         }
      }
      return prev;
   }

   /**
    * Finds the derivative associated with a particular value of t, based on
    * numerical differentation of the current knot points. This is done by
    * linearly interpolating numerical derivatives computed at the mid-points
    * between knots. If t lies before the start or after the end of the list,
    * the derivative will be zero.
    * 
    * <p>The method returns the nearest knot that is at or before {@code t}.
    * By using this returned value as the {@code last} argument in subsequent
    * method calls, one can traverse forward or backward (in time) along the
    * list with O(1) complexity.
    * 
    * @param v
    * stores the derivative result
    * @param t
    * value to find the derivative for
    * @param last
    * knot point used to start the search for the knot containing {@code t}
    * @return nearest knot at or before {@code t}
    */
   public synchronized NumericListKnot numericalDeriv (
      VectorNd v, double t, NumericListKnot last) {

      v.setSize (getDerivSize());
      if (myHead == null) {
         v.setZero();
         return null;
      }
      // try to find knots that bracket the t value

      NumericListKnot prev = findKnotAtOrBefore (t, last);
      NumericListKnot next = prev.next;

      if (prev.t > t) { // before the start of list
         v.setZero();
         return prev;
      }
      else if (next == null) { // at or beyond end of list
         if (prev.t == t && prev.prev != null) {
            next = prev;
            prev = prev.prev;
         }
         else {
            v.setZero();
            return prev;
         }
      }

      int size = getVectorSize();
      double h = next.t-prev.t;
      
      NumericListKnot knot0 = prev;
      NumericListKnot knot1 = next;      
      NumericListKnot knot2 = null;
      if (t - prev.t > h/2) {
         // look forward
         if (next.next != null) {
            knot2 = next.next;
         }
      }
      else {
         // look backward
         if (prev.prev != null) {
            knot0 = prev.prev;
            knot1 = prev;
            knot2 = next;
         }
      }

      // see if this is a SphericalLinear case involving axisAngles or rigid
      // transforms
      Order order = myInterpolation.getOrder();
      if (numRotationSubvecs() == 0 &&
          (order == Order.SphericalLinear || order == Order.SphericalCubic) &&
          (size == 4 || size == 7 || size == 16)) {
         if (knot2 != null) {
            numericalRotationDeriv (v, t, knot0, knot1, knot2);
            if (size == 7 || size == 16) {
               numericalPositionDeriv (v, t, knot0, knot1, knot2);
            }
         }
         else {
            interpLinearRotationDeriv (prev, v, t);
            if (size == 7 || size == 16) {
               interpLinearPositionDeriv (prev, v, t);
            }
         }
      }
      else {
         if (knot2 != null) {
            numericalDeriv (v, t, knot0, knot1, knot2);
         }
         else {
            interpLinearDeriv (prev, v, t);
         }
      }
      return prev;
   }

   private class MyIterator implements Iterator<NumericListKnot> {
      NumericListKnot next;

      MyIterator (NumericListKnot head) {
         next = head;
      }

      public boolean hasNext() {
         return next != null;
      }

      public NumericListKnot next() throws NoSuchElementException {
         if (next == null) {
            throw new NoSuchElementException();
         }
         else {
            NumericListKnot knot = next;
            next = next.next;
            return knot;
         }
      }

      public void remove()
         throws UnsupportedOperationException, IllegalStateException {
         throw new UnsupportedOperationException();
      }
   }

   /**
    * Returns the first knot in this list.
    * 
    * @return first knot in this list
    */
   public NumericListKnot getFirst() {
      NumericListKnot knot = myHead;
      // myLast = knot;
      return knot;
   }

   /**
    * Returns the last knot in this list.
    * 
    * @return last knot in this list
    */
   public NumericListKnot getLast() {
      NumericListKnot knot = myTail;
      // myLast = knot;
      return knot;
   }

   /**
    * Returns the idx-th knot in this list, or {@code null} if so such
    * knot exists;
    * 
    * @return idx-th knot
    */
   public NumericListKnot getKnot(int idx) {
      NumericListKnot knot = myHead;
      for (int i=0; i<idx && knot != null; i++) {
         knot = knot.next;
      }
      return knot;
   }

   /**
    * Returns the number of knots in this list.
    * 
    * @return number of knots
    */
   public int getNumKnots() {
      int cnt = 0;
      for (NumericListKnot knot = myHead; knot != null; knot = knot.next) {
         cnt++;
      }
      return cnt;
   }

   /**
    * Returns true if this list is empty.
    * 
    * @return true if this list is empty
    */
   public boolean isEmpty() {
      return myHead == null;
   }

   /**
    * Removes all knots in the numeric list.
    */
   public synchronized void clear() {
      // invalidate all the knots on this list
      for (NumericListKnot knot = myHead; knot != null; knot = knot.next) {
         knot.myList = null;
      }
      myTail = myHead = myLast = null;
      myMinMaxValid = false;
   }
   
   /**
    * Uniformly scales all the values in this list
    * 
    * @param s scale factor
    */
   public void scale (double s) {
      for (NumericListKnot knot = myHead; knot != null; knot = knot.next) {
         knot.v.scale (s);
      }      
      myMinMaxValid = false;
   }

   /**
    * Smooths the values in this list by applying a mean average filter over a
    * moving window of specified size. This window is centered on each knot,
    * and is reduced in size near the end knots to ensure a symmetric fit. The
    * end knot values are not changed.
    * 
    * @param winSize size of the averaging window. The value should be odd; if
    * it is even, it will be incremented internally to be odd.  The method does
    * nothing if the value is is less than 1. Finally, {@code winSize} will be
    * reduced if necessary to fit the number of knots.
    */
   public void applyMovingAverageSmoothing (int winSize) {
      if (winSize <= 0) {
         return;
      }
      if (winSize%2 == 0) {
         winSize++;
      }
      int numk = getNumKnots();
      if (winSize > numk) {
         winSize = (numk%2 == 0 ? numk-1 : numk);
      }
      if (winSize >= 3) {
         // store computed values in 'averages' until we have passed over the
         // averaging window:
         ArrayDeque<VectorNd> averages = new ArrayDeque<>();
         NumericListKnot tail = myHead; // trailing knot whose value will be set
         int k = 0;
         for (NumericListKnot knot = myHead; knot != null; knot = knot.next) {
            VectorNd avg = new VectorNd(getVectorSize());
            int hsize = winSize/2; // half window size
            // reduce the window size near the ends
            hsize = Math.min(k, hsize);
            hsize = Math.min(numk-1-k, hsize);
            NumericListKnot wknot = knot;
            for (int i=0; i<hsize; i++) {
               wknot = wknot.prev;
            }
            for (int i=0; i<(hsize*2+1); i++) {
               avg.add (wknot.v);
               wknot = wknot.next;
            }
            avg.scale (1/(double)(2*hsize+1));
            averages.add (avg);
            if (averages.size() > winSize/2) {
               tail.v.set (averages.remove());
               tail = tail.next;
            }
            k++;
         }       
         while (averages.size() > 0) {
            tail.v.set (averages.remove());
            tail = tail.next;
         }
         myMinMaxValid = false;
      }
   }

   private VectorNd convolve (NumericListKnot knot, VectorNd wgts, int winSize) {
      VectorNd avg = new VectorNd(getVectorSize());
      for (int i=0; i<winSize; i++) {
         avg.scaledAdd (wgts.get(i), knot.v);
         knot = knot.next;
      }
      return avg;
   }


    /**
    * Smooths the values in this list by applying Savitzky-Golay smoothing over
    * a moving window of specified size. Savitzky-Golay smoothing works by
    * fitting the data values in the window to a polynomial of degree {@code
    * deg}, and then using this to recompute the value in the middle of the
    * window. The polynomial is also used to interpolate the first and last
    * {@code winSize/2} values, since it is not possible to center the window
    * on these.
    * 
    * @param deg degree of the smoothing polynomial. Must be at least 1.
    * @param winSize size of the averaging window. The value must be {@code >=
    * deg+1} and should also be odd; if it is even, it will be incremented
    * internally to be odd. Finally, {@code winSize} will be reduced if
    * necessary to fit the number of knots.
    */
   public void applySavitzkyGolaySmoothing (int winSize, int deg) {
      if (deg < 1) {
         throw new IllegalArgumentException (
            "deg=" + deg + "; must be >= 1");
      }
      if (winSize < deg+1) {
         throw new IllegalArgumentException (
            "winSize=" + winSize + "; must be >= deg+1");
      }
      int minWinSize = (deg%2 == 0 ? deg+1 : deg+2);
      if (winSize%2 == 0) {
         winSize++;
      }
      int numk = getNumKnots();
      if (winSize > numk) {
         winSize = (numk%2 == 0 ? numk-1 : numk);
      }
      if (winSize < minWinSize) {
         return;
      }
      PolynomialFit fit = new PolynomialFit (deg, winSize, 0.0, 1.0);
      VectorNd[] wgts = new VectorNd[winSize];
      // create a weight set for every window point, to allow us to interpolate
      // values at the ends
      for (int i=0; i<winSize; i++) {
         wgts[i] = fit.getSavitzkyGolayWeights (i/(double)(winSize-1));
      }
      // store computed values in 'averages' until we have passed over the
      // averaging window:
      ArrayDeque<VectorNd> averages = new ArrayDeque<>();
      NumericListKnot tail = myHead; // trailing knot whose value will be set
      NumericListKnot knot = myHead;
      for (int k=0; k<numk-winSize+1; k++) {
         if (k == 0) {
            for (int j=0; j<winSize/2; j++) {
               averages.add (convolve (knot, wgts[j], winSize));
            }
         }
         averages.add (convolve (knot, wgts[winSize/2], winSize));
         if (k == numk-winSize) {
            for (int j=winSize/2+1; j<winSize; j++) {
               averages.add (convolve (knot, wgts[j], winSize));
            }
         }
         if (averages.size() > winSize/2) {
            tail.v.set (averages.remove());
            tail = tail.next;
         }
         knot = knot.next;
      }       
      while (averages.size() > 0) {
         tail.v.set (averages.remove());
         tail = tail.next;
      }
      myMinMaxValid = false;
   }

   /**
    * Returns an iterator over all the knots in this numeric list.
    * 
    * @return iterator over ths knots
    */
   public Iterator<NumericListKnot> iterator() {
      return new MyIterator (myHead);
   }

   /** 
    * Returns the values of this numeric list as a two dimensional array of
    * doubles. This facilitates reading the values into a matlab array.  The
    * array is arranged so that each knot point corresponds to a row, the first
    * column gives the time values, and the remaining columns give the knot
    * point values.
    * 
    * @return Values of this numeric list
    */   
   public double[][] getValues () {
      double[][] vals = new double[getNumKnots()][1+myVsize];
      int i=0;
      for (NumericListKnot knot=myHead; knot!=null; knot=knot.next) {
         vals[i][0] = knot.t;
         for (int j=0; j<myVsize; j++) {
            vals[i][j+1] = knot.v.get (j);
         }
         i++;
      }
      return vals;
   }

   /** 
    * Sets the values of this numeric list from a two dimensional array of
    * doubles. This facilitates settings the values from a matlab array.  The
    * arrangement of the array is described in {@link #getValues}.
    * 
    * @param vals Values used to set this numeric list
    */   
   public void setValues (double[][] vals) {
      for (int i=0; i<vals.length; i++) {
         if (vals[i].length != myVsize+1) {
            throw new IllegalArgumentException (
               "Number of columns must equal knot vector size plus one");
         }
      }
      clear();
      NumericListKnot last = null;
      for (int i=0; i<vals.length; i++) {
         NumericListKnot knot = new NumericListKnot (myVsize);
         knot.t = vals[i][0];
         for (int j=0; j<myVsize; j++) {
            knot.v.set (j, vals[i][j+1]);
         }
         add (knot, last);
         last = knot;
      }
   }

   /** 
    * Sets the values of this numeric list from those of another numeric list.
    * The vector size of the source list must be greater than or equal to that
    * of this list; extra values are ignored.
    *
    * <p>The time values {@code t} in this list are set from the time values
    * {@code ts} in the source list according to
    * <pre>
    * t = tscale ts + toffset
    * </pre>
    * 
    * @param src numeric list to copy values from
    * @param tscale scale factor for source time values
    * @param toffset offset for source time values
    */   
   public void setValues (NumericList src, double tscale, double toffset) {
      if (src.getVectorSize() < getVectorSize()) {
         throw new IllegalArgumentException (
            "source list vector size " + src.getVectorSize() +
            " less then destination size " + getVectorSize());
      }
      clear();
      NumericListKnot last = null;
      for (NumericListKnot knot=src.myHead; knot!=null; knot=knot.getNext()) {
         NumericListKnot newKnot = new NumericListKnot (myVsize);
         newKnot.t = tscale*knot.t + toffset;
         for (int j=0; j<myVsize; j++) {
            newKnot.v.set (j, knot.v.get(j));
         }
         add (newKnot, last);
         last = newKnot;
      }
   }

   /**
    * Returns true if the contents of this numeric list equal the contents of
    * another numeric list.
    * 
    * @param list
    * numeric list to compare with
    * @return true if the lists are equal
    */
   public synchronized boolean equals (NumericList list) {
      if (myVsize != list.myVsize || getNumKnots() != list.getNumKnots()) {
         return false;
      }
      NumericListKnot knot0 = myHead;
      NumericListKnot knot1 = list.myHead;
      while (knot0 != null) {
         if (knot0.t != knot1.t || !knot0.v.equals (knot1.v)) {
            return false;
         }
         knot0 = knot0.next;
         knot1 = knot1.next;
      }
      return true;
   }

   /**
    * Returns a string representation of this numeric list.
    * 
    * @return string representation of this list
    */
   public String toString() {
      return toString ("%g");
   }

   /**
    * Returns a String representation of this numeric list, in which each
    * element is formatted using a C <code>printf</code> style format string.
    * For a description of the format string syntax, see {@link
    * maspack.util.NumberFormat NumberFormat}.
    * 
    * @param fmtStr
    * numeric format specification
    * @return String representation of this list
    */
   public String toString (String fmtStr) {
      return toString (new NumberFormat (fmtStr));
   }

   /**
    * Returns a String representation of this numeric list, in which each
    * element is formatted using a C <code>printf</code> style as decribed by
    * the parameter <code>NumberFormat</code>. When called numerous times,
    * this routine can be more efficient than {@link #toString(String)
    * toString(String)}, because the {@link maspack.util.NumberFormat
    * NumberFormat} does not need to be recreated each time from a specification
    * string.
    * 
    * @param fmt
    * numeric format
    * @return String representation of this list
    */
   public String toString (NumberFormat fmt) {
      StringBuffer sbuf = new StringBuffer (20 * getNumKnots());
      NumericListKnot knot = myHead;
      while (knot != null) {
         sbuf.append (fmt.format (knot.t));
         sbuf.append (' ');
         sbuf.append (knot.v.toString (fmt));
         sbuf.append ('\n');
         knot = knot.next;
      }
      return sbuf.toString();
   }

   public String toStringSuper() {
      return super.toString();
   }

   /**
    * Returns a deep copy of this numeric list.
    */
   public NumericList clone() {
      NumericList l = null;
      try {
         l = (NumericList)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("NumericList not clonable");
      }
      l.myRotationSubvecOffsets =
         Arrays.copyOf (myRotationSubvecOffsets, myRotationSubvecOffsets.length);
      l.myLast = l.myHead = l.myTail = null;
      l.myMinMaxValid = false;
      l.a1 = new VectorNd (0);
      l.a2 = new VectorNd (0);
      l.a3 = new VectorNd (0);
      l.myInterpolation = new Interpolation (myInterpolation);
      NumericListKnot last = null;
      for (NumericListKnot knot = myHead; knot != null; knot = knot.next) {
         NumericListKnot newKnot = new NumericListKnot (knot);
         l.add (newKnot, last);
         last = newKnot;
         if (knot == myLast) {
            l.myLast = newKnot;
         }
      }
      return l;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isWritable() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
   throws IOException {
      IndentingPrintWriter.printOpening (pw, "[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      pw.println ("vsize=" + myVsize);
      if (myRotationRep != null) {
         pw.println ("rotationRep=" + myRotationRep);
         pw.print ("rotationSubvecOffsets=");
         Write.writeInts (pw, getRotationSubvecOffsets(), null);
      }
      pw.println ("interpolation=" + myInterpolation);
      pw.println ("knots=[");
      IndentingPrintWriter.addIndentation (pw, 2);      
      for (NumericListKnot knot=myHead; knot!=null; knot=knot.next) {
         pw.print (fmt.format(knot.t) + " ");
         pw.println (knot.v.toString (fmt));
      }
      IndentingPrintWriter.addIndentation (pw, -2);      
      pw.println ("]");
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   protected boolean scanItem (ReaderTokenizer rtok)
      throws IOException {
      // if keyword is a property name, try scanning that
      if (rtok.nextToken() == ReaderTokenizer.TT_WORD) {
         String fieldName = rtok.sval;
         if (fieldName.equals ("vsize")) {
            rtok.scanToken ('=');
            myVsize = rtok.scanInteger();
            return true;
         }
         else if (fieldName.equals ("rotationRep")) {
            rtok.scanToken ('=');
            myRotationRep = rtok.scanEnum(RotationRep.class);
            return true;
         }
         else if (fieldName.equals ("rotationSubvecOffsets")) {
            rtok.scanToken ('=');
            int[] offs = Scan.scanInts (rtok);
            try {
               setRotationSubvecOffsets (offs);
            }
            catch (Exception e) {
               throw new IOException (e);
            }
            return true;
         }
         else if (fieldName.equals ("interpolation")) {
            rtok.scanToken ('=');
            Interpolation interp = new Interpolation();
            interp.scan (rtok, null);
            setInterpolation (interp);
            return true;
         }
         else if (fieldName.equals ("knots")) {
            rtok.scanToken ('=');
            rtok.scanToken ('[');
            while (rtok.nextToken() != ']') {
               NumericListKnot knot = new NumericListKnot (myVsize);
               knot.t = rtok.scanNumber();
               for (int i=0; i<myVsize; i++) {
                  knot.v.set (i, rtok.scanNumber());
               }
               add (knot);
            }
            return true;
         }
      }
      
      rtok.pushBack();
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      clear();
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         if (!scanItem (rtok)) {
            throw new IOException ("Unexpected token: " + rtok);
         }
      }
   }

   /**
    * Checks to see if this list has a position structure, such that the
    * subvectors between any rotation subvectors can contain 3D point data (and
    * hence have a size that is a multiple of 3).
    */
   public boolean hasPositionStructure() {
      int vsize = getVectorSize();
      int numRots = numRotationSubvecs();
      int off = 0; // current vector offset
      int[] rotSubvecOffs = getRotationSubvecOffsets();
      for (int kr=0; kr<=numRots; kr++) {
         // roff is the offset of the next rotation subvector, or vector size
         int roff = (kr < numRots ? rotSubvecOffs[kr] : vsize);
         if ((roff-off)%3 != 0) {
            return false;
         }
         if (kr < numRots) {
            off = roff + myRotationRep.size();
         }
      }
      return true;
   }

   /**
    * If the list data has a position structure (i.e., contains rotation and/or
    * 3D point data), then apply the specified transform to this data.
    */
   public void transformPositionData (AffineTransform3dBase X) {
      GeometryTransformer gtrans = GeometryTransformer.create(X);
      NumericListKnot knot;
      RotationRep rotRep = getRotationRep();
      Quaternion q = new Quaternion();
      RotationMatrix3d R = new RotationMatrix3d();
      Point3d p = new Point3d();
      int[] rotSubvecOffs = getRotationSubvecOffsets();
      int numRots = numRotationSubvecs();
      int vsize = getVectorSize();
      
      if (!hasPositionStructure()) {
          throw new IllegalStateException (
             "List data does not have a position structure");
      }

      double[] pbuf = null; // buffer for previous knot vector

      // apply the transform to each knot
      for (knot = getFirst(); knot != null; knot = knot.getNext()) {
         double[] vbuf = knot.v.getBuffer(); // access vector buffer directly
         int off = 0; // current vector offset
         for (int kr=0; kr<=numRots; kr++) {
            // roff is the offset of the next rotation subvector, or vector size
            int roff = (kr < numRots ? rotSubvecOffs[kr] : vsize);
            // transform any point data between off and roff:
            while (off < roff-2) {
               p.set (vbuf, off);
               gtrans.computeTransformPnt (p, p);
               p.get (vbuf, off);
               off += 3;
            }
            if (kr < numRots) {
               // transform rotation subvector pointed to by off
               // load subvector into quaternion
               q.set (vbuf, roff, rotRep, /*scale*/1);
               // turn into rotation matrix, transform, and reset quaternion
               R.set (q);
               gtrans.computeTransform (R, /*Ndiag*/null, R, /*ref*/null);
               q.set (R);
               // reload subvector with transformed rotatation
               q.get (vbuf, pbuf, roff, rotRep, /*scale*/1);
               off += myRotationRep.size();
            }
         }           
         pbuf = vbuf;
      }
   }

   /**
    * If the list data contains a collection of 3D vectors (i.e., the list
    * vector size is multiple of 3), then apply the specified transform to this
    * data.
    */
   public void transformVectorData (AffineTransform3dBase X) {
      GeometryTransformer gtrans = GeometryTransformer.create(X);
      NumericListKnot knot;
      Vector3d v = new Vector3d();
      int vsize = getVectorSize();
      
      if ((getVectorSize()%3) != 0) {
          throw new IllegalStateException (
             "Numeric list does not contain 3-vector data");
      }

      // apply the transform to each knot
      for (knot = getFirst(); knot != null; knot = knot.getNext()) {
         double[] vbuf = knot.v.getBuffer(); // access vector buffer directly
         int off = 0; // current vector offset
         for (off=0; off<vsize; off += 3) {
            v.set (vbuf, off);
            gtrans.computeTransformVec (v, v, /*ref*/null);
            v.get (vbuf, off);
         }
      }
   }

}
