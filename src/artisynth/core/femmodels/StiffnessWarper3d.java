/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import artisynth.core.materials.FemMaterial;
import artisynth.core.femmodels.FemElement.ElementClass;
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
   protected LinearMaterialCache linear;
   protected LinearMaterialCache corotated;
   protected ElementClass elemClass;
   
   protected RotationMatrix3d R = null;  // warping rotation
   protected int numNodes;

   //   protected Matrix3d J0inv = null;
   //   protected double myConditionNum = 0;
   
   public StiffnessWarper3d (FemElement3dBase elem) {
      this.numNodes = elem.numNodes();
      this.elemClass = elem.getElementClass();
   }
   
   /**
    * Initializes the warper, clears any cached values
    * @param elem element to be used by warper
    */
   public void initialize(FemElement3dBase elem) {
      linear = null;
      corotated = null;      
   }
   
   /**
    * Ensures linear (non-corotated) cache is available, creating if necessary
    * @param e element for which the cache is created
    * @return linear cache
    */
   protected LinearMaterialCache getOrCreateLinearCache (FemElement3dBase e) {
      if (linear == null) {
         linear = new LinearMaterialCache(e);
      }
      return linear;
   }
   
   /**
    * Ensures corotated cache is available, creating if necessary
    * @param e element for which the cache is created
    * @return corotated cache
    */
   protected LinearMaterialCache getOrCreateCorotatedCache (FemElement3dBase e) {
      if (corotated == null) {
         corotated = new LinearMaterialCache(e);
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
    * @param weight weight to combine with integration point weights
    */
   public void addInitialStiffness (
      FemElement3dBase e, FemMaterial mat, double weight) {

      LinearMaterialCache cache = null;
      if (mat.isCorotated()) {
         cache = getOrCreateCorotatedCache(e);
      } else {
         cache = getOrCreateLinearCache(e);
      }
      if (e instanceof FemElement3d) {
         cache.addInitialStiffness((FemElement3d)e, mat, weight);
      }
      else if (e instanceof ShellElement3d) {
         cache.addInitialStiffness((ShellElement3d)e, mat, weight);
      }
      else {
         throw new UnsupportedOperationException (
            "FemElement type "+e.getClass()+" not supported");
      }
   }
   
   /**
    * Adds linear stiffness contributions to an underlying cache
    * @param e    FEM element
    * @param mat  linear material
    * @param weight weight to combine with integration point weights
    */
   public void addInitialStiffness (
      FemElement3dBase e, AuxiliaryMaterial mat, double weight) {

      LinearMaterialCache cache = null;
      if (mat.isCorotated()) {
         cache = getOrCreateCorotatedCache(e);
      } else {
         cache = getOrCreateLinearCache(e);
      }
      if (e instanceof FemElement3d) {
         cache.addInitialStiffness((FemElement3d)e, mat, weight);
      }
      else if (e instanceof ShellElement3d) {
         cache.addInitialStiffness((ShellElement3d)e, mat, weight);
      }
      else {
         throw new UnsupportedOperationException (
            "FemElement type "+e.getClass()+" not supported");
      }
   }

   
   public void computeWarpingRotation (FemElement3dBase elem) {
      IntegrationPoint3d wpnt = elem.getWarpingPoint();
      IntegrationData3d wdata = elem.getWarpingData();
      Matrix3d F = new Matrix3d();
      wpnt.computeGradient(F, elem.getNodes(), wdata.myInvJ0);
      computeRotation(F, null);
   }

   /**
    * Computes a corotated rotation based on the deformation gradient
    * and stores the result in this warper

    * @param F deformation gradient
    * @param P symmetric part of gradient after rotation
    */
   protected void computeRotation (Matrix3d F, SymmetricMatrix3d P) {
      if (R == null) {
         R = new RotationMatrix3d();
      }
      computeRotation (R, P, F);
   }
   
   /**
    * Computes a corotated rotation based on the deformation gradient
    * @param R returns the rotation
    * @param F deformation gradient
    * @param P symmetric part of gradient after rotation
    */
   public static void computeRotation (
      RotationMatrix3d R, SymmetricMatrix3d P, Matrix3d F) {
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
   protected void rotateStiffness (Matrix3d Krot, Matrix3d K) {
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
         rotateStiffness(Kr, corotated.getInitialStiffness00(i, j));
         K.add(Kr);
      }
      
      // linear component
      if (linear != null) {
         K.add (linear.getInitialStiffness00(i, j));
      }
   }

   public void addNodeStiffness (FemNodeNeighbor nbr, int i, int j) {
      
      // corotated component
      if (corotated != null) {
         Matrix3d Kr = new Matrix3d();
         Kr.transform (R, corotated.getInitialStiffness00(i,j));
         nbr.myK00.add(Kr);
         if (corotated.hasShellData()) {
            Kr.transform (R, corotated.getInitialStiffness01(i,j));
            nbr.myK01.add(Kr);
            Kr.transform (R, corotated.getInitialStiffness10(i,j));
            nbr.myK10.add(Kr);
            Kr.transform (R, corotated.getInitialStiffness11(i,j));
            nbr.myK11.add(Kr);
         }
      }
      
      // linear component
      if (linear != null) {
         nbr.myK00.add (linear.getInitialStiffness00(i, j));
         if (linear.hasShellData()) {
            nbr.myK01.add (linear.getInitialStiffness01(i, j));
            nbr.myK10.add (linear.getInitialStiffness10(i, j));
            nbr.myK11.add (linear.getInitialStiffness11(i, j));
         }
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
            corotated.getInitialStiffness00(i, j).mulAdd (tmp, pos, tmp);
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
            linear.getInitialStiffness00(i, j).mulAdd (
               tmp, nodes[j].getLocalPosition(), tmp);
         }
         tmp.sub (linear.getInitialForce(i));
         f.add (tmp);
      }
   }


   public void addNodeForce (
      Vector3d f, Vector3d fback, int i, FemNode3d[] nodes) {
      
      // corotated
      if (corotated != null) {
         Vector3d tmp0 = new Vector3d();
         Vector3d tmp1 = new Vector3d();
         Vector3d posx = new Vector3d();
         Vector3d posy = new Vector3d();

         for (int j=0; j<nodes.length; j++) {
            // rotate position and dir
            R.mulTranspose (posx, nodes[j].getPosition());
            R.mulTranspose (posy, nodes[j].getBackPosition());
            corotated.mulAddK (i, j, tmp0, tmp1, posx, posy);
         }   
         tmp0.sub (corotated.getInitialForce(i));
         R.mul (tmp0, tmp0);
         f.add (tmp0);
         tmp1.sub (corotated.getInitialBackForce(i));
         R.mul (tmp1, tmp1);
         fback.add (tmp1);        
      }
      
      // linear
      if (linear != null) {
         Vector3d tmp0 = new Vector3d();
         Vector3d tmp1 = new Vector3d();
         for (int j=0; j<nodes.length; j++) {
            Vector3d posx = nodes[j].getPosition();
            Vector3d posy = nodes[j].getBackPosition();
            linear.mulAddK (i, j, tmp0, tmp1, posx, posy);
         }
         tmp0.sub (linear.getInitialForce(i));
         f.add (tmp0);
         tmp1.sub (linear.getInitialBackForce(i));
         fback.add (tmp1);
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
   
   // /**
   //  * Gets initial stiffness contribution from all linear
   //  * and corotated linear cached materials
   //  * @param K0 matrix to populate
   //  * @param i first node index
   //  * @param j second node index
   //  */
   // public void getInitialStiffness (Matrix3d K0, int i, int j) {
      
   //    K0.setZero();
   //    if (linear != null) {
   //       K0.add(linear.K0[i][j]);
   //    }
      
   //    if (corotated != null) {
   //       K0.add(corotated.K0[i][j]);
   //    }
   // }
   
   // /**
   //  * Gets initial force contribution from all linear
   //  * and corotated linear cached materials
   //  * @param f0 force vector to populate
   //  * @param i first node index
   //  */
   // public void getInitialForce (Vector3d f0, int i) {
      
   //    f0.setZero();
   //    if (linear != null) {
   //       f0.add(linear.f0[i]);
   //    }
      
   //    if (corotated != null) {
   //       f0.add(corotated.f0[i]);
   //    }
   // }
   
}
