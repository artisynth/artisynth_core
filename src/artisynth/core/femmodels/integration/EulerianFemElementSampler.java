package artisynth.core.femmodels.integration;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemNode3d;
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
public class EulerianFemElementSampler extends FemElementSamplerBase {
   
   /**
    * Tries to estimate the maximum Jacobian of an element.
    * Computes the Jacobian at each of the integration points, computes the element volume,
    * then if the integration points provide a poor estimate of the volume, does some random
    * sampling.
    * @param e element of which to estimate the maximum Jacobian
    * @param canonicalSampler sampler for canonical volume
    * @return an estimated maximum Jacobian
    */
   private static double estimateMaxJacobianRatio(FemElement3d e, CanonicalSampler canonicalSampler) {
      e.computeVolumes();
      double v = e.getVolume();
      double cv = canonicalSampler.volume();  // canonical volume
      double ev = 0; // estimated volume
      double Jmax = 0;

      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
      Vector3d dNds = new Vector3d();
      double[] ncoords = e.getNodeCoords();
      Matrix3d Js = new Matrix3d();
      for (IntegrationPoint3d ipnt : ipnts) {
         double detJ = ipnt.computeJacobianDeterminant(e.getNodes());
         Js.setZero();
         for (int j=0; j<e.numNodes(); ++j) {
            e.getdNds(dNds, j, ipnt.getCoords());
            Js.addOuterProduct(ncoords[3*j],ncoords[3*j+1], ncoords[3*j+2], 
               dNds.x, dNds.y, dNds.z);
         }
         //double detJ = ipnt.getJ().determinant();
         double detJR = detJ/Js.determinant();
         if (detJR > Jmax) {
            Jmax = detJR;
         }
         ev += detJ*ipnt.getWeight();
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
            J.addOuterProduct(nodes[j].getLocalPosition(), dNds);
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
   //   private static double estimateMinJacobian(FemElement3d e, CanonicalSampler canonicalSampler) {
   //      
   //      double minJ = e.computeVolumes();
   //      if (minJ <= 0) {
   //         return 0;
   //      }
   //      
   //      double v = e.getVolume();
   //      double cv = canonicalSampler.volume();  // canonical volume
   //      double ev = 0;                          // estimated volume
   //      
   //      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
   //      for (IntegrationPoint3d ipnt : ipnts) {
   //         ipnt.computeJacobian(e.getNodes());
   //         double detJ = ipnt.getJ().determinant();
   //         if (detJ < minJ) {
   //            minJ = detJ;
   //         }
   //         ev += detJ*ipnt.getWeight();
   //      }
   //
   //      // volume is within 5%
   //      double vfrac = ev/v;
   //      if (vfrac >= 0.95 && vfrac <= 1.05) {
   //         return minJ;
   //      }
   //      
   //      // do some random sampling until we have a good estimate of the volume
   //      double Jsum = 0;
   //      int count = 0;
   //      Point3d c = new Point3d();
   //      Vector3d dNds = new Vector3d();
   //      Matrix3d J = new Matrix3d();
   //      FemNode3d[] nodes = e.getNodes();
   //      for (int i=0; i<10000; ++i) {
   //         canonicalSampler.sample(c);
   //         // compute detJ
   //         J.setZero();
   //         for (int j=0; j<e.numNodes(); ++j) {
   //            e.getdNds(dNds, j, c);
   //            J.addOuterProduct(dNds, nodes[j].getLocalPosition());
   //         }   
   //         double detJ = J.determinant();
   //         if (detJ < minJ) {
   //            minJ = detJ;
   //         }
   //         Jsum += detJ;
   //         ++count;
   //         vfrac = Jsum/count*cv/v;
   //         
   //         // exit early if we have a good volume estimate already
   //         if (i > 30 && vfrac >= 0.95 && vfrac <= 1.05) {
   //            return minJ;
   //         }
   //      }
   //      
   //      // haven't found good estimate, guess at what value we're missing
   //      // (Jsum + detJ)/(count+1)*cv = v
   //      double detJ = v*(count+1)/cv-Jsum;
   //      if (detJ < minJ) {
   //         minJ = detJ;
   //      }
   //      return minJ;
   //   }
   
   /**
    * Uses a sample rejection technique to compute a random sample within an element.
    * @param elem element
    * @param canonicalSampler samples canonical element
    * @param maxJR maximum jacobian ratio (compared to canonical element)
    * @param c natural coordinates at sample position
    * @param J jacobian at sample position
    * @param pnt final point
    */
   public static void sampleElementRejection(FemElement3d elem, CanonicalSampler canonicalSampler, 
      double maxJR, Point3d c, Matrix3d J, Point3d pnt) {
      
      // cap Jacobian so points can still be selected
      if (maxJR > 1e5*elem.getVolume()) {
         maxJR = 1e5*elem.getVolume();
      }
      
      if (c == null) {
         c = new Point3d();
      }
      if (J == null) {
         J = new Matrix3d();
      }
      
      FemNode3d[] nodes = elem.getNodes();
      Vector3d dNds = new Vector3d();
      Matrix3d Js = new Matrix3d();
      double[] ncoords = elem.getNodeCoords();
      
      // probability is (detJ(x)/detJs(x))/vol0
      // upper bound is max(detJ(x)/detJs(x))/vol0
      boolean accept = false;
      while (!accept) {
         canonicalSampler.sampleCoord(c);
         // compute detJ
         J.setZero();
         Js.setZero();
         for (int i=0; i<elem.numNodes(); ++i) {
            elem.getdNds(dNds, i, c);
            J.addOuterProduct(nodes[i].getLocalPosition(), dNds);
            Js.addOuterProduct(ncoords[3*i],ncoords[3*i+1], ncoords[3*i+2], 
               dNds.x, dNds.y, dNds.z);
         }   
         double detJR = J.determinant()/Js.determinant();
         double w = RandomGenerator.nextDouble(0, maxJR);
         if (w < detJR) {
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
   double maxJR;
   
   public EulerianFemElementSampler() {
      super();
      istet = false;
      maxJR = 0;
   }

   public void setElement(FemElement3d elem) {
      if (this.elem != elem) {
         super.setElement(elem);
         this.elem = elem;
         this.sampler = null;
         this.maxJR = 0;
         this.istet = false;
         if (elem != null) {
            this.sampler = CanonicalSampler.get(elem);
            if (elem instanceof TetElement) {
               istet = true;
            } else {
               this.maxJR = estimateMaxJacobianRatio(elem, sampler);
            }
         }
      }
   }
   
   public void sample(Point3d c, Matrix3d J, Point3d pnt) {
      if (istet) {
         if (c == null) {
            c = new Point3d();
         }
         sampleTet((TetElement)elem, c, pnt);
         // compute J
         if (J != null) {
            FemNode3d[] nodes = elem.getNodes();
            Vector3d dNds = new Vector3d();
            J.setZero();
            for (int i=0; i<elem.numNodes(); ++i) {
               elem.getdNds(dNds, i, c);
               J.addOuterProduct(nodes[i].getLocalPosition(), dNds);
            }
         }
      } else {
         sampleElementRejection(elem, sampler, maxJR, c, J, pnt);
      }
   }
   
   @Override
   public void sample(Point3d coord, Point3d pnt) {
      sample(coord, null, pnt);
   }
   
   @Override
   public double isample(Point3d coord, Point3d pnt) {
      sample(coord, null, pnt);
      return 1/elem.getVolume();
   }
}
