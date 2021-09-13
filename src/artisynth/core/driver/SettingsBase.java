package artisynth.core.driver;

import maspack.properties.*;
import maspack.widgets.*;

/**
 * Base class for an object that has settings.
 */
public abstract class SettingsBase implements HasProperties {

   Preferences myPrefs;
   SettingsDialog myDialog;

   /**
    * {@inheritDoc}
    */
   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   /**
    * Returns the preferences (if any) associated with these settings.
    */
   public Preferences getPreferences() {
      return myPrefs;
   }

   /**
    * Sets preferences to be associated with these settings
    */
   public void setPreferences (Preferences prefs) {
      myPrefs = prefs;
   }

   /**
    * Returns the settings dialog, or {@code null} if one has not been created.
    */
   public SettingsDialog getDialog() {
      return myDialog;
   }

   /**
    * Creates and returns settings dialog.
    */
   public SettingsDialog createDialog (
      String name, PreferencesManager prefManager) {

      myDialog =  new SettingsDialog (name, this, prefManager);
      return myDialog;
   }

}

