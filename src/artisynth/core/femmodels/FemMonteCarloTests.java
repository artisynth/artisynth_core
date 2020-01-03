package artisynth.core.femmodels;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.HexElement;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.PyramidElement;
import artisynth.core.femmodels.TetElement;
import artisynth.core.femmodels.FemFieldApproximation;
import artisynth.core.femmodels.integration.IPointFemElementIntegrator;
import artisynth.core.femmodels.integration.MonteCarloFemElementIntegrator;
import artisynth.core.femmodels.integration.MonteCarloIntegrator;
import artisynth.core.femmodels.integration.MonteCarloIntegrator.FunctionNdSampler;
import artisynth.core.femmodels.integration.CanonicalFemElementSampler;
import artisynth.core.femmodels.integration.CanonicalSampler;
import artisynth.core.femmodels.integration.EulerianFemElementSampler;
import artisynth.core.femmodels.integration.FemElementSampler;
import artisynth.core.femmodels.integration.LagrangianFemElementSampler;
import maspack.function.Function3x1;
import maspack.function.Function3x1Base;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.util.Logger;
import maspack.util.Logger.LogLevel;

public class FemMonteCarloTests {
   
   private static class MassFunctionSampler implements FunctionNdSampler {

      FemElementSampler sampler;
      FemElement3d elem;
      Point3d coord;
      Point3d pnt;
      
      public MassFunctionSampler(FemElement3d elem, FemElementSampler sampler) {
         this.elem = elem;
         this.sampler = sampler;
         this.coord = new Point3d();
         this.pnt = new Point3d();
         sampler.setElement(elem);
      }

      @Override
      public int getSize() {
         return elem.numNodes()*(elem.numNodes()+1)/2;
      }

      @Override
      public void sample(VectorNd v, VectorNd p) {
         double sp = sampler.isample(coord, pnt);
         int idx = 0;
         for (int i=0; i<elem.numNodes(); ++i) {
            double phi_i = elem.getN(i, coord);
            for (int j=0; j<=i; ++j) {
               double phi_j = elem.getN(j, coord);
               v.set(idx, phi_i*phi_j);
               p.set(idx, sp);
               ++idx;
            }
         }
      }
   }
   
   public static void testHexIntegration() {
      FemElement3d hex = createHex();
      testIntegration(hex);
   }
   
   private static FemElement3d createTet() {
      FemNode3d[] nodes = new FemNode3d[4];
      for (int i=0; i<8; ++i) {
         nodes[0] = new FemNode3d(-1, -1, -1);
         nodes[1] = new FemNode3d( 1, -1, -1);
         nodes[2] = new FemNode3d( 1,  1, -1);
         nodes[3] = new FemNode3d( 0,  0,  1);
         
      }
      TetElement tet = new TetElement(nodes);
      return tet;
   }
   
   
   private static FemElement3d createHex() {
      FemNode3d[] nodes = new FemNode3d[8];
      double w = 0.2;
      double b = 0.6;
      for (int i=0; i<8; ++i) {
         nodes[0] = new FemNode3d(-w,-w, 1);
         nodes[1] = new FemNode3d( w,-w, 1);
         nodes[2] = new FemNode3d( w, w, 1);
         nodes[3] = new FemNode3d(-w, w, 1);
         nodes[4] = new FemNode3d(-b,-b,-1);
         nodes[5] = new FemNode3d( b,-b,-1);
         nodes[6] = new FemNode3d( b, b,-1);
         nodes[7] = new FemNode3d(-b, b,-1);
      }
      HexElement hex = new HexElement(nodes);
      return hex;
   }
   
   public static void testPyramidIntegration() {
      FemElement3d pyr = createPyramid();
      testIntegration(pyr);
   }
   
   private static FemElement3d createPyramid() {
      FemNode3d[] nodes = new FemNode3d[5];
      for (int i=0; i<5; ++i) {
         //         nodes[0] = new FemNode3d(0.1, -0.3, -0.1);
         //         nodes[1] = new FemNode3d(1.2, 0.1, 0.2);
         //         nodes[2] = new FemNode3d(1.6, 1.6,-0.3);
         //         nodes[3] = new FemNode3d(0.3, 0.8, 0.1);
         //         nodes[4] = new FemNode3d(0.5, 1.2, 1.1);
         nodes[0] = new FemNode3d(-1, -1, -1);
         nodes[1] = new FemNode3d( 1, -1, -1);
         nodes[2] = new FemNode3d( 1,  1, -1);
         nodes[3] = new FemNode3d(-1,  1, -1);
         nodes[4] = new FemNode3d( 0,  0,  1);
      }
      PyramidElement pyr = new PyramidElement(nodes);
      return pyr;
   }
   
   private static class TestFunc extends Function3x1Base {
      @Override
      public double eval(double x, double y, double z) {
         // return x*x + Math.cos(Math.PI/2*y)*Math.exp(z) + 1;
         return x;
      }
   }
   
   // simpler function
   private static class TestFunc2 extends Function3x1Base {
      @Override
      public double eval(double x, double y, double z) {
         return 3*x+2*y*z+2;
      }
   }
   
   private static class Constant extends Function3x1Base {
      @Override
      public double eval(double x, double y, double z) {
         return 1;
      }
   }
   
   private static void testIntegration(FemElement3d elem) {
      
      elem.computeVolumes();
      
      MonteCarloFemElementIntegrator eulerianIntegrator = 
         new MonteCarloFemElementIntegrator(
            new EulerianFemElementSampler());
      eulerianIntegrator.setMaxSamples(10000);
      
      MonteCarloFemElementIntegrator canonicalIntegrator = 
         new MonteCarloFemElementIntegrator(
            new CanonicalFemElementSampler());
      eulerianIntegrator.setMaxSamples(10000);
      
      MonteCarloFemElementIntegrator lagrangianIntegrator = 
         new MonteCarloFemElementIntegrator(
            new LagrangianFemElementSampler());
      eulerianIntegrator.setMaxSamples(10000);
      
      IPointFemElementIntegrator ipntIntegrator = new IPointFemElementIntegrator();
      
      Constant constant = new Constant();
      double vol = elem.getVolume();
      double vole = eulerianIntegrator.integrate(elem, constant);
      double voll = lagrangianIntegrator.integrate(elem, constant);
      double volc = canonicalIntegrator.integrate(elem, constant);
      double voli = ipntIntegrator.integrate(elem, constant);
      
      System.out.println("Volumes: " + vol + ", " + vole + ", " + voll + ", " 
         + volc + ", " + voli );

      vol = elem.getVolume();
      vole = manualIntegration(new EulerianFemElementSampler(), elem, constant);
      voll = manualIntegration(new LagrangianFemElementSampler(), elem, constant);
      volc = manualIntegration(new CanonicalFemElementSampler(), elem, constant);
      voli = ipntIntegrator.integrate(elem, constant);      
      System.out.println("Manual Volumes: " + vol + ", " + vole + ", " + voll + ", " 
         + volc + ", " + voli );
      
      TestFunc testFunc = new TestFunc();
      // analytic
      // double e = Math.exp(1);
      // double ia = 2*((8*e*Math.PI-12)/e + 8*Math.PI + 12*e)/(3*Math.PI);
      double ie = eulerianIntegrator.integrate(elem, testFunc);
      double il = lagrangianIntegrator.integrate(elem, testFunc);
      double ic = canonicalIntegrator.integrate(elem, testFunc);
      double ii = ipntIntegrator.integrate(elem, testFunc);
      System.out.println("Integrals: " + ie + ", " + il + ", " 
         + ic + ", " + ii );
      
      ie = manualIntegration(new EulerianFemElementSampler(), elem, testFunc);
      il = manualIntegration(new LagrangianFemElementSampler(), elem, testFunc);
      ic = manualIntegration(new CanonicalFemElementSampler(), elem, testFunc);
      ii = ipntIntegrator.integrate(elem, testFunc);      
      System.out.println("Manual integrals: " + ie + ", " + il + ", " 
         + ic + ", " + ii );
      
      TestFunc2 testFunc2 = new TestFunc2();
      // analytic
      // ia = 16.0;
      ie = eulerianIntegrator.integrate(elem, testFunc2);
      ic = canonicalIntegrator.integrate(elem, testFunc2);
      il = lagrangianIntegrator.integrate(elem, testFunc2);
      ii = ipntIntegrator.integrate(elem, testFunc2);
      System.out.println("Integrals: " + ie + ", " + il + ", " 
         + ic + ", " + ii );
      
      ie = manualIntegration(new EulerianFemElementSampler(), elem, testFunc2);
      il = manualIntegration(new LagrangianFemElementSampler(), elem, testFunc2);
      ic = manualIntegration(new CanonicalFemElementSampler(), elem, testFunc2);
      ii = ipntIntegrator.integrate(elem, testFunc2);      
      System.out.println("Manual integrals: " + ie + ", " + il + ", " 
         + ic + ", " + ii );
    
   }
   
   public static void testApproximation(FemElement3d elem, Function3x1 func) {

      FemElementSampler sampler = new LagrangianFemElementSampler(); //new EulerianFemElementSampler();
      FemFieldApproximation fapprox = new FemFieldApproximation(sampler);
      fapprox.setIntegrationLimits(1000, 100000, 1e-10);
      
      VectorNd lsqnodes = new VectorNd();
      VectorNd intnodes = new VectorNd();
      VectorNd intipnts = new VectorNd();
      
      fapprox.computeLeastSquaresNodeValues(elem, func, lsqnodes);
      fapprox.computeIntegralNodeValues(elem, func, intnodes);
      fapprox.computeIntegralIPointValues(elem, func, intipnts);
      
      MassFunctionSampler msamples = new MassFunctionSampler(elem, sampler);
      MonteCarloIntegrator mcintegrator = new MonteCarloIntegrator();
      VectorNd mm = new VectorNd(elem.numNodes()*(elem.numNodes()+1)/2);
      mcintegrator.integrate(msamples, mm);
      
      IPointFemElementIntegrator integrator = new IPointFemElementIntegrator();
      double fi = integrator.integrate(elem, func);
      double lsqni = manualNodeIntegration(elem, lsqnodes);
      double intni = manualNodeIntegration(elem, intnodes);
      double intii = manualIPointIntegration(elem, intipnts);
      System.out.println("Volumes: " + fi + ", " + lsqni + ", " + intni + ", " + intii );
      
      VectorNd r = new VectorNd(elem.numNodes());
      integrator.integrateShapeFunctionProduct(elem, func, r);
      for (int i=0; i<elem.numNodes(); ++i) {
         fi = r.get(i);
         lsqni = manualNodeShapeIntegration(elem, i, lsqnodes);
         intni = manualNodeShapeIntegration(elem, i, intnodes);
         intii = manualIPointShapeIntegration(elem, i, intipnts);
         System.out.println("         " + fi + ", " + lsqni + ", " + intni + ", " + intii);
      }
      
   }
   
   private static double manualNodeIntegration(FemElement3d elem, VectorNd nodeVals) {
   
      IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
      int nnodes = elem.numNodes();
      
      double I = 0;
      
      for (int k=0; k<ipnts.length; ++k) {
         IntegrationPoint3d pt = ipnts[k];
         Vector3d coord = pt.getCoords();
         double detJ = pt.computeJacobianDeterminant(elem.getNodes());
         //double detJ = pt.getJ().determinant();
      
         // function value at coordinate
         double s = 0;
         for (int i=0; i<nnodes; ++i) {
            s += nodeVals.get(i)*elem.getN(i, coord);
         }
         
         I += pt.getWeight()*detJ*s;
      }
      
      return I;
   }

   private static double manualIPointIntegration(FemElement3d elem, VectorNd ipntVals) {
      IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
      
      double I = 0;
      
      for (int k=0; k<ipnts.length; ++k) {
         IntegrationPoint3d pt = ipnts[k];
         double detJ = pt.computeJacobianDeterminant(elem.getNodes());
         //double detJ = pt.getJ().determinant();
      
         // function value at coordinate
         I += pt.getWeight()*detJ*ipntVals.get(k);
      }
      
      return I;
   }
   
   private static double manualNodeShapeIntegration(FemElement3d elem, int nidx, VectorNd nodeVals) {
      
      IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
      int nnodes = elem.numNodes();
      
      double I = 0;
      
      for (int k=0; k<ipnts.length; ++k) {
         IntegrationPoint3d pt = ipnts[k];
         Vector3d coord = pt.getCoords();
         double detJ = pt.computeJacobianDeterminant(elem.getNodes());
         //double detJ = pt.getJ().determinant();
      
         // function value at coordinate
         double s = 0;
         for (int i=0; i<nnodes; ++i) {
            s += nodeVals.get(i)*elem.getN(i, coord);
         }
         
         I += pt.getWeight()*detJ*s*elem.getN(nidx, coord);
      }
      
      return I;
   }

   private static double manualIPointShapeIntegration(FemElement3d elem, int nidx, VectorNd ipntVals) {
      IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
      
      double I = 0;
      
      for (int k=0; k<ipnts.length; ++k) {
         IntegrationPoint3d pt = ipnts[k];
         double detJ = pt.computeJacobianDeterminant(elem.getNodes());
         //double detJ = pt.getJ().determinant();
      
         // function value at coordinate
         I += pt.getWeight()*detJ*ipntVals.get(k)*elem.getN(nidx, pt.getCoords());
      }
      
      return I;
   }
   
   @SuppressWarnings("unused")
   private static double manualIntegration(FemElementSampler s, FemElement3d elem, Function3x1 func) {
      if (s instanceof CanonicalFemElementSampler) {
         return manualIntegration((CanonicalFemElementSampler)s, elem, func);
      } else if (s instanceof LagrangianFemElementSampler) {
         return manualIntegration((LagrangianFemElementSampler)s, elem, func);
      } else if (s instanceof EulerianFemElementSampler) {
         return manualIntegration((EulerianFemElementSampler)s, elem, func);
      }
      return 0;
   }
   
   private static double manualIntegration(CanonicalFemElementSampler s, FemElement3d elem,
      Function3x1 func) {
      
      Point3d c = new Point3d();
      Point3d pnt = new Point3d();
      Matrix3d J = new Matrix3d();
      Matrix3d JS = new Matrix3d();
      Vector3d dNds = new Vector3d();
      FemNode3d[] nodes = elem.getNodes();
      
      s.setElement(elem);
      double I = 0;
      double v = CanonicalSampler.get(elem).volume();
      
      double[] nodeCoords = elem.getNodeCoords();
      
      // get location in hex at (0,0,1)
      pnt.setZero();
      for (int j=0; j<elem.numNodes(); ++j) {
         double d = elem.getN(j, new Vector3d(1,0,1));
         pnt.scaledAdd(d, nodes[j].getLocalPosition());
      }
      
      int nsamples = 100000;
      for (int i=0; i<nsamples; ++i) {
         s.sample(c, pnt);
         J.setZero();
         JS.setZero();
         for (int j=0; j<elem.numNodes(); ++j) {
            elem.getdNds(dNds, j, c);
            J.addOuterProduct(nodes[j].getLocalPosition(), dNds);
            JS.addOuterProduct(nodeCoords[3*j], nodeCoords[3*j+1], nodeCoords[3*j+2], dNds.x, dNds.y, dNds.z);
         }
         double detJ = J.determinant();
         double detJS = JS.determinant();
         I += func.eval(pnt)*detJ/detJS/nsamples*v;
      }
      
      return I;
   }
   
   private static double manualIntegration(LagrangianFemElementSampler s, FemElement3d elem,
      Function3x1 func) {
      
      Point3d c = new Point3d();
      Point3d pnt = new Point3d();
      Matrix3d J = new Matrix3d();
      Matrix3d J0 = new Matrix3d();
      Vector3d dNds = new Vector3d();
      FemNode3d[] nodes = elem.getNodes();
      
      s.setElement(elem);
      double I = 0;
      double v = elem.getRestVolume();
      int nsamples = 10000;
      for (int i=0; i<nsamples; ++i) {
         s.sample(c, pnt);
         J.setZero();
         J0.setZero();
         for (int j=0; j<elem.numNodes(); ++j) {
            elem.getdNds(dNds, j, c);
            J.addOuterProduct(nodes[j].getLocalPosition(), dNds);
            J0.addOuterProduct(nodes[j].getRestPosition(), dNds);
         }
         double detJ = J.determinant();
         double detJ0 = J0.determinant();
         I += func.eval(pnt)*detJ/detJ0/nsamples*v;
      }
      
      return I;
   }
   
   private static double manualIntegration(EulerianFemElementSampler s, FemElement3d elem,
      Function3x1 func) {
      Point3d c = new Point3d();
      Point3d pnt = new Point3d();
      
      s.setElement(elem);
      double I = 0;
      double v = elem.getVolume();
      int nsamples = 10000;
      for (int i=0; i<nsamples; ++i) {
         s.sample(c, pnt);
         I += func.eval(pnt)/nsamples*v;
      }
      
      return I;
   }
   
   public static void main(String[] args) {
      
      Logger.getSystemLogger().setLogLevel(LogLevel.INFO);
      
      //testHexIntegration();
      //testPyramidIntegration();
      //testApproximation();
      //testMassMatrix();
      
      testApproximation(createTet(), new Constant());
      testApproximation(createTet(), new TestFunc());
      testApproximation(createTet(), new TestFunc2());
      testApproximation(createHex(), new Constant());
      testApproximation(createHex(), new TestFunc());
      testApproximation(createHex(), new TestFunc2());
      testApproximation(createPyramid(), new Constant());
      testApproximation(createPyramid(), new TestFunc());
      testApproximation(createPyramid(), new TestFunc2());
      
   }
   
}
