/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), 
 * Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.event.MouseInputListener;

import maspack.render.Dragger3d.DragMode;

public class GLMouseAdapter implements GLMouseListener {
   
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
   protected static final int DRAGGER_ACTION = 5;

   private int dragAction = NO_ACTION;

   private int lastX;
   private int lastY;

   private int dragStartX;
   private int dragStartY;

   double myWheelZoomScale = 10.0;
   
   private int multipleSelectionMask = (InputEvent.CTRL_DOWN_MASK);
   // private int dragSelectionMask = (InputEvent.SHIFT_DOWN_MASK);
   private int rotateButtonMask = (InputEvent.BUTTON2_DOWN_MASK);
   private int translateButtonMask =
      (InputEvent.BUTTON2_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
   private int zoomButtonMask =
      (InputEvent.BUTTON2_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
   private int selectionButtonMask = (InputEvent.BUTTON1_DOWN_MASK);
   private int selectionModMask = (InputEvent.SHIFT_DOWN_MASK);
   
   private int draggerConstrainMask = MouseEvent.SHIFT_DOWN_MASK;
   private int draggerDragMask = InputEvent.BUTTON1_DOWN_MASK;
   protected int draggerRepositionMask = (InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
   
   public GLMouseAdapter (GLViewer viewer) {
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
      if ((mask & getMultipleSelectionMask()) != 0) {
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
      
      //      int button = e.getButton();
      //      int mods = e.getModifiersEx();
      //      System.out.println("Mouse: " + MouseEvent.getModifiersExText(mods));
      
      Dragger3d drawTool = viewer.myDrawTool;
      int mods = e.getModifiersEx() & ALL_MODIFIERS;
      boolean grabbed = false;

      if (viewer.myDraggers.size() > 0 || drawTool != null) {
         MouseRayEvent rayEvt = MouseRayEvent.create (e, viewer);
         for (Dragger3d d : viewer.myDraggers) {
            // pass event to any visible Dragger3dBase, or any other Dragger3d
            if (d.isVisible()) {
               updateDraggerModeAndFlags(mods, d);
               if (d.mouseClicked (rayEvt)) {
                  grabbed = true;
                  dragAction = DRAGGER_ACTION;
                  break;
               }
            }
         }
         if (drawTool != null && drawTool.isVisible()) {
            updateDraggerModeAndFlags(mods, drawTool);
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

   private void updateDraggerFlags(int mods, Dragger3d dragger) {
      
      dragger.clearFlags();
      int draggerFlags = 0;
      
      if ((mods & draggerConstrainMask) == draggerConstrainMask) {
         draggerFlags |= Dragger3d.CONSTRAIN;
      }
      if ((mods & draggerRepositionMask) == draggerRepositionMask) {
         draggerFlags |= Dragger3d.REPOSITION;
      }
      
      dragger.setFlags(draggerFlags);
      
   }
   
   private void updateDraggerModeAndFlags(int mods, Dragger3d dragger) {
      
      dragger.clearFlags();
      int draggerFlags = 0;
      Dragger3d.DragMode dragMode = Dragger3d.DragMode.OFF;
      
      if ((mods & draggerConstrainMask) == draggerConstrainMask) {
         draggerFlags |= Dragger3d.CONSTRAIN;
      }
      if ((mods & draggerRepositionMask) == draggerRepositionMask) {
         draggerFlags |= Dragger3d.REPOSITION;
         dragMode = Dragger3d.DragMode.REPOSITION;
      } else if ((mods & draggerDragMask) == draggerDragMask) {
         dragMode = Dragger3d.DragMode.DRAG;
      }
      
      dragger.setDragMode(dragMode);
      dragger.setFlags(draggerFlags);
      
   }
   
   public void mousePressed (MouseEvent e) {
      
      int mask = (e.getModifiersEx() & ALL_MODIFIERS);
      int selectionMask = getSelectionButtonMask();

      // Start checking draggers
      Dragger3d drawTool = viewer.myDrawTool;
      boolean grabbed = false;
      if (viewer.myDraggers.size() > 0 || drawTool != null) {
         MouseRayEvent rayEvt = MouseRayEvent.create (e, viewer);
         for (Dragger3d d : viewer.myDraggers) {
            // pass event to any visible Dragger3dBase, or any other Dragger3d
            if (d.isVisible()) {
               updateDraggerModeAndFlags(mask, d);
               if (d.mousePressed (rayEvt)) {
                  grabbed = true;
                  dragAction = DRAGGER_ACTION;
                  break;
               }
            }
         }
         if (drawTool != null && drawTool.isVisible()) {
            updateDraggerModeAndFlags(mask, drawTool);
            if (drawTool.mousePressed (rayEvt)) {
               grabbed = true;
               dragAction = DRAGGER_ACTION;
            }
         }
      }
      
      if (grabbed) {
         viewer.repaint();
      } else {
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
         else if (mask == getTranslateButtonMask()) {
            dragAction = TRANSLATE;
         }
         else if (mask == getRotateButtonMask()) {
            dragAction = ROTATE;
         }
         else if (mask == getZoomButtonMask()) {
            dragAction = ZOOM;
         }
         else {
            dragAction = NO_ACTION;
         }
      }
      
      dragStartX = lastX = e.getX();
      dragStartY = lastY = e.getY();
   }

   public void mouseReleased (MouseEvent e) {
      
      // End any dragger modes
      Dragger3d drawTool = viewer.myDrawTool;
      boolean grabbed = false;

      if (dragAction == DRAG_SELECT) {
         checkForSelection (e);
         viewer.setDragBox (null);
      } else if (dragAction == DRAGGER_ACTION) {
         if (viewer.myDraggers.size() > 0 || drawTool != null) {
            MouseRayEvent rayEvt = MouseRayEvent.create (e, viewer);
            for (Dragger3d d : viewer.myDraggers) {
               // pass event to any visible Dragger3dBase, or any other Dragger3d
               if (d.isVisible()) {
                  if (d.mouseReleased (rayEvt)) {
                     grabbed = true;
                     d.clearFlags();
                     d.setDragMode(DragMode.OFF);
                     break;
                  }
               }
            }
            if (drawTool != null && drawTool.isVisible()) {
               if (drawTool.mouseReleased (rayEvt)) {
                  grabbed = true;
                  drawTool.clearFlags();
                  drawTool.setDragMode(DragMode.OFF);
               }
            }
         }
         if (grabbed) {
            viewer.repaint();
         }
      }
      
      dragAction = NO_ACTION;
      
      //      if (dragAction == NO_ACTION) {
      //         Dragger3d drawTool = viewer.myDrawTool;
      //         boolean grabbed = false;
      //
      //         if (viewer.myDraggers.size() > 0 || drawTool != null) {
      //            MouseRayEvent rayEvt = MouseRayEvent.create (e, viewer);
      //            for (Dragger3d d : viewer.myDraggers) {
      //               // pass event to any visible Dragger3dBase, or any other Dragger3d
      //               if (d.isVisible()) {
      //                  if (d.mouseReleased (rayEvt)) {
      //                     grabbed = true;
      //                     break;
      //                  }
      //               }
      //            }
      //            if (drawTool != null && drawTool.isVisible()) {
      //               if (drawTool.mouseReleased (rayEvt)) {
      //                  grabbed = true;
      //               }
      //            }
      //         }
      //         if (grabbed) {
      //            viewer.repaint();
      //         }
      //      }
      //      else if (dragAction == DRAG_SELECT) {
      //         checkForSelection (e);
      //         viewer.setDragBox (null);
      //      }
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
         case DRAGGER_ACTION: {
            Dragger3d drawTool = viewer.myDrawTool;
            boolean grabbed = false;
            int mods = e.getModifiersEx() & ALL_MODIFIERS;

            if (viewer.myDraggers.size() > 0 || drawTool != null) {
               MouseRayEvent rayEvt = MouseRayEvent.create (e, viewer);
               for (Dragger3d d : viewer.myDraggers) {
                  // pass to any visible Dragger3dBase, or any other Dragger3d
                  if (d.isVisible()) {
                     updateDraggerFlags(mods, d);
                     if (d.mouseDragged (rayEvt)) {
                        grabbed = true;
                        break;
                     }
                  }
               }
               if (drawTool != null && drawTool.isVisible()) {
                  updateDraggerFlags(mods, drawTool);
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
      
      //if (dragAction == DRAGGER_ACTION) {
         
         int mods = e.getModifiersEx() & ALL_MODIFIERS;
         Dragger3d drawTool = viewer.myDrawTool;
         boolean grabbed = false;
   
         if (viewer.myDraggers.size() > 0 || drawTool != null) {
            MouseRayEvent rayEvt = MouseRayEvent.create (e, viewer);
            for (Dragger3d d : viewer.myDraggers) {
               // pass event to any visible Dragger3dBase, or any other Dragger3d
               if (d.isVisible()) {
                  updateDraggerFlags(mods, d);
                  if (d.mouseMoved (rayEvt)) {
                     grabbed = true;
                     break;
                  }
               }
            }
            if (drawTool != null && drawTool.isVisible()) {
               updateDraggerFlags(mods, drawTool);
               if (drawTool.mouseMoved (rayEvt)) {
                  grabbed = true;
               }
            }
         }
         if (grabbed) {
            viewer.repaint();
         }
      //}
   }

   /**
    * to get the zoom function going through the mouse wheel which is easier to
    * navigate, then by using key combination CTRL + mouse click + mouse drag.
    * The latter is still in the system for backward compatibility author:
    * andreio
    */
   public void mouseWheelMoved (MouseWheelEvent e) {
      int yOff = (int)(e.getWheelRotation() * myWheelZoomScale);
      double r = yOff < 0 ? 0.99 : 1.01;
      double s = Math.pow (r, Math.abs (yOff));
      viewer.zoom (s);
   }
   
   /**
    * Sets the mouse button mask that enables rotation. This should be a
    * combination of the following mouse buttons and extended modifiers defined
    * in java.awt.event.InputEvent: BUTTON1_DOWN_MASK, BUTTON2_DOWN_MASK,
    * BUTTON2_DOWN_MASK, SHIFT_DOWN_MASK, ALT_DOWN_MASK, META_DOWN_MASK, and
    * CTRL_DOWN_MASK.
    * 
    * @param mask
    * rotation button mask
    */
   public void setRotateButtonMask (int mask) {
      rotateButtonMask = mask;
   }

   /**
    * Gets the mouse button mask that enables rotation.
    * 
    * @return rotation button mask
    * @see #setRotateButtonMask
    */
   public int getRotateButtonMask() {
      return rotateButtonMask;
   }

   /**
    * Sets the mouse button mask that enables translation. This should be a
    * combination of the following mouse buttons and extended modifiers defined
    * in java.awt.event.InputEvent: BUTTON1_DOWN_MASK, BUTTON2_DOWN_MASK,
    * BUTTON2_DOWN_MASK, SHIFT_DOWN_MASK, ALT_DOWN_MASK, META_DOWN_MASK, and
    * CTRL_DOWN_MASK.
    * 
    * @param mask
    * translation button mask
    */
   public void setTranslateButtonMask (int mask) {
      translateButtonMask = mask;
   }

   /**
    * Gets the mouse button mask that enables translation.
    * 
    * @return translation button mask
    * @see #setTranslateButtonMask
    */
   public int getTranslateButtonMask() {
      return translateButtonMask;
   }

   /**
    * Sets the mouse button mask that enables zooming. This should be a
    * combination of the following mouse buttons and extended modifiers defined
    * in java.awt.event.InputEvent: BUTTON1_DOWN_MASK, BUTTON2_DOWN_MASK,
    * BUTTON2_DOWN_MASK, SHIFT_DOWN_MASK, ALT_DOWN_MASK, META_DOWN_MASK, and
    * CTRL_DOWN_MASK.
    * 
    * @param mask
    * zooming button mask
    */
   public void setZoomButtonMask (int mask) {
      zoomButtonMask = mask;
   }

   /**
    * Gets the mouse button mask that enables zooming.
    * 
    * @return zooming button mask
    * @see #setZoomButtonMask
    */
   public int getZoomButtonMask() {
      return zoomButtonMask;
   }


   /**
    * set the mouse wheel zoom amount modified by Charles Krzysik on Apr 11th
    * 2008 default: 100
    * 
    * @param s
    * zoomAmount
    */
   public void setMouseWheelZoomScale (double s) {
      myWheelZoomScale = s; // originally s
   }

   /**
    * get the mouse wheel zoom amount default: 100
    * 
    * @return zoomAmount
    */
   public double getMouseWheelZoomScale() {
      return myWheelZoomScale;
   }

   /**
    * Sets the mouse button mask that enables selection. This should be a
    * combination of the following mouse buttons and extended modifiers defined
    * in java.awt.event.InputEvent: BUTTON1_DOWN_MASK, BUTTON2_DOWN_MASK,
    * BUTTON2_DOWN_MASK, SHIFT_DOWN_MASK, ALT_DOWN_MASK, META_DOWN_MASK, and
    * CTRL_DOWN_MASK.
    * 
    * @param mask
    * selection button mask
    */
   public void setSelectionButtonMask (int mask) {
      selectionButtonMask = mask;
   }

   /**
    * Gets the mouse button mask that enables selection.
    * 
    * @return selection button mask
    * @see #setSelectionButtonMask
    */
   public int getSelectionButtonMask() {
      return selectionButtonMask;
   }

   /**
    * Sets the modifier mask that enables multiple selection. This should be a
    * combination of the following extended modifiers defined in
    * java.awt.event.InputEvent: SHIFT_DOWN_MASK, ALT_DOWN_MASK, META_DOWN_MASK,
    * and CTRL_DOWN_MASK.
    * 
    * @param mask
    * multiple selection modifier mask
    */
   public void setMultipleSelectionMask (int mask) {
      multipleSelectionMask = mask;
   }

   /**
    * Gets the modifier mask that enables multiple selection.
    * 
    * @return multiple selection modifier mask
    * @see #setMultipleSelectionMask
    */
   public int getMultipleSelectionMask() {
      return multipleSelectionMask;
   }
   
   //   XXX Unused
   //   /**
   //    * Sets the modifier mask to enable drag selection. This mask should be a
   //    * combination of the following extended modifiers defined in
   //    * java.awt.event.InputEvent: SHIFT_DOWN_MASK, ALT_DOWN_MASK, META_DOWN_MASK,
   //    * and CTRL_DOWN_MASK.
   //    * 
   //    * @param mask
   //    * selection modifier mask
   //    */
   //   public void setDragSelectionMask (int mask) {
   //      dragSelectionMask = mask;
   //   }
   //
   //   /**
   //    * Gets the modifier mask that enables drag selection.
   //    * 
   //    * @return drag selection modifier mask
   //    * @see #setDragSelectionMask
   //    */
   //   public int getDragSelectionMask() {
   //      return dragSelectionMask;
   //   }

   /**
    * Returns the mouse button modifiers that may accompany selection.
    * 
    * @return selection modifier mask
    */
   public int getSelectionModifierMask() {
      return selectionModMask;
   }
   
   public int getDraggerConstrainMask() {
      return draggerConstrainMask;
   }
   
   public void setDraggerConstrainMask(int mask) {
      draggerConstrainMask = mask;
   }
   
   public int getDraggerRepositionMask() {
      return draggerRepositionMask;
   }
   
   public void setDraggerRepositionMask(int mask) {
      draggerRepositionMask = mask;
   }
   
   public void setDraggerDragMask(int mask) {
      draggerDragMask = mask;
   }
   
   public int getDraggerDragMask() {
      return draggerDragMask;
   }

}
