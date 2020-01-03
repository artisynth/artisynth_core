package artisynth.core.femmodels.integration;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.TetElement;
import artisynth.core.femmodels.integration.CanonicalSampler.CanonicalTetSampler;
import maspack.matrix.Point3d;

public abstract class FemElementSamplerBase implements FemElementSampler {

   // cached element
   FemElement3d elem;
   CanonicalSampler sampler;
   
   public FemElementSamplerBase() {
      elem = null;
      sampler = null;
   }
   
   @Override
   public void setElement(FemElement3d elem) {
      if (elem != this.elem) {
         this.elem = elem;
         this.sampler = null;
         if (elem != null) {
            this.sampler = CanonicalSampler.get(elem);
            elem.computeVolumes();
         }
      }
   }
     
   @Override
   public Point3d sample() {
      Point3d pnt = new Point3d();
      sample(pnt);
      return pnt;
   }
   
   @Override
   public void sample(Point3d pnt) {
      sample((Point3d)null, pnt);
   }
   
   /**
    * Samples uniformly from with a tet element
    * @param tet
    * @param coord
    * @param J
    * @param pnt
    */
   protected static void sampleTet(TetElement tet, Point3d coord, Point3d pnt) {
      if (coord == null) {
         coord = new Point3d();
      }
      CanonicalTetSampler.INSTANCE.sampleCoord(coord);
      
      // compute final point based on shape functions
      if (pnt != null) {
         FemNode3d[] nodes = tet.getNodes();
         pnt.setZero();
         for (int i=0; i<tet.numNodes(); ++i) {
            double d = tet.getN(i, coord);
            pnt.scaledAdd(d, nodes[i].getPosition());
         }
      }
   }
   
}
