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
   public int hashCode() {
      return super.hashCode()*31 + model.hashCode();
   }
   
   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      if (obj == this) {
         return true;
      }
      if (obj.getClass() != getClass()) {
         return false;
      }
      
      DemoEntry other = (DemoEntry)obj;
      if (!(model.equals(other.model))) {
         return false;
      }
      
      return super.equals(other);
   }
   
   @Override
   public String toString() {
      return model.toString();
   }
}
