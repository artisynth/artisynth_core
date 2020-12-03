/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui;
import javax.swing.JFrame;

import artisynth.core.gui.probeEditor.NumericProbeEditor;
import artisynth.core.gui.NumericProbeDisplayLarge;
import artisynth.core.probes.WayPoint;
import artisynth.core.probes.WayPointProbe;
import artisynth.core.probes.Probe;
import artisynth.core.workspace.RootModel;

public abstract class Timeline extends JFrame {
   private static final long serialVersionUID = 1L;
   private static boolean multipleSelecting = false;
   
   //public abstract long getSingleStepTime();
   public abstract void setZoomLevel (int zoom);
   //public abstract void setSingleStepTime (long newStep);
   public abstract void resetAll();
   public abstract void automaticProbesZoom();
   public abstract void updateTimeDisplay (double t);
   public abstract void updateComponentSizes();
   public abstract void requestUpdateDisplay();
   public abstract void requestUpdateWidgets();
   public abstract void requestResetAll();
   public abstract void updateProbes(RootModel root);
   //public abstract void pauseTimeline();
   //public abstract void playTimeline();
   //public abstract void playTimeline (long endTime);
   //public abstract void rewindTimeline();
   //public abstract void stepForwardTimeline();
   //public abstract void fastForwardTimeline();
   //   public abstract void addWayPointFromRoot (WayPoint way);
   public abstract void removeWayPointFromRoot (WayPoint way);
   //public abstract void saveAllProbes();
   public abstract void addProbeEditor (NumericProbeEditor editor);
   public abstract void setAllTracksExpanded (boolean expanded);
   public abstract void updateComponentLocations();
   public abstract void repaintVisibleWindow();
   public abstract void setTrackExpanded (
      boolean expanded, boolean isInput, int modelIdx, int trackIdx);
   public abstract void setTrackMuted (
	      boolean muted, boolean isInput, int modelIdx, int trackIdx);
   //public abstract void updateToolbar ();

   public abstract boolean setWayPointsFileFromUser(JFrame frame, String text);
   public abstract boolean loadWayPointsFromAttachedFile(JFrame frame);
   public abstract boolean refreshWayPoints(RootModel root);
   public abstract void clearWayPoints();
   
   public static void setMultipleProbeSelecting (boolean selecting) {
      multipleSelecting = selecting;
   }
   
   public static boolean isMultipleProbeSelecting() {
      return multipleSelecting;
   }

   public abstract NumericProbeDisplayLarge setLargeDisplayVisible (
      Probe probe, boolean visible);
}
