/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import maspack.interpolation.NumericList;
import maspack.util.DoubleInterval;
import maspack.widgets.BooleanSelector;
import maspack.widgets.ButtonCreator;
import maspack.widgets.DoubleIntervalField;
import maspack.widgets.GuiUtils;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import artisynth.core.gui.timeline.GuiStorage;
import artisynth.core.probes.NumericProbeBase;
import artisynth.core.probes.Probe;

/**
 * @author Andrei this class is a driver for the large numeric probe display
 * 
 */
public class NumericProbeDisplayLarge extends JFrame // implements KeyListener
implements ActionListener, ValueChangeListener {
   private static final long serialVersionUID = 7694829010881232134L;

   private NumericProbePanel myPanel = null;

   // the probe object
   private NumericProbeBase myProbe = null;

   // Constants for the large probe display
   public static final boolean LARGE_PROBE_DISPLAY = true;
   public static final boolean SMALL_PROBE_DISPLAY = false;

   private JButton zoomInBtn, zoomOutBtn, moveDisplayBtn, fitRangeBtn,
      pointerBtn, upArrowBtn, downArrowBtn;
   private Border selectBorder =
      BorderFactory.createBevelBorder (BevelBorder.LOWERED);
   private Border normalBorder;
   private String myTrackNumber;

   private final ImageIcon zoomInIcon = loadIcon ("ZoomIn.gif");
   private final ImageIcon zoomOutIcon = loadIcon ("ZoomOut.gif");
   private final ImageIcon moveDisplayIcon = loadIcon ("Hand.gif");
   private final ImageIcon pointerIcon = loadIcon ("Arrow.gif");
   private final ImageIcon upArrowIcon = loadIcon ("UpFullArrow.gif");
   private final ImageIcon downArrowIcon = loadIcon ("DownFullArrow.gif");
   private final ImageIcon fitRangeIcon = loadIcon ("FitRange.gif");

   private DoubleIntervalField yRangeField;
   private DoubleIntervalField xRangeField;
   
   private double yMin, yMax, xMin, xMax;

   private BooleanSelector autoRangeCheck;
   
   private static ImageIcon loadIcon (String fileName) {
      return GuiUtils.loadIcon (
         GuiStorage.class, "BasicIcon/" + fileName);
   }

   public NumericProbePanel getPanel() {
      return myPanel;
   }

   public NumericProbeDisplayLarge (Probe probe, String trackNumber) {
      super();

      if (probe instanceof NumericProbeBase) {
         myProbe = (NumericProbeBase)probe;
         myTrackNumber = trackNumber;
      }
      else {
         throw new IllegalArgumentException ("probe not numeric");
      }
      initialize();
      setDefaultCloseOperation (DISPOSE_ON_CLOSE);
   }

   // Changed function by Andrei
   // Replaced 8 line function into 1 liner - optimization
   protected NumericList getNumericList() {
      return (myProbe != null) ? myProbe.getNumericList() : null;
   }

   private void initialize() {
      setSize (new Dimension (700, 400));
      String inputOutputString =
         (myProbe.isInput()) ? "Input probe" : "Output probe";
      setTitle (
         myTrackNumber + " " + inputOutputString + ": " + myProbe.getName());

      // create probe panels based on the fact if it is input or output probes
      myPanel = (NumericProbePanel) myProbe.getDisplay (
         getWidth(), getHeight(), LARGE_PROBE_DISPLAY);

      if (myPanel.isAutoRanging()) {
         myPanel.adjustRangeIfNecessary();
      }

      JToolBar sideToolBar = new JToolBar(JToolBar.VERTICAL);

      zoomInBtn = ButtonCreator.createIconicButton (
         zoomInIcon, "Zoom In", "Zoom In", true, false, this);
      zoomOutBtn = ButtonCreator.createIconicButton (
         zoomOutIcon, "Zoom Out", "Zoom Out", true, false, this);
      moveDisplayBtn = ButtonCreator.createIconicButton (
         moveDisplayIcon, "Move Display", "Move Display", true, false, this);
      pointerBtn = ButtonCreator.createIconicButton (
         pointerIcon, "Edit Plot", "Edit Plot", true, false, this);
      upArrowBtn = ButtonCreator.createIconicButton (
         upArrowIcon, "Increase Range", "Increase Range", true, false, this);
      downArrowBtn = ButtonCreator.createIconicButton (
         downArrowIcon, "Decrease Range", "Decrease Range", true, false, this);
      fitRangeBtn = ButtonCreator.createIconicButton (
         fitRangeIcon, "Fit Range", "Fit Range", true, false, this);
      
      autoRangeCheck = new BooleanSelector ("Auto range", true);
      autoRangeCheck.addValueChangeListener (this);
      
      setYRange (myPanel.getAutoRange ());      
      setXRange (myPanel.getDefaultDomain ());
      
      yRangeField = new DoubleIntervalField (
         "Y range", new DoubleInterval(yMin, yMax), "%.6g");
      yRangeField.addValueChangeListener (this);
      xRangeField = new DoubleIntervalField (
         "X range", new DoubleInterval(xMin, xMax), "%.6g");
      xRangeField.addValueChangeListener (this);

      sideToolBar.add (pointerBtn);
      sideToolBar.addSeparator();
      sideToolBar.add (zoomInBtn);
      sideToolBar.add (zoomOutBtn);
      sideToolBar.add (moveDisplayBtn);
      sideToolBar.addSeparator();
      sideToolBar.add (upArrowBtn);
      sideToolBar.add (downArrowBtn);
      sideToolBar.add (fitRangeBtn);
      sideToolBar.addSeparator();
      
      JToolBar rangeToolBar = new JToolBar();
      rangeToolBar.add (autoRangeCheck);
      rangeToolBar.addSeparator();
      rangeToolBar.add (yRangeField);
      rangeToolBar.addSeparator();
      rangeToolBar.add (xRangeField);
      rangeToolBar.addSeparator();
      
      normalBorder = zoomInBtn.getBorder();

      getContentPane().setLayout (new BorderLayout());
      getContentPane().add (myPanel, BorderLayout.CENTER);
      getContentPane().add (sideToolBar, BorderLayout.WEST);
      getContentPane().add (rangeToolBar, BorderLayout.NORTH);

      pack();
   }

   /**
    * Switch between the possible display actions of zooming in, zooming out and
    * moving the display around. Only one can be selected at any time, the three
    * items are zoomIn, zoomOut and moveDisplay.
    * 
    * @param displayAction
    */
   private void setDisplayAction (JButton displayItem) {
      JButton[] otherBtns = new JButton[3];

      if (displayItem == zoomInBtn) {
         otherBtns[0] = zoomOutBtn;
         otherBtns[1] = moveDisplayBtn;
         otherBtns[2] = pointerBtn;
      }
      else if (displayItem == zoomOutBtn) {
         otherBtns[0] = zoomInBtn;
         otherBtns[1] = moveDisplayBtn;
         otherBtns[2] = pointerBtn;
      }
      else if (displayItem == moveDisplayBtn) {
         otherBtns[0] = zoomInBtn;
         otherBtns[1] = zoomOutBtn;
         otherBtns[2] = pointerBtn;
      }
      else {
         otherBtns[0] = zoomInBtn;
         otherBtns[1] = zoomOutBtn;
         otherBtns[2] = moveDisplayBtn;
      }

      if (displayItem.isSelected() && displayItem != pointerBtn) {
         displayItem.setBorder (normalBorder);
         displayItem.setSelected (false);
      }
      else {
         if (displayItem != pointerBtn) {
            displayItem.setBorder (selectBorder);
            displayItem.setSelected (true);
         }
         
         for (int i = 0; i < otherBtns.length; i++) {
            otherBtns[i].setBorder (normalBorder);
            otherBtns[i].setSelected (false);
         }
      }
   }
   
   public void actionPerformed (ActionEvent e) {
      String nameOfAction = e.getActionCommand();
      
      yRangeField.removeValueChangeListener (this);
      xRangeField.removeValueChangeListener (this);

      if (nameOfAction == "Zoom In") {
         setDisplayAction (zoomInBtn);
         myPanel.toggleZoomIn();
      }
      else if (nameOfAction == "Zoom Out") {
         setDisplayAction (zoomOutBtn);
         myPanel.toggleZoomOut();
      }
      else if (nameOfAction == "Move Display") {
         setDisplayAction (moveDisplayBtn);
         myPanel.toggleMoveDisplay();
      }
      else if (nameOfAction == "Edit Plot") {
         myPanel.zoomIn = false;
         myPanel.zoomOut = false;
         myPanel.moveDisplay = false;
         setDisplayAction (pointerBtn);
      }
      else if (nameOfAction == "Fit Range") {
         myProbe.applyDefaultDisplayRanges();
      }
      else if (nameOfAction == "Increase Range") {
         myProbe.increaseDisplayRanges();
         myProbe.updateDisplays();
      }
      else if (nameOfAction == "Decrease Range") {
         myProbe.decreaseDisplayRanges();
         myProbe.updateDisplays();
      }
      
      setYRange (myPanel.getDisplayRange());
      setXRange (myPanel.getDisplayDomain());
      
      yRangeField.setValue (new DoubleInterval (yMin, yMax));                 
      xRangeField.setValue (new DoubleInterval (xMin, xMax));   
      
      yRangeField.addValueChangeListener (this);
      xRangeField.addValueChangeListener (this);
   }
   
   private void setYRange (double[] range) {
      yMin = range[0];
      yMax = range[1];
   }
   
   private void setXRange (double[] domain) {
      xMin = domain[0];
      xMax = domain[1];
   }

   public void valueChange (ValueChangeEvent e) {
      Object source = e.getSource();
      
      if (source == yRangeField) {
         double lower = yRangeField.getLowerBound();
         double upper = yRangeField.getUpperBound();
         
         if (lower != yMin && lower < upper) {
            yMin = lower;            
            myPanel.setDisplayRangeManually (lower, upper);
         }
         else if (upper != yMax && upper > lower) {
            yMax = upper;
            myPanel.setDisplayRangeManually (lower, upper);
         }
         
         yRangeField.setValue (new DoubleInterval (yMin, yMax));
         
         autoRangeCheck.setValue (false);
         myPanel.setAutoRanging (false);
      }
      else if (source == xRangeField) {
         double lower = xRangeField.getLowerBound();
         double upper = xRangeField.getUpperBound();
         
         if (lower != xMin && lower < upper) {
            xMin = lower;            
            myPanel.setDisplayDomain (lower, upper);
         }
         else if (upper != xMax && upper > lower) {
            xMax = upper;
            myPanel.setDisplayDomain (lower, upper);
         }
         
         xRangeField.setValue (new DoubleInterval (xMin, xMax));
         
         autoRangeCheck.setValue (false);
         myPanel.setAutoRanging (false);
      }
      else if (source == autoRangeCheck) {
         if (autoRangeCheck.getBooleanValue()) {
            yRangeField.removeValueChangeListener (this);
            xRangeField.removeValueChangeListener (this);
            
            myPanel.setAutoRanging (true);
            
            setYRange (myPanel.getAutoRange ());      
            setXRange (myPanel.getDefaultDomain ());
            
            yRangeField.setValue (new DoubleInterval (yMin, yMax));                 
            xRangeField.setValue (new DoubleInterval (xMin, xMax));
            
            myPanel.setDisplayRangeManually (yMin, yMax);
            myPanel.setDisplayDomain (xMin, xMax);
            
            yRangeField.addValueChangeListener (this);
            xRangeField.addValueChangeListener (this);
         }
      }
      
      myPanel.repaint();   
   }

   @Override
   public void dispose() {
      if (myProbe != null) {
         myProbe.removeDisplay (myPanel);
         myProbe = null;
      }
      super.dispose();
   }

   public void finalize() {
      dispose();
   }
}
