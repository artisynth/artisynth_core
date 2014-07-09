package maspack.geometry.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedReader;
import java.util.ArrayList;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshBase;
import maspack.matrix.Point3d;
import maspack.util.ReaderTokenizer;

/**
 * Reads from ascii STL format
 * @author Antonio
 *
 */
public class StlReader extends MeshReaderBase {

   public static double DEFAULT_TOLERANCE = 1e-15;
   double myTol = DEFAULT_TOLERANCE;
   
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
   
   public static PolygonalMesh read(PolygonalMesh mesh, Reader reader) throws IOException {
      
      return read(null, reader, DEFAULT_TOLERANCE);
   }
   
   public static PolygonalMesh read(PolygonalMesh mesh, Reader reader, double tol) throws IOException {
      
      ReaderTokenizer rtok = new ReaderTokenizer(reader);
      ArrayList<Point3d> nodeList = new ArrayList<Point3d>();
      ArrayList<ArrayList<Integer>> faceList = new ArrayList<ArrayList<Integer>>();
      
      rtok.eolIsSignificant(true);
      
      String solidName = "";
      
      // read until we find "solid"
      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            String word = rtok.sval.toLowerCase();
            if (word.equals("solid")) {
               rtok.nextToken();
               
               if (rtok.ttype == ReaderTokenizer.TT_WORD) {
                  solidName = rtok.sval;
               }
               toEOL(rtok);
            } else if (word.equals("facet")) {
               ArrayList<Integer> face = readFace(rtok, nodeList, tol);
               if (face != null) {
                  faceList.add(face);
               }
            } else if (word.equals("endsolid") || word.equals("end")) {
               
               boolean setMeshName = true;
               if (mesh != null) {
                  setMeshName = false;
               }
               mesh = buildMesh(mesh, nodeList,faceList);
               
               if (setMeshName) {
                  mesh.setName(solidName);
               }
                  
               return mesh;
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
      if (!word.toLowerCase().equals("normal")) {
         throw new IOException("Expecting a normal on line " + rtok.lineno());
      }
      
      // read and discard normals
      double[] vals = new double[3];
      int n = rtok.scanNumbers(vals, 3);
      toEOL(rtok);
      
      // expecting "outer loop"
      String line = readLine(rtok);
      if (!line.toLowerCase().trim().equals("outer loop")) {
         throw new IOException("Expecting 'outer loop' on line " + rtok.lineno());
      }
      
      word = rtok.scanWord();
      while (word.toLowerCase().equals("vertex") && rtok.ttype != ReaderTokenizer.TT_EOF) {
    
         n = rtok.scanNumbers(vals, 3);
         if (n != 3) {
            throw new IOException("Invalid vertex on line " + rtok.lineno());
         }
         
         int idx = findOrAddNode(new Point3d(vals), nodes, tol);
         faceNodes.add(idx);
         
         toEOL(rtok);
         word = rtok.scanWord();
      }
      
      //endloop
      if (!word.toLowerCase().equals("endloop")) {
         throw new IOException("Expected 'endloop' on line " + rtok.lineno());
      }
      toEOL(rtok);
      
      // endfacet
      word = rtok.scanWord();
      if (!word.toLowerCase().equals("endfacet")) {
         throw new IOException("Expected 'endfacet' on line " + rtok.lineno());
      }
      toEOL(rtok);
      
      return faceNodes;
   }
   
   private static int findOrAddNode(Point3d pos, ArrayList<Point3d> nodes, double tol) {
      
      for (int i=0; i<nodes.size(); i++){
         if (nodes.get(i).distance(pos) < tol) {
            return i;
         }
      }
      nodes.add(pos);
      return nodes.size()-1;
   }
   
   private static void toEOL(ReaderTokenizer rtok) throws IOException {
      while (rtok.ttype != ReaderTokenizer.TT_EOL &&  
         rtok.ttype != ReaderTokenizer.TT_EOF) {
         rtok.nextToken();
      }
      if (rtok.ttype == ReaderTokenizer.TT_EOF) {
         rtok.pushBack();
      }
   }
   
   private static String readLine(ReaderTokenizer rtok) throws IOException {

      Reader rtokReader = rtok.getReader();
      String line = "";
      int c;
      while (true) {
         c = rtokReader.read();

         if (c < 0) {
            rtok.ttype = ReaderTokenizer.TT_EOF;
            return line;
         }
         else if (c == '\n') {
            rtok.setLineno(rtok.lineno() + 1); // increase line number
            rtok.ttype = ReaderTokenizer.TT_EOL;
            break;
         }
         line += (char)c;
      }

      return line;
   }

   public PolygonalMesh readMesh (MeshBase mesh) throws IOException {
      if (mesh == null) {
         mesh = new PolygonalMesh();
      }
      if (mesh instanceof PolygonalMesh) {
         BufferedReader iread = 
            new BufferedReader (new InputStreamReader(myIstream));
         return read((PolygonalMesh)mesh, iread, myTol);
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported by this reader");
      }
   }
   
   public static PolygonalMesh read (File file) throws IOException {
      StlReader reader = new StlReader (file);
      return (PolygonalMesh)reader.readMesh (null);
    }

   public static PolygonalMesh read (String fileName) throws IOException {
      return read (new File(fileName));
    }

}
