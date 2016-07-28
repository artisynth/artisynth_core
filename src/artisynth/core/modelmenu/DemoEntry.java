/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

import artisynth.core.driver.ModelInfo;
import artisynth.core.modelmenu.DemoMenuParser.MenuType;

public class DemoEntry extends MenuNode {
   private ModelInfo model;

   public DemoEntry(String filename, String[] args) {
      super(filename);
      setModel(new ModelInfo(filename, filename, args));
   }

   public DemoEntry(String file, String name, String[] args) {
      super(name);
      setModel(new ModelInfo(file, name, args));
   }

   public ModelInfo getModel() {
      return model;
   }

   public void setModel(ModelInfo model) {
      this.model = model;
   }

   @Override
   public MenuType getType() {
      return MenuType.MODEL;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof DemoEntry)) { return false; }
      boolean res = super.equals((MenuNode) obj);
      res = res && (model.equals(((DemoEntry) obj).model));
      return res;
   }
}
