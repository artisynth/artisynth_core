/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import maspack.image.dicom.DicomElement.VR;

/**
 * Decodes images stored in raw form (non-encoded)
 * @author Antonio
 *
 */
public class DicomImageDecoderRaw extends DicomImageDecoderBase {

      
   public DicomPixelBuffer decodeFrame(DicomHeader header, DicomPixelData data) {

      DicomPixelBufferBase out = null;

      // read all important info
      int nSamples = header.getIntValue(DicomTag.SAMPLES_PER_PIXEL, 1);
      int planarConf = header.getIntValue(DicomTag.PLANAR_CONFIGURATION, 0); // 0: interlaced
      int rows = header.getIntValue(DicomTag.ROWS, 0);
      int cols = header.getIntValue(DicomTag.COLUMNS, 0);

      int bitsAllocated = header.getIntValue(DicomTag.BITS_ALLOCATED, 8);
      int bitsStored = header.getIntValue(DicomTag.BITS_STORED, 8);
      int highBit = header.getIntValue(DicomTag.HIGH_BIT, 7);

      int diffBits = highBit+1-bitsStored;
      int maxMask = (1 << bitsStored)-1;

      int pixelRepresentation = header.getIntValue(DicomTag.PIXEL_REPRESENTATION, 0);  // 0: unsigned, 1: signed
      double rescaleSlope = header.getDecimalValue(DicomTag.RESCALE_SLOPE, 1);
      double rescaleIntercept = header.getDecimalValue(DicomTag.RESCALE_INTERCEPT, 0);

      // RGB, MONOCHROME1, MONOCHROME2
      String photoInterp = header.getStringValue(DicomTag.PHOTOMETRIC_ITERPRETATION);
      boolean flipGrayscale = ("MONOCHROME1".equalsIgnoreCase(photoInterp));

      if (nSamples == 1) {
         // single byte
         if (bitsAllocated == 8) {

            if (data.vr != VR.OB) {
               throw new IllegalArgumentException("Sequence type does not match header description");
            }

            int frameLength = nSamples*rows*cols;
            if (pixelRepresentation == 0) {
               out = new UBytePixelBuffer(frameLength);
            } else {
               out = new BytePixelBuffer(frameLength);
            }
            byte[] buff = (byte[])out.getBuffer();

            for (int i=0; i<frameLength; i++) {
               buff[i] = data.b[i];

               // adjust for mis-matched high bit?
               if (diffBits > 0) {
                  buff[i] = (byte)(buff[i] >>> diffBits);
               }
               // remove any outside bits
               buff[i] = (byte)( maxMask & buff[i] ); 

               // adjust monochrome
               if (flipGrayscale) {
                  buff[i] = (byte)( maxMask - (0xFF & buff[i]) );
               }
               
               // apply rescale slope/intercept
               out.setRescale(rescaleSlope, rescaleIntercept);
            }
         } else if (bitsAllocated == 16) {
         
            // short
            if (data.vr != VR.OW) {
               throw new IllegalArgumentException("Sequence type does not match header description");
            }
   
            // separate sequences into appropriate frames
            int frameLength = nSamples*rows*cols;
            if (pixelRepresentation == 0) {
               out = new UShortPixelBuffer(frameLength);
            } else {
               out = new ShortPixelBuffer(frameLength);
            }
            short[] buff = (short[])out.getBuffer();
   
            for (int i=0; i<frameLength; i++) {
               buff[i] = data.s[i];
   
               // adjust for mis-matched high bit?
               if (diffBits > 0) {
                  buff[i] = (short)(buff[i] >>> diffBits);
               }
               // remove any outside bits
               buff[i] = (short)( maxMask & buff[i] ); 
   
               // adjust monochrome
               if (flipGrayscale) {
                  buff[i] = (short)( maxMask - (0xFFFF & buff[i]) );
               }
               
               // rescale
               out.setRescale(rescaleSlope, rescaleIntercept);
            }
         } else {
            throw new IllegalArgumentException("Only support one- or two-byte monochrome pixels");
         }
      } else if (nSamples == 3) {
         // RGB
         if (data.vr != VR.OB) {
            throw new IllegalArgumentException("Sequence type does not match header description");
         }
         if (bitsAllocated != 8) {
            throw new IllegalArgumentException("Only one-byte RGB implemented");
         }

         // separate sequences into appropriate frames
         int frameLength = nSamples*rows*cols;
         out = new RGBPixelBuffer(3*frameLength);
         byte[] buff = (byte[])out.getBuffer();
         int idx = 0;
         byte[] rgb = new byte[3];
         
         for (int i=0; i<frameLength; i++) {
            
            for (int j = 0; j<3; j++) {
               rgb[j] = data.b[idx++];
               // adjust for mis-matched high bit?
               if (diffBits > 0) {
                  rgb[j] = (byte)(rgb[j] >>> diffBits);
               }
               // remove any outside bits
               rgb[j] = (byte)( maxMask & rgb[j] );
            }

            if (planarConf == 1) {
               buff[i] = rgb[0];
               buff[i+frameLength] = rgb[1];
               buff[i+2*frameLength] = rgb[2];
            } else {
               buff[3*i] = rgb[0];
               buff[3*i+1] = rgb[1];
               buff[3*i+2] = rgb[2];
            }
         }
         

      } else {
         throw new IllegalArgumentException("Only 1-byte and 3-byte samples implemented");
      }


      return out;
   }

   @Override
   protected boolean canDecodeFrames(DicomHeader header) {

      DicomTransferSyntax syntax = header.getTransferSyntax();
      if (syntax != null && !syntax.encoded) {
         return true;
      }

      return false;
   }

}
