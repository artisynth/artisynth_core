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
import maspack.matrix.Point3d;

/**
 * Writes FEM mesh geometry to a Gmsh {@code .msh} file.
 */
public class GmshWriter extends FemWriterBase {

   static private double INF = Double.POSITIVE_INFINITY;
   
   public static String version = "4.1";

   static int[] myGmshElemCodes = new int[] {
      4,  // tet
      5,  // hex
      7,  // pyramid
      6,  // wedge
      11, // quadtet
      17, // quadhex
      19, // quadpyramid
      18, // quadwedge
      2,  // shell tri
      3,  // shell quad
   };

   /**
    * Creates a GmshWriter to write to a specified PrintWriter. The actual
    * writing of the FEM geometry is done by {@link #writeFem}.
    * 
    * @param pw writer to write to
    * @throws IOException if an I/O error occurred
    */
   public GmshWriter (PrintWriter pw) throws IOException {
      super (pw);
   }

   /**
    * Creates a GmshWriter to write to a specified file. The actual
    * writing of the FEM geometry is done by {@link #writeFem}.
    * 
    * @param file file to write to
    * @throws IOException if an I/O error occurred
    */
   public GmshWriter (File file) throws IOException {
      super (file);
   }

   /**
    * Creates a GmshWriter to write to a specified file. The actual
    * writing of the FEM geometry is done by {@link #writeFem}.
    * 
    * @param filePath path name of the file to write to
    * @throws IOException if an I/O error occurred
    */
   public GmshWriter (String filePath) throws IOException {
      super (new File(filePath));
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
      GmshWriter writer = new GmshWriter (file);
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
   
   void writeElems (
      PrintWriter pw, ElemType type,
      ArrayList<LinkedList<FemElement3dBase>> elemLists,
      int elemNumInc, int nodeNumInc) {
      
      int typeCode = myGmshElemCodes[type.ordinal()];
      LinkedList<FemElement3dBase> elems = elemLists.get(type.ordinal());
      if (elems.size() > 0) {
         ElementNodeNumbering nodeNumbering = GmshReader.myNodeNumbering;
         int entityTag = type.isShell() ? 2 : 1; // 2 for surface, 1 for volume
         int dimen = type.isShell() ? 2 : 3; // 2 for surface, 3 for volume
         pw.println (dimen+" "+entityTag+" "+typeCode+" "+elems.size());
         for (FemElement3dBase e : elems) {
            int elemTag = e.getNumber() + elemNumInc;
            pw.print (elemTag);
            pw.println (
               nodeNumbering.getNodeIdString (type, e.getNodes(), nodeNumInc));
         }
      }
   }

   /**
    * Writes FEM model geometry to the writer or file associates with this
    * GmshWriter.
    *
    * @param fem FEM model to be written
    * @throws IOException if an I/O error occurred
    */
   public void writeFem (FemModel3d fem) throws IOException {
      
      PrintWriter pw = myPrintWriter;
      int floatSize = 8;

      int numVolElems = fem.numElements();
      int numShellElems = fem.numShellElements();
      // If the model contains both shell and volumetric elements, find which
      // nodes belong to which. We need to to this because Gmsh segregate nodes
      // which belong *only* to shell elements, associating them with a
      // separate surface entity.
      boolean[] isShellNode = null;
      boolean[] isVolumeNode = null;
      if (numShellElems > 0 && numVolElems > 0) {
         isShellNode = new boolean[fem.numNodes()];
         isVolumeNode = new boolean[fem.numNodes()];
         ComponentList<FemNode3d> nodes = fem.getNodes();
         for (ShellElement3d elem : fem.getShellElements()) {
            for (FemNode3d n : elem.getNodes()) {
               isShellNode[nodes.indexOf(n)] = true;
            }
         }
         for (FemElement3d elem : fem.getElements()) {
            for (FemNode3d n : elem.getNodes()) {
               isVolumeNode[nodes.indexOf(n)] = true;
            }
         }
      }
      
      pw.println ("$MeshFormat");
      pw.println (version + " 0 " + floatSize);
      pw.println ("$EndMeshFormat");

      // Found the x,y,z bounds for the volume entity, as well as the surface
      // entity if it exists.
      Point3d minShell = null;
      Point3d maxShell = null;
      Point3d minVol = null;
      Point3d maxVol = null;
      int numShellOnlyNodes = 0;
      for (int i=0; i<fem.numNodes(); i++) {
         FemNode3d n = fem.getNode(i);
         if (numVolElems > 0) {
            // check bounds for volume nodes
            if (isVolumeNode == null || isVolumeNode[i]) {
               if (minVol == null) {
                  minVol = new Point3d(n.getPosition());
                  maxVol = new Point3d(n.getPosition());
               }
               else {
                  n.getPosition().updateBounds (minVol, maxVol);
               }
            }
         }
         if (numShellElems > 0) {
            // check bounds for shell nodes
            if (isShellNode == null || isShellNode[i]) {
               if (minShell == null) {
                  minShell = new Point3d(n.getPosition());
                  maxShell = new Point3d(n.getPosition());
               }
               else {
                  n.getPosition().updateBounds (minShell, maxShell);
               }
            }
            if (numVolElems == 0 || !isVolumeNode[i]) {
               numShellOnlyNodes++;
            }
         }
      }

      // Write out the entities. A volume entity always exists, even if there
      // are no volumetic elements. A surface entity exists if there are shell
      // elements, even if it contains no shell-only nodes (and hence has zero
      // bounds).
      int numSurfaceEntities = fem.numShellElements() > 0 ? 1 : 0;
      int numVolumeEntities = 1; // Gmsh seems to always assume a volume entity
      pw.println ("$Entities");
      pw.println ("0 0 "+numSurfaceEntities+" "+numVolumeEntities);
      if (numSurfaceEntities == 1) {
         pw.println ("2 "+minShell+" "+maxShell+" 0 0");
      }
      if (numVolumeEntities == 1) {
         if (numVolElems > 0) {
            pw.println ("1 "+minVol+" "+maxVol+" 0 0");
         }
         else {
            pw.println ("1 0 0 0 0 0 0 0 0");
         }
      }
      pw.println ("$EndEntities");
      
      pw.println("$Nodes");
      int numNodes = fem.getNodes ().size ();

      // nodeNumInc is the number to add to node numbers to obtain the Gmsh
      // node tag.  If zero-based numbering is being used, numbers need to be
      // incremented by one because Gmsh uses one-based numbering.
      int nodeNumInc = fem.getNodes().getOneBasedNumbering() ? 0 : 1;

      // find the min and max node tags.
      int minNodeTag = fem.getNodes().getMinNumber() + nodeNumInc;
      int maxNodeTag = fem.getNodes().getNumberLimit() - 1 + nodeNumInc;

      // if there is a surface entity, nodes will be written in two blocks -
      // one for the the surface-only nodes and one for the volume nodes.
      int numNodeEntities = 1;
      if (numSurfaceEntities == 1) {
         numNodeEntities++;
      }
      pw.println(numNodeEntities+" "+numNodes+" "+minNodeTag+" "+maxNodeTag);
      if (numSurfaceEntities == 1) {
         // write node block for the surface entity
         pw.println("2 2 0 " + numShellOnlyNodes);         
         if (numShellOnlyNodes > 0) {
            for (int i = 0; i < numNodes; i++) {
               if (numVolElems == 0 || !isVolumeNode[i]) {
                  FemNode3d node = fem.getNode(i);
                  int num = node.getNumber() + nodeNumInc;
                  pw.println (num);
               }
            }
            for (int i = 0; i < numNodes; i++) {
               if (numVolElems == 0 || !isVolumeNode[i]) {
                  Point3d pos = fem.getNode(i).getPosition ();
                  pw.println (pos.x + " " + pos.y + " " + pos.z);
               }
            }
         }
      }
      // write node block for the volume entity
      pw.println("3 1 0 " + (numNodes-numShellOnlyNodes));
      if (numNodes - numShellOnlyNodes > 0) {
         for (int i = 0; i < numNodes; i++) {
            if (numShellElems == 0 || isVolumeNode[i]) {
               int num = fem.getNode(i).getNumber() + nodeNumInc;
               pw.println (num);
            }
         }
         for (int i = 0; i < numNodes; i++) {
            if (numShellElems == 0 || isVolumeNode[i]) {
               Point3d pos = fem.getNode(i).getPosition ();
               pw.println (pos.x + " " + pos.y + " " + pos.z);
            }
         }
      }
      pw.println ("$EndNodes");

      ArrayList<LinkedList<FemElement3dBase>> elemLists =
         new ArrayList<>(ElemType.values().length);
      for (int i=0; i<ElemType.values().length; i++) {
         elemLists.add (new LinkedList<>());
      }
      
      // elemNumInc and shellNumInc are the numbers to add to element node
      // numbers to obtain Gmsg tags for volumetric and shell elements
      int elemNumInc = fem.getElements().getOneBasedNumbering() ? 0 : 1;
      int shellNumInc = fem.getShellElements().getOneBasedNumbering() ? 0 : 1;

      if (numVolElems > 0 && numShellElems > 0) {
         // if number ranges for volumetric and shell elements overlap,
         // increase shellNumInc so that they don't.
         int maxElemTag = fem.getElements().getNumberLimit()-1+elemNumInc;
         int minShellTag = fem.getShellElements().getMinNumber()+shellNumInc;
         if (minShellTag <= maxElemTag) {
            shellNumInc += maxElemTag-minShellTag+1;
         }
      }

      // search through all the elements are collect them by element type in
      // elemLists.
      for (FemElement3d e: fem.getElements ()) {
         ElemType type = ElemType.getType(e);
         elemLists.get(type.ordinal()).add (e);
      }
      
      for (ShellElement3d e: fem.getShellElements ()) {
         ElemType type = ElemType.getType(e);
         elemLists.get(type.ordinal()).add (e);
      }

      // elements will be written in sections, which each section corresponding
      // to a specific element type
      int numElemSections = 0;
      for (int i=0; i<elemLists.size(); i++) {
         if (elemLists.get(i).size() > 0) {
            numElemSections++;
         }
      }
      
      int numElems = numVolElems + numShellElems;
      // find the min and max element tags
      int minElemTag;
      int maxElemTag;
      if (numVolElems > 0) {
         minElemTag = fem.getElements().getMinNumber() + elemNumInc;
         if (numShellElems > 0) {
            maxElemTag = fem.getShellElements().getNumberLimit()-1 + shellNumInc;
         }
         else {
            maxElemTag = fem.getElements().getNumberLimit()-1 + elemNumInc;
         }
      }
      else {
         minElemTag = fem.getShellElements().getMinNumber() + shellNumInc;
         maxElemTag = fem.getShellElements().getNumberLimit() -1 + shellNumInc;
      }

      pw.println ("$Elements");      
      pw.println (numElemSections+" "+numElems+" "+minElemTag+" "+maxElemTag);

      // write shell elements first, since that is what Gmsh does
      writeElems (pw, ElemType.SHELL_TRI, elemLists, shellNumInc, nodeNumInc);
      writeElems (pw, ElemType.SHELL_QUAD, elemLists, shellNumInc, nodeNumInc);

      // write out volumetric elements
      writeElems (pw, ElemType.TET, elemLists, elemNumInc, nodeNumInc);
      writeElems (pw, ElemType.HEX, elemLists, elemNumInc, nodeNumInc);
      writeElems (pw, ElemType.WEDGE, elemLists, elemNumInc, nodeNumInc);
      writeElems (pw, ElemType.PYRAMID, elemLists, elemNumInc, nodeNumInc);
      writeElems (pw, ElemType.QUADTET, elemLists, elemNumInc, nodeNumInc);
      writeElems (pw, ElemType.QUADHEX, elemLists, elemNumInc, nodeNumInc);
      writeElems (pw, ElemType.QUADWEDGE, elemLists, elemNumInc, nodeNumInc);
      writeElems (pw, ElemType.QUADPYRAMID, elemLists, elemNumInc, nodeNumInc);

      pw.println ("$EndElements");
      pw.close ();
   }
       
}
