/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

public interface InheritableProperty extends Property {
   // public boolean isInherited();

   // public boolean isExplicit();

   // public boolean isInactive();

   /**
    * Sets the mode for this property. The mode should be either
    * {@link maspack.properties.PropertyMode#Explicit PropertyMode.Explicit},
    * {@link maspack.properties.PropertyMode#Inherited PropertyMode.Inherited},
    * or {@link maspack.properties.PropertyMode#Inactive PropertyMode.Inactive}.
    * 
    * @param mode
    * new mode for this property.
    * @see #getMode
    */
   public void setMode (PropertyMode mode);

   /**
    * Returns the current mode for this property.
    * 
    * @return current property mode
    * @see #setMode
    */
   public PropertyMode getMode();

}
