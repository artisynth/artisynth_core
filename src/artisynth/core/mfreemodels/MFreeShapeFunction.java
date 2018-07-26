/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

public interface MFreeShapeFunction {
    
   /**
    * Set the rest nodes
    * @param nodes nodes
    */
   public void setNodes(MFreeNode3d[] nodes);
   
   /**
    * Current nodes used for evaluation
    * @return nodes affecting shape function
    */
   public MFreeNode3d[] getNodes();
   
   /**
    * Invalidate any stored rest information
    */
   public void invalidateRestData();
   
   /**
    * Set the evaluation coordinate
    */
   public void setCoordinate(Point3d pnt);
   
   /**
    * Current coordinate to be used for evaluation
    * @return current coordinate
    */
   public Point3d getCoordinate();
   
   /**
    * Returns the value of the i'th shape function (related to node i)
    * @param nidx node index
    * @return computed value
    */
   public double eval(int nidx);
   
   /**
    * Returns the values of the shape functions at the current point
    * @param N vector of shape functions
    */
   public void eval(VectorNd N);
   
   /**
    * Returns the value of the i'th shape function derivatives
    * @param nidx node index
    * @param dNds derivatives (d/dx, d/dy, d/dz)
    */
   public void evalDerivative(int nidx, Vector3d dNds);
   
   /**
    * Returns the value of all shape function derivatives
    * @param dNds derivatives (d/dx, d/dy, d/dz)
    */
   public void evalDerivative(Vector3d[] dNds);
   
   /**
    * Evaluates both the shape function and derivatives at a given point
    * @param N shape function outputs
    * @param dNds shape function derivative outputs
    */
   public void eval(VectorNd N, Vector3d[] dNds);
   
}
