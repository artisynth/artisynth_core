/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import artisynth.core.modelbase.CompositeState;
import artisynth.core.util.TimeBase;
import artisynth.core.workspace.RootModel;

public class WayPoint {
   protected double myTime;
   protected CompositeState myState;
   protected boolean myValidP;
   protected boolean myBreakPointP;

   // links for maintaining double linked list inside the WayPointProbe
   protected WayPoint myNext;
   protected WayPoint myPrev;

   public WayPoint (double t) {
      setTime (t);
   }

   public WayPoint (double t, boolean isBreakPoint) {
      setBreakPoint (isBreakPoint);
      setTime (t);
   }

   public double getTime() {
      return myTime;
   }

   public void setTime (double t) {
      myTime = TimeBase.round(t);
      myValidP = false;
   }

   public boolean isValid() {
      return myValidP;
   }

   public void setValid (boolean valid) {
      myValidP = valid;
   }

   public CompositeState getState() {
      return myState;
   }

   public void setState (RootModel model) {
      myState = (CompositeState)model.createState(null);
      if (myTime == 0) {
         model.getInitialState (myState, null);
      }
      else {
         model.getState (myState);
      }
      setValid (true);
   }
   
   public void setState (CompositeState state) {
      myState = state;
      setValid (true);
   }

   public boolean isBreakPoint() {
      return myBreakPointP;
   }

   public void setBreakPoint (boolean isBreakPoint) {
      myBreakPointP = isBreakPoint;
   }
}
