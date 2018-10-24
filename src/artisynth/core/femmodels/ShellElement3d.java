package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;

import artisynth.core.femmodels.FemElement;
import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.IntegrationData3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.materials.FemMaterial;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.util.ScanToken;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.geometry.Boundable;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.util.InternalErrorException;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Base class for a shell element. Compared to traditional elements, 
 * shell elements are infinitely-thin elements that can better model surfaces.
 * Examples include water surfaces, living tissue, clothing, and aluminium sheet.
 */
public abstract class ShellElement3d extends FemElement3dBase
   implements Boundable {
   
   // the warping point is an integration point at the center of the element,
   // used for corotated linear behavior and other things
   protected IntegrationData3d myWarpingData;
   protected StiffnessWarper3d myWarper = null;
   protected ElementType myType;
   
   // per-element integration point data
   protected IntegrationData3d[] myIntegrationData = null;
   protected boolean myIntegrationDataValid = false;
   
   protected double myDefaultThickness = 0.01; 
   
   private static double DEFAULT_ELEMENT_WIDGET_SIZE = 0.0;
   protected double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
   protected PropertyMode myElementWidgetSizeMode = PropertyMode.Inherited;
   
   protected static FemElementRenderer myRenderer;
   
   public static PropertyList myProps =
      new PropertyList (ShellElement3d.class, FemElement.class);
   
   static {
      myProps.addInheritable (
         "elementWidgetSize:Inherited",
         "size of rendered widget in each element's center",
         DEFAULT_ELEMENT_WIDGET_SIZE, "[0,1]");
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ShellElement3d() {
   }

   public ElementType getType() {
      return myType;
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

   public abstract double[] getNodeCoords ();
   
   public void getNodeCoords (Vector3d coords, int nodeIdx) {
      if (nodeIdx < 0 || nodeIdx >= numNodes()) {
         throw new IllegalArgumentException (
            "Node index must be in the range [0,"+(numNodes()-1)+"]");
      }
      double[] c = getNodeCoords();
      coords.set (c[nodeIdx*3], c[nodeIdx*3+1], c[nodeIdx*3+2]);
   }

   /* --- Integration points and data --- */

   public abstract IntegrationPoint3d getWarpingPoint();

   public IntegrationData3d getWarpingData() {
      IntegrationData3d wdata = myWarpingData;
      if (wdata == null) {
         wdata = new IntegrationData3d();
         wdata.computeInverseRestJacobian (getWarpingPoint(), getNodes());
         myWarpingData = wdata;
      }
      return wdata;
   }

   public abstract double[] getIntegrationCoords ();

   public abstract IntegrationPoint3d[] getIntegrationPoints ();

   protected IntegrationPoint3d[] createIntegrationPoints (
      double[] integCoords) {
      int numi = integCoords.length/4;
      IntegrationPoint3d[] pnts = new IntegrationPoint3d[numi];
      if (integCoords.length != 4*numi) {
         throw new InternalErrorException (
            "Coordinate data length is "+integCoords.length+","
            + " expecting "+4*numi);
      }
      for (int k=0; k<numi; k++) {
         pnts[k] = IntegrationPoint3d.create (
            this, integCoords[k*4], integCoords[k*4+1], integCoords[k*4+2],
            integCoords[k*4+3]);
         pnts[k].setNumber (k);
      }
      return pnts;
   }

   protected IntegrationPoint3d[] createMembraneIntegrationPoints (
      IntegrationPoint3d[] ipnts) {
      // assume that the membrane integration points are simply the first nump
      // integration points, where nump = numPlanarIntegrationPoints()
      int nump = numPlanarIntegrationPoints();
      IntegrationPoint3d[] mpnts = new IntegrationPoint3d[nump];
      for (int i=0; i<nump; i++) {
         mpnts[i] = ipnts[i];
      }
      return mpnts;
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

   /**
    * Number of integration points in the shell plane.
    */
   public abstract int numPlanarIntegrationPoints();

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
   
   // /** 
   //  * Set reference frame information for this element. This can be used for
   //  * computing anisotropic material parameters. In principle, each integration
   //  * point can have its own frame information, but this method simply sets the
   //  * same frame information for all the integration points, storing it in each
   //  * IntegrationData structure. Setting <code>M</code> to <code>null</code>
   //  * removes the frame information.
   //  * 
   //  * @param M frame information (is copied by the method)
   //  */
   // public void setFrame (Matrix3dBase M) {
   //    Matrix3d F = null;
   //    if (M != null) {
   //       F = new Matrix3d (M);
   //    }
   //    IntegrationData3d[] idata = doGetIntegrationData();
   //    for (int i=0; i<idata.length; i++) {
   //       idata[i].myFrame = F;
   //    }
   // }

   // public Matrix3d getFrame() {
   //    IntegrationData3d[] idata = doGetIntegrationData();
   //    return idata[0].getFrame();
   // }

   public void invalidateRestData () {
      super.invalidateRestData();
      // will cause rest Jacobians to be recalculated
      myIntegrationDataValid = false;
      myWarpingData = null;
   }
   
   /* --- coordinates --- */

   public int getNaturalCoordinates (Vector3d coords, Point3d pnt, int maxIters) {
      throw new RuntimeException("Unimplemented");
   }

   /* --- Stiffness warping --- */
   
   /**
    * Retrieves the current stiffness warper.  The warper's
    * cached rest stiffness is updated if necessary
    * 
    * @return stiffness warper
    */
   public StiffnessWarper3d getStiffnessWarper () {
      // don't allow invalid stiffness to leak
      if (!myWarpingStiffnessValidP) {
         updateWarpingStiffness(/*weight=*/1.0);
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

   /* --- Volume --- */

   @Override
   public double computeVolumes () {
      return doComputeVolume (/* isRest= */false);
   }

   @Override
   public double computeRestVolumes () {
      return doComputeVolume (/* isRest= */true);
   }
   
   public double doComputeVolume (boolean isRest) {
      double vol = 0;

      // For each integration point...
      IntegrationPoint3d[] ipnts = getIntegrationPoints ();
      IntegrationData3d[] idata= getIntegrationData ();
      int nump = numIntegrationPoints();
      for (int i = 0; i < nump; i++) {
         double detJ;
         if (isRest) {
            detJ = idata[i].getDetJ0();
         }
         else {
            detJ = ipnts[i].computeJacobianDeterminant(getNodes());
         }
         vol += detJ*ipnts[i].getWeight();
      }
      if (myType == ElementType.MEMBRANE) {
         vol *= myDefaultThickness;
      }
      return vol;
   }

   /* --- Thickness and directors --- */
   
   public double getDefaultThickness() {
      return myDefaultThickness;
   }
   
   public void setDefaultThickness(double newThickness) {
      myDefaultThickness = newThickness;
      
      // Update static dependencies that depend on knowing the shell thickness.
      
      computeRestDirectors ();
      //updateCoContraVectors ();
      invalidateRestData ();
   }
   
   /**
    * Update the rest director of each node.
    * 
    * This should be called whenever node.myAdjElements is updated or 
    * shell thickness is modified, both which the rest director depends on.
    */
   public void computeRestDirectors() {
      for (FemNode3d n : myNodes) {
         if (n.hasDirector()) {
            n.computeRestDirector ();
         }
      }
   }
   
   /* --- Edges and Faces --- */
   
   public abstract int[] getEdgeIndices ();

   public abstract int[] getFaceIndices ();
   
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
   
   /* --- Extrpolation matrices --- */

   public abstract double[] getNodalExtrapolationMatrix();
   
   /**
    * Create a matrix to map data from the nodes to the integration points.
    * 
    * This is the inverse of getNodalExtrapolationMatrix().
    * 
    * @return
    * N-by-K matrix where N is the number of nodes and K is the number of 
    * integration points of this element.
    */
   public MatrixNd getIntegExtrapolationMatrix() {
      int numNodes = myNodes.length;
      int numIntegPts = numIntegrationPoints();
      
      MatrixNd shapeAtIntegMtx = new MatrixNd(numNodes, numIntegPts);
      for (int n = 0; n < numNodes; n++) {
         for (int k = 0; k < numIntegPts; k++) {
            IntegrationPoint3d iPt = getIntegrationPoints()[k];
            double shapeFunc = getN(n, iPt.getCoords ()) ;
            shapeAtIntegMtx.set (n,k, shapeFunc);
         }
      }
      
      return shapeAtIntegMtx;
   }

   public void clearState() {
      IntegrationData3d[] idata = doGetIntegrationData();
      for (int i=0; i<idata.length; i++) {
         idata[i].clearState();
      }
   }
   
   /* --- Geometry --- */

   /**
    * Computes the shell element normal, with respect to rest coordinates, at a
    * specific node, and returns the (cross product) area associated with that
    * normal. This is used for automatically initializing directors.
    */
   public abstract double computeRestNodeNormal (Vector3d nrm, FemNode3d node);
   
   /**
    * Computes a normal for three points oriented counter-clockwise
    * and returns the area of the associated triangle.
    */
   protected double computeNormal (
      Vector3d nrm, Point3d p0, Point3d p1, Point3d p2) {
      Vector3d d01 = new Vector3d();
      Vector3d d02 = new Vector3d();
      d01.sub (p1, p0);
      d02.sub (p2, p0);
      nrm.cross (d01, d02);
      double mag = nrm.norm();
      if (mag != 0) {
         nrm.scale (1/mag);
      }
      return mag/2;
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
      return mat.isInvertible();
   }
   
   
   /* --- Hierarchy --- */
   
   public void connectToHierarchy () {
      super.connectToHierarchy ();
      
      FemNode3d[] nodes = getNodes();
      // add element dependency first, so that directors will be enabled
      // for the each node and hence also for the node neighbors
      for (int i = 0; i < nodes.length; i++) {
         for (int j = 0; j < nodes.length; j++) {
            nodes[i].registerNodeNeighbor(
               nodes[j], /*shell=*/myType==ElementType.SHELL);
         }
         nodes[i].addElementDependency(this);
      }
      invalidateRestDirectors();
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
   
   public void disconnectFromHierarchy () {
      myNbrs = null;

      FemNode3d[] nodes = getNodes();
      //double massPerNode = getMass()/numNodes();
      for (int i = 0; i < nodes.length; i++) {
         for (int j = 0; j < nodes.length; j++) {
            nodes[i].deregisterNodeNeighbor (
               nodes[j], /*shell=*/myType==ElementType.SHELL);
         }
         // nodes[i].addMass(-massPerNode);
         nodes[i].invalidateMassIfNecessary ();  // signal dirty
         nodes[i].removeElementDependency(this);
      }
      invalidateRestDirectors();

      super.disconnectFromHierarchy ();
   }
   
   
   /* --- Collision Box ---*/
   
   public void computeCentroid (Vector3d centroid) {
      throw new RuntimeException("computeCentroid() :: unimplemented.");
   }

   public double computeCovariance (Matrix3d C) {
      throw new RuntimeException("computeCovariance() :: unimplemented.");
   }

   /**
    * Tests whether or not a point is inside an element.  
    * @param pnt point to check if is inside
    * @return true if point is inside the element
    */
   public boolean isInside (Point3d pnt) {
      return false;
   }
   
   public abstract double nearestPoint (Point3d near, Point3d pnt);

   /* --- Scanning, writing and copying --- */
   
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "type")) {
         myType = rtok.scanEnum(ElementType.class);
         return true;
      }
      else if (scanAttributeName (rtok, "defaultThickness")) {
         myDefaultThickness = rtok.scanNumber();
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }      

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println ("type=" + myType);
      pw.println ("defaultThickness=" + fmt.format(myDefaultThickness));
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

   public ShellElement3d copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      ShellElement3d e = (ShellElement3d)super.copy (flags, copyMap);
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
   
      // Note that frame information is not presently duplicated
      e.myIntegrationData = null;
      e.myIntegrationDataValid = false;

      e.myWarper = null;

      e.setElementWidgetSizeMode (myElementWidgetSizeMode);
      if (myElementWidgetSizeMode == PropertyMode.Explicit) {
         e.setElementWidgetSize (myElementWidgetSize);
      }
      return e;
   }

   void invalidateRestDirectors() {
      for (FemNode3d n : myNodes) {
         n.invalidateRestDirectorIfNecessary();
      }
   }

   /* --- Misc Methods --- */
}
