/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.*;

/**
 * Axis-angle representation of a rigid-body rotation. Any spatial rotation can
 * be represented as a rotation about a specific axis by a certain angular
 * amount. An angle of 0 corresponds to the "identity" rotation (i.e., no
 * rotation).
 * 
 * <p>The {@code axis} value is normalized. However, the representation is
 * still not unique: negating both {@code axis} and {@code angle}, or adding
 * 2*PI to {@code angle}, results in the same rotation.
 */
public class AxisAngle implements Clonable {

   static final boolean maxQuaternionAnglePi = true;

   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double ANGLE_EPSILON = 10 * DOUBLE_PREC;

   private static final double RTOD = 180/Math.PI;
   private static final double DTOR = Math.PI/180;

   public static final AxisAngle IDENTITY = new AxisAngle();
   public static final AxisAngle ZERO = new AxisAngle(0,0,0,0);

   public static final AxisAngle ROT_X_90 = new AxisAngle(1, 0, 0, Math.toRadians(90));
   public static final AxisAngle ROT_Y_90 = new AxisAngle(0, 1, 0, Math.toRadians(90));
   public static final AxisAngle ROT_Z_90 = new AxisAngle(0, 0, 1, Math.toRadians(90));

   /**
    * Rotation axis.
    */
   public Vector3d axis;

   /**
    * Angle of rotation about the axis, in radians.
    */
   public double angle;

   /**
    * Creates an AxisAngle and initializes it to the identity rotation.
    */
   public AxisAngle() {
      this.axis = new Vector3d (1, 0, 0);
      this.angle = 0;
   }

   /**
    * Creates an AxisAngle and initializes it to the prescribed values. The axis
    * values are normalized.
    * 
    * @param values
    * AxisAngle values given as an array. The x, y, and z directions of the
    * rotation axis are given by elements 0 through 2, and the rotation angle
    * (in radians) is given by element 3.
    */
   public AxisAngle (double[] values) {
      this();
      set (values);
   }

   /**
    * Creates an AxisAngle and initializes it to the prescribed values. The axis
    * values are normalized.
    * 
    * @param x
    * rotation axis x direction
    * @param y
    * rotation axis y direction
    * @param z
    * rotation axis z direction
    * @param ang
    * rotation angle, in radians
    */
   public AxisAngle (double x, double y, double z, double ang) {
      this();
      set (x, y, z, ang);
   }

   /**
    * Creates an AxisAngle and sets it to the prescribed values. The axis values
    * are normalixed.
    * 
    * @param axis
    * rotation axis
    * @param ang
    * rotation angle (in radians)
    */
   public AxisAngle (Vector3d axis, double ang) {
      this();
      set (axis, ang);
   }

   /**
    * Creates an AxisAngle and initializes it from an existing AxisAngle.
    * 
    * @param axisAng
    * AxisAngle to supply initial values
    */
   public AxisAngle (AxisAngle axisAng) {
      this();
      set (axisAng);
   }

   /**
    * Creates an AxisAngle initialized to the specified rotation
    * matrix.
    * 
    * @param R
    * rotation matrix
    */
   public AxisAngle (RotationMatrix3d R) {
      this();
      set (R);
   }

   /**
    * Sets this AxisAngle to the prescribed values. The axis values are
    * normalized.
    * 
    * @param x
    * rotation axis x direction
    * @param y
    * rotation axis y direction
    * @param z
    * rotation axis z direction
    * @param ang
    * rotation angle, in radians
    */
   public void set (double x, double y, double z, double ang) {
      axis.set (x, y, z);
      axis.normalize();
      this.angle = ang;
   }

   /**
    * Sets this AxisAngle to the prescribed values. The x, y, and z directions
    * of the rotation axis are given by the first three values, and the
    * rotation angle (in radians) is given by the fourth value. The axis values
    * will be normalized.
    * 
    * @param vals
    * AxisAngle values given as an array
    * @param off offset within {@code vals} where the values begin
    */
   public void set (double[] vals, int off) {
      set (vals[off+0], vals[off+1], vals[off+2], vals[off+3]);
   }

   /**
    * Sets this AxisAngle to the prescribed values. The x, y, and z directions
    * of the rotation axis are given by the first three values, and the
    * rotation angle (in radians) is given by the fourth value. The axis values
    * will be normalized.
    * 
    * @param vals
    * AxisAngle values given as an array
     */
   public void set (double[] vals) {
      set (vals[0], vals[1], vals[2], vals[3]);
   }

   /**
    * Sets this AxisAngle to the prescribed values. The axis values are
    * normalixed.
    * 
    * @param axis
    * rotation axis
    * @param ang
    * rotation angle (in radians)
    */
   public void set (Vector3d axis, double ang) {
      this.axis.set (axis);
      this.axis.normalize();
      this.angle = ang;
   }

   /**
    * Sets this AxisAngle to the values of another AxisAngle.
    * 
    * @param axisAng
    * AxisAngle supplying new values
    */
   public void set (AxisAngle axisAng) {
      this.axis.set (axisAng.axis);
      this.angle = axisAng.angle;
   }

   /**
    * Sets this AxisAngle to the values appropriate for the specified rotation
    * matrix.
    * 
    * @param R
    * rotation matrix
    */
   public void set (RotationMatrix3d R) {
      angle = R.getAxisAngle (axis);
   }

   /**
    * Sets this AxisAngle to the values appropriate for the specified
    * quaternion.
    * 
    * @param q
    * quaternion
    */
   public void set (Quaternion q) {
      double mag = q.u.norm();
      if (mag == 0) {
         angle = 0;
         axis.set (1, 0, 0);
      }
      else {
         angle = 2*Math.atan2 (mag, q.s); 
         if (maxQuaternionAnglePi && angle > Math.PI) {
            angle -= 2*Math.PI;
         }
         axis.scale (1/mag, q.u);
      }
   }

   /**
    * Gets the quaternion associated with this axis angle.
    * 
    * @param q 
    * return the quaternion
    */
   public void get (Quaternion q) {
      double sin = Math.sin (angle/2);
      double cos = Math.cos (angle/2);

      q.s = cos;
      q.u.scale (sin, axis);
      if (maxQuaternionAnglePi && angle < 0) {
         q.negate();
      }
   }

   /**
    * Gets the values associated with this AxisAngle. See {@link
    * #set(double[])} for a description of the values.
    * 
    * @param vals
    * returns the AxisAngle values
    * @param off
    * offset within {@code vals} where the values are stored
    */
   public void get (double[] vals, int off) {
      vals[off++] = axis.x;
      vals[off++] = axis.y;
      vals[off++] = axis.z;
      vals[off] = angle;
   }

   /**
    * Copies the elements of this AxisAngle into an array of doubles. The
    * array must have a length {@code >= 4 + off}. The method accepts
    * an optional argument {@code refs} which specifies a reference
    * AxisAngle. If the negative of this AxisAngle is closer distance-wise
    * to the reference, then the negative values are copied instead.
    * 
    * @param vals array into which values are copied
    * @param refs if non-null, specifies values of a reference AxisAngle
    * @param off offset within {@code vals} and {@code refs} where values
    * are stored
    * @param angScale factor by which the output and reference angles are scaled
    */
   public void get (double[] vals, double[] refs, int off, double angScale) {
      if (vals.length < 4+off) {
         throw new IllegalArgumentException (
            "argument 'values' must have length >= 4+off");
      }
      boolean negate = false;
      if (refs != null) {
         AxisAngle ref =
            new AxisAngle (refs[off+0], refs[off+1], refs[off+2], refs[off+3]);
         if (angScale != 1.0) {
            ref.angle /= angScale;
         }
         double dsqrPos = ref.numericDistanceSquared (axis, angle);
         ref.negate();
         double dsqrNeg = ref.numericDistanceSquared (axis, angle);
         if (dsqrPos > dsqrNeg) {
            negate = true;
         }
      }
      if (negate) {
         vals[off+0] = -axis.x;
         vals[off+1] = -axis.y;
         vals[off+2] = -axis.z;
         vals[off+3] = -angScale*angle;
      }
      else {
         vals[off+0] = axis.x;
         vals[off+1] = axis.y;
         vals[off+2] = axis.z;
         vals[off+3] = angScale*angle;
      }
   }
   
   /**
    * Gets the values associated with this AxisAngle. See {@link
    * #set(double[])} for a description of the values.
    * 
    * @param vals
    * returns the AxisAngle values
    */
   public void get (double[] vals) {
      vals[0] = axis.x;
      vals[1] = axis.y;
      vals[2] = axis.z;
      vals[3] = angle;
   }
   
   /** 
    * Transforms a vector v1 by the rotation implied by this axis-angle
    * using Rodriguez's rotation formula
    * 
    * @param vr result vector
    * @param v1 vector to be transformed
    */
   public void transform(Vector3d vr, Vector3d v1) {
      // rotate location
      double cosa = Math.cos (angle);
      double sina = Math.sin (angle);
      Vector3d tmp = new Vector3d();
      tmp.cross (axis, v1);
      tmp.scale (sina);
      tmp.scaledAdd (cosa, v1);
      tmp.scaledAdd ((1-cosa)*axis.dot (v1), axis);
      vr.set (tmp);
   }

   /** 
    * Transforms a vector v1 by the rotation implied by the
    * inverse of this axis-angle using Rodriguez's rotation formula
    * 
    * @param vr result vector
    * @param v1 vector to be transformed
    */
   public void inverseTransform(Vector3d vr, Vector3d v1) {
      // rotate location
      double cosa = Math.cos (angle);
      double sina = -Math.sin (angle);
      Vector3d tmp = new Vector3d();
      tmp.cross (axis, v1);
      tmp.scale (sina);
      tmp.scaledAdd (cosa, v1);
      tmp.scaledAdd ((1-cosa)*axis.dot (v1), axis);
      vr.set (tmp);
   }
   
   /**
    * Returns a String representation of this AxisAngle, consisting of the x, y,
    * and z components of the axis, followed by the angle (in radians).
    * 
    * @return String representation
    */
   public String toString() {
      return toString ("%g");
   }

   /**
    * Returns a String representation of this AxisAngle, consisting of the x, y,
    * and z components of the axis, followed by the angle (in radians). Each
    * element is formatted using a C <code>printf</code> style format string.
    * For a description of the format string syntax, see
    * {@link maspack.util.NumberFormat NumberFormat}.
    * 
    * @param fmtStr
    * numeric format specification
    * @return String representation of this vector
    */
   public String toString (String fmtStr) {
      return toString (new NumberFormat (fmtStr));
   }

   /**
    * Returns a String representation of this AxisAngle, consisting of the x, y,
    * and z components of the axis, followed by the angle (in radians). Each
    * element is formatted using a C <code>printf</code> style as decribed by
    * the parameter <code>NumberFormat</code>. When called numerous times,
    * this routine can be more efficient than
    * {@link #toString(String) toString(String)}, because the {@link
    * maspack.util.NumberFormat NumberFormat} does not need to be recreated each
    * time from a specification string.
    * 
    * @param fmt
    * numeric format
    * @return String representation of this vector
    */
   public String toString (NumberFormat fmt) {
      StringBuffer buf = new StringBuffer (80);
      buf.append (fmt.format (axis.x));
      buf.append (' ');
      buf.append (fmt.format (axis.y));
      buf.append (' ');
      buf.append (fmt.format (axis.z));
      buf.append (' ');
      buf.append (fmt.format (angle));

      return buf.toString();
   }

   /**
    * Returns true if the elements of this AxisAngle equal those of another
    * AxisAngle within a prescribed tolerance <code>epsilon</code>.
    * 
    * @param axisAng
    * AxisAngle to compare with
    * @param eps
    * comparison tolerance
    * @return false if the AxisAngles are not equal within the specified
    * tolerance
    */
   public boolean epsilonEquals (AxisAngle axisAng, double eps)
      throws ImproperSizeException {
      if (!axis.epsilonEquals (axisAng.axis, eps)) {
         return false;
      }
      double dist = Math.abs (angle - axisAng.angle);
      return (dist <= eps);
   }

   /**
    * Negates the axis and angle of this AxisAngle.
    */
   public void negate() {
      angle = -angle;
      axis.negate();
   }

   /**
       * Returns true if the elements of this AxisAngle equal those of another
       * AxisAngle exactly.
       * 
       * @param axisAng
       * AxisAngle to compare with
       * @return false if the AxisAngles are not equal
       */
      public boolean equals (AxisAngle axisAng) {
      if (!axis.equals (axisAng.axis)) {
         return false;
      }
      return (angle == axisAng.angle);
   }

   /**
    * Returns true if the supplied object is an AxisAngle and its elements equal
    * those of this AxisAngle exactly.
    * 
    * @param obj
    * Object to compare with
    * @return false if the Object does not equal this AxisAngle
    */
   public boolean equals (Object obj) {
      if (obj instanceof AxisAngle) {
         return equals ((AxisAngle)obj);
      }
      else {
         return false;
      }
   }

   @Override
   public AxisAngle clone ()  {
      AxisAngle aa = null;
      
      try {
         aa = (AxisAngle)super.clone ();
         aa.axis = axis.clone ();  // separate copy of axis
         aa.angle = angle;
      } catch (CloneNotSupportedException e) {}
      
      return aa;
   }

   /**
    * Sets this AxisAngle from a set of doubles implementing a given rotation
    * representation.
    *
    * @param vals values specifying the rotation
    * @param off offset within vals where rotation info starts
    * @param rotRep rotation representation
    * @param s factor by which values are scaled
    */
   public void set (double[] vals, int off, RotationRep rotRep, double s) {
      double si = 1/s;
      switch (rotRep) {
         case ZYX_DEG: {
            si *= DTOR;
            // no break
         }
         case ZYX: {
            RotationMatrix3d R = new RotationMatrix3d();
            R.setRpy (si*vals[off+0], si*vals[off+1], si*vals[off+2]);
            set (R);
            break;
         }
         case XYZ_DEG: {
            si *= DTOR;
            // no break
         }
         case XYZ: {
            RotationMatrix3d R = new RotationMatrix3d();
            R.setXyz (si*vals[off+0], si*vals[off+1], si*vals[off+2]);
            set (R);
            break;
         }
         case AXIS_ANGLE: {
            set (si*vals[off+0], si*vals[off+1], 
                 si*vals[off+2], si*vals[off+3]);
            break;
         }
         case AXIS_ANGLE_DEG: {
            set (si*vals[off+0], si*vals[off+1], 
                 si*vals[off+2], DTOR*si*vals[off+3]);
            break;
         }
         case QUATERNION: {
            set (new Quaternion (
               si*vals[off+0], si*vals[off+1], si*vals[off+2], si*vals[off+3]));
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Unimplemented rotation represention " + this);
         }
      }
   }

   /**
    * Get a specified rotation representation from this AxisAngle.
    *
    * @param vals returns the values specifying the rotation
    * @param refs if non-null, specifies a reference set of values to
    * be used to resolve non-unique solutions
    * @param off offset within vals where rotation info starts
    * @param rotRep rotation representation
    * @param s factor by which values are scaled
    */
   public void get (double[] vals, double[] refs, int off, 
      RotationRep rotRep, double s) {
      switch (rotRep) {
         case ZYX_DEG: {
            s *= RTOD;
            // no break
         }
         case ZYX: {
            RotationMatrix3d R = new RotationMatrix3d(this);
            R.getZyxAngles (vals, refs, off, s);
            break;
         }
         case XYZ_DEG: {
            s *= RTOD;
            // no break
         }
         case XYZ: {
            RotationMatrix3d R = new RotationMatrix3d(this);
            R.getXyzAngles (vals, refs, off, s);
            break;
         }
         case AXIS_ANGLE_DEG: {
            s *= RTOD;
            // no break
         }
         case AXIS_ANGLE: {
            get (vals, refs, off, s);
            break;
         }
         case QUATERNION: {
            Quaternion q = new Quaternion(this);
            q.get (vals, refs, off, s);
            if (refs != null) {
               Quaternion qref = new Quaternion();
               AxisAngle aref = new AxisAngle();
               qref.set (refs);
               qref.getAxisAngle (aref);
               q.setAxisAngle (aref);
            }
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Unimplemented rotation representation " + this);
         }
      }
   }

   /**
    * Returns the square of the distance between this AxisAngle and a second
    * specified by {@code axis} and {@code ang}, where <i>distance</i> is
    * described in the docs for {@link #numericDistance}.
    *
    * @param axis axis of the AxisAngle to obtain distance squared to
    * @param ang angle of AxisAngle to obtain distance squared to
    * @return distance squared to the specified AxisAngle
    */
   public double numericDistanceSquared (Vector3d axis, double ang) {
      double dang = RotationRep.canonicalAngle (ang-angle);
      Vector3d daxis = new Vector3d();
      daxis.sub (axis, this.axis);
      return dang*dang + daxis.normSquared();
   }

   /**
    * Returns the distance between this AxisAngle and a second specified by
    * {@code axis} and {@code ang}. Distance here refers not to angular
    * distance between the two orientations, but distance between the
    * representations themselves:
    * <pre>
    * d = sqrt( deltaAngle^2 + ||deltaAxis||^2 )
    * </pre>
    * where deltaAngle is the difference between the two angles (reduced to the
    * range {@code (-PI,PI]}, and deltaAxis is the difference between the two
    * axes. In particular, an AxisAngle and its negative have maximal distance
    * between each other, even though both describe the same orientation.
    *
    * @param axis axis of the AxisAngle to obtain distance to
    * @param ang angle of AxisAngle to obtain distance to
    * @return distance to the specified AxisAngle
    */
   public double numericDistance (Vector3d axis, double ang) {
      return Math.sqrt (numericDistanceSquared (axis, ang));
   }

   /**
    * Sets this AxisAngle to a random one.
    */
   public void setRandom() {
      axis.setRandom();
      axis.normalize();
      angle = RandomGenerator.nextDouble (-Math.PI, Math.PI);
   }     
}
