/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import maspack.matrix.MatrixBlock;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.properties.PropertyList;
import maspack.util.DataBuffer;
import maspack.util.InternalErrorException;
import maspack.util.ListRemove;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/** 
 * NOTE: This class is still under construction. scaleDistance(),
 * transformGeometry(), and scan() and write() are unlikely to work properly.
 */
public abstract class ParticleConstraintNdBase extends ConstrainerBase
implements ScalableUnits {

   protected class ParticleInfo {
      Particle myPart;
      boolean myEngagedP;
      double[] myLam;
      double[] myDist;
      MatrixBlock myBlk; // should be 3xX

      ParticleInfo (Particle p) {
         myPart = p;
         myBlk = null;
      }
   }

   ArrayList<ParticleInfo> myParticleInfo;
   boolean myUnilateralP = false;
   double myBreakSpeed = Double.NEGATIVE_INFINITY;
   double myPenetrationTol = 0;
   private double myDamping = 0;
   private double myCompliance = 0;

   public static PropertyList myProps =
      new PropertyList (ParticleConstraintNdBase.class, ConstrainerBase.class);

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
         //notifyParentOfChange (StructureChangeEvent.defaultEvent);
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
      for (int i=0; i<myParticleInfo.size(); ++i) {
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
         for (ParticleInfo pi : myParticleInfo) {
            Particle p = pi.myPart;
            if (p.getSolveIndex() != -1) {
               sizes.append (pi.myDist.length);
            }
         }
      }
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {

      if (!myUnilateralP) {
         int bj = GT.numBlockCols();
         for (ParticleInfo pi : myParticleInfo) {    
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
         for (ParticleInfo pi : myParticleInfo) {      
            if (pi.myPart.getSolveIndex() != -1) {
               for (int i=0; i<pi.myDist.length; ++i) {
                  ConstraintInfo gi = ginfo[idx++];
                  gi.dist = pi.myDist[i];
                  gi.compliance = myCompliance;
                  gi.damping = myDamping;
                  gi.force = 0;
               }
            }
         }
      }
      return idx;
   }

   public int setBilateralImpulses (VectorNd lam, double h, int idx) {

      if (!myUnilateralP) {
         for (ParticleInfo pi : myParticleInfo) {
            if (pi.myPart.getSolveIndex() != -1) {
               for (int j=0; j<pi.myLam.length; ++j) {
                  pi.myLam[j] = lam.get(idx++);
               }
            }
         }
      }
      return idx;
   }

   public int getBilateralImpulses (VectorNd lam, int idx) {

      if (!myUnilateralP) {
         for (ParticleInfo pi : myParticleInfo) {
            if (pi.myPart.getSolveIndex() != -1) {
               for (int j=0; j<pi.myLam.length; ++j) {
                  lam.set (idx++, pi.myLam[j]);
               }
            }
         }
      }
      return idx;
   }

   public void getUnilateralSizes (VectorNi sizes) {

      if (myUnilateralP) {
         for (ParticleInfo pi : myParticleInfo) {
            if (pi.myEngagedP && pi.myPart.getSolveIndex() != -1) {
               sizes.append (pi.myDist.length);
            }
         }
      }
   }

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dg, int numu) {

      if (myUnilateralP) {
         int bj = NT.numBlockCols();
         for (ParticleInfo pi : myParticleInfo) {   
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
         for (ParticleInfo pi : myParticleInfo) {
            if (pi.myEngagedP && pi.myPart.getSolveIndex() != -1) {
               for (int j=0; j<pi.myDist.length; ++j) {
                  ConstraintInfo gi = ninfo[idx++];
                  gi.dist = pi.myDist[j];
                  gi.compliance = myCompliance;
                  gi.damping = myDamping;
                  gi.force = 0;
               }
            }
         }
      }
      return idx;
   }

   public int setUnilateralImpulses (VectorNd the, double h, int idx) {

      if (myUnilateralP) {
         for (ParticleInfo pi : myParticleInfo) {
            if (pi.myEngagedP && pi.myPart.getSolveIndex() != -1) {
               for (int j=0; j<pi.myLam.length; ++j) {
                  pi.myLam[j] = the.get(idx++);
               }
            }
         }
      }
      return idx;
   }

   public int getUnilateralImpulses (VectorNd the, int idx) {

      if (myUnilateralP) {
         for (ParticleInfo pi : myParticleInfo) {
            if (pi.myEngagedP && pi.myPart.getSolveIndex() != -1) {
               for (int j=0; j<pi.myLam.length; ++j) {
                  the.set (idx++, pi.myLam[j]);
               }
            }
         }
      }
      return idx;
   }

   public void zeroImpulses() {
      for (ParticleInfo pi : myParticleInfo) {
         for (int j=0; j<pi.myLam.length; ++j) {
            pi.myLam[j] = 0;
         }
      }
   }

   protected double updateEngagement (ParticleInfo pi, double maxpen) {
      boolean oldEngaged = pi.myEngagedP;

      boolean engaged = false;

      VectorNd vr = new VectorNd(pi.myDist.length);
      VectorNd v = new VectorNd(pi.myPart.getVelocity());
      pi.myBlk.mul(vr, v);

      for (int j=0; j<pi.myDist.length; ++j) {
         if (pi.myDist[j] < 0) {
            engaged = true;
            if (-pi.myDist[j] > maxpen) {
               maxpen = -pi.myDist[j];
            }
         }
      }

      // check if sufficient velocity to break
      if (oldEngaged && !engaged) {
         boolean breakEngagement = false;
         for (int j=0; j<pi.myDist.length; ++j) {
            if (vr.get(j) > myBreakSpeed)  {
               breakEngagement = true;
            }
         }
         if (breakEngagement) {
            pi.myEngagedP = false;
         }
      } else if (engaged){
         pi.myEngagedP = true;
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
      for (ParticleInfo pi : myParticleInfo) {
         list.add (pi.myPart);
      }
   }

   @Override
   public void getSoftReferences (List<ModelComponent> refs) {
      super.getSoftReferences (refs);
      for (ParticleInfo pi : myParticleInfo) {
         refs.add (pi.myPart);
      }
   }

   @Override
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      super.updateReferences (undo, undoInfo);   

      if (undo) {
         Object obj = undoInfo.removeFirst();
         if (obj != NULL_OBJ) {
            @SuppressWarnings("unchecked")
            ListRemove<ParticleInfo> lr = (ListRemove<ParticleInfo>)obj;
            lr.undo();
         }
      }
      else {
         // remove soft references which aren't in the hierarchy any more:
         ListRemove<ParticleInfo> particleRemove = null;
         for (int i=0; i<myParticleInfo.size(); ++i) {
            ParticleInfo pi = myParticleInfo.get(i);
            if (!ComponentUtils.isConnected (this, pi.myPart)) {
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

   /** 
    * {@inheritDoc}
    */
   public void skipAuxState (DataBuffer data) {
      int nump = data.zget();
      data.zskip (nump); // state stored is "myEngagedP" for each particle
   }

   public void getAuxState (DataBuffer data) {
      // store engaged information for particles, regardless of whether
      // we are unilateral or not
      data.zput (myParticleInfo.size());
      for (ParticleInfo pi : myParticleInfo) {
         data.zput (pi.myEngagedP ? 1 : 0);
      }
   }

   public void getInitialAuxState (
      DataBuffer newData, DataBuffer oldData) {

      // engaged always starts out as false
      newData.zput (myParticleInfo.size());
      for (ParticleInfo pi : myParticleInfo) {
         newData.zput (0);
      }
      if (oldData != null) {
         skipAuxState (oldData);
      }
   }

   public void setAuxState (DataBuffer data) {
      int nump = data.zget();
      if (nump != numParticles()) {
         throw new InternalErrorException (
            "Stored state has information for " + nump +
            " particles, but constraint has " + numParticles());
      }
      for (ParticleInfo pi : myParticleInfo) {
         pi.myEngagedP = (data.zget() != 0);
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
      for (ParticleInfo pi : myParticleInfo) {
         parts.add (pi.myPart);
      }
      return parts;
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
         throws IOException {

      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt);
      pw.print ("particles=");
      ScanWriteUtils.writeBracketedReferences (pw, getParticleList(), ancestor);
   }

}
