/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

import java.awt.Component;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.border.*;

import java.net.URL;

import artisynth.core.util.ArtisynthPath;
import artisynth.core.modelmenu.ModelScriptMenuParser.MenuType;

public class LabelEntry extends MenuNode {

   JLabel myLabel;

   public LabelEntry() {
      super();
   }

   public LabelEntry (String text) {
      super (text);
   }

   @Override
   public MenuType getType() {
      return MenuType.LABEL;
   }

   public JLabel getComponent() {
      return myLabel;
   }

   public Component updateComponent (ModelScriptMenu modelMenu) {

      if (myLabel == null) {
         myLabel = new JLabel (getTitle());
         myLabel.setBorder (new EmptyBorder (2, 7, 2, 2));
         if (getIcon() != null) {
            URL iconFile = ArtisynthPath.findResource (getIcon());
            myLabel.setIcon (new ImageIcon (iconFile));
         }
         myLabel.setFont (getFont());
      }
      else {
         if (!stringEquals (getTitle(), myLabel.getText())) {
            myLabel.setText (getTitle());
         }
         if (getFont() != myLabel.getFont()) {
            myLabel.setFont (getFont());
         }
      }
      return myLabel;
   }
}
