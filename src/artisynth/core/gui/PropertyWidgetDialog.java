/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JSeparator;

import maspack.properties.CompositeProperty;
import maspack.properties.EditingProperty;
import maspack.properties.HostList;
import maspack.properties.PropTreeCell;
import maspack.properties.Property;
import maspack.properties.PropertyInfo;
import maspack.util.DoubleInterval;
import maspack.util.InternalErrorException;
import maspack.util.NumericInterval;
import maspack.util.StringHolder;
import maspack.widgets.BooleanSelector;
import maspack.widgets.ColorSelector;
import maspack.widgets.DoubleIntervalField;
import maspack.widgets.LabeledComponentBase;
import maspack.widgets.LabeledComponentPanel;
import maspack.widgets.OptionPanel;
import maspack.widgets.PropertyWidget;
import maspack.widgets.StringField;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.widgets.ValueCheckListener;
import artisynth.core.driver.Main;
import artisynth.core.gui.widgets.PropertyField;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;

public class PropertyWidgetDialog extends JDialog implements ActionListener,
ValueChangeListener {
   Main myMain;
   // PropertyChooser myChooser;
   PropertyField myPropField;
   BooleanSelector mySliderSelector;

   DoubleIntervalField myRangeField;
   ColorSelector myLabelFontColorSelector;
   ColorSelector myBackgroundColorSelector;
   StringField myLabelTextField;

   JButton myOKButton;
   boolean myDisposed = false;
   int myReturnValue = OptionPanel.CANCEL_OPTION;

   private static double inf = Double.POSITIVE_INFINITY;

   private static HashMap<PropertyInfo,DoubleInterval> myRangeMap =
      new HashMap<PropertyInfo,DoubleInterval>();

   public PropertyWidgetDialog (Dialog dialog, String title, Main main) {
      super (dialog, title);
      myMain = main;
      initialize();
   }

   public PropertyWidgetDialog (Frame frame, String title, Main main) {
      super (frame, title);
      myMain = main;
      initialize();
   }

   private void initialize() {
      setDefaultCloseOperation (DISPOSE_ON_CLOSE);
      Container pane = getContentPane();
      pane.setLayout (new BoxLayout (pane, BoxLayout.Y_AXIS));

      LabeledComponentPanel propPanel = new LabeledComponentPanel();
      propPanel.setAlignmentX (Component.CENTER_ALIGNMENT);

      // myChooser = new PropertyChooser ("component/property", 30, myMain);
      // myChooser.setAlignmentX (Component.CENTER_ALIGNMENT);
      // myChooser.addValueChangeListener (this);
      // pane.add (Box.createRigidArea (new Dimension (0, 4)));
      // propPanel.addWidget (myChooser);

      myPropField = new PropertyField ("component/property", 30, myMain);
      myPropField.setAlignmentX (Component.CENTER_ALIGNMENT);
      myPropField.addValueChangeListener (this);
      propPanel.addWidget (myPropField);

      propPanel.addWidget (new JSeparator());

      mySliderSelector = new BooleanSelector ("slider");
      mySliderSelector.addValueChangeListener (this);
      propPanel.addWidget (mySliderSelector);

      myRangeField = new DoubleIntervalField (
         "range", new DoubleInterval (0, 1), "%.6g");
//          new VectorMultiField (
//             "range", new String[] { "min:", "max:" }, new Vector2d (0, 1),
//             "%.6f");
      // myRangeField.setAlignmentX (Component.CENTER_ALIGNMENT);
      propPanel.addWidget (myRangeField);

      myLabelTextField = new StringField ("labelText", 20);
      myLabelTextField.setEnabledAll (false);
      myLabelTextField.addValueCheckListener (new ValueCheckListener() {
         public Object validateValue (ValueChangeEvent e, StringHolder errMsg) {
            String newName = (String)e.getValue();
            String err = ModelComponentBase.checkName (newName, null);
            if (err != null) {
               err = "Invalid name '" + newName + "': " + err;
               newName = null;
            }
            if (errMsg != null) {
               errMsg.value = err;
            }
            return newName;
         }
      });

      propPanel.addWidget (myLabelTextField);

      myLabelFontColorSelector = new ColorSelector ("labelFontColor");
      myLabelFontColorSelector.enableNullColors();
      myLabelFontColorSelector.setValue (null);
      propPanel.addWidget (myLabelFontColorSelector);

      myBackgroundColorSelector = new ColorSelector ("backgroundColor");
      myBackgroundColorSelector.enableNullColors();
      myBackgroundColorSelector.setValue (null);
      propPanel.addWidget (myBackgroundColorSelector);

      // sliderPanel.add (myRangeField);

      // pane.add (Box.createRigidArea (new Dimension (0, 4)));
      // pane.add (sliderPanel);
      pane.add (propPanel);
      pane.add (new JSeparator());

      // set slider value here because valueChanged needs some
      // other widgets instantiated.
      mySliderSelector.setValue (false);
      mySliderSelector.setEnabledAll (false);

      OptionPanel options = new OptionPanel ("OK Cancel", this);
      myOKButton = options.getButton ("OK");
      myOKButton.setEnabled (false);
      options.setAlignmentX (Component.CENTER_ALIGNMENT);
      pane.add (options);
      pack();
   }

   @Override
   public void dispose() {
      if (!myDisposed) {
         myPropField.dispose();
         myRangeField.dispose();
         mySliderSelector.dispose();
         myDisposed = true;
      }
      super.dispose();
   }

   public int getReturnValue() {
      return myReturnValue;
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("OK")) {
         setVisible (false);
         saveRange (myPropField.getPropertyInfo());
         myReturnValue = OptionPanel.OK_OPTION;
         dispose();
      }
      else if (cmd.equals ("Cancel")) {
         setVisible (false);
         dispose();
      }
   }

   public static PropertyWidgetDialog createDialog (
      Window owner, String title, Main main) {
      if (owner instanceof Dialog) {
         return new PropertyWidgetDialog ((Dialog)owner, title, main);
      }
      else if (owner instanceof Frame) {
         return new PropertyWidgetDialog ((Frame)owner, title, main);
      }
      else {
         throw new InternalErrorException ("Unsupported window type: " + owner);
      }
   }

   public PropertyInfo getPropertyInfo() {
      return myPropField.getPropertyInfo();
   }

   public String getPropertyPath() {
      return myPropField.getPropertyPath();
   }

   public ModelComponent getModelComponent() {
      return myPropField.getModelComponent();
   }

   public boolean isSliderSelected() {
      return mySliderSelector.getBooleanValue();
   }

   public double getSliderMinimum() {
      return myRangeField.getLowerBound();
   }

   public double getSliderMaximum() {
      return myRangeField.getUpperBound();
   }

   private DoubleInterval getDefaultRange (Property prop) {
      DoubleInterval range = myRangeMap.get (prop.getInfo());
      if (range == null) {
         NumericInterval nrange = PropertyWidget.getNumericRange (prop);
         if (nrange != null) {
            range = new DoubleInterval (nrange);
         }
         else {
            range = new DoubleInterval (0, 1);
         }
         myRangeMap.put (prop.getInfo(), range);
      }
      return range;
   }

   private EditingProperty createEditingProperty (Property prop) {
      if (prop.getHost() == null) {
         throw new InternalErrorException ("Property " + prop.getName()
         + " does not have host");
      }
      HostList hostList = new HostList (1);
      hostList.addHost (prop.getHost());
      // get single property from host list
      String[] restricted = new String[] { prop.getName() };
      String[] excluded = null;
      PropTreeCell tree =
         hostList.commonProperties (
            null, /* allowReadonly= */false, restricted, excluded);
      if (tree.numChildren() == 0) {
         throw new InternalErrorException ("Property " + prop.getName()
         + " not found in host");
      }
      // expand save data in hostList down one level ...
      hostList.saveBackupValues (tree);
      return new EditingProperty (
         tree.getFirstChild(), hostList, /* live= */true);
   }

   public LabeledComponentBase createWidget() {
      if (getModelComponent() == null || getPropertyInfo() == null) {
         throw new IllegalStateException ("component and/or property not set");
      }
      LabeledComponentBase widget;
      ModelComponent comp = getModelComponent();
      String propPath = getPropertyPath();
      Property prop = comp.getProperty (propPath);
      if (prop == null) {
         throw new InternalErrorException ("property '" + propPath
         + "' not found for component " + comp.getClass());
      }

      if (CompositeProperty.class.isAssignableFrom (
             prop.getInfo().getValueClass())) { 
         // replace prop with an EditingProperty, since
         // CompositePropertyWidgets only work properly with those
         prop = createEditingProperty (prop);
      }

      if (isSliderSelected()) {
         widget =
            PropertyWidget.create (
               prop, getSliderMinimum(), getSliderMaximum());
      }
      else {
         widget = PropertyWidget.create (prop);
      }
      String labelText = myLabelTextField.getStringValue();
      widget.setLabelText (labelText);
      if (!myLabelFontColorSelector.valueIsNull()) {
         widget.setLabelFontColor (myLabelFontColorSelector.getColor());
      }
      if (!myBackgroundColorSelector.valueIsNull()) {
         widget.setBackgroundColor (myBackgroundColorSelector.getColor());
      }
      return widget;
   }

   private void updateRange (ModelComponent comp, PropertyInfo info) {
      Property prop = comp.getProperty (info.getName());
      if (prop == null) {
         throw new InternalErrorException (
            "Cannot create property '"+info.getName()+
            " for component type "+comp.getClass());
      }
      DoubleInterval newRange = getDefaultRange (prop);
      double currentValue = ((Number)prop.get()).doubleValue();
      if (currentValue < newRange.getLowerBound()) {
         newRange.setLowerBound (currentValue);
      }
      else if (currentValue > newRange.getUpperBound()) {
         newRange.setUpperBound (currentValue);
      }
      myRangeField.setValue (newRange);
   }

   private void saveRange (PropertyInfo info) {
      if (info != null) { // save old range, if applicable
         DoubleInterval oldRange = myRangeMap.get (info);
         if (oldRange != null) {
            oldRange.set (myRangeField.getRangeValue());
         }
      }
   }

   public void valueChange (ValueChangeEvent e) {
      if (e.getSource() == myPropField) {

         PropertyInfo info = myPropField.getPropertyInfo();
         ModelComponent comp = myPropField.getModelComponent();

         if (info != null && PropertyWidget.canCreateWithSlider (info)) {
            mySliderSelector.setEnabledAll (true);
            mySliderSelector.setValue (true);
            updateRange (comp, info);
         }
         else {
            mySliderSelector.setValue (false);
            mySliderSelector.setEnabledAll (false);
         }
         if (info != null && comp != null) {
            myOKButton.setEnabled (true);
            myLabelTextField.setEnabledAll (true);
            myLabelTextField.setValue (info.getName());
         }
         else {
            myOKButton.setEnabled (false);
            myLabelTextField.setEnabledAll (false);
            myLabelTextField.setValue (null);
         }
      }
      else if (e.getSource() == mySliderSelector) {
         boolean sliderChoosen = (Boolean)mySliderSelector.getValue();
         myRangeField.setEnabledAll (sliderChoosen);
      }
   }
}
