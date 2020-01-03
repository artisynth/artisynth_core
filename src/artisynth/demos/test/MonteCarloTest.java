package artisynth.demos.test;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.workspace.RootModel;
import artisynth.core.femmodels.integration.MonteCarloSampler;
import maspack.geometry.PointMesh;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

/**
 * Tests the MonteCarloSampler for tets, pyramids, and wedges.
 * 
 * @author Antonio
 */
public class MonteCarloTest extends RootModel {
   
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      int nsamples = 100000;
      
      addTets(nsamples, new Point3d(0,0,0));
      addPyramids(nsamples, new Point3d(2.5, 0, 0));
      addWedges(nsamples, new Point3d(0, 2.5, 0));
      
   }
   
   private void addTets(int nsamples, Vector3d origin) {
      RenderableComponentList<MeshComponent> tets = new RenderableComponentList<>(MeshComponent.class, "tets");
      addRenderable(tets);
      
      PointMesh[] tetMesh = new PointMesh[8];
      Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE, Color.MAGENTA, Color.CYAN, Color.WHITE, Color.GRAY};
      for (int i=0; i<tetMesh.length; ++i) {
         tetMesh[i] = new PointMesh();
         tetMesh[i].setName("tet" + i);
         RenderProps.setPointColor(tetMesh[i], colors[i]);
      }
      
      Point3d s = new Point3d();
      for (int i=0; i<nsamples; ++i) {
         MonteCarloSampler.sampleTet(s);
      
         // separate into a sub-volume
         if (s.x > 0.5) {
            tetMesh[0].addVertex(s, false);
         } else if (s.y > 0.5) {
            tetMesh[1].addVertex(s, false);
         } else if (s.z > 0.5) {
            tetMesh[2].addVertex(s, false);
         } else if (s.y + s.z < s.x) {
            tetMesh[3].addVertex(s, false);
         } else if (s.x + s.z < s.y) {
            tetMesh[4].addVertex(s, false);
         } else if (s.x + s.y < s.z) {
            tetMesh[5].addVertex(s, false);
         } else if (s.x + s.y > 0.5){
            tetMesh[6].addVertex(s, false);
         } else {
            tetMesh[7].addVertex(s, false);
         }
      }
      
      for (PointMesh mesh : tetMesh) {
         FixedMeshBody body = new FixedMeshBody(mesh);
         body.transformGeometry(new RigidTransform3d(origin, AxisAngle.IDENTITY));
         tets.add(body);
         System.out.println(mesh.getName() + ": " + mesh.numVertices() + " (" + mesh.numVertices()*1.0/nsamples + ")");
      }
   }
   
   private void addPyramids(int nsamples, Point3d origin) {
      RenderableComponentList<MeshComponent> pyramids = new RenderableComponentList<>(MeshComponent.class, "pyramids");
      addRenderable(pyramids);
      
      PointMesh[] meshes = new PointMesh[8];
      Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE, Color.MAGENTA, Color.CYAN, Color.WHITE, Color.GRAY};
      for (int i=0; i<meshes.length; ++i) {
         meshes[i] = new PointMesh();
         meshes[i].setName("pyramid" + i);
         RenderProps.setPointColor(meshes[i], colors[i % colors.length]);
      }
      
      Point3d s = new Point3d();
      Point3d s2 = new Point3d();
      for (int i=0; i<nsamples; ++i) {
         MonteCarloSampler.samplePyramid(s2);
         s.set(s2.x, s2.y, (s2.z+1)/2);
         // separate into a sub-volume
         if (s.x >= 0 && s.y >= 0 && s.x > s.z && s.y > s.z) {
            meshes[0].addVertex(s2, false);
         } else if (s.x >= 0 && s.y < 0  && s.x > s.z && s.y < -s.z) {
            meshes[1].addVertex(s2, false);
         } else if (s.y >= 0 && s.x < -s.z && s.y > s.z) {
            meshes[2].addVertex(s2, false);
         } else if (s.x < -s.z && s.y < -s.z){
            meshes[3].addVertex(s2, false);
         } else if (s.z > 0.5) {
            meshes[4].addVertex(s2, false);
         } else if (s.x <= s.z && s.y <= s.z && s.x > -s.z && s.y > -s.z) {
            meshes[5].addVertex(s2, false);
         } else if (s.x <= s.z && s.x > -s.z) {
            meshes[6].addVertex(s2, false);
         } else {
            meshes[7].addVertex(s2, false);
         }
      }
      
      for (PointMesh mesh : meshes) {
         FixedMeshBody body = new FixedMeshBody(mesh);
         body.transformGeometry(new RigidTransform3d(origin, AxisAngle.IDENTITY));
         pyramids.add(body);
         System.out.println(mesh.getName() + ": " + mesh.numVertices() + " (" + mesh.numVertices()*1.0/nsamples + ")");
      }
      
   }

   private void addWedges(int nsamples, Point3d origin) {
      RenderableComponentList<MeshComponent> wedges = new RenderableComponentList<>(MeshComponent.class, "wedges");
      addRenderable(wedges);
      
      PointMesh[] meshes = new PointMesh[8];
      Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE, Color.MAGENTA, Color.CYAN, Color.WHITE, Color.GRAY};
      for (int i=0; i<meshes.length; ++i) {
         meshes[i] = new PointMesh();
         meshes[i].setName("wedge" + i);
         RenderProps.setPointColor(meshes[i], colors[i % colors.length]);
      }
      
      Point3d s = new Point3d();
      for (int i=0; i<nsamples; ++i) {
         MonteCarloSampler.sampleWedge(s);
      
         // separate into a sub-volume
         if (s.z < 0) {
            if (s.x + s.y < 0.5) {
               meshes[0].addVertex(s, false);
            } else if (s.x > 0.5) {
               meshes[1].addVertex(s, false);
            } else if (s.y > 0.5) {
               meshes[2].addVertex(s, false);
            } else {
               meshes[3].addVertex(s, false);
            }
         } else {
            if (s.x + s.y < 0.5) {
               meshes[4].addVertex(s, false);
            } else if (s.x > 0.5) {
               meshes[5].addVertex(s, false);
            } else if (s.y > 0.5) {
               meshes[6].addVertex(s, false);
            } else {
               meshes[7].addVertex(s, false);
            }
         }
      }
      
      for (PointMesh mesh : meshes) {
         FixedMeshBody body = new FixedMeshBody(mesh);
         body.transformGeometry(new RigidTransform3d(origin, AxisAngle.IDENTITY));
         wedges.add(body);
         System.out.println(mesh.getName() + ": " + mesh.numVertices() + " (" + mesh.numVertices()*1.0/nsamples + ")");
      }
      
   }
   
}
