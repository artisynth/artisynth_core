package artisynth.core.materials;

import maspack.util.DataBuffer;
import maspack.matrix.SymmetricMatrix3d;

/**
 * Stores state information for QLV (Quasi-Linear Viscoelastic Behavior)
 */
public class QLVState extends ViscoelasticState {

   protected SymmetricMatrix3d mySigmaPrev;
   protected SymmetricMatrix3d mySigmaSave;
   protected SymmetricMatrix3d myDeltaSigma;
   protected double[] myGHPrev;
   protected double[] myS;
   
   protected double myH = 0;

   public QLVState () {
      mySigmaPrev = new SymmetricMatrix3d();
      mySigmaSave = new SymmetricMatrix3d();
      myDeltaSigma = new SymmetricMatrix3d();
      myGHPrev = new double[QLVBehavior.N_MAX*6];
      myS = new double[QLVBehavior.N_MAX];
   }

   public int getStateSize() {
      // need to save sigma, deltaSigma, gH, and s
      return 12 + 6*QLVBehavior.N_MAX + QLVBehavior.N_MAX;
   }

   /** 
    * Stores the state data in a DataBuffer
    */
   public void getState (DataBuffer data) {
      data.dput (mySigmaSave.m00);
      data.dput (mySigmaSave.m11);
      data.dput (mySigmaSave.m22);
      data.dput (mySigmaSave.m01);
      data.dput (mySigmaSave.m02);
      data.dput (mySigmaSave.m12);
      data.dput (myDeltaSigma.m00);
      data.dput (myDeltaSigma.m11);
      data.dput (myDeltaSigma.m22);
      data.dput (myDeltaSigma.m01);
      data.dput (myDeltaSigma.m02);
      data.dput (myDeltaSigma.m12);
      for (int k=0; k<myGHPrev.length; k++) {
         data.dput (myGHPrev[k]);             
      }
      for (int k=0; k<myS.length; k++) {
         data.dput (myS[k]);
      }
   }   

   /** 
    * Sets the state data from a buffer of doubles.
    */
   public void setState (DataBuffer data) {
      mySigmaSave.m00 = data.dget();
      mySigmaSave.m11 = data.dget();
      mySigmaSave.m22 = data.dget();
      mySigmaSave.m01 = data.dget();
      mySigmaSave.m02 = data.dget();
      mySigmaSave.m12 = data.dget();
      myDeltaSigma.m00 = data.dget();
      myDeltaSigma.m11 = data.dget();
      myDeltaSigma.m22 = data.dget();
      myDeltaSigma.m01 = data.dget();
      myDeltaSigma.m02 = data.dget();
      myDeltaSigma.m12 = data.dget();
      for (int k=0; k<myGHPrev.length; k++) {
         myGHPrev[k] = data.dget();
      }
      for (int k=0; k<myS.length; k++) {
         myS[k] = data.dget();
      }
   }

}
