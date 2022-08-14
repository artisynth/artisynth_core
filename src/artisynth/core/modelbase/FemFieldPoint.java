package artisynth.core.modelbase;

import maspack.matrix.Point3d;

/**
 * Provides information about an FEM integration point that can be used to
 * evaluate the value of a {@link FieldComponent} at that point.
 */
public interface FemFieldPoint {

   /**
    * Returns the point's rest position.
    */
   Point3d getRestPos();

   /**
    * Returns the point's current spatial position.
    */
   Point3d getSpatialPos();

   /**
    * Returns an integer describing the type of element containing the point.
    * 0 indicates a volumetric element, 1 indicates a shell element.
    */
   int getElementType();

   /**
    * Returns the number of the element containing the point.
    */
   int getElementNumber();

   /**
    * Returns the index of the integration point within the element.  This
    * should be in the range 0 to n-1, where n is the total number of element
    * integration point, including the warping point.
    */
   int getElementSubIndex();

   /**
    * Returns the node numbers of the element containing the point.
    */
   int[] getNodeNumbers();   

   /**
    * Returns the nodal weights of the point within the element, with each
    * weight corresponding to the node whose number is returned by {@link
    * #getNodeNumbers}.
    */
   double[] getNodeWeights();

}
