/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * 
 * @author andreio moved out the key listener to extend it
 * 
 */

public class GenericKeyHandler implements ArtisynthKeyHandler {
   MainFrame myMainFrame;

   /**
    * set the default values to null, they need to be attached from the outside
    * because we don't know where this key handler will be used, in which class
    */
   public GenericKeyHandler() {
      myMainFrame = null;
   }

   public void setMainFrame (MainFrame extMainFrame) {
      myMainFrame = extMainFrame;
   }
   
   public MainFrame getMainFrame() {
      return myMainFrame;
   }

   /**
    * parsing the keys typed and performing actions
    * 
    */

   public void keyTyped (KeyEvent e) {
      switch (e.getKeyChar()) {
         case 'q':

            Main.exit (0);
            break;

         case ' ':
         case 'p':

            if (Main.getTimeline() != null) {
               if (Main.getScheduler().isPlaying()) {
                  myMainFrame.getMenuBarHandler().actionPerformed (
                     new ActionEvent (this, 0, "Pause"));
               }
               else {
                  myMainFrame.getMenuBarHandler().actionPerformed (
                     new ActionEvent (this, 0, "Play"));
               }
            }
            break;

         case 'r':

            if (Main.getTimeline() != null) {
               if (Main.getScheduler().isPlaying()) {
                  myMainFrame.getMenuBarHandler().actionPerformed (
                     new ActionEvent (this, 0, "Pause"));
               }

               myMainFrame.getMenuBarHandler().actionPerformed (
                  new ActionEvent (this, 0, "Reset"));
            }
            break;

         case 's':

            myMainFrame.getMenuBarHandler().actionPerformed (
               new ActionEvent (this, 0, "Single step"));
            break;

         case 'w':

            myMainFrame.getMenuBarHandler().actionPerformed (
               new ActionEvent (this, 0, "Reset view"));
            break;

         case 't':

            if (Main.getTimeline() != null) {
               if (Main.getTimeline().isShowing()) {
                  myMainFrame.getMenuBarHandler().actionPerformed (
                     new ActionEvent (this, 0, "Hide timeline"));
               }
               else {
                  myMainFrame.getMenuBarHandler().actionPerformed (
                     new ActionEvent (this, 0, "Show timeline"));
               }
            }
            break;

         case 'z':
            Main.getUndoManager().undoLastCommand();
            break;

         default:
            // System.out.print(e.getKeyCode() + " " + (int) e.getKeyChar());
      }
   }

   /**
    * key press events for the program moved out of GLViewer to clean up code in
    * GL viewer. This function belongs here
    */

   public void keyPressed(KeyEvent e) {
      int code = e.getKeyCode();
      // if (code >= 0 && code <= maxKeyCode) {
      //    myKeyPressed[code] = true;
      // }
      if (code == KeyEvent.VK_RIGHT) {
	 Main.getScheduler().fastForward();
	 if (Main.getMovieMaker().isGrabbing()) {
	    try {
	       Main.getMovieMaker().grab();
	    } catch (Exception e1) {
//	       e1.printStackTrace();
	    }
	 }
      }
      else if (code == KeyEvent.VK_LEFT) {
	 Main.getScheduler().rewind();
      }
   }

   public void keyReleased (KeyEvent e) {
      // int code = e.getKeyCode();
      // if (code >= 0 && code <= maxKeyCode) {
      //    myKeyPressed[code] = false;
      // }
      
      int code = e.getKeyCode();
      int mods = e.getModifiersEx();
      
      // Ctrl + Shift + Backspace for reload
      if ((mods & (KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)) == 
         (KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)) {
         if (code == KeyEvent.VK_BACK_SPACE) {
            // Pause if playing
            if (Main.getTimeline() != null) {
               if (Main.getScheduler().isPlaying()) {
                  myMainFrame.getMenuBarHandler().actionPerformed (
                     new ActionEvent (this, 0, "Pause"));
               }
            }
            myMainFrame.getMenuBarHandler().actionPerformed(
               new ActionEvent(this, 0, "Reload model"));
         }
      }
   }
}
