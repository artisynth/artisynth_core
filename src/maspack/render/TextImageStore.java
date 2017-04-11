package maspack.render;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import maspack.util.BinaryTreeRectanglePacker;
import maspack.util.Rectangle;
import maspack.util.RectanglePacker;

/**
 * Stores text characters in an image, potentially to be used by texture maps
 * for drawing text.
 * 
 * @author Antonio
 *
 */
public class TextImageStore {

   // RGBA color model
   private static final ComponentColorModel RGBA_COLOR =
   new ComponentColorModel (
      ColorSpace.getInstance (ColorSpace.CS_sRGB), new int[] { 8, 8, 8, 8 },
      true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
   static final int GLYPH_BORDER = 2; // # pixels around the border of each
   // character
   static final boolean DEFAULT_ANTIALIASING = true;
   static final int DEFAULT_FONT_SIZE = 32;
   static final Font DEFAULT_FONT =
   new Font (Font.SERIF, Font.PLAIN, DEFAULT_FONT_SIZE);
   static final Color DEFAULT_COLOR = Color.WHITE;
   public static boolean DEBUG = false;
   boolean antialiased = false;

   JFrame debugFrame;

   private static class GlyphId {
      final Font font;
      final int glyphCode;

      public GlyphId (Font font, int code) {
         this.font = font;
         this.glyphCode = code;
      }

      @Override
      public int hashCode () {
         return glyphCode * 31 + font.hashCode ();
      }

      @Override
      public boolean equals (Object obj) {
         if (this == obj) {
            return true;
         }
         if (obj == null || getClass () != obj.getClass ()) {
            return false;
         }

         GlyphId other = (GlyphId)obj;
         if (glyphCode != other.glyphCode) {
            return false;
         }

         if (font == null && other.font != null) {
            return false;
         }

         return font.equals (other.font);
      }

      public Font getFont () {
         return font;
      }

      public int getGlyphCode () {
         return glyphCode;
      }
   }

   public static class GlyphLoc {
      final Rectangle rect;
      final double baselinex;
      final double baseliney;

      public GlyphLoc (Rectangle tile, double baselineX, double baselineY) {
         this.rect = tile;
         this.baselinex = baselineX;
         this.baseliney = baselineY;
      }

      public int getX () {
         return rect.x ();
      }

      public int getWidth () {
         return rect.width ();
      }

      public int getY () {
         return rect.y ();
      }

      public int getHeight () {
         return rect.height ();
      }

      public double getBaselineOffsetX () {
         return baselinex;
      }

      public double getBaselineOffsetY () {
         return baseliney;
      }
   }

   /**
    * A Glyph, with an associated GlyphVector from which it came. The underlying
    * GlyphVector can be used to keep track of individual strings. The Glyph
    * itself is broken out so that characters can be renderer non-sequentially.
    *
    */
   public static class Glyph {
      GlyphId id;
      int gidx;
      GlyphVector vec;

      private Glyph (GlyphId id, GlyphVector vec, int gidx) {
         this.id = id;
         this.vec = vec;
         this.gidx = gidx;
      }

      /**
       * @return glyph font
       */
      public Font getFont () {
         return id.getFont ();
      }

      /**
       * @return glyph code point
       */
      public int getGlyphCode () {
         return id.getGlyphCode ();
      }

      /**
       * Location of the glyph's baseline w.r.t. the underlying glyph vector
       * 
       * @return 2D location
       */
      public Point2D getLocation () {
         return vec.getGlyphPosition (gidx);
      }

      /**
       * Metrics of the glyph within the glyph vector
       * 
       * @return metrics
       */
      public GlyphMetrics getMetrics () {
         return vec.getGlyphMetrics (gidx);
      }

      /**
       * Vector the Glyph is associated with
       * 
       * @return vector
       */
      public GlyphVector getGlyphVector () {
         return vec;
      }

      /**
       * @return index of this glyph within the underlying glyph vector.
       */
      public int getGlyphIndex () {
         return gidx;
      }

      private GlyphId getId () {
         return id;
      }
   }

   /**
    * Glyph with storage location in backed image
    * 
    * @author Antonio
    */
   public static class GlyphStore {
      Glyph glyph;
      GlyphLoc loc;
      int storageId;

      private GlyphStore (Glyph glyph, int storageId, GlyphLoc loc) {
         this.glyph = glyph;
         this.loc = loc;
         this.storageId = storageId;
      }

      /**
       * An ID number that corresponds to the storage version number in the
       * backing image. As long as the backer's version is equal to this ID,
       * this store is valid. Otherwise, it may need to be re-uploaded.
       * 
       */
      public int getStorageId () {
         return storageId;
      }

      /**
       * Left coordinate in image
       * 
       */
      public int getLeft () {
         return loc.getX ();
      }

      /**
       * Width of storage
       * 
       */
      public int getWidth () {
         return loc.getWidth ();
      }

      /**
       * Bottom coordinate in image, assuming bottom-left origin
       * 
       */
      public int getBottom () {
         return loc.getY ();
      }

      public int getHeight () {
         return loc.getHeight ();
      }

      /**
       * Offset of baseline from bottom-left
       * 
       * @return x offset
       */
      public double getBaselineX () {
         return loc.getBaselineOffsetX ();
      }

      /**
       * Offset of baseline from bottom-left
       * 
       * @return y offset
       */
      public double getBaselineY () {
         return loc.getBaselineOffsetY ();
      }

      /**
       * Location of character baseline relative to the baseline of the first
       * character
       * 
       * @return location
       */
      public Point2D getBaselineLocation () {
         return glyph.getLocation ();
      }

      /**
       * Distance to advance baseline after printing character
       * 
       * @return x distance
       */
      public double getAdvanceX () {
         return glyph.getMetrics ().getAdvanceX ();
      }

      /**
       * Distance to advance baseline after printing character
       * 
       * @return y distance
       */
      public double getAdvanceY () {
         return glyph.getMetrics ().getAdvanceY ();
      }

      /**
       * The Glyph itself
       * 
       * @return glyph
       */
      public Glyph getGlyph () {
         return glyph;
      }
   }

   /**
    * Adjust bounds by accounting for rounding of pixels, and adding a border.
    * Also reverses the Y direction so that the bottom-left is the origin
    * 
    * @param rect
    * original bounds
    * @param border
    * border pixels
    * @return adjusted bounds
    */
   private static Rectangle2D adjustBounds (Rectangle2D rect, int border) {

      double x = rect.getMinX () - border;
      double y = -rect.getMaxY () - border;
      double w = Math.ceil (rect.getWidth ()) + 2 * border;
      double h = Math.ceil (rect.getHeight ()) + 2 * border;

      return new Rectangle2D.Double (x, y, w, h);
   }

   // map needs: grid location (tex coordinate), baseline origin for character
   HashMap<GlyphId,GlyphLoc> glyphMap;
   private int storageId; // instance
   private int storageVersion; // for detecting any changes
   private boolean storageModified; // flags a change
   private RectanglePacker packer; // packs rectangles

   BufferedImage image; // backing storage
   Rectangle dirty; // region of image that has been modified since last "clean"
   Graphics2D graphics; // for drawing graphics

   Font defaultFont;

   public TextImageStore (RectanglePacker packer, boolean antialiasing) {

      this.packer = packer;

      // sRGBA color model
      WritableRaster raster =
      Raster.createInterleavedRaster (
         DataBuffer.TYPE_BYTE, packer.getWidth (), packer.getHeight (), 4,
         null);
      image = new BufferedImage (RGBA_COLOR, raster, false, null);
      dirty = null;

      graphics = image.createGraphics ();
      graphics.setColor (DEFAULT_COLOR);
      graphics.setBackground (new Color (120, 120, 120, 0)); // clear
      graphics.clearRect (0, 0, image.getWidth (), image.getHeight ());
      graphics.setRenderingHint (
         RenderingHints.KEY_FRACTIONALMETRICS,
         RenderingHints.VALUE_FRACTIONALMETRICS_ON);

      this.antialiased = false;
      setAntialiasing (antialiasing);
      // graphics.setComposite (AlphaComposite.Src);

      glyphMap = new HashMap<> ();

      storageId = 0;
      storageVersion = 0;

      defaultFont = DEFAULT_FONT;

      if (DEBUG) {
         debugFrame = createDisplayFrame (this);
         debugFrame.setVisible (true);
      }

   }

   public void setFont (Font font) {
      defaultFont = font;
   }

   public Font getFont () {
      return defaultFont;
   }

   public FontMetrics getFontMetrics (Font font) {
      return graphics.getFontMetrics (font);
   }

   public FontMetrics getFontMetrics () {
      return graphics.getFontMetrics ();
   }

   /**
    * Enable or disable antialiasing in the graphics pipeline
    */
   public void setAntialiasing (boolean set) {

      if (set != antialiased) {
         clear();
         antialiased = set;
         if (set) {
            graphics.setRenderingHint (
               RenderingHints.KEY_TEXT_ANTIALIASING,
               RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         }
         else {
            graphics.setRenderingHint (
               RenderingHints.KEY_TEXT_ANTIALIASING,
               RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
         }
      }

   }

   /**
    * Creates an array of Glyphs, each containing identifying information, as
    * well as location information, so they can be processed glyph by glyph. If
    * the input array is not sufficiently large to accommodate all glyphs, a new
    * array is constructed.
    * 
    * @param str
    * input string
    * @param out
    * array to populate, or null to create a new array
    * @return populated glyph array
    */
   public Glyph[] createGlyphs (String str, Glyph[] out) {
      GlyphVector glyphvec =
      defaultFont.createGlyphVector (graphics.getFontRenderContext (), str);
      return createGlyphs (glyphvec, out);
   }

   /**
    * Creates a glyph vector using the current font and image context
    *
    * @return vector
    */
   public GlyphVector createGlyphVector (String str) {
      return createGlyphVector (defaultFont, str);
   }

   /**
    * Creates a glyph vector using the current font and image context
    *
    * @return vector
    */
   public GlyphVector createGlyphVector (Font font, String str) {
      GlyphVector glyphvec =
      font.createGlyphVector (graphics.getFontRenderContext (), str);
      return glyphvec;
   }

   /**
    * Creates an array of Glyphs, each containing identifying information, as
    * well as location information, so they can be processed glyph by glyph. If
    * the input array is not sufficiently large to accommodate all glyphs, a new
    * array is constructed.
    * 
    * @param glyphvec
    * glyph vector
    * @param out
    * array to populate, or null to create a new array
    * @return populated glyph array
    */
   public Glyph[] createGlyphs (GlyphVector glyphvec, Glyph[] out) {

      if (out == null || out.length < glyphvec.getNumGlyphs ()) {
         out = new Glyph[glyphvec.getNumGlyphs ()];
      }
      for (int i = 0; i < glyphvec.getNumGlyphs (); ++i) {
         int code = glyphvec.getGlyphCode (i);
         out[i] =
         (new Glyph (new GlyphId (glyphvec.getFont (), code), glyphvec, i));
      }

      return out;
   }

   /**
    * Checks to see if the glyph is already in the backing image, returning the
    * storage information if present, or null otherwise.
    * 
    * @return storage info
    */
   public GlyphStore get (Glyph glyph) {
      GlyphLoc loc = glyphMap.get (glyph.getId ());
      // see if we need to add it to the map
      if (loc != null) {
         return new GlyphStore (glyph, storageId, loc);
      }
      return null;
   }

   /**
    * Attempts to upload glyphs for an entire string.
    *
    * @return true if successful, false if some glyphs failed
    */
   public boolean upload (String str) {
      boolean success = true;
      Glyph[] glyphs = createGlyphs (str, null);
      for (Glyph g : glyphs) {
         GlyphStore store = upload (g);
         if (store == null) {
            success = false;
         }
      }
      return success;
   }

   public Graphics2D getGraphics () {
      return graphics;
   }

   /**
    * Attempts to upload glyphs for an entire string.
    * 
    * @return true if successful, false if some glyphs failed
    */
   public boolean upload (GlyphVector vec) {
      boolean success = true;
      Glyph[] glyphs = createGlyphs (vec, null);
      for (Glyph g : glyphs) {
         GlyphStore store = upload (g);
         if (store == null) {
            success = false;
         }
      }
      return success;
   }

   /**
    * Mark a region of the backing image as "dirty" (i.e. modified)
    */
   public void markDirty (Rectangle rect) {
      if (dirty == null) {
         dirty = rect;
         return;
      }
      int x = Math.min (dirty.x (), rect.x ());
      int y = Math.min (dirty.y (), rect.y ());
      int w =
      Math.max (
         Math.max (dirty.width (), rect.width ()),
         Math.max (
            rect.x () + rect.width () - dirty.x (),
            dirty.x () + dirty.width () - rect.x ()));
      int h =
      Math.max (
         Math.max (dirty.height (), rect.height ()),
         Math.max (
            rect.y () + rect.height () - dirty.y (),
            dirty.y () + dirty.height () - rect.y ()));

      dirty = new Rectangle (x, y, w, h);
   }

   /**
    * Get bytes from the dirty region in a rasterized way, row-major, starting
    * with bottom-left;
    * 
    * @param out
    * buffer to fill with RGBA bytes
    * @return rectangular region to update
    */
   public Rectangle getDirtyData (ByteBuffer out) {
      Rectangle dirty = getDirty ();
      if (dirty != null) {
         getData (dirty, out);
      }
      return dirty;
   }

   /**
    * Retrieves all bytes for the given region in a rasterized way, row-major,
    * starting with bottom left.
    * 
    * @param region
    * pixel region from which to obtain bytes
    * @param out
    * buffer to fill with RGBA bytes
    */
   public void getData (Rectangle region, ByteBuffer out) {

      WritableRaster raster = image.getRaster ();
      DataBufferByte dataBuffer = (DataBufferByte)(raster.getDataBuffer ());
      byte[] data = dataBuffer.getData ();

      int pixelWidth = 4;
      int imageHeight = image.getHeight ();
      int imagePixelWidth = image.getWidth () * pixelWidth;
      int rowWidth = region.width () * pixelWidth;

      int pos =
      (imageHeight - region.y () - 1) * imagePixelWidth
      + region.x () * pixelWidth;
      for (int i = 0; i < region.height (); ++i) {
         out.put (data, pos, rowWidth);
         pos -= imagePixelWidth; // advance up a row
      }
   }

   /**
    * Retrieves image data for the entire backing array, rasterized row-major
    * with pixels starting at bottom-left of image
    * 
    * @param out
    * buffer to fill with content
    */
   public void getData (ByteBuffer out) {
      getData (
         new Rectangle (0, 0, image.getWidth (), image.getHeight ()), out);
   }

   /**
    * Size of a pixel in bytes
    * 
    * @return pixel size
    */
   public int getPixelSize () {
      int bitsize =
      DataBuffer.getDataTypeSize (
         image.getRaster ().getDataBuffer ().getDataType ());
      return bitsize / 8;
   }

   /**
    * Type of image
    * 
    * @see java.awt.image.BufferedImage#getType()
    */
   public int getImageType () {
      return image.getType ();
   }

   /**
    * Width of image
    * 
    * @return width
    */
   public int getWidth () {
      return image.getWidth ();
   }

   /**
    * Height of image
    * 
    * @return height
    */
   public int getHeight () {
      return image.getHeight ();
   }

   /**
    * Get the region marked as dirty
    * 
    */
   public Rectangle getDirty () {
      return dirty;
   }

   /**
    * Mark the dirty region as now cleaned
    */
   public void markClean () {
      dirty = null;
   }

   /**
    * Attempts to store a glyph into the backing image store, returning the
    * storage information. If the glyph is already stored, then the existing
    * backing store is returned. Otherwise, the {@link RectanglePacker} will
    * attempt to place the glyph in the backing store.
    * 
    * @param glyph
    * glyph to try to upload to the backing store (if not already present)
    * @return the resulting storage information, or null if it doesn't fit
    */
   public GlyphStore upload (Glyph glyph) {

      GlyphStore store = get (glyph);
      if (store != null) {
         return store;
      }

      // new single glyph vector
      Font glyphFont = glyph.getFont ();
      GlyphVector vec =
      glyphFont.createGlyphVector (
         graphics.getFontRenderContext (),
         new int[] { glyph.getGlyphCode () });

      // determine required glyph size
      GlyphMetrics metrics = vec.getGlyphMetrics (0);
      Rectangle2D rect = adjustBounds (metrics.getBounds2D (), GLYPH_BORDER);

      // pack glyph into backing store
      Rectangle packed =
      packer.pack ((int)rect.getWidth (), (int)rect.getHeight ());
      if (packed == null) {
         return null; // didn't fit
      }

      // determine layout in image
      int left = packed.x ();
      int top = image.getHeight () - packed.y () - packed.height ();
      int bottom = image.getHeight () - packed.y () - 1;
      int width = packed.width ();
      int height = packed.height ();

      // draw to image
      // Clear out the area we're going to draw into
      // graphics.clearRect (left, top, width, height);
      graphics.drawGlyphVector (
         vec, (float)(left - rect.getMinX ()),
         (float)(bottom + rect.getMinY ()));
      markDirty (packed);
      storageModified = true;

      // debugging, draw border
      if (DEBUG) {
         Color pen = graphics.getColor ();
         graphics.setColor (Color.CYAN);
         graphics.drawRect (left, top, width - 1, height - 1);
         graphics.setColor (pen);
      }

      // store and return glyph location in image
      GlyphLoc loc = new GlyphLoc (packed, -rect.getMinX (), -rect.getMinY ());
      glyphMap.put (glyph.getId (), loc);
      store = new GlyphStore (glyph, storageId, loc);

      if (DEBUG) {
         debugFrame.repaint ();
      }

      return store;
   }

   /**
    * Save backing image to a file (convenience method)
    */
   public void saveImage (String type, File dest) {
      try {
         ImageIO.write (image, type, dest);
      }
      catch (IOException e) {
         e.printStackTrace ();
      }
   }

   /**
    * The backing image
    * 
    * @return image
    */
   public BufferedImage getImage () {
      return image;
   }

   /**
    * Clears the backing image, increments the storage Id so that any existing
    * external storage glyphs become invalid
    */
   public void clear () {
      // clear everything away
      graphics.clearRect (0, 0, image.getWidth (), image.getHeight ());
      packer.clear ();
      storageModified = true;
      markClean ();
      ++storageId;
   }

   /**
    * ID for identifying valid glyph stores. As long as the glyph store's
    * Storage ID matches this number, the store is valid.
    * 
    * @return storage id number
    */
   public int getStorageId () {
      return storageId;
   }

   /**
    * Checks whether glyph storage is still valid
    *
    * @return true if still valid
    */
   public boolean isValid (GlyphStore storage) {
      if (storageId == storage.getStorageId ()) {
         return true;
      }
      return false;
   }

   /**
    * Version number for checking if backing image has been updated
    * 
    */
   public int getStorageVersion () {
      if (storageModified) {
         ++storageVersion;
         storageModified = false;
      }
      return storageVersion;
   }

   /**
    * Simple class to help debug storage
    */
   private static class ImagePanel extends JPanel {
      private static final long serialVersionUID = 1L;
      private BufferedImage image;

      public ImagePanel (BufferedImage image) {
         this.image = image;
         setOpaque (false);
         setLayout (null);
      }

      @Override
      protected void paintComponent (Graphics g) {
         super.paintComponent (g);
         g.drawImage (image, 0, 0, null); // see javadoc for more info on the
         // parameters
      }
   }

   public static JFrame createDisplayFrame (TextImageStore store) {

      JFrame frame = new JFrame ("Glyph Packing");
      // frame.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
      // frame.setUndecorated (true);
      frame.setPreferredSize (
         new Dimension (store.getWidth () + 30, store.getHeight () + 70));
      frame.setBackground (new Color (0, 0, 128, 255));
      frame.getContentPane ().setBackground (new Color (255, 255, 255, 0));

      ImagePanel panel;
      panel = new ImagePanel (store.getImage ());
      panel.setVisible (true);

      frame.getContentPane ().add (panel);
      frame.pack ();
      frame.setVisible (true);

      return frame;
   }

   static public void main (String args[]) throws Exception {

      int width = 512;
      int height = 512;

      TextImageStore.DEBUG = true;
      TextImageStore content =
      new TextImageStore (
         new BinaryTreeRectanglePacker (width, height), true);



      content.debugFrame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);

      content.setFont (new Font(Font.MONOSPACED, Font.PLAIN, 32));
      content.upload (
         "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec in lorem suscipit ante lobortis lacinia et a mauris. Mauris at vulputate diam. Quisque eu mattis orci, a molestie nisl. Quisque sed tempor est. Aliquam mattis, enim eu dignissim finibus, sapien purus tempus ante, vel hendrerit ipsum nisl eget mauris. Fusce eu vestibulum ipsum. Cras ac interdum lectus, non ultrices sem. Ut velit mauris, porta id leo at, ornare fermentum sem. Cras ac maximus ex, in sodales ante. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Nulla ac elit ut arcu vulputate congue. Ut lectus ipsum, cursus id nibh sed, consectetur dignissim magna. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Donec quis nibh sed felis posuere semper vel sed ante.");

      content.upload ("The quick brown fox jumps over the lazy dog".toLowerCase ());
      content.upload ("The quick brown fox jumps over the lazy dog".toUpperCase ());

      Font font = new Font (Font.SERIF, Font.BOLD, 64);
      content.setFont (font);
      content.upload (
      "Praesent semper consequat rhoncus. Cras quis massa mauris. Proin maximus iaculis blandit. Quisque sed massa mattis nulla laoreet cursus. Nulla iaculis auctor urna at faucibus. Nulla gravida nulla at mauris gravida, sit amet pellentesque ante aliquam. Ut pulvinar urna vel congue ullamcorper. Cras ac libero a ipsum molestie auctor. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Nullam non feugiat libero, ut ultrices diam. Morbi sed efficitur lacus. Fusce semper arcu ac varius lacinia. Mauris non mauris facilisis, imperdiet est vitae, luctus urna. Nam dapibus sit amet nibh in lobortis. Vivamus gravida commodo magna, id fringilla quam congue sed. Nullam ut turpis elit.");
      content.upload ("The quick brown fox jumps over the lazy dog".toLowerCase ());
      content.upload ("The quick brown fox jumps over the lazy dog".toUpperCase ());

      // // loop through all glyph characters
      // int sc = 0;
      // for (int i=0; i<font.getNumGlyphs (); ++i) {
      // GlyphVector gv = font.createGlyphVector (content.getGraphics
      // ().getFontRenderContext (), new int[]{i});
      // boolean success = content.upload (gv);
      // if (success) {
      // ++sc;
      // }
      // panel.repaint();
      // }
      // System.out.println (sc + " glyphs successfully uploaded");

      content.saveImage ("png", new File ("tmp/glyphs0.png"));
   }
}
