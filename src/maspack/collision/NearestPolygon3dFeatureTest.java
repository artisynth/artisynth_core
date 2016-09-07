package maspack.collision;

import java.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.util.*;

public class NearestPolygon3dFeatureTest extends UnitTest {

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

   public void test (
      double[] pvals, double px, double py, int outsideCheck, int... vtxCheck) {

      NearestPolygon3dFeature nfeat = new NearestPolygon3dFeature();
      nfeat.init (new Point3d (px, py, 0), Vector3d.Z_UNIT, EPS);

      ArrayList<ArrayList<Point2d>> pntSets = createPointSets (pvals);
      test (pntSets, new Point3d(px, py, 0), outsideCheck, vtxCheck);
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

   public void test (
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

      NearestPolygon3dFeature nfeat = new NearestPolygon3dFeature();
      nfeat.init (px, nrm, EPS);

      ArrayList<Point3d> allPnts = new ArrayList<Point3d>();

      int k = 0;
      for (ArrayList<Point3d> pntSet : pntSets) {
         for (int j=0; j<pntSet.size(); j++) {
            Point3d p = pntSet.get(j);
            if (j == 0) {
               if (k == 0) {
                  nfeat.start();
               }
               else {
                  nfeat.restart();
               }
               nfeat.advance (p);
            }
            else {
               if (p == null) {
                  nfeat.close();
               }
               else {
                  nfeat.advance (p);
               }
            }
            allPnts.add (p);
         }
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

      NearestPolygon3dFeature nfeat = new NearestPolygon3dFeature();
      double dtol = 1e-14;
      nfeat.init (pnt3d, nrm, dtol);

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
               nfeat.restart ();
               for (int k=0; k<nump; k++) {
                  nfeat.advance (polygon3d.get(k));
               }
               nfeat.close();
            }
         }
         else {
            int ka = kstart;
            boolean restart = true;
            for (int j=0; j<nump; j++) {
               int kb = (ka+1)%nump;
               Point3d pa = polygon3d.get(ka);
               Point3d pb = polygon3d.get(kb);

               double[] clip = intersectSegmentTriangle (
                  polygon.get(ka), polygon.get(kb), tri);
               if (clip[0] == 0.0 && clip[1] == 1.0) {
                  if (restart) {
                     nfeat.restart ();
                  }
                  nfeat.advance (pa);
                  nfeat.advance (pb);
                  restart = false;
               }
               else if (clip[0] == 0.0 && clip[1] < 1.0) {
                  if (restart) {
                     nfeat.restart ();
                  }
                  nfeat.advance (interp (clip[1], pa, pb));
                  restart = true;
               }
               else if (clip[1] > 0.0 && clip[1] == 1.0) {
                  nfeat.restart ();
                  nfeat.advance (interp (clip[0], pa, pb));
                  nfeat.advance (pb);
                  restart = false;
               }
               else if (clip[1] > 0.0 && clip[1] < 1.0) {
                  nfeat.restart ();
                  nfeat.advance (interp (clip[0], pa, pb));
                  nfeat.advance (interp (clip[1], pa, pb));
                  restart = true;
               }
               else if (clip[0] == -1) {
                  restart = true;
               }
               ka = kb;
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
   
   public void test() {
         
      test (myTri,   0.400000, -0.100000,  OUT, 0, 1);
      test (myTri,   0.999999, -0.100000,  OUT, 0, 1);
      test (myTri,   1.000001, -0.100000,  OUT, 0, 1, 2);
      test (myTri,   1.000001, -0.000001,  OUT, 0, 1, 2);
      test (myTri,   1.100000,  0.099999,  OUT, 0, 1, 2);
      test (myTri,   1.100000,  0.100001,  OUT, 1, 2);
      test (myTri,   0.400000,  0.400000,   IN, 1, 2);
      test (myTri,   0.000001,  1.000000,  OUT, 1, 2);
      test (myTri,  -0.000001,  1.000001,  OUT, 1, 2, 0);
      test (myTri,  -0.100000,  1.000001,  OUT, 1, 2, 0);
      test (myTri,  -0.100000,  0.999999,  OUT, 2, 0);
      test (myTri,  -0.100000,  0.000001,  OUT, 2, 0);
      test (myTri,  -0.100000, -0.000001,  OUT, 2, 0, 1);
      test (myTri,  -0.100000, -0.100000,  OUT, 2, 0, 1);
      test (myTri,  -0.000001, -0.100000,  OUT, 2, 0, 1);
      test (myTri,   0.000001, -0.100000,  OUT, 0, 1);

      // testing M just inside each corner

      test (myM,     0.9999,    0.9998,     IN, 0, 1);
      test (myM,     0.0,      -0.5001,     IN, 0, 1, 2);
      test (myM,     0.0001,   -0.5000,     IN, 0, 1);
      test (myM,    -0.0001,   -0.5000,     IN, 1, 2);
      test (myM,    -0.8999,    0.9998,     IN, 1, 2);
      test (myM,    -0.8999,    0.9996,     IN, 2, 3);
      test (myM,    -0.9999,   -0.9999,     IN, 2, 3);
      test (myM,    -0.9998,   -0.9999,     IN, 3, 4);
      test (myM,    -0.3001,   -0.9999,     IN, 4, 5);
      test (myM,    -0.5001,   -0.2999,     IN, 4, 5, 6);
      test (myM,    -0.5001,   -0.3000,     IN, 4, 5, 6);
      test (myM,    -0.5001,   -0.4000,     IN, 4, 5);
      test (myM,    -0.5000,   -0.2999,     IN, 4, 5, 6);
      test (myM,    -0.4999,   -0.3000,     IN, 5, 6);
      test (myM,    -0.5000,   -0.3001,     IN, 4, 5);
      test (myM,     0.00001,  -0.9990,     IN, 6, 7);
      test (myM,     0.5001,   -0.2999,     IN, 6, 7, 8);
      test (myM,     0.5001,   -0.4000,     IN, 7, 8);
      test (myM,     0.5001,   -0.3000,     IN, 6, 7, 8);
      test (myM,     0.5000,   -0.2999,     IN, 6, 7, 8);
      test (myM,     0.4999,   -0.3000,     IN, 6, 7);
      test (myM,     0.5000,   -0.3001,     IN, 7, 8);
      test (myM,     0.3001,   -0.9999,     IN, 7, 8);
      test (myM,     0.9998,   -0.9997,     IN, 9, 0);
      test (myM,     0.9998,   -0.9999,     IN, 8, 9);

      // testing M just outside each corner      

      test (myM,     1.0001,    1.0001,    OUT, 9, 0, 1);
      test (myM,     1.0001,    0.999999,  OUT, 9, 0);
      test (myM,     1.0000,    1.0001,    OUT, 9, 0, 1);
      test (myM,     0.9999,    1.0000,    OUT, 0, 1);
      test (myM,     0.00001,  -0.4999,    OUT, 0, 1);
      test (myM,    -0.9001,    1.0001,    OUT, 1, 2, 3);
      test (myM,    -0.9001,    1.0000,    OUT, 2, 3);
      test (myM,    -0.9000,    1.0001,    OUT, 1, 2, 3);
      test (myM,    -0.8999,    1.0000,    OUT, 1, 2);
      test (myM,    -1.0001,   -0.9999,    OUT, 2, 3);
      test (myM,    -1.0001,   -1.0000,    OUT, 2, 3, 4);
      test (myM,    -1.0001,   -1.0001,    OUT, 2, 3, 4);
      test (myM,    -0.9999999,-1.0001,    OUT, 3, 4);
      test (myM,    -0.9999,   -1.0001,    OUT, 3, 4);
      test (myM,    -0.2999,   -0.9999,    OUT, 4, 5);
      test (myM,    -0.2999,   -1.0000,    OUT, 3, 4, 5);
      test (myM,    -0.2999,   -1.0001,    OUT, 3, 4, 5);
      test (myM,    -0.3000001,-1.0001,    OUT, 3, 4);
      test (myM,    -0.3001,   -1.0001,    OUT, 3, 4);
      test (myM,    -0.4999,   -0.3002,    OUT, 5, 6);
      test (myM,     0.0001,   -1.0000,    OUT, 6, 7);
      test (myM,     0.0,      -1.0001,    OUT, 5, 6, 7);
      test (myM,    -0.0001,   -1.0000,    OUT, 5, 6);
      test (myM,     0.4999,   -0.3002,    OUT, 6, 7);
      test (myM,     0.2999,   -0.9999,    OUT, 7, 8);
      test (myM,     0.2999,   -1.0000,    OUT, 7, 8, 9);
      test (myM,     0.2999,   -1.0001,    OUT, 7, 8, 9);
      test (myM,     0.3001,   -1.0001,    OUT, 8, 9);
      test (myM,     0.299999, -1.0001,    OUT, 7, 8, 9);
      test (myM,     1.0001,   -0.9999,    OUT, 9, 0);
      test (myM,     1.0001,   -1.0000001, OUT, 8, 9, 0);
      test (myM,     1.0001,   -1.0001,    OUT, 8, 9, 0);
      test (myM,     0.9999999,-1.0001,    OUT, 8, 9);
      test (myM,     0.9999,   -1.0001,    OUT, 8, 9);

      // test the closed line segment

      double SML = 0.001;

      test (mySeg,   -SML,     -SML,       UNK, 1, 0, 1);
      test (mySeg,   -SML,     -1.0,       UNK, 1, 0, 1);
      test (mySeg,    1.5-SML, -1.0,       UNK, 1, 0, 1);
      test (mySeg,    1.5+SML, -1.0,       UNK, 0, 1);
      test (mySeg,    2.0,     -1.0,       UNK, 0, 1);
      test (mySeg,    3.5-SML,  2.0,       UNK, 0, 1);
      test (mySeg,    3.5+SML,  2.0,       UNK, 0, 1, 0);
      test (mySeg,    0.5+SML,  4.0,       UNK, 0, 1, 0);
      test (mySeg,    0.5-SML,  4.0,       UNK, 0, 1);
      test (mySeg,  -1.5+SML,   1.0,       UNK, 0, 1);
      test (mySeg,  -1.5-SML,   1.0,       UNK, 1, 0, 1);

      // test the point

      test (myPnts,  1.02,      1.2,       UNK,  0);
      test (myPnts,  3.01,      3.78,      UNK,  3);

      test (myMixed, 1.02,      1.2,       OUT,  3, 6);
      test (myMixed, 3.01,      3.78,      OUT,  3, 6);

      test (myLines, 1.0-EPS,   0.0,        IN,  0, 1);
      test (myLines, 1.0+EPS,   0.0,       OUT,  0, 1);
      test (myLines, 0.0-EPS,   2.0,        IN, 0, 1);
      test (myLines, 0.0+EPS,   2.0,       OUT, 0, 1);

      test (myLines, 2.0-EPS,   0.0-EPS,    IN, 2, 3);
      test (myLines, 2.2-EPS,  -0.1-EPS,   OUT, 2, 3);
      test (myLines, 2.2-EPS,  -0.1+EPS,   OUT, 2, 3);
      test (myLines, 1.8+EPS,   0.1-EPS,    IN, 2, 3);
      test (myLines, 1.8+EPS,   0.1+EPS,    IN, 2, 3);

      test (myLines, 2.9,       2.0-EPS,    IN, 2, 3);
      test (myLines, 2.9,       2.0+EPS,    IN, 3, 4);
      test (myLines, 3.2,       1.9-EPS,   OUT, 2, 3);
      test (myLines, 3.2,       1.9+EPS,   OUT, 2, 3, 4);
      test (myLines, 3.2,       2.1-EPS,   OUT, 2, 3, 4);
      
      test (myLines, 3.2,       2.1+EPS,   OUT, 3, 4);

      test (myLines, 1.8,       3.9-EPS,    IN, 3, 4);
      test (myLines, 1.8,       3.9+EPS,    IN, 3, 4, 5);
      test (myLines, 2.2,       4.2+EPS,    IN, 3, 4, 5);
      test (myLines, 2.2,       4.2-EPS,    IN, 4, 5);

      test (myLines, 3.1,       3.1-EPS,    IN, 4, 5);
      test (myLines, 3.1,       3.1+EPS,    IN, 4, 5);
      test (myLines, 2.9,       2.9-EPS,   OUT, 4, 5);
      test (myLines, 2.9,       2.9+EPS,   OUT, 4, 5);
      
      double[] triangle = new double[] {
        -3, -1,  5, -2, 5, 5 };
      
      insideTest (myPolys, 3, triangle);
      
   }

   public static void main (String[] args) {
      NearestPolygon3dFeatureTest tester = new NearestPolygon3dFeatureTest();

      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }

}

