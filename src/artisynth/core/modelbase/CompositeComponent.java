/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.io.*;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import java.util.*;

/**
 * ModelComponent which contains sub-components.
 */
public interface CompositeComponent
   extends ModelComponent, ComponentChangeListener, IndexedComponentList {

   /**
    * Specifies how the a composite component should be displayed in a
    * navigation panel.
    */
   public enum NavpanelDisplay {
      /**
       * Component should be visible in the panel, with named
       * children ordered according to their component ordering.
       */
      NORMAL,

      /**
       * Component should be visible in the panel, with named
       * children ordered alphebetically.
       */
      ALPHABETIC
    };   

   /**
    * Returns a specific sub-component of this ModelComponent, identified by
    * name or string representation of the sub-component's number
    * 
    * @param nameOrNumber
    * name or number of the sub-component
    * @return named sub-component, or null if the component does not exist.
    */
   public ModelComponent get (String nameOrNumber);

   /**
    * Returns a specific sub-component of this ModelComponent, identified by
    * index.
    * 
    * @param idx
    * index of the sub-component
    * @return indexed sub-component, or null if the component does not exist.
    */
   public ModelComponent get (int idx);

   /**
    * Returns a specific sub-component of this ModelComponent, identified by
    * number.
    * 
    * @param num
    * number of the sub-component
    * @return specified sub-component, or null if the component does not exist.
    */
   public ModelComponent getByNumber (int num);

   /**
    * Returns the number of components in this CompositeComponent.
    * 
    * @return number of sub-components
    */
   public int numComponents();

   /**
    * Returns the index of a specified sub-component, or -1 if that the
    * component is not present.
    *
    * @param comp component whose index is requested
    * @return indexed sub-component
    */
   public int indexOf (ModelComponent comp);

   /**
    * Recursively searches for a sub-component of this ModelComponent,
    * identified by a path of component names.
    * 
    * @param path
    * path leading to the sub-component
    * @return named sub-component, or null if the component does not exist.
    */
   public ModelComponent findComponent (String path);

   /**
    * Returns the current upper limit for numbers among all sub-components in
    * this composite. This is one greater than the maximum sub-component number
    * currently assigned. A value of 0 means that there are no sub-components.
    * This method is useful for creating and sizing arrays whose contents are
    * indexed by component numbers.
    * 
    * @return upper limit for numbers among all sub-components
    */
   public int getNumberLimit();

   /**
    * Returns the DisplayMode for this component. This specifies
    * how the component should be displayed in a navigation panel.
    *
    * @return display mode for this component
    */    
   public NavpanelDisplay getNavpanelDisplay();
      
   /**
    * Notifies this composite component that a change has occured within one or
    * more of its descendants. When this occurs, the composite may need to
    * invalidate cached information that depends on the descendants.
    * <p>
    * 
    * This method should propagate the notification up the component hierarchy
    * by calling {@link
    * artisynth.core.modelbase.CompositeComponent#notifyParentOfChange
    * notifyParentOfChange}.
    * 
    * @param e
    * optional argument giving specific information about the change
    */
   public void componentChanged (ComponentChangeEvent e);

   public void updateNameMap (
      String newName, String oldName, ModelComponent comp);

   /** 
    * Returns true if the component hierarchy formed by this component and its
    * descendents is closed with respect to references. In other words, all
    * components referenced by components within the hierarchy are themselves
    * components within the hierarchy.
    * 
    * <p>In particular, this means that one does not need to search outside the
    * hierarchy when looking for dependencies.
    * 
    * @return true if this component's hierarchy is closed with respect
    * to references.
    */
   public boolean hierarchyContainsReferences();
}
