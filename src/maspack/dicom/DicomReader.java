package maspack.dicom;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import maspack.dicom.DicomElement.VR;
import maspack.threads.NamedThreadFactory;
import maspack.util.BinaryInputStream;
import maspack.util.FunctionTimer;

public class DicomReader {

   private ArrayList<DicomImageDecoder> imageDecoders;

   public DicomReader() {
      initializeDecoders();
   }

   private void initializeDecoders() {
      imageDecoders = new ArrayList<DicomImageDecoder>(3);
      imageDecoders.add(new DicomImageDecoderRaw()); // raw format
      imageDecoders.add(new DicomImageDecoderImageIO());
      // ImageMagick, if found
      DicomImageDecoderImageMagick dIM = new DicomImageDecoderImageMagick();
      if (dIM.isValid()) {
         imageDecoders.add(dIM); // ImageMagick
      } else {
         // ImageIO image decoder
   
      }
   }

   public void addImageDecoder(DicomImageDecoder decoder) {
      addImageDecoderFirst(decoder);
   }

   public void addImageDecoderFirst(DicomImageDecoder decoder) {
      imageDecoders.add(0, decoder);
   }

   public void addImageDecoderLast(DicomImageDecoder decoder) {
      imageDecoders.add(decoder);
   }

   private class SliceReaderCallable implements Callable<DicomSlice[]> {

      private BinaryInputStream bis;
      private String sliceName;

      public SliceReaderCallable(File file) {

         sliceName = file.getName();

         try {
            bis =
               new BinaryInputStream(
                  new BufferedInputStream(new FileInputStream(file)));
         }
         catch (FileNotFoundException e) {
            bis = null;
         }

      }

      @Override
      public DicomSlice[] call() throws IOException {
         return readSlice(sliceName, bis);
      }

   }

   public DicomImage read(
      DicomImage im, String directory, Pattern filePattern,
      boolean checkSubdirectories) throws IOException {

      return read(im, directory, filePattern, checkSubdirectories, -1);
   }

   public DicomImage read(
      DicomImage im, String directory, Pattern filePattern,
      boolean checkSubdirectories, int temporalPosition) throws IOException {

      // load in directory
      File dir = new File(directory);

      ArrayList<File> fileList =
         getAllFiles(dir, filePattern, checkSubdirectories);

      if (fileList == null) {
         throw new IOException(
            "No valid image files in directory '" + directory + "'");
      }

      int cpus = Runtime.getRuntime().availableProcessors();

      ExecutorService executor =
         Executors
            .newFixedThreadPool(cpus, new NamedThreadFactory("dicom_reader"));
      LinkedList<Future<DicomSlice[]>> sliceReaders =
         new LinkedList<Future<DicomSlice[]>>();
      ExecutorCompletionService<DicomSlice[]> ecs =
         new ExecutorCompletionService<DicomSlice[]>(executor);

      int nReaders = 0;
      for (int i = 0; i < fileList.size(); i++) {
         SliceReaderCallable reader = new SliceReaderCallable(fileList.get(i));
         sliceReaders.add(ecs.submit(reader));
         nReaders++;
      }
      executor.shutdown();

      FunctionTimer timer = new FunctionTimer();
      timer.start();

      // process futures as they come in
      int nProcessed = 0;
      while (nProcessed < nReaders) {

         Future<DicomSlice[]> fut = null;
         try {
            fut = ecs.take();
         }
         catch (InterruptedException e1) {
            e1.printStackTrace();
            return null;
         }
         if (fut.isDone()) {
            nProcessed++;

            // process
            try {
               DicomSlice[] slices = fut.get();
               if (slices != null) {
                  // split up into frames
                  for (int i = 0; i < slices.length; i++) {
                     int stime = temporalPosition;
                     if (stime < 0) {
                        // attempt to read time from slice
                        int[] vals =
                           slices[i].getHeader().getMultiIntValue(
                              DicomTag.TEMPORAL_POSITON_IDENTIFIER);
                        if (vals != null) {
                           stime = vals[0];
                        }
                        else {
                           // set unknown time
                           stime = 0;
                        }
                     }
                     slices[i].info.temporalPosition = stime;
                     if (im == null) {
                        im = new DicomImage(dir.getName(), slices[i]);
                     }
                     else {
                        im.addSlice(slices[i]);
                     }
                  }
               }
            }
            catch (Exception e) {
               e.printStackTrace();
            } // end try-catching errors
         } // end checking if future complete
      } // end main loop

      timer.stop();
      double usec = timer.getTimeUsec();
      System.out.println("Read took " + usec * 1e-6 + " seconds");

      if (im.title == null) {
         im.title = dir.getName();
      }

      return im;
   }

   private ArrayList<File> getAllFiles(
      File root, Pattern filePattern, boolean recursive) {

      ArrayList<File> out = new ArrayList<File>();

      File[] subFiles = root.listFiles();
      for (File sf : subFiles) {
         if (recursive && sf.isDirectory()) {
            out.addAll(getAllFiles(sf, filePattern, recursive));
         }
         else if (sf.isFile()) {
            // check that it passes the supplied pattern
            if (filePattern != null) {
               // check unix and windows file patterns
               if (filePattern.matcher(sf.getAbsolutePath().replace('\\', '/')).matches()) {
                  out.add(sf);
               } else if (filePattern.matcher(sf.getAbsolutePath().replace('/', '\\')).matches()) {
                  out.add(sf);
               }
            }
         }
      }

      return out;

   }

   public DicomSlice[] readSlice(String sliceTitle, BinaryInputStream in)
      throws IOException {

      // DICOM specifies little endian to start, single byte
      in.setLittleEndian(true);
      in.setByteChar(true);

      // HEADER INFO
      // check first characters, look for DICOM header
      char[] c4 = new char[4];

      c4[0] = (char)in.readByte();
      if (c4[0] != 'D') {
         // 128 header bytes
         in.skip(127);
         c4[0] = (char)in.readByte();
      }

      DicomHeader header = new DicomHeader();

      // ensure format
      c4[1] = (char)in.readByte();
      c4[2] = (char)in.readByte();
      c4[3] = (char)in.readByte();

      if (c4[0] != 'D' || c4[1] != 'I' || c4[2] != 'C' || c4[3] != 'M') {
         throw new IOException(
            "Couldn't find 'DICM' identifer, found: " + c4[0] + c4[1] + c4[2]
               + c4[3]);
      }

      short[] s = new short[2];
      s[0] = in.readShort();
      s[1] = in.readShort();
      int tagId = toTagId(s[0], s[1]);

      boolean littleEnd = false;
      boolean explicit = true;
      boolean past0002 = false;

      // read until we hit data
      while (tagId != DicomTag.PIXEL_DATA) {
         DicomElement elem;

         if (!past0002 || explicit) {
            // Type:
            char c0 = (char)in.readByte();
            char c1 = (char)in.readByte();
            elem = readExplicitElement(tagId, c0, c1, in);
         }
         else {
            elem = readImplicitElement(tagId, in);
         }
         header.addInfo(tagId, elem);

         // Find transfer syntax
         if (tagId == DicomTag.TRANSFER_SYNTAX_UID) {
            String uid = (String)elem.value;
            DicomTransferSyntax ts = DicomHeader.getTransferSyntax(uid);
            if (ts == null) {
               System.err
                  .println("Warning: unknown transfer syntax '" + uid + "'");
               ts = new DicomTransferSyntax(uid);
            }

            littleEnd = ts.littleEndian;
            explicit = ts.explicit;
         }

         // queue up next tag
         s[0] = in.readShort();
         s[1] = in.readShort();
         tagId = toTagId(s[0], s[1]);

         // outside file header, might need to switch
         // endian-ness and explicit-ness
         if (!past0002 && (s[0] & 0xFFFF) != 0x0002) {

            past0002 = true;
            if (!littleEnd) {
               // switch to big endian
               in.setLittleEndian(false);
               // flip around tagId
               s[0] = flipBytes(s[0]);
               s[1] = flipBytes(s[1]);
               tagId = toTagId(s[0], s[1]);
            }
         }
      }

      // parse pixel data
      if (tagId == DicomTag.PIXEL_DATA) {

         DicomPixelBuffer[] pixels = parsePixels(header, in);
         DicomSlice[] out = new DicomSlice[pixels.length];

         // split up into frames
         for (int i = 0; i < pixels.length; i++) {
            String title = sliceTitle;
            if (pixels.length > 1) {
               title = sliceTitle + "_" + i;
            }

            // XXX adjust frame header
            // DicomHeader sliceHeader = header.clone();

            out[i] = new DicomSlice(title, header, pixels[i]);
         }

         return out;
      }

      return null;
   }

   public DicomImage read(
      DicomImage dcm, String sliceTitle, BinaryInputStream in)
         throws IOException {

      DicomSlice[] slices = readSlice(sliceTitle, in);

      if (slices != null) {
         // split up into frames
         for (int i = 0; i < slices.length; i++) {
            if (dcm == null) {
               dcm = new DicomImage(sliceTitle, slices[i]);
            }
            else {
               dcm.addSlice(slices[i]);
            }
         }

      }

      return dcm;

   }

   private short flipBytes(short s) {
      short nybb1 = (short)((s & 0xFF00) >>> 8);
      short nybb2 = (short)((s & 0x00FF) << 8);
      return (short)(nybb1 | nybb2);
   }

   private int toTagId(short t1, short t2) {
      int out = ((t1 & 0xFFFF) << 16) | (t2 & 0xFFFF);
      return out;
   }

   private DicomElement readImplicitElement(int tagId, BinaryInputStream in)
      throws IOException {
      VR implicitVR = DicomTag.getImplicitVR(tagId);

      if (implicitVR == null) {
         throw new IOException(
            "Unknown implicit VR for tag: " + Integer.toHexString(tagId));
      }

      return readElement(tagId, implicitVR, in, false);
   }

   protected DicomElement readElement(
      int tagId, VR vr, BinaryInputStream in, boolean explicit)
         throws IOException {

      int length = 0;
      if (vr == VR.OB || vr == VR.OW || vr == VR.OF || vr == VR.SQ
         || vr == VR.UN || vr == VR.UT) {

         // skip 2, followed by 4-byte integer length
         short reserved = in.readShort();
         if (reserved != 0) {
            System.err.println("DICOM error: reserved bits not zero");
         }
         length = in.readInt();

         switch (vr) {
            case OB: {
               // other byte
               if (length == 0xFFFFFFFF) {
                  throw new IOException(
                     "Undefined length for VR 'OB' not currently supported");
               }
               else {
                  byte[] b = new byte[length];
                  in.read(b);
                  return new DicomElement(tagId, VR.OB, b);
               }
            }
            case OW: {
               // other word
               if (length == 0xFFFFFFFF) {
                  throw new IOException(
                     "Undefined length for VR 'OW' not currently supported");
               }
               else {
                  int nw = length / 2;
                  short[] w = new short[nw];
                  for (int i = 0; i < nw; i++) {
                     w[i] = in.readShort();
                  }
                  return new DicomElement(tagId, VR.OW, w);
               }
            }
            case OF: {
               // other float string
               if (length == 0xFFFFFFFF) {
                  throw new IOException(
                     "Undefined length for VR 'OW' not currently supported");
               }
               else {
                  int nf = length / 4;
                  float[] f = new float[nf];
                  for (int i = 0; i < nf; i++) {
                     f[i] = in.readFloat();
                  }
                  return new DicomElement(tagId, VR.OF, f);
               }
            }
            case SQ: {
               // sequence
               Object data = null;
               if (length == 0xFFFFFFFF) {
                  data = readSequence(in, explicit);
               }
               else {
                  data = readSequence(in, length, explicit);
               }
               return new DicomElement(tagId, VR.SQ, data);
            }
            case UN: {
               // according to spec must be little-endian
               boolean oldEndian = in.isLittleEndian();
               // unknown
               Object data;
               if (length == 0xFFFFFFFF) {
                  throw new IOException(
                     "Undefined length for VR 'UN' not currently supported");
               }
               else {
                  data = readBytes(in, length);
               }
               in.setLittleEndian(oldEndian);
               return new DicomElement(tagId, VR.UN, data);

            }
            case UT: {
               // unlimited text
               if (length == 0xFFFFFFFF) {
                  throw new IOException(
                     "VR of UT cannot have undefined length");
               }
               String str = readString(in, length);
               return new DicomElement(tagId, VR.UT, str);
            }
            default:
               return null; // will never get here
         }

      }

      // for all other items, read length appropriately
      if (explicit) {
         length = in.readUnsignedShort();
      }
      else {
         length = in.readInt();
      }

      if (length == 0xFFFFFFFF) {
         // need to read to end of sequence delimination
         throw new IOException(
            "Undefined length not implemented for VR: " + vr);
      }

      if (vr == null) {
         System.err.println("huh?");
      }

      switch (vr) {
         case AE: {
            // Application entity
            String str = readString(in, length);
            return new DicomElement(tagId, VR.AE, str);
         }
         case AS: {
            // Age string
            String str = readString(in, length);
            return new DicomElement(tagId, VR.AS, str);
         }
         case AT: {
            // Attribute tag
            int nTags = length / 2; // multiplicity
            int[] tags = new int[nTags];
            for (int i = 0; i < nTags; i++) {
               short t1 = in.readShort();
               short t2 = in.readShort();
               tags[i] = toTagId(t1, t2);
            }
            return new DicomElement(tagId, VR.AT, tags);
         }
         case CS: {
            // Code string
            String str = readString(in, length);
            return new DicomElement(tagId, VR.CS, str);
         }
         case DA: {
            // Date, YYYYMMDD or YYYY.MM.DD
            String date = readDateString(in, length);
            return new DicomElement(tagId, VR.DA, date);
         }
         case DL: {
            // Encapsulation/Sequence delimiter
            throw new IOException("Unexpected delimiter element");
         }
         case DS: {
            // Decimal string
            String ds = readString(in, length).trim();
            return new DicomElement(tagId, VR.DS, ds);
         }
         case DT: {
            // Date time
            String str = readString(in, length);
            return new DicomElement(tagId, VR.DT, str);
         }
         case FD: {
            // Double-precision float
            int multiplicity = length / 8;
            double[] d = new double[multiplicity];
            for (int i = 0; i < multiplicity; i++) {
               d[i] = in.readDouble();
            }
            return new DicomElement(tagId, VR.FD, d);
         }
         case FL: {
            // Single-precision float
            int multiplicity = length / 4;
            float[] f = new float[multiplicity];
            for (int i = 0; i < multiplicity; i++) {
               f[i] = in.readFloat();
            }
            return new DicomElement(tagId, VR.FL, f);
         }
         case IS: {
            // Integer string
            String i = readString(in, length).trim();
            return new DicomElement(tagId, VR.IS, i);
         }
         case LO: {
            // Long string
            String str = readString(in, length);
            return new DicomElement(tagId, VR.LO, str);
         }
         case LT: {
            // Long text
            String str = readString(in, length);
            return new DicomElement(tagId, VR.LT, str);
         }
         case PN: {
            // person names
            String nameGroupStr = readString(in, length);
            return new DicomElement(tagId, VR.PN, nameGroupStr);
         }
         case SH: {
            // short string
            String str = readString(in, length);
            return new DicomElement(tagId, VR.SH, str);
         }
         case SL: {
            // signed long (32-bit)
            int multiplicity = length / 4;
            int[] si = new int[multiplicity];
            for (int i = 0; i < multiplicity; i++) {
               si[i] = in.readInt();
            }
            return new DicomElement(tagId, VR.SL, si);
         }
         case SS: {
            // signed short (16-bit)
            int multiplicity = length / 2;
            short[] ss = new short[multiplicity];
            for (int i = 0; i < multiplicity; i++) {
               ss[i] = in.readShort();
            }
            return new DicomElement(tagId, VR.SS, ss);
         }
         case ST: {
            // short text
            String str = readString(in, length);
            return new DicomElement(tagId, VR.ST, str);
         }
         case TM: {
            // time
            String str = readString(in, length).trim();
            return new DicomElement(tagId, VR.TM, str);
         }
         case UI: {
            // unique identifier
            String uid = readString(in, length);
            return new DicomElement(tagId, VR.UI, uid);
         }
         case UL: {
            // unsigned long
            int multiplicity = length / 4;
            int[] ui = new int[multiplicity];
            for (int i = 0; i < multiplicity; i++) {
               ui[i] = in.readInt();
            }
            return new DicomElement(tagId, VR.UL, ui);
         }
         case US: {
            // unsigned short
            int multiplicity = length / 2;
            short[] us = new short[multiplicity];
            for (int i = 0; i < multiplicity; i++) {
               us[i] = in.readShort();
            }
            return new DicomElement(tagId, VR.US, us);
         }
         default:
      }

      throw new IOException("Failed to read in VR: " + vr);
   }

   private DicomElement[] readSequence(BinaryInputStream in, boolean explicit)
      throws IOException {

      DicomElement[] items = new DicomElement[0];
      int numElements = 0;

      boolean doneSequence = false;
      while (!doneSequence) {

         DicomElement item = readSequenceItem(in, explicit);
         if (item.tagId == DicomTag.SEQUENCE_DELIMINATION) {
            doneSequence = true;
         }
         else if (item.tagId == DicomTag.ITEM_DELIMINATION) {
            // ignore
         }
         else {
            // expand array
            numElements++;
            DicomElement[] oldOut = items;
            items = new DicomElement[numElements];
            for (int i = 0; i < numElements - 1; i++) {
               items[i] = oldOut[i];
            }
            items[numElements - 1] = item;
         }

      }

      return items;
   }

   private DicomElement[] readSequence(
      BinaryInputStream in, int length, boolean explicit) throws IOException {

      DicomElement[] items = new DicomElement[0];
      int numElements = 0;
      int start = in.getByteCount();

      int end = start + length;

      while (in.getByteCount() < end) {

         DicomElement item = readSequenceItem(in, explicit);
         // System.out.println(item.toString());
         if (item.tagId != DicomTag.ITEM_DELIMINATION) {
            // expand array
            numElements++;
            DicomElement[] oldOut = items;
            items = new DicomElement[numElements];
            for (int i = 0; i < numElements - 1; i++) {
               items[i] = oldOut[i];
            }
            items[numElements - 1] = item;
         }
      }

      return items;
   }

   private DicomElement readSequenceItem(BinaryInputStream in, boolean explicit)
      throws IOException {

      // Get tag/length
      short s0 = in.readShort();
      short s1 = in.readShort();
      int length = in.readInt();
      int tagId = toTagId(s0, s1);

      if (tagId == DicomTag.SEQUENCE_DELIMINATION) {
         return new DicomElement(tagId, VR.DL, null);
      }
      else if (tagId == DicomTag.ITEM) {

         // read item
         if (length == 0xFFFFFFFF) {

            ArrayList<DicomElement> elems = new ArrayList<DicomElement>(1);

            // read element of unknown size
            s0 = in.readShort();
            s1 = in.readShort();
            tagId = toTagId(s0, s1);

            while (tagId != DicomTag.ITEM_DELIMINATION) {
               if (explicit) {
                  char c0 = in.readChar();
                  char c1 = in.readChar();
                  DicomElement elem = readExplicitElement(tagId, c0, c1, in);
                  elems.add(elem);
               }
               else {
                  DicomElement elem = readImplicitElement(tagId, in);
                  elems.add(elem);
               }
               s0 = in.readShort();
               s1 = in.readShort();
               tagId = toTagId(s0, s1);
            }

            // read length from item delimination
            length = in.readInt();
            DicomElement item =
               new DicomElement(
                  DicomTag.ITEM, VR.DL,
                  elems.toArray(new DicomElement[elems.size()]));

            return item;

         }
         else {
            ArrayList<DicomElement> elems = new ArrayList<DicomElement>(1);

            // read element of known size
            int start = in.getByteCount();
            int end = start + length;

            while (in.getByteCount() < end) {

               // tag
               s0 = in.readShort();
               s1 = in.readShort();
               tagId = toTagId(s0, s1);

               // element
               if (explicit) {
                  char c0 = in.readChar();
                  char c1 = in.readChar();
                  DicomElement elem = readExplicitElement(tagId, c0, c1, in);
                  elems.add(elem);
               }
               else {
                  DicomElement elem = readImplicitElement(tagId, in);
                  elems.add(elem);
               }
            }
            DicomElement item =
               new DicomElement(
                  DicomTag.ITEM, VR.DL,
                  elems.toArray(new DicomElement[elems.size()]));

            return item;
         }
      }
      else {
         throw new IOException(
            "Invalid item tag: " + Integer.toHexString(tagId));
      }

   }

   private DicomElement readExplicitElement(
      int tagId, char c0, char c1, BinaryInputStream in) throws IOException {

      VR vr = VR.get(c0, c1);
      return readElement(tagId, vr, in, true);
   }

   private String readString(BinaryInputStream in, int len) throws IOException {

      if (len <= 0) {
         return "";
      }
      StringBuilder builder = new StringBuilder();
      char c;

      for (int i = 0; i < len - 1; i++) {
         c = (char)in.readByte();
         builder.append(c);
      }
      // potentially ignore null terminating character
      c = (char)in.readByte();
      if (c != 0) {
         builder.append(c);
      }

      return builder.toString();
   }

   private byte[] readBytes(BinaryInputStream in, int length)
      throws IOException {
      byte[] out = new byte[length];
      in.read(out);
      return out;
   }

   // remove periods if exist
   private String readDateString(BinaryInputStream in, int length)
      throws IOException {

      StringBuilder out = new StringBuilder();

      byte[] vals = new byte[length];
      in.read(vals);

      if (length == 8) {
         for (int i = 0; i < 8; i++) {
            out.append((char)vals[i]);
         }
      }
      else if (length == 10) {
         for (int i = 0; i < 4; i++) {
            out.append((char)vals[i]);
         }
         for (int i = 5; i < 7; i++) {
            out.append((char)vals[i]);
         }
         for (int i = 8; i < 10; i++) {
            out.append((char)vals[i]);
         }
      }
      else {
         throw new IOException("Illegal date length: " + length);
      }

      return out.toString();

   }

   private DicomPixelBuffer decodeFrame(DicomHeader header, DicomPixelData data)
      throws IOException {

      // find appropriate decoder
      for (DicomImageDecoder decoder : imageDecoders) {
         if (decoder.canDecode(header, data)) {
            return decoder.decode(header, data);
         }
      }
      throw new IOException(
         "Unable to find decoder for given image data.  Transfer syntax: "
            + header.getTransferSyntax().name);

   }

   private DicomPixelBuffer[] parsePixels(
      DicomHeader header, BinaryInputStream in) throws IOException {

      // check type
      char c0 = in.readChar();
      char c1 = in.readChar();
      in.skip(2); // reserved

      int nFrames = header.getIntValue(DicomTag.NUMBER_OF_FRAMES, 1);
      DicomPixelBuffer[] frames = new DicomPixelBuffer[nFrames];

      VR vr = VR.get(c0, c1);

      // length
      int length = in.readInt();

      // undefined length, must be encapsulated OB
      if (length == 0xFFFFFFFF) {
         vr = VR.OB;
         // first item is offset table, next are data frames

         // throw new IOException("Variable length not yet implemented");
         // read basic offset table
         int[] offsets = new int[nFrames];

         // read offset table
         short s0 = in.readShort();
         short s1 = in.readShort();
         int tagId = toTagId(s0, s1);
         if (tagId != DicomTag.ITEM) {
            throw new IOException("Expected item tag for offset table, found "
               + String.format("0x%08X", tagId));
         }
         length = in.readInt();
         if (length > 0) {
            // read in offsets
            int noffsets = length / 4;
            for (int i = 0; i < noffsets; i++) {
               offsets[i] = in.readInt();
            }
         }

         int offsetStart = in.getByteCount();

         for (int i = 0; i < nFrames; i++) {
            byte[] ob = null;

            boolean doneFrame = false;
            // read fragments until on to the next frame, or we hit the end
            // sequence tag
            while (!doneFrame) {
               // check start of item
               s0 = in.readShort();
               s1 = in.readShort();
               tagId = toTagId(s0, s1);
               length = in.readInt();
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
                  in.read(ob, offset, length);

               }
               else if (tagId == DicomTag.SEQUENCE_DELIMINATION) {
                  doneFrame = true;
               }
               else {
                  throw new IOException(
                     "Invalid tag in pixel data: "
                        + String.format("0x%08X", tagId));
               }

               // we're at the start of the next frame
               if ((i < nFrames - 1)
                  && (in.getByteCount() >= (offsetStart + offsets[i + 1]))) {
                  doneFrame = true;
               }
            }

            DicomPixelData data = new DicomPixelData(VR.OB, ob);
            frames[i] = decodeFrame(header, data);
         }

      }
      else {
         switch (vr) {
            case OB: {
               int frameLength = length / nFrames;
               for (int i = 0; i < nFrames; i++) {
                  DicomPixelData data = new DicomPixelData(vr, frameLength);
                  in.read(data.b);
                  frames[i] = decodeFrame(header, data);
               }
               break;
            }
            case OW: {
               int frameLength = length / nFrames / 2;
               for (int i = 0; i < nFrames; i++) {
                  DicomPixelData data = new DicomPixelData(vr, frameLength);
                  for (int j = 0; j < frameLength; j++) {
                     data.s[j] = in.readShort();
                  }
                  frames[i] = decodeFrame(header, data);
               }
               break;
            }
            case OF: {
               int frameLength = length / nFrames / 4;
               for (int i = 0; i < nFrames; i++) {
                  DicomPixelData data = new DicomPixelData(vr, frameLength);
                  for (int j = 0; j < frameLength; j++) {
                     data.f[j] = in.readFloat();
                  }
                  frames[i] = decodeFrame(header, data);
               }
               break;
            }
            default:
               throw new IOException("Invalid pixel data type: " + vr);
         }
      }

      return frames;
   }

}
