/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
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
import java.util.ArrayList;
import java.util.LinkedHashMap;

import maspack.matrix.Point3d;
import maspack.util.ReaderTokenizer;

/**
 * Abaqus File reader, only supports the NODE, ELEMENT and INCLUDE keywords
 * 
 * @author Antonio
 * 
 */
public class AbaqusReader {

   private enum FileSection {
      NODE, ELEM, OTHER
   }
   
   // Supported element types:
   // C3D4 - 4 node tet
   // C3D6 - 6 node wedge
   // C3D8 - 8 node hex
   // C3D10 - 10-node quadratic tet
   // C3D20 - 20-node quadratic hex
   public enum ElemType {
      C3D4(4, "C3D4"), 
      C3D6(6, "C3D6"),
      C3D8(8, "C3D8"),
      C3D10(10, "C3D10"),
      C3D20(20, "C3D20"),
      UNKNOWN(0, "UNKNOWN");
      
      private int numNodes;
      private String strId;
      public int getNumNodes() {
         return numNodes;
      }
      public String getString() {
         return strId;
      }
      private ElemType(int nNodes, String str) {
         numNodes = 4;
         strId = str;
      }
   }
   
   public static final String COMMENT = "**";
   public static final char KEYWORD = '*';
   
   static boolean cwHexWarningGiven;
   static boolean cwWedgeWarningGiven;
   static boolean cwTetWarningGiven; 
   static boolean nodeIdWarningGiven;
   
   /**
    * Creates an FemModel with uniform density based on Abaqus data contained in
    * a specified file. 
    * 
    * @param model
    * FEM model to be populated by Abaqus data
    * @param fileName
    * path name of the ABAQUS file
    * @throws IOException
    * if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d model, String fileName)
      throws IOException {
      return read(model, new File(fileName), 1);
   }
   
   /**
    * Creates an FemModel with uniform density based on Abaqus data contained in
    * a specified file. 
    * 
    * @param model
    * FEM model to be populated by Abaqus data
    * @param fileName
    * path name of the ABAQUS file
    * @param density
    * density of the model
    * @throws IOException
    * if this is a problem reading the file
    */
   public static void read (
      FemModel3d model, String fileName, double density)
      throws IOException {
      read(model, new File(fileName), density);
   }
   
   /**
    * Creates an FemModel with uniform density based on Abaqus data contained in
    * a specified file. 
    * 
    * @param model
    * FEM model to be populated by Abaqus data
    * @param file
    * the ABAQUS file
    * @param density
    * density of the model
    * @throws IOException
    * if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d model, File file, double density)
      throws IOException {

      Reader fileReader = null;

      try {
         fileReader = new BufferedReader(new FileReader (file));
         model = read (model, fileReader, density, new File[] {file.getParentFile()});
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (fileReader != null) {
            fileReader.close ();
         }
      }
      
      return model;
   }
   
   /**
    * Creates an FemModel with uniform density based on Abaqus data contained in
    * a specified file
    * 
    * @param model
    * FEM model to be populated by Abaqus data
    * @param fileReader
    * reader supplying node and element data in the Abaqus format
    * @param density
    * density of the model
    * @throws IOException
    * if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d model, Reader fileReader, double density,
      File[] includeDirs) throws IOException {

      // boolean useOneBasedNum = (options & ONE_BASED_NUMBERING) != 0;
      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear ();
      }
      model.setDensity (density);
      
      cwHexWarningGiven=false;
      cwWedgeWarningGiven=false;
      cwTetWarningGiven=false; 
      nodeIdWarningGiven=false;
      
      LinkedHashMap<Integer, Point3d> nodeMap = 
         new LinkedHashMap<Integer, Point3d>();
      LinkedHashMap<Integer, Integer> nodeIdMap = 
         new LinkedHashMap<Integer, Integer> ();
      LinkedHashMap<Integer, ArrayList<Integer>> elemMap = 
         new LinkedHashMap<Integer, ArrayList<Integer>>();
      
      readFile (fileReader, nodeMap, elemMap, includeDirs);
      
      for (int nodeId : nodeMap.keySet ()) {
         Point3d pos = nodeMap.get (nodeId);
         
         FemNode3d node = new FemNode3d (pos);
         model.addNumberedNode (node, nodeId);
         
         // Store new node ID to match with element node IDs
         nodeIdMap.put (nodeId, node.getNumber ());
      }
         
      ArrayList<HexElement> hexElems = new ArrayList<HexElement> ();
      
      for (int elemId : elemMap.keySet ()) {
         ArrayList<Integer> nodeList = elemMap.get (elemId);
         
         switch (nodeList.size ()) {
            case 4:
               createTet (model, nodeList, elemId);
               break;
            case 6:
               createWedge (model, nodeList, elemId);
               break;
            case 8:
               hexElems.add (createHex (model, nodeList, elemId));
               break;
            case 10:
               createQuadTet (model, nodeList, elemId);
               break;
            case 20:
               createQuadHex (model, nodeList, elemId);
               break;
            default:
               System.out.println ("Ignoring unknown element type with " +
                  nodeList.size() + "number of nodes");
         }
      }
      
      // TODO implement for quadhex elements
      HexElement.setParities (hexElems);

      return model;
      
   }
   
   private static void createTet (FemModel3d model, 
      ArrayList<Integer> nodeIds, int elemId) {
      
      FemNode3d n0 = model.getByNumber (nodeIds.get (0));
      FemNode3d n1 = model.getByNumber (nodeIds.get (1));
      FemNode3d n2 = model.getByNumber (nodeIds.get (2));
      FemNode3d n3 = model.getByNumber (nodeIds.get (3));
      
      TetElement e;

      if (TetElement.computeVolume (n0, n1, n2, n3) >= 0) {
         e = new TetElement (n0, n1, n2, n3);
      }
      else {
         e = new TetElement (n0, n2, n1, n3);

         if (cwTetWarningGiven) {
            System.out.println ("found cw tet");
            cwTetWarningGiven = true;
         }
      }
      
      model.addNumberedElement (e, elemId);
   }
   
   private static void createQuadTet (FemModel3d model, 
      ArrayList<Integer> nodeIds, int elemId) {
      
      FemNode3d n0 = model.getByNumber (nodeIds.get (0));
      FemNode3d n1 = model.getByNumber (nodeIds.get (1));
      FemNode3d n2 = model.getByNumber (nodeIds.get (2));
      FemNode3d n3 = model.getByNumber (nodeIds.get (3));
      FemNode3d n4 = model.getByNumber (nodeIds.get (4));
      FemNode3d n5 = model.getByNumber (nodeIds.get (5));
      FemNode3d n6 = model.getByNumber (nodeIds.get (6));
      FemNode3d n7 = model.getByNumber (nodeIds.get (7));
      FemNode3d n8 = model.getByNumber (nodeIds.get (8));
      FemNode3d n9 = model.getByNumber (nodeIds.get (9));
      
      QuadtetElement e;
      
      if (TetElement.computeVolume (n0, n1, n2, n3) >= 0) {
         e = new QuadtetElement (n0, n1, n2, n3, n4, n5, n6,
               n7, n8, n9);
      }
      else {
         e = new QuadtetElement (n0, n2, n1, n3, n6, n5, n4,
               n7, n9, n8);
         
         if (cwTetWarningGiven) {
            System.out.println ("found ccw quadtet");
            cwTetWarningGiven = true;
         }
      }
      
      model.addNumberedElement (e, elemId);
   }
   
   private static HexElement createHex (FemModel3d model, 
      ArrayList<Integer> nodeIds, int elemId) {

      FemNode3d n0 = model.getByNumber (nodeIds.get (0));
      FemNode3d n1 = model.getByNumber (nodeIds.get (1));
      FemNode3d n2 = model.getByNumber (nodeIds.get (2));
      FemNode3d n3 = model.getByNumber (nodeIds.get (3));
      FemNode3d n4 = model.getByNumber (nodeIds.get (4));
      FemNode3d n5 = model.getByNumber (nodeIds.get (5));
      FemNode3d n6 = model.getByNumber (nodeIds.get (6));
      FemNode3d n7 = model.getByNumber (nodeIds.get (7));
      
      HexElement e;
      
      if (HexElement.computeVolume (n3, n2, n1, n0, n7, n6, n5, n4) >= 0) {
         e = new HexElement (n3, n2, n1, n0, n7, n6, n5, n4);
      }
      else {
         e = new HexElement (n0, n1, n2, n3, n4, n5, n6, n7);

         if (!cwHexWarningGiven) {
            System.out.println ("found cw hex");
            cwHexWarningGiven = true;
         }
         
         if (e.computeVolumes () < 0) {
            System.out.println ("found neg volume hex, # " + e.getNumber ());
         }
      }
      
      try {
         model.addNumberedElement (e, elemId);
      } catch (Exception err) {
         System.out.println("element " + elemId + " caused a problem");
      }
      
      return e;
   }
   
   private static void createQuadHex (FemModel3d model, 
      ArrayList<Integer> nodeIds, int elemId) {

      FemNode3d n0 = model.getByNumber (nodeIds.get (0));
      FemNode3d n1 = model.getByNumber (nodeIds.get (1));
      FemNode3d n2 = model.getByNumber (nodeIds.get (2));
      FemNode3d n3 = model.getByNumber (nodeIds.get (3));
      FemNode3d n4 = model.getByNumber (nodeIds.get (4));
      FemNode3d n5 = model.getByNumber (nodeIds.get (5));
      FemNode3d n6 = model.getByNumber (nodeIds.get (6));
      FemNode3d n7 = model.getByNumber (nodeIds.get (7));
      FemNode3d n8 = model.getByNumber (nodeIds.get (8));
      FemNode3d n9 = model.getByNumber (nodeIds.get (9));
      FemNode3d n10 = model.getByNumber (nodeIds.get (10));
      FemNode3d n11 = model.getByNumber (nodeIds.get (11));
      FemNode3d n12 = model.getByNumber (nodeIds.get (12));
      FemNode3d n13 = model.getByNumber (nodeIds.get (13));
      FemNode3d n14 = model.getByNumber (nodeIds.get (14));
      FemNode3d n15 = model.getByNumber (nodeIds.get (15));
      FemNode3d n16 = model.getByNumber (nodeIds.get (16));
      FemNode3d n17 = model.getByNumber (nodeIds.get (17));
      FemNode3d n18 = model.getByNumber (nodeIds.get (18));
      FemNode3d n19 = model.getByNumber (nodeIds.get (19));
      
      QuadhexElement e;
      
      if (HexElement.computeVolume (n3, n2, n1, n0, n7, n6, n5, n4) >= 0) {
         e = new QuadhexElement (
            new FemNode3d[] {n3, n2, n1, n0, n7, n6, n5, n4, n10, n9, n8, n11,
                             n14, n13, n12, n15, n19, n18, n17, n16});
      }
      else {
         e = new QuadhexElement (
            new FemNode3d[] {n0, n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11,
                             n12, n13, n14, n15, n16, n17, n18, n19});

         if (!cwHexWarningGiven) {
            System.out.println ("found cw quadhex");
            cwHexWarningGiven = true;
         }
         
         if (e.computeVolumes () < 0) {
            System.out.println ("found neg volume quadhex, # " + e.getNumber ());
         }
      }  
      
      model.addNumberedElement (e, elemId);
   }

   private static void createWedge (FemModel3d model, 
      ArrayList<Integer> nodeIds, int elemId) {
      
      FemNode3d n0 = model.getByNumber (nodeIds.get (0));
      FemNode3d n1 = model.getByNumber (nodeIds.get (1));
      FemNode3d n2 = model.getByNumber (nodeIds.get (2));
      FemNode3d n3 = model.getByNumber (nodeIds.get (3));
      FemNode3d n4 = model.getByNumber (nodeIds.get (4));
      FemNode3d n5 = model.getByNumber (nodeIds.get (5));

      WedgeElement e = new WedgeElement (n0, n1, n2, n3, n4, n5);

      if (e.computeVolumes () < 0) {
         e = new WedgeElement (n2, n1, n0, n5, n4, n3);
         
         if (!cwWedgeWarningGiven) {
            System.out.println ("found ccw wedge");
            cwWedgeWarningGiven = true;
         }
         
         if (e.computeVolumes () < 0) {
            System.out.println ("found inverted wedge, # " + e.getNumber ());
         }
      }
      
      model.addNumberedElement (e, elemId);
   }
   
   
   private static void readFile(Reader reader, LinkedHashMap<Integer, Point3d> nodeMap,
      LinkedHashMap<Integer, ArrayList<Integer>> elemMap, File [] includeDirs) throws IOException {
      
      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (reader));
      rtok.eolIsSignificant (true);
      rtok.wordChar('*');
      rtok.whitespaceChar(',');  //ignore commas

      FileSection mySection = FileSection.OTHER;
      ElemType myElemType = ElemType.UNKNOWN;
      
      int nodeId = 0;
      int elemId = 0;
      
      while (rtok.nextToken () != ReaderTokenizer.TT_EOF) {
         
         // determine type
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (rtok.sval.startsWith(COMMENT)) {
               // ignore
            } else if (rtok.sval.charAt(0) == KEYWORD) {
               // potentially change mode
               
               String keyword = rtok.sval.substring(1);
               if (keyword.equalsIgnoreCase("NODE")) {
                  mySection = FileSection.NODE;
               } else if (keyword.equalsIgnoreCase("ELEMENT")) {
                  mySection = FileSection.ELEM;
                  
                  // determine type
                  String line = readLine(rtok);
                  String type = parseKey("TYPE=", line);
                  myElemType = ElemType.UNKNOWN;
                  for (ElemType et : ElemType.values()) {
                     if (et.getString().equalsIgnoreCase(type)) {
                        myElemType = et;
                        break;
                     }
                  }
                  
                  if (myElemType == ElemType.UNKNOWN) {
                     System.err.println("Warning: unknown element type '" + type + "'");
                  }
                  
               } else if (keyword.equalsIgnoreCase("INCLUDE")){
                  
                  String line = readLine(rtok);
                  String fileName = parseKey("INPUT=", line);
                  
                  // find file
                  File input = findFile(fileName, includeDirs);
                  if (input == null) {
                     throw new IOException("Cannot find INCLUDE file '" + fileName + "'");
                  }
                  
                  FileReader inputReader = null;
                  try {
                     inputReader = new FileReader(input);
                     readFile(inputReader, nodeMap, elemMap, includeDirs);
                  } catch (IOException e) {
                     throw e;
                  } finally {
                     if (inputReader != null) {
                        inputReader.close();
                     }
                  }
               } else {
                  mySection = FileSection.OTHER;
                  System.out.println("Warning: ignoring section '" + keyword + "'");
               }
            }
            toEOL(rtok);   // skip to end-of-line
         } else {
            rtok.pushBack();
            
            // action depends on mode
            switch (mySection) {
               case ELEM:
                  elemId = rtok.scanInteger();
                  
                  ArrayList<Integer> nodes = new ArrayList<Integer>();
                  while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
                     nodes.add((int)rtok.nval);
                  }
                  elemMap.put(elemId, nodes);
                  toEOL(rtok);
                  break;
               case NODE:
                  nodeId = rtok.scanInteger();
                  double x = rtok.scanNumber();
                  double y = rtok.scanNumber();
                  double z = rtok.scanNumber();
                  nodeMap.put(nodeId, new Point3d(x,y,z));
                  toEOL(rtok);
                  break;
               case OTHER:
                  toEOL(rtok);
            }
            
         }
         
      }
      
   }
   
   private static File findFile(String fileName, File[] dirs) {
      
      File file = null;
      
      file = new File(fileName);
      if (file.canRead()) {
         return file;
      }
      
      for (File dir : dirs) {
         file = new File(dir, fileName);
         if (file.canRead()) {
            return file;
         }
      }
      
      return null;
      
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
   
   
   private static String parseKey(String keyName, String line) {
      
      String linesmall = line.toLowerCase();
      String keysmall = keyName.toLowerCase();
      String val = null;
      
      int istart = linesmall.indexOf(keysmall);
      if (istart <0) {
         return null;
      }
      istart = istart + keyName.length();
      if (istart >= line.length()) {
         return null;
      }
      val = line.substring(istart);
      istart = findChar(val,", \r\n");
      if (istart > 0) {
         val = val.substring(0, istart);
      }
      return val;
      
   }
   
   private static int findChar(String str, String charSet) {
      for (int i=0; i<str.length(); i++) {
         for (int j=0; j<charSet.length(); j++) {
            if (str.charAt(i)==charSet.charAt(j)) {
               return i;
            }
         }
      }
      return -1;
   }
   
}
