package maspack.render;

import java.nio.ByteBuffer;

import maspack.util.BinaryTreeRectanglePacker;
import maspack.util.Rectangle;
import maspack.util.ReferenceCountedBase;

/**
 * Wrapper around a {@link TextImageStore} for storing texture content
 * @author Antonio
 *
 */
public class TextureContentText extends ReferenceCountedBase implements TextureContent {

   public static boolean DEFAULT_ANTIALIASING = true;
   
   int width;
   int height;
   TextImageStore textImage;
   
   public TextureContentText (int width, int height) {
      this(width, height, DEFAULT_ANTIALIASING);
   }
   
   public TextureContentText (int width, int height, boolean antialiasing) {
      this.width = width;
      this.height = height;
      textImage = new TextImageStore ( 
         new BinaryTreeRectanglePacker (width, height), antialiasing);
   }
   
   public void setAntialiasing(boolean set) {
      textImage.setAntialiasing (set);
   }
   
   /**
    * Backing storage so that characters can be uploaded.
    * All updates should be done through the backing store.
    * @return backing store
    */
   public TextImageStore getBackingStore() {
      return textImage;
   }
   
   @Override
   public int getWidth () {
      return width;
   }

   @Override
   public int getHeight () {
      return height;
   }

   @Override
   public int getPixelSize () {
      return textImage.getPixelSize ();
   }

   @Override
   public void getData (ByteBuffer out) {
      textImage.getData (out);
   }
   
   @Override
   public void getData (Rectangle rect, ByteBuffer out) {
      textImage.getData (rect, out);
   }
   
   @Override
   public Rectangle getDirty () {
      return textImage.getDirty ();
   }
   
   @Override
   public boolean isDirty () {
      return (getDirty () != null);
   }
   
   @Override
   public void markClean () {
      textImage.markClean ();
   }
   
   @Override
   public Object getKey () {
      return textImage;
   }

   @Override
   public ContentFormat getFormat () {
      return ContentFormat.RGBA_BYTE_4;
   }

   @Override
   public TextureContentText acquire () {
      return (TextureContentText)super.acquire ();
   }

}
