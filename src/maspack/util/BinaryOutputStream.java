/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC),
 * Antonio Sanchez (UBC), Elliotte Harold
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.*;

/**
 * A data output stream class that can be set to convert its output from
 * big-endian to little-endian. Characters can also be specified to be
 * either word or byte length.
 *
 * The code is a mash-up of Elliotte Rusty Harold's LittleEndianOutputStream
 * and Antonio Sanchez's BinaryStreamReader.
 *
 * @author John E. Lloyd, Antonio Sanchez, Elliotte Harold, Dec 16, 2013
 */
public class BinaryOutputStream extends FilterOutputStream {

   private int myFlags = 0x00;
   private int myWriteCount = 0;
   
   public static final int LITTLE_ENDIAN = 0x01; // 1 for little, 0 for big
   public static final int BYTE_CHAR = 0x02;  // 2 for byte-char, 0 for word
   
   /**
    * Creates a new BinaryOutputStream from an output stream. Flag values are
    * set to their defaults so that the stream will be big-endian and use word
    * characters instead of byte characters.
    *
    * @param out underlying output stream
    */
   public BinaryOutputStream (OutputStream out) {
      this (out, 0);
   }

   /**
    * Creates a new BinaryOutputStream from an output stream and prescribed set
    * of flag values.
    *
    * @param out underlying output stream
    * @param flags flag values
    */
   public BinaryOutputStream (OutputStream out, int flags) {
      super(out);
      myWriteCount = 0;
      setFlags(flags);
   }

   /**
    * Sets the flags for this stream.
    *
    * @param flags new flag values
    */
   public void setFlags (int flags) {
      this.myFlags = flags;
   }

   /**
    * Gets the flags for this stream.
    *
    * @return flags for this stream
    */
   public int getFlags () {
      return myFlags;
   }         

   /**
    * Returns <code>true</code> if this stream converts it's output to
    * little-endian where appropriate.
    *
    * @return <code>true</code> if this stream converts to little-endian
    */
   public boolean isLittleEndian() {
      return ((myFlags & LITTLE_ENDIAN) != 0);
   }
   
   /**
    * Sets whether or not this stream converts its output to little-endian.
    *
    * @param little if <code>true</code>, enables little-endian conversion.
    */
   public void setLittleEndian (boolean little) {
      if (little) {
         myFlags = (myFlags | LITTLE_ENDIAN);
      }
      else {
         myFlags = (myFlags & ~LITTLE_ENDIAN);
      }
   }
   
   /**
    * Returns <code>true</code> if this stream uses byte-length characters as
    * opposed to 2-byte word characters.
    *
    * @return <code>true</code> if this stream uses byte-length characters
    */
   public boolean usesByteChar() {
      return ((myFlags & BYTE_CHAR) != 0);
   }
   
   /**
    * Sets whether or not this stream uses byte-length characters as opposed to
    * 2-byte word characters.
    *
    * @param bytechar if <code>true</code>, enables byte-length characters.
    */
   public void setByteChar(boolean bytechar) {
      if (bytechar) {
         myFlags = (myFlags | BYTE_CHAR);
      }
      else {
         myFlags = (myFlags & ~BYTE_CHAR);
      }
   }

   /**
    * Writes a single byte value to this stream.
    *
    * @param b byte value to write
    */
   public final void write (int b) throws IOException {
      out.write (b);
      myWriteCount++;
   }

   /**
    * Writes <code>len</code> byte values from <code>data</code>to this stream,
    * starting at <code>off</code>.
    *
    * @param data byte values to write
    * @param off starting point within <code>data</code>
    * @param len number of bytes to write
    */
   public final void write (byte[] data, int off, int len) throws IOException {
      out.write(data, off, len);
      myWriteCount += len;
   }

   /**
    * Writes a boolean value to this stream.
    *
    * @param b boolean value to write
    */
   public final void writeBoolean (boolean b) throws IOException {
      write (b ? 1 : 0);
   }

   /**
    * Writes a byte value to this stream.
    *
    * @param b byte value to write
    */
   public final void writeByte (int b) throws IOException {
      out.write (b);
      myWriteCount++;
   }

   /**
    * Writes a short value to this stream. If {@link #isLittleEndian} returns
    * <code>true</code>, then the byte order is switched from
    * big-endian to little-endian.
    *
    * @param s short value to write
    */
   public final void writeShort (int s) throws IOException {
      if (isLittleEndian()) {
         out.write (s & 0xFF);
         out.write ((s >>> 8) & 0xFF);         
      }
      else {
         out.write ((s >>> 8) & 0xFF); 
         out.write (s & 0xFF);
      }
      myWriteCount += 2;
   }

   /**
    * Writes a character value to this stream. The character is either one or
    * two bytes, depending on whether {@link #usesByteChar} returns
    * <code>true</code> or <code>false</code>. In the latter case, if {@link
    * #isLittleEndian} returns <code>true</code>, then the byte order is switched
    * from big-endian to little-endian.
    *
    * @param c character value to write
    */
   public final void writeChar (int c) throws IOException {
      if (usesByteChar()) {
         write (c);
      }
      else {
         writeShort (c);
      }
   }

   /**
    * Writes an integer value to this stream. If {@link #isLittleEndian} returns
    * <code>true</code>, then the byte order is switched from
    * big-endian to little-endian.
    *
    * @param v integer value to write
    */
   public final void writeInt (int v) throws IOException {
      if (isLittleEndian()) {
         out.write(v & 0xFF);
         out.write((v >>> 8) & 0xFF);
         out.write((v >>> 16) & 0xFF);
         out.write((v >>> 24) & 0xFF);
      }
      else {
         out.write((v >>> 24) & 0xFF);
         out.write((v >>> 16) & 0xFF);
         out.write((v >>> 8) & 0xFF);
         out.write(v & 0xFF);
      }
      myWriteCount += 4;
   }

   /**
    * Writes a long value to this stream. If {@link #isLittleEndian} returns
    * <code>true</code>, then the byte order is switched from
    * big-endian to little-endian.
    *
    * @param v long value to write
    */
   public final void writeLong (long v) throws IOException {
      if (isLittleEndian()) {
         out.write((int) v & 0xFF);
         out.write((int) (v >>> 8) & 0xFF);
         out.write((int) (v >>> 16) & 0xFF);
         out.write((int) (v >>> 24) & 0xFF);
         out.write((int) (v >>> 32) & 0xFF);
         out.write((int) (v >>> 40) & 0xFF);
         out.write((int) (v >>> 48) & 0xFF);
         out.write((int) (v >>> 56) & 0xFF);
      }
      else {
         out.write((int) (v >>> 56) & 0xFF);
         out.write((int) (v >>> 48) & 0xFF);
         out.write((int) (v >>> 40) & 0xFF);
         out.write((int) (v >>> 32) & 0xFF);
         out.write((int) (v >>> 24) & 0xFF);
         out.write((int) (v >>> 16) & 0xFF);
         out.write((int) (v >>> 8) & 0xFF);
         out.write((int) v & 0xFF);
      }
      myWriteCount += 8;
   }


   /**
    * Writes the bytes of a string to this stream.
    *
    * @param s string to write
    */
   public void writeBytes (String s) throws IOException {
      int len = s.length();
      for (int i = 0; i < len; i++) {
         out.write((byte) s.charAt(i));
      }
      myWriteCount += len;
   }

   /**
    * Writes the characters of a string to this stream. Characters are either
    * one or two bytes, depending on whether {@link #usesByteChar} returns
    * <code>true</code> or <code>false</code>. In the latter case, if {@link
    * #isLittleEndian} returns <code>true</code>, then the byte order is switched
    * from big-endian to little-endian.
    */
   public void writeChars (String s) throws IOException {
      if (usesByteChar()) {
         writeBytes (s);
      }
      else {
         int len = s.length();
         for (int i = 0; i < len; i++) {
            writeShort (s.charAt(i));
         }
      }
   }

   /**
    * Writes a float value to this stream. If {@link #isLittleEndian} returns
    * <code>true</code>, then the byte order is switched from
    * big-endian to little-endian.
    *
    * @param f float value to write
    */
   public final void writeFloat (float f) throws IOException {
      this.writeInt(Float.floatToIntBits(f));
   }

   /**
    * Writes a double value to this stream. If {@link #isLittleEndian} returns
    * <code>true</code>, then the byte order is switched from
    * big-endian to little-endian.
    *
    * @param d double value to write
    */
   public final void writeDouble (double d) throws IOException {
      this.writeLong(Double.doubleToLongBits(d));
   }

   /**
    * Writes a UTF-8 string to this stream.
    *
    * @param s string to write
    */
   public final void writeUTF (String s) throws IOException {
      int numchars = s.length();
      int numbytes = 0;
      for (int i = 0 ; i < numchars ; i++) {
         int c = s.charAt(i);
         if ((c >= 0x0001) && (c <= 0x007F)) numbytes++;
         else if (c > 0x07FF) numbytes += 3;
         else numbytes += 2;
      }
      if (numbytes > 65535) throw new UTFDataFormatException();
      out.write((numbytes >>> 8) & 0xFF);
      out.write(numbytes & 0xFF);
      for (int i = 0 ; i < numchars ; i++) {
         int c = s.charAt(i);
         if ((c >= 0x0001) && (c <= 0x007F)) {
            out.write(c);
         }
         else if (c > 0x07FF) {
            out.write(0xE0 | ((c >> 12) & 0x0F));
            out.write(0x80 | ((c >> 6) & 0x3F));
            out.write(0x80 | (c & 0x3F));
            myWriteCount += 2;
         }
         else {
            out.write(0xC0 | ((c >> 6) & 0x1F));
            out.write(0x80 | (c & 0x3F));
            myWriteCount += 1;
         }
      }
      myWriteCount += numchars + 2;
   }

   /**
    * Returns the number of bytes that have been written to this stream.
    */
   public int size() {
      return myWriteCount;
   }

}

