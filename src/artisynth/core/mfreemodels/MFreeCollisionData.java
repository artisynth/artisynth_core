/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import maspack.collision.ContactPenetratingPoint;
import maspack.collision.EdgeEdgeContact;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.VectorNd;
import maspack.util.BinaryHeap;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.CollisionData;
import artisynth.core.mechmodels.CollisionHandler;
import artisynth.core.mechmodels.DeformableCollisionData;
import artisynth.core.mechmodels.DeformableCollisionDataBase;
import artisynth.core.mechmodels.DeformableContactConstraint;
import artisynth.core.mechmodels.DynamicMechComponent;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidBodyCollisionData;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;

public class MFreeCollisionData extends DeformableCollisionDataBase {

   protected MFreeModel3d myModel;
   ContactPenetratingPoint.DistanceComparator cppComparator;


   public MFreeCollisionData(MFreeModel3d mod, PolygonalMesh mesh) {
      super(mesh);
      myModel = mod;
      cppComparator = ContactPenetratingPoint.createMinDistanceComparator();
   }

   @Override
   public MFreeModel3d getComponent() {
      return myModel;
   }

   public MFreeContactConstraint createContact(Vertex3d... vtxs) {
      return new MFreeContactConstraint(vtxs);
   }

   @Override
   public void addContact(DeformableContactConstraint c) {
      if (c instanceof MFreeContactConstraint) {
         super.addContact(c);
      } else {
         throw new IllegalArgumentException("Constraint must be of type MFreeContactConstraint");
      }
   }

   protected boolean vertexIsAttached (Vertex3d vtx, Collidable comp) {

      ArrayList<MFreeNode3d> nodes = ((MFreeVertex3d)vtx).getDependentNodes();
      for (MFreeNode3d node : nodes) {
         if (node.isAttached()) {
            DynamicMechComponent[] masters = node.getAttachment().getMasters();
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

      return false;
   }

   @Override
   public int numActiveMasters(ContactPenetratingPoint cpp, boolean isVertex) {

      int numActive = 0;
      if (isVertex) {
         MFreeVertex3d fvtx = (MFreeVertex3d)cpp.vertex;

         for (MFreeNode3d node : fvtx.getDependentNodes()) {
            if (node.isActive()) {
               numActive += 1;
            }
         }
      } else {
         // go through face

         HalfEdge he = cpp.face.firstHalfEdge();
         for (int i=0; i<3; i++) {
            MFreeVertex3d fvtx = (MFreeVertex3d)he.getHead();
            for (MFreeNode3d node : fvtx.getDependentNodes()) {
               if (node.isActive()) {
                  numActive++;
               }
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
      MFreeVertex3d vt = (MFreeVertex3d)he.getTail();
      MFreeVertex3d vh = (MFreeVertex3d)he.getHead();

      int numActive = 0;

      for (MFreeNode3d node : vt.getDependentNodes()) {
         if (node.isActive()) {
            numActive++;
         }
      }
      for (MFreeNode3d node : vh.getDependentNodes()) {
         if (node.isActive()) {
            numActive++;
         }
      }
      return numActive;
   }


   //   @Override
   //   public void reduceConstraints(
   //      ArrayList<ContactPenetratingPoint> femPoints,
   //      ArrayList<ContactPenetratingPoint> otherPoints, 
   //      CollisionData otherData) {
   //
   //      // sort all constraints by distance, keep one per node?
   //      BinaryHeap<ContactPenetratingPoint> bh = new BinaryHeap<ContactPenetratingPoint>(cppComparator);
   //      bh.addAll(femPoints);
   //      bh.addAll(otherPoints);
   //
   //      HashMap<MFreeNode3d, ContactPenetratingPoint> constraintMap 
   //      = new HashMap<MFreeNode3d,ContactPenetratingPoint>();
   //
   //
   //      while (bh.size() > 0) {
   //         ContactPenetratingPoint cpp = bh.poll();
   //
   //         boolean added = false;
   //         if (!CollisionHandler.doBodyFaceContact || femPoints.contains(cpp)) {
   //            // vertex belongs to mfree
   //
   //            MFreeVertex3d mvtx = (MFreeVertex3d)cpp.vertex;
   //
   //            for (MFreeNode3d node : mvtx.getDependentNodes()) {
   //               if (!constraintMap.containsKey(node)) {
   //                  constraintMap.put(node, cpp);
   //                  added = true;
   //               }
   //            }
   //
   //
   //         } else {
   //
   //            // face belongs to mfree
   //            HalfEdge he = cpp.face.firstHalfEdge();
   //            for (int i=0; i<3; i++) {
   //               MFreeVertex3d fvtx = (MFreeVertex3d)he.getHead();
   //               for (MFreeNode3d node : fvtx.getDependentNodes()) {
   //                  if (!constraintMap.containsKey(node)) {
   //                     constraintMap.put(node, cpp);
   //                     added = true;
   //                  }
   //               }
   //               he.getNext();
   //            }
   //
   //         }
   //      }
   //
   //      Collection<ContactPenetratingPoint> retainVals = constraintMap.values();
   //
   //      femPoints.retainAll(retainVals);
   //      otherPoints.retainAll(retainVals);            
   //
   //   }

   @Override
   public int getVertexDependencies(Vertex3d vtx, ArrayList<Point> pointDeps,
      VectorNd pointWgts, ArrayList<Frame> frameDeps, VectorNd frameWgts) {

      frameDeps.clear();
      frameWgts.setSize(0);
      if (vtx instanceof MFreeVertex3d) {
         MFreeVertex3d mvtx = (MFreeVertex3d)vtx;

         int nNodes = mvtx.getDependentNodes().size();

         pointDeps.clear();
         pointDeps.addAll(mvtx.getDependentNodes());
         pointWgts.setSize(pointDeps.size());
         VectorNd coords = mvtx.getNodeCoordinates();

         int idxStart = pointDeps.size()-nNodes; 
         for (int i=0; i<nNodes; i++) {
            pointWgts.set(idxStart+i, coords.get(i));
         }
         return nNodes;
      } else {
         pointWgts.setSize(0);
      }

      return 0;
   }

   @Override
   public int addConstraintInfo(Vertex3d vtx, Point3d constraintLoc,
      double weight, DeformableContactConstraint con) {

      if (vtx instanceof MFreeVertex3d) {
         MFreeVertex3d mvtx = (MFreeVertex3d)vtx;

         ArrayList<MFreeNode3d> nodes = mvtx.getDependentNodes();
         VectorNd coords = mvtx.getNodeCoordinates();
         int nNodes = nodes.size();

         for (int i=0; i<nNodes; i++) {
            con.addPoint(nodes.get(i), weight*coords.get(i));
         }
         return nNodes;
      }

      return 0;

   }

   @Override
   public void markMasters(Vertex3d vtx, boolean marked) {
      MFreeVertex3d mvtx = (MFreeVertex3d)vtx;
      for (MFreeNode3d node : mvtx.getDependentNodes()) {
         node.setMarked(marked);
      }
      
   }

   @Override
   public int numActiveUnmarkedMasters(Vertex3d vtx) {
      int nm = 0;
      MFreeVertex3d mvtx = (MFreeVertex3d)vtx;
      for (MFreeNode3d node : mvtx.getDependentNodes()) {
         if (node.isActive() && !node.isMarked()) {
            nm++;
         }
      }
      return nm;
   }



}
