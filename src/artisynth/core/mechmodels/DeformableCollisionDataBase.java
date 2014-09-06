/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import maspack.collision.ContactPenetratingPoint;
import maspack.collision.ContactPenetratingPoint.DistanceComparator;
import maspack.collision.EdgeEdgeContact;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.util.BinaryHeap;
import maspack.util.DataBuffer;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;

public abstract class DeformableCollisionDataBase 
implements DeformableCollisionData {

   protected PolygonalMesh myMesh;

   protected double myPenetrationTol;
   protected double myMu;

   private boolean myContactsChanged = true;
   private DeformableContactConstraint myContactStub;   // used for hashmap

   private static final ContactPenetratingPoint.DistanceComparator maxCppComparator = 
      ContactPenetratingPoint.createMaxDistanceComparator();
   
   protected LinkedHashMap<DeformableContactConstraint,DeformableContactConstraint> myConstraints;

   public DeformableCollisionDataBase(PolygonalMesh mesh, DeformableContactConstraint stub) {
      myMesh = mesh;
      myConstraints = new LinkedHashMap<DeformableContactConstraint,
         DeformableContactConstraint>();
      myContactStub = stub;
   }

   public DeformableCollisionDataBase(PolygonalMesh mesh) {
      this(mesh, null);
      myContactStub = createContact();
   }


   @Override
   public PolygonalMesh getMesh() {
      return myMesh;
   }

   @Override
   public void getBilateralSizes (VectorNi sizes) {
      for (int i=0; i<myConstraints.values().size(); i++) {
         sizes.append (1);
      }
   }

   @Override
   public int getBilateralInfo(ConstraintInfo[] ginfo, int idx) {
      for (DeformableContactConstraint c : myConstraints.values()) {
         c.setSolveIndex (idx);
         ConstraintInfo gi = ginfo[idx++];
         if (c.getDistance() < -myPenetrationTol) {
            gi.dist = (c.getDistance() + myPenetrationTol);
         }
         else {
            gi.dist = 0;
         }
      }
      return idx;
   }

   @Override
   public int addBilateralConstraints(SparseBlockMatrix GT, VectorNd dg,
      int numb) {

      double[] dbuf = (dg != null ? dg.getBuffer() : null);

      for (DeformableContactConstraint c : myConstraints.values()) {
         c.addConstraintBlocks (GT, GT.numBlockCols());
         if (dbuf != null) {
            dbuf[numb] = c.getDerivative();
         }
         numb++;
      }
      return numb;
   }

   @Override
   public int maxFrictionConstraintSets() {
      return myConstraints.values().size();
   }

   @Override
   public int addFrictionConstraints(SparseBlockMatrix DT,
      FrictionInfo[] finfo, int numf) {
      for (DeformableContactConstraint c : myConstraints.values()) {
         numf = c.addFrictionConstraints (DT, finfo, numf);
      }
      return numf;
   }

   @Override
   public void updateFrictionConstraints() {
      for (DeformableContactConstraint c : myConstraints.values()) {
         c.updateFriction ();
      }
   }

   @Override
   public int setBilateralImpulses(VectorNd lam, int idx) {
      for (DeformableContactConstraint c : myConstraints.values()) {
         c.setImpulse (lam.get (idx++));
      }
      return idx;
   }

   @Override
   public void zeroImpulses() {
      for (DeformableContactConstraint c : myConstraints.values()) {
         c.setImpulse (0);
      }   
   }

   @Override
   public int getBilateralImpulses(VectorNd lam, int idx) {
      for (DeformableContactConstraint c : myConstraints.values()) {
         lam.set (idx++, c.getImpulse());
      }
      return idx;
   }

   @Override
   public void clearContactActivity() {
      for (DeformableContactConstraint c : myConstraints.values()) {
         c.setActive (false);
      }
   }

   @Override
   public boolean hasActiveContact(
      ContactPenetratingPoint cpp, boolean isVertex) {

      DeformableContactConstraint cons = null;
      if (isVertex) {
         cons = getContact(cpp.vertex);
      } else {
         Vertex3d[] vtxs = cpp.face.getVertices();
         cons = getContact(vtxs);
      }

      return (cons != null && cons.isActive());

   }

   protected DeformableContactConstraint getContact(
      Vertex3d... vtxs) {
      myContactStub.setVertices(vtxs);
      return myConstraints.get(myContactStub);
   }

   @Override
   public DeformableContactConstraint getContact(ContactPenetratingPoint cpp,
      boolean isVertex) {

      DeformableContactConstraint cons = null;
      Vertex3d[] vtxs = null;
      if (isVertex) {
         vtxs = new Vertex3d[]{cpp.vertex};
         cons = getContact(vtxs);
      } else {
         vtxs = cpp.face.getVertices();
         cons  = getContact(vtxs);
      }


      if (cons == null) {
         cons = createContact(vtxs);
         return cons;
      }
      else // contact already exists
      {
         double lam = cons.getImpulse();
         // do not activate constraint if contact is trying to separate
         if (lam < 0) {
            return null;
         }
         else { // will need to set this active later
            return cons;
         }
      }
   }

   @Override
   public DeformableContactConstraint getContact(EdgeEdgeContact eec, boolean isFirst) {

      Vertex3d v0=null, v1 = null;

      HalfEdge he;
      if (!isFirst) {
         he = eec.edge0;
      } else {
         he = eec.edge1;
      }
      v0 = he.head;
      v1 = he.tail;

      DeformableContactConstraint cons = getContact(v0, v1);

      if (cons == null) {
         cons = createContact(v0, v1);
         return cons;
      }
      else // contact already exists
      {
         double lam = cons.getImpulse();
         // do not activate constraint if contact is trying to separate
         if (lam < 0) {
            return null;
         }
         else { // will need to set this active later
            return cons;
         }
      }
   }

   @Override
   public void addContact(DeformableContactConstraint c) {
      myConstraints.put (c, c);
      myContactsChanged = true;
   }

   @Override
   public boolean hasActiveContact(EdgeEdgeContact eec, boolean isFirst) {

      Vertex3d v0=null, v1 = null;

      HalfEdge he;
      if (!isFirst) {
         he = eec.edge0;
      } else {
         he = eec.edge1;
      }
      v0 = (Vertex3d)he.head;
      v1 = (Vertex3d)he.tail;

      DeformableContactConstraint cons = getContact(v0, v1);
      return (cons != null && cons.isActive());
   }

   @Override
   public void removeInactiveContacts() {
      Iterator<DeformableContactConstraint> it =
         myConstraints.values().iterator();
      while (it.hasNext()) {
         DeformableContactConstraint c = it.next();
         if (!c.isActive()) {
            it.remove();
            myContactsChanged = true;
         }
      }
   }

   public boolean hasActiveContacts() {
      for (DeformableContactConstraint c : myConstraints.values()) {
         if (c.isActive()) {
            return true;
         }
      }
      return false;
   }

   public int numActiveContacts() {

      int idx = 0;
      for (DeformableContactConstraint c : myConstraints.values()) {
         if (c.isActive()) {
            idx++;
         }
      }
      return idx;

   }

   @Override
   public void notifyContactsChanged() {
      myContactsChanged = true;
   }

   @Override
   public boolean contactsHaveChanged() {
      boolean changed = myContactsChanged;
      myContactsChanged = false;
      return changed;
   }

   @Override
   public void clearContactData() {
      myConstraints.clear();
   }

   @Override
   public void skipAuxState(DataBuffer data) {
      data.zskip (2);
   }

   @Override
   public void getAuxState(DataBuffer data, CollisionData otherData) {
      data.zput (myConstraints.size());
      data.zput ((myContactsChanged ? 1 : 0));
      for (DeformableContactConstraint c : myConstraints.values()) {
         c.getAuxState (data, this, otherData);
      }
   }

   @Override
   public void setAuxState(DataBuffer data, CollisionData other) {
      clearContactData();
      int nc = data.zget();
      myContactsChanged = (data.zget() == 1);
      for (int i=0; i<nc; i++) {
         DeformableContactConstraint c = createContact();
         c.setAuxState (data, this, other);
         c.setFriction (myMu);
         myConstraints.put (c, c);
      }
   }

   @Override
   public void getInitialAuxState(DataBuffer newData, DataBuffer oldData) {
      // just create a state in which there are no contacts
      newData.zput (0); // no contact constraints
      newData.zput (1); // contacts have changed
   }

   @Override
   public void setPenetrationTol(double tol) {
      myPenetrationTol = tol;
   }

   @Override
   public double getCollisionTol() {
      return myPenetrationTol;
   }

   @Override
   public void setFriction(double mu) {
      myMu = mu;
   }

   @Override
   public double getFriction() {
      return myMu;
   }

   protected boolean vertexIsAttached (Vertex3d vtx, Collidable comp) {

      // ArrayList<MFreeNode3d> nodes = ((MFreeVertex3d)vtx).getDependentNodes();
      ArrayList<Point> nodes = new ArrayList<Point>();
      VectorNd wgts = new VectorNd();
      ArrayList<Frame> frames = new ArrayList<Frame>();
      VectorNd frameWgts = new VectorNd();
      getVertexDependencies(vtx, nodes, wgts, frames, frameWgts);

      for (Point node : nodes) {
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
      }
      return false;
   }

   protected boolean faceAttachedTo (Face face, Collidable comp) {
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         if (vertexIsAttached (he.getHead(), comp)) {
            return true;
         }
         he = he.getNext();
      }
      while (he != he0);
      return false;
   }

   protected boolean vertexRegionAttachedTo (Vertex3d vtx, Collidable comp) {
      if (vertexIsAttached (vtx, comp)) {
         return true;
      }
      Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
      while (it.hasNext()) {
         HalfEdge he = it.next();
         if (vertexIsAttached (he.getTail(), comp)) {
            return true;
         }
      }
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
   public int addConstraintInfo(Vertex3d vtx, Point3d pnt, double weight,
      DeformableContactConstraint con) {

      ArrayList<Point> deps = new ArrayList<Point>();
      VectorNd wgts = new VectorNd();
      ArrayList<Frame> frameDeps = new ArrayList<Frame>();
      VectorNd frameWgts = new VectorNd();
      int ndeps = getVertexDependencies(vtx, deps, wgts, frameDeps, frameWgts);
      Point3d loc = new Point3d();
      
      for (int i=0; i<deps.size(); i++) {
         con.addPoint(deps.get(i), weight*wgts.get(i));
      }
      for (int i=0; i<frameDeps.size(); i++) {
         Frame frame = frameDeps.get(i);
         loc.inverseTransform(frame.getPose(), pnt);
         con.addFrame(frame, weight*frameWgts.get(i), loc);
      }

      return ndeps;

   }
    
   
   @Override
   public void reduceConstraints(
      ArrayList<ContactPenetratingPoint> myPoints,
      ArrayList<ContactPenetratingPoint> otherPoints, 
      CollisionData otherData) {
      
      ArrayList<ContactPenetratingPoint> points0 = new ArrayList<ContactPenetratingPoint>(myPoints.size());
      ArrayList<ContactPenetratingPoint> points1 = new ArrayList<ContactPenetratingPoint>(otherPoints.size());
      
      BinaryHeap<ContactPenetratingPoint> bh0 = new BinaryHeap<ContactPenetratingPoint>(maxCppComparator);
      bh0.addAll(myPoints);
      BinaryHeap<ContactPenetratingPoint> bh1 = new BinaryHeap<ContactPenetratingPoint>(maxCppComparator);
      bh1.addAll(otherPoints);
      
      ContactPenetratingPoint cpp0 = bh0.poll();
      ContactPenetratingPoint cpp1 = bh1.poll();
      while (cpp0 != null || cpp1 != null) {
         
         // Find next deepest penetrating distance
         boolean isFirst = true;
         ContactPenetratingPoint cpp = null;
         if (cpp0 != null) {
            if (cpp1 != null) {
               if (cpp0.distance >= cpp1.distance) {
                  cpp = cpp0;
                  isFirst = true;
               } else {
                  cpp = cpp1;
                  isFirst = false;
               }
            } else {
               cpp = cpp0;
               isFirst = true;
            }
         } else {
            cpp = cpp1;
            isFirst = false;
         }
         
         // check number of free components
         if (isFirst) {
            int nFree = 0;
            nFree = numActiveUnmarkedMasters(cpp.vertex);
            if (nFree == 0) {
               HalfEdge he = cpp.face.firstHalfEdge();
               for (int i=0; i<3; i++) {
                  nFree = otherData.numActiveUnmarkedMasters(he.head);
                  he = he.getNext();
                  if (nFree > 0) {
                     break;
                  }
               }
            }
            if (nFree > 0) {
               points0.add(cpp);
               
               // mark masters
               markMasters(cpp.vertex, true);
               HalfEdge he = cpp.face.firstHalfEdge();
               for (int i=0; i<3; i++) {
                  otherData.markMasters(he.head, true);
                  he = he.getNext();
               }
            }
            cpp0 = bh0.poll();
         } else {
            int nFree = 0;
            nFree = otherData.numActiveUnmarkedMasters(cpp.vertex);
            if (nFree == 0) {
               HalfEdge he = cpp.face.firstHalfEdge();
               for (int i=0; i<3; i++) {
                  nFree = numActiveUnmarkedMasters(he.head);
                  he = he.getNext();
                  if (nFree > 0) {
                     break;
                  }
               }
            }
            if (nFree > 0) {
               points1.add(cpp);
               
               // mark masters
               otherData.markMasters(cpp.vertex, true);
               HalfEdge he = cpp.face.firstHalfEdge();
               for (int i=0; i<3; i++) {
                  markMasters(he.head, true);
                  he = he.getNext();
               }
            }
            cpp1 = bh1.poll();
         } // end checking if dealing with 0 or 1
      } // end while loop
      
      
      // unmark masters
      for (ContactPenetratingPoint cpp : points0) {
         markMasters(cpp.vertex, false);
         HalfEdge he = cpp.face.firstHalfEdge();
         for (int i=0; i<3; i++) {
            otherData.markMasters(he.head, false);
            he = he.getNext();
         }
      }
      
      for (ContactPenetratingPoint cpp : points1) {
         otherData.markMasters(cpp.vertex, false);
         HalfEdge he = cpp.face.firstHalfEdge();
         for (int i=0; i<3; i++) {
            markMasters(he.head, false);
            he = he.getNext();
         }
      }
      
      myPoints.clear();
      myPoints.addAll(points0);
      otherPoints.clear();
      otherPoints.addAll(points1);
   }
}
