/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

/** 
 * Storage class to describe label alignment for components which have
 * alignable labels.
 */
public class LabelSpacing {

   public int labelWidth;
   public int preSpacing;

   public void set (LabelSpacing spacing) {
      labelWidth = spacing.labelWidth;
      preSpacing = spacing.preSpacing;
   }

   public void set (int labelWidth, int preSpacing) {
      this.labelWidth = labelWidth;
      this.preSpacing = preSpacing;
   }

   /** 
    * Sets this spacing so that both labelWidth and and preSpacing are at least
    * as large as those of a supplied spacing, and returns true if either field
    * was changed.
    */
   public boolean expand (LabelSpacing spacing) {
      boolean expanded = false;
      if (labelWidth < spacing.labelWidth) {
         labelWidth = spacing.labelWidth;
         expanded = true;
      }
      if (preSpacing < spacing.preSpacing) {
         preSpacing = spacing.preSpacing;
         expanded = true;
      }
      return expanded;
   }      

   public boolean isEqual (LabelSpacing spacing) {
      return (spacing.labelWidth == labelWidth &&
              spacing.preSpacing == preSpacing);
   }

   public String toString() {
      return labelWidth + " " + preSpacing;
   }
}