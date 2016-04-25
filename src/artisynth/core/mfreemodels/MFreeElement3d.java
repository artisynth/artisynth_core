/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;

import artisynth.core.femmodels.AuxiliaryMaterial;
import artisynth.core.femmodels.FemElement;
import artisynth.core.femmodels.FemNodeNeighbor;
import artisynth.core.femmodels.IntegrationData3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.LinearMaterial;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Boundable;
import maspack.geometry.MeshRendererBase.MeshRenderInfo;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.PolygonalMeshRenderer;
import maspack.geometry.Vertex3d;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix6d;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixBlockBase;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;
import maspack.util.InternalErrorException;

public class MFreeElement3d extends FemElement implements Boundable {

   protected MFreeNode3d[] myNodes;
   protected FemNodeNeighbor[][] myKBlocks;
   boolean[][] active;
   
   protected ArrayList<IntegrationData3d> myIntegrationData;
   protected ArrayList<MFreeIntegrationPoint3d> myIntegrationPoints;
   protected ArrayList<int[]> myIntegrationNodeIdxs;
   protected VectorNd myIntegrationWeights;
   protected boolean myIntegrationDataValid = false;
   
//   protected double myLagrangePressure;
   protected MatrixBlock[] myIncompressConstraints = null;
   
   protected PolygonalMesh myBoundaryMesh = null;
   boolean renderMeshValid = false;

   protected MFreeStiffnessWarper3d myWarper = null;
   protected MFreeIntegrationPoint3d myWarpingPoint = null;
   protected IntegrationData3d myWarpingData = null;

   private static double DEFAULT_ELEMENT_WIDGET_SIZE = 0.0;
   protected double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
   protected PropertyMode myElementWidgetSizeMode = PropertyMode.Inherited;

   // Auxiliary Materials are mainly used for implementing muscle fibres
   protected ArrayList<AuxiliaryMaterial> myAuxMaterials = null;

   public static PropertyList myProps =
      new PropertyList(MFreeElement3d.class, FemElement.class);

   static {
      myProps.addInheritable(
         "elementWidgetSize:Inherited",
         "size of rendered widget in each element's center",
         DEFAULT_ELEMENT_WIDGET_SIZE, "[0,1]");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setElementWidgetSize(double size) {
      myElementWidgetSize = size;
      myElementWidgetSizeMode =
         PropertyUtils.propagateValue(
            this, "elementWidgetSize",
            myElementWidgetSize, myElementWidgetSizeMode);
   }

   public double getElementWidgetSize() {
      return myElementWidgetSize;
   }

   public void setElementWidgetSizeMode(PropertyMode mode) {
      myElementWidgetSizeMode =
         PropertyUtils.setModeAndUpdate(
            this, "elementWidgetSize", myElementWidgetSizeMode, mode);
   }

   public PropertyMode getElementWidgetSizeMode() {
      return myElementWidgetSizeMode;
   }

   public MFreeElement3d (MFreeNode3d[] nodes) {

      myNodes = nodes.clone();
      active = new boolean[myNodes.length][myNodes.length];
      setAllTermsActive(true);   // on by default
      myIntegrationData = new ArrayList<IntegrationData3d>();
      myIntegrationPoints = new ArrayList<MFreeIntegrationPoint3d>();
      myIntegrationNodeIdxs = new ArrayList<int[]>();
      myIntegrationWeights = new VectorNd(0);

   }
   
   public boolean isInside(Point3d pnt) {
      if (myBoundaryMesh != null) {
         BVFeatureQuery query = new BVFeatureQuery();
         return query.isInsideOrientedMesh (myBoundaryMesh, pnt, 1e-10);
      }
      for (MFreeNode3d node : myNodes) {
         if (!node.isInDomain(pnt, 0)) {
            return false;
         }
      }
      return true;
   }

   public boolean isInvertedAtRest() {
      ArrayList<MFreeIntegrationPoint3d> ipnts = getIntegrationPoints();
      ArrayList<IntegrationData3d> idata = getIntegrationData();
      
      for (int i=0; i<ipnts.size(); i++) {
         MFreeIntegrationPoint3d ipnt = ipnts.get(i);
         IntegrationData3d idat = idata.get(i);
         
         ipnt.computeJacobianAndGradient(idat.getInvJ0());
         if (ipnt.getDetF()< 0) {
            return true;
         }
      }
      
      return false;
      
   }
   
   public void setAllTermsActive(boolean act) {
      for (int i = 0; i < myNodes.length; i++) {
         for (int j = 0; j < myNodes.length; j++) {
            active[i][j] = act;
         }
      }
   }

   public void setColTermsActive(int j, boolean act) {
      for (int i = 0; i < myNodes.length; i++) {
         active[i][j] = act;
      }
   }

   public void setRowTermsActive(int i, boolean act) {
      for (int j = 0; j < myNodes.length; j++) {
         active[i][j] = act;
      }
   }

   public void setTermActive(int i, int j, boolean act) {
      this.active[i][j] = act;
   }
   
   public void setDiagonalTermsActive(boolean act) {
      for (int i=0; i<numNodes(); i++) {
         this.active[i][i] = act;
      }
   }

   public boolean isTermActive(int i, int j) {
      return this.active[i][j];
   }
   
   public boolean isTermActive(MFreeNode3d rowNode, MFreeNode3d colNode) {
      int idxRow = getNodeIdx(rowNode);
      int idxCol = getNodeIdx(colNode);
      return isTermActive(idxRow, idxCol);
   }
   
   public int getNodeIdx(MFreeNode3d node) {
      for (int i = 0; i < myNodes.length; i++) {
         if (myNodes[i] == node) {
            return i;
         }
      }
      return -1;
   }

   public void setColTermsActive(MFreeNode3d node, boolean act) {
      int idxj = getNodeIdx(node);
      if (idxj >= 0) {
         setColTermsActive(idxj, act);
      }
   }

   public void setRowTermsActive(MFreeNode3d node, boolean act) {
      int idxi = getNodeIdx(node);
      if (idxi >= 0) {
         setRowTermsActive(idxi, act);
      }
   }

   public void setTermActive(MFreeNode3d nodei, MFreeNode3d nodej, boolean act) {
      int idxi = getNodeIdx(nodei);
      int idxj = getNodeIdx(nodej);
      if (idxi >= 0 && idxj >= 0) {
         setTermActive(idxi, idxj, act);
      }
   }

   public MFreeNode3d[] getNodes() {
      return myNodes;
   }
   
   public MFreeNode3d getNode(int idx) {
      return myNodes[idx];
   }

   public int numNodes() {
      return myNodes.length;
   }

   public int numIntegrationPoints() {
      return myIntegrationPoints.size();
   }

   public ArrayList<MFreeIntegrationPoint3d> getIntegrationPoints() {
      return myIntegrationPoints;
   }
   
   public MFreeIntegrationPoint3d getIntegrationPoint(int idx) {
      return myIntegrationPoints.get(idx);
   }
   
   public int getIntegrationPointIndex(IntegrationPoint3d pnt) {
      int idx = myIntegrationPoints.indexOf(pnt);
      return idx;
   }
   
   public ArrayList<IntegrationData3d> getIntegrationData() {
      return myIntegrationData;
   }
   
   public IntegrationData3d getIntegrationData(int idx) {
      return myIntegrationData.get(idx);
   }
   
   public ArrayList<int[]> getIntegrationIndices() {
      return myIntegrationNodeIdxs;
   }
   
   public int[] getIntegrationIndices(int idx) {
      return myIntegrationNodeIdxs.get(idx);
   }
   public void clearState() {
      for (IntegrationData3d idat : myIntegrationData) {
         idat.clearState();
      }
   }

   public VectorNd getIntegrationWeights() {
      return myIntegrationWeights;
   }

   public double getIntegrationWeight(int idx) {
      return myIntegrationWeights.get(idx);
   }
   
   public void setIntegrationWeights(VectorNd weights) {
      myIntegrationWeights = weights;
      updateAllVolumes();
   }
   
   public void updateAllVolumes() {
      computeRestVolumes();
      computeVolumes();
   }
   
   public void addIntegrationPoint(MFreeIntegrationPoint3d ipnt, IntegrationData3d idata,
      double iwgt, boolean updateVolumes) {
      
      if (idata == null) {
         idata = new IntegrationData3d();
         idata.setRestInverseJacobian(new Matrix3d(Matrix3d.IDENTITY), 1);
      }
      
      int[] idxs = new int[numNodes()];
      for (int j=0; j<numNodes(); j++) {
         idxs[j] = ipnt.getDependentNodes().indexOf(myNodes[j]);
         if (idxs[j]<0) {
            return; //XXX happens when right on the boundary
         }
      }
      
      int idx = myIntegrationWeights.adjustSize(1);
      myIntegrationWeights.set(idx, iwgt);
      myIntegrationData.add(idata);
      myIntegrationPoints.add(ipnt);
      myIntegrationNodeIdxs.add(idxs);
      
      if (updateVolumes) {
         updateAllVolumes();
      }
   }

   public void setIntegrationData(ArrayList<IntegrationData3d> data) {
      myIntegrationData = new ArrayList<IntegrationData3d>(data.size());
      myIntegrationData.addAll(data);
   }
   
   public void setIntegrationPoints(ArrayList<MFreeIntegrationPoint3d> points, ArrayList<IntegrationData3d> data) {
      
      myIntegrationPoints = new ArrayList<MFreeIntegrationPoint3d>(points.size()); 
      myIntegrationNodeIdxs = new ArrayList<int[]>(points.size());
      
      for (int i=0; i<points.size(); i++) {
         IntegrationData3d idata = null;
         if (data != null) {
            idata = data.get(i);
         }
         addIntegrationPoint(points.get(i), idata, points.get(i).getWeight(), false);
      }
      updateAllVolumes();
      
   }
   
   public void setIntegrationPoints(ArrayList<MFreeIntegrationPoint3d> points) {
      setIntegrationPoints(points, null);
   }

   public MFreeIntegrationPoint3d getWarpingPoint() {
      return myWarpingPoint;
   }
   
   public IntegrationData3d getWarpingData() {
      return myWarpingData;
   }
   
   public void setWarpingPoint(MFreeIntegrationPoint3d warp) {
      myWarpingPoint = warp;
      myWarpingData = new IntegrationData3d();
      myWarpingData.setRestJacobian(Matrix3d.IDENTITY);
   }
   
   public void setWarpingPoint(MFreeIntegrationPoint3d warp, IntegrationData3d data) {
      myWarpingPoint = warp;
      myWarpingData = data;
   }
   
   public void setWarpingPointData(IntegrationData3d data) {
      myWarpingData = data;
   }

   // Auxiliary materials, used for implementing muscle fibres
   public void addAuxiliaryMaterial(AuxiliaryMaterial mat) {
      if (myAuxMaterials == null) {
         myAuxMaterials = new ArrayList<AuxiliaryMaterial>(4);
      }
      myAuxMaterials.add(mat);
   }

   public boolean removeAuxiliaryMaterial(AuxiliaryMaterial mat) {
      if (myAuxMaterials != null) {
         return myAuxMaterials.remove(mat);
      }
      else {
         return false;
      }
   }

   public int numAuxiliaryMaterials() {
      return myAuxMaterials == null ? 0 : myAuxMaterials.size();
   }
   
   public AuxiliaryMaterial[] getAuxiliaryMaterials() {
      if (myAuxMaterials == null) {
         return new AuxiliaryMaterial[0];
      }
      else {
         return myAuxMaterials.toArray(new AuxiliaryMaterial[0]);
      }
   }
   
   public void connectToHierarchy() {
      super.connectToHierarchy();

      MFreeNode3d[] nodes = getNodes();
      for (int i = 0; i < nodes.length; i++) {
         for (int j = 0; j < nodes.length; j++) {
            nodes[i].registerNodeNeighbor(nodes[j]);
         }
         nodes[i].addElementDependency(this);
      }

      setMass(0);
      
      myKBlocks = new FemNodeNeighbor[numNodes()][numNodes()];
      for (int i=0; i<myNodes.length; i++) {
         MFreeNode3d node = myNodes[i];
         int cnt = 0;
         for (FemNodeNeighbor nbr : node.getNodeNeighbors()){
            int j = getLocalNodeIndex (nbr.getNode());
            if (j != -1) {
               myKBlocks[i][j] = nbr;
               cnt++;
            }
         }
         if (cnt != myNodes.length) {
            System.out.println ("element class " + getClass());
            throw new InternalErrorException (
               "Node "+node.getNumber()+" has "+cnt+
               " local neighbors, expecting "+myNodes.length);
         }
      }
   }
   
   public void disconnectFromHierarchy() {
      myKBlocks = null;
      
      MFreeNode3d[] nodes = getNodes();
      double massPerNode = getMass() / numNodes();
      for (int i = 0; i < nodes.length; i++) {
         for (int j = 0; j < nodes.length; j++) {
            nodes[i].deregisterNodeNeighbor(nodes[j]);
         }
         nodes[i].addMass(-massPerNode);
         nodes[i].removeElementDependency(this);
      }
      super.disconnectFromHierarchy();
   }

   public void addNodeStiffness(int i, int j, boolean corotated) {
      if (!myWarpingStiffnessValidP) {
         updateWarpingStiffness();
      }
      if (active[i][j]) {
         myWarper.addNodeStiffness(myKBlocks[i][j], active, i, j, corotated);
      }
   }
   
   public void addMaterialStiffness(int i, int j, Vector3d gi, Matrix6d D,
      SymmetricMatrix3d sig, Vector3d gj, double dv) {
      if (active[i][j]) {
         myKBlocks[i][j].addMaterialStiffness(gi, D, sig, gj, dv);
      }
   }
   
   public void addPressureStiffness(int i, int j, Vector3d gi, double p,
      Vector3d gj, double dv) {
      if (active[i][j]) {
         myKBlocks[i][j].addPressureStiffness(gi, p, gj, dv);
      }
   }
   
   public void addDilationalStiffness(int i, int j, double kp, Vector3d intGi, Vector3d intGj) {
      if (active[i][j]) {
         myKBlocks[i][j].addDilationalStiffness(kp, intGi, intGj);
      }
   }
   
   public void addDilationalStiffness (int i, int j,
      MatrixNd Rinv, MatrixBlock GT_i, MatrixBlock GT_j) {
      if (active[i][j]) {
         myKBlocks[i][j].addDilationalStiffness(Rinv, GT_i, GT_j);
      }
   }

   public void setTransposedStiffness() {
      for (int i=0; i<myNodes.length; i++) {
         for (int j=i+1; j<myNodes.length; j++) {
            setTransposedStiffness(j, i);
         }
      }
   }
   
   /**
    * Sets K[i][j] = K[j][i]^T
    */
   public void setTransposedStiffness(int i, int j) {
      myKBlocks[i][j].setTransposedStiffness(myKBlocks[j][i]);
   }

   public void addNodeForce(Vector3d f, int i, boolean corotated) {
      if (!myWarpingStiffnessValidP) {
         updateWarpingStiffness();
      }
      myWarper.addNodeForce(f, active, i, myNodes, corotated, myKBlocks[i]);
   }

   public void updateWarpingStiffness() {
      FemMaterial mat = getEffectiveMaterial();
      if (mat instanceof LinearMaterial) {
         if (myWarper == null) {
            myWarper = new MFreeStiffnessWarper3d(numNodes());
         }
         LinearMaterial lmat = (LinearMaterial)mat;
         myWarper.computeInitialStiffness(
            this, lmat.getYoungsModulus(), lmat.getPoissonsRatio());
      }
      myWarpingStiffnessValidP = true;
   }

   public void computeWarping() {
      if (!myWarpingStiffnessValidP) {
         updateWarpingStiffness();
      }

      MFreeIntegrationPoint3d wpnt = getWarpingPoint();
      IntegrationData3d wdata = getWarpingData();
      wpnt.updatePosState(); // Safety... shouldn't be required because it should be updated by model
      wpnt.computeJacobianAndGradient(wdata.getInvJ0());
      myWarper.computeRotation(wpnt.getF(), null);
   }

   public void computeWarping(Matrix3d F, SymmetricMatrix3d P) {
      if (!myWarpingStiffnessValidP) {
         updateWarpingStiffness();
      }
      myWarper.computeRotation(F, P);
   }
   
   public FemNodeNeighbor getStiffnessBlock(int i, int j) {
      return myKBlocks[i][j];
   }

   public FemNodeNeighbor[][] getStiffnessBlocks() {
      return myKBlocks;
   }

   public MFreeStiffnessWarper3d getStiffnessWarper() {
      return myWarper;
   }

   public void computeCentroid(Vector3d centroid) {

      if (myBoundaryMesh != null) {
         myBoundaryMesh.computeCentroid(centroid);
      } else {
         // XXX HACK
         // based on integration points
         if (myIntegrationPoints.size() > 0) {
            centroid.setZero();
            for (MFreeIntegrationPoint3d ipnt : myIntegrationPoints) {
               centroid.add(ipnt.getPosition());
            }
            centroid.scale(1.0/myIntegrationPoints.size());
         } else {
            centroid.setZero();
            for (MFreeNode3d node : myNodes) {
               centroid.add(node.getTruePosition());
            }
            centroid.scale(1.0/myNodes.length);
         }
         
      }
   }

   public double computeCovariance (Matrix3d C) {
      return -1;
   }
   
   public void updateBounds(Point3d min, Point3d max) {
      
      if (myBoundaryMesh != null) {
         myBoundaryMesh.updateBounds(min, max);
      } else { 
         // XXX HACK, based on ipnts or nodes
         if (myIntegrationPoints.size() > 0) {
            for (MFreeIntegrationPoint3d ipnt : myIntegrationPoints) {
               Point3d pos = ipnt.getPosition();
               pos.updateBounds(min, max);
            }
         } else {
            for (MFreeNode3d node : myNodes) {
               node.updateBounds(min, max);
            }
         }
      }
   }

   public double getRestVolume() {
      return myRestVolume;
   }

   public void setRestVolume(double vol) {
      
      // adjust integration weights
      if (myIntegrationWeights != null) {
         double rcompute = computeRestVolumes();
         myIntegrationWeights.scale(vol / rcompute);
         computeVolumes();
      }
      myRestVolume = vol;
   }
   
   public void updateJacobiansAndGradients() {
      
      ArrayList<MFreeIntegrationPoint3d> ipnts = getIntegrationPoints();
      ArrayList<IntegrationData3d> idata = getIntegrationData();
      setInverted(false);

      for (int i = 0; i < ipnts.size(); i++) {
         MFreeIntegrationPoint3d ipnt = ipnts.get(i);
         IntegrationData3d idat = idata.get(i);
         ipnt.computeJacobianAndGradient(idat.getInvJ0());
         double detJ = ipnt.computeInverseJacobian();
         if (detJ <= 0) {
            setInverted(true);
         }
      }
   }

   @Override
   public double computeVolumes() {
            
      double vol = 0;
      double minDetJ = Double.MAX_VALUE;

      ArrayList<MFreeIntegrationPoint3d> ipnts = getIntegrationPoints();
      ArrayList<IntegrationData3d> idata = getIntegrationData();
      VectorNd iwgts = getIntegrationWeights();
      
      for (int i=0; i<ipnts.size(); i++) {
         MFreeIntegrationPoint3d pt = ipnts.get(i);
         
         double detJ = pt.getJ().determinant();
         double dv = detJ*iwgts.get(i);
         // normalize detJ to get true value relative to rest position
         detJ *= idata.get(i).getDetJ0();
         if (detJ < minDetJ) {
            minDetJ = detJ;
         }
         vol += dv;
      }
      myVolume = vol;
      return minDetJ;
      
   }
   
   public void setVolume(double vol) {
      myVolume = vol;
   }
   
   @Override
   protected double computeRestVolumes() {
      double vol = 0;
      ArrayList<IntegrationData3d> idata = getIntegrationData();
      VectorNd iwgts = getIntegrationWeights();
      
      for (int i=0; i<numIntegrationPoints(); i++) {
         double dv = iwgts.get(i)*idata.get(i).getDetJ0();
         vol += dv;
      }
      myRestVolume = vol;
      myRestVolumeValidP = true;
      return vol;
   }

   private MeshRenderInfo myRenderInfo = null;
   
   @Override
   public void prerender (RenderList list) {
      super.prerender(list);
      renderMeshValid = false;
      myRenderInfo = PolygonalMeshRenderer.getInstance ().prerender (
         myBoundaryMesh, getRenderProps(), myRenderInfo);
   }
   
   protected void renderEdges(Renderer renderer, RenderProps props) {
      if (myBoundaryMesh != null) {
         PolygonalMeshRenderer.getInstance().renderEdges(
            renderer, myBoundaryMesh, props, /*selected=*/false, myRenderInfo);
      }
   }

   public void renderWidget(Renderer renderer, RenderProps props, int flags) {
      renderWidget(renderer, myElementWidgetSize, props, flags);
   }

   public void renderWidget(Renderer renderer, double size,
      RenderProps props, int flags) {

      if (myBoundaryMesh != null && size > 0) {
         if (!renderMeshValid) {
            for (Vertex3d vtx : myBoundaryMesh.getVertices()) {
               if (vtx instanceof MFreeVertex3d) {
                  ((MFreeVertex3d)vtx).updatePosAndVelState();
               }
            }
            renderMeshValid = true;
         }
         Point3d cntr = new Point3d();
         AffineTransform3d trans =  new AffineTransform3d();
         myBoundaryMesh.computeCentroid(cntr);
         cntr.scale(1-size);
         trans.setTranslation(cntr);
         trans.applyScaling(size, size, size);
         
         renderer.pushModelMatrix();
         renderer.mulModelMatrix (trans);
         //renderer.drawMesh(props, myBoundaryMesh, 0);
         myBoundaryMesh.render (renderer, props, 0);
         renderer.popModelMatrix();
         
      }
   }

   public void setBoundaryMesh(PolygonalMesh bmesh) {
      myBoundaryMesh = bmesh;
   }

   public PolygonalMesh getBoundaryMesh() {
      return myBoundaryMesh;
   }

   /**
    * Computes the current coordinates and deformation gradient at the
    * warping point, using render coordinates. This is used in element
    * rendering.
    */
   public void computeRenderCoordsAndGradient (Matrix3d F, float[] coords) {
      MFreeIntegrationPoint3d ipnt = getWarpingPoint();
      IntegrationData3d idata = getWarpingData();
      ipnt.computeGradientForRender (F, getNodes(), idata.getInvJ0());
      ipnt.computeCoordsForRender (coords, getNodes());
   }
   
   @Override
   public void render(Renderer renderer, int flags) {
      RenderProps myProps = getRenderProps();
      if (myProps.isVisible()) {
         super.render(renderer, flags);
         renderWidget(renderer, myProps, 0);
      }
   }
   
   /** 
    * Set reference frame information for this element. This can be used for
    * computing anisotropic material parameters. In principle, each integration
    * point can have its own frame information, but this method simply sets the
    * same frame information for all the integration points, storing it in each
    * IntegrationData structure. Setting <code>M</code> to <code>null</code>
    * removes the frame information.
    * 
    * @param M frame information (is copied by the method)
    */
   public void setFrame (Matrix3dBase M) {
      Matrix3d F = null;
      if (M != null) {
         F = new Matrix3d (M);
      }
      ArrayList<IntegrationData3d> idata = getIntegrationData();
      for (int i=0; i<idata.size(); i++) {
         idata.get(i).setFrame(F);
      }
   }

   public Matrix3d getFrame() {
      ArrayList<IntegrationData3d> idata = getIntegrationData();
      return idata.get(0).getFrame();
   }
   
   /**  
    * Returns an array of MatrixBlocks to be used as constraints to make the
    * element incompressible. Note this method does not compute values for
    * these constraints; it only returns storage for them.
    * 
    * @return incompressibility constraints
    */
   public MatrixBlock[] getIncompressConstraints() {
      if (myIncompressConstraints == null) {
         int n = numNodes();
         MatrixBlock[] constraints = new MatrixBlock[n];
         for (int i=0; i<n; i++) {
            constraints[i] = MatrixBlockBase.alloc (3, 1);
         }
         myIncompressConstraints = constraints;
      }
      return myIncompressConstraints;
   }
   
   // implementation of IndexedPointSet

   public int numPoints() {
      return numNodes();
   }

   public Point3d getPoint (int idx) {
      return myNodes[idx].getPosition();
   }
   
   public void render(
      Renderer renderer, RenderProps rprops, int flags) {
      
      Shading savedShading = renderer.setLineShading (rprops);
      renderer.setLineColoring (rprops, isSelected());
      if (rprops.getLineWidth() > 0) {
         switch (rprops.getLineStyle()) {
            case LINE: {
               renderer.setLineWidth (rprops.getLineWidth());
               renderEdges (renderer, rprops);
               renderer.setLineWidth (1);
               break;
            }
            case CYLINDER: {
               renderEdges (renderer,rprops);
               break;
            }
            default:
               break;
         }
      }
      renderer.setShading (savedShading);
   }   
   
}
