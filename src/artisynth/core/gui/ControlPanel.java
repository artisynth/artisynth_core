/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.properties.PropertyUtils;
import maspack.properties.EditingProperty;
import maspack.util.*;
import maspack.widgets.ButtonMasks;
import maspack.widgets.GuiUtils;
import maspack.widgets.LabeledComponentBase;
import maspack.widgets.LabeledControl;
import maspack.widgets.OptionPanel;
import maspack.widgets.PropertyFrame;
import maspack.widgets.PropertyPanel;
import maspack.widgets.PropertyWidget;
import maspack.widgets.PropertyWindow;
import maspack.widgets.StringField;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.widgets.ValueCheckListener;
import maspack.widgets.WidgetDialog;
import artisynth.core.driver.Main;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.gui.editorManager.RemoveComponentsCommand;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CompositeComponentBase;
import artisynth.core.modelbase.MutableCompositeComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;

public class ControlPanel extends ModelComponentBase
   implements PropertyWindow, ActionListener {
   PropertyFrame myFrame;

   public static PropertyList myProps =
      new PropertyList (ControlPanel.class, ModelComponentBase.class);

   private static boolean DEFAULT_SCROLLABLE = true;
   private boolean myScrollableP = DEFAULT_SCROLLABLE;

   private class InternalRerenderListener implements ValueChangeListener {
      public void valueChange (ValueChangeEvent e) {
         RootModel rootModel = RootModel.getRoot (ControlPanel.this);
         if (rootModel != null) {
            rootModel.rerender();
         }
      }
   }

   protected InternalRerenderListener myRerenderListener =
      new InternalRerenderListener();

   // A property panel that allows widgets to be added to it
   private class AddablePropertyPanel extends PropertyPanel {

      private class AddWidgetHandler extends WindowAdapter {
         
         private JComponent mySource;

         public AddWidgetHandler (JComponent source) {
            // widget from which the request initiated
            mySource = source;
         }

         public void windowClosed (WindowEvent e) {
            System.out.println ("window closed");
            PropertyWidgetDialog dialog = (PropertyWidgetDialog)e.getSource();
            if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
               LabeledComponentBase widget = dialog.createWidget();
               Property prop = PropertyWidget.getProperty (widget);
               addPropertyWidget (
                  prop, widget, getComponentIndex(mySource)+1);
               repackContainingWindow();
            }
         }
      }

      @Override
      public ArrayList<String> getMenuActionCommands() {
         ArrayList<String> actions = super.getMenuActionCommands();
         if (Main.getMain() != null && mySelectedWidgets.size() == 1) {
            actions.add (0, "add widget");
         }
         return actions;
      }

      @Override
      public void actionPerformed (ActionEvent e) {
         String command = e.getActionCommand();
         
         if (command.equals ("add widget")) {
            Window win = SwingUtilities.getWindowAncestor (this);
            if (win != null) {
               PropertyWidgetDialog dialog =
                  PropertyWidgetDialog.createDialog (
                     win, "widget creation dialog", Main.getMain());
               GuiUtils.locateVertically (
                  dialog, this, GuiUtils.BELOW);
               GuiUtils.locateHorizontally (
                  dialog, this, GuiUtils.CENTER);
               JComponent sourceComp = null;
               if (mySelectedWidgets.iterator().hasNext()) {
                  sourceComp = mySelectedWidgets.iterator().next();
               }
               dialog.addWindowListener (new AddWidgetHandler (sourceComp));
               dialog.setVisible (true);
            }
         }
         else {
            super.actionPerformed (e);
         }
      }
   }

   /**
    * MouseListener to listener for double clicks on a widget, and
    * then "select" the ModelComponent, if any, associated with that widget
    */
   private class MouseListener extends MouseAdapter {

      private ModelComponent getPropertyComponent (LabeledComponentBase widget) {
         Property prop = PropertyWidget.getProperty (widget);
         if (prop != null && prop.getHost() instanceof ModelComponent) {
            return (ModelComponent)prop.getHost();
         }
         else {
            return null;
         }
      }

      public void mouseClicked (MouseEvent e) {
         if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
            PropertyPanel panel = myFrame.getPropertyPanel();
            JComponent comp = panel.findWidget (e);
            if (comp instanceof LabeledComponentBase) {
               ModelComponent c =
                  getPropertyComponent ((LabeledComponentBase)comp);
               System.out.println ("c=" + c);
               if (c != null) {
                  SelectionManager selManager =
                     Main.getMain().getSelectionManager();
                  selManager.clearSelections();
                  selManager.addSelected (c);
               }
            }
         }
      }      
   }

   static {
      myProps.add ("size * *", "size of the control panel", null, "1E");
      myProps.add ("location * *", "location of the control panel", null, "1E");
      myProps.add (
         "scrollable isScrollable", "sets panel to be scrollable",
         DEFAULT_SCROLLABLE);
   }

   public void setName (String name) {
      super.setName (name);
      if (myFrame != null) {

         myFrame.setTitle (name == null ? "panel " + getNumber() : name);
         
         JTabbedPane tabbedPane = Main.getRootModel().getControlPanelTabs();
         if (tabbedPane != null) {
            int tabIdx = tabbedPane.indexOfComponent (myFrame.getContentPane ());
            if (tabIdx > -1) {
               tabbedPane.setTitleAt (tabIdx, myFrame.getTitle ());
            }
         }
      }
   }

   @Override 
   public void setDefaultValues () {
      setScrollable (DEFAULT_SCROLLABLE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ControlPanel() {
      this (null);
   }

   public ControlPanel (String name) {
      this (name, null);
   }

   public ControlPanel (String name, String options) {
      setName (name);
      myFrame =
         new PropertyFrame (
            name==null ? "" : name, options, new AddablePropertyPanel());
      myFrame.setScrollable (myScrollableP);
      OptionPanel optpanel = myFrame.getOptionPanel();
      if (optpanel != null) {
         optpanel.addMouseListener (new MouseHandler());
      }
      myFrame.addWindowListener (new WindowAdapter() {
         public void windowClosed (WindowEvent e) {
            removeFromParent();
         }

         public void windowClosing (WindowEvent e) {}
      });
      myFrame.getPropertyPanel().addMouseListener (new MouseListener());
   }

   private void removeFromParent() {
      if (myParent instanceof MutableCompositeComponent) {
         RemoveComponentsCommand rmCmd =
            new RemoveComponentsCommand ("delete controlPanel", this);
         Main.getUndoManager().saveStateAndExecute (rmCmd);
      }
   }

   private class MouseHandler extends MouseAdapter {
      public void mousePressed (MouseEvent e) {
         if (e.getModifiersEx() == ButtonMasks.getContextMenuMask()) {
            JPopupMenu popup = createWindowPopup();
            popup.show (e.getComponent(), e.getX(), e.getY());
         }
      }
   }

   protected JMenuItem createPopupItem (String cmd, ActionListener l) {
      JMenuItem item = new JMenuItem (cmd);
      item.setActionCommand (cmd);
      item.addActionListener (l);
      return item;
   }

   protected JPopupMenu createWindowPopup() {
      JPopupMenu popup = new JPopupMenu();

      popup.add (createPopupItem ("add widget", this));
      popup.add (createPopupItem ("set name", this));
      popup.add (createPopupItem ("save as ...", this));
      
      JTabbedPane tabbedPane = Main.getRootModel().getControlPanelTabs();
      if (tabbedPane.indexOfComponent (myFrame.getContentPane ()) < 0) {
         popup.add (createPopupItem ("merge panel", this));
      }
      else {
         popup.add (createPopupItem ("separate panel", this));
      }

      return popup;
   }

   public int numWidgets() {
      return myFrame.getPropertyPanel().numWidgets();
   }

   public void setScrollable (boolean scrollable) {
      if (scrollable != myScrollableP) {
         myScrollableP = scrollable;
         if (myFrame != null) {
            myFrame.setScrollable (scrollable);
         }
      }
   }

   public boolean isScrollable() {
      return myFrame.isScrollable();
   }

   public Component addWidget (Component comp) {
      return myFrame.addWidget (comp);
   }

   public LabeledComponentBase addWidget (Property prop) {
      return myFrame.addWidget (prop);
   }

   public LabeledComponentBase addWidget (Property prop, double min, double max) {
      return myFrame.addWidget (prop, min, max);
   }

   public LabeledComponentBase addWidget (HasProperties host, String name) {
      return myFrame.addWidget (host, name);
   }

   public LabeledComponentBase addWidget (
      HasProperties host, String name, double min, double max) {
      return myFrame.addWidget (host, name, min, max);
   }

   public LabeledComponentBase addWidget (
      String labelText, HasProperties host, String name) {
      return myFrame.addWidget (labelText, host, name);
   }

   public LabeledComponentBase addWidget (
      String labelText, HasProperties host, String name, double min, double max) {
      return myFrame.addWidget (labelText, host, name, min, max);
   }

   public Component addLabel (String text) {
      JLabel label = new JLabel (text);
      label.setForeground (new Color (0.4f, 0.4f, 0.8f));
      GuiUtils.setItalicFont (label);
      addWidget (label);
      return label;
   }

   public PropertyPanel getPropertyPanel() {
      return myFrame.getPropertyPanel();
   }

   public PropertyFrame getFrame() {
      return myFrame;
   }

   public void dispose() {
      myFrame.dispose();
   }

   public void updateWidgetValues() {
      myFrame.updateWidgetValues();
   }

   public void addGlobalValueChangeListener (ValueChangeListener l) {
      myFrame.addGlobalValueChangeListener (l);
   }

   public void removeGlobalValueChangeListener (ValueChangeListener l) {
      myFrame.removeGlobalValueChangeListener (l);
   }

   public ValueChangeListener[] getGlobalValueChangeListeners() {
      return myFrame.getGlobalValueChangeListeners();
   }

   public void setSynchronizeObject (Object syncObj) {
      myFrame.setSynchronizeObject (syncObj);
   }

   public Object getSynchronizeObject() {
      return myFrame.getSynchronizeObject();
   }

   public void locateRight (Component comp) {
      GuiUtils.locateRight (myFrame, comp);
   }

   public boolean isLiveUpdatingEnabled() {
      return myFrame.isLiveUpdatingEnabled();
   }

   public void enableLiveUpdating (boolean enable) {
      myFrame.enableLiveUpdating (enable);
   }

//   public void enableAutoRerendering (boolean enable) {
//      myFrame.enableAutoRerendering (enable);
//   }

//   public boolean isAutoRerenderingEnabled() {
//      return myFrame.isAutoRerenderingEnabled();
//   }

   // public void show() {
   //    myFrame.setVisible (true);
   // }

   public void writeWidget (
      PrintWriter pw, Component comp, NumberFormat fmt,
      CompositeComponent ancestor) throws IOException {
      String aliasOrName = ClassAliases.getAliasOrName (comp.getClass());
      if (!(comp instanceof LabeledComponentBase)) {
         pw.println (aliasOrName + " [ ]");
      }
      else {
         LabeledComponentBase widget = (LabeledComponentBase)comp;
         pw.println (aliasOrName);
         pw.print ("[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         Property prop = PropertyWidget.getProperty (widget);
         if (prop != null) {
            String propPath =
               ComponentUtils.getWritePropertyPathName (prop, ancestor);
            if (propPath != null) {
               pw.println ("property=" + propPath);
            }
         }
         widget.getAllPropertyInfo().writeNonDefaultProps (widget, pw, fmt);
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      CompositeComponent ancestor =
         ComponentUtils.castRefToAncestor (ref);
      pw.print ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt);
      OptionPanel optpanel = myFrame.getOptionPanel();
      if (optpanel != null) {
         pw.println ("options=\"" + optpanel.getButtonString()
         + "\"");
      }
      else {
         pw.println ("options=null");
      }
      for (Component comp : getPropertyPanel().getWidgets()) {
         writeWidget (pw, comp, fmt, ancestor);
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public void scanWidget (
      String classNameOrAlias, ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      Component comp;
      try {
         comp =
            (Component)ClassAliases.newInstance (
               classNameOrAlias, Component.class);
      }
      catch (Exception e) {
         throw new IOException (
            "Could not instantiate widget " + classNameOrAlias +
            ", line " + rtok.lineno() + ":\n"  + e.getMessage());
      }
      if (!(comp instanceof LabeledComponentBase)) {
         rtok.scanToken ('[');
         rtok.scanToken (']');
         tokens.offer (new StringToken ("widget", rtok.lineno()));
         tokens.offer (new ObjectToken(comp));
         return;
      }
      LabeledComponentBase widget = (LabeledComponentBase)comp;
      rtok.scanToken ('[');
      String propPath = null;
      while (rtok.nextToken() != ']') {
         if (scanAttributeName (rtok, "property")) {
            propPath = rtok.scanQuotedString ('"');
         }
         else if (rtok.tokenIsWord()) {
            String fieldName = rtok.sval;               
            if (!ScanWriteUtils.scanProperty (rtok, widget)) {
               System.out.println (
                  "Warning: internal widget property '" + fieldName +
                  "' not found for " + widget + "; ignoring");
               widget = null;
               break;
            }
         }
         else {
            throw new IOException ("Unexpected token: " + rtok);
         }
      }
      if (widget != null) {
         tokens.offer (new StringToken ("widget", rtok.lineno()));
         if (propPath != null) {
            tokens.offer (new StringToken (propPath, rtok.lineno()));
         }
         tokens.offer (new ObjectToken (widget, rtok.lineno()));
      }
   }

   public void postscanWidget (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      String propPath = null;
      if (tokens.peek() instanceof StringToken) {
         propPath = (String)tokens.poll().value();
      }
      if (!(tokens.peek() instanceof ObjectToken)) {
         throw new IOException (
            "Expecting ObjectToken containing widget");
      }
      Component comp = (Component)tokens.poll().value();
      if (comp instanceof LabeledComponentBase) {
         Property property = null;
         LabeledComponentBase widget = (LabeledComponentBase)comp;
         // mask valueChangeListeners because otherwise setting properties
         // such as range could cause the property value itself to be reset
         if (widget instanceof LabeledControl) {
            ((LabeledControl)widget).maskValueChangeListeners (true);
         }
         if (propPath != null) {
            property = ancestor.getProperty (propPath);
            if (property == null) {
               System.out.println (
                  "Ignoring control panel widget for " + propPath);
               widget = null;
            }
            else {
               boolean widgetSet = false;
               try {
                  // initialize widget, but don't set properties because
                  // these will have already been set in the scan method
                  widgetSet = PropertyWidget.initializeWidget (
                     widget, property);
               }
               catch (Exception e) {
                  e.printStackTrace(); 
                  widgetSet = false;
               }
               if (widgetSet == false) {
                  System.out.println (
                     "Warning: widget " + widget +
                     " inappropriate for property " + propPath + "; ignoring");
                  widget = null;
               }
            }
         }
         if (widget instanceof LabeledControl) {
            ((LabeledControl)widget).maskValueChangeListeners (false);
         }
         // finish widget is called at the end because it will set the
         // widget's value, and we shouldn't do this until all the widget
         // properties (such as the range) are initialized
         if (widget != null && property != null) {
            PropertyWidget.finishWidget (widget, property);
            getPropertyPanel().processPropertyWidget (property, widget);
         }
         if (widget != null) {
            addWidget (widget);
         }
      }
      else {
         addWidget (comp);
      }
   }

   public boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      
      if (super.scanItem (rtok, tokens)) {
         return true;
      }
      rtok.nextToken();
      if (scanAttributeName (rtok, "options")) {
         String options = rtok.scanQuotedString ('"');
         if (options != null) {
            OptionPanel optpanel = myFrame.addOptionPanel (options);
            optpanel.addMouseListener (new MouseHandler());
            myFrame.pack();
         }
         return true;
      }
      else if (rtok.tokenIsWord()) {
         scanWidget (rtok.sval, rtok, tokens);
         return true;
      }
      rtok.pushBack();
      return false;
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "widget")) {
          postscanWidget (tokens, ancestor);
          return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
   public void pack() {
      myFrame.pack();
   }

   public void setFocusableWindowState(boolean enable) {
      myFrame.setFocusableWindowState(enable);
   }

   public void setVisible (boolean visible) {
      myFrame.setVisible (visible);
   }

   public boolean isVisible () {
      return myFrame.isVisible();
   }

   public void setSize (Dimension dim) {
      myFrame.setSize (dim);
   }

   public void setSize (int w, int h) {
      myFrame.setSize (w, h);
   }

   public Dimension getSize() {
      return myFrame.getSize();
   }

   public void setLocation (Point dim) {
      myFrame.setLocation (dim);
   }

   public void setLocation (int x, int y) {
      myFrame.setLocation (x, y);
   }

   public int getHeight() {
      return myFrame.getHeight();
   }

   public int getWidth() {
      return myFrame.getWidth();
   }

   public Point getLocation() {
      return myFrame.getLocation();
   }

   private boolean isStale (Property prop) {
      ModelComponent comp = ComponentUtils.getPropertyComponent (prop);
      if (comp == null) {
         System.out.println ("no comp");
         return true;
      }
      if (!PropertyUtils.isConnectedToHierarchy (prop)) {
         System.out.println ("not connected");
         return true;
      }
      RootModel myroot = RootModel.getRoot (this);
      if (RootModel.getRoot(comp) != myroot) {
         System.out.println ("not in root");
         return true;
      }
      return false;
   }

   public boolean removeStalePropertyWidgets() {
      PropertyPanel panel = myFrame.getPropertyPanel();

      LinkedList<LabeledComponentBase> removeList =
         new LinkedList<LabeledComponentBase>();
      for (int i=0; i<panel.numWidgets(); i++) {
         if (panel.getWidget(i) instanceof LabeledComponentBase) {
            LabeledComponentBase widget =
               (LabeledComponentBase)panel.getWidget(i);
            Property prop = panel.getWidgetProperty (widget);
            if (prop != null && !(prop instanceof EditingProperty)) {
               if (isStale (prop)) {
                  removeList.add (widget);
               }
            }
         }
      }
      if (removeList.size() > 0) {
         for (LabeledComponentBase widget : removeList) {
            panel.removeWidget (widget);
            //myWidgetPropMap.remove (widget);
         }
         myFrame.pack();
         return true;
      }
      else {
         return false;
      }
   }

   public void actionPerformed (ActionEvent e) {
      String actionCmd = e.getActionCommand();
      if (actionCmd.equals ("add widget")) {
         getPropertyPanel().actionPerformed (e);
      }
      else if (actionCmd.equals ("set name")) {
//          StringDialog dialog =
//             StringDialog.createDialog (myFrame, "set name", "name:", getName());
//          dialog.getStringField().addValueCheckListener (
//             new ValueCheckListener() {
//                public Object validateValue (
//                   ValueChangeEvent e, StringHolder errMsg) {
//                   String name = (String)e.getValue();
//                   return ModelComponentBase.validateName (name, errMsg);
//                }
//             });
         WidgetDialog dialog =
            WidgetDialog.createDialog (myFrame, "set name", "Set");
         StringField widget = new StringField ("name:", getName(), 20);
         widget.addValueCheckListener (
            new ValueCheckListener() {
               public Object validateValue (
                  ValueChangeEvent e, StringHolder errMsg) {
                  String name = (String)e.getValue();
                  if (name != null && name.length() == 0) {
                     return null;
                  }
                  errMsg.value = ModelComponentBase.checkName (name, null);
                  if (errMsg.value != null) {
                     return Property.IllegalValue;
                  }
                  else {
                     return name;
                  }
               }
            });
         dialog.addWidget (widget);
         GuiUtils.locateVertically (dialog, myFrame, GuiUtils.BELOW);
         GuiUtils.locateHorizontally (dialog, myFrame, GuiUtils.CENTER);
         dialog.setVisible (true);
         if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
            String name = (String)widget.getValue();
            setName (name);
         }
      }
      else if (actionCmd.equals ("save as ...")) {
         Main main = Main.getMain();
         RootModel root = Main.getRootModel();
         JFileChooser chooser = new JFileChooser();
         chooser.setCurrentDirectory (main.getModelDirectory());
         int retVal = chooser.showSaveDialog (myFrame);
         if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
               ComponentUtils.saveComponent (file, this, new NumberFormat (
                  "%.6g"), root);
            }
            catch (Exception ex) {
               JOptionPane.showMessageDialog (myFrame, "Error saving file: "
               + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            main.setModelDirectory (chooser.getCurrentDirectory());
         }
      }
      else if (actionCmd.equals ("merge panel")) {
         Main.getRootModel ().mergeControlPanel (true, this);
      }
      else if (actionCmd.equals ("separate panel")) {
         Main.getRootModel ().mergeControlPanel (false, this);         
      }
      else {
         throw new InternalErrorException ("Unknown action: " + actionCmd);
      }
   }

   public void connectToHierarchy () {
      RootModel rootModel = RootModel.getRoot (this);
      if (rootModel != null) {
         setSynchronizeObject (rootModel);
         setFocusableWindowState (rootModel.isFocusable());
      }
      addGlobalValueChangeListener (myRerenderListener);
      pack();
      setVisible (true);
      super.connectToHierarchy ();
   }

   public void disconnectFromHierarchy () {
      super.disconnectFromHierarchy();
      dispose();
   }


}
