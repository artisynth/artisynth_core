package artisynth.core.driver;

import java.awt.Color;
import javax.swing.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.widgets.*;
import maspack.widgets.GuiUtils.RelativeLocation;
import maspack.properties.*;
import maspack.render.*;
import maspack.render.GL.GLViewer.*;
import maspack.render.GL.GLViewer;

import java.io.IOException;
import java.io.PrintWriter;

import artisynth.core.util.*;
import artisynth.core.util.*;
import artisynth.core.gui.*;
import artisynth.core.gui.timeline.*;
import artisynth.core.gui.jythonconsole.*;

/**
 * Preferences related to mouse settings. Note: this set of preferences is not
 * implemented using properties.
 */
public class MousePrefs extends Preferences implements ValueChangeListener {

   private Main myMain;

   private MouseBindings myBindings;
   private double myWheelZoomScale;

   private MouseBindings mySavedBindings;
   private double mySavedWheelZoomScale;

   static PropertyList myProps = new PropertyList (MousePrefs.class);
   static {
      // no properties at the moment
   }

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public MousePrefs (Main main) {
      myMain = main;
      myBindings = main.getMouseBindings();
      myWheelZoomScale = ViewerManager.DEFAULT_MOUSE_WHEEL_ZOOM_SCALE;
   }

   public MouseBindings getBindings () {
      return myBindings;
   }

   public void setBindings (MouseBindings bindings) {
      myBindings = bindings;
   }

   public double getWheelZoomScale () {
      return myWheelZoomScale;
   }

   public void setWheelZoomScale (double s) {
      myWheelZoomScale = s;
   }

   public void setFromCurrent() {
      myBindings = myMain.getMouseBindings();
      myWheelZoomScale = myMain.getMouseWheelZoomScale();
   }

   public void applyToCurrent() {
      myMain.setMouseBindings (myBindings);
      myMain.setMouseWheelZoomScale (myWheelZoomScale);
   }

   protected PropertyPanel createEditingPanel() {
      MouseSettingsPanel panel = new MouseSettingsPanel (
         myBindings, myMain.getAllMouseBindings(),
         myWheelZoomScale);
      addLoadApplyButtons (panel);

      panel.getBindingsField().addValueChangeListener (this);
      panel.getAutoDetectField().addValueChangeListener (this);
      panel.getWheelZoomField().addValueChangeListener (this);
      
      mySavedBindings = myBindings;
      mySavedWheelZoomScale = myWheelZoomScale;
      return panel;
   }

   @Override
   protected void updateEditingPanelWidgets () {
      if (myEditingPanel != null) {
         MouseSettingsPanel panel = (MouseSettingsPanel)myEditingPanel;
         panel.setBindings (myBindings);
         panel.setWheelZoom (myWheelZoomScale);
      }
   }


   public void setDefaults() {
      myBindings = MouseBindings.Default;
      myWheelZoomScale = ViewerManager.DEFAULT_MOUSE_WHEEL_ZOOM_SCALE;      
   }

   void reloadEditPanelProps() {
      if (myEditingPanel != null) {
         mySavedBindings = myBindings;
         mySavedWheelZoomScale = myWheelZoomScale;
         updateEditingPanelWidgets();  
      }
   }

   void restoreEditPanelProps() {
      if (myEditingPanel != null) {
         myBindings = mySavedBindings;
         myWheelZoomScale = mySavedWheelZoomScale;
      }
   }

   public void writeItems (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      pw.println ("bindings=" + myBindings.getName());
      pw.println ("wheelZoomScale=" + fmt.format(myWheelZoomScale));
   }

   protected boolean scanItem (ReaderTokenizer rtok) 
      throws IOException {
      rtok.nextToken();
      if (rtok.tokenIsWord ("bindings")) {
         rtok.scanToken ('=');
         String name = rtok.scanWord();
         if (name.equals ("Default")) {
            myBindings = MouseBindings.Default;
         }
         else {
            for (MouseBindings b : myMain.getAllMouseBindings()) {
               if (b.getName().equals (name)) {
                  myBindings = b;
                  break;
               }
            }
         }
         if (myBindings == null) {
            System.out.println (
               "WARNING: unknown mouse binding '"+name+"' in preferences file");
            myBindings = MouseBindings.Default;
         }
         return true;
      }
      else if (rtok.tokenIsWord ("wheelZoomScale")) {
         rtok.scanToken ('=');
         myWheelZoomScale = rtok.scanNumber();
         return true;
      }
      else {
         return false;
      }
   }

   public void valueChange (ValueChangeEvent e) {
      MouseSettingsPanel panel = (MouseSettingsPanel)myEditingPanel;      
      myBindings = panel.getBindings();
      myWheelZoomScale = panel.getWheelZoom();
   }
}

/*

Mouse bindings set when we add a viewer, 

Main.setMouseBindings called from

   Main.setMouseBindings (name) - called from command line
   MenuBarHandler.valueChange() - when mouse dialog changes value
   MousePrefs.applyToCurrent()

   If we don't init from command line, bindings will be set
   first time we call getMouseBindings(), which happens in:

      open mouse settings dialog
      MousePrefs.setFromCurrent()
      MousePrefs()
      adding a viewer
 */
