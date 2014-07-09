/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.properties.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import maspack.render.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.util.*;

public class ParticlePlaneConstraint extends ConstrainerBase
   implements ScalableUnits, TransformableGeometry {

   Matrix3x1Block myBlk;
   Particle myParticle;
   double myOff;
   double myLam;

   public ParticlePlaneConstraint () {
      myBlk = new Matrix3x1Block ();
   }

   public ParticlePlaneConstraint (Particle p, Plane plane) {
      myParticle = p;
      myBlk = new Matrix3x1Block (plane.normal);
      myOff = plane.offset;
   }

   public Particle getParticle() {
      return myParticle;
   }

   public void getBilateralSizes (VectorNi sizes) {
      if (myParticle.getSolveIndex() != -1) {
         sizes.append (1);
      }
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb, IntHolder changeCnt) {

      int idx = myParticle.getSolveIndex();
      int bj = GT.numBlockCols();
      if (idx != -1) {
         GT.addBlock (idx, bj, myBlk);
         if (dg != null) {
            dg.set (numb, 0);
         }
         numb++;
      }
      return numb;
   }

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {
      if (myParticle.getSolveIndex() == -1) {
         return idx;
      }
      ConstraintInfo gi = ginfo[idx++];
      Vector3d pos = myParticle.myState.pos;
      gi.dist = (myBlk.m00*pos.x + myBlk.m10*pos.y + myBlk.m20*pos.z - myOff);
      gi.compliance = 0;
      gi.damping = 0;
      return idx;
   }

   public int setBilateralImpulses (VectorNd lam, double h, int idx) {
      if (myParticle.getSolveIndex() != -1) {
         myLam = lam.get(idx++);
      }
      return idx;
   }

   public int getBilateralImpulses (VectorNd lam, int idx) {
      if (myParticle.getSolveIndex() != -1) {
         lam.set (idx++, myLam);
      }
      return idx;
   }
   
   public void zeroImpulses() {
      myLam = 0;
   }

   public void getUnilateralSizes (VectorNi sizes) {
      // nothing to add
   }

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu, double t) {
      return numu;
   }

   public int getUnilateralInfo (ConstraintInfo[] ninfo, int idx) {
      return idx;
   }

   public int setUnilateralImpulses (VectorNd the, double h, int idx) {
      return idx;
   }

   public double updateConstraints (double t, int flags) {
      return 0;
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      refs.add (myParticle);
   }

//   @Override
//   public void connectToHierarchy () {
//      super.connectToHierarchy ();
//      myParticle.addBackReference (this);
//   }
//
//   @Override
//   public void disconnectFromHierarchy() {
//      super.disconnectFromHierarchy();
//      myParticle.removeBackReference (this);
//   }

   public void render (GLRenderer renderer, int flags) {
   }
   
   public void scaleMass (double s) {
   }

   public void scaleDistance (double s) {
      myOff *= s;
   }

   public void transformGeometry (AffineTransform3dBase X) {
      transformGeometry (X, this, 0);
   }

   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {

      Plane plane = new Plane (myBlk.m00, myBlk.m10, myBlk.m20, myOff);
      plane.transform (X, plane);
      myBlk.set (plane.normal);
      myOff = plane.offset;
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "particle", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "plane")) {
         Plane plane = new Plane();
         plane.scan (rtok);
         myBlk.set (plane.normal);
         myOff = plane.offset;
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "particle")) {
         myParticle =
            postscanReference (tokens, Particle.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      
      Plane plane = new Plane (myBlk.m00, myBlk.m10, myBlk.m20, myOff);
      pw.println ("particle=" +
                  ComponentUtils.getWritePathName(ancestor,myParticle));
      pw.println ("plane=[" + plane.toString (fmt) + "]");
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt);
   }

}     
