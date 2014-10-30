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
import maspack.render.*;
import maspack.properties.*;
import maspack.util.*;

public class PullController extends ControllerBase
implements SelectionListener, MouseInputListener {

//   /**
//    * Interface for handling pull events
//    */
//   public interface Pullable {
//
//      boolean isPullable();
//
//      /**
//       * Constructs force origin storage data given a mouse ray
//       * (e.g. intersect ray with mesh to determine for origin point)
//       * If null, assumes that there is no origin, so no force can
//       * be applied
//       */
//      public Object getOriginData(MouseRayEvent ray);
//
//      /**
//       * Determines the world-coordinate point to which force will
//       * be applied (used for determining magnitude of force)
//       */
//      public Point3d getOriginPoint(Object data);
//
//      public double getPointRenderRadius();
//
//      /**
//       * Given the supplied force origin info and a force vector,
//       * apply the force (typically sets an external force)
//       */
//      public void applyForce(Object orig, Vector3d force);
//   }


   private MouseRayEvent myPullEvent = null;
   private SelectionManager mySelectionManager;

   private double myStiffness = DEFAULT_STIFFNESS;

   // myComponent, myBodyPnt and myPullPos are shared between the GUI thread
   // (where they are set from mouse and select actions) and the scheduler
   // thread, where they are used to compute the applied force. Access to these
   // hence needs to be synchronized
   private ModelComponent myComponent = null;
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
      myStiffness = k;
   }

   public PullController (SelectionManager selManager) {
      myRenderProps = createDefaultRenderProps();
      mySelectionManager = selManager;
      setName ("pullController");
   }

   private double getPointRenderRadius (Point pnt) {
      RenderProps props = pnt.getRenderProps();
      if (props == null) {
         if (pnt.getParent() instanceof Renderable) {
            props = ((Renderable)pnt.getParent()).getRenderProps();
         }
      }
      if (props != null) {
         if (props.getPointStyle() == RenderProps.PointStyle.SPHERE) {
            return props.getPointRadius();
         }
      }
      return 0;
   }

   private void initializeComponent (ModelComponent comp, MouseRayEvent ray) {
      myComponent = null;
      if (comp instanceof Pullable) {

         Pullable p = (Pullable)comp;
         if (p.isPullable()) {
            myOriginData = p.getOriginData (
               ray.getRay().getOrigin(), ray.getRay().getDirection());
            if (myOriginData != null) {
               myComponent = comp;
               myPointRenderRadius = p.getPointRenderRadius();
            }
         }
      } else if (comp instanceof Point) {
         myComponent = comp;
         myPointRenderRadius = getPointRenderRadius ((Point)comp);
      }
      //      else if (comp instanceof RigidBody) {
      //         RigidBody bod = (RigidBody)comp;
      //         PolygonalMesh mesh = bod.getMesh();
      //         myPointRenderRadius = 0;
      //         if (mesh != null) {
      //            Point3d pnt = EditorUtils.intersectWithMesh (mesh, ray);
      //            if (pnt != null) {
      //               myBodyPnt = new Point3d(pnt);
      //               myBodyPnt.inverseTransform (bod.getPose());
      //               //myBodyPnt.setZero();
      //               myComponent = comp;
      //            }
      //         }
      //      }
   }

   private void applyForce (Vector3d force) {

      if (myComponent instanceof Pullable) {
         ((Pullable)myComponent).applyForce(myOriginData, force);
      } 
      else if (myComponent instanceof Point) {
         ((Point)myComponent).setExternalForce (force);
      }
      //      else if (myComponent instanceof RigidBody) {
      //         RigidBody body = (RigidBody)myComponent;
      //         Wrench bodyForce = new Wrench();
      //         body.computeAppliedWrench (bodyForce, force, myBodyPnt);
      //         bodyForce.transform (body.getPose().R);
      //         body.setExternalForce (bodyForce);
      //      }
      else {
         throw new InternalErrorException (
            "Unimplemented component type " + myComponent.getClass());
      }
   }

   private Point3d getOriginPoint() {

      if (myComponent instanceof Pullable) {
         return ((Pullable)myComponent).getOriginPoint(myOriginData);
      } 
      else if (myComponent instanceof Point) {
         return ((Point)myComponent).getPosition();
      }
      //      else if (myComponent instanceof RigidBody) {
      //         RigidBody body = (RigidBody)myComponent;
      //         Point3d pnt = new Point3d(myBodyPnt);
      //         pnt.transform (body.getPose());
      //         return pnt;
      //      }
      else {
         throw new InternalErrorException (
            "Unimplemented component type " + myComponent.getClass());
      }
   }   

   private float[] getOriginRenderCoords() {

      if (myComponent instanceof Pullable) {
         Point3d pnt = ((Pullable)myComponent).getOriginPoint(myOriginData);
         float[] coords = new float[3];
         coords[0] = (float)pnt.x;
         coords[1] = (float)pnt.y;
         coords[2] = (float)pnt.z;
         return coords;
      }
      else if (myComponent instanceof Point) {
         return ((Point)myComponent).myRenderCoords;
      }
      //      else if (myComponent instanceof RigidBody) {
      //         RigidBody body = (RigidBody)myComponent;
      //         Point3d pnt = new Point3d(myBodyPnt);
      //         pnt.transform (body.myRenderFrame);
      //         float[] coords = new float[3];
      //         coords[0] = (float)pnt.x;
      //         coords[1] = (float)pnt.y;
      //         coords[2] = (float)pnt.z;
      //         return coords;
      //      }
      else {
         throw new InternalErrorException (
            "Unimplemented component type " + myComponent.getClass());
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
            if (e.getClickCount() == 2) {
               if (myHasPersistentComponent) {
                  // remove the component
                  myComponent = null;
                  myHasPersistentComponent = false;
               }
               else {
                  myHasPersistentComponent = true;
               }
               if (!Main.isSimulating()) {
                  Main.rerender();
               }                
            }
            else if (!myHasPersistentComponent) {
               myComponent = null;
               if (!Main.isSimulating()) {
                  Main.rerender();
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
               getOriginPoint(), MouseRayEvent.create (e, viewer));

            if (!Main.isSimulating()) {
               Main.rerender();
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
            myComponent = null;
         }
         if (!Main.isSimulating()) {
            Main.rerender();
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

   // GLRenderable implementation
   public void prerender (RenderList list) {
   }

   public void render (GLRenderer renderer, int flags) {

      if (myComponent != null) {
         if (myPullPos != null || myHasPersistentComponent) {

            double saveRadius = myRenderProps.getPointRadius();
            PropertyMode saveRadiusMode = myRenderProps.getPointRadiusMode();

            if (saveRadius <= myPointRenderRadius) {
               myRenderProps.setPointRadius (1.05*myPointRenderRadius);
            }
            renderer.drawPoint (
               myRenderProps, getOriginRenderCoords(), false);
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
               myRenderProps, getOriginRenderCoords(), pullCoords, false);
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
      props.setPointStyle (RenderProps.PointStyle.POINT);
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

   public synchronized void apply (double t0, double t1) {
      if (myComponent != null && myPullPos != null) {
         Vector3d force = new Vector3d();
         Point3d origin = getOriginPoint();
         force.sub (myPullPos, origin);
         force.scale (myStiffness);
         applyForce (force);
      }
   }

   public void setModel (Model model) {
      super.setModel (model);
      double radius = 0;
      double mass = 0;
      if (model instanceof Renderable) {
         radius = RenderableUtils.getRadius ((Renderable)model);
      }
      if (model instanceof MechSystemBase) {
         mass = ((MechSystemBase)model).getMass();
      }
      if (radius > 0) {
         myRenderProps.setPointStyle (RenderProps.PointStyle.SPHERE);
         myRenderProps.setPointRadius (0.02*radius);
         myRenderProps.setLineRadius (0.01*radius);
         myRenderProps.setLineStyle (RenderProps.LineStyle.SOLID_ARROW);
      }
      if (mass > 0) {
         // set stiffness so that a displacement of 1/10 radius will accelerate
         // the whole model to a velocity of radius/sec in 1 seconds
         setStiffness (10*mass/(1*1));
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