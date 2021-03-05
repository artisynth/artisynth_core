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

public abstract class PointAttachment extends DynamicAttachmentBase 
   implements CopyableComponent {

   protected Point myPoint;
   protected DynamicComponent[] myMasters;

   protected PointAttachment() {
      myPoint = null;
   }
   
   protected PointAttachment(Point pnt) {
      myPoint = pnt;
   }
   
   public Point getSlave() {
      return myPoint;
   }

   public Point getPoint() {
      return myPoint;
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
   }

   protected void collectMasters (List<DynamicComponent> masters) {
   }

   protected MatrixBlock[] allocateMasterBlocks() {
      DynamicComponent[] masters = getMasters();
      MatrixBlock[] blks = new MatrixBlock[masters.length];
      for (int i=0; i<blks.length; i++) {
         blks[i] = MatrixBlockBase.alloc (masters[i].getVelStateSize(), 3);
      }
      return blks;
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

   /**
    * If necessary, notify this attachment's master components that the
    * position state of one or more of them has changed. This is only used in
    * situations where the position is changed outside of a normal simulation
    * computation, such as in {@link #createNumericMasterBlocks}. Attachments
    * only need to implement this method when setting the position state of one
    * or more masters, in isolation, incurs the need for other updates before a
    * subsequwent call to {@link #updatePosStates()} will perform correctly.
    */
   public void notifyMastersOfPositionChange() {
   }

   /**
    * Checks a set of master blocks to see if they are consistent with those
    * computed numerically. Use for debugging and testing only.
    */
   public boolean checkMasterBlocks (MatrixBlock[] blks, double eps) {
      if (blks.length != numMasters()) {
         throw new IllegalArgumentException (
            "number of blks "+blks.length+" != number of masters "+numMasters());
      }
      MatrixBlock[] chkBlks = createNumericMasterBlocks();
      boolean passed = true;
      for (int idx=0; idx<blks.length; idx++) {
         MatrixNd Err = new MatrixNd(blks[idx]);
         Err.sub (new MatrixNd(chkBlks[idx]));
         double err = Err.frobeniusNorm()/chkBlks[idx].frobeniusNorm();
         if (err > eps) {
            System.out.println (
               "Block "+idx+" for master "+getMasters()[idx]+":");
            System.out.println (blks[idx].toString ("%12.6f"));
            System.out.println ("numeric value:");
            System.out.println (chkBlks[idx].toString ("%12.6f"));
            System.out.println ("error " + err + ":");
            System.out.println (Err.toString ("%12.6f"));
            passed = false;
         }
      }
      return passed;
   }

   /**
    * Creates a set of master blocks for this attachment numerically.  Used for
    * debugging and testing only.
    */
   protected MatrixBlock[] createNumericMasterBlocks() {
      MatrixBlock[] blks = allocateMasterBlocks();
      int idx = 0;
      double h = 1e-8; // step size for numeric differentiation
      Vector3d pntPos0 = new Vector3d(myPoint.getPosition());
      for (DynamicComponent c : getMasters()) {
         // compute master block for each master
         int psize = c.getPosStateSize();
         int vsize = c.getVelStateSize();
         double[] pos0 = new double[psize];
         double[] pos = new double[psize];
         c.getPosState (pos0, 0); // save initial position of idx-th master

         // numeric velocities imparted by each master DOF form the rows of the
         // idx-th master block:
         for (int i=0; i<vsize; i++) {
            double[] unitVel = new double[vsize];
            unitVel[i] = 1.0;
            for (int k=0; k<psize; k++) {
               pos[k] = pos0[k];
            }
            // adiust master position along the i-th DOF:
            c.addPosImpulse (pos, 0, h, unitVel, 0);
            c.setPosState (pos, 0);
            notifyMastersOfPositionChange();
            updatePosStates();            
            // compute resulting numeric point velocity:
            Vector3d numVel = new Vector3d(myPoint.getPosition());
            numVel.sub (pntPos0);
            numVel.scale (1/h);
            // use this velocity to the the i-th block colum
            blks[idx].setRow (i, numVel);
            c.setPosState (pos0, 0);
         }
         idx++;
      }
      notifyMastersOfPositionChange();
      updatePosStates(); 
      return blks;
   }
  

}
