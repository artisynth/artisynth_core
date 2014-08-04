/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.util.*;
import java.awt.Rectangle;

import artisynth.core.driver.Main;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.modelbase.*;

/**
 * This is the base class for all other component Editors. A component Editor is
 * created for each component that can be edited. A component can be edited if
 * other components can be added to it. An example of a component editor is the
 * RigidBodyEditor.
 * 
 */
public abstract class EditorBase {
   // a list of class objects of the components that can be added to the
   // component an editor is associated with
   protected Main myMain;
   protected EditorManager myEditManager;

   // flag to indicate editing actions which should be performed exclusively
   public static final int EXCLUSIVE = 0x01;
   // flag to indicate editing actions should be disabled
   public static final int DISABLED = 0x02;

   public EditorBase (Main main, EditorManager editManager) {
      init(main, editManager);
   }
   
   protected void init(Main main, EditorManager editManager) {
      myMain = main;
      myEditManager = editManager;
   }

   /**
    * Adds editing actions that can be provided by this editor, for
    * a given selection context, to an action map.
    *
    * @param actions action map to which actions are appended.
    * @param selManager
    * used to query the current selection context
    */
   public void addActions (
      EditActionMap actions, SelectionManager selManager) {
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selectedItems,
      Rectangle popupBounds) {
   }

   // public void setMouseRay(MouseRayEvent ray)
   // {
   // myRay = ray;
   // }

   // /**
   // * Add a component to the list of addable components.
   // * @param component
   // */
   // public void addComponent(Class component)
   // {
   // myAddableComponents.add(component);
   // }

   // /**
   // * Remove a component from the list of addable components.
   // * @param component
   // */
   // public void removeComponent(Class component)
   // {
   // myAddableComponents.remove(component);
   // }

   // /**
   // * Check if a particular component can be added to the component associated
   // * with the editor.
   // * @param component
   // * @return
   // */
   // public boolean isAddableComponent(Class<?> component)
   // {
   // return myAddableComponents.contains(component);
   // }

   // public ArrayList<Class<?>> getAddableComponents()
   // {
   // return myAddableComponents;
   // }

   // public ArrayList<JMenuItem> getEditingActions()
   // {
   // return myEditingActions;
   // }

   // public boolean getEditAsGroup()
   // {
   // return editAsGroup;
   // }

   /**
    * Returns true if the selection list contains a single component, which is
    * also an instance of a specified class.
    */
   public boolean containsSingleSelection (
      LinkedList<ModelComponent> selection, Class<?> cls) {
      if (selection.size() == 1 && cls.isInstance (selection.get (0))) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Returns true if the selection list contains components which are all
    * instances of a specified class.
    */
   public boolean containsMultipleSelection (
      LinkedList<ModelComponent> selection, Class<?> cls) {
      for (ModelComponent c : selection) {
         if (!cls.isInstance (c)) {
            return false;
         }
      }
      return true;
   }

   /**
    * Returns true if the selection list contains components which are all
    * instances of a specified class and have the same parent.
    */
   public boolean containsMultipleCommonParentSelection (
      LinkedList<ModelComponent> selection, Class<?> cls) {
      CompositeComponent parent = null;
      if (selection.size() > 0) {
         parent = selection.get(0).getParent();
      }
      for (ModelComponent c : selection) {
         if (!cls.isInstance (c) || c.getParent() != parent) {
            return false;
         }
      }
      return true;
   }

   /**
    * Returns true if the selection list contains two selections, which are both
    * instances of the specified class.
    */
   public boolean containsDoubleSelection (
      LinkedList<ModelComponent> selection, Class<?> cls) {
      if (selection.size() == 2 && cls.isInstance (selection.get (0)) &&
          cls.isInstance (selection.get (1))) {
         return true;
      }
      else {
         return false;
      }
   }
}
