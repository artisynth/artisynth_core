package artisynth.core.materials;

import maspack.util.DataBuffer;
import maspack.matrix.SymmetricMatrix3d;

/**
 * Stores state information for QLV (Quasi-Linear Viscoelastic Behavior)
 */
public class QLVState extends ViscoelasticState {

   protected SymmetricMatrix3d mySigmaPrev;
   protected SymmetricMatrix3d mySigmaSave;
   protected double[] myGHPrev;
   protected double[] myS;
   
   protected double myH = 0;

   public QLVState () {
      mySigmaPrev = new SymmetricMatrix3d();
      mySigmaSave = new SymmetricMatrix3d();
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
      data.dput (mySigmaPrev.m00);
      data.dput (mySigmaPrev.m11);
      data.dput (mySigmaPrev.m22);
      data.dput (mySigmaPrev.m01);
      data.dput (mySigmaPrev.m02);
      data.dput (mySigmaPrev.m12);
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
      mySigmaSave.m10 = mySigmaSave.m01;
      mySigmaSave.m20 = mySigmaSave.m02;
      mySigmaSave.m21 = mySigmaSave.m12;

      mySigmaPrev.m00 = data.dget();
      mySigmaPrev.m11 = data.dget();
      mySigmaPrev.m22 = data.dget();
      mySigmaPrev.m01 = data.dget();
      mySigmaPrev.m02 = data.dget();
      mySigmaPrev.m12 = data.dget();
      mySigmaPrev.m10 = mySigmaPrev.m01;
      mySigmaPrev.m20 = mySigmaPrev.m02;
      mySigmaPrev.m21 = mySigmaPrev.m12;

      for (int k=0; k<myGHPrev.length; k++) {
         myGHPrev[k] = data.dget();
      }
      for (int k=0; k<myS.length; k++) {
         myS[k] = data.dget();
      }
   }

}
