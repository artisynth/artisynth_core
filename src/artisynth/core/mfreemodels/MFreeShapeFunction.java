/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.function.DifferentiableFunction3x1;

public abstract class MFreeShapeFunction implements DifferentiableFunction3x1 {

   public enum MFreeShapeFunctionType {
      MLS, GMLS
   }
   
   public abstract MFreeShapeFunctionType getType();
   
   public static MFreeShapeFunction create(MFreeShapeFunctionType type, MFreeNode3d node) {
      switch(type) {
         case MLS:
            return new MLSShapeFunction(node);
         case GMLS:
            return new GMLSShapeFunction((MFreeHermiteNode3d)node, 1, 1, 1);
      }
      return null;
   }
   
}
