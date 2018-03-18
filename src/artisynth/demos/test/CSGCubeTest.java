package artisynth.demos.test;

import java.io.IOException;

import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderable;

public class CSGCubeTest extends RootModel {
   
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      PolygonalMesh cube1 = MeshFactory.createBox(1, 1, 1);
      
      PolygonalMesh cube2 = MeshFactory.createBox(0.5, 0.5, 0.5);
      cube2.transform(new RigidTransform3d(new Vector3d(0.25,0.25,0.25), AxisAngle.IDENTITY));
      
      cube1.setHardEdgesFromFaceNormals(0.707);
      cube2.setHardEdgesFromFaceNormals(0.707);
      
      PolygonalMesh isect = MeshFactory.getIntersection(cube1, cube2);
      isect.setHardEdgesFromFaceNormals(0.707);
      PolygonalMesh sub =  MeshFactory.getSubtraction(cube1, cube2);
      sub.setHardEdgesFromFaceNormals(0.707);
      PolygonalMesh union = MeshFactory.getUnion(cube1, cube2);
      union.setHardEdgesFromFaceNormals(0.707);
      
      addRenderable(new FixedMeshBody("cube1", cube1));
      addRenderable(new FixedMeshBody("cube2", cube2));
      addRenderable(new FixedMeshBody("intersection", isect));
      addRenderable(new FixedMeshBody("subtraction", sub));
      addRenderable(new FixedMeshBody("union", union));
      
      for (Renderable r : renderables()) {
         if (r instanceof FixedMeshBody) {
            FixedMeshBody fmesh = (FixedMeshBody)r;
            PolygonalMesh mesh = (PolygonalMesh)(fmesh.getMesh());
            
            System.out.println("Mesh: " + fmesh.getName());
            System.out.println("  # verts: " + mesh.numVertices());
            System.out.println("  # faces: " + mesh.numFaces());
            System.out.println("  closed:  " + mesh.isClosed());
            System.out.println("  manifo:  " + mesh.isManifold());
            System.out.println("  area:    " + mesh.computeArea());
            System.out.println("  volume:  " + mesh.computeVolume());
         }
      }
      
      //      PolygonalMesh cube3 = new PolygonalMesh(cube1);
      //      cube3 = MeshFactory.subdivide(cube3);
      //      cube3.mergeCoplanarFaces(0.99);
      //      for (Face f : cube3.getFaces()) {
      //         System.out.println(f.getIndex() + ", " + f.numEdges());
      //      }
      //      
      //      int[] nrmIdxs = cube3.getNormalIndices();
      //      int[] offsets = cube3.getFeatureIndexOffsets();
      //      for (int i=0; i<offsets.length-1; ++i) {
      //         System.out.println("Face " + i + ": ");
      //         for (int j = offsets[i]; j<offsets[i+1]; ++j) {
      //            Vector3d nrm = cube3.getNormal(nrmIdxs[j]);
      //            System.out.println(nrm.toString());
      //         }
      //      }
      //      FixedMeshBody fm = new FixedMeshBody("cube3", cube3); 
      //      addRenderable(fm);
      //      RenderProps.setFaceStyle(fm, FaceStyle.FRONT_AND_BACK);
      //      RenderProps.setFaceColor(fm, Color.BLUE);
      //      RenderProps.setBackColor(fm, Color.YELLOW);
      
      RenderProps.setDrawEdges(this, true);
      
   }

}
