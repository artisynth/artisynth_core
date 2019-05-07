package maspack.geometry;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

public class AffineTransformerTest extends GeometryTransformerTest { 

   protected GeometryTransformer[] createTransformers() {
      int numTransformers = 10;

      AffineTransform3d Xlist[] = new AffineTransform3d[numTransformers];
      // create random AffineTransforms, with an equal number reflective and
      // non-reflective.
      for (int i=0; i<Xlist.length; i++) {
         Xlist[i] = new AffineTransform3d();
         Xlist[i].setRandom();
         if (i < numTransformers/2) {
            if (Xlist[i].A.determinant() < 0) {
               Xlist[i].A.negateColumn(2);
            }
         }
         else {
            if (Xlist[i].A.determinant() > 0) {
               Xlist[i].A.negateColumn(2);
            }           
         }
      }
      GeometryTransformer[] transformers =
         new GeometryTransformer[Xlist.length];
      for (int i=0; i<Xlist.length; i++) {
         transformers[i] = new AffineTransformer(Xlist[i]);
      }
      return transformers;
   }

   protected void computePointCheck (
      Point3d pcheck, Point3d p0, GeometryTransformer gtr) {
      AffineTransform3d X = ((AffineTransformer)gtr).myX;
      pcheck.transform (X, p0);
   }

   protected void computeVectorCheck (
      Vector3d vcheck, Vector3d v0, Vector3d r, GeometryTransformer gtr) {
      AffineTransform3d X = ((AffineTransformer)gtr).myX;
      vcheck.transform (X, v0);
   }

   protected void computeNormalCheck (
      Vector3d ncheck, Vector3d n0, Vector3d r, GeometryTransformer gtr) {
      AffineTransform3d X = ((AffineTransformer)gtr).myX;
      X.A.mulInverseTranspose (ncheck, n0);
   }

   protected void computeRigidTransformCheck (
      RigidTransform3d Tcheck, RigidTransform3d T0, 
      GeometryTransformer gtr) {
      AffineTransform3d X = ((AffineTransformer)gtr).myX;
      computeRotationMatrixCheck (Tcheck.R, T0.R, X);
      X.A.mul (Tcheck.p, T0.p);
      Tcheck.p.add (X.p);
   }

   protected void computeAffineTransformCheck (
      AffineTransform3d Xcheck, AffineTransform3d X0, 
      GeometryTransformer gtr) {
      AffineTransform3d X = ((AffineTransformer)gtr).myX;
      Xcheck.mul (X, X0);
   }

   void computeRotationMatrixCheck (
      RotationMatrix3d Rcheck, RotationMatrix3d R0, AffineTransform3d X) {

      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factorLeft (X.A);
      Matrix3d Q = new Matrix3d();
      polard.getQ(Q);
      Rcheck.mul (Q, R0);
      if (Q.determinant() < 0) {
         flipColumn (Rcheck, maxDiagIndex(Q));
      }
   }

   protected void computeRotationMatrixCheck (
      RotationMatrix3d Rcheck, RotationMatrix3d R0, Vector3d r,
      GeometryTransformer gtr) {
      AffineTransform3d X = ((AffineTransformer)gtr).myX;

      computeRotationMatrixCheck (Rcheck, R0, X);
   }

   protected void computeMatrixCheck (
      Matrix3d Mcheck, Matrix3d M0, Vector3d r,
      GeometryTransformer gtr) {
      AffineTransform3d X = ((AffineTransformer)gtr).myX;
      Mcheck.mul (X.A, M0);
   }

   protected void computePlaneCheck (
      Plane pcheck, Plane p0, Vector3d r,
      GeometryTransformer gtr) {
      AffineTransform3d X = ((AffineTransformer)gtr).myX;
      pcheck.transform (X, p0);
   }

   protected PolygonalMesh computeMeshCheck (
      PolygonalMesh mesh, GeometryTransformer gtr) {
      AffineTransform3d X = ((AffineTransformer)gtr).myX;
      mesh = mesh.copy();

      if (mesh.hasNormals()) {
         if (mesh.hasExplicitNormals()) {
            for (Vector3d n : mesh.getNormals()) {
               X.A.mulInverseTranspose (n, n);
               n.normalize();
            }
         }
         else {
            mesh.clearNormals();
         }
      }
      for (Vertex3d v : mesh.getVertices()) {
         v.pnt.transform (X);
      }
      return mesh;
   }

   protected PolygonalMesh computeMeshWorldCheck (
      PolygonalMesh mesh, GeometryTransformer gtr) {
      AffineTransform3d X = ((AffineTransformer)gtr).myX;
      mesh = mesh.copy();
      RigidTransform3d TMWold = new RigidTransform3d(mesh.getMeshToWorld());
      RigidTransform3d TMWnew = new RigidTransform3d();
      computeRigidTransformCheck (TMWnew, TMWold, gtr);
      mesh.setMeshToWorld(TMWnew);

      if (mesh.hasNormals()) {
         if (mesh.hasExplicitNormals()) {
            for (Vector3d n : mesh.getNormals()) {
               n.transform (TMWold);
               X.A.mulInverseTranspose (n, n);
               n.normalize();
               n.inverseTransform (TMWnew);
            }
         }
         else {
            mesh.clearNormals();
         }
      }
      for (Vertex3d v : mesh.getVertices()) {
         v.pnt.transform (TMWold);
         v.pnt.transform (X);
         v.pnt.inverseTransform (TMWnew);
      }
      return mesh;
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      AffineTransformerTest tester = new AffineTransformerTest();
      tester.runtest();
   }   

}
