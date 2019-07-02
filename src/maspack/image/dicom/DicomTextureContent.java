package maspack.image.dicom;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import maspack.image.dicom.DicomPixelBuffer.PixelType;
import maspack.matrix.Point2d;
import maspack.render.TextureContent;
import maspack.util.BinaryTreeRectanglePacker;
import maspack.util.BufferUtilities;
import maspack.util.Pair;
import maspack.util.Rectangle;
import maspack.util.ReferenceCountedBase;

public class DicomTextureContent extends ReferenceCountedBase implements TextureContent {

   public static final int COL_ROW_PLANE = 0;
   public static final int COL_SLICE_PLANE = 1;
   public static final int ROW_SLICE_PLANE = 2;
   
   DicomImage image;
   DicomWindowPixelInterpolator window;
   
   String lastWindow;
   int lastWindowCentre;
   int lastWindowWidth;
   boolean[] valid; // marks region in backing image as invalid
   boolean[] dirty;
   Rectangle[] rects;
   
   ByteBuffer textureImage; // backing image
   int textureWidth;
   int textureHeight;
   PixelType internalStorage;
   
   // slice info
   int col;
   int row;
   int slice;
   int time;
   
   public DicomTextureContent(DicomImage image) {
      this.image = image;
      lastWindow = null;
      lastWindowCentre = -1;
      lastWindowWidth = -1;
      valid = new boolean[3];
      dirty = new boolean[3];
      
      internalStorage = PixelType.UBYTE;
      switch(image.getPixelType ()) {
         case BYTE:
         case UBYTE:
            internalStorage = PixelType.UBYTE;
            break;
         case SHORT:
         case USHORT:
            internalStorage = PixelType.USHORT;
            break;
         case UBYTE_RGB:
            internalStorage = PixelType.UBYTE_RGB;
            break;
         default:
            break;
         
      }
      
      invalidateData ();
      
      // Full dynamic range
      window = new DicomWindowPixelInterpolator();
      
      double maxIntensity = image.getMaxIntensity();
      double minIntensity = image.getMinIntensity();
      double c = (maxIntensity+minIntensity)/2;
      double diff = maxIntensity-minIntensity;
      window.addWindowPreset("FULL DYNAMIC", (int)Math.round(c), (int)Math.round(diff));
      window.setWindow("FULL DYNAMIC");
      
      // add other windows, loaded from first slice
      DicomHeader header = image.getSlice(0).getHeader();
      double[] windowCenters = header.getMultiDecimalValue(DicomTag.WINDOW_CENTER);
      if (windowCenters != null) {
         int numWindows = windowCenters.length;
         double[] windowWidths = header.getMultiDecimalValue(DicomTag.WINDOW_WIDTH);
         String[] windowNames = header.getMultiStringValue(DicomTag.WINDOW_CENTER_AND_WIDTH_EXPLANATION);
         
         for (int i=0; i<numWindows; i++) {
            String wname;
            if (windowNames != null) {
               wname = windowNames[i];
            } else {
               wname = "WINDOW" + i;
            }
            window.addWindowPreset(wname, (int)windowCenters[i], (int)windowWidths[i]);
         }
      }
      
      int ncols = image.getNumCols ();
      int nrows = image.getNumRows ();
      int nslices = image.getNumSlices ();
      
      col = ncols/2;
      row = nrows/2;
      slice = nslices/2;
      time = 0;
      
      rects = packRects(nrows, ncols, nslices);
      updateBounds(rects);
      textureImage = BufferUtilities.newNativeByteBuffer (getPixelSize()*textureWidth*textureHeight);
   }
   
   public DicomWindowPixelInterpolator getWindowConverter() {
      return window;
   }
   
   /**
    * column to use for the row-slice plane
    * @param c column index
    */
   public void setColumn(int c) {
      if (col != c && c >= 0 && c < image.getNumCols ()) {
         col = c;
         invalidateData(ROW_SLICE_PLANE);
      }
   }
   
   /**
    * @return column for the row-slice plane
    */
   public int getColumn() {
      return col;
   }
   
   /**
    * Row to use for the column-slice plane
    * @param r row index
    */
   public void setRow(int r) {
      if (row != r && r >= 0 && r < image.getNumRows ()) {
         row = r;
         invalidateData(COL_SLICE_PLANE);
      }
   }
   
   /**
    * @return column used on row-slice plane
    */
   public int getRow() {
      return row;
   }
   
   /**
    * Slice to use on col-row plane
    * @param s slice index
    */
   public void setSlice(int s) {
      if (slice != s && s >= 0 && s < image.getNumSlices ()) {
         slice = s;
         invalidateData(COL_ROW_PLANE);
      }
   }
   
   /**
    * @return slice to use on the col-row plane
    */
   public int getSlice() {
      return slice;
   }
   
   /**
    * For 4D DICOM images, sets the time index for all slices
    * @param t time index
    */
   public void setTime(int t) {
      if (time != t && t >= 0 && t < image.getNumTimes ()) {
         time = t;
         invalidateData ();
      }
   }
   
   /**
    * @return time index for slices
    */
   public int getTime() {
      return time;
   }
   
   /**
    * Normalized row-slice plane position within volume
    * @param x normalized column [0,1]
    */
   public void setX(double x) {
      int ncols = image.getNumCols();
      if (ncols < 2) {
         setColumn(0);
      } else {
         setColumn ((int)Math.min(Math.floor(x*ncols), ncols-1));
      }
   }
   
   /**
    * Normalized row-slice plane position within volume
    * @return normalized column [0,1]
    */
   public double getX() {
      int ncols = image.getNumCols();
      if (ncols < 2) {
         return 0;
      }
      return (getColumn ()+0.5)/ncols;
   }
   
   /**
    * Normalized col-slice plane position within volume
    * @param y normalized row [0,1]
    */
   public void setY(double y) {
      int nrows = image.getNumRows();
      if (nrows < 2) {
         setRow(0);
      } else {
         setRow ((int)Math.min(Math.floor(y*nrows), nrows-1));
      }
   }
   
   /**
    * Normalized col-slice plane position within volume
    * @return normalized row [0,1]
    */
   public double getY() {
      int nrows = image.getNumRows();
      if (nrows < 2) {
         return 0;
      }
      return (getRow () + 0.5)/nrows;
   }
   
   /**
    * Normalized col-row plane position within volume
    * @param z normalized slice [0,1]
    */
   public void setZ(double z) {
      int nslices = image.getNumSlices();
      if (nslices < 2) {
         setSlice(0);       
      } else {
         setSlice ((int)Math.min(Math.floor(z*nslices), nslices-1));
      }
   }
   
   /**
    * Normalized col-row plane position within volume
    * @return normalized slice [0,1]
    */
   public double getZ() {
      int nslices = image.getNumSlices();
      if (nslices < 2) {
         return 0;
      }
      return (getSlice () + 0.5)/nslices;
   }
   
   /**
    * Normalized time to use
    * @param t normalized time [0,1]
    */
   public void setT(double t) {
      setTime ((int)Math.round (t*(image.getNumTimes ()-1)));
   }
   
   /**
    * Normalized time to use
    * @return normalized time [0,1]
    */
   public double getT() {
      return (double)getTime ()/(image.getNumTimes ()-1);
   }
   
   /**
    * Uploads dicom pixels to backing buffer
    * @param plane plane index
    */
   protected void updateBackingBuffer(int plane) {
      
      Rectangle rect = rects[plane];
      int psize = getPixelSize ();
      int scanline = psize*textureWidth;
      
      synchronized (textureImage) {
         textureImage.limit (textureImage.capacity ());
         textureImage.position (rect.y ()*scanline+psize*rect.x ());
         
         switch (plane) {
            case COL_ROW_PLANE:
               image.getPixels (0, 0, slice, 1, 1, 1, rect.width (), rect.height (),
                  1, time, internalStorage, scanline, 0, window, textureImage);
               break;
            case COL_SLICE_PLANE:
               image.getPixels (0, row, 0, 1, 1, 1, rect.width (), 1,
                  rect.height (), time, internalStorage, 0, scanline, window, textureImage);
               break;
            case ROW_SLICE_PLANE:
               image.getPixels (col, 0, 0, 1, 1, 1, 1, rect.width (), rect.height (),
                  time, internalStorage, 0, scanline, window, textureImage);
               break;
         }
         valid[plane] = true;   
      }

   }
   
   /**
    * Get the closest greater power of 2 to the fold number
    * 
    * @param fold
    * The target number
    * @return The power of 2
    */
   private int get2Fold (int fold) {
      int out = 2;
      while (out < fold) {
         out *= 2;
      }
      return out;
   }
   
   protected void updateBounds(Rectangle[] rects) {
      textureWidth = 0;
      textureHeight = 0;
      for (Rectangle r : rects) {
         int x = r.x()+r.width ();
         if (x > textureWidth) {
            textureWidth = x;
         }
         int y = r.y ()+r.height ();
         if (y > textureHeight) {
            textureHeight = y;
         }
      }
      
      textureWidth = get2Fold (textureWidth);
      textureHeight = get2Fold (textureHeight);
   }
   
   
   protected Rectangle[] packRects(int nrows, int ncols, int nslices) {
      
      // pack three textures into one
      // rows x cols, rows x slices, cols x slices
      List<Pair<Integer,Rectangle>> planes = new ArrayList<Pair<Integer,Rectangle>>(3);
      planes.add(new Pair<Integer,Rectangle> (COL_ROW_PLANE, new Rectangle(0,0,ncols,nrows)));
      planes.add(new Pair<Integer,Rectangle> (COL_SLICE_PLANE, new Rectangle(0,0,ncols,nslices)));
      planes.add(new Pair<Integer,Rectangle> (ROW_SLICE_PLANE, new Rectangle(0,0,nrows,nslices)));
      
      // sort by area descending
      Collections.sort (planes, new Comparator<Pair<Integer,Rectangle>>() {
         @Override
         public int compare (
            Pair<Integer,Rectangle> o1, Pair<Integer,Rectangle> o2) {
            int a1 = o1.second ().area ();
            int a2 = o2.second ().area ();
            
            if (a1 > a2) {
               return -1;
            } else if (a1 < a2) {
               return 1;
            }
            return 0;
         }
      });
      
      int totalwidth = 2*ncols + nrows;
      int totalheight = nrows + 2*nslices;
      int maxdim = Math.max (totalwidth, totalheight);
      
      Rectangle[] out = new Rectangle[3];
      
      // tightly pack into rectangle
      BinaryTreeRectanglePacker packer = new BinaryTreeRectanglePacker (maxdim, maxdim);
      for (int i=0; i<3; ++i) {
         out[planes.get (i).first] = packer.pack (planes.get (i).second);
      }
      
      return out;
   }
   
   protected void invalidateData(int plane) {
      valid[plane] = false;
      dirty[plane] = true;
   }
   
   protected void invalidateData() {
      for (int i=0; i<valid.length; ++i) {
         invalidateData(i);
      }
   }
   
   @Override
   public int getWidth () {
      return textureWidth;
   }

   @Override
   public int getHeight () {
      return textureHeight;
   }
   
   /**
    * Texture coordinates for a given plane ({@link #COL_ROW_PLANE}, 
    * {@link #ROW_SLICE_PLANE}, or {@link #COL_SLICE_PLANE}), starting with the
    * bottom-left corner and working around clockwise.
    * @param plane plane index to compute coordinates of
    * @return texture coordinates
    */
   public Point2d[] getTextureCoordinates(int plane) {
      Point2d[] out = new Point2d[4];
      
      Rectangle rect = rects[plane];
      int w = textureWidth-1;
      int h = textureHeight-1;
      
      double tx1 = (double)rect.x ()/w;
      double tx2 = ((double)rect.x ()+rect.width ()-1)/w;
      double ty1 = (double)rect.y ()/h;
      double ty2 = ((double)rect.y ()+rect.height ()-1)/h;
      out[0] = new Point2d(tx1, ty1);
      out[1] = new Point2d(tx1, ty2);
      out[2] = new Point2d(tx2, ty2);
      out[3] = new Point2d(tx2, ty1);
      
      return out;
   }

   @Override
   public int getPixelSize () {
      switch (internalStorage) {
         case BYTE:
         case UBYTE:
            return 1;
         case UBYTE_RGB:
            return 3;
         case SHORT:
         case USHORT:
            return 2;
         default:
            break;
      }
      return 0;
   }
   
   /**
    * Prepares texture content for rendering
    */
   public void prerender() {
      maybeUpdateImage ();
   }
   
   /**
    * Updates backing image buffer based on any changed info
    * @return true if backing image has been updated
    */
   protected boolean maybeUpdateImage() {

      // check if we need to mark as dirty
      if (lastWindow != window.getWindow ()) {
         invalidateData ();
      } else if (lastWindowCentre != window.getWindowCenter ()) {
         invalidateData ();
      } else if (lastWindowWidth != window.getWindowWidth ()) {
         invalidateData ();
      }
      
      boolean updated = false;
      for (int i=0; i<valid.length; ++i) {
         if (!valid[i]) {
            updateBackingBuffer (i);
            updated = true;
         }
      }
      
      if (updated) {
         lastWindow = window.getWindow ();
         lastWindowCentre = window.getWindowCenter ();
         lastWindowWidth = window.getWindowWidth ();
      }
      
      return updated;
   }

   @Override
   public void getData (ByteBuffer out) {
      maybeUpdateImage ();
      // showTexture ();
      
      synchronized (textureImage) {
         textureImage.rewind();
         textureImage.limit (textureImage.capacity ());
         out.put (textureImage);
         textureImage.rewind ();
         textureImage.limit(textureImage.capacity ());  
      }
   }

   @Override
   public void getData (Rectangle rect, ByteBuffer out) {
      maybeUpdateImage ();
      
      // showTexture ();
      
      int pixelWidth = getPixelSize ();
      int scanline = textureWidth * pixelWidth;
      int rowWidth = rect.width () * pixelWidth;

      int pos = rect.y () * scanline + rect.x () * pixelWidth;
      
      synchronized (textureImage) {
         for (int i = 0; i < rect.height (); ++i) {
            textureImage.limit (pos+rowWidth);
            textureImage.position (pos);
            out.put (textureImage);
            pos += scanline; // advance up a row
         }
         // reset position/limit
         textureImage.rewind ();
         textureImage.limit(textureImage.capacity ());
      }
   }
   
   /**
    * Simple class to help debug storage
    */
   public static class ImagePanel extends JPanel {
      private static final long serialVersionUID = 1L;
      private BufferedImage image;

      public ImagePanel (BufferedImage image) {
         this.image = image;
         setOpaque (false);
         setLayout (null);
      }
      
      public void setImage(BufferedImage im) {
         image = im;
         repaint ();
      }

      @Override
      protected void paintComponent (Graphics g) {
         super.paintComponent (g);
         g.drawImage (image, 0, 0, null); // see javadoc for more info on the
                                          // parameters
      }
   }
   
   static JFrame textureFrame = null;
   static ImagePanel panel = null;
   
   public void showTexture() {
      BufferedImage image = createImage();
      
      if (textureFrame == null) {
         textureFrame = new JFrame ("DicomTextureContent texture");
         panel = new ImagePanel (image);
         textureFrame.getContentPane().setBackground (Color.BLACK);
         textureFrame.getContentPane ().add (panel);
         textureFrame.setSize (image.getWidth ()+30, image.getHeight ()+70);
         textureFrame.setVisible (true);
      } else {
         panel.setImage (image);
         textureFrame.setSize (image.getWidth ()+30, image.getHeight ()+70);
         if (!textureFrame.isVisible ()) {
            textureFrame.setVisible (true);
         }
      }

      textureFrame.repaint ();
   }
   
   public BufferedImage createImage() {

      ComponentColorModel colorModel = null;
      colorModel = new ComponentColorModel (ColorSpace.getInstance (ColorSpace.CS_sRGB),
         new int[] {8, 8, 8, 8}, true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
      
      switch (internalStorage) {
         case UBYTE:
            colorModel = new ComponentColorModel (ColorSpace.getInstance (ColorSpace.CS_GRAY),
               new int[] {8}, false, false, ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
            break;
         case UBYTE_RGB:
            colorModel = new ComponentColorModel (ColorSpace.getInstance (ColorSpace.CS_sRGB),
               new int[] {8, 8, 8, 0}, false, false, ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
            break;
         case USHORT:
            colorModel = new ComponentColorModel (ColorSpace.getInstance (ColorSpace.CS_GRAY),
               new int[] {16}, false, false, ComponentColorModel.OPAQUE, DataBuffer.TYPE_USHORT);
            break;
         default:
         
      }
      
      // sRGBA color model
      WritableRaster raster = Raster.createInterleavedRaster (
         colorModel.getTransferType (), textureWidth, textureHeight, colorModel.getNumComponents (), null);
      BufferedImage image = new BufferedImage (colorModel, raster, false, null);
      
      synchronized(textureImage) {
         textureImage.position (0);
         textureImage.limit (textureImage.capacity ());
         
         // flip vertically
         int scanline = textureWidth;
         int pos = textureWidth*textureHeight-scanline;
         for (int i=0; i<textureHeight; ++i) {
            for (int j=0; j<textureWidth; ++j) {
               switch(internalStorage) {
                  case UBYTE:
                     raster.getDataBuffer ().setElem (pos+j, textureImage.get ());
                     break;
                  case UBYTE_RGB:
                     raster.getDataBuffer ().setElem (0, pos+j, textureImage.get ());
                     raster.getDataBuffer ().setElem (1, pos+j, textureImage.get ());
                     raster.getDataBuffer ().setElem (2, pos+j, textureImage.get ());
                     break;
                  case USHORT:
                     raster.getDataBuffer ().setElem (pos+j, textureImage.getShort ());
                     break;
                  default:
                  
               }
            }
            pos -= scanline;
         }
         textureImage.rewind ();
      }
      
      return image;
      
      
   }

   @Override
   public boolean isDirty () {
      maybeUpdateImage ();
      
      for (int i=0; i<dirty.length; ++i) {
         if (dirty[i]) {
            return true;
         }
      }
      
      return false;
   }

   @Override
   public Rectangle getDirty () {
      maybeUpdateImage ();
      
      int left = textureWidth; // past the end
      int bottom = textureHeight;
      int right = -1;
      int top = -1;
      
      for (int i=0; i<3; ++i) {
         if (dirty[i]) {
            left = Math.min (left, rects[i].x ());
            right = Math.max (right, rects[i].x ()+rects[i].width ()-1);
            bottom = Math.min (bottom, rects[i].y ());
            top = Math.max (top, rects[i].y ()+rects[i].height ()-1);
         }
      }
      
      if (left == textureWidth || bottom == textureHeight) {
         return null;
      }
      
      return new Rectangle(left,bottom,right-left+1,top-bottom+1);
   }

   @Override
   public void markClean () {
      // mark all planes valid
      for (int i=0; i<dirty.length; ++i) {
         dirty[i] = false;
      }
   }

   @Override
   public ContentFormat getFormat () {
      switch (internalStorage) {
         case BYTE:
            return ContentFormat.GRAYSCALE_BYTE;
         case UBYTE:
            return ContentFormat.GRAYSCALE_UBYTE;
         case UBYTE_RGB:
            return ContentFormat.RGB_BYTE_3;
         case SHORT:
            return ContentFormat.GRAYSCALE_SHORT;
         case USHORT:
            return ContentFormat.GRAYSCALE_USHORT;
      }
      return null;
   }

   @Override
   public Object getKey () {
      return image;
   }

   @Override
   public DicomTextureContent acquire () {
      return (DicomTextureContent)super.acquire ();
   }
   
   public void dispose() {
      textureImage = BufferUtilities.freeDirectBuffer (textureImage);
   }
   
   @Override
   protected void finalize () throws Throwable {
      super.finalize ();
      dispose ();
   }
 

}
