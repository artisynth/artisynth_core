package maspack.geometry.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.HashMap;

import maspack.geometry.AABBTree;
import maspack.geometry.BVNode;
import maspack.geometry.Boundable;
import maspack.geometry.MeshBase;
import maspack.geometry.io.MeshWriter.DataFormat;
// import maspack.geometry.KDTree3d;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.FunctionTimer;
import maspack.util.ReaderTokenizer;

/**
 * Reads from ascii STL format
 * @author Antonio
 *
 */
public class StlReader extends MeshReaderBase {

   public static double DEFAULT_TOLERANCE = 1e-15;
   double myTol = DEFAULT_TOLERANCE;
   DataFormat myDataFormat = null; // will be filled in when the mesh is read

   static class HashedPoint3d extends Point3d {

      int myHashCode;

      HashedPoint3d (double x, double y, double z) {
         super (x, y, z);
         myHashCode = computeHashCode();
      }

      @Override 
      public boolean equals (Object obj) {
         if (obj instanceof HashedPoint3d) {
            HashedPoint3d hpnt = (HashedPoint3d)obj;
            return equals (hpnt);
         }
         else {
            return false;
         }
      }

      @Override 
      public int hashCode() {
         return myHashCode;
      }

   }

   public StlReader (InputStream is) throws IOException {
      super (is);
   }

   public StlReader (File file) throws IOException {
      super (file);
  }

   public StlReader (String fileName) throws IOException {
      this (new File(fileName));
   }

   /**
    * Sets tolerance to use when merging vertices
    */
   public void setTolerance(double tol) {
      myTol = tol;
   }
   
   /**
    * Gets tolerance to use when merging vertices
    */
   public double getTolerance() {
      return myTol;
   }

   /**
    * Gets the data format that was determined for the file. Will be null until
    * the file is read.
    */
   public DataFormat getDataFormat() {
      return myDataFormat;
   }

   /**
    * Tries to determine if the file is ascii or binary. Ideally, ascii files
    * start with the keyword "solid", but some binary files do as well.  So we
    * read (up to) the first 1000 bytes and look for (a) all ascii characters,
    * and (b) the keyword "facet".
    */
   private static boolean isAscii (InputStream is) throws IOException {
      boolean ascii = true;
      int TESTSIZE = 1000;
      is.mark (TESTSIZE);
      byte[] bbuf = new byte[TESTSIZE];
      int nr = is.read (bbuf, 0, TESTSIZE);

      String str = (new String(bbuf));
      if (!str.startsWith ("solid") ||
          !str.contains ("facet")) {
         ascii = false;
      }
      if (ascii) {
         // extra check for non-ascii characters
         for (int i=0; i<nr; i++) {
            if (bbuf[i] < 0) {
               // non-ascii character
               ascii = false;
               break;
            }
         }
      }
      is.reset ();
      return ascii;
   }
   
//   public static PolygonalMesh read(PolygonalMesh mesh, Reader reader) throws IOException {
//      
//      return read(null, reader, DEFAULT_TOLERANCE);
//   }
   
//   public static PolygonalMesh read(PolygonalMesh mesh, Reader reader, double tol) throws IOException {
   public static PolygonalMesh read(
      PolygonalMesh mesh, InputStream is, double tol) throws IOException {
      
      if (isAscii (is)) {
         BufferedReader iread = 
            new BufferedReader (new InputStreamReader(is));         
         return readASCII(mesh, iread, tol);
      }
      else {
         return readBinary(mesh, is, tol);
      }
   }
   
   public static PolygonalMesh readBinary(
      PolygonalMesh mesh, InputStream is, double tol) throws IOException {
      boolean _printDebug = false;
      // Byte ordering is assumed to be Little Endian (see wikipedia on STL format).
      // Format of binary STL is 
      // 80 byte header (skip)
      // 4 byte int indicating num facets to follow
      is.skip (80);
      byte[] bbuf = new byte[4];
      is.read(bbuf,0,4);
      
      // This is a simple way to read unsigned long from 4 bytes (LittleEndian)
      // There is no other method for reading unsigned 4-byte Int with ByteBuffer!
      long numFacets = 0;
      numFacets |= bbuf[3] & 0xFF;
      numFacets <<= 8;
      numFacets |= bbuf[2] & 0xFF;
      numFacets <<= 8;
      numFacets |= bbuf[1] & 0xFF;
      numFacets <<= 8;
      numFacets |= bbuf[0] & 0xFF;

      if (_printDebug) {
         System.out.println("Num facets: "+ numFacets);
      }
      
      ArrayList<Point3d> nodeList = new ArrayList<Point3d>();
      ArrayList<ArrayList<Integer>> faceList = new ArrayList<ArrayList<Integer>>();

      if (_printDebug) {
         System.out.print("Reading file... ");
      }
      // For big files, it is slightly faster to read one facet
      // at a time than the whole file at once (for some reason).
      long start = System.nanoTime ();
      int facetSize = 50;
      bbuf = new byte[facetSize];
      
      ArrayList<Point3d> allPoints = new ArrayList<Point3d>(3*(int)numFacets);
      ArrayList<ArrayList<Integer>> allFaces = new ArrayList<ArrayList<Integer>>((int)numFacets);

      {
         int idx = 0;
         for (long i=0; i<numFacets; i++) {
            int nBytesRead = is.read(bbuf,0,facetSize);
            if (nBytesRead < facetSize) {
               throw new IOException ("Invalid STL file detected! (non-matching size)");
            }
            ByteBuffer bb = ByteBuffer.wrap(bbuf);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            
            // Ignore normal
            bb.getFloat ();
            bb.getFloat ();
            bb.getFloat ();
            
            ArrayList<Integer> face = new ArrayList<Integer>();
            // Read all 3 vertices
            double[] vals = new double[3];
            for (int j=0; j<3; j++) {
               vals[0] = bb.getFloat();
               vals[1] = bb.getFloat();
               vals[2] = bb.getFloat();
               Point3d pnt;
               pnt = new Point3d(vals);
               allPoints.add (pnt);
               face.add (idx++);
            }
            allFaces.add (face);
            bb.getShort (); // Attribute byte count should = 0
         }
      }
      
      //      if (_printDebug) {
      //         System.out.println ("("+1.e-9*(System.nanoTime()-start)+")");
      //         System.out.print ("Building spatial hash table... ");
      //         start = System.nanoTime ();
      //      }
      //      
      //      SpatialHashTable<Point3d> table = new SpatialHashTable<Point3d>(tol);
      //      table.setup (allPoints, allPoints);
      
      //      if (_printDebug) {
      //         System.out.println ("("+1.e-9*(System.nanoTime()-start)+")");
      //         System.out.print ("Scanning for unique verts... ");
      //         start = System.nanoTime ();
      //      }
      //      
      //      HashMap<Point3d, Integer> allToUniqueMap = new HashMap<Point3d, Integer> (allPoints.size());
      //      double tolSq = tol*tol;
      //      for (Point3d pnt : allPoints) {
      //         if (allToUniqueMap.containsKey (pnt)) {
      //            continue;
      //         }
      //         
      //         // Find all points within tol of pnt
      //         List<Point3d> results = new ArrayList<Point3d>(); 
      //         List<Point3d> cell = table.getElsNear (pnt);//table.getCellsNearOld (pnt);
      //         //while (it.hasNext ()) {
      //         //   List<Point3d> cell = it.next ();
      //         //   if (cell == null) 
      //         //      continue;
      //         if (cell != null) {
      //            for (Point3d neighbour : cell) {
      //               if (neighbour.distanceSquared (pnt) < tolSq) {
      //                  results.add (neighbour);
      //               }
      //            }
      //         }
      //         int idx = nodeList.size();
      //         nodeList.add (pnt);
      //         for (Point3d neighbour : results) {
      //            allToUniqueMap.put (neighbour, idx);
      //         }
      //      }
      //      
      //      if (_printDebug) {
      //         System.out.println ("("+1.e-9*(System.nanoTime()-start)+")");
      //         System.out.print ("Building faceList... ");
      //         start = System.nanoTime ();
      //
      //      }
      //
      //      // Build face list by looking up the index of the Unique vert through hashmap.
      //      for (ArrayList<Integer> face : allFaces) {
      //         ArrayList<Integer> faceNodes = new ArrayList<Integer>(3);
      //         for (int i=0; i<3; i++) {
      //            int idx = allToUniqueMap.get (allPoints.get (face.get(i)));
      //            faceNodes.add(idx);
      //         }
      //         
      //         faceList.add (faceNodes);
      //      }
      //      
      if (_printDebug) {
         System.out.println ("("+1.e-9*(System.nanoTime()-start)+")");
         System.out.print ("merging nodes binary... ");
         start = System.nanoTime ();
      }
      
      // XXX tested 2x faster than spatial hashmap
      nodeList.addAll (allPoints);
      faceList.addAll (allFaces);
      mergeNearbyNodes (nodeList, faceList, tol);
      if (_printDebug) {
         System.out.println ("("+1.e-9*(System.nanoTime()-start)+")");
         System.out.print ("building mesh... ");
         start = System.nanoTime ();
      }
      mesh = buildMesh(mesh, nodeList, faceList);
      
      if (_printDebug) {
         System.out.println ("("+1.e-9*(System.nanoTime()-start)+")");
         System.out.println("Done!");
         System.out.println("Unique verts: " + nodeList.size ());
         System.out.println("Unique faces: " + allFaces.size ());
      }

         
      return mesh;
      
   }
   
   public static PolygonalMesh readASCII(
      PolygonalMesh mesh, Reader reader, double tol) throws IOException {
      ReaderTokenizer rtok = new ReaderTokenizer(reader);
      ArrayList<Point3d> nodeList = new ArrayList<Point3d>();
      ArrayList<ArrayList<Integer>> faceList = new ArrayList<ArrayList<Integer>>();
      boolean _printDebug = false;
      rtok.eolIsSignificant(true);
      
      String solidName = "";

      if (_printDebug) {
         System.out.print("Reading file... ");
      }
      long start = System.nanoTime ();

      // read until we find "solid"
      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            String word = rtok.sval.toLowerCase();
            switch(word) {
               case "solid": {
                  rtok.nextToken();
                  if (rtok.ttype == ReaderTokenizer.TT_WORD) {
                     solidName = rtok.sval;
                  }
                  toEOL(rtok);
                  break;
               }
               case "facet": {
                  ArrayList<Integer> face = readFace(rtok, nodeList, tol);
                  if (face != null) {
                     faceList.add(face);
                  }
                  break;
               }
               case "endsolid":
               case "end": {
                  boolean setMeshName = true;
                  if (mesh != null) {
                     setMeshName = false;
                  }
               
                  if (_printDebug) {
                     System.out.println ("("+1.e-9*(System.nanoTime()-start)+")");
                     System.out.print ("merging nodes... ");
                     start = System.nanoTime ();
                  }
                  mergeNearbyNodes (nodeList, faceList, tol);
                  if (_printDebug) {
                     System.out.println ("("+1.e-9*(System.nanoTime()-start)+")");
                     System.out.print ("building mesh... ");
                     start = System.nanoTime ();
                  }
                  mesh = buildMesh(mesh, nodeList, faceList);
                  if (_printDebug) {
                     System.out.println ("("+1.e-9*(System.nanoTime()-start)+")");
                     System.out.println("Done!");
                     System.out.println("Unique verts: " + nodeList.size ());
                     System.out.println("Unique faces: " + faceList.size ());
                  }
               
                  if (setMeshName) {
                     mesh.setName(solidName);
                  }
                  return mesh;
               }
            } 
         }
      }
      
      return null;
      
   }
   
   private static PolygonalMesh buildMesh(PolygonalMesh mesh, ArrayList<Point3d> nodes, ArrayList<ArrayList<Integer>> faces) {

      if (mesh == null) {
         mesh = new PolygonalMesh();
      } else {
         mesh.clear();
      }
      
      Point3d[] pnts = new Point3d[nodes.size()];
      int[][] faceIndices = new int[faces.size()][];
      for (int i=0; i<nodes.size(); i++) {
         pnts[i] = nodes.get(i);
      }
      
      ArrayList<Integer> face;
      for (int i=0; i<faces.size(); i++) {
         face = faces.get(i);
         faceIndices[i] = new int[face.size()];
         for (int j=0; j<face.size(); j++) {
            faceIndices[i][j] = face.get(j);
         }
      }
      mesh.set(pnts, faceIndices);
      
      return mesh;
   }
   
   private static ArrayList<Integer> readFace(ReaderTokenizer rtok, ArrayList<Point3d> nodes, double tol) throws IOException {
      
      ArrayList<Integer> faceNodes = new ArrayList<Integer>(3); 
      
      String word = rtok.scanWord();
      if (!word.equalsIgnoreCase("normal")) {
         throw new IOException("Expecting a normal on line " + rtok.lineno());
      }
      
      // read and discard normals
      double[] vals = new double[3];
      int n = rtok.scanNumbers(vals, 3);
      toEOL(rtok);
      
      // expecting "outer loop"
      String line = rtok.readLine();
      if (!line.trim().equalsIgnoreCase("outer loop")) {
         throw new IOException("Expecting 'outer loop' on line " + rtok.lineno());
      }
      
      word = rtok.scanWord();
      while (word.equalsIgnoreCase("vertex") && rtok.ttype != ReaderTokenizer.TT_EOF) {
    
         n = rtok.scanNumbers(vals, 3);
         if (n != 3) {
            throw new IOException("Invalid vertex on line " + rtok.lineno());
         }
         
         int idx = nodes.size ();
         nodes.add (new Point3d(vals));
         faceNodes.add(idx);
         
         toEOL(rtok);
         word = rtok.scanWord();
      }
      
      //endloop
      if (!word.equalsIgnoreCase("endloop")) {
         throw new IOException("Expected 'endloop' on line " + rtok.lineno());
      }
      toEOL(rtok);
      
      // endfacet
      word = rtok.scanWord();
      if (!word.equalsIgnoreCase("endfacet")) {
         throw new IOException("Expected 'endfacet' on line " + rtok.lineno());
      }
      toEOL(rtok);
      
      return faceNodes;
   }
   
   private static class PointBoundable implements Boundable {

      Point3d pnt;
      int idx;
      
      public PointBoundable(Point3d pnt, int idx) {
         this.pnt = pnt;
         this.idx = idx;
      }
      
      @Override
      public int numPoints () {
         return 1;
      }

      @Override
      public Point3d getPoint (int idx) {
         return pnt;
      }

      @Override
      public void computeCentroid (Vector3d centroid) {
         centroid.set (pnt);
      }

      @Override
      public void updateBounds (Vector3d min, Vector3d max) {
         pnt.updateBounds (min, max);
      }

      @Override
      public double computeCovariance (Matrix3d C) {
         return -1;
      }
   }
   
   private static void mergeNearbyNodesOld (
      ArrayList<Point3d> nodes, ArrayList<ArrayList<Integer>> faces, double tol) {
      
      // build bounding volume tree if points
      AABBTree tree = new AABBTree ();
      tree.setMargin (tol);
      tree.setMaxLeafElements (4);
      ArrayList<PointBoundable> nb = new ArrayList<>(nodes.size ());
      for (int i=0; i<nodes.size (); ++i) {
         nb.add (new PointBoundable (nodes.get (i), i));
      }
      tree.build (nb);
      
      // map each point to nearest
      int[] idxmap = new int[nodes.size ()];
      for (int i=0; i<idxmap.length; ++i) {
         idxmap[i] = -1;
      }
      
      ArrayList<BVNode> bvnodes = new ArrayList<>();
      ArrayList<Point3d> npoints = new ArrayList<>();
      
      int nidx = 0;
      for (int i=0; i<idxmap.length; ++i) {
         if (idxmap[i] < 0) {
            idxmap[i] = nidx;
            
            Point3d p = nodes.get (i);
            npoints.add (new Point3d(p));
            
            // find other nearby nodes
            bvnodes.clear ();
            tree.intersectPoint (bvnodes, p);
            for (BVNode node : bvnodes) {
               for (Boundable b : node.getElements ()) {
                  PointBoundable pb = (PointBoundable)b;
                  // mark any nearby nodes
                  if (idxmap[pb.idx] < 0) {
                     if (pb.pnt.distance (p) < tol) {
                        idxmap[pb.idx] = nidx;
                     }
                  } // unmarked
               } // boundables
            } // nodes
            nidx++;  // next new node
         } // unmarked
      } // nodes
      
      nodes.clear ();
      nodes.addAll (npoints);
      
      for (ArrayList<Integer> face : faces) {
         for (int j=0; j<face.size (); ++j) {
            face.set (j, idxmap[face.get (j)]);
         }
      }
      
   }
   
   private static void mergeNearbyNodes (
      ArrayList<Point3d> nodes, ArrayList<ArrayList<Integer>> faces, double tol) {
      
      float loadFactor = 0.75f;
      int initialCapacity = (int)(nodes.size()/loadFactor);
      LinkedHashMap<HashedPoint3d,Integer> unodes =
         new LinkedHashMap<>(initialCapacity, loadFactor);

      // map each point to nearest
      int[] idxmap = new int[nodes.size ()];
      int idx = 0;
      for (int i=0; i<nodes.size(); i++) {
         Point3d n = nodes.get(i);
         HashedPoint3d p = new HashedPoint3d (n.x, n.y, n.z);
         Integer ival = unodes.get (p);
         if (ival == null) {
            ival = idx;
            unodes.put (p, idx++);
         }
         idxmap[i] = ival;
      }
      nodes.clear ();
      for (Point3d p : unodes.keySet()) {
         nodes.add (p);
      }
      for (ArrayList<Integer> face : faces) {
         for (int j=0; j<face.size (); ++j) {
            face.set (j, idxmap[face.get (j)]);
         }
      }
      
   }
   
   //   private static int findOrAddNode(Point3d pos, ArrayList<Point3d> nodes, double tol) {
   //      
   //      for (int i=0; i<nodes.size(); i++){
   //         if (nodes.get(i).distance(pos) < tol) {
   //            return i;
   //         }
   //      }
   //      nodes.add(pos);
   //      return nodes.size()-1;
   //   }
   
   private static void toEOL(ReaderTokenizer rtok) throws IOException {
      while (rtok.ttype != ReaderTokenizer.TT_EOL &&  
         rtok.ttype != ReaderTokenizer.TT_EOF) {
         rtok.nextToken();
      }
      if (rtok.ttype == ReaderTokenizer.TT_EOF) {
         rtok.pushBack();
      }
   }
   
   @Override
   public PolygonalMesh readMesh() throws IOException {
      return (PolygonalMesh)readMesh (new PolygonalMesh());
   }

   public PolygonalMesh readMesh (MeshBase mesh) throws IOException {
      if (mesh == null) {
         mesh = new PolygonalMesh();
      }
      if (mesh instanceof PolygonalMesh) {
         FunctionTimer timer = null; // new FunctionTimer();
         if (timer != null) {
            timer.start();
         }
         InputStream is = new BufferedInputStream(myIstream);
         PolygonalMesh pmesh = (PolygonalMesh)mesh;
         if (isAscii (is)) {
            BufferedReader iread = 
               new BufferedReader (new InputStreamReader(is));
            myDataFormat = DataFormat.ASCII;
            pmesh = readASCII(pmesh, iread, myTol);
         } 
         else {
            myDataFormat = DataFormat.BINARY_LITTLE_ENDIAN;
            pmesh = readBinary(pmesh, is, myTol);
         }        
         if (timer != null) {
            timer.stop();
            System.out.println ("Stl read in " + timer.result(1));
         }
         return pmesh;
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for '.stl' files");
      }
   }
   
   public static PolygonalMesh read (File file) throws IOException {
      StlReader reader = null;
      try {
         reader = new StlReader (file);
         return (PolygonalMesh)reader.readMesh (null);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (reader != null) {
            reader.close();
         }
      }      
    }

   public static PolygonalMesh read (String fileName) throws IOException {
      return read (new File(fileName));
    }

}
