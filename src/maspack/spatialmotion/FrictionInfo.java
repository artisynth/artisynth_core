package maspack.spatialmotion;

import maspack.matrix.VectorNd;
/**
 * Contains information for a single friction constraint set.  This is
 * associated with one bilateral or unilateral constraint, and corresponds to a
 * single column block entry in the transposed friction constraint matrix
 * <code>DT</code>.  The constraint set contains either one or two friction
 * directions (corresponding to a column block size of either 1 or 2).
 */
public class FrictionInfo {

   // Flag indicating that the associated contact constraint is BILATERAL
   public static final int BILATERAL = 0x01;

   public double mu;        // friction coefficient
   public int blockIdx;     // constraint block index within DT
   public int blockSize;    // constraint block size within DT
   public int contactIdx0;  // corresponding contact constraint index
   public int contactIdx1;  // second contact constraint index (if needed)
   public int flags;        // information flags

   public double stictionCreep; // allowed creep "velocity" for stiction.
   // stictionCreep > 0 regularizes friction force computation
   
   public double stictionCompliance;
   // if stictionCompliance > 0, stictionDisp gives displacements along
   // first and second friction directions:
   public double stictionDisp0;
   public double stictionDisp1;


   public FrictionInfo() {
      contactIdx1 = -1;
   }

   public FrictionInfo (FrictionInfo finfo) {
      set (finfo);
   }

   public void set (FrictionInfo finfo) {
      this.mu = finfo.mu;
      this.blockIdx = finfo.blockIdx;
      this.blockSize = finfo.blockSize;
      this.contactIdx0 = finfo.contactIdx0;
      this.contactIdx1 = finfo.contactIdx1;
      this.flags = finfo.flags;
      this.stictionCreep = finfo.stictionCreep;
   }

   public void set2D (int bi, int ci, double mu) {
      this.mu = mu;
      this.blockIdx = bi;
      this.blockSize = 2;
      this.contactIdx0 = ci;
      this.contactIdx1 = -1;
      this.flags = 0;
   }
      
   /**
    * Returns the maximum friction value based on the most recent contact force
    * and the coefficient of friction. Contacts forces are stored in the
    * supplied vector lam, at the location(s) indexed by contactIdx0 and
    * (possibly) contactIdx1.
    */
   public double getMaxFriction (VectorNd lam) {
      if (contactIdx1 == -1) {
         double val = lam.get(contactIdx0);
         if (val > 0) {
            return mu*val;
         }
         else {
            return 0;
         }
      }
      else {
         double val0 = Math.min(lam.get(contactIdx0), 0);
         double val1 = Math.min(lam.get(contactIdx1), 0);
         return mu*Math.hypot (val0, val1);
      }
   }

   /**
    * Checks FrictionInfo for equality
    */
   public boolean equals (FrictionInfo fi) {
      return (mu == fi.mu &&
              contactIdx0 == fi.contactIdx0 &&
              contactIdx1 == fi.contactIdx1 &&
              flags == fi.flags);
   }
};

