package maspack.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import maspack.util.Rectangle;

/**
 * Loads image on-demand from a file
 * @author Antonio
 *
 */
public class TextureContentFile extends TextureContentImage {

   File file;
  
   public TextureContentFile(String filename) {
      super(null);
      setFileName(filename);
   }
   
   public void setFileName(String filename) {
      this.file = new File(filename);
      super.setImage (null, false);
   }
   
   public String getFileName() {
      return file.getAbsolutePath ();
   }
   
   public File getFile() {
      return file;
   }
   
   private boolean maybeLoadImage() {
      BufferedImage image = getImage ();
      if (image != null) {
         return false;  // image exists
      }
      
      try {
         image = ImageIO.read (file);
         super.setImage (image, true);
         return true;
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      return false;
   }
   
   @Override
   public void getData (ByteBuffer buff) {
      maybeLoadImage ();
      super.getData (buff);
   }
   
   @Override
   public void getData (Rectangle region, ByteBuffer buff) {
      maybeLoadImage ();
      super.getData (region, buff);
   }
   
   @Override
   public Rectangle getDirty () {
      maybeLoadImage ();
      return super.getDirty ();
   }
   
   @Override
   public boolean isDirty () {
      maybeLoadImage ();
      return super.isDirty ();
   }
   
   /**
    * Uses the filename as a key so that multiple textures referring to the same file
    */
   @Override
   public Object getKey () {
      return file;
   }
   
   @Override
   public int getHeight () {
      maybeLoadImage ();
      return super.getHeight ();
   }
   
   @Override
   public int getWidth () {
      maybeLoadImage ();
      return super.getWidth ();
   }
   
   @Override
   public int getPixelSize () {
      maybeLoadImage ();
      return super.getPixelSize ();
   }
   
   @Override
   public TextureContentFile acquire () {
      return (TextureContentFile)(super.acquire ());
   }
   
}
