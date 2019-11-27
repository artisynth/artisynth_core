package artisynth.demos.wrapping;

import java.util.ArrayList;

import artisynth.core.mechmodels.Point;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;

/**
 * A path between two points, computed using exact solutions. These paths 
 * are differentiable, and each point on the path holds its unit tangent 
 * vector to the path at that point. 
 * @author Omar
 */
public class ExactWrapPath {
   private WrapSegment segs[];          // Segments of the path.
   private int nSegments;               // Number of segments on the path.
   private ArrayList<Point3d> drawP;    // Points to be used for rendering.
   private Point3d refPoint;            // Reference point for straight paths
   
   /**
    * Create a new empty path with 3 segments.
    */
   public ExactWrapPath() {
      segs = new WrapSegment[3];
      nSegments = 3;
      drawP = new ArrayList<Point3d>();
   }
   
   /**
    * Create a new empty path with n segments.
    * @param n          number of segments in the path
    */
   public ExactWrapPath(int n) {
      segs = new WrapSegment[n];
      nSegments = n;
      drawP = new ArrayList<Point3d>();
   }
   
   /**
    * Create a new path from the given points.
    * @param points     points on the path (these points will be changed if they
    *                   are input in world coordinates)
    * @param tangents   path tangents at the points (world frame)
    * @param n          number of points
    * @param world      true if the points are in world coordinates
    */
   public ExactWrapPath(Point3d points[], Vector3d tangents[], int n,
      boolean world) {
      segs = new WrapSegment[n - 1];
      nSegments = n - 1;
      drawP = new ArrayList<Point3d>();
      
      WrapPoint wps[] = new WrapPoint[n];
      for(int i = 0; i < n; i++) {
         wps[i] = new WrapPoint(points[i], tangents[i]);
      }
      for(int i = 0; i < n - 1; i++) {
         segs[i] = new WrapSegment(wps[i], wps[i + 1]);
      }
   }
   
   /**
    * Get the number of points in this path.
    * @return           number of points in this path
    */
   public int getNumPoints() {
      return nSegments + 1;
   }
   
   /**
    * Get the number of segments in this path.
    * @return           number of segments in this path
    */
   public int getNumSegments() {
      return nSegments;
   }
   
   /**
    * Get a point on the path.
    * @param i          index of the point
    * @return           point of the given index
    */
   public WrapPoint getPoint(int i) {
      if (segs.length < i) {
         return segs[segs.length-1].getEndPoint (); // return end point of first segment
      }
      else if(i > 0) {
         return segs[i - 1].getEndPoint();
      } else {
         return segs[0].getStartPoint();
      }
   }
   
   /**
    * Get a segment from the path.
    * @param i          index of the segment
    * @return           segment of the given index
    */
   public WrapSegment getSegment(int i) {
      return segs[i];
   }
   
   /**
    * Get the length of the path.
    * @return           length of the path
    */
   public double getLength() {
      double l = 0;
      for(int i = 0; i < nSegments; i++) {
         l = l + segs[i].getLength();
      }
      return l;
   }
   
   /**
    * Returns time derivative of the length.
    * @return           time derivative of length
    */
   public double getLengthDot() {
      int scale = -1;
      double dldt = 0;
      Vector3d temp = new Vector3d();
      if (nSegments > 0) {
         dldt += lengthDot (segs[0].getStartPoint (), temp, scale);
         scale = -scale;
      }
      for (WrapSegment s : segs) {
         dldt += lengthDot (s.getEndPoint (), temp, scale);
         scale = -scale;
      }  
      return dldt;
   }
   
   private double lengthDot (WrapPoint wp, Vector3d temp, int scale) {
      temp.set (wp.getTangent ());
      temp.normalize ().scale (scale);
      return temp.dot (wp.getPoint ().getVelocity ());
   }

   public void applyForces(double F) {
      Vector3d force = new Vector3d();
      WrapPoint wp;
      Point pnt;
      
      // Apply forces to the inner points of the path.
      for(int i = 1; i <= nSegments; i++) {
         wp = getPoint(i);
         pnt = wp.getPoint();
         force.set(wp.getTangent());
         // If previous segment was straight, pull away from direction of
         // traversal. If next segment is straight, pull in the direction of
         // traversal. If neither was straight, don't apply a force at this
         // point.
         if(segs[i - 1].isStraight() || i == nSegments) {
            force.scale(-F);
         } else if(segs[i].isStraight()) {
            force.scale(F);
         } else {
            force.set(0, 0, 0);
         }
//         f.getFrame().addPointForce(f.getLocation(), force);
         pnt.addForce (force);
      }
      
      // Apply forces to the start point of the path.
      wp = segs[0].getStartPoint();
      pnt = wp.getPoint();
      force.set(wp.getTangent());
      force.scale(F);
      pnt.addForce(force);
   }
   
   /**
    * Adds the given point to the list of draw points.
    * @param p          the point to add (in world frame)
    * @return           true if the add was successful; false otherwise
    */
   public boolean addDrawPoint(Point3d p) {
      return drawP.add(p);
   }
   
   /**
    * Returns the list of draw points.
    * @return           list of draw points
    */
   public ArrayList<Point3d> getDrawPoints() {
      return drawP;
   }
   
   public ArrayList<Point3d> getAllABPoints () {
      ArrayList<Point3d> pnts = new ArrayList<> ();
      for(WrapSegment seg : segs) {
         if (!seg.isStraight ()) {
            Point3d pnt = seg.getStartPoint ().getPoint ().getPosition ();
            if (!pnts.contains (pnt)) {
               pnts.add (pnt);
            }
            pnt = seg.getEndPoint ().getPoint ().getPosition ();
            if (!pnts.contains (pnt)) {
               pnts.add (pnt);
            }
         }
      }
      return pnts;
   }
   
   /**
    * Renders this path.
    * @param renderer   renderer to use for drawing
    * @param props      render properties
    */
   public void renderPath(Renderer renderer, RenderProps props) {
      Point3d prev = null;
      for(Point3d p : drawP) {
         if(prev != null) {
            renderer.drawLine(props,
               new float[] {(float) prev.x, (float) prev.y, (float) prev.z},
               new float[] {(float) p.x, (float) p.y, (float) p.z},
                              null, true, false);
         }
         prev = p;
      }
   }

   /**
    * Get reference point. This is a point on the wrappable that acts as a
    * combined A/B point reference for paths that do *not* touch the surface.
    * If the path does touch the surface, returns null.
    */
   public Point3d getRefPoint() {
      return refPoint;
   }

   /**
    * Sets the reference point.
    */
   public void setRefPoint(Point3d pnt) {
      if (pnt == null) {
         refPoint = null;
      }
      else {
         refPoint = new Point3d(pnt);
      }
   }
   
   /**
    * A segment on the path. Can be a line or a geodesic curve.
    * @author Omar
    */
   protected class WrapSegment {
      private WrapPoint p0;             // Start point of the segment.
      private WrapPoint p1;             // End point of the segment.
      private double length;            // Length of the segment.
      private boolean straight;         // Is this segment a straight line?
      
      /**
       * Create a segment with the given endpoints. Length is calculated
       * assuming the segment is straight.
       * @param ip0     starting point
       * @param ip1     end point
       */
      public WrapSegment(WrapPoint ip0, WrapPoint ip1) {
         p0 = ip0;
         p1 = ip1;
         straight = true;
         length = 0; // to be calculated and set externally
      }
      
      /**
       * Set curvature of this segment.
       * @param str     is the segment straight?
       */
      public void setStraight(boolean str) {
         straight = str;
      }
      
      /**
       * Is this segment straight or curved?
       * @return        true if the segment is straight, false otherwise
       */
      public boolean isStraight() {
         return straight;
      }
      
      /**
       * Set the length of this segment.
       * @param nl      length of this segment
       */
      public void setLength(double nl) {
         length = nl;
      }
      
      /**
       * Get the length of this segment.
       * @return        length of this segment
       */
      public double getLength() {
         return length;
      }
      
      /**
       * Get the starting point of this segment.
       * @return        starting point of this segment
       */
      public WrapPoint getStartPoint() {
         return p0;
      }
      
      /**
       * Get the ending point of this segment.
       * @return        ending point of this segment
       */
      public WrapPoint getEndPoint() {
         return p1;
      }
   }
   
   /**
    * A point on the path.
    * @author Omar
    */
   protected class WrapPoint {
      private Point loc;          // Location of the point.
      private Vector3d tangent;         // Tangent at the point (world frame).
      private double[] coords;          // Parametric coordinates of the point.
      
      /**
       * Create a new point with the given location and tangent.
       * @param l       location of the point (in the given frame)
       * @param t       tangent at the point (world frame)
       */
      public WrapPoint(Point3d l, Vector3d t) {
         loc = new Point(l);
         tangent = t;
         coords = null;
      }
      
      /**
       * Create a new point with the given location, tangent, and 2 parametric
       * coordinates.
       * @param l       location of the point (in the given)
       * @param t       tangent at the point (world frame)
       * @param c1      first parametric coordinate
       * @param c2      second parametric coordinate
       */
      public WrapPoint(Point3d l, Vector3d t, double c1, double c2) {
         loc = new Point(l);
         tangent = t;
         coords = new double[] {c1, c2};
      }
      
      /**
       * Set the parametric coordinates of this point.
       * @param c       parametric coordinates of this point
       */
      public void setCoordinates(double c[]) {
         coords = c;
      }
      
      /**
       * Get the frame marker that describes this wrap point.
       * @return        frame marker
       */
      public Point getPoint() {
         return loc;
      }
      
      /**
       * Get location of this point in world frame.
       * @return        location of this point in world frame
       */
      public Point3d getPosition() {
         return loc.getPosition();
      }
      
      /**
       * Get tangent to the path at this point in world frame.
       * @return        tangent at this point in world frame
       */
      public Vector3d getTangent() {
         return tangent;
      }
      
      /**
       * Get parametric coordinates of this point.
       * @return        parametric coordinates of this point
       */
      public double[] getCoords() {
         return coords;
      }
   }
}
