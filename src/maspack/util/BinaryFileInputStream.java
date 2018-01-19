package maspack.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class BinaryFileInputStream extends BinaryInputStream {

   File myFile;
   int myMarkBytes = 0;

   /**
    * Creates a new BinaryFileInputStream from a file. Flag values are
    * set to their defaults so that the stream will be big-endian and use word
    * characters instead of byte characters.
    *
    * @param in underlying input stream
    */
   public BinaryFileInputStream(File file, int flags) throws FileNotFoundException {
      super(new BufferedInputStream(new FileInputStream(file)), flags);
      myFile = file;
   }
   
   public BinaryFileInputStream(File file) throws FileNotFoundException {
      this(file, 0);
   }
   
   File getFile() {
      return myFile;
   }
   
   
}
