/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.properties.CompositeProperty;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyInfo;
import maspack.properties.PropertyList;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Converts (interpolates) pixel values from a stored raw form to a new 
 * form appropriate for display (e.g. windowing)
 * @author Antonio
 *
 */
public abstract class DicomPixelInterpolator implements CompositeProperty {

   static Class<?>[] mySubClasses =
      new Class<?>[] { DicomWindowPixelInterpolator.class, DicomRawPixelInterpolator.class };

   public static Class<?>[] getSubClasses() {
      return mySubClasses;
   }

   PropertyInfo myPropInfo;
   HasProperties myPropHost;

   public PropertyInfo getPropertyInfo() {
      return myPropInfo;
   }

   public void setPropertyInfo(PropertyInfo info) {
      myPropInfo = info;
   }

   public HasProperties getPropertyHost() {
      return myPropHost;
   }

   public void setPropertyHost(HasProperties newParent) {
      myPropHost = newParent;
   }

   public static PropertyList myProps = new PropertyList(
      DicomPixelInterpolator.class);

   public Property getProperty(String name) {
      return PropertyList.getProperty(name, this);
   }

   public boolean hasProperty(String name) {
      return getAllPropertyInfo().get(name) != null;
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public boolean isWritable() {
      return true;
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref) 
      throws IOException {

      pw.println ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ref);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public void scan (ReaderTokenizer rtok, Object ref) 
      throws IOException {

      getAllPropertyInfo().setDefaultValues (this);
      getAllPropertyInfo().setDefaultModes (this);
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         if (!getAllPropertyInfo().scanProp (this, rtok)) {
            throw new IOException ("unexpected input: " + rtok);
         }
      }
   }

   public DicomPixelInterpolator clone() {
      DicomPixelInterpolator dpc = null;
      try {
         dpc = (DicomPixelInterpolator)super.clone();
      } catch (CloneNotSupportedException e) {
         throw new InternalErrorException("cannot clone super in DicomPixelConverter");
      }
      return dpc;
   }
   
   // abstract methods
   /**
    * Interpolates a new grayscale value
    * @param in input grayscale
    * @param ymin minimum output
    * @param ymax maximum output
    * @return interpolated output
    */
   public abstract int interpGrayscale(double in, int ymin, int ymax);
   
   /**
    * Interpolates a new rgb value
    * @param in input rgb
    * @param ymin minimum output value
    * @param ymax maximum output value
    * @param out output rgb
    */
   public abstract void interpRGB(double[] in, int ymin, int ymax, int[] out);
   
   /**
    * Interpolates an RGB value to grayscale
    * @param rgb input rgb
    * @param ymin minimum output value
    * @param ymax maximum output value
    * @return output grayscale
    */
   public abstract int interpRGBToGrayscale(double[] rgb, int ymin, int ymax);
   
   /**
    * Interpolates a grayscale value to RGB
    * @param gray input grayscale
    * @param ymin minimum output value
    * @param ymax maximum output value
    * @param out output rgb
    */
   public abstract void interpGrayscaleToRGB(double gray, int ymin, int ymax, int[] out);
   
//   
//   /**
//    * Interpolates from grayscale (byte) to RGB (byte) values
//    * @param in input pixel values
//    * @param idx starting input index
//    * @param out output buffer to fill
//    * @param odx starting output index
//    * @return next index in output buffer
//    */
//   public abstract int interpByteRGB(byte[] in, int idx, byte[] out, int odx);
//
//   /**
//    * Interpolates from grayscale (byte) to grayscale (byte) values
//    * @param in input pixel values
//    * @param idx starting input index
//    * @param out output buffer to fill
//    * @param odx starting output index
//    * @return next index in output buffer
//    */
//   public abstract int interpByteByte(byte[] in, int idx, byte[] out, int odx);
//
//   /**
//    * Interpolates from grayscale (byte) to grayscale (short) values
//    * @param in input pixel values
//    * @param idx starting input index
//    * @param out output buffer to fill
//    * @param odx starting output index
//    * @return next index in output buffer
//    */
//   public abstract int interpByteShort(byte[] in, int idx, short[] out, int odx);
//
//   /**
//    * Interpolates from RGB (byte) to RGB (byte) values
//    * @param in input pixel values
//    * @param idx starting input index
//    * @param out output buffer to fill
//    * @param odx starting output index
//    * @return next index in output buffer
//    */
//   public abstract int interpRGBRGB(byte[] in, int idx, byte[] out, int odx);
//
//   /**
//    * Interpolates from RGB (byte) to grayscale (byte) values
//    * @param in input pixel values
//    * @param idx starting input index
//    * @param out output buffer to fill
//    * @param odx starting output index
//    * @return next index in output buffer
//    */
//   public abstract int interpRGBByte(byte[] in, int idx, byte[] out, int odx);
//
//   /**
//    * Interpolates from RGB (byte) to grayscale (short) values
//    * @param in input pixel values
//    * @param idx starting input index
//    * @param out output buffer to fill
//    * @param odx starting output index
//    * @return next index in output buffer
//    */
//   public abstract int interpRGBShort(byte[] in, int idx, short[] out, int odx);
//
//   /**
//    * Interpolates from grayscale (short) to RGB (byte) values
//    * @param in input pixel values
//    * @param idx starting input index
//    * @param out output buffer to fill
//    * @param odx starting output index
//    * @return next index in output buffer
//    */
//   public abstract int interpShortRGB(short[] in, int idx, byte[] out, int odx);
//
//   /**
//    * Interpolates from grayscale (short) to grayscale (byte) values
//    * @param in input pixel values
//    * @param idx starting input index
//    * @param out output buffer to fill
//    * @param odx starting output index
//    * @return next index in output buffer
//    */
//   public abstract int interpShortByte(short[] in, int idx, byte[] out, int odx);
//
//   /**
//    * Interpolates from grayscale (short) to grayscale (short) values
//    * @param in input pixel values
//    * @param idx starting input index
//    * @param out output buffer to fill
//    * @param odx starting output index
//    * @return next index in output buffer
//    */
//   public abstract int interpShortShort(
//      short[] in, int idx, short[] out, int odx);
//
//   /**
//    * Interpolates between two pixel buffers whose class determines their value 
//    * representation type 
//    * @param in input pixel values
//    * @param idx starting input index
//    * @param out output buffer to fill
//    * @param odx starting output index
//    * @return next index in output buffer
//    */
//   public abstract int interp(
//      DicomPixelBuffer in, int idx, DicomPixelBuffer out, int odx);

}
