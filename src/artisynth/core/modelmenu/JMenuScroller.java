package artisynth.core.modelmenu;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuSelectionManager;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * A class that provides scrolling capabilities to a long menu dropdown or popup
 * menu. A number of items can optionally be frozen at the top and/or bottom of
 * the menu.
 * <P>
 * <B>Implementation note:</B> The default number of items to display at a time
 * is MenuEntry.DEFAULT_MAX_ROWS, and the default scrolling interval 
 * is 125 milliseconds.
 * <P>
 *
 * Modified from:
 *      http://www.camick.com/java/source/MenuScroller.java
 *
 * @version 1.5.0 04/05/12
 */
public class JMenuScroller {

   // private JMenu menu;
   private JPopupMenu menu;
   private Component[] menuItems;
   private MenuScrollItem upItem;
   private MenuScrollItem downItem;
   private final MenuScrollListener menuListener = new MenuScrollListener ();
   private int scrollCount;
   private int width;
   private int interval;
   private int firstIndex = 0;
   private int keepVisibleIndex = -1;

   /**
    * Registers a menu to be scrolled with the default number of items to
    * display at a time and the default scrolling interval.
    * 
    * @param menu
    * the menu
    * @return the JMenuScroller
    */
   public static JMenuScroller setScrollerFor (JMenu menu) {
      return new JMenuScroller (menu);
   }

   /**
    * Registers a popup menu to be scrolled with the default number of items to
    * display at a time and the default scrolling interval.
    * 
    * @param menu
    * the popup menu
    * @return the JMenuScroller
    */
   public static JMenuScroller setScrollerFor (JPopupMenu menu) {
      return new JMenuScroller (menu);
   }

   /**
    * Registers a menu to be scrolled with the default number of items to
    * display at a time and the specified scrolling interval.
    * 
    * @param menu
    * the menu
    * @param scrollCount
    * the number of items to display at a time
    * @return the JMenuScroller
    * @throws IllegalArgumentException
    * if scrollCount is 0 or negative
    */
   public static JMenuScroller setScrollerFor (JMenu menu, int scrollCount) {
      return new JMenuScroller (menu, scrollCount);
   }

   /**
    * Registers a popup menu to be scrolled with the default number of items to
    * display at a time and the specified scrolling interval.
    * 
    * @param menu
    * the popup menu
    * @param scrollCount
    * the number of items to display at a time
    * @return the JMenuScroller
    * @throws IllegalArgumentException
    * if scrollCount is 0 or negative
    */
   public static JMenuScroller setScrollerFor (
      JPopupMenu menu, int scrollCount) {
      return new JMenuScroller (menu, scrollCount);
   }

   /**
    * Registers a menu to be scrolled, with the specified number of items to
    * display at a time and the specified scrolling interval.
    * 
    * @param menu
    * the menu
    * @param scrollCount
    * the number of items to be displayed at a time
    * @param interval
    * the scroll interval, in milliseconds
    * @return the JMenuScroller
    * @throws IllegalArgumentException
    * if scrollCount or interval is 0 or negative
    */
   public static JMenuScroller setScrollerFor (
      JMenu menu, int scrollCount, int interval) {
      return new JMenuScroller (menu, scrollCount, interval);
   }

   /**
    * Registers a popup menu to be scrolled, with the specified number of items
    * to display at a time and the specified scrolling interval.
    * 
    * @param menu
    * the popup menu
    * @param scrollCount
    * the number of items to be displayed at a time
    * @param interval
    * the scroll interval, in milliseconds
    * @return the JMenuScroller
    * @throws IllegalArgumentException
    * if scrollCount or interval is 0 or negative
    */
   public static JMenuScroller setScrollerFor (
      JPopupMenu menu, int scrollCount, int interval) {
      return new JMenuScroller (menu, scrollCount, interval);
   }

   /**
    * Constructs a <code>JMenuScroller</code> that scrolls a menu with the
    * default number of items to display at a time, and default scrolling
    * interval.
    * 
    * @param menu
    * the menu
    */
   public JMenuScroller (JMenu menu) {
      this (menu, MenuEntry.DEFAULT_MAX_ROWS);
   }

   /**
    * Constructs a <code>JMenuScroller</code> that scrolls a popup menu with the
    * default number of items to display at a time, and default scrolling
    * interval.
    * 
    * @param menu
    * the popup menu
    */
   public JMenuScroller (JPopupMenu menu) {
      this (menu, MenuEntry.DEFAULT_MAX_ROWS);
   }

   /**
    * Constructs a <code>JMenuScroller</code> that scrolls a menu with the
    * specified number of items to display at a time, and default scrolling
    * interval.
    * 
    * @param menu
    * the menu
    * @param scrollCount
    * the number of items to display at a time
    * @throws IllegalArgumentException
    * if scrollCount is 0 or negative
    */
   public JMenuScroller (JMenu menu, int scrollCount) {
      this (menu, scrollCount, 50);
   }

   /**
    * Constructs a <code>JMenuScroller</code> that scrolls a popup menu with the
    * specified number of items to display at a time, and default scrolling
    * interval.
    * 
    * @param menu
    * the popup menu
    * @param scrollCount
    * the number of items to display at a time
    * @throws IllegalArgumentException
    * if scrollCount is 0 or negative
    */
   public JMenuScroller (JPopupMenu menu, int scrollCount) {
      this (menu, scrollCount, 150);
   }

   /**
    * Constructs a <code>JMenuScroller</code> that scrolls a menu with the
    * specified number of items to display in the scrolling region, and the
    * specified scrolling interval.
    * 
    * @param menu
    * the menu
    * @param scrollCount
    * the number of items to display in the scrolling portion
    * @param interval
    * the scroll interval, in milliseconds
    * @throws IllegalArgumentException
    * if scrollCount or interval is 0 or negative
    */
   public JMenuScroller (JMenu menu, int scrollCount, int interval) {
      this (menu.getPopupMenu (), scrollCount, interval);
   }

   /**
    * Constructs a <code>JMenuScroller</code> that scrolls a popup menu with the
    * specified number of items to display in the scrolling region, and the
    * specified scrolling interval.
    * 
    * @param menu
    * the popup menu
    * @param scrollCount
    * the number of items to display in the scrolling portion
    * @param interval
    * the scroll interval, in milliseconds
    * @throws IllegalArgumentException
    * if scrollCount or interval is 0 or negative
    */
   public JMenuScroller (JPopupMenu menu, int scrollCount, int interval) {
      if (scrollCount <= 0 || interval <= 0) {
         throw new IllegalArgumentException (
            "scrollCount and interval must be greater than 0");
      }

      upItem = new MenuScrollItem (MenuIcon.UP, -1);
      downItem = new MenuScrollItem (MenuIcon.DOWN, +1);
      setScrollCount (scrollCount);
      setInterval (interval);

      this.menu = menu;
      menu.addPopupMenuListener (menuListener);
      menu.addMouseWheelListener (menuListener);
   }

   /**
    * Returns the scroll interval in milliseconds
    * 
    * @return the scroll interval in milliseconds
    */
   public int getInterval () {
      return interval;
   }

   /**
    * Sets the scroll interval in milliseconds
    * 
    * @param interval
    * the scroll interval in milliseconds
    * @throws IllegalArgumentException
    * if interval is 0 or negative
    */
   public void setInterval (int interval) {
      if (interval <= 0) {
         throw new IllegalArgumentException ("interval must be greater than 0");
      }
      upItem.setInterval (interval);
      downItem.setInterval (interval);
      this.interval = interval;
   }

   /**
    * Returns the number of items in the scrolling portion of the menu.
    *
    * @return the number of items to display at a time
    */
   public int getscrollCount () {
      return scrollCount;
   }

   /**
    * Sets the number of items in the scrolling portion of the menu.
    * 
    * @param scrollCount
    * the number of items to display at a time
    * @throws IllegalArgumentException
    * if scrollCount is 0 or negative
    */
   public void setScrollCount (int scrollCount) {
      if (scrollCount <= 0) {
         throw new IllegalArgumentException (
            "scrollCount must be greater than 0");
      }
      this.scrollCount = scrollCount;
      MenuSelectionManager.defaultManager ().clearSelectedPath ();
   }

   /**
    * Scrolls the specified item into view each time the menu is opened. Call
    * this method with <code>null</code> to restore the default behavior, which
    * is to show the menu as it last appeared.
    *
    * @param item
    * the item to keep visible
    * @see #keepVisible(int)
    */
   public void keepVisible (JMenuItem item) {
      if (item == null) {
         keepVisibleIndex = -1;
      }
      else {
         int index = menu.getComponentIndex (item);
         keepVisibleIndex = index;
      }
   }

   /**
    * Scrolls the item at the specified index into view each time the menu is
    * opened. Call this method with <code>-1</code> to restore the default
    * behavior, which is to show the menu as it last appeared.
    *
    * @param index
    * the index of the item to keep visible
    * @see #keepVisible(javax.swing.JMenuItem)
    */
   public void keepVisible (int index) {
      keepVisibleIndex = index;
   }

   /**
    * Removes this JMenuScroller from the associated menu and restores the
    * default behavior of the menu.
    */
   public void dispose () {
      if (menu != null) {
         menu.removePopupMenuListener (menuListener);
         menu.removeMouseWheelListener (menuListener);
         menu = null;
      }
   }

   /**
    * Ensures that the <code>dispose</code> method of this JMenuScroller is
    * called when there are no more refrences to it.
    * 
    * @exception Throwable
    * if an error occurs.
    * @see JMenuScroller#dispose()
    */
   @Override
   public void finalize () throws Throwable {
      dispose ();
   }

   private void refreshMenu () {
      if (menuItems != null && menuItems.length > 0) {
         firstIndex = Math.max (0, firstIndex);
         firstIndex = Math.min (menuItems.length - scrollCount, firstIndex);

         upItem.setActive (firstIndex > 0);
         downItem.setActive (firstIndex + scrollCount < menuItems.length);

         menu.removeAll ();
         menu.add (upItem);  
         for (int i = firstIndex; i < scrollCount + firstIndex; i++) {
            menu.add (menuItems[i]);
         }
         menu.add (downItem);

         Dimension preferred =
            new Dimension (width + 10, menu.getPreferredSize ().height);
         menu.setPreferredSize (preferred);
         
         JComponent parent = (JComponent)upItem.getParent ();
         parent.revalidate ();
         parent.repaint ();
      }
   }

   private class MenuScrollListener
   implements PopupMenuListener, MouseWheelListener {

      @Override
      public void popupMenuWillBecomeVisible (PopupMenuEvent e) {
         setMenuItems ();
      }

      @Override
      public void popupMenuWillBecomeInvisible (PopupMenuEvent e) {
         restoreMenuItems ();
      }

      @Override
      public void popupMenuCanceled (PopupMenuEvent e) {
         restoreMenuItems ();
      }

      private void setMenuItems () {
         if (menuItems == null) {
            menuItems = menu.getComponents ();
         }
         width = menu.getPreferredSize ().width;
         
         if (keepVisibleIndex >= 0 && keepVisibleIndex <= menuItems.length
            && (keepVisibleIndex > firstIndex + scrollCount
         || keepVisibleIndex < firstIndex)) {
            firstIndex = Math.min (firstIndex, keepVisibleIndex);
            firstIndex =
               Math.max (firstIndex, keepVisibleIndex - scrollCount + 1);
         }
         if (menuItems.length > scrollCount) {
            refreshMenu ();
         }
      }

      private void restoreMenuItems () {
         menu.setPreferredSize (null);
         menu.removeAll ();
         for (Component component : menuItems) {
            menu.add (component);
         }
         // John Lloyd, July 2021: 
         // don't pack because that causes a visible "after flash"
         // before the menu becomes invisible         
         //menu.pack ();
         width = menu.getPreferredSize ().width;
      }

      public void mouseWheelMoved (MouseWheelEvent mwe) {
         firstIndex += mwe.getWheelRotation ();
         mwe.consume ();  // prevent default wheel action
         refreshMenu ();
      }
   }

   private class MenuScrollTimer extends Timer {
      private static final long serialVersionUID = 12345L;

      public MenuScrollTimer (final int increment, int interval) {
         super (interval, new ActionListener () {

            @Override
            public void actionPerformed (ActionEvent e) {
               firstIndex += increment;
               refreshMenu ();
            }
         });
      }
   }

   private class MenuScrollItem extends JMenuItem implements ChangeListener {
      private static final long serialVersionUID = 54321L;

      private MenuScrollTimer timer;
      private boolean active = false;
      private boolean upscroll;

      public MenuScrollItem (MenuIcon icon, int increment) {
         setIcon (icon);
         setDisabledIcon (icon);
         setBackground (Color.LIGHT_GRAY);
         setEnabled (true);
         timer = new MenuScrollTimer (increment, interval);
         upscroll = increment < 0;
         addChangeListener (this);
      }
      
      @Override
      public void processMenuKeyEvent (MenuKeyEvent e) {
         // intercept key events
         int key = e.getKeyCode ();

         if ( this.isArmed () && upscroll && (key == KeyEvent.VK_UP || key == KeyEvent.VK_KP_UP)) {
            e.consume ();
         } else if ( this.isArmed () && !upscroll &&  (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_KP_DOWN)) {
            e.consume ();
         }
      }
      
      @Override
      protected void processMouseEvent (MouseEvent e) {
        
         // intercept mouse events
         int id = e.getID ();
         if (id != MouseEvent.MOUSE_CLICKED && id != MouseEvent.MOUSE_PRESSED && id != MouseEvent.MOUSE_RELEASED) {
            super.processMouseEvent (e);
         }
      }

      public void setActive (boolean set) {
         active = set;
      }

      public boolean isActive () {
         return active;
      }

      public void setInterval (int interval) {
         timer.setDelay (interval);
      }

      @Override
      public void stateChanged (ChangeEvent e) {
         if (isArmed () && !timer.isRunning ()) {
            timer.start ();
         }
         if (!isArmed () && timer.isRunning ()) {
            timer.stop ();
         }
      }
   }

   private static enum MenuIcon implements Icon {

      UP (9, 1, 9), DOWN (1, 9, 1);
      final int[] xPoints = { 1, 7, 13 };
      final int[] yPoints;

      MenuIcon (int... yPoints) {
         this.yPoints = yPoints;
      }

      @Override
      public void paintIcon (Component c, Graphics g, int x, int y) {

         MenuScrollItem item = (MenuScrollItem)c;
         Dimension size = c.getSize ();
         int iwidth = xPoints[2] + 1;
         int iheight = Math.max (yPoints[0], yPoints[1]) + 1;
         Graphics g2 =
            g.create (size.width / 2 - 5, size.height / 2 - 5, iwidth, iheight);
         g2.setColor (Color.GRAY);
         g2.drawPolygon (xPoints, yPoints, 3);
         if (item.isActive ()) {
            g2.setColor (Color.GRAY.darker ());
            g2.fillPolygon (xPoints, yPoints, 3);
         }
         g2.dispose ();
      }

      @Override
      public int getIconWidth () {
         return 0;
      }

      @Override
      public int getIconHeight () {
         return 0;
      }
   }
}