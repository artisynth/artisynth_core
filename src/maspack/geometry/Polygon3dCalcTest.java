package maspack.geometry;

import java.util.*;
import java.io.*;
import maspack.matrix.*;
import maspack.util.*;

public class Polygon3dCalcTest extends UnitTest {

   Polygon3dCalcTest() {
   }

   double[] square = new double[] {
      +1.0, 1.0, 
      -1.0, 1.0,
      -1.0, -1.0,
      +1.0, -1.0
   };

   double[] fiveSided = new double[] {
      0.0, 0.0,
      1.0, 0.0, 
      2.0, 1.0, 
      0.5, 2.0,
      0.0, 1.0,
   };

   double[] triangle = new double[] {
      0.0, 0.0,
      1.0, 0.0, 
      1.0, 1.0,
   };

   double[] angle = new double[] {
      0.0, 0.0,
      1.0, 0.0,
      2.0, 0.0,
      2.0, 1.0,
      1.0, 1.0,
      1.0, 2.0,
      0.0, 2.0,
      0.0, 1.0 };

   double[] man = new double[] {
      0.0, -1.0,
      0.5, -5.0,
      0.5, -9.0,
      1.5, -9.0,
      1.5, -5.0,
      1.5, -1.0,
      1.5,  0.0,
      1.0,  1.5,
      1.5,  3.5,
      3.5,  0.0,
      4.5,  0.5,
      2.0,  5.0,
      0.5,  5.5,
      0.5,  6.0,
      1.2,  7.0,
      1.0,  8.0,
      0.5,  9.0,

     -0.5,  9.0,
     -1.0,  8.0,
     -1.2,  7.0,
     -0.5,  6.0,
     -0.5,  5.5,
     -2.0,  5.0,
     -4.5,  0.5,
     -3.5,  0.0,
     -1.5,  3.5,
     -1.0,  1.5,
     -1.5,  0.0,
     -1.5, -1.0,
     -1.5, -5.0,
     -1.5, -9.0,
     -0.5, -9.0,
     -0.5, -5.0 };

   double[] connectedHole = new double[] {
      0.0, -2.0,
      2.0, -2.0,
      2.0,  0.0,
      1.0,  0.0,
      0.0, -1.0,
     -1.0, -1.0,
     -1.0,  1.0,
      0.0,  1.0,
      1.0,  0.0,
      2.0,  0.0,
      2.0,  2.0,
      0.0,  2.0,
     -2.0,  2.0,
     -2.0, -2.0
   };

   double[] innerSquare = new double[] {
      0.0,  1.0, 
      0.0,  3.0,
      2.0,  3.0,
      2.0,  1.0,
   };

   double[] innerTriangle = new double[] {
     -2.0,  2.0, 
     -2.0,  4.0,
      0.0,  4.0,
   };

   double[] outerPoly = new double[] {
     -4.0, 1.0,
      0.0, 0.0,
      3.0, 0.0,
      3.0, 4.0,
     -2.0, 5.0,
   };

   double[] outerPoly2 = new double[] {
      0.0, 0.0,
      1.0, 4.0,
     -2.0, 7.0,
     -3.0, 7.0,
     -4.0, 4.0,
     -3.0, 4.0,
     -2.0, 6.0,
     -3.0, 0.0,
   };

   void printTriangles (ArrayList<Vertex3d> triVtxs) {
      for (int k=0; k<triVtxs.size(); k+= 3) {
         Vertex3d v0 = triVtxs.get(k+0);
         Vertex3d v1 = triVtxs.get(k+1);
         Vertex3d v2 = triVtxs.get(k+2);

         System.out.println (
            "  "+v0.getIndex()+" "+v1.getIndex()+" "+v2.getIndex());

         // Point3d p0 = v0.pnt;
         // Point3d p1 = v1.pnt;
         // Point3d p2 = v2.pnt;

         // System.out.printf ("%8.3f %8.3f %8.3f\n", p0.x, p0.y, p0.z);
         // System.out.printf ("%8.3f %8.3f %8.3f\n", p1.x, p1.y, p1.z);
         // System.out.printf ("%8.3f %8.3f %8.3f\n", p2.x, p2.y, p2.z);
         // System.out.println ("");
      }
   }

   private void testTri (double[]... pvals) {
      System.out.println ("testing");
      Polygon3dCalc calc = new Polygon3dCalc (Vector3d.Z_UNIT, 0);
      ArrayList<Vertex3d> triVtxs = new ArrayList<Vertex3d>();
      ArrayList<Vertex3d> polyVtxs = createVertices (pvals[0], 0);      
      ArrayList<ArrayList<Vertex3d>> holes =
         new ArrayList<ArrayList<Vertex3d>>();
      int idx = polyVtxs.size();
      for (int i=1; i<pvals.length; i++) {
         ArrayList<Vertex3d> vtxs = createVertices(pvals[i], idx);
         idx += vtxs.size();
         holes.add (vtxs);
      }
      calc.triangulate (triVtxs, polyVtxs, holes);
      printTriangles (triVtxs);
   }

   public void testTriangulate() {
      // testTri (triangle);
      // testTri (square);
      // testTri (fiveSided);
      // testTri (angle);
      // testTri (man);
      // testTri (connectedHole);
      //testTri (outerPoly, innerSquare, innerTriangle);
      testTri (outerPoly2, innerTriangle);
   }

   private static final double EPS = 1e-10;

   private static final double END = 1e8;
   private static final double CLOSE = 1e9;

   ArrayList<ArrayList<Point2d>> createPointSets (double[] pvals) {
      ArrayList<ArrayList<Point2d>> pntSets =
         new ArrayList<ArrayList<Point2d>>();

      ArrayList<Point2d> pntSet = new ArrayList<Point2d>();
      pntSets.add (pntSet);
      int k = 0;
      while (k < pvals.length) {
         if (pvals[k] == CLOSE || pvals[k] == END) {
            if (pvals[k] == CLOSE) {
               pntSet.add (null);
            }
            pntSet = null;
            k++;
         }
         else if (k < pvals.length-1) {
            if (pntSet == null) {
               pntSet = new ArrayList<Point2d>();
               pntSets.add (pntSet);
            }
            pntSet.add (new Point2d(pvals[k], pvals[k+1]));
            k += 2;
         }
         else {
            break;
         }
      }
      return pntSets;
   }

   public void testNF (
      double[] pvals, double px, double py, int outsideCheck, int... vtxCheck) {

      //NearestPolygon3dFeature nfeat = new NearestPolygon3dFeature();
      //Polygon3dCalc calc = new Polygon3dCalc(Vector3d.Z_UNIT, EPS);
      //Point3d pnt = new Point3d (px, py, 0);
      //nfeat.init (new Point3d (px, py, 0), Vector3d.Z_UNIT, EPS);

      ArrayList<ArrayList<Point2d>> pntSets = createPointSets (pvals);
      testNF (pntSets, new Point3d(px, py, 0), outsideCheck, vtxCheck);
   }

   private ArrayList<ArrayList<Point3d>> create3dPointSets (
      ArrayList<ArrayList<Point2d>> pntSets,
      RigidTransform3d T, boolean reverse) {

      ArrayList<ArrayList<Point3d>> newSets =
         new ArrayList<ArrayList<Point3d>>(pntSets.size());
      

      for (ArrayList<Point2d> pntSet : pntSets) {
         ArrayList<Point3d> newSet = new ArrayList<Point3d>();

         for (int k=0; k<pntSet.size(); k++) {
            newSet.add (null);
         }
         int nump = pntSet.size();
         if (pntSet.get(pntSet.size()-1) == null) {
            nump--;
         }
         for (int k=0; k<nump; k++) {
            Point2d p2d = pntSet.get(k);
            Point3d p3d = new Point3d(p2d.x, p2d.y, 0);
            if (T != null) {
               p3d.transform (T);
            }
            if (reverse) {
               newSet.set (nump-1-k, p3d);
            }
            else {
               newSet.set (k, p3d);
            }
         }
         newSets.add (newSet);
      }
      return newSets;
   }

   public void testNF (
      ArrayList<ArrayList<Point2d>> pntSets, Point3d px,
      int outsideCheck, int[] vtxCheck) {
      
      ArrayList<ArrayList<Point3d>> pntSets3d =
         create3dPointSets (pntSets, null, false);
      doTest (pntSets3d, Vector3d.Z_UNIT, px, outsideCheck, vtxCheck);
      int[] reverseVtxCheck = getReverseVertices (pntSets3d, vtxCheck);
      int reverseOutsideCheck;
      if (outsideCheck == -1) {
         reverseOutsideCheck = -1;
      }
      else {
         reverseOutsideCheck = (outsideCheck == 1 ? 0 : 1);
      }

      RigidTransform3d T = new RigidTransform3d();
      Point3d pxx = new Point3d();
      Vector3d nrm = new Vector3d();
      for (int i=0; i<5; i++) {
         T.setRandom();
         pxx.transform (T, px);
         T.R.getColumn (2, nrm);
         doTest (create3dPointSets (pntSets, T, false), nrm, pxx,
              outsideCheck, vtxCheck);
         doTest (create3dPointSets (pntSets, T, true), nrm, pxx,
              reverseOutsideCheck, reverseVtxCheck);
      }
   }

   private int getNumPoints (ArrayList<Point3d> pntSet) {
      int nump = pntSet.size();
      if (pntSet.get(pntSet.size()-1) == null) {
         nump--;
      }
      return nump;
   }

   private ArrayList<Point3d> getPointSetForIndex (
      ArrayList<ArrayList<Point3d>> pntSets, int idx) {
      int base = 0;
      for (ArrayList<Point3d> pntSet : pntSets) {
         int nump = getNumPoints (pntSet);
         if (base <= idx && idx < base+nump) {
            return pntSet;
         }
         base += nump;
      }
      return null;
   }

   int[] getReverseVertices (
      ArrayList<ArrayList<Point3d>> pntSets, int[] vtxCheck) {

      int idx = vtxCheck[0];
      int base = 0;
      for (ArrayList<Point3d> pntSet : pntSets) {
         int nump = getNumPoints (pntSet);
         if (base <= idx && idx < base+nump) {
            int numv = vtxCheck.length;
            int[] vtxReverse = new int[numv];
            for (int i=0; i<numv; i++) {
               int vi = vtxCheck[i]-base;
               while (vi+1 < nump &&
                      pntSet.get(vi).distance(pntSet.get(vi+1)) <= EPS) {
                  vi++;
               }
               vi += base;
               vtxReverse[numv-1-i] = (base+nump-1)-vi+base;
            }
            return vtxReverse;
         }
         base += nump;
      }
      return null;

   }

   /**
    * Return true if idx belongs to a two-point closed polygon in
    * pntSets.
    */
   private boolean isTwoPointClosed (
      ArrayList<ArrayList<Point3d>> pntSets, int idx) {
      ArrayList<Point3d> pntSet = getPointSetForIndex (pntSets, idx);
      if (pntSet != null) {
         int nump = getNumPoints (pntSet);
         return nump == 2 && pntSet.size() == 3;
      }
      return false;
   }
           
   public void doTest (
      ArrayList<ArrayList<Point3d>> pntSets,
      Vector3d nrm, Point3d px, int outsideCheck, int[] vtxCheck) {

      //NearestPolygon3dFeature nfeat = new NearestPolygon3dFeature();
      //nfeat.init (px, nrm, EPS);
      Polygon3dCalc calc = new Polygon3dCalc(nrm, EPS);

      ArrayList<Point3d> allPnts = new ArrayList<Point3d>();

      int k = 0;
      Polygon3dFeature nfeat = new Polygon3dFeature();
      for (ArrayList<Point3d> pntSet : pntSets) {
         Vertex3dList poly = new Vertex3dList(/*closed=*/false);
         for (int j=0; j<pntSet.size(); j++) {
            Point3d p = pntSet.get(j);
            if (p != null) {
               poly.addNonCoincident (new Vertex3d (p), EPS);
            }
            else {
               poly.setClosed (true);
            }
            allPnts.add (p);
         }
         calc.nearestFeature (nfeat, px, poly, 0);
         k++;
      }
      if (vtxCheck.length != nfeat.numVertices()) {
         throw new TestException (
            "Nearest feature has "+nfeat.numVertices()+
            ", expected "+vtxCheck.length);
      }
      int[] vidxs = new int[vtxCheck.length];
      for (int i=0; i<vtxCheck.length; i++) {
         vidxs[i] = allPnts.indexOf(nfeat.getVertex(i));
      }
      if (vtxCheck.length == 2 && isTwoPointClosed(pntSets, vtxCheck[0])) {
         // closest segment belongs to a two point closed polygon,
         // and so could be ambiguous
         Point3d chk0 = allPnts.get(vtxCheck[0]);
         Point3d chk1 = allPnts.get(vtxCheck[1]);
         Point3d pnt0 = nfeat.getVertex(0);
         Point3d pnt1 = nfeat.getVertex(1);
         if ((chk0!=pnt0 || chk1!=pnt1) && (chk0!=pnt1 || chk1!=pnt0)) {
            throw new TestException (
               "Expected vertices [ 0 1 ] or [ 1 0 ], got ["+
               ArraySupport.toString(vidxs)+"]");
         }
      }
      else {
         for (int i=0; i<vtxCheck.length; i++) {
            Point3d p = allPnts.get(vtxCheck[i]);
            if (p != nfeat.getVertex (i)) {
               throw new TestException (
                  "Expected vertices ["+ArraySupport.toString(vtxCheck)+
                  "], got ["+ ArraySupport.toString(vidxs)+"]");
            }
         }
      }
      int outside = nfeat.isOutside (/*clockwise=*/false);
      //System.out.println ("outside(false) = " + outside);
      if (outsideCheck != outside) {
         throw new TestException (
            "Expected outside(/*clockwise=*/false)=" +
            outsideCheck + ", got "+outside);
      }
      if (outsideCheck != -1) {
         outsideCheck = outsideCheck == 0 ? 1 : 0;
         outside = nfeat.isOutside (/*clockwise=*/true);
         //System.out.println ("outside(true) = " + outside);
         if (outsideCheck != outside) {
            throw new TestException (
               "Expected outside(/*clockwise=*/true)=" +
               outsideCheck + ", got "+outside);
         }
      }
   }

   private double rightTurn (
      Vector2d pa, Vector2d pb, Vector2d pc) {

      double dabx = pb.x-pa.x;
      double daby = pb.y-pa.y;
      double dbcx = pc.x-pb.x;
      double dbcy = pc.y-pb.y;
      return dbcx*daby - dbcy*dabx;
   }

   private boolean isInsideTriangle (Vector2d p, Vector2d[] tri) {
      return (rightTurn (tri[0], tri[1], p) < 0 &&
              rightTurn (tri[1], tri[2], p) < 0 &&
              rightTurn (tri[2], tri[0], p) < 0);
   }

   private void intersect (double[] clip, double smin, double smax) {
      if (clip[0] != -1) {
         // check for overlap
         if (smin >= clip[1] || smax <= clip[0]) {
            clip[0] = -1;
            clip[1] = -1;
         }
         else {
            clip[0] = Math.max (clip[0], smin);
            clip[1] = Math.min (clip[1], smax);
         }
      }
   }

   private void clipLineSegment (
      double[] clip, Vector2d pa, Vector2d pb, Vector2d p0, Vector2d p1) {

      Vector2d dir01 = new Vector2d();
      Vector2d dir0a = new Vector2d();
      Vector2d dir0b = new Vector2d();

      dir01.sub (p1, p0);
      dir0a.sub (pa, p0);
      dir0b.sub (pb, p0);

      double crossa = dir0a.cross (dir01);
      double crossb = dir0b.cross (dir01);
      if (crossb > 0) {
         if (crossa > 0) {
            clip[0] = -1;
            clip[1] = -1;
         }
         else {
            double smax = Math.min(Math.max(0,crossa/(crossa-crossb)),1);
            intersect (clip, 0, smax);
         }
      }
      else {
         if (crossa > 0) {
            double smin = Math.min(Math.max(0,crossa/(crossa-crossb)),1);
            intersect (clip, smin, 1);
         }
      }
   }

   private double[] intersectSegmentTriangle (
      Vector2d pa, Vector2d pb, Vector2d[] tri) {

      double[] clip = new double[] { 0.0, 1.0 };

      clipLineSegment (clip, pa, pb, tri[0], tri[1]);
      clipLineSegment (clip, pa, pb, tri[1], tri[2]);
      clipLineSegment (clip, pa, pb, tri[2], tri[0]);
      return clip;
   }

   private int numInsideVertices (ArrayList<Vector2d> polygon, Vector2d[] tri) {
      int num = 0;
      for (Vector2d p : polygon) {
         if (isInsideTriangle (p, tri)) {
            num++;
         }
      }
      return num;
   }

   private Point3d interp (double s, Point3d pa, Point3d pb) {
      Point3d p = new Point3d();
      p.combine (1-s, pa, s, pb);
      return p;
   }

   private void insideTest (
      double[] pvals, double dx, double[] tvals) {
       
      int numPoses = 20;
      int numPoints = 1000;

      ArrayList<ArrayList<Point2d>> polygons = createPointSets(pvals);
      for (int i=0; i<numPoses; i++) {
         RigidTransform2d T2 = 
            new RigidTransform2d(
               RandomGenerator.nextDouble(-dx,dx),
               RandomGenerator.nextDouble(-dx,dx),
               RandomGenerator.nextDouble(-Math.PI,Math.PI));
         T2.setIdentity();
         Point2d[] tri = new Point2d[3];
         for (int j=0; j<3; j++) {
            tri[j] = new Point2d(tvals[2*j+0], tvals[2*j+1]);
            tri[j].transform (T2);
         }
         for (int k=0; k<numPoints; k++) {
            double s1 = RandomGenerator.nextDouble(0,1);
            double s2 = RandomGenerator.nextDouble(0,1);
            if (s1+s2 > 1) {
               s1 = 1-s1;
               s2 = 1-s2;
            }
            Point2d p = new Point2d();
            p.scaledAdd (1-s1-s2, tri[0]);
            p.scaledAdd (s1, tri[1]);
            p.scaledAdd (s2, tri[2]);
            RigidTransform3d T3 = new RigidTransform3d();
            insideTest (p, polygons, tri, T3, /*clockwise=*/false);
            T3.setRandom();
            //insideTest (p, polygons, tri, T3, /*clockwise=*/false);
         }
      }
   }
   
   
   private void insideTest (
      Vector2d p, ArrayList<ArrayList<Point2d>> polygons,
      Point2d[] tri, RigidTransform3d T, boolean clockwise) {

      // see if the point is inside or outside the polygons

      ArrayList<ArrayList<Point3d>> polygons3d = 
         create3dPointSets (polygons, T, /*reverse=*/false);
         new ArrayList<ArrayList<Point3d>>();

      Point3d pnt3d = new Point3d (p.x, p.y, 0);
      pnt3d.transform (T);
      WindingCalculator wcalc = new WindingCalculator();
      Vector3d ray = new Vector3d (1, 0, 0);
      Vector3d nrm = new Vector3d ();
      ray.transform (T);
      T.R.getColumn (2, nrm);
      wcalc.init (ray, nrm);
      
      boolean insideCheck = false;
      for (ArrayList<Point3d> polygon3d : polygons3d) {
         int nump = getNumPoints(polygon3d);
         wcalc.start (pnt3d, polygon3d.get(nump-1));
         for (int i=0; i<nump; i++) {
            wcalc.advance (polygon3d.get(i));
         }
         if (wcalc.getNumber() != 0) {
            insideCheck = true;
            break;
         }
      }


      double dtol = 1e-14;
      Polygon3dCalc calc = new Polygon3dCalc(nrm, dtol);
      Polygon3dFeature nfeat = new Polygon3dFeature();
      //nfeat.init (pnt3d, nrm, dtol);


      for (int i=0; i<polygons.size(); i++) {
         ArrayList<Point2d> polygon = polygons.get(i);
         ArrayList<Point3d> polygon3d = polygons3d.get(i);
         int nump = getNumPoints(polygon3d);
         int kstart = -1;
         boolean allOutside = true;

         for (int ka=0; ka<nump; ka++) {
            int kb = (ka+1)%nump;
            double[] clip = intersectSegmentTriangle (
               polygon.get(ka), polygon.get(kb), tri);
            if (clip[0] != -1) {
               allOutside = false;
            }
            if (clip[0] > 0) {
               kstart = ka;
               break;
            }
         }
         if (kstart == -1) {
            if (!allOutside) {
               // polygon is completely inside triangle
               Vertex3dList poly = new Vertex3dList(/*closed=*/true);
               for (int k=0; k<nump; k++) {
                  poly.addNonCoincident (new Vertex3d(polygon3d.get(k)), EPS);
               }
               calc.nearestFeature (nfeat, pnt3d, poly, 0);
            }
         }
         else {
            int ka = kstart;
            Vertex3dList poly = new Vertex3dList(/*closed=*/false);
            for (int j=0; j<nump; j++) {
               int kb = (ka+1)%nump;
               Point3d pa = polygon3d.get(ka);
               Point3d pb = polygon3d.get(kb);

               double[] clip = intersectSegmentTriangle (
                  polygon.get(ka), polygon.get(kb), tri);
               if (clip[0] == 0.0 && clip[1] == 1.0) {
                  poly.addNonCoincident (new Vertex3d(pa), EPS);
                  poly.addNonCoincident (new Vertex3d(pb), EPS);
               }
               else if (clip[0] == 0.0 && clip[1] < 1.0) {
                  poly.addNonCoincident (
                     new Vertex3d(interp (clip[1], pa, pb)), EPS);
                  calc.nearestFeature (nfeat, pnt3d, poly, 0);
                  poly.clear();
               }
               else if (clip[1] > 0.0 && clip[1] == 1.0) {
                  if (poly.size() > 0) {
                     calc.nearestFeature (nfeat, pnt3d, poly, 0);
                     poly.clear();
                  }
                  poly.addNonCoincident (
                     new Vertex3d(interp (clip[0], pa, pb)), EPS);
                  poly.addNonCoincident (new Vertex3d(pb), EPS);
               }
               else if (clip[1] > 0.0 && clip[1] < 1.0) {
                  if (poly.size() > 0) {
                     calc.nearestFeature (nfeat, pnt3d, poly, 0);
                     poly.clear();
                  }
                  poly.addNonCoincident (
                     new Vertex3d (interp (clip[0], pa, pb)), EPS);
                  poly.addNonCoincident (
                     new Vertex3d (interp (clip[1], pa, pb)), EPS);
                  calc.nearestFeature (nfeat, pnt3d, poly, 0);
                  poly.clear();
               }
               else if (clip[0] == -1) {
                  if (poly.size() > 0) {
                     calc.nearestFeature (nfeat, pnt3d, poly, 0);
                     poly.clear();
                  }
               }
               ka = kb;
            }
            if (poly.size() > 0) {
               calc.nearestFeature (nfeat, pnt3d, poly, 0);
               poly.clear();
            }
         }

      }
      boolean inside = (nfeat.isOutside(clockwise) == 0);
      if (inside != insideCheck) {
         System.out.println ("triangle:");
         for (int i=0; i<3; i++) {
            System.out.println (tri[i].toString("%12.8f"));
         }
         System.out.println ("point:");
         System.out.println (p.toString("%12.8f"));
         nfeat.print ("nfeat:");
         throw new TestException (
            "Point "+p+": inside=" + inside + ", expected " + insideCheck);
         
      }
   }

   double[] myTri = new double[] {
      +0.0,  0.0,    1.0,  0.0,   0.0,  1.0, CLOSE };

   double[] myM = new double[] {
      +1.0,  1.0,    0.0, -0.5,  -0.9,  1.0,   -1.0, -1.0,  -0.3, -1.0, 
      -0.5, -0.3,    0.0, -1.0,   0.5, -0.3,    0.3, -1.0,   1.0, -1.0, CLOSE };

   double S = 0.1452*EPS;

   // some points very close together:
   double[] myPnts = new double[] {
      +1.0,  1.0,    1.0+S, 1.0-S,  1.0+2*S, 1.0-2*S, END, 
      +3.0,  4.0,    3.0-S, 4.0-S,  3.0+3*S, 4.0-2*S, END
   };
   
   // a point and a segment
   double[] myMixed = new double[] {
      +1.0,  1.0,    1.0+S, 1.0-S,  1.0+2*S, 1.0-2*S, END, 
      +3.0,  4.0,    3.0-S, 4.0-S,  3.0+3*S, 4.0-2*S,
      +4.0,  3.0,    4.0-S, 3.0-S,  4.0+3*S, 3.0-2*S, END,
   };
   
   // a closed line segment
   double[] mySeg = new double[] {
      0.0, 0.0,   2.0, 3.0,   CLOSE };

   double[] myLines = new double[] {
      1.0, 0.0,   0.0, 2.0,  END,
      2.0, 0.0,   3.0, 2.0,   2.0, 4.0,  3.0, 3.0,  END,
   };

   double[] myPolys = new double[] {
     +0.0,-2.0,   0.0,-1.0,  -1.0,-1.5,  -1.0, 0.0,  1.0, 1.0,   1.0, 2.0,
     -1.0, 1.0,  -2.0, 2.0,  -3.0, 1.0,  -4.0, 4.0, -4.0,-2.0,  -3.0,-2.0,
     -3.0, 0.0,  -2.0, 1.0,  -2.0,-2.0,  CLOSE,
     -3.0, 2.0,  -2.0, 3.0,  -2.0, 4.0,  -3.0, 4.0, CLOSE,
     +2.0,-2.0,   2.0, 1.0,   0.0, 0.0,  CLOSE,
     +4.0,-1.0,   4.0, 4.0,   3.0, 4.0,   3.0, 2.0,  2.0, 3.0,   2.0, 4.0,
     +0.0, 3.0,   0.0, 4.0,  -1.0, 4.0,  -1.0, 2.0,  1.0, 3.0,   3.0, 1.0,
     +3.0,-1.0,   CLOSE
   };
      
   int OUT = 1;
   int IN = 0;
   int UNK = -1;
   
   public void testNearestFeature() {

         //testNF (myTri,   0.400000, -0.100000,  OUT, 0, 1);
         //testNF (myTri,   0.999999, -0.100000,  OUT, 0, 1);
      testNF (myTri,   1.000001, -0.100000,  OUT, 0, 1, 2);
      testNF (myTri,   1.000001, -0.000001,  OUT, 0, 1, 2);
      testNF (myTri,   1.100000,  0.099999,  OUT, 0, 1, 2);
      testNF (myTri,   1.100000,  0.100001,  OUT, 1, 2);
      testNF (myTri,   0.400000,  0.400000,   IN, 1, 2);
      testNF (myTri,   0.000001,  1.000000,  OUT, 1, 2);
      testNF (myTri,  -0.000001,  1.000001,  OUT, 1, 2, 0);
      testNF (myTri,  -0.100000,  1.000001,  OUT, 1, 2, 0);
      testNF (myTri,  -0.100000,  0.999999,  OUT, 2, 0);
      testNF (myTri,  -0.100000,  0.000001,  OUT, 2, 0);
      testNF (myTri,  -0.100000, -0.000001,  OUT, 2, 0, 1);
      testNF (myTri,  -0.100000, -0.100000,  OUT, 2, 0, 1);
      testNF (myTri,  -0.000001, -0.100000,  OUT, 2, 0, 1);
      testNF (myTri,   0.000001, -0.100000,  OUT, 0, 1);

      // testing M just inside each corner

      testNF (myM,     0.9999,    0.9998,     IN, 0, 1);
      testNF (myM,     0.0,      -0.5001,     IN, 0, 1, 2);
      testNF (myM,     0.0001,   -0.5000,     IN, 0, 1);
      testNF (myM,    -0.0001,   -0.5000,     IN, 1, 2);
      testNF (myM,    -0.8999,    0.9998,     IN, 1, 2);
      testNF (myM,    -0.8999,    0.9996,     IN, 2, 3);
      testNF (myM,    -0.9999,   -0.9999,     IN, 2, 3);
      testNF (myM,    -0.9998,   -0.9999,     IN, 3, 4);
      testNF (myM,    -0.3001,   -0.9999,     IN, 4, 5);
      testNF (myM,    -0.5001,   -0.2999,     IN, 4, 5, 6);
      testNF (myM,    -0.5001,   -0.3000,     IN, 4, 5, 6);
      testNF (myM,    -0.5001,   -0.4000,     IN, 4, 5);
      testNF (myM,    -0.5000,   -0.2999,     IN, 4, 5, 6);
      testNF (myM,    -0.4999,   -0.3000,     IN, 5, 6);
      testNF (myM,    -0.5000,   -0.3001,     IN, 4, 5);
      testNF (myM,     0.00001,  -0.9990,     IN, 6, 7);
      testNF (myM,     0.5001,   -0.2999,     IN, 6, 7, 8);
      testNF (myM,     0.5001,   -0.4000,     IN, 7, 8);
      testNF (myM,     0.5001,   -0.3000,     IN, 6, 7, 8);
      testNF (myM,     0.5000,   -0.2999,     IN, 6, 7, 8);
      testNF (myM,     0.4999,   -0.3000,     IN, 6, 7);
      testNF (myM,     0.5000,   -0.3001,     IN, 7, 8);
      testNF (myM,     0.3001,   -0.9999,     IN, 7, 8);
      testNF (myM,     0.9998,   -0.9997,     IN, 9, 0);
      testNF (myM,     0.9998,   -0.9999,     IN, 8, 9);

      // testing M just outside each corner      

      testNF (myM,     1.0001,    1.0001,    OUT, 9, 0, 1);
      testNF (myM,     1.0001,    0.999999,  OUT, 9, 0);
      testNF (myM,     1.0000,    1.0001,    OUT, 9, 0, 1);
      testNF (myM,     0.9999,    1.0000,    OUT, 0, 1);
      testNF (myM,     0.00001,  -0.4999,    OUT, 0, 1);
      testNF (myM,    -0.9001,    1.0001,    OUT, 1, 2, 3);
      testNF (myM,    -0.9001,    1.0000,    OUT, 2, 3);
      testNF (myM,    -0.9000,    1.0001,    OUT, 1, 2, 3);
      testNF (myM,    -0.8999,    1.0000,    OUT, 1, 2);
      testNF (myM,    -1.0001,   -0.9999,    OUT, 2, 3);
      testNF (myM,    -1.0001,   -1.0000,    OUT, 2, 3, 4);
      testNF (myM,    -1.0001,   -1.0001,    OUT, 2, 3, 4);
      testNF (myM,    -0.9999999,-1.0001,    OUT, 3, 4);
      testNF (myM,    -0.9999,   -1.0001,    OUT, 3, 4);
      testNF (myM,    -0.2999,   -0.9999,    OUT, 4, 5);
      testNF (myM,    -0.2999,   -1.0000,    OUT, 3, 4, 5);
      testNF (myM,    -0.2999,   -1.0001,    OUT, 3, 4, 5);
      testNF (myM,    -0.3000001,-1.0001,    OUT, 3, 4);
      testNF (myM,    -0.3001,   -1.0001,    OUT, 3, 4);
      testNF (myM,    -0.4999,   -0.3002,    OUT, 5, 6);
      testNF (myM,     0.0001,   -1.0000,    OUT, 6, 7);
      testNF (myM,     0.0,      -1.0001,    OUT, 5, 6, 7);
      testNF (myM,    -0.0001,   -1.0000,    OUT, 5, 6);
      testNF (myM,     0.4999,   -0.3002,    OUT, 6, 7);
      testNF (myM,     0.2999,   -0.9999,    OUT, 7, 8);
      testNF (myM,     0.2999,   -1.0000,    OUT, 7, 8, 9);
      testNF (myM,     0.2999,   -1.0001,    OUT, 7, 8, 9);
      testNF (myM,     0.3001,   -1.0001,    OUT, 8, 9);
      testNF (myM,     0.299999, -1.0001,    OUT, 7, 8, 9);
      testNF (myM,     1.0001,   -0.9999,    OUT, 9, 0);
      testNF (myM,     1.0001,   -1.0000001, OUT, 8, 9, 0);
      testNF (myM,     1.0001,   -1.0001,    OUT, 8, 9, 0);
      testNF (myM,     0.9999999,-1.0001,    OUT, 8, 9);
      testNF (myM,     0.9999,   -1.0001,    OUT, 8, 9);

      // test the closed line segment

      double SML = 0.001;

      testNF (mySeg,   -SML,     -SML,       UNK, 1, 0, 1);
      testNF (mySeg,   -SML,     -1.0,       UNK, 1, 0, 1);
      testNF (mySeg,    1.5-SML, -1.0,       UNK, 1, 0, 1);
      testNF (mySeg,    1.5+SML, -1.0,       UNK, 0, 1);
      testNF (mySeg,    2.0,     -1.0,       UNK, 0, 1);
      testNF (mySeg,    3.5-SML,  2.0,       UNK, 0, 1);
      testNF (mySeg,    3.5+SML,  2.0,       UNK, 0, 1, 0);
      testNF (mySeg,    0.5+SML,  4.0,       UNK, 0, 1, 0);
      testNF (mySeg,    0.5-SML,  4.0,       UNK, 0, 1);
      testNF (mySeg,  -1.5+SML,   1.0,       UNK, 0, 1);
      testNF (mySeg,  -1.5-SML,   1.0,       UNK, 1, 0, 1);

      // test the point

      testNF (myPnts,  1.02,      1.2,       UNK,  0);
      testNF (myPnts,  3.01,      3.78,      UNK,  3);

      testNF (myMixed, 1.02,      1.2,       OUT,  3, 6);
      testNF (myMixed, 3.01,      3.78,      OUT,  3, 6);

      testNF (myLines, 1.0-EPS,   0.0,        IN,  0, 1);
      testNF (myLines, 1.0+EPS,   0.0,       OUT,  0, 1);
      testNF (myLines, 0.0-EPS,   2.0,        IN, 0, 1);
      testNF (myLines, 0.0+EPS,   2.0,       OUT, 0, 1);

      testNF (myLines, 2.0-EPS,   0.0-EPS,    IN, 2, 3);
      testNF (myLines, 2.2-EPS,  -0.1-EPS,   OUT, 2, 3);
      testNF (myLines, 2.2-EPS,  -0.1+EPS,   OUT, 2, 3);
      testNF (myLines, 1.8+EPS,   0.1-EPS,    IN, 2, 3);
      testNF (myLines, 1.8+EPS,   0.1+EPS,    IN, 2, 3);

      testNF (myLines, 2.9,       2.0-EPS,    IN, 2, 3);
      testNF (myLines, 2.9,       2.0+EPS,    IN, 3, 4);
      testNF (myLines, 3.2,       1.9-EPS,   OUT, 2, 3);
      testNF (myLines, 3.2,       1.9+EPS,   OUT, 2, 3, 4);
      testNF (myLines, 3.2,       2.1-EPS,   OUT, 2, 3, 4);
      
      testNF (myLines, 3.2,       2.1+EPS,   OUT, 3, 4);

      testNF (myLines, 1.8,       3.9-EPS,    IN, 3, 4);
      testNF (myLines, 1.8,       3.9+EPS,    IN, 3, 4, 5);
      testNF (myLines, 2.2,       4.2+EPS,    IN, 3, 4, 5);
      testNF (myLines, 2.2,       4.2-EPS,    IN, 4, 5);

      testNF (myLines, 3.1,       3.1-EPS,    IN, 4, 5);
      testNF (myLines, 3.1,       3.1+EPS,    IN, 4, 5);
      testNF (myLines, 2.9,       2.9-EPS,   OUT, 4, 5);
      testNF (myLines, 2.9,       2.9+EPS,   OUT, 4, 5);
      
      double[] triangle = new double[] {
        -3, -1,  5, -2, 5, 5 };
      
      insideTest (myPolys, 3, triangle);
   }

   double[] longPoly = new double[] {
      4.617858690349127, 4.617858690349127, 5.0,
      4.638185701989151, 4.9022519930221975, 5.0,
      4.673832265199071, 5.400979696815883, 5.0,
      4.70947882840899, 5.899707400609566, 5.0,
      4.7451253916189104, 6.3984351044032515, 5.0,
      4.780771954828831, 6.897162808196936, 5.0,
      4.816418518038751, 7.39589051199062, 5.0,
      4.852065081248671, 7.8946182157843054, 5.0,
      4.887711644458592, 8.393345919577989, 5.0,
      4.923358207668512, 8.892073623371674, 5.0,
      4.959004770878432, 9.39080132716536, 5.0,
      4.994651334088353, 9.889529030959043, 5.0,
      5.030297897298272, 10.388256734752728, 5.0,
      5.065944460508192, 10.886984438546413, 5.0,
      5.101591023718112, 11.385712142340097, 5.0,
      5.137237586928032, 11.88443984613378, 5.0,
      5.1728841501379526, 12.383167549927467, 5.0,
      5.208530713347873, 12.88189525372115, 5.0,
      5.244177276557793, 13.380622957514836, 5.0,
      5.2798238397677135, 13.879350661308521, 5.0,
      5.778551543561397, 13.8437040980986, 5.0,
      6.277279247355082, 13.808057534888679, 5.0,
      6.776006951148766, 13.77241097167876, 5.0,
      7.274734654942451, 13.73676440846884, 5.0,
      7.7734623587361344, 13.70111784525892, 5.0,
      8.27219006252982, 13.665471282049, 5.0,
      8.770917766323503, 13.629824718839078, 5.0,
      9.269645470117188, 13.594178155629159, 5.0,
      9.768373173910874, 13.55853159241924, 5.0,
      10.267100877704559, 13.52288502920932, 5.0,
      10.765828581498242, 13.487238465999399, 5.0,
      11.264556285291928, 13.451591902789477, 5.0,
      11.763283989085611, 13.415945339579558, 5.0,
      12.262011692879296, 13.380298776369639, 5.0,
      12.76073939667298, 13.34465221315972, 5.0,
      13.259467100466665, 13.309005649949798, 5.0,
      13.255653775153409, 13.255653775153409, 5.0,
      14.241374874535156, 14.241374874535156, 5.0,
      13.829487930680191, 14.270814494327247, 5.0,
      13.330760226886506, 14.306461057537167, 5.0,
      12.83203252309282, 14.342107620747088, 5.0,
      12.333304819299137, 14.377754183957009, 5.0,
      11.834577115505452, 14.413400747166929, 5.0,
      11.335849411711767, 14.449047310376848, 5.0,
      10.837121707918083, 14.484693873586767, 5.0,
      10.3383940041244, 14.520340436796689, 5.0,
      9.839666300330714, 14.55598700000661, 5.0,
      9.340938596537029, 14.59163356321653, 5.0,
      8.842210892743342, 14.627280126426449, 5.0,
      8.343483188949659, 14.662926689636368, 5.0,
      7.844755485155974, 14.69857325284629, 5.0,
      7.346027781362291, 14.73421981605621, 5.0,
      6.847300077568606, 14.76986637926613, 5.0,
      6.348572373774923, 14.80551294247605, 5.0,
      5.849844669981238, 14.841159505685969, 5.0,
      5.351116966187554, 14.87680606889589, 5.0,
      4.852389262393869, 14.912452632105811, 5.0,
      4.353661558600184, 14.94809919531573, 5.0,
      4.318014995390263, 14.449371491522045, 5.0,
      4.282368432180343, 13.95064378772836, 5.0,
      4.246721868970422, 13.451916083934673, 5.0,
      4.211075305760502, 12.95318838014099, 5.0,
      4.175428742550581, 12.454460676347306, 5.0,
      4.139782179340662, 11.955732972553623, 5.0,
      4.1041356161307405, 11.457005268759938, 5.0,
      4.068489052920821, 10.958277564966252, 5.0,
      4.032842489710902, 10.459549861172569, 5.0,
      3.997195926500982, 9.960822157378884, 5.0,
      3.961549363291062, 9.462094453585198, 5.0,
      3.9259028000811416, 8.963366749791515, 5.0,
      3.8902562368712212, 8.46463904599783, 5.0,
      3.854609673661301, 7.965911342204145, 5.0,
      3.8189631104513806, 7.467183638410461, 5.0,
      3.7833165472414603, 6.9684559346167765, 5.0,
      3.74766998403154, 6.469728230823092, 5.0,
      3.7120234208216196, 5.971000527029407, 5.0,
      3.6763768576117, 5.4722728232357225, 5.0,
      3.6407302944017808, 4.973545119442038, 5.0,
      3.6050837311918604, 4.474817415648353, 5.0,
      3.56943716798194, 3.9760897118546685, 5.0,
      3.5381343047485956, 3.5381343047485956, 5.0,
   };

   double[] holeTriangle = new double[] {
      5.0, -5.0, 0.0,
      5.0, 5.0, 0.0,
      -5.0, 5.0, 0.0,
   };

   double[] hole0 = new double[] {
      3.0, -1.4766200423477458, 2.1684043449710089E-19,
      2.6117656100123083, -1.0, 2.1684043449710089E-19,
      3.0, -0.843574415217252, 2.1684043449710089E-19,
      3.2766730928634455, -1.0, -8.673617379884035E-19,
   };

   double[] hole1 = new double[] {
      3.0, 2.6635016477605364, -1.3010426069826053E-18,
      2.584941216554936, 3.0, 0.0,
      3.0000000000000004, 3.5536038529683243, -4.3368086899420177E-19,
      3.5536038529683243, 3.0, 8.673617379884035E-19,
   };

   public void testSpecial0() {
      Vertex3dList vtxs = new Vertex3dList(/*closed=*/true, longPoly);
      Polygon3dCalc calc = new Polygon3dCalc (
         Vector3d.Z_UNIT, 1.1357816691600548E-10);
      ArrayList<Vertex3d> triVtxs = new ArrayList<Vertex3d>();
      calc.debug = true;
      calc.triangulate (triVtxs, vtxs, null);
   }

   public void testSpecial1() {
      Vertex3dList vtxs = new Vertex3dList(/*closed=*/true);
      int idx = vtxs.set(holeTriangle);
      Polygon3dCalc calc = new Polygon3dCalc (
         Vector3d.Z_UNIT, 1.1357816691600548E-10);
      ArrayList<Vertex3d> triVtxs = new ArrayList<Vertex3d>();
      ArrayList<Vertex3dList> holes = new ArrayList<Vertex3dList>();
      Vertex3dList hole = new Vertex3dList(/*closed=*/true);
      idx = hole.set (hole0, idx);
      holes.add (hole);
      hole = new Vertex3dList(/*closed=*/true);
      idx = hole.set (hole1, idx);
      holes.add (hole);
      calc.debug = true;

               System.out.println ("outer poly");
               for (Vertex3dNode n : vtxs) {
                  System.out.println (
                     " " + n.getVertex().getIndex() +
                     " " + n.getVertex().pnt.toString("%8.3f"));
               }
               for (int hi=0; hi<holes.size(); hi++) {
                  System.out.println ("hole " + hi);
                  for (Vertex3dNode n : holes.get(hi)) {
                     System.out.println (
                        " " + n.getVertex().getIndex() +
                        " " + n.getVertex().pnt.toString("%8.3f"));
                  }
               }
      calc.triangulate (triVtxs, vtxs, holes);
   }

   public void test() {

      //testSpecial0();
      //testTriangulate ();
      testNearestFeature ();
   }

   private ArrayList<Vertex3d> createVertices (double[] pval, int idx) {
      ArrayList<Vertex3d> vtxs = new ArrayList<Vertex3d>(pval.length/2);
      for (int i=0; i<pval.length; i+=2) {
         Vertex3d vtx = new Vertex3d (pval[i], pval[i+1], 0);
         vtx.setIndex (idx++);
         vtxs.add (vtx);
      }
      return vtxs;
   }

   private void triangulateFromFile (String fileName) {
      
      Vector3d nrm = new Vector3d();
      double dtol = 0;
      ArrayList<Vector3d> vertices = new ArrayList<Vector3d>();
         
      try {
         ReaderTokenizer rtok =
            new ReaderTokenizer (new BufferedReader (new FileReader (fileName)));
         rtok.wordChars ("./$");
         nrm.scan (rtok);
         dtol = rtok.scanNumber();
         while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            rtok.pushBack();
            Vector3d v = new Vector3d();
            v.scan (rtok);
            vertices.add (v);
         }
         rtok.close();
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
      Polygon3dCalc calc = new Polygon3dCalc (nrm, dtol);
      Vertex3dList vtxs = new Vertex3dList(/*closed=*/true, vertices);
      ArrayList<Vertex3d> triVtxs = new ArrayList<Vertex3d>();
      calc.debug = true;
      calc.triangulate (triVtxs, vtxs, null);
      printTriangles (triVtxs);
   }

   private static void printUsageAndExit() {
      System.out.println (
         "Usage: Polygon3dCalcTest [-triangulate <file>] -help");
      System.exit(1); 
   }

   public static void main (String[] args) {

      String triangulateFile = null;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-triangulate")) {
            i++;
            if (i == args.length) {
               System.out.println ("-triangulate requires a file name argument");
               System.exit(1); 
            }
            triangulateFile = args[i];
         }
         else if (args[i].equals ("-help")) {
            printUsageAndExit();
         }
         else {
            printUsageAndExit();            
         }
      }
      
      Polygon3dCalcTest tester = new Polygon3dCalcTest();

      if (triangulateFile != null) {
         tester.triangulateFromFile (triangulateFile);
         return;
      }

      //tester.testSpecial1();
      tester.runtest();
   }
}
