/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import maspack.geometry.AABBTree;
import maspack.geometry.BSPTree;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.BVFeatureQuery.InsideQuery;
import maspack.geometry.BVNode;
import maspack.geometry.BVTree;
import maspack.geometry.Boundable;
import maspack.geometry.Face;
import maspack.geometry.MeshBase;
import maspack.geometry.MeshFactory;
import maspack.geometry.OBB;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.graph.DirectedEdge;
import maspack.graph.DirectedGraph;
import maspack.graph.Vertex;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.Matrix3d;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.util.FastRadialMarcher;
import maspack.util.FunctionTimer;
import maspack.util.IndexedBinaryHeap;
import maspack.util.Point3dGridUtility;
import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.IntegrationData3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.PointFem3dAttachment;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.PointParticleAttachment;
import artisynth.core.mfreemodels.MFreeShapeFunction.MFreeShapeFunctionType;
import artisynth.core.mfreemodels.RadialWeightFunction.RadialWeightFunctionType;

public class MFreeFactory {

   public static double DEFAULT_TOLERANCE = 1e-10;
   public static int DEFAULT_GRID_IPNT_FACTOR = 2; // each "hex" has two points
   public static RadialWeightFunctionType DEFAULT_RADIAL_KERNEL_TYPE = 
      RadialWeightFunctionType.SPLINE;
   public static MFreeShapeFunctionType DEFAULT_SHAPE_FUNCTION_TYPE = 
      MFreeShapeFunctionType.MLS;

   public static MFreeModel3d createBeam(MFreeModel3d model,
      double[] size, int res[], double rfactor) {
      return createBeam(model, DEFAULT_RADIAL_KERNEL_TYPE, DEFAULT_SHAPE_FUNCTION_TYPE,
         size, res, rfactor);
   }
   
   public static MFreeModel3d createBeam(MFreeModel3d model,
      RadialWeightFunctionType fType, MFreeShapeFunctionType sType,
      double[] size, int res[], double rfactor) {

      int ires[] = new int[3];
      for (int i = 0; i < 3; i++) {
         ires[i] = (res[i] - 1) * DEFAULT_GRID_IPNT_FACTOR;
      }

      return createBeam(model, fType, sType, size, res, ires, rfactor);
   }
   
   public static MFreeModel3d createModel(MFreeModel3d model, PolygonalMesh surface, RigidTransform3d pos,
      int nNodes, int nIPnts) {
      return createModel(model, surface, pos, nNodes, nIPnts, null, -1);
   }
   
   public static MFreeModel3d createModel(MFreeModel3d model, PolygonalMesh surface, RigidTransform3d pos,
      int nNodes, int nIPnts, int[] gridRes, double rscale) {
      
      if (model == null) {
         model = new MFreeModel3d();
      }
      if (pos != null) {
         surface.transform(pos);   
      }
      if (gridRes == null) {
         OBB meshObb = new OBB (surface);
         //OBBNode rootObb = surface.getObbtree().getRoot().getObb();
         Vector3d hw = new Vector3d(meshObb.getHalfWidths());
         double vol = hw.x*hw.y*hw.z*8;
         double ipntVol = vol/nIPnts;
         
         // find rough dimensions of one ipnt, same proportions
         // as root OBB
         double scale = Math.pow(ipntVol/vol, 0.333);
         
         // we want at least 5 points along each dimension,
         // find minimum distance
         double min = hw.minElement();
         double minFactor = 1.0/scale*5;
         int nx = (int)Math.ceil(minFactor*hw.x/min);
         int ny = (int)Math.ceil(minFactor*hw.y/min);
         int nz = (int)Math.ceil(minFactor*hw.z/min);
         
         gridRes = new int[]{nx,ny,nz};
      }
      if (rscale <= 0) {
         rscale = 10;
      }
      
      surface.triangulate();

      double r =
         rscale * 3.0 / 4.0
            * Math.pow(surface.computeVolume() / nNodes, 0.333) / Math.PI;

      Point3d[] ipntLocs =
         generateNodes(surface, gridRes, nIPnts);
      Point3d[] nodeLocs = Arrays.copyOf(ipntLocs, nNodes); // cut first
                                                            // section

      createModel(
         model, nodeLocs, r, ipntLocs, RadialWeightFunctionType.SPLINE,
         MFreeShapeFunctionType.MLS, surface);

      model.updateNodeMasses(-1, null);
      
      return model;
   }
   
   public static Point3d[] generateNodes(PolygonalMesh mesh, int[] res, int nPoints) {

      Vector3d centroid = new Vector3d();
      mesh.computeCentroid(centroid);
      RigidTransform3d trans = new RigidTransform3d();
      trans.setTranslation(centroid);

      OBB obb = PointDistributor.getTightOBB(mesh, trans);
      Vector3d widths = new Vector3d();
      obb.getWidths(widths);
      obb.getTransform(trans);

      int nx = res[0];
      int ny = res[1];
      int nz = res[2];

      double dx, dy, dz;
      double xOffset, yOffset, zOffset;
      if (nx == 1) {
         widths.x = 0;
         dx = 0;
      } else {
         dx = widths.x / (nx - 1);
      }
      if (ny == 1) {
         widths.y = 0;
         dy = 0;
      } else {
         dy = widths.y / (ny - 1);
      }
      if (nz == 1) {
         widths.z = 0;
         dz = 0;
      } else {
         dz = widths.z / (nz - 1);
      }
      xOffset = -widths.x / 2;
      yOffset = -widths.y / 2;
      zOffset = -widths.z / 2;

      Point3d[][][] pnts = new Point3d[nx][ny][nz];

      BVFeatureQuery query = new BVFeatureQuery();
      double tol = 1e-15;

      double x, y, z;
      for (int i = 0; i < nx; i++) {
         x = xOffset + i * dx;
         for (int j = 0; j < ny; j++) {
            y = yOffset + j * dy;
            for (int k = 0; k < nz; k++) {
               z = zOffset + k * dz;
               Point3d pnt = new Point3d(x, y, z);
               pnt.transform(trans);

               InsideQuery rayTest = query.isInsideMesh(mesh, pnt, tol);
               if (rayTest == InsideQuery.INSIDE) {
                  pnts[i][j][k] = pnt;
               } else if (rayTest == InsideQuery.OUTSIDE) {
                  pnts[i][j][k] = null;
               } else {
                  System.out.println("unsure");
               }
            }
         }
      }

      Point3dGridUtility pgu = new Point3dGridUtility(pnts);
      FastRadialMarcher marcher = new FastRadialMarcher(nx * ny * nz, pgu);

      marcher.initializeArrays();
      double[] dists = marcher.getDistance();
      for (int i = 0; i < nx; i++) {
         for (int j = 0; j < ny; j++) {
            for (int k = 0; k < nz; k++) {
               if (pnts[i][j][k] == null) {
                  dists[k + j * nz + i * ny * nz] = -1;
               }
            }
         }
      }
      marcher.getDistanceHeap().clear();
      marcher.getDistanceHeap().setAll();

      Point3d[] out = new Point3d[nPoints];
      int i, j, k;
      for (int idx = 0; idx < nPoints; idx++) {
         int farthest = 0;
         IndexedBinaryHeap dheap = marcher.getDistanceHeap();
         farthest = dheap.peek();
         int nextSample = farthest;

         // idx = i*ny*nz+j*nz+k
         k = nextSample % nz;
         j = ((nextSample - k) / nz) % ny;
         i = (nextSample - k - j * nz) / (ny * nz);

         out[idx] = pnts[i][j][k];
         marcher.march(nextSample);
      }

      return out;

   }


   public static MFreeModel3d createBeam(MFreeModel3d model,
      RadialWeightFunctionType fType, MFreeShapeFunctionType sType,
      double[] size, int res[], int[] ipntRes, double rfactor) {

      ArrayList<MFreeNode3d> nodeList = new ArrayList<MFreeNode3d>();

      if (rfactor <= 0) {
         rfactor = 1.15;
      }

      // nodes
      double x, y, z;
      double dx = size[0] / (res[0]-1);
      double dy = size[1] / (res[1]-1);
      double dz = size[2] / (res[2]-1);

      double prad = Math.max(Math.max(dx, dy), dz) * rfactor;

      // nodes
      for (int i = 0; i < res[0]; i++) {
         for (int j = 0; j < res[1]; j++) {
            for (int k = 0; k < res[2]; k++) {
               x = -size[0] / 2 + dx * i;
               y = -size[1] / 2 + dy * j;
               z = -size[2] / 2 + dz * k;

               MFreeNode3d node = createNode(x, y, z, prad, fType, sType);
               nodeList.add(node);

            }
         }
      }

      // surface mesh
      PolygonalMesh surface =
         MeshFactory.createQuadBox(
            size[0], size[1], size[2], Point3d.ZERO, res[0]-1, res[1]-1, res[2]-1);
      surface.triangulate();

      // offset ipnts from border
      dx = size[0] / ipntRes[0];
      dy = size[1] / ipntRes[1];
      dz = size[2] / ipntRes[2];
      double[] ipntSize = { size[0] - dx, size[1] - dy, size[2] - dz };
      Point3d[] pnts =
         PointDistributor.getGrid(ipntSize, ipntRes, Point3d.ZERO);

      CubaturePoint3d[] cpnts = new CubaturePoint3d[pnts.length];
      double wp = surface.computeVolume() / pnts.length; // equal weighting
      for (int i = 0; i < pnts.length; i++) {
         cpnts[i] = new CubaturePoint3d(pnts[i], wp);
      }
      return createModel(model, nodeList, surface, cpnts);

   }

   public static MFreeModel3d createModel(MFreeModel3d model,
      Point3d[] nodeLocs, double nodeRad,
      Point3d[] ipntLoc, PolygonalMesh surface) {
   
      return createModel(model, nodeLocs, nodeRad, ipntLoc, 
         DEFAULT_RADIAL_KERNEL_TYPE, DEFAULT_SHAPE_FUNCTION_TYPE,
         surface);
      
   }
   
   
   public static MFreeModel3d createModel(MFreeModel3d model,
      Point3d[] nodeLocs, double nodeRad,
      Point3d[] ipntLoc, RadialWeightFunctionType fType,
      MFreeShapeFunctionType sType, PolygonalMesh surface) {

      if (model == null) {
         model = new MFreeModel3d();
      }

      ArrayList<MFreeNode3d> nodeList = new ArrayList<MFreeNode3d>();

      for (Point3d pnt : nodeLocs) {
         MFreeNode3d node =
            createNode(pnt.x, pnt.y, pnt.z, nodeRad, fType, sType);
         nodeList.add(node);
      }
      surface.triangulate();

      CubaturePoint3d[] cpnts = new CubaturePoint3d[ipntLoc.length];
      double wp = surface.computeVolume() / ipntLoc.length;
      for (int i = 0; i < ipntLoc.length; i++) {
         cpnts[i] = new CubaturePoint3d(ipntLoc[i], wp);
      }

      return createModel(model, nodeList, surface, cpnts);

   }
   
   public static MFreeModel3d createPairedModel(MFreeModel3d model,
      Point3d[] nodeLocs, double nodeRad,
      Point3d[] ipntLoc, RadialWeightFunctionType fType,
      MFreeShapeFunctionType sType, PolygonalMesh surface) {

      if (model == null) {
         model = new MFreeModel3d();
      }

      ArrayList<MFreeNode3d> nodeList = new ArrayList<MFreeNode3d>();

      for (Point3d pnt : nodeLocs) {
         MFreeNode3d node =
            createNode(pnt.x, pnt.y, pnt.z, nodeRad, fType, sType);
         nodeList.add(node);
      }
      surface.triangulate();

      CubaturePoint3d[] cpnts = new CubaturePoint3d[ipntLoc.length];
      double wp = surface.computeVolume() / ipntLoc.length;
      for (int i = 0; i < ipntLoc.length; i++) {
         cpnts[i] = new CubaturePoint3d(ipntLoc[i], wp);
      }

      return createPairedModel(model, nodeList, surface, cpnts);

   }

   public static MFreeModel3d createPairedModel(MFreeModel3d model,
      List<MFreeNode3d> nodes, PolygonalMesh surface,
      CubaturePoint3d[] cpnts) {

      if (model == null) {
         model = new MFreeModel3d();
      }

      MFreeNode3d[] nodeArray = nodes.toArray(new MFreeNode3d[nodes.size()]);
      boolean[][] iChart = buildIntersectionChart(nodeArray);

      AABBTree tree = new AABBTree();
      tree.build(nodeArray, nodeArray.length);

      // compute node coordinates
      for (MFreeNode3d node : nodes) {
         ArrayList<MFreeNode3d> deps =
            findNodesContaining(node.getRestPosition(), tree, 0);
         VectorNd coords = new VectorNd();
         getShapeCoords(coords, node.getRestPosition(), deps);
         node.setDependentNodes(deps, coords);
      }
      model.addNodes(nodes);

      ArrayList<MFreeElement3d> elemList = createPairedElements(nodes, iChart);

      MFreeIntegrationPoint3d[] ipnts = createIntegrationPoints(cpnts, tree);

      MFreeElement3d[] elemArray =
         elemList.toArray(new MFreeElement3d[elemList.size()]);
      AABBTree elemTree = new AABBTree();
      elemTree.build(elemArray, elemArray.length);

      distributePairedIPoints(elemArray, ipnts, null, elemTree, 0);
      trimEmptyElements(elemList);

      addWarpingPoints(elemList, tree);

      surface = (PolygonalMesh)convertToMFreeMesh(surface, tree, DEFAULT_TOLERANCE);

      model.addElements(elemList);
      model.addMesh(surface);
      model.updateNodeMasses(-1, null);

      return model;

   }

   public static MFreeModel3d createModel(MFreeModel3d model,
      List<MFreeNode3d> nodes, PolygonalMesh surface,
      CubaturePoint3d[] cpnts) {

      MFreeNode3d[] nodeArray = nodes.toArray(new MFreeNode3d[nodes.size()]);

      FunctionTimer timer = new FunctionTimer();

      // timer.start();
      // boolean[][] iChart = buildIntersectionChart(nodeArray);
      // timer.stop();
      // System.out.println("Intersection chart: " + timer.getTimeUsec()/1000 +
      // " ms");

      AABBTree nodeTree = new AABBTree();

      timer.start();
      nodeTree.build(nodeArray, nodeArray.length);
      timer.stop();
      System.out.println("Node BVTree: " + timer.getTimeUsec() / 1000
         + " ms");

      // compute node coordinates
      timer.start();
      for (MFreeNode3d node : nodes) {
         ArrayList<MFreeNode3d> deps =
            findNodesContaining(node.getRestPosition(), nodeTree, 0);
         VectorNd coords = new VectorNd();
         getShapeCoords(coords, node.getRestPosition(), deps);
         node.setDependentNodes(deps, coords);
      }
      timer.stop();
      System.out.println("Node coordinates: " + timer.getTimeUsec() / 1000
         + " ms");

      // timer.start();
      // DirectedGraph<int[], Integer> connectivityGraph =
      // IntersectionFactory.buildConnectivityGraph(iChart);
      // timer.stop();
      // System.out.println("Connectivity graph: " + timer.getTimeUsec()/1000 +
      // " ms");
      // timer.start();
      // ArrayList<MFreeElement3d> elemList = createPartitionedElements(nodes,
      // connectivityGraph);
      // timer.stop();
      // System.out.println("Partitioned elements: " + timer.getTimeUsec()/1000
      // + " ms");

      timer.start();
      MFreeIntegrationPoint3d[] ipnts =
         createIntegrationPoints(cpnts, nodeTree);
      timer.stop();
      System.out.println("Integration points: " + timer.getTimeUsec() / 1000
         + " ms");

      HashMap<MFreeIntegrationPoint3d,MFreeElement3d> pntMap =
         new HashMap<MFreeIntegrationPoint3d,MFreeElement3d>(ipnts.length);
      timer.start();
      ArrayList<MFreeElement3d> elemList =
         createPartitionedElementsFromPoints(ipnts, pntMap);
      timer.stop();
      System.out.println("Building elements from points: "
         + timer.getTimeUsec() / 1000 + " ms");

      timer.start();
      distributeIPointsFromMap(pntMap);
      timer.stop();
      System.out.println("Distributing integration Points: "
         + timer.getTimeUsec() / 1000 + " ms");

      trimEmptyElements(elemList);

      addWarpingPoints(elemList, nodeTree);

      surface = (PolygonalMesh)convertToMFreeMesh(surface, nodeTree, DEFAULT_TOLERANCE);

      if (model == null) {
         model = new MFreeModel3d();
      }

      model.addNodes(nodes);
      model.addElements(elemList);
      model.addMesh(surface);
      model.updateNodeMasses(-1, null);

      return model;

   }

   public static ArrayList<MFreeElement3d> trimEmptyElements(
      List<MFreeElement3d> elemList) {

      ArrayList<MFreeElement3d> removed = new ArrayList<MFreeElement3d>();
      Iterator<MFreeElement3d> elemIt = elemList.iterator();

      while (elemIt.hasNext()) {
         MFreeElement3d elem = elemIt.next();
         if (elem.numIntegrationPoints() == 0 || elem.numNodes() == 0) {
            elemIt.remove();
            removed.add(elem);
         }
      }

      return removed;

   }

   public static MFreeIntegrationPoint3d[] createIntegrationPoints(
      CubaturePoint3d[] cpnts, BVTree nodeTree) {

      MFreeIntegrationPoint3d[] ipnts =
         new MFreeIntegrationPoint3d[cpnts.length];

      for (int i = 0; i < cpnts.length; i++) {
         ArrayList<MFreeNode3d> deps =
            findNodesContaining(cpnts[i], nodeTree, 0);
         VectorNd coords = new VectorNd(deps.size());
         ArrayList<Vector3d> grad = new ArrayList<Vector3d>(deps.size());
         getShapeCoordsAndGradients(coords, grad, cpnts[i], deps);
         ipnts[i] =
            MFreeIntegrationPoint3d.create(deps, coords, grad, cpnts[i].w);
         ipnts[i].setID(i);
      }

      return ipnts;
   }

   public static void distributePairedIPoints(MFreeElement3d[] elemList,
      MFreeIntegrationPoint3d[] ipnts,
      IntegrationData3d[] idata, BVTree elemTree, double tol) {

      for (int i = 0; i < ipnts.length; i++) {
         MFreeIntegrationPoint3d ipnt = ipnts[i];
         IntegrationData3d idat = null;
         if (idata != null) {
            idat = idata[i];
         } else {
            idat = new IntegrationData3d();
            idat.setRestInverseJacobian(new Matrix3d(Matrix3d.IDENTITY), 1);
         }

         ArrayList<MFreeElement3d> celems =
            findPairedElementsContaining(ipnt.getPosition(), elemTree, tol);
         for (MFreeElement3d elem : celems) {
            elem.addIntegrationPoint(ipnt, idat, ipnt.getWeight(), false);
         }
      }

      for (MFreeElement3d elem : elemList) {
         elem.updateAllVolumes();
      }

   }

   public static void distributeIPointsFromMap(
      HashMap<MFreeIntegrationPoint3d,MFreeElement3d> pntMap) {

      for (MFreeIntegrationPoint3d ipnt : pntMap.keySet()) {
         IntegrationData3d idat = new IntegrationData3d();
         idat.setRestInverseJacobian(new Matrix3d(Matrix3d.IDENTITY), 1);
         MFreeElement3d elem = pntMap.get(ipnt);
         elem.addIntegrationPoint(ipnt, idat, ipnt.getWeight(), false);
      }

      for (MFreeElement3d elem : pntMap.values()) {
         elem.updateAllVolumes();
      }

   }

   public static void distributePartitionedIPoints(
      DirectedGraph<MFreeElement3d,MFreeNode3d> connectivity,
      MFreeIntegrationPoint3d[] ipnts,
      IntegrationData3d[] idata, BVTree elemTree, double tol) {

      for (int i = 0; i < ipnts.length; i++) {
         MFreeIntegrationPoint3d ipnt = ipnts[i];
         IntegrationData3d idat = null;
         if (idata != null) {
            idat = idata[i];
         } else {
            idat = new IntegrationData3d();
            idat.setRestInverseJacobian(new Matrix3d(Matrix3d.IDENTITY), 1);
         }

         ArrayList<MFreeElement3d> celems =
            findPartitionedElementsContaining(
               ipnt.getPosition(), connectivity, elemTree, tol);
         for (MFreeElement3d elem : celems) {
            elem.addIntegrationPoint(ipnt, idat, ipnt.getWeight(), false);
         }
      }

      for (Vertex<MFreeElement3d,MFreeNode3d> vtx : connectivity.getVertices()) {
         MFreeElement3d elem = vtx.getData();
         if (elem != null) {
            elem.updateAllVolumes();
         }
      }

   }

   public static void addWarpingPoints(List<MFreeElement3d> elems,
      BVTree nodeTree) {
      for (MFreeElement3d elem : elems) {
         MFreeIntegrationPoint3d wpnt =
            createWarpingPoint(elem, null, nodeTree);
         IntegrationData3d wdat = new IntegrationData3d();
         wdat.setRestInverseJacobian(new Matrix3d(Matrix3d.IDENTITY), 1);
         elem.setWarpingPoint(wpnt, wdat);
      }
   }

   public static MFreeIntegrationPoint3d createWarpingPoint(
      MFreeElement3d elem, Point3d pos, BVTree nodeTree) {
      if (pos == null) {
         pos = new Point3d();
         elem.computeCentroid(pos);
      }

      ArrayList<MFreeNode3d> deps = findNodesContaining(pos, nodeTree, 0);
      VectorNd coords = new VectorNd(deps.size());
      ArrayList<Vector3d> grad = new ArrayList<Vector3d>(deps.size());
      getShapeCoordsAndGradients(coords, grad, pos, deps);
      return MFreeIntegrationPoint3d.create(deps, coords, grad, 1);

   }

   public static ArrayList<MFreeElement3d> createPartitionedElements(
      List<MFreeNode3d> nodes, DirectedGraph<int[],Integer> connectivityGraph) {

      ArrayList<MFreeElement3d> elems =
         new ArrayList<MFreeElement3d>(connectivityGraph.numVertices());

      for (Vertex<int[],Integer> vtx : connectivityGraph.getVertices()) {

         int[] idxs = vtx.getData();
         if (idxs.length > 0) {
            MFreeNode3d[] enodes = new MFreeNode3d[idxs.length];
            for (int i = 0; i < idxs.length; i++) {
               enodes[i] = nodes.get(idxs[i]);
            }
            MFreeElement3d elem = new MFreeElement3d(enodes);
            elem.setAllTermsActive(true);
            elems.add(elem);
         }
      }

      return elems;

   }

   public static ArrayList<MFreeElement3d> createPairedElements(
      List<MFreeNode3d> nodes, boolean[][] iChart) {
      // elements from node pairs
      ArrayList<MFreeElement3d> elemList = new ArrayList<MFreeElement3d>();
      for (int i = 0; i < nodes.size(); i++) {
         MFreeNode3d nodeA = nodes.get(i);
         MFreeElement3d e = new MFreeElement3d(new MFreeNode3d[] { nodeA });
         e.setTermActive(0, 0, true);
         elemList.add(e);
         for (int j = i + 1; j < nodes.size(); j++) {
            if (iChart[i][j]) {
               e =
                  new MFreeElement3d(new MFreeNode3d[] { nodeA, nodes.get(j) });
               e.setTermActive(0, 1, true);
               e.setTermActive(1, 0, true);
               e.setTermActive(0, 0, false);
               e.setTermActive(1, 1, false);
               elemList.add(e);
            }
         }
      }

      return elemList;
   }

   public static void createNodeMeshes(List<MFreeNode3d> nodes,
      BVTree nodeTree, PolygonalMesh surface) {

      // set nodal influence regions
      PolygonalMesh icoSphere = MeshFactory.createIcosahedralSphere(1, 2);

      HashMap<MFreeNode3d,PolygonalMesh> meshMap =
         new HashMap<MFreeNode3d,PolygonalMesh>();
      AffineTransform3d trans = new AffineTransform3d();
      for (MFreeNode3d node : nodes) {
         PolygonalMesh nmesh = null;
         if (node.isRadial()) {
            nmesh = new PolygonalMesh(icoSphere);
            double r = node.getInfluenceRadius();
            trans.setIdentity();
            trans.setTranslation(node.getRestPosition());
            trans.applyScaling(r, r, r);
            nmesh.transform(trans);
         }
         if (nmesh != null) {
            nmesh = MeshFactory.getIntersection(nmesh, surface);
            nmesh = (PolygonalMesh)convertToMFreeMesh(nmesh, nodeTree, DEFAULT_TOLERANCE);
            meshMap.put(node, nmesh);
         }
      }

      // I do this after generating all meshes so that isInDomain doesn't start
      // using the meshes before all are ready (speed issue)
      for (MFreeNode3d node : nodes) {
         PolygonalMesh nmesh = meshMap.get(node);
         if (nmesh != null) {
            node.setBoundaryMesh(nmesh);
         }
      }
   }

   public static void createPairedElemMeshes(List<MFreeElement3d> elemList,
      BVTree nodeTree) {

      // only intersections
      for (MFreeElement3d elem : elemList) {
         if (elem.numNodes() == 1) {
            elem.setBoundaryMesh(elem.getNode(0).getBoundaryMesh());
         } else {
            PolygonalMesh mesh =
               new PolygonalMesh(elem.getNode(0).getBoundaryMesh());
            for (int i = 1; i < elem.numNodes(); i++) {
               mesh =
                  MeshFactory.getIntersection(mesh, elem
                     .getNode(i).getBoundaryMesh());
            }
            mesh = (PolygonalMesh)convertToMFreeMesh(mesh, nodeTree, DEFAULT_TOLERANCE);
            elem.setBoundaryMesh(mesh);
         }
      }

   }

   public static void createPartitionedElemMeshes(
      List<MFreeElement3d> elemList, List<MFreeNode3d> nodeList,
      BVTree nodeTree, DirectedGraph<int[],Integer> connectivityGraph) {

      HashMap<Integer,BSPTree> meshMap =
         new HashMap<Integer,BSPTree>(nodeList.size());
      HashMap<BSPTree,MFreeNode3d> meshMapInv =
         new HashMap<BSPTree,MFreeNode3d>(nodeList.size());

      HashMap<int[],BSPTree> nullMap = new HashMap<int[],BSPTree>(1);
      for (int i = 0; i < nodeList.size(); i++) {
         MFreeNode3d node = nodeList.get(i);
         BSPTree tree = new BSPTree(node.getBoundaryMesh());
         meshMap.put(i, tree);
         meshMapInv.put(tree, node);
      }
      DirectedGraph<BSPTree,BSPTree> meshGraph =
         connectivityGraph.exchangeData(nullMap, meshMap);
      IntersectionFactory.buildSpatialPartition(meshGraph, null);
      DirectedGraph<BSPTree,MFreeNode3d> nodeGraph =
         meshGraph.exchangeEdgeData(meshMapInv);
      Vertex<BSPTree,MFreeNode3d> root = nodeGraph.getVertex(0);

      for (MFreeElement3d elem : elemList) {

         Vertex<BSPTree,MFreeNode3d> vtx = root;
         for (MFreeNode3d node : elem.getNodes()) {
            for (DirectedEdge<BSPTree,MFreeNode3d> edge : vtx.getForwardEdges()) {
               if (edge.getData() == node) {
                  vtx = edge.traverseForwards();
                  break;
               }
            }
         }

         PolygonalMesh mesh = vtx.getData().generateMesh();
         if (mesh.numFaces() > 0) {
            mesh = (PolygonalMesh)convertToMFreeMesh(mesh, nodeTree, DEFAULT_TOLERANCE);
            elem.setBoundaryMesh(mesh);
         }

      }
   }

   public static ArrayList<MFreeElement3d> findPairdElementsContaining(
      Point3d pnt, BVTree bvtree, double tol) {

      ArrayList<MFreeElement3d> deps = new ArrayList<MFreeElement3d>();
      ArrayList<BVNode> bvNodes = new ArrayList<BVNode>(16);
      bvtree.intersectPoint(bvNodes, pnt);

      if (bvNodes.size() == 0) {
         return deps;
      }

      for (BVNode n : bvNodes) {
         Boundable[] elements = n.getElements();
         for (int i = 0; i < elements.length; i++) {
            MFreeElement3d elem = (MFreeElement3d)elements[i];

            boolean isInside = true;
            for (MFreeNode3d node : elem.getNodes()) {
               if (!node.isInDomain(pnt, tol)) {
                  isInside = false;
                  break;
               }
            }
            if (isInside) {
               deps.add(elem);
            }
         }
      }
      return deps;
   }

   public static ArrayList<MFreeElement3d> findPartitionedElementsContaining(
      Point3d pnt, DirectedGraph<MFreeElement3d,MFreeNode3d> connectivity,
      BVTree bvtree, double tol) {

      ArrayList<MFreeElement3d> deps = new ArrayList<MFreeElement3d>();
      ArrayList<BVNode> bvNodes = new ArrayList<BVNode>(16);
      bvtree.intersectPoint(bvNodes, pnt);

      if (bvNodes.size() == 0) {
         return deps;
      }

      for (BVNode n : bvNodes) {
         Boundable[] elements = n.getElements();
         for (int i = 0; i < elements.length; i++) {
            MFreeElement3d elem = (MFreeElement3d)elements[i];

            boolean isInside = true;
            for (MFreeNode3d node : elem.getNodes()) {
               if (!node.isInDomain(pnt, tol)) {
                  isInside = false;
                  break;
               }
            }

            // exclude deeper intersections
            if (isInside) {
               Vertex<MFreeElement3d,MFreeNode3d> vtx =
                  connectivity.findVertex(elem);
               for (DirectedEdge<MFreeElement3d,MFreeNode3d> edge : vtx
                  .getForwardEdges()) {
                  MFreeNode3d node = edge.getData();
                  if (node.isInDomain(pnt, tol)) {
                     isInside = false;
                     break;
                  }
               }
            }
            if (isInside) {
               deps.add(elem);
            }
         }
      }
      return deps;
   }

   public static DirectedGraph<MFreeElement3d,MFreeNode3d> convertConnectivity(
      List<MFreeNode3d> nodes, List<MFreeElement3d> elems,
      DirectedGraph<int[],Integer> graph) {

      DirectedGraph<MFreeElement3d,MFreeNode3d> out = graph.cloneStructure();

      // copy over nodes
      for (int i = 0; i < graph.numDirectedEdges(); i++) {
         DirectedEdge<int[],Integer> idxEdge = graph.getDirectedEdge(i);
         DirectedEdge<MFreeElement3d,MFreeNode3d> nodeEdge =
            out.getDirectedEdge(i);
         nodeEdge.setData(nodes.get(idxEdge.getData()));
      }

      // fill in vertices by traversing along edges
      Vertex<MFreeElement3d,MFreeNode3d> base = out.getVertex(0);
      for (MFreeElement3d elem : elems) {
         MFreeNode3d[] nodeArray = elem.getNodes();
         Vertex<MFreeElement3d,MFreeNode3d> elemVtx =
            out.traverseEdgesForward(base, nodeArray);
         elemVtx.setData(elem);
      }

      return out;

   }

   public static ArrayList<MFreeElement3d> findPairedElementsContaining(
      Point3d pnt, BVTree bvtree, double tol) {

      ArrayList<MFreeElement3d> deps = new ArrayList<MFreeElement3d>();
      ArrayList<BVNode> bvNodes = new ArrayList<BVNode>(16);
      bvtree.intersectPoint(bvNodes, pnt);

      if (bvNodes.size() == 0) {
         return deps;
      }

      for (BVNode n : bvNodes) {
         Boundable[] elements = n.getElements();
         for (int i = 0; i < elements.length; i++) {
            MFreeElement3d elem = (MFreeElement3d)elements[i];

            boolean isInside = true;
            for (MFreeNode3d node : elem.getNodes()) {
               if (!node.isInDomain(pnt, tol)) {
                  isInside = false;
                  break;
               }
            }
            if (isInside) {
               deps.add(elem);
            }
         }
      }
      return deps;
   }

   public static ArrayList<MFreeNode3d> findNodesContaining(Point3d pnt,
      BVTree bvtree, double tol) {

      ArrayList<MFreeNode3d> deps = new ArrayList<MFreeNode3d>();
      ArrayList<BVNode> bvNodes = new ArrayList<BVNode>(16);
      bvtree.intersectPoint(bvNodes, pnt);

      if (bvNodes.size() == 0) {
         return deps;
      }

      for (BVNode n : bvNodes) {
         Boundable[] elements = n.getElements();
         for (int i = 0; i < elements.length; i++) {
            if (((MFreeNode3d)elements[i]).isInDomain(pnt, tol)) {
               deps.add((MFreeNode3d)elements[i]);
            }
         }
      }
      return deps;
   }

   private static MFreeNode3d createNode(double x, double y, double z,
      double rad, RadialWeightFunctionType wType, MFreeShapeFunctionType sType) {
      MFreeNode3d node = new MFreeNode3d(x, y, z);
      RadialWeightFunction ffun =
         RadialWeightFunction.createWeightFunction(
            wType, node.getRestPosition(), rad);
      MFreeShapeFunction sfun =
         MFreeShapeFunction.create(sType, node);
      node.setWeightFunction(ffun);
      node.setShapeFunction(sfun);
      return node;
   }

   public static void updatePointCoordinates(List<? extends MFreePoint3d> pnts,
      BVTree nodeTree, double tol) {

      VectorNd coords = new VectorNd();
      for (MFreePoint3d pnt : pnts) {
         ArrayList<MFreeNode3d> deps =
            findNodesContaining(pnt.getRestPosition(), nodeTree, tol);
         getShapeCoords(coords, pnt.getRestPosition(), deps);
         pnt.setDependentNodes(deps, coords);
      }
   }

   public static MeshBase convertToMFreeMesh(MeshBase orig,
      BVTree nodeTree, double tol) {

      VectorNd coords = new VectorNd();

      HashMap<Vertex3d,MFreeVertex3d> vtxMap =
         new HashMap<Vertex3d,MFreeVertex3d>();

      ArrayList<MFreeVertex3d> vtxs = 
         new ArrayList<MFreeVertex3d>(orig.numVertices());
      for (Vertex3d vtx : orig.getVertices()) {
         ArrayList<MFreeNode3d> deps =
            findNodesContaining(vtx.getPosition(), nodeTree, tol);
         getShapeCoords(coords, vtx.getPosition(), deps);
         MFreeVertex3d nvtx = new MFreeVertex3d(deps, coords);
         vtxMap.put(vtx, nvtx);
         vtxs.add(nvtx);
      }
      MeshBase out = orig.copy();
      out.replaceVertices (vtxs);
      out.setFixed(false);
      
      // copy other properties
      out.setName(orig.getName());
      out.setRenderProps(orig.getRenderProps());

      return out;

   }

   private static void getShapeCoords(VectorNd coords, Point3d pnt,
      ArrayList<MFreeNode3d> deps) {

      int nDeps = deps.size();
      coords.setSize(deps.size());
      MatrixNd MInv = null;

      for (int i = 0; i < nDeps; i++) {
         MFreeNode3d bnode = deps.get(i);
         MFreeShapeFunction fun = bnode.getShapeFunction();

         if (fun instanceof MLSShapeFunction) {
            MLSShapeFunction mls = (MLSShapeFunction)fun;
            if (MInv == null) {
               MInv = new MatrixNd();
               double d = mls.computeMInv(MInv, pnt, deps);
               if (d > 1e10) {
                  System.out.println("Poorly conditioned point: " + pnt);
               }
            }

            double v = mls.eval(pnt, MInv, deps);
            coords.set(i, v);

         } else {
            coords.set(i, fun.eval(pnt));
         }
      }

   }

   private static int getShapeCoordsAndGradients(VectorNd coords,
      ArrayList<Vector3d> grad,
      Point3d pnt, ArrayList<MFreeNode3d> deps) {

      int nDeps = deps.size();
      grad.clear();
      grad.ensureCapacity(nDeps);
      coords.setSize(deps.size());

      MatrixNd MInv = null;
      int[] dx = { 1, 0, 0 };
      int[] dy = { 0, 1, 0 };
      int[] dz = { 0, 0, 1 };

      for (int i = 0; i < nDeps; i++) {
         MFreeNode3d bnode = deps.get(i);
         MFreeShapeFunction fun = bnode.getShapeFunction();

         if (fun instanceof MLSShapeFunction) {
            MLSShapeFunction mls = (MLSShapeFunction)fun;
            if (MInv == null) {
               MInv = new MatrixNd();
               double d = mls.computeMInv(MInv, pnt, deps);
               if (d > 1e10) {
                  System.out.println("Poorly conditioned point: " + pnt);
               }
            }

            double v = mls.eval(pnt, MInv, deps);
            coords.set(i, v);

            Vector3d nodegrad = new Vector3d();
            nodegrad.x = mls.evalDerivative(pnt, dx, MInv, deps);
            nodegrad.y = mls.evalDerivative(pnt, dy, MInv, deps);
            nodegrad.z = mls.evalDerivative(pnt, dz, MInv, deps);
            grad.add(nodegrad);

         } else {
            coords.set(i, fun.eval(pnt));
            Vector3d nodegrad = new Vector3d();
            nodegrad.x = fun.evalDerivative(pnt, dx);
            nodegrad.y = fun.evalDerivative(pnt, dy);
            nodegrad.z = fun.evalDerivative(pnt, dz);
            grad.add(nodegrad);
         }
      }

      return nDeps;
   }

   public static MFreeModel3d cloneFem(MFreeModel3d model, FemModel3d fem) {

      if (model == null) {
         model = new MFreeModel3d();
      }

      HashMap<FemNode3d,MFreeNode3d> nodeMap =
         new HashMap<FemNode3d,MFreeNode3d>();
      HashMap<MFreeNode3d,FemNode3d> nodeMapInv =
         new HashMap<MFreeNode3d,FemNode3d>();
      ArrayList<MFreeNode3d> nodeList = new ArrayList<MFreeNode3d>();

      // duplicate nodes
      for (FemNode3d node : fem.getNodes()) {
         MFreeNode3d mnode = new MFreeNode3d(node.getRestPosition());
         ArrayList<MFreeNode3d> deps = new ArrayList<MFreeNode3d>(1);
         deps.add(mnode);
         VectorNd coords = new VectorNd(new double[] { 1 });
         mnode.setDependentNodes(deps, coords);
         nodeMap.put(node, mnode);
         nodeMapInv.put(mnode, node);
         nodeList.add(mnode);
      }

      // convert surface mesh
      FemMeshComp surfaceFem = fem.getSurfaceMeshComp ();
      PolygonalMesh mesh = (PolygonalMesh)surfaceFem.getMesh ();
      PolygonalMesh surfaceMFree = new PolygonalMesh();
      HashMap<Vertex3d,MFreeVertex3d> vtxMap = new HashMap<Vertex3d,MFreeVertex3d>();
      
      for (Vertex3d vtx : mesh.getVertices()) {
         
         MFreeVertex3d mvtx = null;
         
         PointAttachment pa = surfaceFem.getAttachment(vtx.getIndex());
         
         if (pa instanceof PointFem3dAttachment) {
            PointFem3dAttachment pfa = (PointFem3dAttachment)pa;
            FemNode[] masters = pfa.getNodes();
            
            ArrayList<MFreeNode3d> deps = new ArrayList<MFreeNode3d>(masters.length);
            VectorNd coords = new VectorNd(masters.length);
            
            for (int j=0; j<masters.length; j++) {
               //mlist.add (new ContactMaster (masters[j], pfa.getCoordinate(j)));
               deps.add (nodeMap.get (masters[j]));
               coords.set (j, pfa.getCoordinate (j));
            }
            
            mvtx = new MFreeVertex3d (deps, coords);
            
         }
         else {
            PointParticleAttachment ppa = (PointParticleAttachment)pa;
            DynamicComponent[] masters = ppa.getMasters ();

            ArrayList<MFreeNode3d> deps = new ArrayList<MFreeNode3d>(1);
            deps.add (nodeMap.get (masters[0]));
            VectorNd coords = new VectorNd(new double[] { 1 });
            mvtx = new MFreeVertex3d (deps, coords);
            
         }     
         
         vtxMap.put(vtx, mvtx);
         surfaceMFree.addVertex(mvtx);
      }
      
      for (Face face : mesh.getFaces()) {
         Vertex3d[] verts = new Vertex3d[face.numVertices()];
         for (int i = 0; i < face.numVertices(); i++) {
            verts[i] = vtxMap.get(face.getVertex(i));
         }
         surfaceMFree.addFace(verts);
      }

      // integration regions by copying elements
      ArrayList<MFreeElement3d> elemList =
         new ArrayList<MFreeElement3d>(fem.numElements());
      HashMap<FemElement3d,MFreeElement3d> elemMap =
         new HashMap<FemElement3d,MFreeElement3d>(fem.numElements());

      for (FemElement3d elem : fem.getElements()) {
         MFreeNode3d[] elemNodes = new MFreeNode3d[elem.numNodes()];
         FemNode3d[] fnodes = elem.getNodes();
         for (int i = 0; i < elem.numNodes(); i++) {
            elemNodes[i] = nodeMap.get(fnodes[i]);
         }

         MFreeElement3d region = new MFreeElement3d(elemNodes);
         region.setAllTermsActive(true);

         ArrayList<MFreeIntegrationPoint3d> mpnts =
            new ArrayList<MFreeIntegrationPoint3d>(elem.numIntegrationPoints());
         ArrayList<IntegrationData3d> mdata =
            new ArrayList<IntegrationData3d>(elem.numIntegrationPoints());
         VectorNd mwgts = new VectorNd(elem.numIntegrationPoints());

         IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
         IntegrationData3d[] idata = elem.getIntegrationData();
         ArrayList<MFreeNode3d> deps =
            new ArrayList<MFreeNode3d>(elem.numNodes());
         for (MFreeNode3d node : elemNodes) {
            deps.add(node);
         }

         for (int i = 0; i < ipnts.length; i++) {
            Point3d pos = new Point3d();
            ipnts[i].computePosition(pos, elem.getNodes());
            // Vector3d [] gradU =
            // ipnts[i].updateShapeGradient(idata[i].getInvJ0());
            Vector3d[] gradU = ipnts[i].getGNs();
            ArrayList<Vector3d> grads = new ArrayList<Vector3d>();
            for (Vector3d g : gradU) {
               grads.add(g);
            }

            // MFreeIntegrationPoint3d mpnt =
            // MFreeIntegrationPoint3d.create(pos, deps,
            // ipnts[i].getShapeWeights(), grads,
            // ipnts[i].getWeight()*idata[i].getDetJ0());
            MFreeIntegrationPoint3d mpnt = MFreeIntegrationPoint3d.create(deps,
               ipnts[i].getShapeWeights(), grads, ipnts[i].getWeight());
            IntegrationData3d mdat = new IntegrationData3d();
            mdat.setRestInverseJacobian(
               idata[i].getInvJ0(), idata[i].getDetJ0());

            mpnts.add(mpnt);
            // mwgts.set(i, ipnts[i].getWeight()*idata[i].getDetJ0());
            mwgts.set(i, ipnts[i].getWeight());
            mdata.add(mdat);
         }

         // set warping point
         if (region.getWarpingPoint() == null) {
            IntegrationPoint3d wpnt = elem.getWarpingPoint();
            IntegrationData3d wdat = elem.getWarpingData();
            Point3d pos = new Point3d();
            wpnt.computePosition(pos, elem.getNodes());
            // Vector3d [] gradU = wpnt.updateShapeGradient(wdat.getInvJ0());
            Vector3d[] gradU = wpnt.getGNs();
            ArrayList<Vector3d> grads = new ArrayList<Vector3d>();
            for (Vector3d g : gradU) {
               grads.add(g);
            }
            // MFreeIntegrationPoint3d mpnt =
            // MFreeIntegrationPoint3d.create(pos, deps,
            // wpnt.getShapeWeights(), grads, wpnt.getWeight()*wdat.getDetJ0());
            MFreeIntegrationPoint3d mpnt = MFreeIntegrationPoint3d.create(deps,
               wpnt.getShapeWeights(), grads, wpnt.getWeight());
            region.setWarpingPoint(mpnt);
            IntegrationData3d wdata = new IntegrationData3d();
            wdata.setRestInverseJacobian(wdat.getInvJ0(), wdat.getDetJ0());
            region.setWarpingPoint(mpnt, wdata);
         }
         region.setIntegrationPoints(mpnts, mdata);
         region.setIntegrationWeights(mwgts);
         elemList.add(region);
         elemMap.put(elem, region);

      }

      // add everything to model
      model.addNodes(nodeList);
      model.addElements(elemList);
      model.addMesh(surfaceMFree);
      
      // copy properties
      model.setDensity(fem.getDensity());
      model.setParticleDamping(fem.getParticleDamping());
      model.setStiffnessDamping(fem.getStiffnessDamping());

      // copy over all masses
      for (FemNode3d node : fem.getNodes()) {
         nodeMap.get(node).setMass(node.getMass());
      }

      for (FemElement3d elem : fem.getElements()) {
         elemMap.get(elem).setMass(elem.getMass());
      }
      
      model.setMaterial(fem.getMaterial());

      return model;
   }

   public static boolean[][] buildIntersectionChart(List<MFreeNode3d> nodeList) {

      int n = nodeList.size();
      boolean[][] out = new boolean[n][n];

      for (int i = 0; i < n; i++) {
         out[i][i] = true;
         MFreeNode3d node1 = nodeList.get(i);
         for (int j = i + 1; j < n; j++) {
            boolean intersects = node1.intersects(nodeList.get(j));
            out[i][j] = intersects;
            out[j][i] = intersects;
         }
      }
      return out;

   }

   public static boolean[][] buildIntersectionChart(MFreeNode3d[] nodeArray) {
      int n = nodeArray.length;
      boolean out[][] = new boolean[n][n];

      for (int i = 0; i < n; i++) {
         out[i][i] = true;
         MFreeNode3d node1 = nodeArray[i];
         for (int j = i + 1; j < n; j++) {
            boolean intersects = node1.intersects(nodeArray[j]);
            out[i][j] = intersects;
            out[j][i] = intersects;
         }
      }
      return out;

   }

   public static <A extends MFreePoint3d> ArrayList<MFreeElement3d>
      createPartitionedElementsFromPoints(A[] pnts,
         HashMap<A,MFreeElement3d> pntMap) {
      ArrayList<MFreeElement3d> elems = new ArrayList<MFreeElement3d>();

      for (A pnt : pnts) {
         ArrayList<MFreeNode3d> nodes = pnt.getDependentNodes();
         MFreeElement3d elem = findElem(nodes, elems);
         if (elem == null) {
            elem =
               new MFreeElement3d(nodes.toArray(new MFreeNode3d[nodes.size()]));
            elem.setAllTermsActive(true);
            elems.add(elem);
         }
         pntMap.put(pnt, elem);
      }

      return elems;

   }

   private static MFreeElement3d findElem(List<MFreeNode3d> nodes,
      List<MFreeElement3d> elemList) {

      for (MFreeElement3d elem : elemList) {
         MFreeNode3d[] enodes = elem.getNodes();
         if (enodes.length == nodes.size()) {
            if (compareLists(Arrays.asList(enodes), nodes)) {
               return elem;
            }
         }
      }
      return null;

   }

   private static <E> boolean compareLists(List<E> list1, List<E> list2) {
      HashSet<E> set1 = new HashSet<E>(list1);
      HashSet<E> set2 = new HashSet<E>(list2);
      return set1.equals(set2);
   }
   
}
