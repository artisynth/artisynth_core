/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import artisynth.core.driver.*;
import artisynth.core.gui.selectionManager.*;
import maspack.matrix.Line;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.RotationMatrix3d;
import maspack.render.IsRenderable;
import maspack.render.GL.GLClipPlane;
import maspack.render.GL.GLViewer;

/**
 * This is the base class for all EditWidgets. An EditWidget is responsible for
 * editing components together. Usually there is a parent component which is
 * being edited and child components are being added to it. Each Edit Widget is
 * responsible for combing a particular pair or set of components when they are
 * to be added.
 * 
 */
public abstract class EditingAgent {
   protected static UndoManager myUndoManager;
   protected static SelectionManager mySelectionManager;
   protected static ViewerManager myViewerManager;
   protected static EditorManager myEditManager;

   protected Main myMain;

   public EditingAgent (Main main) {
      myMain = main;
      myUndoManager = myMain.getUndoManager();
      mySelectionManager = myMain.getSelectionManager();
      myViewerManager = myMain.getViewerManager();
      myEditManager = myMain.getEditorManager();
   }

   /**
    * Intersect a ray with a view plane defined by the current eye direction and
    * a reference point.
    */
   public Point3d intersectViewPlane (Line ray, Point3d ref, GLViewer viewer) {
      Point3d res = new Point3d();
      RotationMatrix3d R = viewer.getCenterToWorld().R;
      Plane plane = new Plane (new Vector3d (R.m02, R.m12, R.m22), ref);
      plane.intersectRay (res, ray.getDirection(), ray.getOrigin());
      return res;
   }

   /**
    * Intersects a ray with a viewer clip plane and returns the corresponding
    * point. If there is no intersection because the plane is perpendicular to
    * the eye direction, then null is returned.
    */
   public Point3d intersectClipPlane (Line ray, GLClipPlane clipPlane) {
      Point3d res = new Point3d();
      Plane plane = new Plane();
      clipPlane.getPlane (plane);

      if (ray.intersectPlane (res, plane) == Double.POSITIVE_INFINITY) {
         return null;
      }
      else {
         return res;
      }
   }

   private static final double inf = Double.POSITIVE_INFINITY;

   /**
    * Returns the center point of a renderable object.
    */
   public Point3d getCenter (IsRenderable r) {
      Point3d max = new Point3d (-inf, -inf, -inf);
      Point3d min = new Point3d (inf, inf, inf);
      r.updateBounds (min, max);
      Point3d center = new Point3d();
      if (max.x == -inf) { // then the renderable provided no bounds, so set
                           // center to 0
         center.setZero();
      }
      else {
         center.add (min, max);
         center.scale (0.5);
      }
      return center;
   }

   /**
    * Check that the context for this agent is valid. If it is not, then it
    * should be disposed. A valid context typically means that all components
    * associated with this agent are still present in the model hierarchy.
    * 
    * @return true if context for this agent is valid.
    */
   protected boolean isContextValid() {
      return true;
   }

}
