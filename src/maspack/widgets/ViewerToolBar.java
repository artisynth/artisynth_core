/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.RigidTransform3d;
import maspack.render.Dragger3d.DraggerType;
import maspack.render.GridPlane;
import maspack.render.RenderListener;
import maspack.render.RendererEvent;
import maspack.render.Viewer;
import maspack.render.GL.GLClipPlane;
import maspack.util.InternalErrorException;

public class ViewerToolBar extends JToolBar 
   implements ActionListener, RenderListener {
   private static final long serialVersionUID = -5831854489618084709L;

   private static HashMap<String,ImageIcon> myIcons =
       new HashMap<String,ImageIcon>();

   private static ImageIcon clipPlaneIcon = loadIcon ("clipPlane.png");
   private static ImageIcon noClipPlaneIcon = loadIcon ("noClipPlane.png");
   private static ImageIcon clipPlaneAddIcon = loadIcon ("clipPlaneAdd.png");

   protected GridDisplay myGridDisplay = null;
   protected int myGridDisplayIndex = -1;
   protected boolean myGridDisplayEnabled = true;
   protected int myButtonIndex;

   private static ImageIcon loadIcon (String fileName) {
      return GuiUtils.loadIcon (ViewerToolBar.class, "icons/" + fileName);
   }

   protected LinkedList<ClipPlaneControl> myClipPlaneControls =
      new LinkedList<ClipPlaneControl>();

   protected Viewer myViewer;
   JButton myAxialViewButton;
   JButton myClipPlaneAddButton;

   static AxisAlignedRotation myDefaultAxialViews[] = new AxisAlignedRotation[] {
      AxisAlignedRotation.X_Z,
      AxisAlignedRotation.X_Y,
      AxisAlignedRotation.Y_Z,
      AxisAlignedRotation.Y_X,
      AxisAlignedRotation.Z_X,
      AxisAlignedRotation.Z_Y,

      AxisAlignedRotation.NX_Z,
      AxisAlignedRotation.NX_Y,
      AxisAlignedRotation.NY_Z,
      AxisAlignedRotation.NY_X,
      AxisAlignedRotation.NZ_X,
      AxisAlignedRotation.NZ_Y,

      AxisAlignedRotation.X_NZ,
      AxisAlignedRotation.X_NY,
      AxisAlignedRotation.Y_NZ,
      AxisAlignedRotation.Y_NX,
      AxisAlignedRotation.Z_NX,
      AxisAlignedRotation.Z_NY,

      AxisAlignedRotation.NX_NZ,
      AxisAlignedRotation.NX_NY,
      AxisAlignedRotation.NY_NZ,
      AxisAlignedRotation.NY_NX,
      AxisAlignedRotation.NZ_NX,
      AxisAlignedRotation.NZ_NY,
   };
   
   ArrayList<AxialViewMenuItem> myAxialViewMenuItems;

   private static class AxialViewMenuItem extends JMenuItem {
      private static final long serialVersionUID = -7023461128958734796L;
      private AxisAlignedRotation myAxialView;

      AxialViewMenuItem (AxisAlignedRotation view) {
         super();
         setAxialView (view);
         setIconTextGap (0);
      }      

      // public void setIcon (Icon icon) {
      //    super.setIcon (icon);
      //    System.out.println ("prefsize=" + getPreferredSize());
      // }

      AxisAlignedRotation getAxialView() {
         return myAxialView;
      }

      void setAxialView (AxisAlignedRotation view) {
         setIcon (getIconFromFile (getOrientationIconName(view)));
         setToolTipText (
            "Align axes to " + view.toString().toLowerCase() + " view");
         myAxialView = view;
      }      

   }

   private static String getOrientationIconName (AxisAlignedRotation rot) {
      return "axes_" + rot.toString().toLowerCase();
   }

   private static ImageIcon getIconFromFile (String name) {
      ImageIcon icon = myIcons.get(name);
      if (icon == null) {
         // need to load the icon
         icon = loadIcon (name+".png");
         myIcons.put (name, icon);
      }
      return icon;
   }

   private JButton createAndAddButton (
      String actionCmd, String toolTipText, Icon icon, 
      ActionListener listener) {
      JButton button =
         ButtonCreator.createIconicButton (
            icon, actionCmd, toolTipText, ButtonCreator.BUTTON_ENABLED, true,
            listener);
      add (button, myButtonIndex++);
      return button;
   }

   private ArrayList<AxialViewMenuItem> createAxialViewMenuItems (
      ActionListener l) {

      ArrayList<AxialViewMenuItem> itemList = new ArrayList<>();
      for (AxisAlignedRotation rot : myDefaultAxialViews) {
         AxialViewMenuItem item = new AxialViewMenuItem (rot);
         item.addActionListener (l);
         itemList.add (item);
      }
      return itemList;
   }

   private void setAxialViewIcon (Icon icon) {
      myAxialViewButton.setIcon (icon);
   }

   public ViewerToolBar (Viewer viewer, boolean allowGridDisplay) {
      super ("viewer toolbar");
      JPopupMenu.setDefaultLightWeightPopupEnabled (false);
      myViewer = viewer;
      myViewer.addRenderListener (this);
      myGridDisplayEnabled = allowGridDisplay;

      // just get a stub icon for now ...
      ImageIcon tempIcon = getIconFromFile ("axes_x_z");

      myAxialViewButton =
         createAndAddButton (
            "selectAxialView", "select an axis-aligned view", tempIcon, this);

      myClipPlaneAddButton =
         createAndAddButton (
            "addClipPlane", "add a clip-plane", clipPlaneAddIcon, this);

      if (myGridDisplayEnabled) {
         // add glue to separate buttons from the grid display
         add (Box.createHorizontalGlue());
         myGridDisplayIndex = getComponentCount();
         // add a placeholder for the grid display
         add(GridDisplay.createPlaceHolder());
      }

      myAxialViewMenuItems = createAxialViewMenuItems (this);
   }

   public void actionPerformed (ActionEvent e) {
      // set the newly selected item
      Object source = e.getSource();
      String command = e.getActionCommand();

      for (AxialViewMenuItem item : myAxialViewMenuItems) {
         if (source == item) {
            myViewer.setAxialView (item.getAxialView());
            myViewer.autoFit();
            setAxialViewIcon (item.getIcon());
            return;
         }
      }
      if (command == "selectAxialView") {
         JPopupMenu menu = createViewPopup();
         menu.show (myAxialViewButton, 0, myAxialViewButton.getHeight());
      }
      else if (command == "addClipPlane") {
         addClipPlane();
      }
   }

   private void addClipPlane() {
      GLClipPlane clipPlane = myViewer.addClipPlane(null, 0);

      ClipPlaneControl ctrl = new ClipPlaneControl (clipPlane);

      ctrl.setColor (getClipPlaneColor (myClipPlaneControls.size()));
      if (myViewer.numFreeClipSurfaces() > 0) {
         ctrl.setActive (true);
      }
      else {
         ctrl.setActive (false);
      }
      myClipPlaneControls.add (ctrl);
   }

   private void removeClipPlane (ClipPlaneControl ctrl) {
      myClipPlaneControls.remove (ctrl);
      myViewer.removeClipPlane (ctrl.myClipPlane);
      remove (ctrl.myButton);
      myButtonIndex--;
      ctrl.dispose();
      revalidate();
   }
   
   public void clearClipPlanes() {
      for (ClipPlaneControl ctrl : myClipPlaneControls) {
         removeClipPlane (ctrl);
      }
   }

   /**
    * Create the icon array used to select a new axial view.
    */
   private JPopupMenu createViewPopup () {
      JPopupMenu menu = new JPopupMenu();

      VerticalGridLayout menuGrid = new VerticalGridLayout (6, 0);
      menu.setLayout (menuGrid);

      // find the current view and the associated index in itemList
      AxisAlignedRotation curView = myViewer.getAxialView();
      int curIdx = -1;
      for (int idx=0; idx<myAxialViewMenuItems.size(); idx++) {
         if (myAxialViewMenuItems.get(idx).myAxialView == curView) {
            curIdx = idx;
            break;
         }
      }
      if (curIdx == -1) {
         // this really can't happen ...
         throw new InternalErrorException (
            "Current view not present in menu item list");
      }
      // The menu items are arranged in a 6 x 4 grid, with items added in
      // column-major order, so that if idx is the idx-th item added, we have
      //
      // idx = col*6 + row
      // col = idx/6
      // row = idx%6
      // 
      // The default ordering is for items to be added in the order supplied by
      // myAxialViewMenuItems. However, if the current view does not correspond
      // to the first item in this list, we shift the rows, as well as the
      // columns for the first row, so that the current item appears in
      // location (0, 0).

      // Find the row and column associated with the current view
      int curRow = curIdx%6;
      int curCol = curIdx/6;
      // Create shifted row and column indices that allow the current index to
      // assume location (0, 0)
      int[] shiftedRows = new int[6];
      int row = 0;
      shiftedRows[row++] = curRow;
      for (int i=0; i<shiftedRows.length; i++) {
         if (i != curRow) {
            shiftedRows[row++] = i;
         }
      }
      int[] shiftedCols = new int[4];
      int col = 0;
      shiftedCols[col++] = curCol;
      for (int i=0; i<shiftedCols.length; i++) {
         if (i != curCol) {
            shiftedCols[col++] = i;
         }
      }
      // Add the menu items in an order that accounts for the row/column
      // shifting
      for (int j=0; j<4; j++) {
         for (int i=0; i<6; i++) {
            row = shiftedRows[i];
            col = (i==0 ? shiftedCols[j] : j);
            menu.add (myAxialViewMenuItems.get(col*6+row));
         }
      }
      return menu;
   }

   Color getClipPlaneColor (int index) {
      switch (index % 6) {
         case 0: {
            return new Color (87, 163, 128); // forest green
         }
         case 1: { // return new Color (0.2f, 0.2f, 0.8f);
            return new Color (86, 105, 164); // blue gray
         }
         case 2: {
            return new Color (204, 161, 50); // gold
         }
         case 3: { // return new Color (0.8f, 0.2f, 0.8f);
            return new Color (190, 100, 190); // mauve
         }
         case 4: {
            return new Color (56, 214, 197);
         }
         case 5: {
            return new Color (189, 68, 68); // rust red
         }
         default: {
            throw new InternalErrorException (
               "color not defined for index " + index);
         }
      }

   }

   class ClipPlaneControl implements ActionListener {
      JButton myButton;
      GLClipPlane myClipPlane;
      Color mySelectedColor;
      Color myButtonColor;
      Color myColor = Color.LIGHT_GRAY;
      PropertyDialog myPropDialog;

      Border mySelectedBorder = new BevelBorder (BevelBorder.LOWERED);
      Border myRegularBorder;

      JMenuItem myPropertyItem;
      JMenuItem myResetItem;
      JMenuItem myCenterItem;
      JMenuItem myEnableSlicingItem;
      JMenuItem myDisableSlicingItem;
      JMenuItem myHideTransformerItem;
      JMenuItem myShowTransformerItem;
      JMenuItem myShowGridItem;
      JMenuItem myHideGridItem;
      JMenuItem myDeleteItem;
      // JMenuItem myChangeColorItem;

      ArrayList<JMenuItem> myAlignAxisMenuItems;

      public Color getColor() {
         return myColor;
      }

      private void setOtherColors() {
         myButtonColor = myColor;
         mySelectedColor = myButtonColor.brighter();
      }

      public void setColor (Color color) {
         myColor = color;
         setOtherColors();
         myClipPlane.setMajorColor (myColor);
      }

      ClipPlaneControl (GLClipPlane clipPlane) {
         setOtherColors();
         myButton =
            createAndAddButton (
               "activate", "activate/deactive this clip-plane", clipPlaneIcon,
               this);
         myRegularBorder = myButton.getBorder();

         myButton.addMouseListener (new MouseAdapter() {
            public void mousePressed (MouseEvent e) {
               if (e.getModifiersEx() == ButtonMasks.getContextMenuMask()) {
                  JPopupMenu popup = createPopup();
                  popup.show (myButton, 0, myButton.getHeight());
               }
            }
         });
         createMenuItems();
         myClipPlane = clipPlane;
      }


      private JMenuItem createMenuItem (String cmd, String toolTipText) {
         JMenuItem item = new JMenuItem (cmd);
         item.setActionCommand (cmd);
         item.addActionListener (this);
         item.setToolTipText (toolTipText);
         return item;
      }

      private void createMenuItems() {
         myPropertyItem =
            createMenuItem (
               "Set properties", "set properties for this clip plane");
         myResetItem =
            createMenuItem (
               "Reset", "reset clip plane to current view");
         myCenterItem =
            createMenuItem (
               "Center", "center clip plane to current view");

         myAlignAxisMenuItems = new ArrayList<JMenuItem>();
         myAlignAxisMenuItems.add (createMenuItem (
            "Align +X", "align with the +X axis"));
         myAlignAxisMenuItems.add (createMenuItem (
            "Align -X", "align with the -X axis"));
         myAlignAxisMenuItems.add (createMenuItem (
            "Align +Y", "align with the +Y axis"));
         myAlignAxisMenuItems.add (createMenuItem (
            "Align -Y", "align with the -Y axis"));
         myAlignAxisMenuItems.add (createMenuItem (
            "Align +Z", "align with the +Z axis"));
         myAlignAxisMenuItems.add (createMenuItem (
            "Align -Z", "align with the -Z axis"));

         myEnableSlicingItem =
            createMenuItem (
               "Enable slicing", "enable slicing for this clip plane");
         myDisableSlicingItem =
            createMenuItem (
               "Disable slicing", "disable slicing for this clip plane");
         
         myShowTransformerItem =
            createMenuItem (
               "Show transformer", "show transformer dragger");
         myHideTransformerItem =
            createMenuItem (
               "Hide transformer", "hide transformer dragger");
         myShowGridItem =
            createMenuItem (
               "Show grid", "show a grid for the plane");
         myHideGridItem =
            createMenuItem ("Hide grid", "hide the plane grid");
         myDeleteItem =
            createMenuItem ("Delete", "delete this clip plane");
      }

      private JPopupMenu createPopup() {
         JPopupMenu menu = new JPopupMenu();

         if (myPropDialog == null) {
            menu.add (myPropertyItem);
         }
         if (!myClipPlane.isAutoSized()) {
            menu.add (
               createMenuItem (
                  "Turn auto-sizing on",
               "enable grid to size itself with viewer zoom level"));
         }
         else {
            menu.add (
               createMenuItem (
                  "Turn auto-sizing off",
               "disable grid from sizing itself with viewer zoom level"));
         }

         if (myClipPlane.isSlicingEnabled()) {
            menu.add(myDisableSlicingItem);
         }
         else {
            menu.add(myEnableSlicingItem);
            myEnableSlicingItem.setEnabled (
               myViewer.numFreeClipSurfaces() > 0);
         }
         if (myClipPlane.isGridVisible()) {
            menu.add (myHideGridItem);
         }
         else {
            menu.add (myShowGridItem);
         }
         if (myClipPlane.getDragger() == DraggerType.None) {
            menu.add (myShowTransformerItem);
         }
         else {
            menu.add (myHideTransformerItem);
         }
         menu.add (myResetItem);
         menu.add (myCenterItem);
         for (JMenuItem item : myAlignAxisMenuItems) {
            menu.add (item);
         }
         menu.add (myDeleteItem);

         return menu;
      }

      private void updateButtonAppearance() {
         if (myButton.isSelected()) {
            myButton.setBorder (mySelectedBorder);
            myButton.setBackground (mySelectedColor);
         }
         else {
            myButton.setBorder (myRegularBorder);
            myButton.setBackground (myButtonColor);
         }
         myButton.repaint();
      }
      
      public void setActive (boolean active) {
            if (active) {
               myClipPlane.setClippingEnabled (true);
               myButton.setActionCommand ("deactivate");
               myButton.setSelected (true);
               myButton.setIcon (clipPlaneIcon);
            }
            else {
               myClipPlane.setClippingEnabled (false);
               myButton.setActionCommand ("activate");
               myButton.setSelected (false);
               myButton.setIcon (noClipPlaneIcon);
            }
            updateButtonAppearance();
      }

      void showPropertyDialog() {
         myPropDialog =
            new PropertyDialog ("Clip plane properties", myClipPlane, "Done");
         myPropDialog.addWindowListener (new WindowAdapter() {
            public void windowClosed (WindowEvent e) {
               removePropertyDialog();
            }
         });
         GuiUtils.locateRight (myPropDialog, ViewerToolBar.this);
         myPropDialog.addGlobalValueChangeListener (
            new ValueChangeListener() {
               public void valueChange (ValueChangeEvent e) {
                  myViewer.rerender();
               }
            });
         myPropDialog.setVisible (true);
      }

      void removePropertyDialog() {
         if (myPropDialog != null) {
            //Main.getWorkspace().deregisterDisposable (myPropDialog);
            // myViewer.removeViewerListener (this);
            myPropDialog.dispose();
            myPropDialog = null;
         }
      }

      private void alignToAxis (String cmd) {
         RigidTransform3d X =
            new RigidTransform3d (myClipPlane.getGridToWorld());
         if (cmd == "Align +X") {
            X.R.setAxisAngle (0, 1, 0, Math.PI / 2);
         }
         else if (cmd == "Align -X") {
            X.R.setAxisAngle (0, 1, 0, -Math.PI / 2);
         }
         else if (cmd == "Align +Y") {
            X.R.setAxisAngle (1, 0, 0, -Math.PI / 2);
         }
         else if (cmd == "Align -Y") {
            X.R.setAxisAngle (1, 0, 0, Math.PI / 2);
         }
         else if (cmd == "Align +Z") {
            X.R.setIdentity();
         }
         else if (cmd == "Align -Z") {
            X.R.setAxisAngle (1, 0, 0, Math.PI);
         }
         myClipPlane.setGridToWorld (X);
      }

      public void actionPerformed (ActionEvent e) {
         String command = e.getActionCommand();

         if (command == "Set properties") {
            showPropertyDialog();
         }
         else if (command == "Turn auto-sizing on") {
            myClipPlane.setAutoSized (true);
         }
         else if (command == "Turn auto-sizing off") {
            myClipPlane.setAutoSized (false);
         }
         else if (command == "activate") {
            int numPlanesNeeded = 1;
            if (myClipPlane.isSlicingEnabled()) {
               numPlanesNeeded++;
            }
            if (myViewer.numFreeClipSurfaces() >= numPlanesNeeded) {
               setActive (true);
            }
         }
         else if (command == "deactivate") {
            setActive (false);
         }
         else if (command == "Enable slicing") {
        	myClipPlane.setSlicingEnabled(true);
         }
         else if (command == "Disable slicing") {
        	myClipPlane.setSlicingEnabled(false);
         }
         else if (command == "Show transformer") {
            myClipPlane.setDragger (DraggerType.Transrotator);
            myClipPlane.setGridVisible (true);
         }
         else if (command == "Hide transformer") {
            myClipPlane.setDragger (DraggerType.None);
         }
         else if (command == "Hide grid") {
            myClipPlane.setGridVisible (false);
         }
         else if (command == "Show grid") {
            myClipPlane.setGridVisible (true);
         }
         else if (command == "Center") {
            myClipPlane.centerInViewer();
         }
         else if (command == "Reset") {
            myClipPlane.resetInViewer();
         }
         else if (command == "Delete") {
            removeClipPlane (this);
         }
         // else if (command == "changeColor")
         // {
         // changeColor();
         // }
         else {
            for (JMenuItem item : myAlignAxisMenuItems) {
               if (e.getSource() == item) {
                  alignToAxis (item.getActionCommand());
               }
            }
         }
      }

      public void dispose() {
         removePropertyDialog();
      }

      public void finalize() {
         dispose();
      }

      void updateWidgets() {
         if (myPropDialog != null) {
            myPropDialog.updateWidgetValues();
         }
         if (!myColor.equals (myClipPlane.getMajorColor())) {
            setColor (myClipPlane.getMajorColor());
            updateButtonAppearance();
         }
      }
   }

   /**
    * Called after the viewer state has been changed by some other agent, to
    * update the appearance of the widgets and icons.
    */
   public void updateWidgets() {
      if (myGridDisplayEnabled) {
         boolean gridOn = myViewer.getGridVisible();
         GridPlane grid = myViewer.getGrid();
         if ((myGridDisplay != null) != gridOn) {
            if (gridOn) {
               myGridDisplay =
                  GridDisplay.createAndAdd (grid, this, myGridDisplayIndex);
            }
            else {
               GridDisplay.removeAndDispose (
                  myGridDisplay, this, myGridDisplayIndex);
               myGridDisplay = null;
            }
         }
      }
      if (myGridDisplay != null) {
         myGridDisplay.updateWidgets();
      }
      for (ClipPlaneControl cpc : myClipPlaneControls) {
         cpc.updateWidgets();
      }
   }

   public void renderOccurred (RendererEvent e) {
      updateWidgets();
   }
   
   public void dispose() {
      if (myViewer != null) {
         myViewer.removeRenderListener (this);
      }
   }
   
   protected void finalize() throws Throwable {
      try {
         dispose();
      }
      finally {
         super.finalize();
      }
   }
}
