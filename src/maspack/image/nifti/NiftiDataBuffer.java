package maspack.image.nifti;

import java.util.Arrays;

import maspack.image.nifti.NiftiHeader.DataType;
import maspack.util.Clonable;

public class NiftiDataBuffer implements Clonable {

   boolean[] bools;
   byte[] bytes;
   short[] shorts;
   int[] ints;
   long[] longs;
   float[] floats;
   double[] doubles;
   Object buff;
   
   int vstep;
   int istep;
   int jstep;
   int kstep;
   int tstep;

   // always 5 dimensional
   DataType dataType;
   boolean complex;
   boolean signed;
   int dims[];

   public NiftiDataBuffer(DataType type, int[] dims) {
      int size = dims[0]*dims[1]*dims[2]*dims[3]*dims[4];  // x,y,z,t,v
      this.dataType = type;
      this.dims = new int[5];
      for (int i=0; i<5; ++i) {
         this.dims[i] = dims[i];
      }

      bools = null;
      bytes = null;
      shorts = null;
      ints = null;
      longs = null;
      floats = null;
      doubles = null;
      buff = null;

      signed = true;
      complex = false;
      
      vstep = 1;
      
      switch(type) {
         case DT_BINARY:
            bools = new boolean[size];
            buff = bools;
            signed = false;
            break;
         case NIFTI_TYPE_COMPLEX128:
         case NIFTI_TYPE_COMPLEX256:
            vstep = 2;
            doubles = new double[size*2];
            buff = doubles;
            complex = true;
            break;
         case NIFTI_TYPE_COMPLEX64:
            vstep = 2;
            floats = new float[size*2];
            buff = floats;
            complex = true;
            break;
         case NIFTI_TYPE_FLOAT128:
         case NIFTI_TYPE_FLOAT64:
            doubles = new double[size];
            buff = doubles;
            break;
         case NIFTI_TYPE_FLOAT32:
            floats = new float[size];
            buff = floats;
            break;
         case NIFTI_TYPE_INT16:
            shorts = new short[size];
            buff = shorts;
            break;
         case NIFTI_TYPE_INT32:
            ints = new int[size];
            buff = ints;
            break;
         case NIFTI_TYPE_INT64:
            longs = new long[size];
            buff = longs;
            break;
         case NIFTI_TYPE_INT8:
            bytes = new byte[size];
            buff = bytes;
            break;
         case NIFTI_TYPE_RGB24:
            vstep = 3;
            bytes = new byte[3*size];
            buff = bytes;
            break;
         case NIFTI_TYPE_UINT16:
            signed = false;
            shorts = new short[size];
            buff = shorts;
            break;
         case NIFTI_TYPE_UINT32:
            signed = false;
            ints = new int[size];
            buff = ints;
            break;
         case NIFTI_TYPE_UINT64:
            signed = false;
            longs = new long[size];
            buff = longs;
            break;
         case NIFTI_TYPE_UINT8:
            signed = false;
            bytes = new byte[size];
            buff = bytes;
            break;
         default:
         case DT_UNKNOWN:
            break;
      }
      
      istep = vstep*dims[4];
      jstep = istep*dims[0];
      kstep = jstep*dims[1];
      tstep = kstep*dims[2];   
   }
   
   public int getIndex(int v, int i, int j, int k, int t) {
      return v*vstep+i*istep+j*jstep+k*kstep+t*tstep;
   }
   
   public int getRealIndex(int v, int i, int j, int k, int t) {
      return getIndex(v,i,j,k,t);
   }
   
   public int getImaginaryIndex(int v, int i, int j, int k, int t) {
      if (!complex) {
         return -1;
      }
      return getIndex(v,i,j,k,t)+1;
   }
   
   public int getRedIndex(int v, int i, int j, int k, int t) {
      return getIndex(v,i,j,k,t);
   }
   
   public int getGreenIndex(int v, int i, int j, int k, int t) {
      if (dataType == DataType.NIFTI_TYPE_RGB24) {
         return getIndex(v,i,j,k,t)+1;
      }
      return getIndex(v,i,j,k,t);
   }
   
   public int getBlueIndex(int v, int i, int j, int k, int t) {
      if (dataType == DataType.NIFTI_TYPE_RGB24) {
         return getIndex(v,i,j,k,t)+2;
      }
      return getIndex(v,i,j,k,t);
   }

   public DataType getDataType() {
      return dataType;
   }

   public int getNumValuesPerVoxel() {
      return dims[4];
   }
   
   /**
    * Returns the number of entities that make up a value.  For example, complex numbers have a value size of 2,
    * byte RGB24 has a value of 3.
    * 
    * @return value size
    */
   public int getNumEntitiesPerValue() {
      return vstep;
   }
   
   public NiftiDataBuffer clone() {
      try {
         NiftiDataBuffer copy = (NiftiDataBuffer)super.clone();
         copy.dims = Arrays.copyOf(dims, dims.length);
         switch (dataType) {
            case DT_BINARY:
               copy.bools = Arrays.copyOf(bools, bools.length);
               buff = copy.bools;
               break;
            case NIFTI_TYPE_FLOAT32:
            case NIFTI_TYPE_COMPLEX64:
               copy.floats = Arrays.copyOf(floats, floats.length);
               copy.buff = copy.floats;
               break;
            case NIFTI_TYPE_FLOAT64:
            case NIFTI_TYPE_FLOAT128:
            case NIFTI_TYPE_COMPLEX128:
            case NIFTI_TYPE_COMPLEX256:
               copy.doubles = Arrays.copyOf(doubles, doubles.length);
               copy.buff = copy.doubles;
               break;
            case NIFTI_TYPE_INT16:
            case NIFTI_TYPE_UINT16:
               copy.shorts = Arrays.copyOf(shorts, shorts.length);
               copy.buff = copy.shorts;
               break;
            case NIFTI_TYPE_INT32:
            case NIFTI_TYPE_UINT32:
               copy.ints = Arrays.copyOf(ints, ints.length);
               copy.buff = copy.ints;
               break;
            case NIFTI_TYPE_INT64:
            case NIFTI_TYPE_UINT64:
               copy.longs = Arrays.copyOf(longs, longs.length);
               copy.buff = copy.longs;
               break;
            case NIFTI_TYPE_INT8:
            case NIFTI_TYPE_UINT8:
            case NIFTI_TYPE_RGB24:
               copy.bytes = Arrays.copyOf(bytes, bytes.length);
               copy.buff = copy.bytes;
               break;
            case DT_UNKNOWN:
            default:
               break;
            
         }
         
         return copy;
      }
      catch (CloneNotSupportedException e) {
      }
      return null;
   }
   
   public float[] getFloat32() {
      return floats;
   }
   
   public double[] getFloat64() {
      return doubles;
   }
   
   /**
    * Gets a value from the buffer
    * @param e entity within value
    * @param v value index
    * @param i column index
    * @param j row index
    * @param k slice index
    * @param t time index
    * @return value
    */
   public double getValue(int e, int v, int i, int j, int k, int t) {
      int idx = getIndex (v, i, j, k, t) + e;
      return getValue(idx);
   }
   
   protected double getValue(int idx) {
      switch(getDataType ()) {
         case DT_BINARY:
            if (bools[idx]) {
               return 1.0;
            }
            return 0;
         case NIFTI_TYPE_COMPLEX128:
         case NIFTI_TYPE_COMPLEX256:
            return doubles[idx];
         case NIFTI_TYPE_COMPLEX64:
            return floats[idx];
         case NIFTI_TYPE_FLOAT128:
         case NIFTI_TYPE_FLOAT64:
            return doubles[idx];
         case NIFTI_TYPE_FLOAT32:
            return floats[idx];
         case NIFTI_TYPE_INT16:
            return (double)shorts[idx];
         case NIFTI_TYPE_INT32:
            return ints[idx];
         case NIFTI_TYPE_INT64:
            return longs[idx];
         case NIFTI_TYPE_INT8:
            return bytes[idx];
         case NIFTI_TYPE_RGB24:
            return (bytes[idx] & 0xFF);
         case NIFTI_TYPE_UINT16:
            return (shorts[idx] & 0xFFFF);
         case NIFTI_TYPE_UINT32:
            return ((long)(ints[idx]) & 0xFFFFFFFFL);
         case NIFTI_TYPE_UINT64: {
            double v = longs[idx];
            if (v < 0) {
               v += 18446744073709551616.0; // add 2^64
            }
            return v;
         }
         case NIFTI_TYPE_UINT8:
            return bytes[idx] & 0xFF;
         default:
         case DT_UNKNOWN:
            break;
      }
      return 0;
   }
   
   

}
