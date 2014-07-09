/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

public interface LabeledWidget extends HasAlignableLabels {

   public void setLabelText (String text);

   public void setToolTipText (String text);
   
   public String getToolTipText ();

   public String getLabelText ();

}
