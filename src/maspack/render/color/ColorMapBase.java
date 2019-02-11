package maspack.render.color;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;

import maspack.properties.CompositeProperty;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyInfo;
import maspack.properties.PropertyList;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Scannable;

public abstract class ColorMapBase
   implements ColorMap, HasProperties, CompositeProperty, Scannable {

   static Class<?>[] mySubClasses = new Class<?>[] {
      HueColorMap.class,
      GreyscaleColorMap.class,
      JetColorMap.class,
      RainbowColorMap.class
   };

   public static PropertyList myProps = new PropertyList (ColorMapBase.class);
   PropertyInfo myPropInfo;
   HasProperties myPropHost;
   public static Class<?>[] getSubClasses() {
      return mySubClasses;
   }
   
   public Property getProperty(String pathName) {
      return PropertyList.getProperty(pathName, this);
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   public PropertyInfo getPropertyInfo () { 
      return myPropInfo;
    }

   public void setPropertyInfo (PropertyInfo info)  {
      myPropInfo = info;
    }

   public HasProperties getPropertyHost()  {
      return myPropHost;
    }

   public void setPropertyHost (HasProperties newParent)  {
      myPropHost = newParent;
    }
   
   /**
    * Converts hsv to rgb.  It is safe to have the same
    * array as both input and output.
    */
   protected static void HSVtoRGB(double hsv[], double rgb[] ) {
      double h = hsv[0];
      double s = hsv[1];
      double v = hsv[2];

      if (s == 0) {
         rgb[0] = v;
         rgb[1] = v;
         rgb[2] = v;
      } else {
         h = (h - Math.floor(h))* 6;
         double f = h - Math.floor(h);
         double p = v * (1.0 - s);
         double q = v * (1.0 - s * f);
         double t = v * (1.0 - (s * (1.0 - f)));
         switch ((int) h) {
            case 0:
               rgb[0] = v;
               rgb[1] = t;
               rgb[2] = p;
               break;
            case 1:
               rgb[0] = q; 
               rgb[1] = v; 
               rgb[2] = p; 
               break;
            case 2:
               rgb[0] = p;
               rgb[1] = v;
               rgb[2] = t;
               break;
            case 3:
               rgb[0] = p;
               rgb[1] = q;
               rgb[2] = v;
               break;
            case 4:
               rgb[0] = t;
               rgb[1] = p;
               rgb[2] = v;
               break;
            case 5:
               rgb[0] = v;
               rgb[1] = p;
               rgb[2] = q;
               break;
         }
      }

   }

   /**
    * Converts rgb to hsv.  It is safe to have the same
    * array as both input and output.
    */
   protected static void RGBtoHSV(double rgb[], double hsv[] ) {
      double r = rgb[0];
      double g = rgb[1];
      double b = rgb[2];

      double cmax = (r > g) ? r : g;
      if (b > cmax) cmax = b;
      double cmin = (r < g) ? r : g;
      if (b < cmin) cmin = b;

      hsv[2] = cmax;
      if (cmax != 0)
         hsv[1] = (cmax - cmin) / cmax;
      else
         hsv[1] = 0;
      
      if (cmin == cmax) {
         hsv[0] = 0;
      } else if (hsv[1] == 0)
         hsv[0] = 0;
      else {
         double hue = 0;
         double redc = (cmax - r) / (cmax - cmin);
         double greenc = (cmax - g) / (cmax - cmin);
         double bluec = (cmax - b) / (cmax - cmin);
         if (r == cmax)
            hue = bluec - greenc;
         else if (g == cmax)
            hue = 2.0 + redc - bluec;
         else
            hue = 4.0 + greenc - redc;
         hue = hue / 6.0;
         if (hue < 0)
            hue = hue + 1.0;
         hsv[0] = hue;
      }
   }

   /**
    * Converts hsv to rgb.  It is safe to have the same
    * array as both input and output.
    */
   protected static void HSVtoRGB(float hsv[], float rgb[] ) {
      float h = hsv[0];
      float s = hsv[1];
      float v = hsv[2];

      if (s == 0) {
         rgb[0] = v;
         rgb[1] = v;
         rgb[2] = v;
      } else {
         h = (float)(h - Math.floor(h))* 6;
         float f = h - (float)Math.floor(h);
         float p = v * (1.0f - s);
         float q = v * (1.0f - s * f);
         float t = v * (1.0f - (s * (1.0f - f)));
         switch ((int) h) {
            case 0:
               rgb[0] = v;
               rgb[1] = t;
               rgb[2] = p;
               break;
            case 1:
               rgb[0] = q; 
               rgb[1] = v; 
               rgb[2] = p; 
               break;
            case 2:
               rgb[0] = p;
               rgb[1] = v;
               rgb[2] = t;
               break;
            case 3:
               rgb[0] = p;
               rgb[1] = q;
               rgb[2] = v;
               break;
            case 4:
               rgb[0] = t;
               rgb[1] = p;
               rgb[2] = v;
               break;
            case 5:
               rgb[0] = v;
               rgb[1] = p;
               rgb[2] = q;
               break;
         }
      }

   }

   /**
    * Converts rgb to hsv.  It is safe to have the same
    * array as both input and output.
    */
   protected static void RGBtoHSV(float rgb[], float hsv[] ) {
      float r = rgb[0];
      float g = rgb[1];
      float b = rgb[2];

      float cmax = (r > g) ? r : g;
      if (b > cmax) cmax = b;
      float cmin = (r < g) ? r : g;
      if (b < cmin) cmin = b;

      hsv[2] = cmax;
      if (cmax != 0)
         hsv[1] = (cmax - cmin) / cmax;
      else
         hsv[1] = 0;
      if (hsv[1] == 0)
         hsv[0] = 0;
      else {
         float hue = 0;
         float redc = (cmax - r) / (cmax - cmin);
         float greenc = (cmax - g) / (cmax - cmin);
         float bluec = (cmax - b) / (cmax - cmin);
         if (r == cmax)
            hue = bluec - greenc;
         else if (g == cmax)
            hue = 2.0f + redc - bluec;
         else
            hue = 4.0f + greenc - redc;
         hue = hue / 6.0f;
         if (hue < 0)
            hue = hue + 1.0f;
         hsv[0] = hue;
      }
   }
   
   public static int getColor3Value(Color c) {
      int cint = c.getRed() << 16;
      cint |= c.getGreen() << 8;
      cint |= c.getBlue();
      return cint;
   }
   
   public static int getColor4Value(Color c) {
      int cint = c.getAlpha() << 24;
      cint |= c.getRed() << 16;
      cint |= c.getGreen() << 8;
      cint |= c.getBlue();
      return cint;
   }
   
   double[] tmp = new double[4];
   
   private static void toByte(double[] dd, byte[] bb) {
      for (int i=0; i<bb.length; ++i) {
         bb[i] = (byte)(dd[i]*255);
      }
   }
   
   @Override
   public void getRGB(double a, byte[] rgb) {
      getRGB(a, tmp);
      toByte(tmp, rgb);
   }
   
   @Override
   public void getHSV(double a, byte[] hsv) {
      getHSV(a, tmp);
      toByte(tmp, hsv);
   }
   
   public abstract ColorMapBase copy();
   
   @Override
   public ColorMapBase clone() throws CloneNotSupportedException {
      throw new CloneNotSupportedException("Clone not implemented");
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      pw.println ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ref);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   /**
    * {@inheritDoc}
    */
   public boolean isWritable() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         if (!getAllPropertyInfo().scanProp (this, rtok)) {
            throw new IOException ("unexpected input: " + rtok);
         }
      }
   }

}
