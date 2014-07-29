package maspack.geometry.io;

import java.io.IOException;

import maspack.geometry.MeshBase;

/**
 * Interface for a simple mesh reader utility
 * Reads a single mesh from a file provided
 * 
 * @author Antonio
 *
 */
public interface MeshReader {

   //public MeshBase read (InputStream istream) throws IOException;
   //public MeshBase read (File file) throws IOException;
   //public MeshBase read (String filename) throws IOException;
   
   public MeshBase readMesh (MeshBase mesh) throws IOException;
   public void close();

   //public MeshBase read (MeshBase mesh, InputStream istream) throws IOException;
   //public MeshBase read (MeshBase mesh, File file) throws IOException;
   //public MeshBase read (MeshBase mesh, String filename) throws IOException;
   
}
