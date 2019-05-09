/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.*;
import java.io.IOException;

/**
 * Implements a line of 3-space, characterized by a point <i>p</i> and a
 * direction <i>u</i>. This representation is redundant, since a line has only
 * four degrees of freedom. However, the point-direction representation is
 * convenient for parametrically expressing points along the line, as
 * 
 * <pre>
 * x = p + u s
 * </pre>
 * 
 * @author John E. Lloyd
 */
public class Line {
   protected Point3d myP; // origin of the line
   protected Vector3d myU; // direction of the line (normalized)

   private static double DOUBLE_PREC = 2.220446049250313e-16;

   public Line() {
      myP = new Point3d();
      myU = new Vector3d (1, 0, 0);
   }

   public Line (Point3d p, Vector3d u) {
      this();
      set (p, u);
   }

   public Line (Line line) {
      this();
      set (line);
   }

   public Line (double px, double py, double pz, double ux, double uy, double uz) {
      this();
      set (px, py, pz, ux, uy, uz);
   }

   public void set (Point3d p, Vector3d u) {
      myP.set (p);
      myU.set (u);
      myU.normalize();
   }

   public void set (
      double px, double py, double pz, double ux, double uy, double uz) {
      myP.set (px, py, pz);
      myU.set (ux, uy, uz);
      myU.normalize();
   }

   public void set (Line line) {
      myP.set (line.myP);
      myU.set (line.myU);
   }

   public Point3d getOrigin() {
      return myP;
   }

   public Vector3d getDirection() {
      return myU;
   }

   public void setOrigin (Point3d p) {
      myP.set (p);
   }
   
   public void setOrigin(double x, double y, double z) {
      myP.set(x,y,z);
   }

   public void setPoints (Point3d p0, Point3d p1) {
      myP.set (p0);
      myU.sub (p1, p0);
      myU.normalize();
   }

   public void setDirection (Vector3d u) {
      myU.set (u);
      myU.normalize();
   }
   
   public void setDirection(double x, double y, double z) {
      myU.set(x,y,z);
      myU.normalize();
   }

   public void getPluecker (Vector3d u, Vector3d v) {
      u.set (myU);
      v.cross (myU, myP);
   }

   /**
    * Applies an affine transformation to this line, in place.
    * 
    * @param X
    * affine transformation
    */
   public void transform (AffineTransform3dBase X) {
      transform (X, this);
   }

   /**
    * Applies an inverse affine transformation to this line, in place.
    * 
    * @param X
    * affine transformation
    */
   public void inverseTransform (AffineTransform3dBase X) {
      inverseTransform (X, this);
   }

   /**
    * Applies an affine transformation to a specified line and places the result
    * in this line.
    * 
    * @param X
    * affine transformation
    * @param line
    * line to transform
    */
   public void transform (AffineTransform3dBase X, Line line) {
      myP.transform (X, line.myP);
      myU.transform (X, line.myU);
      myU.normalize();
   }

   /**
    * Applies an inverse affine transformation to a specified line and places
    * the result in this line.
    * 
    * @param X
    * affine transformation
    * @param line
    * line to transform
    */
   public void inverseTransform (AffineTransform3dBase X, Line line) {
      myP.inverseTransform (X, line.myP);
      myU.inverseTransform (X, line.myU);
      myU.normalize();
   }

   /**
    * Returns the perpendicular distance of this line to a point
    * 
    * @param p1
    * point to find distance to
    * @return distance of the point from this line
    */
   public double distance (Point3d p1) {
      Vector3d tmp = new Vector3d();
      tmp.sub (p1, myP); // p1 - origin
      tmp.scaledAdd (tmp.dot (myU), myU, myP);
      return tmp.distance (p1);
   }

   /**
    * Returns the perpendicular distance of this line to another line.
    * 
    * @param line
    * line to find distance to
    * @return distance of the line from this line
    */
   public double distance (Line line) {
      Vector3d u1 = myU; // break out u1 and u2 for clarity
      Vector3d u2 = line.myU;

      Vector3d tmp = new Vector3d();
      tmp.cross (u1, u2);
      double denom = tmp.normSquared();
      if (denom < 100 * DOUBLE_PREC) {
         // lines are parallel, project other line's origin
         // onto this line
         tmp.sub (line.myP, myP);
         double lam1 = tmp.dot (myU);
         tmp.scaledAdd (lam1, myU, myP);
         return tmp.distance (line.myP);
      }
      else {
         tmp.sub (myP, line.myP);
         double k1 = -u1.dot (tmp);
         double k2 = u2.dot (tmp);
         double dotU = u1.dot (u2);
         double lam1 = (k1 + dotU * k2) / denom;
         double lam2 = (dotU * k1 + k2) / denom;
         // find the closest point on this line and then subtract
         // the closest point on the other line
         tmp.scaledAdd (lam1, u1, myP); // closest point on this line
         tmp.sub (line.myP);
         tmp.scaledAdd (-lam2, u2, tmp);
         return tmp.norm();
      }
   }
   
   /**
    * Returns the perpendicular distance of this line to another line.
    * 
    * @param line
    * line to find distance to
    * @param point
    * point on the current line nearest to the provided line
    * @param nearPoint
    * point on line nearest to this
    * @return distance of the line from this line
    */
   public double distance (Line line, Point3d point, Point3d nearPoint) {
      Vector3d u1 = myU; // break out u1 and u2 for clarity
      Vector3d u2 = line.myU;

      Vector3d tmp = new Vector3d();
      tmp.cross (u1, u2);
      double denom = tmp.normSquared();
      if (denom < 100 * DOUBLE_PREC) {
         // lines are parallel
         tmp.sub (line.myP, myP);
         double lam1 = tmp.dot (myU);
         point.scaledAdd (lam1, myU, myP);
         nearPoint.set (line.myP);
         return point.distance (nearPoint);
      }
      else {
         tmp.sub (myP, line.myP);
         double k1 = -u1.dot (tmp);
         double k2 = u2.dot (tmp);
         double dotU = u1.dot (u2);
         double lam1 = (k1 + dotU * k2) / denom;
         double lam2 = (dotU * k1 + k2) / denom;
         // find the closest point on this line and then subtract
         // the closest point on the other line
         point.scaledAdd (lam1, u1, myP); // closest point on this line
         nearPoint.scaledAdd (lam2, u2, line.myP);
         return point.distance (nearPoint);
      }
   }

   /**
    * Returns the nearest point on this line to another point.
    * 
    * @param pr
    * returns nearest point value
    * @param p1
    * point to find nearest point to
    * @return signed distance of the nearest point from the origin along the
    * direction
    */
   public double nearestPoint (Point3d pr, Vector3d p1) {
      Vector3d tmp = new Vector3d();
      tmp.sub (p1, myP);
      double lam = tmp.dot (myU);
      pr.scaledAdd (lam, myU, myP);
      return lam;
   }

   /**
    * Finds the nearest point on this line to another line. If the lines are
    * parallel, then this line's origin is returned.
    * 
    * @param p
    * returns the nearest point value
    * @param line
    * line to find nearest point to
    * @return signed distance of the nearest point from the origin along the
    * direction
    */
   public double nearestPoint (Point3d p, Line line) {
      Vector3d u1 = myU; // break out u1 and u2 for clarity
      Vector3d u2 = line.myU;

      Vector3d tmp = new Vector3d();
      tmp.cross (u1, u2);
      double denom = tmp.normSquared();
      if (denom < 100 * DOUBLE_PREC) {
         p.set (myP);
         return 0;
      }
      else {
         tmp.sub (myP, line.myP);
         double k1 = -u1.dot (tmp);
         double k2 = u2.dot (tmp);
         double dotU = u1.dot (u2);
         double lam1 = (k1 + dotU * k2) / denom;
         // double lam2 = (dotU*k1 + k2)/denom;
         p.scaledAdd (lam1, u1, myP);
         return lam1;
      }
   }

   /**
    * Finds the intersection of this line with a plane. If the line and the
    * plane are parallel, then the intersection is set to this line's origin
    * value and the method returns infinity.
    * 
    * @param p
    * returns the intersection point
    * @param plane
    * plane to intersect with
    * @return signed distance along direction from the origin to the
    * intersection point
    */
   public double intersectPlane (Point3d p, Plane plane) {
      double denom = myU.dot (plane.normal);
      if (Math.abs (denom) < 100 * DOUBLE_PREC) {
         p.set (myP);
         return Double.POSITIVE_INFINITY;
      }
      else {
         double lam = (plane.offset - plane.normal.dot (myP)) / denom;
         p.scaledAdd (lam, myU, myP);
         return lam;
      }
   }

   /**
    * Returns a String representation of this Line, consisting of the x, y, and
    * z coordinates of the origin, followed by the coordinates of the direction.
    * 
    * @return String representation
    */
   public String toString() {
      return toString ("%g");
   }

   /**
    * Returns a String representation of this Line, consisting of the x, y, and
    * z coordinates of the origin, followed by the coordinates of the direction.
    * Each element is formatted using a C <code>printf</code> style format
    * string. For a description of the format string syntax, see
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
    * Returns a String representation of this Line, consisting of the x, y, and
    * z coordinates of the origin, followed by the coordinates of the direction.
    * Each element is formatted using a C <code>printf</code> style as
    * decribed by the parameter <code>NumberFormat</code>. When called
    * numerous times, this routine can be more efficient than
    * {@link #toString(String) toString(String)}, because the {@link
    * maspack.util.NumberFormat NumberFormat} does not need to be recreated each
    * time from a specification string.
    * 
    * @param fmt
    * numeric format
    * @return String representation of this line
    */
   public String toString (NumberFormat fmt) {
      StringBuffer buf = new StringBuffer (80);
      buf.append (fmt.format (myP.x));
      buf.append (' ');
      buf.append (fmt.format (myP.y));
      buf.append (' ');
      buf.append (fmt.format (myP.z));
      buf.append (' ');
      buf.append (fmt.format (myU.x));
      buf.append (' ');
      buf.append (fmt.format (myU.y));
      buf.append (' ');
      buf.append (fmt.format (myU.z));

      return buf.toString();
   }

   /**
    * Sets this line to values read from a ReaderTokenizer. The input should
    * consist of a sequence of six numbers, separated by white space and
    * optionally surrounded by square brackets <code>[ ]</code>. These
    * numbers give the coordinates of the line origin and direction,
    * repsectively.
    * 
    * @param rtok
    * Tokenizer from which line values are read. Number parsing should be
    * enabled.
    * @throws IOException
    * if an I/O or formatting error is encountered
    */
   public void scan (ReaderTokenizer rtok) throws IOException {
      boolean bracketted = false;
      if (rtok.nextToken() == '[') {
         bracketted = true;
      }
      myP.x = rtok.scanNumber();
      myP.y = rtok.scanNumber();
      myP.z = rtok.scanNumber();
      myU.x = rtok.scanNumber();
      myU.y = rtok.scanNumber();
      myU.z = rtok.scanNumber();
      myU.normalize();
      if (bracketted) {
         rtok.scanToken (']');
      }
   }

}
