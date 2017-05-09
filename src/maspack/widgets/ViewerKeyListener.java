/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.event.*;

import maspack.render.*;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GLGridPlane;
import maspack.render.GL.GLGridPlane.AxisLabeling;
import maspack.matrix.*;

public class ViewerKeyListener implements KeyListener {
   
   GLViewer myViewer;

   public ViewerKeyListener (GLViewer viewer) {
      myViewer = viewer;
   }

   public void keyPressed (KeyEvent e) {
      int code = e.getKeyCode();
      AxisAlignedRotation viewSet = null;

      if (code == KeyEvent.VK_NUMPAD7 ||
          (code == KeyEvent.VK_HOME &&
           e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD)) {
         if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == 0) {
            viewSet = AxisAlignedRotation.X_Y;
         }
         else {
            viewSet = AxisAlignedRotation.X_NY;
         }
      }
      else if (code == KeyEvent.VK_NUMPAD1 ||
               (code == KeyEvent.VK_END &&
                e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD)) {
         if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == 0) {
            viewSet = AxisAlignedRotation.X_Z;
         }
         else {
            viewSet = AxisAlignedRotation.NX_Z;
         }
      }
      else if (code == KeyEvent.VK_NUMPAD3 ||
               (code == KeyEvent.VK_PAGE_DOWN &&
                e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD)) {
         if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == 0) {
            viewSet = AxisAlignedRotation.Y_Z;
         }
         else {
            viewSet = AxisAlignedRotation.NY_Z;
         }
      }

      if (viewSet != null) {
         myViewer.setAxialView (viewSet);
      } // needed for KeyListener interface
   }

   public void keyReleased (KeyEvent e) {
      // needed for KeyListener interface
   }

   public void keyTyped (KeyEvent e) {
      switch (e.getKeyChar()) {
         case 'o': {
            myViewer.setOrthographicView (!myViewer.isOrthogonal());
            break;
         }
         case 'a': {
            if (myViewer.getAxisLength() == 0.0) {
               myViewer.setAxisLength (GLViewer.AUTO_FIT);
            }
            else {
               myViewer.setAxisLength (0.0);
            }
            myViewer.rerender();
            break;
         }
         case '~': {
            RigidTransform3d X = new RigidTransform3d();
            myViewer.getEyeToWorld (X);
            X.invert();
            System.out.println (X.toString (
               "%10.5f", RigidTransform3d.AXIS_ANGLE_STRING));
            break;
         }
         case 'g': {
            myViewer.setGridVisible (!myViewer.getGridVisible());
            myViewer.rerender();
            break;
         }
         case 'l': {
            GLGridPlane grid = myViewer.getGrid();
            boolean labelsVisible =
               (grid.getXAxisLabeling() != AxisLabeling.OFF ||
                grid.getYAxisLabeling() != AxisLabeling.OFF);
            if (labelsVisible) {
               grid.setXAxisLabeling (AxisLabeling.OFF);
               grid.setYAxisLabeling (AxisLabeling.OFF);
            }
            else {
               grid.setXAxisLabeling (AxisLabeling.ON);
               grid.setYAxisLabeling (AxisLabeling.ON);
            }
            myViewer.rerender();
            break;
         }
      }
   }

}
