package artisynth.core.mechmodels;

import java.io.*;
import java.util.*;

import maspack.geometry.*;
import maspack.util.*;
import maspack.matrix.*;
import maspack.spatialmotion.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

public class RigidCylinder extends RigidBody implements Wrappable {
   
   double myRadius = 1.0;

   private static double MACH_PREC = 1e-16;
   
   private class TransformConstrainer
      implements GeometryTransformer.Constrainer {
    
      public void apply (AffineTransform3dBase X) {

         // constrain the transform to uniform scaling in the x-y plane
         if (X instanceof AffineTransform3d) {
            if (!X.equals (AffineTransform3d.IDENTITY)) {

               AffineTransform3d XA = (AffineTransform3d)X;
               Matrix3d A = new Matrix3d(XA.A);

               // factor A into the polar decomposition A = Q P, then remove all
               // the off-diagonal terms of P, with P.m00 and P.m11 set to
               // identical scale factors than give the same change in cross
               // cylindrical area as P(0:1,0:1).
               PolarDecomposition3d pd = new PolarDecomposition3d(A);
               Matrix3d P = pd.getP();
               double sxy = Math.sqrt (Math.abs(P.m00*P.m11 - P.m01*P.m10));
               double sz = P.m22;
               A.setDiagonal (sxy, sxy, sz);
               A.mul (pd.getQ(), A);

               XA.A.set (A);
               XA.p.setZero();
            }
         }
         else if (X instanceof RigidTransform3d) {
            ((RigidTransform3d)X).set (RigidTransform3d.IDENTITY);
         }        
      }
      
      public boolean isReflecting() {
         return false;
      }
   }

   public RigidCylinder() {
      super (null);
   }

   public double getRadius() {
      return myRadius;
   }

   public void setRadius (double r) {
      if (r != myRadius) {
         myRadius = r;
      }
   }      

   public RigidCylinder (String name, double r, double h, double density) {
      this (name, r, h, density, 20);
   }

   public RigidCylinder (
      String name, double r, double h, double density, int nsides) {

      super (name);
      PolygonalMesh mesh = MeshFactory.createCylinder (r, h, nsides);
      setMesh (mesh, null);
      // body.setDensity (density);
      double mass = Math.PI*r*r*h*density;
      setInertia (SpatialInertia.createCylinderInertia (mass, r, h));
      myRadius = r;
      myTransformConstrainer = new TransformConstrainer();
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      pw.println ("radius=" + fmt.format(getRadius()));
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "radius")) {
         double r = rtok.scanNumber();
         setRadius (r);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   private int signCross2d (Point3d p0, Point3d p1) {
      double xprod = p0.x*p1.y - p0.y*p1.x;
      return xprod >= 0 ? 1 : -1;
   }

   public void surfaceTangent (
      Point3d pr, Point3d pa, Point3d p1, double lam, Vector3d sideNrm) {

      // transform p0 and p1 into local coordinates
      Point3d paloc = new Point3d(pa);
      Point3d p1loc = new Point3d(p1);    
      paloc.inverseTransform (getPose());
      p1loc.inverseTransform (getPose());

      // there are at most two tangent points. t0loc and t1loc are used to
      // store the values for these in local coordinates:
      Point3d t0loc = new Point3d();
      Point3d t1loc = new Point3d();

      double za = paloc.z;
      double z1 = p1loc.z;
      paloc.z = 0;
      p1loc.z = 0;

      if (QuadraticUtils.circleTangentPoints (
             t0loc, t1loc, paloc, myRadius) == 0) {
         // project p0 or p1 to the surface
         double mag = paloc.norm();
         if (mag != 0) {
            t0loc.scale (myRadius/mag, paloc);
         }
         else {
            t0loc.scale (myRadius/p1loc.norm(), p1loc);
         }
      }
      else if (signCross2d (paloc, p1loc) == signCross2d (paloc, t1loc)) {
         // use t1 instead of t0
         t0loc.set (t1loc);
      }
      double l = LineSegment.projectionParameter (paloc, p1loc, t0loc);
      t0loc.z = (1-l)*za + l*z1;
      pr.transform (getPose(), t0loc);
   }


   public double penetrationDistance (Vector3d nrm, Matrix3d dnrm, Point3d p0) {
      if (nrm == null) {
         nrm = new Vector3d();
      }
      Point3d p0loc = new Point3d(p0);
      p0loc.inverseTransform (getPose());
      nrm.set (p0loc);
      nrm.z = 0;
      double mag = nrm.norm();
      if (mag >= 1.5*myRadius) {
         return Wrappable.OUTSIDE;
      }
      else if (mag > 0) {
         nrm.scale (1/mag);
      }
      else {
         nrm.set (1, 0, 0);
      }
      if (dnrm != null) {
         if (mag > 0) {
            // dnrm = (I - nrm nrm^T)/mag, but can be computed faster term by term
            dnrm.setZero();
            dnrm.m00 = nrm.y*nrm.y;
            dnrm.m11 = nrm.x*nrm.x;
            dnrm.m01 = -nrm.x*nrm.y;
            dnrm.m10 = dnrm.m01;
            dnrm.scale (1.0/mag);
            dnrm.transform (getPose().R);
            //dnrm.mul (getPose().R, dnrm);
         }
         else {
            dnrm.setZero();
         }
      }
      nrm.transform (getPose());
      return mag-myRadius;
   }

   /**
    * {@inheritDoc}
    */
   public double getCharacteristicRadius() {
      return myRadius;
   }
   
   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // Update the radius. The appropriate scaling is determined by
      // applying the transform constrainer to the local affine transform
      // induced by the transformation. 
      if (gtr.isRestoring()) {
         myRadius = gtr.restoreObject (myRadius);
      }
      else { 
         if (gtr.isSaving()) {
            gtr.saveObject (myRadius);
         }
         AffineTransform3d XL = gtr.computeLocalAffineTransform (
            getPose(), myTransformConstrainer); 
         // need to take abs() since diagonal entries could be negative
         // if XL is a reflection
         myRadius *= Math.abs(XL.A.m00);
      }
      super.transformGeometry (gtr, context, flags);
   }

}
