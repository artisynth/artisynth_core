/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui;

import java.awt.Component;
import java.util.HashMap;

import javax.swing.JComponent;

import maspack.widgets.LabeledComponentPanel;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.modelbase.ModelComponent;

public class SelectableComponentPanel extends LabeledComponentPanel implements
SelectionListener {
   private SelectionManager mySelectionManager;
   private HashMap<ModelComponent,JComponent> myComponentWidgetMap;
   private HashMap<JComponent,ModelComponent> myWidgetComponentMap;

   public SelectableComponentPanel() {
      super();
      myComponentWidgetMap = new HashMap<ModelComponent,JComponent>();
      myWidgetComponentMap = new HashMap<JComponent,ModelComponent>();
   }

   public SelectionManager getSelectionManager() {
      return mySelectionManager;
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

   public ModelComponent getComponent (JComponent widget) {
      return myWidgetComponentMap.get (widget);
   }

   public JComponent getWidget (ModelComponent comp) {
      return myComponentWidgetMap.get (comp);
   }

   public void mapWidgetToComponent (JComponent widget, ModelComponent comp) {
      if (getComponentIndex (widget) == -1) {
         throw new IllegalArgumentException ("Widget " + widget
         + " not present in panel");
      }
      if (comp == null) {
         ModelComponent oldComp = myWidgetComponentMap.remove (widget);
         if (oldComp != null) {
            myComponentWidgetMap.remove (oldComp);
         }
      }
      else {
         myWidgetComponentMap.put (widget, comp);
         myComponentWidgetMap.put (comp, widget);
      }
   }

   public boolean removeWidget (Component widget) {
      if (getComponentIndex (widget) != -1) {
         if (widget instanceof JComponent) {
            mapWidgetToComponent ((JComponent)widget, null);
         }
         super.removeWidget (widget);
         return true;
      }
      else {
         return false;
      }
   }

   public void selectWidget (JComponent widget) {
      super.selectWidget (widget);
      ModelComponent comp = myWidgetComponentMap.get (widget);
      if (mySelectionManager != null && comp != null && !comp.isSelected()) {
         mySelectionManager.addSelected (comp);
      }
   }

   public void deselectWidget (JComponent widget) {
      super.deselectWidget (widget);
      ModelComponent comp = myWidgetComponentMap.get (widget);
      if (mySelectionManager != null && comp != null && comp.isSelected()) {
         mySelectionManager.removeSelected (comp);
      }
   }

   public void deselectAllWidgets() {
      super.deselectAllWidgets();
      if (mySelectionManager != null) {
         mySelectionManager.clearSelections();
      }
   }

   public void selectionChanged (SelectionEvent e) {
      JComponent widget;
      for (ModelComponent c : e.getRemovedComponents()) {
         if ((widget = myComponentWidgetMap.get (c)) != null) {
            super.deselectWidget (widget);
         }
      }
      for (ModelComponent c : e.getAddedComponents()) {
         if ((widget = myComponentWidgetMap.get (c)) != null) {
            super.selectWidget (widget);
         }
      }
   }

}
