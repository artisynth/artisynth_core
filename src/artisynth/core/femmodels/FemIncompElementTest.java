package artisynth.core.femmodels;

import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.materials.IncompressibleMaterialBase.BulkPotential;
import artisynth.core.materials.YeohMaterial;
import artisynth.core.mechmodels.SolveMatrixTest;
import maspack.matrix.Point3d;
import maspack.util.TestException;
import maspack.util.UnitTest;

/**
 * Checks soft incompressibility on a single element under a homogeneous
 * deformation, for every 3D element type and every soft incompressibility
 * method. Under a homogeneous deformation the pressure field is constant, so
 * the nodal mean stress must equal the analytic material pressure and the
 * nodal energy density must equal the analytic strain energy density,
 * regardless of how the pressure is projected onto the element. This catches
 * the wrong pressure weight normalization for elements with more than one
 * pressure value per element (QuadhexElement, QuadwedgeElement).
 *
 * <p>The stiffness matrix is also checked against a numeric Jacobian of the
 * nodal forces, since the pressure weight matrix enters the tangent (via
 * Rinv) as well as the stress.
 */
public class FemIncompElementTest extends UnitTest {

   static final double C1 = 50e6;
   static final double C2 = 10e6;
   static final double K = 100*C1;
   // relative tolerance for the numeric stiffness check; correct
   // configurations come in below 2e-7
   static final double STIFFNESS_TOL = 1e-5;

   FemElement3d createElement (String type, FemModel3d fem) {
      FemElement3d proto;
      switch (type) {
         case "hex": proto = new HexElement(); break;
         case "quadhex": proto = new QuadhexElement(); break;
         case "tet": proto = new TetElement(); break;
         case "quadtet": proto = new QuadtetElement(); break;
         case "wedge": proto = new WedgeElement(); break;
         case "quadwedge": proto = new QuadwedgeElement(); break;
         case "pyramid": proto = new PyramidElement(); break;
         case "quadpyramid": proto = new QuadpyramidElement(); break;
         default: throw new IllegalArgumentException (type);
      }
      double[] c = proto.getNodeCoords();
      int n = proto.numNodes();
      FemNode3d[] nodes = new FemNode3d[n];
      for (int i=0; i<n; i++) {
         nodes[i] = new FemNode3d (c[i*3], c[i*3+1], c[i*3+2]);
         fem.addNode (nodes[i]);
      }
      switch (type) {
         case "hex": return new HexElement (nodes);
         case "quadhex": return new QuadhexElement (nodes);
         case "tet": return new TetElement (nodes[0],nodes[1],nodes[2],nodes[3]);
         case "quadtet": return new QuadtetElement (nodes);
         case "wedge": return new WedgeElement (nodes);
         case "quadwedge": return new QuadwedgeElement (nodes);
         case "pyramid": return new PyramidElement (nodes);
         case "quadpyramid": return new QuadpyramidElement (nodes);
         default: throw new IllegalArgumentException (type);
      }
   }

   double analyticEnergy (double lx, double ly, double lz, BulkPotential pot) {
      double J = lx*ly*lz;
      double I1 = lx*lx + ly*ly + lz*lz;
      double x = Math.pow (J, -2.0/3.0)*I1 - 3;
      double U = (pot == BulkPotential.QUADRATIC ?
         K*(J-1)*(J-1)/2 : K*Math.log(J)*Math.log(J)/2);
      return C1*x + C2*x*x + U;
   }

   double analyticPressure (double J, BulkPotential pot) {
      return (pot == BulkPotential.QUADRATIC ? K*(J-1) : K*Math.log(J)/J);
   }

   void testElement (
      String type, IncompMethod method, BulkPotential pot, double[] stretch) {

      FemModel3d fem = new FemModel3d();
      FemElement3d e = createElement (type, fem);
      fem.addElement (e);
      fem.setDensity (1000);
      YeohMaterial mat = new YeohMaterial (C1, C2, 0, K);
      mat.setBulkPotential (pot);
      fem.setMaterial (mat);
      fem.setSoftIncompMethod (method);
      fem.setComputeNodalStress (true);
      fem.setComputeNodalEnergyDensity (true);
      for (FemNode3d n : fem.getNodes()) {
         Point3d p = new Point3d (n.getRestPosition());
         p.x *= stretch[0];
         p.y *= stretch[1];
         p.z *= stretch[2];
         n.setPosition (p);
      }
      e.computeVolumes();
      fem.updateStressAndStiffness();

      double J = stretch[0]*stretch[1]*stretch[2];
      double pchk = analyticPressure (J, pot);
      double Wchk = analyticEnergy (stretch[0], stretch[1], stretch[2], pot);
      double tol = 1e-8*Math.max (K, C1);
      String name = type + " " + method + " " + pot + " J=" + J;
      for (FemNode3d n : fem.getNodes()) {
         checkEquals (
            name + ": nodal mean stress", n.getStress().trace()/3, pchk, tol);
         checkEquals (
            name + ": nodal energy density", n.getEnergyDensity(), Wchk, tol);
      }
      double kerr = new SolveMatrixTest().testStiffness (fem, 1e-8);
      if (kerr > STIFFNESS_TOL) {
         throw new TestException (
            name + ": stiffness error " + kerr + ", tol=" + STIFFNESS_TOL);
      }
   }

   public void test() {
      String[] types = {
         "hex", "quadhex", "tet", "quadtet",
         "wedge", "quadwedge", "pyramid", "quadpyramid" };
      double[][] stretches = {
         { 1/Math.sqrt(1.2), 1/Math.sqrt(1.2), 1.2 }, // isochoric
         { 1, 1, 1.2 } };                              // J = 1.2
      for (String type : types) {
         for (double[] s : stretches) {
            for (BulkPotential pot : BulkPotential.values()) {
               testElement (type, IncompMethod.ELEMENT, pot, s);
               testElement (type, IncompMethod.NODAL, pot, s);
               testElement (type, IncompMethod.FULL, pot, s);
            }
         }
      }
   }

   public static void main (String[] args) {
      FemIncompElementTest tester = new FemIncompElementTest();
      tester.runtest();
   }
}
