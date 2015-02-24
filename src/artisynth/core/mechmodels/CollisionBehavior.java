/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

/**
 * Contains information describing the appropriate collision response
 * between two bodies.
 */
public class CollisionBehavior {
   
   public enum Method {
      DEFAULT,
      VERTEX_PENETRATION,
      VERTEX_EDGE_PENETRATION,
      CONTOUR_REGION,
      VERTEX_PENETRATION_BILATERAL
   }

   boolean myEnabled = true;
   double myFriction = 0.0;
   CollidablePair myPair = null;
   Method myMethod = Method.DEFAULT;
//   MechModel myModel = null;

   /** 
    * Creates a new CollisionBehavior with default values.
    */
   public CollisionBehavior() {
      myEnabled = false;
      myFriction = 0.0;
      myMethod = Method.DEFAULT;
   }

   /** 
    * Creates a new CollisionBehavior whose values are copied from
    * an existing one.
    * 
    * @param b behavior to copy
    */
   public CollisionBehavior (CollisionBehavior b) {
      set (b);
   }

   /** 
    * Creates a new CollisionBehavior with a specified enabling
    * and friction.
    * 
    * @param enabled true if collisions are enabled
    * @param mu friction coefficient
    */
   public CollisionBehavior (boolean enabled, double mu) {
      setEnabled (enabled);
      setFriction (mu);
   }

   /** 
    * Gets the friction associated with this behavior.
    * 
    * @return friction associated with this behavior
    */
   public double getFriction() {
      return myFriction;
   }

   /** 
    * Sets the friction coefficent associated with this behavior.
    * 
    * @param mu friction associated with this behavior
    */
   public void setFriction (double mu) {
      myFriction = mu;
   }

   /** 
    * Returns true if collisions are enabled in this behavior.
    * 
    * @return true if collisions are enabled
    */
   public boolean isEnabled() {
      return myEnabled;
   }

   /** 
    * Enables or disabled collisions for this behavior.
    * 
    * @param enabled if true, enables collisions
    */
   public void setEnabled (boolean enabled) {
      myEnabled = enabled;
   }

   /** 
    * Returns the collision method to be used by this behavior.
    * 
    * @return collision method for this behavior
    */
   public Method getMethod() {
      return myMethod;
   }

   /** 
    * Set the collision method to be used by this behavior.
    * 
    * @param method collision method to be used
    */
   public void setMethod (Method method) {
      myMethod = method;
   }

   /** 
    * Returns true if this behavior equals another. The values
    * returned by {@link #getPair getPair}
    * are not considered in this comparision.
    * 
    * @param b behavior to compare with
    * @return true if this behavior equals b
    */
   public boolean equals (CollisionBehavior b) {
      return (myEnabled == b.myEnabled &&
              myFriction == b.myFriction &&
              myMethod == b.myMethod);
   }

   public void set (CollisionBehavior v) {
      myFriction = v.myFriction;
      myEnabled = v.myEnabled;
      myMethod = v.myMethod;
   }

   /** 
    * {@inheritDoc}
    */
   public boolean equals (Object obj) {
      if (!(obj instanceof CollisionBehavior)){
         return false;
      }
      return equals ((CollisionBehavior)obj);
   }

   public String toString() {
      return ("enabled=" + myEnabled + " friction=" + myFriction +
             " method=" + myMethod);
   }

//   /** 
//    * Returns the MechModel responsible for determining this behavior. This
//    * information is not normally set by the application.
//    * 
//    * @return MechModel associated with this behavior
//    */
//   public MechModel getModel() {
//      return myModel;
//   }

   /** 
    * Returns the collision pair associated with this behavior. This
    * information is not normally set by the application.
    * 
    * @return collision pair associated with this behavior.
    */
   public CollidablePair getPair() {
      return myPair;
   }
   
}
