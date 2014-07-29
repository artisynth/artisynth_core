package maspack.geometry.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public interface MeshWriterFactory {

   MeshWriter newWriter(String fileName) throws IOException;
   MeshWriter newWriter(File file) throws IOException;
   MeshWriter newWriter(OutputStream stream);
   String[] getFileExtensions();
   
}
