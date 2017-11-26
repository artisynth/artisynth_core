/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC) and Doga Tekin (ETH).
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.driver.Main.ManipulatorFrameSpec;

/**
 * 
 * @author andreio moved out the key listener to extend it
 * 
 */
public class GenericKeyHandler implements KeyListener {

   Main myMain;
   MainFrame myMainFrame;
   SelectionManager mySelectionManager;

   /**
    * Create a KeyHandler using a handle to Main
    */
   public GenericKeyHandler(Main main) {
      myMain = main;
      myMainFrame = main.getMainFrame();
      mySelectionManager = main.getSelectionManager();
   }

   public MainFrame getMainFrame() {
      return myMainFrame;
   }
   
   public void setMainFrame(MainFrame mainFrame) {
      this.myMainFrame = mainFrame;
   }
   
   private void setSelectionToSelectionParent() {
      ModelComponent c = mySelectionManager.getLastSelected();
      if (c != null && c.getParent() != null) {
         mySelectionManager.clearSelections();
         mySelectionManager.addSelected (c.getParent());
      }
   }

   public static String getKeyBindings() {
      return (
         "Keyboard bindings for ArtiSynth:\n"+
         "\n"+
         "  q   - quit\n"+
         "  t   - toggle time line visible\n" +
         "  z   - undo last command\n" +
         "  ESC - selection parent of last selection\n" +
         "\n"+
         "Play controls:\n" + 
         "  p or SPC - play/pause\n" +
         "  s   - single step\n" +
         "  r   - reset\n" +
         "\n" +
         "Viewer controls:\n" +
         "  v   - reset view\n" +
         "  o   - toggle orthographic/perspective view\n" +
         "  a   - toggle viewer axes\n" +
         "  g   - toggle viewer grid\n" +
         "  l   - toggle viewer grid labels\n" +
         "\n" +
         "Selection and manipulation:\n" +
         "  c   - clear selection\n" +
         "  d   - reset elliptical cursor size to default\n" +
         "  w   - set current manipulator frame to world coordinates\n" +
         "  b   - set current manipulator frame to body/local coordinates\n" +
         "\n" +
         "Note: you need to focus on the graphics viewer to get the bindings.");
   }

   /**
    * parsing the keys typed and performing actions
    */
   public void keyTyped (KeyEvent e) {
      switch (e.getKeyChar()) {
         case 'q':

            Main.exit (0);
            break;

         case ' ':
         case 'p':

            if (myMain.getTimeline() != null) {
               if (myMain.getScheduler().isPlaying()) {
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

            if (myMain.getTimeline() != null) {
               if (myMain.getScheduler().isPlaying()) {
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

         case 'v':

            myMainFrame.getMenuBarHandler().actionPerformed (
               new ActionEvent (this, 0, "Reset view"));
            break;

         case 't':

            if (myMain.getTimeline() != null) {
               if (myMain.getTimeline().isShowing()) {
                  myMainFrame.getMenuBarHandler().actionPerformed (
                     new ActionEvent (this, 0, "Hide timeline"));
               }
               else {
                  myMainFrame.getMenuBarHandler().actionPerformed (
                     new ActionEvent (this, 0, "Show timeline"));
               }
            }
            break;

         case 'z': {
            myMain.getUndoManager().undoLastCommand();
            break;
         }
         case 'w': {
            myMain.resetDraggerFrame (ManipulatorFrameSpec.WORLD);
            break;
         }
         case 'b': {
            myMain.resetDraggerFrame (ManipulatorFrameSpec.LOCAL);
            break;
         }
         case 'c': {
            mySelectionManager.clearSelections();
            break;
         }
         case 'd':
            myMain.getViewerManager().resetEllipticCursorSize();
            break;
         case '\033': // escape
            setSelectionToSelectionParent();
            break;
            
         default:
            //System.out.println(e.getKeyCode() + " " + e.getKeyChar());
            //System.out.println(e.getKeyCode() + " " + (int) e.getKeyChar());
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
	 myMain.getScheduler().fastForward();
	 if (myMain.getMovieMaker().isGrabbing()) {
	    try {
	       myMain.getMovieMaker().grab();
	    } catch (Exception e1) {
//	       e1.printStackTrace();
	    }
	 }
      }
      else if (code == KeyEvent.VK_LEFT) {
	 myMain.getScheduler().rewind();
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
            if (myMain.getTimeline() != null) {
               if (myMain.getScheduler().isPlaying()) {
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
