/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.PrintStream;

import maspack.collision.ContactPenetratingPoint;
import maspack.collision.EdgeEdgeContact;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.util.DataBuffer;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;

public interface DeformableContactConstraint {

   int hashCode();
   
   /**
    * For creating a "stub" to quickly retrieve an existing constraint
    * @param vtxs
    */
   void setVertices(Vertex3d... vtxs);

   public boolean equals(Object obj);

   public int getSolveIndex();

   public void setSolveIndex(int idx);

   public double getDistance();

   public void setDistance(double d);

   public double getDerivative();

   /**
    * Adds matrix blocks for this constraint to a specified block column of the
    * transposed constraint matrix.
    * 
    * @param GT
    * transposed constraint matrix
    * @param bj
    * block column where blocks should be added
    */
   public void addConstraintBlocks(SparseBlockMatrix GT, int bj);

   /**
    * Sets the friction coefficient for this constraint.
    */
   public void setFriction(double mu);

   /**
    * Returns the friction coefficent for this constraint.
    */
   public double getFriction();

   public void updateFriction();

   public int addFrictionConstraints(
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf);

   /**
    * Returns the normal for this constraint.
    */
   public Vector3d getNormal();

   /**
    * Returns the impulse associated with this constraint.
    */
   public double getImpulse();

   /**
    * Sets the impulse associated with this constraint. In particular, this will
    * be done after velocity solves. The resulting impulse is then used to
    * compute friction and determine if the constraint should be broken.
    */
   public void setImpulse(double lam);

   /**
    * Returns true if this constraint is marked as being active.
    */
   public boolean isActive();

   /**
    * Marks this constraint as being inactive.
    */
   public void setActive(boolean active);

   /**
    * Returns true if the constraint is marked as being 
    * already added to the system
    */
   public boolean isAdded();
   
   /**
    * Marks this constraint as being added to the system
    */
   public void setAdded(boolean added);
   
   /**
    * Begin the process of setting this constraint. After this method call,
    * components can be added using the addPoint() and addFrame() methods. When
    * all components have been added, endSet() should be called.
    */
   public void beginSet();

   /**
    * Concludes the process of setting this constraint.
    */
   public void endSet();

//   /**
//    * Returns true if the component structure of this constraint has changed.
//    * This will be true if, since the last call to clearComponents(), new
//    * component infos have been added, or if any infos are left on the free
//    * list.
//    */
//   public boolean componentsChanged();

   /**
    * Returns true if at least one of the components associated with this
    * constraint is controllable.
    */
   public boolean isControllable();

   public void skipAuxState(DataBuffer data);

   public void getAuxState(DataBuffer data, CollisionData myData, CollisionData otherData);

   public void setAuxState(
      DataBuffer data, CollisionData myData, CollisionData otherData);

   public void print(PrintStream os);
   
   /**
    * Assigns an edge-edge collision
    * 
    * @param eec
    * edge-edge contact
    * @param mu
    * friction coefficient
    * @param thisData the primary deformable collision info
    * @param otherData the other object's collision info
    */
   public void setEdgeEdge(EdgeEdgeContact eec, double mu,
      DeformableCollisionData thisData,
      DeformableCollisionData otherData);

   /**
    * Assigns an edge-rigidBody collision
    * 
    * @param eec
    * edge-edge contact
    * @param mu
    * friction coefficient
    * @param thisData the primary deformable collision info
    * @param rbData the other object's collision info
    */
   public void setEdgeRigidBody(EdgeEdgeContact eec, double mu,
      DeformableCollisionData thisData, RigidBodyCollisionData rbData);

   /**
    * Assigns a Vertex->Rigid body collision, returning the perpendicular
    * distance to the face
    * 
    * @param cpp
    * contact penetrating point
    * @param mu
    * friction coefficient
    * @param thisData the primary deformable collision info
    * @param rbData the other object's collision info
    * @param useSignedDistanceCollider flag for determining normal
    *        and collision location information
    * @return perpendicular distance to face
    */
   public double setVertexRigidBody(ContactPenetratingPoint cpp, double mu,
      DeformableCollisionData thisData,
      RigidBodyCollisionData rbData, boolean useSignedDistanceCollider);

   /**
    * Assigns a Face->RigidBody collision, returning the perpendicular
    * distance to the face
    * 
    * @param cpp
    * contact penetrating point
    * @param mu friction coefficient
    * @param thisData the primary deformable info
    * @param rbData the other object's collision info
    * @return perpendicular distance to face
    */
   public double setFaceRigidBody(ContactPenetratingPoint cpp, double mu,
      DeformableCollisionData thisData, RigidBodyCollisionData rbData);

   /**
    * Assigns a Vertex->Deformable collision, returning the perpendicular
    * distance to the face
    * 
    * @param cpp
    * contact penetrating point
    * @param mu
    * friction coefficient
    * @param thisData the primary deformable collision info
    * @param otherData the other object's collision info
    * @return perpendicular distance to face
    */
   public double setVertexDeformable(ContactPenetratingPoint cpp, double mu,
      DeformableCollisionData thisData, DeformableCollisionData otherData);
   
   /**
    * Adds a point as a component for this constraint.
    * 
    * @param pnt
    * point to add
    * @param weight
    * weighting factor for the point
    */
   public void addPoint(Point pnt, double weight);
   
   /**
    * Adds a frame as a component for this constraint.
    * 
    * @param frame
    * Frame to add
    * @param loc
    * location of the point in frame coordinates
    */
   public void addFrame(Frame frame, double weight, Point3d loc);
   
   
   /**
    * Returns the number of points involved in the constraint
    * @return number of points involved
    */
   public int numPoints();

   /**
    * Returns the number of frames involved in the constraint
    * @return number of frames involved
    */
   public int numFrames();

}
