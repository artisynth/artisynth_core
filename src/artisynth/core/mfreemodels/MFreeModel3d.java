/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemMarker;
import artisynth.core.femmodels.FemModel;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.PointFem3dAttachment;
import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.FemControlPanel;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mfreemodels.MFreeFactory.FemElementTreeNode;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
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
import maspack.matrix.SparseMatrixNd;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.util.DynamicArray;
import maspack.util.InternalErrorException;

public class MFreeModel3d extends FemModel3d  {

   protected AABBTree myNodeTree;
   protected AABBTree myRestNodeTree;               // rest nodes only
   protected FemElementTreeNode myElementNodeTree;  // composition of elements
   protected boolean myModelIsAtRest;                        // model is at rest

   public MFreeModel3d () {
      this(null);
   }

   public MFreeModel3d (String name) {
      super(name);
      myRestNodeTree = null;
      myElementNodeTree = null;
      myModelIsAtRest = true;
   }
   
   @Override
   protected void clearCachedData (ComponentChangeEvent e) {
      super.clearCachedData (e);
      myRestNodeTree = null;
      myElementNodeTree = null;
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

   protected void updateBVHierarchies() {
      if (myAABBTree == null) {
         myAABBTree = new AABBTree();
         Boundable[] elements = new Boundable[numElements()];
         for (int i = 0; i < elements.length; i++) {
            elements[i] = myElements.get(i);
         }
         myAABBTree.build(elements, numElements());
      } else {
         myAABBTree.update();
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

   /**
    * Wrapper boundable class around MFreeNode3d that considers the node
    * at rest, as well as its influence radius. 
    */
   public static class MFreeRestNode3d implements Boundable {
      MFreeNode3d node;
      public MFreeRestNode3d(MFreeNode3d node) {
         this.node = node;
      }
      
      @Override
      public int numPoints () {
         return 9;  // box around sphere of influence
      }
      
      @Override
      public Point3d getPoint (int idx) {
         Point3d pos = new Point3d();
         pos.set (node.getRestPosition ());
         double r = node.getInfluenceRadius ();
         switch(idx) {
            case 0:
               break;
            case 1: {
               pos.x -= r;
               pos.y -= r;
               pos.z -= r;
               break;
            }
            case 2: {
               pos.x -= r;
               pos.y += r;
               pos.z -= r;
               break;
            }
            case 3: {
               pos.x += r;
               pos.y += r;
               pos.z -= r;
               break;
            }
            case 4: {
               pos.x += r;
               pos.y -= r;
               pos.z -= r;
               break;
            } 
            case 5: {
               pos.x -= r;
               pos.y -= r;
               pos.z += r;
               break;
            }
            case 6: {
               pos.x -= r;
               pos.y += r;
               pos.z += r;
               break;
            } 
            case 7: {
               pos.x += r;
               pos.y += r;
               pos.z += r;
               break;
            } 
            case 8: {
               pos.x += r;
               pos.y -= r;
               pos.z += r;
               break;
            } 
         }
         return pos;
      }
      @Override
      public void computeCentroid (Vector3d centroid) {
         centroid.set (node.getRestPosition ());
      }
      
      @Override
      public void updateBounds (Vector3d min, Vector3d max) {
         Point3d pos = new Point3d(node.getRestPosition ());
         double r = node.getInfluenceRadius ();
         pos.x -= r;
         pos.y -= r;
         pos.z -= r;
         pos.updateBounds (min, max);
         pos.x += 2*r;
         pos.y += 2*r;
         pos.z += 2*r;
         pos.updateBounds (min, max);
      }
      
      @Override
      public double computeCovariance (Matrix3d C) {
         int np = numPoints();
         for (int i=0; i<np; ++i) {
            Point3d pos = getPoint (i);
            C.addOuterProduct (pos, pos);
         }
         return np;
      }
      
      public MFreeNode3d getNode() {
         return node;
      }
   }
   
   /**
    * BV Tree that bounds the nodes at rest, and their spherical influence regions.  The
    * Boundables are "rest nodes".
    * 
    * @see MFreeRestNode3d
    * @return BV tree for nodes at rest
    */
   public BVTree getRestNodeBVTree() {
      if (myRestNodeTree == null) {
         myRestNodeTree = new AABBTree();
         Boundable[] nodes = new Boundable[numNodes()];
         for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new MFreeRestNode3d((MFreeNode3d)myNodes.get(i));
         }
         myRestNodeTree.build(nodes, numNodes());
      }
      return myRestNodeTree;
   }
   
   /**
    * Gets a BV tree that surrounds nodes (both false and true positions)
    * 
    * Note: does not surround influence regions, since these may be quite deformed 
    * if not at rest, only bounds the nodes themselves
    */
   public BVTree getNodeBVTree() {
      if (!myBVTreeValid || myNodeTree == null) {
         updateBVHierarchies();
      }
      return myNodeTree;
   }

   /**
    * The element bv tree bounds integration points contained by the element, which may be
    * slightly smaller than the true element bounds
    * @return bvtree for elements
    */
   public BVTree getElementBVTree() {
      if (!myBVTreeValid || myAABBTree == null) {
         updateBVHierarchies();
      }
      return myAABBTree;
   }
   
   /**
    * @see #getElementBVTree()
    */
   @Override
   public BVTree getBVTree () {
      return getElementBVTree ();
   }


   /**
    * Adds a marker to this FemModel. The element to which it belongs is
    * determined automatically. If the marker's current position does not lie
    * within the model, then null is returned.
    * 
    * @param pos position to place a marker in the model
    * 
    */
   public FemMarker addMarker(Point3d pos) {
      FemMarker mkr = new FemMarker();
      Point3d coord = new Point3d();
      VectorNd N = new VectorNd();

      coord.set(pos);
      FemNode3d[] nodes = findNaturalCoordinates(pos, coord, N);
      
      if (nodes == null) {
         return null;
      }
      
      mkr.setPosition(pos);
      mkr.setFromNodes(nodes, N.getBuffer ());
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
   
   public boolean areCoplanarAtRest(MFreeNode3d[] nodes) {
      if (nodes.length <= 3) {
         return true;
      }

      // assumes points do not overlap
      Vector3d v1 = new Vector3d(nodes[0].getRestPosition ());
      v1.sub(nodes[1].getRestPosition ());
      double v1n = v1.norm();
      double eps = v1n*1e-5;  // epsilon for plane projection
      v1.scale(1.0/v1n); // normalize

      // find non-colinear point
      Vector3d v2 = new Vector3d();
      Vector3d normal = new Vector3d();
      int jl = -1;
      for (int j=2; j<nodes.length; ++j) {
         v2.sub(nodes[0].getRestPosition (), nodes[j].getRestPosition ());
         v2.normalize();
         normal.cross(v1, v2);
         double v3n = normal.norm();
         if (v3n  > 1e-10) {
            normal.scale(1.0/v3n);
            jl = j;
            break;
         }
      }

      // colinear
      if (jl < 0) {
         return true;
      }

      double d = normal.dot(nodes[0].getRestPosition ());
      for (int k=jl+1; k<nodes.length; ++k) {
         double dd = normal.dot(nodes[k].getRestPosition ())-d;
         if (Math.abs(dd) > eps) {
            return false;
         }
      }

      return true;
   }
   
   /**
    * Computes natural coordinates at rest, along with dependent nodes and the shape
    * function evaluated at the given point
    * @param rpnt position at rest
    * @param coords coordinates at rest
    * @param N evaluated shape functions
    * @return dependent nodes
    */
   public MFreeNode3d[] findNaturalRestCoordinates(Point3d rpnt, Point3d coords, VectorNd N) {
      
      coords.set (rpnt);
      
      ArrayList<BVNode> bvnodes = new ArrayList<BVNode>();
      BVTree restTree = getRestNodeBVTree ();
      restTree.intersectPoint (bvnodes, rpnt);
      
      DynamicArray<MFreeNode3d> nodes = new DynamicArray<> (MFreeNode3d.class);
      for (BVNode bvnode : bvnodes) {
         for (Boundable b : bvnode.getElements ()) {
            MFreeNode3d mnode = ((MFreeRestNode3d)b).getNode ();
            if (mnode.isInRestDomain (rpnt)) {
               nodes.add (mnode);
            }
         }
      }
      
      if (nodes.size () == 0) {
         return null;
      }
      
      // compute shape function
      int order = MLSShapeFunction.LINEAR_ORDER;
      if (nodes.size () < 4 || areCoplanarAtRest (nodes.getArray ())) {
         order = MLSShapeFunction.CONSTANT_ORDER;
      }
      MLSShapeFunction mls = new MLSShapeFunction (nodes.getArray (), order);
      
      mls.setCoordinate (rpnt);
      N.setSize (nodes.size ());
      mls.eval (N);
      
      return nodes.getArray ();
   }

   /**
    * Finds the containing element and node coordinates
    * @param pnt 3D point in world coordinates to find natural coordinates of
    * @param coords natural coordinates
    * @param N shape function values
    * @return the dependent nodes
    */
   public MFreeNode3d[] findNaturalCoordinates(Point3d pnt, Point3d coords, VectorNd N) {

      // if at rest, use rest coordinates
      if (myModelIsAtRest) {
         return findNaturalRestCoordinates (pnt, coords, N);
      }
      
      // otherwise, solve for rest coordinates, start with a guess at nearest ipnt or node
      BVFeatureQuery query = new BVFeatureQuery();
      NearestIPointCalculator dcalc = new NearestIPointCalculator(pnt);
      MFreeElement3d elem = (MFreeElement3d)query.nearestObject(getElementBVTree(), dcalc);
      MFreeIntegrationPoint3d ipnt = dcalc.nearestIPoint();
      
      // check if any nodes are closer
      MFreePoint3d nearest = ipnt;
      double d = ipnt.getPosition ().distance (pnt);
      for (FemNode3d node : ipnt.getDependentNodes ()) {
         double nd = node.distance (pnt);
         if (nd < d) {
            nearest = (MFreeNode3d)node;
            d = nd;
         }
      }

      // try to compute coords
      // XXX may not be within element, but use as approximation
      coords.set(nearest.getRestPosition ());
      N.set (nearest.getNodeCoordinates ());
      elem.getNaturalCoordinates(coords, pnt, 1000, N);
      
      return (MFreeNode3d[])nearest.getDependentNodes ();
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
      // XXX may not truly be nearest
      BVFeatureQuery query = new BVFeatureQuery();
      NearestIPointCalculator dcalc = new NearestIPointCalculator(pnt);
      query.nearestObject(getElementBVTree(), dcalc);

      MFreeElement3d elem = (MFreeElement3d)dcalc.nearestObject();

      // nearest ipnt or node
      MFreeIntegrationPoint3d ipnt = dcalc.nearestIPoint();

      // check if any nodes are closer
      MFreePoint3d np = ipnt;
      double d = ipnt.getPosition ().distance (pnt);
      for (FemNode3d node : ipnt.getDependentNodes ()) {
         double nd = node.distance (pnt);
         if (nd < d) {
            np = (MFreeNode3d)node;
            d = nd;
         }
      }
      nearest.set(np.getPosition());
      N.set(np.getNodeCoordinates ());

      // try to compute natural coordinates within element
      elem.getNaturalCoordinates (nearest, pnt, N);
      
      // recompute nearest
      FemNode3d[] nodes = elem.getNodes ();
      nearest.setZero ();
      for (int i=0; i<nodes.length; ++i) {
         nearest.scaledAdd (N.get (i), nodes[i].getPosition ());
      }

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
      BVTree bvtree = getNodeBVTree();
      ArrayList<BVNode> nodes = new ArrayList<BVNode>();
      bvtree.intersectSphere(nodes, pnt, maxDist);
      
      FemNode3d nearest = null;
      double dist = 1 + 2 * maxDist;
      for (BVNode n : nodes) {
         Boundable[] elements = n.getElements();
         for (int i = 0; i < elements.length; i++) {
            MFreeNode3d node = (MFreeNode3d)elements[i];
            double d = node.getTruePosition().distance(pnt);
            if (d < dist && d <= maxDist) {
               dist = d;
               nearest = node;
            }
         }
      }
      return nearest;
   }

   public void updateSlavePos () {
      super.updateSlavePos ();

      myModelIsAtRest = true;
      // nodes
      for (FemNode3d node : myNodes) {
         ((MFreeNode3d)node).updateSlavePos();
         if (node.getPosition ().distanceSquared (node.getRestPosition ()) != 0) {
            myModelIsAtRest = false;
         }
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

      public Point3d nearestPoint() {
         return myIpnt.getPosition ();
      }
      
   }
   
   public double integrate(Function3x1 fun) {
      double out = 0;
      if (!myStiffnessesValidP) {
         updateJacobians();
      }

      for (FemElement3d elem : myElements) {
         IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();

         for (int i = 0; i < elem.numIntegrationPoints(); i++) {
            IntegrationPoint3d ipnt = ipnts[i];
            VectorNd shapeFunc = ipnt.getShapeWeights();

            double detJ = ipnt.computeJacobianDeterminant (elem.getNodes()); 
            for (int j = 0; j < elem.numNodes(); j++) {
               // if (elem.isTermActive(j, j)) {
               double f =
                  fun.eval(((MFreePoint3d)ipnt).getPosition()) * ipnt.getWeight() * shapeFunc.get(j);
               f = f * detJ; // ipnt.getDetF() * idat.getDetJ0();
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
             VectorNd shapeCoords = ipnt.getShapeWeights();

            double detJ = ipnt.computeJacobianDeterminant (e.getNodes());
            for (int i = 0; i < e.numNodes(); i++) {
               for (int j = i; j < e.numNodes(); j++) {

                  int bi = e.getNodes()[i].getNumber();
                  int bj = e.getNodes()[j].getNumber();

                  double m =
                     myDensity * shapeCoords.get(i)
                     * shapeCoords.get(j) * ipnt.getWeight() * detJ;

                  M.set(bi, bj, M.get(bi, bj) + m);
                  if (i != j) {
                     M.set(bj, bi, M.get(bj, bi) + m);
                  }
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
         
         VectorNd ones = new VectorNd(massMatrix.rowSize ());
         for (int i=0; i<ones.size (); ++i) {
            ones.set (i, 1);
         }
         
         massMatrix.mul (massMatrixDiag, ones);
      }

      double mTrace = massMatrixDiag.sum();

      for (FemNode3d node : myNodes) {
         int i = node.getNumber();
         double m = totalMass / mTrace * massMatrixDiag.get(i);
         node.setExplicitMass(m);
      }

   }

   public void updateJacobians() {
      for (FemElement3d region : myElements) {
         computeJacobianAndGradient(region);
      }
   }

   private void computeJacobianAndGradient(FemElement3d region) {

      IntegrationPoint3d[] ipnts = region.getIntegrationPoints();
      //IntegrationData3d[] idata = region.getIntegrationData();
      region.setInverted(false);

      for (int i = 0; i < ipnts.length; i++) {
         IntegrationPoint3d ipnt = ipnts[i];
         //IntegrationData3d idat = idata[i];
         double detJ = ipnt.computeJacobianDeterminant (region.getNodes());
         //double detJ = ipnt.computeInverseJacobian();

         checkElementCondition (region, detJ, /*recordInversion=*/true);
      }
   }
   
   public PointAttachment createPointAttachment(Point pnt, double reduceTol) {
      
      if (pnt.isAttached()) {
         throw new IllegalArgumentException ("point is already attached");
      }
      if (ComponentUtils.isAncestorOf (this, pnt)) {
         throw new IllegalArgumentException (
            "FemModel is an ancestor of the point");
      }
      
      Point3d coord = new Point3d();
      VectorNd N = new VectorNd();
      coord.set(pnt.getPosition ());
      FemNode3d[] nodes =  findNaturalCoordinates(pnt.getPosition (), coord, N);
      
      PointFem3dAttachment ax = new PointFem3dAttachment (pnt);
      ax.setFromNodes (Arrays.asList (nodes), N);
      return ax;
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
