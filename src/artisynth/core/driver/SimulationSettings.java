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
import artisynth.core.util.*;
import artisynth.core.util.*;
import artisynth.core.gui.*;
import artisynth.core.gui.timeline.*;
import artisynth.core.gui.jythonconsole.*;
import maspack.solvers.*;

/**
 * Preferences related to running simulations
 */
public class SimulationSettings extends SettingsBase {

   public static PropertyList myProps =
      new PropertyList (SimulationSettings.class);

   public static final double DEFAULT_MAX_STEP_SIZE =
      ModelBase.DEFAULT_MAX_STEP_SIZE;
   
   public static final PosStabilization DEFAULT_STABILIZATION = 
      MechSystemBase.DEFAULT_STABILIZATION;

   public static final ColliderType DEFAULT_COLLIDER_TYPE =
      CollisionManager.DEFAULT_COLLIDER_TYPE;

   public static final boolean DEFAULT_ABORT_ON_INVERTED_ELEMENTS =
      FemModel3d.DEFAULT_ABORT_ON_INVERTED_ELEMENTS;

   public static final boolean DEFAULT_HYBRID_SOLVES_ENABLED =
      MechSystemSolver.DEFAULT_HYBRID_SOLVES_ENABLED;

   public static final int DEFAULT_NUM_SOLVER_THREADS = -1;

   // to be incorporated later
   public static SparseSolverId DEFAULT_MATRIX_SOLVER = SparseSolverId.Pardiso;

   static {
      myProps.add (
         "maxStepSize",
         "default simulation step size",
         DEFAULT_MAX_STEP_SIZE, "(0,inf] NS");
      myProps.add (
         "stabilization",
         "default method for position stablization",
         DEFAULT_STABILIZATION);
      myProps.add (
         "colliderType",
         "default collider type for collision detection",
         DEFAULT_COLLIDER_TYPE);
      myProps.add (
         "abortOnInvertedElements",
         "abort when FEM encounters inverted elements with non-linear materials",
         DEFAULT_ABORT_ON_INVERTED_ELEMENTS);
      myProps.add (
         "hybridSolvesEnabled", 
         "enable use of hybrid direct/iterative solves",
         DEFAULT_HYBRID_SOLVES_ENABLED);
      myProps.add (
         "numSolverThreads", 
         "number of threads to use in the sparse solver",
         DEFAULT_NUM_SOLVER_THREADS);
   }

   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public double getMaxStepSize () {
      return ModelBase.getDefaultMaxStepSize();
   }

   public void setMaxStepSize (double maxStepSize) {
      ModelBase.setDefaultMaxStepSize (maxStepSize);
   }

   public PosStabilization getStabilization () {
      return MechSystemBase.getDefaultStabilization();
   }

   public void setStabilization (PosStabilization stabilization) {
      MechSystemBase.setDefaultStabilization (stabilization);
   }

   public ColliderType getColliderType () {
      return CollisionManager.getDefaultColliderType();
   }

   public Range getColliderTypeRange () {
      return new EnumRange<ColliderType> (
         ColliderType.class, new ColliderType[] {
            ColliderType.TRI_INTERSECTION,
            ColliderType.AJL_CONTOUR });
   }

   public void setColliderType (ColliderType type) {
      CollisionManager.setDefaultColliderType (type);
   }

   public boolean getAbortOnInvertedElements () {
      return FemModel3d.abortOnInvertedElems;
   }

   public void setAbortOnInvertedElements (boolean enable) {
      FemModel3d.abortOnInvertedElems = enable;
   }

   public boolean getHybridSolvesEnabled () {
      return MechSystemSolver.myDefaultHybridSolveP;
   }

   public void setHybridSolvesEnabled (boolean enabled) {
      MechSystemSolver.myDefaultHybridSolveP = enabled;
   }

   public int getNumSolverThreads () {
      return PardisoSolver.getDefaultNumThreads();
   }

   public void setNumSolverThreads (int num) {
      PardisoSolver.setDefaultNumThreads(num);
   } 

   // for later use:

   public SparseSolverId getMatrixSolver () {
      return null;
   }

   public void setMatrixSolver (SparseSolverId MatrixSolver) {
   }


}
