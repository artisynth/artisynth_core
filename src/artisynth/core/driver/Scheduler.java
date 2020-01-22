/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import maspack.matrix.NumericalException;
import maspack.util.InternalErrorException;
import artisynth.core.modelbase.*;
import artisynth.core.probes.*;
import artisynth.core.util.TimeBase;
import artisynth.core.workspace.RenderProbe;
import artisynth.core.workspace.RootModel;
import artisynth.core.workspace.SleepProbe;
import artisynth.core.workspace.Workspace;

import java.util.*;

public class Scheduler {

   public static boolean checkState = false;

   public boolean setStateBeforeInit = true;
   private boolean debugStepComputation = false;

   private double myTime = 0;
   private boolean myRealTimeAdvanceP = true;
   private RenderProbe myRenderProbe;
   private SleepProbe mySleepProbe;
   private Player myPlayer = null;
   private Exception myLastException = null;
      private double myRealTimeScaling = 1.0;

   public static boolean useNewAdvance = true;

   protected LinkedList<SchedulerListener> myListeners;

   private Main myMain;
   
   public Scheduler(Main main) {
      myListeners = new LinkedList<SchedulerListener>();
      myMain = main;
   }

   public enum Action {
      Reset,
         Rewind,
         Play,
         Pause,
         Stopped,
         Step,
         Forward,
         Advance,
         StepTimeSet,
         };

   protected void fireListeners (Action action) {
      for (SchedulerListener l : myListeners) {
         l.schedulerActionPerformed (action);
      }
   }

   public void addListener (SchedulerListener l) {
      myListeners.add (l);
   }

   public boolean removeListener (SchedulerListener l) {
      return myListeners.remove (l);
   }
   
   public void setRenderProbe (RenderProbe probe) {
      myRenderProbe = probe;
   }

   public RenderProbe getRenderProbe() {
      return myRenderProbe;
   }

   public void setSleepProbe (SleepProbe probe) {
      mySleepProbe = probe;
   }

   private void dosleep (long msec) {
      try {
         Thread.sleep (msec);
      }
      catch (Exception e) { // 
      }
   }

   private RootModel getRootModel() {
      return myMain.getRootModel();
   }

   private double getStepSize() {
      return myMain.getMaxStep();
   }
   
   private Workspace getWorkspace() {
      return myMain.getWorkspace();
   }

   private WayPointProbe getWayPoints() {
      return getRootModel().getWayPoints();
   }

   private WayPoint getWayPoint (double time) {
      return getRootModel().getWayPoint(time);
   }

   private volatile int playerCount = 0;

   /**
    * player class
    * 
    * @author andreio
    * 
    */
   private class Player extends Thread {
      private final String playerPrefix = "Player-";
      double endTime;
      boolean myStopReq;
      boolean myAlive;
      boolean myStopping = false;
      boolean myKilled = false;
      double startTime; // simulation time
      double timeScale; // simulationTime = timeScale * real_time
      long realStartMsec; // real start time, in msec
      long lastYieldMsec; // last time we yielded, in msec
      Workspace myWorkspace;
      LinkedList<Runnable> myActions;

      private void doinitialize (double endTime) {
         myStopReq = false;
         myAlive = true;
         myLastException = null;
         // timeScale = 1;
         myActions = new LinkedList<Runnable>();
         this.endTime = endTime;
         myWorkspace = getWorkspace();
      }

      public Player (double endTime) {
         timeScale = 1.0;
         doinitialize (endTime);
         setName(playerPrefix + playerCount);
         playerCount++;
      }
      
      public double getTimeScale () {
    	  return timeScale;
      }
      
      public void setTimeScale (double s) {
    	  if (s <= 0) {
    		  s = 1.0;
    	  }
    	  timeScale = s;
      }

      private void applyOutputProbes (double t, ArrayList<Probe> eventProbes) {
         for (Probe p : eventProbes) {
            if (!p.isInput() && p.isActive()) {
               p.apply (t);
            }
         }
      }

      void checkState (RootModel root) {
         CompositeState rootState = root.getState (/*annotated=*/true);
         root.setState (rootState);
         CompositeState testState = root.getState (/*annotated=*/true);
         StringBuilder errMsg = new StringBuilder();
         if (!testState.equals (rootState, errMsg)) {
            System.out.println (errMsg);
            throw new InternalErrorException (
               "Error: state differs after set");
         }           
      }

      /**
       * Reset the real start time to be synchronized with the current
       * simulation time.
       */
      void resetRealStartTime() {
         long simElapsedMsec = (long)(timeScale*1000*(myTime-startTime));
         realStartMsec = System.currentTimeMillis() - simElapsedMsec;
      }

      void doadvance (double t0sec, double t1sec, int flags) {
         
         RootModel rootModel = getRootModel();
         if (checkState) {
            checkState (rootModel);
         }
         
         rootModel.advance (t0sec, t1sec, flags);
         myTime = t1sec;

         long realMsec1 = System.currentTimeMillis();
         long realElapsedMsec = realMsec1 - realStartMsec;
         long simElapsedMsec = (long)(timeScale*1000*(myTime-startTime));

         if (realMsec1 - lastYieldMsec > 50) {
            // Yield if we haven't done so in the last 50 msec
            lastYieldMsec = realMsec1;
            Thread.yield();
         }
         else if (myRealTimeAdvanceP && simElapsedMsec - realElapsedMsec > 10) {
            // if connected to a viewer, slow down to real time
            // sleep only if we are more than 10 msec off real time, since
            // sleep usually works in 10 msec increments
            dosleep (simElapsedMsec - realElapsedMsec);
         }
         if (realMsec1 - myWorkspace.getLastRenderTime() > 200) {
            // rerender if we haven't done so in 200 msec
            myWorkspace.rerender();
         }
      }

      private void processActionRequests () {
         Runnable action;
         do {
            action = null;
            synchronized (Scheduler.this) {
               if (myActions.size() > 0) {
                  action = myActions.poll();
               }
            }
            if (action != null) {
               action.run();
            }
         }
         while (action != null);
      }         

      public void play() {
         ArrayList<Probe> eventProbes = new ArrayList<Probe>();
         realStartMsec = System.currentTimeMillis();
         startTime = myTime;
         lastYieldMsec = realStartMsec;
         RootModel root = getRootModel();

         root.setStopRequest (false);
         while (myAlive) {
            double t0 = myTime;
            double t1;

            if (t0 == 0) {
               // apply any output probes that need to be applied at t0. 
               // use -1 as current time to get any events at t0
               t1 = nextEvent (root, eventProbes, -1, endTime);
               if (t1 == 0) {
                  applyOutputProbes (t1, eventProbes);
                  eventProbes.clear();
               }
            }
            if ((t1 = nextEvent (root, eventProbes, t0, endTime)) != -1) {
               try {
                  doadvance (t0, t1, 0);
                  fireListeners (Action.Advance);
                  applyOutputProbes (t1, eventProbes);
               }
               catch (Exception e) {
                  // stop the scheduler if there is a stop request
                  e.printStackTrace();
                  myLastException = e;
                  myStopReq = true;
               }
            }
            if (t1 == -1 ||
                TimeBase.compare (myTime, endTime) >= 0 ||
                root.getStopRequest()) {
               myStopReq = true;
            }
            if (myStopReq) {
               // do a final rerender. This will result in an action request
               // being posted.
               myWorkspace.rerender(); 
               // set stopping so that we will accept no more actions requests
               myStopping = true;
            }
            processActionRequests();
            
            synchronized (Scheduler.this) {
               if (myStopping) {
                  myStopReq = false;
                  myStopping = false;
                  myAlive = false;
               }
            }

         }
         fireListeners (Action.Stopped);
      }         

      /**
       * main run method of a thread
       */
      public void run() {
         while (!myKilled) {
            play();
            synchronized (this) {
               while (!myAlive && !myKilled) {
                  try {
                     wait();
                  }
                  catch (Exception e) {
                  }
               }
            }
         }
      }

      public synchronized void start (double endTime) {
         doinitialize (endTime);
         notify ();
      }

      public synchronized void dispose() {
         myStopReq = true;
         myKilled = true;
         notify ();
      }
   }

   private boolean isPlayerAlive() {
      return myPlayer != null && myPlayer.myAlive;
   }

   public boolean getRealTimeAdvance() {
      return myRealTimeAdvanceP;
   }

   public void setRealTimeAdvance (boolean enable) {
      if (myRealTimeAdvanceP != enable) {
         if (myPlayer != null) {
            myPlayer.resetRealStartTime();
         }
         myRealTimeAdvanceP = enable;
      }
   }
   
   public void setRealTimeScaling (double s) {
      if (s != myRealTimeScaling) {
         myRealTimeScaling = s;
         if (myPlayer != null) {
            myPlayer.setTimeScale(s);
         }
      }
   }
   
   public double getRealTimeScaling () {
      return myRealTimeScaling;
   }

   public void initialize (double time) {
      myTime = TimeBase.round(time);
      if (setStateBeforeInit) {
         WayPoint way0 = getWayPoint (0);
         if (way0 != null) { 
            if (!way0.isValid()) {
               way0.setState (getRootModel());
            }
            else if (!way0.getState().isAnnotated()) {
             // waypoint contains state information which was loaded 
             // from an external file. Need to add augmentation information 
             // to this state.
             CompositeState scannedState = way0.getState();
             way0.setState (getRootModel());
             way0.getState().set (scannedState);              
            }
         }         
         getWorkspace().initialize (time);
      }
      else {
         getWorkspace().initialize (time);
         WayPoint way0 = getWayPoint (0);
         if (way0 != null && !way0.isValid()) {
            way0.setState (getRootModel());
         }
      }
   }

   public void waitForPlayingToStop() {
      while (isPlaying()) {
         dosleep (10);
      }
   }
   
   public boolean stopRequestPending() {
      return (myPlayer != null && myPlayer.myStopReq);
   }

   public void setTime (double time) {

      time = TimeBase.round(time);
      if (getWorkspace().rootModelHasState()) {
         WayPoint way = getWayPoint (time);
         if (way == null) {
            throw new IllegalArgumentException (
               "can't find way point for time " + time);
         }
         setTime (way);
      }
      else {
         if (isPlaying()) {
            stopRequest();
            waitForPlayingToStop();
         }
         updateInitialStateIfNecessary();
         // if there is no state, we simply call advance with t0 = t1.
         getWorkspace().advance (time, time, 0);
         myTime = time;
         getWorkspace().rerender(); // force a repaint
      }
   }

   public void setInitialTime (double time) { 
      myTime = TimeBase.round(time);
   }

   public void setTime (WayPoint way) {
      if (isPlaying()) {
         stopRequest();
         waitForPlayingToStop();
      }
      updateInitialStateIfNecessary();
      if (!way.isValid()) {
         throw new IllegalArgumentException (
            "way point at time "+way.getTime()+" is not valid");
      }
      myTime = TimeBase.round(way.getTime());
      getWorkspace().getRootModel().setState (way.getState());
      getWorkspace().getRootModel().initialize (myTime);
      getWorkspace().rerender(); // force a repaint
   }

   public void reset() {
      if (isPlaying()) {
         stopRequest();
         waitForPlayingToStop();
      }
      updateInitialStateIfNecessary();
      WayPoint way = getWayPoint (0);
      if (way != null && way.isValid()) {
         reset (way);
      }
      else {
         myTime = 0;
         if (setStateBeforeInit) {
            way.setState (getRootModel());           
            getWorkspace().initialize (myTime);
         }
         else {
            getWorkspace().initialize (myTime);
            way.setState (getRootModel());
         }
         getWorkspace().rerender();
         fireListeners (Action.Reset);
      }
   }

   public void reset (WayPoint way) {
      if (isPlaying()) {
         stopRequest();
         waitForPlayingToStop();
      }
      if (!way.isValid()) {
         throw new IllegalStateException ("waypoint is not valid");
      }
      updateInitialStateIfNecessary();
      myTime = TimeBase.round(way.getTime());
      if (setStateBeforeInit) {
         getWorkspace().getRootModel().setState (way.getState());
         getWorkspace().initialize (myTime);
      }
      else {
         getWorkspace().getRootModel().setState (way.getState());
         getWorkspace().initialize (myTime);
         way.setState (getRootModel());
      }
      getWorkspace().rerender();
      fireListeners (Action.Reset);
   }

   public double getTime() {
      return myTime;
   }

   /**
    * Moves to the previous waypoint, if available.
    * @return true if we have moved, false otherwise
    */
   public boolean rewind() {
      WayPoint way = getWayPoints().getValidBefore (getTime());
      if (way != null) {
         setTime (way);
         getWorkspace().rerender();
         fireListeners (Action.Rewind);
         return true;
      }
      return false;
   }
   
   /**
    * Moves to the next waypoint, if available.
    * @return true if we have moved, false otherwise
    */
   public boolean fastForward() {
      WayPoint way = getWayPoints().getValidAfter (getTime());
      if (way != null) {
         setTime (way);
         getWorkspace().rerender();
         fireListeners (Action.Rewind);
         return true;
      } else {
         return false;
      }
   }

   public synchronized void stopRequest() {
      if (isPlayerAlive()) {
         myPlayer.myStopReq = true;
      }
   }

   
   public void play() {
      playRequest (Double.POSITIVE_INFINITY);
   }

   public void play (double time) {
      playRequest (getTime() + time);
   }

   public void pause() {
      if (isPlaying()) {
         stopRequest();
         waitForPlayingToStop();
         fireListeners (Action.Pause);
      }
   }

   public void step() {
      double stepSize = getStepSize();
      double t = getTime();
      // play to the next time after t lying on a step boundary
      playRequest (t + (stepSize-TimeBase.modulo(t,stepSize)));
   }

   public void playRequest (double endTime) {
      if (isPlaying()) {
         stopRequest();
         waitForPlayingToStop();
      }
      updateInitialStateIfNecessary();
      fireListeners (Action.Play);
      WayPoint brk = getWayPoints().getBreakPointAfter (getTime());
      if (brk != null) {
         if (TimeBase.compare (endTime, brk.getTime()) > 0) {
            endTime = brk.getTime();
         }
      }
      synchronized (this) {
         if (myPlayer == null) {
            myPlayer = new Player (endTime);
            myPlayer.setTimeScale (myRealTimeScaling);
            myPlayer.start();
         }
         else {
            myPlayer.start(endTime);
         }
      }
   }

   public synchronized boolean requestAction (Runnable action) {
      if (myPlayer != null && myPlayer.myAlive && !myPlayer.myStopping) {
         return myPlayer.myActions.offer (action);
      }
      else {
         return false;
      }
   }

   public synchronized boolean isPlaying() {
      return isPlayerAlive();
   }

   public Exception getLastException() {
      return myLastException;
   }
   
   public Thread getThread() {
      return myPlayer;
   }      

   private void updateInitialStateIfNecessary() {
      RootModel root = getRootModel();
      if (root != null) {
         root.getWayPoints().updateInitialStateIfNecessary();
      }
//      if (!myInitialStateValidP) {
//         RootModel root = getRootModel();
//         WayPoint way0 = getWayPoint (0);
//         if (root != null && way0 != null) {
//            CompositeState state = 
//               (CompositeState)root.createState(null);
//            root.getInitialState (state, way0.getState());
//            way0.setState (state);
//         }
//         myInitialStateValidP = true;
//      }
   }

   /** 
    * Checks to see if a probe has an event at time t1 in the interval (t0,
    * min].  If t1 = min, the probe is placed in the event list for that
    * time. Otherwise, if t0 < t1 < min, then min is reset to to t1 and the
    * eventList is reset to include only this probe.
    */
   private double checkEventTime (
      ArrayList<Probe> probeList, Probe p, double t0, double min) {
      
      if (p.isActive()) {
         double t1 = p.nextEventTime (t0);
         if (t1 != -1) {
            if (TimeBase.compare (t1, min) <= 0) {
               if (TimeBase.compare (t1, min) < 0) {
                  probeList.clear();
                  if (debugStepComputation && t0 != -1) {
                     System.out.println (
                        "Step comoute: next probe event time is " + t1 + ", " +
                        p.getName()+" "+p);
                  }
                  min = t1;
               }
               probeList.add (p);
            }
         }
      }
      return min;
   }

   public double nextEvent (
      RootModel root, ArrayList<Probe> probeList, double t0, double endTime) {
      double min = endTime;

      if (myRenderProbe != null) {
         min = checkEventTime (probeList, myRenderProbe, t0, min);
      }
      if (mySleepProbe != null) {
         min = checkEventTime (probeList, mySleepProbe, t0, min);
      }
      double stepSize = getStepSize();
      if (stepSize != -1 && t0 != -1) {
         // check for the next step size boundary
         double nextStepTime = t0+(stepSize-TimeBase.modulo(t0,stepSize));
         if (TimeBase.compare (nextStepTime, min) < 0) {
            if (debugStepComputation && t0 != -1) {
               System.out.println ("Step comoute: step size is " + stepSize);
            }         
            min = nextStepTime;
            probeList.clear();
         }
      }
      if (min == t0) {
         return -1;
      }
      return TimeBase.round (min);
   }

   public void dispose() {
      if (myPlayer != null) {
         myPlayer.dispose();
         myPlayer = null;
      }
   }
}
