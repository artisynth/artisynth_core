/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;

/**
 * A model component associated with a dynamic attachment which can be used as
 * an attachment request.  Implementations include DynamicAttachmentComp, where
 * the attachment and the request are the same, and Marker, which contains the
 * attachment internally. An attachment request can be registered with a
 * DynamicComponent D, so that the correct linkage between D and the attachment
 * is established when D connects to the hierarchy in such a way that the
 * request component is reachable.
 */
public interface AttachingComponent extends ModelComponent {
   
   public void connectAttachment (DynamicComponent dcomp);
   
   public DynamicAttachment getAttachment();

}
