/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Rectangle;
import java.awt.event.*;
import javax.swing.event.*;
import maspack.util.*;

public class GLMouseListener implements MouseInputListener, MouseWheelListener {
   protected GLViewer viewer;

   protected static int ALL_MODIFIERS =
      (InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK |
       InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK |
       InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK |
       InputEvent.BUTTON3_DOWN_MASK);

   protected static final int NO_ACTION = 0;
   protected static final int TRANSLATE = 1;
   protected static final int ZOOM = 2;
   protected static final int ROTATE = 3;
   protected static final int DRAG_SELECT = 4;

   private int dragAction = NO_ACTION;

   private int lastX;
   private int lastY;

   private int dragStartX;
   private int dragStartY;

   protected GLMouseListener (GLViewer viewer) {
      this.viewer = viewer;
   }

   public void setGLViewer (GLViewer viewer) {
      this.viewer = viewer;
   }

   public GLViewer getGLViewer() {
      return viewer;
   }

   private void checkForSelection (MouseEvent e) {
      int mask = (e.getModifiersEx() & ALL_MODIFIERS);
      int mode = 0;

      GLSelectionEvent selEvent = new GLSelectionEvent();
      selEvent.myModifiersEx = e.getModifiersEx();
      if ((mask & viewer.getMultipleSelectionMask()) != 0) {
         mode |= GLSelectionEvent.MULTIPLE;
      }
      viewer.selectionEvent = selEvent;

      double x, y, w, h; // delimits the pick region (with x, y at the center)
      boolean ignoreDepthTest;
      Rectangle dragBox = viewer.getDragBox();
      if (dragBox != null) {
         x = dragBox.x + dragBox.width/2.0;
         y = dragBox.y + dragBox.height/2.0;
         w = dragBox.width;
         h = dragBox.height;
         mode |= GLSelectionEvent.DRAG;
         ignoreDepthTest = true;
      }
      else {
         x = e.getX();
         y = e.getY();
         w = 5.0;
         h = 5.0;
         ignoreDepthTest = false;
      }
      viewer.setPick (x, y, w, h, ignoreDepthTest);
      selEvent.setFlags (mode);
      // {
      // GLSelectionEvent selEvent = new GLSelectionEvent();
      // selEvent.myModifiersEx = e.getModifiersEx();
      // selEvent.setMode(SelectionType.Clear);
      // viewer.selectionEvent = selEvent;
      // }
   }

   public void mouseClicked (MouseEvent e) {
      Dragger3d drawTool = viewer.myDrawTool;
      boolean grabbed = false;

      if (viewer.myDraggers.size() > 0 || drawTool != null) {
         MouseRayEvent rayEvt = MouseRayEvent.create (e, viewer);
         for (Dragger3d d : viewer.myDraggers) {
            // pass event to any visible Dragger3dBase, or any other Dragger3d
            if (d.isVisible()) {
               if (d.mouseClicked (rayEvt)) {
                  grabbed = true;
                  break;
               }
            }
         }
         if (drawTool != null && drawTool.isVisible()) {
            if (drawTool.mouseClicked (rayEvt)) {
               grabbed = true;
            }
         }
      }
      if (grabbed) {
         viewer.repaint();
      }
   }

   public void mouseEntered (MouseEvent e) {
   }

   public void mouseExited (MouseEvent e) {
   }

   public void mousePressed (MouseEvent e) {
      int mask = (e.getModifiersEx() & ALL_MODIFIERS);
      int selectionMask = viewer.getSelectionButtonMask();

      if (mask == viewer.getTranslateButtonMask()) {
         dragAction = TRANSLATE;
      }
      else if (mask == viewer.getRotateButtonMask()) {
         dragAction = ROTATE;
      }
      else if (mask == viewer.getZoomButtonMask()) {
         dragAction = ZOOM;
      }
      else {
         Dragger3d drawTool = viewer.myDrawTool;
         boolean grabbed = false;

         if (viewer.myDraggers.size() > 0 || drawTool != null) {
            MouseRayEvent rayEvt = MouseRayEvent.create (e, viewer);
            for (Dragger3d d : viewer.myDraggers) {
               // pass event to any visible Dragger3dBase, or any other Dragger3d
               if (d.isVisible()) {
                  if (d.mousePressed (rayEvt)) {
                     grabbed = true;
                     break;
                  }
               }
            }
            if (drawTool != null && drawTool.isVisible()) {
               if (drawTool.mousePressed (rayEvt)) {
                  grabbed = true;
               }
            }
         }
         if (grabbed) {
            viewer.repaint();
         }
         if (!grabbed && viewer.isSelectionEnabled() &&
             (mask & selectionMask) == selectionMask) {
            if (viewer.getSelectOnPress()) {
               checkForSelection (e);               
               dragAction = NO_ACTION;
               viewer.setDragBox (null);
            }
            else {
               dragAction = DRAG_SELECT;
            }
         }
         else {
            dragAction = NO_ACTION;
         }
      }
      dragStartX = lastX = e.getX();
      dragStartY = lastY = e.getY();
   }

   public void mouseReleased (MouseEvent e) {
      if (dragAction == NO_ACTION) {
         Dragger3d drawTool = viewer.myDrawTool;
         boolean grabbed = false;

         if (viewer.myDraggers.size() > 0 || drawTool != null) {
            MouseRayEvent rayEvt = MouseRayEvent.create (e, viewer);
            for (Dragger3d d : viewer.myDraggers) {
               // pass event to any visible Dragger3dBase, or any other Dragger3d
               if (d.isVisible()) {
                  if (d.mouseReleased (rayEvt)) {
                     grabbed = true;
                     break;
                  }
               }
            }
            if (drawTool != null && drawTool.isVisible()) {
               if (drawTool.mouseReleased (rayEvt)) {
                  grabbed = true;
               }
            }
         }
         if (grabbed) {
            viewer.repaint();
         }
      }
      else if (dragAction == DRAG_SELECT) {
         checkForSelection (e);
         viewer.setDragBox (null);
      }
   }

   public void mouseDragged (MouseEvent e) {
      int xOff = e.getX() - lastX, yOff = e.getY() - lastY;

      switch (dragAction) {
         case ROTATE: {
            viewer.rotate (-xOff / 200.0, -yOff / 200.0);
            break;
         }
         case ZOOM: {
            double r = yOff < 0 ? 0.99 : 1.01;
            double s = Math.pow (r, Math.abs (yOff));
            viewer.zoom (s);
            // viewer.repaint();
            break;
         }
         case TRANSLATE: {
            double dpp = viewer.centerDistancePerPixel();
            viewer.translate (dpp * xOff, -dpp * yOff);
            break;
         }
         case NO_ACTION: {
            Dragger3d drawTool = viewer.myDrawTool;
            boolean grabbed = false;

            if (viewer.myDraggers.size() > 0 || drawTool != null) {
               MouseRayEvent rayEvt = MouseRayEvent.create (e, viewer);
               for (Dragger3d d : viewer.myDraggers) {
                  // pass to any visible Dragger3dBase, or any other Dragger3d
                  if (d.isVisible()) {
                     if (d.mouseDragged (rayEvt)) {
                        grabbed = true;
                        break;
                     }
                  }
               }
               if (drawTool != null && drawTool.isVisible()) {
                  if (drawTool.mouseDragged (rayEvt)) {
                     grabbed = true;
                  }
               }
            }
            if (grabbed) {
               viewer.repaint();
            }
            break;
         }
         case DRAG_SELECT: {
            int x = dragStartX;
            int y = dragStartY;
            int w = e.getX() - x;
            int h = e.getY() - y;
            if (w == 0) {
               w = 1;
            }
            else if (w < 0) {
               x = e.getX();
               w = -w;
            }
            if (h == 0) {
               h = 1;
            }
            else if (h < 0) {
               y = e.getY();
               h = -h;
            }
            viewer.setDragBox (new Rectangle (x, y, w, h));
            viewer.repaint();
         }
      }
      lastX = e.getX();
      lastY = e.getY();
   }

   public void mouseMoved (MouseEvent e) {
      Dragger3d drawTool = viewer.myDrawTool;
      boolean grabbed = false;

      if (viewer.myDraggers.size() > 0 || drawTool != null) {
         MouseRayEvent rayEvt = MouseRayEvent.create (e, viewer);
         for (Dragger3d d : viewer.myDraggers) {
            // pass event to any visible Dragger3dBase, or any other Dragger3d
            if (d.isVisible()) {
               if (d.mouseMoved (rayEvt)) {
                  grabbed = true;
                  break;
               }
            }
         }
         if (drawTool != null && drawTool.isVisible()) {
            if (drawTool.mouseMoved (rayEvt)) {
               grabbed = true;
            }
         }
      }
      if (grabbed) {
         viewer.repaint();
      }
   }

   /**
    * to get the zoom function going through the mouse wheel which is easier to
    * navigate, then by using key combination CTRL + mouse click + mouse drag.
    * The latter is still in the system for backward compatibility author:
    * andreio
    */
   public void mouseWheelMoved (MouseWheelEvent e) {
      int yOff = (int)(e.getWheelRotation() * viewer.myWheelZoomScale);
      double r = yOff < 0 ? 0.99 : 1.01;
      double s = Math.pow (r, Math.abs (yOff));
      viewer.zoom (s);

   }

}
