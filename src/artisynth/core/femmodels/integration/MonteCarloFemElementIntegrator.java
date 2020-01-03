package artisynth.core.femmodels.integration;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.integration.FemElementSampler;
import maspack.function.Function3x1;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.VectorNd;

/**
 * Monte-Carlo integration over FEM element
 *
 */
public class MonteCarloFemElementIntegrator extends MonteCarloIntegrator 
   implements FemElementIntegrator  {
   
   private static class MyFunctionSampler implements FunctionSampler {
      FemElementSampler sampler;
      FemElement3d elem;
      Function3x1 function;
      Point3d coord;
      Point3d pnt;
      boolean useRest;
      
      public MyFunctionSampler() {
         sampler = null;
         elem = null;
         function = null;
         coord = new Point3d();
         pnt = new Point3d();
         useRest = false;
      }
      
      public void setUseRestPosition(boolean set) {
         useRest = set;
      }
      
      public void setSampler(FemElementSampler sampler) {
         this.sampler = sampler;
      }
      
      public void setElement(FemElement3d elem) {
         sampler.setElement(elem);
         this.elem = elem;
      }
      
      public void setFunction(Function3x1 func) {
         this.function = func;
      }

      @Override
      public void sample(Vector2d vp) {
         
         if (useRest) {
            vp.y = sampler.isample(coord, null);
            // compute rest
            pnt.setZero();
            FemNode3d[] nodes = elem.getNodes();
            for (int i=0; i<elem.numNodes(); ++i) {
               double s = elem.getN(i, coord);
               pnt.scaledAdd(s, nodes[i].getRestPosition());
            }
         } else {
            vp.y = sampler.isample(coord, pnt);   
         }
         
         vp.x = function.eval(pnt);
      }      
   }
   
   private static class MyShapeFunctionProductSampler extends MyFunctionSampler {
      
      private int sidx;
      public MyShapeFunctionProductSampler() {
         super();
         sidx = 0;
      }
      
      public void setShapeFunction(int idx) {
         sidx = idx;
      }
      
      @Override
      public void sample(Vector2d vp) {
         super.sample(vp);
         vp.x = vp.x * elem.getN(sidx, coord);
      }      
   }
   
   MyFunctionSampler fsampler;
   MyShapeFunctionProductSampler sfpsampler;
   
   public MonteCarloFemElementIntegrator(FemElementSampler sampler) {
      super();
      fsampler = new MyFunctionSampler();
      sfpsampler = new MyShapeFunctionProductSampler();
      setSampler(sampler);
   }
   
   public void setSampler(FemElementSampler sampler) {
      fsampler.setSampler(sampler);
      sfpsampler.setSampler(sampler);
   }


   @Override
   public double integrate(FemElement3d elem, Function3x1 func) {
      fsampler.setUseRestPosition(false);
      fsampler.setElement(elem);
      fsampler.setFunction(func);
      return super.integrate(fsampler, elem.getVolume());
   }

   @Override
   public void integrateShapeFunctionProduct(
      FemElement3d elem, Function3x1 func, VectorNd out) {
      sfpsampler.setUseRestPosition(false);
      sfpsampler.setElement(elem);
      sfpsampler.setFunction(func);
      
      out.setSize(elem.numNodes());
      double v = elem.getVolume();
      for (int i=0;i<elem.numNodes(); ++i) {
         sfpsampler.setShapeFunction(i);
         double d = super.integrate(sfpsampler, v);
         out.set(i, d);
      }
   }
   
   @Override
   public double integrateRest(FemElement3d elem, Function3x1 func) {
      fsampler.setUseRestPosition(true);
      fsampler.setElement(elem);
      fsampler.setFunction(func);
      return super.integrate(fsampler, elem.getVolume());
   }

   @Override
   public void integrateShapeFunctionProductRest(
      FemElement3d elem, Function3x1 func, VectorNd out) {
      sfpsampler.setUseRestPosition(true);
      sfpsampler.setElement(elem);
      sfpsampler.setFunction(func);
      
      out.setSize(elem.numNodes());
      double v = elem.getVolume();
      for (int i=0;i<elem.numNodes(); ++i) {
         sfpsampler.setShapeFunction(i);
         double d = super.integrate(sfpsampler, v);
         out.set(i, d);
      }
   }
   
}
