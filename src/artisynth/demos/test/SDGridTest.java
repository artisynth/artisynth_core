package artisynth.demos.test;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.Face;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.DistanceGrid;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.matrix.VectorTransformer3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;

public class SDGridTest extends RootModel {
   
   DistanceGrid sdgrid;
   
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      
      PolygonalMesh mesh = new PolygonalMesh();
      Vertex3d[] v = new Vertex3d[9*2+8];
      Face[] f = new Face[8*6];
      int idx = 0;
      int fidx = 0;
      
      double c = 64;
      
      v[idx++] = mesh.addVertex(-c,-c,-c);
      v[idx++] = mesh.addVertex(-c,-c, 0);
      v[idx++] = mesh.addVertex(-c,-c, c);
      v[idx++] = mesh.addVertex(-c, 0,-c);
      v[idx++] = mesh.addVertex(-c, 0, 0);
      v[idx++] = mesh.addVertex(-c, 0, c);
      v[idx++] = mesh.addVertex(-c, c,-c);
      v[idx++] = mesh.addVertex(-c, c, 0);
      v[idx++] = mesh.addVertex(-c, c, c);
      
      // left
      f[fidx++] = mesh.addFace(v[0], v[1], v[4]);
      f[fidx++] = mesh.addFace(v[1], v[2], v[4]);
      f[fidx++] = mesh.addFace(v[2], v[5], v[4]);
      f[fidx++] = mesh.addFace(v[5], v[8], v[4]);
      f[fidx++] = mesh.addFace(v[8], v[7], v[4]);
      f[fidx++] = mesh.addFace(v[7], v[6], v[4]);
      f[fidx++] = mesh.addFace(v[6], v[3], v[4]);
      f[fidx++] = mesh.addFace(v[3], v[0], v[4]);
      
      v[idx++] = mesh.addVertex( 0,-c,-c);
      v[idx++] = mesh.addVertex( 0,-c, 0);
      v[idx++] = mesh.addVertex( 0,-c, c);
      v[idx++] = mesh.addVertex( 0, 0,-c);
      // test.addVertex( 0, 0, 0);
      v[idx++] = mesh.addVertex( 0, 0, c);
      v[idx++] = mesh.addVertex( 0, c,-c);
      v[idx++] = mesh.addVertex( 0, c, 0);
      v[idx++] = mesh.addVertex( 0, c, c);
      
      
      f[fidx++] = mesh.addFace(v[0], v[9], v[10]); // back
      f[fidx++] = mesh.addFace(v[1], v[0], v[10]);
      f[fidx++] = mesh.addFace(v[2], v[1], v[10]);
      f[fidx++] = mesh.addFace(v[11], v[2], v[10]);
      f[fidx++] = mesh.addFace(v[2], v[11], v[13]); // top
      f[fidx++] = mesh.addFace(v[5], v[2], v[13]);
      f[fidx++] = mesh.addFace(v[8], v[5], v[13]);
      f[fidx++] = mesh.addFace(v[16], v[8], v[13]);
      f[fidx++] = mesh.addFace(v[8], v[16], v[15]); // front
      f[fidx++] = mesh.addFace(v[7], v[8], v[15]);
      f[fidx++] = mesh.addFace(v[6], v[7], v[15]);
      f[fidx++] = mesh.addFace(v[14], v[6], v[15]);
      f[fidx++] = mesh.addFace(v[3], v[6], v[12]);  // bottom
      f[fidx++] = mesh.addFace(v[6], v[14], v[12]);
      f[fidx++] = mesh.addFace(v[0], v[3], v[12]);
      f[fidx++] = mesh.addFace(v[9], v[0], v[12]);
      
      v[idx++] = mesh.addVertex( c,-c,-c);
      v[idx++] = mesh.addVertex( c,-c, 0);
      v[idx++] = mesh.addVertex( c,-c, c);
      v[idx++] = mesh.addVertex( c, 0,-c);
      v[idx++] = mesh.addVertex( c, 0, 0);
      v[idx++] = mesh.addVertex( c, 0, c);
      v[idx++] = mesh.addVertex( c, c,-c);
      v[idx++] = mesh.addVertex( c, c, 0);
      v[idx++] = mesh.addVertex( c, c, c);
      
      f[fidx++] = mesh.addFace(v[17], v[9], v[12]);  // bottom
      f[fidx++] = mesh.addFace(v[20], v[17], v[12]);
      f[fidx++] = mesh.addFace(v[23], v[20], v[12]);
      f[fidx++] = mesh.addFace(v[14], v[23], v[12]);
      f[fidx++] = mesh.addFace(v[9], v[17], v[10]);
      f[fidx++] = mesh.addFace(v[17], v[18], v[10]);
      f[fidx++] = mesh.addFace(v[18], v[19], v[10]);
      f[fidx++] = mesh.addFace(v[19], v[11], v[10]);
      f[fidx++] = mesh.addFace(v[11], v[19], v[13]);
      f[fidx++] = mesh.addFace(v[19], v[22], v[13]);
      f[fidx++] = mesh.addFace(v[22], v[25], v[13]);
      f[fidx++] = mesh.addFace(v[25], v[16], v[13]);
      f[fidx++] = mesh.addFace(v[16], v[25], v[15]);
      f[fidx++] = mesh.addFace(v[25], v[24], v[15]);
      f[fidx++] = mesh.addFace(v[24], v[23], v[15]);
      f[fidx++] = mesh.addFace(v[23], v[14], v[15]);
      
      f[fidx++] = mesh.addFace(v[25], v[22], v[21]);
      f[fidx++] = mesh.addFace(v[22], v[19], v[21]);
      f[fidx++] = mesh.addFace(v[19], v[18], v[21]);
      f[fidx++] = mesh.addFace(v[18], v[17], v[21]);
      f[fidx++] = mesh.addFace(v[17], v[20], v[21]);
      f[fidx++] = mesh.addFace(v[20], v[23], v[21]);
      f[fidx++] = mesh.addFace(v[23], v[24], v[21]);
      f[fidx++] = mesh.addFace(v[24], v[25], v[21]);
      
      // Move in some corner(s)
      v[0].setPosition(new Point3d(0,0,0));
      //      v[25].setPosition(new Point3d(0,0,0));
      
      int divisions = 3;
      mesh = MeshFactory.subdivide(mesh, divisions);
      
      // randomize vertex order and re-number (to test different chiralities)
      ArrayList<Vertex3d> vertices = mesh.getVertices();
      Collections.shuffle(vertices);
      for (int i=0; i<vertices.size(); ++i) {
         vertices.get(i).setIndex(i);
      }
      
      
      FixedMeshBody fm = new FixedMeshBody("cube", mesh);
      RenderProps.setFaceStyle(fm, FaceStyle.FRONT_AND_BACK);
      addRenderable(fm);
      
      int cells = 1<<(divisions+2);
      double margin = 1.0/cells;
      cells += 2;
      
      sdgrid = new DistanceGrid(
         mesh.getFaces(), margin, cells, /*signed=*/true);

      // test a bunch of inside points
      Vector3d norm = new Vector3d();
      double dx = 2*c/((1<<(divisions+1)));
      for (double x=-c+dx; x<c; x+=dx) {
         for (double y=-c+dx; y<c; y+=dx) {
            for (double z=-c+dx; z<c; z+=dx) {
               double d = sdgrid.getLocalDistanceAndNormal(norm, x, y, z);
               // check bottom corner
               if ((x < 0 && y < 0 && z < 0)) { //||(x > 0 && y > 0 && z > 0)) {
                  if (d < 0) {
                     System.err.println("Point (" + x + "," + y + "," + z + ") incorrectly labelled as inside");
                  }
               } else {
                  if (d > 0) {
                     System.err.println("Point (" + x + "," + y + "," + z + ") incorrectly labelled as outside");
                  }
               }
            }
         }
      }
      
      RenderProps.setDrawEdges(fm, true);
   }
   
   Vector3d gridToLocal (
      VectorTransformer3d TGL, double x, double y, double z) {
      Vector3d loc = new Vector3d();
      TGL.transformPnt (loc, new Vector3d(x, y, z));
      return loc;
   }
   
   @Override
   public void render (Renderer renderer, int flags) {

      if (sdgrid != null) {
         Shading savedShading = renderer.getShading();
         renderer.setShading (Shading.NONE);
         renderer.setPointSize(5);
         
         VectorTransformer3d TGL = sdgrid.getGridToLocalTransformer();
         Vector3i res = sdgrid.getResolution();
   
         renderer.beginDraw (DrawMode.LINES); // Draw 4 vertical lines.
         renderer.addVertex (gridToLocal (TGL, res.x  , res.y  , res.z  ));
         renderer.addVertex (gridToLocal (TGL, res.x  , res.y  , 0));
         renderer.addVertex (gridToLocal (TGL, 0,       res.y  , res.z  ));
         renderer.addVertex (gridToLocal (TGL, 0,       res.y  , 0));
         renderer.addVertex (gridToLocal (TGL, 0,       0,       res.z  ));
         renderer.addVertex (gridToLocal (TGL, 0,       0,       0));
         renderer.addVertex (gridToLocal (TGL, res.x  , 0,       res.z  ));
         renderer.addVertex (gridToLocal (TGL, res.x  , 0,       0));
         // Draw a diagonal line from max to min.
         renderer.addVertex (gridToLocal (TGL, 0,       0,       0));
         renderer.addVertex (gridToLocal (TGL, res.x  , res.y  , res.z  ));
         renderer.endDraw();
         
         
         double[] phi = sdgrid.getVertexDistances();
         
         // Draw the vertices on the grid.
         Vector3d coords = new Vector3d();
         for (int i = 0; i < phi.length; i++) {
            Vector3i vxyz = new Vector3i();
            sdgrid.vertexToXyzIndices (vxyz, i);
            sdgrid.getLocalVertexCoords (coords, vxyz);
         
            if (phi[i] <= 0) {
               renderer.setColor(Color.BLUE);
               renderer.drawPoint(coords);
            } else {
               renderer.setColor(Color.RED);
               renderer.drawPoint(coords);
            }
            
         }
         
         renderer.setPointSize(1);
         renderer.setShading (savedShading);
      }
   }

}
