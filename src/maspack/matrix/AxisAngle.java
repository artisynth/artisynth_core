/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.Clonable;
import maspack.util.NumberFormat;

/**
 * Axis-angle representation of a rigid-body rotation. Any spatial rotation can
 * be represented as a rotation about a specific axis by a certain angular
 * amount. An angle of 0 corresponds to the "identity" rotation (i.e., no
 * rotation).
 * 
 * <p>
 * In order to keep the representation unique, the axis is normalized and the
 * angle is kept in the range 0 {@code <=} angle {@code <} Math.PI.
 */
public class AxisAngle implements Clonable {
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double ANGLE_EPSILON = 10 * DOUBLE_PREC;

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
    * Sets this AxisAngle to the prescribed values. The axis values are
    * normalized.
    * 
    * @param values
    * AxisAngle values given as an array. The x, y, and z directions of the
    * rotation axis are given by elements 0 through 2, and the rotation angle
    * (in radians) is given by element 3.
    */
   public void set (double[] values) {
      set (values[0], values[1], values[2], values[3]);
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
      double umag = q.u.norm();

      // angle will be in the range 0 to 2 PI
      angle = 2 * Math.atan2 (umag, q.s);
      if (angle < ANGLE_EPSILON || angle > 2 * Math.PI - ANGLE_EPSILON) {
         angle = 0;
         axis.set (1, 0, 0);
         return;
      }
      else if (angle > Math.PI) {
         angle = 2 * Math.PI - angle;
         axis.scale (-1 / umag, q.u);
      }
      else {
         axis.scale (1 / umag, q.u);
      }
   }

   /**
    * Gets the values associated with this AxisAngle.
    * 
    * @param values
    * returns the AxisAngle values. The x, y, and z directions of the rotation
    * axis are given by elements 0 through 2, and the rotation angle (in
    * radians) is given by element 3.
    */
   public void get (double[] values) {
      values[0] = axis.x;
      values[1] = axis.y;
      values[2] = axis.z;
      values[3] = angle;
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

   public static void main (String[] args) {
      RotationMatrix3d R = new RotationMatrix3d();
      R.setRpy (Math.toRadians(90), 0, Math.toRadians(180));
      System.out.println ("a=" + R.getAxisAngle());
   }
      
}


