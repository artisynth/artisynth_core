/**
 * Copyright (c) 2014, by the Authors: John E. Lloyd (UBC),
 * Antonio Sanchez (UBC), Elliotte Harold
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.*;

/**
 * A data input stream class that can be set to convert its input from
 * little-endian to big-endian. Characters can also be specified to be
 * either word or byte length.
 *
 * The code is a mash-up of Elliotte Rusty Harold's LittleEndianInputStream,
 * Java source code, and Antonio Sanchez's BinaryStreamReader.
 *
 * @author John E. Lloyd, Antonio Sanchez, Elliotte Harold, Dec 16, 2013
 */
public class BinaryInputStream extends FilterInputStream {

   private int myFlags = 0x00;
   private int myByteCount = 0;
   
   public static final int BIG_ENDIAN = 0x00;   
   public static final int LITTLE_ENDIAN = 0x01; // 1 for little, 0 for big
   public static final int BYTE_CHAR = 0x02;  // 2 for byte-char, 0 for word
   
   /**
    * Creates a new BinaryInputStream from an input stream. Flag values are
    * set to their defaults so that the stream will be big-endian and use word
    * characters instead of byte characters.
    *
    * @param in underlying input stream
    */
   public BinaryInputStream (InputStream in) {
      this (in, 0);
   }

   /**
    * Creates a new BinaryInputStream from an input stream and prescribed set
    * of flag values.
    *
    * @param in underlying input stream
    * @param flags flag values
    */
   public BinaryInputStream (InputStream in, int flags) {
      super(in);
      myByteCount = 0;
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
    * Returns <code>true</code> if this stream converts its input from
    * little-endian where appropriate.
    *
    * @return <code>true</code> if this stream converts from little-endian
    */
   public boolean isLittleEndian() {
      return ((myFlags & LITTLE_ENDIAN) != 0);
   }
   
   /**
    * Sets whether or not this stream converts its input from little-endian.
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
    * Reads an array of bytes from this stream. The number of bytes
    * read is equal to the length of the array.
    *
    * @param b array in which to store the bytes
    */
   public final void readFully(byte b[]) throws IOException {
      readFully(b, 0, b.length);
   }

   /**
    * Reads <code>len</code> bytes from an input stream and stores them in
    * <code>buf</code> starting at <code>off</code>.
    *
    * @param b array in which to store the bytes
    * @param off starting offset for storing bytes
    * @param len number of bytes to read
    */
   public final void readFully(byte b[], int off, int len) throws IOException {
      if (len < 0) {
         throw new IndexOutOfBoundsException();
      }
      int n = 0;
      while (n < len) {
         int count = in.read(b, off + n, len - n);
         if (count < 0) {
            throw new EOFException();
         }
         n += count;
         myByteCount += count;
      }
   }

   /**
    * Skips over <code>n</code> bytes from this stream.
    *
    * @param n number of bytes to skip
    * @return the number of bytes actually skipped
    */
   public final int skipBytes(int n) throws IOException {
      int total = 0;
      int cur = 0;

      while ((total < n) && ((cur = (int)in.skip(n - total)) > 0)) {
         total += cur;
      }
      myByteCount += total;
      return total;
   }
   
   // public final long skip(long n) throws IOException {
   //    long s = super.skip(n);
   //    return s;
   // }

   private int doread() throws IOException {
      int v = in.read();
      if (v == -1) {
         throw new EOFException();
      }
      myByteCount++;
      return v;
   }

   /**
    * Reads a boolean value from this stream.
    *
    * @return the read boolean value
    */
   public final boolean readBoolean() throws IOException {
      return (doread() != 0);
   }

   /**
    * Reads a byte value from this stream.
    *
    * @return the read byte value
    */
   public final byte readByte() throws IOException {
      return (byte)doread();
   }

   /**
    * Reads an unsigned byte value from this stream.
    *
    * @return the read unsigned byte value
    */
   public final int readUnsignedByte() throws IOException {
      return doread();
   }

   /**
    * Reads a short value from this stream. If {@link #isLittleEndian} returns
    * <code>true</code>, then the byte order is switched from
    * little-endian to big-endian.
    *
    * @return the read short value
    */
   public final short readShort() throws IOException {

      int byte1 = doread();
      int byte2 = doread();
      if (isLittleEndian()) {
         return (short)((byte2 << 8) + (byte1 << 0));
      }
      else {
         return (short)((byte1 << 8) + (byte2 << 0));
      }
   }

   /**
    * Reads an unsigned short value from this stream. If {@link #isLittleEndian}
    * returns <code>true</code>, then the byte order is switched from
    * little-endian to big-endian.
    *
    * @return the read unsigned short value
    */
   public final int readUnsignedShort() throws IOException {

      int byte1 = doread();
      int byte2 = doread();
      if (isLittleEndian()) {
         return (byte2 << 8) + (byte1 << 0); 
      }
      else {
         return (byte1 << 8) + (byte2 << 0);   
      }
   }

   /**
    * Reads a character value from this stream. The character is either one or
    * two bytes, depending on whether {@link #usesByteChar} returns
    * <code>true</code> or <code>false</code>. In the latter case, if {@link
    * #isLittleEndian} returns <code>true</code>, then the byte order is
    * switched from little-endian to big-endian.
    *
    * @return the read character value
    */
   public final char readChar() throws IOException {
      int byte1 = doread();
      int byte2 = 0;
      
      if (usesByteChar()) {
         return (char)byte1;
      } else {
         byte2 = doread();
      }
      
      if (isLittleEndian()) {
         return (char)((byte2 << 8) + (byte1 << 0));
      }
      else {
         return (char)((byte1 << 8) + (byte2 << 0));
      }
   }

   /**
    * Reads an integer value from this stream. If {@link #isLittleEndian}
    * returns <code>true</code>, then the byte order is switched from
    * little-endian to big-endian.
    *
    * @return the read integer value
    */
   public final int readInt() throws IOException {
      
      int byte1 = doread();
      int byte2 = doread();
      int byte3 = doread();
      int byte4 = doread();
      
      if (isLittleEndian()) {
         return ((byte4 << 24) + (byte3 << 16) + (byte2 << 8) + (byte1 << 0));
      }
      else {
         return ((byte1 << 24) + (byte2 << 16) + (byte3 << 8) + (byte4 << 0));
      }
   }

   private byte readBuffer[] = new byte[8];

   /**
    * Reads a long value from this stream. If {@link #isLittleEndian}
    * returns <code>true</code>, then the byte order is switched from
    * little-endian to big-endian.
    *
    * @return the read long value
    */
   public final long readLong() throws IOException {

      int byte1 = doread();
      int byte2 = doread();
      int byte3 = doread();
      int byte4 = doread();
      int byte5 = doread();
      int byte6 = doread();
      int byte7 = doread();
      int byte8 = doread();

      int hi, lo;
      if (isLittleEndian()) {
         hi = (byte8 << 24) + (byte7 << 16) + (byte6 << 8) + byte5;
         lo = (byte4 << 24) + (byte3 << 16) + (byte2 << 8) + byte1;
      }
      else {
         hi = (byte1 << 24) + (byte2 << 16) + (byte3 << 8) + byte4;
         lo = (byte5 << 24) + (byte6 << 16) + (byte7 << 8) + byte8;
      }
      return ((long)(hi)<<32) + (0xffffffffL & (long)lo);
   }

   /**
    * Reads a float value from this stream. If {@link #isLittleEndian}
    * returns <code>true</code>, then the byte order is switched from
    * little-endian to big-endian.
    *
    * @return the read float value
    */
   public final float readFloat() throws IOException {
      return Float.intBitsToFloat(readInt());
   }

   /**
    * Reads a double value from this stream. If {@link #isLittleEndian}
    * returns <code>true</code>, then the byte order is switched from
    * little-endian to big-endian.
    *
    * @return the read double value
    */
   public final double readDouble() throws IOException {
      return Double.longBitsToDouble(readLong());
   }

    /**
    * working array initialized on demand by readUTF
    */
   private char chararr[] = new char[80];

   /**
    * Reads a UTF-8 string from this stream.
    *
    * @return the read UTF string
    */
   public final String readUTF() throws IOException {
      int byte1 = doread();
      int byte2 = doread();
      int numbytes = (byte1 << 8) + byte2;
      if (chararr.length < numbytes) {
         chararr = new char[numbytes * 2];
      }
      int numread = 0;
      int numchars = 0;
      while (numread < numbytes) {
         byte1 = doread();
         int byte3;
         // Look at the first four bits of byte1 to determine how many
         // bytes in this char.
         int test = byte1 >> 4;
         if (test < 8) { // one byte
            numread++;
            chararr[numchars++] = (char) byte1;
         }
         else if (test == 12 || test == 13) { // two bytes
            numread += 2;
            if (numread > numbytes) throw new UTFDataFormatException();
            byte2 = doread();
            if ((byte2 & 0xC0) != 0x80) throw new UTFDataFormatException();
            chararr[numchars++] = (char) (((byte1 & 0x1F) << 6) | (byte2 & 0x3F));
         }
         else if (test == 14) { // three bytes
            numread += 3;
            if (numread > numbytes) throw new UTFDataFormatException();
            byte2 = doread();
            byte3 = doread();
            if (((byte2 & 0xC0) != 0x80) || ((byte3 & 0xC0) != 0x80)) {
               throw new UTFDataFormatException();
            }
            chararr[numchars++] = (char)
               (((byte1 & 0x0F) << 12) | ((byte2 & 0x3F) << 6) | (byte3 & 0x3F));
         }
         else { // malformed
            throw new UTFDataFormatException();
         }
      } // end while
      return new String(chararr, 0, numchars);
   }

   /**
    * Returns the total number of bytes that have been read by this stream.
    *
    *@return number of bytes read
    */
   public int getByteCount() {
      return myByteCount;
   }
   
   // @Override
   // public void close() throws IOException {
   //    super.close();
   //    myByteCount = 0;
   // }

}

