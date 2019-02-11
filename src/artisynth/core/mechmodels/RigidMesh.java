package artisynth.core.mechmodels;

import java.io.*;
import java.util.*;

import maspack.geometry.*;
import maspack.geometry.DistanceGrid.TetDesc;
import maspack.util.*;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.spatialmotion.*;
import maspack.render.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

public class RigidMesh extends RigidBody implements Wrappable {

   BVFeatureQuery myQuery = new BVFeatureQuery();
   NagataInterpolator myNagata = new NagataInterpolator();

   boolean myUseQuadraticTangents = true;
   boolean mySmooth = false;
   boolean myRayCastTangent = false;
   boolean myFindNearestHorizonPoint = true;
   // int myMaxGridDivisions = 0;
   // SignedDistanceGrid mySDGrid = null;
   String myTangentProblemFile = "tanProb.txt";

   public void setSmoothInterpolation (boolean smooth) {
      mySmooth = smooth;
   }

   public boolean getSmoothInterpolation () {
      return mySmooth;
   }

   public void setRayCastTangent (boolean enable) {
      myRayCastTangent = enable;
   }

   public boolean getRayCastTangent() {
      return myRayCastTangent;
   }

   public static PropertyList myProps =
      new PropertyList (RigidMesh.class, RigidBody.class);

   static {
      myProps.add (
         "smoothInterpolation", 
         "use quadratic Nagata interpolation", false);

      myProps.add (
         "rayCastTangent", 
         "use ray cast to determine surface tangent", false);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   

   public RigidMesh (RigidTransform3d XBodyToWorld, SpatialInertia M,
                     PolygonalMesh mesh, String meshFileName) {
      super (XBodyToWorld, M, mesh, meshFileName);
   }

   public RigidMesh (
      String name, PolygonalMesh mesh, String meshFilePath, 
      double density, double scale) {
      super (name);
      if (!mesh.isTriangular()) {
         throw new IllegalArgumentException("Mesh is not triangular");
      }
      mesh.scale (scale);
      AffineTransform3d X = null;
      if (scale != 1) {
         X = AffineTransform3d.createScaling (scale);
      }    
      setDensity (density);
      setMesh (mesh, meshFilePath, X);
   }

   public RigidMesh() {
      super (null);
   }

   /**
    * Returns the volume of the tet formed by px and the face vertices.  If
    * positive, then px is "outside" the face in the counter-clockwise sense.
    */
   double orient3d (Point3d px, Face face) {
      HalfEdge he = face.firstHalfEdge();
      Point3d p2 = he.getTail().pnt;
      Point3d p0 = he.getHead().pnt;
      he = he.getNext();
      Point3d p1 = he.getHead().pnt;

      Vector3d a = new Vector3d();
      Vector3d b = new Vector3d();
      Vector3d c = new Vector3d();

      a.sub (p1, p0);
      b.sub (p2, p0);
      c.sub (px, p0);

      double cybx = c.y*b.x;
      double cxby = c.x*b.y;
      double cxay = c.x*a.y;
      double cyax = c.y*a.x;
      double axby = a.x*b.y;
      double aybx = a.y*b.x;

      return a.z*(cybx-cxby) + b.z*(cxay-cyax) + c.z*(axby-aybx);
   }

   HalfEdge getEdge (Face face, int code) {
      switch (code) {
         case Face.EDGE_01: {
            return face.getEdge(1);
         }
         case Face.EDGE_12: {
            return face.getEdge(2);
         }
         case Face.EDGE_20: {
            return face.getEdge(0);
         }
      }
      return null;
   }            

   Vertex3d getVertex (Face face, int code) {
      switch (code) {
         case Face.VERTEX_0: {
            return face.firstHalfEdge().getHead();
         }
         case Face.VERTEX_1: {
            return face.getVertex(1);
         }
         case Face.VERTEX_2: {
            return face.getVertex(2);
         }
      }
      return null;
   }            


   /**
    * Class to holder the edge or vertex feature associated with a plane/mesh
    * intersection point.
    */
   private class FeatureHolder {
      HalfEdge myEdge;    // edge associated with this feature
      boolean myVertexP;  // true if feature is a vertex equal to edge.getHead()

      FeatureHolder() {
      }

      FeatureHolder (HalfEdge edge, boolean isVertex) {
         set (edge, isVertex);
      }

      Feature getFeature() {
         return myVertexP ? myEdge.getHead() : myEdge;
      }

      void set (FeatureHolder fhold) {
         myEdge = fhold.myEdge;
         myVertexP = fhold.myVertexP;
      }

      boolean isVertex() {
         return myVertexP;
      }

      HalfEdge getEdge() {
         return myEdge;
      }

      void set (HalfEdge edge, boolean isVertex) {
         myEdge = edge;
         myVertexP = isVertex;
      }

      void reverse() {
         myEdge = myEdge.opposite;
         if (myVertexP) {
            myEdge = myEdge.getNext().getNext();
         }
      }
   }

   /**
    * Given an intersection point feature fhold0, advance to the next feature
    * fhold1 along the contour.
    */
   boolean advanceFeature (
      FeatureHolder fhold1, FeatureHolder fhold0, Plane plane) {

      if (fhold0.isVertex()) {
         // current feature is a vertex

         HalfEdge he = fhold0.myEdge.getNext();
         int side = plane.side (he.getHead().pnt);
         HalfEdge he0 = he.opposite.getNext();
         he = he0;
         do {
            int lastSide = side;
            side = plane.side (he.getHead().pnt);
            if (side == 0) {
               fhold1.set (he, /*isVertex=*/true);
               return true;
            }
            else if (side*lastSide < 0) {
               fhold1.set (he.getNext(), lastSide==0);
               return true;
            }
            he = he.opposite.getNext();
         }
         while (he != he0);
         // shouldn't happen?
         return false;
      }
      else {
         // current feature is an edge

         HalfEdge fedge = fhold0.getEdge();
         HalfEdge heNext = fedge.opposite.getNext();
         int tside = plane.side (fedge.getTail().pnt);
         int nside = plane.side (heNext.getHead().pnt);

         if (nside*tside <= 0) {
            fhold1.set (heNext, nside == 0);
         }
         else {
            fhold1.set (heNext.getNext(), /*isVertex=*/false);
         }
         return true;
      }
   }

   private static double TOL = 1e-10;

   private class Plane {
      Vector3d myNrm;
      Point3d myPnt;

      Plane() {
         myNrm = new Vector3d();
         myPnt = new Point3d();
      }

      void initialize (Face face, Point3d ph, Point3d pa, Point3d p1) {
         
         // XXX check for and handle colinear case
         Vector3d pa1 = new Vector3d();
         Vector3d pah = new Vector3d();
         pa1.sub (p1, pa);
         pah.sub (ph, pa);
         myNrm.cross (pa1, pah);
         //System.out.println ("nrm=" + myNrm.norm());
         myNrm.normalize();
         myPnt.set (pa);
      }

      double distance (Point3d pnt) {
         Vector3d r = new Vector3d();
         r.sub (pnt, myPnt);
         return r.dot (myNrm);
      }      

      int side (Point3d pnt) {
         return side (distance (pnt));
      }

      int side (double dist) {
         if (dist > TOL) {
            return 1;
         }
         else if (dist < -TOL) {
            return -1;
         }
         else {
            return 0;
         }
      }

      boolean intersectEdge (Point3d pi, HalfEdge he) {
         double s = intersectEdge (he);
         if (s == -1) {
            return false;
         }
         else {
            pi.combine (1-s, he.getTail().pnt, s, he.getHead().pnt);
            return true;
         }
      }

      double intersectEdge (HalfEdge he) {
         Point3d head = he.getHead().pnt;
         Point3d tail = he.getTail().pnt;
         double hdist = distance (head);
         double tdist = distance (tail);
         
         if (side(hdist)*side (tdist) > 0) {
            return -1;
         }
         else {
            double s;
            if (side(hdist) == 0) {
               s = 1.0;
            }
            else if (side(tdist) == 0) {
               s = 0.0;
            }
            else {
               s = Math.abs(tdist/(tdist-hdist));
            }
            return s;
         }
      }

      boolean intersectFace (FeatureHolder fhold, Face face) {
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         do {
            double s = intersectEdge (he);
            if (s != -1) {
               if (s == 0) {
                  fhold.set (he.getNext().getNext(), /*isVertex=*/true);
               }
               else {
                  fhold.set (he, side (he.getHead().pnt) == 0);
               }
               return true;
            }
            he = he.getNext();
         }         
         while (he != he0);
         return false;
      }
   }      

   HalfEdge findHorizonEdge (
      FeatureHolder fhold, Point3d pa, boolean inside) {

      if (fhold.isVertex()) {
         HalfEdge he = fhold.myEdge;
         boolean inside0 = (orient3d (pa, he.getFace()) <= 0);
         he = he.getNext().opposite;
         while (he != fhold.myEdge) {
            if ((orient3d (pa, he.getFace()) <= 0) != inside0) {
               return he;
            }
            he = he.getNext().opposite;
         }
      }
      else {
         Face face = fhold.myEdge.getFace();
         Face faceOpp = fhold.myEdge.getOppositeFace();
         // just check the edge
         if ((orient3d (pa, face) <= 0) != (orient3d (pa, faceOpp) <= 0)) {
            return fhold.myEdge;
         }
      }
      return null;      
   }
      
   HalfEdge findFirstHorizonPoint (
      Point3d ph, Point3d pa, Point3d p1, Face face) {

      boolean inside = (orient3d (pa, face) <= 0);

      Vector3d nrm = new Vector3d();
      Plane plane = new Plane();
      plane.initialize (face, ph, pa, p1);

      //System.out.println ("plane nrm=" + plane.myNrm);
      //System.out.println ("plane pnt=" + plane.myPnt);

      FeatureHolder fhold = new FeatureHolder();

      //System.out.println (
      //   "nearest face=" + face.getIndex() + " p1=" + p1.toString("%8.3f"));
      if (!plane.intersectFace (fhold, face)) {
         System.out.println ("FACE NOT INTERSECTED");
         return null;
      }
      HalfEdge hh;
      if ((hh = findHorizonEdge (fhold, pa, inside)) != null) {
         plane.intersectEdge (ph, hh);
         return hh;
      }
      FeatureHolder fnext = new FeatureHolder();
      fnext.set (fhold);
      fnext.reverse();
      if (!advanceFeature (fnext, fnext, plane)) {
         System.out.println ("can't advance");
         return null;
      }

      if ((hh = findHorizonEdge (fnext, pa, inside)) != null) {
         plane.intersectEdge (ph, hh);
         return hh;
      }

      Point3d pi = new Point3d();
      plane.intersectEdge (pi, fhold.myEdge);
      double d0 = pi.distance (pa);
      //System.out.println ("fhold=" + fhold.myEdge.vertexStr() + " d0=" + d0);

      plane.intersectEdge (pi, fnext.myEdge);
      double d1 = pi.distance (pa);
      //System.out.println ("fnext=" + fnext.myEdge.vertexStr() + " d1=" + d1);

      //System.out.println ("inside=" + inside);

      // if we are inside, want to advance towards pa; otherwise,
      // we want to advance away
      if ((inside && d1 < d0) || (!inside && d1 > d0)) {
         // use fnext to advance
         fhold.set (fnext);
      }
      
      Feature feat0 = fhold.getFeature();
      do {
         // System.out.println (
         //    "   fedge=" + fhold.myEdge.vertexStr());
         if ((hh = findHorizonEdge (fhold, pa, inside)) != null) {
            plane.intersectEdge (ph, hh);
            return hh;
         }
         if (!advanceFeature (fhold, fhold, plane)) {
            System.out.println ("can't advance");
            return null;
         }
      }
      while (fhold.getFeature() != feat0);   
      return null;
   }

   private HalfEdge nextHorizonEdge (
      HalfEdge prev, Point3d pa) {

      boolean inside0 = (orient3d (pa, prev.getFace()) <= 0);
      HalfEdge he = prev;                                 
      boolean inside = inside0;
      do {
         he = he.getNext().opposite;
         inside = (orient3d (pa, he.getFace()) <= 0);
      }
      while (inside0 == inside);
      return he.opposite;
   }

   private double projectionParameter (HalfEdge he, Point3d pa, Point3d p1) {
      double[] params = new double[2];
      LineSegment.nearestPointParameters (
         params, he.getTail().pnt, he.getHead().pnt, pa, p1);
      return params[0];
   }

   boolean findNearestHorizonPoint (
      Point3d pr, HalfEdge he, Point3d pa, Point3d p1, boolean debug) {

      double s = projectionParameter (he, pa, p1);
      int cnt = 0;
      if (debug) System.out.println ("s=" + s);
      while (s < 0.0 || s > 1.0) {
         //System.out.println ("he=" + he.vertexStr() + " s=" + s);
         if (s < 0) {
            he = nextHorizonEdge (he.opposite, pa);
         }
         else {
            he = nextHorizonEdge (he, pa);
         }
         if (debug) System.out.println ("he=" + he.vertexStr());
         s = projectionParameter (he, pa, p1);
         if (s < 0.0) {
            // this means we're going backwards - stuck at the vertex
            s = 0;
         }
         if (debug) System.out.println ("s=" + s);
         if (cnt++ > 50) {
            System.out.println ("cnt=" + cnt);
            return false;
         }
      }
      pr.combine (1-s, he.getTail().pnt, s, he.getHead().pnt);
      // System.out.println (
      //    "cnt=" + cnt + " " + pr.toString ("%g")); //16.12f"));
      return true;
   }

   boolean writeTanProblem = false;

   public void surfaceTangent (
      Point3d pr, Point3d pa, Point3d p1, double lam0, Vector3d sideNrm) {

      PolygonalMesh mesh = getSurfaceMesh();
      if (hasDistanceGrid()) {

         if (myUseQuadraticTangents) {
            DistanceGrid grid = getDistanceGrid();
            boolean found = grid.findQuadSurfaceTangent (pr, p1, pa, sideNrm);
            if (!found || writeTanProblem) {
               if (!found) {
                  System.out.println ("couldn't find tangent");
               }
               System.out.println ("p1=" + p1.toString ("%10.5f"));
               System.out.println ("pa=" + pa.toString ("%10.5f"));
               System.out.println ("nm=" + sideNrm.toString ("%10.5f"));
               if (myTangentProblemFile != null) {
                  System.out.println ("Writing problem to "+myTangentProblemFile);
                  DistanceGridSurfCalc.TangentProblem tprob =
                     new DistanceGridSurfCalc.TangentProblem (
                        p1, pa, sideNrm, grid);
                  tprob.write (myTangentProblemFile, "%g");
               }
               grid.setDebug (1);
               grid.findQuadSurfaceTangent (pr, p1, pa, sideNrm);
               grid.setDebug (0);
               // pr will already be set to either p0 or the nearest
               // surface poin ps
            }
            return;
         }
         
         BVFeatureQuery bvq = new BVFeatureQuery();

         if (myRayCastTangent) {
            Vector3d dir = new Vector3d();
            dir.sub (p1, pa);
            dir.normalize();
            Point3d near = bvq.nearestPointAlongRay (mesh, pa, dir);
            pr.set (near);
            return;
         }

         Point3d p1Loc = new Point3d(p1);
         Point3d paLoc = new Point3d(pa);
         if (!mesh.meshToWorldIsIdentity()) {
            p1Loc.inverseTransform (mesh.getMeshToWorld());
            paLoc.inverseTransform (mesh.getMeshToWorld());
         }

         if (bvq.isInsideOrientedMesh (mesh, pa, -1)) {
            bvq.getFaceForInsideOrientedTest (pr, /*uv=*/null);
            if (!mesh.meshToWorldIsIdentity()) {
               pr.transform (mesh.getMeshToWorld());
            }
            return;
         }
         Face face = bvq.nearestFaceToPoint (pr, null, mesh, p1);
         if (!mesh.meshToWorldIsIdentity()) {
            pr.inverseTransform (mesh.getMeshToWorld());
         }

         // System.out.println ("surfaceTangent:");
         // System.out.println ("pa=" + pa);
         // System.out.println ("p1=" + p1);

         // System.out.println ("nearest face=" + face.getIndex());
         // System.out.println ("nearest pr=" + pr);

         HalfEdge he = findFirstHorizonPoint (pr, paLoc, p1Loc, face);
         //System.out.println ("he=" + he.vertexStr());
         //System.out.println ("hp=" + pr.toString("%8.3f"));
         boolean inside0 = (orient3d(paLoc, he.getFace()) >= 0);
         boolean inside1 = (orient3d(paLoc, he.getOppositeFace()) >= 0);
         if (inside0 == inside1) {
            throw new InternalErrorException (
               "Not a horizon edge "+he.vertexStr()+" inside=" + inside0);
         }
         
         // Once we have found the horizon point, find the nearest point
         // along the horizon

         // May 11: removed nearest point calculation so we can just try
         // tangent in the plane

         if (myFindNearestHorizonPoint &&
             !findNearestHorizonPoint (pr, he, paLoc, p1Loc, false)) {
            //System.out.println ("pa=" + pa);
            //System.out.println ("p1=" + p1);
            //System.out.println ("lam0=" + lam0);
            findNearestHorizonPoint (pr, he, paLoc, p1Loc, true);
            throw new InternalErrorException ("Inf loop");
         }

         //System.out.println ("np=" + pr.toString("%8.3f"));

         // 
         //face.nearestPoint (pr, p1Loc);

         //System.out.println (
         //   "face=" + face.getIndex() + " " + vxyz);
         if (!mesh.meshToWorldIsIdentity()) {
            pr.transform (mesh.getMeshToWorld());
         }
      }
      else {
         if (mesh != null) {
            Vector3d dir = new Vector3d();
            dir.sub (p1, pa);
            myQuery.nearestFaceAlongRay (
               pr, /*uv=*/null, mesh.getBVTree(), pa, dir);
         }
         else {
            pr.setZero();
         }
      }
   }

   public TetDesc getQuadTet (Point3d p0) {
      if (hasDistanceGrid()) {
         DistanceGrid grid = getDistanceGrid();
         Point3d p0loc = new Point3d();
         p0loc.inverseTransform (getPose(), p0);
         return grid.getQuadTet (p0loc);
      }
      else {
         return null;
      }
   }

   public double penetrationDistance (Vector3d nrm, Matrix3d Dnrm, Point3d p0) {

      PolygonalMesh mesh = getSurfaceMesh();
      if (mesh == null) {
         return Wrappable.OUTSIDE;
      }
      
      Point3d near = new Point3d();
      Vector3d diff = new Vector3d();

      if (hasDistanceGrid()) {
         DistanceGrid grid = getDistanceGrid();
         Point3d p0loc = new Point3d();
         p0loc.inverseTransform (getPose(), p0);
         //double d = grid.getLocalDistanceAndNormal (nrm, Dnrm, p0loc);
         double d = grid.getQuadDistanceAndGradient (nrm, Dnrm, p0loc);
         RotationMatrix3d R = getPose().R;
         if (Dnrm != null) {
            Dnrm.transform (R);
         }
         if (nrm != null) {
            nrm.transform (R);
         }
         if (d == DistanceGrid.OUTSIDE_GRID) {
            return Wrappable.OUTSIDE;
         }
         else {
            return d;
         }
      }
      else {
         if (Dnrm != null) {
            Dnrm.setZero();
         }
         if (mySmooth) {
            myNagata.nearestPointOnMesh (near, nrm, mesh, p0, 1e-8, myQuery);
         }
         else {
            Face face = myQuery.nearestFaceToPoint (
               near, /*uv=*/null, mesh.getBVTree(), p0);
            if(face == null)
               return Wrappable.OUTSIDE;
            if (nrm != null) {
               nrm.set (face.getWorldNormal());
            }
         }
         diff.sub (p0, near);
         double d = diff.dot(nrm);
         return d;
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public double getCharacteristicRadius() {
      if (hasDistanceGrid()) {
         DistanceGrid grid = getDistanceGrid();
         return grid.getWidths().minElement()/2;
      }
      else {
         return RenderableUtils.getRadius (this);         
      }
   }
   

}
