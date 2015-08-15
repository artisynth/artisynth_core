package maspack.dicom;

import java.util.Arrays;
import java.util.HashMap;

import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.util.StringRange;

public class DicomWindowPixelConverter extends DicomPixelConverter {

   private static final int BYTE_MASK = 0xFF;
   private static final int SHORT_MASK = 0xFFFF;
   private static final byte BYTE_MAX = (byte)BYTE_MASK;
   private static final short SHORT_MAX = (short)SHORT_MASK;
   
   public static PropertyList myProps = new PropertyList(DicomWindowPixelConverter.class);   
   
   static {
      myProps.add("window", "window preset", "CUSTOM");
      myProps.add("windowCenter", "Center intensity value", 0x7FF, "[-Inf,Inf]");
      myProps.add("windowWidth", "Width of window", 0x7FF, "[1,Inf]");
   }
   
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
   public static final int DEFAULT_WINDOW_WIDTH = 0x0007FF;
   
   private HashMap<String,WindowPreset> presetMap;
   WindowPreset currentPreset;
   WindowPreset customPreset;
   int windowCenter;
   int windowWidth;
   int nextIdx = 0;
   
   public DicomWindowPixelConverter() {
      this(DEFAULT_WINDOW_CENTER, DEFAULT_WINDOW_WIDTH);
   }
   
   public DicomWindowPixelConverter(int center, int width) {
      presetMap = new HashMap<String, WindowPreset>();
      customPreset = new WindowPreset("CUSTOM", center, width, Integer.MAX_VALUE);
      presetMap.put("CUSTOM", customPreset);
      setWindow("CUSTOM");
   }
   
   public String getWindow() {
      return currentPreset.name;
   }
   
   public String[] getWindowNames() {
      return  presetMap.keySet().toArray(new String[0]);
   }
   
   public int numWindows() {
      return presetMap.size();
   }
   
   public void addWindowPreset(String preset, int center, int width) {
      presetMap.put(preset, new WindowPreset(preset, center, width, nextIdx++));
   }
   
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
   
   public StringRange getWindowRange() {
      WindowPreset[] presets = presetMap.values().toArray(new WindowPreset[0]);
      Arrays.sort(presets);
      String[] range = new String[presets.length];
      for (int i=0; i<presets.length; i++) {
         range[i] = presets[i].name;
      }
      return new StringRange(range);
   }
   
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
         notifyHostOfPropertyChange("windowCenter");
      }
   }
   
   public int getWindowCenter() {
      return windowCenter;
   }
   
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
         
         notifyHostOfPropertyChange("windowWidth");
      }
      
   }
   
   public int getWindowWidth() {
      return windowWidth;
   }
   
   @Override
   public int interpByteRGB(byte[] in, int idx, byte[] out, int odx) {
      
      byte bval;
      
      // 2x-2c-1
      int val = ( in[idx] << 1 ) - (windowCenter << 1) + 1;
      if ( val <= -(windowWidth-1) ) {
         bval = 0;
      } else if ( val > (windowWidth-1) ) {
         bval = BYTE_MAX;
      } else {
         val = ( ( (val << 8) - val ) / (windowWidth-1) ) >>> 1;
         bval = (byte)val;
      }
      
      out[odx++] = bval;
      out[odx++] = bval;
      out[odx++] = bval;
      return odx;
   }

   @Override
   public int interpByteByte(byte[] in, int idx, byte[] out, int odx) {
      // 2x-2c-1
      int val = ( in[idx] << 1 ) - (windowCenter << 1) + 1;
      if ( val <= -(windowWidth-1) ) {
         out[odx++] = 0;
      } else if ( val > (windowWidth-1) ) {
         out[odx++] = BYTE_MAX;
      } else {
         out[odx++] = (byte)( ( ( (val << 8) - val ) / (windowWidth-1) ) >>> 1 );
      }
      return odx;
   }

   @Override
   public int interpByteShort(byte[] in, int idx, short[] out, int odx) {
      // 2x-2c-1
      int val = ( in[idx] << 1 ) - (windowCenter << 1) + 1;
      if ( val <= -(windowWidth-1) ) {
         out[odx++] = 0;
      } else if ( val > (windowWidth-1) ) {
         out[odx++] = SHORT_MAX;
      } else {
         out[odx++] = (byte)( ( ( (val << 16) - val ) / (windowWidth-1) ) >>> 1 );
      }
      return odx;
   }
   
   @Override
   public int interpRGBRGB(byte[] in, int idx, byte[] out, int odx) {
      
      for (int i=0; i<3; i++) {
         int val = ( (in[idx++] & BYTE_MASK) << 1 ) - (windowCenter << 1) + 1;
         if ( val <= -(windowWidth-1) ) {
            out[odx++] = 0;
         } else if ( val > (windowWidth-1) ) {
            out[odx++] = BYTE_MAX;
         } else {
            out[odx++] = (byte)( ( ( (val << 8) - val ) / (windowWidth-1) ) >>> 1 );
         }
      }
      return odx;
   }

   @Override
   public int interpRGBByte(byte[] in, int idx, byte[] out, int odx) {
      int val = (in[idx] & BYTE_MASK) + (in[idx+1] & BYTE_MASK) + (in[idx+2] & BYTE_MASK);
      val = ( val << 1 ) - 3*(windowCenter << 1) + 3;
      if (val <= -3*(windowWidth-1)) {
         out[odx++] = 0;
      } else if (val > 3*(windowWidth-1)) {
         out[odx++] = BYTE_MAX;
      } else {
         out[odx++] = (byte)( (  ((val << 8) - val) / (3* (windowWidth-1) ) ) >>> 1);
      }
      return odx;
   }

   @Override
   public int interpRGBShort(byte[] in, int idx, short[] out, int odx) {
      int val = (in[idx] & BYTE_MASK) + (in[idx+1] & BYTE_MASK) + (in[idx+2] & BYTE_MASK);
      val = ( val << 1 ) - 3*(windowCenter << 1) + 3;
      if (val <= -3*(windowWidth-1)) {
         out[odx++] = 0;
      } else if (val > 3*(windowWidth-1)) {
         out[odx++] = SHORT_MAX;
      } else {
         out[odx++] = (byte)( (  ((val << 16) - val) / (3* (windowWidth-1) ) ) >>> 1);
      }
      return odx;
   }

   @Override
   public int interpShortRGB(short[] in, int idx, byte[] out, int odx) {
      byte bval;
      // 2x-2c-1
      int val = ( in[idx] << 1 ) - (windowCenter << 1) + 1;
      if ( val <= -(windowWidth-1) ) {
         bval = 0;
      } else if ( val > (windowWidth-1) ) {
         bval = BYTE_MAX;
      } else {
         val = ( ( (val << 8) - val ) / (windowWidth-1) ) >>> 1;
         bval = (byte)val;
      }
      out[odx++] = bval;
      out[odx++] = bval;
      out[odx++] = bval;
      return odx;
   }

   @Override
   public int interpShortByte(short[] in, int idx, byte[] out, int odx) {
      // 2x-2c-1
      int val = ( in[idx] << 1 ) - (windowCenter << 1) + 1;
      if ( val <= -(windowWidth-1) ) {
         out[odx++] = 0;
      } else if ( val > (windowWidth-1) ) {
         out[odx++] = BYTE_MAX;
      } else {
         val = val + (windowWidth-1);
         out[odx++] = (byte)( ( ( (val << 8) - val ) / (windowWidth-1) ) >>> 1 );
      }
      return odx;
   }

   @Override
   public int interpShortShort(short[] in, int idx, short[] out, int odx) {
      // 2x-2c-1
      int val = ( in[idx] << 1 ) - (windowCenter << 1) + 1;
      if ( val <= -(windowWidth-1) ) {
         out[odx++] = 0;
      } else if ( val > (windowWidth-1) ) {
         out[odx++] = SHORT_MAX;
      } else {
         out[odx++] = (byte)( ( ( (val << 16) - val ) / (windowWidth-1) ) >>> 1 );
      }
      return odx;
   }

   @Override
   public int
      interp(DicomPixelBuffer in, int idx, DicomPixelBuffer out, int odx) {
      
      switch (in.getPixelType()) {
         case BYTE: {
            
            switch (out.getPixelType()) {
               case BYTE: {
                  return interpByteByte((byte[])in.getPixels(), idx, (byte[])out.getPixels(), odx);
               }
               case RGB: {
                  return interpByteRGB((byte[])in.getPixels(), idx, (byte[])out.getPixels(), odx);
               }
               case SHORT: {
                  return interpByteShort((byte[])in.getPixels(), idx, (short[])out.getPixels(), odx);
               }
               default: {
                  throw new IllegalArgumentException("Unknown type: " + out.getPixelType());
               }  
            }
         }
         case RGB: {
            switch (out.getPixelType()) {
               case BYTE: {
                  return interpRGBByte((byte[])in.getPixels(), idx, (byte[])out.getPixels(), odx);
               }
               case RGB: {
                  return interpRGBRGB((byte[])in.getPixels(), idx, (byte[])out.getPixels(), odx);
               }
               case SHORT: {
                  return interpRGBShort((byte[])in.getPixels(), idx, (short[])out.getPixels(), odx);
               }
               default: {
                  throw new IllegalArgumentException("Unknown type: " + out.getPixelType());
               }  
            }
         }
         case SHORT: {
            switch (out.getPixelType()) {
               case BYTE: {
                  return interpShortByte((short[])in.getPixels(), idx, (byte[])out.getPixels(), odx);
               }
               case RGB: {
                  return interpShortRGB((short[])in.getPixels(), idx, (byte[])out.getPixels(), odx);
               }
               case SHORT: {
                  return interpShortShort((short[])in.getPixels(), idx, (short[])out.getPixels(), odx);
               }
               default: {
                  throw new IllegalArgumentException("Unknown type: " + out.getPixelType());
               }  
            }
         }
         default: {
            throw new IllegalArgumentException("Unknown type: " + in.getPixelType());
         }
         
      }
      
   }
   
}
