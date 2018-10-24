/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.*;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;

/*
 * Lists the nodes associated with an element face, arranged in either
 * clockwise or counter-clockwise order. Two nodes sets are considered equal
 * if they contain the same nodes in either the same or reverse order. This
 * class is used in conjunction with a hash table to find adjacent faces
 * among different elements.
 */
public class FaceNodes3d {
   FemNode3d[] nodes;    // list of corner nodes
   FemNode3d[] allNodes; // list of all nodes, including those attached to edges
   FemElement3dBase myElement;
   int myFlags = 0;
   private static int HIDDEN = 0x01;
   private static int OVERLAPPING = 0x02;

   protected FaceNodes3d (FemElement3dBase elem, int n) {
      myElement = elem;
      nodes = new FemNode3d[n];
   }

   FaceNodes3d (
      FemElement3d elem, FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3) {
      myElement = elem;
      nodes = new FemNode3d[4];
      nodes[0] = n0;
      nodes[1] = n1;
      nodes[2] = n2;
      nodes[3] = n3;
   }

   public void setAllNodes (FemNode3d[] nodes) {
      allNodes = new FemNode3d[nodes.length];
      for (int i=0; i<nodes.length; i++) {
         allNodes[i] = nodes[i];
      }
   }      

   FemNode3d[] getNodes() {
      return nodes;
   }

   FemNode3d[] getAllNodes() {
      return allNodes;
   }

   public int numNodes() {
      return nodes.length;
   }

   public int numAllNodes() {
      return allNodes.length;
   }

   @Override
      public int hashCode() {
      int mod = (Integer.MAX_VALUE / nodes.length);
      int code = 0;
      for (int i = 0; i < nodes.length; i++) {
         code += nodes[i].hashCode() % mod;
      }
      return code;
   }

   @Override
      public boolean equals (Object obj) {
      if (!(obj instanceof FaceNodes3d)) {
         return false;
      }
      FaceNodes3d f = (FaceNodes3d)obj;

      if (nodes.length != f.nodes.length) {
         return false;
      }

      int idx0 = -1;
      int i;
      // find index of nodes[0] in f, if any
      for (i = 0; i < nodes.length; i++) {
         if (f.nodes[i] == nodes[0]) {
            idx0 = i;
            break;
         }
      }
      if (idx0 == -1) {
         return false;
      }
      // check both directions
      int k = idx0;
      for (i = 1; i < nodes.length; i++) {
         k = (k + 1) % nodes.length;
         if (nodes[i] != f.nodes[k]) {
            break;
         }
      }
      if (i == nodes.length) {
         return true;
      }
      k = idx0;
      for (i = 1; i < nodes.length; i++) {
         k = (k == 0 ? nodes.length - 1 : k - 1);
         if (nodes[i] != f.nodes[k]) {
            break;
         }
      }
      if (i == nodes.length) {
         return true;
      }
      return false;
   }

   public String toString() {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < nodes.length; i++) {
         builder.append (nodes[i].getNumber() + " ");
      }
      return builder.toString();
   }

   public FemElement3dBase getElement () {
      return myElement;
   }

   public boolean isHidden() {
      return (myFlags & HIDDEN) != 0;
   }

   public void setHidden (boolean hidden) {
      if (hidden) {
         myFlags |= HIDDEN;
      }
      else {
         myFlags &= ~HIDDEN;
      }
   }

   public boolean isOverlapping() {
      return (myFlags & OVERLAPPING) != 0;
   }

   public void setOverlapping (boolean hidden) {
      if (hidden) {
         myFlags |= OVERLAPPING;
      }
      else {
         myFlags &= ~OVERLAPPING;
      }
   }

   /** 
    * Returns true if all the nodes of this face are contained
    * within a given face.
    * 
    * @param f face to check for containment within
    * @return true if this face is contained within f
    */
   public boolean isContained (FaceNodes3d f) {
      if (f.allNodes.length < allNodes.length) {
         return false;
      }
      for (int i=0; i<allNodes.length; i++) {
         boolean found = false;
         for (int j=0; j<f.allNodes.length; j++) {
            if (allNodes[i] == f.allNodes[j]) {
               found = true;
               break;
            }
         }
         if (!found) {
            return false;
         }
      }
      return true;
   }

   public boolean containsNode (DynamicComponent c) {
      for (int i=0; i<nodes.length; i++) {
         if (nodes[i] == c) {
            return true;
         }
      }
      return false;
   }

   /** 
    * Queries if this face has a node from its own mesh attached to it.
    *
    * @return <code>true</code> if this face is attached to a node from its own
    * mesh
    */
   public boolean hasSelfAttachedNode() {
      // If this face is attached to, the relevant attachment will be
      // contained in the master attachment list of the first node.
      LinkedList<DynamicAttachment> masters = 
         nodes[0].getMasterAttachments();
      ModelComponent nodeList = nodes[0].getParent();
      if (masters == null) {
         return false;
      }
      for (DynamicAttachment a : masters) {
         if (a instanceof PointFem3dAttachment) {
            PointFem3dAttachment attach = (PointFem3dAttachment)a;
            if (attach.numMasters() == nodes.length &&
                attach.getSlave().getParent() == nodeList) {
               boolean attachmentIsThisFace = true;
               for (DynamicComponent c : a.getMasters()) {
                  if (!containsNode (c)) {
                     attachmentIsThisFace = false;
                     break;
                  }
               }
               if (attachmentIsThisFace) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   /** 
    * Queries if at least one of this face's nodes is itself attached to a face
    * belonging to the same mesh
    *
    * @return <code>true</code> if a face node is attached to another node on
    * the same mesh
    */
   public boolean isSelfAttachedToFace() {
      ModelComponent fem = null;
      if (nodes.length > 0) {
         fem = nodes[0].getGrandParent();
      }
      for (FemNode3d n : nodes) {
         PointFem3dAttachment a = FemModel3d.getFaceAttachment (n);
         if (a != null) {
            boolean attached = true;
            for (FemNode m : a.getNodes()) {
               if (m.getGrandParent() != fem) {
                  attached = false;
                  break;
               }
            }
            if (attached) {
               return true;
            }
         }
      }
      return false;
   }

//   protected FemNode3d[] createTriangle (
//      FemNode3d n0, FemNode3d n1, FemNode3d n2) {
//      
//      FemNode3d[] tri = new FemNode3d[3];
//      tri[0] = n0;
//      tri[1] = n1;
//      tri[2] = n2;
//      return tri;
//   }

   public FemNode3d[][] triangulate() {
      if (myElement != null) {
         return myElement.triangulateFace (this);
      }
      else {
         return null;
      }
   }

}
