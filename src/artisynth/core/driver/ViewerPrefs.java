package artisynth.core.driver;

import java.awt.Color;
import java.awt.Font;
import javax.swing.*;

import maspack.matrix.*;
import maspack.properties.*;
import maspack.widgets.*;
import maspack.render.*;
import maspack.render.Renderer.AxisDrawStyle;
import maspack.render.Viewer.RotationMode;

/**
 * Preferences related to the viewer
 */
public class ViewerPrefs extends Preferences {

   protected ViewerManager myViewerManager;
   protected Main myMain;

   private Color myBackgroundColor = ViewerManager.DEFAULT_BACKGROUND_COLOR;
   private Color mySelectionColor = ViewerManager.DEFAULT_SELECTION_COLOR;
   private AxisDrawStyle myAxisDrawStyle = ViewerManager.DEFAULT_AXIS_DRAW_STYLE;
   private double myAxisLengthRadiusRatio =
      ViewerManager.DEFAULT_AXIS_LENGTH_RADIUS_RATIO;
   private RotationMode myRotationMode = ViewerManager.DEFAULT_ROTATION_MODE;
   private AxisAlignedRotation myDefaultAxialView =
      ViewerManager.DEFAULT_AXIAL_VIEW;
   private Vector2d myEllipticCursorSize = 
      new Vector2d(ViewerManager.DEFAULT_ELLIPTIC_CURSOR_SIZE);
   private int mySurfaceResolution = ViewerManager.DEFAULT_SURFACE_RESOLUTION; 
   private GraphicsInterface myGraphics = Main.DEFAULT_GRAPHICS;

   private double myFrameRate = Main.DEFAULT_FRAME_RATE;

   // not currently exported as preferences:
   private boolean myOrthographicView = ViewerManager.DEFAULT_ORTHOGRAPHIC_VIEW;
   private boolean myDrawAxes = ViewerManager.DEFAULT_DRAW_AXES;
   private boolean myDrawGrid = ViewerManager.DEFAULT_DRAW_GRID;

   static PropertyList myProps = new PropertyList (ViewerPrefs.class);

   static {
      myProps.add (
         "backgroundColor", "background color",
         ViewerManager.DEFAULT_BACKGROUND_COLOR);
      myProps.add (
         "selectionColor", "selection color",
         ViewerManager.DEFAULT_SELECTION_COLOR);
      myProps.add (
         "axisDrawStyle",
         "draw style for rendering coordinate axes",
         ViewerManager.DEFAULT_AXIS_DRAW_STYLE);
      myProps.add (
         "axisLengthRadiusRatio",
         "default length/radius ratio used for rendering solid axes",
         ViewerManager.DEFAULT_AXIS_LENGTH_RADIUS_RATIO, "NS");
      myProps.add (
         "rotationMode",
         "method for interactive rotation",
         ViewerManager.DEFAULT_ROTATION_MODE);
      myProps.add(
         "defaultAxialView",
         "default axis-aligned view orientation",
         ViewerManager.DEFAULT_AXIAL_VIEW);
      myProps.add (
         "surfaceResolution", "resolution for built-in curved primitives", 
         ViewerManager.DEFAULT_SURFACE_RESOLUTION);
      myProps.add(
         "ellipticCursorSize", "dimension of the elliptic cursor", 
         ViewerManager.DEFAULT_ELLIPTIC_CURSOR_SIZE);
      myProps.add(
         "graphics", "graphics interface to use for rendering", 
         Main.DEFAULT_GRAPHICS);
   }

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public ViewerPrefs (Main main) {
      myMain = main;
      myViewerManager = main.getViewerManager();
   }

   public Color getBackgroundColor () {
      return myBackgroundColor;
   }

   public void setBackgroundColor (Color backgroundColor) {
      myBackgroundColor = backgroundColor;
   }

   public Color getSelectionColor () {
      return mySelectionColor;
   }

   public void setSelectionColor (Color selectionColor) {
      mySelectionColor = selectionColor;
   }

   public AxisDrawStyle getAxisDrawStyle () {
      return myAxisDrawStyle;
   }

   public void setAxisDrawStyle (AxisDrawStyle axisDrawStyle) {
      myAxisDrawStyle = axisDrawStyle;
   }

   public double getAxisLengthRadiusRatio () {
      return myAxisLengthRadiusRatio;
   }

   public void setAxisLengthRadiusRatio (double ratio) {
      myAxisLengthRadiusRatio = ratio;
   }

   public RotationMode getRotationMode () {
      return myRotationMode;
   }

   public void setRotationMode (RotationMode rotationMode) {
      myRotationMode = rotationMode;
   }

   public AxisAlignedRotation getDefaultAxialView () {
      return myDefaultAxialView;
   }

   public void setDefaultAxialView (AxisAlignedRotation defaultAxialView) {
      myDefaultAxialView = defaultAxialView;
   }

   public boolean getOrthographicView () {
      return myOrthographicView;
   }

   public void setOrthographicView (boolean enable) {
      myOrthographicView = enable;
   }

   public boolean getDrawAxes () {
      return myDrawAxes;
   }

   public void setDrawAxes (boolean enable) {
      myDrawAxes = enable;
   }

   public boolean getDrawGrid () {
      return myDrawGrid;
   }

   public void setDrawGrid (boolean enable) {
      myDrawGrid = enable;
   }

   public Vector2d getEllipticCursorSize () {
      return myEllipticCursorSize;
   }

   public void setEllipticCursorSize (Vector2d ellipticCursorSize) {
      myEllipticCursorSize = ellipticCursorSize;
   }

   public int getSurfaceResolution () {
      return mySurfaceResolution;
   }

   public void setSurfaceResolution (int surfaceResolution) {
      mySurfaceResolution = surfaceResolution;
   }

   public GraphicsInterface getGraphics () {
      return myGraphics;
   }

   public void setGraphics (GraphicsInterface graphics) {
      myGraphics = graphics;
   }

   public double getFrameRate () {
      return myFrameRate;
   }

   public void setFrameRate (double rate) {
      myFrameRate = rate;
   }

   public void applyToCurrent() {
      myViewerManager.setBackgroundColor (getBackgroundColor());
      myViewerManager.setSelectionColor (getSelectionColor());
         
      myViewerManager.setAxisDrawStyle (getAxisDrawStyle());
      myViewerManager.setAxisLengthRadiusRatio (getAxisLengthRadiusRatio());
      myViewerManager.setRotationMode (getRotationMode());
      myViewerManager.setDefaultAxialView (getDefaultAxialView());
      // myViewerManager.setOrthographicView (getOrthographicView());
      // myViewerManager.setDrawAxes (getDrawAxes());
      // myViewerManager.setDrawGrid (getDrawGrid());
      myViewerManager.setSurfaceResolution (getSurfaceResolution());
      myViewerManager.setEllipticCursorSize (getEllipticCursorSize());

      if (myViewerManager.getDialog() != null) {
         myViewerManager.getDialog().updateWidgetValues();
      }
   }

   public void setFromCurrent() {
      setBackgroundColor (myViewerManager.getBackgroundColor());
      setSelectionColor (myViewerManager.getSelectionColor());
         
      setAxisDrawStyle (myViewerManager.getAxisDrawStyle());
      setAxisLengthRadiusRatio (myViewerManager.getAxisLengthRadiusRatio());
      setRotationMode (myViewerManager.getRotationMode());
      setDefaultAxialView (myViewerManager.getDefaultAxialView());
      // setOrthographicView (myViewerManager.getOrthographicView());
      // setDrawAxes (myViewerManager.getDrawAxes());
      // setDrawGrid (myViewerManager.getDrawGrid());
      setSurfaceResolution (myViewerManager.getSurfaceResolution());
      setEllipticCursorSize (myViewerManager.getEllipticCursorSize());
      setGraphics (myMain.getGraphics());
   }

   protected PropertyPanel createEditingPanel() {
      PropertyPanel panel = createDefaultEditingPanel();

      // create separator and asterix for the 'graphics' widget
      EnumSelector widget = (EnumSelector)panel.getWidget ("graphics");
      widget.setLabelText ("graphics *");
      panel.addWidget (new JSeparator(),  panel.getComponentIndex (widget));

      addLoadApplyButtons (
         panel, new String[] { "  * restart required" });

      return panel;
   }

}
