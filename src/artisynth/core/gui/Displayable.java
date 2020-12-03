/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui;

import javax.swing.JPanel;

/**
 * Interface for displayable objects.
 */

public interface Displayable {
   // /**
   // * Display the contents of this object into a specified
   // * component using a supplied graphics context. This
   // * method will be called within the component's
   // * paint routine.
   // *
   // * @param c component into which this object is being drawn
   // * @param g graphics context to perform the drawing
   // */
   // public void display (Component c, Graphics g);

//   /**
//    * Gets the range hints associated with this displayable object. Ranges hints
//    * that are unused should be given values of 0.
//    * 
//    * @param ranges
//    * range hints. The first and second elements are the minimum and maximum
//    * horizontal values, and the third and fourth elements are the minimun and
//    * maximum vertical values.
//    * @see #setRangeHints
//    */
//
//   public void getRangeHints (double[] ranges);
//
//   /**
//    * Sets the range hints associated with this displayable object. This is
//    * optional information which specifies minimum and maximum values in both
//    * the horizontal and vertical directions.
//    * 
//    * @param ranges
//    * range hints. The first and second elements are the minimum and maximum
//    * horizontal values, and the third and fourth elements are the minimun and
//    * maximum vertical values.
//    * @see #getRangeHints
//    */

//   public void setRangeHints (double[] ranges);

   // myProbe is needed by large probe display to refresh the small display
   public JPanel getDisplay (
      int width, int height, boolean isLargeDisplay);

   public boolean removeDisplay (JPanel display);

   public void updateDisplays();

}
