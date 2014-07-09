/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

public class DemoModel {
   private String name;

   private String file;

   public DemoModel(String name, String file) {
      this.setName(name);
      this.setFile(file);
   }

   public DemoModel(String file) {
      this.setName(file);
      this.setFile(file);
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getFile() {
      return file;
   }

   public void setFile(String file) {
      this.file = file;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof DemoModel)) { return false; }
      DemoModel dmObj = (DemoModel) obj;
      return (this.file.equals(dmObj.file) && this.name.equals(dmObj.name));
   }
}
