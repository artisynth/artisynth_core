package artisynth.core.driver;

import artisynth.core.femmodels.FemModel3d;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.CollisionManager.ColliderType;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.mechmodels.MechSystemSolver;
import artisynth.core.mechmodels.MechSystemSolver.PosStabilization;
import artisynth.core.modelbase.ModelBase;
import maspack.properties.PropertyList;
import maspack.solvers.PardisoSolver;
import maspack.solvers.SparseSolverId;
import maspack.util.EnumRange;
import maspack.util.Range;

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

   public static final boolean DEFAULT_USE_IMPLICIT_FRICTION =
      MechSystemBase.DEFAULT_USE_IMPLICIT_FRICTION;

   public static final boolean DEFAULT_ABORT_ON_INVERTED_ELEMENTS =
      FemModel3d.DEFAULT_ABORT_ON_INVERTED_ELEMENTS;

   public static final boolean DEFAULT_HYBRID_SOLVES_ENABLED =
      MechSystemSolver.DEFAULT_HYBRID_SOLVES_ENABLED;

   public static final int DEFAULT_NUM_SOLVER_THREADS = -1;

   public static final boolean DEFAULT_SHOW_ILL_CONDITIONED_SOLVES = true;
   private boolean myShowIllConditionedSolves =
      DEFAULT_SHOW_ILL_CONDITIONED_SOLVES;

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
         "useImplicitFriction",
         "default setting for using implicit friction",
         DEFAULT_USE_IMPLICIT_FRICTION);
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
      myProps.add (
         "showIllConditionedSolves", 
         "print a message when a solve is ill conditioned",
         DEFAULT_SHOW_ILL_CONDITIONED_SOLVES);
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

   public boolean getUseImplicitFriction () {
      return MechSystemBase.getDefaultUseImplicitFriction();
   }

   public void setUseImplicitFriction (boolean enable) {
      MechSystemBase.setDefaultUseImplicitFriction (enable);
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

   public boolean getShowIllConditionedSolves () {
      return myShowIllConditionedSolves;
   }

   public void setShowIllConditionedSolves (boolean enable) {
      PardisoSolver.setShowPerturbedPivots (enable);
      myShowIllConditionedSolves = enable;
   } 

   // for later use:

   public SparseSolverId getMatrixSolver () {
      return null;
   }

   public void setMatrixSolver (SparseSolverId MatrixSolver) {
   }


}
