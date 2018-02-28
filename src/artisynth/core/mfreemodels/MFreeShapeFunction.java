/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

public interface MFreeShapeFunction {
    
   /**
    * Current coordinate to be used for evaluation
    * @return current coordinate
    */
   public Point3d getCoordinate();
   
   /**
    * Current nodes used for evaluation
    * @return nodes affecting shape function
    */
   public MFreeNode3d[] getNodes();
   
   /**
    * Update internals for computing the shape function value and derivatives at the
    * given point
    * @param pnt  point at which to evaluate shape functions
    * @param nodes dependent nodes
    */
   public void update(Point3d pnt, MFreeNode3d[] nodes);
   
   /**
    * Returns the value of the i'th shape function (related to node i)
    * @param nidx node index
    * @return computed value
    */
   public double eval(int nidx);
   
   /**
    * Returns the value of the i'th shape function derivatives
    * @param nidx node index
    * @param dNds derivatives (d/dx, d/dy, d/dz)
    */
   public void evalDerivative(int nidx, Vector3d dNds);

   /**
    * Update only if coords and nodes are different
    * @param coords natural coordinates
    * @param myNodes nodes for shape function
    * @return true if updated
    */
   public boolean maybeUpdate(Vector3d coords, MFreeNode3d[] myNodes);
   
}
