/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;

import artisynth.core.gui.widgets.*;
import artisynth.core.probes.*;
import maspack.util.*;
import maspack.widgets.BooleanSelector;
import maspack.widgets.ButtonMasks;
import maspack.widgets.ColorSelector;
import maspack.widgets.GuiUtils;
import maspack.widgets.LabelSpacing;
import maspack.widgets.LabeledComponent;
import maspack.widgets.PropertyFrame;
import maspack.widgets.PropertyPanel;
import maspack.widgets.StringField;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;

public class LegendDisplay extends PropertyFrame {
   
   private PlotTraceManager myTraceManager;
   
   private boolean myLabelsEditable = false;
   private NumericProbePanel myDataPanel;
   
   private LabeledComponent myCaption;
   private JLabel myColorLabel;
   private JLabel myVisibleLabel;

   /**
    * Create a LegendDisplay for data associated with a panel
    * showing numeric probe data.
    * 
    * @param panel panel displaying the data
    * @param traceManager manager for the data plot traces
    */  
   public LegendDisplay (
      NumericProbePanel panel, PlotTraceManager traceManager) {
      super ("Display legend");
      myDataPanel = panel;
      myTraceManager = traceManager;
      build();
   }
   
   protected void build() {
      initialize();
      addOptionPanel ("Close");
      setPanel (new Panel());

      myCaption = createCaptionWidget();

      buildWidgets();

      myOptionPanel.addMouseListener (new MouseHandler());
      //enableAutoRerendering (true);
      setDefaultCloseOperation (DISPOSE_ON_CLOSE);
      addComponentListener (new ResizeHandler());
      pack();     
   }

   void updateDisplaysWithoutAutoRanging() {
      myDataPanel.updateDisplaysWithoutAutoRanging();
   }
   
   LabeledComponent createCaptionWidget() {
      LabeledComponent caption = new LabeledComponent (" ");
      myColorLabel = new JLabel ("color");
      Dimension size = myColorLabel.getPreferredSize();
      size.width = 2*ColorSelector.getButtonSize().width;
      GuiUtils.setFixedSize (myColorLabel, size);
      caption.addMajorComponent (myColorLabel);
      myVisibleLabel = new JLabel ("visible");
      GuiUtils.setFixedSize (
         myVisibleLabel, myVisibleLabel.getPreferredSize());
      caption.addMajorComponent (myVisibleLabel);
      return caption;
   }

   private void buildWidgets() {
      addWidget (myCaption);
      for (int order=0; order<myTraceManager.numTraces(); order++) {
         int index = myTraceManager.getOrderedTraceIndex(order);
         PlotTraceInfo pti = myTraceManager.getTraceInfo (index);
         Widget widget = new Widget (pti, index);
         if (myLabelsEditable) {
            widget.setLabelEditable (true);
         }
         addWidget (widget);
      }
   }

   public void rebuild() {
      myPanel.removeAllWidgets();
      buildWidgets();
      pack();
   }

   public void dispose() {
      super.dispose();
      if (myDataPanel != null) {
         myDataPanel.removeLegend();
      }
   }

   private class ResizeHandler extends ComponentAdapter {
      public void componentResized (ComponentEvent e) {
         // resize captionLabel manually if labels are editable
         Widget widget = ((Panel)myPanel).getFirstWidget();
         if (widget != null && myLabelsEditable) {
            LabelSpacing spc = new LabelSpacing();
            myCaption.getLabelSpacing (spc);
            spc.labelWidth +=
               (widget.getColorSwatch().getX() - myColorLabel.getX());
            myCaption.setLabelSpacing (spc);
//             myCaption.setLabelWidth (
//                myCaption.getLabelWidth() +
//                (widget.getColorSwatch().getX() - myColorLabel.getX()));
         }
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
      
      if (myLabelsEditable) {
         popup.add (createPopupItem ("Fix labels", this));
      }
      else {
         popup.add (createPopupItem ("Edit labels", this));
      }
      popup.add (createPopupItem ("Set all visible", this));

      return popup;
   }  

   class Panel extends PropertyPanel {
      protected void moveWidgets (JComponent comp) {
         super.moveWidgets (comp);
         Component[] comps = getComponents();
         int numTraces = myTraceManager.numTraces();
         int[] indices = new int[numTraces];
         int order = 0;
         for (int i=0; i<comps.length; i++) {
            if (comps[i] instanceof Widget) {
               int idx = ((Widget)comps[i]).getPlotTraceIndex();
               if (order >= indices.length) {
                  throw new InternalErrorException (
                     "Number of widgets exceeds number of traces");
               }
               indices[order++] = idx;
            }         
         }
         if (order != numTraces) {
            throw new InternalErrorException (
               "Number of widgets = "+order+", number of traces="+numTraces);
         }
         myTraceManager.setTraceOrder (indices);
         updateDisplaysWithoutAutoRanging();
      }

      public Widget getFirstWidget() {
         for (int i=0; i<getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof Widget){
               return (Widget)c;
            }
         }
         return null;
      }
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd == "Fix labels") {
         myLabelsEditable = false;
         Component[] comps = myPanel.getComponents();
         for (int i=0; i<comps.length; i++) {
            if (comps[i] instanceof Widget) {
               ((Widget)comps[i]).setLabelEditable (false);
            }
         }
         pack();
      }
      else if (cmd == "Edit labels") {
         myLabelsEditable = true;
         Component[] comps = myPanel.getComponents();
         for (int i=0; i<comps.length; i++) {
            if (comps[i] instanceof Widget) {
               ((Widget)comps[i]).setLabelEditable (true);
            }
         }
         pack();
      }
      else if (cmd == "Set all visible") {
         Component[] comps = myPanel.getComponents();
         for (int i=0; i<comps.length; i++) {
            if (comps[i] instanceof Widget) {
               ((Widget)comps[i]).setTraceVisible (true);
            }
         }
      }
      else {
         super.actionPerformed (e);
      }
   }
   
   class Widget extends ColorSelector implements ValueChangeListener {

      private BooleanSelector myVisibilitySelector;
      private int myPlotTraceIdx;
      private StringField myLabelField;
      private boolean myLabelIsEditable;

      Widget (PlotTraceInfo pti, int idx) {
         super (myTraceManager.getTraceLabel(idx), 
                myTraceManager.getTraceColor(idx));
         myVisibilitySelector =
            new BooleanSelector ("", myTraceManager.isTraceVisible(idx));
         addMajorComponent (myVisibilitySelector);
         addValueChangeListener (this);
         myVisibilitySelector.addValueChangeListener (this);
         GuiUtils.setFixedSize (
            myVisibilitySelector, myVisibleLabel.getPreferredSize());
         myPlotTraceIdx = idx;
      }

      public void valueChange (ValueChangeEvent e) {
         if (e.getSource() == this) {
            myTraceManager.setTraceColor (myPlotTraceIdx, getColor());
            updateDisplaysWithoutAutoRanging();
         }
         else if (e.getSource() == myVisibilitySelector) {
            myTraceManager.setTraceVisible (
               myPlotTraceIdx, myVisibilitySelector.getBooleanValue());
            updateDisplaysWithoutAutoRanging();
         }
         else if (e.getSource() == myLabelField) {
            myTraceManager.setTraceLabel (
               myPlotTraceIdx, myLabelField.getStringValue());
            updateDisplaysWithoutAutoRanging();
         }
         else {
            throw new InternalErrorException (
               "Unknown value change event from " + e.getSource());
         }
      }

      public int getPlotTraceIndex() {
         return myPlotTraceIdx;
      }

      void setTraceVisible (boolean visible) {
         if (visible != myVisibilitySelector.getBooleanValue()) {
            myVisibilitySelector.setValue (visible);
         }
      }

      public void setLabelEditable (boolean enable) {
         if (enable != myLabelIsEditable) {
            if (enable) {
               myLabelField = new StringField (
                  "", myTraceManager.getTraceLabel(myPlotTraceIdx), 20);
               myLabelField.setStretchable (true);
               setStretchable (true);
               myLabelField.addValueChangeListener (this);
               addMajorComponent (myLabelField, 1);
               setLabelText ("");
            }
            else {
               myLabelField.dispose();
               setStretchable (false);
               removeMajorComponent (myLabelField);
               setLabelText (myTraceManager.getTraceLabel(myPlotTraceIdx));
            }
            myLabelIsEditable = enable;
         }
      }

      public boolean isLabelEditable() {
         return myLabelIsEditable;
      }

      public void dispose() {
         if (myVisibilitySelector != null) {
            myVisibilitySelector.dispose();
            myVisibilitySelector = null;
         }            
         if (myLabelField != null) {
            myLabelField.dispose();
            myLabelField = null;
         }
         super.dispose();
      }
   }
}
