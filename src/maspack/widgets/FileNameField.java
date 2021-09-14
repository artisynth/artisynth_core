/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.event.*;
import javax.swing.*;
import java.io.*;

import maspack.util.StringHolder;
import maspack.util.FileSearchPath;
import maspack.properties.Property;

public class FileNameField extends StringField {
   private static final long serialVersionUID = 1L;
   JFileChooser myFileChooser = new JFileChooser();
   JButton myBrowseButton;
   boolean myFileMustBeReadable = false;
   boolean myFileMustExist = false;
   FileSearchPath mySearchPath = null;

   /**
    * Creates a FileNameField with specified label text and an empty file name.
    * 
    * @param labelText
    * text for the control label
    * @param ncols
    * approximate width of the text field in columns
    */

   public FileNameField (String labelText, int ncols) {
      super (labelText, ncols);
      addBrowseButton();
      setVoidValueEnabled (true);
      setValue (Property.VoidValue);
   }

   /**
    * Creates a FileNameField with specified label text and initial value.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial value for the file name
    * @param ncols
    * approximate width of the text field in columns
    */
   public FileNameField (String labelText, String initialValue, int ncols) {
      super (labelText, ncols);
      addBrowseButton();
      setVoidValueEnabled (true);
      setValue (initialValue);
   }

   /**
    * Creates a FileNameField with specified label text and initial value.
    * 
    * @param labelText
    * text for the control label
    * @param file
    * initial file value
    * @param ncols
    * approximate width of the text field in columns
    */
   public FileNameField (String labelText, File file, int ncols) {
      super (labelText, ncols);
      addBrowseButton();
      setVoidValueEnabled (true);
      setValue (file.getAbsolutePath());
      myFileChooser.setSelectedFile (file);
   }

   public FileSearchPath getSearchPath() {
      return mySearchPath;
   }

   /**
    * Sets a search path to use for finding files that don't have absolute path
    * names.
    */
   public void setSearchPath (FileSearchPath path) {
      mySearchPath = path;
   }
   
   private void addBrowseButton() {
      setStretchable (true);
      myBrowseButton = new JButton ("Browse");
      myBrowseButton.addActionListener (new ActionListener() {
         public void actionPerformed (ActionEvent e) {
            // special case when file == '.'
            if (getStringValue().equals(".")) {
               myFileChooser.setSelectedFile (
                  new File(new File(".").getAbsolutePath()).getParentFile());
            }
            int returnVal = myFileChooser.showOpenDialog (FileNameField.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
               String path =
                  myFileChooser.getSelectedFile().getAbsolutePath();
               try {
                  setValue (path);
               }
               catch (IllegalValueException exc) {
                  GuiUtils.showError (FileNameField.this, exc.getMessage());
               }
               catch (IllegalArgumentException exc) {
                  GuiUtils.showError (FileNameField.this, exc.getMessage());
               }
            }
         }
      });
      addMajorComponent (myBrowseButton);
   }

   protected void updateChooser() {
      if (myFileChooser != null) { // might not be allocated yet
         if (myValue instanceof String) {
            myFileChooser.setSelectedFile (new File ((String)myValue));
         }
      }
   }

   public boolean getFileMustBeReadable() {
      return myFileMustBeReadable;
   }
   
   public void setFileMustBeReadable (boolean enable) {
      myFileMustBeReadable = enable;
   }
   
   public boolean getFileMustExist() {
      return myFileMustExist;
   }

   public void setFileMustExist (boolean enable) {
      myFileMustExist = enable;
   }
   
   public Object validateValue (Object value, StringHolder errMsg) {
      value = validateBasic (value, String.class, errMsg);
      if (value == Property.IllegalValue) {
         return value;
      }
      if (value instanceof String) {
         String path = (String)value;
         if (!path.equals ("")) {
            if (myFileMustBeReadable || myFileMustExist) {
               File file = new File(path);
               if (mySearchPath != null && !file.isAbsolute()) {
                  File xfile = mySearchPath.findFile (file);
                  if (file != xfile) {
                     file = xfile;
                     value = xfile.getAbsolutePath();
                  }
               }
               if (file == null || !file.exists()) {
                  return illegalValue ("file "+path+" does not exist", errMsg);
               }
               if (myFileMustBeReadable) {
                  if (!file.canRead()) {
                     return illegalValue ("file "+file+" not readable", errMsg);
                  }
               }
            }
            else {
               // check that the parent folder of the file exists
               File dir =
                  new File(new File(path).getAbsolutePath()).getParentFile();
               if (!dir.isDirectory()) {
                  return illegalValue (
                     "directory " + dir  + " does not exist", errMsg);
               }
            }
         }
      }
      return validValue (value, errMsg);
   }

   protected void updateDisplay() {
      super.updateDisplay();
      updateChooser();
   }

   /**
    * Returns the JFileChooser associated with this control.
    * 
    * @return file chooser for this control
    */

   public JFileChooser getFileChooser() {
      return myFileChooser;
   }


   public File getChooserDirectory() {
      return myFileChooser.getCurrentDirectory();
   }

   public void setChooserDirectory (File file) {
      if (file.isDirectory()) { //paranoid
         myFileChooser.setCurrentDirectory(file);
      }
   }

   /**
    * Returns the JButton used to initiate file browsing on this control.
    * 
    * @return file browsing button for this control
    */
   public JButton getBrowseButton() {
      return myBrowseButton;
   }

   /**
    * Returns the file associated with this widget's value, or {@code null}
    * if no file is selected.
    * 
    * @return file associated with widget value
    */
   public File getFile() {
      String fileName = getStringValue();
      if (fileName == null || fileName.equals("")) {
         return null;
      }
      else {
         return new File (fileName);
      }
   }
   
   /**
    * Sets the file associated with this widget's value. The value is set from
    * the file's absolute path.
    * 
    * @param file associated with widget value
    */
   public void setFile (File file) {
      if (file == null) {
         setValue ("");
      }
      else {
         setValue (file.getAbsolutePath());
      }
   }

   @Override 
   public void setValue (Object value) {
      super.setValue (value);
   }
   
}
