/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

public class StartFromMatlab {

   public StartFromMatlab() {
      this (null);
   }

   public StartFromMatlab (String[] args) {
      if (args == null) {
         args = new String[0];
      }
      Main.main (args);
      Main.getMain().setRunningUnderMatlab (true);
   }

   public Main getMain() {
      return Main.getMain();
   }

}
