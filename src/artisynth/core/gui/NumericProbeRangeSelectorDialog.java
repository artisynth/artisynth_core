/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.BoxLayout;

import artisynth.core.gui.widgets.*;
import maspack.util.InternalErrorException;
import maspack.widgets.BooleanSelector;
import maspack.widgets.DoubleField;
import maspack.widgets.GuiUtils;
import maspack.widgets.LabeledComponentPanel;
import maspack.widgets.OptionPanel;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;

/**
 * @author Chad Modified by Andrei
 * @version Alpha code for Changing the display range of a probe is still under
 * development
 */

public class NumericProbeRangeSelectorDialog extends JDialog implements
ActionListener, ValueChangeListener {
   private static final long serialVersionUID = 1L;

   private NumericProbePanel display = null;
   private LabeledComponentPanel myPanel;
   private DoubleField maxYField, minYField;
   private BooleanSelector autoRangeField;
   private double minYRange, maxYRange;
   private boolean autoRanging;

   public NumericProbeRangeSelectorDialog (NumericProbePanel DisplayProbe) {
      super();
      super.setTitle ("Display Range");
      this.setAlwaysOnTop (true);
      commonProbeRangeSelectorInit (DisplayProbe);
   }

   private boolean maskAutoRangeDisable = false;

   public void valueChange (ValueChangeEvent e) {
      if (e.getSource() == maxYField || e.getSource() == minYField) {
         if (minYField.getDoubleValue() >= maxYField.getDoubleValue()) {
            
            minYField.removeValueChangeListener (this);
            maxYField.removeValueChangeListener (this);
            
            minYField.setValue (0);
            maxYField.setValue (0);
            
            minYField.addValueChangeListener (this);
            maxYField.addValueChangeListener (this);
            
            GuiUtils.showError (this, "Minimum Y must be lower than maximum Y");
         }
         else {
            display.setYRange (
               minYField.getDoubleValue(), maxYField.getDoubleValue());
            if (!maskAutoRangeDisable) {
               display.setAutoRanging (false);
            }
            display.repaint();
         }
      }
      else if (e.getSource() == autoRangeField) {
         boolean enabled = autoRangeField.getBooleanValue();
//         if (enabled) {
//            double[] range = display.getDefaultRange();
//            maskAutoRangeDisable = true;
//            minYField.setValue (range[0]);
//            maxYField.setValue (range[1]);
//            maskAutoRangeDisable = false;
//            display.repaint();
//         }
         display.setAutoRanging (enabled);
      }
      else {
         throw new InternalErrorException ("unknown value change source: "
         + e.getSource());
      }
   }

   private void commonProbeRangeSelectorInit (NumericProbePanel DisplayProbe) {
      display = DisplayProbe;
      super.setModal (true);
      minYRange = display.getYRange().getLowerBound();
      maxYRange = display.getYRange().getUpperBound();
      autoRanging = display.isAutoRanging();

      getContentPane().setLayout (
         new BoxLayout (getContentPane(), BoxLayout.Y_AXIS));

      myPanel = new LabeledComponentPanel();
      myPanel.setAlignmentX (Component.CENTER_ALIGNMENT);
      getContentPane().add (myPanel);
      generateWidgets (myPanel);

      JSeparator sep = new JSeparator();
      sep.setAlignmentX (Component.CENTER_ALIGNMENT);
      getContentPane().add (sep);

      OptionPanel options = new OptionPanel ("OK", this);
      options.setAlignmentX (Component.CENTER_ALIGNMENT);
      getContentPane().add (options);

      pack();
      Point offset = display.getLocationOnScreen();
      this.setLocation (offset.x, offset.y);
      // this.setSize(new Dimension(350, 125));
      // this.setMinimumSize(new Dimension(350, 125));
      setVisible (true);
   }

   public void generateWidgets (LabeledComponentPanel panel) {
      maxYField = new DoubleField ("Display Maximum (y) :", maxYRange, "%8.3f");
      maxYField.addValueChangeListener (this);
      panel.addWidget (maxYField);

      minYField = new DoubleField ("Display Minimum (y) :", minYRange, "%8.3f");
      minYField.addValueChangeListener (this);
      panel.addWidget (minYField);

      autoRangeField = new BooleanSelector ("Auto range: ", autoRanging);
      autoRangeField.addValueChangeListener (this);
      panel.addWidget (autoRangeField);
   }

   public void actionPerformed (ActionEvent e) {
      String nameOfAction = e.getActionCommand();
      if (nameOfAction.equals ("OK")) {
//         display.setDefaultRange (minYField.getDoubleValue(),
//                                  maxYField.getDoubleValue());
         // display.setDisplayRange(
         // minYField.getDoubleValue(), maxYField.getDoubleValue());

         // set new display range in probe
         display.myProbe.setDefaultDisplayRange (
            minYField.getDoubleValue(), maxYField.getDoubleValue());
         display.repaint();
         setVisible (false);
      }
      // else if (nameOfAction.equals ("Cancel")) {
      //    display.setDisplayRange (minYRange, maxYRange);
      //    // set auto-ranging last because setDisplayRange will clear it
      //    display.setAutoRanging (autoRanging);
      //    display.repaint();
      //    setVisible (false);
      // }
      else {
         throw new InternalErrorException ("Unknown action: " + nameOfAction);
      }
   }
} // end of DisplayRangeSelectorDialog
