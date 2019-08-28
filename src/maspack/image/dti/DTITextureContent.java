package maspack.image.dti;

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

import maspack.matrix.Point2d;
import maspack.render.TextureContent;
import maspack.util.BinaryTreeRectanglePacker;
import maspack.util.BufferUtilities;
import maspack.util.Pair;
import maspack.util.Rectangle;
import maspack.util.ReferenceCountedBase;

public class DTITextureContent extends ReferenceCountedBase implements TextureContent {

   public static final int COL_ROW_PLANE = 0;
   public static final int COL_SLICE_PLANE = 1;
   public static final int ROW_SLICE_PLANE = 2;
   
   DTIField image;
   DTIColorComputer voxelator;
   
   int lastVersion;
   boolean[] valid; // marks region in backing image as invalid
   boolean[] dirty;
   Rectangle[] rects;
   
   ByteBuffer textureImage; // backing image
   int textureWidth;
   int textureHeight;
   
   // slice info
   int col;
   int row;
   int slice;
   
   public DTITextureContent(DTIField image, DTIColorComputer voxelator) {
      this.image = image;
      lastVersion = -1;
      valid = new boolean[3];
      dirty = new boolean[3];
      
      // Full dynamic range
      this.voxelator = voxelator; 
      
      int ncols = image.getNumCols ();
      int nrows = image.getNumRows ();
      int nslices = image.getNumSlices ();
      
      col = ncols/2;
      row = nrows/2;
      slice = nslices/2;
      
      rects = packRects(nrows, ncols, nslices);
      updateBounds(rects);
      
      invalidateData ();
      textureImage = BufferUtilities.newNativeByteBuffer (getPixelSize()*textureWidth*textureHeight);
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
   
   private void getPixels( 
      int i, int di, int ni, 
      int j, int dj, int nj, 
      int k, int dk, int nk,
      int scanline, int pageline, ByteBuffer buff) {
      
      for (int kk=0; kk<nk; ++kk) {
         int kpos = buff.position();
         for (int jj=0; jj<nj; ++jj) {
            int jpos = buff.position();
            for (int ii=0; ii<ni; ++ii) {
               voxelator.get(image.getVoxel(i+ii*di, j+jj*dj, k+kk*dk), buff);
            }
            if (scanline > 0) {
               buff.position(jpos + scanline);
            }
         }
         if (pageline > 0) {
            buff.position(kpos + pageline);
         }
      }
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
               getPixels (0, 1, rect.width(), 0, 1, rect.height(), slice, 1, 1,
                  scanline, 0, textureImage);
               break;
            case COL_SLICE_PLANE:
               getPixels (0, 1, rect.width(), row, 1, 1, 0, 1, rect.height (), 
                  0, scanline, textureImage);
               break;
            case ROW_SLICE_PLANE:
               getPixels (col, 1, 1, 0, 1, rect.width(), 0, 1, rect.height(),
                  0, scanline, textureImage);
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
    * @param plane index of plane to get coordinates of
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
      switch (voxelator.getFormat()) {
         case GRAYSCALE:
            return 1;
         case GRAYSCALE_ALPHA:
            return 2;
         case RGB:
            return 3;
         case RGBA:
            return 4;
         default:
      }
      return 0;
   }
   
   public void setPixelGenerator(DTIColorComputer voxelator) {
      this.voxelator = voxelator;
      lastVersion = voxelator.getVersion()-1; // trigger update of image
      
      invalidateData ();
      BufferUtilities.freeDirectBuffer(textureImage);
      textureImage = BufferUtilities.newNativeByteBuffer (getPixelSize()*textureWidth*textureHeight);
   }
   
   public DTIColorComputer getPixelGenerator() {
      return voxelator;
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
      if (lastVersion != voxelator.getVersion()) {
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
         lastVersion = voxelator.getVersion();
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
         textureFrame = new JFrame ("DTITextureContent texture");
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
      int pixelBytes = 4;
      colorModel = new ComponentColorModel (ColorSpace.getInstance (ColorSpace.CS_sRGB),
         new int[] {8, 8, 8, 8}, true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
      
      switch (voxelator.getFormat()) {
         case GRAYSCALE:
            pixelBytes = 1;
            colorModel = new ComponentColorModel (ColorSpace.getInstance (ColorSpace.CS_GRAY),
               new int[] {8}, false, false, ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
            break;
         case GRAYSCALE_ALPHA:
            pixelBytes = 2;
            colorModel = new ComponentColorModel (ColorSpace.getInstance (ColorSpace.CS_GRAY),
               new int[] {8, 8}, true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
            break;
         case RGB:
            pixelBytes = 3;
            colorModel = new ComponentColorModel (ColorSpace.getInstance (ColorSpace.CS_sRGB),
               new int[] {8, 8, 8, 0}, false, false, ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
            break;
         case RGBA:
            pixelBytes = 4;
            colorModel = new ComponentColorModel (ColorSpace.getInstance (ColorSpace.CS_sRGB),
               new int[] {8, 8, 8, 8}, true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
            break;
         default:
         
      }
      
      WritableRaster raster = Raster.createInterleavedRaster (
         colorModel.getTransferType (), textureWidth, textureHeight, colorModel.getNumComponents (), null);
      BufferedImage image = new BufferedImage (colorModel, raster, false, null);
      
      synchronized(textureImage) {
         textureImage.position (0);
         textureImage.limit (textureImage.capacity ());
         
         // flip vertically
         int scanline = textureWidth*pixelBytes;
         int pos = textureWidth*textureHeight*pixelBytes-scanline;
         for (int i=0; i<textureHeight; ++i) {
            for (int j=0; j<scanline; ++j) {
               raster.getDataBuffer ().setElem (pos+j, textureImage.get ());   
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
      switch (voxelator.getFormat()) {
         case GRAYSCALE:
            return ContentFormat.GRAYSCALE_BYTE;
         case GRAYSCALE_ALPHA:
            return ContentFormat.GRAYSCALE_ALPHA_BYTE_2;
         case RGB:
            return ContentFormat.RGB_BYTE_3;
         case RGBA:
            return ContentFormat.RGBA_BYTE_4;
      }
      return null;
   }

   @Override
   public Object getKey () {
      return image;
   }

   @Override
   public DTITextureContent acquire () {
      return (DTITextureContent)super.acquire ();
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
