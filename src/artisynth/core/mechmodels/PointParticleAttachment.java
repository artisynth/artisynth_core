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

public class PointParticleAttachment extends PointAttachment {
   
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
   
//   public void addScaledExternalForce(Point3d pnt, double s, Vector3d f) {
//      myParticle.addScaledExternalForce(s, f);
//   }

//   protected MatrixBlock createRowBlock (int colSize) {
//      return createRowBlockNew (colSize);
//   }
//
//   protected MatrixBlock createColBlock (int rowSize) {
//      return createColBlockNew (rowSize);
//   }
//
//   protected MatrixBlock createRowBlockNew (int colSize) {
//      switch (colSize) {
//         case 1:
//            return new Matrix3x1Block();
//         case 3:
//            return new Matrix3x3Block();
//         case 6: 
//            return new Matrix3x6Block();
//         default:
//            return new MatrixNdBlock(3, colSize);
//      }
//   }
//
//   protected MatrixBlock createColBlockNew (int rowSize) {
//      switch (rowSize) {
//         case 1:
//            return new Matrix1x3Block();
//         case 3:
//            return new Matrix3x3Block();
//         case 6: 
//            return new Matrix6x3Block();
//         default:
//            return new MatrixNdBlock(rowSize, 3);
//      }
//   }

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

//   @Override
//   public void connectToHierarchy () {
//      Point point = getPoint();
//      Particle particle = getParticle();
//      if (point == null) {
//         throw new InternalErrorException ("null point");
//      }
//      if (particle == null) {
//         throw new InternalErrorException ("null particle");
//      }
//      super.connectToHierarchy ();
//      point.setAttached (this);
//      particle.addMasterAttachment (this);
//   }
//
//   @Override
//   public void disconnectFromHierarchy() {
//      Point point = getPoint();
//      Particle particle = getParticle();
//      if (point == null || particle == null) {
//         throw new InternalErrorException ("null point and/or particle");
//      }
//      super.disconnectFromHierarchy();
//      point.setAttached (null);
//      particle.removeMasterAttachment (this);
//   }

//   @Override
//   public void getHardReferences (List<ModelComponent> refs) {
//      super.getHardReferences (refs);
//      Point point = getPoint();
//      Particle particle = getParticle();
//      if (point == null || particle == null) {
//         throw new InternalErrorException ("null point and/or particle");
//      }
//      super.getHardReferences (refs);
//      refs.add (point);
//      refs.add (particle);
//   }

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


}
