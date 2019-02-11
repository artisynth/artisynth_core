package artisynth.core.mechmodels;

import java.io.*;
import java.util.*;

import maspack.geometry.*;
import maspack.util.*;
import maspack.matrix.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

public class RigidTorus extends RigidBody implements Wrappable {
   
   double myOuterRadius = 1.0;
   double myInnerRadius = 0.25;
   private static double MACH_PREC = 1.0e-16;
   private static double DTOR = Math.PI/180;

   public RigidTorus() {
      super (null);
   }

   public double getOuterRadius() {
      return myOuterRadius;
   }

   public void setOuterRadius (double r) {
      if (r != myOuterRadius) {
         myOuterRadius = r;
      }
   }

   public double getInnerRadius() {
      return myInnerRadius;
   }

   public void setInnerRadius (double r) {
      if (r != myInnerRadius) {
         myInnerRadius = r;
      }
   }      

   public RigidTorus (String name, double router, double rinner, double density) {
      this (name, router, rinner, density, 64, 32);
   }

   public RigidTorus (
      String name, double router, double rinner, double density,
      int nouter, int ninner) {

      super (name);
      PolygonalMesh mesh = MeshFactory.createTorus (
         router, rinner, nouter, ninner);
      setInertiaFromDensity (density);
      setMesh (mesh, null);
      myOuterRadius = router;
      myInnerRadius = rinner;
      myTransformConstrainer = 
         new GeometryTransformer.UniformScalingConstrainer();
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      pw.println ("outerRadius=" + fmt.format(getOuterRadius()));
      pw.println ("innerRadius=" + fmt.format(getInnerRadius()));
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "outerRadius")) {
         double r = rtok.scanNumber();
         setOuterRadius (r);
         return true;
      }
      else if (scanAttributeName (rtok, "innerRadius")) {
         double r = rtok.scanNumber();
         setInnerRadius (r);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   private int signCross2d (Point3d p0, Point3d p1) {
      double xprod = p0.x*p1.y - p0.y*p1.x;
      return xprod >= 0 ? 1 : -1;
   }

   /**
    * Computes <code>f(lam) = (ps-pa)^T n<code> for a given value of
    * <code>lam</code>. All quantities are assumed to be in local coordinates.
    * The parameter <code>lam</code> determines a point <code>p</code>
    * along the line segment according to
    * <pre>
    * p = pa + lam del
    * </pre>
    *
    * @param n returns the surface normal at the nearest surface point
    * @param ps if non-<code>null</code>, returns the nearest surface point
    * @param pa beginning point for the line segment
    * @param del displacement vector for the line segment
    * @param lam parameter determining the point along the line segment
    * @return f value corresponding to <code>lam</code>
    */
   private double computeF (
      Vector3d n, Point3d ps, Point3d pa, Vector3d del, double lam) {

      Point3d p = new Point3d();
      p.scaledAdd (lam, del, pa);
      double d = penetrationDistanceLoc (n, null, p, 1000);
      p.scaledAdd (-d, n);
      if (ps != null) {
         ps.set (p);
      }
      p.sub (pa);
      return p.dot (n);
   }

   /**
    * Computes the derivative <code>df(lam)</code> for <code>f(lam) = (ps-pa)^T
    * n<code> for a given value of <code>lam</code>.. All quantities are
    * assumed to be in local coordinates. The parameter <code>lam</code>
    * determines a point <code>p</code> along the line segment according to
    * <pre>
    * p = pa + lam del
    * </pre>
    * 
    * @param n surface normal at the nearest surface point
    * @param ps nearest surface point
    * @param pa beginning point for the line segment
    * @param del displacement vector for the line segment
    * @param lam parameter determining the point along the line segment
    * @return derivative df corresponding to <code>lam</code>
    */
   private double computeDf (
      Vector3d n, Point3d ps, Point3d pa, Vector3d del, double lam) {

      // 
      // df(lam) = dps^T n + (ps-pa)^T dn
      //
      // where dps and dn are the derivatives of ps and n with respect to lam.
      //
      // In turn, ps = pc + r n, where pc is the point on the torus circle
      // corresponding to p, and r is the inner radius, and so
      //
      // dps = dpc + r dn
      //
      // To determine dpc and dn, we note that
      //
      // ps = [ R p_x / c   R p_y / c    0 ]
      // 
      // n = q / ||q||
      //
      // where R is the outer radius, c = sqrt(p_x^2 + p_y^2), and q = p - pc,
      // and then apply the general relationship
      //
      // d (q/||q||) = 
      //
      //             [ (q_y^2 + q_z^2) dq_x - q_x q_y dq_y - q_x q_z dq_z ]
      //        1    [                                                    ]
      //     ------- [ (q_x^2 + q_z^2) dq_y - q_y q_x dq_x - q_y q_z dq_z ]
      //     ||q||^3 [                                                    ]
      //             [ (q_x^2 + q_y^2) dq_z - q_z q_x dq_x - q_z q_y dq_y ]
      //
      // The above gives a formula for dn directly, while for dpc it yields
      //
      //               [ p_y^2 dp_x - p_x p_y dp_y ]
      //          R    [                           ]
      // dpx = ------- [ p_x^2 dp_y - p_y p_x dp_x ]
      //         c^3   [                           ]
      //               [            0              ]
      //
      // where, by definition, dp = del.
      //
      double R = myOuterRadius;
      double r = myInnerRadius;
      Point3d p = new Point3d();
      p.scaledAdd (lam, del, pa);
      double c2 = p.x*p.x + p.y*p.y;
      double c = Math.sqrt (c2);
      double c3 = c*c2;
      Vector3d dpc = new Vector3d();
      dpc.x = R/c3*(p.y*p.y*del.x - p.x*p.y*del.y);
      dpc.y = R/c3*(p.x*p.x*del.y - p.x*p.y*del.x);
      Vector3d dq = new Vector3d(del);
      dq.sub (dpc);
      Vector3d q = new Vector3d(p);
      q.x -= myOuterRadius*p.x/c;
      q.y -= myOuterRadius*p.y/c;
      Vector3d dn = new Vector3d();
      double qmag2 = q.x*q.x + q.y*q.y + q.z*q.z;
      double qmag3 = qmag2*Math.sqrt (qmag2);
      dn.x = ((q.y*q.y+q.z*q.z)*dq.x - q.x*q.y*dq.y - q.x*q.z*dq.z)/qmag3;
      dn.y = ((q.x*q.x+q.z*q.z)*dq.y - q.y*q.x*dq.x - q.y*q.z*dq.z)/qmag3;
      dn.z = ((q.x*q.x+q.y*q.y)*dq.z - q.z*q.x*dq.x - q.z*q.y*dq.y)/qmag3;
      p.sub (ps, pa);
      return dpc.dot(n) + r*dn.dot(n) + dn.dot(p);
   }

   public void surfaceTangent (
      Point3d pr, Point3d pa, Point3d p1, double lam0, Vector3d sideNrm) {

      // Surface tangent calculation works as follows. For a given point p on
      // the line defined by (pa,p1), let
      //
      // pc = corresponding point on the circle running through the torus
      // q = p - pc = vector from pc to p
      // n = q/||q|| = normal vector from p to the surface
      // ps = nearest point on torus surface
      //
      // The tangent point will be p such that (ps-pa)^T n = 0
      //
      // We can then set this up as a root finding problem in lam, such that we
      // find lam that gives us a p for which
      //
      // f(lam) = (ps-pa)^T n = 0
      //
      // We use Newton's method combined with bisection for safety, starting
      // the search in the interval [lam0, 1], and expanding this interval if
      // necessary to bracket the root.
      Vector3d nrm = new Vector3d();

      // start by testing pa to see if it is inside the torus. If it is, just
      // return the projection to the surface.
      double d = penetrationDistance (nrm, null, pa);
      if (d < 0) {
         pr.scaledAdd (-d, nrm, pa);
         return;
      }
      // convert pa and p1 to local coordinates
      Point3d paLoc = new Point3d(pa);
      Point3d p1Loc = new Point3d(p1);
      paLoc.inverseTransform (getPose());
      p1Loc.inverseTransform (getPose());
      Vector3d del = new Vector3d();
      del.sub (p1Loc, paLoc);

      Point3d p = new Point3d();

      // compute dlam to be the approximate variation in lam corresponding to
      // moving 30 degrees with respect to the inner radius
      double dlam = Math.tan(DTOR*30)*myInnerRadius/del.norm();
      // now try to find an initial root bracket starting at lam=1.0
      double f1 = computeF (nrm, null, paLoc, del, 1.0);      
      double hi = 1.0+dlam;
      double lo = 1.0-dlam;
      double fhi = computeF (nrm, null, paLoc, del, hi);
      double flo = computeF (nrm, null, paLoc, del, lo);

      int iter = 0;
      int maxIter = 3;
      if (f1*fhi <= 0) {
         flo = f1;
         lo = 1.0;
      }
      else if (f1*flo <= 0) {
         fhi = f1;
         hi = 1.0;
      }
      else {
         while (fhi*flo > 0 && iter < maxIter) {
            //System.out.println (" fhi=" + fhi + " flo=" + flo);
            if (Math.abs(fhi) > Math.abs(flo)) {
               lo -= dlam;
               flo = computeF (nrm, null, paLoc, del, lo);
            }
            else {
               hi += dlam;
               fhi = computeF (nrm, null, paLoc, del, hi);
            }
            iter++;
         }
      }
      double lam;
      // compute tolerance for finding the root
      double ftol = 100*MACH_PREC*del.norm();
      Point3d psLoc = new Point3d();
      if (fhi*flo < -ftol) {
         // find root within interval. Start looking with lam at the center
         lam = (hi+lo)/2;

         maxIter = 50;
         double f = 0;
         for (iter=0; iter<maxIter; iter++) {
            f = computeF (nrm, psLoc, paLoc, del, lam);
            if (Math.abs(f) < ftol) {
               // f is close enough to zero, we are done
               break;
            }
            if((flo<0 && f>0) || (flo>0 && f<0)){
               // if sign change between lo and mid, move hi
               hi=lam;
               fhi=f;
            }
            else {
               // otherwise sign change between hi and mid, move lo
               lo=lam;
               flo=f;
            }
            if (lo > hi) {
               throw new InternalErrorException (
                  "interval exchanged, lo=" + lo + " hi=" + hi + " iter=" + iter);
            }  
            // compute df to apply Newton step
            double df = computeDf (nrm, psLoc, paLoc, del, lam);
            if (df != 0) {
               lam = lam - f/df; // Newton step
            }
            if (df == 0 || lam <= lo || lam >= hi) {
               // if df == 0 or Newton step takes us outside interval,
               // revert to bisection
               lam = (hi+lo)/2;
            }
            if (hi-lo < 1e-10) {
               // if interval is tight enough, done
               break;
            }
         }
      }
      else if (Math.abs(flo) < ftol) {
         lam = lo;
      }
      else if (Math.abs(fhi) < ftol) {
         lam = hi;
      }
      else {
         // Shouldn't happen
         System.out.println ("NOT bracketed " + flo + " " + fhi);
         pr.set (p1);
         return;
      }
      pr.scaledAdd (lam, del, paLoc);
      d = penetrationDistanceLoc (nrm, null, pr, 1000);
      pr.scaledAdd (-d, nrm);
      pr.transform (getPose());
   }

   private double penetrationDistanceLoc (
      Vector3d nrm, Matrix3d dnrm, Point3d p0loc, double outsideLimit) {

      if (nrm == null) {
         nrm = new Vector3d();
      }
      double r = Math.sqrt (p0loc.x*p0loc.x + p0loc.y*p0loc.y);
      if (r == 0) {
         return Wrappable.OUTSIDE;
      }
      Vector3d pc = new Vector3d (p0loc.x, p0loc.y, 0);
      double mag = pc.norm();
      if (mag == 0) {
         // leave normal unchanged
         return Wrappable.OUTSIDE;
      }
      else {
         pc.scale (myOuterRadius/mag);
      }
      nrm.sub (p0loc, pc);
      mag = nrm.norm();
      if (mag == 0) {
         // leave normal unchanged
         if (dnrm != null) {
            dnrm.setZero();
         }
         return -myInnerRadius;
      }
      else if (mag >= outsideLimit*myInnerRadius) {
         return Wrappable.OUTSIDE;
      }
      else {
         double magi = 1.0/mag;
         nrm.scale (magi);
         if (dnrm != null) {
            // first form dnrm in a coordinate frame rotated about z so that
            // the x axis coincides with (p0loc.x, p0loc.y).

            double s = p0loc.y/r; // sin
            double c = p0loc.x/r; // cos

            // find normal coords in rotated frame. ny = 0.
            double nx = c*nrm.x + s*nrm.y;
            double nz = nrm.z;

            double m00 = nz*nz*magi;
            double m02 = -nx*nz*magi;

            double m11 = nx/r;

            double m20 = -nx*nz*magi;
            double m22 = nx*nx*magi;

            // now rotate into torus coordinates:
            // dnrm = R * dnrm * inv(R)

            dnrm.m00 = c*c*m00 + s*s*m11;
            dnrm.m01 = c*s*(m00-m11);
            dnrm.m02 = c*m20;

            dnrm.m10 = dnrm.m01;
            dnrm.m11 = s*s*m00 + c*c*m11;
            dnrm.m12 = s*m20;

            dnrm.m20 = dnrm.m02;
            dnrm.m21 = dnrm.m12;
            dnrm.m22 = m22;
         }
         return mag-myInnerRadius;
      }
   }

   public double penetrationDistance (Vector3d nrm, Matrix3d dnrm, Point3d p0) {
      Point3d p0loc = new Point3d(p0);
      p0loc.inverseTransform (getPose());

      double d = penetrationDistanceLoc (nrm, dnrm, p0loc, 1.5);
      if (dnrm != null) {
         dnrm.transform (getPose().R);
      }
      if (nrm != null && d != Wrappable.OUTSIDE && d != -myInnerRadius) {
         nrm.transform (getPose());
      }
      return d;
   }

   /**
    * {@inheritDoc}
    */
   public double getCharacteristicRadius() {
      return myInnerRadius;
   }
   
   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // Update the inner and outer radii. The appropriate scaling is 
      // determined by applying the transform constrainer to the local affine 
      // transform induced by the transformation. 
      if (gtr.isRestoring()) {
         myInnerRadius = gtr.restoreObject (myInnerRadius);
         myOuterRadius = gtr.restoreObject (myOuterRadius);
      }
      else {
         if (gtr.isSaving()) {
            gtr.saveObject (myInnerRadius);
            gtr.saveObject (myOuterRadius);
         }
         AffineTransform3d XL = gtr.computeLocalAffineTransform (
            getPose(), myTransformConstrainer); 
         // need to take abs() since diagonal entries could be negative
         // if XL is a reflection
         double scale = Math.abs(XL.A.m00);
         myInnerRadius *= scale;
         myOuterRadius *= scale;
      }     
 
      super.transformGeometry (gtr, context, flags);
   }


}
