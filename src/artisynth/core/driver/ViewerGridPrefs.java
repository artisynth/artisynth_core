package artisynth.core.driver;

import java.awt.Color;
import javax.swing.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.widgets.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.render.GL.GLViewer.*;
import maspack.render.GridPlane.*;
import maspack.render.GL.GLViewer;

import java.io.IOException;
import java.io.PrintWriter;

import artisynth.core.util.*;

/**
 * Preferences related to the viewer
 */
public class ViewerGridPrefs extends Preferences {

   protected ViewerManager myViewerManager;

   protected static final Color DARK_RED = new Color (0.5f, 0, 0);
   protected static final Color DARK_GREEN = new Color (0, 0.5f, 0);
   protected static final Color DARK_BLUE = new Color (0, 0, 0.5f);

   public static int DEFAULT_MIN_CELL_PIXELS = 10;
   private int myMinCellPixels = DEFAULT_MIN_CELL_PIXELS;

   private static Color myDefaultMajorColor = new Color (0.4f, 0.4f, 0.4f);
   private float[] myMajorRGB = createRGB (myDefaultMajorColor);

   private static Color myDefaultMinorColor = null;
   private float[] myMinorRGB = createRGB (myDefaultMinorColor);

   private static Color myDefaultXAxisColor = DARK_RED;
   private float[] myXAxisRGB = createRGB (myDefaultXAxisColor);

   private static Color myDefaultYAxisColor = DARK_GREEN;
   private float[] myYAxisRGB = createRGB (myDefaultYAxisColor);   

   private static Color myDefaultZAxisColor = DARK_BLUE;
   private float[] myZAxisRGB = createRGB (myDefaultZAxisColor);

   public static int DEFAULT_LINE_WIDTH = 1;
   private int myLineWidth = DEFAULT_LINE_WIDTH;

   public static AxisLabeling DEFAULT_X_AXIS_LABELING = AxisLabeling.OFF;
   AxisLabeling myXAxisLabeling = DEFAULT_X_AXIS_LABELING;

   public static AxisLabeling DEFAULT_Y_AXIS_LABELING = AxisLabeling.OFF;
   AxisLabeling myYAxisLabeling = DEFAULT_Y_AXIS_LABELING;

   public static double DEFAULT_LABEL_SIZE = 15.0;
   double myLabelSize = DEFAULT_LABEL_SIZE;
   
   public static final Color DEFAULT_LABEL_COLOR = null;
   float[] myLabelRGB = createRGB(DEFAULT_LABEL_COLOR);

   private Color createColor (float[] rgb) {
      if (rgb == null) {
         return null;
      }
      else {
         return new Color (rgb[0], rgb[1], rgb[2]);
      }
   }

   private static float[] createRGB (Color color) {
      if (color == null) {
         return null;
      }
      else {
         return color.getRGBColorComponents (null);
      }
   }

   public static PropertyList myProps =
      new PropertyList (ViewerGridPrefs.class);

   static final AxisAngle myDefaultAxisAngle = new AxisAngle();

   static {
      myProps.add ("minCellPixels", "minimum number of pixels in a cell", 10);
      myProps.add ("majorColor",
         "color for major cell divisions", myDefaultMajorColor);
      myProps.add ("minorColor",
         "color for minor cell divisions", myDefaultMinorColor);
      myProps.add (
         "xAxisColor", "color for x (horizontal) axis", myDefaultXAxisColor);
      myProps.add (
         "yAxisColor", "color for y (vertical) axis", myDefaultYAxisColor);
      myProps.add (
         "zAxisColor", "color for z (perpendicular) axis", myDefaultZAxisColor);
      myProps.add ("lineWidth", "width of rendering lines", DEFAULT_LINE_WIDTH);
      myProps.add(
         "xAxisLabeling", "controls labeling of the x grid axis",
         DEFAULT_X_AXIS_LABELING);
      myProps.add(
         "yAxisLabeling", "controls labeling of the y grid axis",
         DEFAULT_Y_AXIS_LABELING);
      myProps.add(
         "labelSize", "'em' size of axis labels, in pixels",
         DEFAULT_LABEL_SIZE, "NS");  
      myProps.add(
         "labelColor", "color for axis labels", DEFAULT_LABEL_COLOR);        
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public ViewerGridPrefs (ViewerManager viewerManager) {
      myViewerManager = viewerManager;
   }

   public int getMinCellPixels() {
      return myMinCellPixels;
   }

   public void setMinCellPixels (int n) {
      myMinCellPixels = n;
   }

   public Color getMajorColor() {
      return createColor (myMajorRGB);
   }

   public void setMajorColor (Color color) {
      if (color == null) {
         throw new IllegalArgumentException ("color must not be null");
      }
      myMajorRGB = createRGB (color);
   }
   
   public Color getMinorColor() {
      return createColor (myMinorRGB);
   }

   public void setMinorColor (Color color) {
      myMinorRGB = createRGB (color);
   }

   public Color getXAxisColor() {
      return createColor (myXAxisRGB);
   }

   public void setXAxisColor (Color color) {
      myXAxisRGB = createRGB (color);
   }

   public Color getYAxisColor() {
      return createColor (myYAxisRGB);
   }

   public void setYAxisColor (Color color) {
      myYAxisRGB = createRGB (color);
   }

   public Color getZAxisColor() {
      return createColor (myZAxisRGB);
   }

   public void setZAxisColor (Color color) {
      myZAxisRGB = createRGB (color);
   }

   public int getLineWidth() {
      return myLineWidth;
   }

   public void setLineWidth (int width) {
      myLineWidth = width;
   }

   public AxisLabeling getXAxisLabeling() {
      return myXAxisLabeling;
   }
   
   public void setXAxisLabeling (AxisLabeling labeling) {
      myXAxisLabeling = labeling;
   }
   
   public AxisLabeling getYAxisLabeling() {
      return myYAxisLabeling;
   }
   
   public void setYAxisLabeling (AxisLabeling labeling) {
      myYAxisLabeling = labeling;
   }

   public double getLabelSize () {
      return myLabelSize;
   }
   
   public void setLabelSize (double emSize) {
      myLabelSize = emSize;
   }
   
   public Color getLabelColor () {
      return createColor (myLabelRGB);
   }

   public void setLabelColor (Color color) {
      myLabelRGB = createRGB(color);
   }
   
   public void applyToCurrent() {
      for (int i=0; i<myViewerManager.numViewers(); i++) {
         GridPlane grid = myViewerManager.getViewer(i).getGrid();
         grid.setMinCellPixels (getMinCellPixels());
         grid.setMajorColor (getMajorColor());
         grid.setMinorColor (getMinorColor());
         grid.setXAxisColor (getXAxisColor());
         grid.setYAxisColor (getYAxisColor());
         grid.setZAxisColor (getZAxisColor());
         grid.setLineWidth (getLineWidth());
         grid.setXAxisLabeling (getXAxisLabeling());
         grid.setYAxisLabeling (getYAxisLabeling());
         grid.setLabelSize (getLabelSize());
         grid.setLabelColor (getLabelColor());
      }
   }

   public void setFromCurrent() {
      if (myViewerManager.numViewers() < 1) {
         throw new InternalErrorException (
            "Main viewer not yet set in ViewerManager");
      }
      GridPlane grid = myViewerManager.getViewer(0).getGrid();
      setMinCellPixels (grid.getMinCellPixels());
      setMajorColor (grid.getMajorColor());
      setMinorColor (grid.getMinorColor());
      setXAxisColor (grid.getXAxisColor());
      setYAxisColor (grid.getYAxisColor());
      setZAxisColor (grid.getZAxisColor());
      setLineWidth (grid.getLineWidth());
      setXAxisLabeling (grid.getXAxisLabeling());
      setYAxisLabeling (grid.getYAxisLabeling());
      setLabelSize (grid.getLabelSize());
      setLabelColor (grid.getLabelColor());      
   }

   protected PropertyPanel createEditingPanel() {
      PropertyPanel panel = createDefaultEditingPanel();
      addLoadApplyButtons (panel);
      return panel;
   }

}
