/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import maspack.util.InternalErrorException;
import maspack.widgets.ButtonCreator;
import maspack.widgets.ButtonMasks;
import maspack.widgets.GuiUtils;
import maspack.widgets.*;
import maspack.properties.*;
import artisynth.core.gui.Displayable;
import artisynth.core.driver.Main.SelectionMode;

public class SelectionToolbar extends JToolBar implements ActionListener {

   private class ButtonInfo {
      JButton myButton;
      PropertyDialog myPropDialog;
      HasProperties myPropHost;
      SelectionMode myMode;
      
      ButtonInfo (SelectionMode mode, JButton button) {
         myMode = mode;
         myButton = button;
      }

      void showPropertyDialog() {
         if (myPropHost == null) {
            return;
         }
         if (myPropDialog == null) {
            PropertyPanel panel = new PropertyPanel();
            String[] excludeProps = new String[] {
               "name", "navpanelVisibility", "renderProps" };
            myPropDialog =
               new PropertyDialog (
                  myMode + " properties", myPropHost, excludeProps, "OK");
            myPropDialog.pack();
         }
         myPropDialog.locateRight(myMain.getFrame());
         //dialog.setSynchronizeObject(myMain.getRootModel());
         // dialog.addWindowListener(new WindowAdapter() {
         //    public void windowClosed(WindowEvent e) {
         //       myPullControllerPropertyDialog = null;
         //    }
         // });
         if (!myPropDialog.isVisible()) {
            myMain.registerWindow (myPropDialog);
            myPropDialog.setVisible(true);
         }
      }
   }

   ArrayList<ButtonInfo> myButtons = new ArrayList<>();
   ButtonInfo myLastSelectedButton = null;

   private Main myMain;
   private ThinBevelBorder thinBevelBorder;
   private BevelBorder bevelBorder;
   private Border border;
   private Color background;
   
   private HashMap<SelectionMode,HasProperties> myModeHostMap;

   private MouseAdapter myMouseAdapter;

   private class LocalMouseAdapter extends MouseAdapter {

      @Override
      public void mousePressed (MouseEvent e) {
         if (e.getModifiersEx() == ButtonMasks.getContextMenuMask()) {
            maybeShowContextPopup(e);
         }
      }
   }

   private void maybeShowContextPopup (MouseEvent e) {
      if (e.getSource() instanceof JButton) {
         JButton button = (JButton)e.getSource();
         ButtonInfo binfo = getButtonInfo (button);
         if (binfo == null) {
            throw new UnsupportedOperationException (
               "Unknown button " + button.getText());
         }
         if (binfo.myPropHost != null) {
            myLastSelectedButton = binfo;
            JPopupMenu popup = new JPopupMenu();
            JMenuItem item = new JMenuItem ("Edit properties ...");
            item.addActionListener (this);
            popup.add (item);
            popup.setLightWeightPopupEnabled (false);
            popup.show (e.getComponent(), e.getX(), e.getY());
         }
      }
   }

   private String uncapitalize (String str) {
      if (str == null || str.isEmpty()) {
        return str;
      }
      return str.substring(0, 1).toLowerCase() + str.substring(1);
   }

   private JButton createButton (
      SelectionMode mode, String iconFileName, String toolTipText) {

      String modeName = uncapitalize (mode.toString());
      ImageIcon icon = null;
      if (iconFileName != null) {
         icon = GuiUtils.loadIcon (
            Displayable.class, "icon/"+iconFileName);
      }
      if (icon != null) {
         icon.setDescription (modeName);
      }
      JButton button =
         ButtonCreator.createIconicButton (
            icon, modeName + "mode", toolTipText, ButtonCreator.BUTTON_ENABLED,
            true, this);
      button.addMouseListener (myMouseAdapter);
      add (button);
      myButtons.add (new ButtonInfo (mode, button));
      return button;
   }

   private JButton createButton (
      String modeName, String iconFileName, String toolTipText) {

      ImageIcon icon = null;
      if (iconFileName != null) {
         icon = GuiUtils.loadIcon (
            Displayable.class, "icon/"+iconFileName);
      }
      if (icon != null) {
         icon.setDescription (modeName);
      }
      JButton button =
         ButtonCreator.createIconicButton (
            icon, modeName + "mode", toolTipText, ButtonCreator.BUTTON_ENABLED,
            true, this);
      button.addMouseListener (myMouseAdapter);
      add (button);
      return button;
   }

   public SelectionToolbar (Main m) {
      super ("Edit mode selection toolbar");

      myButtons = new ArrayList<>();
      myModeHostMap = new HashMap();
      myMouseAdapter = new LocalMouseAdapter();
      myMain = m;
      JButton selectButton = createButton (
         SelectionMode.Select, "ToolSelectLarge.png",
         "select components");
      createButton (
         SelectionMode.EllipticSelect, "ToolEllipticSelectLarge.png",
         "elliptical component selection");
      createButton (
         SelectionMode.Translate, "ToolMoveLarge.png",
         "select and translate components");
      createButton (
         SelectionMode.Rotate, "ToolRotateLarge.png",
         "select and rotate components");
      createButton (
         SelectionMode.Transrotate, "ToolTransrotateLarge.png",
         "select and translate/rotate components");
      createButton (
         SelectionMode.ConstrainedTranslate, "ToolConstrainedMove.png",
         "select and translate mesh constrained components");
      createButton (
         SelectionMode.Scale, "ToolScaleLarge.png",
         "select and scale components");
      createButton(
         SelectionMode.AddMarker, "ToolAddMarkerLarge.png", 
         "add marker to component");
      createButton (
         SelectionMode.Pull, "ToolPullLarge.png",
         "select and pull components");
      createButton (
         SelectionMode.Measure, "ToolMeasureLarge.png",
         "measure distances");
      // articulatedTransformButton = createButton (
      //    "articulatedTransform", "ArticulatedTransformsEnabled.png",
      //    "enable ");
      // articulatedTransformsEnabledIcon =
      //    GuiUtils.loadIcon (iconPath + "ArticulatedTransformsEnabled.png");
      // articulatedTransformsDisabledIcon =
      //    GuiUtils.loadIcon (iconPath + "ArticulatedTransformsDisabled.png");
      thinBevelBorder = new ThinBevelBorder (BevelBorder.LOWERED);
      bevelBorder = new BevelBorder (BevelBorder.LOWERED);
      border = selectButton.getBorder();
      background = selectButton.getBackground();
      //setArticulatedTransformButton (main.getArticulatedTransformsEnabled());
   }

   // /**
   // * Check if the add marker button is currently selected.
   // *
   // */
   // public boolean addingMarker()
   // {
   // return addMarkerButton.isSelected();
   // }

   // void setArticulatedTransformButton (boolean enabled) {
   //    JButton button = articulatedTransformButton;
   //    if (enabled) {
   //       button.setSelected (true);
   //       button.setIcon (articulatedTransformsEnabledIcon);
   //       button.setBorder (bevelBorder);
   //       button.setBackground (Color.LIGHT_GRAY);
   //    }
   //    else {
   //       button.setSelected (false);
   //       button.setIcon (articulatedTransformsDisabledIcon);
   //       button.setBorder (border);
   //       button.setBackground (background);
   //    }
   // }

   public void setModePropertyHost (
      SelectionMode mode, HasProperties host) {

      ButtonInfo binfo = getButtonInfo (mode);
      binfo.myPropHost = host;
      binfo.myPropDialog = null;
   }

   public void actionPerformed (ActionEvent e) {
      // set the selection mode

      if (e.getSource() instanceof JButton) {
         JButton button = (JButton)e.getSource();
         ButtonInfo binfo = getButtonInfo (button);
         if (binfo == null) {
            throw new UnsupportedOperationException (
               "Unknown button " + button.getText());
         }
         myMain.setSelectionMode (binfo.myMode);
      }
      else if (e.getSource() instanceof JMenuItem) {
         String cmd = e.getActionCommand();
         if (cmd.equals ("Edit properties ...")) {
            if (myLastSelectedButton != null) {
               myLastSelectedButton.showPropertyDialog();
            }
         }
      }
   }

   private ButtonInfo getButtonInfo (SelectionMode mode) {
      for (ButtonInfo binfo : myButtons) {
         if (binfo.myMode == mode) {
            return binfo;
         }
      }
      throw new InternalErrorException ("unimplemented mode " + mode);
   }      

   private ButtonInfo getButtonInfo (JButton button) {
      for (ButtonInfo binfo : myButtons) {
         if (binfo.myButton == button) {
            return binfo;
         }
      }
      return null;
   }      

   void setSelectionButtons (SelectionMode mode) {
      JButton selectedButton = getButtonInfo (mode).myButton;

      // set the decoration on the selected button
      selectedButton.setBorder (bevelBorder);
      selectedButton.setBackground (Color.LIGHT_GRAY);

      // set the decoration on the unselected buttons
      for (ButtonInfo binfo : myButtons) {
         JButton button = binfo.myButton;
         if (button != selectedButton) {
            button.setBorder (border);
            button.setBackground (background);
         }
      }
   }
}
