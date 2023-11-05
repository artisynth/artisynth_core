package artisynth.core.fields;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.LinkedList;

import artisynth.core.femmodels.FemElement3dBase;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.*;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.FemFieldPoint;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.mechmodels.*;
import artisynth.core.util.ScanToken;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.util.DynamicBooleanArray;
import maspack.util.DynamicDoubleArray;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.DoubleInterval;
import maspack.render.*;
import maspack.util.DoubleInterval;
import maspack.util.*;
import maspack.geometry.*;
import maspack.properties.*;
import maspack.render.Renderer.*;
import maspack.render.color.ColorMapBase;

/**
 * Utility methods for implementing ScalarFields and rendering in particular.
 */
public class ScalarFieldUtils {

   interface ScalarVertexFunction {
      double valueAt (MeshComponent mcomp, Vertex3d vtx);
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
      List<? extends MeshComponent> mcomps, ColorMapBase colorMap,
      DoubleInterval range, ScalarVertexFunction vfunc) {

      RenderObject rob = new RenderObject();
      // create a triangle group for each mesh
      for (int mid=0; mid<mcomps.size(); mid++) {
         rob.createTriangleGroup();
      }
      // add the colors 
      addColors (rob, colorMap);

      int mid = 0; // mesh id
      for (MeshComponent mcomp : mcomps) {
         int pbase = rob.numPositions();
         if (mcomp.isMeshPolygonal()) {
            // mesh should be polygonal; just being careful
            PolygonalMesh mesh = (PolygonalMesh)mcomp.getMesh();
            // add the positions  and find the color index for each vertex
            Point3d wpnt = new Point3d();
            int[] cidxs = new int[mesh.numVertices()];
            int i = 0;
            for (Vertex3d vtx : mesh.getVertices()) {
               vtx.getWorldPoint (wpnt);
               rob.addPosition (wpnt);
               double val = vfunc.valueAt (mcomp, vtx);
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
