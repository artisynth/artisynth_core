/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import javax.swing.*;

import java.util.*;

import maspack.widgets.MouseBindings.MouseAction;

public class MouseSettingsPanel extends PropertyPanel
   implements ValueChangeListener {
   
   private static final long serialVersionUID = -8163801337166221686L;
   protected BooleanSelector myAutoDetectField;
   protected StringSelector mySelector;
   protected LinkedHashMap<String,MouseBindings> myBindingsMap;
   protected MouseBindings myBindings = null;
   protected MouseBindings myEffectiveBindings = null;
   protected DoubleField myWheelZoom;
   protected ArrayList<StringField> myMaskInfos =
      new ArrayList<StringField>();
   
   public MouseSettingsPanel (
      MouseBindings bindings, List<MouseBindings> allBindings, double zoomScale) {
      build (bindings, allBindings, zoomScale);
   }

   protected void addMaskInfo (MouseAction action, MouseBindings settings) {
      StringField text =
         new StringField (action.getActionDescription(), 20);
      text.setEnabledAll (false);
      setMaskInfoText (action, text, settings);
      myMaskInfos.add (text);
      addWidget (text);
   }

   protected void setMaskInfoText (
      MouseAction action, StringField text, MouseBindings settings) {
      text.setValue (settings.maskToString(settings.getFullMask (action)));
   }

   protected void addLabel (String text) {
      JLabel label = new JLabel(text);
      GuiUtils.setItalicFont (label);
      addWidget (label);
   }

   protected void build (
      MouseBindings bindings, List<MouseBindings> allBindings, double zoomScale) {

      // build bindings map and list of binding names
      myBindingsMap = new LinkedHashMap<String,MouseBindings>();
      String[] allNames = new String[allBindings.size()];
      int k = 0;
      for (MouseBindings b : allBindings) {
         myBindingsMap.put (b.getName(), new MouseBindings(b));
         allNames[k++] = b.getName();
      }

      // create widgets; update later
      myAutoDetectField =
         new BooleanSelector ("Auto detect", false);
      addWidget (myAutoDetectField);

      mySelector = new StringSelector (
         "Bindings", allNames[0], allNames);
      addWidget (mySelector);

      // widget values will be update here
      setBindings (bindings);

      addLabel (" Viewpoint control:");
      addMaskInfo (MouseAction.ROTATE_VIEW, myEffectiveBindings);
      addMaskInfo (MouseAction.TRANSLATE_VIEW, myEffectiveBindings);
      addMaskInfo (MouseAction.ZOOM_VIEW, myEffectiveBindings);

      addLabel (" Component selection:");
      addMaskInfo (MouseAction.SELECT_COMPONENTS, myEffectiveBindings);
      addMaskInfo (MouseAction.MULTIPLE_SELECTION, myEffectiveBindings);
      addMaskInfo (MouseAction.ELLIPTIC_DESELECT, myEffectiveBindings);
      addMaskInfo (MouseAction.RESIZE_ELLIPTIC_CURSOR, myEffectiveBindings);
      addMaskInfo (MouseAction.CONTEXT_MENU, myEffectiveBindings);

      addLabel (" Manipulator controls:");
      addMaskInfo (MouseAction.MOVE_DRAGGER, myEffectiveBindings);
      addMaskInfo (MouseAction.DRAGGER_CONSTRAIN, myEffectiveBindings);
      addMaskInfo (MouseAction.DRAGGER_REPOSITION, myEffectiveBindings);

      JLabel label = new JLabel (
         " (LMB, MMB, RMB = left, middle, right mouse buttons)");
      GuiUtils.setItalicFont (label);
      addWidget (label);
      
      addWidget (new JSeparator());
      myWheelZoom = new DoubleField ("Wheel zoom scale", zoomScale);
      addWidget (myWheelZoom);

      // add value change listeners
      myAutoDetectField.addValueChangeListener (this);
      mySelector.addValueChangeListener (this);
   }

   public MouseBindings getBindings() {
      return myBindings;
   }

   public MouseBindings getEffectiveBindings() {
      return myEffectiveBindings;
   }

   public void setBindings (MouseBindings bindings) {

      boolean autoDetect = false;
      if (bindings.getName().equals ("Default")) {
         myBindings = MouseBindings.Default;
         myEffectiveBindings = MouseBindings.createDefaultBindings();
         autoDetect = true;
      }
      else {
         myBindings = myBindingsMap.get(bindings.getName());
         if (myBindings == null) {
            throw new IllegalArgumentException (
               "Unknown mouse bindings '"+bindings.getName()+"'");
         }
         myEffectiveBindings = myBindings;
      }
      mySelector.setValue (myEffectiveBindings.getName());
      mySelector.setEnabledAll (!autoDetect);
      myAutoDetectField.setValue (autoDetect);
   }

   public double getWheelZoom() {
      return myWheelZoom.getDoubleValue();
   }

   public void setWheelZoom (double zoom) {
      myWheelZoom.setValue(zoom);
   }

   public DoubleField getWheelZoomField() {
      return myWheelZoom;
   }

   public StringSelector getBindingsField() {
      return mySelector;
   }

   public BooleanSelector getAutoDetectField() {
      return myAutoDetectField;
   }

   private void updateMaskDisplay() {
      int k = 0;
      for (MouseAction action : MouseAction.values()) {
         setMaskInfoText (action, myMaskInfos.get(k++), myEffectiveBindings);
      }
      repaint();
   }

   public void valueChange (ValueChangeEvent evt) {
      Object source = evt.getSource();
      if (source == mySelector) {
         myBindings = myBindingsMap.get(mySelector.getValue());
         myEffectiveBindings = myBindings;
         updateMaskDisplay();
      }
      else if (source == myAutoDetectField) {
         boolean autoDetect = myAutoDetectField.getBooleanValue();
         if (autoDetect) {
            myBindings = MouseBindings.Default;
            myEffectiveBindings = MouseBindings.createDefaultBindings();
            mySelector.maskValueChangeListeners (true);
            mySelector.setValue (myEffectiveBindings.getName());
            mySelector.maskValueChangeListeners (false);
            mySelector.setEnabledAll (false);
            updateMaskDisplay();
         }
         else {
            myBindings = myEffectiveBindings;
            mySelector.setEnabledAll (true);
         }
      }
   }

}
