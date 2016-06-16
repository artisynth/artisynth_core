/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */


package maspack.render;

import java.awt.event.*;

public interface DrawTool extends IsSelectable {

   public boolean isVisible();

   public void mouseClicked (MouseRayEvent e);

   public boolean mousePressed (MouseRayEvent e);

   public void mouseReleased (MouseRayEvent e);

   public void mouseDragged (MouseRayEvent e);

   public boolean mouseMoved (MouseRayEvent e);

}
