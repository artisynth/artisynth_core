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
import javax.swing.border.*;

import java.util.*;

import maspack.util.*;
import maspack.render.*;
import maspack.render.DrawToolBase.FrameBinding;
import maspack.render.GL.GLViewer;

public class DraggerToolBar extends JToolBar implements ActionListener {

   public enum ButtonType {
      Select (
         "ToolSelectLarge.png", "select components", true),
      Translate (
         "ToolMoveLarge.png", "translate components", true),
      Rotate (
         "ToolRotateLarge.png", "rotate components", true),
      TransRotate (
         "ToolTransrotateLarge.png", "translate and rotate components", true),
      Scale (
         "ToolScaleLarge.png", "scale components", true),
      Draw (
         "ToolPencilLarge.png", "draw a curve freehand", false),
      Spline (
         "ToolSplineLarge.png", "draw a NURBS curve", false),
      AddPoint (
         "ToolAddMarkerLarge.png", "add a point", false);

      String myFileName;
      String myDescription;
      boolean myUsesSelection;

      ButtonType (String fileName, String description, boolean usesSelection) {
         myFileName = fileName;
         myDescription = description;
         myUsesSelection = usesSelection;
      }
   }

   class ButtonDesc {
      JButton myButton;
      ButtonType myType;
      boolean myExclusive;
      boolean mySelected;
      boolean myUsesSelection;

      ButtonDesc (JButton button, ButtonType type) {
         myButton = button;
         myType = type;
         myExclusive = true;
         if (type != null) {
            myUsesSelection = type.myUsesSelection;
         }
      }

      boolean isButtonExclusive() {
         return myExclusive;
      }

      boolean usesSelection() {
         return myUsesSelection;
      }

      void setSelected (boolean selected) {
         if (selected != mySelected) {
            if (selected) {
               myButton.setBackground (Color.LIGHT_GRAY);
               myButton.setBorder (myBevelBorder);
            }
            else {
               myButton.setBackground (myDefaultColor);
               myButton.setBorder (myDefaultBorder);
            }
            mySelected = selected;
         }
      }   
      
      boolean isSelected() {
         return mySelected;
      }
   }

   ArrayList<ButtonDesc> myButtons;
   GLViewer myViewer;
   Dragger3dBase myDragger;
   Dragger3dListener myDraggerListener;
   DrawToolBase myDrawTool;
   DrawToolListener myDrawToolListener;
   protected BevelBorder myBevelBorder;
   protected Border myDefaultBorder;
   protected Color myDefaultColor;
   protected FrameBinding myDrawToolFrameBinding = FrameBinding.VIEW_PLANE;
   protected double myDrawToolFrameOffset = 0;
   protected int mySplineMaxDegree = 3;

   public void setDrawToolFrameBinding (FrameBinding binding) {
      myDrawToolFrameBinding = binding;
      if (myDrawTool != null) {
         myDrawTool.setFrameBinding (binding);
      }
   }

   public FrameBinding getDrawToolFrameBinding() {
      return myDrawToolFrameBinding;
   }

   public void setDrawToolFrameOffset (double offset) {
      myDrawToolFrameOffset = offset;
      if (myDrawTool != null) {
         myDrawTool.setFrameOffset (offset);
      }
   }

   public double getDrawToolFrameOffset() {
      return myDrawToolFrameOffset;
   }

   public void setSplineToolMaxDegree (int maxd) {
      mySplineMaxDegree = maxd;
      if (myDrawTool instanceof SplineTool) {
         ((SplineTool)myDrawTool).setMaxDegree (maxd);
      }
   }

   public int getSplineToolMaxDegree() {
      return mySplineMaxDegree;
   }

   public DraggerToolBar (
      GLViewer viewer, Dragger3dListener draggerListener,
      ButtonType... buttonTypes) {

      super ("Dragger tool bar");

      myViewer = viewer;
      myDraggerListener = draggerListener;
      myButtons = new ArrayList<ButtonDesc>();

      myBevelBorder = new BevelBorder (BevelBorder.LOWERED);

      // create a select button just to get the default color and border
      
      JButton button = createButton (ButtonType.Select);
      myDefaultBorder = button.getBorder();
      myDefaultColor = button.getBackground();
      for (int i=0; i<buttonTypes.length; i++) {
         addButton (buttonTypes[i]);
      }
      updateWidgets();
   }

   public void setDraggerListener (Dragger3dListener l) {
      if (l != myDraggerListener) {
         if (myDragger != null) {
            if (myDraggerListener != null) {
               myDragger.removeListener (myDraggerListener);
            }
            if (l != null) {
               myDragger.addListener (l);
            }
         }
         myDraggerListener = l;
      }
   }

   public void setDrawToolListener (DrawToolListener l) {
      if (l != myDrawToolListener) {
         if (myDrawTool != null) {
            if (myDrawToolListener != null) {
               myDrawTool.removeListener (myDrawToolListener);
            }
            if (l != null) {
               myDrawTool.addListener (l);
            }
         }
         myDrawToolListener = l;
      }
   }

   private JButton createButton (ButtonType type) {
      ImageIcon icon = GuiUtils.loadIcon (
         DraggerToolBar.class, "icons/" + type.myFileName);

      JButton button =
         ButtonCreator.createIconicButton (
            icon, type.toString(), type.myDescription,
            ButtonCreator.BUTTON_ENABLED, true, this);
      return button;
   }

   public void addButton (ButtonType type) {
      ButtonDesc desc = new ButtonDesc (createButton (type), type);
      myButtons.add (desc);
      add (desc.myButton);
   }   

   public void updateWidgets() {
      // For simplicity, assume that myCurrentDragger will not be added to
      // or removed from the viewer independently.

      // Make sure current button state is consistent with viewer selection
      // being enabled
      ButtonDesc currentSelected = getCurrentSelectedButton();
      if (myViewer.isSelectionEnabled()) {
         if (currentSelected == null) {
            ButtonDesc buttonDesc = null;
            for (ButtonDesc desc : myButtons) {
               if (desc.isButtonExclusive()) {
                  buttonDesc = desc;
                  break;
               }
            }
            if (buttonDesc != null) {
               applyButton (buttonDesc);
            }
         }
      }
      else {
         if (currentSelected != null) {
            if (myDragger != null) {
               removeDragger();
            }
            if (myDrawTool != null) {
               removeDrawTool();
            }
            currentSelected.setSelected (false);
         }
      }
   }

   protected void addDragger (Dragger3dBase dragger) {
      myViewer.setSelectionEnabled (true);
      if (myDraggerListener != null) {
         dragger.addListener (myDraggerListener);
      }     
      myViewer.addDragger (dragger);
      myDragger = dragger;
      myViewer.rerender();
   }

   protected void removeDragger () {
      if (myDragger != null) {
         myViewer.removeDragger (myDragger);
         if (myDraggerListener != null) {
            myDragger.removeListener (myDraggerListener);
         }
         myDragger = null;
         myViewer.rerender();
      }
   }
   
   public Dragger3dBase getDragger() {
      return myDragger;
   }

   protected void addDrawTool (DrawToolBase tool) {
      myDrawTool = tool;
      if (myDrawToolListener != null) {
         myDrawTool.addListener (myDrawToolListener);
      }      
      myViewer.setSelectionEnabled (true);
      myViewer.setDrawTool (tool);
      myViewer.rerender();
   }

   protected void removeDrawTool () {
      if (myDrawTool != null) {
         myViewer.setDrawTool (null);
         if (myDrawToolListener != null) {
            myDrawTool.removeListener (myDrawToolListener);
         }
         myDrawTool = null;
         myViewer.rerender();
      }
   }

   protected ButtonDesc getCurrentSelectedButton () {
      for (ButtonDesc desc : myButtons) {
         if (desc.isButtonExclusive() && desc.isSelected()) {
            return desc;
         }
      }
      return null;
   }

   public void actionPerformed (ActionEvent evt) {
      ButtonDesc buttonDesc = null;
      for (ButtonDesc desc : myButtons) {
         if (evt.getSource() == desc.myButton) {
            buttonDesc = desc;
            break;
         }
      }
      applyButton (buttonDesc);
      // maybe call actionPerformed in the application?
   }

   protected void applyButton (ButtonDesc buttonDesc) {   
      if (myDragger != null) {
         removeDragger();
      }
      if (myDrawTool != null) {
         removeDrawTool();
      }
      ButtonDesc currentSelected = getCurrentSelectedButton();
      if (buttonDesc == currentSelected) {
         // then toggle selection
         currentSelected.setSelected (false);
         if (currentSelected.usesSelection()) {
            myViewer.setSelectionEnabled (false);
         }
      }
      else if (buttonDesc.isButtonExclusive()) {
         Dragger3dBase dragger = null;
         DrawToolBase drawTool = null;

         if (currentSelected != null) {
            currentSelected.setSelected (false);
         }
         
         switch (buttonDesc.myType) {
            case Select: {
               dragger = null;
               myViewer.setSelectionEnabled (true);
               break;
            }
            case Translate: {
               dragger = new Translator3d();
               break;
            }
            case Rotate: {
               dragger = new Rotator3d();
               break;               
            }
            case TransRotate: {
               dragger = new Transrotator3d();
               break;
            }
            case Scale: {
               dragger = new RotatableScaler3d();
               break;
            }
            case Draw:{
               drawTool = new FreehandTool();
               break;
            }
            case Spline:{
               SplineTool splineTool = new SplineTool();
               splineTool.setMaxDegree (mySplineMaxDegree);
               drawTool = splineTool;               
               break;
            }
            case AddPoint: {
               drawTool = new PointTool();
               break;
            }
            default: {
               throw new InternalErrorException (
                  "Unimplemented button type: " + buttonDesc.myType);
            }
         }
         if (dragger != null) {
            if (dragger instanceof Dragger3dBase) {
               double size =
                  (myViewer.distancePerPixel(myViewer.getCenter())*
                   myViewer.getScreenWidth() / 6);
               ((Dragger3dBase)dragger).setSize (size);
            }
            addDragger (dragger);
         }
         if (drawTool != null) {
            drawTool.setFrameBinding (myDrawToolFrameBinding);
            drawTool.setFrameOffset (myDrawToolFrameOffset);
            addDrawTool (drawTool);
         }
         buttonDesc.setSelected (true);
         myViewer.setSelectionEnabled (buttonDesc.usesSelection());
      }
   }

}
