package maspack.render;

import java.nio.ByteBuffer;

import maspack.util.Rectangle;
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
      GRAYSCALE_UBYTE,
      GRAYSCALE_ALPHA_BYTE_2,
      GRAYSCALE_SHORT,
      GRAYSCALE_USHORT,
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
    */
   public void getData (ByteBuffer out);
   
   /**
    * Get raw byte data for a region, rasterized in row-major
    * form with (0,0) in the bottom-left corner.
    * @param rect rectangular region to acquire data for
    * @param out buffer to fill
    */
   public void getData(Rectangle rect, ByteBuffer out);
   
   /**
    * All or part of the texture has been marked as "dirty" (i.e. updated)
    */
   public boolean isDirty();
   
   /**
    * The portion of the texture that has been modified (marked "dirty")
    * @return bounding region of dirty area, null if nothing marked as dirty
    */
   public Rectangle getDirty();
   
   /**
    * Mark the entire texture as being clean
    */
   public void markClean();
   
   /**
    * Format of data returned by {@link #getData}
    * @return format of data in buffer
    * @see #getData
    */
   public ContentFormat getFormat();
   
   /**
    * A key for referring to this texture.  If two contents have the same
    * key, then it is assumed they have the same underlying data (e.g.
    * a filename, if loaded directly from a file).  The key should be
    * immutable (at least from a hashcode/equals perspective)
    * @return uniquely identifying key
    */
   public Object getKey();
   
   @Override
   public TextureContent acquire();
   
}
