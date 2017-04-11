/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.util;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Read a sequence of bits from a stream
 */
public class BitInputStream extends FilterInputStream {

   boolean littleEndian = false;
   private int bitsRemaining;  // number of bits remaining in current byte
   private int byteCount;      // number of bytes read
   private byte currByte;
   
   private static final int rmask[] = {
       0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff
   };
   
   private static final int lmask[] = {
      0x00, 0x80, 0xc0, 0xe0, 0xf0, 0xf8, 0xfc, 0xfe, 0xff
   };
   
   private static final int BYTE_SIZE = 8;
   
   public BitInputStream (InputStream in) {
      this(in, false);
   }
   
   public BitInputStream(InputStream in, boolean littleEndian) {
      super(in);
      bitsRemaining = 0;
      setLittleEndian(littleEndian);
   }
   
   public void setLittleEndian(boolean littleEndian) {
      this.littleEndian = littleEndian;
   }
   
   public boolean isLittleEndian() {
      return littleEndian;
   }
   
   /**
    * The number of bits read
    */
   public int getBitCount() {
      return byteCount*BYTE_SIZE - bitsRemaining;
   }
   
   /**
    * Read up to 32 bits of data
    * 
    * @param nbits number of bits to read
    * @return integer containg bit values
    */
   public int readBits(int nbits) throws IOException {
      if (nbits < 0 || nbits > 32) {
         throw new IOException("Can only read up to 32 bits at a time");
      }
      
      if (isLittleEndian()) {
         return readLittleEndianBits(nbits);
      } else {
         return readBigEndianBits(nbits);
      }
   }
   
   private int readLittleEndianBits(int nbits) throws IOException {
      
      int out = 0;
      int pos = 0;
      
      while (nbits > bitsRemaining) {
         
         // prepend remaining bits
         int bits = currByte & rmask[bitsRemaining];
         bits <<= pos;
         out |= bits;

         nbits -= bitsRemaining;
         pos += bitsRemaining;
         
         currByte = readByte();
         bitsRemaining = BYTE_SIZE;    // 8 bits remaining
      }
      
      // Any remaining bits
      if (nbits > 0) {
         bitsRemaining = BYTE_SIZE-nbits;
         int bits = (currByte & lmask[nbits]) >>> bitsRemaining;
         bits <<= pos;
         out |= bits;
      }
      
      return out;
   }
   
   private int readBigEndianBits(int nbits) throws IOException {
      
      int out = 0;
      while (nbits > bitsRemaining) {
         // append remaining bits
         out <<= bitsRemaining;
         out |= currByte & rmask[bitsRemaining];
         nbits -= bitsRemaining;
         
         currByte = readByte();
         bitsRemaining = BYTE_SIZE;    // 8 bits remaining
      }
      
      // Any remaining bits
      if (nbits > 0) {
         bitsRemaining = BYTE_SIZE-nbits;
         out <<= nbits;
         out |= (currByte & lmask[nbits]) >>> bitsRemaining;
      }
      return out;
   }
      
   @Override
   /**
    * Reads the next bit in the stream, returns -1 if there are no more bits.
    */
   public int read() throws IOException {
      
      // read 1 bits, or -1 of not possible
      int out;
      if (bitsRemaining > 0) {
         out = ( currByte & rmask[bitsRemaining+1] ) >> (bitsRemaining-1);
         bitsRemaining--;
         return out;
      }
      
      int nextCurr = in.read();
      if (nextCurr < 0) {
         return -1;
      }
      
      byteCount++;
      currByte = (byte)nextCurr;
      bitsRemaining = BYTE_SIZE-1;
      out = currByte >>> bitsRemaining;
      
      return out;
      
   }
   
   private byte readByte() throws IOException {
      int v = in.read();
      if (v == -1) {
         throw new EOFException();
      }
      byteCount++;
      return (byte)v;
   }
   
   /**
    * Reads up to <code>8*b.length</code> bits of data from this
    * input stream into an array of bytes. This method blocks until some
    * input is available.
    *
    * @param      b   the buffer into which the data is read.
    * @return     the total number of bits read into the buffer, or
    *             <code>-1</code> if there is no more data because the end of
    *             the stream has been reached.
    * @exception  IOException  if an I/O error occurs.
    * @see        #read(byte[], int, int)
    */
   public int read(byte b[]) throws IOException {
       return read(b, 0, b.length*BYTE_SIZE);
   }
   
   /**
    * Reads up to <code>len</code> bits of data from this input stream
    * into an array of bytes. If <code>len</code> is not zero, the method
    * blocks until some input is available; otherwise, no
    * bytes are read and <code>0</code> is returned.
    *
    * @param      b     the buffer into which the data is read.
    * @param      off   the start bit offset in the destination array <code>b</code>
    * @param      len   the maximum number of bits read.
    * @return     the total number of bits read into the buffer, or
    *             <code>-1</code> if there is no more data because the end of
    *             the stream has been reached.
    * @exception  NullPointerException If <code>b</code> is <code>null</code>.
    * @exception  IndexOutOfBoundsException If <code>off</code> is negative,
    * <code>len</code> is negative, or <code>len</code> is greater than
    * <code>8*b.length - off</code>
    * @exception  IOException  if an I/O error occurs.
    */
   public int read(byte b[], int off, int len) throws IOException {
      throw new IOException("Not yet implemented"); 
   }
   
   @Override
   /**
    * Skips over and discards <code>n</code> bits of data.  Returns the
    * number of bits skipped.
    */
   public long skip(long n) throws IOException {
      
      long skipped = 0;
      
      if (n < bitsRemaining) {
         bitsRemaining -= n;
         return n;
      }
      
      // clear bits remaining
      skipped += bitsRemaining;
      bitsRemaining = 0;
      
      // how many bytes to skip
      long skipBytes = (n >>> 3);
      long skippedBytes = 0;
      int cur = 0;
      while ((skippedBytes < skipBytes) 
         && ((cur = (int)in.skip(skipBytes - skippedBytes)) > 0)) {
         skippedBytes += cur;
      }
      byteCount += skippedBytes;
      skipped += BYTE_SIZE*skippedBytes;
      
      if ( (skipped < n) &&  (cur = (int)(n-skipped)) < BYTE_SIZE) {
         int v = in.read();
         if (v != -1) {
            // discard final bits
            byteCount++;
            currByte = (byte)v;
            bitsRemaining = BYTE_SIZE - cur;
            skipped += cur;
         }
      }
      
      return skipped;
   }
   
   /**
    * Returns an estimate of the number of bits that can be read (or
    * skipped over) from this input stream without blocking by the next
    * caller of a method for this input stream. The next caller might be
    * the same thread or another thread.  A single read or skip of this
    * many bits will not block, but may read or skip fewer fewer.
    * <p>
    * This method returns the result of {@link #in in}.available().
    *
    * @return     an estimate of the number of bits that can be read (or skipped
    *             over) from this input stream without blocking.
    * @exception  IOException  if an I/O error occurs.
    */
   public int available() throws IOException {
       return bitsRemaining + BYTE_SIZE*in.available();
   }
   
   @Override
   public synchronized void mark(int readlimit) {
      // nothing
   }
   
   @Override
   public synchronized void reset() throws IOException {
      // nothing
   }
   
   @Override
   public boolean markSupported() {
      return false;
   }
}
