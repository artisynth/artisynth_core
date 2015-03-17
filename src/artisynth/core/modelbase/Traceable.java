/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

/**
 * Indicates a component that has properties which are 'traceable', meaning
 * that a Tracing probe can be attached to them. A present, such properties
 * must represent either a Point3d, or a vector object of length 3.
 */
public interface Traceable extends ModelComponent {

   /**
    * Returns a list of all traceable properties in this Traceable.
    */
   public String[] getTraceables();
   
   /**
    * For a given traceable property, returns the name of a property (if any) 
    * which provides a reference position to be used for the traceable property.
    * The property name can be prepended with either a <code>+</code> or
    * <code>-</code> character, which is used to control the rendering
    * of the tracing probe in case the traceable property is a 3-vector.
    * A prepended <code>+</code> character means that the vector will
    * be rendered starting at, and directed away from the reference 
    * position (or <i>render as pull</i>), while a <code>+</code> character 
    * means that the vector will be rendered starting away from, and
    * directed into the reference position (or <i>render as push</i>).
    *
    * @return reference position property name, or <code>null</code>.
    */
   public String getTraceablePositionProperty (String traceableName);
   
}
