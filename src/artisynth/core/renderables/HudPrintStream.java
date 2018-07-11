/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import maspack.matrix.Point2d;
import maspack.properties.PropertyList;
import maspack.render.FaceRenderProps;
import maspack.render.IsRenderable;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;

/**
 * Simple component to print info on the main viewer
 */
public class HudPrintStream extends TextComponentBase {

   public static String defaultFontName = Font.MONOSPACED;
   public static int defaultNumDisplayLines = 1;
   public static double defaultLineSpacing = 1.2;
   public static int defaultBufferSize = 64;

   Point2d myPos;
   String[] myTextLines;
   int myMaxDisplayLines;
   double myLineSpacing;

   Point2d renderPos = new Point2d();
   int myLineIdx;
   int myNumLines;
   int myBufferSize;
   int myScrollOffset;

   boolean isFullScreen;
   int lastMaxDisplayLines;
   Point2d lastLocation;
   VerticalAlignment lastVAlignment;

   public static PropertyList myProps = new PropertyList(
      HudPrintStream.class, TextComponentBase.class);

   static {
      myProps.add("position", "display position", Point2d.ZERO);
      myProps.add("lineSpacing", "height of line proportional to font size",
         defaultLineSpacing);
      myProps.add(
         "numDisplayLines", "number of lines to display",
         defaultNumDisplayLines);
      myProps.add("bufferSize", "size of the buffer", defaultBufferSize);
      myProps.add("scrollOffset", "offset for scrolling", 0);
      myProps.add("fullscreen isFullscreen *", "set full screen mode", false);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Basic component, defaulting to 5 lines of 12pt monospaced font in bottom
    * left corner
    */
   public HudPrintStream () {
      setDefaults();
   }

   @Override
   protected void setDefaults() {
      myFont = new Font(defaultFontName, Font.PLAIN, defaultFontSize);
      myRenderProps = createDefaultRenderProps();
      hAlignment = defaultHAlignment;
      vAlignment = defaultVAlignment;
      myTextSize = defaultTextSize;

      myPos = new Point2d();
      myMaxDisplayLines = defaultNumDisplayLines;
      myLineSpacing = defaultLineSpacing;
      myBufferSize = defaultBufferSize;
      myTextLines = new String[myBufferSize];

      myLineIdx = 0;
      myScrollOffset = 0;
      clear();

   }

//   @Override
//   public void renderx(GLRenderer renderer, int flags) {
//      if (isSelectable() || !renderer.isSelecting()) {
//         render(renderer, 0);
//      }
//   }
   
   @Override
   public void render(Renderer renderer, int flags) {

      if (!isSelectable() && renderer.isSelecting()) {
         return;
      }

      FaceRenderProps rprops = (FaceRenderProps)getRenderProps();

      // Position is assumed to be ([0,1], [0,1])
      int sw = renderer.getScreenWidth();
      int sh = renderer.getScreenHeight();
      renderPos.set(myPos.x * sw, myPos.y * sh);

      // print from top to bottom
      // for consistency, assume line top as 0.75 font size
      Rectangle2D box = renderer.getTextBounds (myFont, "X", myTextSize);
      double t = box.getHeight ()+box.getY ();
      // double vc = box.getY ()+box.getHeight ()/2;
      double b = box.getY ();
      double a;
      
      double dy = myTextSize * myLineSpacing;

      int nLines;
      if (isFullScreen) {
         nLines = (int)Math.floor(((double)sh - myTextSize) / dy + 1);
         nLines = Math.min(myNumLines, nLines);

      } else {
         nLines = Math.min(myNumLines, myMaxDisplayLines);
      }

      switch (vAlignment) {
         case TOP:
            renderPos.add(0, -t);
            break;
         case CENTRE:
            a = -t + (myTextSize + dy * (nLines - 1)) / 2;
            renderPos.add(0, a);
            break;
         case BOTTOM:
            a = -b + (dy * (nLines - 1));
            renderPos.add(0, a);
            break;
      }

      if (isFullScreen) {
         // we don't want the last text line to go below the bottom of the
         // screen
         a = renderPos.y - (nLines - 1) * dy + b;
         if (a < 0) {
            renderPos.y -= a;
         }
      }

//      boolean saved2d = renderer.is2DRendering(); 
//      if (!saved2d) {
//         renderer.begin2DRendering(sw, sh);
//      }
      
      // render lines top to bottom
      float[] rgb = new float[3];
      rprops.getFaceColor(rgb);
      renderer.setColor(rgb[0], rgb[1], rgb[2], (float)rprops.getAlpha());

      myScrollOffset = Math.min(myScrollOffset, myNumLines - nLines);
      int offset = myScrollOffset;
      // if last line is just a newline, then offset by one
      if ("".equals(myTextLines[myLineIdx])
         && (myScrollOffset < myNumLines - nLines)) {
         offset = Math.min(offset + 1, myNumLines - nLines);
      }
      
      Shading oldShading = renderer.getShading ();
      renderer.setShading (Shading.NONE);
      
      int j = 0;
      String str;
      float[] loc = new float[3];
      for (int i = 0; i < nLines; i++) {
         // text left computation
         j =
            (myLineIdx + i + 2 * myBufferSize - nLines - offset + 1)
               % myBufferSize;
         str = myTextLines[j];
         box = renderer.getTextBounds(myFont, str, myTextSize);
         double w = box.getWidth();
         // double h = box.getHeight();
         double dx = 0;
         switch (hAlignment) {
            case CENTRE:
               dx = -w / 2;
               break;
            case LEFT:
               dx = 0;
               break;
            case RIGHT:
               dx = -w;
               break;
         }

         loc[0] = (float)(renderPos.x + dx);
         loc[1] = (float)(renderPos.y - i * dy);
         renderer.drawText(myFont, str, loc, myTextSize);

      }
      
      renderer.setShading (oldShading);
      
//      if (!saved2d) {
//         renderer.end2DRendering();
//      }
      
   }

   /**
    * Appends blank lines to clear the screen
    */
   public void cls() {
      String str = "";
      for (int i=0; i<myMaxDisplayLines+2; i++) {
         str = str + "\n";
      }
      print(str);
   }

   /**
    * Clears the buffer
    */
   public void clear() {
      myLineIdx = 0;
      myNumLines = 1;
      myScrollOffset = 0;
      myTextLines[0] = "";
   }

   /**
    * Standard printf functionality
    */
   public void printf(String fmt, Object... args) {
      String str = String.format(fmt, args);
      print(str);
   }

   /**
    * Standard print functionality
    */
   public void print(String str) {

      str = processControlCharacters(myTextLines[myLineIdx] + str);

      // separate lines
      String[] lines = str.split("\r?\n",-1);

      int n = Math.min(lines.length, myBufferSize);
      fillLines(lines, lines.length - n, n);

      myLineIdx = (myLineIdx + n - 1) % myBufferSize;
      myNumLines = Math.min(myNumLines + n - 1, myBufferSize);

      //      // add trailing newline if exists
      //      if (str.endsWith("\n")) {
      //         myLineIdx = (myLineIdx + 1) % myBufferSize;
      //         myNumLines = Math.min(myNumLines + 1, myBufferSize);
      //         myTextLines[myLineIdx] = "";
      //      }
   }

   /**
    * Standard println functionality
    */
   public void println(String str) {
      print(str + "\n");
   }

   private void fillLines(String[] lines, int idx, int len) {
      int j;
      for (int i = 0; i < len; i++) {
         j = (i + myLineIdx) % myBufferSize;
         myTextLines[j] = lines[idx + i];
      }
   }

   /**
    * Maximum number of lines to display
    */
   public int getNumDisplayLines() {
      return myMaxDisplayLines;
   }

   /**
    * Sets maximum number of lines to display
    */
   public void setNumDisplayLines(int max) {

      if (!isFullScreen) {
         if (max != myMaxDisplayLines) {
            if (max > myBufferSize) {
               setBufferSize(max);
            }
            myMaxDisplayLines = max;
         }
      } else {
         lastMaxDisplayLines = max;
         if (max > myBufferSize) {
            setBufferSize(max);
         }
      }

   }

   /**
    * Sets maximum number of lines to store
    */
   public void setBufferSize(int buffSize) {

      myNumLines = Math.min(myNumLines, buffSize);
      myMaxDisplayLines = Math.min(myMaxDisplayLines, myNumLines);

      String[] newArray = new String[buffSize];
      int j = 0;
      for (int i = 0; i < myNumLines; i++) {
         j = (i + myLineIdx + myBufferSize - myNumLines + 1) % myBufferSize;
         newArray[i] = myTextLines[j];
      }
      myLineIdx = myNumLines - 1;
      myTextLines = newArray;
      myBufferSize = buffSize;

   }

   /**
    * Returns maximum size of line buffer
    */
   public int getBufferSize() {
      return myBufferSize;
   }

   /**
    * Sets the line spacing, 1 for single-space
    */
   public void setLineSpacing(double sp) {
      myLineSpacing = sp;
   }

   /**
    * Gets the line spacing
    */
   public double getLineSpacing() {
      return myLineSpacing;
   }

   /**
    * Gets current 2D normalized coordinates, [0 1]x[0 1]
    */
   public Point2d getPosition() {
      return myPos;
   }

   /**
    * Sets the 2D normalized coordinates, [0 1]x[0 1]
    */
   public void setPosition(Point2d pos) {
      if (!isFullScreen) {
         myPos.set(pos);
      } else {
         lastLocation.set(pos);
      }
   }

   @Override
   public int getRenderHints() {
      return (IsRenderable.TRANSPARENT | IsRenderable.TWO_DIMENSIONAL);
   }

   // handles *SOME* control characters
   // Carriage return not quite correct, but good enough to overwrite
   // a single line
   private String processControlCharacters(String input) {

      char[] chars = input.toCharArray();
      ArrayList<Character> out = new ArrayList<Character>(chars.length);

      int lastn = -1;
      int idx = 0;
      int add = 0;
      for (int i = 0; i < chars.length; i++) {
         switch (chars[i]) {
            case '\b':
               if (idx > 0) {
                  idx--;
               }
               break;
            case '\r':
               idx = lastn + 1;
               break;
            case '\n':
               lastn = idx;
               addOrSet(idx++, out, '\n');
               break;
            case '\t':
               add = 8 - ((idx - lastn + 7) % 8);
               for (int j = 0; j < add; j++) {
                  addOrSet(idx++, out, ' ');
               }
               break;
            default:
               addOrSet(idx++, out, chars[i]);
         }
      }

      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < idx; i++) {
         builder.append(out.get(i));
      }

      return builder.toString();
   }

   private void addOrSet(int pos, ArrayList<Character> list, Character val) {
      if (pos == list.size()) {
         list.add(val);
      } else if (pos < list.size()) {
         list.set(pos, val);
      }
   }

   /**
    * Moves the HUD to the top-left corner
    */
   public void locateTopLeft() {
      myPos.set(0.0, 1.0);
      hAlignment = HorizontalAlignment.LEFT;
      vAlignment = VerticalAlignment.TOP;
   }

   /**
    * Moves the HUD to the top-centre
    */
   public void locateTopCentre() {
      myPos.set(0.5, 1.0);
      hAlignment = HorizontalAlignment.CENTRE;
      vAlignment = VerticalAlignment.TOP;
   }

   /**
    * Moves the HUD to the top-right
    */
   public void locateTopRight() {
      myPos.set(1.0, 1.0);
      hAlignment = HorizontalAlignment.RIGHT;
      vAlignment = VerticalAlignment.TOP;
   }

   /**
    * Moves the HUD to the bottom-left corner
    */
   public void locateBottomLeft() {
      myPos.set(0.0, 0.0);
      hAlignment = HorizontalAlignment.LEFT;
      vAlignment = VerticalAlignment.BOTTOM;
   }

   /**
    * Moves the HUD to the bottom-centre
    */
   public void locateBottomCentre() {
      myPos.set(0.5, 0.0);
      hAlignment = HorizontalAlignment.CENTRE;
      vAlignment = VerticalAlignment.BOTTOM;
   }

   /**
    * Moves the HUD to the bottom-right
    */
   public void locateBottomRight() {
      myPos.set(1.0, 0.0);
      hAlignment = HorizontalAlignment.RIGHT;
      vAlignment = VerticalAlignment.BOTTOM;
   }

   /**
    * Makes the HUD full-screen according to the viewer's current height
    */
   public void fullscreen() {

      // save previous values so we can return
      lastLocation = new Point2d(myPos);
      lastVAlignment = vAlignment;

      // move to top
      myPos.set(myPos.x, 1);
      vAlignment = VerticalAlignment.TOP;

      isFullScreen = true;
   }

   public void toggleFullscreen() {

      if (!isFullScreen) {
         fullscreen();
      } else {

         // return to last
         myPos.set(lastLocation);
         vAlignment = lastVAlignment;
         isFullScreen = false;
      }

   }

   public boolean isFullscreen() {
      return isFullScreen;
   }

   public void setFullscreen(boolean enable) {
      if (isFullScreen) {
         toggleFullscreen();
      } else {
         fullscreen();
      }
   }

   @Override
   public void setVerticalAlignment(VerticalAlignment vAlignment) {
      if (!isFullScreen) {
         super.setVerticalAlignment (vAlignment);
      } else {
         lastVAlignment = vAlignment;
      }
   }

   /**
    * Set number of lines to scroll up 0 &lt;= offset &lt;= # buffered lines - lines
    * on screen
    */
   public void setScrollOffset(int offset) {
      myScrollOffset = offset;
      myScrollOffset = Math.max(myScrollOffset, 0);
      myScrollOffset = Math.min(myScrollOffset, myNumLines);
   }

   /**
    * Gets the current scroll offset
    */
   public int getScrollOffset() {
      return myScrollOffset;
   }

   /**
    * Scroll up by one line
    */
   public void scrollUp() {
      setScrollOffset(myScrollOffset + 1);
   }

   /**
    * Scroll down by one line
    */
   public void scrollDown() {
      setScrollOffset(myScrollOffset - 1);
   }

}
