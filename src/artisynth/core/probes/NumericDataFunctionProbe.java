/**
 * Copyright (c) 2019, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.util.ArrayList;
import artisynth.core.modelbase.*;

/**
 * Base class for a NumericProbe that uses DataFunctions.
 */
public abstract class NumericDataFunctionProbe extends NumericProbeBase
   implements CopyableComponent {

   protected ArrayList<DataFunction> myDataFunctions =
      new ArrayList<DataFunction>();
   protected ArrayList<HasNumericState> myDataFunctionsWithState = null;

   protected boolean updateDataFunctionsWithState() {
      ArrayList<HasNumericState> stateFxns = new ArrayList<HasNumericState>();
      for (DataFunction fxn : myDataFunctions) {
         if (fxn instanceof HasNumericState) {
            stateFxns.add ((HasNumericState)fxn);
         }
      }
      if (stateFxns.size() == 0) {
         stateFxns = null;
      }
      boolean changed =
         (((stateFxns == null) != (myDataFunctionsWithState != null)) ||
          (stateFxns != null && !stateFxns.equals (myDataFunctionsWithState)));
      myDataFunctionsWithState = stateFxns;
      return changed;
   }

   /**
    * Sets a data function to be used by this probe, either in
    * {@link NumericControlProbe#applyData} (for NumericControlProbes),
    * or {@link NumericMonitorProbe#generateData} (for NumericMonitorProbes).
    * If the data function is set to
    * <code>null</code>, then this probe will do nothing. If the
    * {@link NumericControlProbe#applyData} or
    * {@link NumericMonitorProbe#generateData}
    * method is overridden by a subclass, then the data
    * application is determined instead by the overriding method.
    *
    * @see #getDataFunction
    */
   public void setDataFunction (DataFunction func) {
      if (myDataFunctions.size() == 0) {
         if (func != null) {
            myDataFunctions.add (func);
         }
      }
      else {
         if (func != null) {
            myDataFunctions.set (0, func);
         }
         else {
            myDataFunctions.clear();
         }
      }
      if (updateDataFunctionsWithState()) {
         // changing state disposition invalidates waypoints
         notifyParentOfChange (new DynamicActivityChangeEvent(this));
      }
   }

   /**
    * Returns the data function, if any, that is used by this probe's {@link
    * #apply(double)} method to apply data for this probe.
    *
    * @see #setDataFunction
    */
   public DataFunction getDataFunction() {
      return myDataFunctions.size() > 0 ? myDataFunctions.get(0) : null;
   }

   public boolean hasState() {
      return myDataFunctionsWithState != null;
   }

   public ComponentState createState (
      ComponentState prevState) {
      if (myDataFunctionsWithState != null){
         return new NumericState();
      }
      else {
         return new EmptyState();
      }
   }

   public void getState (ComponentState state) {
      if (myDataFunctionsWithState != null) {
         NumericState data = (NumericState)state;
         for (HasNumericState fxn : myDataFunctionsWithState) {
            fxn.getState (data);
         }
      }
   }

   public void setState (ComponentState state) {
      if (myDataFunctionsWithState != null) {
         NumericState data = (NumericState)state;
         data.resetOffsets();
         for (HasNumericState fxn : myDataFunctionsWithState) {
            fxn.setState (data);
         }
      }
   }

   public void getInitialState (
      ComponentState newstate, ComponentState oldstate) {
      if (myDataFunctionsWithState != null) {
         ComponentStateUtils.getInitialState (
            newstate, oldstate, myDataFunctionsWithState);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return dataFunctionsAreCopyable (myDataFunctions);
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isCloneable() {
      return isDuplicatable();
   }

   public Object clone() throws CloneNotSupportedException {
      NumericDataFunctionProbe probe = (NumericDataFunctionProbe)super.clone();
      probe.myDataFunctions = copyDataFunctions (myDataFunctions);
      return probe;
   }

}
