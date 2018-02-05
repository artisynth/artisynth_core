/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.*;
import java.util.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.geometry.*;
import maspack.properties.*;
import maspack.util.InternalErrorException;
import maspack.render.Renderer;
import maspack.render.RenderableUtils;
import maspack.render.RenderProps;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.IncompressibleMaterial;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.PointAttachable;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.FrameAttachable;
import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.util.*;

public abstract class FemElement3d extends FemElement
   implements Boundable, PointAttachable, FrameAttachable {
   protected FemNode3d[] myNodes;
   protected FemNodeNeighbor[][] myNbrs = null;
   // average shape function gradient; used for incompressibility
   //protected Vector3d[] myAvgGNx = null; 
   // altGnx is an alternate copy of avgGNx for use in a second constraint matrix
   //protected Matrix3x1Block[] myIncompressConstraints1 = null;
   protected MatrixBlock[] myIncompressConstraints = null;
   //protected Matrix3x1Block[] myIncompressConstraints2 = null;
   
   protected IntegrationData3d[] myIntegrationData;
   protected IntegrationData3d myWarpingData;
   protected boolean myIntegrationDataValid = false;

   protected double[] myRestVolumes;
   protected double[] myVolumes;
   protected double[] myLagrangePressures;

   //protected SymmetricMatrix3d myAvgStress = new SymmetricMatrix3d();
   //protected double myLagrangePressure;
   protected StiffnessWarper3d myWarper = null;
   private int myIncompressIdx = -1;
   //private int myLocalIncompressIdx = -1;

   private static Matrix1x1 myPressureWeightMatrix = null;

   private static double DEFAULT_ELEMENT_WIDGET_SIZE = 0.0;
   protected double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
   protected PropertyMode myElementWidgetSizeMode = PropertyMode.Inherited;

   // Auxiliary Materials are mainly used for implementing muscle fibres
   protected ArrayList<AuxiliaryMaterial> myAuxMaterials = null;

   public static PropertyList myProps =
      new PropertyList (FemElement3d.class, FemElement.class);

   static {
      myProps.addInheritable (
         "elementWidgetSize:Inherited",
         "size of rendered widget in each element's center",
         DEFAULT_ELEMENT_WIDGET_SIZE, "[0,1]");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FemElement3d() {
      int npvals = numPressureVals();
      myLagrangePressures = new double[npvals];
      myRestVolumes = new double[npvals];
      myVolumes = new double[npvals];
   }

   public void setElementWidgetSize (double size) {
      myElementWidgetSize = size;
      myElementWidgetSizeMode = 
         PropertyUtils.propagateValue (
            this, "elementWidgetSize",
            myElementWidgetSize, myElementWidgetSizeMode);
   }

   public double getElementWidgetSize () {
      return myElementWidgetSize;
   }

   public void setElementWidgetSizeMode (PropertyMode mode) {
      myElementWidgetSizeMode =
         PropertyUtils.setModeAndUpdate (
            this, "elementWidgetSize", myElementWidgetSizeMode, mode);
   }

   public PropertyMode getElementWidgetSizeMode() {
      return myElementWidgetSizeMode;
   }

   @Override
   public FemNode3d[] getNodes() {
      return myNodes;
   }
   
   public FemNodeNeighbor[][] getNodeNeighbors() {
      return myNbrs;
   }

   // index of the incompressibility constraint associated with
   // this element, if any
   public int getIncompressIndex() {
      return myIncompressIdx;
   }

   public void setIncompressIndex (int idx) {
      myIncompressIdx = idx;
   }

//   // index of the local incompressibility constraint associated
//   // with this element, if any
//   public int getLocalIncompressIndex() {
//      return myLocalIncompressIdx;
//   }

//   public void setLocalIncompressIndex (int idx) {
//      myLocalIncompressIdx = idx;
//   }

   /**
    * Returns the number of pressure variables associated with this element.  
    * All of the linear elements have one pressure variable, whereas some of 
    * the quadratic elements have more.
    * 
    * @return number of pressure variables.make
    * 
    */
   public int numPressureVals() {
      // higher order elements should override this if necessary
      return 1;
   }

   /**
    * Returns the value of the pressure shape function. By default, this method
    * returns 1, corresponding to a single pressure variable with constant
    * value over the entire element. Elements with a larger number of pressure
    * DOFs should override this method to supply the appropriate shape
    * functions.
    *
    * @param i index of the pressure variable; should be less
    * than the value returned by {@link #numPressureVals}
    * @param coords coordinates at which the shape function should
    * be evaluated.
    * @return value of the pressure shape function
    */
   public double getH (int i, Vector3d coords) {
      return 1;
   }

   /**
    * Returns the pressure weight matrix for this element. The pressure
    * weight matrix is given by the inverse of the integral of
    * H^T H, where H is the row vector formed from the pressure
    * shape functions.
    *
    * <p>By default, this method returns a pressure weight matrix for the case
    * where there is only one pressure value. Such matrices always have a
    * single value of 1. Elements with a larger number of pressure values
    * should override this method to return a pressure weight matrix
    * appropriate for that element.
    *
    * @return pressure weight matrix for this element
    */
   public Matrix getPressureWeightMatrix () {
      if (myPressureWeightMatrix == null) {
         myPressureWeightMatrix = new Matrix1x1();
         myPressureWeightMatrix.m00 = 1;
      }
      return myPressureWeightMatrix;
   }

   /**
    * Creates a pressure weight matrix for an element. Intended for
    * use when overriding of {@link #getPressureWeightMatrix}.
    */
   protected Matrix createPressureWeightMatrix () {
      int npvals = numPressureVals();
      if (npvals == 1) {
         Matrix1x1 M = new Matrix1x1();
         M.m00 = 1;
         return M;
      }
      else {
         IntegrationPoint3d[] ipnts = getIntegrationPoints(); 
         MatrixNd M = new MatrixNd (npvals, npvals);
         for (int k=0; k<ipnts.length; k++) {
            double[] H = ipnts[k].getPressureWeights().getBuffer();
            for (int i=0; i<npvals; i++) {
               for (int j=0; j<npvals; j++) {
                  M.add (i, j, H[i]*H[j]);
               }
            }
         }
         M.invert();
         if (npvals == 2) {
            return new Matrix2d (M);
         }
         else if (npvals == 4) {
            return new Matrix4d (M);
         }
         else {
            return M;
         }
      }
   }
   
//   /** 
//    * Returns an array of vectors which is used to store the average
//    * gradient of the shape functions. This is used in incompressibility
//    * calculations. Note that this method does not actually compute the
//    * gradient.
//    * 
//    * @return array used to store average shape function gradients
//    */
//   public Vector3d[] getAvgGNx() {
//      if (myAvgGNx == null) {
//         myAvgGNx = new Vector3d[numNodes()];
//         for (int i=0; i<numNodes(); i++) {
//            myAvgGNx[i] = new Vector3d();
//         }
//      }
//      return myAvgGNx;
//   }

   /**  
    * Returns an array of MatrixBlocks to be used as constraints to make the
    * element incompressible. Note this method does not compute values for
    * these constraints; it only returns storage for them.
    *
    * <p>There is one block for each node, with each of size 3 x m, where m is
    * the number of pressure degrees-of-freedom (returned by
    * {@link #numPressureVals()}.
    * 
    * @return incompressibility constraints
    */
   public MatrixBlock[] getIncompressConstraints() {
      if (myIncompressConstraints == null) {
         int n = numNodes();
         MatrixBlock[] constraints = new MatrixBlock[n];
         for (int i=0; i<n; i++) {
            constraints[i] = MatrixBlockBase.alloc (3, numPressureVals());
         }
         myIncompressConstraints = constraints;
      }
      return myIncompressConstraints;
   }

//   /** 
//    * Zeros the incompressibility constraints for this element.
//    */
//   public void zeroIncompressConstraints() {
//      MatrixBlock[] constraints = getIncompressConstraints();
//      for (int i=0; i<numNodes(); i++) {
//         constraints[i].setZero();
//      }
//   }
//
//   /** 
//    * Adds scaled gradient values to the incompressibility constraints,
//    * for use in computing the constraints values.
//    * If G_i is the constraint for node i, then this method computes
//    * <p>
//    * G_i += H^T GNx[i]
//    * </p>
//    * where <code>H</code> is a row vector of scalar values,
//    * and <code>GNx[i]</code> is the shape function gradient for
//    * node i.
//    *
//    * @param H row vector of scalar values, whose length should be
//    * at least equal to the number of pressure DOFs.
//    * @param GNx array of shape function gradients, one for each node.
//    */
//   public void addToIncompressConstraints (double[] H, Vector3d[] GNx) {
//      
//      MatrixBlock[] constraints = getIncompressConstraints();
//      for (int i=0; i<numNodes(); i++) {
//         FemUtilities.addToIncompressConstraints (
//            constraints[i], H, GNx[i]);
//      }
//   } 

//   private Matrix3x1Block[] allocIncompressConstraints1 (int n) {
//      Matrix3x1Block[] constraints = new Matrix3x1Block[n];
//      for (int i=0; i<n; i++) {
//         constraints[i] = new Matrix3x1Block();
//      }
//      return constraints;
//   }
//
//   /**  
//    * Returns an array of Matrix3x1Blocks to be used as constraints to make the
//    * element incompressible. Note this method does not compute values for
//    * these constraints; it only returns storage for them.
//    * 
//    * @return incompressibility constraints
//    */
//   public Matrix3x1Block[] getIncompressConstraints1() {
//      if (myIncompressConstraints1 == null) {
//         myIncompressConstraints1 = allocIncompressConstraints1(numNodes());
//      }
//      return myIncompressConstraints1;
//   }

//   /** 
//    * Returns an alternate array of Matrix3x1Blocks to be used as constraints
//    * to make the element incompressible. This alternate set can be used to
//    * create a second constraint system for enforcing incompressibility.  Note
//    * this method does not compute values for these constraints; it only
//    * returns storage for them.
//    * 
//    * @return alternate set of incompressibility constraints
//    */
//   public Matrix3x1Block[] getIncompressConstraints2() {
//      if (myIncompressConstraints2 == null) {
//         myIncompressConstraints2 = allocIncompressConstraints(numNodes());
//      }
//      return myIncompressConstraints2;
//   }

   /**
    * Tests whether or not a point is inside an element.  
    * @param pnt point to check if is inside
    * @return true if point is inside the element
    */
   public boolean isInside (Point3d pnt) {
      Vector3d coords = new Vector3d();
      if (!getNaturalCoordinates (coords, pnt)) {
         // if the calculation did not converge, assume we are outside
         return false;
      }
      return coordsAreInside (coords);
   }
   
   /** 
    * Returns true if the rest position for this element results in a negative
    * Jacobian determinant for at least one integration point. This method
    * should be used sparingly since it is computes this result from scratch.
    *
    * @return true if the rest position is inverted.
    */
   public boolean isInvertedAtRest() {
      IntegrationPoint3d[] ipnts = getIntegrationPoints();
      for (int i = 0; i < ipnts.length; i++) {
         IntegrationData3d idata = new IntegrationData3d();
         
         /*
          *  code snippet from IntegrationData3d.computeRestJacobian()
          *  -- there may be better way to check "invertedness"
          */
         Matrix3d J0 = new Matrix3d();
         J0.setZero();
         for (int j = 0; j < myNodes.length; j++) {
            Vector3d pos = myNodes[j].myRest;
            Vector3d dNds = ipnts[i].GNs[j];
            J0.addOuterProduct(pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
         }
         if (idata.getInvJ0().fastInvert(J0) <= 0) {
            return true;
         }
      }
      return false;
   }      

//   public boolean getNaturalCoordinates (VectorNd coords, Point3d pnt) {
//       return false;
//    }

   public int getNumEdges() {
      int num = 0;
      int[] idxs = getEdgeIndices();
      for (int i=0; i<idxs.length; i+=(idxs[i]+1)) {
         num++;
      }
      return num;
   }

   public int getNumFaces() {
      int num = 0;
      int[] idxs = getFaceIndices();
      for (int i=0; i<idxs.length; i+=(idxs[i]+1)) {
         num++;
      }
      return num;
   }

   /**
    * Returns an array of FaceNodes3d describing a set of faces
    * associated with this element. If adjoining elements have matching
    * faces, then the elimination of repeated faces can be used to
    * generate an external mesh for the FEM. If the returned list has zero
    * length, then this method is not supported for the element in question.
    * 
    * @return list of faces for this element
    */
   public FaceNodes3d[] getFaces() {
      FaceNodes3d[] faces = new FaceNodes3d[getNumFaces()];
      int[] idxs = getFaceIndices();
      int k = 0;
      for (int i=0; i<faces.length; i++ ) {
         faces[i] = new FaceNodes3d(this, idxs[k++]);
         FemNode3d[] faceNodes = faces[i].getNodes();
         for (int j=0; j<faceNodes.length; j++) {
            faceNodes[j] = myNodes[idxs[k++]];
         }
      }
      return faces;
   }

   public FemNode3d[][] triangulateFace (FaceNodes3d face) {
      FemNode3d[] nodes = face.getNodes();
      FemNode3d[][] triangles = new FemNode3d[nodes.length-2][3];
      for (int i=0; i<triangles.length; i++) {
         setTriangle (triangles[i], nodes[0], nodes[i+1], nodes[i+2]);
      }
      return triangles;
   }

   protected void setTriangle (
      FemNode3d[] tri, FemNode3d n0, FemNode3d n1, FemNode3d n2) {
      tri[0] = n0;
      tri[1] = n1;
      tri[2] = n2;
   }

   public void computeCentroid (Vector3d centroid) {
      centroid.setZero();
      for (int i = 0; i < numNodes(); i++) {
         centroid.add (myNodes[i].getPosition());
      }
      centroid.scale (1.0 / numNodes());
   }

   /**
    * Compute position within element based on natural coordinates
    * @param pnt  populated position within element
    * @param coords natural coordinates
    */
   public void computePosition(Point3d pnt, Vector3d coords) {
      pnt.setZero();
      for (int i=0; i<numNodes(); ++i) {
         pnt.scaledAdd(getN(i, coords), myNodes[i].getPosition());
      }
   }

   public double computeCovariance (Matrix3d C) {
      double vol = 0;

      Point3d pnt = new Point3d();
      IntegrationPoint3d[] ipnts = getIntegrationPoints();
      for (int i=0; i<ipnts.length; i++) {
         IntegrationPoint3d pt = ipnts[i];
         // compute the current position of the integration point in pnt:
         pnt.setZero();
         for (int k=0; k<pt.N.size(); k++) {
            pnt.scaledAdd (pt.N.get(k), myNodes[k].getPosition());
         }
         // now get dv, the current volume at the integration point
         pt.computeJacobian (myNodes);
         double detJ = pt.getJ().determinant();
         double dv = detJ*pt.getWeight();
         C.addScaledOuterProduct (dv, pnt, pnt);
         vol += dv;
      }
      return vol;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      for (int i = 0; i < numNodes(); i++) {
         myNodes[i].getPosition().updateBounds (pmin, pmax);
      }
   }

   public abstract int numIntegrationPoints();

   public abstract double[] getIntegrationCoords ();

   public abstract double[] getNodalExtrapolationMatrix ();

   public abstract double getN (int i, Vector3d coords);

   public abstract void getdNds (Vector3d dNds, int i, Vector3d coords);
   
   public abstract IntegrationPoint3d[] getIntegrationPoints();

   public abstract IntegrationPoint3d getWarpingPoint();

   public abstract int[] getEdgeIndices();

   public abstract int[] getFaceIndices();

   private IntegrationData3d[] doGetIntegrationData() {
      IntegrationData3d[] idata = myIntegrationData;
      if (idata == null) {
         int numPnts = getIntegrationPoints().length;
         idata = new IntegrationData3d[numPnts];
         for (int i=0; i<numPnts; i++) {
            idata[i] = new IntegrationData3d();
         }
         myIntegrationData = idata;
      }
      return idata;
   }

   public IntegrationData3d[] getIntegrationData() {
      IntegrationData3d[] idata = doGetIntegrationData();
      if (!myIntegrationDataValid) {
         // compute rest Jacobians and such
         IntegrationPoint3d[] ipnts = getIntegrationPoints();
         for (int i=0; i<idata.length; i++) {
            idata[i].computeRestJacobian (ipnts[i].GNs, myNodes);
         }
         myIntegrationDataValid = true;
      }
      return idata;
   }

   public void clearState() {
      IntegrationData3d[] idata = doGetIntegrationData();
      for (int i=0; i<idata.length; i++) {
         idata[i].clearState();
      }
   }

   public IntegrationData3d getWarpingData() {
      IntegrationData3d wdata = myWarpingData;
      if (wdata == null) {
         int numPnts = getIntegrationPoints().length;
         if (numPnts == 1) {
            // then integration and warping points/data are the same
            wdata = getIntegrationData()[0];
         }
         else {
            wdata = new IntegrationData3d();
            wdata.computeRestJacobian (getWarpingPoint().GNs, myNodes);
         }
         myWarpingData = wdata;
      }
      return wdata;
   }
   
   public static IntegrationPoint3d[] createIntegrationPoints(FemElement3d exampleElem, double[] cdata) {
      int numi = cdata.length/4;
      IntegrationPoint3d[] pnts = new IntegrationPoint3d[numi];
      if (cdata.length != 4*numi) {
         throw new InternalErrorException (
            "Coordinate data length is "+cdata.length+", expecting "+4*numi);
      }
      for (int k=0; k<numi; k++) {
         pnts[k] = IntegrationPoint3d.create (
            exampleElem, cdata[k*4], cdata[k*4+1], cdata[k*4+2], cdata[k*4+3]);
         pnts[k].setNumber (k);
      }
      return pnts;
   }
   
   protected IntegrationPoint3d[] createIntegrationPoints(double[] cdata) {
      return createIntegrationPoints(this, cdata);
   }
   
   protected IntegrationPoint3d[] createIntegrationPoints() {
      int numi = numIntegrationPoints();
      IntegrationPoint3d[] pnts = new IntegrationPoint3d[numi];
      double[] cdata = getIntegrationCoords();
      if (cdata.length != 4*numi) {
         throw new InternalErrorException (
            "Coordinate data length is "+cdata.length+", expecting "+4*numi);
      }
      for (int k=0; k<numi; k++) {
         pnts[k] = IntegrationPoint3d.create (
            this, cdata[k*4], cdata[k*4+1], cdata[k*4+2], cdata[k*4+3]);
         pnts[k].setNumber (k);
      }
      return pnts;
      // Vector3d coords = new Vector3d();
      // Vector3d dNds = new Vector3d();
      // VectorNd shapeVals = new VectorNd(numNodes());
      // VectorNd pressureShapeVals = new VectorNd(numPressureVals());
      // for (int k=0; k<numi; k++) {
      //    coords.set (coordData[k*4], coordData[k*4+1], coordData[k*4+2]);
      //    pnts[k] = new IntegrationPoint3d (
      //       numNodes(), numPressureVals(), 
      //       coords.x, coords.y, coords.z, coordData[k*4+3]);
      //    for (int i=0; i<numNodes(); i++) {
      //       shapeVals.set (i, getN (i, coords));
      //       getdNds (dNds, i, coords);
      //       pnts[k].setShapeGrad (i, dNds);
      //    }
      //    for (int i=0; i<numPressureVals(); i++) {
      //       pressureShapeVals.set (i, getH (i, coords));
      //    }
      //    pnts[k].setShapeVals (shapeVals);
      //    pnts[k].setPressureShapeVals (pressureShapeVals);
      // }
      // return pnts;
   }

//   protected IntegrationPoint3d createIntegrationPoint (
//      double s0, double s1, double s2, double w) {
//
//      Vector3d coords = new Vector3d();
//      Vector3d dNds = new Vector3d();
//      VectorNd shapeVals = new VectorNd(numNodes());     
//      VectorNd pressureShapeVals = new VectorNd(numPressureVals());     
//
//      IntegrationPoint3d pnt =
//         new IntegrationPoint3d (
//            numNodes(), numPressureVals(), s0, s1, s2, w);
//      coords.set (s0, s1, s2);
//      for (int i=0; i<numNodes(); i++) {
//         shapeVals.set (i, getN (i, coords));
//         getdNds (dNds, i, coords);
//         pnt.setShapeGrad (i, dNds);
//      }
//      for (int i=0; i<numPressureVals(); i++) {
//         pressureShapeVals.set (i, getH (i, coords));
//      }
//      pnt.setShapeVals (shapeVals);
//      pnt.setPressureShapeVals (pressureShapeVals);
//      return pnt;
//   }

   /** 
    * Used to create edge nodes for quadratic elements.
    */
   protected static FemNode3d createEdgeNode (FemNode3d n0, FemNode3d n1) {
      Point3d px = new Point3d();
      px.add (n0.getPosition(), n1.getPosition());
      px.scale (0.5);
      return new FemNode3d (px);
   }   

   public void connectToHierarchy () {
      super.connectToHierarchy ();
      
      FemNode3d[] nodes = getNodes();
      for (int i = 0; i < nodes.length; i++) {
         for (int j = 0; j < nodes.length; j++) {
            nodes[i].registerNodeNeighbor(nodes[j]);
         }
         nodes[i].addElementDependency(this);
      }
      setMass(0);

      myNbrs = new FemNodeNeighbor[numNodes()][numNodes()];
      for (int i=0; i<myNodes.length; i++) {
         FemNode3d node = myNodes[i];
         int cnt = 0;
         for (FemNodeNeighbor nbr : node.getNodeNeighbors()){
            int j = getLocalNodeIndex (nbr.myNode);
            if (j != -1) {
               myNbrs[i][j] = nbr;
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

   public void invalidateRestData () {
      super.invalidateRestData();
      // will cause rest Jacobians to be recalculated
      myIntegrationDataValid = false;
      myWarpingData = null;
   }

   public void disconnectFromHierarchy () {
      myNbrs = null;

      FemNode3d[] nodes = getNodes();
      //double massPerNode = getMass()/numNodes();
      for (int i = 0; i < nodes.length; i++) {
         for (int j = 0; j < nodes.length; j++) {
            nodes[i].deregisterNodeNeighbor(nodes[j]);
         }
         // nodes[i].addMass(-massPerNode);
         nodes[i].invalidateMassIfNecessary ();  // signal dirty
         nodes[i].removeElementDependency(this);
      }

      super.disconnectFromHierarchy ();
   }

   public abstract boolean coordsAreInside (Vector3d coords);

   public boolean getMarkerCoordinates (
      VectorNd coords, Point3d pnt, boolean checkInside) {
      if (coords.size() < numNodes()) {
         throw new IllegalArgumentException (
            "coords size "+coords.size()+" != number of nodes "+numNodes());
      }
      Vector3d ncoords = new Vector3d();
      boolean converged = getNaturalCoordinates (ncoords, pnt);
      for (int i=0; i<numNodes(); i++) {
         coords.set (i, getN (i, ncoords));
      }
      if (!converged) {
         return false;
      }
      else if (checkInside) {
         return coordsAreInside (ncoords);
      }
      else {
         return true;
      }
   }
   
   public abstract double[] getNodeCoords ();

   public void getNodeCoords (Vector3d coords, int nodeIdx) {
      if (nodeIdx < 0 || nodeIdx >= numNodes()) {
         throw new IllegalArgumentException (
            "Node index must be in the range [0,"+(numNodes()-1)+"]");
      }
      double[] c = getNodeCoords();
      coords.set (c[nodeIdx*3], c[nodeIdx*3+1], c[nodeIdx*3+2]);
   }

   /**
    * Calls {@link #getNaturalCoordinates(Vector3d,Point3d,int)}
    * with a default maximum number of interations.
    * 
    * @param coords
    * Outputs the natural coordinates, and supplies (on input) an initial
    * guess as to their position.
    * @param pnt
    * A given point (in world coords)
    * @return true if the calculation converged
    */
   public boolean getNaturalCoordinates (Vector3d coords, Point3d pnt) {
      return getNaturalCoordinates (coords, pnt, /*maxIters=*/1000) >= 0;
   }

   private void computeNaturalCoordsResidual (
      Vector3d res, Vector3d coords, Point3d pnt) {

      res.setZero();
      for (int k=0; k<numNodes(); k++) {
         res.scaledAdd (
            getN (k, coords), myNodes[k].getLocalPosition(), res);
      }
      res.sub (pnt);
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
   public int getNaturalCoordinates (
      Vector3d coords, Point3d pnt, int maxIters) {

      if (!coordsAreInside(coords)) {
         // if not inside, reset coords to 0
         coords.setZero();
      }

      // if FEM is frame-relative, transform to local coords
      Point3d lpnt = new Point3d(pnt);
      if (getGrandParent() instanceof FemModel3d) {
         FemModel3d fem = (FemModel3d)getGrandParent();
         if (fem.isFrameRelative()) {
            lpnt.inverseTransform (fem.getFrame().getPose());
         }
      }

      Vector3d res = new Point3d();
      int i;

      double tol = RenderableUtils.getRadius (this) * 1e-12;
      computeNaturalCoordsResidual (res, coords, lpnt);
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
            getdNds (dNds, k, coords);
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

         // if del is very small assume we are close to the root. Assume
         // natural coordinates are generally around 1 in magnitude, so we can
         // use an absolute value for a tolerance threshold
         if (del.norm() < 1e-10) {
            //System.out.println ("1 res=" + res.norm());
            return i+1;
         }

         prevCoords.set (coords);         
         coords.sub (del);                              
         computeNaturalCoordsResidual (res, coords, lpnt);
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
            // start by limiting del to a magnitude of 1
            if (del.norm() > 1) {
               del.normalize();  
            }
            // and keep cutting the step size in half until the residual starts
            // dropping again
            double alpha = 0.5;
            while (alpha > eps && rn > prn) {
               coords.scaledAdd (-alpha, del, prevCoords);
               computeNaturalCoordsResidual (res, coords, lpnt);
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
   

   public int getNaturalCoordinatesStd (
      Vector3d coords, Point3d pnt, int maxIters) {

      if (!coordsAreInside(coords)) {
         coords.setZero();
      }
      // if FEM is frame-relative, transform to local coords
      Point3d lpnt = new Point3d(pnt);
      if (getGrandParent() instanceof FemModel3d) {
         FemModel3d fem = (FemModel3d)getGrandParent();
         if (fem.isFrameRelative()) {
            lpnt.inverseTransform (fem.getFrame().getPose());
         }
      }

      LUDecomposition LUD = new LUDecomposition();

      /*
       * solve using Newton's method: Need a good guess! Just guessed zero here.
       */
      Vector3d dNds = new Vector3d();
      Matrix3d dxds = new Matrix3d();
      Vector3d res = new Point3d();
      Vector3d del = new Point3d();
      Vector3d prevCoords = new Vector3d();
      Vector3d prevRes = new Point3d();

      int i;
      for (i = 0; i < maxIters; i++) {

         // compute the Jacobian dx/ds for the current guess
         dxds.setZero();
         res.setZero();
         for (int k=0; k<numNodes(); k++) {
            getdNds (dNds, k, coords);
            dxds.addOuterProduct (myNodes[k].getLocalPosition(), dNds);
            res.scaledAdd (
               getN (k, coords), myNodes[k].getLocalPosition(), res);
         }
         res.sub (lpnt);
         LUD.factor (dxds);
         
         double cond = LUD.conditionEstimate (dxds);
         if (cond > 1e10)
            System.err.println (
               "Warning: condition number for solving natural coordinates is "
               + cond);
         LUD.solve (del, res);
         // assume that natural coordinates are general around 1 in magnitude
         if (del.norm() < 1e-10) {
            // System.out.println ("* res=" + res.norm());
            return i+1;
         }
         
         prevCoords.set(coords);
         prevRes.set(res);
         
         // it may be that "coords + del" is a worse solution.  Let's make sure we
         // go the correct way
         // binary search suitable alpha in [0 1]
         double eps = 1e-12;
         double prn2 = prevRes.normSquared();
         double rn2 = prn2*2; // initialization
         double alpha = 1;
         del.normalize();  // if coords expected ~1, we don't want to stray too far
         
         while (alpha > eps && rn2 > prn2) {
            coords.scaledAdd (-alpha, del, prevCoords);
            res.setZero();
            for (int k=0; k<numNodes(); k++) {
               res.scaledAdd (
                  getN (k, coords), myNodes[k].getLocalPosition(), res);
            }
            res.sub (lpnt);
            
            rn2 = res.normSquared();
            alpha = alpha / 2;
         }         
         if (alpha < eps) {
            return -1;  // failed
         }
         //System.out.println ("res=" + res.norm() + " alpha=" + alpha);
      }
      //      System.out.println ("coords: " + coords.toString ("%4.3f"));
      return -1;
   }
   
   public boolean getNaturalCoordinatesOld (
      Vector3d coords, Point3d pnt, int maxIters) {

      if (!coordsAreInside(coords)) {
         coords.setZero();
      }

      // if FEM is frame-relative, transform to local coords
      Point3d lpnt = new Point3d(pnt);
      if (getGrandParent() instanceof FemModel3d) {
         FemModel3d fem = (FemModel3d)getGrandParent();
         if (fem.isFrameRelative()) {
            lpnt.inverseTransform (fem.getFrame().getPose());
         }
      }
      LUDecomposition LUD = new LUDecomposition();

      /*
       * solve using Newton's method: Need a good guess! Just guessed zero here.
       */
      Vector3d dNds = new Vector3d();
      Matrix3d dxds = new Matrix3d();
      Vector3d res = new Point3d();
      Vector3d del = new Point3d();

      int i;
      for (i = 0; i < maxIters; i++) {
//         double s1 = coords.x;
//         double s2 = coords.y;
//         double s3 = coords.z;

         // compute the Jacobian dx/ds for the current guess
         dxds.setZero();
         res.setZero();
         for (int k=0; k<numNodes(); k++) {
            getdNds (dNds, k, coords);
            dxds.addOuterProduct (myNodes[k].getLocalPosition(), dNds);
            res.scaledAdd (getN (k, coords), myNodes[k].getLocalPosition(), res);
         }
         res.sub (pnt);
         System.out.println ("res=" + res.norm());
         LUD.factor (dxds);
         double cond = LUD.conditionEstimate (dxds);
         if (cond > 1e10)
            System.err.println (
               "Warning: condition number for solving natural coordinates is "
               + cond);
         LUD.solve (del, res);
         // assume that natural coordinates are general around 1 in magnitude
         if (del.norm() < 1e-10) {
            break;
         }
         System.out.println ("iter=" + i + " del=" + del.norm());
         coords.sub (del);
      }
      //      System.out.println ("coords: " + coords.toString ("%4.3f"));

      // natural coordinates should all be between -1 and +1 iff the point is
      // inside it
      return true;
   }

//   /**
//    * Default method to get the element volume. Uses quadrature. Individual
//    * elements can override this with a more efficient method if needed.
//    */
//   public double getVolume() {
//      return computeVolume();
//   }
   
   /**
    * Default method to compute an element's volume and partial volumes.  Uses
    * quadrature. If the number of pressure values is 1, then there is only one
    * partial rest volume which is equal to the overall rest volume. The volume
    * at each quadrature point is also stored in the <code>dv</code> field of
    * the elements integration data, for possible future use.
    *
    * <p>The method should return the minimum Jacobian value found when
    * computing the volume for this element. A negative value indicates
    * element inversion.
    *
    * <p>Individual elements can override this with a more efficient 
    * method if needed.
    */
   public double computeVolumes() {
      int npvals = numPressureVals();
      
      double vol = 0;
      for (int k=0; k<npvals; k++) {
         myVolumes[k] = 0;
      }
      
      double minDetJ = Double.MAX_VALUE;

      IntegrationPoint3d[] ipnts = getIntegrationPoints();
      IntegrationData3d[] idata = getIntegrationData();
      for (int i=0; i<ipnts.length; i++) {
         IntegrationPoint3d pt = ipnts[i];
         pt.computeJacobian (myNodes);
         double detJ = pt.getJ().determinant();
         double dv = detJ*pt.getWeight();
         // normalize detJ to get true value relative to rest position
         detJ /= idata[i].getDetJ0();
         idata[i].setDv(dv);
         if (npvals > 1) {
            double[] H = pt.getPressureWeights().getBuffer();
            for (int k=0; k<npvals; k++) {
               myVolumes[k] += H[k]*dv;
            } 
         }
         if (detJ < minDetJ) {
            minDetJ = detJ;
         }
         vol += dv;
      }
      if (npvals == 1) {
         myVolumes[0] = vol;         
      }      
      myVolume = vol;
      return minDetJ;
   }

   public void computeJacobian(Vector3d s, Matrix3d J) {
      FemNode3d[] nodes = getNodes();
      J.setZero();
      Vector3d dNds = new Vector3d();
      for (int i=0; i<nodes.length; ++i) {
         getdNds(dNds, i, s);
         J.addOuterProduct(nodes[i].getLocalPosition(), dNds);
      }
   }
   
   /**
    * Volumes array for use with incompressibility 
    * @return current volumes
    */
   public double[] getVolumes() {
      return myVolumes;
   }

   public void computePressures (
      double[] pressures, IncompressibleMaterial imat) {

      int npvals = numPressureVals();
      IntegrationPoint3d[] ipnts = getIntegrationPoints();
      IntegrationData3d[] idata = getIntegrationData();
      for (int k=0; k<npvals; k++) {
         pressures[k] = 0;
      }
      for (int i=0; i<ipnts.length; i++) {
         double dV = idata[i].getDetJ0()*ipnts[i].getWeight();
         IntegrationPoint3d pt = ipnts[i];
         pt.computeJacobian (myNodes);
         double detJ = pt.getJ().determinant();
         double[] H = pt.getPressureWeights().getBuffer();
         for (int k=0; k<npvals; k++) {
            pressures[k] += H[k]*dV*imat.getEffectivePressure (detJ);
         }            
      }
      
   }

   /**
    * Lagrange pressures array for use with incompressibility 
    * @return pressures
    */
   public double[] getLagrangePressures() {
      return myLagrangePressures;
   }
   
   /**
    * Default method to compute the element rest volume and partial volumes.
    * Uses quadrature. If the number of pressure values is 1, then there is
    * only one partial rest volume which is equal to the overall rest
    * volume. Individual elements can override this with a more efficient
    * method if needed.
    */
   public double computeRestVolumes() {
      int npvals = numPressureVals();

      double vol = 0;
      for (int k=0; k<npvals; k++) {
         myRestVolumes[k] = 0;
      }
      
      IntegrationPoint3d[] ipnts = getIntegrationPoints();
      IntegrationData3d[] idata = getIntegrationData();
      for (int i=0; i<ipnts.length; i++) {
         double dV = idata[i].getDetJ0()*ipnts[i].getWeight();
         if (npvals > 1) {
            double[] H = ipnts[i].getPressureWeights().getBuffer();
            for (int k=0; k<npvals; k++) {
               myRestVolumes[k] += H[k]*dV;
            }
         }
         vol += dV;
      }
      if (npvals == 1) {
         myRestVolumes[0] = vol;         
      }
      return vol;
   }
   
   /**
    * Volumes array for use with incompressibility 
    * @return rest volumes
    */
   public double[] getRestVolumes() {
      return myRestVolumes;
   }

//    /** 
//     * Returns the average stress that has been computed for this element.
//     * 
//     * @return average stress (must not be modified).
//     */
//    public SymmetricMatrix3d getStress() {
//       return myAvgStress;
//    }

   public void addNodeStiffness (
      Matrix3d Kij, int i, int j, boolean corotated) {
      if (!myWarpingStiffnessValidP) {
         updateWarpingStiffness();
      }
      myWarper.addNodeStiffness (Kij, i, j, corotated);
   }  

   public void addNodeStiffness (
      int i, int j, boolean corotated) {
      if (!myWarpingStiffnessValidP) {
         updateWarpingStiffness();
      }
      myWarper.addNodeStiffness (myNbrs[i][j], i, j, corotated);
   }  

    public void addNodeForce (Vector3d f, int i, boolean corotated) {
      if (!myWarpingStiffnessValidP) {
         updateWarpingStiffness();
      }
      myWarper.addNodeForce (f, i, myNodes, corotated);
   }
    
   public void addNodeForce0(Vector3d f, int i, boolean corotated) {
      if (!myWarpingStiffnessValidP) {
         updateWarpingStiffness();
      }
      myWarper.addNodeForce0(f, i, corotated);
   }
   
   public void addNodeForce0(VectorNd f, int offset, int i, boolean corotated) {
      if (!myWarpingStiffnessValidP) {
         updateWarpingStiffness();
      }
      myWarper.addNodeForce0(f, offset, i, corotated);
   }

   public StiffnessWarper3d getStiffnessWarper() {
      if (myWarper == null){
         myWarper = new StiffnessWarper3d (numNodes());
      }
      return myWarper;
   }
   
   public void updateWarpingStiffness() {
      // System.out.println("updating stiffness: E="+myE+", nu="+myNu);

      FemMaterial mat = getEffectiveMaterial();
      if (mat instanceof LinearMaterial) {
         if (myWarper == null){
            myWarper = new StiffnessWarper3d (numNodes());
         }
         LinearMaterial lmat = (LinearMaterial)mat;
         myWarper.computeInitialStiffness (
            this, lmat.getYoungsModulus(), lmat.getPoissonsRatio());
//         IntegrationPoint3d wpnt = getWarpingPoint();
//         IntegrationData3d wdata = getWarpingData();
//         wdata.computeRestJacobian (wpnt.GNs, myNodes);
      }
      myWarpingStiffnessValidP = true;
   }

   /**
    * Queries if the effective material for this element, and all auxiliary
    * materials, are defined for non-positive deformation gradients.
    *
    * @return <code>true</code> if the materials associated with this
    * element are invertible
    */
   public boolean materialsAreInvertible() {
      
      FemMaterial mat = getEffectiveMaterial();
      if (!mat.isInvertible()) {
         return false;
      }
      if (myAuxMaterials != null) {
         for (int i=0; i<myAuxMaterials.size(); i++) {
            if (!myAuxMaterials.get(i).isInvertible()) {
               return false;
            }
         }
      }
      return true;
   }

//   public void computeWarping() {
//      if (!myWarpingStiffnessValidP) {
//         updateWarpingStiffness();
//      }
//      IntegrationPoint3d wpnt = getWarpingPoint();
//      IntegrationData3d wdata = getWarpingData();
//      wpnt.computeJacobianAndGradient (myNodes, wdata.myInvJ0);
//      myWarper.computeRotation (wpnt.F, null);
//   }

   public void computeWarping (Matrix3d F, SymmetricMatrix3d P) {
      if (myWarpingStiffnessValidP && myWarper == null) {
         System.out.println ("invalid");
      }
      if (!myWarpingStiffnessValidP) {
         updateWarpingStiffness();
      }
      myWarper.computeRotation (F, P);
   }

   public abstract void renderWidget (
      Renderer renderer, double size, RenderProps props);

   public double computeDirectedRenderSize (Vector3d dir) {
      IntegrationPoint3d ipnt = getWarpingPoint();
      return ipnt.computeDirectedSizeForRender (dir, getNodes());
   }

   /**
    * Computes the current coordinates and deformation gradient at the
    * warping point, using render coordinates. This is used in element
    * rendering.
    *
    * @param F returns the deformation gradient
    * @param coords returns the current coordinates
    */
   public void computeRenderCoordsAndGradient (Matrix3d F, float[] coords) {
      IntegrationPoint3d ipnt = getWarpingPoint();
      IntegrationData3d idata = getWarpingData();
      ipnt.computeGradientForRender (F, getNodes(), idata.myInvJ0);
      ipnt.computeCoordsForRender (coords, getNodes());
   }

   protected int getNodeIndex (FemNode3d n) {
      for (int i=0; i<myNodes.length; i++) {
         if (myNodes[i] == n) {
            return i;
         }
      }
      return -1;
   }

   protected boolean containsNode (FemNode3d n) {
      return getNodeIndex(n) != -1;
   }

   public boolean hasEdge (FemNode3d n0, FemNode3d n1) {
      return false;
   }

   public boolean hasFace (FemNode3d n0, FemNode3d n1, FemNode3d n2) {
      return false;
   }

   public boolean hasFace (FemNode3d n0, FemNode3d n1,
                           FemNode3d n2, FemNode3d n3) {
      return false;
   }

   // Auxiliary materials, used for implementing muscle fibres
   public void addAuxiliaryMaterial (AuxiliaryMaterial mat) {
      if (myAuxMaterials == null) {
         myAuxMaterials = new ArrayList<AuxiliaryMaterial>(4);
      }
      myAuxMaterials.add (mat);
   }

   public boolean removeAuxiliaryMaterial (AuxiliaryMaterial mat) {
      if (myAuxMaterials != null) {
         return myAuxMaterials.remove (mat);
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
         return myAuxMaterials.toArray (new AuxiliaryMaterial[0]);
      }
   }

   static int numEdgeSegs = 10;

   /**
    * Support methods for computing the extrapolants that map values at an
    * element's integration points back to its nodes. These are used to
    * determine the extrapolation matrix that is returned by the element's
    * getNodalExtroplationMatrix() method. Extrapolation from integration
    * points to nodes is used to compute things such as nodal stresses.
    *
    * <p> In general, the extrapolants are determined by mapping the integration
    * points (or a suitable subset thereof) onto a special finite element of
    * their own, and computing the shape function values for the nodes
    * in the new coordinate system.
    */
   /** 
    * Returns scaled values for the nodal coordinates of this element as an
    * array of Vector3d. An optional offset value can be added to the values as
    * well.
    */
   protected Vector3d[] getScaledNodeCoords (double s, Vector3d offset) {
      int nn = numNodes();
      Vector3d[] array = new Vector3d[nn];
      double[] coords = getNodeCoords();
      for (int i=0; i<array.length; i++) {
         array[i] = new Vector3d (coords[i*3], coords[i*3+1], coords[i*3+2]);
         array[i].scale (s);
         if (offset != null) {
            array[i].add (offset);
         }
      }
      return array;
   }

   /** 
    * Creates a node extrapolation matrix from a set of (transformed) nodal
    * coordinate values. The size of this matrix is n x m, where n is the
    * number of coordinate values and m is the number of integration points for
    * the element. The extrapolation is based on the shape functions of a
    * specified sub-element. If the number of sub-element nodes is less than m,
    * the remaining matrix elements are set to 0.
    */
   protected double[] createNodalExtrapolationMatrix (
      Vector3d[] ncoords, int m, FemElement3d subelem) {
      
      int n = ncoords.length;
      double[] mat = new double[n*m];
      for (int i=0; i<n; i++) {
         for (int j=0; j<m; j++) {
            if (j < subelem.numNodes()) {
               mat[i*m+j] = subelem.getN (j, ncoords[i]);
            }
            else {
               mat[i*m+j] = 0;
            }
         }
      }
      return mat;
   }

   @Override
   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myVolume *= (s * s * s);
      myRestVolume *= (s * s * s);
      for (int i=0; i<numPressureVals(); i++) {
         myLagrangePressures[i] /= s;
      }
      //myLagrangePressure /= s;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      for (int i=0; i<numNodes(); i++) {
         if (!ComponentUtils.addCopyReferences (refs, myNodes[i], ancestor)) {
            return false;
         }
      }
      return true;
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
      IntegrationData3d[] idata = doGetIntegrationData();
      for (int i=0; i<idata.length; i++) {
         idata[i].myFrame = F;
      }
   }

   public Matrix3d getFrame() {
      IntegrationData3d[] idata = doGetIntegrationData();
      return idata[0].getFrame();
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "frame")) {
         Matrix3d M = new Matrix3d();
         M.scan (rtok);
         setFrame (M);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }      

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      Matrix3dBase M = getFrame();
      if (M != null) {
         pw.println ("frame=[");
         IndentingPrintWriter.addIndentation (pw, 2);
         M.write (pw, fmt);
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }   

   @Override
   public FemElement3d copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FemElement3d e = (FemElement3d)super.copy (flags, copyMap);

      e.myNodes = new FemNode3d[numNodes()];
      for (int i=0; i<numNodes(); i++) {
         FemNode3d n = myNodes[i];
         FemNode3d newn = (FemNode3d)ComponentUtils.maybeCopy (flags, copyMap, n);
         if (newn == null) {
            throw new InternalErrorException (
               "No duplicated node found for node number "+n.getNumber());
         }
         e.myNodes[i] = newn;
      }
      e.myNbrs = null;
      //e.myAvgGNx = null;
      e.myIncompressConstraints = null;
      //e.myIncompressConstraints1 = null;
//      e.myIncompressConstraints2 = null;
   
      // Note that frame information is not presently duplicated
      e.myIntegrationData = null;
      e.myIntegrationDataValid = false;

      //protected SymmetricMatrix3d myAvgStress = new SymmetricMatrix3d();
      e.myLagrangePressures = new double[numPressureVals()];
      //e.myLagrangePressure = 0;
      e.myWarper = null;
      e.myIncompressIdx = -1;
      //e.myLocalIncompressIdx = -1;

      e.setElementWidgetSizeMode (myElementWidgetSizeMode);
      if (myElementWidgetSizeMode == PropertyMode.Explicit) {
         e.setElementWidgetSize (myElementWidgetSize);
      }

      e.myAuxMaterials = null;
      if (myAuxMaterials != null) {
         for (AuxiliaryMaterial a : myAuxMaterials) {
            try {
               e.addAuxiliaryMaterial ((AuxiliaryMaterial)a.clone());
            }
            catch (Exception ex) {
               throw new InternalErrorException (
                  "Can't clone " + a.getClass());
            }
            
         }
      }

      return e;
   }


   public VectorNd computeGravityWeights () {

      VectorNd weights = new VectorNd (numNodes());
      IntegrationPoint3d[] ipnts = getIntegrationPoints();       
      double wtotal = 0;
      for (int k=0; k<ipnts.length; k++) {
         IntegrationPoint3d pnt = ipnts[k];
         VectorNd N = pnt.getShapeWeights();
         double w = pnt.getWeight();
         for (int i=0; i<numNodes(); i++) {
            weights.add (i, w*N.get(i));
         }
         wtotal += w;
      }
      weights.scale (1/wtotal);
      return weights;
   }
   
   
   public static FemElement3d createElement(FemNode3d[] nodes, boolean flipped) {
      
      if (flipped) {
         switch(nodes.length) {
            case 4:
               return new TetElement(nodes[0], nodes[2], nodes[1], nodes[3]);
            case 5:
               return new PyramidElement(nodes[0], nodes[3], nodes[2], nodes[1], nodes[4]);
            case 6:
               return new WedgeElement(nodes[0], nodes[2], nodes[1], nodes[3], nodes[5], nodes[4]);
            case 8:
               return new HexElement(nodes[0], nodes[3], nodes[2], nodes[1], 
                  nodes[4], nodes[7], nodes[6], nodes[5]);
            default:
               throw new IllegalArgumentException(
                  "Unknown element type with " + nodes.length + " nodes");
         }
      } else {
         return createElement(nodes);
      }
   }
   
   public static FemElement3d createElement(FemNode3d[] nodes) {
      
      switch(nodes.length) {
         case 4:
            return new TetElement(nodes);
         case 5:
            return new PyramidElement(nodes);
         case 6:
            return new WedgeElement(nodes);
         case 8:
            return new HexElement(nodes);
         default:
            throw new IllegalArgumentException(
               "Unknown element type with " + nodes.length + " nodes");
      }
      
   }

   public PointAttachment createPointAttachment (
      Point pnt) {
      if (pnt.isAttached()) {
         throw new IllegalArgumentException ("point is already attached");
      }
      if (!(pnt instanceof Particle)) {
         throw new IllegalArgumentException ("point is not a particle");
      }
      if (ComponentUtils.isAncestorOf (getGrandParent(), pnt)) {
         throw new IllegalArgumentException (
            "Element's FemModel is an ancestor of the point");
      }
      PointFem3dAttachment ax = new PointFem3dAttachment (pnt);
      ax.setFromElement (pnt.getPosition(), this);
      ax.updateAttachment();
      return ax;
   }
   /**
    * {@inheritDoc}
    */
   public FrameFem3dAttachment createFrameAttachment (
      Frame frame, RigidTransform3d TFW) {

      if (frame == null && TFW == null) {
         throw new IllegalArgumentException (
            "frame and TFW cannot both be null");
      }
      if (frame != null && frame.isAttached()) {
         throw new IllegalArgumentException ("frame is already attached");
      }
      FrameFem3dAttachment ffa = new FrameFem3dAttachment (frame);
      if (!ffa.setFromElement (TFW, this)) {
         return null;
      }
      if (frame != null) {
         if (DynamicAttachment.containsLoop (ffa, frame, null)) {
            throw new IllegalArgumentException (
               "attachment contains loop");
         }
      }
      return ffa;
   }

   // implementation of IndexedPointSet

   public int numPoints() {
      return numNodes();
   }

   public Point3d getPoint (int idx) {
      return myNodes[idx].getPosition();
   }
   
}

