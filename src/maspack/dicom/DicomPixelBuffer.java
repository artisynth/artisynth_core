/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.dicom;

import java.nio.ByteBuffer;

/**
 * Stores a set of pixels, either as grayscale bytes, grayscale shorts,
 * or RGB bytes.
 * @author Antonio
 *
 */
public interface DicomPixelBuffer {
   
   /**
    * Type of storage
    */
   public enum PixelType {
      BYTE, SHORT, BYTE_RGB
   }
   
   /**
    * @return the type of buffer
    */
   PixelType getPixelType();
   
   /**
    * @return number of pixels in the buffer
    */
   int getNumPixels();
   
   /**
    * @param n the pixel index
    * @return a representation of the <code>n</code>th pixel
    */
   Object getPixel(int n);
   
   /**
    * Populates a buffer of pixel values from those stored in this buffer,
    * using a supplied interpolator.  The output format matches the stored type.
    * Pixel values [x, x+dx, x+2*dx, ..., x+(nx-1)*dx] are copied into the output buffer.
    * 
    * @param x starting pixel index in this buffer
    * @param dx pixel step in this buffer
    * @param nx number of pixels to interpolate from
    * @param type type of pixels to output
    * @param pixels output array
    * @param interp interpolator for converting pixels for output display
    * @return the number of bytes written to the buffer
    */
   public int getPixels(int x, int dx, int nx, PixelType type, ByteBuffer pixels, 
      DicomPixelInterpolator interp);
   
   /**
    * Populates an array of RGB pixel values from pixel values stored in this buffer,
    * using a supplied interpolator.
    * Pixel values [x, x+dx, x+2*dx, ..., x+(nx-1)*dx] are copied into the output buffer.
    * 
    * 
    * @param x starting pixel index in this buffer
    * @param dx pixel step in this buffer
    * @param nx number of pixels to interpolate from
    * @param pixels output array of RGB values
    * @param offset offset in output array to fill
    * @param interp interpolator for converting pixels for output display
    * @return the next index in the output pixels buffer to fill
    */
   public int getPixelsRGB(int x,
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelInterpolator interp);
   
   /**
    * Populates an array of grayscale pixel values from pixel values stored in this buffer,
    * using a supplied interpolator.
    * Pixel values [x, x+dx, x+2*dx, ..., x+(nx-1)*dx] are copied into the output buffer.
    * 
    * 
    * @param x starting pixel index in this buffer
    * @param dx pixel step in this buffer
    * @param nx number of pixels to interpolate from
    * @param pixels output array of grayscale values
    * @param offset offset in output array to fill
    * @param interp interpolator for converting pixels for output display
    * @return the next index in the output pixels buffer to fill
    */
   public int getPixelsByte(int x, 
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelInterpolator interp);
   
   /**
    * Populates an array of grayscale pixel values from pixel values stored in this buffer,
    * using a supplied interpolator.
    * Pixel values [x, x+dx, x+2*dx, ..., x+(nx-1)*dx] are copied into the output buffer.
    * 
    * 
    * @param x starting pixel index in this buffer
    * @param dx pixel step in this buffer
    * @param nx number of pixels to interpolate from
    * @param pixels output array of grayscale values
    * @param offset offset in output array to fill
    * @param interp interpolator for converting pixels for output display
    * @return the next index in the output pixels buffer to fill
    */
   public int getPixelsShort(int x,
      int dx,
      int nx, short[] pixels, int offset,
      DicomPixelInterpolator interp);
   
   /**
    * Populates a new pixel buffer from pixel values stored in this buffer,
    * using a supplied interpolator.
    * Pixel values [x, x+dx, x+2*dx, ..., x+(nx-1)*dx] are copied into the output buffer.
    * 
    * 
    * @param x starting pixel index in this buffer
    * @param dx pixel step in this buffer
    * @param nx number of pixels to interpolate from
    * @param pixels output buffer (buffer class determines type)
    * @param offset offset in output array to fill
    * @param interp interpolator for converting pixels for output display
    * @return the next index in the output pixels buffer to fill
    */
   public int getPixels(int x, 
      int dx,
      int nx,
      DicomPixelBuffer pixels, int offset,
      DicomPixelInterpolator interp);
   
   /**
    * Populates this pixel buffer using an array of RGB pixel values and a supplied interpolator.
    * Pixel values [x, x+dx, x+2*dx, ..., x+(nx-1)*dx] are set from the input array.
    * 
    * @param x starting pixel index in this buffer
    * @param dx pixel step in this buffer
    * @param nx number of pixels to interpolate to
    * @param pixels input array of RGB values
    * @param offset offset in input array
    * @param interp interpolator for converting pixels to the appropriate format
    * @return the next index in the input pixels buffer to read from
    */
   public int setPixelsRGB(int x,
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelInterpolator interp);
   
   /**
    * Populates this pixel buffer using an array of grayscale pixel values and a supplied interpolator.
    * Pixel values [x, x+dx, x+2*dx, ..., x+(nx-1)*dx] are set from the input array.
    * 
    * @param x starting pixel index in this buffer
    * @param dx pixel step in this buffer
    * @param nx number of pixels to interpolate to
    * @param pixels input array of grayscale values
    * @param offset offset in input array
    * @param interp interpolator for converting pixels to the appropriate format
    * @return the next index in the input pixels buffer to read from
    */
   public int setPixelsByte(int x, 
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelInterpolator interp);
   
   /**
    * Populates this pixel buffer using an array of grayscale pixel values and a supplied interpolator.
    * Pixel values [x, x+dx, x+2*dx, ..., x+(nx-1)*dx] are set from the input array.
    * 
    * @param x starting pixel index in this buffer
    * @param dx pixel step in this buffer
    * @param nx number of pixels to interpolate to
    * @param pixels input array of grayscale values
    * @param offset offset in input array
    * @param interp interpolator for converting pixels to the appropriate format
    * @return the next index in the input pixels buffer to read from
    */
   public int setPixelsShort(int x,
      int dx,
      int nx, short[] pixels, int offset,
      DicomPixelInterpolator interp);
   
   /**
    * Populates this pixel buffer using a supplied pixel buffer and interpolator.
    * Pixel values [x, x+dx, x+2*dx, ..., x+(nx-1)*dx] are set from the input buffer.
    * 
    * @param x starting pixel index in this buffer
    * @param dx pixel step in this buffer
    * @param nx number of pixels to interpolate to
    * @param pixels input buffer (class determines type)
    * @param offset offset in input buffer
    * @param interp interpolator for converting pixels to the appropriate format
    * @return the next index in the input pixels buffer to read from
    */
   public int setPixels(int x, 
      int dx,
      int nx,
      DicomPixelBuffer pixels, int offset,
      DicomPixelInterpolator interp);
   
   /**
    * @return the maximum pixel intensity, for use in auto-windowing
    */
   public int getMaxIntensity();
   
   /**
    * @return the minimum pixel intensity, for use in auto-windowing
    */
   public int getMinIntensity();
   
   /**
    * @return the underlying buffer
    */
   public Object getBuffer();
   
}
