/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.ModelComponent;
import maspack.matrix.RigidTransform3d;

/**
 * Defines a component to which a frame can be attached.
 * 
 * @author lloyd
 */
public interface FrameAttachable extends PointAttachable {
   
   /**
    * Returns a FrameAttachment that attaches a <code>frame</code> to this
    * component. Once attached the frame will follow the body around.  The
    * initial pose of the frame is specified by <code>TFW</code>, which gives
    * its position and orientation in world coordinates. If <code>TFW</code> is
    * <code>null</code>, then the current pose of the frame is used. If
    * <code>frame</code> is <code>null</code>, then a virtual attachment is
    * created at the initial pose specified by
    * <code>TFW</code>. <code>frame</code> and <code>TFW</code> cannot both be
    * <code>null</code>.
    * 
    * <p>In some cases, it may not be possible to attach the frame at
    * the requested location. In that case, the method will relocate the 
    * frame to the nearest feasible attachment location.
    * 
    * @param frame frame to be attached
    * @param TFW transform from (initial) frame coordinates to world
    * coordinates
    * @return attachment connecting <code>frame</code> to this component
    */
   public FrameAttachment createFrameAttachment (
      Frame frame, RigidTransform3d TFW);

}

