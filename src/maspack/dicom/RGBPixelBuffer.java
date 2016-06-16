/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.dicom;

import java.nio.ByteBuffer;

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
      return PixelType.BYTE_RGB;
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
   
   @Override
   public int getPixels(int x, int dx, int nx, PixelType type, ByteBuffer pixels, 
      DicomPixelInterpolator interp) {
      
      int off = 0;
      switch (type) {
         case BYTE: {
            byte[] buff = new byte[1];
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               interp.interpRGBByte (this.pixels, iidx, buff, 0);
               pixels.put (buff[0]);
               iidx += 3*dx;
            }
            off = nx;
            break;
         }
         case BYTE_RGB: {
            byte[] buff = new byte[3];
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               interp.interpRGBRGB (this.pixels, iidx, buff, 0);
               pixels.put (buff);
               iidx += 3*dx;
            }
            off = 3*nx;
            break;
         }
         case SHORT: {
            short[] buff = new short[1];
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               interp.interpRGBShort (this.pixels, iidx, buff, 0);
               pixels.putShort (buff[0]);
               iidx += 3*dx;
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
         oidx = interp.interpRGBRGB(this.pixels, iidx, pixels, oidx);
         iidx += 3*dx;
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
         oidx = interp.interpRGBByte(this.pixels, iidx, pixels, oidx);
         iidx += 3*dx;
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
         oidx = interp.interpRGBShort(this.pixels, iidx, pixels, oidx);
         iidx += 3*dx;
      }
      return oidx;
   }

   public int getPixels(int x, 
      int dx,
      int nx,
      DicomPixelBuffer pixels, int offset,
      DicomPixelInterpolator interp) {

      return pixels.setPixelsRGB(x, dx, nx, this.pixels, offset, interp);
   }

   public int setPixelsRGB(int x,
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelInterpolator interp) {

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
      DicomPixelInterpolator interp) {

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
      DicomPixelInterpolator interp) {

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
      DicomPixelInterpolator interp) {

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
