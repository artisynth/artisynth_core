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

   private int colorToInt (float c) {
      return (int)(255*c);
   }
   
   private void writeVertexInfo (
      PrintWriter pw,
      ArrayList<Vertex3d> vertices,
      ArrayList<Vector3d> normals,
      ArrayList<float[]> colors) {

      for (int i=0; i<vertices.size(); i++) {
         Point3d pos = vertices.get(i).getPosition();
         pw.print (myFmt.format(pos.x) + " " + 
                   myFmt.format(pos.y) + " " +
                   myFmt.format(pos.z));
         if (normals != null && i < normals.size()) {
            Vector3d nrm = normals.get(i);
            pw.print (" " + myFmt.format(nrm.x) + 
                      " " + myFmt.format(nrm.y) +
                      " " + myFmt.format(nrm.z));
         }
         if (colors != null && i < colors.size()) {
            float[] color = colors.get(i);
            pw.print (" " + colorToInt(color[0]) + 
                      " " + colorToInt(color[1]) +
                      " " + colorToInt(color[2]));
         }
         pw.println ("");
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
      ArrayList<Vertex3d> vertices, 
      ArrayList<Vector3d> normals,
      ArrayList<float[]> colors) throws IOException {

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
         if (colors != null && i < colors.size()) {
            float[] color = colors.get(i);
            bos.writeByte (colorToInt(color[0]));
            bos.writeByte (colorToInt(color[1]));
            bos.writeByte (colorToInt(color[2]));
         }
      }
   }

   public void writeMesh (PolygonalMesh mesh) throws IOException {
      
      PrintWriter pw = new PrintWriter (
         new BufferedWriter (new OutputStreamWriter (myOstream)));

      ArrayList<Vertex3d> vertices = mesh.getVertices();
      ArrayList<Vector3d> normals = null;
      ArrayList<float[]> vertexColors = null;
      ArrayList<float[]> featureColors = null;
      if (getWriteNormals (mesh)) {
         normals = mesh.getNormals();
      }
      if (getWriteColors (mesh)) {
         if (mesh.isVertexColored()) {
            vertexColors = mesh.getColors();
         }
         else if (mesh.isFeatureColored()) {
            featureColors = mesh.getColors();
         }
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
      if (vertexColors != null) {
         pw.println("property uchar red");
         pw.println("property uchar green");
         pw.println("property uchar blue");
      }
      pw.println("element face " + mesh.numFaces());
      pw.println("property list uchar int vertex_indices");
      if (featureColors != null) {
         pw.println("property uchar red");
         pw.println("property uchar green");
         pw.println("property uchar blue");
      }
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
         writeVertexInfo (pw, vertices, normals, vertexColors);

         for (int i=0; i<mesh.numFaces(); i++) {
            Face face = mesh.getFace(i);
            pw.print(face.numVertices());
            for (int k=0; k<face.numVertices(); k++) {
               pw.print(" " + face.getVertex(k).getIndex());
            }
            if (featureColors != null) {
               float[] color = featureColors.get(i);
               pw.print (" " + colorToInt(color[0]) + 
                         " " + colorToInt(color[1]) +
                         " " + colorToInt(color[2]));
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
         writeVertexInfo (bos, vertices, normals, vertexColors);
      
         for (int i=0; i<mesh.numFaces(); i++) {
            Face face = mesh.getFace(i);
            bos.writeByte (face.numVertices());
            for (int k=0; k<face.numVertices(); k++) {
               bos.writeInt (face.getVertex(k).getIndex());
            }
            if (featureColors != null) {
               float[] color = featureColors.get(i);
               bos.writeByte (colorToInt(color[0]));
               bos.writeByte (colorToInt(color[1]));
               bos.writeByte (colorToInt(color[2]));
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
      ArrayList<float[]> vertexColors = null;
      if (getWriteNormals(mesh)) {
         normals = mesh.getNormals();
      }
      if (getWriteColors (mesh)) {
         if (mesh.isVertexColored()) {
            vertexColors = mesh.getColors();
         }
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
      if (vertexColors != null) {
         pw.println("property uchar red");
         pw.println("property uchar green");
         pw.println("property uchar blue");
      }
      pw.println("element face 0");
      pw.println("property list uchar int vertex_indices");
      pw.println("end_header");
      pw.flush();

      if (myDataFormat == DataFormat.ASCII) {
         writeVertexInfo (pw, vertices, normals, vertexColors);
         pw.flush();
      }
      else {
         BinaryOutputStream bos =
            new BinaryOutputStream (new BufferedOutputStream (myOstream));
         if (myDataFormat == DataFormat.BINARY_LITTLE_ENDIAN) {
            bos.setLittleEndian (true);
         }
         writeVertexInfo (bos, vertices, normals, vertexColors);
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
