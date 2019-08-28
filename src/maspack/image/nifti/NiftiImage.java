package maspack.image.nifti;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import maspack.image.VolumeImage;
import maspack.image.nifti.NiftiHeader.DataType;
import maspack.image.nifti.NiftiHeader.XFormCode;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.Matrix3d;
import maspack.matrix.Vector3d;
import maspack.util.Clonable;

public class NiftiImage implements VolumeImage, Clonable {

   public static enum ImageSpace {
      VOXEL,
      SCALED,
      QUATERNION,
      AFFINE,
      DETECT
   }
   
   NiftiHeader header;
   String title;
   
   // vals,x,y,z,t
   NiftiDataBuffer buff;
   
   public NiftiImage(String title, String fileName) {
      this(title, new File(fileName));
   }
   
   public NiftiImage(String title, File file) {
      try {
         NiftiImage im = NiftiReader.read(file);
         this.header = im.header;
         this.buff = im.buff;
         setTitle(title);
      }
      catch (IOException e) {
         throw new RuntimeException("Cannot read file", e);
      }
   }
   
   public NiftiImage(String title, NiftiHeader header, NiftiDataBuffer buff) {
      setTitle(title);
      this.header = header;
      this.buff = buff;
      
      // maybe adjust buff to apply slope and scale?
      if (buff.getDataType() != DataType.NIFTI_TYPE_RGB24
         && header != null && header.scl_slope != 0) {
         
         DataType ntype = DataType.NIFTI_TYPE_FLOAT64;
         if (buff.getDataType().isComplex()) {
            ntype = DataType.NIFTI_TYPE_COMPLEX128;
         }
         NiftiDataBuffer nbuff = new NiftiDataBuffer(ntype, 
            Arrays.copyOf(buff.dims, buff.dims.length));
         
         // create new buffer
         switch (buff.getDataType()) {
            case DT_BINARY:
               for (int i=0; i<buff.bools.length; ++i) {
                  nbuff.doubles[i] = header.scl_inter;
                  if (buff.bools[i]) {
                     nbuff.doubles[i] += header.scl_slope;
                  }
               }
               break;
            case NIFTI_TYPE_COMPLEX128:
            case NIFTI_TYPE_COMPLEX256:
               for (int i=0; i<buff.doubles.length; ++i) {
                  nbuff.doubles[i] = header.scl_inter + header.scl_slope*buff.doubles[i];
               }
               break;
            case NIFTI_TYPE_COMPLEX64:
               for (int i=0; i<buff.floats.length; ++i) {
                  nbuff.doubles[i] = header.scl_inter + header.scl_slope*buff.floats[i];
               }
               break;
            case NIFTI_TYPE_FLOAT128:
            case NIFTI_TYPE_FLOAT64:
               for (int i=0; i<buff.doubles.length; ++i) {
                  nbuff.doubles[i] = header.scl_inter + header.scl_slope*buff.doubles[i];
               }
               break;
            case NIFTI_TYPE_FLOAT32:
               for (int i=0; i<buff.floats.length; ++i) {
                  nbuff.doubles[i] = header.scl_inter + header.scl_slope*buff.floats[i];
               }
               break;
            case NIFTI_TYPE_INT16:
               for (int i=0; i<buff.shorts.length; ++i) {
                  nbuff.doubles[i] = header.scl_inter + header.scl_slope*buff.shorts[i];
               }
               break;
            case NIFTI_TYPE_INT32:
               for (int i=0; i<buff.ints.length; ++i) {
                  nbuff.doubles[i] = header.scl_inter + header.scl_slope*buff.ints[i];
               }
               break;
            case NIFTI_TYPE_INT64:
               for (int i=0; i<buff.longs.length; ++i) {
                  nbuff.doubles[i] = header.scl_inter + header.scl_slope*buff.longs[i];
               }
               break;
            case NIFTI_TYPE_INT8:
               for (int i=0; i<buff.bytes.length; ++i) {
                  nbuff.doubles[i] = header.scl_inter + header.scl_slope*buff.bytes[i];
               }
               break;
            case NIFTI_TYPE_UINT16:
               for (int i=0; i<buff.shorts.length; ++i) {
                  nbuff.doubles[i] = header.scl_inter + header.scl_slope*toUnsigned(buff.shorts[i]);
               }
               break;
            case NIFTI_TYPE_UINT32:
               for (int i=0; i<buff.ints.length; ++i) {
                  nbuff.doubles[i] = header.scl_inter + header.scl_slope*toUnsigned(buff.ints[i]);
               }
               break;
            case NIFTI_TYPE_UINT64:
               for (int i=0; i<buff.longs.length; ++i) {
                  nbuff.doubles[i] = header.scl_inter + header.scl_slope*toUnsigned(buff.longs[i]);
               }
               break;
            case NIFTI_TYPE_UINT8:
               for (int i=0; i<buff.bytes.length; ++i) {
                  nbuff.doubles[i] = header.scl_inter + header.scl_slope*toUnsigned(buff.bytes[i]);
               }
               break;
            default:
            case DT_UNKNOWN:
            case NIFTI_TYPE_RGB24:
               break;
            
         }
         
         this.buff = nbuff;
      }
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
   
   public void setTitle(String title) {
      this.title = title;
   }
   
   public String getTitle() {
      return title;
   }
   
   /**
    * Determines the most likely transform, checking if Quaternion-based
    * exists first, then Affine, then None.
    * @return transform
    */
   public AffineTransform3d getVoxelTransform() {
      return getVoxelTransform(ImageSpace.DETECT);
   }
   
   protected ImageSpace detectTransform() {
      ImageSpace method = ImageSpace.SCALED;
      if (header.qform_code != XFormCode.NIFTI_XFORM_UNKNOWN) {
         method = ImageSpace.QUATERNION;
      } else if (header.sform_code != XFormCode.NIFTI_XFORM_UNKNOWN) {
         method = ImageSpace.AFFINE;
      }
      return method;
   }
   
   public XFormCode getVoxelTransformCode(ImageSpace method) {
      switch (method) {
         case AFFINE:
            return header.sform_code;
         case QUATERNION:
            return header.qform_code;
         case DETECT:
            return getVoxelTransformCode(detectTransform());
         case SCALED:
         case VOXEL:
         default:
            return XFormCode.NIFTI_XFORM_UNKNOWN;
         
      }
   }
   
   public AffineTransform3d getVoxelTransform(ImageSpace method) {
      if (method == ImageSpace.DETECT) {
         method = detectTransform();
      }
      if ((method == ImageSpace.QUATERNION) && (header.qform_code != XFormCode.NIFTI_XFORM_UNKNOWN)) {
         // quaternion-based
         AffineTransform3d trans = new AffineTransform3d();
         double a = header.quatern.s;
         double b = header.quatern.u.x;
         double c = header.quatern.u.y;
         double d = header.quatern.u.z;
         trans.A.set(a*a+b*b-c*c-d*d, 2*b*c-2*a*d, 2*b*d+2*a*c,
            2*b*c+2*a*d, a*a+c*c-b*b-d*d, 2*c*d-2*a*b,
            2*b*d-2*a*c, 2*c*d+2*a*b, a*a+d*d-c*c-b*b);
         double qfac = header.pixdim[0];
         // qfac must be 1 or -1
         if (qfac <= -0.5) {
            qfac = -1;
         } else {
            qfac = 1;
         }
         trans.A.mul(new Matrix3d(
            header.pixdim[1], 0, 0,
            0, header.pixdim[2], 0,
            0, 0, qfac*header.pixdim[3]));
         trans.p.set(header.qoffset);
         return trans;
      } else if ((method == ImageSpace.AFFINE) && (header.sform_code != XFormCode.NIFTI_XFORM_UNKNOWN) ){
         // affine
         return header.srow;
      } else if (method == ImageSpace.VOXEL){
         return new AffineTransform3d();  // unscaled
      } else if (method == ImageSpace.SCALED){
         // pixdim
         return new AffineTransform3d(
            header.pixdim[1], 0, 0, 0,
            0, header.pixdim[2], 0, 0,
            0, 0, header.pixdim[3], 0
            );
      } 
      return new AffineTransform3d();  // identity
   }
   
   public Vector3d getVoxelSpacing() {
      return new Vector3d(header.pixdim[1], header.pixdim[2], header.pixdim[3]);
   }
   
   public int numDimensions() {
      return (int)header.dim[0];
   }
   
   public int getDim(int idx) {
      return (int)header.dim[1+idx];
   }
   
   public int getNumCols() {
      return buff.dims[0];
   }
   
   public int getNumRows() {
      return buff.dims[1];
   }
   
   public int getNumSlices() {
      return buff.dims[2];
   }
   
   public int getNumTimes() {
      return buff.dims[3];
   }
   
   /**
    * Number of voxels per time instance
    * @return rows*cols*slices
    */
   public int getNumVoxels() {
      return getNumRows()*getNumCols()*getNumSlices();
   }
   
   public int getNumValuesPerVoxel() {
      return buff.dims[4];
   }
   
   public DataType getDataType() {
      return buff.dataType;
   }
   
   public NiftiDataBuffer getDataBuffer() {
      return buff;
   }
   
   public NiftiHeader getHeader() {
      return header;
   }
   
   public double getValue(int v, int i, int j, int k, int t) {
      int idx = buff.getIndex(v, i, j, k, t);
      switch(buff.getDataType()) {
         case DT_BINARY:
            if (buff.bools[idx]) {
               return 1;
            }
            return 0;
         case NIFTI_TYPE_COMPLEX64:
         case NIFTI_TYPE_FLOAT32:
            return buff.floats[idx];
         case NIFTI_TYPE_FLOAT64:
         case NIFTI_TYPE_FLOAT128:
         case NIFTI_TYPE_COMPLEX256:
         case NIFTI_TYPE_COMPLEX128:
            return buff.doubles[idx];
         case NIFTI_TYPE_INT16:
            return buff.shorts[idx];
         case NIFTI_TYPE_INT32:
            return buff.ints[idx];
         case NIFTI_TYPE_INT64:
            return buff.longs[idx];
         case NIFTI_TYPE_INT8:
            return buff.bytes[idx];
         case NIFTI_TYPE_UINT16:
            return toUnsigned(buff.shorts[idx]);
         case NIFTI_TYPE_UINT32:
            return toUnsigned(buff.ints[idx]);
         case NIFTI_TYPE_UINT64:
            return toUnsigned(buff.longs[idx]);
         case NIFTI_TYPE_UINT8:
         case NIFTI_TYPE_RGB24:
            return toUnsigned(buff.bytes[idx]);
         default:
         case DT_UNKNOWN:
            break;
      }
      return 0;
   }
   
   public float getFloat32(int v, int i, int j, int k, int t) {
      int idx = buff.getIndex(v, i, j, k, t);
      return buff.floats[idx];
   }
   
   public double getFloat64(int v, int i, int j, int k, int t) {
      int idx = buff.getIndex(v, i, j, k, t);
      return buff.doubles[idx];
   }
   
   public double getComplex128Real(int v, int i, int j, int k, int t) {
      int idx = buff.getRealIndex(v, i, j, k, t);
      return buff.doubles[idx];
   }
   
   public double getComplex128Imaginary(int v, int i, int j, int k, int t) {
      int idx = buff.getImaginaryIndex(v, i, j, k, t);
      return buff.doubles[idx];
   }
   
   public long getInt64(int v, int i, int j, int k, int t) {
      int idx = buff.getIndex(v, i, j, k, t);
      return buff.longs[idx];
   }
   
   public int getInt32(int v, int i, int j, int k, int t) {
      int idx = buff.getIndex(v, i, j, k, t);
      return buff.ints[idx];
   }
   
   public short getInt16(int v, int i, int j, int k, int t) {
      int idx = buff.getIndex(v, i, j, k, t);
      return buff.shorts[idx];
   }
   
   public byte getInt8(int v, int i, int j, int k, int t) {
      int idx = buff.getIndex(v, i, j, k, t);
      return buff.bytes[idx];
   }
   
   public boolean getBinary(int v, int i, int j, int k, int t) {
      int idx = buff.getIndex(v, i, j, k, t);
      return buff.bools[idx];
   }
   
   public byte getRed(int v, int i, int j, int k, int t) {
      int idx = buff.getRedIndex(v, i, j, k, t);
      return buff.bytes[idx];
   }
   
   public byte getGreen(int v, int i, int j, int k, int t) {
      int idx = buff.getGreenIndex(v, i, j, k, t);
      return buff.bytes[idx];
   }
   
   public byte getBlue(int v, int i, int j, int k, int t) {
      int idx = buff.getBlueIndex(v, i, j, k, t);
      return buff.bytes[idx];
   }

   /**
    * Fills a buffer with pixel values from the image
    */
   public int getPixels(int i, int di, int ni, int j, int dj, int nj,
      int k, int dk, int nk, int t,
      int scanline, int pageline,
      NiftiPixelGenerator voxelator,
      ByteBuffer pixels) {

      int offset = pixels.position();
      voxelator.getPixels(buff, i, di, ni, j, dj, nj, k, dk, nk, t, scanline,
         pageline, pixels);
      return pixels.position()-offset;
   }
   
   public NiftiImage clone() {
      try {
         NiftiImage copy = (NiftiImage)super.clone();
         copy.header = header.clone();
         copy.buff = buff.clone();
         return copy;
      }
      catch (CloneNotSupportedException e) {
         e.printStackTrace();
      }
      return null;
   }

   @Override
   public double getValue (int channel, int col, int row, int slice) {
      int v = channel / buff.getNumEntitiesPerValue ();
      int e = channel % buff.getNumEntitiesPerValue ();
      return buff.getValue (v, e, col, row, slice, 0);
   }

   @Override
   public int getNumChannels () {
      return buff.getNumValuesPerVoxel ()*buff.getNumEntitiesPerValue ();
   }
}
