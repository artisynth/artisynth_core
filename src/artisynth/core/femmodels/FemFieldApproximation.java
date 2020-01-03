package artisynth.core.femmodels;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.TetElement;
import artisynth.core.femmodels.integration.MonteCarloFemElementIntegrator;
import artisynth.core.femmodels.integration.FemElementSampler;
import maspack.function.Function3x1;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.QRDecomposition;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

/**
 * Computes values in order to best approximate a field within a finite element
 *
 */
public class FemFieldApproximation {

   static int NUM_LSQ_SAMPLES = 1000;
   static int MIN_INTEGRATION_SAMPLES = 1000;
   static int MAX_INTEGRATION_SAMPLES = 100000;
   static double MAX_INTEGRATION_VARIANCE = 1e-10;
   
   FemElementSampler sampler;  // used to sample values from within element
   MonteCarloFemElementIntegrator integrator;
   int numLSQSamples;
   FemElement3d elem;
   QRDecomposition QRM;
   
   public FemFieldApproximation(FemElementSampler sampler) {
      this.sampler = sampler;
      this.integrator = new MonteCarloFemElementIntegrator(sampler);
      this.numLSQSamples = NUM_LSQ_SAMPLES;
      this.elem = null;
      QRM = null;
   }
   
   private void setElement(FemElement3d elem) {
      if (elem != this.elem) {
         sampler.setElement(elem);
         this.elem = elem;
         this.QRM = null;
      }
   }
   
   /**
    * Number of samples to use when approximating function with least-squares
    * @param n samples
    */
   public void setNumLSQSamples(int n) {
      numLSQSamples = n;
   }
   
   public void setSampler(FemElementSampler sampler) {
      if (sampler != this.sampler) {
         this.sampler = sampler;
         sampler.setElement(elem);
         integrator.setSampler(sampler);
      }
   }
   
   public void setIntegrationLimits(int minSamples, int maxSamples, double maxVariance) {
      integrator.setLimits(minSamples, maxSamples, maxVariance);
   }
   
   private static void addOuterProduct(MatrixNd A, VectorNd b, VectorNd c) {
      int n = b.size();
      for (int i=0; i<n; ++i) {
         double bb = b.get(i);
         for (int j=0; j<n; ++j) {
            double cc = c.get(j);
            A.add(i, j, bb*cc);
         }
      }
   }
   
   
   /**
    * Computes best-fitting coefficients {w_i} to minimize the least squared error
    * \sum_i ||f(x_i) - \hat{f}(x_i)||^2, where \hat{f}(x) = \sum_j w_i \phi_j(x) interpolates
    * the target function according to the FEM shape functions
    * @param elem element to fit
    * @param func function to fit
    * @param out node values
    */
   public void computeLeastSquaresNodeValues(FemElement3d elem, Function3x1 func, VectorNd out) {
      setElement(elem);
      
      int nnodes = elem.numNodes();
      int nsamples = Math.max(numLSQSamples, nnodes);   
      
      MatrixNd A = new MatrixNd(nnodes, nnodes);
      VectorNd b = new VectorNd(nnodes);
      
      Point3d pnt = new Point3d();
      Point3d coord = new Point3d();

      // row for computing A
      VectorNd a = new VectorNd(nnodes);
      for (int i=0; i<nsamples; ++i) {
         sampler.sample(coord, pnt);
         for (int j=0; j<elem.numNodes(); ++j) {
            a.set(j, elem.getN(j, coord));
         }
         double c = func.eval(pnt);
         addOuterProduct(A, a, a);
         b.scaledAdd(c, a);
      }
      
      QRDecomposition QR = new QRDecomposition(A);
      out.setSize(nnodes);
      QR.solve(out, b);
   }
   
   public MatrixNd computeMassMatrix(FemElement3d elem) {
      
      // use integration points
      FemNode3d[] nodes = elem.getNodes();
      int nnodes = nodes.length;
      MatrixNd M = new MatrixNd(nnodes, nnodes);
      
      
      if (elem instanceof TetElement) {
         // approximate
         elem.computeRestVolumes ();
         double v0 = elem.getRestVolume ();
         for (int i=0; i<nnodes; ++i) {
            M.set (i, i, v0/10);
            for (int j=0; j<i; ++j) {
               M.set (i, j, v0/20);
            }
         }
       
      } else {
         
         // integrate
         IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
         
         for (int k=0; k<ipnts.length; ++k) {
            IntegrationPoint3d pt = ipnts[k];
            Vector3d coords = pt.getCoords();
            double detJ = pt.computeJacobianDeterminant(nodes);
            //double detJ = pt.getJ().determinant();
            for (int i=0; i<nnodes; ++i) {
               double phi_i = elem.getN(i, coords);
               for (int j=0; j<=i; ++j) {
                  double phi_j = elem.getN(j, coords);
                  M.add(i, j, detJ*pt.getWeight()*phi_i*phi_j);
               }
            }
         }
      }

      // symmetric terms
      for (int i=0; i<nnodes; ++i) {
         for (int j=0; j<i; ++j) {
            M.set(j, i, M.get(i, j));
         }
      }
      
      return M;
   }
   
   /**
    * Computes best-fitting coefficients {w_i} to minimize the error between shape-function integrals:
    * E = ||int_x f(x)\phi_i(x)dx - int_x \hat{f}(x)\phi_i(x)dx||^2, where 
    * \hat{f}(x) = \sum_j w_i \phi_j(x) interpolates the target function according to the FEM 
    * shape functions.
    * @param elem element to evaluate
    * @param func function to fit
    * @param out node values
    */
   public void computeIntegralNodeValues(FemElement3d elem, Function3x1 func, VectorNd out) {
      setElement(elem);
      
      int nnodes = elem.numNodes();
      
      QRDecomposition QR = QRM;
      if (QR == null) {
         MatrixNd M = computeMassMatrix(elem);
         QR = new QRDecomposition(M);
         QRM = QR;
      }
      
      VectorNd b = new VectorNd(nnodes);
      integrator.integrateShapeFunctionProduct(elem, func, b);
      
      QR.solve(out, b);
   }
   
   //   /**
   //    * Computes projected coefficients {w_i}, where w_i = &lt;f,\phi_i(x)&gt; = \int_x f(x)\phi_i(x)dx
   //    * @param elem
   //    * @param func
   //    * @param out
   //    */
   //   public void computeProjectionNodeValues(FemElement3d elem, Function3x1 func, VectorNd out) {
   //      setElement(elem);
   //      
   //      integrator.integrateShapeFunctionProduct(elem, func, out);
   //      System.out.println(out.sum());
   //      
   //   }
   
   /**
    * Computes best-fitting coefficients {w_i} to minimize the error between shape-function integrals:
    * E = ||int_x f(x)\phi_i(x)dx - \hat{g}||^2, where 
    * \hat{g} = \sum_j w_i p_i(x_j)*\phi_j(x_j)|dx/dX|(x) is the discretized integral of
    * f(x)\phi_j(x) using the element's quadrature rules.  This method requires that there
    * be at least as many shape functions (i.e. nodes) as integration points.
    * @param elem element to consider
    * @param func function to approximate
    * @param out ipnt values
    */
   public void computeIntegralIPointValues(FemElement3d elem, Function3x1 func, VectorNd out) {
      setElement(elem);
      
      IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
      int nnodes = elem.numNodes();
      if (ipnts.length > nnodes) {
         throw new IllegalArgumentException("Element has insufficient number of shape functions");
      }
      
      MatrixNd A = new MatrixNd(nnodes, ipnts.length);
      
      for (int k=0; k<ipnts.length; ++k) {
         IntegrationPoint3d pt = ipnts[k];
         Vector3d coords = pt.getCoords();
         double detJ = pt.computeJacobianDeterminant(elem.getNodes());
         //double detJ = pt.getJ().determinant();
         for (int i=0; i<nnodes; ++i) {
            A.set(i, k, pt.getWeight()*detJ*elem.getN(i, coords));
         }
      }
      
      // rhs
      VectorNd b = new VectorNd(nnodes);
      integrator.integrateShapeFunctionProduct(elem, func, b);
      
      VectorNd r = new VectorNd(ipnts.length);
      A.mulTranspose(r, b);
      A.mulTransposeLeft(A, A);
      QRDecomposition QR = new QRDecomposition(A);
      QR.solve(out, r);
   }
   
}
