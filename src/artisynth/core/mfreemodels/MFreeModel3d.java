/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import maspack.function.ConstantFuntion3x1;
import maspack.function.Function3x1;
import maspack.geometry.AABBTree;
import maspack.geometry.BVTree;
import maspack.geometry.Boundable;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.DenseMatrix;
import maspack.matrix.EigenDecomposition;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3x3Block;
import maspack.matrix.Matrix6d;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixNd;
import maspack.matrix.NumericalException;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseMatrixNd;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.color.ColorMapBase;
import maspack.render.color.HueColorMap;
import artisynth.core.femmodels.AuxiliaryMaterial;
import artisynth.core.femmodels.FemModel;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.FemNodeNeighbor;
import artisynth.core.femmodels.FemUtilities;
import artisynth.core.femmodels.IntegrationData3d;
import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.FemControlPanel;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.IncompressibleMaterial;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.SolidDeformation;
import artisynth.core.mechmodels.CollidableBody;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.Collidable.Collidability;
import artisynth.core.mechmodels.CollidableDynamicComponent;
import artisynth.core.mechmodels.CollisionHandler;
import artisynth.core.mechmodels.ContactMaster;
import artisynth.core.mechmodels.ContactPoint;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.HasAuxState;
import artisynth.core.mechmodels.MechSystemModel;
import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.mechmodels.MeshComponentList;
import artisynth.core.mechmodels.PointList;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScalableUnits;

public class MFreeModel3d extends FemModel implements TransformableGeometry,
   ScalableUnits, MechSystemModel,
   CollidableBody, CopyableComponent {

   protected SparseBlockMatrix M = null;
   protected VectorNd b = null;

   protected PointList<MFreeNode3d> myNodes;
   // protected CountedList<MFreePoint3d> myEvaluationPoints;
   protected MFreeElement3dList myElements;
   protected MeshComponentList<MeshComponent> myMeshes;

   protected AABBTree myElementAABBTree;
   protected AABBTree myNodeAABBTree;
   protected boolean myBVTreeValid;

   protected boolean mySurfaceMeshValid = false;
   protected int myCollidableIndex;

   // record inverted elements
   private double myMinDetJ; // used to record inverted elements
   private MFreeElement3d myMinDetJElement = null; // element with
                                                   // "worst" DetJ

   public static boolean checkTangentStability = false;
   public static boolean abortOnInvertedElems = false;

   private int myNumInverted = 0; // counts number of inverted elements
//   static maspack.render.Material myInvertedMaterial =
//      maspack.render.Material.createDiffuse(1f, 0f, 0f, 0f, 32f);
   // private boolean myIncompressibleP = false;
   private double myIncompCompliance = 0;
   public static IncompMethod DEFAULT_HARD_INCOMP = IncompMethod.OFF;
   private IncompMethod myHardIncompMethod = DEFAULT_HARD_INCOMP;
   private boolean myHardIncompMethodValidP = false;
   public static IncompMethod DEFAULT_SOFT_INCOMP = IncompMethod.ELEMENT;
   private IncompMethod mySoftIncompMethod = DEFAULT_SOFT_INCOMP;
   private boolean mySoftIncompMethodValidP = false;

   private static double DEFAULT_ELEMENT_WIDGET_SIZE = 0.0;
   private double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
   PropertyMode myElementWidgetSizeMode = PropertyMode.Inherited;

   protected MFreeAuxMaterialBundleList myAdditionalMaterialsList;
   
   protected boolean myClearMeshColoring = false;
   protected boolean myComputeNodalStress = false; /// XXX TODO: extrapmat in MFreeNode3d
   protected boolean myComputeNodalStrain = false;

   public static ColorMapBase defaultColorMap = createDefaultColorMap();
   protected ColorMapBase myColorMap;
   protected PropertyMode myColorMapMode = PropertyMode.Inherited;

   public static PropertyList myProps =
      new PropertyList(MFreeModel3d.class, FemModel.class);

   static {
      myProps.add(
         "softIncompMethod", "method of enforcing soft incompressibility",
         DEFAULT_SOFT_INCOMP);
      myProps.addInheritable(
         "elementWidgetSize:Inherited",
         "size of rendered widget in each element's center",
         DEFAULT_ELEMENT_WIDGET_SIZE, "[0,1]");
      myProps.addInheritable("colorMap:Inherited", "color map for stress/strain", 
         defaultColorMap, "CE");
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
   public void getCollidables (List<Collidable> list, int level) {
      list.add (this);
   }

   public void setComputeNodalStress(boolean enable) {
      if (enable != myComputeNodalStress) {
         myComputeNodalStress = enable;
         if (!enable) {
            // release memory used for computing stress
            for (MFreeNode3d n : myNodes) {
               n.setAvgStressP(null);
            }
         }
      }
   }

   public void setComputeNodalStrain(boolean enable) {
      if (enable != myComputeNodalStrain) {
         myComputeNodalStrain = enable;
         if (!enable) {
            // release memory used for computing strain
            for (MFreeNode3d n : myNodes) {
               n.setAvgStrainP(null);
            }
         }
      }
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      myDensity = DEFAULT_DENSITY;
      myStiffnessDamping = DEFAULT_STIFFNESS_DAMPING;
      myMassDamping = DEFAULT_MASS_DAMPING;
      myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
      setMaterial(new LinearMaterial());
      myColorMap = createDefaultColorMap();
   }
   
   public static ColorMapBase createDefaultColorMap() {
      return new HueColorMap(0.7, 0);
   }

   public MFreeModel3d () {
      this(null);
   }

   public MFreeModel3d (String name) {
      super(name);
   }

   protected void initializeChildComponents() {
      myNodes = new PointList<MFreeNode3d>(MFreeNode3d.class, "nodes", "n");
      myMeshes = new MeshComponentList<MeshComponent>(MeshComponent.class, "geometry", "g");
      // myEvaluationPoints = new CountedList<MFreePoint3d>();
      myElements = new MFreeElement3dList("elements", "e");
      myAdditionalMaterialsList =
    	         new MFreeAuxMaterialBundleList("materials", "m");
      
      add(myNodes);
      add(myMeshes);
      add(myElements);
      add(myAdditionalMaterialsList);

      super.initializeChildComponents();
   }
   
   public void addMaterialBundle(MFreeAuxMaterialBundle bundle) {
	      if (!myAdditionalMaterialsList.contains(bundle)) {
	         for (MFreeAuxMaterialElementDesc d : bundle.getElements()) {
	            bundle.checkElementDesc(this, d);
	         }
	         myAdditionalMaterialsList.add(bundle);
	      }
	   }

	   public boolean removeMaterialBundle(MFreeAuxMaterialBundle bundle) {
	      return myAdditionalMaterialsList.remove(bundle);
	   }

	   public void clearMaterialBundles() {
	      myAdditionalMaterialsList.removeAll();
	   }

	   public RenderableComponentList<MFreeAuxMaterialBundle> getMaterialBundles() {
	      return myAdditionalMaterialsList;
	   }


   protected void updateBVHierarchies() {
      myElementAABBTree = new AABBTree();
      Boundable[] elements = new Boundable[numElements()];
      for (int i = 0; i < elements.length; i++) {
         elements[i] = myElements.get(i);
      }
      myElementAABBTree.build(elements, numElements());

      myNodeAABBTree = new AABBTree();
      Boundable[] nodes =
         myNodes.toArray(new Boundable[numNodes()]);
      myNodeAABBTree.build(nodes, numNodes());

      myBVTreeValid = true;
   }

   public BVTree getNodeBVTree() {
      if (myNodeAABBTree == null || !myBVTreeValid) {
         updateBVHierarchies();
      }
      return myNodeAABBTree;
   }

   public BVTree getElementBVTree() {
      if (myElementAABBTree == null || !myBVTreeValid) {
         updateBVHierarchies();
      }
      return myElementAABBTree;
   }

   public PointList<MFreeNode3d> getNodes() {
      return myNodes;
   }

   public MFreeNode3d getNode(int idx) {
      return myNodes.get(idx);
   }

   @Override
   public MFreeNode3d getByNumber(int num) {
      return myNodes.getByNumber(num);
   }

   public MFreeElement3d getElementByNumber(int num) {
      return myElements.getByNumber(num);
   }

   @Override
   public RenderableComponentList<MFreeElement3d> getElements() {
      return myElements;
   }

   public void addNodes(List<MFreeNode3d> nodes) {
      myNodes.addAll(nodes);
      // myEvaluationPoints.addAll(nodes);
   }

   public void addNode(MFreeNode3d p) {
      myNodes.add(p);
      // myEvaluationPoints.add(p);
   }

   public void addNumberedNode(MFreeNode3d p, int number) {
      myNodes.addNumbered(p, number);
      // myEvaluationPoints.add(p);
   }

   public boolean removeNode(MFreeNode3d p) {
      if (myNodes.remove(p)) {
         // myEvaluationPoints.remove(p);
         return true;
      }
      return false;
   }

   public void addElements(List<MFreeElement3d> regionList) {
      for (MFreeElement3d region : regionList) {
         addElement(region);
      }
   }

   @Override
   public MFreeElement3d getElement(int idx) {
      return myElements.get(idx);
   }

   //   private void attachElement(MFreeElement3d e) {
   //      MFreeNode3d[] nodes = e.getNodes();
   //      for (int i = 0; i < nodes.length; i++) {
   //         for (int j = 0; j < nodes.length; j++) {
   //            nodes[i].registerNodeNeighbor(nodes[j]);
   //         }
   //         nodes[i].addElementDependency(e);
   //      }
   //
   //      e.setMass(0);
   //      invalidateStressAndStiffness();
   //      computeJacobianAndGradient(e);
   //      e.computeVolumes();
   //      e.computeRestVolumes();
   //
   //      myEvaluationPoints.addAll(e.getIntegrationPoints());
   //      if (e.getWarpingPoint() != null) {
   //         myEvaluationPoints.add(e.getWarpingPoint());
   //      }
   //
   //   }

   public void addElement(MFreeElement3d e) {
      myElements.add(e);
   }

   public void addNumberedElement(MFreeElement3d e, int elemId) {
      myElements.addNumbered(e, elemId);
   }

   public boolean removeElement(MFreeElement3d e) {
      return myElements.remove(e);
   }

   public void clearElements() {
      myElements.removeAll();
      for (int i = 0; i < myNodes.size(); i++) {
         myNodes.get(i).setMass(0);
      }
   }

   public LinkedList<FemNodeNeighbor> getNodeNeighbors(FemNode3d node) {
      return node.getNodeNeighbors();
   }

   private LinkedList<FemNodeNeighbor> myEmptyNeighborList =
      new LinkedList<FemNodeNeighbor>();

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

   @Override
   protected void updateNodeForces(double t) {
      if (!myStressesValidP) {
         updateStressAndStiffness();
      }
      boolean hasGravity = !myGravity.equals(Vector3d.ZERO);
      Vector3d fk = new Vector3d();
      Vector3d fd = new Vector3d();

      // gravity, internal and mass damping
      for (MFreeNode3d n : myNodes) {
         if (hasGravity) {
            n.addScaledForce(n.getMass(), myGravity);
         }
         fk.set(n.getInternalForce());
         fd.setZero();
         if (myStiffnessDamping != 0) {
            for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
               nbr.addDampingForce(fd);
            }
            for (FemNodeNeighbor nbr : getIndirectNeighbors(n)) {
               nbr.addDampingForce(fd);
            }
            fd.scale(myStiffnessDamping);
         }
         fd.scaledAdd(myMassDamping * n.getMass(), n.getFalseVelocity(), fd);
         n.subForce(fk);
         n.subForce(fd);
      }

//      if (stepAdjust != null && myMinDetJ <= 0) {
//         stepAdjust.recommendAdjustment(0.5, "element inversion");
//      }
   }

   // DIVBLK
   public void updateStressAndStiffness() {

      for (MFreeNode3d n : myNodes) {
         n.getInternalForce().setZero();
         if (myComputeNodalStress) {
            n.zeroStress();
         }
         if (myComputeNodalStrain) {
            n.zeroStrain();
         }
         for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
            nbr.zeroStiffness();
         }
         for (FemNodeNeighbor nbr : getIndirectNeighbors(n)) {
            nbr.zeroStiffness();
         }

      }

      if (!myVolumeValid) {
         updateJacobians();
         updateVolume();
      }

      Matrix6d D = new Matrix6d();
      new SymmetricMatrix3d();
      // compute new forces as well as stiffness matrix if warping is enabled
      myMinDetJ = Double.MAX_VALUE;
      myMinDetJElement = null;
      myNumInverted = 0;

      double mins = Double.MAX_VALUE;
      MFreeElement3d minE = null;
      IncompMethod softIncomp = getSoftIncompMethod();

      for (MFreeElement3d region : myElements) {
         FemMaterial mat = getRegionMaterial(region);
         computeMaterialStressAndStiffness(region, mat, D);
         if (checkTangentStability) {
            double s = checkMatrixStability(D);
            if (s < mins) {
               mins = s;
               minE = region;
            }
         }
      }

      // incompressibility
      if (softIncomp == IncompMethod.ELEMENT) {
         for (MFreeElement3d elem : myElements) {
            FemMaterial mat = getRegionMaterial(elem);
            if (mat.isIncompressible()) {
               computeElementIncompressibility(
                  elem, (IncompressibleMaterial)mat, D);
            }
         }
      } else {

      }

      if (checkTangentStability && minE != null) {
         System.out.println("min s=" + mins + ", element " + minE.getNumber());
      }

      if (myNumInverted > 0) {
         System.out.println(
            "Warning: " + myNumInverted + " inverted elements, min detJ=" +
               myMinDetJ + ", element " + myMinDetJElement.getNumber());
         if (abortOnInvertedElems) {
            throw new NumericalException("Inverted elements");
         }
      }

      if (!myStiffnessesValidP && mySolveMatrixSymmetricP) {
         for (FemNode3d n : myNodes) {
            int bi = n.getSolveIndex();
            if (bi != -1) {
               for (FemNodeNeighbor nbr : getNodeNeighbors(n)) {
                  int bj = nbr.getNode().getSolveIndex();
                  if (bj > bi) {
                     FemNodeNeighbor nbrT =
                        nbr.getNode().getNodeNeighborBySolveIndex(bi);
                     nbrT.setTransposedStiffness(nbr);
                  }
               }
               for (FemNodeNeighbor nbr : getIndirectNeighbors(n)) {
                  int bj = nbr.getNode().getSolveIndex();
                  if (bj > bi) {
                     FemNodeNeighbor nbrT =
                        nbr.getNode().getIndirectNeighborBySolveIndex(bi);
                     nbrT.setTransposedStiffness(nbr);
                  }
               }
            }
         }
      }
      myStiffnessesValidP = true;
      myStressesValidP = true;
   }

   private boolean softNodalIncompressAllowed() {
      return (myMaterial instanceof IncompressibleMaterial);
   }

   private boolean hardNodalIncompressAllowed() {
      return (myMaterial instanceof IncompressibleMaterial);
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
      myHardIncompMethod = method;
      myHardIncompMethodValidP = false;
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

   public IncompMethod getSoftIncompMethod() {
      if (!mySoftIncompMethodValidP) {
         if (!softNodalIncompressAllowed() &&
            (mySoftIncompMethod == IncompMethod.NODAL ||
            mySoftIncompMethod == IncompMethod.AUTO)) {
            mySoftIncompMethod = IncompMethod.ELEMENT;
         }
         else if (mySoftIncompMethod == IncompMethod.AUTO) {
            if (myElements.size() > numActiveNodes()) {
               mySoftIncompMethod = IncompMethod.NODAL;
            }
            else {
               mySoftIncompMethod = IncompMethod.ELEMENT;
            }
         }
         mySoftIncompMethodValidP = true;
      }
      return mySoftIncompMethod;
   }

   public void setSoftIncompMethod(IncompMethod method) {
      if (method == IncompMethod.ON ||
         method == IncompMethod.OFF) {
         throw new IllegalArgumentException(
            "Unsupported method: " + method);
      }
      mySoftIncompMethod = method;
      mySoftIncompMethodValidP = false;
   }

   public void setMaterial(FemMaterial mat) {
      super.setMaterial(mat);
   }

   private FemMaterial getRegionMaterial(MFreeElement3d e) {
      FemMaterial mat = e.getMaterial();
      if (mat == null) {
         mat = myMaterial;
      }
      return mat;
   }

   public void updateJacobians() {
      for (MFreeElement3d region : myElements) {
         computeJacobianAndGradient(region);
      }
   }

   private void computeJacobianAndGradient(MFreeElement3d region) {

      ArrayList<MFreeIntegrationPoint3d> ipnts = region.getIntegrationPoints();
      ArrayList<IntegrationData3d> idata = region.getIntegrationData();
      region.setInverted(false);

      for (int i = 0; i < ipnts.size(); i++) {
         MFreeIntegrationPoint3d ipnt = ipnts.get(i);
         IntegrationData3d idat = idata.get(i);
         ipnt.computeJacobianAndGradient(idat.getInvJ0());
         double detJ = ipnt.computeInverseJacobian();

         if (detJ < myMinDetJ) {
            myMinDetJ = detJ;
            myMinDetJElement = region;
         }
         if (detJ <= 0) {
            region.setInverted(true);
            myNumInverted++;
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

   private void computeElementIncompressibility(MFreeElement3d e,
      IncompressibleMaterial imat, Matrix6d D) {

      ArrayList<MFreeIntegrationPoint3d> ipnts = e.getIntegrationPoints();
      ArrayList<IntegrationData3d> idata = e.getIntegrationData();
      ArrayList<int[]> idxs = e.getIntegrationIndices();
      VectorNd iwgts = e.getIntegrationWeights();

      double pressure = 0;
      MatrixNd Rinv = new MatrixNd(1, 1);
      Matrix6d Dinc = new Matrix6d();
      SymmetricMatrix3d Sinc = new SymmetricMatrix3d();

      double Jpartial = e.getVolume() / e.getRestVolume();
      pressure = imat.getEffectivePressure(Jpartial);
      Rinv.set(0, 0, imat.getEffectiveModulus(Jpartial) / e.getRestVolume());

      MatrixBlock[] constraints = e.getIncompressConstraints();
      for (int i = 0; i < e.myNodes.length; i++) {
         constraints[i].setZero();
      }

      imat.computePressureStress(Sinc, pressure);
      imat.computePressureTangent(Dinc, pressure);

      for (int k = 0; k < ipnts.size(); k++) {
         MFreeIntegrationPoint3d pt = ipnts.get(k);
         IntegrationData3d dt = idata.get(k);
         pt.setWeight(iwgts.get(k));
         pt.computeJacobianAndGradient(dt.getInvJ0());
         double detJ = pt.computeInverseJacobian();
         double dv = detJ * iwgts.get(k);
         Vector3d[] GNx = pt.updateShapeGradient(pt.getInvJ());
         int[] ids = idxs.get(k);

         for (int i = 0; i < e.myNodes.length; i++) {
            FemNode3d nodei = e.myNodes[i];
            int bi = nodei.getSolveIndex();

            if (e.isTermActive(i, i)) {
               FemUtilities.addStressForce(
                  nodei.getInternalForce(), GNx[ids[i]], Sinc, dv);
               FemUtilities.addToIncompressConstraints(
                  constraints[i], new double[] { 1 }, GNx[ids[i]], dv);
            }
            if (bi != -1) {
               for (int j = 0; j < e.myNodes.length; j++) {
                  int bj = e.myNodes[j].getSolveIndex();
                  if (!mySolveMatrixSymmetricP || bj >= bi) {
                     e.addMaterialStiffness(
                        i, j, GNx[ids[i]], Dinc, Sinc,
                        GNx[ids[j]], dv);
                     e.addPressureStiffness(
                        i, j, GNx[ids[i]], pressure, GNx[ids[j]], dv);
                  }

               } // looping through nodes
            } // if bi != -1

         } // looping through nodes computing stress
      } // looping through ipnts

      for (int i = 0; i < e.myNodes.length; i++) {
         int bi = e.myNodes[i].getSolveIndex();
         if (bi != -1) {
            for (int j = 0; j < e.myNodes.length; j++) {
               int bj = e.myNodes[j].getSolveIndex();
               if (!mySolveMatrixSymmetricP || bj >= bi) {
                  e.addDilationalStiffness(i, j,
                     Rinv, constraints[i], constraints[j]);
               }
            }
         }
      }
   }

   // DIVBLK
   private void computeMaterialStressAndStiffness(MFreeElement3d e,
      FemMaterial mat, Matrix6d D) {

      ArrayList<MFreeIntegrationPoint3d> ipnts = e.getIntegrationPoints();
      ArrayList<IntegrationData3d> idata = e.getIntegrationData();
      ArrayList<int[]> idxs = e.getIntegrationIndices();
      VectorNd iwgts = e.getIntegrationWeights();

      MFreeNode3d[] nodes = e.getNodes();
      double[] nodalExtrapMat = null;
      
      SymmetricMatrix3d myEps = new SymmetricMatrix3d();
      MFreeIntegrationPoint3d wpnt = e.getWarpingPoint();
      
      SymmetricMatrix3d sigmaAux = null;
      Matrix6d DAux = null;
      if (e.numAuxiliaryMaterials() > 0) {
         sigmaAux = new SymmetricMatrix3d();
         DAux = new Matrix6d();
      }

      // see if material is linear
      boolean corotated = false;
      LinearMaterial linMat = null;
      if (mat instanceof LinearMaterial) {
         linMat = (LinearMaterial)mat;
         corotated = linMat.isCorotated();
         IntegrationData3d wdat = e.getWarpingData();
         wpnt.computeJacobianAndGradient(wdat.getInvJ0());
         wpnt.getStress().setZero();
         if (corotated) {
            e.computeWarping(wpnt.getF(), myEps);
         }
         else {
            myEps.setSymmetric(wpnt.getF());
         }
         // compute Cauchy strain
         myEps.m00 -= 1;
         myEps.m11 -= 1;
         myEps.m22 -= 1;
         
         for (int i = 0; i < nodes.length; i++) {
            int bi = nodes[i].getSolveIndex();
            if (bi != -1) {
               MFreeNode3d n = nodes[i];
               if (!myStiffnessesValidP) {
                  for (int j = 0; j < nodes.length; j++) {
                     int bj = nodes[j].getSolveIndex();
                     if (!mySolveMatrixSymmetricP || bj >= bi) {
                        e.addNodeStiffness(i, j, corotated);
                     }
                  }
               }
               e.addNodeForce(n.getInternalForce(), i, corotated);
            }
         }
      }

      
      //      if (myComputeNodalStress || myComputeNodalStrain) {
      //         nodalExtrapMat = e.getNodalExtrapolationMatrix();
      //         if (linMat != null) {
      //            linMat.addStress(wpnt.getStress(), 
      //                  myEps, corotated ? e.myWarper.R : null);
      //            for (int i = 0; i < nodes.length; i++) {
      //               MFreeNode3d nodei = nodes[i];
      //               if (myComputeNodalStress) {
      //                  nodei.addScaledStress(
      //                     1.0 / nodei.numAdjacentElements(), wpnt.getStress());
      //               }
      //               if (myComputeNodalStrain) {
      //                  nodei.addScaledStrain(
      //                     1.0 / nodei.numAdjacentElements(), myEps);
      //               }
      //            }
      //         }
      //      }
      
      e.setInverted(false); // will check this below
      if (linMat == null || e.numAuxiliaryMaterials() > 0) {
         
         SolidDeformation def = new SolidDeformation();
         
         for (int k = 0; k < ipnts.size(); k++) {
            MFreeIntegrationPoint3d pt = ipnts.get(k);
            IntegrationData3d dt = idata.get(k);
            pt.setWeight(iwgts.get(k));
            pt.computeJacobianAndGradient(dt.getInvJ0());
            def.setF (pt.getF());
            double detJ = pt.computeInverseJacobian();

            if (detJ < myMinDetJ) {
               myMinDetJ = detJ;
               myMinDetJElement = e;
            }
            if (detJ <= 0) {
               e.setInverted(true);
               myNumInverted++;
            }
            double dv = detJ * iwgts.get(k);
            Vector3d[] GNx = pt.updateShapeGradient(pt.getInvJ());
            int[] ids = idxs.get(k);

            if (linMat != null) {
               pt.getStress().setZero();
               if (D != null) {
                  D.setZero();
               }
            }
            else {
               Matrix3d Q = dt.getFrame();
               if (Q == null) {
                  Q = Matrix3d.IDENTITY;
               }
               pt.setAveragePressure(0);
               mat.computeStress (pt.getStress(), def, Q, null);
               mat.computeTangent (D, pt.getStress(), def, Q, null);
               //mat.computeStress (pt.getStress(), pt, dt, null);
               //mat.computeTangent (D, pt.getStress(), pt, dt, null);
            }
            // System.out.println ("k="+k+"\n" + pt.sigma.toString ("%10.6f"));
            if (e.numAuxiliaryMaterials() > 0) {
               for (AuxiliaryMaterial aux : e.myAuxMaterials) {
                  aux.computeStress (sigmaAux, def, pt, dt, mat);
                  pt.getStress().add (sigmaAux);
                  if (D != null) {
                     aux.computeTangent (DAux, sigmaAux, def, pt, dt, mat);
                     D.add (DAux);
                  }
                  
//                  if (D != null) {
//                     aux.addStressAndTangent(pt.getStress(), D, pt, dt, mat);
//                  } else {
//                     aux.addStress(pt.getStress(), pt, dt, mat);
//                  }
               }
            }

            // if (mat.getViscoBehavior() != null) {
            //    ViscoelasticBehavior veb = mat.getViscoBehavior();
            //    // XXX XXX instead of 0.01 we want h, the step size
            //    veb.computeStress(pt.getStress(), pt, null, 0.01);
            //    if (D != null) {
            //       veb.computeTangent(D, pt, null, 0.01);
            //    }
            // } else {
            //    dt.clearState();
            // }

            for (int i = 0; i < e.myNodes.length; i++) {
               FemNode3d nodei = e.myNodes[i];
               int bi = nodei.getSolveIndex();

               if (e.isTermActive(i, i)) {
                  FemUtilities.addStressForce(
                     nodei.getInternalForce(), GNx[ids[i]], pt.getStress(), dv);
               }

               if (D != null) {
                  if (bi != -1) {
                     for (int j = 0; j < e.myNodes.length; j++) {
                        int bj = e.myNodes[j].getSolveIndex();
                        if (!mySolveMatrixSymmetricP || bj >= bi) {
                           e.addMaterialStiffness(
                              i, j, GNx[ids[i]], D, pt.getStress(),
                              GNx[ids[j]], dv);
                        }

                     } // looping through nodes
                  } // if bi != -1
               } // if D != null
               
               //               if (nodalExtrapMat != null) {
               //                  if (myComputeNodalStress) {
               //                     double a = nodalExtrapMat[i * ipnts.length + k];
               //                     if (a != 0) {
               //                        nodei.addScaledStress(
               //                           a / nodei.numAdjacentElements(), pt.sigma);
               //                     }
               //                  }
               //                  if (myComputeNodalStrain) {
               //                     double a = nodalExtrapMat[i * ipnts.length + k];
               //                     if (a != 0) {
               //                        pt.computeRightCauchyGreen(C);
               //                        C.m00 -= 1;
               //                        C.m11 -= 1;
               //                        C.m22 -= 1;
               //                        C.scale(0.5);
               //                        nodei.addScaledStrain(
               //                           a / nodei.numAdjacentElements(), C);
               //                     }
               //                  }
               //               }
               
               
            } // looping through nodes computing stress
         } // looping through ipnts
      } // checking if nonlinear material component

   }

   public void checkInversion() {
      myMinDetJ = Double.MAX_VALUE;
      myMinDetJElement = null;
      myNumInverted = 0;
      for (MFreeElement3d e : myElements) {
         // FemMaterial mat = getRegionMaterial(e);
         // if (!(mat instanceof LinearMaterial)) {
         computeJacobianAndGradient(e);
         // }
      }
   }

   public int numSurfaceMeshes() {
      return myMeshes.size();
   }

   public MeshBase getMesh(int idx) {
      return myMeshes.get(idx).getMesh();
   }
   
   @Override
   public PolygonalMesh getCollisionMesh() {
      for (MeshComponent mc : myMeshes) {
         if (mc.getMesh() instanceof PolygonalMesh) {
            return (PolygonalMesh)(mc.getMesh());
         }
      }
      return null;
   }
   
   @Override 
   public Collidability getCollidable() {
      PolygonalMesh mesh = getCollisionMesh();
      if (mesh != null) {
         return Collidability.EXTERNAL;
      }
      return Collidability.OFF;
   }
   
   @Override
   public Collidable getCollidableAncestor() {
      return null;
   }

   @Override
   public boolean isCompound() {
      return false;
   }

   @Override
   public boolean isDeformable () {
      return true;
   }
   
   public Collection<MeshComponent> getMeshes() {
      return myMeshes;
   }

   public MeshComponent getMeshComponent(int idx) {
      return myMeshes.get(idx);
   }
   
   protected MeshComponent findMesh(MeshBase mesh) {
      for (MeshComponent mc : myMeshes) {
         if (mc.getMesh() == mesh) {
            return mc;
         }
      }
      return null;
   }

   public boolean removeMesh(MeshBase mesh) {

      // remove points
      MeshComponent mc = findMesh(mesh);
      if (mc != null) {
         //         for (Vertex3d vtx : mesh.getVertices()) {
         //            myEvaluationPoints.remove(vtx);
         //         }
         return myMeshes.remove(mc);
      }
      return false;
   }

   public MeshComponent addMesh(MeshBase mesh) {
      
      // check that it's an mfree mesh
      if (!(mesh.getVertex(0) instanceof MFreeVertex3d)) {
        mesh = MFreeFactory.convertToMFreeMesh(mesh, getNodeBVTree(), 1e-5);
      }
      
      MeshComponent mc = new MeshComponent(mesh, null, null);
      if (mesh.getRenderProps() != null) {
         mc.setRenderProps(mesh.getRenderProps());
      }
      
      myMeshes.add(mc);
      // add points
      //      for (Vertex3d vtx : mesh.getVertices()) {
      //         if (vtx instanceof MFreeVertex3d) {
      //            myEvaluationPoints.add((MFreeVertex3d)vtx);
      //         }
      //      }
      mesh.setFixed(false);
      mesh.setColorsFixed(false);
      
      return mc;
   }

   public void clearMeshes() {

      // clear points
      //      for (MeshComponent mesh : myMeshes) {
      //         for (Vertex3d vtx : mesh.getMesh().getVertices()) {
      //            myEvaluationPoints.remove(vtx);
      //         }
      //      }
      myMeshes.clear();
   }

   protected boolean checkSolveMatrixIsSymmetric() {
      if (!myMaterial.hasSymmetricTangent()) {
         return false;
      }
      for (int i = 0; i < myElements.size(); i++) {
         MFreeElement3d e = myElements.get(i);
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

   private void addBlockVelJacobian(
      SparseNumberedBlockMatrix M, MFreeNode3d node,
      FemNodeNeighbor nbr, double s) {

      if (nbr.getNode().getSolveIndex() != -1) {
         Matrix3x3Block blk =
            (Matrix3x3Block)M.getBlockByNumber(nbr.getBlockNumber());
         if (nbr.getNode() == node && node.isActive()) {
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
         MFreeNode3d node = myNodes.get(i);
         if (node.getSolveIndex() != -1) {
            for (FemNodeNeighbor nbr : getNodeNeighbors(node)) {
               addBlockVelJacobian(M, node, nbr, s);
            }
            for (FemNodeNeighbor nbr : getIndirectNeighbors(node)) {
               addBlockVelJacobian(M, node, nbr, s);
            }
         }
      }
   }

   private void addBlockPosJacobian(
      SparseNumberedBlockMatrix M, MFreeNode3d node,
      FemNodeNeighbor nbr, double s) {

      if (nbr.getNode().getSolveIndex() != -1) {
         Matrix3x3Block blk =
            (Matrix3x3Block)M.getBlockByNumber(nbr.getBlockNumber());
         nbr.addPosJacobian(blk, s);
      }

   }

   public void addPosJacobian(
      SparseNumberedBlockMatrix M, double s) {

      if (!myStressesValidP || !myStiffnessesValidP) {
         updateStressAndStiffness();
      }

      for (int i = 0; i < myNodes.size(); i++) {
         MFreeNode3d node = myNodes.get(i);
         if (node.getSolveIndex() != -1) {
            for (FemNodeNeighbor nbr : getNodeNeighbors(node)) {
               addBlockPosJacobian(M, node, nbr, s);
            }
            for (FemNodeNeighbor nbr : getIndirectNeighbors(node)) {
               addBlockPosJacobian(M, node, nbr, s);
            }
         }
      }
   }

   private void addStiffnessBlock(
      SparseNumberedBlockMatrix S, FemNodeNeighbor nbr, int bi) {

      int bj = nbr.getNode().getSolveIndex();
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
      //nbr.setBlock(blk);
      nbr.setBlockNumber(blkNum);
   }

   public void addSolveBlocks(SparseNumberedBlockMatrix S) {

      for (int i = 0; i < myNodes.size(); i++) {
         FemNode3d node = myNodes.get(i);
         int bi = node.getSolveIndex();
         if (bi != -1) {
            for (FemNodeNeighbor nbr : getNodeNeighbors(node)) {
               addStiffnessBlock(S, nbr, bi);
            }
            for (FemNodeNeighbor nbr : getIndirectNeighbors(node)) {
               addStiffnessBlock(S, nbr, bi);
            }
         }
      }

   }

   public void recursivelyInitialize(double t, int level) {

      if (t == 0) {
         for (MFreeElement3d region : myElements) {
            region.invalidateRestData();
            region.setInverted(false);
         }
         for (MFreeNode3d n : myNodes) {
            n.zeroStress();
         }
         invalidateStressAndStiffness();
         updateAllRestVolumes();
      }

      super.recursivelyInitialize(t, level);
   }

//   public boolean recursivelyCheckStructureChanged() {
//      return false;
//   }

   public double integrate(Function3x1 fun) {
      double out = 0;
      if (!myStiffnessesValidP) {
         updateJacobians();
      }

      for (MFreeElement3d elem : myElements) {
         for (int i = 0; i < elem.numIntegrationPoints(); i++) {
            MFreeIntegrationPoint3d ipnt = elem.getIntegrationPoint(i);
            IntegrationData3d idat = elem.getIntegrationData(i);
            double iwgt = elem.getIntegrationWeight(i);
            int[] idxs = elem.getIntegrationIndices(i);
            VectorNd shapeFunc = ipnt.getShapeWeights();

            for (int j = 0; j < elem.numNodes(); j++) {
               if (elem.isTermActive(j, j)) {
                  double f =
                     fun.eval(ipnt.getPosition()) * iwgt
                        * shapeFunc.get(idxs[j]);
                  f = f * ipnt.getDetF() * idat.getDetJ0();
                  out += f;
               }
            }
         }
      }

      return out;
   }

   public double integrateVolume() {
      return integrate(new ConstantFuntion3x1(1));
   }

   public void updateAllRestVolumes() {

      // clear nodal volumes
      for (MFreeNode3d node : myNodes) {
         node.setRestVolume(0);
         node.setPartitionRestVolume(0);
      }

      double totalVolume = 0;

      for (MFreeElement3d elem : myElements) {
         MFreeNode3d[] nodes = elem.getNodes();
         double elemVolume = 0;

         for (int i = 0; i < elem.numIntegrationPoints(); i++) {
            MFreeIntegrationPoint3d ipnt = elem.getIntegrationPoint(i);
            IntegrationData3d idat = elem.getIntegrationData(i);
            double iwgt = elem.getIntegrationWeight(i);
            int[] idxs = elem.getIntegrationIndices(i);
            VectorNd shapeFunc = ipnt.getShapeWeights();

            double f = iwgt * idat.getDetJ0();

            // element
            elemVolume += f;

            for (int j = 0; j < elem.numNodes(); j++) {
               if (elem.isTermActive(j, j)) {

                  double g = f * shapeFunc.get(idxs[j]);

                  // total
                  totalVolume += g;

                  // nodal
                  nodes[j].addRestVolume(f);
                  nodes[j].addPartitionRestVolume(g);
               }
            }
         }
         elem.setRestVolume(elemVolume);

      }

      myRestVolume = totalVolume;

   }

   public void updateAllVolumes() {

      double totalVolume = 0;
      if (!myStiffnessesValidP) {
         updateJacobians();
      }

      // clear nodal volumes
      for (MFreeNode3d node : myNodes) {
         node.setVolume(0);
         node.setPartitionVolume(0);
      }

      for (MFreeElement3d elem : myElements) {
         MFreeNode3d[] nodes = elem.getNodes();
         double elemVolume = 0;

         for (int i = 0; i < elem.numIntegrationPoints(); i++) {
            MFreeIntegrationPoint3d ipnt = elem.getIntegrationPoint(i);
            IntegrationData3d idat = elem.getIntegrationData(i);
            double iwgt = elem.getIntegrationWeight(i);
            int[] idxs = elem.getIntegrationIndices(i);
            VectorNd shapeFunc = ipnt.getShapeWeights();

            double f = iwgt * ipnt.getDetF() * idat.getDetJ0();

            // element
            elemVolume += f;

            for (int j = 0; j < elem.numNodes(); j++) {
               if (elem.isTermActive(j, j)) {

                  double g = f * shapeFunc.get(idxs[j]);

                  // total
                  totalVolume += g;

                  // nodal
                  nodes[j].addVolume(f);
                  nodes[j].addPartitionVolume(g);
               }
            }
         }
         elem.setVolume(elemVolume);

      }

      myVolume = totalVolume;
      myVolumeValid = true;
   }

   public double integrateMass() {
      return integrate(new ConstantFuntion3x1(myDensity));
   }

   public double computeConsistentMassMatrixEntry(MFreeNode3d node1,
      MFreeNode3d node2) {

      // collect elements
      ArrayList<MFreeElement3d> depElems = new ArrayList<MFreeElement3d>();
      for (MFreeElement3d elem : node1.getMFreeElementDependencies()) {
         if (!depElems.contains(elem)) {
            if (elem.isTermActive(node1, node2)) {
               depElems.add(elem);
            }
         }
      }

      if (!myStiffnessesValidP) {
         for (MFreeElement3d elem : depElems) {
            computeJacobianAndGradient(elem);
         }
      }

      double m = 0;
      for (MFreeElement3d elem : depElems) {

         int idx1 = elem.getNodeIdx(node1);
         int idx2 = elem.getNodeIdx(node2);

         if (elem.isTermActive(idx1, idx2)) {
            for (int i = 0; i < elem.numIntegrationPoints(); i++) {
               MFreeIntegrationPoint3d ipnt = elem.getIntegrationPoint(i);
               IntegrationData3d idat = elem.getIntegrationData(i);
               double iwgt = elem.getIntegrationWeight(i);

               m +=
                  myDensity * ipnt.getShapeCoordinate(node1)
                     * ipnt.getShapeCoordinate(node2)
                     * iwgt * ipnt.getDetF() * idat.getDetJ0();
            }
         }

      }
      return m;

   }

   public SparseMatrixNd computeConsistentMassMatrix() {

      int nNodes = myNodes.size();
      SparseMatrixNd M = new SparseMatrixNd(nNodes, nNodes);

      updateJacobians();

      for (MFreeElement3d e : myElements) {
         for (int k = 0; k < e.numIntegrationPoints(); k++) {

            MFreeIntegrationPoint3d ipnt = e.getIntegrationPoint(k);
            IntegrationData3d idat = e.getIntegrationData(k);
            double iwgt = e.getIntegrationWeight(k);
            int[] idxs = e.getIntegrationIndices(k);
            VectorNd shapeCoords = ipnt.getShapeWeights();

            for (int i = 0; i < e.numNodes(); i++) {
               for (int j = i; j < e.numNodes(); j++) {
                  if (e.isTermActive(i, j)) {
                     int bi = e.getNode(i).getNumber();
                     int bj = e.getNode(j).getNumber();

                     double m = myDensity * shapeCoords.get(idxs[i])
                        * shapeCoords.get(idxs[j])
                        * iwgt * ipnt.getDetF() * idat.getDetJ0();

                     M.set(bi, bj, M.get(bi, bj) + m);
                     if (i != j) {
                        M.set(bj, bi, M.get(bj, bi) + m);
                     }

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
         for (int i = 0; i < massMatrix.rowSize(); i++) {
            double rowSum = 0;
            for (int j = 0; j < massMatrix.colSize(); j++) {
               rowSum += massMatrix.get(i, j);
            }
            // rowSum += massMatrix.get(i, i);
            massMatrixDiag.set(i, rowSum);
         }
      }

      double mTrace = massMatrixDiag.sum();

      for (MFreeNode3d node : myNodes) {
         int i = node.getNumber();
         double m = totalMass / mTrace * massMatrixDiag.get(i);
         node.setMass(m);
      }

   }

   @Override
   public void initialize(double t0) {
      super.initialize(t0);
      updatePosState();
      updateVelState();
   }

   // @Override
   // public synchronized StepAdjustment advance(
   //    double t0, double t1, int flags) {

   //    initializeAdvance (t0, t1, flags);

   //    if (t0 == 0) {
   //       updateForces(t0);
   //    }

   //    if (!myDynamicsEnabled) {
   //       mySolver.nonDynamicSolve(t0, t1, myStepAdjust);
   //       recursivelyFinalizeAdvance(null, t0, t1, flags, 0);
   //    }
   //    else {
   //       mySolver.solve(t0, t1, myStepAdjust);
   //       DynamicComponent c = checkVelocityStability();
   //       if (c != null) {
   //          throw new NumericalException(
   //             "Unstable velocity detected, component "
   //                + ComponentUtils.getPathName(c));
   //       }
   //       recursivelyFinalizeAdvance(myStepAdjust, t0, t1, flags, 0);
   //       // checkForInvertedElements();
   //    }

   //    finalizeAdvance (t0, t1, flags);
   //    return myStepAdjust;
   // }

   /**
    * {@inheritDoc}
    */
   public DynamicComponent checkVelocityStability() {
      PointList<MFreeNode3d> nodes = getNodes();
      for (int i = 0; i < nodes.size(); i++) {
         MFreeNode3d node = nodes.get(i);
         Vector3d vel = node.getFalseVelocity();
         if (node.velocityLimitExceeded (myMaxTranslationalVel, 0)) {
            return node;
         }
      }
      return null;
   }

   public double getEnergy() {
      double e = 0;
      for (MFreeNode3d n : getNodes()) {
         e += n.getFalseVelocity().normSquared() / 2;
      }
      return e;
   }

   public void setSurfaceRendering(SurfaceRender mode) {
      super.setSurfaceRendering(mode);
      if (mode != SurfaceRender.Stress && mode != SurfaceRender.Strain) {
         myClearMeshColoring = true;
      }
   }

   public void render(Renderer renderer, int flags) {
      super.render(renderer, flags);
   }

   public void prerender(RenderList list) {
      list.addIfVisible(myNodes);
      list.addIfVisible(myElements);
      list.addIfVisible(myMarkers);
      list.addIfVisible(myMeshes);
      myAdditionalMaterialsList.prerender(list);
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public void invalidateSurfaceMesh() {
      mySurfaceMeshValid = false;
   }

   protected void clearCachedData(ComponentChangeEvent e) {
      super.clearCachedData(e);
      mySolveMatrix = null;
      myBVTreeValid = false;
   }

   private void handleGeometryChange() {
      
      myBVTreeValid = false;
      invalidateStressAndStiffness();
      updatePosState(); // should this be updateSlavePos()?
   }

   public void componentChanged(ComponentChangeEvent e) {
      if (e.getCode() == ComponentChangeEvent.Code.STRUCTURE_CHANGED) {
         if (e.getComponent() == myElements || e.getComponent() == myNodes) {
            invalidateSurfaceMesh();
         }
         clearCachedData(null);
         // should invalidate elasticity
      }
      else if (e.getCode() == ComponentChangeEvent.Code.GEOMETRY_CHANGED) { 
         handleGeometryChange();
      }
      notifyParentOfChange(e);
   }

   @Override
   protected void notifyStructureChanged(Object comp) {
      clearCachedData(null);
      super.notifyStructureChanged(comp);
   }

   public void updateSlavePos () {
      super.updateSlavePos ();

      // nodes
      for (MFreeNode3d node : myNodes) {
         node.updatePosAndVelState();
      }
      
      // integration points
      for (MFreeElement3d elem : myElements) {
         for (MFreeIntegrationPoint3d mfip : elem.getIntegrationPoints()) {
            mfip.updatePosAndVelState();
         }
         MFreePoint3d warp = elem.getWarpingPoint();
         if (warp != null) {
            warp.updatePosAndVelState();
         }
      }
      
      // meshes
      for (MeshComponent mc : myMeshes) {
         mc.updateSlavePos();
         for (Vertex3d vtx : mc.getMesh().getVertices()) {
            if (vtx instanceof MFreeVertex3d) {
               ((MFreeVertex3d)vtx).updatePosAndVelState();
            }
         }
      }
      
      //      for (MFreePoint3d pnt : myEvaluationPoints) {
      //         pnt.updatePosAndVelState();
      //      }
      //      for (MeshComponent mc : myMeshes) {
      //         mc.updateSlavePos();
      //      }
      myBVTreeValid = false;
   }  
   
//   public void recursivelyUpdatePosState(int flags) {
//      super.recursivelyUpdatePosState(flags);
//
//      for (MFreePoint3d pnt : myEvaluationPoints) {
//         pnt.updatePosAndVelState();
//      }
//
//      for (MeshComponent mc : myMeshes) {
//         mc.updateSlavePos();
//      }
//
//      myBVTreeValid = false;
//   }

   public void recursivelyFinalizeAdvance(
      StepAdjustment stepAdjust, double t0, double t1, int flags, int level) {

      // special implementation of updateVolume that checks for inverted
      // Jacobians
      double minDetJ = Double.MAX_VALUE;
      myNumInverted = 0;

      for (MFreeElement3d e : myElements) {
         FemMaterial mat = getRegionMaterial(e);
         double detJ = e.computeVolumes();
         e.setInverted(false);
         if (!(mat instanceof LinearMaterial)) {
            if (detJ < minDetJ) {
               minDetJ = detJ;
            }
            if (detJ <= 0) {
               e.setInverted(true);
               myNumInverted++;
            }
         }
      }
      if (stepAdjust != null && minDetJ <= 0) {
         stepAdjust.recommendAdjustment(0.5, "element inversion");
      }
   }

   public double updateVolume() {
      // myVolume = integrateVolume();
      // myVolumeValid = true;
      updateAllVolumes();
      return myVolume;
   }

   public void invalidateStressAndStiffness() {
      super.invalidateStressAndStiffness();
      // should invalidate matrices for incompressibility here. However, at the
      // moment these are being rebuilt for each calculation anyway
   }

   public void invalidateRestData() {
      super.invalidateRestData();
   }

   public void resetRestPosition() {
      for (FemNode3d n : myNodes) {
         n.resetRestPosition();
      }
      invalidateRestData();
      notifyParentOfChange(new ComponentChangeEvent(Code.STRUCTURE_CHANGED));
   }

   public double updateConstraints(double t, int flags) {
      return 0;
   }
   
   public void getConstrainedComponents (List<DynamicComponent> list) {
      if (getHardIncompMethod() != IncompMethod.OFF) {
         list.addAll (myNodes);
      }
   }
   
   public int setBilateralImpulses(VectorNd lam, double h, int idx) {
      return idx;
   }

   public void zeroImpulses() {
   }

   public int getBilateralImpulses(VectorNd lam, int idx) {
      return idx;
   }

   public void getBilateralSizes(VectorNi sizes) {
   }

   // DIVBLK
   public int addBilateralConstraints(
      SparseBlockMatrix GT, VectorNd dg, int numb) {
      return numb;
   }

   public int getBilateralInfo(ConstraintInfo[] ginfo, int idx) {
      return idx;
   }

   public static void addControls(
      ControlPanel controlPanel, FemModel femModel, ModelComponent topModel) {
      FemControlPanel.addFemControls(controlPanel, femModel, topModel);
      controlPanel.addWidget(femModel, "surfaceRendering");
      controlPanel.addWidget(femModel, "elementWidgetSize", 0, 1.0);
   }

   public void dispose() {
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
      return false;
   }

   public void transformGeometry(AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      
      // This is now handled in the node.transformGeometry():
//      for (MFreeNode3d n : myNodes) {
//         n.getRestPosition().transform(gtr);
//      }
//      for (MFreeElement3d region : myElements) {
//         region.invalidateRestData();
//      }
      updateLocalAttachmentPos();
      invalidateStressAndStiffness();
      updatePosState();
      
      if (myMinBound != null) {
         gtr.transformPnt (myMinBound);
      }
      if (myMaxBound != null) {
         gtr.transformPnt (myMaxBound);
      }
   }   

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      context.addAll (myNodes);
   } 
   
   public boolean getCopyReferences(List<ModelComponent> refs,
      ModelComponent ancestor) {
      // TODO Auto-generated method stub
      return false;
   }

   public void getAuxStateComponents(List<HasAuxState> comps, int level) {
      // TODO Auto-generated method stub

   }

   //   public CountedList<MFreePoint3d> getEvaluationPoints() {
   //      return myEvaluationPoints;
   //   }

   public void computeShapeMatrix() {

      M = new SparseBlockMatrix();
      b = new VectorNd();
      // for (int i = 0; i< numNodes(); i++) {
      // MFreeNode3d nodei = myNodes.get(i);
      // VectorNd coords = nodei.getNodeCoordinates();
      // ArrayList<MFreeNode3d> deps = nodei.getDependentNodes();
      // for (int j=0; j<deps.size(); j++) {
      // int col = deps.get(j).getNumber();
      // M.set(i,col,coords.get(j));
      // }
      // }

      // LUDecomposition lu = new LUDecomposition(M);
      // MatrixNd out = new MatrixNd();
      // lu.inverse(out);
      //
      // System.out.println("M = [");
      // for (int i=0; i<numNodes(); i++) {
      // for (int j=0; j<numNodes(); j++) {
      // System.out.print(" " + M.get(i, j));
      // }
      // System.out.println();
      // }
      // System.out.println("];");
      //
      // System.out.println("Mi = [");
      // for (int i=0; i<numNodes(); i++) {
      // for (int j=0; j<numNodes(); j++) {
      // System.out.print(" " + out.get(i, j));
      // }
      // System.out.println();
      // }
      // System.out.println("];");

   }
   
   @Override
   public void updateBounds(Vector3d pmin, Vector3d pmax) {
      // TODO Auto-generated method stub
      updatePosState();
      super.updateBounds(pmin, pmax);
      for (MeshComponent mc : myMeshes) {
         mc.updateBounds(pmin, pmax);
      }
   }
   
   public ColorMapBase getColorMap() {
      return myColorMap;
   }
   
   public void setColorMap(ColorMapBase colorMap) {
      myColorMap = colorMap;
      myColorMapMode =
         PropertyUtils.propagateValue (
            this, "colorMap", colorMap, myColorMapMode);
   }

   public PropertyMode getColorMapMode() {
      return myColorMapMode;
   }
   
   public void setColorMapMode(PropertyMode mode) {
      if (mode != myColorMapMode) {
         myColorMapMode = PropertyUtils.setModeAndUpdate (
            this, "colorMap", myColorMapMode, mode);
      }
   }

   public void getVertexMasters (List<ContactMaster> mlist, Vertex3d vtx) {
      
      // XXX currently assumed vtx is instance of MFreeVertex3d
      // This will change once MFreeModel uses FemMeshComp equivalent
      if (vtx instanceof MFreeVertex3d) {
         MFreeVertex3d mvtx = (MFreeVertex3d)vtx;
         
         ArrayList<MFreeNode3d> nodes = mvtx.getDependentNodes ();
         VectorNd coords = mvtx.getNodeCoordinates ();
         
         for (int j=0; j<nodes.size (); j++) {
            mlist.add(new ContactMaster(nodes.get(j), coords.get(j)));
         }      
      } else {
         System.out.println("Unknown masters.");
      }
   }
   
   public boolean containsContactMaster (CollidableDynamicComponent comp) {
      return comp.getParent() == myNodes;      
   }   

   public boolean allowCollision (
      ContactPoint cpnt, Collidable other, Set<Vertex3d> attachedVertices) {
      if (CollisionHandler.attachedNearContact (
         cpnt, other, attachedVertices)) {
         return false;
      }
      return true;
   }

   public int getCollidableIndex() {
      return myCollidableIndex;
   }
   
   public void setCollidableIndex (int idx) {
      myCollidableIndex = idx;
   }
   
   @Override
   public void getMassMatrixValues (SparseBlockMatrix M, VectorNd f, double t) {
      int bi;

      for (int i=0; i<myNodes.size(); i++) {
         FemNode3d n = myNodes.get(i);
          if ((bi = n.getSolveIndex()) != -1) {
            n.getEffectiveMass (M.getBlock (bi, bi), t);
            n.getEffectiveMassForces (f, t, M.getBlockRowOffset (bi));
          }
      }
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
      for (int i=0; i<myNodes.size(); i++) {
         FemNode3d n = myNodes.get(i);
         int bk = n.getSolveIndex();
         if (bk != -1) {
            int idx = M.getBlockRowOffset (bk);
            if (idx < asize) {
               n.mulInverseEffectiveMass (
                  M.getBlock(bk, bk), abuf, fbuf, idx);
            }
         }
      }
   }

}
