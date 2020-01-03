package artisynth.core.femmodels.integration;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.IntegrationData3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.TetElement;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.RandomGenerator;

/**
 * Samples uniformly in Eulerian space
 * @author Antonio
 *
 */
public class LagrangianFemElementSampler extends FemElementSamplerBase {

   /**
    * Tries to estimate the maximum Jacobian of an element.
    * Computes the Jacobian at each of the integration points, computes the element volume,
    * then if the integration points provide a poor estimate of the volume, does some random
    * sampling.
    * @param e element of which to estimate the maximum Jacobian
    * @param canonicalSampler sampler for canonical volume
    * @return an estimated maximum Jacobian
    */
   private static double estimateMaxRestJacobianRatio(FemElement3d e, 
      CanonicalSampler canonicalSampler) {
      e.computeVolumes();
      double v = e.getRestVolume();
      double cv = canonicalSampler.volume();  // canonical volume
      double ev = 0; // estimated volume
      double Jmax = 0;

      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
      IntegrationData3d[] idata = e.getIntegrationData();
      Vector3d dNds = new Vector3d();
      double[] ncoords = e.getNodeCoords();
      Matrix3d Js = new Matrix3d();

      for (int i=0; i<ipnts.length; ++i) {
         double detJ = idata[i].getDetJ0();
         Js.setZero();
         for (int j=0; j<e.numNodes(); ++j) {
            e.getdNds(dNds, j, ipnts[i].getCoords());
            Js.addOuterProduct(ncoords[3*j],ncoords[3*j+1], ncoords[3*j+2], 
               dNds.x, dNds.y, dNds.z);
         }
         double detJR = detJ/Js.determinant();
         if (detJR > Jmax) {
            Jmax = detJR;
         }
         ev += detJ*ipnts[i].getWeight();
      }

      // volume is within 5%
      double vfrac = ev/v;
      if (vfrac >= 0.95 && vfrac <= 1.05) {
         return Jmax;
      }

      // do some random sampling until we have a good estimate of the volume
      double Jsum = 0;
      int count = 0;
      Point3d c = new Point3d();
      Matrix3d J = new Matrix3d();
      FemNode3d[] nodes = e.getNodes();
      for (int i=0; i<10000; ++i) {
         canonicalSampler.sampleCoord(c);
         // compute detJ
         J.setZero();
         Js.setZero();
         for (int j=0; j<e.numNodes(); ++j) {
            e.getdNds(dNds, j, c);
            J.addOuterProduct(nodes[j].getRestPosition(), dNds);
            Js.addOuterProduct(ncoords[3*j],ncoords[3*j+1], ncoords[3*j+2], 
                    dNds.x, dNds.y, dNds.z);
         }   
         double detJR = J.determinant()/Js.determinant();
         if (detJR > Jmax) {
            Jmax = detJR;
         }
         Jsum += detJR;
         ++count;
         vfrac = Jsum/count*cv/v;

         // exit early if we have a good volume estimate already
         if (i > 30 && vfrac >= 0.95 && vfrac <= 1.05) {
            return Jmax;
         }
      }

      // haven't found good estimate, guess at what value we're missing
      // (Jsum + detJ)/(count+1)*cv = v
      double detJR = v*(count+1)/cv-Jsum;
      if (detJR > Jmax) {
         Jmax = detJR;
      }
      return Jmax;
   }

   //   /**
   //    * Tries to estimate the minimum Jacobian of an element.
   //    * Computes the Jacobian at each of the integration points, computes the element volume,
   //    * then if the integration points provide a poor estimate of the volume, does some random
   //    * sampling.
   //    * @param e element of which to estimate the minumum Jacobian
   //    * @param canonicalSampler sampler for canonical volume
   //    * @return an estimated minimum Jacobian
   //    */
   //   private static double estimateMinRestJacobian(FemElement3d e, CanonicalSampler canonicalSampler) {
   //      
   //      double minJ0 = e.computeRestVolumes();
   //      if (minJ0 <= 0) {
   //         return 0;
   //      }
   //      
   //      double v = e.getRestVolume();
   //      double cv = canonicalSampler.volume();  // canonical volume
   //      double ev = 0;                          // estimated volume
   //      
   //      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
   //      IntegrationData3d[] idata = e.getIntegrationData();
   //      for (int i=0; i<idata.length; ++i) {
   //         double detJ0 = idata[i].getDetJ0();
   //         if (detJ0 < minJ0) {
   //            minJ0 = detJ0;
   //         }
   //         ev += detJ0*ipnts[i].getWeight();
   //      }
   //
   //      // volume is within 5%
   //      double vfrac = ev/v;
   //      if (vfrac >= 0.95 && vfrac <= 1.05) {
   //         return minJ0;
   //      }
   //      
   //      // do some random sampling until we have a good estimate of the volume
   //      double Jsum = 0;
   //      int count = 0;
   //      Point3d c = new Point3d();
   //      Vector3d dNds = new Vector3d();
   //      Matrix3d J0 = new Matrix3d();
   //      FemNode3d[] nodes = e.getNodes();
   //      for (int i=0; i<10000; ++i) {
   //         canonicalSampler.sample(c);
   //         // compute detJ
   //         J0.setZero();
   //         for (int j=0; j<e.numNodes(); ++j) {
   //            e.getdNds(dNds, j, c);
   //            J0.addOuterProduct(dNds, nodes[j].getRestPosition());
   //         }   
   //         double detJ0 = J0.determinant();
   //         if (detJ0 < minJ0) {
   //            minJ0 = detJ0;
   //         }
   //         Jsum += detJ0;
   //         ++count;
   //         vfrac = Jsum/count*cv/v;
   //         
   //         // exit early if we have a good volume estimate already
   //         if (i > 30 && vfrac >= 0.95 && vfrac <= 1.05) {
   //            return minJ0;
   //         }
   //      }
   //      
   //      // haven't found good estimate, guess at what value we're missing
   //      // (Jsum + detJ)/(count+1)*cv = v
   //      double detJ = v*(count+1)/cv-Jsum;
   //      if (detJ < minJ0) {
   //         minJ0 = detJ;
   //      }
   //      return minJ0;
   //   }

   /**
    * Uses a sample rejection technique to compute a random sample within an element.
    * @param elem element
    * @param canonicalSampler samples canonical element
    * @param maxJR0 maximum rest jacobian ratio (compared to canonical element)
    * @param c natural coordinates at sample position
    * @param J0 rest jacobian at sample position
    * @param pnt final point
    */
   public static void sampleElementRejection(FemElement3d elem, 
      CanonicalSampler canonicalSampler, 
      double maxJR0, Point3d c, Matrix3d J0, Point3d pnt) {

      // cap Jacobian so points can still be selected
      if (maxJR0 > 1e5*elem.getVolume()) {
         maxJR0 = 1e5*elem.getVolume();
      }

      if (c == null) {
         c = new Point3d();
      }
      if (J0 == null) {
         J0 = new Matrix3d();
      }

      FemNode3d[] nodes = elem.getNodes();
      Vector3d dNds = new Vector3d();
      Matrix3d Js = new Matrix3d();
      double[] ncoords = elem.getNodeCoords();
      
      // probability is (detJ0(x)/detJs(x))/vol0
      // upper bound is max(detJ0(x)/detJs(x))/vol0
      boolean accept = false;
      while (!accept) {
         canonicalSampler.sampleCoord(c);
         // compute detJ
         J0.setZero();
         Js.setZero();
         for (int i=0; i<elem.numNodes(); ++i) {
            elem.getdNds(dNds, i, c);
            J0.addOuterProduct(nodes[i].getRestPosition(), dNds);
            Js.addOuterProduct(ncoords[3*i],ncoords[3*i+1], ncoords[3*i+2], 
                    dNds.x, dNds.y, dNds.z);
         }   
         double detJR0 = J0.determinant()/Js.determinant();
         double w = RandomGenerator.nextDouble(0, maxJR0);
         if (w < detJR0) {
            accept = true;
         }
      }

      // compute final point based on shape functions
      if (pnt != null) {
         pnt.setZero();
         for (int i=0; i<elem.numNodes(); ++i) {
            double d = elem.getN(i, c);
            pnt.scaledAdd(d, nodes[i].getPosition());
         }
      }
   }

   boolean istet;
   double maxJR0;

   public LagrangianFemElementSampler() {
      super();
      istet = false;
      maxJR0 = 0;
      RandomGenerator.setSeed(0);
   }

   public void setElement(FemElement3d elem) {
      if (this.elem != elem) {
         super.setElement(elem);
         this.elem = elem;
         this.sampler = null;
         this.maxJR0 = 0;
         this.istet = false;
         if (elem != null) {
            this.sampler = CanonicalSampler.get(elem);
            if (elem instanceof TetElement) {
               istet = true;
            } else {
               this.maxJR0 = estimateMaxRestJacobianRatio(elem, sampler);
            }
         }
      }
   }

   public void sample(Point3d c, Matrix3d J0, Point3d pnt) {
      if (istet) {
         if (c == null) {
            c = new Point3d();
         }
         sampleTet((TetElement)elem, c, pnt);
         // compute J
         if (J0 != null) {
            FemNode3d[] nodes = elem.getNodes();
            Vector3d dNds = new Vector3d();
            J0.setZero();
            for (int i=0; i<elem.numNodes(); ++i) {
               elem.getdNds(dNds, i, c);
               J0.addOuterProduct(nodes[i].getRestPosition(), dNds);
            }
         }
      } else {
         sampleElementRejection(elem, sampler, maxJR0, c, J0, pnt);
      }
   }

   @Override
   public void sample(Point3d coord, Point3d pnt) {
      sample(coord, null, pnt);
   }

   @Override
   public double isample(Point3d coord, Point3d pnt) {

      if (coord == null) {
         coord = new Point3d();
      }
      Matrix3d J = new Matrix3d();
      sample(coord, J, pnt);
      double detJ0 = J.determinant();

      // compute true J
      FemNode3d[] nodes = elem.getNodes();
      Vector3d dNds = new Vector3d();
      J.setZero();
      for (int i=0; i<elem.numNodes(); ++i) {
         elem.getdNds(dNds, i, coord);
         J.addOuterProduct(nodes[i].getLocalPosition(), dNds);
      }

      double detJ = J.determinant();

      return detJ0/(detJ*elem.getRestVolume());

   }
}
