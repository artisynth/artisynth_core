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
import javax.swing.event.TreeWillExpandListener;

/***
 * @author andreio to respond to expansion and contraction events of the
 *         NavPanelMutableNode tree nodes
 */

public class NavPanelWillExpandListener implements TreeWillExpandListener {
   JTree tree = null;

   NavPanelWillExpandListener() {
      super();
   }

   public void treeWillCollapse(TreeExpansionEvent e) {
   }

   public void treeWillExpand(TreeExpansionEvent e) {
      Object obj = e.getPath().getLastPathComponent();
      if (obj instanceof NavPanelNode) {
	 NavPanelNode node = (NavPanelNode) obj;
	 if (!node.isChildListExpanded()) {
	    node.buildChildList();
	 }
      }
   }

}
