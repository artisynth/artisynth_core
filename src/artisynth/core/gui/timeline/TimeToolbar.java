package artisynth.core.gui.timeline;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import maspack.util.NumberFormat;
import maspack.widgets.ButtonCreator;
import artisynth.core.driver.GenericKeyHandler;
import artisynth.core.driver.Main;
import artisynth.core.workspace.RootModel;

public class TimeToolbar extends JToolBar {
   private static final long serialVersionUID = 1L;
   
   //private JPanel timestepPanel;
   private JLabel timeLabel;
   private TimelineController parent;
   //private JTextField timestepTextField;

   private JButton zoomInButton;
   private JButton zoomOutButton;
   private JButton resetButton;
   private JButton fastBackwardButton;
   private JButton playButton;
   private JButton fastForwardButton;
   private JButton singleStepButton;

   private double myStepTime = 0;

   // // updates the stepTime into the timestep field
   // void updateStepTime () {
   //    double newStep = parent.myScheduler.getStepTime();
   //    if (newStep != myStepTime) {
   //       getTimestepTextField().setText (Double.toString (newStep*1000));
   //       myStepTime = newStep;
   //    }
   // }

   double getStepTime () {
      return myStepTime;
   }      

   private void createTimeToolbar (TimelineController parent) {
      this.parent = parent;
      //timestepPanel = new JPanel();
      
      JPanel timeDisplay = new JPanel();
      timeDisplay.setLayout (new BoxLayout (timeDisplay, BoxLayout.X_AXIS));
      timeDisplay.setOpaque (false);
      timeDisplay.setFont (GuiStorage.TOOLBAR_FONT);
      
      timeLabel = new JLabel();
      timeLabel.setBackground (Color.WHITE);
      timeLabel.setOpaque (true);
      timeLabel.setBorder (BorderFactory.createLineBorder (Color.LIGHT_GRAY));
      timeLabel.setHorizontalAlignment (SwingConstants.RIGHT);
      timeLabel.setFont (GuiStorage.TOOLBAR_FONT);
      
      JLabel textLabel = new JLabel ("Current Time: ");
      textLabel.setFont (GuiStorage.TOOLBAR_FONT);
      timeDisplay.add (textLabel);
      timeDisplay.add (timeLabel);

      Dimension frameDimension = new Dimension (80, 15);
      timeLabel.setSize (frameDimension);
      timeLabel.setMinimumSize (frameDimension);
      timeLabel.setMaximumSize (frameDimension);
      timeLabel.setPreferredSize (frameDimension);
      
      updateTimeDisplay (0);
      
      //timestepTextField = new JTextField();
      
      zoomInButton = makeButton (
         GuiStorage.ZOOM_IN_ICON,
         "Zoom in", "Zoom in");
      add (zoomInButton);

      zoomOutButton = makeButton (
         GuiStorage.ZOOM_OUT_ICON,
         "Zoom out", "Zoom out");
      add (zoomOutButton);
      addSeparator();

      resetButton = makeButton (
         GuiStorage.RESET_ICON,
         "Reset", "Reset time to 0");
      add (resetButton);

      fastBackwardButton = makeButton (
         GuiStorage.REWIND_ICON,
         "Skip back", "Skip back to previous valid waypoint");
      add (fastBackwardButton);

      playButton = makeButton (
         GuiStorage.PLAY_ICON,
         "Play", "Start simulation");
      add (playButton);

      singleStepButton = makeButton (
         GuiStorage.STEP_FORWARD_ICON,
         "Single step", "Advance simulation by a single time step");
      add (singleStepButton);

      fastForwardButton = makeButton (
         GuiStorage.FAST_FORWARD_ICON,
         "Skip forward", "Skip forward to next valid waypoint");
      add (fastForwardButton);
      addSeparator();

      // add (makeButton(GuiStorage.SET_ICON, "Set"));
      // addSeparator();

      add (makeButton (GuiStorage.SAVE_ALL_PROBES_ICON,
                       "Save output probe data", "Save output probe data"));

      //timestepInitialization();

      // add appropriate spaces
      addSeparator (new Dimension (20, 0));

      add (timeDisplay);
      // addSeparator (new Dimension (20, 0));
      // add (timestepPanel);
   }

   public TimeToolbar (TimelineController parent) {
      super();
      createTimeToolbar (parent);
   }

   // public JTextField getTimestepTextField() {
   //    return timestepTextField;
   // }

   public JButton makeButton (ImageIcon icon, String command, String toolTip) {
      JButton button = new JButton (icon);
      button.setToolTipText (toolTip);
      button.setSize (ButtonCreator.SMALL_BUTTON_SIZE);
      button.setMinimumSize (ButtonCreator.SMALL_BUTTON_SIZE);
      button.setMaximumSize (ButtonCreator.SMALL_BUTTON_SIZE);
      button.setPreferredSize (ButtonCreator.SMALL_BUTTON_SIZE);

      button.setActionCommand (command);
      button.addActionListener (new TimelineButtonListener());

      GenericKeyHandler keyHandler = new GenericKeyHandler(parent.myMain);
      
      button.addKeyListener (keyHandler);
      return button;
   }

   // private void timestepInitialization() {
   //    Dimension fieldSize = new Dimension (48, 19);
   //    timestepTextField.setSize (fieldSize);
   //    timestepTextField.setMinimumSize (fieldSize);
   //    timestepTextField.setMaximumSize (fieldSize);
   //    timestepTextField.setPreferredSize (fieldSize);
   //    timestepTextField.setHorizontalAlignment (SwingConstants.RIGHT);
   //    timestepTextField.setFont (GuiStorage.TOOLBAR_FONT);
   //    timestepTextField.setBorder (
   //       BorderFactory.createLineBorder (Color.LIGHT_GRAY));

   //    timestepTextField.getDocument().addDocumentListener (
   //       new TimestepListener());

   //    timestepPanel.setLayout (new BoxLayout (timestepPanel, BoxLayout.X_AXIS));
   //    timestepPanel.setOpaque (false);
   //    timestepPanel.setFont (GuiStorage.TOOLBAR_FONT);

   //    JLabel TimeStepLabel = new JLabel ("Time Step: ");
   //    JLabel msecLabel = new JLabel ("msec");
   //    TimeStepLabel.setFont (GuiStorage.TOOLBAR_FONT);
   //    msecLabel.setFont (GuiStorage.TOOLBAR_FONT);

   //    timestepPanel.add (TimeStepLabel);
   //    timestepPanel.add (timestepTextField);
   //    timestepPanel.add (msecLabel);
   // }

   private void refreshToolbar(RootModel root) {
      boolean timeIsZero = (parent.myScheduler.getTime() == 0);
      if (!parent.myScheduler.isPlaying ()) {
         //resetButton.setEnabled (!timeIsZero);
         fastBackwardButton.setEnabled (!timeIsZero);
         playButton.setIcon (GuiStorage.PLAY_ICON);
         playButton.setActionCommand ("Play");
         playButton.setToolTipText ("Start simulation");
         singleStepButton.setEnabled (true);
         fastForwardButton.setEnabled (parent.isNextValidWayAvailable(root));
      }
      else {
         //resetButton.setEnabled (true);
         fastBackwardButton.setEnabled (false);
         playButton.setIcon (GuiStorage.PAUSE_ICON);
         playButton.setActionCommand ("Pause");
         playButton.setToolTipText ("Pause simulation");
         singleStepButton.setEnabled (false);
         fastForwardButton.setEnabled (false);
      }
   }

   NumberFormat timefmt = new NumberFormat ("%9.6f");

   public void updateTimeDisplay (double t) {
      timeLabel.setText (timefmt.format(t));
   }

   public void updateToolbarState (RootModel root) {      
      //updateStepTime();
      validateZoom();      
      refreshToolbar (root);
      validateFastForward(root);
   }

   public void validateFastForward(RootModel root) {
      // For all buttons check to see if there is a valid
      // waypoint existing after the current time, then
      // enable the button.
      boolean enabled = parent.isNextValidWayAvailable(root);
      if (enabled != fastForwardButton.isEnabled()) {
         fastForwardButton.setEnabled (enabled);
      }
   }

   public void validateZoom() {
      boolean enabled;
      int zoomLevel = parent.getZoomLevel();
      enabled = (zoomLevel != TimelineConstants.MINIMUM_ZOOM);
      if (zoomOutButton.isEnabled() != enabled) {
         zoomOutButton.setEnabled (enabled);
      }
      enabled = (zoomLevel != TimelineConstants.MAXIMUM_ZOOM);
      if (zoomInButton.isEnabled() != enabled) {
         zoomInButton.setEnabled (enabled);
      }
   }

   // //====================================================
   // // TimestepListener class
   // //====================================================

   // public class TimestepListener implements DocumentListener {
   //    public void insertUpdate (DocumentEvent e) {
   //       String input = getTimestepTextField().getText().trim();
   //       double newStepTime;
   //       try { // input in milli-sec
   //          newStepTime = 0.001 * Double.parseDouble (input);
   //          myStepTime = newStepTime;
   //          parent.setStepTime (newStepTime);
   //       }
   //       catch (NumberFormatException exception) {}
   //    }

   //    public void removeUpdate (DocumentEvent e) {
   //       String input = getTimestepTextField().getText().trim();
   //       double newStepTime;
   //       try { // input in milli-sec
   //          newStepTime = 0.001 * Double.parseDouble (input);
   //          myStepTime = newStepTime;
   //          parent.setStepTime (newStepTime);
   //       }
   //       catch (NumberFormatException exception) {}
   //    }

   //    public void changedUpdate (DocumentEvent e) {}
   // }
   
   //========================================================
   // TimelineButtonListener class
   //========================================================

   public class TimelineButtonListener implements ActionListener {
      public void actionPerformed (ActionEvent e) {
         String nameOfAction = e.getActionCommand();

         if (parent.rootModelExists() == false) {
            return;
         }
         else if (nameOfAction == "Reset") {
            parent.myScheduler.reset();            
            //parent.myScheduler.rewind();
         }
         else if (nameOfAction == "Skip back") {       
            parent.myScheduler.rewind();
         }
         else if (nameOfAction == "Play") {
            parent.myScheduler.play();
         }
         else if (nameOfAction == "Pause") {
            parent.myScheduler.pause();
            //parent.pauseTimeline();
         }
         else if (nameOfAction == "Single step") {
            parent.myScheduler.step();
         }
         else if (nameOfAction == "Skip forward") {
            parent.myScheduler.fastForward();
         }
         else if (nameOfAction == "Zoom in") {
            parent.zoom (TimelineConstants.ZOOM_IN);
         }
         else if (nameOfAction == "Zoom out") {
            parent.zoom (TimelineConstants.ZOOM_OUT);
         }
         else if (nameOfAction == "Set") {
            parent.setAllInputProbes();
         }
         else if (nameOfAction == "Save output probe data") {
            parent.saveOutputProbeData();
         }
         
         updateToolbarState(parent.myMain.getRootModel());
         parent.requestUpdateDisplay();
      }
   }
}
