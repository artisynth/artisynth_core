/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import maspack.image.dicom.DicomElement.VR;

/**
 * Holds (potentially encoded) image data extracted directly from a DICOM slice
 * @author Antonio
 */
public class DicomPixelData {

   VR vr;

   public final byte[] b;
   public final short[] s;
   public final float[] f;
   public final Object data;

   /**
    * Initializes buffer with a value representation and size
    * @param vr value representation (OB=byte, OW=short, OF=float)
    * @param size number of elements
    */
   public DicomPixelData(VR vr, int size) {
      this.vr = vr;
      switch (vr) {
         case OB:
            b = new byte[size];
            data = b;
            f = null;
            s = null;
            break;
         case OW:
            s = new short[size];
            data = s;
            f = null;
            b = null;
            break;
         case OF:
            f = new float[size];
            data = f;
            b = null;
            s = null;
            break;
         default:
            throw new IllegalArgumentException("Only types OB, OW, OF can be initialized with a size");
      }
   }
   
   /**
    * Assigns data to a new pixel buffer
    * @param vr value representation (OB=byte, OW=short, OF=float)
    * @param data array of data, must be consistent with value representation
    */
   public DicomPixelData(VR vr, Object data) {
      this.vr = vr;
      switch (vr) {
         case OB:
            b = (byte[])data;
            this.data = b;
            f = null;
            s = null;
            break;
         case OW:
            s = (short[])data;
            this.data = s;
            f = null;
            b = null;
            break;
         case OF:
            f = (float[])data;
            this.data = f;
            b = null;
            s = null;
            break;
         default:
            this.data = data;
            b = null;
            s = null;
            f = null;
      }
   }
   
   /**
    * @return the value representation of the data (OB=byte, OW=short, OF=float)
    */
   public VR getType() {
      return vr;
   }
   
   /**
    * @return the data buffer array, whose type corresponds with the value representation
    */
   public Object getData() {
      return data;
   }
   
   /**
    * @return byte data array, if it is in fact a byte buffer 
    */
   public byte[] getByteData() {
      return b;
   }
   
   /**
    * @return short data array, if it is in fact a short buffer 
    */
   public short[] getShortData() {
      return s;
   }
   
   /**
    * @return float data array, if it is in fact a float buffer 
    */
   public float[] getFloatData() {
      return f;
   }

}
