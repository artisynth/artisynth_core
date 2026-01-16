/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.workspace;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.event.MouseInputListener;

import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.driver.Main;
import artisynth.core.driver.ViewerManager;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.gui.editorManager.EditorUtils;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.*;
import maspack.render.GL.GLViewer;
import maspack.properties.*;
import maspack.util.*;

/**
 * Tool that measures the distance between two selected points in the model.
 */
public class MeasurementTool extends RenderableComponentBase
implements SelectionListener, MouseInputListener {

   private static Color DEFAULT_RENDER_COLOR = new Color (0.2f, 0.2f, 1f);
   private Color myRenderColor = DEFAULT_RENDER_COLOR;

   // size of the default point render radius as a fraction of current screen
   // width
   private static double DEFAULT_RADIUS_RATIO = 0.003;
   private double myRadiusRatio = DEFAULT_RADIUS_RATIO;

   boolean textOnTop = false;
   boolean textOnRight = true;

   private class PointData {
      ModelComponent myComp;
      Point myPoint;
      PointAttachment myAttachment;
      double myRenderRadius;

      PointData (ModelComponent comp, Point point, PointAttachment pa) {
         myComp = comp;
         myPoint = point;
         myAttachment = pa;
      }
   }

   private MouseRayEvent myRayEvent = null;
   private SelectionManager mySelectionManager;
   private double myRootRadius = 0; // current root model default radius
   private Main myMain;

   private ArrayList<PointData> myPointData = new ArrayList<>();
   private PointData[] myRenderData = null;
   private String myRenderText;

   public static PropertyList myProps =
      new PropertyList (MeasurementTool.class, RenderableComponentBase.class);

   static {
      myProps.add (
         "renderColor", "color for points and the line between them", 
         DEFAULT_RENDER_COLOR);
      myProps.add (
         "radiusRatio", "point radius as a fraction of screen width", 
         DEFAULT_RADIUS_RATIO);
   }   

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /* ---- property accessors ---- */
   
   public Color getRenderColor() {
      return myRenderColor;
   }

   public void setRenderColor (Color color) {
      myRenderColor = color;
   }

   public double getRadiusRatio() {
      return myRadiusRatio;
   }

   public void setRadiusRatio (double ratio) {
      myRadiusRatio = ratio;
   }

   /* ---- end property accessors ---- */

   public MeasurementTool (SelectionManager selManager) {
      myMain = Main.getMain();
      myRenderProps = createDefaultRenderProps();
      mySelectionManager = selManager;
      setName ("measurementTool");
   }

   private double searchForExplicitPointRadius (ModelComponent comp) {
      if (comp instanceof Point) {
         while (comp != null) {
            if (comp instanceof Renderable) {
               RenderProps props = ((Renderable)comp).getRenderProps();
               if (props != null &&
                   props.getPointRadiusMode() == PropertyMode.Explicit) {
                  return props.getPointRadius();
               }
            }
            comp = comp.getParent();
         }
      }
      return 0;
   }      

   private double getRenderRadius (ModelComponent comp) {
      if (comp instanceof Renderable) {
         RenderProps props = ((Renderable)comp).getRenderProps();
         if (props == null) {
            if (comp.getParent() instanceof Renderable) {
               props = ((Renderable)comp.getParent()).getRenderProps();
            }
         }
         if (props != null) {
            if (props.getPointStyle() == PointStyle.SPHERE) {
               return props.getPointRadius();
            }
         }
      }
      return 0;
   }

   int numPoints() {
      return myPointData.size();
   }

   public void clear() {
      clearPoints();
   }
   
   private void clearLastPoint() {
      if (myPointData.size() > 1) {
         myPointData.remove (1);
      }
   }

   private void clearPoints() {
      myPointData.clear();
   }

   private boolean computeRayIntersection (
      Point3d nearest, HasSurfaceMesh comp, Line ray) {
      
      PolygonalMesh[] meshes = comp.getSurfaceMeshes();
      if (meshes == null || meshes.length == 0) {
         return false;
      }
      double nearestDistance = Double.POSITIVE_INFINITY;
      for (PolygonalMesh mesh : meshes) {
         Point3d pos = BVFeatureQuery.nearestPointAlongRay (
            mesh, ray.getOrigin(), ray.getDirection());
         if (pos != null) {
            double d = pos.distance(ray.getOrigin());
            if (d < nearestDistance) {
               nearestDistance = d;
               nearest.set (pos);
            }
         }
      }
      return nearestDistance != Double.POSITIVE_INFINITY;
   }

   private PointData initializeComponent (
      ModelComponent comp, MouseRayEvent rev) {
      PointData pdata = null;

      if (comp instanceof PointAttachable && comp instanceof HasSurfaceMesh) {
         Point3d isect = new Point3d();
         if (computeRayIntersection (isect, (HasSurfaceMesh)comp, rev.getRay())) {
            Point pnt = new Point (isect);
            PointAttachment pa =
               ((PointAttachable)comp).createPointAttachment (pnt);
            if (pa != null) {
               pdata = new PointData (comp, pnt, pa);
            }
         }
      }
      else if (comp instanceof Point) {
         pdata = new PointData (comp, (Point)comp, null);
      }
      else if (comp instanceof Frame) {
         Frame frame = (Frame)comp;
         Point3d pos = new Point3d(frame.getPose().p);
         Point pnt = new Point(pos);
         PointAttachment pa = 
            ((PointAttachable)comp).createPointAttachment (pnt);
         pdata = new PointData (comp, pnt, pa);
      }
      if (pdata != null) {
         pdata.myRenderRadius = searchForExplicitPointRadius (pdata.myComp);
      }
      return pdata;      
   }

   public synchronized void selectionChanged (SelectionEvent e) {

      if (myRayEvent != null) {
         ModelComponent comp = e.getLastAddedComponent();
         if (comp != null) {
            PointData pdata = initializeComponent (comp, myRayEvent);
            if (pdata != null) {
               if (numPoints() < 2) {
                  myPointData.add (pdata);
               }
               else {
                  myPointData.set (1, pdata);
               }
            }
         }
         myRayEvent = null;
      }
   }

   public synchronized void mouseClicked (MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
         if (numPoints() > 0 && e.getClickCount() >= 2) {
            clearPoints();
            if (!myMain.isSimulating()) {
               myMain.rerender();
            }                
         }
      }
   }

   public synchronized void mousePressed (MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
         mySelectionManager.clearSelections();
         Viewer viewer =
            ViewerManager.getViewerFromComponent (e.getComponent());
         myRayEvent = MouseRayEvent.create (e, viewer);
      }
   }

   public synchronized void mouseDragged (MouseEvent e) {
   }

   public synchronized void mouseReleased (MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
         myRayEvent = null;
         if (!myMain.isSimulating()) {
            myMain.rerender();
         }     
      }
   }

   public void mouseMoved (MouseEvent e) {
   }

   public void mouseEntered (MouseEvent e) {
   }

   public void mouseExited (MouseEvent e) {
   }

   // IsRenderable implementation
   public void prerender (RenderList list) {
      PointData[] renderData = null;
      if (numPoints() > 0) {
         for (PointData pdata : myPointData) {
            if (pdata.myAttachment != null) {
               pdata.myAttachment.updatePosStates();
               pdata.myPoint.prerender (list);
            }
         }
         renderData = myPointData.toArray (new PointData[0]);
         if (renderData.length == 2) {
            myRenderText = String.format (
               "%g", renderData[0].myPoint.distance (renderData[1].myPoint));
         }
      }
      myRenderData = renderData;
   }

   public void render (Renderer r, int flags) {

      if (myRenderData != null) {
         double distPerPixel = r.distancePerPixel(r.getCenter());
         double defaultRadius =
            r.getScreenWidth()*myRadiusRatio*distPerPixel;
         r.setColor (myRenderColor);
         for (PointData pdata : myRenderData) {
            Point3d renderPos = new Point3d (pdata.myPoint.myRenderCoords);
            // increase point radius by one pixel to ensure visibility
            double radius = Math.max (
               defaultRadius, pdata.myRenderRadius+r.distancePerPixel(renderPos));
            r.drawSphere (renderPos, radius);
         }
         if (myRenderData.length == 2) {
            // render a line between the two points

            float[] p0 = myRenderData[0].myPoint.myRenderCoords;
            float[] p1 = myRenderData[1].myPoint.myRenderCoords;
            r.setColor (myRenderColor);
            r.drawCylinder (p0, p1, 0.5*defaultRadius, /*capped*/false);

            double emSize =  0.035*r.getScreenHeight();
            double margin = 0.03*r.getScreenWidth();

            Shading savedShading = r.getShading();
            r.setShading (Shading.NONE);

            r.begin2DRendering();
            Color textColor;
            if (r instanceof Viewer) {
               textColor = 
                  getOppositeColor (((Viewer)r).getBackgroundColor());
            }
            else {
               textColor = Color.BLACK;
            }
            r.setColor (textColor);

            double x = margin;
            Rectangle2D bounds = r.getTextBounds (
               r.getDefaultFont(), myRenderText, emSize);
            if (textOnRight) {
               double w = bounds.getWidth();
               x = r.getScreenWidth() - margin - w;
            }
            double y;
            if (textOnTop) {
               y = r.getScreenHeight() - margin - bounds.getHeight();
            }
            else {
               y = margin;
            }
            Vector3d pos = new Vector3d (x, y, 0);
            r.drawText (myRenderText, pos, emSize);

            r.end2DRendering();
            r.setShading (savedShading);
         }
      }
   }

   Color getOppositeColor (Color color) {
      int r = color.getRed();
      int g = color.getGreen();
      int b = color.getBlue();
      r = (r > 127 ? 0 : 255);
      g = (g > 127 ? 0 : 255);
      b = (b > 127 ? 0 : 255);
      return new Color(r, g, b);
   }

   public RenderProps createRenderProps() {
      return RenderProps.createPointLineProps (this);
   }   

   public static RenderProps createDefaultRenderProps() {
      RenderProps props = new PointLineRenderProps();
      props.setLineWidth (1);
      props.setPointStyle (PointStyle.POINT);
      props.setPointSize (1);
      return props;
   }   

   public boolean isSelectable() {
      return false;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   /**
    * Should be called whenever this measurement tool is assigned to a
    * RootModel
    */
   public void setRootModelDefaults (RootModel root) {
      double radius = RenderableUtils.getRadius (root);
      if (radius > 0) {
         myRenderProps.setPointStyle (PointStyle.SPHERE);
         myRenderProps.setPointRadius (0.005*radius);
         myRenderProps.setLineRadius (0.002*radius);
         myRenderProps.setLineStyle (LineStyle.CYLINDER);
      }
      myRootRadius = radius;
   }

   /** 
    * Returns false; prevents this component from being writing out as
    * part of a RootModel file.
    */
   public boolean isWritable() {
      return false;
   }

}
