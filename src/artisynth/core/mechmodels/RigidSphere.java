package artisynth.core.mechmodels;

import java.io.*;
import java.util.*;

import maspack.geometry.*;
import maspack.util.*;
import maspack.matrix.*;
import maspack.spatialmotion.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

public class RigidSphere extends RigidBody implements Wrappable {
   
   double myRadius = 1.0;
   private static double MACH_PREC = 1e-16;

   public RigidSphere() {
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

   public RigidSphere (String name, double r, double density) {
      this (name, r, density, 20);
   }

   public RigidSphere (String name, double r, double density, int nslices) {
      super (name);      
      PolygonalMesh mesh = MeshFactory.createSphere (r, nslices);
      setMesh (mesh, null);
      // body.setDensity (density);
      
      double mass = 4/3.0*Math.PI*r*r*r*density;
      setInertia (SpatialInertia.createSphereInertia (mass, r));
      myRadius = r;
      myTransformConstrainer = 
         new GeometryTransformer.UniformScalingConstrainer();
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
      Point3d pr, Point3d p0, Point3d p1, double lam, Vector3d sideNrm) {

      RigidTransform3d XBW = new RigidTransform3d();
      Vector3d del10 = new Vector3d();
      Vector3d delc0 = new Vector3d();
      Vector3d xprod = new Vector3d();

      del10.sub (p1, p0);
      delc0.sub (getPose().p, p0);
      xprod.cross (delc0, del10);
      double mag = xprod.norm();
      if (mag <= 100*MACH_PREC*Math.max(del10.norm(), delc0.norm())) {
         // use sideNrm instead
         XBW.R.setZDirection (sideNrm);
      }
      else {
         XBW.R.setZDirection (xprod);
      }
      XBW.p.set (getPose().p);

      // transform into this coordinate system and so a circle tangent
      // calculation

      Point3d p0loc = new Point3d(p0);
      Point3d p1loc = new Point3d(p1);    
      p0loc.inverseTransform (XBW);
      p1loc.inverseTransform (XBW);

      Point3d t0loc = new Point3d();
      Point3d t1loc = new Point3d();

      if (QuadraticUtils.circleTangentPoints (
             t0loc, t1loc, p0loc, myRadius) == 0) {
         // project pr to the surface
         mag = p0loc.norm();
         if (mag != 0) {
            t0loc.scale (myRadius/mag, p0loc);
         }
         else {
            t0loc.scale (myRadius/p1loc.norm(), p1loc);
         }
      }
      else if (signCross2d (p0loc, p1loc) == signCross2d (p0loc, t1loc)) {
         // use t1 instead of t0
         t0loc.set (t1loc);
      }
      pr.transform (XBW, t0loc);
   }

   public double penetrationDistance (Vector3d nrm, Matrix3d dnrm, Point3d p0) {
      if (nrm == null) {
         nrm = new Vector3d();
      }
      Point3d p0loc = new Point3d(p0);
      p0loc.inverseTransform (getPose());
      nrm.set (p0loc);
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
         if (mag >= 0) {
            // dnrm = (I - nrm nrm^T)/mag
            dnrm.outerProduct (nrm, nrm);
            dnrm.m00 -= 1;
            dnrm.m11 -= 1;
            dnrm.m22 -= 1;
            dnrm.scale (-1.0/mag);
            dnrm.transform (getPose().R);
         }
         else {
            dnrm.setZero();
         }
      }
      nrm.transform (getPose());
      return mag - myRadius;
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
