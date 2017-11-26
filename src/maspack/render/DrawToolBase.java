/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Color;
import java.awt.event.*;
import java.util.LinkedList;

import maspack.matrix.*;
import maspack.render.GL.GLClipPlane;
import maspack.render.GL.GLViewer;
import maspack.util.*;

public abstract class DrawToolBase extends DragToolBase {

   protected boolean myVisibleP;
   protected GLViewer myViewer;
   protected int myLineWidth = 2;
   protected Color myLineColor = Color.RED;
   protected LinkedList<DrawToolListener> myListeners;
   protected RigidTransform3d myFrame;
   protected static final double EPS = 1e-10;

   public enum FrameBinding {
      VIEW_PLANE,
         CLIPPING_PLANE,
         INTERNAL_FRAME
   }

   protected FrameBinding myFrameBinding = FrameBinding.CLIPPING_PLANE;
   protected double myFrameOffset = 0;

   public void setFrameBinding (FrameBinding binding) {
      myFrameBinding = binding;
   }

   public FrameBinding getFrameBinding () {
      return myFrameBinding;
   }

   public void setFrameOffset (double offset) {
      myFrameOffset = offset;
   }

   public double getFrameOffset () {
      return myFrameOffset;
   }

   /**
    * Clears the drawn contents of this tool.
    */
   public abstract void clear();

   private boolean getTransformFromClippingPlane(RigidTransform3d X) {
      if (myViewer.getNumClipPlanes() > 0) {
         GLClipPlane plane = myViewer.getClipPlane (0);
         X.set (plane.getGridToWorld());
         return true;
      }
      else {
         return false;
      }
   }

   private void getTransformFromNearPlane(RigidTransform3d X) {
      X.invert (myViewer.getViewMatrix());
      // multiply by 1.01 to make sure we are slightly inside the view
      // plane, so that we don't get clipped.
      X.mulXyz (0, 0, -1.01*myViewer.getViewPlaneDistance());
   }

   public void setFrame (RigidTransform3d X) {
      myFrame.set (X);
   }

   public RigidTransform3d getFrame () {
      return myFrame;
   }

   public void getToolToWorld (RigidTransform3d X) {
      switch (myFrameBinding) {
         case VIEW_PLANE: {
            getTransformFromNearPlane (X);
            break;
         }
         case CLIPPING_PLANE: {
            if (!getTransformFromClippingPlane (X)) {
               getTransformFromNearPlane (X);
            }
            break;
         }
         case INTERNAL_FRAME: {
            X.set (myFrame);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented binding " + myFrameBinding);
         }
      }
      if (myFrameOffset != 0) {
         X.mulXyz (0, 0, myFrameOffset);
      }
   }

   /**
    * Intersect the ray from a MouseRayEvent with the x-y plane of this tool's
    * coordinate frame. Returns false if the plane and ray do not intersect.
    */
   protected boolean intersectRay (Vector3d isect, Line ray) {

      RigidTransform3d X = new RigidTransform3d();
      getToolToWorld (X);
      Line toolRay = new Line (ray);

      toolRay.inverseTransform (X);

      Vector3d dir = toolRay.getDirection();
      Vector3d origin = toolRay.getOrigin();

      //RigidTransform3d X = new RigidTransform3d();
      //getToolToWorld (X);
      //RotationMatrix3d R = X.R;
      //Vector3d nrm = new Vector3d(R.m02, R.m12, R.m22);
      //Vector3d del = new Vector3d();
      //del.sub (origin, X.p);

      double dot = dir.z;
      if (Math.abs(dot) <= EPS) {
         // plane and ray are parallel
         return false;
      }
      double dist = origin.z;
      isect.scaledAdd (-dist/dot, dir, origin);
      return true;      
   }

   protected static final double inf = Double.POSITIVE_INFINITY;

   protected DrawToolBase() {
      myFrame = new RigidTransform3d();
      myListeners = new LinkedList<DrawToolListener>();
      myVisibleP = true;
   }

   public void addListener (DrawToolListener l) {
      myListeners.add (l);
   }

   public boolean removeListener (DrawToolListener l) {
      return myListeners.remove (l);
   }

   public void fireDrawToolAddedListeners () {
      DrawToolEvent e = null;
      for (DrawToolListener l : myListeners) {
         if (e == null) {
            e = new DrawToolEvent (this, myViewer, 0);
         }
         l.drawToolAdded (e);
      }
   }
   
   public void fireDrawToolBeginListeners (int modifiersEx) {
      DrawToolEvent e = null;
      for (DrawToolListener l : myListeners) {
         if (e == null) {
            e = new DrawToolEvent (this, myViewer, modifiersEx);
         }
         l.drawToolBegin (e);
      }
   }

   public void fireDrawToolEndListeners (int modifiersEx) {
      DrawToolEvent e = null;
      for (DrawToolListener l : myListeners) {
         if (e == null) {
            e = new DrawToolEvent (this, myViewer, modifiersEx);
         }
         l.drawToolEnd (e);
      }
   }
   
   public void fireDrawToolRemovedListeners () {
      DrawToolEvent e = null;
      for (DrawToolListener l : myListeners) {
         if (e == null) {
            e = new DrawToolEvent (this, myViewer, 0);
         }
         l.drawToolRemoved (e);
      }
   }

   public boolean isVisible() {
      return myVisibleP;
   }

   public void setVisible (boolean visible) {
      myVisibleP = visible;
   }

   public boolean mouseClicked (MouseRayEvent e) {
      return false;
   }

   public boolean mousePressed (MouseRayEvent e) {
      return false;
   }

   public boolean mouseReleased (MouseRayEvent e) {
      return false;
   }

   public boolean mouseDragged (MouseRayEvent e) {
      return false;
   }

   public boolean mouseMoved (MouseRayEvent e) {
      return false;
   }

   /** 
    * Used by the viewer to set a reference to itself when the drawTool is added.
    */
   public void setViewer (GLViewer viewer) {
      if (viewer == null && myViewer != null) {
         fireDrawToolRemovedListeners();
      }
      myViewer = viewer;

      if (viewer != null) {
         fireDrawToolAddedListeners();
     }
   }

   /** 
    * Returns a reference to the viewer which is handling this drawTool. Used
    * mainly to determine the size and alignment of constrained motions.
    * Should not be modified in any way.
    */
   public GLViewer getViewer() {
      return myViewer;
   }

   protected double getConstrainedStepSize() {
      return getConstrainedStepSize (myViewer);
   }

   // XXX need to finish this
   protected double getConstrainedStepSize (GLViewer viewer) {
      if (viewer != null) {
         if (viewer.getGridVisible()) {
            return viewer.getCellSize()/viewer.getCellDivisions();
         }
         // else {
         //    int minPixelsPerStep = 10;
         //    return Round.up125 (
         //    minPixelsPerStep*viewer.distancePerPixel(myXDrawToolToWorld.p));
         // }
      }
      return 10;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public int numSelectionQueriesNeeded() {
      return -1;
   }

   /**
    * {@inheritDoc}
    */
   public void getSelection (LinkedList<Object> list, int qid) {
   }

   /**
    * {@inheritDoc}
    */
   public void prerender (RenderList list) {
   }

   /**
    * {@inheritDoc}
    */
   public int getRenderHints() {
      return 0;
   }
   

}
