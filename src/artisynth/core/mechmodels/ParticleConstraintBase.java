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
import maspack.geometry.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import maspack.geometry.*;
import maspack.render.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.util.*;

/** 
 * NOTE: This class is still under construction. scaleDistance(),
 * transformGeometry(), and scan() and write() are unlikely to work properly.
 */
public abstract class ParticleConstraintBase extends ConstrainerBase
   implements ScalableUnits {

   protected class ParticleInfo {

      Particle myPart;
      double myLam;
      boolean myEngagedP;
      double myDist;
      Matrix3x1Block myBlk;

      ParticleInfo (Particle p) {
         myPart = p;
         myBlk = new Matrix3x1Block();
      }
   }

   ArrayList<ParticleInfo> myParticleInfo;
   boolean myUnilateralP = false;
   double myBreakSpeed = Double.NEGATIVE_INFINITY;
   double myPenetrationTol = 0;
   private double myDamping = 0;
   private double myCompliance = 0;

   public static PropertyList myProps =
      new PropertyList (ParticleConstraintBase.class, ConstrainerBase.class);

   static {
      myProps.add (
         "unilateral isUnilateral *", "unilateral constraint flag", false);
      myProps.add (
         "penetrationTol", "penetration tolerance for unilateral mode", 0);
      myProps.add (
         "compliance", "compliance for this constraint", 0);
      myProps.add (
         "damping", "damping for this constraint", 0);      
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getDamping() {
      return myDamping;
   }

   public void setDamping (double d) {
      myDamping = d;
   }

   public double getCompliance() {
      return myCompliance;
   }

   public void setCompliance (double c) {
      myCompliance = c;
   }

   public double getPenetrationTol () {
      return myPenetrationTol;
   }

   public void setPenetrationTol (double tol) {
      myPenetrationTol = tol;
   }

   public boolean isUnilateral() {
      return myUnilateralP;
   }

   public void setUnilateral (boolean unilateral) {
      if (myUnilateralP != unilateral) {
         myUnilateralP = unilateral;
         notifyParentOfChange (StructureChangeEvent.defaultEvent);
      }
   }

   protected abstract ParticleInfo createParticleInfo (Particle p);

   public void addParticle (Particle p) {
      myParticleInfo.add(createParticleInfo (p));
      notifyParentOfChange (StructureChangeEvent.defaultEvent);
   }

   public void addParticles (Collection<? extends Particle> parts) {
      for (Particle p : parts) {
         addParticle (p);
      }
   }

   private int getParticleIndex (Particle p) {
      for (int i=0; i<myParticleInfo.size(); i++) {
         if (myParticleInfo.get(i).myPart == p) {
            return i;
         }
      }
      return -1;
   }

   public Particle getParticle (int idx) {
      if (idx < 0 || idx >= myParticleInfo.size()) {
         throw new ArrayIndexOutOfBoundsException (
            "idx=" + idx + ", number of particles=" + myParticleInfo.size());
      }
      return myParticleInfo.get(idx).myPart;
   }

   public int numParticles() {
      return myParticleInfo.size();      
   }

   public boolean removeParticle (Particle p) {
      int idx = getParticleIndex (p);
      if (idx != -1) {
         myParticleInfo.remove (idx);
         notifyParentOfChange (StructureChangeEvent.defaultEvent);
         return true;
      }
      else {
         return false;
      }
   }

   public void getBilateralSizes (VectorNi sizes) {

      if (!myUnilateralP) {
         for (int i=0; i<myParticleInfo.size(); i++) {
            Particle p = myParticleInfo.get(i).myPart;
            if (p.getSolveIndex() != -1) {
               sizes.append (1);
            }
         }
      }
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {

      if (!myUnilateralP) {
         int bj = GT.numBlockCols();
         for (int i=0; i<myParticleInfo.size(); i++) {
            ParticleInfo pi = myParticleInfo.get(i);    
            int idx = pi.myPart.getSolveIndex();
            if (idx != -1) {
               GT.addBlock (idx, bj++, pi.myBlk);
               if (dg != null) {
                  dg.set (numb, 0);
               }
               numb++;
            }
         }
      }
      return numb;
   }

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {

      if (!myUnilateralP) {
         for (int i=0; i<myParticleInfo.size(); i++) {      
            ParticleInfo pi = myParticleInfo.get(i);
            if (pi.myPart.getSolveIndex() != -1) {
               ConstraintInfo gi = ginfo[idx++];
               gi.dist = pi.myDist;
               gi.compliance = myCompliance;
               gi.damping = myDamping;
               gi.force = 0;
            }
         }
      }
      return idx;
   }

   public int setBilateralForces (VectorNd lam, double s, int idx) {

      if (!myUnilateralP) {
         for (int i=0; i<myParticleInfo.size(); i++) {
            ParticleInfo pi = myParticleInfo.get(i);
            if (pi.myPart.getSolveIndex() != -1) {
               pi.myLam = lam.get(idx++)*s;
            }
         }
      }
      return idx;
   }

   public int getBilateralForces (VectorNd lam, int idx) {

      if (!myUnilateralP) {
         for (int i=0; i<myParticleInfo.size(); i++) {
            ParticleInfo pi = myParticleInfo.get(i);
            if (pi.myPart.getSolveIndex() != -1) {
               lam.set (idx++, pi.myLam);
            }
         }
      }
      return idx;
   }
   
   public void getUnilateralSizes (VectorNi sizes) {

      if (myUnilateralP) {
         for (int i=0; i<myParticleInfo.size(); i++) {
            ParticleInfo pi = myParticleInfo.get(i);
            if (pi.myEngagedP && pi.myPart.getSolveIndex() != -1) {
               sizes.append (1);
            }
         }
      }
   }

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dg, int numu) {

      if (myUnilateralP) {
         int bj = NT.numBlockCols();
         for (int i=0; i<myParticleInfo.size(); i++) {
            ParticleInfo pi = myParticleInfo.get(i);    
            int idx = pi.myPart.getSolveIndex();
            if (pi.myEngagedP && idx != -1) {
               NT.addBlock (idx, bj++, pi.myBlk);
               if (dg != null) {
                  dg.set (numu, 0);
               }
               numu++;
            }
         }
      }
      return numu;
   }

   public int getUnilateralInfo (ConstraintInfo[] ninfo, int idx) {

      if (myUnilateralP) {
         for (int i=0; i<myParticleInfo.size(); i++) {      
            ParticleInfo pi = myParticleInfo.get(i);
            if (pi.myEngagedP && pi.myPart.getSolveIndex() != -1) {
               ConstraintInfo gi = ninfo[idx++];
               gi.dist = pi.myDist;
               gi.compliance = myCompliance;
               gi.damping = myDamping;
               gi.force = 0;
            }
         }
      }
      return idx;
   }

   public int setUnilateralForces (VectorNd the, double s, int idx) {

      if (myUnilateralP) {
         for (int i=0; i<myParticleInfo.size(); i++) {
            ParticleInfo pi = myParticleInfo.get(i);
            if (pi.myEngagedP && pi.myPart.getSolveIndex() != -1) {
               pi.myLam = the.get(idx++)*s;
            }
         }
      }
      return idx;
   }

   public int getUnilateralForces (VectorNd the, int idx) {

      if (myUnilateralP) {
         for (int i=0; i<myParticleInfo.size(); i++) {
            ParticleInfo pi = myParticleInfo.get(i);
            if (pi.myEngagedP && pi.myPart.getSolveIndex() != -1) {
               the.set (idx++, pi.myLam);
            }
         }
      }
      return idx;
   }
   
   public void zeroForces() {

      for (int i=0; i<myParticleInfo.size(); i++) {
         myParticleInfo.get(i).myLam = 0;
      }
   }

   protected double updateEngagement (ParticleInfo pi, double maxpen) {
      boolean oldEngaged = pi.myEngagedP;
      if (pi.myDist < 0) {
         pi.myEngagedP = true;
         if (-pi.myDist > maxpen) {
            maxpen = -pi.myDist;
         }
      }
      if (pi.myEngagedP && pi.myDist >= 0) {
         if (pi.myBlk.dot(pi.myPart.getVelocity()) > myBreakSpeed)  {
            pi.myEngagedP = false;
            pi.myLam = 0;
         }
      }
      return maxpen;
   }

   public boolean isEngaged(int idx) {
      if (idx < 0 || idx >= myParticleInfo.size()) {
         throw new ArrayIndexOutOfBoundsException (
            "idx=" + idx + ", number of particles=" + myParticleInfo.size());
      }
      return myParticleInfo.get(idx).myEngagedP;
   }
   
   public void getConstrainedComponents (List<DynamicComponent> list) {
      for (int i=0; i<myParticleInfo.size(); i++) {
         list.add (myParticleInfo.get(i).myPart);
      }
   }
   
   @Override
   public void getSoftReferences (List<ModelComponent> refs) {
      super.getSoftReferences (refs);
      for (int i=0; i<myParticleInfo.size(); i++) {
         refs.add (myParticleInfo.get(i).myPart);
      }
   }

   @Override
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      super.updateReferences (undo, undoInfo);   

      if (undo) {
         Object obj = undoInfo.removeFirst();
         if (obj != NULL_OBJ) {
            ((ListRemove<ParticleInfo>)obj).undo();
         }
      }
      else {
         // remove soft references which aren't in the hierarchy any more:
         ListRemove<ParticleInfo> particleRemove = null;
         for (int i=0; i<myParticleInfo.size(); i++) {
            ParticleInfo pi = myParticleInfo.get(i);
            if (!ComponentUtils.areConnected (this, pi.myPart)) {
               if (particleRemove == null) {
                  particleRemove =
                     new ListRemove<ParticleInfo>(myParticleInfo);
               }
               particleRemove.requestRemove(i);
            }
         }
         if (particleRemove != null) {
            particleRemove.remove();
            undoInfo.addLast (particleRemove);
         }
         else {
            undoInfo.addLast (NULL_OBJ);
         }
      }
   }

   public void getState (DataBuffer data) {
      // store engaged information for particles, regardless of whether
      // we are unilateral or not
      data.zput (myParticleInfo.size());
      for (int i=0; i<myParticleInfo.size(); i++) {
         data.zputBool (myParticleInfo.get(i).myEngagedP);
      }
   }

   public void setState (DataBuffer data) {
      int nump = data.zget();
      if (nump != numParticles()) {
         throw new InternalErrorException (
            "Stored state has information for " + nump +
            " particles, but constraint has " + numParticles());
      }
      for (int i=0; i<myParticleInfo.size(); i++) {
         myParticleInfo.get(i).myEngagedP = data.zgetBool();
      }
   }      

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();      
      if (scanAndStoreReferences (rtok, "particles", tokens) != -1) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "particles")) {
         ArrayList<Particle> parts = new ArrayList<Particle>();
         postscanReferences (tokens, parts, Particle.class, ancestor);
         myParticleInfo.clear();
         addParticles (parts);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   private ArrayList<Particle> getParticleList() {
      ArrayList<Particle> parts = 
         new ArrayList<Particle>(myParticleInfo.size());
      for (int i=0; i<myParticleInfo.size(); i++) {
         parts.add (myParticleInfo.get(i).myPart);
      }
      return parts;
   }
   
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ancestor);
      pw.print ("particles=");
      ScanWriteUtils.writeBracketedReferences (pw, getParticleList(), ancestor);
   }

}
