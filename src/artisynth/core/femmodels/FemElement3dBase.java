package artisynth.core.femmodels;

import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;
import artisynth.core.util.ScanToken;
import artisynth.core.modelbase.*;
import artisynth.core.materials.*;

public abstract class FemElement3dBase extends FemElement {
   
   // the warping point is an integration point at the center of the element,
   // used for corotated linear behavior and other things
   protected IntegrationData3d myWarpingData;
   protected StiffnessWarper3d myWarper = null;

    // per-element integration point data
   protected IntegrationData3d[] myIntegrationData;
   protected boolean myIntegrationDataValid = false;

   protected static double DEFAULT_ELEMENT_WIDGET_SIZE = 0.0;
   protected double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
   protected PropertyMode myElementWidgetSizeMode = PropertyMode.Inherited;

   protected FemNode3d[] myNodes;
   protected FemNodeNeighbor[][] myNbrs = null;

   public static PropertyList myProps =
      new PropertyList (FemElement3dBase.class, FemElement.class);

   static {
      myProps.addInheritable (
         "elementWidgetSize:Inherited",
         "size of rendered widget in each element's center",
         DEFAULT_ELEMENT_WIDGET_SIZE, "[0,1]");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /* --- Element Widget Size --- */

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

   /* --- Nodes and neighbors --- */

   public abstract double[] getNodeCoords ();

   public void getNodeCoords (Vector3d coords, int nodeIdx) {
      if (nodeIdx < 0 || nodeIdx >= numNodes()) {
         throw new IllegalArgumentException (
            "Node index must be in the range [0,"+(numNodes()-1)+"]");
      }
      double[] c = getNodeCoords();
      coords.set (c[nodeIdx*3], c[nodeIdx*3+1], c[nodeIdx*3+2]);
   }

   @Override
   public FemNode3d[] getNodes() {
      return myNodes;
   }
   
   public FemNodeNeighbor[][] getNodeNeighbors() {
      return myNbrs;
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

   /* --- Integration points and data --- */

   /**
    * Returns the natural coordinates and weights for the default integration
    * points associated with this element. The information is returned as an
    * array of length {@code 4*num}, where {@code num} is the number of
    * coordinates, and the information for each coordinate consists of a
    * 4-tuple giving the three natural coordinates followed by the weight.
    *
    * @return coordinates and weights for the default integration points.
    */
   public abstract double[] getIntegrationCoords();

   public abstract IntegrationPoint3d[] getIntegrationPoints();

   /**
    * Create a set of integration points for a given element type.
    *
    * @param sampleElem representative element type
    * @param ncoords location of the integration points in natural coordinates
    * @return created integration points 
    */
   public static IntegrationPoint3d[] createIntegrationPoints (
      FemElement3dBase sampleElem, double[] ncoords) {
      int numi = ncoords.length/4;
      IntegrationPoint3d[] pnts = new IntegrationPoint3d[numi];
      if (ncoords.length != 4*numi) {
         throw new InternalErrorException (
            "Coordinate data length is "+ncoords.length+", expecting "+4*numi);
      }
      for (int k=0; k<numi; k++) {
         pnts[k] = IntegrationPoint3d.create (
            sampleElem,
            ncoords[k*4], ncoords[k*4+1], ncoords[k*4+2], ncoords[k*4+3]);
         pnts[k].setNumber (k);
      }
      return pnts;
   }

   /**
    * Create a set of integration points for this element type, using
    * the natural coordinates returned by {@link #getIntegrationCoords}.
    *
    * @return created integration points 
    */   
   protected IntegrationPoint3d[] createIntegrationPoints () {
      return createIntegrationPoints (this, getIntegrationCoords());
   }

   /**
    * Create a set of integration points for this element type, using
    * a specified set of natural coordinates.
    *
    * @param ncoords location of the integration points in natural coordinates
    * @return created integration points 
    */   
   protected IntegrationPoint3d[] createIntegrationPoints (double[] ncoords) {
      return createIntegrationPoints (this, ncoords);
   }

   public IntegrationData3d[] getIntegrationData() {
      IntegrationData3d[] idata = doGetIntegrationData();
      if (!myIntegrationDataValid) {
         // compute rest Jacobians and such
         IntegrationPoint3d[] ipnts = getIntegrationPoints();
         for (int i=0; i<idata.length; i++) {
            idata[i].computeInverseRestJacobian (ipnts[i], getNodes());
         }
         myIntegrationDataValid = true;
      }
      return idata;
   }

   protected IntegrationData3d[] doGetIntegrationData() {
      IntegrationData3d[] idata = myIntegrationData;
      if (idata == null) {
         int numPnts = numIntegrationPoints();
         idata = new IntegrationData3d[numPnts];
         for (int i=0; i<numPnts; i++) {
            idata[i] = new IntegrationData3d();
         }
         myIntegrationData = idata;
      }
      return idata;
   }

   public void invalidateRestData () {
      super.invalidateRestData();
      // will cause rest Jacobians to be recalculated
      myIntegrationDataValid = false;
      myWarpingData = null;
   }

   /* --- Stiffness warping --- */

   public abstract IntegrationPoint3d getWarpingPoint();

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
            wdata.computeInverseRestJacobian (getWarpingPoint(), myNodes);
         }
         myWarpingData = wdata;
      }
      return wdata;
   }
   
   /**
    * Explicitly sets a stiffness warper
    * @param warper new stiffness warper to use
    */
   public void setStiffnessWarper(StiffnessWarper3d warper) {
      myWarper = warper;
      myWarpingStiffnessValidP = false;
   }
   
   /**
    * Retrieves the current stiffness warper.  The warper's
    * cached rest stiffness is updated if necessary
    * @param weight meta weighting to be applied to integration points
    * when updating the rest stiffness. Default value should be 1.0. 
    * @return stiffness warper
    */
   public StiffnessWarper3d getStiffnessWarper(double weight) {
      // don't allow invalid stiffness to leak
      if (!myWarpingStiffnessValidP) {
         updateWarpingStiffness(weight);
      }
      return myWarper;
   }

   protected StiffnessWarper3d createStiffnessWarper () {
      return new StiffnessWarper3d (this);
   }

   protected void updateWarpingStiffness(double weight) {
      FemMaterial mat = getEffectiveMaterial();
      if (myWarper == null){
         myWarper = createStiffnessWarper();
      } else {
         myWarper.initialize(this);
      }
      
      if (mat.isLinear()) {
         myWarper.addInitialStiffness (this, mat, weight);
      }
      myWarpingStiffnessValidP = true;
   }
   
   /* --- Edges and Faces --- */

   public abstract int[] getFaceIndices();

   public abstract int[] getEdgeIndices();

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

   public int getNumFaces() {
      int num = 0;
      int[] idxs = getFaceIndices();
      for (int i=0; i<idxs.length; i+=(idxs[i]+1)) {
         num++;
      }
      return num;
   }

   /* --- Shape functions and coordinates --- */

   public abstract double getN (int i, Vector3d coords);

   public abstract void getdNds (Vector3d dNds, int i, Vector3d coords);

   /* --- Scanning, writing and copying --- */

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
   
}
