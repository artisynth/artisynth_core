package maspack.collision;

import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;

import javax.swing.JFrame;

import maspack.collision.ContactInfo;
import maspack.collision.SurfaceMeshCollider;
import maspack.geometry.*;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.IsRenderable;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GLViewerFrame;

public class Test {
   PolygonalMesh mesh1, mesh2;
   ContactInfo info = null;

   public static void main (String[] args) {
      for (int i = 1200; i <= 1400; i = i + 200)
         TestMatrix (i);
      // new Test().test1();
      // testCaseMeshes();
   }

   static void TestMatrix (int n) {
      int reps = 1;
      MatrixNd m1 = new MatrixNd (n, n), m2 = new MatrixNd (n, n), m3 =
         new MatrixNd (n, n);
      Random r = new Random (1234);
      for (int i = 0; i < n; i++) {
         for (int j = 0; i < n; i++) {
            m1.set (i, j, r.nextDouble());
            m2.set (i, j, r.nextDouble());
         }
      }
      long t = System.nanoTime();
      for (int i = 0; i < reps; i++) {
         m3.mul (m1, m2);
      }
      t = System.nanoTime() - t;
      System.out.println (
         n + "x" + n + ", " + reps + ": " + (float)t / 1e9 + " sec");
   }

   void test1() {
      GLViewerFrame frame = new GLViewerFrame ("collide", 1000, 800);
      frame.setDefaultCloseOperation (JFrame.DISPOSE_ON_CLOSE);
      GLViewer vw = frame.getViewer();
      frame.setVisible (true);
      KeyHandler keyHandler = new KeyHandler (vw);
      vw.addKeyListener (keyHandler);
      vw.addRenderable (new RenderableAxes());
      case2 (frame);
      vw.autoFit();
      mesh1.setRenderBuffered (false);
      // Make sure all faces are included in the hierarchies.
      /*
       * AjlBvTree bvh = mesh0.getBvHierarchy(); for (Face f: mesh0.getFaces())
       * if (!bvh.getRoot().includesFace(f)) throw new RuntimeException("missing
       * face"); bvh = mesh1.getBvHierarchy(); for (Face f: mesh1.getFaces()) if
       * (!bvh.getRoot().includesFace(f)) throw new RuntimeException("missing
       * face");
       */

      // Make sure intersections are consistent.
      /*
       * MeshIntersectionConsistencyTest.testConsistency(mesh0, mesh1);
       * MeshIntersectionConsistencyTest.testConsistency(mesh1, mesh0);
       */

      SurfaceMeshCollider collider = new SurfaceMeshCollider();
      vw.rerender();

      info = collider.getContacts (mesh1, mesh2);
      // if (info != null) vw.addRenderable(info);
      // vw.rerender();

      Vector3d pert = new Vector3d (0.00003, 0, 0);
      for (int i = 0; i < 4e5; i++) {
         // if (info != null) vw.removeRenderable(info);
         if (mesh1.isFixed()) {
            mesh1.getMeshToWorld().p.add (pert);
         }
         else {
            for (Vertex3d v : mesh1.getVertices())
               v.pnt.add (pert);
         }
         mesh1.notifyVertexPositionsModified();
         // mesh1.invalidateWorldCoords();
         // mesh1.invalidateFaceNormals();
         mesh1.updateFaceNormals();
         info = collider.getContacts (mesh1, mesh2);
         // if (info != null) vw.addRenderable(info);
         vw.rerender();
      }
      System.out.println ("test done");
      // if (info != null) vw.addRenderable(info);
      vw.rerender();

   }

   void case1 (GLViewerFrame frame) {
      double sphereRadius = 3.0;
      Point3d sphere0Pos = new Point3d (0.0, 0.0, 0.0);
      mesh2 = MeshFactory.createSphere (sphereRadius, 32);
      if (!mesh2.isTriangular())
         throw new RuntimeException ("mesh not triangular");
      positionRigidMesh (mesh2, sphere0Pos);
      setMeshRenderProps (mesh2, frame, Color.BLUE);
      if (!mesh2.isClosed())
         throw new RuntimeException ("mesh not closed");
      if (!mesh2.isTriangular())
         throw new RuntimeException ("mesh not triangular");

      mesh1 = MeshFactory.createSphere (sphereRadius, 32);
      if (!mesh1.isTriangular())
         throw new RuntimeException ("mesh not triangular");
      Point3d mesh1Pos = new Point3d (sphereRadius * 2 * 0.95, 0, 0);
      positionRigidMesh (mesh1, mesh1Pos);
      setMeshRenderProps (mesh1, frame, Color.GREEN);
      if (!mesh1.isClosed())
         throw new RuntimeException ("mesh not closed");
      if (!mesh1.isTriangular())
         throw new RuntimeException ("mesh not triangular");
   }

   void case2 (GLViewerFrame frame) {
      Point3d pos1 = new Point3d (-1.0, 0.0, 0.0);
      mesh1 = MeshFactory.createBox (1.0, 2.0, 3.0);
      positionRigidMesh (mesh1, pos1);
      setMeshRenderProps (mesh1, frame, Color.GREEN);
      if (!mesh1.isClosed())
         throw new RuntimeException ("mesh1 not closed");

      Point3d pos0 = new Point3d (0.01, 0.01, 0.01);
      mesh2 = MeshFactory.createBox (1.0, 2.0, 3.0);
      positionRigidMesh (mesh2, pos0);
      setMeshRenderProps (mesh2, frame, Color.BLUE);
      if (!mesh2.isClosed())
         throw new RuntimeException ("mesh0 not closed");

   }

   static void positionFemMesh (PolygonalMesh mesh, Point3d meshPosition) {
      mesh.setFixed (false);
      for (Vertex3d v : mesh.getVertices()) {
         v.pnt.add (meshPosition);
      }
   }

   static void positionRigidMesh (PolygonalMesh mesh, RigidTransform3d X0) {
      mesh.setFixed (true);
      mesh.setMeshToWorld (X0);
   }

   static void positionRigidMesh (PolygonalMesh mesh, Point3d meshPosition) {
      RigidTransform3d X0 = new RigidTransform3d();
      X0.p.set (meshPosition);
      positionRigidMesh (mesh, X0);
   }

   static void setMeshRenderProps (
      PolygonalMesh mesh, GLViewerFrame frame, Color col) {
      RenderProps rp = mesh.getRenderProps();
      rp.setDrawEdges (true);
      rp.setFaceStyle (Renderer.FaceStyle.NONE);
      rp.setLineColor (col);
      mesh.setRenderProps (rp);
      frame.getViewer().addRenderable (mesh);
      frame.getViewer().addRenderable (mesh.getBVTree());
   }

   // Make sure every face is included in its mesh's bvtree
   /*
    * static void testBvHierarchy(PolygonalMesh mesh) { AjlBvTree bvh =
    * mesh.getBvHierarchy(); for (Face f: mesh.getFaces()) if
    * (!bvh.getRoot().includesFace(f)) throw new RuntimeException("missing
    * face"); }
    */
   class RenderableAxes implements IsRenderable {

      public void render (Renderer renderer, int flags) {

         double axisSize = 1000.0;
         renderer.drawAxes (
            RigidTransform3d.IDENTITY, axisSize, 1, /*highlight=*/false);
      }

      public int getRenderHints() {
         return 0;
      }

      public void prerender (RenderList list) {
      }

      public void updateBounds (Vector3d pmin, Vector3d pmax) {
      }
   }

   public class KeyHandler extends KeyAdapter {
      GLViewer myViewer;

      KeyHandler() {
      }

      public KeyHandler (GLViewer myViewer) {
         this.myViewer = myViewer;
      }

      public void keyTyped (KeyEvent e) {
         if (info == null)
            return;
         switch (e.getKeyChar()) {
            case '1':
               System.out.println ("1");
               // info.incDisplayNumber();
               myViewer.rerender();
               break;
            case '2':
               System.out.println ("2");
               // info.decDisplayNumber();
               myViewer.rerender();
               break;
            default:
               System.out.print (e.getKeyCode() + " " + (int)e.getKeyChar());
         }
      }
   }
}
