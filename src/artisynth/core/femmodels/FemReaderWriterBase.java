package artisynth.core.femmodels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.*;
import java.util.*;

import artisynth.core.femmodels.FemElement.ElementClass;
import maspack.geometry.MeshBase;
import maspack.util.NumberFormat;

/**
 * Base class containing functionality that can be used to implement both FEM
 * geometry readers and writers.
 * 
 * @author John E Lloyd
 */
public abstract class FemReaderWriterBase {
   /**
    * Internal class used for describing ArtiSynth element types and some of
    * their attributes.
    */
   protected enum ElemType {
      TET(4,false),
      HEX(8,false),
      PYRAMID(5,false),
      WEDGE(6,false),
      QUADTET(10,false),
      QUADHEX(20,false),
      QUADPYRAMID(13,false),
      QUADWEDGE(15,false),                  
      SHELL_TRI(3,true),
      SHELL_QUAD(4,true);

      int myNumNodes;
      boolean myShellP;

      ElemType (int numNodes, boolean isShell) {
         myNumNodes = numNodes;
         myShellP = isShell;
      }

      int numNodes() {
         return myNumNodes;
      }

      boolean isShell() {
         return myShellP;
      }

      static ElemType getType (FemElement3dBase elem) {
         if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
            switch(elem.getNodes ().length) {
               case 4: return ElemType.TET;
               case 5: return ElemType.PYRAMID;
               case 6: return ElemType.WEDGE;
               case 8: return ElemType.HEX;
               case 10: return ElemType.QUADTET;
               case 20: return ElemType.QUADHEX;
               case 15: return ElemType.QUADWEDGE;
               case 13: return ElemType.QUADPYRAMID;
               default:
                  throw new UnsupportedOperationException (
                     "Unknown element type: " + elem);
            }
         }
         else {
            switch(elem.getNodes ().length) {
               case 3: return ElemType.SHELL_TRI;
               case 4: return ElemType.SHELL_QUAD;
               default:
                  throw new UnsupportedOperationException (
                     "Unknown element type: " + elem);
            }
         }
      }
   };

   /**
    * Internal class used for describing ArtiSynth element types and some of
    * their attributes.
    */
   static class ElementNodeNumbering {
      int[][] myNumberings = new int[ElemType.values().length][];

      public void set (ElemType type, int[] numbering) {
         if (numbering != null && type.numNodes() != numbering.length) {
            throw new IllegalArgumentException (
               "Numbering for type "+type+" must have "+type.numNodes()+" nodes");
         }
         myNumberings[type.ordinal()] = numbering;
      }

      public int[] get (ElemType type) {
         return myNumberings[type.ordinal()];
      }

      /**
       * Find the element FEM nodes corresponding to a set of node numbers as
       * described in the input. Nodes are obtained from their input ids using
       * a 'nodeMap' implemented as a HashMap.
       */
      public void findNodes (
         FemNode3d[] nodes, ElemType type, int[] nodeIds,
         HashMap<Integer,FemNode3d> nodeMap) {

         int[] numbering = get(type);
         if (numbering != null) {
            for (int i=0; i<type.numNodes(); i++) {
               nodes[numbering[i]] = nodeMap.get(nodeIds[i]);
            }
         }
         else {
            for (int i=0; i<type.numNodes(); i++) {
               nodes[i] = nodeMap.get(nodeIds[i]);
            }
         }
      }

      /**
       * Find the element FEM nodes corresponding to a set of node numbers as
       * described in the input. Nodes are obtained from their input ids using
       * a 'nodeMap' implemented as an array.
       */
      public void findNodes (
         FemNode3d[] nodes, ElemType type, int[] nodeIds,
         FemNode3d[] nodeMap) {

         int[] numbering = get(type);
         if (numbering != null) {
            for (int i=0; i<type.numNodes(); i++) {
               nodes[numbering[i]] = nodeMap[nodeIds[i]];
            }
         }
         else {
            for (int i=0; i<type.numNodes(); i++) {
               nodes[i] = nodeMap[nodeIds[i]];
            }
         }
      }

      /**
       * Return a string listing the (integer) node ids for a given element.
       * Node ids are determined by adding numInc to the node number.
       */
      public String getNodeIdString (
         ElemType type, FemNode3d[] nodes, int numInc) {

         StringBuilder sb = new StringBuilder();
         int[] numbering = get(type);
         if (numbering != null) {
            for (int i=0; i<type.numNodes(); i++) {
               sb.append (" ");
               sb.append (nodes[numbering[i]].getNumber()+numInc);
            }
         }
         else {
            for (int i=0; i<type.numNodes(); i++) {
               sb.append (" ");
               sb.append (nodes[i].getNumber()+numInc);
            }
         }
         return sb.toString();
      }
   }

   /**
    * Can be used to describe a node and its identification number.
    */
   protected class NodeDesc {
      int myTag;
      FemNode3d myNode;

      NodeDesc (FemNode3d node, int tag) {
         myNode = node;
         myTag = tag;
      }
   }

   /**
    * Enumerated type specifying the preferred type of a shell element.
    */
   public enum ShellType {
      /**
       * Membrane shell element (no bending stiffness)
       */
      MEMBRANE,

      /**
       * Full shell element (has bending stiffness)
       */
      SHELL;
   };

   /**
    * Can be used to describe an element by its type, nodes,
    * and identification number.
    */
   protected class ElemDesc {
      int myTag;
      ElemType myType;
      ShellType myShellType;
      FemNode3d[] myNodes;

      ElemDesc (ElemType type, FemNode3d[] nodes, int tag) {
         myType = type;
         myNodes = nodes;
         myTag = tag;
      }

      ElemDesc (ElemType type, FemNode3d[] nodes, ShellType shellType, int tag) {
         myType = type;
         myNodes = nodes;
         myTag = tag;
         myShellType = shellType;
      }
   }   

   /**
    * Create the element FEM nodes corresponding to a set of node numbers as
    * described in the input. Nodes are obtained from their input ids using a
    * 'nodeMap' implemented as an array.
    *
    * @param type element type
    * @param nodeIds node numbers as prescribed by the input
    * @param ordering if non-null, defines the mapping from the
    * input nodes to ArtiSynth nodes
    * nunbers in nodeIds that should be used to define the nodes
    * @param nodeMap maps from input node ids to nodes
    * @return created element nodes
    */
   FemNode3d[] createNodes (
      ElemType type, int[] nodeIds, int[] ordering, 
      HashMap<Integer,FemNode3d> nodeMap) {

      FemNode3d[] nodes = new FemNode3d[type.numNodes()];
      if (ordering != null) {
         for (int i=0; i<nodes.length; i++) {
            nodes[ordering[i]] = nodeMap.get(nodeIds[i]);
         }
      }
      else {
         for (int i=0; i<nodes.length; i++) {
            nodes[i] = nodeMap.get(nodeIds[i]);
         }
      }
      return nodes;
   }

}
