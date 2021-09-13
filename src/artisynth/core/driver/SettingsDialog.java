package artisynth.core.driver;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import maspack.widgets.*;
import maspack.properties.*;

/**
 * Dialog used to set operational settings.
 */
public class SettingsDialog extends PropertyDialog {

   PreferencesManager myPrefsManager;
   Preferences myPreferences;
   boolean mySaveOccurred = false;

   public SettingsDialog (
      String name, PropertyPanel panel,
      Preferences prefs, PreferencesManager prefsManager) {
      super (name, panel, null);

      myPreferences = prefs;
      myPrefsManager = prefsManager;
      initOptionPanel();
   }

   public SettingsDialog (
      String name, SettingsBase settings, PreferencesManager prefsManager) {
      super (name, new HostList (new HasProperties[] {settings}), null);

      if (settings.getPreferences() != null) {
          myPreferences = settings.getPreferences();
      }
      myPrefsManager = prefsManager;
      initOptionPanel();
   }

   protected void initOptionPanel() {

      JSeparator sep = new JSeparator();
      sep.setAlignmentX (Component.CENTER_ALIGNMENT);
      getContentPane().add (GuiUtils.createBoxFiller());
      getContentPane().add (sep);

      JPanel optionPanel = new JPanel();
      optionPanel.setBorder (BorderFactory.createEmptyBorder (5, 5, 5, 5));
      optionPanel.setLayout (new BoxLayout(optionPanel, BoxLayout.X_AXIS));

      if (myPreferences != null) {
         GuiUtils.addHorizontalButton (
            optionPanel, "Save", this, "Save settings in startup preferences");
      }
      
      optionPanel.add (Box.createHorizontalGlue());
      GuiUtils.addHorizontalButton (
         optionPanel, "Cancel", this, "Close dialog without make changes");
      optionPanel.add (Box.createRigidArea (new Dimension (10, 10)));
      GuiUtils.addHorizontalButton (
         optionPanel, "Done", this, "Close dialog");

      getContentPane().add (optionPanel);
      pack();
   }

   @Override
    public void restoreValues() {
      super.restoreValues();
      undoPreferenceSave();
   }  

   protected void undoPreferenceSave() {
      if (mySaveOccurred) {
         // resave preferences
         myPreferences.setFromCurrent();
         myPrefsManager.save();
      }
   }
   
   public void actionPerformed (ActionEvent e) {
      String actionCmd = e.getActionCommand();
      if (actionCmd.equals ("Save")) {
         myPreferences.setFromCurrent();
         myPrefsManager.save();
         mySaveOccurred = true;
      }
      else {
         super.actionPerformed (e);
      }
   }

   public void reloadSettings() {
      myHostList.saveBackupValues (myTree);
      updateWidgetValues();
      mySaveOccurred = false;
   }         

}
