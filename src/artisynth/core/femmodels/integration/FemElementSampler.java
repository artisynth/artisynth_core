package artisynth.core.femmodels.integration;

import artisynth.core.femmodels.FemElement3d;
import maspack.matrix.Point3d;

public interface FemElementSampler {
   
   /**
    * Sets the element to sample from
    * @param elem element to sample from
    */
   public void setElement(FemElement3d elem);
   
   /**
    * Generates a random point from within the previously used or assigned element
    * @return generated point
    */
   public Point3d sample();

   /**
    * Generates a sample point from within the previously used or assigned finite element
    * @param pnt populated sampled point
    */
   public void sample(Point3d pnt);
      
   /**
    * Generates a sample point from within the previously used or assigned finite element
    * @param coord sampled local coordinate within the element (natural coordinates)
    * @param pnt spatial location of point
    */
   public void sample(Point3d coord, Point3d pnt);

   /**
    * Generates a sample point from within the previously used or assigned finite element,
    * returns probability density of point, used mainly for numerical integration
    * @param coord sampled local coordinate within the element (natural coordinates)
    * @param pnt spatial location of point
    * @return probability density function at sampled point
    */
   public double isample(Point3d coord, Point3d pnt);
   
}
