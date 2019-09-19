package maspack.geometry.io;

import java.io.*;
import java.util.*;

import maspack.geometry.*;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.BinaryOutputStream;

/**
 * Writes a PointMesh to a binary .xyzb file.
 * @author John Lloyd, Jan 2014
 */
public class XyzbWriter extends MeshWriterBase {
   
   public XyzbWriter (OutputStream os) throws IOException{
      super (os);
   }

   public XyzbWriter (File file) throws IOException {
      super (file);
   }

   public XyzbWriter (String fileName) throws IOException {
      this (new File(fileName));
   }

   public void writeMesh (MeshBase mesh) throws IOException {

      BinaryOutputStream bout =
         new BinaryOutputStream (new BufferedOutputStream (myOstream));
      bout.setLittleEndian (true);

      if (mesh instanceof PointMesh) {
         writeMesh (bout, (PointMesh)mesh);
      }
      else {
         bout.close();
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for '.xyzb' files");
      }
   }

   /**
    * Writes a PointMesh to a PrintWriter, using the binary xyzb
    * format.
    * 
    * @param bout
    * Binary output stream to write this mesh to
    * @param mesh
    * PointMesh to be written
    */
   public void writeMesh (BinaryOutputStream bout, PointMesh mesh)
      throws IOException {

      ArrayList<Vertex3d> vertices = mesh.getVertices();
      ArrayList<Vector3d> normals = null;
      if (getWriteNormals (mesh)) {
         normals = mesh.getNormals();
      }
      
      for (int i=0; i<vertices.size(); i++) {
         Point3d pnt = vertices.get(i).pnt;

         bout.writeFloat ((float)pnt.x);
         bout.writeFloat ((float)pnt.y);
         bout.writeFloat ((float)pnt.z);

         if (normals != null) {
            Vector3d nrm = normals.get(i);
            bout.writeFloat ((float)nrm.x);
            bout.writeFloat ((float)nrm.y);
            bout.writeFloat ((float)nrm.z);
         }
      }
      bout.flush();
   }

//   public void write (File file, MeshBase mesh) throws IOException {
//      FileOutputStream fout = null;
//      try {
//         fout = new FileOutputStream(file);
//         write (fout, mesh);
//      } catch (IOException ex) {
//         throw ex;
//      } finally {
//         closeQuietly (fout);
//      }
//   }

//   public void write (String filename, MeshBase mesh) throws IOException {
//      write (new File(filename), mesh);
//   }

   public static void writeMesh (String fileName, MeshBase mesh)
      throws IOException {
      writeMesh (new File(fileName), mesh);
   }      

   public static void writeMesh (File file, MeshBase mesh)
      throws IOException {
      XyzbWriter writer = null;
      try {
         writer = new XyzbWriter(file);
         writer.writeMesh (mesh);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (writer != null) {
            writer.close();
         }
      }     
   }      

   private void closeQuietly(OutputStream out) {
      if (out != null) {
         try {
            out.close();
         } catch (IOException e) {}
      }
   }
}
