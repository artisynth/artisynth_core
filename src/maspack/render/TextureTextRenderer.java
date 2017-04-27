package maspack.render;

import java.awt.Font;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import maspack.render.TextImageStore.Glyph;
import maspack.render.TextImageStore.GlyphStore;
import maspack.util.BinaryTreeRectanglePacker;
import maspack.util.BufferUtilities;
import maspack.util.Rectangle;

public abstract class TextureTextRenderer implements TextRenderer {

   public static boolean DEFAULT_ANTIALIASING = true;
   public static Font DEFAULT_FONT = new Font (Font.SERIF, Font.PLAIN, 32);
   public static int DEFAULT_SIZE = 512;

   TextImageStore textstore;

   boolean antialiasing;
   int maxDimension;
   Font font;
   boolean textureUploaded;
   
   ArrayList<GlyphQueue> queue;
   
   public TextureTextRenderer (int preferredWidth, int preferredHeight, int maxDimension) {
      this (
         preferredWidth, preferredHeight, maxDimension,
         DEFAULT_ANTIALIASING);
   }

   public TextureTextRenderer (int preferredWidth, int preferredHeight, 
      int maxDimension, boolean antialiasing) {

      this.maxDimension = maxDimension;
      this.antialiasing = antialiasing;
      setPreferredSize (preferredWidth, preferredHeight); // initializes store
      setFont (DEFAULT_FONT);
      textureUploaded = false;
   }
   
   /**
    * @return Image store with packed glyphs
    */
   public TextImageStore getImageStore() {
      return textstore;
   }
   
   /**
    * @return width of the backing image store
    */
   public int getWidth() {
      return textstore.getWidth ();
   }
   
   /**
    * @return height of the backing image store
    */
   public int getHeight() {
      return textstore.getHeight ();
   }
   
   /**
    * Enable or disable antialiasing in the backing image store
    */
   public void setAntialiasing (boolean set) {
      antialiasing = set;
      textstore.setAntialiasing (set);
   }

   /**
    * Whether or not antialiasing is enabled
    */
   public boolean isAntialiasing () {
      return antialiasing;
   }

   /**
    * Default font to use for methods in which the font
    * is not specified
    * @param font default font
    */
   public void setFont (Font font) {
      this.font = font;
      textstore.setFont (font);
   }
   
   /**
    * @return the default font
    */
   public Font getFont() {
      return font;
   }

   protected static class GlyphQueue {
      float[] origin;
      float scale;
      Glyph[] glyphqueue;
      int remaining;

      public GlyphQueue (Glyph[] glyphs, float[] origin, float scale) {
         this.glyphqueue = glyphs;
         this.origin = origin;
         this.scale = scale;
         remaining = glyphs.length;
      }
   }
   
   /**
    * Prepare a queue for optimized rendering of characters
    */
   public void begin() {
      queue = new ArrayList<> ();
   }
   
   /**
    * Renders text with a given font at a provided baseline location.
    * @param font font to use.  Note that the font's size controls the
    * resolution of the glyphs in the backing texture.  The final drawn
    * font size can be controlled using {@code emsize}.  Rendering
    * might not take place immediately.  This class creates a queue
    * to minimize the number of required changes to a backing texture.
    * To force a render, use {@link #flush()}.
    * 
    * @param text text to draw
    * @param loc baseline location to start drawing
    * @param emsize size of an 'em' unit
    * @return the advance distance in the x-direction (width of text)
    */
   @Override
   public float drawText (Font font, String text, float[] loc, float emsize) {
      
      GlyphVector vec = textstore.createGlyphVector (font, text);
      Glyph[] glyphs = textstore.createGlyphs (vec, null);

      // determine origin and scale
      float[] origin = new float[3];
      origin[0] = loc[0];
      origin[1] = loc[1];
      if (loc.length > 2) {
         origin[2] = loc[2];
      }
      // scale distances by this number, font-size independent
      float scale = emsize / font.getSize ();

      queue.add (new GlyphQueue (glyphs, origin, scale));
      
      return (float)(vec.getGlyphPosition (vec.getNumGlyphs ()).getX ()*scale);

   }
   
   /**
    * Renders an actual Glyph based on it's storage info at a particular location
    * and scale
    * @param store glyph storage
    * @param origin baseline location to render glyph
    * @param scale size of the 'em' unit
    */
   protected abstract void render (GlyphStore store, float[] origin, float scale);

   /**
    * Draws a set of glyphs directly, without modifying the underlying text
    * storage texture
    * 
    * @param queue
    * glyphs to draw
    * @return unfinished queues
    */
   protected ArrayList<GlyphQueue> draw (ArrayList<GlyphQueue> queue) {
      ArrayList<GlyphQueue> remaining =
         new ArrayList<GlyphQueue> (queue.size ());

      // first ones we can render without changing the texture
      for (GlyphQueue gq : queue) {
         int s = gq.remaining;
         Glyph[] glyphs = gq.glyphqueue;
         int skipped = 0;
         for (int i = 0; i < s; ++i) {
            GlyphStore store = textstore.get (glyphs[i]);
            if (store == null) {
               glyphs[skipped++] = glyphs[i]; // add back to queue
            }
            else {
               render (store, gq.origin, gq.scale);
            }
         }
         gq.remaining = skipped;

         if (skipped > 0) {
            remaining.add (gq);
         }
      }

      return remaining;
   }
   
   /**
    * Replace the entire contents of a texture with the provided RGBA bytes,
    * starting with the bottom-left pixel.  Buffer is of size 4*width*height.
    * @param width width of texture
    * @param height height of texture
    * @param buff pixel data 4-byte RGBA, starting with bottom-left, scanning
    * horizontally (row-major)
    */
   protected abstract void replaceTexture(int width, int height, ByteBuffer buff);
   
   /**
    * Replace a portion of the contents of a texture with the provided RGBA bytes,
    * starting with the bottom-left pixel.  Buffer is of size 4*width*height.
    * @param x starting horizontal pixel
    * @param y starting vertical pixel (from bottom)
    * @param width width of texture
    * @param height height of texture
    * @param buff pixel data 4-byte RGBA, starting with bottom-left, scanning
    * horizontally (row major)
    */
   protected abstract void replaceSubTexture(int x, int y, int width, int height, ByteBuffer buff);
   
   /**
    * Draw characters to screen now
    */
   public void flush () {

      queue = draw (queue);
      // now try to upload any that were missing
      while (queue.size () > 0) {
         // upload as many as we can before we start drawing
         for (GlyphQueue gq : queue) {
            int s = gq.remaining;
            Glyph[] glyphs = gq.glyphqueue;
            for (int i = 0; i < s; ++i) {
               textstore.upload (glyphs[i]);
            }
         }

         // update texture
         if (textureUploaded) {
            Rectangle dirty = textstore.getDirty ();
            ByteBuffer buff =
               BufferUtilities
                  .newNativeByteBuffer (dirty.area() * 4);
            textstore.getData (dirty, buff);
            buff.flip ();
            
            replaceSubTexture(dirty.x (), dirty.y (), dirty.width (), dirty.height (), buff);
            
            textstore.markClean ();
            buff = BufferUtilities.freeDirectBuffer (buff);
         } else {
            ByteBuffer buff =BufferUtilities.newNativeByteBuffer (textstore.getWidth ()*textstore.getHeight () * 4);
            textstore.getData (buff);
            buff.flip ();
            replaceTexture (textstore.getWidth (), textstore.getHeight (), buff);
            textstore.markClean ();
            buff = BufferUtilities.freeDirectBuffer (buff);
            textureUploaded = true;
         }
         
         // draw
         queue = draw (queue);

         // if any couldn't be uploaded, clear backing (maybe resize) and try
         // again
         if (queue.size () > 0) {
            setPreferredSize (
               textstore.getWidth () * 2, textstore.getHeight () * 2);
            textstore.clear ();
         }
      }

   }
   
   
   public void end() {
      flush();
      queue = null;
   }
   
   @Override
   public Rectangle2D getTextBounds(Font font, String text, float size) {
      
      Rectangle2D bounds = font.getStringBounds (text, textstore.getGraphics ().getFontRenderContext ());
      double scale = size/font.getSize ();
      
      return new Rectangle2D.Double (scale*bounds.getMinX (), -scale*bounds.getMaxY (), 
         scale*bounds.getWidth (), scale*bounds.getHeight ());
      
   }
   
   public void setPreferredSize (int width, int height) {
      width = Math.min (width, maxDimension);
      height = Math.min (height, maxDimension);
      textstore =
         new TextImageStore (
            new BinaryTreeRectanglePacker (width, height), antialiasing);
      textureUploaded = false;
   }

}
