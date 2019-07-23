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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import maspack.matrix.Point3d;
import artisynth.core.modelbase.ModelComponentBase;

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
      C3D4(4, "C3D4", TetElement.class), 
      C3D6(6, "C3D6", WedgeElement.class),
      C3D8(8, "C3D8", HexElement.class),
      C3D10(10, "C3D10", QuadtetElement.class),
      C3D20(20, "C3D20", QuadhexElement.class),
      S3(3, "S3", ShellTriElement.class),
      S4(4, "S4", ShellQuadElement.class),
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
         myClass = FemElement3dBase.class;
      }

      private ElemType(
         int nNodes, String str, Class<? extends FemElement3dBase> cls) {
         numNodes = 4;
         strId = str;
         myClass = cls;
      }
      
      public Class<? extends FemElement3dBase> getElemClass() {
         return myClass; 
      }
      
   }
   
   public static class ElemTypeList extends ArrayList<FemElement3d> {
      private static final long serialVersionUID = 9812341234L;
      ElemType myType = ElemType.UNKNOWN;
      public ElemTypeList(ElemType type) {
         myType = type;
      }
      
      public ElemType getType() {
         return myType;
      }
      
      public boolean accepts(FemElement3d elem) {
         if (elem.getClass() == myType.getElemClass()) {
            return true;
         }
         return false;
      }
      
   }
   
   public static final String COMMENT = "**";
   public static final char KEYWORD = '*';
   
   static boolean cwHexWarningGiven;
   static boolean cwWedgeWarningGiven;
   static boolean cwTetWarningGiven; 
   static boolean nodeIdWarningGiven;
   
   /**
    * Writes a FemModel into an Abaqus data file.
    * 
    * @param model
    * FEM model to be written
    * @param fileName
    * path name of the Abaqus node file
    * @throws IOException
    * if this is a problem writing the file
    */
   public static void write (
      FemModel3d model, String fileName)
      throws IOException {
      write(model, new File(fileName));
   }
   
   /**
    * Writes a FemModel into an Abaqus data file.
    * 
    * @param model
    * FEM model to be written
    * @param file
    * the Abaqus node file
    * @throws IOException
    * if this is a problem writing the file
    */
   public static void write (
      FemModel3d model, File file)
      throws IOException {

      PrintWriter fileWriter = null;

      try {
         fileWriter = new PrintWriter(new FileWriter(file));
         write (model, fileWriter);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (fileWriter != null) {
            fileWriter.close ();
         }
      }
   }
   
   /**
    * Writes to an Abaqus file
    * 
    * @param model
    * FEM model to be written
    * @param fileWriter
    * writer to write out data
    * @throws IOException
    * if this is a problem writing the file
    */
   public static void write (FemModel3d model, PrintWriter fileWriter) throws IOException {

      // boolean useOneBasedNum = (options & ONE_BASED_NUMBERING) != 0;
      
      writeHeader (fileWriter, model.getName());
      
      writeNodes(fileWriter, model.getNodes());
      writeElements(fileWriter, model.getElements());
      
      int[] bounds = findMaxMinNumber(model.getNodes());
      writeNSet(fileWriter, "nodes", bounds[0], bounds[1]);
      bounds = findMaxMinNumber(model.getElements());
      writeElSet(fileWriter, "elements", bounds[0], bounds[1]);
      
      writeFooter(fileWriter);
      
   }
   
   public static int[] findMaxMinNumber(
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
   
   public static void writeNodes(
      PrintWriter writer, Collection<FemNode3d> nodes) {
      
      writer.println("*NODE");
      for (FemNode3d node : nodes) {
         Point3d pos = node.getPosition();
         writer.println(node.getNumber() + ", " + pos.x + ", " + pos.y + ", " + pos.z);
      }
      writer.println("**\n**");
      
   }
   
   public static void writeInclude(PrintWriter writer, String inputFile) {
      writer.println("*INCLUDE, INPUT=" + inputFile);
   }
   
   public static void writeNSet(PrintWriter writer, String name, int minIdx, int maxIdx) {
      writer.println("*NSET, NSET=" + name + " GENERATE");
      writer.println(minIdx + ", " + maxIdx);
   }
   
   public static void writeElSet(PrintWriter writer, String name, int minIdx, int maxIdx) {
      writer.println("*ELSET, ELSET=" + name + " GENERATE");
      writer.println(minIdx + ", " + maxIdx);
   }
   
   public static void writeElements(
      PrintWriter writer, Collection<FemElement3d> elems) {
      ElemTypeList elemLists[] = separateElements(elems);
      
      int minIdx = Integer.MAX_VALUE;
      int maxIdx = Integer.MIN_VALUE;
      
      for (ElemTypeList list : elemLists) {
         if (list.size() > 0) {
            
            writer.println("*ELEMENT, TYPE=" + list.getType().getString());
            for (FemElement3d elem : list) {
               int n = elem.getNumber();
               writer.print(n);
               
               if (n > maxIdx) {
                  maxIdx = n;
               } else if (n < minIdx) {
                  minIdx = n;
               }
               
               for (FemNode3d node : elem.getNodes()) {
                  writer.print(", " + node.getNumber());
               }
               writer.println();
            }
            writer.println("**\n**");
         }
      }
      
   }
   
   private static ElemTypeList[] separateElements (
      Collection<FemElement3d> elems) {
      ElemType[] types = ElemType.values();
      ElemTypeList[] elemLists = new ElemTypeList[types.length];
      ElemTypeList unknown = null;
      
      for (int i=0; i<elemLists.length; i++) {
         elemLists[i] = new ElemTypeList(types[i]);
         if (types[i] == ElemType.UNKNOWN) {
            unknown = elemLists[i];
         }
      }
      
      for (FemElement3d elem : elems) {
         int idx = getElemTypeIdx(elem, types);
         if (idx >= 0) {
            elemLists[idx].add(elem);
         } else {
            unknown.add(elem);
         }
      }
      
      return elemLists;
   }
   
   private static int getElemTypeIdx(FemElement3d elem, ElemType[] types) {
      
      for (int i=0; i<types.length; i++) {
         if (elem.getClass() == types[i].getElemClass()) {
            return i;
         }
      } 
      return -1;
      
   }
   
   
   private static void writeHeader(PrintWriter writer, String name) {
      
      String header =  "**=========================================================================\n"
                     + "**     MODEL: " + name + "\n"
                     + "**\n"
                     + "**     CREATED ON:\n"
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
      writer.print(header);
      
   }
   
   private static void writeFooter(PrintWriter writer) {
      
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
      writer.print(footer);
   }

   protected AbaqusWriter (OutputStream os) {
      myOstream = os;
   }

   protected AbaqusWriter (File file) throws IOException {
      this (new FileOutputStream (file));
      myFile = file;
   }

   protected AbaqusWriter (String fileName) throws IOException {
      this (new File(fileName));
   }
   
   @Override
   public void writeFem(FemModel3d fem) throws IOException {
      write(fem, new PrintWriter(myOstream));
      
   }
   
}
