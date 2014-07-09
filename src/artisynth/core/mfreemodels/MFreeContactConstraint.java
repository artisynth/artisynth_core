/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;

import maspack.collision.ContactPenetratingPoint;
import maspack.collision.EdgeEdgeContact;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import artisynth.core.mechmodels.DeformableCollisionData;
import artisynth.core.mechmodels.DeformableContactConstraintGeneric;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidBodyCollisionData;

public class MFreeContactConstraint extends
   DeformableContactConstraintGeneric {

   // temp variables
   private Point3d loc = new Point3d();
   private Vector3d normal = new Vector3d();
   private Vector3d disp = new Vector3d();
   
   /**
    * Creates a new contact constraint identified by three vertices.
    */
   public MFreeContactConstraint (Vertex3d... vtxs) {
      setVertices(vtxs);
      myActive = false;
   }

   protected void addPoint(MFreeVertex3d fvtx, double weight) {

      ArrayList<MFreeNode3d> nodes = fvtx.getDependentNodes();
      VectorNd coords = fvtx.getNodeCoordinates();

      for (int i = 0; i < nodes.size(); i++) {
         addPoint(nodes.get(i), weight * coords.get(i));
      }
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

      MFreeVertex3d vtx = (MFreeVertex3d)cpp.vertex;

      myInfo.type = VERTEX_BODY;
      myInfo.normal.set(normal);
      setFriction(mu);
      
      beginSet();
      addPoint(vtx, 1);
      addFrame(body, -1, loc);
      endSet();
      
      double dist = -cpp.distance;

      return dist;
   }

   @Override
   public double setFaceRigidBody(ContactPenetratingPoint cpp,
      double mu,
      DeformableCollisionData thisData,
      RigidBodyCollisionData rbData) {

      RigidBody body = rbData.getBody();
      MFreeVertex3d vtx0 = (MFreeVertex3d)cpp.face.getVertex(0);
      MFreeVertex3d vtx1 = (MFreeVertex3d)cpp.face.getVertex(1);
      MFreeVertex3d vtx2 = (MFreeVertex3d)cpp.face.getVertex(2);

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
      
      myInfo.type = BODY_FACE;
      myInfo.normal.set(normal);
      myInfo.normal.negate();  // reverse, since belongs to face
      setFriction(mu);
      
      beginSet();
      addPoint(vtx0, w0);
      addPoint(vtx1, w1);
      addPoint(vtx2, w2);
      // XXX was set to cpp.vertex.pnt
      addFrame(body, -1, loc);
      endSet();
      
      return dist;

   }

   @Override
   public void setEdgeRigidBody(EdgeEdgeContact eec,
      double mu,
      DeformableCollisionData thisData,
      RigidBodyCollisionData rbData) {

      RigidBody body = rbData.getBody();
      RigidTransform3d XBW = body.getPose(); // body to world transform

      MFreeVertex3d vt = (MFreeVertex3d)eec.edge1.tail;
      MFreeVertex3d vh = (MFreeVertex3d)eec.edge1.head;
      double wh = 1.0 - eec.s1;
      double wt = eec.s1;

      loc.inverseTransform(XBW, eec.point1);
      normal.transform(XBW, eec.point1ToPoint0Normal);
      normal.normalize();
      
      myInfo.type = EDGE_BODY;
      myInfo.normal.set(normal);
      setFriction(mu);
      
      beginSet();
      addPoint(vt, wt);
      addPoint(vh, wh);
      addFrame(body, -1, loc);
      endSet();
      
   }

   @Override
   public void setEdgeEdge(EdgeEdgeContact eec, double mu,
      DeformableCollisionData thisData, DeformableCollisionData otherData) {
      MFreeVertex3d v0t = (MFreeVertex3d)eec.edge0.tail;
      MFreeVertex3d v0h = (MFreeVertex3d)eec.edge0.head;
      Vertex3d v1t = eec.edge1.tail;
      Vertex3d v1h = eec.edge1.head;
      
      myInfo.type = EDGE_EDGE;
      myInfo.normal.set(eec.point1ToPoint0Normal);
      setFriction(mu);
      
      beginSet();
      // add vertices
      addPoint(v0t, 1 - eec.s0);
      addPoint(v0h, eec.s0);
      otherData.addConstraintInfo(v1t, eec.point1, eec.s1-1, this);
      otherData.addConstraintInfo(v1h, eec.point1, -eec.s1, this);
      endSet();
      
   }

   @Override
   public double setVertexDeformable(ContactPenetratingPoint cpp,
      double mu,
      DeformableCollisionData thisData, DeformableCollisionData otherData) {
      double w0 = 1.0 - (cpp.coords.x + cpp.coords.y);
      double w1 = cpp.coords.x;
      double w2 = cpp.coords.y;

      MFreeVertex3d vtx0 = (MFreeVertex3d)cpp.vertex;
      Vertex3d vtx1 = cpp.face.getVertex(0);
      Vertex3d vtx2 = cpp.face.getVertex(1);
      Vertex3d vtx3 = cpp.face.getVertex(2);
      cpp.face.computeNormal(normal);

      double dist = -cpp.distance;

      myInfo.type = VERTEX_FACE;
      myInfo.normal.set(normal);
      setFriction(mu);
      
      beginSet();
      addPoint(vtx0, 1);
      otherData.addConstraintInfo(vtx1, cpp.position, -w0, this);
      otherData.addConstraintInfo(vtx2, cpp.position, -w1, this);
      otherData.addConstraintInfo(vtx3, cpp.position, -w2, this);
      endSet();

      return dist;
   }

}
