package maspack.geometry.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;

public abstract class MeshReaderBase implements MeshReader {

   protected InputStream myIstream;
   protected File myFile;

   protected MeshReaderBase (InputStream is) {
      myIstream = is;
   }

   protected MeshReaderBase (File file) throws IOException {
      this (new FileInputStream (file));
      myFile = file;
   }

   private void closeQuietly(InputStream in) {
      if (in != null) {
         try {
            in.close();
         } catch (IOException e) {}
      }
   }

   public MeshBase readMesh() throws IOException {
      return readMesh (new PolygonalMesh());
   }

   public void close() {
      closeQuietly(myIstream);
   }
   
   @Override
   protected void finalize() throws Throwable {
      super.finalize();
      close();
   }

}
