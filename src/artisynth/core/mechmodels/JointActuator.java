/**
 * Copyright (c) 2025, by the Authors: Alexander Denk, University of
 * Duisburg-Essen, Chair of Mechanics and Robotics, alexander.denk@uni-due.de
 * and John E Lloyd (UBC).
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.ArrayList;
import java.util.List;
import java.util.Deque;
import java.io.PrintWriter;
import java.io.IOException;

import artisynth.core.mechmodels.ExcitationComponent;
import artisynth.core.mechmodels.ExcitationSourceList;
import artisynth.core.mechmodels.ExcitationUtils;
import artisynth.core.mechmodels.ForceComponent;
import artisynth.core.mechmodels.JointBase;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.modelbase.HasNumericState;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import artisynth.core.util.ScalableUnits;
import maspack.matrix.Matrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.properties.PropertyList;
import maspack.util.DataBuffer;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Applies, as an external force, the generalized force associated with a joint
 * coordinate. This is done by setting the actuator's {@code excitation}
 * property, which is then scaled by the {@code forceScale} property to
 * determine the actual applied force.
 */
public class JointActuator extends ModelComponentBase
   implements ExcitationComponent, ForceComponent, HasNumericState,
              ScalableUnits {

   public static final boolean DEFAULT_ENABLED = true;
   protected boolean myEnabledP = DEFAULT_ENABLED;

   public static final double DEFAULT_FORCE_SCALE = 1;
   protected double myForceScale = DEFAULT_FORCE_SCALE;

   public static final double DEFAULT_EXCITATION = 0;
   protected double myExcitation = DEFAULT_EXCITATION;

   protected CombinationRule myComboRule = CombinationRule.Sum;
   protected ExcitationSourceList myExcitationSources;

   JointCoordinateHandle myCoord;

   public static PropertyList myProps =
      new PropertyList (JointActuator.class, ModelComponentBase.class);
   
   static {
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
         DEFAULT_EXCITATION, "[-1,1]");
   }
   
   public PropertyList getAllPropertyInfo () {
      return myProps; 
   }

   /**
    * No-args constructor needed for write/scan.
    */
   public JointActuator () {
   }

   /**
    * Creates a named JointActuator from a specified coordinate handle.
    * 
    * @param name
    * name of the JointActuator (can be {@code null})
    * @param ch
    * handle describing the coordinate and its joint
    * @param forceScale
    * excitation scale factor that determines the actual applied force
    */
   public JointActuator (
      String name, JointCoordinateHandle ch, double forceScale) {
      super (name);
      myCoord = new JointCoordinateHandle (ch);
      myForceScale = forceScale;
   }

   /**
    * Creates an unnamed JointActuator from a specified coordinate handle.
    * 
    * @param ch
    * handle describing the coordinate and its joint
    * @param forceScale
    * excitation scale factor that determines the actual applied force
    */
   public JointActuator (JointCoordinateHandle ch, double forceScale) {
      this (null, ch, forceScale);
   }

   /**
    * Creates a named JointActuator from a specified joint and coordinate index.
    * 
    * @param name
    * name of the JointActuator (can be {@code null})
    * @param joint
    * joint containing the coordinate
    * @param idx
    * index of the coordinate within the joint
    * @param forceScale
    * excitation scale factor that determines the actual applied force
    */
   public JointActuator (
      String name, JointBase joint, int idx, double forceScale) {
      super (name);
      myCoord = new JointCoordinateHandle (joint, idx);
      myForceScale = forceScale;
   }

   /**
    * Returns the handle for the coordinate/joint pair used by this
    * JointActuator. Should not be modified.
    *
    * @return coordinate handle
    */
   public JointCoordinateHandle getCoordinateHandle() {
      return myCoord;
   }

   /* ---- property accessors ----- */

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
    * Sets the force scale associated with this JointActuator. The applied
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
    * Queries the force scale associated with this JointActuator. See
    * {@link #setForceScale}.
    *
    * @return force scale
    */
   public double getForceScale () {
      return myForceScale;
   }

   /**
    * Sets the excitation associated with this JointActuator. The applied
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
    * Queries the excitation associated with this JointActuator. See
    * {@link #setExcitation}.
    *
    * @return excitation value
    */
   @Override
   public double getExcitation () {
      return myExcitation;
   }

   /* ---- ForceEffector methods ----- */

   /**
    * {@inheritDoc}
    */
   @Override
   public void applyForces (double t) {
      if (myEnabledP) {
         myCoord.applyForce (myExcitation * myForceScale);
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

   /* ---- HasNumericState methods ----- */

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

   /* ---- ExcitationComponent methods ----- */

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
   
   /* ---- reference updating ---- */

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
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      refs.add (myCoord.getJoint());
   }

   /**
    * Creates a complete set of JointActuators for all coordinates associated
    * with a specific joint.
    * 
    * @param mech 
    * if non-null, adds the actuators to the MechModel's set of force effectors
    * @param joint
    * joint for which the actuators should be created
    * @param forceScale
    * force scale for each actuator
    * @return list of the created exciters
    */
   public static ArrayList<JointActuator> createActuators (
      MechModel mech, JointBase joint, double forceScale) {
      ArrayList<JointActuator> exs = new ArrayList<> ();
      for (int idx = 0; idx < joint.numCoordinates (); idx++) {
         exs.add (new JointActuator (/*name*/null, joint, idx, forceScale));
      }
      if (mech != null) {
         for (JointActuator ex : exs) {
            mech.addForceEffector (ex);
         }
      }
      return exs;
   }

   /* --- begin I/O methods --- */

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.print ("coordinate=");
      myCoord.write (pw, ancestor);
      super.writeItems (pw, fmt, ancestor);
      if (myExcitationSources != null) {
         myExcitationSources.write (pw, "excitationSources", fmt, ancestor);
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "coordinate")) {
         tokens.offer (new StringToken ("coordinate", rtok.lineno()));
         myCoord = new JointCoordinateHandle();
         myCoord.scan (rtok, tokens);
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
      
      if (postscanAttributeName (tokens, "coordinate")) {
         myCoord.postscan (tokens, ancestor);
         return true;
      }
      else if (postscanAttributeName (tokens, "excitationSources")) {
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

}
