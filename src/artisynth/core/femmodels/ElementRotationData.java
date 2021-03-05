package artisynth.core.femmodels;

import maspack.matrix.*;

/**
 * Stores on-demand generated data associated with the rotation of a FEM
 * element, as extracted from the "warping" point located at its center.
 */
public class ElementRotationData {

   public static int R_VALID = 0x1;  // also means H is valid
   public static int INVB_VALID = 0x2;

   RotationMatrix3d myR;
   Matrix3d myH;
   Matrix3d myInvB;
   int myValidFlags = 0;

   public ElementRotationData() {
      myR = new RotationMatrix3d();
      myH = new Matrix3d();
      // myInvB will be allocated on demand
   }

   public void computeR (FemElement3dBase elem, PolarDecomposition3d polard) {
      IntegrationPoint3d wpnt = elem.getWarpingPoint();
      IntegrationData3d wdat = elem.getWarpingData();  
      Matrix3d F = new Matrix3d();
      wpnt.computeGradient (F, elem.getNodes(), wdat.getInvJ0());
      if (polard == null) {
         polard = new PolarDecomposition3d();
      }
      polard.factor (F);
      myR.set (polard.getR());
      polard.getH(myH);
      myValidFlags |= R_VALID;
   }

   public void computeInvB (FemElement3dBase elem, PolarDecomposition3d polard) {
      if ((myValidFlags & R_VALID) == 0) {
         computeR (elem, polard);
      }
      if (myInvB == null) {
         myInvB = new Matrix3d();
      }
      myInvB.negate (myH);
      myInvB.addDiagonal (myH.trace());
      myInvB.fastInvert (myInvB);
      myValidFlags |= INVB_VALID;
   }

   public void invalidate() {
      myValidFlags = 0;
   }
}
