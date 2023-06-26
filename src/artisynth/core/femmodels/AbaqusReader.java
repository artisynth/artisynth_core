/**
 * Copyright (c) 2023, by the Authors: Antonio Sanchez, John E Lloyd (UBC)
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
import maspack.matrix.VectorNi;
import maspack.util.ReaderTokenizer;
import maspack.util.ArraySupport;
import maspack.util.DynamicIntArray;
import maspack.util.InternalErrorException;

/**
 * Abaqus File reader, only supports the NODE, ELEMENT and INCLUDE keywords
 * 
 * @author Antonio, John E Lloyd
 */
public class AbaqusReader implements FemReader {

   /** 
    * In static method calls, tells the reader to number the nodes and elements
    * starting from zero. This preserves the Abaqus numbering of nodes and
    * elements, only with the numbers decremented by one.
    */   
   public static int ZERO_BASED_NUMBERING = 0x2;

   /** 
    * In static method calls, tells the reader to ignore Abaqus numbering of
    * nodes and elements. Instead, these will be numbered according to the
    * order they appear in the file, starting at 0. This option takes
    * precedence over {@link #ZERO_BASED_NUMBERING}.
    */   
   public static int RESET_NUMBERING = 0x4;

   File myFile = null;
   File[] myIncludeDirs = null;
   ReaderTokenizer myRtok = null;

   static public double DEFAULT_SHELL_THICKNESS = 0.001;
   static public boolean DEFAULT_ZERO_BASED_NUMBERING = false;
   static public boolean DEFAULT_RESET_NUMBERING = false;
   static double myDefaultShellThickness = DEFAULT_SHELL_THICKNESS;

   public static void setDefaultShellThickness (double thickness) {
      myDefaultShellThickness = thickness;
   }

   public static double getDefaultShellThickness () {
      return myDefaultShellThickness;
   }

   protected double myDensity = FemModel.DEFAULT_DENSITY;
   protected double myShellThickness = DEFAULT_SHELL_THICKNESS;
   protected boolean myZeroBasedNumbering = DEFAULT_ZERO_BASED_NUMBERING;
   protected boolean myResetNumbering = DEFAULT_RESET_NUMBERING;

   // used when reseting numbering
   protected int[] myNodeNumMap;

   public void setShellThickness (double thickness) {
      myShellThickness = thickness;
   }

   public double getShellThickness () {
      return myShellThickness;
   }

   public void setDensity (double density) {
      myDensity = density;
   }

   public double getDensity () {
      return myDensity;
   }

   public void setZeroBasedNumbering (boolean enable) {
      myZeroBasedNumbering = enable;
   }

   public boolean getZeroBasedNumbering () {
      return myZeroBasedNumbering;
   }

   public void setResetNumbering (boolean enable) {
      myResetNumbering = enable;
   }

   public boolean getResetNumbering () {
      return myResetNumbering;
   }

   boolean mySuppressWarnings = false;

   public boolean getSuppressWarnings() {
      return mySuppressWarnings;
   }
   
   public void setSuppressWarnings(boolean enable) {
      mySuppressWarnings = enable;
   }
   
   public AbaqusReader (Reader reader, File[] includeDirs) {
      myRtok = new ReaderTokenizer (new BufferedReader (reader));
      if (includeDirs == null) {
         myIncludeDirs = new File[0];
      }
      else {
         myIncludeDirs = new File[includeDirs.length];
         for (int k=0; k<includeDirs.length; k++) {
            myIncludeDirs[k] = includeDirs[k];
         }
      }
      myShellThickness = myDefaultShellThickness;
   }

   public AbaqusReader (File file, File[] includeDirs) throws IOException {
      this (new FileReader (file), includeDirs);
      myFile = file;
   }
   
   public AbaqusReader (File file) throws IOException {
      this (new FileReader (file), new File[] {file.getParentFile()});
      myFile = file;
   }
   
   public AbaqusReader (String filename) throws IOException {
      this (new File(filename));
   }
   
   @Override
   public FemModel3d readFem (FemModel3d fem) throws IOException {

      if (fem == null) {
         fem = new FemModel3d();
      } else {
         fem.clear ();
      }

      fem.setDensity (getDensity());
      
      LinkedHashMap<Integer, Point3d> nodeMap = 
         new LinkedHashMap<Integer, Point3d>();
//      LinkedHashMap<Integer, Integer> nodeIdMap = 
//         new LinkedHashMap<Integer, Integer> ();
      LinkedHashMap<Integer, ElemDesc> elemMap = 
         new LinkedHashMap<Integer, ElemDesc>();
      
      int maxNodeId = readFile (myRtok, nodeMap, elemMap);
      
      if (myResetNumbering) {
         // map from Abaqus node numbers to new numbers
         myNodeNumMap = new int[maxNodeId+1];
      }      
      for (int nodeId : nodeMap.keySet ()) {
         Point3d pos = nodeMap.get (nodeId);
         
         FemNode3d node = new FemNode3d (pos);
         if (myResetNumbering) {
            fem.addNode (node);
            myNodeNumMap[nodeId] = node.getNumber();
         }
         else {
            fem.addNumberedNode (node, nodeId);
         }         
         // Store new node ID to match with element node IDs
         //nodeIdMap.put (nodeId, node.getNumber ());
      }
         
      ArrayList<HexElement> hexElems = new ArrayList<HexElement> ();
      
      for (int elemId : elemMap.keySet ()) {
         ElemDesc edesc = elemMap.get (elemId);
         int[] nodeList = edesc.myNodeIds;
         if (myResetNumbering) {
            for (int i=0; i<nodeList.length; i++) {
               nodeList[i] = myNodeNumMap[nodeList[i]];
            }
         }
         switch (edesc.myType) {
            case C3D4:
               createTet (fem, nodeList, elemId);
               break;
            case C3D6:
               createWedge (fem, nodeList, elemId);
               break;
            case C3D8:
               hexElems.add (createHex (fem, nodeList, elemId));
               break;
            case C3D10:
               createQuadTet (fem, nodeList, elemId);
               break;
            case C3D20:
               createQuadHex (fem, nodeList, elemId);
               break;
            case S3:
               createShellTri (fem, nodeList, elemId, /*membrane=*/false);
               break;
            case S4:
               createShellQuad (fem, nodeList, elemId, /*membrane=*/false);
               break;
            case CPS3:
               createShellTri (fem, nodeList, elemId, /*membrane=*/true);
               break;
            case CPS4:
               createShellQuad (fem, nodeList, elemId, /*membrane=*/true);
               break;
            default:
               System.out.println (
                  "Ignoring unknown element type " + edesc.myType);
         }
      }

      // C3D4 C3D10M C3D8R S3 S4 CPS3
      
      // TODO implement for quadhex elements
      HexElement.setParities (hexElems);

      return fem;
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
      S3(3, "S3"), // membrane
      S4(4, "S4"), // membrane
      CPS3(3, "CPS3"), 
      CPS4(4, "CPS4"),
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

   private static class ElemDesc {
      ElemType myType;
      int[] myNodeIds;

      ElemDesc (ElemType type, int[] nodeIds) {
         myType = type;
         myNodeIds = nodeIds;
      }
   }
   
   private static final String COMMENT = "**";
   private static final char KEYWORD = '*';
   
   private boolean cwHexWarningGiven;
   private boolean cwWedgeWarningGiven;
   private boolean cwTetWarningGiven; 
   private boolean cwQuadtetWarningGiven; 
   private boolean cwQuadhexWarningGiven; 
   private boolean nodeIdWarningGiven;
   
   /**
    * Creates an FemModel with uniform density (of 1) based on Abaqus data
    * contained in a specified file.
    * 
    * @param fem
    * FEM model to be populated by Abaqus data. If <code>null</code>, a
    * new model is created
    * @param fileName
    * path name of the ABAQUS file
    * @return created model
    * @throws IOException
    * if this is a problem reading the file
    */
   public static FemModel3d read (FemModel3d fem, String fileName)
      throws IOException {
      return read(fem, new File(fileName), 1, /*options=*/0);
   }
   
   /**
    * Creates an FemModel with uniform density based on Abaqus data contained
    * in a specified file.
    * 
    * @param fem
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
      FemModel3d fem, String fileName, double density, int options)
      throws IOException {
      return read(fem, new File(fileName), density, options);
   }
   
   /**
    * Creates an FemModel with uniform density based on Abaqus data contained in
    * a specified file. 
    * 
    * @param fem
    * FEM model to be populated by Abaqus data. If <code>null</code>, a
    * new model is created
    * @param file
    * the ABAQUS file
    * @param density
    * density of the FEM model
    * @param options
    * option flags. Options include {@link #ZERO_BASED_NUMBERING}.
    * @return created FEM model
    * @throws IOException
    * if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d fem, File file, double density, int options)
      throws IOException {

      AbaqusReader reader = null;
      try {
         reader = new AbaqusReader (file);
         reader.setZeroBasedNumbering ((options & ZERO_BASED_NUMBERING) != 0);
         reader.setResetNumbering ((options & RESET_NUMBERING) != 0);
         reader.setDensity (density);
         return reader.readFem (fem);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (reader != null) {
            reader.close ();
         }
      }
   }
   
   /**
    * Creates an FemModel with uniform density based on Abaqus data contained in
    * a specified file
    * 
    * @param fem FEM model to be populated by Abaqus data format. If
    * <code>null</code>, a new model is created
    * @param file the ABAQUS file
    * @param density
    * density of the FEM model
    * @param options
    * option flags. Options include {@link #ZERO_BASED_NUMBERING}.
    * @param includeDirs list of directories to search for include files
    * @return created FEM model
    * @throws IOException
    * if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d fem, File file, double density, int options,
      File[] includeDirs) throws IOException {

      AbaqusReader reader = null;
      try {
         reader = new AbaqusReader (file, includeDirs);
         reader.setZeroBasedNumbering ((options & ZERO_BASED_NUMBERING) != 0);
         reader.setResetNumbering ((options & RESET_NUMBERING) != 0);
         reader.setDensity (density);
         return reader.readFem (fem);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (reader != null) {
            reader.close ();
         }
      }
   }
   
   void addElement (
      FemModel3d fem, FemElement3dBase elem, int elemId) {

      if (elem instanceof FemElement3d) {
         if (myResetNumbering) {
            fem.addElement ((FemElement3d)elem);
         }
         else {
            fem.addNumberedElement ((FemElement3d)elem, elemId);
         }
      }
      else if (elem instanceof ShellElement3d) {
         if (myResetNumbering) {
            fem.addShellElement ((ShellElement3d)elem);
         }
         else {
            fem.addNumberedShellElement ((ShellElement3d)elem, elemId);
         }
      }
   }

   private void createTet (
      FemModel3d fem, int[] nodeIds, int elemId) {
      
      FemNode3d n0 = fem.getNodeByNumber (nodeIds[0]);
      FemNode3d n1 = fem.getNodeByNumber (nodeIds[1]);
      FemNode3d n2 = fem.getNodeByNumber (nodeIds[2]);
      FemNode3d n3 = fem.getNodeByNumber (nodeIds[3]);
      
      TetElement e;

      if (TetElement.computeVolume (n0, n1, n2, n3) >= 0) {
         e = new TetElement (n0, n1, n2, n3);
      }
      else {
         e = new TetElement (n0, n2, n1, n3);
         if (!mySuppressWarnings && cwTetWarningGiven) {
            System.out.println ("WARNING: fixed orientation for some tet elements");
            cwTetWarningGiven = true;
         }
      }
      addElement (fem, e, elemId);
   }
   
   private void createQuadTet (
      FemModel3d fem, int[] nodeIds, int elemId) {
      
      FemNode3d n0 = fem.getNodeByNumber (nodeIds[0]);
      FemNode3d n1 = fem.getNodeByNumber (nodeIds[1]);
      FemNode3d n2 = fem.getNodeByNumber (nodeIds[2]);
      FemNode3d n3 = fem.getNodeByNumber (nodeIds[3]);
      FemNode3d n4 = fem.getNodeByNumber (nodeIds[4]);
      FemNode3d n5 = fem.getNodeByNumber (nodeIds[5]);
      FemNode3d n6 = fem.getNodeByNumber (nodeIds[6]);
      FemNode3d n7 = fem.getNodeByNumber (nodeIds[7]);
      FemNode3d n8 = fem.getNodeByNumber (nodeIds[8]);
      FemNode3d n9 = fem.getNodeByNumber (nodeIds[9]);
      
      QuadtetElement e;
      
      if (TetElement.computeVolume (n0, n1, n2, n3) >= 0) {
         e = new QuadtetElement (n0, n1, n2, n3, n4, n5, n6,
               n7, n8, n9);
      }
      else {
         e = new QuadtetElement (n0, n2, n1, n3, n6, n5, n4,
               n7, n9, n8);

         if (!mySuppressWarnings && cwQuadtetWarningGiven) {
            System.out.println ("WARNING: fixed orientation for some quadtet elements");
            cwQuadtetWarningGiven = true;
         }
      }
      
      addElement (fem, e, elemId);
   }
   
   private HexElement createHex (
      FemModel3d fem, int[] nodeIds, int elemId) {

      FemNode3d n0 = fem.getNodeByNumber (nodeIds[0]);
      FemNode3d n1 = fem.getNodeByNumber (nodeIds[1]);
      FemNode3d n2 = fem.getNodeByNumber (nodeIds[2]);
      FemNode3d n3 = fem.getNodeByNumber (nodeIds[3]);
      FemNode3d n4 = fem.getNodeByNumber (nodeIds[4]);
      FemNode3d n5 = fem.getNodeByNumber (nodeIds[5]);
      FemNode3d n6 = fem.getNodeByNumber (nodeIds[6]);
      FemNode3d n7 = fem.getNodeByNumber (nodeIds[7]);
      
      HexElement e;
      
      if (HexElement.computeVolume (n3, n2, n1, n0, n7, n6, n5, n4) >= 0) {
         e = new HexElement (n3, n2, n1, n0, n7, n6, n5, n4);
      }
      else {
         e = new HexElement (n0, n1, n2, n3, n4, n5, n6, n7);
         
         if (!mySuppressWarnings && !cwHexWarningGiven) {
            System.out.println ("WARNING: fixed orientation for some hex elements");
            cwHexWarningGiven = true;
         }
         
         if (!mySuppressWarnings && e.computeVolumes () < 0) {
            System.out.println (
               "WARNING: negative volume for hex number " + e.getNumber());
         }
      }
      
      try {
         addElement (fem, e, elemId);
      } catch (Exception err) {
         System.out.println("element " + elemId + " caused a problem");
      }
      
      return e;
   }
   
   private void createQuadHex (
      FemModel3d fem, int[] nodeIds, int elemId) {

      FemNode3d n0 = fem.getNodeByNumber (nodeIds[0]);
      FemNode3d n1 = fem.getNodeByNumber (nodeIds[1]);
      FemNode3d n2 = fem.getNodeByNumber (nodeIds[2]);
      FemNode3d n3 = fem.getNodeByNumber (nodeIds[3]);
      FemNode3d n4 = fem.getNodeByNumber (nodeIds[4]);
      FemNode3d n5 = fem.getNodeByNumber (nodeIds[5]);
      FemNode3d n6 = fem.getNodeByNumber (nodeIds[6]);
      FemNode3d n7 = fem.getNodeByNumber (nodeIds[7]);
      FemNode3d n8 = fem.getNodeByNumber (nodeIds[8]);
      FemNode3d n9 = fem.getNodeByNumber (nodeIds[9]);
      FemNode3d n10 = fem.getNodeByNumber (nodeIds[10]);
      FemNode3d n11 = fem.getNodeByNumber (nodeIds[11]);
      FemNode3d n12 = fem.getNodeByNumber (nodeIds[12]);
      FemNode3d n13 = fem.getNodeByNumber (nodeIds[13]);
      FemNode3d n14 = fem.getNodeByNumber (nodeIds[14]);
      FemNode3d n15 = fem.getNodeByNumber (nodeIds[15]);
      FemNode3d n16 = fem.getNodeByNumber (nodeIds[16]);
      FemNode3d n17 = fem.getNodeByNumber (nodeIds[17]);
      FemNode3d n18 = fem.getNodeByNumber (nodeIds[18]);
      FemNode3d n19 = fem.getNodeByNumber (nodeIds[19]);
      
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

         if (!mySuppressWarnings && !cwQuadhexWarningGiven) {
            System.out.println ("WARNING: fixed orientation for some quadhex elements");
            cwQuadhexWarningGiven = true;
         }
         
         if (!mySuppressWarnings && e.computeVolumes () < 0) {
            System.out.println (
               "WARNING: negative volume for quadhex number " + e.getNumber());
         }
      }  
      
      addElement (fem, e, elemId);
   }

   private void createWedge (
      FemModel3d fem, int[] nodeIds, int elemId) {
      
      FemNode3d n0 = fem.getNodeByNumber (nodeIds[0]);
      FemNode3d n1 = fem.getNodeByNumber (nodeIds[1]);
      FemNode3d n2 = fem.getNodeByNumber (nodeIds[2]);
      FemNode3d n3 = fem.getNodeByNumber (nodeIds[3]);
      FemNode3d n4 = fem.getNodeByNumber (nodeIds[4]);
      FemNode3d n5 = fem.getNodeByNumber (nodeIds[5]);

      WedgeElement e = new WedgeElement (n0, n1, n2, n3, n4, n5);

      if (e.computeVolumes () < 0) {
         e = new WedgeElement (n2, n1, n0, n5, n4, n3);

         if (!mySuppressWarnings && !cwWedgeWarningGiven) {
            System.out.println ("WARNING: fixed orientation for some wedge elements");
            cwWedgeWarningGiven = true;
         }
         
         if (!mySuppressWarnings && e.computeVolumes () < 0) {
            System.out.println (
               "WARNING: negative volume for wedge number " + e.getNumber());
         }
      }
      
      addElement (fem, e, elemId);
   }
   
   private void createShellTri (
      FemModel3d fem, int[] nodeIds, int elemId, boolean isMembrane) {
      
      FemNode3d n0 = fem.getNodeByNumber (nodeIds[0]);
      FemNode3d n1 = fem.getNodeByNumber (nodeIds[1]);
      FemNode3d n2 = fem.getNodeByNumber (nodeIds[2]);
      
      ShellTriElement e =
         new ShellTriElement (
            n0, n1, n2, myShellThickness, isMembrane);
      
      addElement (fem, e, elemId);
   }
   
   private void createShellQuad (
      FemModel3d fem, int[] nodeIds, int elemId, boolean isMembrane) {
      
      FemNode3d n0 = fem.getNodeByNumber (nodeIds[0]);
      FemNode3d n1 = fem.getNodeByNumber (nodeIds[1]);
      FemNode3d n2 = fem.getNodeByNumber (nodeIds[2]);
      FemNode3d n3 = fem.getNodeByNumber (nodeIds[3]);
      
      ShellQuadElement e =
         new ShellQuadElement (
            n0, n1, n2, n3, myShellThickness, isMembrane);
      
      addElement (fem, e, elemId);
   }

   private int readFile (
      ReaderTokenizer rtok,
      LinkedHashMap<Integer, Point3d> nodeMap,
      LinkedHashMap<Integer, ElemDesc> elemMap) throws IOException {

      rtok.eolIsSignificant (true);
      rtok.wordChar('*');
      rtok.whitespaceChar(',');  //ignore commas

      FileSection mySection = FileSection.OTHER;
      ElemType myElemType = ElemType.UNKNOWN;
      
      int nodeId = 0;
      int elemId = 0;

      boolean warnedNumNodes = false;
      int maxNodeId = -1;
      
      while (rtok.nextToken () != ReaderTokenizer.TT_EOF) {
         
         // determine type
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (rtok.sval.startsWith(COMMENT)) {
               // ignore
            }
            else if (rtok.sval.charAt(0) == KEYWORD) {
               // potentially change mode
               rtok.whitespaceChar (',');
               
               String keyword = rtok.sval.substring(1);
               if (keyword.equalsIgnoreCase("NODE")) {
                  mySection = FileSection.NODE;
               }
               else if (keyword.equalsIgnoreCase("ELEMENT")) {
                  mySection = FileSection.ELEM;
                  
                  // determine type
                  String line = rtok.readLine();
                  myElemType = ElemType.UNKNOWN;
                  String type = parseKey("TYPE=", line);
                  if (type != null) {
                     type = type.toUpperCase();
                     for (ElemType et : ElemType.values()) {
                        if (type.startsWith (et.getString())) {
                           myElemType = et;
                           break;
                        }
                     }
                     if (!mySuppressWarnings && myElemType == ElemType.UNKNOWN) {
                        System.err.println(
                           "WARNING: unknown element type '" + type +
                           "', line " + rtok.lineno() + "; ignoring");
                     }
                  }
                  else if (!mySuppressWarnings) {
                     System.err.println(
                        "WARNING: element type not specified, line " +
                        rtok.lineno() + "; ignoring");
                  }
                  warnedNumNodes = false;
               }
               else if (keyword.equalsIgnoreCase("INCLUDE")){
                  String line = rtok.readLine();
                  String fileName = parseKey("INPUT=", line);
                  
                  // find file
                  File input = findFile(fileName, myIncludeDirs);
                  if (input == null) {
                     throw new IOException(
                        "Cannot find INCLUDE file '" + fileName + "'");
                  }
                  
                  ReaderTokenizer rt = null;
                  try {
                     rt = new ReaderTokenizer (
                        new BufferedReader (new FileReader(input)));
                     readFile (rt, nodeMap, elemMap);
                  } catch (IOException e) {
                     throw e;
                  } finally {
                     if (rt != null) {
                        rt.close();
                     }
                  }
               }
               else {
                  mySection = FileSection.OTHER;
                  if (!mySuppressWarnings) {
                     System.out.println (
                        "WARNING: ignoring section '" + keyword +
                        "', line " + rtok.lineno());
                  }
               }
            }
            toEOL(rtok);   // skip to end-of-line
            if (mySection == FileSection.ELEM) {
               // do *not* ignore commas in ELEM section. That's because a
               // comma tells us when we still expect an additional element
               // node number.
               rtok.ordinaryChar (',');
            }
         }
         else {
            rtok.pushBack();
            // action depends on mode
            switch (mySection) {
               case ELEM: {
                  if (myElemType != ElemType.UNKNOWN) {
                     rtok.nextToken();
                     if (!rtok.tokenIsInteger()) {
                        throw new IOException (
                           "Expecting element number at line start, got "+rtok);
                     }
                     elemId = (int)(myZeroBasedNumbering ? rtok.lval-1 : rtok.lval);
                     ArrayList<Integer> nodes = new ArrayList<Integer>();
                     // scan node numbers until one not followed by a ','
                     while (rtok.nextToken() == ',') {
                        rtok.nextToken();
                        if (rtok.ttype == ReaderTokenizer.TT_EOL) {
                           // skip to next line
                           rtok.nextToken();
                        }
                        if (rtok.tokenIsInteger()) {
                           if (myZeroBasedNumbering) {
                              nodes.add((int)(rtok.lval-1));
                           }
                           else {
                              nodes.add((int)(rtok.lval));
                           }
                        }
                        else {
                           throw new IOException (
                              "Expecting node number, got "+rtok);
                        } 
                     }
                     if (nodes.size() != myElemType.numNodes()) {
                        if (!mySuppressWarnings && !warnedNumNodes) {
                           System.out.println (
                              "WARNING: elementType "+myElemType+" expects "+
                              myElemType.numNodes()+" nodes; got "+nodes.size());
                           warnedNumNodes = true;
                        }
                     }
                     else {
                        ElemDesc edesc =
                           new ElemDesc (
                              myElemType, ArraySupport.toIntArray (nodes));
                        elemMap.put(elemId, edesc);
                     }
                  }
                  toEOL(rtok);
                  break;
               }
               case NODE: {
                  nodeId = rtok.scanInteger();
                  if (myZeroBasedNumbering) {
                     nodeId--;
                  }
                  double x = rtok.scanNumber();
                  double y = rtok.scanNumber();
                  double z = rtok.scanNumber();
                  nodeMap.put(nodeId, new Point3d(x,y,z));
                  if (nodeId > maxNodeId) {
                     maxNodeId = nodeId;
                  }
                  toEOL(rtok);
                  break;
               }
               case OTHER: {
                  toEOL(rtok);
                  break;
               }
            }
         }
      }
      return maxNodeId;
   }
   
   private File findFile(String fileName, File[] dirs) {
      
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
   
   private void toEOL(ReaderTokenizer rtok) throws IOException {
      while (rtok.ttype != ReaderTokenizer.TT_EOL &&  
         rtok.ttype != ReaderTokenizer.TT_EOF) {
         rtok.nextToken();
      }
      if (rtok.ttype == ReaderTokenizer.TT_EOF) {
         rtok.pushBack();
      }
   }
   
   private String readLine(ReaderTokenizer rtok) throws IOException {

      Reader rtokReader = rtok.getReader();
      String line = "";
      int c;
      StringBuilder sb = new StringBuilder();
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
         else if (c != '\r') {
            line += (char)c;
         }
      }
      System.out.println ("readLine '"+line+"'");
      return line;
   }
   
   
   private String parseKey(String keyName, String line) {
      
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
   
   private int findChar(String str, String charSet) {
      for (int i=0; i<str.length(); i++) {
         for (int j=0; j<charSet.length(); j++) {
            if (str.charAt(i)==charSet.charAt(j)) {
               return i;
            }
         }
      }
      return -1;
   }

   private static void printUsageAndExit() {
      System.out.println ("Usage: AbaqusReader [-help] <filename>]");
   }

   public static void main (String[] args) {
      String filename = null;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-help")) {
            printUsageAndExit();
         }
         else if (filename == null) {
            filename = args[i];
         }
         else {
            System.out.println ("Unknown option "+args[i]);
            printUsageAndExit();
         }
      }
      if (filename == null) {
         printUsageAndExit();
      }
      try {
         AbaqusReader reader = new AbaqusReader(filename);
         FemModel3d fem = reader.readFem (null);
         System.out.println (
            "numNodes=" + fem.numNodes() + " numElems=" + fem.numElements());
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   public void close() {
      if (myFile != null && myRtok != null) {
         myRtok.close();
         myRtok = null;
      }
   }

   protected void finalize() throws Throwable {
      super.finalize();
      close();
   }
   
}
