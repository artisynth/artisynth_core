package maspack.geometry.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.geometry.MeshBase;
import maspack.matrix.Point3d;
import maspack.util.ReaderTokenizer;

public class GtsReader extends MeshReaderBase {

   public GtsReader (InputStream is) throws IOException {
      super (is);
   }

   public GtsReader (File file) throws IOException {
      super (file);
   }

   public GtsReader (String fileName) throws IOException {
      this (new File(fileName));
   }

   static class Edge {

      Vertex3d myVtx1;
      Vertex3d myVtx2;

      Edge (Vertex3d vtx1, Vertex3d vtx2) {
         myVtx1 = vtx1;
         myVtx2 = vtx2;
      }

      public int hashCode() {
         return (myVtx1.hashCode()/2 + myVtx2.hashCode()/2);
      }

      public boolean equals (Object obj) {
         if (!(obj instanceof Edge)) {
            return false;
         }
         Edge edge = (Edge)obj;
         return ((edge.myVtx1 == myVtx1 && edge.myVtx2 == myVtx2) ||
            (edge.myVtx1 == myVtx2 && edge.myVtx2 == myVtx1));
      }
   };

   private static int[] getFaceIndices (ArrayList<Edge> edges) {
      int[] idxs = new int[edges.size()];
      Edge prev = edges.get(edges.size()-1);
      for (int i=0; i<edges.size(); i++) {
         Edge edge = edges.get(i);
         if (edge.myVtx1 == prev.myVtx1 || edge.myVtx1 == prev.myVtx2) {
            idxs[i] = edge.myVtx1.getIndex();
         }
         else if (edge.myVtx2 == prev.myVtx1 || edge.myVtx2 == prev.myVtx2) {
            idxs[i] = edge.myVtx2.getIndex();
         }
         else {
            return null;
         }
         prev = edge;
      }
      return idxs;
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
            "Mesh type "+mesh.getClass()+" not supported for '.gts' files");
      }
   }

   public PolygonalMesh readMesh (PolygonalMesh mesh)
      throws IOException {

      if (mesh == null) {
         mesh = new PolygonalMesh();
      } else {
         mesh.clear();
      }

      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (
            new InputStreamReader(myIstream)));

      rtok.commentChar ('!');
      rtok.commentChar ('#');
      rtok.eolIsSignificant(true);

      int numVerts = rtok.scanInteger();
      int numEdges = rtok.scanInteger();
      int numFaces = rtok.scanInteger();
      while (rtok.nextToken() != ReaderTokenizer.TT_EOL)
         ;

      ArrayList<Edge> edgeList = new ArrayList<Edge>();


      for (int i=0; i<numVerts; i++) {
         double x = rtok.scanNumber();
         double y = rtok.scanNumber();
         double z = rtok.scanNumber();
         mesh.addVertex (new Point3d (x, y, z));
         while (rtok.nextToken() != ReaderTokenizer.TT_EOL)
            ;
      }

      ArrayList<Vertex3d> verts = mesh.getVertices();
      for (int i=0; i<numEdges; i++) {
         int idx1 = rtok.scanInteger()-1;
         int idx2 = rtok.scanInteger()-1;
         if (idx1 < 0 || idx1 >= numVerts) {
            System.out.println ("Error: vertex "+idx1+" out of range, "+rtok);
            System.exit(1); 
         }
         if (idx2 < 0 || idx2 >= numVerts) {
            System.out.println ("Error: vertex "+idx2+" out of range, "+rtok);
            System.exit(1); 
         }
         edgeList.add (
            new Edge (verts.get(idx1), verts.get(idx2)));
         while (rtok.nextToken() != ReaderTokenizer.TT_EOL)
            ;
      }

      for (int i=0; i<numFaces; i++) {
         ArrayList<Edge> edges = new ArrayList<Edge>();
         while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
            if (!rtok.tokenIsInteger()) {
               System.out.println ("Error: edge index expected, "+rtok);
               System.exit(1); 
            }
            int idx = (int)rtok.lval - 1;
            if (idx < 0 || idx >= edgeList.size()) {
               System.out.println (
                  "Error: edge index "+idx+" out of range, "+rtok);
               System.exit(1); 
            }
            edges.add (edgeList.get(idx));
         }
         int[] idxs = getFaceIndices (edges);
         if (idxs == null) {
            System.out.println (
               "Error: face edges are not adjacent, " + rtok);
         }
         mesh.addFace (idxs);
         rtok.pushBack();
         while (rtok.nextToken() != ReaderTokenizer.TT_EOL)
            ;
      }
      return mesh;
   }

   public static PolygonalMesh read (File file) throws IOException {
      GtsReader reader = null;
      try {
         reader = new GtsReader(file);
         return (PolygonalMesh)reader.readMesh ();
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


