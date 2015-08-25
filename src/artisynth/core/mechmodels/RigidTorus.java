package artisynth.core.mechmodels;

import java.io.*;
import java.util.*;

import maspack.geometry.*;
import maspack.util.*;
import maspack.matrix.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

public class RigidTorus extends RigidBody {
   
   double myOuterRadius = 1.0;
   double myInnerRadius = 0.25;

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
      this (name, router, rinner, density, 20, 10);
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

   public void surfaceTangent (
      Point3d pr, Point3d p0, Point3d p1, double lam, Vector3d sideNrm) {

      pr.setZero();
   }

   public double penetrationDistance (Vector3d nrm, Point3d p0) {
      Point3d p0loc = new Point3d(p0);
      p0loc.inverseTransform (getPose());

      double r = Math.sqrt (p0loc.x*p0loc.x + p0loc.y*p0loc.y);
      if (r == 0) {
         return 0;
      }
      Vector3d pmid = new Vector3d (p0loc.x, p0loc.y, 0);
      double mag = pmid.norm();
      if (mag == 0) {
         // leave normal unchanged
         return Math.hypot (myOuterRadius, p0loc.z) - myInnerRadius;
      }
      else {
         pmid.scale (myOuterRadius/mag);
      }
      nrm.sub (p0loc, pmid);
      mag = nrm.norm();
      if (mag == 0) {
         // leave normal unchanged
         return myInnerRadius;
      }
      else {
         nrm.scale (1/mag);
         return mag-myInnerRadius;
      }
   }

}
