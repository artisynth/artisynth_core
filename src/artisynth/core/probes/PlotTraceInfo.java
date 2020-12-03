/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.awt.Color;
import maspack.util.*;
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

   private static TraceColor[] myPaletteColors = new TraceColor[] {
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

}
