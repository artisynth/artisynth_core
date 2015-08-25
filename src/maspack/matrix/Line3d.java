/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.NumberFormat;

/**
 * Line3d represents a vector description of a line in three dimensions, which 
 * is described by a point on the line and a direction vector.
 */
public class Line3d {

   /**
    * Direction vector
    */
   protected final Vector3d dir;
   
   /**
    * Point on the line
    */
   protected final Point3d pnt;
   
   /**
    * Creates a Line3d and initializes it to the x axis.
    */
   public Line3d() {
      this.dir = new Vector3d (1, 0, 0);
      this.pnt = new Point3d( 0, 0, 0);
   }

   /**
    * Creates a Line3d and initialises it to the prescribed values. The 
    * direction values are normalised.
    * 
    * @param values
    * Line3d values given as an array. The x, y, and z components of the
    * direction vector are given by elements 0 through 2, and point is given
    * by elements 3-5.
    */
   public Line3d (double[] values) {
      this();
      set (values);
   }

   /**
    * Creates a Line3d and initialises it to the prescribed values. The 
    * direction values are normalised.
    * @param dirx
    * x component of the direction vector
    * @param diry
    * y component of the direction vector
    * @param dirz
    * z component of the direction vector
    * @param x
    * x component of the point
    * @param y
    * y component of the point
    * @param z
    * z component of the point
    */
   public Line3d (double dirx, double diry, double dirz, double x, double y, double z) {
      this();
      set (dirx, diry, dirz, x, y, z);
   }

   /**
    * Creates an Line3d and sets it to the prescribed values. The direction 
    * values are normalised.
    * 
    * @param direction
    * direction vector
    * @param point
    * Point on the line
    */
   public Line3d (Vector3d direction, Point3d point) {
      this();
      set (direction, point);
   }

   /**
    * Creates a Line3d and initializes it from an existing Line3d.
    * 
    * @param line3d
    * Line3d to supply initial values
    */
   public Line3d (Line3d line3d) {
      this();
      set (line3d);
   }

   /**
    * Sets this Line3d to the prescribed values. The direction values are
    * normalized.
    * 
    * @param dirx
    * x component of direction vector
    * @param diry
    * y component of direction vector
    * @param dirz
    * z component of direction vector
    * @param x
    * x component of point
    * @param y
    * y component of point
    * @param z
    * z component of point
    */
   public void set (double dirx, double diry, double dirz, double x, double y, double z) {
      dir.set (dirx,diry,dirz);
      pnt.set (x,y,z);
      dir.normalize();
   }

   /**
    * Sets this Line3d to the prescribed values. The direction values are
    * normalized.
    * 
    * @param values
    * Line3d values given as an array. The x, y, and z components of the 
    * direction vector are given by elements 0 through 2, and the position
    * components by elements 3 through 5
    */
   public void set (double[] values) {
      set (values[0], values[1], values[2], values[3], values[4], values[5]);
   }

   /**
    * Sets this Line3d to the prescribed values. The direction values are
    * normalised.
    * 
    * @param direction
    * direction vector
    * @param point
    * point on the line
    */
   public void set (Vector3d direction, Point3d point) {
      this.dir.set (direction);
      this.dir.normalize ();
      this.pnt.set (point);
   }

   /**
    * Sets this Line3d to the values of another Line3d.
    * 
    * @param line3d
    * Line3d supplying new values
    */
   public void set (Line3d line3d) {
      this.dir.set (line3d.dir);
      this.pnt.set (line3d.pnt);
   }


   /**
    * Gets the values associated with this Line3d.
    * 
    * @param values
    * returns the Line3d values. The x, y, and z components of the direction
    * vector are given by elements 0 through 2, and the components of the point
    * on the line are given by elements 3 through 5
    */
   public void get (double[] values) {
      values[0] = dir.x;
      values[1] = dir.y;
      values[2] = dir.z;
      values[3] = pnt.x;
      values[4] = pnt.y;
      values[5] = pnt.z;
   }

   /**
    * Returns a String representation of this Line3d, consisting of the
    * components of the direction vector followed by those of the point
    * 
    * @return String representation
    */
   public String toString() {
      return toString ("%g");
   }

   /**
    * Returns a String representation of this Point3d, consisting of the x, y,
    * and z components of the direction vector, followed by components of the
    * point.  Each element is formatted using a C <code>printf</code> style 
    * format string. For a description of the format string syntax, see
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
    * Returns a String representation of this Point3d, consisting of the x, y,
    * and z components of the direction vector, followed by components of the
    * point.  Each element is formatted using a C <code>printf</code> style as 
    * decribed by the parameter <code>NumberFormat</code>. When called numerous
    * times, this routine can be more efficient than
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
      buf.append (fmt.format (dir.x));
      buf.append (' ');
      buf.append (fmt.format (dir.y));
      buf.append (' ');
      buf.append (fmt.format (dir.z));
      buf.append (' ');
      buf.append (fmt.format (pnt.x));
      buf.append (' ');
      buf.append (fmt.format (pnt.y));
      buf.append (' ');
      buf.append (fmt.format (pnt.z));

      return buf.toString();
   }

   
   /**
    * Finds the closest point to the supplied Point3d that lies
    * on the line.
    * @param point
    * The Point3d to project
    * @return
    * Point3d on the line representing the projection
    */
   public Point3d project (Vector3d point) {
      
      // x = b + (a-b)dot(v) / v^2, where line=b+k*v
      Point3d out = new Point3d(point);
      
      out.sub(pnt);
      double k = out.dot(dir) / dir.normSquared();
      out.set(dir);
      out.scale(k);
      out.add(pnt);

      return out;
   }

   /**
    * Finds the distance of a point to this line.
    * @param pnt
    * Point to find the distance to
    * @return
    * distance to the point
    */
   public double distance (Vector3d pnt) {
      return pnt.distance (project (pnt));
   }
   
   /**
    * Returns true if the supplied point lies on this line within
    * a specified tolerance
    * 
    * @param point
    * Point to test if is on the line
    * @param eps
    * Tolerance distance
    * @return
    * True if point is sufficiently close to line
    */
   public boolean epsilonContains(Point3d point, double eps) {
      
      Point3d proj = project(point);
      if (proj.distance (point) > eps) {
         return false;
      }
      return true;
   }

   
   
   /**
    * Returns true if supplied point lies exactly on the line
    * @param point
    * Point3d to test
    * @return
    * true if point is on the line
    */
   public boolean contains(Point3d point) {
      Point3d proj = project(point);
      if (!proj.equals (point)) {
         return false;
      }
      return true;
   }
   
   /**
    * Returns true if the elements of this Line3d equal those of another
    * Line3d within a prescribed tolerance <code>epsilon</code>.
    * 
    * @param line3d
    * Line3d to compare with
    * @param eps
    * comparison tolerance
    * @return false if the Line3ds are not equal within the specified
    * tolerance
    */
   public boolean epsilonEquals (Line3d line3d, double eps)
      throws ImproperSizeException {
      if (!dir.epsilonEquals (line3d.dir, eps)) {
         return false;
      }
      
      if (!epsilonContains (line3d.pnt, eps)) {
         return false;
      }
      return true;
   }

   /**
    * Returns true if the elements of this Line3d equal those of another
    * Line3d exactly.
    * 
    * @param line3d
    * Line3d to compare with
    * @return false if the Line3ds are not equal
    */
   public boolean equals (Line3d line3d) {
      if (!dir.equals (line3d.dir)) {
         return false;
      }
      if (!contains(line3d.pnt)) {
         return false;
      }
      return true;
   }

   /**
    * Returns true if the supplied object is a Line3d and its elements equal
    * those of this Line3d exactly.
    * 
    * @param obj
    * Object to compare with
    * @return false if the Object does not equal this Line3d
    */
   public boolean equals (Object obj) {
      if (obj.equals (this)) {
         return true;
      } else if (obj instanceof Line3d) {
         return equals ((Line3d)obj);
      } else {
         return false;
      }
   }
   
   /**
    * Gets the direction vector for the line
    * @param d
    * Filled by the values of the direction vector
    */
   public void getDirection(Vector3d d) {
      d.set (dir);
   }
   
   /**
    * Gets a point on the line
    * @param p
    * Filled by values of a point on the line
    */
   public void getPoint(Vector3d p) {
      p.set (pnt);
   }
   
   public void setDirection(Vector3d d) {
      dir.set(d);
   }
   
   public void setPoint(Point3d p) {
      pnt.set(p);
   }

   /**
    * Finds the point on THIS line that is closest to the supplied line
    * @param line
    * The Line3d to which we search for the closest point
    * @param epsilon
    * Tolerance for which lines are considered parallel
    * @return
    * The point on the current line which is closest to the supplied Line3d.
    * Returns null if lines are parallel
    */   
   public Point3d closestPoint(Line3d line, double epsilon) {
      
      Point3d out1 = new Point3d();
      boolean passed = closestPointBetweenLines(this, line, out1, null, epsilon);
      if (!passed) {
         return null;
      }

      return out1;
   }
   
   public static boolean closestPointBetweenLines(Line3d line1, Line3d line2, Point3d pnt1, Point3d pnt2, double epsilon) {
      return closestPointBetweenLines(line1.dir, line1.pnt, line2.dir, line2.pnt, pnt1, pnt2, epsilon);
   }
   
   public static boolean closestPointBetweenLines(Vector3d v1, Point3d p1, Vector3d v2, Point3d p2, Point3d out1, Point3d out2, double epsilon) {
      
      // null situation is when two lines are parallel
      if ((1 - Math.abs(v2.dot(v1) / v2.norm() / v1.norm())) <= epsilon) {
         return false;
      }

      // set v3 to be perpendicular to v1 and v2
      Vector3d v3 = new Vector3d(v2);
      v3.cross(v1); // give direction vector of new line
      v3.normalize();

      // solve for points connecting line
      Point3d b = new Point3d(p2);
      b.sub(p1);
      Matrix3d M =
         new Matrix3d(v1.x, -v2.x, v3.x, v1.y, -v2.y, v3.y, v1.z, -v2.z, v3.z);
      Matrix3d Minv = new Matrix3d(M);
      
      // solves Mx = b for x
      Minv.invert();
      Minv.mul(b);

      // point on first line is p1+x1*v1
      if (out1 != null) {
         out1.set(v1);
         out1.scale(b.x);
         out1.add(p1);
      }
      
      // point on second line is p2+x2*v2
      if (out2 != null) {
         out2.set(v2);
         out2.scale(b.y);
         out2.add(p2);
      }
      
      return true;
   }

   /**
    * Finds the point on THIS line that is closest to the supplied line
    * @param line
    * The Line3d to which we search for the closest point
    * @return
    * The point on the current line which is closest to the supplied Line3d
    * Returns null if lines are parallel
    */
   public Point3d closestPoint(Line3d line) {
      return closestPoint(line, 0);
   }
   
}
