/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import java.awt.*;
import javax.swing.*;
import java.beans.*;

/**
 * A that sets up a progress bar to monitor file transfer progress.
 */
public class ProgressBarTransferListener implements FileTransferListener {

   String myBarMessagePrefix; // message to preceed file name of progress bar
   Window myWindow; // optional window to contain the progress panel
   double myStartTime = 0; // transfer start time (sec)
   double myLaunchDelay = 0.5; // time (sec) to wait before launching panel
   double myLaunchThreshold = 2.0; // ETA (sec) required to launch panel
   String myFileName = null; // name of file being transfered
   boolean myPanelLaunched = false; // true once panel has been launched

   int myPercent = 0; // current transfer percentage

   public ProgressBarTransferListener (String messagePrefix, Window window) {
      myBarMessagePrefix = messagePrefix;
      myWindow = window;
   }

   /**
    * Returns the time in seconds that we should wait before launching the
    * progress panel.
    */
   public double getLaunchDelay () {
      return myLaunchDelay;
   }
         
   /**
    * Sets the time in seconds that we should wait before launching the
    * progress panel.
    */
   public void setLaunchDelay (double delay) {
      myLaunchDelay = delay;
   }

   /**
    * Returns the ETA in seconds required to launch the progress panel.
    */
   public double getLaunchThreshold () {
      return myLaunchThreshold;
   }
         
   /**
    * Sets the ETA in seconds required to launch the progress panel.
    */
   public void setLaunchThreshold (double eta) {
      myLaunchThreshold = eta;
   }

   private class ProgressPanel extends JPanel implements PropertyChangeListener {
      private static final long serialVersionUID = 1L;
      JFrame myFrame;
      JProgressBar myBar;
      JLabel myLabel;

      /**
       * Monitor class that polls progress percentage and updates the progress
       * bar accordingly. Ideally, we would like to simply set the progress bar
       * value in the transferUpdated callback, but we can't do that because
       * the value can only be set from within the GUI dispatch thread.  Hence
       * we have to go throw a complicated mechanism involving this polling
       * task, which in turn sets a "progress" property that results in a
       * propertyChange method being called in the GUI dispatch thread.
       *
       * There really ought to be an easier way to do this!
       */
      private class Monitor extends SwingWorker<Void, Void> {
         /**
          * Executed in a background thread.
          */
         @Override
            public Void doInBackground() {
            int percent = getPercent();
            //Initialize progress property.
            setProgress(percent);
            while (percent < 100) {
               try {
                  Thread.sleep(100);
               }
               catch (InterruptedException ignore) {
               }
               int p = getPercent();
               if (p > percent) {
                  percent = p;
                  setProgress(percent);
               }
            }
            return null;
         }

         /*
          * Executed in GUI dispatch thread
          */
         @Override
            public void done() {
            myFrame.dispose();
         }
      }

      ProgressPanel (JFrame frame, String labelText) {
         super();
         setLayout (new BoxLayout (this, BoxLayout.Y_AXIS));
         myLabel = new JLabel (labelText);
         myLabel.setAlignmentX (Component.CENTER_ALIGNMENT);
         myBar = new JProgressBar(0, 100);
         myBar.setAlignmentX (Component.CENTER_ALIGNMENT);
         myBar.setValue (0);
         myBar.setStringPainted(true);
         setOpaque(true); //content panes must be opaque
         add (myLabel);
         add (Box.createRigidArea(new Dimension (0, 4)));
         add (myBar);
         setBorder (BorderFactory.createEmptyBorder (4, 4, 4, 4));
         myFrame = frame;
      }

      public void start() {
         Monitor mon = new Monitor();
         mon.addPropertyChangeListener(this);
         mon.execute();        
      }

      /**
       * Invoked when task's progress property changes. Executed in GUI
       * dispatch thread.
       */
      public void propertyChange(PropertyChangeEvent evt) {
         if ("progress" == evt.getPropertyName()) {
            int progress = (Integer) evt.getNewValue();
            myBar.setValue(progress);
         }
      }
   }

   private void launchProgressPanel () {
      // Create and set up the window.
      JFrame frame = new JFrame("Transfer Progress");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      // Create and set up the content pane.
      ProgressPanel panel =
         new ProgressPanel(frame, myBarMessagePrefix + myFileName + ":");

      frame.setContentPane(panel);

      // Display the window.
      frame.pack();
      if (myWindow != null) {
         frame.setLocationRelativeTo (myWindow) ;
      }
      frame.setVisible(true);    
      panel.start();
   }

   /**
    * Used by the progress panel to get the transfer completion percentage.
    */
   private synchronized int getPercent() {
      return myPercent;
   }

   /**
    * Used by the listener update callback to set the transfer completion
    * percentage for use by the progress panel.
    */
   private synchronized void setPercent (int p) {
      myPercent = p;
   }
   
   /**
    * Called while the file transfer is in progress.
    */
   public void transferUpdated(FileTransferEvent event) {

      double pp = event.getProgress();
      double timeDiff = (event.getEventTime()/1000 - myStartTime);
      double eta = -1;
      
      if (pp > 0) {
         eta = timeDiff/pp; // estimated total time
      }

      setPercent ((int)(100*pp));
      // if enough time has elapsed, and the ETA is long enough,
      // launch the progress panel
      if (!myPanelLaunched) {
         if (eta > myLaunchThreshold && timeDiff > myLaunchDelay) {
            // use invokeLater because the panel has to be created
            // and launched within the GUI event thread.
            SwingUtilities.invokeLater (new Runnable() {
                  public void run() {
                     launchProgressPanel();
                  }
               });
            myPanelLaunched = true;
         }
      }
   }

   /**
    * Called when the file transfer begins.
    */
   public void transferStarted (FileTransferEvent event) {
      myFileName = event.getSourceFile().getName().getBaseName();
      myStartTime = event.getEventTime()/1000;
   }

   /**
    * Called when the file transfer is done.
    */
   public void transferCompleted(FileTransferEvent event) {
      transferUpdated(event); // do final update
   }
}
