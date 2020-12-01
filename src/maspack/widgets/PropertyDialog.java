/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import maspack.properties.EditingProperty;
import maspack.properties.HasProperties;
import maspack.properties.HostList;
import maspack.properties.PropTreeCell;
import maspack.properties.Property;
import maspack.util.InternalErrorException;

public class PropertyDialog extends JDialog implements ActionListener,
   PropertyWindow {
   private static final long serialVersionUID = -2151015189829300640L;
   protected PropertyPanel myPanel;
   protected boolean myScrollableP = false;
   protected JScrollPane myScrollPane;
   protected OptionPanel myOptionPanel;
   protected int myReturnValue = JOptionPane.NO_OPTION;
   // private LinkedList<PropertyWindowBase> myChildren;
   // private PropertyWindowBase myParent;
   HostList myHostList;
   PropTreeCell myTree;

   private boolean myLiveUpdatingEnabled = true;

   public OptionPanel getOptionPanel() {
      return myOptionPanel;
   }
   
   /**
    * Should be called in the constructor, after the Window has been initialized
    * and getContentPane() returns a valid container.
    */
   protected void initialize (String options) {
      // myChildren = new LinkedList<PropertyWindowBase>();
      // myParent = null;
      getContentPane().setLayout (
         new BoxLayout (getContentPane(), BoxLayout.Y_AXIS));

      if (options != null) {
         JSeparator sep = new JSeparator();
         sep.setAlignmentX (Component.CENTER_ALIGNMENT);
         getContentPane().add (GuiUtils.createBoxFiller());
         getContentPane().add (sep);

         myOptionPanel = new OptionPanel (options, this);
         myOptionPanel.setAlignmentX (Component.CENTER_ALIGNMENT);
         getContentPane().add (myOptionPanel);
      }
   }

   // protected abstract Window getWindow();

   // protected abstract Container getContentPane();

   // public void pack()
   // {
   // getWindow().pack();
   // }

   // public void setVisible (boolean visible)
   // {
   // getWindow().setVisible(visible);
   // }

   // public boolean isVisible()
   // {
   // return getWindow().isVisible();
   // }

   // public void setLocation (int x, int y)
   // {
   // getWindow().setLocation (x, y);
   // }

   // public void setSize(int w, int h)
   // {
   // getWindow().setSize(w, h);
   // }

   // public int getWidth()
   // {
   // return getWindow().getWidth();
   // }

   // public int getHeight()
   // {
   // return getWindow().getHeight();
   // }

   private JScrollPane createScrollPane() {
      JScrollPane pane =  new JScrollPane (myPanel);
      pane.setAlignmentX (Component.CENTER_ALIGNMENT);
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
      myPanel.setAlignmentX (Component.CENTER_ALIGNMENT);
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

   // private void setParentIfCompositePropertyWidget (Component comp)
   // { if (comp instanceof CompositePropertyWidget)
   // { ((CompositePropertyWidget)comp).setParentWindow (this);
   // }
   // }

   public Component addWidget (Component comp) {
      myPanel.addWidget (comp);
      // setParentIfCompositePropertyWidget (comp);
      return comp;
   }

   public Component addWidget (Component comp, int idx) {
      myPanel.addWidget (comp, idx);
      return comp;
   }

   public LabeledComponentBase addWidget (Property prop) {
      LabeledComponentBase comp = myPanel.addWidget (prop);
      // setParentIfCompositePropertyWidget (comp);
      return comp;
   }

   public LabeledComponentBase addWidget (Property prop, double min, double max) {
      LabeledComponentBase comp = myPanel.addWidget (prop, min, max);
      // setParentIfCompositePropertyWidget (comp);
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
      if (actionCmd.equals ("OK") || actionCmd.equals ("Done")) {
         myReturnValue = OptionPanel.OK_OPTION;
         setVisible (false);
         dispose();
      }
      else if (actionCmd.equals ("Reset")) {
         System.out.println ("reset");
         if (myHostList != null) {
            myHostList.restoreBackupValues();
            myHostList.getCommonValues (myTree, /* live= */true);
            fireGlobalValueChangeListeners();
         }
      }
      else if (actionCmd.equals ("Cancel")) {
         myReturnValue = OptionPanel.CANCEL_OPTION;
         //System.out.println ("host list=" + myHostList);
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

   // public void addWindowListener (WindowListener l)
   // {
   // getWindow().addWindowListener (l);
   // }

   // public void removeWindowListener (WindowListener l)
   // {
   // getWindow().removeWindowListener (l);
   // }

   // public WindowListener[] getWindowListeners()
   // {
   // return getWindow().getWindowListeners();
   // }

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

   // protected PropertyDialog (Window owner)
   // {
   // super (owner);
   // }

   protected PropertyDialog (Frame owner, String title) {
      super (owner, title);
   }

   protected PropertyDialog (Dialog owner, String title) {
      super (owner, title);
   }

   protected PropertyDialog() {
      super();
   }

   // public PropertyDialog (String name)
   // {
   // this (name, new PropertyPanel(), null);
   // }

   // public PropertyDialog (String name, String options)
   // {
   // this (name, new PropertyPanel(), options);
   // }

   public PropertyDialog (String name, HasProperties host, String options) {
      this (name, new PropertyPanel (host), options);
   }

   public PropertyDialog (String name, HostList hostList, String options) {
      this (
         name, hostList.commonProperties (null, /* allowReadOnly= */false),
         hostList, options);
   }

   public PropertyDialog (String name, PropTreeCell tree, HostList hostList,
   String options) {
      this (name, createPanelFromHostList (tree, hostList), options);
      myTree = tree;
      myHostList = hostList;
   }

   public PropertyDialog (String name, PropertyPanel panel, String options) {
      super();
      setTitle (name);
      initialize (options);
      setPanel (panel);
      //enableAutoRerendering (true);
      setDefaultCloseOperation (DISPOSE_ON_CLOSE);
      pack();
   }

   public static PropertyDialog createDialog (
      Window owner, String name, HostList hostList, String options) {
      PropTreeCell tree =
         hostList.commonProperties (null, /* allowReadOnly= */false);
      return createDialog (owner, name, tree, hostList, options);
   }

   public static PropertyDialog createDialog (
      Window owner, String name, PropTreeCell tree, HostList hostList,
      String options) {
      PropertyPanel panel = createPanelFromHostList (tree, hostList);
      PropertyDialog dialog = createDialog (owner, name, panel, options);
      dialog.myHostList = hostList;
      dialog.myTree = tree;
      return dialog;
   }

   public static PropertyDialog createDialog (
      Window owner, String name, PropertyPanel panel, String options) {
      PropertyDialog dialog;
      if (owner instanceof Dialog) {
         dialog = new PropertyDialog ((Dialog)owner, name, panel, options);
      }
      else if (owner instanceof Frame) {
         dialog = new PropertyDialog ((Frame)owner, name, panel, options);
      }
      else if (owner == null) {
         dialog = new PropertyDialog (name, panel, options);
      }
      else {
         throw new InternalErrorException ("Unsupported window type: "
         + owner.getClass());
      }
      return dialog;
   }
   
   public static PropertyDialog createDialog (
      String title, Iterable<? extends HasProperties> hosts, String optionStr,
      Component parentComp, ValueChangeListener globalChangeListener) {
   
      HostList hostList = new HostList (hosts);
      PropTreeCell tree =
         hostList.commonProperties (null, /* allowReadonly= */true);
      tree.removeDescendant ("renderProps");
      if (tree.numChildren() == 0) {
         GuiUtils.showNotice (
            parentComp, "No common properties for selected components");
         return null;
      }
      else {
         PropertyDialog dialog =
            new PropertyDialog (
               "Edit properties", tree, hostList, optionStr);
         dialog.setScrollable (true);
         if (globalChangeListener != null) {
            dialog.addGlobalValueChangeListener (globalChangeListener);
         }
         dialog.locateRight (parentComp);
         dialog.setTitle (title);
         return dialog;
      }
   }

   /* duplicate constructor for compatibility with Java 1.5 */

   protected void inheritGlobalListeners (Window owner) {
      if (owner instanceof PropertyWindow) {
         PropertyWindow ownerWindow = (PropertyWindow)owner;
         ValueChangeListener[] listeners =
            ownerWindow.getGlobalValueChangeListeners();
         if (listeners != null && listeners.length > 0) {
            for (int i = 0; i < listeners.length; i++) {
               addGlobalValueChangeListener (listeners[i]);
            }
         }
         Object syncObj = ownerWindow.getSynchronizeObject();
         if (syncObj != null) {
            setSynchronizeObject (syncObj);
         }
      }
   }

   public PropertyDialog (Dialog owner, String name, PropertyPanel panel,
   String options) {

      super (owner);
      setTitle (name);
      initialize (options);
      setPanel (panel);
      inheritGlobalListeners (owner);
      // enableAutoRerendering (true);
      setDefaultCloseOperation (DISPOSE_ON_CLOSE);
      pack();
   }

   public PropertyDialog (Frame owner, String name, PropertyPanel panel,
   String options) {
      /* explicit cast for compatibility with Java 1.5 */
      super (owner);
      setTitle (name);
      initialize (options);
      setPanel (panel);
      inheritGlobalListeners (owner);
      // enableAutoRerendering (true);
      setDefaultCloseOperation (DISPOSE_ON_CLOSE);
      pack();
   }

   // public boolean isModal()
   // {
   // return myDialog.isModal();
   // }

   // public void setModal (boolean modal)
   // {
   // myDialog.setModal (modal);
   // }

}
