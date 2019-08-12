/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import maspack.util.InternalErrorException;
import maspack.widgets.ButtonCreator;
import maspack.widgets.GuiUtils;
import maspack.widgets.ThinBevelBorder;
import artisynth.core.gui.Displayable;
import artisynth.core.driver.Main.SelectionMode;


public class SelectionToolbar extends JToolBar implements ActionListener {
   static String iconPath = "/artisynth/core/gui/icon/";

   private JButton selectButton;
   private JButton ellipticSelectButton;
   private JButton scaleButton;
   private JButton translateButton;
   private JButton transrotateButton;
   private JButton rotateButton;
   private JButton constrainedTranslateButton;
   private JButton pullButton;
   // private JButton articulatedTransformButton;
   // private ImageIcon articulatedTransformsEnabledIcon;
   // private ImageIcon articulatedTransformsDisabledIcon;
   private JButton addMarkerButton;
   private Main main;
   private ThinBevelBorder thinBevelBorder;
   private BevelBorder bevelBorder;
   private Border border;
   private Color background;

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
      add (button);
      return button;
   }

   public SelectionToolbar (Main m) {
      super ("Edit mode selection toolbar");

      main = m;
      selectButton = createButton (
         "select", "ToolSelectLarge.png",
         "select components");
      ellipticSelectButton = createButton (
         "ellipticSelect", "ToolEllipticSelectLarge.png",
         "elliptical component selection");
      translateButton = createButton (
         "translate", "ToolMoveLarge.png",
         "select and translate components");
      rotateButton = createButton (
         "rotate", "ToolRotateLarge.png",
         "select and rotate components");
      transrotateButton = createButton (
         "transrotate", "ToolTransrotateLarge.png",
         "select and translate/rotate components");
      constrainedTranslateButton = createButton (
         "constrainedTranslate", "ToolConstrainedMove.png",
         "select and translate mesh constrained components");
      scaleButton = createButton (
         "scale", "ToolScaleLarge.png",
         "select and scale components");
      addMarkerButton = createButton(
         "addMarker", "ToolAddMarkerLarge.png", 
         "add marker to component");
      pullButton = createButton (
         "pull", "ToolPullLarge.png",
         "select and pull components");
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


   public void actionPerformed (ActionEvent e) {
      // set the newly selected item
      if (e.getSource() == selectButton)
         main.setSelectionMode (Main.SelectionMode.Select);
      else if (e.getSource() == ellipticSelectButton)
         main.setSelectionMode (Main.SelectionMode.EllipticSelect);
      else if (e.getSource() == scaleButton)
         main.setSelectionMode (Main.SelectionMode.Scale);
      else if (e.getSource() == translateButton)
         main.setSelectionMode (Main.SelectionMode.Translate);
      else if (e.getSource() == transrotateButton)
         main.setSelectionMode (Main.SelectionMode.Transrotate);
      else if (e.getSource() == rotateButton)
         main.setSelectionMode (Main.SelectionMode.Rotate);
      else if (e.getSource() == constrainedTranslateButton)
         main.setSelectionMode (Main.SelectionMode.ConstrainedTranslate);
      else if (e.getSource () == addMarkerButton)
         main.setSelectionMode (Main.SelectionMode.AddMarker);
      else if (e.getSource() == pullButton)
         main.setSelectionMode (Main.SelectionMode.Pull);
   }

   public void setSelectionButtons() {
      // create an array of all the selection buttons
      ArrayList<JButton> selectionButtons = new ArrayList<JButton>();

      selectionButtons.add (selectButton);
      selectionButtons.add (ellipticSelectButton);
      selectionButtons.add (scaleButton);
      selectionButtons.add (transrotateButton);
      selectionButtons.add (translateButton);
      selectionButtons.add (rotateButton);
      selectionButtons.add (constrainedTranslateButton);
      selectionButtons.add (addMarkerButton);
      selectionButtons.add (pullButton);
      //selectionButtons.add (articulatedTransformButton);

      // get the button that is currently selected
      SelectionMode mode = main.getSelectionMode();
      JButton selectedButton = null;

      switch (mode) {
         case Select: {
            selectedButton = selectButton;
            break;
         }
         case EllipticSelect: {
            selectedButton = ellipticSelectButton;
            break;
         }
         case Scale: {
            selectedButton = scaleButton;
            break;
         }
         case Translate: {
            selectedButton = translateButton;
            break;
         }
         case Transrotate: {
            selectedButton = transrotateButton;
            break;
         }
         case Rotate: {
            selectedButton = rotateButton;
            break;
         }
         case ConstrainedTranslate: {
            selectedButton = constrainedTranslateButton;
            break;
         }
         case AddMarker: {
            selectedButton = addMarkerButton;
            break;
         }
         case Pull: {
            selectedButton = pullButton;
            break;
         }
         default: {
            throw new InternalErrorException ("unimplemented mode " + mode);
         }
      }

      // remove the button that is currently selected from the array list
      selectionButtons.remove (selectedButton);

      // set the decoration on the selected button
      selectedButton.setBorder (bevelBorder);
      selectedButton.setBackground (Color.LIGHT_GRAY);

      // set the decoration on the unselected buttons
      for (JButton b : selectionButtons) {
         b.setBorder (border);
         b.setBackground (background);
      }
   }
}
