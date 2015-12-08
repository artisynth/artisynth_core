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
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

import java.io.*;

public abstract class PointAttachment extends DynamicAttachment 
   implements CopyableComponent {

   protected Point myPoint;
   protected DynamicComponent[] myMasters;
   protected MatrixBlock[] myMasterBlocks;

   public Point getSlave() {
      return myPoint;
   }

   public Point getPoint() {
      return myPoint;
   }

   public int getSlaveSolveIndex() {
      if (myPoint != null) {
         return myPoint.getSolveIndex();
      }
      else {
         return -1;
      }
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
      for (int i=0; i<myMasterBlocks.length; i++) {
         myMasterBlocks[i] =
            MatrixBlockBase.alloc (myMasters[i].getVelStateSize(), 3);
      }
      updateMasterBlocks();      
   }

   protected void collectMasters (List<DynamicComponent> masters) {
      if (myPoint != null && myPoint.getPointFrame() != null) {
         masters.add (myPoint.getPointFrame());
      }
   }

   protected void printMasterBlocks (String fmtStr) {
      for (int i=0; i<myMasterBlocks.length; i++) {
         System.out.println ("M"+i+"\n");
         System.out.println (myMasterBlocks[i].toString(fmtStr));
      }
   }

   protected Vector3d computeMasterVelocity() {
      double[] vel = new double[3];

      for (int i=0; i<myMasterBlocks.length; i++) {
         DynamicComponent master = myMasters[i];
         double[] mvel = new double[master.getVelStateSize()];
         master.getVelState (mvel, 0);
         myMasterBlocks[i].mulTransposeAdd (vel, 0, mvel, 0);
      }
      return new Vector3d (vel[0], vel[1], vel[2]);
   }

   protected int updateMasterBlocks() {
      if (myMasters == null) {
         initializeMasters();
      }      
      if (myPoint != null) {
         Frame frame = myPoint.getPointFrame();
         if (frame != null) {
            MatrixBlock blk = myMasterBlocks[0];
            frame.computeLocalPointForceJacobian (
               blk, myPoint.getLocalPosition(),frame.getPose().R);
            blk.scale (-1);
            return 1;
         }
      }
      return 0;
   }
   
   /**
    * Indicates that this attachment is <i>flexible</i>. That means that
    * underlying body to which the point is attached is deformable.
    *
    * @return <code>true</code> if this attachment is flexible.
    */
   //public abstract boolean isFlexible();

   /**
    * Returns the current position of the attached point, in world coordinates.
    *
    * @param pos used to return current point position
    */
   public abstract void getCurrentPos (Vector3d pos);
   
   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      
      super.writeItems (pw, fmt, ancestor);
      if (myPoint != null) {
         pw.print ("point=");
         pw.println (ComponentUtils.getWritePathName (
                        ancestor, myPoint));
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();      
      if (scanAndStoreReference (rtok, "point", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "point")) {
         myPoint = postscanReference (tokens, Point.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   @Override
   public PointAttachment copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      PointAttachment a = (PointAttachment)super.copy (flags, copyMap);

      a.myMasters = null;
      a.myMasterBlocks = null;
      // EDIT: for FrameMarker.copy() can eventually lead here with copyMap=null, Sanchez (Nov 30, 2011)
      if (copyMap != null) {
//         if (copyMap.get (myPoint) == null) {
//            System.out.println ("not here: " + myPoint);
//         }
      }
      
      if (myPoint != null) {
         a.myPoint =
            (Point)ComponentUtils.maybeCopy (flags, copyMap, myPoint);
      }
      return a;
   }

   public void applyForces() {
      Frame pframe = myPoint.getPointFrame();
      if (pframe != null) {
         pframe.subPointForce (myPoint.getLocalPosition(), myPoint.myForce);
      }
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

   public int addTargetJacobian (SparseBlockMatrix j, int bi) {
      // Required by MotionTargetTerm
      return 0;
   }

}
