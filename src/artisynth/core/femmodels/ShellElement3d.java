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
   
   protected ElementClass myElementClass;
   protected double myDefaultThickness = 0.01; 
   
   protected static FemElementRenderer myRenderer;
   
   public ShellElement3d() {
   }

   public ElementClass getElementClass() {
      return myElementClass;
   }
   
   /* --- Integration points and data --- */

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
   
   /**
    * Number of integration points in the shell plane.
    */
   public abstract int numPlanarIntegrationPoints();
   
   /* --- coordinates --- */

   public int getNaturalCoordinates (Vector3d coords, Point3d pnt, int maxIters) {
      throw new RuntimeException("Unimplemented");
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
      if (myElementClass == ElementClass.MEMBRANE) {
         // for membrane elements, we need to explicitly scale the volume
         // by the thickness because the deformation gradient contains
         // no scaling in the normal direction.
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
               nodes[j], /*shell=*/myElementClass==ElementClass.SHELL);
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
               nodes[j], /*shell=*/myElementClass==ElementClass.SHELL);
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
      if (scanAttributeName (rtok, "elementClass")) {
         myElementClass = rtok.scanEnum(ElementClass.class);
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
      pw.println ("elementClass=" + myElementClass);
      pw.println ("defaultThickness=" + fmt.format(myDefaultThickness));
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
