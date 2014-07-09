/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import artisynth.core.util.ScalableUnits;
import artisynth.core.util.TransformableGeometry;
import maspack.matrix.*;
import maspack.render.*;

public class TransformableList<C extends ModelComponent & ScalableUnits & TransformableGeometry>
extends ScalableList<C> implements TransformableGeometry {
   
   public TransformableList (Class<C> type) {
      this (type, null, null);
   }
   
   public TransformableList (Class<C> type, String name, String shortName) {
      super (type, name, shortName);
   }

//   public TransformableList (Class<C> type, String name, String shortName,
//   CompositeComponent parent) {
//      super (type, name, shortName, parent);
//   }

   public void transformGeometry (AffineTransform3dBase X) {
      transformGeometry (X, this, 0);
   }

   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
      for (int i = 0; i < size(); i++) {
         get (i).transformGeometry (X, topObject, flags);
      }
   }
}
