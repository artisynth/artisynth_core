/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import maspack.render.GL.GLViewer;

/**
 * Class that listens to key events, and executes a scroll-up, scroll-down or
 * fullscreen toggle for a HudPrintStream object.
 * 
 * @author Antonio
 * 
 */
public class HudKeyListener implements KeyListener {

   public static KeyCombo defaultScrollUpKeys =
      new KeyCombo(KeyEvent.VK_UP, (char)0, 0);
   public static KeyCombo defaultScrollDownKeys =
      new KeyCombo(KeyEvent.VK_DOWN, (char)0, 0);
   public static KeyCombo defaultFullscreenKeys =
      new KeyCombo(0, '`', 0);

   HudPrintStream myHud;
   GLViewer myViewer;
   KeyCombo myScrollUpKeys = defaultScrollUpKeys;
   KeyCombo myScrollDownKeys = defaultScrollDownKeys;
   KeyCombo myFullscreenKeys = defaultFullscreenKeys;

   private static class KeyCombo {
      int keyCode;
      char keyChar;
      int keyMods;

      public KeyCombo (int code, char ch, int mods) {
         keyCode = code;
         keyChar = ch;
         keyMods = mods;
      }

      public boolean matches(int code, char ch, int mods) {
         if ((code != 0 && code == keyCode && keyMods == mods) ||
            (ch == keyChar && keyMods == mods)) {
            return true;
         } else {
            return false;
         }
      }

   }

   /**
    * Creates a key listener for a HudPrintStream object
    * 
    * @param hud
    * the HudPrintStream object to control
    */
   public HudKeyListener (HudPrintStream hud) {
      myHud = hud;
      myViewer = null;
   }

   /**
    * Creates a key listener for a HudPrintStream object
    * 
    * @param hud
    * the HudPrintStream object to control
    * @param viewer
    * (optional) a viewer used to trigger re-render events after changes
    */
   public HudKeyListener (HudPrintStream hud, GLViewer viewer) {
      myHud = hud;
      myViewer = viewer;
   }

   @Override
   public void keyPressed(KeyEvent e) {
      if (myScrollDownKeys.matches(
         e.getKeyCode(), e.getKeyChar(), e.getModifiersEx())) {
         myHud.scrollDown();
         rerender();
      } else if (myScrollUpKeys.matches(
         e.getKeyCode(), e.getKeyChar(), e.getModifiersEx())) {
         myHud.scrollUp();
         rerender();
      }
   }

   @Override
   public void keyReleased(KeyEvent e) {
   }

   @Override
   public void keyTyped(KeyEvent e) {
      if (myFullscreenKeys.matches(
         e.getKeyCode(), e.getKeyChar(), e.getModifiersEx())) {
         myHud.toggleFullscreen();
         rerender();
      }
   }
   
   private void rerender() {
	   if (myViewer != null) {
		   myViewer.rerender();
	   }
   }

   /**
    * Set key combination for scrolling up, executed in the keyPressed event. If
    * either the keyCode or the keyChar match, and if the modifiers match, the
    * command is executed.
    * 
    * @param keyCode
    * numeric code
    * @param keyChar
    * (optional) character
    * @param keyModifiers
    * modifiers
    */
   public void setScrollUpCombo(int keyCode, char keyChar, int keyModifiers) {
      myScrollUpKeys = new KeyCombo(keyCode, keyChar, keyModifiers);
   }

   /**
    * Set key combination for scrolling down, executed in the keyPressed event.
    * If either the keyCode or the keyChar match, and if the modifiers match,
    * the command is executed.
    * 
    * @param keyCode
    * numeric code
    * @param keyChar
    * (optional) character
    * @param keyModifiers
    * modifiers
    */
   public void setScrollDownCombo(int keyCode, char keyChar, int keyModifiers) {
      myScrollDownKeys = new KeyCombo(keyCode, keyChar, keyModifiers);
   }

   /**
    * Set key combination for toggling fullscreen, executed in the keyTyped
    * event. If either the keyCode or the keyChar match, and if the modifiers
    * match, the command is executed.
    * 
    * @param keyCode
    * numeric code
    * @param keyChar
    * (optional) character
    * @param keyModifiers
    * modifiers
    */
   public void setFullscreenCombo(int keyCode, char keyChar, int keyModifiers) {
      myFullscreenKeys = new KeyCombo(keyCode, keyChar, keyModifiers);
   }

   /**
    * Creates and registers a HudKeyListener object
    * 
    * @param hud
    * the HudPrintStream to control
    * @param comp
    * the component on which to listen for key events
    * @return the created HudKeyListener
    */
   public static HudKeyListener createListener(HudPrintStream hud,
      Component comp) {
      HudKeyListener kl = new HudKeyListener(hud);
      if (comp != null) {
         comp.addKeyListener(kl);
      }
      return kl;
   }

//   /**
//    * Creates and registers a HudKeyListener object
//    * 
//    * @param hud
//    * the HudPrintStream to control
//    * @param comp
//    * the component on which to listen for key events
//    * @param renderer
//    * renderer object for triggering rerender events
//    * @return the created HudKeyListener
//    */
//   public static HudKeyListener createListener(HudPrintStream hud,
//      Component comp, Renderer renderer) {
//      HudKeyListener kl = new HudKeyListener(hud, renderer);
//      if (comp != null) {
//         comp.addKeyListener(kl);
//      }
//      return kl;
//   }

   /**
    * Creates and registers a HudKeyListener object
    * 
    * @param hud
    * the HudPrintStream to control
    * @param viewer
    * the component on which to listen for key events/re-render
    * @return the created HudKeyListener
    */
   public static HudKeyListener createListener(HudPrintStream hud,
      GLViewer viewer) {
      HudKeyListener kl = new HudKeyListener(hud, viewer);
      if (viewer != null) {
         viewer.addKeyListener(kl);
      }
      return kl;
   }

}
