package artisynth.core.materials;

import maspack.util.DataBuffer;
import maspack.util.NumberFormat;
import maspack.matrix.SymmetricMatrix3d;

/**
 * Stores state information for QLV (Quasi-Linear Viscoelastic Behavior)
 */
public class QLVState extends ViscoelasticState implements MaterialStateObject {

   protected SymmetricMatrix3d mySPrev;
   protected SymmetricMatrix3d mySSave;
   protected double[] myAHPrev;
   protected double[] myB;
   
   public double myH = 0;

   public QLVState () {
      this (QLVBehavior.N_MAX);
   }

   public QLVState (int n) {
      mySPrev = new SymmetricMatrix3d();
      mySSave = new SymmetricMatrix3d();
      myAHPrev = new double[6*n];
      myB = new double[n];
   }

   public int getStateSize() {
      // need to save sigma, deltaSigma, gH, and s
      return 12 + (6 + 1)*myAHPrev.length;
   }

   /** 
    * Stores the state data in a DataBuffer
    */
   public void getState (DataBuffer data) {
      data.dput (myH);
      data.dput (mySSave);
      data.dput (mySPrev);
      for (int k=0; k<myAHPrev.length; k++) {
         data.dput (myAHPrev[k]);             
      }
      for (int k=0; k<myB.length; k++) {
         data.dput (myB[k]);
      }
   }   

   /** 
    * Sets the state data from a buffer of doubles.
    */
   public void setState (DataBuffer data) {
      myH = data.dget();
      data.dget (mySSave);
      data.dget (mySPrev);

      for (int k=0; k<myAHPrev.length; k++) {
         myAHPrev[k] = data.dget();
      }
      for (int k=0; k<myB.length; k++) {
         myB[k] = data.dget();
      }
   }
   
   public String toString (String fmtStr) {
      NumberFormat fmt = new NumberFormat(fmtStr);
      StringBuilder sb = new StringBuilder();
      sb.append ("h=" + myH);
      sb.append ("SSave=\n");
      sb.append (mySSave.toString(fmt));
      sb.append ("SPrev=\n");
      sb.append (mySPrev.toString(fmt));
      sb.append ("AHprev=\n");
      for (int k=0; k<myAHPrev.length; k+=6) {
         sb.append (
            (k/6) + " " + 
            fmt.format(myAHPrev[k+0]) + " " + fmt.format(myAHPrev[k+1]) + " " +
            fmt.format(myAHPrev[k+2]) + " " + fmt.format(myAHPrev[k+3]) + " " +
            fmt.format(myAHPrev[k+4]) + " " + fmt.format(myAHPrev[k+5]) + "\n");
      }
      sb.append ("b=\n");
      for (int k=0; k<myB.length; k++) {
         sb.append (k + " " + fmt.format(myB[k]) + "\n");
      }
      return sb.toString();
   }

   public boolean hasState() {
      return true;
   }

}
