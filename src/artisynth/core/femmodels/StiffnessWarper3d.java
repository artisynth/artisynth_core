/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import artisynth.core.materials.FemMaterial;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix6d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

/** 
 * Implements stiffness warping for a particular 3D FEM element.
 *
 * <p>Note: it is important that all these methods are called from the same
 * (simulation) thread. In particular, they should not be called by methods
 * activated by the GUI.
 */
public class StiffnessWarper3d {
 
   // cached linear material info
   LinearMaterialCache linear;
   LinearMaterialCache corotated;
   
   protected RotationMatrix3d R = null;  // warping rotation
   protected int numNodes;

   //   protected Matrix3d J0inv = null;
   //   protected double myConditionNum = 0;
   
   public StiffnessWarper3d (FemElement3d elem) {
      this.numNodes = elem.numNodes();
   }
   
   /**
    * Initializes the warper, clears any cached values
    * @param elem element to be used by warper
    */
   public void initialize(FemElement3d elem) {
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
   public void addInitialStiffness (FemElement3d e, FemMaterial mat) {

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
   public void addInitialStiffness (FemElement3d e, AuxiliaryMaterial mat) {

      LinearMaterialCache cache = null;
      if (mat.isCorotated()) {
         cache = getOrCreateCorotatedCache();
      } else {
         cache = getOrCreateLinearCache();
      }
      
      cache.addInitialStiffness(e, mat);
   }

   
   public void computeWarpingRotation(FemElement3d elem) {
      IntegrationPoint3d wpnt = elem.getWarpingPoint();
      IntegrationData3d wdata = elem.getWarpingData();
      Matrix3d F = new Matrix3d();
      wpnt.computeGradient(F, elem.getNodes(), wdata.myInvJ0);
      computeRotation(F, null);
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
   
   /**
    * Gets initial stiffness contribution from all linear
    * and corotated linear cached materials
    * @param K0 matrix to populate
    * @param i first node index
    * @param j second node index
    */
   public void getInitialStiffness(Matrix3d K0, int i, int j) {
      
      K0.setZero();
      if (linear != null) {
         K0.add(linear.K0[i][j]);
      }
      
      if (corotated != null) {
         K0.add(corotated.K0[i][j]);
      }
   }
   
   /**
    * Gets initial force contribution from all linear
    * and corotated linear cached materials
    * @param f0 force vector to populate
    * @param i first node index
    */
   public void getInitialForce(Vector3d f0, int i) {
      
      f0.setZero();
      if (linear != null) {
         f0.add(linear.f0[i]);
      }
      
      if (corotated != null) {
         f0.add(corotated.f0[i]);
      }
   }
   
}
