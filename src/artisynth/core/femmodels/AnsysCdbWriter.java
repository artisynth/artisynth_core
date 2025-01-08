/**
 * Copyright (c) 2024, by the Authors: John E Lloyd (UBC), Isaac McKay (USASK).
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;

import artisynth.core.modelbase.ComponentList;
import artisynth.core.femmodels.FemElement.ElementClass;
import maspack.matrix.Point3d;
import maspack.util.*;

/**
 * Writes FEM mesh geometry to an Ansys {@code .cdb} file.
 */
public class AnsysCdbWriter extends FemWriterBase {

   // map from Ansys hex node numbers to ArtiSynth node numbers:
   static int[] myHexNodeOrdering = new int[] {
      0, 3, 2, 1, 4, 7, 6, 5 };
   // map from Ansys quadhex node numbers to ArtiSynth node numbers:
   static int[] myQuadhexNodeOrdering = new int[] {
      0, 3, 2, 1, 4, 7, 6, 5, 11, 10, 9, 8, 15, 14, 13, 12, 16, 19, 18, 17 };

   // element node patterns for condensed elements
   static int[] myTetNodePattern = new int[] {
      0, 1, 2, 2, 3, 3, 3, 3 };
   static int[] myPyramidNodePattern = new int[] {
      0, 1, 2, 3, 4, 4, 4, 4 };
   static int[] myWedgeNodePattern = new int[] {
      0, 1, 2, 2, 3, 4, 5, 5 };

   static int[] myQuadpyramidNodePattern = new int[] {
      0, 1, 2, 3, 4, 4, 4, 4, 5, 6, 7, 8, 4, 4, 4, 4, 9, 10, 11, 12 };
   static int[] myQuadwedgeNodePattern = new int[] {
      0, 1, 2, 2, 3, 4, 5, 5, 6, 7, 2, 8, 9, 10, 5, 11, 12, 13, 14, 14 };

   static int[] myShellTriNodePattern = new int[] {
      0, 1, 2, 2 };

   static private double INF = Double.POSITIVE_INFINITY;

   /**
    * Controls how EBLOCKs are written
    */
   public enum EBlockKey {
      /**
       * solid format, 19 fields per first line for each element
       */
      SOLID(8),

      /**
       * compact format, only element id and nodes specified
       */
      COMPACT(10),

      /**
       * blank format, 15 field per first line for each element
       */
      BLANK(10);

      int myMaxFirstLineNodes;

      EBlockKey (int nflnodes) {
         myMaxFirstLineNodes = nflnodes;
      }

      int maxFirstLineNodes() {
         return myMaxFirstLineNodes;
      }
      
   };

   boolean myUseEtBlock = false;
   EBlockKey myEBlockKey = EBlockKey.SOLID;
   
   public static String version = "4.1";

   public enum AnsysType {
      SOLID285(285,4,true),            // 4 node tet
      SOLID185(185,8,true),            // 8 node hex
      SOLID187(187,10,true),           // 10 node tet
      SOLID186(186,20,true),           // 20 node hex
      SHELL181(181,4,false),           // 4 node shell quad, regular
      SHELL181_MEMBRANE(181,4,false);  // 4 node shell quad, membrane

      AnsysType(int elemNum, int numNodes, boolean volumetric) {
         myElemNum = elemNum;
         myNumNodes = numNodes;
         myVolumetricP = volumetric;
      }

      int myElemNum;
      int myNumNodes;
      boolean myVolumetricP;

      int getElemNumber() {
         return myElemNum;
      }

      int numNodes() {
         return myNumNodes;
      }

      int getTypeNumber() {
         return ordinal()+1;
      }

      static int numTypes() {
         return values().length;
      }

      boolean isVolumetric() {
         return myVolumetricP;
      }
   }

   /**
    * Creates an AnsysCdbWriter to write to a specified PrintWriter. The actual
    * writing of the FEM geometry is done by {@link #writeFem}.
    * 
    * @param pw writer to write to
    * @throws IOException if an I/O error occurred
    */
   public AnsysCdbWriter (PrintWriter pw) throws IOException {
      super (pw);
   }

   /**
    * Creates an AnsysCdbWriter to write to a specified file. The actual
    * writing of the FEM geometry is done by {@link #writeFem}.
    * 
    * @param file file to write to
    * @throws IOException if an I/O error occurred
    */
   public AnsysCdbWriter (File file) throws IOException {
      super (file);
   }

   /**
    * Creates an AnsysCdbWriter to write to a specified file. The actual
    * writing of the FEM geometry is done by {@link #writeFem}.
    * 
    * @param filePath path name of the file to write to
    * @throws IOException if an I/O error occurred
    */
   public AnsysCdbWriter (String filePath) throws IOException {
      super (new File(filePath));
   }

   /**
    * Queries whether the ETBLOCK command is used for defining
    * element types.
    *
    * @return {@code true} if ETBLOCK command is used
    */
   public boolean getUseEtBlock() {
      return myUseEtBlock;
   }

   /**
    * Sets whether the ETBLOCK command is used for defining element types.
    * The default value if {@code false}.
    *
    * @param enable if {@code true}, the ETBLOCK command will be used
    */
   public void setUseEtBlock (boolean enable) {
      myUseEtBlock = enable;
   }

   /**
    * Queries the key used for writing EBLOCKs.
    *
    * @return EBLOCK key
    */
   public EBlockKey getEBlockKey() {
      return myEBlockKey;
   }

   /**
    * Sets the key used for writing EBLOCKs. The default value is {@link
    * EBlockKey#SOLID}.
    *
    * @param key key for writing EBLOCKs
    */
   public void setEBlockKey (EBlockKey key) {
      if (key == null) {
         throw new IllegalArgumentException ("argument 'key' is null");
      }
      myEBlockKey = key;
   }

   /**
    * Writes FEM model geometry to a specified file.
    *
    * @param filePath path name of the file to write to
    * @param fem FEM model to be written
    * @throws IOException if an I/O error occurred
    */
   public static void write (String filePath, FemModel3d fem)
      throws IOException {
      write (new File (filePath), fem);
   }
   
   /**
    * Writes FEM model geometry to a specified file.
    *
    * @param file file to write to
    * @param fem FEM model to be written
    * @throws IOException if an I/O error occurred
    */
   public static void write (File file, FemModel3d fem)
      throws IOException {
      AnsysCdbWriter writer = new AnsysCdbWriter (file);
      try {
         writer.writeFem (fem);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         writer.close();
      }
   }


   /**
    * Find the type of Ansys element to implement a given ArtiSynth volumetric
    * element.
    */
   AnsysType getAnsysType (FemElement3d elem) throws IOException {
      switch (elem.numNodes()) {
         case 4: {
            return AnsysType.SOLID285;
         }
         case 5:
         case 6:
         case 8: {
            return AnsysType.SOLID185;
         }
         case 10: {
            return AnsysType.SOLID187;
         }
         case 13:
         case 15:
         case 20: {
            return AnsysType.SOLID186;
         }
         default: {
            throw new IOException (
               "Unsupported element "+elem);
         }
      }
   }
   /**
    * Find the type of Ansys element to implement a given ArtiSynth volumetric
    * element.
    */
   AnsysType getAnsysType (ShellElement3d elem) {
      if (elem.getElementClass() == ElementClass.MEMBRANE) {
         return AnsysType.SHELL181_MEMBRANE;
      }
      else {
         return AnsysType.SHELL181;
      }
   }

   void extractElemNodeIds (
      int[] nodeIds, FemElement3dBase elem, AnsysType atype, int nodeNumInc) {

      ElemType etype = ElemType.getType(elem);      
      FemNode3d[] nodes = elem.getNodes();

      // Depending on the element, ordering is used to reorder nodes (for hex
      // and quadhex), or to create the expanded node set for condensed
      // elements (wedge, pyramid, quadwedge, quadpyramid)
      int[] ordering = null;
      switch (etype) {
         case SHELL_TRI: ordering = myShellTriNodePattern; break;
         case PYRAMID: ordering = myPyramidNodePattern; break;
         case WEDGE: ordering = myWedgeNodePattern; break;
         case HEX: ordering = myHexNodeOrdering; break;
         case QUADPYRAMID: ordering = myQuadpyramidNodePattern; break;
         case QUADWEDGE: ordering = myQuadwedgeNodePattern; break;
         case QUADHEX: ordering = myQuadhexNodeOrdering; break;
         default: {
            // no ordering
         }
      }
      if (ordering != null) {
         for (int i=0; i<atype.numNodes(); i++) {
            nodeIds[i] = nodes[ordering[i]].getNumber() + nodeNumInc;
         }
      }
      else {
         // in this case, elem.numNodes() == atype.numNodes()
         for (int i=0; i<atype.numNodes(); i++) {
            nodeIds[i] = nodes[i].getNumber() + nodeNumInc;
         }
      }
   }

   /**
    * Writes FEM model geometry to the writer or file associates with this
    * AnsysCdbWriter.
    *
    * @param fem FEM model to be written
    * @throws IOException if an I/O error occurred
    */
   public void writeFem (FemModel3d fem) throws IOException {
      
      PrintWriter pw = myPrintWriter;

      pw.println ("/COM,ArtiSynth AnsysCdbWriter 2024");
      pw.println ("/PREP7");
      pw.println ("/NOPR");

      // find what different element types we have
      boolean[] hasType = new boolean[AnsysType.numTypes()];
      ArrayList<LinkedList<FemElement3dBase>> elemsByType = null;
      int[] maxElemNums = null;
      
      if (myEBlockKey == EBlockKey.SOLID) {
         // we can print the elements as two heterogeneous volume and shell
         // block, and so we only need to determine which types are needed 
         for (FemElement3d e: fem.getElements ()) {
            hasType[getAnsysType(e).ordinal()] = true;
         }
         for (ShellElement3d e: fem.getShellElements ()) {
            hasType[getAnsysType(e).ordinal()] = true;
         }
      }
      else {
         // we need to use separate EBLOCKs for each element type, and so
         // need to group the elements by type
         elemsByType = new ArrayList<>();
         for (int k=0; k<AnsysType.numTypes(); k++) {
            elemsByType.add (k, new LinkedList<>());
         }
         maxElemNums = new int[AnsysType.numTypes()];
         for (FemElement3d e: fem.getElements ()) {
            int k = getAnsysType(e).ordinal();
            elemsByType.get(k).add (e);
            if (maxElemNums[k] < e.getNumber()) {
               maxElemNums[k] = e.getNumber();
            }
         }
         for (ShellElement3d e: fem.getShellElements ()) {
            int k = getAnsysType(e).ordinal();
            elemsByType.get(k).add (e);
            if (maxElemNums[k] < e.getNumber()) {
               maxElemNums[k] = e.getNumber();
            }
         }
         for (int k=0; k<AnsysType.numTypes(); k++) {
            hasType[k] = (elemsByType.get(k).size() > 0);
         }
      }

      if (myUseEtBlock) {
         int numTypes = 0;
         int maxType = 0;
         for (int k=0; k<hasType.length; k++) {
            if (hasType[k]) {
               numTypes++;
               maxType = AnsysType.values()[k].getTypeNumber();
            }
         }
         if (numTypes > 0) {
            pw.println ("ETBLOCK, "+numTypes+", "+maxType);
            pw.println ("(2i6,19a3)");
            NumberFormat ifmt = new NumberFormat ("%6d");
            for (int k=0; k<hasType.length; k++) {
               if (hasType[k]) {
                  AnsysType atype = AnsysType.values()[k];                  
                  pw.print (ifmt.format(atype.getTypeNumber()));
                  pw.print (ifmt.format(atype.getElemNumber()));
                  if (atype == AnsysType.SHELL181_MEMBRANE) {
                     pw.print ("  1");
                  }
                  pw.println ("");
               }
            }
            pw.println ("    -1");
         }
      }
      else {
         for (int k=0; k<hasType.length; k++) {
            if (hasType[k]) {
               AnsysType atype = AnsysType.values()[k];
               int typeNum = atype.getTypeNumber();
               pw.println ("ET, "+typeNum+","+atype.getElemNumber());
               if (atype == AnsysType.SHELL181_MEMBRANE) {
                  pw.println ("KEYOPT, "+typeNum+", 1, 1");
               }
            }
         }
      }

      // write out nodes

      // nodeNumInc is the number to add to node numbers to obtain the Ansys
      // node id.  If zero-based numbering is being used, numbers need to be
      // incremented by one because Ansys uses one-based numbering.
      int nodeNumInc = fem.getNodes().getOneBasedNumbering() ? 0 : 1;
      // find the maximum node number
      int maxNodeNum = fem.getNodes().getNumberLimit()-1 + nodeNumInc;

      pw.println ("NBLOCK,6,SOLID,  "+maxNodeNum+", "+fem.numNodes());
      pw.println ("(3i9,6e22.13e3)");
      NumberFormat ifmt = new NumberFormat ("%9d");
      NumberFormat ffmt = new NumberFormat ("%22.13e");
      ffmt.setNumExpDigits (3);
      StringBuilder sb = new StringBuilder();
      for (FemNode3d node : fem.getNodes()) {
         sb.setLength(0);
         sb.append (ifmt.format (node.getNumber()+nodeNumInc));
         sb.append ("        0        0");
         Point3d pos = node.getRestPosition();
         sb.append (ffmt.format (pos.x));
         sb.append (ffmt.format (pos.y));
         sb.append (ffmt.format (pos.z));
         pw.println (sb.toString());
      }
      pw.println ("N,R5.3,LOC,       -1,"); // XXX FINISH

      // figure out number increments and maximum numbers for both volumetric
      // and shell elements
      int numVolElems = fem.numElements();
      int elemNumInc = fem.getElements().getOneBasedNumbering() ? 0 : 1;
      int maxElemNum = fem.getElements().getNumberLimit()-1 + elemNumInc;

      int numShellElems = fem.numShellElements();
      int shellNumInc = fem.getShellElements().getOneBasedNumbering() ? 0 : 1;
      if (numVolElems > 0 && numShellElems > 0) {
         // if number ranges for volumetric and shell elements overlap,
         // increase shellNumInc so that they don't.
         int minShellNum = fem.getShellElements().getMinNumber()+shellNumInc;
         if (minShellNum <= maxElemNum) {
            shellNumInc += maxElemNum-minShellNum+1;
         }
      }
      int maxShellNum = fem.getShellElements().getNumberLimit()-1 + shellNumInc;

      String keyStr = myEBlockKey.toString();
      // write volumetric elements out as a single EBLOCK

      // elemNumInc and shellNumInc are the numbers to add to element node
      // numbers to obtain Gmsg tags for volumetric and shell elements
      int[] nodeIds = new int[20];
      ifmt = new NumberFormat ("%10i");
      if (myEBlockKey == EBlockKey.SOLID) {
         // write volumetric elements out as a single EBLOCK
         if (numVolElems > 0) {
            writeEBlockHeader (pw, maxElemNum, numVolElems);
            for (FemElement3d elem: fem.getElements ()) {
               AnsysType atype = getAnsysType(elem);
               buildElemDesc (sb, ifmt, atype, elem.getNumber()+elemNumInc);
               extractElemNodeIds (nodeIds, elem, atype, nodeNumInc);
               writeElemNodeIds (pw, sb, ifmt, nodeIds, atype);
            }
            pw.println ("        -1");
         }
         
         // write shell elements out as a single EBLOCK
         if (numShellElems > 0) {
            writeEBlockHeader (pw, maxShellNum, numShellElems);
            for (ShellElement3d elem: fem.getShellElements ()) {
               AnsysType atype = getAnsysType(elem);
               buildElemDesc (sb, ifmt, atype, elem.getNumber()+shellNumInc);
               extractElemNodeIds (nodeIds, elem, atype, nodeNumInc);
               writeElemNodeIds (pw, sb, ifmt, nodeIds, atype);
            }
            pw.println ("        -1");
         }
      }
      else {
         // write a separate EBLOCK for each type
         for (int k=0; k<hasType.length; k++) {
            if (hasType[k]) {
               AnsysType atype = AnsysType.values()[k];
               int numInc = atype.isVolumetric() ? elemNumInc : shellNumInc;
               pw.println ("TYPE,"+atype.getTypeNumber());
               writeEBlockHeader (
                  pw, maxElemNums[k]+numInc, elemsByType.get(k).size());
               for (FemElement3dBase elem : elemsByType.get(k)) {
                  buildElemDesc (sb, ifmt, atype, elem.getNumber()+numInc);
                  extractElemNodeIds (nodeIds, elem, atype, nodeNumInc);
                  writeElemNodeIds (pw, sb, ifmt, nodeIds, atype);
               }
               pw.println ("        -1");
            }
         }
      }
      
      pw.println ("/GO");
      pw.println ("FINISH");
      pw.close ();
   }

   void writeElemNodeIds (
      PrintWriter pw, StringBuilder sb, NumberFormat ifmt,
      int[] nodeIds, AnsysType atype) {
      
      int i = 0;
      int maxNodes = myEBlockKey.maxFirstLineNodes();
      while (i<Math.min(atype.numNodes(), maxNodes)) {
         sb.append (ifmt.format (nodeIds[i++]));
      }
      pw.println (sb.toString());
      if (atype.numNodes() > maxNodes) {
         sb.setLength(0);
         while (i<atype.numNodes()) {
            sb.append (ifmt.format (nodeIds[i++]));
         }
         pw.println (sb.toString());
      }
   }

   void writeEBlockHeader (PrintWriter pw, int maxElemNum, int numElems) {
      switch (myEBlockKey) {
         case SOLID: {
            pw.println ("EBLOCK,19,SOLID, "+maxElemNum+", "+numElems);
            pw.println ("(19i10)");
            break;
         }
         case COMPACT: {
            pw.println ("EBLOCK,10,COMPACT, "+maxElemNum+", "+numElems);
            pw.println ("(11i10)");
            break;
         }
         case BLANK: {
            pw.println ("EBLOCK,15,, "+maxElemNum+", "+numElems);
            pw.println ("(15i10)");
            break;
         }
      }
   }

   void buildElemDesc (
      StringBuilder sb, NumberFormat ifmt, AnsysType atype, int elemId) {
      sb.setLength(0);
      switch (myEBlockKey) {
         case SOLID: {
            sb.append ("         1");
            sb.append (ifmt.format (atype.getTypeNumber()));
            sb.append ("         1         1         0");
            sb.append ("         0         0         0");
            sb.append (ifmt.format (atype.numNodes()));
            sb.append ("         0");                 
            sb.append (ifmt.format (elemId));
            break;
         }
         case COMPACT: {
            sb.append (ifmt.format (elemId));
            break;
         }
         case BLANK: {
            sb.append (ifmt.format (elemId));            
            sb.append ("         1         1         1         0");
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Unknown EBLOCK key " + myEBlockKey);
         }
      }
   }
       
}
