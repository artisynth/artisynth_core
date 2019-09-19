package maspack.geometry.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedReader;
import java.util.ArrayList;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshBase;
import maspack.matrix.Point3d;
import maspack.util.ReaderTokenizer;

/**
 * Reads from ascii OFF format
 * @author Antonio
 *
 */
public class OffReader extends MeshReaderBase {
   
   public OffReader (InputStream is) throws IOException {
      super (is);
   }

   public OffReader (File file) throws IOException {
      super (file);
   }

   public OffReader (String fileName) throws IOException {
      this (new File(fileName));
   }

   public static PolygonalMesh read(PolygonalMesh mesh, Reader reader) throws IOException {
      
      ReaderTokenizer rtok = new ReaderTokenizer(reader);
      ArrayList<Point3d> nodeList = new ArrayList<Point3d>();
      ArrayList<ArrayList<Integer>> faceList = new ArrayList<ArrayList<Integer>>();
      
      rtok.eolIsSignificant(false);
      
      int nVerts = 0;
      int nFaces = 0;
      int nEdges = 0;
      
      // read first info
      rtok.nextToken();
      if (rtok.ttype != ReaderTokenizer.TT_WORD || !rtok.sval.equalsIgnoreCase("OFF")) {
         throw new IOException("Expected 'OFF' at start of file");
      }
      nVerts = rtok.scanInteger();
      nFaces = rtok.scanInteger();
      nEdges = rtok.scanInteger();
      
      if (nEdges != 0) {
         System.err.println("Separate edges not supported\n");
      }
      
      double[] vals =  new double[3];
      
      for (int i=0; i<nVerts; i++) {
         int nread = rtok.scanNumbers(vals, 3);
         if (nread != 3) {
            throw new IOException("Failed to read vertices");
         }
         Point3d pnt = new Point3d(vals[0], vals[1], vals[2]);
         nodeList.add(pnt);
      }
      
      for (int i=0; i<nFaces; i++) {
         int nv = rtok.scanInteger();
         ArrayList<Integer> vtxs = new ArrayList<Integer>(nv);
         for (int j=0; j<nv; j++) {
            int vIdx = rtok.scanInteger();
            vtxs.add(vIdx);
         }
         faceList.add(vtxs);
      }
      return buildMesh(mesh, nodeList, faceList);
        
   }
   
   private static PolygonalMesh buildMesh(PolygonalMesh mesh, ArrayList<Point3d> nodes, ArrayList<ArrayList<Integer>> faces) {
      
      if (mesh == null) {
         mesh = new PolygonalMesh();
      } else {
         mesh.clear();
      }
      
      Point3d[] pnts = new Point3d[nodes.size()];
      int[][] faceIndices = new int[faces.size()][];
      for (int i=0; i<nodes.size(); i++) {
         pnts[i] = nodes.get(i);
      }
      
      ArrayList<Integer> face;
      for (int i=0; i<faces.size(); i++) {
         face = faces.get(i);
         faceIndices[i] = new int[face.size()];
         for (int j=0; j<face.size(); j++) {
            faceIndices[i][j] = face.get(j);
         }
      }
      mesh.set(pnts, faceIndices);
      
      return mesh;
   }

   @Override
   public PolygonalMesh readMesh() throws IOException {
      return (PolygonalMesh)readMesh (new PolygonalMesh());
   }

   public PolygonalMesh readMesh (MeshBase mesh) throws IOException {
      if (mesh == null) {
         mesh = new PolygonalMesh();
      }
      if (mesh instanceof PolygonalMesh) {
         BufferedReader iread = 
            new BufferedReader (new InputStreamReader(myIstream));
         return read ((PolygonalMesh)mesh, iread);
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for '.off' files");
      }
   }
   
   public static PolygonalMesh read (File file) throws IOException {
      OffReader reader = null;
      try {
         reader = new OffReader (file);
         return (PolygonalMesh)reader.readMesh (null);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (reader != null) {
            reader.close();
         }
      }      
    }

   public static PolygonalMesh read (String fileName) throws IOException {
      return read (new File(fileName));
    }

}
