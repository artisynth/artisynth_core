/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import maspack.render.GL.GLGridPlane;
import maspack.render.GL.GLGridResolution;
import maspack.render.GL.GLViewer;
import maspack.util.Disposable;

public class GridDisplay extends GridResolutionField 
   implements ActionListener, Disposable {

   GLGridPlane myGrid;
   PropertyDialog myPropDialog;

   //JMenuItem myPropertyItem;

   public GridDisplay (String label, GLGridPlane grid) {
      super (label);
      getTextField().setEnabled (true);
      addMouseListener (new MouseAdapter() {
            public void mousePressed (MouseEvent e) {
               if (e.getModifiersEx() == ButtonMasks.getContextMenuMask()) {
                  JPopupMenu popup = createPopup();
                  popup.show (GridDisplay.this, 0, getHeight());
               }
            }
         });
      addValueChangeListener (new ValueChangeListener() {
            public void valueChange (ValueChangeEvent e) {
               myGrid.setResolution ((GLGridResolution)getValue());
               GLViewer viewer = myGrid.getViewer();
               if (viewer != null) { // shouldn't be null, just being paranoid
                  viewer.rerender();
               }
            }
         });
      //createMenuItems();
      myGrid = grid;
   }

   private void showPropertyDialog() {
      myPropDialog = new PropertyDialog ("Grid properties", myGrid, "Done");
      myPropDialog.addWindowListener (new WindowAdapter() {
         public void windowClosed (WindowEvent e) {
            removePropertyDialog();
         }
      });
      GuiUtils.locateRight (myPropDialog, this);
      myPropDialog.addGlobalValueChangeListener (
         new ValueChangeListener() {
            public void valueChange (ValueChangeEvent e) {
               GLViewer viewer = myGrid.getViewer();
               if (viewer != null) {
                  viewer.rerender();
               }
            }
         });
      myPropDialog.setVisible (true);
   }

   void removePropertyDialog() {
      if (myPropDialog != null) {
         myPropDialog.dispose();
         myPropDialog = null;
      }
   }

   // private void createMenuItems() {
   //    myPropertyItem =
   //       createMenuItem (
   //          "Set properties", "Set properties", "set properties for this grid");
   // }

   private JMenuItem createMenuItem (
      String cmd, String toolTipText) {
      JMenuItem item = new JMenuItem (cmd);
      item.setActionCommand (cmd);
      item.addActionListener (this);
      item.setToolTipText (toolTipText);
      return item;
   }

   private JPopupMenu createPopup() {
      JPopupMenu menu = new JPopupMenu();
      
      if (!myGrid.isAutoSized()) {
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
      if (myPropDialog == null) {
         menu.add (
            createMenuItem (
               "Set properties", "set properties for this grid"));
      }
      return menu;
   }

   public void actionPerformed (ActionEvent e) {
      String command = e.getActionCommand();

      if (command == "Set properties") {
         showPropertyDialog();
      }
      else if (command == "Turn auto-sizing on") {
         myGrid.setAutoSized(true);
      }
      else if (command == "Turn auto-sizing off") {
         myGrid.setAutoSized(false);
       }
   }

   public void updateWidgets() {
      // mask value change listeners so that we don't generate a spurious call
      // to myGrid.setResolution() and thereby turn off auto-sizing
      maskValueChangeListeners (true);
      setValue (myGrid.getResolution());
      maskValueChangeListeners (false);
      if (myPropDialog != null) {
         myPropDialog.updateWidgetValues();
      }
   }

   public void dispose() {
      removePropertyDialog();
   }

   protected void finalize() throws Throwable {
      try {
         dispose();
      }
      finally {
         super.finalize();
      }
   }

   /**
    * Creates a grid display and add it to a panel at the location specified by
    * idx. It is assumed that a place holder component already exists at the
    * location.
    */
   public static GridDisplay createAndAdd (
      GLGridPlane grid, JComponent panel, int idx) {
      GridDisplay display = new GridDisplay ("Grid:", grid);
      panel.remove (idx);
      panel.add (display, idx);
      panel.revalidate();
      //panel.repaint();
      //display.updateWidgets();
      return display;
   }

   /**
    * Removes a grid display from a panel at the location specified by idx.  A
    * placeholder object is then added at the indicated location.
    */
   public static void removeAndDispose (
      GridDisplay display, JComponent panel, int idx) {
      panel.remove (display);
      panel.add (createPlaceHolder(), idx);
      display.dispose();
      panel.revalidate();
      panel.repaint();
   }

   /**
    * Creates a place-holder component for a grid display.
    */
   public static Component createPlaceHolder() {
      return Box.createRigidArea(new Dimension(32, 19));
   }
   

}
