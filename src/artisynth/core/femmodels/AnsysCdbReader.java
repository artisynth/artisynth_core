/**
 * Copyright (c) 2024, by the Authors: John E Lloyd (UBC).
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.Collections;
import java.util.LinkedList;

import maspack.util.*;

/**
 * Reads FEM mesh geometry from Ansys {@code .cdb} files.
 */
public class AnsysCdbReader extends FemReaderBase {

   // map from Ansys hex node numbers to ArtiSynth node numbers:
   static int[] myHexNodeOrdering = new int[] {
      0, 3, 2, 1, 4, 7, 6, 5 };
   // map from Ansys quadhex node numbers to ArtiSynth node numbers:
   static int[] myQuadhexNodeOrdering = new int[] {
      0, 3, 2, 1, 4, 7, 6, 5, 11, 10, 9, 8, 15, 14, 13, 12, 16, 19, 18, 17 };

   int myEBlockCnt; // counts the number of EBLOCKS read
   // shell41 - quad membrane
   // shell28 - quad shell?

   // shell181 - 4 node structural, with node rotation
   // shell281 - 8 node structural, with node rotation

   // solid5 - 8 node hex, coupled field (has temp,volt,mag) at the nodes
   // solid98 - 10 node tet, coupled field (has temp,volt,mag) at the nodes
   // solid185 - 8 node hex, 3 dof nodes, wedge option; tet & pyr (not advised)
   // solid186 - 20 node hex, 3 dof nodes, tet, pyr and wedge options
   // solid187 - 10 node tet, 3 dof nodes
   // solid285 - 4 node tet, 4 dof nodes (pressure)

   // solid226 - 20 node hex, up to 6 dof per node - tet, pyr and wedge optionfebi

   // based on ansys to abaqus conversion (altair hyperworks, 2022)

   // shell43,shell63,shell181 -> S3
   // shell43,shell63,shell181 -> S4

   // shell181 - membrane if KEYOPT(1)=1
   // shell163 - membrane if KEYOPT(1)={5,9}
   // shell63 - membrane if KEYOPT(1)=1
   // shell41  - membrane shell

   // not structural: 69,70,96,97
   // not structural: 87,90,117

   // solid{45,62,64,69,70,96,97,164,185} -> C3D4 (tet)
   // solid{62,70,96,97,164} -> C3D8 (hex)
   // solid{5,45,62,64,69,70,96,97,164,185} -> C3D6 (wedge)
   // solid{62,70,96,97,164} -> C3D5 (pyramid)
   // solid{87,90,92,95,98,117,148,168,186,187,191} -> C3D10 (quad tet)
   // solid{90,95,117,186} -> C3D20 (quad pyramid as deg hex)
   // solid{90,95,117,147,186,191} -> C3D15 (quad wedge)
   // solid{90,95,117,147,186,191} -> C3D20 (quad hex)

   // returns the number of nodes for a give element number
   static HashMap<Integer,Integer> myNumNodesMap = new HashMap<>();
   static {
      // shell elements
      myNumNodesMap.put (28, 4);  // SHELL28
      myNumNodesMap.put (41, 4);  // SHELL41
      myNumNodesMap.put (43, 4);  // SHELL43
      myNumNodesMap.put (63, 4); // SHELL63
      myNumNodesMap.put (163, 4); // SHELL163
      myNumNodesMap.put (181, 4); // SHELL181

      // tet elements
      myNumNodesMap.put (285, 4); // SOLID285

      // quad tet elements
      myNumNodesMap.put (98, 10);  // SOLID98
      myNumNodesMap.put (92, 10);  // SOLID92
      myNumNodesMap.put (148, 10);  // SOLID148
      myNumNodesMap.put (168, 10);  // SOLID168
      myNumNodesMap.put (187, 10);  // SOLID187

      // hex elements
      myNumNodesMap.put (5, 8);   // SOLID5
      myNumNodesMap.put (45, 8);   // SOLID45
      myNumNodesMap.put (62, 8);   // SOLID62  magneto-structural
      myNumNodesMap.put (65, 8);   // SOLID65
      myNumNodesMap.put (164, 8);   // SOLID164
      myNumNodesMap.put (185, 8);   // SOLID185

      // quad hex elements
      myNumNodesMap.put (95, 20);    // SOLID95
      myNumNodesMap.put (147, 20);   // SOLID147
      myNumNodesMap.put (186, 20);   // SOLID186
      myNumNodesMap.put (226, 20);   // SOLID226
   };

   boolean isAnsysShellElem (int elemNum) {
      switch (elemNum) {
         case 28:
         case 41:
         case 43:
         case 63:
         case 163:
         case 181: {
            return true;
         }
         default: {
            return false;
         }
      }
   }

   /**
    * Stores the keyopts for a particular Ansys element.
    */
   class AnsysElemType {
      int myNumber; // ANSYS element number
      DynamicIntArray myKeyopts; 

      AnsysElemType (int number) {
         myNumber = number;
         myKeyopts = new DynamicIntArray();
      }

      /**
       * Set keyopt using one-based key.
       */
      int getKeyopt (int key) {
         return (key <= myKeyopts.size() ? myKeyopts.get(key-1) : 0);
      }

      /**
       * Get keyopt using one-based key.
       */
      void setKeyopt (int key, int value) {
         while (key > myKeyopts.size()) {
            myKeyopts.add (0);
         }
         myKeyopts.set (key-1, value);
      }

      ShellType getShellType() {
         boolean membrane = false;
         switch (myNumber) {
            case 181:
            case 63: {
               membrane = (getKeyopt(1) == 1);
               break;
            }
            case 163: {
               int keyopt1 = getKeyopt(1);
               membrane = (keyopt1 == 5 || keyopt1 == 9);
               break;
            }
            case 41: {
               membrane = true;
               break;
            }      
         }
         return membrane ? ShellType.MEMBRANE : ShellType.SHELL;
      }
   }

   HashMap<Integer,AnsysElemType> myElemTypeMap =
      new HashMap<Integer,AnsysElemType>();
   int myElemType = -1;

   static ElementNodeNumbering myNodeNumbering;
   static {
      ElementNodeNumbering numbering = new ElementNodeNumbering();

      // numberings are the same as ArtiSynth except for the following:
      numbering.set (
         ElemType.HEX, new int[] { 0, 3, 2, 1, 4, 7, 6, 5 });
      numbering.set (
         ElemType.QUADTET, new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 9, 8 });
      numbering.set (
         ElemType.QUADHEX, new int[] {
            0, 3, 2, 1, 4, 7, 6, 5, 11, 8, 16, 10, 19, 9, 18, 17, 15, 12, 14,13});
      numbering.set (
         ElemType.QUADWEDGE, new int[] {
            0, 1, 2, 3, 4, 5, 6, 8, 12, 7, 13, 14, 9, 11, 10 });
      numbering.set (
         ElemType.QUADPYRAMID, new int[] {
            0, 1, 2, 3, 4, 5, 8, 9, 6, 10, 7, 11, 12 });

      myNodeNumbering = numbering;
   }
   
   /**
    * Creates a reader to read from a specified file. The actual
    * reading of the FEM geometry is done using {@link #readFem}.
    * 
    * @param file file to read from
    * @throws IOException if an I/O error occurred
    */
   public AnsysCdbReader (File file) throws IOException {
      super (file);
   }
   
   /**
    * Creates a reader to read from a specified file. The actual reading of the
    * FEM geometry is done using {@link #readFem}.
    * 
    * @param filePath path name of the file to read from
    * @throws IOException if an I/O error occurred
    */
   public AnsysCdbReader (String filePath) throws IOException {
      super (new File(filePath));
   }
   
   /**
    * Creates a reader to read from a specified Reader. The actual reading of
    * the FEM geometry is done using {@link #readFem}.
    * 
    * @param reader reader to read from
    * @throws IOException if an I/O error occurred
    */
   public AnsysCdbReader (Reader reader) throws IOException {
      super (reader);
   }
   
   /**
    * Creates an FEM and reads its geometry from a specifed file.
    *
    * @param filePath path name of the file to read from
    * @return created FEM model
    * @throws IOException if an I/O error occurred
    */
   public static FemModel3d read (String filePath) throws IOException {
      return read (new File(filePath));
   }
  
   /**
    * Creates an FEM and reads its geometry from a specifed file.
    * 
    * @param file file to read from
    * @return created FEM model
    * @throws IOException if an I/O error occurred
    */
   public static FemModel3d read (File file) throws IOException {
      return read (null, file, 0);
   }

   /**
    * Reads the geometry for an FEM model from a specified file.
    *
    * @param fem FEM model to be populated by geometry data. If
    * <code>null</code>, a new model is created
    * @param file file to read from
    * @param options flags controlling how the input is read.
    * Current flags include {@link #PRESERVE_NUMBERING},
    * {@link #PRESERVE_NUMBERING_BASE0}, and
    * {@link #SUPPRESS_WARNINGS}.
    * @return created FEM model
    * @throws IOException if an I/O error occurred
    */
   public static FemModel3d read (FemModel3d fem, File file, int options)
      throws IOException {
      if (fem == null) {
         fem = new FemModel3d();
      }
      AnsysCdbReader femReader = new AnsysCdbReader (file);
      femReader.setOptionsFromFlags (options);
      try {
         femReader.readFem (fem);
         return fem;
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         femReader.close();
      }
   }
  
   /**
    * Reads an FEM from a specifed file.
    * 
    * @param fem FEM whose geometry should be read from the file. If {@code
    * null}, then an FEM is created.
    * @param file file to read from
    * @param shellThickness default thickness to use for shell or membrane
    * elements, if not specified in the file
    * @param shellType default shell type to use, if not specified in the file
    * @param options option flags. Flags include {@link #PRESERVE_NUMBERING}
    * and {@link #PRESERVE_NUMBERING_BASE0}.
    * @return FEM that was read
    * @throws IOException if an I/O error occurred
    */
   public static FemModel3d read (
      FemModel3d fem, File file,
      double shellThickness, ShellType shellType, int options) 
      throws IOException {
      if (fem == null) {
         fem = new FemModel3d();
      }
      AnsysCdbReader femReader = new AnsysCdbReader (file);
      femReader.setShellThickness (shellThickness);
      femReader.setShellType (shellType);
      femReader.setOptionsFromFlags (options);
      try {
         femReader.readFem (fem);
         return fem;
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         femReader.close();
      }
   }
  
   /**
    * Reads an FEM from the file or Reader associated with this FEM reader.
    * 
    * @param fem FEM whose geometry should be read. If {@code
    * null}, then an FEM is created.
    * @return FEM that was read
    * @throws IOException if an I/O error occurred
    */
   public FemModel3d readFem (FemModel3d fem)
      throws IOException {

      if (fem == null) {
         fem = new FemModel3d();
      }
      if (myNumbering == Numbering.PRESERVE) {
         fem.setOneBasedNodeElementNumbering(true);
      }
      ReaderTokenizer rtok = new ReaderTokenizer (myReader);
      rtok.eolIsSignificant(true);
      myElemTypeMap.clear();
      myElemType = -1;
      myEBlockCnt = 0;

      // XXX not sure if EBLOCK can preceed NBLOCK so we
      // will assume not.
      HashMap<Integer,FemNode3d> nodeMap = new HashMap<>();
      // collect all read nodes into a list of nodes
      ArrayList<NodeDesc> nodeDescs = new ArrayList<>();
      ArrayList<ElemDesc> elemDescs = new ArrayList<>();
      
      // read model info from file
      do {
         if (rtok.nextToken() == ReaderTokenizer.TT_WORD) {
            String cmd = rtok.sval.toUpperCase();
            switch (cmd) {
               case "NBLOCK": {
                  //System.out.println ("reading NBLOCK at " + rtok.lineno());
                  readNblock (rtok, nodeDescs, nodeMap);
                  break;
               }
               case "EBLOCK": {
                  //System.out.println ("reading EBLOCK at " + rtok.lineno());
                  readEblock (rtok, elemDescs, nodeMap);
                  myEBlockCnt++;
                  break;
               }
               case "ETBLOCK": {
                  //System.out.println ("reading ETBLOCK at " + rtok.lineno());
                  readEtBlock (rtok);
                  break;
               }
               case "ET": {
                  //System.out.println ("parsing ET at line " + rtok.lineno());
                  parseEtCommand (rtok);
                  break;
               }
               case "KEYOPT":
               case "KEYOP": {
                  //System.out.println ("parsing KEYOPT at line "+rtok.lineno());
                  parseKeyoptCommand (rtok);
                  break;
               }
               case "TYPE": {
                  //System.out.println ("parsing TYPE at line " + rtok.lineno());
                  parseTypeCommand (rtok);
                  break;
               }
               default: {
                  rtok.skipLine(); // skip rest of line
               }
            }
         }
         else if (rtok.ttype != ReaderTokenizer.TT_EOL) {
            rtok.skipLine(); // skip line
         }
      }
      while (rtok.ttype != ReaderTokenizer.TT_EOF);

      // build the model
      
      // add the nodes. Do we want to sort them by number?
      for (NodeDesc nd : nodeDescs) {
         addNode (fem, nd.myNode, nd.myTag);
      }
      // add the elements. Sort them first if there was more than one EBLOCK
      if (myEBlockCnt > 1) {
         Collections.sort (
            elemDescs,
            (e0, e1) -> (e0.myTag<e1.myTag ? -1 : (e0.myTag==e1.myTag ? 0 : 1)));
      }
      for (ElemDesc ed : elemDescs) {
         createAndAddElement (fem, ed);
      }
      myLineCnt = rtok.lineno();
      return fem;
   }

   protected void readNblock (
      ReaderTokenizer rtok, ArrayList<NodeDesc> nodeDescs,
      HashMap<Integer,FemNode3d> nodeMap) throws IOException {

      rtok.scanToken (',');
      int numFields = rtok.scanInteger(); // number of fields per node
      rtok.scanToken (',');
      String[] args = rtok.readLine().trim().split (",");
      if (args.length < 3) {
         throw new IOException (
            "NBLOCK command: expecting >= 4 arguments, line " + rtok.lineno());
      }
      String key = args[0].trim();
      boolean solid = (key.equalsIgnoreCase ("SOLID"));
      int maxNodeNum = parseNonNegInteger (args[1]);
      if (maxNodeNum < 0) {
         throw new IOException (
            "NBLOCK command: expecting max node number, got '"+args[1]+
            "', line "+rtok.lineno());
      }
      int numNodes = parseNonNegInteger (args[2]);
      if (numNodes < 0) {
         throw new IOException (
            "NBLOCK command: expecting number of nodes, got '"+args[2]+
            "', line "+rtok.lineno());
      }
      rtok.skipLine(); // ignore format line
      double[] xyz = new double[3];
      for (int i=0; i<numNodes; i++) {
         int nodeId = rtok.scanInteger();
         if (solid) {
            rtok.scanInteger(); // solid model entity - ignore
            rtok.scanInteger(); // line location - ignore
         }
         int k = 0;
         while (rtok.nextToken() != ReaderTokenizer.TT_EOL) {
            if (k < 3) {
               if (!rtok.tokenIsNumber()) {
                  throw new IOException (
                     "NBLOCK command: expected node coordinate, got "+rtok);
               }
               xyz[k++] = rtok.nval;
            }
         }
         // zero any missing coords
         while (k < 3) {
            xyz[k++] = 0;
         }
         FemNode3d node = new FemNode3d (
            myScaling.x*xyz[0], myScaling.y*xyz[1], myScaling.z*xyz[2]);
         nodeDescs.add (new NodeDesc (node, nodeId));
         nodeMap.put (nodeId, node);
      }
      String endLine = rtok.readLine().trim();
      if (!endLine.startsWith ("N,")) {
         throw new IOException (
            "NBLOCK command: terminating N command missing, line "+rtok.lineno());
      }
   }

   /**
    * Try to get the number of nodes from the element number.
    */
   private int findNumNodesFromElemNum (int elemNum) {
      Integer ival = myNumNodesMap.get(elemNum);
      if (ival != null) {
         return ival;
      }
      return -1;
   }

   /**
    * Try to get the number of nodes from the element type.
    */
   private int findNumNodesFromElemType (int elemType) {
      AnsysElemType et = myElemTypeMap.get(elemType);
      if (et != null) {
         return findNumNodesFromElemNum (et.myNumber);
      }
      return -1;
   }

   private int getElemNumber (int elemTypeNum) {
      AnsysElemType et = myElemTypeMap.get(elemTypeNum);
      if (et != null) {
         return et.myNumber;
      }
      else {
         return -1; 
      }
   }   

   protected void readEblock (
      ReaderTokenizer rtok, ArrayList<ElemDesc> elemDescs,
      HashMap<Integer,FemNode3d> nodeMap) throws IOException {

      rtok.scanToken (',');
      int numReal = rtok.scanInteger(); // number of real ints to read
      rtok.scanToken (',');
      String[] args = rtok.readLine().trim().split (",");
      if (args.length < 3) {
         throw new IOException (
            "EBLOCK command: expecting >= 4 arguments, line " + rtok.lineno());
      }
      String key = args[0].trim();
      boolean solid = key.equalsIgnoreCase("SOLID");
      boolean compact = key.equalsIgnoreCase("COMPACT");
      int numElems = parseNonNegInteger (args[2]);
      if (numElems < 0) {
         throw new IOException (
            "EBLOCK command: expecting number of elements, got '"+args[2]+
            "', line "+rtok.lineno());
      }
      rtok.skipLine(); // ignore format line
      int elemTypeNum, numNodes = 0, elemId;
      int[] nodeNums = new int[20];
      int[] condensedNums = new int[20];
      // nodePresent is used to condence node numbers:
      HashSet<Integer> nodePresent = new HashSet<>();
      for (int i=0; i<numElems; i++) {
         AnsysElemType aetype = null;
         if (solid) {
            rtok.scanInteger(); // material number
            elemTypeNum = rtok.scanInteger(); // element type
            rtok.scanInteger(); // real constant; ignore
            rtok.scanInteger(); // section ID
            rtok.scanInteger(); // element coordinate system
            rtok.scanInteger(); // birth/death flag
            rtok.scanInteger(); // solid model reference number
            rtok.scanInteger(); // elem shape flag
            numNodes = rtok.scanInteger(); // number of nodes
            rtok.scanInteger(); // exclude key
            elemId = rtok.scanInteger(); // element id
            aetype = myElemTypeMap.get(elemTypeNum);
            if (aetype == null) {
               System.out.println (
                  "WARNING: Undefined element tyoe number "+elemTypeNum);
            }
            else {
               int nnodes = findNumNodesFromElemNum (aetype.myNumber);
               if (nnodes == -1) {
                  System.out.println (
                     "WARNING: Unknown element number "+aetype.myNumber);
               }
               else if (nnodes != numNodes) {
                  System.out.println ("WARNING: fum nodes DO NOT MATCH");
               }
            }
         }
         else {
            if (myElemType == -1) {
               throw new IOException (
                  "EBLOCK: element type number has not been specified");
            }
            elemTypeNum = myElemType;
            // if myElemType != -1, we know it has an entry in myElemTypeMap
            aetype = myElemTypeMap.get(elemTypeNum);
            // try to get the number of nodes from the element type
            numNodes = findNumNodesFromElemNum (aetype.myNumber);
            if (numNodes == -1) {
               throw new IOException (
                  "EBLOCK: unsupported element number " + aetype.myNumber);
            }
            if (compact) {
               elemId = rtok.scanInteger(); // element id
            }
            else {
               elemId = rtok.scanInteger(); // element id
               rtok.scanInteger(); // section ID
               rtok.scanInteger(); // real constant; ignore
               rtok.scanInteger(); // material number
               rtok.scanInteger(); // element coordinate system
            }
         }

         int k = 0;
         while (rtok.nextToken() != ReaderTokenizer.TT_EOL) {
            if (k < numNodes) {
               if (!rtok.tokenIsInteger()) {
                  throw new IOException (
                     "EBLOCK command: expecting node number, got "+rtok);
               }
               nodeNums[k++] = (int)rtok.lval;
            }
         }
         if (k < numNodes) {
            // will continue on next line
            while (k < numNodes) {
               nodeNums[k++] = rtok.scanInteger();
            }
            rtok.skipLine(); // ignore anything else on line
         }
         ElemType etype = null;
         int[] nodeIds = nodeNums;
         switch (numNodes) {
            case 4: {
               etype = infer4NodeElem (nodeNums, getElemNumber(elemTypeNum));
               break;
            }
            case 10: {
               etype = ElemType.QUADTET;
               break;
            }
            case 8:
            case 20: {
               etype = condenseNodeNums (
                  condensedNums, nodeNums, numNodes, nodePresent);
               nodeIds = condensedNums;
               break;
            }
            default: {
               String errMsg = "Unsupported Ansys element node count "+numNodes;
               int elemNum = getElemNumber(elemTypeNum);
               if (elemNum != -1) {
                  errMsg += ", element number "+elemNum;
               }
               throw new IOException (errMsg);
            }
         }
         int[] ordering = null;
         if (etype == ElemType.HEX) {
            ordering = myHexNodeOrdering;
         }
         else if (etype == ElemType.QUADHEX) {
            ordering = myQuadhexNodeOrdering;
         }
         FemNode3d[] nodes = createNodes (etype, nodeIds, ordering, nodeMap);
         ShellType shellType = null;
         if (etype.isShell() && aetype != null) {
            shellType = aetype.getShellType();
         }
         ElemDesc edesc = new ElemDesc (
            etype, nodes, shellType, elemId);
         elemDescs.add (edesc);
      }
      rtok.nextToken();
      if (!rtok.tokenIsInteger() || rtok.lval != -1) {
         throw new IOException (
            "EBLOCK command: missing -1 terminator at line "+rtok.lineno());
      }
      rtok.skipLine();      
   }

   ElemType infer4NodeElem (int[] nodeNums, int elemNum) {
      if (elemNum == 285) {
         // tet element
         return ElemType.TET;
      }
      else {
         // shell element
         if (nodeNums[2] == nodeNums[3]) {
            // tri element
            return ElemType.SHELL_TRI;
         }
         else {
            // quad element                  
            return ElemType.SHELL_QUAD;
         }
      }
   }

   ElemType condenseNodeNums (
      int[] condensedNums, int[] nodeNums, int numNodes,
      HashSet<Integer> nodePresent) {

      nodePresent.clear();
      int k = 0;
      for (int i=0; i<numNodes; i++) {
         int num = nodeNums[i];
         if (!nodePresent.contains(num)) {
            condensedNums[k++] = num;
            nodePresent.add (num);
         }
      }
      switch (k) {
         case 4: return ElemType.TET;
         case 5: return ElemType.PYRAMID;
         case 6: return ElemType.WEDGE;
         case 8: return ElemType.HEX;
         case 10: return ElemType.QUADTET;
         case 13: return ElemType.QUADPYRAMID;
         case 15: return ElemType.QUADWEDGE;
         case 20: return ElemType.QUADHEX;
         default: {
            return null;
         }
      }
   }

   String[] splitEtBlockField (String line, int[] fieldLens) {
      ArrayList<String> entries = new ArrayList<>();
      int linelen = line.length();
      int beginIdx = 0;
      if (linelen != 0) {
         for (int i=0; i<fieldLens.length; i++) {
            int len = fieldLens[i];
            int endIdx = Math.min (beginIdx+len, linelen);
            if (beginIdx >= endIdx) {
               // line ended sooner than expected
               break;
            }
            entries.add (line.substring (beginIdx, endIdx));
            beginIdx += len;
         }
      }
      return entries.toArray (new String[0]);
   }

   protected void readEtBlock (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken (',');
      int numTypes = rtok.scanInteger();
      rtok.scanToken (',');
      int maxType = rtok.scanInteger();
      rtok.skipLine(); // just in case there's anything else on the line
      String format = rtok.readLine();
      int[] fieldLens = parseFormat (format);
      if (fieldLens == null) {
         throw new IOException (
            "ETBLOCK command: can't parse format '"+format+
            "' at line "+rtok.lineno());
      }
      for (int i=0; i<numTypes; i++) {
         String[] entries = splitEtBlockField (rtok.readLine(), fieldLens);
         if (entries.length < 2) {
            throw new IOException (
               "ETBLOCK command: expecting minimum two entries per field, line "+
               rtok.lineno());
         }
         int typeNum = parseNonNegInteger (entries[0]);
         if (typeNum < 0) {
            throw new IOException (
               "ETBLOCK command: expecting type num, got '"+
               entries[0]+"', line " + rtok.lineno());
         }
         int elemNum = parseElementNumber (entries[1]);
         if (elemNum < 0) {
            throw new IOException (
               "ETBLOCK command: expecting element name or number, got '"+
               entries[1]+"', line " + rtok.lineno());
         }
         AnsysElemType et = myElemTypeMap.get(typeNum);
         if (et == null) {
            et = new AnsysElemType (elemNum);
            myElemTypeMap.put (typeNum, et);
         }
         // check next 18 fields for key opt values
         for (int k=0; k<Math.min(18, entries.length-2); k++) {
            int key = k+1;
            try {
               int opt = Integer.parseInt(entries[k+2].trim());
               et.setKeyopt (key, opt);
            }
            catch (Exception e) {
               // ignore
            }
         }
      }
      rtok.nextToken();
      if (!rtok.tokenIsInteger() || rtok.lval != -1) {
         throw new IOException (
            "ETBLOCK command: missing -1 terminator at line "+rtok.lineno());
      }
      rtok.skipLine();      
   }

   protected void parseEtCommand (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken (',');
      int typeNum = rtok.scanInteger();
      rtok.nextToken();
      if (rtok.ttype == ReaderTokenizer.TT_EOL) {
         if (myElemTypeMap.get(typeNum) == null) {
            throw new IOException (
               "ET command: element type num "+typeNum+" is not defined");
         }
         myElemType = typeNum;         
         return;
      }
      else if (rtok.ttype != ',') {
         throw new IOException (
            "ET command: expected ',' but got "+rtok);
      }
      // rest of line contains elemNum and keyopts
      String[] args = rtok.readLine().trim().split(",");
      int elemNum = parseElementNumber (args[0]);
      if (elemNum < 0) {
         throw new IOException (
            "ET command: expected element number or name, got "+rtok);
      }
      AnsysElemType et = myElemTypeMap.get(typeNum);
      if (et == null) {
         et = new AnsysElemType (elemNum);
         myElemTypeMap.put (typeNum, et);
      }
      myElemType = typeNum;         
      // there should be at most 6 keyopts
      for (int i=0; i<Math.min(6,args.length-1); i++) {
         int key = i+1;
         try {
            int opt = Integer.parseInt(args[key].trim());
            et.setKeyopt (key, opt);
         }
         catch (Exception e) {
            // ignore
         }
      }
   }

   protected void parseKeyoptCommand (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken (',');
      int typeNum = 0;
      rtok.nextToken();
      if (rtok.tokenIsInteger()) {
         typeNum = (int)rtok.lval;
      }
      else if (rtok.tokenIsWord()) {
         // special word target; ignore
         rtok.skipLine();
         return;
      }
      else {
         throw new IOException (
            "KEYOPT command: expected type number or label, got "+rtok);
      }
      rtok.scanToken (',');
      int key = rtok.scanInteger();
      rtok.scanToken (',');
      int opt = rtok.scanInteger();
      rtok.skipLine(); // just in case there's some else on the line
      AnsysElemType et = myElemTypeMap.get(typeNum);
      if (et == null) {
         throw new IOException (
            "KEYOPT command: type number "+typeNum+" undefined");
      }
      et.setKeyopt (key, opt);
   }

   protected void parseTypeCommand (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken (',');
      int typeNum = rtok.scanInteger();
      rtok.skipLine(); // just in case there's some else on the line
      AnsysElemType et = myElemTypeMap.get(typeNum);
      if (et == null) {
         throw new IOException (
            "TYPE command: type number "+typeNum+" undefined");
      }
      myElemType = typeNum;
   }

   static int parseNonNegInteger (String str) {
      int value = -1;
      try {
         value = Integer.parseInt(str.trim());
      }
      catch (Exception e) {
      }
      return value;
   }

   /**
    * Parses an element name or number and returns its number. Returns -1 if
    * the name does not terminate in an integer.
    */
   static int parseElementNumber (String nameOrNum) {
      int elemNum = 0;
      int len = nameOrNum.length();
      int idx = 0;
      while (idx<len && !Character.isDigit(nameOrNum.charAt(idx))) {
         idx++;
      }
      if (idx == len) {
         return -1;
      }
      while (idx<len && Character.isDigit(nameOrNum.charAt(idx))) {
         elemNum = 10*elemNum + (nameOrNum.charAt(idx)-'0');
         idx++;
      }
      return elemNum;
   }

   /**
    * Parse a format string and return an array giving the length of each
    * field. If the format does not parse correctly, returns null.
    */
   static int[] parseFormat(String str) {
      DynamicIntArray fieldLengths = new DynamicIntArray();
      str = str.trim();
      int slen = str.length();
      if (slen < 2 || str.charAt(0) != '(' || str.charAt(slen-1) != ')') {
         return null;
      }
      str = str.substring (1,slen-1);
      String[] descs = str.split(",");
      for (String desc : descs) {
         desc = desc.trim();
         int dlen = desc.length();
         int numFields = 0;
         int fieldLength = 0;
         if (dlen == 0 || !Character.isDigit(desc.charAt(0))) {
            return null;
         }
         int idx = 0;
         while (idx < dlen && Character.isDigit(desc.charAt(idx))) {
            numFields = numFields*10 + (desc.charAt(idx)-'0');
            idx++;
         }
         idx++; // skip format character
         if (idx >= dlen || !Character.isDigit(desc.charAt(idx))) {
            return null;
         }
         while (idx < dlen && Character.isDigit(desc.charAt(idx))) {
            fieldLength = fieldLength*10 + (desc.charAt(idx)-'0');
            idx++;
         }
         for (int i=0; i<numFields; i++) {
            fieldLengths.add (fieldLength);
         }
      }
      return fieldLengths.toArray();
   }

   public static void main (String[] args) throws IOException {
      if (args.length == 0) {
         System.out.println ("Arguments: <cdbFile>");
         System.exit(1);
      }
      String fileName = args[0];
      FemModel3d fem = AnsysCdbReader.read (fileName);
   }
}
