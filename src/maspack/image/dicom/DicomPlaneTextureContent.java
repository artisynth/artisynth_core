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

import javax.swing.JFrame;
import javax.swing.JPanel;

import maspack.image.dicom.DicomPixelBuffer.PixelType;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector2i;
import maspack.render.TextureContent;
import maspack.util.BufferUtilities;
import maspack.util.Rectangle;
import maspack.util.ReferenceCountedBase;

public class DicomPlaneTextureContent extends ReferenceCountedBase implements TextureContent {

   DicomImage image;
   DicomWindowPixelInterpolator window;

   String lastWindow;
   int lastWindowCentre;
   int lastWindowWidth;
   boolean valid; // marks region in backing image as invalid
   boolean dirty;
   Rectangle rect;

   ByteBuffer textureImage; // backing image
   Vector2i  res;
   PixelType internalStorage;

   // slice info
   boolean interpolate;
   RigidTransform3d location;
   Vector2d widths;
   int time;

   public DicomPlaneTextureContent(DicomImage image, 
      RigidTransform3d plane, Vector2d widths) {

      // based on resolution of the underlying image
      this(image, plane, widths, null);

   }

   public DicomPlaneTextureContent(DicomImage image, 
      RigidTransform3d plane, Vector2d widths, Vector2i res) {
      this.image = image;
      lastWindow = null;
      lastWindowCentre = -1;
      lastWindowWidth = -1;
      interpolate = false;
      valid = false;
      dirty = true;
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

      this.location = plane.copy();
      this.widths = widths.clone();
      time = 0;

      if (res == null) {
         res = new Vector2i();
         double mind = Math.min(image.pixelSpacingSlice, Math.min(image.pixelSpacingCols, image.pixelSpacingRows));
         res.x = (int)(2*Math.ceil(widths.x/mind));
         res.y = (int)(2*Math.ceil(widths.y/mind));
      }

      rect  = new Rectangle(0,0,res.x, res.y);
      this.res = new Vector2i(get2Fold(res.x), get2Fold(res.y));

      textureImage = BufferUtilities.newNativeByteBuffer (getPixelSize()*this.res.x*this.res.y);
   }

   public DicomWindowPixelInterpolator getWindowConverter() {
      return window;
   }


   /**
    * Linearly interpolate between voxels
    * @param set if true, interpolates, otherwise uses nearest voxel
    */
   public void setSpatialInterpolation(boolean set) {
      if (interpolate != set) {
         interpolate = set;
         invalidateData();
      }
   }
   
   public boolean getSpatialInterpolation() {
      return interpolate;
   }

   public void setLocation(RigidTransform3d loc) {
      if (!loc.equals(this.location)) {
         this.location.set(loc);
         invalidateData();
      }
   }

   public void setWidths(Vector2d widths) {
      if (!this.widths.equals(widths)) {
         this.widths.set(widths);
         invalidateData();
      }
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
   protected void updateBackingBuffer() {

      int psize = getPixelSize ();

      int scanline = psize*res.x;

      // voxels into spatial
      AffineTransform3d vtrans = image.getVoxelTransform();
      vtrans.invert();  // spatial into voxels

      // point for computing location
      Point3d pnt = new Point3d();
      Point3d vpnt = new Point3d();

      double dx = widths.x/rect.width();
      double dy = widths.y/rect.height();

      synchronized (textureImage) {
         textureImage.limit (textureImage.capacity ());

         for (int j=0; j<rect.height(); ++j) {
            textureImage.position ((rect.y ()+j)*scanline+psize*rect.x ());

            for (int i=0; i<rect.width(); ++i) {
               pnt.x = (-widths.x + (2*i+1)*dx )/2.0;
               pnt.y = (-widths.y + (2*j+1)*dy )/2.0;
               pnt.z = 0;

               vpnt.transform(location, pnt);  // world coordinate
               vpnt.transform(vtrans);         // voxel coordinate

               int vx = (int)Math.floor(vpnt.x);
               int vy = (int)Math.floor(vpnt.y);
               int vz = (int)Math.floor(vpnt.z);

               // interpolate around voxel
               int[] voxel = new int[3];
               switch(internalStorage) {
                  case UBYTE:
                  case BYTE: {
                     double val = 0;

                     if (interpolate) {
                        // scale factors for interp
                        double cx = (vpnt.x-vx)/image.pixelSpacingCols;
                        double cy = (vpnt.y-vy)/image.pixelSpacingRows;
                        double cz = (vpnt.z-vz)/image.pixelSpacingSlice;
                        double[][] cc = {{1-cx, cx},{1-cy, cy}, {1-cz, cz}};

                        int xmin = Math.max(vx, 0);
                        int xmax = Math.min(vx+2, image.getNumCols()-1);
                        int ymin = Math.max(vy, 0);
                        int ymax = Math.min(vy+2, image.getNumRows()-1);
                        int zmin = Math.max(vz, 0);
                        int zmax = Math.min(vz+2, image.getNumSlices()-1);
                        for (int xx = xmin; xx < xmax; ++xx) {
                           for (int yy = ymin; yy < ymax; ++yy) {
                              for (int zz = zmin; zz<zmax; ++zz) {
                                 image.getPixels(xx, yy, zz, 0, 0, 0, 1, 1, 1, 
                                    PixelType.UBYTE, 0, 0, window, voxel, 0);
                                 // interpolate
                                 val += cc[0][xx-vx]*cc[1][yy-vy]*cc[2][zz-vz]*(voxel[0]);
                              }
                           }
                        }
                     } else {
                        int vvx = (int)Math.round(vpnt.x);
                        int vvy = (int)Math.round(vpnt.y);
                        int vvz = (int)Math.round(vpnt.z);
                        if (vvx >= 0 && vvx < image.getNumCols() && vvy >= 0 && vvy < image.getNumRows() 
                           && vvz >= 0 && vvz < image.getNumSlices()) {
                           image.getPixels(vvx, vvy, vvz, 0, 0, 0, 1, 1, 1, 
                              PixelType.UBYTE, 0, 0, window, voxel, 0);
                           val = voxel[0];
                        }
                     }

                     // round and convert to byte
                     textureImage.put((byte)Math.round(val));

                     break;
                  }
                  case UBYTE_RGB: {
                     double rval = 0;
                     double gval = 0;
                     double bval = 0;

                     if (interpolate) {
                        // scale factors for interp
                        double cx = (vpnt.x-vx)/image.pixelSpacingCols;
                        double cy = (vpnt.y-vy)/image.pixelSpacingRows;
                        double cz = (vpnt.z-vz)/image.pixelSpacingSlice;
                        double[][] cc = {{1-cx, cx},{1-cy, cy}, {1-cz, cz}};

                        int xmin = Math.max(vx, 0);
                        int xmax = Math.min(vx+2, image.getNumCols()-1);
                        int ymin = Math.max(vy, 0);
                        int ymax = Math.min(vy+2, image.getNumRows()-1);
                        int zmin = Math.max(vz, 0);
                        int zmax = Math.min(vz+2, image.getNumSlices()-1);
                        for (int xx = xmin; xx < xmax; ++xx) {
                           for (int yy = ymin; yy < ymax; ++yy) {
                              for (int zz = zmin; zz<zmax; ++zz) {
                                 image.getPixels(xx, yy, zz, 0, 0, 0, 1, 1, 1,
                                    PixelType.UBYTE_RGB, 0, 0, window,
                                    voxel, 0);
                                 // interpolate
                                 double ccc = cc[0][xx-vx]*cc[1][yy-vy]*cc[2][zz-vz]; 
                                 rval += ccc*(voxel[0]);
                                 gval += ccc*(voxel[1]);
                                 bval += ccc*(voxel[2]);
                              }
                           }
                        }
                     } else {
                        int vvx = (int)Math.round(vpnt.x);
                        int vvy = (int)Math.round(vpnt.y);
                        int vvz = (int)Math.round(vpnt.z);
                        if (vvx >= 0 && vvx < image.getNumCols() && vvy >= 0 && vvy < image.getNumRows() 
                           && vvz >= 0 && vvz < image.getNumSlices()) {
                           image.getPixels(vvx, vvy, vvz, 0, 0, 0, 1, 1, 1, 
                              PixelType.UBYTE_RGB, 0, 0, window, voxel, 0);
                           rval = voxel[0];
                           gval = voxel[1];
                           bval = voxel[2];
                        }
                     }
                     // round and convert to byte
                     textureImage.put((byte)Math.round(rval));
                     textureImage.put((byte)Math.round(gval));
                     textureImage.put((byte)Math.round(bval));
                     break;
                  }
                  case USHORT:
                  case SHORT: {
                     double val = 0;

                     if (interpolate) {
                        // scale factors for interp, pixel space have widths of 1
                        double cx = (vpnt.x-vx);
                        double cy = (vpnt.y-vy);
                        double cz = (vpnt.z-vz);
                        double[][] cc = {{1-cx, cx},{1-cy, cy}, {1-cz, cz}};

                        int xmin = Math.max(vx, 0);
                        int xmax = Math.min(vx+2, image.getNumCols()-1);
                        int ymin = Math.max(vy, 0);
                        int ymax = Math.min(vy+2, image.getNumRows()-1);
                        int zmin = Math.max(vz, 0);
                        int zmax = Math.min(vz+2, image.getNumSlices()-1);
                        for (int xx = xmin; xx < xmax; ++xx) {
                           for (int yy = ymin; yy < ymax; ++yy) {
                              for (int zz = zmin; zz<zmax; ++zz) {
                                 image.getPixels(xx, yy, zz, 0, 0, 0, 1, 1, 1, 
                                    PixelType.USHORT, 0, 0, window, voxel, 0);
                                 // interpolate
                                 val += cc[0][xx-vx]*cc[1][yy-vy]*cc[2][zz-vz]*(voxel[0]);
                              }
                           }
                        }
                     } else {
                        int vvx = (int)Math.round(vpnt.x);
                        int vvy = (int)Math.round(vpnt.y);
                        int vvz = (int)Math.round(vpnt.z);
                        if (vvx >= 0 && vvx < image.getNumCols() && vvy >= 0 && vvy < image.getNumRows() 
                           && vvz >= 0 && vvz < image.getNumSlices()) {
                           image.getPixels(vvx, vvy, vvz, 0, 0, 0, 1, 1, 1,
                              PixelType.USHORT, 0, 0, window, voxel, 0);
                           val = voxel[0];
                        }
                     }
                     // round and convert to short
                     textureImage.putShort((short)(Math.round(val)));

                     break;
                  }                
               }

            }
         }

         valid = true;

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

   protected void invalidateData() {
      valid = false;
      dirty = true;
   }

   @Override
   public int getWidth () {
      return res.x;
   }

   @Override
   public int getHeight () {
      return res.y;
   }

   /**
    * Texture coordinates, starting with the
    * bottom-left corner and working around clockwise.
    *
    * @return texture coordinates
    */
   public Point2d[] getTextureCoordinates() {
      Point2d[] out = new Point2d[4];

      int w = res.x-1;
      int h = res.y-1;

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
      if (!valid) {
         updateBackingBuffer();
         updated = true;
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
      int scanline = res.x * pixelWidth;
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
         colorModel.getTransferType (), res.x, res.y, colorModel.getNumComponents (), null);
      BufferedImage image = new BufferedImage (colorModel, raster, false, null);

      synchronized(textureImage) {
         textureImage.position (0);
         textureImage.limit (textureImage.capacity ());

         // flip vertically
         int scanline = res.x;
         int pos = res.x*res.y-scanline;
         for (int i=0; i<res.y; ++i) {
            for (int j=0; j<res.x; ++j) {
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
      return dirty;
   }

   @Override
   public Rectangle getDirty () {
      maybeUpdateImage ();

      int left = res.x; // past the end
      int bottom = res.y;
      int right = -1;
      int top = -1;


      if (dirty) {
         left = Math.min (left, rect.x ());
         right = Math.max (right, rect.x ()+rect.width ()-1);
         bottom = Math.min (bottom, rect.y ());
         top = Math.max (top, rect.y ()+rect.height ()-1);
      }


      if (left == res.x || bottom == res.y) {
         return null;
      }

      return new Rectangle(left,bottom,right-left+1,top-bottom+1);
   }

   @Override
   public void markClean () {
      dirty = false;
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
      return this;
   }

   @Override
   public DicomPlaneTextureContent acquire () {
      return (DicomPlaneTextureContent)super.acquire ();
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
