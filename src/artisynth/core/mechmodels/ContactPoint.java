/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.Arrays;
import java.util.Iterator;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;

/**
 * Stores information about a contact point, including the point
 * value (in world coordinates), and, if necessary, the associated
 * vertices and weights.
 */
public class ContactPoint {
   
   Point3d myPoint;     // location of the point (world coordinates)
   Vertex3d[] myVtxs;   // vertices associated with the point
   double[] myWgts;     // weights associated with each vertex

   public ContactPoint () {
      myPoint = new Point3d();
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
   
   public Point3d getPoint() {
      return myPoint;
   }

   public Vertex3d[] getVertices() {
      return myVtxs;
   }

   public int numVertices() {
      return myVtxs != null ? myVtxs.length : 0;
   }

   public double[] getWeights() {
      return myWgts;
   }

   public void set (Point3d pnt) {
      myPoint.set (pnt);
      myVtxs = null;
      myWgts = null;
   }

   public void set (Vertex3d vtx) {
      myPoint.set (vtx.getWorldPoint());
      myVtxs = new Vertex3d[] { vtx };
      myWgts = new double[] { 1.0 };
   }

   public void set (ContactPoint cpnt) {
      myPoint.set (cpnt.myPoint);
      if (cpnt.myVtxs == null) {
         myVtxs = null;
         myWgts = null;
      }
      else {
         myVtxs = Arrays.copyOf (cpnt.myVtxs, cpnt.myVtxs.length);
         myWgts = Arrays.copyOf (cpnt.myWgts, cpnt.myWgts.length);
      }
   }

   public void set (Point3d pnt, Feature feat) {
      myPoint.set (pnt);
      setVerticesAndWeights (feat);
   }

   public void set (Point3d pnt, Face face, Vector2d coords) {
      myPoint.set (pnt);
      double w1 = coords.x;
      double w2 = coords.y;
      myVtxs = face.getTriVertices();
      myWgts = new double[] { 1-(w1+w2), w1, w2 };
   }

   public void set (Point3d pnt, HalfEdge he, double w1) {
      myPoint.set (pnt);
      myVtxs = new Vertex3d[] { he.tail, he.head };
      myWgts = new double[] { 1-w1, w1 };
   }

   public void set (Point3d pnt, Vertex3d[] vtxs, double[] wgts) {
      myPoint.set (pnt);
      if (vtxs.length != wgts.length) {
         throw new IllegalArgumentException (
            "vtxs and wgts differ in length: "+vtxs.length+" vs. "+wgts.length);
      }
      myVtxs = Arrays.copyOf (vtxs, vtxs.length);
      myWgts = Arrays.copyOf (wgts, wgts.length);
   }

   public void setVerticesAndWeights (Feature feat) {
      switch (feat.getType()) {
         case Feature.VERTEX_3D: {
            Vertex3d vtx = (Vertex3d)feat;
            myVtxs = new Vertex3d[] {vtx};
            myWgts = new double[] { 1 };
            break;
         }
         case Feature.HALF_EDGE: {
            HalfEdge he = (HalfEdge)feat;
            setForEdge (he.tail, he.head);
            break;
         }
         case Feature.FACE: {
            Face face = (Face)feat;
            HalfEdge he0 = face.firstHalfEdge();
            HalfEdge he1 = he0.getNext();
            HalfEdge he2 = he1.getNext();
            setForTriangle (he0.head, he1.head, he2.head);
            break;
         }
         default:{
            throw new InternalErrorException (
               "Unimplemented feature type " + feat.getClass());
         }
      }
   }

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

   protected static ContactPoint setState (
      DataBuffer data, PolygonalMesh mesh) {

      int numv = data.zget();
      if (numv == -1) {
         return null;
      }
      ContactPoint cpnt = new ContactPoint();
      data.dget(cpnt.myPoint);
      if (numv == 0) {
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
         return code;
      }
   }

   public boolean equals(Object obj) {
      if (obj instanceof ContactPoint) {
         Vertex3d[] otherVtxs = ((ContactPoint)obj).myVtxs;

         if (otherVtxs.length != myVtxs.length) {
            return false;
         }

         // XXX Should we account for potential directional ambiguity?
         //     e.g. in face or edge... 
         //     I believe these will always be consistent
         for (int i=0; i<myVtxs.length; i++) {
            if (myVtxs[i] != otherVtxs[i]) {
               return false;
            }
         }
         return true;

      }
      return false;
   }

   public boolean isOnCollidable (CollidableBody cbody) {
      if (myVtxs != null) {
         return myVtxs[0].getMesh() == cbody.getCollisionMesh();
      }
      else {
         return false;
      }
   }
   
   public String toString (String fmtStr) {
      String str = "[ pos=" + myPoint.toString (fmtStr);
      for (int i=0; i<myVtxs.length; i++) {
         str += " v"+i+"=" + myVtxs[i].getIndex();
      }
      str += " ]";
      return str;
   }

}
