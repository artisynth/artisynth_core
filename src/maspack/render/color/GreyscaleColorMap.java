package maspack.render.color;

import java.awt.Color;

import maspack.properties.PropertyList;
import maspack.util.DoubleInterval;

/**
 * Interpolates greyscale color values
 * @author Antonio
 *
 */
public class GreyscaleColorMap extends ColorMapBase {

   public static double defaultMinBrightness = 0;
   public static double defaultMaxBrightness = 1;
   
   public double myMinBrightness;
   public double myMaxBrightness;
   private DoubleInterval myBrightnessRange;
   
   public static PropertyList myProps = new PropertyList (GreyscaleColorMap.class, 
      ColorMapBase.class);
   static {
      myProps.add("brightnessRange * *", "range of grayscale values", 
         new DoubleInterval(defaultMinBrightness, defaultMaxBrightness));
   }
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   /**
    * Creates a default grayscale color map
    */
   public GreyscaleColorMap() {
      this(defaultMinBrightness,defaultMaxBrightness);
   }
   
   /**
    * Creates a grayscale map with specified min and max
    * brightness values
    */
   public GreyscaleColorMap(double min, double max) {
      setBrightnessRange(min, max);
   }
   
   /**
    * Sets the greyscale range [min, max].  If max {@code <} min,
    * then colors will be interpolated from bright to dark
    */
   public void setBrightnessRange(double min, double max) {
      myMinBrightness = Math.max(Math.min(min,1),0);
      myMaxBrightness = Math.max(Math.min(max,1),0);
      if (myBrightnessRange == null) {
         myBrightnessRange = new DoubleInterval();
      }
      myBrightnessRange.set(myMinBrightness, myMaxBrightness);
   }
   
   /**
    * Sets the greyscale range [min, max].  If max {@code <} min,
    * then colors will be interpolated from bright to dark
    */
   public void setBrightnessRange(DoubleInterval range) {
      setBrightnessRange(range.getLowerBound(), range.getUpperBound());
   }
   
   /**
    * Gets the brightness range
    */
   public DoubleInterval getBrightnessRange() {
      return myBrightnessRange;
   }
   
   /**
    * Interpolates a brightness value with parameter a in [0,1]
    */
   public double getBrightness(double a) {
      return (1-a)*myMinBrightness + a*myMaxBrightness;
   }
   
   @Override
   public Color getColor(double a) {
      double b = getBrightness(a);
      return new Color((float)b,(float)b,(float)b);
   }

   @Override
   public void getRGB(double a, double[] rgb) {
      double b = getBrightness(a);
      rgb[0] = b;
      rgb[1] = b;
      rgb[2] = b;
   }

   @Override
   public void getRGB(double a, float[] rgb) {
      float b = (float)getBrightness(a);
      rgb[0] = b;
      rgb[1] = b;
      rgb[2] = b;
   }

   @Override
   public void getHSV(double a, double[] hsv) {
      hsv[0] = 0;
      hsv[1] = 0;
      hsv[2] = getBrightness(a); 
   }

   @Override
   public void getHSV(double a, float[] hsv) {
      hsv[0] = 0;
      hsv[1] = 0;
      hsv[2] = (float)getBrightness(a);
   }
   
   @Override
   public GreyscaleColorMap clone() throws CloneNotSupportedException {
      return copy();
   }
   
   @Override
   public GreyscaleColorMap copy() {
      return new GreyscaleColorMap(myMinBrightness, myMaxBrightness);
   }

}
