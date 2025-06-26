package artisynth.core.probes;

import java.io.*;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.properties.*;

import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.probes.IKSolver.SearchStrategy;

public class IKProbe extends NumericControlProbe {

   IKSolver mySolver;

   public static final boolean DEFAULT_BODIES_NON_DYNAMIC_IF_ACTIVE = false;
   protected boolean myBodiesNonDynamicIfActive =
      DEFAULT_BODIES_NON_DYNAMIC_IF_ACTIVE;

   public static PropertyList myProps =
      new PropertyList (IKProbe.class, NumericControlProbe.class);

   static {
      myProps.add (
         "massRegularization",
         "mass regulaization coefficient for body solve blocks",
         IKSolver.DEFAULT_MASS_REGULARIZATION);
      myProps.add (
         "maxIterations",
         "max number of iterations per IK solve step",
         IKSolver.DEFAULT_MAX_ITERATIONS);
      myProps.add (
         "convergenceTol",
         "converage tolerance for each IK solve step",
         IKSolver.DEFAULT_CONVERGENCE_TOL);
      myProps.add (
         "searchStrategy",
         "least-squares search strategy for the IK solve steps",
         IKSolver.DEFAULT_SEARCH_STRATEGY);
      myProps.add (
         "bodiesNonDynamicIfActive",
         "set all bodies to be non-dynamic when the probe is active",
         DEFAULT_BODIES_NON_DYNAMIC_IF_ACTIVE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Empty constructor required for write/scan. 
    */
   public IKProbe () {
      mySolver = new IKSolver();
   }

   public IKProbe (
      String name, MechModel mech, Collection<FrameMarker> mkrs,
      VectorNd wgts, double startTime, double stopTime) {
      mySolver = new IKSolver (mech, mkrs, wgts);
      if (name != null) {
         setName (name);
      }
      setVsize (3*mySolver.numMarkers());
      setStartTime (startTime);
      setStopTime (stopTime);
      setData (getStartTime());
      setData (getStopTime());
   }

   public IKProbe (
      String name, MechModel mech, Collection<FrameMarker> mkrs,
      VectorNd wgts, String fileName) throws IOException {
      if (fileName == null) {
         throw new IllegalArgumentException ("null fileName");
      }
      mySolver = new IKSolver (mech, mkrs, wgts);
      setVsize (3*numMarkers());
      setAttachedFileName (fileName);
      load(/*setTimes=*/true);
      if (name != null) {
         setName (name);
      }
   }

   /* --- property accessors --- */

   /**
    * Queries the mass regulaization coefficient for this IKProbe.
    *
    * @see #setMassRegularization
    * @return mass regulaization coefficient
    */
   public double getMassRegularization() {
      return mySolver.getMassRegularization();
   }

   /**
    * Sets the mass regulaization coefficient for this IKProbe. The default
    * value is 0.001. See {@link IKSolver#setMassRegularization} for details.
    *
    * @see #getMassRegularization
    * @param c mass regulaization coefficient
    */
   public void setMassRegularization (double c) {
      mySolver.setMassRegularization (c);
   }

   /**
    * Queries the maximum number of iterations allowed in each IK solve step.
    *
    * @see #setMaxIterations
    * @return maximum number of solve iterations
    */
   public int getMaxIterations() {
      return mySolver.getMaxIterations();
   }

   /**
    * Set the maximum number of iterations allowed in each IK solve step.  The
    * default value is 20.
    *
    * @see #getMaxIterations
    * @param maxi maximum number of solve iterations
    */
   public void setMaxIterations (int maxi) {
      mySolver.setMaxIterations (maxi);
   }

   /**
    * Queries the convergance tolerance used in each IK solve step.
    * 
    * #see #setConvergenceTol
    * @return convergence tolerance
    */
   public double getConvergenceTol() {
      return mySolver.getConvergenceTol();
   }

   /**
    * Sets the convergance tolerance {@code tol} used in each IK solve step.
    * See {@link IKSolver#setConvergenceTol} for details.
    * 
    * #see #getConvergenceTol
    * @param tol convergence tolerance
    */
   public void setConvergenceTol (double tol) {
      mySolver.setConvergenceTol (tol);
   }

   /**
    * Queries the least-squares search strategy for the IK solve steps.
    * 
    * #see #setSearchStrategy
    * @return IK search strategy
    */
   public SearchStrategy getSearchStrategy() {
      return mySolver.getSearchStrategy();
   }

   /**
    * Sets the least-squares search strategy for the IK solve steps.
    * 
    * #see #getSearchStrategy
    * @param strat IK search strategy
    */
   public void setSearchStrategy (SearchStrategy strat) {
      mySolver.setSearchStrategy(strat);
   }

   /**
    * Queries the {@code bodiesNonDynamicIfActive} property of this
    * probe. See {@link #setBodiesNonDynamicIfActive} for details.
    *
    * @return {@code true} if {@code bodiesNonDynamicIfActive} is enabled.
    */
   public boolean getBodiesNonDynamicIfActive () {
      return myBodiesNonDynamicIfActive;
   }

   /**
    * Sets the {@code bodiesNonDynamicIfActive} property of this probe.
    * If true, then the {@code dynamic} property of the bodies associated 
    * with this probe is set {@code false} when the probe is active. When
    * the probe transitions to being inactive, {@link #resetBodiesDynamic}
    * is called.
    * 
    * @param enable new value for the {@code bodiesNonDynamicIfActive} 
    * property of this probe
    */
   public void setBodiesNonDynamicIfActive (boolean enable) {
      if (enable != myBodiesNonDynamicIfActive) {
         if (enable && !isScanning() && isActive()) {
            setBodiesDynamic (false);
         }
         myBodiesNonDynamicIfActive = enable;
      }
   }

   /**
    * Returns the IKSolver used by this probe.
    *
    * @return probe's IKSolver
    */
   public IKSolver getSolver() {
      return mySolver;
   }

   @Override
   public void setActive (boolean enable) {
      boolean activityChanged = (enable != isActive());
      super.setActive (enable);
      if (myBodiesNonDynamicIfActive) {
         if (activityChanged) {
            if (enable) {
               setBodiesDynamic (false);
            }
            else {
               resetBodiesDynamic();
            }
         }
      }
   }

   public void applyData (VectorNd vec, double t, double trel) {
      int niters = mySolver.solve (vec);
   }

   public void setData (double t) {
      addData (t, getCurrentMarkerValues());
   }         

   /**
    * Sets the {@code dynamic} property of all the bodies associated
    * with this probe.
    *
    * @param dynamic setting for each body's {@code dynamic} property
    */
   public void setBodiesDynamic (boolean dynamic) {
      mySolver.setBodiesDynamic (dynamic);
   }

   /**
    * Restore the {@code dynamic} property setting of all the bodies associated
    * with this probe to their value at initialization.
    */
   public void resetBodiesDynamic() {
      mySolver.resetBodiesDynamic(); 
   }

   public boolean isSettable() {
      return true;
   }

   private VectorNd getCurrentMarkerValues() {
      VectorNd vals = new VectorNd (3*mySolver.numMarkers());
      int k = 0;
      for (FrameMarker mkr : mySolver.getMarkers()) {
         vals.setSubVector (k*3, mkr.getPosition());
         k++;
      }
      return vals;
   }

   /* --- Begin I/O methods --- */

   public boolean scanItem (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "solver")) {
         tokens.offer (new StringToken ("solver", rtok.lineno()));
         mySolver.scan (rtok, tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
 
      if (postscanAttributeName (tokens, "solver")) {
         mySolver.postscan (tokens, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      pw.print ("solver=");
      mySolver.write (pw, fmt, ancestor);
      super.writeItems (pw, fmt, ancestor);
   }

   /* --- end I/O methods --- */

   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      IKProbe probe;
      try {
         probe = (IKProbe)clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("Cannot clone IKProbe");
      }
      probe.mySolver = new IKSolver (mySolver);
      return probe;
   }

   /**
    * Returns the markers used by this IKProbe
    *
    * @return markers used by this probe
    */
   public ArrayList<FrameMarker> getMarkers() {
      return mySolver.getMarkers();
   }

   /**
    * Returns the number of markers used by this IKProbe.
    *
    * @return number of markers used by this probe
    */
   public int numMarkers() {
      return mySolver.numMarkers();
   }

   @Override
   protected Object[] getPropsOrDimens () {
      Object[] props = new Object[mySolver.numMarkers()];
      int k = 0;
      for (FrameMarker mkr : getMarkers()) {
         props[k++] = mkr.getProperty ("position");
      }
      return props;
   }

}
