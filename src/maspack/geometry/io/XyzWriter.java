package maspack.geometry.io;

import java.io.*;
import java.util.*;

import maspack.geometry.*;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.NumberFormat;

/**
 * Writes a PointMesh to an ascii .xyz file.
 * @author John Lloyd, Jan 2014
 */
public class XyzWriter extends MeshWriterBase {

   public XyzWriter (OutputStream os) throws IOException{
      super (os);
      setFormat ("%.10g");
   }

   public XyzWriter (File file) throws IOException {
      super (file);
      setFormat ("%.10g");
   }

   public XyzWriter (String fileName) throws IOException {
      this (new File(fileName));
   }

   public void writeMesh (MeshBase mesh) throws IOException {

      PrintWriter pw =
         new PrintWriter (
            new BufferedWriter (new OutputStreamWriter (myOstream)));

      if (mesh instanceof PointMesh) {
         writeMesh (pw, (PointMesh)mesh);
         pw.close();
      }
      else {
         pw.close();
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for '.xyz' files");
      }
   }

   /**
    * Writes a PointMesh to a PrintWriter, using the simple ascii xyz
    * format.
    * 
    * <p>
    * The format used to print the vertex and normal coordinates can be
    * controlled by
    * {@link #setFormat(String)} or {@link #setFormat(NumberFormat)}.
    * The default format has eight decimal places and is specified
    * by the string <code>"%.8g"</code>.
    * 
    * @param pw
    * PrintWriter to write this mesh to
    * @param mesh
    * PointMesh to be written
    */
   public void writeMesh (PrintWriter pw, PointMesh mesh) 
      throws IOException {

      ArrayList<Vertex3d> vertices = mesh.getVertices();
      ArrayList<Vector3d> normals = null;
      if (getWriteNormals (mesh)) {
         normals = mesh.getNormals();
      }
      
      for (int i=0; i<vertices.size(); i++) {
         Point3d pnt = vertices.get(i).pnt;
         pw.print (
            myFmt.format (pnt.x) + " " + myFmt.format (pnt.y) + " " +
            myFmt.format (pnt.z));
         if (normals != null) {
            Vector3d nrm = normals.get(i);
            pw.print (
               " " + myFmt.format (nrm.x) + " " + myFmt.format (nrm.y) + " " +
               myFmt.format (nrm.z));
         }
         pw.println ("");
      }
      pw.flush();
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


   private void closeQuietly(OutputStream out) {
      if (out != null) {
         try {
            out.close();
         } catch (IOException e) {}
      }
   }
}
