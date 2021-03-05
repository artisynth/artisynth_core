package artisynth.core.opensim.customjoint;

import java.util.HashMap;

import artisynth.core.opensim.components.Constant;
import artisynth.core.opensim.components.Coordinate;
import artisynth.core.opensim.components.FunctionBase;
import artisynth.core.opensim.components.TransformAxis;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.MatrixNd;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SVDecomposition;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.spatialmotion.EulerFilter;
import maspack.spatialmotion.Wrench;
import maspack.spatialmotion.RigidBodyCoupling;
import maspack.spatialmotion.RigidBodyConstraint;
import maspack.spatialmotion.Twist;
import maspack.util.DoubleInterval;

/**
 * OpenSim Custom Joint coupling
 * - According to forums and documentation, OpenSim uses INTRINSIC rotations
 * - Tranlation is performed after rotation
 *
 */
public class OpenSimCustomCoupling extends RigidBodyCoupling {

   /**
    * "Coordinate" that drives the motion
    */
   public static class Coord {
      Coordinate info;
      double val;
      
      public Coord (Coordinate coordinate) {
         info = coordinate;
         val = coordinate.getDefaultValue ();
      }
      
      public double value() {
         return val;
      }
      
      public DoubleInterval getRange() {
         return info.getRange ();
      }

      public void projectCoordinates () {
         if (info.getClamped ()) {
            val = info.getRange ().clipToRange (val);
         } else if (info.getLocked ()) {
            val = info.getDefaultValue ();
         }
      }
   }
   
   /**
    * Axes (either translation or rotation) that is driven
    * by the coordinates and a "function"
    */
   public static class TAxis {
      TransformAxis info;
      Coord[] coords;
      DoubleInterval range;
      
      public TAxis (TransformAxis info, HashMap<String,Coordinate> cmap) {
         this.info = info;
         String[] coordsNames = info.getCoordinates ();
         int len = 0;
         if (coordsNames != null) {
            len = coordsNames.length;
         }
         
         coords = new Coord[len];
         for (int i=0; i<len; ++i) {
            coords[i] = new Coord(cmap.get(coordsNames[i]));
         }
         range = computeBounds ();
      }

      public Vector3d getAxis() {
         return info.getAxis ();
      }
      
      public boolean isFixed() {
         // fixed if no coords
         if (coords.length == 0) {
            return true;
         }
         // fixed if function is constant
         FunctionBase func = info.getFunction ();
         if (func == null || func instanceof Constant) {
            return true;
         }
         // fixed if all coords are fixed
         for (Coord c : coords) {
            if (!c.info.getLocked ()) {
               return false;
            }
         }
         return true;
      }
      
      public boolean isRestricted() {
         // restricted if all coords are restricted
         if (coords.length == 0) {
            return true;
         }
         for (Coord c : coords) {
            if (!c.info.getClamped ()) {
               return false;
            }
         }
         return true;
      }
      
      public double evaluate() {
         FunctionBase func = info.getFunction ();
         if (func == null) {
            return 0;
         }
         
         // collect values
         VectorNd cvals = new VectorNd(coords.length);
         for (int i=0; i<coords.length; ++i) {
            cvals.set(i, coords[i].val);
         }
         
         double val = func.evaluate(cvals);
         
         return val;
      }
      
      public double getLowerBound() {
         return range.getLowerBound ();
      }
      
      public double getUpperBound() {
         return range.getUpperBound ();
      }
      
      public DoubleInterval computeBounds() {
         // check all coordinate bounds
         double min = Double.POSITIVE_INFINITY;
         double max = Double.NEGATIVE_INFINITY;
         
         FunctionBase f = info.getFunction ();
         if (f == null) {
            return new DoubleInterval();
         }
         
         // XXX should really maximize/minimize rather than just evaluate at bounds
         // try all bounds
         VectorNd cvals = new VectorNd(coords.length);
         int ninputs = 1<<coords.length;
         for (int i=0; i<ninputs; ++i) {
            for (int j = 0; j < coords.length; ++j) {
               boolean set = ((1<<j) & i) != 0;
               if (set) {
                  cvals.set(j, coords[j].getRange().getLowerBound ());
               } else {
                  cvals.set(j, coords[j].getRange ().getUpperBound ());
               }
            }
            double val = f.evaluate (cvals);
            if (val < min) {
               min = val;
            }
            if (val > max) {
               max = val;
            }
         }
         
         return new DoubleInterval(min, max);
      }
      
      
      /**
       * Update internal coordinate given a desired value, uses gradient descent
       * respecting bounds
       * @param fx f(x), we seek x
       */
      public void updateCoords(double fx) {
         
         FunctionBase func = info.getFunction ();
         // fx = range.clipToRange (fx);
         
         int N = coords.length;
         
         // current value and coordinates
         // collect values
         VectorNd x = new VectorNd(N);
         for (int i=0; i<N; ++i) {
            x.set(i, coords[i].val);
         }
         VectorNd nx = new VectorNd(N);

         // gradient descent respecting bounds
         // minimize 0.5*|dx|^2 + lambda*(fx - f - df*dx)
         MatrixNd J = new MatrixNd(N+1, N+1);
         J.setIdentity ();
         J.set(N, N, 0);
         
         VectorNd b = new VectorNd(N+1);
         VectorNd h = new VectorNd(N+1);
         
         double f = func.evaluate(x);
         VectorNd df = new VectorNd(coords.length);
         func.evaluateDerivative(x, df);
         
         double err0 = Math.abs (f - fx);
         double err = err0;
         int niters = 0;
         SVDecomposition svd = new SVDecomposition ();
         while (err > 1e-5*err0 && niters < 100) {
            
            for (int i=0; i<N; ++i) {
               J.set(N, i, df.get(i));
               J.set(i, N, df.get(i));
            }
            b.set(N, fx-f);
            
            // factor and solve for new step and lambda
            svd.factor (J);
            svd.solve (h, b);
            
            @SuppressWarnings("unused")
            double lambda = h.get (N);
            
            // search with step direction in h
            double alpha = 1;
            double perr = err;
            do {
               for (int i=0; i<N; ++i) {
                  // only step part-way
                  double nval = x.get (i) + alpha*h.get (i);
                  // nval = coords[i].getRange ().clipToRange (nval);
                  nx.set (i,nval);
               }
               
               f = func.evaluate (nx);
               func.evaluateDerivative (nx, df);
               err = Math.abs(f - fx);
               alpha = alpha*0.5;  // reduce step size for next iteration
            } while (err > perr);
            
            // update x
            x.set(nx);
            niters++;
         }
         
         // update coordinates
         for (int i=0; i<N; ++i) {
            coords[i].val = x.get(i);
         }

      }

      public void projectCoordinates () {
         if (coords != null) {
            for (int j=0; j<coords.length; ++j) {
               coords[j].projectCoordinates();   
            }
         }
      }
   }
   
   TAxis[] rotAxes;
   TAxis[] transAxes;
   int[] order;
   Matrix3d transMat;
   Matrix3d invTransMat;
   Matrix3d rotMat;
   Matrix3d invRotMat;
   int bilaterals;
   int unilaterals;
   
   public OpenSimCustomCoupling(TransformAxis[] axes, Coordinate[] coords) {
      rotAxes = new TAxis[3];
      transAxes = new TAxis[3];
      
      // coordinate map
      HashMap<String,Coordinate> cmap = new HashMap<> ();
      for (Coordinate c : coords) {
         cmap.put(c.getName (), c);
      }
      
      for (int i=0; i<3; ++i) {
         rotAxes[i] = new TAxis(axes[i], cmap);
         transAxes[i] = new TAxis(axes[i+3], cmap);
      }
      
      bilaterals = 0;
      unilaterals = 0;
      order = null;
       
      transMat = new Matrix3d();
      invTransMat = new Matrix3d();
      rotMat = new Matrix3d();
      invRotMat = new Matrix3d();
      
      for (int i=0; i<3; i++) {
         transMat.setColumn(i, transAxes[i].getAxis());
         rotMat.setColumn(2-i, rotAxes[i].getAxis());  // first axes goes in z position (intrinsic angles)
         if (transAxes[i].isFixed()) {
            bilaterals++;
         } else if (transAxes[i].isRestricted()) {
            unilaterals++;
         }
         if (rotAxes[i].isFixed()) {
            bilaterals++;
         } else if (rotAxes[i].isRestricted()) {
            unilaterals++;
         }
      }
      
      invTransMat.set(transMat);
      if (!invTransMat.invert()) {
         SVDecomposition3d svd = new SVDecomposition3d(transMat);
         svd.pseudoInverse(invTransMat);
      }
      invRotMat.set(rotMat);
      if (!invRotMat.invert()) {
         SVDecomposition3d svd = new SVDecomposition3d(rotMat);
         svd.pseudoInverse(invRotMat);
      }
      
   }

   @Override
   public int numBilaterals() {
      return bilaterals;
   }

   @Override
   public int numUnilaterals() {
      return unilaterals;
   }

   @Override
   public void initializeConstraints() {

      if (bilaterals == 0 && unilaterals == 0) {
         // XXX called in the RigidBodyCoupling constructor. Nothing to do
         // yet. Will be callled again in OpenSimCustomCoupling constructor
         return;
      }
      int[] flags = new int[6];
      Wrench[] wrenches = new Wrench[6];

      this.order = new int[6];          // order places bilaterals first
      int biIdx = 0;
      int uniIdx = numBilaterals();     // start after bilaterals
      
      for (int i=0; i<3; i++) {
         // translation axes
         if (transAxes[i].isFixed()) {
            int j = biIdx++;
            this.order[i] = j;
            flags[j] = (BILATERAL | LINEAR);
            wrenches[j] = new Wrench();
            wrenches[j].f.set(transAxes[i].getAxis());
            wrenches[j].m.setZero();
         } else if (transAxes[i].isRestricted()){
            int j = uniIdx++;
            this.order[i] = j;
            flags[j] = LINEAR;
         } else {
            this.order[i] = -1;
         }
      }
      
      for (int i=0; i<3; i++) {
         // rotation axes
         if (rotAxes[i].isFixed()) {
            int j = biIdx++;
            this.order[3+i] = j;
            flags[j] = (BILATERAL | ROTARY);
            wrenches[j] = new Wrench();
            wrenches[j].f.setZero();
            wrenches[j].m.set(rotAxes[i].getAxis()); // current axes, assuming no initial rotationsm.setZero();
         } else if (rotAxes[i].isRestricted()){
            int j = uniIdx++;
            this.order[3+i] = j;
            flags[j] = ROTARY;
         } else {
            this.order[3+i] = -1;
         }
      }
      for (int i=0; i<biIdx+uniIdx; i++) {
         if (wrenches[i] != null) {
            addConstraint (flags[i], wrenches[i]);
         }
         else {
            addConstraint (flags[i]);
         }
      }
      
   }
   
   @Override
   public void updateConstraints(
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {

      // translations (applied to C?)
      // compute coordinates
      Vector3d t = new Vector3d();
      invTransMat.mul(t, TCD.p);
      
      for (int i=0; i<3; i++) {
         transAxes[i].updateCoords(t.get(i));
         int j = order[i];
         if (j >= 0) {
            RigidBodyConstraint cinfo = getConstraint(j);
            //cinfo.coordinate = t.get(i); // set coordinate translation
            // transform to frame C
            Vector3d f = new Vector3d();
            f.mulTranspose(TCD.R, transAxes[i].getAxis ());
            cinfo.setWrenchG (f, Vector3d.ZERO);
            //cinfo.wrenchC.f.mulTranspose(TCD.R, transAxes[i].getAxis ());
            //cinfo.wrenchC.m.setZero();
            //cinfo.distance = cinfo.wrenchC.dot(errC);
            //cinfo.dotWrenchC.setZero();
            
            if (transAxes[i].isRestricted()) {
               
               // maybe set engaged
               double val = transAxes[i].evaluate ();
               double lval = transAxes[i].getLowerBound();
               double uval = transAxes[i].getUpperBound();
               if (updateEngaged) {
                  for (Coord coord : transAxes[i].coords) {
                     maybeSetEngaged(cinfo, coord.val, coord.getRange().getLowerBound(), coord.getRange().getUpperBound());
                  }
               }
               if (cinfo.getEngaged() == 1) {
                  // distance between evaluated locations along translation axis
                  cinfo.setDistance (val-lval);
               } else if (cinfo.getEngaged() == -1) {
                  cinfo.setDistance (uval-val);
               }
            }
         }
      }
      
      // rotations
      Matrix3d R = new Matrix3d(TCD.R);
      R.mul(rotMat);
      R.mul(invRotMat, R);  
      double[] rpy = new double[3];
      double[] ref = new double[3];
      for (int i=0; i<3; i++) {
         ref[i] = rotAxes[i].evaluate();
      }
      getRPY(R, rpy);
      // Resolve any ambiguity, keeping angles closest to previous
      EulerFilter.filter(ref, rpy, 1e-5, rpy);
      
      // System.out.println("RPY: " + rpy[0] + " " + rpy[1] + " " + rpy[2]);
      
      RotationMatrix3d Rr = new RotationMatrix3d();
      RotationMatrix3d Rp = new RotationMatrix3d();
      
      // applied to frame C, order of rotations is reversed?
      Matrix3d Dypr = new Matrix3d();
      
      // intrinsically, rotate about rotAxis[0] then transformed [1] then transformed [2]?
      Dypr.setColumn(0, rotAxes[0].getAxis());
      Rr.setAxisAngle(rotAxes[0].getAxis(), rpy[0]);

      // roll rotAxes[1]
      Vector3d a = new Vector3d();
      Rr.mul(a, rotAxes[1].getAxis());
      Dypr.setColumn(1, a);
      Rp.setAxisAngle(rotAxes[1].getAxis (), rpy[1]);  // t is rotated axis 1, new pivot for pitch
      
      // roll and pitch rotAxes[2]
      Rp.mul(a, rotAxes[2].getAxis ());      
      Rr.mul(a, a);
      Dypr.setColumn(2, a);
      
      // Dypr has 3 axes relative to D, let's make them relative to C
      Dypr.mulTransposeLeft(TCD.R, Dypr); 
      
      // XXX potential singularity!! 
      boolean success = Dypr.invert();
      if (!success) {
         // need to rebuild, since previous invert attempt modified matrix
         Dypr.setColumn(0, rotAxes[0].getAxis());
         Rr.setAxisAngle(rotAxes[0].getAxis(), rpy[0]);
         Rr.mul(a, rotAxes[1].getAxis());
         Dypr.setColumn(1, a);
         Rp.setAxisAngle(rotAxes[1].getAxis(), rpy[1]);
         Rp.mul(a, rotAxes[2].getAxis());      
         Rr.mul(a, a);
         Dypr.setColumn(2, a);
         Dypr.mulTransposeLeft(TCD.R, Dypr); 
         
         SVDecomposition3d svd = new SVDecomposition3d(Dypr);
         svd.pseudoInverse(Dypr);
      }
      // columns should now give us wrenches
      for (int i=0; i<3; i++) {
         rotAxes[i].updateCoords(rpy[i]);
         
         int j = order[3+i];
         if (j >= 0) {
            RigidBodyConstraint cinfo = getConstraint(j);            
            //cinfo.coordinate = rpy[i]; // set coordinate angle
            //cinfo.wrenchC.f.setZero();
            //cinfo.dotWrenchC.setZero();
            //cinfo.distance = cinfo.wrenchC.dot(errC);
            //Dypr.getRow(i, cinfo.wrenchC.m);
            Vector3d m = new Vector3d();
            Dypr.getRow(i, m);
            cinfo.setWrenchG (Vector3d.ZERO, m);
            if (rotAxes[i].isRestricted()) {
               // maybe set engaged
               if (updateEngaged) {
                  for (Coord coord : rotAxes[i].coords) {
                     maybeSetEngaged(cinfo, coord.val, coord.getRange().getLowerBound(), coord.getRange().getUpperBound());
                  }
               }
               
               double val = rotAxes[i].evaluate ();
               double lval = rotAxes[i].getLowerBound();
               double uval = rotAxes[i].getUpperBound();

               if (cinfo.getEngaged() == 1) {
                  // distance between evaluated locations along translation axis
                  cinfo.setDistance (val-lval);
               } else if (cinfo.getEngaged() == -1) {
                  cinfo.setDistance (uval-val);
               }
            }
         }
      }
      
      
            
   }
   
   /**
    * Gets the z-y-x (intrinsic) roll-pitch-yaw angles corresponding to a given rotation.
    * @param M
    * Matrix to compute rotations
    * @param rpy
    * returns the angles (roll, pitch, yaw, in that order) in radians.
    */
   public void getRPY (Matrix3dBase M, double[] rpy) {
      double sroll, croll, nx, ny, p;
         
      double EPSILON = 2.220446049250313e-15;
      nx = M.m00;
      ny = M.m10;
      if (Math.abs (nx) < EPSILON && Math.abs (ny) < EPSILON) {
         rpy[0] = 0.;
         rpy[1] = Math.atan2 (-M.m20, nx);
         rpy[2] = Math.atan2 (-M.m12, M.m11);
      }
      else {
         rpy[0] = (p = Math.atan2 (ny, nx));
         sroll = Math.sin (p);
         croll = Math.cos (p);
         rpy[1] = Math.atan2 (-M.m20, croll * nx + sroll * ny);
         rpy[2] =
            Math.atan2 (sroll * M.m02 - croll * M.m12, croll * M.m11 - sroll * M.m01);
      }
   }
   
   /**
    * Sets this rotation to one produced by intrinsic roll-pitch-yaw angles (z-y-x). The
    * coordinate frame corresponding to these angles is produced by a rotation
    * of roll about the z axis, followed by a rotation of pitch about the new y
    * axis, and finally a rotation of yaw about the new x axis.
    * 
    * @param M
    * Matrix to fill
    * @param rpy
    * roll, pitch, yaw, in order
    */
   public void setRPY (Matrix3dBase M, double[] rpy) {
      double sroll, spitch, syaw, croll, cpitch, cyaw;

      double roll = rpy[0];
      double pitch = rpy[1];
      double yaw = rpy[2];
      sroll = Math.sin (roll);
      croll = Math.cos (roll);
      spitch = Math.sin (pitch);
      cpitch = Math.cos (pitch);
      syaw = Math.sin (yaw);
      cyaw = Math.cos (yaw);

      M.m00 = croll * cpitch;
      M.m10 = sroll * cpitch;
      M.m20 = -spitch;

      M.m01 = croll * spitch * syaw - sroll * cyaw;
      M.m11 = sroll * spitch * syaw + croll * cyaw;
      M.m21 = cpitch * syaw;

      M.m02 = croll * spitch * cyaw + sroll * syaw;
      M.m12 = sroll * spitch * cyaw - croll * syaw;
      M.m22 = cpitch * cyaw;
   }
   
   @Override
   public void projectToConstraints(
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {
      TGD.set(TCD);
      
      // assuming translation relative to D, not affected by rotations
      Vector3d t = new Vector3d();
      // compute translation w.r.t. supplied axes 
      invTransMat.mul(t, TGD.p);
      for (int i=0; i<3; i++) {
         if (transAxes[i].isFixed()) {
            // force to fixed value
            t.set(i, transAxes[i].evaluate());
         } else if (transAxes[i].isRestricted ()) {
            // project coordinates
            transAxes[i].projectCoordinates();
            t.set(i, transAxes[i].evaluate ());
         }
      }
      transMat.mul(TGD.p, t); // transform back to spatial
      
      // Compute euler angles, by first changing basis
      // and aligning axes s.t.
      // [rot(2) rot(1) rot(0)]
      Matrix3d R = new Matrix3d(TGD.R);
      R.mul(rotMat);
      R.mul(invRotMat, R);  
      double[] rot = new double[3];
      double[] ref = new double[3];
      for (int i=0; i<3; i++) {
         ref[i] = rotAxes[i].evaluate();
      }
      getRPY(R, rot);
      EulerFilter.filter(ref, rot, 1e-5, rot);
      boolean changed = false;
      for (int i=0; i<3; i++) {
         if (rotAxes[i].isFixed()) {
            rot[i] = rotAxes[i].evaluate();
            changed = true;
         } else if (rotAxes[i].isRestricted ()) {
            rotAxes[i].projectCoordinates();
            rot[i] = rotAxes[i].evaluate ();
            changed = true;
         }
      }
      
      if (changed) {
         // set angles and transform back
         setRPY(R, rot);
         R.mul(invRotMat);
         R.mul(rotMat, R);
         TGD.R.set(R);
      }
      
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {
      TCD.setIdentity();
   }

}
