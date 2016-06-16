/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Component;
import java.awt.event.MouseEvent;

import maspack.matrix.Line;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.GL.GLViewer;

public class MouseRayEvent extends MouseEvent {
   protected double myViewPlaneWidth;
   protected double myViewPlaneHeight;
   protected double myViewPlaneDistance;
   protected int myScreenWidth;
   protected int myScreenHeight;
   protected Line myRay;
   protected GLViewer myViewer;

   protected MouseRayEvent (Component source, int id, long when, int modifiers,
   int x, int y, int clickCount, boolean popupTrigger) {
      super (source, id, when, modifiers, x, y, clickCount, popupTrigger);
   }

   public static MouseRayEvent create (MouseEvent e, GLViewer viewer) {
      MouseRayEvent de =
         new MouseRayEvent (
            (Component)e.getSource(), e.getID(), e.getWhen(),
            e.getModifiersEx(), e.getX(), e.getY(), e.getClickCount(),
            e.isPopupTrigger());

      de.myScreenWidth = viewer.getScreenWidth();
      de.myScreenHeight = viewer.getScreenHeight();

      double vph = viewer.getViewPlaneHeight();
      double vpw = viewer.getViewPlaneWidth();

      de.myViewPlaneHeight = vph;
      de.myViewPlaneWidth = vpw;
      de.myViewPlaneDistance = viewer.getViewPlaneDistance();

      // vx and vy give the current cursor location in the viewplane

      double vx = vpw * (-0.5 + de.getX() / (double)de.myScreenWidth);
      double vy = vph * (0.5 - de.getY() / (double)de.myScreenHeight);

      if (viewer.isOrthogonal()) {
         de.myRay = new Line (vx, vy, de.myViewPlaneDistance, 0, 0, -1);
      }
      else {
         de.myRay = new Line (0, 0, 0, vx, vy, -de.myViewPlaneDistance);
      }
      RigidTransform3d XWorldToBase = new RigidTransform3d();
      viewer.getViewMatrix (XWorldToBase);
      de.myRay.inverseTransform (XWorldToBase);

      de.myViewer = viewer;

      return de;
   }

   public int getScreenWidth() {
      return myScreenWidth;
   }

   public int getScreenHeight() {
      return myScreenHeight;
   }

   public double getViewPlaneWidth() {
      return myViewPlaneWidth;
   }

   public double getViewPlaneHeight() {
      return myViewPlaneWidth;
   }

   public double getViewPlaneDistance() {
      return myViewPlaneDistance;
   }

   public Line getRay() {
      return myRay;
   }

   public GLViewer getViewer() {
      return myViewer;
   }

   public double distancePerPixel (Vector3d pnt) {
      return myViewer.distancePerPixel (pnt);
   }

}
