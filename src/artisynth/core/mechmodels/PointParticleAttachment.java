/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

//import artisynth.core.mechmodels.DynamicMechComponent.Activity;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

import java.io.*;

public class PointParticleAttachment 
   extends PointAttachment implements ContactMaster {
   
   Particle myParticle;

   protected void collectMasters (List<DynamicComponent> masters) {
      super.collectMasters (masters);
      masters.add (myParticle);
   }
   
   public Particle getParticle() {
      return myParticle;
   }

//   public int numMasters() {
//      return 1;
//   }

   public void setParticle (Particle particle) {
      removeBackRefsIfConnected();
      myParticle = particle;
      invalidateMasters();
      addBackRefsIfConnected();
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }

   void setPoint (Point point) {
      myPoint = point;
   }

   public PointParticleAttachment() {
   }

   public PointParticleAttachment (Particle master, Point slave) {
      this();
      setParticle (master);
      setPoint (slave);
   }

   public void updatePosStates() {
      Point3d pntw = new Point3d();
      getCurrentPos (pntw);
      myPoint.setPosition (pntw);
   }

   public void getCurrentPos (Vector3d pos) {
      pos.set (myParticle.getPosition());
   }
   
   public void updateVelStates() {
      myPoint.setVelocity (myParticle.getVelocity());
   }

   public void applyForces() {
      super.applyForces();
      myParticle.addForce (myPoint.myForce);
   }

   public void mulSubGTM (MatrixBlock D, MatrixBlock M, int idx) {
      D.add (M);
   }

   public void mulSubMG (MatrixBlock D, MatrixBlock M, int idx) {
      D.add (M);
   }

   public MatrixBlock getGT (int idx) {
      Matrix3x3Block blk = new Matrix3x3Block();
      blk.setDiagonal (-1, -1, -1);
      return blk;
   }
   
   public void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx) {
      ybuf[yoff  ] += xbuf[xoff  ];
      ybuf[yoff+1] += xbuf[xoff+1];
      ybuf[yoff+2] += xbuf[xoff+2];
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "particle", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "particle")) {
         setParticle (postscanReference (
            tokens, Particle.class, ancestor));
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println ("particle=" + ComponentUtils.getWritePathName (
                     ancestor, myParticle));
   }

   public void updateAttachment() {
      // nothing to do here
   }

   public void addMassToMasters() {
      double m = myPoint.getEffectiveMass();
      if (m != 0) {
         myParticle.addEffectiveMass (m);
      }
      myPoint.addEffectiveMass(-m);
   }
   
   public boolean getDerivative (double[] buf, int idx) {
      buf[idx  ] = 0;
      buf[idx+1] = 0;
      buf[idx+2] = 0;
      return false;
   }

   public PointParticleAttachment copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      PointParticleAttachment a =
         (PointParticleAttachment)super.copy (flags, copyMap);

      //a.myMasters = null;      
      if (myParticle != null) {
         a.myParticle =
            (Particle)ComponentUtils.maybeCopy (flags, copyMap, myParticle);
      }
      return a;
   }

   /* --- begin ContactMaster implementation --- */

   public void add1DConstraintBlocks (
      SparseBlockMatrix GT, int bj, double scale, 
      ContactPoint cpnt, Vector3d dir) {
      if (myParticle != null) {
         int bi = myParticle.getSolveIndex();
         if (bi != -1) {
            Matrix3x1Block blk = (Matrix3x1Block)GT.getBlock (bi, bj);
            if (blk == null) {
               blk = new Matrix3x1Block();
               GT.addBlock (bi, bj, blk);
            }
            blk.scaledAdd (scale, dir);
         }
      }
   }

   public void add2DConstraintBlocks (
      SparseBlockMatrix GT, int bj, double scale, 
      ContactPoint cpnt, Vector3d dir0, Vector3d dir1) {
      if (myParticle != null) {
         int bi = myParticle.getSolveIndex();
         if (bi != -1) {
            Matrix3x2Block blk = (Matrix3x2Block)GT.getBlock (bi, bj);
            if (blk == null) {
               blk = new Matrix3x2Block();
               GT.addBlock (bi, bj, blk);
            }
            blk.m00 += scale*dir0.x;
            blk.m10 += scale*dir0.y;
            blk.m20 += scale*dir0.z;
            blk.m01 += scale*dir1.x;
            blk.m11 += scale*dir1.y;
            blk.m21 += scale*dir1.z;
         }
      }
   }
   
   public void addRelativeVelocity (
      Vector3d vel, double scale, ContactPoint cpnt) {
      if (myParticle != null) {
         vel.scaledAdd (scale, myParticle.getVelocity());
      }
   }

   public boolean isControllable() {
      if (myParticle != null) {
         return myParticle.isControllable();
      }
      else {
         return false;
      }
   }
   
   public int collectMasterComponents (
      HashSet<DynamicComponent> masters, boolean activeOnly) {
      if (myParticle != null && (!activeOnly || myParticle.isActive())) {
         if (masters.add (myParticle)) {
            return 1;
         }
      }
      return 0;
   }

   /* --- end ContactMaster implementation --- */
}
