package maspack.image.nifti;

import java.util.Arrays;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.Quaternion;
import maspack.matrix.Vector3d;
import maspack.util.Clonable;

public class NiftiHeader implements Clonable {

   public static class Units {
      public static enum Space {
         NIFTI_UNITS_UNKNOWN(0),
         NIFTI_UNITS_METER(1),
         NIFTI_UNITS_MM(2),
         NIFTI_UNITS_MICRON(3);
         private int id;
         private Space(int id) {
            this.id = id;
         }
         public static Space find(int id) {
            for (Space code : values()) {
               if (code.id == id) {
                  return code;
               }
            }
            return NIFTI_UNITS_UNKNOWN;
         }
      }
      public static enum Time {
         NIFTI_UNITS_UNKNOWN(0),
         NIFTI_UNITS_SEC(8),
         NIFTI_UNITS_MSEC(16),
         NIFTI_UNITS_USEC(24),
         NIFTI_UNITS_HZ(32),
         NIFTI_UNITS_PPM(40),
         NIFTI_UNITS_RADS(48);
         private int id;
         private Time(int id) {
            this.id = id;
         }
         public static Time find(int id) {
            for (Time code : values()) {
               if (code.id == id) {
                  return code;
               }
            }
            return NIFTI_UNITS_UNKNOWN;
         }
      }
      
      int code;
      private Space space;
      private Time time;
      
      public Units(int xyzt_code) {
         code = xyzt_code;
         int sp = xyzt_code & 0x07;
         space = Space.find(sp);
         int tm = xyzt_code & 0x38;
         time = Time.find(tm);
      }
      
      @Override
      public String toString() {
         StringBuilder out = new StringBuilder();
         if (space == Space.NIFTI_UNITS_UNKNOWN && time == Time.NIFTI_UNITS_UNKNOWN) {
            out.append("NIFTI_UNITS_UNKNOWN");
         } else if (space == Space.NIFTI_UNITS_UNKNOWN) {
            out.append(time);
         } else if (time == Time.NIFTI_UNITS_UNKNOWN) {
            out.append(space);
         } else {
            out.append(space);
            out.append("|");
            out.append(time);
         }
         return out.toString();
      }
   }
   
   public static enum SliceCode {
      NIFTI_SLICE_UNKNOWN(0),
      NIFTI_SLICE_SEQ_INC(1),
      NIFTI_SLICE_SEQ_DEC(2),
      NIFTI_SLICE_ALT_INC(3),
      NIFTI_SLICE_ALT_DEC(4);
      
      private int id;
      private SliceCode(int id) {
         this.id = id;
      }
      
      public static SliceCode find(int id) {
         for (SliceCode code : values()) {
            if (code.id == id) {
               return code;
            }
         }
         return NIFTI_SLICE_UNKNOWN;
      }
   }
   
   public static enum XFormCode {
      NIFTI_XFORM_UNKNOWN(0),
      NIFTI_XFORM_SCANNER_ANAT(1),
      NIFTI_XFORM_ALIGNED_ANAT(2),
      NIFTI_XFORM_TALAIRACH(3),
      NIFTI_XFORM_MNI_152(4);
      
      private int id;
      private XFormCode(int id) {
         this.id = id;
      }
      
      public static XFormCode find(int id) {
         for (XFormCode code : values()) {
            if (code.id == id) {
               return code;
            }
         }
         return NIFTI_XFORM_UNKNOWN;
      }
   }
   
   public static enum IntentCode {
      NIFTI_INTENT_CORREL(2),
      NIFTI_INTENT_TTEST(3),
      NIFTI_INTENT_FTEST(4),
      NIFTI_INTENT_ZSCORE(5),
      NIFTI_INTENT_CHISQ(6),
      NIFTI_INTENT_BETA(7),
      NIFTI_INTENT_BINOM(8),
      NIFTI_INTENT_GAMMA(9),
      NIFTI_INTENT_POISSON(10),
      NIFTI_INTENT_NORMAL(11),
      NIFTI_INTENT_FTEST_NONC(12),
      NIFTI_INTENT_CHISQ_NONC(13),
      NIFTI_INTENT_LOGISTIC(14),
      NIFTI_INTENT_LAPLACE(15),
      NIFTI_INTENT_UNIFORM(16),
      NIFTI_INTENT_TTEST_NONC(17),
      NIFTI_INTENT_WEIBULL(18),
      NIFTI_INTENT_CHI(19),
      NIFTI_INTENT_INVGAUSS(20),
      NIFTI_INTENT_EXTVAL(21),
      NIFTI_INTENT_PVAL(22),
      NIFTI_INTENT_LOGPVAL(23),
      NIFTI_INTENT_LOG10PVAL(24),

      NIFTI_INTENT_UNKNOWN(0),
      NIFTI_INTENT_ESTIMATE(1001),
      NIFTI_INTENT_LABEL(1002),
      NIFTI_INTENT_NEURONAME(1003),
      NIFTI_INTENT_GENMATRIX(1004),
      NIFTI_INTENT_SYMMATRIX(1005),
      NIFTI_INTENT_DISPVECT(1006),
      NIFTI_INTENT_VECTOR(1007),
      NIFTI_INTENT_POINTSET(1008),
      NIFTI_INTENT_TRIANGLE(1009),
      NIFTI_INTENT_QUATERNION(1010),
      NIFTI_INTENT_DIMLESS(1011);
      
      public static int NIFTI_FIRST_STATCODE = 2;
      public static int NIFTI_LAST_STATCODE = 24;
      
      private int id;
      private IntentCode(int id) {
         this.id = id;
      }
      
      public static IntentCode find(int id) {
         for (IntentCode code : values()) {
            if (code.id == id) {
               return code;
            }
         }
         return NIFTI_INTENT_UNKNOWN;
      }
   }
   
   public static enum DataType {
      DT_UNKNOWN(0),        // dunno
      DT_BINARY(1),         // 1 bit/voxel
      //      DT_UNSIGNED_CHAR(2),  // unsigned char, 8 bits/voxel
      //      DT_SIGNED_SHORT(4),   // signed short, 16 bits/voxel
      //      DT_SIGNED_INT(8),     // signed int, 32 bits/voxel
      //      DT_FLOAT(16),         // float, 32 bits/voxel
      //      DT_COMPLEX(32),       // complex float, 64 bits/voxel
      //      DT_DOUBLE(64),        // double, 64 bits/voxel
      //      DT_RGB(128),          // RGB triple, 3-byte (24 bits)/voxel
      //      DT_ALL(255),          // not useful?
      //      // aliases
      //      DT_UINT8(2),
      //      DT_INT16(4),
      //      DT_INT32(8),
      //      DT_FLOAT32(16),
      //      DT_COMPLEX64(32),
      //      DT_FLOAT64(64),
      //      DT_RGB24(128),
      //      // NIFTI-specific
      //      DT_INT8(256),
      //      DT_UINT16(512),
      //      DT_UINT32(768),
      //      DT_INT64(1024),
      //      DT_UINT64(1280),
      //      DT_FLOAT128(1536),
      //      DT_COMPLEX128(1792),
      //      DT_COMPLEX256(2048),
      // NIFTI aliases
      NIFTI_TYPE_UINT8(2), 
      NIFTI_TYPE_INT16(4), 
      NIFTI_TYPE_INT32(8), 
      NIFTI_TYPE_FLOAT32(16), 
      NIFTI_TYPE_COMPLEX64(32), 
      NIFTI_TYPE_FLOAT64(64), 
      NIFTI_TYPE_RGB24(128), 
      NIFTI_TYPE_INT8(256), 
      NIFTI_TYPE_UINT16(512), 
      NIFTI_TYPE_UINT32(768), 
      NIFTI_TYPE_INT64(1024), 
      NIFTI_TYPE_UINT64(1280), 
      NIFTI_TYPE_FLOAT128(1536), 
      NIFTI_TYPE_COMPLEX128(1792), 
      NIFTI_TYPE_COMPLEX256(2048), 
      ;
      int id;
      private DataType(int id) {
         this.id = id;
      }
      public static DataType find(int id) {
         for (DataType type : values()) {
            if (type.id == id) {
               return type;
            }
         }
         return DataType.DT_UNKNOWN;
      }
      public boolean isComplex() {
         return (this == NIFTI_TYPE_COMPLEX128 || this == NIFTI_TYPE_COMPLEX256 || this == NIFTI_TYPE_COMPLEX64);
      }
   }
   
   public static class Extension {
      int esize;
      int ecode;
      byte[] edata;
   }
   
   int sizeof_hdr;        // 540 for V2, 348 for V1
   char[] magic;          // valid signature
   DataType datatype;     // defines data type
   short bitpix;          // number bits/voxel
   long[] dim;            // data array dimensions
   double intent_p1;      // 1st intent parameter
   double intent_p2;      // 2nd intent parameter
   double intent_p3;      // 3rd intent parameter
   double[] pixdim;       // grid spacings
   long vox_offset;       // offset into .nii file
   double scl_slope;      // data scaling: slope
   double scl_inter;      // data scaling: offset
   double cal_max;        // max display intensity
   double cal_min;        // min display intensity
   double slice_duration; // time for 1 slice
   double toffset;        // time axis shift
   long slice_start;      // first slice index
   long slice_end;        // last slice index
   String descrip;        // text description
   String aux_file;       // auxiliary filename
   XFormCode qform_code;  // NIFTI_XFORM_* code
   XFormCode sform_code;  // NIFTI_XFORM_* code
   Quaternion quatern;    // quaternion rotation
   Vector3d qoffset;      // x-y-z shift
   AffineTransform3d srow;// affine transform
   SliceCode slice_code;  // slice timing order
   Units xyzt_units;      // units of pixdim[1..4]
   IntentCode intent_code;    // NIFTI_INTENT_* code
   String intent_name;    // 'name' or meaning of data
   char dim_info;         // MRI slice ordering
   // unused bytes
   
   Extension[] extensions;
   
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("magic: ");
      for (int i=0; i<magic.length; ++i) {
         sb.append((char)magic[i]);
      }
      sb.append('\n');
      
      sb.append("datatype: ");
      sb.append(datatype);
      sb.append('\n');
      
      sb.append("bitpix: ");
      sb.append(bitpix);
      sb.append('\n');
      
      sb.append("dim: {");
      sb.append(dim[0]);
      for (int i=1; i<dim.length; ++i) {
         sb.append(", ");
         sb.append(dim[i]);
      }
      sb.append("}\n");
      
      sb.append("intent_p1: ");
      sb.append(intent_p1);
      sb.append('\n');
      
      sb.append("intent_p2: ");
      sb.append(intent_p2);
      sb.append('\n');
      
      sb.append("intent_p3: ");
      sb.append(intent_p3);
      sb.append('\n');
      
      sb.append("pixdim: {");
      sb.append(pixdim[0]);
      for (int i=1; i<pixdim.length; ++i) {
         sb.append(", ");
         sb.append(pixdim[i]);
      }
      sb.append("}\n");
      
      sb.append("vox_offset: ");
      sb.append(vox_offset);
      sb.append('\n');
      
      sb.append("scl_slope: ");
      sb.append(scl_slope);
      sb.append('\n');
      
      sb.append("scl_inter: ");
      sb.append(scl_inter);
      sb.append('\n');
      
      sb.append("cal_max: ");
      sb.append(cal_max);
      sb.append('\n');
      
      sb.append("cal_min: ");
      sb.append(cal_min);
      sb.append('\n');
      
      sb.append("slice_duration: ");
      sb.append(slice_duration);
      sb.append('\n');
      
      sb.append("toffset: ");
      sb.append(toffset);
      sb.append('\n');
      
      sb.append("slice_start: ");
      sb.append(slice_start);
      sb.append('\n');
      
      sb.append("slice_end: ");
      sb.append(slice_end);
      sb.append('\n');
      
      sb.append("descrip: ");
      sb.append(descrip);
      sb.append('\n');
      
      sb.append("aux_file: ");
      sb.append(aux_file);
      sb.append('\n');
      
      sb.append("qform_code: ");
      sb.append(qform_code);
      sb.append('\n');
      
      sb.append("sform_code: ");
      sb.append(sform_code);
      sb.append('\n');
      
      sb.append("quatern: ");
      sb.append("[ "); 
      sb.append(quatern); 
      sb.append(" ]\n");
      
      sb.append("qoffset: [ ");
      sb.append(qoffset);
      sb.append(" ]\n");
      
      sb.append("affine: \n");
      sb.append("  [");
      sb.append(srow);
      sb.append(" ]\n");
      
      sb.append("slice_code: ");
      sb.append(slice_code);
      sb.append('\n');
      
      sb.append("xyzt_units: ");
      sb.append(xyzt_units);
      sb.append('\n');
      
      sb.append("intent_code: ");
      sb.append(intent_code);
      sb.append('\n');
      
      sb.append("intent_name: ");
      sb.append(intent_name);
      sb.append('\n');
      
      sb.append("dim_info: ");
      sb.append(dim_info);
      sb.append('\n');
      
      return sb.toString();
   }
   
   public NiftiHeader clone() {
      try {
         NiftiHeader copy = (NiftiHeader)(super.clone());
         if (extensions != null) {
            copy.extensions = Arrays.copyOf(extensions, extensions.length);
         }
         copy.dim = Arrays.copyOf(dim, dim.length);
         copy.magic = Arrays.copyOf(magic, magic.length);
         copy.pixdim = Arrays.copyOf(pixdim, pixdim.length);
         copy.quatern = quatern.clone();
         copy.qoffset = qoffset.clone();
         copy.srow = srow.copy();
         
         return copy;
      }
      catch (CloneNotSupportedException e) {
      }
      return null;
   }

   public void setDescription(String desc) {
      this.descrip = desc;
   }
   
   public String getDescription() {
      return descrip;
   }

   public int getSizeOfHeader() {
      return sizeof_hdr;
   }

   public char[] getMagic() {
      return magic;
   }
   
   public void setMagic(char[] magic) {
      this.magic = magic;
   }

   public DataType getDataType() {
      return datatype;
   }

   public void setDataType(DataType datatype) {
      this.datatype = datatype;
   }

   public short getBitPix() {
      return bitpix;
   }

   public void setBitPix(short bitpix) {
      this.bitpix = bitpix;
   }

   public long[] getDim() {
      return dim;
   }

   public void setDim(long[] dim) {
      this.dim = dim;
   }

   public double getIntentP1() {
      return intent_p1;
   }

   public void setIntentP1(double intent_p1) {
      this.intent_p1 = intent_p1;
   }

   public double getIntentP2() {
      return intent_p2;
   }

   public void setIntentP2(double intent_p2) {
      this.intent_p2 = intent_p2;
   }

   public double getIntentP3() {
      return intent_p3;
   }

   public void setIntentP3(double intent_p3) {
      this.intent_p3 = intent_p3;
   }

   public double[] getPixDim() {
      return pixdim;
   }

   public void setPixDim(double[] pixdim) {
      this.pixdim = pixdim;
   }

   public long getVoxOffset() {
      return vox_offset;
   }

   public void setVoxOffset(long vox_offset) {
      this.vox_offset = vox_offset;
   }

   public double getSclSlope() {
      return scl_slope;
   }

   public void setSclSlope(double scl_slope) {
      this.scl_slope = scl_slope;
   }

   public double getSclInter() {
      return scl_inter;
   }

   public void setSclInter(double scl_inter) {
      this.scl_inter = scl_inter;
   }

   public double getCalMax() {
      return cal_max;
   }

   public void setCalMax(double cal_max) {
      this.cal_max = cal_max;
   }

   public double getCalMin() {
      return cal_min;
   }

   public void setCalMin(double cal_min) {
      this.cal_min = cal_min;
   }

   public double getSliceDuration() {
      return slice_duration;
   }

   public void setSliceDuration(double slice_duration) {
      this.slice_duration = slice_duration;
   }

   public double getTOffset() {
      return toffset;
   }

   public void setTOffset(double toffset) {
      this.toffset = toffset;
   }

   public long getSliceStart() {
      return slice_start;
   }

   public void setSliceStart(long slice_start) {
      this.slice_start = slice_start;
   }

   public long getSliceEnd() {
      return slice_end;
   }

   public void setSliceEnd(long slice_end) {
      this.slice_end = slice_end;
   }

   public String getAuxFile() {
      return aux_file;
   }

   public void setAuxFile(String aux_file) {
      this.aux_file = aux_file;
   }

   public XFormCode getQFormCode() {
      return qform_code;
   }

   public void setQFormCode(XFormCode qform_code) {
      this.qform_code = qform_code;
   }

   public XFormCode getSFormCode() {
      return sform_code;
   }

   public void setSFormCode(XFormCode sform_code) {
      this.sform_code = sform_code;
   }

   public Quaternion getQuaternion() {
      return quatern;
   }

   public void setQuaternion(Quaternion quatern) {
      this.quatern = quatern;
   }

   public Vector3d getQOffset() {
      return qoffset;
   }

   public void setQOffset(Vector3d qoffset) {
      this.qoffset = qoffset;
   }

   public AffineTransform3d getSRow() {
      return srow;
   }

   public void setSRow(AffineTransform3d srow) {
      this.srow = srow;
   }

   public SliceCode getSliceCode() {
      return slice_code;
   }

   public void setSliceCode(SliceCode slice_code) {
      this.slice_code = slice_code;
   }

   public Units getXyztUnits() {
      return xyzt_units;
   }

   public void setXyztUnits(Units xyzt_units) {
      this.xyzt_units = xyzt_units;
   }

   public IntentCode getIntentCode() {
      return intent_code;
   }

   public void setIntentCode(IntentCode intent_code) {
      this.intent_code = intent_code;
   }

   public String getIntentName() {
      return intent_name;
   }

   public void setIntentName(String intent_name) {
      this.intent_name = intent_name;
   }

   public char getDimInfo() {
      return dim_info;
   }

   public void setDimInfo(char dim_info) {
      this.dim_info = dim_info;
   }

   public Extension[] getExtensions() {
      return extensions;
   }

   public void setExtensions(Extension[] extensions) {
      this.extensions = extensions;
   }
   
   
   
}
