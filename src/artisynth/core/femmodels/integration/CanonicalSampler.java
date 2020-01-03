package artisynth.core.femmodels.integration;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.HexElement;
import artisynth.core.femmodels.PyramidElement;
import artisynth.core.femmodels.QuadhexElement;
import artisynth.core.femmodels.QuadpyramidElement;
import artisynth.core.femmodels.QuadtetElement;
import artisynth.core.femmodels.QuadwedgeElement;
import artisynth.core.femmodels.TetElement;
import artisynth.core.femmodels.WedgeElement;
import maspack.matrix.Point3d;

/**
 * Samples uniformly from the canonical shape of a Finite Element
 * @author Antonio
 *
 */
public abstract class CanonicalSampler {

   /**
    * Spatial position within canonical element
    * @param pnt output sample point
    */
   public abstract void sample(Point3d pnt);
   
   /**
    * Natural coordinate within canonical element
    * @param coord sample canonical coordinate
    */
   public void sampleCoord(Point3d coord) {
      sample(coord);
   }
   
   /**
    * Volume of canonical element
    * @return volume
    */
   public abstract double volume();

   /**
    * Samples uniformly from within the basic Tet
    */
   public static class CanonicalTetSampler extends CanonicalSampler {
      @Override
      public void sample(Point3d pnt) {
         MonteCarloSampler.sampleTet(pnt);
      }
      public double volume() {
         return 1.0/6;
      }
      
      public static CanonicalTetSampler INSTANCE = new CanonicalTetSampler();
   }

   /**
    * Samples uniformly from within basic pyramid
    */
   public static class CanonicalPyramidSampler extends CanonicalSampler {
      @Override
      public void sample(Point3d pnt) {
         MonteCarloSampler.samplePyramid(pnt);
      }
      
      @Override
      public void sampleCoord(Point3d coord) {
         sample(coord);
         // convert back to natural coordinates
         if (coord.z < 1-1e-15) {
            coord.x = coord.x*2/(1-coord.z);
            coord.y = coord.y*2/(1-coord.z);  
         }
      }
      
      public double volume() {
         return 8.0/3.0;
      }
      public static CanonicalPyramidSampler INSTANCE = new CanonicalPyramidSampler();
   }

   /**
    * Samples uniformly from within basic wedge
    */
   public static class CanonicalWedgeSampler extends CanonicalSampler {
      @Override
      public void sample(Point3d pnt) {
         MonteCarloSampler.sampleWedge(pnt);
      }
      public double volume() {
         return 1.0;
      }
      public static CanonicalWedgeSampler INSTANCE = new CanonicalWedgeSampler();
   }

   /**
    * Samples uniformly from within basic hex
    */
   public static class CanonicalHexSampler extends CanonicalSampler {
      @Override
      public void sample(Point3d pnt) {
         MonteCarloSampler.sampleHex(pnt);
      }
      public double volume() {
         return 8.0;
      }
      public static CanonicalHexSampler INSTANCE = new CanonicalHexSampler();
   }

   /**
    * Gets the appropriate sampler for a given FEM
    */
   public static CanonicalSampler get(FemElement3d elem) {
      if ((elem instanceof TetElement) || (elem instanceof QuadtetElement)) {
         return CanonicalTetSampler.INSTANCE;
      } else if ((elem instanceof PyramidElement) || (elem instanceof QuadpyramidElement)) {
         return CanonicalPyramidSampler.INSTANCE;
      } else if ((elem instanceof WedgeElement) || (elem instanceof QuadwedgeElement)) {
         return CanonicalWedgeSampler.INSTANCE;
      } else if ((elem instanceof HexElement) || (elem instanceof QuadhexElement)) {
         return CanonicalHexSampler.INSTANCE;
      } else {
         throw new IllegalArgumentException("Unknown element type: " + elem.getClass());
      }      
   }

}
