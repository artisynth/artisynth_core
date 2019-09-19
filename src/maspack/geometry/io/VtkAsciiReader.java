package maspack.geometry.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.ReaderTokenizer;

/**
 * Reads from ascii VTK format
 * http://www.vtk.org/wp-content/uploads/2015/04/file-formats.pdf
 * @author Antonio
 *
 */
public class VtkAsciiReader extends MeshReaderBase {

   public static double DEFAULT_TOLERANCE = 1e-15;
   private double myTol = DEFAULT_TOLERANCE;
   
   public VtkAsciiReader (InputStream is) throws IOException {
      super (is);
   }

   public VtkAsciiReader (File file) throws IOException {
      super (file);
   }

   public VtkAsciiReader (String fileName) throws IOException {
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
      return read(mesh, reader, DEFAULT_TOLERANCE);
   }
   
   public static PolygonalMesh read(PolygonalMesh mesh, Reader reader, double tol) throws IOException {
      
      ReaderTokenizer rtok = new ReaderTokenizer(reader);
      ArrayList<Point3d> nodeList = new ArrayList<Point3d>();
      ArrayList<ArrayList<Integer>> faceList = new ArrayList<ArrayList<Integer>>();
      
      rtok.eolIsSignificant(false);
      
      // read until we find a dataset
      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
          
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (rtok.sval.equalsIgnoreCase("DATASET")) {
               rtok.nextToken();
               String dataType = rtok.sval;
               
               if (dataType.equalsIgnoreCase("POLYDATA")) {
                  parsePolyData(rtok, nodeList, faceList);
                  return buildMesh(mesh, nodeList, faceList);
               } else {
                  System.err.println("Error: unknown dataset type '" + dataType + "'");
               }
            }
         }
         
      }
         
      return null;
      
   }
   
   private static void parsePolyData(ReaderTokenizer rtok, 
      ArrayList<Point3d> nodeList, ArrayList<ArrayList<Integer>> faces)
      throws IOException {
      
      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (rtok.sval.equalsIgnoreCase("POINTS")) {
               // parse points
               nodeList.clear();
               
               // number of points
               int nPnts = rtok.scanInteger();
               nodeList.ensureCapacity(nPnts);
               
               String numericType = rtok.scanWord();
               ignore(numericType); // ignore
               
               double[] vals = new double[3];
               for (int i=0; i<nPnts; i++) {
                  int nRead = rtok.scanNumbers(vals, 3);
                  if (nRead == 3) {
                     nodeList.add(new Point3d(vals));
                  } else {
                     throw new IOException("Not enough digits on line " + rtok.lineno());
                  }
               }
               
               
            } else if (rtok.sval.equalsIgnoreCase("POLYGONS")) {
               faces.clear();
               
               int nPolys = rtok.scanInteger();
               int nRead = 0;
               int nNums = rtok.scanInteger();
               
               for (int i=0; i<nPolys; i++) {
                  int nNodes = rtok.scanInteger();
                  nRead++;
                  ArrayList<Integer> nodeIdxs = 
                     new ArrayList<Integer>(nNodes);
                  for (int j =0; j<nNodes; j++) {
                     nodeIdxs.add(rtok.scanInteger());
                     nRead++;
                  }
                  faces.add(nodeIdxs);
               }
               if (nRead != nNums) {
                  System.err.println("Hmm... we got the wrong number of numbers");
               }
            } else if (rtok.sval.equalsIgnoreCase("TRIANGLE_STRIPS")) {
               faces.clear();
               
               int nStrips = rtok.scanInteger();
               int nRead = 0;
               int nNums = rtok.scanInteger();
               
               for (int i=0; i<nStrips; i++) {
                  int nNodes = rtok.scanInteger();
                  nRead++;
                  ArrayList<Integer> nodeIdxs = 
                     new ArrayList<Integer>(nNodes);
                  for (int j =0; j<nNodes; j++) {
                     nodeIdxs.add(rtok.scanInteger());
                     nRead++;
                  }
                  
                  // add a triangle strip based on nodeIdxs
                  for (int j=2; j<nNodes; ++j) {
                     ArrayList<Integer> face = new ArrayList<>();
                     if ((j%2)==1) {
                        face.add(nodeIdxs.get(j-1));
                        face.add(nodeIdxs.get(j-2));
                        face.add(nodeIdxs.get(j));
                     } else {
                        face.add(nodeIdxs.get(j-2));
                        face.add(nodeIdxs.get(j-1));
                        face.add(nodeIdxs.get(j));
                     }
                     faces.add(face);
                  }
               }
               if (nRead != nNums) {
                  System.err.println("Hmm... we got the wrong number of numbers");
               }
            } else if (rtok.sval.equalsIgnoreCase("POINT_DATA")) {
               // number of points
               int nPnts = rtok.scanInteger();
               // type and info
               String attr = rtok.scanWord();
               if (attr.equalsIgnoreCase("SCALARS")) {
                  String dataName = rtok.scanWord();
                  String dataType = rtok.scanWord();
                  // XXX optional number of components?
                  // int numComp = rtok.scanInteger();
                  // XXX optional lookup-table
                  System.err.println("Ignoring SCALARS");
               } else if (attr.equalsIgnoreCase("NORMALS")) {
                  String dataName = rtok.scanWord();
                  String dataType = rtok.scanWord();
                  for (int j=0; j<nPnts; ++j) {
                     double x = rtok.scanNumber();
                     double y = rtok.scanNumber();
                     double z = rtok.scanNumber();
                     Vector3d normal = new Vector3d(x,y,z);
                  }
                  System.err.println("Ignoring NORMALS");
               }
               
               
               
            } else {
               System.err.println("Unknown heading '" + rtok.sval + "'");
            }
         }
         
         
      }
      
   }
   
   private static void ignore(Object o) {
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

   @Override
   public PolygonalMesh readMesh() throws IOException {
      return (PolygonalMesh)readMesh (new PolygonalMesh());
   }

   public MeshBase readMesh (MeshBase mesh) throws IOException {
      if (mesh == null) {
         mesh = new PolygonalMesh();
      }
      if (mesh instanceof PolygonalMesh) {
         return readMesh ((PolygonalMesh)mesh);
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for '.vtk' files");
      }
   }

   public PolygonalMesh readMesh (PolygonalMesh mesh) throws IOException {
      BufferedReader iread = 
         new BufferedReader (new InputStreamReader(myIstream));
      return read(mesh, iread, myTol);
   }
   
   public static PolygonalMesh read (File file) throws IOException {
      VtkAsciiReader reader = null;
      try {
         reader = new VtkAsciiReader (file);
         return (PolygonalMesh)reader.readMesh (new PolygonalMesh());
      }
      catch (Exception e) {
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
