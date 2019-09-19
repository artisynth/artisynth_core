package maspack.geometry.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshBase;
import maspack.util.ReaderTokenizer;

/**
 * Reads a polygonal mesh from the OpenSim ASC format
 * 
 * New ASCII: NORM_ASCII nVert nFaces bounding box vx vy vz nx ny nz (vertex
 * coordinates) ... nv n1 n2 n3 (face vertex index list, 0 indexed) ...
 * 
 * Old ASCII: nVert nFaces vx vy vz ... nv n1 n2 n3 (1 indexed)
 * 
 * Binary:
 * 
 * @author antonio
 */

public class OpenSimAscReader extends MeshReaderBase {

   public OpenSimAscReader (InputStream is) throws IOException {
      super (is);
   }

   public OpenSimAscReader (File file) throws IOException {
      super (file);
   }

   public OpenSimAscReader (String fileName) throws IOException {
      this (new File(fileName));
   }

   public PolygonalMesh readMesh (PolygonalMesh mesh)
      throws IOException {
      
      if (mesh == null) {
         mesh = new PolygonalMesh();
      } else {
         mesh.clear();
      }
      
      ReaderTokenizer rtok = new ReaderTokenizer(
         new InputStreamReader(myIstream));
      
      rtok.eolIsSignificant(false);
      boolean oldFormat = false;

      int nVertices = 0;
      int nFaces = 0;
      double[] tmp = new double[6];
      int nReadVals = 6;

      // read first symbol, which should be NORM_ASCII or a number of vertices
      if (rtok.nextToken() != ReaderTokenizer.TT_WORD) {

         if (rtok.ttype == ReaderTokenizer.TT_NUMBER) {
            oldFormat = true;
            nReadVals = 3;
            nVertices = (int)rtok.nval;
         } else {
            throw new IOException("Expected " + ReaderTokenizer.TT_WORD +
               " on line " + rtok.lineno());
         }
      }

      if (!oldFormat) {
         String type = rtok.sval;
         if (type.compareTo("NORM_ASCII") != 0) {
            throw new IOException("Unknown file type: " + type);
         }

         if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER) {
            throw new IOException("Expected " + ReaderTokenizer.TT_NUMBER +
               " on line " + rtok.lineno());
         }
         nVertices = (int)rtok.nval;
      }

      if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER) {
         throw new IOException("Expected " + ReaderTokenizer.TT_NUMBER +
            " on line " + rtok.lineno());
      }
      nFaces = (int)rtok.nval;

      if (!oldFormat) {
         int iRead = rtok.scanNumbers(tmp, 6); // bounding box
         if (iRead != 6) {
            throw new IOException("Expected bounding box on line "
               + rtok.lineno());
         }
      }

      // load all vertices now
      for (int i = 0; i < nVertices; i++) {
         int nRead = rtok.scanNumbers(tmp, nReadVals);
         if (nRead != nReadVals) {
            throw new IOException("Expected number of values on line "
               + rtok.lineno());
         }
         mesh.addVertex(tmp[0], tmp[1], tmp[2]);
      }

      // load all faces
      for (int i = 0; i < nFaces; i++) {
         int nV = (int)rtok.scanNumber();
         int[] face = new int[nV];
         if (scanIntegers(rtok, face, nV) != nV) {
            throw new IOException("Expected number of vertices on line "
               + rtok.lineno());
         }

         if (oldFormat) {
            for (int j = 0; j < nV; j++) {
               face[j] -= 1; // start indices at zero
            }
         }

         mesh.addFace(face);
      }
      
      return mesh;

   }

   private static int scanIntegers(ReaderTokenizer rt, int[] vals, int max)
      throws IOException {
      for (int i = 0; i < max; i++) {
         rt.nextToken();
         if (rt.ttype == ReaderTokenizer.TT_NUMBER) {
            vals[i] = (int)rt.nval;
         }
         else {
            return i;
         }
      }
      return max;
   }

   @Override
   public PolygonalMesh readMesh() throws IOException {
      return (PolygonalMesh)readMesh (new PolygonalMesh());
   }

   public MeshBase readMesh (MeshBase mesh) throws IOException {
      if (mesh instanceof PolygonalMesh) {
         return readMesh ((PolygonalMesh)mesh);
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for OpenSim files");
      }
   }

   public static PolygonalMesh read (File file) throws IOException {
      OpenSimAscReader reader = new OpenSimAscReader (file);
      return (PolygonalMesh)reader.readMesh (new PolygonalMesh());
   }

   public static PolygonalMesh read (String fileName) throws IOException {
      return read (new File(fileName));
   }


}
