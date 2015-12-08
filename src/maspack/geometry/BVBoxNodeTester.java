/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;

import maspack.util.InternalErrorException;
/**
 * A worker class that tests AABB and OBB bounding boxes for intersection. Each
 * instance of this class expects the first and second bounding box to always
 * be of a particular type, as specified by the enum <code>BoxTypes</code>.
 */
public class BVBoxNodeTester implements BVNodeTester {
   
   RotationMatrix3d myR = new RotationMatrix3d();
   Vector3d myPd = new Vector3d();
   Vector3d myPx = new Vector3d();
   Vector3d myHw1 = new Vector3d();
   Vector3d myHw2 = new Vector3d();
   Vector3d myC1 = new Vector3d(); // center of AABB1, if needed
   Vector3d myC2 = new Vector3d(); // center of AABB2, if needed

   /**
    * Decsribes the type expected for the first and second bounding box.
    */
   public enum BoxTypes {
      /**
       * The first and second boxes should both be AABBs.
       */
      AABBtoAABB,
      /**
       * The first box should be an AABB and the second should be an OBB.
       */
      AABBtoOBB, 
      /**
       * The first box should be an OBB and the second should be an AABB.
       */
      OBBtoAABB, 
      /**
       * The first and second boxes should both be OBBs.
       */
      OBBtoOBB,
      /**
       * Unknown box types
       */
      Uknown
   };

   private BoxTypes myBoxTypes = BoxTypes.AABBtoAABB;

   BVBoxNodeTester (BoxTypes boxTypes) {
      myBoxTypes = boxTypes;
   }
   

   BVBoxNodeTester (BVTree bvh1, BVTree bvh2) {
      this (bvh1.getRoot(), bvh2.getRoot());
   }         

   BVBoxNodeTester (BVNode node1, BVNode node2) {

      if ((!(node1 instanceof AABB) && !(node1 instanceof OBB)) ||
          (!(node2 instanceof AABB) && !(node2 instanceof OBB))) {
         throw new IllegalArgumentException (
            "BVBoxNodeTester can handle only AABBNodes and OBBNodes");
      }
      if (node1 instanceof AABB) {
         if (node2 instanceof AABB) {
            myBoxTypes = BoxTypes.AABBtoAABB;
         }
         else {
            myBoxTypes = BoxTypes.AABBtoOBB;
         }
      }
      else {
         if (node2 instanceof AABB) {
            myBoxTypes = BoxTypes.OBBtoAABB;
         }
         else {
            myBoxTypes = BoxTypes.OBBtoOBB;
         }
      }
   }
   
   /**
    * Compyte the center and half-widths for an AABB.
    */
   private final void computeCenterAndHalfWidths (
      Vector3d c, Vector3d hw, AABB box) {
      c.x = (box.myMax.x + box.myMin.x)/2;
      c.y = (box.myMax.y + box.myMin.y)/2;
      c.z = (box.myMax.z + box.myMin.z)/2;
      hw.x = (box.myMax.x - box.myMin.x)/2;
      hw.y = (box.myMax.y - box.myMin.y)/2;
      hw.z = (box.myMax.z - box.myMin.z)/2;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isDisjoint (BVNode node1, BVNode node2, RigidTransform3d XBA) {
      switch (myBoxTypes) {
         case AABBtoAABB: {
            return isDisjoint ((AABB)node1, (AABB)node2, XBA);
         }
         case AABBtoOBB: {
            return isDisjoint ((AABB)node1, (OBB)node2, XBA);
         }
         case OBBtoAABB: {
            return isDisjoint ((OBB)node1, (AABB)node2, XBA);
         }
         case OBBtoOBB: {
            return isDisjoint ((OBB)node1, (OBB)node2, XBA);
         }
         default:{
            throw new InternalErrorException (
               "Unimplemented box types: " + myBoxTypes);
         }
      }
   }

   public boolean isDisjoint (
      AABB box1, AABB box2, RigidTransform3d XBA) {
      if (XBA == RigidTransform3d.IDENTITY) {
         return (
            box1.myMax.x < box2.myMin.x || box1.myMin.x > box2.myMax.x ||
            box1.myMax.y < box2.myMin.y || box1.myMin.y > box2.myMax.y ||
            box1.myMax.z < box2.myMin.z || box1.myMin.z > box2.myMax.z);
      }
      else {
         computeCenterAndHalfWidths (myC1, myHw1, box1);
         computeCenterAndHalfWidths (myC2, myHw2, box2);
         myPd.transform (XBA.R, myC2);
         myPd.add (XBA.p);
         myPd.sub (myC1);
         return isDisjoint (myHw1, myHw2, XBA.R, myPd);
      }
   }

   public boolean isDisjoint (AABB box1, OBB box2, RigidTransform3d XBA) {
      computeCenterAndHalfWidths (myC1, myHw1, box1);
      if (XBA == RigidTransform3d.IDENTITY) {
         myPd.sub (box2.myX.p, myC1);
         return isDisjoint (myHw1, box2.myHalfWidths, box2.myX.R, myPd);
      }
      else {
         myPd.set (box2.myX.p);
         myPx.sub (XBA.p, myC1);
         myR.transpose (XBA.R);
         return isDisjoint (
            myHw1, box2.myHalfWidths, myR, box2.myX.R, myPd, myPx);
      }
   }

   public boolean isDisjoint (OBB box1, AABB box2, RigidTransform3d XBA) {
      computeCenterAndHalfWidths (myC2, myHw2, box2);
      if (XBA == RigidTransform3d.IDENTITY) {
         myPd.sub (box1.myX.p, myC2);
         return isDisjoint (myHw2, box1.myHalfWidths, box1.myX.R, myPd);
      }
      else {
         myPd.transform (XBA.R, myC2);
         myPd.add (XBA.p);
         myPd.sub (box1.myX.p);
         myPx.setZero();
         return isDisjoint (
            box1.myHalfWidths, myHw2,
            box1.myX.R, XBA.R, myPd, myPx);
      }
   }

   public boolean isDisjoint (OBB box1, OBB box2, RigidTransform3d XBA) {
      if (XBA == RigidTransform3d.IDENTITY) {
         myPd.sub (box2.myX.p, box1.myX.p);
         myPx.setZero();
         return isDisjoint (
            box1.myHalfWidths, box2.myHalfWidths,
            box1.myX.R, box2.myX.R, myPd, myPx);
      }
      else {
         myPd.transform (XBA.R, box2.myX.p);
         myPd.add (XBA.p);
         myPd.sub (box1.myX.p);
         myPx.setZero();
         myR.mul (XBA.R, box2.myX.R);
         return isDisjoint (
            box1.myHalfWidths, box2.myHalfWidths,
            box1.myX.R, myR, myPd, myPx);
      }
   }

   public static boolean isDisjoint (OBB box1, OBB box2) {
      Vector3d pd = new Vector3d();
      pd.sub (box2.myX.p, box1.myX.p);
      return isDisjoint (
         box1.myHalfWidths, box2.myHalfWidths,
         box1.myX.R, box2.myX.R, pd, Vector3d.ZERO);
   }

   public static boolean isDisjoint (AABB box1, AABB box2) {
      return (
         box1.myMax.x < box2.myMin.x || box1.myMin.x > box2.myMax.x ||
         box1.myMax.y < box2.myMin.y || box1.myMin.y > box2.myMax.y ||
         box1.myMax.z < box2.myMin.z || box1.myMin.z > box2.myMax.z);
   }

   /**
    * Determines if two bounding boxes with half widths <code>hw1</code> and
    * <code>hw2</code> are disjoint. The first box is assumed to be axis-aligned
    * and centered at the origin, while the coordinate frame of the second
    * box has position and orientation given by
    * <pre>
    * p21 = R1^T pd + px
    * R21 = R1^T R2
    * </pre>
    * The reason for providing <code>p21</code> and <code>R21</code> in
    * factored form is so their components can be computed on demand, as
    * they may not all be needed before the test is decided.
    *
    * <p> For details on this algorithm, see pg 338 of Game Physics by David
    * Eberly, or "OBBTree: A Hierarchichal Structure for Rapid Interference
    * Detection", Gottschalk Lin &amp; Manocha
    */
   public static final boolean isDisjoint (
      Vector3d hw1, Vector3d hw2, RotationMatrix3d R1, RotationMatrix3d R2,
      Vector3d pd, Vector3d px) {

      double t;
      double cutoff = 1-1e-10;

      // mij and pi give the transformation from the argument obb to this obb.

      // A1 x A2 = A0
      double p_x = R1.m00 * pd.x + R1.m10 * pd.y + R1.m20 * pd.z + px.x;
      if ((t = p_x) < 0)
         t = -t;
      double m00 = R1.m00 * R2.m00 + R1.m10 * R2.m10 + R1.m20 * R2.m20;
      double m01 = R1.m00 * R2.m01 + R1.m10 * R2.m11 + R1.m20 * R2.m21;
      double m02 = R1.m00 * R2.m02 + R1.m10 * R2.m12 + R1.m20 * R2.m22;
      double abs00 = (m00 >= 0 ? m00 : -m00);
      double abs01 = (m01 >= 0 ? m01 : -m01);
      double abs02 = (m02 >= 0 ? m02 : -m02);
      if (t > (hw1.x + hw2.x * abs00 + hw2.y * abs01 + hw2.z * abs02))
         return true;

      // B1 x B2 = B0
      double m10 = R1.m01 * R2.m00 + R1.m11 * R2.m10 + R1.m21 * R2.m20;
      double m20 = R1.m02 * R2.m00 + R1.m12 * R2.m10 + R1.m22 * R2.m20;
      double p_y = R1.m01 * pd.x + R1.m11 * pd.y + R1.m21 * pd.z + px.y;
      double p_z = R1.m02 * pd.x + R1.m12 * pd.y + R1.m22 * pd.z + px.z;
      if ((t = p_x * m00 + p_y * m10 + p_z * m20) < 0)
         t = -t;
      double abs10 = (m10 >= 0 ? m10 : -m10);
      double abs20 = (m20 >= 0 ? m20 : -m20);
      if (t > (hw2.x + hw1.x * abs00 + hw1.y * abs10 + hw1.z * abs20))
         return true;

      // A2 x A0 = A1
      if ((t = p_y) < 0)
         t = -t;
      double m11 = R1.m01 * R2.m01 + R1.m11 * R2.m11 + R1.m21 * R2.m21;
      double m12 = R1.m01 * R2.m02 + R1.m11 * R2.m12 + R1.m21 * R2.m22;
      double abs11 = (m11 >= 0 ? m11 : -m11);
      double abs12 = (m12 >= 0 ? m12 : -m12);
      if (t > (hw1.y + hw2.x * abs10 + hw2.y * abs11 + hw2.z * abs12))
         return true;

      // A0 x A1 = A2
      if ((t = p_z) < 0)
         t = -t;
      double m21 = R1.m02 * R2.m01 + R1.m12 * R2.m11 + R1.m22 * R2.m21;
      double m22 = R1.m02 * R2.m02 + R1.m12 * R2.m12 + R1.m22 * R2.m22;
      double abs21 = (m21 >= 0 ? m21 : -m21);
      double abs22 = (m22 >= 0 ? m22 : -m22);
      if (t > (hw1.z + hw2.x * abs20 + hw2.y * abs21 + hw2.z * abs22))
         return true;

      // B2 x B0 = B1
      if ((t = p_x * m01 + p_y * m11 + p_z * m21) < 0)
         t = -t;
      if (t > (hw2.y + hw1.x * abs01 + hw1.y * abs11 + hw1.z * abs21))
         return true;

      // B0 x B1 = B2
      if ((t = p_x * m02 + p_y * m12 + p_z * m22) < 0)
         t = -t;
      if (t > (hw2.z + hw1.x * abs02 + hw1.y * abs12 + hw1.z * abs22))
         return true;

      
      // If any faces are (nearly) parallel, we are done (according to Eberly)
      if ( abs00 > cutoff || abs01 > cutoff || abs02 > cutoff
         || abs10 > cutoff || abs11 > cutoff || abs12 > cutoff
         || abs20 > cutoff || abs21 > cutoff || abs22 > cutoff) {
         return false;
      }
      
      // A0 x B0
      if ((t = p_z * m10 - p_y * m20) < 0)
         t = -t;
      if (t > (hw1.y * abs20 + hw1.z * abs10 + hw2.y * abs02 + hw2.z * abs01))
         return true;

      // A0 x B1
      if ((t = p_z * m11 - p_y * m21) < 0)
         t = -t;
      if (t > (hw1.y * abs21 + hw1.z * abs11 + hw2.x * abs02 + hw2.z * abs00))
         return true;

      // A0 x B2
      if ((t = p_z * m12 - p_y * m22) < 0)
         t = -t;
      if (t > (hw1.y * abs22 + hw1.z * abs12 + hw2.x * abs01 + hw2.y * abs00))
         return true;

      // A1 x B0
      if ((t = p_x * m20 - p_z * m00) < 0)
         t = -t;
      if (t > (hw1.x * abs20 + hw1.z * abs00 + hw2.y * abs12 + hw2.z * abs11))
         return true;

      // A1 x B1
      if ((t = p_x * m21 - p_z * m01) < 0)
         t = -t;
      if (t > (hw1.x * abs21 + hw1.z * abs01 + hw2.x * abs12 + hw2.z * abs10))
         return true;

      // A1 x B2
      if ((t = p_x * m22 - p_z * m02) < 0)
         t = -t;
      if (t > (hw1.x * abs22 + hw1.z * abs02 + hw2.x * abs11 + hw2.y * abs10))
         return true;

      // A2 x B0
      if ((t = p_y * m00 - p_x * m10) < 0)
         t = -t;
      if (t > (hw1.x * abs10 + hw1.y * abs00 + hw2.y * abs22 + hw2.z * abs21))
         return true;

      // A2 x B1
      if ((t = p_y * m01 - p_x * m11) < 0)
         t = -t;
      if (t > (hw1.x * abs11 + hw1.y * abs01 + hw2.x * abs22 + hw2.z * abs20))
         return true;

      // A2 x B2
      if ((t = p_y * m02 - p_x * m12) < 0)
         t = -t;
      if (t > (hw1.x * abs12 + hw1.y * abs02 + hw2.x * abs21 + hw2.y * abs20))
         return true;

      return false;
   }

   /**
    * Determines if two bounding boxes with half widths <code>hw1</code> and
    * <code>hw2</code> are disjoint. The first box is assumed to be axis-aligned
    * and centered at the origin, while the coordinate frame of the second
    * box has position and orientation given by <code>p21</code> and
    * <code>R21</code>, respectively.
    *
    * <p> For details on this algorithm, see pg 338 of Game Physics by David
    * Eberly, or "OBBTree: A Hierarchichal Structure for Rapid Interference
    * Detection", Gottschalk Lin &amp; Manocha
    */
   public static final boolean isDisjoint (
      Vector3d hw1, Vector3d hw2, RotationMatrix3d R21, Vector3d p21) {

      double t;

      // A1 x A2 = A0
      double p_x = p21.x;
      if ((t = p_x) < 0)
         t = -t;
      double m00 = R21.m00;
      double m01 = R21.m01;
      double m02 = R21.m02;
      double abs00 = (m00 >= 0 ? m00 : -m00);
      double abs01 = (m01 >= 0 ? m01 : -m01);
      double abs02 = (m02 >= 0 ? m02 : -m02);
      if (t > (hw1.x + hw2.x * abs00 + hw2.y * abs01 + hw2.z * abs02))
         return true;

      // B1 x B2 = B0
      double m10 = R21.m10;
      double m20 = R21.m20;
      double p_y = p21.y;
      double p_z = p21.z;
      if ((t = p_x * m00 + p_y * m10 + p_z * m20) < 0)
         t = -t;
      double abs10 = (m10 >= 0 ? m10 : -m10);
      double abs20 = (m20 >= 0 ? m20 : -m20);
      if (t > (hw2.x + hw1.x * abs00 + hw1.y * abs10 + hw1.z * abs20))
         return true;

      // A2 x A0 = A1
      if ((t = p_y) < 0)
         t = -t;
      double m11 = R21.m11;
      double m12 = R21.m12;
      double abs11 = (m11 >= 0 ? m11 : -m11);
      double abs12 = (m12 >= 0 ? m12 : -m12);
      if (t > (hw1.y + hw2.x * abs10 + hw2.y * abs11 + hw2.z * abs12))
         return true;

      // A0 x A1 = A2
      if ((t = p_z) < 0)
         t = -t;
      double m21 = R21.m21;
      double m22 = R21.m22;
      double abs21 = (m21 >= 0 ? m21 : -m21);
      double abs22 = (m22 >= 0 ? m22 : -m22);
      if (t > (hw1.z + hw2.x * abs20 + hw2.y * abs21 + hw2.z * abs22))
         return true;

      // B2 x B0 = B1
      if ((t = p_x * m01 + p_y * m11 + p_z * m21) < 0)
         t = -t;
      if (t > (hw2.y + hw1.x * abs01 + hw1.y * abs11 + hw1.z * abs21))
         return true;

      // B0 x B1 = B2
      if ((t = p_x * m02 + p_y * m12 + p_z * m22) < 0)
         t = -t;
      if (t > (hw2.z + hw1.x * abs02 + hw1.y * abs12 + hw1.z * abs22))
         return true;

      // A0 x B0
      if ((t = p_z * m10 - p_y * m20) < 0)
         t = -t;
      if (t > (hw1.y * abs20 + hw1.z * abs10 + hw2.y * abs02 + hw2.z * abs01))
         return true;

      // A0 x B1
      if ((t = p_z * m11 - p_y * m21) < 0)
         t = -t;
      if (t > (hw1.y * abs21 + hw1.z * abs11 + hw2.x * abs02 + hw2.z * abs00))
         return true;

      // A0 x B2
      if ((t = p_z * m12 - p_y * m22) < 0)
         t = -t;
      if (t > (hw1.y * abs22 + hw1.z * abs12 + hw2.x * abs01 + hw2.y * abs00))
         return true;

      // A1 x B0
      if ((t = p_x * m20 - p_z * m00) < 0)
         t = -t;
      if (t > (hw1.x * abs20 + hw1.z * abs00 + hw2.y * abs12 + hw2.z * abs11))
         return true;

      // A1 x B1
      if ((t = p_x * m21 - p_z * m01) < 0)
         t = -t;
      if (t > (hw1.x * abs21 + hw1.z * abs01 + hw2.x * abs12 + hw2.z * abs10))
         return true;

      // A1 x B2
      if ((t = p_x * m22 - p_z * m02) < 0)
         t = -t;
      if (t > (hw1.x * abs22 + hw1.z * abs02 + hw2.x * abs11 + hw2.y * abs10))
         return true;

      // A2 x B0
      if ((t = p_y * m00 - p_x * m10) < 0)
         t = -t;
      if (t > (hw1.x * abs10 + hw1.y * abs00 + hw2.y * abs22 + hw2.z * abs21))
         return true;

      // A2 x B1
      if ((t = p_y * m01 - p_x * m11) < 0)
         t = -t;
      if (t > (hw1.x * abs11 + hw1.y * abs01 + hw2.x * abs22 + hw2.z * abs20))
         return true;

      // A2 x B2
      if ((t = p_y * m02 - p_x * m12) < 0)
         t = -t;
      if (t > (hw1.x * abs12 + hw1.y * abs02 + hw2.x * abs21 + hw2.y * abs20))
         return true;

      return false;
   }


}
