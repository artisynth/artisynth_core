/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.matrix.*;
import maspack.render.GL.GLViewer;
import maspack.util.Round;

import java.util.LinkedList;

public abstract class Dragger3dBase extends DragToolBase {
   protected RigidTransform3d myXDraggerToWorld;
   //protected boolean mySelectedP;
   protected boolean myVisibleP;
   //protected boolean myMovableP = true;
   protected double mySize = 1;
   protected int myLineWidth = 2;
   protected LinkedList<Dragger3dListener> myListeners;

   protected static final double inf = Double.POSITIVE_INFINITY;

   protected Dragger3dBase() {
      myListeners = new LinkedList<Dragger3dListener>();
      myXDraggerToWorld = new RigidTransform3d();
      //mySelectedP = false;
      myVisibleP = true;
      mySize = 1;
   }

   public void addListener (Dragger3dListener l) {
      myListeners.add (l);
   }

   public boolean removeListener (Dragger3dListener l) {
      return myListeners.remove (l);
   }

   public void fireDraggerAddedListeners () {
      Dragger3dEvent e = null;
      RigidTransform3d IDENTITY = RigidTransform3d.IDENTITY;
      for (Dragger3dListener l : myListeners) {
         if (e == null) {
            e = new Dragger3dEvent (this, myViewer, IDENTITY, IDENTITY, 0);
         }
         l.draggerAdded (e);
      }
   }
   
   public void fireDraggerBeginListeners (
      AffineTransform3dBase X, AffineTransform3dBase Xinc, int modifiersEx) {
      if (myDragMode == DragMode.DRAG) {
         Dragger3dEvent e = null;
         for (Dragger3dListener l : myListeners) {
            if (e == null) {
               e = new Dragger3dEvent (this, myViewer, X, Xinc, modifiersEx);
            }
            l.draggerBegin (e);
         }
      }
   }

   public void fireDraggerMoveListeners (
      AffineTransform3dBase X, AffineTransform3dBase Xinc, int modifiersEx) {
      if (myDragMode == DragMode.DRAG) {
         Dragger3dEvent e = null;
         for (Dragger3dListener l : myListeners) {
            if (e == null) {
               e = new Dragger3dEvent (this, myViewer, X, Xinc, modifiersEx);
            }
            l.draggerMove (e);
         }
      }
   }

   public void fireDraggerEndListeners (
      AffineTransform3dBase X, AffineTransform3dBase Xinc, int modifiersEx) {
      if (myDragMode == DragMode.DRAG) {
         Dragger3dEvent e = null;
         for (Dragger3dListener l : myListeners) {
            if (e == null) {
               e = new Dragger3dEvent (this, myViewer, X, Xinc, modifiersEx);
            }
            l.draggerEnd (e);
         }
      }
   }
   
   public void fireDraggerRemovedListeners () {
      Dragger3dEvent e = null;
      RigidTransform3d IDENTITY = RigidTransform3d.IDENTITY;
      for (Dragger3dListener l : myListeners) {
         if (e == null) {
            e = new Dragger3dEvent (this, myViewer, IDENTITY, IDENTITY, 0);
         }
         l.draggerRemoved (e);
      }
   }
 
//   public boolean isSelected() {
//      return mySelectedP;
//   }

//   public void setSelected (boolean selected) {
//      mySelectedP = selected;
//   }

   public boolean isVisible() {
      return myVisibleP;
   }

   public void setVisible (boolean visible) {
      myVisibleP = visible;
   }

//   /**
//    * Returns true if this dragger can be moved <i>without</i> without causing
//    * drag events.
//    * 
//    * @return true if movable
//    */
//   public boolean isMovable() {
//      return myMovableP;
//   }
//
//   /**
//    * Sets whether or not this dragger can be moved <i>without</i> without
//    * causing drag events, by using the CTRL modifier key.
//    * 
//    * @param movable
//    * if true, enables movement without dragging
//    */
//   public void setMovable (boolean movable) {
//      myMovableP = movable;
//   }

   public void setSize (double s) {
      mySize = s;
   }

   public double getSize() {
      return mySize;
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

//   public void draggerSelected (MouseRayEvent e) {
//   }

   public RigidTransform3d getDraggerToWorld() {
      return myXDraggerToWorld;
   }

   public void setDraggerToWorld (RigidTransform3d X) {
      myXDraggerToWorld.set (X);
   }

   public void setPosition (Vector3d pos) {
      myXDraggerToWorld.p.set (pos);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      Point3d pnt = new Point3d (myXDraggerToWorld.p);
      pnt.x -= mySize;
      pnt.y -= mySize;
      pnt.z -= mySize;
      pnt.updateBounds (pmin, pmax);
      pnt.x += 2 * mySize;
      pnt.y += 2 * mySize;
      pnt.z += 2 * mySize;
      pnt.updateBounds (pmin, pmax);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   public void prerender (RenderList list) {
   }

   public int getRenderHints() {
      return 0;
   }

   GLViewer myViewer = null;

   /** 
    * Used by the viewer to set a reference to itself when the dragger is added.
    */
   public void setViewer (GLViewer viewer) {
      if (viewer == null) {
         fireDraggerRemovedListeners();
      }
      myViewer = viewer;
      // updateKeyMasks (viewer);
      if (viewer != null) {
         fireDraggerAddedListeners();
     }
   }

   /** 
    * Returns a reference to the viewer which is handling this dragger. Used
    * mainly to determine the size and alignment of constrained motions.
    * Should not be modified in any way.
    */
   public GLViewer getViewer() {
      return myViewer;
   }

   protected double getConstrainedStepSize() {
      return getConstrainedStepSize (myViewer);
   }

   protected double getConstrainedStepSize (GLViewer viewer) {
      if (viewer != null) {
         if (viewer.getGridVisible()) {
            return viewer.getCellSize()/viewer.getCellDivisions();
         }
         else {
            int minPixelsPerStep = 10;
            return Round.up125 (
               minPixelsPerStep*viewer.distancePerPixel(myXDraggerToWorld.p));
         }
      }
      else {
         return mySize/10;
      }
   }
}
