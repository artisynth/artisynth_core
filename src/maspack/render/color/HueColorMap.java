package maspack.render.color;

import java.awt.Color;

import maspack.properties.PropertyList;
import maspack.util.DoubleInterval;

/**
 * Interpolates color values by varying the hue
 * @author Antonio
 *
 */
public class HueColorMap extends ColorMapBase {

   public static double defaultMinHue = 0;       // red
   public static double defaultMaxHue = 5.0/6.0; // violet
   public static double defaultSaturation = 1;
   public static double defaultBrightness = 1;
   
   private double [] tmpC = new double[3];
   
   private double myMinHue;
   private double myMaxHue;
   private double mySaturation;
   private double myBrightness;
   private DoubleInterval myHueRange;
   
   public static PropertyList myProps = new PropertyList (HueColorMap.class, 
      ColorMapBase.class);
   static {
      myProps.add("hueRange * *", "range of hues", new DoubleInterval(defaultMinHue, defaultMaxHue));
      myProps.add("saturation * *", "saturation", defaultSaturation,"[0,1]");
      myProps.add("brightness * *", "brightness", defaultBrightness,"[0,1]");
   }
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   /**
    * Creates a default Hue-varying colormap
    */
   public HueColorMap() {
      this(defaultMinHue, defaultMaxHue);
   }
   
   /**
    * Creates a hue map, setting the min and max hues
    * @see HueColorMap#setHueRange(double, double)
    */
   public HueColorMap(double minHue, double maxHue) {
      setHueRange(minHue, maxHue);
      mySaturation = defaultSaturation;
      myBrightness = defaultBrightness;
   }
   
   /**
    * Sets the hue range.  Normally, these values are in the range
    * [0, 1]; however, the hues support wrapping.  If maxHue is less
    * than minHue, colors will be interpolated backwards.
    * <p>
    * Examples:<br>
    * [0, 1/6] will interpolate from red to yellow <br>
    * [1/6, 0] will interpolate from yellow to red <br>
    * [5/6, 7/6] will interpolate from violet to yellow <br>
    */
   public void setHueRange(double minHue, double maxHue) {
      myMinHue = minHue;
      myMaxHue = maxHue;
      if (myHueRange == null) {
         myHueRange = new DoubleInterval();
      }
      myHueRange.set(minHue, maxHue);
   }
   
   /**
    * Saturation to use for interpolation
    */
   public void setSaturation(double s) {
      mySaturation = Math.min(Math.max(s,0),1);
   }
   
   /**
    * Constant saturation used by map
    */
   public double getSaturation() {
      return mySaturation;
   }
   
   /**
    * Brightness value to use for interpolation
    */
   public void setBrightness(double v) {
      myBrightness = Math.min(Math.max(v,0),1);
   }
   
   /**
    * Constant brightness used by map
    */
   public double getBrightness() {
      return myBrightness;
   }
   
   @Override
   public Color getColor(double a) {
      getHSV(a, tmpC);
      return Color.getHSBColor((float)tmpC[0], (float)tmpC[1], (float)tmpC[2]);
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
      
      hsv[0] = getHue(a);
      hsv[1] = mySaturation;
      hsv[2] = myBrightness;

   }
   
   /**
    * Computes the interpolated hue with a in [0,1]
    */
   public double getHue(double a) {
      double h = (1-a)*myMinHue + a*myMaxHue;
      h = h % 1;
      return h;
   }

   @Override
   public void getHSV(double a, float[] hsv) {
      hsv[0] = (float)getHue(a);
      hsv[1] = (float)mySaturation;
      hsv[2] = (float)myBrightness;
   }
   
   /**
    * Gets the range of hue values
    * @see #setHueRange(double, double)
    */
   public DoubleInterval getHueRange() {
      return myHueRange;
   }
   
   /**
    * Sets the hue range.  Normally, these values are in the range
    * [0, 1]; however, the hues support wrapping.  If maxHue is less
    * than minHue, colors will be interpolated backwards.
    * <p>
    * Examples:<br>
    * [0, 1/6] will interpolate from red to yellow <br>
    * [1/6, 0] will interpolate from yellow to red <br>
    * [5/6, 7/6] will interpolate from violet to yellow <br>
    */
   public void setHueRange(DoubleInterval range) {
      setHueRange(range.getLowerBound(), range.getUpperBound());
   }

   @Override
   public HueColorMap copy() {
      HueColorMap out = new HueColorMap(myMinHue, myMaxHue);
      out.myBrightness = myBrightness;
      out.mySaturation = mySaturation;
      return out;
   }
   
   @Override
   public ColorMapBase clone() throws CloneNotSupportedException {
      return copy();
   }
   

}
