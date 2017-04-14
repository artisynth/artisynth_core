/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.ArrayList;

import maspack.matrix.Point2d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;

public class FreehandTool extends DrawToolBase {

   protected ArrayList<Point2d> myPoints;
   protected boolean myClosedP;

   public FreehandTool() {
      myPoints = new ArrayList<Point2d>();
   }

   public void clear() {
      myPoints.clear();
   }

   public boolean mousePressed (MouseRayEvent e) {
      if (isVisible()) {
         //DragMode mode = getDragMode (e);
         DragMode mode = getDragMode ();
         if (mode != DragMode.OFF) {
            Vector3d isect = new Vector3d();
            int height = myViewer.getScreenHeight();
            myDragMode = mode;
            myPoints.clear();
            intersectRay (isect, e.getRay());
            myPoints.add (new Point2d(isect.x, isect.y));
            System.out.println ("isect=" + isect);
            fireDrawToolBeginListeners (e.getModifiersEx());
            return true;
         }
      }
      return false;
   }

   public boolean mouseReleased (MouseRayEvent e) {
      if (myDragMode != DragMode.OFF) {
         fireDrawToolEndListeners (e.getModifiersEx());
         myDragMode = DragMode.OFF;
         clearFlags();
         return true;
      }
      return false;
   }

   public boolean mouseDragged (MouseRayEvent e) {
      if (myDragMode !=  DragMode.OFF) {      
         Vector3d isect = new Vector3d();
         int height = myViewer.getScreenHeight();
         intersectRay (isect, e.getRay());
         myPoints.add (new Point2d(isect.x, isect.y));
         return true;
      }
      return false;
   }

   public void render (Renderer renderer, int flags) {
      if (!myVisibleP) {
         return;
      }
     
      renderer.pushModelMatrix ();
      RigidTransform3d X = new RigidTransform3d();
      getToolToWorld (X);
      renderer.mulModelMatrix (X);

      Shading savedShading = renderer.setShading (Shading.NONE);
      float[] rgb = new float[4];
      myLineColor.getRGBColorComponents (rgb);
      renderer.setColor (rgb);
      renderer.setLineWidth (myLineWidth);

      if (myClosedP) {
         renderer.beginDraw (DrawMode.LINE_LOOP);
      }
      else {
         renderer.beginDraw (DrawMode.LINE_STRIP);
      }
      for (int i=0; i<myPoints.size(); i++) {
         Point2d p = myPoints.get(i);
         renderer.addVertex (p.x, p.y, 0);
      }
      renderer.endDraw ();

      renderer.setLineWidth (1);
      renderer.setShading (savedShading);

      renderer.popModelMatrix ();
   }
}
