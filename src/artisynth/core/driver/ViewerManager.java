/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.MouseInfo;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.workspace.RootModel;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.AxisAngle;
import maspack.matrix.RotationMatrix3d;
import maspack.render.Dragger3d;
import maspack.render.IsRenderable;
import maspack.render.RenderList;
import maspack.render.Renderer;
import maspack.render.Renderer.HighlightStyle;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GLMouseAdapter;
import maspack.render.GL.GLViewerFrame;
import maspack.render.GL.GLViewerPanel;
import maspack.widgets.ButtonMasks;
import maspack.widgets.GuiUtils;
import maspack.widgets.MouseBindings;
import maspack.widgets.PropertyDialog;
import maspack.widgets.ViewerPopupManager;

/**
 * Driver class for model rendering. Each time the top-level model needs to be
 * rendered, this class builds the render list from the model hierarchy and
 * passes it to the GLViewer for execution.
 */

public class ViewerManager {
   private LinkedList<IsRenderable> myRenderables =
      new LinkedList<IsRenderable>();
   private LinkedList<Dragger3d> myDraggers = new LinkedList<Dragger3d>();

   // Flags for special "refresh" rendering
   public static final int DEFAULT_REFRESH_FLAGS = Renderer.SORT_FACES;
   private int myRefreshRenderFlags = DEFAULT_REFRESH_FLAGS;

   private MouseBindings myMouseBindings = null;

   public static final double DEFAULT_MOUSE_WHEEL_ZOOM_SCALE = 10;
   private double myMouseWheelZoomScale = DEFAULT_MOUSE_WHEEL_ZOOM_SCALE;

   boolean myDefaultDrawAxes = false;
   double myDefaultAxisLength = -1;
   boolean myDefaultOrthographic = false;
   boolean myDefaultDrawGrid = false;
   Color myBackgroundColor = Color.BLACK;
   Color mySelectionColor = Color.YELLOW;

   RenderList myRenderList;

   private class PopupManager extends ViewerPopupManager {

      PopupManager (GLViewer viewer) {
         super (viewer);
      }

      @Override
      public void actionPerformed (ActionEvent e) {
         String cmd = e.getActionCommand();
         if ("Set viewer properties".equals(cmd)) {
            PropertyDialog dialog = createPropertyDialog("OK Cancel");
            dialog.setVisible (true);
         }
         else if ("Set viewer grid properties".equals(cmd)) {
            PropertyDialog dialog = createGridPropertyDialog("OK Cancel");
            dialog.setVisible (true);
         }
         else if ("Refresh view".equals(cmd)) {
            myViewer.rerender(myRefreshRenderFlags);
         }
      }
   }

   private ArrayList<PopupManager> myPopupManagers =
      new ArrayList<PopupManager>();
   private ArrayList<GLViewer> myViewers =
      new ArrayList<GLViewer>();
   private ArrayList<MouseInputListener> myMouseListeners =
      new ArrayList<MouseInputListener>();
   private ArrayList<KeyListener> myKeyListeners =
      new ArrayList<KeyListener>();
   private boolean selectionEnabledP = true;
   private boolean selectOnPressP = false;
   private boolean ellipticSelectionP = false;

   // private EditorManager myEditorManager;

   public void setDefaultDrawAxes (boolean drawAxes) {
      myDefaultDrawAxes = drawAxes;
   }

   public boolean getDefaultDrawAxes() {
      return myDefaultDrawAxes;
   }

   public void setDefaultDrawGrid (boolean drawGrid) {
      myDefaultDrawGrid = drawGrid;
   }

   public boolean getDefaultDrawGrid() {
      return myDefaultDrawGrid;
   }

   public void setDefaultOrthographic (boolean ortho) {
      myDefaultOrthographic = ortho;
   }

   public boolean getDefaultOrthographic() {
      return myDefaultOrthographic;
   }

   public void setDefaultAxisLength (double l) {
      myDefaultAxisLength = l;
   }

   public double getDefaultAxisLength() {
      return myDefaultAxisLength;
   }

   private int numMouseButtons() {
      return MouseInfo.getNumberOfButtons();
   }

   public MouseBindings getMouseBindings() {
      if (myMouseBindings == null) {
         int numButtons = numMouseButtons();
         if (numButtons <= 1) {
            myMouseBindings = new MouseBindings(MouseBindings.OneButton);
         }
         else if (numButtons == 2) {
            myMouseBindings = new MouseBindings(MouseBindings.TwoButton);
         }
         else {
            myMouseBindings = new MouseBindings(MouseBindings.ThreeButton);
         }
      }
      return myMouseBindings;
   }

   private void setMouseBindings (GLViewer viewer, MouseBindings bindings) {
      int numButtons = numMouseButtons();
      GLMouseAdapter adapter = (GLMouseAdapter)viewer.getMouseHandler();
      if (adapter == null) {
         // XXX paranoid - don't think we need to do this
         adapter = new GLMouseAdapter(viewer);
         viewer.setMouseHandler(adapter);
      }
      bindings.apply (adapter, numButtons);
   }

   public void setMouseBindings (MouseBindings bindings) {
      if (!bindings.equals (myMouseBindings)) {
         myMouseBindings = new MouseBindings(bindings);
         for (GLViewer v : myViewers) {
            setMouseBindings (v, bindings);
         }       
      }
   }

   public double getMouseWheelZoomScale() {
      return myMouseWheelZoomScale;
   }

   public void setMouseWheelZoomScale (double scale) {
      if (scale != myMouseWheelZoomScale) {
         myMouseWheelZoomScale = scale;
         for (GLViewer v : myViewers) {
            v.getMouseHandler().setMouseWheelZoomScale (scale);
         }       
      }
   }

   public void addRenderable (IsRenderable r) {
      myRenderables.add (r);
      myRenderList = null;
   }

   public void removeRenderable (IsRenderable r) {
      myRenderables.remove (r);
      myRenderList = null;

   }

   public void clearRenderables() {
      myRenderables.clear();
      myRenderList = null;
   }

   public RenderList getRenderList() {
      if (myRenderList == null) {
         myRenderList = buildRenderList();
      }
      return myRenderList;
   }

   public void addDragger (Dragger3d d) {
      myDraggers.add (d);
      for (GLViewer v : myViewers) {
         v.addDragger (d);
      }
   }

   public void removeDragger (Dragger3d d) {
      myDraggers.remove (d);
      for (GLViewer v : myViewers) {
         v.removeDragger (d);
      }
   }

   public void clearDraggers() {
      myDraggers.clear();
      for (GLViewer v : myViewers) {
         v.clearDraggers();
      }
   }

   public ViewerManager() {
   }

   public ViewerManager (GLViewer viewer) {
      this();
      ViewerMouseInputListener listener = new ViewerMouseInputListener();
      addMouseListener (listener);
      doAddViewer (viewer);
   }

   private void doAddViewer (GLViewer viewer) {
      myViewers.add (viewer);
      myPopupManagers.add (new PopupManager (viewer));
      viewer.setSelectionEnabled (selectionEnabledP);
      viewer.setSelectOnPress (selectOnPressP);
      setMouseBindings (viewer, getMouseBindings());
      viewer.getMouseHandler().setMouseWheelZoomScale (myMouseWheelZoomScale);
      for (MouseInputListener l : myMouseListeners) {
         viewer.addMouseInputListener (l);
      }
      for (KeyListener l : myKeyListeners) {
         viewer.addKeyListener (l);
      }
   }

   public void addViewer (GLViewer viewer) {
      if (getPopupManager (viewer) == null) {
         doAddViewer (viewer);
      }
      else {
         throw new IllegalStateException ("Viewer already known to manager");
      }
   }

   public void setSelectionHighlightStyle (HighlightStyle mode) {
      for (GLViewer v : myViewers) {
         v.setHighlightStyle (mode);
      }
   }

   public HighlightStyle getSelectionHighlightStyle() {
      if (myViewers.size() > 0) {
         return myViewers.get(0).getHighlightStyle();
      }
      else {
         return HighlightStyle.NONE;
      }
   }

   public boolean removeViewer (GLViewer viewer) {
      int idx = myViewers.indexOf (viewer);
      if (idx != -1) {
         for (MouseInputListener l : myMouseListeners) {
            viewer.removeMouseInputListener (l);
         }
         for (KeyListener l : myKeyListeners) {
            viewer.removeKeyListener (l);
         }
         myViewers.remove (idx);
         myPopupManagers.remove (idx);
         return true;
      }
      else {
         return false;
      }
   }

   public int numViewers() {
      return myViewers.size();
   }

   public GLViewer getViewer (int idx) {
      return myViewers.get(idx);
   }

   private PopupManager getPopupManager (GLViewer viewer) {
      for (PopupManager pm : myPopupManagers) {
         if (pm.getViewer() == viewer) {
            return pm;
         }
      }
      return null;
   }

   public void resetViewer (GLViewer viewer) {
      viewer.clearDraggers();
      for (Dragger3d d : myDraggers) {
         viewer.addDragger (d);
      }
      viewer.setExternalRenderList (getRenderList());
      if (myDefaultOrthographic) {
         viewer.autoFitOrtho ();
      }
      else {
         viewer.autoFitPerspective ();
      }

      if (myDefaultDrawAxes) {
         if (myDefaultAxisLength > 0) {
            viewer.setAxisLength (myDefaultAxisLength);
         }
         else {
            viewer.setAxisLength (GLViewer.AUTO_FIT);
         }
      }
      viewer.setGridVisible (myDefaultDrawGrid);
      viewer.setBackgroundColor (myBackgroundColor);
      viewer.setHighlightColor (mySelectionColor);
      viewer.setBlendDestFactor(GLViewer.DEFAULT_DST_BLENDING);
   }

   public void setBackgroundColor (Color color) {
      myBackgroundColor = color;
      for (GLViewer v : myViewers) {
         v.setBackgroundColor (color);
      }
   }

   public Color getBackgroundColor() {
      return myBackgroundColor;
   }

   public void setSelectionColor (Color color) {
      mySelectionColor = color;
      for (GLViewer v : myViewers) {
         v.setHighlightColor (color);
      }
   }

   public Color getSelectionColor() {
      return mySelectionColor;
   }

   public void resetViewers (AxisAngle frontView) {
      AxisAlignedRotation view = 
         AxisAlignedRotation.getNearest (new RotationMatrix3d(frontView));
      for (GLViewer v : myViewers) {
         v.setDefaultAxialView (view);
         resetViewer (v);
      }
   }

   RenderList buildRenderList() {
      RenderList list = new RenderList();
      list.addIfVisibleAll (myRenderables);
      RootModel root = Main.getMain().getRootModel();
      if (root != null) {
	 // modified by Ian: add RootModel to the render list, 
	 // 	note that prerender is called by addIfVisible()
	 //root.prerender (list);
	 list.addIfVisible(root);
      }
      return list;
   }

   public void render() {
      // System.out.println("vm_render");
      myRenderList = buildRenderList();
      for (GLViewer v : myViewers) {
         v.setExternalRenderList (myRenderList);
         v.rerender();
      }
   }
   
   /**
    * causes the repaint of the viewers
    * 
    */
   public void paint() {
      for (GLViewer v : myViewers) {
         v.paint();
      }
   }

   // ===============================================

   /**
    * Set the cursor on all the viewers.
    * 
    * @param cursor
    * The type of cursor to display.
    */
   public void setCursor (Cursor cursor) {
      for (GLViewer v : myViewers) {
         v.getCanvas().setCursor (cursor);
      }
   }

   /**
    * Remove the specified mouse input listener from each of the viewers.
    */
   public void removeMouseListener (MouseInputListener listener) {
      for (GLViewer v : myViewers) {
         v.removeMouseInputListener (listener);
      }

      myMouseListeners.remove (listener);
   }

   /**
    * Add a mouse input listener to each of the viewers.
    * 
    * @param listener listener to add
    */
   public void addMouseListener (MouseInputListener listener) {
      // add the listener to the list of listeners
      myMouseListeners.add (listener);

      // add the listener to each viewer
      for (GLViewer v : myViewers) {
         v.addMouseInputListener (listener);
      }
   }

   public ArrayList<MouseInputListener> getMouseListeners() {
      return myMouseListeners;
   }

   /**
    * Remove the specified key listener from each of the viewers.
    */
   public void removeKeyListener (KeyListener listener) {
      for (GLViewer v : myViewers) {
         v.removeKeyListener (listener);
      }
      myKeyListeners.remove (listener);
   }

   /**
    * Add a key listener to each of the viewers.
    * 
    * @param listener listener to add
    */
   public void addKeyListener (KeyListener listener) {
      // add the listener to the list of listeners
      myKeyListeners.add (listener);

      // add the listener to each viewer
      for (GLViewer v : myViewers) {
         v.addKeyListener (listener);
      }
   }

   public ArrayList<KeyListener> getKeyListeners() {
      return myKeyListeners;
   }

   /**
    * Set whether or not selection is being allowed on all the existing viewers.
    * Iterates through the viewers setting thier specific selection values to
    * that of the ViewerManager.
    * 
    * @param selection
    * Whether or not selection is enabled.
    */
   public void setSelectionEnabled (boolean selection) {
      selectionEnabledP = selection;

      for (GLViewer v : myViewers) {
         v.setSelectionEnabled (selectionEnabledP);
      }
   }

   /**
    * Returns true if viewer selection is enabled.
    * 
    * @return true if viewer selection is enabled
    */
   public boolean isSelectionEnabled() {
      return selectionEnabledP;
   }

   /**
    * Set whether or not selection is done when the mouse is pressed.
    * If enabled, this automatically diables "drag selection".
    * 
    * @param enable
    * Whether or not selection is enabled.
    */
   public void setSelectOnPress (boolean enable) {
      selectOnPressP = enable;

      for (GLViewer v : myViewers) {
         v.setSelectOnPress (selectOnPressP);
      }
   }

   /**
    * Returns true if "select on press" is enabled for the viewers.
    * 
    * @return true if "select on press" is enabled
    */
   public boolean getSelectOnPress() {
      return selectOnPressP;
   }
   
   public void resetEllipticCursorSize () {
      for (GLViewer v : myViewers) {
         v.resetEllipticCursorSize();
         v.repaint();
      }
   }  
   
   /**
    * Set whether or not elliptic selection is enabled in all the viewers.
    * 
    * @param enable
    * Whether or not elliptic selection is enabled.
    */
   public void setEllipticSelection (boolean enable) {
      ellipticSelectionP = enable;

      for (GLViewer v : myViewers) {
         v.setEllipticSelection (enable);
         v.setEllipticCursorActive (enable);
      }
   }

   /**
    * Returns true if elliptic selection is enabled for the viewers.
    * 
    * @return true if elliptic selection is enabled.
    */
   public boolean getEllipticSelection() {
      return ellipticSelectionP;
   }
   
   

   /**
    * Find the GLViewer (if any) associated with a particular component
    */
   public static GLViewer getViewerFromComponent (Component comp) {
      while (comp != null) {
         if (comp instanceof GLViewerPanel) {
            return ((GLViewerPanel)comp).getViewer();
         }
         else if (comp instanceof GLViewerFrame) {
            return ((GLViewerFrame)comp).getViewer();
         }
         else {
            comp = comp.getParent();
         }
      }
      return null;
   }

   private void displayPopup (MouseEvent e, PopupManager pm) {
      JPopupMenu popup = new JPopupMenu();
      popup.add (GuiUtils.createMenuItem (
         pm, "Set viewer properties", "Set properties for this viewer"));
      popup.add (GuiUtils.createMenuItem (
         pm, "Set viewer grid properties",
         "Set properties for this viewer's grid"));
      popup.add (GuiUtils.createMenuItem (
         pm, "Refresh view",
         "Refresh the rendered view, sorting faces if necessary"));
      popup.show (e.getComponent(), e.getX(), e.getY());
   }

   /**
    * The mouse input listener for a GLViewer.
    * 
    * This class needs to be instantiated once for the first GLViewer that is
    * created, then afterwards any viewers that are created automatically get
    * this listener attached to them by the ViewerManager.
    */
   private class ViewerMouseInputListener extends MouseInputAdapter {

      public ViewerMouseInputListener() {
         super();
      }

      public void mousePressed (MouseEvent e) {
         SelectionManager selectionManager =
            Main.getMain().getSelectionManager();
         if (e.getModifiersEx() == ButtonMasks.getContextMenuMask()) {
            if (selectionManager.getNumSelected() == 0) {
               GLViewer viewer = null;
               if (e.getSource() instanceof Component) {
                  viewer = getViewerFromComponent ((Component)e.getSource());
               }
               if (viewer != null) {
                  PopupManager pm = getPopupManager (viewer);
                  displayPopup (e, pm);
               }
            }
            else {
               selectionManager.displayPopup (e);
            }
         }

      }

   } // end viewerMouseInputListener

}
