/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

import artisynth.core.modelmenu.DemoMenuParser.MenuType;

public class DemoEntry extends MenuEntry {
   private DemoModel model;

   public DemoEntry(String filename) {
      super(filename);
      setModel(new DemoModel(filename, filename));
   }

   public DemoEntry(String name, String file) {
      super(name);
      setModel(new DemoModel(name, file));
   }

   public DemoModel getModel() {
      return model;
   }

   public void setModel(DemoModel model) {
      this.model = model;
   }

   @Override
   public MenuType getType() {
      return MenuType.MODEL;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof DemoEntry)) { return false; }
      boolean res = super.equals((MenuEntry) obj);
      res = res && (model.equals(((DemoEntry) obj).model));
      return res;
   }
}
