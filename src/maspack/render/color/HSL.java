package maspack.render.color;

import java.awt.Color;
import maspack.util.*;

/**
 * Class to convert between RGB and HSL color representations.
 */
public class HSL {
   
   public static float[] RGBtoHSL (float[] hsl, float[] rgb) {
      return RGBtoHSL (hsl, rgb[0], rgb[1], rgb[2]);
   }

   public static float[] RGBtoHSL (float[] hsl, float r, float g, float b) {
      if (hsl == null) {
         hsl = new float[3];
      }
      else if (hsl.length < 3) {
         throw new IllegalArgumentException ("hsl length must be >= 3");
      }
      float max = r;
      float min = r;
      if (g > max) {
         max = g;
      }
      else if (g < min) {
         min = g;
      }
      if (b > max) {
         max = b;
      }
      else if (b < min) {
         min = b;
      }
      float c = max-min;
      float l = max-c/2;
      float h;
      if (c != 0) {
         float hp;
         if (max == r) {
            hp = (g-b)/c;
            if (hp < 0) {
               hp += 6;
            }
         }
         else if (max == g) {
            hp = (b-r)/c + 2;
         }
         else {
            hp = (r-g)/c + 4;
         }
         h = hp/6;
      }
      else {
         h = 0;
      }
      float s;
      if (l ==0 || l == 1) {
         s = 0;
      }
      else {
         s = (max-l)/Math.min(l,1-l);
      }
      hsl[0] = h;
      hsl[1] = s;
      hsl[2] = l;
      return hsl;
   }

   public static float[] HSLtoRGB (float[] rgb, float[] hsl) {
      return HSLtoRGB (rgb, hsl[0], hsl[1], hsl[2]);
   }

   public static float[] HSLtoRGB (float[] rgb, float h, float s, float l) {
      if (rgb == null) {
         rgb = new float[3];
      }
      else if (rgb.length < 3) {
         throw new IllegalArgumentException ("rgb length must be >= 3");
      }

      float c = (1-Math.abs(2*l-1))*s;

      float h6 = h*6;
      float x = c*(1-Math.abs((h6%2) -1));
      float r, g, b;
      r = g = b = 0;
      if (0 <= h6 && h6 < 1) {
         r = c; g = x;
      }
      else if (1 <= h6 && h6 < 2) {
         r = x; g = c;
      }
      else if (2 <= h6 && h6 < 3) {
         g = c; b = x;
      }
      else if (3 <= h6 && h6 < 4) {
         g = x; b = c;
      }
      else if (4 <= h6 && h6 < 5) {
         r = x; b = c;
      }
      else {
         r = c; b = x;
      }
      float m = Math.max(l-c/2,0);
      rgb[0] = r+m;
      rgb[1] = g+m;
      rgb[2] = b+m;
      return rgb;
   }

   public static float[] getHSLComponents (Color color) {
      return RGBtoHSL (null, color.getRGBColorComponents(null));
   }

}
