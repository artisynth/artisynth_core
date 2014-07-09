/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import javax.swing.event.ChangeEvent;

public class ValueChangeEvent extends ChangeEvent {
   private static final long serialVersionUID = 1L;
   private Object myValue;

   public ValueChangeEvent (Object source, Object value) {
      super (source);
      myValue = value;
   }

   public Object getValue() {
      return myValue;
   }
}
