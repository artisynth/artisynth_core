/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

/**
 * A utility class to help specify (and accumulate) recommendations for
 * how to adjust the step size.
 */
public class StepAdjustment {
   public double myScaling;
   public String myMessage;

   public StepAdjustment () {
      set (1, null);
   }

   public StepAdjustment (double s) {
      set (s, null);
   }

   public StepAdjustment (double s, String message) {
      set (s, message);
   }

   public void recommendAdjustment (double s) {
      recommendAdjustment (s, null);
   }

   public void recommendAdjustment (double s, String message) {
      if (s > 1) {
         // recommending increase
         if (myScaling == 1 || (myScaling > 1 && s < myScaling)) {
            myScaling = s;
            myMessage = message;            
         }
      }
      else if (s < 1) {
         // recommending decrease
         if (s < myScaling) {
            myScaling = s;
            myMessage = message;            
         }
      }
   }

   public void clear() {
      myScaling = 1;
      myMessage = null;
   }

   public void set (double s, String message) {
      myScaling = s;
      myMessage = message;
   }

   public void setMessage (String message) {
      myMessage = message;
   }

   public String getMessage() {
      return myMessage;
   }

   public void setScaling (double s) {
      myScaling = s;
   }

   public double getScaling() {
      return myScaling;
   }
}
