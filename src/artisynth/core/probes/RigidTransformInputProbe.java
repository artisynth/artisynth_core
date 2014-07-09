/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import artisynth.core.mechmodels.RigidBody;
import maspack.interpolation.Interpolation;
import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.matrix.Matrix3d;
import maspack.matrix.Quaternion;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

/**
 * @author Andrew K Ho
 * @since 2013-07-16
 */
public class RigidTransformInputProbe extends InputProbe {
   protected NumericList myTransAndQuaternParams;
   protected final int myVectorSize = 7; 
   protected Interpolation myInterpolation;
   protected RigidBody myRigid;
   protected VectorNd myTmpVec;

   protected boolean myExtendEnd = false; // Don't interpolate past end point

   /**
    * Default constructor.
    */
   public RigidTransformInputProbe () {
      super (null);
      myRigid = null;
   }

   /**
    * Creates a RigidTransformInputProbe for a specified RigidBody
    *
    * @param rigid rigid body for this probe
    */
   public RigidTransformInputProbe (RigidBody rigid) {
      this ();
      setRigid (rigid);
   }

   /**
    * Set the rigid body associated with this probe.
    *
    * This must only be called once in the lifetime of the probe. Reseting the
    * rigid body is not supported at the moment.  Create a new
    * RigidTransformInputProbe instead.
    *
    * @param rigid rigid body for this probe
    */
   public void setRigid (RigidBody rigid) {
      if (rigid != null) {
         throw new UnsupportedOperationException (
            "A rigid body has already been set! Aborting");
      }

      myRigid = rigid;

      /*
       * setup default interpolator
       */
      myTransAndQuaternParams = new NumericList (myVectorSize);
      myInterpolation =
         new Interpolation (Interpolation.Order.Linear, myExtendEnd);

      myTransAndQuaternParams.setInterpolation (myInterpolation);

      myTmpVec = new VectorNd (myVectorSize);

      setStartTime (0.);
      setStopTime (0.);
      setUpdateInterval (-1);
      setActive (true);
   }

   @Override
   public void initialize (double t) {
      apply (t);
   }

   /**
    * Set the interpolation order of the interpolator.
    *
    * @param interpOrder Enum value. Can be Linear, Cubic, Step, etc. 
    * See maspack.interpolation.Interpolation
    */
   
   public void setInterpolation (Interpolation.Order interpOrder) {
      myInterpolation.setOrder (interpOrder);
   }

   /**
    * Set your own interpolation method
    *
    * @param interp An initialized instance of Interpolation
    */
   public void setInterpolation (Interpolation interp) {
      myInterpolation = interp;
      myTransAndQuaternParams.setInterpolation (myInterpolation);
   }

   public void addTransform (double t, RigidTransform3d tx) {
      VectorNd posVector = new VectorNd ();

      Vector3d offset = tx.getOffset ();
      posVector.append (offset.x);
      posVector.append (offset.y);
      posVector.append (offset.z);

      RotationMatrix3d rot = new RotationMatrix3d();
      Vector3d scale = new Vector3d();
      Matrix3d shear = new Matrix3d();
      
      tx.getMatrixComponents (rot, scale, shear);

      Quaternion q = new Quaternion(rot.getAxisAngle ());
      posVector.append (q.s);
      posVector.append (q.u.x);
      posVector.append (q.u.y);
      posVector.append (q.u.z);

      NumericListKnot knot = new NumericListKnot (myVectorSize);
      knot.t = t;
      knot.v.set (posVector);
      myTransAndQuaternParams.add (knot);

      if (t < getStartTime()) {
         setStartTime (t);
      }
      if (t > getStopTime ()) {
         setStopTime (t);
      }
   }

   /**
    * Apply this probe at time t to model myModel.
    *
    * @param t time to interpolate vertex positions to.
    */
   public void apply (double t) {
      if (myRigid == null) {
         return;
      }

      double tloc = (t-getStartTime()) / myScale;
      myTransAndQuaternParams.interpolate (myTmpVec, tloc);

      RigidTransform3d tx = new RigidTransform3d ();

      Vector3d offset = new Vector3d ();
      offset.x = myTmpVec.get (0);
      offset.y = myTmpVec.get (1);
      offset.z = myTmpVec.get (2);
      tx.setTranslation (offset);

      Quaternion q = new Quaternion();
      q.s = myTmpVec.get (3);
      q.u.x = myTmpVec.get (4);
      q.u.y = myTmpVec.get (5);
      q.u.z = myTmpVec.get (6);

      RotationMatrix3d rot = new RotationMatrix3d (q);

      RigidTransform3d rigidTx = new RigidTransform3d ();
      rigidTx.setTranslation (offset);
      rigidTx.setRotation (rot);

      myRigid.setPose (rigidTx);
   }
}
