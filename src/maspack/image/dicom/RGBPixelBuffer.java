/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import java.nio.ByteBuffer;

/**
 * Stores a set of pixels in RGB byte form (3 consecutive bytes per pixel)
 * @author Antonio
 */
public class RGBPixelBuffer extends DicomPixelBufferBase {

   byte[] pixels;
   static final int BYTE_MASK = 0xFF;

   public RGBPixelBuffer(int size) {
      this(new byte[size]);
   }
   
   public RGBPixelBuffer(byte[] pixels) {
      super(PixelType.UBYTE_RGB);
      this.pixels = pixels;
   }
   
   /**
    * Creates a new buffer from a portion of an existing byte array
    * @param pixels buffer of pixels to copy from
    * @param offset offset in buffer to start copy
    * @param length total length of bytes (i.e. must be multiple of 3 for rgb)
    */
   public RGBPixelBuffer (byte[] pixels, int offset, int length) {
      super(PixelType.UBYTE_RGB);
      this.pixels = new byte[length];
      for (int i=0; i<length; ++i) {
         this.pixels[i] = pixels[offset+i];
      }
   }
   
   public RGBPixelBuffer (int[] pixels) {
      super(PixelType.UBYTE_RGB);
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
      return PixelType.UBYTE_RGB;
   }

   @Override
   protected int getNumValues() {
      return pixels.length;
   }
   
   @Override
   public int getNumPixels() {
      return pixels.length/3;
   }

   @Override
   public int getValue(int idx) {
      return pixels[idx];
   }
   
   @Override
   protected void setValue(int idx, int val) {
      pixels[idx] = (byte)val;
   }
   
   public byte[] getBuffer() {
      return pixels;
   }
   
   @Override
   public int getPixels(
      int x, int dx, int nx, PixelType type,
      DicomPixelInterpolator interp,  ByteBuffer pixels) {

      int off = 0;
      double[] rgb = new double[3];
      switch (type) {
         case BYTE: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               rgb[0] = getRescaledValue(iidx);
               rgb[1] = getRescaledValue(iidx+1);
               rgb[2] = getRescaledValue(iidx+2);
               int val = interp.interpRGBToGrayscale(rgb, 
                  Byte.MIN_VALUE, Byte.MAX_VALUE);
               pixels.put ((byte)val);
               iidx += 3*dx;
            }
            off = nx;
            break;
         }
         case UBYTE: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               rgb[0] = getRescaledValue(iidx);
               rgb[1] = getRescaledValue(iidx+1);
               rgb[2] = getRescaledValue(iidx+2);
               int val = interp.interpRGBToGrayscale(rgb, 0, UBYTE_MAX);
               pixels.put ((byte)val);
               iidx += 3*dx;
            }
            off = nx;
            break;
         }
         case UBYTE_RGB: {
            int[] buff = new int[3];
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               rgb[0] = getRescaledValue(iidx);
               rgb[1] = getRescaledValue(iidx+1);
               rgb[2] = getRescaledValue(iidx+2);
               interp.interpRGB(rgb, 
                  0, UBYTE_MAX, buff);
               pixels.put ((byte)buff[0]);
               pixels.put ((byte)buff[1]);
               pixels.put ((byte)buff[2]);
               iidx += 3*dx;
            }
            off = nx*3;
            break;
         }
         case SHORT: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               rgb[0] = getRescaledValue(iidx);
               rgb[1] = getRescaledValue(iidx+1);
               rgb[2] = getRescaledValue(iidx+2);
               int val = interp.interpRGBToGrayscale(rgb, 
                  Short.MIN_VALUE, Short.MAX_VALUE);
               pixels.putShort ((short)val);
               iidx += 3*dx;
            }
            off = nx*2;
            break;
         }
         case USHORT: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               rgb[0] = getRescaledValue(iidx);
               rgb[1] = getRescaledValue(iidx+1);
               rgb[2] = getRescaledValue(iidx+2);
               int val = interp.interpRGBToGrayscale(rgb, 
                  USHORT_MIN, USHORT_MAX);
               pixels.putShort ((short)val);
               iidx += 3*dx;
            }
            off = nx*2;
            break;
         }
      }

      return off;
   }
   
   @Override
   public int getPixels(
      int x, int dx, int nx, PixelType type,
      DicomPixelInterpolator interp, int[] pixels, int offset) {

      int off = offset;
      double [] rgb = new double[3];
      switch (type) {
         case BYTE: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               rgb[0] = getRescaledValue(iidx);
               rgb[1] = getRescaledValue(iidx+1);
               rgb[2] = getRescaledValue(iidx+2);
               int val = interp.interpRGBToGrayscale(rgb, 
                  Byte.MIN_VALUE, Byte.MAX_VALUE);
               pixels[off++] = val; 
               iidx += 3*dx;
            }
            break;
         }
         case UBYTE: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               rgb[0] = getRescaledValue(iidx);
               rgb[1] = getRescaledValue(iidx+1);
               rgb[2] = getRescaledValue(iidx+2);
               int val = interp.interpRGBToGrayscale(rgb, 
                  0, UBYTE_MAX);
               pixels[off++] = val;
               iidx += 3*dx;
            }
            break;
         }
         case UBYTE_RGB: {
            int[] buff = new int[3];
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               rgb[0] = getRescaledValue(iidx);
               rgb[1] = getRescaledValue(iidx+1);
               rgb[2] = getRescaledValue(iidx+2);
               interp.interpRGB(rgb, 0, UBYTE_MAX, buff);
               pixels[off++] = buff[0];
               pixels[off++] = buff[1];
               pixels[off++] = buff[2];
               iidx += 3*dx;
            }
            break;
         }
         case SHORT: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               rgb[0] = getRescaledValue(iidx);
               rgb[1] = getRescaledValue(iidx+1);
               rgb[2] = getRescaledValue(iidx+2);
               int val = interp.interpRGBToGrayscale(rgb, 
                  Short.MIN_VALUE, Short.MAX_VALUE);
               pixels[off++] = val;
               iidx += 3*dx;
            }
            break;
         }
         case USHORT: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               rgb[0] = getRescaledValue(iidx);
               rgb[1] = getRescaledValue(iidx+1);
               rgb[2] = getRescaledValue(iidx+2);
               int val = interp.interpRGBToGrayscale(rgb, 
                  USHORT_MIN, USHORT_MAX);
               pixels[off++] = val;
               iidx += 3*dx;
            }
            break;
         }
      }

      return off;
   }
   
   @Override
   public int getNumChannels () {
      return 3;
   }

}
