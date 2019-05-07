/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.Arrays;
import java.util.Collection;

import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix3d;
import maspack.matrix.SymmetricMatrix3d;

/**
 * A general class for storing integer, double, and Object data. It is
 * intended mainly as a convenience utility for storing component state.
 *
 * <p> DataBuffer maintains three independent buffers for storing integer,
 * double, and Object data. Data is added to each using a <i>put()</i>
 * operation and can be read back using a <i>get()</i> operation. Each
 * buffer has a <i>size</i> and an <i>offset</i>. When data is added using
 * <i>put()</i>, it is added at the end of the buffer and the size is
 * increased. When data is read back using <i>put()</i>, it is read from
 * the current offset location, and the offset is then increased.  The offset
 * is not allowed to increase beyond the current size.  The <i>put()</i>
 * and <i>get()</i> operations therefore act like insert and remove
 * operations for a queue. However, unlike a queue, the <i>get()</i>
 * operation does not actually remove data; it simply advances the offset.
 * Other <i>set()</i> and <i>peek()</i> operations allow data to be
 * set and read within the buffer without affecting its offset or size.
 *
 * <p>The methods implementing the various operations (<i>put()</i>,
 * <i>get()</i>, <i>set()</i>, <i>size()</i>, etc.) for each data type take the
 * name of the operation prefaced with a single character identifying the data
 * type:
 * <dl>
 * <dt>{@code z}</dt><dd>integer</dd>
 * <dt>{@code d}</dt><dd>double</dd>
 * <dt>{@code o}</dt><dd>object</dd>
 * </dl>
 *
 * For example, the <i>put()</i> and <i>get()</i> operations are {@code zput()}
 * and {@code zget()} for integers and {@code dput()} and {@code dget()} for
 * doubles.
 *
 * <p> As an example, we consider storing and retrieving information
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
 * while the restore operation might look like this:
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
 * <p>
 * For convenience,<i>put()</i> and <i>get()</i> operations are also defined to
 * save and restore aggregate types, such as {@link Vector3d} and {@link
 * Matrix3d}.
 *
 * <p> Internally, each buffer is implementing as an array, whose length
 * defines the current buffer capacity. When the size increases beyond the
 * capacity, the array is resized automatically and the capacity is increased.
 * Attempting to move the offset beyond the current size will cause an
 * exception to be thrown. The buffer structure is shown in the following
 * diagram, in which entries marked with 'x' contain actual data.
 * <pre>
 * |x|x|x|x|x|x|x|x|x|x|x|x|x|x|x|x|x|x| | | | | | | | | | | | | | |
 *          |                           |                           |
 *   offset-+                      size-+                  capacity-+
 * </pre>
 */
/*
 * The question arises as to whether we should also store bytes in addition to
 * integers. This would allow us to store some quantities, such as booleans, in
 * a more compact form. However, it would also increase the size of the frame
 * used to delimit the state boundary between different components. If the
 * component has a lot of floating point state relative to int/boolean state,
 * the difference is trivial.
 *
 * Consider some examples: Point, TetElement, and HexElement. For the float
 * state, point has 72 bytes (position, velocity, force), while TetElement and
 * HexElement may have 200 and 1600 bytes, respectively (if we assume 200 bytes
 * of material state per integration point). Assume also that each component
 * needs one boolean of storage. Meanwhile, the storage overhead for each frame
 * will be perhaps 44 with bytes and 40 without. The per-component storage
 * needs then become:
 *
 *                      Bytes       No bytes
 * Without frames:
 *     Point             73           76
 *     TetElement       201          204
 *     HexElement      1601         1604
 *
 * With frames:
 *     Point            117          116
 *     TetElement       245          244
 *     HexElement      1645         1644
 *
 * Overall, the difference is not significant, and so we omit bytes for now for
 * simplicity.
 */
public class DataBuffer {

   protected int[] zbuf;    // buffer for integer information
   protected int zsize;     // number of integers in the buffer
   protected int zoff;      // offset for next integer to be read

   protected double[] dbuf; // buffer for double information
   protected int dsize;     // number of doubles in the buffer
   protected int doff;      // offset for next double to be read

   protected Object[] obuf; // buffer for Object information
   protected int osize;     // number of Objects in the buffer
   protected int ooff;      // offset for next Object to be read

   public static class Offsets {
      public int zoff;
      public int doff;
      public int ooff;

      public static Offsets ZERO = new Offsets();

      public Offsets () {
      }

      public Offsets (int zoff, int doff, int ooff) {
         this.zoff = zoff;
         this.doff = doff;
         this.ooff = ooff;
      }

      public Offsets (Offsets offs) {
         this.zoff = offs.zoff;
         this.doff = offs.doff;
         this.ooff = offs.ooff;
      }

      public void sub (Offsets offs0, Offsets offs1) {
         this.zoff = offs0.zoff - offs1.zoff;
         this.doff = offs0.doff - offs1.doff;
         this.ooff = offs0.ooff - offs1.ooff;
      }

      public void add (Offsets offs) {
         this.zoff += offs.zoff;
         this.doff += offs.doff;
         this.ooff += offs.ooff;
      }

      public String toString() {
         return "(z="+zoff+",d="+doff+",o="+ooff+")";
      }

      public boolean equals (Offsets offs) {
         return (zoff == offs.zoff && doff == offs.doff && ooff == offs.ooff);
      }
   }

   public DataBuffer () {
      zbuf = new int[0];
      dbuf = new double[0];
      obuf = new Object[0];
   }

   /**
    * Creates a new DataBuffer with specified capacities for its bytes,
    * double, integer, double and Object buffers.
    */
   public DataBuffer (int zcap, int dcap, int ocap) {
      this.zbuf = new int[zcap];
      this.dbuf = new double[dcap];
      this.obuf = new Object[ocap];
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
    * Adds a boolean value to the int buffer, increasing its size.
    *
    * @param bool boolean value to add
    */
   public void zputBool (boolean bool) {
      if (zsize == zbuf.length) {
         zEnsureCapacity (zsize+1);
      }
      zbuf[zsize++] = (bool ? 1 : 0);
   }

   /**
    * Returns the boolean value at the current int buffer offset, and
    * increases the offset. This value is {@code false} if the underlying
    * int value is 0, and {@code true} otherwise. If the current offset is 
    * equal to <code>zsize()</code>, an exception is thrown.
    *
    * @return boolean value at the current offset
    */
   public boolean zgetBool() {
      if (zoff >= zsize) {
         throw new ArrayIndexOutOfBoundsException (
            "zoff=" + zoff + ", size=" + zsize);
      }
      return (zbuf[zoff++] != 0);
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
            "zoff=" + zoff + ", size=" + zsize);
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
            "zoff=" + zoff + ", size=" + zsize);
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
    * Adds three doubles from a Vector3d to the double buffer, increasing its
    * size.
    *
    * @param vec vector to add
    */
   public void dput (Vector3d vec) {
      if (dsize > dbuf.length-3) {
         dEnsureCapacity (dsize+3);
      }
      dbuf[dsize++] = vec.x;
      dbuf[dsize++] = vec.y;
      dbuf[dsize++] = vec.z;
   }
   
   /**
    * Adds nine doubles from a Matrix3dBase to the double buffer, increasing its
    * size.
    *
    * @param M matrix to add
    */
   public void dput (Matrix3dBase M) {
      if (dsize > dbuf.length-9) {
         dEnsureCapacity (dsize+9);
      }
      dbuf[dsize++] = M.m00;
      dbuf[dsize++] = M.m01;
      dbuf[dsize++] = M.m02;
      dbuf[dsize++] = M.m10;
      dbuf[dsize++] = M.m11;
      dbuf[dsize++] = M.m12;
      dbuf[dsize++] = M.m20;
      dbuf[dsize++] = M.m21;
      dbuf[dsize++] = M.m22;
   }
   
   /**
    * Adds six doubles from a SymmetricMatrix3d to the double buffer,
    * increasing its size.
    *
    * @param M symmetric matrix to add
    */
   public void dput (SymmetricMatrix3d M) {
      if (dsize > dbuf.length-6) {
         dEnsureCapacity (dsize+6);
      }
      dbuf[dsize++] = M.m00;
      dbuf[dsize++] = M.m11;
      dbuf[dsize++] = M.m22;
      dbuf[dsize++] = M.m01;
      dbuf[dsize++] = M.m02;
      dbuf[dsize++] = M.m12;
   }
   
   /**
    * Adds doubles from a VectorNd to the double buffer, increasing its
    * size.
    *
    * @param vec vector to add
    */
   public void dput (VectorNd vec) {
      int vsize = vec.size();
      if (dsize > dbuf.length-vsize) {
         dEnsureCapacity (dsize+vsize);
      }
      double[] vbuf = vec.getBuffer();
      for (int i=0; i<vsize; i++) {
         dbuf[dsize++] = vbuf[i];
      }
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
            "doff=" + doff + ", size=" + dsize);
      }
      return dbuf[doff++];
   }
   
   /**
    * Gets three doubles starting at the current double buffer offset,
    * places them into a Vector3d, and increases the offset. If
    * the buffer does not have three doubles remaining past the offset,
    * an exception is thrown.
    *
    * @param vec returns vector 
    */
   public void dget (Vector3d vec) {
      if (doff > dsize-3) {
         throw new ArrayIndexOutOfBoundsException (
            "buffer does not have 3 doubles past the offset "+
            "(doff=" + doff + ", size=" + dsize +")");
      }
      vec.x = dbuf[doff++];
      vec.y = dbuf[doff++];
      vec.z = dbuf[doff++];
   }
   
   /**
    * Gets nine doubles starting at the current double buffer offset,
    * places them into a Matrix3dBase, and increases the offset. If
    * the buffer does not have nine doubles remaining past the offset,
    * an exception is thrown.
    *
    * @param M returns the matrix 
    */
   public void dget (Matrix3dBase M) {
      if (doff > dsize-9) {
         throw new ArrayIndexOutOfBoundsException (
            "buffer does not have 9 doubles past the offset "+
            "(doff=" + doff + ", size=" + dsize);
      }
      M.m00 = dbuf[doff++];
      M.m01 = dbuf[doff++];
      M.m02 = dbuf[doff++];
      M.m10 = dbuf[doff++];
      M.m11 = dbuf[doff++];
      M.m12 = dbuf[doff++];
      M.m20 = dbuf[doff++];
      M.m21 = dbuf[doff++];
      M.m22 = dbuf[doff++];
   }
   
   /**
    * Gets six doubles starting at the current double buffer offset, places
    * them into a SymmetricMatrix3d, and increases the offset. If the buffer
    * does not have six doubles remaining past the offset, an exception is
    * thrown.
    *
    * @param M returns the symmetric matrix 
    */
   public void dget (SymmetricMatrix3d M) {
      if (doff > dsize-6) {
         throw new ArrayIndexOutOfBoundsException (
            "buffer does not have 6 doubles past the offset "+
            "(doff=" + doff + ", size=" + dsize);
      }
      M.m00 = dbuf[doff++];
      M.m11 = dbuf[doff++];
      M.m22 = dbuf[doff++];
      M.m01 = dbuf[doff++];
      M.m02 = dbuf[doff++];
      M.m12 = dbuf[doff++];
      M.m10 = M.m01;
      M.m20 = M.m02;
      M.m21 = M.m12;
   }
   
   /**
    * Gets doubles starting at the current double buffer offset, places them
    * into a VectorNd, and increases the offset. If the buffer does not have
    * enough doubles remaining past the offset, an exception is thrown.
    *
    * @param vec returns the vector
    */
   public void dget (VectorNd vec) {
      int vsize = vec.size();
      if (doff > dsize-vsize) {
         throw new ArrayIndexOutOfBoundsException (
            "buffer does not have "+vsize+" doubles past the offset "+
            "(doff=" + doff + ", size=" + dsize);
      }
      double[] vbuf = vec.getBuffer();
      for (int i=0; i<vsize; i++) {
         vbuf[i] = dbuf[doff++];
      }
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
            "doff=" + doff + ", size=" + dsize);
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
            "offset=" + off + ", size=" + zsize);
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
            "ooff=" + ooff + ", size=" + osize);
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
            "ooff=" + ooff + ", size=" + osize);
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

   public void clear () {
      zsize = 0;
      dsize = 0;
      osize = 0;
      zoff = 0;
      doff = 0;
      ooff = 0;
   }      

   public void set (DataBuffer data) {

      if (data.zsize > zbuf.length) {
         zbuf = Arrays.copyOf (data.zbuf, data.zsize);
      }
      else {
         System.arraycopy (data.zbuf, 0, zbuf, 0, data.zsize);
      }
      zsize = data.zsize;

      if (data.dsize > dbuf.length) {
         dbuf = Arrays.copyOf (data.dbuf, data.dsize);
      }
      else {
         System.arraycopy (data.dbuf, 0, dbuf, 0, data.dsize);
      }
      dsize = data.dsize;

      if (data.osize > obuf.length) {
         obuf = Arrays.copyOf (data.obuf, data.osize);
      }
      else {
         System.arraycopy (data.obuf, 0, obuf, 0, data.osize);
      }
      osize = data.osize;

      zoff = data.zoff;
      doff = data.doff;
      ooff = data.ooff;
   }

   public boolean equals (DataBuffer data) {
      return zequals(data) && dequals(data) && oequals(data);
   }

   public boolean numericEquals (DataBuffer data) {
      return zequals(data) && dequals(data);
   }

   public void resetOffsets () {
      zoff = 0;
      doff = 0;
      ooff = 0;
   }      

   public Offsets getSizes () {
      return new Offsets (zsize, dsize, osize);
   }

   public Offsets getNumericSizes () {
      return new Offsets (zsize, dsize, 0);
   }

   public Offsets getOffsets() {
      return new Offsets (zoff, doff, ooff);
   }

   public Offsets getNumericOffsets() {
      return new Offsets (zoff, doff, 0);
   }

}
