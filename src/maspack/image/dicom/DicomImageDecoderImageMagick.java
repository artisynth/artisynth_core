/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import maspack.concurrency.NamedThreadFactory;
import maspack.util.BinaryInputStream;
import maspack.util.BitInputStream;
import maspack.util.ProcessMonitor;

/**
 * Relies on the ImageMagick command-line utilities to decode an image slice 
 * @author Antonio
 *
 */
public class DicomImageDecoderImageMagick extends DicomImageDecoderBase {

   private static final String DEFAULT_CONVERT_COMMAND = "convert";
   private static final String DEFAULT_IDENTIFY_COMMAND = "identify";

   private String convertCmd;
   private String identifyCmd;
   private ExecutorService executor;    // handling read thread

   public DicomImageDecoderImageMagick() {
      // more than one since many threads can use this
      executor = Executors.newCachedThreadPool(new NamedThreadFactory("ImageMagick"));    
      setCommands(DEFAULT_IDENTIFY_COMMAND, DEFAULT_CONVERT_COMMAND);
   }

   public boolean isValid() {
      return (convertCmd != null && identifyCmd != null);
   }

   /**
    * Creates a decoder provided the supplied conversion command
    */
   public DicomImageDecoderImageMagick(String identifyCmd, String convertCmd) {
      setCommands(identifyCmd, convertCmd);
   }

   /**
    * Set the command-line command to call ImageMagick's "identify" and
    * "convert" utilities.
    * 
    * @param identify
    * the base command for the identify utility (simply "identify" if it exists
    * in your $PATH, otherwise the full path to the executable)
    */
   public void setCommands(String identify, String convert) {
      identifyCmd = findImageMagickCommand(identify);
      convertCmd = findImageMagickCommand(convert);
   }

   private String findImageMagickCommand(String cmd) {

      String[] cmdArray = new String[2];
      cmdArray[0] = cmd;
      cmdArray[1] = "--version";

      boolean found = false;

      // looking for ImageMagick in the output
      final String imStr = "ImageMagick";
      found = didContainOutput(cmdArray, imStr);
      if (found) {
         return cmdArray[0];
      }

      // check paths in environment variable
      String path = System.getenv("PATH");
      String[] pathsExpanded = path.split(File.pathSeparator);

      for (int i = 0; i < pathsExpanded.length; i++) {
         cmdArray[0] = new File(pathsExpanded[i], cmd).getAbsolutePath();
         found = didContainOutput(cmdArray, imStr);
         if (found) {
            return cmdArray[0];
         }
      }
      return null;

   }

   private boolean didContainOutput(String[] cmdArray, String lineContent) {
      return didContainOutput(executor,  cmdArray, lineContent);
   }
    
   private static boolean didContainOutput(ExecutorService executor, 
      String[] cmdArray, String lineContent) {

      ProcessBuilder procBuild = new ProcessBuilder(cmdArray);
      try {
         Process proc = procBuild.start();

         ProcessMonitor procMon = new ProcessMonitor(proc);
         executor.execute(procMon);
         
         BufferedReader errorReader =
            new BufferedReader(new InputStreamReader(proc.getErrorStream()));
         BufferedReader outputReader =
            new BufferedReader(new InputStreamReader(proc.getInputStream()));

         // clear error
         boolean found = false;
         while (!procMon.isComplete()) {
            while (errorReader.read() >= 0) {}

            // look for desired output
            String line = null;
            while ((line = outputReader.readLine()) != null) {
               if (line.contains(lineContent)) {
                  found = true;
               }
            }
         }

         // last attempt for catching data
         while (errorReader.read() >= 0) {}
         String line = null;
         while ((line = outputReader.readLine()) != null) {
            if (line.contains(lineContent)) {
               found = true;
            }
         }

         return found;

      } catch (Exception e) {}

      return false;
   }
   
   /**
    * Checks for availability of ImageMagick on the system path
    * @return true if available
    */
   public static boolean checkForImageMagick() {
   
      String[] cmdArray = new String[2];
      String cmd = DEFAULT_CONVERT_COMMAND;
      cmdArray[0] = cmd;
      cmdArray[1] = "--version";

      boolean found = false;
      ExecutorService executor = Executors.newCachedThreadPool(
         new NamedThreadFactory("ImageMagick"));   

      // looking for ImageMagick in the output
      final String imStr = "ImageMagick";
      found = didContainOutput(executor, cmdArray, imStr);
      if (found) {
         executor.shutdown();
         return true;
      }

      // check paths in environment variable
      String path = System.getenv("PATH");
      String[] pathsExpanded = path.split(File.pathSeparator);

      for (int i = 0; i < pathsExpanded.length; i++) {
         cmdArray[0] = new File(pathsExpanded[i], cmd).getAbsolutePath();
         found = didContainOutput(executor, cmdArray, imStr);
         if (found) {
            executor.shutdown();
            return true;
         }
      }
      
      executor.shutdown();
      return false;
      
   }

   @Override
   public DicomPixelBuffer decodeFrame(DicomHeader header, DicomPixelData data) {

      // System.out.println("Using imageMagick to decode image");
      
      if (convertCmd == null) {
         throw new IllegalArgumentException(
            "ImageMagick's \"convert\" command not found");
      }

      DicomPixelBufferBase out = null;

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

      // little endian by default
      String[] cmdArray = { convertCmd, "-", "-endian", "MSB", "<type>:-" };
      int OUTTYPE_IDX = 4;

      if (nSamples == 1) {
         cmdArray[OUTTYPE_IDX] = "gray:-";
         if ((bitsAllocated != 8) && !(bitsAllocated == 16)) {
            throw new IllegalArgumentException(
               "Decoder only supports 8- or 16-bit grayscale");
         }
      } else if (nSamples == 3) {
         cmdArray[OUTTYPE_IDX] = "rgb:-";
         if (bitsAllocated != 8) {
            throw new IllegalArgumentException(
               "Decoder only supports 8-bit RGB");
         }
      } else {
         throw new IllegalArgumentException(
            "Decoder only supports grayscale or RGB");
      }

      // need to read in byte buffer in case short not yet complete
      int frameLength = nSamples * rows * cols;
      int bufferLength = frameLength * (bitsAllocated >>> 3);
      byte[] bbuff = new byte[bufferLength];

      // run conversion process
      ProcessBuilder procBuild = new ProcessBuilder(cmdArray);
      StringBuilder errorMsg = new StringBuilder();
      try {
         Process proc = procBuild.start();

         ProcessMonitor procMon = new ProcessMonitor(proc);// ProcessMonitor.create(proc);
         executor.execute(procMon);
         
         BufferedReader errorReader =
            new BufferedReader(new InputStreamReader(proc.getErrorStream()));
         BinaryInputStream outputReader =
            new BinaryInputStream(proc.getInputStream());
         BufferedOutputStream inputWriter =
            new BufferedOutputStream(proc.getOutputStream());

         // send all input
         inputWriter.write(data.b);
         inputWriter.flush();
         inputWriter.close();

         int offset = 0;
         while (!procMon.isComplete()) {
            // read output
            boolean eatMore = true;
            while (eatMore) {
               int length =
                  outputReader.read(bbuff, offset, bufferLength - offset);
               if (length < 0 || (bufferLength == offset)) {
                  eatMore = false;
               } else {
                  offset = offset + length;
               }
            }

            // clear error stream
            int val;
            while ((val = errorReader.read()) >= 0) {
               errorMsg.append((char)val);
            }
         }

         // read last of data
         // clear error stream
         int val;
         while ((val = errorReader.read()) >= 0) {
            errorMsg.append((char)val);
         }

         // read output
         if (offset < bufferLength) {
            boolean eatMore = true;
            while (eatMore) {
               int length =
                  outputReader.read(bbuff, offset, bufferLength - offset);
               if (length < 0) {
                  eatMore = false;
               } else {
                  offset = offset + length;
               }
            }
         }
         // done reading
         outputReader.close();
         errorReader.close();

         bufferLength = offset;
         
         if (offset == 0 && errorMsg.length() > 0) {
            String err = errorMsg.toString();
            System.err.println(err);
            throw new IllegalArgumentException("Error from ImageMagick: " + err);
         }

      } catch (Exception e) {
         throw new IllegalStateException("Failed to decode image data", e);
      }

      // buffer is full, determine how many actual bits per sample in decoded
      // stream
      int nTrueBitsPerSample = (bufferLength << 3) / frameLength;
      BitInputStream bitStream =
         new BitInputStream(new ByteArrayInputStream(bbuff));

      try {
         // fix up values
         if (nSamples == 1) {

            // single byte grayscale
            if (bitsAllocated == 8) {
               if (pixelRepresentation == 0) {
                  out = new UBytePixelBuffer(frameLength);
               } else {
                  out = new BytePixelBuffer(frameLength);
               }
               byte[] buff = (byte[])out.getBuffer();

               for (int i = 0; i < frameLength; i++) {

                  // adjust for mis-matched high bit?
                  buff[i] = (byte)bitStream.readBits(nTrueBitsPerSample);
                  if (diffBits > 0) {
                     buff[i] = (byte)(buff[i] >>> diffBits);
                  }
                  // remove any outside bits
                  buff[i] = (byte)(maxMask & buff[i]);

                  // adjust monochrome
                  if (flipGrayscale) {
                     buff[i] = (byte)(maxMask - (0xFF & buff[i]));
                  }
                  
                  // rescale
                  out.setRescale(rescaleSlope, rescaleIntercept);
               }
            } else if (bitsAllocated == 16) {

               // short
               // separate sequences into appropriate frames
               if (pixelRepresentation == 0) {
                  out = new UShortPixelBuffer(frameLength);
               } else {
                  out = new ShortPixelBuffer(frameLength);
               }
               short[] buff = (short[])out.getBuffer();

               for (int i = 0; i < frameLength; i++) {

                  // convert little-endian bytes
                  buff[i] = (short)bitStream.readBits(nTrueBitsPerSample);

                  // adjust for mis-matched high bit?
                  if (diffBits > 0) {
                     buff[i] = (short)(buff[i] >>> diffBits);
                  }
                  // remove any outside bits
                  buff[i] = (short)(maxMask & buff[i]);

                  // adjust monochrome
                  if (flipGrayscale) {
                     buff[i] = (short)(maxMask - (0xFFFF & buff[i]));
                  }

                  // rescale
                  out.setRescale(rescaleSlope, rescaleIntercept);
               }
            } else {
               throw new IllegalArgumentException(
                  "Only support one- or two-byte monochrome pixels");
            }
         } else if (nSamples == 3) {
            // RGB

            cmdArray[OUTTYPE_IDX] = "rgb:-";
            if (bitsAllocated != 8) {
               throw new IllegalArgumentException(
                  "Only one-byte RGB implemented");
            }

            // separate sequences into appropriate frames
            out = new RGBPixelBuffer(3 * frameLength);
            byte[] buff = (byte[])out.getBuffer();
            byte[] rgb = new byte[3];

            for (int i = 0; i < frameLength; i++) {

               for (int j = 0; j < 3; j++) {
                  rgb[j] = (byte)bitStream.readBits(nTrueBitsPerSample);
                  // adjust for mis-matched high bit?
                  if (diffBits > 0) {
                     rgb[j] = (byte)(rgb[j] >>> diffBits);
                  }
                  // remove any outside bits
                  rgb[j] = (byte)(maxMask & rgb[j]);
               }

               if (planarConf == 1) {
                  buff[i] = rgb[0];
                  buff[i + frameLength] = rgb[1];
                  buff[i + 2 * frameLength] = rgb[2];
               } else {
                  buff[3 * i] = rgb[0];
                  buff[3 * i + 1] = rgb[1];
                  buff[3 * i + 2] = rgb[2];
               }
            }

         } else {
            throw new IllegalArgumentException(
               "Only 1-byte and 3-byte samples implemented");
         }
      } catch (Exception e) {
         throw new RuntimeException("Something bad happened due to this: ", e);
      } finally {
         try {
            bitStream.close();
         } catch (IOException e) {}
      }

      return out;

   }

   @Override
   protected boolean canDecodeFrames(DicomHeader header) {

      if (identifyCmd == null || convertCmd == null) {
         return false;
      }

      // can only decode OB data?
      // if (data.vr != VR.OB) {
      //   return false;
      // }

      //      // send data to ImageMagick
      //      String[] identify = { identifyCmd, "-" }; // read from standard input
      //      
      //      ProcessBuilder procBuild = new ProcessBuilder(identify);
      //      try {
      //         Process proc = procBuild.start();
      //
      //         ProcessMonitor procMon = new ProcessMonitor(proc);
      //         executor.execute(procMon);
      //         
      //         BufferedReader errorReader =
      //            new BufferedReader(new InputStreamReader(proc.getErrorStream()));
      //         BufferedReader outputReader =
      //            new BufferedReader(new InputStreamReader(proc.getInputStream()));
      //         BufferedOutputStream inputWriter =
      //            new BufferedOutputStream(proc.getOutputStream());
      //
      //         // send all input
      //         inputWriter.write(bin.b);
      //         inputWriter.flush();
      //         inputWriter.close();
      //
      //         // clear error
      //         boolean success = true;
      //         while (!procMon.isComplete()) {
      //            // clear error stream
      //            while (errorReader.read() >= 0) {}
      //            // look for desired output
      //            String line = null;
      //            while ((line = outputReader.readLine()) != null) {
      //               if (line.contains("no decode delegate")) {
      //                  success = false;
      //               }
      //            }
      //         }
      //
      //         // read last of data
      //         // clear error stream
      //         while (errorReader.read() >= 0) {}
      //         // look for desired output
      //         String line = null;
      //         while ((line = outputReader.readLine()) != null) {
      //            if (line.contains("no decode delegate")) {
      //               success = false;
      //            }
      //         }
      //
      //         return success;
      //
      //      } catch (Exception e) {}
      //
      //      return false;
      
     return true;
   }
   
   @Override
   public void finalize() {
      executor.shutdown();
      // System.out.println("Executor complete");
   }

}
