/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

public interface MutableCompositeComponent<C extends ModelComponent> extends
CompositeComponent {

   /**
    * Adds a component to this MutableCompositeComponent. The component is
    * placed in the last position and its index value is set accordingly.
    * 
    * @param comp
    * component to be adde
    * @return <code>true</code> if the component was actually added
    */
   public boolean add (C comp);

   /**
    * Adds a set of components to this MutableCompositeComponent. This routine is
    * intended to provide greater efficiency for adding a large number of
    * components at one time. If the argument <code>indices</code> is not
    * null, it specified the index locations at which the components should be
    * added. It is up to the caller to insure that the specified index values
    * make sense; in particular, they must not exceed the final size of the
    * component and no two specified indices can be the same.
    * 
    * @param comps
    * list of components to be added
    * @param indices
    * (optional) index locations of components
    * @param num
    * number of components to be added
    */
   public void addComponents (ModelComponent[] comps, int[] indices, int num);

   /**
    * Removes a component from this MutableCompositeComponent, returning true if the
    * componet was in fact found and removed.
    * 
    * @param comp
    * component to be added
    * @return true if the component was removed
    */
   public boolean remove (Object comp);

   /**
    * Removes a set of components from this MutableCompositeComponent. This routine is
    * intended to provide greater efficiency for adding a large number of
    * components at one time. If the argument <code>indices</code> is not
    * null, it is used to return the indices where the components were located.
    * 
    * @param comps
    * components to be removed
    * @param indices
    * (optional) stores the indices of the removed components
    * @param num
    * number of components to be removed
    */
   public void removeComponents (ModelComponent[] comps, int[] indices, int num);

   /**
    * Queries whether or not this component is editable from the ArtiSynth
    * GUI. In particular, if this method returns {@code false},
    * it should not be possible to use the GUU to delete child components.
    *
    * @return {@code true} if this component can be edited from the GUI.
    */
   public boolean isEditable();
}
