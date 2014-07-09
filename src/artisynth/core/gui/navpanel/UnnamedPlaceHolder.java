/**
 * Copyright (c) 2014, by the Authors: Chad Decker (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.navpanel;

public class UnnamedPlaceHolder {
   NavPanelNode myParent;

   public UnnamedPlaceHolder(NavPanelNode parent) {
      myParent = parent;
   }

   public void toggleUnnamedVisible() {
      myParent.setUnnamedVisible(!myParent.isUnnamedVisible());
   }
}
