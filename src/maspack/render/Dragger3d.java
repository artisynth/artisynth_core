/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;



public interface Dragger3d extends IsSelectable {
   //public boolean isSelected();
   
   public enum DraggerType {
      None, Rotator, Translator, Transrotator, Scalar, ConstrainedTranslator, Jack
   };
   
   public final int CONSTRAIN = 0x01;
   public final int REPOSITION = 0x02;
   
   public enum DragMode {
      OFF, DRAG, REPOSITION
   }
   
   public boolean mouseClicked (MouseRayEvent e);

   public boolean mousePressed (MouseRayEvent e);

   public boolean mouseReleased (MouseRayEvent e);

   public boolean mouseDragged (MouseRayEvent e);

   public boolean mouseMoved (MouseRayEvent e);

   public boolean isVisible();

   // Flags used for setting various modes
   public int getFlags();
   
   public void setFlags(int f);
   
   public void clearFlags();
   
   public void setDragMode(DragMode mode);
   
   public DragMode getDragMode();
   
   //public void draggerSelected (MouseRayEvent e);
}
