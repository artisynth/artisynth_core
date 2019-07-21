/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemModel;
import artisynth.core.femmodels.IntegrationData3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.MuscleBundle;
import artisynth.core.femmodels.MuscleBundle.DirectionRenderType;
import artisynth.core.femmodels.MuscleBundleList;
import artisynth.core.femmodels.MuscleElementDesc;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.GenericMuscle;
import artisynth.core.materials.MaterialBase;
import artisynth.core.materials.MaterialChangeEvent;
import artisynth.core.materials.MuscleMaterial;
import artisynth.core.mechmodels.ExcitationComponent;
import artisynth.core.mechmodels.ExcitationSourceList;
import artisynth.core.mechmodels.ExcitationUtils;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.MuscleExciter;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.util.ScanToken;
import maspack.geometry.BVNode;
import maspack.geometry.BVTree;
import maspack.geometry.Boundable;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.LineSegment;
import maspack.geometry.PolylineMesh;
import maspack.geometry.Vertex3d;
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
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.widgets.LabeledComponentBase;

public class MFreeMuscleModel extends MFreeModel3d
   implements ExcitationComponent {

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
   //  protected boolean myFiberMeshActive = false;
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
      new PropertyList(MFreeMuscleModel.class, MFreeModel3d.class);

   static {
      myProps.add("activations", "muscle activations", null);
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
   @Override
   public void addExcitationSource (ExcitationComponent ex, double gain) {
      if (myExcitationSources == null) {
         myExcitationSources = new ExcitationSourceList();
      }
      myExcitationSources.add (ex, gain);
   }

   /**
    * {@inheritDoc}
    */
   @Override
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
   @Override
   public double getExcitationGain (ExcitationComponent ex) {
      return ExcitationUtils.getGain (myExcitationSources, ex);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean setExcitationGain (ExcitationComponent ex, double gain) {
      return ExcitationUtils.setGain (myExcitationSources, ex, gain);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public double getNetExcitation() {
      return ExcitationUtils.combine(
         myExcitation, myExcitationSources, myComboRule);
   }

   /**
    * {@inheritDoc}
    */
   @Override
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

   public MFreeMuscleModel () {
      this(null);
   }

   public MFreeMuscleModel (String name) {
      super(name);
      myMuscleList =
         new MuscleBundleList("bundles", "b");
      myExciterList =
         new ComponentList<MuscleExciter>(MuscleExciter.class, "exciters", "x");
      add(myMuscleList);
      add(myExciterList);
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

   public void setMuscleMaterial(MuscleMaterial mat) {
      if (mat == null) {
         throw new IllegalArgumentException(
            "MuscleMaterial not allowed to be null for MuscleBundle");
      }
      MuscleMaterial old = myMuscleMat;
      myMuscleMat = (MuscleMaterial)MaterialBase.updateMaterial(
         this, "muscleMaterial", myMuscleMat, mat);
      // issue change event in case solve matrix symmetry or state has changed:
      MaterialChangeEvent mce = 
         MaterialBase.symmetryOrStateChanged ("muscleMaterial", mat, old);
      if (mce != null) {
         if (mce.stateChanged()) {
            // TODO: HANDLE ELEMENT STATE CHNAGE
         }
         componentChanged (mce);
      }     
   }

   public void addMuscleBundle(MuscleBundle bundle) {
      if (!myMuscleList.contains(bundle)) {
         //         for (Muscle fibre : bundle.getFibres()) {
         //            bundle.checkFibrePoints(this, fibre);
         //         }
         //         for (MuscleElementDesc d : bundle.getElements()) {
         //            bundle.checkElementDesc(this, d);
         //         }
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

   public void scaleDistance(double s) {
      super.scaleDistance(s);
      myMuscleList.scaleDistance(s);
      myMuscleMat.scaleDistance(s);
      myDirectionRenderLen *= s;
   }

   public void scaleMass(double s) {
      super.scaleMass(s);
      myMuscleList.scaleMass(s);
      myMuscleMat.scaleMass(s);
   }

   //   private static int addVertex(PolygonalMesh mesh, FemNode3d n) {
   //      int index = -1;
   //      index = mesh.getVertices().indexOf(n.getPosition());
   //      if (index == -1) {
   //         index =
   //            mesh.getVertices().indexOf(
   //               mesh.addVertex(n.getPosition(), true));
   //      }
   //      return index;
   //   }

   // @Override
   // public void updateForces (double t, StepAdjust stepAdjust) {
   // // this is a copy for FemModel.updateForces, except that bundle
   // // forces are also applied
   // // if (isTopLevel()) {
   // // getMassMatrix (t, null); // make sure effective masses are computed
   // // }
   // resetMarkerForces();
   // updateNodeForces (t, stepAdjust);
   // for (MuscleBundle mus : myMuscleList) {
   // mus.applyForce (t, stepAdjust);
   // }
   // if (isTopLevel()) {
   // applyAttachmentForces();
   // }
   // myForcesNeedUpdating = false;
   // }
   //
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

   public static void addControls(
      ControlPanel controlPanel, FemModel femModel, ModelComponent topModel) {
      MFreeModel3d.addControls(controlPanel, femModel, topModel);
      controlPanel.addWidget(femModel, "muscleMaterial");
   }

   public static void addBundleControls(
      ControlPanel panel, MFreeMuscleModel muscle) {
      for (MuscleBundle b : muscle.getMuscleBundles()) {
         LabeledComponentBase widget =
            panel.addWidget(b.getName(), b, "excitation");
         if (b.getRenderProps() != null) {
            widget.setLabelFontColor(b.getRenderProps().getLineColor());
         }
      }
   }

   /**
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

   public boolean hasSymmetricTangent() {
      MuscleMaterial mat = getMuscleMaterial();
      if (mat != null) {
         return mat.hasSymmetricTangent();
      }
      else {
         return true;
      }
   }

//   public void addTangent(
//      Matrix6d D, SymmetricMatrix3d stress, IntegrationPoint3d pt,
//      IntegrationData3d dt, FemMaterial baseMat) {
//
//      MuscleMaterial mat = getMuscleMaterial();
//      if (mat != null && dt.getFrame() != null) {
//         myTmpDir.x = dt.getFrame().m00;
//         myTmpDir.y = dt.getFrame().m10;
//         myTmpDir.z = dt.getFrame().m20;
//         mat.addTangent(D, stress, getNetExcitation(), myTmpDir, pt, baseMat);
//      }
//   }
   
//   public void computeTangent(
//      Matrix6d D, SymmetricMatrix3d stress, SolidDeformation def,
//      IntegrationPoint3d pt, IntegrationData3d dt, FemMaterial baseMat) {
//
//      MuscleMaterial mat = getMuscleMaterial();
//      if (mat != null && dt.getFrame() != null) {
//         myTmpDir.x = dt.getFrame().m00;
//         myTmpDir.y = dt.getFrame().m10;
//         myTmpDir.z = dt.getFrame().m20;
//         mat.computeTangent(D, stress, getNetExcitation(), myTmpDir, def, baseMat);
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
//      if (mat != null && dt.getFrame() != null) {
//         myTmpDir.x = dt.getFrame().m00;
//         myTmpDir.y = dt.getFrame().m10;
//         myTmpDir.z = dt.getFrame().m20;
//         mat.addStress(sigma, getNetExcitation(), myTmpDir, pt, baseMat);
//      }
//   }
   
//   public void computeStress(
//      SymmetricMatrix3d sigma, SolidDeformation def,
//      IntegrationPoint3d pt, IntegrationData3d dt, FemMaterial baseMat) {
//      
//      MuscleMaterial mat = getMuscleMaterial();
//      if (mat != null && dt.getFrame() != null) {
//         myTmpDir.x = dt.getFrame().m00;
//         myTmpDir.y = dt.getFrame().m10;
//         myTmpDir.z = dt.getFrame().m20;
//         mat.computeStress(sigma, getNetExcitation(), myTmpDir, def, baseMat);
//      }
//      else {
//         sigma.setZero ();
//      }
//   }
   
//   private SymmetricMatrix3d tmpStress = new SymmetricMatrix3d();
//   @Override
//   public void addStressAndTangent(SymmetricMatrix3d sigma, Matrix6d D,
//      IntegrationPoint3d pt, IntegrationData3d dt, FemMaterial baseMat) {
//      computeStress(tmpStress, pt, dt, baseMat);
//      sigma.add(tmpStress);
//      addTangent(D, tmpStress, pt, dt, baseMat);
//   }
//   
//   @Override
//   public void computeStressAndTangent(SymmetricMatrix3d sigma, Matrix6d D,
//      IntegrationPoint3d pt, IntegrationData3d dt, FemMaterial baseMat) {
//      computeStress(sigma, pt, dt, baseMat);
//      computeTangent(D, sigma, pt, dt, baseMat);
//   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      super.transformGeometry (gtr, context, flags);
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      context.addTransformableDescendants (myMuscleList, flags);
      super.addTransformableDependencies (context, flags);
   } 
   
   public static boolean computeAverageFiberDirection(
      Vector3d dir, Point3d pos, double rad, PolylineMesh mesh) {

      BVTree bvh = mesh.getBVTree();
      ArrayList<BVNode> nodes = new ArrayList<BVNode>();

      Matrix3d cov = new Matrix3d();
      SVDecomposition3d svd = new SVDecomposition3d();

      Vector3d tmp = new Vector3d();
      Matrix3d tmp2 = new Matrix3d();

      bvh.intersectSphere(nodes, pos, rad);

      dir.setZero();

      int nsegs = 0;
      
      Vector3d segmentSum = new Vector3d(); //for computing sign of direction vector

      // System.out.println("p=[");
      for (BVNode n : nodes) {
         Boundable[] elements = n.getElements();

         for (int i = 0; i < elements.length; i++) {

            LineSegment seg = (LineSegment)elements[i];
            seg = getSegmentInsideSphere(seg, pos, rad);

            if (seg != null) {
               tmp.sub(seg.myVtx1.pnt, seg.myVtx0.pnt);
               
               if (tmp.norm() >= 1e-8 * rad) {
//                  System.out.println(seg.myVtx0.getPosition() + " " +
//                     seg.myVtx1.getPosition());
                  nsegs++;
                  // prepare to average directions using SVD
                  computeCov(tmp2, tmp);
                  cov.add(tmp2);
                  segmentSum.add(tmp);
               }
            }
         }
      }
//      System.out.println("];");

      if (nsegs > 0) {

         // we are technically including both +/- directions, so
         // we have twice the number of points
         cov.scale(2.0 / (2.0 * nsegs - 1));
         try {
            svd.factor(cov);
         } catch (Exception e) {
            //System.err.println(e.getMessage());
         }
         tmp2 = svd.getU(); // principal components
         tmp2.getColumn(0, dir);

         dir.normalize();

         // flip sign if not in same direction as
         // most line segments
         if (dir.dot(segmentSum)<0) {
            dir.scale(-1);
         }
         
         
//          System.out.println("c=["+pos + " " + rad + "];");
//          System.out.println("dir=[" + dir +"];\n");

         return true;
      }
      else {
         return false;
      }

   }

   private static void computeCov(Matrix3d mat, Vector3d vec) {
      mat.m00 = vec.x * vec.x;
      mat.m01 = vec.x * vec.y;
      mat.m02 = vec.x * vec.z;
      mat.m10 = mat.m01;
      mat.m11 = vec.y * vec.y;
      mat.m12 = vec.y * vec.z;
      mat.m20 = mat.m02;
      mat.m21 = mat.m12;
      mat.m22 = vec.z * vec.z;
   }

   // intersects segment with the given sphere, and returns
   // the portion inside or null if no intersection
   private static LineSegment getSegmentInsideSphere (
      LineSegment segment, Point3d pos,
      double rad) {

      Point3d p1 = segment.myVtx0.getPosition();
      Point3d p2 = segment.myVtx1.getPosition();

      // check if segment starts inside
      boolean isP1Inside = isInsideSphere(p1, pos, rad);
      Point3d p = new Point3d(); // p2-c
      Point3d dp = new Point3d(); // p1-p2

      double r2 = rad * rad;
      double a, b, c, d, lambda1, lambda2; // for use with quadratic equation

      p.sub(p2, pos);
      dp.sub(p1, p2);

      // find intersection with sphere
      a = dp.normSquared();
      b = 2 * dp.dot(p);
      c = p.normSquared() - r2;

      d = b * b - 4 * a * c;
      if (d >= 0) {
         d = Math.sqrt(d);
         lambda1 = (-b + d) / (2 * a);
         lambda2 = (-b - d) / (2 * a);
      } else {
         lambda1 = Double.NaN;
         lambda2 = Double.NaN;
      }

      // if p2 is inside, return
      if (p.normSquared() <= r2) {
         if (isP1Inside) {
            return segment;
         } else {

            // find intersection
            if (lambda1 >= 0 && lambda1 <= 1) {
               p1.scaledAdd(lambda1, dp, p2);
            } else {
               p1.scaledAdd(lambda2, dp, p2);
            }
            return new LineSegment(new Vertex3d(p1), new Vertex3d(p2));
         }

      } else {
         // if p1 inside, find single intersection
         if (isP1Inside) {
            if (lambda1 >= 0 && lambda1 <= 1) {
               p2.scaledAdd(lambda1, dp);
            } else {
               p2.scaledAdd(lambda2, dp);
            }
            return new LineSegment(new Vertex3d(p1), new Vertex3d(p2));

         } else {

            // check if passes entirely through sphere
            if (d >= 0) {
               if (lambda1 >= 0 && lambda1 <= 1 && lambda2 >= 0 && lambda2 <= 1) {
                  p1.scaledAdd(lambda1, dp, p2);
                  p2.scaledAdd(lambda2, dp);
                  return new LineSegment(new Vertex3d(p1), new Vertex3d(p2));
               }

            } // done checking if crossed sphere
         } // done checking if p1 outside
      } // done checking

      return null;

   }

   private static boolean isInsideSphere(Point3d pos, Point3d c, double rad) {

      Point3d p = new Point3d();
      p.sub(pos, c);
      if (p.normSquared() <= rad * rad) {
         return true;
      }
      return false;

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
   
//   public void setFiberMeshActive(boolean enable) {
//      if (myFiberMesh == null) {
//         throw new IllegalStateException(
//            "Fiber mesh is null; must be set before it can be activated");
//      }
//      setFiberMeshActive(enable, 0, myFiberMesh);
//   }

//   public void setFiberMeshActive(
//      boolean enable, double dist, PolylineMesh mesh) {
//
//      if (enable) {
//         if (dist <= 0) {
//            dist = 0.05 * RenderableUtils.getRadius(mesh);
//         }
//         activateFiberMesh(dist, mesh);
//      }
//      else {
//         deactivateFiberMesh();
//      }
//      myFiberMeshActive = enable;
//      myDrawFibers = enable;
//   }

//   private void activateFiberMesh(double rad, PolylineMesh mesh) {
//
//      Point3d pos = new Point3d();
//      Vector3d dir = new Vector3d();
//      Vector3d dirOld = new Vector3d();
//
//      for (FemElement3d e : getElements()) {
//         IntegrationPoint3d[] pnts = e.getIntegrationPoints();
//         IntegrationData3d[] data = e.getIntegrationData();
//         boolean elemIsActive = false;
//         for (int i = 0; i < pnts.length; i++) {
//            pnts[i].computePosition(pos, e.getNodes());
//
//            boolean valid = false;
//            valid = computeAverageFiberDirection(dir, pos, rad, mesh);
//
//            // compare old and new methods
//            // valid = computeAverageFiberDirection_Old(dirOld, pos, rad, mesh);
//            // if (dir.dot(dirOld) < 0) {
//            // dirOld.scale(-1);
//            // }
//            // if (dir.dot(dirOld) < Math.cos(Math.toRadians(10))
//            // && dirOld.norm() != 0) { // 10 degree difference
//            // System.out.println("old: " + dirOld + ", new: " + dir
//            // + ", angle: " + Math.toDegrees(Math.acos(dir.dot(dirOld))));
//            // }
//
//            if (valid) {
//               Matrix3d F = new Matrix3d();
//               F.m00 = dir.x;
//               F.m10 = dir.y;
//               F.m20 = dir.z;
//               data[i].myFrame = F;
//               elemIsActive = true;
//            }
//            else {
//               data[i].myFrame = null;
//            }
//         }
//         if (elemIsActive) {
//            e.addAuxiliaryMaterial(this);
//         }
//      }
//   }
//
//   private void deactivateFiberMesh() {
//
//      for (FemElement3d e : getElements()) {
//         IntegrationData3d[] data = e.getIntegrationData();
//         for (int i = 0; i < data.length; i++) {
//            data[i].myFrame = null;
//         }
//         e.removeAuxiliaryMaterial(this);
//      }
//   }

   public void prerender(RenderList list) {
      super.prerender(list);
      myMuscleList.prerender(list);
//      if (myFiberMeshActive) {
         double dirLen = getDirectionRenderLen();
         if (dirLen > 0) {
            for (FemElement3d e : getElements()) {
               // This is to ensure that the invJ0 in the warping data is
               // updated in the current (simulation) thread:
               e.getWarpingData();
            }
         }
//      }
   }

   protected void renderElementDirection(Renderer renderer, RenderProps props, FemElement3d elem,
      float[] coords0, float[] coords1, Matrix3d F, Vector3d dir, double len) {
      
      IntegrationData3d[] idata = elem.getIntegrationData();   
      elem.computeRenderCoordsAndGradient(F, coords0);
      int ndirs = 0;
      dir.setZero();
      for (int i = 0; i < idata.length; i++) {
         Matrix3d Frame = idata[i].getFrame();
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
   
   protected void renderIPointDirection(Renderer renderer, RenderProps props, FemElement3d elem,
      float[] coords0, float[] coords1, Matrix3d F, Vector3d dir, double len) {
      
      IntegrationPoint3d[] ipnt = elem.getIntegrationPoints();
      IntegrationData3d[] idata = elem.getIntegrationData();   
      
      for (int i=0; i<ipnt.length; i++) {
         Matrix3d Frame = idata[i].getFrame();
         
         if (Frame != null) {
         
            dir.x = Frame.m00;
            dir.y = Frame.m10;
            dir.z = Frame.m20;
            
            ipnt[i].computeGradientForRender(F, elem.getNodes(), idata[i].getInvJ0());
            ipnt[i].computeCoordsForRender(coords0, elem.getNodes());
            F.mul(dir,dir);
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
      Renderer renderer, RenderProps props, FemElement3d elem,
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
            for (FemElement3d e : getElements()) {
               
               renderDirection(
                  renderer, fiberRenderProps, e, coords0, coords1, F, dir, dirLen);
            }
         }
      }
//   }

   /**
    * {@inheritDoc}
    */
   @Override
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
   @Override
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
   @Override
   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "excitationSources")) {
         myExcitationSources.postscan (tokens, ancestor);
         return true;
      }   
      return super.postscanItem (tokens, ancestor);
   }
}
