/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.util.*;
import java.awt.event.*;

import maspack.matrix.*;
import maspack.render.*;
import maspack.render.GL.GLViewer;
import maspack.render.Renderer.*;
import maspack.geometry.*;

public class SplineTool extends DrawToolBase {

   protected NURBSCurve2d myCurve;
   protected ArrayList<Point2d> myPoints;
   protected boolean myClosedP;
   protected double mySelectDist = 3;
   protected int mySelectedPointIdx = -1;
   protected int myMaxDegree = 3;

   private enum Mode {
      READY,
      CONSTRUCTING,
      EDITING,
      DRAGGING
   };

   private class KeyHandler extends KeyAdapter {
      public void keyPressed (KeyEvent e) {
         System.out.println ("key=" + e.getKeyCode());
         if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (myMode != Mode.READY) {
               myMode = Mode.READY;
               deselectControlPoint();
               fireDrawToolEndListeners (e.getModifiersEx());
            }
         }
         else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (myMode == Mode.EDITING) {
               if (myCurve != null) {
                  myCurve.convertToBezier();
                  myViewer.repaint();
               }
            }
         }
         else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            System.out.println ("Delete");
            if (myMode != Mode.READY) {
               clear();
               myMode = Mode.READY;
               deselectControlPoint();
            }
         }
      }
   }

   protected KeyHandler myKeyHandler = new KeyHandler();

   protected Mode myMode = Mode.READY;

   public SplineTool() {
   }

   public int getMaxDegree() {
      return myMaxDegree;
   }

   public void setMaxDegree (int maxd) {
      if (maxd < 1) {
         throw new IllegalArgumentException ("Max degree must be >= 1");
      }
      if (maxd != myMaxDegree) {
         if (myCurve != null && myCurve.getDegree() > maxd) {
            myCurve.reset (maxd, myCurve.getType(), /*knots=*/null);
         }
         myMaxDegree = maxd;
      }
   }

   public NURBSCurve2d getCurve() {
      return myCurve;
   }

   public void clear() {
      setCurve (null);
   }

   public void setCurve (NURBSCurve2d curve) {
      if (curve != null) {
         myCurve = new NURBSCurve2d (curve);
         if (myCurve.getDegree() > myMaxDegree) {
            myCurve.reset (myMaxDegree, myCurve.getType(), /*knots=*/null);
         }
         myMode = Mode.EDITING;
      }
      else {
         myCurve = null;
         myMode = Mode.READY;
      }
   }

   @Override
   public void setViewer (GLViewer viewer) {
      if (viewer == null && myViewer != null) {
         myViewer.removeKeyListener (myKeyHandler);
      }
      super.setViewer (viewer);
      if (viewer != null) {
         viewer.addKeyListener (myKeyHandler);
      }
   }

   private NURBSCurve2d createInitialCurve (double x, double y) {
      NURBSCurve2d curve = new NURBSCurve2d();
      RenderProps props = curve.createRenderProps();
      props.setLineWidth (2);
      props.setEdgeWidth (2);
      props.setPointSize (4);
      curve.setRenderProps (props);

      Vector4d[] ctrlPnts = new Vector4d[] {
         new Vector4d (x, y, 0, 1),
         new Vector4d (x, y, 0, 1)
      };
      curve.set (1, NURBSCurve2d.OPEN, ctrlPnts, /*knots=*/null);
      return curve;
   }

   protected void selectControlPoint (int i) {
      myCurve.selectControlPoint (i, true);
      mySelectedPointIdx = i;
   }

   protected void deselectControlPoint () {
      if (mySelectedPointIdx != -1) {
         if (myCurve != null) {
            myCurve.selectControlPoint (mySelectedPointIdx, false);
         }
         mySelectedPointIdx = -1;
      }
   }

   protected void setControlPoint (int i, double x, double y) {
      myCurve.getControlPoint (i).set (x, y, 0, 1);
   }

   protected void addControlPoint (double x, double y) {
      System.out.printf ("add control %g %g\n", x, y);
      myCurve.addControlPoint (new Vector4d (x, y, 0, 1), -1);
      int deg = myCurve.getDegree();
      if (deg < myMaxDegree) {
         myCurve.reset (deg+1, myCurve.getType(), /*knots=*/null);
      }
   }

   public boolean mouseClicked (MouseRayEvent e) {
      if (e.getClickCount() == 2) {
         if (myMode == Mode.CONSTRUCTING) {
            // two extra points were added by the double click - these
            // must be removed. 
            int numc = myCurve.numControlPoints();
            // also may need to reduce curve degree
            int maxd = numc-3;
            if (maxd < myCurve.getDegree()) {
               System.out.println ("Resetting curve");
               myCurve.reset (maxd, myCurve.getType(), /*knots=*/null);
            }
            myCurve.removeControlPoint();
            myCurve.removeControlPoint();

            if ((e.getModifiersEx() & MouseRayEvent.SHIFT_DOWN_MASK) != 0) {
               // change the NURB from open to closed
               myCurve.reset (
                  myCurve.getDegree(), NURBSCurve3d.CLOSED, /*knots=*/null);
            }
            myMode = Mode.EDITING;
            return true;
         }
      }
      return false;
   }

   public boolean mousePressed (MouseRayEvent e) {
      if (isVisible()) {
         Vector3d isect = new Vector3d();
         intersectRay (isect, e.getRay());
         if (myMode == Mode.READY) {
            myCurve = createInitialCurve (isect.x, isect.y);
            fireDrawToolBeginListeners (e.getModifiersEx());
            myMode = Mode.CONSTRUCTING;
         }
         else if (myMode == Mode.CONSTRUCTING) {
            int numc = myCurve.numControlPoints();
            setControlPoint (numc-1, isect.x, isect.y);
            addControlPoint (isect.x, isect.y);
         }
         else if (mySelectedPointIdx != -1) {
            myMode = Mode.DRAGGING;
         }
         return true;
      }
      return false;
   }

   public boolean mouseMoved (MouseRayEvent e) {
      if (myMode == Mode.CONSTRUCTING) {
         Vector3d isect = new Vector3d();
         intersectRay (isect, e.getRay());
         int numc = myCurve.numControlPoints();
         setControlPoint (numc-1, isect.x, isect.y);
         return true;         
      }
      else if (myMode == Mode.EDITING) {
         deselectControlPoint();
         Vector3d isect = new Vector3d();
         intersectRay (isect, e.getRay());
         RigidTransform3d X = new RigidTransform3d();
         getToolToWorld (X);
         double dist = mySelectDist*e.distancePerPixel (X.p);
         for (int i=0; i<myCurve.numControlPoints(); i++) {
            Vector4d pnt = myCurve.getControlPoint(i);
            if (Math.abs(pnt.x-isect.x) < dist &&
                Math.abs(pnt.y-isect.y) < dist) {
               selectControlPoint (i);
               break;
            }
         }
         return true;
      }
      else {
         return false;
      }
   }

   public boolean mouseReleased (MouseRayEvent e) {
      if (myMode == Mode.DRAGGING) {
         myMode = Mode.EDITING;
         return true;
      }
      return false;
   }

   public boolean mouseDragged (MouseRayEvent e) {
      if (myMode == Mode.DRAGGING) {
         Vector3d isect = new Vector3d();
         intersectRay (isect, e.getRay());
         setControlPoint (mySelectedPointIdx, isect.x, isect.y);
         return true;
      }
      return false;
   }

   public void render (Renderer renderer, int flags) {
      if (!myVisibleP) {
         return;
      }
      if (myCurve != null) {
         RigidTransform3d X = new RigidTransform3d();
         getToolToWorld (X);
         myCurve.setObjToWorld (X);
         myCurve.render (renderer, 0);
      }
   }
}
