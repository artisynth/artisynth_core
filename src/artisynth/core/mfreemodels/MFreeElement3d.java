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

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode;
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
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.SparseMatrixCRS;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.numerics.GoldenSectionSearch;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;
import maspack.util.DynamicArray;
import maspack.util.InternalErrorException;

public class MFreeElement3d extends FemElement3d implements Boundable { //, TransformableGeometry {

   private MFreeShapeFunction myShapeFunction;
   
   protected DynamicArray<MFreeIntegrationPoint3d> myIntegrationPoints;
   MatrixNd myNodalExtrapolationMatrix = null;
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
      
      Point3d coords = new Point3d();
      VectorNd N = new VectorNd(numNodes ());
      getNaturalCoordinates (coords, pnt, 1000, N);
        
      return coordsAreInside (coords);
   }

   public boolean isInvertedAtRest() {
      MFreeIntegrationPoint3d[] ipnts = getIntegrationPoints();
      IntegrationData3d[] idata = getIntegrationData();
      
      Matrix3d F = new Matrix3d();
      for (int i=0; i<ipnts.length; i++) {
         MFreeIntegrationPoint3d ipnt = ipnts[i];
         IntegrationData3d idat = idata[i];
         
         if (ipnt.computeGradient(F, ipnt.myDependentNodes, idat.getInvJ0()) < 0) {
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
      
//   public void clearState() {
//      for (IntegrationData3d idat : myIntegrationData) {
//         idat.clearState();
//      }
//   }

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
            return; // happens when right on the boundary
         }
      }
      
      //      int idx = myIntegrationWeights.adjustSize(1);
      //      myIntegrationWeights.set(idx, iwgt);
      int nipnts = numIntegrationPoints ();
      if (myIntegrationData == null) {
         myIntegrationData = new IntegrationData3d[1];
      } else if (myIntegrationData.length < nipnts + 1){
         myIntegrationData = Arrays.copyOf (myIntegrationData, nipnts+1);
      }
      myIntegrationData[nipnts] = idata;
      
      ipnt.setNumber(nipnts);
      myIntegrationPoints.add(ipnt);
      //      myIntegrationNodeIdxs.add(idxs);
      
      myNodalExtrapolationMatrix = null;
      
      if (updateVolumes) {
         updateAllVolumes();
      }
   }

   public void setIntegrationData(ArrayList<IntegrationData3d> data) {
      myIntegrationData = new IntegrationData3d[data.size ()];
      for (int i=0; i<data.size (); ++i) {
         myIntegrationData[i] = data.get (i);
      }
   }
   
   public void setIntegrationPoints(ArrayList<MFreeIntegrationPoint3d> points, 
      ArrayList<IntegrationData3d> data) {
      
      myIntegrationPoints = new DynamicArray<>(MFreeIntegrationPoint3d.class, points.size()); 
      myIntegrationData = new IntegrationData3d[points.size ()];
      
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
      myIntegrationData = Arrays.copyOf (data, data.length);
      myNodalExtrapolationMatrix = null;
      updateAllVolumes();
   }
   
   public void setIntegrationPoints(ArrayList<MFreeIntegrationPoint3d> points) {
      setIntegrationPoints(points, null);
   }

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
         // centroid of integration points
         if (myIntegrationPoints.size() > 0) {
            centroid.setZero();
            for (MFreeIntegrationPoint3d ipnt : myIntegrationPoints) {
               ipnt.updatePosState ();
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

   SparseMatrixCRS volumeExtrapolationMatrix;
   public static boolean extrapolateVolumeFromShapeFunctions = true;
   
   public boolean extrapolatesNodalVolume() {
      return true;
   }
   
   private void computeVolumeExtrapolationMatrix() {

      IntegrationPoint3d[] ipnts = getIntegrationPoints ();
      int[] indices = new int[ipnts.length*numNodes ()*2];
      double[] values = new double[ipnts.length*numNodes ()];
      int numVals = 0;
      
      for (int i=0; i<numIntegrationPoints (); ++i) {
         if (extrapolateVolumeFromShapeFunctions) {
            VectorNd N = ipnts[i].getShapeWeights ();
            for (int j = 0; j<numNodes(); ++j) {
               indices[2*numVals] = i;
               indices[2*numVals+1] = j;
               values[numVals] = N.get (j);
               ++numVals;
            }
         } else {
            
            // set to nearest node, or divide equally if equidistant
            // XXX for some reason this ends up being non-symmetric, leads to instabilities?
            VectorNd N = ipnts[i].getShapeWeights ();
            double maxN = Double.NEGATIVE_INFINITY;
            
            int maxIdx = -1;
            for (int j=0; j<N.size (); ++j) {
               double n = N.get (j);
               if (n > maxN ) {
                  maxN = n;
                  maxIdx = j;
               }
            }
            
            // count how many within epsilon?
            double eps = 1e-12/N.size ();
            int maxCount = 0;
            for (int j=0; j<N.size (); ++j) {
               double n = N.get (j);
               if (n > maxN-eps) {
                  ++maxCount;
               }
            }
            
            //            double w = 1.0/maxCount;
            //            int nmaxCount = 0;
            //            for (int j=0; j<N.size (); ++j) {
            //               if (N.get (j) > maxN-eps) {
            //                  indices[2*numVals] = i;
            //                  indices[2*numVals+1] = j;
            //                  values[numVals] = w;
            //                  ++numVals;
            //                  ++nmaxCount;
            //               }
            //            }
            //            if (nmaxCount != maxCount) {
            //               System.out.println ("hmm...");
            //            }
            indices[2*numVals] = i;
            indices[2*numVals+1] = maxIdx;
            values[numVals] = 1.0;
            ++numVals;
            
         }
      }
      
      volumeExtrapolationMatrix = new SparseMatrixCRS (numIntegrationPoints (), numNodes ());
      volumeExtrapolationMatrix.set (values, indices, numVals);
      
   }

   public SparseMatrixCRS getNodalVolumeExtrapolationMatrix () {
      if (volumeExtrapolationMatrix == null) {
         computeVolumeExtrapolationMatrix ();
      }
      return volumeExtrapolationMatrix;
   }

   public void updateBounds(Vector3d min, Vector3d max) {
      
      if (myBoundaryMesh != null) {
         myBoundaryMesh.updateBounds(min, max);
      } else { 
         // based on ipnts or nodes
         if (myIntegrationPoints.size() > 0) {
            for (MFreeIntegrationPoint3d ipnt : myIntegrationPoints) {
               Point3d pos = ipnt.getPosition();
               pos.updateBounds(min, max);
            }
         }
         //         else {
         //            for (FemNode3d node : myNodes) {
         //               node.updateBounds(min, max);
         //            }
         //         }
      }
   }

//   public double getRestVolume() {
//      return myRestVolume;
//   }

   public void setRestVolume(double vol) {
      // adjust integration weights
      
      computeRestVolumes();
      for (MFreeIntegrationPoint3d pt : myIntegrationPoints.getArray()) {
         pt.setWeight(pt.getWeight() * vol / getRestVolume());
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
      public RenderProps createRenderProps () {
         // allow points
         return new RenderProps ();
      }
   
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
         
         if (isSelected()) {
            flags |= Renderer.HIGHLIGHT;
         }
         
         myBoundaryMesh.render (renderer, props, flags);
         renderer.popModelMatrix();
      } else {
         
         // draw ipnts
         float[] coords = new float[3];
         for (MFreeIntegrationPoint3d ipnt : getIntegrationPoints ()) {
            ipnt.getPosition ().get (coords);
            renderer.drawPoint (myRenderProps, coords, isSelected());
         }         
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
      
      renderWidget (renderer, rprops, flags);
      
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
   public MatrixNd getNodalExtrapolationMatrix() {
      if (myNodalExtrapolationMatrix == null) {
         // build
         IntegrationData3d[] idata = getIntegrationData();
         IntegrationPoint3d[] ipnts = getIntegrationPoints();
         myNodalExtrapolationMatrix = 
            new MatrixNd (idata.length, myNodes.length);
         for (int k=0; k<idata.length; ++k) {
            VectorNd N = ipnts[k].getShapeWeights();
            for (int j=0; j<myNodes.length; ++j) {
               myNodalExtrapolationMatrix.set (j, k, N.get(j));
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
      
      // XXX exclusion
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
    * @return the number of iterations required for convergence, or
    * -1 if the calculation did not converge.
    */
   public int getNaturalCoordinates (Vector3d coords, Point3d pnt, int maxIters) {
      VectorNd N = new VectorNd(numNodes ());
      return getNaturalCoordinates (coords, pnt, maxIters, N);
   }
   
   protected boolean isElementAtRest() {
      for (FemNode3d node : myNodes) {
         double d = node.getPosition ().distanceSquared (node.getRestPosition ());
         if (d > 0) {
            return false;
         }
      }
      return true;
   }
   
   public void getNaturalRestCoordinates (Vector3d coords, Point3d pnt, VectorNd N) {
      coords.set (pnt);
      myShapeFunction.setNodes ((MFreeNode3d[])this.getNodes ());
      myShapeFunction.setCoordinate (pnt);
      myShapeFunction.eval (N);
   }
   
   /**
    * Given point p, get its natural coordinates with respect to this element.
    * Returns true if the algorithm converges, false if a maximum number of 
    * iterations is reached. Uses a modified Newton's method and Golden-Section Search to solve the 
    * equations. The <code>coords</code> argument that returned the coordinates is
    * used, on input, to supply an initial guess of the coordinates.
    * 
    * @param coords
    * Outputs the natural coordinates, and supplies (on input) an initial
    * guess as to their position.
    * @param pnt
    * A given point (in world coords)
    * @param maxIters
    * Maximum number of iterations
    * @param N
    * Resulting shape function values
    * @return the number of iterations required for convergence, or
    * -1 if the calculation did not converge.
    */
   public int getNaturalCoordinates (Vector3d coords, Point3d pnt, int maxIters, VectorNd N) {

      if (N == null) {
         N = new VectorNd(numNodes());
      } else {
         N.setSize(numNodes());
      }
      
      // if FEM is frame-relative, transform to local coords
      Point3d lpnt = new Point3d(pnt);
      // initialize coords
      Point3d pcoord = new Point3d(coords);
      
      
      if (getGrandParent() instanceof FemModel3d) {
         FemModel3d fem = (FemModel3d)getGrandParent();
         if (fem.isFrameRelative()) {
            lpnt.inverseTransform (fem.getFrame().getPose());
         }
      }
      
      if (isElementAtRest ()) {
         getNaturalRestCoordinates (coords, lpnt, N);
         return 0;
      }

      Vector3d res = new Point3d();
      int i;

      double eps = 1e-12;
      double tol = ((MFreeNode3d)myNodes[0]).getInfluenceRadius() * eps;

      myShapeFunction.setNodes ((MFreeNode3d[])myNodes);
      myShapeFunction.setCoordinate (pcoord);
      
      computeNaturalCoordsResidual (res, pcoord, lpnt, N);
      
      double prn = res.norm();
      //System.out.println ("res=" + prn);
      if (prn < tol) {
         // already have the right answer
         coords.set (pcoord);
         return 0;
      }

      LUDecomposition LUD = new LUDecomposition();
      Point3d prevCoords = new Point3d();
      Vector3d dNds = new Vector3d();
      Matrix3d dxds = new Matrix3d();
      Vector3d del = new Point3d();

      GSSResidual func = new GSSResidual(this, lpnt);
      
      /*
       * solve using Newton's method.
       */
      for (i = 0; i < maxIters; i++) {
         // compute the Jacobian dx/ds for the current guess
         dxds.setZero();
         for (int k=0; k<numNodes(); k++) {
            myShapeFunction.evalDerivative(k, dNds);
            dxds.addOuterProduct (myNodes[k].getLocalPosition(), dNds);
         }
         LUD.factor (dxds);
         double cond = LUD.conditionEstimate (dxds);
         if (cond > 1e10)
            System.err.println (
               "Warning: condition number for solving natural coordinates is "
               + cond);
         // solve Jacobian to obtain an update for the coords
         LUD.solve (del, res);
         del.negate ();
         
         if (del.norm () < tol) {
            coords.set (pcoord);
            return i+1;
         }

         prevCoords.set (pcoord); 
         pcoord.add (del);
         
         myShapeFunction.setCoordinate(pcoord);
         computeNaturalCoordsResidual (res, pcoord, lpnt, N);
         double rn = res.norm();
         //System.out.println ("res=" + rn);

         // If the residual norm is within tolerance, we have converged.
         if (rn < tol) {
            //System.out.println ("2 res=" + rn);
            coords.set (pcoord);
            return i+1;
         }
         
         // do a golden section search in range
         func.set (prevCoords, del);
         double alpha = GoldenSectionSearch.minimize(func, 0, 1, eps, 0.8*prn*prn);
         pcoord.scaledAdd (alpha, del, prevCoords);
         computeNaturalCoordsResidual(res, pcoord, lpnt, N);
         rn = res.norm();
         
         //System.out.println (" alpha=" + alpha + " rn=" + rn + " prn=" + prn);
         if (alpha < eps) {
            return -1;  // failed
         }
         
         prn = rn;
      }
      coords.set (pcoord);
      return -1; // failed
   }
   
   @Override
   public double getN(int i, Vector3d coords) {
      Point3d pnt = new Point3d(coords);
      myShapeFunction.setNodes ((MFreeNode3d[])myNodes);
      myShapeFunction.setCoordinate (pnt);
      return myShapeFunction.eval(i);
   }
   
   @Override
   public void getdNds(Vector3d dNds, int i, Vector3d coords) {
      Point3d pnt = new Point3d(coords);
      myShapeFunction.setNodes ((MFreeNode3d[])myNodes);
      myShapeFunction.setCoordinate(pnt);
      myShapeFunction.evalDerivative(i, dNds);
   }

   // cache local node index
   HashMap<FemNode,Integer> cachedLocalNodeIndex = null;
   @Override
   public int getLocalNodeIndex (FemNode p) {
      if (cachedLocalNodeIndex == null) {
         cachedLocalNodeIndex = new HashMap<> ();
         for (int i=0; i<myNodes.length; ++i) {
            cachedLocalNodeIndex.put (myNodes[i], i);
         }
      }
      Integer idx = cachedLocalNodeIndex.get (p);
      if (idx == null) {
         idx = -1;
      }
      return idx;
   }
   
   @Override
   public double[] getIntegrationCoords() {
      return new double[0];
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
   public int[] getTriangulatedFaceIndices() {
      return new int[0];
   }

   @Override
   public double[] getNodeCoords() {
      return new double[0];
   }

   @Override
   public double[] getNodeMassWeights() {
      return new double[0];
   }

   @Override
   public void renderWidget(Renderer renderer, double size, RenderProps props) {
      
   }
   
}
