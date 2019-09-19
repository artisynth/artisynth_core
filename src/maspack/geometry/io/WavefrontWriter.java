package maspack.geometry.io;

import java.io.*;
import java.util.*;

import maspack.geometry.*;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.NumberFormat;

/**
 * Writes meshes to an Alias Wavefront .obj file format
 * @author John Lloyd, Jan 2014
 *
 */
public class WavefrontWriter extends MeshWriterBase {
   
   boolean myFacesClockwise = false;
   boolean myZeroIndexed = false;
   int vOffset = 0;

   public boolean getFacesClockwise () {
      return myFacesClockwise;
   }

   public void setFacesClockwise (boolean enable) {
      myFacesClockwise = enable;
   }

   public boolean getZeroIndexed () {
      return myZeroIndexed;
   }

   public void setZeroIndexed (boolean enable) {
      myZeroIndexed = enable;
   }

   public WavefrontWriter (OutputStream os) throws IOException{
      super (os);
      setFormat ("%.10g");
   }

   public WavefrontWriter (File file) throws IOException {
      super (file);
      setFormat ("%.10g");
   }

   public WavefrontWriter (String fileName) throws IOException {
      this (new File(fileName));
   }

   public void writeString(String str) throws IOException {
      PrintWriter pw =  new PrintWriter (       
         new BufferedWriter (new OutputStreamWriter (myOstream)));
      pw.write (str);
      pw.flush ();
   }
   
   public void writeMesh (MeshBase mesh) throws IOException {

      PrintWriter pw =
         new PrintWriter (
            new BufferedWriter (new OutputStreamWriter (myOstream)));

      if (mesh instanceof PolygonalMesh) {
         writeMesh (pw, (PolygonalMesh)mesh);
      }
      else if (mesh instanceof PolylineMesh) {
         writeMesh (pw, (PolylineMesh)mesh);
      }
      else if (mesh instanceof PointMesh) {
         writeMesh (pw, (PointMesh)mesh);
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for '.obj' files");
      }
   }

   /**
    * Writes a PolygonalMesh to a PrintWriter, using an Alias Wavefront "obj"
    * file format. Vertices are printed first, each starting with the letter
    * "v" and followed by x, y, and z coordinates. Faces are printed next,
    * starting with the letter "f" and followed by a list of integers which
    * gives the indices of that face's vertices. Unless {@link
    * #getZeroIndexed()} returns <code>true</code>, these indices are
    * 1-based, and unless {@link #getFacesClockwise()} returns
    * <code>true</code>, they are printed in counter-clockwise order. For
    * example, a mesh consisting of a simple tetrahedron might be written like
    * this:
    * 
    * <pre>
    *    v 0.0 0.0 0.0
    *    v 1.0 0.0 0.0
    *    v 0.0 1.0 0.0
    *    v 0.0 0.0 1.0
    *    f 1 2 3
    *    f 0 2 1
    *    f 0 3 2
    *    f 0 1 3
    * </pre>
    * 
    * <p>
    * The format used to print the vertex, normal, and texture coordinates can be
    * controlled by
    * {@link #setFormat(String)} or {@link #setFormat(NumberFormat)}.
    * The default format has eight decimal places and is specified
    * by the string <code>"%.8g"</code>.
    * 
    * @param pw
    * PrintWriter to write the mesh to
    * @param mesh
    * PolygonalMesh to be written.
    */   
    public void writeMesh (PrintWriter pw, PolygonalMesh mesh) throws IOException {

      ArrayList<Vertex3d> vertices = mesh.getVertices();
      ArrayList<Vector3d> normals = null;
      int[] normalIndices = mesh.getNormalIndices();
      int[] indexOffs = mesh.getFeatureIndexOffsets();
      ArrayList<Vector3d> textureCoords = mesh.getTextureCoords();
      int[] textureIndices = mesh.getTextureIndices();
      ArrayList<Face> faces = mesh.getFaces();

      boolean writeTextureInfo = textureCoords != null;
      if (getWriteNormals (mesh)) {
         normals = mesh.getNormals();
      }

      int[] oldIdxs = new int[vertices.size()];
      int idx = 0;
      for (Vertex3d vertex : vertices) {
         Point3d pnt = vertex.pnt;
         pw.println (
            "v " + myFmt.format (pnt.x) + " " + myFmt.format (pnt.y) + " " +
            myFmt.format (pnt.z));
         
         // save old vertex index
         oldIdxs[idx] = vertex.getIndex();
         vertex.setIndex(idx);
         idx++;
      }
      if (normals != null) {
         for (Vector3d vn : normals) {
            pw.println (
               "vn " + myFmt.format (vn.x) + " " + myFmt.format (vn.y) +
               " " + myFmt.format (vn.z));
         }
      }
      if (writeTextureInfo) {
         for (Vector3d vt : textureCoords) {
            pw.println (
               "vt " + myFmt.format (vt.x) + " " + myFmt.format (vt.y) +
               " " + myFmt.format (vt.z));
         }
      }
      int faceCnt = 0;
      for (Face face : faces) {
         pw.print ("f");
         Vertex3d[] vtxList = new Vertex3d[face.numEdges()];
         int k = 0;
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         do {
            vtxList[k++] = he.head;
            he = he.getNext();
         }
         while (he != he0);
         if (myFacesClockwise) {
            // reverse vertex list
            for (k=1; k<=(vtxList.length-1)/2; k++) {
               int l = vtxList.length-k;
               Vertex3d tmp = vtxList[l];
               vtxList[l] = vtxList[k];
               vtxList[k] = tmp;
            }
         }
         int foff = indexOffs[faceCnt];
         for (k=0; k<vtxList.length; k++) {
            Vertex3d vtx = vtxList[k];
            if (myZeroIndexed) {
               pw.print (" " + (vtx.getIndex()+vOffset));
               if (writeTextureInfo) {
                  pw.print ("/" + textureIndices[foff+k]);
               }
               if (normals != null) {
                  if (!writeTextureInfo) {
                     pw.print ("/");
                  }
                  pw.print ("/" + normalIndices[foff+k]);
               }
            }
            else {
               pw.print (" " + (vtx.getIndex() + 1 + vOffset));
               if (writeTextureInfo) {
                  pw.print ("/" + (textureIndices[foff+k]+1));
               }
               if (normals != null) {
                  if (!writeTextureInfo) {
                     pw.print ("/");
                  }
                  pw.print ("/" + (normalIndices[foff+k]+1));
               }
            }
         }
         pw.println ("");
         faceCnt++;
      }
      pw.flush();
      
      // restore vertex indices
      idx = 0;
      for (Vertex3d vertex : vertices) {
         vertex.setIndex(oldIdxs[idx]);
         idx++;
      }
      vOffset += vertices.size();
   }

   /**
    * Writes a PolylineMesh to a PrintWriter, using an Alias Wavefront "obj"
    * file format. Vertices are printed first, each starting with the letter
    * "v" and followed by x, y, and z coordinates. Lines are printed next,
    * starting with the letter "f" and followed by a list of integers which
    * gives the indices of that line's vertices. Unless {@link
    * #getZeroIndexed()} returns <code>true</code>, these indices are
    * 1-based. An example of a simple three point line is:
    * 
    * <pre>
    *    v 1.0 0.0 0.0
    *    v 0.0 1.0 0.0
    *    v 0.0 0.0 1.0
    *    l 0 1 2
    * </pre>
    * 
    * <p>
    * The format used to print the vertex coordinates can be
    * controlled by
    * {@link #setFormat(String)} or {@link #setFormat(NumberFormat)}.
    * The default format has eight decimal places and is specified
    * by the string <code>"%.8g"</code>.
    * 
    * @param pw
    * PrintWriter to write the mesh to
    * @param mesh
    * PolylineMesh to be written
    */
   public void writeMesh (PrintWriter pw, PolylineMesh mesh) throws IOException {

      ArrayList<Vertex3d> vertices = mesh.getVertices();
      ArrayList<Polyline> lines = mesh.getLines();

      for (Vertex3d vertex : vertices) {
         Point3d pnt = vertex.pnt;
         pw.println (
            "v " + myFmt.format (pnt.x) + " " + myFmt.format (pnt.y) + " " +
            myFmt.format (pnt.z));
      }
      // int lineCnt = 0;
      for (Polyline line : lines) {
         // int idxCnt = 0;
         pw.print ("l");
         int[] idxs = line.getVertexIndices();
         for (int i=0; i<idxs.length; i++) {
            if (myZeroIndexed) {
               pw.print (" " + (idxs[i]+vOffset));
            }
            else {
               pw.print (" " + (idxs[i] + 1+vOffset));
            }
         }
         pw.println ("");
      }
      pw.flush();
      
      vOffset += vertices.size();
   }

   /**
    * Writes a PointMesh to a PrintWriter, using an Alias Wavefront "obj" file
    * format. Vertices are printed first, each starting with the letter "v" and
    * followed by x, y, and z coordinates. Normals, if present, are printed
    * next, starting with the letter "vn" and followed by x, y, and z
    * coordinates.
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
   public void writeMesh (PrintWriter pw, PointMesh mesh) throws IOException {

      ArrayList<Vertex3d> vertices = mesh.getVertices();
      ArrayList<Vector3d> normals = null;
      if (getWriteNormals (mesh)) {
         normals = mesh.getNormals();
      }      
      
      for (Vertex3d vertex : vertices) {
         Point3d pnt = vertex.pnt;
         pw.println (
            "v " + myFmt.format (pnt.x) + " " + myFmt.format (pnt.y) + " " +
            myFmt.format (pnt.z));
      }
      if (normals != null) {
         for (Vector3d nrm : normals) {
            pw.println (
               "vn " + myFmt.format (nrm.x) + " " + myFmt.format (nrm.y) + " " +
               myFmt.format (nrm.z));
         }
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

   public static void writeMesh (String fileName, MeshBase mesh)
      throws IOException {
      writeMesh (new File(fileName), mesh);
   }      

   public static void writeMesh (File file, MeshBase mesh)
      throws IOException {
      WavefrontWriter writer = null;
      try {
         writer = new WavefrontWriter(file);
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
