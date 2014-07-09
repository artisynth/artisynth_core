/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import maspack.render.*;
import maspack.widgets.DraggerToolBar.ButtonType;

//import javax.swing.JToolBar;

public class ViewerFrame extends JFrame
   implements ActionListener, GLViewerListener {
   
   private static final long serialVersionUID = 1L;
   protected GLViewer viewer;

   protected ViewerToolBar myViewerToolBar;
   protected DraggerToolBar myDraggerToolBar;
   protected JPanel myTopPanel;
   protected JPanel myLeftPanel;
   protected JColorChooser myColorChooser = new JColorChooser();
   protected int myGridDisplayIndex;
   protected GridDisplay myGridDisplay;
   protected ViewerPopupManager myPopupManager;

   public GLViewer getViewer() {
      return viewer;
   }

   public ViewerFrame (String name, int width, int height) {
      super (name);
      viewer = new GLViewer (width, height);
      getContentPane().add (viewer.getCanvas());
      viewer.addViewerListener (this);
      pack();
   }

   public ViewerFrame (GLViewer shareWith, String name, int width, int height) {
      this (shareWith, name, width, height, false);
   }

   public ViewerFrame (GLViewer shareWith, String name, int width,
                       int height, boolean undecorated) {
      super (name);
      viewer = new GLViewer (shareWith, width, height);
      setUndecorated (undecorated);
      getContentPane().add (viewer.getCanvas());
      viewer.addViewerListener (this);
      pack();
   }

   public void addRenderable (GLRenderable r) {
      viewer.addRenderable(r);
   }

   public void removeRenderable (GLRenderable r) {
      viewer.removeRenderable(r);
   }

   public void clearRenderables() {
      viewer.clearRenderables();
   }

   protected JMenuItem addMenuItem (JMenu menu, String name) {
      JMenuItem item = new JMenuItem (name);
      item.addActionListener(this);
      item.setActionCommand(name);
      menu.add (item);
      return item;
   }

   protected JMenuItem addMenuItem (JPopupMenu popup, String name) {
      JMenuItem item = new JMenuItem (name);
      item.addActionListener(this);
      item.setActionCommand(name);
      popup.add (item);
      return item;
   }

   protected void createFileMenu (JMenu menu) {
      addMenuItem (menu, "Quit");
   }

   protected void createViewMenu (JMenu menu) {
      addMenuItem (menu, "Background color");
      if (viewer.isOrthogonal()) {
         addMenuItem(menu, "Perspective view");
      }
      else {
         addMenuItem(menu, "Orthographic view");
      }
   }   

   public JMenuBar addMenuBar() {
      JMenuBar menuBar = new JMenuBar();
      setJMenuBar (menuBar);

      JMenu menu = new JMenu ("File");
      menu.addMenuListener(new MenuAdapter() {
         public void menuSelected(MenuEvent m_evt) {
            createFileMenu((JMenu)m_evt.getSource());
         }
      });      
      menuBar.add (menu);      

      menu = new JMenu ("View");
      menu.addMenuListener(new MenuAdapter() {
         public void menuSelected(MenuEvent m_evt) {
            createViewMenu((JMenu)m_evt.getSource());
         }
      });      
      menuBar.add (menu);  
      return menuBar;
   }

   public void addTopToolPanel() {
      if (myTopPanel != null) {
         throw new IllegalStateException ("Top tool panel already added");
      }
      myTopPanel = new JPanel();
      myTopPanel.setLayout(new BoxLayout(myTopPanel, BoxLayout.LINE_AXIS));
      myTopPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      getContentPane().add(myTopPanel, BorderLayout.NORTH);
   }

   public void addGridDisplay() {
      if (myGridDisplay != null) {
         throw new IllegalStateException ("Grid display already added");
      }
      if (myTopPanel == null) {
         addTopToolPanel();
      }
      // add glue to separate label from grid display
      myTopPanel.add(Box.createHorizontalGlue());
      myGridDisplayIndex = myTopPanel.getComponentCount();
      // add a place-holder component for the grid display
      myTopPanel.add(GridDisplay.createPlaceHolder());
   }

   public void addLeftToolPanel() {
      if (myLeftPanel != null) {
         throw new IllegalStateException ("Left tool panel already added");
      }
      myLeftPanel = new JPanel(new BorderLayout());
      getContentPane().add(myLeftPanel, BorderLayout.WEST);
   }

   public void addViewerToolBar (int orientation) {
      if (myViewerToolBar != null) {
         throw new IllegalStateException ("Viewer tool bar already added");
      }
      myViewerToolBar = new ViewerToolBar (viewer, /*addGridPanel=*/false);
      myViewerToolBar.setOrientation(orientation);
      if (orientation == JToolBar.VERTICAL) {
         if (myLeftPanel == null) {
            addLeftToolPanel();
         }
         myLeftPanel.add (myViewerToolBar, BorderLayout.SOUTH);
      }
      else {
         if (myTopPanel == null) {
            addTopToolPanel();
         }
         myTopPanel.add (myViewerToolBar, BorderLayout.WEST);
      }
   }

   public void addDraggerToolBar (ButtonType... buttonTypes) {
      if (myDraggerToolBar != null) {
         throw new IllegalStateException ("Dragger tool bar already added");
      }
      if (myLeftPanel == null) {
         addLeftToolPanel();
      }
      myDraggerToolBar = new DraggerToolBar (viewer, null, buttonTypes);
      myDraggerToolBar.setOrientation(JToolBar.VERTICAL);
      myLeftPanel.add (myDraggerToolBar, BorderLayout.NORTH);
   }

   public DraggerToolBar getDraggerToolBar() {
      return myDraggerToolBar;
   }

   public void addPopupManager() {
      myPopupManager = new ViewerPopupManager (viewer);
       viewer.addMouseInputListener (new MouseInputAdapter() {
         public void mousePressed (MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) {
               displayPopup (e);
            }
         }
      });     
   }

   public void addKeyListener() {
      viewer.addKeyListener (new ViewerKeyListener(viewer));
   }

   public void setBackgroundColor() {

      myColorChooser.setColor(viewer.getBackgroundColor());

      ActionListener setBColor = new ActionListener() {   
         @Override
         public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals("OK")) {
               viewer.setBackgroundColor(myColorChooser.getColor());
            } else if (cmd.equals("Cancel")) {
               // do nothing
            }
         }
      };
      JDialog dialog =
      JColorChooser.createDialog(
         this, "color chooser", /* modal= */true, myColorChooser,
         setBColor, setBColor);
      GuiUtils.locateRight(dialog, this);
      dialog.setVisible(true);
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Quit")) {
         System.exit (0);
      }
      else if (cmd.equals("Edit viewer props")) {
         PropertyDialog dialog =
            myPopupManager.createPropertyDialog ("OK Cancel");
         dialog.setVisible(true);
      }
      else if (cmd.equals("Background color")) {
         setBackgroundColor();
      }
      else if (cmd.equals("Perspective view")) {
         viewer.setOrthographicView(false);
      }
      else if (cmd.equals("Orthographic view")) {
         viewer.setOrthographicView(true);
      }
   }

   protected void createPopupMenu (JPopupMenu popup) {
      JMenuItem item = new JMenuItem("Edit viewer props");
      item.addActionListener(this);
      item.setActionCommand("Edit viewer props");
      popup.add(item);      
   }

   protected void displayPopup (MouseEvent evt) {
      JPopupMenu popup = new JPopupMenu();
      createPopupMenu (popup);
      popup.setLightWeightPopupEnabled (false);
      popup.show (evt.getComponent(), evt.getX(), evt.getY());
   }

   protected void updateWidgets() {
      // return if  not visible, since updating widgets while
      // frame is being set visible can cause some problems
      if (!isVisible()) {
         return;
      }
      if (myTopPanel != null) {
         // if we have a top panel, then we have a grid display there
         boolean gridOn = viewer.getGridVisible();
         GLGridPlane grid = viewer.getGrid();
         if ((myGridDisplay != null) != gridOn) {
            if (gridOn) {
               myGridDisplay =
                  GridDisplay.createAndAdd (
                     grid, myTopPanel, myGridDisplayIndex);
            }
            else {
               GridDisplay.removeAndDispose (
                  myGridDisplay, myTopPanel, myGridDisplayIndex);
               myGridDisplay = null;
            }
         }
         if (myGridDisplay != null) {
            myGridDisplay.updateWidgets();
         }
      }
   }

   /**
    * interface face method for GLViewerListener.
    */
   public void renderOccurred (GLViewerEvent e) {
      updateWidgets();
   }



}
