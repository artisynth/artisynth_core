/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;

public class LabeledControlOptionPaneDemo {
   public int doDialogue() {

      LabeledComponentPanel panel = new LabeledComponentPanel();
      panel.addWidget (new DoubleField ("number", 0));
      panel.addWidget (new StringField ("tag", 8));
      panel.setBorder (BorderFactory.createEtchedBorder());
      int ret =
         JOptionPane.showConfirmDialog (
            null, panel, "Dialogue title", JOptionPane.YES_NO_OPTION,
            JOptionPane.PLAIN_MESSAGE);

      return ret;
   }

   public LabeledControlOptionPaneDemo() {
      int ret = doDialogue();
      if (ret == JOptionPane.YES_OPTION) {
         System.out.println ("YES returned: " + ret);
      }
      else {
         System.out.println ("Returned: " + ret);
      }
   }

   public static void main (String[] args) {
      new LabeledControlOptionPaneDemo();
   }
}
