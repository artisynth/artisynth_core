/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.*;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import javax.imageio.ImageIO;

import maspack.interpolation.Interpolation;
import maspack.interpolation.Interpolation.Order;
import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.matrix.VectorNd;
import maspack.matrix.Point2d;
import maspack.widgets.*;
import maspack.util.*;
import maspack.properties.*;
import artisynth.core.driver.Main;
import artisynth.core.probes.NumericProbeBase;
import artisynth.core.probes.PlotTraceInfo;
import artisynth.core.probes.PlotTraceManager;
import artisynth.core.probes.Probe;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.ExtensionFileFilter;
import artisynth.core.gui.widgets.ImageFileChooser;
import artisynth.core.gui.DataRenderer.TextAlign;
import artisynth.core.modelbase.PropertyChangeListener;
import artisynth.core.modelbase.PropertyChangeEvent;

/**
 * @author Chad Added to display a thin line for outputprobes, and not to
 * display datapoints per John's request
 * 
 * @version 1.0 revised March 06/2006
 * @version 2.0 reimplemented May 10/2006
 */

public class NumericProbePanel extends JPanel
   implements HasProperties, ValueChangeListener {
   // private NumericProbeDisplayLarge largeDisplayToUpdate = null;

   // indices into the margins array:
   private static final int LEFT = 0;
   private static final int RIGHT = 1;
   private static final int TOP = 2;
   private static final int BOTTOM = 3;

   private static final double EPS = 1e-15;

   float[] myMargins = new float[4];

   public static Color DARK_GREEN = new Color (0f, 0.5f, 0f);
   public static Color DarkOliveGreen = new Color (85, 107, 47);
   public static Color IndianRed = new Color (205, 92, 92);
   public static Color DarkOrange = new Color (255, 140, 0);
   public static Color BlueViolet = new Color (138, 43, 226);
   public static Color NavajoWhite = new Color (255, 222, 173);
   public static Color PeachPuff = new Color (255, 218, 185);

   boolean myLargeDisplay = false;
   // Virtual time means that the time scale is in probe virtual coordinates.
   // Otherwise, it is in timeline coordinates.
   protected boolean useVirtualTime = true;

   // properties

   protected static final boolean DEFAULT_AUTO_RANGING = true;
   protected boolean myAutoRangingP = DEFAULT_AUTO_RANGING;

   protected static final boolean DEFAULT_DRAW_GRID = false;
   protected boolean myDrawGrid = DEFAULT_DRAW_GRID;

   protected static final int DEFAULT_TICK_LENGTH = 4;
   protected int myTickLength = DEFAULT_TICK_LENGTH;

   protected static final int DEFAULT_LINE_WIDTH = 1;
   protected int myLineWidth = DEFAULT_LINE_WIDTH;

   protected static final int DEFAULT_KNOT_SIZE = 5;
   protected int myKnotSize = DEFAULT_KNOT_SIZE;

   protected static final boolean DEFAULT_KNOTS_VISIBLE = false;
   protected boolean myKnotsVisible = DEFAULT_KNOTS_VISIBLE;

   protected static DoubleInterval DEFAULT_X_RANGE = new DoubleInterval(0,1);
   protected DoubleInterval myXRange = new DoubleInterval(DEFAULT_X_RANGE);

   protected static DoubleInterval DEFAULT_Y_RANGE = new DoubleInterval(-1,1);
   protected DoubleInterval myYRange = new DoubleInterval(DEFAULT_Y_RANGE);
   // if true, do not disable auto ranging when setting YRange
   protected boolean myMaskAutoRangingDisable = false;

   // colors used for plotting
   public static Color[] colorList =
      { Color.RED, DARK_GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA,
       DarkOrange, Color.PINK, BlueViolet, NavajoWhite, Color.GRAY,
       DarkOliveGreen, IndianRed, DarkOrange };

   protected enum CursorMode {
      SELECT,
      ZOOM_IN,
      ZOOM_OUT,
      TRANSLATE
   };

   NumericProbeBase myProbe = null;  // probe associated with panel data
   
   // data and associated parameters for when there is no probe: 
   NumericList myNumericList;
   PlotTraceManager myTraceManager;
   double myScale = 1.0;
   double myStartTime = 0.0;
   double myStopTime = 1.0;
   boolean myInputDataP = true;
   
   //ProbeInfo myProbeInfo = null;

   // cache this value to see if we need to redraw when interpolation changes
   Interpolation.Order myLastInterpolationOrder = null;

   // ===========================================================
   // swing components and handlers
   // ===========================================================

   protected DisplayListener displayListener;
   protected WheelListener wheelListener;

   protected PropertyDialog myPropertyDialog = null;
   protected LegendDisplay myLegendDisplay = null;
   protected File myExportFile = null;

   // ===========================================================
   // variables needed for editing
   // ===========================================================

   protected NumericListKnot coincidentKnot;
   protected Point initialMousePos;
   protected boolean editingP;
   protected NumericListKnot oldKnot;
   protected NumericListKnot newKnot;
   protected int dirtyIndex;

   // ===========================================================
   // variables needed for plotting
   // ===========================================================

   protected static final int SELECT_MARGIN = 3;
   protected static final int DRAG_MARGIN = 3;
   protected static final long serialVersionUID = 0xdeadbeef;

   // ===========================================================
   // used for zooming and moving the display, only one can be true at a time
   // ===========================================================
   
   private Cursor zoomInCursor, zoomOutCursor, translateCursor;
   private Point zoomPnt0; // initial point of zoom selection
   private Point zoomPnt1; // second point of zoom selection
   protected CursorMode myCursorMode = CursorMode.SELECT;
   private Point previousPoint = null; // the previous point that was dragged to

   private ArrayList<PropertyChangeListener> myPropListeners = new ArrayList<>();

   private PlotLabelFormat myLabelFormat = new PlotLabelFormat();

   public ScreenTransform createScreenTransform() {
      return new ScreenTransform (
         myXRange, getPlotWidth(), myMargins[LEFT],
         myYRange, getPlotHeight(), myMargins[TOP],
         /*scale=*/!useVirtualTime ? getScale() : 1.0);
   }

   /**
    * Describes the value and spacing between plot ticks. The value
    * is expressed 
    * <pre>
    *   value  = m * 10^e
    * </pre>
    * where {@code m} and {@code e} are both integers.
    */
   protected static class TickValue {
      long myM;
      int myE;
      double myValue;

      public TickValue (double value, long m, int e) {
         myValue = value;
         myM = m;
         myE = e;
      }

      public TickValue (TickValue tval) {
         myValue = tval.myValue;
         myM = tval.myM;
         myE = tval.myE;
      }

      public double getValue() {
         return myValue;
      }

      public long getM() {
         return myM;
      }

      public int getE() {
         return myE;
      }

      public void add (TickValue spacing) {
         if (spacing.getE() != myE) {
            throw new IllegalArgumentException (
               "spacing has incompatible exponent "+spacing.getE()+
               ", expected " + myE);
         }
         myM += spacing.getM();
         myValue += spacing.getValue();
      }

   }

   /**
    * Maps x and y values to and from pixel coordinates.
    */
   public static class ScreenTransform {

      double xvelPerPixel;
      double yvalPerPixel;
      double pixelsPerXval;
      double pixelsPerYval;
      public double minXval;
      double maxYval;
      public float leftMargin;
      float topMargin;

      ScreenTransform (
         DoubleInterval xrange, double plotWidth, float left,
         DoubleInterval yrange, double plotHeight, float top, double scale) {

         xvelPerPixel = xrange.getRange()/(plotWidth-1)/scale;
         minXval = xrange.getLowerBound();
         pixelsPerXval = 1/xvelPerPixel;
         leftMargin = left;
         yvalPerPixel = yrange.getRange()/(plotHeight-1);
         maxYval = yrange.getUpperBound();
         pixelsPerYval = 1/yvalPerPixel;        
         topMargin = top;
      }

      public double yvaluePerPixel() {
         return yvalPerPixel;
      }

      public double xvaluePerPixel() {
         return xvelPerPixel;
      }

      public double pixelToXvalue (int xp) {
         return xvelPerPixel*(xp-leftMargin) + minXval;
      }

      public float xvalueToPixel (double x) {
         return Math.round((x-minXval)*pixelsPerXval) + leftMargin;
      }

      public double pixelToYvalue (int yp) {
         return yvalPerPixel*(topMargin-yp) + maxYval;
      }

      public float yvalueToPixel (double y) {
         double yp = Math.round((maxYval-y)*pixelsPerYval) + topMargin;
         if (yp > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
         }
         else if (yp < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
         }
         else {
            return (int)yp;
         }
      }
   }

   private static class PanelRenderer implements DataRenderer {

      Graphics2D myG;
      boolean mySolid = false;
      Shape mySavedClip = null;

      PanelRenderer (Graphics2D g) {
         myG = g;
      }

      public void beginPlot (float width, float height, boolean antialiasing) {
         RenderingHints rh;
         if (antialiasing) {
            rh = new RenderingHints(
               RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_ON);
         }
         else {
            rh = new RenderingHints(
               RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_OFF);
         }
         myG.setRenderingHints(rh);
      }
      
      public void endPlot () {
      }
      
      public void beginLineGraphics (float lineWidth, Color color) {
         myG.setStroke (new BasicStroke ((int)lineWidth));
         myG.setColor (color);
         mySolid = false;
      }

      public void beginSolidGraphics (Color color) {
         myG.setColor (color);
         mySolid = true;
      }

      public void beginTextGraphics (float fontHeight, Color color) {
         myG.setColor (color);
         myG.setFont (new Font (null, 0, (int)fontHeight));      
         mySolid = false;
      }

      public void drawLine (float x1, float y1, float x2, float y2) {
         myG.drawLine ((int)x1, (int)y1, (int)x2, (int)y2);
      }

      public void drawRect (float x, float y, float w, float h) {
         if (mySolid) {
            myG.fillRect ((int)x, (int)y, (int)w, (int)h);
         }
         else {
            myG.drawRect ((int)x, (int)y, (int)w, (int)h);
         }
      }

      public void drawCircle (float cx, float cy, float size) {
         int r = (int)Math.floor(size/2f);
         if (mySolid) {
            myG.fillOval ((int)cx-r, (int)cy-r, (int)size, (int)size);
         }
         else {
            myG.drawOval ((int)cx-r, (int)cy-r, (int)size, (int)size);
         }
      }

      public void drawText (String text, float x, float y, TextAlign alignment) {
         Rectangle2D rect = myG.getFontMetrics().getStringBounds (text, myG);
         switch (alignment) {
            case LEFT: {
               myG.drawString (text, x, y);
               break;
            }
            case CENTER: {
               myG.drawString (text, x-(float)rect.getWidth()/2f, y);
               break; 
            }
            case RIGHT: {
               myG.drawString (text, x-(float)rect.getWidth(), y);
               break;
            }
            default: {
               throw new InternalErrorException (
                  "Unimplemented alignment "+alignment);
            }
         }
      }

      public boolean allowsFloatPolylines() {
         return false;
      }

      public void drawPolyline (float[] xvals, float[] yvals, int cnt) {
         throw new UnsupportedOperationException();
      }

      public void drawPolyline (int[] xvals, int[] yvals, int cnt) {
         myG.drawPolyline (xvals, yvals, cnt);
      }

      public void endGraphics() {
         mySolid = false;
      }

      public void beginClipping (float x, float y, float w, float h) {
         mySavedClip = myG.getClip();
         myG.setClip ((int)x, (int)y, (int)w, (int)h);
      }

      public void endClipping () {
         myG.setClip (mySavedClip);
      }
   }

   public static PropertyList myProps =
      new PropertyList (NumericProbePanel.class);

   static {
      myProps.add (
         "xRange", "x axis range in display", DEFAULT_X_RANGE);
      myProps.add (
         "yRange", "y axis range in display", DEFAULT_Y_RANGE);
      myProps.add (
         "autoRanging isAutoRanging setAutoRanging",
         "grid range is adjusted automatically", DEFAULT_AUTO_RANGING);
      myProps.add (
         "drawGrid", "enable drawinog the grid", DEFAULT_DRAW_GRID);
      myProps.add (
         "tickLength", "length of label ticks", DEFAULT_TICK_LENGTH);
      myProps.add (
         "lineWidth", "width of trace lines", DEFAULT_LINE_WIDTH);
      myProps.add (
         "knotSize", "knot point size (pixels)", DEFAULT_KNOT_SIZE);
      myProps.add (
         "knotsVisible", "draw knot points if true", DEFAULT_KNOTS_VISIBLE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * {@inheritDoc}
    */
   public Property getProperty (String path) {
      return PropertyList.getProperty (path, this);
   }

   //
   // Properties
   //

   public void setDrawGrid (boolean enable) {
      if (myDrawGrid != enable) {
         myDrawGrid = enable;
         firePropertyChangeListeners ("drawGrid");
         repaint();
      }
   }

   public boolean getDrawGrid() {
      return myDrawGrid;
   }


   public void setXRange (DoubleInterval range) {
      if (!range.equals (myXRange)) {
         myXRange = new DoubleInterval (range);
      }
   }

   public void setXRange (double min, double max) {
      setXRange (new DoubleInterval (min, max));
   }

   public void setDefaultXRange () {
      setXRange (getDefaultXRange());
   }

   public DoubleInterval getXRange() {
      return new DoubleInterval (myXRange);
   }

   public Range getXRangeRange() {
      return new NumericIntervalRange(null, /*allowEmpty=*/false);
   }

   public void setYRange (DoubleInterval range) {
      if (!range.equals (myYRange)) {
         myYRange = new DoubleInterval (range);
      }
   }

   public void setYRange (double min, double max) {
      setYRange (new DoubleInterval (min, max));
   }

   public void setDefaultYRange () {
      setYRange (getDefaultYRange());
   }

   public DoubleInterval getYRange() {
      return new DoubleInterval (myYRange);
   }

   public Range getYRangeRange() {
      return new NumericIntervalRange(null, /*allowEmpty=*/false);
   }

   public void setTickLength (int len) {
      if (myTickLength != len) {
         myTickLength = len;
         firePropertyChangeListeners ("tickLength");
      }
   }

   public int getTickLength() {
      return myTickLength;
   }

   public void setLineWidth (int width) {
      if (myLineWidth != width) {
         myLineWidth = width;
         firePropertyChangeListeners ("lineWidth");
      }
   }

   public int getLineWidth() {
      return myLineWidth;
   }
   
   public int getKnotSize() {
      return myKnotSize;
   }
   
   public void setKnotSize (int size) {
      if (myKnotSize != size) {
         myKnotSize = size;
         firePropertyChangeListeners ("knotSize");
      }
   }

   public boolean getKnotsVisible() {
      return myKnotsVisible;
   }
   
   public void setKnotsVisible (boolean enable) {
      if (myKnotsVisible != enable) {
         myKnotsVisible = enable;
         firePropertyChangeListeners ("knotsVisible");
      }
   }

   public boolean isAutoRanging() {
      return myAutoRangingP;
   }

   public void setAutoRanging (boolean enable) {
      if (myAutoRangingP != enable) {
         myAutoRangingP = enable;
         firePropertyChangeListeners ("autoRanging");
         if (enable) {
            fitDisplayRange();
         }
      }
   }


   // Changed
   // Replaced 8 line function into 1 liner - optimization
   protected NumericList getNumericList() {
      return myNumericList;
   }

   public Interpolation.Order getInterpolationOrder() {
      return getInterpolation().getOrder ();
   }
   
   public void setInterpolationOrder (Interpolation.Order order) {
      if (order != getInterpolationOrder()) {
         if (myProbe != null) {
            myProbe.setInterpolationOrder(order);
         }
         else {
            myNumericList.setInterpolationOrder(order);
         }
         Main.getMain().rewidgetUpdate();
      }
   }
   
   public Interpolation getInterpolation() {
      if (myProbe != null) {
         return myProbe.getInterpolation();
      }
      else {
         return myNumericList.getInterpolation();
      }
   }
   
   protected void updateDisplaysWithoutAutoRanging() {
      if (myProbe != null) {
         myProbe.updateDisplaysWithoutAutoRanging();
      }
      else {
         repaintWithoutAutoRanging();
      }
   }
   
   protected void updateDisplays() {
      if (myProbe != null) {
         myProbe.updateDisplays();
      }
      else {
         repaint();
      }
   }

   protected double getScale() {
      if (myProbe != null) {
         return myProbe.getScale();
      }
      else {
         return myScale;
      }
   }
   
   protected double getStartTime() {
      if (myProbe != null) {
         return myProbe.getStartTime();
      }
      else {
         return myStartTime;
      }
   }
   
   protected double getStopTime() {
      if (myProbe != null) {
         return myProbe.getStopTime();
      }
      else {
         return myStopTime;
      }
   }
   
   protected LegendDisplay getLegend() {
      if (myProbe != null) {
         return myProbe.getLegend();
      }
      else {
         return myLegendDisplay;         
      }
   }
   
   protected void setLegend (LegendDisplay legend) {
      if (myProbe != null) {
         myProbe.setLegend(legend);
      }
      else {
         myLegendDisplay = legend;         
      }
   }
   
   protected void removeLegend() {
      LegendDisplay legend = getLegend();
      if (legend != null) {
         if (legend.isVisible()) {
            legend.dispose();
         }
         setLegend (null);
      }        
   }
   
   public void setDisplaySize (int width, int height) {
      width = (width <= 0 ? 1 : width);
      // to resolve a bug when probed are created with a display size of zero
      Dimension size = new Dimension (width, height);
      this.setSize (size);
      this.setMinimumSize (size);
      this.setMaximumSize (size);
      this.setPreferredSize (size);
   }

   public CursorMode getCursorMode() {
      return myCursorMode;
   }

   public void setCursorMode (CursorMode mode) {
      myCursorMode = mode;
   }

   public DoubleInterval getDefaultYRange() {
      double[] range;
      if (myProbe != null) {
         range = myProbe.getDefaultDisplayRange();
      }
      else {
         range = NumericProbeBase.getRange (myNumericList);
      }     
      return new DoubleInterval (range[0], range[1]);
   }

   public DoubleInterval getDefaultXRange() {
      double min, max;
      if (useVirtualTime) {
         min = 0;
         max =
            (getStopTime() - getStartTime())
            / getScale();
      }
      else {
         min = getStartTime();
         max = getStopTime();
      }
      return new DoubleInterval (min, max);      
   }

   public void resetDisplay() {
      //rangeLevel = 0;
      setDefaultYRange();
      setDefaultXRange();
      repaint();
   }

   // repaint required to draw timeline cursor in ProbeDisplayArea JPanel
   public void repaint() {
      if (isAutoRanging()) {
         adjustRangeIfNecessary();
      }
      if (this.getParent() != null) {
         this.getParent().repaint();
      }
   }

   public void repaintWithoutAutoRanging() {
      if (this.getParent() != null) {
         this.getParent().repaint();
      }
   }

   protected void addPropertyChangeListener (PropertyChangeListener l) {
      myPropListeners.add (l);
   }

   protected boolean removePropertyChangeListener (PropertyChangeListener l) {
      return myPropListeners.remove (l);
   }

   protected void firePropertyChangeListeners (String propName) {
      PropertyChangeEvent e = new PropertyChangeEvent (this, propName);
      for (PropertyChangeListener l : myPropListeners) {
         l.propertyChanged (e);
      }
      // refresh property dialog if necessary
      if (myPropertyDialog != null) {
         myPropertyDialog.updateWidgetValues();
      }
   }

   protected void fieldInitialization() {

      //setAutoRange();
      setDefaultYRange();
      setDefaultXRange();

      editingP = false;
   }

   protected void appearanceInitialization() {
      this.setAlignmentX (Component.CENTER_ALIGNMENT);
      this.setAlignmentY (Component.CENTER_ALIGNMENT);
      this.setOpaque (false);
   }

   protected void listenerInitialization() {
      this.displayListener = new DisplayListener();
      this.wheelListener = new WheelListener();
      this.addMouseListener (displayListener);
      this.addMouseMotionListener (displayListener);
      this.addMouseWheelListener (wheelListener);
   }

   private static Cursor createCursor (String fileName, String toolTip) {
      String imagePath = "artisynth/core/gui/timeline/BasicIcon/" + fileName;
      URL url = ArtisynthPath.getResource (imagePath);
      if (url == null) {
         System.out.println ("Warning: can't find cursor image " + imagePath);
         return null;
      }
      try {
         url.openStream();
      }
      catch (Exception e) {
         System.out.println ("Warning: can't open " + imagePath);
      }
      Toolkit toolkit = Toolkit.getDefaultToolkit();
      Image image = toolkit.getImage (url);
      if (image == null) {
         System.out.println ("Warning: can't find cursor image " + imagePath);
         return null;
      }
      return toolkit.createCustomCursor (image, new Point (0, 0), toolTip);
   }

   /**
    * Initialize the two zoom cursor images for zooming in and out of the
    * display.
    * 
    */
   private void zoomCursorInitialization() {
      zoomInCursor = createCursor ("ZoomIn.gif", "zoom in");
      zoomOutCursor = createCursor ("ZoomOut.gif", "zoom out");
      translateCursor = createCursor ("Hand.gif", "move display");
   }

   protected void fitDisplayRange() {
      double[] range = NumericProbeBase.getRange(getNumericList());
      setYRange (range[0], range[1]);
      // setDataBuffer(true);
      repaint();
      
   }

   private void drawZeroLine (DataRenderer r, ScreenTransform xform) {
      float zpixel = xform.yvalueToPixel (0);
      if (zpixel >= myMargins[TOP] && zpixel < myMargins[TOP]+getPlotHeight()) {
         r.beginLineGraphics (1, Color.LIGHT_GRAY);
         float xs = myMargins[LEFT];
         r.drawLine (xs, zpixel, xs+getPlotWidth()-1, zpixel);
         r.endGraphics();
      }
   }

   int fontHeight = 10;

   /**
    * Repaint if probe property changes require it.
    */
   public void repaintForPropertyChanges() {
      if (getInterpolationOrder() != myLastInterpolationOrder) {
         repaint();
      }
   }

   public void adjustRangeIfNecessary() {
      double[] minMax = new double[2];
      myNumericList.getMinMaxValues(minMax);
      double range = minMax[1] - minMax[0];

      if (myNumericList.isEmpty() || range < 1e-13) {
         minMax[0] = -1;
         minMax[1] = 1;
         range = 2;
      }
      else {
         minMax[0] -= 0.1 * range;
         minMax[1] += 0.1 * range;
      }
      double maxY = myYRange.getUpperBound();
      double minY = myYRange.getLowerBound();
      if (maxY < minMax[1]) {
         maxY = minMax[1] + 0.25 * range;
      }
      if (minY > minMax[0]) {
         minY = minMax[0] - 0.25 * range;
      }
      setYRange (minY, maxY);
   }

   protected float getPlotWidth() {
      return getWidth() - (myMargins[LEFT]+myMargins[RIGHT]);
   }

   protected float getPlotHeight() {
      return getHeight() - (myMargins[TOP]+myMargins[BOTTOM]);
   }

   protected PlotTraceManager getTraceManager() {
      return myTraceManager;
   }

   protected void updateMargins (float[] margins, boolean largeDisplay) {
      if (largeDisplay) {
         margins[BOTTOM] = 20f;
         margins[TOP] = 15f;
         margins[LEFT] = 30f;
         margins[RIGHT] = 15f;

         // Adjust left and right margins to make sure they are large enough
         // for labels. We do this by computing the string bounds on tentative
         // max or min label values.

         // For left margin, we need to make sure it is large enough for the
         // y-axis labels.
         double valuePerYPixel = myYRange.getRange()/getPlotHeight();
         TickValue ySpacing = getTickSpacing (valuePerYPixel);

         double maxY = myYRange.getUpperBound();
         double minY = myYRange.getLowerBound();

         // most digit characters are 8 pixels wide, except for '.' and '-'
         int pixelsPerChar = 8;

         String str;
         str = createLabelString (getTickValueAbove(minY,ySpacing), false);
         margins[LEFT] =
            Math.max (str.length()*pixelsPerChar+5, margins[LEFT]);
         str = createLabelString (getTickValueBelow(maxY,ySpacing), false);
         margins[LEFT] =
            Math.max (str.length()*pixelsPerChar+5, margins[LEFT]);

         // For right margin, we need to make sure it is large enough for the
         // final x-axis label.
         double maxTime = myXRange.getUpperBound();
         double timePerYPixel = myXRange.getRange()/getPlotWidth();
         TickValue xSpacing = getTickSpacing (timePerYPixel);

         str = createLabelString (getTickValueBelow (maxTime,xSpacing), false);
         margins[RIGHT] =
            Math.max (str.length()/2*pixelsPerChar+5, margins[RIGHT]);
      }
      else {
         // no margins for small display
         margins[LEFT] = 0f;
         margins[BOTTOM] = 0f;
         margins[TOP] = 0f;
         margins[RIGHT] = 0f;
      }
   }         

   protected void clipToMargins (Point p) {
      int left = (int)myMargins[LEFT];
      int right = (int)(getPlotWidth()+myMargins[LEFT]-1);
      int top = (int)myMargins[TOP];
      int bottom = (int)(myMargins[TOP]+getPlotHeight()-1);

      if (p.x < left) {
         p.x = left;
      }
      else if (p.x > right) {
         p.x = right;
      }
      if (p.y < top) {
         p.y = top;
      }
      else if (p.y > bottom) {
         p.y = bottom;
      }
   }
   
   public void drawPlot (DataRenderer r, boolean largeDisplay) {
      updateMargins (myMargins, largeDisplay);
      ScreenTransform xform = createScreenTransform();

      r.beginPlot (getWidth(), getHeight(), /*antialiasing=*/true);
      if (largeDisplay) {
         TickValue[] xticks = getXTickValues();
         TickValue[] yticks = getYTickValues();
         if (myDrawGrid) {
            drawGrid (r, xform, xticks, yticks);
         }
         drawLabels (r, xform, xticks, yticks);
         drawTicks (r, xform, xticks, yticks);
      }
      else {
         drawZeroLine (r, xform);
      }

      // ======================================================
      // Actual data plotting
      if (myNumericList != null) {
         // Draw plot lines
         drawPlotLines (r, xform);
         if (myKnotsVisible) {
            // Draw Knot Points
            drawKnots (r, xform);
         }
      }
      r.endPlot();
   }

   public void paint (Graphics g) {

      //setAntialiasing (g, true);
      DataRenderer r = new PanelRenderer((Graphics2D)g);
      if (myLargeDisplay) {
         // don't need to do this in small display for some reason ...
         r.beginSolidGraphics (getBackground());
         r.drawRect(0, 0, getWidth(), getHeight());
         r.endGraphics();
      }
      drawPlot (r, myLargeDisplay);
      // draw the zoom selecting rectangle if it exists
      if (zoomPnt0 != null && zoomPnt1 != null) {
         g.setColor (Color.GRAY);
         int x = Math.min (zoomPnt0.x, zoomPnt1.x);
         int y = Math.min (zoomPnt0.y, zoomPnt1.y);
         int w = Math.abs (zoomPnt1.x - zoomPnt0.x);
         int h = Math.abs (zoomPnt1.y - zoomPnt0.y);
         g.drawRect (x, y, w, h);
      }
   }

   void interpolateValues (
      int[] segNums, float[][] yBuffer, ScreenTransform xform) {

      Order interpOrder = getInterpolationOrder();
      boolean linearInterp =
         (interpOrder == Order.Linear || interpOrder == Order.Step);

      NumericList list = getNumericList();
      if (list != null) {
         int maxXPixels = segNums.length;
         NumericListKnot prevKnot = null;
         VectorNd yvec = new VectorNd (list.getVectorSize());

         // Create line plot
         int segNum = -1;
         double prevt = -1;
         for (int i = 0; i < maxXPixels; i++) {
            float xpixel = i + myMargins[LEFT];
            double x = xform.pixelToXvalue ((int)xpixel);
            if (linearInterp) {
               NumericListKnot nextKnot = list.findKnotAtOrBefore (x, prevKnot);
               if (nextKnot != prevKnot) {
                  segNum++;
               }
               segNums[i] = segNum;
            }
            else {
               segNums[i] = i;
            }
            prevKnot = list.interpolate (yvec, x, getInterpolation(), prevKnot);
            for (int k = 0; k < yvec.size(); k++) {
               yBuffer[k][i] = xform.yvalueToPixel (yvec.get(k));
            }
         }
      }
   }

   /**
    * Stores a list of x and y points for drawing a polyline using
    * java.awt.Graphics.
    */
   private interface PolylineBuffer {
      
      void addPoint (float x, float y);

      void drawAndClear (DataRenderer r);
   }

   private class FloatPolylineBuffer implements PolylineBuffer {

      float[] myXbuf;
      float[] myYbuf;
      int myCnt;

      FloatPolylineBuffer (int size) {
         myXbuf = new float[size];
         myYbuf = new float[size];
      }

      public void addPoint (float x, float y) {
         myXbuf[myCnt] = x;
         myYbuf[myCnt] = y;
         myCnt++;
      }

      public void drawAndClear (DataRenderer r) {
         if (myCnt > 0) {
            r.drawPolyline (myXbuf, myYbuf, myCnt);
            myCnt = 0;
         }
      }
   }

   private class IntPolylineBuffer implements PolylineBuffer  {

      int[] myXbuf;
      int[] myYbuf;
      int myCnt;

      IntPolylineBuffer (int size) {
         myXbuf = new int[size];
         myYbuf = new int[size];
      }

      public void addPoint (float x, float y) {
         myXbuf[myCnt] = (int)x;
         myYbuf[myCnt] = (int)y;
         myCnt++;
      }

      public void drawAndClear (DataRenderer r) {
         if (myCnt > 0) {
            r.drawPolyline (myXbuf, myYbuf, myCnt);
            myCnt = 0;
         }
      }
   }

   /**
    * Queries whether a given y value is out of range. If it is not, the method
    * returns -1. If it is, returns the range value (min or max) that the value
    * is below or above.
    */
   protected static float clipY (float y, float min, float max) {
      if (y < min) {
         return min;
      }
      else if (y > max) {
         return max;
      }
      else {
         return -1f;
      }
   }

   private void drawPlotLines (DataRenderer r, ScreenTransform xform) {

      NumericList list = getNumericList();
      if (list == null) {
         // just in case ...
         return;
      }

      int numXPixels = (int)getPlotWidth();
      int yVectorSize = list.getVectorSize();

      float[][] yBuffer = new float[yVectorSize][numXPixels];
      int[] segNums = new int[numXPixels];

      interpolateValues (segNums, yBuffer, xform);

      PolylineBuffer lineBuf;
      if (r.allowsFloatPolylines()) {
         lineBuf = new FloatPolylineBuffer (numXPixels);
      }
      else {
         lineBuf = new IntPolylineBuffer (numXPixels);
      }

      // shrink ymin/ymax by 1 pixel to account spaced used by drawing the plot
      // border
      float ymin = myMargins[TOP]+1;
      float ymax = myMargins[TOP]+getPlotHeight()-1;
      float xmin = myMargins[LEFT];

      r.beginClipping (
         xmin+1, ymin, getPlotWidth()-2, getPlotHeight()-2);

      // iterate through each plot trace, in the appropriate order.
      for (int k = yVectorSize - 1; k >= 0; k--) {

         int idx = myTraceManager.getOrderedTraceIndex (k);
         PlotTraceInfo pti = myTraceManager.getTraceInfo (idx);

         if (pti.isVisible()) {
            // if plot trace is visible, then go through all the x,y point
            // pairs and create a polyline sequence for each contiguous set of
            // points whose y values are not out of range.  For
            // efficiency and the improve rendering quality, we also do not add
            // polyline points which lie on the same (linearly interpolated)
            // segment.
            r.beginLineGraphics (myLineWidth, pti.getColor());

            float prevOutOfRange = -1; // previous value out of range
            float prevY = -1;          // previous y value
            float lastXAdded = -1;     // last x value added to current polyline

            for (int i = 0; i < numXPixels; i++) {
               float y = yBuffer[idx][i];
               float x = i + xmin;

               float outOfRange = clipY (y, ymin, ymax);

               if (outOfRange == -1) {
                  // current y is in range.
                  //
                  // add the previous y if it was out of range, or linear
                  //   segments have changed and it was not already added
                  // add the current y if:
                  //   the previous y was out of range OR
                  //   there is no previous y (i == 0)
                  //   linear segments have changed
                  if (prevOutOfRange != -1) {
                     lineBuf.addPoint (x-1, prevY);
                     lineBuf.addPoint (x, y);
                     lastXAdded = x;
                  }
                  else if (i == 0) {
                     lineBuf.addPoint (x, y);
                     lastXAdded = x;
                  }
                  else if (segNums[i-1] != segNums[i]) {
                     if (lastXAdded != x-1) {
                        // ensure previous point added to avoid sampling issues
                        lineBuf.addPoint (x-1, prevY);
                     }
                     lineBuf.addPoint (x, y);
                     lastXAdded = x;
                  }
               }
               else {
                  // current y is OUT of range.
                  //
                  // if there was a previous y value that was in range,
                  // or out of range in a different way, then:
                  //   add the previous y value if it was not already added
                  //   add the current y value and close off the polyline
                  if (i > 0 && prevOutOfRange != outOfRange) {
                     if (lastXAdded != x-1) {
                        lineBuf.addPoint (x-1, prevY);
                     }
                     lineBuf.addPoint (x, y);
                     lineBuf.drawAndClear (r);
                     lastXAdded = -1;
                  }
               }
               prevY = y;
               prevOutOfRange = outOfRange;
            }
            // if last point was in range, then add it if it was not previously
            // added and close off the polyline
            if (prevOutOfRange == -1) {
               if (lastXAdded != xmin+numXPixels-1) {
                  lineBuf.addPoint (xmin+numXPixels-1, prevY);
               }
               lineBuf.drawAndClear(r);
            }
            r.endGraphics();
         }
      }
      r.endClipping();
      myLastInterpolationOrder = getInterpolationOrder();
   }

   private void drawKnots (DataRenderer r, ScreenTransform xform) {

      float xmin = myMargins[LEFT];
      float xmax = myMargins[LEFT]+getPlotWidth()-1;
      float ymin = myMargins[TOP];
      float ymax = myMargins[TOP]+getPlotHeight()-1;

      ArrayList<NumericListKnot> visibleKnots
         = new ArrayList<>((int)getPlotWidth());
      NumericList list = getNumericList();
      Iterator<NumericListKnot> it = list.iterator();
      float xprev = -1;
      while (it.hasNext()) {
         NumericListKnot knot = it.next();
         float x = xform.xvalueToPixel (knot.t);
         if (x != xprev && x >= xmin && x <= xmax) {
            visibleKnots.add (knot);
         }
         xprev = x;
      }
      for (int k = list.getVectorSize()-1; k >= 0; k--) {
         int idx = myTraceManager.getOrderedTraceIndex (k);
         PlotTraceInfo pti = myTraceManager.getTraceInfo (idx);
         if (pti.isVisible()) {
            r.beginSolidGraphics (pti.getColor());
            for (NumericListKnot knot : visibleKnots) {
               float y = xform.yvalueToPixel (knot.v.get(idx));
               if (y >= ymin && y <= ymax) {
                  float x = xform.xvalueToPixel (knot.t);
                  float size = myKnotSize;
                  if (myLargeDisplay) {
                     r.drawCircle (x, y, size);
                  }
                  else {
                     float hs = (int)Math.floor(size/2f);
                     r.drawRect (x-hs, y-hs, size, size);
                  }
               }
            }
            r.endGraphics();
         }
      }
   }

   static private double[] units = new double[] { 5, 2, 1 };
   static private int maxPixels = 90;

   protected TickValue getTickSpacing (double valuePerPixel) {
      double maxUnits = maxPixels * valuePerPixel;
      double exp = Math.floor (Math.log10 (maxUnits));
      double base = Math.pow (10, exp);
      int u;
      for (u = 0; u < units.length - 1; u++) {
         if (base * units[u] < maxUnits) {
            break;
         }
      }
      return new TickValue (base*units[u], (int)units[u], (int)exp);
   }

   protected TickValue getTickValueBelow (double max, TickValue spacing) {
      long m = (long)Math.floor (max/spacing.getValue());
      return new TickValue (
         m*spacing.getValue(), m*spacing.getM(), spacing.getE());
   }
   
   protected TickValue getTickValueAbove (double min, TickValue spacing) {
      long m = (long)Math.ceil (min/spacing.getValue());
      return new TickValue (
         m*spacing.getValue(), m*spacing.getM(), spacing.getE());
   }
   
   protected double getSpacing (double valuePerPixel) {
      double maxUnits = maxPixels * valuePerPixel;
      double exp = Math.floor (Math.log10 (maxUnits));
      double base = Math.pow (10, exp);
      int u;
      for (u = 0; u < units.length - 1; u++) {
         if (base * units[u] < maxUnits) {
            break;
         }
      }
      double spacing = base * units[u];
      return spacing;
   }

   public static double round (double d, int decimalPlace){
      BigDecimal bd = new BigDecimal (Double.toString (d));
      bd = bd.setScale (decimalPlace, BigDecimal.ROUND_HALF_UP);
      System.out.println( d + " :: " + decimalPlace + " :: " + bd.doubleValue ());
      return bd.doubleValue();
    }

   private boolean labelHasExponent (String str) {
      for (int i=0; i<str.length(); i++) {
         char c = str.charAt (i);
         if (c == 'e' || c == 'E') {
            return true;
         }
      }
      return false;
   }

   String createLabelString (TickValue tval, boolean forceExponent) {
      return myLabelFormat.createLabel (
         tval.getM(), tval.getE(), forceExponent);
   }

   TickValue[] getYTickValues () {
      ScreenTransform xform = createScreenTransform();
      TickValue spacing = getTickSpacing (xform.yvaluePerPixel());
      TickValue ytick = getTickValueAbove (myYRange.getLowerBound(), spacing);

      ArrayList<TickValue> values = new ArrayList<>();
      double maxY = myYRange.getUpperBound();
      double tol = EPS*maxY;
      for (int i = 0; ytick.getValue() <= maxY+tol; i++) {
         values.add (new TickValue(ytick));
         ytick.add (spacing);
      }
      return values.toArray(new TickValue[0]);
   }

   TickValue[] getXTickValues () {
      ScreenTransform xform = createScreenTransform();
      TickValue spacing = getTickSpacing (xform.xvaluePerPixel());
      TickValue xtick = getTickValueAbove (myXRange.getLowerBound(), spacing);

      ArrayList<TickValue> values = new ArrayList<>();
      double maxX = myXRange.getUpperBound();
      double tol = EPS*maxX;
      for (int i = 0; xtick.getValue() <= maxX+tol; i++) {
         values.add (new TickValue(xtick));
         xtick.add (spacing);
      }
      return values.toArray(new TickValue[0]);
   }

   private void drawLabels (
      DataRenderer r, ScreenTransform xform,
      TickValue[] xticks, TickValue[] yticks) {

      r.beginTextGraphics (fontHeight, Color.BLACK);

      // If max/min y tick labels have exponents, force exponents for all:
      boolean forceLabelExponent = false;
      String label = createLabelString (yticks[0], false);
      if (myLabelFormat.containsExponent(label)) {
         forceLabelExponent = true;
      }
      else {
         label = createLabelString (yticks[yticks.length-1], false);
         if (myLabelFormat.containsExponent(label)) {
            forceLabelExponent = true;
         }
      }

      for (int i=0; i<yticks.length; i++) {
         float yi = xform.yvalueToPixel(yticks[i].getValue());
         label = createLabelString (yticks[i], forceLabelExponent);
         float xs = myMargins[LEFT] - 4;
         float ys = yi + fontHeight/2 - 1;
         // g.setColor (getBackground());
         // g.fillRect (xs - 1, ys - h, w + 2, h + 2);
         r.drawText (label, xs, ys, TextAlign.RIGHT);
      }

      // If max x tick label has an exponent, force exponents for all:
      label = createLabelString (xticks[xticks.length-1], false);
      forceLabelExponent = myLabelFormat.containsExponent(label);

      for (int i = 0; i<xticks.length; i++) {
         float xi = xform.xvalueToPixel (xticks[i].getValue());
         label = createLabelString (xticks[i], forceLabelExponent);
         float xs = xi;
         float ys = getHeight() - myMargins[BOTTOM] + fontHeight;
         // g.setColor (getBackground());
         // g.fillRect (xs - 1, ys - h, w + 2, h + 2);
         r.drawText (label, xs, ys, TextAlign.CENTER);
      }
      r.endGraphics();
   }

   private void drawTicks (
      DataRenderer r, ScreenTransform xform,
      TickValue[] xticks, TickValue[] yticks) {

      r.beginLineGraphics (1, Color.BLACK);
      // y axis ticks
      for (int i=0; i<yticks.length; i++) {
         float yi = xform.yvalueToPixel(yticks[i].getValue());
         float x0 = myMargins[LEFT];
         r.drawLine (x0, yi, x0+myTickLength, yi);
      }

      // x axis ticks
      for (int i = 0; i<xticks.length; i++) {
         float xi = xform.xvalueToPixel (xticks[i].getValue());
         float y0 = myMargins[TOP]+getPlotHeight()-1;
         r.drawLine (xi, y0, xi, y0-myTickLength);
      }
      // box around the whole graph:
      r.drawRect (
         myMargins[LEFT], myMargins[TOP], getPlotWidth()-1, getPlotHeight()-1);

      r.endGraphics();
   }

   private void drawGrid (
      DataRenderer r, ScreenTransform xform,
      TickValue[] xticks, TickValue[] yticks) {

      r.beginLineGraphics (1, Color.LIGHT_GRAY);
      // horizontal lines
      for (int i=0; i<yticks.length; i++) {
         float yi = xform.yvalueToPixel(yticks[i].getValue());
         float x0 = myMargins[LEFT];
         r.drawLine (x0, yi, x0+getPlotWidth()-1, yi);
      }
      // vertical lines
      for (int i = 0; i<xticks.length; i++) {
         float xi = xform.xvalueToPixel (xticks[i].getValue());
         float y0 = myMargins[TOP]+getPlotHeight()-1;
         r.drawLine (xi, myMargins[TOP], xi, y0);
      }
      r.endGraphics();
   }
   
   public void swapDrawIndicies (int a, int b) {
      PlotTraceInfo ptiA = myTraceManager.getTraceInfo (a);
      PlotTraceInfo ptiB = myTraceManager.getTraceInfo (b);
      myTraceManager.swapTraceOrder (ptiA, ptiB);
      updateDisplaysWithoutAutoRanging();
   }         

   /**
    * author: Chad version 1.0: Requests a scale vactor from the user, by which
    * all of the values in the numeric probe are multiplied.
    */

   protected void scaleProbeValues() {
      String input =
         JOptionPane.showInputDialog (
            this, "Scale factor", "Scale Probe", JOptionPane.PLAIN_MESSAGE);

      Double scaleFactor = null;

      try {
         scaleFactor = (Double.parseDouble (input));
      }
      catch (Exception ignore) {
      }

      if (scaleFactor != null && !scaleFactor.isNaN()) {
         myNumericList.scale (scaleFactor);
         updateDisplays();
      }
   }

   /**
    * Zoom in on a particular point on the numeric probe display.
    * 
    * @param zoomPoint
    * The point to zoom in on.
    */
   public void zoomIn (Point zoomPoint) {
      ScreenTransform xform = createScreenTransform();
      double scalingFactor = 2;

      // zoom in on the y range
      DoubleInterval defYRange = getDefaultYRange();

      double rangeSize = myYRange.getRange()/scalingFactor;
      double yCenterPoint = xform.pixelToYvalue (zoomPoint.y);
      double newYMin = yCenterPoint - (rangeSize / 2);
      double newYMax = yCenterPoint + (rangeSize / 2);

      if (newYMin < defYRange.getLowerBound()) {
         // the new y minimum is outside the range, default the range
         newYMin = defYRange.getLowerBound();
         newYMax = newYMin + rangeSize;
      }
      else if (newYMax > defYRange.getUpperBound()) {
         // the new y maximum is outside the range, default the range
         newYMax = defYRange.getUpperBound();
         newYMin = newYMax - rangeSize;
      }

      setYRange (newYMin, newYMax);
      setAutoRanging (false);

      // zoom in on the time domain
      double domainSize = myXRange.getRange()/scalingFactor;
      double xCenterPoint = xform.pixelToXvalue (zoomPoint.x);
      DoubleInterval newXRange = new DoubleInterval (
         xCenterPoint - (domainSize / 2), xCenterPoint + (domainSize / 2));

      newXRange.intersect (getDefaultXRange());
      setXRange (newXRange);
   }

   /**
    * Zoom out on a particular point on the numeric probe display. Only zoom out
    * as far as the original display range.
    * 
    * @param zoomPoint
    * The point to zoom out on.
    */
   public void zoomOut (Point zoomPoint) {
      
      ScreenTransform xform = createScreenTransform();
      
      double scalingFactor = 2;

      // zoom out on the y range
      DoubleInterval defYRange = getDefaultYRange();
      double defYmin = defYRange.getLowerBound();
      double defYmax = defYRange.getUpperBound();
      double defaultRangeSize = defYRange.getRange();

      double rangeSize = myYRange.getRange()*scalingFactor;
      double yCenterPoint = xform.pixelToYvalue (zoomPoint.y);
      double newYMin = yCenterPoint - (rangeSize / 2);
      double newYMax = yCenterPoint + (rangeSize / 2);

      if (newYMin < defYmin && newYMax <= defYmax &&
          rangeSize <= defaultRangeSize) {
         // just the y min is out of range and the new display height is not
         // greater than the default
         newYMin = defYmin;
         newYMax = newYMin + rangeSize;
      }
      else if (newYMin >= defYmin && newYMax > defYmax &&
               rangeSize <= defaultRangeSize) {
         // just the y max is out of range and the new display height is not
         // greater than the default
         newYMax = defYmax;
         newYMin = newYMax - rangeSize;
      }
      else if (newYMin < defYmin || newYMax > defYmax) {
         // either the y min or the y max or both are out of range
         newYMin = defYmin;
         newYMax = defYmax;
      }
      setYRange (newYMin, newYMax);
      setAutoRanging (false);

      // zoom out on the time domain
      DoubleInterval defXRange = getDefaultXRange();
      double defXmin = defXRange.getLowerBound();
      double defXmax = defXRange.getUpperBound();
      double defaultDomainSize = defXRange.getRange();

      double domainSize = myXRange.getRange()*scalingFactor;
      double xCenterPoint = xform.pixelToXvalue (zoomPoint.x);
      double newXMin = xCenterPoint - (domainSize / 2);
      double newXMax = xCenterPoint + (domainSize / 2);

      if (newXMin < defXmin && newXMax <= defXmax &&
          domainSize <= defaultDomainSize) {
         // just the x min is out of range and the new display width is not
         // greater than the default
         newXMin = defXmin;
         newXMax = newXMin + domainSize;
      }
      else if (newXMin >= defXmin && newXMax > defXmax &&
               domainSize <= defaultDomainSize) {
         newXMax = defXmax;
         newXMin = newXMax - domainSize;
      }
      else if (newXMin < defXmin || newXMax > defXmax) {
         newXMin = defXmin;
         newXMax = defXmax;
      }

      setXRange (newXMin, newXMax);
   }

   /**
    * Move the display around when it is zoomed in on. Constrain the movement to
    * where the probe is displayed.
    * 
    * @param xDistance
    * The distance to move the display in pixels.
    * @param yDistance
    * The distance to move the display in pixels.
    */
   public void moveDisplay (long xDistance, long yDistance) {
      
      ScreenTransform xform = createScreenTransform();
      double xDist = xDistance * xform.xvaluePerPixel();
      double yDist = yDistance * xform.yvaluePerPixel();

      double maxTime = myXRange.getUpperBound();
      double minTime = myXRange.getLowerBound();

      // get the maximum values of the display so we dont display past these
      // points
      DoubleInterval defYRange = getDefaultYRange();
      DoubleInterval defXRange = getDefaultXRange();

      if ((minTime + xDist) > defXRange.getLowerBound() &&
          (maxTime + xDist) < defXRange.getUpperBound()) {
         setXRange (minTime+xDist, maxTime+xDist);
         repaint();
      }

      double maxY = myYRange.getUpperBound();
      double minY = myYRange.getLowerBound();
      if ((minY + yDist) > defYRange.getLowerBound() &&
          (maxY + yDist) < defYRange.getUpperBound()) {
         setYRange (minY+yDist, maxY+yDist);
         setAutoRanging (false);
         repaint();
      }
   }

   /**
    * Increase the y range that is being viewed.  This is different from
    * zooming because we can increase the range past the original display
    * range.
    */
   public void increaseYRange() {
      double scalingFactor = 2;

      // get the center point of the probe
      double rangeSize = myYRange.getRange();
      double yCenterPoint = myYRange.getUpperBound() - (rangeSize / 2);
      
      double newYMin = yCenterPoint - (rangeSize * scalingFactor / 2);
      double newYMax = yCenterPoint + (rangeSize * scalingFactor / 2);
      
      setYRange (newYMin, newYMax);
      setAutoRanging (false);
      repaint();
   }

   /**
    * Decrease the y range that is being viewed. When the range is increased this
    * function undoes those increases.
    */
   public void decreaseYRange() {
      double scalingFactor = 2;

      // zoom in on the y range
      double rangeSize = myYRange.getRange();
      double yCenterPoint = myYRange.getUpperBound() - (rangeSize / 2);
      
      double newYMin = yCenterPoint - (rangeSize / scalingFactor / 2);
      double newYMax = yCenterPoint + (rangeSize / scalingFactor / 2);

      setYRange (newYMin, newYMax);
      setAutoRanging (false);
      repaint();
   }

   /**
    * Increase the x range that is being viewed.  This is different from
    * zooming because we can increase the range past the original display
    * range.
    */
   public void increaseXRange() {
      double scalingFactor = 2;

      double rangeSize = myXRange.getRange();
      double newTMin = myXRange.getLowerBound();
      double newTMax = newTMin + rangeSize*scalingFactor;
      setXRange (newTMin, newTMax);
      repaint();
   }

   /**
    * Decrease the x range that is being viewed. When the range is increased this
    * function undoes those increases.
    */
   public void decreaseXRange() {
      double scalingFactor = 0.5;

      double rangeSize = myXRange.getRange();
      double newTMin = myXRange.getLowerBound();
      double newTMax = newTMin + rangeSize*scalingFactor;
      setXRange (newTMin, newTMax);
      repaint();
   }

   public Point2d pixelsToCoords (Point p) {
      ScreenTransform xform = createScreenTransform();
      return new Point2d (xform.pixelToXvalue (p.x), xform.pixelToYvalue (p.y));
   }

   protected class WheelListener implements MouseWheelListener {

      public void mouseWheelMoved (MouseWheelEvent e) {
         if (myCursorMode != CursorMode.SELECT) {
            // determine if we are zooming in or out based on the direction the
            // mouse wheel is moved
            int wheelRotation = e.getWheelRotation();

            if (wheelRotation > 0) {
               // zoom positively
               zoomIn (e.getPoint());
               repaint();
            }
            else {
               // zoom negatively
               zoomOut (e.getPoint());
               repaint();
            }
         }
      }
   }

   protected class DisplayListener implements MouseInputListener,
   ActionListener {
      
      
      private NumericListKnot checkSelection(Point initialMousePos) {
         boolean found = false;
         ScreenTransform xform = createScreenTransform();

         double initTimeInSec = xform.pixelToXvalue (initialMousePos.x);
         NumericListKnot closestKnot =
            getNumericList().findKnotClosest (initTimeInSec);

         // enter the loop only if closest exists, in other words we got the
         // knot
         if (closestKnot != null) {
            int closestXPixel = (int)xform.xvalueToPixel (closestKnot.t);
            
            // see if delta-t is small enough to trigger a selection
            if ((closestXPixel - initialMousePos.x) <= SELECT_MARGIN &&
                (closestXPixel - initialMousePos.x) >= -SELECT_MARGIN) {
               // search through every value of closest.v to see if
               // the click is in the vicinity of any of them

               // many points same place
               for (int j = 0; j < closestKnot.v.size(); j++) {

                  double scannedYValue = (double)closestKnot.v.get (j);

                  int YPixel = (int)xform.yvalueToPixel (scannedYValue);
                  if ((YPixel - initialMousePos.y) <= SELECT_MARGIN &&
                      (YPixel - initialMousePos.y >= -SELECT_MARGIN) &&
                      found == false) {
                     for (int v = 0; v < closestKnot.v.size(); v++) {
                        int idx = myTraceManager.getOrderedTraceIndex (v);

                        if (closestKnot.v.get (j) -
                            closestKnot.v.get (idx) == 0) {
                           dirtyIndex = idx;
                           found = true;
                           break;
                        }
                     }
                  }
               }
            }
         }
         if (found) 
            return closestKnot;
         else 
            return null;
      }
      
      // the mouse is pressed on the pane where the knots are to move the knots
      public void mousePressed (MouseEvent e) {
         if (myCursorMode == CursorMode.ZOOM_IN &&
             e.getButton() == MouseEvent.BUTTON1) {
            // zooming in is true so set the initial point for if we drag to
            // select an area to zoom in on
            zoomPnt0 = e.getPoint();
         }

         if (e.isPopupTrigger()) {
            // get the initial location of the mouse press
            initialMousePos = e.getPoint();

            NumericListKnot closestKnot = checkSelection (initialMousePos);

            if (closestKnot != null) {
                  JPopupMenu editMenu = new JPopupMenu();

                  JMenuItem deleteDataPoint =
                     new JMenuItem ("Delete knot point");
                  deleteDataPoint.addActionListener (displayListener);

                  JMenuItem editDataPoint = new JMenuItem ("Edit knot point");
                  editDataPoint.addActionListener (displayListener);

                  editMenu.add (deleteDataPoint);
                  editMenu.add (editDataPoint);
                  
                  newKnot = closestKnot; // keep track of knot to be deleted.
                  editMenu.show (e.getComponent(), e.getX(), e.getY());
               }
               else {
                  // otherwise just show the regular popup menu
                  JPopupMenu popupMenu = createDisplayPopup();
                  popupMenu.show (e.getComponent(), e.getX(), e.getY());
               }
         }
         
         // perform editing only on press of primary mouse button
         if (e.getButton() == MouseEvent.BUTTON1 && myInputDataP) {
            // get the initial location of the mouse press
            initialMousePos = e.getPoint();

            // System.out.println("Initial: " + initialMousePos);

            NumericListKnot closestKnot = checkSelection(initialMousePos);
            if (closestKnot != null) {
               // if editing is enabled, let the oldKnot be the closest knot
               editingP = true;
               oldKnot = closestKnot;
               coincidentKnot = null;
            }
         }
      } // end mouse pressed

      private JMenuItem createMenuItem (String name, String cmd) {
         JMenuItem menuItem = new JMenuItem (name);
         menuItem.addActionListener (displayListener);
         menuItem.setActionCommand (cmd != null ? cmd : name);
         return menuItem;
      }

      private JMenuItem createMenuItem (String name) {
         return createMenuItem (name, name);
      }

      private JPopupMenu createDisplayPopup () {
         JPopupMenu.setDefaultLightWeightPopupEnabled (false);
         JPopupMenu popupMenu = new JPopupMenu();

         JMenuItem menuItem = createMenuItem ("Edit range and properties ...");
         popupMenu.add (menuItem);
         if (myPropertyDialog != null) {
            menuItem.setEnabled (false);
         }
         if (getKnotsVisible()) {
            popupMenu.add (createMenuItem ("Hide knot points"));
         }
         else {
            popupMenu.add (createMenuItem ("Show knot points"));
         }
         if (getLegend() == null) {
            popupMenu.add (createMenuItem ("Show legend ..."));
         }
         else {
            popupMenu.add (createMenuItem ("Hide legend"));
         }
         if (myInputDataP) {
            //popupMenu.addSeparator();
            JMenu submenu = new JMenu("Interpolation");
            submenu.add (createMenuItem ("Step"));
            submenu.add (createMenuItem ("Linear"));
            submenu.add (createMenuItem ("Cubic"));
            submenu.add (createMenuItem ("CubicStep"));
            if (getVectorSize() == 4 || getVectorSize() == 7 || getVectorSize() == 16) {
               submenu.add (createMenuItem ("SphericalLinear"));
               submenu.add (createMenuItem ("SphericalCubic"));
            }
            popupMenu.add (submenu);
         }

         if (isLargeDisplay()) {
            popupMenu.addSeparator();
            popupMenu.add (createMenuItem ("Clone display"));
            popupMenu.add (createMenuItem ("Export image as ..."));
         }
         popupMenu.addSeparator();
         popupMenu.add (createMenuItem ("Scale data ..."));
         popupMenu.add (createMenuItem ("Fit ranges to data"));

         return popupMenu;
      }

      public void mouseReleased (MouseEvent e) {
         if (myCursorMode == CursorMode.TRANSLATE) {
            // set the previous point that was moved from to null because we
            // are no longer moving the display
            previousPoint = null;
         }
         if (e.getButton() == MouseEvent.BUTTON1 &&
             zoomPnt0 != null && zoomPnt1 != null) {
            ScreenTransform xform = createScreenTransform();
            // an area has been zoom selected, so zoom in on that area
            int xpmin = Math.min (zoomPnt0.x, zoomPnt1.x);
            int xpmax = Math.max (zoomPnt0.x, zoomPnt1.x);
            int ypmin = Math.min (zoomPnt0.y, zoomPnt1.y);
            int ypmax = Math.max (zoomPnt0.y, zoomPnt1.y);

            DoubleInterval newYRange = new DoubleInterval (
               xform.pixelToYvalue (ypmax), xform.pixelToYvalue (ypmin));
            DoubleInterval newXRange = new DoubleInterval (
               xform.pixelToXvalue (xpmin), xform.pixelToXvalue (xpmax));

            // clip x to allowed range
            newXRange.intersect (getDefaultXRange());

            setYRange (newYRange);
            setAutoRanging (false);

            setXRange (newXRange);
            repaint();

            zoomPnt0 = null;
            zoomPnt1 = null;
         }

         // display of popup menu only on press of secondary mouse button
         if (e.isPopupTrigger()) {
            // get the initial location of the mouse press
            initialMousePos = e.getPoint();
            // System.out.println("Initial: " + initialMousePos);

            NumericListKnot closestKnot = checkSelection(initialMousePos);

            if (closestKnot != null) {
               // search through every value of closest.v to see if
               // the click is in the vicinity of any of them

               JPopupMenu editMenu = new JPopupMenu();
               JMenuItem deleteDataPoint =
                  new JMenuItem ("Delete knot point");
               deleteDataPoint.addActionListener (displayListener);
               JMenuItem editDataPoint = new JMenuItem ("Edit knot point");
               editDataPoint.addActionListener (displayListener);
               editMenu.add (deleteDataPoint);
               editMenu.add (editDataPoint);
               newKnot = closestKnot; // keep track of knot to be deleted.
               editMenu.show (e.getComponent(), e.getX(), e.getY());
            }
            else {
               // otherwise just show the regular popup menu              
               JPopupMenu popupMenu = createDisplayPopup();
               popupMenu.show (e.getComponent(), e.getX(), e.getY());
            }
         }

         // perform editing only on press of primary mouse button
         if (e.getButton() == MouseEvent.BUTTON1 && myInputDataP) {
            // get the initial location of the mouse press
            clipToMargins (e.getPoint());
            editingP = false;
         }
         
         if (isAutoRanging()) {
            repaint();
         }
      }

      public void mouseDragged (MouseEvent e) {
         // only enter this loop when the initial mouse press is close
         // enough to a knot point
         if (editingP == true && myInputDataP) {
            double tempTime, tempYValue;
            Point tempCoor = e.getPoint();

            // limit tempCoor to the bounds of the display
            clipToMargins (tempCoor);            

            // only allows dragging when there is a minimum number of
            // pixel deviation from the initial mouse press
            if (((tempCoor.x - initialMousePos.x) > DRAG_MARGIN ||
                 (tempCoor.x - initialMousePos.x) < -DRAG_MARGIN) ||
                ((tempCoor.y - initialMousePos.y) > DRAG_MARGIN ||
                 (tempCoor.y - initialMousePos.y) < -DRAG_MARGIN)) {
               // create a new knot with a copy of the old one
               newKnot = new NumericListKnot (oldKnot);

               // calculate the new info of the new knot,
               // and make changes to the new knot

               // the new way of setting time
               ScreenTransform xform = createScreenTransform();
               tempTime = xform.pixelToXvalue (tempCoor.x);

               tempYValue = xform.pixelToYvalue (tempCoor.y);
               newKnot.t = tempTime;
               newKnot.v.set (dirtyIndex, tempYValue);

               // add the new knot to the numeric list
               coincidentKnot = getNumericList().add (newKnot);

               // remove the selected knot from the display
               // if it's not already removed by the add action
               if (coincidentKnot == null) {
                  getNumericList().remove (oldKnot);
               }

               // replace oldKnot with the new knot, anticipating
               // that newKnot will be removed on the next drag event
               oldKnot = newKnot;
            }
         }
         else if (myCursorMode == CursorMode.TRANSLATE) {
            if (previousPoint == null) {
               previousPoint = e.getPoint();
            }
            else {
               moveDisplay (previousPoint.x - e.getX(), e.getY()
               - previousPoint.y);
               previousPoint = e.getPoint();
            }
         }
         else if (myCursorMode == CursorMode.ZOOM_IN &&
                  e.getModifiers() == MouseEvent.BUTTON1_MASK) {
            zoomPnt1 = new Point (e.getX(), e.getY());
         }
         // repaint();
         updateDisplaysWithoutAutoRanging();
      }

      public void mouseClicked (MouseEvent e) {
         if (myCursorMode == CursorMode.ZOOM_IN &&
             e.getButton() == MouseEvent.BUTTON1) {
            // zoom in
            zoomIn (e.getPoint());
            repaint();
         }
         else if (myCursorMode == CursorMode.ZOOM_OUT &&
                  e.getButton() == MouseEvent.BUTTON1) {
            // zoom out
            zoomOut (e.getPoint());
            repaint();
         }
         else if (myInputDataP) // only add knots for input probes
         {
            if (e.getClickCount() == 2) {
               int size = getVectorSize();

               ScreenTransform xform = createScreenTransform();
               double tempTime = xform.pixelToXvalue (e.getPoint().x);
               newKnot = new NumericListKnot (size);
               NumericList list = getNumericList();
               VectorNd vec = new VectorNd (size);
               if (getNumericList().getNumKnots() == 0) {
                  vec.setZero();
               }
               list.interpolate (vec, tempTime);
               newKnot.t = tempTime;
               newKnot.v = vec;
               getNumericList().add (newKnot);
               System.out.println ("adding new knot at" + newKnot.t);
               updateDisplaysWithoutAutoRanging();
            }
         }
      }

      public void mouseEntered (MouseEvent e) {
         switch (myCursorMode) {
            case SELECT: {
               setCursor (Cursor.getDefaultCursor());  
               break;
            }
            case ZOOM_IN: {
               setCursor (zoomInCursor);
               break;
            }
            case ZOOM_OUT: {
               setCursor (zoomOutCursor);
               break;
            }
            case TRANSLATE: {
               setCursor (translateCursor);
               break;
            }
            default: {
               throw new InternalErrorException (
                  "Cursor not defined for CursorMode " + myCursorMode);
            }
         }
      }

      public void mouseExited (MouseEvent e) {
         setCursor (Cursor.getDefaultCursor());
      }

      /**
       * author: andreio cursor changes now if the mouse is moved over the spot
       * where knot is located on the large display
       */

      public void mouseMoved (MouseEvent e) {
         // get the initial location of the mouse press
         initialMousePos = e.getPoint();

         // System.out.println("Initial: " + initialMousePos);

         ScreenTransform xform = createScreenTransform();
         double initTimeInSec = xform.pixelToXvalue (initialMousePos.x);

         NumericListKnot closestKnot =
            getNumericList().findKnotClosest (initTimeInSec);

         // enter the loop only if closest exists, in other words we got the
         // knot and only if we are not currently zooming
         if (closestKnot != null) {
            // changes to add y-scanning into the code June10th
            int closestXPixel = (int)xform.xvalueToPixel (closestKnot.t);

            for (int i = 0; i < closestKnot.v.size(); i++) {
               if ((closestXPixel - initialMousePos.x) <= SELECT_MARGIN &&
                   (closestXPixel - initialMousePos.x) >= -SELECT_MARGIN &&
                   myInputDataP) {
                  break;
               }
            }

            // see if delta-t is small enough to trigger a selection
            if (myCursorMode == CursorMode.SELECT) {
               if ((closestXPixel - initialMousePos.x) <= SELECT_MARGIN &&
                   (closestXPixel - initialMousePos.x) >= -SELECT_MARGIN &&
                   myInputDataP) {
                  setCursor (Cursor.getPredefinedCursor (1));
               }
               else {
                  setCursor (Cursor.getDefaultCursor());
               }
            }
         }// end changes

      }

      // sort out the performed action on the display
      public void actionPerformed (ActionEvent e) {
         String nameOfAction = e.getActionCommand();

         if (nameOfAction == "Step") {
            if (myInputDataP) {
               setInterpolationOrder (Interpolation.Order.Step);
            }
         }
         else if (nameOfAction == "Linear") {
            if (myInputDataP) {
               setInterpolationOrder (Interpolation.Order.Linear);
            }
         }
         else if (nameOfAction == "Cubic") {
            if (myInputDataP) {
               setInterpolationOrder (Interpolation.Order.Cubic);
            }
         }
         else if (nameOfAction == "CubicStep") {
            if (myInputDataP) {
               setInterpolationOrder (Interpolation.Order.CubicStep);
            }
         }
         else if (nameOfAction == "SphericalLinear") {
            if (myInputDataP) {
               System.out.println ("setting");
               setInterpolationOrder (Interpolation.Order.SphericalLinear);
            }
         }
         else if (nameOfAction == "SphericalCubic") {
            if (myInputDataP) {
               setInterpolationOrder (Interpolation.Order.SphericalCubic);
            }
         }
         else if (nameOfAction == "Reset ranges") {
            fitDisplayRange();
         }
         else if (nameOfAction == "Scale data ...") {
            scaleProbeValues();
         }
         else if (nameOfAction == "Show knot points") {
            setKnotsVisible (true);
         }
         else if (nameOfAction == "Hide knot points") {
            setKnotsVisible (false);
         }
         else if (nameOfAction == "Delete knot point") {
            getNumericList().remove (newKnot);
         }
         else if (nameOfAction == "Edit knot point") {
            double oldVal = newKnot.v.get (dirtyIndex);
            String input =
               JOptionPane.showInputDialog (
                  "Please enter value", Double.toString (oldVal));
            if (input != null) {
               try {
                  double newVal = Double.parseDouble (input);
                  newKnot.v.set (dirtyIndex, newVal);
               }
               catch (Exception ex) {
                  System.err.println (ex.getMessage());
               }
            }
         }
         else if (nameOfAction == "Clone display") {
            Window win = 
               SwingUtilities.windowForComponent (NumericProbePanel.this);
            if (win instanceof NumericProbeDisplayLarge) {
               NumericProbeDisplayLarge newDisplay = 
                  new NumericProbeDisplayLarge ((NumericProbeDisplayLarge)win);
               GuiUtils.locateBelow (newDisplay, NumericProbePanel.this);
               newDisplay.setVisible (true);
            }
         }
         else if (nameOfAction == "Export image as ...") {
            exportImageAs();
         }
         else if (nameOfAction == "Edit range and properties ...") {
            if (myPropertyDialog == null) {
               myPropertyDialog = createPropertyDialog("OK");
               myPropertyDialog.addWindowListener (
                  new WindowAdapter() {
                     public void windowClosed (WindowEvent e) {
                        myPropertyDialog = null;
                     }
                  });
               myPropertyDialog.setVisible (true);
            }
         }
         else if (nameOfAction == "Show legend ...") {
            LegendDisplay legend = getLegend();
            if (legend == null) {
               legend = new LegendDisplay (
                  NumericProbePanel.this, myTraceManager);
               setLegend (legend);
            }
            if (!legend.isVisible()) {
               GuiUtils.locateRight (legend, NumericProbePanel.this);
               legend.setVisible (true);
            }
         }
         else if (nameOfAction == "Hide legend") {
            LegendDisplay legend = getLegend();
            if (legend != null) {
               legend.dispose();
               setLegend (null);
            }
         }
         updateDisplaysWithoutAutoRanging();
      }

   }

   private PropertyDialog createPropertyDialog (String controlStr) {
      PropertyDialog dialog =
         new PropertyDialog (
            "Display range and properties", new PropertyPanel(), controlStr);

      if (isLargeDisplay()) {
         dialog.addWidget ("x range", this, "xRange");
      }
      LabeledControl yrangeWidget =
         (LabeledControl)dialog.addWidget ("y range", this, "yRange");
      dialog.addWidget (this, "autoRanging");
      if (isLargeDisplay()) {
         dialog.addWidget (this, "drawGrid");
         dialog.addWidget (this, "tickLength");
         dialog.addWidget (this, "lineWidth");
      }
      dialog.addWidget (this, "knotsVisible");
      dialog.addWidget (this, "knotSize");

      yrangeWidget.addValueChangeListener (
         new ValueChangeListener() {
            public void valueChange (ValueChangeEvent e) {
               setAutoRanging (false);
            }
         });

      dialog.pack();
      dialog.locateRight (this);
      dialog.addGlobalValueChangeListener (this);

      //registerDialog (dialog);
      return dialog;
   }

   public void exportImageAs () {
      ArrayList<ExtensionFileFilter> extraFilters = new ArrayList<>();
      extraFilters.add (
         new ExtensionFileFilter ("Encapsulated PostScript (*.eps)", "eps"));
      extraFilters.add (
         new ExtensionFileFilter ("Scalable vector graphics (*.svg)", "svg"));
      ImageFileChooser chooser =
         new ImageFileChooser (myExportFile, extraFilters);
      if (chooser.showValidatedDialog (this, "Export as") ==
          JFileChooser.APPROVE_OPTION) {
         File file = chooser.getSelectedFileWithExtension();
         boolean confirm = true;
         if (file.exists()) {
            confirm = GuiUtils.confirmOverwrite (this, file);
         }
         if (confirm) { 
            String format = chooser.getSelectedFileExtension();
            if (chooser.isImageFile (file)) {
               try {
                  BufferedImage img =
                     new BufferedImage(
                        getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                  Graphics g = img.getGraphics();
                  g.setColor(getForeground());
                  g.setFont(getFont());
                  paintAll(g);
                  if (!ImageIO.write (img, format, file)) {
                     throw new IOException (
                        "writer not found for file type '"+format+"'");
                  }
                  myExportFile = file;
               }
               catch (Exception e) {
                  GuiUtils.showError (
                     this, "Error saving image: " + e.getMessage());
               }
            }
            else if (format.equalsIgnoreCase ("svg")) {
               try {
                  SvgRenderer r = new SvgRenderer (file);
                  drawPlot (r, /*large=*/true);
                  myExportFile = file;
               }
               catch (Exception e) {
                  GuiUtils.showError (
                     this, "Error saving image: " + e.getMessage());
               }
            }
            else if (format.equalsIgnoreCase ("ps") || 
                     format.equalsIgnoreCase ("eps")) {
               try {
                  PostScriptRenderer r = new PostScriptRenderer (file);
                  drawPlot (r, /*large=*/true);
                  myExportFile = file;
               }
               catch (IOException e) {
                  GuiUtils.showError (
                     this, "Error saving image: " + e.getMessage());
               }
            }
            else {
               throw new InternalErrorException (
                  "Export not supported for for file type ." + format);
            }
         }
      }
   }
   
   public void valueChange (ValueChangeEvent e) {
      repaint();
   }

   public NumericProbePanel (Probe probe) {
      if (probe instanceof NumericProbeBase) {
         myProbe = (NumericProbeBase)probe;
         myNumericList = myProbe.getNumericList();
         myTraceManager = myProbe.getTraceManager();
         myInputDataP = myProbe.isInput();
         myKnotsVisible = myInputDataP;
      }
      else {
         throw new IllegalArgumentException ("probe is of not a numeric type");
      }
      build();
   }
   
   public NumericProbePanel (NumericProbePanel panel) {
      myProbe = null;
      myNumericList = (NumericList)panel.myNumericList.clone();
      myNumericList.setInterpolation (panel.getInterpolation());
      myTraceManager = panel.myTraceManager.copy();
      myStartTime = panel.getStartTime();
      myStopTime = panel.getStopTime();
      myScale = panel.getScale();
      myInputDataP = panel.myInputDataP;
      myDrawGrid = panel.myDrawGrid;
      myAutoRangingP = panel.myAutoRangingP;
      myTickLength = panel.myTickLength;
      myLineWidth = panel.myLineWidth;
      myKnotSize = panel.myKnotSize;
      myKnotsVisible = panel.myKnotsVisible;
      build();
      if (panel.isLargeDisplay()) {
         setLargeDisplay (true);
      }
      setXRange (panel.getXRange());
      setYRange (panel.getYRange());
      setDisplaySize (panel.getWidth(), panel.getHeight());
   }
   
   protected void build() {
      fieldInitialization();
      appearanceInitialization();
      listenerInitialization();
      zoomCursorInitialization();
   }

   public void resetDrawOrder (int size) {
      myTraceManager.resetTraceOrder();
      updateDisplaysWithoutAutoRanging();
   }


   public void resetColors() {
      myTraceManager.resetTraceColors();
      updateDisplaysWithoutAutoRanging();
   }

   public void setLineColor (int index, Color color) {
      myTraceManager.setTraceColor (index, color);
      updateDisplaysWithoutAutoRanging();
   }

   public boolean isLargeDisplay() {
      return myLargeDisplay;
   }

   public void setLargeDisplay (boolean isLargeDisplay) {
      this.myLargeDisplay = isLargeDisplay;
      setBackground (Color.WHITE);
      useVirtualTime = isLargeDisplay;
      setLineWidth (2);
      setKnotSize (7);
   }

   public int getVectorSize() {
      if (myProbe != null) {
         return myProbe.getVsize();
      }
      else {
         return myNumericList.getVectorSize();
      }
   }

   public void dispose() {
      if (myPropertyDialog != null) {
         myPropertyDialog.dispose();
         myPropertyDialog = null;
      }
      if (myLegendDisplay != null) {
         myLegendDisplay.dispose();
         myLegendDisplay = null;
      }
   }
}
