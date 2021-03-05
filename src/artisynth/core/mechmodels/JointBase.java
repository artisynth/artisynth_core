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
import maspack.util.DoubleInterval;
import maspack.properties.*;
import maspack.render.*;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.Frame.AxisDrawStyle;

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

   public PropertyList getAllPropertyInfo() {
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

   /**
    * Returns the {@code idx}-th coordinate for this joint. If the joint is
    * connected to other bodies, the value is inferred from the current TCD
    * transform.  Otherwise, it is obtained from a stored internal value.
    *
    * @param idx index of the coordinate
    * @return the coordinate value
    */
   public double getCoordinate (int idx) {
      int numc = numCoordinates();
      if (idx < 0 || idx >= numc) {
         throw new IndexOutOfBoundsException (
            "idx is "+idx+", must be between 0 and "+(numc-1));
      }
      RigidTransform3d TCD = null;
      if (attachmentsInitialized()) {
         // get TCD for estimating coordinates
         TCD = new RigidTransform3d();
         getCurrentTCD (TCD);
      }
      return myCoupling.getCoordinate (idx, TCD);
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
    * Sets the current coordinates, if any, for this joint. If coordinates are
    * not supported, this method does nothing. Otherwise, {@code coords}
    * supplies the coordinates and should have a length &gt;= {@link
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
