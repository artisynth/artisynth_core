/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import maspack.collision.ContactPenetratingPoint;
import maspack.collision.EdgeEdgeContact;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.VectorNd;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.CollisionData;
import artisynth.core.mechmodels.CollisionHandler;
import artisynth.core.mechmodels.DeformableCollisionData;
import artisynth.core.mechmodels.DeformableCollisionDataBase;
import artisynth.core.mechmodels.DeformableContactConstraint;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidBodyCollisionData;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;

public class FemCollisionData extends DeformableCollisionDataBase {

   FemModel3d myFemMod;

   public FemCollisionData(FemModel3d mod, PolygonalMesh mesh) {
      super(mesh, new FemContactConstraint());
      myFemMod = mod;
   }

   @Override
   public FemModel3d getComponent() {
      return myFemMod;
   }

   @Override
   public FemContactConstraint createContact(Vertex3d... vtxs) {
      FemMeshVertex v0 = null, v1 = null, v2 = null;

      if (vtxs.length >= 3) {
         v0 = (FemMeshVertex)vtxs[0];
         v1 = (FemMeshVertex)vtxs[1];
         v2 = (FemMeshVertex)vtxs[2];
      } else if (vtxs.length == 2) {
         v0 = (FemMeshVertex)vtxs[0];
         v1 = (FemMeshVertex)vtxs[1];
      } else if (vtxs.length == 1) {
         v0 = (FemMeshVertex)vtxs[0];
      }

      return new FemContactConstraint(v0, v1, v2);
   }


   @Override
   public void addContact(DeformableContactConstraint c) {
      if (c instanceof FemContactConstraint) {
         super.addContact(c);
      } else {
         throw new IllegalArgumentException("Constraint must be of type FemContactConstraint");
      }
   }

   protected boolean vertexIsAttached (Vertex3d vtx, Collidable comp) {

      FemNode node = ((FemMeshVertex)vtx).getPoint();
      if (node.isAttached()) {
         DynamicComponent[] masters = node.getAttachment().getMasters();
         for (int i=0; i<masters.length; i++) {

            // check parent and grandparent in case is fem or any other kind of deformable
            if (masters[i] == comp) {
               return true;
            }
            CompositeComponent parent = masters[i].getParent();
            if (masters[i] == parent) {
               return true;
            }
            CompositeComponent grandparent = ComponentUtils.getGrandParent(masters[i]);
            if (masters[i] == grandparent) {
               return true;
            }

         }
      }
      return false;
   }

   @Override
   public boolean allowCollision(ContactPenetratingPoint cpp, boolean isVertex,
      CollisionData otherData) {

      boolean allow;

      if (isVertex) {
         allow = !(vertexRegionAttachedTo (cpp.vertex, otherData.getComponent()));
      } else {
         allow = !(faceAttachedTo (cpp.face, otherData.getComponent()));
      }

      if (!allow) {
         // if (isVertex && cpp.vertex instanceof FemMeshVertex) {
         //    System.out.println (
         //       "disallowing A for " +
         //       ((FemMeshVertex)cpp.vertex).getPoint().getSolveIndex());
         // }
         return false;
      }

      // check enough DOFs
      if (numActiveMasters(cpp, isVertex) > 0) {
         return true;
      }
      if (otherData instanceof RigidBodyCollisionData) {
         RigidBody body = ((RigidBodyCollisionData)otherData).getBody();      
         if (body.isActive()) {
            return true;
         }
      } else if (otherData instanceof DeformableCollisionData) {
         if (((DeformableCollisionData)otherData).numActiveMasters(
            cpp, !isVertex) > 0) {
            return true;
         }
      }

         // if (isVertex && cpp.vertex instanceof FemMeshVertex) {
         //    System.out.println (
         //       "disallowing B for " +
         //       ((FemMeshVertex)cpp.vertex).getPoint().getSolveIndex());
         // }

      return false;

   }

   @Override
   public boolean allowCollision(EdgeEdgeContact eec, boolean isFirst,
      CollisionData otherData) {

      if (numActiveMasters(eec, isFirst) > 0) {
         return true;
      }
      if (otherData instanceof RigidBodyCollisionData) {
         RigidBody body = ((RigidBodyCollisionData)otherData).getBody();      
         if (body.isActive()) {
            return true;
         }
      } else if (otherData instanceof DeformableCollisionData) {
         if (((DeformableCollisionData)otherData).numActiveMasters(eec, !isFirst) > 0) {
            return true;
         }
      }

      return false;

   }

   @Override
   public int numActiveMasters(ContactPenetratingPoint cpp, boolean isVertex) {

      int numActive = 0;
      if (isVertex) {
         FemMeshVertex fvtx = (FemMeshVertex)cpp.vertex;
         if (fvtx.getPoint().isActive()) {
            numActive = 1;
         }
      } else {
         // go through face

         HalfEdge he = cpp.face.firstHalfEdge();
         for (int i=0; i<3; i++) {
            FemMeshVertex fvtx = (FemMeshVertex)he.getHead();
            if (fvtx.getPoint().isActive()) {
               numActive++;
            }
            he.getNext();
         }
      }

      return numActive;

   }

   @Override
   public int numActiveMasters(EdgeEdgeContact eec, boolean isFirst) {

      // try to determine if any component is active
      HalfEdge he = eec.edge1;
      if (isFirst) {
         he = eec.edge0;
      }
      FemMeshVertex vt = (FemMeshVertex)he.getTail();
      FemMeshVertex vh = (FemMeshVertex)he.getHead();
      FemNode3d nodet = (FemNode3d)vt.getPoint();
      FemNode3d nodeh = (FemNode3d)vh.getPoint();

      int numActive = 0;
      if (nodet.isActive()) {
         numActive++;
      }
      if (nodeh.isActive()) {
         numActive++;
      }
      return numActive;
   }

   //   @Override
   //   public void reduceConstraints(
   //      ArrayList<ContactPenetratingPoint> femPoints,
   //      ArrayList<ContactPenetratingPoint> otherPoints, 
   //      CollisionData otherData) {
   //
   //      
   //      // only keep one per face
   //      HashMap<Face,Double> faceContactDist = new HashMap<Face,Double>();
   //      HashMap<Face,ContactPenetratingPoint> faceContactPoint =
   //      new HashMap<Face,ContactPenetratingPoint>();
   //
   //      // only keep one rbPoint per fem face
   //      if (CollisionHandler.doBodyFaceContact) {
   //      for (ContactPenetratingPoint cpp : otherPoints) {
   //         //         loc.transform (XBW, cpp.vertex.pnt);
   //         //         disp.sub (loc, cpp.position);
   //         //         double d = disp.dot (cpp.face.getNormal());
   //
   //         double d = -cpp.distance;
   //         //System.out.println ("d=" + d + " " + cpp.distance);
   //         Double minDist = faceContactDist.get(cpp.face);
   //         if (minDist == null || d < minDist.doubleValue()) {
   //            minDist = new Double(d);
   //            faceContactDist.put (cpp.face, minDist);
   //            faceContactPoint.put (cpp.face, cpp);
   //         }
   //      }
   //      }
   //
   //      // loop through fem->rb, and if than any face, replace
   //         for (ContactPenetratingPoint cpp : femPoints) {
   //            // XXX may not be a FemMeshVertex...
   //            FemMeshVertex vtx = (FemMeshVertex)cpp.vertex;
   //            FemNode3d node = (FemNode3d)vtx.getPoint();
   //
   //            if (!node.isActive()) {
   //               continue;
   //            }
   //            if (!allowCollision(cpp, true, otherData)) {
   //               continue;
   //            }
   //            //         if (useSignedDistanceCollider) {
   //            //            normal.set (cpp.normal);
   //            //         }
   //            //         else {
   //            //            normal.transform (XBW, cpp.face.getNormal());
   //            //         }
   //            //         disp.sub (node.getPosition(), cpp.position);
   //            //         double dist = disp.dot(normal);
   //            //         
   //            //         if ( Math.abs(dist + cpp.distance)/(Math.abs(dist) + Math.abs(cpp.distance)) > 1e-3 ) {
   //            //            System.out.println("Distances differ");
   //            //         }
   //
   //            double dist = -cpp.distance;
   //
   //            // allow only if larger than all of the faces
   //            boolean add = true;
   //            Iterator<HalfEdge> hit = vtx.getIncidentHalfEdges();
   //            while (hit.hasNext()) {
   //               HalfEdge he = hit.next();
   //               Face f = he.getFace();
   //               Double minDist = faceContactDist.get(f);
   //               if (minDist != null && dist > minDist.doubleValue()) {
   //                  add = false;
   //                  break;
   //               }
   //            }
   //
   //            // if we are adding this vertex in place of all faces
   //            // then replace in the faceContactPoint map
   //            if (add) {
   //               hit = vtx.getIncidentHalfEdges();
   //               while (hit.hasNext()) {
   //                  HalfEdge he = hit.next();
   //                  Face f = he.getFace();
   //                  faceContactPoint.put(f, cpp);
   //                  faceContactDist.put(f, dist);
   //               }
   //            }
   //      }
   //
   //      Collection<ContactPenetratingPoint> cppsToKeep = faceContactPoint.values();
   //      femPoints.retainAll(cppsToKeep);
   //      otherPoints.retainAll(cppsToKeep);
   //
   //   }

   @Override
   public int getVertexDependencies(Vertex3d vtx, ArrayList<Point> pointDeps,
      VectorNd pointWgts, ArrayList<Frame> frameDeps, VectorNd frameWgts) {

      //      pointDeps.clear();
      //      frameDeps.clear();
      //      frameWgts.setSize(0);
      //      if (vtx instanceof FemMeshVertex) {
      //         FemMeshVertex fvtx = (FemMeshVertex)vtx;
      //         pointDeps.add(fvtx.getPoint());
      //         pointWgts.setSize(pointDeps.size());
      //         pointWgts.set(pointWgts.size()-1, 1);
      //         return 1;
      //      } else {
      //         pointWgts.setSize(0);
      //      }
	     if (vtx instanceof FemMeshVertex) {
	         FemMeshVertex fvtx = (FemMeshVertex)vtx;
	         pointDeps.add(fvtx.getPoint());
	         pointWgts.setSize(pointDeps.size());
	         pointWgts.set(pointWgts.size()-1, 1);
	         return 1;
	      }
      return 0;
   }

   @Override
   public int addConstraintInfo(Vertex3d vtx, Point3d constraintLoc,
      double weight, DeformableContactConstraint con) {

      if (vtx instanceof FemMeshVertex) {
         con.addPoint(((FemMeshVertex)vtx).getPoint(), weight);
         return 1;
      }
      return 0;
   }

   @Override
   public void markMasters(Vertex3d vtx, boolean marked) {
      FemMeshVertex fmv = (FemMeshVertex)vtx;
      FemNode node = fmv.getPoint();
      node.setMarked(marked);
   }

   @Override
   public int numActiveUnmarkedMasters(Vertex3d vtx) {
      FemMeshVertex fmv = (FemMeshVertex)vtx;
      FemNode node = fmv.getPoint();
      if (node.isActive() && !node.isMarked()) {
         return 1;
      }
      return 0;
   }



}
