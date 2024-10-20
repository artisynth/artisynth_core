/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.interpolation;

import java.util.*;

import maspack.interpolation.Interpolation.Order;
import maspack.matrix.*;
import maspack.util.*;
import maspack.util.UnitTest;

class NumericListTest extends UnitTest {
   private int myVsize = 2;
   NumericList list = new NumericList (myVsize);

   private static final double RTOD = 180.0/Math.PI;
   private static final double DTOR = Math.PI/180.0;

   static final double EPS = 1e-16;

   NumericListKnot newKnot (double t, double v0, double v1) {
      NumericListKnot knot = new NumericListKnot (myVsize);
      knot.t = t;
      knot.v.set (0, v0);
      knot.v.set (1, v1);
      return knot;
   }

   void addRandomKnot (NumericList list, double t) {
      NumericListKnot knot = new NumericListKnot (list.getVectorSize());
      knot.t = t;
      knot.v.setRandom ();
      list.add (knot);
   }

   NumericList createRigidTransformList (double... times) {
      NumericList list = new NumericList(16);
      RigidTransform3d T = new RigidTransform3d();
      for (double t : times) {
         T.setRandom();
         NumericListKnot knot = new NumericListKnot(16);
         knot.t = t;
         T.get (knot.v.getBuffer());
         list.add (knot);         
      }
      return list;
   }

   NumericList createAxisAngleList (double... times) {
      NumericList list = new NumericList(4);
      RigidTransform3d T = new RigidTransform3d();
      AxisAngle axisAng = new AxisAngle();
      for (double t : times) {
         T.setRandom();
         T.R.getAxisAngle (axisAng);
         NumericListKnot knot = new NumericListKnot(4);
         knot.t = t;
         knot.v.setSubVector (0, axisAng.axis);
         knot.v.set (3, RTOD*axisAng.angle);
         list.add (knot);         
      }
      return list;
   }

   NumericList createPosAxisAngleList (double... times) {
      NumericList list = new NumericList(7);
      RigidTransform3d T = new RigidTransform3d();
      AxisAngle axisAng = new AxisAngle();
      for (double t : times) {
         T.setRandom();
         T.R.getAxisAngle (axisAng);
         NumericListKnot knot = new NumericListKnot(7);
         knot.t = t;
         knot.v.setSubVector (0, T.p);
         knot.v.setSubVector (3, axisAng.axis);
         knot.v.set (6, RTOD*axisAng.angle);
         list.add (knot);         
      }
      return list;
   }

   NumericList createRotationSubvecList (
      String spec, RotationRep rotRep, double... times) {

      int numRot = 0;
      int numPos = 0;
      int j = 0;
      DynamicIntArray offsets = new DynamicIntArray();
      for (int i=0; i<spec.length(); i++) {
         if (spec.charAt(i) == 'r') {
            offsets.add (j);
            j += rotRep.size();
            numRot++;
         }
         else {
            numPos++;
            j += 3;
         }
      }
      Quaternion[] quats = allocQuats(numRot);
      VectorNd vtrans = new VectorNd (numPos*3);

      int vsize;
      NumericList list;
      if (offsets.size() > 0) {
         vsize = numPos*3 + numRot*rotRep.size();
         list = new NumericList(vsize, rotRep, offsets.getArray());
      }
      else {
         vsize = numPos*3;
         list = new NumericList(vsize);
      }

      for (double t : times) {
         vtrans.setRandom();
         for (int k=0; k<numRot; k++) {
            quats[k].setRandom();
            quats[k].normalize();
         }
         NumericListKnot knot = new NumericListKnot(vsize);
         knot.t = t;
         packRotationAndTransTerms (knot.v, null, vtrans, quats, list);
         list.add (knot);         
      }
      return list;
   }

   NumericList makeTestList (double[] vals) {
      int k = 0;
      NumericList newList = new NumericList (myVsize);
      while (k < vals.length - myVsize) {
         NumericListKnot knot = new NumericListKnot (myVsize);
         knot.t = vals[k++];
         for (int i = 0; i < myVsize; i++) {
            knot.v.set (i, vals[k++]);
         }
         knot.next = null;
         if (newList.myTail == null) {
            knot.prev = null;
            newList.myHead = knot;
         }
         else {
            knot.prev = newList.myTail;
            newList.myTail.next = knot;
         }
         newList.myTail = knot;
      }
      return newList;
   }

   void checkContents (NumericList list, double[] vals) {
      NumericList check = makeTestList (vals);
      if (!list.equals (check)) {
         throw new TestException (
            "Expecting list\n" + check.toString ("%8.3f") +
            "Got:\n" + list.toString ("%8.3f"));
      }
   }

   void checkInterpolation (NumericList list, double t, double v0, double v1) {
      VectorNd v = new VectorNd (myVsize);
      VectorNd vcheck = new VectorNd (myVsize);
      vcheck.set (0, v0);
      vcheck.set (1, v1);
      list.interpolate (v, t);
      if (!v.epsilonEquals (vcheck, 1e-9)) {
         throw new TestException (
            "Interpolation at time " + t + "\n" +
            "Got " + v.toString ("%8.3f") +
            ", expected " + vcheck.toString ("%8.3f"));
      }
   }

   public NumericListTest() {
   }

   public void simpleTests() {
      list.add (newKnot (0, 0, 0));
      list.add (newKnot (2, 4, 0));
      list.add (newKnot (4, 0, 0));

      checkContents (list, new double[] { 0, 0, 0, 2, 4, 0, 4, 0, 0 });
      checkInterpolation (list, 0, 0, 0);
      checkInterpolation (list, -1, 0, 0);
      checkInterpolation (list, 1, 0, 0);
      checkInterpolation (list, 2, 4, 0);
      checkInterpolation (list, 3, 4, 0);
      checkInterpolation (list, 4, 0, 0);
      checkInterpolation (list, 5, 0, 0);

      list.clear();
      list.add (newKnot (4, 0, 0));
      list.add (newKnot (2, 4, 0));
      list.add (newKnot (0, 0, 0));

      checkContents (list, new double[] { 0, 0, 0, 2, 4, 0, 4, 0, 0 });

      NumericList newList = null;

      newList = (NumericList)list.clone();

      checkContents (newList, new double[] { 0, 0, 0, 2, 4, 0, 4, 0, 0 });

      list.clear();
      list.add (newKnot (3, 1, 0));
      list.add (newKnot (5, 3, 0));
      list.add (newKnot (2, 2, 0));

      checkContents (list, new double[] { 2, 2, 0, 3, 1, 0, 5, 3, 0 });

      checkInterpolation (list, 1, 0, 0);
      checkInterpolation (list, 2, 2, 0);
      checkInterpolation (list, 4, 1, 0);
      list.getInterpolation().setDataExtended (true);
      checkInterpolation (list, 5, 3, 0);
      checkInterpolation (list, 6, 3, 0);
      list.getInterpolation().setDataExtended (false);
      checkInterpolation (list, 6, 0, 0);

      list.setInterpolation (new Interpolation (
         Interpolation.Order.Linear, false));

      checkInterpolation (list, 1, 0, 0);
      checkInterpolation (list, 2, 2, 0);
      checkInterpolation (list, 4, 2, 0);
      list.getInterpolation().setDataExtended (true);
      checkInterpolation (list, 5, 3, 0);
      checkInterpolation (list, 6, 3, 0);
      list.getInterpolation().setDataExtended (false);
      checkInterpolation (list, 6, 0, 0);
   }

   public void test() {
      //simpleTests();
      testInterpolation();
   }

   public void testInterpolation (NumericList list) {
      double t0 = list.getFirst().t;
      double tL = list.getLast().t;

      int vsize = list.getVectorSize();

      boolean extend = list.getInterpolation().isDataExtended();
      Order order = list.getInterpolation().getOrder();
      RotationRep rotRep = list.getRotationRep();

      double tol = 100*EPS;

      int transSize = getTransSize (list);
      int numRots = numRotationTerms (list);

      VectorNd ptrans0 = new VectorNd(transSize);
      VectorNd ptrans1 = new VectorNd(transSize);
      VectorNd ptrans2 = new VectorNd(transSize);
      VectorNd vtrans0 = new VectorNd(transSize);
      VectorNd vtrans1 = new VectorNd(transSize);
      Quaternion[] quats0 = allocQuats(numRots);
      Quaternion[] quats1 = allocQuats(numRots);
      Quaternion[] quats2 = allocQuats(numRots);
      Vector3d[] rotVels0 = alloc3Vecs(numRots);
      Vector3d[] rotVels1 = alloc3Vecs(numRots);

      VectorNd ptrans = new VectorNd(transSize);
      VectorNd vtrans = new VectorNd(transSize);
      Quaternion[] quats = allocQuats(numRots);
      Vector3d[] rotVels = alloc3Vecs(numRots);
      
      VectorNd vchk = new VectorNd(vsize);
      VectorNd dchk = new VectorNd(transSize + numRots*3);
      VectorNd nchk = new VectorNd(transSize + numRots*3);
      VectorNd v = new VectorNd(vsize);
      VectorNd d = new VectorNd(transSize + numRots*3);
      VectorNd n = new VectorNd(transSize + numRots*3);

      int nsamps = 10;
      for (int i=-1; i<=(nsamps+1); i++) {
         double t = t0 + i*(tL-t0)/(double)nsamps;
         NumericListKnot prev = list.findKnotAtOrBefore(t, null);
         NumericListKnot next = prev.getNext();

         if (t < prev.t || (next == null && t > prev.t)) {
            if (extend) {
               vchk.set (prev.v);
            }
            else {
               vchk.setZero();
            }
            dchk.setZero();
            nchk.setZero();
         }
         else if (next == null && prev.prev == null) { // t == prev.t
            dchk.setZero();
            nchk.setZero();
            vchk.set (prev.v);            
         }
         else {
            if (next == null) {
               // shift prev and next back so we can compute the velocities
               next = prev;
               prev = prev.prev;
            }
            NumericListKnot pprev = prev.getPrev();
            NumericListKnot nnext = next.getNext();

            if (order == Order.CubicStep) {
               zeroVelocities (vtrans0, rotVels0);
               zeroVelocities (vtrans1, rotVels1);
            }
            else if (order == Order.Linear ||
                     order == Order.SphericalLinear) {
               computeVelocities (vtrans0, rotVels0, prev, next, list);
               computeVelocities (vtrans1, rotVels1, prev, next, list);
            }
            else {
               // cubic
               if (pprev == null) {
                  computeVelocities (vtrans0, rotVels0, prev, next, list);
               }
               else {
                  computeVelocities (vtrans0, rotVels0, pprev, next, list);
               }
               if (nnext == null) {
                  computeVelocities (vtrans1, rotVels1, prev, next, list);
               }
               else {
                  computeVelocities (vtrans1, rotVels1, prev, nnext, list);
               }
            }
            unpackRotationAndTransTerms (ptrans0, quats0, prev.v, list);
            unpackRotationAndTransTerms (ptrans1, quats1, next.v, list);
            
            double h = next.t-prev.t;
            double s = (t-prev.t)/h;
            if (transSize > 0) {
               if (order == Order.Linear ||
                   order == Order.SphericalLinear) {
                  ptrans.combine (1-s, ptrans0, s, ptrans1);
                  vtrans.set (vtrans0);
               }
               else {
                  VectorNd.hermiteInterpolate (
                     ptrans, ptrans0, vtrans0, ptrans1, vtrans1, s, h);
                  VectorNd.hermiteVelocity (
                     vtrans, ptrans0, vtrans0, ptrans1, vtrans1, s, h);
               }
            }
            for (int k=0; k<numRots; k++) {
               if (order == Order.Linear ||
                   order == Order.SphericalLinear) {
                  quats[k].sphericalInterpolate (quats0[k], s, quats1[k]);
                  rotVels[k].set (rotVels0[k]);
               }
               else {
                  Quaternion.sphericalHermiteGlobal (
                     quats[k], rotVels[k],
                     quats0[k], rotVels0[k], quats1[k], rotVels1[k], s, h);
               }
            }
            VectorNd vref = (s < 0.5 ? prev.v : next.v);
            if (next.next == null && next.t == t) {
               vchk.set (next.v);
            }
            else {
               packRotationAndTransTerms (vchk, vref, ptrans, quats, list);
            }
            packVelocityTerms (dchk, vtrans, rotVels, list);

            // compute check values for numerical derivative
            double h0 = 0;
            double h1 = 0;
            if (t - prev.t > h/2 && nnext != null) {
               computeVelocities (vtrans0, rotVels0, prev, next, list);
               computeVelocities (vtrans1, rotVels1, next, nnext, list);
               h0 = next.t-prev.t;
               h1 = nnext.t-next.t;
               s = (t - (prev.t+next.t)/2)/(h0/2+h1/2);
            }
            else if (t - prev.t <= h/2 && pprev != null) {
               computeVelocities (vtrans0, rotVels0, pprev, prev, list);
               computeVelocities (vtrans1, rotVels1, prev, next, list);
               h0 = prev.t-pprev.t;
               h1 = next.t-prev.t;
               s = (t - (pprev.t+prev.t)/2)/(h0/2+h1/2);
            }
            if (h0 != 0) {
               // interpolate velocities
               vtrans.combine (1-s, vtrans0, s, vtrans1);
               for (int k=0; k<numRots; k++) {
                  rotVels[k].combine (1-s, rotVels0[k], s, rotVels1[k]);
               }
            }
            else {
               // just use linear velocities
               computeVelocities (vtrans, rotVels, prev, next, list);
            }
            packVelocityTerms (nchk, vtrans, rotVels, list);
         }
         list.interpolate (v, t);
         list.interpolateDeriv (d, t);
         list.numericalDeriv (n, t);
         tol = 300*EPS*vchk.infinityNorm();
         if (!v.epsilonEquals (vchk, tol)) {
            System.out.println ("prev=" + prev.v);
            System.out.println ("next=" + next.v);
         }
         String info = "(t="+t+", order="+order+", rotRep="+rotRep+")";
         checkEquals ("value at "+info, v, vchk, tol);
         checkEquals ("deriv at "+info, d, dchk, tol);
         checkEquals ("numderiv at "+info, n, nchk, tol);
      }
   }

   Quaternion[] allocQuats (int num) {
      Quaternion[] quats = new Quaternion[num];
      for (int k=0; k<num; k++) {
         quats[k] = new Quaternion();
      }
      return quats;
   }

   Vector3d[] alloc3Vecs (int num) {
      Vector3d[] vecs = new Vector3d[num];
      for (int k=0; k<num; k++) {
         vecs[k] = new Vector3d();
      }
      return vecs;
   }

   void computeVelocities (
      VectorNd tvel, Vector3d[] rotVels, 
      NumericListKnot knot0, NumericListKnot knot1, NumericList list) {
      
      int numRots = rotVels.length;
      VectorNd ptran0 = new VectorNd(tvel.size());
      VectorNd ptran1 = new VectorNd(tvel.size());
      Quaternion[] quats0 = allocQuats(numRots);
      Quaternion[] quats1 = allocQuats(numRots);
      unpackRotationAndTransTerms (ptran0, quats0, knot0.v, list);
      unpackRotationAndTransTerms (ptran1, quats1, knot1.v, list);
      double h = knot1.t - knot0.t;
      tvel.sub (ptran1, ptran0);
      tvel.scale (1/h);
      for (int k=0; k<numRots; k++) {
         quats0[k].sphericalVelocity (rotVels[k], quats1[k], h);
      }
   }

   void zeroVelocities (VectorNd tvel, Vector3d[] rotVels) {
      int numRots = rotVels.length;
      tvel.setZero();
      for (int k=0; k<numRots; k++) {
         rotVels[k].setZero();
      }
   }

   int numRotationTerms (NumericList list) {
      Order order = list.getInterpolationOrder();
      int vsize = list.getVectorSize();
      int[] rotOffs = list.getRotationSubvecOffsets();
      if (rotOffs.length > 0) {
         return rotOffs.length;
      }
      else if ((order==Order.SphericalLinear || order==Order.SphericalCubic) &&
               (vsize==4 || vsize==7 || vsize==16)) {
         return 1;
      }
      else {
         return 0;
      }
   }

   int getTransSize (NumericList list) {
      Order order = list.getInterpolationOrder();
      int vsize = list.getVectorSize();
      int[] rotOffs = list.getRotationSubvecOffsets();
      if (rotOffs.length > 0) {
         RotationRep rotRep = list.getRotationRep();
         return vsize - rotOffs.length*rotRep.size();
      }
      else if ((order==Order.SphericalLinear || order==Order.SphericalCubic) &&
               (vsize==4 || vsize==7 || vsize==16)) {
         return vsize == 4 ? 0 : 3;
      }
      else {
         return vsize;
      }
   }

   void unpackRotationAndTransTerms (
      VectorNd ptran, Quaternion[] quats, VectorNd vec, NumericList list) {
      Order order = list.getInterpolationOrder();
      int vsize = list.getVectorSize();
      double[] vbuf = vec.getBuffer();
      int[] rotOffs = list.getRotationSubvecOffsets();
      if (rotOffs.length > 0) {
         VectorNd v = new VectorNd();
         RotationRep rotRep = list.getRotationRep();
         if (rotRep == null) {
            throw new InternalErrorException (
               "List has rotation subvectors but rotation rep is null");
         }
         int k = 0; // rotation term index
         for (int i=0; i<vsize; i++) {
            if (k < rotOffs.length && i == rotOffs[k]) {
               quats[k].set (vbuf, i, rotRep, /*scale*/1);
               i += (rotRep.size()-1);
               k++;
            }
            else {
               v.append (vec.get(i));
            }
         }
         ptran.set (v);
      }
      else if ((order==Order.SphericalLinear || order==Order.SphericalCubic) &&
               (vsize==4 || vsize==7 || vsize==16)) {
         switch (vsize) {
            case 4: {
               quats[0].set (vbuf, 0, RotationRep.AXIS_ANGLE_DEG, /*scale*/1);
               ptran.setSize(0);
               break;
            }
            case 7: {
               quats[0].set (vbuf, 3, RotationRep.AXIS_ANGLE_DEG, /*scale*/1);
               ptran.setSize(3);
               ptran.set (0, vbuf[0]);
               ptran.set (1, vbuf[1]);
               ptran.set (2, vbuf[2]);
               break;
            }
            case 16: {
               RotationMatrix3d R = new RotationMatrix3d();
               R.set (vbuf[0], vbuf[1], vbuf[2], 
                      vbuf[4], vbuf[5], vbuf[6], 
                      vbuf[8], vbuf[9], vbuf[10]);
               quats[0].set (R);
               ptran.setSize(3);
               ptran.set (0, vbuf[3]);
               ptran.set (1, vbuf[7]);
               ptran.set (2, vbuf[11]);
               break;
            }
            default: {
               // cant happen
            }
         }
      }
      else {
         ptran.set (vec);
      }
   }

   void packRotationAndTransTerms (
      VectorNd pos, VectorNd ref,
      VectorNd ptran, Quaternion[] quats, NumericList list) {
      Order order = list.getInterpolationOrder();
      int vsize = list.getVectorSize();
      double[] pbuf = pos.getBuffer();
      double[] rbuf = (ref != null ? ref.getBuffer() : null);
      int[] rotOffs = list.getRotationSubvecOffsets();
      if (rotOffs.length > 0) {
         RotationRep rotRep = list.getRotationRep();
         if (rotRep == null) {
            throw new InternalErrorException (
               "List has rotation subvectors but rotation rep is null");
         }
         int k = 0; // rotation term index
         int j = 0; // linear index
         for (int i=0; i<vsize; i++) {
            if (k < rotOffs.length && i == rotOffs[k]) {
               quats[k].get (pbuf, rbuf, i, rotRep, /*scale*/1);
               i += (rotRep.size()-1);
               k++;
            }
            else {
               pbuf[i] = ptran.get(j++);
            }
         }
      }
      else if ((order==Order.SphericalLinear || order==Order.SphericalCubic) &&
               (vsize==4 || vsize==7 || vsize==16)) {
         switch (vsize) {
            case 4: {
               quats[0].get (
                  pbuf, null, 0, RotationRep.AXIS_ANGLE_DEG, /*scale*/1);
               break;
            }
            case 7: {
               quats[0].get (
                  pbuf, null, 3, RotationRep.AXIS_ANGLE_DEG, /*scale*/1);
               
               ptran.get (pbuf);
               break;
            }
            case 16: {
               RotationMatrix3d R = new RotationMatrix3d();
               R.set (quats[0]);
               pbuf[0] = R.m00; pbuf[1] = R.m01; pbuf[2] = R.m02;
               pbuf[4] = R.m10; pbuf[5] = R.m11; pbuf[6] = R.m12;
               pbuf[8] = R.m20; pbuf[9] = R.m21; pbuf[10] = R.m22;
               pbuf[3] = ptran.get(0);
               pbuf[7] = ptran.get(1);
               pbuf[11] = ptran.get(2);
               pbuf[12] = 0; pbuf[13] = 0; pbuf[14] = 0; pbuf[15] = 1; 
               break;
            }
            default: {
               // cant happen
            }
         }
      }
      else {
         pos.set(ptran);
      }
   }

   void packVelocityTerms (
      VectorNd vel, VectorNd vtran, Vector3d[] angVels, NumericList list) {
      Order order = list.getInterpolationOrder();
      int vsize = list.getVectorSize();
      double[] vbuf = vel.getBuffer();
      int[] rotOffs = list.getRotationSubvecOffsets();
      if (rotOffs.length > 0) {
         RotationRep rotRep = list.getRotationRep();
         if (rotRep == null) {
            throw new InternalErrorException (
               "List has rotation subvectors but rotation rep is null");
         }
         int k = 0; // rotation term index
         int l = 0; // linear index
         int j = 0; // velocity index
         for (int i=0; i<vsize; i++) {
            if (k < rotOffs.length && i == rotOffs[k]) {
               vel.setSubVector (j, angVels[k]);
               i += (rotRep.size()-1);
               j += 3;
               k++;
            }
            else {
               vbuf[j++] = vtran.get(l++);
            }
         }
      }
      else if ((order==Order.SphericalLinear || order==Order.SphericalCubic) &&
               (vsize==4 || vsize==7 || vsize==16)) {
         switch (vsize) {
            case 4: {
               vel.setSubVector (0, angVels[0]);
               break;
            }
            case 7:
            case 16: {
               vel.setSubVector (0, vtran);
               vel.setSubVector (3, angVels[0]);
               break;
            }
            default: {
               // cant happen
            }
         }
      }
      else {
         vel.set(vtran);
      }
   }

   public void testInterpolation() {
      double[] times0 = new double[] { 0, 1, 2 };
      double[] times1 = new double[] { 1, 1.5, 3.45, 4.2, 10 };
      double[] times2 = new double[] { 0, 0.5, 2.3, 3.0, 5.3 };

      RotationRep[] rotReps = RotationRep.values();

      ArrayList<NumericList> lists = new ArrayList<>();
      for (double[] times : new double[][] { times0, times1, times2 }) {
         for (RotationRep rotRep : rotReps) {
            lists.add (createRotationSubvecList ("ppp", rotRep, times));
            lists.add (createRotationSubvecList ("r", rotRep, times));
            lists.add (createRotationSubvecList ("rr", rotRep, times));
            lists.add (createRotationSubvecList ("pr", rotRep, times));
            lists.add (createRotationSubvecList ("prrprp", rotRep, times));
            lists.add (createRotationSubvecList ("prprp", rotRep, times));
         }
         lists.add (createAxisAngleList (times));
         lists.add (createPosAxisAngleList (times));
         lists.add (createRigidTransformList (times));
      }
      
      for (NumericList list : lists) {
         for (Order order : Order.values()) { 
            if (order != Order.Step && order != Order.Parabolic) {
               list.setInterpolation (new Interpolation (order, /*extend*/false));
               testInterpolation (list);
               list.setInterpolation (new Interpolation (order, /*extend*/true));
               testInterpolation (list);
            }
         }
      }
   }

   public static void main (String[] args) {
      NumericListTest tester = new NumericListTest();

      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }
}
