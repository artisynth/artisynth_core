package maspack.geometry;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

public class DeformationTransformerTest extends GeometryTransformerTest { 

   /**
    * Implements a synthetic deformation defined by
    *
    * x' = x + 1/4 x^2 + 1/4 x y
    * y' = y + 1/2 y^2 + 1/4 y z
    * z' = z + 1/4 z^2 + 1/4 z x
    */
   class MyTransformer extends DeformationTransformer {
      
      public void getDeformation (Vector3d p, Matrix3d F, Vector3d r) {
         double x = r.x;
         double y = r.y;
         double z = r.z;

         if (p != null) {
            p.x = x + 0.25*x*x + 0.25*x*y;
            p.y = y + 0.50*y*y + 0.25*x*y;
            p.z = z + 0.25*z*z + 0.25*z*x;
         }
         
         if (F != null) {
            F.m00 = 1 + 0.5*x + 0.25*y; 
            F.m01 = 0.25*x;
            F.m02 = 0;

            F.m10 = 0;
            F.m11 = 1 + y + 0.25*z; 
            F.m12 = 0.25*y;

            F.m20 = 0.25*z;
            F.m21 = 0;
            F.m22 = 1 + 0.5*z + 0.25*x;
         }
      }
   }

   /**
    * Implements a synthetic reflecting deformation defined by
    *
    * x' = -x + 1/4 x^2 - 1/4 x y
    * y' = y + 1/2 y^2 + 1/4 y z
    * z' = z + 1/4 z^2 - 1/4 z x
    */
   class MyReflector extends DeformationTransformer {
      
      public void getDeformation (Vector3d p, Matrix3d F, Vector3d r) {
         double x = r.x;
         double y = r.y;
         double z = r.z;

         if (p != null) {
            p.x = -x + 0.25*x*x - 0.25*x*y;
            p.y = y + 0.50*y*y + 0.25*x*y;
            p.z = z + 0.25*z*z - 0.25*z*x;
         }
         
         if (F != null) {
            F.m00 = -1 + 0.5*x - 0.25*y; 
            F.m01 = -0.25*x;
            F.m02 = 0;

            F.m10 = 0;
            F.m11 = 1 + y + 0.25*z; 
            F.m12 = 0.25*y;

            F.m20 = -0.25*z;
            F.m21 = 0;
            F.m22 = 1 + 0.5*z - 0.25*x;
         }
      }
   }

   protected GeometryTransformer[] createTransformers() {
      return new GeometryTransformer[] {
         new MyTransformer(), new MyReflector() };
   }
 
   protected void computePointCheck (
      Point3d pcheck, Point3d p0, GeometryTransformer gtr) {
      ((DeformationTransformer)gtr).getDeformation (pcheck, null, p0);
   }

   protected void computeVectorCheck (
      Vector3d vcheck, Vector3d v0, Vector3d r, GeometryTransformer gtr) {
      Matrix3d F = new Matrix3d();
      ((DeformationTransformer)gtr).getDeformation (null, F, r);
      F.mul (vcheck, v0);
   }

   protected void computeNormalCheck (
      Vector3d ncheck, Vector3d n0, Vector3d r, GeometryTransformer gtr) {
      Matrix3d F = new Matrix3d();
      ((DeformationTransformer)gtr).getDeformation (null, F, r);
      F.mulInverseTranspose (ncheck, n0);
   }

   protected void computeRigidTransformCheck (
      RigidTransform3d Tcheck, RigidTransform3d T0, 
      GeometryTransformer gtr) {

      Matrix3d F = new Matrix3d();
      ((DeformationTransformer)gtr).getDeformation (Tcheck.p, F, T0.p);
      computeRotationMatrixCheck (Tcheck.R, T0.R, F);
   }

   protected void computeAffineTransformCheck (
      AffineTransform3d Xcheck, AffineTransform3d X0, 
      GeometryTransformer gtr) {

      Matrix3d F = new Matrix3d();
      ((DeformationTransformer)gtr).getDeformation (Xcheck.p, F, X0.p);
      Xcheck.A.mul (F, X0.A);
   }

   void computeRotationMatrixCheck (
      RotationMatrix3d Rcheck, RotationMatrix3d R0, Matrix3dBase F) {

      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factorLeft (F);
      Matrix3d Q = new Matrix3d();
      polard.getQ(Q);
      Rcheck.mul (Q, R0);
      if (Q.determinant() < 0) {
         flipColumn (Rcheck, maxDiagIndex (Q));
      }
   }

   protected void computeRotationMatrixCheck (
      RotationMatrix3d Rcheck, RotationMatrix3d R0, Vector3d r,
      GeometryTransformer gtr) {

      Matrix3d F = new Matrix3d();
      ((DeformationTransformer)gtr).getDeformation (null, F, r);
      computeRotationMatrixCheck (Rcheck, R0, F);
   }

   protected void computeMatrixCheck (
      Matrix3d Mcheck, Matrix3d M0, Vector3d r,
      GeometryTransformer gtr) {

      Matrix3d F = new Matrix3d();
      ((DeformationTransformer)gtr).getDeformation (null, F, r);
      Mcheck.mul (F, M0);
   }

   protected void computePlaneCheck (
      Plane pcheck, Plane p0, Vector3d r,
      GeometryTransformer gtr) {

      Matrix3d F = new Matrix3d();
      Vector3d p = new Vector3d(); 
      ((DeformationTransformer)gtr).getDeformation (p, F, r);     
      Vector3d nrm = new Vector3d();
      F.mulInverseTranspose (nrm, p0.normal);
      nrm.normalize();
      pcheck.set (nrm, nrm.dot(p));
   }

   private Point3d[] getNormalRefs (PolygonalMesh mesh) {
      Point3d[] refs = new Point3d[mesh.numNormals()];
      if (refs.length > 0) {
         int[] indices = mesh.getNormalIndices();
         int[] featureIndices = mesh.createFeatureIndices();
         int[] vertexIndices = mesh.createVertexIndices();
         if (Arrays.equals (indices, featureIndices)) {
            for (int i=0; i<mesh.numFaces(); i++) {
               Face face = mesh.getFace(i);
               refs[i] = new Point3d();
               face.computeCentroid (refs[i]);               
            }
         }
         else if (Arrays.equals (indices, vertexIndices)) {
            for (int i=0; i<mesh.numVertices(); i++) {
               refs[i] = mesh.getVertex(i).pnt;
            }
         }
         else {
            int[] cnts = new int[mesh.numNormals()];
            for (int i=0; i<refs.length; i++) {
               refs[i] = new Point3d();
            }
            for (int i=0; i<indices.length; i++) {
               refs[indices[i]].add (mesh.getVertex(vertexIndices[i]).pnt);
               cnts[indices[i]]++;
            }
            for (int i=0; i<refs.length; i++) {
               refs[i].scale (1.0/cnts[i]);
            }
         }
      }
      return refs;
   }

   protected PolygonalMesh computeMeshCheck (
      PolygonalMesh mesh, GeometryTransformer gtr) {
      mesh = mesh.copy();

      if (mesh.hasNormals()) {
         if (mesh.hasExplicitNormals()) {
            Point3d[] refs = getNormalRefs(mesh);
            for (int i=0; i<mesh.numNormals(); i++) {
               Vector3d n = mesh.getNormal(i);
               computeNormalCheck (n, n, refs[i], gtr);
               n.normalize();
            }
         }
         else {
            mesh.clearNormals();
         }
      }
      for (Vertex3d v : mesh.getVertices()) {
         computePointCheck (v.pnt, v.pnt, gtr);
      }
      return mesh;
   }

   protected PolygonalMesh computeMeshWorldCheck (
      PolygonalMesh mesh, GeometryTransformer gtr) {
      mesh = mesh.copy();

      mesh = mesh.copy();
      RigidTransform3d TMWold = new RigidTransform3d(mesh.getMeshToWorld());
      RigidTransform3d TMWnew = new RigidTransform3d();
      computeRigidTransformCheck (TMWnew, TMWold, gtr);
      mesh.setMeshToWorld(TMWnew);

      if (mesh.hasNormals()) {
         if (mesh.hasExplicitNormals()) {
            Point3d[] refs = getNormalRefs(mesh);            
            for (int i=0; i<mesh.numNormals(); i++) {
               Vector3d n = mesh.getNormal(i);
               n.transform (TMWold);
               computeNormalCheck (n, n, refs[i], gtr);
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
         computePointCheck (v.pnt, v.pnt, gtr);
         v.pnt.inverseTransform (TMWnew);
      }
      return mesh;
   }


   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      DeformationTransformerTest tester = new DeformationTransformerTest();
      tester.runtest();
   }   

}
