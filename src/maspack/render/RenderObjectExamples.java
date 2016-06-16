package maspack.render;

import java.util.HashMap;
import java.util.List;

import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.Renderer.DrawMode;

public class RenderObjectExamples {
   
   private static HashMap<Object,RenderObject> shared = new HashMap<> ();

   /**
    * Example showing basic construction, but advanced usage in the form
    * of multiple color buffers.
    */
   public static void example1(Renderer renderer, int selectedAxis) {
      
      RenderObject axes = shared.get("axes");
      
      // create if doesn't exist
      if (axes == null) {
         axes = new RenderObject();  // constructor
      
         byte[] red    = new byte[]{(byte)255,(byte)0,  (byte)0,  (byte)255};
         byte[] green  = new byte[]{(byte)0,  (byte)255,(byte)0,  (byte)255};
         byte[] blue   = new byte[]{(byte)0,  (byte)0,  (byte)255,(byte)255};
         // byte[] yellow = new byte[]{(byte)255,(byte)255,(byte)0,  (byte)255};
         float[] origin = new float[]{0,0,0};
         float[] xunit = new float[]{1,0,0};
         float[] yunit = new float[]{0,1,0};
         float[] zunit = new float[]{0,0,1};
         
         int cx = axes.addColor(red);
         int cy = axes.addColor(green);
         int cz = axes.addColor(blue);
         
         // regular axes
         axes.setCurrentColor(cx);
         axes.addLine(origin, xunit);
         axes.setCurrentColor(cy);
         axes.addLine(origin, yunit);
         axes.setCurrentColor(cz);
         axes.addLine(origin, zunit);
         
         shared.put ("axes", axes);
      }
            
      // <apply transform>
      // draw axes
      renderer.draw(axes);
      
   }
   
   /**
    * Example showing a simplified mesh renderer 
    */
   public static void example2(Renderer renderer, PolygonalMesh mesh, boolean drawEdges, boolean drawVertices) {
      
      RenderObject r = new RenderObject();
      
      int[] nidxs = null;
      int[] cidxs = null;
      int[] tidxs = null;

      // add all appropriate info
      for (Vertex3d vtx : mesh.getVertices()) {
         Point3d pos = vtx.getPosition();
         r.addPosition((float)pos.x, (float)pos.y, (float)pos.z);
      }
      if (mesh.getNormals() != null) {
         for (Vector3d nrm : mesh.getNormals()) {
            r.addNormal((float)nrm.x, (float)nrm.y, (float)nrm.z);
         }
         nidxs = mesh.getNormalIndices();
      }
      if (mesh.getColors() != null) {
         for (float[] color : mesh.getColors()) {
            r.addColor(color);
         }
         cidxs = mesh.getColorIndices();
      }
      if (mesh.getTextureCoords() != null) {
         for (Vector3d texCoord : mesh.getTextureCoords()) {
            // according to existing MeshRenderer, we need to flip y
            r.addTextureCoord((float)texCoord.x, (float)(1-texCoord.y));
         }
         tidxs = mesh.getTextureIndices();
      }
      
      // build faces
      int[] indexOffs = mesh.getFeatureIndexOffsets();
      List<Face> faces = mesh.getFaces();
      for (int i=0; i<faces.size(); i++) {
         Face f = faces.get(i);
         int foff = indexOffs[f.idx];
         
         int[] pidxs = f.getVertexIndices();
         
         int[] vidxs = new int[pidxs.length]; // vertex indices
         for (int j=0; j<pidxs.length; j++) {
            vidxs[j] = r.addVertex(
               pidxs[j], 
               nidxs != null ? nidxs[foff+j] : -1,
               cidxs != null ? cidxs[foff+j] : -1,
               tidxs != null ? tidxs[foff+j] : -1);
         }
         
         // triangle fan for faces, line loop for edges
         r.addTriangleFan(vidxs);
         if (drawEdges) {
            r.addLineLoop(vidxs);
         }
         
      }
      
      // draw mesh
      // <set face color and maybe polygon offset>
      // draw faces
      renderer.drawTriangles(r);
      if (drawEdges) {
         // <set edge color>
         // draw edges
         renderer.drawLines(r);
      }
      if (drawVertices) {
         // <set vertex color>
         // Rather than set up a set of point primitives, which would result
         // in an extra index array in corresponding VBOs, draw the vertex
         // array directly with an appropriate mode.
         renderer.drawVertices(r, DrawMode.POINTS);  // draw all vertices as points
         
         // less-efficient alternative:
         // for (int i=0; i<r.numVertices(); i++) {
         //    r.addPoint(i);
         // }
         // renderer.drawPoints(r);
      }
      
   }
   
}
