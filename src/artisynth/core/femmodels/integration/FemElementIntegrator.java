package artisynth.core.femmodels.integration;

import artisynth.core.femmodels.FemElement3d;
import maspack.function.Function3x1;
import maspack.matrix.VectorNd;

/**
 * Integrate a function across a finite element
 */
public interface FemElementIntegrator {
   
   /**
    * Integrates a function defined over the finite element
    * @param elem finite element
    * @param func function to integrate
    * @return result of integral
    */
   public double integrate(FemElement3d elem, Function3x1 func);
    
   /**
    * Integrates a function times the shape functions
    * @param elem finite element
    * @param func function to integrate
    * @param out output, length equal to number of shape functions (i.e. FEM nodes)
    */
   public void integrateShapeFunctionProduct(FemElement3d elem, Function3x1 func, VectorNd out);
   
   /**
    * Integrates a function defined over the finite element at rest coordinates
    * @param elem finite element
    * @param func function to integrate
    * @return result of integral
    */
   public double integrateRest(FemElement3d elem, Function3x1 func);
    
   /**
    * Integrates a function times the shape functions at rest coordinates
    * @param elem finite element
    * @param func function to integrate
    * @param out output, length equal to number of shape functions (i.e. FEM nodes)
    */
   public void integrateShapeFunctionProductRest(FemElement3d elem, Function3x1 func, VectorNd out);
   
}
