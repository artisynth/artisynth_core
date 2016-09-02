package maspack.render.color;

import java.awt.Color;

public interface ColorMap {

   
   /**
    * Returns an interpolated color
    * @param a input, usually in the range [0,1]
    * @return the color
    */
   public Color getColor(double a);
   
   /**
    * Fills an array with the interpolated color values
    * in RGB format
    * @param a input, usually in the range [0,1]
    * @param rgb output color
    */
   public void getRGB(double a, double[] rgb);
   
   /**
    * Fills an array with the interpolated color values
    * in RGB format
    * @param a input, usually in the range [0,1]
    * @param rgb output color
    */
   public void getRGB(double a, float[] rgb);
   
   /**
    * Fills an array with the interpolated color values
    * in RGB format
    * @param a input, usually in the range [0,1]
    * @param rgb output color
    */
   public void getRGB(double a, byte[] rgb);
   
   /**
    * Fills an array with the interpolated color values
    * in HSV format
    * @param a input, usually in the range [0,1]
    * @param hsv output color
    */
   public void getHSV(double a, double[] hsv);
   
   /**
    * Fills an array with the interpolated color values
    * in HSV format
    * @param a input, usually in the range [0,1]
    * @param hsv output color
    */
   public void getHSV(double a, float[] hsv);
   
   /**
    * Fills an array with the interpolated color values
    * in HSV format
    * @param a input, usually in the range [0,1]
    * @param hsv output color
    */
   public void getHSV(double a, byte[] hsv);

}
