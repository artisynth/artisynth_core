package maspack.numerics;

import java.util.*;
import maspack.matrix.*;

/**
 * Estimates rigid transformations from point data. The class is initialized
 * with 3 or more points in local coordinates, and then estimates transforms
 * when given the sames points in world coordinates.
 *
 * <p>The algorithm is the same as the SVD algorithm given in D.W. Eggert,
 * A. Lorusso, R.B. Fischer, ``Estimating 3-D rigid body transformations: a
 * comparisonof four major algorithms'', Machine Vision and Applications (1997)
 * 9: 272–290.
 */
public class RigidPoseEstimator {
   
   protected ArrayList<Vector3d> myLocalPoints;
   protected Vector3d myLocalCenter;

   public RigidPoseEstimator() {
   }

   public RigidPoseEstimator (Collection<? extends Vector3d> lpoints) {
      setLocalPoints (lpoints);
   }

   /**
    * Sets the local points for this estimator.
    *
    * @param lpoints collection of local points
    */
   public void setLocalPoints (Collection<? extends Vector3d> lpoints) {
      if (lpoints.size() < 3) {
         throw new IllegalArgumentException (
            "Only "+lpoints.size()+" points specified; must be least 3");
      }
      // compute local center and store local points *relative* to this
      myLocalCenter = new Vector3d();
      for (Vector3d p : lpoints) {
         myLocalCenter.add (p);
      }
      myLocalCenter.scale (1.0/lpoints.size());
      myLocalPoints = new ArrayList<>();
      for (Vector3d p : lpoints) {
         Vector3d loc = new Vector3d(p);
         loc.sub (myLocalCenter);
         myLocalPoints.add (loc);
      }
   }

   /**
    * Estimates the transform from local to world coordinates given the
    * corresponding point positions in world coordinates. These are specified
    * as a single vector of length 3*nump, where nump is the number of points.
    *
    * @param T returns the transform 
    * @param wpositions world point positions given as a single vector
    */
   public void estimatePose (
      RigidTransform3d T, VectorNd wpositions) {
      if (myLocalPoints == null) {
         throw new IllegalStateException (
            "Estimator has not been initialized with local points");
      }
      int nump = myLocalPoints.size();
      if (wpositions.size() < 3*nump) {
         throw new IllegalArgumentException (
            "wpositions has size "+wpositions.size() +
            ", expected " + 3*nump);
      }
      ArrayList<Vector3d> wpoints = new ArrayList<>();
      for (int i=0; i<nump; i++) {
         Vector3d pos = new Vector3d();
         wpositions.getSubVector (3*i, pos);
         wpoints.add (pos);
      }
      estimatePose (T, wpoints);
   }

   /**
    * Estimates the transform from local to world coordinates given the
    * corresponding point positions in world coordinates.
    *
    * @param T returns the transform 
    * @param wpoints collection of world points
    */
   public void estimatePose (
      RigidTransform3d T, List<? extends Vector3d> wpoints) {

      if (myLocalPoints == null) {
         throw new IllegalStateException (
            "Estimator has not been initialized with local points");
      }
      int nump = myLocalPoints.size();
      if (wpoints.size() < nump) {
         throw new IllegalArgumentException (
            "Only "+wpoints.size()+" points specified; expected " + nump);
      }
      // compute world centroid
      Vector3d worldCenter = new Vector3d();
      for (Vector3d p : wpoints) {
         worldCenter.add (p);
      }
      worldCenter.scale (1.0/nump);
      // compute correlation matrix 
      Matrix3d F = new Matrix3d();
      Vector3d vec = new Vector3d();
      for (int i=0; i<nump; i++) {
         vec.sub (wpoints.get(i), worldCenter);
         F.addOuterProduct (vec, myLocalPoints.get(i));
      }
      // Use polar decomposition to extra R
      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factor (F);
      RotationMatrix3d R = polard.getR();

      // rotate local center to world:
      Vector3d localCenterInWorld = new Vector3d();
      localCenterInWorld.transform (R, myLocalCenter);
      
      T.R.set (R);
      T.p.sub (worldCenter, localCenterInWorld);            
   }
}
