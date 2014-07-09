/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;

/**
 * Indicates a model component that contains one or more DynamicAttachments
 * internally, as either child or internal (hidden) components.
 */
public interface HasAttachments {

   /**
    * Returns the attachments contained by this component. This will be called
    * by MechModelBase when creating a complete list of all the attachments in
    * the model.
    */
   public void getAttachments (List<DynamicAttachment> list);
}
