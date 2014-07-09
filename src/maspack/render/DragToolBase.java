/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.event.*;
/**
 * Base class for tools that work using mouse-based drag movements. In
 * particular, this class handles the necessary modifier masks.
 */
public class DragToolBase {

   protected enum DragMode {
      OFF, DRAG, REPOSITION
   }

   protected DragMode myDragMode = DragMode.OFF;
   
   // Indicates that the dragger motions should be constrained to a
   // convenient fixed-size interval
   protected int myConstrainMask = MouseEvent.SHIFT_DOWN_MASK;
   
   // Indicates that the dragger should drag the object(s) it is attached to
   protected int myDragMask = InputEvent.BUTTON1_DOWN_MASK;
   
   // Indicates that the dragger should reposition its coordinate frame 
   // without dragging drag the object(s) it is attached to. 
   protected int myRepositionMask = 
      (InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
   
   protected int myModsMask = 
      (myDragMask | myConstrainMask | myRepositionMask);      

   protected DragMode getDragMode (MouseEvent e) {
      int mods = (e.getModifiersEx() & myModsMask);
      if (mods == myRepositionMask) {
         return DragMode.REPOSITION;
      }
      else if ((mods & myDragMask) == myDragMask) {
         return DragMode.DRAG;
      }
      else {
         return DragMode.OFF;
      }
   }

   protected boolean dragIsConstrained (MouseEvent e) {
      return (e.getModifiersEx() & myConstrainMask) == myConstrainMask;
   }

   protected boolean dragIsRepositioning (MouseEvent e) {
      return (e.getModifiersEx() & myRepositionMask) == myRepositionMask;
   }

   public void updateKeyMasks (GLViewer viewer) {
      
      myConstrainMask = MouseEvent.SHIFT_DOWN_MASK;

      if (viewer != null) {
         myDragMask = viewer.getSelectionButtonMask();
      } else {
         myDragMask = InputEvent.BUTTON1_DOWN_MASK;
      }
      myRepositionMask = 
         (InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
      
      // add "alt" button to move if dragging uses CTRL+Button1
      if (myRepositionMask == myDragMask) {
         myRepositionMask |= InputEvent.ALT_DOWN_MASK;
      }
      
      myModsMask = (myDragMask | myConstrainMask | myRepositionMask);
   }

}