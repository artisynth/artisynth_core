package artisynth.core.opensim.components;

import java.util.Deque;
import java.util.List;
import java.io.PrintWriter;
import java.io.IOException;

import maspack.util.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.properties.*;

import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import artisynth.core.util.ScalableUnits;

/**
 * Custom ArtiSynth force effector to implement OpenSim's PointToPointActuator.
 */
public class TwoPointActuator extends RenderableCompositeBase
   implements  ExcitationComponent, ForceComponent, HasNumericState,
               ScalableUnits {

   protected PointList<Point> myPoints;
   public float[] myRenderCoordsA = new float[3];
   public float[] myRenderCoordsB = new float[3];

   public static final boolean DEFAULT_ENABLED = true;
   protected boolean myEnabledP = DEFAULT_ENABLED;

   public static final double DEFAULT_FORCE_SCALE = 1;
   protected double myForceScale = DEFAULT_FORCE_SCALE;

   public static final double DEFAULT_EXCITATION = 0;
   protected double myExcitation = DEFAULT_EXCITATION;

   protected CombinationRule myComboRule = CombinationRule.Sum;
   protected ExcitationSourceList myExcitationSources;

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createRenderProps (host);
      return props;
   }

   public static PropertyList myProps =
      new PropertyList (TwoPointActuator.class, RenderableCompositeBase.class);

   static {
      myProps.add (
         "renderProps", "render properties", defaultRenderProps(null));
      myProps.add (
         "enabled isEnabled",
         "enables or disables the force producing ability of this actuator",
         DEFAULT_ENABLED);
      myProps.add (
         "forceScale",
         "force magnitude corresponding to an excitation of 1",
         DEFAULT_FORCE_SCALE);
      myProps.add (
         "excitation",
         "excitation inducing the applied force",
         DEFAULT_EXCITATION, "[0,1]");
   }

   public PropertyList getAllPropertyInfo () {
      return myProps; 
   }

   protected void initializeChildComponents() {
      myPoints = new PointList<> (Point.class, "points");
      add (myPoints);
   }

   /**
    * no-args constructor needed for scan/write.
    */
   public TwoPointActuator() {
      initializeChildComponents();
   }

   private void addPoint (RigidBody body, Point3d loc) {
      if (body != null) {
         FrameMarker mkr = new FrameMarker();
         mkr.setFrame (body);
         mkr.setLocation (loc);
         myPoints.addFixed (mkr);
      }
      else {
         myPoints.addFixed (new Point (loc));
      }
   }


   public TwoPointActuator (
      String name, RigidBody bodyA, Point3d locA, RigidBody bodyB, Point3d locB) {
      
      setName (name);
      initializeChildComponents();
      addPoint (bodyA, locA);
      addPoint (bodyB, locB);
   }

   // ---- property accessors -----

   /**
    * Queries whether this actuator is enabled.
    *
    * @return {@code true} if this actuator is enabled
    */
   public boolean isEnabled() {
      return myEnabledP;
   }

   /**
    * Sets whether or not this actuator is enabled. A disabled actuator
    * produces no force.
    *
    * @param enabled if {@code true}, enables this actuator
    */
   public void setEnabled (boolean enabled) {
      if (myEnabledP != enabled) {
	 myEnabledP = enabled;
      }
   }

   /**
    * Sets the force scale associated with this TwoPointActuator. The applied
    * coordinate force is given by
    * <pre>
    * excitation * forceScale
    * </pre>
    *
    * @param s force scale
    */
   public void setForceScale (double s) {
      myForceScale = s;
   }

   /**
    * Queries the force scale associated with this TwoPointActuator. See
    * {@link #setForceScale}.
    *
    * @return force scale
    */
   public double getForceScale () {
      return myForceScale;
   }

   /**
    * Sets the excitation associated with this TwoPointActuator. The applied
    * coordinate force is given by
    * <pre>
    * excitation * forceScale
    * </pre>
    *
    * @param e excitation value
    */
   @Override
   public void setExcitation (double e) {
      myExcitation = e;
   }

   /**
    * Queries the excitation associated with this TwoPointActuator. See
    * {@link #setExcitation}.
    *
    * @return excitation value
    */
   @Override
   public double getExcitation () {
      return myExcitation;
   }

   // ---- ForceEffector methods -----

   /**
    * {@inheritDoc}
    */
   @Override
   public void applyForces (double t) {
      if (!myEnabledP) {
         return;
      }
      Point p0 = myPoints.get(0);
      Point p1 = myPoints.get(1);
      double dist = p0.distance (p1);
      if (dist > 0) {
         Vector3d f = new Vector3d();         
         f.sub (p1.getPosition(), p0.getPosition());
         f.scale (myExcitation*myForceScale/dist);
         p0.addScaledForce ( 1.0, f);
         p1.addScaledForce (-1.0, f);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      // no solve blocks needed because no jacobians are needed
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void addPosJacobian (SparseNumberedBlockMatrix M, double s) {
      // no jacobians needed because joint actuation is an external force
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
      // no jacobians needed because joint actuation is an external force
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getJacobianType () {
      return Matrix.SPD;
   }

   // ---- HasNumericState methods -----

   /**
    * {@inheritDoc}
    */
   @Override
   public void getState (DataBuffer data) {
      data.dput (myExcitation);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setState (DataBuffer data) {
      myExcitation = data.dget();
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasState() {
      return true;
   }

   // ---- ExcitationComponent methods -----

   /**
    * {@inheritDoc}
    */
   @Override
   public void setCombinationRule (CombinationRule rule) {
      myComboRule = rule;
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public CombinationRule getCombinationRule () {
      return myComboRule;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void addExcitationSource (ExcitationComponent ex, double gain) {
      if (myExcitationSources == null) {
         myExcitationSources = new ExcitationSourceList ();
      }
      myExcitationSources.add (ex, gain);
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
   public double getExcitationGain (ExcitationComponent ex) {
      return ExcitationUtils.getGain (myExcitationSources, ex);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean removeExcitationSource (ExcitationComponent ex) {
      boolean removed = false;
      if (myExcitationSources != null) {
         removed = myExcitationSources.remove (ex);
         if (myExcitationSources.size () == 0) {
            myExcitationSources = null;
         }
      }
      return removed;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public double getNetExcitation () {
      return ExcitationUtils.combine (
         myExcitation, myExcitationSources, myComboRule);
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

   /* ---- reference updating ---- */

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

   /* ---- render methods ---- */

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   public void prerender (RenderList list) {
      Point3d pos;
      pos = myPoints.get(0).getPosition();
      myRenderCoordsA[0] = (float)pos.x;
      myRenderCoordsA[1] = (float)pos.y;
      myRenderCoordsA[2] = (float)pos.z;
      pos = myPoints.get(1).getPosition();
      myRenderCoordsB[0] = (float)pos.x;
      myRenderCoordsB[1] = (float)pos.y;
      myRenderCoordsB[2] = (float)pos.z;
      list.addIfVisible (myPoints);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      myPoints.get(0).updateBounds (pmin, pmax);
      myPoints.get(1).updateBounds (pmin, pmax);
   }

   public void render (Renderer renderer, int flags) {
      renderer.drawLine (
         getRenderProps(), myRenderCoordsA, myRenderCoordsB, isSelected());
   }

   /* ---- I/O methods ---- */

   /* --- begin I/O methods --- */

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      if (myExcitationSources != null) {
         myExcitationSources.write (pw, "excitationSources", fmt, ancestor);
      }
   }

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

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "excitationSources")) {
         myExcitationSources.postscan (tokens, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   /* --- ScalableUnits methods --- */

   @Override
   public void scaleDistance (double s) {
      myForceScale *= s;
   }

   @Override
   public void scaleMass (double s) {
      myForceScale *= s;
   }

   // transform geometry

}
