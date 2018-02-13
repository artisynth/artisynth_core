/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.*;

import java.io.*;

/**
 * Implements a plane in three space, as characterized by the points x which
 * satisfy
 * 
 * <pre>
 *  T
 * n  x - d = 0
 * </pre>
 * 
 * where n is the plane normal and d is the offset.
 * 
 * @author John Lloyd
 */
public class Plane implements java.io.Serializable {
   private static final long serialVersionUID = 1L;
   private static final double EPS = 1e-12;

   /**
    * Plane normal
    */
   public Vector3d normal;

   /**
    * Plane offset (distance of the origin from the plane)
    */
   public double offset;

   /**
    * Creates a plane initialized to the x-y plane.
    */
   public Plane() {
      normal = new Vector3d (0, 0, 1);
      offset = 0;
   }

   /**
    * Creates a plane initialized to another plane.
    * 
    * @param p
    * plane to copy
    */
   public Plane (Plane p) {
      this();
      set (p);
   }

   /**
    * Creates a plane which passes through a particular point with a specified
    * normal direction.
    * 
    * @param n
    * normal direction
    * @param p
    * point to pass through
    */
   public Plane (Vector3d n, Vector3d p) {
      this();
      set (n, p);
   }

   /**
    * Creates a plane from normal components and the an offset value.
    * 
    * @param nx
    * normal x component
    * @param ny
    * normal y component
    * @param nz
    * normal z component
    * @param off
    * offset value
    */
   public Plane (double nx, double ny, double nz, double off) {
      this();
      set (nx, ny, nz, off);
   }

   /**
    * Creates a plane with a specified normal and offset.
    * 
    * @param n
    * normal direction
    * @param d
    * offset (equal to the dot product of the normal and every point on the
    * plane).
    */
   public Plane (Vector3d n, double d) {
      this();
      set (n, d);
   }

   /**
    * Constructs a plane given three non-colinear points arranged
    * counter-clockwise around the normal.
    * 
    * @param p1
    * first point
    * @param p2
    * second point
    * @param p3
    * third point
    * @throws IllegalArgumentException
    * if the points are colinear
    */
   public Plane (Point3d p1, Point3d p2, Point3d p3)
   throws IllegalArgumentException {
      this();
      set (p1, p2, p3);
   }

   /**
    * Sets this plane to the values of another plane.
    * 
    * @param p
    * plane to copy
    */
   public void set (Plane p) {
      normal.set (p.normal);
      offset = p.offset;
   }

   /**
    * Sets the plane to the indicated normal direction and offset. The normal
    * will normalized.
    * 
    * @param n
    * normal direction
    * @param d
    * offset
    */
   public void set (Vector3d n, double d) {
      this.normal.set (n);
      this.normal.normalize();
      this.offset = d;
   }

   /**
    * Sets the plane to the indicated normal direction and offset. The normal
    * will normalized.
    * 
    * @param nx
    * x component of normal direction
    * @param ny
    * y component of normal direction
    * @param nz
    * z component of normal direction
    * @param d
    * offset
    */
   public void set (double nx, double ny, double nz, double d) {
      this.normal.set (nx, ny, nz);
      this.normal.normalize();
      this.offset = d;
   }

   /**
    * Sets this plane to pass through a particular point with a specified normal
    * direction.
    * 
    * @param n
    * normal direction
    * @param p
    * point to pass through
    */
   public void set (Vector3d n, Vector3d p) {
      normal.normalize (n);
      offset = p.dot (normal);
   }

   /**
    * Sets this plane to the x-y plane defined by a RigidTransformation. The
    * normal is the transformation's z axis, while the point is the
    * transformation's origin.
    * 
    * @param X
    * transformation defining the plane
    */
   public void set (RigidTransform3d X) {
      double nx = X.R.m02;
      double ny = X.R.m12;
      double nz = X.R.m22;
      normal.set (nx, ny, nz);
      offset = X.p.x * nx + X.p.y * ny + X.p.z * nz;
   }

   /**
    * Sets this plane to pass through three non-colinear points arranged
    * counter-clockwise around the normal.
    * 
    * @param p1
    * first point
    * @param p2
    * second point
    * @param p3
    * third point
    * @throws IllegalArgumentException
    * if the points are colinear
    */
   public void set (Point3d p1, Point3d p2, Point3d p3)
      throws IllegalArgumentException {
      double d1x = p2.x - p1.x;
      double d1y = p2.y - p1.y;
      double d1z = p2.z - p1.z;

      double d2x = p3.x - p1.x;
      double d2y = p3.y - p1.y;
      double d2z = p3.z - p1.z;

      normal.x = d1y * d2z - d1z * d2y;
      normal.y = d1z * d2x - d1x * d2z;
      normal.z = d1x * d2y - d1y * d2x;
      double mag = normal.norm();
      if (mag == 0) {
         throw new IllegalArgumentException ("colinear points");
      }
      normal.scale (1 / mag);
      // compute offset as the average offset for all three points;
      // should be marginally more robust
      offset  = normal.dot (p1);
      offset += normal.dot (p2);
      offset += normal.dot (p3);
      offset /= 3;
   }
   
   public void flip() {
      normal.negate();
      offset = -offset;
   }

   /**
    * Returns the signed distance between a point and this plane. The distance
    * is positive if the point is on the same side as the normal.
    * 
    * @param p
    * point to compute distance for
    * @return signed distance to plane
    */
   public double distance (Vector3d p) {
      return normal.dot (p) - offset;
   }

   /**
    * Projects a point onto this plane, and returns the signed distance of the
    * original point from the plane.
    * 
    * @param pr
    * projected point
    * @param p1
    * point to project
    * @return signed distance of p1
    */
   public double project (Point3d pr, Point3d p1) {
      double dist = distance (p1);
      pr.scaledAdd (-dist, normal, p1);
      return dist;
   }
   
   /**
    * Projects a vector onto this plane, by removing any component
    * that is perpendicular to it.
    * 
    * @param vr returns the projected vector
    * @param v1 vector to project
    */
   public void projectVector (Vector3d vr, Vector3d v1) {
      vr.scaledAdd (-v1.dot(normal), normal, v1);
   }
   
   /**
    * Reflects a point about this plane.
    * 
    * @param pr
    * reflected point
    * @param p1
    * point to reflect
    */
   public void reflect (Point3d pr, Point3d p1) {
      double dist = distance (p1);
      pr.scaledAdd (-2*dist, normal, p1);
   }

   /**
    * Intersects this plane with a directed ray.
    * 
    * @param isect
    * intersection point
    * @param dir
    * direction of the ray
    * @param origin
    * origin of the ray
    * @return false if the ray is pointing away from the plane
    */
   public boolean intersectRay (Point3d isect, Vector3d dir, Vector3d origin) {
      double dot = normal.dot (dir);
      double dist = distance (origin);
      if (dot * dist >= 0) {
         return false;
      }
      isect.scaledAdd (-dist / dot, dir, origin);
      return true;
   }

   /**
    * Intersects this plane with a line. If the line and plane are parallel,
    * then postive infinity is returned.
    * 
    * @param isect
    * intersection point (optional)
    * @param dir
    * vector in the direction of the line
    * @param pnt
    * point lying on the line
    * @return intersection parameter along dir, or +infinity if there is no
    * intersection
    */
   public double intersectLine (Point3d isect, Vector3d dir, Vector3d pnt) {
      double dot = normal.dot (dir);
      double dist = distance (pnt);
      if (dot == 0) {
         return Double.POSITIVE_INFINITY;
      }
      double s = -dist / dot;
      if (s == Double.POSITIVE_INFINITY || s == Double.NEGATIVE_INFINITY) {
         return Double.POSITIVE_INFINITY;
      }
      if (isect != null) {
         isect.scaledAdd (s, dir, pnt);
      }
      return s;
   }

   /**
    * Intersects this plane with another plane. The result is returned as a
    * point and direction vector describing the resulting line. The point will
    * be the one nearest to the origin. If the planes are parallel, this method
    * returns <code>false</code> and the point and direction are undefined. In
    * that case the offsets should be compared to see if the planes are
    * coincident.
    *
    * @param pnt returns a point on the intersection line.
    * @param dir returns a unit vector in the direction of the intersection line.
    * @param plane plane to intersect
    * @return <code>true</code> if the planes intersect in a line
    * or <code>false</code> if they are parallel.
    */
   public boolean intersectPlane (Point3d pnt, Vector3d dir, Plane plane) {
      dir.cross (normal, plane.normal);
      double mag = dir.norm();
      if (mag < EPS) {
         // assume planes are parallel
         return false;
      }
      else {
         dir.scale (1/mag);
         // compute pnt from A^T (A A^T)^{-1} b, where
         //
         //     [ n1^T ]      [ d1 ]
         // A = [      ]  b = [    ]
         //     [ n2^T ]      [ d2 ]
         //
         // and n1 and n2 are the normals for the two planes.
         //
         //     [ a  b ]
         // Let [      ] = A A^T
         //     [ b  c ]
         double a = normal.dot (normal);
         double b = normal.dot (plane.normal);
         double c = plane.normal.dot (plane.normal);
         double denom = a*c - b*b;
         pnt.scale ((c*offset - b*plane.offset)/denom, normal);
         pnt.scaledAdd ((-b*offset + a*plane.offset)/denom, plane.normal);
         return true;
      }
   }

   /**
    * Returns a String representation of this Plane, consisting of the x, y, and
    * z components of the normal, followed by the offset.
    * 
    * @return String representation
    */
   public String toString() {
      return toString ("%g");
   }

   /**
    * Returns a String representation of this Plane, consisting of the x, y, and
    * z components of the normal, followed by the offset. Each element is
    * formatted using a C <code>printf</code> style format string. For a
    * description of the format string syntax, see
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
    * Returns a String representation of this Plane, consisting of the x, y, and
    * z components of the normal, followed by the offset. Each element is
    * formatted using a C <code>printf</code> style as decribed by the
    * parameter <code>NumberFormat</code>. When called numerous times, this
    * routine can be more efficient than
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
      buf.append (fmt.format (normal.x));
      buf.append (' ');
      buf.append (fmt.format (normal.y));
      buf.append (' ');
      buf.append (fmt.format (normal.z));
      buf.append (' ');
      buf.append (fmt.format (offset));

      return buf.toString();
   }

   /**
    * Applies an affine transformation to this plane, in place.
    * 
    * @param X
    * affine transformation
    */
   public void transform (AffineTransform3dBase X) {
      transform (X, this);
   }

   /**
    * Applies an inverse affine transformation to this plane, in place.
    * 
    * @param X
    * affine transformation
    */
   public void inverseTransform (AffineTransform3dBase X) {
      inverseTransform (X, this);
   }

   /**
    * Applies an affine transformation to a specified plane and places the
    * result in this plane.
    * 
    * @param X
    * affine transformation
    * @param plane
    * plane to transform
    */
   public void transform (AffineTransform3dBase X, Plane plane) {
      X.getMatrix().mulInverseTranspose (normal, plane.normal);
      offset = plane.offset + normal.dot (X.getOffset());
      // normalize:
      double mag = normal.norm();
      normal.scale (1/mag);
      offset /= mag;
   }

   /**
    * Applies a vector transform to this plane.
    * 
    * @param T
    * vector transformer
    */
   public void transform (VectorTransformer3d T) {
      transform (T, this);
   }
   
   /**
    * Applies a vector transform to a plane and places the result in this plane.
    * 
    * @param T
    * vector transformer
    * @param plane
    * plane to be transformed
    */
   public void transform (VectorTransformer3d T, Plane plane) {
      Point3d p = new Point3d();
      p.scale (plane.offset, plane.normal); // point on the untransformed plane
      T.transformCovec (normal, plane.normal);
      normal.normalize();
      T.transformPnt (p, p);
      offset = p.dot(normal);
   }

   /**
    * Applies an inverse vector transform to a plane and places 
    * the result in this plane.
    * 
    * @param T
    * vector transformer
    * @param plane
    * plane to be transformed
    */
   public void inverseTransform (VectorTransformer3d T, Plane plane) {
      Point3d p = new Point3d();
      p.scale (plane.offset, plane.normal); // point on the untransformed plane
      T.inverseTransformCovec (normal, plane.normal);
      normal.normalize();
      T.inverseTransformPnt (p, p);
      offset = p.dot(normal);
   }

   /**
    * Applies an inverse vector transform to this place.
    * 
    * @param T
    * vector transformer
    */
   public void inverseTransform (VectorTransformer3d T) {
      inverseTransform (T, this);
   }

   /**
    * Applies an inverse affine transformation to a specified plane and places
    * the result in this plane.
    * 
    * @param X
    * affine transformation
    * @param plane
    * plane to transform
    */
   public void inverseTransform (AffineTransform3dBase X, Plane plane) {
      offset = plane.offset - plane.normal.dot (X.getOffset());
      X.getMatrix().mulTranspose (normal, plane.normal);
      // normalize:
      double mag = normal.norm();
      normal.scale (1/mag);
      offset /= mag;
   }

   /**
    * Applies a rigid transformation to this plane, in place.
    * 
    * @param X
    * rigid transformation
    */
   public void transform (RigidTransform3d X) {
      transform (X, this);
   }

   /**
    * Applies an inverse rigid transformation to this plane, in place.
    * 
    * @param X
    * rigid transformation
    */
   public void inverseTransform (RigidTransform3d X) {
      inverseTransform (X, this);
   }

   /**
    * Applies a rigid transformation to a specified plane and places the
    * result in this plane.
    * 
    * @param X
    * rigid transformation
    * @param plane
    * plane to transform
    */
   public void transform (RigidTransform3d X, Plane plane) {
      X.R.mul (normal, plane.normal);
      offset = plane.offset + normal.dot (X.p);
   }

   /**
    * Applies an inverse rigid transformation to a specified plane and places
    * the result in this plane.
    * 
    * @param X
    * rigid transformation
    * @param plane
    * plane to transform
    */
   public void inverseTransform (RigidTransform3d X, Plane plane) {
      offset = plane.offset - plane.normal.dot (X.p);
      X.R.mulTranspose (normal, plane.normal);
   }

   /**
    * Scales the units of this plane by the specified scale factor. This amounts
    * to simply scaling the offset.
    * 
    * @param s
    * scale factor by which to scale plane
    */
   public void scale (double s) {
      offset *= s;
   }

   /**
    * Returns the normal for this plane.
    * 
    * @return normal for the plane
    */
   public Vector3d getNormal() {
      return normal;
   }

   public void setNormal(Vector3d nrm) {
      normal.set(nrm);
   }
   
   /**
    * Returns the offset for this plane.
    * 
    * @return offset for the plane
    */
   public double getOffset() {
      return offset;
   }
   
   public void setOffset(double o) {
      offset = o;
   }

   /**
    * Sets the contents of this plane to values read from a ReaderTokenizer. The
    * input should consist of a sequence of four numbers, separated by white
    * space and optionally surrounded by square brackets <code>[ ]</code>.
    * 
    * @param rtok
    * Tokenizer from which vector values are read. Number parsing should be
    * enabled.
    * @throws IOException
    * if an I/O or formatting error is encountered
    */
   public void scan (ReaderTokenizer rtok) throws IOException {
      boolean bracketted = false;
      if (rtok.nextToken() == '[') {
         bracketted = true;
      }
      else {
         rtok.pushBack();
      }
      normal.x = rtok.scanNumber();
      normal.y = rtok.scanNumber();
      normal.z = rtok.scanNumber();
      offset = rtok.scanNumber();
      if (bracketted) {
         rtok.scanToken (']');
      }
   }

   private double house (double[] v, double[] x, int off, int num) {
      double magSqr = 0;
      for (int i = 0; i < num; i++) {
         magSqr += x[i + off] * x[i + off];
      }
      double mag = Math.sqrt (magSqr);
      double sgnX0 = (x[off] >= 0 ? 1 : -1);
      double vmagSqr = 1;
      if (mag != 0) {
         double beta = x[off] + sgnX0 * mag;
         for (int i = 1; i < num; i++) {
            v[i] = x[off + i] / beta;
            vmagSqr += v[i] * v[i];
         }
      }
      else {
         for (int i = 1; i < num; i++) {
            v[i] = x[off + i];
            vmagSqr += v[i] * v[i];
         }
      }
      v[0] = -mag * sgnX0;
      return vmagSqr;
   }

   private void housePreMul (
      double[] x, int off, double[] v, int num, double vmagSqr) {
      double w = x[off];
      for (int i = 1; i < num; i++) {
         w += x[off + i] * v[i];
      }
      w *= -2 / vmagSqr;
      x[off] += w;
      for (int i = 1; i < num; i++) {
         x[off + i] += w * v[i];
      }
   }

   /**
    * Fits this plane to a set of points.
    * 
    * @param pnts
    * points to fit
    * @param num
    * number of points
    */
   public void fit (Point3d[] pnts, int num) {
      if (num < 3) {
         throw new IllegalArgumentException ("Less than three points specified");
      }
      if (pnts.length < num) {
         throw new IllegalArgumentException (
            "Point array does not contain num points");
      }
      Point3d centroid = new Point3d();
      for (int i = 0; i < num; i++) {
         centroid.add (pnts[i]);
      }
      centroid.scale (1 / (double)num);
      Matrix3d R = new Matrix3d();
      Matrix3d V = new Matrix3d();

      double[] px = new double[num];
      double[] py = new double[num];
      double[] pz = new double[num];

      for (int i = 0; i < num; i++) {
         px[i] = pnts[i].x - centroid.x;
         py[i] = pnts[i].y - centroid.y;
         pz[i] = pnts[i].z - centroid.z;
      }

      double[] v = px; // reuse px storage for v
      double vmagSqr = house (v, px, 0, num);
      housePreMul (py, 0, v, num, vmagSqr);
      housePreMul (pz, 0, v, num, vmagSqr);

      R.m00 = v[0];
      R.m01 = py[0];
      R.m02 = pz[0];

      vmagSqr = house (v, py, 1, num - 1);
      housePreMul (pz, 1, v, num - 1, vmagSqr);

      R.m11 = v[0];
      R.m12 = pz[1];

      double magSqr = 0;
      for (int i = 2; i < num; i++) {
         magSqr += pz[i] * pz[i];
      }
      R.m22 = (pz[2] >= 0 ? -Math.sqrt (magSqr) : Math.sqrt (magSqr));

      @SuppressWarnings("unused")
      SVDecomposition3d svd = new SVDecomposition3d (null, V, R);
      normal.x = V.m02;
      normal.y = V.m12;
      normal.z = V.m22;
      offset = 0;
      for (int i = 0; i < num; i++) {
         offset += normal.dot (pnts[i]);
      }
      offset /= num;
   }

   public void setRandom() {
      normal.setRandom();
      normal.normalize();
      offset = RandomGenerator.nextDouble (-0.5, 0.5);
   }

   public boolean equals (Plane plane) {
      return normal.equals(plane.normal) && offset == plane.offset;
   }

   public boolean epsilonEquals (Plane plane, double eps) {
      return (normal.epsilonEquals(plane.normal, eps) &&
              Math.abs(offset-plane.offset) <= eps);
   }

}
