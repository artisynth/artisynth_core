/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.metal.MetalSliderUI;

import maspack.util.DoubleInterval;
import maspack.util.NumericInterval;

/**
 * A subclass of JSlider that allows us to specify exactly how many pixels
 * are in the track. This is useful for creating sliders that produce
 * reasonable increments as they are dragged. 
*/
public class Slider extends JSlider {

   private int myTrackLength = 200;
   private static int myOffset = -1;
   private static boolean myOffsetDetermined = false;

   public Slider() {
      super();
   }

   public Slider (int min, int max) {
      super (min, max);
   }

   public Dimension getPreferredSize() {
      Dimension size = new Dimension(super.getPreferredSize());
      if (!myOffsetDetermined) {
         calculateOffset();
      }
      if (myOffset != -1) {
         size.width = myTrackLength + myOffset;
      }
      return size;
   }

   private static class TestMetalUI extends MetalSliderUI {
      public TestMetalUI (){
         super ();
      }

      public int getTrackBuffer(){
         return trackBuffer;
      }
   }

   private static void calculateOffset () {
      JSlider slider = new JSlider (0, 10000);
      Dimension prefSize = slider.getPreferredSize();
      slider.setSize (prefSize);
      SliderUI ui = slider.getUI();
      if (ui instanceof MetalSliderUI) {
         TestMetalUI testUI = new TestMetalUI();
         slider.setUI (testUI);
         myOffset = 2*testUI.getTrackBuffer();
      }
      else if (ui.getClass().getName().endsWith("AquaSlider")) {
	  myOffset = 28;
      }
      myOffsetDetermined = true;
   }

   public static void main (String[] args) {
      calculateOffset();
   }

   
}
