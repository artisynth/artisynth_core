package maspack.geometry.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import maspack.geometry.Face;
import maspack.geometry.MeshBase;
import maspack.geometry.PointMesh;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.NumberFormat;

/**
 * Writes to ascii STL format
 * @author Antonio
 *
 */
public class StlWriter extends MeshWriterBase {
   
   public StlWriter (OutputStream os) throws IOException{
      super (os);
      setFormat ("%.10g");
   }

   public StlWriter (File file) throws IOException {
      super (file);
      setFormat ("%.10g");
   }

   public StlWriter (String fileName) throws IOException {
      this (new File(fileName));
   }

   public void writeMesh (PolygonalMesh mesh) throws IOException {
      
      NumberFormat fmt = myFmt;
      
      PrintWriter pw = 
         new PrintWriter (
            new BufferedWriter (new OutputStreamWriter(myOstream))); 
      
      String name = mesh.getName();
      if (name == null) {
         name = "";
      }
      
      pw.println("solid " + name);
      
      for (Face face : mesh.getFaces()) {
         Vector3d n = face.getNormal();
         
         if (face.isTriangle()) {
            pw.println("facet normal " + fmt.format((float)n.x) + " " 
               + fmt.format((float)n.y) + " " + fmt.format((float)n.z));
         } else {
            pw.println("facet normal 0 0 0");
         }
         pw.println("  outer loop");
         for (int i=0; i<face.numVertices(); i++) {
            Point3d pos = face.getVertex(i).getPosition();
            pw.println("    vertex " + fmt.format((float)pos.x) + " "
               + fmt.format((float)pos.y) + " " + fmt.format((float)pos.z));
         }   
         pw.println("  endloop");
         pw.println("endfacet");
      
      }
      pw.println("endsolid " + name);
      pw.close();
   }

   @Override
   public void writeMesh (MeshBase mesh)
      throws IOException {

      if (mesh instanceof PolygonalMesh) {
         writeMesh ((PolygonalMesh)mesh);
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for '.stl' files");
      }
   }
//   @Override
//   public void write(OutputStream ostream, PolygonalMesh mesh)
//      throws IOException {
//      
//      PrintWriter pw = new PrintWriter(ostream);
//      write(pw, mesh, myFmt);
//      
//   }
   
}
