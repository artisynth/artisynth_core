/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import maspack.matrix.AffineTransform2dBase;
import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.PointLineRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public class Polygon2d implements Renderable {
   Vertex2d firstVertex;

   RenderProps myRenderProps = new PointLineRenderProps();

   private class VertexIterator implements ListIterator {
      Vertex2d next;
      Vertex2d prev;
      int index;

      VertexIterator() {
         next = firstVertex;
         prev = null;
         index = -1;
      }

      public void add (Object obj) throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
      }

      public boolean hasNext() {
         return next != null;
      }

      public boolean hasPrevious() {
         return prev != null;
      }

      public Object next() throws NoSuchElementException {
         if (next == null) {
            throw new NoSuchElementException();
         }
         index++;
         prev = next;
         next = next.next;
         return prev;
      }

      public Object previous() throws NoSuchElementException {
         if (prev == null) {
            throw new NoSuchElementException();
         }
         index--;
         next = prev;
         prev = prev.prev;
         return next;
      }

      public int previousIndex() {
         return index;
      }

      public int nextIndex() {
         return index + 1;
      }

      public void remove() throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
      }

      public void set (Object obj) throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
      }
   }

   public Polygon2d() {
      firstVertex = null;
   }

   public ListIterator getVertices() {
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
   
   private Polygon2d listToHull (LinkedList<Point2d> list) {
      int numv = list.size()-1;
      if (numv < 1) {
         return new Polygon2d();
      }
      Point2d[] pnts = new Point2d[numv];
      Iterator<Point2d> it = list.iterator();
      it.next(); // discard first vertex
      for (int k=0; k<numv; k++) {
         pnts[k] = it.next();
      }
      return new Polygon2d (pnts);
   }

   /**
    * Computes the convex hull of this Polygon2d, under the assumption that it
    * describes a simple polygon. The result is returned as another Polygon2d,
    * with vertices arranged clockwise.
    *
    * <p>The method uses the O(n) time algorithm described by A Melkman, "On-Li
    * Construction of the Convex Hull of a Simple Polyline", Information
    * Processing Letters (1987). The implementation does not use robust
    * predicates and so may produce results that are inaccurate at a
    * machine-precision level.
    */
   public Polygon2d simpleConvexHull() {

      if (numVertices() <= 2) {
         Polygon2d poly = new Polygon2d();
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

}
