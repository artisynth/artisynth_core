/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

/**
 * Indicates a model component that contains objects (not necessarily just
 * ModelComponents) whose position and/or velocity are completely coupled to
 * the position and velocity of the dynamic components of the system.  A good
 * example is a skinned mesh, attached to underlying Frames and Particles, in
 * which the slave objects are the mesh vertices.
 *
 * <p>
 * Whenever the positions and/or velocities of the dynamic system components
 * change, the position and velocity state of the slaved object within
 * this component must be updated using {@link #updateSlavePos()}
 * and/or {@link #updateSlaveVel()}.
 */
public interface HasSlaveObjects {

   /**
    * Called when the system's dynamic position state changes, to update the
    * position state of the slave objects.
    */
   public void updateSlavePos();

   /**
    * Called when the system's dynamic velocity state changes, to update the
    * velocity state of the slave objects.
    */
   public void updateSlaveVel();
}
