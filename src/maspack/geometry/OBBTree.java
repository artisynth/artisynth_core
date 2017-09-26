/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Collection;

import maspack.geometry.OBB.Method;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.render.Renderer;
import maspack.render.RenderableUtils;
import maspack.render.GL.GLSupport;
import maspack.util.InternalErrorException;
//import maspack.util.ScanSupport;
import maspack.util.RandomGenerator;

public class OBBTree extends BVTree {
   private OBB root;

   private ElemData[] elemData;

   private Point3d meanCentroid = new Point3d();
   private Point3d tmpPnt = new Point3d();
   private Vector3d[] obbAxes = new Vector3d[3];

   public static int myDefaultMaxLeafElems = 2;
   public static Method myDefaultMethod = Method.Covariance;
   //public static Method myDefaultMethod = Method.ConvexHull;
   private Method myMethod = myDefaultMethod;

   /**
    * Returns the method used generate the OBBs within the tree
    */
   Method getMethod() {
      return myMethod;
   }

   /**
    * Sets the method used to generate the OBBs within the tree.
    * This setting will take effect upon to next call to build.
    *
    * @param method new method to be used to generate the OBBs
    */
   public void setMethod (Method method) {
      myMethod = method;
   }

   public OBB getRoot() {
      return root;
   }

   // DBG
   public boolean debug = false;

   public OBBTree() {
      for (int i = 0; i < 3; i++) {
         obbAxes[i] = new Vector3d();
      }
      setMaxLeafElements(myDefaultMaxLeafElems);
      setMethod(myDefaultMethod);
   }

   public OBBTree (
      MeshBase mesh, Method method, int maxLeafElems, double margin) {
      this();
      setMaxLeafElementsForMesh (mesh, maxLeafElems);
      setMarginForMesh (mesh, margin);
      setMethod (method);
      build (mesh);
      numberNodes (getRoot(), 0);
   }

   public OBBTree (
      MeshBase mesh, int maxLeafElems, double margin) {
      this (mesh, myDefaultMethod, maxLeafElems, margin);
   }
   public OBBTree (MeshBase mesh, Method method) {
      this(mesh, method, myDefaultMaxLeafElems, -1);
   }

   public OBBTree (MeshBase mesh, int maxLeafElems) {
      this(mesh, myDefaultMethod, maxLeafElems, -1);
   }

   public OBBTree (MeshBase mesh) {
      this(mesh, myDefaultMethod, myDefaultMaxLeafElems, -1);
   }

   static class ElemData {
      Point3d myCentroid;
      Boundable myElem;
      int myIdx;

      ElemData (Boundable e, int idx) {
         myCentroid = new Point3d();
         myElem = e;
         myIdx = idx;
      }         
   }

   private Boundable[] createElementArray (ElemData[] edata, int num) {
      Boundable[] elems = new Boundable[num];
      for (int i = 0; i < num; i++) {
         elems[i] = edata[i].myElem;
      }
      return elems;
   }
   
   public void build (Boundable[] elems, int num) {
      // XXX this is a hack to handle situations (like boxes) where
      // there is a small number of large faces, and so an OBB that
      // covers even two elements can be quite large. There should
      // be a better way to determine this.
      if (elems.length < 50) {
         setMaxLeafElements (1);
      }
      elemData = new ElemData[num];
      for (int i = 0; i < num; i++) {
         ElemData edata = new ElemData (elems[i], i);
         elems[i].computeCentroid (edata.myCentroid);
         elemData[i] = edata;
      }
      root = computeOBBNode (elemData, num, null);
   }

   private OBB computeOBBNode (
      ElemData[] edata, int num, OBB parent) {

      meanCentroid.setZero();

      for (int i = 0; i < num; i++) {
         meanCentroid.add (edata[i].myCentroid);
      }
      meanCentroid.scale (1 / (double)num);

      //OBB obb = new OBB (edata, num, myMargin);
      Boundable[] elems = createElementArray (edata, num);
      OBB node = new OBB ();
      node.set (elems, num, myMargin, myMethod);
      node.setParent (parent);
      node.getSortedAxes (obbAxes);

      if (num > myMaxLeafElements) { // subdivide the box
         
         ElemData[] elemsU = new ElemData[num];
         ElemData[] elemsL = new ElemData[num];
         int numU = 0;
         int numL = 0;

         // start with long axis
         int axisIdx = 0;
         do {
            
            numU = 0;
            numL = 0;
            Vector3d axis = obbAxes[axisIdx];
            for (int i = 0; i < num; i++) {
               //face.computeCentroid (elemCentroid);
               tmpPnt.sub (edata[i].myCentroid, meanCentroid);
               //tmpPnt.sub (elemCentroid, meanCentroid);
               if (axis.dot (tmpPnt) >= 0) {
                  elemsU[numU++] = edata[i];
               }
               else {
                  elemsL[numL++] = edata[i];
               }
            }
            axisIdx++;  // potentially move to next axis
         } while (axisIdx < 3 && (numL == 0 || numU == 0) );
            
         if (numU < num && numL < num) {
            // System.out.println ("numU=" + numU);
            OBB nodeU = computeOBBNode (elemsU, numU, node);
            // System.out.println ("numL=" + numL);
            OBB nodeL = computeOBBNode (elemsL, numL, node);

            node.addChild (nodeL);
            node.addChild (nodeU);
         }
         else {
            node.setElements (elems); // createElementArray (edata, num));
         }
      }
      else {
         node.setElements (elems); // createElementArray (edata, num));
      }
      return node;
   }

   public void recursiveRender (
      Renderer renderer, int flags, BVNode node, int level) {
      if (node.isLeaf()) {
         ((OBB)node).render (renderer, flags);
      }
      BVNode child;
      for (child = node.myFirstChild; child != null; child = child.myNext) {
         recursiveRender (renderer, flags, child, level + 1);
      }
   }

   public int recursiveDepth (BVNode node, int level) {
      if (node.isLeaf()) {
         return level;
      }
      else {
         int max = -1;
         BVNode child;
         for (child = node.myFirstChild; child != null; child = child.myNext) {
            int depth = recursiveDepth (child, level + 1);
            if (depth > max) {
               max = depth;
            }
         }
         return max;
      }
   }

   public int getDepth() {
      return recursiveDepth (root, 0);
   }

   public void render (Renderer renderer, int flags) {
      if (root != null) {
         renderer.pushModelMatrix();
         renderer.mulModelMatrix (myBvhToWorld);
         recursiveRender (renderer, flags, root, 0);
         renderer.popModelMatrix();
      }
   }

   public int numNodes() {
      return recursiveNumNodes (root);
   }

   public int recursiveNumNodes(BVNode node) {
      int num = 0;
      for (BVNode child = node.myFirstChild; child != null; child = child.myNext) {
         num += recursiveNumNodes (child);
      }
      return num + 1;
   }


   protected void updateRecursively (OBB node) {
      if (node.isLeaf()) {
         node.update (myMargin);
      }
      else {
         OBB child = (OBB)node.myFirstChild;
         while (child != null) {
            updateRecursively (child);
            child = (OBB)child.getNext();
         }
      }
   }

   public void update() {
      updateRecursively (getRoot());
   }

}
