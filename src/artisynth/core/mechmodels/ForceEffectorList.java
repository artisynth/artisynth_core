/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.*;

import maspack.util.*;
import artisynth.core.modelbase.*;
import maspack.render.*;
import artisynth.core.util.*;

import java.util.*;

public class ForceEffectorList extends ComponentList<ForceComponent> implements
ScalableUnits {
   protected static final long serialVersionUID = 1;

   public ForceEffectorList () {
      this (null, null);
   }
   
   public ForceEffectorList (String name, String shortName) {
      super (ForceComponent.class, name, shortName);
   }

   public boolean hasParameterizedType() {
      return false;
   }
   
   public void scaleDistance (double s) {
      for (int i = 0; i < size(); i++) {
         ForceEffector fe = get (i);
         if (fe instanceof ScalableUnits) {
            ((ScalableUnits)fe).scaleDistance (s);
         }
      }
   }

   public void scaleMass (double s) {
      for (int i = 0; i < size(); i++) {
         ForceEffector fe = get (i);
         if (fe instanceof ScalableUnits) {
            ((ScalableUnits)fe).scaleMass (s);
         }
      }
   }
   

}
