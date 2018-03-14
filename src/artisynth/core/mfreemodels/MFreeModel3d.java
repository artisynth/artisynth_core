/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemMarker;
import artisynth.core.femmodels.FemModel;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.FrameFem3dAttachment;
import artisynth.core.femmodels.IntegrationData3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.PointFem3dAttachment;
import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.FemControlPanel;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.ConnectableBody;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.HasAuxState;
import artisynth.core.mechmodels.HasSurfaceMesh;
import artisynth.core.mechmodels.MechSystemModel;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachable;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.PointParticleAttachment;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScalableUnits;
import maspack.function.ConstantFuntion3x1;
import maspack.function.Function3x1;
import maspack.geometry.AABBTree;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.BVFeatureQuery.ObjectDistanceCalculator;
import maspack.geometry.BVNode;
import maspack.geometry.BVTree;
import maspack.geometry.Boundable;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseMatrixNd;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.util.InternalErrorException;

public class MFreeModel3d extends FemModel3d  {

   protected AABBTree myElementTree;
   protected AABBTree myNodeTree;
   protected boolean myBVTreeValid;
   protected AABBTree myRestNodeTree; // rest nodes only

   public MFreeModel3d () {
      this(null);
   }

   public MFreeModel3d (String name) {
      super(name);
      myRestNodeTree = null;      
   }

   /**
    * Creates a surface mesh component in the first location of the mesh list
    */
   protected MFreeMeshComp createSurfaceMeshComponent() {
      MFreeMeshComp surf = new MFreeMeshComp(this);
      surf.setName(DEFAULT_SURFACEMESH_NAME);
      surf.setSurfaceRendering(getSurfaceRendering());
      surf.markSurfaceMesh(true);
      surf.setCollidable (Collidability.EXTERNAL);
      return surf;
   }     
   
   private MFreeMeshComp doGetSurfaceMeshComp() {
      if (myMeshList.size()==0 || !myMeshList.get(0).isSurfaceMesh()) {
         throw new InternalErrorException (
            "surface mesh component missing from mesh list");
      }
      return (MFreeMeshComp)myMeshList.get(0);
   }
   
   public MFreeMeshComp setSurfaceMesh(PolygonalMesh mesh) {
      // Create embedded mesh
      MFreeMeshComp surfMesh = doGetSurfaceMeshComp();
      MFreeMeshComp.createEmbedded(surfMesh, mesh);
      setSurfaceMeshComp(surfMesh);
      return surfMesh;
   }

   private void updateBVHierarchies() {
      if (myElementTree == null) {
         myElementTree = new AABBTree();
         Boundable[] elements = new Boundable[numElements()];
         for (int i = 0; i < elements.length; i++) {
            elements[i] = myElements.get(i);
         }
         myElementTree.build(elements, numElements());
      } else {
         myElementTree.update();
      }
      if (myNodeTree == null) {
         myNodeTree = new AABBTree();
         Boundable[] nodes = new Boundable[numNodes()];
         for (int i = 0; i < nodes.length; i++) {
            nodes[i] = (MFreeNode3d)myNodes.get(i);
         }
         myNodeTree.build(nodes, numNodes());
      } else {
         myNodeTree.update();
      }
      myBVTreeValid = true;
   }

   public BVTree getNodeBVTree() {
      if (!myBVTreeValid || myNodeTree == null) {
         updateBVHierarchies();
      }
      return myNodeTree;
   }

   public BVTree getElementBVTree() {
      if (myElementTree == null || myElementTree == null) {
         updateBVHierarchies();
      }
      return myElementTree;
   }


   /**
    * Adds a marker to this FemModel. The element to which it belongs is
    * determined automatically. If the marker's current position does not lie
    * within the model and {@code project == true}, it will be projected onto 
    * the model's surface.
    * 
    * @param pos
    * position to place a marker in the model
    * 
    */
   public FemMarker addMarker(Point3d pos) {
      FemMarker mkr = new FemMarker();
      Point3d coord = new Point3d();
      VectorNd N = new VectorNd();

      FemNode3d[] nodes =  findNaturalCoordinates(pos, coord, N);

      mkr.setPosition(pos);
      double[] wgts = new double[N.size()];
      for (int i=0; i<N.size(); ++i) {
         wgts[i] = N.get(i);
      }
      mkr.setFromNodes(nodes, wgts);
      addMarker(mkr);
      return mkr;
   }

   public MFreeMeshComp addMesh(MeshBase mesh) {
      String meshName =
         ModelComponentBase.makeValidName(mesh.getName(), null, myMeshList);
      return addMesh(meshName, mesh);
   }

   public MFreeMeshComp addMesh(String name, MeshBase mesh) {
      mesh.setFixed(false);
      mesh.setColorsFixed(false);
      MFreeMeshComp surf = MFreeMeshComp.createEmbedded(this, mesh);
      surf.setName(name);
      addMesh(surf);
      return surf;
   }

   private void addMesh(MFreeMeshComp mesh) {
      mesh.setCollidable (Collidability.INTERNAL);
      myMeshList.add(mesh);
   }

   /**
    * Finds the containing element and node coordinates
    * @param pnt 3D point in world coordinates to find natural coordinates of
    * @param coords natural coordinates
    * @param N shape function values
    * @return the containing element if it exists
    */
   public FemNode3d[] findNaturalCoordinates(Point3d pnt, Point3d coords, VectorNd N) {

      BVFeatureQuery query = new BVFeatureQuery();

      NearestIPointCalculator dcalc 
      = new NearestIPointCalculator(pnt);
      query.nearestObject(getElementBVTree(), dcalc);

      FemElement3d elem = dcalc.nearestObject();
      MFreeIntegrationPoint3d ipnt = dcalc.nearestIPoint();

      // try to compute coords
      coords.set(ipnt.getRestPosition());
      int n = ((MFreeElement3d)elem).getNaturalCoordinates(coords, pnt, 1000, N);
      return elem.getNodes();
   }
   
   /**
    * Finds the nearest element and node coordinates
    * @param nearest nearest point
    * @return the nearest element
    */
   public FemElement3d findNearestElement(Point3d nearest, Point3d pnt) {
      VectorNd N = new VectorNd();
      return findNearestElement(nearest, pnt, N);
   }

   /**
    * Finds the nearest element and node coordinates
    * @param nearest nearest point
    * @param N shape function evaluation at point
    * @return the nearest element
    */
   public FemElement3d findNearestElement(Point3d nearest, Point3d pnt, VectorNd N) {

      BVFeatureQuery query = new BVFeatureQuery();

      NearestIPointCalculator dcalc 
      = new NearestIPointCalculator(pnt);
      query.nearestObject(getElementBVTree(), dcalc);

      FemElement3d elem = dcalc.nearestObject();
      MFreeIntegrationPoint3d ipnt = dcalc.nearestIPoint();

      nearest.set(ipnt.getPosition());
      N.set(ipnt.getShapeWeights());

      return elem;

   }

   /**
    * Finds the nearest node to a specified point that is within
    * a specified maximum distance. If no node is within the
    * specified maximum distance, <code>null</code> is returned.
    * 
    * @param pnt Point for which the nearest node should be located
    * @param maxDist Maximum distance that the node must be from the
    * point. If <code>maxDist</code> &lt; 0, then <code>null</code>
    * will be returned.
    * @return Nearest point within the prescribed distance, or <code>null</code>
    * if there is no such point
    */
   public FemNode3d findNearestNode(Point3d pnt, double maxDist) {
      if (maxDist < 0) {
         return null;
      }
      BVTree bvtree = getElementBVTree();
      ArrayList<BVNode> nodes = new ArrayList<BVNode>();
      bvtree.intersectSphere(nodes, pnt, maxDist);
      FemNode3d nearest = null;
      double dist = 1 + 2 * maxDist;
      for (BVNode n : nodes) {
         Boundable[] elements = n.getElements();
         for (int i = 0; i < elements.length; i++) {
            FemElement3d e = (FemElement3d)elements[i];
            for (int k = 0; k < e.numNodes(); k++) {
               double d = e.getNodes()[k].getPosition().distance(pnt);
               if (d < dist && d <= maxDist) {
                  dist = d;
                  nearest = e.getNodes()[k];
               }
            }
         }
      }
      return nearest;
   }

   public void updateSlavePos () {
      super.updateSlavePos ();

      // nodes
      for (FemNode3d node : myNodes) {
         ((MFreeNode3d)node).updateSlavePos();
      }

      // integration points
      for (FemElement3d elem : myElements) {
         for (IntegrationPoint3d mfip : elem.getIntegrationPoints()) {
            ((MFreeIntegrationPoint3d)mfip).updateSlavePos();
         }
         MFreePoint3d warp = (MFreePoint3d)elem.getWarpingPoint();
         if (warp != null) {
            warp.updateSlavePos();
         }
      }

      // meshes
      myMeshList.updateSlavePos();

      myBVTreeValid = false;

      if (myFrameConstraint != null && !myFrameRelativeP) {
         myFrameConstraint.updateFramePose(/*frameRelative=*/false);
      }
   }     
  
   public static void addControls(
      ControlPanel controlPanel, FemModel femModel, ModelComponent topModel) {
      FemControlPanel.addFemControls(controlPanel, femModel, topModel);
      controlPanel.addWidget(femModel, "surfaceRendering");
      controlPanel.addWidget(femModel, "elementWidgetSize", 0, 1.0);
   }
  
   private static class NearestIPointCalculator implements ObjectDistanceCalculator {

      Point3d myPnt;
      MFreeIntegrationPoint3d myIpnt;
      FemElement3d myElem;
      double myDist;

      public NearestIPointCalculator(Point3d pnt) {
         myPnt = new Point3d(pnt);
         reset();
      }

      @Override
      public void reset() {
         myIpnt = null;
         myElem = null;
         myDist = Double.POSITIVE_INFINITY;
      }

      @Override
      public double nearestDistance(BVNode node) {
         return node.distanceToPoint(myPnt);
      }

      @Override
      public double nearestDistance(Boundable e) {
         FemElement3d elem = (FemElement3d)e;
         double dmin = Double.MAX_VALUE;
         for (IntegrationPoint3d cb : elem.getIntegrationPoints()) {
            MFreeIntegrationPoint3d ipt = (MFreeIntegrationPoint3d)cb;
            double d = ipt.getPosition().distance(myPnt);
            if (d < dmin) {
               dmin = d;
               myDist = dmin;
               myElem = elem;
               myIpnt = ipt;
            }
         }
         return dmin;
      }

      @Override
      public FemElement3d nearestObject() {
         return myElem;
      }

      public MFreeIntegrationPoint3d nearestIPoint() {
         return myIpnt;
      }

      @Override
      public double nearestDistance() {
         return myDist;
      }

   }

   private static class RestNode implements Boundable {

      FemNode3d node;
      Point3d[] pnts;

      public RestNode(FemNode3d node, double r) {
         this.node = node;
         pnts = new Point3d[2];
         Point3d pos = node.getRestPosition();
         pnts[0] = new Point3d(pos.x+r, pos.y+r, pos.z+r);
         pnts[1] = new Point3d(pos.x-r, pos.y-r, pos.z-r);
      }

      public FemNode3d getNode() {
         return node;
      }

      @Override
      public int numPoints() {
         return 2;
      }

      @Override
      public Point3d getPoint(int idx) {
         return pnts[idx];
      }

      @Override
      public void computeCentroid(Vector3d centroid) {
         centroid.set(node.getRestPosition());
      }

      @Override
      public void updateBounds(Vector3d min, Vector3d max) {
         pnts[0].updateBounds(min, max);
         pnts[1].updateBounds(min, max);
      }

      @Override
      public double computeCovariance(Matrix3d C) {
         return -1;
      }
   }

   private static AABBTree buildRestNodeTree(Collection<FemNode3d> nodes) {
      AABBTree nodeTree = new AABBTree();
      RestNode[] rnodes = new RestNode[nodes.size()];
      int idx = 0;
      for (FemNode3d node : nodes) {
         rnodes[idx] = new RestNode(node, ((MFreeNode3d)node).getInfluenceRadius());
         ++idx;
      }
      nodeTree.build(rnodes, rnodes.length);
      return nodeTree;
   }

   /**
    * Finds nodes containing the given point at rest
    * @param pnt point to find nodes for
    * @param out output list of nodes influencing region
    * @return number of nodes found
    */
   public int findDependentNodesAtRest(Point3d pnt, List<FemNode3d> out) {

      AABBTree nodeTree = myRestNodeTree;
      if (nodeTree == null) {
         nodeTree = buildRestNodeTree(myNodes);
         myRestNodeTree = nodeTree;
      }

      ArrayList<BVNode> bvNodes = new ArrayList<BVNode>(16);
      nodeTree.intersectPoint(bvNodes, pnt);

      if (bvNodes.size() == 0) {
         return 0;
      }

      int count = 0;
      for (BVNode n : bvNodes) {
         Boundable[] elements = n.getElements();
         for (int i = 0; i < elements.length; i++) {
            RestNode rnode = (RestNode)elements[i];
            FemNode3d node = rnode.getNode();
            if (((MFreeNode3d)node).isInDomain(pnt, 0)) {
               out.add(node);
               ++count;
            }
         }
      }
      return count;
   }

   public double integrate(Function3x1 fun) {
      double out = 0;
      if (!myStiffnessesValidP) {
         updateJacobians();
      }

      for (FemElement3d elem : myElements) {
         IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
         IntegrationData3d idata[] = elem.getIntegrationData();

         for (int i = 0; i < elem.numIntegrationPoints(); i++) {
            IntegrationPoint3d ipnt = ipnts[i];
            IntegrationData3d idat = idata[i];
            VectorNd shapeFunc = ipnt.getShapeWeights();

            for (int j = 0; j < elem.numNodes(); j++) {
               // if (elem.isTermActive(j, j)) {
               double f =
                  fun.eval(((MFreePoint3d)ipnt).getPosition()) * ipnt.getWeight() * shapeFunc.get(j);
               f = f * ipnt.getDetF() * idat.getDetJ0();
               out += f;
               // }
            }
         }
      }

      return out;
   }

   public double integrateVolume() {
      return integrate(new ConstantFuntion3x1(1));
   }

   public double integrateMass() {
      return integrate(new ConstantFuntion3x1(myDensity));
   }

   public SparseMatrixNd computeConsistentMassMatrix() {

      int nNodes = myNodes.size();
      SparseMatrixNd M = new SparseMatrixNd(nNodes, nNodes);

      updateJacobians();

      for (FemElement3d e : myElements) {
         for (int k = 0; k < e.numIntegrationPoints(); k++) {

            IntegrationPoint3d ipnt = e.getIntegrationPoints()[k];
            IntegrationData3d idat = e.getIntegrationData()[k];

            VectorNd shapeCoords = ipnt.getShapeWeights();

            for (int i = 0; i < e.numNodes(); i++) {
               for (int j = i; j < e.numNodes(); j++) {
                  // if (e.isTermActive(i, j)) {
                  int bi = e.getNodes()[i].getNumber();
                  int bj = e.getNodes()[j].getNumber();

                  double m =
                     myDensity * shapeCoords.get(i)
                     * shapeCoords.get(j) * ipnt.getWeight() * ipnt.getDetF()
                     * idat.getDetJ0();

                  M.set(bi, bj, M.get(bi, bj) + m);
                  if (i != j) {
                     M.set(bj, bi, M.get(bj, bi) + m);
                  }

                  // }
               }
            }
         }
      }

      return M;

   }

   public void updateNodeMasses(double totalMass, VectorNd massMatrixDiag) {

      if (totalMass <= 0) {
         totalMass = integrateMass();
      }

      if (massMatrixDiag == null) {
         SparseMatrixNd massMatrix = computeConsistentMassMatrix();
         massMatrixDiag = new VectorNd(massMatrix.rowSize());
         for (int i = 0; i < massMatrix.rowSize(); i++) {
            double rowSum = 0;
            for (int j = 0; j < massMatrix.colSize(); j++) {
               rowSum += massMatrix.get(i, j);
            }
            // rowSum += massMatrix.get(i, i);
            massMatrixDiag.set(i, rowSum);
         }
      }

      double mTrace = massMatrixDiag.sum();

      for (FemNode3d node : myNodes) {
         int i = node.getNumber();
         double m = totalMass / mTrace * massMatrixDiag.get(i);
         node.setMass(m);
         node.setMassExplicit(true);
      }

   }

   public void updateJacobians() {
      for (FemElement3d region : myElements) {
         computeJacobianAndGradient(region);
      }
   }

   private void computeJacobianAndGradient(FemElement3d region) {

      IntegrationPoint3d[] ipnts = region.getIntegrationPoints();
      IntegrationData3d[] idata = region.getIntegrationData();
      region.setInverted(false);

      for (int i = 0; i < ipnts.length; i++) {
         IntegrationPoint3d ipnt = ipnts[i];
         IntegrationData3d idat = idata[i];
         ipnt.computeJacobianAndGradient(region.getNodes(), idat.getInvJ0());
         double detJ = ipnt.computeInverseJacobian();

         if (detJ < myMinDetJ) {
            myMinDetJ = detJ;
            myMinDetJElement = region;
         }
         if (detJ <= 0) {
            region.setInverted(true);
            myNumInverted++;
         }
      }
   }
   
   public PointAttachment createPointAttachment (Point pnt) {
      return createPointAttachment (pnt, /*reduceTol=*/1e-8);
   }
   
   @Override
      public void initialize(double t) {
         super.initialize(t);
         
         updateSlavePos();
         updateSlaveVel();
      }
   
}
