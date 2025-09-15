package maspack.geometry;

import java.util.*;

import org.poly2tri.*;
import org.poly2tri.triangulation.sets.PointSet;
import org.poly2tri.triangulation.sets.ConstrainedPointSet;
import org.poly2tri.triangulation.point.TPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

import maspack.util.*;
import maspack.matrix.*;

/**
 * Computes Delaunay and constrained Delaunay triangulations. It uses the
 * utility Poly2Tri, available from
 *
 * https://github.com/orbisgis/poly2tri.java
 *
 * and released under the following BSD 3-Clause license:
 *
 * <p> Copyright (c) 2015, Poly2Tri Contributors. All rights reserved.
 *
 * <p> Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * <ol>
 *
 * <li>Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * <li>Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * <li>Neither the name of Poly2Tri nor the names of its contributors may be
 * used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * </ol>
 *
 * <p> THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class DelaunayTriangulator {

   /**
    * Returns information about a specific Delaunay triangle.
    */
   public static class Triangle {

      int myIndex;
      IndexedPoint[] myVertices;
      Triangle[] myAdjacents;

      Triangle (TriangulationPoint[] vertices, int idx) {
         myVertices = new IndexedPoint[3];
         for (int i=0; i<3; i++) {
            myVertices[i] = (IndexedPoint)vertices[i];
         }
         myIndex= idx; 
      }

      void setAdjacents (Triangle[] adjacents) {
         myAdjacents = adjacents;
      }

      /**
       * Returns the triangle adjacent to the edge between vertices {@code vi}
       * and {@code (vi+1)%3}. Returns {@code null} if there is no adjacent
       * triangle.
       *
       * @param vi local vertex index, in the range 0 to 2
       * @return adjacent triangle, or {@code null}
       */
      public Triangle getAdjacentTriangle (int vi) {
         return myAdjacents[vi];
      }

      /**
       * Returns the index (with respect to all triangles in the triangulation)
       * of the triangle adjacent to the edge between vertices {@code vi} and
       * {@code (vi+1)%3}. Returns -1 if there is no adjacent triangle.
       *
       * @param vi local vertex index, in the range 0 to 2
       * @return adjacent triangle index, or -1

       */
      public int getAdjacentTriangleIndex (int vi) {
         if (myAdjacents[vi] == null) {
            return -1;
         }
         else {
            return myAdjacents[vi].myIndex;
         }
      }

      /**
       * Returns the point (from the input point set) for the {@code vi}-th
       * vertex in this triangle. Vertices are arranged counter-clockwise
       * around the triangle.
       *
       * @param vi local vertex index, in the range 0 to 2
       * @return input point associated with the vertex
       */
      public Vector2d getPoint (int vi) {
         return myVertices[vi].myPoint;
      }

      /**
       * Returns the index (with respect to the original point set)
       * corresponding to the {@code vi}-th vertex in this triangle. Vertices
       * are arranged counter-clockwise around the triangle.
       *
       * @param vi local vertex index, in the range 0 to 2.
       * @return index of the vertex input point, or -1
       */
      public int getPointIndex (int vi) {
         return myVertices[vi].myIndex;
      }

      /**
       * Returns the index of this triangle with respect to all triangles
       * in the triangulation.
       *
       * @return triangle index
       */
      public int getIndex() {
         return myIndex;
      }
   }

   static class IndexedPoint extends TPoint {

      private int myIndex;
      private Vector2d myPoint;

      public IndexedPoint (Vector2d pnt, int idx) {
         super (pnt.x, pnt.y);
         myPoint = pnt;
         myIndex = idx;
      }

      public int getIndex() {
         return myIndex;
      }

      public Vector2d getPoint() {
         return myPoint;
      }
   }

   private static List<Triangle> createTriangles (List<DelaunayTriangle> dtris) {
      LinkedHashMap<DelaunayTriangle,Triangle> triMap = new LinkedHashMap<>();
      int tidx = 0;
      for (DelaunayTriangle dtri : dtris) {
         Triangle tri = new Triangle (dtri.points, tidx++);
         triMap.put (dtri, tri);
      }
      for (DelaunayTriangle dtri : dtris) {
         Triangle[] adjacents = new Triangle[3];
         for (int i=0; i<3; i++) {
            adjacents[(i+1)%3] = triMap.get(dtri.neighbors[i]);
         }
         triMap.get(dtri).setAdjacents (adjacents);
      }
      ArrayList<Triangle> tris = new ArrayList<>();
      tris.addAll (triMap.values());
      return tris;
   }

   /**
    * Computes the Delaunay triangulation of a set of 2D points. The triangles
    * are returned as a list of {@link Triangle} structures.
    *
    * <p>As indicated in the documentation for Poly2Tri, points should be
    * unique within machine precision.
    * 
    * @param points points to compute the triangulation for
    * @return list of triangles
    */
   public static List<Triangle> triangulate (List<? extends Vector2d> points) {
      ArrayList<TriangulationPoint> tpnts = new ArrayList<>();
      for (int i=0; i<points.size(); i++) {
         tpnts.add (new IndexedPoint (points.get(i), i));
      }
      PointSet pset = new PointSet (tpnts);
      Poly2Tri.triangulate (pset);
      return createTriangles (pset.getTriangles());
   }

   /**
    * Computes the constrained Delaunay triangulation of a set of 2D
    * points. The constraints are indicated as an array of point indices
    * defining the constraint edges. If there are {@code numc} constraints,
    * then this array should have a length of {@code 2*numc}, with two point
    * indices per constraint. The triangles are returned as a list of {@link
    * Triangle} structures.
    *
    * <p>As indicated in the documentation for Poly2Tri, points should be
    * unique within machine precision, and no check is made to ensure that the
    * constraints constitute a valid constraint set (e.g., no check is made to
    * ensure that constraint edges do not cross eah other).
    * 
    * @param points points to compute the triangulation for
    * @param constraints if non-null, specifices indices of the points defining
    * the constrained edges
    * @return list of triangles
    */
   public static List<Triangle> triangulate (
      List<? extends Vector2d> points, int[] constraints) {

      if (constraints == null) {
         return triangulate (points);
      }
      if ((constraints.length%2) != 0) {
         throw new IllegalArgumentException (
            "length of 'constraints' is not even");
      }
      for (int i=0; i<constraints.length; i++) {
         if (constraints[i] >= points.size() || constraints[i] < 0) {
            throw new IllegalArgumentException (
               "constraints["+i+"] = " + constraints[i] +
               ", which is out of the point index range 0 to " +
               points.size());
         }
      }
      ArrayList<TriangulationPoint> tpnts = new ArrayList<>();
      for (int i=0; i<points.size(); i++) {
         tpnts.add (new IndexedPoint (points.get(i), i));
      }
      ConstrainedPointSet pset = new ConstrainedPointSet (tpnts, constraints);
      Poly2Tri.triangulate (pset);
      return createTriangles (pset.getTriangles());
   }

   private static PolygonalMesh createMesh (
      List<Triangle> triangles, List<? extends Vector2d> points) {
      PolygonalMesh mesh = new PolygonalMesh();
      for (Vector2d pnt : points) {
         mesh.addVertex (new Point3d(pnt.x, pnt.y, 0));
      }
      for (Triangle tri : triangles) {
         mesh.addFace (
            tri.getPointIndex(0),
            tri.getPointIndex(1),
            tri.getPointIndex(2));         
      }
      return mesh;
   }

   /**
    * Creates a PolygonalMesh, in the x-y plane, from the Delaunay
    * triangulation of a set of 2D points.
    *
    * <p>As indicated in the documentation for Poly2Tri, points should be
    * unique within machine precision.
    * 
    * @param points points to compute the triangulation for
    * @return mesh created from the triangulation
    */
   public static PolygonalMesh createMesh (List<? extends Vector2d> points) {
      return createMesh (triangulate (points, null), points);
   }

   /**
    * Creates a PolygonalMesh, in the x-y plane, from the constrained Delaunay
    * triangulation of a set of 2D points. The constraints are indicated as an
    * array of point indices defining the constraint edges. If there are {@code
    * numc} constraints, then this array should have a length of {@code
    * 2*numc}, with two point indices per constraint.
    *
    * <p>As indicated in the documentation for Poly2Tri, points should be
    * unique within machine precision, and no check is made to ensure that the
    * constraints constitute a valid constraint set (e.g., no check is made to
    * ensure that constraint edges do not cross eah other).
    * 
    * @param points points to compute the triangulation for
    * @param constraints if non-null, specifices indices of the points defining
    * the constrained edges
    * @return mesh created from the triangulation
    */
   public static PolygonalMesh createMesh (
      List<? extends Vector2d> points, int[] constraints) {
      return createMesh (triangulate (points, constraints), points);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      ArrayList<Vector2d> pnts = new ArrayList<>();
      int npts = 3;
      for (int i=0; i<npts; i++) {
         Vector2d vec = new Vector2d();
         vec.setRandom();
         pnts.add (vec);
      }
      //List<Triangle> tris = triangulate (pnts, new int[] {0, 1, 1, 2});
      List<Triangle> tris = triangulate (pnts);
      int k = 0;
      System.out.println ("points:");
      for (Vector2d pnt : pnts) {
         System.out.println (" "+pnt.toString ("%8.5f"));
      }
      for (Triangle tri : tris) {
         System.out.print ("tri:");
         for (int vi=0; vi<3; vi++) {
            System.out.print (" "+tri.getPointIndex(vi));
         }
         System.out.println ("");
      }
   }



}
