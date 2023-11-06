package maspack.render.color;

import java.awt.Color;
import maspack.util.*;

/**
 * Unit tests for HSL.
 */
public class HSLTest extends UnitTest {
   
   public void test() {
      
      Color[] colors = new Color[] {
         new Color (1f, 133/255f, 1f),
         new Color (1f, 133/255f, 203/255f),
         new Color (1f, 203/255f, 133/255f),
         new Color (133/255f, 1f, 203/255f),
         new Color (203/255f, 1f, 133/255f),
         new Color (203/255f, 133/255f, 1f),
         new Color (133/255f, 203/255f, 1f),
         new Color (37/255f, 1f, 132/255f),
         new Color (1f, 1f, 1f),
         new Color (0f, 0f, 0f),
      };
      float[][] hslChks = new float[][] {
         new float[] { 300/360f, 1f, 0.76f },
         new float[] { 325/360f, 1f, 0.76f },
         new float[] { 34/360f, 1f, 0.76f },
         new float[] { 154/360f, 1f, 0.76f },
         new float[] { 85/360f, 1f, 0.76f },
         new float[] { 274/360f, 1f, 0.76f },
         new float[] { 205/360f, 1f, 0.76f },
         new float[] { 146/360f, 1f, 0.57f },
         new float[] { 0f, 0f, 1f },
         new float[] { 0f, 0f, 0f },
      };
      int k = 0;
      for (Color c : colors) {
         float[] rgb = c.getRGBColorComponents(null);
         float[] hsl = HSL.getHSLComponents (c);
         float[] chk = hslChks[k];
         for (int i=0; i<3; i++) {
            if (Math.abs(hsl[i]-chk[i]) > 1/255.0) {
               System.out.printf (
                  "  rgb %8.5f %8.5f %8.5f\n", rgb[0], rgb[1], rgb[2]);
               System.out.printf (
                  "  hsl %8.5f %8.5f %8.5f\n", hsl[0], hsl[1], hsl[2]);
               System.out.printf (
                  "  chk %8.5f %8.5f %8.5f\n", chk[0], chk[1], chk[2]);
               throw new TestException (
                  "RGBtoHSL failed, k=" + k +
                  " err=" + Math.abs(hsl[i]-chk[i]));
            }
         }
         chk = HSL.HSLtoRGB (null, hsl);
         for (int i=0; i<3; i++) {
            if (Math.abs(rgb[i]-chk[i]) > 1e-6) {
               System.out.printf (
                  "  rgb %8.5f %8.5f %8.5f\n", rgb[0], rgb[1], rgb[2]);
               System.out.printf (
                  "  hsl %8.5f %8.5f %8.5f\n", hsl[0], hsl[1], hsl[2]);
               System.out.printf (
                  "  chk %8.5f %8.5f %8.5f\n", chk[0], chk[1], chk[2]);
               throw new TestException (
                  "RGB to HSL to RGB failed for random case, k=" + k +
                  " err=" + Math.abs(rgb[i]-chk[i]));
            }
         }
         k++;
      }

      int nrand = 1000;
      for (k=0; k<nrand; k++) {
         float r = (float)RandomGenerator.nextDouble (0, 1);
         float g = (float)RandomGenerator.nextDouble (0, 1);
         float b = (float)RandomGenerator.nextDouble (0, 1);
         float[] rgb = new float[] { r, g, b };
         float[] hsl = HSL.RGBtoHSL (null, rgb);
         float[] chk = HSL.HSLtoRGB (null, hsl);
         for (int i=0; i<3; i++) {
            if (Math.abs(rgb[i]-chk[i]) > 1e-6) {
               System.out.printf (
                  "  rgb %8.5f %8.5f %8.5f\n", rgb[0], rgb[1], rgb[2]);
               System.out.printf (
                  "  hsl %8.5f %8.5f %8.5f\n", hsl[0], hsl[1], hsl[2]);
               System.out.printf (
                  "  chk %8.5f %8.5f %8.5f\n", chk[0], chk[1], chk[2]);
               throw new TestException (
                  "RGB to HSL to RGB failed for random case, k=" + k +
                  " err=" + Math.abs(rgb[i]-chk[i]));
            }
         }
      }
      
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);

      HSLTest tester = new HSLTest();
      tester.runtest();
   }
}
