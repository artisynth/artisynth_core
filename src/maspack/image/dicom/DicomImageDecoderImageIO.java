/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import maspack.image.dicom.DicomElement.VR;
import maspack.util.BitInputStream;

/**
 * Relies on Java's ImageIO to decode DICOM slices
 * @author Antonio
 *
 */
public class DicomImageDecoderImageIO implements DicomImageDecoder {

   @Override
   public DicomPixelBuffer decode(DicomHeader header, DicomPixelData data) {
      
      DicomPixelBuffer out = null;

      // get dimensions, whether RGB or grayscale, and bit depth
      int nSamples = header.getIntValue(DicomTag.SAMPLES_PER_PIXEL, 1);
      int planarConf = header.getIntValue(DicomTag.PLANAR_CONFIGURATION, 0); // 0:
                                                                             // interlaced
      int rows = header.getIntValue(DicomTag.ROWS, 0);
      int cols = header.getIntValue(DicomTag.COLUMNS, 0);

      int bitsAllocated = header.getIntValue(DicomTag.BITS_ALLOCATED, 8);
      int bitsStored = header.getIntValue(DicomTag.BITS_STORED, 8);
      int highBit = header.getIntValue(DicomTag.HIGH_BIT, 7);

      int diffBits = highBit + 1 - bitsStored;
      int maxMask = (1 << bitsStored) - 1;

      int pixelRepresentation =
         header.getIntValue(DicomTag.PIXEL_REPRESENTATION, 0); // 0: unsigned,
                                                               // 1: signed
      double rescaleSlope = header.getDecimalValue(DicomTag.RESCALE_SLOPE, 1);
      double rescaleIntercept = header.getDecimalValue(DicomTag.RESCALE_INTERCEPT, 0);

      // RGB, MONOCHROME1, MONOCHROME2
      String photoInterp =
         header.getStringValue(DicomTag.PHOTOMETRIC_ITERPRETATION);
      boolean flipGrayscale = ("MONOCHROME1".equalsIgnoreCase(photoInterp));

      // need to read in byte buffer in case short not yet complete
      int frameLength = nSamples * rows * cols;
      int bufferLength = frameLength * (bitsAllocated >>> 3);
      byte[] bbuff = new byte[bufferLength];

      BufferedImage im = null;
      try {
         ByteArrayInputStream bais = new ByteArrayInputStream(data.b);
         ImageInputStream stream = ImageIO.createImageInputStream(bais);
         im = ImageIO.read(stream);
      } catch(Exception e) {
         throw new RuntimeException("Failed to read image using ImageIO", e);
      }
      Raster raster = im.getData();
      
      int nb = raster.getNumBands();
      int[] pixelArray = new int[nb];
      int minX = raster.getMinX();
      int minY = raster.getMinY();
      int width = raster.getWidth();
      int height = raster.getHeight();

         // fix up values
         if (nSamples == 1) {

            // single byte grayscale
            if (bitsAllocated == 8) {
               out = new BytePixelBuffer(frameLength);
               byte[] buff = (byte[])out.getBuffer();

               int pidx = 0;
               for (int x = 0; x < width; x++) {
                  for (int y = 0; y < height; y++) {
                     raster.getPixel(minX + x, minY + y, pixelArray);
                     
                     // convert bands to pixel buffer
                     byte p = (byte)pixelArray[0];
                     if (diffBits > 0) {
                        p = (byte)(p >>> diffBits);
                     }
                     
                     // remove any outside bits
                     p = (byte)(maxMask & p);

                     // adjust monochrome
                     if (flipGrayscale) {
                       p = (byte)(maxMask - (0xFF & p));
                     }
                     
                     // rescale
                     p = (byte)(p*rescaleSlope + rescaleIntercept);
                     buff[pidx++] = p;
                  }
               }
               
            } else if (bitsAllocated == 16) {
               // short
               out = new ShortPixelBuffer(frameLength);
               short[] buff = (short[])out.getBuffer();
               
               int pidx = 0;
               for (int x = 0; x < width; x++) {
                  for (int y = 0; y < height; y++) {
                     raster.getPixel(minX + x, minY + y, pixelArray);
                     
                     // convert bands to pixel buffer
                     short p = (short)pixelArray[0];
                     if (diffBits > 0) {
                        p = (byte)(p >>> diffBits);
                     }
                     
                     // remove any outside bits
                     p = (byte)(maxMask & p);

                     // adjust monochrome
                     if (flipGrayscale) {
                       p = (byte)(maxMask - (0xFF & p));
                     }
                     
                     // rescale
                     p = (byte)(p*rescaleSlope + rescaleIntercept);
                     buff[pidx++] = p;
                  }
               }
               
            } else {
               throw new IllegalArgumentException(
                  "Only support one- or two-byte monochrome pixels");
            }
         } else if (nSamples == 3) {
            
            // RGB
            if (bitsAllocated != 8) {
               throw new IllegalArgumentException(
                  "Only one-byte RGB implemented");
            }

            // separate sequences into appropriate frames
            out = new RGBPixelBuffer(3 * frameLength);
            byte[] buff = (byte[])out.getBuffer();
            byte[] rgb = new byte[3];

            int pidx = 0;
            for (int x = 0; x < width; x++) {
               for (int y = 0; y < height; y++) {
                  raster.getPixel(minX + x, minY + y, pixelArray);
                  
                  for (int k=0; k<3; k++) {
                     rgb[k] = (byte)pixelArray[k];
                     if (diffBits > 0) {
                        rgb[k] = (byte)(rgb[k] >>> diffBits);
                     }
                     // remove any outside bits
                     rgb[k] = (byte)(maxMask & rgb[k]);
                  }
                  
                  if (planarConf == 1) {
                     buff[pidx] = rgb[0];
                     buff[pidx + frameLength] = rgb[1];
                     buff[pidx + 2 * frameLength] = rgb[2];
                  } else {
                     buff[3 * pidx] = rgb[0];
                     buff[3 * pidx + 1] = rgb[1];
                     buff[3 * pidx + 2] = rgb[2];
                  }
                  pidx++;
               }
            }

         } else {
            throw new IllegalArgumentException(
               "Only 1-byte and 3-byte samples implemented");
         }

      return out;
      
   }

   @Override
   public boolean canDecode(DicomHeader header, DicomPixelData data) {

      if (data.getType() == VR.OB) {
      
         ByteArrayInputStream bais = new ByteArrayInputStream(data.getByteData());
         // create an image input stream from the specified file
         ImageInputStream iis = null;
         try {
            iis = ImageIO.createImageInputStream(bais);
            // get all currently registered readers that recognize the image format
            Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
      
            // no readers
            if (iter.hasNext()) {
            // get the first reader
               ImageReader reader = iter.next();
               System.out.println("Format: " + reader.getFormatName());
               
               return true;
            }
         }
         catch (IOException e) {
         } finally {
            // close stream
            try {
               if (iis != null) {
                  iis.close();
               }
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
  

      }
      
      return false;
   }

}
