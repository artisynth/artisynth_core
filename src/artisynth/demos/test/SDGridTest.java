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
import maspack.geometry.SignedDistanceGrid;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;

public class SDGridTest extends RootModel {
   
   SignedDistanceGrid sdgrid;
   
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
      
      sdgrid = new SignedDistanceGrid(mesh, margin, cells);

      // test a bunch of inside points
      Vector3d norm = new Vector3d();
      double dx = 2*c/((1<<(divisions+1)));
      for (double x=-c+dx; x<c; x+=dx) {
         for (double y=-c+dx; y<c; y+=dx) {
            for (double z=-c+dx; z<c; z+=dx) {
               double d = sdgrid.getDistanceAndNormal(norm, x, y, z);
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
   
   @Override
   public void render (Renderer renderer, int flags) {

      
      if (sdgrid != null) {
         Shading savedShading = renderer.getShading();
         renderer.setShading (Shading.NONE);
         renderer.setPointSize(5);
         
         Vector3d maxGrid = new Vector3d();
         Vector3d min = sdgrid.getMin();
         int[] gridSize = sdgrid.getGridSize();
         Vector3d gridCellSize = sdgrid.getGridCellSize();
         
         maxGrid.x = min.x + (gridSize[0]-1) * gridCellSize.x;
         maxGrid.y = min.y + (gridSize[1]-1) * gridCellSize.y; 
         maxGrid.z = min.z + (gridSize[2]-1) * gridCellSize.z;
   
         renderer.beginDraw (DrawMode.LINES); // Draw 4 vertical lines.
         renderer.addVertex (maxGrid.x, maxGrid.y, maxGrid.z);
         renderer.addVertex (maxGrid.x, maxGrid.y, min.z);
         renderer.addVertex (min.x, maxGrid.y, maxGrid.z);
         renderer.addVertex (min.x, maxGrid.y, min.z);
         renderer.addVertex (min.x, min.y, maxGrid.z);
         renderer.addVertex (min.x, min.y, min.z);
         renderer.addVertex (maxGrid.x, min.y, maxGrid.z);
         renderer.addVertex (maxGrid.x, min.y, min.z);
         // Draw a diagonal line from max to min.
         renderer.addVertex (min.x, min.y, min.z);
         renderer.addVertex (maxGrid.x, maxGrid.y, maxGrid.z);
         renderer.endDraw();
         
         
         double[] phi = sdgrid.getPhi();
         
         // Draw the vertices on the grid.
         double myVertex[] = new double[3];
         for (int i = 0; i < phi.length; i++) {
            int z = (i / (gridSize[0] * gridSize[1]));
            int y = (i - z * (gridSize[0] * gridSize[1])) / (gridSize[0]);
            int x = (i % (gridSize[0]));
            sdgrid.getMeshCoordinatesFromGrid (x, y, z, myVertex);   
         
            if (phi[i] <= 0) {
               renderer.setColor(Color.BLUE);
               renderer.drawPoint(myVertex[0], myVertex[1], myVertex[2]);
            } else {
               renderer.setColor(Color.RED);
               renderer.drawPoint(myVertex[0], myVertex[1], myVertex[2]);
            }
            
         }
         
         renderer.setPointSize(1);
         renderer.setShading (savedShading);
      }
   }

}
