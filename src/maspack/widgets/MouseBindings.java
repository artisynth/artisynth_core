package maspack.widgets;

import java.awt.event.*;
import java.util.*;

import maspack.render.GL.GLMouseAdapter;
import maspack.util.InternalErrorException;

/**
 * Contains information about what combinations of mouse buttons and modifier
 * keys should be used to perform certain GUI operations.
 */
public class MouseBindings implements Cloneable {

   /**
    * Left mouse button
    */
   public static final int LMB = 0x0001;
   
   /**
    * Middle mouse button
    */
   public static final int MMB = 0x0002;
   
   /**
    * Right mouse button
    */
   public static final int RMB = 0x0004;

   /**
    * Shift key
    */
   public static final int SHIFT = 0x0008;
   
   /**
    * Control key
    */
   public static final int CTRL = 0x0010;
   
   /**
    * Alt key
    */
   public static final int ALT = 0x0020;
   
   /**
    * Meta key: usually bound to the Command or Windows key
    */
   public static final int META = 0x0040;

   public static final MouseBindings ThreeButton = createThreeButton();
   public static final MouseBindings TwoButton = createTwoButton();
   public static final MouseBindings OneButton = createOneButton();
   public static final MouseBindings Laptop = createLaptop();
   public static final MouseBindings Mac = createMac();
   public static final MouseBindings Kees = createKees();
   
   private static MouseBindings createThreeButton () {
      MouseBindings bindings = new MouseBindings();

      bindings.setAction (MouseAction.ROTATE_VIEW, MMB);
      bindings.setAction (MouseAction.TRANSLATE_VIEW, MMB|SHIFT);
      bindings.setAction (MouseAction.ZOOM_VIEW, MMB|CTRL);

      bindings.setAction (MouseAction.SELECT_COMPONENTS, LMB);
      bindings.setAction (MouseAction.MULTIPLE_SELECTION, CTRL);
      bindings.setAction (MouseAction.ELLIPTIC_DESELECT, SHIFT);
      bindings.setAction (
         MouseAction.RESIZE_ELLIPTIC_CURSOR, LMB|CTRL|SHIFT);

      bindings.setAction (MouseAction.MOVE_DRAGGER, LMB);
      bindings.setAction (MouseAction.DRAGGER_CONSTRAIN, SHIFT);
      bindings.setAction (MouseAction.DRAGGER_REPOSITION, CTRL);
      bindings.setAction (MouseAction.CONTEXT_MENU, RMB);  
      bindings.setName ("ThreeButton");
      return bindings;
   }

   private static MouseBindings createTwoButton () {
      MouseBindings bindings = new MouseBindings();

      bindings.setAction (MouseAction.ROTATE_VIEW, LMB | ALT);
      bindings.setAction (MouseAction.TRANSLATE_VIEW, LMB | ALT | SHIFT);
      bindings.setAction (MouseAction.ZOOM_VIEW, LMB | ALT |CTRL);

      bindings.setAction (MouseAction.SELECT_COMPONENTS, LMB);
      bindings.setAction (MouseAction.MULTIPLE_SELECTION, CTRL);
      bindings.setAction (MouseAction.ELLIPTIC_DESELECT, SHIFT);
      bindings.setAction (
         MouseAction.RESIZE_ELLIPTIC_CURSOR, LMB|CTRL|SHIFT);

      bindings.setAction (MouseAction.MOVE_DRAGGER, LMB);
      bindings.setAction (MouseAction.DRAGGER_CONSTRAIN, SHIFT);
      bindings.setAction (MouseAction.DRAGGER_REPOSITION, CTRL);
      bindings.setAction (MouseAction.CONTEXT_MENU, RMB);         
      bindings.setName ("TwoButton");
      return bindings;
   }

   private static MouseBindings createOneButton () {
      MouseBindings bindings = new MouseBindings();

      bindings.setAction (MouseAction.ROTATE_VIEW, LMB | ALT);
      bindings.setAction (MouseAction.TRANSLATE_VIEW, LMB | ALT | SHIFT);
      bindings.setAction (MouseAction.ZOOM_VIEW, LMB | ALT |CTRL);

      bindings.setAction (MouseAction.SELECT_COMPONENTS, LMB);
      bindings.setAction (MouseAction.MULTIPLE_SELECTION, CTRL);
      bindings.setAction (MouseAction.ELLIPTIC_DESELECT, SHIFT);
      bindings.setAction (
         MouseAction.RESIZE_ELLIPTIC_CURSOR, LMB|CTRL|SHIFT);

      bindings.setAction (MouseAction.MOVE_DRAGGER, LMB);
      bindings.setAction (MouseAction.DRAGGER_CONSTRAIN, SHIFT);
      bindings.setAction (MouseAction.DRAGGER_REPOSITION, CTRL);
      bindings.setAction (MouseAction.CONTEXT_MENU, LMB | META);
      bindings.setName ("OneButton");
      return bindings;         
   }

   private static MouseBindings createLaptop () {
      MouseBindings bindings = new MouseBindings();

      bindings.setAction (MouseAction.ROTATE_VIEW, LMB);
      bindings.setAction (MouseAction.TRANSLATE_VIEW, LMB|SHIFT);
      bindings.setAction (MouseAction.ZOOM_VIEW, LMB|ALT);

      bindings.setAction (MouseAction.SELECT_COMPONENTS, LMB|CTRL);
      bindings.setAction (MouseAction.MULTIPLE_SELECTION, SHIFT);
      bindings.setAction (MouseAction.ELLIPTIC_DESELECT, SHIFT);
      bindings.setAction (
         MouseAction.RESIZE_ELLIPTIC_CURSOR, LMB|CTRL|SHIFT);

      bindings.setAction (MouseAction.MOVE_DRAGGER, LMB);
      bindings.setAction (MouseAction.DRAGGER_CONSTRAIN, SHIFT);
      bindings.setAction (MouseAction.DRAGGER_REPOSITION, ALT);
      bindings.setAction (MouseAction.CONTEXT_MENU, RMB);         
      bindings.setName ("Laptop");
      return bindings;
   }

   private static MouseBindings createMac () {
      MouseBindings bindings = new MouseBindings();

      bindings.setAction (MouseAction.ROTATE_VIEW, LMB|ALT);
      bindings.setAction (MouseAction.TRANSLATE_VIEW, LMB|ALT|SHIFT);
      bindings.setAction (MouseAction.ZOOM_VIEW, LMB|ALT|META);

      bindings.setAction (MouseAction.SELECT_COMPONENTS, LMB);
      bindings.setAction (MouseAction.MULTIPLE_SELECTION, META);
      bindings.setAction (MouseAction.ELLIPTIC_DESELECT, SHIFT);
      bindings.setAction (
         MouseAction.RESIZE_ELLIPTIC_CURSOR, LMB|CTRL|SHIFT);

      bindings.setAction (MouseAction.MOVE_DRAGGER, LMB);
      bindings.setAction (MouseAction.DRAGGER_CONSTRAIN, SHIFT);
      bindings.setAction (MouseAction.DRAGGER_REPOSITION, ALT);
      bindings.setAction (MouseAction.CONTEXT_MENU, LMB|CTRL);         
      bindings.setName ("Mac");
      return bindings;
   }

   private static MouseBindings createKees () {
      MouseBindings bindings = createLaptop();
      bindings.setAction (
         MouseAction.MULTIPLE_SELECTION, MouseBindings.CTRL);
      bindings.setName ("Kees");
      return bindings;
   }
   
   public enum MouseAction {
      /**
       * Rotate the current view
       */
      ROTATE_VIEW,

      /**
       * Translate the current view
       */
      TRANSLATE_VIEW,

      /**
       * Zoom the current view
       */
      ZOOM_VIEW,

      /**
       * Perform click or drag-based selection
       */
      SELECT_COMPONENTS,

      /**
       * Mask to allow multiple selection
       */
      MULTIPLE_SELECTION,

      /**
       * Mask to allow deselection when using elliptic selection
       */
      ELLIPTIC_DESELECT,

      /**
       * Resize the elliptic cursor
       */
      RESIZE_ELLIPTIC_CURSOR,

      /**
       * Invoke to the context menu
       */
      CONTEXT_MENU,

      /**
       * Move dragger fixtures
       */
      MOVE_DRAGGER,

      /**
       * Mask to constrain dragger motion
       */
      DRAGGER_CONSTRAIN,

      /**
       * Mask to allow dragger repositioning
       */
      DRAGGER_REPOSITION;

      public String getActionDescription() {
         String str = toString().replace('_',' ');
         return str.charAt(0) + str.substring(1).toLowerCase();
      }
   }

   private int[] myActionMasks = new int[MouseAction.values().length];
   private String myName = null;

   public String getName() {
      return myName;
   }

   public void setName(String name) {
      myName = name;
   }

   public void setAction (MouseAction action, int value) {
      myActionMasks[action.ordinal()] = value;
   }

   public MouseBindings() {
   }

   public MouseBindings (MouseBindings bindings) {
      set (bindings);
   }

   public void set (MouseBindings bindings) {
      for (int i=0; i<myActionMasks.length; i++) {
         myActionMasks[i] = bindings.myActionMasks[i];
      }
   }

   public void apply (GLMouseAdapter adapter, int numButtons) {
      
      adapter.setRotateButtonMask(
         getMouseMask (MouseAction.ROTATE_VIEW, numButtons));
      adapter.setTranslateButtonMask (
         getMouseMask (MouseAction.TRANSLATE_VIEW, numButtons));
      adapter.setZoomButtonMask(
         getMouseMask (MouseAction.ZOOM_VIEW, numButtons));
         
      adapter.setSelectionButtonMask(
         getMouseMask (MouseAction.SELECT_COMPONENTS, numButtons));
      adapter.setMultipleSelectionMask(
         getMouseMask (MouseAction.MULTIPLE_SELECTION, numButtons));
      adapter.setEllipticDeselectMask(
         getMouseMask (MouseAction.ELLIPTIC_DESELECT, numButtons));
      adapter.setEllipticCursorResizeMask(
         getMouseMask (MouseAction.RESIZE_ELLIPTIC_CURSOR, numButtons));

      adapter.setDraggerDragMask(
         getMouseMask (MouseAction.MOVE_DRAGGER, numButtons));
      adapter.setDraggerConstrainMask(
         getMouseMask (MouseAction.DRAGGER_CONSTRAIN, numButtons));
      adapter.setDraggerRepositionMask(
         getMouseMask (MouseAction.DRAGGER_REPOSITION, numButtons));

      ButtonMasks.setContextMenuMask (
         getMouseMask (MouseAction.CONTEXT_MENU, numButtons));
   }

   /**
    * Get the actual mouse mask for an action.
    */
   protected int getMouseMask (MouseAction action, int numButtons) {
      return toMouseMask (getActionMask (action), numButtons);
   }
   
   /**
    * Translates the action mask used by this class into the equivalent Java AWT 
    * mouse mask.
    */
   protected int toMouseMask (int mask, int numButtons) {
      
      int mouseMask = 0;
      if ((mask & LMB) != 0) {
         mouseMask |= InputEvent.BUTTON1_DOWN_MASK;
      }      
      if ((mask & MMB) != 0) {
         if (numButtons >= 2) {
            mouseMask |= InputEvent.BUTTON2_DOWN_MASK;
         }
         else {
            mouseMask |= InputEvent.BUTTON1_DOWN_MASK;
         }
      }
      if ((mask & RMB) != 0) {
         if (numButtons >= 3) {
            mouseMask |= InputEvent.BUTTON3_DOWN_MASK;
         }
         else if (numButtons == 2) {
            mouseMask |= InputEvent.BUTTON2_DOWN_MASK;
         }
         else {
            mouseMask |= InputEvent.BUTTON1_DOWN_MASK;
         }
      }
      if ((mask & SHIFT) != 0) {
         mouseMask |= InputEvent.SHIFT_DOWN_MASK;
      }
      if ((mask & CTRL) != 0) {
         mouseMask |= InputEvent.CTRL_DOWN_MASK;
      }
      if ((mask & ALT) != 0) {
         mouseMask |= InputEvent.ALT_DOWN_MASK;
      }
      if ((mask & META) != 0) {
         mouseMask |= InputEvent.META_DOWN_MASK;
      }
      return mouseMask;
   }
   
   protected int getFullMask (MouseAction action) {
      switch (action) {
         case MULTIPLE_SELECTION:
         case ELLIPTIC_DESELECT: {
            return (getActionMask(MouseAction.SELECT_COMPONENTS) |
                    getActionMask(action));
         }
         case DRAGGER_CONSTRAIN:
         case DRAGGER_REPOSITION: {
            return (getActionMask(MouseAction.MOVE_DRAGGER) |
                    getActionMask(action));
         }
         default: {
            return getActionMask(action);
         }
      }
   }
   
   public int getActionMask (MouseAction action) {
      return myActionMasks[action.ordinal()];
   }

   protected String appendStr (String str, String suf) {
      if (str == null) {
         return suf;
      }
      else {
         return str + "+" + suf;
      }
   }      

   protected String maskToString (int mask) {
      String str = null;
      if ((mask & LMB) == LMB) {
         str = appendStr (str, "LMB");
      }
      if ((mask & MMB) == MMB) {
         str = appendStr (str, "MMB");
      }
      if ((mask & RMB) == RMB) {
         str = appendStr (str, "RMB");
      }
      if ((mask & META) == META) {
         str = appendStr (str, "META");
      }
      if ((mask & ALT) == ALT) {
         str = appendStr (str, "ALT");
      }
      if ((mask & SHIFT) == SHIFT) {
         str = appendStr (str, "SHIFT");
      }
      if ((mask & CTRL) == CTRL) {
         str = appendStr (str, "CTRL");
      }
      return str;
   }

   boolean equals (MouseBindings bindings) {
      for (int i=0; i<myActionMasks.length; i++) {
         if (myActionMasks[i] != bindings.myActionMasks[i]) {
            return false;
         }
      }
      return true;
   }

   public MouseBindings clone() {
      MouseBindings bindings;
      try {
         bindings = (MouseBindings)super.clone();
      }
      catch (Exception e) {
         throw new InternalErrorException ("MouseBindings cannot be cloned");
      }
      bindings.myActionMasks =
         Arrays.copyOf (myActionMasks, myActionMasks.length);
      return bindings;
   }
      
}

