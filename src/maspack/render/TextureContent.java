package maspack.render;

import java.nio.ByteBuffer;

import maspack.util.ReferenceCounted;

/**
 * Content of Texture Properties (either raw data, or file)
 *
 */
public interface TextureContent extends ReferenceCounted {
   
   public enum ContentFormat {
      RGB_BYTE_3,
      RGBA_BYTE_4,
      RGBA_INTEGER,
      GRAYSCALE_BYTE,
      GRAYSCALE_ALPHA_BYTE_2,
      GRAYSCALE_SHORT,
      GRAYSCALE_ALPHA_SHORT_2
   }

   /**
    * Number of pixels across width in content
    * @return number of pixels
    */
   public int getWidth();
   
   /**
    * Number of pixels across height in content
    * @return number of pixels
    */
   public int getHeight();
   
   /**
    * Size of pixels in bytes
    * @return size
    */
   public int getPixelSize();

   /**
    * Get raw byte data of content, rasterized in
    * row-major form, with (0,0) in the bottom-left
    * corner.
    * @return raw content
    */
   public void getData (ByteBuffer out);
   
   /**
    * Format of data returned by {@link #getData()}
    * @return format of data in buffer
    * @see #getData()
    */
   public ContentFormat getFormat();
   
   @Override
   public TextureContent acquire();
   
}
