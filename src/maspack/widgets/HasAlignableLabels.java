/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

public interface HasAlignableLabels {

   public void setLabelSpacing (LabelSpacing spacing);

   public void getLabelSpacing (LabelSpacing spacing);

   public void getPreferredLabelSpacing (LabelSpacing spacing);
}
