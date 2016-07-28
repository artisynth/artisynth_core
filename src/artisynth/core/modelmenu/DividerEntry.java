/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

import artisynth.core.modelmenu.DemoMenuParser.MenuType;

public class DividerEntry extends MenuNode {

   public DividerEntry(String title) {
      super(title);
   }

   @Override
   public MenuType getType() {
      return MenuType.DIVIDER;
   }

}