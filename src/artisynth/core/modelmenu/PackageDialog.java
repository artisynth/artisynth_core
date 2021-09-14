package artisynth.core.modelmenu;

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import artisynth.core.driver.RootModelManager;

import maspack.graph.*;
import maspack.util.*;
import maspack.widgets.*;
import maspack.properties.*;

public class PackageDialog extends WidgetDialog {
   
   AutoCompleteStringField myPackageField;
   StringField myTitleField;
   boolean myTitleExplicitlySet = false;

   private String getTailName (String packname) {
      int lastDot = packname.lastIndexOf ('.');
      if (lastDot != -1) {
         return packname.substring (lastDot+1);
      }
      else {
         return packname;
      }
   }
   
   String getPackageName() {
      return myPackageField.getStringValue();
   }

   /**
    * To add, package field must not be empty
    */
   private boolean readyToAdd() {
      return !myPackageField.valueIsEmpty();
   }   

   static class PackageValidator implements ValueCheckListener {
      public Object validateValue (
         ValueChangeEvent e, StringHolder errMsg) {
         String str = (String)e.getValue();
         // null string is OK
         if (str.length() == 0) {
            return str;
         }
         if (!ClassFinder.packageExists(str)) {
            if (errMsg != null) {
               errMsg.value = ""+str+" is not a known package";
            }
            return Property.IllegalValue;
         }
         else {
            return str;
         }
      }
   }

   private void maybeSetTitleFromPackage () {
      if (!myPackageField.valueIsEmpty()) {
         if (!myTitleExplicitlySet) {
            myTitleField.setValue (getDefaultTitle());
            // need to reset myTitleExplicitlySet since text handler will set it
            // to true
            myTitleExplicitlySet = false; 
         }
      }
   }

   private String getDefaultTitle () {
      return getTailName(myPackageField.getStringValue());
   }

   public PackageDialog (
      Frame frame, PackageEntry packageEntry, String packageName,
      String title, String confirmCmd, RootModelManager rmm) {

      super (frame, title, confirmCmd);

      boolean editing = confirmCmd.equals ("Done");

      myPackageField =
         new AutoCompleteStringField (
            "Package: ", "", 28, rmm.getKnownPackageNames());
      myPackageField.setStretchable (true);
      myPackageField.setToolTipText (
         "Name of the package containing the model classes");
      if (editing) {
         myPackageField.setValue (packageEntry.getPackageName());
         myTitleExplicitlySet =
            !getDefaultTitle().equals(packageEntry.getTitle());
      }
      else {
         myPackageField.setValue (packageName);
      }
      myPackageField.setEnterValueOnFocusLost (false);
      myPackageField.addValueCheckListener (new PackageValidator());
      addWidget (myPackageField);

      myTitleField =
         (StringField)addWidget ("Menu title: ", packageEntry, "title");
      if (!editing) {
         maybeSetTitleFromPackage();
      }

      addWidget ("Flat view: ", packageEntry, "flatView");
      if (editing) {
         ModelScriptMenuEditor.addMenuWidgets (this, packageEntry);
      }
      ModelScriptMenuEditor.addEnableOnNonNullText (this, myPackageField, confirmCmd);

      // disable confirm button and set up listeners to enable it only when the
      // package and title fields are both not empty.

      OptionPanel options = getOptionPanel();
      JButton confirmButton = options.getButton (confirmCmd);
      confirmButton.setEnabled (readyToAdd());

      myTitleField.addValueChangeListener (
         new ValueChangeListener () {
            public void valueChange (ValueChangeEvent e) {
               String text = (String)e.getValue();
               myTitleExplicitlySet = (text != null && text.length() > 0);
               confirmButton.setEnabled (readyToAdd());
            }
         });
      myPackageField.addValueChangeListener (
         new ValueChangeListener () {
            public void valueChange (ValueChangeEvent e) {
               maybeSetTitleFromPackage();
               confirmButton.setEnabled (readyToAdd());
            }
         });
      pack();
   }
}


