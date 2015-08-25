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

   public RigidEllipsoid() {
      super (null);
      myAxisLengths = new Vector3d();
   }

   public void getAxisLengths (Vector3d lengths) {
      myAxisLengths.get (lengths);
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
      Point3d pr, Point3d p0, Point3d p1, double lam, Vector3d sideNrm) {

      double a = myAxisLengths.x;
      double b = myAxisLengths.y;
      double c = myAxisLengths.z;

      Point3d loc0 = new Point3d(p0);
      Point3d loc1 = new Point3d(p1);
      loc0.inverseTransform (getPose());
      loc1.inverseTransform (getPose());
      QuadraticUtils.ellipsoidSurfaceTangent (
         pr, loc0, loc1, a, b, c);
      pr.transform (getPose());
      
   }

   public double penetrationDistance (Vector3d nrm, Point3d p0) {
      double a = myAxisLengths.x;
      double b = myAxisLengths.y;
      double c = myAxisLengths.z;

      Point3d loc0 = new Point3d(p0);
      loc0.inverseTransform (getPose());
      double d = QuadraticUtils.ellipsoidPenetrationDistance (
         nrm, loc0, a, b, c);
      nrm.transform (getPose());
      return d;
   }

}
