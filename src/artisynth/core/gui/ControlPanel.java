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
import maspack.properties.HostList;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.properties.PropertyUtils;
import maspack.properties.EditingProperty;
import maspack.properties.GenericPropertyHandle;
import maspack.util.*;
import maspack.widgets.ButtonMasks;
import maspack.widgets.GuiUtils;
import maspack.widgets.LabeledComponentBase;
import maspack.widgets.LabeledControl;
import maspack.widgets.NumericFieldSlider;
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
import artisynth.core.workspace.RootModel;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import artisynth.core.util.ObjectToken;

/**
 * Provides a graphic panel containing widgets for editing properties and
 * performing other forms of model interaction.
 */
public class ControlPanel extends ModelComponentBase
   implements PropertyWindow, ActionListener {

   PropertyFrame myFrame;
   PropertyPanel myPanel;
   String myOptionsString; // store separately in case there is no frame

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
               int insertIndex;
               if (mySource != null) {
                  insertIndex = getComponentIndex(mySource)+1;
               }
               else {
                  insertIndex = numWidgets();
               }
               addPropertyWidget (prop, widget, insertIndex);
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
            PropertyPanel panel = myPanel;
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
         
         RootModel root = Main.getMain().getRootModel();
         if (root != null) {
            JTabbedPane tabbedPane = root.getControlPanelTabs();
            if (tabbedPane != null) {
               int tabIdx =
                  tabbedPane.indexOfComponent (myFrame.getContentPane ());
               if (tabIdx > -1) {
                  tabbedPane.setTitleAt (tabIdx, myFrame.getTitle ());
               }
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
      myPanel = new AddablePropertyPanel();
      myOptionsString = options;
      Main main = Main.getMain();
      if (main != null && main.getMainFrame() != null) {
         myFrame =
            new PropertyFrame (
               name==null ? "" : name, options, myPanel);
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
      }
      myPanel.addMouseListener (new MouseListener());
   }

   private void removeFromParent() {
      if (myParent instanceof MutableCompositeComponent) {
         RemoveComponentsCommand rmCmd =
            new RemoveComponentsCommand ("delete controlPanel", this);
         Main.getMain().getUndoManager().saveStateAndExecute (rmCmd);
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
      
      JTabbedPane tabbedPane =
         Main.getMain().getRootModel().getControlPanelTabs();
      if (tabbedPane == null || 
          tabbedPane.indexOfComponent (myFrame.getContentPane ()) < 0) {
            popup.add (createPopupItem ("merge panel", this));
      }
      else {
         popup.add (createPopupItem ("separate panel", this));
      }

      return popup;
   }

   public int numWidgets() {
      return myPanel.numWidgets();
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
      return myScrollableP;
   }

   public Component addWidget (Component comp) {
      myPanel.addWidget (comp);
      return comp;
   }

   public LabeledComponentBase addWidget (Property prop) {
      return myPanel.addWidget (prop);
   }

   public LabeledComponentBase addWidget (Property prop, double min, double max) {
      return myPanel.addWidget (prop, min, max);
   }

   public LabeledComponentBase addWidget (HasProperties host, String name) {
      return myPanel.addWidget (host, name);
   }

   public LabeledComponentBase addWidget (
      HasProperties host, String name, double min, double max) {
      return myPanel.addWidget (host, name, min, max);
   }

   public LabeledComponentBase addWidget (
      String labelText, HasProperties host, String name) {
      return myPanel.addWidget (labelText, host, name);
   }

   public LabeledComponentBase addWidget (
      String labelText, HasProperties host, String name, double min, double max) {
      return myPanel.addWidget (labelText, host, name, min, max);
   }

   /**
    * Create and add a labeled widget that controls a specified property in one
    * or more hosts. The property's name is used as the widget's label.
    *
    * @param propPath path of the property with respect to each host
    * @param hosts one or more hosts of the specified property
    * @return the created widget, or {@code null} if the specified
    * property was not found in all hosts
    * @throws IllegalArgumentException if no hosts are specified
    */
   public LabeledComponentBase addWidget (
      String propPath, HasProperties... hosts) {
      return myPanel.addWidget (propPath, hosts);
   }

   /**
    * Create and add a labeled widget that controls a specified property in one
    * or more hosts. The property's name is used as the widget's label.
    *
    * @param propPath path of the property with respect to each host
    * @param hosts one or more hosts of the specified property
    * @return the created widget, or {@code null} if the specified
    * property was not found in all hosts
    * @throws IllegalArgumentException if no hosts are specified
    */
   public LabeledComponentBase addWidget (
      String propPath, double min, double max, HasProperties... hosts) {
      return myPanel.addWidget (propPath, min, max, hosts);
   }

   /**
    * Create and add a labeled widget that controls a specified property in one
    * or more hosts.
    *
    * @param labelText label text for the widget 
    * @param propPath path of the property with respect to each host
    * @param hosts one or more hosts of the specified property
    * @return the created widget, or {@code null} if the specified
    * property was not found in all hosts
    * @throws IllegalArgumentException if no hosts are specified
    */
   public LabeledComponentBase addWidget (
      String labelText, String propPath, HasProperties... hosts) {
      return myPanel.addWidget (labelText, propPath, hosts);
   }

   /**
    * Create and add a slider widget that controls a specified scalar property
    * in one or more hosts.
    *
    * @param labelText label text for the widget 
    * @param propPath path of the property with respect to each host
    * @param min initial minimum value for the slider
    * @param max initial maximum value for the slider
    * @param hosts one or more hosts of the specified property
    * @return the created widget, or {@code null} if the specified property is
    * not a scalar property found in all hosts
    * @throws IllegalArgumentException if no hosts are specified
    */
   public LabeledComponentBase addWidget (
      String labelText, String propPath,
      double min, double max, HasProperties... hosts) {
      return myPanel.addWidget (labelText, propPath, min, max, hosts);
   }

   public void addWidgets (HasProperties host) {
      myPanel.addWidgets (PropertyUtils.createProperties (host));
   }

   public JLabel addLabel (String text) {
      return myPanel.addLabel (text);
   }

   public PropertyPanel getPropertyPanel() {
      return myPanel;
   }

   public PropertyFrame getFrame() {
      return myFrame;
   }

   public void dispose() {
      if (myFrame != null) {
         myFrame.dispose();
      }
   }

   public void updateWidgetValues() {
      if (myFrame != null) {
         myFrame.updateWidgetValues();
      }
   }

   public void addGlobalValueChangeListener (ValueChangeListener l) {
      if (myFrame != null) {
         myFrame.addGlobalValueChangeListener (l);
      }
   }

   public void removeGlobalValueChangeListener (ValueChangeListener l) {
      if (myFrame != null) {
         myFrame.removeGlobalValueChangeListener (l);
      }
   }

   public ValueChangeListener[] getGlobalValueChangeListeners() {
      if (myFrame != null) {
         return myFrame.getGlobalValueChangeListeners();
      }
      else {
         return new ValueChangeListener[0];
      }
   }

   public void setSynchronizeObject (Object syncObj) {
      if (myFrame != null) {
         myFrame.setSynchronizeObject (syncObj);
      }
      else {
         myPanel.setSynchronizeObject (syncObj);
      }
   }

   public Object getSynchronizeObject() {
      return myPanel.getSynchronizeObject();
   }

   public void locateRight (Component comp) {
      if (myFrame != null) {
         GuiUtils.locateRight (myFrame, comp);
      }
   }

   public boolean isLiveUpdatingEnabled() {
      return myFrame != null ? myFrame.isLiveUpdatingEnabled() : false;
   }

   public void enableLiveUpdating (boolean enable) {
      if (myFrame != null) {
         myFrame.enableLiveUpdating (enable);
      }
   }

   // Serialization methods

   public void writeWidget (
      PrintWriter pw, Component comp, NumberFormat fmt,
      CompositeComponent ancestor) throws IOException {
      String aliasOrName = ClassAliases.getAliasOrName (comp.getClass());
      if (!(comp instanceof LabeledComponentBase)) {
         pw.println (aliasOrName + " [ ]");
      }
      else if (comp instanceof CoordinateWidget) {
         pw.println (aliasOrName);
         ((CoordinateWidget)comp).write (pw, fmt, ancestor);
      }
      else {
         LabeledComponentBase widget = (LabeledComponentBase)comp;
         pw.println (aliasOrName);
         pw.print ("[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         Property prop = PropertyWidget.getProperty (widget);
         if (prop instanceof EditingProperty) {
            // editing property, multiple hosts: write out the path of each
            // hosts with respect to the ancestor
            HostList hostList = ((EditingProperty)prop).getHostList();
            pw.println ("properties=[");
            IndentingPrintWriter.addIndentation (pw, 2);
            for (HasProperties host : hostList) {
               pw.println (
                  ComponentUtils.getWritePropertyPathName (
                     prop.getName(), host, ancestor));
            }
            IndentingPrintWriter.addIndentation (pw, -2);
            pw.println ("]");
         }
         else if (prop != null) {
            // standard property, single host: write out the path of the host
            // with respect to the ancestor
            String propPath =
               ComponentUtils.getWritePropertyPathName (prop, ancestor);
            if (propPath != null) {
               pw.println ("property=" + propPath);
            }
         }
         widget.getAllPropertyInfo().writeNonDefaultProps (
            widget, pw, fmt, ancestor);
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }
   
   private boolean widgetIsWritable (Component widget) {
      if (!ClassAliases.isClassValid (widget.getClass())) {
         return false;
      }
      Property prop = getWidgetProperty (widget);
      ModelComponent comp = null;
      if (prop instanceof GenericPropertyHandle) {
         comp = ComponentUtils.getPropertyComponent (prop);
      }
      if (comp == null || comp.isWritable()) {
         return true;
      }
      if (comp instanceof RootModel && 
          RootModel.isBaseProperty(prop.getName())) {
         return true;         
      }
      return false;
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor) 
      throws IOException {
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ancestor);
      String optionsStr = null;
      if (myFrame != null) {
         OptionPanel optpanel = myFrame.getOptionPanel();
         if (optpanel != null) {
            optionsStr = optpanel.getButtonString();
         }
      }
      else {
         optionsStr = myOptionsString;
      }
      if (optionsStr != null) {
         pw.println ("options=\"" + optionsStr + "\"");
      }
      else {
         pw.println ("options=null");
      }
      for (Component widget : myPanel.getWidgets()) {
         if (widgetIsWritable (widget)) {
            writeWidget (pw, widget, fmt, ancestor);
         }
      }
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      CompositeComponent ancestor = ComponentUtils.castRefToAncestor (ref);
      IndentingPrintWriter.printOpening (pw, "[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      writeItems (pw, fmt, ancestor);
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
      }
      else if (comp instanceof CoordinateWidget) {
         tokens.offer (new StringToken ("widget", rtok.lineno()));
         tokens.offer (new ObjectToken(comp));
         ((CoordinateWidget)comp).scan (rtok, tokens);
      }
      else {
         LabeledComponentBase widget = (LabeledComponentBase)comp;
         widget.setScanning (true);
         rtok.scanToken ('[');
         String propPath = null; // one property path specified
         String[] propPaths = null; // multiple property paths specified
         while (rtok.nextToken() != ']') {
            if (scanAttributeName (rtok, "property")) {
               // widget controls property in one host
               propPath = rtok.scanQuotedString ('"');
            }
            else if (scanAttributeName (rtok, "properties")) {
               // widget controls same property in multiple hosts
               rtok.scanToken ('[');
               ArrayList<String> paths = new ArrayList<>();
               while (rtok.nextToken() != ']') {
                  if (!rtok.tokenIsQuotedString ('"')) {
                     throw new IOException (
                        "Expected property path, got " + rtok);
                  }
                  paths.add (rtok.sval);
               }
               propPaths = paths.toArray(new String[0]);
            }
            else if (rtok.tokenIsWord()) {
               String fieldName = rtok.sval;               
               if (!ScanWriteUtils.scanProperty (rtok, widget, tokens)) {
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
            else if (propPaths != null) {
               tokens.offer (new ObjectToken (propPaths, rtok.lineno()));
            }
            tokens.offer (new ObjectToken (widget, rtok.lineno()));
         }
      }
   }

   public void postscanWidget (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      String propPath = null; // one property path specified
      String[] propPaths = null; // multiple property paths specified
      // look for a StringToken specifying a single property path
      if (tokens.peek() instanceof StringToken) {
         propPath = (String)tokens.poll().value();
      }
      Component comp = null; 
      if (tokens.peek() instanceof ObjectToken) {
         Object obj = tokens.poll().value();
         // look for an ObjectToken specifying multiple property paths      
         if (obj instanceof String[]) {
            propPaths = (String[])obj;
            if (tokens.peek() instanceof ObjectToken) {
               obj = tokens.poll().value();
            }
         }
         if (obj instanceof Component) {
            comp = (Component)obj;
         }
      }
      if (comp == null) {
         throw new IOException (
            "Expecting ObjectToken containing widget");
      }
      if (comp instanceof CoordinateWidget) {
         ((CoordinateWidget)comp).postscan (tokens, ancestor);
         addWidget (comp);
      }
      else if (comp instanceof LabeledComponentBase) {
         Property property = null;
         LabeledComponentBase widget = (LabeledComponentBase)comp;
         // mask valueChangeListeners because otherwise setting properties
         // such as range could cause the property value itself to be reset
         if (widget instanceof LabeledControl) {
            ((LabeledControl)widget).maskValueChangeListeners (true);
         }
         String propErrDesc = null; // describes prop path(s) in error messages
         if (propPath != null) {
            // for a single property path create a standard property
            propErrDesc = "property " + propPath; // for error messages
            property = ancestor.getProperty (propPath);
         }
         else if (propPaths != null) {
            // for multiple property paths create an EditingProperty
            propErrDesc = "properties " + propPaths[0] + "...";
            property = EditingProperty.createProperty (
               propPaths, ancestor, /*isLive=*/true);
         }
         if (property == null) {
            System.out.println (
               "Ignoring control panel widget for " + propErrDesc);
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
                  " inappropriate for " + propErrDesc + "; ignoring");
               widget = null;
            }
         }
         if (widget instanceof LabeledControl) {
            ((LabeledControl)widget).maskValueChangeListeners (false);
         }
         // finish widget is called at the end because it will set the
         // widget's value, and we shouldn't do this until all the widget
         // properties (such as the range) are initialized
         if (widget != null && property != null) {
            if (widget instanceof NumericFieldSlider) {
               // for sliders, disable auto ranging so that we are sure to
               // restore the correct range
               NumericFieldSlider slider = (NumericFieldSlider)widget;
               boolean savedAutoRanging = slider.getAutoRangingEnabled();
               slider.setAutoRangingEnabled (false);
               PropertyWidget.finishWidget (widget, property);
               slider.setAutoRangingEnabled (savedAutoRanging);
            }
            else {
               PropertyWidget.finishWidget (widget, property);
            }
            myPanel.processPropertyWidget (property, widget);
         }
         if (widget != null) {
            addWidget (widget);
            widget.setScanning (false);
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
            if (myFrame != null) {
               OptionPanel optpanel = myFrame.addOptionPanel (options);
               optpanel.addMouseListener (new MouseHandler());
               myFrame.pack();
            }
            else {
               myOptionsString = options;
            }
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
      if (myFrame != null) {
         myFrame.pack();
      }
   }

   public void setFocusableWindowState(boolean enable) {
      if (myFrame != null) {
         myFrame.setFocusableWindowState(enable);
      }
   }

   public void setVisible (boolean visible) {
      if (myFrame != null) {
         myFrame.setVisible (visible);
      }
   }

   public boolean isVisible () {
      return myFrame != null ? myFrame.isVisible() : false;
   }

   public void setSize (Dimension dim) {
      if (myFrame != null) {
         myFrame.setSize (dim);
      }
   }

   public void setSize (int w, int h) {
      if (myFrame != null) {
         myFrame.setSize (w, h);
      }
   }

   public Dimension getSize() {
      return myFrame != null ? myFrame.getSize() : new Dimension(0,0);
   }

   public void setLocation (Point dim) {
      if (myFrame != null) {
         myFrame.setLocation (dim);
      }
   }

   public void setLocation (int x, int y) {
      if (myFrame != null) {
         myFrame.setLocation (x, y);
      }
   }

   public int getHeight() {
      return myFrame != null ? myFrame.getHeight() : 0;
   }

   public int getWidth() {
      return myFrame != null ? myFrame.getWidth() : 0;
   }

   public Point getLocation() {
      return myFrame != null ? myFrame.getLocation() : new Point(0,0);
   }

   private boolean isStale (Property prop) {
      ModelComponent comp = ComponentUtils.getPropertyComponent (prop);
      if (comp == null) {
         return false;
      }
      if (!PropertyUtils.isConnectedToHierarchy (prop)) {
         return true;
      }
      return isStale (comp);
   }

   private boolean isStale (ModelComponent comp) {
      RootModel myroot = RootModel.getRoot (this);
      // John Lloyd Aug 12 2015
      // myroot might be null if this method is called while this panel is
      // being scanned - which can happen because components are added to
      // ComponentLists before their parent is set. So if myroot is null,
      // we don't bother with the stale check
      if (myroot != null && RootModel.getRoot(comp) != myroot) {
         return true;
      }
      return false;
   }      

   public boolean removeStalePropertyWidgets() {

      LinkedList<LabeledComponentBase> removeList =
         new LinkedList<LabeledComponentBase>();
      for (int i=0; i<myPanel.numWidgets(); i++) {
         if (myPanel.getWidget(i) instanceof LabeledComponentBase) {
            LabeledComponentBase widget =
               (LabeledComponentBase)myPanel.getWidget(i);
            if (widget instanceof CoordinateWidget) {
               CoordinateWidget cwidget = (CoordinateWidget)widget;
               ModelComponent joint = cwidget.getHandle().getJoint();
               if (joint == null || isStale(joint)) {
                  removeList.add (widget);                  
               }
            }
            else {
               Property prop = myPanel.getWidgetProperty (widget);
               if (prop != null && !(prop instanceof EditingProperty)) {
                  if (isStale (prop)) {
                     removeList.add (widget);
                  }
               }
            }
         }
      }
      if (removeList.size() > 0) {
         for (LabeledComponentBase widget : removeList) {
            myPanel.removeWidget (widget);
            //myWidgetPropMap.remove (widget);
         }
         if (myFrame != null) {
            notifyWidgetsChanged();
            myFrame.pack();
         }
         return true;
      }
      else {
         return false;
      }
   }

   public void actionPerformed (ActionEvent e) {
      String actionCmd = e.getActionCommand();
      if (actionCmd.equals ("add widget")) {
         myPanel.actionPerformed (e);
      }
      else if (actionCmd.equals ("set name")) {
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
         RootModel root = main.getRootModel();
         JFileChooser chooser = new JFileChooser();
         chooser.setCurrentDirectory (main.getModelDirectory());
         int retVal = chooser.showSaveDialog (myFrame);
         if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
               main.saveComponent (file, "%g", this, false, root);
            }
            catch (Exception ex) {
               GuiUtils.showError (
                  myFrame, "Error saving file: " + ex.getMessage());
            }
            main.setModelDirectory (chooser.getCurrentDirectory());
         }
      }
      else if (actionCmd.equals ("merge panel")) {
         Main main = Main.getMain();
         main.getRootModel ().mergeControlPanel (true, this);
      }
      else if (actionCmd.equals ("separate panel")) {
         Main main = Main.getMain();
         main.getRootModel ().mergeControlPanel (false, this);         
      }
      else {
         throw new InternalErrorException ("Unknown action: " + actionCmd);
      }
   }


   @Override
   public void getSoftReferences (List<ModelComponent> refs) {
      HashSet<ModelComponent> myrefs = new HashSet<ModelComponent>();
      for (int i=0; i<myPanel.numWidgets(); i++) {
         if (myPanel.getWidget(i) instanceof LabeledComponentBase) {
            LabeledComponentBase widget =
               (LabeledComponentBase)myPanel.getWidget(i);
            if (widget instanceof CoordinateWidget) {
               CoordinateWidget cwidget = (CoordinateWidget)widget;
               ModelComponent joint = cwidget.getHandle().getJoint();
               if (joint != null) {
                  myrefs.add (joint);
               }
            }
            else {
               Property prop = myPanel.getWidgetProperty (widget);
               if (prop instanceof GenericPropertyHandle) {
                  // standard property associated with a single component
                  ModelComponent comp =
                     ComponentUtils.getPropertyComponent (prop);
                  if (comp != null &&
                      !ComponentUtils.isAncestorOf (comp, this)) {
                     myrefs.add (comp);
                  }
               }
               else if (prop instanceof EditingProperty) {
                  // EditingProperty associated with multiple components
                  HostList hostList = ((EditingProperty)prop).getHostList();
                  for (HasProperties host : hostList) {
                     ModelComponent comp =
                        ComponentUtils.getPropertyComponent (host);
                     if (comp != null &&
                         !ComponentUtils.isAncestorOf (comp, this)) {
                        myrefs.add (comp);
                     }
                  }
               }
            // XXX - other cases?
            }
         }
      }
      refs.addAll (myrefs);
   }

   /**
    * Information about a widget that was removed and may need to be returned.
    */
   private class WidgetRemoveInfo {
      Property myProp;
      LabeledComponentBase myComp;
      int myIdx;

      WidgetRemoveInfo (Property prop, LabeledComponentBase comp, int idx) {
         myProp = prop;
         myComp = comp;
         myIdx = idx;
      }
   }         

   /**
    * Information about an EditingProperty whose hostList was changed and may
    * need to be restored.
    */
   private class WidgetHostInfo {
      EditingProperty myProp;
      LabeledComponentBase myComp;
      HostList myHostList;

      WidgetHostInfo (
         EditingProperty prop, LabeledComponentBase comp, HostList hostList) {
         myProp = prop;
         myComp = comp;
         myHostList = hostList;
      }
      
      void restore() {
         myProp.setHostList (myHostList, /*isLive=*/true);
      }
   }         

   private Property getWidgetProperty (Component widget) {
      if (widget instanceof LabeledComponentBase) {
         LabeledComponentBase lwidget = (LabeledComponentBase)widget;
         return myPanel.getWidgetProperty (lwidget);
      }
      return null;
   }

   @Override
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      super.updateReferences (undo, undoInfo);

      boolean changed = false;
      if (undo) {
         Object obj;
         // check first for host lists that need to be restored
         while ((obj=undoInfo.removeFirst()) != NULL_OBJ) {
            WidgetHostInfo info = (WidgetHostInfo)obj;
            info.restore();
         }
         // check next for widgets that need to be restored
         while ((obj=undoInfo.removeFirst()) != NULL_OBJ) {
            WidgetRemoveInfo info = (WidgetRemoveInfo)obj;
            if (info.myProp != null) {
               myPanel.addPropertyWidget (info.myProp, info.myComp, info.myIdx);
            }
            else {
               // will be a CoordinateWidget
               myPanel.addWidget (info.myComp, info.myIdx);
            }
            changed = true;
         }
      }
      else {
         // remove soft references which aren't in the hierarchy any more:
         ArrayList<WidgetRemoveInfo> removeList =
            new ArrayList<WidgetRemoveInfo>();
         for (int i=0; i<myPanel.numWidgets(); i++) {
            Component widget = myPanel.getWidget(i);
            Property prop = getWidgetProperty (widget);
            boolean removeWidget = false;
            if (widget instanceof CoordinateWidget) {
               CoordinateWidget cwidget = (CoordinateWidget)widget;
               ModelComponent comp = cwidget.getHandle().getJoint();
               if (comp != null && !ComponentUtils.areConnected (this, comp)) {
                  removeWidget = true;
               }
            }
            else if (prop instanceof GenericPropertyHandle) {
               // standard property associated with a single host. If the
               // host's component has been removed, removed the widget.
               ModelComponent comp = ComponentUtils.getPropertyComponent (prop);
               if (comp != null &&
                   !ComponentUtils.areConnected (this, comp)) {
                  removeWidget = true;
               }
            }
            else if (prop instanceof EditingProperty) {
               // EditingProperty associated with multiple hosts. Store the
               // hosts which still have components in curHostList.
               EditingProperty eprop = (EditingProperty)prop;
               HostList hostList = eprop.getHostList();
               HostList curHostList = new HostList(hostList.numHosts());
               for (HasProperties host : hostList) {
                  ModelComponent comp =
                     ComponentUtils.getPropertyComponent (host);
                  if (comp == null ||
                      ComponentUtils.areConnected (this, comp)) {
                     curHostList.addHost (host);
                  }
               }
               if (curHostList.numHosts() == hostList.numHosts()) {
                  // all present; no need to change anything
               }
               else if (curHostList.numHosts() == 0) {
                  // all gone, remove the widget
                  removeWidget = true;
               }
               else {
                  // some gone; update the host list
                  LabeledComponentBase lwidget = (LabeledComponentBase)widget;
                  undoInfo.add (
                     new WidgetHostInfo (eprop, lwidget, hostList));
                  eprop.setHostList (curHostList, /*isLive=*/true);
               }
            }
            if (removeWidget) {
               LabeledComponentBase lwidget = (LabeledComponentBase)widget;
               removeList.add (new WidgetRemoveInfo (prop, lwidget, i));
               changed = true;
            }
            // TODO - handle other cases
         }
         for (WidgetRemoveInfo info : removeList) {
            myPanel.removeWidget (info.myComp);
         }
         undoInfo.addLast (NULL_OBJ); // terminator for widget host info 
         undoInfo.addAll (removeList);
         undoInfo.addLast (NULL_OBJ);
      }
      if (changed && myFrame != null) {
         notifyWidgetsChanged();
         myFrame.pack();
      }
   }

   public void connectToHierarchy (CompositeComponent hcomp) {
      if (hcomp == getParent()) {
         RootModel rootModel = RootModel.getRoot (this);
         if (rootModel != null) {
            setSynchronizeObject (rootModel);
            setFocusableWindowState (rootModel.isFocusable());
         }
         addGlobalValueChangeListener (myRerenderListener);
         pack();
         setVisible (true);
      }
      super.connectToHierarchy (hcomp);
   }

   public void disconnectFromHierarchy (CompositeComponent hcomp) {
      super.disconnectFromHierarchy(hcomp);
      if (hcomp == getParent()) {
         dispose();
      }
   }

   protected void notifyWidgetsChanged() {
   }
}
