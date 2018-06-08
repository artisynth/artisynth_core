/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

/**
 * Stores a set of pixels in grayscale short form
 * @author Antonio
 */
public class ShortPixelBuffer extends DicomPixelBufferBase {

   short[] pixels;

   public ShortPixelBuffer(int size) {
      this(new short[size]);
   }
   
   public ShortPixelBuffer(short[] pixels) {
      super(PixelType.SHORT);
      this.pixels = pixels;
   }
   
   public ShortPixelBuffer (short[] pixels, int offset, int length) {
      super(PixelType.SHORT);
      this.pixels = new short[length];
      for (int i=0; i<length; ++i) {
         this.pixels[i] = pixels[offset+i];
      }
   }

   @Override
   public int getNumPixels() {
      return pixels.length;
   }

   @Override
   public int getValue(int idx) {
      return pixels[idx];
   }
   
   @Override
   protected void setValue(int idx, int val) {
      pixels[idx] = (short)val;
   }
   
   @Override
   protected int getNumValues() {
      return pixels.length;
   }

   public short[] getBuffer() {
      return pixels;
   }
   
   @Override
   public int getNumChannels () {
      return 1;
   }
   
}
