package artisynth.core.driver;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import maspack.widgets.*;
import maspack.properties.*;

/**
 * Dialog used to set operational settings.
 */
public class MouseSettingsDialog
   extends SettingsDialog implements ValueChangeListener {

   private MouseBindings mySavedBindings;
   private double mySavedWheelZoomScale;
   private Main myMain;
   private MouseSettingsPanel myMousePanel;

   public MouseSettingsDialog (String name, Main main) {
      super (name, 
             new MouseSettingsPanel (
                main.getMouseBindings(), main.getAllMouseBindings(),
                main.getMouseWheelZoomScale()),
             main.myMousePrefs, main.myPreferencesManager);

      myMain = main;
      myMousePanel = (MouseSettingsPanel)myPanel;
      PropertyPanel.addValueChangeListener (
         myMousePanel.getWheelZoomField(), this);
      PropertyPanel.addValueChangeListener (
         myMousePanel.getBindingsField(), this);
      PropertyPanel.addValueChangeListener (
         myMousePanel.getAutoDetectField(), this);
      mySavedBindings = main.getMouseBindings();
      mySavedWheelZoomScale = main.getMouseWheelZoomScale();
   }

   public void reloadSettings() {
      mySavedBindings = myMain.getMouseBindings();
      mySavedWheelZoomScale = myMain.getMouseWheelZoomScale();
      mySaveOccurred = false;
      updateWidgetValues();
   }

   public void updateWidgetValues() {
      myMousePanel.setBindings (myMain.getMouseBindings());
      myMousePanel.setWheelZoom (myMain.getMouseWheelZoomScale());
   }

   @Override
   public void restoreValues() {
      myMain.setMouseBindings (mySavedBindings);
      myMain.setMouseWheelZoomScale (mySavedWheelZoomScale);      
      undoPreferenceSave();
   }

   public MouseBindings getBindings() {
      return myMousePanel.getBindings();
   }

   public double getWheelZoom() {
      return myMousePanel.getWheelZoom();
   }

   public void setBindings (MouseBindings bindings) {
      myMousePanel.setBindings (bindings);
   }

   public void setWheelZoom (double zoom) {
      myMousePanel.setWheelZoom (zoom);
   }

   public void valueChange (ValueChangeEvent evt) {
      myMain.setMouseBindings (getBindings());
      myMain.setMouseWheelZoomScale (getWheelZoom());
   }

}
