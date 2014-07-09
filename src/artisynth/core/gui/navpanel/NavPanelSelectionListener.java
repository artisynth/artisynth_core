/**
 * Copyright (c) 2014, by the Authors: Chad Decker (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.navpanel;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

public class NavPanelSelectionListener implements TreeSelectionListener {
   private NavPanelTreeModel myModel;

   public NavPanelSelectionListener(NavPanelTreeModel model) {
      myModel = model;
   }

   public void valueChanged(TreeSelectionEvent e) {
      int selcnt = 0;
      TreePath[] paths = e.getPaths();
      for (int i = 0; i < paths.length; i++) {
	 Object obj = paths[i].getLastPathComponent();
	 if (obj instanceof UnnamedPlaceHolder && e.isAddedPath(i)) {
	    ((UnnamedPlaceHolder) obj).toggleUnnamedVisible();
	 }
      }
   }
}
