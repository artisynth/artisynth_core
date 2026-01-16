/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui;

import java.awt.Component;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.swing.*;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import artisynth.core.mechmodels.CoordinateSetter;
import artisynth.core.mechmodels.JointBase;
import artisynth.core.mechmodels.JointCoordinateHandle;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.CoordinateSetter.SetStatus;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.util.ScanToken;
import maspack.spatialmotion.RigidBodyConstraint.MotionType;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.widgets.BooleanSelector;
import maspack.widgets.*;
import maspack.widgets.DoubleFieldSlider;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;

import maspack.properties.*;

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

   private static final double RTOD = 180/Math.PI;
   private static final double DTOR = Math.PI/180;

   MechModel myMech;
   int myMechVersion = -1;
   ArrayList<CoordinateWidget> myCoordWidgets;
   ArrayList<MultiPointSpring> myMultiPointSprings;
   CoordinateSetter myCoordSetter;
   SettingsPanel mySettingsPanel;
   boolean myRequestMode = false;

   public static boolean DEFAULT_USE_DEGREES = true;
   boolean myUseDegrees = DEFAULT_USE_DEGREES;

   public static PropertyList myProps =
      new PropertyList (CoordinatePanel.class, ControlPanel.class);

   static {
      myProps.add (
         "useDegrees", "use degrees for rotary coordinates",
         DEFAULT_USE_DEGREES);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Queries whether this coordinate panel uses degrees or radians
    * for rotary coordinate values.
    *
    * @return {@code true} if the panel uses degrees
    */
   public boolean getUseDegrees() {
      return myUseDegrees;
   }

   /**
    * Sets whether this coordinate panel uses degrees or radians for rotary
    * coordinate values.
    *
    * @param enable if {@code true}, the panel will use degrees
    */
   public void setUseDegrees (boolean enable) {
      if (enable != myUseDegrees) {
         if (myCoordWidgets != null) {
            for (CoordinateWidget w : myCoordWidgets) {
               w.setUseDegrees (enable);
            }
         }
         myUseDegrees = enable;
      }
   }

   public static class SettingsPanel extends PropertyPanel {
      
      JButton myRequestSetButton;
      BooleanSelector myRequestModeSelector;
      BooleanSelector myUseDegreesButton;

      public SettingsPanel() {
         myUseDegreesButton = new BooleanSelector ("degrees");
         myUseDegreesButton.setToolTipText (
            "Use degrees for rotary coordinates");        
         addWidget (myUseDegreesButton);

         myRequestSetButton = new JButton ("Set");
         myRequestSetButton.setActionCommand ("set coords");
         myRequestSetButton.setEnabled (false);

         myRequestModeSelector = new BooleanSelector ("multiset");
         myRequestModeSelector.setToolTipText (
            "Defer setting coordinates until the 'Set' button is pressed");
         myRequestModeSelector.add (Box.createHorizontalGlue());
         myRequestModeSelector.add (myRequestSetButton);
         myRequestModeSelector.add (Box.createRigidArea(new Dimension(2, 0)));
         myRequestModeSelector.setStretchable(true);

         addWidget (myRequestModeSelector);
      }
   }

   /**
    * No-args constructor required for write/scan.
    */
   public CoordinatePanel() {
      this (null, null);
   }

   /**
    * Creates a CoordinatePanel with a specified {@link MechModel}.
    *
    * @param mech {@code MechModel} associated with the panel
    */
   public CoordinatePanel (MechModel mech) {
      super ("coordinate settings", null);
      setMechModel (mech);
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
      return addCoordinateWidget (null, new JointCoordinateHandle (joint, cidx));
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
      return addCoordinateWidget (null, new JointCoordinateHandle (joint, cidx));
   }

   /**
    * Creates and adds a widget to this panel for the {@code cidx}-th
    * coordinate of a specified joint.
    *
    * @param label label optional widget label; if {@code null}, the label is set
    * to the coordinate name
    * @param joint joint containing the coordinate
    * @param cidx coordinate index
    * @return created widget
    */
   public CoordinateWidget addCoordinateWidget (
      String label, JointBase joint, int cidx) {  
      return addCoordinateWidget (label, new JointCoordinateHandle (joint, cidx));
   }

   /**
    * Creates and adds a widget to this panel for the coordinate specified by a
    * handle. The widget's label is set to the coordinate name.
    *
    * @param handle handle specifying the coordinate's joint and index
    * @return created widget
    */
   public CoordinateWidget addCoordinateWidget (JointCoordinateHandle handle) {
      return addCoordinateWidget (null, handle);
   }

   /**
    * Creates and adds a widget to this panel for the coordinate specified by a
    * handle.
    *
    * @param label optional widget label; if {@code null}, the label is set
    * to the coordinate name
    * @param handle handle specifying the coordinate's joint and index
    * @return created widget
    */
   public CoordinateWidget addCoordinateWidget (
      String label, JointCoordinateHandle handle) {
      CoordinateWidget widget =
         new CoordinateWidget (label, handle, myUseDegrees);
      widget.clearValueChangeListeners();
      widget.addValueChangeListener (this);
      addWidget (widget);
      notifyWidgetsChanged();
      return widget;
   }

   /**
    * Creates and adds a widget to this panel for the coordinate specified by a
    * handle. The widget's label is set to the coordinate name.
    *
    * @param handle handle specifying the coordinate's joint and index
    * @param min minimum widget value
    * @param max maximum widget value
    * @return created widget
    */
   public CoordinateWidget addCoordinateWidget (
      JointCoordinateHandle handle, double min, double max) {
      return addCoordinateWidget (null, handle, min, max);
   }

   /**
    * Creates and adds a widget to this panel for the coordinate specified by a
    * handle.
    *
    * @param label optional widget label; if {@code null}, the label is set
    * to the coordinate name
    * @param handle handle specifying the coordinate's joint and index
    * @param min minimum widget value
    * @param max maximum widget value
    * @return created widget
    */
   public CoordinateWidget addCoordinateWidget (
      String label, JointCoordinateHandle handle, double min, double max) {
      CoordinateWidget widget =
         new CoordinateWidget (label, handle, min, max, myUseDegrees);
      widget.clearValueChangeListeners();
      widget.addValueChangeListener (this);
      addWidget (widget);
      notifyWidgetsChanged();
      return widget;
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
         cwidgets.add (addCoordinateWidget (
                          null, new JointCoordinateHandle (joint, idx)));
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

   /**
    * Gets the coordinate widget with the specified label, or {@code null} if
    * no such widget is present.
    *
    * @param label label of the widget being sought
    * @return coordinate widget
    */
   public CoordinateWidget getCoordinateWidget (String label) {
      for (int k=0; k<myPanel.numWidgets(); k++) {
         Component w = myPanel.getWidget(k);
         if (w instanceof CoordinateWidget) {
            CoordinateWidget cw = (CoordinateWidget)w;
            if (cw.getLabelText().equals(label)) {
               return cw;
            }
         }
      }
      return null;
   }

   /**
    * Sets the coordinate associated with the specified widget label to a given
    * value.
    *
    * @param label label of the widget associated with the coordinate
    * @param value desired coordinate value
    */
   public SetStatus setCoordinate (String label, double value) {
      CoordinateWidget widget = getCoordinateWidget (label);
      if (widget == null) {
         throw new IllegalArgumentException ("no widget with label " + label);
      }
      return getCoordinateSetter().setCoordinate (widget.getHandle(), value);
   }

   /**
    * Queries the coordinate associated with the specified widget label.
    *
    * @param label label of the widget associated with the coordinate
    * @return coordinate value
    */
   public double getCoordinate (String label) {
      CoordinateWidget widget = getCoordinateWidget (label);
      if (widget == null) {
         throw new IllegalArgumentException ("no widget with label " + label);
      }
      return widget.getHandle().getValue();
   }

   /**
    * Sets the coordinate associated with the specified widget label to a given
    * value. For rotary coordinates, the value should be given in degrees.
    *
    * @param label label of the widget associated with the coordinate
    * @param value desired coordinate value
    */
   public SetStatus setCoordinateDeg (String label, double value) {
      CoordinateWidget widget = getCoordinateWidget (label);
      if (widget == null) {
         throw new IllegalArgumentException ("no widget with label " + label);
      }
      return getCoordinateSetter().setCoordinateDeg (widget.getHandle(), value);
   }

   /**
    * Queries the coordinate associated with the specified widget label.
    * For rotary coordinates, the value is given in degrees.
    *
    * @param label label of the widget associated with the coordinate
    * @return coordinate value
    */
   public double getCoordinateDeg (String label) {
      CoordinateWidget widget = getCoordinateWidget (label);
      if (widget == null) {
         throw new IllegalArgumentException ("no widget with label " + label);
      }
      return widget.getHandle().getValueDeg();
   }

   /**
    * Queues a request to set the coordinate associated with the specified
    * widget label to a given value.
    *
    * @param label label of the widget associated with the coordinate
    * @param value desired coordinate value
    */
   public void setRequest (String label, double value) {
      CoordinateWidget widget = getCoordinateWidget (label);
      if (widget == null) {
         throw new IllegalArgumentException ("no widget with label " + label);
      }
      getCoordinateSetter().request (widget.getHandle(), value);
   }

   /**
    * Queues a request to set the coordinate associated with the specified
    * widget label to a given value. For rotary coordinates, the value should
    * be given in degrees.
    *
    * @param label label of the widget associated with the coordinate
    * @param value desired coordinate value
    */
   public void setRequestDeg (String label, double value) {
      CoordinateWidget widget = getCoordinateWidget (label);
      if (widget == null) {
         throw new IllegalArgumentException ("no widget with label " + label);
      }
      getCoordinateSetter().requestDeg (widget.getHandle(), value);
   }

   /**
    * Applies all outstanding coordinate set requests.
    */
   public SetStatus setCoordinates () {
      return getCoordinateSetter().setCoordinates();
   }

   /**
    * Adds a <i>settings panel</i> to this coordinate panel that exposes
    * various settings, including:
    *
    * <ol>
    *
    * <li> <i>use degrees</i>, controlling whether rotatary coordinates
    * values are expressed in degrees or radians;
    *
    * <li> <i>multiset mode</i>, allowing multiple coordinate values to
    * be set simultaneously. In <i>multiset mode</i>, changes to coordinate
    * widget values do not result in actual coordinate changes until the
    * panel's {@code Set} button is clicked, at which point all coordinates are
    * set at once.
    *
    * </ol>
    */
   public void addSettingsPanel() {
      connectSettingsPanel (new SettingsPanel());
      addWidget (new JSeparator());
      addWidget (mySettingsPanel);
   }

   protected void connectSettingsPanel (SettingsPanel panel) {
      mySettingsPanel = panel;
      mySettingsPanel.myRequestSetButton.addActionListener (this);
      mySettingsPanel.myRequestModeSelector.addValueChangeListener (this);
      mySettingsPanel.myUseDegreesButton.setValue (myUseDegrees);
      mySettingsPanel.myUseDegreesButton.addValueChangeListener (this);
   }

   /**
    * Returns the coordinate setter used by this panel.
    */
   public CoordinateSetter getCoordinateSetter() {
      if (myCoordSetter == null) {
         myCoordSetter = new CoordinateSetter (myMech);
      }
      return myCoordSetter;
   }

   public void actionPerformed (ActionEvent e) {
      String actionCmd = e.getActionCommand();
      if (actionCmd.equals ("set coords")) {
         if (myRequestMode) {
            getCoordinateSetter().setCoordinates();
            updateCoordinateWidgets();
            rerender();
         }
      }
   }

   public void valueChange (ValueChangeEvent e) {
      if (e.getSource() instanceof CoordinateWidget) {
         JointCoordinateHandle handle =
            ((CoordinateWidget)e.getSource()).getHandle();
         double value = (Double)e.getValue();
         if (myUseDegrees && handle.getMotionType() == MotionType.ROTARY) {
            value *= DTOR;
         }
         if (myRequestMode) {
            getCoordinateSetter().request (handle, value);
         }
         else {
            getCoordinateSetter().setCoordinate (handle, value);
            // getCoordinateSetter().setCoordinate (
            //    handle.getJoint(), handle.getIndex(), value);
         }
      }
      else if (mySettingsPanel != null) {
         if (e.getSource() == mySettingsPanel.myRequestModeSelector) {
            myRequestMode =
               mySettingsPanel.myRequestModeSelector.getBooleanValue();
            if (!myRequestMode) {
               getCoordinateSetter().clearRequests();
               updateWidgetValues();
            }
            mySettingsPanel.myRequestSetButton.setEnabled (myRequestMode);
         }
         else if (e.getSource() == mySettingsPanel.myUseDegreesButton) {
            setUseDegrees (mySettingsPanel.myUseDegreesButton.getBooleanValue());
         }
      }
   }

   public void updateComponentCaches() {
      if (myMech != null) {
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
      else {
         System.out.println (
            "ERROR: CoordinatePanel created without MechModel");
      }
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

   protected void updateCoordinateWidgets() {
      for (CoordinateWidget widget : myCoordWidgets) {
         widget.updateValue();
      }
   }

   public void updateWidgetValues() {
      super.updateWidgetValues();
      updateComponentCaches();
      if (myFrame != null) {
         if (!myRequestMode) {
            updateCoordinateWidgets();
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

   /**
    * {@inheritDoc}
    */
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

   /**
    * {@inheritDoc}
    */
   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      // replace coordinate widget value change listeners with this panel
      updateCoordinateWidgetList();
      for (CoordinateWidget widget : myCoordWidgets) {
         widget.clearValueChangeListeners();
         widget.addValueChangeListener (this);
      }
      for (int i=0; i<myPanel.numWidgets(); i++) {
         Component comp = myPanel.getWidget(i);
         if (comp instanceof SettingsPanel) {
            connectSettingsPanel ((SettingsPanel)comp);
         }
      }
   }     

   protected void notifyWidgetsChanged() {
      myCoordWidgets = null;
   }


}
