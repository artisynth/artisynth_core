/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.util.Map;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.VectorNd;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.MatrixBlock;
import maspack.matrix.*;
import maspack.util.DoubleInterval;
import maspack.properties.*;
import maspack.render.*;
import maspack.spatialmotion.Wrench;
import maspack.spatialmotion.RigidBodyConstraint.MotionType;
import maspack.render.Renderer.AxisDrawStyle;

import artisynth.core.modelbase.*;

/**
 * Subclass of BodyConnector that provides support for coordinates.
 */
public abstract class JointBase extends BodyConnector  {

   protected static final double INF = Double.POSITIVE_INFINITY;

   protected static final double RTOD = 180.0/Math.PI; 
   protected static final double DTOR = Math.PI/180.0; 
   
   public static final double DEFAULT_SHAFT_LENGTH = 0;
   protected double myShaftLength = DEFAULT_SHAFT_LENGTH;

   public static final double DEFAULT_SHAFT_RADIUS = 0;
   protected double myShaftRadius = DEFAULT_SHAFT_RADIUS;

   public static PropertyList myProps =
      new PropertyList (JointBase.class, BodyConnector.class);

   static {
      myProps.add (
         "shaftLength", 
         "length of rendered shaft", DEFAULT_SHAFT_LENGTH);
      myProps.add (
         "shaftRadius",
         "radius of rendered shaft", DEFAULT_SHAFT_RADIUS);
   }

   public PropertyInfoList getAllPropertyInfo() {
      return myProps;
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      myShaftLength = DEFAULT_SHAFT_LENGTH;
      myShaftRadius = DEFAULT_SHAFT_RADIUS;
   }

   protected double toRadians (double deg) {
      double rad = DTOR*deg;
      double degchk = RTOD*rad; 
      // make sure DTOR*(RTOD*rad) = rad. One check seems to do work.
      if (degchk != deg) {
         rad = DTOR*degchk;
      }
      return rad;
   }

   /**
    * Returns a length used for rendering shafts or axes associated with this
    * joint. See {@link #setShaftLength} for details.
    *
    * @return shaft rendering length
    */
   public double getShaftLength() {
      return myShaftLength;
   }

   /**
    * Sets a length used for rendering shafts or axes associated with this
    * joint. The default value is 0. Setting a value of -1 will invoke a legacy
    * rendering method, in shafts and axes are rendered as lines, using line
    * rendering properties, with their length specified by the {@code
    * axisLength} property.
    *
    * @param l shaft rendering length
    */
   public void setShaftLength (double l) {
      myShaftLength = l;
   }

   /**
    * Returns a radius used for rendering shafts or axes associated this
    * joint. See {@link #getShaftRadius} for details.
    *
    * @return shaft rendering radius
    */
   public double getShaftRadius() {
      return myShaftRadius;
   }

   /**
    * Sets a radius used for rendering shafts or axes associated with this
    * joint. A value of 0 (which is the default value) will cause the
    * joint to use a default radius which is proportional to
    * {@link #getShaftLength}.
    *
    * @param r shaft rendering radius
    */
   public void setShaftRadius (double r) {
      myShaftRadius = r;
   }

   /**
    * Returns either the shaft render length, or, if that is -1,
    * the axis length.
    */
   protected double getEffectiveShaftLength() {
      if (myShaftLength < 0) {
         return myAxisLength;
      }
      else {
         return myShaftLength;
      }
   }

   /**
    * Returns either the shaft render radius, or, if that is 0,
    * a default value computed from the shaft render length.
    */
   protected double getEffectiveShaftRadius() {
      if (myShaftRadius == 0) {
         return myShaftLength/20.0;
      }
      else {
         return myShaftRadius;
      }
   }

   /**
    * Queries the range for the {@code idx}-th coordinate.
    *
    * @param idx coordinate index
    * @return coordinate range
    */
   public DoubleInterval getCoordinateRange (int idx) {
      return myCoupling.getCoordinateRange (idx);
   }

   /**
    * Queries the minimum range value for the {@code idx}-th coordinate.
    *
    * @param idx coordinate index
    * @return minimum range
    */
   public double getMinCoordinate (int idx) {
      return myCoupling.getCoordinateRange(idx).getLowerBound();
   }

   /**
    * Queries the maximum range value for the {@code idx}-th coordinate.
    *
    * @param idx coordinate index
    * @return maximum range
    */
   public double getMaxCoordinate (int idx) {
      return myCoupling.getCoordinateRange(idx).getUpperBound();
   }

   /**
    * Sets the range for the {@code idx}-th coordinate. Specifying {@code
    * range} as {@code null} will set the range to {@code (-inf, inf)}.
    *
    * @param idx coordinate index
    * @param range new range value
    */
   public void setCoordinateRange (int idx, DoubleInterval range) {
      if (range == null) {
         range = new DoubleInterval (-INF, INF);
      }
      myCoupling.setCoordinateRange (idx, range);
      if (attachmentsInitialized()) {
         // if we are connected to bodies, might have to update the coordinate
         double value = myCoupling.getCoordinate (idx, null);
         double clipped = myCoupling.clipCoordinate (idx, value);
         if (clipped != value) {
            setCoordinate (idx, clipped);
         }
      }     
   }

   /**
    * Queries the range for the {@code idx}-th coordinate in degrees.
    *
    * @param idx coordinate index
    * @return coordinate range in degrees
    */
   public DoubleInterval getCoordinateRangeDeg (int idx) {
      DoubleInterval range = myCoupling.getCoordinateRange (idx);
      return new DoubleInterval (
         RTOD*range.getLowerBound(), RTOD*range.getUpperBound());
   }

   /**
    * Queries the minimum range value for the {@code idx}-th coordinate in
    * degrees.
    *
    * @param idx coordinate index
    * @return minimum range in degrees
    */
   public double getMinCoordinateDeg (int idx) {
      return RTOD*myCoupling.getCoordinateRange(idx).getLowerBound();
   }

   /**
    * Queries the maximum range value for the {@code idx}-th coordinate in
    * degrees.
    *
    * @param idx coordinate index
    * @return maximum range in degrees
    */
   public double getMaxCoordinateDeg (int idx) {
      return RTOD*myCoupling.getCoordinateRange(idx).getUpperBound();
   }

   /**
    * Sets the range for the {@code idx}-th coordinate in degrees. Specifying
    * {@code range} as {@code null} will set the range to {@code (-inf, inf)}.
    *
    * @param idx coordinate index
    * @param range new range value in degrees
    */
   public void setCoordinateRangeDeg (int idx, DoubleInterval range) {
      if (range == null) {
         range = new DoubleInterval (-INF, INF);
      }
      setCoordinateRange (
         idx, new DoubleInterval (
            toRadians(range.getLowerBound()),
            toRadians(range.getUpperBound())));
   }

   /**
    * Returns the number of coordinates, if any, associated with this joint. If
    * coordinates are not supported, this method returns 0.
    *
    * @return number of joint coordinates
    */
   public int numCoordinates() {
      return myCoupling.numCoordinates();
   }

   private void checkCoordinateIndex (int idx) {
      int numc = numCoordinates();
      if (idx < 0 || idx >= numc) {
         if (numc == 0) {
            throw new IndexOutOfBoundsException (
               "joint does not have coordinates");
         }
         else {
            throw new IndexOutOfBoundsException (
               "idx is "+idx+", must be between 0 and "+(numc-1));
         }
      }
   }

   /**
    * Returns the {@code idx}-th coordinate value for this joint, converted
    * to degrees.
    *
    * @param idx index of the coordinate
    * @return coordinate value, converted to degrees.
    */
   public double getCoordinateDeg (int idx) {
      return Math.toDegrees (getCoordinate(idx));
   }

   /**
    * Returns the {@code idx}-th coordinate for this joint. If the joint is
    * connected to other bodies, the value is inferred from the current TCD
    * transform.  Otherwise, it is obtained from a stored internal value.
    *
    * @param idx index of the coordinate
    * @return the coordinate value
    */
   public double getCoordinate (int idx) {
      checkCoordinateIndex (idx);
      RigidTransform3d TCD = null;
      if (attachmentsInitialized()) {
         // get TCD for estimating coordinates
         TCD = new RigidTransform3d();
         getCurrentTCD (TCD);
      }
      return myCoupling.getCoordinate (idx, TCD);
   }

   /**
    * Returns the current speed of the {@code idx}-th coordinate for this joint.
    *
    * @param idx index of the coordinate
    * @return the coordinate speed
    */
   public double getCoordinateSpeed (int idx) {
      checkCoordinateIndex (idx);
      return myCoupling.getCoordinateSpeed (idx);
   }
   
   /**
    * Returns the motion type of the {@code idx}-th coordinate for this joint.
    *
    * @param idx index of the coordinate
    * @return coordinate motion type
    */   
   public MotionType getCoordinateMotionType (int idx) {
      checkCoordinateIndex (idx);
      return myCoupling.getCoordinateMotionType(idx);
   }
   
   /**
    * Applies a generalized force to the {@code idx}-th coordinate for this 
    * joint.
    *
    * @param idx index of the coordinate
    * @param f generalized force to apply
    */
   public void applyCoordinateForce (int idx, double f) {
      checkCoordinateIndex (idx);
      Wrench wrG = myCoupling.getCoordinateWrench (idx);
      if (wrG != null) {
         Wrench wrX = new Wrench();
         wrX.inverseTransform (myTCwG, wrG); // transform to world-aligned TC
         wrX.scale (f);
         myAttachmentA.applyForce (wrX);
         wrX.inverseTransform (myTDwG, wrG); // transform to world-aligned TD
         wrX.scale (-f);
         myAttachmentB.applyForce (wrX);
      }
   }

   private void addSolveBlocks (
      SparseNumberedBlockMatrix M, DynamicComponent ci, DynamicComponent cj) {
      int bi = ci.getSolveIndex();
      int bj = cj.getSolveIndex();
      if (bi != -1 && bj != -1) {
         MatrixBlock blk = M.getBlock (bi, bj);
         if (blk == null) {
            M.addBlock (
               bi, bj, MatrixBlockBase.alloc (
                  ci.getVelStateSize(), cj.getVelStateSize()));
         }
      }
   }

   public void addCoordinateSolveBlocks (SparseNumberedBlockMatrix M, int idx) {
      checkCoordinateIndex (idx);
      Wrench wrG = myCoupling.getCoordinateWrench (idx);
      if (wrG != null) {
         DynamicComponent[] Amasters = myAttachmentA.getMasters();
         DynamicComponent[] Bmasters = myAttachmentB.getMasters();
         for (DynamicComponent cAi : Amasters) {
            for (DynamicComponent cAj : Amasters) {
               addSolveBlocks (M, cAi, cAj);
            }
            for (DynamicComponent cBj : Bmasters) {
               addSolveBlocks (M, cAi, cBj);
            }
         }
         for (DynamicComponent cBi : Bmasters) {
            for (DynamicComponent cAj : Amasters) {
               addSolveBlocks (M, cBi, cAj);
            }
            for (DynamicComponent cBj : Bmasters) {
               addSolveBlocks (M, cBi, cBj);
            }
         }
      }
   }

   private void addVelJacobian (
      MatrixBlock blk, double[] wi, double[] wj, double s) {

      if (blk instanceof Matrix6dBase) {
         Matrix6dBase M = (Matrix6dBase)blk;

         double wj0 = wj[0]*s;
         double wj1 = wj[1]*s;
         double wj2 = wj[2]*s;
         double wj3 = wj[3]*s;
         double wj4 = wj[4]*s;
         double wj5 = wj[5]*s;

         M.m00 += wi[0]*wj0;
         M.m01 += wi[0]*wj1;
         M.m02 += wi[0]*wj2;
         M.m03 += wi[0]*wj3;
         M.m04 += wi[0]*wj4;
         M.m05 += wi[0]*wj5;

         M.m10 += wi[1]*wj0;
         M.m11 += wi[1]*wj1;
         M.m12 += wi[1]*wj2;
         M.m13 += wi[1]*wj3;
         M.m14 += wi[1]*wj4;
         M.m15 += wi[1]*wj5;

         M.m20 += wi[2]*wj0;
         M.m21 += wi[2]*wj1;
         M.m22 += wi[2]*wj2;
         M.m23 += wi[2]*wj3;
         M.m24 += wi[2]*wj4;
         M.m25 += wi[2]*wj5;

         M.m30 += wi[3]*wj0;
         M.m31 += wi[3]*wj1;
         M.m32 += wi[3]*wj2;
         M.m33 += wi[3]*wj3;
         M.m34 += wi[3]*wj4;
         M.m35 += wi[3]*wj5;

         M.m40 += wi[4]*wj0;
         M.m41 += wi[4]*wj1;
         M.m42 += wi[4]*wj2;
         M.m43 += wi[4]*wj3;
         M.m44 += wi[4]*wj4;
         M.m45 += wi[4]*wj5;

         M.m50 += wi[5]*wj0;
         M.m51 += wi[5]*wj1;
         M.m52 += wi[5]*wj2;
         M.m53 += wi[5]*wj3;
         M.m54 += wi[5]*wj4;
         M.m55 += wi[5]*wj5;
      }
      else {
         for (int i=0; i<wi.length; i++) {
            for (int j=0; j<wj.length; j++) {
               blk.set (i, j, blk.get(i, j)+s*wi[i]*wj[j]);
            }
         }
      }
   }

   public void addCoordinateVelJacobian (SparseNumberedBlockMatrix M, int idx, double s) {
      checkCoordinateIndex (idx);
      //s = 0;
      Wrench wrG = myCoupling.getCoordinateWrench (idx);
      if (wrG != null) {
         Wrench wrC = new Wrench();
         wrC.inverseTransform (myTCwG, wrG); // transform to world-aligned TC
         double[] wc = new double[6];
         wrC.get (wc);
         Wrench wrD = new Wrench();
         wrD.inverseTransform (myTDwG, wrG); // transform to world-aligned TD
         double[] wd = new double[6];
         wrD.get (wd);

         MatrixBlock[] Ablks = myAttachmentA.getMasterBlocks();
         double[][] wAc = new double[Ablks.length][];
         for (int i=0; i<Ablks.length; i++) {
            MatrixBlock blk = Ablks[i];
            wAc[i] = new double[blk.rowSize()];
            blk.mulAdd (wAc[i], 0, wc, 0);            
         }
         MatrixBlock[] Bblks = myAttachmentB.getMasterBlocks();
         double[][] wBd = new double[Bblks.length][];
         for (int i=0; i<Bblks.length; i++) {
            MatrixBlock blk = Bblks[i];
            wBd[i] = new double[blk.rowSize()];
            blk.mulAdd (wBd[i], 0, wd, 0);            
         }

         DynamicComponent[] Amasters = myAttachmentA.getMasters();
         DynamicComponent[] Bmasters = myAttachmentB.getMasters();
         for (int i=0; i<Amasters.length; i++) {
            int bi = Amasters[i].getSolveIndex();
            if (bi != -1) {
               for (int j=0; j<Amasters.length; j++) {
                  int bj = Amasters[j].getSolveIndex();
                  if (bj != -1) {
                     addVelJacobian (M.getBlock(bi, bj), wAc[i], wAc[j], s);
                  }
               }
               for (int j=0; j<Bmasters.length; j++) {
                  int bj = Bmasters[j].getSolveIndex();
                  if (bj != -1) {
                     addVelJacobian (M.getBlock(bi, bj), wAc[i], wBd[j], -s);
                  }
               }
            }
         }
         for (int i=0; i<Bmasters.length; i++) {
            int bi = Bmasters[i].getSolveIndex();
            if (bi != -1) {
               for (int j=0; j<Amasters.length; j++) {
                  int bj = Amasters[j].getSolveIndex();
                  if (bj != -1) {
                     MatrixBlock blkAj = Ablks[j];
                     addVelJacobian (M.getBlock(bi, bj), wBd[i], wAc[j], -s);
                  }
               }
               for (int j=0; j<Bmasters.length; j++) {
                  int bj = Bmasters[j].getSolveIndex();
                  if (bj != -1) {
                     addVelJacobian (M.getBlock(bi, bj), wBd[i], wBd[j], s);
                  }
               }
            }
         }
      }
   }
      
   /**
    * Returns the current coordinates, if any, for this joint. Coordinates are
    * returned in {@code coords}, whose size is set to the value returned by
    * {@link #numCoordinates()}. If the joint is connected to other bodies, the
    * coordinates are inferred from the current TCD transform.  Otherwise, they
    * are obtained from stored internal values.
    *
    * @param coords returns the coordinate values
    */
   public void getCoordinates (VectorNd coords) {
      int numc = numCoordinates();
      coords.setSize (numc);
      if (numc > 0) {
         RigidTransform3d TCD = null;
         if (attachmentsInitialized()) {
            // get TCD for estimating coordinates
            TCD = new RigidTransform3d();
            getCurrentTCD (TCD);
         }
         myCoupling.getCoordinates (coords, TCD);
      }
   }

   /**
    * Returns the coordinate values currently stored for this joint.
    * These are the values that have been most recently set or
    * computed, but may be independent of the current TCD transform.
    * Coordinates are returned in {@code coords}, whose size is set to 
    * the value returned by {@link #numCoordinates()}.
    *
    * @param coords returns the coordinate values
    */
   public void getStoredCoordinates (VectorNd coords) {
      int numc = numCoordinates();
      if (numc > 0) {
         coords.setSize (numc);
         myCoupling.getCoordinates (coords, /*TCD=*/null);
      }
   }

   /**
    * Returns the value of the TCD transform based on the coordinate values 
    * currently stored for this joint. This may be different from
    * the value based on the poses of the attached bodies.
    *
    * @param TCD returns the stored TCD transform
    */
   public void getStoredTCD (RigidTransform3d TCD) {
      VectorNd coords = new VectorNd();
      getStoredCoordinates (coords);
      coordinatesToTCD (TCD, coords);
   }

   /**
    * Sets the {@code idx}-th coordinate for this joint. If the joint is
    * connected to other bodies, their poses are adjusted appropriately.
    *
    * @param idx index of the coordinate  
    * @param value new coordinate value
    */
   public void setCoordinate (int idx, double value) {
      int numc = numCoordinates();
      if (idx < 0 || idx >= numc) {
         throw new IndexOutOfBoundsException (
            "idx is "+idx+", must be between 0 and "+(numc-1));
      }
      RigidTransform3d TGD = null;
      if (attachmentsInitialized()) {
         TGD = new RigidTransform3d();
         getCurrentTCD (TGD);
      }
      myCoupling.setCoordinateValue (idx, value, TGD);
      if (TGD != null) {
         // if we are connected to the hierarchy, adjust the poses of the
          // attached bodies appropriately.
         adjustPoses (TGD);
      }        
   }

   /**
    * Sets the {@code idx}-th coordinate for this joint, with the value
    * specified in degrees.
    *
    * @param idx index of the coordinate  
    * @param new coordinate value, in degrees
    */
   public void setCoordinateDeg (int idx, double value) {
      setCoordinate (idx, Math.toRadians(value));
   }
   
   /**
    * Sets the current coordinates, if any, for this joint. If coordinates are
    * not supported, this method does nothing. Otherwise, {@code coords}
    * supplies the coordinates and should have a length {@code >=} {@link
    * #numCoordinates}. If the joint is connected to other bodies, their poses
    * are adjusted appropriately.
    *
    * @param coords new coordinate values
    */
   public void setCoordinates (VectorNd coords) {
      int numc = numCoordinates();
      if (numc > 0) {
         RigidTransform3d TGD = null;
         if (attachmentsInitialized()) {
            TGD = new RigidTransform3d();
         }
         myCoupling.setCoordinateValues (coords, TGD);
         if (TGD != null) {
            // if we are connected to the hierarchy, adjust the poses of the
            // attached bodies appropriately.
            adjustPoses (TGD);
         }
      }        
   }

   /**
    * Computes the TCD transform for a set of coordinates, if coordinates are
    * supported. Otherwise this method does nothing.
    *
    * @param TCD returns the TCD transform
    * @param coords supplies the coordinate values and
    * must have a length &gt;= {@link #numCoordinates}.
    */
   public void coordinatesToTCD (RigidTransform3d TCD, VectorNd coords) {
      myCoupling.coordinatesToTCD (TCD, coords);
   }
   
   public void getPosition(Point3d pos) {
      pos.set(getCurrentTDW().p);
   }

   /**
    * Utility method to help with rendering a shaft along the z axis.
    */
   protected void computeZAxisEndPoints (
      Point3d p0, Point3d p1, double slen, RigidTransform3d TCW) {
      Vector3d uW = new Vector3d(); // joint axis vector in world coords

      // first set p0 to contact center in world coords
      p0.set (TCW.p);
      // now get axis unit vector in world coords
      uW.set (TCW.R.m02, TCW.R.m12, TCW.R.m22);
      p0.scaledAdd (-0.5 * slen, uW, p0);
      p1.scaledAdd (slen, uW, p0);
   }

   /**
    * Utility method to render a shaft along the z axis of a 
    * frame using shaft length and radius.
    * @param TFW TODO
    */
   protected void renderZShaft (Renderer renderer, RigidTransform3d TFW) {
      double slen = getShaftLength();
      if (slen > 0) {
         Point3d p0 = new Point3d();
         Point3d p1 = new Point3d();
         computeZAxisEndPoints (p0, p1, slen, TFW);
         renderer.setFaceColoring (myRenderProps, isSelected());
         double r = getEffectiveShaftRadius();
         renderer.drawCylinder (p0, p1, r, /*capped=*/true);
      }
   }

   /**
    * Utility method to update bounds for the rendered z shaft
    * @param TFW TODO
    */
   protected void updateZShaftBounds (
      Vector3d pmin, Vector3d pmax, RigidTransform3d TFW, double slen) {
      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();
      computeZAxisEndPoints (p0, p1, slen, TFW);
      p0.updateBounds (pmin, pmax);
      p1.updateBounds (pmin, pmax);
   }

}
