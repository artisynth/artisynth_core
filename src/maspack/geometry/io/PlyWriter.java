package maspack.geometry.io;

import java.io.*;
import java.util.*;

import maspack.geometry.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.geometry.io.MeshWriter.DataFormat;

/**
 * Writes to ascii PLY format
 * @author Antonio
 *
 */
public class PlyWriter extends MeshWriterBase {

   // public enum DataFormat {
   //    ASCII, BINARY_LITTLE_ENDIAN, BINARY_BIG_ENDIAN };

   public enum DataType {
      CHAR, UCHAR, SHORT, USHORT, INT, UINT, FLOAT, DOUBLE }
   ;

   protected DataFormat myDataFormat = DataFormat.ASCII;
   protected DataType myFloatType = DataType.DOUBLE;

   public DataFormat getDataFormat () {
      return myDataFormat;
   }

   public void setDataFormat (DataFormat fmt) {
      myDataFormat = fmt;
   }

   public DataType getFloatType () {
      return myFloatType;
   }

   public void setFloatType (DataType type) {
      if (type != DataType.FLOAT && type != DataType.DOUBLE) {
         throw new IllegalArgumentException (
            "Float type must be FLOAT or DOUBLE");
      }
      myFloatType = type;
   }

   public PlyWriter (OutputStream os) throws IOException{
      super (os);
      setFormat ("%.10g");
   }

   public PlyWriter (File file) throws IOException {
      super (file);
      setFormat ("%.10g");
   }

   public PlyWriter (String fileName) throws IOException {
      this (new File(fileName));
   }

   // public static void write (
   //    PrintWriter writer, PolygonalMesh mesh, NumberFormat fmt) {
   // }
   
   private void writeVertexInfo (
      PrintWriter pw, ArrayList<Vertex3d> vertices, ArrayList<Vector3d> normals) {

      for (int i=0; i<vertices.size(); i++) {
         Point3d pos = vertices.get(i).getPosition();
         pw.println(myFmt.format(pos.x) + " " + 
                    myFmt.format(pos.y) + " " +
                    myFmt.format(pos.z));
         if (normals != null && i < normals.size()) {
            Vector3d nrm = normals.get(i);
            pw.println(myFmt.format(nrm.x) + " " + 
                       myFmt.format(nrm.y) + " " +
                       myFmt.format(nrm.z));
         }
      }
   }

   private void writeNumber (BinaryOutputStream bos, double val)
      throws IOException {

      switch (myFloatType) {
         case DOUBLE: {
            bos.writeDouble (val);
            break;
         }
         case FLOAT: {
            bos.writeFloat ((float)val);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented data size: " + myFloatType);
         }
      }
   }            

   private void writeVertexInfo (
      BinaryOutputStream bos,
      ArrayList<Vertex3d> vertices, ArrayList<Vector3d> normals)
      throws IOException {

      for (int i=0; i<vertices.size(); i++) {
         Point3d pos = vertices.get(i).getPosition();
         writeNumber (bos, pos.x);
         writeNumber (bos, pos.y);
         writeNumber (bos, pos.z);
         if (normals != null && i < normals.size()) {
            Vector3d nrm = normals.get(i);
            writeNumber (bos, nrm.x);
            writeNumber (bos, nrm.y);
            writeNumber (bos, nrm.z);
         }
      }
   }

   public void writeMesh (PolygonalMesh mesh) throws IOException {
      
      PrintWriter pw = new PrintWriter (
         new BufferedWriter (new OutputStreamWriter (myOstream)));

      ArrayList<Vertex3d> vertices = mesh.getVertices();
      ArrayList<Vector3d> normals = null;
      if (getWriteNormals (mesh)) {
         normals = mesh.getNormals();
      }

      String dsize = getFloatType().toString().toLowerCase();

      pw.println("ply");
      pw.println("format " + myDataFormat.toString().toLowerCase() + " 1.0");
      pw.println("element vertex " + mesh.numVertices());
      
      pw.println("property "+dsize+" x");
      pw.println("property "+dsize+" y");
      pw.println("property "+dsize+" z");
      if (normals != null) {
         pw.println("property "+dsize+" nx");
         pw.println("property "+dsize+" ny");
         pw.println("property "+dsize+" nz");
      }
      pw.println("element face " + mesh.numFaces());
      pw.println("property list uchar int vertex_indices");
      pw.println("end_header");
      pw.flush();

      int[] oldIdxs = new int[mesh.numVertices()];
      int idx = 0;
      for (Vertex3d vtx : mesh.getVertices()) {
          // protect vertex indices numbers
         oldIdxs[idx] = vtx.getIndex();
         vtx.setIndex(idx);
         idx++;
      }
      
      if (myDataFormat == DataFormat.ASCII) {
         writeVertexInfo (pw, vertices, normals);
      
         for (Face face : mesh.getFaces()) {
            pw.print(face.numVertices());
            for (int i=0; i<face.numVertices(); i++) {
               pw.print(" " + face.getVertex(i).getIndex());
            }
            pw.println();
         }
         pw.flush();
      }
      else {
         BinaryOutputStream bos =
            new BinaryOutputStream (new BufferedOutputStream (myOstream));
         if (myDataFormat == DataFormat.BINARY_LITTLE_ENDIAN) {
            bos.setLittleEndian (true);
         }
         writeVertexInfo (bos, vertices, normals);
      
         for (Face face : mesh.getFaces()) {
            bos.writeByte (face.numVertices());
            for (int i=0; i<face.numVertices(); i++) {
               bos.writeInt (face.getVertex(i).getIndex());
            }
         }
         bos.flush();
      }
      
      // restore vertex indices
      idx = 0;
      for (Vertex3d vtx : mesh.getVertices()) {
         vtx.setIndex(oldIdxs[idx]);
         idx++;
      }
   }

   public void writeMesh (PointMesh mesh) throws IOException {
      
      PrintWriter pw = new PrintWriter (
         new BufferedWriter (new OutputStreamWriter (myOstream)));

      ArrayList<Vertex3d> vertices = mesh.getVertices();
      ArrayList<Vector3d> normals = null;
      if (getWriteNormals(mesh)) {
         normals = mesh.getNormals();
      }
      String dsize = getFloatType().toString().toLowerCase();

      pw.println("ply");
      pw.println("format " + myDataFormat.toString().toLowerCase() + " 1.0");
      pw.println("element vertex " + mesh.numVertices());
      
      pw.println("property "+dsize+" x");
      pw.println("property "+dsize+" y");
      pw.println("property "+dsize+" z");
      if (normals != null) {
         pw.println("property "+dsize+" nx");
         pw.println("property "+dsize+" ny");
         pw.println("property "+dsize+" nz");
      }
      pw.println("element face 0");
      pw.println("property list uchar int vertex_indices");
      pw.println("end_header");
      pw.flush();

      if (myDataFormat == DataFormat.ASCII) {
         writeVertexInfo (pw, vertices, normals);
         pw.flush();
      }
      else {
         BinaryOutputStream bos =
            new BinaryOutputStream (new BufferedOutputStream (myOstream));
         if (myDataFormat == DataFormat.BINARY_LITTLE_ENDIAN) {
            bos.setLittleEndian (true);
         }
         writeVertexInfo (bos, vertices, normals);
         bos.flush();
      }
   }

   @Override
   public void writeMesh (MeshBase mesh)
      throws IOException {

      if (mesh instanceof PolygonalMesh) {
         writeMesh ((PolygonalMesh)mesh);
      }
      else if (mesh instanceof PointMesh) {
         writeMesh ((PointMesh)mesh);
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for '.ply' files");
      }     
   }

   public static void writeMesh (String fileName, MeshBase mesh)
      throws IOException {
      writeMesh (new File(fileName), mesh);
   }      

   public static void writeMesh (File file, MeshBase mesh)
      throws IOException {
      PlyWriter writer = null;
      try {
         writer = new PlyWriter(file);
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


   
}
