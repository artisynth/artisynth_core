package maspack.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class TextureContentFile extends TextureContentImage {

   File file;
  
   public TextureContentFile(String filename) {
      super(null);
      this.file = new File(filename);
      
      BufferedImage image;
      try {
         image = ImageIO.read (file);
         super.loadImage (image);
      }
      catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   public String getFileName() {
      return file.getAbsolutePath ();
   }
   
   public File getFile() {
      return file;
   }
   
}
