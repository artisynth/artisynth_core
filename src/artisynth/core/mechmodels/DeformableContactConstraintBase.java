/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.PrintStream;

import maspack.geometry.Vertex3d;
import maspack.matrix.Matrix3x1Block;
import maspack.matrix.Matrix6x1Block;
import maspack.matrix.MatrixBlock;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.spatialmotion.Twist;
import maspack.util.DataBuffer;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;

public abstract class DeformableContactConstraintBase implements DeformableContactConstraint {

   protected CompInfo myHeadComp;
   protected CompInfo myTailComp;
   protected CompInfo myFreeComps;

   protected double myImpulse;
   protected boolean myActive;
   protected boolean myAdded;
   protected double myMu; // friction coefficient

   protected Vector3d myDdir; // friction direction
   protected double myVtMag; // original magnitude of tangential velocity
   protected Twist myTmpT = new Twist();

   protected static double DOUBLE_PREC = 2.2204460492503131e-16;
   protected boolean myCompsChanged = false;

   public static final int VERTEX_FACE = 1;
   public static final int VERTEX_BODY = 2;
   public static final int EDGE_EDGE = 3;
   public static final int EDGE_BODY = 4;
   public static final int BODY_FACE = 5;

   protected int mySolveIndex = -1;

   protected class ConstraintStorage {
      public int type;
      public double distance;
      public Vector3d normal;
      public Vertex3d[] vtxs1;
      public Point3d point1;
      public VectorNd wgts1;
      public Vertex3d[] vtxs2;
      public VectorNd wgts2;
      public Point3d point2;

      public ConstraintStorage() {
         type = -1;
         distance = 0;
         normal = new Vector3d();
         wgts1 = new VectorNd(0);
         wgts2 = new VectorNd(0);
         vtxs1 = new Vertex3d[0];
         vtxs2 = new Vertex3d[0];
         point1 = new Point3d();
         point2 = new Point3d();
      }

      public void set(int type, double dist, Vector3d normal, 
         Vertex3d[] vtxs1, Point3d point1, double[] wgts1,
         Vertex3d[] vtxs2, Point3d point2, double[] wgts2) {
         this.type = type;
         this.distance = dist;
         this.normal.set(normal);
         this.vtxs1 = vtxs1;
         this.wgts1.setSize(wgts1.length);
         this.wgts1.set(wgts1);
         this.vtxs2 = vtxs2;
         this.wgts2.setSize(wgts2.length);
         this.wgts2.set(wgts2);

      }

      public void skipAuxState(DataBuffer data) {
         data.zskip(3 + vtxs1.length + vtxs2.length);
         data.dskip(10 + wgts1.size() + wgts2.size());
      }

      // read/write
      public void getAuxState(DataBuffer data, CollisionData myData,
         CollisionData otherData) {

         data.zput(type);
         data.dput(distance);
         data.dput(normal.x);
         data.dput(normal.y);
         data.dput(normal.z);

         data.zput(vtxs1.length);
         for (int i=0; i<vtxs1.length; i++) {
            data.zput(vtxs1[i].getIndex());
            data.dput(wgts1.get(i));
         }
         data.dput(point1.x);
         data.dput(point1.y);
         data.dput(point1.z);

         data.zput(vtxs2.length);
         for (int i=0; i<vtxs2.length; i++) {
            data.zput(vtxs2[i].getIndex());
            data.dput(wgts2.get(i));
         }
         data.dput(point2.x);
         data.dput(point2.y);
         data.dput(point2.z);
      }

      public void setAuxState(
         DataBuffer data, CollisionData myData, CollisionData otherData) {

         type = data.zget();
         distance = data.dget();
         normal.x = data.dget();
         normal.y = data.dget();
         normal.z = data.dget();

         int nvtxs1 = data.zget();
         vtxs1 = new Vertex3d[nvtxs1];
         wgts1.setSize(nvtxs1);
         for (int i=0; i<nvtxs1; i++) {
            int vidx = data.zget();
            vtxs1[i] = myData.getMesh().getVertex(vidx);
            wgts1.set(i, data.dget());
         }
         point1.x = data.dget();
         point1.y = data.dget();
         point1.z = data.dget();

         int nvtxs2 = data.zget();
         vtxs2 = new Vertex3d[nvtxs2];
         wgts2.setSize(nvtxs2);
         for (int i=0; i<nvtxs2; i++) {
            int vidx = data.zget();
            vtxs2[i] = otherData.getMesh().getVertex(vidx);
            wgts2.set(i, data.dget());
         }
         point2.x = data.dget();
         point2.y = data.dget();
         point2.z = data.dget();

      }

      public void setFromInfo(CollisionData thisData, CollisionData otherData) {
         DeformableCollisionData defData = (DeformableCollisionData)thisData;
         //         switch (type) {
         //            case BODY_FACE: {
         //               RigidBodyCollisionData rbData = (RigidBodyCollisionData)otherData;
         //               setBodyFaceConstraint(thisData, vtxs1[0], vtxs1[1], vtxs1[2], point1, wgts1.get(0),
         //                  wgts1.get(1),  wgts1.get(2), rbData.getBody(), wgts2.get(0), point2);
         //               break;
         //            }
         //            case EDGE_BODY: {
         //               RigidBodyCollisionData rbData = (RigidBodyCollisionData)otherData;
         //               setEdgeBodyConstraint(defData, vtxs1[0], vtxs1[1], point1, wgts1.get(0), wgts1.get(1), 
         //                  rbData.getBody(), wgts2.sum(), point2);
         //               break;
         //            }
         //            case EDGE_EDGE: {
         //               DeformableCollisionData defData2 = (DeformableCollisionData)otherData;
         //               setEdgeEdgeConstraint(defData, vtxs1[0], vtxs1[1], wgts1.get(0), wgts1.get(1), 
         //                  point1, defData2, vtxs2[0], vtxs2[1], wgts2.get(0), wgts2.get(1), point2);
         //               break;
         //            }
         //            case VERTEX_BODY: {
         //               RigidBodyCollisionData rbData = (RigidBodyCollisionData)otherData;
         //               setVertexBodyConstraint(vtxs1[0], point1, wgts1.get(0), 
         //                  rbData.getBody(), wgts2.sum(), point2, defData);
         //               break;
         //            }
         //            case VERTEX_FACE: {
         //               DeformableCollisionData defData2 = (DeformableCollisionData)otherData;
         //               setVertexFaceConstraint(defData, vtxs1[0], point1, wgts1.get(0), defData2, 
         //                  vtxs2[0], vtxs2[1], vtxs2[2], point2, wgts2.get(0), wgts2.get(1), 
         //                  wgts2.get(2));
         //               break;
         //            }
         //            default:
         //               break;
         //         }
      }

   }

   protected ConstraintStorage myInfo;

   public DeformableContactConstraintBase () {
      myInfo = new ConstraintStorage();
      myActive = false;
      myAdded = false;
   }

   // index of this constraint within the bilateral constraint matrix GT
   public int getSolveIndex() {
      return mySolveIndex;
   }

   public void setSolveIndex(int idx) {
      mySolveIndex = idx;
   }

   /**
    * The interpenetration distance when the contact was first created.
    */
   public double getDistance() {
      return myInfo.distance;
   }

   public void setDistance(double d) {
      myInfo.distance = d;
   }

   public double getDerivative() {
      // TODO - implement this
      return 0;
   }

   /**
    * Adds matrix blocks for this constraint to a specified block column of the
    * transposed constraint matrix.
    * 
    * @param GT
    * transposed constraint matrix
    * @param bj
    * block column where blocks should be added
    */
   public void addConstraintBlocks(SparseBlockMatrix GT, int bj) {
      for (CompInfo info = myHeadComp; info != null; info = info.getNext()) {
         if (info.getSolveIndex() != -1) {
            MatrixBlock blk = info.getBlock();
            MatrixBlock nblk = blk.clone();
            nblk.setBlockRow(blk.getBlockRow());
            GT.addBlock(nblk.getBlockRow(), bj, nblk);
         }
      }
   }

   public void printConstraintInfo(PrintStream ps) {
      for (CompInfo info = myHeadComp; info != null; info = info.getNext()) {
         if (info.getSolveIndex() != -1) {
            MatrixBlock blk = info.getBlock();
            ps.println("row "+ blk.getBlockRow() + ":" + blk);
         }
      }
   }

   public int addFrictionConstraints(
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {

      computeFrictionDir();
      if (myVtMag > 0 && myHeadComp != null) {
         finfo[numf].mu = myMu;
         finfo[numf].contactIdx = mySolveIndex;
         finfo[numf].flags = FrictionInfo.BILATERAL;
         for (CompInfo info = myHeadComp; info != null; info = info.getNext()) {
            // finfo[numf].offset0 += info.addFrictionBlock (DT, myDdir, numf);
            info.addFrictionBlock(DT, myDdir, numf);
         }
         numf++;
      }
      return numf;
   }

   /**
    * Sets the friction coefficient for this constraint.
    */
   public void setFriction(double mu) {
      myMu = mu;
   }

   /**
    * Returns the friction coefficent for this constraint.
    */
   public double getFriction() {
      return myMu;
   }

   /**
    * Returns the normal for this constraint.
    */
   public Vector3d getNormal() {
      return myInfo.normal;
   }

   public abstract int hashCode();

   public abstract boolean equals(Object obj);

   /**
    * Returns the impulse associated with this constraint.
    */
   public double getImpulse() {
      return myImpulse;
   }

   /**
    * Sets the impulse associated with this constraint. In particular, this will
    * be done after velocity solves. The resulting impulse is then used to
    * compute friction and determine if the constraint should be broken.
    */
   public void setImpulse(double lam) {
      myImpulse = lam;
   }

   /**
    * Returns true if this constraint is marked as being active.
    */
   public boolean isActive() {
      return myActive;
   }

   /**
    * Marks this constraint as being inactive.
    */
   public void setActive(boolean active) {
      myActive = active;
   }
   
   /**
    * Returns true if the constraint is marked as being 
    * already added to the system
    */
   public boolean isAdded() {
      return myAdded;
   }
   
   /**
    * Marks this constraint as being added to the system
    */
   public void setAdded(boolean added) {
      myAdded = added;
   }

   /**
    * Begin the process of setting this constraint. After this method call,
    * components can be added using the addPoint() and addFrame() methods. When
    * all components have been added, endSet() should be called.
    */
   public void beginSet() {
      myCompsChanged = false;
      myFreeComps = myHeadComp;
      myHeadComp = null;
      myTailComp = null;
   }

   /**
    * Concludes the process of setting this constraint.
    */
   public void endSet() {
   }

   /**
    * Returns true if the component structure of this constraint has changed.
    * This will be true if, since the last call to clearComponents(), new
    * component infos have been added, or if any infos are left on the free
    * list.
    */
   public boolean componentsChanged() {
      if (myFreeComps != null) {
         myCompsChanged = true;
      }
      return myCompsChanged;
   }

   /**
    * Finds or allocates a CompInfo for a specific component. If no CompInfo is
    * found on the free list, a new one is allocated.
    */
   protected CompInfo findCompInfo(DynamicMechComponent comp) {
      CompInfo prev = null;
      for (CompInfo info = myFreeComps; info != null; info = info.getNext()) {
         if (info.getComp() == comp) {
            if (prev != null) {
               prev.setNext(info.getNext());
            }
            else {
               myFreeComps = info.getNext();
            }
            return info;
         }
         prev = info;
      }
      return null;
   }

   protected CompInfo findAddedCompInfo(DynamicMechComponent comp) {
      for (CompInfo info = myHeadComp; info != null; info = info.getNext()) {
         if (info.getComp() == comp) {
            return info;
         }
      }
      return null;
   }

   /**
    * Returns true if at least one of the components associated with this
    * constraint is controllable.
    */
   public boolean isControllable() {
      for (CompInfo info = myHeadComp; info != null; info = info.getNext()) {
         if (info.getComp().isControllable()) {
            return true;
         }
      }
      return false;
   }


   // Friction direction is set to the negative tangential velocity direction.
   private double computeFrictionDir() {

      if (myMu == 0) {
         return 0;
      }
      if (myDdir == null) {
         myDdir = new Vector3d();
      }

      // compute relative velocity into myDdir
      myDdir.setZero();
      for (CompInfo info = myHeadComp; info != null; info = info.getNext()) {
         info.addRelativeVelocity(myDdir, myTmpT);
      }
      // turn this into tangential velocity
      myDdir.scaledAdd(-myInfo.normal.dot(myDdir), myInfo.normal);
      myVtMag = myDdir.norm();

      if (myVtMag > DOUBLE_PREC) {
         myDdir.scale(-1 / myVtMag);
         return myVtMag;
      }
      else {
         return 0;
      }
   }

   public void updateFriction() {
      computeFrictionDir();
   }

   protected void addCompInfo(CompInfo info) {
      if (myTailComp != null) {
         myTailComp.setNext(info);
      }
      else {
         myHeadComp = info;
      }
      info.setNext(null);
      myTailComp = info;
      // info.next = myHeadComp;
      // myHeadComp = info;
   }

   /**
    * Adds a point as a component for this constraint.
    * 
    * @param vtx (optional)
    * the vertex associated with this point (if FemMeshVertex)
    * @param pnt
    * point to add
    * @param weight
    * weighting factor for the point.  Note: if the weight is positive, it is assumed
    * to belong to the 'first' component in the collision.  If negative, it is
    * assumed to belong to the second.
    */
   public void addPoint(Point pnt, double weight) {

      // first try to find an already-added component
      // in case we need to adjust the weight
      CompInfo info = findAddedCompInfo(pnt);
      if (info == null) {

         // assign new info if required
         info = findCompInfo(pnt);
         if (info == null) {
            PointInfo pinfo = new PointInfo(pnt);

            info = pinfo;
            myCompsChanged = true;
         }
         ((PointInfo)info).setConstraint(myInfo.normal, weight);
         addCompInfo(info);
      } else {

         // otherwise, simply adjust weight
         PointInfo pinfo = (PointInfo)info;
         pinfo.setConstraint(myInfo.normal, pinfo.getWeight() + weight);   
      }           

   }

   /**
    * Adds a frame as a component for this constraint.
    * @param frame
    * Frame to add
    * @param loc
    * location of the point in frame coordinates
    */
   public void addFrame(Frame frame, double weight, Point3d loc) {
      
   // first try to find an already-added component
      // in case we need to adjust the weight
      CompInfo info = findAddedCompInfo(frame);
      if (info == null) {

         // assign new info if required
         info = findCompInfo(frame);
         if (info == null) {
            FrameInfo finfo = new FrameInfo(frame);

            info = finfo;
            myCompsChanged = true;
         }
         ((FrameInfo)info).setConstraint(myInfo.normal, weight, loc);
         addCompInfo(info);
      } else {
         // otherwise, simply adjust weight
         FrameInfo finfo = (FrameInfo)info;
         finfo.setConstraint(myInfo.normal, finfo.getWeight() + weight, loc);   
      }           

   }

   @Override
   public int numPoints() {
      int np = 0;
      for (CompInfo info = myHeadComp; info != null; info = info.getNext()) {
         if (info instanceof PointInfo){
            np++;
         }
      }
      return np;
   }

   @Override
   public int numFrames() {
      int nf = 0;
      for (CompInfo info = myHeadComp; info != null; info = info.getNext()) {
         if (info instanceof FrameInfo){
            nf++;
         }
      }
      return nf;
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
      Vertex3d v0, Vertex3d v1, Point3d loc1, double w0, double w1,
      DeformableCollisionData thisData,
      Vertex3d v2, Vertex3d v3, Point3d loc2, double w2, double w3,
      DeformableCollisionData otherData) {

      setFriction(mu);
      myInfo.set(EDGE_EDGE, 0, nrml, new Vertex3d[]{v0, v1}, loc1, 
         new double[]{w0, w1}, 
         new Vertex3d[]{v2, v3}, loc2, new double[]{w2, w3});
      
      setEdgeEdgeConstraint(thisData, v0, v1, w0, w1, loc1, otherData,
         v2, v3, w2, w3, loc2);
   }
   
   private void setEdgeEdgeConstraint(DeformableCollisionData thisData,
      Vertex3d v0, Vertex3d v1, double w0, double w1, Point3d loc0,
      DeformableCollisionData otherData, Vertex3d v2, Vertex3d v3,
      double w2, double w3, Point3d loc1) {
      
      beginSet();
      thisData.addConstraintInfo(v0, loc0, w0, this);
      thisData.addConstraintInfo(v1, loc0, w1, this);
      otherData.addConstraintInfo(v2, loc1, -w2, this);
      otherData.addConstraintInfo(v3, loc1, -w3, this);
      endSet();
   }

   /**
    * Set the constraint for a deformable-deformable node-face contact.
    */
   public void setVertexFace(
      Vector3d nrml, double mu, Vertex3d v0, Point3d loc1, double w0,
      DeformableCollisionData thisData,
      Vertex3d v1, Vertex3d v2, Vertex3d v3, Point3d loc2, 
      double w1, double w2, double w3, DeformableCollisionData otherData) {

      setFriction(mu);
      myInfo.set(VERTEX_FACE, 0, nrml, new Vertex3d[]{v0}, loc1, 
         new double[] {w0}, new Vertex3d[]{v1, v2, v3}, loc2, 
         new double[]{w1, w2, w3});

      setVertexFaceConstraint(thisData, v0, loc1, w0, otherData, v1, v2, v3,
         loc2, w1, w2, w3 );
   }
   
   private void setVertexFaceConstraint(CollisionData thisData, 
      Vertex3d v0, Point3d loc0, double w0,
      CollisionData otherData, Vertex3d v1, Vertex3d v2, Vertex3d v3,
      Point3d loc1, double w1, double w2, double w3) {
      
      beginSet();
      thisData.addConstraintInfo(v0, loc0, w0, this);
      otherData.addConstraintInfo(v1, loc0, -w1, this);
      otherData.addConstraintInfo(v2, loc1, -w2, this);
      otherData.addConstraintInfo(v3, loc1, -w3, this);
      endSet();
   }

   /**
    * Sets the constraint for rigidBody-deformable node-face contact
    */
   public void setBodyFace(Vector3d nrml, double mu, 
      Vertex3d v0, Vertex3d v1, Vertex3d v2, Point3d loc1,
      double w0, double w1, double w2, DeformableCollisionData thisData,
      Vertex3d rbvtx, RigidBody body, Point3d loc2, double wrb) {

      setFriction(mu);
      myInfo.set(BODY_FACE, 0, nrml, new Vertex3d[]{v0, v1, v2}, 
         loc1, new double[]{w0, w1, w2}, new Vertex3d[] {rbvtx}, loc2, new double[]{wrb});
      
      setBodyFaceConstraint(thisData, v0, v1, v2, loc1, w0, w1, w2, body, wrb, loc2);
      
   }

   private void setBodyFaceConstraint(CollisionData thisData, Vertex3d v0, Vertex3d v1,
      Vertex3d v2, Point3d loc0, double w0, double w1, double w2, RigidBody body,
      double wrb, Point3d loc1) {
      beginSet();
      thisData.addConstraintInfo (v0, loc0, w0, this);
      thisData.addConstraintInfo (v1, loc0, w1, this);
      thisData.addConstraintInfo (v2, loc0, w2, this);
      addFrame(body, -wrb, loc1);
      endSet();
   }
   
   /**
    * Set the constraint for a deformable-rigidBody node-face contact.
    */
   public void setVertexBody(
      Vector3d nrml, double mu, Vertex3d v0, Point3d loc1, double w0, 
      DeformableCollisionData thisData,
      RigidBody body, Point3d loc2, 
      Vertex3d vb0, Vertex3d vb1, Vertex3d vb2, 
      double wb0, double wb1, double wb2) {

      setFriction(mu);
      myInfo.set(VERTEX_BODY, 0, nrml, new Vertex3d[]{v0}, loc1, new double[]{w0}, 
         new Vertex3d[]{vb0, vb1, vb2}, loc2, new double[]{wb0, wb1, wb2});
      
      double w = wb0+(wb1+wb2);  // ordered this way for numerical reasons
      setVertexBodyConstraint(v0, loc1, w0, body, w, loc2, thisData);
   }
   
   private void setVertexBodyConstraint(Vertex3d v0, Point3d loc0, double w0, RigidBody rb,
      double wrb, Point3d loc1, DeformableCollisionData thisData) {
      beginSet();
      thisData.addConstraintInfo (v0, loc0, w0, this);
      addFrame(rb, -wrb, loc1);
      endSet();
   }

   /**
    * Set the constraint for a deformable-rigidBody edge-edge contact.
    */
   public void setEdgeBody(
      Vector3d nrml, double mu, Vertex3d v0, Vertex3d v1, Point3d loc1,
      double w0, double w1, DeformableCollisionData thisData,
      RigidBody body, Point3d loc2, Vertex3d vb0, Vertex3d vb1, double wb0, double wb1) {

      setFriction(mu);
      myInfo.set(EDGE_BODY, 0, nrml, new Vertex3d[]{v0, v1}, loc1, new double[] {w0, w1},
         new Vertex3d[]{vb0, vb1}, loc2, new double[]{wb0, wb1});
      
      setEdgeBodyConstraint(thisData, v0, v1, loc1, w0, w1, body, wb0+wb1, loc2);
   }
   
   private void setEdgeBodyConstraint(DeformableCollisionData thisData,
      Vertex3d v0, Vertex3d v1, Point3d loc0, double w0, double w1,
      RigidBody body, double wrb, Point3d loc1) {
      beginSet();
      thisData.addConstraintInfo (v0, loc0, w0, this);
      thisData.addConstraintInfo (v1, loc0, w1, this);
      addFrame(body, -wrb, loc1);
      endSet();
   }

   public void skipAuxState (DataBuffer data) {
      myInfo.skipAuxState(data);
   }

   public void getAuxState(DataBuffer data, CollisionData myData,
      CollisionData otherData) {
      myInfo.getAuxState(data, myData, otherData);
   }

   public void setAuxState(
      DataBuffer data, CollisionData myData, CollisionData otherData) {
      myActive = false;
      myInfo.setAuxState(data, myData, otherData);
      myInfo.setFromInfo(myData, otherData);
   }


   /**
    * Provides matrix block information and computational function for a single
    * dynamic component associated with a contact constraint.
    */
   public abstract class CompInfo {
      private CompInfo next;

      /**
       * Returns the dynamic component
       */
      public abstract DynamicMechComponent getComp();

      /**
       * Returns the solve index of the dynamic component
       */
      public abstract int getSolveIndex();

      /**
       * Returns true if the dynamic component is parametric
       */
      public abstract boolean isParametric();

      public abstract double computeDirectionalVelocity(
         Vector3d dir, Twist tmpT);

      // /**
      // * Computes the component's constraint value for its parametric
      // * velocity.
      // */
      // public abstract double parametricConstraintValue (Twist tmpT);

      public abstract void addFrictionBlock(
         SparseBlockMatrix DT, Vector3d dir, int bj);

      /**
       * Uses the component's current velocity to compute its contribution to
       * the current relative velocity.
       */
      public abstract void addRelativeVelocity(Vector3d vrel, Twist tmpT);

      /**
       * Returns the matrix block associated with this constraint. This block
       * will be added to the transposed constraint matrix GT.
       */
      public abstract MatrixBlock getBlock();

      public CompInfo getNext() {
         return next;
      }

      public void setNext(CompInfo next) {
         this.next = next;
      }

   }

   public class PointInfo extends CompInfo {
      private Point myPoint;
      private double myWeight;
      Matrix3x1Block myBlk;

      public Matrix3x1Block getBlock() {
         return myBlk;
      }

      public DynamicMechComponent getComp() {
         return getPoint();
      }

      public int getSolveIndex() {
         return getPoint().getSolveIndex();
      }

      public boolean isParametric() {
         return getPoint().isParametric();
      }

      public PointInfo (Point pnt) {
         setPoint(pnt);
         myBlk = new Matrix3x1Block();
         myBlk.setBlockRow(getPoint().getSolveIndex());
      }

      public void setConstraint(Vector3d nrml, double weight) {
         myBlk.scale(weight, nrml);
         setWeight(weight);
      }

      public double getWeight() {
         return myWeight;
      }

      public double computeDirectionalVelocity(Vector3d dir, Twist tmpT) {
         return getWeight() * dir.dot(getPoint().getVelocity());
      }

      // public double parametricConstraintValue (Twist tmpT) {
      // Vector3d vel = tmpT.v;
      // if (myPoint.computeParametricVelocity (vel)) {
      // return vel.x * myBlk.m00 + vel.y * myBlk.m10 + vel.z * myBlk.m20;
      // }
      // else {
      // return 0;
      // }
      // }

      public void addFrictionBlock(
         SparseBlockMatrix DT, Vector3d dir, int bj) {

         int bi = getPoint().getSolveIndex();
         // double rhs = 0;
         if (bi != -1) {
            Matrix3x1Block blk = new Matrix3x1Block();
            blk.scale(getWeight(), dir);
            DT.addBlock(bi, bj, blk);
         }
         // TODO: change isParametric() to bi == -1
         // if (isParametric()) {
         // rhs = -myWeight*dir.dot (myPoint.getVelocity());
         // }
         // return rhs;
      }

      public void addRelativeVelocity(Vector3d vrel, Twist tmpT) {
         vrel.scaledAdd(getWeight(), getPoint().getVelocity());
      }

      public Point getPoint() {
         return myPoint;
      }

      public void setPoint(Point myPoint) {
         this.myPoint = myPoint;
      }

      public void setWeight(double myWeight) {
         this.myWeight = myWeight;
      }
   }

   public class FrameInfo extends CompInfo {
      private Frame myFrame;
      private double myWeight;
      private Point3d myLoc;
      Matrix6x1Block myBlk;

      private void updateBlock() {
         getFrame().computeAppliedWrench(myBlk, myInfo.normal, getLoc());
         if (Frame.dynamicVelInWorldCoords) {
            myBlk.transform(getFrame().getPose().R);
         }
         myBlk.scale(getWeight());
      }

      public Matrix6x1Block getBlock() {
         updateBlock();
         return myBlk;
      }

      public DynamicMechComponent getComp() {
         return getFrame();
      }

      public int getSolveIndex() {
         return getFrame().getSolveIndex();
      }

      public boolean isParametric() {
         return getFrame().isParametric();
      }

      public FrameInfo (Frame frame) {
         setFrame(frame);
         setLoc(new Point3d());
         myBlk = new Matrix6x1Block();
         myBlk.setBlockRow(frame.getSolveIndex());
      }

      public void setConstraint(
         Vector3d nrml, double weight, Point3d loc) {
         setWeight(weight);
         getLoc().set(loc);
         // updateBlock();
      }

      public double computeDirectionalVelocity(Vector3d dir, Twist tmpT) {
         getFrame().getBodyVelocity(tmpT);
         // compute contact velocity in tmpT.v
         tmpT.v.crossAdd(tmpT.w, getLoc(), tmpT.v);
         tmpT.v.transform(getFrame().getPose().R);
         return getWeight() * dir.dot(tmpT.v);
      }

      // public double parametricConstraintValue (Twist tmpT) {
      // if (myFrame.computeParametricVelocity (tmpT)) {
      // updateBlock();
      // return (tmpT.v.x*myBlk.m00 + tmpT.v.y*myBlk.m10 + tmpT.v.z*myBlk.m20 +
      // tmpT.w.x*myBlk.m30 + tmpT.w.y*myBlk.m40 + tmpT.w.z*myBlk.m50);
      // }
      // else {
      // return 0;
      // }
      // }

      public void addFrictionBlock(
         SparseBlockMatrix DT, Vector3d dir, int bj) {

         Matrix6x1Block blk = new Matrix6x1Block();
         getFrame().computeAppliedWrench(blk, dir, getLoc());
         // TODO: change isParametric() to bi == -1
         // double rhs = 0;
         // if (isParametric()) {
         // Twist bodyVel = new Twist();
         // myFrame.getBodyVelocity (bodyVel);
         // rhs = -myWeight*blk.dot (bodyVel.v, bodyVel.w);
         // }
         if (Frame.dynamicVelInWorldCoords) {
            blk.transform(getFrame().getPose().R);
         }
         int bi = getFrame().getSolveIndex();
         if (bi != -1) {
            blk.scale(getWeight());
            DT.addBlock(bi, bj, blk);
         }
         // return rhs;
      }

      public void addRelativeVelocity(Vector3d vrel, Twist tmpT) {
         getFrame().computePointVelocity(tmpT.v, getLoc());
         vrel.scaledAdd(getWeight(), tmpT.v);
      }

      public Frame getFrame() {
         return myFrame;
      }

      public void setFrame(Frame myFrame) {
         this.myFrame = myFrame;
      }

      public double getWeight() {
         return myWeight;
      }

      public void setWeight(double myWeight) {
         this.myWeight = myWeight;
      }

      public Point3d getLoc() {
         return myLoc;
      }

      public void setLoc(Point3d myLoc) {
         this.myLoc = myLoc;
      }
   }

}
