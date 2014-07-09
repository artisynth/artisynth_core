/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import maspack.matrix.AffineTransform3dBase;

public interface TransformableGeometry {
   
   /*
    * Indicates that the system is currently simulating.
    */
   public static final int SIMULATING = 0x01;
   
   /*
    * Indicates that articulation constraints will be enforced as
    * the transform proceeds.
    */
   public static final int ARTICULATED = 0x02;
   
   /**
    * Applies an affine transformation to the geometry of this object. This
    * method should be equivalent to
    * 
    * <pre>
    * transformGeometry (X, this, 0);
    * </pre>
    * 
    * @param X
    * affine transformation
    */
   public void transformGeometry (AffineTransform3dBase X);

   /**
    * Applies an affine transformation to the geometry of this object. If
    * recursively invoked within a component hierarchy, then
    * <code>topComponent</code> should be the component for which the method
    * was initially invoked. The variable <code>flags</code> provides
    * information about the context in which the transformation is
    * being applied. At present, the available flags are 
    * {@link #SIMULATING} and {@link #ARTICULATED}.
    * 
    * @param X
    * affine transformation
    * @param topObject
    * component on which the method was initially invoked
    * @param flags provides information about the context in which the
    * transformation is being applied.
    */
   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags);

}
