/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.Vector3d;

public class AABBTree extends BVTree {

   private static final double INF = Double.POSITIVE_INFINITY;
   
   protected AABB myRoot;

   private class ElemDesc {
      Vector3d myCentroid;
      Boundable myElem;

      ElemDesc (Boundable elem) {
         myElem = elem;
         myCentroid = new Vector3d();
         elem.computeCentroid (myCentroid);
      }
   }

   public AABB getRoot() {
      return myRoot;
   }

   public AABBTree () {
   }

   public AABBTree (MeshBase mesh, int maxLeafElems, double margin) {
      setMaxLeafElementsForMesh (mesh, maxLeafElems);
      setMarginForMesh (mesh, margin);
      build (mesh);
      numberNodes (myRoot, 0);
   }
   
   public AABBTree (MeshBase mesh, int maxLeafElems) {
      this (mesh, maxLeafElems, -1);
   }
   
   public AABBTree (MeshBase mesh) {
      this (mesh, 2, -1);
   }
  
   private boolean splitNode (
      AABB node, ElemDesc[] edescs,
      int nelems, int depth) {

      Vector3d minc = new Vector3d ( INF,  INF,  INF);
      Vector3d maxc = new Vector3d (-INF, -INF, -INF);
      Vector3d avgc = new Vector3d ();
      Vector3d diff = new Vector3d ();

      ElemDesc[] leftElems = new ElemDesc[nelems];
      ElemDesc[] rightElems = new ElemDesc[nelems];

      int numLeft = 0;
      int numRight = 0;
      double max = Double.NEGATIVE_INFINITY;
      double min = Double.POSITIVE_INFINITY;
      for (int i = 0; i < nelems; i++) {
         Vector3d centroid = edescs[i].myCentroid;
         centroid.updateBounds (minc, maxc);
         avgc.add (centroid);
      }
      avgc.scale (1/(double)nelems);
      diff.sub (maxc, minc);
      int longAxis = diff.maxAbsIndex();
      double center = avgc.get (longAxis);

      for (int i = 0; i < nelems; i++) {
         Vector3d centroid = edescs[i].myCentroid;
         if (centroid.get(longAxis) <= center) {
            leftElems[numLeft++] = edescs[i];
         }
         else {
            rightElems[numRight++] = edescs[i];
         }
      }
      // just in case: only can happen if all midpoints equal the center
      if (numRight == 0) {
         rightElems[numRight++] = leftElems[--numLeft];
      }

      if (numLeft < nelems && numRight < nelems) {
         node.addChild (buildNodesRecursively (leftElems, numLeft, depth + 1));
         node.addChild (buildNodesRecursively (rightElems, numRight, depth + 1));
         return true;
      }
      else {
         return false;
      }
   }

   private void setNodeBounds (AABB node, ElemDesc[] edescs, int nelems) {
      node.myMax.set (-INF, -INF, -INF);
      node.myMin.set ( INF,  INF,  INF);
      for (int i=0; i<nelems; i++) {
         edescs[i].myElem.updateBounds (node.myMin, node.myMax);
      }
      node.myMax.add ( myMargin,  myMargin,  myMargin);
      node.myMin.add (-myMargin, -myMargin, -myMargin);
   }

   private void setLeafElements (AABB node, ElemDesc[] edescs, int nelems) {
      Boundable[] elems = new Boundable[nelems];
      for (int i=0; i<nelems; i++) {
         elems[i] = edescs[i].myElem;
      }
      node.setElements (elems);
   }

   AABB buildNodesRecursively (
      ElemDesc[] edescs, int nelems, int depth) {
      AABB node = new AABB();

      setNodeBounds (node, edescs, nelems);

      // removed maxDepth test for now ...
      // if ((myMaxDepth == -1 || depth < myMaxDepth) &&
      if (nelems > myMaxLeafElements) {
         if (!splitNode (node, edescs, nelems, depth)) {
            setLeafElements (node, edescs, nelems);
         }
      }
      else {
         setLeafElements (node, edescs, nelems);
      }
      return node;
   }

   public void build (
      Boundable[] elements, int nelems) {
      //myPoints = new Point3d[maxPoints];
      ElemDesc[] edescs = new ElemDesc[nelems];
      for (int i=0; i<nelems; i++) {
         edescs[i] = new ElemDesc (elements[i]);
      }
      myRoot = buildNodesRecursively (edescs, nelems, 0);
   }

   protected void updateRecursively (AABB node) {
      double margin = myMargin;
      if (node.isLeaf()) {
         node.update(margin);
      }
      else {
         AABB child = (AABB)node.myFirstChild;
         if (child != null) {
            updateRecursively (child);
            //node.updateForAABB (child, margin);
            node.set (child, margin);
            child = (AABB)child.getNext();
            while (child != null) {
               updateRecursively (child);
               node.updateForAABB (child, margin);
               child = (AABB)child.getNext();
            }
         }
      }
   }

   public void update() {
      updateRecursively (myRoot);
   }

}
