package maspack.render.color;

import java.awt.Color;

import maspack.properties.PropertyDesc;
import maspack.properties.PropertyList;

/**
 * Similar to JetColorMap, interpolates in HSV
 * space
 * @author Antonio
 *
 */
public class RainbowColorMap extends InterpolatingColorMap {

   public static Color[] rainbow  = new Color[]{
      Color.getHSBColor(0, 1, 1),      // red
      Color.getHSBColor(30f/360, 1, 1),  // orange
      Color.getHSBColor(60f/360, 1, 1),   // yellow
      Color.getHSBColor(120f/360, 1, 1),   // green
      Color.getHSBColor(180f/360, 1, 1),  // cyan
      Color.getHSBColor(240f/360, 1, 1),  // blue
      Color.getHSBColor(270f/360, 1, 1),   // purple
      Color.getHSBColor(300f/360, 1, 1),   // magenta
      
   };
   protected float[][] hsvSet;
   
   public static PropertyList myProps = new PropertyList (RainbowColorMap.class, 
      InterpolatingColorMap.class);
   static {
      PropertyDesc desc = myProps.get("colorArray");
      desc.setDefaultValue(getColorString(rainbow));
   }
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public RainbowColorMap() {
      setColorArray(rainbow);
   }
   
   public RainbowColorMap(Color[] colors) {
      setColorArray(colors);
   }
   
   /**
    * Sets the list of colors within which to interpolate.
    * These colors are evenly distributed in the interval
    * [0, 1], and interpolation is linear between any pairs
    */
   public void setColorArray(Color[] colors) {
      super.setColorArray(colors);
      
      // build internal list of colors
      hsvSet = new float[colors.length][4];
      for (int i=0; i<colors.length; i++) {
         Color.RGBtoHSB(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue(), hsvSet[i]);
         hsvSet[i][3] = (float)colors[i].getAlpha()/255.0f;
      }
   }
   
   private void interpolateHSV(double a, float[] hsv1, float[] hsv2, float [] hsvOut) {
      double h1 = hsv1[0];
      double h2 = hsv2[0];
      
      if (h1 > h2) {
         h2 += 1;
      }
      hsvOut[0] = (float)(((1-a)*h1 + a*h2) % 1);
      hsvOut[1] = (float)((1-a)*hsv1[1] + a*hsv2[1]);
      hsvOut[2] = (float)((1-a)*hsv1[2] + a*hsv2[2]);
   }
   
   private void interpolateHSV(double a, float[] hsv1, float[] hsv2, double [] hsvOut) {
      double h1 = hsv1[0];
      double h2 = hsv2[0];
      
      if (h1 > h2) {
         h2 += 1;
      }
      hsvOut[0] = ((1-a)*h1 + a*h2) % 1;
      hsvOut[1] = ((1-a)*hsv1[1] + a*hsv2[1]);
      hsvOut[2] = ((1-a)*hsv1[2] + a*hsv2[2]);
   }

   @Override
   public void getRGB(double a, double[] rgb) {
      getHSV(a, rgb);
      HSVtoRGB(rgb, rgb);
   }
   
   @Override
   public void getRGB(double a, float[] rgb) {
      getHSV(a, rgb);
      HSVtoRGB(rgb, rgb);
   }

   @Override
   public void getHSV(double a, double[] hsv) {
      int nColors = myColorArray.length;  

      // determine where we are

      // determine where we are
      double c = a*(nColors-1);
      int b = (int)c;
      if (b == nColors-1) {
         b--;
      }
      
      a = c-b; // fraction left over
      interpolateHSV(a, hsvSet[b], hsvSet[b+1], hsv);
   }

   @Override
   public void getHSV(double a, float[] hsv) {
      int nColors = myColorArray.length;  

      // determine where we are

      // determine where we are
      double c = a*(nColors-1);
      int b = (int)c;
      if (b == nColors-1) {
         b--;
      }
      
      a = c-b; // fraction left over
      interpolateHSV(a, hsvSet[b], hsvSet[b+1], hsv);
   }
   
   public RainbowColorMap copy() {
      return new RainbowColorMap(myColorArray);
   }
   
   private float[] hsv1 = new float[3];
   private float[] hsv2 = new float[3];
   @Override
   protected void
      interpolateColorRGB(double a, Color c1, Color c2, float[] rgb) {
      
      Color.RGBtoHSB(c1.getRed(), c1.getGreen(), c1.getBlue(), hsv1);
      Color.RGBtoHSB(c2.getRed(), c2.getGreen(), c2.getBlue(), hsv2);
      interpolateHSV(a, hsv1, hsv2, rgb);
      HSVtoRGB(rgb, rgb);
      
   }

   @Override
   protected void
      interpolateColorHSV(double a, Color c1, Color c2, float[] hsv) {
      Color.RGBtoHSB(c1.getRed(), c1.getGreen(), c1.getBlue(), hsv1);
      Color.RGBtoHSB(c2.getRed(), c2.getGreen(), c2.getBlue(), hsv2);
      interpolateHSV(a, hsv1, hsv2, hsv);
      
   }

   @Override
   protected void
      interpolateColorRGB(double a, Color c1, Color c2, double[] rgb) {
      Color.RGBtoHSB(c1.getRed(), c1.getGreen(), c1.getBlue(), hsv1);
      Color.RGBtoHSB(c2.getRed(), c2.getGreen(), c2.getBlue(), hsv2);
      interpolateHSV(a, hsv1, hsv2, rgb);
      HSVtoRGB(rgb, rgb);
   }

   @Override
   protected void
      interpolateColorHSV(double a, Color c1, Color c2, double[] hsv) {
      Color.RGBtoHSB(c1.getRed(), c1.getGreen(), c1.getBlue(), hsv1);
      Color.RGBtoHSB(c2.getRed(), c2.getGreen(), c2.getBlue(), hsv2);
      interpolateHSV(a, hsv1, hsv2, hsv);
   }
   
}
