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
import maspack.properties.*;
import maspack.util.*;
import maspack.widgets.*;
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
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import artisynth.core.util.ObjectToken;

/**
 * A subclass of {@link ControlPanel} with support for widgets that control
 * joint coordinate values.
 */
public class CoordinatePanel extends ControlPanel
   implements ValueChangeListener {

   MechModel myMech;
   int myMechVersion = -1;
   ArrayList<CoordinateWidget> myCoordWidgets;
   ArrayList<MultiPointSpring> myMultiPointSprings;

   public CoordinatePanel() {
      this (null);
   }

   public CoordinatePanel (String name) {
      super (name, null);
   }

   public CoordinatePanel (String name, String options) {
      super (name, options);
   }

   public MechModel getMechModel() {
      return myMech;
   }

   public void setMechModel (MechModel mech) {
      myMech = mech;
   }

   public CoordinateWidget addCoordinateWidget (
      JointBase joint, String coordName) {
      int coordIdx = joint.getCoordinateIndex (coordName);
      if (coordIdx == -1) {
         throw new IllegalArgumentException (
            "Coordinate name "+coordName+" not found in joint");
      }
      return addCoordinateWidget (null, joint, coordIdx);
   }


   public CoordinateWidget addCoordinateWidget (
      String label, JointBase joint, String coordName) {
      int coordIdx = joint.getCoordinateIndex (coordName);
      if (coordIdx == -1) {
         throw new IllegalArgumentException (
            "Coordinate name "+coordName+" not found in joint");
      }
      return addCoordinateWidget (label, joint, coordIdx);
   }

   public CoordinateWidget addCoordinateWidget (
      String label, JointBase joint, int coordIdx) {  
      CoordinateWidget widget = new CoordinateWidget (label, joint, coordIdx);
      widget.clearValueChangeListeners();
      widget.addValueChangeListener (this);
      addWidget (widget);
      notifyWidgetsChanged();
      return widget;
   }

   public CoordinateWidget addCoordinateWidget (JointBase joint, int coordIdx) {  
      return addCoordinateWidget (null, joint, coordIdx);
   }

   public void addCoordinateWidgets (JointBase joint) {
      for (int idx=0; idx<joint.numCoordinates(); idx++) {
         addCoordinateWidget (null, joint, idx);
      }
   }

   public void valueChange (ValueChangeEvent e) {
      if (e.getSource() instanceof CoordinateWidget) {
         JointCoordinateHandle handle =
            ((CoordinateWidget)e.getSource()).getHandle();
         handle.setValueDeg ((Double)e.getValue());
         if (myMech != null) {
            myMech.updatePosState();
            updateWrapPaths();
         }
      }
   }

   public void updateComponentCaches() {
      if (myMechVersion != myMech.getStructureVersion()) {
         myMultiPointSprings = null;
      }
      if (myMultiPointSprings == null) {
         myMultiPointSprings = new ArrayList<>();
         myMech.recursivelyFind (myMultiPointSprings, MultiPointSpring.class);
         myMechVersion = myMech.getStructureVersion();
      }
      updateCoordinateWidgetList();
   }

   public void updateCoordinateWidgetList() {
      if (myCoordWidgets == null) {
         myCoordWidgets = new ArrayList<>();
         for (int i=0; i<myPanel.numWidgets(); i++) {
            Component comp = myPanel.getWidget(i);
            if (comp instanceof CoordinateWidget) {
               myCoordWidgets.add ((CoordinateWidget)comp);
            }
         }
      }
   }

   public void updateWrapPaths() {
      updateComponentCaches();
      for (MultiPointSpring spr : myMultiPointSprings) {
         spr.updateWrapSegments();
      }
   }

   public void updateWidgetValues() {
      super.updateWidgetValues();
      updateComponentCaches();
      if (myFrame != null) {
         for (CoordinateWidget widget : myCoordWidgets) {
            widget.updateValue();
         }
      }
   }

   /* --- I/O methods --- */

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor) 
      throws IOException {
      if (myMech != null) {
         pw.println (
            "mechmodel="+ComponentUtils.getWritePathName (ancestor, myMech));
      }
      super.writeItems (pw, fmt, ancestor);
   }

   public boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "mechmodel", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "mechmodel")) {
         myMech = postscanReference (tokens, MechModel.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      // replace coordinate widget value change listeners with this panel
      updateCoordinateWidgetList();
      for (CoordinateWidget widget : myCoordWidgets) {
         widget.clearValueChangeListeners();
         widget.addValueChangeListener (this);
      }
   }     

   protected void notifyWidgetsChanged() {
      myCoordWidgets = null;
   }

}
