/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.dicom;

/**
 * Stores a set of pixels in RGB byte form (3 consecutive bytes per pixel)
 * @author Antonio
 */
public class RGBPixelBuffer implements DicomPixelBuffer {

   byte[] pixels;

   public RGBPixelBuffer(int size) {
      pixels = new byte[size];
   }
   
   public RGBPixelBuffer(byte[] pixels) {
      this.pixels = pixels;
   }
   
   public RGBPixelBuffer (int[] pixels) {
      this.pixels = new byte[pixels.length*3];
      int sidx = 0;
      for (int i=0; i<pixels.length; i++) {
         this.pixels[sidx++] = (byte)((pixels[i] & 0x00FF0000) >> 16);
         this.pixels[sidx++] = (byte)((pixels[i] & 0x0000FF00) >> 8);
         this.pixels[sidx++] = (byte)((pixels[i] & 0x000000FF));
      }

   }

   @Override
   public PixelType getPixelType() {
      return PixelType.RGB;
   }

   @Override
   public int getNumPixels() {
      return pixels.length/3;
   }

   @Override
   public byte[] getPixel(int n) {
      int sidx = 3*n;
      return new byte[]{pixels[sidx], pixels[sidx+1], pixels[sidx+2]};
   }

   public int getPixelsRGB(int x,
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelConverter interp) {

      int iidx = x;
      int oidx = offset;
      for (int i = 0; i < nx; i++) {
         oidx = interp.interpRGBRGB(this.pixels, iidx, pixels, oidx);
         iidx += 3*dx;
      }
      return oidx;
   }

   public int getPixelsByte(int x, 
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelConverter interp) {

      int iidx = x;
      int oidx = offset;
      for (int i = 0; i < nx; i++) {
         oidx = interp.interpRGBByte(this.pixels, iidx, pixels, oidx);
         iidx += 3*dx;
      }
      return oidx;
   }

   public int getPixelsShort(int x,
      int dx,
      int nx, short[] pixels, int offset,
      DicomPixelConverter interp) {

      int iidx = x;
      int oidx = offset;
      for (int i = 0; i < nx; i++) {
         oidx = interp.interpRGBShort(this.pixels, iidx, pixels, oidx);
         iidx += 3*dx;
      }
      return oidx;
   }

   public int getPixels(int x, 
      int dx,
      int nx,
      DicomPixelBuffer pixels, int offset,
      DicomPixelConverter interp) {

      return pixels.setPixelsRGB(x, dx, nx, this.pixels, offset, interp);
   }

   public int setPixelsRGB(int x,
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelConverter interp) {

      int oidx = x;
      int iidx = offset;
      for (int i = 0; i < nx; i++) {
         iidx = interp.interpRGBRGB(pixels, iidx, this.pixels, oidx);
         oidx += 3*dx;
      }
      return iidx;
   }

   public int setPixelsByte(int x, 
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelConverter interp) {

      int oidx = x;
      int iidx = offset;
      for (int i = 0; i < nx; i++) {
         iidx = interp.interpByteRGB(pixels, iidx, this.pixels, oidx);
         oidx += 3*dx;
      }
      return iidx;
   }

   public int setPixelsShort(int x,
      int dx,
      int nx, short[] pixels, int offset,
      DicomPixelConverter interp) {

      int oidx = x;
      int iidx = offset;
      for (int i = 0; i < nx; i++) {
         iidx = interp.interpShortRGB(pixels, iidx, this.pixels, oidx);
         oidx += 3*dx;
      }
      return iidx;
   }

   public int setPixels(int x, 
      int dx,
      int nx,
      DicomPixelBuffer pixels, int offset,
      DicomPixelConverter interp) {

      return pixels.getPixelsRGB(x, dx, nx, this.pixels, offset, interp);
   }
   
   public int getMaxIntensity() {
      int max = Byte.MIN_VALUE;
      for (int i=0; i<pixels.length; i++) {
         int val = pixels[i];
         if (val > max) {
            max = val;
         }
      }
      return max;
   }
   
   public int getMinIntensity() {
      int min = Byte.MAX_VALUE;
      for (int i=0; i<pixels.length; i++) {
         int val = pixels[i];
         if (val < min) {
            min = val;
         }
      }
      return min;
   }
   
   public byte[] getBuffer() {
      return pixels;
   }

}
