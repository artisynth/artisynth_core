/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.awt.Color;
import maspack.util.*;
import maspack.widgets.*;
import maspack.render.color.HSL;
import java.io.*;
import java.util.*;

/**
 * Stores information used in plotting a specific trace for a NumericProbe
 * vector.
 */
public class PlotTraceInfo {
   int myOrder;     // order in which the trace should be drawn
   Color myColor;   // color with which to draw the entry
   String myLabel;  // label identifying the entry
   boolean myVisible; // true if the trace is visible

   static class TraceColor extends java.awt.Color {
      private String myName;

      public TraceColor (String name, int r, int g, int b) {
         super (r, g, b);
         myName = name;
      }

      public TraceColor (String name, float r, float g, float b) {
         super (r, g, b);
         myName = name;
      }

      public String getName() {
         return myName;
      }
      
      public TraceColor copy() {
         return new TraceColor (myName, getRed(), getGreen(), getBlue());
      }
   }

   // original colors
   private static TraceColor[] myOldPaletteColors = new TraceColor[] {
      new TraceColor ("Red", 255, 0, 0),
      new TraceColor ("DarkGreen", 0f, 0.5f, 0f),
      new TraceColor ("Blue", 0, 0, 255),
      new TraceColor ("Cyan", 0, 255, 255),      
      new TraceColor ("Magenta", 255, 0, 255),
      new TraceColor ("DarkOrange", 255, 140, 0),
      new TraceColor ("Pink", 255, 150, 150),
      new TraceColor ("BlueViolet", 138, 43, 226),
      new TraceColor ("NavajoWhite", 255, 222, 173),
      new TraceColor ("Gray", 125, 125, 125),
      new TraceColor ("DarkOliveGreen", 85, 107, 47),
      new TraceColor ("IndianRed", 205, 92, 92),
      new TraceColor ("PeachPuff", 255, 218, 185),
   };

   // R    8    7
   // G    6    6
   // B    7    7
   // V         4
   // OY        5

   // new colors
   private static TraceColor[] myPaletteColors = new TraceColor[] {
      new TraceColor ("Red", 255, 0, 0), // 0 R
      new TraceColor ("Green", 0, 200, 0), // 1 G 
      new TraceColor ("Blue", 0, 0, 255), // 2 B 
      new TraceColor ("Cyan", 0, 236, 236), // 3 B  
      new TraceColor ("Magenta", 255, 0, 255), // 4 V
      new TraceColor ("Gold1", 230, 160, 0), // 5 OY
      new TraceColor ("Teal", 0, 158, 115), // 6 G
      new TraceColor ("Ocean", 0, 114, 178), // 7 B
      new TraceColor ("DarkViolet", 148, 0, 211), // 8 V
      new TraceColor ("DeepPink", 255, 20, 145), // 9 R

      new TraceColor ("Orange", 255, 127, 0), // 10 OY
      new TraceColor ("DarkOliveGreen4", 110, 139, 61), // 11 G
      new TraceColor ("WebBlue", 0, 128, 255), // 12 B
      new TraceColor ("MediumPurple1", 171, 130, 255), // 13 V
      new TraceColor ("WebMaroon", 0.5f, 0f, 0f), // 14 R
      new TraceColor ("Gold3", 205, 173, 0), // 15 OY
      new TraceColor ("Chartreuse3", 102, 205, 0), // 16 G
      new TraceColor ("SteelBlue", 0.27f, 0.51f, 0.71f), // 17 B
      new TraceColor ("IndianRed", 205, 92, 92), // 18 R
      new TraceColor ("Gold2", 1f, 0.84f, 0f), // 19 OY

      new TraceColor ("SpringGreen", 0f, 1f, 0.5f), // 20 G
      new TraceColor ("PowderBlue", 86, 180, 233), // 21 B
      new TraceColor ("Pink", 255, 150, 150), // 22 R
      new TraceColor ("Orange3", 205, 133, 0), // 23 OY
      new TraceColor ("DarkGreen", 0, 100, 0), // 24 G
      new TraceColor ("NavyBlue", 0, 0, 127), // 25 B
      new TraceColor ("Violet", 128, 0, 128), // 26 V
      new TraceColor ("PurpleRed", 200, 0, 64), // 27 OY
      new TraceColor ("Tomato", 1f, 0.39f, 0.28f), // 28 R
      new TraceColor ("SandyBrown", 245, 163, 97), // 29 R
   };

   // Sample colors taken from Our World in Data
   private static TraceColor[] myOWIDPaletteColors = new TraceColor[] {
      new TraceColor ("Canada", 193, 80, 101),
      new TraceColor ("Germany", 44, 132, 101),
      new TraceColor ("Italy", 109, 62, 145),
      new TraceColor ("India", 190, 89, 21),
      new TraceColor ("UK", 207, 10, 102),
      new TraceColor ("US", 24, 71, 15),
      new TraceColor ("Afghanistan", 40, 107, 187),
      new TraceColor ("Africa", 136, 48, 57),
      new TraceColor ("Albania", 153, 109, 57),
      new TraceColor ("Algeria", 0, 41, 91),
      new TraceColor ("AmericanSamoa", 154, 81, 41),
      new TraceColor ("Andorra", 196, 82, 62),
      new TraceColor ("Angola", 162, 85, 156),
      new TraceColor ("Anguilla", 0, 130, 145),
      new TraceColor ("Antingua", 87, 129, 69),
      new TraceColor ("Argentina", 151, 0, 70),
      new TraceColor ("Armenia", 0, 132, 126),
      new TraceColor ("Aruba", 177, 53, 7),
      new TraceColor ("Asia", 192, 64, 0),
      new TraceColor ("Australia", 0, 135, 94),
   };

   private static LinkedHashMap<String,TraceColor> myPaletteMap;
   
   static {
      myPaletteMap = new LinkedHashMap<String,TraceColor>();
      for (TraceColor c : myPaletteColors) {
         myPaletteMap.put (c.getName(), c);
      }
   }

   public static int numPalatteColors() {
      return myPaletteColors.length;
   }
   
   public static TraceColor[] getPaletteColors() {
      return myPaletteColors;
   }

   public static Color getPaletteColor (String name) {
      return myPaletteMap.get (name);
   }

   public PlotTraceInfo () {
      myOrder = -1;
      myLabel = null;
      myVisible = true;
      myColor = null;
   }

   public PlotTraceInfo (int order, String label, Color color, boolean visible) {
      myOrder = order;
      myLabel = label;
      myColor = color;
      myVisible = visible;
   }
   
   public PlotTraceInfo (PlotTraceInfo info) {
      myOrder = info.myOrder;
      myLabel = info.myLabel;
      myColor = info.myColor;
      myVisible = info.myVisible;
   }

   public int getOrder() {
      return myOrder;
   }

   public void setOrder(int order) {
      myOrder = order;
   }

   public String getLabel () {
      return myLabel;
   }

   public void setLabel(String label) {
      myLabel = label;
   }

   public Color getColor () {
      return myColor;
   }

   public void setColor(Color color) {
      myColor = color;
   }

   public boolean isVisible () {
      return myVisible;
   }

   public void setVisible (boolean visible) {
      myVisible = visible;
   }

   public boolean scanItem (ReaderTokenizer rtok)
      throws IOException {
         
      rtok.nextToken();
      if (rtok.ttype == ReaderTokenizer.TT_WORD) {
         if (rtok.sval.equals ("order")) {
            rtok.scanToken ('=');
            myOrder = rtok.scanInteger();
         }
         else if (rtok.sval.equals ("label")) {
            rtok.scanToken ('=');
            myLabel = rtok.scanQuotedString('"');
         }
         else if (rtok.sval.equals ("visible")) {
            rtok.scanToken ('=');
            myVisible = rtok.scanBoolean();
         }
         else if (rtok.sval.equals ("color")) {
            rtok.scanToken ('=');
            if (rtok.nextToken() == ReaderTokenizer.TT_WORD) {
               Color c = getPaletteColor (rtok.sval);
               if (c == null) {
                  throw new IOException ("Unrecognized color " + rtok.sval);
               }
               myColor = c;
            }
            else {
               rtok.pushBack();
               myColor = Scan.scanColor (rtok);
            }
         }
         else {
            throw new IOException (
               "Unrecognized keyword '"+rtok.sval+"'");
         }
         return true;
      }
      else {
         rtok.pushBack();
         return false;
      }
   }

   public void scan (ReaderTokenizer rtok)
      throws IOException {
      rtok.scanToken ('[');
      while (scanItem (rtok)) {
      }
      rtok.scanToken (']');
   }

   public void write (PrintWriter pw) {
      pw.print ("[ ");
      pw.print ("order=" + myOrder + " ");
      pw.print ("label=\"" + myLabel + "\" ");
      pw.print ("visible=" + myVisible + " ");
      pw.print ("color=");
      if (myColor instanceof TraceColor) {
         pw.print (((TraceColor)myColor).getName());
      }
      else {
         Write.writeColor (pw, myColor, /*newline=*/false);
      }
      pw.println (" ]");
   }

   static float[] RGBtoHSL (float[] rgb) {
      float min = 2;
      float max = -1;
      int maxi = -1;
      for (int i=0; i<3; i++) {
         if (rgb[i] > max) {
            max = rgb[i];
            maxi = i;
         }
         if (rgb[i] < min) {
            min = rgb[i];
         }
      }
      float h, s, l;
      l = (max+min)/2;
      if (max == min) {
         s = h = 0;
      }
      else {
         if (l <= 0.5f) {
            s = (max-min)/(max+min);
         }
         else {
            s = (max-min)/(2-max-min);
         }
         switch (maxi) {
            case 0: { // red is max
               h = (rgb[1]-rgb[2])/(max-min);
               break;
            }
            case 1: { // blue is max
               h = 2 + (rgb[2]-rgb[0])/(max-min);
               break;
            }
            case 2:
            default: { // green is max
               h = 4 + (rgb[0]-rgb[1])/(max-min);
               break;
            }
         }
         if (h < 0) {
            h += 6;
         }
         h /= 6;
      }
      return new float[] { h, s, l };
   }

   static float[] HSLtoRGB (float[] hsl) {
      float r, g, b;

      float h = hsl[0];
      float s = hsl[1];
      float l = hsl[2];

      float c = (1-Math.abs(2*l-1))*s;
      float h6 = h*6;
      float mod2 = h6/2 - (int)(h6/2);
      System.out.println ("mod2=" + mod2);
      float x = c*(1-Math.abs(mod2 -1));
      float r1, g1, b1;
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
      return new float[] { r+m, g+m, b+m };
   }

   public static void printHSB (TraceColor[] colors) {
      for (TraceColor c : colors) {
         float[] rgb = c.getRGBColorComponents(null);
         float[] hsl = HSL.RGBtoHSL (null, rgb);
         double lum = 0.2126*rgb[0] + 0.7152*rgb[1] + 0.0722*rgb[2];
         System.out.printf (
            "%16s %6.4f %6.4f %6.4f l=%6.4f\n",
            c.getName(), hsl[0], hsl[1], hsl[2], lum);
      }
   }

   private static TraceColor[] randomPalette (int n) {
      TraceColor[] colors = new TraceColor[n];
      for (int i=0; i<n; i++) {
         float h = i/(float)n;
         double lum = 0;
         float[] rgb;
         do {
            float l = (float)RandomGenerator.nextDouble (0.2, .7);
            float[] hsl = new float[] { h, 1f, l};
            rgb = HSL.HSLtoRGB (null, hsl);
            lum = 0.2126*rgb[0] + 0.7152*rgb[1] + 0.0722*rgb[2];            
            if (lum > 0.73) {
               System.out.println ("rejected h=" + h);
            }
            float[] chk = RGBtoHSL (rgb);
            for (int k=0; k<3; k++) {
               if (Math.abs(chk[k]-hsl[k]) > 1e-6) {
                  System.out.printf ("bad conv, i=%d k=%d er=%g\n",
                                     i, k, Math.abs(chk[k]-hsl[k]));
                  System.out.printf ("  rgb=%g %g %g\n", rgb[0], rgb[1], rgb[2]);
                  System.out.printf ("  hsl=%g %g %g\n", hsl[0], hsl[1], hsl[2]);
                  System.out.printf ("  chk=%g %g %g\n", chk[0], chk[1], chk[2]);
                  break;
               }
            }
            

         }
         while (lum > 0.73);
         colors[i] =
            new TraceColor ("color"+i, rgb[0], rgb[1], rgb[2]);
      }
      return colors;
   }

   public static void main (String[] args) {

      RandomGenerator.setSeed (0x1234);

      TraceColor[] palette = myPaletteColors;

      palette = randomPalette (36);

      PropertyPanel panel = new PropertyPanel();
      for (TraceColor c : palette) {
         panel.addWidget (new ColorSelector (c.getName(), c));
      }
      PropertyFrame frame =
         new PropertyFrame ("Plot Trace Palatte", null, panel);    

      System.out.println ("Palatte:");
      printHSB (palette);
      // System.out.println ("OWID:");
      // printHSB (myOWIDPaletteColors);
      frame.pack();
      frame.setVisible (true);
   }

}
