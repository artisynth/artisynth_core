package artisynth.core.gui.timeline;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.ImageIcon;

import maspack.widgets.GuiUtils;

public class GuiStorage {
   private static ImageIcon loadIcon (String name) {
      return GuiUtils.loadIcon (GuiStorage.class, "BasicIcon/" + name);
   }

   protected static final ImageIcon INPUT_TRACK_ICON =
      loadIcon ("InputTrack16.jpg");
   protected static final ImageIcon OUTPUT_TRACK_ICON =
      loadIcon ("OutputTrack16.jpg");

   protected static final ImageIcon EXPAND_TRACK_ICON =
      loadIcon ("ExpandTrack17.jpg");
   protected static final ImageIcon CONTRACT_TRACK_ICON =
      loadIcon ("ContractTrack17.jpg");
   protected static final ImageIcon MUTE_TRACK_ICON =
      loadIcon ("MuteTrack17.jpg");
   protected static final ImageIcon UNMUTE_TRACK_ICON =
      loadIcon ("UnmuteTrack17.jpg");

   protected static final ImageIcon SHOW_TRACKS_ICON = loadIcon ("Show.gif");
   protected static final ImageIcon HIDE_TRACKS_ICON = loadIcon ("Hide.gif");

   protected static final ImageIcon ZOOM_IN_ICON = loadIcon ("ZoomIn.gif");
   protected static final ImageIcon ZOOM_OUT_ICON = loadIcon ("ZoomOut.gif");

   protected static final ImageIcon NAVBAR_ICON = loadIcon ("navPanel.gif");
   protected static final ImageIcon PLAY_ICON = loadIcon ("Play.gif");
   protected static final ImageIcon PAUSE_ICON = loadIcon ("Pause.gif");
   protected static final ImageIcon RESET_ICON = loadIcon ("Reset.gif");
   protected static final ImageIcon REWIND_ICON = loadIcon ("Rewind.gif");
   protected static final ImageIcon STEP_BACK_ICON = loadIcon ("StepBack.gif");
   protected static final ImageIcon STEP_FORWARD_ICON =
      loadIcon ("StepForward.gif");
   protected static final ImageIcon FAST_FORWARD_ICON =
      loadIcon ("FastForward.gif");
   protected static final ImageIcon SAVE_ALL_PROBES_ICON =
      loadIcon ("Save.gif");

   protected static final ImageIcon STATIC_SOLVE_ICON =
      loadIcon ("Equilibrium.gif");
   protected static final ImageIcon STATIC_STEP_ICON =
      loadIcon ("EquilibriumStep.gif");
   protected static final ImageIcon ADD_ICON = loadIcon ("Add.png");

   // =======================================================
   // color initialization
   protected static final Color COLOR_MODEL_WORKSPACE_NORMAL =
      new Color (235, 235, 235);
   protected static final Color COLOR_MODEL_WORKSPACE_HIGHLIGHT =
      new Color (215, 215, 215);
   protected static final Color COLOR_MODEL_TIMELINE =
      new Color (228, 228, 228);

   protected static final Color COLOR_INPUTTRACK_WORKSPACE =
      new Color (237, 245, 253);
   protected static final Color COLOR_OUTPUTTRACK_WORKSPACE =
      new Color (254, 242, 230);

   protected static final Color COLOR_PROBE_TRACK = new Color (242, 242, 242);

   protected static final Color COLOR_INPUTPROBE_BEFORE_NORMAL =
      new Color (97, 174, 237);
   protected static final Color COLOR_INPUTPROBE_BEFORE_HIGHLIGHT =
      new Color (187, 223, 253);
   protected static final Color COLOR_INPUTPROBE_AFTER_NORMAL =
      new Color (66, 111, 185);
   protected static final Color COLOR_INPUTPROBE_AFTER_HIGHLIGHT =
      new Color (125, 159, 210);

   protected static final Color COLOR_OUTPUTPROBE_BEFORE_NORMAL =
      new Color (246, 133, 130);
   protected static final Color COLOR_OUTPUTPROBE_BEFORE_HIGHLIGHT =
      new Color (251, 208, 207);
   protected static final Color COLOR_OUTPUTPROBE_AFTER_NORMAL =
      new Color (192, 63, 75);
   protected static final Color COLOR_OUTPUTPROBE_AFTER_HIGHLIGHT =
      new Color (244, 143, 150);

   protected static final Color COLOR_PROBE_MUTED_NORMAL =
      new Color (140, 140, 140);
   protected static final Color COLOR_PROBE_MUTED_HIGHLIGHT =
      new Color (210, 210, 210);
   
   protected static final Color COLOR_PROBE_SELECTED_NORMAL =
      new Color (248, 227, 116);   
   protected static final Color COLOR_PROBE_SELECTED_HIGHLIGHT =
      new Color (248, 241, 167);

   protected static final Color COLOR_WAYPOINT_INVALID_NORMAL =
      new Color (255, 43, 43);
   protected static final Color COLOR_WAYPOINT_INVALID_HIGHLIGHT =
      new Color (255, 170, 170);
   protected static final Color COLOR_WAYPOINT_VALID_NORMAL =
      new Color (0, 0, 125);
   protected static final Color COLOR_WAYPOINT_VALID_HIGHLIGHT =
      new Color (125, 125, 255);

   protected static final Color COLOR_TIMESCALE_BACKGROUND =
      new Color (230, 230, 230);

   // =======================================================
   // dimension and size initialization

   protected static final int TRACK_NORMAL_HEIGHT = 20;
   protected static final int TRACK_EXPAND_HEIGHT = 100;
   // arbitrary value
   protected static final int DEFAULT_TRACK_LENGTH = 1000;

   protected static final Dimension ICON_SIZE = new Dimension (18, 18);
   protected static final Dimension BUTTON_SIZE_SMALL = new Dimension (25, 25);
   protected static final Dimension BUTTON_SIZE_LARGE = new Dimension (32, 32);
   protected static final Dimension TOGGLE_BUTTON_SIZE = new Dimension (18, 18);
   protected static final Dimension WAY_MARKER_SIZE = new Dimension (5, 18);
   protected static final Dimension VIRTUAL_WAY_SIZE = new Dimension (3, 18);

   protected static final int PROBE_NORMAL_HEIGHT = 18;
   protected static final int PROBE_EXPAND_HEIGHT = 98;
   protected static final int PROBE_LABEL_HEIGHT = PROBE_NORMAL_HEIGHT;
   protected static final int PROBE_DETAIL_HEIGHT =
      PROBE_EXPAND_HEIGHT - PROBE_LABEL_HEIGHT;

   protected static final int TIMELINE_PANEL_WIDTH = 125;
   
   protected static final double[] ZOOM_LEVELS = { 
      0, 30, 12, 6, 3, 1.2, 
      0.6, 0.3, 0.12, 0.06, 0.03, 0.012, 0.006 };

   // =======================================================
   // font initialization
   protected static final Font TIMESCALE_FONT =
      new Font ("Arial", Font.PLAIN, 9);
   protected static final Font TOOLBAR_FONT =
      new Font ("New Gothic", Font.BOLD, 11);

   public static ImageIcon getNavBarIcon() {
      return NAVBAR_ICON;
   }

   public static ImageIcon getPlayIcon() {
      return PLAY_ICON;
   }

   public static ImageIcon getPauseIcon() {
      return PAUSE_ICON;
   }

   public static ImageIcon getResetIcon() {
      return RESET_ICON;
   }

   public static ImageIcon getRewindIcon() {
      return REWIND_ICON;
   }

   public static ImageIcon getStepForwardIcon() {
      return STEP_FORWARD_ICON;
   }

   public static ImageIcon getFastForwardIcon() {
      return FAST_FORWARD_ICON;
   }

   public static ImageIcon getStaticSolveIcon() {
      return STATIC_SOLVE_ICON;
   }

   public static ImageIcon getStaticStepIcon() {
      return STATIC_STEP_ICON;
   }

   public static ImageIcon getAddIcon() {
      return ADD_ICON;
   }

//   public static Dimension getButtonSize (boolean large) {
//      if (large) {
//         return BUTTON_SIZE_LARGE;
//      }
//      else {
//         return BUTTON_SIZE_SMALL;
//      }
//   }
}
