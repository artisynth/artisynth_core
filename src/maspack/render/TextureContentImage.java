package maspack.render;

import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;

import maspack.util.ReferenceCountedBase;

public class TextureContentImage extends ReferenceCountedBase implements TextureContent {

   // RGBA color model
   private static final ComponentColorModel RGBA_COLOR =
      new ComponentColorModel (
         ColorSpace.getInstance (ColorSpace.CS_sRGB),
         new int[] { 8, 8, 8, 8 }, true, false,
         ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);

   //   // RGB Color model
   //   private static final ComponentColorModel RGB_COLOR =
   //      new ComponentColorModel (
   //         ColorSpace.getInstance (ColorSpace.CS_sRGB),
   //         new int[] { 8, 8, 8, 0 }, false, false, 
   //         ComponentColorModel.OPAQUE,
   //         DataBuffer.TYPE_BYTE);
   
   public static boolean DEFAULT_USE_POWER_OF_TWO = true;
   BufferedImage image;
   
   public TextureContentImage() {
      this(null, DEFAULT_USE_POWER_OF_TWO);
   }
   
   public TextureContentImage(BufferedImage image, boolean powerOfTwo) {
      if (image != null) {
         setImage(image, powerOfTwo);
      } else {
         this.image = null;
      }
   }
   
   protected void setImage(BufferedImage image, boolean powerOfTwo) {
      this.image = convert (image, powerOfTwo);
   }
   
   /**
    * Get the closest greater power of 2 to the fold number
    * 
    * @param fold
    * The target number
    * @return The power of 2
    */
   private int get2Fold (int fold) {
      int ret = 2;
      while (ret < fold) {
         ret *= 2;
      }
      return ret;
   }
   
   /**
    * Converts to RGBA, maybe scales to power-of-two
    * @param bufferedImage
    * @param buff
    */
   private BufferedImage convert(BufferedImage image, boolean pow2) {
      int width = image.getWidth ();
      int height = image.getHeight();
      
      Image copyme = image;
      if (pow2) {
         int width2f = get2Fold (width);
         int height2f = get2Fold (height);
         if (width != width2f || height != height2f) {
            copyme = image.getScaledInstance(width2f, height2f, Image.SCALE_SMOOTH);
         }
         width =  width2f;
         height = height2f;
      }
      WritableRaster raster = Raster.createInterleavedRaster (DataBuffer.TYPE_BYTE, width, height, 4, null);
      image = new BufferedImage(RGBA_COLOR, raster, false, null);
      image.getGraphics().drawImage(copyme, 0, 0 , null);
      
      return image;
   }

   @Override
   public int getWidth () {
      return image.getWidth ();
   }

   @Override
   public int getHeight () {
      return image.getHeight ();
   }
   
   @Override
   public int getPixelSize () {
      return 4; // always RGBA
   }
   
   public BufferedImage getImage() {
      return image;
   }
   
   @Override
   public void getData(ByteBuffer buff) {
      
      // build a byte buffer from the temporary image
      // that be used by OpenGL to produce a texture.
      byte[] data = ((DataBufferByte)image.getRaster().getDataBuffer()).getData ();

      // flip rows
      int height = image.getHeight ();
      int rowlength = data.length/height;
      int pos = data.length-rowlength;
      for (int i=0; i<height; ++i) {
         buff.put (data, pos, rowlength);
         pos -= rowlength;
      }
   }
   
   @Override
   public ContentFormat getFormat () {
      return ContentFormat.RGBA_BYTE_4;  // since we currently always convert it to this
   }
   
   @Override
   public TextureContentImage acquire () {
      return (TextureContentImage)super.acquire ();
   }
}
