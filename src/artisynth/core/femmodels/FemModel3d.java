/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import maspack.geometry.AABBTree;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.BVNode;
import maspack.geometry.BVTree;
import maspack.geometry.Boundable;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.DenseMatrix;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3x1Block;
import maspack.matrix.Matrix3x3Block;
import maspack.matrix.Matrix6d;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixNd;
import maspack.matrix.NumericalException;
import maspack.matrix.Point3d;
import maspack.matrix.SVDecomposition;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.color.ColorMapBase;
import maspack.render.color.HueColorMap;
import maspack.util.ArraySupport;
import maspack.util.DataBuffer;
import maspack.util.DoubleInterval;
import maspack.util.EnumRange;
import maspack.util.FunctionTimer;
import maspack.util.IndentingPrintWriter;
import maspack.util.IntHolder;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.Range;
import maspack.util.ReaderTokenizer;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.IncompressibleMaterial;
import artisynth.core.materials.IncompressibleMaterial.BulkPotential;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.SolidDeformation;
import artisynth.core.materials.ViscoelasticBehavior;
import artisynth.core.materials.ViscoelasticState;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.DeformableCollisionData;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.HasAuxState;
import artisynth.core.mechmodels.MechSystemModel;
import artisynth.core.mechmodels.MeshComponentList;
import artisynth.core.mechmodels.PointList;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.util.ArtisynthIO;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import artisynth.core.util.TransformableGeometry;

public class FemModel3d extends FemModel
implements TransformableGeometry, ScalableUnits, MechSystemModel,
Collidable, CopyableComponent, HasAuxState {
   protected PointList<FemNode3d> myNodes;
   // protected ArrayList<LinkedList<FemElement3d>> myElementNeighbors;

   public static boolean abortOnInvertedElems = false;
   public static boolean checkTangentStability = false;
   public static boolean noIncompressStiffnessDamping = false;
   // if the minimum real detJ for all elements is <= this number,
   // then request a step size reduction:
   public static double detJStepReductionLimit = 0.01;
   // This will disable detJ step reduction:
   // public static double detJStepReductionLimit = -Double.MAX_VALUE;

   
   private boolean myAbortOnInvertedElems = abortOnInvertedElems;
   private boolean myWarnOnInvertedElems = true;
   
   protected FunctionTimer timer = new FunctionTimer();

   protected void timerStart() {
      timer.start();
   }

   protected void timerStop(String msg) {
      timer.stop();
      System.out.println(msg + ": " + timer.result(1));
   }

   protected AABBTree myAABBTree;
   protected boolean myBVTreeValid;

   protected FemElement3dList myElements;
   protected AuxMaterialBundleList myAdditionalMaterialsList;
   // ArrayList<FemNode3d> myActiveNodes = null;
   HashMap<FemNode3d,FemMeshVertex> mySurfaceNodeMap;

   // private String mySolveMatrixFile = "solve.mat";
   private String mySolveMatrixFile = null;

   // private boolean myIncompressibleP = false;
   private double myIncompCompliance = 0;
   public static IncompMethod DEFAULT_HARD_INCOMP = IncompMethod.OFF;
   private IncompMethod myHardIncompMethod = DEFAULT_HARD_INCOMP;
   private boolean myHardIncompMethodValidP = false;
   public static IncompMethod DEFAULT_SOFT_INCOMP = IncompMethod.AUTO;
   private IncompMethod mySoftIncompMethod = DEFAULT_SOFT_INCOMP;
   private boolean mySoftIncompMethodValidP = false;

   // extra blocks in the solve matrix for soft nodel incomp stiffness;
   // needed for soft nodal incompressibility
   private boolean myNodalIncompBlocksAllocatedP = false;
   // incompressibility constraints attached to each FemNodeNeighbour;
   // needed for hard and soft nodal incompressibility
   private boolean myNodalIncompConstraintsAllocatedP = false;
   private boolean myHardIncompConfigValidP = false;
   private boolean myNodalRestVolumesValidP = false;
   private boolean myNodalIncompressConstraintsValidP = false;
   private boolean myHardIncompConstraintsChangedP = true;
   private double myHardIncompUpdateTime = -1;

   // total number of incompressibility constraints (GT.colSize(), not blocks)
   private int myNumIncompressConstraints = 0;

   private VectorNd myDg = null;
   private VectorNd myIncompressLambda = new VectorNd();

   // keep track of the number of tet, hex, and quadratic elements
   private int myNumTetElements = 0;
   private int myNumNodalMappedElements = 0;
   private int myNumQuadraticElements = 0;

   private double myMinDetJ; // used to record inverted elements
   private FemElement3d myMinDetJElement = null; // element with "worst" DetJ
   private int myNumInverted = 0; // used to tally number of inverted elements

   private static double DEFAULT_ELEMENT_WIDGET_SIZE = 0.0;
   private double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
   PropertyMode myElementWidgetSizeMode = PropertyMode.Inherited;

   // maximum number of pressure DOFs that can occur in an element
   private static int MAX_PRESSURE_VALS = 8;
   // maximum number of nodes for elements associated with nodal
   // incompressibility
   private static int MAX_NODAL_INCOMP_NODES = 8;

   // temp space for computing pressures
   private VectorNd myPressures = new VectorNd(MAX_PRESSURE_VALS);
   private MatrixNd myRinv = new MatrixNd();
   // temp space for computing pressure stiffness
   private double[] myKp = new double[MAX_PRESSURE_VALS];
   // temp space for computing nodal incompressibility constraints
   private Vector3d[] myNodalConstraints = new Vector3d[MAX_NODAL_INCOMP_NODES];
   // temp for computing element-wise linear stiffness strain
   private SymmetricMatrix3d myEps = new SymmetricMatrix3d();

   // protected ArrayList<FemSurface> myEmbeddedSurfaces;
   protected MeshComponentList<FemMesh> myMeshList;

   HashMap<FemElement3d,int[]> ansysElemProps =
   new HashMap<FemElement3d,int[]>();

   // protected boolean myClearMeshColoring = false;
   protected boolean myComputeNodalStress = false;
   protected boolean myComputeNodalStrain = false;
   // protected boolean mySubSurfaceRendering = true;

   public static ColorMapBase defaultColorMap = new HueColorMap(0.7, 0);
   protected ColorMapBase myColorMap;
   protected PropertyMode myColorMapMode = PropertyMode.Inherited;

   static maspack.render.Material myInvertedMaterial =
   maspack.render.Material.createDiffuse(1f, 0f, 0f, 0f, 32f);

   public static PropertyList myProps =
   new PropertyList(FemModel3d.class, FemModel.class);

   static {
      myProps.add(
         "incompressible",
         "Enforce incompressibility using constraints", DEFAULT_HARD_INCOMP);
      myProps.add(
         "softIncompMethod", "method of enforcing soft incompressibility",
         DEFAULT_SOFT_INCOMP);
      // myProps.add (
      // "hardIncompMethod", "method of enforcing hard incompressibility",
      // DEFAULT_HARD_INCOMP);
      myProps.add(
         "incompCompliance",
         "Compliance for incompressibilty constraints", 0, "[0,inf]");
      // myProps.add("subSurfaceRendering", "render sub-surface meshes", false);
      // myProps.addReadOnly ("freeVolume",
      // "Volume associated with free nodes");
      myProps.addInheritable(
         "elementWidgetSize:Inherited",
         "size of rendered widget in each element's center",
         DEFAULT_ELEMENT_WIDGET_SIZE, "[0,1]");
      myProps.addInheritable("colorMap:Inherited", "color map for stress/strain", 
         defaultColorMap, "CE");
   }

   //   public boolean getSubSurfaceRendering() {
   //      return mySubSurfaceRendering;
   //   }
   //
   //   public void setSubSurfaceRendering(boolean enable) {
   //      mySubSurfaceRendering = enable;
   //   }

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

   public void getCollidables(List<Collidable> list, int level) {
      list.add(this);
      // traverse forward for additional collidables (e.g. FemMesh)
      recursivelyGetLocalComponents (this, list, Collidable.class);
   }

   public void setComputeNodalStress(boolean enable) {
      if (enable != myComputeNodalStress) {
         myComputeNodalStress = enable;
         if (!enable) {
            // release memory used for computing stress
            for (FemNode3d n : myNodes) {
               n.myAvgStress = null;
            }
         }
      }
   }

   public void setComputeNodalStrain(boolean enable) {
      if (enable != myComputeNodalStrain) {
         myComputeNodalStrain = enable;
         if (!enable) {
            // release memory used for computing strain
            for (FemNode3d n : myNodes) {
               n.myAvgStrain = null;
            }
         }
      }
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      // myNu = DEFAULT_NU;
      // myE = DEFAULT_E;
      myDensity = DEFAULT_DENSITY;
      myStiffnessDamping = DEFAULT_STIFFNESS_DAMPING;
      myMassDamping = DEFAULT_MASS_DAMPING;
      myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
      myHardIncompMethod = DEFAULT_HARD_INCOMP;
      mySoftIncompMethod = DEFAULT_SOFT_INCOMP;
      myColorMap = defaultColorMap.copy();
      myAutoGenerateSurface = defaultAutoGenerateSurface;
   }

   public FemModel3d () {
      this(null);
   }

   protected void initializeChildComponents() {
      myNodes = new PointList<FemNode3d>(FemNode3d.class, "nodes", "n");
      myElements = new FemElement3dList("elements", "e");
      myAdditionalMaterialsList =
      new AuxMaterialBundleList("materials", "mat");
      mySurfaceNodeMap = new HashMap<FemNode3d,FemMeshVertex>();
      addFixed(myNodes);
      addFixed(myElements);
      addFixed(myAdditionalMaterialsList);
      super.initializeChildComponents();
   }

   public void addMaterialBundle(AuxMaterialBundle bundle) {
      if (!myAdditionalMaterialsList.contains(bundle)) {
         for (AuxMaterialElementDesc d : bundle.getElements()) {
            bundle.checkElementDesc(this, d);
         }
         myAdditionalMaterialsList.add(bundle);
      }
   }

   public boolean removeMaterialBundle(AuxMaterialBundle bundle) {
      return myAdditionalMaterialsList.remove(bundle);
   }

   public void clearMaterialBundles() {
      myAdditionalMaterialsList.removeAll();
   }

   public RenderableComponentList<AuxMaterialBundle> getMaterialBundles() {
      return myAdditionalMaterialsList;
   }

   public FemModel3d (String name) {
      super(name);
      setDefaultValues();
      for (int i = 0; i < MAX_NODAL_INCOMP_NODES; i++) {
         myNodalConstraints[i] = new Vector3d();
      }

      myMeshList =
      new MeshComponentList<FemMesh>(
      FemMesh.class, "meshes", "msh");
      addFixed(myMeshList);
      // add auto-generated surface
      if (myAutoGenerateSurface) {
         getOrCreateEmptySurfaceMesh();
         mySurfaceMeshValid = false;
      }
   }

   protected BVTree getBVTree() {
      if (myAABBTree == null || !myBVTreeValid) {
         myAABBTree = new AABBTree();
         Boundable[] elements = new Boundable[numElements()];
         for (int i = 0; i < elements.length; i++) {
            elements[i] = myElements.get(i);
         }
         myAABBTree.build(elements, numElements());
         myBVTreeValid = true;
      }
      return myAABBTree;
   }

   @Override
   public PointList<FemNode3d> getNodes() {
      return myNodes;
   }

   @Override
   public FemNode3d getNode(int idx) {
      return myNodes.get(idx);
   }

   @Override
   public FemNode3d getByNumber(int num) {
      return myNodes.getByNumber(num);
   }

   public FemElement3d getElementByNumber(int num) {
      return myElements.getByNumber(num);
   }

   @Override
   public RenderableComponentList<FemElement3d> getElements() {
      return myElements;
   }

   public void addNode(FemNode3d p) {
      myNodes.add(p);
   }

   public void addNumberedNode(FemNode3d p, int number) {
      myNodes.addNumbered(p, number);
   }

   public boolean removeNode(FemNode3d p) {

      // check if any elements dependent on this node
      LinkedList<FemElement3d> elems = p.getElementDependencies();
      for (FemElement3d elem : elems) {
         if (myElements.contains(elem)) {
            System.err.println("Error: unable to remove node because some elements still depend on it");
            return false;
         }
      }


      // make sure no surfaces depend on it
      for (FemMesh fm : myMeshList) {
         if (myAutoGenerateSurface && fm.isSurfaceMesh()) {
            // ignore autogenerated surface mesh
            mySurfaceMeshValid = false;
         } else if (fm.hasNodeDependency(p)) {
            System.err.println("Error: unable to remove node because the mesh '" 
            + fm.getName() + "' still depends on it");
            return false;
         }
      }


      if (myNodes.remove(p)) {
         return true;
      }
      else {
         return false;
      }
   }

   @Override
   public FemElement3d getElement(int idx) {
      return myElements.get(idx);
   }

   public LinkedList<FemNodeNeighbor> getNodeNeighbors(FemNode3d node) {
      return node.getNodeNeighbors();
   }

   private LinkedList<FemNodeNeighbor> myEmptyNeighborList =
   new LinkedList<FemNodeNeighbor>();

   /**
    * Gets the indirect neighbors for a node. This is used when computing
    * soft nodal-based incompressibility. See the documentation in
    * FemNode3d.getgetIndirectNeighbors().
    */
   protected LinkedList<FemNodeNeighbor> getIndirectNeighbors(FemNode3d node) {
      LinkedList<FemNodeNeighbor> indirect;
      if ((indirect = node.getIndirectNeighbors()) != null) {
         return indirect;
      }
      else {
         // returning a default empty list if indirect == null
         return myEmptyNeighborList;
      }
   }

   public LinkedList<FemElement3d> getElementNeighbors(FemNode3d node) {
      return node.getElementDependencies();
   }

   // private void attachElement(FemElement3d e) {
   //
   // // FemNode3d[] nodes = e.getNodes();
   // // for (int i = 0; i < nodes.length; i++) {
   // // for (int j = 0; j < nodes.length; j++) {
   // // nodes[i].registerNodeNeighbor(nodes[j]);
   // // }
   // // nodes[i].addElementDependency(e);
   // // }
   // // e.setMass(0);
   // // if (e instanceof TetElement) {
   // // myNumTetElements++;
   // // }
   // // else if (e.integrationPointsMapToNodes()) {
   // // myNumNodalMappedElements++;
   // // }
   // // else if (e instanceof QuadtetElement ||
   // // e instanceof QuadhexElement ||
   // // e instanceof QuadwedgeElement ||
   // // e instanceof QuadpyramidElement) {
   // // myNumQuadraticElements++;
   // // }
   // //invalidateStressAndStiffness();
   // }

   public void addElement(FemElement3d e) {
      myElements.add(e);
      if (myAutoGenerateSurface) {
         mySurfaceMeshValid = false;
      }
   }

   public void addNumberedElement(FemElement3d e, int elemId) {
      myElements.addNumbered(e, elemId);
      if (myAutoGenerateSurface) {
         mySurfaceMeshValid = false;
      }
   }

   // private void detachElement(FemElement3d e) {
   // // FemNode3d[] nodes = e.getNodes();
   // // double massPerNode = e.getMass() / e.numNodes();
   // // for (int i = 0; i < nodes.length; i++) {
   // // for (int j = 0; j < nodes.length; j++) {
   // // nodes[i].deregisterNodeNeighbor(nodes[j]);
   // // }
   // // nodes[i].addMass(-massPerNode);
   // // nodes[i].removeElementDependency(e);
   // // }
   // // if (e instanceof TetElement) {
   // // myNumTetElements--;
   // // }
   // // else if (e.integrationPointsMapToNodes()) {
   // // myNumNodalMappedElements--;
   // // }
   // // else if (e instanceof QuadtetElement ||
   // // e instanceof QuadhexElement ||
   // // e instanceof QuadwedgeElement ||
   // // e instanceof QuadpyramidElement) {
   // // myNumQuadraticElements--;
   // // }
   // //invalidateStressAndStiffness();
   // }

   public boolean removeElement(FemElement3d e) {
      boolean success = myElements.remove(e);
      if (myAutoGenerateSurface) {
         mySurfaceMeshValid = false;
      }
      return success;
   }

   public void clearElements() {
      myElements.removeAll();
      for (int i = 0; i < myNodes.size(); i++) {
         myNodes.get(i).setMass(0);
      }
      if (myAutoGenerateSurface) {
         mySurfaceMeshValid = false;
      }
   }

   /**
    * Adds a marker to this FemModel. The element to which it belongs is
    * determined automatically. If the marker's current position does not lie
    * within the model, it is projected onto the model's surface.
    * 
    * @param mkr
    * marker point to add to the model
    */
   public void addMarker(FemMarker mkr) {
      FemElement3d elem = findContainingElement(mkr.getPosition());
      if (elem == null) {
         Point3d newLoc = new Point3d();
         elem = findNearestSurfaceElement(newLoc, mkr.getPosition());
         mkr.setPosition(newLoc);
      }
      addMarker(mkr, elem);
   }
   
   /**
    * Adds a marker to this FemModel. The element to which it belongs is
    * determined automatically. If the marker's current position does not lie
    * within the model, it is projected onto the model's surface.
    * 
    * @param pnt
    * point to place a marker in the model
    */
   public FemMarker addMarker(Point3d pos) {
      FemMarker mkr = new FemMarker();
      FemElement3d elem = findContainingElement(pos);
      if (elem == null) {
         Point3d newLoc = new Point3d();
         elem = findNearestSurfaceElement(newLoc, pos);
         mkr.setPosition(newLoc);
      } else {
         mkr.setPosition(pos);
      }
      addMarker(mkr, elem);
      return mkr;
   }

   public FemMarker addNumberedMarker(Point3d pos, int markerId) {
      FemMarker mkr = new FemMarker();
      FemElement3d elem = findContainingElement(pos);
      if (elem == null) {
         Point3d newLoc = new Point3d();
         elem = findNearestSurfaceElement(newLoc, pos);
         mkr.setPosition(newLoc);
      } else {
         mkr.setPosition(pos);
      }
      addMarker(mkr, elem, markerId);
      return mkr;
   }

   @Override
   protected void updateNodeForces(double t) {
      if (!myStressesValidP) {
         updateStressAndStiffness();
      }
      boolean hasGravity = !myGravity.equals(Vector3d.ZERO);
      Vector3d fk = new Vector3d();
      Vector3d fd = new Vector3d();

      for (FemNode3d n : myNodes) {
         // n.setForce (n.getExternalForce());
         if (hasGravity) {
            n.addScaledForce(n.getMass(), myGravity);
         }
         fk.set(n.myInternalForce);
         fd.setZero();
         if (myStiffnessDamping != 0) {
            for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
               nbr.addDampingForce(fd);
            }
            // used for soft nodal-based incompressibilty:
            for (FemNodeNeighbor nbr : getIndirectNeighbors(n)) {
               nbr.addDampingForce(fd);
            }
            fd.scale(myStiffnessDamping);
         }
         // XXX Should particle have their own damping?
         fd.scaledAdd(myMassDamping * n.getMass(), n.getVelocity(), fd);
         n.subForce(fk);
         n.subForce(fd);
      }
      // if (stepAdjust != null && myMinDetJ <= 0) {
      // stepAdjust.recommendAdjustment (0.5, "element inversion");
      // }
   }

   // private void computeJacobianAndGradient (FemElement3d e) {
   // IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
   // IntegrationData3d[] idata = e.getIntegrationData();
   //
   // e.setInverted (false); // will check this below
   // //e.myAvgStress.setZero();
   // for (int k=0; k<ipnts.length; k++) {
   // IntegrationPoint3d pt = ipnts[k];
   // pt.computeJacobianAndGradient (e.myNodes, idata[k].myInvJ0);
   // double detJ = pt.computeInverseJacobian();
   // if (detJ < myMinDetJ) {
   // myMinDetJ = detJ;
   // myMinDetJElement = e;
   // }
   // if (detJ <= 0) {
   // e.setInverted (true);
   // myNumInverted++;
   // }
   // }
   // }

   protected double checkMatrixStability(DenseMatrix D) {
      SVDecomposition svd = new SVDecomposition();
      VectorNd eig = svd.getEigenValues(D, null);
      double min = eig.minElement();
      double max = eig.maxElement();
      if (Math.abs(max) > Math.abs(min)) {
         return min / max;
      }
      else {
         return max / min;
      }
   }

   private void computePressuresAndRinv(
      FemElement3d e, IncompressibleMaterial imat) {

      int npvals = e.numPressureVals();

      myRinv.setSize(npvals, npvals);
      myPressures.setSize(npvals);

      double[] pbuf = myPressures.getBuffer();
      double restVol = e.getRestVolume();

      if (npvals > 1) {
         myPressures.setZero();
         IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
         IntegrationData3d[] idata = e.getIntegrationData();

         if (imat.getBulkPotential() != BulkPotential.QUADRATIC) {
            myRinv.setZero();
         }
         for (int k = 0; k < ipnts.length; k++) {
            IntegrationPoint3d pt = ipnts[k];
            pt.computeJacobian(e.getNodes());
            double detJ0 = idata[k].getDetJ0();
            double detJ = pt.getJ().determinant() / detJ0;
            double dV = detJ0 * pt.getWeight();
            double[] H = pt.getPressureWeights().getBuffer();
            for (int i = 0; i < npvals; i++) {
               pbuf[i] += H[i] * imat.getEffectivePressure(detJ) * dV;
            }
            if (imat.getBulkPotential() != BulkPotential.QUADRATIC) {
               double mod = imat.getEffectiveModulus(detJ);
               for (int i = 0; i < npvals; i++) {
                  for (int j = 0; j < npvals; j++) {
                     myRinv.add(i, j, H[i] * H[j] * mod * dV);
                  }
               }
            }
         }
         Matrix W = e.getPressureWeightMatrix();
         W.mul(myPressures, myPressures);
         myPressures.scale(1 / restVol);
         if (imat.getBulkPotential() == BulkPotential.QUADRATIC) {
            myRinv.set(W);
            myRinv.scale(imat.getBulkModulus() / restVol);
         }
         else {
            // optimize later
            MatrixNd Wtmp = new MatrixNd(W);
            Wtmp.scale(1.0 / restVol);
            myRinv.mul(Wtmp);
            myRinv.mul(Wtmp, myRinv);
         }
      }
      else {
         double Jpartial = e.myVolumes[0] / e.myRestVolumes[0];
         pbuf[0] = (imat.getEffectivePressure(Jpartial) +
         0 * e.myLagrangePressures[0]);
         myRinv.set(0, 0, imat.getEffectiveModulus(Jpartial) / restVol);
      }
   }

   // private void computeRinv (FemElement3d e, IncompressibleMaterial imat) {
   // int npvals = e.numPressureVals();

   // myRinv.setSize (npvals, npvals);
   // myRinv.setZero();

   // if (npvals == 1) {
   // double Jpartial = e.myVolumes[0]/e.myRestVolumes[0];
   // myRinv.set (0, 0, imat.getEffectiveModulus(Jpartial)/e.myRestVolumes[0]);
   // }
   // else {
   // IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
   // IntegrationData3d[] idata = e.getIntegrationData();
   // for (int k=0; k<ipnts.length; k++) {
   // IntegrationPoint3d pt = ipnts[k];
   // pt.computeJacobian (e.getNodes());
   // double detJ = pt.getJ().determinant();
   // double dV = idata[k].getDetJ0()*pt.getWeight();
   // double[] H = pt.getPressureWeights().getBuffer();
   // for (int i=0; i<npvals; i++) {
   // for (int j=0; j<npvals; j++) {
   // myRinv.add (i, j, H[i]*H[j]*dV/imat.getEffectiveModulus(detJ));
   // }
   // }
   // }
   // myRinv.invert();
   // }
   // }

   private boolean requiresWarping(FemElement3d elem, FemMaterial mat) {
      if (mat instanceof LinearMaterial) {
         if (((LinearMaterial)mat).isCorotated()) {
            return true;
         }
      }

      for (AuxiliaryMaterial aux : elem.getAuxiliaryMaterials()) {
         if (aux instanceof AuxMaterialElementDesc) {
            AuxMaterialElementDesc desc = (AuxMaterialElementDesc)aux;
            if ((mat = desc.getEffectiveMaterial()) instanceof LinearMaterial) {
               if (((LinearMaterial)mat).isCorotated()) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   private boolean containsLinearMaterial(FemElement3d elem, FemMaterial mat) {
      if (mat instanceof LinearMaterial) {
         return true;
      }

      for (AuxiliaryMaterial aux : elem.getAuxiliaryMaterials()) {
         if (aux instanceof AuxMaterialElementDesc) {
            AuxMaterialElementDesc desc = (AuxMaterialElementDesc)aux;
            if ((mat = desc.getMaterial()) instanceof LinearMaterial) {
               return true;
            }
         }
      }
      return false;
   }

   // DIVBLK
   private void computeNonlinearStressAndStiffness(
      FemElement3d e, FemMaterial mat, Matrix6d D, IncompMethod softIncomp) {

      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
      IntegrationData3d[] idata = e.getIntegrationData();
      FemNode3d[] nodes = e.getNodes();
      int npvals = e.numPressureVals();
      double pressure = 0; // pressure for incompressibility
      double vol = 0;
      double restVol = 0;
      IncompressibleMaterial imat = null;
      Vector3d[] avgGNx = null;
      MatrixBlock[] constraints = null;
      double[] nodalExtrapMat = null;
      SymmetricMatrix3d C = new SymmetricMatrix3d();

      SymmetricMatrix3d sigmaAux = null;
      Matrix6d DAux = null;
      if (e.numAuxiliaryMaterials() > 0) {
         sigmaAux = new SymmetricMatrix3d();
         DAux = new Matrix6d();
      }

      // see if material is linear
      boolean corotated = false;
      IntegrationPoint3d wpnt = null;
      LinearMaterial linMat = null;
      if (mat instanceof LinearMaterial) {

         linMat = (LinearMaterial)mat;
         corotated = linMat.isCorotated();
         wpnt = e.getWarpingPoint();
         IntegrationData3d data = e.getWarpingData();
         wpnt.computeJacobianAndGradient(e.myNodes, data.myInvJ0);
         wpnt.sigma.setZero();
         if (corotated) {
            e.computeWarping(wpnt.F, myEps);
         }
         else {
            myEps.setSymmetric(wpnt.F);
         }
         // compute Cauchy strain
         myEps.m00 -= 1;
         myEps.m11 -= 1;
         myEps.m22 -= 1;
      }

      e.setInverted(false); // will check this below
      vol = e.getVolume();
      if (mat.isIncompressible() && softIncomp != IncompMethod.NODAL) {
         imat = (IncompressibleMaterial)mat;
         if (softIncomp == IncompMethod.ELEMENT) {

            computePressuresAndRinv(e, imat);
            if (D != null) {
               constraints = e.getIncompressConstraints();
               for (int i = 0; i < e.myNodes.length; i++) {
                  constraints[i].setZero();
               }
            }
            restVol = e.getRestVolume();
         }
      }
      else if (softIncomp == IncompMethod.NODAL) {
         if (e instanceof TetElement) {
            ((TetElement)e).getAreaWeightedNormals(myNodalConstraints);
            for (int i = 0; i < 4; i++) {
               myNodalConstraints[i].scale(-1 / 12.0);
            }
         }
         else {
            for (int i = 0; i < e.numNodes(); i++) {
               myNodalConstraints[i].setZero();
            }
         }

      }

      if (linMat != null) {
         for (int i = 0; i < nodes.length; i++) {
            int bi = nodes[i].getSolveIndex();
            if (bi != -1) {
               FemNode3d n = nodes[i];
               if (!myStiffnessesValidP) {
                  for (int j = 0; j < nodes.length; j++) {
                     int bj = nodes[j].getSolveIndex();
                     if (!mySolveMatrixSymmetricP || bj >= bi) {
                        e.addNodeStiffness(i, j, corotated);
                     }
                  }
               }
               e.addNodeForce(n.myInternalForce, i, corotated);
            }
         }
      }

      if (myComputeNodalStress || myComputeNodalStrain) {
         nodalExtrapMat = e.getNodalExtrapolationMatrix();
         if (linMat != null) {
            linMat.addStress(wpnt.sigma,
               myEps, corotated ? e.myWarper.R : null);
            for (int i = 0; i < nodes.length; i++) {
               FemNode3d nodei = nodes[i];
               if (myComputeNodalStress) {
                  nodei.addScaledStress(
                     1.0 / nodei.numAdjacentElements(), wpnt.sigma);
               }
               if (myComputeNodalStrain) {
                  nodei.addScaledStrain(
                     1.0 / nodei.numAdjacentElements(), myEps);
               }
            }
         }
      }

      double[] pbuf = myPressures.getBuffer();
      // e.myAvgStress.setZero();
      if (linMat == null || e.numAuxiliaryMaterials() > 0) {

         SolidDeformation def = new SolidDeformation();

         for (int k = 0; k < ipnts.length; k++) {
            IntegrationPoint3d pt = ipnts[k];
            IntegrationData3d dt = idata[k];
            pt.computeJacobianAndGradient(e.myNodes, idata[k].myInvJ0);
            def.setF(pt.F);
            double detJ = pt.computeInverseJacobian();
            if (detJ < myMinDetJ) {
               myMinDetJ = detJ;
               myMinDetJElement = e;
            }
            if (detJ <= 0 && !e.materialsAreInvertible()) {
               e.setInverted(true);
               myNumInverted++;
            }
            double dv = detJ * pt.getWeight();
            Vector3d[] GNx = pt.updateShapeGradient(pt.myInvJ);

            // compute pressure
            pressure = 0;
            double[] H = null;

            if (softIncomp == IncompMethod.ELEMENT) {
               H = pt.getPressureWeights().getBuffer();
               for (int l = 0; l < npvals; l++) {
                  pressure += H[l] * pbuf[l];
               }
            }
            else if (softIncomp == IncompMethod.NODAL) {
               if (e instanceof TetElement) {
                  // use the average pressure for all nodes
                  pressure = 0;
                  for (int i = 0; i < nodes.length; i++) {
                     pressure += nodes[i].myPressure;
                  }
                  pressure /= nodes.length;
               }
               else {
                  pressure = nodes[k].myPressure;
               }
            }
            else if (softIncomp == IncompMethod.FULL && imat != null) {
               pressure = imat.getEffectivePressure(detJ / dt.getDetJ0());
            }

            Matrix3d Q = (dt.myFrame != null ? dt.myFrame : Matrix3d.IDENTITY);

            pt.avgp = pressure;
            def.setAveragePressure(pressure);
            if (linMat != null) {
               pt.sigma.setZero();
               if (D != null) {
                  D.setZero();
               }
            } else {
               mat.computeStress(pt.sigma, def, Q, null);
               if (D != null) {
                  mat.computeTangent(D, pt.sigma, def, Q, null);
               }
               // mat.computeStress (pt.sigma, pt, dt, null);
               // if (D != null) {
               // mat.computeTangent(D, pt.sigma, pt, dt, null);
               // }
            }

            // reset pressure to zero for auxiliary
            pt.avgp = 0;
            def.setAveragePressure(0);
            if (e.numAuxiliaryMaterials() > 0) {
               for (AuxiliaryMaterial aux : e.myAuxMaterials) {
                  aux.computeStress(sigmaAux, def, pt, dt, mat);
                  pt.sigma.add(sigmaAux);
                  if (D != null) {
                     aux.computeTangent(DAux, sigmaAux, def, pt, dt, mat);
                     D.add(DAux);
                  }

                  // if (D != null) {
                  // aux.addStressAndTangent(pt.sigma, D, pt, dt, mat);
                  // } else {
                  // aux.addStress(pt.sigma, pt, dt, mat);
                  // }
               }
            }
            pt.avgp = pressure; // bring back pressure term
            def.setAveragePressure(pressure);
            if (mat.getViscoBehavior() != null) {
               ViscoelasticBehavior veb = mat.getViscoBehavior();
               ViscoelasticState state = idata[k].getViscoState();
               if (state == null) {
                  state = veb.createState();
                  idata[k].setViscoState(state);
               }
               veb.computeStress(pt.sigma, state);
               if (D != null) {
                  veb.computeTangent(D, state);
               }
            }
            else {
               dt.clearState();
            }

            // pt.avgp += e.myLagrangePressure;

            // e.myAvgStress.scaledAdd (dv, pt.sigma);

            for (int i = 0; i < e.myNodes.length; i++) {
               FemNode3d nodei = e.myNodes[i];
               int bi = nodei.getSolveIndex();
               FemUtilities.addStressForce(
                  nodei.myInternalForce, GNx[i], pt.sigma, dv);

               if (D != null) {
                  double p = 0;
                  double kp = 0;
                  if (mat.isIncompressible()
                  && softIncomp != IncompMethod.NODAL) {
                     if (softIncomp == IncompMethod.ELEMENT) {
                        FemUtilities.addToIncompressConstraints(
                           constraints[i], H, GNx[i], dv);
                     }
                     else if (softIncomp == IncompMethod.FULL) {
                        double dV = dt.getDetJ0() * pt.getWeight();
                        kp = imat.getEffectiveModulus(detJ / dt.getDetJ0()) * dV;
                     }
                     p = pressure;
                  }
                  else if (softIncomp == IncompMethod.NODAL) {
                     if (e.integrationPointsMapToNodes()) {
                        myNodalConstraints[i].scale(dv, GNx[i]);
                     }
                     else { // tet element
                        for (FemNodeNeighbor nbr : getNodeNeighbors(nodei)) {
                           int j = e.getLocalNodeIndex(nbr.myNode);
                           if (j != -1) {
                              nbr.myDivBlk.scaledAdd(1, myNodalConstraints[j]);
                           }
                        }
                     }
                  }
                  if (bi != -1) {
                     for (int j = 0; j < e.myNodes.length; j++) {
                        int bj = e.myNodes[j].getSolveIndex();
                        if (!mySolveMatrixSymmetricP || bj >= bi) {
                           e.myNbrs[i][j].addMaterialStiffness(
                              GNx[i], D, p, pt.sigma, GNx[j], dv);
                           if (kp != 0) {
                              e.myNbrs[i][j].addDilationalStiffness(
                                 kp, GNx[i], GNx[j]);
                           }
                        }
                     }
                  }
               }
               if (nodalExtrapMat != null) {
                  if (myComputeNodalStress) {
                     double a = nodalExtrapMat[i * ipnts.length + k];
                     if (a != 0) {
                        nodei.addScaledStress(
                           a / nodei.numAdjacentElements(), pt.sigma);
                     }
                  }
                  if (myComputeNodalStrain) {
                     double a = nodalExtrapMat[i * ipnts.length + k];
                     if (a != 0) {
                        // pt.computeRightCauchyGreen(C);
                        def.computeRightCauchyGreen(C);
                        C.m00 -= 1;
                        C.m11 -= 1;
                        C.m22 -= 1;
                        C.scale(0.5);
                        nodei.addScaledStrain(
                           a / nodei.numAdjacentElements(), C);
                     }
                  }
               }
            }
            if (D != null &&
            softIncomp == IncompMethod.NODAL &&
            e.integrationPointsMapToNodes()) {
               for (FemNodeNeighbor nbr : getNodeNeighbors(e.myNodes[k])) {
                  int j = e.getLocalNodeIndex(nbr.myNode);
                  if (j != -1) {
                     nbr.myDivBlk.scaledAdd(1, myNodalConstraints[j]);
                  }
               }
            }
         }
      }
      if (D != null) {
         if (mat.isIncompressible() && softIncomp == IncompMethod.ELEMENT) {
            boolean kpIsNonzero = false;
            for (int l = 0; l < npvals; l++) {
               double Jpartial = e.myVolumes[l] / e.myRestVolumes[l];
               myKp[l] =
               imat.getEffectiveModulus(Jpartial) / e.myRestVolumes[l];
               if (myKp[l] != 0) {
                  kpIsNonzero = true;
               }
            }
            // double kp = imat.getEffectiveModulus(vol/restVol)/restVol;
            if (true) {
               for (int i = 0; i < e.myNodes.length; i++) {
                  int bi = e.myNodes[i].getSolveIndex();
                  if (bi != -1) {
                     for (int j = 0; j < e.myNodes.length; j++) {
                        int bj = e.myNodes[j].getSolveIndex();
                        if (!mySolveMatrixSymmetricP || bj >= bi) {
                           e.myNbrs[i][j].addDilationalStiffness(
                              myRinv, constraints[i], constraints[j]);
                        } // end filling in symmetric
                     } // end filling in dilatational stiffness
                  } // end checking if valid index
               } // end looping through nodes
            } // XXX ALWAYS??
         } // end soft elem incompress
      } // end checking if computing tangent

   }

   /**
    * Computes the average deformation gradient for an element.
    */
   protected void computeAvgGNx(FemElement3d e) {

      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
      IntegrationData3d[] idata = e.getIntegrationData();

      Vector3d[] avgGNx = null;
      MatrixBlock[] constraints = null;

      constraints = e.getIncompressConstraints();
      for (int i = 0; i < e.myNodes.length; i++) {
         constraints[i].setZero();
      }

      e.setInverted(false);
      for (int k = 0; k < ipnts.length; k++) {
         IntegrationPoint3d pt = ipnts[k];
         pt.computeJacobianAndGradient(e.myNodes, idata[k].myInvJ0);
         double detJ = pt.computeInverseJacobian();
         if (detJ <= 0) {
            e.setInverted(true);
            // if (abortOnInvertedElems) {
            // throw new NumericalException ("Inverted elements");
            // }
         }
         double dv = detJ * pt.getWeight();
         Vector3d[] GNx = pt.updateShapeGradient(pt.myInvJ);

         double[] H = pt.getPressureWeights().getBuffer();
         for (int i = 0; i < e.myNodes.length; i++) {
            FemUtilities.addToIncompressConstraints(
               constraints[i], H, GNx[i], dv);
         }
      }
   }

   // private void computeLinearStressAndStiffness (
   // FemElement3d e, LinearMaterial mat, Matrix6d D, SymmetricMatrix3d Eps) {
   //
   // boolean corotated = mat.isCorotated();
   // int numAux = e.numAuxiliaryMaterials();
   // IntegrationPoint3d pnt = e.getWarpingPoint();
   // IntegrationData3d data = e.getWarpingData();
   //
   // double dv = 1; // will be overwritten below
   // Vector3d[] GNx = null;
   //
   // pnt.computeJacobianAndGradient (e.myNodes, data.myInvJ0);
   // pnt.sigma.setZero();
   // if (corotated) {
   // e.computeWarping (pnt.F, Eps);
   // }
   // else {
   // Eps.setSymmetric (pnt.F);
   // }
   // // compute Cauchy strain
   // Eps.m00 -= 1;
   // Eps.m11 -= 1;
   // Eps.m22 -= 1;
   //
   // if (numAux > 0) {
   // double detJ = pnt.computeInverseJacobian();
   // if (detJ < myMinDetJ) {
   // myMinDetJ = detJ;
   // myMinDetJElement = e;
   // }
   // if (detJ <= 0) {
   // e.setInverted (true);
   // myNumInverted++;
   // }
   // dv = detJ*pnt.getWeight();
   // GNx = pnt.updateShapeGradient(pnt.myInvJ);
   //
   // pnt.avgp = 0; // pressure = 0 since no incompressible
   // if (D != null) {
   // D.setZero();
   // }
   // for (AuxiliaryMaterial aux : e.myAuxMaterials) {
   // aux.addStress (pnt.sigma, pnt, data, mat);
   // if (D != null) {
   // aux.addTangent (D, pnt, data, mat);
   // }
   // }
   // }
   // FemNode3d[] enodes = e.getNodes();
   // for (int i = 0; i < enodes.length; i++) {
   // int bi = enodes[i].getSolveIndex();
   // if (bi != -1) {
   // FemNode3d n = enodes[i];
   // if (!myStiffnessesValidP) {
   // for (int j=0; j < enodes.length; j++) {
   // int bj = enodes[j].getSolveIndex();
   // if (!mySolveMatrixSymmetricP || bj >= bi) {
   // //e.addNodeStiffness (e.myNbrs[i][j].myK, i, j, corotated);
   // e.addNodeStiffness (i, j, corotated);
   // }
   // }
   // }
   // e.addNodeForce (n.myInternalForce, i, corotated);
   //
   // if (numAux > 0) {
   // FemUtilities.addStressForce (
   // n.myInternalForce, GNx[i], pnt.sigma, dv);
   // if (D != null) {
   // for (int j=0; j < enodes.length; j++) {
   // int bj = enodes[j].getSolveIndex();
   // if (!mySolveMatrixSymmetricP || bj >= bi) {
   // e.myNbrs[i][j].addMaterialStiffness (
   // GNx[i], D, 0, pnt.sigma, GNx[j], dv);
   // }
   // }
   // }
   // }
   // }
   // }
   // if (myComputeNodalStress || myComputeNodalStrain) {
   // mat.addStress (pnt.sigma, Eps, corotated ? e.myWarper.R : null);
   // for (int i=0; i<e.myNodes.length; i++) {
   // FemNode3d nodei = e.myNodes[i];
   // if (myComputeNodalStress) {
   // nodei.addScaledStress (1.0/nodei.numAdjacentElements(), pnt.sigma);
   // }
   // if (myComputeNodalStrain) {
   // nodei.addScaledStrain (1.0/nodei.numAdjacentElements(), Eps);
   // }
   // }
   // }
   // }
   //
   //
   // private void computeLinearStressAndStiffnessX (
   // FemElement3d e, LinearMaterial mat, Matrix6d D, SymmetricMatrix3d Eps) {
   //
   // boolean corotated = mat.isCorotated();
   // //int numAux = e.numAuxiliaryMaterials();
   // IntegrationPoint3d pnt = e.getWarpingPoint();
   // IntegrationData3d data = e.getWarpingData();
   //
   // double dv = 1; // will be overwritten below
   // Vector3d[] GNx = null;
   //
   // pnt.computeJacobianAndGradient (e.myNodes, data.myInvJ0);
   // pnt.sigma.setZero();
   // if (corotated) {
   // e.computeWarping (pnt.F, Eps);
   // }
   // else {
   // Eps.setSymmetric (pnt.F);
   // }
   // // compute Cauchy strain
   // Eps.m00 -= 1;
   // Eps.m11 -= 1;
   // Eps.m22 -= 1;
   //
   // FemNode3d[] enodes = e.getNodes();
   // for (int i = 0; i < enodes.length; i++) {
   // int bi = enodes[i].getSolveIndex();
   // if (bi != -1) {
   // FemNode3d n = enodes[i];
   // if (!myStiffnessesValidP) {
   // for (int j=0; j < enodes.length; j++) {
   // int bj = enodes[j].getSolveIndex();
   // if (!mySolveMatrixSymmetricP || bj >= bi) {
   // //e.addNodeStiffness (e.myNbrs[i][j].myK, i, j, corotated);
   // e.addNodeStiffness (i, j, corotated);
   // }
   // }
   // }
   // e.addNodeForce (n.myInternalForce, i, corotated);
   // }
   // }
   // if (myComputeNodalStress || myComputeNodalStrain) {
   // mat.addStress (pnt.sigma, Eps, corotated ? e.myWarper.R : null);
   // for (int i=0; i<e.myNodes.length; i++) {
   // FemNode3d nodei = e.myNodes[i];
   // if (myComputeNodalStress) {
   // nodei.addScaledStress (1.0/nodei.numAdjacentElements(), pnt.sigma);
   // }
   // if (myComputeNodalStrain) {
   // nodei.addScaledStrain (1.0/nodei.numAdjacentElements(), Eps);
   // }
   // }
   // }
   // }
   //

   private FemMaterial getElementMaterial(FemElement3d e) {
      FemMaterial mat = e.getMaterial();
      if (mat == null) {
         mat = myMaterial;
      }
      return mat;
   }

   // public void checkInversion () {
   // // if (!myVolumeValid) {
   // // updateVolume();
   // // }
   // myMinDetJ = Double.MAX_VALUE;
   // myMinDetJElement = null;
   // myNumInverted = 0;
   // for (FemElement3d e : myElements) {
   // FemMaterial mat = getElementMaterial(e);
   // if (!(mat instanceof LinearMaterial)) {
   // computeJacobianAndGradient (e);
   // }
   // }
   // }

   private void updateNodalPressures(IncompressibleMaterial imat) {

      for (FemNode3d n : myNodes) {
         n.myVolume = 0;
      }
      for (FemElement3d e : myElements) {
         FemNode3d[] nodes = e.myNodes;
         if (e instanceof TetElement) {
            double vol = e.getVolume();
            for (int i = 0; i < nodes.length; i++) {
               nodes[i].myVolume += vol / 4;
            }
         }
         else if (e.integrationPointsMapToNodes()) {
            IntegrationData3d[] idata = e.getIntegrationData();
            for (int i = 0; i < nodes.length; i++) {
               nodes[i].myVolume += idata[i].myDv;
            }
         }
      }
      for (FemNode3d n : myNodes) {
         if (volumeIsControllable(n)) {
            n.myPressure =
            imat.getEffectivePressure(n.myVolume / n.myRestVolume);
         }
         else {
            n.myPressure = 0;
         }
      }
   }

   private void updateNodalRestVolumes() {

      for (FemNode3d n : myNodes) {
         n.myRestVolume = 0;
      }
      for (FemElement3d e : myElements) {
         FemNode3d[] nodes = e.myNodes;
         if (e instanceof TetElement) {
            double vol = e.getRestVolume();
            for (int i = 0; i < nodes.length; i++) {
               nodes[i].myRestVolume += vol / 4;
            }
         }
         else if (e.integrationPointsMapToNodes()) {
            IntegrationData3d[] idata = e.getIntegrationData();
            IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
            for (int i = 0; i < nodes.length; i++) {
               nodes[i].myRestVolume += ipnts[i].myWeight * idata[i].myDetJ0;
            }
         }
      }
      myNodalRestVolumesValidP = true;
   }

   // DIVBLK
   public void updateStressAndStiffness() {

      // allocate or deallocate nodal incompressibility blocks
      setNodalIncompBlocksAllocated (getSoftIncompMethod()==IncompMethod.NODAL);

      // timerStart();
      // checkInversion();
      // timerStop("checkInversion");
      // timerStart();
      // clear existing internal forces and maybe stiffnesses
      for (FemNode3d n : myNodes) {
         n.myInternalForce.setZero();
         if (!myStiffnessesValidP) {
            for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
               nbr.zeroStiffness();
            }
            // used for soft nodal-based incompressibilty:
            for (FemNodeNeighbor nbr : getIndirectNeighbors(n)) {
               nbr.zeroStiffness();
            }
         }
         if (myComputeNodalStress) {
            n.zeroStress();
         }
         if (myComputeNodalStrain) {
            n.zeroStrain();
         }
      }
      if (!myVolumeValid) {
         updateVolume();
      }

      IncompMethod softIncomp = getSoftIncompMethod();

      if (softIncomp == IncompMethod.NODAL) {
         if (!myNodalRestVolumesValidP) {
            updateNodalRestVolumes();
         }
         setNodalIncompConstraintsAllocated(true);
         updateNodalPressures((IncompressibleMaterial)myMaterial);
         for (FemNode3d n : myNodes) {
            for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
               nbr.myDivBlk.setZero();
            }
         }
      }

      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d Eps = new SymmetricMatrix3d();
      // compute new forces as well as stiffness matrix if warping is enabled

      myMinDetJ = Double.MAX_VALUE;
      myMinDetJElement = null;
      myNumInverted = 0;

      double mins = Double.MAX_VALUE;
      FemElement3d minE = null;

      for (FemElement3d e : myElements) {
         FemMaterial mat = getElementMaterial(e);
         computeNonlinearStressAndStiffness(e, mat, D, softIncomp);
         if (checkTangentStability) {
            double s = checkMatrixStability(D);
            if (s < mins) {
               mins = s;
               minE = e;
            }
         }
         // if (mat instanceof LinearMaterial) {
         // computeLinearStressAndStiffness (e, (LinearMaterial)mat, D, Eps);
         // }
         // else {
         // computeNonlinearStressAndStiffness (e, mat, D, softIncomp);

         // if (checkTangentStability) {
         // double s = checkMatrixStability (D);
         // if (s < mins) {
         // mins = s;
         // minE = e;
         // }
         // }
         // }
      }
      if (softIncomp == IncompMethod.NODAL) {
         IncompressibleMaterial imat = (IncompressibleMaterial)myMaterial;
         for (FemNode3d n : myNodes) {
            if (volumeIsControllable(n)) {
               double restVol = n.myRestVolume;
               myKp[0] =
               imat.getEffectiveModulus(n.myVolume / restVol) / restVol;
               // myKp[0] = 1;
               if (myKp[0] != 0) {
                  for (FemNodeNeighbor nbr_i : getNodeNeighbors(n)) {
                     int bi = nbr_i.myNode.getSolveIndex();
                     for (FemNodeNeighbor nbr_j : getNodeNeighbors(n)) {
                        int bj = nbr_j.myNode.getSolveIndex();
                        if (!mySolveMatrixSymmetricP || bj >= bi) {
                           FemNodeNeighbor nbr =
                           nbr_i.myNode.getNodeNeighbor(nbr_j.myNode);
                           if (nbr == null) {
                              nbr =
                              nbr_i.myNode.getIndirectNeighbor(nbr_j.myNode);
                           }
                           if (nbr == null) {
                              throw new InternalErrorException(
                                 "No neighbor block at bi=" + bi + ", bj=" + bj);
                           }
                           else {
                              nbr.addDilationalStiffness(
                                 myKp, nbr_i.myDivBlk, nbr_j.myDivBlk);
                           }

                        }
                     }
                  }
               }
            }
         }
      }

      if (checkTangentStability && minE != null) {
         System.out.println("min s=" + mins + ", element " + minE.getNumber());
      }

      if (myNumInverted > 0) {
         if (myWarnOnInvertedElems) {
            System.out.println(
               "Warning: " + myNumInverted + " inverted elements; min detJ=" +
               myMinDetJ + ", element " + myMinDetJElement.getNumber());
         }
         if (myAbortOnInvertedElems) {
            throw new NumericalException("Inverted elements");
         }
      }

      if (!myStiffnessesValidP && mySolveMatrixSymmetricP) {
         for (FemNode3d n : myNodes) {
            int bi = n.getSolveIndex();
            if (bi != -1) {
               for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
                  int bj = nbr.myNode.getSolveIndex();
                  if (bj > bi) {
                     FemNodeNeighbor nbrT =
                     nbr.myNode.getNodeNeighborBySolveIndex(bi);
                     nbrT.setTransposedStiffness(nbr);
                  }
               }
               // used for soft nodal-based incompressibilty:
               for (FemNodeNeighbor nbr : getIndirectNeighbors(n)) {
                  int bj = nbr.myNode.getSolveIndex();
                  if (bj > bi) {
                     FemNodeNeighbor nbrT =
                     nbr.myNode.getIndirectNeighborBySolveIndex(bi);
                     nbrT.setTransposedStiffness(nbr);
                  }
               }
            }
         }
      }
      myStiffnessesValidP = true;
      myStressesValidP = true;
      // timerStop("stressAndStiffness");
   }

   public void updateStress() {
      // clear existing internal forces and maybe stiffnesses
      timerStart();
      for (FemNode3d n : myNodes) {
         n.myInternalForce.setZero();
         for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
            nbr.zeroStiffness();
         }
         // used for soft nodal-based incompressibilty:
         for (FemNodeNeighbor nbr : getIndirectNeighbors(n)) {
            nbr.zeroStiffness();
         }
         if (myComputeNodalStress) {
            n.zeroStress();
         }
         if (myComputeNodalStrain) {
            n.zeroStrain();
         }
      }
      if (!myVolumeValid) {
         updateVolume();
      }
      IncompMethod softIncomp = getSoftIncompMethod();

      if (softIncomp == IncompMethod.NODAL) {
         updateNodalPressures((IncompressibleMaterial)myMaterial);
      }

      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d Eps = new SymmetricMatrix3d();
      // compute new forces as well as stiffness matrix if warping is enabled
      // myMinDetJ = Double.MAX_VALUE;
      for (FemElement3d e : myElements) {
         FemMaterial mat = getElementMaterial(e);
         computeNonlinearStressAndStiffness(
            e, mat, /* D= */null, softIncomp);
         // if (mat instanceof LinearMaterial) {
         // computeLinearStressAndStiffness (
         // e, (LinearMaterial)mat, D, Eps);
         // }
         // else {
         // computeNonlinearStressAndStiffness (
         // e, mat, /*D=*/null, softIncomp);
         // }
      }
      myStressesValidP = true;
   }

   public static boolean defaultAutoGenerateSurface = true;  // add surface mesh to model by default
   public static String DEFAULT_SURFACEMESH_NAME = "surface";
   private boolean myAutoGenerateSurface = defaultAutoGenerateSurface;
   protected boolean mySurfaceMeshValid = false;

   //protected PolygonalMesh mySurfaceMesh = null;
   //   protected FemMesh myFineSurface = null;
   //   protected boolean myFineSurfaceValid = false;
   //   protected PolygonalMesh myCollisionMesh = null;

   // Submeshes are a temporary hack to allow self-collisions within an FEM
   // model. Since our collision code doesn't currently support self-
   // intersection for a single mesh, if we want self-intersections, we instead
   // can give the model a set of "sub-meshes", each of which is a closed
   // manifold mesh comprising part of the FEM's volume. If there are two or
   // more such meshes present, self-intersection is facilitated by enabling
   // collisions between all the sub-meshes.
   //   protected ArrayList<PolygonalMesh> mySubSurfaces =
   //   new ArrayList<PolygonalMesh>();

   //   public int numSubSurfaces() {
   //      return mySubSurfaces.size();
   //   }
   //
   //   public PolygonalMesh getSubSurface(int idx) {
   //      return mySubSurfaces.get(idx);
   //   }
   //
   //   public boolean removeSubSurface(PolygonalMesh mesh) {
   //      return mySubSurfaces.remove(mesh);
   //   }
   //
   //   public void addSubSurface(PolygonalMesh mesh) {
   //      mySubSurfaces.add(mesh);
   //   }
   //
   //   public void clearSubSurfaces() {
   //      mySubSurfaces.clear();
   //   }

   // public int numActiveComponents() {
   // return getActiveNodes().size();
   // }

   protected boolean checkSolveMatrixIsSymmetric() {
      if (!myMaterial.hasSymmetricTangent()) {
         return false;
      }
      for (int i = 0; i < myElements.size(); i++) {
         FemElement3d e = myElements.get(i);
         FemMaterial m = e.getMaterial();
         if (m != null && !m.hasSymmetricTangent()) {
            return false;
         }
         if (e.numAuxiliaryMaterials() > 0) {
            for (AuxiliaryMaterial aux : e.myAuxMaterials) {
               if (!aux.hasSymmetricTangent()) {
                  return false;
               }
            }
         }
      }
      return true;
   }

   public int getJacobianType() {
      if (mySolveMatrixSymmetricP) {
         return Matrix.SYMMETRIC;
      }
      else {
         return Matrix.INDEFINITE;
      }
   }

   private void addNeighborVelJacobian(
      SparseNumberedBlockMatrix M, FemNode3d node,
      FemNodeNeighbor nbr, double s) {

      if (nbr.myNode.getSolveIndex() != -1) {
         Matrix3x3Block blk =
         (Matrix3x3Block)M.getBlockByNumber(nbr.myBlkNum);
         if (nbr.myNode == node && node.isActive()) {
            nbr.addVelJacobian(
               blk, s, myStiffnessDamping, myMassDamping);
         }
         else {
            nbr.addVelJacobian(blk, s, myStiffnessDamping, 0);
         }
      }
   }

   public void addVelJacobian(
      SparseNumberedBlockMatrix M, double s) {

      if (!myStressesValidP || !myStiffnessesValidP) {
         updateStressAndStiffness();
      }
      for (int i = 0; i < myNodes.size(); i++) {
         FemNode3d node = myNodes.get(i);
         if (node.getSolveIndex() != -1) {
            for (FemNodeNeighbor nbr : getNodeNeighbors(node)) {
               addNeighborVelJacobian(M, node, nbr, s);
            }
            // used for soft nodal-based incompressibilty:
            for (FemNodeNeighbor nbr : getIndirectNeighbors(node)) {
               addNeighborVelJacobian(M, node, nbr, s);
            }
         }
      }
   }

   public void addPosJacobian(
      SparseNumberedBlockMatrix M, double s) {

      if (!myStressesValidP || !myStiffnessesValidP) {
         updateStressAndStiffness();
      }
      for (int i = 0; i < myNodes.size(); i++) {
         FemNode3d node = myNodes.get(i);
         if (node.getSolveIndex() != -1) {
            for (FemNodeNeighbor nbr : getNodeNeighbors(node)) {
               if (nbr.myNode.getSolveIndex() != -1) {
                  Matrix3x3Block blk =
                  (Matrix3x3Block)M.getBlockByNumber(nbr.myBlkNum);
                  nbr.addPosJacobian(blk, s);
               }
            }
            // used for soft nodal-based incompressibilty:
            for (FemNodeNeighbor nbr : getIndirectNeighbors(node)) {
               if (nbr.myNode.getSolveIndex() != -1) {
                  Matrix3x3Block blk =
                  (Matrix3x3Block)M.getBlockByNumber(nbr.myBlkNum);
                  nbr.addPosJacobian(blk, s);
               }
            }
         }
      }
      // System.out.println ("symmetric=" + mySolveMatrix.isSymmetric(1e-6));
   }

   private void addNodeNeighborBlock(
      SparseNumberedBlockMatrix S, FemNodeNeighbor nbr, int bi) {

      int bj = nbr.myNode.getSolveIndex();
      Matrix3x3Block blk = null;
      int blkNum = -1;
      if (bj != -1) {
         blk = (Matrix3x3Block)S.getBlock(bi, bj);
         if (blk == null) {
            blk = new Matrix3x3Block();
            S.addBlock(bi, bj, blk);
         }
         blkNum = blk.getBlockNumber();
      }
      // nbr.setBlock (blk);
      nbr.setBlockNumber(blkNum);
   }

   public void addSolveBlocks(SparseNumberedBlockMatrix S) {
      setNodalIncompBlocksAllocated(getSoftIncompMethod() == IncompMethod.NODAL);

      for (int i = 0; i < myNodes.size(); i++) {
         FemNode3d node = myNodes.get(i);
         int bi = node.getSolveIndex();
         if (bi != -1) {
            for (FemNodeNeighbor nbr : getNodeNeighbors(node)) {
               addNodeNeighborBlock(S, nbr, bi);
            }
            // used for soft nodal-based incompressibilty:
            for (FemNodeNeighbor nbr : getIndirectNeighbors(node)) {
               addNodeNeighborBlock(S, nbr, bi);
            }
         }
      }
      // System.out.println ("sparsity=\n" + S.getBlockPattern());
   }

   public void recursivelyInitialize(double t, int level) {
      if (t == 0) {
         setNodalIncompBlocksAllocated (
            getSoftIncompMethod()==IncompMethod.NODAL);

         for (FemElement3d e : myElements) {
            e.invalidateRestData();
            // e.getRestVolume();
            e.setInverted(false);
            for (int k = 0; k < e.numPressureVals(); k++) {
               e.myLagrangePressures[k] = 0;
            }
            e.clearState();
         }
         for (FemNode3d n : myNodes) {
            n.zeroStress();
         }
         // paranoid ... should already be invalid:
         invalidateStressAndStiffness();
         // not sure what we cleared the activeNodes ...
         // myActiveNodes = null;
      }
      // myForcesNeedUpdating = true;
      // make sure we update values in div matrix, if needed
      myHardIncompUpdateTime = -1;
      super.recursivelyInitialize(t, level);
      // updateVolume(); volume will be updated by updateForces();

      // embedded surfaces
      myMeshList.updateSlavePos();
   }

   //   public boolean recursivelyCheckStructureChanged() {
   //      return false;
   ////      return setNodalIncompBlocksAllocated (
   ////         getSoftIncompMethod()==IncompMethod.NODAL);
   //   }

   public synchronized StepAdjustment advance(
      double t0, double t1, int flags) {
      // implicitStep (TimeBase.ticksToSeconds(t1 - t0));
      // System.out.println(TimeBase.ticksToSeconds(t1-t0));
      // System.out.println ("fem " + (t1-t0)/1000000);
      if (myProfileP) {
         steptime.start();
      }

      if (t0 == 0) {
         updateForces(t0);
      }
      StepAdjustment stepAdjust = new StepAdjustment();

      if (!myDynamicsEnabled) {
         mySolver.nonDynamicSolve(t0, t1, stepAdjust);
         // set parametric targets
         // setParametricTargets (1, TimeBase.round(t1-t0));
         // if (!myDynamicsEnabled) {
         // return 1;
         recursivelyFinalizeAdvance(null, t0, t1, flags, 0);
      }
      else {
         mySolver.solve(t0, t1, stepAdjust);
         DynamicComponent c = checkVelocityStability();
         if (c != null) {
            throw new NumericalException(
               "Unstable velocity detected, component "
               + ComponentUtils.getPathName(c));
         }
         recursivelyFinalizeAdvance(stepAdjust, t0, t1, flags, 0);
         // checkForInvertedElements();
      }

      if (myProfileP) {
         steptime.stop();
         System.out.println(
            "time=" + steptime.result(1));
      }
      return stepAdjust;
   }

   public void setSurfaceRendering(SurfaceRender mode) {

      // SurfaceRender oldMode = mySurfaceRendering;
      super.setSurfaceRendering(mode);

      if (myMeshList.size() > 0) {
         FemMesh surf = myMeshList.get(0);
         if (surf.isSurfaceMesh()) {
            surf.setSurfaceRendering(mode);
         }
      }

      //      if (mode != SurfaceRender.Stress && mode != SurfaceRender.Strain) {
      //         myClearMeshColoring = true;
      //      }
      //      if (mode != oldMode) {
      //         myFineSurfaceValid = false;
      //         myFineSurface = null;
      //         if (mode == SurfaceRender.Stress) {
      //            setComputeNodalStress(true);
      //         }
      //         else if (mode == SurfaceRender.Strain) {
      //            setComputeNodalStrain(true);
      //         }
      //
      //         // XXX other entities may be recording or plotting stress/strain
      //         // so don't ever clear mode
      //         // if (oldMode == SurfaceRender.Stress) {
      //         // setComputeNodalStress (false);
      //         // }
      //         // else if (oldMode == SurfaceRender.Strain) {
      //         // setComputeNodalStrain (false);
      //         // }
      //         //         else if (mode == SurfaceRender.Fine) {
      //         //            getFineSurfaceMesh();
      //         //         }
      //      }
   }

   public void render(GLRenderer renderer, int flags) {
      SurfaceRender rendering = getSurfaceRendering();
      if (rendering != SurfaceRender.None && getSurfaceMesh() != null) {
         // int flags = isSelected() ? GLRenderer.SELECTED : 0;
         // if (rendering == SurfaceRender.Fine && getFineSurfaceMesh() != null) {
         //            PolygonalMesh mesh = getFineSurfaceMesh();
         //            mesh.render (renderer, myRenderProps, flags);
         //         }
         //         else {
         //            if ((rendering == SurfaceRender.Stress || 
         //            		rendering == SurfaceRender.Strain)) {
         //               flags |= GLRenderer.VERTEX_COLORING;
         //               flags |= GLRenderer.HSV_COLOR_INTERPOLATION;
         //            }
         //            PolygonalMesh mesh = getSurfaceMesh();
         //            mesh.render (renderer, myRenderProps, flags);
         //         }
      }
      //      if (getSubSurfaceRendering()) {
      //         for (PolygonalMesh mesh : mySubSurfaces) {
      //            mesh.render (renderer, myRenderProps, 0);
      //            //renderer.drawMesh(myRenderProps, mesh, 0);
      //         }
      //      }

   }

   public DoubleInterval getNodalPlotRange(SurfaceRender rendering) {

      if (!(rendering == SurfaceRender.Strain || 
      rendering == SurfaceRender.Stress)) {
         return null;
      }

      double min = Double.MAX_VALUE;
      double max = 0;
      for (int i = 0; i < myNodes.size(); i++) {
         FemNode3d node = myNodes.get(i);
         double s;
         if (rendering == SurfaceRender.Stress) {
            s = (float)node.getVonMisesStress();
         }
         else {
            s = (float)node.getVonMisesStrain();
         }
         if (s < min) {
            min = s;
         }
         if (s > max) {
            max = s;
         }
      }
      return new DoubleInterval(min, max);
   }

   public void prerender(RenderList list) {
      list.addIfVisible(myNodes);
      list.addIfVisible(myElements);
      list.addIfVisible(myMarkers);
      // build surface mesh if needed
      if (myAutoGenerateSurface && !mySurfaceMeshValid) {
         getSurfaceMesh();  // triggers creation of surface mesh
      }

      list.addIfVisible(myMeshList);
      myAdditionalMaterialsList.prerender(list);


      // PolygonalMesh surfaceMesh = null;
      //    SurfaceRender surfaceRendering = getSurfaceRendering();
      //      if (surfaceRendering != SurfaceRender.None &&
      //         (surfaceMesh = getSurfaceMesh()) != null) {
      //         getSurfaceMesh().saveRenderInfo();
      //         getSurfaceMesh().clearDisplayList(myRenderProps);
      //      }

      //      if (surfaceRendering == SurfaceRender.Fine && 
      //         getFineSurfaceMesh() != null) {
      //         getFineSurfaceMesh().saveRenderInfo();
      //         getFineSurfaceMesh().clearDisplayList(myRenderProps);
      //      }
      //      if (getSubSurfaceRendering()) {
      //         for (PolygonalMesh mesh : mySubSurfaces) {
      //            mesh.saveRenderInfo();
      //            mesh.clearDisplayList(myRenderProps);
      //         }
      //      }
      //      if (myClearMeshColoring && surfaceMesh != null) {
      //         for (Vertex3d v : surfaceMesh.getVertices()) {
      //            v.setColor(null);
      //         }
      //         myClearMeshColoring = false;
      //      }
      //      if (surfaceMesh != null &&
      //         (surfaceRendering == SurfaceRender.Stress ||
      //         surfaceRendering == SurfaceRender.Strain)) {
      //         
      //         float[] colorArray = new float[3];
      //         if (myStressPlotRanging == Ranging.Auto) {
      //            myStressPlotRange.merge(getNodalPlotRange(surfaceRendering));
      //         }
      //         ArrayList<Vertex3d> vertices = surfaceMesh.getVertices();
      //         for (int i = 0; i < vertices.size(); i++) {
      //            FemMeshVertex vtx = (FemMeshVertex)vertices.get(i);
      //            double value;
      //            if (surfaceRendering == SurfaceRender.Stress) {
      //               value = ((FemNode3d)vtx.myPnt).getVonMisesStress();
      //            }
      //            else {
      //               value = ((FemNode3d)vtx.myPnt).getVonMisesStrain();
      //            }
      //            //double h = 2 / 3.0 * (1 - value / myStressPlotRange.getRange());
      //            //vtx.setColorHSV(h, 1, 1, getRenderProps().getAlpha());
      //            
      //            myColorMap.getRGB(value/myStressPlotRange.getRange(), colorArray);
      //            vtx.setColor(colorArray[0], colorArray[1], colorArray[2],
      //               getRenderProps().getAlpha());
      //         }
      //      }
   }

   public void getSelection(LinkedList<Object> list, int qid) {
      super.getSelection(list, qid);
   }

   public static FemModel3d createGrid(
      String name, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ, double density) {
      FemModel3d mod = new FemModel3d(name);
      mod.setDensity(density);
      FemFactory.createTetGrid(mod, widthX, widthY, widthZ, numX, numY, numZ);
      // mod.setToGrid (widthX, widthY, widthZ, numX, numY, numZ);
      return mod;
   }

   public static FemModel3d createHexGrid(
      String name, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ, double density) {
      FemModel3d mod = new FemModel3d(name);
      mod.setDensity(density);
      FemFactory.createHexGrid(mod, widthX, widthY, widthZ, numX, numY, numZ);
      // mod.setToGrid (widthX, widthY, widthZ, numX, numY, numZ);
      return mod;
   }

   protected void doclear() {
      super.doclear();
      myMeshList.clear();
      myElements.clear();
      myNodes.clear();
      mySurfaceMeshValid = false;
   }

   public void scan(ReaderTokenizer rtok, Object ref) throws IOException {
      doclear();
      setDefaultValues();
      super.scan(rtok, ref);

      // now done by AddRemoveHandler
      // for (FemElement3d e : myElements)
      // { attachElement (e);
      // }
      invalidateStressAndStiffness();
      notifyStructureChanged(this);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
   throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "minBound")) {
         myMinBound = new Point3d();
         myMinBound.scan(rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "maxBound")) {
         myMaxBound = new Point3d();
         myMaxBound.scan(rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "autoGenerateSurface")) {
         myAutoGenerateSurface = rtok.scanBoolean();
         if (myAutoGenerateSurface) {
            mySurfaceMeshValid = false;
         }
         return true;
      }
      // XXX seems to be a lot of missing properties...

      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      invalidateStressAndStiffness();
      notifyStructureChanged(this);
   }

   public void writeMesh(PrintWriter pw, PolygonalMesh mesh) {
      pw.print("[ ");
      IndentingPrintWriter.addIndentation(pw, 2);
      for (Face face : mesh.getFaces()) {
         pw.print("f");
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         do {
            FemMeshVertex vtx = (FemMeshVertex)he.head;
            pw.print(" " + vtx.myPnt.getNumber());
            he = he.getNext();
         } while (he != he0);
         pw.println("");
      }
      IndentingPrintWriter.addIndentation(pw, -2);
      pw.println("]");
   }

   public PolygonalMesh scanMesh(String fileName) throws IOException {
      return scanMesh(ArtisynthIO.newReaderTokenizer(fileName));
   }

   /**
    * Creates a triangular polygonal mesh from a list of faces whose vertices
    * are described by FEM node numbers.
    */
   public PolygonalMesh scanMesh(ReaderTokenizer rtok) throws IOException {

      PolygonalMesh mesh = new PolygonalMesh();
      HashMap<FemNode3d,FemMeshVertex> nodeVertexMap =
      new HashMap<FemNode3d,FemMeshVertex>();

      mesh.setFixed(false);
      mesh.setUseDisplayList(true); // use display list for surface mesh
      // it is manually cleared at prerender
      rtok.scanToken('[');
      ArrayList<FemMeshVertex> vtxList = new ArrayList<FemMeshVertex>();
      while (rtok.nextToken() == ReaderTokenizer.TT_WORD &&
      rtok.sval.equals("f")) {
         vtxList.clear();
         while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER &&
         rtok.tokenIsInteger()) {
            int nnum = (int)rtok.lval;
            FemNode3d node = null;
            if ((node = myNodes.getByNumber(nnum)) == null) {
               throw new IOException("Node " + nnum + " not found, " + rtok);
            }
            FemMeshVertex vtx = nodeVertexMap.get(node);
            if (vtx == null) {
               vtx = new FemMeshVertex(node);
               nodeVertexMap.put(node, vtx);
               mesh.addVertex(vtx);
            }
            vtxList.add(vtx);
         }
         if (vtxList.size() != 3) {
            throw new IOException(
               "Face has " + vtxList.size() + " vertices instead of 3, " + rtok);
         }
         mesh.addFace(vtxList.toArray(new FemMeshVertex[0]));
         rtok.pushBack();
      }
      rtok.pushBack();
      rtok.scanToken(']');
      if (mesh.getNumVertices() == 0) {
         return null;
      }
      else {
         mesh.setRenderBuffered(true);
         return mesh;
      }
   }

   public void writeSurfaceMesh(PrintWriter pw) {
      writeMesh(pw, getSurfaceMesh());
   }

   public void writeSurfaceMesh(String fileName) {
      try {
         IndentingPrintWriter pw =
         ArtisynthIO.newIndentingPrintWriter(fileName);
         writeMesh(pw, getSurfaceMesh());
         pw.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void scanSurfaceMesh(ReaderTokenizer rtok) throws IOException {
      PolygonalMesh mesh = scanMesh(rtok);
      setSurfaceMesh(mesh);
   }

   public void scanSurfaceMesh(String fileName) throws IOException {
      scanSurfaceMesh(ArtisynthIO.newReaderTokenizer(fileName));
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      if (myMinBound != null) {
         pw.println("minBound=[" + myMinBound.toString(fmt) + "]");
      }
      if (myMaxBound != null) {
         pw.println("maxBound=[" + myMaxBound.toString(fmt) + "]");
      }

      // write other properties?
      pw.println("autoGenerateSurface=" + myAutoGenerateSurface);

      super.writeItems(pw, fmt, ancestor);
   }

   public void transformGeometry(AffineTransform3dBase X) {
      transformGeometry(X, this, 0);
   }

   public void transformGeometry(
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
      myNodes.transformGeometry(X, topObject, flags);
      for (FemNode3d n : myNodes) {
         n.myRest.transform(X);
      }
      for (FemElement3d e : myElements) {
         e.invalidateRestData();
      }
      updateLocalAttachmentPos();
      invalidateStressAndStiffness();
      // // Is this needed? Don't think so ...

      // for (DynamicAttachment a : myAttachments) {
      // a.updateAttachment();
      // }
      if (myMinBound != null) {
         myMinBound.transform(X);
      }
      if (myMaxBound != null) {
         myMaxBound.transform(X);
      }

      // update meshes
      myMeshList.updateSlavePos();
   }

   // public ArrayList<FemNode3d> getActiveNodes() {
   // if (myActiveNodes == null) {
   // myActiveNodes = new ArrayList<FemNode3d>();
   // for (int i = 0; i < myNodes.size(); i++) {
   // if (MechModel.isActive (myNodes.get (i))) {
   // myActiveNodes.add (myNodes.get (i));
   // }
   // }
   // }
   // return myActiveNodes;
   // }

   // public boolean getIncompressible() {
   // return getHardIncompMethod() != IncompMethod.OFF;
   // }

   public IncompMethod getIncompressible() {
      return getHardIncompMethod();
   }

   // public void setIncompressible (boolean enable) {

   // if (enable != (getHardIncompMethod() != IncompMethod.OFF)) {
   // if (enable) {
   // setHardIncompMethod (IncompMethod.AUTO);
   // }
   // else {
   // setHardIncompMethod (IncompMethod.OFF);
   // }
   // }

   // // if (enable != myIncompressibleP) {
   // // myHardIncompConfigValidP = false;
   // // //myDivMatrix = null;
   // // // myDivMasses = null;
   // // myHardIncompConstraintsChangedP = true;
   // // myIncompressibleP = enable;
   // //notifyStructureChanged (this);
   // // }
   // }

   public void setMaterial(FemMaterial mat) {
      mySoftIncompMethodValidP = false;
      super.setMaterial(mat);
      updateSoftIncompMethod();
   }

   private boolean softNodalIncompressAllowed() {
      return (numTetElements() + numNodalMappedElements() == myElements.size() && 
      myMaterial instanceof IncompressibleMaterial);
   }

   private boolean hardNodalIncompressAllowed() {
      return (numTetElements() + numNodalMappedElements() == myElements.size());
   }

   public IncompMethod getHardIncompMethod() {
      if (!myHardIncompMethodValidP) {
         if (!hardNodalIncompressAllowed() &&
         (myHardIncompMethod == IncompMethod.NODAL ||
         myHardIncompMethod == IncompMethod.AUTO ||
         myHardIncompMethod == IncompMethod.ON)) {
            myHardIncompMethod = IncompMethod.ELEMENT;
         }
         else if (myHardIncompMethod == IncompMethod.AUTO ||
         myHardIncompMethod == IncompMethod.ON) {
            if (myElements.size() > numActiveNodes()) {
               myHardIncompMethod = IncompMethod.NODAL;
            }
            else {
               myHardIncompMethod = IncompMethod.ELEMENT;
            }
         }
         myHardIncompMethodValidP = true;
      }
      return myHardIncompMethod;
   }

   public void setIncompressible(IncompMethod method) {
      if (method == IncompMethod.FULL) {
         throw new IllegalArgumentException(
            "Unsupported method: " + method);
      }
      myHardIncompMethod = method;
      myHardIncompConfigValidP = false;
      myHardIncompConstraintsChangedP = true;
      myHardIncompMethodValidP = false;
   }

   public Range getHardIncompMethodRange() {
      return new EnumRange<IncompMethod>(
      IncompMethod.class, new IncompMethod[] {
                                              IncompMethod.NODAL,
                                              IncompMethod.ELEMENT,
                                              IncompMethod.AUTO,
                                              IncompMethod.ON,
                                              IncompMethod.OFF });
   }

   /**
    * Transforms a requested soft incompressible method into an allowed one.
    */
   protected IncompMethod getAllowedSoftIncompMethod (IncompMethod method) {
      if (myElements == null) {
         // can't evaluate without element information
         return method; 
      }
      if (!softNodalIncompressAllowed() &&
      (method == IncompMethod.NODAL ||
      method == IncompMethod.AUTO)) {
         method = IncompMethod.ELEMENT;
      }
      else if (mySoftIncompMethod == IncompMethod.AUTO) {
         if (myElements.size() > numActiveNodes()) {
            method = IncompMethod.NODAL;
         }
         else {
            method = IncompMethod.ELEMENT;
         }
      }
      return method;
   }

   // public IncompMethod getSoftIncompMethod() {
   //    if (!mySoftIncompMethodValidP) {
   //       mySoftIncompMethod = getAllowedSoftIncompMethod (mySoftIncompMethod);
   //       mySoftIncompMethodValidP = true;
   //    }
   //    return mySoftIncompMethod;
   // }

   // public void setSoftIncompMethod(IncompMethod method) {
   //    if (method == IncompMethod.ON ||
   //       method == IncompMethod.OFF) {
   //       throw new IllegalArgumentException(
   //          "Unsupported method: " + method);
   //    }
   //    mySoftIncompMethod = method;
   //    mySoftIncompMethodValidP = false;
   // }

   protected void updateSoftIncompMethod () {
      setSoftIncompMethod (mySoftIncompMethod);
   }

   public void setSoftIncompMethod(IncompMethod method) {
      if (method == IncompMethod.ON ||
      method == IncompMethod.OFF) {
         throw new IllegalArgumentException(
            "Unsupported method: " + method);
      }
      IncompMethod old = mySoftIncompMethod;
      // evaluate which soft incomp method is allowed
      mySoftIncompMethod = getAllowedSoftIncompMethod (method);
      if ((mySoftIncompMethod == IncompMethod.NODAL) !=
      (old == IncompMethod.NODAL)) {
         // if the method changed to or from NODAL, send a structure changed
         // event because this means that the system matrix will change
         // by adding or NODAL incompressibility blocks
         notifyStructureChanged (this);
      }
   }

   public IncompMethod getSoftIncompMethod() {
      // update allowed soft incomp method if necessary. If the method
      // has changed, we assume that the appropriate structure change
      // notification has been sent by whatever caused the change in
      // the first place.
      mySoftIncompMethod = getAllowedSoftIncompMethod (mySoftIncompMethod);
      return mySoftIncompMethod;
   }

   public Range getSoftIncompMethodRange() {
      return new EnumRange<IncompMethod>(
      IncompMethod.class, new IncompMethod[] {
                                              IncompMethod.NODAL,
                                              IncompMethod.ELEMENT,
                                              IncompMethod.AUTO,
                                              IncompMethod.FULL });
   }

   public double getIncompCompliance() {
      return myIncompCompliance;
   }

   public void setIncompCompliance(double c) {
      if (c < 0) {
         throw new IllegalArgumentException("compliance must be non-negative");
      }
      myIncompCompliance = c;
   }

   private double getVolumeError(FemElement3d e) {
      // System.out.println ("vol= " + e.getVolume());
      return e.getVolume() - e.getRestVolume();
   }

   private double getLocalVolumeError(
      FemNode3d[] nodes, IntegrationPoint3d pt, IntegrationData3d dt) {

      pt.computeJacobian(nodes);
      double detJ = pt.getJ().determinant();
      double vol = detJ * pt.getWeight();
      double volr = dt.getDetJ0() * pt.getWeight();
      return (vol - volr);
   }

   private boolean isControllable(FemNode3d node) {
      return node.isControllable(); // getSolveIndex() != -1;
   }

   private boolean volumeIsControllable(FemNode3d node) {
      return node.isActive();
      // for (FemNodeNeighbor nbr : getNodeNeighbors(node)) {
      // if (isControllable(nbr.myNode)) {
      // return true;
      // }
      // }
      // return false;
   }

   private boolean hasControllableNodes(FemElement3d elem) {
      return elem.hasControllableNodes();
   }

   // private boolean getParametricVelocity (FemNode3d node, Vector3d vel) {
   // if (!MechModel.myParametricsInSystemMatrix &&
   // node.computeParametricVelocity (vel)) {
   // return true;
   // }
   // else {
   // return false;
   // }
   // }

   // DIVBLK
   /**
    * Updates the divergence matrix. Returns true if the matrix
    * was recreated.
    */
   private void updateHardIncompInfo(double time) {

      if (time != myHardIncompUpdateTime) {
         myHardIncompUpdateTime = time;

         if (!myHardIncompConfigValidP) {
            configureHardIncomp();
         }
         if (getHardIncompMethod() == IncompMethod.NODAL) {
            updateHardNodalIncompInfo(myDg, time);
         }
         else if (getHardIncompMethod() == IncompMethod.ELEMENT) {
            updateHardElementIncompInfo(myDg, time);
         }
         else {
            throw new IllegalArgumentException(
               "unsupported hard incompress method " + getHardIncompMethod());
         }
      }
   }

   protected void updateElementCounts() {
      if (myNumTetElements == -1) {
         myNumTetElements = 0;
         myNumNodalMappedElements = 0;
         myNumQuadraticElements = 0;
         for (int i=0; i<myElements.size(); i++) {
            FemElement3d e = myElements.get(i);
            if (e instanceof TetElement) {
               myNumTetElements++;
            }
            else if (e.integrationPointsMapToNodes()) {
               myNumNodalMappedElements++;
            }
            else if (e instanceof QuadtetElement ||
            e instanceof QuadhexElement ||
            e instanceof QuadwedgeElement ||
            e instanceof QuadpyramidElement) {
               myNumQuadraticElements++;
            }
         }
      }
   }           

   public int numTetElements() {
      updateElementCounts();
      return myNumTetElements;
   }

   public int numQuadraticElements() {
      updateElementCounts();
      return myNumQuadraticElements;
   }

   protected int numNodalMappedElements() {
      updateElementCounts();
      return myNumNodalMappedElements;
   }

   /**
    * Update the blocks uses in the incompressibility constraint matrices.
    * These are stored in the myDviBlk fields of each FemNodeNeighbor.
    * Derivative values for inactive nodes are stored in b.
    */
   // DIVBLK
   private void updateHardNodalIncompInfo(VectorNd b, double time) {

      Vector3d tmp1 = new Vector3d();
      Vector3d tmp2 = new Vector3d();

      b.setZero();
      for (FemNode3d n : myNodes) {
         if (n.getIncompressIndex() != -1) {
            for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
               // if (isControllable (nbr.myNode)) {
               nbr.myDivBlk.setZero();
               // }
            }
         }
      }
      int idx;
      for (FemElement3d e : myElements) {
         FemNode3d[] enodes = e.getNodes();
         double dg = 0;
         if (e instanceof TetElement) {
            TetElement tet = (TetElement)e;
            tet.getAreaWeightedNormals(myNodalConstraints);
            for (int i = 0; i < 4; i++) {
               myNodalConstraints[i].scale(-1 / 12.0);
            }
            // dg = tet.getIncompDerivative (tmp1, tmp2)/4.0;
         }
         else if (e.integrationPointsMapToNodes()) {
            for (int i = 0; i < e.numNodes(); i++) {
               myNodalConstraints[i].setZero();
            }
         }
         for (int i = 0; i < enodes.length; i++) {
            FemNode3d n = enodes[i];
            if (e.integrationPointsMapToNodes()) {
               IntegrationPoint3d pt = e.getIntegrationPoints()[i];
               IntegrationData3d dt = e.getIntegrationData()[i];
               pt.computeJacobianAndGradient(e.myNodes, dt.myInvJ0);
               double detJ = pt.computeInverseJacobian();
               double dv = detJ * pt.getWeight();
               Vector3d[] GNx = pt.updateShapeGradient(pt.myInvJ);
               for (int k = 0; k < GNx.length; k++) {
                  myNodalConstraints[k].scale(dv, GNx[k]);
               }
            }
            if ((idx = n.getIncompressIndex()) != -1) {
               for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
                  FemNode3d nnode = nbr.myNode;
                  int j = e.getLocalNodeIndex(nnode);
                  if (j != -1) {
                     // if (isControllable (nnode)) {
                     nbr.myDivBlk.scaledAdd(1, myNodalConstraints[j]);
                     // }
                  }
               }
               b.add(idx, dg);
            }
         }
      }
   }

   private void updateHardElementIncompInfo(VectorNd b, double time) {
      int ci = 0;
      Vector3d pvel = new Vector3d();

      IncompMethod softIncomp = getSoftIncompMethod();
      b.setZero();

      for (FemElement3d e : myElements) {
         if (e.getIncompressIndex() != -1) {
            if (softIncomp != IncompMethod.ELEMENT ||
            !getElementMaterial(e).isIncompressible() ||
            time == 0) {
               // need to do this at time=0 since stresses may not have been
               // computed yet
               computeAvgGNx(e);
            }
            for (int k = 0; k < e.numPressureVals(); k++) {
               b.set(ci++, 0);
            }
         }
      }
   }

   // private int incompressibilitySupported() {
   // if (myIncompressSupported == -1) {
   // if (myNumTetElements + myNumNodalMapped == myElements.size() &&
   // myNumTetElements > myNumNodalMapped) {
   // myIncompressSupported = NODE_INCOMPRESS;
   // System.out.println ("NODAL");
   // }
   // else {
   // myIncompressSupported = ELEM_INCOMPRESS;
   // System.out.println ("ELEMENT");
   // }
   // }
   // return myIncompressSupported;
   // }

   private boolean hasActiveNodes() {
      for (int i = 0; i < myNodes.size(); i++) {
         if (myNodes.get(i).isActive()) {
            return true;
         }
      }
      return false;
   }

   private int numActiveNodes() {
      int num = 0;
      for (int i = 0; i < myNodes.size(); i++) {
         if (myNodes.get(i).isActive()) {
            num++;
         }
      }
      return num;
   }

   private void configureHardIncomp() {
      if (!hasActiveNodes()) {
         return;
      }
      IncompMethod method = getHardIncompMethod();
      if (method == IncompMethod.NODAL) {
         configureHardNodalIncomp();
      }
      else if (method == IncompMethod.ELEMENT) {
         configureHardElementIncomp();
      }
      else {
         throw new IllegalArgumentException(
            "unsupported hard incompressibility method " + method);
      }
      myDg = new VectorNd(myNumIncompressConstraints);
      myHardIncompConfigValidP = true;
      myHardIncompConstraintsChangedP = true;
   }

   private boolean setNodalIncompBlocksAllocated(boolean allocated) {
      if (myNodalIncompBlocksAllocatedP != allocated) {
         for (FemNode3d n : myNodes) {
            if (allocated) {
               for (FemNodeNeighbor nbr_i : getNodeNeighbors(n)) {
                  FemNode3d node_i = nbr_i.myNode;
                  for (FemNodeNeighbor nbr_j : getNodeNeighbors(n)) {
                     FemNode3d node_j = nbr_j.myNode;
                     if (node_i.getNodeNeighbor(node_j) == null &&
                     node_i.getIndirectNeighbor(node_j) == null) {
                        // System.out.println (
                        // "adding block at "+node_i.getSolveIndex()+" "+
                        // node_j.getSolveIndex());
                        node_i.addIndirectNeighbor(node_j);
                     }
                  }
               }
            }
            else {
               n.clearIndirectNeighbors();
            }
         }
         // XXX signal structure change for solve matrix
         myNodalIncompBlocksAllocatedP = allocated;
         return true;
      }
      else {
         return false;
      }
   }

   private void setNodalIncompConstraintsAllocated(boolean allocated) {
      if (myNodalIncompConstraintsAllocatedP != allocated) {
         for (FemNode3d n : myNodes) {
            if (allocated) {
               for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
                  nbr.myDivBlk = new Matrix3x1Block();
               }
            }
            else {
               for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
                  nbr.myDivBlk = null;
               }
            }
         }
         myNodalIncompConstraintsAllocatedP = allocated;
      }
   }

   // private void allocNodalIncompressConstraints() {
   // // XXX
   // // Allocate for all nodes just to be safe.
   // for (FemNode3d n : myNodes) {
   // for (FemNodeNeighbor nbr : getNodeNeighbors (n)) {
   // // myDivBlk will be used by addBilateralConstraints
   // if (nbr.myDivBlk == null) {
   // nbr.myDivBlk = new Matrix3x1Block();
   // }
   // }
   // n.clearIndirectNeighbors();
   // }
   // for (FemNode3d n : myNodes) {
   // for (FemNodeNeighbor nbr_i : getNodeNeighbors (n)) {
   // for (FemNodeNeighbor nbr_j : getNodeNeighbors (n)) {
   // if (nbr_i.myNode.getNodeNeighbor (nbr_j.myNode) == null &&
   // nbr_i.myNode.getIndirectNeighbor (nbr_j.myNode) == null) {
   // System.out.println (
   // "adding block at "+nbr_i.myNode.getSolveIndex()+" "+
   // nbr_j.myNode.getSolveIndex());
   // nbr_i.myNode.addIndirectNeighbor (nbr_j.myNode);
   // }
   // }
   // }
   // }
   // myNodalIncompressConstraintsValidP = true;
   // }

   private void configureHardNodalIncomp() {

      if (!hasActiveNodes()) {
         return;
      }

      // determine which nodes have which controllablity
      int ci = 0; // constraint index
      for (FemNode3d n : myNodes) {
         int idx = -1;
         // XXX for now, only enforce incompressibility around nodes that are
         // free. That decreases the accuracy but increases the chance that the
         // resulting set of constraints will have full rank.
         // if (n.isActive()) {
         // idx = ci++;
         // }

         // This is what we should do, but it can lead to rank-deficient
         // constraint sets:
         if (volumeIsControllable(n)) {
            idx = ci++;
         }
         n.setIncompressIndex(idx);
      }
      myNumIncompressConstraints = ci;
      myIncompressLambda.setSize(ci);
      myIncompressLambda.setZero();
      System.out.println("num incomp=" + ci);

      setNodalIncompConstraintsAllocated(true);
   }

   private void configureHardElementIncomp() {

      int ci = 0; // constraint index
      for (FemElement3d e : myElements) {
         int npvals = e.numPressureVals();
         int cidx = -1;
         if (hasControllableNodes(e)) {
            cidx = ci;
            ci += npvals;
         }
         e.setIncompressIndex(cidx);
         for (int i = 0; i < npvals; i++) {
            e.myLagrangePressures[i] = 0;
         }
      }
      myNumIncompressConstraints = ci;
   }

   /**
    * Prints the nodes of this FEM in a format which is compatible with ANSYS.
    * 
    * @param pw
    * PrintWriter to which nodes are written
    */
   public void printANSYSNodes(PrintWriter pw) {
      NumberFormat ifmt = new NumberFormat("%8d");
      int nodeIdx = 1;
      for (FemNode3d n : myNodes) {
         pw.println(ifmt.format(nodeIdx) + " "
         + n.myRest.toString("%16.9e    "));
         nodeIdx++;
      }
      pw.flush();
   }

   /**
    * Prints the elements of this FEM in a format which is compatible with
    * ANSYS.
    * 
    * @param pw
    * PrintWriter to which elements are written
    */
   public void printANSYSElements(PrintWriter pw) {
      NumberFormat dfmt = new NumberFormat("%6d");
      int femIdx = 1;
      for (FemElement3d e : myElements) {
         FemNode3d[] nodes = e.getNodes();
         int[] nums = new int[13];
         if (e instanceof TetElement) {
            int idx0 = myNodes.indexOf(nodes[0]) + 1;
            int idx1 = myNodes.indexOf(nodes[1]) + 1;
            int idx2 = myNodes.indexOf(nodes[2]) + 1;
            int idx3 = myNodes.indexOf(nodes[3]) + 1;

            nums[0] = idx0;
            nums[1] = idx1;
            nums[2] = idx2;
            nums[3] = idx2;
            nums[4] = idx3;
            nums[5] = idx3;
            nums[6] = idx3;
            nums[7] = idx3;
         }
         else {
            throw new IllegalArgumentException("Unknown element type: "
            + e.getClass().getName());
         }
         for (int i = 8; i < 12; i++) {
            nums[i] = 1;
         }
         nums[12] = 0;
         for (int i = 0; i < nums.length; i++) {
            pw.print(dfmt.format(nums[i]));
         }
         pw.println("       " + femIdx);
         femIdx++;

      }
      pw.flush();
   }

   //   public void setSurfaceEdgeHard(FemNode3d n0, FemNode3d n1, boolean hard) {
   //      PolygonalMesh mesh = getSurfaceMesh();
   //      FemMeshVertex v0 = mySurfaceNodeMap.get(n0);
   //      FemMeshVertex v1 = mySurfaceNodeMap.get(n1);
   //      if (v0 == null || v1 == null) {
   //         throw new IllegalArgumentException(
   //            "One or both nodes not on surface mesh");
   //      }
   //      mesh.setHardEdge(v0, v1, hard);
   //   }

   public FemMeshVertex getSurfaceMeshVertex(FemNode node) {
      getSurfaceMesh();
      return mySurfaceNodeMap.get(node);
   }

   public boolean isSurfaceNode(FemNode3d node) {
      getSurfaceMesh();
      return mySurfaceNodeMap.containsKey(node);
   }

   private static class ArrayElementFilter extends FemModel.ElementFilter {
      Collection<FemElement> myElems;

      public ArrayElementFilter (Collection<FemElement> elems) {
         myElems = elems;
      }

      public boolean elementIsValid(FemElement e) {
         return myElems.contains(e);
      }
   }

   /**
    * Recreates the surface mesh based on all elements
    */
   protected FemMesh recreateSurfaceMesh() {
      FemMesh femMesh = getOrCreateEmptySurfaceMesh();

      // by default, build fine surface mesh if quadratic elements present
      if (numQuadraticElements() > 0) {
         femMesh.createFineSurface (3, new ElementFilter(), mySurfaceNodeMap);
      } else {
         femMesh.createSurface(new ElementFilter(), mySurfaceNodeMap);
      }
      mySurfaceMeshValid = true;
      return femMesh;
   }

   public FemMesh createSurfaceMesh(Collection<FemElement> elems) {
      return createSurfaceMesh(new ArrayElementFilter(elems));
   }

   public FemMesh createSurfaceMesh (ElementFilter efilter) {
      FemMesh femMesh = getOrCreateEmptySurfaceMesh();
      femMesh.createSurface (efilter, mySurfaceNodeMap);
      // mySurfaceMesh = (PolygonalMesh)femMesh.getMesh();
      mySurfaceMeshValid = true;
      myAutoGenerateSurface = false;   // disable auto regeneration since mesh was manually created
      return femMesh;
   }

   public FemMesh createFineSurfaceMesh (int resolution, ElementFilter efilter) {
      // XXX note: if element is removed and we're on "auto" surface mesh,
      // we may lose ability to re-create
      FemMesh femMesh = null;
      femMesh = getOrCreateEmptySurfaceMesh();
      femMesh.createFineSurface (resolution, efilter, mySurfaceNodeMap);
      // mySurfaceMesh = (PolygonalMesh)femMesh.getMesh();
      mySurfaceMeshValid = true;
      myAutoGenerateSurface = false;   // disable since manually created
      return femMesh;
   }

   //   public void createSurfaceMeshOld (ElementFilter efilter) {
   //      mySurfaceNodeMap.clear();
   //
   //      PolygonalMesh mesh = new PolygonalMesh();
   //      mesh.setFixed(false);
   //      mesh.setUseDisplayList(true);
   //
   //      LinkedList<FaceNodes3d> allFaces = new LinkedList<FaceNodes3d>();
   //      // faces adjacent to each node
   //      ArrayList<LinkedList<FaceNodes3d>> nodeFaces =
   //         new ArrayList<LinkedList<FaceNodes3d>>(numNodes());
   //
   //      for (int i = 0; i < numNodes(); i++) {
   //         nodeFaces.add(new LinkedList<FaceNodes3d>());
   //      }
   //
   //      // create a list of all the faces for all the elements, and for
   //      // each node, create a list of all the faces it is associated with
   //      for (FemElement3d e : myElements) {
   //         if (efilter.elementIsValid(e)) {
   //            FaceNodes3d[] faces = e.getTriFaces();
   //            for (FaceNodes3d f : faces) {
   //               addEdgeNodesToFace(f);
   //               for (FemNode3d n : f.getAllNodes()) {
   //                  int idx = myNodes.indexOf(n);
   //                  if (idx == -1) {
   //                     throw new InternalErrorException(
   //                        "Element " + e.getNumber() + ": bad node "
   //                           + n.getNumber());
   //                  }
   //                  nodeFaces.get(myNodes.indexOf(n)).add(f);
   //               }
   //               allFaces.add(f);
   //            }
   //         }
   //      }
   //
   //      // now for each face, check to see if it is overlapping with other faces
   //      HashSet<FaceNodes3d> adjacentFaces = new HashSet<FaceNodes3d>();
   //      for (FaceNodes3d f : allFaces) {
   //         if (!f.isHidden()) {
   //            adjacentFaces.clear();
   //            for (FemNode3d n : f.getAllNodes()) {
   //               Iterator<FaceNodes3d> it =
   //                  nodeFaces.get(myNodes.indexOf(n)).iterator();
   //               while (it.hasNext()) {
   //                  FaceNodes3d g = it.next();
   //                  if (g.isHidden()) {
   //                     it.remove();
   //                  }
   //                  else if (g.getElement() != f.getElement()) {
   //                     adjacentFaces.add(g);
   //                  }
   //               }
   //            }
   //            for (FaceNodes3d g : adjacentFaces) {
   //               if (f.isContained(g)) {
   //                  f.setHidden(true);
   //                  g.setOverlapping(true);
   //               }
   //               if (g.isContained(f)) {
   //                  g.setHidden(true);
   //                  f.setOverlapping(true);
   //               }
   //            }
   //         }
   //      }
   //
   //      // form the surface mesh from the non-overlapping faces
   //      for (FaceNodes3d f : allFaces) {
   //         if (!f.isOverlapping() &&
   //            !f.hasSelfAttachedNode() &&
   //            !f.isSelfAttachedToFace()) {
   //            FemNode3d[][] triangles = f.triangulate();
   //            boolean triangulatedQuad =
   //               (triangles.length == 2 && triangles[0][0] == triangles[1][0]);
   //            for (int i = 0; i < triangles.length; i++) {
   //               FemNode3d[] tri = triangles[i];
   //               FemMeshVertex[] vtxs = new FemMeshVertex[3];
   //               for (int j = 0; j < 3; j++) {
   //                  if ((vtxs[j] = mySurfaceNodeMap.get(tri[j])) == null) {
   //                     vtxs[j] = addSurfaceVertex(mesh, tri[j]);
   //                  }
   //               }
   //               Face face = mesh.addFace(vtxs);
   //               if (triangulatedQuad && i == 0) {
   //                  face.setFirstQuadTriangle(true);
   //               }
   //            }
   //         }
   //      }
   //      mesh.setRenderBuffered(true);
   //      mySurfaceMesh = mesh;
   //      mySurfaceMeshValid = true;
   //   }

   public PolygonalMesh getSurfaceMesh() {

      if (myMeshList.size() < 1) {
         return null;
      }

      // if auto, take first.  If not, take first one marked as a surface mesh;
      MeshBase mesh = myMeshList.get(0).getMesh();
      if (myAutoGenerateSurface && !mySurfaceMeshValid) {
         FemMesh newFM = recreateSurfaceMesh();         
         mesh = newFM.getMesh(); // grab newly created mesh
         // paranoid: call in case mesh is rendered directly before prerender() 
         PolygonalMesh smesh = (PolygonalMesh)mesh;
         smesh.saveRenderInfo();
         return smesh;
      } else if (myAutoGenerateSurface) {
         return (PolygonalMesh)mesh;
      }

      // find first mesh marked as surface mesh
      for (FemMesh fm : myMeshList) {
         if (fm.isSurfaceMesh()) {
            mesh = fm.getMesh();
            if (mesh instanceof PolygonalMesh) {
               return (PolygonalMesh)mesh;
            }
         }
      }

      return null;   // null if not autogenerated
   }
   
   @Override
   public boolean isCollidable () {
      PolygonalMesh mesh = getSurfaceMesh();
      if (mesh != null) {
         return true;
      }
      return false;
   }

   //   /**
   //    * Explicitly sets a mesh to be used for collisions. Note that this feature
   //    * is only supported in code - collision meshs aren't copied, and they can't
   //    * be saved or restored.
   //    */
   //   public void setCollisionMesh(PolygonalMesh mesh) {
   //      myCollisionMesh = mesh;
   //   }

   //   public PolygonalMesh getFineSurfaceMesh() {
   //      if (!myFineSurfaceValid) {
   //         if (numQuadraticElements() > 0) {
   //            FemMesh femMesh = new FemMesh(this);
   //            HashMap<FemNode3d,FemMeshVertex> nodeMap = new HashMap<FemNode3d,FemMeshVertex>();
   //            femMesh.createFineSurface (3, new ElementFilter(), nodeMap);
   //            myFineSurface = femMesh;
   //            // paranoid: call case mesh is rendered directly before prerender() 
   //            femMesh.getMesh().saveRenderInfo();
   //         }
   //         else {
   //            myFineSurface = null;
   //         }
   //         myFineSurfaceValid = true;
   //      }
   //      if (myFineSurface == null) {
   //         return null;
   //      }
   //      else {
   //         return (PolygonalMesh)myFineSurface.getMesh();
   //      }
   //   }

   public FemMesh addMesh(MeshBase mesh) {
      String meshName = ModelComponentBase.makeValidName(mesh.getName(), null, myMeshList);
      return addMesh(meshName, mesh);
   }
   
   public FemMesh addMesh(String name, MeshBase mesh) {
      mesh.setFixed(false);
      FemMesh surf = FemMesh.createEmbedded(this, mesh);
      surf.setName(name);
      myMeshList.add(surf);
      return surf;
   }

   public FemMesh getMesh(String name) {
      return myMeshList.get(name);
   }

   public MeshComponentList<FemMesh> getMeshes() {
      return myMeshList;
   }
   
   public int getNumMeshes() {
      return myMeshList.size();
   }

   public void addMesh(FemMesh surf) {
      if (surf.myFem == this) {
         myMeshList.add (surf);
      }
   }

   private void addMesh(FemMesh surf, int pos) {
      if (surf.myFem == this) {
         myMeshList.add(surf, pos);
      }
   }

   public boolean removeMesh(FemMesh surf) {
      if (myAutoGenerateSurface) {
         // removing autogenerated surface
         if (surf == myMeshList.get(0)) {
            myAutoGenerateSurface = false;
         }
      }
      return myMeshList.remove(surf);
   }

   public void removeAllMeshes() {
      myMeshList.clear();
      if (myAutoGenerateSurface) {
         // regenerate surface mesh
         setAutoGenerateSurface(true);
      }
   }

   private FemMesh getOrCreateEmptySurfaceMesh() {
      if (myMeshList.size()==0 || !myMeshList.get(0).isSurfaceMesh()) {
         FemMesh surf = new FemMesh(this);
         surf.setName(DEFAULT_SURFACEMESH_NAME);
         surf.setSurfaceRendering(getSurfaceRendering());
         surf.markSurfaceMesh(true);
         addMesh(surf, 0);
         return surf;
      }

      return myMeshList.get(0);
   }

   public void setAutoGenerateSurface(boolean val) {
      if (val != myAutoGenerateSurface) {
         myAutoGenerateSurface = val;
         if (myAutoGenerateSurface) {
            recreateSurfaceMesh();
         } else {
            FemMesh surf = null;
            if (myMeshList.size() > 0) {
               surf = myMeshList.get(0);
            }
            if (surf.isSurfaceMesh()) {
               removeMesh(surf);
            }
         }
      }
   }

   public boolean isAutoGeneratingSurface() {
      return myAutoGenerateSurface;
   }

   private FemMeshVertex addSurfaceVertex(PolygonalMesh mesh, FemNode3d n) {
      FemMeshVertex vtx = new FemMeshVertex(n);
      mySurfaceNodeMap.put(n, vtx);
      mesh.addVertex(vtx);
      return vtx;
   }

   // public int numNeighbors() {
      // int num = 0;
      // for (FemNode3d node : myNodes) {
         // num += node.getNodeNeighbors().size();
         // }
      // return num;
      // }

   //   /**
   //    * Makes sure that a polygonal mesh is a valid surface mesh: every vertex
   //    * must be a FemMeshVertex for a node in this FEM.
   //    */
   //   private boolean checkFemMesh(MeshBase mesh) {
      //	   for (Vertex3d vtx : mesh.getVertices()) {
         //		   if (vtx instanceof FemMeshVertex) {
            //			   if (!myNodes.contains(((FemMeshVertex)vtx).getPoint())) {
               //				   return false;
   //			   }
   //		   } else {
   //			   return false;
   //		   }
   //	   }
   //	   return false;
   //   }


   public FemMesh setSurfaceMesh(PolygonalMesh mesh) {

      // Create embedded mesh
      FemMesh surfMesh = getOrCreateEmptySurfaceMesh();
      FemMesh.createEmbedded(surfMesh, mesh);

      // build map
      mySurfaceNodeMap.clear();
      for (Vertex3d vtx : mesh.getVertices()) {
         if (vtx instanceof FemMeshVertex) {
            //throw new IllegalArgumentException(
            //   "vertex " + vtx.getIndex() + " is not a FemMeshVertex");
            FemMeshVertex fmv = (FemMeshVertex)vtx;
            FemNode3d node = (FemNode3d)fmv.getPoint();
            if (node == null || node.getGrandParent() != this) {
               throw new IllegalArgumentException(
                  "vertex " + vtx.getIndex()
                  + ": node is null or not part of this FEM");
            }
            mySurfaceNodeMap.put(node, fmv);
         } else {
            System.err.println("Warning: supplied mesh contains vertex that " +
            "is not a FemMeshVertex...\n  Internal mySurfaceNodeMap may not be valid" );
         }

      }
      // mySurfaceMesh = mesh;

      myAutoGenerateSurface = false;
      mySurfaceMeshValid = true;


      return surfMesh;
   }

   public void invalidateSurfaceMesh() {
      mySurfaceMeshValid = false;
      // myFineSurfaceValid = false;
   }

   protected void clearCachedData() {
      super.clearCachedData();
      // clearIncompressVariables();
      mySolveMatrix = null;
      // myActiveNodes = null;
      myBVTreeValid = false;
      myNodalIncompressConstraintsValidP = false;
      mySoftIncompMethodValidP = false;
      myHardIncompMethodValidP = false;
      myHardIncompConfigValidP = false;
      myNumTetElements = -1; // invalidates all element counts
   }

   public void componentChanged(ComponentChangeEvent e) {
      if (e.getComponent() == myElements || e.getComponent() == myNodes) {
         invalidateSurfaceMesh();
      }
      clearCachedData();
      if (e.getCode() == ComponentChangeEvent.Code.STRUCTURE_CHANGED) {
         // should invalidate elasticity

         // check if surface mesh deleted
         if (myMeshList != null && e.getComponent() == myMeshList) {
            if (myAutoGenerateSurface) {
               if (myMeshList.size() < 1 || 
               !myMeshList.get(0).isSurfaceMesh()) {
                  // re-generate
                  getOrCreateEmptySurfaceMesh();
                  mySurfaceMeshValid = false;
               }
            }
         }
      }
      notifyParentOfChange(e);
   }

   @Override
   protected void notifyStructureChanged(Object comp) {
      clearCachedData();
      super.notifyStructureChanged(comp);
   }

   public FemElement3d getSurfaceElement(Face face) {
      if (face.numVertices() != 3) {
         throw new IllegalArgumentException(
            "Face does not belong to a surface mesh");
      }
      FemNode3d[] nodes = new FemNode3d[3];
      for (int i = 0; i < 3; i++) {
         Vertex3d vtx = face.getVertex(i);
         if (!(vtx instanceof FemMeshVertex)) {
            throw new IllegalArgumentException(
               "Face does not belong to a surface mesh");
         }
         nodes[i] = (FemNode3d)((FemMeshVertex)vtx).getPoint();
      }
      // find the one neighboring elements that is common to all
      // three vertices
      LinkedList<FemElement3d> elems = new LinkedList<FemElement3d>();

      elems.addAll(nodes[0].getElementDependencies());
      elems.retainAll(nodes[1].getElementDependencies());
      elems.retainAll(nodes[2].getElementDependencies());

      if (elems.size() != 1) {
         return null;
      }
      else {
         return elems.get(0);
      }
   }

   /**
    * Returns the element within an FEM that contains a specified
    * point, or <code>null</code> if there is no such element.
    * 
    * @param pnt Point for which containing element is desired.
    * @return containing element, or null.
    */
   public FemElement3d findContainingElement(Point3d pnt) {
      BVTree bvtree = getBVTree();
      ArrayList<BVNode> nodes = new ArrayList<BVNode>(16);
      bvtree.intersectPoint(nodes, pnt);
      // System.out.println ("num nodes " + nodes.size());
      if (nodes.size() == 0) {
         return null;
      }
      for (BVNode n : nodes) {
         Boundable[] elements = n.getElements();
         for (int i = 0; i < elements.length; i++) {
            if (((FemElement3d)elements[i]).isInside(pnt)) {
               return (FemElement3d)elements[i];
            }
         }
      }
      return null;
   }

   /**
    * Returns the nearest surface element to a specified point,
    * which is found by projecting the point onto the FEM surface.
    * The location of the projection is returned in <code>loc</code>.
    * 
    * @param loc Projected location of the point onto the surface.
    * @param pnt Point for which nearest surface element is desired.
    * @return Nearest surface element.
    */
   public FemElement3d findNearestSurfaceElement(Point3d loc, Point3d pnt) {
      Vector2d coords = new Vector2d();
      Face face = BVFeatureQuery.getNearestFaceToPoint(
         loc, coords, getSurfaceMesh(), pnt);
      FemElement3d elem = getSurfaceElement(face);
      if (elem == null) {
         throw new InternalErrorException("surface element not found for face");
      }
      return elem;
   }

   /**
    * Returns the element within an FEM that contains a specified point, or if
    * there is no such element, finds the closest surface element.
    * 
    * @param loc Location of the point, within the FEM or projected onto the
    * surface.
    * @param pnt Point for which the nearest element is desired.
    * @return Nearest element.
    */
   public FemElement3d findNearestElement(Point3d loc, Point3d pnt) {
      FemElement3d e = findContainingElement(pnt);
      if (e == null) {
         e = findNearestSurfaceElement(loc, pnt);
      }
      else {
         loc.set(pnt);
      }
      return e;
   }

   /**
    * Finds the nearest node to a specified point that is within
    * a specified maximum distance. If no node is within the
    * specified maximum distance, <code>null</code> is returned.
    * 
    * @param pnt Point for which the nearest node should be located
    * @param maxDist Maximum distance that the node must be from the
    * point. If <code>maxDist</code> < 0, then <code>null</code>
    * will be returned.
    * @return Nearest point within the prescribed distance, or <code>null</code>
    * if there is no such point
    */
   public FemNode3d findNearestNode(Point3d pnt, double maxDist) {
      if (maxDist < 0) {
         return null;
      }
      BVTree bvtree = getBVTree();
      ArrayList<BVNode> nodes = new ArrayList<BVNode>();
      bvtree.intersectSphere(nodes, pnt, maxDist);
      FemNode3d nearest = null;
      double dist = 1 + 2 * maxDist;
      for (BVNode n : nodes) {
         Boundable[] elements = n.getElements();
         for (int i = 0; i < elements.length; i++) {
            FemElement3d e = (FemElement3d)elements[i];
            for (int k = 0; k < e.numNodes(); k++) {
               double d = e.myNodes[k].getPosition().distance(pnt);
               if (d < dist && d <= maxDist) {
                  dist = d;
                  nearest = e.myNodes[k];
               }
            }
         }
      }
      return nearest;
   }

   public void updateSlavePos() {
      super.updateSlavePos();

      // getSurfaceMesh().notifyVertexPositionsModified();
      // getSurfaceMesh().updateFaceNormals();
      //      if (myCollisionMesh != null) {
      //         myCollisionMesh.notifyVertexPositionsModified();
      //         myCollisionMesh.updateFaceNormals();
      //      }
      //      for (PolygonalMesh mesh : mySubSurfaces) {
      //         mesh.notifyVertexPositionsModified();
      //         mesh.updateFaceNormals();
      //      }
      //      if (myFineSurface != null) {
      //         myFineSurface.updateSlavePos();
      //      }
      myMeshList.updateSlavePos();
      myBVTreeValid = false;
   }

   /**
    * Checks for inverted elements. The number of inverted elements is stored in
    * myNumInverted. The minimum determinant, and the associated element, is
    * stored in myMinDetJ and myMinDetJElement.
    */
   private void updateVolumeAndCheckForInversion() {
      // special implementation of updateVolume that checks for inverted
      // Jacobians
      double volume = 0;
      myMinDetJ = Double.MAX_VALUE;
      myMinDetJElement = null;
      myNumInverted = 0;
      for (FemElement3d e : getElements()) {
         FemMaterial mat = getElementMaterial(e);
         double detJ = e.computeVolumes();
         e.setInverted(false);
         if (!(mat instanceof LinearMaterial)) {
            if (detJ < myMinDetJ) {
               if (!e.materialsAreInvertible()) {
                  myMinDetJ = detJ;
                  myMinDetJElement = e;
               }
            }
            if (detJ <= 0) {
               if (!e.materialsAreInvertible()) {
                  e.setInverted(true);
                  myNumInverted++;
               }
            }
         }
         volume += e.getVolume();
      }
      myVolume = volume;
      myVolumeValid = true;
   }

   /**
    * Mark as inverted any element whose rest Jacobians are inverted.
    * This is intended as a debugging tool that can be called by the
    * application to check for inverted rest elements.
    */
   boolean done = false;

   public int markInvertedRestElements() {
      int cnt = 0;
      for (FemElement3d e : getElements()) {
         IntegrationData3d[] idata = e.getIntegrationData();
         boolean inverted = false;
         for (int i = 0; i < idata.length; i++) {
            if (idata[i].myDetJ0 <= 0) {
               inverted = true;
            }
         }
         if (inverted) {
            e.setInverted(true);
            if (!done) {
               System.out.println("elem=" + e.getNumber());
               done = true;
            }

            cnt++;
         }
      }
      return cnt;
   }

   public void recursivelyFinalizeAdvance(
      StepAdjustment stepAdjust, double t0, double t1, int flags, int level) {

      // we can update volumes and check for inversion in about 1/20 the time
      // it takes to update forces, so we do that instead:
      updateVolumeAndCheckForInversion();
      if (stepAdjust != null && myMinDetJ <= detJStepReductionLimit) {
         stepAdjust.recommendAdjustment(
            0.5, "detJ "+myMinDetJ+" below limit of "+detJStepReductionLimit +
            ", element " + myMinDetJElement.getNumber());
      }
   }

   public void invalidateStressAndStiffness() {
      super.invalidateStressAndStiffness();
      // should invalidate matrices for incompressibility here. However, at the
      // moment these are being rebuilt for each calculation anyway
   }
   
   // update, if necessary, nodal rest volumes
   protected void invalidateNodalRestVolumes() {
      myNodalRestVolumesValidP = false;
   }

   public void invalidateRestData() {
      super.invalidateRestData();
      invalidateNodalRestVolumes();
   }

   public void resetRestPosition() {
      for (FemNode3d n : myNodes) {
         n.resetRestPosition();
      }
      invalidateRestData();
      notifyParentOfChange(new ComponentChangeEvent(Code.STRUCTURE_CHANGED));
   }

   public double updateConstraints(double t, int flags) {
      if (!myVolumeValid) {
         updateVolume();
      }
      if (getHardIncompMethod() != IncompMethod.OFF) {
         updateHardIncompInfo(t);
      }
      return 0;
   }

   public int setBilateralImpulses(VectorNd lam, double h, int idx) {
      IncompMethod hardIncomp = getHardIncompMethod();
      if (hardIncomp == IncompMethod.NODAL) {
         lam.getSubVector(idx, myIncompressLambda);
         idx += myNumIncompressConstraints;
      }
      else if (hardIncomp == IncompMethod.ELEMENT) {
         double[] buf = lam.getBuffer();
         for (int i = 0; i < myElements.size(); i++) {
            FemElement3d e = myElements.get(i);
            if (e.getIncompressIndex() != -1) {
               for (int k = 0; k < e.numPressureVals(); k++) {
                  e.myLagrangePressures[k] = buf[idx++];
               }
            }
         }
      }
      return idx;
   }

   public void zeroImpulses() {
      IncompMethod hardIncomp = getHardIncompMethod();
      if (hardIncomp == IncompMethod.NODAL) {
         myIncompressLambda.setZero();
      }
      else if (hardIncomp == IncompMethod.ELEMENT) {
         for (int i = 0; i < myElements.size(); i++) {
            FemElement3d e = myElements.get(i);
            if (e.getIncompressIndex() != -1) {
               for (int k = 0; k < e.numPressureVals(); k++) {
                  e.myLagrangePressures[k] = 0;
               }
            }
         }
      }
   }

   public int getBilateralImpulses(VectorNd lam, int idx) {
      IncompMethod hardIncomp = getHardIncompMethod();
      if (hardIncomp == IncompMethod.NODAL) {
         lam.setSubVector(idx, myIncompressLambda);
         idx += myNumIncompressConstraints;
      }
      else if (hardIncomp == IncompMethod.ELEMENT) {
         double[] buf = lam.getBuffer();
         for (int i = 0; i < myElements.size(); i++) {
            FemElement3d e = myElements.get(i);
            if (e.getIncompressIndex() != -1) {
               for (int k = 0; k < e.numPressureVals(); k++) {
                  buf[idx++] = e.myLagrangePressures[k];
               }
            }
         }
      }
      return idx;
   }

   public void getBilateralSizes(VectorNi sizes) {
      IncompMethod hardIncomp = getHardIncompMethod();
      if (hardIncomp != IncompMethod.OFF) {
         if (!myHardIncompConfigValidP) {
            configureHardIncomp();
         }
         if (hardIncomp == IncompMethod.NODAL) {
            for (int i = 0; i < myNumIncompressConstraints; i++) {
               sizes.append(1);
            }
         }
         else if (hardIncomp == IncompMethod.ELEMENT) {
            for (int i = 0; i < myElements.size(); i++) {
               FemElement3d e = myElements.get(i);
               if (e.getIncompressIndex() != -1) {
                  sizes.append(e.numPressureVals());
               }
            }
         }
      }
   }

   // DIVBLK
   public int addBilateralConstraints(
      SparseBlockMatrix GT, VectorNd dg, int numb, IntHolder changeCnt) {
      if (myHardIncompConstraintsChangedP) {
         changeCnt.value++;
         myHardIncompConstraintsChangedP = false;
      }
      IncompMethod hardIncomp = getHardIncompMethod();
      if (hardIncomp != IncompMethod.OFF) {

         // add the necessary columns to the matrix
         int ncons = myNumIncompressConstraints;

         int bj = GT.numBlockCols();
         if (hardIncomp == IncompMethod.NODAL) {
            // for TET case, ncons equals number of constraint blocks
            for (int j = 0; j < ncons; j++) {
               GT.addCol(1);
            }
            // For controllable node, add the incompressibility constraint
            for (FemNode3d n : myNodes) {
               if (n.getIncompressIndex() != -1) {
                  for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
                     // if (isControllable (nbr.myNode)) {
                        GT.addBlock(
                           nbr.myNode.getSolveIndex(), bj, nbr.myDivBlk);
                        // }
                  }
                  bj++;
               }
            }
         }
         else if (hardIncomp == IncompMethod.ELEMENT) {
            for (FemElement3d e : myElements) {
               if (e.getIncompressIndex() != -1) {
                  MatrixBlock[] constraints;
                  constraints = e.getIncompressConstraints();
                  for (int i = 0; i < e.numNodes(); i++) {
                     FemNode3d n = e.myNodes[i];
                     // if (isControllable (n)) {
                        GT.addBlock(n.getSolveIndex(), bj, constraints[i]);
                        // }
                  }
                  bj++;
               }
            }
         }
         if (dg != null) {
            double[] dbuf = dg.getBuffer();
            for (int i = 0; i < ncons; i++) {
               dbuf[numb + i] = myDg.get(i);
            }
         }
         numb += ncons;
      }
      return numb;
   }

   public int getBilateralInfo(ConstraintInfo[] ginfo, int idx) {
      IncompMethod hardIncomp = getHardIncompMethod();
      if (hardIncomp != IncompMethod.OFF) {
         int ncols = myNumIncompressConstraints;
         int ci;

         double damping = 0;
         if (myIncompCompliance != 0) {
            // set critical damping
            double mass = getMass() / myNumIncompressConstraints;
            damping = 2 * Math.sqrt(mass / myIncompCompliance);
         }

         if (!myVolumeValid) {
            updateVolume();
         }
         if (hardIncomp == IncompMethod.NODAL) {
            for (ci = 0; ci < ncols; ci++) {
               ConstraintInfo gi = ginfo[idx + ci];
               gi.dist = 0; // values will be accumulated below
               gi.compliance = myIncompCompliance;
               gi.damping = damping;
            }

            for (FemElement3d elem : myElements) {
               if (elem instanceof TetElement) {
                  double tdiv = getVolumeError(elem);

                  for (int j = 0; j < 4; j++) {
                     FemNode3d node = elem.myNodes[j];
                     if ((ci = node.getIncompressIndex()) != -1) {
                        // if the tet contains inactive nodes, should
                        // we use something other than 0.25*tdiv?
                        // Also, why do we use +tdiv for NODE and -tdiv for ELEM?
                        ginfo[idx + ci].dist += tdiv * 0.25;
                     }
                  }
               }
               else if (elem.integrationPointsMapToNodes()) {
                  FemNode3d[] nodes = elem.myNodes;
                  IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
                  IntegrationData3d[] idata = elem.getIntegrationData();
                  double verr;
                  for (int i = 0; i < nodes.length; i++) {
                     if ((ci = nodes[i].getIncompressIndex()) != -1) {
                        verr = getLocalVolumeError(nodes, ipnts[i], idata[i]);
                        ginfo[idx + ci].dist += verr;
                     }
                  }
               }
            }
         }
         else if (hardIncomp == IncompMethod.ELEMENT) {
            ci = idx;
            for (FemElement3d e : myElements) {
               if (e.getIncompressIndex() != -1) {
                  for (int k = 0; k < e.numPressureVals(); k++) {
                     ConstraintInfo gi = ginfo[ci++];
                     gi.dist = e.myVolumes[k] - e.myRestVolumes[k];
                     gi.compliance = myIncompCompliance;
                     gi.damping = damping;
                  }
               }
            }
         }
         idx += ncols;
      }
      return idx;
   }

//   Used in createSurface:
//
//   private void addEdgeNodesToFace(FaceNodes3d face) {
//      LinkedList<FemNode3d> allNodes = new LinkedList<FemNode3d>();
//      FemNode3d[] nodes = face.getNodes();
//      for (int i = 0; i < nodes.length; i++) {
//         FemNode3d n0 = nodes[i];
//         FemNode3d n1 = (i < nodes.length - 1) ? nodes[i + 1] : nodes[0];
//         ArrayList<FemNode3d> edgeNodes = getEdgeNodes(n0, n1);
//         allNodes.add(n0);
//         if (edgeNodes != null) {
//            allNodes.addAll(edgeNodes);
//         }
//      }
//      face.setAllNodes(allNodes.toArray(new FemNode3d[0]));
//   }

//   PointFem3dAttachment getEdgeAttachment(FemNode3d node) {
//      if (node.getAttachment() instanceof PointFem3dAttachment) {
//         PointFem3dAttachment attachment =
//            (PointFem3dAttachment)node.getAttachment();
//         if (attachment.numMasters() == 2) {
//            return attachment;
//         }
//      }
//      return null;
//   }

   private FemElement3d[] elementsWithEdge(FemNode3d n0, FemNode3d n1) {
      HashSet<FemElement3d> allElems = new HashSet<FemElement3d>();
      LinkedList<FemElement3d> elemsWithEdge =
      new LinkedList<FemElement3d>();

      allElems.addAll(n0.getElementDependencies());
      allElems.addAll(n1.getElementDependencies());
      for (FemElement3d e : allElems) {
         if (e.hasEdge(n0, n1)) {
            elemsWithEdge.add(e);
         }
      }
      return elemsWithEdge.toArray(new FemElement3d[0]);
   }

   private int numElementsWithEdge(FemNode3d n0, FemNode3d n1) {
      return elementsWithEdge(n0, n1).length;
   }

//   private ArrayList<FemNode3d> getEdgeNodes(
//      FemNode3d n0, FemNode3d n1) {
//
//      LinkedList<DynamicAttachment> masters = n0.getMasterAttachments();
//      if (masters == null) {
//         return null;
//      }
//      ArrayList<FemNode3d> nodes = new ArrayList<FemNode3d>();
//      ArrayList<Double> weights = new ArrayList<Double>();
//      for (DynamicAttachment a : masters) {
//         if (a instanceof PointFem3dAttachment) {
//            PointFem3dAttachment pfa = (PointFem3dAttachment)a;
//            if (pfa.numMasters() == 2 && pfa.getSlave() instanceof FemNode3d) {
//               FemNode3d slaveNode = (FemNode3d)pfa.getSlave();
//               if (slaveNode.getGrandParent() == this &&
//                  containsNode(n1, pfa.getMasters())) {
//                  nodes.add(slaveNode);
//                  double w = pfa.getCoordinates().get(0);
//                  if (n0 == pfa.getMasters()[0]) {
//                     weights.add(w);
//                  }
//                  else {
//                     weights.add(1 - w);
//                  }
//               }
//            }
//         }
//      }
//      // just do a bubble sort; easier to implement
//      for (int i = 0; i < nodes.size() - 1; i++) {
//         for (int j = i + 1; j < nodes.size(); j++) {
//            if (weights.get(j) < weights.get(i)) {
//               double w = weights.get(i);
//               weights.set(i, weights.get(j));
//               weights.set(j, w);
//               FemNode3d n = nodes.get(i);
//               nodes.set(i, nodes.get(j));
//               nodes.set(j, n);
//            }
//         }
//      }
//      return nodes;
//   }

   static PointFem3dAttachment getEdgeAttachment(
      FemNode3d n0, FemNode3d n1) {
      double weight = 0;
      PointFem3dAttachment attachment = null;
      LinkedList<DynamicAttachment> masters = n0.getMasterAttachments();
      if (masters == null) {
         return null;
      }
      for (DynamicAttachment a : masters) {
         if (a instanceof PointFem3dAttachment) {
            PointFem3dAttachment pfa = (PointFem3dAttachment)a;
            if (pfa.numMasters() == 2 && pfa.getSlave() instanceof FemNode3d) {
               if (containsNode(n1, pfa.getMasters())) {
                  double w = pfa.getCoordinates().get(0);
                  if (Math.abs(w - 0.5) < Math.abs(weight - 0.5)) {
                     // this point is closer
                     attachment = pfa;
                  }
               }
            }
         }
      }
      return attachment;
   }

   private FemNode3d createNode(FemNode3d[] nodes) {
      Point3d pos = new Point3d();
      FemNode3d node = new FemNode3d();
      for (FemNode3d n : nodes) {
         pos.add(n.getPosition());
      }
      pos.scale(1.0 / nodes.length);
      node.setPosition(pos);
      pos.setZero();
      for (FemNode3d n : nodes) {
         pos.add(n.getRestPosition());
      }
      pos.scale(1.0 / nodes.length);
      node.setRestPosition(pos);
      return node;
   }

   static PointFem3dAttachment getFaceAttachment(FemNode3d node) {
      if (node.getAttachment() instanceof PointFem3dAttachment) {
         PointFem3dAttachment attachment =
         (PointFem3dAttachment)node.getAttachment();
         if (attachment.numMasters() > 2) {
            return attachment;
         }
      }
      return null;
   }

   private FemElement3d[] elementsWithFace(
      FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3) {
      HashSet<FemElement3d> allElems = new HashSet<FemElement3d>();
      LinkedList<FemElement3d> elemsWithFace =
      new LinkedList<FemElement3d>();

      allElems.addAll(n0.getElementDependencies());
      allElems.addAll(n1.getElementDependencies());
      allElems.addAll(n2.getElementDependencies());
      allElems.addAll(n3.getElementDependencies());
      for (FemElement3d e : allElems) {
         if (e.hasFace(n0, n1, n2, n3)) {
            elemsWithFace.add(e);
         }
      }
      return elemsWithFace.toArray(new FemElement3d[0]);
   }

   private int numElementsWithFace(
      FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3) {
      return elementsWithFace(n0, n1, n2, n3).length;
   }

   static boolean containsNode(FemNode3d n, FemNode[] nodes) {
      for (int i = 0; i < nodes.length; i++) {
         if (nodes[i] == n) {
            return true;
         }
      }
      return false;
   }

   private static double[] faceWeights = new double[] {0.25, 0.25, 0.25, 0.25};
   private static double[] edgeWeights = new double[] { 0.5, 0.5 };

   private PointFem3dAttachment getFaceAttachment(
      FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3) {
      VectorNd idealWeights = new VectorNd(faceWeights);
      double mindist = Double.MAX_VALUE;
      PointFem3dAttachment attachment = null;
      LinkedList<DynamicAttachment> masters = n0.getMasterAttachments();
      if (masters == null) {
         return null;
      }
      for (DynamicAttachment a : masters) {
         if (a instanceof PointFem3dAttachment) {
            PointFem3dAttachment pfa = (PointFem3dAttachment)a;
            if (pfa.numMasters() == 4 && pfa.getSlave() instanceof FemNode3d) {
               FemNode[] nodes = pfa.getMasters();
               if (containsNode(n1, nodes) &&
               containsNode(n2, nodes) &&
               containsNode(n3, nodes)) {
                  double d = idealWeights.distance(pfa.getCoordinates());
                  if (d < mindist) {
                     mindist = d;
                     attachment = pfa;
                  }
               }
            }
         }
      }
      return attachment;
   }

   HexElement createHex(FemNode3d[] nodes,
      int i0, int i1, int i2, int i3,
      int i4, int i5, int i6, int i7) {
      return new HexElement(nodes[i0], nodes[i1], nodes[i2], nodes[i3],
         nodes[i4], nodes[i5], nodes[i6], nodes[i7]);
   }

   public void subdivideHexs(List<HexElement> hexs) {
      for (HexElement hex : hexs) {
         subdivideHex(hex);
      }
   }

   public void subdivideHex(HexElement hex) {
      if (hex.getParent() != myElements) {
         throw new IllegalArgumentException(
            "Hex is not contained in this model");
      }
      LinkedList<FemNode3d> addedNodes = new LinkedList<FemNode3d>();
      LinkedList<FemElement3d> addedElements =
      new LinkedList<FemElement3d>();
      LinkedList<DynamicAttachment> addedAttachments =
      new LinkedList<DynamicAttachment>();
      LinkedList<DynamicAttachment> removedAttachments =
      new LinkedList<DynamicAttachment>();

      int idx = 0;
      FemNode3d[] nodes = new FemNode3d[27];
      for (FemNode3d n : hex.getNodes()) {
         nodes[idx++] = n;
      }
      for (int i = 0; i < 12; i++) {
         // for each edge ...
         FemNode3d n0 = nodes[HexElement.edgeIdxs[2 * i + 0]];
         FemNode3d n1 = nodes[HexElement.edgeIdxs[2 * i + 1]];
         PointFem3dAttachment edgeAttach = getEdgeAttachment(n0, n1);
         FemNode3d edgeNode;
         if (edgeAttach != null) {
            // use the existing mid node as the edge node
            edgeNode = (FemNode3d)edgeAttach.getSlave();
            if (numElementsWithEdge(n0, n1) == 1) {
               // don't need the attachment any more, so remove it
               removedAttachments.add(edgeAttach);
            }
         }
         else {
            edgeNode = createNode(new FemNode3d[] { n0, n1 });
            addedNodes.add(edgeNode);
            if (numElementsWithEdge(n0, n1) > 1) {
               PointFem3dAttachment a =
               new PointFem3dAttachment(
                  edgeNode, new FemNode[] { n0, n1 }, edgeWeights);
               addedAttachments.add(a);
            }
         }
         nodes[idx++] = edgeNode;
      }
      for (int i = 0; i < 6; i++) {
         // for each face ...
         FemNode3d n0 = nodes[HexElement.faceIdxs[4 * i + 0]];
         FemNode3d n1 = nodes[HexElement.faceIdxs[4 * i + 1]];
         FemNode3d n2 = nodes[HexElement.faceIdxs[4 * i + 2]];
         FemNode3d n3 = nodes[HexElement.faceIdxs[4 * i + 3]];
         PointFem3dAttachment faceAttach = getFaceAttachment(n0, n1, n2, n3);
         FemNode3d faceNode;
         if (faceAttach != null) {
            // use the existing center node as the face node
            faceNode = (FemNode3d)faceAttach.getSlave();
            if (numElementsWithFace(n0, n1, n2, n3) == 1) {
               // don't need the attachment any more, so remove it
               removedAttachments.add(faceAttach);
            }
         }
         else {
            faceNode = createNode(new FemNode3d[] { n0, n1, n2, n3 });
            addedNodes.add(faceNode);
            if (numElementsWithFace(n0, n1, n2, n3) > 1) {
               PointFem3dAttachment a =
               new PointFem3dAttachment(
                  faceNode, new FemNode[] { n0, n1, n2, n3 }, faceWeights);
               addedAttachments.add(a);
            }
         }
         nodes[idx++] = faceNode;
      }

      FemNode3d centerNode = createNode((FemNode3d[])hex.getNodes());
      addedNodes.add(centerNode);
      nodes[idx++] = centerNode;

      addedElements.add(createHex(nodes, 0, 8, 20, 11, 16, 24, 26, 23));
      addedElements.add(createHex(nodes, 8, 1, 9, 20, 24, 17, 21, 26));
      addedElements.add(createHex(nodes, 20, 9, 2, 10, 26, 21, 18, 25));
      addedElements.add(createHex(nodes, 11, 20, 10, 3, 23, 26, 25, 19));
      addedElements.add(createHex(nodes, 16, 24, 26, 23, 4, 12, 22, 15));
      addedElements.add(createHex(nodes, 24, 17, 21, 26, 12,
         
         5, 13, 22));
      addedElements.add(createHex(nodes, 26, 21, 18, 25, 22, 13, 6, 14));
      addedElements.add(createHex(nodes, 23, 26, 25, 19, 15, 22, 14, 7));

      for (DynamicAttachment a : removedAttachments) {
         removeAttachment(a);
      }
      removeElement(hex);
      for (FemNode3d n : addedNodes) {
         addNode(n);
      }
      for (FemElement3d e : addedElements) {
         addElement(e);
      }
      for (DynamicAttachment a : addedAttachments) {
         addAttachment(a);
      }
   }

   public void dispose() {
   }

   public void useAnsysNumbering() {
      myNodes.setNumberingStartAtOne();
   }

   @Override
   public void scaleDistance(double s) {
      super.scaleDistance(s);
      myVolume *= (s * s * s);
   }

   /**
    * {@inheritDoc}
    */
    public boolean isDuplicatable() {
       return true;
    }

    public boolean getCopyReferences(
       List<ModelComponent> refs, ModelComponent ancestor) {

       return true;
    }

    public void getAuxStateComponents(List<HasAuxState> comps, int level) {
       comps.add(this);
    }

    public void advanceAuxState(double t0, double t1) {
       for (int i = 0; i < myElements.size(); i++) {
          FemElement3d e = myElements.get(i);
          FemMaterial mat = getElementMaterial(e);
          if (mat.getViscoBehavior() != null) {
             ViscoelasticBehavior veb = mat.getViscoBehavior();
             IntegrationData3d[] idata = e.getIntegrationData();
             for (int k = 0; k < idata.length; k++) {
                ViscoelasticState state = idata[k].getViscoState();
                if (state == null) {
                   state = veb.createState();
                   idata[k].setViscoState(state);
                }
                veb.advanceState(state, t0, t1);
             }
          }
       }
    }

    /**
     * {@inheritDoc}
     */
     public void skipAuxState(DataBuffer data) {

        int dsize = data.zget();
        int zsize = data.zget();
        data.dskip (dsize);
        data.zskip (zsize);
     }

     public void getAuxState(DataBuffer data) {

        int didx0 = data.dsize();
        int zidx0 = data.zsize();
        data.zput (0);    // reserve space for storing dsize and zsize
        data.zput (0);

        for (int i = 0; i < myElements.size(); i++) {
           IntegrationData3d[] idata = myElements.get(i).getIntegrationData();
           for (int k = 0; k < idata.length; k++) {
              idata[k].getState(data);
           }          
        }
        // store the amount of space used, for use by increaseAuxStateOffsets
        data.zset (zidx0, data.dsize()-didx0);
        data.zset (zidx0+1, data.zsize()-zidx0-2);
     }

     public void getInitialAuxState(
        DataBuffer newData, DataBuffer oldData) {

        int zidx0 = newData.zsize();
        newData.zput (0);     // make space for size spaces, to be stored below
        newData.zput (0);
        
        for (int i = 0; i < myElements.size(); i++) {
           IntegrationData3d[] idata = myElements.get(i).getIntegrationData();
           for (int k=0; k<idata.length; k++) {
              idata[k].getZeroState (newData);
           }
        }
        // store the amount of space used, for use by increaseAuxStateOffsets
        newData.zset (zidx0, 0);
        newData.zset (zidx0+1, newData.zsize()-zidx0-2);
     }

     public void setAuxState(DataBuffer data) {

        int dsize = data.zget(); // should use this for sanity checking?
        int zsize = data.zget();

        for (int i = 0; i < myElements.size(); i++) {
           IntegrationData3d[] idata = myElements.get(i).getIntegrationData();
           for (int k = 0; k < idata.length; k++) {
              idata[k].setState (data);
           }
        }
     }

     @Override
     public FemModel3d copy(
        int flags, Map<ModelComponent,ModelComponent> copyMap) {

        if (copyMap == null) {
           copyMap = new HashMap<ModelComponent,ModelComponent>();
           flags = CopyableComponent.COPY_REFERENCES;
        }

        FemModel3d fem = (FemModel3d)super.copy(flags, copyMap);

        for (FemNode3d n : myNodes) {
           FemNode3d newn = n.copy(flags, copyMap);
           newn.setName(n.getName());
           copyMap.put(n, newn);
           fem.myNodes.addNumbered(newn, n.getNumber());
           fem.myNodes.setRenderProps(myNodes.getRenderProps());
        }
        for (FemElement3d e : myElements) {
           FemElement3d newe = e.copy(flags, copyMap);
           newe.setName(e.getName());
           copyMap.put(e, newe);
           fem.myElements.addNumbered(newe, e.getNumber());
           fem.myElements.setRenderProps(myElements.getRenderProps());
        }
        for (FemMarker m : myMarkers) {
           FemMarker newm = m.copy(flags, copyMap);
           newm.setName(m.getName());
           fem.myMarkers.addNumbered(newm, m.getNumber());
           fem.myMarkers.setRenderProps(myMarkers.getRenderProps());
        }
        for (DynamicAttachment a : myAttachments) {
           DynamicAttachment newa = a.copy(flags, copyMap);
           newa.setName(a.getName());
           fem.myAttachments.addNumbered(newa, a.getNumber());
        }

        fem.ansysElemProps = new HashMap<FemElement3d,int[]>();
        for (Map.Entry<FemElement3d,int[]> ent : ansysElemProps.entrySet()) {
           FemElement3d newe = (FemElement3d)copyMap.get(ent.getKey());
           int[] props = ArraySupport.copy(ent.getValue());
           fem.ansysElemProps.put(newe, props);
        }

        // if (mySurfaceMeshValid) {
           // if (mySurfaceMesh != null) {
              // ArrayList<FemMeshVertex> vtxs = new ArrayList<FemMeshVertex>();
              // for (Vertex3d v : mySurfaceMesh.getVertices()) {
                 // FemNode n = ((FemMeshVertex)v).getPoint();
                 // FemMeshVertex newv = new FemMeshVertex((FemNode)copyMap.get(n));
                 // vtxs.add(newv);
                 // }
              // PolygonalMesh mesh = mySurfaceMesh.copyWithVertices(vtxs);
              // fem.setSurfaceMesh(mesh);
              // }
           // else {
              // fem.mySurfaceMesh = null;
              // }
           // }
        // else {
      //         fem.mySurfaceMeshValid = false;
      //         fem.mySurfaceMesh = null;
           // }
      //      fem.myFineSurfaceValid = false;
      //      fem.myFineSurface = null;
      //
      //      fem.myCollisionMesh = null;
      //      if (myCollisionMesh != null) {
      //         fem.myCollisionMesh = myCollisionMesh; // XXX duplicate?
      //      }

      for (FemMesh mc : myMeshList) {
         FemMesh newFmc = mc.copy(flags, copyMap);
         fem.addMesh(newFmc);
      }
      fem.myAutoGenerateSurface = myAutoGenerateSurface;
      fem.mySurfaceMeshValid = false;  // trigger update

      fem.mySurfaceNodeMap = new HashMap<FemNode3d,FemMeshVertex>();
      for (Entry<FemNode3d,FemMeshVertex> e : fem.mySurfaceNodeMap.entrySet()) {
         FemNode3d key = (FemNode3d)copyMap.get (e.getKey());
         // find corresponding vertex?
         // fem.mySurfaceNodeMap.put(key, vertex);
      }

        // fem.setIncompressible (myIncompressibleP);
        // fem.setSubSurfaceRendering(mySubSurfaceRendering);

        fem.setElementWidgetSizeMode(myElementWidgetSizeMode);
        if (myElementWidgetSizeMode == PropertyMode.Explicit) {
           fem.setElementWidgetSize(myElementWidgetSize);
        }

        fem.clearCachedData();

        fem.myAABBTree = null;
        fem.myBVTreeValid = false;

        fem.mySolveMatrixFile = null;

        fem.myNumIncompressConstraints = 0;
        fem.myHardIncompUpdateTime = -1;
        fem.myHardIncompConstraintsChangedP = true;

        // fem.myFreeVolume = myFreeVolume;

        // fem.myClearMeshColoring = myClearMeshColoring;
      fem.myComputeNodalStress = myComputeNodalStress;
      fem.myComputeNodalStrain = myComputeNodalStrain;

        fem.myHardIncompMethod = myHardIncompMethod;
        fem.myHardIncompMethodValidP = myHardIncompMethodValidP;
        fem.mySoftIncompMethod = mySoftIncompMethod;
        fem.mySoftIncompMethodValidP = mySoftIncompMethodValidP;

        fem.myNodalIncompBlocksAllocatedP = false;
        fem.myNodalIncompConstraintsAllocatedP = false;

        fem.myPressures = new VectorNd(MAX_PRESSURE_VALS);
        fem.myKp = new double[MAX_PRESSURE_VALS];
        fem.myNodalConstraints = new Vector3d[MAX_NODAL_INCOMP_NODES];
        for (int i = 0; i < MAX_NODAL_INCOMP_NODES; i++) {
           fem.myNodalConstraints[i] = new Vector3d();
        }

        return fem;
     }

     public ColorMapBase getColorMap() {
        return myColorMap;
     }

     public void setColorMap(ColorMapBase colorMap) {
        myColorMap = colorMap;
        myColorMapMode =
        PropertyUtils.propagateValue(
           this, "colorMap", colorMap, myColorMapMode);
     }

     public PropertyMode getColorMapMode() {
        return myColorMapMode;
     }

     public void setColorMapMode(PropertyMode mode) {
        if (mode != myColorMapMode) {
           myColorMapMode = PropertyUtils.setModeAndUpdate(
              this, "colorMap", myColorMapMode, mode);
        }
     }

     private FemMesh getSurfaceFemMesh() {
        for (FemMesh fmc : myMeshList) {
           if (fmc.isSurfaceMesh ()) {
              return fmc;
           }
        }
        return null;
     }
     
     public DeformableCollisionData createCollisionData() {

        if (isAutoGeneratingSurface()) {
           return new FemCollisionData (this, getSurfaceMesh ());
           //return new EmbeddedCollisionData (this, getSurfaceFemMesh ());
        }

        FemMesh fm = getSurfaceFemMesh();
        if (fm != null) {
           return new EmbeddedCollisionData (this, fm);
        }
        
        return null;
     }
     
     public boolean isAbortOnInvertedElements() {
        return myAbortOnInvertedElems;
     }
     
     public void setAbortOnInvertedElements(boolean set) {
        myAbortOnInvertedElems = set;
     }
     
     public void setWarnOnInvertedElements(boolean set) {
        myWarnOnInvertedElems = set;
     }
     
     public boolean isWarnOnInvertedElements() {
        return myWarnOnInvertedElems;
     }

}
