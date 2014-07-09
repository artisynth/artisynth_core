package maspack.render.color;

import java.awt.Color;
import java.util.ArrayList;

import maspack.properties.PropertyList;

public abstract class InterpolatingColorMap extends ColorMapBase {
      
      public static final Color[] defaultColorArray = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, 
         Color.CYAN, Color.BLUE, Color.MAGENTA};
   
      protected Color[] myColorArray;
      protected float[] ctmp = new float[3];
      String myColorString;
      
      public static PropertyList myProps = new PropertyList (InterpolatingColorMap.class, 
         ColorMapBase.class);
      
      static {
         myProps.add("colorArray getColorString setColorString", "set of colors", 
            getColorString(defaultColorArray));
      }
      @Override
      public PropertyList getAllPropertyInfo() {
         return myProps;
      }
      
      /**
       * Sets the list of colors within which to interpolate.
       * These colors are evenly distributed in the interval
       * [0, 1], and interpolation is linear between any pairs
       */
      public void setColorArray(Color[] colors) {
         myColorArray = colors;
         myColorString = getColorString(colors);
      }
      
      public String getColorString() {
         return myColorString;
      }
      
      public void setColorString(String str) {
         if (!str.equalsIgnoreCase(myColorString)) {
            Color[] carray = getColorArray(str);
            setColorArray(carray);
         }
      }
      
      @Override
      public Color getColor(double a) {
         getRGB(a, ctmp);
         return new Color(ctmp[0], ctmp[1], ctmp[2]);
      }

      @Override
      public void getRGB(double a, double[] rgb) {
         
         int nColors = myColorArray.length;  

         // determine where we are
         double c = a*(nColors-1);
         int b = (int)c;
         if (b >= nColors-1) {
            b = nColors-2;
            a = 1;
         } else if (b < 0) {
            b = 0;
            a = 0;
         } else {
            a = c-b; // fraction left over
         }
         
         interpolateColorRGB(a, myColorArray[b], myColorArray[b+1], rgb);
         
      }
      
      protected abstract void interpolateColorRGB(double a, Color c1, Color c2, float[] rgb);
      protected abstract void interpolateColorHSV(double a, Color c1, Color c2, float[] hsv);
      protected abstract void interpolateColorRGB(double a, Color c1, Color c2, double[] rgb);
      protected abstract void interpolateColorHSV(double a, Color c1, Color c2, double[] hsv);

      @Override
      public void getRGB(double a, float[] rgb) {
         int nColors = myColorArray.length;  

         // determine where we are
         double c = a*(nColors-1);
         int b = (int)c;
         if (b >= nColors-1) {
            b = nColors-2;
            a = 1;
         } else if (b < 0) {
            b = 0;
            a = 0;
         } else {
            a = c-b; // fraction left over
         }
         
         interpolateColorRGB(a, myColorArray[b], myColorArray[b+1], rgb);
      }

      @Override
      public void getHSV(double a, double[] hsv) {
         int nColors = myColorArray.length;  

         // determine where we are
         double c = a*(nColors-1);
         int b = (int)c;
         if (b >= nColors-1) {
            b = nColors-2;
            a = 1;
         } else if (b < 0) {
            b = 0;
            a = 0;
         } else {
            a = c-b; // fraction left over
         }
         
         interpolateColorHSV(a, myColorArray[b], myColorArray[b+1], hsv);
      }

      @Override
      public void getHSV(double a, float[] hsv) {
         int nColors = myColorArray.length;  

         // determine where we are
         double c = a*(nColors-1);
         int b = (int)c;
         if (b >= nColors-1) {
            b = nColors-2;
            a = 1;
         } else if (b < 0) {
            b = 0;
            a = 0;
         } else {
            a = c-b; // fraction left over
         }
         
         interpolateColorRGB(a, myColorArray[b], myColorArray[b+1], hsv);
      }
      
      /**
       * Creates a string listing an array of colors of the
       * form "0x________ 0x________ ..."
       */
      public static String getColorString(Color[] carray) {
         // get integers
         StringBuilder out = new StringBuilder();
         
         int cint = getColor3Value(carray[0]);
         out.append( String.format("0x%06x", cint) );
         for (int i=1; i<carray.length; i++) {
            cint = getColor3Value(carray[i]);
            out.append( String.format(" 0x%06x", cint) );
         }
         
         return out.toString();
      }
      
      /**
       * Parses a string listing colors in hex format to generate
       * an array of {@link Color} objects
       */
      public static Color[] getColorArray(String colorString) {
         colorString = colorString.replaceAll("[^0-9A-Fa-fxX\\ ]", "");
         String[] strC = colorString.split(" ");
         ArrayList<Color> out = new ArrayList<Color>();
         
         int cInt;
         for (String c : strC) {
            try {
               cInt = Integer.decode(c);
               out.add(new Color(cInt));
            } catch (Exception e) {
               System.err.println("Cannot decode integer " + c);
            }
         }
         
         return out.toArray(new Color[out.size()]);
         
      }
      
      @Override
      public abstract InterpolatingColorMap copy();
      
      public InterpolatingColorMap clone() {
         return copy();
      }

}
