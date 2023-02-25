package artisynth.core.driver;

import java.awt.Color;
import javax.swing.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.widgets.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.render.GL.GLViewer.*;
import maspack.render.GL.GLViewer;

import java.io.IOException;
import java.io.PrintWriter;

import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.MechSystemSolver.*;
import artisynth.core.mechmodels.CollisionManager.*;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.util.*;
import artisynth.core.util.*;
import artisynth.core.gui.*;
import artisynth.core.gui.timeline.*;
import artisynth.core.gui.jythonconsole.*;
import maspack.solvers.*;

/**
 * Preferences related to running simulations. The properties are imported
 * directly from SimulationSettings, and revised to support local variable
 * settings.
 */
public class SimulationPrefs extends Preferences {

   SimulationSettings mySettings; // current application settings

   static PropertyList myProps =
      new PropertyList (SimulationPrefs.class, SimulationSettings.class);

   private double myMaxStepSize =
      SimulationSettings.DEFAULT_MAX_STEP_SIZE;

   private PosStabilization myStabilization =
      SimulationSettings.DEFAULT_STABILIZATION;

   private ColliderType myColliderType =
      SimulationSettings.DEFAULT_COLLIDER_TYPE;

   private boolean myUseImplicitFriction =
      SimulationSettings.DEFAULT_USE_IMPLICIT_FRICTION;

   private boolean myAbortOnInvertedElements =
      SimulationSettings.DEFAULT_ABORT_ON_INVERTED_ELEMENTS;

   private boolean myHybridSolvesEnabled =
      SimulationSettings.DEFAULT_HYBRID_SOLVES_ENABLED;

   private int myNumSolverThreads =
      SimulationSettings.DEFAULT_NUM_SOLVER_THREADS;

   // to be incorporated later
   private SparseSolverId myMatrixSolver =
      SimulationSettings.DEFAULT_MATRIX_SOLVER;

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public SimulationPrefs (SimulationSettings settings) {
      mySettings = settings;
   }

   public double getMaxStepSize () {
      return myMaxStepSize;
   }

   public void setMaxStepSize (double maxStepSize) {
      myMaxStepSize = maxStepSize;
   }

   public PosStabilization getStabilization () {
      return myStabilization;
   }

   public void setStabilization (PosStabilization stabilization) {
      myStabilization = stabilization;
   }

   public ColliderType getColliderType () {
      return myColliderType;
   }

   public Range getColliderTypeRange () {
      return new EnumRange<ColliderType> (
         ColliderType.class, new ColliderType[] {
            ColliderType.TRI_INTERSECTION,
            ColliderType.AJL_CONTOUR });
   }

   public void setColliderType (ColliderType colliderType) {
      myColliderType = colliderType;
   }

   public boolean getUseImplicitFriction () {
      return myUseImplicitFriction;
   }

   public void setUseImplicitFriction (boolean enable) {
      myUseImplicitFriction = enable;
   }

   public boolean getAbortOnInvertedElements () {
      return myAbortOnInvertedElements;
   }

   public void setAbortOnInvertedElements (boolean abortOnInvertedElements) {
      myAbortOnInvertedElements = abortOnInvertedElements;
   }

   public SparseSolverId getMatrixSolver () {
      return myMatrixSolver;
   }

   public void setMatrixSolver (SparseSolverId MatrixSolver) {
      myMatrixSolver = MatrixSolver;
   }

   public boolean getHybridSolvesEnabled () {
      return myHybridSolvesEnabled;
   }

   public void setHybridSolvesEnabled (boolean hybridSolvesEnabled) {
      myHybridSolvesEnabled = hybridSolvesEnabled;
   }

   public int getNumSolverThreads () {
      return myNumSolverThreads;
   }

   public void setNumSolverThreads (int num) {
      myNumSolverThreads = num;
  } 

   public void setFromCurrent() {
      setMaxStepSize (mySettings.getMaxStepSize());
      setStabilization (mySettings.getStabilization());
      setColliderType (mySettings.getColliderType());
      setUseImplicitFriction (mySettings.getUseImplicitFriction());
      setAbortOnInvertedElements (mySettings.getAbortOnInvertedElements());
      setHybridSolvesEnabled (mySettings.getHybridSolvesEnabled());
      setNumSolverThreads (mySettings.getNumSolverThreads());
   }

   public void applyToCurrent() {
      mySettings.setMaxStepSize (getMaxStepSize());
      mySettings.setStabilization (getStabilization());
      mySettings.setColliderType (getColliderType());
      mySettings.setUseImplicitFriction (getUseImplicitFriction());
      mySettings.setAbortOnInvertedElements (getAbortOnInvertedElements());
      mySettings.setHybridSolvesEnabled (getHybridSolvesEnabled());
      mySettings.setNumSolverThreads (getNumSolverThreads());

      if (mySettings.getDialog() != null) {
         mySettings.getDialog().updateWidgetValues();
      }
   }

   protected PropertyPanel createEditingPanel() {
      PropertyPanel panel = createDefaultEditingPanel();
      addLoadApplyButtons (
         panel, new String[] {
            "  * applied values will take effect on the next model load"});
            
      return panel;
   }
}
