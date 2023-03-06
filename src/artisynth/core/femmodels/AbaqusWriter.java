/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import maspack.matrix.Point3d;
import maspack.util.NumberFormat;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.femmodels.FemElement.ElementClass;

/**
 * Abaqus File reader, only supports the NODE and ELEMENT keywords
 * 
 * @author Antonio
 * 
 */
public class AbaqusWriter extends FemWriterBase {

   //XXX TODO: use number format
   
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
      S3(3, "S3"),
      S4(4, "S4"),
      CPS3(3, "CPS3"),
      CPS4(4, "CPS4"),
      UNKNOWN(0, "UNKNOWN");
      
      private int numNodes;
      private String strId;
      private Class<? extends FemElement3dBase> myClass;
      
      public int numNodes() {
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
   
   public static class ElemTypeList extends ArrayList<FemElement3dBase> {
      private static final long serialVersionUID = 9812341234L;
      ElemType myType = ElemType.UNKNOWN;
      public ElemTypeList(ElemType type) {
         myType = type;
      }
      
      public ElemType getType() {
         return myType;
      }
   }
   
   private final String COMMENT = "**";
   private final char KEYWORD = '*';
   
   private boolean cwHexWarningGiven;
   private boolean cwWedgeWarningGiven;
   private boolean cwTetWarningGiven; 
   private boolean nodeIdWarningGiven;
   private boolean mySuppressWarnings = false;

   public boolean getSuppressWarnings() {
      return mySuppressWarnings;
   }
   
   public void setSuppressWarnings(boolean enable) {
      mySuppressWarnings = enable;
   }
   
   /**
    * Writes a FemModel into an Abaqus data file.
    * 
    * @param fem
    * FEM model to be written
    * @param fileName
    * path name of the Abaqus node file
    * @throws IOException
    * if this is a problem writing the file
    */
   public static void write (FemModel3d fem, String fileName)
      throws IOException {
      write(fem, new File(fileName));
   }
   
   /**
    * Writes a FemModel into an Abaqus data file.
    * 
    * @param fem
    * FEM model to be written
    * @param file
    * the Abaqus node file
    * @throws IOException
    * if this is a problem writing the file
    */
   public static void write (FemModel3d fem, File file)
      throws IOException {

      AbaqusWriter writer = null;
      try {
         writer = new AbaqusWriter (file);
         writer.writeFem (fem);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (writer != null) {
            writer.close ();
         }
      }
   }

   /**
    * Nodes must be renumbered if any node has a number of 0.
    */
   private boolean nodesMustBeRenumbered (
      ComponentList<FemNode3d> nodes) {

      for (FemNode3d n : nodes) {
         if (n.getNumber() == 0) {
            return true;
         }
      }
      return false;
   }

   /**
    * Determine the maximum field width required to the numbers for a given
    * list of elements.
    */
   private <T extends ModelComponent> int computeFieldWidth (
      ComponentList<T> comps, int baseWidth, int numberBase) {
      
      int maxNum;
      if (numberBase > 0) {
         maxNum = comps.size() + numberBase;
      }
      else {
         maxNum = comps.getNumberLimit();
      }
      return Math.max (baseWidth, (int)Math.ceil (Math.log10 (maxNum)));
   }
   
   /**
    * Elements must be renumbered if (a) any element has a number of 0, or (b)
    * the same number is used by a solid and shell element.
    */
   private boolean elementsMustBeRenumbered (
      ComponentList<FemElement3d> solids, 
      ComponentList<ShellElement3d> shells) {

      boolean[] marked =
         new boolean[Math.max(solids.getNumberLimit(), shells.getNumberLimit())];
      int solidNumBase = -1;
      int shellNumBase = -1;
      for (FemElement3d e : solids) {
         int num = e.getNumber();
         if (num == 0) {
            return true;
         }
         marked[num] = true;
      }
      for (ShellElement3d e : shells) {
         int num = e.getNumber();
         if (num == 0) {
            return true;
         }
         if (marked[num]) {
            // overlap
            return true;
         }
      }
      return false;
   }
   
   private int[] findMaxMinNumber(
      Collection<? extends ModelComponentBase> list) {

      int[] out = new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE};
      
      for (ModelComponentBase cmp : list) {
         if (cmp.getNumber() < out[0]) {
            out[0] = cmp.getNumber();
         } else if (cmp.getNumber() > out[1]) {
            out[1] = cmp.getNumber();
         }
      }
      return out;
   }
   
   private void writeNodes (
      PrintWriter pw, ComponentList<FemNode3d> nodes, int numBase, int fieldWidth) {

      NumberFormat fmt = new NumberFormat ("%" + fieldWidth + "d");
      pw.println("*NODE");
      for (FemNode3d node : nodes) {
         int num;
         if (numBase > 0) {
            num = numBase + nodes.indexOf (node);
         }
         else {
            num = node.getNumber();
         }
         Point3d pos = node.getPosition();
         pw.println (
            fmt.format(num)+", "+
            myFmt.format(pos.x)+", "+
            myFmt.format(pos.y)+", "+
            myFmt.format(pos.z));
      }
      pw.println("**\n**");
      
   }
   
   private void writeInclude(
      PrintWriter pw, String inputFile) {
      pw.println("*INCLUDE, INPUT=" + inputFile);
   }
   
   private void writeElems (
      PrintWriter pw, ComponentList<? extends FemElement3dBase> elems,
      int numBase, ComponentList<FemNode3d> nodes, int nodeNumBase,
      int fieldWidth) {
      
      NumberFormat fmt = new NumberFormat ("%" + fieldWidth + "d");
      ElemTypeList elemLists[] = separateElements(elems);
      
      for (ElemTypeList list : elemLists) {
         if (list.size() > 0) {
            
            pw.println("*ELEMENT, TYPE=" + list.getType().getString());
            for (FemElement3dBase elem : list) {
               StringBuilder sb = new StringBuilder();
               int elemNum;
               if (numBase > 0) {
                  elemNum = numBase + elems.indexOf (elem);
               }
               else {
                  elemNum = elem.getNumber();
               }
               sb.append (fmt.format(elemNum));
               sb.append (", ");

               // get node numbers
               int[] nodeNums = new int[elem.numNodes()];
               int k = 0;
               for (FemNode3d node : elem.getNodes()) {
                  if (nodeNumBase > 0) {
                     nodeNums[k++] = nodeNumBase + nodes.indexOf(node);
                  }
                  else {
                     nodeNums[k++] = node.getNumber();
                  }
               }
               if (elem instanceof HexElement) {
                  reorderHexNodeNums (nodeNums);
               }
               else if (elem instanceof QuadhexElement) {
                  reorderQuadHexNodeNums (nodeNums);
               }
               int numsPerLine = 80/(fieldWidth+2);
               int num = 1;
               for (k=0; k<nodeNums.length; k++) {
                  sb.append (fmt.format(nodeNums[k]));
                  if (k<nodeNums.length-1) {
                     if (++num > numsPerLine) {
                        sb.append (",\n");
                        num = 0;
                     }
                     else {
                        sb.append (", ");
                     }
                  }
               }
               pw.println (sb.toString());
            }
            pw.println("**\n**");
         }
      }
   }

   private void swapNumbers (int[] nodeNums, int i, int j) {
      int tmp = nodeNums[i];
      nodeNums[i] = nodeNums[j];
      nodeNums[j] = tmp;
   }

   private void reorderHexNodeNums (int[] nodeNums) {
      swapNumbers (nodeNums, 0, 3);
      swapNumbers (nodeNums, 1, 2);
      swapNumbers (nodeNums, 4, 7);
      swapNumbers (nodeNums, 5, 6);
   }
   
   private void reorderQuadHexNodeNums (int[] nodeNums) {
      reorderHexNodeNums (nodeNums);
      swapNumbers (nodeNums, 8, 10);
      swapNumbers (nodeNums, 12, 14);
      swapNumbers (nodeNums, 16, 19);
      swapNumbers (nodeNums, 17, 18);
   }
   
   private ElemTypeList[] separateElements (
      Collection<? extends FemElement3dBase> elems) {
      ElemType[] types = ElemType.values();
      ElemTypeList[] elemLists = new ElemTypeList[types.length];
      HashSet<Class<? extends FemElement3dBase>> unknowns = new HashSet<>();
      
      for (int i=0; i<elemLists.length; i++) {
         elemLists[i] = new ElemTypeList(types[i]);
      }
      
      for (FemElement3dBase elem : elems) {
         int idx = getElemTypeIdx(elem);
         if (idx >= 0) {
            elemLists[idx].add(elem);
         } else {
            unknowns.add (elem.getClass());
         }
      }
      if (unknowns.size() > 0) {
         if (!mySuppressWarnings) {
            System.out.println (
               "WARNING: ignoring the following unsupported elements:");
         }
         for (Class<? extends FemElement3dBase> eclass : unknowns) {
            System.out.println (" " + eclass.getName());
         }
      }
      return elemLists;
   }
   
   private int getElemTypeIdx(FemElement3dBase elem) {
      Class<? extends FemElement3dBase> eclass = elem.getClass();
      ElemType type = null;
      switch (elem.getElementClass()) {
         case VOLUMETRIC: {
            if (eclass == TetElement.class) {
               type = ElemType.C3D4;
            }
            else if (eclass == WedgeElement.class) {
               type = ElemType.C3D6;
            }
            else if (eclass == HexElement.class) {
               type = ElemType.C3D8;
            }
            else if (eclass == QuadtetElement.class) {
               type = ElemType.C3D10;
            }
            else if (eclass == QuadhexElement.class) {
               type = ElemType.C3D20;
            }
            break;
         }
         case SHELL: {
            if (eclass == ShellTriElement.class) {
               type = ElemType.CPS3;
            }
            else if (eclass == ShellQuadElement.class) {
               type = ElemType.CPS4;
            }
            break;
         }
         case MEMBRANE: {
            if (eclass == ShellTriElement.class) {
               type = ElemType.S3;
            }
            else if (eclass == ShellQuadElement.class) {
               type = ElemType.S4;
            }
            break;
         }
      }
      return type != null ? type.ordinal() : -1;
   }
   
   private void writeHeader(PrintWriter pw, String name) {
      
      String header =  "**=========================================================================\n"
                     + "**     MODEL: " + name + "\n"
                     + "**\n"
                     + "**     CREATED by ArtiSynth AbaqusWriter ON:\n"
                     + "**        ==> " + new Date() + "\n"
                     + "** \n"
                     + "**     MODIFICATION HISTORY:\n"
                     + "**        ==> \n"
                     + "** \n"
                     + "**=========================================================================\n"
                     + "**                            MODEL DATA                                 **\n"
                     + "**=========================================================================\n"
                     + "**                                               Node / Element Definitions\n"
                     + "**\n";
      pw.print(header);
      
   }
   
   private void writeFooter(PrintWriter pw) {
      
      String footer =  "**\n" +
         "**\n" +
         "**=========================================================================\n" +
         "**                                                    Material  Definitions\n" +
         "**\n" +
         "**\n" +
         "**=========================================================================\n" +
         "**                          HISTORY DATA                                 **\n" +
         "**=========================================================================\n" +
         "**------------------------------------------------------------STEP 1\n" +
         "*STEP\n" +
         "**\n" +
         "*STATIC\n" +
         "**\n" +
         "**\n" +
         "**------- Output Requests  --->\n" +
         "**\n" +
         "**\n" +
         "**\n" +
         "**\n" +
         "*END STEP\n" +
         "**\n" +
         "**\n" +
         "**=========================================================================\n" +
         "**                         END OF ABAQUS INPUT DECK                      **\n" +
         "**=========================================================================\n";
      pw.print(footer);
   }

   /**
    * Writes a FEM model to an Abaqus file
    * 
    * @param fem
    * FEM model to be written
    * @throws IOException
    * if this is a problem writing the file
    */
   public void writeFem (FemModel3d fem) throws IOException {

      // boolean useOneBasedNum = (options & ONE_BASED_NUMBERING) != 0;
      PrintWriter pw = myPrintWriter;
      
      writeHeader (pw, fem.getName());

      ComponentList<FemNode3d> nodes = fem.getNodes();
      int nodeNumBase = -1;
      if (nodesMustBeRenumbered (nodes)) {
         nodeNumBase = 1;
      }
      int nodeFieldWidth = computeFieldWidth (nodes, 0, nodeNumBase);
      writeNodes(pw, nodes, nodeNumBase, nodeFieldWidth);

      // see if there is a number class between solid and shell elements
      ComponentList<FemElement3d> solids = fem.getElements();
      ComponentList<ShellElement3d> shells = fem.getShellElements();

      int solidNumBase = -1;
      int shellNumBase = -1;
      if (elementsMustBeRenumbered (solids, shells)) {
         solidNumBase = 1;
         shellNumBase = solids.size()+1;
         if (!mySuppressWarnings) {
            System.out.println (
               "WARNING: elements will be renumbered for Abaqus compatibility");
         }
      }
      int elemFieldWidth;
      elemFieldWidth = computeFieldWidth (solids, nodeFieldWidth, solidNumBase);
      elemFieldWidth = computeFieldWidth (shells, elemFieldWidth, shellNumBase);
      
      //int[] elemNumBounds = new int[] { Integer.MAX_VALUE, Integer.MIN_VALUE };
      writeElems (pw, solids, solidNumBase, nodes, nodeNumBase, elemFieldWidth);
      writeElems (pw, shells, shellNumBase, nodes, nodeNumBase, elemFieldWidth);
      
      // int[] nodeNumBounds = findMaxMinNumber(fem.getNodes());
      // writeNSet(pw, "nodes", nodeNumBounds);
      // writeElSet(pw, "elements", elemNumBounds);
      
      writeFooter(pw);
      pw.flush();
   }

   public AbaqusWriter (PrintWriter pw) throws IOException {
      super (pw);
   }
   
   public AbaqusWriter (File file) throws IOException {
      super (new PrintWriter (new BufferedWriter (new FileWriter (file))));
      myFile = file;
   }

   public AbaqusWriter (String fileName) throws IOException {
      this (new File(fileName));
   }
   
   // @Override
   // public void writeFem (FemModel3d fem) throws IOException {
   //    write(fem, new PrintWriter(myOstream));
      
   // }
   
}
