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
import maspack.util.ArraySupport;

/**
 * Abaqus File reader, only supports the NODE, ELEMENT and INCLUDE keywords
 * 
 * @author Antonio
 * 
 */
public class AbaqusReader implements FemReader {

   /** 
    * Tells the reader to number the nodes and elements starting from zero.
    */   
   public static int ZERO_BASED_NUMBERING = 0x2;

   File myFile = null;

   static boolean myReadShellsAsMembranes = false;
   static double myShellThickness = 0.001;
   
   public static void setReadShellsAsMembranes (boolean enable) {
      myReadShellsAsMembranes = enable;
   }

   public static boolean getReadShellsAsMembranes () {
      return myReadShellsAsMembranes;
   }

   public static void setShellThickness (double enable) {
      myShellThickness = enable;
   }

   public static double getShellThickness () {
      return myShellThickness;
   }

   public AbaqusReader(File file) {
      myFile = file;
   }
   
   public AbaqusReader(String filename) {
      myFile = new File(filename);
   }
   
   @Override
   public FemModel3d readFem(FemModel3d fem) throws IOException {
      return read(fem, myFile.getAbsolutePath());
   }

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
      // shell elements
      S3(3, "S3"), 
      S4(4, "S4"),
      UNKNOWN(0, "UNKNOWN");
      
      private int numNodes;
      private String strId;
      public int numNodes() {
         return numNodes;
      }
      public String getString() {
         return strId;
      }
      private ElemType(int nNodes, String str) {
         numNodes = nNodes;
         strId = str;
      }
   }

   public static class ElemDesc {
      ElemType myType;
      int[] myNodeIds;

      ElemDesc (ElemType type, int[] nodeIds) {
         myType = type;
         myNodeIds = nodeIds;
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
    * FEM model to be populated by Abaqus data. If <code>null</code>, a
    * new model is created
    * @param fileName
    * path name of the ABAQUS file
    * @return created model
    * @throws IOException
    * if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d model, String fileName)
      throws IOException {
      return read(model, new File(fileName), 1, /*options=*/0);
   }
   
   /**
    * Creates an FemModel with uniform density based on Abaqus data contained in
    * a specified file. 
    * 
    * @param model
    * FEM model to be populated by Abaqus data. If <code>null</code>, a
    * new model is created
    * @param fileName
    * path name of the ABAQUS file
    * @param density
    * density of the model
    * @param options
    * option flags. Options include {@link #ZERO_BASED_NUMBERING}.
    * @return created model
    * @throws IOException
    * if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d model, String fileName, double density, int options)
      throws IOException {
      return read(model, new File(fileName), density, options);
   }
   
   /**
    * Creates an FemModel with uniform density based on Abaqus data contained in
    * a specified file. 
    * 
    * @param model
    * FEM model to be populated by Abaqus data. If <code>null</code>, a
    * new model is created
    * @param file
    * the ABAQUS file
    * @param density
    * density of the model
    * @param options
    * option flags. Options include {@link #ZERO_BASED_NUMBERING}.
    * @return created model
    * @throws IOException
    * if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d model, File file, double density, int options)
      throws IOException {

      Reader fileReader = null;

      try {
         fileReader = new BufferedReader(new FileReader (file));
         model = read (model, fileReader, density, options,
                       new File[] {file.getParentFile()});
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
    * @param fileReader reader supplying node and element data in the Abaqus
    * format. If <code>null</code>, a new model is created
    * @param density
    * density of the model
    * @param options
    * option flags. Options include {@link #ZERO_BASED_NUMBERING}.
    * @param includeDirs list of directories to search for include files
    * @return created model
    * @throws IOException
    * if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d model, Reader fileReader, double density, int options,
      File[] includeDirs) throws IOException {

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
      LinkedHashMap<Integer, ElemDesc> elemMap = 
         new LinkedHashMap<Integer, ElemDesc>();
      
      readFile (fileReader, nodeMap, elemMap, includeDirs, options);
      
      for (int nodeId : nodeMap.keySet ()) {
         Point3d pos = nodeMap.get (nodeId);
         
         FemNode3d node = new FemNode3d (pos);
         model.addNumberedNode (node, nodeId);
         
         // Store new node ID to match with element node IDs
         nodeIdMap.put (nodeId, node.getNumber ());
      }
         
      ArrayList<HexElement> hexElems = new ArrayList<HexElement> ();
      
      for (int elemId : elemMap.keySet ()) {
         ElemDesc edesc = elemMap.get (elemId);
         int[] nodeList = edesc.myNodeIds;
         
         switch (edesc.myType) {
            case C3D4:
               createTet (model, nodeList, elemId, options);
               break;
            case C3D6:
               createWedge (model, nodeList, elemId, options);
               break;
            case C3D8:
               hexElems.add (createHex (model, nodeList, elemId, options));
               break;
            case C3D10:
               createQuadTet (model, nodeList, elemId, options);
               break;
            case C3D20:
               createQuadHex (model, nodeList, elemId, options);
               break;
            case S3:
               createShellTri (model, nodeList, elemId, options);
               break;
            case S4:
               createShellQuad (model, nodeList, elemId, options);
               break;
            default:
               System.out.println (
                  "Ignoring unknown element type " + edesc.myType);
         }
      }
      
      // TODO implement for quadhex elements
      HexElement.setParities (hexElems);

      return model;
      
   }
   
   static void addElement (
      FemModel3d model, FemElement3dBase elem, int elemId, int options) {

      if (elem instanceof FemElement3d) {
         if ((options & ZERO_BASED_NUMBERING) != 0) {
            model.addElement ((FemElement3d)elem);
         }
         else {
            model.addNumberedElement ((FemElement3d)elem, elemId);
         }
      }
      else if (elem instanceof ShellElement3d) {
         if ((options & ZERO_BASED_NUMBERING) != 0) {
            FemNode3d[] nodes = elem.getNodes();
            model.addShellElement ((ShellElement3d)elem);
         }
         else {
            model.addNumberedShellElement ((ShellElement3d)elem, elemId);
         }
      }
   }


   private static void createTet (
      FemModel3d model, int[] nodeIds, int elemId, int options) {
      
      FemNode3d n0 = model.getByNumber (nodeIds[0]);
      FemNode3d n1 = model.getByNumber (nodeIds[1]);
      FemNode3d n2 = model.getByNumber (nodeIds[2]);
      FemNode3d n3 = model.getByNumber (nodeIds[3]);
      
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
      addElement (model, e, elemId, options);
   }
   
   private static void createQuadTet (
      FemModel3d model, int[] nodeIds, int elemId, int options) {
      
      FemNode3d n0 = model.getByNumber (nodeIds[0]);
      FemNode3d n1 = model.getByNumber (nodeIds[1]);
      FemNode3d n2 = model.getByNumber (nodeIds[2]);
      FemNode3d n3 = model.getByNumber (nodeIds[3]);
      FemNode3d n4 = model.getByNumber (nodeIds[4]);
      FemNode3d n5 = model.getByNumber (nodeIds[5]);
      FemNode3d n6 = model.getByNumber (nodeIds[6]);
      FemNode3d n7 = model.getByNumber (nodeIds[7]);
      FemNode3d n8 = model.getByNumber (nodeIds[8]);
      FemNode3d n9 = model.getByNumber (nodeIds[9]);
      
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
      
      addElement (model, e, elemId, options);
   }
   
   private static HexElement createHex (
      FemModel3d model, int[] nodeIds, int elemId, int options) {

      FemNode3d n0 = model.getByNumber (nodeIds[0]);
      FemNode3d n1 = model.getByNumber (nodeIds[1]);
      FemNode3d n2 = model.getByNumber (nodeIds[2]);
      FemNode3d n3 = model.getByNumber (nodeIds[3]);
      FemNode3d n4 = model.getByNumber (nodeIds[4]);
      FemNode3d n5 = model.getByNumber (nodeIds[5]);
      FemNode3d n6 = model.getByNumber (nodeIds[6]);
      FemNode3d n7 = model.getByNumber (nodeIds[7]);
      
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
         addElement (model, e, elemId, options);
      } catch (Exception err) {
         System.out.println("element " + elemId + " caused a problem");
      }
      
      return e;
   }
   
   private static void createQuadHex (
      FemModel3d model, int[] nodeIds, int elemId, int options) {

      FemNode3d n0 = model.getByNumber (nodeIds[0]);
      FemNode3d n1 = model.getByNumber (nodeIds[1]);
      FemNode3d n2 = model.getByNumber (nodeIds[2]);
      FemNode3d n3 = model.getByNumber (nodeIds[3]);
      FemNode3d n4 = model.getByNumber (nodeIds[4]);
      FemNode3d n5 = model.getByNumber (nodeIds[5]);
      FemNode3d n6 = model.getByNumber (nodeIds[6]);
      FemNode3d n7 = model.getByNumber (nodeIds[7]);
      FemNode3d n8 = model.getByNumber (nodeIds[8]);
      FemNode3d n9 = model.getByNumber (nodeIds[9]);
      FemNode3d n10 = model.getByNumber (nodeIds[10]);
      FemNode3d n11 = model.getByNumber (nodeIds[11]);
      FemNode3d n12 = model.getByNumber (nodeIds[12]);
      FemNode3d n13 = model.getByNumber (nodeIds[13]);
      FemNode3d n14 = model.getByNumber (nodeIds[14]);
      FemNode3d n15 = model.getByNumber (nodeIds[15]);
      FemNode3d n16 = model.getByNumber (nodeIds[16]);
      FemNode3d n17 = model.getByNumber (nodeIds[17]);
      FemNode3d n18 = model.getByNumber (nodeIds[18]);
      FemNode3d n19 = model.getByNumber (nodeIds[19]);
      
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
      
      addElement (model, e, elemId, options);
   }

   private static void createWedge (
      FemModel3d model, int[] nodeIds, int elemId, int options) {
      
      FemNode3d n0 = model.getByNumber (nodeIds[0]);
      FemNode3d n1 = model.getByNumber (nodeIds[1]);
      FemNode3d n2 = model.getByNumber (nodeIds[2]);
      FemNode3d n3 = model.getByNumber (nodeIds[3]);
      FemNode3d n4 = model.getByNumber (nodeIds[4]);
      FemNode3d n5 = model.getByNumber (nodeIds[5]);

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
      
      addElement (model, e, elemId, options);
   }
   
   private static void createShellTri (
      FemModel3d model, int[] nodeIds, int elemId, int options) {
      
      FemNode3d n0 = model.getByNumber (nodeIds[0]);
      FemNode3d n1 = model.getByNumber (nodeIds[1]);
      FemNode3d n2 = model.getByNumber (nodeIds[2]);
      
      ShellTriElement e =
         new ShellTriElement (
            n0, n1, n2, myShellThickness, myReadShellsAsMembranes);
      
      addElement (model, e, elemId, options);
   }
   
   private static void createShellQuad (
      FemModel3d model, int[] nodeIds, int elemId, int options) {
      
      FemNode3d n0 = model.getByNumber (nodeIds[0]);
      FemNode3d n1 = model.getByNumber (nodeIds[1]);
      FemNode3d n2 = model.getByNumber (nodeIds[2]);
      FemNode3d n3 = model.getByNumber (nodeIds[3]);
      
      ShellQuadElement e =
         new ShellQuadElement (
            n0, n1, n2, n3, myShellThickness, myReadShellsAsMembranes);
      
      addElement (model, e, elemId, options);
   }
   
   private static void readFile (
      Reader reader, LinkedHashMap<Integer, Point3d> nodeMap,
      LinkedHashMap<Integer, ElemDesc> elemMap,
      File [] includeDirs, int options) throws IOException {

      boolean zeroBasedNumbering = ((options & ZERO_BASED_NUMBERING) != 0);
      
      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (reader));
      rtok.eolIsSignificant (true);
      rtok.wordChar('*');
      rtok.whitespaceChar(',');  //ignore commas

      FileSection mySection = FileSection.OTHER;
      ElemType myElemType = ElemType.UNKNOWN;
      
      int nodeId = 0;
      int elemId = 0;

      boolean warnedNumNodes = false;
      
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
                  String type = parseKey("TYPE=", line).toUpperCase();
                  myElemType = ElemType.UNKNOWN;
                  for (ElemType et : ElemType.values()) {
                     if (type.startsWith (et.getString())) {
                        myElemType = et;
                        break;
                     }
                  }
                  
                  if (myElemType == ElemType.UNKNOWN) {
                     System.err.println("Warning: unknown element type '" + type + "'");
                  }
                  warnedNumNodes = false;
                  
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
                     readFile (
                        inputReader, nodeMap, elemMap, includeDirs, options);
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
                  ArrayList<Integer> nodes = new ArrayList<Integer>();
                  if (zeroBasedNumbering) {
                     elemId = rtok.scanInteger()-1;
                     while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
                        nodes.add((int)rtok.nval-1);
                     }
                  }
                  else {
                     elemId = rtok.scanInteger();
                     while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
                        nodes.add((int)rtok.nval);
                     }
                  }
                  if (nodes.size() != myElemType.numNodes()) {
                     if (!warnedNumNodes) {
                        System.out.println (
                           "Warning: elementType "+myElemType+" expects "+
                           myElemType.numNodes() + " nodes; got " + nodes.size());
                        warnedNumNodes = true;
                     }
                  }
                  else {
                     ElemDesc edesc =
                        new ElemDesc (
                           myElemType, ArraySupport.toIntArray (nodes));
                     elemMap.put(elemId, edesc);
                  }
                  toEOL(rtok);
                  break;
               case NODE:
                  nodeId = rtok.scanInteger();
                  if (zeroBasedNumbering) {
                     nodeId--;
                  }
                  double x = rtok.scanNumber();
                  double y = rtok.scanNumber();
                  double z = rtok.scanNumber();
                  nodeMap.put(nodeId, new Point3d(x,y,z));
                  toEOL(rtok);
                  break;
               case OTHER:
                  toEOL(rtok);
                  break;
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
