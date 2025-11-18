/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD licence. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Collection;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import maspack.matrix.AffineTransform2dBase;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector2d;
import maspack.render.PointLineRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Implements a simple, closed polygon in 2D.
 */
public class Polygon2d implements Renderable, Iterable<Vertex2d> {
   Vertex2d firstVertex;

   protected static final double INF = Double.POSITIVE_INFINITY;

   RenderProps myRenderProps = new PointLineRenderProps();

   private class VertexIterator implements Iterator {
      Vertex2d myNext;

      VertexIterator() {
         myNext = firstVertex;
      }

      public boolean hasNext() {
         return myNext != null;
      }

      public Object next() throws NoSuchElementException {
         if (myNext == null) {
            throw new NoSuchElementException();
         }
         Vertex2d next = myNext;
         myNext = myNext.next;
         if (myNext == firstVertex) {
            myNext = null;
         }
         return next;
      }

      public void remove() throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
      }
   }

   public Polygon2d() {
      firstVertex = null;
   }

   public Iterator getVertices() {
      return new VertexIterator();
   }

   public Iterator<Vertex2d> iterator() {
      return new VertexIterator();
   }

   public Point2d[] getPoints() {
      Point2d[] pnts = new Point2d[numVertices()];
      Vertex2d vtx = firstVertex;
      for (int i=0; i<pnts.length; i++) {
         pnts[i] = new Point2d (vtx.pnt);
         vtx = vtx.next;
      }
      return pnts;
   }

   /**
    * Computes the distance from {@code pnt} to the edge starting
    * at vertex {@code vtx}.
    */
   private void nearestPointOnEdge (Point2d nearPnt, Vertex2d vtx, Point2d pnt) {
      Point2d p0 = vtx.pnt;
      Point2d p1 = vtx.next.pnt;
      Vector2d u = new Vector2d();
      u.sub (p1, p0);
      double len = u.norm();
      if (len == 0) {
         nearPnt.set (p0);
         return;
      }
      u.scale (1/len);
      Vector2d dp = new Vector2d();
      dp.sub (pnt, p0);
      double proj = dp.dot (u);
      if (proj < 0) {
         nearPnt.set (p0);
      }
      else if (proj > len) {
         nearPnt.set (p1);
      }
      else {
         nearPnt.scaledAdd (proj, u, p0);
      }
   }

   /**
    * Finds a nearest edge of this polygon to a given point, using a simple
    * O(n) search of all edges. The edge is represented by its starting vertex.
    * Note that the nearest edge is generally unique only if the polygon is
    * convex and {@code pnt} is outside it.
    *
    * @param nearPnt if non-null, returns the point nearest to {@code pnt}
    * @param pnt point for which the nearest edge is sought
    * @return starting vertex for the nearest edge
    */
   public Vertex2d nearestEdge (Point2d nearPnt, Point2d pnt) {
      double dmin = Double.POSITIVE_INFINITY;
      Vertex2d nearVtx = null;
      Vertex2d vtx = firstVertex;
      Point2d edgePnt = new Point2d();
      if (vtx != null) {
         if (vtx.next == null) {
            if (nearPnt != null) {
               nearPnt.set (vtx.pnt);
            }
            return vtx;
         }
         do {
            nearestPointOnEdge (edgePnt, vtx, pnt);
            double d = edgePnt.distance (pnt);
            if (d < dmin) {
               dmin = d;
               nearVtx = vtx;
               if (nearPnt != null) {
                  nearPnt.set (edgePnt);
               }
            }
            vtx = vtx.next;
         }
         while (vtx != firstVertex);
      }
      return nearVtx;
   }

   /**
    * Finds the distance of this polygon to a given point, defined
    * as the minimum of the distances from the point to each edge.
    * If the polygon has no vertices, then 0 is returned.
    *
    * @param pnt point for which the nearest distance is sought
    */
   public double distance (Point2d pnt) {
      Point2d nearPnt = new Point2d();
      if (nearestEdge (nearPnt, pnt) != null) {
         return nearPnt.distance (pnt);
      }
      else {
         return 0;
      }
   }

   /**
    * Intersects this polygon with a ray defined by p + s u, using O(n) search
    * of all edges. If there is more than one intersection, the nearest to p is
    * returned.
    *
    * @param p starting point of the ray
    * @param u direction of the ray
    * @return intersection point, or null if none
    */
   public Point2d intersectRay (Point2d p, Vector2d u) {
      double dmin = Double.POSITIVE_INFINITY;
      Point2d nearIsect = null;
      Vertex2d vtx = firstVertex;
      Point2d edgePnt = new Point2d();
      if (vtx != null) {
         do {
            Point2d isect = new Point2d();
            if (intersectRay (isect, vtx, p, u) != INF) {
               double d = isect.distance (p);
               if (d < dmin) {
                  dmin = d;
                  nearIsect = isect;
               }
            }
            vtx = vtx.next;
         }
         while (vtx != firstVertex);
      }
      return nearIsect;
   }

   /**
    * Intersect a line segment (p0, p1) defined by a vertex with a ray (p2, s
    * u).
    */
   protected double intersectRay (
      Point2d isect, Vertex2d vtx, Point2d p2, Vector2d u) {

      Point2d p0 = vtx.pnt;
      Point2d p1 = vtx.next.pnt;

      Vector2d u01 = new Vector2d();
      Vector2d u23 = u;
      Vector2d u02 = new Vector2d();
      u01.sub (p1, p0);
      u02.sub (p2, p0);

      double denom = u01.cross(u23);
      double numt = u02.cross(u23);
      double numu = u02.cross(u01);

      // t = numt/denom and u = numu/denom.
      if (denom == 0) {
         // segments are parallel
         return INF;
      }
      if (numt*denom < 0 || numu*denom < 0 ||
          Math.abs(numt) > Math.abs(denom)) {
         // intersection does not lie on both segments
         return INF;
      }
      if (isect != null) {
         isect.scaledAdd (numt/denom, u01, p0);
      }
      return numu/denom;
   }      

   /**
    * Intersect a line segment (p0, p1) defined by a vertex with a line
    * defined ray (p2, s u).
    */
   protected double intersectLine (
      Point2d isect, Vertex2d vtx, Point2d p2, Vector2d u) {

      Point2d p0 = vtx.pnt;
      Point2d p1 = vtx.next.pnt;

      Vector2d u01 = new Vector2d();
      Vector2d u23 = u;
      Vector2d u02 = new Vector2d();
      u01.sub (p1, p0);
      u02.sub (p2, p0);

      double denom = u01.cross(u23);
      double numt = u02.cross(u23);
      double numu = u02.cross(u01);

      // t = numt/denom and u = numu/denom.
      if (denom == 0) {
         // segments are parallel
         return INF;
      }
      if (numt*denom < 0 || Math.abs(numt) > Math.abs(denom)) {
         // intersection does not lie on the (p0, p1) segment
         return INF;
      }
      if (isect != null) {
         isect.scaledAdd (numt/denom, u01, p0);
      }
      return numu/denom;
   }      

   /**
    * Intersect a line segment (p0, p1) defined by a vertex with a ray (p2, s
    * u).
    */
   protected double intersectRay (Vertex2d vtx, Point2d p2, Vector2d u) {
      Point2d p0 = vtx.pnt;
      Point2d p1 = vtx.next.pnt;

      Vector2d u01 = new Vector2d();
      Vector2d u23 = u;
      Vector2d u02 = new Vector2d();
      u01.sub (p1, p0);
      u02.sub (p2, p0);

      double denom = u01.cross(u23);
      double numt = u02.cross(u23);
      double numu = u02.cross(u01);

      // t = numt/denom and u = numu/denom.
      if (denom == 0) {
         // segments are parallel
         return INF;
      }
      if (numt*denom < 0 || numu*denom < 0 ||
          Math.abs(numt) > Math.abs(denom)) {
         // intersection does not lie on both segments
         return INF;
      }
      return numt/denom;
   }      

   public int numVertices() {
      int num = 0;
      Vertex2d vtx = firstVertex;
      if (vtx != null) {
         do {
            vtx = vtx.next;
            num++;
         }
         while (vtx != firstVertex);
      }
      return num;
   }

   public Polygon2d (double[] coords) {
      set (coords, coords.length / 2);
   }

   public Polygon2d (Point2d[] pnts) {
      set (pnts, pnts.length);
   }

   public Polygon2d (Collection<? extends Point2d> pnts) {
      set (pnts);
   }

   public Polygon2d (Collection<? extends Point3d> pnts, RigidTransform3d TPW) {
      set (pnts, TPW);
   }

   public double getMaxCoordinate() {
      Vertex2d vtx = firstVertex;

      double max = 0;
      if (vtx != null) {
         max = vtx.pnt.infinityNorm();
         vtx = vtx.next;
         while (vtx != firstVertex) {
            double norm = vtx.pnt.infinityNorm();
            if (norm > max) {
               max = norm;
            }
            vtx = vtx.next;
         }
      }
      return max;
   }

   public void getBounds (Point2d minValues, Point2d maxValues) {
      Vertex2d vtx = firstVertex;

      if (vtx != null) {
         minValues.set (vtx.pnt);
         maxValues.set (vtx.pnt);
         vtx = vtx.next;
         while (vtx != firstVertex) {
            if (vtx.pnt.x < minValues.x) {
               minValues.x = vtx.pnt.x;
            }
            else if (vtx.pnt.x > maxValues.x) {
               maxValues.x = vtx.pnt.x;
            }
            if (vtx.pnt.y < minValues.y) {
               minValues.y = vtx.pnt.y;
            }
            else if (vtx.pnt.y > maxValues.y) {
               maxValues.y = vtx.pnt.y;
            }
            vtx = vtx.next;
         }
      }
      else {
         minValues.setZero();
         maxValues.setZero();
      }
   }

   public void updateBounds (Point2d minValues, Point2d maxValues) {
      Vertex2d vtx = firstVertex;

      if (vtx != null) {
         do {
            if (vtx.pnt.x < minValues.x) {
               minValues.x = vtx.pnt.x;
            }
            else if (vtx.pnt.x > maxValues.x) {
               maxValues.x = vtx.pnt.x;
            }
            if (vtx.pnt.y < minValues.y) {
               minValues.y = vtx.pnt.y;
            }
            else if (vtx.pnt.y > maxValues.y) {
               maxValues.y = vtx.pnt.y;
            }
            vtx = vtx.next;
         }
         while (vtx != firstVertex);
      }
   }

   public Vertex2d addVertex (double x, double y) {
      Vertex2d vtx = new Vertex2d (x, y);
      appendVertex (vtx);
      return vtx;
   }

   public void appendVertex (Vertex2d vtx) {
      if (firstVertex == null) {
         firstVertex = vtx;
         vtx.prev = vtx;
         vtx.next = vtx;
      }
      else {
         Vertex2d lastVertex = firstVertex.prev;
         lastVertex.next = vtx;
         firstVertex.prev = vtx;
         vtx.prev = lastVertex;
         vtx.next = firstVertex;
      }
   }

   public void prependVertex (Vertex2d vtx) {
      appendVertex (vtx);
      firstVertex = vtx;
   }

   public boolean isEmpty() {
      return firstVertex == null;
   }

   public Vertex2d getLastVertex() {
      return firstVertex != null ? firstVertex.prev : null;
   }

   public Vertex2d getFirstVertex() {
      return firstVertex;
   }

   void removeVertex (Vertex2d vtx) {
      if (vtx == firstVertex) {
         if (vtx.next == vtx) {
            firstVertex = null;
            return;
         }
         else {
            firstVertex = vtx.next;
         }
      }
      vtx.prev.next = vtx.next;
      vtx.next.prev = vtx.prev;
   }

   public void clear() {
      firstVertex = null;
   }

   public void shiftFirstVertex () {
      if (firstVertex != null) {
         firstVertex = firstVertex.next;
      }
   }

   public void getCentroid (Point2d pnt) {
      int nverts = 0;
      pnt.setZero();
      Vertex2d vtx = firstVertex;
      if (vtx != null) {
         do {
            pnt.add (vtx.pnt);
            nverts++;
            vtx = vtx.next;
         }
         while (vtx != firstVertex);
      }
      pnt.scale (1 / (double)nverts);
   }

   public double computeLength () {
      double len = 0;
      Vertex2d vtx = firstVertex;
      if (vtx != null) {
         do {
            len += vtx.distance (vtx.next);
            vtx = vtx.next;
         }
         while (vtx != firstVertex);
      }
      return len;
   }

   public double computeArea () {
      return computeAreaIntegrals (null, null);
   }

   /**
    * Computes the area integrals of this polygon. The code is based on the
    * surface integral approach used in "Fast and Accurate Computation of
    * Polyhedral Mass Properties," Brian Mirtich, journal of graphics tools,
    * volume 1, number 2, 1996. For polygons, the computations are simpler and
    * can be done by traversing the polygonal boundary.
    *
    * <p>The polygon can be ordered either clockwise or counter-clockwise.
    * 
    * @param moa1
    * if non-null, returns the first moment of area
    * @param moa2
    * if non-null, returns the second moment of area (in x and y),
    * and the product of area (in z)l
    * @return area of the polygon
    */  
   public double computeAreaIntegrals (Vector2d moa1, Vector3d moa2) {
      double area = 0;
      Vertex2d vtx = firstVertex;


      if (moa1 != null) {
         moa1.setZero();
      }
      if (moa2 != null) {
         moa2.setZero();
      }

      if (vtx != null) {
         do {
            // (x0,y0) and (x1,y1) give the end points of the edge
            double x0 = vtx.getPosition().x;
            double y0 = vtx.getPosition().y;
            double x1 = vtx.next.getPosition().x;
            double y1 = vtx.next.getPosition().y;

            // (nx,ny) give the non-normalized outward-facing edge normal
            double nx = (y1-y0);
            double ny = -(x1-x0);

            area += nx*(x0+x1)/2;

            double x0sqr = x0*x0;
            double x1sqr = x1*x1;
            double x0x1 = x0*x1;

            double y0sqr = y0*y0;
            double y1sqr = y1*y1;
            double y0y1 = y0*y1;

            if (moa1 != null) {
               moa1.x += nx*(x1sqr + x0x1 + x0sqr);
               moa1.y += ny*(y1sqr + y0y1 + y0sqr);
            }
            if (moa2 != null) {
               moa2.x += nx*(x1sqr*x1 + x0x1*(x1+x0) + x0sqr*x0);
               moa2.y += ny*(y1sqr*y1 + y0y1*(y1+y0) + y0sqr*y0);
               moa2.z +=
                  nx*(x1sqr*(y0+3*y1) + x0sqr*(y1+3*y0) + 2*x0x1*(y0+y1))/2;
            }

            vtx = vtx.next;
         }
         while (vtx != firstVertex);

         double scale = 1;
         if (area < 0) {
            // clockwise polygon - negate the results.
            area = -area;
            scale = -1;
         }

         if (moa1 != null) {
            moa1.scale (scale/6.0);
         }
         if (moa2 != null) {
            moa2.scale (scale/12.0);
         }
      }
      return area;
   }

   public void set (Polygon2d poly) {
      clear();
      Vertex2d vtx = poly.firstVertex;
      if (vtx != null) {
         do {
            appendVertex (new Vertex2d (vtx.pnt));
            vtx = vtx.next;
         }
         while (vtx != poly.firstVertex);
      }
   }

   public void set (double[] coords, int numVertices) {
      clear();
      if (coords.length < numVertices * 2) {
         throw new IllegalArgumentException (
            "not enough coords to specify all vertices");
      }
      if (numVertices < 2) {
         throw new IllegalArgumentException (
            "number of vertices must be at least two");
      }
      for (int i = 0; i < numVertices; i++) {
         appendVertex (new Vertex2d (coords[i * 2 + 0], coords[i * 2 + 1]));
      }
   }

   public void set (Point2d[] pnts, int numVertices) {
      clear();
      if (pnts.length < numVertices) {
         throw new IllegalArgumentException (
            "not enough points to specify all vertices");
      }
      if (numVertices < 2) {
         throw new IllegalArgumentException (
            "number of vertices must be at least two");
      }
      for (int i = 0; i < numVertices; i++) {
         appendVertex (new Vertex2d (pnts[i]));
      }
   }

   public void set (Collection<? extends Point2d> pnts) {
      clear();
      if (pnts.size() < 2) {
         throw new IllegalArgumentException (
            "number of vertices must be at least two");
      }
      for (Point2d p : pnts) {
         appendVertex (new Vertex2d (p));
      }
   }

   public void set (Collection<? extends Point3d> pnts, RigidTransform3d TPW) {
      clear();
      if (pnts.size() < 2) {
         throw new IllegalArgumentException (
            "number of vertices must be at least two");
      }
      Point3d p2d = new Point3d();
      for (Point3d p : pnts) {
         p2d.inverseTransform (TPW, p);
         appendVertex (new Vertex2d (p2d.x, p2d.y));
      }
   }

   public String toString() {
      return toString (new NumberFormat ("%g"));
   }

   public String toString (String fmtStr) {
      return toString (new NumberFormat (fmtStr));
   }

   public String toString (NumberFormat fmt) {
      StringBuffer sbuf = new StringBuffer (64);
      Vertex2d vtx = firstVertex;
      if (vtx != null) {
         do {
            sbuf.append (
               fmt.format (vtx.pnt.x) + " " + fmt.format (vtx.pnt.y) + "\n");
            vtx = vtx.next;
         }
         while (vtx != firstVertex);
      }
      return sbuf.toString();
   }

   public void scan (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken ('[');

      clear();
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         double x = rtok.scanNumber();
         double y = rtok.scanNumber();
         appendVertex (new Vertex2d (x, y));
      }
   }

   private boolean loopEqual (Vertex2d startVtx1, Vertex2d startVtx2, double eps) {
      Vertex2d vtx1 = startVtx1;
      Vertex2d vtx2 = startVtx2;
      do {
         if ((eps == 0 && !vtx1.pnt.equals (vtx2.pnt)) ||
             (eps != 0 && !vtx1.pnt.epsilonEquals (vtx2.pnt, eps))) {
            return false;
         }
         vtx1 = vtx1.next;
         vtx2 = vtx2.next;
      }
      while (vtx1 != startVtx1);
      return true;
   }

   /**
    * Returns true if this polygon is equal to another polygon within a
    * prescribed tolerance eps. The two polyons are considered to be equal if
    * their vertex lists are equal (within the specified tolerance) except for
    * possibly being shifted with respect to each other.
    * 
    * @param poly
    * polygon to be compared with
    * @param eps
    * tolerance value
    * @return true if the polygons are equal
    * @see #epsilonEquals
    */
   public boolean epsilonEquals (Polygon2d poly, double eps) {
      if (numVertices() != poly.numVertices()) {
         return false;
      }
      else if (isEmpty() && poly.isEmpty()) {
         return true;
      }
      else {
         Vertex2d startVtx1 = firstVertex;
         do {
            if (loopEqual (startVtx1, poly.firstVertex, eps)) {
               return true;
            }
            startVtx1 = startVtx1.next;
         }
         while (startVtx1 != firstVertex);
         return false;
      }
   }

   /**
    * Returns true if this polygon is equal to another polygon. The two polyons
    * are considered to be equal if their vertex lists are equal except for
    * possibly being shifted with respect to each other.
    * 
    * @param poly
    * polygon to be compared with
    * @return true if the polygons are equal
    * @see #epsilonEquals
    */
   public boolean equals (Polygon2d poly) {
      return epsilonEquals (poly, 0);
   }

   public void shiftVertices (int n) {
      Vertex2d vtx = firstVertex;
      if (vtx != null) {
         if (n > 0) {
            while (n-- > 0) {
               vtx = vtx.prev;
            }
         }
         else {
            while (n++ < 0) {
               vtx = vtx.next;
            }
         }
         firstVertex = vtx;
      }
   }

   public boolean isConsistent() {
      Vertex2d vtx = firstVertex;
      if (vtx != null) {
         do {
            if (vtx.next.prev != vtx) {
               return false;
            }
            vtx = vtx.next;
         }
         while (vtx != firstVertex);
      }
      return true;
   }

   /**
    * Applies a affine transformation to the vertices of this polygon.
    * 
    * @param X
    * affine transformation
    */
   public void transform (AffineTransform2dBase X) {
      Vertex2d vtx = firstVertex;
      if (vtx != null) {
         do {
            vtx.pnt.transform (X);
            vtx = vtx.next;
         }
         while (vtx != firstVertex);
      }
   }

   /**
    * Applies an inverse affine transformation to the vertices of this polygon.
    * 
    * @param X
    * affine transformation
    */
   public void inverseTransform (AffineTransform2dBase X) {
      Vertex2d vtx = firstVertex;
      if (vtx != null) {
         do {
            vtx.pnt.inverseTransform (X);
            vtx = vtx.next;
         }
         while (vtx != firstVertex);
      }
   }

   // implementation of renderable

   public void prerender (RenderList list) {
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      Vertex2d vtx = firstVertex;
      if (vtx != null) {
         Point3d tmp = new Point3d();
         do {
            tmp.set (vtx.pnt.x, vtx.pnt.y, 0);
            tmp.updateBounds (pmin, pmax);
            vtx = vtx.next;
         }
         while (vtx != firstVertex);
      }
   }

   public void render (Renderer renderer, int flags) {
      render (renderer, myRenderProps, /*flags=*/0);
   }

   public void render (Renderer renderer, RenderProps props, int flags) {

      renderer.setShading (Shading.NONE);

      renderer.setLineWidth (myRenderProps.getLineWidth());
      renderer.setLineColoring (props, /*highlight=*/false);
      //renderer.beginDraw (VertexDrawMode.LINE_STRIP);
      renderer.beginDraw (DrawMode.LINE_LOOP);
      Vertex2d vtx = firstVertex;
      if (vtx != null) {
         do {
            renderer.addVertex (vtx.pnt.x, vtx.pnt.y, 0);
            vtx = vtx.next;
         }
         while (vtx != firstVertex);
      }
      renderer.endDraw();
      renderer.setLineWidth (1);

      renderer.setShading (Shading.FLAT);
   }

   public int getRenderHints() {
      return 0;
   }

   public RenderProps createRenderProps() {
      return new PointLineRenderProps();
   }

   public void setRenderProps (RenderProps props) {
      if (props == null) {
         throw new IllegalArgumentException ("Render props cannot be null");
      }
      myRenderProps = createRenderProps();
      myRenderProps.set (props);      
   }

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public boolean isSelectable() {
      return false;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }

   private double xprod (Point2d p0, Point2d p1, Point2d p2) {
      double d1x = p1.x - p0.x;
      double d1y = p1.y - p0.y;
      double d2x = p2.x - p0.x;
      double d2y = p2.y - p0.y;

      return (d1x*d2y - d2x*d1y);
   }

   private boolean leftTurn (Point2d p0, Point2d p1, Point2d p2) {
      return xprod (p0, p1, p2) > 0;
   }
   
   private boolean rightTurn (Point2d p0, Point2d p1, Point2d p2) {
      return xprod (p0, p1, p2) < 0;
   }
   
   private ConvexPolygon2d listToHull (LinkedList<Point2d> list) {
      int numv = list.size()-1;
      if (numv < 1) {
         return new ConvexPolygon2d();
      }
      Point2d[] pnts = new Point2d[numv];
      ListIterator<Point2d> it = list.listIterator(list.size());
      it.previous(); // discard first vertex
      int k = 0;
      while (it.hasPrevious()) {
         pnts[k++] = it.previous();
      }
      return new ConvexPolygon2d (pnts);
   }

   /**
    * Computes the convex hull of this Polygon2d, under the assumption that it
    * describes a simple polygon. The result is returned as a ConvexPolygon2d,
    * with vertices arranged counter-clockwise.
    *
    * <p>The method uses the O(n) time algorithm described by A Melkman, "On-Li
    * Construction of the Convex Hull of a Simple Polyline", Information
    * Processing Letters (1987). The implementation does not use robust
    * predicates and so may produce results that are inaccurate at a
    * machine-precision level.
    */
   public ConvexPolygon2d simpleConvexHull() {

      if (numVertices() <= 2) {
         ConvexPolygon2d poly = new ConvexPolygon2d();
         poly.set (this);
         return poly;
      }
      // We use a list to implement the deque of the Melkman algorithm, with
      // with add/remove/getFirst() used the access the bottom of the deque and
      // add/remove/getLast() used to access the top.
      LinkedList<Point2d> list = new LinkedList<>();
      
      Point2d[] pnts = getPoints();
      int pidx = 0;
      Point2d p0 = pnts[pidx++];
      Point2d p1 = pnts[pidx++];
      Point2d p2 = pnts[pidx++];

      if (rightTurn (p0, p1, p2)) {
         list.addLast (p0);
         list.addLast (p1);
      }
      else {
         list.addLast (p1);
         list.addLast (p0);
      }
      list.addLast (p2);
      list.addFirst (p2);
      int nv = 3;
      while (pidx < pnts.length) {
         Point2d p = pnts[pidx++];
         while (!leftTurn (p, list.get(0), list.get(1)) &&
                !leftTurn (list.get(nv-1), list.get(nv), p)) {
            if (pidx < pnts.length) {
               p = pnts[pidx++];
            }
            else {
               return listToHull (list);
            }
         }
         while (!rightTurn (list.get(nv-1), list.get(nv), p)) {
            Point2d pr = list.removeLast();
            nv--;
         }
         list.addLast (p);
         nv++;
         while (!rightTurn (p, list.get(0), list.get(1))) {
            Point2d pr = list.removeFirst();
            nv--;
         }
         list.addFirst (p);
         nv++;
      }
      return listToHull (list);
   }

   protected static int counterClockwise (
      Point2d p0, Point2d p1, Point2d p2, double angTol) {
      
      double d1x = p1.x-p0.x;
      double d1y = p1.y-p0.y;
      double d2x = p2.x-p0.x;
      double d2y = p2.y-p0.y;
      double cross = d1x*d2y - d1y*d2x;

      if (angTol > 0) {
         // adjust tolerance to use approximate magnitudes of d1 and d2
         double d1mag = Math.abs (d1x) + Math.abs (d1y);
         double d2mag = Math.abs (d2x) + Math.abs (d2y);
         angTol *= d1mag*d2mag;
      }
      if (cross > angTol) {
         return 1;
      }
      else if (cross < -angTol) {
         return -1;
      }
      else {
         if (d1x*d2x + d1y*d2y > 0) {
            return 0;
         }
         else {
            return -1;
         }
      }
   }      

   /**
    * Computes the convex hull of a list of points, under the assumption that
    * they describe a simple polygon. The result is returned as a
    * ConvexPolygon2d, with vertices arranged counter-clockwise.
    *
    * <p>The method is the same as that used by {@link #simpleConvexHull()}.
    */
   public static ConvexPolygon2d simpleConvexHull (Collection<Point2d> pnts) {
      Polygon2d poly = new Polygon2d(pnts);
      return poly.simpleConvexHull();
   }

   /**
    * Returns {@code true} if this 2d polygon is convex.
    */
   public boolean isConvex () {
      return isConvex (0);
   }

   /**
    * Returns {@code true} if this 2d polygon is convex, within a presribed
    * angular tolerance.
    */  
   public boolean isConvex (double angTol) {
      Vertex2d vtx = firstVertex;
      if (vtx != null) {
         Vertex2d vprev = vtx.prev;
         do {
            Vertex2d vnext = vtx.next;
            if (counterClockwise (vprev.pnt, vtx.pnt, vnext.pnt, angTol) < 0) {
               return false;
            }
            vprev = vtx;
            vtx = vnext;
         }
         while (vtx != firstVertex);
         return true;
      }
      else {
         return false;
      }
   }
}
