/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.Map;
import artisynth.core.modelbase.ModelComponent;

/**
 * A ModelComponent that is also a DynamicAttachment
 */
public interface DynamicAttachmentComp extends DynamicAttachment, ModelComponent {

   public DynamicAttachmentBase copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap);
}
