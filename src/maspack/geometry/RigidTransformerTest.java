package maspack.geometry;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

public class RigidTransformerTest extends GeometryTransformerTest {

   protected GeometryTransformer[] createTransformers() {
      GeometryTransformer[] transformers = new GeometryTransformer[10];
      RigidTransform3d T = new RigidTransform3d();
      for (int i=0; i<transformers.length; i++) {
         T.setRandom();
         transformers[i] = new RigidTransformer(T);
      }
      return transformers;
   }

   protected void computePointCheck (
      Point3d pcheck, Point3d p0, GeometryTransformer gtr) {
      RigidTransform3d T = ((RigidTransformer)gtr).myT;
      pcheck.transform (T, p0);
   }

   protected void computeVectorCheck (
      Vector3d vcheck, Vector3d v0, Vector3d r, GeometryTransformer gtr) {
      RigidTransform3d T = ((RigidTransformer)gtr).myT;
      vcheck.transform (T, v0);
   }

   protected void computeNormalCheck (
      Vector3d ncheck, Vector3d n0, Vector3d r, GeometryTransformer gtr) {
      RigidTransform3d T = ((RigidTransformer)gtr).myT;
      ncheck.transform (T, n0);
   }

   protected void computeRigidTransformCheck (
      RigidTransform3d Tcheck, RigidTransform3d T0, 
      GeometryTransformer gtr) {
      RigidTransform3d T = ((RigidTransformer)gtr).myT;
      Tcheck.mul (T, T0);
   }

   protected void computeAffineTransformCheck (
      AffineTransform3d Xcheck, AffineTransform3d X0, 
      GeometryTransformer gtr) {
      RigidTransform3d T = ((RigidTransformer)gtr).myT;
      Xcheck.mul (T, X0);
   }

   protected void computeRotationMatrixCheck (
      RotationMatrix3d Rcheck, RotationMatrix3d R0, Vector3d r,
      GeometryTransformer gtr) {
      RigidTransform3d T = ((RigidTransformer)gtr).myT;
      Rcheck.mul (T.R, R0);
   }

   protected void computeMatrixCheck (
      Matrix3d Mcheck, Matrix3d M0, Vector3d r,
      GeometryTransformer gtr) {
      RigidTransform3d T = ((RigidTransformer)gtr).myT;
      Mcheck.mul (T.R, M0);
   }

   protected void computePlaneCheck (
      Plane pcheck, Plane p0, Vector3d r,
      GeometryTransformer gtr) {
      RigidTransform3d T = ((RigidTransformer)gtr).myT;
      pcheck.transform (T, p0);
   }

   protected PolygonalMesh computeMeshCheck (
      PolygonalMesh mesh, GeometryTransformer gtr) {
      RigidTransform3d T = ((RigidTransformer)gtr).myT;
      mesh = mesh.copy();
      // modify normals first, since if there are auto generated they
      // will be generated from the current vertex values
      if (mesh.hasNormals()) {
         for (Vector3d n : mesh.getNormals()) {
            n.transform (T);
         }
      }
      for (Vertex3d v : mesh.getVertices()) {
         v.pnt.transform (T);
      }
      return mesh;
   }

   protected PolygonalMesh computeMeshWorldCheck (
      PolygonalMesh mesh, GeometryTransformer gtr) {
      RigidTransform3d T = ((RigidTransformer)gtr).myT;
      mesh = mesh.copy();
      RigidTransform3d TMW = new RigidTransform3d(mesh.getMeshToWorld());
      TMW.mul (T, TMW);
      mesh.setMeshToWorld (TMW);
      return mesh;
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      RigidTransformerTest tester = new RigidTransformerTest();
      tester.runtest();
   }   

}
