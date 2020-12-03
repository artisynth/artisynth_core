/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import maspack.widgets.BooleanSelector;
import maspack.widgets.ButtonCreator;
import maspack.widgets.GuiUtils;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.matrix.Point2d;
import artisynth.core.gui.timeline.GuiStorage;
import artisynth.core.probes.NumericProbeBase;
import artisynth.core.probes.Probe;
import artisynth.core.gui.NumericProbePanel.CursorMode;
import artisynth.core.modelbase.PropertyChangeListener;
import artisynth.core.modelbase.PropertyChangeEvent;

/**
 * @author Andrei this class is a driver for the large numeric probe display
 * 
 */
public class NumericProbeDisplayLarge extends JFrame // implements KeyListener
   implements ActionListener, ValueChangeListener, PropertyChangeListener {
   private static final long serialVersionUID = 7694829010881232134L;

   private NumericProbePanel myPanel = null;

   // the probe object
   private NumericProbeBase myProbe = null;

   // Constants for the large probe display
   public static final boolean LARGE_PROBE_DISPLAY = true;
   public static final boolean SMALL_PROBE_DISPLAY = false;

   private JButton zoomInBtn, zoomOutBtn, translateBtn, fitRangeBtn, gridBtn,
      autoRangeBtn,
      selectBtn, upArrowBtn, downArrowBtn, leftArrowBtn, rightArrowBtn;
   private Border selectBorder =
      BorderFactory.createBevelBorder (BevelBorder.LOWERED);
   private Border normalBorder;
   private String myTrackNumber;

   private final ImageIcon zoomInIcon = loadIcon ("ZoomIn.gif");
   private final ImageIcon zoomOutIcon = loadIcon ("ZoomOut.gif");
   private final ImageIcon moveDisplayIcon = loadIcon ("Hand.gif");
   private final ImageIcon selectIcon = loadIcon ("Arrow.gif");
   private final ImageIcon upArrowIcon = loadIcon ("UpArrow.png");
   private final ImageIcon downArrowIcon = loadIcon ("DownArrow.png");
   private final ImageIcon leftArrowIcon = loadIcon ("LeftArrow.png");
   private final ImageIcon rightArrowIcon = loadIcon ("RightArrow.png");
   //private final ImageIcon fitRangeIcon = loadIcon ("FitRange.gif");
   private final ImageIcon fitRangeIcon = loadIcon ("FitRanges.png");
   private final ImageIcon gridIcon = loadIcon ("GridIcon.gif");
   private final ImageIcon autoRangeIcon = loadIcon ("AutoRefresh.png");

   private CursorPositionPanel myCursorPanel;

   private class CursorPositionPanel extends JPanel {
      
      private static final int fontHeight = 10;
      private Font myFont;
      private String myPosStr = null;
      
      public void updatePosition (Point2d pos) {
         NumberFormat fmt = new NumberFormat ("%.4g");
         myPosStr = fmt.format(pos.x) + ", " + fmt.format(pos.y);
         repaint();
      }

      public void clearPosition() {
         myPosStr = null;
         repaint();
      }

      public CursorPositionPanel() {
         super ();
         setPreferredSize (new Dimension (120, 16));
         setMaximumSize (new Dimension (120, 16));
         myFont = new Font (null, 0, 10);
      }
      
      public void paintComponent (Graphics g) {
         g.setColor (getBackground());
         
         // set antialiasing
         RenderingHints rh = 
            new RenderingHints(
               RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_ON);
         ((Graphics2D)g).setRenderingHints(rh);

         g.fillRect (0, 0, getWidth(), getHeight());
         if (myPosStr != null) {
            g.setFont (myFont);
            g.setColor (getForeground());
            Rectangle2D bounds =
               g.getFontMetrics().getStringBounds (myPosStr, g);
            //g.fillRect(0, 0, getWidth(), getHeight());  
            int xpos = Math.max (0, getWidth()-((int)bounds.getWidth()+2));
            int ypos = ((int)bounds.getHeight()+getHeight())/2;
            g.drawString (myPosStr, xpos, ypos);
         }
      }
   }

   private class CursorPositionListener
      extends MouseAdapter implements MouseWheelListener {

      public void mouseMoved (MouseEvent e) {
         myCursorPanel.updatePosition (myPanel.pixelsToCoords (e.getPoint()));
      }

      public void mouseReleased (MouseEvent e) {
         myCursorPanel.updatePosition (myPanel.pixelsToCoords (e.getPoint()));
      }

      public void mouseClicked (MouseEvent e) {
         myCursorPanel.updatePosition (myPanel.pixelsToCoords (e.getPoint()));
      }

      public void mouseExited (MouseEvent e) {
         myCursorPanel.clearPosition();
      }

      public void mouseWheelMoved (MouseWheelEvent e) {
         myCursorPanel.updatePosition (myPanel.pixelsToCoords (e.getPoint()));
      }

   }

   //private BooleanSelector autoRangeCheck;
   
   private static ImageIcon loadIcon (String fileName) {
      return GuiUtils.loadIcon (
         GuiStorage.class, "BasicIcon/" + fileName);
   }

   public NumericProbePanel getPanel() {
      return myPanel;
   }

   public NumericProbeDisplayLarge (NumericProbeDisplayLarge display) {
      myProbe = null;
      setSize (display.getSize());
      String origTitle = new String(display.getTitle());
      setTitle (origTitle.replace ("Display", "Cloned display"));

      // create probe panels based on the fact if it is input or output probes
      myPanel = new NumericProbePanel (display.getPanel());
      myPanel.addPropertyChangeListener (this);
      CursorPositionListener l = new CursorPositionListener();
      myPanel.addMouseMotionListener (l);
      myPanel.addMouseListener (l);
      myPanel.addMouseWheelListener (l);
            
      initialize();
      setDefaultCloseOperation (DISPOSE_ON_CLOSE);     
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
      setSize (new Dimension (700, 400));
      String titleString =
         "Display of " + 
         ((myProbe.isInput()) ? "input probe " : "output probe ");
      if (myProbe.getName() != null) {
         titleString += "\"" + myProbe.getName() + "\"";
      }
      else {
         titleString += "\"" + myProbe.getNumber() + "\"";        
      }
      setTitle (titleString);

      // create probe panels based on the fact if it is input or output probes
      myPanel = (NumericProbePanel) myProbe.getDisplay (
         getWidth(), getHeight(), LARGE_PROBE_DISPLAY);
      myPanel.addPropertyChangeListener (this);
      CursorPositionListener l = new CursorPositionListener();
      myPanel.addMouseMotionListener (l);
      myPanel.addMouseListener (l);
      myPanel.addMouseWheelListener (l);

      initialize();
      setDefaultCloseOperation (DISPOSE_ON_CLOSE);
   }

   private void initialize() {
      if (myPanel.isAutoRanging()) {
         myPanel.adjustRangeIfNecessary();
      }

      //JToolBar sideToolBar = new JToolBar(JToolBar.VERTICAL);

      zoomInBtn = ButtonCreator.createIconicButton (
         zoomInIcon, "Zoom in", "Zoom-in mode", true, false, this);
      zoomOutBtn = ButtonCreator.createIconicButton (
         zoomOutIcon, "Zoom out", "Zoom-out mode", true, false, this);
      translateBtn = ButtonCreator.createIconicButton (
         moveDisplayIcon, "Translate",
         "Translate mode (when zoomed-in)", true, false, this);
      selectBtn = ButtonCreator.createIconicButton (
         selectIcon, "Select knots", "Select mode (when knots visible)",
         true, false, this);
      upArrowBtn = ButtonCreator.createIconicButton (
         upArrowIcon, "Increase y range", "Increase y range", true, false, this);
      downArrowBtn = ButtonCreator.createIconicButton (
         downArrowIcon, "Decrease y range", "Decrease y range", true,false, this);
      leftArrowBtn = ButtonCreator.createIconicButton (
         leftArrowIcon, "Decrease x range", "Decrease x range", true,false, this);
      rightArrowBtn = ButtonCreator.createIconicButton (
         rightArrowIcon, "Increase x range", "Increase x range", true,false,this);
      fitRangeBtn = ButtonCreator.createIconicButton (
         fitRangeIcon, "Reset ranges", "Fit ranges to data",
         true, false, this);
      gridBtn = ButtonCreator.createIconicButton (
         gridIcon, "Show grid", "Show grid", true, false, this);
      autoRangeBtn = ButtonCreator.createIconicButton (
         autoRangeIcon,
         "Enable automatic ranging",
         "Enable automatic ranging", true, false, this);
      
      myCursorPanel = new CursorPositionPanel();

      // autoRangeCheck = new BooleanSelector ("Auto range", true);
      // autoRangeCheck.setToolTipText ("Enable/disable auto ranging");
      // autoRangeCheck.addValueChangeListener (this);
      
      JToolBar rangeToolBar = new JToolBar();

      rangeToolBar.add (selectBtn);
      rangeToolBar.add (zoomInBtn);
      rangeToolBar.add (zoomOutBtn);
      rangeToolBar.add (translateBtn);
      rangeToolBar.addSeparator();
      rangeToolBar.add (upArrowBtn);
      rangeToolBar.add (downArrowBtn);
      rangeToolBar.add (leftArrowBtn);
      rangeToolBar.add (rightArrowBtn);
      rangeToolBar.add (fitRangeBtn);
      rangeToolBar.addSeparator();
      rangeToolBar.add (gridBtn);
      rangeToolBar.add (autoRangeBtn);
      rangeToolBar.add (Box.createHorizontalGlue());
      rangeToolBar.add (myCursorPanel);
      rangeToolBar.addSeparator();

      normalBorder = zoomInBtn.getBorder();

      getContentPane().setLayout (new BorderLayout());
      getContentPane().add (myPanel, BorderLayout.CENTER);
      //getContentPane().add (sideToolBar, BorderLayout.WEST);
      getContentPane().add (rangeToolBar, BorderLayout.NORTH);
      setCursorMode (CursorMode.SELECT);

      // make sure button settings are consistent with current state
      updateWidgets();
      
      pack();
      System.out.println ("size=" + myCursorPanel.getSize());
   }

   private JButton getCursorButton (CursorMode mode) {
      switch (mode) {
         case SELECT: return selectBtn;
         case ZOOM_IN: return zoomInBtn;
         case ZOOM_OUT: return zoomOutBtn;
         case TRANSLATE: return translateBtn;
         default: {
            throw new InternalErrorException (
               "Button not defined for CursorMode " + mode);
         }
      }
   }

   private void setButtonSelected (JButton button, boolean selected) {
      if (!selected) {
         button.setBorder (normalBorder);
         button.setSelected (false);
      }
      else {
         button.setBorder (selectBorder);
         button.setSelected (true);        
      }
   }

   /**
    * Switch between the possible display actions of zooming in, zooming out and
    * moving the display around. Only one can be selected at any time, the three
    * items are zoomIn, zoomOut and moveDisplay.
    * 
    * @param displayAction
    */
   private void setCursorMode (CursorMode mode) {

      CursorMode oldMode = myPanel.getCursorMode();
      setButtonSelected (getCursorButton(oldMode), false);
      setButtonSelected (getCursorButton(mode), true);

      myPanel.setCursorMode (mode);
   }
   
   public void actionPerformed (ActionEvent e) {
      String nameOfAction = e.getActionCommand();

      if (nameOfAction == "Zoom in") {
         setCursorMode (CursorMode.ZOOM_IN);
      }
      else if (nameOfAction == "Zoom out") {
         setCursorMode (CursorMode.ZOOM_OUT);
      }
      else if (nameOfAction == "Translate") {
         setCursorMode (CursorMode.TRANSLATE);
      }
      else if (nameOfAction == "Select knots") {
         setCursorMode (CursorMode.SELECT);
      }
      else if (nameOfAction == "Reset ranges") {
         myPanel.resetDisplay();
      }
      else if (nameOfAction == "Show grid") {
         myPanel.setDrawGrid (true);
         updateWidgets();
      }
      else if (nameOfAction == "Hide grid") {
         myPanel.setDrawGrid (false);
         updateWidgets();
      }
      else if (nameOfAction == "Enable automatic ranging") {
         // toggle automatic ranging
         myPanel.setAutoRanging (true);
         updateWidgets();
      }
      else if (nameOfAction == "Disable automatic ranging") {
         myPanel.setAutoRanging (false);
         updateWidgets();
      }
      else if (nameOfAction == "Increase y range") {
         myPanel.increaseYRange();
      }
      else if (nameOfAction == "Decrease y range") {
         myPanel.decreaseYRange();
      }
      else if (nameOfAction == "Increase x range") {
         myPanel.increaseXRange();
      }
      else if (nameOfAction == "Decrease x range") {
         myPanel.decreaseXRange();
      }
   }

   public void propertyChanged (PropertyChangeEvent e) {
      updateWidgets();
   }

   protected void updateWidgets() {
      // autoRangeCheck.maskValueChangeListeners (true);
      // autoRangeCheck.setValue (myPanel.isAutoRanging());
      // autoRangeCheck.maskValueChangeListeners (false);
      if (myPanel.isAutoRanging()) {
         autoRangeBtn.setActionCommand ("Disable automatic ranging");
         autoRangeBtn.setToolTipText ("Disable automatic ranging");
         setButtonSelected (autoRangeBtn, true);
      }
      else {
         autoRangeBtn.setActionCommand ("Enable automatic ranging");
         autoRangeBtn.setToolTipText ("Enable automatic ranging");
         setButtonSelected (autoRangeBtn, false);
      }

      if (myPanel.getDrawGrid()) {
         gridBtn.setActionCommand ("Hide grid");
         gridBtn.setToolTipText ("Hide grid");
         setButtonSelected (gridBtn, true);
      }
      else {
         gridBtn.setActionCommand ("Show grid");
         gridBtn.setToolTipText ("Show grid");
         setButtonSelected (gridBtn, false);
      }
   }

   public void valueChange (ValueChangeEvent e) {
      Object source = e.getSource();
      // if (source == autoRangeCheck) {
      //    myPanel.setAutoRanging (autoRangeCheck.getBooleanValue());
      // }
      
      myPanel.repaint();   
   }

   @Override
   public void dispose() {
      if (myProbe != null) {
         myProbe.removeDisplay (myPanel);
         myProbe = null;
      }
      if (myPanel != null) {
         myPanel.dispose();
      }
      super.dispose();
   }

   public void finalize() {
      dispose();
   }
}
