/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import maspack.properties.EditingProperty;
import maspack.properties.HasProperties;
import maspack.properties.HostList;
import maspack.properties.PropTreeCell;
import maspack.properties.Property;
import maspack.util.InternalErrorException;

public class PropertyFrame extends JFrame implements ActionListener,
   PropertyWindow {
   private static final long serialVersionUID = -431713572103062517L;
   protected PropertyPanel myPanel;
   protected boolean myScrollableP = false;
   protected JScrollPane myScrollPane;
   protected OptionPanel myOptionPanel;
   private int myReturnValue = JOptionPane.NO_OPTION;
   HostList myHostList;
   PropTreeCell myTree;

   private boolean myLiveUpdatingEnabled = true;

   /**
    * Should be called in the constructor, after the Window has been initialized
    * and getContentPane() returns a valid container.
    */
   protected void initialize() {
      getContentPane().setLayout (
         new BoxLayout (getContentPane(), BoxLayout.Y_AXIS));
   }

   public OptionPanel addOptionPanel (String options) {
      JSeparator sep = new JSeparator();
      sep.setAlignmentX (Component.LEFT_ALIGNMENT);
      getContentPane().add (GuiUtils.createBoxFiller());
      getContentPane().add (sep);

      myOptionPanel = new OptionPanel (options, this);
      myOptionPanel.setAlignmentX (Component.LEFT_ALIGNMENT);
      // GuiUtils.setFixedHeight (
      // myOptionPanel, myOptionPanel.getPreferredSize().height);
      getContentPane().add (myOptionPanel);
      return myOptionPanel;
   }

   public OptionPanel getOptionPanel () {
      return myOptionPanel;
   }
      
   private JScrollPane createScrollPane() {
      JScrollPane pane =  new JScrollPane (myPanel);
      pane.setAlignmentX (Component.LEFT_ALIGNMENT);
      // Use our own layout manager to remove a bug whereby the scroll pane
      // uses the existing size of the view client, rather than the preferred
      // size, when a window is repacked. This causes problems because the
      // existing size is not necessarily the size that will result when the
      // repack is complete.
      pane.setLayout (new maspack.widgets.ScrollPaneLayout());
      return pane;
   }

   /**
    * Should be called once, and only once, in the constructor to initialize the
    * PropertyPanel for this window.
    */
   protected void setPanel (PropertyPanel panel) {
      myPanel = panel;
      // myPanel.addVerticalFiller();
      myPanel.setAlignmentX (Component.LEFT_ALIGNMENT);
      if (myScrollableP) {
         myScrollPane = createScrollPane();
         getContentPane().add (myScrollPane, 0);
      }
      else {
         getContentPane().add (myPanel, 0);
      }
   }

   public void setScrollable (boolean scrollable) {
      if (scrollable != myScrollableP) {
         myScrollableP = scrollable;
         if (myPanel != null) {
            if (scrollable) {
               getContentPane().remove (myPanel);
               myScrollPane = createScrollPane();
               getContentPane().add (myScrollPane, 0);
            }
            else {
               getContentPane().remove (myScrollPane);
               getContentPane().add (myPanel, 0);
            }
         }
         pack();
         repaint();
      }
   }

   public boolean isScrollable() {
      return myScrollableP;
   }

   public Component addWidget (Component comp) {
      myPanel.addWidget (comp);
      return comp;
   }

   public LabeledComponentBase addWidget (Property prop) {
      LabeledComponentBase comp = myPanel.addWidget (prop);
      return comp;
   }

   public LabeledComponentBase addWidget (Property prop, double min, double max) {
      LabeledComponentBase comp = myPanel.addWidget (prop, min, max);
      return comp;
   }

   public LabeledComponentBase addWidget (HasProperties host, String name) {
      Property prop = host.getProperty (name);
      if (prop != null) {
         return addWidget (prop);
      }
      else {
         return null;
      }
   }

   public LabeledComponentBase addWidget (
      HasProperties host, String name, double min, double max) {
      Property prop = host.getProperty (name);
      if (prop != null) {
         return addWidget (prop, min, max);
      }
      else {
         return null;
      }
   }

   public LabeledComponentBase addWidget (
      String labelText, HasProperties host, String name) {
      LabeledComponentBase widget = addWidget (host, name);
      if (widget != null) {
         widget.setLabelText (labelText);
      }
      return widget;
   }

   public LabeledComponentBase addWidget (
      String labelText, HasProperties host, String name, double min, double max) {
      LabeledComponentBase widget = addWidget (host, name, min, max);
      if (widget != null) {
         widget.setLabelText (labelText);
      }
      return widget;
   }

   public PropertyPanel getPropertyPanel() {
      return myPanel;
   }

   static PropertyPanel createPanelFromHostList (
      PropTreeCell tree, HostList hostList) {
      hostList.saveBackupValues (tree);
      hostList.getCommonValues (tree, /* live= */true);

      return (new PropertyPanel (EditingProperty.createProperties (
         tree, hostList, /* isLive= */true)));
   }

   public void actionPerformed (ActionEvent e) {
      String actionCmd = e.getActionCommand();
      if (actionCmd.equals ("OK") || actionCmd.equals ("Done")
      || actionCmd.equals ("Close")) {
         myReturnValue = OptionPanel.OK_OPTION;
         setVisible (false);
         dispose();
      }
      else if (actionCmd.equals ("Reset")) {
         if (myHostList != null) {
            myHostList.restoreBackupValues();
            myHostList.getCommonValues (myTree, /* live= */true);
            //updateWidgetValues();
            fireGlobalValueChangeListeners();
         }
      }
      else if (actionCmd.equals ("Cancel")) {
         myReturnValue = OptionPanel.CANCEL_OPTION;
         if (myHostList != null) {
            myHostList.restoreBackupValues();
            fireGlobalValueChangeListeners();
         }
         setVisible (false);
         dispose();
      }
      else if (actionCmd.equals ("LiveUpdate")) {
         boolean enabled = !isLiveUpdatingEnabled();
         enableLiveUpdating (enabled);
         myOptionPanel.setLiveUpdateEnabled (enabled);
      }
      else {
         throw new InternalErrorException ("Unknown action: " + actionCmd);
      }
   }

   public int getReturnValue() {
      return myReturnValue;
   }

   public String toString() {
      return super.toString();
   }

   public void dispose() {
      // ListIterator<PropertyWindowBase> lit = myChildren.listIterator();
      // while (!myChildren.isEmpty())
      // { PropertyWindowBase window = myChildren.get(0);
      // window.dispose();
      // }
      // if (myParent != null)
      // { myParent.removeChildWindow (this);
      // }
      if (myPanel != null) {
         myPanel.dispose();
      }
      super.dispose();
   }

   public void updateWidgetValues() {
      myPanel.updateWidgetValues();
      Window[] owned = getOwnedWindows();
      for (int i = 0; i < owned.length; i++) {
         if (owned[i] instanceof PropertyWindow) {
            ((PropertyWindow)owned[i]).updateWidgetValues();
         }
      }
   }

   public void addGlobalValueChangeListener (ValueChangeListener l) {
      myPanel.addGlobalValueChangeListener (l);
      Window[] owned = getOwnedWindows();
      for (int i = 0; i < owned.length; i++) {
         if (owned[i] instanceof PropertyWindow) {
            ((PropertyWindow)owned[i]).addGlobalValueChangeListener (l);
         }
      }
   }

   public void removeGlobalValueChangeListener (ValueChangeListener l) {
      if (myPanel.removeGlobalValueChangeListener (l)) {
         Window[] owned = getOwnedWindows();
         for (int i = 0; i < owned.length; i++) {
            if (owned[i] instanceof PropertyWindow) {
               ((PropertyWindow)owned[i]).removeGlobalValueChangeListener (l);
            }
         }
      }
   }

   public ValueChangeListener[] getGlobalValueChangeListeners() {
      return myPanel.getGlobalValueChangeListeners().toArray (
         new ValueChangeListener[0]);
   }

   public Object getSynchronizeObject() {
      return myPanel.getSynchronizeObject();
   }

   public void setSynchronizeObject (Object syncObj) {
      myPanel.setSynchronizeObject (syncObj);
      Window[] owned = getOwnedWindows();
      for (int i = 0; i < owned.length; i++) {
         if (owned[i] instanceof PropertyWindow) {
            ((PropertyWindow)owned[i]).setSynchronizeObject (syncObj);
         }
      }
   }

   public void locateRight (Component comp) {
      GuiUtils.locateRight (this, comp);
   }

   public boolean isLiveUpdatingEnabled() {
      return myLiveUpdatingEnabled;
   }

   public void enableLiveUpdating (boolean enable) {
      myLiveUpdatingEnabled = enable;
   }

//   public void enableAutoRerendering (boolean enable) {
//      RerenderListener l = getRerenderListener();
//      if ((l != null) != enable) {
//         if (enable) {
//            addGlobalValueChangeListener (new RerenderListener());
//         }
//         else {
//            removeGlobalValueChangeListener (l);
//         }
//      }
//   }

//   public boolean isAutoRerenderingEnabled() {
//      return getRerenderListener() != null;
//   }

   protected void fireGlobalValueChangeListeners () {
      myPanel.fireGlobalValueChangeListeners (new ValueChangeEvent (this, null));
   }

//   private RerenderListener getRerenderListener() {
//      for (ValueChangeListener l : myPanel.getGlobalValueChangeListeners()) {
//         if (l instanceof RerenderListener) {
//            return (RerenderListener)l;
//         }
//      }
//      return null;
//   }

//   public boolean removeStalePropertyWidgets() {
//      if (myPanel.removeStalePropertyWidgets()) {
//         pack();
//         return true;
//      }
//      else {
//         return false;
//      }
//   }

   public PropertyFrame (String name) {
      this (name, null);
   }

   public PropertyFrame (String name, String options) {
      this (name, options, new PropertyPanel());
   }

   public PropertyFrame (String name, String options, PropertyPanel panel) {
      super (name);
      initialize();
      if (options != null) {
         addOptionPanel (options);
      }
      setPanel (panel);
      //enableAutoRerendering (true);
      setDefaultCloseOperation (DISPOSE_ON_CLOSE);
      // don't call pack because once we do, we shouldn't access
      // components outside the GUI thread
      //pack();
   }
}
