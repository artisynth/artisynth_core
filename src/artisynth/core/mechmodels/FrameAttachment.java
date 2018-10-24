/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

import java.io.*;

public abstract class FrameAttachment extends DynamicAttachmentBase 
   implements CopyableComponent, ScalableUnits {

   protected Frame myFrame;
   protected DynamicComponent[] myMasters;
   protected MatrixBlock[] myMasterBlocks;

   public Frame getSlave() {
      return myFrame;
   }

   public Frame getFrame() {
      return myFrame;
   }

   /**
    * {@inheritDoc}
    */
   public DynamicComponent[] getMasters() {
      if (myMasters == null) {
         initializeMasters();
      }
      return myMasters;
   }

   public MatrixBlock[] getMasterBlocks() {
      return myMasterBlocks;
   }

   /**
    * {@inheritDoc}
    */
   public int numMasters() {
      if (myMasters == null) {
         initializeMasters();
      }
      return myMasters.length;
   }

   public void invalidateMasters() {
      myMasters = null;
   }

   protected void initializeMasters() {
      ArrayList<DynamicComponent> masters = new ArrayList<DynamicComponent>();
      collectMasters (masters);
      myMasters = masters.toArray (new DynamicComponent[0]);
      myMasterBlocks = new MatrixBlock[myMasters.length];
      if (myMasters.length > 0) {
         for (int i=0; i<myMasterBlocks.length; i++) {
            myMasterBlocks[i] =
               MatrixBlockBase.alloc (myMasters[i].getVelStateSize(), 6);
         }
      }
   }

   protected void collectMasters (List<DynamicComponent> masters) {
      // must be overriden by subclasses
   }

   protected int updateMasterBlocks() {
      // must be overriden if needed by subclasses
      if (myMasters == null) {
         initializeMasters();
      }      
      return 0;
   }
   
   /**
    * Indicates that this attachment is <i>flexible</i>. That means that
    * underlying body to which the frame is connected is deformable.
    *
    * @return <code>true</code> if this attachment is flexible.
    */
   public abstract boolean isFlexible();

   /**
    * Returns the current pose of the attached frame, in world coordinates.
    *
    * @param TFW used to return current frame pose.
    */
   public abstract void getCurrentTFW (RigidTransform3d TFW);
   
   /**
    * Returns the current undeformed pose of the attached frame, in world
    * coordinates. If the underlying body to which the frame is attached is not
    * flexible, then this method should return the same value as {@link
    * #getCurrentTFW getCurrentTFW()}.
    *
    * @param TFW used to return current undeformed frame pose.
    * @see #isFlexible
    */
   public abstract void getUndeformedTFW (RigidTransform3d TFW);
   
   /**
    * Sets the current pose of the attached frame, in world coordinates.  This
    * will update the internal parameters of the attachment to be consistent
    * with the current valued master coordinates.
    *
    * @param TFW new pose for the attached frame, in world coordinates
    * @return <code>true</code> if the underlying master 
    * components have changed
    */
   public abstract boolean setCurrentTFW (RigidTransform3d TFW);

   /**
    * Returns the current velocity of the attached frame, in frame coordinates.
    * Also optionally returns the velocity derivative term, defined by
    * <pre>
    * \dot J velm
    * </pre>
    * where <code>J</code> is the matrix that maps master velocities
    * to the attached frame velocity (in frame coordinates), and <code>velm</code>
    * are the master velocities.
    *
    * @param vel used to return current frame velocity
    * @param dvel if not <code>null</code>, returns the velocity derivative term
    */
   public abstract void getCurrentVel (Twist vel, Twist dvel);

   /**
    * Returns the average mass of the master components as seen by the attached
    * frame. This is used by BodyConnector for automatically computing
    * critical damping for joint compliance.
    */
   public abstract double getAverageMasterMass();

   /**
    * Returns the average rotational inertia of the master components as seen
    * by the attached frame. This is used by BodyConnector for
    * automatically computing critical damping for joint compliance.
    */
   public abstract double getAverageMasterInertia();

   public void writeItems (
       PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      
      super.writeItems (pw, fmt, ancestor);
      if (myFrame != null) {
         pw.print ("frame=");
         pw.println (ComponentUtils.getWritePathName (
                        ancestor, myFrame));
      }
   }

  protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();      
      if (scanAndStoreReference (rtok, "frame", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "frame")) {
         myFrame = postscanReference (tokens, Frame.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   @Override
   public FrameAttachment copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FrameAttachment a = (FrameAttachment)super.copy (flags, copyMap);

      a.myMasters = null;
      a.myMasterBlocks = null;
      
      if (myFrame != null) {
         a.myFrame =
            (Frame)ComponentUtils.maybeCopy (flags, copyMap, myFrame);
      }
      return a;
   }

   public void applyForces() {
      // must be overriden by subclasses
   }

   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return false;
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public void mulSubGTM (
      MatrixBlock D, MatrixBlock M, int idx) {

      D.mulAdd (myMasterBlocks[idx], M); 
   }

   /**
    * {@inheritDoc}
    */
   public void mulSubMG (
      MatrixBlock D, MatrixBlock M, int idx) {

      MatrixBlock G = myMasterBlocks[idx].createTranspose();
      D.mulAdd (M, G); 
   }

   public MatrixBlock getGT (int idx) {
      MatrixBlock blk = myMasterBlocks[idx].clone();
      blk.negate();
      return blk;
   }
   
   /**
    * {@inheritDoc}
    */
   public void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx) {

      myMasterBlocks[idx].mulAdd (ybuf, yoff, xbuf, xoff);
   }


   /**
    * Nothing to do for scale mass.
    */
   public void scaleMass (double m) {
   }

   /**
    * Sets the top 6x6 sub-matrix of a MatrixBlock to the transpose of the
    * velocity Jacobian relating the velocity of frame A to frame F.
    * Unless RWB is specified, the velocities of both A and F are
    * assumed to be aligned with world coordinates.
    *
    * @param GT 6 x 6 matrix block to be set
    * @param pAFw vector from the origin of F to the origin of A, in world
    * coordinates
    * @param RBW If not <code>null</code>, should be a rotation from some frame
    * B to world. This will rotate the output velocity of the Jacobian so that
    * it is the coordinates of B.
    */
   protected void computeFrameFrameJacobian (
      MatrixBlock GT, Vector3d pAFw, RotationMatrix3d RBW) {

      double x = pAFw.x;
      double y = pAFw.y;
      double z = pAFw.z;     

      Matrix3d PX = new Matrix3d();
      if (RBW == null) {
         RBW = RotationMatrix3d.IDENTITY;
         PX.setSkewSymmetric (pAFw);
      }
      else {
         PX.crossProduct (pAFw, RBW);
      }

      if (GT instanceof Matrix6dBlock) {
         Matrix6dBlock blk = (Matrix6dBlock)GT;
         blk.setSubMatrix00 (RBW);
         blk.setSubMatrix03 (Matrix3d.ZERO);
         blk.setSubMatrix30 (PX);
         blk.setSubMatrix33 (RBW);
      }
      else if (GT instanceof MatrixNdBlock) {
         if (GT.colSize() != 6 || GT.rowSize() < 6) {
            throw new IllegalArgumentException (
               "GT must have size m x 6 with m >= 6");
         }
         MatrixNdBlock blk = (MatrixNdBlock)GT;
         double[] buf = blk.getBuffer();
         // zero the 6 x 6 sub matrix
         for (int i=0; i<36; i++) {
            buf[i] = 0;
         }
         blk.setSubMatrix (0, 0, RBW);
         blk.setSubMatrix (0, 3, Matrix3d.ZERO);
         blk.setSubMatrix (3, 0, PX);
         blk.setSubMatrix (3, 3, RBW);
      }
      else {
         throw new IllegalArgumentException (
            "GT is not an instance of Matrix6x6Block or MatrixNdBlock; is " +
            GT.getClass());
      }
   }

   public void transformGeometry (
      AffineTransform3dBase X, RigidTransform3d TFW) {

      PolarDecomposition3d pd = new PolarDecomposition3d();
      pd.factor (X.getMatrix());

      if (TFW == null) {
         TFW = new RigidTransform3d();
         getCurrentTFW (TFW);
      }
      RigidTransform3d TFWnew = new RigidTransform3d();
      TFWnew.p.mulAdd (X.getMatrix(), TFW.p, X.getOffset());
      TFWnew.R.mul (pd.getR(), TFW.R);
      setCurrentTFW (TFWnew);
   }

}
