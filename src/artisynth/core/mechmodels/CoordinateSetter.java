package artisynth.core.mechmodels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import artisynth.core.mechmodels.KinematicTree.JointNode;
import artisynth.core.mechmodels.KinematicTree.BodyNode;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.VectorNd;
import maspack.spatialmotion.RigidBodyConstraint.MotionType;
import maspack.util.FunctionTimer;
import maspack.util.InternalErrorException;

/**
 * Sets coordinate values for joints within a MechModel, while updating
 * adjacent bodies and respecting all kinematic constraints. More specifically,
 * this class:
 *
 * <ol>
 *
 * <li>Allows an arbitrary set of coordinates to be set across different
 * joints.
 * 
 * <li>Updates the poses of adjacent bodies in the joints' kinematic strucure.
 *
 * <li>Ensures the correct computation of coordinate values in situations where
 * they are coupled due to kinematic lopps and constraints.
 *
 * <li>Updates muscle wrap paths.
 *
 * </ol>
 *
 * Internally, coordinates are set in a series of substeps to ensure that wrap
 * path integrity is maintained.
 */
public class CoordinateSetter {

   private static final double RTOD = 180/Math.PI;
   private static final double DTOR = Math.PI/180;
   private static final double INF = Double.POSITIVE_INFINITY;

   public boolean debug; // debug flag- internal use only 

   protected MechModel myMech;  // model containing coordinates
   // MultiPointSprings that should be updated by the solver. If null, then all
   // springs in the MechModel are updated.
   ArrayList<MultiPointSpring> myMultiSprings;
   int myTotalIterations;  // cummulation iteration count for loop solves

   CoordinateSolver mySolver; // solves for coupled coordinates

   // Pending requests from each coordinate, stored as a map from the
   // coordinate's handle to its desired value.
   HashMap<JointCoordinateHandle,Double> myRequests = new LinkedHashMap<>();

   // Keep a map from joints to their corresponding joint nodes, which are are
   // created on demand and which belong to a KinematicTree describing the
   // articulated structure to which the joint belongs. The map is cleared
   // whenever the structure of the MechModel changes.
   HashMap<JointBase,JointNode> myJointNodeMap = new LinkedHashMap<>();
   int mySystemVersion = -1;  // MechModel system version

   /**
    * Return status for set requests undertaken by the coordinate setter.
    */
   public static class SetStatus {

      static final int UNKNOWN = 0;
      static final int FREE = 1;
      static final int DEPENDENT = 2;
      static final int LIMITED = 3;

      LinkedHashMap<JointCoordinateHandle,Integer>
         myStatusMap = new LinkedHashMap<>();

      boolean myConverged;
      boolean myFinished; // flag used by inner loop to indicate solve is done
      int myNumIters;

      SetStatus (JointCoordinateHandle handle, boolean converged) {
         this(converged);
         myStatusMap.put (handle, FREE);
      }

      SetStatus (boolean converged) {
         //myIndepCoords = new ArrayList<>();
         myConverged = converged;
         myNumIters = 0;
      }

      void addRequests (Collection<JointCoordinateHandle> handles) {
         for (JointCoordinateHandle handle : handles) {
            myStatusMap.put (handle, FREE);
         }
      }

      void addRequests (List<JointCoordRequest> reqs) {
         for (JointCoordRequest req : reqs) {
            myStatusMap.put (req.myHandle, FREE);
         }
      }

      /**
       * Total number of interations required by the loop solver. If none of
       * the set coordinates are embedded in a kinematic loop, the loop solver
       * will not be needed and this metmyJointhod will return 0.
       *
       * @return loop solver iteration count
       */
      public int numIterations() {
         return myNumIters;
      }

      void addIterations (int num) {
         myNumIters += num;
      }

      /**
       * Returns {@code true} if the loop solver converged, or if none of the
       * requested coordinates are embedded in a kinematic loop and all
       * coordinates could therefore be set freely.
       *
       * @return {@code true} if the loop solver converged or was not needed
       */
      public boolean converged() {
         return myConverged;
      }

      void setConverged (boolean converged) {
         myConverged = converged;
      }

      /**
       * Returns the number of coordinates that were requested to be set.
       *
       * @return number of coordinate requested to be set
       */
      public int numCoordinates() {
         return myStatusMap.size();
      }

      /**
       * Returns the coordinates that were requested to be set, as a list of
       * {@link JointCoordinateHandle}s.
       *
       * @return list of coordinates requested to be set
       */
      public List<JointCoordinateHandle> getCoordinates() {
         ArrayList<JointCoordinateHandle> list = new ArrayList<>();
         list.addAll (myStatusMap.keySet());
         return list;
      }

      /**
       * Returns the number of requested coordinates that were determined to be
       * dependent. Dependent coordinates will only occur if some of the
       * requested coordinates are embedded in a kinematic loop.
       *
       * @return number of dependent coordinates
       */
      public int numDependent() {
         int num = 0;
         for (Integer status : myStatusMap.values()) {
            if (status == DEPENDENT) {
               num++;
            }
         }
         return num;
      }

      /**
       * Queries whether a specific requested coordinate was determined to be
       * dependent. A coordinate <i>may</i> be dependent only if it is embedded
       * in a kinematic loop. The method returns {@code false} if the
       * coordinate is not one of the requested coordinates.
       *
       * <p>Dependent coordinates will not generally be located near their
       * requested values, since this kinematic system does not have sufficient
       * degrees of freedom to provide this.
       *
       * @param handle handle to the coordinate
       * @return {@code true} if the coordinate was one of the requested
       * coordinates and was determined to be dependent
       */
      public boolean isDependent (JointCoordinateHandle handle) {
         Integer status = myStatusMap.get (handle);
         return status != null && status == DEPENDENT;
      }

      /**
       * Returns the number of requested coordinates whose setting was
       * restricted because of coordinate limits. This is the total number of
       * coordinates for which {@link #isLimited(JointCoordinateHandle)}
       * returns {@code true}.
       *
       * @return number of requested coordinates restricted by coordinate
       * limits
       */
      public int numLimited() {
         int num = 0;
         for (Integer status : myStatusMap.values()) {
            if (status == LIMITED) {
               num++;
            }
         }
         return num;
      }

      /**
       * Queries whether setting a specific requested coordinate was restricted
       * because of coordinate limits. This may only occur for independent
       * coordinates which are embedded within a kinematic loop and for which
       * attempting to set the requested value caused one or more
       * <i>dependent</i> coordinates to hit a limit. The method returns {@code
       * false} if the coordinate is not one of the requested coordinates.
       *
       * <p>Even if {@link #converged} returns {@code true}, coordinates that
       * are limit restricted will not generally be located near their
       * requested values.
       *
       * @param handle handle to the coordinate
       * @return {@code true} if the coordinate was one of the requested
       * coordinates, and setting it was restricted because of dependent
       * coordinate limits
       */
      public boolean isLimited (JointCoordinateHandle handle) {
         Integer status = myStatusMap.get (handle);
         return status != null && status == LIMITED;
      }

      /**
       * Returns the number of requested coordinates that were determined to be
       * free. This is the total number of coordinates for which {@link
       * #isFree(JointCoordinateHandle)} returns {@code true}.
       *
       * @return number of free coordinates
       */
      public int numFree() {
         int num = 0;
         for (Integer status : myStatusMap.values()) {
            if (status == FREE) {
               num++;
            }
         }
         return num;
      }

      /**
       * Queries whether a specific requested coordinate was determined
       * to be free. A coordinate is free if it is independent and
       * setting it to its requested value did not cause a
       * dependent coordinate to hit a joint limit, i.e.,
       * if {@link #isDependent(JointCoordinateHandle)} and {@link
       * #isLimited(JointCoordinateHandle)} both return {@code false}.
       *
       * <p>If {@link #converged} returns true, then free coordinates should be
       * located at (or very near to) their requested values.
       *
       * @param handle handle to the coordinate
       * @return {@code true} if the coordinate was one of the requested
       * coordinates and was determined to be free
       */
      public boolean isFree (JointCoordinateHandle handle) {
         Integer status = myStatusMap.get (handle);
         return status != null && status == FREE;
      }

      /**
       * Returns a short string the partially describes this status.
       */
      public String toString() {
         String str;
         if (myConverged) {
            str = "CONVERGED";
         }
         else {
            str = "NO CONVERGENCE";
         }
         if (numLimited() > 0) {
            str += ", LIMITED=" + numLimited();
         }
         return str;
      }

      void updateRequestStatus (SetStatus status) {
         for (Map.Entry<JointCoordinateHandle,Integer> entry : 
                 status.myStatusMap.entrySet()) {
            JointCoordinateHandle handle = entry.getKey();
            if (myStatusMap.get (handle) == null) {
               throw new InternalErrorException (
                  "Substatus coordinate "+handle.getName()+" not found");
            }
            myStatusMap.put (handle, entry.getValue());
         }
      }
   };

   /**
    * Describes a set request for a specific joint coordinate.
    */
   static class JointCoordRequest {
      JointCoordinateHandle myHandle;

      double myRequestedValue; // requested value of the coordinate
      double myInitialValue;   // initial value of the coordinate
      // For cases where coordinate values are coupled, we use a
      // CoordinateSolver to solve for all the coupled values at once.  This
      // solve process will involve locked and unlocked certain coordinate
      // values.
      int mySolveIdx;          // coord solve index within the CoordinateSolver
      int mySaveLockedSetting = -1;  // previous lock setting for locked coord
      double mySaveLockedValue = -1; // previous lock value for locked coord

      JointCoordRequest (JointCoordinateHandle jch, double value) {
         myHandle = new JointCoordinateHandle (jch);
         myRequestedValue = myHandle.clipToRange(value);
      }

      /**
       * Return the coordinate's joint.
       */
      JointBase getJoint() {
         return myHandle.getJoint();
      }

      /**
       * Interpolate a target value between the coordinate's initial value and
       * its requested value.
       */
      double computeTargetValue (double s) {
         return (1-s)*myInitialValue + s*myRequestedValue;
      }

      /**
       * Lock a coordinate, saving its previous lock status and value if this
       * has not been previously done.
       */
      void lockCoordinate (double value) {
         if (!myHandle.isLocked()) {
            if (mySaveLockedSetting == -1) {
               mySaveLockedSetting = 0;
               mySaveLockedValue = myHandle.getLockedValue();
            }
            myHandle.setLocked (true);
            myHandle.setLockedValue (value);
         }
      }

      /**
       * Unlock a coordinate, saving its previous lock status and value if this
       * has not been previously done.
       */
      void unlockCoordinate () {
         if (myHandle.isLocked()) {
            if (mySaveLockedSetting == -1) {
               mySaveLockedSetting = 1;
               mySaveLockedValue = myHandle.getLockedValue();
            }
            myHandle.setLocked (false);
         }
      }

      /**
       * Restore the previous lock status and value of the coordinate.
       */
      void restoreLockSetting () {
         if (mySaveLockedSetting != -1) {
            myHandle.setLocked (mySaveLockedSetting == 1 ? true : false);
            myHandle.setLockedValue (mySaveLockedValue);
         }
      }

      /**
       * Compute the minimum number of steps that should be taken in driving
       * the coordinate from its initial value to its requested value.  For
       * rotary coordinates, the each step should be around 15 degrees, while
       * for other coordinates, it should be either 0.2 of the coordinate
       * range, or if the range is unbounded, about 0.1 of the model size.
       */
      int computeNumSteps (double modelSize) {
         myInitialValue = myHandle.getValue();
         double valueChange = Math.abs (myRequestedValue-myInitialValue);
         if (myHandle.getMotionType() == MotionType.ROTARY) {
            // each step should be about 15 degrees
            return (int)Math.ceil(Math.abs(valueChange)/(DTOR*15));
         }
         else {
            double range = myHandle.getValueRange().getRange();
            if (range != INF) {
               // each step should be about 0.2 range distance
               return (int)Math.ceil(5*Math.abs(valueChange)/range);
            }
            else {
               // each step should be about 0.1 model size
               return (int)Math.ceil(10*Math.abs(valueChange)/modelSize);
            }
         }
      }
      
      /**
       * Returns the requested coordinate value, in degrees for rotary
       * coordinates.
       */
      double requestedValueDeg() {
         double val = myRequestedValue;
         if (myHandle.getMotionType() == MotionType.ROTARY) {
            val *= RTOD;
         }
         return val;
      }
   }

   /**
    * Requests that a specific coordinate by set to a given value.  The set
    * operation will be deferred until the next call to {@link
    * #setCoordinates}, which will then attempt to resolve all pending requests
    * at once.
    *
    * @param joint joint containing the coordinate
    * @param idx coordinate index with respect to the joint
    * @param value value to set the coordinate to. Should be given
    * in radians for rotary coordinates.
    */
   public void request (JointBase joint, int idx, double value) {
      myRequests.put (new JointCoordinateHandle (joint, idx), value);
   }

   /**
    * Requests that a specific coordinate by set to a given value.  The set
    * operation will be deferred until the next call to {@link
    * #setCoordinates}, which will then attempt to resolve all pending requests
    * at once.
    *
    * @param handle handle describing the coordinate
    * @param value value to set the coordinate to. Should be given
    * in radians for rotary coordinates.
    */
   public void request (JointCoordinateHandle handle, double value) {
      myRequests.put (new JointCoordinateHandle (handle), value);
   }

   /**
    * Requests that a specific coordinate by set to a given value.  The set
    * operation will be deferred until the next call to {@link
    * #setCoordinates}, which will then attempt to resolve all pending requests
    * at once.
    *
    * @param joint joint containing the coordinate
    * @param idx coordinate index with respect to the joint
    * @param value value to set the coordinate to. Should be given
    * in degrees for rotary coordinates.
    */
   public void requestDeg (JointBase joint, int idx, double value) {
      JointCoordinateHandle handle = new JointCoordinateHandle (joint, idx);
      if (handle.getMotionType() == MotionType.ROTARY) {
         value *= DTOR;
      }
      myRequests.put (handle, value);
   }

   /**
    * Requests that a specific coordinate by set to a given value.  The set
    * operation will be deferred until the next call to {@link
    * #setCoordinates}, which will then attempt to resolve all pending requests
    * at once.
    *
    * @param handle handle describing the coordinate
    * @param value value to set the coordinate to. Should be given
    * in degrees for rotary coordinates.
    */
   public void requestDeg (JointCoordinateHandle handle, double value) {
      if (handle.getMotionType() == MotionType.ROTARY) {
         value *= DTOR;
      }
      myRequests.put (new JointCoordinateHandle (handle), value);
   }

   /**
    * Clears any pending coordinate set requests, so that if {@link
    * #setCoordinates}, is called immediately after, no actions will be taken.
    */
   public void clearRequests() {
      myRequests.clear();
   }

   /**
    * Sets a specific coordinate to a specific value, while attempting to keep
    * all other coordinate values fixed. This will generally not be possible if
    * the coordinate is embedded within a kinematic loop. A returned status
    * value gives information about how well the operation succeeded.
    *
    * @param handle handle describing the coordinate
    * @param value value to set the coordinate to. Should be given
    * in degrees for rotary coordinates.
    * @return status information about how well the operation succeeded
    */
   public SetStatus setCoordinateDeg (
      JointCoordinateHandle handle, double value) {
      if (handle.getMotionType() == MotionType.ROTARY) {
         value *= DTOR;
      }
      return setCoordinate (handle, value);
   }         

   /**
    * Sets a specific coordinate to a specific value, while attempting to keep
    * all other coordinate values fixed. This will generally not be possible if
    * the coordinate is embedded within a kinematic loop. A returned status
    * value gives information about how well the operation succeeded.
    *
    * @param joint joint containing the coordinate
    * @param idx coordinate index with respect to the joint
    * @param value value to set the coordinate to. Should be given
    * in degrees for rotary coordinates.
    * @return status information about how well the operation succeeded
    */
   public SetStatus setCoordinateDeg (
      JointBase joint, int idx, double value) {
      return setCoordinateDeg (new JointCoordinateHandle (joint, idx), value);
   }         

   /**
    * Return the cummulative number of solve iterations for coordinates
    * associated with kinematic loops.
    */
   public int numIterations() {
      return myTotalIterations;
   }

   /**
    * Clears the cummulative solve and iteration counts.
    */
   public void clearSolveCounts() {
      myTotalIterations = 0;
   }

   /**
    * Sets a specific coordinate to a specific value, while attempting to keep
    * all other coordinate values fixed. This will generally not be possible if
    * the coordinate is embedded within a kinematic loop. A returned status
    * value gives information about how well the operation succeeded.
    *
    * @param joint joint containing the coordinate
    * @param idx coordinate index with respect to the joint
    * @param value value to set the coordinate to. Should be given
    * in radian for rotary coordinates.
    * @return status information about how well the operation succeeded
    */
   public SetStatus setCoordinate (JointBase joint, int idx, double value) {
      return setCoordinate (new JointCoordinateHandle (joint, idx), value);
   }
   
   /**
    * Sets a specific coordinate to a specific value, while attempting to keep
    * all other coordinate values fixed. This will generally not be possible if
    * the coordinate is embedded within a kinematic loop. A returned status
    * value gives information about how well the operation succeeded.
    *
    * @param handle handle describing the coordinate
    * @param value value to set the coordinate to. Should be given
    * in radian for rotary coordinates.
    * @return status information about how well the operation succeeded
    */
   public SetStatus setCoordinate (JointCoordinateHandle handle, double value) {
      ArrayList<JointCoordRequest> reqs = new ArrayList<>(1);
      reqs.add (new JointCoordRequest (handle, value));
      JointBase joint = handle.getJoint();
      JointNode jnode = getJointNode (joint);
      if (jnode.isSimple()) {
         int nsteps = computeNumSteps (reqs);
         for (int i=1; i<=nsteps; i++) {
            double s = i/(double)nsteps;
            RigidTransform3d T = computeSimpleNodeTransform (
               jnode, null, s, reqs);
            jnode.updateDistalBodiesAndConnectors (T);
            if (jnode.numChildren() > 1) {
               throw new InternalErrorException (
                  "simple joint node has "+jnode.numChildren()+" children");
            }
            else if (jnode.numChildren() == 1) {
               recursivelySetSimpleCoordinates (
                  jnode.getChildren().get(0), T, s, null);
            }
            updateWrapPaths();
         }
         return new SetStatus(handle, /*converged*/true);
      }
      else {
         return solveForCoupledCoords (jnode, reqs);
      }
   }

   /**
    * Attempts to satisfy all coordinate set requests that have been placed by
    * the {@code request} methods, while keeping all other coordinate values
    * fixed. This will generally not be possible if any of the requested
    * coordinates are embedded within a kinematic loop. A returned status value
    * gives information about how the operation fared.
    *
    * @return status information about how well the operation succeeded
    */
   public SetStatus setCoordinates() {
      SetStatus status = new SetStatus(/*converged*/true);
      status.addRequests (myRequests.keySet());
      if (myRequests.size() > 0) {
         HashMap<JointNode,ArrayList<JointCoordRequest>> simpleReqs =
            new LinkedHashMap<>();
         HashSet<BodyNode> simpleRoots = new LinkedHashSet<>();
         HashMap<JointNode,ArrayList<JointCoordRequest>> coupledReqs =
            new LinkedHashMap<>();

         // divide requests into coupled requests, which involve joints in a
         // kinematic loop and require an iterative solution, and simple
         // requests, for which the coordinates can be set directly and
         // resolved with a single pass through the kinematic tree.
         for (Map.Entry<JointCoordinateHandle,Double> request :
                 myRequests.entrySet()) {
            JointCoordinateHandle handle = request.getKey();
            JointBase joint = handle.getJoint();
            JointNode jnode = getJointNode (joint);
            if (jnode.isSimple()) {
               ArrayList<JointCoordRequest> reqs = simpleReqs.get (jnode);
               if (reqs == null) {
                  reqs = new ArrayList<JointCoordRequest>(6);
                  simpleReqs.put (jnode, reqs);
                  simpleRoots.add (jnode.getRoot());
               }
               reqs.add (new JointCoordRequest (handle, request.getValue()));
            }
            else {
               ArrayList<JointCoordRequest> reqs = coupledReqs.get (jnode);
               if (reqs == null) {
                  reqs = new ArrayList<JointCoordRequest>();
                  coupledReqs.put (jnode, reqs);
               }
               reqs.add (new JointCoordRequest (handle, request.getValue()));
            }
         }
         if (simpleReqs.size() > 0) {
            int nsteps = 1;
            for (ArrayList<JointCoordRequest> reqs : simpleReqs.values()) {
               nsteps = Math.max (nsteps, computeNumSteps (reqs));
            }
            for (int i=1; i<=nsteps; i++) {
               double s = i/(double)nsteps;
               for (BodyNode root : simpleRoots) {
                  RigidTransform3d T = new RigidTransform3d();
                  recursivelySetSimpleCoordinates (root, T, s, simpleReqs);
               }
               updateWrapPaths();
            }
         }
         if (coupledReqs.size() > 0) {
            for (Map.Entry<JointNode,ArrayList<JointCoordRequest>> entry :
                    coupledReqs.entrySet()) {
               SetStatus substatus = 
                  solveForCoupledCoords (entry.getKey(), entry.getValue());
               if (!substatus.converged()) {
                  status.setConverged (false);
               }
               status.addIterations (substatus.numIterations());
               status.updateRequestStatus (substatus);
            }
         }
         myRequests.clear();
      }
      return status;
   }

   /**
    * Creates a new CoordinateSetter for setting joint coordinates
    * within the specified MechModel.
    *
    * @param mech MechModel in which the coordinates are located
    */
   public CoordinateSetter (MechModel mech) {
      this (mech, null);
   }

   /**
    * Creates a new CoordinateSetter for setting joint coordinates within the
    * specified MechModel. An optional {@code springs} argument specifies
    * specific spring components that should be updated as coordinates are set.
    * Otherwise, all springs in the MechModel will be updated.
    *
    * @param mech MechModel in which the coordinates are located
    * @param springs if non-{@code null}, specifies which springs
    * should be updated
    */
   public CoordinateSetter (
      MechModel mech, Collection<? extends PointSpringBase> springs) {
      mySolver = new CoordinateSolver (mech);
      myMech = mech;
      if (springs != null) {
         myMultiSprings = new ArrayList<>();
         for (PointSpringBase psb : springs) {
            if (psb instanceof MultiPointSpring) {
               myMultiSprings.add ((MultiPointSpring)psb);
            }
         }
      }
   }

   /* ---- implementation methods ---- */

   /**
    * Obtain the joint node for a specified joint. This node will be embedded
    * in a KinematicTree describing the surrounding articulated structure.
    */
   private JointNode getJointNode (JointBase joint) {
      maybeClearJointNodes();
      JointNode jntNode;
      if ((jntNode=myJointNodeMap.get(joint)) == null) {
         KinematicTree tree = 
            KinematicTree.findTree (joint, myMech.getAllConstrainers());
         for (JointNode jnode : tree.getJointNodes()) {
            for (BodyConstrainer bcon : jnode.getConstrainers()) {
               if (bcon instanceof JointBase) {
                  myJointNodeMap.put ((JointBase)bcon, jnode);
               }
            }
         }
         jntNode = myJointNodeMap.get(joint);
      }
      return jntNode;
   }     

   /**
    * Clear joint nodes if the MechModel structure has changed.  Nodes will be
    * recreated on demand.
    */
   private void maybeClearJointNodes() {
      if (myMech.getStructureVersion() != mySystemVersion) {
         mySystemVersion = myMech.getStructureVersion();
         myJointNodeMap.clear();
      }
   }

   /**
    * Computes the minimum number of steps that should be taken to satisfy a
    * set of joint requests.
    */
   private int computeNumSteps (List<JointCoordRequest> requests) {
      int nsteps = 1;
      double modelSize = mySolver.getModelSize();
      for (JointCoordRequest req : requests) {
         nsteps = Math.max (nsteps, req.computeNumSteps(modelSize));
      }
      return nsteps;
   }

   /**
    * Update wrap paths in the model, using either the specified set of
    * MultiPointSprings, or all such springs in the MechModel.
    */
   private void updateWrapPaths() {
      if (myMultiSprings != null) {
         for (MultiPointSpring spr : myMultiSprings) {
            spr.updateWrapSegments();
         }
      }
      else if (myMech != null) {
         myMech.updateWrapSegments();
      }
   }

   /**
    * Return the poses for a collection of bodies as an array.
    */
   private RigidTransform3d[] getBodyPoses (Collection<RigidBody> bodies) {
      RigidTransform3d[] TBWs = new RigidTransform3d[bodies.size()];
      int k = 0;
      for (RigidBody body : bodies) {
         TBWs[k++] = new RigidTransform3d (body.getPose());
      }
      return TBWs;
   }

   /**
    * Determine the rigid transform needed to update the bodies of a simple
    * joint node given coordinate requests involving that node, as well as
    * coordinate changes in other joints proximal to the node. The latter are
    * specified by the argument {@code T}.
    *
    * @param jnode joint node
    * @param T if not {@code null}, describes the transform arising from
    * changes in joints proximal to the {@code jnode}
    * @param s step interpolation factor in the range [0,1] used to
    * interpolate the desired coordinate values
    * @param reqs coordinate requests associated with the joint
    */
   private RigidTransform3d computeSimpleNodeTransform (
      JointNode jnode,
      RigidTransform3d T, double s, List<JointCoordRequest> reqs) {
      
      JointBase joint = (JointBase)jnode.myConstrainers.get(0);

      RigidTransform3d TCW = joint.getCurrentTCW();
      RigidTransform3d TDW = joint.getCurrentTDW();      
            
      VectorNd coords = new VectorNd();
      joint.getCoordinates (coords);
      for (JointCoordRequest req : reqs) {
         if (req.getJoint() != joint) {
            throw new InternalErrorException (
               "coord request joint does not belong to joint node");
         }
         coords.set (req.myHandle.getIndex(), req.computeTargetValue(s));
      }
      joint.setCoordinates (coords, /*updateAdjacent*/false);
      RigidTransform3d TCD = new RigidTransform3d();
      joint.getCoupling().coordinatesToTCD (TCD);

      // Find world transform T to adjust poses of distal nodes
      RigidTransform3d TX = new RigidTransform3d(); 
      if (joint.getBodyA() != jnode.myProximalBodyNode.myBody) {
         // distal bodies include bodyA, so compute TX from change to TCW:
         // TX = TCWnew * inv(TCW) = TDW * TCD * inv(TCW)
         TX.mul (TDW, TCD);
         TX.mulInverseRight (TX, TCW);
      }
      else {
         // distal bodies include bodyB, so compute TX from change to TDW
         // TX = TDWnew * inv(TDW) = TCW * inv(TCD) * inv(TDW)
         TX.mulInverseRight (TCW, TCD);
         TX.mulInverseRight (TX, TDW);
      }
      if (T != null) {
         TX.mul (T, TX);
      }
      return TX;      
   }

   /**
    * Recursively set coordinate requests for simple joints, starting at the
    * body node {@code bnode}, updating body poses and connectors in the
    * process.
    *
    * @param bnode body node where the recursion should begin
    * @param T if non-null, specifies the cumulative transform (in world
    * coordinates) that should be applied to all bodies distal to {@code bnode}
    * due to coordinate changes proximal to it
    * @param s step interpolation factor in the range [0,1] used to
    * interpolate the desired coordinate values
    * @param reqMap map from joint nodes to coordinate requests
    */
   private void recursivelySetSimpleCoordinates (
      BodyNode bnode, RigidTransform3d T, double s,
      HashMap<JointNode,ArrayList<JointCoordRequest>> reqMap) {

      for (JointNode jnode : bnode.getChildren()) {
         ArrayList<JointCoordRequest> reqs;
         if (reqMap != null && (reqs=reqMap.get(jnode)) != null) {
            // Joint node will be simple by construction
            T = computeSimpleNodeTransform (jnode, T, s, reqs);
            jnode.updateDistalBodiesAndConnectors (T);
            if (jnode.numChildren() > 1) {
               throw new InternalErrorException (
                  "simple joint node has "+jnode.numChildren()+" children");
            }
            else if (jnode.numChildren() == 1) {
               recursivelySetSimpleCoordinates (
                  jnode.getChildren().get(0), T, s, reqMap);
            }
         }
         else {
            // no joint requests; just update 
            jnode.updateDistalBodiesAndConnectors (T);
            for (BodyNode child : jnode.getChildren()) {
               recursivelySetSimpleCoordinates (child, T, s, reqMap);
            }
         }
      }
   }

   /**
    * Use a coordinate solver to solve for coupled coordinate values within a
    * joint node.
    */
   private SetStatus solveForCoupledCoords (
      JointNode jnode, ArrayList<JointCoordRequest> reqs) {

      FunctionTimer timer = new FunctionTimer();
      timer.start();
      int numSteps = computeNumSteps (reqs);

      ArrayList<RigidBody> bodies = jnode.getRigidBodies();
      mySolver.initialize (reqs, bodies, jnode.getConstrainers());

      // If the joint node's proximal body is not Ground (i.e., null), use it
      // as rge "fixed" body. Record its initial pose so it can restored later.
      RigidBody fixedBody = null;
      RigidTransform3d TGW0 = null;
      if ((fixedBody=jnode.getProximalRigidBody()) != null) {
         //System.out.println ("fixedBody=" + fixedBody.getName());
         TGW0 = new RigidTransform3d(fixedBody.getPose());
      }
   
      VectorNd targetCoords = mySolver.getCoordValues(null);
      // getDefaultTargetValues (targetCoords, useMidRangeLoopTargets);

      // change value over numSteps, doing a looser constraint solve after
      // the intermediate steps

      mySolver.updateBodiesAndConnectors();
      mySolver.updateSolveIndexMap(myMech);

      SetStatus returnStatus = new SetStatus(/*converged*/true);
      returnStatus.addRequests (reqs);

      ArrayList<JointCoordRequest> indepReqs =
         mySolver.findIndependentRequests (reqs);
      if (debug) {
         System.out.print (indepReqs.size()+ " independent coords: ");
         for (JointCoordRequest req : indepReqs){
            System.out.printf (
               " %s -> %10.5f",
               getCoordName(req.myHandle), req.requestedValueDeg());
         }
         System.out.println ("");
      }

      if (debug) {
         System.out.println ("NSTEPS=" + numSteps);
         targetCoords.set (mySolver.getCoordValues(null));
         for (JointCoordRequest req : reqs) {
            targetCoords.set (req.mySolveIdx, req.myRequestedValue  );
         }
         System.out.println ("FULLTARGET=" + mySolver.toDegrees(targetCoords));
      }
      
      for (int stepNum=1; stepNum<=numSteps; stepNum++) {
         RigidTransform3d[] TBWs =null;
         if (jnode.numChildren() > 0) {
            TBWs = getBodyPoses (bodies);
         }
         SetStatus status;

         if (debug) {
            System.out.println ("  STEP " + stepNum);
         }
         status = mySolver.solveForStep (
            indepReqs, targetCoords, fixedBody, TGW0, stepNum, numSteps);

         if (fixedBody != null) {
            // transform all body poses to restore the pose of the fixed body
            RigidTransform3d T = new RigidTransform3d();
            T.mulInverseRight (TGW0, fixedBody.getPose());
            fixedBody.transformPose (T);
            jnode.updateDistalBodiesAndConnectors (T);
         }      
         for (BodyNode child : jnode.getChildren()) {
            RigidTransform3d T = new RigidTransform3d();
            if (child.myBody instanceof RigidBody) {
               RigidBody rbod = (RigidBody)child.myBody;
               int bidx = jnode.indexOfRigidBody (rbod);
               T.mulInverseRight (rbod.getPose(), TBWs[bidx]);
               recursivelyUpdateBodiesAndConnectors (child, T);
            }
         }
         updateWrapPaths();
         if (status.myFinished) {
            returnStatus.setConverged (status.converged());
            break;
         }
         if (stepNum == numSteps && !status.converged()) {
            returnStatus.setConverged (false);
         }
      }
      mySolver.finalizeStatus (returnStatus);
      int icnt = mySolver.numIterations();
      myTotalIterations += icnt;
      returnStatus.addIterations (icnt);
      timer.stop();
      return returnStatus;
   }

   /**
    * Recursively updates the constraints and body poses for all
    * joints and bodies that are distal to a given body node.
    *
    * @param bnode body node where the recursion should begin
    * @param T specifies the transform (in world
    * coordinates) that should be applied to all bodies distal to {@code bnode}
    * due to coordinate changes proximal to it
    */
   private void recursivelyUpdateBodiesAndConnectors (
      BodyNode bnode, RigidTransform3d T) {

      for (JointNode jnode : bnode.getChildren()) {
         jnode.updateDistalBodiesAndConnectors (T);
         for (BodyNode child : jnode.getChildren()) {
            recursivelyUpdateBodiesAndConnectors (child, T);
         }
      }
   }
   
   /**
    * Generate a name for a coordinate, using its name and joint number.
    */
   private String getCoordName (JointCoordinateHandle handle) {
      return handle.getName() + handle.getJoint().getNumber();
   }
}
