/**
 * Copyright (c) 2015, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.*;

import maspack.geometry.GeometryTransformer;
import maspack.matrix.*;

/**
 * Defines an object used to implement transform geometry actions.
 * These actions can be requested from within the 
 * {@link TransformableGeometry#transformGeometry} method of a 
 * transformable object by calling the 
 * {@link TransformGeometryContext#addAction} method of the
 * {@link TransformGeometryContext}. Requested actions are
 * called after the <code>transformGeometry</code> method
 * has been called for all the transformables in the context,
 * and their purpose is usually to request an update operation of
 * some sort that can only be completed after all the geometry has
 * been transformed.
 */
public interface TransformGeometryAction {

   /**
    * Called from within the {@link TransformGeometryContext#apply} method
    * of a {@link TransformGeometryContext} after after the 
    * <code>transformGeometry</code> method has been called for all 
    * the transformables in the context.
    * 
    * @param gtr transformer implementing the transform
    * @param context context issuing this call
    * @param flags specifies conditions associated with the transformation.
    * At present, the available flags are 
    * {@link TransformableGeometry#TG_SIMULATING} and 
    * {@link TransformableGeometry#TG_ARTICULATED}.
    */
   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags);

}
