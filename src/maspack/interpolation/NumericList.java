/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.interpolation;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.*;

import maspack.interpolation.Interpolation.Order;
import maspack.matrix.*;
import maspack.util.*;

/**
 * A linked list of vector values arranged in order with respect to a parameter
 * t. Can be interpolated to produce a continuous vector function.
 */
public class NumericList
   implements Cloneable, Iterable<NumericListKnot>, Scannable {
                                    
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
   private double myMaxValue;
   private double myMinValue;

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
    * Returns the size of the vectors associated with this list.
    */
   public int getVectorSize() {
      return myVsize;
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
    * Creates a knot with the specified values and time and adds it into this
    * numeric list. The knot will be added at the proper location so that all t
    * values are monotonically increasing. Any existing knot which has the same
    * t value as the new knot will be removed and returned.
    * 
    * @param vals
    * values for the knot
    * @param t
    * time at which the knot should be added
    * @return existing knot with the same t value, if any
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
      updateMinMaxValues (knot);
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

   public void getMinMaxValues (double[] minMax) {
      if (!myMinMaxValid) {
         updateMinMaxValues();
      }
      minMax[0] = myMinValue;
      minMax[1] = myMaxValue;
   }

   private void updateMinMaxValues (NumericListKnot knot) {
      double[] vbuf = knot.v.getBuffer();
      for (int i = 0; i < myVsize; i++) {
         double x = vbuf[i];
         if (x > myMaxValue) {
            myMaxValue = x;
         }
         if (x < myMinValue) {
            myMinValue = x;
         }
      }
   }

   private void updateMinMaxValues() {
      myMinValue = Double.POSITIVE_INFINITY;
      myMaxValue = Double.NEGATIVE_INFINITY;
      for (NumericListKnot knot = myHead; knot != null; knot = knot.next) {
         updateMinMaxValues (knot);
      }
      myMinMaxValid = true;
   }

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
            if (x >= myMaxValue) // using >= for paranoid reasons
            {
               minMaxUpdateNeeded = true;
               break;
            }
            if (x <= myMinValue) {
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
    * the current contents of this list. If t lies before the start or after the
    * end of the list, the interpolation value is set to the first or last knot
    * value.
    * 
    * @param v
    * stores the interpolation result
    * @param t
    * value to interpolate for
    * @throws ImproperStateException
    * if the list is empty
    */
   public void interpolate (VectorNd v, double t) {
      myLast = interpolate (v, t, myInterpolation, myLast);
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

   private void getAngularVelocity (
      Vector3d w, Quaternion q0, Quaternion q1, double h) {
      Quaternion qd = new Quaternion();
      qd.mulInverseLeft (q0, q1);
      qd.log (w);
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
         getAngularVelocity (w0, qr, q1, next.t-prevprev.t);
      }
      else {
         getAngularVelocity (w0, q0, q1, next.t-prev.t);
      }
      if (nextnext != null) {
         getRotation (qr, nextnext.v);
         getAngularVelocity (w1, q0, qr, nextnext.t-prev.t);
      }
      else {
         getAngularVelocity (w1, q0, q1, next.t-prev.t);
      }
      Quaternion.sphericalHermiteGlobal (
         qr, null, q0, w0, q1, w1, s, next.t-prev.t);
      setRotation (v, qr);
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

   private void interpCubicPose (
      NumericListKnot prev, VectorNd v, double t) {

      NumericListKnot next = prev.next;
      Quaternion q0 = new Quaternion();
      Quaternion q1 = new Quaternion();

   }

   private void interpCubicStep (
      NumericListKnot prev, VectorNd v, double t) {

      NumericListKnot next = prev.next;
      double s = (t-prev.t)/(next.t-prev.t);
      
      allocateTmps (v.size());
      VectorNd v0 = myTmp0;
      VectorNd v1 = myTmp1;
      v0.setZero ();
      v1.setZero ();

      VectorNd.hermiteInterpolate (v, prev.v, v0, next.v, v1, s, next.t-prev.t);
   }
   
   private void interpCubic (
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

   private void interpCubicOld (NumericListKnot prev, VectorNd v, double t) {

      int VectorSize = prev.v.size();
      boolean zeroDerivative = false;

      NumericListKnot prevprev = prev.prev;
      NumericListKnot next = prev.next;

      NumericListKnot nextnext = next.next;

      if (prevprev == null) {
         zeroDerivative = true;
         prevprev = new NumericListKnot (VectorSize);
         prevprev.v.setZero();
         prevprev.t = 0;
      }
      if (nextnext == null) {
         nextnext = new NumericListKnot (VectorSize);
         nextnext.v.setZero();
         nextnext.t = Math.max (0, (2 * next.t) - prev.t);
      }
      double times[] = new double[4];
      times[0] = prevprev.t;
      times[1] = prev.t;
      times[2] = next.t;
      times[3] = nextnext.t;

      v.setSize (VectorSize);
      double holder;
      double vValues[] = new double[4];
      for (int i = 0; i < VectorSize; i++) {
         vValues[0] = prevprev.v.get (i);
         vValues[1] = prev.v.get (i);
         vValues[2] = next.v.get (i);
         vValues[3] = nextnext.v.get (i);

         if (zeroDerivative)
            holder = CubicSpline.interpolate (0, vValues, times, t);
         else
            holder = CubicSpline.interpolate (vValues, times, t);

         if (Double.isNaN (holder))
            holder = 0;
         v.set (i, holder);
      }
   }

   
   public synchronized NumericListKnot interpolate (
      VectorNd v, double t, Order order, boolean extendData,
      NumericListKnot last) {
      if (myHead == null) {
         v.setZero();
         return null;
      }
      // try to find knots that bracket the t value

      NumericListKnot prev = findKnotAtOrBefore (t, last);
      NumericListKnot next = prev.next;

      if (prev.t > t) // before the start of list
      {
         if (extendData) {
            v.set (prev.v);
         }
         else {
            v.setZero();
         }
         return prev;
      }
      else if (next == null) // after end of list
      {
         if (extendData || prev.t == t) {
            v.set (prev.v);
         }
         else {
            v.setZero();
         }
         return prev;
      }

      int size = v.size();

      // change interpolation if necessary to make it compatible with the data
      switch (order) {
         case SphericalLinear: {
            if (size != 4 && size != 7 && size != 16) {
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
               if (size != 4 && size != 7 && size != 16) {
                  order = Order.Linear; 
               }
               else {
                  order = Order.SphericalLinear;
               }
            }
            else {
               if (size != 4 && size != 7 && size != 16) {
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
            v.interpolate (prev.v, (t - prev.t) / (next.t - prev.t), next.v);
            int VectorSize = prev.v.size();
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
   public Object clone() {
      NumericList l = null;
      try {
         l = (NumericList)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("NumericList not clonable");
      }
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
}
