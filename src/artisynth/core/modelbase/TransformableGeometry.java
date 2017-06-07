/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.matrix.AffineTransform3dBase;
import maspack.geometry.GeometryTransformer;

/**
 * Interface provided by model components whose geometry can be transformed.
 * This entails using a {@link GeometryTransformer} to apply either a
 * rigid or deforming transformation to the geometric attributes of
 * the component, such as point and mesh vertex positions, frame poses,
 * vector orientations, etc.
 * 
 * @author John E Lloyd
  */
public interface TransformableGeometry {
   
   /**
    * Flag indicating that the system is currently simulating.
    */
   public static final int TG_SIMULATING = 0x01;
   
   /**
    * Flag indicating that rigid body articulation constraints should
    * be enforced as the transform proceeds.
    */
   public static final int TG_ARTICULATED = 0x02;

   /**
    * Flag indicating that topology should be adjusted to preserve orientation
    * if the GeometryTransformer is reflecting (i.e., {@link
    * GeometryTransformer#isReflecting} returns <code>true</code>).  For
    * example, if this flag is set and the transform is reflecting, then the
    * vertex ordering of faces in a polygon should be reversed.
    */
   public static final int TG_PRESERVE_ORIENTATION = 0x04;
   
   /**
    * Flag indicating that transform is being applied through the GUI
    * with use of a dragger, allowing one to filter transforms.
    */
   public static final int TG_DRAGGER = 0x08;

   /**
    * Applies an affine transformation to the geometry of this component. This
    * method should be equivalent to
    * <pre>
    * TransformGeometryContext.transform (this, X, 0);
    * </pre>
    * 
    * @param X
    * affine transformation to apply to the component
    */
   public void transformGeometry (AffineTransform3dBase X);

   /**
    * Transforms the geometry of this component, using the geometry transformer
    * <code>gtr</code> to transform its individual attributes. The
    * <code>context</code> argument supplies information about what other
    * components are currently being transformed, and also allows the
    * requesting of update actions to be performed after all transform called
    * have completed. The context is also the usual entity that calls
    * this method, from within its {@link TransformGeometryContext#apply}
    * method. The argument <code>flags</code> provides flags to specify
    * various conditions associated with the the transformation. 
    * At present, the available flags are {@link #TG_SIMULATING} and 
    * {@link #TG_ARTICULATED}.
    *
    * <p>This method is not usually called directly by applications. 
    * Instead, it is typically called from within the 
    * {@link TransformGeometryContext#apply} method of the context,
    * which takes care of the various operations needed for a
    * complete transform operation, including calling 
    * {@link #addTransformableDependencies} to collect other 
    * components that should be transformed, calling 
    * {@link #transformGeometry} for each component, notifying
    * component parents that the geometry has changed, and calling
    * any requested {@link TransformGeometryAction}s. More details
    * are given in the documentation for 
    * {@link TransformGeometryContext#apply}.
    * 
    * <p>{@link TransformGeometryContext} provides a number of
    * static convenience <code>transform</code> methods
    * which take care of building the context and calling
    * <code>apply()</code> for a specified set of components.
    * 
    * <p>This method should <i>not</i>
    * generally call <code>transformGeometry()</code> for its descendant
    * components. Instead, descendants needing transformation should be
    * specified by adding them to the context in the method {@link
    * #addTransformableDependencies}.
    * 
    * @param gtr
    * transformer implementing the transform
    * @param context context information, including what other components
    * are being transformed
    * @param flags specifies conditions associated with the transformation
    */
   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags);
   
   /**
    * Adds to <code>context</code> any transformable components which should be
    * transformed as the same time as this component. This will generally
    * include descendant components, and may also include other components to
    * which this component is connected in some way.
    * 
    * <p>This method is generally called from with the 
    * {@link TransformGeometryContext#apply} method of a 
    * {@link TransformGeometryContext}.
    * 
    * @param context context information, to which the dependent components
    * are added.
    * @param flags specifies conditions associated with the transformation
    */
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags);

}
