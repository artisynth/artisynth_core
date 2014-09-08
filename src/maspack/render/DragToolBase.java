/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), 
 * Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

/**
 * Base class for tools that work using mouse-based drag movements.
 */
public abstract class DragToolBase implements Dragger3d {

   protected int myFlags;
   
   protected DragMode myDragMode = DragMode.OFF;
   public boolean isDragging() {
      return myDragMode != DragMode.OFF;
   }
   
   //   // Indicates that the dragger motions should be constrained to a
   //   // convenient fixed-size interval
   //   protected int myConstrainMask = MouseEvent.SHIFT_DOWN_MASK;
   //   
   //   // Indicates that the dragger should drag the object(s) it is attached to
   //   protected int myDragMask = InputEvent.BUTTON1_DOWN_MASK;
   //   
   //   // Indicates that the dragger should reposition its coordinate frame 
   //   // without dragging drag the object(s) it is attached to. 
   //   protected int myRepositionMask = 
   //      (InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
   //   
   //   protected int myModsMask = 
   //      (myDragMask | myConstrainMask | myRepositionMask);      

   //   protected DragMode getDragMode (MouseEvent e) {
   //      
   //      int mods = (e.getModifiersEx() & myModsMask);
   //      if ((mods & myConstrainMask) == myConstrainMask) {
   //         myFlags |= CONSTRAIN;
   //      }
   //      if ((mods & myRepositionMask) == myRepositionMask) {
   //         myFlags |= REPOSITION;
   //      }
   //      
   //      if (mods == myRepositionMask) {
   //         return DragMode.REPOSITION;
   //      }
   //      else if ((mods & myDragMask) == myDragMask) {
   //         return DragMode.DRAG;
   //      }
   //      else {
   //         return DragMode.OFF;
   //      }
   //      
   //   }

   //   protected boolean dragIsConstrained (MouseEvent e) {
   //      return (e.getModifiersEx() & myConstrainMask) == myConstrainMask;
   //   }
   
   protected boolean dragIsConstrained () {
      return (myFlags & CONSTRAIN) != 0;
   }

   //   protected boolean dragIsRepositioning (MouseEvent e) {
   //      return (e.getModifiersEx() & myRepositionMask) == myRepositionMask;
   //   }
   
   protected boolean dragIsRepositioning () {
      return (myFlags & REPOSITION) != 0;
   }

   // public void updateKeyMasks (GLViewer viewer) {
      
      //      myConstrainMask = MouseEvent.SHIFT_DOWN_MASK;
      //
      //      if (viewer != null && viewer.getMouseHandler() != null) {
      //         myDragMask = viewer.getMouseHandler().getSelectionButtonMask();
      //      } else {
      //         myDragMask = InputEvent.BUTTON1_DOWN_MASK;
      //      }
      //      myRepositionMask = 
      //         (InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
      //      
      //      // add "alt" button to move if dragging uses CTRL+Button1
      //      if (myRepositionMask == myDragMask) {
      //         myRepositionMask |= InputEvent.ALT_DOWN_MASK;
      //      }
      //      
      //      myModsMask = (myDragMask | myConstrainMask | myRepositionMask);
   // }
   
   public int getFlags() {
      return myFlags;
   }
   
   public void setFlags(int f) {
      myFlags = f;
   }
   
   public void clearFlags() {
      myFlags = 0;
   }
   
   public void setDragMode(DragMode mode) {
      myDragMode = mode;
   }
   
   public DragMode getDragMode() {
      return myDragMode;
   }

}
