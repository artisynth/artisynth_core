/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import java.util.Arrays;
import java.util.HashMap;

import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.util.StringRange;

/**
 * Window interpolator, rescales intensities based on a center intensity and width
 * (as defined in the DICOM file format).  The interpolator allows for multiple named
 * windows, which can be read directly from a DICOM file.
 * @author Antonio
 *
 */
public class DicomWindowPixelInterpolator extends DicomPixelInterpolator {

   public static PropertyList myProps = new PropertyList(DicomWindowPixelInterpolator.class);   
   
   static {
      myProps.add("window", "window preset", "CUSTOM");
      myProps.add("windowCenter", "Center intensity value", 0x7FF, "[-Inf,Inf]");
      myProps.add("windowWidth", "Width of window", 0x7FF, "[1,Inf]");
   }
   
   /**
    * Preset loaded from DICOM file
    * @author Antonio
    *
    */
   private static class WindowPreset implements Comparable<WindowPreset>{
      String name;
      int width;
      int center;
      int idx;
      public WindowPreset(String name, int center, int width, int idx) {
         this.center = center;
         this.width = width;
         this.name = name;
         this.idx = idx;
      }
      
      @Override
      public int compareTo(WindowPreset o) {
         if (idx < o.idx) {
            return -1;
         } else if (idx > o.idx) {
            return 1;
         }
         return 0;
      }
   }
   
   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public static final int DEFAULT_WINDOW_CENTER = 0x0007FF;
   public static final int DEFAULT_WINDOW_WIDTH = 0x0007FF << 1;
   
   private HashMap<String,WindowPreset> presetMap;
   WindowPreset currentPreset;
   WindowPreset customPreset;
   int windowCenter;
   int windowWidth;
   int nextIdx = 0;
   
   /**
    * Create a default windowed interpolator, centered at intensity 2047 with
    * width 2*2047.
    */
   public DicomWindowPixelInterpolator() {
      this(DEFAULT_WINDOW_CENTER, DEFAULT_WINDOW_WIDTH);
   }
   
   /**
    * Constructs a new interpolator given a center intensity and width
    * @param center centre intensity
    * @param width intensity width
    */
   public DicomWindowPixelInterpolator(int center, int width) {
      presetMap = new HashMap<String, WindowPreset>();
      customPreset = new WindowPreset("CUSTOM", center, width, Integer.MAX_VALUE);
      presetMap.put("CUSTOM", customPreset);
      setWindow("CUSTOM");
   }
   
   /**
    * @return the name of the currently active preset window
    */
   public String getWindow() {
      return currentPreset.name;
   }
   
   /**
    * @return list of preset window names
    */
   public String[] getWindowNames() {
      return  presetMap.keySet().toArray(new String[0]);
   }
   
   /**
    * @return the number of window presets available
    */
   public int numWindows() {
      return presetMap.size();
   }
   
   /**
    * Adds a window preset to this interpolator
    * @param preset name of the preset window
    * @param center center intensity of window
    * @param width width of window
    */
   public void addWindowPreset(String preset, int center, int width) {
      presetMap.put(preset, new WindowPreset(preset, center, width, nextIdx++));
   }
   
   /**
    * Sets the currently active preset window
    * @param preset name of preset
    */
   public void setWindow(String preset) {
      if (preset != null) {
         WindowPreset window = presetMap.get(preset);
         if (window != null) {
            currentPreset = window;
            setWindowCenter(window.center);
            setWindowWidth(window.width);
         }
      } else {
         currentPreset = customPreset;
      }
   }
   
   /**
    * @return a range of strings representing the various presets available
    * (mostly for internal use in widgets)
    */
   public StringRange getWindowRange() {
      WindowPreset[] presets = presetMap.values().toArray(new WindowPreset[0]);
      Arrays.sort(presets);
      String[] range = new String[presets.length];
      for (int i=0; i<presets.length; i++) {
         range[i] = presets[i].name;
      }
      return new StringRange(range);
   }
   
   /**
    * Sets the center of the window.  Note that if this differs from the
    * value defined in a selected preset, then the window will automatically
    * be changed to the 'custom' sized window
    */
   public void setWindowCenter(int center) {
      if (center != windowCenter) {
         windowCenter = center;
         
         if (currentPreset != customPreset) {
            if (center != currentPreset.center) {
               currentPreset = customPreset;
            }
         }
         if (currentPreset == customPreset) {
            customPreset.center = center;
         }
      }
   }
   
   /**
    * @return the middle intensity for the window
    */
   public int getWindowCenter() {
      return windowCenter;
   }
   
   /**
    * Sets the width of the window.  Note that if the width differs
    * from the currently active preset, then the window will automatically
    * be switched to the 'custom' sized window
    */
   public void setWindowWidth(int width) {
      
      if (width != windowWidth) {
         if (width < 1) {
            width = 1;
         }
         windowWidth = width;
         
         if (currentPreset != customPreset) {
            if (width != currentPreset.width) {
               currentPreset = customPreset;
            }
         } 
         if (currentPreset == customPreset) {
            customPreset.width = width;
         }
      }
      
   }
   
   /**
    * @return the window width
    */
   public int getWindowWidth() {
      return windowWidth;
   }
   
   private int rescale(double in, int center, int width, int ymin, int ymax) {
      
      // threshold
      if (width == 1) {
         if (in >= center) {
            return ymax; 
         } else {
            return ymin;
         }
      }
      
      // interpolated
      int val;
      if (in <= center - 0.5 - (width-1)/2) {
         val = ymin;
      } else if (in > center - 0.5 + (width-1)/2) {
         val = ymax;
      } else {
         val = (int)((2.0*in-2*center+width)*(ymax-ymin)/(2*width-2))+ymin;
      }
      return val;
   }
   
   @Override
   public int interpGrayscale(double in, int ymin, int ymax) {
      return rescale(in, windowCenter, windowWidth, ymin, ymax);
   }
   
   @Override
   public void interpRGB(double[] in, int ymin, int ymax, int[] out) {
      for (int i=0; i<3; i++) {
         out[i] = rescale(in[i], windowCenter, windowWidth,  ymin, ymax);
      }
   }
   
   @Override
   public void interpGrayscaleToRGB(double gray, int ymin, int ymax, int[] out) {
      int val = rescale(gray, windowCenter, windowWidth,  ymin, ymax);
      out[0] = val;
      out[1] = val;
      out[2] = val;
   }
   
   @Override
   public int interpRGBToGrayscale(double[] rgb, int ymin, int ymax) {
      double gray = ( rgb[0] + rgb[1] + rgb[2])/3;
      gray = rescale (gray, windowCenter, windowWidth, ymin, ymax);
      return (int)gray;
   }
   
//   @Override
//   public int interpByteRGB(byte[] in, int idx, byte[] out, int odx) {
//      byte bval = (byte)rescale(in[idx], windowCenter, windowWidth,  BYTE_MAX);
//      out[odx++] = bval;
//      out[odx++] = bval;
//      out[odx++] = bval;
//      return odx;
//   }
//
//   @Override
//   public int interpByteByte(byte[] in, int idx, byte[] out, int odx) {
//      out[odx++] = (byte)rescale(in[idx], windowCenter, windowWidth,  BYTE_MAX);
//      return odx;
//   }
//
//   @Override
//   public int interpByteShort(byte[] in, int idx, short[] out, int odx) {
//      out[odx++] = (short)rescale(in[idx], windowCenter, windowWidth,  SHORT_MAX);
//      return odx;
//   }
//   
//   @Override
//   public int interpRGBRGB(byte[] in, int idx, byte[] out, int odx) {
//      for (int i=0; i<3; i++) {
//         out[odx++] = (byte)rescale(in[idx++], windowCenter, windowWidth,  BYTE_MAX);
//      }
//      return odx;
//   }
//
//   @Override
//   public int interpRGBByte(byte[] in, int idx, byte[] out, int odx) {
//      int val3 = (in[idx] & BYTE_MASK) + (in[idx+1] & BYTE_MASK) + (in[idx+2] & BYTE_MASK);
//      out[odx++] = (byte)rescale (val3, 3*windowCenter, 3*windowWidth, BYTE_MAX);
//      return odx;
//   }
//
//   @Override
//   public int interpRGBShort(byte[] in, int idx, short[] out, int odx) {
//      int val3 = (in[idx] & BYTE_MASK) + (in[idx+1] & BYTE_MASK) + (in[idx+2] & BYTE_MASK);
//      out[odx++] = (byte)rescale (val3, 3*windowCenter, 3*windowWidth, SHORT_MAX);
//      return odx;
//   }
//
//   @Override
//   public int interpShortRGB(short[] in, int idx, byte[] out, int odx) {      
//      byte bval = (byte)rescale(in[idx], windowCenter, windowWidth, BYTE_MAX);
//      out[odx++] = bval;
//      out[odx++] = bval;
//      out[odx++] = bval;
//      return odx;
//   }
//
//   @Override
//   public int interpShortByte(short[] in, int idx, byte[] out, int odx) {
//      out[odx++] =  (byte)rescale(in[idx], windowCenter, windowWidth, BYTE_MAX);
//      return odx;
//   }
//
//   @Override
//   public int interpShortShort(short[] in, int idx, short[] out, int odx) {
//      out[odx++] = (short)rescale(in[idx], windowCenter, windowWidth, SHORT_MAX);
//      
//      return odx;
//   }
//
//   @Override
//   public int
//      interp(DicomPixelBuffer in, int idx, DicomPixelBuffer out, int odx) {
//      
//      switch (in.getPixelType()) {
//         case BYTE: {
//            
//            switch (out.getPixelType()) {
//               case BYTE: {
//                  return interpByteByte((byte[])in.getBuffer(), idx, (byte[])out.getBuffer(), odx);
//               }
//               case BYTE_RGB: {
//                  return interpByteRGB((byte[])in.getBuffer(), idx, (byte[])out.getBuffer(), odx);
//               }
//               case SHORT: {
//                  return interpByteShort((byte[])in.getBuffer(), idx, (short[])out.getBuffer(), odx);
//               }
//               default: {
//                  throw new IllegalArgumentException("Unknown type: " + out.getPixelType());
//               }  
//            }
//         }
//         case BYTE_RGB: {
//            switch (out.getPixelType()) {
//               case BYTE: {
//                  return interpRGBByte((byte[])in.getBuffer(), idx, (byte[])out.getBuffer(), odx);
//               }
//               case BYTE_RGB: {
//                  return interpRGBRGB((byte[])in.getBuffer(), idx, (byte[])out.getBuffer(), odx);
//               }
//               case SHORT: {
//                  return interpRGBShort((byte[])in.getBuffer(), idx, (short[])out.getBuffer(), odx);
//               }
//               default: {
//                  throw new IllegalArgumentException("Unknown type: " + out.getPixelType());
//               }  
//            }
//         }
//         case SHORT: {
//            switch (out.getPixelType()) {
//               case BYTE: {
//                  return interpShortByte((short[])in.getBuffer(), idx, (byte[])out.getBuffer(), odx);
//               }
//               case BYTE_RGB: {
//                  return interpShortRGB((short[])in.getBuffer(), idx, (byte[])out.getBuffer(), odx);
//               }
//               case SHORT: {
//                  return interpShortShort((short[])in.getBuffer(), idx, (short[])out.getBuffer(), odx);
//               }
//               default: {
//                  throw new IllegalArgumentException("Unknown type: " + out.getPixelType());
//               }  
//            }
//         }
//         default: {
//            throw new IllegalArgumentException("Unknown type: " + in.getPixelType());
//         }
//         
//      }
//      
//   }
   
}
