package maspack.image.nifti;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import maspack.image.nifti.NiftiHeader.DataType;
import maspack.image.nifti.NiftiHeader.Extension;
import maspack.image.nifti.NiftiHeader.IntentCode;
import maspack.image.nifti.NiftiHeader.SliceCode;
import maspack.image.nifti.NiftiHeader.Units;
import maspack.image.nifti.NiftiHeader.XFormCode;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.Quaternion;
import maspack.matrix.Vector3d;
import maspack.util.BinaryInputStream;
import maspack.util.BitInputStream;

/**
 * Class for reading Analyze/Nifti-format files
 * @author Antonio
 *
 */
public class NiftiReader {
   
   /**
    * Nifti V1 or Analyze 7.5 format with separate header and image files
    * @param hdr header file
    * @param img image file
    * @return resulting image
    * @throws IOException on read failure
    */
   public static NiftiImage read(File hdr, File img) throws IOException {
      
      NiftiImage image = null;
      
      BinaryInputStream bhdr = new BinaryInputStream(new BufferedInputStream(new FileInputStream(hdr)));
      bhdr.setLittleEndian(true);
      
      NiftiHeader header = null;
      int dims[] = new int[5];
      
      try {
         header = readHeader(bhdr);
         header.extensions = readExtensions(bhdr);
         
         long ndims = header.dim[0];
         
         if (ndims == 3) {
            // single output, no time
            dims[0] = (int)header.dim[1];
            dims[1] = (int)header.dim[2];
            dims[2] = (int)header.dim[3];
            dims[3] = 1;
            dims[4] = 1;
         } else if (ndims == 4) {
            // no time
            dims[0] = (int)header.dim[1];
            dims[1] = (int)header.dim[2];
            dims[2] = (int)header.dim[3];
            dims[3] = 1;
            dims[4] = (int)header.dim[4];
         } else {
            dims[0] = (int)header.dim[1];
            dims[1] = (int)header.dim[2];
            dims[2] = (int)header.dim[3];
            dims[3] = (int)header.dim[4];
            dims[4] = (int)header.dim[5];
         }
         
      } catch (IOException ioe) {
         throw ioe;
      } finally {
         bhdr.close();
      }
      
      BinaryInputStream bimg = new BinaryInputStream(new BufferedInputStream(new FileInputStream(img)));
      bimg.setLittleEndian (true);
      
      try {
         NiftiDataBuffer buff = readData(bimg, dims, header.datatype);
         image = new NiftiImage(hdr.getName(), header, buff);
      } catch (IOException ioe) {
         throw ioe;
      } finally {
         bimg.close ();
      }
      
      return image;
   }
   
   public static NiftiImage read(File file) throws IOException {
      
      NiftiImage image = null;
      
      BinaryInputStream bis = new BinaryInputStream(new BufferedInputStream(new FileInputStream(file)));
      bis.setLittleEndian(true);
      try {
         NiftiHeader header = readHeader(bis);
         header.extensions = readExtensions(bis);
         
         int dims[] = new int[5];
         long ndims = header.dim[0];
         
         if (ndims == 3) {
            // single output, no time
            dims[0] = (int)header.dim[1];
            dims[1] = (int)header.dim[2];
            dims[2] = (int)header.dim[3];
            dims[3] = 1;
            dims[4] = 1;
         } else if (ndims == 4) {
            // no time
            dims[0] = (int)header.dim[1];
            dims[1] = (int)header.dim[2];
            dims[2] = (int)header.dim[3];
            dims[3] = 1;
            dims[4] = (int)header.dim[4];
         } else {
            dims[0] = (int)header.dim[1];
            dims[1] = (int)header.dim[2];
            dims[2] = (int)header.dim[3];
            dims[3] = (int)header.dim[4];
            dims[4] = (int)header.dim[5];
         }
         
         bis = advanceToOffset(bis, file, header.vox_offset);
         
         NiftiDataBuffer buff = readData(bis, dims, header.datatype);
         image = new NiftiImage(file.getName(), header, buff);
      } catch (IOException ioe) {
         throw ioe;
      } finally {
         bis.close();
      }
      
      return image;
      
   }
   
   public static Extension[] readExtensions(BinaryInputStream reader) throws IOException {
      
      byte[] extension = new byte[4];
      try {
         reader.readFully(extension, 0, 4);
      } catch (EOFException eof) {
         // no extension in header
         return null;
      }
      
      if (extension[0] == 0) {
         return null;
      }
      
      // read extensions
      ArrayList<Extension> extensions = new ArrayList<>();
      while (extension[0] != 0) {
         Extension ext = new Extension();
         ext.esize = reader.readInt();
         ext.ecode = reader.readInt();
         int nebytes = ext.esize-16;
         ext.edata = new byte[nebytes];
         reader.readFully(ext.edata, 0, nebytes);
         extensions.add(ext);
         reader.readFully(extension, 0, 4);
      }
      
      return extensions.toArray(new Extension[extensions.size()]);
   }
   
   public static NiftiHeader readHeader(BinaryInputStream reader) throws IOException {
      
      NiftiHeader header = new NiftiHeader();
      
      boolean v2 = true;
      // size of header, 4-byte integer
      header.sizeof_hdr = reader.readInt();
      
      // detect endianness
      if (header.sizeof_hdr == 0x5c010000) {
         reader.setLittleEndian(!reader.isLittleEndian());
         header.sizeof_hdr = 0x015c;
      } else if (header.sizeof_hdr == 0x1c020000) {
         reader.setLittleEndian(!reader.isLittleEndian());
         header.sizeof_hdr = 0x021c;
      }
      
      // single byte char
      reader.setByteChar(true);
      
      if (header.sizeof_hdr == 348) {
         v2 = false;
      } else if (header.sizeof_hdr != 540) {
         throw new IOException("Not a NIFTI file, expected header size of 348 or 540, received: " + header.sizeof_hdr);
      }
      
      if (v2) {
         readHeaderV2(reader, header);
      } else {
         readHeaderV1(reader, header);
      }
      
      return header;
   }
   
   private static int strlen(char[] c) {
      for (int i=0; i<c.length; ++i) {
         if (c[i] == 0) {
            return i;
         }
      }
      return c.length;
   }
   
   @SuppressWarnings("unused")
   private static void readHeaderV1(BinaryInputStream reader, NiftiHeader header) throws IOException {
      
      // unused data type
      char[] data_type = new char[10];
      for (int i=0; i<data_type.length; ++i) {
         data_type[i] = reader.readChar();
      }
      
      // unused db name
      char[] db_name = new char[18];
      for (int i=0; i<db_name.length; ++i) {
         db_name[i] = reader.readChar();
      }
      
      // unused
      int extents = reader.readInt();
      short session_error = reader.readShort();
      char regular = reader.readChar();
      
      header.dim_info = reader.readChar();
      header.dim = new long[8];
      for (int i=0; i<header.dim.length; ++i) {
         header.dim[i] = reader.readShort();
      }
      
      header.intent_p1 = reader.readFloat();
      header.intent_p2 = reader.readFloat();
      header.intent_p3 = reader.readFloat();
      short intent_code = reader.readShort();
      header.intent_code = IntentCode.find(intent_code);
      int datatype = reader.readShort();
      header.datatype = DataType.find(datatype);
      header.bitpix = reader.readShort();
      header.slice_start = reader.readShort();
      
      header.pixdim = new double[8];
      for (int i=0; i<header.pixdim.length; ++i) {
         header.pixdim[i] = reader.readFloat();
      }
      
      float vox_offset = reader.readFloat();
      header.vox_offset = (long)(vox_offset);
      
      header.scl_slope = reader.readFloat();
      header.scl_inter = reader.readFloat();
      header.slice_end = reader.readShort();
      char slice_code = reader.readChar();
      header.slice_code = SliceCode.find(slice_code);
      char xyzt_units = reader.readChar();
      header.xyzt_units = new Units(xyzt_units);
      header.cal_max = reader.readFloat();
      header.cal_min = reader.readFloat();
      header.slice_duration = reader.readFloat();
      header.toffset = reader.readFloat();
      
      // unused
      int glmax = reader.readInt();
      int glmin = reader.readInt();
      
      char[] descrip = new char[80];
      for (int i=0; i<descrip.length; ++i) {
         descrip[i] = reader.readChar();
      }
      header.descrip = new String(descrip, 0, strlen(descrip));
      
      char[] aux_file = new char[24];
      for (int i=0; i<aux_file.length; ++i) {
         aux_file[i] = reader.readChar();
      }
      header.aux_file = new String(aux_file, 0, strlen(aux_file));
      
      int xform_code = reader.readShort();
      header.qform_code = XFormCode.find(xform_code);
      xform_code = reader.readShort();
      header.sform_code = XFormCode.find(xform_code);
      
      
      double quatern_b = reader.readFloat();
      double quatern_c = reader.readFloat();
      double quatern_d = reader.readFloat();
      double quatern_a = Math.sqrt(1.0-(quatern_b*quatern_b+quatern_c*quatern_c+quatern_d*quatern_d));
      header.quatern = new Quaternion(quatern_a, quatern_b, quatern_c, quatern_d);
      double qoffset_x = reader.readFloat();
      double qoffset_y = reader.readFloat();
      double qoffset_z = reader.readFloat();
      header.qoffset = new Vector3d(qoffset_x, qoffset_y, qoffset_z);
      
      double[] srow_x = new double[4];
      for (int i=0; i<srow_x.length; ++i) {
         srow_x[i] = reader.readFloat();
      }
      double[] srow_y = new double[4];
      for (int i=0; i<srow_y.length; ++i) {
         srow_y[i] = reader.readFloat();
      }
      double[] srow_z = new double[4];
      for (int i=0; i<srow_z.length; ++i) {
         srow_z[i] = reader.readFloat();
      }
      header.srow = new AffineTransform3d(
         srow_x[0], srow_x[1], srow_x[2], srow_x[3],
         srow_y[0], srow_y[1], srow_y[2], srow_y[3],
         srow_z[0], srow_z[1], srow_z[2], srow_z[3]
         );
      
      char[] intent_name = new char[16];
      for (int i=0; i<intent_name.length; ++i) {
         intent_name[i] = reader.readChar();
      }
      header.intent_name = new String(intent_name, 0, strlen(intent_name));
      
      header.magic = new char[4];  // ni1\0 or n+1\0
      for (int i=0; i<header.magic.length; ++i) {
         header.magic[i] = reader.readChar();
      }
   }
   
   private static void readHeaderV2(BinaryInputStream reader, NiftiHeader header) throws IOException {
      // magic 8 bytes
      header.magic = new char[8];
      for (int i=0; i<header.magic.length; ++i) {
         header.magic[i] = reader.readChar();
      }
      int datatype = reader.readShort();
      header.datatype = DataType.find(datatype);
      header.bitpix = reader.readShort();
      
      header.dim = new long[8];
      for (int i=0; i<header.dim.length; ++i) {
         header.dim[i] = reader.readLong();
      }

      header.intent_p1 = reader.readDouble();
      header.intent_p2 = reader.readDouble();
      header.intent_p3 = reader.readDouble();
      header.pixdim = new double[8];
      for (int i=0; i<header.pixdim.length; ++i) {
         header.pixdim[i] = reader.readDouble();
      }
      
      header.vox_offset = reader.readLong();
      header.scl_slope = reader.readDouble();
      header.scl_inter = reader.readDouble();
      header.cal_max = reader.readDouble();
      header.cal_min = reader.readDouble();
      header.slice_duration = reader.readDouble();
      header.toffset = reader.readDouble();
      header.slice_start = reader.readLong();
      header.slice_end = reader.readLong();
      
      char[] descrip = new char[80];
      for (int i=0; i<descrip.length; ++i) {
         descrip[i] = reader.readChar();
      }
      header.descrip = new String(descrip, 0, strlen(descrip));
      
      char[] aux_file = new char[24];
      for (int i=0; i<aux_file.length; ++i) {
         aux_file[i] = reader.readChar();
      }
      header.aux_file = new String(aux_file, 0, strlen(aux_file));
      
      int xform_code = reader.readInt();
      header.qform_code = XFormCode.find(xform_code);
      xform_code = reader.readInt();
      header.sform_code = XFormCode.find(xform_code);
      
      double quatern_b = reader.readDouble();
      double quatern_c = reader.readDouble();
      double quatern_d = reader.readDouble();
      double quatern_a = Math.sqrt(1.0-(quatern_b*quatern_b+quatern_c*quatern_c+quatern_d*quatern_d));
      header.quatern = new Quaternion(quatern_a, quatern_b, quatern_c, quatern_d);
      double qoffset_x = reader.readDouble();
      double qoffset_y = reader.readDouble();
      double qoffset_z = reader.readDouble();
      header.qoffset = new Vector3d(qoffset_x, qoffset_y, qoffset_z);
      
      double[] srow_x = new double[4];
      for (int i=0; i<srow_x.length; ++i) {
         srow_x[i] = reader.readDouble();
      }
      double[] srow_y = new double[4];
      for (int i=0; i<srow_y.length; ++i) {
         srow_y[i] = reader.readDouble();
      }
      double[] srow_z = new double[4];
      for (int i=0; i<srow_z.length; ++i) {
         srow_z[i] = reader.readDouble();
      }
      header.srow = new AffineTransform3d(
         srow_x[0], srow_x[1], srow_x[2], srow_x[3],
         srow_y[0], srow_y[1], srow_y[2], srow_y[3],
         srow_z[0], srow_z[1], srow_z[2], srow_z[3]
         );
      
      int slice_code = reader.readInt();
      header.slice_code = SliceCode.find(slice_code);
      int xyzt_units = reader.readInt();
      header.xyzt_units = new Units(xyzt_units);
      int intent_code = reader.readInt();
      header.intent_code = IntentCode.find(intent_code);
      
      char[] intent_name = new char[16];
      for (int i=0; i<intent_name.length; ++i) {
         intent_name[i] = reader.readChar();
      }
      header.intent_name = new String(intent_name, 0, strlen(intent_name));
      
      header.dim_info = reader.readChar();
      
      char[] unused_str = new char[15];
      for (int i=0; i<unused_str.length; ++i) {
         unused_str[i] = reader.readChar();
      }
   }
   
   private static void fillBools(BinaryInputStream bis, int[] dims, boolean[] buff) throws IOException {

      @SuppressWarnings("resource")
      BitInputStream bitstream = new BitInputStream(bis);
      
      int vstep = 1;
      int istep = vstep*dims[4];
      int jstep = istep*dims[0];
      int kstep = jstep*dims[1];
      int tstep = kstep*dims[2];
      // int size = tstep*dims[3];
      
      for (int v = 0; v<dims[4]; ++v) {
         for (int t=0; t<dims[3]; ++t) {
            for (int k=0; k<dims[2]; ++k) {
               for (int j=0; j<dims[1]; ++j) {
                  for (int i=0; i<dims[0]; ++i) {
                     int b = bitstream.read();
                     int idx = t*tstep + k*kstep + j*jstep + i*istep+v*vstep;
                     buff[idx] = (b != 0);
                  }
               }
            }
         }
      }
   }

   private static void fillComplex256(BinaryInputStream bis, int[] dims, double[] buff) throws IOException {

      int vstep = 2;
      int istep = vstep*dims[4];
      int jstep = istep*dims[0];
      int kstep = jstep*dims[1];
      int tstep = kstep*dims[2];
      // int size = tstep*dims[3];

      for (int v = 0; v<dims[4]; ++v) {
         for (int t=0; t<dims[3]; ++t) {
            for (int k=0; k<dims[2]; ++k) {
               for (int j=0; j<dims[1]; ++j) {
                  for (int i=0; i<dims[0]; ++i) {
                     double re = bis.readLongDouble();
                     double im = bis.readLongDouble();
                     
                     int idx = t*tstep + k*kstep + j*jstep + i*istep+v*vstep;
                     buff[idx] = re;
                     buff[idx+1] = im;
                  }
               }
            }
         }
      }
   }
   
   private static void fillComplex128(BinaryInputStream bis, int[] dims, double[] buff) throws IOException {

      int vstep = 2;
      int istep = vstep*dims[4];
      int jstep = istep*dims[0];
      int kstep = jstep*dims[1];
      int tstep = kstep*dims[2];
      // int size = tstep*dims[3]; 
      
      for (int v = 0; v<dims[4]; ++v) {
         for (int t=0; t<dims[3]; ++t) {
            for (int k=0; k<dims[2]; ++k) {
               for (int j=0; j<dims[1]; ++j) {
                  for (int i=0; i<dims[0]; ++i) {
                     double re = bis.readDouble();
                     double im = bis.readDouble();
                     
                     int idx = t*tstep + k*kstep + j*jstep + i*istep+v*vstep;
                     buff[idx] = re;
                     buff[idx+1] = im;
                  }
               }
            }
         }
      }
   }
   
   private static void fillComplex64(BinaryInputStream bis, int[] dims, float[] buff) throws IOException {

      int vstep = 2;
      int istep = vstep*dims[4];
      int jstep = istep*dims[0];
      int kstep = jstep*dims[1];
      int tstep = kstep*dims[2];
      // int size = tstep*dims[3];
      
      for (int v = 0; v<dims[4]; ++v) {
         for (int t=0; t<dims[3]; ++t) {
            for (int k=0; k<dims[2]; ++k) {
               for (int j=0; j<dims[1]; ++j) {
                  for (int i=0; i<dims[0]; ++i) {
                     float re = bis.readFloat();
                     float im = bis.readFloat();
                     
                     int idx = t*tstep + k*kstep + j*jstep + i*istep+v*vstep;
                     buff[idx] = re;
                     buff[idx+1] = im;
                  }
               }
            }
         }
      }
   }
   
   private static void fillFloat128(BinaryInputStream bis, int[] dims, double[] buff) throws IOException {

      int vstep = 1;
      int istep = vstep*dims[4];
      int jstep = istep*dims[0];
      int kstep = jstep*dims[1];
      int tstep = kstep*dims[2];
      // int size = tstep*dims[3];

      for (int v = 0; v<dims[4]; ++v) {
         for (int t=0; t<dims[3]; ++t) {
            for (int k=0; k<dims[2]; ++k) {
               for (int j=0; j<dims[1]; ++j) {
                  for (int i=0; i<dims[0]; ++i) {
                     int idx = t*tstep + k*kstep + j*jstep + i*istep+v*vstep;
                     buff[idx] = bis.readLongDouble();
                  }
               }
            }
         }
      }
   }
   
   private static void fillFloat64(BinaryInputStream bis, int[] dims, double[] buff) throws IOException {

      int vstep = 1;
      int istep = vstep*dims[4];
      int jstep = istep*dims[0];
      int kstep = jstep*dims[1];
      int tstep = kstep*dims[2];
      // int size = tstep*dims[3];
      
      for (int v = 0; v<dims[4]; ++v) {
         for (int t=0; t<dims[3]; ++t) {
            for (int k=0; k<dims[2]; ++k) {
               for (int j=0; j<dims[1]; ++j) {
                  for (int i=0; i<dims[0]; ++i) {
                     int idx = t*tstep + k*kstep + j*jstep + i*istep+v*vstep;
                     buff[idx] = bis.readDouble();
                  }
               }
            }
         }
      }
   }

   private static void fillFloat32(BinaryInputStream bis, int[] dims, float[] buff) throws IOException {

      int vstep = 1;
      int istep = vstep*dims[4];
      int jstep = istep*dims[0];
      int kstep = jstep*dims[1];
      int tstep = kstep*dims[2];
      // int size = tstep*dims[3];
      
      for (int v = 0; v<dims[4]; ++v) {
         for (int t=0; t<dims[3]; ++t) {
            for (int k=0; k<dims[2]; ++k) {
               for (int j=0; j<dims[1]; ++j) {
                  for (int i=0; i<dims[0]; ++i) {
                     int idx = t*tstep + k*kstep + j*jstep + i*istep+v*vstep;
                     buff[idx] = bis.readFloat();
                  }
               }
            }
         }
      }
   }
   
   private static void fillInt8(BinaryInputStream bis, int[] dims, byte[] buff) throws IOException {

      int vstep = 1;
      int istep = vstep*dims[4];
      int jstep = istep*dims[0];
      int kstep = jstep*dims[1];
      int tstep = kstep*dims[2];
      // int size = tstep*dims[3];

      for (int v = 0; v<dims[4]; ++v) {
         for (int t=0; t<dims[3]; ++t) {
            for (int k=0; k<dims[2]; ++k) {
               for (int j=0; j<dims[1]; ++j) {
                  for (int i=0; i<dims[0]; ++i) {
                     int idx = t*tstep + k*kstep + j*jstep + i*istep+v*vstep;
                     buff[idx] = bis.readByte();
                  }
               }
            }
         }
      }
   }
   
   private static void fillInt16(BinaryInputStream bis, int[] dims, short[] buff) throws IOException {

      int vstep = 1;
      int istep = vstep*dims[4];
      int jstep = istep*dims[0];
      int kstep = jstep*dims[1];
      int tstep = kstep*dims[2];
      // int size = tstep*dims[3];

      for (int v = 0; v<dims[4]; ++v) {
         for (int t=0; t<dims[3]; ++t) {
            for (int k=0; k<dims[2]; ++k) {
               for (int j=0; j<dims[1]; ++j) {
                  for (int i=0; i<dims[0]; ++i) {
                     int idx = t*tstep + k*kstep + j*jstep + i*istep+v*vstep;
                     buff[idx] = bis.readShort();
                  }
               }
            }
         }
      }
   }
   
   private static void fillInt32(BinaryInputStream bis, int[] dims, int[] buff) throws IOException {

      int vstep = 1;
      int istep = vstep*dims[4];
      int jstep = istep*dims[0];
      int kstep = jstep*dims[1];
      int tstep = kstep*dims[2];
      // int size = tstep*dims[3];
      
      for (int v = 0; v<dims[4]; ++v) {
         for (int t=0; t<dims[3]; ++t) {
            for (int k=0; k<dims[2]; ++k) {
               for (int j=0; j<dims[1]; ++j) {
                  for (int i=0; i<dims[0]; ++i) {
                     int idx = t*tstep + k*kstep + j*jstep + i*istep+v*vstep;
                     buff[idx] = bis.readInt();
                  }
               }
            }
         }
      }
   }
   
   private static void fillInt64(BinaryInputStream bis, int[] dims, long[] buff) throws IOException {

      int vstep = 1;
      int istep = vstep*dims[4];
      int jstep = istep*dims[0];
      int kstep = jstep*dims[1];
      int tstep = kstep*dims[2];
      // int size = tstep*dims[3];
      
      for (int v = 0; v<dims[4]; ++v) {
         for (int t=0; t<dims[3]; ++t) {
            for (int k=0; k<dims[2]; ++k) {
               for (int j=0; j<dims[1]; ++j) {
                  for (int i=0; i<dims[0]; ++i) {
                     int idx = t*tstep + k*kstep + j*jstep + i*istep+v*vstep;
                     buff[idx] = bis.readLong();
                  }
               }
            }
         }
      }
   }
   
   private static void fillRGB24(BinaryInputStream bis, int[] dims, byte[] buff) throws IOException {

      int vstep = 3;
      int istep = vstep*dims[4];
      int jstep = istep*dims[0];
      int kstep = jstep*dims[1];
      int tstep = kstep*dims[2];
      // int size = tstep*dims[3];
      
      for (int v = 0; v<dims[4]; ++v) {
         for (int t=0; t<dims[3]; ++t) {
            for (int k=0; k<dims[2]; ++k) {
               for (int j=0; j<dims[1]; ++j) {
                  for (int i=0; i<dims[0]; ++i) {
                     int idx = t*tstep + k*kstep + j*jstep + i*istep+v*vstep;
                     buff[idx] = bis.readByte();
                     buff[idx+1] = bis.readByte();
                     buff[idx+2] = bis.readByte();
                  }
               }
            }
         }
      }
   }
   
   public static BinaryInputStream advanceToOffset(BinaryInputStream bis, File file, long offset) throws IOException {
      if (bis.getByteCount() > offset) {
         boolean littleEndian = bis.isLittleEndian();
         bis.close();
         bis = new BinaryInputStream(new BufferedInputStream(new FileInputStream(file)));
         bis.setLittleEndian(littleEndian);
         bis.skip(offset);
      } else if (bis.getByteCount() < offset) {
         long skip = offset-bis.getByteCount(); 
         bis.skip(skip);
      }
      return bis;
   }
   
   public static NiftiDataBuffer readData(BinaryInputStream bis, int[] dims, DataType dataType) throws IOException {

      NiftiDataBuffer buff = new NiftiDataBuffer(dataType, dims);
      
      switch (dataType) {
         case DT_BINARY:
            fillBools(bis, dims, buff.bools);
            break;
         case NIFTI_TYPE_COMPLEX128:
            fillComplex128(bis, dims, buff.doubles);
            break;
         case NIFTI_TYPE_COMPLEX256:
            fillComplex256(bis, dims, buff.doubles);
            buff.dataType = DataType.NIFTI_TYPE_COMPLEX128;  // converted
            break;
         case NIFTI_TYPE_COMPLEX64:
            fillComplex64(bis, dims, buff.floats);
            break;
         case NIFTI_TYPE_FLOAT128:
            fillFloat128(bis, dims, buff.doubles);
            buff.dataType = DataType.NIFTI_TYPE_FLOAT64;   // converted
            break;
         case NIFTI_TYPE_FLOAT32:
            fillFloat32(bis, dims, buff.floats);
            break;
         case NIFTI_TYPE_FLOAT64:
            fillFloat64(bis, dims, buff.doubles);
            break;
         case NIFTI_TYPE_INT16:
         case NIFTI_TYPE_UINT16:
            fillInt16(bis, dims, buff.shorts);
            break;
         case NIFTI_TYPE_INT32:
         case NIFTI_TYPE_UINT32:
            fillInt32(bis, dims, buff.ints);
            break;
         case NIFTI_TYPE_INT64:
         case NIFTI_TYPE_UINT64:
            fillInt64(bis, dims, buff.longs);
            break;
         case NIFTI_TYPE_INT8:
         case NIFTI_TYPE_UINT8:
            fillInt8(bis, dims, buff.bytes);
            break;
         case NIFTI_TYPE_RGB24:
            fillRGB24(bis, dims, buff.bytes);
            break;
         case DT_UNKNOWN:
         default:
            throw new IOException("Cannot fill with an unknown data type");
         
      }

      return buff;
   }
   
}
