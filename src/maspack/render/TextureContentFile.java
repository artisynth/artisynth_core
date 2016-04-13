package maspack.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

/**
 * Loads image on-demand from a file
 * @author Antonio
 *
 */
public class TextureContentFile extends TextureContentImage {

   File file;
  
   public TextureContentFile(String filename) {
      this.file = new File(filename);
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
      maybeLoadImage();
      super.getData (buff);
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
