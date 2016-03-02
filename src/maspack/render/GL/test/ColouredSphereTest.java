package maspack.render.GL.test;

import java.awt.Color;

import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.mechmodels.MeshComponent;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;
import maspack.render.GL.test.MultiViewer.SimpleSelectable;
import maspack.render.color.HueColorMap;

public class ColouredSphereTest extends GL2vsGL3Tester {
   
   @Override
   protected void addContent (MultiViewer mv) {
   
      PolygonalMesh mesh = MeshFactory.createOctahedralSphere (1, 4);
      
      HueColorMap map = new HueColorMap ();
      
      mesh.setVertexColoringEnabled();
      for (int i=0; i<mesh.numVertices(); ++i) {
         // hsv interpolation of colors based on height (-1 to 1)
         Vertex3d vtx = mesh.getVertex (i);
         double pos = vtx.getPosition ().z;
         Color c = map.getColor ((pos+1)/2);
         mesh.setColor (i, c);
      }
      
      RenderProps rprops = new RenderProps();
      rprops.setShading (Shading.PHONG);
      rprops.setShininess (128);
      rprops.setSpecular (Color.WHITE);
      mesh.setRenderProps(rprops);
      
      // FixedMeshBody fm = new FixedMeshBody (mesh);
      
      mv.addRenderable (mesh);
      
   }
   
   public static void main (String[] args) {
      ColouredSphereTest tester = new ColouredSphereTest();
      tester.run ();
   }

}
