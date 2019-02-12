package maspack.collision;

// Timing test - full test with EdgeInfo on sarlat: 128u

// seed 18e80e3f
import java.util.*;
import java.io.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.geometry.io.*;
import maspack.geometry.MeshFactory.VertexMap;
import maspack.geometry.MeshFactory.FaceType ;
import maspack.collision.SurfaceMeshIntersector.FaceCalculator;
import maspack.collision.SurfaceMeshIntersector.RegionType;
import maspack.collision.SurfaceMeshIntersector.CSG;

import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.IntHolder;
import argparser.StringHolder;

public class SurfaceMeshIntersectorTest extends UnitTest {

   double DOUBLE_PREC = 1e-16;
   double EPS = 1e-9;

   private int ALL_DEGENERACIES = PolygonalMesh.ALL_DEGENERACIES;
   private int NON_MANIFOLD_EDGES = PolygonalMesh.NON_MANIFOLD_EDGES;
   private int NON_MANIFOLD_VERTICES = PolygonalMesh.NON_MANIFOLD_VERTICES;
   private int ISOLATED_VERTICES = PolygonalMesh.ISOLATED_VERTICES;
   private int OPEN_EDGES = PolygonalMesh.OPEN_EDGES;

   private int MANIFOLD =
      (NON_MANIFOLD_EDGES | NON_MANIFOLD_VERTICES | ISOLATED_VERTICES);

   private int WATERTIGHT =
      (NON_MANIFOLD_EDGES | ISOLATED_VERTICES | OPEN_EDGES);

   private int CLOSED =
      (MANIFOLD | OPEN_EDGES);

   boolean printContourIndexMapping = false;
   boolean checkIntersectionMesh = true;
   boolean checkUnionMesh = true;
   boolean checkDifferenceMesh = true;
   boolean checkCoincidentOrdering = true;

   int checkIntersectionTopology = CLOSED;
   int checkUnionTopology = CLOSED;
   int checkDifferenceTopology = CLOSED;

   int passCount = 0;
   int failCount = 0;
   boolean abortOnFail = true;
   boolean printFailInfo = true;

   int numNonManifoldEdges = 0;
   int numNonManifoldVertices = 0;
   int numOpenEdges = 0;
   int numIsolatedVertices = 0;

   int numNonManifoldEdgeChecks = 0;
   int numNonManifoldVertexChecks = 0;
   int numOpenEdgeChecks = 0;
   int numIsolatedVertexChecks = 0;

   static double OPEN = -1.0;

   PolygonalMesh myDiamond;

   int myRandomSeed;
   boolean mySaveTestResults;

   PolygonalMesh myLastMesh0;
   PolygonalMesh myLastMesh1;
   RigidTransform3d myLastMeshToWorld0;
   RigidTransform3d myLastMeshToWorld1;
   ContactInfo myLastContactInfo;
   PolygonalMesh myLastImesh;
   PolygonalMesh myLastUmesh;

   private abstract class RandomTransform {

      public abstract RigidTransform3d nextTransform();
   }

   public boolean getSaveTestResults() {
      return mySaveTestResults;
   }

   public void setSaveTestResults (boolean enable) {
      mySaveTestResults = enable;
   }

   public String getTestFailFileName() {
      return myTestFailFileName;
   }

   public void setTestFailFileName (String name) {
      myTestFailFileName = name;
   }

   public PolygonalMesh getLastMesh0() {
      return myLastMesh0;
   }

   public PolygonalMesh getLastMesh1() {
      return myLastMesh1;
   }

   public RigidTransform3d getLastMeshToWorld0() {
      return myLastMeshToWorld0;
   }

   public RigidTransform3d getLastMeshToWorld1() {
      return myLastMeshToWorld1;
   }

   public PolygonalMesh getLastIMesh() {
      return myLastImesh;
   }

   public PolygonalMesh getLastUMesh() {
      return myLastUmesh;
   }

   public ContactInfo getLastContactInfo() {
      return myLastContactInfo;
   }

   private class RandomTransform2d extends RandomTransform {

      double myRange;

      public RandomTransform2d (double range) {
         myRange = range;
      }

      public RigidTransform3d nextTransform() {
         double x = RandomGenerator.nextDouble (-myRange, myRange);
         double y = RandomGenerator.nextDouble (-myRange, myRange);
         double ang = RandomGenerator.nextDouble (-Math.PI, Math.PI);
         return new RigidTransform3d (x, y, 0, ang, 0, 0);
         //return new RigidTransform3d (0, 0, 0, 0, 0, 0);
      }
   }
   
   public SurfaceMeshIntersectorTest (int randomSeed) {

      if (randomSeed == -1) {
         // use random seed
         randomSeed = RandomGenerator.nextInt (Integer.MAX_VALUE);
      }
      RandomGenerator.setSeed (randomSeed);
      myRandomSeed = randomSeed;
      //System.out.printf ("random seed=%x\n", randomSeed);

      myDiamond = new PolygonalMesh();
      try {
         myDiamond.read (
            new StringReader (
               "v 0 0 0.5\n" +
               "v -0.5 -0.5 0.0\n" +
               "v 0.5 -0.5 0.0\n" +
               "v 0.5 0.5 0.0\n" +
               "v -0.5 0.5 0.0\n" +
               "v 0 0 -0.5\n" +
               "f 1 2 3\n" + 
               "f 1 3 4\n" + 
               "f 1 4 5\n" + 
               "f 1 5 2\n" + 
               "f 6 3 2\n" + 
               "f 6 4 3\n" + 
               "f 6 5 4\n" + 
               "f 6 2 5"));     
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
   }

   private boolean debug = false;
   private boolean debugx = false;

   private static void writeMesh (PrintWriter pw, PolygonalMesh mesh)
      throws IOException  {

      NumberFormat fmt = new NumberFormat ("%g");
      RigidTransform3d TMW;

      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      pw.print ("[ ");      
      IndentingPrintWriter.addIndentation (pw, 2);
      TMW = mesh.getMeshToWorld();
      TMW.write (pw, fmt);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
      mesh.write (pw, fmt, /* zeroIndexed= */false);
      pw.println ("EOF"); 
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   private static void scanMesh (ReaderTokenizer rtok, PolygonalMesh mesh)
      throws IOException  {

      RigidTransform3d TMW = new RigidTransform3d();
      rtok.scanToken ('[');
      TMW.scan (rtok);
      mesh.read (rtok, false);
      mesh.setMeshToWorld (TMW);
      rtok.scanToken (']');
   }

   public static void writeProblem (
      String fileName, PolygonalMesh mesh0, PolygonalMesh mesh1, 
      TestInfo tinfo) throws IOException {
      writeProblem (fileName, mesh0, mesh1, tinfo, null);
   }

   public static void writeProblem (
      String fileName, PolygonalMesh mesh0, PolygonalMesh mesh1, 
      TestInfo tinfo, CSG csgOp) throws IOException {
      PrintWriter pw = new IndentingPrintWriter (
         new PrintWriter (new BufferedWriter (new FileWriter (fileName))));
      writeProblem (pw, mesh0, mesh1, tinfo, csgOp);
   }

   public static void writeProblem (
      PrintWriter pw, PolygonalMesh mesh0, PolygonalMesh mesh1, TestInfo tinfo)
      throws IOException {
      writeProblem (pw, mesh0, mesh1, tinfo, null);
   }
   
   public static void writeProblem (
      PrintWriter pw, PolygonalMesh mesh0, PolygonalMesh mesh1, 
      TestInfo tinfo, CSG csgOp)
      throws IOException {

      pw.print ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      pw.print ("mesh0=");
      writeMesh (pw, mesh0);
      pw.print ("mesh1=");
      writeMesh (pw, mesh1);
      if (tinfo != null) {
         pw.print ("testInfo=");
         tinfo.write (pw);
      }
      if (csgOp != null) {
         pw.println ("csgOp=" + csgOp);
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
      pw.close();
   }
   
   public static CSG scanProblem (
      String fileName,
      PolygonalMesh mesh0, PolygonalMesh mesh1, TestInfo tinfo) 
      throws IOException {

      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (new FileReader (fileName)));
      return scanProblem (rtok, mesh0, mesh1, tinfo);
   }

   public static CSG scanProblem (
      ReaderTokenizer rtok,
      PolygonalMesh mesh0, PolygonalMesh mesh1, TestInfo tinfo)
      throws IOException {
   
      CSG csgOp = null;
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord ("mesh0")) {
            rtok.scanToken ('=');
            scanMesh (rtok, mesh0);
         }
         else if (rtok.tokenIsWord ("mesh1")) {
            rtok.scanToken ('=');
            scanMesh (rtok, mesh1);
         }
         else if (rtok.tokenIsWord ("testInfo")) {
            rtok.scanToken ('=');
            tinfo.scan (rtok);
         }
         else if (rtok.tokenIsWord ("csgOp")) {
            rtok.scanToken ('=');
            String opName = rtok.scanWord();
            try {
               csgOp = CSG.valueOf(opName);
            }
            catch (Exception e) {
               throw new IOException ("Unrecognized csg type: "+rtok);
            }
         }
         else {
            throw new IOException ("Unexpected token " + rtok);
         }
      }
      return csgOp;
   }

   public static class TestInfo {
      public int[][] regions0Check;
      public int[][] regions1Check;
      public double[] regions0AreaCheck;
      public double[] regions1AreaCheck;
      public double[][] contoursCheck;
      public RigidTransform3d TBW;
      public RigidTransform3d T10;


      public void write (PrintWriter pw) throws IOException {
         IndentingPrintWriter.printOpening (pw, "[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         pw.print ("contours=");
         writeContours (pw, contoursCheck);
         pw.print ("regions0=");
         writeRegions (pw, regions0Check);
         pw.print ("regions1=");
         writeRegions (pw, regions1Check);
         pw.print ("regions0Area=");
         writeRegionAreas (pw, regions0AreaCheck);
         pw.print ("regions1Area=");
         writeRegionAreas (pw, regions1AreaCheck);
         if (TBW != null) {
            pw.println ("TBW=[");
            IndentingPrintWriter.addIndentation (pw, 2);
            TBW.write (pw, new NumberFormat ("%g"));
            IndentingPrintWriter.addIndentation (pw, -2);
            pw.println ("]");
         }
         if (T10 != null) {
            pw.println ("T10=[");
            IndentingPrintWriter.addIndentation (pw, 2);
            T10.write (pw, new NumberFormat ("%g"));
            IndentingPrintWriter.addIndentation (pw, -2);
            pw.println ("]");
         }
         
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");         
      }
      
      public void scan (ReaderTokenizer rtok) throws IOException {
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            if (rtok.tokenIsWord ("contours")) {
               rtok.scanToken ('=');
               contoursCheck = scanContours (rtok);
            }
            else if (rtok.tokenIsWord ("regions0")) {
               rtok.scanToken ('=');
               regions0Check = scanRegions (rtok);
            }
            else if (rtok.tokenIsWord ("regions1")) {
               rtok.scanToken ('=');
               regions1Check = scanRegions (rtok);
            }
            else if (rtok.tokenIsWord ("regions0Area")) {
               rtok.scanToken ('=');
               regions0AreaCheck = scanRegionAreas (rtok);
            }
            else if (rtok.tokenIsWord ("regions1Area")) {
               rtok.scanToken ('=');
               regions1AreaCheck = scanRegionAreas (rtok);
            }
            else if (rtok.tokenIsWord ("TBW")) {
               rtok.scanToken ('=');
               TBW = new RigidTransform3d();
               TBW.scan (rtok);
            }
            else if (rtok.tokenIsWord ("T10")) {
               rtok.scanToken ('=');
               T10 = new RigidTransform3d();
               T10.scan (rtok);
            }
            else {
               throw new IOException ("Unexpected token " + rtok);
            }
         }
      }

      public int[][] scanRegions (ReaderTokenizer rtok) throws IOException {
         ArrayList<int[]> regionList = new ArrayList<int[]>();
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            if (rtok.tokenIsWord ("null")) {
               regionList.add (null);
            }
            else {
               rtok.pushBack();
               regionList.add (Scan.scanInts (rtok));
            }
         }
         int[][] regions = new int[regionList.size()][];
         for (int i=0; i<regions.length; i++) {
            regions[i] = regionList.get(i);
         }
         return regions;
      }

      public double[] scanRegionAreas (ReaderTokenizer rtok) throws IOException {
         rtok.nextToken();
         if (rtok.tokenIsWord ("null")) {
            return null;
         }
         else {
            rtok.pushBack();
            return Scan.scanDoubles (rtok);
         }
      }

      public void writeRegions (PrintWriter pw, int[][] regions)
         throws IOException {
         pw.println ("[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (int i=0; i<regions.length; i++) {
            if (regions[i] == null) {
               pw.println ("null");
            }
            else {
               Write.writeInts (pw, regions[i], null);
            }
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println (" ]");
      }

      public void writeRegionAreas (PrintWriter pw, double[] regionAreas)
         throws IOException {
         if (regionAreas == null) {
            pw.println ("null");
         }
         else {
            Write.writeDoubles (pw, regionAreas, null);           
         }
      }

      public double[][] scanContours (ReaderTokenizer rtok) throws IOException {
         ArrayList<double[]> contourList = new ArrayList<double[]>();
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            contourList.add (Scan.scanDoubles (rtok));
         }
         double[][] contours = new double[contourList.size()][];
         for (int i=0; i<contours.length; i++) {
            contours[i] = contourList.get(i);
         }
         return contours;
      }

      public void writeContours (PrintWriter pw, double[][] contours)
         throws IOException {
         pw.println ("[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (int i=0; i<contours.length; i++) {
            writeContour (pw, contours[i]);
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }

      public void writeContour (PrintWriter pw, double[] contour)
         throws IOException {

         pw.print ("[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (int i = 0; i < contour.length/3; i++) {
            pw.println (contour[3*i]+" "+contour[3*i+1]+" "+contour[3*i+2]);
         }
         if (contour.length%3 != 0) {
            pw.println (OPEN);
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }

   }

   private int[][] createTestRegions (
      ContactInfo cinfo, ArrayList<PenetrationRegion> regions) {
      
      int[][] regionsCheck = new int[2*regions.size()][];
      int k = 0;
      for (PenetrationRegion r : regions) {
         int[] contours = new int[r.myContours.size()];
         int[] vertices = new int[r.numVertices()];

         int i = 0;
         for (IntersectionContour c : r.myContours) {
            contours[i++] = cinfo.myContours.indexOf (c);
         }
         i = 0;
         for (Vertex3d v : r.myVertices) {
            vertices[i++] = v.getIndex();
         }
         regionsCheck[k++] = contours;
         regionsCheck[k++] = vertices;
      }
      return regionsCheck;
   }

   private double[] createTestRegionsAreas (
      ContactInfo cinfo, ArrayList<PenetrationRegion> regions,
      PolygonalMesh mesh, boolean clockwise) {

      double[] regionsAreasCheck = new double[regions.size()];
      int k = 0;
      //System.out.println ("Begin createTestRegionsAreas");
      for (PenetrationRegion r : regions) {
         regionsAreasCheck[k++] = r.getArea(); // computeArea(mesh, clockwise);
      }
      //System.out.println ("End createTestRegionsAreas");
      return regionsAreasCheck;
   }

   private ContactInfo createTestInfo (
      TestInfo testInfo, PolygonalMesh mesh0, PolygonalMesh mesh1, double tol) {
      
//      SurfaceMeshCollider collider = new SurfaceMeshCollider();
//      ContactInfo cinfo = collider.getContacts (mesh0, mesh1);
      SurfaceMeshIntersector smi = createIntersector();
      ContactInfo cinfo = smi.findContoursAndRegions (mesh0, mesh1);

      if (cinfo != null) {
         int numContours =
            (cinfo.myContours != null ? cinfo.myContours.size() : 0);
         testInfo.contoursCheck = new double[numContours][];
         for (int k=0; k<numContours; k++) {
            IntersectionContour c = cinfo.myContours.get(k);
            ArrayList<? extends Point3d> corners = c.getCornerPoints(tol);
            double[] vals = new double[3*corners.size()];
            for (int i=0; i<corners.size(); i++) {
               Point3d p = corners.get(i);
               vals[3*i  ] = p.x;
               vals[3*i+1] = p.y;
               vals[3*i+2] = p.z;
            }
            testInfo.contoursCheck[k] = vals;
         }

         testInfo.regions0Check = createTestRegions (cinfo, cinfo.myRegions0);
         testInfo.regions1Check = createTestRegions (cinfo, cinfo.myRegions1);
         testInfo.regions0AreaCheck =
            createTestRegionsAreas (cinfo, cinfo.myRegions0, mesh0, false);
         testInfo.regions1AreaCheck = 
            createTestRegionsAreas (cinfo, cinfo.myRegions1, mesh1, true);
         //testInfo.regions0AreaCheck = null;
         //testInfo.regions1AreaCheck = null;
      }
      else {
         testInfo.contoursCheck = new double[0][];
         testInfo.regions0Check = new int[0][];
         testInfo.regions1Check = new int[0][];
         testInfo.regions0AreaCheck = new double[0];
         testInfo.regions1AreaCheck = new double[0];
      }
      
      return cinfo;
   }

   private boolean checkContour (
      IntersectionContour contour, double[] vals, RigidTransform3d X) {

      boolean checkIsClosed = (vals.length%3 == 0);
      if (checkIsClosed != contour.isClosed()) {
         return false;
      }
      
      Point3d[] pnts;
      if (contour.isClosed()) {
         pnts = new Point3d[vals.length/3+1];
      }
      else {
         pnts = new Point3d[vals.length/3];
      }
      
      if (debug) {
         System.out.println ("points");
      }
      int k;
      for (k=0; k<vals.length/3; k++) {
         pnts[k] = new Point3d (vals[3*k], vals[3*k+1], vals[3*k+2]);
         if (X != null) {
            pnts[k].transform (X);
         }
         if (debug) {
            System.out.println ("  " + pnts[k].toString ("%18.13f"));
         }
      }

      if (debug) {
         System.out.println ("contour");
         for (int i=0; i<contour.size(); i++) {
            IntersectionPoint p = contour.get(i);
            System.out.println ("  " + p.toString ("%18.13f"));
         }
      }

      // get length for epsilon
      double tol = EPS*contour.computeLength();
      int j0 = 0;
      if (contour.isClosed()) {
         // repeat end point so we don't have to loop
         pnts[pnts.length-1] = pnts[0]; 
         // find contour point equal to pnts[0]
         while (j0 < contour.size()) {
            if (contour.get(j0).epsilonEquals (pnts[0], tol)) {
               break;
            }
            j0++;
         }
         if (j0 == contour.size()) {
            if (debug) System.out.println ("j0 not found");
            return false;
         }
      }
      int j = j0;
      k = 0;
      Point3d ps = new Point3d();
      double sprev = 0;
      do {
         if (debug) System.out.println ("k=" + k + " j=" + j);

         double s = LineSegment.projectionParameter (
            pnts[k], pnts[k+1], contour.getWrapped(j));

         double segLen = pnts[k+1].distance(pnts[k]);
         // normalize s tolerance to the segment length
         double stol = tol/segLen;

         if (debug) System.out.println ("s=" + s);
         
         ps.combine (1-s, pnts[k], s, pnts[k+1]);
         if (!ps.epsilonEquals (contour.getWrapped(j), tol)) {
            if (debug) System.out.println ("strayed off path");
            return false;
         }
         if (s-sprev < -stol) {
            if (debug) System.out.println ("going backwards");
            return false;
         }
         else if (s > 1+stol) {
            if (debug) System.out.println ("overshoot");
            return false;
         }
         if (k == pnts.length-1 && Math.abs(s) > stol) {
            if (debug) System.out.println ("should be at the end");
            return false;
         }
         if (Math.abs(s-1) < stol && k < pnts.length-2) {
            // have hit the next pnt, time to advance k
            sprev = 0;
            k++;
         }
         else {
            sprev = s;
         }
         j = (j+1)%contour.size();
      }
      while (j != j0);
      return true;
   }

   private void printVertices (String msg, HashSet<Integer> vidxs) {
      System.out.print (msg + " ");
      for (Integer idx : vidxs) {
         System.out.print (idx+" ");
      }
      System.out.println ("");
   }

   private void printVertices (String msg, int[] vidxs) {
      System.out.print (msg + " ");
      for (int i=0; i<vidxs.length; i++) {
         System.out.print (vidxs[i]+" ");
      }
      System.out.println ("");
   }

   private int[] getPenetratingIndices (
      ArrayList<PenetratingPoint> points) {
      ArrayList<Integer> list = new ArrayList<Integer>();
      for (int i=0; i<points.size(); i++) {
         list.add (points.get(i).vertex.getIndex());
      }
      return ArraySupport.toIntArray (list);
   }

   private boolean indicesEqual (int[] indices, int[] check) {
      if (indices.length != check.length) {
         return false;
      }
      HashSet<Integer> remainingIndices = new HashSet<Integer>();
      for (int k=0; k<indices.length; k++) {
         remainingIndices.add (indices[k]);
      }
      if (remainingIndices.size() != check.length) {
         return false;
      }
      for (int i=0; i<check.length; i++) {
         if (remainingIndices.contains (check[i])) {
            remainingIndices.remove (check[i]);
         }
         else {
            return false;
         }
      }
      return true;         
   }

   public static int[] getVertexIndices (PenetrationRegion region) {
      ArrayList<Integer> list = new ArrayList<Integer>();
      for (Vertex3d v : region.myVertices) {
         list.add (v.getIndex());
      }
      int[] indices = new int[list.size()];
      for (int k=0; k<list.size(); k++) {
         indices[k] = list.get(k);
      }
      return indices;
   }

   private int[] getContourIndices (
      PenetrationRegion region, ArrayList<IntersectionContour> contours) {
      ArrayList<Integer> list = new ArrayList<Integer>();
      for (IntersectionContour c : region.myContours) {
         list.add (contours.indexOf (c));
      }
      int[] indices = new int[list.size()];
      for (int k=0; k<list.size(); k++) {
         indices[k] = list.get(k);
      }
      return indices;
   }

   public boolean regionEqualsCheck (
      PenetrationRegion region, int[] checkContours, int[] checkVertices,
      ArrayList<IntersectionContour> contours) {

      if (!indicesEqual (getContourIndices(region, contours), checkContours)) {
         return false;
      }
      if (checkVertices != null &&
          !indicesEqual (getVertexIndices(region), checkVertices)) {
         return false;
      }
      return true;
   }

//   LinkedHashSet<Face> findInsideFaces (
//      PenetrationRegion region, SurfaceMeshIntersector smi, boolean inside) {
//
//      LinkedHashMap<Face,FaceAreaCalculator> calcMap =
//         new LinkedHashMap<Face,FaceAreaCalculator>();
//
//      double atol = smi.myAreaTol;
//      double ptol = smi.myPositionTol;
//      boolean clockwise = region.myClockwise;
//
//      for (Vertex3d v : region.myVertices) {
//         Iterator<HalfEdge> it = v.getIncidentHalfEdges();
//         while (it.hasNext()) {
//            HalfEdge he = it.next();
//            smi.getFaceAreaCalculator (he.getFace(), calcMap);
//            if (he.opposite != null) {
//               smi.getFaceAreaCalculator (he.opposite.getFace(), calcMap);
//            }
//         }
//      }
//
//      PolygonalMesh mesh = region.myMesh;
//      for (IntersectionContour c : region.myContours) {
//         FaceAreaCalculator fcalc = null;
//         HashSet<Face> contourFaces = new HashSet<Face>();
//         HashSet<Face> zeroAreaFaces = new HashSet<Face>();
//         int kenter = c.getFirstFaceEntryIndex (mesh, null);
//         if (kenter == -1) {
//            Face face = c.findSegmentFace (0, mesh);
//            fcalc = smi.getFaceAreaCalculator (face, calcMap);
//            fcalc.outsideArea = 0;
//            for (int k=0; k<c.size(); k++) {
//               fcalc.addOutsideArea (
//                  c.get(k), c.getWrapped(k+1), clockwise);
//            }
//            fcalc.clearOutsideArea (mesh, 0, 0);
//         }
//         else {
//            int k = kenter;
//            Face lastFace = null;            
//            IntersectionPoint pentry = null;
//            for (int i=0; i<c.size(); i++) {
//               Face face = c.findSegmentFace (k, mesh);
//               if (face != null) {
//                  IntersectionPoint pk = c.get(k);
//                  IntersectionPoint pn = c.getWrapped(k+1);
//                  if (face != lastFace) {
//                     // entering face
//                     fcalc = smi.getFaceAreaCalculator (face, calcMap);
//                     pentry = pk;
//                     if (fcalc.contour != c) {
//                        fcalc.clearOutsideArea (
//                           mesh, atol, ptol);
//                        fcalc.setContour (c, c.get(k));
//                     }
//                  }
//                  fcalc.addOutsideArea (pk, pn, clockwise);
//                  Face nextFace = c.findSegmentFace (k+1, mesh);
//                  if (nextFace != face) {
//                     // exiting face
//                     // compute area contribution for face boundary from exit to
//                     // entry
//                     fcalc.addBoundaryArea (pn, pentry, clockwise);
//                  }                 
//               }
//               lastFace = face;
//               k = c.getWrappedIndex (k+1);
//            }
//         }
//      }
//      LinkedHashSet<Face> faces = new LinkedHashSet<Face>();
//      for (FaceAreaCalculator fcalc : calcMap.values()) {
//         fcalc.clearOutsideArea (mesh, atol, ptol);
//         if (!smi.removeZeroAreaFaces) {
//            atol = 0;
//         }
//         if (fcalc.insideArea >= atol) {
//            faces.add (fcalc.face);
//         }
//      }
//      return faces;
//   }

   public void checkRegions (
      String msg, int[][] regionsCheck, double[] areasCheck,
      ArrayList<PenetrationRegion> regions, 
      ArrayList<IntersectionContour> contours,
      PolygonalMesh mesh, SurfaceMeshIntersector smi, boolean clockwise) {

      if (regionsCheck.length%2 != 0) {
         throw new TestException (
            "Regions check has odd number of entries");
      }
      int numRegionsCheck = regionsCheck.length/2;
      if (numRegionsCheck != regions.size()) {
         if (printFailInfo) {
            System.out.println ("regions:");
            for (PenetrationRegion r : regions) {
               System.out.println (" "+r.toString (contours));
            }
         }
         throw new TestException (
            msg + "Expected "+numRegionsCheck+", got " + regions.size());
      }
      if (areasCheck != null && numRegionsCheck != areasCheck.length) {
         throw new TestException (
            msg + "numRegions=" + numRegionsCheck +
            ", numAreaChecks=" + areasCheck.length);
      }
      
      HashSet<PenetrationRegion> regionsToCheck =
         new HashSet<PenetrationRegion>();
      regionsToCheck.addAll (regions);

      double areaTol = 10000*mesh.computeArea()*DOUBLE_PREC;

      for (int k=0; k<numRegionsCheck; k++) {
         int[] contourCheck = regionsCheck[k*2];
         int[] vertexCheck = regionsCheck[k*2+1];
         PenetrationRegion found = null;
         for (PenetrationRegion r : regionsToCheck) {
            if (regionEqualsCheck (
                   r, contourCheck, vertexCheck, contours)) {
               found = r;
               break;
            }
         }
         
         if (found != null) {
            regionsToCheck.remove (found);
//            if (false) {
//            HashSet<Face> insideFaces =
//               findInsideFaces (found, smi, /*inside=*/true);
//
//            if (!insideFaces.equals (found.myFaces)) {
//               System.out.println (
//                  "InsideFaces: " +
//                  ArraySupport.toString(getFaceIndices(found.myFaces)));
//               System.out.println (
//                  "Found insideFaces: " +
//                  ArraySupport.toString(getFaceIndices(insideFaces)));
//               throw new TestException (
//                  msg + "region " +k+", inconsistent set of inside faces");
//            }
//            }
            if (areasCheck != null) {
               double area = found.getArea(); // computeArea (mesh, clockwise);
               double check = areasCheck[k];
               if (Math.abs(area-check) > areaTol) {
                  throw new TestException (
                     msg + "Area for "+k+" is "+area+", expected "+check+
                     ", tol=" + areaTol);
               }
            }
         }
         else {
            String vertexStr =
               (vertexCheck != null ?
                ArraySupport.toString(vertexCheck) : "null");
            throw new TestException (
               msg + "Region not found: contours=[ " +
               ArraySupport.toString (contourCheck) +
               " ] vertices=[ " + vertexStr + " ]");
         }
      }
   }

   private void printCheckContour (String name, double[] corners) {
      if (name != null) {
         System.out.println (name);
      }
      int numc = corners.length/3;
      for (int i=0; i<numc; i++) {
         System.out.printf (
            "  %20.16f %20.16f %20.16f\n",
            corners[i*3], corners[i*3+1], corners[i*3+2]);
      }
   }

   public void testContacts (
      PolygonalMesh mesh0, PolygonalMesh mesh1, RandomTransform rand,
      int ntests, int nperturb,
      int[][] regions0Check, int[][] regions1Check, 
      double[] regions0AreaCheck, 
      double[] regions1AreaCheck,
      double[]... contoursCheck) {

      if (nperturb < 0) {
         nperturb = 0;
      }
      for (int i=0; i<1+nperturb; i++) {
         RigidTransform3d TBW = null;
         if (i > 0) {
            TBW = new RigidTransform3d();
            TBW.setRandom();
         }

         if (debugx) {
            if (i==4) {
               SurfaceMeshIntersector.debug2 = true;
               SurfaceMeshIntersector.debugTBW = TBW;
            }
         }
         // if ( regions0AreaCheck != null) {
         //    System.out.print ("regions0Area=");
         //    for (double a : regions0AreaCheck) {
         //       System.out.print (" " + a);
         //    }
         //    System.out.println ("");
         // }
         // if ( regions1AreaCheck != null) {
         //    System.out.print ("regions1Area=");
         //    for (double a : regions1AreaCheck) {
         //       System.out.print (" " + a);
         //    }
         //    System.out.println ("");
         // }

         testContacts (
            mesh0, mesh1, TBW, null, regions0Check, regions1Check,
            regions0AreaCheck, regions1AreaCheck, contoursCheck);
         SurfaceMeshIntersector.debug2 = false;

         if (rand != null) {
            for (int k=0; k<ntests; k++) {
               testContacts (
                  mesh0, mesh1, TBW, rand.nextTransform(),
                  regions0Check, regions1Check,
                  regions0AreaCheck, regions1AreaCheck,
                  contoursCheck);
            }
         }
      }
   }

   String myTestFailFileName = null;
   //String problemFileName = null;

   public void testContacts (
      PolygonalMesh mesh0, PolygonalMesh mesh1,
      RigidTransform3d TBW, RigidTransform3d T10,
      int[][] regions0Check, int[][] regions1Check, 
      double[] regions0AreaCheck, 
      double[] regions1AreaCheck,
      double[]... contoursCheck) {

      TestInfo tinfo = new TestInfo();

      tinfo.TBW = TBW;
      tinfo.T10 = T10;
      tinfo.regions0Check = regions0Check;
      tinfo.regions1Check = regions1Check;
      tinfo.regions0AreaCheck = regions0AreaCheck;
      tinfo.regions1AreaCheck = regions1AreaCheck;
      tinfo.contoursCheck = contoursCheck;
      testContacts (mesh0, mesh1, tinfo);
   }      

   private double checkCSGMeshArea (
      String csgOp, PolygonalMesh mesh, SurfaceMeshIntersector smi,
      List<PenetrationRegion> regions0, List<PenetrationRegion> regions1) {

      // make sure area matches
      double area = 0;
      for (PenetrationRegion r : regions0) {
         area += r.getArea();
      }
      for (PenetrationRegion r : regions1) {
         area += r.getArea();
      }
      double meshArea = mesh.computeArea();
      double atol = 1e-12*area;
      if (Math.abs(meshArea-area) > atol) {
         throw new TestException (
            csgOp + " mesh area=" + meshArea +
            ", region area=" + area + " tol=" + atol);
      }
      //System.out.println ("aerror=" + Math.abs(meshArea-area)/meshArea);
      return meshArea;
   }

   // private void checkMeshTopology (
   //    String csgStr, PolygonalMesh mesh, boolean checkClosed) {
   //    if (!mesh.isManifold()) {
   //       numNonManifoldMeshes++;
   //       if (true) {
   //          mesh.printDegenerateFaces();
   //          mesh.printDegeneracies(PolygonalMesh.ALL_DEGENERACIES);
   //          throw new TestException (csgStr+ " mesh not manifold");
   //       }
   //    }
   //    else {
   //       numManifoldMeshes++;
   //    }
   //    if (checkClosed) {
   //       if (!mesh.isClosed()) {
   //          numNonClosedMeshes++;
   //          if (true) {
   //             mesh.printDegenerateFaces();
   //             mesh.printDegeneracies(PolygonalMesh.ALL_DEGENERACIES);
   //             throw new TestException (csgStr + " mesh not closed");
   //          }
   //       }
   //       else {
   //          numClosedMeshes++;
   //       }
   //    }
   // }

   private void checkMeshTopology (
      String csgStr, PolygonalMesh mesh, int code) {

      if ((code & NON_MANIFOLD_EDGES) != 0) {
         numNonManifoldEdgeChecks++;
         if (mesh.hasNonManifoldEdges()) {
            numNonManifoldEdges++;
            if (true) {
               mesh.printDegenerateFaces();
               mesh.printDegeneracies(ALL_DEGENERACIES);
               throw new TestException (csgStr+" mesh has non-manifold edges");
            }
         }
      }
      if ((code & NON_MANIFOLD_VERTICES) != 0) {
         numNonManifoldVertexChecks++;
         if (mesh.hasNonManifoldVertices()) {
            numNonManifoldVertices++;
            if (true) {
               mesh.printDegenerateFaces();
               mesh.printDegeneracies(ALL_DEGENERACIES);
               throw new TestException (csgStr+" mesh has non-manifold vertices");
            }
         }
      }
      if ((code & OPEN_EDGES) != 0) {
         numOpenEdgeChecks++;
         if (mesh.hasOpenEdges()) {
            numOpenEdges++;
            if (true) {
               mesh.printDegenerateFaces();
               mesh.printDegeneracies(ALL_DEGENERACIES);
               throw new TestException (csgStr+" mesh has open edges");
            }
         }
      }
      if ((code & ISOLATED_VERTICES) != 0) {
         numIsolatedVertexChecks++;
         if (mesh.hasIsolatedVertices()) {
            numIsolatedVertices++;
            if (true) {
               mesh.printDegenerateFaces();
               mesh.printDegeneracies(ALL_DEGENERACIES);
               throw new TestException (csgStr+" mesh has isolated vertices");
            }
         }
      }
   }

   boolean contoursEqual (
      ArrayList<IntersectionContour> contours,
      double[][] contoursCheck) {
      
      if (contours.size() != contoursCheck.length) {
         return false;
      }
      HashSet<IntersectionContour> foundContours =
         new HashSet<IntersectionContour>();
      for (double[] ccheck : contoursCheck) {
         IntersectionContour found = null;
         for (IntersectionContour c : contours) {
            if (!foundContours.contains (c)) {
               if (checkContour (c, ccheck, RigidTransform3d.IDENTITY)) {
                  found = c;
                  break;
               }
            }
         }
         if (found != null) {
            foundContours.add(found);
         }
         else {
            return false;
         }
      }
      return true;
   }      

   protected SurfaceMeshIntersector createIntersector() {
      SurfaceMeshIntersector smi = new SurfaceMeshIntersector();
      smi.setSilent (getSilent());
      return smi;
   }
   
   public void testContacts (
      PolygonalMesh mesh0, PolygonalMesh mesh1, TestInfo tinfo) {

//      collider.meshIntersector.myCheckCoincidentOrdering =
//         checkCoincidentOrdering;

      RigidTransform3d TMW0Orig = null;
      RigidTransform3d TMW1Orig = null;
      RigidTransform3d TMW0 = new RigidTransform3d();
      RigidTransform3d TMW1 = new RigidTransform3d();
      RigidTransform3d T1W = null;
      //tinfo.TBW = null;
      if (tinfo.TBW != null) {
         TMW0Orig = new RigidTransform3d (mesh0.getMeshToWorld());
         TMW0 = new RigidTransform3d();
         TMW0.mul (tinfo.TBW, TMW0Orig);
         mesh0.setMeshToWorld (TMW0);
      }
      
      if (tinfo.TBW != null || tinfo.T10 != null) {
         TMW1Orig = new RigidTransform3d (mesh1.getMeshToWorld());
         TMW1 = new RigidTransform3d();
         T1W = new RigidTransform3d();
         if (tinfo.TBW != null) {
            T1W.set (tinfo.TBW);
         }
         if (tinfo.T10 != null) {
            T1W.mul (T1W, tinfo.T10);
         }
         TMW1.mul (T1W, TMW1Orig);
         mesh1.setMeshToWorld (TMW1);
      }

      // System.out.println (
      //    "vertices0=[ "+
      //    ArraySupport.toString (getPenetratingIndices (cinfo.points0))+"]");

      PolygonalMesh imesh = null;
      PolygonalMesh umesh = null;
      PolygonalMesh dmesh = null;
      ContactInfo cinfo = null;
      ContactInfo uinfo = null;
      ContactInfo dinfo = null;
      SurfaceMeshIntersector smi = createIntersector();
      try {
         ArrayList<IntersectionContour> contours = 
            smi.findContours (mesh0, mesh1);

         if (contours.size() > 0 && checkIntersectionMesh) {
            cinfo = smi.findRegions (RegionType.INSIDE, RegionType.INSIDE);
            imesh = smi.createCSGMesh (cinfo);
            checkMeshTopology ("Intersection", imesh, checkIntersectionTopology);
            if (checkUnionMesh) {
               uinfo = smi.findRegions (RegionType.OUTSIDE, RegionType.OUTSIDE);
               umesh = smi.createCSGMesh (uinfo);
               checkMeshTopology ("Union", umesh, checkUnionTopology);
            }
            if (checkDifferenceMesh) {
               dinfo = smi.findRegions (RegionType.OUTSIDE, RegionType.INSIDE);
               dmesh = smi.createCSGMesh (dinfo);
               checkMeshTopology ("Difference", dmesh, checkDifferenceTopology);
            }
            
            if (smi.triangulationError) {
               throw new TestException (
                  "Triangulation error discovered");
            }
            if (smi.coincidentError) {
               throw new TestException (
                  "Coincident error discovered");
            }
         }
         else {
            cinfo = smi.findContoursAndRegions (
               mesh0, RegionType.INSIDE, mesh1, RegionType.INSIDE);
         }
         if (cinfo.numContours() == 0) {
            cinfo = null;
         }
         //ContactInfo cinfo = collider.getContacts (mesh0, mesh1);
         smi.traceContourDebug = false;
         if (cinfo == null) {
            if (tinfo.contoursCheck.length != 0) {
               throw new TestException (
                  "No collision detected when one was expected");
            }
         }
         else {
            for (IntersectionContour c : cinfo.getContours()) {
               if (c.isClockwise (mesh0, mesh1)) {
                  throw new TestException (
                     "Some contours not counter clockwise with respect to mesh0");
               }
            }
            // check if the contours are correct
            if (tinfo.contoursCheck.length != cinfo.myContours.size()) {
               throw new TestException (
                  "Got " + cinfo.myContours.size() + " contours, expected " +
                  tinfo.contoursCheck.length);
            }
            ArrayList<IntersectionContour> foundContours =
               new ArrayList<IntersectionContour>();
            int chki = 0;
            for (double[] ccheck : tinfo.contoursCheck) {
               IntersectionContour found = null;
               for (IntersectionContour c : cinfo.myContours) {
                  if (!foundContours.contains (c)) {
                     if (checkContour (c, ccheck, T1W)) {
                        found = c;
                        break;
                     }
                  }
               }
               if (found != null) {
                  if (printContourIndexMapping) {
                     System.out.println (
                        "contour "+cinfo.myContours.indexOf(found)+
                        " = check "+chki);
                  }
                  foundContours.add(found);
               }
               else {
                  if (printFailInfo) {
                     printCheckContour ("checkContour", ccheck);
                     System.out.println ("contours:" );
                     RigidTransform3d T = null;
                     if (T1W != null) {
                        T = new RigidTransform3d();
                        T.invert (T1W);
                     }
                     for (int k=0; k<cinfo.myContours.size(); k++) {
                        IntersectionContour c = cinfo.myContours.get(k);
                        c.printCornerPoints (
                           "contour "+k+" closed="+c.isClosed(), "%20.16f", T);
                        System.out.println ("full contour:");
                        for (IntersectionPoint p : c) {
                           System.out.println (
                              smi.toString (p, T));
                        }
                     }
                     // System.out.println ("contour 2 in detail:");
                     // IntersectionContour contour = cinfo.myContours.get(2);
                     // for (IntersectionPoint p : contour) {
                     //    System.out.println (
                     //       smi.toString (p, T));
                     // }
                  }
                  throw new TestException ("Contour not found");
               }
               chki++;
            }
            // System.out.println ("regions0 areas:");
            // for (PenetrationRegion r : cinfo.regions0) {
            //    System.out.printf ("  %12.8f\n", r.computeArea (mesh0, false));
            // }
            // System.out.println ("regions1 areas:");
            // for (PenetrationRegion r : cinfo.regions1) {
            //    System.out.printf ("  %12.8f\n", r.computeArea (mesh1, true));
            // }
            if (tinfo.regions0Check != null) {
               checkRegions (
                  "Regions0: ", tinfo.regions0Check, tinfo.regions0AreaCheck,
                  cinfo.myRegions0, foundContours, mesh0, smi, false);
            }
            if (tinfo.regions1Check != null) {
               checkRegions (
                  "Regions1: ", tinfo.regions1Check, tinfo.regions1AreaCheck,
                  cinfo.myRegions1, foundContours, mesh1, smi, true);
            }
            if (imesh != null) {
               double iarea = checkCSGMeshArea (
                  "Intersection", imesh, smi, cinfo.myRegions0, cinfo.myRegions1);
               if (umesh != null) {
                  double uarea = checkCSGMeshArea (
                     "Union", umesh, smi, uinfo.myRegions0, uinfo.myRegions1);
                  double meshArea = 0;
                  double combinedArea = 0;
                  if (cinfo.myRegions0.size() > 0) {
                     meshArea += mesh0.computeArea();
                  }
                  if (cinfo.myRegions1.size() > 0) {
                     meshArea += mesh1.computeArea();
                  }
                  double atol = 1e-12*meshArea;
                  if (Math.abs(iarea+uarea-meshArea) > atol) {
                     System.out.println ("iarea=" + iarea);
                     System.out.println ("uarea=" + uarea);
                     throw new TestException (
                        "Combined union-intersection area "+(iarea+uarea)+
                        " != combined mesh area " + meshArea +
                        " atol=" + atol);
                  }
               }
               if (dmesh != null) {
                  double darea = checkCSGMeshArea (
                     "Difference", dmesh, smi, 
                     dinfo.myRegions0, dinfo.myRegions1);
               }
            }
         }
         
         if (TMW0Orig != null) {
            mesh0.setMeshToWorld(TMW0Orig);
         }
         if (TMW1Orig != null) {
            mesh1.setMeshToWorld(TMW1Orig);
         }
         passCount++;
      }
      catch (Exception te) {

         failCount++;      
         System.out.println ("TMW0=\n" + mesh0.getMeshToWorld());
         System.out.println ("TMW1=\n" + mesh1.getMeshToWorld());
         System.out.printf ("randomSeed=%x\n", myRandomSeed);
         if (TMW0Orig != null) {
            mesh0.setMeshToWorld(TMW0Orig);
         }
         if (TMW1Orig != null) {
            mesh1.setMeshToWorld(TMW1Orig);
         }
         if (abortOnFail || !(te instanceof TestException)) {
            if (myTestFailFileName != null) {
               try {
                  System.out.println ("mesh0 numv=" + mesh0.numVertices());
                  writeProblem (myTestFailFileName, mesh0, mesh1, tinfo);
                  System.out.println (
                     "Failed test written to " + myTestFailFileName);
                  if (imesh != null) {
                     imesh.write (
                        new File("imesh.obj"), "%20.12f", true);
                     System.out.println (
                        "Intersection mesh written to imesh.obj");
                  }
                  if (umesh != null) {
                     umesh.write (
                        new File("umesh.obj"), "%20.12f", true);
                     System.out.println (
                        "Union mesh written to umesh.obj");
                  }
                  if (dmesh != null) {
                     dmesh.write (
                        new File("dmesh.obj"), "%20.12f", true);
                     System.out.println (
                        "Difference mesh written to dmesh.obj");
                  }
               }
               catch (Exception e) {
                  e.printStackTrace(); 
               }
            }
            throw te;
         }
      }

      synchronized (this) {
         /* synchronized in case an observer process wants to look at snap
          * shots of these objects.
          */
         if (mySaveTestResults) {
            myLastContactInfo = cinfo;
            myLastMesh0 = mesh0;
            myLastMesh1 = mesh1;
            myLastMeshToWorld0 = new RigidTransform3d (TMW0);
            myLastMeshToWorld1 = new RigidTransform3d (TMW1);
            myLastImesh = imesh;
            myLastUmesh = umesh;
         }
      }
      
      // if (problemFileName != null) {
      //    try {
      //       writeProblem (problemFileName, mesh0, mesh1, tinfo);
      //       System.out.println ("Failed test written to " + problemFileName);
      //    }
      //    catch (Exception e) {
      //       e.printStackTrace(); 
      //    }
      //    problemFileName = null;
      // }
   }

   public void specialTests() {
   }

   private void singleFaceTestOuterInner (
      PolygonalMesh outer, PolygonalMesh inner, RigidTransform3d TBW) {
      
      testContacts (
         outer, inner, TBW, RigidTransform3d.IDENTITY,
         new int[][] {
            new int[] { 0 },
            null,
            new int[] { 1 },
            null,
         },
         new int[][] {
            new int[] { 0, 1 },
            null 
         },
         new double[] {2.0, 2.0},
         new double[] {2*Math.sqrt(5) + 2},
         new double[][] {
            new double[] {
               -1.0000,  1.0000, -0.5000,
               1.0000,  1.0000, -0.5000,
               0.0000, -1.0000, -0.5000,
            },
            new double[] {
               0.0000, -1.0000,  0.5000,
               1.0000,  1.0000,  0.5000,
               -1.0000,  1.0000,  0.5000,
            }});
   }

   private void singleFaceTestInnerOuter (
      PolygonalMesh inner, PolygonalMesh outer, RigidTransform3d TBW) {
      
      testContacts (
         inner, outer, TBW, RigidTransform3d.IDENTITY,
         new int[][] {
            new int[] { 0, 1 },
            null 
         },
         new int[][] {
            new int[] { 0 },
            null,
            new int[] { 1 },
            null,
         },
         new double[] {2*Math.sqrt(5) + 2},
         new double[] {2.0, 2.0},
         new double[][] {
            new double[] {
               -1.0000,  1.0000, -0.5000,
               0.0000, -1.0000, -0.5000,
               1.0000,  1.0000, -0.5000,
            },
            new double[] {
               0.0000, -1.0000,  0.5000,
               -1.0000,  1.0000,  0.5000,
               1.0000,  1.0000,  0.5000,
            }});
   }

   /**
    * Special tests that involve penetration regions consisting of a single
    * face that contacts the contour at only the edges or vertices.
    */
   public void singleFaceTests() {

      if (!mySilentP) {
         System.out.println ("Single face tests:");
      }

      String denseOuterStr = new String(
         "v  2  3  0.5\n" + 
         "v  0  3  0.5\n" + 
         "v -2  3  0.5\n" + 
         "v -2 -1  0.5\n" + 
         "v  0 -3  0.5\n" + 
         "v  2 -1  0.5\n" + 
         "v  1  1  0.5\n" + 
         "v -1  1  0.5\n" + 
         "v  0 -1  0.5\n" + 
         "v  2  3 -0.5\n" + 
         "v  0  3 -0.5\n" + 
         "v -2  3 -0.5\n" + 
         "v -2 -1 -0.5\n" + 
         "v  0 -3 -0.5\n" + 
         "v  2 -1 -0.5\n" + 
         "v  1  1 -0.5\n" + 
         "v -1  1 -0.5\n" + 
         "v  0 -1 -0.5\n" + 

         "f 0 1 6\n" +
         "f 0 6 5\n" +
         "f 1 2 7\n" +
         "f 2 3 7\n" +
         "f 5 8 4\n" +
         "f 8 3 4\n" +
         "f 1 7 6\n" +
         "f 6 7 8\n" +
         "f 7 3 8\n" +
         "f 6 8 5\n" +

         "f 1 0 9\n" +
         "f 1 9 10\n" +
         "f 2 1 10\n" +
         "f 2 10 11\n" +
         "f 3 2 11\n" +
         "f 3 11 12\n" +
         "f 4 3 12\n" +
         "f 4 12 13\n" +
         "f 5 4 13\n" +
         "f 5 13 14\n" +
         "f 0 5 14\n" +
         "f 0 14 9\n" +
               
         "f 10 9 15\n" +
         "f 15 9 14\n" +
         "f 12 11 16\n" +
         "f 16 11 10\n" +
         "f 10 15 16\n" +
         "f 16 17 12\n" +
         "f 16 15 17\n" +
         "f 15 14 17\n" +
         "f 12 17 13\n" +
         "f 17 14 13\n");

      PolygonalMesh denseOuter =
         WavefrontReader.readFromString (denseOuterStr, true);

      String sparseOuterStr = new String(
         "v  2  3  0.5\n" + 
         "v  0  3  0.5\n" + 
         "v -2  3  0.5\n" + 
         "v -2 -1  0.5\n" + 
         "v  0 -3  0.5\n" + 
         "v  2 -1  0.5\n" + 
         "v  2  3 -0.5\n" + 
         "v  0  3 -0.5\n" + 
         "v -2  3 -0.5\n" + 
         "v -2 -1 -0.5\n" + 
         "v  0 -3 -0.5\n" + 
         "v  2 -1 -0.5\n" + 

         "f 0 1 5\n" +
         "f 1 2 3\n" +
         "f 5 3 4\n" +
         "f 1 3 5\n" +

         "f 1 0 6\n" +
         "f 1 6 7\n" +
         "f 2 1 7\n" +
         "f 2 7 8\n" +
         "f 3 2 8\n" +
         "f 3 8 9\n" +
         "f 4 3 9\n" +
         "f 4 9 10\n" +
         "f 5 4 10\n" +
         "f 5 10 11\n" +
         "f 0 5 11\n" +
         "f 0 11 6\n" +
               
         "f 7 6 11\n" +
         "f 9 8 7\n" +
         "f 9 11 10\n" +
         "f 7 11 9\n");

      PolygonalMesh sparseOuter = 
           WavefrontReader.readFromString (sparseOuterStr, true);

      String denseInnerStr = new String(
         "v  1  1  1.5\n" + 
         "v -1  1  1.5\n" + 
         "v  0 -1  1.5\n" + 
         "v  1  1  0.5\n" + 
         "v -1  1  0.5\n" + 
         "v  0 -1  0.5\n" + 
         "v  1  1 -0.5\n" + 
         "v -1  1 -0.5\n" + 
         "v  0 -1 -0.5\n" + 
         "v  1  1 -1.5\n" + 
         "v -1  1 -1.5\n" + 
         "v  0 -1 -1.5\n" + 
         "f 0 1 2\n" + 

         "f 2 1 4\n" + 
         "f 2 4 5\n" + 
         "f 0 2 5\n" + 
         "f 0 5 3\n" + 
         "f 1 0 3\n" + 
         "f 1 3 4\n" + 

         "f 5 4 7\n" + 
         "f 5 7 8\n" + 
         "f 3 5 8\n" + 
         "f 3 8 6\n" + 
         "f 4 3 6\n" + 
         "f 4 6 7\n" + 

         "f 8 7 10\n" + 
         "f 8 10 11\n" + 
         "f 6 8 11\n" + 
         "f 6 11 9\n" + 
         "f 7 6 9\n" + 
         "f 7 9 10\n" + 

         "f 9 11 10\n");     

      PolygonalMesh denseInner = 
         WavefrontReader.readFromString (denseInnerStr, true);

      PolygonalMesh sparseInner = MeshFactory.createPrism (
         new double[] { 1.0, 1.0, -1.0, 1.0, 0.0, -1.0 }, 2.0);       

      int numRandomPerturbations = 20;      

      PolygonalMesh[] innerMeshes =
         new PolygonalMesh[] {denseInner, sparseInner};
      PolygonalMesh[] outerMeshes =
         new PolygonalMesh[] {denseOuter, sparseOuter};

      RigidTransform3d TBW = new RigidTransform3d();

      // special case: creates a triangle where all vertices are inside but the
      // contour goes the wrong way so the triangle is still outside the region
      TBW.set (new double[] {
            0.9999999999999997,4.440892098500626E-16,5.082739784612045E-16,1.1102230246251565E-16,
            4.579669976578771E-16,0.9999999999999996,-5.551115123125783E-16,-2.220446049250313E-16,
            5.087076593301987E-16,-5.620504062164855E-16,0.9999999999999994,-1.6653345369377348E-16,
            0.0,0.0,0.0,1.0
         });
      singleFaceTestOuterInner (denseOuter, sparseInner, TBW);            
      //System.out.println ("done");

      if (false) {
         
      for (PolygonalMesh inner : innerMeshes) {
         for (PolygonalMesh outer : outerMeshes) {
      
            for (int kr=0; kr<numRandomPerturbations+1; kr++) {
               TBW.setIdentity();
               if (kr > 0) {
                  RigidTransform3d TRAN = new RigidTransform3d();
                  TRAN.setRandom();
                  TBW.mulInverseLeft (TRAN, TRAN);
                  TBW.mul (TRAN);
                  TBW.mulInverse (TRAN);
               }
               singleFaceTestOuterInner (outer, inner, TBW);
            }
         }
      }

      for (PolygonalMesh inner : innerMeshes) {
         for (PolygonalMesh outer : outerMeshes) {
      
            for (int kr=0; kr<numRandomPerturbations+1; kr++) {
               TBW.setIdentity();
               if (kr > 0) {
                  RigidTransform3d TRAN = new RigidTransform3d();
                  TRAN.setRandom();
                  TBW.mulInverseLeft (TRAN, TRAN);
                  TBW.mul (TRAN);
                  TBW.mulInverse (TRAN);
               }
               singleFaceTestInnerOuter (inner, outer, TBW);
            }
         }
      }
      }
      
   }

   public void simpleBoxTests() {

      if (!mySilentP) {
         System.out.println ("Simple box tests:");
      }

      PolygonalMesh cube = MeshFactory.createBox (1.0, 1.0, 1.0);
      PolygonalMesh box5 =
         MeshFactory.createBox (2.0, 2.0, 1.0, Point3d.ZERO, 5, 5, 1);
      PolygonalMesh tallBox65 = MeshFactory.createBox (0.6, 0.5, 2.0);

      PolygonalMesh tallBox55 = MeshFactory.createBox (0.5, 0.5, 2.0);

      double[][] cubeTallBox55Contours = new double[][] {
         new double[] {
            -0.25, -0.25, 0.5, 
             0.25, -0.25, 0.5, 
             0.25,  0.25, 0.5, 
            -0.25,  0.25, 0.5,
         },
         new double[] {
            -0.25, -0.25, -0.5, 
            -0.25,  0.25, -0.5,
             0.25,  0.25, -0.5, 
             0.25, -0.25, -0.5, 
         }};

      PolygonalMesh cube2 =
         MeshFactory.createBox (1.0, 1.0, 1.0, Point3d.ZERO, 2, 2, 2);

      PolygonalMesh cube4 =
         MeshFactory.createBox (1.0, 1.0, 1.0, Point3d.ZERO, 4, 4, 4);

      RandomTransform randt = new RandomTransform2d (0.5);
         
      testContacts (
         cube, tallBox65, null, 1, 10,
         new int[][] {
            new int[] { 0 },
            new int[] {},
            new int[] { 1 },
            new int[] {},
         },
         new int[][] {
            new int[] { 0, 1 },
            new int[] {},
         },
         new double[] {0.3, 0.3},
         new double[] {2.2},
         new double[] {
            -0.30, -0.25, 0.5, 
             0.30, -0.25, 0.5, 
             0.30,  0.25, 0.5, 
            -0.30,  0.25, 0.5,
         }, 
         new double[] {
            -0.30, -0.25, -0.5, 
            -0.30,  0.25, -0.5,
             0.30,  0.25, -0.5, 
             0.30, -0.25, -0.5, 
         });

      testContacts (
         cube, tallBox55, null, 1, 20,
         new int[][] {
            new int[] { 0 },
            new int[] {},
            new int[] { 1 },
            new int[] {},
         },
         new int[][] {
            new int[] { 0, 1 },
            new int[] {},
         },
         new double[] {0.25, 0.25},
         new double[] {2.0},
         cubeTallBox55Contours);

      //System.out.println ("CUBE2");
      testContacts (
         cube2, tallBox55, null, 1, 0,
         new int[][] {
            new int[] { 0 },
            new int[] { 6 },               
            new int[] { 1 },
            new int[] { 2 },
         },
         new int[][] {
            new int[] { 0, 1 },
            new int[] {},
         },
         new double[] {0.25, 0.25},
         new double[] {2.0},
         cubeTallBox55Contours);
      
      // Hard one: if we slightly perturb, then we can get bad contours - with
      // intermediate back and forth points, because we are dealing with
      // intersections where the edges lie exactly on the faces:

      //System.out.println ("CUBE4");
      testContacts (
         cube4, tallBox55, null, 1, 0,
         new int[][] {
            new int[] { 0 },
            null,
            new int[] { 1 },
            null,
         },
         new int[][] {
            new int[] { 0, 1 },
            new int[] {},
         },
         new double[] {0.25, 0.25},
         new double[] {2.0},
         cubeTallBox55Contours);

      testContacts (
         box5, tallBox65, randt, 3, 1,
         new int[][] {
            new int[] { 0 },
            null,
            new int[] { 1 },
            null,
         },
         new int[][] {
            new int[] { 0, 1 },
            new int[] {},

         },
         new double[] {0.3, 0.3},
         new double[] {2.2},
         new double[] {
            -0.30, -0.25, 0.5, 
             0.30, -0.25, 0.5, 
             0.30,  0.25, 0.5, 
            -0.30,  0.25, 0.5,
         }, 
         new double[] {
            -0.30, -0.25, -0.5, 
            -0.30,  0.25, -0.5,
             0.30,  0.25, -0.5, 
             0.30, -0.25, -0.5, 
         });


      tallBox65.setMeshToWorld (new RigidTransform3d (0, 0, 1.0));

      testContacts (
         cube, tallBox65, null, 1, 0,
         new int[][] {
            new int[] { 0 },
            new int[] {},
         },
         new int[][] {
            new int[] { 0 },
            new int[] { 0, 1, 2, 3 },
         },             
         new double[] { 0.3, },
         new double[] { 1.4 },
         new double[] {
            -0.30, -0.25, 0.5, 
             0.30, -0.25, 0.5, 
             0.30,  0.25, 0.5, 
            -0.30,  0.25, 0.5,
         });

      tallBox65.setMeshToWorld (new RigidTransform3d (0.3, 0, 0));

      testContacts (
         cube, tallBox65, null, 1, 10,
         new int[][] {
            new int[] { 0 },
            new int[] {},

         },
         new int[][] {
            new int[] { 0 },
            new int[] {},
         },
         new double[] { 1.0 },
         new double[] { 1.5 },
         new double[] {
             0.00, -0.25,  0.5, 
             0.50, -0.25,  0.5, 
             0.50, -0.25, -0.5, 
             0.00, -0.25, -0.5, 
             0.00,  0.25, -0.5, 
             0.50,  0.25, -0.5, 
             0.50,  0.25,  0.5, 
             0.00,  0.25,  0.5, 
         });

      tallBox65.setMeshToWorld (new RigidTransform3d (0.3, 0, 1.0));
      testContacts (
         cube, tallBox65, null, 1, 10,
         new int[][] {
            new int[] { 0 },
            new int[] {},

         },
         new int[][] {
            new int[] { 0, },
            new int[] { 0, 1 },
         },
         new double[] { 0.5 },
         new double[] { 1.0 },
         new double[] {
             0.00, -0.25,  0.5, 
             0.50, -0.25,  0.5, 
             0.50, -0.25,  0.0, 
             0.50,  0.25,  0.0, 
             0.50,  0.25,  0.5, 
             0.00,  0.25,  0.5, 
         });

      tallBox65.setMeshToWorld (new RigidTransform3d (0.2, 0, 0.0));
      // is very unstable ... two contour can easily become one
      testContacts (
         cube, tallBox65, null, 1, 0,
         new int[][] {
            new int[] { 0 },
            new int[] {},
            new int[] { 1 },
            new int[] {},
         },
         new int[][] {
            new int[] { 0, 1 },
            new int[] { },
         },
         new double[] { 0.3, 0.3 },
         new double[] { 2.2},
         new double[] { 
          -0.10000000000000,   0.25000000000000,  -0.50000000000000,
           0.50000000000000,   0.25000000000000,  -0.50000000000000,
           0.50000000000000,  -0.25000000000000,  -0.50000000000000,
          -0.10000000000000,  -0.25000000000000,  -0.50000000000000,
         },
         new double[] { 
          -0.10000000000000,  -0.25000000000000,   0.50000000000000,
           0.50000000000000,  -0.25000000000000,   0.50000000000000,
           0.50000000000000,   0.25000000000000,   0.50000000000000,
          -0.10000000000000,   0.25000000000000,   0.50000000000000,
         });

      // no intersection detected if EPS is 0 ...
      myDiamond.setMeshToWorld (new RigidTransform3d (0, 0, 0.5+EPS));
      testContacts (
         cube, myDiamond, null, 1, 0,
         new int[][] {
            new int[] { 0 },
            new int[] {},
         },
         new int[][] {
            new int[] { 0 },
            new int[] { 5 },
         },
         new double[] { 1.0-4*EPS },
         new double[] { (1.0-4*EPS)*Math.sqrt(2) },
         new double[] { 
            -0.50, -0.50, 0.5,
             0.50, -0.50, 0.5,
             0.50,  0.50, 0.5,
            -0.50,  0.50, 0.5,
         });

      PolygonalMesh plate3x3 = MeshFactory.createBox (
            3.0, 3.0, 1.0, Point3d.ZERO, 3, 3, 1);
      PolygonalMesh plate6x6 = MeshFactory.createBox (
            3.0, 3.0, 1.0, Point3d.ZERO, 6, 6, 1);
      PolygonalMesh beam1x5 = MeshFactory.createBox (
            1.0, 1.0, 5.0, Point3d.ZERO, 1, 1, 5);
      PolygonalMesh beam2x10 = MeshFactory.createBox (
            1.0, 1.0, 5.0, Point3d.ZERO, 2, 2, 10);

      PolygonalMesh[] plates = new PolygonalMesh[] {plate3x3,  plate6x6};
      PolygonalMesh[] beams = new PolygonalMesh[] {beam1x5,  beam2x10};

      RigidTransform3d TPW = new RigidTransform3d();
      RigidTransform3d T10 = new RigidTransform3d();

      for (PolygonalMesh plate : plates) {
         for (PolygonalMesh beam : beams) {
            for (int i=-4; i<=4; i++) {
               T10.p.set (0, 0, 0.25*i);
               testContacts (
                  beam, plate, TPW, T10,
                  new int[][] {
                     new int[] { 0, 1 },
                     null,
                  },
                  new int[][] {
                     new int[] { 0 },
                     null,
                     new int[] { 1 },
                     null,
                  },
                  new double[] { 4.0 },
                  new double[] { 1.0, 1.0 },
                  new double[] { 
                     -0.50, -0.50, -0.5,
                     0.50, -0.50, -0.5,
                     0.50,  0.50, -0.5,
                     -0.50,  0.50, -0.5,
                  },
                  new double[] { 
                     -0.50, -0.50, 0.5,
                     -0.50,  0.50, 0.5,
                     0.50,  0.50, 0.5,
                     0.50, -0.50, 0.5,
                  });
            }
         }
      }
   }

   double[][] myPlateSquareTorusContours = new double[][] {
         new double[] { 
          -0.00000000000000,  -0.75000000000000,  -0.25000000000000,
           0.75000000000000,   0.00000000000000,  -0.25000000000000,


           0.00000000000000,   0.75000000000000,  -0.25000000000000,
          -0.75000000000000,   0.00000000000000,  -0.25000000000000,
         },
         new double[] { 
          -0.00000000000000,  -1.25000000000000,   0.25000000000000,
           1.25000000000000,   0.00000000000000,   0.25000000000000,
           0.00000000000000,   1.25000000000000,   0.25000000000000,
          -1.25000000000000,   0.00000000000000,   0.25000000000000,
         },
         new double[] { 
          -1.25000000000000,   0.00000000000000,  -0.25000000000000,
           0.00000000000000,   1.25000000000000,  -0.25000000000000,
           1.25000000000000,   0.00000000000000,  -0.25000000000000,
          -0.00000000000000,  -1.25000000000000,  -0.25000000000000,
         },
         new double[] { 
          -0.75000000000000,   0.00000000000000,   0.25000000000000,
           0.00000000000000,   0.75000000000000,   0.25000000000000,
           0.75000000000000,   0.00000000000000,   0.25000000000000,
          -0.00000000000000,  -0.75000000000000,   0.25000000000000,
         }};

   double[][] myPlateSquareTorusPerpContours = new double[][] {
         new double[] { 
          -0.25000000000000,  -0.00000000000000,  -0.25000000000000,
          -0.75000000000000,  -0.50000000000000,  -0.25000000000000,
          -1.25000000000000,  -0.00000000000000,  -0.25000000000000,
          -0.75000000000000,   0.50000000000000,  -0.25000000000000,
         },
         new double[] { 
          -0.75000000000000,  -0.50000000000000,   0.25000000000000,
          -0.25000000000000,  -0.00000000000000,   0.25000000000000,
          -0.75000000000000,   0.50000000000000,   0.25000000000000,
          -1.25000000000000,   0.00000000000000,   0.25000000000000,
         },
         new double[] { 
           0.75000000000000,  -0.50000000000000,  -0.25000000000000,
           0.25000000000000,  -0.00000000000000,  -0.25000000000000,
           0.75000000000000,   0.50000000000000,  -0.25000000000000,
           1.25000000000000,  -0.00000000000000,  -0.25000000000000,
         },
         new double[] { 
           0.75000000000000,  -0.50000000000000,   0.25000000000000,
           1.25000000000000,   0.00000000000000,   0.25000000000000,
           0.75000000000000,   0.50000000000000,   0.25000000000000,
           0.25000000000000,  -0.00000000000000,   0.25000000000000,
         }};


   private double sqr (double x) {
      return x*x;
   }

   public void torusTests() {  

      if (!mySilentP) {
         System.out.println ("Torus tests:");
      }

      RandomTransform randt = new RandomTransform2d (0.5);

      PolygonalMesh squareTorus = MeshFactory.createTorus (1.0, 0.5, 4, 4);      
      PolygonalMesh plate = MeshFactory.createBox (4.0, 4.0, 0.5);
      PolygonalMesh plate5 = MeshFactory.createBox (
         5.0, 5.0, 0.5, Point3d.ZERO, 5, 5, 1);

      double sqrt2 = Math.sqrt(2);

      double innerSide = 0.75*sqrt2;
      double outerSide = 1.25*sqrt2;
      double topArea = sqr(outerSide) - sqr(innerSide);
      double innerSidex = 0.5*sqrt2;
      double outerSidex = 1.5*sqrt2;

      double scale = Math.sqrt(1.5);

      double innerSideArea = scale*(innerSidex+innerSide);
      double outerSideArea = scale*(outerSidex+outerSide);

      testContacts (
         plate, squareTorus, null, 1, 10,
         new int[][] {
            new int[] { 1, 3 },
            new int[] {},
            new int[] { 0, 2 },
            new int[] {},
         },
         new int[][] {
            new int[] { 0, 3 },
            new int[] { 4, 5, 10, 14},
            new int[] { 1, 2 },
            new int[] { 0, 3, 9, 13},
         },
         new double[] { topArea, topArea },
         new double[] { innerSideArea, outerSideArea },
         myPlateSquareTorusContours);

      testContacts (
         plate5, squareTorus, randt, 50, 4,
         new int[][] {
            new int[] { 1, 3 },
            null,
            new int[] { 0, 2 },
            null, 
         },
         new int[][] {
            new int[] { 0, 3 },
            new int[] { 4, 5, 10, 14},
            new int[] { 1, 2 },
            new int[] { 0, 3, 9, 13},
         },
         new double[] { topArea, topArea },
         new double[] { innerSideArea, outerSideArea },
         myPlateSquareTorusContours);
      
      squareTorus.setMeshToWorld (
         new RigidTransform3d (0, 0, 0, 0, 0, Math.PI/2));

      testContacts (
         plate, squareTorus, null, 1, 10,
         new int[][] {
            new int[] { 0 },
            new int[] {},
            new int[] { 1 },
            new int[] {},
            new int[] { 2 },
            new int[] {},
            new int[] { 3 },
            new int[] {},
         },
         new int[][] {
            new int[] { 0, 1 },
            new int[] { 8, 9, 10, 11 },
            new int[] { 2, 3 },
            new int[] { 0, 1, 4, 6},
         },
         new double[] { 0.5, 0.5, 0.5, 0.5 },
         new double[] { sqrt2*scale, sqrt2*scale },
         myPlateSquareTorusPerpContours);
      
      testContacts (
         plate5, squareTorus, randt, 50, 4,
         new int[][] {
            new int[] { 0 },
            null,
            new int[] { 1 },
            null,
            new int[] { 2 },
            null,
            new int[] { 3 },
            null,
         },
         new int[][] {
            new int[] { 0, 1 },
            new int[] { 8, 9, 10, 11 },
            new int[] { 2, 3 },
            new int[] { 0, 1, 4, 6},
         },
          new double[] { 0.5, 0.5, 0.5, 0.5 },
         new double[] { sqrt2*scale, sqrt2*scale },
         myPlateSquareTorusPerpContours);

   }

   private static final double zh = 5.0;

   double[][] myLegoContours = new double[][] {
      new double[] {-11, -8, zh,  -1, -8, zh,  -1, 7, zh, -11, 7, zh }, // 0
      new double[] {-10, -7, zh, -10,  6, zh,  -2, 6, zh,  -2,-7, zh }, // 1 
      new double[] { -9,  0, zh,  -3,  0, zh,  -3, 5, zh,  -9, 5, zh }, // 2
      new double[] { -8,  1, zh,  -8,  4, zh,  -4, 4, zh,  -4, 1, zh }, // 3
      new double[] { -9, -6, zh,  -3, -6, zh,  -3,-1, zh,  -9,-1, zh }, // 4
      new double[] { -8, -5, zh,  -8, -2, zh,  -4,-2, zh,  -4,-5, zh }, // 5
      new double[] { -7, -4, zh,  -5, -4, zh,  -5,-3, zh,  -7,-3, zh }, // 6

      new double[] {  0,  2, zh,   5,  2, zh,   5, 7, zh,   0, 7, zh }, // 7
      new double[] {  1,  3, zh,   1,  6, zh,   4, 6, zh,   4, 3, zh }, // 8
      new double[] {  2,  4, zh,   3,  4, zh,   3, 5, zh,   2, 5, zh }, // 9

      new double[] {  6,  2, zh,  11,  2, zh,  11, 7, zh,   6, 7, zh }, // 10
      new double[] {  7,  3, zh,   7,  6, zh,  10, 6, zh,  10, 3, zh }, // 11
   };


   int[] myRegions1_0_verts = new int[] {
      321, 97, 735, 909, 452, 567, 616, 244, 245, 506, 507, 566, 617,
      794, 94, 186, 187, 383, 453, 681, 795, 848, 849, 959, 95, 96, 320,
      382, 680, 734, 908, 958, 963, 967, 979, 975, 980, 964, 968, 983, 984,
      987, 988, 972, 976, 971, 537, 706, 766, 929, 129, 130, 345, 346, 538,
      588, 880, 930, 280, 134, 408, 991, 705, 765, 133, 207, 208, 407, 477,
      478, 651, 819, 820, 994, 279, 652, 587, 879, 106, 101, 117, 118, 114,
      100, 105, 122, 126, 109, 110, 121, 125, 113 };

   int[] myRegions1_1_verts = new int[] {
      327, 513, 328, 526, 514, 266, 251, 252, 253, 254, 265, 273, 339, 340,
      389, 390, 460, 471, 517, 522, 525, 529, 532, 256, 257, 261, 262, 269,
      270, 274, 401, 402, 459, 472, 518, 521 };

   int[] myRegions1_2_verts = new int[] {
      624, 637, 623, 629, 625, 626, 628, 633, 645, 646, 641, 642, 634, 638,
      868, 801, 802, 855, 856, 859, 867, 813, 814, 860, 863, 864, 871, 874,
      699, 700, 759, 760, 741, 742, 687, 688 };

   int[] myRegions1_3_verts = new int[] {
      753, 754, 747, 749, 748, 750 };

   int[] myRegions1_4_verts = new int[] {
      351, 362, 361, 417, 413, 418, 414, 422, 425, 428, 352, 421, 157, 140,
      141, 144, 145, 158, 153, 154, 139, 149, 150, 142, 299, 300, 223, 224,
      214, 285, 213, 286 };

   int[] myRegions1_5_verts = new int[] {
      292, 291, 294, 293 };

   int[] myRegions1_6_verts = new int[] {
      315, 377, 168, 169, 181, 437, 182, 240, 163, 164, 166, 173, 229, 230,
      316, 367, 368, 434, 441, 445, 165, 174, 177, 178, 239, 305, 306, 378,
      433, 438, 442, 448 };

   int plateDebug = 0;

   void testPlateLego (
      PolygonalMesh plate, PolygonalMesh lego,
      RigidTransform3d T1, int nperturb) {

      if (nperturb < 0) {
         nperturb = 0;
      }
      for (int i=0; i<1+nperturb; i++) {
         RigidTransform3d TBW = null;
         if (i > 0) {
            TBW = new RigidTransform3d();
            TBW.setRandom();
         }
         testContacts (
            plate, lego, TBW, T1,
            new int[][] {
               new int[] { 0, 1 },
               null,
               new int[] { 2, 3 },
               null,
               new int[] { 4, 5 },
               null,
               new int[] { 6 },
               null,
               new int[] { 7, 8 },
               null,
               new int[] { 9 },
               null,
               new int[] { 10, 11 },
               null,
            },
            new int[][] {
               new int[] { 0, 1 },
               myRegions1_0_verts,
               new int[] { 2, 3 },
               myRegions1_1_verts,
               new int[] { 4, 5 },
               myRegions1_2_verts,
               new int[] { 6 },
               myRegions1_3_verts,
               new int[] { 7, 8 },
               myRegions1_4_verts,            
               new int[] { 9 },
               myRegions1_5_verts,
               new int[] { 10, 11 },
               myRegions1_6_verts,            
            },
            new double[] { 46.0, 18.0, 18.0, 2.0, 16.0, 1.0, 16.0 },
            null,
            myLegoContours);
      }
   }

   public void singleFaceContourTests() {

      if (!mySilentP) {
         System.out.println ("Single face contour tests:");
      }

      PolygonalMesh lego1 = MeshFactory.createSkylineMesh (
         22.0, 16.0, 2.0, 22, 16,
         "1111111111",
         "1        1",
         "1 111111 1",
         "1 1    1 1",
         "1 1 11 1 1",
         "1 1    1 1",
         "1 111111 1",
         "1        1",
         "1 111111 1",
         "1 1    1 1",
         "1 1    1 1 11111 11111",
         "1 1    1 1 1   1 1   1",
         "1 111111 1 1 1 1 1   1",
         "1        1 1   1 1   1",
         "1111111111 11111 11111",
         "");

      PolygonalMesh plate1 = MeshFactory.createBox (
         80.0, 80.0, 10.0, Point3d.ZERO, 1, 1, 1);
      PolygonalMesh plate7 = MeshFactory.createBox (
         80.0, 80.0, 10.0, Point3d.ZERO, 7, 7, 7);
      PolygonalMesh plate15 = MeshFactory.createBox (
         80.0, 80.0, 10.0, Point3d.ZERO, 17, 17, 17);

      RandomTransform randt = new RandomTransform2d (15.0);

      lego1.setMeshToWorld (new RigidTransform3d (0, 0, 6.0, 0, 0, Math.PI));

      for (int i=0; i<44; i++) { // i=20 gives a bug
         RigidTransform3d T1 = new RigidTransform3d (i-22, 0, 0);
         testPlateLego (plate1, lego1, T1, 5);
         testPlateLego (plate7, lego1, T1, 5);
         testPlateLego (plate15, lego1, T1, 5);
      }

      for (int i=0; i<100; i++) {
         testPlateLego (plate1, lego1, randt.nextTransform(), 5);
         testPlateLego (plate7, lego1, randt.nextTransform(), 5);
         testPlateLego (plate15, lego1, randt.nextTransform(), 5);
      }
         
      PolygonalMesh indented = MeshFactory.createSkylineMesh (
         2.25, 2.25, 1.0, 9, 9, 
         "111111111",
         "111111111",
         "111111111",
         "111111111",
         "1111 1111",
         "111111111",
         "111111111",
         "111111111",
         "111111111");

      //indented.setMeshToWorld (new RigidTransform3d (0, 0, 1));
      PolygonalMesh cube = MeshFactory.createBox (1.0, 1.0, 1.0);
      cube.setMeshToWorld (new RigidTransform3d (0, 0, 0));
      randt = new RandomTransform2d (0.25);

      testContacts (
         indented, cube, null, 0, 5,
         new int[][] {
            new int[] { 0 },
            new int[] { 109, 110, 131, 133 }, //{ 111, 112, 113, 114 },
         },
         new int[][] {
            new int[] { 0 },
            new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
         },
         new double[] { 0.5 + sqr(.25) },
         new double[] { 6-sqr(.25) },
         new double[][] {
            new double[] {
               0.125,  0.125,  0.5,
              -0.125,  0.125,  0.5,
              -0.125, -0.125,  0.5,
               0.125, -0.125,  0.5,
            }
         });

      cube.setMeshToWorld (new RigidTransform3d (0.25, -0.25, 0));

      testContacts (
         indented, cube, null, 0, 5,
         new int[][] {
            new int[] { 0 },
            new int[] { 109, 110, 131, 133 }, //{ 111, 112, 113, 114 },
         },
         new int[][] {
            new int[] { 0 },
            new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
         },
         new double[] { 0.5 + sqr(.25) },
         new double[] { 6-sqr(.25) },
         new double[][] {
            new double[] {
               0.125,  0.125,  0.5,
              -0.125,  0.125,  0.5,
              -0.125, -0.125,  0.5,
               0.125, -0.125,  0.5,
            }
         });

      cube.setMeshToWorld (new RigidTransform3d (0, 0, 0));

      testContacts (
         cube, indented, randt, 4, 4,
         new int[][] {
            new int[] { 0 },
            new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
         },
         new int[][] {
            new int[] { 0 },
            new int[] { 109, 110, 131, 133 }, //{ 111, 112, 113, 114 }, 

         },
         new double[] { 6-sqr(.25) },
         new double[] { 0.5 + sqr(.25) },
         new double[][] {
            new double[] {
               0.125,  0.125,  0.5,
               0.125, -0.125,  0.5,
              -0.125, -0.125,  0.5,
              -0.125,  0.125,  0.5,
            }
         });      
      
   }

   private double[][] crownTopContours0 = new double[][] {
      new double[] {
         -1.0,  0.0, 1.0,
         -1.0, -1.0, 1.0,
         0.0,  0.0, 1.0,
         1.0, -1.0, 1.0,
         1.0,  0.0, 1.0,
         1.0,  1.0, 1.0,
         0.0,  0.0, 1.0,
         -1.0,  1.0, 1.0
      }};

   private void crownTopBoxTest0 (
      PolygonalMesh mesh0, PolygonalMesh mesh1, RigidTransform3d T10) {

      testContacts (
         mesh0, mesh1, RigidTransform3d.IDENTITY, T10,
         new int[][] {
            new int[] { 0 },
            null,
         },
         new int[][] {
            new int[] { 0 },
            null,
         },
         new double[] { 3.449489742783178 },
         new double[] { 2.0 },
         crownTopContours0);      
   }

   private double[][] crownTopContours1 = new double[][] {
      new double[] {
         -1.0,  1.0, 1.0,
         -1.0, -1.0, 1.0,
         0.0,  0.0, 1.0,
      },
      new double[] {
         0.0,  0.0, 1.0,
         1.0, -1.0, 1.0,
         1.0, 1.0, 1.0,
      }};

   private void crownTopBoxTest1 (
      PolygonalMesh mesh0, PolygonalMesh mesh1, RigidTransform3d T10) {

      testContacts (
         mesh0, mesh1, RigidTransform3d.IDENTITY, T10,
         new int[][] {
            new int[] { 0 },
            null,
            new int[] { 1 },
            null,
         },
         new int[][] {
            new int[] { 0 },
            null,
            new int[] { 1 },
            null,
         },
         new double[] { 1.72474487139159, 1.72474487139159 },
         new double[] { 1.0, 1.0 },
         crownTopContours1);      
   }

   public void crownTopBoxTests() {

      if (!mySilentP) {
         System.out.println ("Crown top box tests:");
      }

      SurfaceMeshIntersector smi = createIntersector();

      // crown box
      PolygonalMesh crownBox = MeshFactory.createBox (
         2.0, 2.0, 2.0, Point3d.ZERO, 2, 2, 2,
         /*addNormals=*/false, FaceType.ALT_TRI);

      crownBox.getVertex(7).pnt.z -= 0.5;
      crownBox.getVertex(11).pnt.z -= 0.5;
      crownBox.getVertex(5).pnt.z += 0.5;
      crownBox.getVertex(14).pnt.z += 0.5;
      crownBox.notifyVertexPositionsModified();

      PolygonalMesh plate = MeshFactory.createBox (
         3.0, 3.0, 1.0, Point3d.ZERO, 2, 2, 1,
         /*addNormals=*/false, FaceType.ALT_TRI); 

      plate.transform (new RigidTransform3d (0, 0, 1.5));

      checkIntersectionTopology = WATERTIGHT;
      checkUnionTopology = WATERTIGHT;
      checkDifferenceTopology = WATERTIGHT;

      int numRandomPerturbations = 200;
      for (int i=0; i<numRandomPerturbations; i++) {
         RigidTransform3d T10 = new RigidTransform3d();
         if (i > 0) {
            RigidTransform3d TRAN = new RigidTransform3d();
            TRAN.setRandom();
            T10.mulInverseLeft (TRAN, TRAN);
            T10.mul (TRAN);
            T10.mulInverse (TRAN);
         }
         plate.setMeshToWorld (T10);
         ArrayList<IntersectionContour> contours =
            smi.findContours (crownBox, plate);
         plate.setMeshToWorld (RigidTransform3d.IDENTITY);

         //System.out.println ("T10=\n" + T10);
         if (contoursEqual (contours, crownTopContours0)) {
            crownTopBoxTest0 (crownBox, plate, T10);
         }
         else if (contoursEqual (contours, crownTopContours1)) {
            crownTopBoxTest1 (crownBox, plate, T10);
         }
         else {
            throw new TestException ("Contours not found");
         }
      }

      checkIntersectionTopology = CLOSED;
      checkUnionTopology = CLOSED;
      checkDifferenceTopology = CLOSED;
   }

   double[][] crownContours0 = new double[][] {
      new double[] {
         -0.68301270189222,  -0.68301270189222,   1.00000000000000,
         0.00000000000000,   0.00000000000000,   1.00000000000000,
         -0.25000000000000,  -0.93301270189222,   1.00000000000000,
         0.00000000000000,  -1.00000000000000,   1.00000000000000,
         0.25000000000000,  -0.93301270189222,   1.00000000000000,
         0.00000000000000,   0.00000000000000,   1.00000000000000,
         0.68301270189222,  -0.68301270189222,   1.00000000000000,
         0.86602540378444,  -0.50000000000000,   1.00000000000000,
         0.93301270189222,  -0.25000000000000,   1.00000000000000,
         0.00000000000000,   0.00000000000000,   1.00000000000000,
         0.93301270189222,   0.25000000000000,   1.00000000000000,
         0.86602540378444,   0.50000000000000,   1.00000000000000,
         0.68301270189222,   0.68301270189222,   1.00000000000000,
         0.00000000000000,   0.00000000000000,   1.00000000000000,
         0.25000000000000,   0.93301270189222,   1.00000000000000,
         0.00000000000000,   1.00000000000000,   1.00000000000000,
         -0.25000000000000,   0.93301270189222,   1.00000000000000,
         0.00000000000000,   0.00000000000000,   1.00000000000000,
         -0.68301270189222,   0.68301270189222,   1.00000000000000,
         -0.86602540378444,   0.50000000000000,   1.00000000000000,
         -0.93301270189222,   0.25000000000000,   1.00000000000000,
         0.00000000000000,   0.00000000000000,   1.00000000000000,
         -0.93301270189222,  -0.25000000000000,   1.00000000000000,
         -0.86602540378444,  -0.50000000000000,   1.00000000000000,
      }
   };

   double[][] crownContours6 = new double[][] {
      new double[] {
         -0.68301270189222,  -0.68301270189222,   1.00000000000000,
         -0.00000000000000,  -0.00000000000000,   1.00000000000000,
         -0.93301270189222,  -0.25000000000000,   1.00000000000000,
         -0.86602540378444,  -0.50000000000000,   1.00000000000000,
      },
      new double[] {      
         0.00000000000000,  -0.00000000000000,   1.00000000000000,
         -0.25000000000000,  -0.93301270189222,   1.00000000000000,
         0.00000000000000,  -1.00000000000000,   1.00000000000000,
         0.25000000000000,  -0.93301270189222,   1.00000000000000,
      },
      new double[] {      
         -0.93301270189222,   0.25000000000000,   1.00000000000000,
         -0.00000000000000,   0.00000000000000,   1.00000000000000,
         -0.68301270189222,   0.68301270189222,   1.00000000000000,
         -0.86602540378444,   0.50000000000000,   1.00000000000000,
      },
      new double[] {      
         -0.25000000000000,   0.93301270189222,   1.00000000000000,
         0.00000000000000,   0.00000000000000,   1.00000000000000,
         0.25000000000000,   0.93301270189222,   1.00000000000000,
         0.00000000000000,   1.00000000000000,   1.00000000000000,
      },
      new double[] {      
         0.00000000000000,  -0.00000000000000,   1.00000000000000,
         0.68301270189222,  -0.68301270189222,   1.00000000000000,
         0.86602540378444,  -0.50000000000000,   1.00000000000000,
         0.93301270189222,  -0.25000000000000,   1.00000000000000,
      },
      new double[] {      
         0.00000000000000,   0.00000000000000,   1.00000000000000,
         0.93301270189222,   0.25000000000000,   1.00000000000000,
         0.86602540378444,   0.50000000000000,   1.00000000000000,
         0.68301270189222,   0.68301270189222,   1.00000000000000,
      }
   };

   private void crownCylinderTest0 (
      PolygonalMesh mesh0, PolygonalMesh mesh1, RigidTransform3d T10) {

      testContacts (
         mesh0, mesh1, RigidTransform3d.IDENTITY, T10,
         new int[][] {
            new int[] { 0 },
            null,
         },
         new int[][] {
            new int[] { 0 },
            null,
         },
         new double[] { 2.47372097455856 },
         new double[] { 1.5 },
         crownContours0);      
   }

   private void crownCylinderTest6 (
      PolygonalMesh mesh0, PolygonalMesh mesh1, RigidTransform3d T10) {

      double a = 2.47372097455856/6;
      testContacts (
         mesh0, mesh1, RigidTransform3d.IDENTITY, T10,
         new int[][] {
            new int[] { 0 },
            null,
            new int[] { 1 },
            null,
            new int[] { 2 },
            null,
            new int[] { 3 },
            null,
            new int[] { 4 },
            null,
            new int[] { 5 },
            null,
         },
         new int[][] {
            new int[] { 0 },
            null,
            new int[] { 1 },
            null,
            new int[] { 2 },
            null,
            new int[] { 3 },
            null,
            new int[] { 4 },
            null,
            new int[] { 5 },
            null,
         },
         new double[] { a, a, a, a, a, a },
         new double[] { 0.25, 0.25, 0.25, 0.25, 0.25, 0.25 },
         crownContours6);      
   }

   public void crownCylinderTests() {

      if (!mySilentP) {
         System.out.println ("Crown cylinder tests:");
      }

      SurfaceMeshIntersector smi = createIntersector();

      PolygonalMesh crownCylinder = MeshFactory.createCylinder (1.0, 2.0, 12);

      int[] upVtxs = new int[] {2, 7, 11, 15, 19, 23 };
      int[] downVtxs = new int[] {3, 5, 9, 13, 17, 21};

      for (int k : upVtxs) {
         crownCylinder.getVertex(k).pnt.z += 0.25;
      }
      for (int k : downVtxs) {
         crownCylinder.getVertex(k).pnt.z -= 0.25;
      }
      crownCylinder.notifyVertexPositionsModified();

      // crown will intersect on a single face
      PolygonalMesh facePlate = MeshFactory.createBox (4.0, 4.0, 1.0);
      // offset so crown lies entirely on one face
      facePlate.transform (new RigidTransform3d (0.8, -0.8, 1.5));

      // crown will intersect on the single edge at the center 
      PolygonalMesh edgePlate = MeshFactory.createBox (4.0, 4.0, 1.0);
      edgePlate.transform (new RigidTransform3d (0, 0, 1.5));

      // crown will intersect on a vertex at the center 
      PolygonalMesh vertexPlate = MeshFactory.createBox (
         3.0, 3.0, 1.0, Point3d.ZERO, 2, 2, 1,
         /*addNormals=*/false, FaceType.ALT_TRI);
      vertexPlate.transform (new RigidTransform3d (0, 0, 1.5));

      //System.out.println ("plate area=" + facePlate.computeArea());
      //System.out.println ("crown area=" + crownCylinder.computeArea());

      checkIntersectionTopology = WATERTIGHT;
      checkUnionTopology = WATERTIGHT;
      checkDifferenceTopology = WATERTIGHT;

      PolygonalMesh[] plates = new PolygonalMesh[] {
         facePlate, edgePlate, vertexPlate };

      int numRandomPerturbations = 100;
      for (int k=0; k<plates.length; k++) {
         PolygonalMesh plate = plates[k];
         for (int i=0; i<numRandomPerturbations+1; i++) {
            //System.out.println ("k="+k+" i="+ i);
            RigidTransform3d T10 = new RigidTransform3d();
            if (i > 0) {
               RigidTransform3d TRAN = new RigidTransform3d();
               TRAN.setRandom();
               T10.mulInverseLeft (TRAN, TRAN);
               T10.mul (TRAN);
               T10.mulInverse (TRAN);
            }
            plate.setMeshToWorld (T10);
            ArrayList<IntersectionContour> contours =
               smi.findContours (crownCylinder, plate);
            plate.setMeshToWorld (RigidTransform3d.IDENTITY);

            //System.out.println ("T10=\n" + T10);
            if (contoursEqual (contours, crownContours0)) {
               //System.out.println ("contours 0");
               crownCylinderTest0 (crownCylinder, plate, T10);
            }
            else {
               //System.out.println ("contours 6");
               crownCylinderTest6 (crownCylinder, plate, T10);
            }
            // else if (contoursEqual (contours, crownContours1)) {
            //    crownTopBoxTest1 (crownCylinder, plate, T10);
            // }
            // else {
            //    throw new TestException ("Contours not found");
            // }
         }
      }

      checkUnionTopology = CLOSED;
      checkIntersectionTopology = CLOSED;
      checkDifferenceTopology = CLOSED;
   }

   private boolean isInterior (Vertex3d v) {
      Iterator<HalfEdge> it = v.getIncidentHalfEdges();
      while (it.hasNext()) {
         HalfEdge he = it.next();
         if (he.opposite == null) {
            return false;
         }
      }
      return true;
   }

   private void perturbZ (PolygonalMesh mesh, double min, double max) {
      for (Vertex3d v : mesh.getVertices()) {
         if (isInterior (v)) {
            v.pnt.z = RandomGenerator.nextDouble (min, max);
         }
      }
      mesh.notifyVertexPositionsModified();
      mesh.updateFaceNormals();
   }

   public void perturbedPlateTests() {

      if (!mySilentP) {
         System.out.println ("Perturbed plate tests:");
      }

      PolygonalMesh plate1x1_0 =
         MeshFactory.createRectangle (10.0, 10.0, 1, 1, false);
      PolygonalMesh plate5x5_0 =
         MeshFactory.createRectangle (10.0, 10.0, 5, 5, false);
      PolygonalMesh plate20x20_0 =
         MeshFactory.createRectangle (10.0, 10.0, 20, 20, false);
      PolygonalMesh plate5x5_1 =
         MeshFactory.createRectangle (10.0, 10.0, 5, 5, false);
      PolygonalMesh bigPlate20x20_1 =
         MeshFactory.createRectangle (30.0, 30.0, 5, 5, false);

      //plate5x5_1.setMeshToWorld (new RigidTransform3d (0, 0, 0.01));

      RandomTransform randt = new RandomTransform2d (5.0);
      
      for (int i=0; i<100; i++) {

         checkIntersectionTopology = MANIFOLD;
         checkUnionTopology = MANIFOLD;
         checkDifferenceTopology = MANIFOLD;

         plate5x5_1.setMeshToWorld (new RigidTransform3d (0, 0, 0.01));

         perturbZ (plate5x5_1, -0.02, 0.01);

         TestInfo tinfo = new TestInfo();
         ContactInfo cinfo =
            createTestInfo (tinfo, plate5x5_0, plate5x5_1, 1e-12);
         for (int k=1; k<tinfo.regions0Check.length; k+=2) {
            tinfo.regions0Check[k] = null;
         }

         testContacts (plate1x1_0, plate5x5_1, null, 0, 4, 
                       tinfo.regions0Check, tinfo.regions1Check,
                       tinfo.regions0AreaCheck, 
                       tinfo.regions1AreaCheck,
                       tinfo.contoursCheck);
         testContacts (plate20x20_0, plate5x5_1, null, 0, 4,
                       tinfo.regions0Check, tinfo.regions1Check,
                       tinfo.regions0AreaCheck, 
                       tinfo.regions1AreaCheck,
                       tinfo.contoursCheck);
         
         // now flip the plate around so that both plates are facing each other
         checkIntersectionTopology = CLOSED;

         plate5x5_1.setMeshToWorld (
            new RigidTransform3d (0, 0, 0.01, 0, 0, Math.PI));
         perturbZ (plate5x5_1, -0.01, 0.02);
         cinfo =
            createTestInfo (tinfo, plate5x5_0, plate5x5_1, 1e-12);
         for (int k=0; k<tinfo.regions0Check.length/2; k++) {
            tinfo.regions0Check[2*k+1] = null;
            tinfo.regions0AreaCheck[k] =
               cinfo.myRegions0.get(k).computePlanarArea (
                  Vector3d.Z_UNIT, Point3d.ZERO, /*clockwize=*/false);
         }

         testContacts (plate5x5_0, plate5x5_1, null, 0, 4, 
                       tinfo.regions0Check, tinfo.regions1Check,
                       tinfo.regions0AreaCheck, 
                       tinfo.regions1AreaCheck,
                       tinfo.contoursCheck);
         testContacts (plate1x1_0, plate5x5_1, null, 0, 2, 
                       tinfo.regions0Check, tinfo.regions1Check,
                       tinfo.regions0AreaCheck, 
                       tinfo.regions1AreaCheck,
                       tinfo.contoursCheck);
         testContacts (plate20x20_0, plate5x5_1, null, 0, 2,
                       tinfo.regions0Check, tinfo.regions1Check,
                       tinfo.regions0AreaCheck, 
                       tinfo.regions1AreaCheck,
                       tinfo.contoursCheck);

         // bigPlate20x20_1.setMeshToWorld (
         //    new RigidTransform3d (0.56273e-12, .33336e-11, 0));
         //if (i == 18) debug = true;
         testContacts (bigPlate20x20_1, plate5x5_1, randt, 4, 4, // 4, 4
                       tinfo.regions0Check, tinfo.regions1Check,
                       tinfo.regions0AreaCheck, 
                       tinfo.regions1AreaCheck,
                       tinfo.contoursCheck);
         
         checkUnionTopology = CLOSED;
         checkDifferenceTopology = CLOSED;
         debug = false;
      }
   }

   public void miscTests() {

      if (!mySilentP) {
         System.out.println ("Misc tests:");
      }

      PolygonalMesh hollowRect = MeshFactory.createSkylineMesh (
         3.0, 4.0, 1.0, 3, 4,
         "111",
         "1 1",
         "1 1",
         "111");

      PolygonalMesh boxA = MeshFactory.createBox (
         6.0, 5.0, 1.0, Point3d.ZERO, 3, 5, 1);

      hollowRect.transform (new RigidTransform3d (0.0, 0.0, -1.0));

      double[][] hollowRectContours = new double[][] {
         new double[] {
            -1.5, -2.0, -0.5, 
            -1.5,  2.0, -0.5, 
             1.5,  2.0, -0.5, 
             1.5, -2.0, -0.5, 
         },
         new double[] {
            -0.5, -1.0, -0.5, 
             0.5, -1.0, -0.5, 
             0.5,  1.0, -0.5, 
            -0.5,  1.0, -0.5, 
         },
      };           

      testContacts (
         boxA, hollowRect, null, 1, 0,
         new int[][] {
            new int[] { 0, 1 },
            new int[] {32, 17, 30, 13, 28, 9, 24, 2 },
         },
         new int[][] {
            new int[] { 0, 1 },
            null,
         },
         new double[] {10.0},
         new double[] {20.0},
         hollowRectContours);

      PolygonalMesh boxB = MeshFactory.createBox (
         4.0, 5.0, 1.0, Point3d.ZERO, 2, 5, 1);

      testContacts (
         boxB, hollowRect, null, 1, 0,
         new int[][] {
            new int[] { 0, 1 },
            new int[] {2, 17},
         },
         new int[][] {
            new int[] { 0, 1 },
            null,
         },
         new double[] {10.0},
         new double[] {20.0},
         hollowRectContours);

     PolygonalMesh twoBar = MeshFactory.createSkylineMesh (
         3.0, 4.0, 1.0, 3, 4, 
         "   ",
         "1 1",
         "1 1",
         "   ");

      twoBar.transform (new RigidTransform3d (0.0, 0.0, -1.0));

      double[][] twoBarContours = new double[][] {
         new double[] {
            -1.5, -1.0, -0.5, 
            -1.5,  1.0, -0.5, 
            -0.5,  1.0, -0.5, 
            -0.5, -1.0, -0.5, 
         },
         new double[] {
             0.5, -1.0, -0.5, 
             0.5,  1.0, -0.5, 
             1.5,  1.0, -0.5, 
             1.5, -1.0, -0.5, 
         },
      }; 

      testContacts (
         boxA, twoBar, null, 1, 0,
         new int[][] {
            new int[] { 0 },
            new int[] {9, 13},
            new int[] { 1 },
            new int[] {28, 30},
         },
         new int[][] {
            new int[] { 0 },
            null,
            new int[] { 1 },
            null,
         },
         new double[] {2.0, 2.0},
         new double[] {5.0, 5.0},
         twoBarContours);

   }

   public void closedContourTests() {

      if (!mySilentP) {
         System.out.println ("Closed contour tests:");
      }
      //abortOnFail = false;
      //printFailInfo = false;

      PolygonalMesh hollowBox0 = MeshFactory.createSkylineMesh (
         3.0, 3.0, 1.0, 3, 3,
         "333",
         "3 3",
         "333");

      // shift so that vertices will be aligned
      hollowBox0.transform (new RigidTransform3d (0.0, 0.0, -1.5));   
      //hollowBox0.transform (new RigidTransform3d (0.0, 1.5, 0));

      PolygonalMesh plate1x1 = MeshFactory.createBox (
         10.0, 5.0, 1.0, Point3d.ZERO, 1, 1, 1);
      PolygonalMesh plate10x5 = MeshFactory.createBox (
         10.0, 5.0, 1.0, Point3d.ZERO, 10, 5, 1,
         /*addNormals=*/false, MeshFactory.FaceType.ALT_TRI);
      PolygonalMesh plate7x3 = MeshFactory.createBox (
         10.0, 5.0, 1.0, Point3d.ZERO, 7, 3, 1,
         /*addNormals=*/false, MeshFactory.FaceType.ALT_TRI);

      double[][] hollowBoxContours0 = new double[][] {
         // contours on the front (+z) side of the plate
         new double[] {
            -0.5, -0.5, 0.5, 
            -0.5,  0.5, 0.5,
             0.5,  0.5, 0.5, 
             0.5, -0.5, 0.5, 
         },
         new double[] {
            -1.5,  1.5, 0.5,
            -1.5, -1.5, 0.5, 
             1.5, -1.5, 0.5, 
             1.5,  1.5, 0.5, 
         },
         // contours on the back (-z) side of the plate
         new double[] {
            -0.5, -0.5, -0.5, 
             0.5, -0.5, -0.5, 
             0.5,  0.5, -0.5, 
            -0.5,  0.5, -0.5,
         },
         new double[] {
            -1.5,  1.5, -0.5,
             1.5,  1.5, -0.5, 
             1.5, -1.5, -0.5, 
            -1.5, -1.5, -0.5, 
         },
      };           

      RigidTransform3d T10 = new RigidTransform3d();

      PolygonalMesh[] plates = new PolygonalMesh[] {
         plate1x1, plate10x5, plate7x3 
      };

      // setting this to > 0 causes degeneracy errors at the moment
      int numRandomPerturbations = 100;

      RigidTransform3d TBW = new RigidTransform3d();

      // special test:
      TBW.set (new double[] {
            0.9999999999999999,-1.1102230246251565E-16,1.1102230246251565E-16,2.7755575615628914E-17,
            -1.1102230246251565E-16,1.0000000000000002,-2.7755575615628914E-16,-5.551115123125783E-17,
            5.551115123125783E-17,-2.7755575615628914E-16,1.0,2.0816681711721685E-17,
            0.0,0.0,0.0,1.0
         });
      T10.p.set (-2.5, 0, 0);
      testContacts (
         plate10x5, hollowBox0, TBW, T10, 
         new int[][] {
            new int[] { 0, 1 },
            null,
            new int[] { 2, 3 },
            null,
         },
         new int[][] {
            new int[] {0, 2},
            null,
            new int[] {1, 3},
            null,
         },
         new double[] {8.0, 8.0},
         new double[] {4.0, 12.0},
         hollowBoxContours0);

      // can't check inside vertices since some are right on the boundary:
      for (int kr=0; kr<numRandomPerturbations+1; kr++) {
         TBW.setIdentity();
         if (kr > 0) {
            RigidTransform3d TRAN = new RigidTransform3d();
            TRAN.setRandom();
            TBW.mulInverseLeft (TRAN, TRAN);
            TBW.mul (TRAN);
            TBW.mulInverse (TRAN);
         }
         for (int ip=0; ip<plates.length; ip++) {
            PolygonalMesh plate = plates[ip];
            for (int i=-6; i<=6; i++) {
               //System.out.println ("kr="+kr+" ip="+ip+" i=" + i);
               T10.p.set (0.5*i, 0, 0);
               testContacts (
                  plate, hollowBox0, TBW, T10, 
                  new int[][] {
                     new int[] { 0, 1 },
                     null,
                     new int[] { 2, 3 },
                     null,
                  },
                  new int[][] {
                     new int[] {0, 2},
                     null,
                     new int[] {1, 3},
                     null,
                  },
                  new double[] {8.0, 8.0},
                  new double[] {4.0, 12.0},
                  hollowBoxContours0);
            }
         }
      }
   }

   public void openContourTests() {

      if (!mySilentP) {
         System.out.println ("Open contour tests:");
      }
      //abortOnFail = false;
      //printFailInfo = false;

      // coincident ordering tests aren't completely reliable with open contours
      //checkCoincidentOrdering = false; 
      checkIntersectionTopology = MANIFOLD;
      checkUnionTopology = ISOLATED_VERTICES;
      checkDifferenceTopology = ISOLATED_VERTICES;

      PolygonalMesh hollowBox0 = MeshFactory.createSkylineMesh (
         3.0, 4.0, 1.0, 3, 4,
         "222",
         "222",
         "2 2",
         "222");

      hollowBox0.transform (new RigidTransform3d (0.0, 1.5, 0));

      PolygonalMesh rect1x1 = MeshFactory.createRectangle (10.0, 4.0, false);
      PolygonalMesh rect10x4 = MeshFactory.createRectangle (
         10.0, 4.0, 10, 4, false);
      PolygonalMesh rect7x3 = MeshFactory.createRectangle (
         10.0, 4.0, 7, 3, false);

      double[][] hollowBoxContours0 = new double[][] {
         new double[] {
            -0.5,  0.5, 0.0, 
            -0.5,  1.5, 0.0,
             0.5,  1.5, 0.0, 
             0.5,  0.5, 0.0, 
         },
         new double[] {
            -1.5,  2.0, 0.0,
            -1.5, -0.5, 0.0, 
             1.5, -0.5, 0.0, 
             1.5,  2.0, 0.0, 
            OPEN,
         },
      };           

      RigidTransform3d T10 = new RigidTransform3d();

      PolygonalMesh[] rects = new PolygonalMesh[] {
         rect1x1, rect10x4, rect7x3 
      };

      // setting this to > 0 causes degeneracy errors at the moment
      int numRandomPerturbations = 100;

      for (int kz=0; kz<2; kz++) {
         // shift hollowBox in z direction; last shift aligns vertices
         hollowBox0.transform (new RigidTransform3d (0.0, 0.0, -0.5));   
         int[] hollowBoxInsideVtxs = new int[] {18, 21, 38, 41};
         double hollowBoxInsideArea;
         if (kz == 0) {
            hollowBoxInsideVtxs = new int[] {18, 21, 38, 41};
            hollowBoxInsideArea = 3.0;
         }
         else {
            // can't check since answer is ambiguous; some vertices are right
            // on the boundary:
            hollowBoxInsideVtxs = null;
            hollowBoxInsideArea = 5.0;
         }
         //System.out.println ("kz=" + kz);
         for (int kr=0; kr<numRandomPerturbations+1; kr++) {
            RigidTransform3d TBW = new RigidTransform3d();
            if (kr > 0) {
               RigidTransform3d TRAN = new RigidTransform3d();
               TRAN.setRandom();
               TBW.mulInverseLeft (TRAN, TRAN);
               TBW.mul (TRAN);
               TBW.mulInverse (TRAN);
            }
            //System.out.println ("kr=" + kr);
            for (PolygonalMesh rect : rects) {
               for (int i=-6; i<=6; i++) {
                  T10.p.set (0.5*i, 0, 0);
                  //SurfaceMeshIntersector.debug2 = ((i==-5)&&(kr==215)&&(kz==0));
                  //System.out.println ("i=" + i + " kr=" + kr + " kz=" + kz);
                  testContacts (
                     rect, hollowBox0, TBW, T10, 
                     new int[][] {
                        new int[] { 0, 1 },
                        null,
                     },
                     new int[][] {
                        new int[] {0},
                        hollowBoxInsideVtxs,
                     },
                     new double[] {6.5},
                     new double[] {hollowBoxInsideArea},
                     hollowBoxContours0);
               }
            }
         }
      }

      PolygonalMesh hollowBox1 = MeshFactory.createSkylineMesh (
         3.0, 4.0, 1.0, 3, 4, 
         "222",
         "2 2",
         "2 2",
         "222");

      hollowBox1.transform (new RigidTransform3d (0.0, 2.5, 0));

      double[][] hollowBoxContours1 = new double[][] {
         new double[] {
             0.5,  2.0, 0.0,
             0.5,  1.5, 0.0, 
            -0.5,  1.5, 0.0, 
            -0.5,  2.0, 0.0, 
            OPEN,
         },
         new double[] {
            -1.5,  2.0, 0.0,
            -1.5,  0.5, 0.0, 
             1.5,  0.5, 0.0, 
             1.5,  2.0, 0.0, 
            OPEN,
         },
      };           

      for (int kz=0; kz<2; kz++) {
         // shift hollowBox in z direction; last shift aligns vertices
         hollowBox1.transform (new RigidTransform3d (0.0, 0.0, -0.5));   
         for (int kr=0; kr<numRandomPerturbations+1; kr++) {
            RigidTransform3d TBW = new RigidTransform3d();
            if (kr > 0) {
               TBW.setRandom();
               TBW.mulInverse (TBW);
            }
            for (PolygonalMesh rect : rects) {
               for (int i=-6; i<=6; i++) {
                  T10.p.set (0.5*i, 0, 0);
                  testContacts (
                     rect, hollowBox1, TBW, T10, 
                     new int[][] {
                        new int[] { 0, 1 },
                        null,
                     },
                     new int[][] {
                     },
                     new double[] {4.0},
                     new double[] {},
                     hollowBoxContours1);
               }
            }
         }
      }
      
      // shift everything down 0.5 so we get more vertex alignment

      hollowBoxContours1 = new double[][] {
         new double[] {
             0.5,  2.0, 0.0,
             0.5,  1.0, 0.0, 
            -0.5,  1.0, 0.0, 
            -0.5,  2.0, 0.0, 
            OPEN,
         },
         new double[] {
            -1.5,  2.0, 0.0,
            -1.5,  0.0, 0.0, 
             1.5,  0.0, 0.0, 
             1.5,  2.0, 0.0, 
            OPEN,
         },
      };

      hollowBox1.transform (new RigidTransform3d (0, -0.5, 1.0));

      for (int kz=0; kz<2; kz++) {
         // shift hollowBox in z direction; last shift aligns vertices
         hollowBox1.transform (new RigidTransform3d (0.0, 0.0, -0.5));   
         for (int kr=0; kr<numRandomPerturbations+1; kr++) {
            RigidTransform3d TBW = new RigidTransform3d();
            if (kr > 0) {
               TBW.setRandom();
               TBW.mulInverse (TBW);
            }
            for (PolygonalMesh rect : rects) {
               for (int i=-6; i<=6; i++) {
                  //SurfaceMeshIntersector.debug2 = ((i==2)&&(kr==297)&&(kz==1));
                  //System.out.println ("i=" + i + " kr=" + kr + " kz=" + kz);
                  T10.p.set (0.5*i, 0, 0);
                  testContacts (
                     rect, hollowBox1, TBW, T10, 
                     new int[][] {
                        new int[] { 0, 1 },
                        null,
                     },
                     new int[][] {
                     },
                     new double[] {5.0},
                     new double[] {},
                     hollowBoxContours1);
               }
            }
         }
      }
      //checkCoincidentOrdering = true;
      checkUnionTopology = CLOSED;
      checkIntersectionTopology = CLOSED;
      checkDifferenceTopology = CLOSED;
   }

   public static int[] getFaceIndices(Collection<Face> faces) {
      int[] idxs = new int[faces.size()];
      int i=0;
      for (Face f : faces) {
         idxs[i++] = f.getIndex();
      }
      return idxs;      
   }

   private void runTimingTest() {

      String dataDir = 
         PathFinder.expand (
            "$ARTISYNTH_HOME/src/maspack/geometry/sampleData/");

      PolygonalMesh mesh0 = null;
      PolygonalMesh mesh1 = null;

      try {
         mesh0 = new PolygonalMesh (dataDir+"molar1.2.obj");
         mesh1 = new PolygonalMesh (dataDir+"molar2.2.obj");
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }

      int numTrials = 200;
      SurfaceMeshCollider scollider = new SurfaceMeshCollider();
      MeshCollider mcollider = new MeshCollider();

      //boolean rigidBodyRigidBody = true;

      System.out.println ("Warming up ...");
      for (int i=0; i<100; i++) {
         //scollider.meshIntersector.findContours (mesh0, mesh1);
         scollider.getContacts (mesh0, mesh1);
         mcollider.getContacts (mesh0, mesh1);
      }

      scollider.meshIntersector.numClosestIntersectionCalls = 0;
      scollider.meshIntersector.numRobustClosestIntersectionCalls = 0;
      scollider.meshIntersector.tot = 0;

      System.out.println ("SurfaceMeshCollider ...");
      FunctionTimer timer = new FunctionTimer();
      timer.start();
      RigidTransform3d T = new RigidTransform3d();
      for (int i=0; i<numTrials; i++) {
         T.R.setRandom();
         mesh1.setMeshToWorld (T);
         scollider.meshIntersector.findContours (mesh0, mesh1);
         //scollider.getContacts (mesh0, mesh1);
      }
      timer.stop();
      System.out.println ("surfaceMeshCollider: " + timer.result(numTrials));
      System.out.println ("MeshCollider ...");
      timer.start();
      for (int i=0; i<numTrials; i++) {
         T.R.setRandom();
         mesh1.setMeshToWorld (T);
         mcollider.getContacts (mesh0, mesh1);
      }
      timer.stop();
      System.out.println ("meshCollider:        " + timer.result(numTrials));
      System.out.println (
         "numClosestIntersectionCalls = " +
         scollider.meshIntersector.numClosestIntersectionCalls);
      System.out.println (
         "numRobustClosestIntersectionCalls = " +
         scollider.meshIntersector.numRobustClosestIntersectionCalls);
      System.out.println ("tot = " + scollider.meshIntersector.tot);
   }

   private void runTestProblem (String fileName) {

      TestInfo tinfo = new TestInfo();
      PolygonalMesh mesh0 = new PolygonalMesh();
      PolygonalMesh mesh1 = new PolygonalMesh();

      try {
         scanProblem (fileName, mesh0, mesh1, tinfo);
      }
      catch (Exception e) {
         e.printStackTrace(); 
         System.exit(1);
      }
      testContacts (mesh0, mesh1, tinfo);
   }

   public void test() {
      if (true) {
         specialTests();
         simpleBoxTests();
         torusTests();
         singleFaceContourTests();
         perturbedPlateTests();
         miscTests();
         closedContourTests();
         openContourTests();
         singleFaceTests();
         crownCylinderTests();
      }
      else {
         crownCylinderTests();
      }
      if (!abortOnFail) {
         System.out.println ("pass=" + passCount);
         System.out.println ("fail=" + failCount);
      }

      if (!mySilentP) {
         System.out.println ("Detected mesh degeneracies:");
         System.out.println (
            "nonManifoldEdges="+numNonManifoldEdges+
            "/"+numNonManifoldEdgeChecks);
         System.out.println (
            "nonManifoldVertices="+numNonManifoldVertices+
            "/"+numNonManifoldVertexChecks);
         System.out.println (
            "openEdges="+numOpenEdges+
            "/"+numOpenEdgeChecks);
         System.out.println (
            "isolatedVertices="+numIsolatedVertices+
            "/"+numIsolatedVertexChecks);

         System.out.println ("");
         System.out.println (
            "numRegularCalls=" + SurfaceMeshIntersector.numRegularCalls);
         System.out.println (
            "numRobustCalls=" + SurfaceMeshIntersector.numRobustCalls);
         System.out.println (
            "numEdgeAdds=" + SurfaceMeshIntersector.numEdgeAdds);
         System.out.println (
            "numClosestIntersectionCalls=" +
            SurfaceMeshIntersector.numClosestIntersectionCalls);

         System.out.println (
            "numEdgeOutsideTests = " +
            Polygon3dCalc.numOutsideTests);
         System.out.println (
            "numEdgeAmbiguous = " +
            Polygon3dCalc.numOutsideAmbiguous);
      }      
   }

   static BooleanHolder testTiming = new BooleanHolder (false);
   static StringHolder testFile = new StringHolder();
   static IntHolder randomSeed = new IntHolder(0x1234);
   static BooleanHolder verbose = new BooleanHolder (false);

   public static void main (String[] args) {

      ArgParser parser =
         new ArgParser (
            "java maspack.collision.SurfaceMeshIntersectorTest [options]");
      parser.addOption (
         "-test %s #test with a specific test file", testFile);
      parser.addOption (
         "-timing %v #run timing tests", testTiming);
      parser.addOption (
         "-randomSeed %x " +
         "#seed for random values. -1 means choose one randomly", randomSeed);
      parser.addOption (
         "-verbose %v #print information about tests", verbose);

      parser.matchAllArgs (args, 0, 0);

      //System.out.println ("randomSeed=" + randomSeed.value);

      SurfaceMeshIntersectorTest tester =
         new SurfaceMeshIntersectorTest (randomSeed.value);
      tester.setSilent (!verbose.value);

      if (testFile.value != null) {
         try {
            tester.runTestProblem (testFile.value);
         }
         catch (Exception e) {
            e.printStackTrace(); 
         }
      }
      else if (testTiming.value) {
         tester.runTimingTest();
      }
      else {
         tester.setTestFailFileName ("contactTestFail.out");
         tester.runtest();
      }
   }
}
