/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import artisynth.core.femmodels.AuxiliaryMaterial;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.IntegrationData3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.LinearMaterialCache;
import artisynth.core.femmodels.StiffnessWarper3d;
import artisynth.core.materials.FemMaterial;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

/** 
 * Implements stiffness warping for a particular integration region.
 *
 * <p>Note: it is important that all these methods are called from the same
 * (simulation) thread. In particular, they should not be called by methods
 * activated by the GUI.
 */
public class MFreeStiffnessWarper3d extends StiffnessWarper3d {
 
//   Matrix3d[][] K0;
//   MFreeElement3d myElem;
//   
//   // A and R are used to compute stiffness warping. 
//   protected Matrix3d A = new Matrix3d();
//   protected RotationMatrix3d R = new RotationMatrix3d();
//   protected Vector3d tmp = new Vector3d();
//   protected Vector3d pos = new Vector3d();
//   protected SVDecomposition3d SVD = new SVDecomposition3d();
//
//   protected double myConditionNum = 0;
//
//   public MFreeStiffnessWarper3d (int numNodes) {
//      K0 = new Matrix3d[numNodes][numNodes];
//      
//      for (int i=0; i<numNodes; i++) {
//         for (int j=0; j<numNodes; j++) {
//            K0[i][j] = new Matrix3d();  
//         }
//      }
//   }
//
//   public void computeInitialStiffness (MFreeElement3d e, FemMaterial mat) {
//
//      // reset stiffness and RHS
//      for (int i=0; i<e.myNodes.length; i++) {
//         for (int j=0; j<e.myNodes.length; j++) {
//            K0[i][j].setZero();
//         }
//         // f0[i].setZero();
//      }
//      
//      MFreeIntegrationPoint3d[] ipnts = e.getIntegrationPoints();
//      IntegrationData3d[] idata = e.getIntegrationData();
//      myElem = e;
//      
//      SolidDeformation def = new SolidDeformation();
//      def.setAveragePressure(0);
//      def.setF(Matrix3d.IDENTITY);
//      def.setR(Matrix3d.IDENTITY);
//
//      Matrix6d D = new Matrix6d(); // fill in ...
//      
//      for (int k=0; k<ipnts.length; k++) {
//         MFreeIntegrationPoint3d pt = ipnts[k];
//         IntegrationData3d idat = idata[k];
//         double dv = pt.getWeight()*idat.getDetJ0();
//         Vector3d[] GNx = pt.updateShapeGradient(idat.getInvJ0());
//         
//         Matrix3d Q = idat.getFrame() == null ? Matrix3d.IDENTITY : idat.getFrame();
//         
//         // compute tangent matrix under zero stress
//         mat.computeTangent(D, SymmetricMatrix3d.ZERO, def, Q, null);
//         
//         for (int i=0; i<e.myNodes.length; i++) {
//            for (int j=0; j<e.myNodes.length; j++) {
//               Matrix3d KA = new Matrix3d();
//               
//               FemUtilities.addMaterialStiffness (
//                  KA, GNx[i], D, SymmetricMatrix3d.ZERO, GNx[j], dv);
//               K0[i][j].add(KA);
//               
//            }
//         }
//      }      
//      
//      //      // initial RHS
//      //      Vector3d tmp = new Vector3d();
//      //      for (int i=0; i<e.myNodes.length; i++) {
//      //         tmp.setZero();
//      //         for (int j=0; j<e.myNodes.length; j++) {
//      //            K0[i][j].mulAdd (tmp, e.myNodes[j].getRestPosition(), tmp);
//      //            //             if (e.getNumber() == 0 && i<4 && j<4) {
//      //            //                System.out.println ("K0["+i+"]["+j+"]");
//      //            //                System.out.println (K0[i][j].toString("%10.5f"));
//      //            //             }
//      //         }
//      //         f0[i].set (tmp);
//      //      }
//   }
//   
//   public Matrix3d[][] getInitialStiffnesses() {
//      return K0;
//   }
//   
//   public RotationMatrix3d getRotation() {
//      return R;
//   }
//
//   public void computeRotation (Matrix3d F, SymmetricMatrix3d P) {
//      if (R == null) {
//         R = new RotationMatrix3d();
//      }
//      SVDecomposition3d SVD = new SVDecomposition3d();
//      SVD.polarDecomposition(R, P, F);
//   }
//
//   public void addNodeStiffness(Matrix3d Kij, boolean[][] active, int i, int j, boolean warping) {
//      
//      if (active[i][j]) {
//         if (warping) {
//            A.mulTransposeRight (K0[i][j], R);
//            A.mul (R, A);
//            Kij.add (A);
//         }
//         else {
//            Kij.add (K0[i][j]);
//         }
//      }
//   }
//   
//   public void addNodeStiffness (FemNodeNeighbor nbr, int i, int j, boolean warping) {
//      
//      // if (active[i][j]) {
//         if (warping) {
//            A.mulTransposeRight (K0[i][j], R);
//            A.mul (R, A);
//            nbr.addStiffness (A);
//         }
//         else {
//            
////            String pntStr = "stiff(" + (myElem.myNodes[i].getNumber()+1) + "," + (myElem.myNodes[j].getNumber()+1) + "," + (myElem.getNumber()+1) + ","+
////               (myElem.getGrandParent().getNumber() + 1) + ")";
////            System.out.println(pntStr + ".K = [" + K0[i][j] + "];");
////            System.out.println(pntStr + ".nbr = [" + (myElem.myNodes[i].getNumber()+1) + "," +(nbr.getNode().getNumber()+1) + "];");
//            
//            nbr.addStiffness (K0[i][j]);
//            
//         }
//      // }
//   }
//
//   public void addNodeForce (
//      Vector3d f, int i, MFreeNode3d[] nodes, boolean warping, FemNodeNeighbor[] nbr) {
//
//      if (warping) {
//         tmp.setZero();
//         //R.setIdentity();
//         for (int j=0; j<nodes.length; j++) {
//            // if (active[i][j]) { 
//               R.mulTranspose (pos, nodes[j].getFalsePosition());
//               pos.sub(nodes[j].getRestPosition());
//               // R.mulTranspose (pos, nodes[j].getFalseDisplacement());
//               K0[i][j].mulAdd (tmp, pos, tmp);
//            // }
//         }
//         R.mul (tmp, tmp);
//         f.add (tmp);
//         
//      }
//      else {
//         tmp.setZero();
//         for (int j=0; j<nodes.length; j++) {
//            // if (active[i][j]) {
//               Vector3d tmpF = new Vector3d();
//               nodes[j].getFalseDisplacement(pos);
//               K0[i][j].mulAdd (tmp, pos, tmp);
//               K0[i][j].mul(tmpF,pos);
//               
//            // }
//         }
//         f.add (tmp);
//      }
//   }
//
//   public void setRotation(Matrix3dBase R) {
//      if (this.R == null) {
//         this.R = new RotationMatrix3d();
//      }
//      this.R.set(R);
//   }
   
   
// cached linear material info
   LinearMaterialCache linear;
   LinearMaterialCache corotated;
   
   protected RotationMatrix3d R = null;  // warping rotation
   protected int numNodes;

   //   protected Matrix3d J0inv = null;
   //   protected double myConditionNum = 0;
   
   public MFreeStiffnessWarper3d (MFreeElement3d elem) {
      super(elem);
      this.numNodes = elem.numNodes();
   }
   
   /**
    * Initializes the warper, clears any cached values
    * @param elem element to be used by warper
    */
   public void initialize(MFreeElement3d elem) {
      linear = null;
      corotated = null;      
   }
   
   /**
    * Ensures linear (non-corotated) cache is available, creating if necessary
    * @return linear cache
    */
   LinearMaterialCache getOrCreateLinearCache() {
      if (linear == null) {
         linear = new LinearMaterialCache(numNodes);
      }
      return linear;
   }
   
   /**
    * Ensures corotated cache is available, creating if necessary
    * @return corotated cache
    */
   LinearMaterialCache getOrCreateCorotatedCache() {
      if (corotated == null) {
         corotated = new LinearMaterialCache(numNodes);
      }
      return corotated;
   }
   
   /**
    * Checks if warper contains any cached data
    * @return true if contains cached data, false otherwise
    */
   public boolean isCacheEmpty() {
      return (corotated == null) && (linear == null);
   }
  
   /**
    * Adds linear stiffness contributions to an underlying cache
    * @param e    FEM element
    * @param mat  linear material
    */
   public void addInitialStiffness (MFreeElement3d e, FemMaterial mat) {

      LinearMaterialCache cache = null;
      if (mat.isCorotated()) {
         cache = getOrCreateCorotatedCache();
      } else {
         cache = getOrCreateLinearCache();
      }
      
      cache.addInitialStiffness(e, mat);
   }
   
   /**
    * Adds linear stiffness contributions to an underlying cache
    * @param e    FEM element
    * @param mat  linear material
    */
   public void addInitialStiffness (MFreeElement3d e, AuxiliaryMaterial mat) {

      LinearMaterialCache cache = null;
      if (mat.isCorotated()) {
         cache = getOrCreateCorotatedCache();
      } else {
         cache = getOrCreateLinearCache();
      }
      
      cache.addInitialStiffness(e, mat);
   }

   //   public void setInitialJ (
   //      FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3) {
   //      Vector3d tmp = new Vector3d();
   //      Matrix3d A = new Matrix3d();
   //      tmp.sub (n1.myRest, n0.myRest);
   //      A.setColumn (0, tmp);
   //      tmp.sub (n2.myRest, n0.myRest);
   //      A.setColumn (1, tmp);
   //      tmp.sub (n3.myRest, n0.myRest);
   //      A.setColumn (2, tmp);
   //
   //      J0inv = new Matrix3d();
   //      J0inv.invert (A);
   //      myConditionNum = A.infinityNorm() * J0inv.infinityNorm();
   //   }
   //
   //   /**
   //    * 
   //    * @return
   //    */
   //   public double getConditionNum () {
   //      return myConditionNum;
   //   }
   //
   //   public void computeWarping (
   //      FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3) {
   //      Vector3d tmp = new Vector3d();
   //      Matrix3d A = new Matrix3d();
   //      tmp.sub (n1.getLocalPosition(), n0.getLocalPosition());
   //      A.setColumn (0, tmp);
   //      tmp.sub (n2.getLocalPosition(), n0.getLocalPosition());
   //      A.setColumn (1, tmp);
   //      tmp.sub (n3.getLocalPosition(), n0.getLocalPosition());
   //      A.setColumn (2, tmp);
   //
   //      A.mul (J0inv);
   //      computeRotation (A, null);
   //   }
   
   public void computeWarpingRotation(MFreeElement3d elem) {
      IntegrationPoint3d wpnt = elem.getWarpingPoint();
      IntegrationData3d wdata = elem.getWarpingData();
      wpnt.computeJacobianAndGradient(elem.getNodes(), wdata.getInvJ0());
      computeRotation(wpnt.getF(), null);
   }

   /**
    * Computes a corotated rotation based on the deformation gradient
    * @param F deformation gradient
    * @param P symmetric part of gradient after rotation
    */
   protected void computeRotation (Matrix3d F, SymmetricMatrix3d P) {
      if (R == null) {
         R = new RotationMatrix3d();
      }
      SVDecomposition3d SVD = new SVDecomposition3d();
      SVD.polarDecomposition(R, P, F);
   }
   
   /**
    * Manually sets rotation to use for warping
    * @param R warping rotation
    */
   public void setRotation(Matrix3dBase R) {
      if (this.R == null) {
         this.R = new RotationMatrix3d();
      }
      this.R.set(R);
   }
   
   /**
    * Gets current warping rotation matrix
    * @return warping rotation
    */
   public RotationMatrix3d getRotation() {
      return R;
   }

   //   /**
   //    * Computes F = RP, R a rotation matrix, P a symmetric matrix
   //    * @param F matrix to decompose
   //    * @param R rotational component
   //    * @param P symmetric component
   //    */
   //   public static void computeRotation (Matrix3d F, Matrix3d R, SymmetricMatrix3d P) {
   //      SVDecomposition3d SVD = new SVDecomposition3d();
   //      try {
   //         SVD.factor (F);
   //      }
   //      catch (Exception e) {
   //         System.out.println ("F=\n" + F.toString ("%g"));
   //         R.setIdentity();
   //      }
   //      
   //      Matrix3d U = SVD.getU();
   //      Matrix3d V = SVD.getV();
   //      Vector3d s = new Vector3d();
   //      SVD.getS(s);
   //
   //      double detU = U.orthogonalDeterminant();
   //      double detV = V.orthogonalDeterminant();
   //      if (detV * detU < 0) { /* then one is negative and the other positive */
   //         if (detV < 0) { /* negative last column of V */
   //            V.m02 = -V.m02;
   //            V.m12 = -V.m12;
   //            V.m22 = -V.m22;
   //         }
   //         else /* detU < 0 */
   //         { /* negative last column of U */
   //            U.m02 = -U.m02;
   //            U.m12 = -U.m12;
   //            U.m22 = -U.m22;
   //         }
   //         s.z = -s.z;
   //      }
   //      R.mulTransposeRight (U, V);
   //      if (P != null) {
   //         // place the symmetric part in P
   //         P.mulDiagTransposeRight (V, s);
   //      }
   //   }
   //
   //   public void addNodeStiffness (Matrix3d Kij, int i, int j) {
   //      
   //      // corotated component
   //      if (K0corotated != null) {
   //         Matrix3d A = new Matrix3d();
   //         A.mulTransposeRight (K0corotated[i][j], R);
   //         A.mul (R, A);
   //         Kij.add (A);
   //      }
   //      
   //      // linear component
   //      if (K0 != null) {
   //         Kij.add (K0[i][j]);
   //      }
   //   }

   
   /**
    * Rotates a 3x3 stiffness block
    * @param Krot rotated stiffness
    * @param K initial stiffness
    */
   protected void rotateStiffness(Matrix3d Krot, Matrix3d K) {
      Krot.mulTransposeRight(K, R);
      Krot.mul(R, Krot);
   }
   
   /**
    * Adds the total stiffness contributions between nodes i and j
    * from all cached linear and corotated linear materials to the given 
    * matrix
    * 
    * @param K local stiffness matrix
    * @param i first node index
    * @param j second node index
    */
   public void addNodeStiffness (Matrix3d K, int i, int j) {
     
      // corotated component
      if (corotated != null) {
         Matrix3d Kr = new Matrix3d();
         rotateStiffness(Kr, corotated.getInitialStiffness(i, j));
         K.add(Kr);
      }
      
      // linear component
      if (linear != null) {
         K.add (linear.getInitialStiffness(i, j));
      }
   }

   /**
    * Adds the total force contribution due to stiffness from all
    * cached linear materials for node i
    *  
    * @param f output force
    * @param i node index
    * @param nodes element nodes
    */
   public void addNodeForce (
      Vector3d f, int i, FemNode3d[] nodes) {
      
      // corotated
      if (corotated != null) {
         Vector3d tmp = new Vector3d();
         Vector3d pos = new Vector3d();
         for (int j=0; j<nodes.length; j++) {
            // rotate position
            R.mulTranspose (pos, nodes[j].getLocalPosition());
            corotated.getInitialStiffness(i, j).mulAdd (tmp, pos, tmp);
         }
         // subtract f0
         tmp.sub (corotated.getInitialForce(i));
         // rotate back
         R.mul (tmp, tmp);
         // add force
         f.add (tmp);
      }
      
      // linear
      if (linear != null) {
         Vector3d tmp = new Vector3d();
         for (int j=0; j<nodes.length; j++) {
            linear.getInitialStiffness(i, j).mulAdd (tmp, nodes[j].getLocalPosition(), tmp);
         }
         tmp.sub (linear.getInitialForce(i));
         f.add (tmp);
      }
   }
   
   
   // required for static analysis
   /**
    * Adds initial force contribution due to cached stiffness
    * 
    * @param f output force vector
    * @param offset offset within force vector to add contribution
    * @param i node for which to add force
    */
   public void addNodeForce0(VectorNd f, int offset, int i) {
      
      // corotated
      if (corotated != null) {
         Vector3d tmp = new Vector3d();
         R.mul(tmp, corotated.getInitialForce(i));
         f.set(offset, f.get(offset) + tmp.x);
         f.set(offset+1, f.get(offset+1) + tmp.y);
         f.set(offset+2, f.get(offset+2) + tmp.z);
      }
      
      // linear
      if (linear != null) {
         Vector3d f0 = linear.getInitialForce(i);
         f.set(offset, f.get(offset) + f0.x);
         f.set(offset+1, f.get(offset+1) + f0.y);
         f.set(offset+2, f.get(offset+2) + f0.z);
      }
   }

}
