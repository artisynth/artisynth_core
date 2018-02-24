/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import artisynth.core.femmodels.FemNode3d;
import maspack.matrix.Point3d;
import maspack.matrix.VectorNd;

public interface MFreePoint3d {
   
   public FemNode3d[] getDependentNodes();
   public void setDependentNodes(FemNode3d[] nodes, VectorNd coords);
   public VectorNd getNodeCoordinates();
   public void setNodeCoordinates(VectorNd coords);
   public void updatePosState();
   public void updateVelState();
   public void updateSlavePos();
   public Point3d getPosition();
   public Point3d getRestPosition();
   
   /**
    * Removes all dependencies with have very low weights
    * ( |w| &lt;= tol )
    * @return true if modified
    */
   public boolean reduceDependencies(double tol);
   
   
}
