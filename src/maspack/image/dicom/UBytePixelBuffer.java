/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

/**
 * Stores a set of pixels in grayscale byte form
 * @author Antonio
 */
public class UBytePixelBuffer extends DicomPixelBufferBase {

   byte[] pixels;
   
   public UBytePixelBuffer(int size) {
      this(new byte[size]);
   }
   
   public UBytePixelBuffer (byte[] pixels) {
      super(PixelType.UBYTE);
      this.pixels = pixels; 
   }
   
   public UBytePixelBuffer (byte[] pixels, int offset, int length) {
      super(PixelType.UBYTE);
      this.pixels = new byte[length];
      for (int i=0; i<length; ++i) {
         this.pixels[i] = pixels[offset+i];
      }
   }

   @Override
   public int getValue(int idx) {
      return pixels[idx] & UBYTE_MAX;
   }
   
   @Override
   protected void setValue(int idx, int val) {
      pixels[idx] = (byte)val;
   }
   
   @Override
   protected int getNumValues() {
      return pixels.length;
   }
   
   @Override
   public int getNumPixels() {
      return pixels.length;
   }
 
   @Override
   public byte[] getBuffer() {
      return pixels;
   }
   
   @Override
   public int getNumChannels () {
      return 1;
   }

}
