package maspack.geometry.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedHashMap;

import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.geometry.io.GtsReader.Edge;

public class GtsWriter extends MeshWriterBase {

   public GtsWriter (OutputStream os) throws IOException{
      super (os);
      setFormat ("%.10g");
   }

   public GtsWriter (File file) throws IOException {
      super (file);
      setFormat ("%.10g");
   }

   public GtsWriter (String fileName) throws IOException {
      this (new File(fileName));
   }

   public void writeMesh (PolygonalMesh mesh) throws IOException {
      
      PrintWriter pw = 
         new PrintWriter (
            new BufferedWriter (new OutputStreamWriter (myOstream)));
      
      int[] oldIdxs = new int[mesh.numVertices()];
      int vidx = 0;
      for (Vertex3d vtx : mesh.getVertices()) {
          // protect vertex indices numbers
         oldIdxs[vidx] = vtx.getIndex();
         vtx.setIndex(vidx);
         vidx++;
      }
      
      LinkedHashMap<Edge,Integer> edgeList =
         new LinkedHashMap<Edge,Integer>();
      for (Face f : mesh.getFaces()) {
         HalfEdge he0 = f.firstHalfEdge();
         HalfEdge he = he0;
         do {
            HalfEdge next = he.getNext();
            Edge edge = new Edge (he.head, next.head);
            if (edgeList.get (edge) == null) {
               edgeList.put (edge, edgeList.size());
            }
            he = next;
         }
         while (he != he0);
      }
      pw.println (
         mesh.numVertices()+" "+edgeList.size()+" "+mesh.numFaces());
      for (Vertex3d vtx : mesh.getVertices()) {
         vtx.pnt.write (pw, myFmt);
         pw.println ("");
      }
      for (Edge edge : edgeList.keySet()) {
         pw.println ((edge.myVtx1.getIndex()+1)+" "+(edge.myVtx2.getIndex()+1));
      }
      for (Face f : mesh.getFaces()) {
         HalfEdge he0 = f.firstHalfEdge();
         HalfEdge he = he0;
         do {
            HalfEdge next = he.getNext();
            Edge edge = new Edge (he.head, next.head);
            Integer idx = edgeList.get (edge);
            pw.print ((idx+1)+" ");
            he = next;
         }
         while (he != he0);
         pw.println ("");
      }
      
      // restore vertex indices
      vidx = 0;
      for (Vertex3d vtx : mesh.getVertices()) {
         vtx.setIndex(oldIdxs[vidx]);
         vidx++;
      }
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
            "Mesh type "+mesh.getClass()+" not supported for '.gts' files");
      }     
   }

}
