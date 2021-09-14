/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import javax.swing.JSeparator;

import artisynth.core.modelmenu.ModelScriptMenuParser.MenuType;

public class SeparatorEntry extends MenuNode {

   JSeparator mySeparator;

   public SeparatorEntry() {
      super();
   }

   public SeparatorEntry (String title) {
      super (title);
   }

   @Override
   public MenuType getType() {
      return MenuType.DIVIDER;
   }

   public JSeparator getComponent() {
      return mySeparator;
   }

   public Component updateComponent (ModelScriptMenu modelMenu) {
      if (mySeparator == null) {
         mySeparator = new JSeparator();
         mySeparator.setLayout (new GridLayout());
      }
      return mySeparator;
   }
      
}
