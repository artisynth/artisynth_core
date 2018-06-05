/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.List;

import gdcm.Bitmap;
import gdcm.Image;
import gdcm.ImageChangePhotometricInterpretation;
import gdcm.ImageReader;
import gdcm.LookupTable;
import gdcm.PhotometricInterpretation;
import gdcm.PixelFormat;
import maspack.fileutil.NativeLibraryManager;
import maspack.image.dicom.DicomPixelBuffer.PixelType;
import maspack.util.BinaryFileInputStream;

/**
 * Relies on the GDCM library to decode an image slice 
 */
public class DicomImageDecoderGDCM implements DicomImageDecoder {
 
   static boolean loadedNativeLibrary = false;
   
   /**
    * Creates a GDCM decoder
    */
   public DicomImageDecoderGDCM() {
      if (!loadedNativeLibrary) {
         NativeLibraryManager.load("gdcmjni");
         loadedNativeLibrary = true;
      }
   }

   public boolean isValid() {
      return true;
   }


   public boolean canDecode(DicomHeader header) {

      DicomTransferSyntax dts = header.getTransferSyntax();

      if (dts.uid.equals("1.2.840.10008.1.2.4.90")
         || dts.uid.equals("1.2.840.10008.1.2.4.91")
         || dts.uid.equals("1.2.840.10008.1.2.4.92")
         || dts.uid.equals("1.2.840.10008.1.2.4.93")
         || dts.uid.equals("1.2.840.10008.1.2.4.70")) {
         return true;
      } else {
         System.out.println("GDCM: unknown transfer syntax '" + dts.uid + 
            "', attempting to decode anyways...");
      }
      
      return true;
   }
   
   public static byte[] GetAsByte(Image input) {
      long len = input.GetBufferLength();
      byte[] buffer = new byte[(int)len];
      PhotometricInterpretation pi = input.GetPhotometricInterpretation();
      if (pi.GetType() == PhotometricInterpretation.PIType.MONOCHROME1) {
         ImageChangePhotometricInterpretation icpi =
            new ImageChangePhotometricInterpretation();
         icpi.SetInput(input);
         icpi.SetPhotometricInterpretation(
            new PhotometricInterpretation(
               PhotometricInterpretation.PIType.MONOCHROME2));
         if (icpi.Change()) {
            Bitmap output = icpi.GetOutput();
            output.GetArray(buffer);
         }
         return buffer;
      } else {
         input.GetArray(buffer);
         return buffer;
      }
   }

   public static short[] GetAsShort(Image input) {
      long len = input.GetBufferLength(); // length in bytes
      short[] buffer = new short[(int)len / 2];
      PhotometricInterpretation pi = input.GetPhotometricInterpretation();
      if (pi.GetType() == PhotometricInterpretation.PIType.MONOCHROME1) {
         ImageChangePhotometricInterpretation icpi =
            new ImageChangePhotometricInterpretation();
         icpi.SetInput(input);
         icpi.SetPhotometricInterpretation(
            new PhotometricInterpretation(
               PhotometricInterpretation.PIType.MONOCHROME2));
         if (icpi.Change()) {
            Bitmap output = icpi.GetOutput();
            output.GetArray(buffer);
         }
         return buffer;
      } else {
         input.GetArray(buffer);
         return buffer;
      }
   }

   @Override
   public boolean decodeFrames(
      DicomHeader header, BinaryFileInputStream bin,
      List<DicomPixelBuffer> frames)  throws IOException {
      
      
      // modified from https://github.com/malaterre/GDCM/blob/master/Examples/Java/ScanDirectory.java
      ImageReader ir = new ImageReader();
      ir.SetFileName(bin.getFile().getAbsolutePath());
      
      // re-open file and read image
      if (ir.Read()) {
         Image img = ir.GetImage();
         
         PixelFormat pf = img.GetPixelFormat();
         PhotometricInterpretation pi = img.GetPhotometricInterpretation();

         ColorModel colorModel = null;
         PixelType pixelType = null;
         
         if (pf.GetSamplesPerPixel() == 1) {
            if (pi.GetType() == PhotometricInterpretation.PIType.MONOCHROME1
               || pi.GetType() == PhotometricInterpretation.PIType.MONOCHROME2) {
               
               PixelFormat.ScalarType stype = pf.GetScalarType();
               if (pf.GetScalarType() == PixelFormat.ScalarType.UINT8) {
                  pixelType = PixelType.UBYTE;
               } else if (pf.GetScalarType() == PixelFormat.ScalarType.INT8) {
                  pixelType = PixelType.BYTE;
               } else if (pf.GetScalarType() == PixelFormat.ScalarType.UINT12
                  || pf.GetScalarType() == PixelFormat.ScalarType.UINT16) {
                  pixelType = PixelType.USHORT;
               } else if (stype == PixelFormat.ScalarType.INT12
                  || pf.GetScalarType() == PixelFormat.ScalarType.INT16) {
                  pixelType = PixelType.SHORT;
               }
            } else if (pi.GetType() == PhotometricInterpretation.PIType.PALETTE_COLOR) {
               LookupTable lut = img.GetLUT();
               long rl = lut.GetLUTLength(LookupTable.LookupTableType.RED);
               byte[] rbuf = new byte[(int)rl];
               long rl2 = lut.GetLUT(LookupTable.LookupTableType.RED, rbuf);
               assert rl == rl2;
               long gl = lut.GetLUTLength(LookupTable.LookupTableType.GREEN);
               byte[] gbuf = new byte[(int)gl];
               long gl2 = lut.GetLUT(LookupTable.LookupTableType.GREEN, gbuf);
               assert gl == gl2;
               long bl = lut.GetLUTLength(LookupTable.LookupTableType.BLUE);
               byte[] bbuf = new byte[(int)bl];
               long bl2 = lut.GetLUT(LookupTable.LookupTableType.BLUE, bbuf);
               assert bl == bl2;
               colorModel = new IndexColorModel(8, (int)rl, rbuf, gbuf, bbuf);
            }
         } else if (pf.GetSamplesPerPixel() == 3) {
            if (pf.GetScalarType() == PixelFormat.ScalarType.UINT8) {
               pixelType = PixelType.UBYTE_RGB;
            }
         }

         int width = (int)(img.GetDimension(0));
         int height = (int)(img.GetDimension(1));
         int slices = (int)(img.GetDimension(2));
         
         double rescaleSlope = header.getDecimalValue(DicomTag.RESCALE_SLOPE, 1);
         double rescaleIntercept = header.getDecimalValue(DicomTag.RESCALE_INTERCEPT, 0);
         
         // System.out.println("Image: " + width + " x " + height + " x " + slices);
         
         if (pixelType == PixelType.UBYTE) {
            byte[] buffer = GetAsByte(img);
            // System.out.println("Buffer length: " + buffer.length);
           
            // separate into frames
            int slicelen = width*height;
            for (int i=0; i<slices; ++i) {
               DicomPixelBufferBase buff = new UBytePixelBuffer(buffer, i*slicelen, slicelen);
               // apply rescale slope/intercept
               buff.setRescale(rescaleSlope, rescaleIntercept);
               frames.add(buff);
            }
         } else if (pixelType == PixelType.BYTE) {
            byte[] buffer = GetAsByte(img);
            // System.out.println("Buffer length: " + buffer.length);
           
            // separate into frames
            int slicelen = width*height;
            for (int i=0; i<slices; ++i) {
               DicomPixelBufferBase buff = new BytePixelBuffer(buffer, i*slicelen, slicelen);
               // apply rescale slope/intercept
               buff.setRescale(rescaleSlope, rescaleIntercept);
               frames.add(buff);
            }
         } else if (pixelType == PixelType.UBYTE_RGB) {
            byte[] buffer = GetAsByte(img);
            // System.out.println("Buffer length: " + buffer.length);
           
            // separate into frames
            int slicelen = width*height*3;
            for (int i=0; i<slices; ++i) {
               DicomPixelBufferBase buff = new RGBPixelBuffer(buffer, i*slicelen, slicelen);
               buff.setRescale(rescaleSlope, rescaleIntercept);
               frames.add(buff);
            }
         } else if (pixelType == PixelType.USHORT) {
            short[] buffer = GetAsShort(img);
            // System.out.println("Buffer length: " + buffer.length);
            
            int slicelen = width*height;
            for (int i=0; i<slices; ++i) {
               DicomPixelBufferBase buff = new UShortPixelBuffer(buffer, i*slicelen, slicelen);
               buff.setRescale(rescaleSlope, rescaleIntercept);
               frames.add(buff);
            }
         } else if (pixelType == PixelType.SHORT) {
            short[] buffer = GetAsShort(img);
            // System.out.println("Buffer length: " + buffer.length);
            
            int slicelen = width*height;
            for (int i=0; i<slices; ++i) {
               DicomPixelBufferBase buff = new ShortPixelBuffer(buffer, i*slicelen, slicelen);
               buff.setRescale(rescaleSlope, rescaleIntercept);
               frames.add(buff);
            }
         } else if (colorModel != null) {
            
            // convert to image then extract bytes
            int slicelen = width*height;
            BufferedImage bi = new BufferedImage(colorModel,
               colorModel.createCompatibleWritableRaster(width, height), false, null);
            byte[] buffer = GetAsByte(img);
            byte[] slice = new byte[slicelen];
            int[] rgb = new int[slicelen];
            
            for (int i=0; i<slices; ++i) {

               WritableRaster wr = bi.getRaster();
               for (int j=0; j<slicelen; ++j) {
                  slice[j] = buffer[slicelen*i+j];
               }               
               wr.setDataElements (0, 0, width, height, slice);

               // extract rgb
               bi.getRGB(0, 0, width, height, rgb, 0, width);
               
               // add buffer to frames
               DicomPixelBufferBase buff = new RGBPixelBuffer(rgb);
               buff.setRescale(rescaleSlope, rescaleIntercept);
               frames.add(buff);
            }
            
            // clear file
            ir.SetFile (null);
         } else {
            System.err.println("GDCM failed to detect type " + pi.GetString());
            return false;
         }
         
      } else {
         System.err.println("GDCM failed to read " + bin.getFile().getAbsolutePath());
         return false;
      }
      
      return true;
   }

}
