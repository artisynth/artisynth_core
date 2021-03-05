/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.List;

import maspack.matrix.RigidTransform3d;
import artisynth.core.modelbase.ModelComponent;

/**
 * Defines a component to which joints can be attached.
 * 
 * @author lloyd
 */
public interface ConnectableBody extends ModelComponent, FrameAttachable {
   
   public void addConnector (BodyConnector c);

   public void removeConnector (BodyConnector c);
   
   public boolean containsConnector (BodyConnector c);

   public List<BodyConnector> getConnectors();
   
   public void transformPose (RigidTransform3d T);

   public boolean isFreeBody();
   
   public boolean isDeformable();
   
}

