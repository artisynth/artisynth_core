/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.PrintStream;

import maspack.collision.ContactPenetratingPoint;
import maspack.collision.EdgeEdgeContact;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;

public class DeformableContactConstraintGeneric 
extends DeformableContactConstraintBase {

   // temp variables
   private Point3d loc = new Point3d();
   private Vector3d normal = new Vector3d();
   private Vector3d disp = new Vector3d();

   public DeformableContactConstraintGeneric() {
      clearVertices();
   }

   public void setVertices(Vertex3d... vtxs) {
      myInfo.vtxs1 = vtxs;
      myInfo.wgts1.setSize(vtxs.length);
   }

   public Vertex3d[] getVertices() {
      return myInfo.vtxs1;
   }

   public void clearVertices() {
      myInfo.vtxs1 = new Vertex3d[0];
      myInfo.wgts1.setSize(0);
   }

   /**
    * Creates a new contact constraint identified by a set of vertices
    */
   public DeformableContactConstraintGeneric (Vertex3d... vtxs) {
      setVertices(vtxs);
   }

   public int hashCode() {
      int code = 0;

      for (Vertex3d vtx : getVertices()) {
         if (vtx != null) {
            code += vtx.hashCode();
         }
      }
      return code;
   }

   public boolean equals(Object obj) {
      if (obj instanceof DeformableContactConstraintGeneric) {
         Vertex3d [] objVtxs = ((DeformableContactConstraintGeneric)obj).getVertices();
         Vertex3d [] myVtxs = getVertices();

         if (objVtxs.length != myVtxs.length) {
            return false;
         }

         // XXX Should we account for potential directional ambiguity?
         //     e.g. in face or edge... 
         //     I believe these will always be consistent
         for (int i=0; i<myVtxs.length; i++) {
            if (myVtxs[i] != objVtxs[i]) {
               return false;
            }
         }
         return true;

      }

      return false;

   }

   public void print(PrintStream os) {
      os.println("type=" + myInfo.type);
      os.println("mu=" + myMu);
      os.println("nrml=" + myInfo.normal);
      os.println("distance=" + myInfo.distance);
      os.println("numn=" + numPoints());
      for (CompInfo info = myHeadComp; info != null; info = info.getNext()) {
         if (info instanceof PointInfo) {
            PointInfo pinfo = (PointInfo)info;
            os.println(" point=" + pinfo.getPoint().getNumber() + " w=" + pinfo.getWeight());
         }
         else if (info instanceof FrameInfo) {
            FrameInfo finfo = (FrameInfo)info;
            os.println(" frame=" + finfo.getFrame().getNumber() + " w=" + finfo.getWeight() +
               " loc=" + finfo.getLoc());
         }
      }
   }

   /**
    * Set the constraint for a deformable-deformable node-face contact.
    */
   public void setVertexFace(
      Vector3d nrml, double mu, Vertex3d v0, Vertex3d v1,
      Vertex3d v2, Vertex3d v3, double w0, double w1, double w2,
      DeformableCollisionData thisData, DeformableCollisionData otherData) {

      myInfo.type = VERTEX_FACE;
      myInfo.normal.set(nrml);
      myInfo.set(VERTEX_FACE, 0, nrml, new Vertex3d[]{v0}, new Point3d(), new double[] {1}, 
         new Vertex3d[]{v1, v2, v3},new Point3d(), new double[] {w0, w1, w2});
      setFriction(mu);

      beginSet();
      thisData.addConstraintInfo(v0, null, 1, this);
      otherData.addConstraintInfo(v1, null, -w0, this);
      otherData.addConstraintInfo(v2, null, -w1, this);
      otherData.addConstraintInfo(v3, null, -w2, this);
      endSet();
   }

   public String toString() {
      String out = "";
      for (Vertex3d vtx : getVertices()) {
         if (vtx != null) {
            out += vtx.getIndex() + " ";
         }
      }
      return out;
   }

   @Override
   public double setVertexRigidBody(ContactPenetratingPoint cpp,
      double mu, DeformableCollisionData thisData,
      RigidBodyCollisionData rbData, boolean useSignedDistanceCollider) {

      RigidBody body = rbData.getBody();
      RigidTransform3d XBW = body.getPose(); // body to world transform

      loc.inverseTransform(XBW, cpp.position);
      if (useSignedDistanceCollider) {
         normal.set(cpp.normal);
      }
      else {
         normal.transform(XBW, cpp.face.getNormal());
      }

      double wb0 = 1-(cpp.coords.x+cpp.coords.y);

      Vertex3d vtx = cpp.vertex;
      setVertexBody(normal, mu, vtx, vtx.pnt, 1, thisData, body, loc, 
         cpp.face.getVertex(0), cpp.face.getVertex(1), cpp.face.getVertex(2),
         wb0, cpp.coords.x, cpp.coords.y);

      disp.sub (vtx.getWorldPoint(), cpp.position);
      double dist = disp.dot (normal);

      //      NumberFormat fmt = new NumberFormat ("%.10g");
      //      System.out.println("VRB n:" + myInfo.normal.toString(fmt) + " (" + vtx.getIndex() + ")" +
      //      " (" + body.getName() + "," + loc.toString(fmt) + ")" + " dist:" + fmt.format (dist));

      return dist;
   }

   @Override
   public double setFaceRigidBody(ContactPenetratingPoint cpp,
      double mu,
      DeformableCollisionData thisData,
      RigidBodyCollisionData rbData) {

      RigidBody body = rbData.getBody();
      Vertex3d vtx0 = cpp.face.getVertex(0);
      Vertex3d vtx1 = cpp.face.getVertex(1);
      Vertex3d vtx2 = cpp.face.getVertex(2);

      double w0 = 1.0 - (cpp.coords.x + cpp.coords.y);
      double w1 = cpp.coords.x;
      double w2 = cpp.coords.y;

      // compute normal from scratch because previous contacts
      // may have caused it to change
      cpp.face.computeNormal(normal);

      RigidTransform3d XBW = body.getPose();
      loc.transform(XBW, cpp.vertex.pnt);
      disp.sub(loc, cpp.position);
      double dist = disp.dot(normal);

      normal.negate();  // acting in opposite direction
      setBodyFace(normal, mu, vtx0, vtx1, vtx2, cpp.position, w0, w1, w2,
         thisData, cpp.vertex, body, cpp.vertex.pnt, 1);

      return dist;

   }

   @Override
   public void setEdgeRigidBody(EdgeEdgeContact eec,
      double mu,
      DeformableCollisionData thisData,
      RigidBodyCollisionData rbData) {

      RigidBody body = rbData.getBody();
      RigidTransform3d XBW = body.getPose(); // body to world transform

      Vertex3d vt = eec.edge1.tail;
      Vertex3d vh = eec.edge1.head;
      double wh = 1.0 - eec.s1;
      double wt = eec.s1;

      // XXX changed from point1 to point0, check consistency
      loc.inverseTransform(XBW, eec.point0);
      normal.transform(XBW, eec.point1ToPoint0Normal);
      normal.normalize();

      setEdgeBody(
         normal, mu, vt, vh, eec.point1, wt, wh, thisData, body, loc, 
         eec.edge0.tail, eec.edge0.head, eec.s0, 1-eec.s0);
   }

   @Override
   public void setEdgeEdge(EdgeEdgeContact eec, double mu,
      DeformableCollisionData thisData, 
      DeformableCollisionData otherData) {

      Vertex3d v0t = eec.edge0.tail;
      Vertex3d v0h = eec.edge0.head;
      Vertex3d v1t = eec.edge1.tail;
      Vertex3d v1h = eec.edge1.head;
      setEdgeEdge(
         eec.point1ToPoint0Normal, mu, 
         v0t, v0h, eec.point0, 1 - eec.s0, eec.s0, thisData,
         v1t, v1h, eec.point1, 1 - eec.s1, eec.s1, otherData);
   }

   @Override
   public double setVertexDeformable(ContactPenetratingPoint cpp,
      double mu,
      DeformableCollisionData thisData, DeformableCollisionData otherData) {
      double w0 = 1.0 - (cpp.coords.x + cpp.coords.y);
      double w1 = cpp.coords.x;
      double w2 = cpp.coords.y;

      Vertex3d vtx0 = cpp.vertex;
      Vertex3d vtx1 = cpp.face.getVertex(0);
      Vertex3d vtx2 = cpp.face.getVertex(1);
      Vertex3d vtx3 = cpp.face.getVertex(2);
      cpp.face.computeNormal(normal);

      disp.sub(vtx0.getWorldPoint(), vtx1.getWorldPoint());
      double dist = disp.dot(normal);

      setVertexFace(
         normal, mu, vtx0, cpp.vertex.pnt, 1, thisData,
         vtx1, vtx2, vtx3, cpp.position, w0, w1, w2, otherData);

      //      NumberFormat fmt = new NumberFormat ("%.10g");
      //      System.out.println("VD n:" + myInfo.normal.toString(fmt) + " (" + vtx0.getIndex() + "," + 1 + ")" +
      //      " (" + vtx1.getIndex() + "," + fmt.format(-w0) + ")" +
      //      " (" + vtx2.getIndex() + "," + fmt.format (-w1) + ")" +
      //      " (" + vtx3.getIndex() + "," + fmt.format(-w2) + ")" + " dist:" + fmt.format (dist));

      return dist;
   }

}
