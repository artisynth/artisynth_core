package artisynth.core.femmodels;

import maspack.util.*;
import maspack.matrix.*;

import artisynth.core.femmodels.PointSkinAttachment.*;
import artisynth.core.femmodels.SkinMeshBody.*;
import artisynth.core.materials.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

public class FemSkinDerivTest extends UnitTest {
   
   FemModel3d myFem;
   FemModelInfo myFemInfo;

   ComponentState myState0;
   ComponentState myState1;

   private void computeRH (RotationMatrix3d R, Matrix3d H, FemElement3d elem) {
      IntegrationPoint3d wpnt = elem.getWarpingPoint();
      IntegrationData3d wdat = elem.getWarpingData();

      Matrix3d F = new Matrix3d();
      wpnt.computeGradient (F, elem.getNodes(), wdat.getInvJ0());
      PolarDecomposition3d pd = new PolarDecomposition3d(F);
      R.set (pd.getR());
      if (H != null) {
         pd.getH(H);
      }
   }

   private void setFemState (ComponentState state) {
      myFem.setState (state);
      myFem.invalidateElementRotationData();
   }

   /**
    * Test the formula that for a single node of an FEM, the contribution that
    * its velocity {@code v} makes to the spin (angular velocity) at an
    * integration point is given by
    * <pre>
    * w = R inv(B) [ dDdX ] R^T v,  B = tr(H)I - H
    * </pre>
    * where {@code R} and {@code H} come from the polar decomposition
    * <pre>
    * F = R H
    * </pre>
    * of the deformation gradient {@code F}.
    */
   private void testSpinDeriv() {
      setFemState (myState1);
      FemElement3d elem = myFem.getElement(13);
      IntegrationPoint3d wpnt = elem.getWarpingPoint();
      IntegrationData3d wdat = elem.getWarpingData();

      RotationMatrix3d R0 = new RotationMatrix3d();
      Matrix3d H = new Matrix3d();      

      // check contribution of node 34 to spin
      FemNode3d n34 = myFem.getNode(34);

      computeRH (R0, H, elem);
      int k = elem.getNodeIndex (n34);
      Vector3d GNs = wpnt.getGNs()[k];
      Vector3d GNX = new Vector3d();
      wdat.getInvJ0().mulTranspose (GNX, GNs);

      Matrix3d B = new Matrix3d();
      B.negate(H);
      B.addDiagonal (H.trace());
      Matrix3d Binv = new Matrix3d();
      Binv.invert (B);

      Matrix3d blk = new Matrix3d();
      blk.transpose (R0);
      blk.crossProduct (GNX, blk);
      blk.mul (Binv, blk);
      blk.mul (R0, blk);

      Matrix3d blkChk = new Matrix3d();
      double h = 1e-8;
      Point3d npos0 = new Point3d (n34.getPosition());
      for (int j=0; j<3; j++) {
         RotationMatrix3d R = new RotationMatrix3d();
         Point3d npos = new Point3d();
         npos.set (j, h);
         npos.add (npos0);
         n34.setPosition (npos);
         computeRH (R, null, elem);
         Matrix3d DR = new Matrix3d();
         DR.sub (R, R0);
         DR.scale (1/h);
         DR.mulTransposeRight (DR, R0);
         Vector3d w = new Vector3d (-DR.m12, DR.m02, -DR.m01);
         blkChk.setColumn (j, w);
         n34.setPosition (npos0);
      }

      double tol = blkChk.frobeniusNorm()*1e-8;
      Matrix3d Err = new Matrix3d();
      Err.sub (blk, blkChk);
      double err = Err.frobeniusNorm()/blkChk.frobeniusNorm();
      if (err > 1e-7) {
         System.out.println ("blk=\n" + blk.toString ("%10.6f"));
         System.out.println ("blkChk=\n" + blkChk.toString ("%10.6f"));
         System.out.println ("Error=" + err);
         throw new TestException ("spin deriv failed");
      }
      setFemState (myState0);
   }

   private MatrixNd computeDeriv (ElementConnection econ) {
      FemNode3d[] nodes = econ.myElem.getNodes();
      MatrixNd deriv = new MatrixNd (3, 3*nodes.length);
      MatrixBlock[] blks = new MatrixBlock[nodes.length];
      int[] blkIdxs = new int[nodes.length];
      for (int i=0; i<nodes.length; i++) {
         blks[i] = new Matrix3x3Block();
         blkIdxs[i] = i;
      }

      // call addPosition to update data used to compute blocks
      Point3d pos0 = new Point3d();
      PolarDecomposition3d polard = new PolarDecomposition3d();
      econ.addPosition (pos0, polard);
      econ.updateMasterBlocks (blks, blkIdxs, 0);
      Matrix3d M = new Matrix3d();
      for (int i=0; i<nodes.length; i++) {
         M.transpose ((Matrix3x3Block)blks[i]);
         deriv.setSubMatrix (0, i*3, M);         
      }
      return deriv;
   }

   private MatrixNd computeNumericDeriv (ElementConnection econ) {
      FemNode3d[] nodes = econ.myElem.getNodes();
      MatrixNd deriv = new MatrixNd (3, 3*nodes.length);
      Point3d pos0 = new Point3d();
      PolarDecomposition3d polard = new PolarDecomposition3d();
      econ.addPosition (pos0, polard);

      double h = 1e-8;
      for (int i=0; i<nodes.length; i++) {
         Point3d npos0 = new Point3d(nodes[i].getPosition());
         for (int k=0; k<3; k++) {
            Vector3d inc = new Vector3d();
            inc.set (k, h);
            Point3d npos = new Point3d (npos0);
            npos.add (inc);
            nodes[i].setPosition (npos);
            myFem.invalidateElementRotationData();
            Point3d pos = new Point3d();
            econ.addPosition (pos, polard);
            Vector3d del = new Vector3d();
            del.sub (pos, pos0);
            del.scale (1/h);
            deriv.set (0, i*3+k, del.x);
            deriv.set (1, i*3+k, del.y);
            deriv.set (2, i*3+k, del.z);
         }
         nodes[i].setPosition (npos0);
      }
      myFem.invalidateElementRotationData();
      return deriv;
   }

   private Point3d randomPos() {
      Point3d pnt = new Point3d();
      pnt.setRandom();
      return pnt;
   }

   public FemSkinDerivTest() {
      // myNode0 = new FemNode3d(randomPos());
      // myNode1 = new FemNode3d(randomPos());
      // myNode2 = new FemNode3d(randomPos());
      // myNode3 = new FemNode3d(randomPos());
      // nodes = new FemNode3d[] {
      //    myNode0, myNode1, myNode2, myNode2};

      int nelems = 4;
      myFem = FemFactory.createHexGrid (
         null, 1.0, 0.25, 0.25, nelems, nelems/2, nelems/2);
      for (FemNode3d n : myFem.getNodes()) {
         if (n.getPosition().x == -0.5) {
            n.setDynamic (false);
         }
      }
      myFem.setMaterial (new LinearMaterial (50000.0, 0.49));
      myState0 = myFem.createState(null);
      myState1 = myFem.createState(null);
      myFem.getInitialState (myState0, null);
      double h = 0.01;
      for (int i=0; i<50; i++) {
         myFem.preadvance (h*i, h*(i+1), 0);
         myFem.advance (h*i, h*(i+1), 0);
      }
      myFem.getState (myState1);
      setFemState (myState0);
      myFemInfo = new FemModelInfo (myFem);
   }

   private void testDeriv (ElementConnection econ, double tol) {
      MatrixNd D = computeDeriv (econ);
      MatrixNd Dnum = computeNumericDeriv (econ);
      MatrixNd Err = new MatrixNd();
      Err.sub (D, Dnum);
      double err = Err.frobeniusNorm()/Dnum.frobeniusNorm();
      if (err > tol) {
         System.out.println ("D=\n" + D.toString ("%12.8f"));
         System.out.println ("Dnum=\n" + Dnum.toString ("%12.8f"));
         System.out.println (
            "Err=" + err + "\n" + Err.toString ("%12.8f"));
         throw new TestException ("Derivative error=" + err);
      }
   }

   public void testElementR (double x, double y, double z) {
      PointSkinAttachment attachment = new PointSkinAttachment();
      NearestPoint drec = new NearestPoint();
      drec.elem = myFem.getElement(13);
      drec.nearPoint.set (x, y, z);
      Point3d pos = new Point3d (x+0.25, y, z);
      drec.distance = 0.25;
      ElementConnection econ = attachment.addElementConnection (
         pos, drec, myFemInfo, 1.0);

      testDeriv (econ, 3e-7);      
   }

   public void testElementR() {
      testElementR (0.5, 0, 0);
      testElementR (0.5, 0, 0.05);
      testElementR (0.5, -0.05, 0);
      testElementR (0.5, -0.05, 0.05);

      setFemState (myState1);
      testElementR (0.5, 0, 0);
      testElementR (0.5, 0, 0.05);
      testElementR (0.5, -0.05, 0);
      testElementR (0.5, -0.05, 0.05);
      setFemState (myState0);
   }

   public void test() {
      testSpinDeriv();
      testElementR();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      FemSkinDerivTest tester = new FemSkinDerivTest();
      tester.runtest();
   }

}
