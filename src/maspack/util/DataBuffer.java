/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;

/**
 * A general class for storing double, integer, and Object data. It is intended
 * mainly as a convenience utility for storing component state.
 *
 * <p> DataBuffer maintains three independent buffers for storing double,
 * integer, and Object data. Data is added to each using a <code>put()</code>
 * operation and can be read back using a <code>get()</code> operation. Each
 * buffer has a <i>size</i> and an <i>offset</i>. When data is added using
 * <code>put()</code>, it is added at the end of the buffer and the size is
 * increased. When data is read back using <code>put()</code>, it is read from
 * the current offset location, and the offset is then increased.  The offset
 * is not allowed to increase beyond the current size.  The <code>put()</code>
 * and <code>get()</code> operations therefore act like inset and remove
 * operations for a queue. However, unlike a queue, the <code>get()</code>
 * operation does not actually remove data; it simply advances the offset.
 * Other <code>set()</code> and <code>peek()</code> operations allow data to be
 * set and read within the buffer without affecting its offset or size.
 *
 * <p> As an example, we consider storing and retriveing information
 * about a vector. The store operation might look like this:
 * <pre>
 * {@code
 *    saveVector (DataBuffer data) {
 *       data.zput (size);               // store vector size as an integer
 *       for (int i=0; i<size; i++) {
 *          data.dout (vector.get(i));   // store vector data as doubles
 *       }
 *    }
 * }
 * </pre>
 * while the restore operation migth look like this:
 * <pre>
 * {@code
 *    loadVector (DataBuffer data) {
 *       int size = data.zget ();        // get the vector size
 *       vector = new VectorNd(size);
 *       for (int i=0; i<size; i++) {
 *          vector.set (i, data.dget()); // restore the vector data
 *       }
 *    }
 * }
 * </pre>
 *
 * <p> Internally, each buffer is implementing as an array, whose length
 * defines the current buffer capacity. When the size increases beyond the
 * capacity, the array is resized automatically and the capacity is increased.
 * Attempting to move the offset beyond the current size will cause an
 * excpetion to be thrown. The buffer structure is shown in the following
 * diagram, in which entries marked with 'x' contain actual data.
 * <pre>
 * |x|x|x|x|x|x|x|x|x|x|x|x|x|x|x|x|x|x| | | | | | | | | | | | | | |
 *          |                           |                           |
 *   offset-+                      size-+                  capacity-+
 * </pre>
 */
public class DataBuffer {
   protected double[] dbuf; // buffer for double information
   protected int dsize;     // number of doubles in the buffer
   protected int doff;      // offset for next double to be read

   protected int[] zbuf;    // buffer for integer information
   protected int zsize;     // number of integers in the buffer
   protected int zoff;      // offset for next integer to be read

   protected Object[] obuf; // buffer for Object information
   protected int osize;     // number of Objects in the buffer
   protected int ooff;      // offset for next Object to be read

   public DataBuffer () {

      dbuf = new double[0];
      zbuf = new int[0];
      obuf = new Object[0];
   }

   /**
    * Creates a new DataBuffer with specified capacities for its double,
    * integer, and Object buffers.
    */
   public DataBuffer (
      int dcap, int zcap, int ocap) {

      this.dbuf = new double[dcap];
      this.zbuf = new int[zcap];
      this.obuf = new Object[ocap];
   }

   public DataBuffer (int dcap, int zcap) {

      this (dcap, zcap, 0);
   }

   // double portion


   /**
    * Returns the amount of data in the double buffer.
    *
    * @return amount of data in the double buffer
    */
   public int dsize() {
      return dsize;
   }
  
   /**
    * Adds a double to the double buffer, increasing its size.
    *
    * @param d double to add
    */
   public void dput (double d) {
      if (dsize == dbuf.length) {
         dEnsureCapacity (dsize+1);
      }
      dbuf[dsize++] = d;
   }
   
   /**
    * Overwrites a value in the double buffer at a specified location
    * <code>i</code>. If the location is outside the range <code>0</code> to
    * <code>dsize()-1</code>, inclusive, an exception is thrown.
    *
    * @param i index at which to overwrite data
    * @param d new data value to set
    */
   public void dset (int i, double d) {
      if (i >= dsize) {
         throw new ArrayIndexOutOfBoundsException (
            "index=" + i + ", size=" + dsize);
      }
      dbuf[i] = d;
   }
   
   /**
    * Resets the size of the double buffer. If the size is increased,
    * the new space will be padded with zeros. If the size is decreased,
    * the offset will be adjusted to enusre that is does not exceed
    * the size.
    *
    * @param size new size for the double buffer
    */
   public void dsetSize (int size) {
      if (size > dbuf.length) {
         dEnsureCapacity (size);
      }
      dsize = size;
      if (doff > dsize) {
         doff = dsize;
      }
   }
   
   /**
    * Returns the double value at the current double buffer offset, and
    * increases the offset. If the current offset is equal to
    * <code>dsize()</code>, an exception is thrown.
    *
    * @return double value at the current offset
    */
   public double dget() {
      if (doff >= dsize) {
         throw new ArrayIndexOutOfBoundsException (
            "index=" + doff + ", size=" + dsize);
      }
      return dbuf[doff++];
   }
   
   /**
    * Returns the double value at the current double buffer offset, but does
    * <i>not</i> increase the offset.  If the current offset is equal to
    * <code>dsize()</code>, an exception is thrown.
    *
    * @return double value at the current offset
    */
   public double dpeek() {
      if (doff >= dsize) {
         throw new ArrayIndexOutOfBoundsException (
            "index=" + doff + ", size=" + dsize);
      }
      return dbuf[doff];
   }
   
   /**
    * Returns the double value at a specified location <code>i</code>.
    * If the location is outside the range <code>0</code> to
    * <code>dsize()-1</code>, inclusive, an exception is thrown.
    *
    * @param i index at which to obtain the data
    * @return double value at location <code>i</code>
    */
   public double dpeek (int i) {
      if (i >= dsize) {
         throw new ArrayIndexOutOfBoundsException (
            "index=" + i + ", size=" + dsize);
      }
      return dbuf[i];
   }
   
   /**
    * Advances the double buffer offset forward by <code>n</code>.
    * If this causes the offset to exceed <code>dsize()</code>,
    * an exception is thrown.
    *
    * @param n amount to advance the double buffer
    */
   public void dskip (int n) {
      if (doff+n > dsize) {
         throw new ArrayIndexOutOfBoundsException (
            "tried to advance "+ (doff+n-dsize) + " beyond size " + dsize);
      }
      doff += n;
   }
   
   /**
    * Returns the current double buffer offset.
    *
    * @return current double buffer offset
    */
   public int doffset() {
      return doff;
   }
   
   /**
    * Sets the double buffer offset. If the requested offset is outside the
    * range <code>0</code> to <code>dsize()-1</code>, inclusive, an exception
    * is thrown.
    *
    * @param off new double buffer offset
    */
   public void dsetOffset (int off) {
      if (off < 0 || off > dsize) {
         throw new ArrayIndexOutOfBoundsException (
            "offset=" + off + ", size=" + dsize);
      }
      doff = off;
   }
   
   /**
    * Returns true if the double buffer contents of this DataBuffer
    * and another DataBuffer are equal.
    *
    * @return true if the double buffers are equal
    */
   public boolean dequals (DataBuffer data) {
      if (data.dsize() != dsize()) {
         return false;
      }
      return ArraySupport.equals (dbuf, data.dbuf, dsize());
   }

   /**
    * Ensures that the double buffer has a specified capacity. The
    * actualy capacity allocated may exceed this.
    *
    * @param cap request double buffer capacity
    */
   public void dEnsureCapacity (int cap) {
      if (cap > dbuf.length) {
         dbuf = Arrays.copyOf (dbuf, (int)Math.max (16, 1.5*(cap)));
      }
   }
   
   /**
    * Returns the current array used to store the double buffer. Note that this
    * array may be changed by any operation that changes the double buffer's
    * size or capacity.
    *
    * @return current double buffer
    */
   public double[] dbuffer() {
      return dbuf;
   }
  
    // integer portion

   /**
    * Returns the amount of data in the integer buffer.
    *
    * @return amount of data in the integer buffer
    */
   public int zsize() {
      return zsize;
   }
   
   /**
    * Adds a integer to the integer buffer, increasing its size.
    *
    * @param z integer to add
    */
   public void zput (int z) {
      if (zsize == zbuf.length) {
         zEnsureCapacity (zsize+1);
      }
      zbuf[zsize++] = z;
   }
   
   /**
    * Overwrites a value in the integer buffer at a specified location
    * <code>i</code>. If the location is outside the range <code>0</code> to
    * <code>zsize()-1</code>, inclusive, an exception is thrown.
    *
    * @param i index at which to overwrite data
    * @param z new data value to set
    */
   public void zset (int i, int z) {
      if (i >= zsize) {
         throw new ArrayIndexOutOfBoundsException (
            "index=" + i + ", size=" + zsize);
      }
      zbuf[i] = z;
   }
   
   /**
    * Resets the size of the integer buffer. If the size is increased,
    * the new space will be padded with zeros. If the size is decreased,
    * the offset will be adjusted to enusre that is does not exceed
    * the size.
    *
    * @param size new size for the integer buffer
    */
   public void zsetSize (int size) {
      if (size > zbuf.length) {
         zEnsureCapacity (size);
      }
      zsize = size;
      if (zoff > zsize) {
         zoff = zsize;
      }
   }

   /**
    * Returns the integer value at the current integer buffer offset, and
    * increases the offset. If the current offset is equal to
    * <code>zsize()</code>, an exception is thrown.
    *
    * @return integer value at the current offset
    */
   public int zget() {
      if (zoff >= zsize) {
         throw new ArrayIndexOutOfBoundsException (
            "index=" + zoff + ", size=" + zsize);
      }
      return zbuf[zoff++];
   }
   
   /**
    * Returns the integer value at the current integer buffer offset, but does
    * <i>not</i> increase the offset.  If the current offset is equal to
    * <code>zsize()</code>, an exception is thrown.
    *
    * @return integer value at the current offset
    */
   public int zpeek() {
      if (zoff >= zsize) {
         throw new ArrayIndexOutOfBoundsException (
            "index=" + zoff + ", size=" + zsize);
      }
      return zbuf[zoff];
   }
   
   /**
    * Returns the integer value at a specified location <code>i</code>.
    * If the location is outside the range <code>0</code> to
    * <code>zsize()-1</code>, inclusive, an exception is thrown.
    *
    * @param i index at which to obtain the data
    * @return integer value at location <code>i</code>
    */
   public int zpeek (int i) {
      if (i >= zsize) {
         throw new ArrayIndexOutOfBoundsException (
            "index=" + i + ", size=" + zsize);
      }
      return zbuf[i];
   }
   
   /**
    * Advances the integer buffer offset forward by <code>n</code>.
    * If this causes the offset to exceed <code>zsize()</code>,
    * an exception is thrown.
    *
    * @param n amount to advance the integer buffer
    */
   public void zskip (int n) {
      if (zoff+n > zsize) {
         throw new ArrayIndexOutOfBoundsException (
            "tried to advance "+ (zoff+n-zsize) + " beyond size " + zsize);
      }
      zoff += n;
   }
   
   /**
    * Returns the current integer buffer offset.
    *
    * @return current integer buffer offset
    */
   public int zoffset() {
      return zoff;
   }
   
   /**
    * Sets the integer buffer offset. If the requested offset is outside the
    * range <code>0</code> to <code>zsize()-1</code>, inclusive, an exception
    * is thrown.
    *
    * @param off new integer buffer offset
    */
   public void zsetOffset (int off) {
      if (off < 0 || off > zsize) {
         throw new ArrayIndexOutOfBoundsException (
            "offset=" + off + ", size=" + zsize);
      }
      zoff = off;
   }
   
   /**
    * Returns true if the integer buffer contents of this DataBuffer
    * and another DataBuffer are equal.
    *
    * @return true if the integer buffers are equal
    */
   public boolean zequals (DataBuffer data) {
      if (data.zsize() != zsize()) {
         return false;
      }
      return ArraySupport.equals (zbuf, data.zbuf, zsize());
   }

   /**
    * Ensures that the integer buffer has a specified capacity. The
    * actualy capacity allocated may exceed this.
    *
    * @param cap request integer buffer capacity
    */
   public void zEnsureCapacity (int cap) {
      if (cap > zbuf.length) {
         zbuf = Arrays.copyOf (zbuf, (int)Math.max (16, 1.5*(cap)));
      }
   }
   
   /**
    * Returns the current array used to store the integer buffer. Note that this
    * array may be changed by any operation that changes the integer buffer's
    * size or capacity.
    *
    * @return current integer buffer
    */
   public int[] zbuffer() {
      return zbuf;
   }

   // object portion

   /**
    * Returns the amount of data in the Object buffer.
    *
    * @return amount of data in the Object buffer
    */
   public int osize() {
      return osize;
   }
   
   /**
    * Adds a Object to the Object buffer, increasing its size.
    *
    * @param o Object to add
    */
   public void oput (Object o) {
      if (osize == obuf.length) {
         oEnsureCapacity (osize+1);
      }
      obuf[osize++] = o;
   }
   
   /**
    * Adds a collection of Objects to the Object buffer, increasing its size.
    *
    * @param objs  Objects to add
    */
   public void oputs (Collection<? extends Object> objs) {
      if (osize + objs.size() > obuf.length) {
         oEnsureCapacity (osize+objs.size());
      }
      for (Object o : objs) {
         obuf[osize++] = o;
      }
   }
   
   /**
    * Overwrites a value in the Object buffer at a specified location
    * <code>i</code>. If the location is outside the range <code>0</code> to
    * <code>osize()-1</code>, inclusive, an exception is thrown.
    *
    * @param i index at which to overwrite data
    * @param o new data value to set
    */
   public void oset (int i, Object o) {
      if (i >= osize) {
         throw new ArrayIndexOutOfBoundsException (
            "index=" + i + ", size=" + osize);
      }
      obuf[i] = o;
   }
   
   /**
    * Resets the size of the Object buffer. If the size is increased, the new
    * space will be padded with <code>null</code>s. If the size is decreased,
    * the offset will be adjusted to enusre that is does not exceed the size.
    *
    * @param size new size for the Object buffer
    */
   public void osetSize (int size) {
      if (size > obuf.length) {
         oEnsureCapacity (size);
      }
      osize = size;
      if (ooff > osize) {
         ooff = osize;
      }
   }

   /**
    * Returns the Object value at the current Object buffer offset, and
    * increases the offset. If the current offset is equal to
    * <code>osize()</code>, an exception is thrown.
    *
    * @return Object value at the current offset
    */
   public Object oget() {
      if (ooff >= osize) {
         throw new ArrayIndexOutOfBoundsException (
            "index=" + ooff + ", size=" + osize);
      }
      return obuf[ooff++];
   }
   
   /**
    * Returns the Object value at the current Object buffer offset, but does
    * <i>not</i> increase the offset.  If the current offset is equal to
    * <code>osize()</code>, an exception is thrown.
    *
    * @return Object value at the current offset
    */
   public Object opeek() {
      if (ooff >= osize) {
         throw new ArrayIndexOutOfBoundsException (
            "index=" + ooff + ", size=" + osize);
      }
      return obuf[ooff];
   }
   
   /**
    * Returns the Object value at a specified location <code>i</code>.
    * If the location is outside the range <code>0</code> to
    * <code>osize()-1</code>, inclusive, an exception is thrown.
    *
    * @param i index at which to obtain the data
    * @return Object value at location <code>i</code>
    */
   public Object opeek (int i) {
      if (i >= osize) {
         throw new ArrayIndexOutOfBoundsException (
            "index=" + i + ", size=" + osize);
      }
      return obuf[i];
   }
   
   /**
    * Advances the Object buffer offset forward by <code>n</code>.
    * If this causes the offset to exceed <code>osize()</code>,
    * an exception is thrown.
    *
    * @param n amount to advance the Object buffer
    */
   public void oskip (int n) {
      if (ooff+n > osize) {
         throw new ArrayIndexOutOfBoundsException (
            "tried to advance "+ (ooff+n-osize) + " beyond size " + osize);
      }
      ooff += n;
   }
   
   /**
    * Returns the current Object buffer offset.
    *
    * @return current Object buffer offset
    */
   public int ooffset() {
      return ooff;
   }
   
   /**
    * Sets the Object buffer offset. If the requested offset is outside the
    * range <code>0</code> to <code>osize()-1</code>, inclusive, an exception
    * is thrown.
    *
    * @param off new Object buffer offset
    */
   public void osetOffset (int off) {
      if (off < 0 || off > osize) {
         throw new ArrayIndexOutOfBoundsException (
            "offset=" + off + ", size=" + osize);
      }
      ooff = off;
   }
   
   /**
    * Returns true if the Object buffer contents of this DataBuffer
    * and another DataBuffer are equal.
    *
    * @return true if the Object buffers are equal
    */
   public boolean oequals (DataBuffer data) {
      if (osize() != data.osize()) {
         return false;
      }
      return ArraySupport.equals (obuf, data.obuf, osize());
   }

   /**
    * Ensures that the Object buffer has a specified capacity. The
    * actualy capacity allocated may exceed this.
    *
    * @param cap request Object buffer capacity
    */
   public void oEnsureCapacity (int cap) {
      if (cap > obuf.length) {
         obuf = Arrays.copyOf (obuf, (int)Math.max (16, 1.5*(cap)));
      }
   }
   
  /**
    * Returns the current array used to store the Object buffer. Note that this
    * array may be changed by any operation that changes the Object buffer's
    * size or capacity.
    *
    * @return current Object buffer
    */
   public Object[] obuffer() {
      return obuf;
   }

   // general methods

   public void setSize (int dsize, int zsize) {
      dsetSize (dsize);
      zsetSize (zsize);
   }

   public void clear () {
      dsize = 0;
      zsize = 0;
      osize = 0;
      
      doff = 0;
      zoff = 0;
      ooff = 0;
   }      

   public void set (DataBuffer data) {

      if (data.dsize > dbuf.length) {
         dbuf = Arrays.copyOf (data.dbuf, data.dsize);
      }
      else {
         System.arraycopy (data.dbuf, 0, dbuf, 0, data.dsize);
      }
      dsize = data.dsize;

      if (data.zsize > zbuf.length) {
         zbuf = Arrays.copyOf (data.zbuf, data.zsize);
      }
      else {
         System.arraycopy (data.zbuf, 0, zbuf, 0, data.zsize);
      }
      zsize = data.zsize;

      if (data.osize > obuf.length) {
         obuf = Arrays.copyOf (data.obuf, data.osize);
      }
      else {
         System.arraycopy (data.obuf, 0, obuf, 0, data.osize);
      }
      osize = data.osize;

      doff = data.doff;
      zoff = data.zoff;
      ooff = data.ooff;
   }

   public boolean equals (DataBuffer data) {
      return dequals (data) && zequals (data) && oequals (data);
   }

   public boolean equals (DataBuffer data, boolean printFailPoint) {
      if (dsize() != data.dsize()) {
         if (printFailPoint) {
            System.out.println (
               "unequal dbuf sizes: "+dsize()+
               " vs. "+data.dsize());
         }
         return false;
      }
      else if (!dequals (data)) {
         if (printFailPoint) {
            for (int i=0; i<dsize(); i++) {
               if (dpeek(i) != data.dpeek(i)) {
                  System.out.println (
                     "unequal dbuf at  i="+i+": " +
                     dpeek(i) + " vs. " + data.dpeek(i));
               }
            }
         }
         return false;
      }
      if (zsize() != data.zsize()) {
         if (printFailPoint) {
            System.out.println (
               "unequal zbuf sizes: "+zsize()+
               " vs. "+data.zsize());
         }
         return false;
      }
      else if (!zequals (data)) {
         if (printFailPoint) {
            for (int i=0; i<zsize(); i++) {
               if (zpeek(i) != data.zpeek(i)) {
                  System.out.println (
                     "unequal zbuf at  i="+i+": " +
                     zpeek(i) + " vs. " + data.zpeek(i));
               }
            }
         }
         return false;
      }
      if (osize() != data.osize()) {
         if (printFailPoint) {
            System.out.println (
               "unequal obuf sizes: "+osize()+
               " vs. "+data.osize());
         }
         return false;
      }
      else if (!oequals (data)) {
         if (printFailPoint) {
            for (int i=0; i<osize(); i++) {
               Object o1 = opeek(i);
               Object o2 = data.opeek(i);
               if (((o1 == null) != (o2 == null)) ||
                   ((o1 != null) && !(o1.equals(o2)))) {
                  System.out.println (
                     "unequal obuf at  i="+i+": " +
                     o1 + " vs. " + o2);
               }
            }
         }
         return false;
      }
      return true;
   }

   public void resetOffsets () {
      doff = 0;
      zoff = 0;
      ooff = 0;
   }      

   public void setBuffersAndOffsets (DataBuffer data) {

      dbuf = data.dbuf;
      zbuf = data.zbuf;
      obuf = data.obuf;

      dsize = data.dsize;
      zsize = data.zsize;
      osize = data.osize;
      
      doff = data.doff;
      zoff = data.zoff;
      ooff = data.ooff;     
   }

   public void putData (DataBuffer data, int numd, int numz) {
      if (numd > 0) {
         for (int i=0; i<numd; i++) {
            dput (data.dget());
         }
      }
      if (numz > 0) {
         for (int i=0; i<numz; i++) {
            zput (data.zget());
         }
      }
   }


}
