package maspack.geometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import maspack.matrix.Vector3d;

public class BSPTree {


   // Adapted from evanw CSG library: https://github.com/evanw/csg.js/
   /*
    * Copyright (c) 2011 Evan Wallace (http://madebyevan.com/)
    *
    * Permission is hereby granted, free of charge, to any person obtaining a copy 
    * of this software and associated documentation files (the "Software"), to 
    * deal in the Software without restriction, including without limitation the
    * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
    * sell copies of the Software, and to permit persons to whom the Software is 
    * furnished to do so, subject to the following conditions:
    *
    * The above copyright notice and this permission notice shall be included in 
    * all copies or substantial portions of the Software.
    *
    * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
    * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
    * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
    * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
    * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
    * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
    * DEALINGS IN THE SOFTWARE.
    */

   // Constructive Solid Geometry (CSG) is a modeling technique that uses Boolean
   // operations like union and intersection to combine 3D solids. This library
   // implements CSG operations on meshes elegantly and concisely using BSP trees,
   // and is meant to serve as an easily understandable implementation of the
   // algorithm. All edge cases involving overlapping coplanar polygons in both
   // solids are correctly handled.
   // 
   // 
   // ## Implementation Details
   // 
   // All CSG operations are implemented in terms of two functions, `clipTo()` and
   // `invert()`, which remove parts of a BSP tree inside another BSP tree and swap
   // solid and empty space, respectively. To find the union of `a` and `b`, we
   // want to remove everything in `a` inside `b` and everything in `b` inside `a`,
   // then combine polygons from `a` and `b` into one solid:
   // 
   //     a.clipTo(b);
   //     b.clipTo(a);
   //     a.build(b.allPolygons());
   // 
   // The only tricky part is handling overlapping coplanar polygons in both trees.
   // The code above keeps both copies, but we need to keep them in one tree and
   // remove them in the other tree. To remove them from `b` we can clip the
   // inverse of `b` against `a`. The code for union now looks like this:
   // 
   //     a.clipTo(b);
   //     b.clipTo(a);
   //     b.invert();
   //     b.clipTo(a);
   //     b.invert();
   //     a.build(b.allPolygons());
   // 
   // Subtraction and intersection naturally follow from set operations. If
   // union is `A | B`, subtraction is `A - B = ~(~A | B)` and intersection is
   // `A & B = ~(~A | ~B)` where `~` is the complement operator.
   // 
   // ## License
   // 
   // Copyright (c) 2011 Evan Wallace (http://madebyevan.com/), under the MIT license.

   // # class Polygon
   // Represents a convex polygon. The vertices used to initialize a polygon must
   // be coplanar and form a convex loop. They do not have to be `CSG.Vertex`
   // instances but they must behave similarly (duck typing can be used for
   // customization).
   // 
   // Each convex polygon has a `shared` property, which is shared between all
   // polygons that are clones of each other or were split from the same polygon.
   // This can be used to define per-polygon properties (such as surface color).
   private static class Polygon {
      Vertex3d[] verts;
      Plane plane;

      public Polygon(List<? extends Vertex3d> verts) {
         this.verts = verts.toArray(new Vertex3d[verts.size()]);

         plane = Plane.fromPoints(this.verts[0], this.verts[1], this.verts[2]);
      }

      public Polygon(Vertex3d[] verts ) {
         this.verts = verts.clone();
         plane = Plane.fromPoints(verts[0], verts[1], verts[2]);
      }

      public Polygon clone() {
         return new Polygon(verts);
      }

      public void flip() {
         Vertex3d tmpVtx;
         int n = verts.length;
         for (int i=0; i<(n/2); i++) {
            tmpVtx = verts[i];
            verts[i] = verts[n-i-1];
            verts[n-i-1] = tmpVtx;
         }
         plane.flip();
      }
   }

   // # class Plane
   private static class Plane {
      Vector3d normal;
      double EPSILON = 1e-5;
      double w;

      public Plane(Vector3d nrm, double w) {
         normal = new Vector3d(nrm);
         this.w = w;
      }
      public void flip() {
         normal.negate();
         w = -w;
      }

      public Plane clone() {
         return new Plane(normal,w);
      }

      public static Plane fromPoints(Vertex3d a, Vertex3d b, Vertex3d c) {
         Vector3d ba = new Vector3d(b.getPosition());
         ba.sub(a.getPosition());
         Vector3d ca = new Vector3d(c.getPosition());
         ca.sub(a.getPosition());
         Vector3d normal = new Vector3d();
         normal.cross(ba, ca);
         normal.normalize();
         return new Plane(normal, normal.dot(a.getPosition()));
      }


      // Split `polygon` by this plane if needed, then put the polygon or polygon
      // fragments in the appropriate lists. Coplanar polygons go into either
      // `coplanarFront` or `coplanarBack` depending on their orientation with
      // respect to this plane. Polygons in front or in back of this plane go into
      // either `front` or `back`.      
      public void splitPolygon(Polygon poly, ArrayList<Polygon> coplanarFront,
         ArrayList<Polygon> coplanarBack, ArrayList<Polygon> front, 
         ArrayList<Polygon> back) {

         final int COPLANAR = 0;
         final int FRONT = 1;
         final int BACK = 2;
         final int SPANNING = 3;

         // Classify each point as well as the entire polygon into one of the above
         // four classes.         
         int pType = 0;
         int vTypes[] = new int[poly.verts.length];

         for (int i=0; i<poly.verts.length; i++) {
            double t = this.normal.dot(poly.verts[i].getPosition()) - this.w;
            int type;
            if (t < -EPSILON) {
               type = BACK;
            } else if (t > EPSILON) {
               type = FRONT;
            } else {
               type = COPLANAR;
            }
            vTypes[i] = type;
            pType |= type;
         }

         switch(pType) {
            case COPLANAR:
               if (this.normal.dot(poly.plane.normal) > 0) {
                  coplanarFront.add(poly);
               } else {
                  coplanarBack.add(poly);
               }
               break;
            case FRONT:
               front.add(poly);
               break;
            case BACK:
               back.add(poly);
               break;
            case SPANNING:
               ArrayList<Vertex3d> f = new ArrayList<Vertex3d>();
               ArrayList<Vertex3d> b = new ArrayList<Vertex3d>();

               for (int i=0; i<poly.verts.length; i++) {
                  int j = (i+1)%poly.verts.length;
                  int ti = vTypes[i];
                  int tj = vTypes[j];
                  Vertex3d vi = poly.verts[i];
                  Vertex3d vj = poly.verts[j];

                  if (ti != BACK) {
                     f.add(vi);
                  }
                  if (ti != FRONT) {
                     if (ti != BACK) {
                        b.add(vi.clone());  // was clone
                     } else {
                        b.add(vi);
                     }
                  }
                  if ((ti | tj)==SPANNING) {
                     Vector3d diff = new Vector3d(vj.getPosition());
                     diff.sub(vi.getPosition());
                     double t = (this.w - this.normal.dot(vi.getPosition())) / this.normal.dot(diff);
                     Vertex3d v = vi.interpolate(t, vj);
                     f.add(v);
                     b.add(v.clone()); // was clone
                  }

               }
               if (f.size() >= 3) {
                  front.add(new Polygon(f));
               }
               if (b.size() >= 3) {
                  back.add(new Polygon(b));
               }
               break;

         }

      }

   }


   // Holds a node in a BSP tree. A BSP tree is built from a collection of polygons
   // by picking a polygon to split along. That polygon (and all other coplanar
   // polygons) are added directly to that node and the other polygons are added to
   // the front and/or back subtrees. This is not a leafy BSP tree since there is
   // no distinction between internal and leaf nodes.
   private static class Node {
      Plane plane;
      Node front;
      Node back;
      ArrayList<Polygon> poly;

      public Node() {
         poly = new ArrayList<Polygon>();
      }

      public Node(ArrayList<Polygon> poly) {
         this();
         build(poly);
      }

      public Node clone() {
         Node node = new Node();
         node.plane = this.plane.clone();
         if (front != null) {
            node.front = this.front.clone();
         } else {
            node.front = null;
         }
         if (back != null) {
            node.back = this.back.clone();
         } else {
            node.back = null;
         }            
         node.poly = new ArrayList<Polygon>(poly.size());
         for (Polygon p : poly) {
            node.poly.add(p.clone());
         }
         return node;
      }

      // Convert solid space to empty space and empty space to solid space.
      public void invert () {
         for (Polygon p : poly) {
            p.flip();
         }
         plane.flip();
         if (this.front != null) {
            front.invert();
         }
         if (this.back != null) {
            back.invert();
         }
         Node tmp = this.front;
         this.front = this.back;
         this.back = tmp;
      }

      // Recursively remove all polygons in `polygons` that are inside this BSP
      // tree.
      public ArrayList<Polygon> clipPolygons(ArrayList<Polygon> polygons) {
         ArrayList<Polygon> frontP = new ArrayList<Polygon>();
         ArrayList<Polygon> backP = new ArrayList<Polygon>();

         if (this.plane == null) {
            frontP.addAll(polygons);
            return frontP;
         }

         for (Polygon poly : polygons) {
            this.plane.splitPolygon(poly, frontP, backP, frontP, backP);
         }
         if (this.front != null) {
            frontP = this.front.clipPolygons(frontP);
         }
         if (this.back != null) {
            backP = this.back.clipPolygons(backP);
         } else {
            backP.clear(); 
         }
         frontP.addAll(backP);
         return frontP; 
      }

      //  Remove all polygons in this BSP tree that are inside the other BSP tree
      // `bsp`.
      public void clipTo(Node bsp) {
         this.poly = bsp.clipPolygons(this.poly);
         if (this.front != null) {
            this.front.clipTo(bsp);
         }
         if (this.back != null) {
            this.back.clipTo(bsp);
         }
      }

      // Return a list of all polygons in this BSP tree.
      public ArrayList<Polygon> allPolygons() {
         ArrayList<Polygon> polygons = new ArrayList<Polygon>(poly.size());
         polygons.addAll(poly);
         if (this.front != null) {
            polygons.addAll(this.front.allPolygons());
         }
         if (this.back != null) {
            polygons.addAll(this.back.allPolygons());
         }
         return polygons;
      }

      // Build a BSP tree out of `polygons`. When called on an existing tree, the
      // new polygons are filtered down to the bottom of the tree and become new
      // nodes there. Each set of polygons is partitioned using the first polygon
      // (no heuristic is used to pick a good split).
      public void build( ArrayList<Polygon> polygons) {

         if (polygons.size() == 0) {
            return;
         }

         if (this.plane == null) { 
            this.plane = polygons.get(0).plane.clone();
         }

         ArrayList<Polygon> frontP = new ArrayList<Polygon>();
         ArrayList<Polygon> backP = new ArrayList<Polygon>();

         for (Polygon poly : polygons) {
            this.plane.splitPolygon(poly, this.poly, this.poly, frontP, backP);
         }

         if (frontP.size() > 0) {
            if (this.front == null) { 
               this.front = new Node();
            }
            this.front.build(frontP);
         }
         if (backP.size() > 0) {
            if (this.back == null)  {
               this.back = new Node();
            }
            this.back.build(backP);
         }
      }

   }


   private ArrayList<Polygon> myPolygons;

   public BSPTree() {
      myPolygons = new ArrayList<Polygon>();
   }
   public BSPTree(PolygonalMesh mesh) {
      this();
      setFromMesh(mesh);
   }

   public void setFromMesh(PolygonalMesh mesh) {

      if (!mesh.isTriangular()) {
         mesh = new PolygonalMesh(mesh);
         mesh.triangulate();
      }

      // HashMap<Vertex3d,Vtx> vtxMap = new HashMap<Vertex3d,Vtx>(mesh.numVertices());
      // mesh.computeVertexNormals();
      // ArrayList<Vector3d> nrms = mesh.getNormalList();

      // for (int i=0; i<mesh.numVertices(); i++) {
      //   Vertex3d vtx = mesh.getVertex(i);
      // Vector3d nrm = nrms.get(i);
      // Vtx nVtx = new Vtx(vtx.getPosition(), nrm);
      // vtxMap.put(vtx, nVtx);
      //}

      for (Face face : mesh.getFaces()) {
         ArrayList<Vertex3d> vtxs = new ArrayList<Vertex3d>(face.numVertices());
         for (int i=0; i<face.numVertices(); i++) {
            //vtxs.add(vtxMap.get(face.getVertex(i)));
            vtxs.add(face.getVertex(i));
         }
         Polygon p = new Polygon(vtxs);
         myPolygons.add(p);
      }

   }

   public PolygonalMesh generateMesh() {
      return generateMesh(null);
   }

   public Vertex3d addToMap(Vertex3d vtx, HashMap<Vertex3d,Vertex3d> vtxMap, ArrayList<Vertex3d> vtxList, double tol) {

      if (vtxMap.containsKey(vtx)) {
         return vtxMap.get(vtx);
      }

      for (Vertex3d vtx2 : vtxList) {
         if (vtx2.getPosition().distance(vtx.getPosition()) < tol) {
            vtxMap.put(vtx, vtx2);
            return vtx2;
         }
      }

      vtxList.add(vtx);
      vtxMap.put(vtx,vtx);
      return vtx;
   }

   public PolygonalMesh generateMesh(PolygonalMesh mesh) {

      if (mesh == null) {
         mesh = new PolygonalMesh();
      }

      HashMap<Vertex3d,Vertex3d> vtxMap = new HashMap<Vertex3d,Vertex3d>();
      ArrayList<Vertex3d> vtxList = new ArrayList<Vertex3d>();

      for (Polygon p : myPolygons) {
         for (Vertex3d v : p.verts) {
            addToMap(v, vtxMap, vtxList, 1e-10);
         }
      }

      HashMap<Vertex3d,Vertex3d> vtxNewMap = new HashMap<Vertex3d,Vertex3d>(vtxList.size());
      for (Vertex3d vtx : vtxList) {
         Vertex3d newVtx = vtx.copy();
         vtxNewMap.put(vtx, newVtx);
         mesh.addVertex(newVtx);
      }

      for (Polygon p : myPolygons) {
         Vertex3d[] vtxArray = new Vertex3d[p.verts.length];
         for (int i=0; i<vtxArray.length; i++) {
            vtxArray[i] = vtxNewMap.get(vtxMap.get(p.verts[i]));
         }

         mesh.addFace(vtxArray);
      }      
      mesh.triangulate();
      MeshFactory.closeSeams(mesh);

      return mesh;

   }

   public int numPolygons() {
      return myPolygons.size();
   }

   public boolean isEmpty() {
      return (myPolygons.size() == 0);
   }

   public BSPTree(BSPTree tree) {
      myPolygons = tree.getPolygonsClone();
   }

   private ArrayList<Polygon> getPolygonsClone() {
      ArrayList<Polygon> polyClone = new ArrayList<Polygon>(myPolygons.size());
      for (Polygon poly : myPolygons) {
         polyClone.add(poly.clone());
      }
      return polyClone;
   }


   // Return a new CSG solid representing space in either this solid or in the
   // solid `csg`. Neither this solid nor the solid `csg` are modified.
   // 
   //     A.union(B)
   // 
   //     +-------+            +-------+
   //     |       |            |       |
   //     |   A   |            |       |
   //     |    +--+----+   =   |       +----+
   //     +----+--+    |       +----+       |
   //          |   B   |            |       |
   //          |       |            |       |
   //          +-------+            +-------+
   // 
   public BSPTree union(BSPTree csg) {
      Node a = new Node(getPolygonsClone());
      Node b = new Node(csg.getPolygonsClone());
      a.clipTo(b);
      b.clipTo(a);
      b.invert();
      b.clipTo(a);
      b.invert();
      a.build(b.allPolygons());
      return fromPolygons(a.allPolygons());
   }

   // Return a new CSG solid representing space in this solid but not in the
   // solid `csg`. Neither this solid nor the solid `csg` are modified.
   // 
   //     A.subtract(B)
   // 
   //     +-------+            +-------+
   //     |       |            |       |
   //     |   A   |            |       |
   //     |    +--+----+   =   |    +--+
   //     +----+--+    |       +----+
   //          |   B   |
   //          |       |
   //          +-------+
   // 
   public BSPTree subtract(BSPTree csg) {

      if (csg.numPolygons() < 1 || numPolygons() < 1) {
         return new BSPTree(this);
      }

      Node a = new Node(this.getPolygonsClone());
      Node b = new Node(csg.getPolygonsClone());
      a.invert();
      a.clipTo(b);
      b.clipTo(a);
      b.invert();
      b.clipTo(a);
      b.invert();

      a.build(b.allPolygons());
      a.invert();
      return fromPolygons(a.allPolygons());
   }

   // Return a new CSG solid representing space both this solid and in the
   // solid `csg`. Neither this solid nor the solid `csg` are modified.
   // 
   //     A.intersect(B)
   // 
   //     +-------+
   //     |       |
   //     |   A   |
   //     |    +--+----+   =   +--+
   //     +----+--+    |       +--+
   //          |   B   |
   //          |       |
   //          +-------+
   // 
   public BSPTree intersect(BSPTree csg) {

      if (csg.numPolygons() < 1) {
         return new BSPTree(); //empty
      } else if (numPolygons() < 1) {
         return new BSPTree();
      }

      Node a = new Node(this.getPolygonsClone());
      Node b = new Node(csg.getPolygonsClone());
      a.invert();
      b.clipTo(a);
      b.invert();
      a.clipTo(b);
      b.clipTo(a);
      a.build(b.allPolygons());
      a.invert();
      return fromPolygons(a.allPolygons());
   }

   // Return a new CSG solid with solid and empty space switched. This solid is
   // not modified.
   public BSPTree inverse() {
      BSPTree csg = this.clone();
      for (Polygon p : myPolygons) {
         p.flip();
      }
      return csg;
   }

   //Construct a CSG solid from a list of `CSG.Polygon` instances.
   public static BSPTree fromPolygons( ArrayList<Polygon> polygons) {
      BSPTree csg = new BSPTree();
      csg.myPolygons = polygons;
      return csg;
   }

   public BSPTree clone() {
      BSPTree csg = new BSPTree();
      for (Polygon p : myPolygons) {
         csg.myPolygons.add(p.clone());
      }
      return csg;
   }

   public static PolygonalMesh getIntersection(PolygonalMesh mesh1,
      PolygonalMesh mesh2) {
      BSPTree tree1 = new BSPTree(mesh1);
      BSPTree tree2 = new BSPTree(mesh2);
      BSPTree outTree = tree1.intersect(tree2);
      return outTree.generateMesh();
   }

   public static PolygonalMesh
   getUnion(PolygonalMesh mesh1, PolygonalMesh mesh2) {
      BSPTree tree1 = new BSPTree(mesh1);
      BSPTree tree2 = new BSPTree(mesh2);
      BSPTree outTree = tree1.union(tree2);
      return outTree.generateMesh();
   }

   public static PolygonalMesh getSubtraction(PolygonalMesh mesh1,
      PolygonalMesh mesh2) {
      BSPTree tree1 = new BSPTree(mesh1);
      BSPTree tree2 = new BSPTree(mesh2);
      BSPTree outTree = tree1.subtract(tree2);
      return outTree.generateMesh();
   }

}
