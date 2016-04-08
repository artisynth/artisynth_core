package maspack.render;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.util.Hashtable;

import maspack.util.BufferUtilities;
import maspack.util.ReferenceCountedBase;

public class TextureContentImage extends ReferenceCountedBase implements TextureContent {

   private static final ComponentColorModel RGBA_COLOR =
      new ComponentColorModel (
         ColorSpace.getInstance (ColorSpace.CS_sRGB),
         new int[] { 8, 8, 8, 8 }, true, false,
         ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);

   private static final ComponentColorModel RGB_COLOR =
      new ComponentColorModel (
         ColorSpace.getInstance (ColorSpace.CS_sRGB),
         new int[] { 8, 8, 8, 0 }, false, false, 
         ComponentColorModel.OPAQUE,
         DataBuffer.TYPE_BYTE);
  
   BufferedImage image;
   
   public TextureContentImage(BufferedImage image) {
      if (image != null) {
         loadImage(image);
      } else {
         image = null;
      }
   }
   
   protected void loadImage(BufferedImage image) {
      this.image = convertPow2 (image);
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
   
   private BufferedImage convertPow2(BufferedImage image) {
      
      int width = image.getWidth ();
      int height = image.getHeight();
      int width2f = get2Fold (width);
      int height2f = get2Fold (height);
      
      if (width != width2f || height != height2f) {
         // rescale to be power of two
         Image sampled = image.getScaledInstance(width2f, height2f, Image.SCALE_SMOOTH);
         image = new BufferedImage(width2f, width2f, image.getType());
         image.getGraphics().drawImage(sampled, 0, 0 , null);
      }

      return image;
   }

   @Override
   public int getPixelWidth () {
      return image.getWidth ();
   }

   @Override
   public int getPixelHeight () {
      return image.getHeight ();
   }
   
   private ByteBuffer convertImageData (BufferedImage bufferedImage) {

      ByteBuffer imageBuffer = null;
      WritableRaster raster;
      BufferedImage texImage;
      int width = bufferedImage.getWidth();
      int height = bufferedImage.getHeight();

      // create a raster that can be used by OpenGL as a source
      // for a texture
      // XXX maybe check other color spaces?
      if (bufferedImage.getColorModel().hasAlpha()) {
         raster = Raster.createInterleavedRaster (
            DataBuffer.TYPE_BYTE, width, height, 4, null);
         texImage = new BufferedImage (
            RGBA_COLOR, raster, false, new Hashtable<String,Object>());
      } else {
         raster = Raster.createInterleavedRaster (
            DataBuffer.TYPE_BYTE, height, width, 3, null);
         texImage = new BufferedImage (RGB_COLOR, raster, false, new Hashtable<String,Object>());
      }

      // copy the source image into the produced image
      Graphics g = texImage.getGraphics();
      g.setColor (new Color (0f, 0f, 0f, 0f));
      g.fillRect (0, 0, width, height);
      g.drawImage (bufferedImage, 0, 0, null);

      // build a byte buffer from the temporary image
      // that be used by OpenGL to produce a texture.
      byte[] data = ((DataBufferByte)texImage.getRaster().getDataBuffer()).getData ();

      imageBuffer = BufferUtilities.newNativeByteBuffer (data.length);
      imageBuffer.put (data, 0, data.length);
      imageBuffer.flip ();

      return imageBuffer;
   }
   
   public BufferedImage getImage() {
      return image;
   }
   
   @Override
   public ByteBuffer getData() {
      return convertImageData (image);
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
