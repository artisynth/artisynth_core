/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.Map;
import java.util.List;

/**
 * Indicates an ArtiSynth ModelComponent that can be copied.
 */
public interface CopyableComponent extends ModelComponent {
   /**
    * Indicates that any referenced model components should themselves
    * be copied.
    */
   public static final int COPY_REFERENCES = 0x1;
   
   /**
    * For deformable components (such as FEM models), indicates that the 
    * copy should be made in the component's rest position. 
    */
   public static final int REST_POSITION = 0x2;
   
   /**
    * Create a copy of this component. If <code>COPY_REFERENCES</code>
    * is set in <code>flags</code>, then any component referenced
    * by this component should itself be set to a copy. This
    * should be done first checking <code>copyMap</code> for an 
    * existing copy of the referenced component. If there is no existing
    * copy, then a copy should be created by calling <code>copy</code>
    * recursively and adding the new copy to <code>copyMap</code>.
    *
    * @param flags flags to control the copying
    * @param copyMap map to possible existing instances of referenced
    * components
    * @return copy of this component
    */
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap);

   /**
    * Returns true if this component can be duplicated. Duplication means that
    * we can expect to be able to make a complete copy of the component along
    * with all it's external references.  This method should return
    * <code>true</code> if and only if {@link #getCopyReferences} returns true.
    *
    * <p>This method is not currently used. It is intended to provide a faster
    * way of determining if a component can be duplicated, without having to
    * use {@link #getCopyReferences} to build the list of copy references.
    *
    * @return <code>true</code> if this component can be duplicated.
    */
   public boolean isDuplicatable();
   
   /**
    * Collects external references which must also be copied in order to
    * duplicate this component. These references should exclude those which are
    * contained within a specified component hierarchy. This method should
    * return <code>true</code> if and only if {@link #isDuplicatable} returns
    * true.
    * 
    * @param refs
    * list to which references are appended
    * @param ancestor
    * root node of the hierarchy from which references are to be excluded
    * @return false if it is discovered that the component cannot be duplicated
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor);
}
