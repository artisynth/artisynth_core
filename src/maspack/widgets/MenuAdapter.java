/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import javax.swing.JMenu;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

/**
 * Adapter class for swing.MenuListener. Implemented here because it
 * doesn't seem to exist in swing itself.
 */
public class MenuAdapter implements MenuListener {

   public void menuCanceled (MenuEvent m_evt) {
   }
   
   public void menuDeselected(MenuEvent m_evt) {
      JMenu menu = (JMenu)m_evt.getSource();
      menu.removeAll ();
   }
   
   public void menuSelected(MenuEvent m_evt) {
   }

}
