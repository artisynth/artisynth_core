package maspack.collision;

import java.util.ArrayList;

// import maspack.geometry.AjlAabbNode;
// import maspack.geometry.AjlBvNode;
// import maspack.geometry.AjlObbNode;
// import maspack.geometry.AjlObbTree;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;

public class CollisionMetrics {
   public String name;
   public long totalTime = 0, cullTime = 0, traceTime = 0, scanTime = 0,
   walkTime = 0, regionTime = 0, rigidTime = 0, femTime = 0,
   nContactRegions = 0, nCpps = 0, bvTime = 0;
   public int primInts = 0, isDjCalls = 0, getContactsCalls = 0,
   leafIntersects = 0, openContours = 0, openHalfContours = 0;

   long reportTime = 2000000000L; // 2 seconds, in nanoseconds
   long elapsedRealTime;
   boolean started = false;
   boolean timeReported = false;

   public CollisionMetrics (String nm) {
      name = nm;
   }

   void report (ContactInfo info) {
//      // if (info != null) histogramDepths(info.regions);
//      if (info != null) {
//         if (info.regions != null)
//            nContactRegions += info.regions.size();
//         if (info.points0 != null)
//            nCpps += info.points0.size();
//         if (info.points1 != null)
//            nCpps += info.points1.size();
//      }
//      long time = System.nanoTime();
//      totalTime += time;
//      if (!timeReported) {
//         // if (Main.getScheduler().getTime() > reportTime) {
//         //    elapsedRealTime += System.nanoTime();
//         //    reportTime();
//         // }
//      }
   }

   void reportTime() {
      float e = 1f / elapsedRealTime; // 1.0f/1e9f;
      System.out.printf (
         // maxBvVol=%7.1e
         "%n" + name + 
         " sim=%6.3f c=%4.2f nc=%4.2f cull=%6.2f scn=%6.2f trc=%6.2f " +
         "wlk=%6.2f rgn=%6.2f rig=%6.2f fem=%6.2f%n",
         ((float)elapsedRealTime / (float)reportTime),
         ((float)totalTime * e), ((float)(elapsedRealTime - totalTime) * e),
         cullTime * e, scanTime * e, traceTime * e, walkTime * e, regionTime
         * e, rigidTime * e, femTime * e);
      System.out.printf (
         "primInt=%7.1e gcCall=%d djCall=%d lfInt=%d ncr=%d ncpp=%d bv=%6.2f%n",
         (float)primInts, getContactsCalls, isDjCalls, leafIntersects,
         nContactRegions, nCpps, ((float)bvTime) * e);
      System.out.println (
         "computedFaceNormals=" + PolygonalMesh.computedFaceNormals);
      // reportFaceHist();
      // reportEdgeHist();
      // System.out.println("totalEdges="+AjlObbTree.totalEdges);
      // reportSepAxisHist();
      // reportBvVols();
      // reportRegionDepths();
      // reportDepthHists();
      timeReported = true;
      System.out.println (
         "open half contours=" + openHalfContours +
         " open contours=" + openContours);
   }

   public int[] faceHist = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

   void reportFaceHist() {
      int totFace = 0;
      for (int i = 0; i < faceHist.length; i++) {
         System.out.println (faceHist[i] + " with " + i + " faces");
         totFace += faceHist[i] * i;
      }
      System.out.println ("totFaces=" + totFace);
   }

   public int[] edgeHist = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

   void reportEdgeHist() {
      int totEdge = 0;
      for (int i = 0; i < edgeHist.length; i++) {
         System.out.println (edgeHist[i] + " with " + i + " edges");
         totEdge += edgeHist[i] * i;
      }
      System.out.println ("totEdges=" + totEdge);
   }

   public int[] regionDepths;
   public float regionDepthFactor = 200.0f;

   public void histogramDepths (ArrayList<ContactPlane> regions) {
      if (regionDepths == null) {
         regionDepths = new int[100];
         for (int i = 0; i < regionDepths.length; i++)
            regionDepths[i] = 0;
      }
      if (regions != null) {
         for (ContactPlane r : regions) {
            // System.out.println("depth"+r.depth);
            if (r.depth < 0)
               throw new RuntimeException ("negative depth");
            regionDepths[(int)Math.round (r.depth * regionDepthFactor)] += 1;
         }
      }
   }

   public void reportRegionDepths() {
      System.out.println ("");
      System.out.println ("region depths:");
      for (int i = 0; i < regionDepths.length; i++) {
         System.out.printf (
            "%6.3f %d%n", ((float)i) / regionDepthFactor, regionDepths[i]);
      }
   }

   public int depthHistSize = 12;
   public int[][] depthDisjHist = new int[depthHistSize][depthHistSize];
   public int[][] depthOvlpHist = new int[depthHistSize][depthHistSize];

   public void reportDepthHists() {
      System.out.println ("");
      System.out.println ("disjoint");
      reportDepthHist (depthDisjHist);
      System.out.println ("");
      System.out.println ("overlapping");
      reportDepthHist (depthOvlpHist);
      System.out.println ("");
   }

   public void reportDepthHist (int[][] hist) {
      for (int i = 0; i < depthHistSize; i++) {
         for (int j = 0; j < depthHistSize; j++) {
            System.out.printf ("%8d", hist[i][j]);
         }
         System.out.println ("");
      }
   }

   public int[] bvVols;
   public float bvVolFactor = 40.0f;

   public void histogramBvVol (double vol) {
      if (bvVols == null) {
         bvVols = new int[50];
         for (int i = 0; i < bvVols.length; i++)
            bvVols[i] = 0;
      }
      bvVols[Math.min (bvVols.length - 1, (int)Math.round (vol * bvVolFactor))] +=
         1;
   }

   public void reportBvVols() {
      System.out.println ("");
      System.out.println ("bv vols:");
      for (int i = 0; i < bvVols.length; i++) {
         System.out.printf ("%6.3f %d%n", ((float)i) / bvVolFactor, bvVols[i]);
      }
   }

   public int[] sepAxisHist =
      { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

   public void reportSepAxisHist() {
      System.out.println ("");
      System.out.println ("sep axis:");
      for (int i = 0; i < sepAxisHist.length; i++) {
         System.out.println (i + " " + sepAxisHist[i]);
      }
   }
}
