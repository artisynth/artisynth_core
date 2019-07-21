/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import maspack.matrix.Matrix;
import maspack.matrix.MatrixBlock;
import maspack.matrix.Vector3d;
import maspack.render.*;
import maspack.render.Renderer.LineStyle;
import maspack.render.color.ColorUtils;
import maspack.properties.*;
import maspack.properties.PropertyInfo.Edit;
import maspack.spatialmotion.Wrench;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.util.ScanToken;
import artisynth.core.materials.AxialMaterial;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.materials.AxialMuscleMaterial;
import artisynth.core.materials.ConstantAxialMuscle;
import artisynth.core.materials.LinearAxialMaterial;
import artisynth.core.materials.LinearAxialMuscle;
import artisynth.core.materials.PaiAxialMuscle;
import artisynth.core.materials.PeckAxialMuscle;
import artisynth.core.mechmodels.PointSpringBase.SegmentData;
import artisynth.core.modelbase.*;

public class MultiPointMuscle extends MultiPointSpring implements ExcitationComponent {

   protected ExcitationSourceList myExcitationSources;
   protected CombinationRule myComboRule = CombinationRule.Sum;

   // muscle type determines force-length characteristics
   protected boolean enabled = true;
   private static final Color disabledLineColor = Color.LIGHT_GRAY;
   private static final LineStyle disabledLineStyle = LineStyle.LINE;
   private Color enabledLineColor = null;
   private LineStyle enabledLineStyle = null;
   
   protected double myExcitation; // default = 0.0;

   // minimum activation level
   protected static final double minActivation = 0.0;
   // maximum activation level
   protected static final double maxActivation = 1.0;

   protected MatrixBlock myActBlk0;
   protected MatrixBlock myActBlk1;
   protected Wrench tmpBodyWrench = new Wrench();

   protected float[] myExcitationColor = null;
   protected PropertyMode myExcitationColorMode = PropertyMode.Inherited;
   protected double myMaxColoredExcitation = 1.0;
   protected PropertyMode myMaxColoredExcitationMode = PropertyMode.Inherited;

   protected float[] myRenderColor = null;

   public static PropertyList myProps =
      new PropertyList (MultiPointMuscle.class, MultiPointSpring.class);

   public MultiPointMuscle() {
      this (null);
   }
   
   public MultiPointMuscle(String name) {
      super (name);
   }

   public MultiPointMuscle (
      String name, double k, double d, double maxf, double l) {
      this (name);
      setRestLength (l);
      setMaterial (new SimpleAxialMuscle (k, d, maxf));
   }

   public static MultiPointMuscle createConstant (double maxForce) {
      MultiPointMuscle m = new MultiPointMuscle();
      ConstantAxialMuscle mat = new ConstantAxialMuscle();
      mat.setMaxForce (maxForce);
      m.setMaterial(mat);
      return m;
   }

   public static MultiPointMuscle createConstant() {
      MultiPointMuscle m = new MultiPointMuscle();
      m.setMaterial(new ConstantAxialMuscle());
      return m;
   }

   public static MultiPointMuscle createLinear (double maxForce, double maxLen) {
      MultiPointMuscle m = new MultiPointMuscle();
      LinearAxialMuscle mat = new LinearAxialMuscle();
      mat.setMaxForce (maxForce);
      mat.setMaxLength (maxLen);
      m.setMaterial(mat);
      return m;
   }

   public static MultiPointMuscle createLinear() {
      MultiPointMuscle m = new MultiPointMuscle();
      m.setMaterial(new LinearAxialMuscle());
      return m;
   }

   public static MultiPointMuscle createPai (
      double maxForce, double optLen, double maxLen, double ratio) {
      MultiPointMuscle m = new MultiPointMuscle();
      PaiAxialMuscle mat = new PaiAxialMuscle();
      mat.setMaxForce (maxForce);
      mat.setOptLength (optLen);
      mat.setMaxLength (maxLen);
      mat.setTendonRatio (ratio);
      m.setMaterial(mat);
      return m;
   }

   public static MultiPointMuscle createPai() {
      MultiPointMuscle m = new MultiPointMuscle();
      m.setMaterial(new PaiAxialMuscle());
      return m;
   }

   public static MultiPointMuscle createPeck (
      double maxForce, double optLen, double maxLen, double ratio) {
      MultiPointMuscle m = new MultiPointMuscle();
      PeckAxialMuscle mat = new PeckAxialMuscle();
      mat.setMaxForce (maxForce);
      mat.setOptLength (optLen);
      mat.setMaxLength (maxLen);
      mat.setTendonRatio (ratio);
      mat.setPassiveFraction(0.015); //Peck value = 0.0115
      m.setMaterial(mat);
      return m;
   }

   public static MultiPointMuscle createPeck (
      String name, double maxForce, double optLen, double maxLen, double ratio) {
      MultiPointMuscle m = createPeck (maxForce, optLen, maxLen, ratio);
      m.setName (name);
      return m;
   }

   public static MultiPointMuscle createPeck() {
      MultiPointMuscle m = new MultiPointMuscle();
      m.setMaterial(new PeckAxialMuscle());
      return m;
   }

   static {
      myProps.add ("enabled isEnabled *", "muscle is enabled", true);
      myProps.add ("excitation", "internal muscle excitation", 0.0, "[0,1] NW");
      myProps.addReadOnly (
         "netExcitation", "total excitation including excitation sources");
      myProps.addReadOnly (
         "forceNorm *", "norm of total force applied by muscle (N)", "%.8g AE");
      myProps.addReadOnly (
         "passiveForceNorm *", "norm of passive force generated by muscle (N)",
         "%.8g AE");
//      myProps.add (
//         "maxForce * *", "maximum force applied by muscle", AxialMuscleMaterial.DEFAULT_MAX_FORCE);
//      myProps.add ("optLength * *", "length for max force capacity", AxialMuscleMaterial.DEFAULT_OPT_LENGTH, "%.8g");
//      myProps.add ("maxLength * *", "max length of muscle stretch", AxialMuscleMaterial.DEFAULT_MAX_LENGTH, "%.8g");
//      myProps.add ("tendonRatio * *", "tendon to fibre length ratio", AxialMuscleMaterial.DEFAULT_TENDON_RATIO, "%.8g");
//      myProps.add (
//         "passiveFraction * *", "percentage of maxForce applied passively",
//         AxialMuscleMaterial.DEFAULT_PASSIVE_FRACTION, "%.8g");
//      myProps.add (
//         "forceScaling * *", "scale factor from nominal force units",
//         AxialMuscleMaterial.DEFAULT_SCALING, "%.8g");
      myProps.get ("length").setFormat (new NumberFormat ("%.8g"));

      // set unused props from AxialSpring to never edit
      myProps.get ("restLength").setEditing (Edit.Never);
      //myProps.get ("stiffness").setEditing (Edit.Never);
      myProps.addInheritable (
         "excitationColor", "color of activated muscles", null);
       myProps.addInheritable (
         "maxColoredExcitation",
         "excitation value for maximum colored excitation", 1.0, "[0,1]");
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


   public PropertyList getAllPropertyInfo() {
      return myProps;
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
   public void initialize (double t) {
      if (t == 0) {
         setExcitation (0);         
      }
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

   public float[] getRenderColor() {
      return myRenderColor;
   }

   @Override
   public void prerender (RenderList list) {
      RenderProps props = myRenderProps;
      if (props == null) {
         if (getParent() instanceof Renderable) {
            props = ((Renderable)getParent()).getRenderProps();
         }
      }
      if (props != null && myExcitationColor != null) {
         if (myRenderColor == null) {
            myRenderColor = new float[3];
         }
         float[] baseColor = props.getLineColorF();
         double s = Math.min(getNetExcitation()/getMaxColoredExcitation(), 1);
         ColorUtils.interpolateColor (
            myRenderColor, baseColor, myExcitationColor, s);
      }
      else {
         myRenderColor = null;
      }
      super.prerender (list);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public double getNetExcitation() {
      return ExcitationUtils.combine (
         myExcitation, myExcitationSources, myComboRule);
   }

   /**
    * Computes the force magnitude acting along the unit vector from the first
    * to the second particle.
    * 
`    * @return force magnitude
    */
   public double computeF (double l, double ldot) {
      AxialMaterial mat = getEffectiveMaterial();
      if (enabled && mat != null) {
         return mat.computeF (l, ldot, myRestLength, getNetExcitation());
      }
      else {
         return 0;
      }
   }
   
   /**
    * Computes the force magnitude acting along the unit vector from the first
    * to the second particle with zero excitation.
    * 
    * @return force magnitude
    */
   public double computePassiveF (double l, double ldot) {
      AxialMaterial mat = getEffectiveMaterial();
      if (enabled && mat != null) {
	 return mat.computeF(l, ldot, myRestLength, 0);
      }
      else {
	 return 0;
      }
   }

   /**
    * Computes the derivative of spring force magnitude (acting along the unit
    * vector from the first to the second particle) with respect to spring
    * length.
    * 
    * @return force magnitude derivative with respect to length
    */
   public double computeDFdl (double l, double ldot) {
      AxialMaterial mat = getEffectiveMaterial();
      if (enabled && mat != null) {
         return mat.computeDFdl (l, ldot, myRestLength, getNetExcitation());
      }
      else {
         return 0;
      }
   }



   /**
    * Computes the derivative of spring force magnitude (acting along the unit
    * vector from the first to the second particle)with respect to the time
    * derivative of spring length.
    * 
    * @return force magnitude derivative with respect to length time derivative
    */
   public double computeDFdldot (double l, double ldot) {
      AxialMaterial mat = getEffectiveMaterial();
      if (enabled && mat != null) {
         return mat.computeDFdldot (l, ldot, myRestLength, getNetExcitation());
      }
      else {
         return 0;
      }
   }


   /**
    * sets the opt length to current muscle length and max length with the
    * original ratio of opt to max length
    * 
    */
   public void resetLengthProps() {
      if (myMaterial instanceof AxialMuscleMaterial) {
	 AxialMuscleMaterial mat = (AxialMuscleMaterial)myMaterial;
	 double length = getLength();
	 double optLength = mat.getOptLength();
	 double maxLength = mat.getMaxLength();
	 double maxOptRatio =
	       (optLength != 0.0) ? maxLength / optLength : 1.0;
	 mat.setOptLength(length);
	 mat.setMaxLength(length * maxOptRatio); 

      }
      else {
         System.err.println("resetLengthProps(), current material not AxialMuscleMaterial");
      }
   }

   public double getForceNorm() {
      updateSegsIfNecessary();
      double len = getActiveLength();
      double dldt = getActiveLengthDot();
      return computeF (len, dldt);
   }
   
   public double getPassiveForceNorm() {
      updateSegsIfNecessary();
      double len = getActiveLength();
      double dldt = getActiveLengthDot();
      return computePassiveF (len, dldt);
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
//      forceScaling *= s;
//      myOptLength *= s;
//      myMaxLength *= s;
   }

   public void scaleMass (double s) {
      super.scaleMass (s);
//      forceScaling *= s;
   }

   public int getJacobianType() {
      return Matrix.SYMMETRIC;
   }

   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled (boolean enabled) {
      if (this.enabled != enabled) {
	 this.enabled = enabled;
	 updateLineRenderProps(enabled);
      }
   }
   
   private void updateLineRenderProps(boolean enabled) {
      if (enabled) {
	 if (enabledLineColor == null)
	    RenderProps.setLineColorMode(this, PropertyMode.Inherited);
	 else
	    RenderProps.setLineColor(this, enabledLineColor);
	 
	 if (enabledLineStyle == null) 
	    RenderProps.setLineStyleMode(this, PropertyMode.Inherited);
	 else
	    RenderProps.setLineStyle(this, enabledLineStyle);	 
      }
      else {
	 if (getRenderProps() != null && 
	     getRenderProps().getLineColorMode() == PropertyMode.Explicit)
	    enabledLineColor = getRenderProps().getLineColor();
	 
	 if (getRenderProps() != null && 
	     getRenderProps().getLineStyleMode() == PropertyMode.Explicit) 
	    enabledLineStyle = getRenderProps().getLineStyle();
	 
	 RenderProps.setLineColor(this, disabledLineColor);
	 RenderProps.setLineStyle(this, disabledLineStyle);
      }
	 
   }


//   public double getMaxForce() {
//      if (myMaterial instanceof AxialMuscleMaterial) {
//         return ((AxialMuscleMaterial)myMaterial).getMaxForce();
//      }
//      else {
//         return 0;
//      }
//   }
//
//   public void setMaxForce (double f) {
//      if (myMaterial instanceof AxialMuscleMaterial) {
//         ((AxialMuscleMaterial)myMaterial).setMaxForce(f);
//      }
//      else {
//         System.err.println("setMaxForce(), current material not AxialMuscleMaterial");
//      }
//   }

//   public double getOptLength() {
//      if (myMaterial instanceof AxialMuscleMaterial) {
//         return ((AxialMuscleMaterial)myMaterial).getOptLength();
//      }
//      else {
//         return 0;
//      }
//   }
//
//   public void setOptLength (double l) {
//      if (myMaterial instanceof AxialMuscleMaterial) {
//         ((AxialMuscleMaterial)myMaterial).setOptLength(l);
//      }
//      else {
//         System.err.println("setOptLength(), current material not AxialMuscleMaterial");
//      }
//   }
//
//   public double getMaxLength() {
//      if (myMaterial instanceof AxialMuscleMaterial) {
//         return ((AxialMuscleMaterial)myMaterial).getMaxLength();
//      }
//      else {
//         return 0;
//      }   }
//
//   public void setMaxLength (double l) {
//      if (myMaterial instanceof AxialMuscleMaterial) {
//         ((AxialMuscleMaterial)myMaterial).setMaxLength(l);
//      }
//      else {
//         System.err.println("setMaxLength(), current material not AxialMuscleMaterial");
//      }
//   }
//
//   public double getTendonRatio() {
//      if (myMaterial instanceof AxialMuscleMaterial) {
//         return ((AxialMuscleMaterial)myMaterial).getTendonRatio();
//      }
//      else {
//         return 0;
//      }   }
//
//   public void setTendonRatio (double r) {
//      if (myMaterial instanceof AxialMuscleMaterial) {
//         ((AxialMuscleMaterial)myMaterial).setTendonRatio(r);
//      }
//      else {
//         System.err.println("setTendonRatio(), current material not AxialMuscleMaterial");
//      }
//   }
//
//   public double getPassiveFraction() {
//      if (myMaterial instanceof AxialMuscleMaterial) {
//         return ((AxialMuscleMaterial)myMaterial).getPassiveFraction();
//      }
//      else {
//         return 0;
//      }
//   }
//
//   public void setPassiveFraction (double r) {
//      if (myMaterial instanceof AxialMuscleMaterial) {
//         ((AxialMuscleMaterial)myMaterial).setPassiveFraction(r);
//      }
//      else {
//         System.err.println("setPassiveFraction(), current material not AxialMuscleMaterial");
//      }
//   }

//   public double getDamping() {
//      if (myMaterial instanceof AxialMuscleMaterial) {
//         return ((AxialMuscleMaterial)myMaterial).getDamping();
//      }
//      else {
//         return 0;
//      }
//   }
//
//   public void setDamping (double d) {
//      if (myMaterial instanceof AxialMuscleMaterial) {
//         ((AxialMuscleMaterial)myMaterial).setDamping(d);
//      }
//      else {
//         System.err.println("setDamping(), current material not AxialMuscleMaterial");
//      }
//   }

//   public double getForceScaling() {
//      if (myMaterial instanceof AxialMuscleMaterial) {
//         return ((AxialMuscleMaterial)myMaterial).getForceScaling();
//      }
//      else {
//         return 0;
//      }   }
//
//   public void setForceScaling (double newForceScaling) {
//      if (myMaterial instanceof AxialMuscleMaterial) {
//         ((AxialMuscleMaterial)myMaterial).setForceScaling(newForceScaling);
//      }
//      else {
//         System.err.println("setForceScaling(), current material not AxialMuscleMaterial");
//      }
//   }

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
