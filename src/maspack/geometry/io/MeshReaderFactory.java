package maspack.geometry.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface MeshReaderFactory {

   MeshReader newReader(String fileName)  throws IOException;
   MeshReader newReader(File file)  throws IOException;
   MeshReader newReader(InputStream stream);
   
   String[] getFileExtensions();
   
}
