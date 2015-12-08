/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;

/**
 * Defines a component to which a point can be attached.
 * 
 * @author lloyd
 */
public interface PointAttachable extends ModelComponent {
   
   /**
    * Returns a PointAttachment that attaches <code>pnt</code>
    * to this component. It should not be assumed that <code>pnt</code>
    * is currently connected to the component hierarchy, and no attempt
    * should be made to connect the returned attachment to the hierarchy;
    * the latter, if desired, is the responsibility of the caller.
    * 
    * <p>In some cases, it may not be possible to attach the point at
    * its present location. In that case, the method will create an attachment
    * to the nearest feasible location.
    * 
    * @param pnt point for which an attachment should be created
    * @return attachment connecting <code>pnt</code> to this component
    */
   public PointAttachment createPointAttachment (Point pnt);

}

