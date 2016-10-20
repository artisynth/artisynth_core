/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import java.nio.ByteBuffer;

/**
 * Stores a set of pixels in grayscale byte form
 * @author Antonio
 */
public class BytePixelBuffer implements DicomPixelBuffer {

   byte[] pixels;

   public BytePixelBuffer(int size) {
      pixels = new byte[size];
   }
   
   public BytePixelBuffer (byte[] pixels) {
      this.pixels = pixels; 
   }

   @Override
   public PixelType getPixelType() {
      return PixelType.BYTE;
   }

   @Override
   public int getNumPixels() {
      return pixels.length;
   }

   @Override
   public Byte getPixel(int n) {
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
               interp.interpByteByte (this.pixels, iidx, buff, 0);
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
               interp.interpByteRGB (this.pixels, iidx, buff, 0);
               pixels.put (buff);
               iidx += dx;
            }
            off = nx*3;
            break;
         }
         case SHORT: {
            short[] buff = new short[1];
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               interp.interpByteShort (this.pixels, iidx, buff, 0);
               pixels.putShort (buff[0]);
               iidx += dx;
            }
            off = nx*2;
            break;
         }
      }
      
      return off;
   }
   
   @Override
   public int getPixelsRGB(int x,
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelInterpolator interp) {
    
      int iidx = x;
      int oidx = offset;
      for (int i = 0; i < nx; i++) {
         //         pixels[oidx++] = this.pixels[iidx];
         //         pixels[oidx++] = this.pixels[iidx];
         //         pixels[oidx++] = this.pixels[iidx];
         oidx = interp.interpByteRGB(this.pixels, iidx, pixels, oidx);
         iidx += dx;
      }
      return oidx;
   }
   
   @Override
   public int getPixelsByte(int x, 
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelInterpolator interp) {
      
      int iidx = x;
      int oidx = offset;
      for (int i = 0; i < nx; i++) {
         // pixels[oidx++] = this.pixels[iidx];
         oidx = interp.interpByteByte(this.pixels, iidx, pixels, oidx);
         iidx += dx;
      }
      return oidx;
   }
   
   @Override
   public int getPixelsShort(int x,
      int dx,
      int nx, short[] pixels, int offset,
      DicomPixelInterpolator interp) {
      
      int iidx = x;
      int oidx = offset;
      for (int i = 0; i < nx; i++) {
         oidx = interp.interpByteShort(this.pixels, iidx, pixels, oidx);
         iidx += dx;
      }
      return oidx;
   }
   
   @Override
   public int getPixels(int x, 
      int dx,
      int nx,
      DicomPixelBuffer pixels, int offset,
      DicomPixelInterpolator interp) {
      
      return pixels.setPixelsByte(x, dx, nx, this.pixels, offset, interp);
   }
   
   @Override
   public int setPixelsRGB(int x,
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelInterpolator interp) {
    
      int oidx = x;
      int iidx = offset;
      for (int i = 0; i < nx; i++) {
         //this.pixels[oidx] = (byte)((pixels[iidx++] + pixels[iidx++] + pixels[iidx++])/3); 
         iidx = interp.interpRGBByte(pixels, iidx, this.pixels, oidx);
         oidx += dx;
      }
      return iidx;
   }
   
   @Override
   public int setPixelsByte(int x, 
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelInterpolator interp) {
      
      int oidx = x;
      int iidx = offset;
      for (int i = 0; i < nx; i++) {
         iidx = interp.interpByteByte(pixels, iidx, this.pixels, oidx);
         oidx += dx;
      }
      return iidx;
   }
   
   @Override
   public int setPixelsShort(int x,
      int dx,
      int nx, short[] pixels, int offset,
      DicomPixelInterpolator interp) {
      
      int oidx = x;
      int iidx = offset;
      for (int i = 0; i < nx; i++) {
         //this.pixels[iidx] = (byte)(pixels[oidx++] >> 8);
         iidx = interp.interpShortByte(pixels, iidx, this.pixels, oidx);
         oidx += dx;
      }
      return iidx;
   }
   
   @Override
   public int setPixels(int x, 
      int dx,
      int nx,
      DicomPixelBuffer pixels, int offset,
      DicomPixelInterpolator interp) {
      
      return pixels.getPixelsByte(x, dx, nx, this.pixels, offset, interp);
   }
   
   @Override
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
   
   @Override
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
   
   @Override
   public byte[] getBuffer() {
      return pixels;
   }
   
}
