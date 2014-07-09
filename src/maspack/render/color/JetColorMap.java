package maspack.render.color;

import java.awt.Color;

import maspack.properties.PropertyDesc;
import maspack.properties.PropertyList;

/**
 * Colormap that implements Matlab's default "jet" interpolation.
 * More generally, it interpolates between a given list of colors
 * @author Antonio
 *
 */
public class JetColorMap extends InterpolatingColorMap {

   private static Color[] defaultColorArray = new Color[] {
      new Color(0x00007F), // dark blue
      new Color(0x0000FF), // blue
      new Color(0x007FFF), // dark cyan
      new Color(0x00FFFF), // cyan
      new Color(0x7FFF7F), // dark green
      new Color(0xFFFF00), // yellow
      new Color(0xFF7F00), // orange
      new Color(0xFF0000), // red
      new Color(0x7F0000), // dark red
   };
   
   public static PropertyList myProps = new PropertyList (JetColorMap.class, 
      InterpolatingColorMap.class);
   static {
      PropertyDesc desc = myProps.get("colorArray");
      desc.setDefaultValue(getColorString(defaultColorArray));
   }
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public JetColorMap() {
      this(defaultColorArray);
   }
   
   /**
    * Creates a ColorMap that evenly interpolates between 
    * the supplied colors.
    *
    * @see #setColorArray(Color[])
    */
   public JetColorMap(Color[] colors) {
      setColorArray(colors);
   }
   
   @Override
   public JetColorMap copy() {
      return new JetColorMap(myColorArray);
   }
   
   public JetColorMap clone() {
      return copy();
   }

   @Override
   protected void
      interpolateColorRGB(double a, Color c1, Color c2, float[] rgb) {
      rgb[0] = (float)(((1-a)*c1.getRed() + a*c2.getRed())/255.0);
      rgb[1] = (float)(((1-a)*c1.getGreen() + a*c2.getGreen())/255.0);
      rgb[2] = (float)(((1-a)*c1.getBlue() + a*c2.getBlue())/255.0);
      
   }

   @Override
   protected void
      interpolateColorHSV(double a, Color c1, Color c2, float[] hsv) {
      hsv[0] = (float)(((1-a)*c1.getRed() + a*c2.getRed())/255.0);
      hsv[1] = (float)(((1-a)*c1.getGreen() + a*c2.getGreen())/255.0);
      hsv[2] = (float)(((1-a)*c1.getBlue() + a*c2.getBlue())/255.0);
      RGBtoHSV(hsv, hsv);
   }

   @Override
   protected void
      interpolateColorRGB(double a, Color c1, Color c2, double[] rgb) {
      rgb[0] = (((1-a)*c1.getRed() + a*c2.getRed())/255.0);
      rgb[1] = (((1-a)*c1.getGreen() + a*c2.getGreen())/255.0);
      rgb[2] = (((1-a)*c1.getBlue() + a*c2.getBlue())/255.0);      
   }

   @Override
   protected void
      interpolateColorHSV(double a, Color c1, Color c2, double[] hsv) {
      hsv[0] = (((1-a)*c1.getRed() + a*c2.getRed())/255.0);
      hsv[1] = (((1-a)*c1.getGreen() + a*c2.getGreen())/255.0);
      hsv[2] = (((1-a)*c1.getBlue() + a*c2.getBlue())/255.0);      
      RGBtoHSV(hsv, hsv);
   }
   
   

}
