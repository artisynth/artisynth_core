package artisynth.core.gui;

import java.awt.Color;

/**
 * Very simple interface used to create numeric data plots, either for online
 * graphics or for rendering to a file.
 */
public interface DataRenderer {

   public enum TextAlign {
      LEFT,
      CENTER,
      RIGHT
   };

   public void beginPlot (float width, float height, boolean antialiasing);

   public void endPlot ();

   public void beginLineGraphics (float lineWidth, Color color);

   public void beginSolidGraphics (Color color);

   public void drawCircle (float cx, float cy, float size);

   public void beginTextGraphics (float fontSize, Color color);

   public void drawLine (float x1, float y1, float x2, float y2);

   public void drawRect (float x, float y, float w, float h);

   public void drawText (String text, float x, float y, TextAlign halign);

   public boolean allowsFloatPolylines();

   public void drawPolyline (float[] xvals, float[] yvals, int cnt);

   public void drawPolyline (int[] xvals, int[] yvals, int cnt);

   public void endGraphics ();

   public void beginClipping (float x, float y, float w, float h);

   public void endClipping ();

   
}
