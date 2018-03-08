package maspack.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class BinaryFileInputStream extends BinaryInputStream {

   File myFile;

   /**
    * Creates a new BinaryFileInputStream from a file. Flag values are
    * set to their defaults so that the stream will be big-endian and use word
    * characters instead of byte characters.
    *
    * @param file underlying file 
    * @param flags flag values
    */
   public BinaryFileInputStream(File file, int flags) throws FileNotFoundException {
      super(new BufferedInputStream(new FileInputStream(file)), flags);
      myFile = file;
   }
   
   public BinaryFileInputStream(File file) throws FileNotFoundException {
      this(file, 0);
   }
   
   public File getFile() {
      return myFile;
   }
   
   
}
