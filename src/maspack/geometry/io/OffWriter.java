package maspack.geometry.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import maspack.geometry.Face;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;

/**
 * Writes to ascii OFF format
 * @author Antonio
 *
 */
public class OffWriter extends MeshWriterBase {

   public OffWriter (OutputStream os) throws IOException{
      super (os);
      setFormat ("%.10g");
   }

   public OffWriter (File file) throws IOException {
      super (file);
      setFormat ("%.10g");
   }

   public OffWriter (String fileName) throws IOException {
      this (new File(fileName));
   }
   
   public static void writeMesh(String fileName, PolygonalMesh mesh) throws IOException {
      OffWriter writer = null;
      try {
         new OffWriter(fileName);
         writer.writeMesh(mesh);
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
   
   public void writeMesh (PolygonalMesh mesh) throws IOException {
      
      PrintWriter pw = new PrintWriter (
         new BufferedWriter (new OutputStreamWriter (myOstream)));

      pw.println("OFF");
      pw.printf("%d %d %d\n", mesh.numVertices(), mesh.numFaces(), 0);
      pw.flush();

      int[] oldIdxs = new int[mesh.numVertices()];
      int idx = 0;
      for (Vertex3d vtx : mesh.getVertices()) {
         Point3d pnt = vtx.getPosition();
         pw.println(myFmt.format(pnt.x)+ " " + myFmt.format(pnt.y) + " " + myFmt.format(pnt.z));
      
         // protect vertex indices numbers
         oldIdxs[idx] = vtx.getIndex();
         vtx.setIndex(idx);
         idx++;
      }
      
      for (Face face : mesh.getFaces()) {
         int nf = face.numVertices();
         pw.print(nf);
         for (int j=0; j<face.numVertices(); j++) {
            pw.print(" " + face.getVertex(j).getIndex());
         }
         pw.println();
      }
      pw.flush();
      
      // restore vertex indices
      idx = 0;
      for (Vertex3d vtx : mesh.getVertices()) {
         vtx.setIndex(oldIdxs[idx]);
         idx++;
      }
   }

   @Override
   public void writeMesh (MeshBase mesh)
      throws IOException {

      if (mesh instanceof PolygonalMesh) {
         writeMesh ((PolygonalMesh)mesh);
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for '.off' files");
      }     
   }


   
}
