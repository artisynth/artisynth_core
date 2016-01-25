/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.*;
import maspack.matrix.*;

public class NURBSMesh extends PolygonalMesh {
   // private double[] surfaceU;
   // private double[] surfaceV;

   private int[] NURBSIdxToGrid;

   private void createNURBSFaces (
      ArrayList<int[]> list, int i0, int i1, int i2, int i3, boolean triangular) {
      if (i0 == i1 || i1 == i2) {
         list.add (new int[] { i0, i2, i3 });
      }
      else if (i2 == i3 || i3 == i0) {
         list.add (new int[] { i0, i1, i2 });
      }
      else {
         if (triangular) {
            list.add (new int[] { i0, i1, i2 });
            list.add (new int[] { i2, i3, i0 });
         }
         else {
            list.add (new int[] { i0, i1, i2, i3 });
         }
      }
   }

   public void set (NURBSSurface surface, boolean triangular) {
      clear();
      double[] startEnd = new double[2];

      Vector4d[] ctrlPnts = surface.getControlPoints();
      int numCtrlPntsU = surface.numCtrlPntsU;
      int numCtrlPntsV = surface.numCtrlPntsV;

      int numVertsU = surface.getResolutionU() + 1;
      surface.getRangeU (startEnd);
      double ustart = startEnd[0];
      double uend = startEnd[1];

      int numVertsV = surface.getResolutionV() + 1;
      surface.getRangeV (startEnd);
      double vstart = startEnd[0];
      double vend = startEnd[1];

      // see if we should collapse the vertices for ustart, uend,
      // vstart, or vend

      boolean collapseVstart =
         ctrlPntsEqual (ctrlPnts, 0, numCtrlPntsV, numCtrlPntsU);
      boolean collapseVend =
         ctrlPntsEqual (ctrlPnts, numCtrlPntsV - 1, numCtrlPntsV, numCtrlPntsU);
      boolean collapseUstart = ctrlPntsEqual (ctrlPnts, 0, 1, numCtrlPntsV);
      boolean collapseUend =
         ctrlPntsEqual (
            ctrlPnts, (numCtrlPntsU - 1) * numCtrlPntsV, 1, numCtrlPntsV);

      if ((collapseUstart || collapseUend) && (collapseVstart || collapseVend)) {
         throw new IllegalArgumentException (
            "Control points spatiallly coincident along two dimensions");
      }

      // start by creating a grid of vertex indices ...

      int[] gridToIdx = new int[numVertsU * numVertsV];
      for (int i = 0; i < gridToIdx.length; i++) {
         gridToIdx[i] = i;
      }

      // modify these indices to account for surface closure
      // and collapsed vertices ...

      if (surface.ucurve.isClosed()) {
         int lastu = (numVertsU - 1);
         for (int j = 0; j < numVertsV; j++) {
            gridToIdx[lastu * numVertsV + j] = gridToIdx[j];
         }
      }

      if (surface.vcurve.isClosed()) {
         int lastv = (numVertsV - 1);
         for (int i = 0; i < numVertsU; i++) {
            gridToIdx[i * numVertsV + lastv] = gridToIdx[i * numVertsV];
         }
      }

      if (collapseVstart) {
         collapseIndices (gridToIdx, 0, numVertsV, numVertsU);
         System.out.println ("collasping vstart");
      }
      if (collapseVend) {
         collapseIndices (gridToIdx, numVertsV - 1, numVertsV, numVertsU);
         System.out.println ("collasping vend");
      }
      if (collapseUstart) {
         collapseIndices (gridToIdx, 0, 1, numVertsV);
      }
      if (collapseUend) {
         collapseIndices (gridToIdx, (numVertsU - 1) * numVertsV, 1, numVertsV);
      }

      // count the number of actual vertices left ...
      int numVerts = 0;
      for (int gridk = 0; gridk < gridToIdx.length; gridk++) {
         if (gridToIdx[gridk] == gridk) {
            numVerts++;
         }
      }

      // now allocate points accordingly and reindex the vertices

      Point3d[] pnts = new Point3d[numVerts];
      int[] idxToGrid = new int[numVerts];
      int[] reindex = new int[numVertsU * numVertsV];

      int k = 0;
      for (int i = 0; i < numVertsU; i++) {
         double u = ustart + (uend - ustart) * i / (numVertsU - 1.0);
         for (int j = 0; j < numVertsV; j++) {
            int gridk = i * numVertsV + j;
            if (gridToIdx[gridk] == gridk) {
               pnts[k] = new Point3d();
               double v = vstart + (vend - vstart) * j / (numVertsV - 1.0);
               surface.eval (pnts[k], u, v);
               reindex[gridk] = k;
               idxToGrid[k] = gridk;
               k++;
            }
         }
      }

      for (int gridk = 0; gridk < gridToIdx.length; gridk++) {
         gridToIdx[gridk] = reindex[gridToIdx[gridk]];
      }

      ArrayList<int[]> faceList = new ArrayList<int[]>();
      for (int i = 0; i < numVertsU - 1; i++) {
         for (int j = 0; j < numVertsV - 1; j++) {
            createNURBSFaces (
               faceList, gridToIdx[i * numVertsV + j],
               gridToIdx[i * numVertsV + j + 1],
               gridToIdx[(i + 1) * numVertsV + j + 1],
               gridToIdx[(i + 1) * numVertsV + j], triangular);
         }
      }
      set (pnts, (int[][])faceList.toArray (new int[0][]));
      setFixed (false);
      NURBSIdxToGrid = idxToGrid;
   }

   public void updateVertices (NURBSSurface surface)
      throws IllegalStateException {
      if (NURBSIdxToGrid == null) {
         throw new IllegalStateException (
            "mesh has not been created from a NURBS surface");
      }
      double[] startEnd = new double[2];

      int numVertsU = surface.getResolutionU() + 1;
      surface.getRangeU (startEnd);
      double ustart = startEnd[0];
      double uend = startEnd[1];

      int numVertsV = surface.getResolutionV() + 1;
      surface.getRangeV (startEnd);
      double vstart = startEnd[0];
      double vend = startEnd[1];

      for (int k = 0; k < numVertices(); k++) {
         Vertex3d vtx = (Vertex3d)myVertices.get (k);
         int i = NURBSIdxToGrid[k] / numVertsV;
         int j = NURBSIdxToGrid[k] % numVertsV;
         double u = ustart + (uend - ustart) * i / (numVertsU - 1.0);
         double v = vstart + (vend - vstart) * j / (numVertsV - 1.0);
         surface.eval (vtx.pnt, u, v);
      }
   }

}
