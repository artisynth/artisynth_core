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

import maspack.collision.ContactPenetratingPoint;
import maspack.collision.EdgeEdgeContact;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.VectorNd;
import maspack.util.BinaryHeap;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.CollisionData;
import artisynth.core.mechmodels.CollisionHandler;
import artisynth.core.mechmodels.DeformableCollisionData;
import artisynth.core.mechmodels.DeformableCollisionDataBase;
import artisynth.core.mechmodels.DeformableContactConstraint;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.PointParticleAttachment;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidBodyCollisionData;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;

public class EmbeddedCollisionData extends DeformableCollisionDataBase {

   protected FemModel3d myModel;
   protected FemMesh myFMC;

   ContactPenetratingPoint.DistanceComparator cppComparator;

   public EmbeddedCollisionData(FemModel3d mod, FemMesh fmc) {
      super((PolygonalMesh)fmc.getMesh());
      myModel = mod;
      cppComparator = ContactPenetratingPoint.createMinDistanceComparator();
      myFMC = fmc;
   }

   @Override
   public DeformableContactConstraint createContact(Vertex3d... vtxs) {
      return new EmbeddedContactConstraint(vtxs);
      //return new FemContactConstraint(vtxs);
   }

   protected boolean vertexIsAttached (Vertex3d vtx, Collidable comp) {

      int vidx = vtx.getIndex();
      PointAttachment pa = myFMC.getAttachment (vidx);
      
      // XXX there's probably a much more efficient way of doing this
      // if comp is deformable, do we need to check whether nodes are
      // attached to the face's nodes?  Might need to take in the face
      // as an additional argument
      int numMasters = pa.numMasters ();
      DynamicComponent[] masters = pa.getMasters ();
      for (int i=0; i<numMasters; i++) {
         if (masters[i] instanceof FemNode3d) {
            FemNode3d node = (FemNode3d)masters[i];
            if (node.isAttached ()) {
               DynamicComponent[] nodeMasters = node.getAttachment().getMasters();
               for (int j=0; j<masters.length; j++) {
                  // check parent and grandparent in case is fem or 
                  // any other kind of deformable
                  if (nodeMasters[j] == comp) {
                     return true;
                  }
                  CompositeComponent parent = nodeMasters[j].getParent();
                  if (nodeMasters[j] == parent) {
                     return true;
                  }
                  CompositeComponent grandparent = ComponentUtils.getGrandParent(nodeMasters[j]);
                  if (nodeMasters[j] == grandparent) {
                     return true;
                  }
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
         int vidx = cpp.vertex.getIndex();
         PointAttachment pa = myFMC.getAttachment (vidx);
         
         int numMasters = pa.numMasters ();
         DynamicComponent[] masters = pa.getMasters ();
         for (int i=0; i<numMasters; i++) {
            if (masters[i] instanceof FemNode3d) {
               FemNode3d node = (FemNode3d)masters[i];
               if (node.isActive ()) {
                  numActive += 1;
               }
            }
         }
      } else {
         // go through face

         HalfEdge he = cpp.face.firstHalfEdge();
         for (int i=0; i<3; i++) {
            int vidx = he.getHead().getIndex();
            PointAttachment pa = myFMC.getAttachment (vidx);
            
            int numMasters = pa.numMasters ();
            DynamicComponent[] masters = pa.getMasters ();
            for (int j=0; i<numMasters; i++) {
               if (masters[j] instanceof FemNode3d) {
                  FemNode3d node = (FemNode3d)masters[j];
                  if (node.isActive()) {
                     numActive++;
                  }
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
      Vertex3d vt = he.getTail();
      Vertex3d vh = he.getHead();

      int numActive = 0;

      int vidx = vt.getIndex();
      PointAttachment pa = myFMC.getAttachment (vidx);
      
      int numMasters = pa.numMasters ();
      DynamicComponent[] masters = pa.getMasters ();
      for (int i=0; i<numMasters; i++) {
         if (masters[i] instanceof FemNode3d) {
            FemNode3d node = (FemNode3d)masters[i];
            if (node.isActive()) {
               numActive++;
            }
         }
      }

      vidx = vh.getIndex();
      pa = myFMC.getAttachment (vidx);
      
      numMasters = pa.numMasters ();
      masters = pa.getMasters ();
      for (int i=0; i<numMasters; i++) {
         if (masters[i] instanceof FemNode3d) {
            FemNode3d node = (FemNode3d)masters[i];
            if (node.isActive()) {
               numActive++;
            }
         }
      }
      return numActive;
   }

   @Override
   public int getVertexDependencies(Vertex3d vtx, ArrayList<Point> pointDeps,
      VectorNd pointWgts, ArrayList<Frame> frameDeps, VectorNd frameWgts) {

      int vidx = vtx.getIndex();
      PointAttachment pa = myFMC.getAttachment (vidx);
      
      pointDeps.clear();
      frameDeps.clear();
      frameWgts.setSize(0);
      pointWgts.setSize(0);
      
      int ndeps = 0;
      
      if (pa instanceof PointParticleAttachment) {
         PointParticleAttachment pp = (PointParticleAttachment)pa;
         pointDeps.add(pp.getParticle());
         pointWgts.setSize(pointDeps.size());
         pointWgts.set(pointWgts.size()-1, 1);
         ndeps = 1;
      } else if (pa instanceof PointFem3dAttachment) {
         PointFem3dAttachment pf = (PointFem3dAttachment)pa;
         int size = pf.numMasters ();
         
         pointWgts.setSize (pointWgts.size() + size);
         FemNode[] masters = pf.getMasters ();
         for (int i=0; i<size; i++) {
            pointDeps.add (masters[i]);
         }
         pointWgts.setSubVector(pointWgts.size()-size, pf.getCoordinates ());
         ndeps = size;
      }
      
      return ndeps;
   }

   //   @Override
   //   public void reduceConstraints(ArrayList<ContactPenetratingPoint> defPoints,
   //      ArrayList<ContactPenetratingPoint> otherPoints,
   //      CollisionData otherData) {
   //
   //      // sort all constraints by distance, keep one per node?
   //      BinaryHeap<ContactPenetratingPoint> bh = new BinaryHeap<ContactPenetratingPoint>(cppComparator);
   //      bh.addAll(defPoints);
   //      bh.addAll(otherPoints);
   //
   //      HashMap<FemNode3d, ContactPenetratingPoint> constraintMap 
   //         = new HashMap<FemNode3d,ContactPenetratingPoint>();
   //
   //      while (bh.size() > 0) {
   //         ContactPenetratingPoint cpp = bh.poll();
   //
   //         if (!CollisionHandler.doBodyFaceContact || defPoints.contains(cpp)) {
   //            
   //            // vertex belongs to fem
   //            int vidx = cpp.vertex.getIndex();
   //
   //            for (DynamicMechComponent master : myFMC.getAttachment(vidx).getMasters()) {
   //               if (master instanceof FemNode3d ) {
   //                  FemNode3d node = (FemNode3d)master;
   //                  if (!constraintMap.containsKey(node)) {
   //                     constraintMap.put(node, cpp);
   //                  }
   //               }
   //            }
   //
   //
   //         } else if (CollisionHandler.doBodyFaceContact){
   //
   //            // face belongs to fem
   //            HalfEdge he = cpp.face.firstHalfEdge();
   //            for (int i=0; i<3; i++) {
   //
   //               int vidx = he.getHead().getIndex();
   //               for (DynamicMechComponent master : myFMC.getAttachment(vidx).getMasters()) {
   //                  if (master instanceof FemNode3d ) {
   //                     FemNode3d node = (FemNode3d)master;
   //                     if (!constraintMap.containsKey(node)) {
   //                        constraintMap.put(node, cpp);
   //                     }
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
   //      defPoints.retainAll(retainVals);
   //      otherPoints.retainAll(retainVals); 
   //
   //   }

   @Override
   public Collidable getComponent() {
      return myModel;
   }

   @Override
   public void markMasters(Vertex3d vtx, boolean marked) {
      
      PointAttachment pa = myFMC.getAttachment(vtx.getIndex());
      for (DynamicComponent dmc : pa.getMasters()) {
         dmc.setMarked(marked);
      }
      
   }

   @Override
   public int numActiveUnmarkedMasters(Vertex3d vtx) {
      int nm = 0;
      PointAttachment pa = myFMC.getAttachment(vtx.getIndex());
      for (DynamicComponent dmc : pa.getMasters()) {
         if (dmc.isActive() && !dmc.isMarked()) {
            nm++;
         }
      }
      return nm;
   }

}
