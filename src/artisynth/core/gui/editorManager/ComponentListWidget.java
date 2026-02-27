/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.util.*;

import artisynth.core.gui.selectionManager.SelectionListener;

import artisynth.core.modelbase.*;
import artisynth.core.driver.*;
import artisynth.core.gui.selectionManager.*;

import maspack.util.ListView;
import maspack.widgets.ButtonMasks;

public class ComponentListWidget<E extends ModelComponent> extends
AbstractListModel implements SelectionListener, ListSelectionListener {
   private CompositeComponent myAncestor;
   private ListView<E> myListView;
   private ArrayList<E> myLocalList;
   private boolean myIncremental = false;
   // private boolean mySelectionEnabled = true;
   private SelectionManager mySelectionManager;
   private JList myJList;


   private class ListMouseAdapter extends MouseInputAdapter {

      public void mousePressed(MouseEvent e) {
         if (e.getModifiersEx() == ButtonMasks.getContextMenuMask()) {
            mySelectionManager.displayMinimalPopup(e);
         }
      }
   }
   // private Color myDefaultSelectionColor;

   public ComponentListWidget (ListView<E> list, CompositeComponent ancestor) {
      myJList = new JList();
      // myDefaultSelectionColor = myJList.getSelectionBackground();
      myListView = list;
      myAncestor = ancestor;
      myLocalList = new ArrayList<E>();
      myJList.setModel (this);
      myJList.addMouseListener (new ListMouseAdapter());
      // Not entirely sure what lead anchor notification is, but it
      // seems to have the effect of generating valueChange events
      // from the selection model that have indices which are out
      // bounds.
      ListSelectionModel selectionModel = myJList.getSelectionModel();
      if (selectionModel instanceof DefaultListSelectionModel) {
         DefaultListSelectionModel defSelModel =
            (DefaultListSelectionModel)selectionModel;
         // defSelModel.setLeadAnchorNotificationEnabled (false);
      }
      myJList.addListSelectionListener (this);
   }
   
   /**
    * Sets the component list and associated ancestor which this widget 
    * provides a view. A list value of null will clear the display list 
    * and no widgets will be displayed.
    * 
    * @param list new component list
    */
   public void setComponentList (
      ListView<E> list, CompositeComponent ancestor) {
      myListView = list;
      myAncestor = ancestor;
      update();
   }

   /**
    * Returns the ancestor component associated with this component list.
    * 
    * @return current ancestor component
    */
   public CompositeComponent getAncestor() {
      return myAncestor;
   }

   /**
    * Sets the ancestor component associated with this component list.
    * 
    * @param ancestor
    * new component
    */
   public void setAncestor (CompositeComponent ancestor) {
      myAncestor = ancestor;
   }

   protected String getName (E comp, CompositeComponent ancestor) {
      if (comp.getParent() == null) {
         String name = comp.getName();
         if (name == null) {
            return Integer.toString (comp.getNumber());
         }
         else {
            return name;
         }
      }
      else {
         return ComponentUtils.getPathName (ancestor, comp);
      }
   }

   /**
    * Get the string name of the component at the specified location in the
    * list.
    */
   public Object getElementAt (int idx) {
      return getName (myLocalList.get (idx), myAncestor);
   }

   /**
    * Returns the number of components in the list.
    * 
    * @return number of components in the list
    */
   public int getSize() {
      return myLocalList.size();
   }

   /**
    * Get the component at the specified position.
    * 
    * @param idx
    * The index of the component in the list.
    * @return The component at the specified position
    */
   public E get (int idx) {
      return myLocalList.get (idx);
   }

   /**
    * Get an array of the components in the list.
    */
   public E[] toArray (E[] dummy) {
      return myLocalList.toArray (dummy);
   }

   public void update() {
      if (myLocalList.size() > 0) {
         fireIntervalRemoved (this, 0, myLocalList.size() - 1);
      }
      myLocalList.clear();
      if (myListView != null) {
         for (int i = 0; i < myListView.size(); i++) {
            myLocalList.add (myListView.get (i));
         }
      }
      int savedSelectionMode = myJList.getSelectionMode();
      myJList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
      if (myLocalList.size() > 0) {
         fireIntervalAdded (this, 0, myLocalList.size() - 1);
         for (int i = 0; i < myLocalList.size(); i++) {
            if (myLocalList.get (i).isSelected()) {
               myJList.addSelectionInterval (i, i);
            }
         }
      }
      myJList.setSelectionMode (savedSelectionMode);
   }

   public void setSelectionManager (SelectionManager manager) {
      if (mySelectionManager != null) {
         mySelectionManager.removeSelectionListener (this);
      }
      mySelectionManager = manager;
      if (mySelectionManager != null) {
         mySelectionManager.addSelectionListener (this);
      }
   }

   private boolean myMaskValueChanged = false;

   /**
    * Handles selection events produced by the JList. If selection is enabled,
    * then these events should be passed on to the selection manager.
    */
   public void valueChanged (ListSelectionEvent e) {
      if (mySelectionManager != null && !myMaskValueChanged) {
         // mySelectionManager.clearSelections();
         for (int i = e.getFirstIndex(); i <= e.getLastIndex(); i++) {
            // with lead anchor notification enabled, indices can be
            // out of bounds (not sure why), so we have to guard against this.
            if (i >= 0 && i < myLocalList.size()) {
               ModelComponent comp = myLocalList.get (i);
               if (myJList.isSelectedIndex (i)) {
                  if (!comp.isSelected()) {
                     if (!mySelectionManager.addSelected (comp)) {
                        myJList.removeSelectionInterval (i, i);
                        System.out.println (" removing " + i);
                     }
                  }
               }
               else {
                  if (comp.isSelected()) {
                     mySelectionManager.removeSelected (comp);
                  }
               }
            }
         }
      }
   }

   public void selectionChanged (SelectionEvent e) {
      if (myListView == null) {
         return;
      }
      for (ModelComponent c : e.getRemovedComponents()) {
         if (myListView.contains (c)) {
            int idx = myLocalList.indexOf (c);
            if (idx != -1) {
               myMaskValueChanged = true;
               myJList.removeSelectionInterval (idx, idx);
               myMaskValueChanged = false;
            }
         }
      }
      for (ModelComponent c : e.getAddedComponents()) {
         if (myListView.contains (c)) {
            int idx = myLocalList.indexOf (c);
            if (idx != -1) {
               myMaskValueChanged = true;
               myJList.addSelectionInterval (idx, idx);
               myMaskValueChanged = false;
            }
         }
      }
   }

   public JList getJList() {
      return myJList;
   }
}
