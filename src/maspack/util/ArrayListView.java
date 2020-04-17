/**
 * Copyright (c) 2020, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.ArrayList;

/**
 * ArrayList based implementation of ListView
 */
public class ArrayListView<E> extends ArrayList<E> implements ListView<E> {

   public ArrayListView() {
      super();
   }
   
   public ArrayListView(int capacity) {
      super(capacity);
   }

}
