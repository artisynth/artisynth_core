/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui;

import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import artisynth.core.driver.Main;

public class RerenderListener implements ValueChangeListener {

   public void valueChange (ValueChangeEvent e) {
      if (Main.getMain() != null) {
         Main.getMain().rerender();
      }
   }
}
