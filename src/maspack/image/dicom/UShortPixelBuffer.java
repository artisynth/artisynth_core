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
public class UShortPixelBuffer extends DicomPixelBufferBase {

   short[] pixels;

   public UShortPixelBuffer(int size) {
      this(new short[size]);
   }
   
   public UShortPixelBuffer(short[] pixels) {
      super(PixelType.USHORT);
      this.pixels = pixels;
   }
   
   public UShortPixelBuffer (short[] pixels, int offset, int length) {
      super(PixelType.USHORT);
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
   protected int getNumValues() {
      return pixels.length;
   }

   @Override
   public int getValue(int idx) {
      return pixels[idx] & USHORT_MAX;
   }
   
   @Override
   protected void setValue(int idx, int val) {
      pixels[idx] = (short)val;
   }
   
   public short[] getBuffer() {
      return pixels;
   }
   
   @Override
   public int getNumChannels () {
      return 1;
   }
   
}
