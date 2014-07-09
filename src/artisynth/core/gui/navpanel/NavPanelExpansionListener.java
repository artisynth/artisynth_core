/**
 * Copyright (c) 2014, by the Authors: Chad Decker (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.navpanel;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;

/***
 * @author andreio to respond to expansion and contraction events of the
 *         NavPanelMutableNode tree nodes
 */

public class NavPanelExpansionListener implements TreeExpansionListener {
   JTree tree = null;

   NavPanelExpansionListener() {
      super();
   }

   public void treeCollapsed(TreeExpansionEvent e) {
      // clear child list and reclaim memory
      Object obj = e.getPath().getLastPathComponent();
      if (obj instanceof NavPanelNode) {
	 NavPanelNode node = (NavPanelNode) obj;
	 if (node.isChildListExpanded()) {
	    node.deallocateChildList();
	 }
      }
   }

   public void treeExpanded(TreeExpansionEvent e) {
      // shouldn't need to do anything here - child list should already
      // be expanded by the WillExpandListener

      // Object obj = e.getPath().getLastPathComponent();
      // if (obj instanceof NavPanelNode) {
      //    NavPanelNode node = (NavPanelNode) obj;
      //    System.out.println (
      //       "expanded: child list expanded=" + node.isChildListExpanded());
      //    if (!node.isChildListExpanded()) {
      //       node.buildChildList();
      //    }
      // }
   }

}
