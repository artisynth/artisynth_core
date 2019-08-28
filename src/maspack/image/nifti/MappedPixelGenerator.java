package maspack.image.nifti;

import java.nio.ByteBuffer;

import maspack.image.nifti.NiftiHeader.DataType;
import maspack.properties.PropertyList;

public class MappedPixelGenerator extends NiftiPixelGenerator {
   
   static {
      NiftiPixelGenerator.registerSubclass(MappedPixelGenerator.class);
   }
   
   private static enum VoxelInput {
      COMPLEX_REAL,
      COMPLEX_IMAGINARY,
      RGB_R,
      RGB_G,
      RGB_B,
      VALUE
   }
   VoxelInput redInput;
   int redVidx;
   VoxelInput greenInput;
   int greenVidx;
   VoxelInput blueInput;
   int blueVidx;
   
   double windowMin;
   double windowMax;
   volatile int version;
   volatile boolean versionValid; 
   
   static PropertyList myProps = new PropertyList(MappedPixelGenerator.class, NiftiPixelGenerator.class);
   static {
      myProps.add("redInput", "data input type", VoxelInput.VALUE);
      myProps.add("redValueIndex", "data value index", 0);
      myProps.add("greenInput", "data input type", VoxelInput.VALUE);
      myProps.add("greenValueIndex", "data value index", 0);
      myProps.add("blueInput", "data input type", VoxelInput.VALUE);
      myProps.add("blueValueIndex", "data value index", 0);
      myProps.add("windowCenter", "center of window range", 0);
      myProps.add("windowWidth", "width of window range", 0);
   }
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public MappedPixelGenerator() {
      this(VoxelInput.VALUE, 0, VoxelInput.VALUE, 0, VoxelInput.VALUE, 0);
   }
   
   public MappedPixelGenerator(VoxelInput red, int redVidx, VoxelInput green, 
      int greenVidx, VoxelInput blue, int blueVidx) {
      this.redInput = red;
      this.redVidx = redVidx;
      this.greenInput = green;
      this.greenVidx = greenVidx;
      this.blueInput = blue;
      this.blueVidx = blueVidx;
      this.version = -1;
      this.versionValid = false;
   }
   
   @Override
   public int getVersion() {
      if (!versionValid) {
         ++version;
         versionValid = true;
      }
      return version;
   }
   
   protected void invalidateVersion() {
      versionValid = false;
   }
   
   public void setRedInput(VoxelInput input) {
      redInput = input;
      invalidateVersion();
   }
   
   public VoxelInput getRedInput() {
      return redInput;
   }
   
   public void setRedValueIndex(int idx) {
      redVidx = idx;
      invalidateVersion();
   }
   
   public int getRedValueIndex() {
      return redVidx;
   }
   
   public void setGreenInput(VoxelInput input) {
      greenInput = input;
      invalidateVersion();
   }
   
   public VoxelInput getGreenInput() {
      return greenInput;
   }
   
   public void setGreenValueIndex(int idx) {
      greenVidx = idx;
      invalidateVersion();
   }
   
   public int getGreenValueIndex() {
      return greenVidx;
   }
   
   public void setBlueInput(VoxelInput input) {
      blueInput = input;
      invalidateVersion();
   }
   
   public VoxelInput getBlueInput() {
      return blueInput;
   }
   
   public void setBlueValueIndex(int idx) {
      blueVidx = idx;
      invalidateVersion();
   }
   
   public int getBlueValueIndex() {
      return blueVidx;
   }
   
   public double getWindowCenter() {
      return (windowMax+windowMin)/2;
   }
   
   public void setWindowCenter(double c) {
      double width = getWindowWidth();
      windowMax = c+width/2;
      windowMin = c-width/2;
      invalidateVersion();
   }
   
   public double getWindowWidth() {
      return (windowMax-windowMin);
   }
   
   public void setWindowWidth(double w) {
      double c = getWindowCenter();
      windowMax = c + w/2;
      windowMin = c - w/2;
      invalidateVersion();
   }
   
   public void setWindow(double center, double width) {
      windowMin = center-width/2;
      windowMax = center+width/2;
      invalidateVersion();
   }
   
   public void setWindowLimits(double min, double max) {
      windowMin = min;
      windowMax = max;
      invalidateVersion();
   }
   
   public void setWindowToRange(double[] buff) {
      windowMin = Double.POSITIVE_INFINITY;
      windowMax = Double.NEGATIVE_INFINITY;
      for (double d : buff) {
         if (d < windowMin) {
            windowMin = d;
         }
         if (d > windowMax) {
            windowMax = d;
         }
      }
      invalidateVersion();
   }
   
   public void setWindowToRange(float[] buff) {
      windowMin = Double.POSITIVE_INFINITY;
      windowMax = Double.NEGATIVE_INFINITY;
      for (float d : buff) {
         if (d < windowMin) {
            windowMin = d;
         }
         if (d > windowMax) {
            windowMax = d;
         }
      }
      invalidateVersion();
   }
   
   public void setWindowToRange(byte[] buff) {
      windowMin = Double.POSITIVE_INFINITY;
      windowMax = Double.NEGATIVE_INFINITY;
      for (byte d : buff) {
         if (d < windowMin) {
            windowMin = d;
         }
         if (d > windowMax) {
            windowMax = d;
         }
      }
      invalidateVersion();
   }
   
   public void setWindowToRange(short[] buff) {
      windowMin = Double.POSITIVE_INFINITY;
      windowMax = Double.NEGATIVE_INFINITY;
      for (short d : buff) {
         if (d < windowMin) {
            windowMin = d;
         }
         if (d > windowMax) {
            windowMax = d;
         }
      }
      invalidateVersion();
   }
   
   public void setWindowToRange(int[] buff) {
      windowMin = Double.POSITIVE_INFINITY;
      windowMax = Double.NEGATIVE_INFINITY;
      for (int d : buff) {
         if (d < windowMin) {
            windowMin = d;
         }
         if (d > windowMax) {
            windowMax = d;
         }
      }
      invalidateVersion();
   }
   
   public void setWindowToRange(long[] buff) {
      windowMin = Double.POSITIVE_INFINITY;
      windowMax = Double.NEGATIVE_INFINITY;
      for (long d : buff) {
         if (d < windowMin) {
            windowMin = d;
         }
         if (d > windowMax) {
            windowMax = d;
         }
      }
      invalidateVersion();
   }
   
   private double toUnsigned(byte d) {
      if (d >= 0) {
         return d;
      }
      return ((double)Byte.MAX_VALUE)+(double)(-d);
   }
   
   private double toUnsigned(short d) {
      if (d >= 0) {
         return d;
      }
      return ((double)Short.MAX_VALUE)+(double)(-d);
   }
   
   private double toUnsigned(int d) {
      if (d >= 0) {
         return d;
      }
      return ((double)Integer.MAX_VALUE)+(double)(-d);
   }
   
   private double toUnsigned(long d) {
      if (d >= 0) {
         return d;
      }
      return ((double)Long.MAX_VALUE)+(double)(-d);
   }
   
   public void setWindowToUnsignedRange(byte[] buff) {
      windowMin = Double.POSITIVE_INFINITY;
      windowMax = Double.NEGATIVE_INFINITY;
      for (byte sd : buff) {
         double d = toUnsigned(sd);
         if (d < windowMin) {
            windowMin = d;
         }
         if (d > windowMax) {
            windowMax = d;
         }
      }
      invalidateVersion();
   }
   
   public void setWindowToUnsignedRange(short[] buff) {
      windowMin = Double.POSITIVE_INFINITY;
      windowMax = Double.NEGATIVE_INFINITY;
      for (short sd : buff) {
         double d = toUnsigned(sd);
         if (d < windowMin) {
            windowMin = d;
         }
         if (d > windowMax) {
            windowMax = d;
         }
      }
      invalidateVersion();
   }
   
   public void setWindowToUnsignedRange(int[] buff) {
      windowMin = Double.POSITIVE_INFINITY;
      windowMax = Double.NEGATIVE_INFINITY;
      for (int sd : buff) {
         double d = toUnsigned(sd);
         if (d < windowMin) {
            windowMin = d;
         }
         if (d > windowMax) {
            windowMax = d;
         }
      }
      invalidateVersion();
   }
   
   public void setWindowToUnsignedRange(long[] buff) {
      windowMin = Double.POSITIVE_INFINITY;
      windowMax = Double.NEGATIVE_INFINITY;
      for (long sd : buff) {
         double d = toUnsigned(sd);
         if (d < windowMin) {
            windowMin = d;
         }
         if (d > windowMax) {
            windowMax = d;
         }
      }
      invalidateVersion();
   }
   
   public void detectDefault(NiftiImage image) {
      detectDefault(image.buff);
   }
   
   public void detectDefault(NiftiDataBuffer buff) {
      DataType type = buff.getDataType();
      int nv = buff.getNumValuesPerVoxel();
      
      redInput = VoxelInput.VALUE;
      redVidx = 0;
      greenInput = VoxelInput.VALUE;
      greenVidx = 0;
      blueInput = VoxelInput.VALUE;
      blueVidx = 0;
      if (nv == 2) {
         greenVidx = 1;
         blueVidx = -1;
      } else if (nv >= 3) {
         greenVidx = 1;
         blueVidx = 2;
      }
      
      switch (type) {
         case DT_BINARY:
            windowMin = 0;
            windowMax = 1;
            break;
         case NIFTI_TYPE_COMPLEX128:
         case NIFTI_TYPE_COMPLEX256:
            redInput = VoxelInput.COMPLEX_REAL;
            greenInput = VoxelInput.COMPLEX_IMAGINARY;
            greenVidx = 0;
            blueVidx = -1;
            setWindowToRange(buff.doubles);
            break;
         case NIFTI_TYPE_COMPLEX64:
            redInput = VoxelInput.COMPLEX_REAL;
            greenInput = VoxelInput.COMPLEX_IMAGINARY;
            greenVidx = 0;
            blueVidx = -1;
            setWindowToRange(buff.floats);
            break;
         case NIFTI_TYPE_FLOAT128:
         case NIFTI_TYPE_FLOAT64:
            setWindowToRange(buff.doubles);
            break;
         case NIFTI_TYPE_FLOAT32:
            setWindowToRange(buff.floats);
            break;
         case NIFTI_TYPE_INT16:
            setWindowToRange(buff.shorts);
            break;
         case NIFTI_TYPE_INT32:
            setWindowToRange(buff.ints);
            break;
         case NIFTI_TYPE_INT64:
            setWindowToRange(buff.longs);
            break;
         case NIFTI_TYPE_INT8:
            setWindowToRange(buff.bytes);
            break;
         case NIFTI_TYPE_RGB24:
            // revert to showing only first value
            greenVidx = 0;
            blueVidx = 0;
            setWindowToRange(buff.bytes);
            break;
         case NIFTI_TYPE_UINT16:
            setWindowToUnsignedRange(buff.shorts);
            break;
         case NIFTI_TYPE_UINT32:
            setWindowToUnsignedRange(buff.ints);
            break;
         case NIFTI_TYPE_UINT64:
            setWindowToUnsignedRange(buff.longs);
            break;
         case NIFTI_TYPE_UINT8:
            setWindowToUnsignedRange(buff.bytes);
            break;
         default:
         case DT_UNKNOWN:
            redVidx = -1;
            greenVidx = -1;
            blueVidx = -1;
            break;
      }
      invalidateVersion();
   }
   
   private byte interp(double x) {
      if (x < windowMin) {
         return 0;
      } else if (x > windowMax) {
         return (byte)(-1);
      }
      return (byte)((x-windowMin)/(windowMax-windowMin)*255);
   }
   
   private void getRGBBinary(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = rgb.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = rgb.position();
            for (int ii=0; ii<ni; ++ii) {
               if ((redVidx >= 0) && (redVidx <= buff.getNumValuesPerVoxel())) {
                  int ridx = buff.getIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  boolean rb = buff.bools[ridx];
                  double r = rb ? 1 : 0;
                  rgb.put(interp(r));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((greenVidx >= 0) && (greenVidx <= buff.getNumValuesPerVoxel())) {
                  int gidx = buff.getIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  boolean gb = buff.bools[gidx];
                  double g = gb ? 1 : 0;
                  rgb.put(interp(g));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((blueVidx >= 0) && (blueVidx <= buff.getNumValuesPerVoxel())) {
                  int bidx = buff.getIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  boolean bb = buff.bools[bidx];
                  double b = bb ? 1 : 0;
                  rgb.put(interp(b));
               } else {
                  rgb.put((byte)0);
               }
            }
            if (scanline > 0) {
               rgb.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            rgb.position(kpos + pageline);
         }
      }
   }
   
   private void getRGBComplex128(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = rgb.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = rgb.position();
            for (int ii=0; ii<ni; ++ii) {
               if (((redVidx >= 0) && (redVidx <= buff.getNumValuesPerVoxel())) && (redVidx <= buff.getNumValuesPerVoxel())) {
                  int ridx = -1;  
                  if (redInput == VoxelInput.COMPLEX_IMAGINARY) {
                     ridx = buff.getImaginaryIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else {
                     ridx = buff.getRealIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  }
                  double r = buff.doubles[ridx];
                  rgb.put(interp(r));
               } else {
                  rgb.put((byte)0);
               }
               
               if (((greenVidx >= 0) && (greenVidx <= buff.getNumValuesPerVoxel())) && (greenVidx <= buff.getNumValuesPerVoxel())) {
                  int gidx = -1;  
                  if (greenInput == VoxelInput.COMPLEX_IMAGINARY) {
                     gidx = buff.getImaginaryIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else {
                     gidx = buff.getRealIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  }
                  double g = buff.doubles[gidx];
                  rgb.put(interp(g));
               } else {
                  rgb.put((byte)0);
               }
               
               if (((blueVidx >= 0) && (blueVidx <= buff.getNumValuesPerVoxel())) && (blueVidx <= buff.getNumValuesPerVoxel())) {
                  int bidx = -1;  
                  if (blueInput == VoxelInput.COMPLEX_IMAGINARY) {
                     bidx = buff.getImaginaryIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else {
                     bidx = buff.getRealIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  }
                  double b = buff.doubles[bidx];
                  rgb.put(interp(b));
               } else {
                  rgb.put((byte)0);
               }
            }
            if (scanline > 0) {
               rgb.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            rgb.position(kpos + pageline);
         }
      }
   }
   
   private void getRGBComplex64(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = rgb.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = rgb.position();
            for (int ii=0; ii<ni; ++ii) {
               if (((redVidx >= 0) && (redVidx <= buff.getNumValuesPerVoxel())) && (redVidx <= buff.getNumValuesPerVoxel())) {
                  int ridx = -1;  
                  if (redInput == VoxelInput.COMPLEX_IMAGINARY) {
                     ridx = buff.getImaginaryIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else {
                     ridx = buff.getRealIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  }
                  double r = buff.floats[ridx];
                  rgb.put(interp(r));
               } else {
                  rgb.put((byte)0);
               }
               
               if (((greenVidx >= 0) && (greenVidx <= buff.getNumValuesPerVoxel())) && (greenVidx <= buff.getNumValuesPerVoxel())) {
                  int gidx = -1;  
                  if (greenInput == VoxelInput.COMPLEX_IMAGINARY) {
                     gidx = buff.getImaginaryIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else {
                     gidx = buff.getRealIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  }
                  double g = buff.floats[gidx];
                  rgb.put(interp(g));
               } else {
                  rgb.put((byte)0);
               }
               
               if (((blueVidx >= 0) && (blueVidx <= buff.getNumValuesPerVoxel())) && (blueVidx <= buff.getNumValuesPerVoxel())) {
                  int bidx = -1;  
                  if (blueInput == VoxelInput.COMPLEX_IMAGINARY) {
                     bidx = buff.getImaginaryIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else {
                     bidx = buff.getRealIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  }
                  double b = buff.floats[bidx];
                  rgb.put(interp(b));
               } else {
                  rgb.put((byte)0);
               }
            }
            if (scanline > 0) {
               rgb.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            rgb.position(kpos + pageline);
         }
      }
   }
   
   private void getRGBFloat64(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = rgb.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = rgb.position();
            for (int ii=0; ii<ni; ++ii) {
               if (((redVidx >= 0) && (redVidx <= buff.getNumValuesPerVoxel())) && (redVidx <= buff.getNumValuesPerVoxel())) {
                  int ridx = buff.getIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double r = buff.doubles[ridx];
                  rgb.put(interp(r));
               } else {
                  rgb.put((byte)0);
               }
               
               if (((greenVidx >= 0) && (greenVidx <= buff.getNumValuesPerVoxel())) && (greenVidx <= buff.getNumValuesPerVoxel())) {
                  int gidx = buff.getIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double g = buff.doubles[gidx];
                  rgb.put(interp(g));
               } else {
                  rgb.put((byte)0);
               }
               
               if (((blueVidx >= 0) && (blueVidx <= buff.getNumValuesPerVoxel())) && (blueVidx <= buff.getNumValuesPerVoxel())) {
                  int bidx = buff.getIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double b = buff.doubles[bidx];
                  rgb.put(interp(b));
               } else {
                  rgb.put((byte)0);
               }
            }
            if (scanline > 0) {
               rgb.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            rgb.position(kpos + pageline);
         }
      }
   }
   
   private void getRGBFloat32(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = rgb.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = rgb.position();
            for (int ii=0; ii<ni; ++ii) {
               if (((redVidx >= 0) && (redVidx <= buff.getNumValuesPerVoxel())) && (redVidx <= buff.getNumValuesPerVoxel())) {
                  int ridx = buff.getIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double r = buff.floats[ridx];
                  rgb.put(interp(r));
               } else {
                  rgb.put((byte)0);
               }
               
               if (((greenVidx >= 0) && (greenVidx <= buff.getNumValuesPerVoxel())) && (greenVidx <= buff.getNumValuesPerVoxel())) {
                  int gidx = buff.getIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double g = buff.floats[gidx];
                  rgb.put(interp(g));
               } else {
                  rgb.put((byte)0);
               }
               
               if (((blueVidx >= 0) && (blueVidx <= buff.getNumValuesPerVoxel())) && (blueVidx <= buff.getNumValuesPerVoxel())) {
                  int bidx = buff.getIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double b = buff.floats[bidx];
                  rgb.put(interp(b));
               } else {
                  rgb.put((byte)0);
               }
            }
            if (scanline > 0) {
               rgb.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            rgb.position(kpos + pageline);
         }
      }
   }
   
   private void getRGBInt64(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = rgb.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = rgb.position();
            for (int ii=0; ii<ni; ++ii) {
               if ((redVidx >= 0) && (redVidx <= buff.getNumValuesPerVoxel())) {
                  int ridx = buff.getIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double r = buff.longs[ridx];
                  rgb.put(interp(r));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((greenVidx >= 0) && (greenVidx <= buff.getNumValuesPerVoxel())) {
                  int gidx = buff.getIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double g = buff.longs[gidx];
                  rgb.put(interp(g));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((blueVidx >= 0) && (blueVidx <= buff.getNumValuesPerVoxel())) {
                  int bidx = buff.getIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double b = buff.longs[bidx];
                  rgb.put(interp(b));
               } else {
                  rgb.put((byte)0);
               }
            }
            if (scanline > 0) {
               rgb.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            rgb.position(kpos + pageline);
         }
      }
   }
   
   private void getRGBUInt64(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = rgb.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = rgb.position();
            for (int ii=0; ii<ni; ++ii) {
               if ((redVidx >= 0) && (redVidx <= buff.getNumValuesPerVoxel())) {
                  int ridx = buff.getIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double r = toUnsigned(buff.longs[ridx]);
                  rgb.put(interp(r));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((greenVidx >= 0) && (greenVidx <= buff.getNumValuesPerVoxel())) {
                  int gidx = buff.getIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double g = toUnsigned(buff.longs[gidx]);
                  rgb.put(interp(g));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((blueVidx >= 0) && (blueVidx <= buff.getNumValuesPerVoxel())) {
                  int bidx = buff.getIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double b = toUnsigned(buff.longs[bidx]);
                  rgb.put(interp(b));
               } else {
                  rgb.put((byte)0);
               }
            }
            if (scanline > 0) {
               rgb.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            rgb.position(kpos + pageline);
         }
      }
   }
   
   private void getRGBInt32(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = rgb.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = rgb.position();
            for (int ii=0; ii<ni; ++ii) {
               if ((redVidx >= 0) && (redVidx <= buff.getNumValuesPerVoxel())) {
                  int ridx = buff.getIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double r = buff.ints[ridx];
                  rgb.put(interp(r));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((greenVidx >= 0) && (greenVidx <= buff.getNumValuesPerVoxel())) {
                  int gidx = buff.getIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double g = buff.ints[gidx];
                  rgb.put(interp(g));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((blueVidx >= 0) && (blueVidx <= buff.getNumValuesPerVoxel())) {
                  int bidx = buff.getIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double b = buff.ints[bidx];
                  rgb.put(interp(b));
               } else {
                  rgb.put((byte)0);
               }
            }
            if (scanline > 0) {
               rgb.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            rgb.position(kpos + pageline);
         }
      }
   }
   
   private void getRGBUInt32(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = rgb.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = rgb.position();
            for (int ii=0; ii<ni; ++ii) {
               if ((redVidx >= 0) && (redVidx <= buff.getNumValuesPerVoxel())) {
                  int ridx = buff.getIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double r = toUnsigned(buff.ints[ridx]);
                  rgb.put(interp(r));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((greenVidx >= 0) && (greenVidx <= buff.getNumValuesPerVoxel())) {
                  int gidx = buff.getIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double g = toUnsigned(buff.ints[gidx]);
                  rgb.put(interp(g));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((blueVidx >= 0) && (blueVidx <= buff.getNumValuesPerVoxel())) {
                  int bidx = buff.getIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double b = toUnsigned(buff.ints[bidx]);
                  rgb.put(interp(b));
               } else {
                  rgb.put((byte)0);
               }
            }
            if (scanline > 0) {
               rgb.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            rgb.position(kpos + pageline);
         }
      }
   }
   
   private void getRGBInt16(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = rgb.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = rgb.position();
            for (int ii=0; ii<ni; ++ii) {
               if ((redVidx >= 0) && (redVidx <= buff.getNumValuesPerVoxel())) {
                  int ridx = buff.getIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double r = buff.shorts[ridx];
                  rgb.put(interp(r));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((greenVidx >= 0) && (greenVidx <= buff.getNumValuesPerVoxel())) {
                  int gidx = buff.getIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double g = buff.shorts[gidx];
                  rgb.put(interp(g));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((blueVidx >= 0) && (blueVidx <= buff.getNumValuesPerVoxel())) {
                  int bidx = buff.getIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double b = buff.shorts[bidx];
                  rgb.put(interp(b));
               } else {
                  rgb.put((byte)0);
               }
            }
            if (scanline > 0) {
               rgb.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            rgb.position(kpos + pageline);
         }
      }
   }
   
   private void getRGBUInt16(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = rgb.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = rgb.position();
            for (int ii=0; ii<ni; ++ii) {
               if ((redVidx >= 0) && (redVidx <= buff.getNumValuesPerVoxel())) {
                  int ridx = buff.getIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double r = toUnsigned(buff.shorts[ridx]);
                  rgb.put(interp(r));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((greenVidx >= 0) && (greenVidx <= buff.getNumValuesPerVoxel())) {
                  int gidx = buff.getIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double g = toUnsigned(buff.shorts[gidx]);
                  rgb.put(interp(g));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((blueVidx >= 0) && (blueVidx <= buff.getNumValuesPerVoxel())) {
                  int bidx = buff.getIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double b = toUnsigned(buff.shorts[bidx]);
                  rgb.put(interp(b));
               } else {
                  rgb.put((byte)0);
               }
            }
            if (scanline > 0) {
               rgb.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            rgb.position(kpos + pageline);
         }
      }
   }
   
   private void getRGBInt8(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = rgb.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = rgb.position();
            for (int ii=0; ii<ni; ++ii) {
               if ((redVidx >= 0) && (redVidx <= buff.getNumValuesPerVoxel())) {
                  int ridx = buff.getIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double r = buff.bytes[ridx];
                  rgb.put(interp(r));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((greenVidx >= 0) && (greenVidx <= buff.getNumValuesPerVoxel())) {
                  int gidx = buff.getIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double g = buff.bytes[gidx];
                  rgb.put(interp(g));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((blueVidx >= 0) && (blueVidx <= buff.getNumValuesPerVoxel())) {
                  int bidx = buff.getIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double b = buff.bytes[bidx];
                  rgb.put(interp(b));
               } else {
                  rgb.put((byte)0);
               }
            }
            if (scanline > 0) {
               rgb.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            rgb.position(kpos + pageline);
         }
      }
   }
   
   private void getRGBUInt8(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = rgb.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = rgb.position();
            for (int ii=0; ii<ni; ++ii) {
               if ((redVidx >= 0) && (redVidx <= buff.getNumValuesPerVoxel())) {
                  int ridx = buff.getIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double r = toUnsigned(buff.bytes[ridx]);
                  rgb.put(interp(r));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((greenVidx >= 0) && (greenVidx <= buff.getNumValuesPerVoxel())) {
                  int gidx = buff.getIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double g = toUnsigned(buff.bytes[gidx]);
                  rgb.put(interp(g));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((blueVidx >= 0) && (blueVidx <= buff.getNumValuesPerVoxel())) {
                  int bidx = buff.getIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  double b = toUnsigned(buff.bytes[bidx]);
                  rgb.put(interp(b));
               } else {
                  rgb.put((byte)0);
               }
            }
            if (scanline > 0) {
               rgb.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            rgb.position(kpos + pageline);
         }
      }
   }
   
   private void getRGBRGB(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = rgb.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = rgb.position();
            for (int ii=0; ii<ni; ++ii) {
               if ((redVidx >= 0) && (redVidx <= buff.getNumValuesPerVoxel())) {
                  int ridx = -1;
                  if (redInput == VoxelInput.RGB_R) {
                     ridx = buff.getRedIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else if (redInput == VoxelInput.RGB_G) {
                     ridx = buff.getGreenIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else if (redInput == VoxelInput.RGB_B) {
                     ridx = buff.getBlueIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else {
                     // default to red
                     ridx = buff.getRedIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  }
                  double r = buff.bytes[ridx];
                  rgb.put(interp(r));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((greenVidx >= 0) && (greenVidx <= buff.getNumValuesPerVoxel())) {
                  int gidx = -1;
                  if (greenInput == VoxelInput.RGB_R) {
                     gidx = buff.getRedIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else if (redInput == VoxelInput.RGB_G) {
                     gidx = buff.getGreenIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else if (redInput == VoxelInput.RGB_B) {
                     gidx = buff.getBlueIndex(greenVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else {
                     // default to green index
                     gidx = buff.getGreenIndex(redVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  }
                  double g = buff.bytes[gidx];
                  rgb.put(interp(g));
               } else {
                  rgb.put((byte)0);
               }
               
               if ((blueVidx >= 0) && (blueVidx <= buff.getNumValuesPerVoxel())) {
                  int bidx = -1;
                  if (blueInput == VoxelInput.RGB_R) {
                     bidx = buff.getRedIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else if (blueInput == VoxelInput.RGB_G) {
                     bidx = buff.getGreenIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else if (blueInput == VoxelInput.RGB_B) {
                     bidx = buff.getBlueIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  } else {
                     // default to blue index
                     bidx = buff.getBlueIndex(blueVidx, i+ii*di, j+jj*dj, k+kk*dk, t);
                  }
                  double b = buff.bytes[bidx];
                  rgb.put(interp(b));
               } else {
                  rgb.put((byte)0);
               }
            }
            if (scanline > 0) {
               rgb.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            rgb.position(kpos + pageline);
         }
      }
   }
  
   @Override
   public void getPixels(
      NiftiDataBuffer buff, 
      int i, int di, int ni, int j, int dj, int nj, int k,
      int dk, int nk, int t, 
      int scanline, int pageline, ByteBuffer rgb) {

      switch(buff.getDataType()) {
         case DT_BINARY:
            getRGBBinary(buff, i, di, ni, j, dj, nj, k, nk, dk, t, scanline, pageline, rgb);
            break;
         case NIFTI_TYPE_COMPLEX128:
         case NIFTI_TYPE_COMPLEX256:
            getRGBComplex128(buff, i, di, ni, j, dj, nj, k, dk, nk, t, scanline, pageline, rgb);
            break;
         case NIFTI_TYPE_COMPLEX64:
            getRGBComplex64(buff, i, di, ni, j, dj, nj, k, dk, nk, t, scanline, pageline, rgb);
            break;
         case NIFTI_TYPE_FLOAT64:
         case NIFTI_TYPE_FLOAT128:
            getRGBFloat64(buff, i, di, ni, j, dj, nj, k, dk, nk, t, scanline, pageline, rgb);
            break;
         case NIFTI_TYPE_FLOAT32:
            getRGBFloat32(buff, i, di, ni, j, dj, nj, k, dk, nk, t, scanline, pageline, rgb);
            break;
         case NIFTI_TYPE_INT16:
            getRGBInt16(buff, i, di, ni, j, dj, nj, k, dk, nk, t, scanline, pageline, rgb);
            break;
         case NIFTI_TYPE_INT32:
            getRGBInt32(buff, i, di, ni, j, dj, nj, k, dk, nk, t, scanline, pageline, rgb);
            break;
         case NIFTI_TYPE_INT64:
            getRGBInt64(buff, i, di, ni, j, dj, nj, k, dk, nk, t, scanline, pageline, rgb);
            break;
         case NIFTI_TYPE_INT8:
            getRGBInt8(buff, i, di, ni, j, dj, nj, k, dk, nk, t, scanline, pageline, rgb);
            break;
         case NIFTI_TYPE_RGB24:
            getRGBRGB(buff, i, di, ni, j, dj, nj, k, dk, nk, t, scanline, pageline, rgb);
            break;
         case NIFTI_TYPE_UINT16:
            getRGBUInt16(buff, i, di, ni, j, dj, nj, k, dk, nk, t, scanline, pageline, rgb);
            break;
         case NIFTI_TYPE_UINT32:
            getRGBUInt32(buff, i, di, ni, j, dj, nj, k, dk, nk, t, scanline, pageline, rgb);
            break;
         case NIFTI_TYPE_UINT64:
            getRGBUInt64(buff, i, di, ni, j, dj, nj, k, dk, nk, t, scanline, pageline, rgb);
            break;
         case NIFTI_TYPE_UINT8:
            getRGBUInt8(buff, i, di, ni, j, dj, nj, k, dk, nk, t, scanline, pageline, rgb);
            break;
         case DT_UNKNOWN:
         default:
            for (int kk=0; kk<nk; ++kk) {
               int kpos = rgb.position();
               for (int jj=0; jj<nj; ++jj) {
                  int jpos = rgb.position();
                  for (int ii=0; ii<ni; ++ii) {
                    rgb.put((byte)0);
                    rgb.put((byte)0);
                    rgb.put((byte)0);
                  }
                  if (scanline > 0) {
                     rgb.position(jpos + scanline);
                  }
               }
               if (pageline > 0) {
                  rgb.position(kpos + pageline);
               }
            }
            break;
         
      }
   }
   
   @Override
   public MappedPixelGenerator clone() {
      return (MappedPixelGenerator)super.clone();
   }

   @Override
   public Format getFormat() {
      return Format.RGB;
   }
   
}
