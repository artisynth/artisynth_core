package maspack.render;

import java.util.HashMap;
import java.util.List;

import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderObject.BuildMode;

public class RenderObjectFactory {

   /**
    * Creates a RenderObject from a PolygonalMesh, with one group of triangle primitives for faces.
    * Non-triangular faces are replaced by a triangle fan.
    * @param mesh 
    * @param flatNormals use "flat" shading normals
    * @param addEdges add edge primitives
    * @return
    */
   public static RenderObject createFromMesh(PolygonalMesh mesh, boolean flatNormals, boolean addEdges) {

      RenderObject r = new RenderObject();

      // add all appropriate info
      for (Vertex3d vtx : mesh.getVertices()) {
         Point3d pos = vtx.getPosition();
         r.addPosition((float)pos.x, (float)pos.y, (float)pos.z);
      }
      if (!flatNormals) {
         if (mesh.hasNormals()) {
            for (Vector3d nrm : mesh.getNormalList()) {
               r.addNormal((float)nrm.x, (float)nrm.y, (float)nrm.z);
            }
         }
      } else {
         for (Face f : mesh.getFaces()) {
            Vector3d nrm = new Vector3d();
            f.computeNormal(nrm);
            r.addNormal((float)nrm.x, (float)nrm.y, (float)nrm.z);
         }
      }
      if (mesh.hasColors()) {
         for (byte[] color : mesh.getColorList()) {
            r.addColor(color);
         }
      }
      if (mesh.hasTextureCoords()) {
         for (Vector3d texCoord : mesh.getTextureCoordList()) {
            // according to existing MeshRenderer, we need to flip y
            r.addTextureCoord((float)texCoord.x, (float)(1-texCoord.y));
         }
      }

      // keep a map of unique vertices to reduce storage requirements
      HashMap<RenderObject.VertexIndexSet,Integer> uniqueVerts = new HashMap<>(); 

      // build faces
      List<Face> faces = mesh.getFaces();
      final int[] invalid = new int[] {-1}; 
      for (int i=0; i<faces.size(); i++) {
         Face f = faces.get(i);

         int[] pidxs = f.getVertexIndices();
         int[] nidxs = invalid;
         int[] cidxs = invalid;
         int[] tidxs = invalid;

         if (!flatNormals) {
            if (mesh.hasNormals()) {
               nidxs = mesh.getNormalIndices().get(i);
            }
         } else {
            nidxs = new int[]{f.getIndex()};
         }
         if (mesh.hasColors()) {
            cidxs = mesh.getColorIndices().get(i);
         }
         if (mesh.hasTextureCoords()) {
            tidxs = mesh.getTextureIndices().get(i);
         }

         int[] vidxs = new int[pidxs.length]; // vertex indices
         for (int j=0; j<pidxs.length; j++) {

            // only add if unique combination
            RenderObject.VertexIndexSet v = new RenderObject.VertexIndexSet(
               pidxs[j], 
               nidxs[j%nidxs.length], // account for invalid
               cidxs[j%cidxs.length], 
               tidxs[j%tidxs.length]);
            Integer vidx = uniqueVerts.get(v);
            if (vidx != null) {
               vidxs[j] = vidx.intValue();
            } else {
               vidxs[j] = r.addVertex(v.pidx, v.nidx, v.cidx, v.tidx);
               uniqueVerts.put(v, vidxs[j]);
            }
         }

         // triangle fan for faces, line loop for edges
         r.addTriangleFan(vidxs);
         if (addEdges) {
            r.addLineLoop(vidxs);
         }

      }

      // r.commit();  // finalize construction
      return r;
   }

   /**
    * Creates a unit cylinder along the z-axis
    * @param nSlices angular resolution
    * @param capped include top/bottom caps
    * @return
    */
   public static RenderObject createCylinder(int nSlices, boolean capped) {

      RenderObject cylinder = new RenderObject();
      
      // reserve memory
      cylinder.ensurePositionCapacity(2*nSlices);
      if (capped) {
         cylinder.ensureNormalCapacity(nSlices+2);
         cylinder.ensureVertexCapacity(4*nSlices);  // top/bottom sides, top/bottom caps
         cylinder.ensureTriangleCapacity(2*nSlices+2*(nSlices-2));
      } else {
         cylinder.ensureNormalCapacity(nSlices);
         cylinder.ensureVertexCapacity(2*nSlices);  // top/bottom sides
         cylinder.ensureTriangleCapacity(2*nSlices);
      }
      
      // sides
      cylinder.beginBuild(BuildMode.TRIANGLE_STRIP);
      for (int i=0; i<nSlices; ++i) {
         double angle = 2*Math.PI/nSlices*i;
         float x = (float)Math.cos(angle);
         float y = (float)Math.sin(angle);
         cylinder.normal(x, y, 0);
         cylinder.vertex(x, y, 1);  // top
         cylinder.vertex(x, y, 0);  // bottom
      }
      cylinder.endBuild();
      // loop around
      cylinder.addTriangle(2*nSlices-2, 2*nSlices-1, 0);
      cylinder.addTriangle(0, 2*nSlices-1, 1);
      
      // caps
      if (capped) {
         // top
         cylinder.beginBuild(BuildMode.TRIANGLE_FAN);
         int nidx = cylinder.addNormal(0,0,1);
         for (int i=0; i<nSlices; ++i) {
            cylinder.addVertex(2*i, nidx); // even positions (top)
         }
         cylinder.endBuild();
         
         // bottom
         cylinder.beginBuild(BuildMode.TRIANGLE_FAN);
         nidx = cylinder.addNormal(0,0,-1);
         cylinder.addVertex(1, nidx);
         for (int i=1; i<nSlices; ++i) {
            int j = nSlices-i;
            cylinder.addVertex(2*j+1, nidx);  // odd positions (bottom)
         }
         cylinder.endBuild();

      }

      // cylinder.commit();
      return cylinder;
   }
   
   /**
    * Creates a unit cone along the z-axis
    * @param nSlices angular resolution
    * @param capped include top/bottom caps
    * @return
    */
   public static RenderObject createCone(int nSlices, boolean capped) {

      RenderObject cone = new RenderObject();
      
      // reserve memory
      cone.ensurePositionCapacity(nSlices+1);
      if (capped) {
         cone.ensureNormalCapacity(nSlices+1);
         cone.ensureTriangleCapacity(3*nSlices+3);
      } else {
         cone.ensureNormalCapacity(nSlices);
         cone.ensureTriangleCapacity(2*nSlices);
      }
      
      // sides
      float r2 = (float)(1.0/Math.sqrt(2));

      // bottom
      for (int i=0; i<nSlices; ++i) {
         double angle = 2*Math.PI/nSlices*i;
         float x = (float)Math.cos(angle);
         float y = (float)Math.sin(angle);
         cone.addPosition(x, y, 0);
         cone.addNormal(x*r2, y*r2, r2);
      }
      
      // top
      int ptop = cone.addPosition(0,0,1);
      
      // sides
      cone.beginBuild(BuildMode.TRIANGLE_STRIP);
      for (int i=0; i<nSlices; ++i) {
         cone.addVertex(ptop, i);
         cone.addVertex(i, i);
      }
      cone.endBuild();
      cone.addTriangle(2*nSlices-2, 2*nSlices-1, 0);
      cone.addTriangle(0, 2*nSlices-1, 1);

      // cap
      if (capped) {
         // bottom
         cone.beginBuild(BuildMode.TRIANGLE_FAN);
         int nidx = cone.addNormal(0,0,-1);
         cone.addVertex(0, nidx);
         for (int i=1; i<nSlices; ++i) {
            int j = nSlices-i;
            cone.addVertex(j, nidx);
         }
         cone.endBuild();
      }

      // cone.commit();
      return cone;
   }
   
   /**
    * Creates a unit tapered ellipsoid along the z-axis
    * @param nSlices angular resolution (longitude)
    * @param nLevels vertical resolution (latitude)
    * @return
    */
   public static RenderObject createTaperedEllipsoid(int nSlices, int nLevels) {

      if (nLevels < 2) {
         nLevels = 2;
      }
      
      RenderObject ellipsoid = new RenderObject();
      
      // reserve memory
      ellipsoid.ensurePositionCapacity(2+nSlices*(nLevels-1));
      ellipsoid.ensureNormalCapacity(2+nSlices*(nLevels-1));
      ellipsoid.ensureTriangleCapacity(2*nSlices*(nLevels-1));
      
      // bottom
      ellipsoid.normal(0, 0, -1);
      ellipsoid.vertex(0, 0, 0);
      
      for (int j=1; j < nLevels; ++j) {
         float h = j * 1.0f / nLevels;
         
         for (int i = 0; i < nSlices; ++i) {
            double ang = i * 2 * Math.PI / nSlices;
            float c0 = (float)Math.cos (ang);
            float s0 = (float)Math.sin (ang);

            float r = (float)Math.sin (h * Math.PI);
            float drdh = (float)(Math.PI * Math.cos (h * Math.PI));

            ellipsoid.normal(c0, s0, -drdh);
            ellipsoid.vertex(c0*r, s0*r, h);
         }
      }

      // top
      ellipsoid.normal(0, 0, 1);
      ellipsoid.vertex(0, 0, 1);

      // triangles
      // bottom
      for (int i=0; i<nSlices; ++i) {
         int j = (i + 1) % nSlices;
         ellipsoid.addTriangle(0, j+1, i+1);
      }

      // middle
      for (int l=0; l<nLevels-2; ++l) {
         int boff = 1+l*nSlices;
         for (int i=0; i<nSlices; ++i) {
            int j = (i + 1) % nSlices;
            ellipsoid.addTriangle(boff+j+nSlices, boff+i+nSlices, boff+i);
            ellipsoid.addTriangle(boff+j, boff+j+nSlices, boff+i);
         }
      }

      // top
      int boff = 1+nSlices*(nLevels-2);
      int toff = boff+nSlices;
      for (int i=0; i<nSlices; ++i) {
         int j = (i + 1) % nSlices;
         ellipsoid.addTriangle( boff+j, toff, boff+i);
      }

      // ellipsoid.commit();
      return ellipsoid;
   }
   
   /**
    * Creates a unit sphere centered at the origin
    * @param nSlices angular resolution (longitude)
    * @param nLevels vertical resolution (latitude)
    * @return
    */
   public static RenderObject createSphere(int nSlices, int nLevels) {

      if (nLevels < 2) {
         nLevels = 2;
      }
      
      RenderObject sphere = new RenderObject();
      
      // reserve memory
      sphere.ensurePositionCapacity(2+nSlices*(nLevels-1));
      sphere.ensureNormalCapacity(2+nSlices*(nLevels-1));
      sphere.ensureTriangleCapacity(2*nSlices*(nLevels-1));
      
      // bottom
      sphere.normal(0, 0, -1);
      sphere.vertex(0, 0, -1);
      
      for (int j=1; j < nLevels; ++j) {
         double hang = j*Math.PI/nLevels;
         float h = -(float)Math.cos(hang);
         float r = (float)Math.sin(hang);
         
         for (int i = 0; i < nSlices; ++i) {
            double ang = i * 2 * Math.PI / nSlices;
            float c0 = (float)Math.cos (ang);
            float s0 = (float)Math.sin (ang);

            sphere.normal(c0*r, s0*r, h);
            sphere.vertex(c0*r, s0*r, h);
         }
      }

      // top
      sphere.normal(0, 0, 1);
      sphere.vertex(0, 0, 1);

      // triangles
      // bottom
      for (int i=0; i<nSlices; ++i) {
         int j = (i + 1) % nSlices;
         sphere.addTriangle(0, j+1, i+1);
      }

      // middle
      for (int l=0; l<nLevels-2; ++l) {
         int boff = 1+l*nSlices;
         for (int i=0; i<nSlices; ++i) {
            int j = (i + 1) % nSlices;
            sphere.addTriangle(boff+j+nSlices, boff+i+nSlices, boff+i);
            sphere.addTriangle(boff+j, boff+j+nSlices, boff+i);
         }
      }

      // top
      int boff = 1+nSlices*(nLevels-2);
      int toff = boff+nSlices;
      for (int i=0; i<nSlices; ++i) {
         int j = (i + 1) % nSlices;
         sphere.addTriangle( boff+j, toff, boff+i);
      }

      // sphere.commit();
      return sphere;
   }
   
   /**
    * Creates a simple set of axes
    * @param x to include x?
    * @param y to include y?
    * @param z to include z?
    * @return
    */
   public RenderObject createAxes(boolean x, boolean y, boolean z) {
      
      RenderObject axes = new RenderObject();
      
      axes.beginBuild(BuildMode.LINES);
      if (x) {
         axes.color(1f, 0, 0, 1f);
         axes.vertex(0, 0, 0);
         axes.vertex(1, 0, 0);
      }
      if (y) {
         axes.color(0, 1f, 0, 1f);
         axes.vertex(0, 0, 0);
         axes.vertex(0, 1, 0);
      }
      if (z) {
         axes.color(0, 0, 1f, 1f);
         axes.vertex(0, 0, 0);
         axes.vertex(0, 0, 1);
      }
      
      axes.endBuild();
      // axes.commit();
      return axes;
      
   }

}
