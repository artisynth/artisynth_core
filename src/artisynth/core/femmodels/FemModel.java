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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.MaterialBase;
import artisynth.core.materials.MaterialChangeEvent;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.Constrainer;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.DynamicAttachmentComp;
import artisynth.core.mechmodels.DynamicAttachmentWorker;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.ForceEffector;
import artisynth.core.mechmodels.HasSlaveObjects;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointList;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelBase;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.PropertyChangeListener;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import maspack.geometry.AABBTree;
import maspack.geometry.BVTree;
import maspack.matrix.Matrix;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.properties.PropertyInfo.Edit;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.solvers.IterativeSolver.ToleranceType;
import maspack.spatialmotion.FrictionInfo;
import maspack.util.DataBuffer;
import maspack.util.DoubleInterval;
import maspack.util.FunctionTimer;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public abstract class FemModel extends MechSystemBase
   implements TransformableGeometry, ScalableUnits, Constrainer, 
   ForceEffector, PropertyChangeListener, HasSlaveObjects {

   protected static int NEEDS_STRESS = 0x01;
   protected static int NEEDS_STRAIN = 0x02;
   protected static int NEEDS_ENERGY = 0x04;

   /* Flags to control computation of stress, strain and strain energy
    * density */
   protected static int COMPUTE_STRESS_EXPLICIT = 0x0001;
   protected static int COMPUTE_STRESS_RENDER = 0x0002;
   protected static int COMPUTE_STRESS_ANY =
      (COMPUTE_STRESS_RENDER | COMPUTE_STRESS_EXPLICIT);

   protected static int COMPUTE_STRAIN_EXPLICIT = 0x0004;
   protected static int COMPUTE_STRAIN_RENDER = 0x0008;
   protected static int COMPUTE_STRAIN_ANY =
      (COMPUTE_STRAIN_RENDER | COMPUTE_STRAIN_EXPLICIT);

   protected static int COMPUTE_ENERGY_EXPLICIT = 0x0010;
   protected static int COMPUTE_ENERGY_RENDER = 0x0020;
   protected static int COMPUTE_ENERGY_ANY =
      (COMPUTE_ENERGY_RENDER | COMPUTE_ENERGY_EXPLICIT);

   protected static int COMPUTE_ANY_RENDER = 
      (COMPUTE_STRESS_RENDER | COMPUTE_STRAIN_RENDER | COMPUTE_ENERGY_RENDER);

   /**
    * Interface that determines if an element is valid
    */
   public interface ElementFilter {
      public boolean elementIsValid (FemElement e);
   }

   protected PointList<FemMarker> myMarkers;
   protected ComponentList<DynamicAttachmentComp> myAttachments;

   /**
    * Specifies a scalar stress/strain measure.
    */
   public enum StressStrainMeasure {
      /**
       * von Mises stress, as defined in
       * https://en.wikipedia.org/wiki/Von_Mises_yield_criterion.
       * It is equal to sqrt(3 J2), where J2 is the second invariant of 
       * the average deviatoric stress.
       */
      VonMisesStress,

      /**
       * von Mises strain, as defined in
       * http://www.continuummechanics.org/vonmisesstress.html,
       * which is equivalent to 
       * https://dianafea.com/manuals/d944/Analys/node405.html.
       * It is equal to sqrt(4/3 J2), where J2 is
       * the second invariant of the average deviatoric strain.
       */
      VonMisesStrain,

      /**
       * Absolute value of the maximum principle stress.
       */
      MAPStress,

      /**
       * Absolute value of the maximum principle strain.
       */
      MAPStrain,

      /**
       * Maximum shear stress, given by
       * <pre>
       * max (|s0-s1|/2, |s1-s2|/2, |s2-s0|/2)
       * </pre>
       * where {@code s0}, {@code s1}, and {@code s2} are the eigenvalues of
       * the stress tensor.
       */
      MaxShearStress,

      /**
       * Maximum shear strain, given by
       * <pre>
       * max (|s0-s1|/2, |s1-s2|/2, |s2-s0|/2)
       * </pre>
       * where {@code s0}, {@code s1}, and {@code s2} are the eigenvalues of
       * the strain tensor.
       */
      MaxShearStrain,

      /**
       * Render as a color map showing the strain energy density.
       */
      EnergyDensity;

      public boolean usesStress() {
         return (
            this==VonMisesStress || this==MAPStress || this==MaxShearStress);
      }

      public boolean usesStrain() {
         return (
            this==VonMisesStrain || this==MAPStrain || this==MaxShearStrain);
      }
      
      public boolean usesEnergy() {
         return this == EnergyDensity;
      }
   };

   /**
    * Specifies how FEM surface meshes should be rendered.
    */
   public enum SurfaceRender {
      
      /**
       * No surface rendering
       */
      None(null),

      /**
       * Rendered as a shaded surface, using the model's face rendering
       * proporties.
       */
      Shaded(null),
      // Fine,

      /**
       * Render as a color map showing the von Mises stress.
       */
      Stress(StressStrainMeasure.VonMisesStress),

      /**
       * Render as a color map showing the von Mises strain.
       */
      Strain(StressStrainMeasure.VonMisesStrain),

      /**
       * Render as a color map showing the maximum absolute principal stress
       * component.
       */
      MAPStress(StressStrainMeasure.MAPStress),

      /**
       * Render as a color map showing the maximum absolute principal strain
       * component.
       */
      MAPStrain(StressStrainMeasure.MAPStrain),

      /**
       * Render as a color map showing the maximum shear stress.
       */
      MaxShearStress(StressStrainMeasure.MaxShearStress),

      /**
       * Render as a color map showing the maximum shear strain.
       */
      MaxShearStrain(StressStrainMeasure.MaxShearStrain),

      /**
       * Render as a color map showing the strain energy density.
       */
      EnergyDensity(StressStrainMeasure.EnergyDensity);

      StressStrainMeasure myStressStrain;

      SurfaceRender (StressStrainMeasure stressStrain) {
         myStressStrain = stressStrain;
      }

      public boolean usesStressOrStrain() {
         return myStressStrain != null;
      }

      public StressStrainMeasure getStressStrainMeasure() {
         return myStressStrain;
      }
   };

   public enum Ranging {
      Fixed,
      Auto
   };

   public enum IncompMethod {
      OFF,
      ON,
      AUTO,
      NODAL,
      ELEMENT,
      FULL
   };
   protected static SurfaceRender DEFAULT_SURFACE_RENDERING =
      SurfaceRender.None;
   protected SurfaceRender mySurfaceRendering = DEFAULT_SURFACE_RENDERING;
   protected Ranging myStressPlotRanging = Ranging.Auto;
   protected DoubleInterval myStressPlotRange = 
       new DoubleInterval(DEFAULT_STRESS_PLOT_RANGE);
   protected PropertyMode mySurfaceRenderingMode = PropertyMode.Inherited;
   protected PropertyMode myStressPlotRangingMode = PropertyMode.Inherited;
   protected PropertyMode myStressPlotRangeMode = PropertyMode.Inherited;

   protected boolean myWarpingP = true;
   protected FunctionTimer steptime = new FunctionTimer();
   protected Point3d myMinBound = null;
   protected Point3d myMaxBound = null;

   protected SparseNumberedBlockMatrix mySolveMatrix = null;
   protected boolean mySolveMatrixSymmetricP = true;

   protected boolean myStressesValidP = false;
   protected boolean myStiffnessesValidP = false;

   protected double myVolume = 0;
   protected boolean myVolumeValid = false;
   protected double myRestVolume = 0;
   protected boolean myRestVolumeValid = false;
   protected double myStrainEnergy;

   protected double myCharacteristicSize = -1;

   protected FemMaterial myMaterial = null;

   // flag to indicate that forces need updating. Since forces are always
   // updated before a call to advance() (within setDefaultInputs()), this
   // flag only needs to be set during the advance routine --
   // specifically, within the setState methods
   protected boolean myForcesNeedUpdating = false;

   protected static Vector3d DEFAULT_GRAVITY = new Vector3d (0, 0, -9.8);;
   protected static double DEFAULT_DENSITY = 1000;
   protected static boolean DEFAULT_WARPING = true;

   protected static double DEFAULT_STIFFNESS_DAMPING = 0;
   protected static double DEFAULT_MASS_DAMPING = 2;
   protected static Ranging DEFAULT_STRESS_PLOT_RANGING = Ranging.Auto;
   protected static DoubleInterval DEFAULT_STRESS_PLOT_RANGE = 
      new DoubleInterval(0,0);

   protected static boolean DEFAULT_COMPUTE_NODAL_STRAIN = false;
   protected static boolean DEFAULT_COMPUTE_NODAL_STRESS = false;
   protected static boolean DEFAULT_COMPUTE_NODAL_ENERGY = false;

   protected int myComputeStressStrainFlags = 0;

   protected static boolean DEFAULT_COMPUTE_STRAIN_ENERGY = false;
   protected boolean myComputeStrainEnergy = DEFAULT_COMPUTE_STRAIN_ENERGY;

   protected double myDensity;
   PropertyMode myDensityMode = PropertyMode.Inherited;

   protected Vector3d myGravity = new Vector3d(DEFAULT_GRAVITY);
   protected PropertyMode myGravityMode = PropertyMode.Inherited;

   protected double myMassDamping;
   protected double myStiffnessDamping;

   protected int myNumInverted = 0;

   protected double myMaxTranslationalVel = 1e10;

   protected AABBTree myAABBTree;
   protected boolean myBVTreeValid;

   public static PropertyList myProps =
      new PropertyList (FemModel.class, MechSystemBase.class);

   static {
      // modify some MechSystemBase properties so they don't appear in property
      // panels
      myProps.get("navpanelVisibility").setEditing(Edit.Never);
      myProps.get("navpanelDisplay").setEditing(Edit.Never);
      myProps.get("dynamicsEnabled").setEditing(Edit.Never);
      myProps.get("penetrationLimit").setEditing(Edit.Never);
      myProps.get("updateForcesAtStepEnd").setEditing(Edit.Never);
      myProps.get("profiling").setEditing(Edit.Never);
      myProps.get("integrator").setEditing(Edit.Never);
      myProps.get("matrixSolver").setEditing(Edit.Never);
      myProps.get("useImplicitFriction").setEditing(Edit.Never);

      // surfaceRendering must be written out explicitly at the end,
      // so it is scanned at the end
      myProps.addInheritable (
         "surfaceRendering:Inherited", "surface rendering mode",
         DEFAULT_SURFACE_RENDERING, "NW");
      myProps.addInheritable ("stressPlotRanging:Inherited", 
         "ranging mode for stress plots",
         DEFAULT_STRESS_PLOT_RANGING);         
      myProps.addInheritable (
         "stressPlotRange:Inherited", 
         "stress value range for color stress plots",
         DEFAULT_STRESS_PLOT_RANGE, "NW");
      myProps.addInheritable (
         "gravity:Inherited", "acceleration of gravity", DEFAULT_GRAVITY);
      myProps.addInheritable ("density:Inherited", "density", DEFAULT_DENSITY);
      myProps.add (
         "particleDamping * *", "damping on each particle",
         DEFAULT_MASS_DAMPING);
      myProps.add (
         "stiffnessDamping * *", "damping on stiffness matrix",
         DEFAULT_STIFFNESS_DAMPING);
      myProps.add (
         "material", "model material parameters", createDefaultMaterial(), "CE");
      myProps.add (
         "computeNodalStress",
         "controls whether nodal stresses are computed",
         DEFAULT_COMPUTE_NODAL_STRESS);
      myProps.add (
         "computeNodalStrain",
         "controls whether nodal strains are computed",
         DEFAULT_COMPUTE_NODAL_STRAIN);
      myProps.add (
         "computeNodalEnergyDensity",
         "controls whether nodal strain energy densities are computed",
         DEFAULT_COMPUTE_NODAL_ENERGY);
      myProps.add (
         "computeStrainEnergy",
         "controls whether strain energy is computed",
         DEFAULT_COMPUTE_STRAIN_ENERGY);
      myProps.addReadOnly (
         "strainEnergy",
         "strain energy, only computed if 'computeStrainEnergy' is true");
      myProps.addReadOnly ("kineticEnergy", "kinetic energy of the model");
      myProps.addReadOnly ("volume *", "volume of the model");
      myProps.addReadOnly ("numInverted", "number of inverted elements");
      myProps.addReadOnly ("mass *", "mass of the model");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      myGravity = new Vector3d (DEFAULT_GRAVITY);
      myGravityMode = PropertyMode.Inherited;
      myDensity = DEFAULT_DENSITY;
      myDensityMode = PropertyMode.Inherited;
      mySurfaceRendering = DEFAULT_SURFACE_RENDERING;
      mySurfaceRenderingMode = PropertyMode.Inherited;
      myStressPlotRanging = DEFAULT_STRESS_PLOT_RANGING;
      myStressPlotRangingMode = PropertyMode.Inherited;
      myStressPlotRange = new DoubleInterval(DEFAULT_STRESS_PLOT_RANGE);
      myStressPlotRangeMode = PropertyMode.Inherited;
      myWarpingP = DEFAULT_WARPING;
      myStiffnessDamping = DEFAULT_STIFFNESS_DAMPING;
      myMassDamping = DEFAULT_MASS_DAMPING;
      setMaxStepSize (0.01);
      setMaterial (createDefaultMaterial());
   }

   protected void initializeChildComponents() {
      myMarkers = new PointList<FemMarker> (FemMarker.class, "markers", "m");
      addFixed (myMarkers);
      myAttachments =
         new ComponentList<DynamicAttachmentComp> (
            DynamicAttachmentComp.class, "attachments", "a");

      addFixed (myAttachments);
   }

   public FemModel (String name) {
      super (name);

      initializeChildComponents();
      
      setRenderProps (createRenderProps());
      myMaxStepSize = ModelBase.getDefaultMaxStepSize();
      setImplicitIterations (20);
      setImplicitPrecision (0.01);
      setDefaultValues();
   }

   public static FemMaterial createDefaultMaterial() {
      return new LinearMaterial();
   }

   public FemMaterial getMaterial() {
      return myMaterial;
   }

   protected void notifyElementsOfMaterialStateChange () {
      // clear states for any elements that use the model material
      for (FemElement e : getAllElements()) {
         if (e.getMaterial() == null) {
            e.notifyStateVersionChanged();
         }
      }
   }

   public <T extends FemMaterial> void setMaterial (T mat) {
      if (mat == null) {
         throw new IllegalArgumentException (
            "Material not allowed to be null");
      }
      FemMaterial oldMat = myMaterial;
      T newMat = MaterialBase.updateMaterial (
         this, "material", myMaterial, mat);
      myMaterial = newMat;
      // issue change event in case solve matrix symmetry or state has changed:
      MaterialChangeEvent mce = 
         MaterialBase.symmetryOrStateChanged ("material", newMat, oldMat);
      if (mce != null) {
         if (mce.stateChanged()) {
            notifyElementsOfMaterialStateChange(); 
         }
         componentChanged (mce);
      }      
      invalidateStressAndStiffness();
      if (!isScanning()) {
         // invalidate cached warping data, but not if we are scanning since
         // that may cause node directors to be updated prematurely
         invalidateRestData();  
      }
   }
   
   public synchronized void setLinearMaterial (
      double E, double nu, boolean corotated) {
      setMaterial (new LinearMaterial (E, nu, corotated));
   }

   // surface rendering

   public synchronized void setSurfaceRendering (SurfaceRender rendering) {
       SurfaceRender prev = mySurfaceRendering;
      if (mySurfaceRendering != rendering) {
         mySurfaceRendering = rendering;
      }
      mySurfaceRenderingMode =
         PropertyUtils.propagateValue (
            this, "surfaceRendering", rendering, mySurfaceRenderingMode);
      
      if (mySurfaceRendering != prev) {
         if (myStressPlotRanging == Ranging.Auto) {
            resetAutoStressPlotRange();
         }
      }
   }

   public SurfaceRender getSurfaceRendering() {
      return mySurfaceRendering;
   }

   public void setSurfaceRenderingMode(PropertyMode mode) {
      if (mode != mySurfaceRenderingMode) {
         mySurfaceRenderingMode = PropertyUtils.setModeAndUpdate (
            this, "surfaceRendering", mySurfaceRenderingMode, mode);
      }
   }
   
   public PropertyMode getSurfaceRenderingMode() {
      return mySurfaceRenderingMode;
   }

   // stress plot ranging 

   public void setStressPlotRanging (Ranging ranging) {
      if (myStressPlotRanging != ranging) {
         myStressPlotRanging = ranging;
         if (ranging == Ranging.Auto) {
            resetAutoStressPlotRange();
         }
      }
      myStressPlotRangingMode =
         PropertyUtils.propagateValue (
            this, "stressPlotRanging", ranging, myStressPlotRangingMode);
   }

   public Ranging getStressPlotRanging (){
      return myStressPlotRanging;
   }
   
   public void setStressPlotRangingMode(PropertyMode mode) {
      Ranging prevRanging = myStressPlotRanging;
      if (mode != myStressPlotRangingMode) {
         myStressPlotRangingMode = PropertyUtils.setModeAndUpdate (
            this, "stressPlotRanging", myStressPlotRangingMode, mode);
      }
      if (myStressPlotRanging != prevRanging) {
         if (myStressPlotRanging == Ranging.Auto) {
            resetAutoStressPlotRange();
         }
      }
   }
   
   public PropertyMode getStressPlotRangingMode() {
      return myStressPlotRangingMode;
   }

   // stress plot range

   /**
    * Updates stress and strain rendering flags, and returns a bit mask
    * of any flags that were newly set.
    */
   protected abstract int updateStressStrainRenderFlags();

   /**
    * Special method to propogate stress plot range values only to meshes and
    * cutplanes with the same surface rendering, stressPlotRanging == Fixed,
    * and stressPlotRangeMode == Inherited.
    */
   protected abstract void propagateFixedStressPlotRange();
   protected abstract void updatePlotRangeIfAuto();
   
   public void setStressPlotRange (DoubleInterval range) {
      myStressPlotRange = new DoubleInterval (range);
      if (myStressPlotRangeMode != PropertyMode.Inactive) {
         myStressPlotRangeMode = PropertyMode.Explicit;
         propagateFixedStressPlotRange();
      }
   }

   public void resetAutoStressPlotRange () {
      if (mySurfaceRendering.usesStressOrStrain() && !isScanning()) {
         myStressPlotRange.set (0, 0);
         updatePlotRangeIfAuto();
      }
   }

   public DoubleInterval getStressPlotRange (){
      return new DoubleInterval (myStressPlotRange);
   }
   
   public void setStressPlotRangeMode(PropertyMode mode) {
      if (mode != myStressPlotRangeMode) {
         if (mode == PropertyMode.Explicit) {
            propagateFixedStressPlotRange();
         }
         else if (myStressPlotRangeMode == PropertyMode.Explicit) {
            // do nothing
         }
         myStressPlotRangeMode = mode;
      }
   }

   public PropertyMode getStressPlotRangeMode() {
      return myStressPlotRangeMode;
   }
   
   // end stress plot range
   
   public void invalidateStressAndStiffness() {
      myStressesValidP = false;
      myStiffnessesValidP = false;
   }
   
   public void invalidateRestData() {
      // getAllElements() can be null if called early during 
      // FemModel3d initialization
      if (getAllElements() != null) {
         for (FemElement e : getAllElements()) {
            e.invalidateRestData();
         }
      }
      myRestVolumeValid = false;
   }
   
   protected void invalidateStressAndMaybeStiffness() {
      myStressesValidP = false;
      myStiffnessesValidP = false;
   }

   /* ------- gravity stuff ---------- */

   public Vector3d getGravity() {
      return new Vector3d(myGravity);
   }

   public void setGravity (Vector3d g) {
      myGravity.set (g);
      myGravityMode = PropertyUtils.propagateValue (
         this, "gravity", g, myGravityMode);
   }

   public void setGravity (double gx, double gy, double gz) {
      setGravity (new Vector3d (gx, gy, gz));
   }

   public PropertyMode getGravityMode() {
      return myGravityMode;
   }

   public void setGravityMode (PropertyMode mode) {
      myGravityMode =
         PropertyUtils.setModeAndUpdate (
            this, "gravity", myGravityMode, mode);
   }

   /* -------- damping stuff --------- */

   public void setParticleDamping (double d) {
      myMassDamping = d;
   }

   public double getParticleDamping() {
      return myMassDamping;
   }

   /**
    * Sets the Rayleigh damping coefficient associated with the FEM's mass
    *
    * @param d new mass damping
    */
   public void setMassDamping(double d) {
      setParticleDamping(d);
   }
   
   /**
    * Gets the Rayleigh damping coefficient associated with the FEM's mass
    *
    * @return mass damping value
    */
   public double getMassDamping() {
      return getParticleDamping();
   }
   
   /**
    * Sets the Rayleigh damping coefficient associated with the FEM's stiffness
    *
    * @param d new stiffness damping
    */
   public void setStiffnessDamping (double d) {
      myStiffnessDamping = d;
   }

   /**
    * Gets the Rayleigh damping coefficient associated with the FEM's stiffness
    *
    * @return stiffness damping value
    */
   public double getStiffnessDamping() {
      return myStiffnessDamping;
   }

   /**
    * Creates myAABBTree if it is null, or otherwise updates it.
    */
   protected abstract void updateBVHierarchies();

   protected BVTree getBVTree() {
      if (myAABBTree == null || !myBVTreeValid) {
         updateBVHierarchies();
      }
      return myAABBTree;
   }
   
   protected abstract void updateStressAndStiffness();

   public synchronized void setDensity (double p) {
      myDensity = p;
      myDensityMode =
         PropertyUtils.propagateValue (
            this, "density", myDensity, myDensityMode);
      invalidateStressAndStiffness();
   }

   public double getDensity() {
      return myDensity;
   }

   public void setDensityMode (PropertyMode mode) {
      myDensityMode =
         PropertyUtils.setModeAndUpdate (this, "density", myDensityMode, mode);
   }

   public PropertyMode getDensityMode() {
      return myDensityMode;
   }

   public int numNodes() {
      return getNodes().size();
   }

   public abstract PointList<? extends FemNode> getNodes();

   public abstract FemNode getNode (int idx);

   public abstract FemNode getNodeByNumber (int num);

   public abstract ArrayList<? extends FemElement> getAllElements();

   public abstract int numAllElements();

   public void addMarker (FemMarker mkr) {
      myMarkers.add (mkr);
   }
   
   public void addMarker (FemMarker mkr, FemElement elem) {
      if (!ModelComponentBase.recursivelyContains (this, elem)) {
         throw new IllegalArgumentException (
            "element not contained within model");
      }
      mkr.setFromElement (elem);
      myMarkers.add (mkr);
   }
   
   public void addMarker (FemMarker mkr, FemElement elem, int markerId) {
      if (!ModelComponentBase.recursivelyContains (this, elem)) {
         throw new IllegalArgumentException (
            "element not contained within model");
      }
      mkr.setFromElement (elem);
      myMarkers.addNumbered (mkr, markerId);
   }

   public void addMarker (
      FemMarker mkr,
      Collection<? extends FemNode> nodes, VectorNd weights) {
      mkr.setFromNodes (nodes, weights);
      myMarkers.add (mkr);
   }

   public void addMarker (FemMarker mkr, FemNode[] nodes, double[] weights) {
      mkr.setFromNodes (nodes, weights);
      myMarkers.add (mkr);
   }

   public boolean addMarker (FemMarker mkr, Collection<? extends FemNode> nodes) {
      boolean status = mkr.setFromNodes (nodes);
      myMarkers.add (mkr);
      return status;
   }

   public boolean addMarker (FemMarker mkr, FemNode[] nodes) {
      boolean status = mkr.setFromNodes (nodes);
      myMarkers.add (mkr);
      return status;
   }

   public boolean removeMarker (FemMarker mkr) {
      if (myMarkers.remove (mkr)) {
         return true;
      }
      else {
         return false;
      }
   }

   public RenderableComponentList<FemMarker> markers() {
      return myMarkers;
   }

   public ComponentList<DynamicAttachmentComp> attachments() {
      return myAttachments;
   }

   public void attachPoint (Point p, FemNode[] nodes, double[] coords) {
      if (p.isAttached()) {
         throw new IllegalArgumentException ("point is already attached");
      }
      if (!ModelComponentBase.recursivelyContains (this, p)) {
         throw new IllegalArgumentException ("point not contained within model");
      }
      for (FemNode n : nodes) {
         if (!ModelComponentBase.recursivelyContains (this, n)) {
            throw new IllegalArgumentException (
               "node not contained within model");
         }
      }
      PointFem3dAttachment ax = new PointFem3dAttachment (p);
      ax.setFromNodes (nodes, coords);
      if (DynamicAttachmentWorker.containsLoop (ax, p, null)) {
         throw new IllegalArgumentException (
            "attachment contains loop");
      }
      addAttachment (ax);      
   }

   public boolean detachPoint (Point p) {
      DynamicAttachment a = p.getAttachment();
      if (a instanceof DynamicAttachmentComp) {
         removeAttachment ((DynamicAttachmentComp)a);
         return true;
      }
      else {
         return false;
      }
   }

   void addAttachment (DynamicAttachmentComp ax) {
      ax.updatePosStates();
      myAttachments.add (ax);
   }

   void removeAttachment (DynamicAttachmentComp ax) {
      myAttachments.remove (ax);
   }

   public void detachAllNodes() {
      myAttachments.removeAll();
   }

   protected void doclear() {
      myMarkers.clear();
      myAttachments.clear();
   }

   public void clear() {
      doclear();
      notifyStructureChanged (this);
   }

   protected void handleComponentChanged (ComponentChangeEvent e) {
      if (e.getCode() == Code.STRUCTURE_CHANGED) {
         clearCachedData (e);
      }
      else if (e.getCode() == Code.DYNAMIC_ACTIVITY_CHANGED) { 
         clearCachedData (e);
      }
      else if (e instanceof MaterialChangeEvent) {
         clearCachedData (e);
         // presumable only need to invalidate rest data if event is 
         // associated with a linear material, but we do this anyway
         invalidateRestData();
      }
   }

   public void componentChanged(ComponentChangeEvent e) {
      handleComponentChanged (e);
      notifyParentOfChange(e);
   }

   /* ----- I/O methods ------ */

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (ScanWriteUtils.scanAndStorePropertyValue (
             rtok, this, "surfaceRendering", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "volume")) {
         myVolume = rtok.scanNumber();
         myVolumeValid = true;
         return true;         
      }
      else if (scanAttributeName (rtok, "computeStressStrainFlags")) {
         myComputeStressStrainFlags = rtok.scanInteger();
         return true;
      }
      else if (scanAttributeName (rtok, "strainEnergy")) {
         myStrainEnergy = rtok.scanNumber();
         return true;
      }
      else if (scanAttributeName (rtok, "stressPlotRange")) {
         myStressPlotRange.scan (rtok, null);
         return true;
      }
      else if (scanAttributeName (rtok, "stressPlotRangeMode")) {
         myStressPlotRangeMode = rtok.scanEnum(PropertyMode.class);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor); 
      // surfaceRendering has to be written near the end because it has to be
      // scanned after the model and mesh structures.
      pw.println ("volume=" + fmt.format(updateVolume()));
      pw.println ("strainEnergy=" + myStrainEnergy);
      pw.println (
         "computeStressStrainFlags=0x" +
         Integer.toHexString(myComputeStressStrainFlags));
      myProps.get("surfaceRendering").writeIfNonDefault (
         this, pw, fmt, ancestor);
      // need to write stressPlotRange explicitly because Auto ranging will
      // change its value regardless of stressPlotRangeMode.
      if (!myStressPlotRange.equals(DEFAULT_STRESS_PLOT_RANGE)) {
         pw.print ("stressPlotRange=");
         myStressPlotRange.write (pw, fmt, null);
      }
      if (myStressPlotRangeMode != PropertyMode.Inherited) {
         pw.println ("stressPlotRangeMode=" + myStressPlotRangeMode);
      }
   }

   /* ----- Aux state methods ------ */

   public void getState(DataBuffer data) {
      data.dput (updateVolume());
      data.dput (myStrainEnergy);
   }
   
   public void setState(DataBuffer data) {
      myVolume = data.dget();
      myVolumeValid = true;
      myStrainEnergy = data.dget();
   }

   public void setBounds (Point3d pmin, Point3d pmax) {
      if (myMinBound == null) {
         myMinBound = new Point3d();
      }
      if (myMaxBound == null) {
         myMaxBound = new Point3d();
      }
      myMinBound.set (pmin);
      myMaxBound.set (pmax);
   }

   public void zeroExternalForces() {
      for (int i = 0; i < numNodes(); i++) {
         getNode (i).zeroExternalForces();
      }
   }

   public boolean forcesNeedUpdating() {
      return myForcesNeedUpdating;
   }

   protected abstract void updateNodeForces (double t);

   public void applyForces (double t) {
      updateNodeForces (t);
      myForcesNeedUpdating = false;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      if (myMinBound != null) {
         myMinBound.updateBounds (pmin, pmax);
      }
      if (myMaxBound != null) {
         myMaxBound.updateBounds (pmin, pmax);
      }
      for (int i = 0; i < numNodes(); i++) {
         getNode (i).updateBounds (pmin, pmax);
      }
   }

   public void render (Renderer renderer, int flags) {
   }

   protected void updateLocalAttachmentPos() {
      for (FemMarker mkr : myMarkers) {
         mkr.updatePosState();
      }
      for (DynamicAttachment a : myAttachments) {
         a.updatePosStates();
      }
   }

   public void getAttachments (List<DynamicAttachment> list, int level) {
      list.addAll (myAttachments);
      for (FemMarker mkr : myMarkers) {
         if (mkr.getAttachment() == null) {
            System.out.println ("NULL attachment for marker "+mkr.getNumber());
         }
         list.add (mkr.getAttachment());
      }
   }         

   /**
    * {@inheritDoc}
    */
   @Override
      public boolean hasState() {
      return true;
   }

   public int getImplicitIterations() {
      return mySolver.getMaxIterations();
   }

   public void setImplicitIterations (int max) {
      mySolver.setMaxIterations (max);
   }

   public ToleranceType getToleranceType() {
      return mySolver.getToleranceType();
   }

   public void setToleranceType (ToleranceType type) {
      mySolver.setToleranceType (type);
   }

   public void scaleDistance (double s) {
      getNodes().scaleDistance (s);
      myMaterial.scaleDistance(s);
      
      if (myGravityMode == PropertyMode.Explicit ||
          topMechSystem(this) == this) { // XXX hack, calling topMechSystem
         myGravity.scale (s);
      }

      myDensity /= (s * s * s);
      for (FemElement e : getAllElements()) {
         e.scaleDistance (s);
      }
      invalidateStressAndStiffness();
      updateLocalAttachmentPos();
      if (myMinBound != null) {
         myMinBound.scale (s);
      }
      if (myMaxBound != null) {
         myMaxBound.scale (s);
      }
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
      myVolumeValid = false;
   }

   public double getMass() {
      double mass = 0;
      for (FemElement e : getAllElements()) {
         mass += e.getMass();
      }
      return mass;
   }

   public double getNodeMass() {
      double mass = 0;
      for (FemNode n : getNodes()) {
         mass += n.getMass();
      }
      return mass;
   }

   public void printNodeMasses(int num) {
      for (int i=0; i<num&&i<getNodes().size(); i++) {
         FemNode n = getNodes().get(i);
         System.out.println (" "+i+" "+n.getMass());
      }
   }

   /**
    * @deprecated Use {@link #getKineticEnergy} instead.
    */
   public double getEnergy() {
      return getKineticEnergy();
   }

   /**
    * Queries the current kinetic energy in this FEM model, assuming lumped
    * nodal masses.
    */
   public double getKineticEnergy() {
      double e = 0;
      for (FemNode n : getNodes()) {
         e += n.getMass()*n.getVelocity().normSquared() / 2;
      }
      return e;
   }

   /**
    * Returns the strain energy for this model. Strain energy is computed
    * only if {@link #getComputeStrainEnergy} returns {@code true}; otherwise,
    * zero is returned.
    * 
    * @return strain energy for this model
    */
   public double getStrainEnergy() {
      return myStrainEnergy;
   }


   /**
    * Queries whether nodal stresses are computed for this model.
    *
    * @return {@code true} if nodal stresses are computed
    */
   public boolean getComputeNodalStress () {
      return (myComputeStressStrainFlags & COMPUTE_STRESS_EXPLICIT) != 0;
   }

   /**
    * Sets whether nodal stresses are computed for this model. The default
    * value is {@code false} to avoid unnecessary computation.
    *
    * @param enable if {@code true}, enables nodal stress computation
    */
   public void setComputeNodalStress (boolean enable) {
      boolean oldenable = getComputeNodalStress();
      if (enable) {
         myComputeStressStrainFlags |= COMPUTE_STRESS_EXPLICIT;
      }
      else {
         myComputeStressStrainFlags &= ~COMPUTE_STRESS_EXPLICIT;
      }
      if (!needsNodalStress()) {
         for (FemNode n : getNodes()) {
            n.maybeClearStress ();
         }
      }
      else if (enable && !oldenable) {
         updateStressAndStiffness();
      }      
   }

   /**
    * Queries whether nodal strains are computed for this model.
    *
    * @return {@code true} if nodal strains are computed
    */
   public boolean getComputeNodalStrain () {
      return (myComputeStressStrainFlags & COMPUTE_STRAIN_EXPLICIT) != 0;
   }

   /**
    * Sets whether nodal strains are computed for this model. The default
    * value is {@code false} to avoid unnecessary computation.
    *
    * @param enable if {@code true}, enables nodal strain computation
    */
   public void setComputeNodalStrain (boolean enable) {
      boolean oldenable = getComputeNodalStrain();
      if (enable) {
         myComputeStressStrainFlags |= COMPUTE_STRAIN_EXPLICIT;
      }
      else {
         myComputeStressStrainFlags &= ~COMPUTE_STRAIN_EXPLICIT;
      }
      if (!needsNodalStrain()) {
         for (FemNode n : getNodes()) {
            n.maybeClearStrain ();
         }
      }
      else if (enable && !oldenable) {
         updateStressAndStiffness();
      }      
   }

   /**
    * Queries whether nodal strain energy densities are computed for this
    * model.
    *
    * @return {@code true} if nodal energy densities are computed
    */
   public boolean getComputeNodalEnergyDensity () {
      return (myComputeStressStrainFlags & COMPUTE_ENERGY_EXPLICIT) != 0;
   }

   /**
    * Sets whether nodal strain energy densities are computed for this
    * model. The default value is {@code false} to avoid unnecessary
    * computation.
    *
    * @param enable if {@code true}, enables nodal energy density computation
    */
   public void setComputeNodalEnergyDensity (boolean enable) {
      boolean oldenable = getComputeNodalEnergyDensity();
      if (enable) {
         myComputeStressStrainFlags |= COMPUTE_ENERGY_EXPLICIT;
      }
      else {
         myComputeStressStrainFlags &= ~COMPUTE_ENERGY_EXPLICIT;
      }
      if (!needsNodalEnergy()) {
         for (FemNode n : getNodes()) {
            n.maybeZeroEnergy ();
         }
      }
      else if (enable && !oldenable) {
         updateStressAndStiffness();
      }
   }

   /**
    * Internal method indicating whether nodal stresses need to be computed
    * because of either an explicit application request or for mesh rendering.
    */
   public boolean needsNodalStress() {
      return (myComputeStressStrainFlags & COMPUTE_STRESS_ANY) != 0;
   }

   /**
    * Internal method indicating whether nodal strains need to be computed
    * because of either an explicit application request or for mesh rendering.
    */
   protected boolean needsNodalStrain() {
      return (myComputeStressStrainFlags & COMPUTE_STRAIN_ANY) != 0;
   }

   /**
    * Internal method indicating whether nodal energies need to be computed
    * because of either an explicit application request or for mesh rendering.
    */
   protected boolean needsNodalEnergy() {
      return (myComputeStrainEnergy ||
              (myComputeStressStrainFlags & COMPUTE_ENERGY_ANY) != 0);
   }

   /**
    * Explicitly enables the collection of nodal stress, strain or energy
    * density values for this model, as required for the specified
    * stress/strain measure.
    *
    * @param m measure indicating whether stress, strain or energy density
    * values are needed
    */
   public void setComputeNodalStressStrain (StressStrainMeasure m) {
      if (m.usesStress()) {
         setComputeNodalStress (true);
      }
      if (m.usesStrain()) {
         setComputeNodalStress (true);
      }
      if (m.usesEnergy()) {
         setComputeNodalEnergyDensity (true);
      }
   }
   
   /**
    * Cancels the computation of nodal stress, strain and energy density values
    * for this model. Values may still be computed on a per-node basis if
    * required for colormap rendering.
    */
   public void clearComputeNodalStressStrain () {
      setComputeNodalStress (false);
      setComputeNodalStress (false);
      setComputeNodalEnergyDensity (false);
   }

   /**
    * Queries whether strain energy is computed for this model.
    *
    * @return {@code true} if strain energy is computed
    */
   public boolean getComputeStrainEnergy () {
      return myComputeStrainEnergy;
   }

   /**
    * Sets whether strain energy is computed for this model. The default value
    * is {@code false} to avoid unnecessary computation.
    *
    * @param enable if {@code true}, enables strain energy computation
    */
   public void setComputeStrainEnergy (boolean enable) {
      boolean oldenable = myComputeStrainEnergy;
      myComputeStrainEnergy = enable;
      if (!enable) {
         myStrainEnergy = 0;
      }
      if (!needsNodalEnergy()) {
         for (FemNode n : getNodes()) {
            n.maybeZeroEnergy ();
         }
      }
      else if (enable && !oldenable) {
         updateStressAndStiffness();
      }         
   }

   public void scaleMass (double s) {
      for (int i = 0; i < numNodes(); i++) {
         FemNode n = getNode (i);
         n.scaleMass (s);
      }
      myDensity *= s;
      myMaterial.scaleMass (s);
      for (FemElement e : getAllElements()) {
         e.scaleMass (s);
      }
      invalidateStressAndStiffness();
   }

   public void getCollidables (List<Collidable> list) {
      if (this instanceof Collidable) {
         list.add ((Collidable)this);
      }
      recursivelyGetLocalComponents (this, list, Collidable.class);
   }      

   public void getDynamicComponents (
      List<DynamicComponent> active,
      List<DynamicComponent> attached, 
      List<DynamicComponent> parametric) {

      for (int i=0; i<numNodes(); i++) {
         FemNode n = getNodes().get(i);
         if (n.isActive()) {
            active.add (n);
         }
         else if (n.isAttached()) {
            attached.add (n);
         }
         else {
            parametric.add (n);
         }
      }
      for (int i=0; i<myMarkers.size(); i++) {
         attached.add (myMarkers.get(i));
      }
   }   
   
   public void getDynamicComponents (List<DynamicComponent> comps) {
      comps.addAll (getNodes());
      comps.addAll (myMarkers);
   }

   public void getConstrainers (List<Constrainer> constrainers, int level) {
      constrainers.add (this);
   }

   public void getForceEffectors (List<ForceEffector> forceEffectors, int level) {
      forceEffectors.add (this);
   }

   /**
    * {@inheritDoc}
    */
   public void getSlaveObjectComponents (List<HasSlaveObjects> comps, int level) {
      comps.add (this);
   }
   
   public void updateSlavePos() {
      myVolumeValid = false;
      myBVTreeValid = false;      
      invalidateStressAndMaybeStiffness();     
   }
   
   public void updateSlaveVel() {
   }

   /**
    * Sets the maximum step size by which this model should be advanced within a
    * simulation loop.
    * 
    * @param sec
    * maximum step size (seconds)
    */
   public void setMaxStepSize (double sec) {
      super.setMaxStepSize (sec);
   }

   public double updateVolume() {
      if (!myVolumeValid) {
         myVolume = computeVolume();
         myVolumeValid = true;
      }
      return myVolume;
   }
   
   protected double computeVolume() {
      double volume = 0;
      for (FemElement e : getAllElements()) {
         e.computeVolumes();
         volume += e.getVolume();
      }
      return volume;     
   }
   
   public double getRestVolume() {
      if (!myRestVolumeValid) {
         updateRestVolume();
      }
      return myRestVolume;
   }
   
   public double updateRestVolume() {
      double volume = 0;
      for (FemElement e : getAllElements()) {
         e.computeRestVolumes();
         volume += e.getRestVolume();
      }
      myRestVolume = volume;
      myRestVolumeValid = true;
      return volume;
   }
   
   public double getVolume() {
      // we don't update the volume in this method, since that might
      // result in FemElement.computeVolume() being called by a thread
      // different from the main simulation thread, which would in term
      // result in a collision with temporary variable use
      return myVolume;
   }

   public int getNumInverted() {
      return myNumInverted;
   }

   public double getImplicitPrecision() {
      return mySolver.getTolerance();
   }

   public void setImplicitPrecision (double prec) {
      mySolver.setTolerance (prec);
   }

   public void getBilateralSizes (VectorNi sizes) {
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {
      return numb;
   }

   public int getBilateralInfo (
      ConstraintInfo[] ginfo, int idx) {
      return idx;
   }

   public void getUnilateralSizes (VectorNi sizes) {
   }

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu) {
      return numu;
   }

   public int getUnilateralInfo (ConstraintInfo[] ninfo, int idx) {
      return idx;
   }

   public int maxFrictionConstraintSets() {
      return 0;
   }

   public int addFrictionConstraints (
      SparseBlockMatrix DT, ArrayList<FrictionInfo> finfo, 
      boolean prune, int numf) {
      return numf;
   }
   
   public int setBilateralForces (VectorNd lam, double s, int idx) {
      return idx;
   }
   
   public void zeroForces() {
   }

   public int getBilateralForces (VectorNd lam, int idx) {
      return idx;
   }

   public int setUnilateralForces (VectorNd the, double s, int idx) {
      return idx;
   }

   public int getUnilateralForces (VectorNd the, int idx) {
      return idx;
   }

   public int setUnilateralState (VectorNi state, int idx) {
      return idx;
   }

   public int getUnilateralState (VectorNi state, int idx) {
      return idx;
   }

   /**
    * {@inheritDoc}
    */
   public int setFrictionForces (VectorNd phi, double s, int idx) {
      return idx;
   }

   /**
    * {@inheritDoc}
    */
   public int getFrictionForces (VectorNd phi, int idx) {
      return idx;
   }
   
   public int setFrictionState (VectorNi state, int idx) {
      return idx;
   }

   public int getFrictionState (VectorNi state, int idx) {
      return idx;
   }

   public void recursivelyInitialize (double t, int level) {
      super.recursivelyInitialize (t, level);
   }

   protected void clearCachedData (ComponentChangeEvent e) {
      super.clearCachedData(e);
      myForcesNeedUpdating = true;
      invalidateStressAndStiffness();
   }

   public double getCharacteristicSize() {
      if (myCharacteristicSize == -1) {
         myCharacteristicSize = RenderableUtils.getRadius (this);
      }
      return myCharacteristicSize;
   }

   /**
    * {@inheritDoc}
    */
   public DynamicComponent checkVelocityStability() {
      PointList<? extends FemNode> nodes = getNodes();
      for (int i=0; i<nodes.size(); i++) {
         FemNode node = nodes.get(i);
         Vector3d vel = node.getVelocity();
         if (node.velocityLimitExceeded (myMaxTranslationalVel, 0)) {
            return node;
         }
      }
      return null;
   }
   
   public FemModel copy (int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FemModel fem = (FemModel)super.copy (flags, copyMap);

      for (int i=0; i<fem.numComponents(); i++) {
         System.out.println (" child " + fem.get(i).getName());
      }
      fem.initializeChildComponents();
      // need to set markers and attachments in the subclass

      fem.setSurfaceRenderingMode (mySurfaceRenderingMode);
      if (mySurfaceRenderingMode == PropertyMode.Explicit) {
         fem.setSurfaceRendering (mySurfaceRendering);
      }
      fem.setStressPlotRangingMode (myStressPlotRangingMode);
      if (myStressPlotRangingMode == PropertyMode.Explicit) {
         fem.setStressPlotRanging (myStressPlotRanging);
      }
      fem.setStressPlotRangeMode (myStressPlotRangeMode);
      if (myStressPlotRangeMode == PropertyMode.Explicit) {
         fem.setStressPlotRange (myStressPlotRange);
      }
      fem.setGravityMode (myGravityMode);
      if (myGravityMode == PropertyMode.Explicit) {
         fem.setGravity (myGravity);
      }
      fem.setDensityMode (myDensityMode);
      if (myDensityMode == PropertyMode.Explicit) {
         fem.setDensity (myDensity);
      }
      fem.setParticleDamping (myMassDamping);
      fem.setStiffnessDamping (myStiffnessDamping);

      fem.myMinBound = null;
      fem.myMaxBound = null;

      fem.setMaterial (myMaterial);

      fem.mySolveMatrix = null;

      fem.myStressesValidP = false;
      fem.myStiffnessesValidP = false;

      fem.steptime = new FunctionTimer();
      fem.myVolume = myVolume;
      fem.myVolumeValid = myVolumeValid;
      
      fem.myCharacteristicSize = myCharacteristicSize;
      
      fem.myForcesNeedUpdating = true;
      fem.myMaxTranslationalVel = myMaxTranslationalVel;

      fem.myComputeStressStrainFlags = myComputeStressStrainFlags;
      return fem;
   }

   public void propertyChanged (PropertyChangeEvent e) {
      if (e instanceof MaterialChangeEvent) {
         invalidateStressAndStiffness();
         if (e.getHost() instanceof FemMaterial && 
             ((FemMaterial)e.getHost()).isLinear()) {
            // invalidate rest data for linear materials, to rebuild
            // the initial warping stiffness matrices
            invalidateRestData();
         }
         MaterialChangeEvent mce = (MaterialChangeEvent)e;
         if (mce.stateChanged() && e.getHost() == getMaterial()) {
            notifyElementsOfMaterialStateChange();
         }
         if (mce.stateOrSymmetryChanged()) {
            notifyParentOfChange (new MaterialChangeEvent (this, mce));  
         }
      }
   }

   public double updateConstraints (double t, int flags) {
      return -1;
   }
   
   public void getConstrainedComponents (HashSet<DynamicComponent> comps) {
   }
   
   // method declarations to implement ForceEffector

   public int getJacobianType() {
      return Matrix.SYMMETRIC;
   }

   public abstract void addSolveBlocks (SparseNumberedBlockMatrix M);
}
