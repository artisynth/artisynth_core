package maspack.collision;

import java.util.*;
import java.io.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.geometry.MeshFactory.VertexMap;
import maspack.geometry.io.*;
import maspack.collision.SurfaceMeshIntersector.FaceIndexComparator;

public class SurfaceMeshIntersectorTest extends UnitTest {

   double DOUBLE_PREC = 1e-16;
   double EPS = 1e-9;

   PolygonalMesh myDiamond;

   private abstract class RandomTransform {

      public abstract RigidTransform3d nextTransform();
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
   
   public SurfaceMeshIntersectorTest() {
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

   private void writeMesh (PrintWriter pw, PolygonalMesh mesh)
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

   private void writeProblem (
      String fileName, PolygonalMesh mesh0, PolygonalMesh mesh1, TestInfo tinfo)
      throws IOException {
      PrintWriter pw = new IndentingPrintWriter (
         new PrintWriter (new BufferedWriter (new FileWriter (fileName))));
      writeProblem (pw, mesh0, mesh1, tinfo);
   }

   private void writeProblem (
      PrintWriter pw, PolygonalMesh mesh0, PolygonalMesh mesh1, TestInfo tinfo)
      throws IOException {

      RigidTransform3d TMW;

      pw.print ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      pw.print ("mesh0=");
      writeMesh (pw, mesh0);
      pw.print ("mesh1=");
      writeMesh (pw, mesh1);
      pw.print ("testInfo=");
      tinfo.write (pw);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
      pw.close();
   }

   public static void scanProblem (
      String fileName,
      PolygonalMesh mesh0, PolygonalMesh mesh1, TestInfo tinfo)
      throws IOException {

      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (new FileReader (fileName)));
      scanProblem (rtok, mesh0, mesh1, tinfo);
   }

   private static void scanProblem (
      ReaderTokenizer rtok,
      PolygonalMesh mesh0, PolygonalMesh mesh1, TestInfo tinfo)
      throws IOException {
   
      RigidTransform3d TMW = new RigidTransform3d();

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
         else {
            throw new IOException ("Unexpected token " + rtok);
         }
      }
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
         int[] vertices = new int[r.numInsideVertices()];

         int i = 0;
         for (IntersectionContour c : r.myContours) {
            contours[i++] = cinfo.myContours.indexOf (c);
         }
         i = 0;
         for (Vertex3d v : r.myInsideVertices) {
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
      
      SurfaceMeshCollider collider = new SurfaceMeshCollider();      
      ContactInfo cinfo = collider.getContacts (mesh0, mesh1);

      if (cinfo != null) {
         int numContours = (cinfo.myContours != null ? cinfo.myContours.size() : 0);
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
      Point3d[] pnts = new Point3d[vals.length/3+1];
      int k = 0;
      if (debug) {
         System.out.println ("points");
      }
      for (int i=0; i<vals.length; i += 3) {
         pnts[k] = new Point3d (vals[i], vals[i+1], vals[i+2]);
         if (X != null) {
            pnts[k].transform (X);
         }
         if (debug) {
            System.out.println ("  " + pnts[k].toString ("%18.13f"));
         }
         k++;
      }

      if (debug) {
         System.out.println ("contour");
         for (int i=0; i<contour.size(); i++) {
            IntersectionPoint p = contour.get(i);
            System.out.println ("  " + p.toString ("%18.13f"));
         }
      }
      
      
      // repeat end point so we don't have to loop
      pnts[pnts.length-1] = pnts[0]; 
      // get length for epsilon
      double tol = EPS*contour.computeLength();
      // find contour point equal to pnts[0]
      int j0 = 0;
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
      int j = j0;
      k = 0;
      Point3d ps = new Point3d();
      double sprev = 0;
      do {
         double s = LineSegment.projectionParameter (
            pnts[k], pnts[k+1], contour.getWrapped(j));
         if (debug) System.out.println ("k=" + k + " j=" + j + " s=" + s);
         
         ps.combine (1-s, pnts[k], s, pnts[k+1]);
         if (!ps.epsilonEquals (contour.getWrapped(j), tol)) {
            if (debug) System.out.println ("strayed off path");
            return false;
         }
         if (s-sprev < -EPS) {
            if (debug) System.out.println ("going backwards");
            return false;
         }
         else if (s > 1+EPS) {
            if (debug) System.out.println ("overshoot");
            return false;
         }
         if (k == pnts.length-1 && Math.abs(s) > EPS) {
            if (debug) System.out.println ("should be at the end");
            return false;
         }
         if (Math.abs(s-1) < EPS && k < pnts.length-2) {
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

   private void checkPenetratingVertices (
      ArrayList<PenetratingPoint> points, int[] check) {
      
      if (check == null) {
         check = new int[0];
      }
      if (points.size() != check.length) {
         throw new TestException (
            "Expected "+check.length+" vertices, got " +  points.size());
      }
      HashSet<Integer> vertices = new HashSet<Integer>();
      for (PenetratingPoint cpp : points) {
         vertices.add (cpp.vertex.getIndex());
      }
      for (int i=0; i<check.length; i++) {
         if (vertices.contains (check[i])) {
            vertices.remove (check[i]);
         }
         else {

            throw new TestException (
               "Penetrating vertex "+check[i]+" not found");
         }
      }
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

   private void checkInsideVertices (
      Collection<Vertex3d> vtxs, int[] check) {
      
      if (check == null) {
         check = new int[0];
      }
      if (vtxs.size() != check.length) {
         throw new TestException (
            "Expected "+check.length+" vertices, got " +  vtxs.size());
      }
      HashSet<Integer> indices = new HashSet<Integer>();
      for (Vertex3d vtx : vtxs) {
         indices.add (vtx.getIndex());
      }
      for (int i=0; i<check.length; i++) {
         if (indices.contains (check[i])) {
            indices.remove (check[i]);
         }
         else {

            throw new TestException (
               "Penetrating vertex "+check[i]+" not found");
         }
      }
   }

   private int[] getVertexIndices (PenetrationRegion region) {
      ArrayList<Integer> list = new ArrayList<Integer>();
      for (Vertex3d v : region.myInsideVertices) {
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

   public void checkRegions (
      String msg, int[][] regionsCheck, double[] areasCheck,
      ArrayList<PenetrationRegion> regions, 
      ArrayList<IntersectionContour> contours,
      PolygonalMesh mesh, boolean clockwise) {

      if (regionsCheck.length%2 != 0) {
         throw new TestException (
            "Regions check has odd number of entries");
      }
      int numRegionsCheck = regionsCheck.length/2;
      if (numRegionsCheck != regions.size()) {
         System.out.println ("regions:");
         for (PenetrationRegion r : regions) {
            System.out.println (" "+r.toString (contours));
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
            HashSet<Face> insideFaces = found.findInsideFaces();
            if (!insideFaces.equals (found.myInsideFaces)) {
               System.out.println (
                  "InsideFaces: " +
                  ArraySupport.toString(getFaceIndices(found.myInsideFaces)));
               System.out.println (
                  "Found insideFaces: " +
                  ArraySupport.toString(getFaceIndices(insideFaces)));
               throw new TestException (
                  "Inconsistent set of inside faces");
            }
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
      int[] insideCheck0, int[] insideCheck1, 
      int[][] regions0Check, 
      int[][] regions1Check,
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
         
         testContacts (
            mesh0, mesh1, TBW, null, insideCheck0, insideCheck1,
            regions0Check, regions1Check, regions0AreaCheck, regions1AreaCheck,
            contoursCheck);

         if (rand != null) {
            for (int k=0; k<ntests; k++) {
               testContacts (
                  mesh0, mesh1, TBW, rand.nextTransform(),
                  insideCheck0, insideCheck1,
                  regions0Check, regions1Check,
                  regions0AreaCheck, regions1AreaCheck, contoursCheck);
            }
         }
      }
   }

   String testFailFileName = null;
   String problemFileName = null;

   public void testContacts (
      PolygonalMesh mesh0, PolygonalMesh mesh1,
      RigidTransform3d TBW, RigidTransform3d T10,
      int[] insideCheck0, int[] insideCheck1, 
      int[][] regions0Check, 
      int[][] regions1Check,
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
      testContacts (mesh0, mesh1, insideCheck0, insideCheck1, tinfo);
   }      

   public void testContacts (
      PolygonalMesh mesh0, PolygonalMesh mesh1,
      int[] insideCheck0, int[] insideCheck1, 
      TestInfo tinfo) {

      SurfaceMeshCollider collider = new SurfaceMeshCollider();

      RigidTransform3d TMW0Orig = null;
      RigidTransform3d TMW1Orig = null;
      RigidTransform3d T1W = null;
      //tinfo.TBW = null;
      if (tinfo.TBW != null) {
         TMW0Orig = new RigidTransform3d (mesh0.getMeshToWorld());
         RigidTransform3d TMW0 = new RigidTransform3d();
         TMW0.mul (tinfo.TBW, TMW0Orig);
         mesh0.setMeshToWorld (TMW0);
      }
      
      if (tinfo.TBW != null || tinfo.T10 != null) {
         TMW1Orig = new RigidTransform3d (mesh1.getMeshToWorld());
         RigidTransform3d TMW1 = new RigidTransform3d();
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
      ContactInfo cinfo = collider.getContacts (mesh0, mesh1);
      // System.out.println (
      //    "vertices0=[ "+
      //    ArraySupport.toString (getPenetratingIndices (cinfo.points0))+"]");

      try {

         if (cinfo == null) {
            if (tinfo.contoursCheck.length != 0) {
               throw new TestException (
                  "No collision detected when one was expected");
            }
         }
         else {
            
            if (insideCheck0 != null) {
               checkPenetratingVertices (
                  cinfo.getPenetratingPoints0(), insideCheck0);
            }
            if (insideCheck1 != null) {
               checkPenetratingVertices (
                  cinfo.getPenetratingPoints1(), insideCheck1);
            }
      
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
                  //System.out.println (
                  // "contour "+cinfo.contours.indexOf(found)+" = check "+chki);
                  foundContours.add(found);
               }
               else {
                  printCheckContour ("checkContour", ccheck);
                  System.out.println ("contours:" );
                  RigidTransform3d T = null;
                  if (T1W != null) {
                     T = new RigidTransform3d();
                     T.invert (T1W);
                  }
                  for (IntersectionContour c : cinfo.myContours) {
                     c.printCornerPoints ("here", "%20.16f", T);
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
                  cinfo.myRegions0, foundContours, mesh0, false);
            }
            if (tinfo.regions1Check != null) {
               checkRegions (
                  "Regions1: ", tinfo.regions1Check, tinfo.regions1AreaCheck,
                  cinfo.myRegions1, foundContours, mesh1, true);
            }
         }
         
         if (TMW0Orig != null) {
            mesh0.setMeshToWorld(TMW0Orig);
         }
         if (TMW1Orig != null) {
            mesh1.setMeshToWorld(TMW1Orig);
         }
      }
      catch (TestException te) {
      
         if (TMW0Orig != null) {
            mesh0.setMeshToWorld(TMW0Orig);
         }
         if (TMW1Orig != null) {
            mesh1.setMeshToWorld(TMW1Orig);
         }
         if (testFailFileName != null) {
            try {
               writeProblem (testFailFileName, mesh0, mesh1, tinfo);
               System.out.println ("Failed test written to " + testFailFileName);
            }
            catch (Exception e) {
               e.printStackTrace(); 
            }
         }
         throw te;
      }
      if (problemFileName != null) {
         try {
            writeProblem (problemFileName, mesh0, mesh1, tinfo);
            System.out.println ("Failed test written to " + problemFileName);
         }
         catch (Exception e) {
            e.printStackTrace(); 
         }
         problemFileName = null;
      }
   }

   public void simpleBoxTests() {

      System.out.println ("Simple box tests:");

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
         new int[] {},
         new int[] {},
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
         new int[] {},
         new int[] {},
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
         new int[] {6, 2},
         new int[] {},
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
         new int[] { 24, 34, 36, 26, 25, 35, 27, 37 },
         new int[] {},
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
         null,
         new int[] {},
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
         new int[] {},
         new int[] {0, 1, 2, 3},             
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
         new int[] {},
         new int[] {},
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
         new int[] {},
         new int[] { 0, 1},
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
      // is very unstable ...
      testContacts (
         tallBox65, cube, null, 1, 0,
         new int[] { },
         new int[] { },
         new int[][] {
            new int[] { 0, 1 },
            new int[] { },
         },
         new int[][] {
            new int[] { 0 },
            new int[] {},
            new int[] { 1 },
            new int[] {},
         },
         new double[] { 2.2},
         new double[] { 0.3, 0.3 },
         new double[] { 
          -0.10000000000000,   0.25000000000000,  -0.50000000000000,
          -0.10000000000000,  -0.25000000000000,  -0.50000000000000,
           0.50000000000000,  -0.25000000000000,  -0.50000000000000,
           0.50000000000000,   0.25000000000000,  -0.50000000000000,
         },
         new double[] { 
          -0.10000000000000,  -0.25000000000000,   0.50000000000000,
          -0.10000000000000,   0.25000000000000,   0.50000000000000,
           0.50000000000000,   0.25000000000000,   0.50000000000000,
           0.50000000000000,  -0.25000000000000,   0.50000000000000,
         });

      // no intersection detected if EPS is 0 ...
      myDiamond.setMeshToWorld (new RigidTransform3d (0, 0, 0.5+EPS));
      testContacts (
         cube, myDiamond, null, 1, 0,
         new int[] { },
         new int[] { 5 },
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

      System.out.println ("Torus tests:");

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
         new int[] { },
         new int[] { 14, 4, 5, 10, 9, 13, 0, 3 },
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
         null,
         new int[] { 14, 4, 5, 10, 9, 13, 0, 3 },
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
         new int[] { },
         new int[] { 11, 9, 8, 10, 4, 1, 0, 6 },
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
         null,
         new int[] { 11, 9, 8, 10, 4, 1, 0, 6 },
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
            null,
            null,
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
            new double[] { 46, 18, 18, 2, 16, 1, 16 },
            null,
            myLegoContours);
      }
   }

   public void singleFaceContourTests() {

      System.out.println ("Single face contour tests:");

      PolygonalMesh lego1 = SurfaceMeshIntersectorTest.createLegoMesh (
         22.0, 16.0, 2.0,
         "XXXXXXXXXX",
         "X        X",
         "X XXXXXX X",
         "X X    X X",
         "X X XX X X",
         "X X    X X",
         "X XXXXXX X",
         "X        X",
         "X XXXXXX X",
         "X X    X X",
         "X X    X X XXXXX XXXXX",
         "X X    X X X   X X   X",
         "X XXXXXX X X X X X   X",
         "X        X X   X X   X",
         "XXXXXXXXXX XXXXX XXXXX",
         "");


      PolygonalMesh plate1 = MeshFactory.createBox (
         80.0, 80.0, 10.0, Point3d.ZERO, 1, 1, 1);
      PolygonalMesh plate7 = MeshFactory.createBox (
         80.0, 80.0, 10.0, Point3d.ZERO, 7, 7, 7);
      PolygonalMesh plate15 = MeshFactory.createBox (
         80.0, 80.0, 10.0, Point3d.ZERO, 17, 17, 17);

      double zh = 5.0;

      RandomTransform randt = new RandomTransform2d (15.0);

      lego1.setMeshToWorld (new RigidTransform3d (0, 0, 6.0, 0, 0, Math.PI));

      for (int i=0; i<44; i++) { // i<44
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
         
      PolygonalMesh indented = SurfaceMeshIntersectorTest.createLegoMesh (
         2.25, 2.25, 2.0, 
         "         ",
         "         ",
         "         ",
         "         ",
         "    -    ",
         "         ",
         "         ",
         "         ",
         "         ");

      indented.setMeshToWorld (new RigidTransform3d (0, 0, 1));
      PolygonalMesh cube = MeshFactory.createBox (1.0, 1.0, 1.0);
      cube.setMeshToWorld (new RigidTransform3d (0, 0, 0));
      randt = new RandomTransform2d (0.25);

      testContacts (
         indented, cube, null, 0, 5,
         null,
         null,
         new int[][] {
            new int[] { 0 },
            new int[] { 111, 112, 113, 114 },               
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

      testContacts (
         cube, indented, randt, 4, 4,
         null,
         null,
         new int[][] {
            new int[] { 0 },
            new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
         },
         new int[][] {
            new int[] { 0 },
            new int[] { 111, 112, 113, 114 },               

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

   private void addToAreaCheck (double[] areas, double a) {
      for (int i=0; i<areas.length; i++) {
         areas[i] += a;
      }
   }
        
   public void perturbedPlateTests() {

      System.out.println ("Perturbed plate tests:");

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

      plate5x5_1.setMeshToWorld (new RigidTransform3d (0, 0, 0.01));

      RandomTransform randt = new RandomTransform2d (5.0);
      
      for (int i=0; i<100; i++) {
         perturbZ (plate5x5_1, -0.02, 0.01);

         TestInfo tinfo = new TestInfo();
         ContactInfo cinfo =
            createTestInfo (tinfo, plate5x5_0, plate5x5_1, 1e-12);
         for (int k=1; k<tinfo.regions0Check.length; k+=2) {
            tinfo.regions0Check[k] = null;
         }
         testContacts (plate1x1_0, plate5x5_1, null, 0, 4, 
                       null, null,
                       tinfo.regions0Check, 
                       tinfo.regions1Check,
                       tinfo.regions0AreaCheck, 
                       tinfo.regions1AreaCheck,
                       tinfo.contoursCheck);
         testContacts (plate20x20_0, plate5x5_1, null, 0, 4,
                       null, null,
                       tinfo.regions0Check, 
                       tinfo.regions1Check,
                       tinfo.regions0AreaCheck, 
                       tinfo.regions1AreaCheck,
                       tinfo.contoursCheck);
         
         // now flip the plate around so that both plates are facing each other

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
                       null, null,
                       tinfo.regions0Check, 
                       tinfo.regions1Check,
                       tinfo.regions0AreaCheck, 
                       tinfo.regions1AreaCheck,
                       tinfo.contoursCheck);
         testContacts (plate1x1_0, plate5x5_1, null, 0, 2, 
                       null, null,
                       tinfo.regions0Check, 
                       tinfo.regions1Check,
                       tinfo.regions0AreaCheck, 
                       tinfo.regions1AreaCheck,
                       tinfo.contoursCheck);
         testContacts (plate20x20_0, plate5x5_1, null, 0, 2,
                       null, null,
                       tinfo.regions0Check, 
                       tinfo.regions1Check,
                       tinfo.regions0AreaCheck, 
                       tinfo.regions1AreaCheck,
                       tinfo.contoursCheck);

         // bigPlate20x20_1.setMeshToWorld (
         //    new RigidTransform3d (0.56273e-12, .33336e-11, 0));
         testContacts (bigPlate20x20_1, plate5x5_1, randt, 4, 4, // 4, 4
                       null, null,
                       tinfo.regions0Check, 
                       tinfo.regions1Check,
                       tinfo.regions0AreaCheck, 
                       tinfo.regions1AreaCheck,
                       tinfo.contoursCheck);
      }
   }

   private static void addSquare (
      PolygonalMesh mesh, double x, double y, double z,
      double dx, double dy, double dz, boolean flip, VertexMap vtxMap) {

      Vertex3d v0, v1, v2, v3;

      if (dz == 0) {
         // create in x-y plane
         v0 = vtxMap.getOrCreate (mesh, x, y, z, null);
         v1 = vtxMap.getOrCreate (mesh, x+dx, y, z, null);
         v2 = vtxMap.getOrCreate (mesh, x+dx, y+dy, z, null);
         v3 = vtxMap.getOrCreate (mesh, x, y+dy, z, null);
      }
      else if (dy == 0) {
         // create in z-x plane         
         v0 = vtxMap.getOrCreate (mesh, x, y, z, null);
         v1 = vtxMap.getOrCreate (mesh, x, y, z+dz, null);
         v2 = vtxMap.getOrCreate (mesh, x+dx, y, z+dz, null);
         v3 = vtxMap.getOrCreate (mesh, x+dx, y, z, null);
      }
      else if (dx == 0) {
         // create in y-z plane
         v0 = vtxMap.getOrCreate (mesh, x, y, z, null);
         v1 = vtxMap.getOrCreate (mesh, x, y+dy, z, null);
         v2 = vtxMap.getOrCreate (mesh, x, y+dy, z+dz, null);
         v3 = vtxMap.getOrCreate (mesh, x, y, z+dz, null);
      }
      else {
         throw new IllegalArgumentException ("square is not planar");
      }
      if (flip) {
         mesh.addFace (v0, v2, v1);
         mesh.addFace (v0, v3, v2);
      }
      else {
         mesh.addFace (v0, v1, v2);
         mesh.addFace (v0, v2, v3);
      }
   }

   public static int[] getFaceIndices(Collection<Face> faces) {
      int[] idxs = new int[faces.size()];
      int i=0;
      for (Face f : faces) {
         idxs[i++] = f.getIndex();
      }
      return idxs;      
   }

   public static PolygonalMesh createLegoMesh (
      double wx, double wy, double wz, String... pattern) {

      int ny = pattern.length;
      int nx = 0;
      for (int i=0; i<pattern.length; i++) {
         if (pattern[i].length() > nx) {
            nx = pattern[i].length();
         }
      }
      double[] height = new double[nx*ny];
      for (int iy=0; iy<ny; iy++) {
         String str = pattern[ny-1-iy];
         for (int ix=0; ix<nx; ix++) {
            char c = (ix < str.length() ? str.charAt(ix) : 0);
            if (c == 'x' || c == 'X') {
               height[iy*nx + ix] = wz;
            }
            else if (c == '-') {
               height[iy*nx + ix] = -wz/2;
            }
         }
      }

      PolygonalMesh mesh = new PolygonalMesh();

      // set map tolerance to be 0.01 times smallest spacing between vertices
      VertexMap vtxMap =
         new VertexMap(Math.min(Math.min(wx/nx*1e-2, wy/ny*1e-2), wz/2*1e-2));

      double dx = wx/nx;
      double dy = wy/ny;

      
      for (int iy=0; iy<ny; iy++) {
         for (int ix=0; ix<nx; ix++) {
            double x = ix*dx-wx/2;
            double y = iy*dy-wy/2;
            addSquare (mesh, x, y, -wz, dx, dy, 0, true, vtxMap);
            double hz;
            if ((hz = height[iy*nx+ix]) != 0) {
               addSquare (mesh, x, y, hz, dx, dy, 0, false, vtxMap);
               if (iy == 0 || height[(iy-1)*nx + ix] == 0) {
                  // add bottom side
                  addSquare (mesh, x, y, 0, dx, 0, hz, true, vtxMap);
               }
               if (iy == ny-1 || height[(iy+1)*nx + ix] == 0) {
                  // add top side
                  addSquare (mesh, x, y+dy, 0, dx, 0, hz, false, vtxMap);
               }
               if (ix == 0 || height[iy*nx + (ix-1)] == 0) {
                  // add left side
                  addSquare (mesh, x, y, 0, 0, dy, hz, true, vtxMap);
               }
               if (ix == (nx-1) || height[iy*nx + (ix+1)] == 0) {
                  // add right side
                  addSquare (mesh, x+dx, y, 0, 0, dy, hz, false, vtxMap);
               }
            }
            else {
               addSquare (mesh, x, y, 0, dx, dy, 0, false, vtxMap);
            }
         }
      }

      // add sides 
      for (int ix=0; ix<nx; ix++) {
         double x = ix*dx-wx/2;
         addSquare (mesh, x, -wy/2, -wz, dx, 0, wz, true, vtxMap);
         addSquare (mesh, x,  wy/2, -wz, dx, 0, wz, false, vtxMap);
      }
      for (int iy=0; iy<ny; iy++) {
         double y = iy*dy-wy/2;
         addSquare (mesh, -wx/2, y, -wz, 0, dy, wz, true, vtxMap);
         addSquare (mesh,  wx/2, y, -wz, 0, dy, wz, false, vtxMap);
      }
      return mesh;
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

      System.out.println ("SurfaceMeshCollider ...");
      FunctionTimer timer = new FunctionTimer();
      timer.start();
      RigidTransform3d T = new RigidTransform3d();
      for (int i=0; i<numTrials; i++) {
         T.R.setRandom();
         mesh1.setMeshToWorld (T);
         //scollider.meshIntersector.findContours (mesh0, mesh1);
         scollider.getContacts (mesh0, mesh1);
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
      testContacts (mesh0, mesh1, null, null, tinfo);
   }

   public void test() {
      simpleBoxTests();
      torusTests();
      singleFaceContourTests();
      perturbedPlateTests();
   }

   private static void printUsageAndExit () {
      System.out.println (
         "Usage: SurfaceMeshIntersectionTest [-timing] [-help] [-test <testFile>]");
      System.exit(1); 
   }

   public static void main (String[] args) {

      String testFile = null;
      boolean testTiming = false;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-help")) {
            printUsageAndExit();
         }
         else if (args[i].equals ("-test")) {
            if (++i == args.length) {
               printUsageAndExit();
            }
            testFile = args[i];
         }
         else if (args[i].equals ("-timing")) {
            testTiming = true;
         }
         else {
            printUsageAndExit();
         }
      }

      RandomGenerator.setSeed (0x1234);      
      SurfaceMeshIntersectorTest tester = new SurfaceMeshIntersectorTest();

      if (testFile != null) {
         try {
            tester.runTestProblem (testFile);
         }
         catch (Exception e) {
            e.printStackTrace(); 
         }
      }
      else if (testTiming) {
         tester.runTimingTest();
      }
      else {
         tester.testFailFileName = "contactTestFail.out";
         tester.runtest();
      }
   }
}
