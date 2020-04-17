/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.io.*;
import java.util.*;

import maspack.util.*;
import maspack.properties.*;
import artisynth.core.util.*;

/**
 * A simple component that provides a reference to another component. Used as
 * the building block for ReferenceLists.
 */
public class WeightedReferenceComp<C extends ModelComponent> 
   extends ReferenceComp<C> {

   public static double DEFAULT_WEIGHT = 1.0;
   protected double myWeight = DEFAULT_WEIGHT;

   public static PropertyList myProps = new PropertyList (
      WeightedReferenceComp.class, ReferenceComp.class);

   static {
      myProps.add (
         "weight", "weighting term for this exciter", DEFAULT_WEIGHT);
   }

   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public WeightedReferenceComp () {
      super();
   }
   
   public WeightedReferenceComp (C ref) {
      super (ref);
   }
   
   public WeightedReferenceComp (C ref, double weight) {
      super (ref);
      setWeight (weight);
   }
   
   public double getWeight() {
      return myWeight;
   }

   public void setWeight (double weight) {
      myWeight = weight;
   }
}
