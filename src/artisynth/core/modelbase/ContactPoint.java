/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.Arrays;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;

/**
 * Stores information about a contact point, including its position value (in
 * world coordinates), and, if necessary, the associated vertices and weights.
 */
public class ContactPoint implements MeshFieldPoint {
   
   Point3d myPoint;     // location of the point (world coordinates)
   Vertex3d[] myVtxs;   // vertices associated with the point
   double[] myWgts;     // weights associated with each vertex

   public ContactPoint () {
      myPoint = new Point3d();
      myVtxs = new Vertex3d[0];
   }

   public ContactPoint (Point3d pnt) {
      this();
      set (pnt);
   }

   public ContactPoint (Vertex3d vtx) {
      this();
      set (vtx);
   }

   public ContactPoint (ContactPoint cpnt) {
      this();
      set (cpnt);
   }

   public ContactPoint (Point3d pnt, Face face, Vector2d coords) {
      this();
      set (pnt, face, coords);
   }

   public ContactPoint (Point3d pnt, HalfEdge he, double w1) {
      this();
      set (pnt, he, w1);
   }

   /**
    * Returns the position of this contact point, in world coordinates.
    *
    * @return point position. Should not be modified.
    */
   public Point3d getPosition() {
      return myPoint;
   }

   /**
    * Returns the current position of this contact point, based on the vertex
    * positions. If there are no vertices, the original position is returned.
    *
    * @return current point position.
    */
   public Point3d getCurrentPosition() {
      Point3d pos = new Point3d();
      if (myVtxs.length == 0) {
         pos.set (myPoint);
      }
      else {
         for (int i=0; i<myVtxs.length; i++) {
            pos.scaledAdd (
               myWgts[i], myVtxs[i].getWorldPoint());
         }
      }
      return pos;
   }

   /**
    * Returns the mesh vertices associated with this contact, if any. If there
    * are no vertices, the returned array with have a length of 0.
    *
    * @return vertices associated with this contact. Should not be modified.
    */
   public Vertex3d[] getVertices() {
      return myVtxs;
   }

   /**
    * Returns the number of mesh vertices associated with this contact, or 0 if
    * there are no vertices.
    *
    * @return number of mesh vertices
    */
   public int numVertices() {
      return myVtxs != null ? myVtxs.length : 0;
   }

   /**
    * Returns the mesh vertex weight associated with this contact, if any. If
    * there are no vertices, this method will return {@code null}.
    *
    * @return vertex weights associated with this contact, or {@code
    * null}. Should not be modified.
    */
   public double[] getWeights() {
      return myWgts;
   }

   /**
    * Sets this contact point to correspond to a specific position in world
    * coordinates, with no mesh vertices.
    */
   public void set (Point3d pnt) {
      myPoint.set (pnt);
      myVtxs = new Vertex3d[0];
      myWgts = null;
   }

   /**
    * Sets this contact point to correspond to a single mesh vertex.
    */
   public void set (Vertex3d vtx) {
      myPoint.set (vtx.getWorldPoint());
      myVtxs = new Vertex3d[] { vtx };
      myWgts = new double[] { 1.0 };
   }

   /**
    * Sets this contact point to be identical to another contact point.
    */
   public void set (ContactPoint cpnt) {
      myPoint.set (cpnt.myPoint);
      if (cpnt.myVtxs == null) {
         myVtxs = null;
      }
      else {
         myVtxs = Arrays.copyOf (cpnt.myVtxs, cpnt.myVtxs.length);
      }
      if (cpnt.myWgts == null) {
         myWgts = null;
      }
      else {
         myWgts = Arrays.copyOf (cpnt.myWgts, cpnt.myWgts.length);
      }     
   }

//   void set (Point3d pnt, Feature feat) {
//      myPoint.set (pnt);
//      setVerticesAndWeights (feat);
//   }

   /**
    * Sets this contact point to correspond to point on a mesh face, with
    * specified barycentric coordinates.
    */
   public void set (Point3d pnt, Face face, Vector2d coords) {
      myPoint.set (pnt);
      double w1 = coords.x;
      double w2 = coords.y;
      myVtxs = face.getTriVertices();
      myWgts = new double[] { 1-(w1+w2), w1, w2 };
   }

   /**
    * Sets this contact point to correspond to a point on a mesh half edge.
    */
   public void set (Point3d pnt, HalfEdge he, double w1) {
      myPoint.set (pnt);
      myVtxs = new Vertex3d[] { he.tail, he.head };
      myWgts = new double[] { 1-w1, w1 };
   }

//   void set (Point3d pnt, Vertex3d[] vtxs, double[] wgts) {
//      myPoint.set (pnt);
//      if (vtxs.length != wgts.length) {
//         throw new IllegalArgumentException (
//            "vtxs and wgts differ in length: "+vtxs.length+" vs. "+wgts.length);
//      }
//      myVtxs = Arrays.copyOf (vtxs, vtxs.length);
//      myWgts = Arrays.copyOf (wgts, wgts.length);
//   }

//   void setVerticesAndWeights (Feature feat) {
//      switch (feat.getType()) {
//         case Feature.VERTEX_3D: {
//            Vertex3d vtx = (Vertex3d)feat;
//            myVtxs = new Vertex3d[] {vtx};
//            myWgts = new double[] { 1 };
//            break;
//         }
//         case Feature.HALF_EDGE: {
//            HalfEdge he = (HalfEdge)feat;
//            setForEdge (he.tail, he.head);
//            break;
//         }
//         case Feature.FACE: {
//            Face face = (Face)feat;
//            HalfEdge he0 = face.firstHalfEdge();
//            HalfEdge he1 = he0.getNext();
//            HalfEdge he2 = he1.getNext();
//            setForTriangle (he0.head, he1.head, he2.head);
//            break;
//         }
//         default:{
//            throw new InternalErrorException (
//               "Unimplemented feature type " + feat.getClass());
//         }
//      }
//   }


   /**
    * Sets this contact point to correspond to an edge contact between two
    * vertices. Used for testing.
    */
   void setForEdge (Vertex3d v1, Vertex3d v2) {
      Vector3d u = new Vector3d();
      u.sub (v1.pnt, v2.pnt);
      double w1;
      switch (u.maxAbsIndex()) {
         case 0: {
            w1 = (myPoint.x-v2.pnt.x)/u.x;
            break;
         }
         case 1: {
            w1 = (myPoint.y-v2.pnt.y)/u.y;
            break;
         }
         case 2: {
            w1 = (myPoint.z-v2.pnt.z)/u.z;
            break;
         }
         default: {
            throw new InternalErrorException (
               "Vector3d returns invalid absIndex: " + u.maxAbsIndex());
         }
      }
      if (w1 < 0) {
         w1 = 0;
      }
      else if (w1 > 1) {
         w1 = 1;
      }
      myWgts = new double[] { w1, 1-w1 };         
      myVtxs = new Vertex3d[] { v1, v2 };
   }

   /**
    * Sets this contact point to correspond to a face contact defined by three
    * vertices. Used for testing.
    */
   void setForTriangle (Vertex3d v1, Vertex3d v2, Vertex3d v3) {
      Vector3d r2 = new Vector3d();
      Vector3d r3 = new Vector3d();
      Vector3d xp = new Vector3d();
      Vector3d r = new Vector3d();
      r2.sub (v2.pnt, v1.pnt);
      r3.sub (v3.pnt, v1.pnt);
      r.sub (myPoint, v1.pnt);
      xp.cross (r2, r3);
      double w2, w3;
      switch (xp.maxAbsIndex()) {
         case 0: {
            // use y/z plane
            w2 = (r.y*r3.z - r.z*r3.y)/xp.x;
            w3 = (r.z*r2.y - r.y*r2.z)/xp.x;
            break;
         }
         case 1: {
            // use x/z plane
            w2 = (r.z*r3.x - r.x*r3.z)/xp.y;
            w3 = (r.x*r2.z - r.z*r2.x)/xp.y;
            break;
         }
         case 2: {
            // use x/y plane
            w2 = (r.x*r3.y - r.y*r3.x)/xp.z;
            w3 = (r.y*r2.x - r.x*r2.y)/xp.z;
            break;
         }
         default: {
            throw new InternalErrorException (
               "Vector3d returns invalid absIndex: " + xp.maxAbsIndex());
         }
      }
      if (w2 < 0) {
         w2 = 0;
      }
      else if (w2 > 1) {
         w2 = 1;
      }
      if (w3 < 0) {
         w3 = 0;
      }
      else if (w3 > 1) {
         w3 = 1;
      }
      myWgts = new double[] { 1-w2-w3, w2, w3 };         
      myVtxs = new Vertex3d[] { v1, v2, v3 };
   }

   /**
    * Save the information of contact point {@code cpnt} into a data buffer.
    */
   public static void getState (DataBuffer data, ContactPoint cpnt) {
      if (cpnt == null) {
         data.zput (-1);
         return;
      }
      data.dput (cpnt.myPoint);
      if (cpnt.myVtxs == null) {
         data.zput (0);
         return;
      }
      else {
         data.zput (cpnt.myVtxs.length);
      }
      Vertex3d[] vtxs = cpnt.myVtxs;
      double[] wgts = cpnt.myWgts;
      for (int i=0; i<vtxs.length; i++) {
         data.zput (vtxs[i].getIndex());
         data.dput (wgts[i]);
      }
   }

   /**
    * Creates a contact point for information stored in a data buffer, using
    * additional information supplied by the point's collision mesh.
    */
   public static ContactPoint setState (
      DataBuffer data, PolygonalMesh mesh) {

      int numv = data.zget();
      if (numv == -1) {
         return null;
      }
      ContactPoint cpnt = new ContactPoint();
      data.dget(cpnt.myPoint);
      if (numv == 0) {
         cpnt.myVtxs = new Vertex3d[0];
         return cpnt;
      }
      Vertex3d[] vtxs = new Vertex3d[numv];
      double[] wgts = new double[numv];
      for (int i=0; i<numv; i++) {
         vtxs[i] = mesh.getVertex (data.zget());
         wgts[i] = data.dget();
      }
      cpnt.myVtxs = vtxs;
      cpnt.myWgts = wgts;
      return cpnt;
   }

   /**
    * Returns a hash code for this contact point. If the point has associated
    * vertices, the code is computed based on them, allowing contact points to
    * be stored in a hash map based solely on their vertex structure.
    *
    * @return hash code for this contact point
    */
   public int hashCode() {
      if (myVtxs == null) {
         return super.hashCode();
      }
      else {
         int code = 0;
         for (Vertex3d vtx : myVtxs) {
            if (vtx != null) {
               code += vtx.hashCode();
            }
         }
         return (code == 0 ? super.hashCode() : code);
      }
   }
   
   /**
    * Queries the vertices associated with this contact point equal those of
    * another. Vertex orderings are assumed to be the same.
    *
    * @param pnt contact point to which vertices are compared
    * @return {@code true} if this point's vertices equal those of {@code pnt}
    */
   public boolean verticesEqual (ContactPoint pnt) {
      Vertex3d[] otherVtxs = pnt.myVtxs;
      if (myVtxs.length != otherVtxs.length) {
         return false;
      }
      if (myVtxs.length == 1) {
         return myVtxs[0] == otherVtxs[0];
         
      }
      else {
         for (int i=0; i<myVtxs.length; i++) {
            if (myVtxs[i] != otherVtxs[i]) {
               return false;
            }
         } 
         return true;
      }
   }

   /**
    * Queries whether this contact point equals another object, based on
    * equality of the vertices. This allows contact points to be stored in a
    * hash map based solely on their vertex structure.
    *
    * @param obj object to which this contact point is compared
    * @return {@code true} if {@code obj} is a contact point whose vertices
    * equal those of this point
    */
   public boolean equals(Object obj) {
      if (obj instanceof ContactPoint) {
         return verticesEqual ((ContactPoint)obj);
      }
      return false;
   }
   
   /**
    * Returns the mesh of the first vertex associated with this contact point,
    * or {@code null} if there are no vertices.
    *
    * @return mesh of the first vertex
    */
   public MeshBase getMesh() {
      if (myVtxs != null && myVtxs.length > 0) {
         return myVtxs[0].getMesh();
      }
      else {
         return null;
      }
   }

   /**
    * Computes an estimate of the local contact for this point, assuming that
    * it is associated with a vertex or edge-edge contact.  Otherwise, the
    * method returns -1.
    *
    * @param nrml contact normal
    * @return area estimate, or -1 if not vertex or edge-edge.
    */
   public double computeContactArea (Vector3d nrml) {
      if (myVtxs == null || myVtxs.length != 1) {
         return -1;
      }
      Vector3d xsum = new Vector3d();
      // sum the cross products of all the face edges surrounding this
      // vertex. This effectively gives an area-weighted vertex normal.
      myVtxs[0].sumEdgeCrossProductsWorld (xsum);
      // take the dot product of the vertex normal with the contact normal to
      // get the area component in the contact plane. Divide by 6 because (a)
      // cross products give twice the area, and (b) the vertex should take
      // only 1/3 of the area of each incident face.
      return Math.abs(nrml.dot(xsum)/6);     
   }
   
   /**
    * Produces a string representation of this contact point, giving the
    * position and vertex indices.
    *
    * @param fmtStr numeric format for the position information
    * @return string representation of this contact point
    */
   public String toString (String fmtStr) {
      String str = "[ pos=" + myPoint.toString (fmtStr);
      if (myVtxs != null) {
         for (int i=0; i<myVtxs.length; i++) {
            str += " v"+i+"=" + myVtxs[i].getIndex();
         }
      }
      str += " ]";
      return str;
   }

}
