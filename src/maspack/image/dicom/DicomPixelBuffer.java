/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import java.nio.ByteBuffer;

/**
 * Stores a set of pixels, either as grayscale bytes, grayscale shorts, or RGB bytes.
 * @author Antonio
 *
 */
public interface DicomPixelBuffer {
   
   /**
    * Type of storage
    */
   public enum PixelType {
      BYTE, UBYTE, SHORT, USHORT, UBYTE_RGB
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
    * @return whether or not the representation is signed (otherwise unsigned)
    */
   boolean isSigned();
   
   /**
    * Populates a buffer of pixel values from those stored in this buffer,
    * using a supplied interpolator.  The output format matches the stored type.
    * Pixel values [x, x+dx, x+2*dx, ..., x+(nx-1)*dx] are copied into the output buffer.
    * 
    * @param x starting pixel index in this buffer
    * @param dx pixel step in this buffer
    * @param nx number of pixels to interpolate from
    * @param type type of pixels to output
    * @param interp interpolator for converting pixels for output display
    * @param pixels output buffer
    * @return the number of bytes written to the buffer
    */
   public int getPixels(int x, int dx, int nx, PixelType type, 
      DicomPixelInterpolator interp, ByteBuffer pixels);
   
   /**
    * Populates a buffer of pixel values from those stored in this buffer,
    * using a supplied interpolator.  The output format matches the stored type.
    * Pixel values [x, x+dx, x+2*dx, ..., x+(nx-1)*dx] are copied into the output buffer.
    * 
    * @param x starting pixel index in this buffer
    * @param dx pixel step in this buffer
    * @param nx number of pixels to interpolate from
    * @param type type of pixels to output
    * @param interp interpolator for converting pixels for output display
    * @return the next offset for writing to the buffer
    * @param pixels output array
    * @param offset offset in output array
    */
   public int getPixels(int x, int dx, int nx, PixelType type, 
      DicomPixelInterpolator interp, int[] pixels, int offset);
   
   /**
    * @return the maximum scaled pixel intensity, for use in auto-windowing
    */
   public double getMaxIntensity();
   
   /**
    * @return the minimum scaled pixel intensity, for use in auto-windowing
    */
   public double getMinIntensity();
   
   /**
    * @return the rescale slope
    */
   public double getRescaleSlope();
   
   /**
    * @return the rescale intercept
    */
   public double getRescaleIntercept();
   

   /**
    * Return the internal pixel value at x.  For RGB buffers, colors are interlaced, so
    * the ith pixel red value is actually at x=3*i
    * @param x value index
    * @return pixel value
    */
   public int getValue(int x);
   
   /**
    * Returns the raw rescaled value within the buffer at location x
    * @param x index within the buffer
    * @return rescaled value
    */
   public double getRescaledValue(int x);
   
   /**
    * Number of "color" channels in the buffer
    * @return number of channels
    */
   public abstract int getNumChannels();
   
   /**
    * @return the underlying buffer
    */
   public Object getBuffer();
   
}
