/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.ReaderTokenizer;

/**
 * A class to read an FEM described as Unstructured Cell Data with a
 * uniform density. The UCD data format is described in Appendix E
 * of the AVS Developer's Guide.
 */
public class UCDReader implements FemReader {

   private File myFile;
   
   public UCDReader(String filename) {
      myFile = new File(filename);
   }
   
   @Override
   public FemModel3d readFem(FemModel3d fem) throws IOException {
      return read(fem, myFile.getAbsolutePath(), -1);
   }
   
   /** 
    * Creates an FemModel with uniform density based on UCD data
    * contained in a specified file.
    * 
    * @param model FEM model to be populated by UCD data
    * @param fileName path name of the UCD file
    * @param density density of the model
    * @throws IOException if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d model, String fileName, double density)
      throws IOException {
      
      return read (model, fileName, density, null);
   }

   /** 
    * Creates an FemModel with uniform density based on UCD data
    * contained in a specified file. The node coordinate data
    * is scaled by a scale factor.
    * 
    * @param model FEM model to be populated by UCD data
    * @param fileName path name of the UCD file
    * @param density density of the model
    * @param scale factor by which node coordinate data should be scaled
    * @throws IOException if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d model, String fileName, double density, double scale)
      throws IOException {
      
      return read (model, fileName, density,
                   new Vector3d (scale, scale, scale));
   }


   /** 
    * Creates an FemModel with uniform density based on UCD data
    * contained in a specified file. The node coordinate data
    * can be scaled non-uniformly using an optional
    * parameter giving scale values about the x, y, and z axes.
    * 
    * @param model FEM model to be populated by UCD data
    * @param fileName path name of the UCD file
    * @param density density of the model
    * @param scale if non-null, gives scaling about the x, y, and z axes
    * @throws IOException if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d model, String fileName, double density, Vector3d scale)
      throws IOException {

      Reader reader = new FileReader (fileName);
      model = read (model, reader, density, scale);
      reader.close();
      return model;
   }

   /** 
    * Creates an FemModel with uniform density based on UCD data read from a
    * Reader. The node coordinate data can be scaled non-uniformly using an
    * optional parameter giving scale values about the x, y, and z axes.
    * 
    * @param model FEM model to be populated by UCD data
    * @param reader reader from which to read UCD data
    * @param density density of the model
    * @param scale if non-null, gives scaling about the x, y, and z axes
    * @throws IOException if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d model, Reader reader, double density, Vector3d scale)
      throws IOException {

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear ();
      }
      if (density >= 0) {
         model.setDensity (density);
      } else {
         model.setDensity(1);
      }
      ReaderTokenizer rtok = new ReaderTokenizer (new BufferedReader (reader));

      int numNodes = rtok.scanInteger();
      int numCells = rtok.scanInteger();
      int numNData = rtok.scanInteger();
      int numCData = rtok.scanInteger();
      int numMData = rtok.scanInteger();
      
      if (numNData > 0) {
         System.out.println ( 
            "Warning: UCD data contains extra node data; ignoring");
      }
      if (numCData > 0) {
         System.out.println ( 
            "Warning: UCD data contains extra cell data; ignoring");
      }
      if (numMData > 0) {
         System.out.println ( 
            "Warning: UCD data contains extra model data; ignoring");
      }

      boolean nodeIdWarningGiven = false;
      Point3d coords = new Point3d();
      for (int i=0; i<numNodes; i++){
         int nodeId = rtok.scanInteger();
         if (i != nodeId && !nodeIdWarningGiven) {
            System.out.println (
               "Warning: UCD data contains non-sequential nodeIds; ignoring");
            nodeIdWarningGiven = true;
         }
         coords.x = rtok.scanNumber();
         coords.y = rtok.scanNumber();
         coords.z = rtok.scanNumber();
         if (scale != null) {
            coords.x *= scale.x;
            coords.y *= scale.y;
            coords.z *= scale.z;
         }
         model.addNode (new FemNode3d (coords));
      }
      
      boolean cellIdWarningGiven = false;
      boolean matWarningGiven = false;
      for (int i=0; i<numCells; i++){
         int cellId = rtok.scanInteger();
         if (i != cellId && !cellIdWarningGiven) {
            System.out.println (
               "Warning: UCD data contains nonsequential cellIds; ignoring");
            cellIdWarningGiven = true;
         }
         int mat = rtok.scanInteger();
         if (mat != 0 && !matWarningGiven) {
            System.out.println (
"Warning: UCD data contains material references for cells; ignoring");
            matWarningGiven = true;
         }
         String cellType = rtok.scanWord();
         if (cellType.equals ("tet")) {
            readTet (rtok, cellId, model);
         }
         else if (cellType.equals ("hex")) {
            readHex (rtok, cellId, model);
         }
         else {
            throw new IOException (
               "Element type '"+cellType+"' is not supported");
         }
      }
      
      return model;
   }

   private static int[] scanNodeIndices (
      ReaderTokenizer rtok, int cellId, FemModel3d model, int num)
      throws IOException {
      int[] idxs = new int[num];
      for (int i=0; i<num; i++) {
         idxs[i] = rtok.scanInteger();
         if (idxs[i] < 0 || idxs[i] >= model.numNodes()) {
            throw new IOException (
               "Cell ID "+cellId+": nonexistent node "+idxs[i]);
         }
      }
      return idxs;
   }

   private static void readTet (
      ReaderTokenizer rtok, int cellId, FemModel3d model)
      throws IOException {
      int[] idxs = scanNodeIndices (rtok, cellId, model, 4);
      // In the UCD format the first three nodes of a Tet are supposedly
      // arranged counter-clockwise about some face, as opposed to the
      // clockwise orientation used by TetGen. On the other hand,
      // we have encountered UCD files that do not obey this convention,
      // and so we test the ordering by computed the tetrahedral volume.
      FemNode3d n0 = model.getNode (idxs[0]);
      FemNode3d n1 = model.getNode (idxs[1]);
      FemNode3d n2 = model.getNode (idxs[2]);
      FemNode3d n3 = model.getNode (idxs[3]);
      if (TetElement.computeVolume (n0, n1, n2, n3) >= 0) {
         model.addElement (new TetElement (n0, n1, n2, n3)); 
      }
      else {
         model.addElement (new TetElement (n0, n2, n1, n3)); 
      }
   }

   private static void readHex (
      ReaderTokenizer rtok, int cellId, FemModel3d model)
      throws IOException {
      int[] idxs = scanNodeIndices (rtok, cellId, model, 8);
      model.addElement (
         new HexElement (model.getNode (idxs[0]),
                         model.getNode (idxs[1]),
                         model.getNode (idxs[2]),
                         model.getNode (idxs[3]), 
                         model.getNode (idxs[4]), 
                         model.getNode (idxs[5]), 
                         model.getNode (idxs[6]), 
                         model.getNode (idxs[7])));
   }

}
