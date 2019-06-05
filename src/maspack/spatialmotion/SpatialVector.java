/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import java.util.Random;

import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorBase;
import maspack.matrix.Matrix6x1;
import maspack.matrix.Matrix6d;
import maspack.matrix.Matrix1x6;
import maspack.util.NumberFormat;

/**
 * A spatial vector, which is a 6-vector composed of two 3-vectors: a <i>free</i>
 * vector and a <i>line</i> vector. Spatial vectors are described in
 * Featherstone, Robot Dynamics Algorithms, and are used to represent quantities
 * such as spatial velocities (or twists), spatial forces (or wrenchs).
 * 
 * <a name="variance"></a>
 * <p>
 * Spatial vectors are either <i>contarvariant</i> (those associated with
 * motion), or <i>covariant</i> (those associated with forces). For a
 * contravariant spatial vector, elements 0-2 correspond to the free vector and
 * elements 3-5 correspond to the line vector. For a covarient spatial vector,
 * elements 0-2 correspond to the line vector and elements 3-5 correspond to the
 * free vector (note that this transposing of element ordering is different from
 * the treatment in Featherstone, which always asigns elements 0-2 to the line
 * vector).
 */
public abstract class SpatialVector extends VectorBase
   implements java.io.Serializable
{ 
   private static double DOUBLE_PREC = 2.220446049250313e-16;

   protected boolean contravariant;

   protected Vector3d lv; // line vector
   protected Vector3d fv; // free vector
   protected Vector3d a;
   protected Vector3d b;

   protected SpatialVector(boolean contravariant)
    {
      lv = new Vector3d();
      fv = new Vector3d();
      if (contravariant)
       { a = fv;
         b = lv;
       }
      else
       { a = lv;
         b = fv;
       }
      this.contravariant = contravariant;
    }

   /**
    * Returns the size of this spatial vector (which is always 6)
    * 
    * @return 6
    */
   public int size()
    {
      return 6;
    }


   /**
    * Returns true if this spatial vector is contravariant.
    * 
    * @return true for contravariant vectors.
    */
   public boolean isContravariant()
    {
      return contravariant;
    }

   /**
    * Gets a single element of this spatial vector. The element ordering depends
    * on whether the vector is <a href="#variance">contravariant or covariant</a>.
    * 
    * @param i
    * element index
    * @return element value throws ArrayIndexOutOfBoundsException if i is not in
    * the range 0 to 5.
    */
   public double get (int i)
    {
      switch(i)
       { case 0: return a.x; 
         case 1: return a.y;
         case 2: return a.z;
         case 3: return b.x;
         case 4: return b.y;
         case 5: return b.z;
         default:
          { throw new ArrayIndexOutOfBoundsException(i);
          }
       }
    }

   /**
    * Copies the elements of this spatial vector into an array of doubles. The
    * array must have a length {@code >=} 6. The element ordering depends on
    * whether the vector is <a href="#variance">contravariant or covariant</a>.
    * 
    * @param values
    * array into which values are copied
    */
   public void get (double[] values) {
      if (values.length < 6) {
         throw new IllegalArgumentException (
            "argument 'values' must have length >= 6");
      }
      values[0] = a.x;
      values[1] = a.y;
      values[2] = a.z;
      values[3] = b.x;
      values[4] = b.y;
      values[5] = b.z;
    }

   /**
    * Copies the value of this spatial vector into a Matrix6x1.
    * 
    * @param M
    * matrix to be set
    */
   public void get (Matrix6x1 M)
    { 
      M.m00 = a.x;
      M.m10 = a.y;
      M.m20 = a.z;
      M.m30 = b.x;
      M.m40 = b.y;
      M.m50 = b.z;
    }

   /**
    * Copies the value of this spatial vector into a Matrix1x6.
    * 
    * @param M
    * matrix to be set
    */
   public void get (Matrix1x6 M)
    { 
      M.m00 = a.x;
      M.m01 = a.y;
      M.m02 = a.z;
      M.m03 = b.x;
      M.m04 = b.y;
      M.m05 = b.z;
    }

   /**
    * Sets a single element of this spatial vector. The element ordering depends
    * on whether the vector is <a href="#variance">contravariant or covariant</a>.
    * 
    * @param i
    * element index
    * @param value
    * element value
    * @throws ArrayIndexOutOfBoundsException
    * if i is not in the range 0 to 5.
    */
   public void set (int i, double value)
    {
      switch(i)
       { case 0: a.x = value; break;
         case 1: a.y = value; break;
         case 2: a.z = value; break;
         case 3: b.x = value; break;
         case 4: b.y = value; break;
         case 5: b.z = value; break;
         default:
          { throw new ArrayIndexOutOfBoundsException(i);
          }
       }
    }

   /**
    * Sets the elements of this spatial vector from an array of doubles. The
    * array must have a length of at least 6. The 
    * element ordering depends on whether the vector is <a
    * href="#variance">contravariant or covariant</a>.
    * 
    * @param values
    * array from which values are copied
    */
   public void set (double[] values) {
      if (values.length < 6) {
         throw new IllegalArgumentException (
            "argument 'values' must have a length of at least 6");
      } 
      a.x = values[0];
      a.y = values[1];
      a.z = values[2];
      b.x = values[3];
      b.y = values[4];
      b.z = values[5];
    }
   
   /**
    * Sets the elements of this spatial vector from an array of doubles, 
    * starting from a particular location. The array must extend for at least
    * 6 elements beyond that location.
    * 
    * @param values
    * array from which values are copied
    * @param idx starting point within values from which copying should begin
    * @return updated idx value
    */
   public int set (double[] values, int idx) {
      if (values.length < 6+idx) {
         throw new IllegalArgumentException (
            "argument 'values' must extend for at least 6 elements past 'idx'");
      } 
      a.x = values[idx++];
      a.y = values[idx++];
      a.z = values[idx++];
      b.x = values[idx++];
      b.y = values[idx++];
      b.z = values[idx++];
      return idx;
    }

   protected void set (SpatialVector v)
    {
      a.x = v.a.x;
      a.y = v.a.y;
      a.z = v.a.z;
      b.x = v.b.x;
      b.y = v.b.y;
      b.z = v.b.z;
    }

   /**
    * Sets the value of this spatial vector from the contents of a Matrix6x1.
    * 
    * @param M
    * matrix giving new values
    */
   public void set (Matrix6x1 M)
    { 
      a.x = M.m00;
      a.y = M.m10;
      a.z = M.m20;
      b.x = M.m30;
      b.y = M.m40;
      b.z = M.m50;
    }

   protected void add (SpatialVector v1, SpatialVector v2)
    {
      a.add (v1.a, v2.a);
      b.add (v1.b, v2.b);
    }

   protected void add (SpatialVector v1)
    {
      a.add (v1.a);
      b.add (v1.b);
    }

   protected void sub (SpatialVector v1, SpatialVector v2)
    {
      a.sub (v1.a, v2.a);
      b.sub (v1.b, v2.b);
    }

   protected void sub (SpatialVector v1)
    {
      a.sub (v1.a);
      b.sub (v1.b);
    }

   protected void negate (SpatialVector v1)
    {
      a.negate (v1.a);
      b.negate (v1.b);
    }

   /**
    * Negates this spatial vector in place.
    */	
   public void negate()
    {
      a.negate();
      b.negate();
    }

   /**
    * Scales the elements of this spatial vector by <code>s</code>.
    * 
    * @param s
    * scaling factor
    */
   public void scale (double s)
    {
      a.scale (s);
      b.scale (s);
    }

   protected void scale (double s, SpatialVector v1)
    {
      a.scale (s, v1.a);
      b.scale (s, v1.b);
    }

   protected void interpolate (
      SpatialVector v1, double s, SpatialVector v2)
    {
      a.interpolate (v1.a, s, v2.a);
      b.interpolate (v1.b, s, v2.b);
    }

   protected void interpolate (double s, SpatialVector v1)
    {
      a.interpolate (s, v1.a);
      b.interpolate (s, v1.b);
    }

   protected void scaledAdd (double s, SpatialVector v1)
    {
      a.scaledAdd (s, v1.a);
      b.scaledAdd (s, v1.b);
    }

   protected void scaledAdd (double s, SpatialVector v1, SpatialVector v2)
    {
      a.x = s*v1.a.x + v2.a.x;
      a.y = s*v1.a.y + v2.a.y;
      a.z = s*v1.a.z + v2.a.z;

      b.x = s*v1.b.x + v2.b.x;
      b.y = s*v1.b.y + v2.b.y;
      b.z = s*v1.b.z + v2.b.z;
    }

   protected void combine (double s, SpatialVector v1,
                           double r, SpatialVector v2)
    {
      a.combine (s, v1.a, r, v2.a);
      b.combine (s, v1.b, r, v2.b);
    }

   /**
    * Returns the 2 norm of this spatial vector. This is the square root of the
    * sum of the squares of the elements.
    * 
    * @return vector 2 norm
    */
   public double norm()
    { 
      return Math.sqrt(a.normSquared() + b.normSquared());
    }

   /**
    * Returns the square of the 2 norm of this spatial vector. This is the sum
    * of the squares of the elements.
    * 
    * @return square of the 2 norm
    */
   public double normSquared()
    { 
      return a.normSquared() + b.normSquared();
    }

   /**
    * Returns the maximum element value of this spatial vector.
    * 
    * @return maximal element
    */
   public double maxElement()
    {
      return Math.max (a.maxElement(), b.maxElement());
    }

   /**
    * Returns the minimum element value of this spatial vector.
    * 
    * @return minimal element
    */
   public double minElement()
    {
      return Math.min (a.minElement(), b.minElement());
    }

   /**
    * Returns the infinity norm of this spatial vector. This is the maximum
    * absolute value over all elements.
    * 
    * @return vector infinity norm
    */
   public double infinityNorm()
    {
      return Math.max (a.infinityNorm(), b.infinityNorm());
    }

   /**
    * Returns the 1 norm of this spatial vector. This is the sum of the absolute
    * values of the elements.
    * 
    * @return vector 1 norm
    */
   public double oneNorm()
    {
      return a.oneNorm() + b.oneNorm();
    }

   /**
    * Returns the dot product of this spatial vector and the spatial vector v1.
    * 
    * @return dot product
    */
   public double dot (SpatialVector v1)
    {
      return ((a.x * v1.a.x + a.y * v1.a.y + a.z * v1.a.z) +
              (b.x * v1.b.x + b.y * v1.b.y + b.z * v1.b.z));
    }

// /**
// * Returns the dot product of this spatial vector and
// * a Matrix6x1.
// *
// * @param M matrix to take dot product with
// * @return dot product
// */
// public double dot (Matrix6x1 M)
// {
// return ((a.x * M.m00 + a.y * M.m10 + a.z * M.m20) +
// (b.x * M.m30 + b.y * M.m40 + b.z * M.m50));
// }

   /**
    * Normalizes this spatial vector in place.
    */
   public void normalize()
    {	
      double lenSqr = normSquared();
      if (Math.abs (lenSqr-1) > 2*DOUBLE_PREC)
       { double len = Math.sqrt(lenSqr);
         a.scale (1/len);
         b.scale (1/len);
       }
    }

   protected void normalize(SpatialVector v1)
    {
      double lenSqr = v1.normSquared();
      if (Math.abs (lenSqr-1) > 2*DOUBLE_PREC)
       { double len = Math.sqrt(lenSqr);
         a.scale (1/len, v1.a);
         b.scale (1/len, v1.b);
       }
      else
       { a.set (v1.a);
         b.set (v1.b);
       }
    }

   /**
    * Returns true if the elements of this spatial vector equal those of vector
    * <code>v1</code>within a prescribed tolerance <code>epsilon</code>.
    * 
    * @param v1
    * vector to compare with
    * @param eps
    * comparison tolerance
    * @return false if the vectors are not equal within the specified tolerance
    */
   public boolean epsilonEquals (SpatialVector v1, double eps)
    {
      return a.epsilonEquals(v1.a, eps) && b.epsilonEquals(v1.b, eps);
    }

   /**
    * Returns true if the elements of this spatial vector exactly equal those of
    * vector <code>v1</code>.
    * 
    * @param v1
    * vector to compare with
    * @return false if the vectors are not equal
    */
   public boolean equals (SpatialVector v1)
    {
      return a.equals(v1.a) && b.equals(v1.b);
    }

   /**
    * Sets the elements of this spatial vector to zero.
    */
   public void setZero()
    {
      a.setZero();
      b.setZero();
    }

   /**
    * Sets the elements of this spatial vector to their absolute values.
    */
   public void absolute()
    {
      a.absolute();
      b.absolute();
    }

   protected void absolute (SpatialVector v1)
    { a.absolute(v1.a);
      b.absolute(v1.fv);
    }

   public void cross (SpatialVector v1, SpatialVector v2)
    {
      // fv = v1.lv x v2.fv + v1.fv x v2.lv
      // lv = v1.lv x v2.lv

      double x, y, z;

      x = v1.lv.y*v2.fv.z - v1.lv.z*v2.fv.y + v1.fv.y*v2.lv.z - v1.fv.z*v2.lv.y;
      y = v1.lv.z*v2.fv.x - v1.lv.x*v2.fv.z + v1.fv.z*v2.lv.x - v1.fv.x*v2.lv.z;
      z = v1.lv.x*v2.fv.y - v1.lv.y*v2.fv.x + v1.fv.x*v2.lv.y - v1.fv.y*v2.lv.x;
      fv.x = x;
      fv.y = y;
      fv.z = z;
      lv.cross (v1.lv, v2.lv);
    }

   /**
    * Applies a rotational transformation to this spatial vector, in place.
    * 
    * @param R
    * rotational transformation matrix
    */
   public void transform (RotationMatrix3d R)
    {
      lv.transform (R);
      fv.transform (R);
    }

   protected void transform (RotationMatrix3d R, SpatialVector v1)
    {
      lv.transform (R, v1.lv);
      fv.transform (R, v1.fv);
    }

   /**
    * Applies an inverse rotational transformation to this spatial vector, in
    * place.
    * 
    * @param R
    * rotational transformation matrix
    */
   public void inverseTransform (RotationMatrix3d R)
    {
      lv.inverseTransform (R);
      fv.inverseTransform (R);
    }

   protected void inverseTransform (RotationMatrix3d R, SpatialVector v1)
    {
      lv.inverseTransform (R, v1.lv);
      fv.inverseTransform (R, v1.fv);
    }

   private void crossAndAdd (
      Vector3d vr, Vector3d v1, Vector3d v2, Vector3d v3)
    { 
      double x = v1.y*v2.z - v1.z*v2.y + v3.x;
      double y = v1.z*v2.x - v1.x*v2.z + v3.y;
      double z = v1.x*v2.y - v1.y*v2.x + v3.z;
      vr.x = x;
      vr.y = y;
      vr.z = z;
    }
	
   /**
    * Applies a rigid spatial transformation to this spatial vector, in place.
    * 
    * @param X
    * rigid spatial transformation
    */
   public void transform (RigidTransform3d X)
    {
      transform (X, this);
    }

   /*
    * [ R p ] If X = [ 0 1 ], and this spatial vector is arranged according to
    * its line and free vector components as (lv,fv), then it is converted into
    * the new frame via
    *  [ lv ]' [ R 0 ] [ lv ] [ ] = [ ] [ ] [ fv ] [ [p] R R ] [ fv ]
    */
   protected void transform (RigidTransform3d X, SpatialVector v1)
    {
      lv.transform (X.R, v1.lv);
      fv.transform (X.R, v1.fv);
      crossAndAdd (fv, X.p, lv, fv);
    }

   /**
    * Applies an inverse rigid spatial transformation to this vector, in place.
    * 
    * @param X
    * rigid spatial transformation
    */
   public void inverseTransform (RigidTransform3d X)
    {
      inverseTransform (X, this);
    }

   /*
    * [ R p ] If X = [ 0 1 ], and the spatial vectors are arranged according to
    * their line and free vector components as (lv, fv), then v1 is converted
    * into the new frame via T [ lv ]' [ R 0 ] [ v1.lv ] [ ] [ ] [ ] [ ] = [ T T ] [ ] [
    * fv ] [ -R [p] R ] [ v1.fv ]
    */
   protected void inverseTransform (RigidTransform3d X, SpatialVector v1)
    {
      crossAndAdd (fv, v1.lv, X.p, v1.fv);
      fv.inverseTransform (X.R, fv);
      lv.inverseTransform (X.R, v1.lv);
    }


   /**
    * {@inheritDoc}
    */
   public String toString (NumberFormat fmt)
    {
      return a.toString(fmt) + " " + b.toString(fmt);
    }


   /**
    * {@inheritDoc}
    */
   public void setRandom()
    {
      super.setRandom();
    }

   /**
    * {@inheritDoc}
    */
   public void setRandom (double lower, double upper)
    {
      super.setRandom (lower, upper);
    }

   /**
    * {@inheritDoc}
    */
   public void setRandom (double lower, double upper, Random generator)
    {
      super.setRandom (lower, upper, generator);
    }

   public SpatialVector clone() {
      SpatialVector sv = (SpatialVector)super.clone();

      sv.lv = new Vector3d(lv);
      sv.fv = new Vector3d(fv);
      if (contravariant)
       { sv.a = sv.fv;
         sv.b = sv.lv;
       }
      else
       { sv.a = sv.lv;
         sv.b = sv.fv;
       }
      sv.contravariant = contravariant;
      return sv;
   }      

   /**
    * Multiplies a spatial vector v1 by a Matrix6d and places the result
    * in this spetial vector.
    *
    * @param M matrix to multiply by
    * @param v1 vector to multiply
    */
   public void mul (Matrix6d M, SpatialVector v1) {
      double ax = v1.a.x;
      double ay = v1.a.y;
      double az = v1.a.z;
      double bx = v1.b.x;
      double by = v1.b.y;
      double bz = v1.b.z;

      a.x = M.m00*ax + M.m01*ay + M.m02*az + M.m03*bx + M.m04*by + M.m05*bz;
      a.y = M.m10*ax + M.m11*ay + M.m12*az + M.m13*bx + M.m14*by + M.m15*bz;
      a.z = M.m20*ax + M.m21*ay + M.m22*az + M.m23*bx + M.m24*by + M.m25*bz;

      b.x = M.m30*ax + M.m31*ay + M.m32*az + M.m33*bx + M.m34*by + M.m35*bz;
      b.y = M.m40*ax + M.m41*ay + M.m42*az + M.m43*bx + M.m44*by + M.m45*bz;
      b.z = M.m50*ax + M.m51*ay + M.m52*az + M.m53*bx + M.m54*by + M.m55*bz;
   }

}
