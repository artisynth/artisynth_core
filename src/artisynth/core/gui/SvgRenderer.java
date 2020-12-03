package artisynth.core.gui;

import java.awt.Color;
import java.io.*;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.interpolation.*;
import artisynth.core.util.*;
import artisynth.core.probes.PlotTraceManager;
import artisynth.core.probes.PlotTraceInfo;
import artisynth.core.gui.NumericProbePanel.ScreenTransform;
import artisynth.core.gui.NumericProbePanel.TickValue;
import artisynth.core.gui.DataRenderer.TextAlign;

public class SvgRenderer implements DataRenderer {

   PrintWriter myWriter;
   int myClipId = 0;
   String myClipPath = null;

   public SvgRenderer (File file) throws IOException {
      myWriter =
         new PrintWriter (
            new BufferedWriter (new FileWriter (file)));
   }

   public void beginPlot (float width, float height, boolean antialiasing) {
      PrintWriter pw = myWriter;
      pw.println ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
      pw.println ("<svg width=\""+width+"\" height=\""+height+"\"");
      pw.println (" viewBox=\"0 0 "+width+" "+height+"\"");
      pw.println ("  xmlns=\"http://www.w3.org/2000/svg\"");
      pw.println ("  xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
      pw.println ("  version=\"1.2\" baseProfile=\"tiny\">");
      pw.println ("<rect width=\"100%\" height=\"100%\" fill=\"white\" />");
      pw.println ("<title>ArtiSynth Svg Document</title>");
      pw.println ("<desc>Generated by ArtiSynth</desc>");
      pw.println ("<defs>");
      pw.println ("</defs>");
      // clear the region
      beginSolidGraphics (Color.WHITE);
      drawRect(0, 0, width, height);
      endGraphics();     
   }

   public void endPlot() {
      myWriter.println ("</svg>");
      myWriter.close();
   }

   public void beginLineGraphics (float width, Color color) {
      PrintWriter pw = myWriter;
      pw.printf ("<g fill=\"none\" stroke=\"%s\"", getColorStr(color));
      pw.printf (" stroke-width=\"%.2f\"", width);
      if (myClipPath != null) {
         pw.printf (" clip-path=\"url(#%s)\"", myClipPath);
      }
      pw.printf (" >\n");
   }

   public void beginSolidGraphics (Color color) {
      PrintWriter pw = myWriter;
      pw.printf (
         "<g stroke=\"none\" fill=\"%s\"", getColorStr(color));
      if (myClipPath != null) {
         pw.printf (" clip-path=\"url(#%s)\"", myClipPath);
      }
      pw.printf (" >\n");
   }

   public void drawCircle (float x, float y, float size) {
      myWriter.printf (
         "<circle cx=\"%.2f\" cy=\"%.2f\" r=\"%.2f\" />\n",
         x+0.5f, y+0.5f, size/2f);
   }

   public void beginTextGraphics (float fontSize, Color color) {
      PrintWriter pw = myWriter;
      pw.printf ("<g font-size=\"%.2f\" font-family=\"Cantarell\"", fontSize);
      pw.printf (" fill=\"%s\"", getColorStr(color));
      if (myClipPath != null) {
         pw.printf (" clip-path=\"url(#%s)\"", myClipPath);
      }
      pw.printf (" >\n");
   }

   public void drawLine (float x1, float y1, float x2, float y2) {
      myWriter.printf (
         "<line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" />\n",
         x1+0.5f, y1+0.5f, x2+0.5f, y2+0.5f);
   }

   public void drawRect (float x, float y, float w, float h) {
      myWriter.printf (
         "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" />\n",
         x+0.5f, y+0.5f, w, h);
   }

   public void drawText (String text, float x, float y, TextAlign halign) {
      String hanchor;
      switch (halign) {
         case RIGHT: {
            hanchor = "end";
            x -= 1;
            break;
         }
         case LEFT: {
            hanchor = "start";
            break;
         }
         case CENTER: {
            hanchor = "middle";
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "No implementation for halign type "+halign);
         }
      }
      myWriter.printf (
         "<text text-anchor=\""+hanchor+
         "\" x=\"%.2f\" y=\"%.2f\">" + text + "</text>\n", x+0.5f, y+0.5f);
   }


   public boolean allowsFloatPolylines() {
      return true;
   }

   public void drawPolyline (int[] xvals, int[] yvals, int cnt) {
      throw new UnsupportedOperationException();
   }

   public void drawPolyline (float[] xvals, float[] yvals, int cnt) {
      myWriter.printf ("<polyline points=\"");
      for (int i=0; i<cnt; i++) {
         myWriter.printf ("%.2f,%.2f ", xvals[i]+0.5f, yvals[i]+0.5f);
      }
      myWriter.printf ("\" />\n");
   }

   public void endGraphics() {
      myWriter.println ("</g>");
   }

   String getColorStr (Color color) {
      NumberFormat fmt = new NumberFormat ("%02x");
      StringBuilder sbuild = new StringBuilder();
      sbuild.append ('#');
      sbuild.append (fmt.format (color.getRed()));
      sbuild.append (fmt.format (color.getGreen()));
      sbuild.append (fmt.format (color.getBlue()));
      return sbuild.toString();      
   }

   public void beginClipping (float x, float y, float w, float h) {
      PrintWriter pw = myWriter;
      // define the clip Id
      myClipPath = "clip" + myClipId++;
      pw.printf ("<clipPath id=\"%s\" >", myClipPath);
      pw.printf (
         "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" />\n",
         x, y, w, h);      
      pw.printf ("</clipPath>");
   }
   
   public void endClipping () {
      myClipPath = null;
   }

}