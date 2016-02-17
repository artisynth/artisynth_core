/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.ArrayList;

import javax.media.opengl.GL2;

import maspack.matrix.Point2d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.GL.GL2.GL2Viewer;

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
            int height = myViewer.getHeight();
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
         int height = myViewer.getHeight();
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
      
      if (!(renderer instanceof GL2Viewer)) {
         return;
      }
      GL2Viewer viewer = (GL2Viewer)renderer;
      GL2 gl = viewer.getGL2();
      
      float[] rgb = new float[3];

      gl.glPushMatrix();
      RigidTransform3d X = new RigidTransform3d();
      getToolToWorld (X);
      renderer.mulModelMatrix (X);

      boolean saveLighting = renderer.isLightingEnabled ();
      renderer.setLightingEnabled (false);
      myLineColor.getRGBColorComponents (rgb);
      renderer.setColor (rgb);
      renderer.setLineWidth (myLineWidth);

      if (myClosedP) {
         gl.glBegin (GL2.GL_LINE_LOOP);
      }
      else {
         gl.glBegin (GL2.GL_LINE_STRIP);
      }
      for (int i=0; i<myPoints.size(); i++) {
         Point2d p = myPoints.get(i);
         gl.glVertex3d (p.x, p.y, 0);
      }
      gl.glEnd();

      renderer.setLineWidth (1);
      renderer.setLightingEnabled (saveLighting);

      gl.glPopMatrix();
   }
}
