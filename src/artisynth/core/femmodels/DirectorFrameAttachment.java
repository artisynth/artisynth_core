package artisynth.core.femmodels;

import java.io.*;
import java.util.List;
import java.util.Deque;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

public class DirectorFrameAttachment extends DynamicAttachmentBase {

   protected FemNode3d myNode;
   protected Point3d myLoc = new Point3d();
   protected Vector3d myLocDir = new Vector3d();
   protected Frame myFrame;
   protected DynamicComponent[] myMasters;
   protected MatrixBlock[] myMasterBlocks;  
   protected MatrixBlock myMasterBlk;

   public DirectorFrameAttachment () {
   }

   public DirectorFrameAttachment (
      FemNode3d node, Frame frame) {
      myNode = node;
      setFrame (frame);
      updateAttachment(); // compute myLoc and myLocDir
   }

   protected void setFrame (Frame frame) {
      myFrame = frame;
      invalidateMasters();
   }

   public Frame getFrame() {
      return myFrame;
   }
   
   public BackNode3d getSlave() {
      return myNode.getBackNode();
   }

   public BackNode3d getBackNode() {
      return myNode.getBackNode();
   }

   public FemNode3d getNode() {
      return myNode;
   }

   void setNode (FemNode3d node) {
      myNode = node;
   }

   public Point3d getLoc() {
      return myLoc;
   }

   void setLoc (Point3d loc) {
      myLoc = new Point3d (loc);
   }

   public Vector3d getLocDir() {
      return myLocDir;
   }

   void setLocDir (Vector3d locDir) {
      myLocDir = new Vector3d (locDir);
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
      myMasters = new DynamicComponent[] { myFrame };
      if (myFrame instanceof DeformableBody) {
         DeformableBody def = (DeformableBody)myFrame;
         myMasterBlk = new MatrixNdBlock (def.getVelStateSize(), 3);
      }
      else {
         myMasterBlk = new Matrix6x3Block();
      }
      myMasterBlk.set (0, 0, 1.0);
      myMasterBlk.set (1, 1, 1.0);
      myMasterBlk.set (2, 2, 1.0);

      myMasterBlocks = new MatrixBlock[] { myMasterBlk };
   }

   public void addMassToMasters() {
      BackNode3d backNode = getBackNode();
      double m = backNode.getEffectiveMass();
      if (m != 0) {
         myFrame.addEffectivePointMass (m, myLoc);
      } 
      backNode.addEffectiveMass(-m);      
   }

   public void applyForces() {
      BackNode3d backNode = myNode.getBackNode();
      
      VectorNd fnode = new VectorNd(3);
      backNode.getForce (fnode.getBuffer(), 0);
      VectorNd fframe = new VectorNd (myFrame.getVelStateSize());
      myMasterBlk.mul (fframe, fnode);
      myFrame.addForce (fframe.getBuffer(), 0);
   }

   public void updateAttachment() {
      // compute myLoc
      myFrame.computePointLocation (myLoc, myNode.getPosition());
      // compute myLocDir
      RigidTransform3d TFL = new RigidTransform3d();
      RigidTransform3d TFW = new RigidTransform3d();
      TFL.p.set (myLoc);
      myFrame.computeFramePosition (TFW, TFL);
      myLocDir.inverseTransform (TFW.R, myNode.getDirector());
   }

   protected void updateMasterBlock (
      RigidTransform3d TFW, RigidTransform3d TFL0) {
      
      // do the frame - point component
      MatrixBlock blk = myMasterBlk;
      Vector3d locw = new Vector3d();
      locw.sub (TFW.p, myFrame.getPose().p);
      //locw.transform (TFW.R, myLoc);
      Vector3d dw = new Vector3d();
      dw.transform (TFW.R, myLocDir);
      locw.sub (dw);

      blk.set (3, 1, -locw.z);
      blk.set (3, 2, locw.y);
      blk.set (4, 0, locw.z);
      blk.set (4, 2, -locw.x);           
      blk.set (5, 0, -locw.y);
      blk.set (5, 1, locw.x);

      if (myFrame instanceof DeformableBody) {
         DeformableBody def = (DeformableBody)myFrame;
         int numc = def.numElasticCoords();
         MatrixNd Pi = new MatrixNd (6, numc);
         def.computeElasticJacobian (Pi, TFL0, /*worldCoords=*/true);

         Vector3d Pi_wcol = new Vector3d();
         Vector3d Pi_vcol = new Vector3d();
         Vector3d dprod = new Vector3d();
         for (int i=0; i<numc; i++) {
            Pi_vcol.set (Pi.get (0, i), Pi.get (1, i), Pi.get (2, i));
            Pi_wcol.set (Pi.get (3, i), Pi.get (4, i), Pi.get (5, i));

            dprod.cross (dw, Pi_wcol);

            blk.set (i+6, 0, Pi_vcol.x + dprod.x);
            blk.set (i+6, 1, Pi_vcol.y + dprod.y);
            blk.set (i+6, 2, Pi_vcol.z + dprod.z);
         }
      }      
   }

   public void updatePosStates() {
      if (myMasterBlocks == null) {
         initializeMasters();
      }
      RigidTransform3d TFL0 = new RigidTransform3d();
      RigidTransform3d TFW = new RigidTransform3d();
      TFL0.p.set (myLoc);
      myFrame.computeFramePosition (TFW, TFL0);
      Vector3d dirw = new Vector3d();
      dirw.transform (TFW.R, myLocDir);
      Point3d bw = new Point3d (TFW.p);
      bw.sub (dirw);
      myNode.setBackPosition (bw);

      updateMasterBlock (TFW, TFL0);
   }

   public void updateVelStates() {
      VectorNd velw = new VectorNd(3);
      VectorNd velf = new VectorNd(myFrame.getVelStateSize());
      myFrame.getVelState (velf.getBuffer(), 0);
      myMasterBlk.mulTranspose (velw, velf);
      myNode.getBackNode().setVelState (velw.getBuffer(), 0);
   }

   public boolean getDerivative (double[] buf, int idx) {
      // TODO - implement later
      buf[idx  ] = 0;
      buf[idx+1] = 0;
      buf[idx+2] = 0;
      return false; // change to true when implemented
   }

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

   public void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx) {
      myMasterBlocks[idx].mulAdd (ybuf, yoff, xbuf, xoff);
   }

   public void mulSubGTM (
      MatrixBlock D, MatrixBlock M, int idx) {
      D.mulAdd (myMasterBlocks[idx], M); 
   }

   // @Override
   // protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
   //    throws IOException {

   //    rtok.nextToken();
   //    if (scanAndStoreReference (rtok, "frame", tokens)) {
   //       return true;
   //    }
   //    else if (scanAndStoreReference (rtok, "node", tokens)) {
   //       return true;
   //    }
   //    else if (scanAttributeName (rtok, "loc")) {
   //       myLoc.scan (rtok);
   //       return true;
   //    }
   //    else if (scanAttributeName (rtok, "locDir")) {
   //       myLocDir.scan (rtok);
   //       return true;
   //    }
   //    rtok.pushBack();
   //    return super.scanItem (rtok, tokens);
   // }

   // @Override
   // protected boolean postscanItem (
   // Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

   //    if (postscanAttributeName (tokens, "node")) {
   //       myNode = postscanReference (tokens, FemNode3d.class, ancestor);
   //       return true;
   //    }
   //    else if (postscanAttributeName (tokens, "frame")) {
   //       setFrame (postscanReference (tokens, Frame.class, ancestor));
   //       return true;
   //    }
   //    return super.postscanItem (tokens, ancestor);
   // }
   
   // @Override
   // public void writeItems (
   //    PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
   //    throws IOException {

   //    super.writeItems (pw, fmt, ancestor);

   //    pw.print ("node=");
   //    pw.println (ComponentUtils.getWritePathName (ancestor, myNode));

   //    pw.print ("frame=");
   //    pw.println (ComponentUtils.getWritePathName (ancestor, myFrame));

   //    pw.print ("loc=");
   //    myLoc.write (pw, fmt, /*withBrackets=*/true);
   //    pw.println ("");

   //    pw.print ("locDir=");
   //    myLocDir.write (pw, fmt, /*withBrackets=*/true);
   //    pw.println ("");
   // }

   /**
    * Redefine to use the node instead of the backNode as hard
    * reference.
    */
   @Override 
   public void getHardReferences (List<ModelComponent> refs) {
      if (myNode != null) {
         refs.add (myNode);
      }
      for (DynamicComponent m : getMasters()) {
         refs.add (m);
      }
   } 
}
