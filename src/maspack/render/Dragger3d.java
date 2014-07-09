/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;


public interface Dragger3d extends GLSelectable {
   //public boolean isSelected();

   public boolean mouseClicked (MouseRayEvent e);

   public boolean mousePressed (MouseRayEvent e);

   public boolean mouseReleased (MouseRayEvent e);

   public boolean mouseDragged (MouseRayEvent e);

   public boolean mouseMoved (MouseRayEvent e);

   public boolean isVisible();

   //public void draggerSelected (MouseRayEvent e);
}
