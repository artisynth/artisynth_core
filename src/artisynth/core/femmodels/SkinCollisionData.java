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
import artisynth.core.mechmodels.DeformableContactConstraintGeneric;
import artisynth.core.mechmodels.DynamicMechComponent;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.PointParticleAttachment;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidBodyCollisionData;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;

public class SkinCollisionData extends DeformableCollisionDataBase {

   protected FemModel3d myModel;
   protected SkinMesh mySMC;

   ContactPenetratingPoint.DistanceComparator cppComparator;

   public SkinCollisionData(SkinMesh smc) {
      super((PolygonalMesh)smc.getMesh(), new DeformableContactConstraintGeneric());
      cppComparator = ContactPenetratingPoint.createMinDistanceComparator();
      mySMC = smc;
   }

   @Override
   public DeformableContactConstraint createContact(Vertex3d... vtxs) {
      return new DeformableContactConstraintGeneric(vtxs);
   }

   protected boolean vertexIsAttached (Vertex3d vtx, Collidable comp) {

      int vidx = vtx.getIndex();
      PointAttachment pa = mySMC.getAttachment (vidx);
      
      // XXX there's probably a much more efficient way of doing this
      // if comp is deformable, do we need to check whether nodes are
      // attached to the face's nodes?  Might need to take in the face
      // as an additional argument
      int numMasters = pa.numMasters ();
      DynamicMechComponent[] masters = pa.getMasters ();
      for (int i=0; i<numMasters; i++) {
         if (masters[i] instanceof Point) {
            Point node = (Point)masters[i];
            if (node.isAttached ()) {
               DynamicMechComponent[] nodeMasters = node.getAttachment().getMasters();
               for (int j=0; j<nodeMasters.length; j++) {
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
         } else if (masters[i] instanceof Frame) {
            Frame frame = (Frame)masters[i];
            if (frame.isAttached ()) {
               DynamicMechComponent[] frameMasters = frame.getAttachment().getMasters();
               for (int j=0; j<frameMasters.length; j++) {
                  // check parent and grandparent in case is fem or 
                  // any other kind of deformable
                  if (frameMasters[j] == comp) {
                     return true;
                  }
                  CompositeComponent parent = frameMasters[j].getParent();
                  if (frameMasters[j] == parent) {
                     return true;
                  }
                  CompositeComponent grandparent = ComponentUtils.getGrandParent(frameMasters[j]);
                  if (frameMasters[j] == grandparent) {
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
         if (((DeformableCollisionData)otherData).numActiveMasters(cpp, !isVertex) > 0) {
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
         PointAttachment pa = mySMC.getAttachment (vidx);
         
         int numMasters = pa.numMasters ();
         DynamicMechComponent[] masters = pa.getMasters ();
         for (int i=0; i<numMasters; i++) {
            if (masters[i] instanceof Point) {
               Point node = (Point)masters[i];
               if (node.isActive ()) {
                  numActive += 1;
               }
            } else if (masters[i] instanceof Frame) {
               Frame frame = (Frame)masters[i];
               if (frame.isActive()) {
                  numActive += 1;
               }
            }
         }
      } else {
         // go through face

         HalfEdge he = cpp.face.firstHalfEdge();
         for (int i=0; i<3; i++) {
            int vidx = he.getHead().getIndex();
            PointAttachment pa = mySMC.getAttachment (vidx);
            
            int numMasters = pa.numMasters ();
            DynamicMechComponent[] masters = pa.getMasters ();
            for (int j=0; i<numMasters; i++) {
               if (masters[j] instanceof Point) {
                  Point node = (Point)masters[j];
                  if (node.isActive()) {
                     numActive++;
                  }
               } else if (masters[j] instanceof Frame) {
                  Frame frame = (Frame)masters[j];
                  if (frame.isActive()) {
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
      PointAttachment pa = mySMC.getAttachment (vidx);
      
      int numMasters = pa.numMasters ();
      DynamicMechComponent[] masters = pa.getMasters ();
      for (int i=0; i<numMasters; i++) {
         if (masters[i] instanceof Point) {
            Point node = (Point)masters[i];
            if (node.isActive()) {
               numActive++;
            }
         } else if (masters[i] instanceof Frame) {
            Frame frame = (Frame)masters[i];
            if (frame.isActive()) {
               numActive++;
            }
         }
      }

      vidx = vh.getIndex();
      pa = mySMC.getAttachment (vidx);
      
      numMasters = pa.numMasters ();
      masters = pa.getMasters ();
      for (int i=0; i<numMasters; i++) {
         if (masters[i] instanceof Point) {
            Point node = (Point)masters[i];
            if (node.isActive()) {
               numActive++;
            }
         } else if (masters[i] instanceof Frame) {
            Frame frame = (Frame)masters[i];
            if (frame.isActive()) {
               numActive++;
            }
         }
      }
      return numActive;
   }

   @Override
   public int getVertexDependencies(Vertex3d vtx, 
      ArrayList<Point> pointDeps, VectorNd pntWgts,
      ArrayList<Frame> frameDeps, VectorNd frameWgts) {

      int vidx = vtx.getIndex();
      PointAttachment pa = mySMC.getAttachment (vidx);
      
      pointDeps.clear();
      frameDeps.clear();
      
      int numPoints = 0;
      int numFrames = 0;
      
      if (pa instanceof PointParticleAttachment) {
         PointParticleAttachment pp = (PointParticleAttachment)pa;
         pntWgts.setSize (1);
         pointDeps.add (pp.getParticle());
         pntWgts.set (0, 1);
         numPoints = 1;
      } else if (pa instanceof PointFem3dAttachment) {
         PointFem3dAttachment pf = (PointFem3dAttachment)pa;
         int size = pf.numMasters ();
         pointDeps.ensureCapacity (size);
         pntWgts.setSize (size);
         FemNode[] masters = pf.getMasters ();
         for (int i=0; i<size; i++) {
            pointDeps.add (masters[i]);
         }
         pntWgts.set (pf.getCoordinates ());
         numPoints = size;
      } else if (pa instanceof PointSkinAttachment) {
         PointSkinAttachment ps = (PointSkinAttachment)pa;
         DynamicMechComponent[] masters = ps.getMasters();
         int numMasters = masters.length;
         pntWgts.setSize(numMasters);
         frameWgts.setSize(numMasters);
         for (int i=0; i<masters.length; i++) {
            if (masters[i] instanceof Point) {
               pntWgts.set(numPoints,ps.getWeight(i));
               pointDeps.add((Point)masters[i]);
               numPoints++;
            } else if (masters[i] instanceof Frame) {
               frameWgts.set(numFrames,ps.getWeight(i));
               frameDeps.add((Frame)masters[i]);
               numFrames++;
            }
         }
         pntWgts.setSize(numPoints);
         pntWgts.setSize(numFrames);
      }
      
      return numPoints + numFrames;
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
   //            for (DynamicMechComponent master : mySMC.getAttachment(vidx).getMasters()) {
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
   //               for (DynamicMechComponent master : mySMC.getAttachment(vidx).getMasters()) {
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
      
      PointAttachment pa = mySMC.getAttachment(vtx.getIndex());
      for (DynamicMechComponent dmc : pa.getMasters()) {
         dmc.setMarked(marked);
      }
      
   }

   @Override
   public int numActiveUnmarkedMasters(Vertex3d vtx) {
      int nm = 0;
      PointAttachment pa = mySMC.getAttachment(vtx.getIndex());
      for (DynamicMechComponent dmc : pa.getMasters()) {
         if (dmc.isActive() && !dmc.isMarked()) {
            nm++;
         }
      }
      return nm;
   }

}
