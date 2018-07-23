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
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Iterator;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputListener;

import maspack.interpolation.Interpolation;
import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.matrix.VectorNd;
import maspack.widgets.GuiUtils;
import artisynth.core.driver.Main;
import artisynth.core.probes.NumericProbeBase;
import artisynth.core.probes.PlotTraceInfo;
import artisynth.core.probes.Probe;
import artisynth.core.util.ArtisynthPath;

/**
 * @author Chad Added to display a thin line for outputprobes, and not to
 * display datapoints per John's request
 * 
 * @version 1.0 revised March 06/2006
 * @version 2.0 reimplemented May 10/2006
 */

public class NumericProbePanel extends JPanel {
   // private NumericProbeDisplayLarge largeDisplayToUpdate = null;

   Double xTime = null, yTime = null;
   Integer xPosition = null, yPosition = null;
   VectorNd yVector = new VectorNd();
   int[] drawOrder;

   public static Color DARK_GREEN = new Color (0f, 0.5f, 0f);
   public static Color DarkOliveGreen = new Color (85, 107, 47);
   public static Color IndianRed = new Color (205, 92, 92);
   public static Color DarkOrange = new Color (255, 140, 0);
   public static Color BlueViolet = new Color (138, 43, 226);
   public static Color NavajoWhite = new Color (255, 222, 173);
   public static Color PeachPuff = new Color (255, 218, 185);

   boolean largeDisplay = false;
   // Virtual time means that the time scale is in probe virtual coordinates.
   // Otherwise, it is in timeline coordinates.
   boolean useVirtualTime = false;

   // colors used for plotting
   public static Color[] colorList =
      { Color.RED, DARK_GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA,
       DarkOrange, Color.PINK, BlueViolet, NavajoWhite, Color.GRAY,
       DarkOliveGreen, IndianRed, DarkOrange };

   public Color[] myColorList;

   // this is the probe obejct
   // the probe object
   NumericProbeBase myProbe = null;
   //ProbeInfo myProbeInfo = null;

   // cache this value to see if we need to redraw when interpolation changes
   Interpolation.Order myLastInterpolationOrder = null;

   protected Boolean[] skipIndicies;

   // ===========================================================
   // swing components and handlers
   // ===========================================================

   protected DisplayListener displayListener;
   protected WheelListener wheelListener;
   protected boolean drawKnotsP;
   private NumericProbeRangeSelectorDialog newRangeSelector;

   // ===========================================================
   // variables needed for editing
   // ===========================================================

   protected NumericListKnot coincidentKnot;
   protected Point initialMousePos;
   protected Point tempCoor;
   protected Point finalCoor;
   protected boolean editingP;
   protected NumericListKnot oldKnot;
   protected NumericListKnot newKnot;
   protected int dirtyIndex;

   // ===========================================================
   // variables needed for plotting
   // ===========================================================

   protected boolean myAutoRangingP = true;
   //protected long ticksPerPixel;
   protected double secondsPerPixel;
   protected double yValuePerPixel;
   private double minYRange, maxYRange;
   private double minXRange, maxXRange;

   protected boolean displayOutputProbeDataPoints = false;
   protected static final int SELECT_MARGIN = 3;
   protected static final int DRAG_MARGIN = 3;
   protected static final long serialVersionUID = 0xdeadbeef;

   // ===========================================================
   // used for zooming and moving the display, only one can be true at a time
   // ===========================================================
   private Cursor zoomInCursor, zoomOutCursor, moveDisplayCursor;
   private Point zoomSelectPoint; // the initial point of the select rectangle
   private Rectangle zoomSelectRect = null;
   protected boolean zoomIn, zoomOut, moveDisplay;
   private Point previousPoint = null; // the previous point that was dragged to
   private int zoomLevel = 0;
   private int rangeLevel = 0;
   private double[] defaultRange = new double[2];

   // the first spacing value that is set for the display
   private double initialXSpacing = 0;
   private double initialYSpacing = 0;
   private boolean setRangeManually = false;

   // Changed
   // Replaced 8 line function into 1 liner - optimization
   protected NumericList getNumericList() {
      return (myProbe != null) ? myProbe.getNumericList() : null;
   }

   public boolean isAutoRanging() {
      return myAutoRangingP;
   }

   public void setAutoRanging (boolean enable) {
      myAutoRangingP = enable;
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

   public void toggleZoomIn() {
      if (zoomIn) {
         zoomIn = false;
      }
      else {
         zoomIn = true;
         zoomOut = false;
         moveDisplay = false;
      }
   }

   public void toggleZoomOut() {
      if (zoomOut) {
         zoomOut = false;
      }
      else {
         zoomOut = true;
         zoomIn = false;
         moveDisplay = false;
      }
   }

   public void toggleMoveDisplay() {
      if (moveDisplay) {
         moveDisplay = false;
      }
      else {
         moveDisplay = true;
         zoomIn = false;
         zoomOut = false;
      }
   }

   public void setAutoRange() {
      double[] autoRange = getAutoRange();
      setDefaultRange (autoRange[0], autoRange[1]);
   }

   public double[] getAutoRange() {
      return myProbe.getDefaultDisplayRange();
   }

   /**
    * Sets the default range to the desired value. The default range is the range
    * the probe display returns to after zooming. Also sets the default range of
    * the small probe display through the probe info.
    * 
    * @param min minimum value
    * @param max maximum value
    */
   public void setDefaultRange (double min, double max) {
      defaultRange[0] = min;
      defaultRange[1] = max;
   }

   public double[] getDefaultRange() {
      return defaultRange;
   }

   public double[] getDefaultDomain() {
      double min, max;
      if (useVirtualTime) {
         min = 0;
         max =
            (myProbe.getStopTime() - myProbe.getStartTime())
            / myProbe.getScale();
      }
      else {
         min = myProbe.getStartTime();
         max = myProbe.getStopTime();
      }
      return new double[] { min, max };
   }

   public void setDefaultDomain() {
      double[] domain = getDefaultDomain();
      setDisplayDomain (domain[0], domain[1]);
   }

   public void setDisplayRange (double min, double max) {
      myAutoRangingP = false;
      minYRange = min;
      maxYRange = max;
   }
   
   public void setDisplayRangeManually (double min, double max) {
      setDisplayRange (min, max);
      setRangeManually  = true;
   }

   public double[] getDisplayRange() {
      return new double[] { minYRange, maxYRange };
   }

   public void setDisplayDomain (double min, double max) {
      minXRange = min;
      maxXRange = max;
   }

   public double[] getDisplayDomain() {
      return new double[] { minXRange, maxXRange };
   }

   public void resetDisplay() {
      zoomLevel = 0;
      rangeLevel = 0;
      double[] range = getDefaultRange();
      setDisplayRange (range[0], range[1]);
      setDefaultDomain();
      repaint();
   }

   public void determineTimePerPixel() {
//      ticksPerPixel =
//         ((long)((maxXRange-minXRange)/TimeBase.ticksToSeconds (getWidth())));
//      if (!useVirtualTime) {
//         ticksPerPixel /= myProbe.getScale();
//      }
//      ticksPerPixel = (ticksPerPixel <= 0 ? 1 : ticksPerPixel);
      secondsPerPixel = (maxXRange-minXRange)/getWidth();
      if (!useVirtualTime) {
         secondsPerPixel /= myProbe.getScale();
      }      
   }

   public void determineYValuePerPixel()// boolean isAutoScale)
   {
      yValuePerPixel = (maxYRange - minYRange) / getHeight();
   }

   // repaint required to draw timeline cursor in ProbeDisplayArea JPanel
   public void repaint() {
      if (myAutoRangingP) {
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

   protected void fieldInitialization() {

      setAutoRange();
      double[] defaultDisplayRange = getDefaultRange();
      double[] defaultDomain = getDefaultDomain();

      minYRange = defaultDisplayRange[0];
      maxYRange = defaultDisplayRange[1];

      minXRange = defaultDomain[0];
      maxXRange = defaultDomain[1];

      defaultRange = new double[] { minYRange, maxYRange };

      editingP = false;
      drawKnotsP = true;
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
      moveDisplayCursor = createCursor ("Hand.gif", "move display");
   }

   /**
    * author: Chad version: 1.0
    * 
    * Alpha Code for Changing the display range of a probe is still under
    * development.
    */
   protected void setDisplayRange() {
      newRangeSelector = new NumericProbeRangeSelectorDialog (this);
      newRangeSelector.setAlwaysOnTop (true);
      newRangeSelector.setTitle ("Probe range selector");

      // setDataBuffer(true);
      repaint();
   }

   protected void fitDisplayRange() {
      double[] range = myProbe.getRange();
      setDisplayRange (range[0], range[1]);
      // setDataBuffer(true);
      repaint();
   }

   /**
    * version 2.0:
    * 
    * The previous version of Numeric probe diaplay used the time of a
    * particular knot, to find out where in the graph coordinates this knot
    * lies, this revision of the code does the opposite. Instead for every x
    * pixel we have in the viewing area, a time will be calculated based on the
    * x coordinate value. This time will then be used to interpolate the y value
    * based upon t (using various methods of interpolation) This method has
    * three main advantages. 1. While there may be more overhead associated with
    * sparcely knotted probes, the resolution of the datapoints will in fact be
    * higher. 2. Under the previous model if the knots were densely packed, the
    * xBuffer constructed would contain more x values, then there are x pixels,
    * resulting in overdrawing pixels. 3. The interpolation of the model is now
    * done by the numeric list, and not the GUI display components allowing for
    * proper abstraction of functionality.
    * 
    */

   private void drawZeroLine (Graphics g) {
      if (!myProbe.isInput()) {
         // System.out.println("printing zero line on output probe");
      }

      int zero;

      // plot zero line
      if (maxYRange <= 0) {
         // when both max and min < 0
         zero = 0;
      }
      else if (minYRange >= 0) {
         // when both max and min > 0
         zero = getHeight();
      }
      else {
         // when max > 0 and min < 0
         zero = (int)((maxYRange) / yValuePerPixel);
      }

      g.setColor (Color.LIGHT_GRAY);
      g.drawLine (0, zero, this.getWidth(), zero);
   }

   int fontHeight = 10;

   /**
    * Repaint if probe property changes require it.
    */
   public void repaintForPropertyChanges() {
      if (myProbe.getInterpolationOrder() != myLastInterpolationOrder) {
         repaint();
      }
   }

   public void adjustRangeIfNecessary() {
      double[] minMax = myProbe.getMinMaxValues();
      double range = minMax[1] - minMax[0];

      if (myProbe.isEmpty() || range < 1e-13) {
         minMax[0] = -1;
         minMax[1] = 1;
         range = 2;
      }
      else {
         minMax[0] -= 0.1 * range;
         minMax[1] += 0.1 * range;
      }
      double maxY = maxYRange;
      double minY = minYRange;
      if (maxY < minMax[1]) {
         maxY = minMax[1] + 0.25 * range;
      }
      if (minY > minMax[0]) {
         minY = minMax[0] - 0.25 * range;
      }
      minYRange = minY;
      maxYRange = maxY;
   }

   public void paint (Graphics g) {
      determineTimePerPixel();
      determineYValuePerPixel();

      g.setFont (new Font (null, 0, fontHeight));

      if (largeDisplay) {
         drawGrids (g);
      }
      else {
         drawZeroLine (g);
      }

      // ======================================================
      // Actual data plotting
      if (myProbe != null) {
         // Draw plot lines
         drawPlotLines (g);
         if (myProbe.isInput() || displayOutputProbeDataPoints) {
            // Draw Knot Points
            drawKnots (g);
         }
      }

      // draw the zoom selecting rectangle if it exists
      g.setColor (Color.GRAY);

      if (zoomSelectRect != null) {
         g.drawRect (
            zoomSelectRect.x, zoomSelectRect.y, zoomSelectRect.width,
            zoomSelectRect.height);
      }

   }

   private void drawPlotLines (Graphics g) {
      Graphics2D g2 = (Graphics2D)g;

      NumericList list = myProbe.getNumericList();

      if (list != null) {
         int numXPixels = this.getWidth();
         NumericListKnot tempKnot;
         int yVectorSize = 0;
         int[] xInterpolationBuffer = null;
         int[][] yInterpolationBuffer = null;

         tempKnot = null;

         // Fix by Ian for interpolation before 1st knot point
         yVectorSize = list.getVectorSize();
         // VectorNd yVector = new VectorNd(yVectorSize);
         yVector.setSize (yVectorSize);

         xInterpolationBuffer = new int[numXPixels];
         yInterpolationBuffer = new int[yVectorSize][numXPixels + 1];

         double timePerPixel = (maxXRange - minXRange) / getWidth();

         tempKnot = null;
         double t;

         // used only if we are not working in virtual time
         double probeScale = myProbe.getScale();
         double probeStartTime =
            myProbe.getStartTime();

         // Create line plot
         for (int index = 0; index < numXPixels; index++) {
            t = minXRange + timePerPixel * index;
            if (!useVirtualTime) { // convert t back to virtual time
               t = (t - probeStartTime) / probeScale;
            }

            // we have to offset the time increment by the minimum x range
            // because we are not starting from zero
            tempKnot =
               list.interpolate (
                  yVector, t, myProbe.getInterpolation(), tempKnot);

            xInterpolationBuffer[index] = index;

            for (int k = 0; k < yVectorSize; k++) {
               if (k < yVector.size()) {
                  yInterpolationBuffer[k][index] =
                     (int)-((yVector.get (k) - maxYRange) / yValuePerPixel);
               }
            }
         }

         if (largeDisplay) {
            g2.setStroke (new BasicStroke (2));
         }

         // Draw line plot
         for (int k = yVectorSize - 1; k >= 0; k--) {

            int idx = myProbe.getOrderedTraceIndex (k);
            PlotTraceInfo pti = myProbe.getPlotTraceInfo (idx);
            if (pti.isVisible()) {
               g2.setColor (pti.getColor());
               g2.drawPolyline (
                  xInterpolationBuffer, yInterpolationBuffer[idx],
                  numXPixels);
            }
         }
      }
      myLastInterpolationOrder = myProbe.getInterpolationOrder();
   }

   private void drawKnots (Graphics g) {
      if (drawKnotsP) {
         double timePerPixel = (maxXRange - minXRange) / getWidth();
         double yFactor = 1.0 / yValuePerPixel;

         NumericList list = myProbe.getNumericList();
         Iterator<NumericListKnot> it = list.iterator();

         // used only if we are not working in virtual time
         double probeScale = myProbe.getScale();
         double probeStartTime =
            myProbe.getStartTime();

         while (it.hasNext()) {
            NumericListKnot knot = it.next();
            double t = knot.t;
            if (!useVirtualTime) { // convert t to timeline time
               t = t * probeScale + probeStartTime;
            }
            int x = (int)((t - minXRange) / timePerPixel);
            for (int v = knot.v.size() - 1; v >= 0; v--) {

               int idx = myProbe.getOrderedTraceIndex (v);
               PlotTraceInfo pti = myProbe.getPlotTraceInfo (idx);
               if (pti.isVisible()){
                  g.setColor (pti.getColor());
                  double y = -(knot.v.get (idx) - maxYRange) * yFactor;
                  if (largeDisplay)
                     g.fillOval (x - 4, (int)y - 4, 8, 8);
                  else
                     g.fillRect (x - 2, (int)y - 2, 5, 5);
               }
            }
         }
      }
   }

   static private double[] units = new double[] { 5, 2, 1 };
   static private int maxPixels = 45;

   private double getSpacing (double valuePerPixel) {
      double maxUnits = maxPixels * valuePerPixel;

      double exp = Math.floor (Math.log10 (maxUnits));
      double base = Math.pow (10, exp);

      int u;
      for (u = 0; u < units.length - 1; u++)
         if (base * units[u] < maxUnits)
            break;
      double spacing = base * units[u];

      return spacing;
   }
   
   public static double round (double d, int decimalPlace){
      BigDecimal bd = new BigDecimal (Double.toString (d));
      bd = bd.setScale (decimalPlace, BigDecimal.ROUND_HALF_UP);
      System.out.println( d + " :: " + decimalPlace + " :: " + bd.doubleValue ());
      return bd.doubleValue();
    }


   // draw the grid lines for the large display
   private void drawGrids (Graphics g) {
      double yFactor = 1.0 / yValuePerPixel;
      double ySpacing = getSpacing (yValuePerPixel);
      double yPos = ySpacing * Math.ceil (minYRange / ySpacing);

//      double xFactor = 1.0 / TimeBase.ticksToSeconds (ticksPerPixel);
//      double xSpacing = getSpacing (TimeBase.ticksToSeconds (ticksPerPixel));
      double xFactor = 1.0/secondsPerPixel;
      double xSpacing = getSpacing (secondsPerPixel);
      
      if (initialXSpacing == 0) {
         // if the initialXSpacing value has NOT been set yet
         initialXSpacing = xSpacing;
      }

      if (initialYSpacing == 0) {
         // if the initialYSpacing value has NOT been set yet
         initialYSpacing = ySpacing;
      }

      // used to setup number printing so that 0 is always shown
      int lines = Math.abs ((int)roundDouble ((yPos / ySpacing)) % 2);

      String yPosStr = "" + yPos;
      String ySpacingStr = "" + ySpacing;
      int round = Math.max (yPosStr.substring (yPosStr.indexOf (".") + 1).length (),
         ySpacingStr.substring (ySpacingStr.indexOf (".") + 1).length ()); // TODO rounding?
      
      for (int i = 0; yPos < maxYRange; yPos += ySpacing, i++) {
         int yi = (int)(-(yPos - maxYRange) * yFactor);

         g.setColor (Color.LIGHT_GRAY);
         g.drawLine (0, yi, getWidth(), yi);

         if (i % 2 == lines) {
            String str = Double.toString (roundDouble (yPos)/*round (yPos, round)*/);
            Rectangle2D rect = g.getFontMetrics().getStringBounds (str, g);
            int h = (int)rect.getHeight();
            int w = (int)rect.getWidth();
            int xs = 1;
            int ys = yi + h / 2;

            g.setColor (getBackground());
            g.fillRect (xs - 1, ys - h, w + 2, h + 2);
            g.setColor (Color.BLACK);
            g.drawString (str, xs, ys);
         }
      }

      double xPos = 0;
      double xOffset = xSpacing * Math.ceil (minXRange / xSpacing);

      if (!largeDisplay) {
         xSpacing = xSpacing * myProbe.getScale();
         xFactor = xFactor / myProbe.getScale();
      }
      
      for (int i = 0; xPos < maxXRange; xPos += xSpacing, i++) {
         int xi = (int)(xPos * xFactor);
         g.setColor (Color.LIGHT_GRAY);
         g.drawLine (xi, 0, xi, getHeight());
         
         if (i % 2 == 0 && i > 0) {
            String str = Double.toString (roundDouble (xPos + xOffset));
            Rectangle2D rect = g.getFontMetrics().getStringBounds (str, g);
            int h = (int)rect.getHeight();
            int w = (int)rect.getWidth();
            int xs = xi - w / 2;
            int ys = getHeight() - 1;

            g.setColor (getBackground());
            g.fillRect (xs - 1, ys - h, w + 2, h + 2);
            g.setColor (Color.BLACK);
            g.drawString (str, xs, ys);
         }
      }
   }

   private static double displayDigits = 8;

   public double roundDouble (double value) {
      double exp = Math.floor (Math.log10 (Math.abs (value)));
      double base = Math.pow (10, displayDigits - exp);
      
      return Math.round (value * base) / base;
   }

   
   public void swapDrawIndicies (int a, int b) {
      PlotTraceInfo ptiA = myProbe.getPlotTraceInfo (a);
      PlotTraceInfo ptiB = myProbe.getPlotTraceInfo (b);
      myProbe.swapPlotTraceOrder (ptiA, ptiB);
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
         myProbe.scaleNumericList (scaleFactor);
      }
   }

   /**
    * Zoom in on a particular point on the numeric probe display.
    * 
    * @param zoomPoint
    * The point to zoom in on.
    */
   public void zoomIn (Point zoomPoint) {
      // determine what the increment level is (divide by 2, 2, 2.5)
      zoomLevel++;
      double scalingFactor = 2;
      if (zoomLevel % 3 == 0) {
         scalingFactor = 2.5;
      }

      // zoom in on the y range
      double[] curDisplayRange = getDisplayRange();
      double[] defaultRange = getDefaultRange();

      double rangeSize =
         (curDisplayRange[1] - curDisplayRange[0]) / scalingFactor;
      double yCenterPoint = maxYRange - (zoomPoint.getY() * yValuePerPixel);
      double newYMin = yCenterPoint - (rangeSize / 2);
      double newYMax = yCenterPoint + (rangeSize / 2);

      if (newYMin < defaultRange[0]) {
         // the new y minimum is outside the range, default the range
         newYMin = defaultRange[0];
         newYMax = newYMin + rangeSize;
      }
      else if (newYMax > defaultRange[1]) {
         // the new y maximum is outside the range, default the range
         newYMax = defaultRange[1];
         newYMin = newYMax - rangeSize;
      }

      setDisplayRange (newYMin, newYMax);

      // zoom in on the x domain
      double[] curDisplayDomain = getDisplayDomain();
      double[] defaultDomain = getDefaultDomain();

      double domainSize =
         (curDisplayDomain[1] - curDisplayDomain[0]) / scalingFactor;
//      double xCenterPoint =
//         minXRange
//         + (zoomPoint.getX() * TimeBase.ticksToSeconds (ticksPerPixel));
      double xCenterPoint = minXRange + (zoomPoint.getX() * secondsPerPixel);
      double newXMin = xCenterPoint - (domainSize / 2);
      double newXMax = xCenterPoint + (domainSize / 2);

      if (newXMin < defaultDomain[0]) {
         newXMin = defaultDomain[0];
         newXMax = newXMin + domainSize;
      }
      else if (newXMax > defaultDomain[1]) {
         newXMax = defaultDomain[1];
         newXMin = newXMax - domainSize;
      }

      setDisplayDomain (newXMin, newXMax);
   }

   /**
    * Zoom out on a particular point on the numeric probe display. Only zoom out
    * as far as the original display range.
    * 
    * @param zoomPoint
    * The point to zoom out on.
    */
   public void zoomOut (Point zoomPoint) {
      // determine what the increment level is (divide by 2, 2, 2.5)
      zoomLevel--;
      double scalingFactor = 2;
      if (zoomLevel % 3 == 0) {
         scalingFactor = 2.5;
      }

      // zoom out on the y range
      double[] curDisplayRange = getDisplayRange();
      double[] defaultRange = getDefaultRange();
      double defaultRangeSize = defaultRange[1] - defaultRange[0];

      double rangeSize =
         (curDisplayRange[1] - curDisplayRange[0]) * scalingFactor;
      double yCenterPoint = maxYRange - (zoomPoint.getY() * yValuePerPixel);
      double newYMin = yCenterPoint - (rangeSize / 2);
      double newYMax = yCenterPoint + (rangeSize / 2);

      if (newYMin < defaultRange[0] && newYMax <= defaultRange[1] &&
          rangeSize <= defaultRangeSize) {
         // just the y min is out of range and the new display height is not
         // greater than the default
         newYMin = defaultRange[0];
         newYMax = newYMin + rangeSize;
      }
      else if (newYMin >= defaultRange[0] && newYMax > defaultRange[1] &&
               rangeSize <= defaultRangeSize) {
         // just the y max is out of range and the new display height is not
         // greater than the default
         newYMax = defaultRange[1];
         newYMin = newYMax - rangeSize;
      }
      else if (newYMin < defaultRange[0] || newYMax > defaultRange[1]) {
         // either the y min or the y max or both are out of range
         newYMin = defaultRange[0];
         newYMax = defaultRange[1];
      }

      if (curDisplayRange[0] == defaultRange[0] && 
          curDisplayRange[1] == defaultRange[1]) {
         // if we didn't zoom because the display is already at the default
         // Range then reset the zoomed in value by one
         zoomLevel++;
      }

      setDisplayRange (newYMin, newYMax);

      // zoom out on the x domain
      double[] curDisplayDomain = getDisplayDomain();
      double[] defaultDomain = getDefaultDomain();
      double defaultDomainSize = defaultDomain[1] - defaultDomain[0];

      double domainSize =
         (curDisplayDomain[1] - curDisplayDomain[0]) * scalingFactor;
      double xCenterPoint = minXRange + (zoomPoint.getX() * secondsPerPixel);
      double newXMin = xCenterPoint - (domainSize / 2);
      double newXMax = xCenterPoint + (domainSize / 2);

      if (newXMin < defaultDomain[0] && newXMax <= defaultDomain[1] &&
          domainSize <= defaultDomainSize) {
         // just the x min is out of range and the new display width is not
         // greater than the default
         newXMin = defaultDomain[0];
         newXMax = newXMin + domainSize;
      }
      else if (newXMin >= defaultDomain[0] && newXMax > defaultDomain[1] &&
               domainSize <= defaultDomainSize) {
         newXMax = defaultDomain[1];
         newXMin = newXMax - domainSize;
      }
      else if (newXMin < defaultDomain[0] || newXMax > defaultDomain[1]) {
         newXMin = defaultDomain[0];
         newXMax = defaultDomain[1];
      }

      setDisplayDomain (newXMin, newXMax);
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
      
      //double xDist = TimeBase.ticksToSeconds (xDistance * ticksPerPixel);
      double xDist = xDistance * secondsPerPixel;
      double yDist = yDistance * yValuePerPixel;

      // get the maximum values of the display so we dont display past these
      // points
      double[] maxDomain = getDefaultDomain();
      double[] maxRange = getAutoRange();

      if ((minXRange + xDist) > maxDomain[0] &&
          (maxXRange + xDist) < maxDomain[1]) {
         double newXMin = minXRange + xDist;
         double newXMax = maxXRange + xDist;
         setDisplayDomain (newXMin, newXMax);
         repaint();
      }

      if ((minYRange + yDist) > maxRange[0] &&
          (maxYRange + yDist) < maxRange[1]) {
         double newYMin = minYRange + yDist;
         double newYMax = maxYRange + yDist;
         setDisplayRange (newYMin, newYMax);
         repaint();
      }
   }

   /**
    * Increase the range that is being viewed without any zooming constraints.
    * This is different from zooming because we can increase the range past the
    * original display range.
    * 
    */
   public void increaseRange() {
      rangeLevel++;
      double scalingFactor = 2;
      if (rangeLevel % 3 == 0) {
         scalingFactor = 2.5;
      }

      // get the center point of the probe

      double[] curDisplayRange = getDisplayRange();
      
      double rangeSize = (curDisplayRange[1] - curDisplayRange[0]);      
      double yCenterPoint = maxYRange - (rangeSize / 2);
      
      double newYMin = yCenterPoint - (rangeSize * scalingFactor / 2);
      double newYMax = yCenterPoint + (rangeSize * scalingFactor / 2);
      
      if (setRangeManually) {
         setRangeManually = false;
         
         double[] autoRange = getAutoRange();
         double newRangeSize = autoRange[1] - autoRange[0];
         int tmpRangeLevel = 1;  
         
         if (newRangeSize > rangeSize) {
            
            while (newRangeSize > rangeSize) {
               rangeLevel = tmpRangeLevel--;
               double tmpScalingFactor = 2;
               if (tmpRangeLevel % 3 == 0) {
                  tmpScalingFactor = 2.5;
               }
               
               double tmpYMin = yCenterPoint - (newRangeSize / tmpScalingFactor / 2);
               double tmpYMax = yCenterPoint + (newRangeSize / tmpScalingFactor / 2);
               
               if (tmpYMax - tmpYMin < rangeSize) {
                  break;
               }
               
               newYMin = tmpYMin;
               newYMax = tmpYMax;
               
               newRangeSize = newYMax - newYMin;
            }
         }
         else if (newRangeSize < rangeSize) {
            
            while (newRangeSize < rangeSize) {
               rangeLevel = ++tmpRangeLevel;
               double tmpScalingFactor = 2;
               if (tmpRangeLevel % 3 == 0) {
                  tmpScalingFactor = 2.5;
               }
               
               newYMin = yCenterPoint - (newRangeSize * tmpScalingFactor / 2);
               newYMax = yCenterPoint + (newRangeSize * tmpScalingFactor / 2);
               
               newRangeSize = newYMax - newYMin;
            }
         }
      }

      setDisplayRange (newYMin, newYMax);

      // only change the default range if the display is not currently zoomed
      // in or out
      if (zoomLevel == 0) {
         setDefaultRange (newYMin, newYMax);
      }
   }

   /**
    * Decrease the range that is being viewed. When the range is increased this
    * function undoes those increases.
    * 
    */
   public void decreaseRange() {
      rangeLevel--;
      double scalingFactor = 2;
      if (rangeLevel % 3 == 2) {
         scalingFactor = 2.5;
      }

      // zoom in on the y range
      double[] curDisplayRange = getDisplayRange();

      double rangeSize = (curDisplayRange[1] - curDisplayRange[0]);
      double yCenterPoint = maxYRange - (rangeSize / 2);
      
      double newYMin = yCenterPoint - (rangeSize / scalingFactor / 2);
      double newYMax = yCenterPoint + (rangeSize / scalingFactor / 2);
      
      if (setRangeManually) {
         setRangeManually = false;
         
         double[] autoRange = getAutoRange();
         double newRangeSize = autoRange[1] - autoRange[0];
         int tmpRangeLevel = 1;  
         
         if (newRangeSize > rangeSize) {
            
            while (newRangeSize > rangeSize) {
               rangeLevel = --tmpRangeLevel;
               double tmpScalingFactor = 2;
               if (tmpRangeLevel % 3 == 2) {
                  tmpScalingFactor = 2.5;
               }
               
               newYMin = yCenterPoint - (newRangeSize / tmpScalingFactor / 2);
               newYMax = yCenterPoint + (newRangeSize / tmpScalingFactor / 2);
               
               newRangeSize = newYMax - newYMin;
            }
         }
         else if (newRangeSize < rangeSize) {
            
            while (newRangeSize < rangeSize) {
               rangeLevel = tmpRangeLevel++;
               double tmpScalingFactor = 2;
               if (tmpRangeLevel % 3 == 2) {
                  tmpScalingFactor = 2.5;
               }
               
               double tmpYMin = yCenterPoint - (newRangeSize * tmpScalingFactor / 2);
               double tmpYMax = yCenterPoint + (newRangeSize * tmpScalingFactor / 2);
               
               if (tmpYMax - tmpYMin > rangeSize) {
                  break;
               }
               
               newYMin = tmpYMin;
               newYMax = tmpYMax;
               
               newRangeSize = newYMax - newYMin;
            }
         }
      }

      setDisplayRange (newYMin, newYMax);

      // only change the default range if the display is not currently zoomed
      // in or out
      if (zoomLevel == 0) {
         setDefaultRange (newYMin, newYMax);
      }
   }

   protected class WheelListener implements MouseWheelListener {

      public void mouseWheelMoved (MouseWheelEvent e) {
         if (zoomIn || zoomOut) {
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

   /**
    * Returns the virtual time corresponding to a particular x pixel value.
    */
   public double xpixelToVirtualTime (int x) {
//      long ticks = x * ticksPerPixel;
//      if (!useVirtualTime) {
//         ticks -= myProbe.getStartTimeTicks();
//      }
//      return TimeBase.ticksToSeconds (ticks) + minXRange;
      double secs = x * secondsPerPixel;
      if (!useVirtualTime) {
         secs -= myProbe.getStartTime();
      }
      return secs + minXRange;
   }

   /**
    * Returns the x pixel value corresponding to a particular virtual time.
    */
   public int virtualTimeToXpixel (double t) {
//      long ticks = TimeBase.secondsToTicks (t - minXRange);
//      if (!useVirtualTime) {
//         ticks += myProbe.getStartTimeTicks();
//      }
//      return (int)(ticks / ticksPerPixel);
      double secs = t - minXRange;
      if (!useVirtualTime) {
         secs += myProbe.getStartTime();
      }
      return (int)(secs / secondsPerPixel);
   }

   protected class DisplayListener implements MouseInputListener,
   ActionListener {
      
      
      private NumericListKnot checkSelection(Point initialMousePos) {
         boolean found = false;
         double initTimeInSec = xpixelToVirtualTime (initialMousePos.x);
         NumericListKnot closestKnot =
            getNumericList().findKnotClosest (initTimeInSec);

         // enter the loop only if closest exists, in other words we got the
         // knot
         if (closestKnot != null) {
            int closestXPixel = virtualTimeToXpixel (closestKnot.t);

            // see if delta-t is small enough to trigger a selection
            if ((closestXPixel - initialMousePos.x) <= SELECT_MARGIN &&
                (closestXPixel - initialMousePos.x) >= -SELECT_MARGIN) {
               // search through every value of closest.v to see if
               // the click is in the vicinity of any of them

               // many points same place
               for (int j = 0; j < closestKnot.v.size(); j++) {

                  double scannedYValue = (double)closestKnot.v.get (j);

                  int YPixel =
                     ((int)((-(scannedYValue - maxYRange)) / yValuePerPixel));
                  if ((YPixel - initialMousePos.y) <= SELECT_MARGIN &&
                      (YPixel - initialMousePos.y >= -SELECT_MARGIN) &&
                      found == false) {
                     for (int v = 0; v < closestKnot.v.size(); v++) {
                        int idx = myProbe.getOrderedTraceIndex (v);

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
         if (zoomIn && e.getButton() == MouseEvent.BUTTON1) {
            // zooming in is true so set the initial point for if we drag to
            // select an area to zoom in on
            zoomSelectPoint = e.getPoint();
         }

         if (e.isPopupTrigger()) {
            // get the initial location of the mouse press
            initialMousePos = e.getPoint();

            NumericListKnot closestKnot = checkSelection (initialMousePos);

            if (closestKnot != null) {
                  JPopupMenu editMenu = new JPopupMenu();

                  JMenuItem deleteDataPoint =
                     new JMenuItem ("Delete Data Point");
                  deleteDataPoint.addActionListener (displayListener);

                  JMenuItem editDataPoint = new JMenuItem ("Edit Data Point");
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
         if (e.getButton() == MouseEvent.BUTTON1 && myProbe.isInput()) {
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

         if (drawKnotsP) {
            popupMenu.add (createMenuItem ("Hide Knot Points"));
         }
         else {
            popupMenu.add (createMenuItem ("Show Knot Points"));
         }
         popupMenu.add (createMenuItem ("Legend"));

         if (myProbe.isInput()) {
            popupMenu.addSeparator();
            popupMenu.add (createMenuItem ("Step Interpolation"));
            popupMenu.add (createMenuItem ("Linear Interpolation"));
            popupMenu.add (createMenuItem ("Cubic Interpolation"));
            popupMenu.add (createMenuItem ("CubicStep Interpolation"));
            if (getVectorSize() == 4) {
               popupMenu.add (createMenuItem ("Spherical Linear Interpolation"));
               popupMenu.add (createMenuItem ("Spherical Cubic Interpolation"));
            }
            else if (getVectorSize() == 16) {
               popupMenu.add (createMenuItem ("Spherical Linear Interpolation"));
               popupMenu.add (createMenuItem ("Spherical Cubic Interpolation"));
            }
         }

         popupMenu.addSeparator();
         popupMenu.add (createMenuItem ("Scale Data Values"));
         
         if (!isLargeDisplay ()) {
            popupMenu.add (createMenuItem ("Set Display Range ..."));
            popupMenu.add (createMenuItem ("Fit Display Range"));
         }
         
         return popupMenu;
      }

      public void mouseReleased (MouseEvent e) {
         if (moveDisplay) {
            // set the previous point that was moved from to null because we
            // are no longer moving the display
            previousPoint = null;
         }
         if (e.getButton() == MouseEvent.BUTTON1 && zoomSelectRect != null) {
            // an area has been zoom selected, so zoom in on that area
            double newYMin =
               (maxYRange - ((zoomSelectRect.y + zoomSelectRect.height) * yValuePerPixel));
            double newYMax = maxYRange - (zoomSelectRect.y * yValuePerPixel);

            double newXMin = minXRange + zoomSelectRect.x * secondsPerPixel;
            //   TimeBase.ticksToSeconds ((zoomSelectRect.x * ticksPerPixel));
            double newXMax = minXRange +
               (zoomSelectRect.x + zoomSelectRect.width) * secondsPerPixel;
               // TimeBase.ticksToSeconds (
                  //((zoomSelectRect.x + zoomSelectRect.width) * ticksPerPixel));

            setDisplayRange (newYMin, newYMax);
            setDisplayDomain (newXMin, newXMax);
            repaint();

            zoomSelectRect = null;
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
                  new JMenuItem ("Delete Data Point");
               deleteDataPoint.addActionListener (displayListener);
               JMenuItem editDataPoint = new JMenuItem ("Edit Data Point");
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
         if (e.getButton() == MouseEvent.BUTTON1 && myProbe.isInput()) {
            // get the initial location of the mouse press
            finalCoor = e.getPoint();

            // limit finalCoor to the bounds of the display
            if (finalCoor.x < 0)
               finalCoor.x = 0;
            else if (finalCoor.x > getWidth())
               finalCoor.x = getWidth();
            if (finalCoor.y < 0)
               finalCoor.y = 0;
            else if (finalCoor.y > getHeight())
               finalCoor.y = getHeight();

            editingP = false;
         }
         
         if (myAutoRangingP) {
            repaint();
         }
      }

      public void mouseDragged (MouseEvent e) {
         // only enter this loop when the initial mouse press is close
         // enough to a data point
         if (editingP == true && myProbe.isInput()) {
            double tempTime, tempYValue;
            tempCoor = e.getPoint();

            // limit tempCoor to the bounds of the display
            if (tempCoor.x < 0)
               tempCoor.x = 0;
            else if (tempCoor.x > getWidth())
               tempCoor.x = getWidth();
            if (tempCoor.y < 0)
               tempCoor.y = 0;
            else if (tempCoor.y > getHeight())
               tempCoor.y = getHeight();

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
               tempTime = xpixelToVirtualTime (tempCoor.x);

               tempYValue =
                  (double)(-(((double)tempCoor.y) * yValuePerPixel) + maxYRange);
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

               xPosition = tempCoor.x;
               yPosition = tempCoor.y;
               xTime = tempTime;
               yTime = tempYValue;
            }
         }
         else if (moveDisplay) {
            if (previousPoint == null) {
               previousPoint = e.getPoint();
            }
            else {
               moveDisplay (previousPoint.x - e.getX(), e.getY()
               - previousPoint.y);
               previousPoint = e.getPoint();
            }
         }
         else if (zoomIn && e.getModifiers() == MouseEvent.BUTTON1_MASK) {
            if (zoomSelectRect == null) {
               // a zoom selecting rectangle has not already been made so make
               // one
               zoomSelectRect = new Rectangle();
            }

            // get the frame bounds so we can check if we're beyond them
            Rectangle frameBounds = getBounds();

            // get the bounds for the x-axis
            if (e.getX() < frameBounds.x) {
               // out of bounds below zero
               zoomSelectRect.x = 0;
            }
            else if (e.getX() >= zoomSelectPoint.x &&
                     e.getX() < frameBounds.width) {
               // normal
               zoomSelectRect.width = Math.abs (e.getX() - zoomSelectPoint.x);
               zoomSelectRect.x = zoomSelectPoint.x;
            }
            else if (e.getX() < frameBounds.width) {
               // flip the x coordinates
               zoomSelectRect.width = Math.abs (e.getX() - zoomSelectPoint.x);
               zoomSelectRect.x = e.getX();
            }

            // get the bounds for the y-axis
            if (e.getY() < frameBounds.y) {
               // out of bounds below zero
               zoomSelectRect.y = 0;
            }
            else if (e.getY() >= zoomSelectPoint.y &&
                     e.getY() < frameBounds.height) {
               // normal
               zoomSelectRect.height = Math.abs (e.getY() - zoomSelectPoint.y);
               zoomSelectRect.y = zoomSelectPoint.y;
            }
            else if (e.getY() < frameBounds.height) {
               // flip the y coordinates
               zoomSelectRect.height = Math.abs (e.getY() - zoomSelectPoint.y);
               zoomSelectRect.y = e.getY();
            }
         }
         // repaint();
         myProbe.updateDisplaysWithoutAutoRanging();
      }

      public void mouseClicked (MouseEvent e) {
         if ((zoomIn || zoomOut || moveDisplay) &&
             e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
            // we are in a zoom or move display mode and a double click occurs,
            // reset to the default range TODO is this logical?
            resetDisplay();
         }
         else if (zoomIn && e.getButton() == MouseEvent.BUTTON1) {
            // zoom in
            zoomIn (e.getPoint());
            repaint();
         }
         else if (zoomOut && e.getButton() == MouseEvent.BUTTON1) {
            // zoom out
            zoomOut (e.getPoint());
            repaint();
         }
         else if (myProbe.isInput()) // only add knots for input probes
         {
            if (e.getClickCount() == 2) {
               int size = myProbe.getVsize();

               tempCoor = e.getPoint();
               double tempTime = xpixelToVirtualTime (tempCoor.x);
               newKnot = new NumericListKnot (size);
               NumericList list = myProbe.getNumericList();
               VectorNd vec = new VectorNd (size);
               if (getNumericList().getNumKnots() == 0) {
                  vec.setZero();
               }
               list.interpolate (vec, tempTime);
               newKnot.t = tempTime;
               newKnot.v = vec;
               getNumericList().add (newKnot);
               System.out.println ("adding new knot at" + newKnot.t);
               myProbe.updateDisplaysWithoutAutoRanging();
            }
         }
      }

      public void mouseEntered (MouseEvent e) {
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

         double initTimeInSec = xpixelToVirtualTime (initialMousePos.x);

         NumericListKnot closestKnot =
            getNumericList().findKnotClosest (initTimeInSec);

         // enter the loop only if closest exists, in other words we got the
         // knot and only if we are not currently zooming
         if (closestKnot != null) {
            // changes to add y-scanning into the code June10th
            int closestXPixel = virtualTimeToXpixel (closestKnot.t);

            for (int i = 0; i < closestKnot.v.size(); i++) {
               tempCoor = e.getPoint();
               if ((closestXPixel - initialMousePos.x) <= SELECT_MARGIN &&
                   (closestXPixel - initialMousePos.x) >= -SELECT_MARGIN &&
                   myProbe.isInput()) {
                  break;
               }
            }

            // see if delta-t is small enough to trigger a selection
            if (moveDisplay) {
               setCursor (moveDisplayCursor);
            }
            else if (zoomIn) {
               setCursor (zoomInCursor);
            }
            else if (zoomOut) {
               setCursor (zoomOutCursor);
            }
            else if ((closestXPixel - initialMousePos.x) <= SELECT_MARGIN &&
                     (closestXPixel - initialMousePos.x) >= -SELECT_MARGIN &&
                     myProbe.isInput()) {
               setCursor (Cursor.getPredefinedCursor (1));
            }
            else {
               setCursor (Cursor.getDefaultCursor());
            }
         }// end changes

      }

      private void setInterpolationOrder (Interpolation.Order order) {
         if (myProbe.getInterpolationOrder() != order) {
            myProbe.setInterpolationOrder (order);
            Main.getMain().rewidgetUpdate();
         }
      }

      // sort out the performed action on the display
      public void actionPerformed (ActionEvent e) {
         String nameOfAction = e.getActionCommand();

         System.out.println ("action=" + nameOfAction);
         if (nameOfAction == "Step Interpolation") {
            if (myProbe.isInput()) {
               setInterpolationOrder (Interpolation.Order.Step);
            }
         }
         else if (nameOfAction == "Linear Interpolation") {
            if (myProbe.isInput()) {
               setInterpolationOrder (Interpolation.Order.Linear);
            }
         }
         else if (nameOfAction == "Cubic Interpolation") {
            if (myProbe.isInput()) {
               setInterpolationOrder (Interpolation.Order.Cubic);
            }
         }
         else if (nameOfAction == "CubicStep Interpolation") {
            if (myProbe.isInput()) {
               setInterpolationOrder (Interpolation.Order.CubicStep);
            }
         }
         else if (nameOfAction == "Spherical Linear Interpolation") {
            if (myProbe.isInput()) {
               System.out.println ("setting");
               setInterpolationOrder (Interpolation.Order.SphericalLinear);
            }
         }
         else if (nameOfAction == "Spherical Cubic Interpolation") {
            if (myProbe.isInput()) {
               setInterpolationOrder (Interpolation.Order.SphericalCubic);
            }
         }
         else if (nameOfAction == "Set Display Range ...") {
            setDisplayRange();
         }
         else if (nameOfAction == "Fit Display Range") {
            fitDisplayRange();
         }
         else if (nameOfAction == "Scale Data Values") {
            scaleProbeValues();
         }
         else if (nameOfAction == "Show Knot Points") {
            drawKnotsP = true;
         }
         else if (nameOfAction == "Hide Knot Points") {
            drawKnotsP = false;
         }
         else if (nameOfAction == "Delete Data Point") {
            getNumericList().remove (newKnot);
         }
         else if (nameOfAction == "Edit Data Point") {
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
         else if (nameOfAction == "Legend") {
            LegendDisplay legend = myProbe.getLegend();
            if (!legend.isVisible()) {
               GuiUtils.locateRight (legend, NumericProbePanel.this);
               legend.setVisible (true);
            }
            
         }

         myProbe.updateDisplaysWithoutAutoRanging();
      }

   }

   public NumericProbePanel (Probe probe) {
      if (probe instanceof NumericProbeBase) {
         myProbe = (NumericProbeBase)probe;
      }
      else
         throw new IllegalArgumentException ("probe is of not a numeric type");

      fieldInitialization();
      appearanceInitialization();
      listenerInitialization();
      zoomCursorInitialization();
      skipIndiciesInit();
      int size = myProbe.getNumericList().getVectorSize();
      drawOrder = new int[size];
      myColorList = new Color[size];
      // System.out.println("draworder and colorlist size = "+size);
      for (int i = 0; i < size; i++) {
         drawOrder[i] = i;
         myColorList[i] = colorList[i % colorList.length];
      }
   }

   public void resetDrawOrder (int size) {
      myProbe.resetTraceOrder();
   }


   public void resetColors() {
      myProbe.resetTraceColors();
   }

   private void skipIndiciesInit() {
      skipIndicies = new Boolean[myProbe.getNumericList().getVectorSize()];
      for (int i = 0; i < skipIndicies.length; i++) {
         skipIndicies[i] = false;
      }
   }

   public void setSkipIndicies (boolean[] skip) {
//       skipIndicies = new Boolean[skip.length];
//       for (int i = 0; i < skipIndicies.length; i++) {
//          skipIndicies[i] = skip[i];
//       }
//       myProbe.updateDisplaysWithoutAutoRanging();
   }

   public void setLineColor (int index, Color color) {
      myProbe.setTraceColor (index, color);
      myProbe.updateDisplaysWithoutAutoRanging();
   }

   // @Override
   // protected void finalize() throws Throwable {
   //    myProbe.removeDisplay (this);
   //    super.finalize();
   // }

   public boolean isLargeDisplay() {
      return largeDisplay;
   }

   public void setLargeDisplay (boolean isLargeDisplay) {
      this.largeDisplay = isLargeDisplay;
      useVirtualTime = isLargeDisplay;
   }

   public int getVectorSize() {
      return myProbe.getVsize();
   }
}
