package artisynth.core.femmodels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import maspack.geometry.io.MeshWriter;
import maspack.matrix.Point3d;
import maspack.util.ReaderTokenizer;

/**
 * Reads from ascii vtk format
 * @author Antonio
 *
 */
public class VtkAsciiReader implements FemReader {

   protected InputStream myIstream;
   protected File myFile;

   public VtkAsciiReader (InputStream is) throws IOException {
      myIstream = is;
   }

   public VtkAsciiReader (File file) throws IOException {
      this(new FileInputStream(file));
      myFile = file;
   }

   public VtkAsciiReader (String fileName) throws IOException {
      this (new File(fileName));
   }
   
   public static FemModel3d read(FemModel3d model, String fileName) throws IOException {
      BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
      return read(model, reader);
   }
   
   public static FemModel3d read(FemModel3d model, Reader reader) throws IOException {

      ReaderTokenizer rtok = new ReaderTokenizer(reader);
      ArrayList<Point3d> nodeList = new ArrayList<Point3d>();
      ArrayList<ArrayList<Integer>> elemList = new ArrayList<ArrayList<Integer>>();

      rtok.eolIsSignificant(false);

      // read until we find a dataset
      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {

         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (rtok.sval.equalsIgnoreCase("DATASET")) {
               rtok.nextToken();
               String dataType = rtok.sval;

               if (dataType.equalsIgnoreCase("UNSTRUCTURED_GRID")) {
                  parseFemData(rtok, nodeList, elemList);
                  return buildFem(model, nodeList, elemList);
               } else {
                  System.err.println("Error: unknown dataset type '" + dataType + "'");
               }
            }
         }

      }

      return null;

   }

   private static void parseFemData(ReaderTokenizer rtok, 
      ArrayList<Point3d> nodeList, ArrayList<ArrayList<Integer>> elems)
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


            } else if (rtok.sval.equalsIgnoreCase("CELLS")) {
               elems.clear();

               int nCells = rtok.scanInteger();
               int nRead = 0;
               int nNums = rtok.scanInteger();

               for (int i=0; i<nCells; i++) {
                  int nNodes = rtok.scanInteger();
                  nRead++;
                  ArrayList<Integer> nodeIdxs = 
                     new ArrayList<Integer>(nNodes);
                  for (int j =0; j<nNodes; j++) {
                     nodeIdxs.add(rtok.scanInteger());
                     nRead++;
                  }
                  elems.add(nodeIdxs);
               }
               if (nRead != nNums) {
                  System.err.println("Hmm... we got the wrong number of numbers");
               }
            } else if (rtok.sval.equalsIgnoreCase("CELL_TYPES")) {
               int nCells = rtok.scanInteger();
               // discard types
               for (int i=0; i<nCells; i++) {
                  int type = rtok.scanInteger();
               }
            } else {
               System.err.println("Unknown heading '" + rtok.sval + "'");
            }
         }


      }

   }

   private static void ignore(Object o) {
   }

   private static FemModel3d buildFem(FemModel3d model, ArrayList<Point3d> nodes, ArrayList<ArrayList<Integer>> elems) {

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }

      for (int i=0; i<nodes.size(); i++) {
         model.addNode(new FemNode3d(nodes.get(i)));
      }

      ArrayList<Integer> elem;
      for (int i=0; i<elems.size(); i++) {
         elem = elems.get(i);
         FemNode3d[] elemNodes = new FemNode3d[elem.size()];
         for (int j=0; j<elem.size(); j++) {
            elemNodes[j] = model.getNode(elem.get(j));
         }
         
         // pyramid, swap node order
         switch (elemNodes.length) {
            case 4:  // tet
               
               break;
            case 5: {
               // pyramid reverse face orientation
               FemNode3d n = elemNodes[0];
               elemNodes[0] = elemNodes[3];
               elemNodes[3] = n;
               n = elemNodes[1];
               elemNodes[1] = elemNodes[2];
               elemNodes[2] = n;
               break;
            }
            default:
               // hex, wedge
               for (int j=0; j<elemNodes.length/2; ++j) {
                  FemNode3d n = elemNodes[j];
                  elemNodes[j] = elemNodes[elemNodes.length-1-j];
                  elemNodes[elemNodes.length-1-j] = n;
               }
         }
         
         FemElement3d e = FemElement3d.createElement(elemNodes, true); 
         model.addElement(e);
         e.computeVolumes();
         if (e.getVolume() < 0.0) {
             System.out.println("Warning: inverted element " + e.getClass());
         }
      }


      return model;
   }

   public FemModel3d readMesh (FemModel3d mesh) throws IOException {
      if (mesh instanceof FemModel3d) {
         return readFem ((FemModel3d)mesh);
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported by this reader");
      }
   }

   @Override
   public FemModel3d readFem (FemModel3d mesh) throws IOException {
      BufferedReader iread = 
         new BufferedReader (new InputStreamReader(myIstream));
      return read(mesh, iread);
   }

   public static FemModel3d read (File file) throws IOException {
      VtkAsciiReader reader = new VtkAsciiReader (file);
      return (FemModel3d)reader.readFem (new FemModel3d());
   }

   public static FemModel3d read (String fileName) throws IOException {
      return read (new File(fileName));
   }

   private void closeQuietly(InputStream in) {
      if (in != null) {
         try {
            in.close();
         } catch (IOException e) {}
      }
   }
   
   public void close() {
      closeQuietly(myIstream);
   }

   @Override
   protected void finalize() throws Throwable {
      super.finalize();
      close();
   }

}
