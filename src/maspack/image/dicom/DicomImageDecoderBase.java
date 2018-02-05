package maspack.image.dicom;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import maspack.image.dicom.DicomElement.VR;
import maspack.util.BinaryFileInputStream;

public abstract class DicomImageDecoderBase implements DicomImageDecoder {

   /**
    * Decode a frame given data
    * 
    * @param header
    * DICOM header
    * @param data
    * raw data from pixel data item
    * @return decoded frame
    */
   public abstract DicomPixelBuffer decodeFrame(
      DicomHeader header, DicomPixelData data);

   private int toTagId(short t1, short t2) {
      int out = ((t1 & 0xFFFF) << 16) | (t2 & 0xFFFF);
      return out;
   }

   protected abstract boolean canDecodeFrames(DicomHeader header);

   @Override
   public boolean decodeFrames(
      DicomHeader header, BinaryFileInputStream bin, List<DicomPixelBuffer> frames) throws IOException {
      
      if (!canDecodeFrames(header)) {
         return false;
      }
      
      // mark in case we need to recover
      bin.mark(bin.available());
      
      int nFrames = header.getIntValue(DicomTag.NUMBER_OF_FRAMES, 1);
      
      DicomTransferSyntax dts = header.getTransferSyntax();
      
      //  check type
      VR vr = null;
      if (!dts.encoded && !dts.explicit && dts.littleEndian) {
         vr = VR.OW;
      } else {
         char c0 = bin.readChar();
         char c1 = bin.readChar();
         vr = VR.get(c0, c1);
         bin.skip(2); // reserved
      }

      // length
      int length = bin.readInt();

      // undefined length, must be encapsulated OB
      if (length == 0xFFFFFFFF) {
         vr = VR.OB;
         // first item is offset table, next are data frames
         // read basic offset table
         int[] offsets = new int[nFrames];

         // read offset table
         short s0 = bin.readShort();
         short s1 = bin.readShort();
         int tagId = toTagId(s0, s1);
         if (tagId != DicomTag.ITEM) {
            throw new IOException("Expected item tag for offset table, found "
               + String.format("0x%08X", tagId));
         }
         length = bin.readInt();
         if (length > 0) {
            // read in offsets
            int noffsets = length / 4;
            for (int i = 0; i < noffsets; i++) {
               offsets[i] = bin.readInt();
            }
         }

         int offsetStart = bin.getByteCount();

         for (int i = 0; i < nFrames; i++) {
            byte[] ob = null;

            boolean doneFrame = false;
            // read fragments until on to the next frame, or we hit the end
            // sequence tag
            while (!doneFrame) {
               // check start of item
               s0 = bin.readShort();
               s1 = bin.readShort();
               tagId = toTagId(s0, s1);
               length = bin.readInt();
               if (tagId == DicomTag.ITEM) {

                  // read fragment into buffer
                  int offset = 0;
                  if (ob == null) {
                     ob = new byte[length];
                  }
                  else {
                     offset = ob.length;
                     ob = Arrays.copyOf(ob, offset + length);
                  }
                  bin.read(ob, offset, length);

               }
               else if (tagId == DicomTag.SEQUENCE_DELIMINATION) {
                  doneFrame = true;
               }
               else {
                  bin.reset();
                  throw new IOException(
                     "Invalid tag in pixel data: "
                        + String.format("0x%08X", tagId));
               }

               // we're at the start of the next frame
               if ((i < nFrames - 1)
                  && (bin.getByteCount() >= (offsetStart + offsets[i + 1]))) {
                  doneFrame = true;
               }
            }

            DicomPixelData data = new DicomPixelData(VR.OB, ob);
            try {
               frames.add(decodeFrame(header, data));
            } catch (Exception e) {
               System.err.println("Decoding attempt failed: " + e.getMessage());
               bin.reset();
               return false;
            }
         }

      }
      else {
         switch (vr) {
            case OB: {
               int frameLength = length / nFrames;
               for (int i = 0; i < nFrames; i++) {
                  DicomPixelData data = new DicomPixelData(vr, frameLength);
                  bin.read(data.b);
                  try {
                     frames.add(decodeFrame(header, data));
                  } catch (Exception e) {
                     System.err.println("Decoding attempt failed: " + e.getMessage());
                     bin.reset();
                     return false;
                  }
               }
               break;
            }
            case OW: {
               int frameLength = length / nFrames / 2;
               for (int i = 0; i < nFrames; i++) {
                  DicomPixelData data = new DicomPixelData(vr, frameLength);
                  for (int j = 0; j < frameLength; j++) {
                     data.s[j] = bin.readShort();
                  }
                  try {
                     frames.add(decodeFrame(header, data));
                  } catch (Exception e) {
                     System.err.println("Decoding attempt failed: " + e.getMessage());
                     bin.reset();
                     return false;
                  }
               }
               break;
            }
            case OF: {
               int frameLength = length / nFrames / 4;
               for (int i = 0; i < nFrames; i++) {
                  DicomPixelData data = new DicomPixelData(vr, frameLength);
                  for (int j = 0; j < frameLength; j++) {
                     data.f[j] = bin.readFloat();
                  }
                  try {
                     frames.add(decodeFrame(header, data));
                  } catch (Exception e) {
                     System.err.println("Decoding attempt failed: " + e.getMessage());
                     bin.reset();
                     return false;
                  }
               }
               break;
            }
            default: {
               // invalid type
               System.err.println("Decoding attempt failed, invalid pixel data type: " + vr);
               bin.reset();
               return false;
            }
            
         }
      }

      return true;
   }

   

}
