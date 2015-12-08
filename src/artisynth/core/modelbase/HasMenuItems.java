/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.List;

/**
 * Indicates a component that may produce menu items for inclusion under the
 * "Models" menu of the main ArtiSynth frame. The RootModel itself, and all of
 * the top-level components added to it, are checked to see if they produce
 * menu items.
 */
public interface HasMenuItems {
   
   /**
    * Returns objects to be displayed under a "Model" menu in the main
    * ArtiSynth frame. The desired objects are added to the <code>list</code>
    * argument. The objects should be be items capable of being added to a
    * JMenu, including Component, JMenuItem, and String.
    * 
    * If the method returns <code>false</code> (the default behavior), that is
    * taken to indicate that this component has no menu items.

    * @param items collects the objects that should be added
    * @return <code>false</code> if there are no items to add to the
    * model menu.
    *
    * @see maspack.widgets.GuiUtils#createMenuItem
    */
   public boolean getMenuItems(List<Object> items);
}

