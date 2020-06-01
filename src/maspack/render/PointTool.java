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

public class PointTool extends DrawToolBase {

   protected ArrayList<Point2d> myPoints;
   protected int myClickCount = 1;

   public PointTool() {
      myPoints = new ArrayList<Point2d>();
   }

   public int numPoints() {
      return myPoints.size();
   }

   public Point2d getPoint (int idx) {
      return myPoints.get (idx);
   }

   public int getClickCount() {
      return myClickCount;
   }

   public void setClickCount (int cnt) {
      myClickCount = cnt;
   }

   public void clear() {
      myPoints.clear();
   }

   public boolean mouseClicked (MouseRayEvent e) {
      if (e.getClickCount() == myClickCount) {
         Vector3d isect = new Vector3d();
         intersectRay (isect, e.getRay());
         fireDrawToolBeginListeners (e.getModifiersEx());         
         myPoints.add (new Point2d(isect.x, isect.y));
         fireDrawToolEndListeners (e.getModifiersEx());         
         return true;
      }
      else {
         return false;
      }
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
      renderer.setPointSize (2*myLineWidth);

      renderer.beginDraw (DrawMode.POINTS);
      for (int i=0; i<myPoints.size(); i++) {
         Point2d p = myPoints.get(i);
         renderer.addVertex (p.x, p.y, 0);
      }
      renderer.endDraw ();

      renderer.setPointSize (1);
      renderer.setShading (savedShading);

      renderer.popModelMatrix ();
   }
}
