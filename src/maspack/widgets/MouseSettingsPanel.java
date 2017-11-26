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
   protected StringSelector mySelector;
   protected LinkedHashMap<String,MouseBindings> myBindingsMap;
   protected MouseBindings myBindings = null;
   protected DoubleField myWheelZoom;
   protected ArrayList<StringField> myMaskInfos =
      new ArrayList<StringField>();
   
   public MouseSettingsPanel (
      MouseBindings bindings, List<MouseBindings> allBindings, double zoomScale) {
      build (bindings, allBindings, zoomScale);
   }

   protected void addMaskInfo (MouseAction action, MouseBindings settings) {
      StringField text =
         new StringField (action.getActionDescription(), 30);
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

      myBindingsMap = new LinkedHashMap<String,MouseBindings>();
      myBindings = null;
      String[] allNames = new String[allBindings.size()];
      int k = 0;
      for (MouseBindings b : allBindings) {
         myBindingsMap.put (b.getName(), new MouseBindings(b));
         if (b.equals (bindings)) {
            myBindings = b;
         }
         allNames[k++] = b.getName();
      }
      if (myBindings == null) {
         myBindings = myBindingsMap.get(allBindings.get(0).getName());
      }

      mySelector = new StringSelector (
         "Bindings", myBindings.getName(), allNames);
      mySelector.addValueChangeListener (this);

      myWheelZoom = new DoubleField ("Wheel zoom scale", zoomScale);

      addWidget (mySelector);

      addLabel (" Viewpoint control:");
      addMaskInfo (MouseAction.ROTATE_VIEW, myBindings);
      addMaskInfo (MouseAction.TRANSLATE_VIEW, myBindings);
      addMaskInfo (MouseAction.ZOOM_VIEW, myBindings);

      addLabel (" Component selection:");
      addMaskInfo (MouseAction.SELECT_COMPONENTS, myBindings);
      addMaskInfo (MouseAction.MULTIPLE_SELECTION, myBindings);
      addMaskInfo (MouseAction.ELLIPTIC_DESELECT, myBindings);
      addMaskInfo (MouseAction.RESIZE_ELLIPTIC_CURSOR, myBindings);
      addMaskInfo (MouseAction.CONTEXT_MENU, myBindings);

      addLabel (" Manipulator controls:");
      addMaskInfo (MouseAction.MOVE_DRAGGER, myBindings);
      addMaskInfo (MouseAction.DRAGGER_CONSTRAIN, myBindings);
      addMaskInfo (MouseAction.DRAGGER_REPOSITION, myBindings);
      
      addWidget (new JSeparator());
      addWidget (myWheelZoom);
   }

   public MouseBindings getBindings() {
      return myBindings;
   }

   public double getWheelZoom() {
      return myWheelZoom.getDoubleValue();
   }

   public DoubleField getWheelZoomField() {
      return myWheelZoom;
   }

   public StringSelector getBindingsField() {
      return mySelector;
   }

   public void valueChange (ValueChangeEvent evt) {
      Object source = evt.getSource();
      if (source == mySelector) {
         myBindings = myBindingsMap.get(mySelector.getValue());
         int k = 0;
         for (MouseAction action : MouseAction.values()) {
            setMaskInfoText (action, myMaskInfos.get(k++), myBindings);
         }
         repaint();
      }
   }

}
