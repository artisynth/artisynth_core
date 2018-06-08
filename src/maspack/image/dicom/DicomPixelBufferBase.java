package maspack.image.dicom;

import java.nio.ByteBuffer;

public abstract class DicomPixelBufferBase implements DicomPixelBuffer {

   static final int BYTE_MASK = 0xFF;
   static final int UBYTE_MIN = 0;
   static final int UBYTE_MAX = 0xFF;
   static final int USHORT_MIN = 0;
   static final int USHORT_MAX = 0xFFFF;

   private PixelType pixelType;
   private double rescaleSlope;
   private double rescaleIntercept;
   
   protected DicomPixelBufferBase(PixelType type) {
      this.pixelType = type;
      this.rescaleSlope = 1.0;
      this.rescaleIntercept = 0;
   }
   
   public void setRescale(double slope, double intercept) {
      this.rescaleSlope = slope;
      this.rescaleIntercept = intercept;
   }

   @Override
   public double getRescaleSlope() {
      return rescaleSlope;
   }
   
   @Override
   public double getRescaleIntercept() {
      return rescaleIntercept;
   }
   
   @Override
   public PixelType getPixelType() {
      return pixelType;
   }

   @Override
   public boolean isSigned() {
      switch(pixelType) {
         case BYTE:
         case SHORT:
            return true;
         case UBYTE_RGB:
         case UBYTE:
         case USHORT:
         default:
      }
      return false;
   }

   /**
    * Return the internal pixel value at idx.  For RGB buffers, colors are interlaced, so
    * the ith pixel red value is actually at idx=3*i
    * @param idx value index
    * @return pixel value
    */
   public abstract int getValue(int idx);
   
   public double getRescaledValue(int idx) {
      return getValue(idx)*rescaleSlope + rescaleIntercept;
   }
   
   /**
    * Sets the internal pixel value at idx.  For RGB buffers, colors are interlaced, so
    * the ith pixel red value is actually at idx=3*i
    * @param idx value index
    * @param val value to set
    */
   protected abstract void setValue(int idx, int val);
   
   /**
    * @return number of values for getValue/setValue
    */
   protected abstract int getNumValues();

   @Override
   public int getPixels(
      int x, int dx, int nx, PixelType type,
      DicomPixelInterpolator interp,  ByteBuffer pixels) {

      int off = 0;
      switch (type) {
         case BYTE: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               int val = interp.interpGrayscale(getRescaledValue(iidx), 
                  Byte.MIN_VALUE, Byte.MAX_VALUE);
               pixels.put ((byte)val);
               iidx += dx;
            }
            off = nx;
            break;
         }
         case UBYTE: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               int val = interp.interpGrayscale(getRescaledValue(iidx), 0, UBYTE_MAX);
               pixels.put ((byte)val);
               iidx += dx;
            }
            off = nx;
            break;
         }
         case UBYTE_RGB: {
            int[] buff = new int[3];
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               interp.interpGrayscaleToRGB(getRescaledValue(iidx), 
                  0, UBYTE_MAX, buff);
               pixels.put ((byte)buff[0]);
               pixels.put ((byte)buff[1]);
               pixels.put ((byte)buff[2]);
               iidx += dx;
            }
            off = nx*3;
            break;
         }
         case SHORT: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               int val = interp.interpGrayscale(getRescaledValue(iidx), 
                  Short.MIN_VALUE, Short.MAX_VALUE);
               pixels.putShort ((short)val);
               iidx += dx;
            }
            off = nx*2;
            break;
         }
         case USHORT: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               int val = interp.interpGrayscale(getRescaledValue(iidx), 
                  USHORT_MIN, USHORT_MAX);
               pixels.putShort ((short)val);
               iidx += dx;
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
      switch (type) {
         case BYTE: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               int val = interp.interpGrayscale(getRescaledValue(iidx), 
                  Byte.MIN_VALUE, Byte.MAX_VALUE);
               pixels[off++] = val; 
               iidx += dx;
            }
            break;
         }
         case UBYTE: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               int val = interp.interpGrayscale(getRescaledValue(iidx), 
                  0, UBYTE_MAX);
               pixels[off++] = val;
               iidx += dx;
            }
            break;
         }
         case UBYTE_RGB: {
            int[] buff = new int[3];
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               interp.interpGrayscaleToRGB(getRescaledValue(iidx), 
                  0, UBYTE_MAX, buff);
               pixels[off++] = buff[0];
               pixels[off++] = buff[1];
               pixels[off++] = buff[2];
               iidx += dx;
            }
            break;
         }
         case SHORT: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               int val = interp.interpGrayscale(getRescaledValue(iidx), 
                  Short.MIN_VALUE, Short.MAX_VALUE);
               pixels[off++] = val;
               iidx += dx;
            }
            break;
         }
         case USHORT: {
            int iidx = x;
            for (int i=0; i<nx; ++i) {
               int val = interp.interpGrayscale(getRescaledValue(iidx), 
                  USHORT_MIN, USHORT_MAX);
               pixels[off++] = val;
               iidx += dx;
            }
            break;
         }
      }

      return off;
   }
   
   public double getMaxIntensity() {
      double max = Double.NEGATIVE_INFINITY;
      int size = getNumValues();
      for (int i=0; i<size; i++) {
         double val = getRescaledValue(i) ;
         if (val > max) {
            max = val;
         }
      }
      return max;
   }
   
   public double getMinIntensity() {
      double min = Double.POSITIVE_INFINITY;
      int size = getNumValues();
      for (int i=0; i<size; i++) {
         double val = getRescaledValue(i);
         if (val < min) {
            min = val;
         }
      }
      return min;
   }

}
