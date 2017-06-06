/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.awt.Color;
import java.awt.geom.Rectangle2D;

import maspack.matrix.AxisAngle;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.Plane;
import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.render.ConvexPoly2d;
import maspack.render.Dragger3dAdapter;
import maspack.render.Dragger3dBase;
import maspack.render.Dragger3dEvent;
import maspack.render.RenderObject;
import maspack.render.Renderer;
import maspack.render.Rotator3d;
import maspack.render.Translator3d;
import maspack.render.Transrotator3d;
import maspack.render.ConvexPoly2d.Vertex2d;
import maspack.render.Dragger3d.DraggerType;
import maspack.render.Dragger3dConstrainer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;
import maspack.render.color.ColorUtils;
import maspack.util.InternalErrorException;
import maspack.util.Round;

public class GLGridPlane implements HasProperties {

   private static Color myDefaultMajorColor = new Color (0.4f, 0.4f, 0.4f);
   private static Color myDefaultMinorColor = null;
   private static Color myDefaultXAxisColor = null;
   private static Color myDefaultYAxisColor = null;
   private static Color myDefaultZAxisColor = null;

   public enum AxisLabeling {
      OFF,
      ON
   };

   /**
    * Ensures that the dragger keeps the grid properly aligned when
    * world alignment is enabled.
    */
   private class AlignConstrainer implements Dragger3dConstrainer {

      public boolean updateRotation (
         RotationMatrix3d RDWnew, RigidTransform3d TDW) {

         AxisAlignedRotation alignedRot =
            AxisAlignedRotation.getNearest (RDWnew);
         RDWnew.set (alignedRot.getMatrix());
         return !RDWnew.equals (TDW.R);
      }

      public boolean updatePosition (
         Vector3d pnew, RigidTransform3d TDW) {

         double csize = myResolution.getMajorCellSize();
         pnew.x = csize*Math.round(pnew.x/csize);
         pnew.y = csize*Math.round(pnew.y/csize);
         pnew.z = csize*Math.round(pnew.z/csize);
         return !pnew.equals (TDW.p);
      }

      public boolean updatePose (
         RigidTransform3d TDWnew, RigidTransform3d TDW) {

         boolean changed = false;
         if (updatePosition (TDWnew.p, TDW)) {
            changed = true;
         }
         // if TDWnew == TDW, then TDW.p might have changed at this point, but
         // that's OK since update rotation doesn't use TDW.p
         if (updateRotation (TDWnew.R, TDW)) {
            changed = true;
         }
         return changed;
      }
   }
  

   RigidTransform3d XGridToWorld = new RigidTransform3d();
   // Desired XGridToWorld value. XGridToWorld may be different from
   // this if myLockAxesToWorld is true:
   RigidTransform3d XGridToWorldTarget = new RigidTransform3d();
   Dragger3dBase myDragger;
   boolean myGridVisible = true;
   GLViewer myViewer = null;
   float[] myMajorRGB = createRGB (myDefaultMajorColor);
   float[] myMinorRGB = createRGB (myDefaultMinorColor);
   //float[] myMinorRGB = new float[3]; // used to compute division color
   float[] myXAxisRGB = createRGB (myDefaultXAxisColor);
   float[] myYAxisRGB = createRGB (myDefaultYAxisColor);
   float[] myZAxisRGB = createRGB (myDefaultZAxisColor);
   double myMinSize;
   int myMinCellPixels = 10;
   boolean myAutoSizedP = true;
   GLGridResolution myResolution = new GLGridResolution (1, 10);
   int myLineWidth = 1;

   public static boolean DEFAULT_LOCK_AXES_TO_WORLD = false;
   boolean myLockAxesToWorld = DEFAULT_LOCK_AXES_TO_WORLD;
   // if true, draw the X/Y axes at their true locations in world
   // coordinates. Otherwise, draw them at the grid center. This
   // can only be true if myLockAxesToWorld is also true.
   public static boolean DEFAULT_USE_WORLD_ORIGIN = false;
   boolean myUseWorldOrigin = DEFAULT_USE_WORLD_ORIGIN;
   public static AxisLabeling DEFAULT_X_AXIS_LABELING = AxisLabeling.OFF;
   AxisLabeling myXAxisLabeling = DEFAULT_X_AXIS_LABELING;
   public static AxisLabeling DEFAULT_Y_AXIS_LABELING = AxisLabeling.OFF;
   AxisLabeling myYAxisLabeling = DEFAULT_Y_AXIS_LABELING;
   public static double DEFAULT_LABEL_SIZE = 15.0;
   double myLabelSize = DEFAULT_LABEL_SIZE;
   
   public static final Color DEFAULT_LABEL_COLOR = null;
   float[] myLabelRGB = createRGB(DEFAULT_LABEL_COLOR);

   private static class RenderInfo {
      // stores all lines
      RenderObject lines;

      int numDivisions;
      int xcnt;
      int ycnt;
      int x_axis_j;
      int y_axis_i;
      float xmin;
      float xmax;
      float ymin;
      float ymax;
      float minorSize;

      float[] xColor;
      float[] yColor;
      float[] majorColor;
      float[] minorColor;

      public RenderInfo() {
         // initialize everything
         lines = new RenderObject();
         numDivisions = 0;
         xcnt = 0;
         ycnt = 0;
         x_axis_j = 0;
         y_axis_i = 0;
         xmin = 0;
         xmax = 0;
         ymin = 0;
         ymax = 0;
         minorSize = 0;
         xColor = new float[]{0,0,0,0};
         yColor = new float[]{0,0,0,0};
         majorColor = new float[]{0,0,0,0};
         minorColor = new float[]{0,0,0,0};
      }

      private float[] copyColor4(float[] c) {
         float[] out = new float[4];
         for (int i=0; i<3; ++i) {
            out[i] = c[i];
         }
         if (c.length > 3) {
            out[3] = c[3];
         } else {
            out[3] = 1;
         }
         return out;
      }

      public boolean update(
         int numDivisions, float minorSize, int xcnt, int ycnt, int x_axis_j, 
         int y_axis_i, float xmin, float xmax, float ymin, float ymax,
         float[] xColor, float[] yColor, 
         float[] majorColor, float[] minorColor) {
         boolean positionsChanged = false;

         if (this.numDivisions != numDivisions) {
            positionsChanged = true;
            this.numDivisions = numDivisions;
         }
         if (this.xcnt != xcnt) {
            positionsChanged = true;
            this.xcnt = xcnt;
         }
         if (this.ycnt != ycnt) {
            positionsChanged = true;
            this.ycnt = ycnt;
         }
         if (this.x_axis_j != x_axis_j) {
            positionsChanged = true;
            this.x_axis_j = x_axis_j;
         }
         if (this.y_axis_i != y_axis_i) {
            positionsChanged = true;
            this.y_axis_i = y_axis_i;
         }
         if (this.xmin != xmin) {
            positionsChanged = true;
            this.xmin = xmin;
         }
         if (this.xmax != xmax) {
            positionsChanged = true;
            this.xmax = xmax;
         }
         if (this.ymin != ymin) {
            positionsChanged = true;
            this.ymin = ymin;
         }
         if (this.ymax != ymax) {
            positionsChanged = true;
            this.ymax = ymax;
         }
         if (this.minorSize != minorSize) {
            positionsChanged = true;
            this.minorSize = minorSize;
         }

         // deal with changing colors
         if (majorColor == null) {
            majorColor = myDefaultMajorColor.getColorComponents (new float[4]);
         }
         if (minorColor == null) {
            minorColor = new float[4];
            for (int i=0; i<3; ++i) {
               minorColor[i] = 0.4f*majorColor[i];  // scale down
            }
            if (majorColor.length > 3) {
               minorColor[3] = majorColor[3];
            } else {
               minorColor[3] = 1f;
            }
         }

         if (xColor == null) {
            xColor = majorColor;
         }
         if (yColor == null) {
            yColor = majorColor;
         }

         // check if colors have changed
         boolean colorsChanged = false;
         float eps = 0.01f;
         if (!ColorUtils.RGBAequals(xColor, this.xColor, eps)) {
            this.xColor = copyColor4 (xColor);
            colorsChanged = true;
         }
         if (!ColorUtils.RGBAequals(yColor, this.yColor, eps)) {
            this.yColor = copyColor4 (yColor);
            colorsChanged = true;
         }
         if (!ColorUtils.RGBAequals(majorColor, this.majorColor, eps)) {
            this.majorColor = copyColor4 (majorColor);
            colorsChanged = true;
         }
         if (!ColorUtils.RGBAequals(minorColor, this.minorColor, eps)) {
            this.minorColor = copyColor4 (minorColor);
            colorsChanged = true;
         }

         // if this changed, we need to rebuild
         if (colorsChanged || positionsChanged) {

            lines.clearAll();

            lines.ensurePositionCapacity(2*(xcnt+ycnt));
            lines.ensureLineCapacity (xcnt+ycnt);
            lines.ensureColorCapacity (4);

            // add colors
            int xcidx = lines.addColor(this.xColor);
            int ycidx = lines.addColor (this.yColor);
            int majorcidx = lines.addColor (this.majorColor);
            int minorcidx = lines.addColor (this.minorColor);

            lines.beginBuild(DrawMode.LINES);

            for (int i = 0; i < xcnt; i++) {
               double x = xmin + i * minorSize;
               int ii = y_axis_i - i;

               if (ii == 0) {
                  lines.setCurrentColor (ycidx);
               } else if (ii%numDivisions == 0) {
                  lines.setCurrentColor (majorcidx);
               } else {
                  lines.setCurrentColor (minorcidx);
               }
               lines.vertex((float)x, (float)ymin, 0);
               lines.vertex((float)x, (float)ymax, 0);
            }

            for (int j = 0; j < ycnt; j++) {
               double y = ymin + j * minorSize;
               int jj = x_axis_j - j;

               if (jj == 0) {
                  lines.setCurrentColor (xcidx);
               } else if (jj%numDivisions == 0) {
                  lines.setCurrentColor (majorcidx);
               } else {
                  lines.setCurrentColor (minorcidx);
               }
               lines.vertex((float)xmin, (float)y, 0);
               lines.vertex((float)xmax, (float)y, 0);
            }   
            lines.endBuild();
         }


         return (positionsChanged || colorsChanged);
      }

      public RenderObject getRenderObject () {
         return lines;
      }

   }

   RenderInfo rcacheInfo = new RenderInfo();


   private static GLGridResolution myDefaultResolution =
   new GLGridResolution (1, 10);

   public static PropertyList myProps = new PropertyList (GLGridPlane.class);

   static AxisAngle myDefaultAxisAngle = new AxisAngle();

   static {
      myProps.add ("resolution", "grid resolution", myDefaultResolution);
      myProps.add ("minSize", "minimum grid size", 1);
      myProps.add (
         "autoSized isAutoSized", 
         "cell sizes are computed automatically", true);
      //      myProps.add (
      //         "autoCellDividing isAutoCellDividing",
      //         "automatically compute number of divisions", true);
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
      myProps.add ("lineWidth", "width of rendering lines", 1);
      myProps.add ("position", "position of the grid coordinate frame", null);
      myProps.get ("position").setAutoWrite (false);
      myProps.add (
         "orientation", "orientation of the grid coordinate frame",
         myDefaultAxisAngle);
      myProps.get ("orientation").setAutoWrite (false);
      myProps.add (
         "lockAxesToWorld", "constrains grid to world axes and coordinates",
         DEFAULT_LOCK_AXES_TO_WORLD);
      myProps.add (
         "useWorldOrigin", "align grid origin with the world origin",
         DEFAULT_USE_WORLD_ORIGIN);    
      myProps.add(
         "xAxisLabeling", "controls labeling of the x grid axis",
         DEFAULT_X_AXIS_LABELING);
      myProps.add(
         "yAxisLabeling", "controls labeling of the y grid axis",
         DEFAULT_Y_AXIS_LABELING);
      myProps.add(
         "labelSize", "'em' size of axis labels, in pixels",
         DEFAULT_LABEL_SIZE);  
      myProps.add(
         "labelColor", "color for axis labels", DEFAULT_LABEL_COLOR);        
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * {@inheritDoc}
    */
   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   /**
    * Returns the desired minimum number of pixels per cell or divided cell.
    * 
    * @return minimum pixels for each cell or divided cell
    */
   public int getMinCellPixels() {
      return myMinCellPixels;
   }

   /**
    * Sets the desired minimum number of pixels per cell or divided cell. This
    * is used when automatically computing cell sizes and/or divisions.
    * 
    * @param n
    * minimum pixels for each cell or divided cell
    */
   public void setMinCellPixels (int n) {
      myMinCellPixels = n;
   }

   /**
    * Returns the cell size for this grid. If auto cell sizing is enabled, this
    * value is computed automatically.
    * 
    * @return size grid cell size
    */
   public double getCellSize() {
      return myResolution.getMajorCellSize();
   }

   /**
    * Sets the line width used to render this grid.
    * 
    * @param width
    * new line width
    */
   public void setLineWidth (int width) {
      myLineWidth = width;
   }

   /**
    * Returns the line width used to render this grid.
    * 
    * @return line width
    */
   public int getLineWidth() {
      return myLineWidth;
   }

   public boolean getLockAxesToWorld() {
      return myLockAxesToWorld;
   }

   public void setLockAxesToWorld (boolean enable) {
      if (myLockAxesToWorld != enable) {
         myLockAxesToWorld = enable;
         if (enable) {
            // reset position; new position will be constrained
            setGridToWorld (getGridToWorld());
            if (myDragger instanceof Transrotator3d) {
               ((Transrotator3d)myDragger).setConstrainer (
                  new AlignConstrainer());
            }
         }
         else {
            if (myDragger instanceof Transrotator3d) {
               ((Transrotator3d)myDragger).setConstrainer (null);
            }
            setUseWorldOrigin (false);
         }
      }
   }
   
   public Point3d getPosition() {
      return new Point3d (XGridToWorld.p);
   }

   public void setPosition (Point3d pos) {
      RigidTransform3d X = new RigidTransform3d (XGridToWorld);
      X.p.set (pos);
      setGridToWorld (X);
   }
   
   // /**
   //  * Set the grid position approximately to <code>pos</code>,
   //  * rounded to align with the current major grid division.
   //  * 
   //  * @param pos approximate grid position
   //  */
   // public void setAlignedPosition (Point3d pos) {
   //    double res = getResolution().getMajorCellSize();
   //    double x = res*Math.round(pos.x/res);
   //    double y = res*Math.round(pos.y/res);
   //    double z = res*Math.round(pos.z/res);
   //    setPosition (new Point3d(x, y, z));
   // }
   
   public AxisAngle getOrientation() {
      AxisAngle axisAng = new AxisAngle();
      XGridToWorld.R.getAxisAngle (axisAng);
      return axisAng;
   }

   public void setOrientation (AxisAngle axisAng) {
      RigidTransform3d X = new RigidTransform3d (XGridToWorld);
      X.R.setAxisAngle (axisAng);
      setGridToWorld (X);
      System.out.println ();
   }

   /**
    * Sets the resolution for this grid. 
    * 
    * <p>If the major cell size is 0, then auto-sizing will be enabled and the 
    * grid size will be computed automatically, based on the viewer's current 
    * zoom level. Otherwise, auto-sizing will be turned off.
    * 
    * @param majorCellSize major cell size
    * @param numDivisions number of divisions per cell 
    */
   public void setResolution (double majorCellSize, int numDivisions) {
      if (majorCellSize == 0) {
         myAutoSizedP = true;
      }
      else {
         myResolution.set (majorCellSize, numDivisions);
         myAutoSizedP = false;
      }
   }

   /**
    * Sets the resolution for this grid. The resolution 
    * <code>res</code> specifies the major cell size and the number of 
    * subdivisions per cell.
    * 
    * <p>If the major cell size is 0, then auto-sizing will be enabled and the 
    * grid size will be computed automatically, based on the viewer's current 
    * zoom level. Otherwise, auto-sizing will be turned off.
    * 
    * @param res 
    * specified new grid resolution, or auto-sizing.
    */
   public void setResolution (GLGridResolution res) {
      setResolution (res.getMajorCellSize(), res.getNumDivisions());
   }

   /**
    * Retuns the resolution for this grid.
    * 
    * @return grid resolution
    */
   public GLGridResolution getResolution() {
      return myResolution;
   }

   /**
    * Returns true is cell sizes for this grid are computed automatically.
    * 
    * @return true is auto cell sizing is enabled
    */
   public boolean isAutoSized() {
      return myAutoSizedP;
   }

   /**
    * Enables or disables auto cell sizing for this grid.
    * 
    * @param enable
    * if true, enables auto cell sizing
    */
   public void setAutoSized (boolean enable) {
      myAutoSizedP = enable;
   }

   /**
    * Returns the number of subdivisions for each cell. If auto cell dividing is
    * enabled, this value is computed automatically.
    * 
    * @return number of cell subdivisions
    */
   public int numCellDivisions() {
      return myResolution.getNumDivisions();
   }

   /**
    * Returns the number of subdivisions for each cell.
    * 
    * @return number of cell subdivisions
    */
   public int getCellDivisions() {
      return myResolution.getNumDivisions();
   }

   /**
    * Sets the minimum grid size.
    */
   public double setMinSize (double size) {
      if (size <= 0) {
         myMinSize = 1.0;
      }
      else {
         myMinSize = size;
         //double halfSize = Round.up125 (size/2);
         //myMinSize = 2*halfSize;
      }
      return myMinSize;
   }

   public double getMinSize() {
      return myMinSize;
   }

   public RigidTransform3d getGridToWorld() {
      return XGridToWorld;
   }

   /**
    * Aligns a point with the nearest point defined by this grid. Both the
    * original and aligned points are described in grid coordinates.
    */
   public void alignPoint (Vector3d aligned, Vector3d pnt) {
      double res = getCellSize()/getCellDivisions();
      aligned.x = res*Math.round(aligned.x/res); 
      aligned.y = res*Math.round(aligned.y/res); 
      aligned.z = res*Math.round(aligned.z/res); 
   }

   RigidTransform3d alignToWorld (RigidTransform3d GTW) {
      RigidTransform3d ATW = new RigidTransform3d();
      double res = getResolution().getMajorCellSize();
      ATW.p.x = res*Math.round(GTW.p.x/res);
      ATW.p.y = res*Math.round(GTW.p.y/res);
      ATW.p.z = res*Math.round(GTW.p.z/res);
      AxisAlignedRotation alignedRot = AxisAlignedRotation.getNearest(GTW.R);
      alignedRot.getMatrix (ATW.R);
      return ATW;
   }

   public void setGridToWorld (RigidTransform3d TGW) {
      XGridToWorldTarget.set (TGW);
      if (myLockAxesToWorld) {
         RigidTransform3d TGWnew = alignToWorld (TGW);
         AlignConstrainer aligner = new AlignConstrainer();
         aligner.updatePose (TGWnew, TGW);
         XGridToWorld.set (TGWnew);
      }
      else {
         XGridToWorld.set (TGW);
      }
      if (myDragger != null) {
         myDragger.setDraggerToWorld (XGridToWorld);
      }
      if (myViewer != null) {
         myViewer.repaint();
      }
   }

   public void getGridToWorld (RigidTransform3d X) {
      X.set (XGridToWorld);
   }

   public void setViewer (GLViewer viewer) {
      if (myViewer != viewer) {
         if (myDragger != null) {
            if (myViewer != null) {
               myViewer.removeDragger (myDragger);
            }
            if (viewer != null) {
               viewer.addDragger (myDragger);
            }
         }
         myViewer = viewer;
      }
   }

   public GLViewer getViewer() {
      return myViewer;
   }

   public DraggerType getDragger() {
      if (myDragger == null) {
         return DraggerType.None;
      }
      else if (myDragger instanceof Translator3d) {
         return DraggerType.Translator;
      }
      else if (myDragger instanceof Transrotator3d) {
         return DraggerType.Transrotator;
      }
      else if (myDragger instanceof Rotator3d) {
         return DraggerType.Rotator;
      }
      else {
         throw new InternalErrorException (
            "Clip plane dragger set to unsupported type " +
            myDragger.getClass());
      }
   }

   private class MyDraggerListener extends Dragger3dAdapter {

      RigidTransform3d TGW0 = new RigidTransform3d();

      MyDraggerListener () {
      }
      
      public void draggerBegin (Dragger3dEvent e) {
         TGW0.set (XGridToWorld);
      }

      public void draggerMove (Dragger3dEvent e) {
         RigidTransform3d TGW = new RigidTransform3d();
         RigidTransform3d T = (RigidTransform3d)e.getTransform();
         TGW.mul (TGW0, T);
         setGridToWorld (TGW);
      }
   }

   public void setDragger (DraggerType type) {
      if (type != getDragger()) {
         if (myDragger != null && myViewer != null) {
            myViewer.removeDragger (myDragger);
         }
         switch (type) {
            case None: {
               myDragger = null;
               break;
            }
            case Translator: {
               myDragger = new Translator3d();
               break;
            }
            case Transrotator: {
               myDragger = new Transrotator3d();
               if (myLockAxesToWorld) {
                  ((Transrotator3d)myDragger).setConstrainer (
                     new AlignConstrainer());
               }
               break;
            }
            case Rotator: {
               myDragger = new Rotator3d();
               break;
            }
            default: {
               throw new IllegalArgumentException (
                  "dragger " + type + " not supported for clipping plane");
            }
         }
         if (myDragger != null) {
            myDragger.setDraggerToWorld (XGridToWorld);
            myDragger.addListener (new MyDraggerListener ());
         }
         if (myViewer != null) {
            if (myDragger != null) {
               myViewer.addDragger (myDragger);
            }
            myViewer.repaint();
         }
      }
   }

   public boolean isGridVisible() {
      return myGridVisible;
   }

   public void setGridVisible (boolean enable) {
      myGridVisible = enable;
      if (myViewer != null) {
         myViewer.repaint();
      }
   }

   public void render (Renderer renderer, int flags) {
      if (myDragger != null && myViewer != null) {
         // double w = myViewer.centerDistancePerPixel()*myViewer.getWidth();
         myDragger.setSize (myMinSize / 4);
         myDragger.render (renderer, 0);
      }
      if (myGridVisible) {
         drawGrid (renderer);
      }
   }

   public void getPlane (Plane plane) {
      RigidTransform3d X = XGridToWorld;
      plane.set (-X.R.m02, -X.R.m12, -X.R.m22, -X.R.m02 * X.p.x - X.R.m12
         * X.p.y - X.R.m22 * X.p.z);
   }

   public Plane getPlane() {
      Plane plane = new Plane();
      getPlane (plane);
      return plane;
   }

   public void getPlaneValues (double[] vals) {
      RigidTransform3d X = XGridToWorld;
      vals[0] = -X.R.m02;
      vals[1] = -X.R.m12;
      vals[2] = -X.R.m22;
      vals[3] = X.R.m02 * X.p.x + X.R.m12 * X.p.y + X.R.m22 * X.p.z;
   }

   public int getPlaneValues (float[] vals, int offset) {
      RigidTransform3d X = XGridToWorld;
      vals[offset++] = (float)(-X.R.m02);
      vals[offset++] = (float)(-X.R.m12);
      vals[offset++] = (float)(-X.R.m22);
      vals[offset++] = (float)(X.R.m02 * X.p.x + X.R.m12 * X.p.y + X.R.m22 * X.p.z);
      return offset;
   }

   public void centerInViewer() {
      if (myViewer != null) {
         RigidTransform3d X = new RigidTransform3d (XGridToWorld);
         X.p.set (myViewer.getCenter());
         setGridToWorld (X);
      }
   }

   public void resetInViewer() {
      if (myViewer != null) {
         setGridToWorld (myViewer.getCenterToWorld());
      }
   }

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

   public Color getMajorColor() {
      return createColor (myMajorRGB);
   }

   public void setMajorColor (Color color) {
      if (color == null) {
         throw new IllegalArgumentException ("color must not be null");
      }
      myMajorRGB = createRGB (color);
   }

   public boolean getUseWorldOrigin() {
      return myUseWorldOrigin;
   }
   
   public void setUseWorldOrigin (boolean enable) {
      if (!myLockAxesToWorld) {
         enable = false;
      }
      myUseWorldOrigin = enable;
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

   public void setLabelSize (double emSize) {
      myLabelSize = emSize;
   }
   
   public double getLabelSize () {
      return myLabelSize;
   }
   
   public void setLabelColor (Color color) {
      myLabelRGB = createRGB(color);
   }
   
   public Color getLabelColor () {
      return createColor (myLabelRGB);
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

   public Color getMinorColor() {
      return createColor (myMinorRGB);
   }

   public void setMinorColor (Color color) {
      myMinorRGB = createRGB (color);
   }

   private void computeFocalPoint (
      Point3d focus, Plane plane, double dirx, double diry, double dirz) {
      plane.intersectLine (
         focus, new Vector3d (dirx, diry, dirz), new Point3d());
   }

   private void intersectBoundary (
      ConvexPoly2d poly, RigidTransform3d XGridToEye,
      double nx, double ny, double nz, double d) {

      Plane plane = new Plane (nx, ny, nz, d);
      if (XGridToEye != RigidTransform3d.IDENTITY) {
         plane.inverseTransform (XGridToEye);
      }
      Vector3d u = new Vector3d();
      Point3d p = new Point3d();
      Plane polyPlane = new Plane (0, 0, 1, 0);
      if (polyPlane.intersectPlane (p, u, plane)) {
         poly.intersectHalfPlane (u.y, -u.x, p.x*u.y - p.y*u.x);
      }
   }

   private class GridCentroidComputer {

      double myZmax;
      double myZmin;
      double mySize;
      double myNear;
      double myFar;

      GridCentroidComputer (double size, double near, double far) {
         mySize = size;
         myNear = near;
         myFar = far;
      }

      double getZmin() {
         return myZmin;
      }

      double getZmax() {
         return myZmax;
      }

      boolean computeCentroid (
         Point3d cent, RigidTransform3d XGridToEye, double vw, double vh) {

         ConvexPoly2d gridBoundary = new ConvexPoly2d();
         gridBoundary.setTolerance (mySize*1e-8);

         // create grid boundary in eye coordinates
         gridBoundary.addVertex (mySize/2, mySize/2);
         gridBoundary.addVertex (-mySize/2, mySize/2);
         gridBoundary.addVertex (-mySize/2, -mySize/2);
         gridBoundary.addVertex (mySize/2, -mySize/2);

         // intersect with near and far planes
         intersectBoundary (gridBoundary, XGridToEye, 0, 0, -1, myNear);
         intersectBoundary (gridBoundary, XGridToEye, 0, 0, 1, -myFar);

         if (gridBoundary.isEmpty()) {
            return false;
         }

         // Project into the view plane and finish clipping there. Record zmin and
         // zmax in case they are needed later to determine focus in degenerate
         // sitiations.
         myZmin = Double.POSITIVE_INFINITY;
         myZmax = Double.NEGATIVE_INFINITY;
         Vertex2d vtx = gridBoundary.firstVertex();
         Point3d peye = new Point3d();
         do {
            peye.set (vtx.pnt.x, vtx.pnt.y, 0);
            peye.transform (XGridToEye);
            double z = peye.z;
            if (z < myZmin) {
               myZmin = z;
            }
            if (z > myZmax) {
               myZmax = z;
            }
            //peye.x *= (-near / z);
            //peye.y *= (-near / z);
            //peye.z = -near;
            vtx.pnt.x = peye.x*(-myNear / z);
            vtx.pnt.y = peye.y*(-myNear / z);
            vtx = vtx.next;
         }
         while (vtx != gridBoundary.firstVertex());
         gridBoundary.setTolerance (vw * 1e-8);

         RigidTransform3d IDENTITY = RigidTransform3d.IDENTITY;

         intersectBoundary (gridBoundary, IDENTITY, -1, 0, 0, -vw / 2);
         intersectBoundary (gridBoundary, IDENTITY, 1, 0, 0, -vw / 2);
         intersectBoundary (gridBoundary, IDENTITY, 0, -1, 0, -vh / 2);
         intersectBoundary (gridBoundary, IDENTITY, 0, 1, 0, -vh / 2);

         if (gridBoundary.isEmpty()) {
            return false;
         }

         Point2d cent2d = new Point2d();
         gridBoundary.computeCentroid (cent2d);
         cent.set (cent2d.x, cent2d.y, -myNear);
         return true;
      }
   }

   /**
    * Given a plane expressed in eye coordinates, compute a representative focal
    * point (in eye coordinates) for this plane, and return the distance
    * per-pixal for this focal point. Returning -1 means that the plane is
    * invisible.
    * 
    * <p>
    * The method works by finding the centroid of the (clipped) polygon
    * associated with the grid boundary, as seen in screen coordinates. This
    * centroid is the projected back onto the plane.
    */
   private double computeFocalPoint (
      Point3d focus, RigidTransform3d TGW, Renderer renderer) {

      RigidTransform3d XGridToEye = new RigidTransform3d();
      RigidTransform3d TWorldToEye = renderer.getViewMatrix();
      XGridToEye.mul (TWorldToEye, TGW);

      if (focus == null) {
         focus = new Point3d();
      }
      if (renderer.isOrthogonal()) {
         Plane plane = new Plane();
         plane.set (XGridToEye);
         computeFocalPoint (focus, plane, 0, 0, 1);
         return renderer.getViewPlaneWidth() / renderer.getScreenWidth();
      }
      double near = renderer.getViewPlaneDistance();
      double far = renderer.getFarPlaneDistance();
      double vw = renderer.getViewPlaneWidth();
      double vh = renderer.getViewPlaneHeight();
      // double fov = renderer.getFieldOfViewY();

      int height = renderer.getScreenHeight();
      double nearDistPerPixel = renderer.getViewPlaneHeight() / height;

      GridCentroidComputer newComp =
      new GridCentroidComputer (myMinSize, near, far);

      Point3d cent = new Point3d();
      if (!newComp.computeCentroid (cent, XGridToEye, vw, vh)) {
         return -1;
      }
      double zmax = newComp.getZmax();
      double zmin = newComp.getZmin();

      RotationMatrix3d R = XGridToEye.R;
      Plane plane =
      new Plane (new Vector3d (R.m02, R.m12, R.m22), new Point3d (
         XGridToEye.p));
      double s = plane.intersectLine (focus, cent, Point3d.ZERO);
      if (s == Double.POSITIVE_INFINITY) {
         focus.scale (-(zmin + zmax) / (2 * near), cent);
      }
      double zref = -focus.z;

      return zref / near * nearDistPerPixel;
   }

   String createLabel (double cellSize, int n) {
      double log10 = (int)Math.log10(cellSize);
      int exp = (int)Math.floor(log10);
      int base = (int)Math.round(Math.pow(10,log10-exp));
      return createLabel (base, exp, n);
   }

   String createLabel (int base, int exp, int n) {

      if (n == 0) {
         return "0";
      }
      StringBuilder sbuild = new StringBuilder();
      sbuild.append (Integer.toString(Math.abs(n*base)));
      int len = sbuild.length();
      if (exp > 3 || exp < -4) {
         // find the number of trailing zeros
         int numz = 0;
         for (int i=len-1; i>=0 && sbuild.charAt(i)=='0'; i--) {
            numz++;
         }
         if (numz > 0) {
            sbuild.setLength (len-numz);
            exp += numz;
            len -= numz;
         }
         if (len > 1) {
            sbuild.insert (1, '.');
            exp += (len-1);
         }
         sbuild.append ('e');
         sbuild.append (exp);
      }
      else {
         if (exp > 0) {
            // add trailing zeros
            for (int i=0; i<exp; i++) {
               sbuild.append ('0');
            }
         }
         else if (exp < 0) {
            // // remove trailing zeros if necessary
            // int numz = 0;
            // for (int i=len-1; i>=0 && sbuild.charAt(i)=='0' && numz < -exp; i--) {
            //    numz++;
            // }
            // if (numz > 0) {
            //    sbuild.setLength (len-numz);
            //    exp += numz;
            //    len -= numz;
            // }
            int didx = len+exp; // index of decimal point
            while (didx < 0) {
               // add leading zeros
               sbuild.insert (0, '0');
               didx++;
            }              
            sbuild.insert (didx, '.');
         }
      }
      if (n < 0) {
         sbuild.insert (0, '-');
      }
      return sbuild.toString();
   }      

   private GLGridResolution updateResolution (double distPerPixel) {
      int[] factors = new int[2];
      double minorSize = Round.up125 (factors, myMinCellPixels*distPerPixel);
      int numDivisions;
      int k = factors[0];
      if (k == 5) {
         numDivisions = 10;
      }
      else {
         numDivisions = (int)Math.round(10.0/k);
      }
      double majorSize = numDivisions*minorSize;     
      return new GLGridResolution (majorSize, numDivisions);
   }

   private float[] getWorldAxisColor (double ux, double uy, double uz) {
      Vector3d uvec = new Vector3d (ux, uy, uz);
      switch (uvec.maxAbsIndex()) {
         case 0: return myXAxisRGB;
         case 1: return myYAxisRGB;
         case 2: return myZAxisRGB;
         default: {
            throw new InternalErrorException (
               "maxAbsIndex for a 3-vector is "+uvec.maxAbsIndex());
         }
      }
   }

   private void drawGrid (Renderer renderer) {
      
      if (myMinSize == 0) {
         return;
      }

      double distPerPixel = computeFocalPoint (null, XGridToWorld, renderer);
      if (distPerPixel == -1) { // grid is invisible
         return;
      }

      if (myAutoSizedP) {
         // start by computing the minor spacing, and then compute the major
         // spacing assume 10 cell divisions
         GLGridResolution res = updateResolution (distPerPixel);
         if (!myResolution.equals (res)) {
            if (myUseWorldOrigin) {
               // May need to change position to accomodate new resolution.
               RigidTransform3d TGWnew =
                  new RigidTransform3d (XGridToWorldTarget);
               AlignConstrainer aligner = new AlignConstrainer();
               aligner.updatePose (TGWnew, XGridToWorldTarget);
               double dpp = computeFocalPoint (null, TGWnew, renderer);
               GLGridResolution resx = updateResolution (dpp);
               if (resx.equals (res)) {
                  myResolution.set (res);
                  setGridToWorld (TGWnew);
               }
               else {
                  //System.out.println ("cycle detected");
               }
               // Old code without cycle detection
               //myResolution.set (res);
               //setGridToWorld (XGridToWorldTarget);
            }
            else {
               myResolution.set (res);
            }
         }
      }

      double majorSize = myResolution.getMajorCellSize();
      int numDivisions = myResolution.getNumDivisions();
      double minorSize; // minor cell size
      int halfCellCnt; // number of minor cells on each side of the x and y axes

      minorSize = majorSize / numDivisions;
      halfCellCnt = (int)Math.ceil (myMinSize/(2*minorSize));
      if (halfCellCnt == 0) {
         halfCellCnt = numDivisions;
      }
      else if (halfCellCnt%numDivisions != 0) {
         // increase effective size so grid boundary lies on a major divison
         halfCellCnt = numDivisions*(halfCellCnt/numDivisions+1);
      }

      // Limit total number of cells that are drawn, in case a sideways view
      // produces many subdivisions.
      int maxCells = 200;
      if (halfCellCnt > maxCells) {
         halfCellCnt = maxCells;
      }

      double halfSize = halfCellCnt*minorSize;

      int xcnt = 2 * halfCellCnt + 1; // number of x axis cell divisions
      double xmax = halfSize; // maximum x value
      double xmin = -xmax; // minimum x value
      int ycnt = 2 * halfCellCnt + 1; // number of y axis cell divisions
      double ymax = halfSize; // maximum y value
      double ymin = -ymax; // minimum y value

      // y_axis_i and x_axis_j are the cell division indices associated with
      // the grid's x and y axes
      double xoff = 0;
      double yoff = 0;
      if (myUseWorldOrigin) {
         Point3d center = new Point3d();
         // compute offset so the axes correspond to the real world center
         center.inverseTransform (XGridToWorld);
         xoff = center.x;
         yoff = center.y;
      }     
      int y_axis_i = (int)Math.round ((xmax+xoff) / minorSize);
      int x_axis_j = (int)Math.round ((ymax+yoff) / minorSize);

      float[] minorRGB = new float[3];
      if (myMinorRGB == null) {
         // create a default minor RBG 
         float s = 0.4f;
         for (int i=0; i<3; i++) {
            minorRGB[i] = s*myMajorRGB[i];
         }
         if (myViewer != null) {
            // blend with background color of viewer
            float[] backgroundRGB = 
            myViewer.getBackgroundColor().getRGBColorComponents(null);
            for (int i=0; i<3; i++) {
               minorRGB[i] += (1-s)*backgroundRGB[i]; 
            }
         }
      }
      else {
         minorRGB = myMinorRGB;
      }

      float[] xColor = myXAxisRGB;
      float[] yColor = myYAxisRGB;
      if (myLockAxesToWorld) {
         // x and y axis colors are determined from corresponding world axes
         RotationMatrix3d R = XGridToWorld.R;
         // world x axis extracted from first column of XGridToWorld.R
         xColor = getWorldAxisColor (R.m00, R.m10, R.m20);
         // world y axis extracted from second column of XGridToWorld.R
         yColor = getWorldAxisColor (R.m01, R.m11, R.m21);
      }
      

      //*********************************************************
      // Old code that adjusted the intensity of the minor axis lines depending
      // on how close we were to changing the grid resolution. This gave a
      // smooth transition between different resolutions when
      // autosizing. Difficult to get right (and not as necessary) with the
      // more complex autosizing algorithm.
      //
      // // minSpc is the minimum possible value for minorSize. Maximum possible
      // // value is 10x this, and the difference is used to determine the minor
      // // grid color
      // double minSpc = myMinCellPixels * distPerPixel;
      //
      // if (myAutoSizingP) {
      //    float s = (float)(minorSize / (10 * minSpc));
      //    for (int i = 0; i < 3; i++) {
      //       myMinorRGB[i] = (0.2 + 0.8 * s)*myDivisionRGB[i]
      //    }
      // }
      //*********************************************************

      // maybe update info
      rcacheInfo.update(
         numDivisions, (float)minorSize, xcnt, ycnt, x_axis_j, y_axis_i, 
         (float)xmin, (float)xmax, (float)ymin, (float)ymax,
         xColor, yColor, myMajorRGB, minorRGB);

      Shading savedShading = renderer.setShading (Shading.NONE);
      renderer.setLineWidth(myLineWidth);

      renderer.pushModelMatrix();
      renderer.mulModelMatrix(XGridToWorld);

      renderer.drawLines (rcacheInfo.getRenderObject());

      renderer.setDepthOffset (1);
      if (myXAxisLabeling != AxisLabeling.OFF ||
          myYAxisLabeling != AxisLabeling.OFF) {
         drawAxisLabels (
            renderer, xcnt, ycnt, majorSize, numDivisions, distPerPixel);
      }

      renderer.popModelMatrix();
      renderer.setLineWidth(1);
      renderer.setShading (savedShading);
   }

   private float[] getEffectiveLabelColor() {
      if (myLabelRGB != null) {
         return myLabelRGB;
      }
      else {
         return myMajorRGB;
      }
      // if (myViewer != null) {
      //    float[] bg = myViewer.getBackgroundColor().getRGBColorComponents(null);
      //    if (bg[0] + bg[1] + bg[2] >= 1.5) {
      //       return Color.BLACK;
      //    }
      //    else {
      //       return Color.WHITE;
      //    }
      // }
      // else {
      //    return Color.WHITE;
      // }
   }
   
   private void drawAxisLabels (
      Renderer renderer, int xcnt, int ycnt, 
      double majorSize, int numDivisions, double pixelSize) {

      double log10 = Math.log10(majorSize);
      int exp = (int)Math.floor(log10);
      int base = (int)Math.round(Math.pow(10,log10-exp));

      renderer.setColor (getEffectiveLabelColor());

      // minimum indices of the major cell divisions along x and y.
      // maximum indices are the negatives of these
      int min_x_idx = -(xcnt-1)/(2*numDivisions);
      int min_y_idx = -(ycnt-1)/(2*numDivisions);

      // Find offset of grid center relative to viewer center. (These aren't
      // necessarily the same because of the need to align grid with major
      // divisions.)
      Point3d goff = new Point3d(renderer.getCenter());
      goff.inverseTransform (XGridToWorld);

      double sw = renderer.getScreenWidth()*pixelSize;
      double sh = renderer.getScreenHeight()*pixelSize;
      // Find indices of major divsions along which to draw x and y axis
      // labels.  These are the lowest and leftmost divisions that are visible
      // in the viewer, with an extra tolerance (0.25*majorSize) to allow room
      // for the labels themselves.
      int x_label_idx = Math.max(-(int)((sh/2-goff.y)/majorSize-0.25),min_y_idx);
      int y_label_idx = Math.max(-(int)((sw/2-goff.x)/majorSize-0.25),min_x_idx);
      double emSize = myLabelSize*pixelSize;

      // Find cordinates at grid center
      Point3d gcenter = new Point3d();
      Vector3d axisDirs = new Vector3d(1, 1, 1);
      if (myUseWorldOrigin) {
         gcenter.inverseTransform (XGridToWorld.R, XGridToWorld.p);
         axisDirs.inverseTransform (XGridToWorld.R, axisDirs);
      }

      if (myXAxisLabeling != AxisLabeling.OFF) {
         Vector3d pos = new Vector3d();
         double y = x_label_idx*majorSize-emSize;
         int izero = (int)Math.round(-gcenter.x/majorSize);
         int sgnx = axisDirs.x > 0 ? 1 : -1;
         int x_min = Math.max(y_label_idx-1,min_x_idx);
         int x_max = Math.min((int)(x_min+sw/majorSize+0.5),-min_x_idx);
         for (int i=x_min; i<=x_max; i++) {
            String label = createLabel (base, exp, sgnx*(i-izero));
            pos.set (i*majorSize+2*pixelSize, y, 0);
            renderer.drawText (label, pos, emSize);
         }
      }
      if (myYAxisLabeling != AxisLabeling.OFF) {
         Vector3d pos = new Vector3d();
         double x = y_label_idx*majorSize-2*pixelSize;
         int jzero = (int)Math.round(-gcenter.y/majorSize);
         int sgny = axisDirs.y > 0 ? 1 : -1;
         int y_min = Math.max(x_label_idx-1,min_y_idx);
         int y_max = Math.min((int)(y_min+sh/majorSize+0.5),-min_y_idx);
         for (int j=y_min; j<=y_max; j++) {
            String label = createLabel (base, exp, sgny*(j-jzero));
            Rectangle2D bounds = renderer.getTextBounds (
               renderer.getDefaultFont(), label, emSize);
            double y = j*majorSize+2*pixelSize;
            pos.set (x-bounds.getWidth(), y, 0);
            renderer.drawText (label, pos, emSize);
         }
      }
   }

   public static void main (String[] args) {
      GLGridPlane gp = new GLGridPlane();

      System.out.println (gp.createLabel (10, -2, 1));
      System.out.println (gp.createLabel (1, -1, 1));

      for (int i=-7; i<=7; i++) {
         double cellSize = Math.pow (10, i);
         System.out.println (gp.createLabel (cellSize, 1));
      }
   }

}

class LineSegment {
   /**
    * Line segment defined by p0 + s*u, where u is a unit vector. Segment
    * endpoints are defined by s1 and s2, and the length is s2 - s1. If s2 < s1,
    * the segment is assumed to be empty.
    */
   Point3d p0;
   Point3d px;
   Vector3d u;
   double s1, s2;

   LineSegment (Point3d p1, Point3d p2) {
      p0 = new Point3d (p1);
      px = new Point3d();
      u = new Vector3d();
      u.sub (p2, p1);
      s1 = 0;
      s2 = u.norm();
      if (s2 != 0) {
         u.scale (1 / s2);
      }
   }

   void getPoint1 (Point3d p1) {
      p1.scaledAdd (s1, u, p0);
   }

   void getPoint2 (Point3d p2) {
      p2.scaledAdd (s2, u, p0);
   }

   void addPoint1 (Vector3d sum) {
      sum.scaledAdd (s1, u);
      sum.add (p0);
   }

   void addPoint2 (Vector3d sum) {
      sum.scaledAdd (s2, u);
      sum.add (p0);
   }

   boolean isEmpty() {
      return s2 < s1;
   }

   double length() {
      return s2 - s1;
  }

   // private static double DOUBLE_PREC = 2e-16;

   /**
    * Intersect this line segment with a half space defined by
    * 
    * <pre>
    *  T
    * n  x - off &gt;= 0
    * </pre>
    */
   void clipWithPlane (double nx, double ny, double nz, double off) {
      if (isEmpty()) {
         return;
      }
      getPoint1 (px);
      double d1 = nx * px.x + ny * px.y + nz * px.z - off;
      getPoint2 (px);
      double d2 = nx * px.x + ny * px.y + nz * px.z - off;
      if (d1 < 0 && d2 < 0) { // completely clipped
         s1 = 1;
         s2 = -1;
      }
      else if (d1 * d2 < 0) {
         double s = (d2 * s1 - d1 * s2) / (d2 - d1);
         // Parnoid: clip s to [s1,s2]
         if (s < s1) {
            s = s1;
         }
         else if (s > s2) {
            s = s2;
         }
         if (d1 < 0) {
            s1 = s;
         }
         else // d2 < 0
         {
            s2 = s;
         }
      }
   }

}
