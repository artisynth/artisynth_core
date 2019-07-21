package artisynth.core.femmodels;

import artisynth.core.materials.FemMaterial;
import artisynth.core.femmodels.FemElement.ElementClass;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

/**
 * Cached stiffness and initial force terms for faster
 * linear material computations.
 */
public class LinearMaterialCache {

   // non-corotated terms
   protected Matrix3d[] K00; // stiffness
   protected Vector3d[] f0;  // force
   
   // extra terms required for shell elements - stiffness and forces
   // associated with the backNodes used to implement directors.
   protected Matrix3d[] K01;
   protected Matrix3d[] K10;
   protected Matrix3d[] K11;

   protected Vector3d[] f1;
   protected int nnodes;

   protected FemDeformedPoint createDeformedPoint() {
      return new FemDeformedPoint();
   }

   /**
    * Initializes empty cache
    */
   public LinearMaterialCache (FemElement3dBase e) {
      ensureCapacity (e);
   }

   public boolean hasShellData() {
      return K10 != null;
   }

   private Matrix3d[] allocK (int n) {
      Matrix3d[] K = new Matrix3d[n*n];
      for (int i=0; i<n*n; i++) {
         K[i] = new Matrix3d();
      }
      return K;
   }

   private Vector3d[] allocF (int n) {
      Vector3d[] f = new Vector3d[n];
      for (int i=0; i<n; i++) {
         f[i] = new Vector3d();
      }
      return f;
   }

   private void ensureCapacity (FemElement3dBase e) {
      int n = e.numNodes();
      if (n != nnodes ||
          (e.getElementClass() == ElementClass.SHELL) != (hasShellData())) {
         // allocate K00 and f0
         K00 = allocK (n);
         f0 = allocF (n);
         // allocate K01, K10, K11 and f1 for shell elements
         if (e.getElementClass() == ElementClass.SHELL) {
            K01 = allocK (n);
            K10 = allocK (n);
            K11 = allocK (n);
            f1 = allocF (n);
         }
         else {
            K01 = null;
            K10 = null;
            K11 = null;
            f1 = null;
         }
         nnodes = n;
      }
   }      
   
   /**
    * Sets all stiffness and force values to zero
    */
   public void clearInitialStiffness() {
      for (int i=0; i<nnodes*nnodes; i++) {
         K00[i].setZero();
      }
      for (int i=0; i<nnodes; ++i) {
         f0[i].setZero();
      }
      if (hasShellData()) {
         for (int i=0; i<nnodes*nnodes; i++) {
            K01[i].setZero();
            K10[i].setZero();
            K11[i].setZero();
         }
         for (int i=0; i<nnodes; ++i) {
            f1[i].setZero();
         }
      }
   }
   
   /**
    * Computes and stores the initial stiffness K00 and force f0 terms
    * @param e   element
    * @param mat linear material
    * @param weight weight to combine with integration point weights
    */
   public void addInitialStiffness (
      FemElement3d e, FemMaterial mat, double weight) {
      
      FemDeformedPoint dpnt = createDeformedPoint();
      FemNode3d[] nodes = e.getNodes();

      // compute stiffness matrix
      Matrix6d D = new Matrix6d();
      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
      IntegrationData3d[] idata = e.getIntegrationData();
      for (int k=0; k<ipnts.length; k++) {

         IntegrationPoint3d pt = ipnts[k];
         IntegrationData3d dt = idata[k];
         RotationMatrix3d R = null; // used if element has prestrain
         
         dpnt.setFromRestPoint (pt, dt, RotationMatrix3d.IDENTITY, e, k);

         double dv0 = dt.myDetJ0*weight*pt.getWeight();

         Matrix3d Q = dt.myFrame == null ? Matrix3d.IDENTITY : dt.myFrame;
         Vector3d[] GNx0 = pt.updateShapeGradient(dt.myInvJ0);

         // compute tangent matrix under zero stress
         SymmetricMatrix3d stress = new SymmetricMatrix3d();
         mat.computeStressAndTangent (stress, D, dpnt, Q, 0.0, null);
         for (int i = 0; i < nodes.length; i++) {
            // normally stress will be zero, unless there is prestrain ...
            FemUtilities.addStressForce(f0[i], GNx0[i], stress, dv0);
            for (int j = 0; j < nodes.length; j++) {
               FemUtilities.addMaterialStiffness (
                  K00[i*nnodes+j], GNx0[i], D, GNx0[j], dv0);
               // XXX adding geometric stiffness makes things unstable - need to check why
               //FemUtilities.addGeometricStiffness (
               //   K00[i*nnodes+j], GNx0[i], stress, GNx0[j], dv0);   
            }
         }
      }      
      
      // initial RHS
      Vector3d tmp = new Vector3d();
      for (int i = 0; i < nodes.length; i++) {
         tmp.setZero();
         for (int j=0; j<nodes.length; j++) {
            K00[i*nnodes+j].mulAdd (tmp, nodes[j].getLocalRestPosition(), tmp);
         }
         f0[i].sub (tmp, f0[i]);
      }
   }
   
   /**
    * Computes and stores the initial stiffness K00 and force f0 terms
    * @param e   element
    * @param mat linear material
    * @param weight weight to combine with integration point weights
    */
   public void addInitialStiffness (
      ShellElement3d e, FemMaterial mat, double weight) {

      if (e.getElementClass() == ElementClass.SHELL) {
         addInitialShellStiffness (e, mat, weight);
      }
      else {
         addInitialMembraneStiffness (e, mat, weight);
      }
   }

   public void addInitialShellStiffness (
      ShellElement3d e, FemMaterial mat, double weight) {
      
      FemDeformedPoint dpnt = createDeformedPoint();
      FemNode3d[] nodes = e.getNodes();
      
      // compute stiffness matrix
      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d stress = new SymmetricMatrix3d();
      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
      IntegrationData3d[] idata = e.getIntegrationData();

      int nump = e.numPlanarIntegrationPoints();
      for (int k=0; k<ipnts.length; k++) {

         IntegrationPoint3d pt = ipnts[k];
         IntegrationData3d dt = idata[k];
         RotationMatrix3d R = null; // used if element has prestrain
         
         dpnt.setFromRestPoint (pt, dt, RotationMatrix3d.IDENTITY, e, k%nump);

         double dv0 = dt.myDetJ0*weight*pt.getWeight();
         double t = pt.getCoords().z;

         Matrix3d Q = dt.myFrame == null ? Matrix3d.IDENTITY : dt.myFrame;

         mat.computeStressAndTangent (stress, D, dpnt, Q, 0.0, null);
         VectorNd Ns = pt.getShapeWeights ();
         Vector3d[] dNs = pt.getGNs();
         for (int i = 0; i < nodes.length; i++) {
            // normally stress will be zero, unless there is prestrain ...
            double iN = Ns.get(i);
            Vector3d idN = dNs[i];
            FemUtilities.addShellStressForce(
               f0[i], f1[i], stress, t, dv0, iN, idN.x, idN.y, dt.getInvJ0());
            for (int j = 0; j < nodes.length; j++) {
               double jN = Ns.get(j);
               Vector3d jdN = dNs[j];
               // XXX should presumably use stress instead of
               // SymmetricMatrix3d.ZERO, but results are unstable
               FemUtilities.addShellMaterialStiffness (
                  K00[i*nnodes+j], K01[i*nnodes+j],
                  K10[i*nnodes+j], K11[i*nnodes+j],
                  iN, jN, idN, jdN, dv0, t,
                  dt.getInvJ0(), SymmetricMatrix3d.ZERO, D);
            }
         }
      }      
      
      // initial RHS
      Vector3d tmp0 = new Vector3d();
      Vector3d tmp1 = new Vector3d();
      for (int i = 0; i < nodes.length; i++) {
         tmp0.setZero();
         tmp1.setZero();
         for (int j=0; j<nodes.length; j++) {
            Vector3d pos = nodes[j].getRestPosition();
            Vector3d backPos = nodes[j].getBackRestPosition();
            mulAddK (i, j, tmp0, tmp1, pos, backPos);
         }
         f0[i].sub (tmp0, f0[i]);
         f1[i].sub (tmp1, f1[i]);
      }
   }

   public void addInitialMembraneStiffness (
      ShellElement3d e, FemMaterial mat, double weight) {
      
      FemDeformedPoint dpnt = createDeformedPoint();
      FemNode3d[] nodes = e.getNodes();
      
      // compute stiffness matrix
      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d stress = new SymmetricMatrix3d();
      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
      IntegrationData3d[] idata = e.getIntegrationData();
      int nump = e.numPlanarIntegrationPoints();

      for (int k=0; k<nump; k++) {

         IntegrationPoint3d pt = ipnts[k];
         IntegrationData3d dt = idata[k];
         RotationMatrix3d R = null; // used if element has prestrain
         
         dpnt.setFromRestPoint (pt, dt, RotationMatrix3d.IDENTITY, e, k);

         double dv0 = e.getDefaultThickness()*dt.myDetJ0*weight*pt.getWeight();

         Matrix3d Q = dt.myFrame == null ? Matrix3d.IDENTITY : dt.myFrame;

         mat.computeStressAndTangent (stress, D, dpnt, Q, 0.0, null);
         Vector3d[] dNs = pt.getGNs();
         for (int i = 0; i < nodes.length; i++) {
            // normally stress will be zero, unless there is prestrain ...
            Vector3d idN = dNs[i];
            FemUtilities.addMembraneStressForce(
               f0[i], stress, dv0, idN.x, idN.y, dt.getInvJ0());
            for (int j = 0; j < nodes.length; j++) {
               Vector3d jdN = dNs[j];
               // XXX should presumably use stress instead of
               // SymmetricMatrix3d.ZERO, but results are unstable
               FemUtilities.addMembraneMaterialStiffness (
                  K00[i*nnodes+j], idN, jdN, dv0,
                  dt.getInvJ0(), SymmetricMatrix3d.ZERO, D);
            }
         }
      }      
      
      // initial RHS
      Vector3d tmp = new Vector3d();
      for (int i = 0; i < nodes.length; i++) {
         tmp.setZero();
         for (int j=0; j<nodes.length; j++) {
            K00[i*nnodes+j].mulAdd (tmp, nodes[j].getLocalRestPosition(), tmp);
         }
         f0[i].sub (tmp, f0[i]);
      }
   }
   
   /**
    * Computes and stores the initial stiffness K00 and force f0 terms
    * @param e   element
    * @param mat linear material
    * @param weight weight to combine with integration point weights
    */
   public void addInitialStiffness (
      FemElement3d e, AuxiliaryMaterial mat, double weight) {

      FemDeformedPoint dpnt = createDeformedPoint();
      FemNode3d[] nodes = e.getNodes();

      // compute stiffness matrix
      Matrix6d D = new Matrix6d();
      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
      IntegrationData3d[] idata = e.getIntegrationData();
      for (int k=0; k<ipnts.length; k++) {

         IntegrationPoint3d pt = ipnts[k];
         IntegrationData3d dt = idata[k];
         RotationMatrix3d R = null; // used if element has prestrain
                  
         dpnt.setFromRestPoint (pt, dt, RotationMatrix3d.IDENTITY, e, k);
         
         double dv0 = dt.myDetJ0*weight*pt.getWeight();

         Vector3d[] GNx0 = pt.updateShapeGradient(dt.myInvJ0);

         // compute tangent matrix under zero stress
         SymmetricMatrix3d stress = new SymmetricMatrix3d();
         mat.computeStressAndTangent(stress, D, dpnt, pt, dt, null);
         for (int i = 0; i < nodes.length; i++) {
            // normally stress will be zero, unless there is prestrain ...
            FemUtilities.addStressForce(f0[i], GNx0[i], stress, dv0);
            for (int j = 0; j < nodes.length; j++) {
               FemUtilities.addMaterialStiffness (
                  K00[i*nnodes+j], GNx0[i], D, GNx0[j], dv0);
               // XXX adding geometric stiffness makes things unstable - need to check why
               //FemUtilities.addGeometricStiffness (
               //   K00[i][j], GNx0[i], stress, GNx0[j], dv0);   
            }
         }
      }      
      
      // initial RHS
      Vector3d tmp = new Vector3d();
      for (int i = 0; i < nodes.length; i++) {
         tmp.setZero();
         for (int j=0; j<nodes.length; j++) {
            K00[i*nnodes+j].mulAdd (tmp, nodes[j].getLocalRestPosition(), tmp);
         }
         f0[i].sub (tmp, f0[i]);
      }
   }
  
   /**
    * Computes and stores the initial stiffness K00 and force f0 terms
    * @param e   element
    * @param mat linear material
    * @param weight weight to combine with integration point weights
    */
   public void addInitialStiffness (
      ShellElement3d e, AuxiliaryMaterial mat, double weight) {

      if (e.getElementClass() == ElementClass.SHELL) {
         addInitialShellStiffness (e, mat, weight);
      }
      else {
         addInitialMembraneStiffness (e, mat, weight);
      }
   }

   public void addInitialShellStiffness (
      ShellElement3d e, AuxiliaryMaterial mat, double weight) {
      
      FemDeformedPoint dpnt = createDeformedPoint();
      FemNode3d[] nodes = e.getNodes();
      
      // compute stiffness matrix
      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d stress = new SymmetricMatrix3d();
      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
      IntegrationData3d[] idata = e.getIntegrationData();

      int nump = e.numPlanarIntegrationPoints();
      for (int k=0; k<ipnts.length; k++) {

         IntegrationPoint3d pt = ipnts[k];
         IntegrationData3d dt = idata[k];
         RotationMatrix3d R = null; // used if element has prestrain
         
         dpnt.setFromRestPoint (pt, dt, RotationMatrix3d.IDENTITY, e, k%nump);

         double dv0 = dt.myDetJ0*weight*pt.getWeight();
         double t = pt.getCoords().z;

         mat.computeStressAndTangent (stress, D, dpnt, pt, dt, null);
         VectorNd Ns = pt.getShapeWeights ();
         Vector3d[] dNs = pt.getGNs();
         for (int i = 0; i < nodes.length; i++) {
            // normally stress will be zero, unless there is prestrain ...
            double iN = Ns.get(i);
            Vector3d idN = dNs[i];
            FemUtilities.addShellStressForce(
               f0[i], f1[i], stress, t, dv0, iN, idN.x, idN.y, dt.getInvJ0());
            for (int j = 0; j < nodes.length; j++) {
               double jN = Ns.get(j);
               Vector3d jdN = dNs[j];
               // XXX should presumably use stress instead of
               // SymmetricMatrix3d.ZERO, but results are unstable
               FemUtilities.addShellMaterialStiffness (
                  K00[i*nnodes+j], K01[i*nnodes+j],
                  K10[i*nnodes+j], K11[i*nnodes+j],
                  iN, jN, idN, jdN, dv0, t,
                  dt.getInvJ0(), SymmetricMatrix3d.ZERO, D);
            }
         }
      }      
      
      // initial RHS
      Vector3d tmp0 = new Vector3d();
      Vector3d tmp1 = new Vector3d();
      for (int i = 0; i < nodes.length; i++) {
         tmp0.setZero();
         tmp1.setZero();
         for (int j=0; j<nodes.length; j++) {
            Vector3d pos = nodes[j].getRestPosition();
            Vector3d backPos = nodes[j].getBackRestPosition();
            mulAddK (i, j, tmp0, tmp1, pos, backPos);
         }
         f0[i].sub (tmp0, f0[i]);
         f1[i].sub (tmp1, f1[i]);
      }
   }

   public void addInitialMembraneStiffness (
      ShellElement3d e, AuxiliaryMaterial mat, double weight) {
      
      FemDeformedPoint dpnt = createDeformedPoint();
      FemNode3d[] nodes = e.getNodes();
      
      // compute stiffness matrix
      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d stress = new SymmetricMatrix3d();
      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
      IntegrationData3d[] idata = e.getIntegrationData();
      int nump = e.numPlanarIntegrationPoints();

      for (int k=0; k<nump; k++) {

         IntegrationPoint3d pt = ipnts[k];
         IntegrationData3d dt = idata[k];
         RotationMatrix3d R = null; // used if element has prestrain
         
         dpnt.setFromRestPoint (pt, dt, RotationMatrix3d.IDENTITY, e, k);

         double dv0 = e.getDefaultThickness()*dt.myDetJ0*weight*pt.getWeight();

         mat.computeStressAndTangent (stress, D, dpnt, pt, dt, null);
         Vector3d[] dNs = pt.getGNs();
         for (int i = 0; i < nodes.length; i++) {
            // normally stress will be zero, unless there is prestrain ...
            Vector3d idN = dNs[i];
            FemUtilities.addMembraneStressForce(
               f0[i], stress, dv0, idN.x, idN.y, dt.getInvJ0());
            for (int j = 0; j < nodes.length; j++) {
               Vector3d jdN = dNs[j];
               // XXX should presumably use stress instead of
               // SymmetricMatrix3d.ZERO, but results are unstable
               FemUtilities.addMembraneMaterialStiffness (
                  K00[i*nnodes+j], idN, jdN, dv0,
                  dt.getInvJ0(), SymmetricMatrix3d.ZERO, D);
            }
         }
      }      
      
      // initial RHS
      Vector3d tmp = new Vector3d();
      for (int i = 0; i < nodes.length; i++) {
         tmp.setZero();
         for (int j=0; j<nodes.length; j++) {
            K00[i*nnodes+j].mulAdd (tmp, nodes[j].getLocalRestPosition(), tmp);
         }
         f0[i].sub (tmp, f0[i]);
      }
   }
   
   /**
    * Retrieves the K00 contribution between nodes i and j
    * @param i first node index
    * @param j second node index
    * @return local stiffness contribution
    */
   public Matrix3d getInitialStiffness00(int i, int j) {
      return K00[i*nnodes+j];
   }
   
   /**
    * Retrieves the K01 contribution between nodes i and j
    * @param i first node index
    * @param j second node index
    * @return local stiffness contribution
    */
   public Matrix3d getInitialStiffness01(int i, int j) {
      return K01[i*nnodes+j];
   }
   
   /**
    * Retrieves the K10 contribution between nodes i and j
    * @param i first node index
    * @param j second node index
    * @return local stiffness contribution
    */
   public Matrix3d getInitialStiffness10(int i, int j) {
      return K10[i*nnodes+j];
   }
   
   /**
    * Retrieves the K00 contribution between nodes i and j
    * @param i first node index
    * @param j second node index
    * @return local stiffness contribution
    */
   public Matrix3d getInitialStiffness11(int i, int j) {
      return K11[i*nnodes+j];
   }
   
   public void mulAddK (
      int i, int j, Vector3d r0, Vector3d x0) {
 
      int idx = i*nnodes+j;
      K00[idx].mulAdd (r0, x0, r0);
   }
   
   public void mulAddK (
      int i, int j, Vector3d r0, Vector3d r1, Vector3d x0, Vector3d x1) {
 
      int idx = i*nnodes+j;
      K00[idx].mulAdd (r0, x0, r0);
      K01[idx].mulAdd (r0, x1, r0);
      K10[idx].mulAdd (r1, x0, r1);
      K11[idx].mulAdd (r1, x1, r1);
   }
   
   /**
    * Retrieves the stiffness-induced initial force on node i
    * @param i node index
    * @return force vector
    */
   public Vector3d getInitialForce(int i) {
      return f0[i];
   }

   public Vector3d getInitialBackForce(int i) {
      return f1[i];
   }
   
   public boolean equals(LinearMaterialCache cache) {
      
      if (cache.nnodes != nnodes) {
         return false;
      }
      for (int i=0; i<nnodes*nnodes; i++) {
         if (!K00[i].equals(cache.K00[i])) {
            return false;
         }
      }
      if (cache.f0.length != f0.length) {
         return false;
      }
      for (int i=0; i<f0.length; i++) {
         if (!f0[i].equals(cache.f0[i])) {
            return false;
         }
      }
      return true;
   }
   
   public boolean epsilonEquals(LinearMaterialCache cache, double eps) {
      
      if (cache.nnodes != nnodes) {
         return false;
      }
      for (int i=0; i<nnodes*nnodes; i++) {
         if (!K00[i].epsilonEquals(cache.K00[i], eps)) {
            return false;
         }
      }
      if (cache.f0.length != f0.length) {
         return false;
      }
      for (int i=0; i<f0.length; i++) {
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
      
      if (cache.nnodes != nnodes) {
         return Double.POSITIVE_INFINITY;
      }

      for (int i=0; i<nnodes*nnodes; i++) {
         diff.sub(K00[i], cache.K00[i]);
         double dist = diff.maxNorm();
         if (dist > maxdist) {
            maxdist = dist;
         }
      }
      if (cache.f0.length != f0.length) {
         return Double.POSITIVE_INFINITY;
      }
      for (int i=0; i<f0.length; i++) {
         dvec.sub(f0[i], cache.f0[i]);
         double dist = dvec.oneNorm();
         if (dist > maxdist) {
            maxdist = dist;
         }
      }

      return maxdist;
   }
  
}
