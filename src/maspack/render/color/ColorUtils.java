package maspack.render.color;

import maspack.render.GL.GLSupport;

public class ColorUtils {

   /**
    * Checks if two colors differ within supplied epsilon.  Colors must
    * have length of at least 3.  If only RGB is supplied, the alpha
    * component is assumed to be one.
    * @param rgba1 first color
    * @param rgba2 second color
    * @return true if any component differs by more than epsilon
    */
   public static boolean RGBAequals(float[] rgba1, float[] rgba2, float eps) {
      for (int i=0; i<3; ++i) {
         if (Math.abs (rgba1[i]-rgba2[2])>eps) {
            return false;
         }
      }
      float a1 = 1;
      float a2 = 1;
      if (rgba1.length > 3) {
         a1 = rgba1[3];
      }
      if (rgba2.length > 3) {
         a2 = rgba2[3];
      }
      return ( Math.abs(a1-a2) <= eps); 
   }

   /**
    * Computes <code>(1-s) color0 + s color1</code> and places the result
    * in <code>result</code>. Only the first three array entries are used.
    * 
    * @param result result value
    * @param color0 first color
    * @param color1 second color
    * @param s interpolation parameter
    */
   public static void interpolateColor (
      float[] result, float[] color0, float[] color1, double s) {
      
      float a = (float)(1-s);
      float b = (float)(s);
   
      result[0] = a*color0[0] + b*color1[0];
      result[1] = a*color0[1] + b*color1[1];
      result[2] = a*color0[2] + b*color1[2];
   }

   /** 
    * Convert an HSV color to RGB representation. It is assumed that all RBG
    * and HSV values are in the range 0-1 (as opposed to the typical HSV ranges
    * of [0-360], [0-100], [0-100]).
    */
   public static void HSVtoRGB (float[] hsv, float[] rgb) {
      float f, p, q, t, hRound;
      int hIndex;
      float h, s, v;
   
      h = hsv[0];
      s = hsv[1];
      v = hsv[2];
   
      if (h < 0) {
         h = 0;
      }
      else if (h >= 1) {
         h = 0;
      }
      hRound = (int)(h*6.0);
      hIndex = ((int)hRound)%6;
      f = (h*6.0f) - hRound;
      p = v*(1.0f - s);
      q = v*(1.0f - f*s);
      t = v*(1.0f - (1.0f - f)*s);
   
      switch(hIndex) {
         case 0:
            rgb[0] = v; rgb[1] = t; rgb[2] = p;
            break;
         case 1:
            rgb[0] = q; rgb[1] = v; rgb[2] = p;
            break;
         case 2:
            rgb[0] = p; rgb[1] = v; rgb[2] = t;
            break;
         case 3:
            rgb[0] = p; rgb[1] = q; rgb[2] = v;
            break;
         case 4:
            rgb[0] = t; rgb[1] = p; rgb[2] = v;
            break;
         case 5:
            rgb[0] = v; rgb[1] = p; rgb[2] = q;
            break;
      }
   }

   private static int RED = 0;
   private static int GREEN = 1;
   private static int BLUE = 2;
   private static float EPSILON = 1.0e-6f;
   
   /** 
    * Convert an RGB color to HSV representation. It is assumed that all RBG
    * and HSV values are in the range 0-1 (as opposed to the typical HSV ranges
    * of [0-360], [0-100], [0-100]).
    */
   public static void RGBtoHSV (float[] hsv, float[] rgb) {
   
      float r = rgb[0];
      float g = rgb[1];
      float b = rgb[2];
   
      float h, s, v, diff;
   
      int minIdx = 0;
      int maxIdx = 0;
      float maxCol = r;
      float minCol = r;
      if (g > maxCol) {
         maxCol = g;
         maxIdx = GREEN;
      }
      if (g < minCol) {
         minCol = g;
         minIdx = GREEN;
      }
      if (b > maxCol) {
         maxCol = b;
         maxIdx = BLUE;
      }
      if (b < minCol) {
         minCol = b;
         minIdx = BLUE;
      }
   
      diff = maxCol - minCol;
      /* H */
      h = 0.0f;
      if (diff >=  EPSILON) {
         if(maxIdx == RED){
            h = ((1.0f/6.0f) * ( (g - b) / diff )) + 1.0f;
            h = h - (int)h;
         }
         else if(maxIdx == GREEN){
            h = ((1.0f/6.0f) * ( (b - r) / diff )) + (1.0f/3.0f);
         }
         else if(maxIdx == BLUE){
            h = ((1.0f/6.0f) * ( (r - g) / diff )) + (2.0f/3.0f);        
         }
      }
      /* S */
      if(maxCol < EPSILON)
         s = 0;
      else
         s = (maxCol - minCol) / maxCol;
   
      /* V */
      v = maxCol;
   
      hsv[0] = h; hsv[1] = s; hsv[2] = v;
   }
   
}
