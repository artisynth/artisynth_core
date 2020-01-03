package artisynth.core.femmodels.integration;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.IntegrationData3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import maspack.function.Function3x1;
import maspack.matrix.Point3d;
import maspack.matrix.VectorNd;

/**
 * Uses the element's integration points to integrate a function
 * @author Antonio
 *
 */
public class IPointFemElementIntegrator implements FemElementIntegrator {

   public IPointFemElementIntegrator() {
   }
   
   @Override
   public double integrate(FemElement3d elem, Function3x1 func) {
      IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
      FemNode3d[] nodes = elem.getNodes();
      Point3d pos = new Point3d();
      
      double out = 0;
      for (int k=0; k<ipnts.length; ++k) {
         IntegrationPoint3d pt = ipnts[k];
         double detJ = pt.computeJacobianDeterminant(nodes);
         pt.computePosition(pos, nodes);      
         double fval = func.eval(pos);
         //double detJ = pt.getJ().determinant();
         out += fval*detJ*pt.getWeight();
      }
      
      return out;
   }
   
   @Override
   public double integrateRest(FemElement3d elem, Function3x1 func) {
      IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
      IntegrationData3d[] idata = elem.getIntegrationData();
      FemNode3d[] nodes = elem.getNodes();
      Point3d pos = new Point3d();
      
      double out = 0;
      for (int k=0; k<ipnts.length; ++k) {
         IntegrationPoint3d pt = ipnts[k];
         pt.computeRestPosition(pos, nodes);
         double fval = func.eval(pos);
         double detJ = idata[k].getDetJ0();
         out += fval*detJ*pt.getWeight();
      }
      
      return out;
   }
   
   /**
    * Integrate the function provided values at the integration points
    * @param elem element
    * @param ivals values of function at integration points
    * @return integrated value
    */
   public static double integrate(FemElement3d elem, VectorNd ivals) {
      IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
      FemNode3d[] nodes = elem.getNodes();
      
      double out = 0;
      for (int k=0; k<ipnts.length; ++k) {
         IntegrationPoint3d pt = ipnts[k];
         double detJ = pt.computeJacobianDeterminant(nodes);  
         double fval = ivals.get(k);
         //double detJ = pt.getJ().determinant();
         out += fval*detJ*pt.getWeight();
      }
      
      return out;
   }
   
   /**
    * Integrate the function provided values at the integration points
    * @param elem element
    * @param ivals values at integration point
    * @return integrated function
    */
   public static double integrateRest(FemElement3d elem, VectorNd ivals) {

      IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
      IntegrationData3d[] idata = elem.getIntegrationData();
      double out = 0;
      for (int k=0; k<idata.length; ++k) {
         double fval = ivals.get(k);
         double detJ = idata[k].getDetJ0();
         out += fval*detJ*ipnts[k].getWeight();
      }
      
      return out;
   }
   
   @Override
   public void integrateShapeFunctionProduct(FemElement3d elem, Function3x1 func, 
      VectorNd out) {

      IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
      FemNode3d[] nodes = elem.getNodes();
      Point3d pos = new Point3d();
      
      out.setSize(nodes.length);
      out.setZero();
      for (int k=0; k<ipnts.length; ++k) {
         IntegrationPoint3d pt = ipnts[k];
         double detJ = pt.computeJacobianDeterminant(nodes);
         pt.computePosition(pos, nodes);         
         //double detJ = pt.getJ().determinant();
         double fval = func.eval(pos)*pt.getWeight()*detJ;
         for (int i=0; i<nodes.length; ++i) {
            out.add(i, fval*elem.getN(i, pt.getCoords()));
         }
      }
   }
   
   @Override
   public void integrateShapeFunctionProductRest(FemElement3d elem, Function3x1 func, 
      VectorNd out) {

      IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
      IntegrationData3d[] idata = elem.getIntegrationData();
      FemNode3d[] nodes = elem.getNodes();
      Point3d pos = new Point3d();
      
      out.setSize(nodes.length);
      out.setZero();
      for (int k=0; k<ipnts.length; ++k) {
         IntegrationPoint3d pt = ipnts[k];
         pt.computePosition(pos, nodes);         
         double detJ = idata[k].getDetJ0();
         double fval = func.eval(pos)*pt.getWeight()*detJ;
         
         for (int i=0; i<nodes.length; ++i) {
            out.add(i, fval*elem.getN(i, pt.getCoords()));
         }
      }

   }
   
   /**
    * Integrate provided values at the integration points
    * @param elem element to integrate over
    * @param ivals values at integration points
    * @param out integrated output
    */
   public static void integrateShapeFunctionProduct(FemElement3d elem, VectorNd ivals, 
      VectorNd out) {

      IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
      FemNode3d[] nodes = elem.getNodes();
      
      out.setSize(nodes.length);
      out.setZero();
      for (int k=0; k<ipnts.length; ++k) {
         IntegrationPoint3d pt = ipnts[k];
         double detJ = pt.computeJacobianDeterminant(nodes);    
         //double detJ = pt.getJ().determinant();
         double fval = ivals.get(k)*pt.getWeight()*detJ;
         for (int i=0; i<nodes.length; ++i) {
            out.add(i, fval*elem.getN(i, pt.getCoords()));
         }
      }
   }
   
   /**
    * Integrate provided values at the integration points
    * @param elem element to integrate over
    * @param ivals values at integration points
    * @param out integration output
    */
   public static void integrateShapeFunctionProductRest(FemElement3d elem, VectorNd ivals, 
      VectorNd out) {

      IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
      IntegrationData3d[] idata = elem.getIntegrationData();
      FemNode3d[] nodes = elem.getNodes();
      
      out.setSize(nodes.length);
      out.setZero();
      for (int k=0; k<ipnts.length; ++k) {
         IntegrationPoint3d pt = ipnts[k];    
         double detJ = idata[k].getDetJ0();
         double fval = ivals.get(k)*pt.getWeight()*detJ;
         
         for (int i=0; i<nodes.length; ++i) {
            out.add(i, fval*elem.getN(i, pt.getCoords()));
         }
      }

   }

}
