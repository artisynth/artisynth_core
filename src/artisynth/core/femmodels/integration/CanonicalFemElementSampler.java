package artisynth.core.femmodels.integration;

import artisynth.core.femmodels.FemNode3d;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

/**
 * Samples uniformly from the canonical element shape (i.e. natural coordinates)
 */
public class CanonicalFemElementSampler extends FemElementSamplerBase {

   @Override
   public void sample(Point3d coord, Point3d pnt) {
      if (coord == null) {
         coord = new Point3d();
      }
      sampler.sampleCoord(coord);
      FemNode3d[] nodes = elem.getNodes();
      
      if (pnt != null) {
         pnt.setZero();
         for (int i=0; i<nodes.length; ++i) {
            pnt.scaledAdd(elem.getN(i, coord), nodes[i].getPosition());
         }
      }
   }
   
   @Override
   public double isample(Point3d coord, Point3d pnt) {
      sample(coord, pnt);
      
      // compute J and density
      Matrix3d J = new Matrix3d();
      Matrix3d Js = new Matrix3d();
      double[] scoords = elem.getNodeCoords();
      FemNode3d[] nodes = elem.getNodes();
      Vector3d dNds = new Vector3d();
      for (int i=0; i<elem.numNodes(); ++i) {
         elem.getdNds(dNds, i, coord);
         J.addOuterProduct(nodes[i].getLocalPosition(), dNds);
         Js.addOuterProduct(scoords[3*i], scoords[3*i+1], scoords[3*i+2], 
            dNds.x, dNds.y, dNds.z);
      }   
      double detJ = J.determinant();
      double detJs = Js.determinant();
      // probability density is detS/(detJ*vol0)
      return detJs/(detJ*sampler.volume());
   }

}
