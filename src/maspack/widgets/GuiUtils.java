/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicSliderUI;

import maspack.util.PathFinder;

public class GuiUtils {
   private GuiUtils() {
   }

   /**
    * Align centers.
    */
   public static final int CENTER = 1;

   /**
    * Align left edges.
    */
   public static final int LEFT_EDGES = 2;

   /**
    * Align right edges.
    */
   public static final int RIGHT_EDGES = 3;

   /**
    * Place one component completely to the left of another.
    */
   public static final int LEFT = 4;

   /**
    * Place one component completely to the right of another.
    */
   public static final int RIGHT = 5;

   /**
    * Align top edges.
    */
   public static final int TOP_EDGES = 6;

   /**
    * Align bottom edges.
    */
   public static final int BOTTOM_EDGES = 7;

   /**
    * Place one component above another.
    */
   public static final int ABOVE = 8;

   /**
    * Place one component below another.
    */
   public static final int BELOW = 9;

   /**
    * Sets the size of a component to be a rigidly fixed as we possibly can.
    */
   public static void setFixedSize (Component comp, Dimension size) {
      comp.setPreferredSize (size);
      comp.setMaximumSize (size);
      comp.setMinimumSize (size);
   }

   /**
    * Sets the size of a component to be a rigidly fixed as we possibly can.
    */
   public static void setFixedSize (Component comp, int w, int h) {
      Dimension size = new Dimension (w, h);
      comp.setPreferredSize (size);
      comp.setMaximumSize (size);
      comp.setMinimumSize (size);
   }

   /**
    * Sets the width of a component to a rigidly fixed value
    */
   public static void setFixedWidth (Component comp, int w) {
      Dimension size = comp.getPreferredSize();
      size.width = w;
      comp.setPreferredSize (new Dimension (size));
      size.height = Integer.MAX_VALUE;
      comp.setMaximumSize (new Dimension (size));
      size.height = 0;
      comp.setMinimumSize (new Dimension (size));
   }

   /**
    * Sets the height of a component to a rigidly fixed value
    */
   public static void setFixedHeight (Component comp, int h) {
      Dimension size = comp.getPreferredSize();
      size.height = h;
      comp.setPreferredSize (new Dimension (size));
      size.width = Integer.MAX_VALUE;
      comp.setMaximumSize (new Dimension (size));
      size.width = 0;
      comp.setMinimumSize (new Dimension (size));
   }

   public static Component createBoxFiller() {
      Dimension min = new Dimension (0, 0);
      Dimension max = new Dimension (Integer.MAX_VALUE, Integer.MAX_VALUE);
      return new Box.Filler (min, min, max);
   }

   /**
    * Returns the window containing a component, or the component itself if the
    * component is a window.
    * 
    * @param comp
    * @return window associated with this component
    */
   private static Window windowForComponent (Component comp) {
      if (comp instanceof Window) {
         return (Window)comp;
      }
      else if (comp != null) {
         return SwingUtilities.windowForComponent (comp);
      }
      else {
         return null;
      }
   }

   // /**
   // * Locates a window as close as possible to a specified reference
   // component,
   // * while making sure that it does not overlap the window containing the
   // * reference component. This is normaly done by placing the window to the
   // * left of the reference window, unless this will cause it to run off the
   // * edge of the screen, in which case it is placed to the right.
   // *
   // * @param win window to be located
   // * @param ref reference component
   // */
   // public static void locateRight (Window win, Component ref)
   // {
   // Window refWin;
   // Point compLoc;
   // if (ref instanceof Window)
   // { refWin = (Window)ref;
   // compLoc = new Point();
   // }
   // else
   // { refWin = SwingUtilities.windowForComponent (ref);
   // compLoc = SwingUtilities.convertPoint (ref, 0, 0, refWin);
   // }
   // if (refWin == null)
   // { return;
   // }
   // Point refLoc = refWin.getLocation();
   // Dimension refSize = refWin.getSize();
   //
   // Point newLoc = new Point (refLoc.x+refSize.width, refLoc.y+compLoc.y);
   //
   // // see if it fits on the current screen and adjust it it does not
   // GraphicsDevice dev = ref.getGraphicsConfiguration().getDevice();
   // int scrWidth = dev.getDisplayMode().getWidth();
   // int scrHeight = dev.getDisplayMode().getHeight();
   // Dimension winSize = win.getSize();
   // if (newLoc.x+winSize.width > scrWidth)
   // { newLoc.x = refLoc.x - winSize.width;
   // }
   // if (newLoc.y+winSize.height > scrHeight)
   // { newLoc.y = scrHeight- winSize.height;
   // }
   // win.setLocation (newLoc);
   // }

   /**
    * Gets the current screen location of a specified point within a reference
    * component. This can then be used for positioning windows relative to that
    * component. The location of the point relative to the component is
    * specified using coordinates xr and yr. These are normalized for the
    * component's size, so that (0,0) indicates the top left, (1,1) indicates
    * the lower right, etc.
    * 
    * @param ref
    * reference component
    * @param xr
    * normalized horizontal point location
    * @param yr
    * normalized vertical point location
    * @return point location in screen coordinates
    */
   public static Point getScreenLocation (Component ref, double xr, double yr) {
      Window refWin; // window containing the reference component
      Point refLoc; // component location relative to its window
      Point pntLoc; // point location in screen coordinates
      Dimension refSize; // size of the reference component

      refWin = windowForComponent (ref);
      refSize = ref.getSize();
      if (refWin == null) {
         pntLoc = ref.getLocation();
      }
      else {
         pntLoc = SwingUtilities.convertPoint (ref, 0, 0, refWin);
         Point refWinLoc = refWin.getLocation();
         pntLoc.x += refWinLoc.x;
         pntLoc.y += refWinLoc.y;
      }
      pntLoc.x += refSize.width * xr;
      pntLoc.y += refSize.height * yr;
      return pntLoc;
   }

   /**
    * Gets the current screen bounds of a component.
    */
   public static Rectangle getScreenBounds (Component ref) {
      Rectangle bounds = new Rectangle();
      Point loc = getScreenLocation (ref, 0, 0);
      bounds.x = loc.x;
      bounds.y = loc.y;
      bounds.width = ref.getWidth();
      bounds.height = ref.getHeight();
      return bounds;
   }
   
   /**
    * Sets the horizonal position of a window relative to another component. The
    * vertical position is unchanged. The relative location is specified by
    * either {@link #LEFT LEFT}, {@link #RIGHT RIGHT}, {@link #CENTER CENTER},
    * {@link #LEFT_EDGES LEFT_EDGES}, or {@link #RIGHT_EDGES RIGHT_EDGES}.
    * This placement may be altered in order to keep the window on the screen.
    * 
    * @param win
    * window to be located
    * @param ref
    * reference component
    * @param location
    * desired horizonal location of the window relative to the component
    */
   public static void locateHorizontally (
      Window win, Component ref, int location) {
      
      Window refWin;            // window containing the reference component
      Point refLoc;             // component location relative to its window
      Dimension refSize;        // size of the reference component
      Dimension winSize;        // size of the window being placed

      refWin = windowForComponent (ref);
      if (refWin == null) {
         return;
      }
      
      refSize = ref.getSize();
      refLoc = SwingUtilities.convertPoint (ref, 0, 0, refWin);
      Point refWinLoc = refWin.getLocation();
      refLoc.x += refWinLoc.x;
      refLoc.y += refWinLoc.y;

      Rectangle screenBounds = getVirtualScreenBounds ();
      winSize = win.getSize();

      // start by centering the window on the reference component
      Point newLoc = win.getLocation();

      // now try to accomodate the relative location
      switch (location) {
         case CENTER: {
            newLoc.x = refLoc.x + (refSize.width - winSize.width) / 2;
            break;
         }
         case LEFT: {
            newLoc.x = refLoc.x - winSize.width;
            if (newLoc.x < screenBounds.x) {
               // move to right-side
               newLoc.x = refLoc.x + refSize.width;
            }
            break;
         }
         case LEFT_EDGES: {
            newLoc.x = refLoc.x;
            break;
         }
         case RIGHT: {
            newLoc.x = refLoc.x + refSize.width;
            if (newLoc.x + winSize.width > screenBounds.x + screenBounds.width) {
               // move to left size
               newLoc.x = refLoc.x - winSize.width;
            }
            break;
         }
         case RIGHT_EDGES: {
            newLoc.x = refLoc.x + (refSize.width - winSize.width);
            break;
         }
         default: {
            throw new IllegalArgumentException ("Unknown location code "
            + location);
         }
      }
      

      // adjust to keep left side on screen
      if (newLoc.x + winSize.width > screenBounds.x + screenBounds.width) {
         newLoc.x = screenBounds.x + screenBounds.width - winSize.width;
      }
      if (newLoc.x < screenBounds.x) {
         newLoc.x = screenBounds.x;
      }
      
      win.setLocation (newLoc);
   }

   /**
    * Sets the vertical position of a window relative to another component. The
    * horizontal position is unchanged. The relative location is specified by
    * either {@link #ABOVE ABOVE}, {@link #BELOW BELOW},
    * {@link #CENTER CENTER}, {@link #TOP_EDGES TOP_EDGES}, or
    * {@link #BOTTOM_EDGES BOTTOM_EDGES}. This placement may be altered in
    * order to keep the window on the screen.
    * 
    * @param win
    * window to be located
    * @param ref
    * reference component
    * @param location
    * desired horizonal location of the window relative to the component
    */
   public static void locateVertically (Window win, Component ref, int location) {
      
      Window refWin;            // window containing the reference component
      Point refLoc;             // component location relative to its window
      Dimension refSize;        // size of the reference component
      Dimension winSize;        // size of the window being placed

      refWin = windowForComponent (ref);
      if (refWin == null) {
         return;
      }
      
      refSize = ref.getSize();
      refLoc = SwingUtilities.convertPoint (ref, 0, 0, refWin);
      Point refWinLoc = refWin.getLocation();
      refLoc.x += refWinLoc.x;
      refLoc.y += refWinLoc.y;

      Rectangle screenBounds = getVirtualScreenBounds ();
      winSize = win.getSize ();
      
      // start by centering the window on the reference component
      Point newLoc = win.getLocation();

      // now try to accomodate the relative location
      switch (location) {
         case CENTER: {
            newLoc.y = refLoc.y + (refSize.height - winSize.height) / 2;
            break;
         }
         case ABOVE: {
            newLoc.y = refLoc.y - winSize.height;
            if (newLoc.y < screenBounds.y) {
               // move below
               newLoc.y = refLoc.y + refSize.height;
            }
            break;
         }
         case TOP_EDGES: {
            newLoc.y = refLoc.y;
            break;
         }
         case BELOW: {
            newLoc.y = refLoc.y + refSize.height;
            if (newLoc.y + winSize.height > screenBounds.y + screenBounds.height) {
               // move above
               newLoc.y = refLoc.y - winSize.height;
            }
            break;
         }
         case BOTTOM_EDGES: {
            newLoc.y = refLoc.y + (refSize.height - winSize.height);
            break;
         }
         default: {
            throw new IllegalArgumentException ("Unknown location code "
            + location);
         }
      }
    
      // adjust to keep top on screen
      if (newLoc.y + winSize.height > screenBounds.y + screenBounds.height) {
         newLoc.y = screenBounds.y + screenBounds.height - winSize.height;
      }
      if (newLoc.y < screenBounds.y) {
         newLoc.y = screenBounds.y;
      }
      win.setLocation (newLoc);
   }

   /**
    * Locates a window relative to a given set of screen bounds. The location is
    * set so that point (wx, wy) in the window maps to point (bx, by) in the
    * bounds. Both sets of points are normalized to relative to the window and
    * bounds sizes, so that (0, 0) corresponds to the upper left corner, (0.5,
    * 0.5) is the center, (1, 1) is the lower right, etc.
    */
   public static void locateRelative (
      Window win, Rectangle bounds, double px, double py, double wx, double wy) {
      int w = win.getWidth();
      int h = win.getHeight();

      Point loc = new Point();
      loc.x = bounds.x + (int)(px * bounds.width - wx * w);
      loc.y = bounds.y + (int)(py * bounds.height - wy * h);

      // clip so that component is visible
      Rectangle scrBounds = GuiUtils.getVirtualScreenBounds();

      if (loc.x + w > scrBounds.x + scrBounds.width) {
         loc.x = scrBounds.x + scrBounds.width - w;
      }
      if (loc.x < scrBounds.x) {
         loc.x = scrBounds.x;
      }
      
      if (loc.y + h > scrBounds.y + scrBounds.height) {
         loc.y = scrBounds.y + scrBounds.height - h;
      }
      if (loc.y < scrBounds.y) {
         loc.y = scrBounds.y;
      }
      
      win.setLocation (loc);
   }

   /**
    * Center a window on the window containing a specified
    * reference component. 
    */
   public static void locateCenter (Window win, Component ref) {
      Window refWin = windowForComponent (ref);
      locateRelative (win, refWin.getBounds(), 0.5, 0.5, 0.5, 0.5);
   }


   /**
    * Locates a window to the right of the window containing a specified
    * reference component. The top edges of the window and the reference
    * component are aligned. Final placement may be altered slightly to keep the
    * window fully on the screen.
    */
   public static void locateRight (Window win, Component ref) {
      Window refWin = windowForComponent (ref);
      locateHorizontally (win, refWin, RIGHT);
      locateVertically (win, ref, TOP_EDGES);
   }

   /**
    * Locates a window to the left of the window containing a specified
    * reference component. The top edges of the window and the reference
    * component are aligned. Final placement may be altered slightly to keep the
    * window fully on the screen.
    */
   public static void locateLeft (Window win, Component ref) {
      Window refWin = windowForComponent (ref);
      locateHorizontally (win, refWin, LEFT);
      locateVertically (win, ref, TOP_EDGES);
   }

   /**
    * Locates a window above the window containing a specified reference
    * component. The left edges of the window and the reference component are
    * aligned. Final placement may be altered slightly to keep the window fully
    * on the screen.
    */
   public static void locateAbove (Window win, Component ref) {
      Window refWin = windowForComponent (ref);
      locateVertically (win, refWin, ABOVE);
      locateHorizontally (win, ref, LEFT_EDGES);
   }

   /**
    * Locates a window below the window containing a specified reference
    * component. The left edges of the window and the reference component are
    * aligned. Final placement may be altered slightly to keep the window fully
    * on the screen.
    */
   public static void locateBelow (Window win, Component ref) {
      Window refWin = windowForComponent (ref);
      locateVertically (win, refWin, BELOW);
      locateHorizontally (win, ref, LEFT_EDGES);
   }

   public static Rectangle getVirtualScreenBounds() {
      Rectangle virtualBounds = new Rectangle();
      GraphicsEnvironment ge =
         GraphicsEnvironment.getLocalGraphicsEnvironment();
      GraphicsDevice[] gs = ge.getScreenDevices();
      for (int j = 0; j < gs.length; j++) {
         GraphicsDevice gd = gs[j];
         GraphicsConfiguration[] gc = gd.getConfigurations();
         for (int i = 0; i < gc.length; i++) {
            virtualBounds = virtualBounds.union (gc[i].getBounds());
         }
      }
      return virtualBounds;
   }

   public static ImageIcon loadIcon (Object ref, String path) {
      String fullpath;
      try {
         fullpath = PathFinder.findSourceDir (ref) + File.separator + path;
      }
      catch (Exception e) {
         System.out.println ("Cannot locate Icon relative to " + ref);
         return null;
      }
      try {
         return new ImageIcon (fullpath);
      }
      catch (Exception e) {
         System.out.println ("Error loading icon from " + fullpath);
         System.out.println (e.getMessage());
         return null;
      }      
   }

   public static int indexOfComponent (Container parent, Component comp) {
      Component[] comps = parent.getComponents();
      for (int i = 0; i < comps.length; i++) {
         if (comps[i] == comp) {
            return i;

         }
      }
      return -1;
   }

   public static boolean containsComponent (Container parent, Component comp) {
      return indexOfComponent (parent, comp) != -1;
   }

   public static int indexOfMenuComponent (JMenu parent, Component comp) {
      Component[] comps = parent.getMenuComponents();
      for (int i = 0; i < comps.length; i++) {
         if (comps[i] == comp) {
            return i;

         }
      }
      return -1;
   }

   public static boolean containsMenuComponent (JMenu parent, Component comp) {
      return indexOfMenuComponent (parent, comp) != -1;
   }

   public static void setItalicFont (JComponent jcomp) {
      Font font = jcomp.getFont();
      jcomp.setFont (new Font (font.getName(), Font.ITALIC, font.getSize()));

   }

   public static void repackComponentWindow (Component c) {
      if (c.isVisible()) {
         Window win = SwingUtilities.windowForComponent (c);
         if (win != null) {
            win.pack();
            win.repaint();
         }
      }
   }

   private static class SliderUIX extends BasicSliderUI {
      public SliderUIX (JSlider slider){
         super (slider);
      }

      public int xPositionForValue (int value) {
         return super.xPositionForValue(value);
      }

      public Rectangle getTrackRect() {
         return trackRect;
      }
   }

   public static TitledBorder createTitledPanelBorder (String title) {
      TitledBorder border = BorderFactory.createTitledBorder(title);
      Font font = border.getTitleFont();
      if (font==null) {
         font = UIManager.getFont("TitledBorder.font"); // Java SE 7 workaround
      }
      border.setTitleFont (new Font(font.getName(), Font.ITALIC, font.getSize()));
      return border;
   }

   public static JMenuItem createMenuItem (
      ActionListener listener, String cmd, String toolTip) {
      JMenuItem item = new JMenuItem (cmd);
      item.setActionCommand (cmd);
      item.addActionListener (listener);
      item.setToolTipText (toolTip);
      return item;
   }

   public static void showError (Component comp, String msg) {
      JOptionPane.showMessageDialog (
         windowForComponent (comp), msg, "Error", JOptionPane.ERROR_MESSAGE);
   }

   public static void showError (Component comp, String msg, Exception e) {
      if (e.getMessage() == null) {
         msg += ": " + e.toString();
      }
      else {
         msg += ": " + e.getMessage();
      }
      showError (comp, msg);
   }

   public static void showWarning (Component comp, String msg) {
      JOptionPane.showMessageDialog (
         windowForComponent (comp), msg,
         "Warning", JOptionPane.WARNING_MESSAGE);
   }

   public static void showNotice (Component comp, String msg) {
      JOptionPane.showMessageDialog (
         windowForComponent (comp), msg,
         "Notice", JOptionPane.INFORMATION_MESSAGE);
   }

   public static boolean confirmAction (Component comp, String msg) {
      int confirmation = JOptionPane.showConfirmDialog (
         windowForComponent (comp), msg,
         "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
      return (confirmation == JOptionPane.YES_OPTION);
   }

   public static boolean confirmOverwrite (Component comp, File file) {
      return confirmAction (comp, "Overwrite existing file "+file+"?");
   }

//   public static void setSliderLength (JSlider slider, int pixels) {
//      BasicSliderUI savedUI = (BasicSliderUI)slider.getUI();
//      SliderUIX testUI = new SliderUIX(slider);
//      slider.setUI (testUI);
//      int length = 200;
//      while (testUI.valueForXPosition (length-1) == slider.getMaximum()) {
//         length--;
//      }
//      System.out.println ("value for 0=" + testUI.valueForXPosition(0));
//      System.out.println ("value for 6=" + testUI.valueForXPosition(5));
//      System.out.println ("length=" + length);
//      System.out.println ("prefsize=" + slider.getPreferredSize());
//      System.out.println (
//          "range=" + slider.getMaximum() + " " + slider.getMinimum());
//      System.out.println ("size=" + slider.getSize());
//
//      System.out.println ("rect=" + testUI.getTrackRect());
//
//      length = (testUI.xPositionForValue(slider.getMaximum()) -
//                    testUI.xPositionForValue(slider.getMinimum()));
//       slider.setUI (savedUI);
//   }
}
