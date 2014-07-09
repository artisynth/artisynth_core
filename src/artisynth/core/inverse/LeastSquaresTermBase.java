/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyList;


public abstract class LeastSquaresTermBase implements LeastSquaresTerm, HasProperties {

   protected double myWeight;
   public static final double defaultWeight = 1;
   
   
   public static PropertyList myProps =
      new PropertyList (LeastSquaresTermBase.class);

   static {
      myProps.add ("weight * *", "weighting factor for this optimization term", 1);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Property getProperty(String pathName) {
      return PropertyList.getProperty(pathName, this);
   }
   
   public LeastSquaresTermBase() {
      this(defaultWeight);
   }
   
   public LeastSquaresTermBase(double weight) {
      myWeight = weight;
   }

   public void setWeight(double w) {
      myWeight = w;
   }
   
   public double getWeight() {
      return myWeight;
   }

}
