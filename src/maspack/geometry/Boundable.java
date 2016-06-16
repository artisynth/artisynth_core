/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;

/**
 * Methods to be implemented by geometric elements that can be enclosed within
 * a bounding volume tree.
 *
 * @author lloyd
 */
public interface Boundable {

   /**
    * Returns the number of points associated with this element, if any, or
    * zero otherwise. If the element has points, then it's spatial extent is
    * assumed to be enclosed within their convex hull. At present, elements
    * that do not have points cannot be enclosed within oriented bounding box
    * (OBB) trees.
    *
    * @return number of points associated with this element
    */
   public int numPoints();

   /**
    * Returns the <code>idx</code>-th point associated with this element.
    * 
    * @param idx index of the point (must be on the range 0 to
    * {@link #numPoints}).
    * @return <code>idx</code>-th point associated with this element.
    * Must not be modified.
    */
   public Point3d getPoint (int idx);

   /**
    * Computed the centroid of this element.
    * 
    * @param centroid returns the computed centroid value.
    */
   public void computeCentroid (Vector3d centroid);

   /**
    * Updates the axis-aligned bounds of this element. The value in
    * <code>min</code> and <code>max</code> should be decreased or increased,
    * respectively, so that all spatial points associated with this element lie
    * within the axis-aligned box defined by <code>min</code> and
    * <code>max</code>.
    *
    * @param min minimum values to be updated
    * @param max maximum values to be updated
    */
   public void updateBounds (Vector3d min, Vector3d max);
   
   /**
    * Computes the covariance of the element, assuming a uniform density
    * of one. The covariance is defined as
    * <pre>
    * int_V \rho x x^T dV,
    * </pre>
    * where <code>\rho</code> is the density, <code>x</code> is any
    * spatial point within the element, and the integral is evaluated
    * over the element's spatial extent. The method returns the element's
    * spatial size, which for elements of dimension 3, 2, 1, or 0 will
    * be a volume, area, length, or discrete value.
    *
    * <p> Implementation of this method is optional, with non-implementation
    * indicated by having the method return -1. Non-implementation may prevent
    * the element from being enclosed within certain types of oriented bounding
    * box (OBB) constructions.
    *
    * @param C returns the covariance
    * @return spatial size of the element, or -1 if this method is not
    * implemented.
    */
   public double computeCovariance (Matrix3d C);

}
