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

import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.IncompressibleMaterial;
import artisynth.core.materials.IncompressibleMaterial.BulkPotential;
import artisynth.core.materials.SolidDeformation;
import artisynth.core.materials.ViscoelasticBehavior;
import artisynth.core.materials.ViscoelasticState;
import artisynth.core.mechmodels.BodyConnector;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.ConnectableBody;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.HasAuxState;
import artisynth.core.mechmodels.HasSurfaceMesh;
import artisynth.core.mechmodels.MechSystemModel;
import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.mechmodels.MeshComponentList;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachable;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.PointParticleAttachment;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ArtisynthIO;
import artisynth.core.util.IntegerToken;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import maspack.geometry.AABBTree;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.BVNode;
import maspack.geometry.BVTree;
import maspack.geometry.Boundable;
import maspack.geometry.Face;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.DenseMatrix;
import maspack.matrix.EigenDecomposition;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3x1Block;
import maspack.matrix.Matrix3x3Block;
import maspack.matrix.Matrix3x6Block;
import maspack.matrix.Matrix6d;
import maspack.matrix.Matrix6dBlock;
import maspack.matrix.Matrix6x3Block;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixNd;
import maspack.matrix.NumericalException;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
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
import maspack.render.RenderList;
import maspack.render.Renderer;
import maspack.render.color.ColorMapBase;
import maspack.render.color.HueColorMap;
import maspack.spatialmotion.SpatialInertia;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import maspack.util.ArraySupport;
import maspack.util.DataBuffer;
import maspack.util.DoubleInterval;
import maspack.util.EnumRange;
import maspack.util.FunctionTimer;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.Range;
import maspack.util.ReaderTokenizer;
import maspack.util.TestException;

public class FemModel3d extends FemModel
implements TransformableGeometry, ScalableUnits, MechSystemModel, Collidable,
CopyableComponent, HasAuxState, HasSurfaceMesh,
PointAttachable, ConnectableBody {

   protected FemModelFrame myFrame;
   protected FrameFem3dConstraint myFrameConstraint;
   protected boolean myFrameRelativeP;
   public static boolean useFrameRelativeCouplingMasses = false;

   protected PointList<FemNode3d> myNodes;
   protected ArrayList<BodyConnector> myConnectors;

   // protected ArrayList<LinkedList<FemElement3d>> myElementNeighbors;

   public static boolean abortOnInvertedElems = false;
   public static boolean checkTangentStability = false;
   public static boolean noIncompressStiffnessDamping = false;
   // if the minimum real detJ for all elements is <= this number,
   // then request a step size reduction:
   public static double detJStepReductionLimit = 0.01;
   // This will disable detJ step reduction:
   // public static double detJStepReductionLimit = -Double.MAX_VALUE;

   // fraction of element mass that should be added to the FemFrame
   // when operating in frame-relative mode
   public static double frameMassFraction = 0.0;

   protected boolean myAbortOnInvertedElems = abortOnInvertedElems;
   protected boolean myWarnOnInvertedElems = true;
   protected boolean myCheckForInvertedElems = true;

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
   protected AuxMaterialBundleList myAuxiliaryMaterialList;

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
   protected boolean myNodalIncompBlocksAllocatedP = false;
   // incompressibility constraints attached to each FemNodeNeighbour;
   // needed for hard and soft nodal incompressibility
   protected boolean myNodalIncompConstraintsAllocatedP = false;
   protected boolean myHardIncompConfigValidP = false;
   protected boolean myNodalRestVolumesValidP = false;
   //private boolean myHardIncompConstraintsChangedP = true;
   private double myHardIncompUpdateTime = -1;

   // total number of incompressibility constraints (GT.colSize(), not blocks)
   private int myNumIncompressConstraints = 0;

   private VectorNd myDg = null;
   private VectorNd myIncompressLambda = new VectorNd();

   // keep track of the number of tet, hex, and quadratic elements
   private int myNumTetElements = 0;
   private int myNumNodalMappedElements = 0;
   private int myNumNodalInterpolatedElements = 0;
   private int myNumQuadraticElements = 0;

   protected double myMinDetJ; // used to record inverted elements
   protected FemElement3d myMinDetJElement = null; // element with "worst" DetJ
   protected int myNumInverted = 0; // used to tally number of inverted elements

   private static double DEFAULT_ELEMENT_WIDGET_SIZE = 0.0;
   private double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
   PropertyMode myElementWidgetSizeMode = PropertyMode.Inherited;

   protected static final Collidability DEFAULT_COLLIDABILITY =
      Collidability.ALL;   
   protected Collidability myCollidability = DEFAULT_COLLIDABILITY;

   // maximum number of pressure DOFs that can occur in an element
   private static int MAX_PRESSURE_VALS = 8;
   // maximum number of nodes for elements associated with nodal
   // incompressibility
   private static int MAX_NODAL_INCOMP_NODES = 8;

   // temp space for computing pressures
   protected VectorNd myPressures = new VectorNd(MAX_PRESSURE_VALS);
   protected MatrixNd myRinv = new MatrixNd();
   // temp space for computing pressure stiffness
   protected double[] myKp = new double[MAX_PRESSURE_VALS];
   // temp space for computing nodal incompressibility constraints
   protected Vector3d[] myNodalConstraints = new Vector3d[MAX_NODAL_INCOMP_NODES];
   // temp for computing element-wise linear stiffness strain
   protected SymmetricMatrix3d myEps = new SymmetricMatrix3d();

   // protected ArrayList<FemSurface> myEmbeddedSurfaces;
   protected MeshComponentList<FemMeshComp> myMeshList;

   HashMap<FemElement3d,int[]> ansysElemProps = new HashMap<FemElement3d,int[]>();

   // protected boolean myClearMeshColoring = false;
   protected boolean myComputeNodalStress = false;
   protected boolean myComputeNodalStrain = false;
   // protected boolean mySubSurfaceRendering = true;

   protected ColorMapBase myColorMap;
   protected PropertyMode myColorMapMode = PropertyMode.Inherited;

   static float[] myInvertedColor = new float[] { 1f, 0f, 0f};

   public static PropertyList myProps =
      new PropertyList(FemModel3d.class, FemModel.class);

   static {
      myProps.add (
         "axisLength * *", "length of rendered frame axes", 0);
      myProps.add (
         "frameRelative isFrameRelative",
         "compute displacements with respect to the coordinate frame",
         false);
      myProps.add(
         "incompressible",
         "Enforce incompressibility using constraints", DEFAULT_HARD_INCOMP);
      myProps.add(
         "softIncompMethod", "method of enforcing soft incompressibility",
         DEFAULT_SOFT_INCOMP);
      myProps.add(
         "incompCompliance",
         "Compliance for incompressibilty constraints", 0, "[0,inf]");
      myProps.addInheritable(
         "elementWidgetSize:Inherited",
         "size of rendered widget in each element's center",
         DEFAULT_ELEMENT_WIDGET_SIZE, "[0,1]");
      myProps.addInheritable("colorMap:Inherited", "color map for stress/strain", 
         createDefaultColorMap(), "CE");
      myProps.add (
         "collidable", 
         "sets the collidability of the FEM", DEFAULT_COLLIDABILITY);
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

   @Override
   public void getCollidables(List<Collidable> list, int level) {
      list.add(this);
      // traverse forward for additional collidables (e.g. FemMeshComp)
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

   public static ColorMapBase createDefaultColorMap() {
      return new HueColorMap(0.7, 0);
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      myDensity = DEFAULT_DENSITY;
      myStiffnessDamping = DEFAULT_STIFFNESS_DAMPING;
      myMassDamping = DEFAULT_MASS_DAMPING;
      myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
      myElementWidgetSizeMode = PropertyMode.Inherited;
      myHardIncompMethod = DEFAULT_HARD_INCOMP;
      mySoftIncompMethod = DEFAULT_SOFT_INCOMP;
      myColorMap = createDefaultColorMap();
      setMaterial(createDefaultMaterial());
      myAutoGenerateSurface = defaultAutoGenerateSurface;
   }

   public FemModel3d () {
      this(null);
   }

   public FemModel3d (String name) {
      super(name);
      setDefaultValues();
      for (int i = 0; i < MAX_NODAL_INCOMP_NODES; i++) {
         myNodalConstraints[i] = new Vector3d();
      }
      myMeshList.addFixed (createSurfaceMeshComponent());
   }

   /**
    * Creates a surface mesh component in the first location of the mesh list
    */
   protected FemMeshComp createSurfaceMeshComponent() {
      FemMeshComp surf = new FemMeshComp(this);
      surf.setName(DEFAULT_SURFACEMESH_NAME);
      surf.setSurfaceRendering(getSurfaceRendering());
      surf.markSurfaceMesh(true);
      surf.setCollidable (Collidability.EXTERNAL);
      return surf;
   }      

   protected void initializeChildComponents() {
      myFrame = new FemModelFrame ("frame");
      myNodes = new PointList<FemNode3d>(FemNode3d.class, "nodes", "n");
      myElements = new FemElement3dList("elements", "e");
      myAuxiliaryMaterialList = new AuxMaterialBundleList("materials", "mat");
      myMeshList =  new MeshComponentList<FemMeshComp>(
         FemMeshComp.class, "meshes", "msh");
      addFixed(myFrame);
      addFixed(myNodes);
      addFixed(myElements);
      addFixed(myAuxiliaryMaterialList);
      addFixed(myMeshList);
      super.initializeChildComponents();
   }

   public void addMaterialBundle(AuxMaterialBundle bundle) {
      if (!myAuxiliaryMaterialList.contains(bundle)) {
         for (AuxMaterialElementDesc d : bundle.getElements()) {
            bundle.checkElementDesc(this, d);
         }
         myAuxiliaryMaterialList.add(bundle);
      }
   }

   public boolean removeMaterialBundle(AuxMaterialBundle bundle) {
      return myAuxiliaryMaterialList.remove(bundle);
   }

   public void clearMaterialBundles() {
      myAuxiliaryMaterialList.removeAll();
   }

   public RenderableComponentList<AuxMaterialBundle> getMaterialBundles() {
      return myAuxiliaryMaterialList;
   }

   private void updateBVHierarchies() {
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
      myBVTreeValid = true;
   }

   protected BVTree getBVTree() {
      if (myAABBTree == null || !myBVTreeValid) {
         updateBVHierarchies();
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

   public void addNodes(Collection<? extends FemNode3d> nodes) {
      myNodes.addAll(nodes);
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
      for (FemMeshComp fm : myMeshList) {
         if (myAutoGenerateSurface && fm.isSurfaceMesh()) {
            // ignore autogenerated surface mesh
            mySurfaceMeshValid = false;
         } else if (fm.hasNodeDependency(p)) {
            System.err.println("Error: unable to remove node because the mesh '" 
               + fm.getName() + "' still depends on it");
            return false;
         }
      }
      myInternalSurfaceMeshComp = null;


      if (myNodes.remove(p)) {
         return true;
      }
      return false;
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
    * FemNode3d.getIndirectNeighbors().
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


   public void addElements(Collection<? extends FemElement3d> elems) {
      for (FemElement3d elem : elems) {
         addElement(elem);
      }
   }

   public void addElement(FemElement3d e) {
      myElements.add(e);
      if (myAutoGenerateSurface) {
         mySurfaceMeshValid = false;
         myInternalSurfaceMeshComp = null;
      }
   }

   public void addNumberedElement(FemElement3d e, int elemId) {
      myElements.addNumbered(e, elemId);
      if (myAutoGenerateSurface) {
         mySurfaceMeshValid = false;
         myInternalSurfaceMeshComp = null;
      }
   }

   public boolean removeElement(FemElement3d e) {
      boolean success = myElements.remove(e);
      if (myAutoGenerateSurface) {
         mySurfaceMeshValid = false;
         myInternalSurfaceMeshComp = null;
      }
      return success;
   }

   public void clearElements() {
      myElements.removeAll();
      for (int i = 0; i < myNodes.size(); i++) {
         myNodes.get(i).clearMass();
      }
      if (myAutoGenerateSurface) {
         mySurfaceMeshValid = false;
         myInternalSurfaceMeshComp = null;
      }
   }

   /**
    * Adds a marker to this FemModel. If the marker has not already been
    * set (i.e., if no nodes or elements have been assigned to it), then
    * it is attached to the nearest element in the model. This is either
    * the containing element, or the nearest element on the model's surface.
    * 
    * @param mkr
    * marker point to add to the model
    */
   public void addMarker (FemMarker mkr) {
      if (mkr.getAttachment().numMasters() == 0) {
         FemElement3d elem = findContainingElement(mkr.getPosition());
         if (elem == null) {
            Point3d newLoc = new Point3d();
            elem = findNearestSurfaceElement(newLoc, mkr.getPosition());
            mkr.setPosition(newLoc);
         }
         addMarker(mkr, elem);
      }
      else {
         super.addMarker (mkr);
      }
   }

   /**
    * Creates and adds a marker to this FemModel. The element to which it
    * belongs is determined automatically. If the marker's current position
    * does not lie within the model, it is projected onto the model's surface.
    * 
    * @param pos
    * position to place a marker in the model
    * @return created marker
    */
   public FemMarker addMarker (Point3d pos) {
      return addMarker(pos, true);
   }

   /**
    * Creates and adds a marker to this FemModel. The element to which it
    * belongs is determined automatically. If the marker's current position
    * does not lie within the model and {@code project == true}, it will be
    * projected onto the model's surface.
    * 
    * @param pos
    * position to place a marker in the model
    * @param project
    * if true and pnt is outside the model, projects to the nearest point
    * on the surface.  Otherwise, uses the original position.
    * @return created marker
    */
   public FemMarker addMarker(Point3d pos, boolean project) {
      FemMarker mkr = new FemMarker();
      FemElement3d elem = findContainingElement(pos);
      if (elem == null) {
         Point3d newLoc = new Point3d();
         elem = findNearestSurfaceElement(newLoc, pos);
         if (project) {
            mkr.setPosition(newLoc);
         } else {
            mkr.setPosition(pos);
         }
      } else {
         mkr.setPosition(pos);
      }
      addMarker(mkr, elem);
      return mkr;
   }

   public FemMarker addNumberedMarker(Point3d pos, int markerId) {
      return addNumberedMarker(pos, true, markerId);
   }

   public FemMarker addNumberedMarker(Point3d pos, boolean project, int markerId) {
      FemMarker mkr = new FemMarker();
      FemElement3d elem = findContainingElement(pos);
      if (elem == null) {
         Point3d newLoc = new Point3d();
         elem = findNearestSurfaceElement(newLoc, pos);
         if (project) {
            mkr.setPosition(newLoc);
         } else {
            mkr.setPosition(pos);
         }
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
      Vector3d fk = new Vector3d(); // stiffness force
      Vector3d fd = new Vector3d(); // damping force
      Vector3d md = new Vector3d(); // mass damping (used with attached frames)

      // gravity, internal and mass damping
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
         if (usingAttachedRelativeFrame()) {
            md.scale (myMassDamping * n.getMass(), n.getVelocity());
            n.subForce (md);
            // if (n.isActive()) {
            //    myFrame.addPointForce (n.getLocalPosition(), n.getForce());
            // }
            fk.add (fd);
            fk.transform (myFrame.getPose().R);
            n.subLocalForce (fk);
            //if (n.isActive()) {
            // myFrame.addPointForce (n.getLocalPosition(), n.getForce());
            //}
         }
         else {
            fd.scaledAdd(myMassDamping * n.getMass(), n.getVelocity(), fd);
            n.subForce(fk);
            n.subForce(fd);
         }
      }
   }

   protected double checkMatrixStability(DenseMatrix D) {
      EigenDecomposition evd = new EigenDecomposition();
      evd.factorSymmetric (D, EigenDecomposition.OMIT_V);
      VectorNd eig = evd.getEigReal();
      double min = eig.get(0);
      double max = eig.get(eig.size()-1);
      if (Math.abs(max) > Math.abs(min)) {
         return min / max;
      }
      else {
         return max / min;
      }
   }

   protected void computePressuresAndRinv(
      FemElement3d e, IncompressibleMaterial imat, double scale) {

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
            myRinv.scale(scale*imat.getBulkModulus() / restVol);
         }
         else {
            // optimize later
            MatrixNd Wtmp = new MatrixNd(W);
            Wtmp.scale(scale / restVol);
            myRinv.mul(Wtmp);
            myRinv.mul(Wtmp, myRinv);
         }
      }
      else {
         double Jpartial = e.myVolumes[0] / e.myRestVolumes[0];
         pbuf[0] = (imat.getEffectivePressure(Jpartial) +
            0 * e.myLagrangePressures[0]);
         myRinv.set(0, 0, scale*imat.getEffectiveModulus(Jpartial) / restVol);
      }
   }

   // DIVBLK
   private void computeStressAndStiffness(FemElement3d e, FemMaterial mat, 
      Matrix6d D, IncompMethod softIncomp) {

      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
      IntegrationData3d[] idata = e.getIntegrationData();
      FemNode3d[] nodes = e.getNodes();
      if (D != null) {
         D.setZero();
      }

      SolidDeformation def = new SolidDeformation();

      //===========================================
      // linear material optimizations
      //===========================================

      // potentially update cached linear material
      StiffnessWarper3d warper = e.getStiffnessWarper(); // internally updates

      // if there is cached linear material, then apply
      if (!warper.isCacheEmpty()) {

         // compute warping rotation
         warper.computeWarpingRotation(e);

         // add force and stiffness
         for (int i = 0; i < nodes.length; i++) {
            int bi = nodes[i].getSolveIndex();
            if (bi != -1) {
               FemNode3d n = nodes[i];
               if (!myStiffnessesValidP) {
                  for (int j = 0; j < nodes.length; j++) {
                     int bj = nodes[j].getSolveIndex();
                     if (!mySolveMatrixSymmetricP || bj >= bi) {
                        warper.addNodeStiffness(e.myNbrs[i][j].getK(), i, j);
                     }
                  }
               }
               // add node force
               warper.addNodeForce(n.myInternalForce, i, nodes);
            }
         }

         if (myComputeNodalStress || (myComputeNodalStrain && mat.isLinear()) ) {

            // estimate at warping point
            RotationMatrix3d R = warper.getRotation();
            IntegrationPoint3d wpnt = e.getWarpingPoint();
            IntegrationData3d wdata = e.getWarpingData();
            wpnt.computeJacobianAndGradient(nodes, wdata.myInvJ0);

            def.clear();
            def.setF(wpnt.F);
            def.setAveragePressure(0);
            def.setR(R);

            SymmetricMatrix3d sigma = new SymmetricMatrix3d();
            SymmetricMatrix3d tmp = new SymmetricMatrix3d();

            // compute nodal stress at wpnt
            if (myComputeNodalStress) {
               // compute linear stress
               mat.computeStress(tmp, def, null, null);
               sigma.add(tmp);
               for (AuxiliaryMaterial amat : e.getAuxiliaryMaterials()) {
                  amat.computeStress(tmp, def, wpnt, e.getWarpingData(), mat);
                  sigma.add(tmp);
               }

               // distribute stress to nodes
               for (int i = 0; i < nodes.length; i++) {
                  nodes[i].addScaledStress(1.0 / nodes[i].numAdjacentElements(), sigma);
               }
            }

            if (myComputeNodalStrain && mat.isLinear()) {

               // Cauchy strain at warping point
               if (mat.isCorotated()) {
                  // remove rotation from F
                  sigma.mulTransposeLeftSymmetric(R, wpnt.F);
               } else {
                  sigma.setSymmetric(wpnt.F);
               }
               sigma.m00 -= 1;
               sigma.m11 -= 1;
               sigma.m22 -= 1;

               // distribute strain to nodes
               for (int i = 0; i < nodes.length; i++) {
                  nodes[i].addScaledStrain(1.0 / nodes[i].numAdjacentElements(), sigma);
               }
            }
         } // stress or strain         
      }

      // exit early if no non-linear materials
      boolean linearOnly = mat.isLinear();
      if (linearOnly) {
         for (AuxiliaryMaterial amat : e.getAuxiliaryMaterials()) {
            if (!amat.isLinear()) {
               linearOnly = false;
               break;
            }
         }
      }
      if (linearOnly) {
         return;
      }

      // we have some non-linear contributions
      e.setInverted(false); // will check this below

      //===========================================
      // non-linear materials
      //===========================================

      // temporary stress and tangent
      SymmetricMatrix3d sigmaTmp = new SymmetricMatrix3d();
      Matrix6d Dtmp = null;
      if (D != null) {
         Dtmp = new Matrix6d();
      }

      // viscoelastic behaviour
      ViscoelasticBehavior veb = mat.getViscoBehavior();
      double vebTangentScale = 1;
      if (veb != null) {
         vebTangentScale = veb.getTangentScale();
      }

      // incompressibility
      IncompressibleMaterial imat = null;
      if (mat.isIncompressible()) {
         imat = (IncompressibleMaterial)mat;
      }
      MatrixBlock[] constraints = null;
      SymmetricMatrix3d C = new SymmetricMatrix3d();

      // initialize incompressible pressure
      double[] pbuf = myPressures.getBuffer();
      if (mat.isIncompressible() && softIncomp == IncompMethod.ELEMENT) {
         computePressuresAndRinv (e, imat, vebTangentScale);
         if (D != null) {
            constraints = e.getIncompressConstraints();
            for (int i = 0; i < e.myNodes.length; i++) {
               constraints[i].setZero();
            }
         }
      }

      // cache invertible flag
      boolean invertibleMaterials = e.materialsAreInvertible();

      // loop through each integration point
      for (int k = 0; k < ipnts.length; k++) {
         IntegrationPoint3d pt = ipnts[k];
         IntegrationData3d dt = idata[k];
         double scaling = dt.getScaling();

         pt.computeJacobianAndGradient(e.myNodes, idata[k].myInvJ0);
         def.clear();
         def.setF(pt.F);
         def.setAveragePressure(0);
         def.setR(null);

         double detJ = pt.computeInverseJacobian();
         if (detJ < myMinDetJ) {
            myMinDetJ = detJ;
            myMinDetJElement = e;
         }
         if (detJ <= 0 && !invertibleMaterials) {
            e.setInverted(true);
            myNumInverted++;
         }

         // compute shape function gradient and volume fraction
         double dv = detJ * pt.getWeight();
         Vector3d[] GNx = pt.updateShapeGradient(pt.myInvJ);

         // compute pressure
         double pressure = 0;
         double[] H = null;
         if (mat.isIncompressible()) {
            if (softIncomp == IncompMethod.ELEMENT) {
               H = pt.getPressureWeights().getBuffer();
               int npvals = e.numPressureVals();
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
               else if (e.integrationPointsMapToNodes()) {
                  pressure = nodes[k].myPressure;
               }
               else if (e.integrationPointsInterpolateToNodes()){
                  // interpolate using shape function
                  VectorNd N = pt.getShapeWeights();
                  pressure = 0;
                  for (int i=0; i<N.size(); ++i) {
                     pressure += nodes[i].myPressure*N.get(i);
                  }
               }
            }
            else if (softIncomp == IncompMethod.FULL) {
               pressure = imat.getEffectivePressure(detJ / dt.getDetJ0());
            }
         }

         // anisotropy rotational frame
         Matrix3d Q = (dt.myFrame != null ? dt.myFrame : Matrix3d.IDENTITY);

         // System.out.println("FEM Pressure: " + pressure);
         pt.avgp = pressure;
         def.setAveragePressure(pressure);

         // clear stress/tangents
         pt.sigma.setZero();
         if (D != null) {
            D.setZero();
         }

         // base material
         if (!mat.isLinear()) {
            mat.computeStress(pt.sigma, def, Q, null);
            if (scaling != 1) {
               pt.sigma.scale(scaling);
            }
            if (D != null) {
               mat.computeTangent(D, pt.sigma, def, Q, null);
               if (scaling != 1) {
                  D.scale(scaling);
               }
            }
         }

         // reset pressure to zero for auxiliary materials
         pt.avgp = 0;
         def.setAveragePressure(0);

         if (e.numAuxiliaryMaterials() > 0) {
            for (AuxiliaryMaterial amat : e.getAuxiliaryMaterials()) {

               // skip linear materials
               if (!amat.isLinear()) {

                  amat.computeStress(sigmaTmp, def, pt, dt, mat);
                  if (scaling != 1) {
                     sigmaTmp.scale(scaling);
                  }
                  pt.sigma.add(sigmaTmp);

                  if (D != null) {
                     amat.computeTangent(Dtmp, sigmaTmp, def, pt, dt, mat);
                     if (scaling != 1) {
                        Dtmp.scale(scaling);
                     }
                     D.add(Dtmp);
                  }
               }
            }
         }

         // XXX only uses non-linear stress
         pt.avgp = pressure;          // bring back pressure term
         def.setAveragePressure(pressure);
         if (veb != null) {
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

         // sum stress/stiffness contributions to each node
         for (int i = 0; i < e.myNodes.length; i++) {
            FemNode3d nodei = e.myNodes[i];
            int bi = nodei.getSolveIndex();

            FemUtilities.addStressForce(
               nodei.myInternalForce, GNx[i], pt.sigma, dv);

            if (D != null) {              
               double p = 0;
               double kp = 0;
               if (mat.isIncompressible() ){
                  if (softIncomp == IncompMethod.ELEMENT) {
                     FemUtilities.addToIncompressConstraints(
                        constraints[i], H, GNx[i], dv);
                     p = pressure;
                  }
                  else if (softIncomp == IncompMethod.FULL) {
                     double dV = dt.getDetJ0() * pt.getWeight();
                     kp = imat.getEffectiveModulus(detJ / dt.getDetJ0()) * dV;
                     p = pressure;
                  }
               }


               // compute stiffness
               if (bi != -1) {
                  for (int j = 0; j < e.myNodes.length; j++) {
                     int bj = e.myNodes[j].getSolveIndex();
                     if (!mySolveMatrixSymmetricP || bj >= bi) {

                        e.myNbrs[i][j].addMaterialStiffness(GNx[i], D, GNx[j], dv);
                        e.myNbrs[i][j].addGeometricStiffness(GNx[i], pt.sigma, GNx[j], dv);
                        e.myNbrs[i][j].addPressureStiffness(GNx[i], p, GNx[j], dv);   

                        if (kp != 0) {
                           e.myNbrs[i][j].addDilationalStiffness(vebTangentScale*kp, GNx[i], GNx[j]);
                        }
                     }
                  }
               }
            } // if D != null

            // nodal stress/strain
            double[] nodalExtrapMat = e.getNodalExtrapolationMatrix();
            if (nodalExtrapMat != null) {
               if (myComputeNodalStress) {
                  double a = nodalExtrapMat[i * ipnts.length + k];
                  if (a != 0) {
                     nodei.addScaledStress(
                        a / nodei.numAdjacentElements(), pt.sigma);
                  }
               }

               // if base material non-linear and computing nodal strain 
               if (myComputeNodalStrain && !mat.isLinear()) {
                  double a = nodalExtrapMat[i * ipnts.length + k];
                  if (a != 0) {
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
         } // looping through nodes computing stress

         // nodal incompressibility constraints
         if (D != null && softIncomp == IncompMethod.NODAL && !(e instanceof TetElement)) {
            if (e.integrationPointsMapToNodes()) {
               for (FemNodeNeighbor nbr : getNodeNeighbors(e.myNodes[k])) {
                  int j = e.getLocalNodeIndex(nbr.myNode);
                  if (j != -1) {
                     // myNodalConstraints[i].scale(dv, GNx[i]);
                     nbr.myDivBlk.scaledAdd(dv, GNx[j]);
                  }
               }
            }  else if (e.integrationPointsInterpolateToNodes()) {
               // distribute according to shape function weights
               VectorNd N = pt.getShapeWeights();
               for (int i = 0; i < N.size(); ++i) {
                  for (FemNodeNeighbor nbr : getNodeNeighbors(e.myNodes[i])) {
                     int j = e.getLocalNodeIndex(nbr.getNode());
                     if (j != -1) {
                        nbr.myDivBlk.scaledAdd(N.get(i)*dv, GNx[j]);
                     }
                  }
               }
            }
         } // soft incompressibility
      } // end looping through integration points

      // tet nodal incompressibility
      if (D != null && mat.isIncompressible() && softIncomp == IncompMethod.NODAL) {
         if (e instanceof TetElement) {
            ((TetElement)e).getAreaWeightedNormals(myNodalConstraints);
            for (int i = 0; i < 4; i++) {
               myNodalConstraints[i].scale(-1 / 12.0);
            }

            for (int i=0; i<e.numNodes(); ++i) {
               for (FemNodeNeighbor nbr : getNodeNeighbors(e.myNodes[i])) {
                  int j = e.getLocalNodeIndex(nbr.myNode);
                  if (j != -1) {
                     nbr.myDivBlk.scaledAdd(1, myNodalConstraints[j]);
                  }
               }
            }
         }
      } // end tet nodal integration

      // element-wise incompressibility constraints
      if (D != null) {
         if (mat.isIncompressible() && softIncomp == IncompMethod.ELEMENT) {
            boolean kpIsNonzero = false;
            int npvals = e.numPressureVals();
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


   private FemMaterial getElementMaterial(FemElement3d e) {
      FemMaterial mat = e.getMaterial();
      if (mat == null) {
         mat = myMaterial;
      }
      return mat;
   }

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
               nodes[i].myVolume += idata[i].getDv();
            }
         }
         else if (e.integrationPointsInterpolateToNodes()){ 
            // distribute using shape function
            IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
            IntegrationData3d[] idata = e.getIntegrationData();
            for (int k=0; k<ipnts.length; ++k) {
               VectorNd N = ipnts[k].getShapeWeights();
               for (int i = 0; i < nodes.length; i++) {
                  nodes[i].myVolume += idata[k].getDv()*N.get(i);
               }
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
         else if (e.integrationPointsInterpolateToNodes()) {
            // distribute based on shape functions
            IntegrationData3d[] idata = e.getIntegrationData();
            IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
            for (int k=0; k<ipnts.length; ++k) {
               VectorNd N = ipnts[k].getShapeWeights();
               for (int i = 0; i < nodes.length; i++) {
                  nodes[i].myRestVolume += ipnts[k].getWeight()* idata[k].getDetJ0()*N.get(i);
               }
            }
         }
      }
      myNodalRestVolumesValidP = true;
   }

   private void computeNodalIncompressibility(IncompressibleMaterial imat, Matrix6d D) {

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

   // DIVBLK
   public void updateStressAndStiffness() {

      // allocate or deallocate nodal incompressibility blocks
      setNodalIncompBlocksAllocated (getSoftIncompMethod()==IncompMethod.NODAL);

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
      // compute new forces as well as stiffness matrix if warping is enabled

      myMinDetJ = Double.MAX_VALUE;
      myMinDetJElement = null;
      myNumInverted = 0;

      double mins = Double.MAX_VALUE;
      FemElement3d minE = null;

      for (FemElement3d e : myElements) {
         FemMaterial mat = getElementMaterial(e);
         computeStressAndStiffness(e, mat, D, softIncomp);
         if (checkTangentStability) {
            double s = checkMatrixStability(D);
            if (s < mins) {
               mins = s;
               minE = e;
            }
         }
      }

      // incompressibility
      if ( (softIncomp == IncompMethod.NODAL) && myMaterial != null && myMaterial.isIncompressible()) {
         computeNodalIncompressibility((IncompressibleMaterial)myMaterial, D);
      }

      if (checkTangentStability && minE != null) {
         System.out.println("min s=" + mins + ", element " + minE.getNumber());
      }

      if (myNumInverted > 0) {
         if (myWarnOnInvertedElems) {
            System.out.println(
               "Warning: " + myNumInverted + " inverted elements; min detJ=" +
                  myMinDetJ + ", element " + 
                  ComponentUtils.getPathName(myMinDetJElement));
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

      if (myMaterial.isIncompressible() && softIncomp == IncompMethod.NODAL) {
         updateNodalPressures((IncompressibleMaterial)myMaterial);
      }

      // compute new forces as well as stiffness matrix if warping is enabled
      // myMinDetJ = Double.MAX_VALUE;
      for (FemElement3d e : myElements) {
         FemMaterial mat = getElementMaterial(e);
         computeStressAndStiffness(
            e, mat, /* D= */null, softIncomp);
      }
      myStressesValidP = true;
   }

   public static boolean defaultAutoGenerateSurface = true;  // add surface mesh to model by default
   public static String DEFAULT_SURFACEMESH_NAME = "surface";
   private boolean myAutoGenerateSurface = defaultAutoGenerateSurface;
   protected boolean mySurfaceMeshValid = false;
   // private surface mesh component which is used in situations like
   // findNearestSurfaceElement() where we need a surface mesh but the
   // official surface mesh is null.
   protected FemMeshComp myInternalSurfaceMeshComp;

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

   protected void addNeighborVelJacobian(
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

   // builds a Stiffness matrix, where entries are ordered by node numbers
   public SparseBlockMatrix createStiffnessMatrix() {

      if (!myStressesValidP || !myStiffnessesValidP) {
         updateStressAndStiffness();
      }

      SparseBlockMatrix M = new SparseBlockMatrix();
      int nnodes = numNodes();
      int[] sizes = new int[nnodes];
      for (int i=0; i<nnodes; ++i) {
         sizes[i] = 3;
      }
      M.addRows (sizes, sizes.length);
      M.addCols (sizes, sizes.length);
      M.setVerticallyLinked (true);

      int idx = 0;
      for (FemNode3d node : getNodes()) {
         node.setIndex(idx++);
      }

      // create solve blocks
      for (FemNode3d node : getNodes()) {
         MatrixBlock blk = node.createSolveBlock();
         M.addBlock(node.getIndex(), node.getIndex(), blk);
      }
      for (int i = 0; i < myNodes.size(); i++) {
         FemNode3d node = myNodes.get(i);
         for (FemNodeNeighbor nbr : getNodeNeighbors(node)) {
            FemNode3d other = nbr.getNode();
            Matrix3x3Block blk = null;
            if (other != node) {
               blk = new Matrix3x3Block();
               M.addBlock(node.getIndex(), nbr.getNode().getIndex(), blk);
            } else {
               blk = (Matrix3x3Block)M.getBlock(node.getIndex(), node.getIndex());
            }
            nbr.addPosJacobian(blk, -1.0);
         }
      }

      // System.out.println ("symmetric=" + mySolveMatrix.isSymmetric(1e-6));

      return M;
   }

   protected void addNodeNeighborBlock(
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

   public void setSurfaceRendering(SurfaceRender mode) {

      //SurfaceRender oldMode = mySurfaceRendering;
      super.setSurfaceRendering(mode);

      updateStressPlotRange();
      if (myMeshList.size() > 0) {
         FemMeshComp surf = myMeshList.get(0);
         if (surf.isSurfaceMesh()) {
            surf.setSurfaceRendering(mode);
         }
      }
   }

   public void render(Renderer renderer, int flags) {
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

   private void updateStressPlotRange() {

      if (mySurfaceRendering != SurfaceRender.Stress &&
         mySurfaceRendering != SurfaceRender.Strain) {
         return;
      }

      if (myStressPlotRanging == Ranging.Auto) {
         myStressPlotRange.merge (getNodalPlotRange(mySurfaceRendering));
      } 

   }

   public void prerender(RenderList list) {
      super.prerender(list);

      list.addIfVisible (myFrame);
      list.addIfVisible(myNodes);
      list.addIfVisible(myElements);
      list.addIfVisible(myMarkers);
      list.addIfVisible(myMeshList);

      // build surface mesh if needed
      if (myAutoGenerateSurface && !mySurfaceMeshValid) {
         getSurfaceMesh();  // triggers creation of surface mesh
      }

      updateStressPlotRange();

      myAuxiliaryMaterialList.prerender(list);
   }

   public void getSelection(LinkedList<Object> list, int qid) {
      super.getSelection(list, qid);
   }

   protected void doclear() {
      super.doclear();
      myElements.clear();
      myNodes.clear();
      myAuxiliaryMaterialList.removeAll();
      clearMeshComps();
   }

   public void scan(ReaderTokenizer rtok, Object ref) throws IOException {
      doclear();
      setDefaultValues();
      super.scan(rtok, ref);
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
         return true;
      }
      else if (scanAttributeName (rtok, "surfaceMeshValid")) {
         // need to defer setting surfaceMeshValid to the end of postscan
         // since otherwise it will be cleared as the FEM is built
         boolean surfaceMeshValid = rtok.scanBoolean();
         tokens.offer (new StringToken ("surfaceMeshValid", rtok.lineno()));
         tokens.offer (new IntegerToken (surfaceMeshValid ? 1 : 0));
         return true;
      }

      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "surfaceMeshValid")) {
         IntegerToken tok = (IntegerToken)tokens.poll();
         mySurfaceMeshValid = (tok.value() == 0 ? false : true);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   } 

   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      invalidateStressAndStiffness();
      notifyStructureChanged(this);
   }

   public boolean writeSurfaceMesh(PrintWriter pw) {
      FemMeshComp fm = getSurfaceMeshComp();
      if (fm != null) {
         return fm.writeMesh (pw);
      }
      else {
         return false;
      }
   }

   public void writeSurfaceMesh(String fileName) {
      IndentingPrintWriter pw = null;
      try {
         pw = ArtisynthIO.newIndentingPrintWriter(fileName);
         writeSurfaceMesh (pw);
      } catch (IOException e) {
         e.printStackTrace();
      }
      finally {
         if (pw != null) {
            pw.close();
         }
      }
   }

   public FemMeshComp scanSurfaceMesh(ReaderTokenizer rtok) throws IOException {
      // Create embedded mesh
      FemMeshComp surfMesh = doGetSurfaceMeshComp();
      surfMesh.scanMesh (rtok);
      myAutoGenerateSurface = false;
      mySurfaceMeshValid = true;
      System.out.println ("surfaceMeshValid");
      myInternalSurfaceMeshComp = null;
      return surfMesh;
   }

   public FemMeshComp scanSurfaceMesh(String fileName) throws IOException {
      return scanSurfaceMesh(ArtisynthIO.newReaderTokenizer(fileName));
   }

   public FemMeshComp scanMesh (ReaderTokenizer rtok) throws IOException {
      FemMeshComp mesh = new FemMeshComp (this);
      mesh.scanMesh (rtok);
      return mesh;
   }

   public FemMeshComp scanMesh (String fileName) throws IOException {
      return scanMesh (ArtisynthIO.newReaderTokenizer(fileName));
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
      pw.println("autoGenerateSurface=" + myAutoGenerateSurface);

      super.writeItems(pw, fmt, ancestor);
      // need to write out surfaceMeshValid at the end because otherwise
      // it will be set invalid as the FEM is being built 
      pw.println("surfaceMeshValid=" + mySurfaceMeshValid);
   }

   public void addConnector (BodyConnector c) {
      if (myConnectors == null) {
         myConnectors = new ArrayList<BodyConnector>();
      }
      myConnectors.add (c);
   }

   public void removeConnector (BodyConnector c) {
      if (myConnectors == null || !myConnectors.remove (c)) {
         throw new InternalErrorException ("connector not found");
      }
      if (myConnectors.size() == 0) {
         myConnectors = null;
      }
   }

   public List<BodyConnector> getConnectors() {
      return myConnectors;
   }

   @Override
   public boolean isFreeBody() {
      // XXX TODO need to finish
      return true;
   }

   public void transformPose (RigidTransform3d T) {
      if (isFrameRelative()) {
         RigidTransform3d TFW = new RigidTransform3d();
         TFW.mul (T, myFrame.getPose());
         myFrame.setPose (TFW);        
      }
      else {
         Point3d pos = new Point3d();
         for (FemNode n : myNodes) {
            n.getPosition (pos);
            pos.transform (T);
            n.setPosition (pos);
         }
      }
      updatePosState();
   }

   public void transformGeometry(AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // note that these bounds, if present, are explicitly set by the user, 
      // so it is appropriate to transform rather then recompute them
      if (myMinBound != null) {
         gtr.transformPnt (myMinBound);
      }
      if (myMaxBound != null) {
         gtr.transformPnt (myMaxBound);
      }
      myBVTreeValid = false;
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      context.addAll (myNodes);
      context.addAll (myMarkers);
      context.addAll(myMeshList);
      context.addAll(myAuxiliaryMaterialList);
   } 

   public IncompMethod getIncompressible() {
      return getHardIncompMethod();
   }

   public void setMaterial(FemMaterial mat) {
      mySoftIncompMethodValidP = false;
      super.setMaterial(mat);
      updateSoftIncompMethod();
   }

   private boolean softNodalIncompressAllowed() {
      int numAllowedElements = numTetElements() + numNodalMappedElements() 
         + numNodalInterpolatedElements();
      return ( myMaterial.isIncompressible() && 
         numAllowedElements == myElements.size());
   }

   private boolean hardNodalIncompressAllowed() {
      int numAllowedElements = numTetElements() + numNodalMappedElements() 
      + numNodalInterpolatedElements();
      return (numAllowedElements == myElements.size());
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
      //myHardIncompConstraintsChangedP = true;
      myHardIncompMethodValidP = false;
   }

   public Range getIncompressibleRange() {
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

   protected void updateSoftIncompMethod () {
      setSoftIncompMethod (mySoftIncompMethod);
   }

   public void setSoftIncompMethod(IncompMethod method) {
      //      if (method == IncompMethod.ON ||
      //         method == IncompMethod.OFF) {
      //         throw new IllegalArgumentException(
      //            "Unsupported method: " + method);
      //      }
      if (method == IncompMethod.ON) {
         method = IncompMethod.AUTO;
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
                                                 IncompMethod.FULL});
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

   // integrates nodal volume error using ipnts
   private double getLocalVolumeError(
      int nidx, FemNode3d[] nodes, IntegrationPoint3d[] pt, IntegrationData3d[] dt) {

      // compute based on shape weights
      double vol = 0;
      double volr = 0;
      for (int k=0; k<pt.length; ++k) {
         pt[k].computeJacobian(nodes);
         double detJ = pt[k].getJ().determinant();
         VectorNd N = pt[k].getShapeWeights();

         double sw = N.get(nidx);
         vol += detJ * pt[k].getWeight()*sw;
         volr += dt[k].getDetJ0()*pt[k].getWeight()*sw;
      }
      return (vol - volr);
   }

   private boolean volumeIsControllable(FemNode3d node) {
      return node.isActive();
   }

   private boolean hasControllableNodes(FemElement3d elem) {
      return elem.hasControllableNodes();
   }

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
         myNumNodalInterpolatedElements = 0;
         myNumQuadraticElements = 0;
         for (int i=0; i<myElements.size(); i++) {
            FemElement3d e = myElements.get(i);
            if (e instanceof TetElement) {
               myNumTetElements++;
            }
            else if (e.integrationPointsMapToNodes()) {
               myNumNodalMappedElements++;
            } else if (e.integrationPointsInterpolateToNodes()) {
               myNumNodalInterpolatedElements++;
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
   
   protected int numNodalInterpolatedElements() {
      updateElementCounts();
      return myNumNodalInterpolatedElements;
   }

   /**
    * Update the blocks uses in the incompressibility constraint matrices.
    * These are stored in the myDviBlk fields of each FemNodeNeighbor.
    * Derivative values for inactive nodes are stored in b.
    */
   // DIVBLK
   private void updateHardNodalIncompInfo(VectorNd b, double time) {

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
            
            for (int i=0; i<enodes.length; ++i) {
               FemNode3d n = enodes[i];
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
         } else if (e.integrationPointsMapToNodes()) {
            for (int i = 0; i < enodes.length; i++) {
               IntegrationPoint3d pt = e.getIntegrationPoints()[i];
               IntegrationData3d dt = e.getIntegrationData()[i];
               pt.computeJacobianAndGradient(e.myNodes, dt.myInvJ0);
               double detJ = pt.computeInverseJacobian();
               double dv = detJ * pt.getWeight();
               Vector3d[] GNx = pt.updateShapeGradient(pt.myInvJ);

               FemNode3d n = enodes[i];
               if ((idx = n.getIncompressIndex()) != -1) {
                  for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
                     FemNode3d nnode = nbr.myNode;
                     int j = e.getLocalNodeIndex(nnode);
                     if (j != -1) {
                        // if (isControllable (nnode)) {
                        nbr.myDivBlk.scaledAdd(dv, GNx[j]);
                        // }
                     }
                  }
                  b.add(idx, dg);
               }
            }
         } else if (e.integrationPointsInterpolateToNodes()){

            // compute constaints based on shape weights
            IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
            IntegrationData3d[] idata = e.getIntegrationData();

            // sum over integration points
            for (int k=0; k<ipnts.length; ++k) {
               VectorNd N = ipnts[k].getShapeWeights();

               IntegrationPoint3d pt = ipnts[k];
               IntegrationData3d dt = idata[k];

               pt.computeJacobianAndGradient(e.myNodes, dt.getInvJ0());
               double detJ = pt.computeInverseJacobian();
               double dv = detJ * pt.getWeight();
               Vector3d[] GNx = pt.updateShapeGradient(pt.getInvJ());

               for (int i = 0; i < enodes.length; i++) {  
                  FemNode3d n = enodes[i];
                  // sum over nodes
                  if ((idx = n.getIncompressIndex()) != -1) {
                     for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
                        FemNode3d nnode = nbr.myNode;
                        int j = e.getLocalNodeIndex(nnode);
                        if (j != -1) {
                           // if (isControllable (nnode)) {
                           nbr.myDivBlk.scaledAdd(dv*N.get(i), GNx[j]);
                           // }
                        }
                     }
                     b.add(idx, dg);
                  }
               } // looping through nodes
            } // loop through ipnts
      
         } // type of element for nodal incompressibility
      } // looping through elements
      
   }

   private void updateHardElementIncompInfo(VectorNd b, double time) {
      int ci = 0;

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
      //myHardIncompConstraintsChangedP = true;
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

   /**
    * Returns the surface mesh vertex (if any) associated with a specified
    * node. The mesh checked is the one returned by {@link #getSurfaceMesh}.
    * 
    * @param node node to check
    * @return surface vertex associated with <code>node</code>, or
    * <code>null</code> if no such vertex exists
    */
   public Vertex3d getSurfaceVertex (FemNode3d node) {
      FemMeshComp femMesh = getSurfaceMeshComp();
      if (femMesh != null) {
         return femMesh.getVertexForNode (node);
      }
      else {
         return null;
      }
   }

   /**
    * Returns the FEM node (if any) associated with a specified
    * surface mesh vertex. The mesh checked is the one returned by 
    * {@link #getSurfaceMesh}.
    * 
    * @param vtx vertex to check
    * @return FEM node associated with <code>vtx</code>, or
    * <code>null</code> if no such node exists
    */
   public FemNode3d getSurfaceNode (Vertex3d vtx) {
      FemMeshComp femMesh = getSurfaceMeshComp();
      if (femMesh != null) {
         return femMesh.getNodeForVertex (vtx);
      }
      else {
         return null;
      }
   }

   /**
    * Returns true if a specified node lies on the surface mesh returned by 
    * {@link #getSurfaceMesh}.
    * 
    * @param node node to check
    * @return <code>true</code> if <code>node</code> lies on the surface mesh
    */
   public boolean isSurfaceNode (FemNode3d node) {
      return getSurfaceVertex (node) != null;
   }

   /**
    * Recreates the surface mesh based on all elements
    */
   protected void createDefaultSurfaceMesh (FemMeshComp meshc) {
      // by default, build fine surface mesh if quadratic elements present
      if (numQuadraticElements() > 0) {
         meshc.createFineSurface (3, new ElementFilter());
      } else {
         meshc.createSurface(new ElementFilter());
      }
   }

   public FemMeshComp createSurfaceMesh (ElementFilter efilter) {
      FemMeshComp femMesh = doGetSurfaceMeshComp();
      femMesh.createSurface (efilter);
      // mySurfaceMesh = (PolygonalMesh)femMesh.getMesh();
      mySurfaceMeshValid = true;
      myInternalSurfaceMeshComp = null;
      // disable auto regeneration since mesh was manually created
      myAutoGenerateSurface = false;   
      return femMesh;
   }

   // Returns the FemMeshComp component for the surface mesh. If appropriate, the
   // surface is generated on demand
   public FemMeshComp getSurfaceMeshComp() {

      if (myMeshList.size() < 1) {
         throw new IllegalArgumentException (
            "Default surface mesh component has been removed");
      }

      // if auto, take first.  If not, take first one marked as a surface mesh;
      if (!mySurfaceMeshValid) {
         if (myAutoGenerateSurface) {
            FemMeshComp meshc = doGetSurfaceMeshComp();
            createDefaultSurfaceMesh (meshc);
            mySurfaceMeshValid = true;
            myInternalSurfaceMeshComp = null;
            MeshBase mesh = meshc.getMesh(); // grab newly created mesh
            // paranoid: call in case mesh is rendered directly before
            // prerender()
            PolygonalMesh smesh = (PolygonalMesh)mesh;
            smesh.saveRenderInfo(myRenderProps);
            return meshc;
         }
         else {
            return null;
         }         
      }
      else {
         return myMeshList.get(0);
      }
   }      

   public PolygonalMesh getSurfaceMesh() {
      FemMeshComp sfm = getSurfaceMeshComp();
      if (sfm != null) {
         return (PolygonalMesh)sfm.getMesh();
      }
      else {
         return null;
      }
   }

   private PolygonalMesh getInternalSurfaceMesh() {
      if (myInternalSurfaceMeshComp == null) {
         myInternalSurfaceMeshComp = new FemMeshComp(this);
         myInternalSurfaceMeshComp.markSurfaceMesh (true);
         createDefaultSurfaceMesh (myInternalSurfaceMeshComp);
      }
      return (PolygonalMesh)myInternalSurfaceMeshComp.getMesh();
   }

   private void testSimpleSurfaceMesh() {
      FemMeshComp sfm = getSurfaceMeshComp();
      if (sfm != null) {
         for (Vertex3d vtx : sfm.getMesh().getVertices()) {
            FemNode3d node = getSurfaceNode (vtx);
            if (node == null) {
               throw new TestException (
                  "no node found for vertex "+vtx.getIndex());
            }
            Vertex3d chk = getSurfaceVertex (node);
            if (chk != vtx) {
               throw new TestException (
                  "no vertex found for node "+node.getNumber());
            }
         }
         System.out.println ("SURFACE OK");
      }
   }

   @Override
   public int numSurfaceMeshes() {
      return MeshComponent.numSurfaceMeshes (myMeshList);
   }

   @Override
   public PolygonalMesh[] getSurfaceMeshes() {
      return MeshComponent.getSurfaceMeshes (myMeshList);
   }

   @Override
   public Collidability getCollidable () {
      getSurfaceMesh(); // build surface mesh if necessary
      return myCollidability;
   }

   public void setCollidable (Collidability c) {
      if (myCollidability != c) {
         myCollidability = c;
         notifyParentOfChange (new StructureChangeEvent (this));
      }
   }

   @Override
   public Collidable getCollidableAncestor() {
      return null;
   }

   @Override
   public boolean isCompound() {
      return true;
   }

   @Override
   public boolean isDeformable () {
      return true;
   }

   public FemMeshComp addMesh(MeshBase mesh) {
      String meshName =
         ModelComponentBase.makeValidName(mesh.getName(), null, myMeshList);
      return addMesh(meshName, mesh);
   }

   public FemMeshComp addMesh(String name, MeshBase mesh) {
      mesh.setFixed(false);
      mesh.setColorsFixed(false);
      FemMeshComp surf = FemMeshComp.createEmbedded(this, mesh);
      surf.setName(name);
      doAddMeshComp(surf);
      return surf;
   }

   private void doAddMeshComp(FemMeshComp mesh) {
      mesh.setCollidable (Collidability.INTERNAL);
      myMeshList.add(mesh);
   }

   public FemMeshComp getMeshComp(String name) {
      return myMeshList.get(name);
   }

   public FemMeshComp getMeshComp(int idx) {
      return myMeshList.get(idx);
   }

   public MeshComponentList<FemMeshComp> getMeshComps() {
      return myMeshList;
   }

   public int numMeshComps() {
      return myMeshList.size();
   }

   public void addMeshComp (FemMeshComp surf) {
      if (surf.getParent() == myMeshList) {
         throw new IllegalArgumentException (
            "FemModel3d already contains specified mesh component");
      }
      if (surf.myFem == this) {
         surf.setCollidable (Collidability.INTERNAL);
         myMeshList.add (surf);
      }
      else {
         throw new IllegalArgumentException (
            "FemMeshComp does not reference the FEM to which is is being added");
      }
   }

   public boolean removeMeshComp (FemMeshComp surf) {
      if (surf == myMeshList.get(0)) {
         throw new IllegalArgumentException (
            "First mesh reserved for default surface and cannot be removed");
      }
      else {
         return myMeshList.remove(surf);
      }
   }

   public void clearMeshComps() {
      for (int i=myMeshList.size()-1; i>0; i--) {
         myMeshList.remove (i);
      }
      mySurfaceMeshValid = false;
      myInternalSurfaceMeshComp = null;
   }

   private FemMeshComp doGetSurfaceMeshComp() {
      if (myMeshList.size()==0 || !myMeshList.get(0).isSurfaceMesh()) {
         throw new InternalErrorException (
            "surface mesh component missing from mesh list");
      }
      return myMeshList.get(0);
   }

   public void setAutoGenerateSurface(boolean val) {
      if (val != myAutoGenerateSurface) {
         myAutoGenerateSurface = val;
      }
   }

   public boolean isAutoGeneratingSurface() {
      return myAutoGenerateSurface;
   }

   public boolean isSurfaceMeshValid() {
      return mySurfaceMeshValid;
   }

   public FemMeshComp setSurfaceMesh(PolygonalMesh mesh) {
      // Create embedded mesh
      FemMeshComp surfMesh = doGetSurfaceMeshComp();
      FemMeshComp.createEmbedded(surfMesh, mesh);
      myAutoGenerateSurface = false;
      mySurfaceMeshValid = true;
      myInternalSurfaceMeshComp = null;
      return surfMesh;
   }
   
   
   public void setSurfaceMeshComp(FemMeshComp mesh) {
      myAutoGenerateSurface = false;
      mySurfaceMeshValid = true;
      myInternalSurfaceMeshComp = null;
      FemMeshComp oldSurface = doGetSurfaceMeshComp();
      if (mesh.getModel() != this) {
         throw new InternalErrorException("Mesh does not belong to model");
      }
      myMeshList.set(0, mesh);
   }

   public void invalidateSurfaceMesh() {
      mySurfaceMeshValid = false;
      myInternalSurfaceMeshComp = null;
   }

   protected void clearCachedData(ComponentChangeEvent e) {
      super.clearCachedData(e);
      // clearIncompressVariables();
      mySolveMatrix = null;
      // myActiveNodes = null;
      myBVTreeValid = false;
      mySoftIncompMethodValidP = false;
      myHardIncompMethodValidP = false;
      myHardIncompConfigValidP = false;
      myNumTetElements = -1; // invalidates all element counts
   }

   // Called when the geometry (but not the topology) of one or
   // more underlying components changes. There is no need to
   // invalidate element-wise rest data, since that
   // should have already been done when the nodes themselves
   // were transformed.
   private void handleGeometryChange() {

      myBVTreeValid = false;
      invalidateStressAndStiffness();
      invalidateNodalRestVolumes();
      myRestVolumeValid = false;

      //computeMasses();

      //updateLocalAttachmentPos();
      updateSlavePos();
   }

   public void componentChanged(ComponentChangeEvent e) {

      if (e.getCode() == ComponentChangeEvent.Code.STRUCTURE_CHANGED) {
         if (e.getComponent() == myElements || e.getComponent() == myNodes) {
            // XXX this invalidates the surface mesh even during scanning
            // which we don't really want. Specifically, the postscan
            // for nodes and elements issue a change event from 
            // myComonents.scanEnd(). 
            if (myAutoGenerateSurface) {
               invalidateSurfaceMesh();               
            }
         }
         clearCachedData(null);
         // should invalidate elasticity
      }
      else if (e.getCode() == ComponentChangeEvent.Code.GEOMETRY_CHANGED) { 
         handleGeometryChange();
      }
      else if (
         e.getCode() == ComponentChangeEvent.Code.DYNAMIC_ACTIVITY_CHANGED) { 
         clearCachedData(null);
      }
      notifyParentOfChange(e);
   }

   @Override
   protected void notifyStructureChanged(Object comp) {
      clearCachedData(null);
      super.notifyStructureChanged(comp);
   }

   public FemElement3d getSurfaceElement (Face face) {

      return getSurfaceMeshComp().getFaceElement (face);
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
      PolygonalMesh surf = getSurfaceMesh();
      if (surf == null || surf.numFaces() == 0) {
         surf = getInternalSurfaceMesh();
      }
      if (surf != null) {
         Face face = BVFeatureQuery.getNearestFaceToPoint (
            loc, coords, surf, pnt);
         FemElement3d elem = getSurfaceElement(face);
         if (elem == null) {
            throw new InternalErrorException (
               "surface element not found for face");
         }
         return elem;
      }
      else {
         return null;
      }
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
    * point. If <code>maxDist</code> &lt; 0, then <code>null</code>
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
      myMeshList.updateSlavePos();
      myBVTreeValid = false;
      if (myFrameConstraint != null && !myFrameRelativeP) {
         myFrameConstraint.updateFramePose(/*frameRelative=*/false);
      }
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
         if (!(mat.isLinear())) {
            if (detJ < myMinDetJ) {
               if (!e.materialsAreInvertible()) {
                  myMinDetJ = detJ;
                  myMinDetJElement = e;
               }
            }
            if (detJ <= 0 && myCheckForInvertedElems) {
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

   public boolean isVolumeValid() {
      return myVolumeValid;
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
      // update forces if any of the meshes use stress/strain plotting.  This
      // will happen anyway if updateForcesAtStepEnd is true.
      boolean updateForces = getUpdateForcesAtStepEnd();
      if (!updateForces) {
         for (FemMeshComp mc : myMeshList) {
            if (mc.isStressOrStrainRendering (mc.getSurfaceRendering())) {
               updateForces = true;
               break;
            }
         }
      }
      if (updateForces) {
         applyForces (t1);
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

   // === Constrainer interface ===

   public double updateConstraints(double t, int flags) {
      if (usingAttachedRelativeFrame()) {
         myFrameConstraint.updateConstraints (t, flags);
      }
      if (!myVolumeValid) {
         updateVolume();
      }
      if (getHardIncompMethod() != IncompMethod.OFF) {
         updateHardIncompInfo(t);
      }
      return 0;
   }

   public void getConstrainedComponents (List<DynamicComponent> list) {
      if (getHardIncompMethod() != IncompMethod.OFF) {
         list.addAll (myNodes);
      }
   }

   public int setBilateralImpulses(VectorNd lam, double h, int idx) {

      if (usingAttachedRelativeFrame()) {
         idx = myFrameConstraint.setBilateralImpulses (lam, h, idx);
      }
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

      if (usingAttachedRelativeFrame()) {
         myFrameConstraint.zeroImpulses();
      }
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

      if (usingAttachedRelativeFrame()) {
         idx = myFrameConstraint.getBilateralImpulses (lam, idx);
      }
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

   public void getBilateralSizes (VectorNi sizes) {

      if (usingAttachedRelativeFrame()) {
         myFrameConstraint.getBilateralSizes (sizes);
      }
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
      SparseBlockMatrix GT, VectorNd dg, int numb) {

      if (usingAttachedRelativeFrame()) {
         numb = myFrameConstraint.addBilateralConstraints (GT, dg, numb);
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

      if (usingAttachedRelativeFrame()) {
         idx = myFrameConstraint.getBilateralInfo (ginfo, idx);
      }
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
               gi.force = 0;
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
               } else {
                  // computes constraint distance using shape functions
                  FemNode3d[] nodes = elem.myNodes;
                  IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
                  IntegrationData3d[] idata = elem.getIntegrationData();
                  double verr;
                  for (int i = 0; i < nodes.length; i++) {
                     if ((ci = nodes[i].getIncompressIndex()) != -1) {
                        verr = getLocalVolumeError(i, nodes, ipnts, idata);
                        ginfo[idx + ci].dist += verr;
                     }
                  } 
               }
            }
         }
         else if (hardIncomp == IncompMethod.ELEMENT) {
            ci = idx;
            for (FemElement3d e : myElements) {
               e.getRestVolume(); // makes sure rest volume is updated
               if (e.getIncompressIndex() != -1) {
                  for (int k = 0; k < e.numPressureVals(); k++) {
                     ConstraintInfo gi = ginfo[ci++];
                     gi.dist = e.myVolumes[k] - e.myRestVolumes[k];
                     gi.compliance = myIncompCompliance;
                     gi.damping = damping;
                     gi.force = 0;
                  }
               }
            }
         }
         idx += ncols;
      }
      return idx;
   }

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

   public PointAttachment createPointAttachment (Point pnt) {
      return createPointAttachment (pnt, /*reduceTol=*/1e-8);
   }

   public PointAttachment createPointAttachment (Point pnt, double reduceTol) {

      if (pnt.isAttached()) {
         throw new IllegalArgumentException ("point is already attached");
      }
      if (ComponentUtils.isAncestorOf (this, pnt)) {
         throw new IllegalArgumentException (
            "FemModel is an ancestor of the point");
      }
      Point3d loc = new Point3d();
      FemElement3d elem = findNearestElement (loc, pnt.getPosition());
      FemNode3d nearestNode = null;
      double nearestDist = Double.MAX_VALUE;
      for (FemNode3d n : elem.getNodes()) {
         double d = n.distance (pnt);
         if (d < nearestDist) {
            nearestNode = n;
            nearestDist = d;
         }
      }
      if (nearestDist <= reduceTol) {
         // just attach to the node
         return new PointParticleAttachment (nearestNode, pnt);
      }
      else {
         return PointFem3dAttachment.create (pnt, elem, loc, reduceTol);
         // Coords are computed in createNearest.  the point's position will be
         // updated, if necessary, by the addRemove hook when the attachement
         // is added.
      }
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
      Point3d loc = new Point3d();
      Point3d pos = new Point3d(TFW != null ? TFW.p : frame.getPose().p);
      FemElement3d elem = findNearestElement (loc, pos);
      if (!loc.equals (pos)) {
         TFW = new RigidTransform3d (TFW);
         TFW.p.set (loc);
      }
      FrameFem3dAttachment ffa = new FrameFem3dAttachment (frame);
      ffa.setFromElement (TFW, elem);
      if (frame != null) {
         if (DynamicAttachment.containsLoop (ffa, frame, null)) {
            throw new IllegalArgumentException (
               "attachment contains loop");
         }
      }
      return ffa;
   }

   /**
    * Returns a FrameAttachment that attaches a <code>frame</code> to this
    * component. Once attached the frame will follow the body around.  The
    * initial pose of the frame is specified by <code>TFW</code>, which gives
    * its position and orientation in world coordinates. If <code>TFW</code> is
    * <code>null</code>, then the current pose of the frame is used. If
    * <code>frame</code> is <code>null</code>, then a virtual attachment is
    * created at the initial pose specified by
    * <code>TFW</code>. <code>frame</code> and <code>TFW</code> cannot both be
    * <code>null</code>.
    * 
    * @param frame frame to be attached
    * @param TFW transform from (initial) frame coordinates to world
    * coordinates
    * @param project if true and if the frame is outside the FEM, then
    * the frame gets projected to the nearest point on the FEM
    * @return attachment attaching <code>frame</code> to this component
    */
   public FrameFem3dAttachment createFrameAttachment (
      Frame frame, RigidTransform3d TFW, boolean project) {

      if (frame == null && TFW == null) {
         throw new IllegalArgumentException (
            "frame and TFW cannot both be null");
      }
      if (frame != null && frame.isAttached()) {
         throw new IllegalArgumentException ("frame is already attached");
      }
      Point3d loc = new Point3d();
      Point3d pos = new Point3d(TFW != null ? TFW.p : frame.getPose().p);
      FemElement3d elem = findNearestElement (loc, pos);
      if (project && !loc.equals (pos)) {
         TFW = new RigidTransform3d (TFW);
         TFW.p.set (loc);
      }
      FrameFem3dAttachment ffa = new FrameFem3dAttachment (frame);
      ffa.setFromElement (TFW, elem);
      if (frame != null) {
         if (DynamicAttachment.containsLoop (ffa, frame, null)) {
            throw new IllegalArgumentException (
               "attachment contains loop");
         }
      }
      return ffa;
   }

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
               if (containsNode(n1, pfa.getNodes())) {
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
               FemNode[] nodes = pfa.getNodes();
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
               PointFem3dAttachment a = new PointFem3dAttachment(edgeNode);
               a.setFromNodes (new FemNode[] { n0, n1 }, edgeWeights);
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
               PointFem3dAttachment a = new PointFem3dAttachment(faceNode);
               a.setFromNodes (new FemNode[] { n0, n1, n2, n3 }, faceWeights);
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
      myAuxiliaryMaterialList.scaleDistance(s);
      myVolume *= (s * s * s);
      myBVTreeValid = false;
      // invalidate trees of meshes as well
      // update skinning positions
      for (MeshComponent mc : myMeshList) {
         mc.scaleDistance(s);
      }
   }

   @Override
   public void scaleMass(double s) {
      super.scaleMass(s);
      myAuxiliaryMaterialList.scaleMass(s);
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
   public FemModel3d copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      if (copyMap == null) {
         copyMap = new HashMap<ModelComponent,ModelComponent>();
         flags = CopyableComponent.COPY_REFERENCES;
      }

      FemModel3d fem = (FemModel3d)super.copy(flags, copyMap);

      // fem.myFrame was created in super.copy(), but we redo this
      // so as to create an exact copy of the orginal frame
      FemModelFrame newFrame = myFrame.copy (flags, copyMap);
      newFrame.setName (myFrame.getName());
      copyMap.put(myFrame, newFrame);
      fem.myFrame = newFrame;
      if (myFrameConstraint != null) {
         fem.attachFrame (myFrame.getPose());
      }
      else {
         fem.attachFrame (null);
      }
      fem.myFrameRelativeP = myFrameRelativeP;

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

      for (int i=0; i<myMeshList.size(); i++) {
         FemMeshComp mc = myMeshList.get(i);
         FemMeshComp newFmc = mc.copy(flags, copyMap);
         if (i == 0) {
            fem.myMeshList.addFixed (newFmc);
         }
         else {
            fem.addMeshComp(newFmc);
         }
         // do this this since addMesh sets collidability by default
         newFmc.setCollidable (mc.getCollidable());        
      }

      fem.myAutoGenerateSurface = myAutoGenerateSurface;
      fem.mySurfaceMeshValid = mySurfaceMeshValid;
      fem.myInternalSurfaceMeshComp = null;

      fem.setElementWidgetSizeMode(myElementWidgetSizeMode);
      if (myElementWidgetSizeMode == PropertyMode.Explicit) {
         fem.setElementWidgetSize(myElementWidgetSize);
      }

      fem.clearCachedData(null);

      fem.myAABBTree = null;
      fem.myBVTreeValid = false;

      fem.mySolveMatrixFile = null;

      fem.myNumIncompressConstraints = 0;
      fem.myHardIncompUpdateTime = -1;

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

   @Override
   public void updateBounds(Vector3d pmin, Vector3d pmax) {
      updateSlavePos();
      super.updateBounds(pmin, pmax);

      myMeshList.updateBounds(pmin, pmax);
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

   public void getNodalDeformationGradients (Matrix3d[] Fnodal) {
      if (Fnodal.length < myNodes.size()) {
         throw new IllegalArgumentException (
            "Fnodal must have length >= " + myNodes.size());
      }
      for (FemElement3d e : myElements) {
         FemNode3d[] enodes = e.myNodes;
         FemMaterial mat = getElementMaterial(e);
         if (mat.isLinear()) {
            IntegrationPoint3d wpnt = e.getWarpingPoint();
            IntegrationData3d data = e.getWarpingData();
            wpnt.computeJacobianAndGradient (enodes, data.myInvJ0);
            for (int i=0; i<enodes.length; i++) {          
               int nidx = myNodes.indexOf(enodes[i]);
               Fnodal[nidx].scaledAdd (
                  1.0/enodes[i].numAdjacentElements(), wpnt.F);
            }
         }
         else {
            IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
            IntegrationData3d[] idata = e.getIntegrationData();
            double[] nodalExtrapMat = e.getNodalExtrapolationMatrix();
            for (int k=0; k<ipnts.length; k++) {
               ipnts[k].computeJacobianAndGradient (e.myNodes, idata[k].myInvJ0);
               for (int i=0; i<enodes.length; i++) {  
                  double a = nodalExtrapMat[i*ipnts.length + k];
                  int nidx = myNodes.indexOf(enodes[i]);                  
                  Fnodal[nidx].scaledAdd (
                     a/enodes[i].numAdjacentElements(), ipnts[k].F); 
               }
            }
         }
      }
   }

   /* =================== Frame support ======================= */

   public FemModelFrame getFrame() {
      return myFrame;
   }

   public boolean isFrameRelative() {
      return myFrameRelativeP;
   }

   public void setFrameRelative (boolean enable) {
      if (myFrameRelativeP != enable) {
         myFrameRelativeP = enable;
         Frame frame = enable ? myFrame : null;
         for (int i=0; i<myNodes.size(); i++) {
            myNodes.get(i).setFrame (frame);
         }
         notifyStructureChanged(this);
      }
   }

   public boolean usingAttachedRelativeFrame() {
      return myFrameConstraint != null && myFrameRelativeP;
   }

   public FrameFem3dConstraint getFrameConstraint() {
      return myFrameConstraint;
   }

   public void attachFrame (RigidTransform3d TRW) {
      if (TRW == null) {
         myFrameConstraint = null;
         myFrame.setPose (RigidTransform3d.IDENTITY);
         myFrame.setVelocity (Twist.ZERO);
      }
      else {
         RigidTransform3d TX = new RigidTransform3d(TRW);
         Point3d pos = new Point3d(TRW.p);
         FemElement3d elem = findContainingElement (pos);
         if (elem == null) {
            Point3d newLoc = new Point3d();
            elem = findNearestSurfaceElement(newLoc, pos);
            TX.p.set (newLoc);
         }         
         myFrame.setPose (TX);
         myFrameConstraint = new FrameFem3dConstraint (myFrame, elem);
      }
   }

   public double getAxisLength() {
      return myFrame.getAxisLength();
   }

   public void setAxisLength (double len) {
      myFrame.setAxisLength (len);
   }

   public void getDynamicComponents (
      List<DynamicComponent> active,
      List<DynamicComponent> attached, 
      List<DynamicComponent> parametric) {

      super.getDynamicComponents (active, attached, parametric);
      if (myFrameRelativeP) {
         if (myFrame.isActive()) {
            active.add (myFrame);
         }
         else {
            parametric.add (myFrame);
         }
      }
      else {
         attached.add (myFrame);
      }
   }

   public void addGeneralMassBlocks (SparseBlockMatrix M) {
      if (myFrameRelativeP && useFrameRelativeCouplingMasses) {
         int bi = myFrame.getSolveIndex();
         if (bi != -1) {
            for (int i=0; i<myNodes.size(); i++) {
               int bj = myNodes.get(i).getSolveIndex();
               if (bj != -1) {
                  M.addBlock (bi, bj, new Matrix6x3Block());
                  M.addBlock (bj, bi, new Matrix3x6Block());
               }
            }
         }
      }
   }

   /**
    * Computes the coupling mass between this model's frame coordinates and a
    * specific frame-relative node. This is a 3x6 matrix with the form
    *
    * [ m R^T   - m R^T [c] ]
    *
    * where R is the frame's orientation, c is the node's local position (
    * rotated into frame coodinates), and m is the node's mass.
    */
   private void setCouplingMass (
      Matrix3x6Block blk, double m, RotationMatrix3d R, Vector3d c) {

      blk.m00 = m*R.m00;
      blk.m01 = m*R.m10;
      blk.m02 = m*R.m20;
      blk.m10 = m*R.m01;
      blk.m11 = m*R.m11;
      blk.m12 = m*R.m21;
      blk.m20 = m*R.m02;
      blk.m21 = m*R.m12;
      blk.m22 = m*R.m22;

      double x = m*c.x;
      double y = m*c.y;
      double z = m*c.z;

      blk.m03 = y*R.m20 - z*R.m10;
      blk.m13 = y*R.m21 - z*R.m11;
      blk.m23 = y*R.m22 - z*R.m12;
      blk.m04 = z*R.m00 - x*R.m20;
      blk.m14 = z*R.m01 - x*R.m21;
      blk.m24 = z*R.m02 - x*R.m22;
      blk.m05 = x*R.m10 - y*R.m00;
      blk.m15 = x*R.m11 - y*R.m01;
      blk.m25 = x*R.m12 - y*R.m02;
   }

   /**
    * Scale the masses of the nodes that are connected to the frame,
    * in order to ensure that the mass matrix is non-singular.
    */
   private void scaleFrameNodeMasses (
      SparseBlockMatrix M, VectorNd f, double s) {

      int bf = myFrame.getSolveIndex();
      FemNode3d[] nodes = myFrameConstraint.getElement().getNodes();
      double[] fbuf = f.getBuffer();

      int bk;
      for (int k=0; k<nodes.length; k++) {
         if ((bk = nodes[k].getSolveIndex()) != -1) {
            M.getBlock (bk, bk).scale (s);
            M.getBlock (bf, bk).scale (s);
            M.getBlock (bk, bf).scale (s);

            // scale fictitious force terms for node
            int idx = M.getBlockRowOffset (bk);
            fbuf[idx++] *= s;
            fbuf[idx++] *= s;
            fbuf[idx++] *= s;
         }
      }
   }

   @Override
   public void getMassMatrixValues (SparseBlockMatrix M, VectorNd f, double t) {

      int bk;

      for (int k=0; k<myNodes.size(); k++) {
         FemNode3d n = myNodes.get(k);
         if ((bk = n.getSolveIndex()) != -1) {
            n.getEffectiveMass (M.getBlock (bk, bk), t);
            n.getEffectiveMassForces (f, t, M.getBlockRowOffset (bk));
         }
      }

      if (myFrameRelativeP) {

         Vector3d wbod = new Vector3d(); // angular velocity in body coords
         RotationMatrix3d R = myFrame.getPose().R;
         double[] fbuf = f.getBuffer();

         wbod.inverseTransform (R, myFrame.getVelocity().w);
         Vector3d w = myFrame.getVelocity().w;

         // update inertia in Frame
         double mass = 0;
         Point3d com = new Point3d();
         SymmetricMatrix3d J = new SymmetricMatrix3d();

         // extra fictitious forces for spatial inertia due to nodal velocity
         Vector3d fv = new Vector3d();
         Vector3d fw = new Vector3d();
         Vector3d fn = new Vector3d();
         Vector3d tmp = new Vector3d();

         Point3d c = new Point3d(); // local node position rotated to world
         Vector3d v = new Vector3d(); // local node velocity rotated to world

         int bf = myFrame.getSolveIndex();

         for (int k=0; k<myNodes.size(); k++) {
            FemNode3d n = myNodes.get(k);

            if ((bk = n.getSolveIndex()) != -1) {
               c.transform (R, n.getLocalPosition());
               v.transform (R, n.getLocalVelocity());
               double m = n.getEffectiveMass();
               //System.out.println ("m " + k + " " + m);
               com.scaledAdd (m, c);
               mass += m;
               SpatialInertia.addPointRotationalInertia (J, m, c);

               if (useFrameRelativeCouplingMasses) {
                  Matrix3x6Block blk   = (Matrix3x6Block)M.getBlock (bk, bf);
                  Matrix6x3Block blkT  = (Matrix6x3Block)M.getBlock (bf, bk);
                  setCouplingMass (blk, m, R, c);

                  blkT.transpose (blk);

                  // compute fictitious forces for nodes, and accumulate nodal
                  // velocity fictitious force terms for spatial inertia

                  tmp.cross (w, v);        // tmp = 2*m (w X v)
                  tmp.scale (2*m);

                  fv.add (tmp);               // fv += 2*m (w X v)
                  fw.crossAdd (c, tmp, fw);  // fw += 2*m (c X w X v)

                  fn.set (tmp);               // fn = 2*m (w X v)

                  tmp.cross (w, c);        // tmp = m*w X c
                  tmp.scale (m);
                  fn.crossAdd (w, tmp, fn);// fn += m (w X w X c)
                  fn.inverseTransform (R);

                  // set fictitious force terms for node
                  int idx = M.getBlockRowOffset (bk);
                  fbuf[idx++] = -fn.x;
                  fbuf[idx++] = -fn.y;
                  fbuf[idx++] = -fn.z;
               }

            }
         }

         com.scale (1/mass);
         SpatialInertia.addPointRotationalInertia (J, -mass, com);

         SpatialInertia S = new SpatialInertia();
         S.set (mass, J, com);
         S.inverseTransform (R); // Frame keeps inertia in local coords
         myFrame.setInertia (S);

         myFrame.getMass (M.getBlock (bf, bf), t);
         myFrame.getEffectiveMassForces (f, t, M.getBlockRowOffset (bf));

         if (useFrameRelativeCouplingMasses) {
            int idx = M.getBlockRowOffset (bf);
            fbuf[idx++] -= fv.x;
            fbuf[idx++] -= fv.y;
            fbuf[idx++] -= fv.z;
            fbuf[idx++] -= fw.x;
            fbuf[idx++] -= fw.y;
            fbuf[idx++] -= fw.z;
         }

         if (frameMassFraction != 0) {
            scaleFrameNodeMasses (M, f, 1.0-frameMassFraction);
         }
      }
   }

   private void zero6Vector (double[] vec) {
      vec[0] = 0;
      vec[1] = 0;
      vec[2] = 0;
      vec[3] = 0;
      vec[4] = 0;
      vec[5] = 0;
   }

   private void zero3Vector (double[] vec) {
      vec[0] = 0;
      vec[1] = 0;
      vec[2] = 0;
   }

   private void scaledAdd (Wrench wr, double s, double[] vec, int idx) {
      wr.f.x += s*vec[idx++];
      wr.f.y += s*vec[idx++];
      wr.f.z += s*vec[idx++];
      wr.m.x += s*vec[idx++];
      wr.m.y += s*vec[idx++];
      wr.m.z += s*vec[idx++];
   }

   private void setFromTwist (double[] vec, int idx, Twist tw) {
      vec[idx++] = tw.v.x;
      vec[idx++] = tw.v.y;
      vec[idx++] = tw.v.z;
      vec[idx++] = tw.w.x;
      vec[idx++] = tw.w.y;
      vec[idx++] = tw.w.z;
   }

   @Override
   public void mulInverseMass (
      SparseBlockMatrix M, VectorNd a, VectorNd f) {

      double[] abuf = a.getBuffer();
      double[] fbuf = f.getBuffer();
      int asize = a.size();

      if (M.getAlignedBlockRow (asize) == -1) {
         throw new IllegalArgumentException (
            "size of 'a' not block aligned with 'M'");
      }
      if (f.size() < asize) {
         throw new IllegalArgumentException (
            "size of 'f' is less than the size of 'a'");
      }
      int bf = myFrame.getSolveIndex();
      int bk;

      if (myFrameRelativeP && bf < asize) {
         int fidx = M.getBlockRowOffset (bf);
         Wrench wtmp = new Wrench();
         double[] tmp6 = new double[6];
         for (int i=0; i<myNodes.size(); i++) {
            FemNode3d n = myNodes.get(i);
            if ((bk=n.getSolveIndex()) != -1) {
               int idx = M.getBlockRowOffset (bk);
               if (idx < asize) {
                  MatrixBlock Mfk = M.getBlock (bf, bk);
                  zero6Vector (tmp6);
                  Mfk.mulAdd (tmp6, 0, fbuf, idx);
                  Matrix3x3Block Mkk = (Matrix3x3Block)M.getBlock(bk, bk);
                  // XXX do we want to use n.mulInverseEffectiveMass?
                  scaledAdd (wtmp, -1/Mkk.m00, tmp6, 0);
               }
            }
         }
         scaledAdd (wtmp, 1, fbuf, fidx);
         // XXX do we need to use getBlock (bf, bf)???
         Matrix6dBlock Mff = (Matrix6dBlock)M.getBlock (bf, bf);
         SpatialInertia S = new SpatialInertia ();
         S.set (Mff);
         Twist ttmp = new Twist();
         S.mulInverse (ttmp, wtmp);
         setFromTwist (abuf, fidx, ttmp);
         double[] tmp3 = new double[3];
         for (int i=0; i<myNodes.size(); i++) {
            FemNode3d n = myNodes.get(i);
            if ((bk=n.getSolveIndex()) != -1) {
               int idx = M.getBlockRowOffset (bk);
               if (idx < asize) {
                  MatrixBlock Mkf = M.getBlock (bk, bf);
                  Matrix3x3Block Mkk = (Matrix3x3Block)M.getBlock(bk, bk);
                  double minv = 1/Mkk.m00;
                  zero3Vector (tmp3);
                  Mkf.mulAdd (tmp3, 0, abuf, fidx);
                  abuf[idx++] = minv*(fbuf[idx] - tmp3[0]);
                  abuf[idx++] = minv*(fbuf[idx] - tmp3[1]);
                  abuf[idx++] = minv*(fbuf[idx] - tmp3[2]);
               }
            }
         }
      }
      else {
         for (int i=0; i<myNodes.size(); i++) {
            FemNode3d n = myNodes.get(i);
            if ((bk=n.getSolveIndex()) != -1) {
               int idx = M.getBlockRowOffset (bk);
               if (idx < asize) {
                  n.mulInverseEffectiveMass (
                     M.getBlock(bk, bk), abuf, fbuf, idx);
               }
            }
         }
      }
   }

   /* =================== end Frame support ======================= */

}
