package artisynth.core.mechmodels;

import java.util.*;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.geometry.*;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

public abstract class DynamicAttachmentTestBase<C extends DynamicAttachment>
   extends UnitTest {

   private double EPS = 1e-12;

   VectorNd getPosState (DynamicComponent c) {
      VectorNd pos = new VectorNd(c.getPosStateSize());
      c.getPosState (pos.getBuffer(), 0);
      return pos;
   }

   VectorNd getVelState (DynamicComponent c) {
      VectorNd vel = new VectorNd(c.getVelStateSize());
      c.getVelState (vel.getBuffer(), 0);
      return vel;
   }

   VectorNd getForce (DynamicComponent c) {
      VectorNd force = new VectorNd(c.getVelStateSize());
      c.getForce (force.getBuffer(), 0);
      return force;
   }

   /**
    * Computes the position for the slave given the current state of the master
    * components.
    *
    * @param pos returns the computed position. On input, will be set to the
    * position state size of the slave.
    * @param at attachment for which the position is to be computed
    */
   public abstract void computeSlavePos (VectorNd pos, C at);

   /**
    * Computes the velocity for the slave given the current state of the master
    * components.
    * 
    * @param vel returns the computed velocity. On input, will be set to the
    * velocity state size of the slave.
    * @param at attachment for which the velocity is to be computed
    */
   public abstract void computeSlaveVel (VectorNd vel, C at);

   /**
    * Computes the force for the idx-th master given the current force on the
    * slave component.
    *
    * @param force returns the computed force. On input, will be set to the
    * velocity state size of the master.
    * @param idx index of the master component
    * @param at attachment for which the force is to be computed
    */
   public abstract void computeMasterForce (VectorNd force, int idx, C at);

   /**
    * Returns an instance of the attachment, complete with master and slave
    * components.
    */
   public abstract C createAttachment();

   protected void setRandomStates (C at) {
      DynamicComponent slave = at.getSlave();
      slave.setRandomForce();

      DynamicComponent[] masters = at.getMasters();
      for (int idx=0; idx<masters.length; idx++) {
         masters[idx].setRandomPosState();
         masters[idx].setRandomVelState();
      }
   }
   
   public void test (C at) {

      DynamicComponent slave = at.getSlave();
      int ssize = slave.getVelStateSize();

      // zero master forces so at.applyForces() will accumulate 
      // the master forces associated with the slave
      DynamicComponent[] masters = at.getMasters();
      for (int idx=0; idx<masters.length; idx++) {
         masters[idx].zeroForces();
      }
      
      at.updatePosStates();
      at.updateVelStates();     
      at.applyForces();

      // check slave position with value computed by computeSlavePos()
      VectorNd posCheck = new VectorNd(slave.getPosStateSize());
      computeSlavePos (posCheck, at);
      VectorNd slavePos = getPosState (slave);
      checkEquals ("slave pos =", slavePos, posCheck, EPS);      

      // check slave velocity with value computed by computeSlaveVel()
      VectorNd velCheck = new VectorNd(slave.getVelStateSize());
      computeSlaveVel (velCheck, at);
      VectorNd slaveVel = getVelState (slave);
      checkEquals ("slave vel =", slaveVel, velCheck, EPS);      

      // load slave force into ssize X 1 matrix S for use in mulSubGTM()
      MatrixNdBlock S = new MatrixNdBlock (ssize, 1);
      VectorNd slaveForce = getForce(slave);
      for (int i=0; i<ssize; i++) {
         S.set (i, 0, slaveForce.get(i));
      }

      for (int idx=0; idx<masters.length; idx++) {
         DynamicComponent master = masters[idx];
         int msize = master.getVelStateSize();

         // check master force with value computed by computeMasterForce()
         VectorNd forceCheck = new VectorNd (msize);
         computeMasterForce (forceCheck, idx, at);
         VectorNd masterForce = getForce(master);
         checkEquals (
            "master force "+idx+" =", masterForce, forceCheck, EPS);

         // check master force with value computed by at.mulSubGTM()
         MatrixNdBlock M = new MatrixNdBlock (msize, 1);
         at.mulSubGTM (M, S, idx);
         for (int i=0; i<msize; i++) {
            forceCheck.set (i, M.get (i, 0));
         }
         checkEquals (
            "master force "+idx+" from mulSubGTM() =",
            masterForce, forceCheck, EPS);
      }

      // check slave vel against sum of G vel for all masters
      slaveVel.setZero();
      for (int idx=0; idx<masters.length; idx++) {
         DynamicComponent master = masters[idx];
         VectorNd masterVel = getVelState (master);
         MatrixNd G = new MatrixNd(at.getGT(idx));
         G.transpose ();
         G.mulAdd (slaveVel, masterVel);
      }
      slaveVel.negate();
      checkEquals ("slave vel from GT =", slaveVel, velCheck, EPS);

      // test mulSubGTM() and mulSubMG() for different matrix sizes, making
      // sure that M G = (G^T M^T)^T. Also check that the vector method
      // mulSubGT() gives the same result as mulSubGTM().
      for (int idx=0; idx<masters.length; idx++) {
         int msize = masters[idx].getVelStateSize();

         MatrixNd GT = new MatrixNd (at.getGT(idx));

         for (int k=1; k<10; k++) {
            // check using GT
            MatrixNd MV = new MatrixNd (ssize,k);
            MV.setRandom();
            MatrixBlock M = MatrixBlockBase.alloc (ssize,k);
            M.set (MV);

            MatrixNd Dcheck = new MatrixNd (msize,k);
            Dcheck.mul (GT, MV);
            Dcheck.negate();

            MatrixBlock D = MatrixBlockBase.alloc (msize,k);
            at.mulSubGTM (D, M, idx);
            checkEquals ("D from mulSubGTM(D,M,"+idx+")=", D, Dcheck, EPS);

            // check against mulSubMG()
            MatrixBlock DT = MatrixBlockBase.alloc (k,msize);
            MatrixBlock MT = MatrixBlockBase.alloc (k,ssize);
            MatrixNd MVT = new MatrixNd (k,ssize);
            MatrixNd DTcheck = new MatrixNd (k,msize);
            MVT.transpose (MV);
            MT.set (MVT);
            at.mulSubMG (DT, MT, idx);
            DTcheck.transpose (Dcheck);
            checkEquals ("D from mulSubMD(D,M,"+idx+")=", DT, DTcheck, EPS);

            if (k==1) {
               int xoff = 7; // assume an offset of 7
               int yoff = 5; // assume an offset of 5
               double[] xbuf = new double[ssize+xoff];
               double[] ybuf = new double[msize+yoff];
               for (int i=0; i<ssize; i++) {
                  xbuf[i+xoff] = MV.get(i,0);
               }
               at.mulSubGT (ybuf, yoff, xbuf, xoff, idx);
               for (int i=0; i<msize; i++) {
                  D.set(i,0, ybuf[i+yoff]);
               }
               checkEquals ("mulSubGT(D,M,"+idx+") =", D, Dcheck, EPS);
            }
         }
      }
   }
   
   public void test() {
      C at = createAttachment();

      int ntrials = 10;
      for (int i=0; i<ntrials; i++) {
         setRandomStates (at);
         test (at);
      }
   }
   
}
