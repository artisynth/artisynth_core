/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Wrapper class which ``holds'' an Object reference, enabling methods to return
 * Object references through arguments.
 */
public class ObjectHolder implements java.io.Serializable {
   
   private static final long serialVersionUID = 1L;
   
   /**
    * Value of the Object reference, set and examined by the application as
    * needed.
    */
   public Object value;

   /**
    * Constructs a new <code>ObjectHolder</code> with an initial value of
    * <code>null</code>.
    */
   public ObjectHolder() {
      value = null;
   }

   /**
    * Constructs a new <code>ObjectHolder</code> with a specific initial
    * value.
    * 
    * @param o
    * Initial Object reference.
    */
   public ObjectHolder (Object o) {
      value = o;
   }
}
