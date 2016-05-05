/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.dicom;

import java.nio.ByteBuffer;

/**
 * Stores a set of pixels in grayscale short form
 * @author Antonio
 */
public class ShortPixelBuffer implements DicomPixelBuffer {

   short[] pixels;

   public ShortPixelBuffer(int size) {
      this.pixels = new short[size];
   }
   
   public ShortPixelBuffer(short[] pixels) {
      this.pixels = pixels;
   }

   @Override
   public PixelType getPixelType() {
      return PixelType.SHORT;
   }

   @Override
   public int getNumPixels() {
      return pixels.length;
   }

   @Override
   public Short getPixel(int n) {
      return pixels[n];
   }
   
   @Override
   public int getPixels(int x, int dx, int nx, PixelType type, ByteBuffer pixels, 
      DicomPixelInterpolator interp) {
      
      int off = 0;
      switch (type) {
         case BYTE: {
            byte[] buff = new byte[1];
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               interp.interpShortByte (this.pixels, iidx, buff, 0);
               pixels.put (buff[0]);
               iidx += dx;
            }
            off = nx;
            break;
         }
         case BYTE_RGB: {
            byte[] buff = new byte[3];
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               interp.interpShortRGB (this.pixels, iidx, buff, 0);
               pixels.put (buff);
               iidx += dx;
            }
            off = 3*nx;
            break;
         }
         case SHORT: {
            short[] buff = new short[1];
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               interp.interpShortShort (this.pixels, iidx, buff, 0);
               pixels.putShort (buff[0]);
               iidx += dx;
            }
            off = 2*nx;
            break;
         }
      }
      
      return off;
   }

   public int getPixelsRGB(int x,
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelInterpolator interp) {

      int iidx = x;
      int oidx = offset;
      for (int i = 0; i < nx; i++) {
         oidx = interp.interpShortRGB(this.pixels, iidx, pixels, oidx);
         iidx += dx;
      }
      return oidx;
   }

   public int getPixelsByte(int x, 
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelInterpolator interp) {

      int iidx = x;
      int oidx = offset;
      for (int i = 0; i < nx; i++) {
         oidx = interp.interpShortByte(this.pixels, iidx, pixels, oidx);
         iidx += dx;
      }
      return oidx;
   }

   public int getPixelsShort(int x,
      int dx,
      int nx, short[] pixels, int offset,
      DicomPixelInterpolator interp) {

      int iidx = x;
      int oidx = offset;
      for (int i = 0; i < nx; i++) {
         oidx = interp.interpShortShort(this.pixels, iidx, pixels, oidx);
         iidx += dx;
      }
      return oidx;
   }

   public int getPixels(int x, 
      int dx,
      int nx,
      DicomPixelBuffer pixels, int offset,
      DicomPixelInterpolator interp) {

      return pixels.setPixelsShort(x, dx, nx, this.pixels, offset, interp);
   }

   public int setPixelsRGB(int x,
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelInterpolator interp) {

      int oidx = x;
      int iidx = offset;
      for (int i = 0; i < nx; i++) { 
         iidx = interp.interpRGBShort(pixels, iidx, this.pixels, oidx);
         oidx += dx;
      }
      return iidx;
   }

   public int setPixelsByte(int x, 
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelInterpolator interp) {

      int oidx = x;
      int iidx = offset;
      for (int i = 0; i < nx; i++) {
         iidx = interp.interpByteShort(pixels, iidx, this.pixels, oidx);
         oidx += dx;
      }
      return iidx;
   }

   public int setPixelsShort(int x,
      int dx,
      int nx, short[] pixels, int offset,
      DicomPixelInterpolator interp) {

      int oidx = x;
      int iidx = offset;
      for (int i = 0; i < nx; i++) {
         iidx = interp.interpShortShort(pixels, iidx, this.pixels, oidx);
         oidx += dx;
      }
      return iidx;
   }

   public int setPixels(int x, 
      int dx,
      int nx,
      DicomPixelBuffer pixels, int offset,
      DicomPixelInterpolator interp) {

      return pixels.getPixelsShort(x, dx, nx, this.pixels, offset, interp);
   }
   
   public int getMaxIntensity() {
      int max = Short.MIN_VALUE;
      for (int i=0; i<pixels.length; i++) {
         int val = pixels[i];
         if (val > max) {
            max = val;
         }
      }
      return max;
   }
   
   public int getMinIntensity() {
      int min = Short.MAX_VALUE;
      for (int i=0; i<pixels.length; i++) {
         int val = pixels[i];
         if (val < min) {
            min = val;
         }
      }
      return min;
   }

   public short[] getBuffer() {
      return pixels;
   }
   
}
