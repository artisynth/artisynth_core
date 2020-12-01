/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.workspace;

import java.awt.Color;
import java.awt.event.MouseEvent;

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
import maspack.render.Renderer.LineStyle;
import maspack.render.GL.GLViewer;
import maspack.properties.*;
import maspack.util.*;

/**
 * Controller that allows a drag motion in the GUI to exert an interactive
 * force on certain GUI components.
 */
public class PullController extends ControllerBase
implements SelectionListener, MouseInputListener {

   private MouseRayEvent myPullEvent = null;
   private SelectionManager mySelectionManager;
   private double myRootRadius = 0; // current root model default radius
   private Main myMain;

   private double myStiffness = DEFAULT_STIFFNESS;
   private boolean myStiffnessExplicitlySetP = false;

   // myComponent, myBodyPnt and myPullPos are shared between the GUI thread
   // (where they are set from mouse and select actions) and the scheduler
   // thread, where they are used to compute the applied force. Access to these
   // hence needs to be synchronized
   private ModelComponent myComponent = null;
   private Point myPoint;
   private PointAttachment myAttachment;

   // private Point3d myBodyPnt;
   private Object myOriginData;
   private Point3d myPullPos;
   // default render radius for a point that is being pulled
   private double myPointRenderRadius = 0;
   // indicates that the component being pulled should be persistent between drags
   private boolean myHasPersistentComponent = false;
   // indicates that a drag happened between a mouse press and release
   private boolean myDragOccurred = false;
   // indicates that mouse button one was pressed and dragging is enables
   private boolean myDragEnabled = false;

   private static double DEFAULT_STIFFNESS = 10;

   public static PropertyList myProps =
      new PropertyList (PullController.class, ControllerBase.class);

   static {
      myProps.add (
         "renderProps", "render properties", createDefaultRenderProps());
      myProps.add ("stiffness", "spring stiffness", DEFAULT_STIFFNESS);
   }   

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getStiffness() {
      return myStiffness;
   }

   public void setStiffness (double k) {
      if (k < 0) {
         k = computeDefaultStiffness();
      }
      else {
         myStiffnessExplicitlySetP = true;
      }
      myStiffness = k;
   }

   public PullController (SelectionManager selManager) {
      myMain = Main.getMain();
      myRenderProps = createDefaultRenderProps();
      mySelectionManager = selManager;
      setName ("pullController");
   }

   private double searchForExplicitPointRadius (ModelComponent comp) {
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

   public void clear() {
      clearComponent();
      myHasPersistentComponent = false;
   }
   
   private void clearComponent() {
      myComponent = null;
      myPoint = null;
      myAttachment = null;
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

   private void initializeComponent (ModelComponent comp, MouseRayEvent rev) {
      clearComponent();

      if (comp instanceof PointAttachable && comp instanceof HasSurfaceMesh) {
         Point3d isect = new Point3d();
         if (computeRayIntersection (isect, (HasSurfaceMesh)comp, rev.getRay())) {
            Point pnt = new Point (isect);
            PointAttachment pa =
               ((PointAttachable)comp).createPointAttachment (pnt);
            if (pa != null) {
               myComponent = comp;
               myAttachment = pa;
               myPoint = pnt;
            }
         }
      }
      else if (comp instanceof Point) {
         myComponent = comp;
         myPoint = (Point)comp;
         myAttachment = null;
      }
      else if (comp instanceof Frame) {
         Frame frame = (Frame)comp;
         Point3d pos = new Point3d(frame.getPose().p);
         Point pnt = new Point(pos);
         PointAttachment pa = 
            ((PointAttachable)comp).createPointAttachment (pnt);
         myComponent = comp;
         myAttachment = pa;
         myPoint = pnt;         
      }
      if (myComponent != null) {
         myPointRenderRadius = searchForExplicitPointRadius (myComponent);
         MechSystem mech = MechSystemBase.topMechSystem (myComponent);
         if (mech instanceof Model && mech != myModel) {
            setModel ((Model)mech);
         }
      }
      
   }

   private void applyForce (Vector3d force) {
      if (myAttachment != null) {
         // point is our our privately created point
         myPoint.setForce (force);
         myAttachment.applyForces();
      }
      else {
         // point is contained in the model and forces will
         // have been zeroed in preadvance
         myPoint.addForce (force);
      }
   }

   public Point3d getPointPosition() {
      if (myPoint != null) {
         if (myAttachment != null) {
            myAttachment.updatePosStates();
         }
         return myPoint.getPosition();
      }
      else {
         return null;
      }
   }

   public synchronized void selectionChanged (SelectionEvent e) {

      if (myPullEvent != null) {
         ModelComponent comp = e.getLastAddedComponent();
         if (comp != null) {
            initializeComponent (comp, myPullEvent);
         }
         myPullEvent = null;
      }
   }

   public synchronized void mouseClicked (MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
         myPullPos = null;
         if (myComponent != null) {
            if (e.getClickCount() >= 2) {
               if (myHasPersistentComponent) {
                  // remove the component
                  clearComponent();
                  myHasPersistentComponent = false;
               }
               else {
                  myHasPersistentComponent = true;
               }
               if (!myMain.isSimulating()) {
                  myMain.rerender();
               }                
            }
            else if (!myHasPersistentComponent) {
               clearComponent();
               if (!myMain.isSimulating()) {
                  myMain.rerender();
               }                
            }
         }
      }
   }

   public synchronized void mousePressed (MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
         mySelectionManager.clearSelections();
         GLViewer viewer =
            ViewerManager.getViewerFromComponent (e.getComponent());
         if (myComponent == null) {
            myPullEvent = MouseRayEvent.create (e, viewer);
         }
         myDragOccurred = false;
         myDragEnabled = true;
      }
   }

   public synchronized void mouseDragged (MouseEvent e) {
      if (myDragEnabled) {
         GLViewer viewer =
            ViewerManager.getViewerFromComponent (e.getComponent());            
         if (myComponent != null) {
            // find the world space point
            myPullPos = EditorUtils.intersectWithPlane (
               getPointPosition(), MouseRayEvent.create (e, viewer));

            if (!myMain.isSimulating()) {
               myMain.rerender();
            }     
         }
         myDragOccurred = true;
      }
   }

   public synchronized void mouseReleased (MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
         if (myComponent != null) {
            applyForce (Vector3d.ZERO);
         }
         myPullEvent = null;
         myPullPos = null;
         if (myDragOccurred && !myHasPersistentComponent) {
            clearComponent();
         }
         if (!myMain.isSimulating()) {
            myMain.rerender();
         }     
      }
      myDragEnabled = false;
   }

   public void mouseMoved (MouseEvent e) {
   }

   public void mouseEntered (MouseEvent e) {
   }

   public void mouseExited (MouseEvent e) {
   }

   // IsRenderable implementation
   public void prerender (RenderList list) {
      if (myAttachment != null) {
         myAttachment.updatePosStates();
         myPoint.prerender (list);
      }
   }

   public void render (Renderer renderer, int flags) {

      if (myComponent != null) {
         if (myPullPos != null || myHasPersistentComponent) {
            // render the pull point with a slightly larger radius
            // than any underlying point so we can see it
            double saveRadius = myRenderProps.getPointRadius();
            PropertyMode saveRadiusMode = myRenderProps.getPointRadiusMode();

            if (saveRadius <= myPointRenderRadius) {
               myRenderProps.setPointRadius (1.05*myPointRenderRadius);
            }
            renderer.drawPoint (
               myRenderProps, myPoint.myRenderCoords, false);
            if (saveRadius <= myPointRenderRadius) {
               if (saveRadiusMode == PropertyMode.Inherited) {
                  myRenderProps.setPointRadiusMode (saveRadiusMode);
               }
               else {
                  myRenderProps.setPointRadius (saveRadius);
               }
            }
         }
         if (myPullPos != null) {
            float[] pullCoords = new float[3];
            pullCoords[0] = (float)myPullPos.x;
            pullCoords[1] = (float)myPullPos.y;
            pullCoords[2] = (float)myPullPos.z;
            renderer.drawLine (
               myRenderProps, myPoint.myRenderCoords, pullCoords, false);
         }
      }
   }

   public RenderProps createRenderProps() {
      return RenderProps.createPointLineProps (this);
   }   

   public static RenderProps createDefaultRenderProps() {
      RenderProps props = new PointLineRenderProps();
      props.setLineWidth (2);
      props.setLineColor (Color.BLUE);
      props.setPointStyle (PointStyle.POINT);
      props.setPointSize (2);
      props.setPointColor (Color.BLUE);
      return props;
   }   

   public boolean isSelectable() {
      return false;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   @Override
   public void initialize (double t0) {
   }
   
   public synchronized void apply (double t0, double t1) {
      if (myComponent != null && myPullPos != null) {
         Vector3d force = new Vector3d();
         force.sub (myPullPos, getPointPosition());
         force.scale (getStiffness());
         applyForce (force);
      }
   }

   protected double computeDefaultStiffness () {

      double mass = 0;
      double gravityStiffness = 0;
      double accelStiffness = 0;
      Model model = getModel();

      if (model instanceof MechSystemBase) {
         mass = ((MechSystemBase)model).getActiveMass();
      }
      if (model instanceof MechModel) {
         double g = ((MechModel)model).getGravity().norm();
         if (g > 0) {
            // enough to suspend entire model against gravity
            gravityStiffness = g*mass/myRootRadius;
         }
      }
      if (mass > 0) {
         // enough accelerate whole model to a velocity of radius/sec in 2 sec
         // accel = radius/2 => force = mass*radius/2 => K radius = mass*radius/2
         accelStiffness = mass/2;
      }
      // choose the maximum of the two values, or default to 10.0
      if (accelStiffness > 0 || gravityStiffness > 0) {
         return (Math.max (accelStiffness, gravityStiffness));
      }
      else {
         return (DEFAULT_STIFFNESS);
      }      
   }

   /**
    * Should be called whenever this pull controller is assigned to a RootModel
    */
   public void setRootModelDefaults (RootModel root) {
      double radius = RenderableUtils.getRadius (root);
      if (radius > 0) {
         myRenderProps.setPointStyle (PointStyle.SPHERE);
         myRenderProps.setPointRadius (0.02*radius);
         myRenderProps.setLineRadius (0.01*radius);
         myRenderProps.setLineStyle (LineStyle.SOLID_ARROW);
      }
      myRootRadius = radius;
      myStiffnessExplicitlySetP = false;
   }

   public void setModel (Model model) {
      super.setModel (model);
      if (!myStiffnessExplicitlySetP) {
         myStiffness = computeDefaultStiffness();
      }
   }

   /** 
    * Returns false; prevents this component from being writing out as
    * part of a RootModel file.
    */
   public boolean isWritable() {
      return false;
   }

}
