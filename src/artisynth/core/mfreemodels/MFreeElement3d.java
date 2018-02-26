/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.IntegrationData3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.StiffnessWarper3d;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Boundable;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.PolygonalMeshRenderer;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.LUDecomposition;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;
import maspack.util.DynamicArray;
import maspack.util.InternalErrorException;

public class MFreeElement3d extends FemElement3d implements Boundable {

   private MFreeShapeFunction myShapeFunction;
   
   protected DynamicArray<IntegrationData3d> myIntegrationData;
   protected DynamicArray<MFreeIntegrationPoint3d> myIntegrationPoints;
   double[] myNodalExtrapolationMatrix = null;
   MFreeIntegrationPoint3d myWarpingPoint;
 
   protected boolean myIntegrationDataValid = false;
  
   protected PolygonalMesh myBoundaryMesh = null;
   boolean renderMeshValid = false;

   public MFreeElement3d (MFreeShapeFunction fun, FemNode3d[] nodes) {
      int npvals = numPressureVals();
      myLagrangePressures = new double[npvals];
      myRestVolumes = new double[npvals];
      myVolumes = new double[npvals];

      myNodes = new MFreeNode3d[nodes.length];
      
      for (int i=0; i<nodes.length; ++i) {
         if (!(nodes[i] instanceof MFreeNode3d)) {
            throw new InternalErrorException("All nodes must be of type MFreeNode3d");
         }
         myNodes[i] = (MFreeNode3d)nodes[i];
      }
      
      myIntegrationData = new DynamicArray<>(IntegrationData3d.class);
      myIntegrationPoints = new DynamicArray<>(MFreeIntegrationPoint3d.class);

      setShapeFunction(fun);
   }
   
   public void setShapeFunction(MFreeShapeFunction f) {
      myShapeFunction = f;
   }
   
   public MFreeShapeFunction getShapeFunction() {
      return myShapeFunction;
   }
   
   public boolean isInside(Point3d pnt) {
      if (myBoundaryMesh != null) {
         BVFeatureQuery query = new BVFeatureQuery();
         return query.isInsideOrientedMesh (myBoundaryMesh, pnt, 1e-10);
      }
      //      for (MFreeNode3d node : myNodes) {
      //         if (!node.isInDomain(pnt, 0)) {
      //            return false;
      //         }
      //      }
      return true;
   }

   public boolean isInvertedAtRest() {
      MFreeIntegrationPoint3d[] ipnts = getIntegrationPoints();
      IntegrationData3d[] idata = getIntegrationData();
      
      for (int i=0; i<ipnts.length; i++) {
         MFreeIntegrationPoint3d ipnt = ipnts[i];
         IntegrationData3d idat = idata[i];
         
         ipnt.computeJacobianAndGradient(idat.getInvJ0());
         if (ipnt.getDetF()< 0) {
            return true;
         }
      }
      
      return false;
      
   }

   public int numIntegrationPoints() {
      return myIntegrationPoints.size();
   }

   public MFreeIntegrationPoint3d[] getIntegrationPoints() {
      return myIntegrationPoints.getArray();
   }
   
   public MFreeIntegrationPoint3d getIntegrationPoint(int idx) {
      return myIntegrationPoints.get(idx);
   }
   
   public int getIntegrationPointIndex(IntegrationPoint3d pnt) {
      return pnt.getNumber();
   }
   
   public IntegrationData3d[] getIntegrationData() {
      return myIntegrationData.getArray();
   }
   
   public IntegrationData3d getIntegrationData(int idx) {
      return myIntegrationData.get(idx);
   }
      
   public void clearState() {
      for (IntegrationData3d idat : myIntegrationData) {
         idat.clearState();
      }
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
         idxs[j] = ipnt.getNodeCoordIdx(myNodes[j]);
         if (idxs[j]<0) {
            return; //XXX happens when right on the boundary
         }
      }
      
      //      int idx = myIntegrationWeights.adjustSize(1);
      //      myIntegrationWeights.set(idx, iwgt);
      myIntegrationData.add(idata);
      // ipnt.setNumber(myIntegrationPoints.size());
      ipnt.setNumber(myIntegrationPoints.size());
      myIntegrationPoints.add(ipnt);
      //      myIntegrationNodeIdxs.add(idxs);
      
      
      myNodalExtrapolationMatrix = null;
      
      if (updateVolumes) {
         updateAllVolumes();
      }
   }

   public void setIntegrationData(ArrayList<IntegrationData3d> data) {
      myIntegrationData = new DynamicArray<>(IntegrationData3d.class, data.size());
      myIntegrationData.addAll(data);
   }
   
   public void setIntegrationPoints(ArrayList<MFreeIntegrationPoint3d> points, ArrayList<IntegrationData3d> data) {
      
      myIntegrationPoints = new DynamicArray<>(MFreeIntegrationPoint3d.class, points.size()); 
      // myIntegrationNodeIdxs = new ArrayList<int[]>(points.size());
      
      for (int i=0; i<points.size(); i++) {
         IntegrationData3d idata = null;
         if (data != null) {
            idata = data.get(i);
         }
         addIntegrationPoint(points.get(i), idata, points.get(i).getWeight(), false);
      }
      updateAllVolumes();
      myNodalExtrapolationMatrix = null;
      
   }
   
   public void setIntegrationPoints(MFreeIntegrationPoint3d[] points, IntegrationData3d[] data) {
      
      myIntegrationPoints = new DynamicArray<>(points);
      myIntegrationData = new DynamicArray<>(data);
      myNodalExtrapolationMatrix = null;
      updateAllVolumes();
   }
   
   public void setIntegrationPoints(ArrayList<MFreeIntegrationPoint3d> points) {
      setIntegrationPoints(points, null);
   }

   //   public MFreeIntegrationPoint3d getWarpingPoint() {
   //      return myWarpingPoint;
   //   }
   //   
   //   public IntegrationData3d getWarpingData() {
   //      return myWarpingData;
   //   }
   
   public void setWarpingPoint(MFreeIntegrationPoint3d warp) {
      myWarpingPoint = warp;
      myWarpingData = new IntegrationData3d();
      myWarpingData.setRestJacobian(Matrix3d.IDENTITY);
   }
   
   @Override
   public MFreeIntegrationPoint3d getWarpingPoint() {
      return myWarpingPoint;
   }
   
   public void setWarpingPoint(MFreeIntegrationPoint3d warp, IntegrationData3d data) {
      myWarpingPoint = warp;
      myWarpingData = data;
   }
   
   public void setWarpingPointData(IntegrationData3d data) {
      myWarpingData = data;
   }
   
   protected StiffnessWarper3d createStiffnessWarper () {
      return new StiffnessWarper3d (this);
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
            for (FemNode3d node : myNodes) {
               centroid.add(((MFreeNode3d)node).getTruePosition());
            }
            centroid.scale(1.0/myNodes.length);
         }
         
      }
   }
   
   @Override
   public boolean integrationPointsInterpolateToNodes() {
      return true;
   }
   
   public void updateBounds(Vector3d min, Vector3d max) {
      
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
            for (FemNode3d node : myNodes) {
               node.updateBounds(min, max);
            }
         }
      }
   }

//   public double getRestVolume() {
//      return myRestVolume;
//   }

   public void setRestVolume(double vol) {
      // adjust integration weights
      
      double rcompute = computeRestVolumes();
      for (MFreeIntegrationPoint3d pt : myIntegrationPoints.getArray()) {
         pt.setWeight(pt.getWeight() * vol / rcompute);
      }
      computeVolumes();
         
      myRestVolume = vol;
   }
   
//   public void updateJacobiansAndGradients() {
//      
//      MFreeIntegrationPoint3d[] ipnts = getIntegrationPoints();
//      IntegrationData3d[] idata = getIntegrationData();
//      setInverted(false);
//
//      for (int i = 0; i < ipnts.length; i++) {
//         MFreeIntegrationPoint3d ipnt = ipnts[i];
//         IntegrationData3d idat = idata[i];
//         ipnt.computeJacobianAndGradient(idat.getInvJ0());
//         double detJ = ipnt.computeInverseJacobian();
//         if (detJ <= 0) {
//            setInverted(true);
//         }
//      }
//   }
//
//   @Override
//   public double computeVolumes() {
//            
//      double vol = 0;
//      double minDetJ = Double.MAX_VALUE;
//      int npvals = numPressureVals();
//
//      for (int k=0; k<npvals; k++) {
//         myVolumes[k] = 0;
//      }
//      
//      IntegrationPoint3d[] ipnts = getIntegrationPoints();
//      IntegrationData3d[] idata = getIntegrationData();
//      // VectorNd iwgts = getIntegrationWeights();
//         
//      for (int i=0; i<ipnts.length; i++) {
//         IntegrationPoint3d pt = ipnts[i];
//         double detJ = pt.getJ().determinant();
//         double dv = detJ*pt.getWeight();
//         // normalize detJ to get true value relative to rest position
//         detJ /= idata[i].getDetJ0();  // (though detJ0 should be 1)
//         idata[i].setDv(dv);
//         if (detJ < minDetJ) {
//            minDetJ = detJ;
//         }
//         if (npvals > 1) {
//            double[] H = pt.getPressureWeights().getBuffer();
//            for (int k=0; k<npvals; k++) {
//               myVolumes[k] += H[k]*dv;
//            } 
//         }
//         vol += dv;
//      }
//      if (npvals == 1) {
//         myVolumes[0] = vol;         
//      }      
//      myVolume = vol;
//      return minDetJ;
//      
//   }
//   
   public void setVolume(double vol) {
      myVolume = vol;
   }
 
//   @Override
//   public double computeRestVolumes() {
//      
//      int npvals = numPressureVals();
//
//      double vol = 0;
//      for (int k=0; k<npvals; k++) {
//         myRestVolumes[k] = 0;
//      }
//      
//      MFreeIntegrationPoint3d[] ipnts = getIntegrationPoints();
//      IntegrationData3d[] idata = getIntegrationData();
//     
//      for (int i=0; i<ipnts.length; i++) {
//         double dV = idata[i].getDetJ0()*ipnts[i].getWeight();
//         if (npvals > 1) {
//            double[] H = ipnts[i].getPressureWeights().getBuffer();
//            for (int k=0; k<npvals; k++) {
//               myRestVolumes[k] += H[k]*dV;
//            }
//         }
//         vol += dV;
//      }
//      
//      if (npvals == 1) {
//         myRestVolumes[0] = vol;         
//      }
//      myRestVolume = vol;
//      
//      //      double vol = 0;
//      //      IntegrationData3d[] idata = getIntegrationData();
//      //      VectorNd iwgts = getIntegrationWeights();
//      //      
//      //      for (int i=0; i<numIntegrationPoints(); i++) {
//      //         double dv = iwgts.get(i)*idata[i].getDetJ0();
//      //         vol += dv;
//      //      }
//      //      myRestVolume = vol;
//      //      myRestVolumeValidP = true;
//      //      return vol;
//      
//      return vol;
//   }

   private PolygonalMeshRenderer myRenderInfo = null;
   
   @Override
   public void prerender (RenderList list) {
      super.prerender(list);
      renderMeshValid = false;
      
      if (myBoundaryMesh != null) {
         myBoundaryMesh.prerender(list);
         if (myRenderInfo == null) {
            myRenderInfo = new PolygonalMeshRenderer (myBoundaryMesh);
         }
         myRenderInfo.prerender (getRenderProps());
      }
   }
   
   protected void renderEdges(Renderer renderer, RenderProps props) {
      if (myBoundaryMesh != null) {
         int flags = 0;
         if (isSelected ()) {
            flags = Renderer.HIGHLIGHT;
         }
         myRenderInfo.renderEdges(renderer, props, flags);
      }
   }

   public void renderWidget(Renderer renderer, RenderProps props, int flags) {
      renderWidget(renderer, myElementWidgetSize, props, flags);
   }

   public void renderWidget(Renderer renderer, double size,
      RenderProps props, int flags) {

      if (myBoundaryMesh != null && size > 0) {
         if (!renderMeshValid) {
            //            for (Vertex3d vtx : myBoundaryMesh.getVertices()) {
            //               if (vtx instanceof MFreeVertex3d) {
            //                  ((MFreeVertex3d)vtx).updatePosAndVelState();
            //               }
            //            }
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
         
         if (isSelected()) {
            flags |= Renderer.HIGHLIGHT;
         }
         myBoundaryMesh.render (renderer, props, flags);
         renderer.popModelMatrix();
         
      }
   }

   public void setBoundaryMesh(PolygonalMesh bmesh) {
      myBoundaryMesh = bmesh;
   }

   public PolygonalMesh getBoundaryMesh() {
      return myBoundaryMesh;
   }

   //   /**
   //    * Computes the current coordinates and deformation gradient at the
   //    * warping point, using render coordinates. This is used in element
   //    * rendering.
   //    */
   //   public void computeRenderCoordsAndGradient (Matrix3d F, float[] coords) {
   //      MFreeIntegrationPoint3d ipnt = getWarpingPoint();
   //      IntegrationData3d idata = getWarpingData();
   //      ipnt.computeGradientForRender (F, getNodes(), idata.getInvJ0());
   //      ipnt.computeCoordsForRender (coords, getNodes());
   //   }
   //   
   //   @Override
   //   public void render(Renderer renderer, int flags) {
   //      RenderProps myProps = getRenderProps();
   //      if (myProps.isVisible()) {
   //         super.render(renderer, flags);
   //         renderWidget(renderer, myProps, 0);
   //      }
   //   }
   //   
   //   /** 
   //    * Set reference frame information for this element. This can be used for
   //    * computing anisotropic material parameters. In principle, each integration
   //    * point can have its own frame information, but this method simply sets the
   //    * same frame information for all the integration points, storing it in each
   //    * IntegrationData structure. Setting <code>M</code> to <code>null</code>
   //    * removes the frame information.
   //    * 
   //    * @param M frame information (is copied by the method)
   //    */
   //   public void setFrame (Matrix3dBase M) {
   //      Matrix3d F = null;
   //      if (M != null) {
   //         F = new Matrix3d (M);
   //      }
   //      IntegrationData3d[] idata = getIntegrationData();
   //      for (int i=0; i<idata.length; i++) {
   //         idata[i].setFrame(F);
   //      }
   //   }
   //
   //   public Matrix3d getFrame() {
   //      IntegrationData3d[] idata = getIntegrationData();
   //      return idata[0].getFrame();
   //   }
   //   
   //   /**  
   //    * Returns an array of MatrixBlocks to be used as constraints to make the
   //    * element incompressible. Note this method does not compute values for
   //    * these constraints; it only returns storage for them.
   //    * 
   //    * @return incompressibility constraints
   //    */
   //   public MatrixBlock[] getIncompressConstraints() {
   //      if (myIncompressConstraints == null) {
   //         int n = numNodes();
   //         MatrixBlock[] constraints = new MatrixBlock[n];
   //         for (int i=0; i<n; i++) {
   //            constraints[i] = MatrixBlockBase.alloc (3, 1);
   //         }
   //         myIncompressConstraints = constraints;
   //      }
   //      return myIncompressConstraints;
   //   }
   //   
   //   // index of the incompressibility constraint associated with
   //   // this element, if any
   //   public int getIncompressIndex() {
   //      return myIncompressIdx;
   //   }
   //
   //   public void setIncompressIndex (int idx) {
   //      myIncompressIdx = idx;
   //   }
   
   // implementation of IndexedPointSet

   public int numPoints() {
      return numNodes() + numIntegrationPoints();
   }

   public Point3d getPoint (int idx) {
      if (idx < numNodes()) {
         return myNodes[idx].getPosition();
      }
      return myIntegrationPoints.get(idx-numNodes()).getPosition();
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
   
//   /**
//    * Returns the number of pressure variables associated with this element.  
//    * All of the linear elements have one pressure variable, whereas some of 
//    * the quadratic elements have more.
//    * 
//    * @return number of pressure variables
//    * 
//    */
//   public int numPressureVals() {
//      // higher order elements should override this if necessary
//      return 1;
//   }
//   
//   /**
//    * Returns the pressure weight matrix for this element. The pressure
//    * weight matrix is given by the inverse of the integral of
//    * H^T H, where H is the row vector formed from the pressure
//    * shape functions.
//    *
//    * <p>By default, this method returns a pressure weight matrix for the case
//    * where there is only one pressure value. Such matrices always have a
//    * single value of 1. Elements with a larger number of pressure values
//    * should override this method to return a pressure weight matrix
//    * appropriate for that element.
//    */
//   public Matrix getPressureWeightMatrix () {
//      if (myPressureWeightMatrix == null) {
//         myPressureWeightMatrix = new Matrix1x1();
//         myPressureWeightMatrix.m00 = 1;
//      }
//      return myPressureWeightMatrix;
//   }
//
   public double[] getNodalExtrapolationMatrix() {
      if (myNodalExtrapolationMatrix == null) {
         // build
         IntegrationData3d[] idata = getIntegrationData();
         IntegrationPoint3d[] ipnts = getIntegrationPoints();
         myNodalExtrapolationMatrix = new double[idata.length*myNodes.length];
         for (int k=0; k<idata.length; ++k) {
            VectorNd N = ipnts[k].getShapeWeights();
            for (int j=0; j<myNodes.length; ++j) {
               myNodalExtrapolationMatrix[j * ipnts.length + k] = N.get(j);
            }
         }
      }
      return myNodalExtrapolationMatrix;
   }
   
   private void computeNaturalCoordsResidual (
      Vector3d res, Vector3d coords, Point3d pnt, VectorNd N) {
      res.setZero();
      for (int k=0; k<numNodes(); k++) {
         double v = myShapeFunction.eval(k);
         N.set(k, v);
         res.scaledAdd (v, myNodes[k].getPosition(), res);
      }
      res.sub (pnt);
   }
   
   /**
    * Computes "natural" coordinates (i.e. shape-function coordinates) of a point
    * in 3D world space
    * @param coords initial guess, value is updated with final coordinate
    * @param pnt world point to determine coordinates of
    * @return true if method is successful, if false may indicate that at iterative
    * method failed to converge
    */
   public boolean getNaturalCoordinates(Point3d coords, Point3d pnt) {
      int iters = getNaturalCoordinates(coords, pnt, 1000, null);
      if (iters < 0) {
         return false;
      }
      return true;
   }
   
   public boolean coordsAreInside(Vector3d coords) {
      for (FemNode3d node : myNodes) {
         if (!((MFreeNode3d)node).isInRestDomain(coords)) {
            return false;
         }
      }
      return true;
   }
   
   public boolean getNaturalCoordinates (Point3d coords, Point3d pnt, VectorNd N) {
      return getNaturalCoordinates(coords, pnt, 1000, N) >= 0;
   }
   
   /**
    * Given point p, get its natural coordinates with respect to this element.
    * Returns true if the algorithm converges, false if a maximum number of 
    * iterations is reached. Uses a modified Newton's method to solve the 
    * equations. The <code>coords</code> argument that returned the coordinates is
    * used, on input, to supply an initial guess of the coordinates.
    * Zero is generally a safe guess.
    * 
    * @param coords
    * Outputs the natural coordinates, and supplies (on input) an initial
    * guess as to their position.
    * @param pnt
    * A given point (in world coords)
    * @param maxIters
    * Maximum number of Newton iterations
    * @param N
    * Resulting shape functionvalues
    * @return the number of iterations required for convergence, or
    * -1 if the calculation did not converge.
    */
   public int getNaturalCoordinates (Point3d coords, Point3d pnt, int maxIters,
      VectorNd N) {

      Vector3d res = new Point3d();
      int i;

      double tol = ((MFreeNode3d)myNodes[0]).getInfluenceRadius() * 1e-12;
      if (tol <= 0) {
         tol = 1e-12;
      }
      
      if (N == null) {
         N = new VectorNd(numNodes());
      } else {
         N.setSize(numNodes());
      }
      
      //      if (!coordsAreInside(coords)) {
      //         return -1;
      //      }
      myShapeFunction.update(coords, (MFreeNode3d[])myNodes);
      computeNaturalCoordsResidual (res, coords, pnt, N);
      
      double prn = res.norm();
      //System.out.println ("res=" + prn);
      if (prn < tol) {
         // already have the right answer
         return 0;
      }

      LUDecomposition LUD = new LUDecomposition();
      Vector3d prevCoords = new Vector3d();
      Vector3d dNds = new Vector3d();
      Matrix3d dxds = new Matrix3d();
      Vector3d del = new Point3d();

      /*
       * solve using Newton's method.
       */
      for (i = 0; i < maxIters; i++) {
         // compute the Jacobian dx/ds for the current guess
         dxds.setZero();
         for (int k=0; k<numNodes(); k++) {
            myShapeFunction.evalDerivative(k, dNds);
            dxds.addOuterProduct (myNodes[k].getPosition(), dNds);
         }
         LUD.factor (dxds);
         double cond = LUD.conditionEstimate (dxds);
         if (cond > 1e10)
            System.err.println (
               "Warning: condition number for solving natural coordinates is "
               + cond);
         // solve Jacobian to obtain an update for the coords
         LUD.solve (del, res);

         prevCoords.set (coords); 
         coords.sub (del);
         //         if (!coordsAreInside(coords)) {
         //            return -1;
         //         }
         myShapeFunction.update(coords,(MFreeNode3d[])myNodes);
         computeNaturalCoordsResidual (res, coords, pnt, N);
         double rn = res.norm();
         //System.out.println ("res=" + rn);

         // If the residual norm is within tolerance, we have converged.
         if (rn < tol) {
            //System.out.println ("2 res=" + rn);
            return i+1;
         }
         
         if (rn > prn) {
            // it may be that "coords + del" is a worse solution.  Let's make
            // sure we go the correct way binary search suitable alpha in [0 1]
            double eps = 1e-12;
            
            // and keep cutting the step size in half until the residual starts
            // dropping again
            double alpha = 0.5;
            while (alpha > eps && rn > prn) {
               coords.scaledAdd (-alpha, del, prevCoords);
               if (!coordsAreInside(coords)) {
                  return -1;
               }
               myShapeFunction.update(coords, (MFreeNode3d[])myNodes);
               computeNaturalCoordsResidual (res, coords, pnt, N);
               rn = res.norm();
               alpha *= 0.5;
               //System.out.println ("  alpha=" + alpha + " rn=" + rn);
            }
            //System.out.println (" alpha=" + alpha + " rn=" + rn + " prn=" + prn);
            if (alpha < eps) {
               return -1;  // failed
            }
         }
         prn = rn;
      }
      return -1; // failed
   }
   
   public double getN(int i, Point3d coords) {
      myShapeFunction.maybeUpdate(coords, (MFreeNode3d[])myNodes);
      return myShapeFunction.eval(i);
   }
   
   public void getdNds(Vector3d dNds, int i, Vector3d coords) {
      myShapeFunction.maybeUpdate(coords, (MFreeNode3d[])myNodes);
      myShapeFunction.evalDerivative(i, dNds);
   }

   @Override
   public double[] getIntegrationCoords() {
      return new double[0];
   }

   @Override
   public double getN(int i, Vector3d coords) {
      return 0;
   }

   @Override
   public int[] getEdgeIndices() {
      return new int[0];
   }

   @Override
   public int[] getFaceIndices() {
      return new int[0];
   }

   @Override
   public double[] getNodeCoords() {
      return new double[0];
   }

   @Override
   public void renderWidget(Renderer renderer, double size, RenderProps props) {
      
      
   }
   
}
