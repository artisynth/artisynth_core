package artisynth.core.mechmodels;

import java.io.*;
import java.util.*;

import maspack.geometry.*;
import maspack.util.*;
import maspack.matrix.*;
import maspack.spatialmotion.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

public class RigidEllipsoid extends RigidBody implements Wrappable {
   
   Vector3d myAxisLengths;
   private static double MACH_PREC = 1e-16;

   private class TransformConstrainer
      implements GeometryTransformer.Constrainer {

      public void apply (AffineTransform3dBase X) {

         // constrain the transform to simple scaling along the x, y, z axes
         if (X instanceof AffineTransform3d && 
             !X.equals (AffineTransform3d.IDENTITY)) {
            AffineTransform3d XA = (AffineTransform3d)X;

            PolarDecomposition3d pd = null;
            Matrix3d P = XA.A;
            if (!P.isSymmetric (1000*MACH_PREC*P.oneNorm())) {
               pd = new PolarDecomposition3d();
               pd.factor (P);
               P = pd.getP();
            }
            XA.A.setDiagonal (P.m00, P.m11, P.m22);
            if (pd != null) {
               XA.A.mul (pd.getR(), XA.A);
            }
         }
      }
   }

   public RigidEllipsoid() {
      super (null);
      myAxisLengths = new Vector3d();
   }

   public void getAxisLengths (Vector3d lengths) {
      myAxisLengths.get (lengths);
   }

   public Vector3d getAxisLengths () {
      return new Vector3d(myAxisLengths);
   }

   public void setAxisLengths (Vector3d lengths) {
      myAxisLengths.set (lengths);
   }

   public RigidEllipsoid (
      String name, double a, double b, double c, double density) {
      this (name, a, b, c, density, 20);
   }

   public RigidEllipsoid (
      String name, double a, double b, double c, double density, int nslices) {
      super (name);      
      myAxisLengths = new Vector3d(a, b, c);
      PolygonalMesh mesh = MeshFactory.createSphere (1.0, nslices);
      AffineTransform3d XScale = new AffineTransform3d();
      XScale.applyScaling (a, b, c);
      mesh.transform (XScale);
      setMesh (mesh, null);
      double mass = 4/3.0*Math.PI*a*b*c*density;
      setInertia (SpatialInertia.createEllipsoidInertia (mass, a, b, c));
      myTransformConstrainer = new TransformConstrainer();
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      pw.print ("axisLengths=");
      myAxisLengths.write (pw, fmt, /*withBrackets=*/true);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "axisLengths")) {
         myAxisLengths.scan (rtok);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public void surfaceTangent (
      Point3d pr, Point3d pa, Point3d p1, double lam0, Vector3d sideNrm) {

      double a = myAxisLengths.x;
      double b = myAxisLengths.y;
      double c = myAxisLengths.z;

      Point3d loca = new Point3d(pa);
      Point3d loc1 = new Point3d(p1);
      loca.inverseTransform (getPose());
      loc1.inverseTransform (getPose());
      QuadraticUtils.ellipsoidSurfaceTangent (
         pr, loca, loc1, a, b, c);
      pr.transform (getPose());
      
   }

   public double penetrationDistance (Vector3d nrm, Matrix3d dnrm, Point3d p0) {
      double a = myAxisLengths.x;
      double b = myAxisLengths.y;
      double c = myAxisLengths.z;

      Point3d loc0 = new Point3d(p0);
      loc0.inverseTransform (getPose());
      double d = QuadraticUtils.ellipsoidPenetrationDistance (
         nrm, loc0, a, b, c);
      if (dnrm != null) {
         dnrm.setZero();
      }
      nrm.transform (getPose());
      return d;
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // Update the axis lengths. The appropriate scaling is determined by
      // applying the transform constrainer to the local affine transform
      // induced by the transformation. 
      if (gtr.isRestoring()) {
         myAxisLengths.set (gtr.restoreObject (myAxisLengths));
      }
      else {
         if (gtr.isSaving()) {
            gtr.saveObject (new Vector3d(myAxisLengths));
         }
         AffineTransform3d XL = gtr.computeRightAffineTransform (getPose());
         myTransformConstrainer.apply (XL);
         myAxisLengths.x *= XL.A.m00;
         myAxisLengths.y *= XL.A.m11;
         myAxisLengths.z *= XL.A.m22;
      }   
      super.transformGeometry (gtr, context, flags);
   }

}
