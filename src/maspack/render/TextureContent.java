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
   public int getPixelWidth();
   
   /**
    * Number of pixels across height in content
    * @return number of pixels
    */
   public int getPixelHeight();

   /**
    * Raw byte data of content
    * @return
    */
   public ByteBuffer getData ();
   
   /**
    * Format of data returned by {@link #getData()}
    * @return format of data in buffer
    * @see #getData()
    */
   public ContentFormat getFormat();
   
   @Override
   public TextureContent acquire();
   
}
