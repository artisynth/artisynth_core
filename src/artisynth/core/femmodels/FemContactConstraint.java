/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.ArrayList;

import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.DataBuffer;
import maspack.util.InternalErrorException;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.CollisionData;
import artisynth.core.mechmodels.DeformableCollisionData;
import artisynth.core.mechmodels.DeformableContactConstraintGeneric;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.RigidBody;

public class FemContactConstraint extends
   DeformableContactConstraintGeneric {

   public FemContactConstraint () {
	   clearVertices();
   }

   /**
    * Creates a new contact constraint identified by three vertices.
    */
   public FemContactConstraint (FemMeshVertex v0, FemMeshVertex v1) {
      setVertices(v0, v1);
   }

   /**
    * Creates a new contact constraint identified by three vertices.
    */
   public FemContactConstraint (FemMeshVertex v0, FemMeshVertex v1,
      FemMeshVertex v2) {
      setVertices(v0, v1, v2);
   }
   
   /**
    * Creates a new contact constraint identified by three vertices.
    */
   public FemContactConstraint (Vertex3d... vtxs) {
      setVertices(vtxs);
   }
   
   public void setFace(Face f) {
      setVertices((FemMeshVertex)f.getVertex(0),
         (FemMeshVertex)f.getVertex(1),
         (FemMeshVertex)f.getVertex(2));
   }

   /**
    * Set the constraint for a deformable-deformable edge-edge contact.
    * 
    * This constraint can be used to prevent interpenetration of two edges edge1
    * and edge0 as follows: n0, n1 are the nodes on the ends of edge0. n2, n3
    * are the nodes on the ends of edge1. w0, w1 are the weights of the nodes
    * n0, n1: 0 <= w0 <= 1, w1 = 1 - w0 They define a position p0 on edge0: p0 =
    * w0*n0.getPosition() + w1*n1.getPosition() Similarly, w2, w3 are the
    * weights of the nodes n2, n3: 0 <= w2 <= 1, w3 = 1 - w2 They define a
    * position p1 on edge1: p1 = w2 * n2.getPosition() + w3 * n3.getPosition()
    * nrml is the direction in which p0 should move to remove the
    * interpenetration. The constraint will ensure that q > 0 where q = (p0 -
    * p1).dot(nrml). and will remain active as long as q < 0. The constraint
    * also generates frictional forces opposite to the direction of any relative
    * motion perpendicular to nrml.
    */
   public void setEdgeEdge(
      Vector3d nrml, double mu,
      FemMeshVertex v0, FemMeshVertex v1, FemMeshVertex v2, FemMeshVertex v3,
      double w0, double w1, double w2, double w3) {

      myInfo.type = EDGE_EDGE;
      myInfo.normal.set(nrml);
      setFriction(mu);
      beginSet();

      addPoint(v0, w0);
      addPoint(v1, w1);
      addPoint(v2, -w2);
      addPoint(v3, -w3);

   }

   /**
    * Set the constraint for a deformable-deformable edge-edge contact.
    * 
    * This constraint can be used to prevent interpenetration of two edges edge1
    * and edge0 as follows: n0, n1 are the nodes on the ends of edge0. n2, n3
    * are the nodes on the ends of edge1. w0, w1 are the weights of the nodes
    * n0, n1: 0 <= w0 <= 1, w1 = 1 - w0 They define a position p0 on edge0: p0 =
    * w0*n0.getPosition() + w1*n1.getPosition() Similarly, w2, w3 are the
    * weights of the nodes n2, n3: 0 <= w2 <= 1, w3 = 1 - w2 They define a
    * position p1 on edge1: p1 = w2 * n2.getPosition() + w3 * n3.getPosition()
    * nrml is the direction in which p0 should move to remove the
    * interpenetration. The constraint will ensure that q > 0 where q = (p0 -
    * p1).dot(nrml). and will remain active as long as q < 0. The constraint
    * also generates frictional forces opposite to the direction of any relative
    * motion perpendicular to nrml.
    */
   public void setEdgeEdge(
      Vector3d nrml, double mu,
      FemMeshVertex v0, FemMeshVertex v1, Point3d loc1, double w0, double w1,
      Vertex3d v2, Vertex3d v3, Point3d loc2, double w2, double w3,
      DeformableCollisionData otherData) {

      myInfo.type = EDGE_EDGE;
      myInfo.normal.set(nrml);
      setFriction(mu);
      
      beginSet();
      addPoint(v0, w0);
      addPoint(v1, w1);
      otherData.addConstraintInfo(v2, loc2, -w2, this);
      otherData.addConstraintInfo(v3, loc2, -w3, this);
      endSet();
   }

   protected void addPoint(FemMeshVertex fvtx, double weight) {
      addPoint(fvtx.getPoint(), weight);
   }
   
   /**
    * Set the constraint for a deformable-deformable node-face contact.
    * For CollisionHandlerOld
    */
   public void setVertexFace(
      Vector3d nrml, double mu, FemMeshVertex v0, FemMeshVertex v1,
      FemMeshVertex v2, FemMeshVertex v3, double w0, double w1, double w2) {

      myInfo.type = VERTEX_FACE;
      myInfo.normal.set(nrml);
      setFriction(mu);
      
      beginSet();
      addPoint(v0, 1);
      addPoint(v1, -w0);
      addPoint(v2, -w1);
      addPoint(v3, -w2);
      endSet();
   }
   
   /**
    * Set the constraint for a deformable-deformable node-face contact.
    */
   public void setVertexFace(
      Vector3d nrml, double mu, FemMeshVertex v0, double w0,
      Vertex3d v1, Vertex3d v2, Vertex3d v3, Point3d loc2,
      double w1, double w2, double w3, DeformableCollisionData otherData) {

      myInfo.type = VERTEX_FACE;
      myInfo.normal.set(nrml);
      setFriction(mu);

      beginSet();
      addPoint(v0, 1);
      otherData.addConstraintInfo(v1, loc2, -w1, this);
      otherData.addConstraintInfo(v2, loc2, -w2, this);
      otherData.addConstraintInfo(v3, loc2, -w3, this);
      endSet();
   }

   //   /**
   //    * Set the constraint for a deformable-deformable node-face contact.
   //    */
   //   public void setVertexFace(
   //      Vector3d nrml, double mu, FemMeshVertex v0, Vertex3d v1,
   //      Vertex3d v2, Vertex3d v3, double w0, double w1, double w2,
   //      DeformableCollisionData otherData) {
   //
   //      myInfo.type = VERTEX_FACE;
   //      myInfo.normal.set(nrml);
   //      setFriction(mu);
   //      beginSet();
   //
   //      addPoint(v0, 1);
   //      otherData.addConstraintInfo(v1, null, -w0, this);
   //      otherData.addConstraintInfo(v2, null, -w1, this);
   //      otherData.addConstraintInfo(v3, null, -w2, this);
   //      endSet();
   //   }
   
   /**
    * Sets the constraint for rigidBody-deformable node-face contact
    */
   public void setBodyFace(Vector3d nrml, double mu, 
      FemMeshVertex v0, FemMeshVertex v1, FemMeshVertex v2,
      double w0, double w1, double w2,
      Vertex3d vbody, RigidBody body, Point3d loc, double wrb) {

      myInfo.type = BODY_FACE;
      myInfo.normal.set(nrml);
      // myInfo.normal.negate();     // XXX reverse, since belongs to face
      setFriction(mu);
      
      beginSet();
      addPoint(v0, -w0);
      addPoint(v1, -w1);
      addPoint(v2, -w2);
      addFrame(body, 1, loc);
      endSet();
   }

   /**
    * Set the constraint for a deformable-rigidBody node-face contact.
    */
   public void setVertexBody(
      Vector3d nrml, double mu, FemMeshVertex v0, double w0,
      RigidBody body, Point3d loc, double wrb) {

      myInfo.type = VERTEX_BODY;
      myInfo.normal.set(nrml);
      setFriction(mu);
      beginSet();
      addPoint(v0, 1);
      addFrame(body, -1, loc);
      endSet();
   }

   /**
    * Set the constraint for a deformable-rigidBody edge-edge contact.
    */
   public void setEdgeBody(
      Vector3d nrml, double mu, FemMeshVertex v0, FemMeshVertex v1,
      double w0, double w1, RigidBody body, Point3d loc, double wrb) {

      myInfo.type = EDGE_BODY;
      myInfo.normal.set(nrml);
      setFriction(mu);
      beginSet();
      addPoint(v0, w0);
      addPoint(v1, w1);
      addFrame(body, -1, loc);
      endSet();
   }

   public int numMyVertices() {
      switch (myInfo.type) {
         case VERTEX_FACE:
         case VERTEX_BODY:
            return 1;
         case EDGE_EDGE:
         case EDGE_BODY:
            return 2;
         case BODY_FACE:
            return 3;
         default: {
            throw new InternalErrorException("Unknown type " + myInfo.type);
         }
      }
   }

   public int numVertices() {
      switch (myInfo.type) {
         case EDGE_EDGE:
            return 4;
         case VERTEX_FACE:
            return 4;
         case VERTEX_BODY:
            return 1;
         case EDGE_BODY:
            return 2;
         case BODY_FACE:
            return 3;
         default: {
            throw new InternalErrorException("Unknown type " + myInfo.type);
         }
      }
   }

   public void skipAuxState (DataBuffer data) {

      int numv = numMyVertices();
      int nump = numPoints();
      int numb = numFrames();

      // these should never happen
      if (nump < 0 || numb < 0) {
         nump = 0;
         numb = 0;
         throw new InternalErrorException ("Uh oh... FAIL!");
      }

      int incd = 4 + nump + 3 * numb;
      int incz = 1 + (1 + numv) + (1 + nump);

      data.dskip (incd); // 4 for normal/distance, nump for weights per point, 3
      data.zskip (incz);
      // 1 for type, 1 for num nodes, numn for node indices, 1 for number of
      // points,
      // nump for point indices, 1 for numbodies

      // System.out.println("fem inc: " + incd + ", " + incz + " [" +
      // (data.didx-incd) + "," + (data.zidx-incz)+ "]");

   }

   //   @Override
   //   public void getAuxState(DataBuffer data, CollisionData myData,
   //      CollisionData otherData) {
   //      getAuxStateOld(data);
   //   }
   
   // for compatibility
   public void getAuxStateOld(DataBuffer data) {

      data.zput (myInfo.type);
      data.dput (myInfo.normal.x);
      data.dput (myInfo.normal.y);
      data.dput (myInfo.normal.z);
      data.dput (myInfo.distance);

      data.zput (numMyVertices());
      Vertex3d[] myVtxs = getVertices();
      switch (myInfo.type) {
         case BODY_FACE:
            data.zput (myVtxs[2].getIndex());
         case EDGE_EDGE:
         case EDGE_BODY:
            data.zput (myVtxs[1].getIndex());
         case VERTEX_FACE:
         case VERTEX_BODY:
            data.zput (myVtxs[0].getIndex());
      }

      int nump = numPoints();
      data.zput (nump);

      for (CompInfo info = myHeadComp; info != null; info = info.getNext()) {
         if (info instanceof PointInfo) {
            PointInfo pinfo = (PointInfo)info;
            data.dput (pinfo.getWeight());
            int idx = pinfo.getPoint().getNumber();
            data.zput (idx);
         }
         else if (info instanceof FrameInfo) {
            Vector3d loc = ((FrameInfo)info).getLoc();
            data.dput (loc.x);
            data.dput (loc.y);
            data.dput (loc.z);
         }
      }
   }
   
   // for compatibility, assumes both meshes belong to FemModel3d
   public void setAuxStateOld(
      DataBuffer data, FemModel3d model, PolygonalMesh mesh0, 
      PolygonalMesh mesh1, Collidable otherComponent) {

      myInfo.type = data.zget();
      myInfo.normal.x = data.dget();
      myInfo.normal.y = data.dget();
      myInfo.normal.z = data.dget();
      myInfo.distance = data.dget();

      ArrayList<Vertex3d> vtxs0 = null;
      if (mesh0 != null) {
         vtxs0 = mesh0.getVertices();
      }

      // retrieve vertices
      int numv = data.zget();
      
      Vertex3d myVtx0 = null;
      Vertex3d myVtx1 = null;
      Vertex3d myVtx2 = null;

      switch (myInfo.type) {
         case BODY_FACE:
            myVtx2 = (FemMeshVertex)vtxs0.get(data.zget());
            myVtx1 = (FemMeshVertex)vtxs0.get(data.zget());
            myVtx0 = (FemMeshVertex)vtxs0.get(data.zget());
            setVertices(myVtx0, myVtx1, myVtx2);
            break;
         case EDGE_EDGE:
         case EDGE_BODY:
            myVtx1 = (FemMeshVertex)vtxs0.get(data.zget());
            myVtx0 = (FemMeshVertex)vtxs0.get(data.zget());
            setVertices(myVtx0, myVtx1);
            break;
         case VERTEX_FACE:
         case VERTEX_BODY:
            myVtx0 = (FemMeshVertex)vtxs0.get(data.zget());
            setVertices(myVtx0);
            break;
      }


      beginSet();

      // retrieve points
      int nump = data.zget();

      // read vertices
      for (int i = 0; i < numv; i++) {
         int idx = data.zget();
         Point pnt = model.getByNumber(idx);
         addPoint(pnt, data.dget());
      }

      // read rest of points
      if (otherComponent instanceof FemModel3d) {
         FemModel3d other = (FemModel3d)otherComponent;
         for (int i = numv; i < nump; i++) {
            int idx = data.zget();
            Point pnt = other.getByNumber(idx);
            addPoint(pnt, data.dget());
         }
      }
      else if (numv < nump) {
         throw new InternalErrorException (
            "Extra points but otherComponent is " + otherComponent.getClass());
      }

      // read body
      if (myInfo.type == VERTEX_BODY || myInfo.type == EDGE_BODY) {
         RigidBody rb = (RigidBody)otherComponent;
         Point3d loc = new Point3d();
         loc.x = data.dget();
         loc.y = data.dget();
         loc.z = data.dget();
         addFrame(rb, -1, loc);
      } else if (myInfo.type == BODY_FACE) {
         RigidBody rb = (RigidBody)otherComponent;
         Point3d loc = new Point3d();
         loc.x = data.dget();
         loc.y = data.dget();
         loc.z = data.dget();
         addFrame(rb, 1, loc);
      }
   }
   
   public void skipAuxStateOld(DataBuffer data) {
      
      data.zskip(2);
      data.dskip(4);
      
      switch (myInfo.type) {
         case BODY_FACE:
            data.zskip(1);
         case EDGE_EDGE:
         case EDGE_BODY:
            data.zskip(1);
         case VERTEX_FACE:
         case VERTEX_BODY:
            data.zskip(1);
      }

      data.zskip(1);
      for (CompInfo info = myHeadComp; info != null; info = info.getNext()) {
         if (info instanceof PointInfo) {
            data.dskip(1);
            data.zskip(1);
         }
         else if (info instanceof FrameInfo) {
            data.dskip(3);
         }
      }
   }
   

   //   @Override
   //   public double setVertexRigidBody(ContactPenetratingPoint cpp, double mu,
   //      DeformableCollisionData thisData, RigidBodyCollisionData rbData,
   //      boolean useSignedDistanceCollider) {
   //
   //      RigidBody body = rbData.getBody();
   //      RigidTransform3d XBW = body.getPose(); // body to world transform
   //
   //      loc.inverseTransform(XBW, cpp.position);
   //      if (useSignedDistanceCollider) {
   //         normal.set(cpp.normal);
   //      }
   //      else {
   //         normal.transform(XBW, cpp.face.getNormal());
   //      }
   //
   //      FemMeshVertex vtx = (FemMeshVertex)cpp.vertex;
   //      FemNode3d node = (FemNode3d)vtx.getPoint();
   //
   //      setVertexBody(normal, mu, vtx, 1, body, loc, 1);
   //      disp.sub(node.getPosition(), cpp.position);
   //      double dist = disp.dot(normal);
   //      
   //      //      NumberFormat fmt = new NumberFormat ("%.10g");
   //      //      System.out.println("VRB n:" + myInfo.normal.toString(fmt) + " (" + vtx.getIndex() + ")" +
   //      //      " (" + body.getName() + "," + loc.toString(fmt) + ")" + " dist:" + fmt.format (dist));
   //
   //      return dist;
   //   }
   //
   //   @Override
   //   public double setFaceRigidBody(ContactPenetratingPoint cpp,
   //      double mu,
   //      DeformableCollisionData thisData,
   //      RigidBodyCollisionData rbData) {
   //
   //      RigidBody body = rbData.getBody();
   //      FemMeshVertex vtx0 = (FemMeshVertex)cpp.face.getVertex(0);
   //      FemMeshVertex vtx1 = (FemMeshVertex)cpp.face.getVertex(1);
   //      FemMeshVertex vtx2 = (FemMeshVertex)cpp.face.getVertex(2);
   //
   //      double w0 = 1.0 - (cpp.coords.x + cpp.coords.y);
   //      double w1 = cpp.coords.x;
   //      double w2 = cpp.coords.y;
   //
   //      // compute normal from scratch because previous contacts
   //      // may have caused it to change
   //      cpp.face.computeNormal(normal);
   //
   //      RigidTransform3d XBW = body.getPose();
   //      loc.transform(XBW, cpp.vertex.pnt);
   //      disp.sub(loc, cpp.position);
   //      double dist = disp.dot(normal);
   //
   //      setBodyFace(normal, mu, vtx0, vtx1, vtx2, w0, w1, w2,cpp.vertex,
   //         body, cpp.vertex.pnt, 1);
   //
   //      return dist;
   //
   //   }
   //
   //   @Override
   //   public void setEdgeRigidBody(EdgeEdgeContact eec,
   //      double mu,
   //      DeformableCollisionData thisData,
   //      RigidBodyCollisionData rbData) {
   //
   //      RigidBody body = rbData.getBody();
   //      RigidTransform3d XBW = body.getPose(); // body to world transform
   //
   //      FemMeshVertex vt = (FemMeshVertex)eec.edge1.tail;
   //      FemMeshVertex vh = (FemMeshVertex)eec.edge1.head;
   //      double wh = 1.0 - eec.s1;
   //      double wt = eec.s1;
   //
   //      loc.inverseTransform(XBW, eec.point0);
   //      normal.transform(XBW, eec.point1ToPoint0Normal);
   //      normal.normalize();
   //
   //      setEdgeBody(
   //         normal, mu, vt, vh, wt, wh, body, loc, 1);
   //   }

   //   @Override
   //   public void setEdgeEdge(EdgeEdgeContact eec, double mu,
   //      DeformableCollisionData thisData,
   //      DeformableCollisionData otherData) {
   //
   //      FemMeshVertex v0t = (FemMeshVertex)eec.edge0.tail;
   //      FemMeshVertex v0h = (FemMeshVertex)eec.edge0.head;
   //      Vertex3d v1t = eec.edge1.tail;
   //      Vertex3d v1h = eec.edge1.head;
   //      setEdgeEdge(
   //         eec.point1ToPoint0Normal, mu, v0t, v0h, eec.point0, 1 - eec.s0, eec.s0,
   //         v1t, v1h, eec.point1, 1 - eec.s1, eec.s1, otherData);
   //   }
   //
   //   @Override
   //   public double setVertexDeformable(ContactPenetratingPoint cpp,
   //      double mu,
   //      DeformableCollisionData thisData,
   //      DeformableCollisionData otherData) {
   //
   //      double w0 = 1.0 - (cpp.coords.x + cpp.coords.y);
   //      double w1 = cpp.coords.x;
   //      double w2 = cpp.coords.y;
   //      FemMeshVertex vtx0 = (FemMeshVertex)cpp.vertex;
   //      Vertex3d vtx1 = cpp.face.getVertex(0);
   //      Vertex3d vtx2 = cpp.face.getVertex(1);
   //      Vertex3d vtx3 = cpp.face.getVertex(2);
   //      cpp.face.computeNormal(normal);
   //
   //      FemNode3d node0 = (FemNode3d)vtx0.getPoint();
   //      disp.sub(node0.getPosition(), vtx1.getWorldPoint());
   //      double dist = disp.dot(normal);
   //      
   //      //      setVertexFace(
   //      //         normal, mu, vtx0, vtx1, vtx2, vtx3, w0, w1, w2, otherData);
   //
   //      setVertexFace(
   //         normal, mu, vtx0, 1, vtx1, vtx2, vtx3, cpp.position, w0, w1, w2, otherData);
   //
   //      return dist;
   //   }


}
