/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;
import maspack.util.NumberFormat;

import java.util.ListIterator;
import java.util.NoSuchElementException;

import maspack.util.ReaderTokenizer;
import java.io.IOException;

public class Polygon3d {
   PolygonVertex3d firstVertex;

   private class VertexIterator implements ListIterator {
      PolygonVertex3d next;
      PolygonVertex3d prev;
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
         if (next == firstVertex) {
            next = null;
         }
         return prev;
      }

      public Object previous() throws NoSuchElementException {
         if (prev == null) {
            throw new NoSuchElementException();
         }
         index--;
         next = prev;
         prev = prev.prev;
         if (prev == firstVertex) {
            prev = null;
         }
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

   public Polygon3d() {
      firstVertex = null;
   }

   public ListIterator getVertices() {
      return new VertexIterator();
   }

   public int numVertices() {
      int num = 0;
      PolygonVertex3d vtx = firstVertex;
      if (vtx != null) {
         do {
            vtx = vtx.next;
            num++;
         }
         while (vtx != firstVertex);
      }
      return num;
   }

   public Polygon3d (double[] coords) {
      set (coords, coords.length / 2);
   }

   public Polygon3d (Point3d[] pnts) {
      set (pnts, pnts.length);
   }

   public double getMaxCoordinate() {
      PolygonVertex3d vtx = firstVertex;

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

   public void getBounds (Point3d minValues, Point3d maxValues) {
      PolygonVertex3d vtx = firstVertex;

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
            if (vtx.pnt.z < minValues.z) {
               minValues.z = vtx.pnt.z;
            }
            else if (vtx.pnt.z > maxValues.z) {
               maxValues.z = vtx.pnt.z;
            }
            vtx = vtx.next;
         }
      }
      else {
         minValues.setZero();
         maxValues.setZero();
      }
   }

   public void updateBounds (Point3d minValues, Point3d maxValues) {
      PolygonVertex3d vtx = firstVertex;

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
            if (vtx.pnt.z < minValues.z) {
               minValues.z = vtx.pnt.z;
            }
            else if (vtx.pnt.z > maxValues.z) {
               maxValues.z = vtx.pnt.z;
            }
            vtx = vtx.next;
         }
         while (vtx != firstVertex);
      }
   }

   public void addVertexAfter (PolygonVertex3d vtx, PolygonVertex3d ref) {
      vtx.next = ref.next;
      vtx.prev = ref;

      ref.next.prev = vtx;
      ref.next = vtx;
   }

   public void appendVertex (PolygonVertex3d vtx) {
      if (firstVertex == null) {
         firstVertex = vtx;
         vtx.prev = vtx;
         vtx.next = vtx;
      }
      else {
         PolygonVertex3d lastVertex = firstVertex.prev;
         lastVertex.next = vtx;
         firstVertex.prev = vtx;
         vtx.prev = lastVertex;
         vtx.next = firstVertex;
      }
   }

   public void prependVertex (PolygonVertex3d vtx) {
      appendVertex (vtx);
      firstVertex = vtx;
   }

   public boolean isEmpty() {
      return firstVertex == null;
   }

   public PolygonVertex3d getLastVertex() {
      return firstVertex != null ? firstVertex.prev : null;
   }

   public PolygonVertex3d getFirstVertex() {
      return firstVertex;
   }

   void removeVertex (PolygonVertex3d vtx) {
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

   void removeVertices (PolygonVertex3d vtx1, PolygonVertex3d vtx2) {
      PolygonVertex3d prevVtx = vtx1.prev;
      PolygonVertex3d nextVtx = vtx2.next;
      if (prevVtx == vtx2) {
         firstVertex = null;
         return;
      }
      prevVtx.next = nextVtx;
      nextVtx.prev = prevVtx;

      // reset firstVertex if necessary
      vtx2.next = null;
      for (PolygonVertex3d vtx = vtx1; vtx != null; vtx = vtx.next) {
         if (vtx == firstVertex) {
            firstVertex = prevVtx;
            break;
         }
      }
   }

   public void clear() {
      firstVertex = null;
   }

   public void set (Polygon3d poly) {
      clear();
      PolygonVertex3d vtx = poly.firstVertex;
      if (vtx != null) {
         do {
            appendVertex (new PolygonVertex3d (vtx.pnt));
            vtx = vtx.next;
         }
         while (vtx != poly.firstVertex);
      }
   }

   public void set (double[] coords, int numVertices) {
      clear();

      if (coords.length < numVertices * 3) {
         throw new IllegalArgumentException (
            "not enough coords to specify all vertices");
      }
      if (numVertices < 2) {
         throw new IllegalArgumentException (
            "number of vertices must be at least two");
      }
      for (int i = 0; i < numVertices; i++) {
         appendVertex (new PolygonVertex3d (
            coords[i * 3 + 0], coords[i * 3 + 1], coords[i * 3 + 2]));
      }
   }

   public void set (Point3d[] pnts, int numVertices) {
      if (pnts.length < numVertices) {
         throw new IllegalArgumentException (
            "not enough points to specify all vertices");
      }
      if (numVertices < 2) {
         throw new IllegalArgumentException (
            "number of vertices must be at least two");
      }
      for (int i = 0; i < numVertices; i++) {
         appendVertex (new PolygonVertex3d (pnts[i]));
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
      PolygonVertex3d vtx = firstVertex;
      if (vtx != null) {
         do {
            sbuf.append (
               fmt.format (vtx.pnt.x) + " " + fmt.format (vtx.pnt.y) +
               " " + fmt.format (vtx.pnt.z) + "\n");
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
         double z = rtok.scanNumber();
         appendVertex (new PolygonVertex3d (x, y, z));
      }
   }

   private boolean loopEqual (
      PolygonVertex3d startVtx1, PolygonVertex3d startVtx2, double eps) {
      PolygonVertex3d vtx1 = startVtx1;
      PolygonVertex3d vtx2 = startVtx2;
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
   public boolean epsilonEquals (Polygon3d poly, double eps) {
      if (numVertices() != poly.numVertices()) {
         return false;
      }
      else if (isEmpty() && poly.isEmpty()) {
         return true;
      }
      else {
         PolygonVertex3d startVtx1 = firstVertex;
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
   public boolean equals (Polygon3d poly) {
      return epsilonEquals (poly, 0);
   }

   void shiftVertices (int n) {
      PolygonVertex3d vtx = firstVertex;
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

   boolean isConsistent() {
      PolygonVertex3d vtx = firstVertex;
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
   public void transform (AffineTransform3dBase X) {
      PolygonVertex3d vtx = firstVertex;
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
   public void inverseTransform (AffineTransform3dBase X) {
      PolygonVertex3d vtx = firstVertex;
      if (vtx != null) {
         do {
            vtx.pnt.inverseTransform (X);
            vtx = vtx.next;
         }
         while (vtx != firstVertex);
      }
   }

}
