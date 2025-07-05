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
import maspack.widgets.DoubleFieldSlider;
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
 * joint coordinate values. These widgets are instances of {@link
 * CoordinateWidget}, which are subclassed of {@link DoubleFieldSlider}.
 *
 * <p>A CoordinatePanel is associated with a MechModel, which is assumed to
 * contain the joints whose coordinates are represented by the panel.  When
 * coordinate values are changed through the panel, the MechModel is asked to
 * update various components, including the wrap paths for {@link
 * MultiPointSpring}s and the positions of attached components.
 */
public class CoordinatePanel extends ControlPanel
   implements ValueChangeListener {

   MechModel myMech;
   int myMechVersion = -1;
   ArrayList<CoordinateWidget> myCoordWidgets;
   ArrayList<MultiPointSpring> myMultiPointSprings;

   public CoordinatePanel() {
      this (null, null);
   }

   /**
    * Creates a CoordinatePanel with a specified name and {@link MechModel}.
    *
    * @param name name of the panel
    * @param mech {@code MechModel} associated with the panel
    */
   public CoordinatePanel (String name, MechModel mech) {
      super (name, null);
      setMechModel (mech);
   }

   /**
    * Creates a CoordinatePanel with a specified name, {@link MechModel}, and
    * options.
    *
    * @param name name of the panel
    * @param mech {@code MechModel} associated with the panel
    * @param options options for the panel
    */
   public CoordinatePanel (String name, MechModel mech, String options) {
      super (name, options);
      setMechModel (mech);
   }

   /**
    * Queries the {@link MechModel} associated with this panel.
    *
    * @return MechModel associated with this panel
    */
   public MechModel getMechModel() {
      return myMech;
   }

   /**
    * Sets the {@link MechModel} associated with this panel.
    *
    * @param mech MechModel to associate with this panel
    */
   public void setMechModel (MechModel mech) {
      myMech = mech;
   }

   /**
    * Creates and adds a widget to this panel for the named coordinate of a
    * specified joint. The widget's label is set to the coordinate name.
    *
    * @param joint joint containing the coordinate
    * @param cname name of the coordinate
    * @return created widget
    */
   public CoordinateWidget addCoordinateWidget (
      JointBase joint, String cname) {
      int cidx = joint.getCoordinateIndex (cname);
      if (cidx == -1) {
         throw new IllegalArgumentException (
            "Coordinate name "+cname+" not found in joint");
      }
      return addCoordinateWidget (null, joint, cidx);
   }

   /**
    * Creates and adds a widget to this panel for the named coordinate of a
    * specified joint.
    *
    * @param label widget label 
    * @param joint joint containing the coordinate
    * @param cname name of the coordinate
    * @return created widget
    */
   public CoordinateWidget addCoordinateWidget (
      String label, JointBase joint, String cname) {
      int cidx = joint.getCoordinateIndex (cname);
      if (cidx == -1) {
         throw new IllegalArgumentException (
            "Coordinate name "+cname+" not found in joint");
      }
      return addCoordinateWidget (label, joint, cidx);
   }

   /**
    * Creates and adds a widget to this panel for the {@code cidx}-th
    * coordinate of a specified joint.
    *
    * @param label widget label 
    * @param joint joint containing the coordinate
    * @param cidx coordinate index
    * @return created widget
    */
   public CoordinateWidget addCoordinateWidget (
      String label, JointBase joint, int cidx) {  
      CoordinateWidget widget = new CoordinateWidget (label, joint, cidx);
      widget.clearValueChangeListeners();
      widget.addValueChangeListener (this);
      addWidget (widget);
      notifyWidgetsChanged();
      return widget;
   }

   /**
    * Creates and adds a widget to this panel for the {@code cidx}-th
    * coordinate of a specified joint. The widget's label is set to the
    * coordinate name.
    *
    * @param joint joint containing the coordinate
    * @param cidx coordinate index
    * @return created widget
    */
   public CoordinateWidget addCoordinateWidget (JointBase joint, int cidx) {  
      return addCoordinateWidget (null, joint, cidx);
   }

   /**
    * Creates and adds a widget to this panel for the {@code cidx}-th
    * coordinate of a specified joint.
    *
    * @param label widget label 
    * @param joint joint containing the coordinate
    * @param cidx coordinate index
    * @param min minimum widget value
    * @param max maximum widget value
    * @return created widget
    */
   public CoordinateWidget addCoordinateWidget (
      String label, JointBase joint, int cidx, double min, double max) {
      CoordinateWidget widget =
         new CoordinateWidget (label, joint, cidx, min, max);
      widget.clearValueChangeListeners();
      widget.addValueChangeListener (this);
      addWidget (widget);
      notifyWidgetsChanged();
      return widget;
   }

   /**
    * Creates and adds a widget to this panel for the {@code cidx}-th
    * coordinate of a specified joint. The widget's label is set to the
    * coordinate name.
    *
    * @param joint joint containing the coordinate
    * @param cidx coordinate index
    * @param min minimum widget value
    * @param max maximum widget value
    * @return created widget
    */
   public CoordinateWidget addCoordinateWidget (
      JointBase joint, int cidx, double min, double max) {
      return addCoordinateWidget (null, joint, cidx, min, max);
   }

   /**
    * Creates and adds widgets to this panel for all the coordinate of a
    * specified joint. The widget labels are set from the coordinate names.
    *
    * @param joint joint containing the coordinates
    * @return list of the created widgets
    */
   public List<CoordinateWidget> addCoordinateWidgets (JointBase joint) {
      ArrayList<CoordinateWidget> cwidgets = new ArrayList<>();
      for (int idx=0; idx<joint.numCoordinates(); idx++) {
         cwidgets.add (addCoordinateWidget (null, joint, idx));
      }
      return cwidgets;
   }

   /**
    * Returns a list of all the coordinate widgets currently in this panel.
    *
    * @return list of all coordinate widgets
    */
   public List<CoordinateWidget> getCoordinateWidgets() {
      ArrayList<CoordinateWidget> cwidgets = new ArrayList<>();
      for (int k=0; k<myPanel.numWidgets(); k++) {
         Component w = myPanel.getWidget(k);
         if (w instanceof CoordinateWidget) {
            cwidgets.add ((CoordinateWidget)w);
         }
      }
      return cwidgets;
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
