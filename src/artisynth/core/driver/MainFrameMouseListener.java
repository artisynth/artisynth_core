/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import artisynth.core.gui.navpanel.NavigationPanel;

/**
 * 
 * @author andreio to introduce the mouse listener to listen for events of type
 * mouseclick in the main splitter panel in the main window this is used to
 * control the contraction and expansion of the navigation panel on the main
 * panel
 * 
 */

class MainFrameMouseListener implements MouseListener {
   NavigationPanel myNavPanel;
   MainFrame myMainFrame;

   public void mouseClicked (MouseEvent e) {
      myNavPanel.setStatus (!myNavPanel.getStatus());
      myMainFrame.refreshSplitPane();
      myMainFrame.getNavPanel().setVisible (myNavPanel.getStatus());
   }

   public void setNavPanel (NavigationPanel navPanel) {
      myNavPanel = navPanel;
   }

   public void setMainFrame (MainFrame mainFrame) {
      myMainFrame = mainFrame;
   }

   public void mouseEntered (MouseEvent e) {
   }

   public void mouseExited (MouseEvent e) {
   }

   public void mousePressed (MouseEvent e) {
   }

   public void mouseReleased (MouseEvent e) {
      myNavPanel.updateParentSize();
   }

}
