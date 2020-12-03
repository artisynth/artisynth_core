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

public class PostScriptRenderer implements DataRenderer {

   PrintWriter myWriter;
   boolean mySolid = false;
   float myHeight;
   float myWidth;
   // scale maps pixels to points, for a typical screen resolution of 96 dpi
   float myScale = 0.75f;

   /**
    * Converts pixel y coordinates to PostScript (flipping things so that
    * y=0 is at the bottom)
    */
   private float getY (float y) {
      return myHeight-myScale*(1+y+0.5f);
   }

   /**
    * Converts pixel x coordinates to PostScript
    */
   private float getX (float x) {
      return myScale*(x+0.5f);
   }

   public PostScriptRenderer (File file) throws IOException {
      myWriter =
         new PrintWriter (
            new BufferedWriter (new FileWriter (file)));
   }

   public void beginPlot (float width, float height, boolean antialiasing) {
      PrintWriter pw = myWriter;

      myHeight = myScale*height;
      myWidth = myScale*width;

      pw.printf ("%%!PS-Adobe-2.0\n");
      pw.printf ("%%%%Creator: ArtiSynth\n");
      pw.printf ("%%%%BoundingBox: 0 0 %.2f %.2f\n", myWidth, myHeight);
      pw.printf ("1 setlinewidth\n");
      pw.printf ("1 setlinecap\n");
      pw.printf ("1 setlinejoin\n");
      // /* set up clipping region */
      // pw.printf ("newpath\n");
      // pw.printf ("%9.4f %9.4f moveto\n", psClipXmin, psClipYmin);
      // pw.printf ("%9.4f %9.4f lineto\n", psClipXmax, psClipYmin);
      // pw.printf ("%9.4f %9.4f lineto\n", psClipXmax, psClipYmax);
      // pw.printf ("%9.4f %9.4f lineto\n", psClipXmin, psClipYmax);
      // pw.printf ("closepath clip\n");
      pw.printf ("%% showright shows right-aligned text\n");
      pw.printf ("/showright {\n");
      pw.printf ("  dup stringwidth pop\n");
      pw.printf ("  neg 0 rmoveto\n");
      pw.printf ("  show\n");
      pw.printf ("} def\n");
      pw.printf ("%% showcenter shows center-aligned text\n");
      pw.printf ("/showcenter {\n");
      pw.printf ("  dup stringwidth pop\n");
      pw.printf ("  -0.5 mul 0 rmoveto\n");
      pw.printf ("  show\n");
      pw.printf ("} def\n");
   }

   public void endPlot() {
      myWriter.printf ("showpage\n");
      myWriter.close();
   }

   private void setColor (Color color) {
      float[] rgb = color.getRGBColorComponents (null);
      myWriter.printf ("%f %f %f setrgbcolor\n", rgb[0], rgb[1], rgb[2]);
   }

   public void beginLineGraphics (float width, Color color) {
      myWriter.printf ("%.2f setlinewidth\n", myScale*width);
      setColor (color);
      mySolid = false;
   }

   public void beginSolidGraphics (Color color) {
      setColor (color);
      mySolid = true;
   }

   public void drawCircle (float x, float y, float size) {
      PrintWriter pw = myWriter;
      pw.printf (
         "%.2f %.2f %.2f 0 360 arc closepath\n",
         getX(x), getY(y), myScale*size/2f);
      if (mySolid) {
         pw.printf ("fill\n");
      }
      else {
         pw.printf ("stroke\n");
      }
   }

   public void beginTextGraphics (float fontSize, Color color) {
      setColor (color);
      myWriter.printf ("/Helvetica findfont\n");
      myWriter.printf ("%.2f scalefont\n", myScale*fontSize);
      myWriter.printf ("setfont\n");
   }

   public void drawLine (float x1, float y1, float x2, float y2) {
      PrintWriter pw = myWriter;
      pw.printf ("newpath\n");
      pw.printf ("%.2f %.2f moveto\n", getX(x1), getY(y1));
      pw.printf ("%.2f %.2f lineto\n", getX(x2), getY(y2));
      pw.printf ("stroke\n");
   }

   public void drawRect (float x, float y, float w, float h) {
      PrintWriter pw = myWriter;
      pw.printf ("newpath\n");
      pw.printf ("%.2f %.2f moveto\n", getX(x), getY(y));
      pw.printf ("0 %.2f rlineto\n", -myScale*h);
      pw.printf ("%.2f 0 rlineto\n", myScale*w);
      pw.printf ("0 %.2f rlineto\n", myScale*h);
      pw.printf ("%.2f 0 rlineto\n", -myScale*w);
      pw.printf ("closepath\n");
      if (mySolid) {
         pw.printf ("fill\n");
      }
      else {
         pw.printf ("stroke\n");
      }
   }

   public void drawText (String text, float x, float y, TextAlign halign) {
      // TODO: adjust x for alignment
      PrintWriter pw = myWriter;
      pw.printf ("%.2f %.2f moveto\n", getX(x), getY(y));
      switch (halign) {
         case RIGHT: {
            pw.printf ("(%s) showright\n", text);
            break;
         }
         case LEFT: {
            pw.printf ("(%s) show\n", text);
            break;
         }
         case CENTER: {
            pw.printf ("(%s) showcenter\n", text);
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "No implementation for halign type "+halign);
         }
      }
   }

   public boolean allowsFloatPolylines() {
      return true;
   }

   public void drawPolyline (int[] xvals, int[] yvals, int cnt) {
      throw new UnsupportedOperationException();
   }

   public void drawPolyline (float[] xvals, float[] yvals, int cnt) {
      if (cnt > 1) {
         myWriter.printf ("newpath\n");
         myWriter.printf (
            "%.2f %.2f moveto\n", getX(xvals[0]), getY(yvals[0]));
         for (int i=1; i<cnt; i++) {
            myWriter.printf (
               "%.2f %.2f lineto\n", getX(xvals[i]), getY(yvals[i]));
         }
         myWriter.printf ("stroke\n");
      }
   }

   public void endGraphics() {
      mySolid = false;
   }

   public void beginClipping (float x, float y, float w, float h) {
      PrintWriter pw = myWriter;
      float hp = myScale*0.5f; // remove mid pixel adjustment
      pw.printf ("gsave\n");
      pw.printf ("newpath\n");
      pw.printf ("%.2f %.2f moveto\n", getX(x)-hp, getY(y)+hp);
      pw.printf ("0 %.2f rlineto\n", -myScale*h);
      pw.printf ("%.2f 0 rlineto\n", myScale*w);
      pw.printf ("0 %.2f rlineto\n", myScale*h);
      pw.printf ("%.2f 0 rlineto\n", -myScale*w);
      pw.printf ("closepath\n");
      pw.printf ("clip\n");
   }
   
   public void endClipping () {
      myWriter.printf ("grestore\n");
   }
}
