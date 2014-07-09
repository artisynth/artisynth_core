/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.*;

/**
 * A spatial vector that represents a spatial force, comprised of a translation
 * force f and a moment m. A wrench is a <a
 * href=SpatialVector#variance>covariant spatial vector</a>, with m being the
 * free vector and f being the line vector.
 */
public class Wrench extends SpatialVector {
   private static final long serialVersionUID = 1L;

   public static final Wrench ZERO = new Wrench();

   /**
    * Translational force
    */
   public Vector3d f;

   /**
    * Moment (rotational force)
    */
   public Vector3d m;

   /**
    * Creates a Wrench and initializes its contents to zero.
    */
   public Wrench() {
      super (/* contravariant= */false);
      m = fv;
      f = lv;
   }

   /**
    * Creates a Wrench and initializes its contents to the values of an existing
    * Wrench.
    * 
    * @param wr
    * wrench whose values are to be copied
    */
   public Wrench (Wrench wr) {
      this();
      set (wr);
   }

   /**
    * Creates a Wrench and initializes its components to the specified values.
    * 
    * @param fx
    * force x component
    * @param fy
    * force y component
    * @param fz
    * force z component
    * @param mx
    * moment x component
    * @param my
    * moment y component
    * @param mz
    * moment z component
    */
   public Wrench (double fx, double fy, double fz, double mx, double my,
   double mz) {
      this();
      set (fx, fy, fz, mx, my, mz);
   }

   /**
    * Creates a Wrench and initializes its components to the specified values.
    * 
    * @param f
    * force
    * @param m
    * moment
    */
   public Wrench (Vector3d f, Vector3d m) {
      this();
      set (f, m);
   }

   /**
    * Sets the values of this wrench to the specified component values.
    * 
    * @param fx
    * force x component
    * @param fy
    * force y component
    * @param fz
    * force z component
    * @param mx
    * moment x component
    * @param my
    * moment y component
    * @param mz
    * moment z component
    */
   public void set (
      double fx, double fy, double fz, double mx, double my, double mz) {
      f.set (fx, fy, fz);
      m.set (mx, my, mz);
   }

   /**
    * Sets the values of this wrench to the specified component values.
    * 
    * @param f
    * force
    * @param m
    * moment
    */
   public void set (Vector3d f, Vector3d m) {
      this.f.set (f);
      this.m.set (m);
   }

   /**
    * Sets the values of this wrench to those of wrench tw.
    * 
    * @param wr
    * wrench whose values are to be copied
    */
   public void set (Wrench wr) {
      super.set (wr);
   }

   /**
    * Adds wrench wr1 to wr2 and places the result in this wrench.
    * 
    * @param wr1
    * left-hand wrench
    * @param wr2
    * right-hand wrench
    */
   public void add (Wrench wr1, Wrench wr2) {
      super.add (wr1, wr2);
   }

   /**
    * Adds this wrench to wr1 and places the result in this wrench.
    * 
    * @param wr1
    * right-hand wrench
    */
   public void add (Wrench wr1) {
      super.add (wr1);
   }

   /**
    * Subtracts wrench wr1 from wr2 and places the result in this wrench.
    * 
    * @param wr1
    * left-hand wrench
    * @param wr2
    * right-hand wrench
    */
   public void sub (Wrench wr1, Wrench wr2) {
      super.sub (wr1, wr2);
   }

   /**
    * Subtracts wr1 from this wrench and places the result in this wrench.
    * 
    * @param wr1
    * right-hand wrench
    */
   public void sub (Wrench wr1) {
      super.sub (wr1);
   }

   /**
    * Sets this wrench to the negative of wr1.
    * 
    * @param wr1
    * wrench to negate
    */
   public void negate (Wrench wr1) {
      super.negate (wr1);
   }

   /**
    * Scales the elements of wrench wr1 by s and places the results in this
    * wrench.
    * 
    * @param s
    * scaling factor
    * @param wr1
    * wrench to be scaled
    */
   public void scale (double s, Wrench wr1) {
      super.scale (s, wr1);
   }

   /**
    * Computes the interpolation (1-s) wr1 + s wr2 and places the result in this
    * wrench.
    * 
    * @param wr1
    * left-hand wrench
    * @param s
    * interpolation factor
    * @param wr2
    * right-hand wrench
    */
   public void interpolate (Wrench wr1, double s, Wrench wr2) {
      super.interpolate (wr1, s, wr2);
   }

   /**
    * Computes the interpolation (1-s) this + s wr1 and places the result in
    * this wrench.
    * 
    * @param s
    * interpolation factor
    * @param wr1
    * right-hand wrench
    */
   public void interpolate (double s, Wrench wr1) {
      super.interpolate (s, wr1);
   }

   /**
    * Computes s wr1 + wr2 and places the result in this wrench.
    * 
    * @param s
    * scaling factor
    * @param wr1
    * wrench to be scaled
    * @param wr2
    * wrench to be added
    */
   public void scaledAdd (double s, Wrench wr1, Wrench wr2) {
      super.scaledAdd (s, wr1, wr2);
   }

   /**
    * Computes s wr1 and add the result to this wrench.
    * 
    * @param s
    * scaling factor
    * @param wr1
    * wrench to be scaled and added
    */
   public void scaledAdd (double s, Wrench wr1) {
      super.scaledAdd (s, wr1);
   }

   /**
    * Computes s1 wr1 + s2 wr2 and places the result in this wrench.
    * 
    * @param s1
    * left-hand scaling factor
    * @param wr1
    * left-hand wrench
    * @param s2
    * right-hand scaling factor
    * @param wr2
    * right-hand wrench
    */
   public void combine (double s1, Wrench wr1, double s2, Wrench wr2) {
      super.combine (s1, wr1, s2, wr2);
   }

   /**
    * Computes a unit wrench in the direction of wr1 and places the result in
    * this wrench.
    * 
    * @param wr1
    * wrench to normalize
    */
   public void normalize (Wrench wr1) {
      super.normalize (wr1);
   }

   /**
    * Sets the elements of this wrench to their absolute values.
    */
   public void absolute (Wrench wr1) {
      super.absolute (wr1);
   }

   /**
    * Applies a rotational transformation to the wrench wr1 and stores the
    * result in this wrench.
    * 
    * @param R
    * rotational transformation matrix
    * @param wr1
    * wrench to transform
    */
   public void transform (RotationMatrix3d R, Wrench wr1) {
      super.transform (R, wr1);
   }

   /**
    * Applies an inverse rotational transformation to the wrench wr1, and stores
    * the result in this wrench.
    * 
    * @param R
    * rotational transformation matrix
    * @param wr1
    * wrench to transform
    */
   public void inverseTransform (RotationMatrix3d R, Wrench wr1) {
      super.inverseTransform (R, wr1);
   }

   /**
    * Applies a rigid spatial transformation to the wrench wr1, and places the
    * result in this wrench.
    * 
    * @param X
    * rigid spatial transformation
    * @param wr1
    * wrench to be transformed
    */
   public void transform (RigidTransform3d X, Wrench wr1) {
      super.transform (X, wr1);
   }

   /**
    * Applies an inverse rigid spatial transformation to the wrench wr1, and
    * places the result in this wrench.
    * 
    * @param X
    * rigid spatial transformation
    * @param wr1
    * wrench to be transformed
    */
   public void inverseTransform (RigidTransform3d X, Wrench wr1) {
      super.inverseTransform (X, wr1);
   }

   public Wrench clone() {
      Wrench wr = (Wrench)super.clone();
      wr.m = wr.fv;
      wr.f = wr.lv;
      return wr;
   }


}
