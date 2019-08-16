/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import maspack.geometry.DelaunayInterpolator;
import maspack.geometry.PolylineMesh;
import maspack.geometry.PolylineInterpolator;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix;
import maspack.matrix.Point3d;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.materials.AxialMuscleMaterial;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.GenericMuscle;
import artisynth.core.materials.MaterialBase;
import artisynth.core.materials.MaterialChangeEvent;
import artisynth.core.materials.MuscleMaterial;
import artisynth.core.mechmodels.AxialSpringList;
import artisynth.core.mechmodels.ExcitationComponent;
import artisynth.core.mechmodels.ExcitationSourceList;
import artisynth.core.mechmodels.ExcitationUtils;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CompositeComponentBase;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.PropertyChangeListener;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScanToken;

public class MuscleBundle extends CompositeComponentBase 
   implements ExcitationComponent, RenderableComponent, TransformableGeometry,
   PropertyChangeListener {

   private static DirectionRenderType DEFAULT_FIBER_RENDER_TYPE = DirectionRenderType.ELEMENT; 
   private DirectionRenderType myDirectionRenderType = DEFAULT_FIBER_RENDER_TYPE;
   PropertyMode myDirectionRenderTypeMode = PropertyMode.Inherited;
   public enum DirectionRenderType {
      ELEMENT, INTEGRATION_POINT
   }
   
   double myExcitation = 0.0;
   protected ExcitationSourceList myExcitationSources;
   protected CombinationRule myComboRule = CombinationRule.Sum;
   protected MuscleMaterial myMuscleMat;
   protected RenderProps myRenderProps;

   protected AxialSpringList<Muscle> myFibres;
   protected MuscleElementDescList myElementDescs;

   public static PropertyList myProps =
      new PropertyList (MuscleBundle.class, CompositeComponentBase.class);

   //protected boolean myElementsActive;
   protected boolean myFibresActive;
   private double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
   PropertyMode myElementWidgetSizeMode = PropertyMode.Inherited;
   private double myDirectionRenderLen = DEFAULT_DIRECTION_RENDER_LEN;
   PropertyMode myDirectionRenderLenMode = PropertyMode.Inherited;

   protected float[] myExcitationColor = null;
   protected PropertyMode myExcitationColorMode = PropertyMode.Inherited;
   protected double myMaxColoredExcitation = 1.0;
   protected PropertyMode myMaxColoredExcitationMode = PropertyMode.Inherited;

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createLineFaceProps (host);
      return props;
   }
   //static private boolean DEFAULT_ELEMENTS_ACTIVE = true;
   static private boolean DEFAULT_FIBRES_ACTIVE = false;
   private static double DEFAULT_ELEMENT_WIDGET_SIZE = 0.0;
   private static double DEFAULT_DIRECTION_RENDER_LEN = 0.0;

   static {
      myProps.add ("renderProps", "render properties", defaultRenderProps(null));
      myProps.add ("excitation", "Muscle excitation", 0.0, "[0,1] NW");
      myProps.addReadOnly ("netExcitation", "total excitation including excitation sources");
//       myProps.add (
//          "elementsActive", "true if element components are active",
//          DEFAULT_ELEMENTS_ACTIVE);
      myProps.add (
         "fibresActive", "true if fibre components are active", 
         DEFAULT_FIBRES_ACTIVE);
      myProps.add (
         "muscleMaterial", "muscle material parameters", null);
      myProps.addInheritable (
         "elementWidgetSize:Inherited",
         "size of rendered widget in each element's center",
         DEFAULT_ELEMENT_WIDGET_SIZE, "[0,1]");
      myProps.addInheritable (
         "directionRenderLen:Inherited",
         "length of directions rendered in each element",
         DEFAULT_DIRECTION_RENDER_LEN);
      myProps.addInheritable(
         "directionRenderType:Inherited",
         "method for rendering fiber directions (per element, or per inode)",
         DEFAULT_FIBER_RENDER_TYPE);
      myProps.addInheritable (
         "excitationColor", "color of activated muscles", null);
       myProps.addInheritable (
         "maxColoredExcitation",
         "excitation value for maximum colored excitation", 1.0, "[0,1]");
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      //myElementsActive = DEFAULT_ELEMENTS_ACTIVE;
      myFibresActive = DEFAULT_FIBRES_ACTIVE;
      myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
      myElementWidgetSizeMode = PropertyMode.Inherited;
      myDirectionRenderLen = DEFAULT_DIRECTION_RENDER_LEN;
      myDirectionRenderLenMode = PropertyMode.Inherited;
      setRenderProps (defaultRenderProps (null));
      myExcitationColor = null;
   }

   public Color getExcitationColor() {
      if (myExcitationColor == null) {
         return null;
      }
      else {
         return new Color (
            myExcitationColor[0], myExcitationColor[1], myExcitationColor[2]);
      }
   }

   public void setExcitationColor (Color color) {
      if (color == null) {
         myExcitationColor = null;
      }
      else {
         myExcitationColor = color.getRGBColorComponents(null);
      }
      myExcitationColorMode =
         PropertyUtils.propagateValue (
            this, "excitationColor", color, myExcitationColorMode);
   }

   public PropertyMode getExcitationColorMode() {
      return myExcitationColorMode;
   }

   public void setExcitationColorMode (PropertyMode mode) {
      myExcitationColorMode =
         PropertyUtils.setModeAndUpdate (
            this, "excitationColor", myExcitationColorMode, mode);
   }

   public double getMaxColoredExcitation() {
      return myMaxColoredExcitation;
   }

   public void setMaxColoredExcitation (double excitation) {
      myMaxColoredExcitation = excitation;
      myMaxColoredExcitationMode =
         PropertyUtils.propagateValue (
            this, "maxColoredExcitation", excitation, myMaxColoredExcitationMode);
   }

   public PropertyMode getMaxColoredExcitationMode() {
      return myMaxColoredExcitationMode;
   }

   public void setMaxColoredExcitationMode (PropertyMode mode) {
      myMaxColoredExcitationMode =
         PropertyUtils.setModeAndUpdate (
            this, "maxColoredExcitation", myMaxColoredExcitationMode, mode);
   }

   public boolean getFibresActive() {
      return myFibresActive;
   }

   public void setFibresActive (boolean active) {
      myFibresActive = active;
      notifyStructureChanged (this); // will change stiffness matrix, etc
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

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

   public void setDirectionRenderLen (double size) {
      myDirectionRenderLen = size;
      myDirectionRenderLenMode = 
         PropertyUtils.propagateValue (
            this, "directionRenderLen",
            myDirectionRenderLen, myDirectionRenderLenMode);
   }

   public double getDirectionRenderLen () {
      return myDirectionRenderLen;
   }

   public void setDirectionRenderLenMode (PropertyMode mode) {
      myDirectionRenderLenMode =
         PropertyUtils.setModeAndUpdate (
            this, "directionRenderLen", myDirectionRenderLenMode, mode);
   }

   public PropertyMode getDirectionRenderLenMode() {
      return myDirectionRenderLenMode;
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
   
   public MuscleBundle() {
      this (null);
   }

   public MuscleBundle (String name) {
      super (name);

      myFibres = new AxialSpringList<Muscle> (Muscle.class, "fibres", "f");
      myElementDescs = new MuscleElementDescList ("elementDescs", "e");

      add (myFibres);
      add (myElementDescs);

      setDefaultValues();
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
   public void initialize (double t) {
      if (t == 0) {
         setExcitation (0);
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public void setExcitation (double a) {
      myExcitation = a;
   }

   /**
    * {@inheritDoc}
    */
   public void setCombinationRule (CombinationRule rule) {
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
      double ex = 
         ExcitationUtils.combine (
            myExcitation, myExcitationSources, myComboRule);
      return ex;
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

   public MuscleMaterial getMuscleMaterial() {
      return myMuscleMat;
   }

   public MuscleMaterial createMuscleMaterial() {
      return new GenericMuscle();
   }

   MuscleMaterial getEffectiveMuscleMaterial () {
      if (myMuscleMat != null) {
         return myMuscleMat;
      }
      else {
         ModelComponent parent = getGrandParent();
         if (parent instanceof FemMuscleModel) {
            return ((FemMuscleModel)parent).getMuscleMaterial();
         }
      }
      return null;      
   }
   
   protected void notifyElementsOfMuscleMatStateChange () {
      // clear states for any elements that use the model material
      for (MuscleElementDesc desc : myElementDescs) {
         desc.myElement.notifyStateVersionChanged();
      }
   }

   public <T extends MuscleMaterial> void setMuscleMaterial (T mat) {
      MuscleMaterial oldMat = getEffectiveMuscleMaterial();
      T newMat = (T)MaterialBase.updateMaterial (
         this, "muscleMaterial", myMuscleMat, mat);
      myMuscleMat = newMat;
      FemModel3d fem = getAncestorFem(this);
      if (fem != null) {
         // issue change event in case solve matrix symmetry or state has changed:
         MaterialChangeEvent mce = 
            MaterialBase.symmetryOrStateChanged (
               "muscleMaterial", newMat, oldMat);
         if (mce != null) {
            if (mce.stateChanged()) {
               notifyElementsOfMuscleMatStateChange();
            }
            notifyParentOfChange (mce);
         }      
         fem.invalidateStressAndStiffness();
         fem.invalidateRestData();
      }
      // return newMat
   }

   public void applyForce (double t) {
      if (myFibresActive) {
         for (int i=0; i<myFibres.size(); i++) {
            myFibres.get(i).applyForces (t);
         }
      }
   }
   
   /**
    * Apply forces due to fibres in bundle, consistent with force-effector
    * @param t time at which force is applied
    */
   public void applyForces(double t) {
      applyForce(t);
   }
   
   /**
    * Return Jacobian type for use as force effector
    * @return type code
    */
   public int getJacobianType() {
      int type = Matrix.SPD;
      if (myFibresActive) {
         for (Muscle m : myFibres) {
            type &= m.getJacobianType ();
         }
      }
      return type;
   }

   public void setMaxForce (double maxForce) {
      for (int i=0; i<myFibres.size(); i++) {
         Muscle mus = myFibres.get(i);
         if (mus.getMaterial() instanceof AxialMuscleMaterial) {
            AxialMuscleMaterial mat = 
               (AxialMuscleMaterial)mus.getMaterial().clone();
            mat.setMaxForce (maxForce);
            mus.setMaterial (mat);
         }
      }
   }
   
   public void addPosJacobian (SparseNumberedBlockMatrix M, double h) {
      if (myFibresActive) {
         for (int i=0; i<myFibres.size(); i++) {
            myFibres.get(i).addPosJacobian (M, h);
         }
      }
   }

   public void addVelJacobian (SparseNumberedBlockMatrix M, double h) {
      if (myFibresActive) {
         for (int i=0; i<myFibres.size(); i++) {
            myFibres.get(i).addVelJacobian (M, h);
         }
      }
   }

   public void addSolveBlocks (SparseNumberedBlockMatrix S) {
      if (myFibresActive) {
         for (int i=0; i<myFibres.size(); i++) {
            myFibres.get(i).addSolveBlocks (S);
         }
      }
   }

   void checkFibrePoints (FemModel femMod, Muscle fibre) {
      if (!ModelComponentBase.recursivelyContains (
             femMod, fibre.getFirstPoint())) {
         throw new IllegalArgumentException (
            "first fibre point not contained within FEM model");
      }
      if (!ModelComponentBase.recursivelyContains (
             femMod, fibre.getSecondPoint())) {
         throw new IllegalArgumentException (
            "second fibre point not contained within FEM model");
      }
   }

   public AxialSpringList<Muscle> getFibres() {
      return myFibres;
   }

   public Muscle addFibre (Point p0, Point p1, AxialMuscleMaterial mat) {
      
      Muscle fibre = new Muscle();
      fibre.setPoints(p0, p1);
      fibre.setRestLength(p0.distance(p1));
      fibre.setMaterial(mat);
      addFibre(fibre);
      
      return fibre;
      
   }
   
   public void addFibre (Muscle fibre) {
      // check to make sure particles are already in the FEM
      FemModel femMod = getAncestorFem(this);
      if (femMod != null) {
         checkFibrePoints (femMod, fibre);
      }
      myFibres.add (fibre);
   }

   public boolean removeFibre (Muscle fibre) {
      return myFibres.remove (fibre);
   }

   public void clearFibres() {
      myFibres.clear();
      notifyStructureChanged (this);
   }

   void checkElementDesc (FemModel femMod, MuscleElementDesc desc) {
      if (!ModelComponentBase.recursivelyContains (
             femMod, desc.getElement())) {
         throw new IllegalArgumentException (
            "desc element not contained within FEM model");
      }
   }

   public MuscleElementDescList getElements() {
      return myElementDescs;
   }

   public void addElement (MuscleElementDesc desc) {
      // check to make sure particles are already in the FEM
      FemModel femMod = getAncestorFem(this);
      if (femMod != null) {
         checkElementDesc (femMod, desc);
      }
      myElementDescs.add (desc);
   }

   public MuscleElementDesc addElement (FemElement3dBase elem, Vector3d dir) {
      MuscleElementDesc desc = new MuscleElementDesc(elem, dir);
      addElement(desc);
      return desc;
   }
   
   public MuscleElementDesc addElement(FemElement3dBase elem, Vector3d[] dirs) {
      MuscleElementDesc desc = new MuscleElementDesc();
      desc.setElement(elem);
      desc.setDirections(dirs);
      addElement(desc);
      return desc;
   }
   
   public boolean removeElement (MuscleElementDesc desc) {
      return myElementDescs.remove (desc);
   }

   public void clearElements() {
      myElementDescs.clear();
   }

   public static FemModel3d getAncestorFem (ModelComponent comp) {
      ModelComponent ancestor = comp.getParent();
      while (ancestor != null) {
         if (ancestor instanceof FemModel) {
            return (FemModel3d)ancestor;
         }
         ancestor = ancestor.getParent();
      }
      return null;
   }

   private void getRestPosition (Point3d pos, Point pnt) {
      if (pnt instanceof FemNode3d) {
         pos.set (((FemNode3d)pnt).getRestPosition());
      }
      else if (pnt instanceof FemMarker) {
         ((FemMarker)pnt).getRestPosition (pos);
      }
      else {
         throw new InternalErrorException (
            "Muscle attached to unsupported point type "+pnt.getClass());
      }
   }

   private double getFibreDist (
      Point3d pnt, Point3d loc, Vector3d dir, double len) {

      //      return loc.distance (pnt);
      Vector3d del = new Vector3d();
      del.sub (pnt, loc);
      double dot = del.dot(dir);
      if (dot > len/2) {
         del.scaledAdd (-len/2, dir);
      }
      else if (dot < -len/2) {
         del.scaledAdd (len/2, dir);
      }
      else {
         del.scaledAdd (-dot, dir);
      }
      return del.norm();
   }


   /** 
    * Returns a list of MuscleElementDesc identifying all elements that are (a)
    * within a specified distance of the fibres, and (b) not already referenced
    * by this bundle.
    */
   public LinkedList<MuscleElementDesc> getNewElementsNearFibres (double dist) {

      LinkedList<MuscleElementDesc> list =
         new LinkedList<MuscleElementDesc>();

      if (myFibres.size() == 0) {
         return list;
      }

      FemModel3d femMod = getAncestorFem(this);
      ArrayList<FemElement3dBase> elems = femMod.getAllElements();
      Point3d[] elemLocs = new Point3d[elems.size()];
      Point3d[] muscleLocs = new Point3d[myFibres.size()];
      Vector3d[] muscleDirs = new Vector3d[myFibres.size()];
      double[] muscleLens = new double[myFibres.size()];
      Point3d pos1 = new Point3d();
      Point3d pos2 = new Point3d();
      Vector3d dir = new Vector3d();

      for (int j=0; j<elems.size(); j++) {
         FemElement3dBase e = elems.get(j);
         elemLocs[j] = new Point3d();
         IntegrationPoint3d warpingPnt = e.getWarpingPoint();
         warpingPnt.computeRestPosition (elemLocs[j], e.getNodes());
      }
      
      for (int i=0; i<myFibres.size(); i++) {
         Muscle fibre = myFibres.get(i);
         getRestPosition (pos1, fibre.getFirstPoint());
         getRestPosition (pos2, fibre.getSecondPoint());
         muscleLocs[i] = new Point3d();
         muscleLocs[i].combine (0.5, pos2, 0.5, pos1);
         dir.sub (pos2, pos1);
         muscleLens[i] = dir.norm();
         dir.normalize ();
         muscleDirs[i] = new Vector3d(dir);
      }

      int[] nearestFibreIndex = new int[elems.size()];
      double[] nearestFibreDistance = new double[elems.size()];
      boolean[] bundleHasElement = new boolean[elems.size()];

      for (int j=0; j<elems.size(); j++) {
         nearestFibreIndex[j] = -1;
         nearestFibreDistance[j] = Double.MAX_VALUE;
      }
      for (int k=0; k<myElementDescs.size(); k++) {
         FemElement3dBase e = myElementDescs.get(k).getElement();
         bundleHasElement[elems.indexOf(e)] = true;
      }

      // for now, just use a very basic order n^2 algorithm, since
      // this method is not likely to be used much and is not
      // time critical
      for (int i=0; i<myFibres.size(); i++) {
         for (int j=0; j<elems.size(); j++) {
            double d = getFibreDist (
               elemLocs[j], muscleLocs[i], muscleDirs[i], muscleLens[i]);
            if (d < dist && d < nearestFibreDistance[j]) {
               nearestFibreDistance[j] = d;
               nearestFibreIndex[j] = i;
            }
         }
      }

      for (int j=0; j<elems.size(); j++) {
         if (!bundleHasElement[j] && nearestFibreIndex[j] != -1) {
            MuscleElementDesc desc = new MuscleElementDesc();
            desc.setElement (elems.get(j));
            desc.setDirection (muscleDirs[nearestFibreIndex[j]]);
            list.add (desc);
         }
      }
      return list;
   }    

   public void addElementsNearFibres (double dist) {

      LinkedList<MuscleElementDesc> list = getNewElementsNearFibres (dist);
      for (MuscleElementDesc desc : list) {
         addElement (desc);
      }
   }

   public void addFiberMeshElements (double rad, PolylineMesh mesh) {

      Point3d pos = new Point3d();
      Vector3d dir = new Vector3d();

      FemModel3d femMod = getAncestorFem(this);

      PolylineInterpolator interp = new PolylineInterpolator(mesh);
      for (FemElement3dBase e : femMod.getAllElements()) {
         IntegrationPoint3d[] pnts = e.getIntegrationPoints();
         boolean elemIsActive = false;
         Vector3d[] dirs = new Vector3d[pnts.length];
         for (int i = 0; i < pnts.length; i++) {
            pnts[i].computePosition(pos, e.getNodes());
           
            int nsegs = interp.computeAverageDirection(dir, pos, rad);

            if (nsegs > 0) {
               dirs[i] = new Vector3d (dir);
               elemIsActive = true;
            }
         }
         if (elemIsActive) {
            MuscleElementDesc desc = new MuscleElementDesc();
            desc.setElement (e);
            desc.setDirections (dirs);
            addElement (desc);
          }
      }
   }

   public DelaunayInterpolator getFibreRestDistanceInterpolator() {
      if (myFibres.size() == 0) {
         return null;
      }
      Point3d pos1 = new Point3d();
      Point3d pos2 = new Point3d();
      Point3d loc = new Point3d();

      Point3d[] muscleLocs = new Point3d[myFibres.size()];
      for (int i=0; i<myFibres.size(); i++) {
         Muscle fibre = myFibres.get(i);
         getRestPosition (pos1, fibre.getFirstPoint());
         getRestPosition (pos2, fibre.getSecondPoint());
         loc.add (pos2, pos1);
         loc.scale (1/2.0);
         muscleLocs[i] = new Point3d(loc);
      }
      DelaunayInterpolator interpolator = new DelaunayInterpolator();
      interpolator.setPoints (muscleLocs);     
      return interpolator;
   }

   public Vector3d[] getFibreRestDirections() {

      if (myFibres.size() == 0) {
         return null;
      }
      Point3d pos1 = new Point3d();
      Point3d pos2 = new Point3d();
      Vector3d dir = new Vector3d();

      Vector3d[] muscleDirs = new Vector3d[myFibres.size()];
      for (int i=0; i<myFibres.size(); i++) {
         Muscle fibre = myFibres.get(i);
         getRestPosition (pos1, fibre.getFirstPoint());
         getRestPosition (pos2, fibre.getSecondPoint());
         dir.sub (pos2, pos1);
         dir.normalize();
         muscleDirs[i] = new Vector3d(dir);
      }
      return muscleDirs;
   }
      

   /** 
    * Computes the directions within individual elements based on the
    * directions of the muscle fibres.
    */
   public void computeElementDirections () {

      DelaunayInterpolator interp = getFibreRestDistanceInterpolator();
      if (interp != null) {
         Vector3d[] restDirs = getFibreRestDirections();
         for (MuscleElementDesc desc : myElementDescs) {
            desc.interpolateDirection (interp, restDirs);
         }
      }
   }

   /** 
    * Computes the directions within individual elements based on the
    * directions of the muscle fibres.
    */
   public void computeElementIpntDirections () {

      DelaunayInterpolator interp = getFibreRestDistanceInterpolator();
      if (interp != null) {
         Vector3d[] restDirs = getFibreRestDirections();
         for (MuscleElementDesc desc : myElementDescs) {
            desc.interpolateIpntDirection (interp, restDirs);
         }
      }
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createLineFaceProps (this);
   }

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public void setRenderProps (RenderProps props) {
      myRenderProps = 
         RenderableComponentBase.updateRenderProps (this, myRenderProps, props);
   }

   public void prerender (RenderList list) {
      list.addIfVisible (myFibres);
      list.addIfVisible (myElementDescs);
   }

   public void render (Renderer renderer, int flags) {
      // Old code from AxialSpringList
      //renderer.drawLines (myRenderProps, iterator());
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return false;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
   }

   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.isTransparent()) {
         code |= TRANSPARENT;
      }
      return code;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public void scaleDistance (double s) {
      for (int i=0; i<myFibres.size(); i++) {
         myFibres.get(i).scaleDistance (s);
      }
      for (int i=0; i<myElementDescs.size(); i++) {
         myElementDescs.get(i).scaleDistance (s);
      }
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
      if (myMuscleMat != null) {
         myMuscleMat.scaleDistance (s);
      }
      myDirectionRenderLen *= s;
   }

   public void scaleMass (double s) {
      for (int i=0; i<myFibres.size(); i++) {
         myFibres.get(i).scaleMass (s);
      }
      for (int i=0; i<myElementDescs.size(); i++) {
         myElementDescs.get(i).scaleMass (s);
      }
   }

   /*
    * XXX - old code from MuscleBundle for organizing fibers
    */
   
   ArrayList<LinkedList<Muscle>> myFascicles =
      new ArrayList<LinkedList<Muscle>> ();
   
   public void addFascicle (LinkedList<Muscle> fascicle) {
      myFascicles.add (fascicle);
   }

   public ArrayList<LinkedList<Muscle>> getFascicles () {
      return myFascicles;
   }
   
   public void clearFascicles() {
      myFascicles.clear();
   }

   public void transformGeometry(AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      // nothing to at the top level
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      context.addAll (myElementDescs);
   } 

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
   
   public void propertyChanged (PropertyChangeEvent e) {
      if (e instanceof MaterialChangeEvent) {
         FemModel fem = getAncestorFem(this);
         if (fem != null) {
            fem.invalidateStressAndStiffness();
            if (e.getHost() instanceof FemMaterial && 
                ((FemMaterial)e.getHost()).isLinear()) {
               // invalidate rest data for linear materials, to rebuild
               // the initial warping stiffness matrices
               fem.invalidateRestData();
            }
            MaterialChangeEvent mce = (MaterialChangeEvent)e;
            if (mce.stateChanged() && e.getHost() == getMuscleMaterial()) {
               notifyElementsOfMuscleMatStateChange();
            }
            if (mce.stateOrSymmetryChanged()) {
               notifyParentOfChange (new MaterialChangeEvent (this, mce));  
            }
         }
      }      
   }

}
