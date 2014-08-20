/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC), John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.ArrayList;

import maspack.collision.ContactPenetratingPoint;
import maspack.collision.EdgeEdgeContact;
import maspack.geometry.Vertex3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.util.DataBuffer;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;

/**
 * Interface for handling collisions with a particular component
 */
public interface DeformableCollisionData extends CollisionData {

   void getBilateralSizes (VectorNi sizes);

   int getBilateralInfo (ConstraintInfo[] ginfo, int idx);

   /**
    * Adds the matrix blocks associated with the bodies contact constraints
    * to the constraint matrix transpose. This method is used when building
    * the constraint matrix structure. Blocks should be added starting at the
    * block column indicated by numb. The block column number is then updated
    * and returned by this method.
    * 
    * <p>
    * Deformable contact constraints are currently implemented as bilateral
    * constraints to save computational effort. The unilateral nature of the
    * constraint is handled by examining its impulse after the velocity
    * solve. If the impulse is negative, that indicates that the constraint
    * is trying to separate and constraint is removed.
    * 
    * @param GT
    * constraint matrix transpose
    * @param numb
    * block column number where the blocks should begin
    * @return updated block column number
    */
   int addBilateralConstraints (SparseBlockMatrix GT, VectorNd dg, int numb);

   int maxFrictionConstraintSets();

   int addFrictionConstraints (SparseBlockMatrix DT, FrictionInfo[] finfo, int numf);

   void updateFrictionConstraints ();

   /**
    * Sets the impulses associated with the contact constraints. This is
    * called by the solver after performing a solve. The resulting impulse
    * information is then used by the constraint to compute friction and to
    * decide when contact constraints should be broken.
    * 
    * @param lam
    * vector containing impulse values
    * @param idx
    * starting index for constraint impulses associated with this collision
    * pair
    * @return updated index value (i.e., idx plus the number of contact
    * constraints).
    */
   int setBilateralImpulses (VectorNd lam, int idx);

   void zeroImpulses();

   int getBilateralImpulses (VectorNd lam, int idx);

   /**
    * Deactivates all the current contact constraints. Constraints may be
    * reactivated later if they are found in the next collision detection and
    * are not separating.
    */
   void clearContactActivity();

   /**
    * Gets a constraint for vertex-face contact.
    * If a matching constraint already exists and is not
    * trying to separate, then that is returned; Otherwise, null is returned.
    * Note that only the constraint container is found or allocated here;
    * it's contents are set later.
    * 
    * @param cpp the contact penetrating point, consisting of a vertex/face 
    *        pair
    * @param isVertex boolean indicating whether for this data, we own
    *        the vertex or the face
    * @return constraint structure
    */
   public DeformableContactConstraint getContact (ContactPenetratingPoint cpp, boolean isVertex);

   /**
    * Gets a constraint for an edge-edge contact.
    * If a matching constraint already exists and is not
    * trying to separate, then that is returned; Otherwise, null is returned.
    * Note that only the constraint container is found or allocated here;
    * it's contents are set later.
    * 
    * @param eec Edge-Edge contact
    * @param isFirst indicates whether this object owns the first edge 
    *        or second
    * @return constraint structure
    */
   public DeformableContactConstraint getContact (EdgeEdgeContact eec, boolean isFirst);
   
   /**
    * Add contact constraint to set of constraints
    */
   public void addContact (DeformableContactConstraint c);
   
   public DeformableContactConstraint createContact(Vertex3d... vtxs);

   /**
    * Returns true if this FEM data set contains an active contact involving
    * a specified vertex or pair of vertices.
    */
   boolean hasActiveContact (ContactPenetratingPoint cpp, boolean isVertex);

   boolean hasActiveContact (EdgeEdgeContact eec, boolean isFirst);
   
   /**
    * Determines whether or not any of the contact constraints are currently
    * active
    */
   public boolean hasActiveContacts();
   
   /**
    * Counts the number of contact constraints that are currently
    * active
    */
   public int numActiveContacts();


   /**
    * Removes all contact contraints which are currently inactive. This is
    * called after all new constraints have been determined.
    */
   void removeInactiveContacts();

   /**
    * Notification that the components associated with a specific contact
    * constraint has changed, and therefore the GT matrix will have a
    * different structure for the next step.
    */
   void notifyContactsChanged();

   /**
    * Returns true if the contact structure has changed such that the GT
    * matrix will have a different structure and will have to be reanalyzed.
    * Contact structure will change if contact constraints are added or
    * removed, or if the components associated with a specific constraint
    * have changed.
    * 
    * @return true if contact structure has changed.
    */
   boolean contactsHaveChanged();

   /**
    * Clears all the contact constraint data. This is done whenever there is
    * a change in component structure or activity, requiring that contact
    * information be rebuilt from scratch.
    */
   void clearContactData();

   
   public void skipAuxState (DataBuffer data);

   public void getAuxState (DataBuffer data, CollisionData otherData);

   public void setAuxState (DataBuffer data, CollisionData otherData);

   public void getInitialAuxState (DataBuffer newData, DataBuffer oldData);
   
   void setPenetrationTol(double tol);
   
   double getCollisionTol();
   
   void setFriction(double mu);
   
   double getFriction();
   
   /**
    * Allow a collision between this object and the supplied 'other'.
    * You should not allow if the objects are attached at any of the
    * involved points.
    * 
    * @param cpp the contact-penetrating point involved between the two.
    * @param isVertex if true, checks if we can allow collision based
    *        on the vertex, otherwise if we are checking the face   
    * @param otherData  the other collision object involved
    * @return true if the collision is allowed, false otherwise
    */
   boolean allowCollision(ContactPenetratingPoint cpp, boolean isVertex,
      CollisionData otherData);
   
   boolean allowCollision(EdgeEdgeContact eec, boolean isFirst, 
      CollisionData otherData);
   
   // XXX used by "allow collision", probably needs to be changed
   // to become more generalizable (active parts might be shared
   // between skinned components)
   int numActiveMasters(ContactPenetratingPoint cpp, boolean isVertex);
   
   int numActiveMasters(EdgeEdgeContact eec, boolean isFirst);
   
   /**
    * Determines the set of points that a vertex's position is dependent
    * upon, along with their weights (assuming a linear relationship)
    * @param vtx the vertex belonging to <code>this</code> collision
    * data
    * @param pointDeps list of points that is filled
    * @param pointWgts weights associated with the returned points
    * @param frameDeps list of frames that is filled
    * @param frameWgts weights associated with the returned frames
    * @return the number of dependent points
    */
   public int getVertexDependencies(Vertex3d vtx, ArrayList<Point> pointDeps,
      VectorNd pointWgts, ArrayList<Frame> frameDeps, VectorNd frameWgts);

   /**
    * Culls the set of constraints to a reduced set
    * @param points list of deformable penetrating points
    * @param otherData collision data from the other object
    */
   void reduceConstraints(ArrayList<ContactPenetratingPoint> points,
      ArrayList<ContactPenetratingPoint> otherPoints,
      CollisionData otherData);
   
}
