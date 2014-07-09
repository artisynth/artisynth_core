/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

public enum PropertyMode {
   /**
    * Indicates that a property value is explicitly set, and should not be
    * inherited from matching properties in an ancestor hierarchy.
    */
   Explicit,

   /**
    * Indicates that a property value is to be inherited from the closest
    * explicitly-set matching property in the ancestor hierarchy.
    */
   Inherited,

   /**
    * Indicates that a property value is inactive. As with {@link #Inherited
    * Inherited}, its value is to be inherited from the closest explicitly-set
    * matching property in the ancestor hierarchy. However, when an inactive
    * value is set, it's mode is not automatically set to <code>Explicit</code>
    * and its new value it not propagated to hierarchy descendants.
    */
   Inactive,

   /**
    * Indicates no mode value. Reserved for internal system use.
    */
   Void
}
