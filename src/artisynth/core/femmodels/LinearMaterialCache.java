package artisynth.core.femmodels;

import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.SolidDeformation;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;

/**
 * Cached stiffness and initial force terms for faster
 * linear material computations.
 */
public class LinearMaterialCache {

   // non-corotated terms
   Matrix3d[][] K0;
   Vector3d[] f0;

   /**
    * Initializes empty cache
    * @param numNodes number of nodes in associated element
    */
   public LinearMaterialCache (int numNodes) {
      // create K0
      K0 = new Matrix3d[numNodes][numNodes];
      for (int i=0; i<numNodes; i++) {
         for (int j=0; j<numNodes; j++) {
            K0[i][j] = new Matrix3d();
         }
      }
      
      // create f0
      f0 = new Vector3d[numNodes];
      for (int i=0; i<numNodes; i++) {
         f0[i] = new Vector3d();
      }
   }
   
   /**
    * Sets all stiffness and force values to zero
    */
   public void clearInitialStiffness() {
      for (int i=0; i<K0.length; i++) {
         for (int j=0; j<K0[i].length; j++) {
            K0[i][j].setZero();
         }
      }
      
      for (int i=0; i<f0.length; ++i) {
         f0[i].setZero();
      }
   }
   
   /**
    * Computes and stores the initial stiffness K0 and force f0 terms
    * @param e   element
    * @param mat linear material
    */
   public void addInitialStiffness (FemElement3d e, FemMaterial mat) {
      
      SolidDeformation def = new SolidDeformation();
      def.setAveragePressure(0);
      def.setF(Matrix3d.IDENTITY);
      def.setR(Matrix3d.IDENTITY);
      
      // compute stiffness matrix
      Matrix6d D = new Matrix6d();
      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
      IntegrationData3d[] idata = e.getIntegrationData();
      for (int k=0; k<ipnts.length; k++) {

         IntegrationPoint3d pt = ipnts[k];
         IntegrationData3d dt = idata[k];
                  
         double dv0 = dt.myDetJ0*pt.getWeight();
         if (dt.myScaling != 1) {
            dv0 *= dt.myScaling;
         }

         Matrix3d Q = dt.myFrame == null ? Matrix3d.IDENTITY : dt.myFrame;
         Vector3d[] GNx0 = pt.updateShapeGradient(dt.myInvJ0);

         // compute tangent matrix under zero stress
         mat.computeTangent(D, SymmetricMatrix3d.ZERO, def, Q, null);
         
         FemNode3d[] nodes = e.getNodes();
         for (int i = 0; i < nodes.length; i++) {
            for (int j = 0; j < nodes.length; j++) {
               FemUtilities.addMaterialStiffness (
                  K0[i][j], GNx0[i], D, GNx0[j], dv0);
            }
         }
      }      
      
      // initial RHS
      Vector3d tmp = new Vector3d();
      FemNode3d[] nodes = e.getNodes();
      for (int i = 0; i < nodes.length; i++) {
         tmp.setZero();
         for (int j=0; j<nodes.length; j++) {
            K0[i][j].mulAdd (tmp, nodes[j].getRestPosition(), tmp);
         }
         f0[i].set (tmp);
      }
   }
   
   /**
    * Computes and stores the initial stiffness K0 and force f0 terms
    * @param e   element
    * @param mat linear material
    */
   public void addInitialStiffness (FemElement3d e, AuxiliaryMaterial mat) {
      
      SolidDeformation def = new SolidDeformation();
      def.setAveragePressure(0);
      def.setF(Matrix3d.IDENTITY);
      def.setR(Matrix3d.IDENTITY);
      
      // compute stiffness matrix
      Matrix6d D = new Matrix6d();
      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
      IntegrationData3d[] idata = e.getIntegrationData();
      for (int k=0; k<ipnts.length; k++) {

         IntegrationPoint3d pt = ipnts[k];
         IntegrationData3d dt = idata[k];
                  
         double dv0 = dt.myDetJ0*pt.getWeight();
         if (dt.myScaling != 1) {
            dv0 *= dt.myScaling;
         }

         Vector3d[] GNx0 = pt.updateShapeGradient(dt.myInvJ0);

         // compute tangent matrix under zero stress
         mat.computeTangent(D, SymmetricMatrix3d.ZERO, def, pt, dt, null);
         
         FemNode3d[] nodes = e.getNodes();
         for (int i = 0; i < nodes.length; i++) {
            for (int j = 0; j < nodes.length; j++) {
               FemUtilities.addMaterialStiffness (
                  K0[i][j], GNx0[i], D, GNx0[j], dv0);
            }
         }
      }      
      
      // initial RHS
      Vector3d tmp = new Vector3d();
      FemNode3d[] nodes = e.getNodes();
      for (int i = 0; i < nodes.length; i++) {
         tmp.setZero();
         for (int j=0; j<nodes.length; j++) {
            K0[i][j].mulAdd (tmp, nodes[j].getRestPosition(), tmp);
         }
         f0[i].set (tmp);
      }
   }
  
   /**
    * Retrieves the local stiffness contribution between nodes i and j
    * @param i first node index
    * @param j second node index
    * @return local stiffness contribution
    */
   public Matrix3d getInitialStiffness(int i, int j) {
      return K0[i][j];
   }
   
   /**
    * Retrieves all local stiffness contributions
    * @return K0
    */
   public Matrix3d[][] getInitialStiffness() {
      return K0;
   }
   
   /**
    * Retrieves the stiffness-induced initial force on node i
    * @param i node index
    * @return force vector
    */
   public Vector3d getInitialForce(int i) {
      return f0[i];
   }
   
   /**
    * Retries all stiffness-induced initial forces
    * @return f0
    */
   public Vector3d[] getInitialForces() {
      return f0;
   }
   
   public boolean equals(LinearMaterialCache cache) {
      
      if (cache.K0.length != K0.length) {
         return false;
      }
      
      for (int i=0; i<K0.length; ++i) {
         if (cache.K0[i].length != K0[i].length) {
            return false;
         }
         
         for (int j=0; j<K0[i].length; ++j) {
            if (!K0[i][j].equals(cache.K0[i][j])) {
               return false;
            }
         }
      }
      
      if (cache.f0.length != f0.length) {
         return false;
      }
      
      for (int i=0; i<f0.length; ++i) {
         if (!f0[i].equals(cache.f0[i])) {
            return false;
         }
      }
      
      return true;
   }
   
   public boolean epsilonEquals(LinearMaterialCache cache, double eps) {
      
      if (cache.K0.length != K0.length) {
         return false;
      }
      
      for (int i=0; i<K0.length; ++i) {
         if (cache.K0[i].length != K0[i].length) {
            return false;
         }
         
         for (int j=0; j<K0[i].length; ++j) {
            if (!K0[i][j].epsilonEquals(cache.K0[i][j], eps)) {
               return false;
            }
         }
      }
      
      if (cache.f0.length != f0.length) {
         return false;
      }
      
      for (int i=0; i<f0.length; ++i) {
         if (!f0[i].epsilonEquals(cache.f0[i], eps)) {
            return false;
         }
      }
      
      return true;
   }
   
   public double maxDistance(LinearMaterialCache cache) {
         
      double maxdist = 0;
      Matrix3d diff = new Matrix3d();
      Vector3d dvec = new Vector3d();
      
      if (cache.K0.length != K0.length) {
         return Double.POSITIVE_INFINITY;
      }

      for (int i=0; i<K0.length; ++i) {
         if (cache.K0[i].length != K0[i].length) {
            return Double.POSITIVE_INFINITY;
         }

         for (int j=0; j<K0[i].length; ++j) {
            diff.sub(K0[i][j], cache.K0[i][j]);
            double dist = diff.maxNorm();
            if (dist > maxdist) {
               maxdist = dist;
            }
         }
      }

      if (cache.f0.length != f0.length) {
         return Double.POSITIVE_INFINITY;
      }

      for (int i=0; i<f0.length; ++i) {
         dvec.sub(f0[i], cache.f0[i]);
         double dist = dvec.oneNorm();
         if (dist > maxdist) {
            maxdist = dist;
         }
      }

      return maxdist;
   }
  
}
