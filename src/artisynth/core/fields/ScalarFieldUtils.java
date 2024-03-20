package artisynth.core.fields;

import java.util.List;

import artisynth.core.femmodels.FemCutPlane;
import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.modelbase.RenderableComponent;
import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderObject;
import maspack.render.color.ColorMapBase;
import maspack.util.DoubleInterval;

/**
 * Utility methods for implementing ScalarFields and rendering in particular.
 */
public class ScalarFieldUtils {

   interface ScalarVertexFunction {
      double valueAt (RenderableComponent rcomp, Vertex3d vtx);
   }

   static int getColorIndex (double value, DoubleInterval range) {
      if (value <= range.getLowerBound()) {
         return 0;
      }
      else if (value >= range.getUpperBound()) {
         return 255;
      }
      else {
         return (int)(255*(value-range.getLowerBound())/range.getRange());
      }
   }

   static void addColors (RenderObject rob, ColorMapBase colorMap) {
      float[] rgb = new float[3];
      for (int i=0; i<256; i++) {
         colorMap.getRGB (i/255.0, rgb);
         rob.addColor (rgb);
      }
   }

   static RenderObject buildMeshRenderObject (
      List<? extends RenderableComponent> rcomps, ColorMapBase colorMap,
      DoubleInterval range, ScalarVertexFunction vfunc) {

      RenderObject rob = new RenderObject();
      // create a triangle group for each mesh
      for (int mid=0; mid<rcomps.size(); mid++) {
         rob.createTriangleGroup();
      }
      // add the colors 
      addColors (rob, colorMap);

      int mid = 0; // mesh id
      for (RenderableComponent rcomp : rcomps) {
         int pbase = rob.numPositions();
         PolygonalMesh mesh = null;
         if (rcomp instanceof MeshComponent) {
            MeshComponent mcomp = (MeshComponent)rcomp;
            if (mcomp.isMeshPolygonal()) {
               mesh = (PolygonalMesh)mcomp.getMesh();
            }
         }
         else if (rcomp instanceof FemCutPlane) {
            mesh = ((FemCutPlane)rcomp).getMesh();
         }
         if (mesh != null) {
            // add the positions  and find the color index for each vertex
            Point3d wpnt = new Point3d();
            int[] cidxs = new int[mesh.numVertices()];
            int i = 0;
            for (Vertex3d vtx : mesh.getVertices()) {
               vtx.getWorldPoint (wpnt);
               rob.addPosition (wpnt);
               double val = vfunc.valueAt (rcomp, vtx);
               cidxs[i++] = getColorIndex (val, range);
            }
            // add faces to render object
            Vector3d nrm = new Vector3d();
            rob.triangleGroup (mid);
            for (Face face : mesh.getFaces()) {
               int[] pidxs = face.getVertexIndices();
               face.getWorldNormal (nrm);
               // add triangles. 
               int nidx = rob.addNormal (
                  (float)nrm.x, (float)nrm.y, (float)nrm.z);
               for (i=1; i<pidxs.length-1; i++) {
                  int v0idx = rob.addVertex (
                     pbase+pidxs[0], nidx, cidxs[pidxs[0]], -1);
                  int v1idx = rob.addVertex (
                     pbase+pidxs[i], nidx, cidxs[pidxs[i]], -1);
                  int v2idx = rob.addVertex (
                     pbase+pidxs[i+1], nidx, cidxs[pidxs[i+1]], -1);
                  rob.addTriangle (v0idx, v1idx, v2idx);     
               }
            }
         }
         mid++;
      }
      return rob;
   }

}
