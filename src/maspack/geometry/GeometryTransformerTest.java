package maspack.geometry;

import java.util.*;

import maspack.matrix.*;
import maspack.geometry.GeometryTransformer.UndoState;
import maspack.geometry.MeshFactory.FaceType;
import maspack.util.*;

public abstract class GeometryTransformerTest extends UnitTest {
   
   protected static final double EPS = 1e-12;
   /**
      points
      vectors
      normals
      RigidTransforms
      AffineTransforms
      RotationMatrix3d
      Matrix3d
      plane
      mesh
   */

   protected void flipColumn (Matrix3dBase M, int colNum) {
      switch (colNum) {
         case 0: {
            M.m00 = -M.m00; M.m10 = -M.m10; M.m20 = -M.m20;
            break;
         }
         case 1: {
            M.m01 = -M.m01; M.m11 = -M.m11; M.m21 = -M.m21;
            break;
         }
         case 2: {
            M.m02 = -M.m02; M.m12 = -M.m12; M.m22 = -M.m22;
            break;
         }
      }
   }

   protected int maxDiagIndex (Matrix3dBase M) {
      int maxIdx = 2;
      double maxd = M.m22;
      if (M.m11 > maxd) {
         maxd = M.m11;
         maxIdx = 1;
      }
      if (M.m00 > maxd) {
         maxIdx = 0;
      }
      return maxIdx;
   }

   protected abstract GeometryTransformer[] createTransformers();

   protected abstract void computePointCheck (
      Point3d pcheck, Point3d p0, GeometryTransformer gtr);

   void testPoints() {
      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();
      Point3d pcheck = new Point3d();

      GeometryTransformer[] gtrs = createTransformers();
      int testcnt = 50/gtrs.length;
      for (GeometryTransformer gtr : gtrs) {
         for (int i=0; i<testcnt; i++) {
            p0.setRandom();
            computePointCheck (pcheck, p0, gtr);
            gtr.setUndoState (UndoState.OFF);
            gtr.transformPnt (p1, p0);
            checkEquals ("transformed point", p1, pcheck, EPS);
            gtr.setUndoState (UndoState.SAVING);
            gtr.transformPnt (p1, p0);
            checkEquals ("transformed point", p1, pcheck, EPS);
            gtr.setUndoState (UndoState.RESTORING);
            gtr.transformPnt (p1, p1);
            checkEquals ("restored point", p1, p0, 0);
         }
      }
   }

   protected abstract void computeVectorCheck (
      Vector3d vcheck, Vector3d v0, Vector3d r, GeometryTransformer gtr);

   void testVectors() {
      Vector3d v0 = new Vector3d();
      Vector3d v1 = new Vector3d();
      Vector3d vcheck = new Vector3d();
      Vector3d r = new Vector3d();

      GeometryTransformer[] gtrs = createTransformers();
      int testcnt = 50/gtrs.length;
      for (GeometryTransformer gtr : gtrs) {
         for (int i=0; i<testcnt; i++) {
            r.setRandom();
            v0.setRandom();
            computeVectorCheck (vcheck, v0, r, gtr);
            gtr.setUndoState (UndoState.OFF);
            gtr.transformVec (v1, v0, r);
            checkEquals ("transformed vector", v1, vcheck, EPS);
            gtr.setUndoState (UndoState.SAVING);
            gtr.transformVec (v1, v0, r);
            checkEquals ("transformed vector", v1, vcheck, EPS);
            gtr.setUndoState (UndoState.RESTORING);
            gtr.transformVec (v1, v1, r);
            checkEquals ("restored vector", v1, v0, 0);
         }
      }
   }

   protected abstract void computeNormalCheck (
      Vector3d ncheck, Vector3d n0, Vector3d r, GeometryTransformer gtr);

   void testNormals() {
      Vector3d n0 = new Vector3d();
      Vector3d n1 = new Vector3d();
      Vector3d ncheck = new Vector3d();
      Vector3d r = new Vector3d();

      GeometryTransformer[] gtrs = createTransformers();
      int testcnt = 50/gtrs.length;
      for (GeometryTransformer gtr : gtrs) {
         for (int i=0; i<testcnt; i++) {
            r.setRandom();
            n0.setRandom();
            computeNormalCheck (ncheck, n0, r, gtr);
            gtr.setUndoState (UndoState.OFF);
            gtr.transformNormal (n1, n0, r);
            checkEquals ("transformed normal", n1, ncheck, EPS);
            gtr.setUndoState (UndoState.SAVING);
            gtr.transformNormal (n1, n0, r);
            checkEquals ("transformed normal", n1, ncheck, EPS);
            gtr.setUndoState (UndoState.RESTORING);
            gtr.transformNormal (n1, n1, r);
            checkEquals ("restored normal", n1, n0, 0);
         }
      }
   }

   protected abstract void computeRigidTransformCheck (
      RigidTransform3d Tcheck, RigidTransform3d T0, 
      GeometryTransformer gtr);

   void testRigidTransforms() {
      RigidTransform3d T0 = new RigidTransform3d();
      RigidTransform3d T1 = new RigidTransform3d();
      RigidTransform3d Tcheck = new RigidTransform3d();

      GeometryTransformer[] gtrs = createTransformers();
      int testcnt = 50/gtrs.length;
      for (GeometryTransformer gtr : gtrs) {
         for (int i=0; i<testcnt; i++) {
            T0.setRandom();
            computeRigidTransformCheck (Tcheck, T0, gtr);
            gtr.setUndoState (UndoState.OFF);
            gtr.transform (T1, T0);
            checkEquals ("transformed rigid transform", T1, Tcheck, EPS);
            gtr.setUndoState (UndoState.SAVING);
            gtr.transform (T1, T0);
            checkEquals ("transformed rigid transform", T1, Tcheck, EPS);
            gtr.setUndoState (UndoState.RESTORING);
            gtr.transform (T1, T1);
            checkEquals ("restored rigid transform", T1, T0, 0);
         }
      }
   }

   protected abstract void computeAffineTransformCheck (
      AffineTransform3d Xcheck, AffineTransform3d X0, 
      GeometryTransformer gtr);

   void testAffineTransforms() {
      AffineTransform3d X0 = new AffineTransform3d();
      AffineTransform3d X1 = new AffineTransform3d();
      AffineTransform3d Xcheck = new AffineTransform3d();

      GeometryTransformer[] gtrs = createTransformers();
      int testcnt = 50/gtrs.length;
      for (GeometryTransformer gtr : gtrs) {
         for (int i=0; i<testcnt; i++) {
            X0.setRandom();
            computeAffineTransformCheck (Xcheck, X0, gtr);
            gtr.setUndoState (UndoState.OFF);
            gtr.transform (X1, X0);
            checkEquals ("transformed affine transform", X1, Xcheck, EPS);
            gtr.setUndoState (UndoState.SAVING);
            gtr.transform (X1, X0);
            checkEquals ("transformed affine transform", X1, Xcheck, EPS);
            gtr.setUndoState (UndoState.RESTORING);
            gtr.transform (X1, X1);
            checkEquals ("restored affine transform", X1, X0, 0);
         }
      }
   }

   protected abstract void computeRotationMatrixCheck (
      RotationMatrix3d Rcheck, RotationMatrix3d R0, Vector3d r,
      GeometryTransformer gtr);

   void testRotationMatrices() {
      RotationMatrix3d R0 = new RotationMatrix3d();
      RotationMatrix3d R1 = new RotationMatrix3d();
      RotationMatrix3d Rcheck = new RotationMatrix3d();
      Vector3d r = new Vector3d();

      GeometryTransformer[] gtrs = createTransformers();
      int testcnt = 50/gtrs.length;
      for (GeometryTransformer gtr : gtrs) {
         for (int i=0; i<testcnt; i++) {
            r.setRandom();
            R0.setRandom();
            computeRotationMatrixCheck (Rcheck, R0, r, gtr);
            gtr.setUndoState (UndoState.OFF);
            gtr.transform (R1, R0, r);
            checkEquals ("transformed rotation matrix", R1, Rcheck, EPS);
            gtr.setUndoState (UndoState.SAVING);
            gtr.transform (R1, R0, r);
            checkEquals ("transformed rotation matrix", R1, Rcheck, EPS);
            gtr.setUndoState (UndoState.RESTORING);
            gtr.transform (R1, R1, r);
            checkEquals ("restored rotation matrix", R1, R0, 0);
         }
      }
   }

   protected abstract void computeMatrixCheck (
      Matrix3d Mcheck, Matrix3d M0, Vector3d r,
      GeometryTransformer gtr);

   void testMatrices() {
      Matrix3d M0 = new Matrix3d();
      Matrix3d M1 = new Matrix3d();
      Matrix3d Mcheck = new Matrix3d();
      Vector3d r = new Vector3d();

      GeometryTransformer[] gtrs = createTransformers();
      int testcnt = 50/gtrs.length;
      for (GeometryTransformer gtr : gtrs) {
         for (int i=0; i<testcnt; i++) {
            r.setRandom();
            M0.setRandom();
            computeMatrixCheck (Mcheck, M0, r, gtr);
            gtr.setUndoState (UndoState.OFF);
            gtr.transform (M1, M0, r);
            checkEquals ("transformed matrix", M1, Mcheck, EPS);
            gtr.setUndoState (UndoState.SAVING);
            gtr.transform (M1, M0, r);
            checkEquals ("transformed matrix", M1, Mcheck, EPS);
            gtr.setUndoState (UndoState.RESTORING);
            gtr.transform (M1, M1, r);
            checkEquals ("restored matrix", M1, M0, 0);
         }
      }
   }

   protected abstract void computePlaneCheck (
      Plane pcheck, Plane p0, Vector3d r,
      GeometryTransformer gtr);

   public void checkEquals (String msg, Plane result, Plane check, double eps) {
      if (!result.epsilonEquals(check, eps)) {
         throw new TestException (
            msg + result.toString() +
            ", expected " + check.toString() + ", eps=" + eps);
      }
   }

   void testPlanes() {
      Plane p0 = new Plane();
      Plane p1 = new Plane();
      Plane pcheck = new Plane();
      Vector3d r = new Vector3d();

      GeometryTransformer[] gtrs = createTransformers();
      int testcnt = 50/gtrs.length;
      for (GeometryTransformer gtr : gtrs) {
         for (int i=0; i<testcnt; i++) {
            r.setRandom();
            p0.setRandom();
            computePlaneCheck (pcheck, p0, r, gtr);
            gtr.setUndoState (UndoState.OFF);
            gtr.transform (p1, p0, r);

            checkEquals ("transformed plane", p1, pcheck, EPS);
            gtr.setUndoState (UndoState.SAVING);
            gtr.transform (p1, p0, r);
            checkEquals ("transformed plane", p1, pcheck, EPS);
            gtr.setUndoState (UndoState.RESTORING);
            gtr.transform (p1, p1, r);
            checkEquals ("restored plane", p1, p0, 0);
         }
      }
   }

   protected abstract PolygonalMesh computeMeshCheck (
      PolygonalMesh mesh, GeometryTransformer gtr);

   protected abstract PolygonalMesh computeMeshWorldCheck (
      PolygonalMesh mesh, GeometryTransformer gtr);

   void checkMeshEquals (PolygonalMesh mesh, PolygonalMesh check, double eps) {
      checkEquals (
         "MeshToWorld",
         mesh.getMeshToWorld(), check.getMeshToWorld(), eps);
      for (int i=0; i<mesh.numVertices(); i++) {
         Point3d p = mesh.getVertices().get(i).pnt;
         Point3d pcheck = check.getVertices().get(i).pnt;
         checkEquals ("mesh vertex "+i, p, pcheck, eps);
      }
      if (mesh.hasNormals() != check.hasNormals()) {
         throw new TestException (
            "mesh.hasNormals()=" + mesh.hasNormals() +
            ", expecting " + check.hasNormals());
      }
      if (mesh.hasNormals()) {
         for (int i=0; i<mesh.numNormals(); i++) {
            Vector3d n = mesh.getNormal(i);
            Vector3d ncheck = check.getNormal(i);
            checkEquals ("mesh normal "+i, n, ncheck, eps);
         }
      }
   }

   void testMesh (PolygonalMesh mesh) {

      GeometryTransformer[] gtrs = createTransformers();
      for (int k=0; k<gtrs.length; k++) {
         GeometryTransformer gtr = gtrs[k];

         PolygonalMesh checkmesh = computeMeshCheck (mesh, gtr);
         PolygonalMesh testmesh = mesh.copy();      
         gtr.setUndoState (UndoState.OFF);
         gtr.transform (testmesh);
         checkMeshEquals (testmesh, checkmesh, EPS);

         gtr.setUndoState (UndoState.SAVING);
         testmesh = mesh.copy();
         gtr.transform (testmesh);
         checkMeshEquals (testmesh, checkmesh, EPS);

         gtr.setUndoState (UndoState.RESTORING);
         gtr.transform (testmesh);
         checkMeshEquals (testmesh, mesh, 0);

         checkmesh = computeMeshWorldCheck (mesh, gtr);
         testmesh = mesh.copy();
         gtr.setUndoState (UndoState.OFF);
         gtr.transformWorld (testmesh, null);
         checkMeshEquals (testmesh, checkmesh, EPS);

         gtr.setUndoState (UndoState.SAVING);
         testmesh = mesh.copy();
         gtr.transformWorld (testmesh, null);
         checkMeshEquals (testmesh, checkmesh, EPS);

         gtr.setUndoState (UndoState.RESTORING);
         gtr.transformWorld (testmesh, null);
         checkMeshEquals (testmesh, mesh, 0);
      }
      
   }

   void testMeshes() {
      // need to be careful with the meshes because the some of the test
      // deformation fields have singularities at locations like (-2,0,0) or
      // (0,-1,0)

      PolygonalMesh mesh =
         MeshFactory.createIcosahedralSphere (1.9, 1);
      // mesh has auto normal creation
      testMesh (mesh);

      // explictly clear normals
      mesh.setNormals (null, null); 
      testMesh (mesh);

      // explcitly set vertex normals
      mesh.clearNormals();
      mesh.setNormals (mesh.getNormals(), mesh.getNormalIndices());
      testMesh (mesh);

      // explicitly set face normals
      ArrayList<Vector3d> faceNormals = new ArrayList<Vector3d>();
      mesh.updateFaceNormals();
      for (Face f : mesh.getFaces()) {
         faceNormals.add (f.getNormal());
      }
      mesh.setNormals (faceNormals, mesh.createFeatureIndices());
      testMesh (mesh);

      // face normals, quads
      mesh = MeshFactory.createBox (
         3.3, 2.2, 1, Point3d.ZERO, 2, 1, 1, true, FaceType.QUAD);
      testMesh (mesh);

      // face normals, triangles
      mesh = MeshFactory.createBox (
         3.3, 2.2, 1, Point3d.ZERO, 2, 1, 1, true, FaceType.TRI);
      testMesh (mesh);
   }

   public void test() {
      testPoints();
      testVectors();
      testNormals();
      testRigidTransforms();
      testAffineTransforms();
      testRotationMatrices();
      testMatrices();
      testPlanes();
      testMeshes();
   }
}

