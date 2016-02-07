/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.*;

import maspack.matrix.AffineTransform3dBase;
import maspack.render.*;
import maspack.util.*;
import artisynth.core.modelbase.*;
import maspack.render.*;
import artisynth.core.util.*;

import java.util.*;

public class MuscleBundleList
   extends RenderableComponentList<MuscleBundle> implements ScalableUnits {

   protected static final long serialVersionUID = 1;

   public MuscleBundleList () {
      this (null, null);
   }
   
   public MuscleBundleList (String name, String shortName) {
      super (MuscleBundle.class, name, shortName);
      setRenderProps (createRenderProps());
   }

   public boolean hasParameterizedType() {
      return false;
   }

   public RenderProps createRenderProps() {
      return RenderProps.createLineProps (this);
   }

   public void scaleDistance (double s) {
      for (int i = 0; i < size(); i++) {
         get (i).scaleDistance (s);
      }
      if (myRenderProps != null) {
         myRenderProps.scaleDistance (s);
      }
   }

   public void scaleMass (double s) {
      for (int i = 0; i < size(); i++) {
         get (i).scaleMass (s);
      }
   }

}
