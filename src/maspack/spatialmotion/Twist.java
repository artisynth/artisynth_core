/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.*;

/**
 * A spatial vector that represents a spatial velocity, comprised of a
 * translation velocity v and an angular velocity w. A twist is a <a
 * href=SpatialVector#variance>contravariant spatial vector</a>, with v being
 * the free vector and w being the line vector.
 */
public class Twist extends SpatialVector {
   private static final long serialVersionUID = 1L;

   public static final Twist ZERO = new Twist();

   /**
    * Translational velocity
    */
   public Vector3d v;

   /**
    * Rotational (angular) velocity
    */
   public Vector3d w;

   /**
    * Creates a Twist and initializes its contents to zero.
    */
   public Twist() {
      super (/* contravariant= */true);
      v = fv;
      w = lv;
   }

   /**
    * Creates a Twist and initializes its contents to the values of an existing
    * Twist.
    * 
    * @param tw
    * twist whose values are to be copied
    */
   public Twist (Twist tw) {
      this();
      set (tw);
   }

   /**
    * Creates a Twist and initializes its components to the specified values.
    * 
    * @param vx
    * translational velocity x component
    * @param vy
    * translational velocity y component
    * @param vz
    * translational velocity z component
    * @param wx
    * rotational velocity x component
    * @param wy
    * rotational velocity y component
    * @param wz
    * rotational velocity z component
    */
   public Twist (double vx, double vy, double vz,
                 double wx, double wy, double wz) {
      this();
      set (vx, vy, vz, wx, wy, wz);
   }

   /**
    * Creates a Twist and initializes its components to the specified values.
    * 
    * @param v
    * translational velocity
    * @param w
    * rotational velocity
    */
   public Twist (Vector3d v, Vector3d w) {
      this();
      set (v, w);
   }

   /**
    * Sets a rigid spatial transformation to correspond to this Twist.
    * 
    * @param X
    * rigid transform
    * @see #extrapolateTransform
    */
   public void setTransform (RigidTransform3d X) {
      X.p.set (v);
      X.R.setAxisAngle (w.x, w.y, w.z, w.norm());
   }

   /**
    * Extrapolates a rigid spatial transformation by an increment corresponding
    * to this Twist scaled by h. The increment is assumed to be in the
    * coordinate frame of the transform, and so this method is equivalent to
    * 
    * <pre>
    * RigidTransform3d Xinc = new RigidTransform3d();
    * Twist inc = new Twist();
    * inc.scale (h, this);
    * Xinc.setTransform (inc);
    * X.mul (Xinc);
    * </pre>
    * 
    * @param X
    * rigid transform to extrapolate
    * @param h
    * scaling factor used to compute increment from this twist
    * @see #setTransform
    */
   public void extrapolateTransform (RigidTransform3d X, double h) {
      RotationMatrix3d Rinc = new RotationMatrix3d();
      // create rotational displacement
      Rinc.setAxisAngle (w.x, w.y, w.z, h * w.norm());
      Vector3d p = X.p;

      double x = p.x; // save original p to be added back later
      double y = p.y;
      double z = p.z;

      p.scale (h, v); // create translational increment from this twist
      X.R.mul (p, p); // convert to base coordinates ...

      p.x += x; // ... and add back original p
      p.y += y;
      p.z += z;

      X.R.mul (Rinc);
   }

   /**
    * Extrapolates a rigid spatial transformation by an increment corresponding
    * to this Twist scaled by h. The increment is assumed to be in a
    * world-aligned frame that shares an origin with the body frame.
    * 
    * @param X
    * rigid transform to extrapolate
    * @param h
    * scaling factor used to compute increment from this twist
    * @see #setTransform
    */
   public void extrapolateTransformWorld (RigidTransform3d X, double h) {
      RotationMatrix3d Rinc = new RotationMatrix3d();
      // create rotational displacement
      Rinc.setAxisAngle (w.x, w.y, w.z, h * w.norm());

      X.p.x += h*v.x;
      X.p.y += h*v.y;
      X.p.z += h*v.z;

      X.R.mul (Rinc, X.R);
   }

   /**
    * Sets the values of this twist to the specified component values.
    * 
    * @param vx
    * translational velocity x component
    * @param vy
    * translational velocity y component
    * @param vz
    * translational velocity z component
    * @param wx
    * rotational velocity x component
    * @param wy
    * rotational velocity y component
    * @param wz
    * rotational velocity z component
    */
   public void set (
      double vx, double vy, double vz, double wx, double wy, double wz) {
      v.set (vx, vy, vz);
      w.set (wx, wy, wz);
   }

   /**
    * Sets the values of this twist to the specified component values.
    * 
    * @param v
    * translational velocity
    * @param w
    * rotational velocity
    */
   public void set (Vector3d v, Vector3d w) {
      this.v.set (v);
      this.w.set (w);
   }

   /**
    * Sets the values of this twist to those of twist tw.
    * 
    * @param tw
    * twist whose values are to be copied
    */
   public void set (Twist tw) {
      super.set (tw);
   }

   /**
    * Sets this twist to a representation of a rigid spatial transformation. The
    * v field is set to the translation vector, while the w field is set to the
    * rotation axis, scaled by the rotation angle (in radians).
    * 
    * @param X
    * rigid transform
    */
   public void set (RigidTransform3d X) {
      v.set (X.p);
      double ang = X.R.getAxisAngle (w);
      w.scale (ang);
   }

   /**
    * Adds twist tw1 to tw2 and places the result in this twist.
    * 
    * @param tw1
    * left-hand twist
    * @param tw2
    * right-hand twist
    */
   public void add (Twist tw1, Twist tw2) {
      super.add (tw1, tw2);
   }

   /**
    * Adds this twist to tw1 and places the result in this twist.
    * 
    * @param tw1
    * right-hand twist
    */
   public void add (Twist tw1) {
      super.add (tw1);
   }

   /**
    * Subtracts twist tw1 from tw2 and places the result in this twist.
    * 
    * @param tw1
    * left-hand twist
    * @param tw2
    * right-hand twist
    */
   public void sub (Twist tw1, Twist tw2) {
      super.sub (tw1, tw2);
   }

   /**
    * Subtracts tw1 from this twist and places the result in this twist.
    * 
    * @param tw1
    * right-hand twist
    */
   public void sub (Twist tw1) {
      super.sub (tw1);
   }

   /**
    * Sets this twist to the negative of tw1.
    * 
    * @param tw1
    * twist to negate
    */
   public void negate (Twist tw1) {
      super.negate (tw1);
   }

   /**
    * Scales the elements of twist tw1 by s and places the results in this
    * twist.
    * 
    * @param s
    * scaling factor
    * @param tw1
    * twist to be scaled
    */
   public void scale (double s, Twist tw1) {
      super.scale (s, tw1);
   }

   /**
    * Computes the interpolation (1-s) tw1 + s tw2 and places the result in this
    * twist.
    * 
    * @param tw1
    * left-hand twist
    * @param s
    * interpolation factor
    * @param tw2
    * right-hand twist
    */
   public void interpolate (Twist tw1, double s, Twist tw2) {
      super.interpolate (tw1, s, tw2);
   }

   /**
    * Computes the interpolation (1-s) this + s tw1 and places the result in
    * this twist.
    * 
    * @param s
    * interpolation factor
    * @param tw1
    * right-hand twist
    */
   public void interpolate (double s, Twist tw1) {
      super.interpolate (s, tw1);
   }

   /**
    * Computes s tw1 + tw2 and places the result in this twist.
    * 
    * @param s
    * scaling factor
    * @param tw1
    * twist to be scaled
    * @param tw2
    * twist to be added
    */
   public void scaledAdd (double s, Twist tw1, Twist tw2) {
      super.scaledAdd (s, tw1, tw2);
   }

   /**
    * Computes s tw1 and adds the result to this twist.
    * 
    * @param s
    * scaling factor
    * @param tw1
    * twist to be scaled and added
    */
   public void scaledAdd (double s, Twist tw1) {
      super.scaledAdd (s, tw1);
   }

   /**
    * Computes s1 tw1 + s2 tw2 and places the result in this twist.
    * 
    * @param s1
    * left-hand scaling factor
    * @param tw1
    * left-hand twist
    * @param s2
    * right-hand scaling factor
    * @param tw2
    * right-hand twist
    */
   public void combine (double s1, Twist tw1, double s2, Twist tw2) {
      super.combine (s1, tw1, s2, tw2);
   }

   /**
    * Computes a unit twist in the direction of tw1 and places the result in
    * this twist.
    * 
    * @param tw1
    * twist to normalize
    */
   public void normalize (Twist tw1) {
      super.normalize (tw1);
   }

   /**
    * Sets the elements of this twist to their absolute values.
    */
   public void absolute (Twist tw1) {
      super.absolute (tw1);
   }

   /**
    * Applies a rotational transformation to the twist tw1 and stores the result
    * in this twist.
    * 
    * @param R
    * rotational transformation matrix
    * @param tw1
    * twist to transform
    */
   public void transform (RotationMatrix3d R, Twist tw1) {
      super.transform (R, tw1);
   }

   /**
    * Applies an inverse rotational transformation to the twist tw1, and stores
    * the result in this twist.
    * 
    * @param R
    * rotational transformation matrix
    * @param tw1
    * twist to transform
    */
   public void inverseTransform (RotationMatrix3d R, Twist tw1) {
      super.inverseTransform (R, tw1);
   }

   /**
    * Applies a rigid spatial transformation to the twist tw1, and places the
    * result in this twist.
    * 
    * @param X
    * rigid spatial transformation
    * @param tw1
    * twist to be transformed
    */
   public void transform (RigidTransform3d X, Twist tw1) {
      super.transform (X, tw1);
   }

   /**
    * Applies an inverse rigid spatial transformation to the twist tw1, and
    * places the result in this twist.
    * 
    * @param X
    * rigid spatial transformation
    * @param tw1
    * twist to be transformed
    */
   public void inverseTransform (RigidTransform3d X, Twist tw1) {
      super.inverseTransform (X, tw1);
   }

   public Twist clone() {
      Twist tw = (Twist)super.clone();
      tw.v = tw.fv;
      tw.w = tw.lv;
      return tw;
   }

}
