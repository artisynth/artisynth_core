/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import maspack.geometry.DelaunayInterpolator;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.Point3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer.Shading;
import maspack.render.GL.GLSupport;
import maspack.util.ArraySort;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.femmodels.MuscleBundle.DirectionRenderType;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.MaterialBase;
import artisynth.core.materials.MuscleMaterial;
import artisynth.core.materials.SolidDeformation;
import artisynth.core.mechmodels.ExcitationComponent;
import artisynth.core.mechmodels.ExcitationSourceList;
import artisynth.core.mechmodels.ExcitationUtils;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CompositeComponentBase;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.*;

/**
 * A class wrapping the description of each FEM element belonging to a
 * MuscleBundle. It implements the AuxiliaryMaterial required to effect muscle
 * activation within the element, and contains the element-specific muscle
 * direction information.
*/
public class MuscleElementDesc
   extends RenderableComponentBase
   implements AuxiliaryMaterial, ExcitationComponent, ScalableUnits, TransformableGeometry {

   FemElement3d myElement;
   private MuscleMaterial myMuscleMat;
   Vector3d myDir = new Vector3d();
   Vector3d[] myDirs = null;
   private double myExcitation = 0;
   protected ExcitationSourceList myExcitationSources;
   protected CombinationRule myComboRule = CombinationRule.Sum;

   // the following are set if an activation color is specified:
   protected float[] myDirectionColor; // render color for directions
   protected float[] myWidgetColor; // render color for elements
 
   // minimum activation level
   protected static final double minActivation = 0.0;
   // maximum activation level
   protected static final double maxActivation = 1.0;

   public MuscleElementDesc () {
      super();
   }

   public MuscleElementDesc (FemElement3d elem, Vector3d dir) {
      this();
      setElement (elem);
      if (dir != null) {
         setDirection (dir);
      }
   }

   public static PropertyList myProps =
      new PropertyList (MuscleElementDesc.class, RenderableComponentBase.class);

   static {
      myProps.add ("renderProps", "render properties", null);
      myProps.add ("direction", "fibre direction", Vector3d.ZERO);
      myProps.addReadOnly (
         "netExcitation", "total excitation including excitation sources");
      myProps.add ("excitation", "internal muscle excitation", 0.0, "[0,1] NW");
      myProps.add (
         "muscleMaterial", "muscle material parameters", null);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
     
   public void setDirection (Vector3d dir) {
      myDir.set (dir);
      myDir.normalize();
   }

   public Vector3d getDirection() {
      return myDir;
   }

   /**
    * Sets a list of directions for this MuscleElementDesc, one for each
    * integration point in the element. This will override the single
    * per-element direction specified by {@link #setDirection}.  If a
    * particular direction is <code>null</code>, then no stress will be applied
    * at the corresponding integration point. Supplying a <code>null</code>
    * value for <code>dirs</code> will disable per-integration point
    * directions.
    */
   public void setDirections (Vector3d[] dirs) {
      if (dirs == null) {
         myDirs = null;
      }
      else {
         myDirs = new Vector3d[dirs.length];
         for (int i=0; i<dirs.length; i++) {
            if (dirs[i] != null) {
               if (dirs[i].equals (Vector3d.ZERO)) {
                  throw new IllegalArgumentException (
                     "direction vector "+i+" is zero");
               }
               myDirs[i] = new Vector3d (dirs[i]);
               myDirs[i].normalize();
            }
         }
      }
   }

   public Vector3d[] getDirections() {
      return myDirs;
   }

   public MuscleMaterial getMuscleMaterial() {
      return myMuscleMat;
   }

   public void setMuscleMaterial (MuscleMaterial mat) {
      myMuscleMat = (MuscleMaterial)MaterialBase.updateMaterial (
         this, "muscleMaterial", myMuscleMat, mat);
      // issue DynamicActivityChange in case solve matrix symmetry has changed:
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);            
   }

   public boolean isInvertible() {
      MuscleMaterial mat = getEffectiveMuscleMaterial();
      return mat == null || mat.isInvertible();
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
      // set activation within valid range
      double valid_a = a;
      valid_a = (valid_a > maxActivation) ? maxActivation : valid_a;
      valid_a = (valid_a < minActivation) ? minActivation : valid_a;
      myExcitation = valid_a;
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
   public void addExcitationSource (ExcitationComponent ex) {
      addExcitationSource (ex, 1);
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
      double net = ExcitationUtils.combineWithAncestor (
         this, myExcitationSources, /*up to grandparent=*/2, myComboRule);
      return net;
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

   /**
    * {@inheritDoc}
    */
   public double getDefaultActivationWeight() {
      return 0;
   }
   
   public void updateBounds(Point3d pmin, Point3d pmax) {
      super.updateBounds(pmin, pmax);
      if (myElement != null)
	 myElement.updateBounds(pmin, pmax);
   }
   
   void setExcitationColors (RenderProps props) {
      ModelComponent gparent = getGrandParent();
      if (gparent instanceof MuscleBundle) {
         MuscleBundle bundle = (MuscleBundle)gparent;
         float[] excitationColor = bundle.myExcitationColor;
         if (excitationColor != null) {
            double s =
               Math.min(getNetExcitation()/bundle.myMaxColoredExcitation, 1);
            float[] baseColor;
            if (myDirectionColor == null) {
               myDirectionColor = new float[4];
            }
            myDirectionColor[3] = (float)props.getAlpha ();
            baseColor = props.getLineColorArray();
            GLSupport.interpolateColor (
               myDirectionColor, baseColor, excitationColor, s);
            if (myWidgetColor == null) {
               myWidgetColor = new float[4];
            }
            baseColor = props.getFaceColorArray();
            GLSupport.interpolateColor (
               myWidgetColor, baseColor, excitationColor, s);
            myWidgetColor[3] = (float)props.getAlpha ();
            
         }
         else {
            myDirectionColor = null;
            myWidgetColor = null;
         }
      }
   }

   public void prerender (RenderList list) {
      // This is to ensure that the invJ0 in the warping data is updated in the
      // current (simulation) thread.
      myElement.getWarpingData();
      setExcitationColors (myRenderProps);
   }
   

   protected void renderINodeDirection(Renderer renderer, RenderProps props,
      float[] coords0, float[] coords1, Matrix3d F, Vector3d dir, double len) {
      
      IntegrationPoint3d[] ipnt = myElement.getIntegrationPoints();
      IntegrationData3d[] idata = myElement.getIntegrationData();   
      
      for (int i=0; i<ipnt.length; i++) {
      
         boolean drawLine = true;
         if (myDirs != null) {
            if (myDirs[i] != null) {
               dir.set(myDirs[i]);
            } else {
               drawLine = false;
               dir.set(0,0,0);
            }
         } else {
            dir.set(myDir);
         }

         double size = myElement.computeDirectedRenderSize (dir);
         
         if (drawLine) {
            ipnt[i].computeGradientForRender(F, myElement.getNodes(), idata[i].myInvJ0);
            ipnt[i].computeCoordsForRender(coords0, myElement.getNodes());
            F.mul(dir,dir);
            dir.scale(0.5*size);
            dir.scale(len);
            
            coords0[0] -= (float)dir.x / 2;
            coords0[1] -= (float)dir.y / 2;
            coords0[2] -= (float)dir.z / 2;
            coords1[0] = coords0[0] + (float)dir.x;
            coords1[1] = coords0[1] + (float)dir.y;
            coords1[2] = coords0[2] + (float)dir.z;
            
            renderer.drawLine(
               props, coords0, coords1, myDirectionColor,
               /*capped=*/true, /*selected=*/false);   
         }
      }
      
   }
   
   protected void renderElementDirection(Renderer renderer, RenderProps props,
      float[] coords0, float[] coords1, Matrix3d F, Vector3d dir, double len) {
      
      myElement.computeRenderCoordsAndGradient (F, coords0);

      if (myDirs != null) {
         // If there are directions for each integration point, use the average
         // direction of all the dirs.
         dir.setZero();
         for (int i=0; i<myDirs.length; i++) {
            if (myDirs[i] != null) {
               dir.add (myDirs[i]);
            }
         }
         dir.normalize();
         F.mul (dir, dir);
      }
      else {
         F.mul (dir, myDir);
      }

      double size = myElement.computeDirectedRenderSize (dir);      
      dir.scale (0.5*size);
      dir.scale(len);
            
      coords0[0] -= (float)dir.x/2;
      coords0[1] -= (float)dir.y/2;
      coords0[2] -= (float)dir.z/2;
            
      coords1[0] = coords0[0] + (float)dir.x;
      coords1[1] = coords0[1] + (float)dir.y;
      coords1[2] = coords0[2] + (float)dir.z;
            
      renderer.drawLine (
         props, coords0, coords1, myDirectionColor, 
         /*capped=*/true, isSelected());
      
   }
   
   void renderDirection (
      Renderer renderer, RenderProps props,
      float[] coords0, float[] coords1, Matrix3d F, Vector3d dir, double len, DirectionRenderType type) {

      
      switch(type) {
         case ELEMENT:
            renderElementDirection(renderer, props, coords0, coords1, F, dir, len);
            break;
         case INTEGRATION_POINT:
            renderINodeDirection(renderer, props, coords0, coords1, F, dir, len);
            break;
      }
      
   }
      
   public void render (Renderer renderer, int flags) {
      render (renderer, myRenderProps, flags);
   }   

   public void render (Renderer renderer, RenderProps props, int flags) {
      double widgetSize = 0;
      double directionLength = 0;
      ModelComponent gparent = getGrandParent();
      DirectionRenderType renderType = DirectionRenderType.ELEMENT;
      
      if (gparent instanceof MuscleBundle) {
         MuscleBundle bundle = (MuscleBundle)gparent;
         widgetSize = bundle.getElementWidgetSize();
         directionLength = bundle.getDirectionRenderLen();
         renderType = bundle.getDirectionRenderType();
      }      
      if (widgetSize != 0) {
         Shading savedShading = renderer.setPropsShading (props);
         renderer.setFaceColoring (props, myWidgetColor, isSelected());
         myElement.renderWidget (renderer, widgetSize, props);
         renderer.setShading (savedShading);
      }
      if (directionLength > 0) {
         Matrix3d F = new Matrix3d();
         Vector3d dir = new Vector3d();
         float[] coords0 = new float[3];
         float[] coords1 = new float[3]; 

         renderDirection (
            renderer, props, coords0, coords1, F, dir,
            directionLength, renderType);
      }
   }

   private MuscleMaterial getEffectiveMuscleMaterial () {
      if (myMuscleMat != null) {
         return myMuscleMat;
      }
      else {
         ModelComponent gparent = getGrandParent();
         if (gparent instanceof MuscleBundle) {
            return ((MuscleBundle)gparent).getEffectiveMuscleMaterial();
         }
      }
      return null;      
   }

//   public void addTangent (
//      Matrix6d D, SymmetricMatrix3d stress, IntegrationPoint3d pt, 
//      IntegrationData3d dt, FemMaterial baseMat) {
//      
//      MuscleMaterial mat = getEffectiveMuscleMaterial();
//      if (mat != null) {
//         Vector3d dir = null;
//         if (myDirs != null) {
//            dir = myDirs[pt.getNumber()];
//         }
//         else {
//            dir = myDir;
//         }
//         if (dir != null) {
//            mat.addTangent (D, stress, getNetExcitation(), dir, pt, baseMat);
//         }
//      }
//   }
   
   public void computeTangent (
      Matrix6d D, SymmetricMatrix3d stress,
      SolidDeformation def, IntegrationPoint3d pt, IntegrationData3d dt, FemMaterial baseMat) {

      MuscleMaterial mat = getEffectiveMuscleMaterial();
      if (mat != null) {
         Vector3d dir = null;
         if (myDirs != null) {
            dir = myDirs[pt.getNumber()];
         }
         else {
            dir = myDir;
         }
         if (dir != null) {
            mat.computeTangent (D, stress, getNetExcitation(), dir, def, baseMat);
         }
      }
   }
  
//   public void addStress (
//      SymmetricMatrix3d sigma, IntegrationPoint3d pt, 
//      IntegrationData3d dt, FemMaterial baseMat) {
//      
//      MuscleMaterial mat = getEffectiveMuscleMaterial();
//      if (mat != null) {
//         Vector3d dir = null;
//         if (myDirs != null) {
//            dir = myDirs[pt.getNumber()];
//         }
//         else {
//            dir = myDir;
//         }
//         if (dir != null) {
//            mat.addStress (sigma, getNetExcitation(), dir, pt, baseMat);
//         }
//      }
//   }
// 

 
   // @Override
   // public void computeStress (
   //    SymmetricMatrix3d sigma, SolidDeformation def,
   //    Matrix3d Q, FemMaterial baseMat) {
      
   //    sigma.setZero();
   //    MuscleMaterial mat = getEffectiveMuscleMaterial();
   //    if (mat != null) {
   //       Vector3d dir = null;
   //       if (myDirs != null) {
   //          dir = myDirs[pt.getNumber()];
   //       }
   //       else {
   //          dir = myDir;
   //       }
   //       if (dir != null) {
   //          mat.addStress (sigma, getNetExcitation(), dir, pt, baseMat);
   //       }
   //    }
   // }
   
   public void computeStress (
      SymmetricMatrix3d sigma, SolidDeformation def,
      IntegrationPoint3d pt, IntegrationData3d dt, FemMaterial baseMat) {
      
      MuscleMaterial mat = getEffectiveMuscleMaterial();
      if (mat != null) {
         Vector3d dir = null;
         if (myDirs != null) {
            dir = myDirs[pt.getNumber()];
         }
         else {
            dir = myDir;
         }
         if (dir != null) {
            mat.computeStress (sigma, getNetExcitation(), dir, def, baseMat);
         }
      }
   }
   
//   private SymmetricMatrix3d tmpStress = new SymmetricMatrix3d();
//   @Override
//   public void addStressAndTangent(SymmetricMatrix3d sigma, Matrix6d D,
//      IntegrationPoint3d pt, IntegrationData3d dt, FemMaterial baseMat) {
//      computeStress(tmpStress, pt, dt, baseMat);
//      sigma.add(tmpStress);
//      addTangent(D, tmpStress, pt, dt, baseMat);
//   }
   
//   @Override
//   public void computeStressAndTangent(SymmetricMatrix3d sigma, Matrix6d D,
//      IntegrationPoint3d pt, IntegrationData3d dt, FemMaterial baseMat) {
//      computeStress(sigma, pt, dt, baseMat);
//      computeTangent(D, sigma, pt, dt, baseMat);
//   }

   public boolean hasSymmetricTangent() {
      MuscleMaterial mat = getEffectiveMuscleMaterial();
      if (mat != null) {
         return mat.hasSymmetricTangent();
      }
      else {
         return true;
      }
   }

   // public void getDependencies (
   //    List<ModelComponent> deps, ModelComponent ancestor) {
   //    super.getDependencies (deps, ancestor);
   //    // TODO: see why these are NOT included in Muscle code:
   //    if (myExcitationSources != null) {
   //       for (ExcitationComponent ex : myExcitationSources) {
   //          if (ex != getGrandParent()) {
   //             ComponentUtils.addDependencies (deps, ex, ancestor);
   //          }
   //       }
   //    }
   // }

   public void interpolateDirection (
      DelaunayInterpolator interp, Vector3d[] restDirs) {

      int[] idxs = new int[4];
      double[] wghts = new double[4];
      Vector3d dir = new Vector3d();
      Point3d loc = new Point3d();

      FemElement3d e = getElement();
      IntegrationPoint3d warpingPnt = e.getWarpingPoint();
      warpingPnt.computeRestPosition (loc, e.getNodes());
      interp.getInterpolation (wghts, idxs, loc);

      // Ideally, we just want to create a weighted average of directions.  But
      // there is a problem: we don't care about the *sign* of the direction,
      // in that -u is the same as u. But the sign can make a huge difference
      // when we average: (u + (-u)) != (u + u). 
      //
      // We handle this as follows: when we add a new direction, we adjust its
      // sign so that it is as closely aligned as possble with the accumulated
      // direction. We also start accumulating directions starting with those
      // that have the largest weight.
      //
      // This is probably not an "optimal" solution - will try to figure
      // this out more correctly later.

      // arrange weights into ascending order
      ArraySort.quickSort (wghts, idxs);
      dir.setZero();
      for (int i=3; i>=0; i--) {
         if (idxs[i] != -1) {
            double w = wghts[i];
            // first time through, dir == 0 and so dot product == 0.
            if (dir.dot (restDirs[idxs[i]]) < 0) {
               w = -w;
            }
            dir.scaledAdd (w, restDirs[idxs[i]]);
         }
      }
      setDirection (dir);
   }

   public FemElement3d getElement() {
      return myElement;
   }

   public void setElement (FemElement3d elem) {
      myElement = elem;
   }
   
   public void transformGeometry(AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // Transform the direction vectors associated with this ElementDesc.
      Point3d ref = new Point3d();
      if (gtr.isAffine()) {
         gtr.transformVec (myDir, ref);
         myDir.normalize();
         if (myDirs != null) {
            for (int i=0; i<myDirs.length; i++) {
               if (myDirs[i] != null) {
                  gtr.transformVec (myDirs[i], ref);
                  myDirs[i].normalize();
               }
            }
         }
      }
      else {
         // need to specify a reference position for each direction, since
         // vector transformations are position dependent.

         // compute element center as a reference position:
         myElement.getWarpingPoint().computePosition (ref, myElement);         
         gtr.transformVec(myDir, ref);
         myDir.normalize();
         if (myDirs != null) {
            IntegrationPoint3d[] ipnts = myElement.getIntegrationPoints();
            for (int i=0; i<myDirs.length; i++) {
               if (myDirs[i] != null) {
                  // compute integration point location as a reference position:
                  ipnts[i].computePosition (ref, myElement);
                  gtr.transformVec (myDirs[i], ref);
                  myDirs[i].normalize();
               }
            }
         }
      }
   }
   
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencies
   }

   public void scaleDistance (double s) {
      if (myMuscleMat != null) {
         myMuscleMat.scaleDistance (s);
      }
      if (myRenderProps != null) {
         myRenderProps.scaleDistance (s);
      }
   }

   public void scaleMass (double s) {
      if (myMuscleMat != null) {
         myMuscleMat.scaleMass (s);
      }
   }

   void referenceElement() {
      myElement.addAuxiliaryMaterial (this);
      //myElement.addBackReference (this);
   }

   void dereferenceElement() {
      //myElement.removeBackReference (this);
      myElement.removeAuxiliaryMaterial (this);
   }
   
   @Override
      public void connectToHierarchy () {
      super.connectToHierarchy ();
      if (MuscleBundle.getAncestorFem (this) != null) {
         referenceElement();
      }
      //ExcitationUtils.addAncestorAsSource (this, /*up to grandparent*/2);
   }

   @Override
      public void disconnectFromHierarchy() {
      //ExcitationUtils.removeAncestorAsSource (this, /*up to grandparent*/2);
      if (MuscleBundle.getAncestorFem (this) != null) {
         dereferenceElement();
      }
      super.disconnectFromHierarchy();
   }
 
   void scanDirections (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken ('[');
      LinkedList<Vector3d> directions = new LinkedList<Vector3d>();
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
            directions.add (null);
         }
         else if (rtok.tokenIsNumber()) {
            rtok.pushBack();
            double x = rtok.scanNumber();
            double y = rtok.scanNumber();
            double z = rtok.scanNumber();
            directions.add (new Vector3d(x, y, z));
         }
         else {
            throw new IOException ("Expected null or number, "+rtok);
         }
      }
      setDirections (directions.toArray(new Vector3d[0]));
   }

   void printDirections (PrintWriter pw, NumberFormat fmt) {
      pw.println ("directions=[");
      IndentingPrintWriter.addIndentation (pw, 2);
      
      for (int i=0; i<myDirs.length; i++) {
         if (myDirs[i] != null) {
            pw.println (myDirs[i].toString (fmt));
         }
         else {
            pw.println ("null");
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public void printElementReference (PrintWriter pw, CompositeComponent ancestor)
      throws IOException {
      pw.print ("element=" +
                ComponentUtils.getWritePathName (ancestor, myElement));
   }

   @Override
   public boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "element", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "directions")) {
         scanDirections (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "excitationSources")) {
         myExcitationSources =
            ExcitationUtils.scan (rtok, "excitationSources", tokens);
         return true;
      }      
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }   

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "element")) {
         setElement (postscanReference (tokens, FemElement3d.class, ancestor));
         return true;
      }
      else if (postscanAttributeName (tokens, "excitationSources")) {
         myExcitationSources.postscan (tokens, ancestor);
         return true;
      }  
      return super.postscanItem (tokens, ancestor);
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      printElementReference (pw, ancestor);
      pw.println ("");
      if (myDirs != null && myDirs.length > 0) {
         printDirections (pw, fmt);
      }
      if (myExcitationSources != null) {
         myExcitationSources.write (pw, "excitationSources", fmt, ancestor);
      }      
   }

}
