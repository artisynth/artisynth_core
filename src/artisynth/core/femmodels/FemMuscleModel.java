/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.PrintWriter;
import java.io.IOException;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Deque;

import maspack.util.NumberFormat;
import maspack.util.ObjectHolder;
import maspack.util.ReaderTokenizer;
import maspack.util.DataBuffer;
import maspack.geometry.BVNode;
import maspack.geometry.BVTree;
import maspack.geometry.Boundable;
import maspack.geometry.LineSegment;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.PolylineMesh;
import maspack.geometry.Vertex3d;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.Point3d;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer.LineStyle;
import artisynth.core.femmodels.MuscleBundle.DirectionRenderType;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.GenericMuscle;
import artisynth.core.materials.MaterialBase;
import artisynth.core.materials.MaterialChangeEvent;
import artisynth.core.materials.MuscleMaterial;
import artisynth.core.mechmodels.ExcitationComponent;
import artisynth.core.mechmodels.ExcitationUtils;
import artisynth.core.mechmodels.ExcitationSourceList;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.MuscleExciter;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScanToken;

public class FemMuscleModel extends FemModel3d implements ExcitationComponent {

   // option added just in case at some point it proves necessary to save
   // muscle excitation values as state
   protected static boolean saveExcitationsAsState = false;

   protected MuscleBundleList myMuscleList;
   protected MuscleMaterial myMuscleMat;
   protected ComponentList<MuscleExciter> myExciterList;

   private static DirectionRenderType DEFAULT_FIBER_RENDER_TYPE = DirectionRenderType.ELEMENT; 
   private static double DEFAULT_DIRECTION_RENDER_LEN = 0.0;
   private static RenderProps DEFAULT_FIBER_RENDER_PROPS = createDefaultFiberRenderProps();

   private double myDirectionRenderLen = DEFAULT_DIRECTION_RENDER_LEN;
   private DirectionRenderType myDirectionRenderType = DEFAULT_FIBER_RENDER_TYPE;
   private RenderProps myFiberRenderProps = new RenderProps(DEFAULT_FIBER_RENDER_PROPS); 

   PropertyMode myDirectionRenderTypeMode = PropertyMode.Inherited;
   PropertyMode myDirectionRenderLenMode = PropertyMode.Inherited;
   //   private boolean myDrawFibers = false;

   protected float[] myExcitationColor = null;
   protected PropertyMode myExcitationColorMode = PropertyMode.Inherited;
   protected double myMaxColoredExcitation = 1.0;
   protected PropertyMode myMaxColoredExcitationMode = PropertyMode.Explicit;

   // fields related to fiber mesh and making the entire muscle an
   // excitation component
   //   protected PolylineMesh myFiberMesh = null;
   //   protected boolean myFiberMeshActive = false;
   protected Vector3d myTmpDir = new Vector3d();
   private double myExcitation = 0;
   protected ExcitationSourceList myExcitationSources;
   protected CombinationRule myComboRule = CombinationRule.Sum;
   protected float[] myDirectionColor = new float[3];
   // minimum activation level
   protected static final double minActivation = 0.0;
   // maximum activation level
   protected static final double maxActivation = 1.0;

   public static PropertyList myProps =
   new PropertyList(FemMuscleModel.class, FemModel3d.class);

   static {
      myProps.add ("activations", "muscle activations", null);
      myProps.add(
         "muscleMaterial", "muscle material parameters",
         createDefaultMuscleMaterial(), "CE");
      myProps.addInheritable(
         "directionRenderLen:Inherited",
         "length of directions rendered in each element",
         DEFAULT_DIRECTION_RENDER_LEN);
      myProps.addInheritable(
         "directionRenderType:Inherited",
         "method for rendering fiber directions (per element, or per inode)",
         DEFAULT_FIBER_RENDER_TYPE);
      myProps.add("fiberRenderProps * *", 
         "render properties for fibers", null);
      myProps.addInheritable(
         "excitationColor", "color of activated muscles", null);
      myProps.addInheritable(
         "maxColoredExcitation",
         "excitation value for maximum colored excitation", 1.0, "[0,1]");
      myProps.addReadOnly(
         "netExcitation", "total excitation including excitation sources");
      myProps.add(
         "excitation", "internal muscle excitation", 0.0, "[0,1] NW");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   private static RenderProps createDefaultFiberRenderProps() {

      if (DEFAULT_FIBER_RENDER_PROPS == null) {
         RenderProps props = new RenderProps();
         props.setLineColor(new Color(0f,1f,1f));
         props.setLineStyle(LineStyle.LINE);
         DEFAULT_FIBER_RENDER_PROPS = props;
      }

      return DEFAULT_FIBER_RENDER_PROPS;
   }

   public Color getExcitationColor() {
      if (myExcitationColor == null) {
         return null;
      }
      else {
         return new Color(
            myExcitationColor[0], myExcitationColor[1], myExcitationColor[2]);
      }
   }

   public void setExcitationColor(Color color) {
      if (color == null) {
         myExcitationColor = null;
      }
      else {
         myExcitationColor = color.getRGBColorComponents(null);
      }
      myExcitationColorMode =
      PropertyUtils.propagateValue(
         this, "excitationColor", color, myExcitationColorMode);
   }

   public PropertyMode getExcitationColorMode() {
      return myExcitationColorMode;
   }

   public void setExcitationColorMode(PropertyMode mode) {
      myExcitationColorMode =
      PropertyUtils.setModeAndUpdate(
         this, "excitationColor", myExcitationColorMode, mode);
   }

   public double getMaxColoredExcitation() {
      return myMaxColoredExcitation;
   }

   public void setMaxColoredExcitation(double excitation) {
      myMaxColoredExcitation = excitation;
      myMaxColoredExcitationMode =
      PropertyUtils.propagateValue(
         this, "maxColoredExcitation", excitation,
         myMaxColoredExcitationMode);
   }

   public PropertyMode getMaxColoredExcitationMode() {
      return myMaxColoredExcitationMode;
   }

   public void setMaxColoredExcitationMode(PropertyMode mode) {
      myMaxColoredExcitationMode =
      PropertyUtils.setModeAndUpdate(
         this, "maxColoredExcitation", myMaxColoredExcitationMode, mode);
   }

   /**
    * {@inheritDoc}
    */
   public double getExcitation() {
      return myExcitation;
   }

   /**
    * {@inheritDoc}
    */
   public void setExcitation(double a) {
      // set activation within valid range
      double valid_a = a;
      valid_a = (valid_a > maxActivation) ? maxActivation : valid_a;
      valid_a = (valid_a < minActivation) ? minActivation : valid_a;
      myExcitation = valid_a;
   }

   /**
    * {@inheritDoc}
    */
   public ExcitationSourceList getExcitationSources() {
      return myExcitationSources;
   }

   /**
    * {@inheritDoc}
    */
   public void addExcitationSource (ExcitationComponent ex, double gain) {
      if (myExcitationSources == null) {
         myExcitationSources = new ExcitationSourceList();
      }
      myExcitationSources.add (ex, gain);
   }

   /**
    * {@inheritDoc}
    */
   public boolean removeExcitationSource (ExcitationComponent ex) {
      boolean removed = false;
      if (myExcitationSources != null) {
         removed = myExcitationSources.remove (ex);
         if (myExcitationSources.size() == 0) {
            myExcitationSources = null;
         }
      }
      return removed;
   }

   /**
    * {@inheritDoc}
    */
   public double getExcitationGain (ExcitationComponent ex) {
      return ExcitationUtils.getGain (myExcitationSources, ex);
   }

   /**
    * {@inheritDoc}
    */
   public boolean setExcitationGain (ExcitationComponent ex, double gain) {
      return ExcitationUtils.setGain (myExcitationSources, ex, gain);
   }

   /**
    * {@inheritDoc}
    */
   public void setCombinationRule(CombinationRule rule) {
      myComboRule = rule;
   }

   /**
    * {@inheritDoc}
    */
   public CombinationRule getCombinationRule() {
      return myComboRule;
   }

   /**
    * {@inheritDoc}
    */
   public double getNetExcitation() {
      return ExcitationUtils.combine(
         myExcitation, myExcitationSources, myComboRule);
   }

   /**
    * {@inheritDoc}
    */
   public void getSoftReferences (List<ModelComponent> refs) {
      super.getSoftReferences (refs);
      if (myExcitationSources != null) {
         myExcitationSources.getSoftReferences (refs);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      super.updateReferences (undo, undoInfo);
      myExcitationSources = ExcitationUtils.updateReferences (
         this, myExcitationSources, undo, undoInfo);
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      myDirectionRenderLen = DEFAULT_DIRECTION_RENDER_LEN;
      myDirectionRenderLenMode = PropertyMode.Inherited;
      myExcitationColor = null;
      myMaxColoredExcitation = 1.0;
   }

   public FemMuscleModel () {
      this(null);
   }

   public FemMuscleModel (String name) {
      super(name);
      myMuscleList = new MuscleBundleList("bundles", "b");
      myExciterList =
      new ComponentList<MuscleExciter>(MuscleExciter.class, "exciters", "x");
      addFixed (myMuscleList);
      addFixed (myExciterList);
      setMuscleMaterial(createDefaultMuscleMaterial());
   }

   public void setDirectionRenderLen(double size) {
      myDirectionRenderLen = size;
      myDirectionRenderLenMode =
      PropertyUtils.propagateValue(
         this, "directionRenderLen",
         myDirectionRenderLen, myDirectionRenderLenMode);
   }

   public double getDirectionRenderLen() {
      return myDirectionRenderLen;
   }

   public void setDirectionRenderLenMode(PropertyMode mode) {
      myDirectionRenderLenMode =
      PropertyUtils.setModeAndUpdate(
         this, "directionRenderLen", myDirectionRenderLenMode, mode);
   }

   public PropertyMode getDirectionRenderLenMode() {
      return myDirectionRenderLenMode;
   }

   public void setFiberRenderProps(RenderProps props) {
      myFiberRenderProps = props;
   }

   public RenderProps getFiberRenderProps() {
      return myFiberRenderProps;
   }

   public void setDirectionRenderType(DirectionRenderType type) {
      myDirectionRenderType = type;
      myDirectionRenderTypeMode =
      PropertyUtils.propagateValue(
         this, "directionRenderType",
         myDirectionRenderType, myDirectionRenderTypeMode);
   }

   public DirectionRenderType getDirectionRenderType() {
      return myDirectionRenderType;
   }

   public void setDirectionRenderTypeMode(PropertyMode mode) {
      myDirectionRenderTypeMode =
      PropertyUtils.setModeAndUpdate(
         this, "directionRenderType", myDirectionRenderTypeMode, mode);
   }

   public PropertyMode getDirectionRenderTypeMode() {
      return myDirectionRenderTypeMode;
   }   

   public static MuscleMaterial createDefaultMuscleMaterial() {
      return new GenericMuscle();
   }

   public MuscleMaterial getMuscleMaterial() {
      return myMuscleMat;
   }

   public MuscleMaterial createMuscleMaterial() {
      return new GenericMuscle();
   }

//   //@Override
//   public boolean isInvertible() {
//      return myMuscleMat == null || myMuscleMat.isInvertible();
//   }
//   
//   //@Override
//   public boolean isLinear() {
//      return myMuscleMat == null;
//   }
//   
//   //@Override
//   public boolean isCorotated() {
//      return myMuscleMat == null;
//   }
   
   protected void notifyElementsOfMuscleMatStateChange () {
      // clear states for any elements that use the model material
      for (MuscleBundle mus : myMuscleList) {
         if (mus.getMuscleMaterial() == null) {
            mus.notifyElementsOfMuscleMatStateChange();
         }
      }
   }
 
   public void setMuscleMaterial(MuscleMaterial mat) {
      if (mat == null) {
         throw new IllegalArgumentException(
         "MuscleMaterial not allowed to be null for FemMuscleModel");
      }
      MuscleMaterial old = myMuscleMat;
      myMuscleMat = (MuscleMaterial)MaterialBase.updateMaterial(
         this, "muscleMaterial", myMuscleMat, mat);
      // issue change event in case solve matrix symmetry or state has changed:
      MaterialChangeEvent mce = 
         MaterialBase.symmetryOrStateChanged ("muscleMaterial", mat, old);
      if (mce != null) {
         if (mce.stateChanged()) {
            notifyElementsOfMuscleMatStateChange();
         }
         componentChanged (mce);
      }
   }

   public void addMuscleBundle(MuscleBundle bundle) {
      if (!myMuscleList.contains(bundle)) {
         for (Muscle fibre : bundle.getFibres()) {
            bundle.checkFibrePoints(this, fibre);
         }
         for (MuscleElementDesc d : bundle.getElements()) {
            bundle.checkElementDesc(this, d);
         }
         myMuscleList.add(bundle);
      }
   }

   public boolean removeMuscleBundle(MuscleBundle bundle) {
      return myMuscleList.remove(bundle);
   }

   public void clearMuscleBundles() {
      myMuscleList.removeAll();
   }

   public RenderableComponentList<MuscleBundle> getMuscleBundles() {
      return myMuscleList;
   }

   public void addMuscleExciter(MuscleExciter mex) {
      // XXX check to see if existing targets are contained ...
      myExciterList.add(mex);
   }

   public boolean removeMuscleExciter(MuscleExciter mex) {
      return myExciterList.remove(mex);
   }

   public ComponentList<MuscleExciter> getMuscleExciters() {
      return myExciterList;
   }

   public void clearMuscleExciters() {
      myExciterList.clear();
   }

   public void scaleDistance(double s) {
      super.scaleDistance(s);
      myMuscleList.scaleDistance(s);
      if (myMuscleMat != null) {
         myMuscleMat.scaleDistance(s);
      }
   }

   public void scaleMass(double s) {
      super.scaleMass(s);
      myMuscleList.scaleMass(s);
      if (myMuscleMat != null) {
         myMuscleMat.scaleMass(s);
      }
   }

   @Override
   public void applyForces(double t) {
      super.applyForces(t);
      for (MuscleBundle mus : myMuscleList) {
         mus.applyForces(t);
      }
   }

   public void addPosJacobian(
      SparseNumberedBlockMatrix M, double s) {
      for (MuscleBundle mus : myMuscleList) {
         mus.addPosJacobian(M, s);
      }
      super.addPosJacobian(M, s);
   }

   public void addVelJacobian(
      SparseNumberedBlockMatrix M, double s) {

      for (MuscleBundle mus : myMuscleList) {
         mus.addVelJacobian(M, s);
      }
      super.addVelJacobian(M, s);
   }

   public void addSolveBlocks(SparseNumberedBlockMatrix S) {
      for (MuscleBundle mus : myMuscleList) {
         mus.addSolveBlocks(S);
      }
      super.addSolveBlocks(S);
   }

   protected void doclear() {
      super.doclear();
      myMuscleList.removeAll();
      myExciterList.removeAll();
   }

   @Override
   public void recursivelyInitialize(double t, int level) {
      if (t == 0) {
         // zero the excitations of any ExcitableComponent at t = 0.
         // Note that inputProbe.setState should do that same thing,
         // so we're just being extra careful here.
         for (MuscleBundle mus : myMuscleList) {
            mus.setExcitation(0);
//            for (MuscleElementDesc desc : mus.getElements()) {
//               desc.setExcitation(0);
//            }
         }
         for (MuscleExciter ex : myExciterList) {
            ex.setExcitation(0);
         }
      }
      super.recursivelyInitialize(t, level);
   }

   /*
    * preliminary methods for inverse solver
    * 
    * @see artisynth.core.mechmodels.MechSystemBase
    */

   public int numActivations() {
      return myMuscleList.size();
   }

   public VectorNd getActivations() {
      VectorNd act = new VectorNd(numActivations());
      getActivations(act, 0);
      return act;
   }

   public void getActivations(VectorNd a, int idx) {
      double[] buf = a.getBuffer();
      for (int i = 0; i < myMuscleList.size(); i++) {
         buf[idx++] = myMuscleList.get(i).getNetExcitation();
      }
   }

   public void setActivations(VectorNd a) {
      setActivations(a, 0);
   }

   public void setActivations(VectorNd a, int idx) {
      double buf[] = a.getBuffer();
      for (int i = 0; i < myMuscleList.size(); i++) {
         myMuscleList.get(i).setExcitation(buf[idx++]);
      }
      myForcesNeedUpdating = true;
      invalidateStressAndStiffness();
   }

   public void notifyActivationsSet() {
      myForcesNeedUpdating = true;
      invalidateStressAndStiffness();
   }

   //   public void setFiberMesh(PolylineMesh mesh) {
   //      myFiberMesh = mesh;
   //   }
   //
   //   public PolylineMesh getFiberMesh() {
   //      return myFiberMesh;
   //   }

//   public boolean hasSymmetricTangent() {
//      MuscleMaterial mat = getMuscleMaterial();
//      if (mat != null) {
//         return mat.hasSymmetricTangent();
//      }
//      else {
//         return true;
//      }
//   }

   //   public void addTangent(
   //      Matrix6d D, SymmetricMatrix3d stress, IntegrationPoint3d pt,
   //      IntegrationData3d dt, FemMaterial baseMat) {
   //
   //      MuscleMaterial mat = getMuscleMaterial();
   //      if (mat != null && dt.myFrame != null) {
   //         myTmpDir.x = dt.myFrame.m00;
   //         myTmpDir.y = dt.myFrame.m10;
   //         myTmpDir.z = dt.myFrame.m20;
   //         mat.addTangent(D, stress, getNetExcitation(), myTmpDir, pt, baseMat);
   //      }
   //   }

//   public void computeTangent(
//      Matrix6d D, SymmetricMatrix3d stress, SolidDeformation def,
//      IntegrationPoint3d pt, IntegrationData3d dt, FemMaterial baseMat) {
//      System.out.println ("COMPUTE TANGENT");
//      MuscleMaterial mat = getMuscleMaterial();
//      if (mat != null && dt.myFrame != null) {
//         myTmpDir.x = dt.myFrame.m00;
//         myTmpDir.y = dt.myFrame.m10;
//         myTmpDir.z = dt.myFrame.m20;
//         mat.computeTangent(D, stress, getNetExcitation(), myTmpDir, def, baseMat);
//      }
//      else {
//         D.setZero ();
//      }
//   }

//   public void computeTangent(
//      Matrix6d D, SymmetricMatrix3d stress, SolidDeformation def,
//      Matrix3d Q, FemMaterial baseMat) {
//
//      System.out.println ("COMPUTE TANGENT");
//      
//      MuscleMaterial mat = getMuscleMaterial();
//      if (mat != null) {
//         myTmpDir.x = Q.m00;
//         myTmpDir.y = Q.m10;
//         myTmpDir.z = Q.m20;
//         mat.computeTangent (
//            D, stress, getNetExcitation(), myTmpDir, def, baseMat);
//      }
//      else {
//         D.setZero ();
//      }
//   }


   //   public void addStress(
   //      SymmetricMatrix3d sigma, IntegrationPoint3d pt,
   //      IntegrationData3d dt, FemMaterial baseMat) {
   //
   //      MuscleMaterial mat = getMuscleMaterial();
   //      if (mat != null && dt.myFrame != null) {
   //         myTmpDir.x = dt.myFrame.m00;
   //         myTmpDir.y = dt.myFrame.m10;
   //         myTmpDir.z = dt.myFrame.m20;
   //         mat.addStress(sigma, getNetExcitation(), myTmpDir, pt, baseMat);
   //      }
   //   }

//   public void computeStress(
//      SymmetricMatrix3d sigma, SolidDeformation def,
//      IntegrationPoint3d pt, IntegrationData3d dt, FemMaterial baseMat) {
//      
//      System.out.println ("COMPUTE STRESS");
//      
//      MuscleMaterial mat = getMuscleMaterial();
//      if (mat != null && dt.myFrame != null) {
//         myTmpDir.x = dt.myFrame.m00;
//         myTmpDir.y = dt.myFrame.m10;
//         myTmpDir.z = dt.myFrame.m20;
//         mat.computeStress (sigma, getNetExcitation(), myTmpDir, def, baseMat);
//      }
//      else {
//         sigma.setZero();
//      }
//   }

//   public void computeStress(
//      SymmetricMatrix3d sigma, SolidDeformation def,
//      Matrix3d Q, FemMaterial baseMat) {
//      System.out.println ("COMPUTE STRESS");
//      MuscleMaterial mat = getMuscleMaterial();
//      if (mat != null) {
//         myTmpDir.x = Q.m00;
//         myTmpDir.y = Q.m10;
//         myTmpDir.z = Q.m20;
//         mat.computeStress (sigma, getNetExcitation(), myTmpDir, def, baseMat);
//      }
//      else {
//         sigma.setZero();
//      }
//   }

   //   public void transformGeometry (
   //      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
   //      super.transformGeometry (gtr, context, flags);
   //   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      context.addTransformableDescendants (myMuscleList, flags);
      super.addTransformableDependencies (context, flags);
   } 

   public void setBundlesActive(boolean active) {
      for (MuscleBundle bundle : myMuscleList) {
         bundle.setFibresActive(active);
      }
   }

   public MuscleBundle addFiberMeshBundle(double rad, PolylineMesh mesh) {
      MuscleBundle bundle = new MuscleBundle();
      addMuscleBundle(bundle);
      bundle.addFiberMeshElements(rad, mesh);
      return bundle;
   }

   /* --- Aux State Methods --- */

   /*
    * state get/set methods have been augmented to optionally save/restore
    * excitation values, in case this proves necessary at some point. This
    * feature is controlled by saveExcitationsAsState and is currently
    * disabled.
    */

   public void advanceState(double t0, double t1) {
      super.advanceState (t0, t1);
   }

   public void getState(DataBuffer data) {
      super.getState (data);
      if (saveExcitationsAsState) {
         // save exitation values for both muscles and exciters
         data.zput (myMuscleList.size());
         for (MuscleBundle b : myMuscleList) {
            data.dput (b.getExcitation());
         }
         data.zput (myExciterList.size());
         for (MuscleExciter e : myExciterList) {
            data.dput (e.getExcitation());
         }
      }
   }
   
   public void setState(DataBuffer data) {

      super.setState (data);
      if (saveExcitationsAsState) {
         // restore exitation values for both muscles and exciters
         int numm = data.zget();
         if (numm != myMuscleList.size()) {
            throw new IllegalStateException (
               "state has "+numm+" muscle excitations, expecting " +
               myMuscleList.size());
         }
         for (MuscleBundle b : myMuscleList) {
            b.setExcitation (data.dget());
         }
         int nume = data.zget();
         if (nume != myExciterList.size()) {
            throw new IllegalStateException (
               "state has "+nume+" exciter excitations, expecting " +
               myExciterList.size());
         }
         for (MuscleExciter e : myExciterList) {
            e.setExcitation (data.dget());
         }
      }
   }

   /* --- Render Methods --- */

   public void prerender(RenderList list) {
      super.prerender(list);
      myMuscleList.prerender(list);
      //      if (myFiberMeshActive) {
      double dirLen = getDirectionRenderLen();
      if (dirLen > 0) {
         for (FemElement3dBase e : getAllElements()) {
            // This is to ensure that the invJ0 in the warping data is
            // updated in the current (simulation) thread:
            e.getWarpingData();
         }
      }
      //      }
   }

   protected void renderElementDirection (
      Renderer renderer, RenderProps props, FemElement3dBase elem,
      float[] coords0, float[] coords1, Matrix3d F, Vector3d dir, double len) {

      IntegrationData3d[] idata = elem.getIntegrationData();   
      elem.computeRenderCoordsAndGradient(F, coords0);
      int ndirs = 0;
      dir.setZero();
      for (int i = 0; i < idata.length; i++) {
         Matrix3d Frame = idata[i].myFrame;
         if (Frame != null) {
            dir.x += Frame.m00;
            dir.y += Frame.m10;
            dir.z += Frame.m20;
            ndirs++;
         }
      }

      if (ndirs > 0) {
         dir.normalize();

         F.mul(dir, dir);
         double size = elem.computeDirectedRenderSize (dir);      
         dir.scale (0.5*size);
         dir.scale(len);

         coords0[0] -= (float)dir.x / 2;
         coords0[1] -= (float)dir.y / 2;
         coords0[2] -= (float)dir.z / 2;

         coords1[0] = coords0[0] + (float)dir.x;
         coords1[1] = coords0[1] + (float)dir.y;
         coords1[2] = coords0[2] + (float)dir.z;

         props.getLineColor(myDirectionColor);
         renderer.drawLine(
            props, coords0, coords1, myDirectionColor,
            /*capped=*/false, /*highlight=*/false);
      }

   }

   protected void renderIPointDirection (
      Renderer renderer, RenderProps props, FemElement3dBase elem,
      float[] coords0, float[] coords1, Matrix3d F, Vector3d dir, double len) {

      IntegrationPoint3d[] ipnt = elem.getIntegrationPoints();
      IntegrationData3d[] idata = elem.getIntegrationData();   

      for (int i=0; i<ipnt.length; i++) {
         Matrix3d Frame = idata[i].myFrame;

         if (Frame != null) {

            dir.x = Frame.m00;
            dir.y = Frame.m10;
            dir.z = Frame.m20;

            ipnt[i].computeGradientForRender(F, elem.getNodes(), idata[i].myInvJ0);
            ipnt[i].computeCoordsForRender(coords0, elem.getNodes());
            F.mul(dir,dir);
            
            double size = elem.computeDirectedRenderSize (dir);
            dir.scale(0.5*size);
            dir.scale(len);

            coords0[0] -= (float)dir.x / 2;
            coords0[1] -= (float)dir.y / 2;
            coords0[2] -= (float)dir.z / 2;
            coords1[0] = coords0[0] + (float)dir.x;
            coords1[1] = coords0[1] + (float)dir.y;
            coords1[2] = coords0[2] + (float)dir.z;

            props.getLineColor(myDirectionColor);
            renderer.drawLine(
               props, coords0, coords1, myDirectionColor,
               /*capped=*/false, /*highlight=*/false);
         }
      }

   }

   void renderDirection(
      Renderer renderer, RenderProps props, FemElement3dBase elem,
      float[] coords0, float[] coords1, Matrix3d F, Vector3d dir, double len) {

      switch(myDirectionRenderType) {
         case ELEMENT:
            renderElementDirection(renderer, props, elem, coords0, coords1, F, dir, len);
            break;
         case INTEGRATION_POINT:
            renderIPointDirection(renderer, props, elem, coords0, coords1, F, dir, len);
            break;
      }

   }

   //   public void setDrawFibers(boolean enable) {
   //      myDrawFibers = enable;
   //   }

   public void render(Renderer renderer, int flags) {
      super.render(renderer, flags);

      //      if (myFiberMesh != null) {
      //         myFiberMesh.render(renderer, myRenderProps, /* flags= */0);
      //      }
      //      if (myDrawFibers) {
      RenderProps fiberRenderProps = myFiberRenderProps;
      if (fiberRenderProps == null) {
         fiberRenderProps = DEFAULT_FIBER_RENDER_PROPS;
      }

      double dirLen = getDirectionRenderLen();
      if (dirLen > 0) {
         Matrix3d F = new Matrix3d();
         Vector3d dir = new Vector3d();
         float[] coords0 = new float[3];
         float[] coords1 = new float[3];
         for (FemElement3dBase e : getAllElements()) {

            renderDirection(
               renderer, fiberRenderProps, e, coords0, coords1, F, dir, dirLen);
         }
      }
   }
   //   }

   /**
    * {@inheritDoc}
    */
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor) 
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      if (myExcitationSources != null) {
         myExcitationSources.write (pw, "excitationSources", fmt, ancestor);
      }
   }

   /**
    * {@inheritDoc}
    */
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
   throws IOException {
      rtok.nextToken();
      if (scanAttributeName (rtok, "excitationSources")) {
         myExcitationSources =
         ExcitationUtils.scan (rtok, "excitationSources", tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   /**
    * {@inheritDoc}
    */
   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "excitationSources")) {
         myExcitationSources.postscan (tokens, ancestor);
         return true;
      }   
      return super.postscanItem (tokens, ancestor);
   }
   
   public void propertyChanged (PropertyChangeEvent e) {
      if (e instanceof MaterialChangeEvent) {
         MaterialChangeEvent mce = (MaterialChangeEvent)e;
         if (mce.stateChanged() && e.getHost() == getMuscleMaterial()) {
            notifyElementsOfMuscleMatStateChange();
         }
         // no need to notify parent - superclass code will do this
      }
      super.propertyChanged (e);
   }

}
