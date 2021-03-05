/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collection;
import java.util.Deque;
import java.io.PrintWriter;
import java.io.IOException;

import maspack.matrix.Matrix;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.geometry.AABBTree;
import maspack.geometry.BVTree;
import maspack.geometry.Boundable;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.Renderer;
import maspack.render.RenderableUtils;
import maspack.solvers.IterativeSolver.ToleranceType;
import maspack.util.DoubleInterval;
import maspack.util.FunctionTimer;
import maspack.util.StringHolder;
import maspack.util.ReaderTokenizer;
import maspack.util.NumberFormat;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.modelbase.PropertyChangeListener;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.util.ScanToken;
import artisynth.core.util.ScalableUnits;
import artisynth.core.materials.*;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.MaterialBase;
import artisynth.core.materials.MaterialChangeEvent;
import artisynth.core.mechmodels.Constrainer;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.DynamicAttachmentComp;
import artisynth.core.mechmodels.DynamicAttachmentWorker;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.ForceEffector;
import artisynth.core.mechmodels.HasSlaveObjects;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointList;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformableGeometry;

public abstract class FemModel extends MechSystemBase
   implements TransformableGeometry, ScalableUnits, Constrainer, 
   ForceEffector, PropertyChangeListener, HasSlaveObjects {

   /**
    * Interface that determines if an element is valid
    */
   public interface ElementFilter {
      public boolean elementIsValid (FemElement e);
   }

   protected PointList<FemMarker> myMarkers;
   protected ComponentList<DynamicAttachmentComp> myAttachments;

   /**
    * Specifies how FEM surface meshes should be rendered.
    */
   public enum SurfaceRender {
      /**
       * No surface rendering
       */
      None,

      /**
       * Rendered as a shaded surface, using the model's face rendering
       * proporties.
       */
      Shaded,
      // Fine,

      /**
       * Render as a color map showing the von Mises stress.
       */
      Stress,

      /**
       * Render as a color map showing the von Mises strain.
       */
      Strain,

      /**
       * Render as a color map showing the maximum absolute principal stress
       * component.
       */
      MAPStress,

      /**
       * Render as a color map showing the maximum absolute principal strain
       * component.
       */
      MAPStrain;

      public boolean usesStress() {
         return this == Stress || this == MAPStress;
      }

      public boolean usesStrain() {
         return this == Strain || this == MAPStrain;
      }
      
      public boolean usesStressOrStrain() {
         return this != None && this != Shaded;
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

   protected SurfaceRender mySurfaceRendering = SurfaceRender.None;
   protected Ranging myStressPlotRanging = Ranging.Auto;
   protected DoubleInterval myStressPlotRange = DEFAULT_STRESS_PLOT_RANGE;
   protected PropertyMode mySurfaceRenderingMode = PropertyMode.Inherited;
   protected PropertyMode myStressPlotRangingMode = PropertyMode.Inherited;
   protected PropertyMode myStressPlotRangeMode = PropertyMode.Inherited;

   protected boolean myWarpingP = true;
   protected FunctionTimer steptime = new FunctionTimer();
   protected Point3d myMinBound = null;
   protected Point3d myMaxBound = null;

   protected SparseNumberedBlockMatrix mySolveMatrix = null;
   protected boolean mySolveMatrixSymmetricP = true;
   //protected SparseBlockMatrix myMassMatrix = null;

   protected boolean myStressesValidP = false;
   protected boolean myStiffnessesValidP = false;

   protected double myVolume = 0;
   protected boolean myVolumeValid = false;
   protected double myRestVolume = 0;
   protected boolean myRestVolumeValid = false;

   protected double myCharacteristicSize = -1;
   protected int myNumIntegrationIndices = -1;

   protected FemMaterial myMaterial = null;

   // flag to indicate that forces need updating. Since forces are always
   // updated before a call to advance() (within setDefaultInputs()), this
   // flag only needs to be set during the advance routine --
   // specifically, within the setState methods
   protected boolean myForcesNeedUpdating = false;

   protected static Vector3d DEFAULT_GRAVITY = new Vector3d (0, 0, -9.8);;
   //protected static double DEFAULT_NU = 0.33;
   //protected static double DEFAULT_E = 500000;
   protected static double DEFAULT_DENSITY = 1000;
   protected static boolean DEFAULT_WARPING = true;
   protected static SurfaceRender DEFAULT_SURFACE_RENDERING =
      SurfaceRender.None;
   protected static double DEFAULT_STIFFNESS_DAMPING = 0;
   protected static double DEFAULT_MASS_DAMPING = 2;
   protected static Ranging DEFAULT_STRESS_PLOT_RANGING = Ranging.Auto;
   protected static DoubleInterval DEFAULT_STRESS_PLOT_RANGE = new DoubleInterval(0,1);

   //double myNu;
   //double myE;
   protected double myDensity;
   //   PropertyMode myNuMode = PropertyMode.Inherited;
   //   PropertyMode myEMode = PropertyMode.Inherited;
   PropertyMode myDensityMode = PropertyMode.Inherited;

   protected Vector3d myGravity = new Vector3d(DEFAULT_GRAVITY);
   protected PropertyMode myGravityMode = PropertyMode.Inherited;

   protected double myMassDamping;
   protected double myStiffnessDamping;

   protected int myNumInverted = 0;

   protected double myMaxTranslationalVel = 1e10;

   protected AABBTree myAABBTree;
   protected boolean myBVTreeValid;

   // protected boolean myTopLevel = false;

   public static PropertyList myProps =
      new PropertyList (FemModel.class, MechSystemBase.class);

   static {
      // surfaceRendering must be written out explicitly at the end,
      // so it is scanned at the end
      myProps.addInheritable (
         "surfaceRendering:Inherited", "surface rendering mode",
         DEFAULT_SURFACE_RENDERING, "NW");
      myProps.addInheritable ("stressPlotRanging:Inherited", 
         "ranging mode for stress plots",
         DEFAULT_STRESS_PLOT_RANGING);         
      myProps.addInheritable ("stressPlotRange:Inherited", 
         "stress value range for color stress plots",
         DEFAULT_STRESS_PLOT_RANGE);
      myProps.addInheritable (
         "gravity:Inherited", "acceleration of gravity", DEFAULT_GRAVITY);
      // myProps.add ("warping * *", "enable rotational warping", DEFAULT_WARPING);
      // myProps.add (
      //    "YoungsModulus", "Youngs modulus", DEFAULT_E);
      // myProps.add (
      //    "PoissonsRatio", "Poissons ratio", DEFAULT_NU);
      myProps.addInheritable ("density:Inherited", "density", DEFAULT_DENSITY);
      myProps.add (
         "particleDamping * *", "damping on each particle",
         DEFAULT_MASS_DAMPING);
      myProps.add (
         "stiffnessDamping * *", "damping on stiffness matrix",
         DEFAULT_STIFFNESS_DAMPING);
      myProps.addReadOnly ("volume *", "volume of the model");
      myProps.addReadOnly ("numInverted", "number of inverted elements");
      myProps.addReadOnly ("mass *", "mass of the model");
//      myProps.addReadOnly ("nodeMass *", "mass of the model");
      myProps.addReadOnly ("energy", "energy of the model");
      myProps.add (
         "material", "model material parameters", createDefaultMaterial(), "CE");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      myGravity = new Vector3d (DEFAULT_GRAVITY);
      myGravityMode = PropertyMode.Inherited;
      //myNu = DEFAULT_NU;
      //myE = DEFAULT_E;
      myDensity = DEFAULT_DENSITY;
      myDensityMode = PropertyMode.Inherited;
      mySurfaceRendering = DEFAULT_SURFACE_RENDERING;
      mySurfaceRenderingMode = PropertyMode.Inherited;
      myStressPlotRanging = DEFAULT_STRESS_PLOT_RANGING;
      myStressPlotRangingMode = PropertyMode.Inherited;
      myStressPlotRange = DEFAULT_STRESS_PLOT_RANGE;
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
      myMaxStepSize = 0.01;
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
      invalidateRestData();  // invalidate cached warping data
   }
   
   public synchronized void setLinearMaterial (
      double E, double nu, boolean corotated) {
      setMaterial (new LinearMaterial (E, nu, corotated));
   }

   // public void setLinearMaterial (double E, double nu) {
   //    setLinearMaterial (E, nu, /*corotated=*/true);
   // }

   public synchronized void setSurfaceRendering (SurfaceRender mode) {
      if (mySurfaceRendering != mode) {
         if (myStressPlotRanging == Ranging.Auto) {
            myStressPlotRange.set (0, 0);
         }
         mySurfaceRendering = mode;
      }
      mySurfaceRenderingMode =
         PropertyUtils.propagateValue (
            this, "surfaceRendering", mode, mySurfaceRenderingMode);
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
   
   public Ranging getStressPlotRanging (){
      return myStressPlotRanging;
   }
   
   public PropertyMode getStressPlotRangingMode() {
      return myStressPlotRangingMode;
   }
   
   public void setStressPlotRangingMode(PropertyMode mode) {
      if (mode != myStressPlotRangingMode) {
         myStressPlotRangingMode = PropertyUtils.setModeAndUpdate (
            this, "stressPlotRanging", myStressPlotRangingMode, mode);
      }
   }
   
   
   public PropertyMode getStressPlotRangeMode() {
      return myStressPlotRangeMode;
   }
   
   public void setStressPlotRangeMode(PropertyMode mode) {
      if (mode != myStressPlotRangeMode) {
         myStressPlotRangeMode = PropertyUtils.setModeAndUpdate (
            this, "stressPlotRange", myStressPlotRangeMode, mode);
      }
   }

   public void setStressPlotRanging (Ranging ranging) {
      if (myStressPlotRanging != ranging) {
         if (ranging == Ranging.Auto) {
            resetStressPlotRange();
         }
         myStressPlotRanging = ranging;
      }
      myStressPlotRangingMode =
         PropertyUtils.propagateValue (
            this, "stressPlotRanging", ranging, myStressPlotRangingMode);
   }

   public DoubleInterval getStressPlotRange (){
      return new DoubleInterval (myStressPlotRange);
   }
   
   public void resetStressPlotRange () {
      myStressPlotRange.set (0, 0);
   }

   public void setStressPlotRange (DoubleInterval range) {
      myStressPlotRange = new DoubleInterval (range);
      myStressPlotRangeMode =
         PropertyUtils.propagateValue (
            this, "stressPlotRange", range, myStressPlotRangeMode);
   }

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

//   public synchronized void setWarping (boolean enable) {
//      myWarpingP = enable;
//      if (myMaterial.hasProperty ("corotated")) {
//         myMaterial.getProperty ("corotated").set (enable);
//      }
//   }

//   public boolean getWarping() {
//      if (myMaterial.hasProperty ("corotated")) {
//         return (Boolean)myMaterial.getProperty ("corotated").get();
//      }
//      else {
//         return myWarpingP;
//      }
//   }

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

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (ScanWriteUtils.scanAndStorePropertyValue (
                  rtok, this, "surfaceRendering", tokens)) {
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
      myProps.get("surfaceRendering").writeIfNonDefault (
         this, pw, fmt, ancestor);
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

//   protected void resetMarkerForces() {
//      for (FemMarker mkr : myMarkers) {
//         mkr.setForcesToExternal();
//      }
//   }

//   protected void resetNodeForces() {
//      for (FemNode n : getNodes()) {
//         n.setForcesToExternal();
//      }
//   }

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
      //myE /= s;
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

   public double getEnergy() {
      double e = 0;
      for (FemNode n : getNodes()) {
         e += n.getVelocity().normSquared() / 2;
      }
      return e;
   }

   public void scaleMass (double s) {
      for (int i = 0; i < numNodes(); i++) {
         FemNode n = getNode (i);
         n.scaleMass (s);
      }
      myDensity *= s;
      //myE *= s;
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

   //protected abstract ArrayList<? extends FemNode> getActiveNodes();

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

//   public int getActivePosState (VectorNd x, int idx) {
//      ArrayList<? extends FemNode> activeNodes = getActiveNodes();
//      double[] buf = x.getBuffer();
//      for (int i = 0; i < activeNodes.size(); i++) {
//         idx = activeNodes.get (i).getPosState (buf, idx);
//      }
//      return idx;
//   }
//
//   public int getActiveVelState (VectorNd x, int idx) {
//      ArrayList<? extends FemNode> activeNodes = getActiveNodes();
//      double[] buf = x.getBuffer();
//      for (int i = 0; i < activeNodes.size(); i++) {
//         idx = activeNodes.get (i).getVelState (buf, idx);
//      }
//      return idx;
//   }

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
   
//   public void recursivelyUpdatePosState (int flags) {
//      myVolumeValid = false;
//      invalidateStressAndMaybeStiffness();
//   }

//   public int getActivePosDerivative (VectorNd dxdt, double t, int idx) {
//      ArrayList<? extends FemNode> activeNodes = getActiveNodes();
//      double[] buf = dxdt.getBuffer();
//      for (int i = 0; i < activeNodes.size(); i++) {
//         idx = activeNodes.get (i).getPosDerivative (buf, idx);
//      }
//      return idx;
//   }

//   public void recursivelyUpdateVelState (int flags) {
//   }
   
   public void updateSlaveVel() {
      
   }

//   protected void checkForInvertedElements() {
//      int num = 0;
//      for (int i = 0; i < numElements(); i++) {
//         if (getElement (i).isInverted()) {
//            num++;
//         }
//      }
//      myNumInverted = num;
//   }

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
      double volume = 0;
      for (FemElement e : getAllElements()) {
         e.computeVolumes();
         volume += e.getVolume();
      }
      myVolume = volume;
      myVolumeValid = true;
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

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {
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
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {
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

   public void recursivelyInitialize (double t, int level) {
      super.recursivelyInitialize (t, level);
   }

   protected void clearCachedData (ComponentChangeEvent e) {
      super.clearCachedData(e);
      myForcesNeedUpdating = true;
      invalidateStressAndStiffness();
      invalidateIntegrationIndices();
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
   
   public void getConstrainedComponents (List<DynamicComponent> list) {
   }
   
   // method declarations to implement ForceEffector

   public int getJacobianType() {
      return Matrix.SYMMETRIC;
   }

   public abstract void addSolveBlocks (SparseNumberedBlockMatrix M);
   
   public void updateIntegrationIndices() {
      if (myNumIntegrationIndices == -1) {
         myNumIntegrationIndices = assignIntegrationIndices();
      }
   }
   
   protected void invalidateIntegrationIndices() {
      myNumIntegrationIndices = -1;
   }
   
   public int numIntegrationIndices() {
      updateIntegrationIndices();
      return myNumIntegrationIndices;
   }
   
   protected abstract int assignIntegrationIndices();

}
