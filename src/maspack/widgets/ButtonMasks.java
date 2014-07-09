/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.event.InputEvent;

/**
 * Defines button masks for various generic operations. Different button masks
 * may be needed on different systems that have different numbers of mouse
 * buttons or different look-and-feel standards.
 */
public class ButtonMasks {
   private static int contextMenuButtonMask = (InputEvent.BUTTON3_DOWN_MASK);

   public static int getContextMenuMask() {
      return contextMenuButtonMask;
   }

   public static void setContextMenuMask (int mask) {
      contextMenuButtonMask = mask;
   }
}
