/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

/**
 * Raw interpolator, does not rescale intensities.
 *
 */
public class DicomRawPixelInterpolator extends DicomPixelInterpolator {

   /**
    * Create a default interpolator.
    */
   public DicomRawPixelInterpolator() {
   }
   
   private int rescale(double in, int ymin, int ymax) {
      if (in > ymax) {
         return ymax;
      } else if (in < ymin) {
         return ymin;
      }
      return (int)Math.round (in);
   }
   
   @Override
   public int interpGrayscale(double in, int ymin, int ymax) {
      return rescale(in, ymin, ymax);
   }
   
   @Override
   public void interpRGB(double[] in, int ymin, int ymax, int[] out) {
      for (int i=0; i<3; i++) {
         out[i] = rescale(in[i],  ymin, ymax);
      }
   }
   
   @Override
   public void interpGrayscaleToRGB(double gray, int ymin, int ymax, int[] out) {
      int val = rescale(gray, ymin, ymax);
      out[0] = val;
      out[1] = val;
      out[2] = val;
   }
   
   @Override
   public int interpRGBToGrayscale(double[] rgb, int ymin, int ymax) {
      double gray = ( rgb[0] + rgb[1] + rgb[2])/3;
      gray = rescale (gray, ymin, ymax);
      return (int)gray;
   }
   
}
