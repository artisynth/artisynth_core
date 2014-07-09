/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import artisynth.core.util.ScalableUnits;
import maspack.matrix.*;
import maspack.render.*;

public class ScalableList<C extends ModelComponent & ScalableUnits> extends
ComponentList<C> implements ScalableUnits {

   public ScalableList (Class<C> type) {
      super (type, null, null);
   }
   
   public ScalableList (Class<C> type, String name, String shortName) {
      super (type, name, shortName);
   }

//   public ScalableList (Class<C> type, String name, String shortName,
//   CompositeComponent parent) {
//      super (type, name, shortName, parent);
//   }

   public void scaleDistance (double s) {
      for (int i = 0; i < size(); i++) {
         get (i).scaleDistance (s);
      }
   }

   public void scaleMass (double s) {
      for (int i = 0; i < size(); i++) {
         get (i).scaleMass (s);
      }
   }
}
