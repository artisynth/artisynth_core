/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.function.DifferentiableFunction3x1;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

public abstract class MFreeWeightFunction implements DifferentiableFunction3x1 {

   public abstract MFreeWeightFunction clone();
   public abstract boolean intersects(MFreeWeightFunction fun);
   public abstract double getIntersectionVolume(MFreeWeightFunction fun);
   public abstract void computeIntersectionCentroid(Point3d centroid, MFreeWeightFunction fun);
   public abstract void computeCentroid(Vector3d centroid);
   public abstract void updateBounds(Vector3d min, Vector3d max);
   public abstract boolean isInDomain(Point3d pos, double tol);
   
}
