/**
 * Copyright (c) 2026, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.image.dicom;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import gdcm.DataElement;
import gdcm.Image;
import gdcm.ImageChangeTransferSyntax;
import gdcm.ImageWriter;
import gdcm.PhotometricInterpretation;
import gdcm.PixelFormat;
import gdcm.Tag;
import gdcm.TransferSyntax;
import maspack.util.TestException;
import maspack.util.UnitTest;

/**
 * Tests DicomImageDecoderGDCM and the GDCM native library: writes a
 * synthetic multi-frame image (8 and 16 bit) with GDCM, uncompressed and
 * in several compressed transfer syntaxes, reads each file back through
 * DicomReader, and compares the pixels with the original values.
 *
 * <pre>
 * java maspack.image.dicom.DicomImageDecoderGDCMTest [library name]
 * </pre>
 *
 * The optional library name (e.g., gdcmjni.3.0.22.1) replaces the one
 * DicomImageDecoderGDCM loads, so that a new build of the library can be
 * tested before artisynth_core uses it.
 */
public class DicomImageDecoderGDCMTest extends UnitTest {

   static final int W = 64;
   static final int H = 48;
   static final int D = 5;

   static class Syntax {
      TransferSyntax.TSType type;
      String uid;
      boolean lossy;

      Syntax (TransferSyntax.TSType type, String uid, boolean lossy) {
         this.type = type;
         this.uid = uid;
         this.lossy = lossy;
      }
   }

   // created after the library is loaded, since gdcm.* needs it
   static Syntax[] createCompressedSyntaxes() {
      return new Syntax[] {
         new Syntax (TransferSyntax.TSType.JPEGLosslessProcess14_1,
                     "1.2.840.10008.1.2.4.70", false),
         new Syntax (TransferSyntax.TSType.JPEGLSLossless,
                     "1.2.840.10008.1.2.4.80", false),
         new Syntax (TransferSyntax.TSType.JPEG2000Lossless,
                     "1.2.840.10008.1.2.4.90", false),
         new Syntax (TransferSyntax.TSType.JPEG2000,
                     "1.2.840.10008.1.2.4.91", true),
         new Syntax (TransferSyntax.TSType.RLELossless,
                     "1.2.840.10008.1.2.5", false),
      };
   }

   File myDir;
   boolean verbose = false;

   /**
    * Pixel value at (i,j,k): a gradient plus a deterministic pattern, so that
    * the data is compressible but not trivial, and differs between frames.
    */
   static int pixelValue (int i, int j, int k, int bits) {
      int val = 3*i + 2*j + 17*k + ((i*7 + j*13 + k*5) % 17)*3;
      return bits == 8 ? val % 256 : 40*val;
   }

   static byte[] createPixels (int bits) {
      int bytes = bits/8;
      byte[] buf = new byte[bytes*W*H*D];
      int n = 0;
      for (int k=0; k<D; k++) {
         for (int j=0; j<H; j++) {
            for (int i=0; i<W; i++) {
               int val = pixelValue (i, j, k, bits);
               buf[n++] = (byte)(val & 0xff);
               if (bytes == 2) {
                  buf[n++] = (byte)((val >> 8) & 0xff);
               }
            }
         }
      }
      return buf;
   }

   static Image createImage (int bits) {
      Image img = new Image();
      img.SetNumberOfDimensions (3);
      img.SetDimension (0, W);
      img.SetDimension (1, H);
      img.SetDimension (2, D);
      img.SetPixelFormat (
         new PixelFormat (bits == 8 ?
            PixelFormat.ScalarType.UINT8 : PixelFormat.ScalarType.UINT16));
      img.SetPhotometricInterpretation (
         new PhotometricInterpretation (
            PhotometricInterpretation.PIType.MONOCHROME2));
      img.SetTransferSyntax (
         new TransferSyntax (TransferSyntax.TSType.ExplicitVRLittleEndian));
      byte[] pixels = createPixels (bits);
      DataElement pixelData = new DataElement (new Tag (0x7fe0, 0x0010));
      pixelData.SetArray (pixels, pixels.length);
      img.SetDataElement (pixelData);
      return img;
   }

   /**
    * Writes the image, compressed to the given syntax unless it is null.
    */
   File writeImage (Image img, Syntax syntax, String name) {
      if (syntax != null) {
         ImageChangeTransferSyntax change = new ImageChangeTransferSyntax();
         change.SetTransferSyntax (new TransferSyntax (syntax.type));
         change.SetInput (img);
         check ("compressing to " + syntax.type, change.Change());
         img = change.GetOutput();
      }
      File file = new File (myDir, name + ".dcm");
      ImageWriter writer = new ImageWriter();
      writer.SetFileName (file.getPath());
      writer.SetImage (img);
      check ("writing " + file, writer.Write());
      return file;
   }

   /**
    * Reads a file through DicomReader, which tries DicomImageDecoderGDCM
    * first, and checks its transfer syntax and pixels.
    */
   void readAndCheck (File file, String uid, int bits, int tol)
      throws IOException {
      DicomReader reader = new DicomReader();
      DicomImage im = reader.read (null, Arrays.asList (file));
      String fileUid = im.getSlice(0).getHeader().getTransferSyntax().uid;
      checkEquals (file.getName() + " transfer syntax", fileUid, uid);
      checkEquals (file.getName() + " columns", im.getNumCols(), W);
      checkEquals (file.getName() + " rows", im.getNumRows(), H);
      checkEquals (file.getName() + " slices", im.getNumSlices(), D);
      int maxDiff = 0;
      for (int k=0; k<D; k++) {
         for (int j=0; j<H; j++) {
            for (int i=0; i<W; i++) {
               int diff = (int)Math.abs (
                  im.getValue (0, i, j, k) - pixelValue (i, j, k, bits));
               maxDiff = Math.max (maxDiff, diff);
            }
         }
      }
      if (maxDiff > tol) {
         throw new TestException (
            file.getName() + ": pixels differ by up to " + maxDiff +
            ", expected at most " + tol);
      }
      if (verbose) {
         System.out.println ("  " + file.getName() + ": max diff " + maxDiff);
      }
   }

   void testImage (int bits) throws IOException {
      Image img = createImage (bits);
      String base = "image" + bits;
      readAndCheck (
         writeImage (img, null, base), "1.2.840.10008.1.2.1", bits, 0);
      for (Syntax syntax : createCompressedSyntaxes()) {
         String name = base + "_" + syntax.type;
         // lossy JPEG 2000: within 2% of the 8-bit range
         int tol = syntax.lossy ? (bits == 8 ? 5 : 5*256) : 0;
         readAndCheck (writeImage (img, syntax, name), syntax.uid, bits, tol);
      }
   }

   public void test() throws IOException {
      myDir = java.nio.file.Files.createTempDirectory ("gdcmtest").toFile();
      try {
         // loads the library, so that gdcm.* can be used
         new DicomImageDecoderGDCM();
         if (verbose) {
            System.out.println ("GDCM " + gdcm.Version.GetVersion());
         }
         testImage (8);
         testImage (16);
      }
      finally {
         for (File f : myDir.listFiles()) {
            f.delete();
         }
         myDir.delete();
      }
   }

   public static void main (String[] args) {
      DicomImageDecoderGDCMTest tester = new DicomImageDecoderGDCMTest();
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-verbose")) {
            tester.verbose = true;
         }
         else if (!args[i].startsWith ("-")) {
            DicomImageDecoderGDCM.nativeLibrary = args[i];
         }
         else {
            tester.printUsageAndExit ("[-verbose] [library name]");
         }
      }
      tester.runtest();
   }
}
