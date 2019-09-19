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
import maspack.properties.Property;

public class FileNameField extends StringField {
   private static final long serialVersionUID = 1L;
   JFileChooser myFileChooser = new JFileChooser();
   JButton myBrowseButton;

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

   private void addBrowseButton() {
      setStretchable (true);
      myBrowseButton = new JButton ("Browse");
      myBrowseButton.addActionListener (new ActionListener() {
         public void actionPerformed (ActionEvent e) {
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
            }
         }
      });
      addMajorComponent (myBrowseButton);
   }

   private boolean validParentPath (String path) {
      File file = new File (path);
      return (file.getParentFile() == null || file.getParentFile().exists());
   }

   protected void updateChooser() {
      if (myFileChooser != null) // might not be allocated yet
      {
         if (myValue instanceof String) {
            myFileChooser.setSelectedFile (new File ((String)myValue));
         }
      }
   }

   public Object validateValue (Object value, StringHolder errMsg) {
      value = validateBasic (value, String.class, errMsg);
      if (value == Property.IllegalValue) {
         return value;
      }
      if (value instanceof String) {
         String path = (String)value;
         if (!path.equals ("")) {
            if (!validParentPath (path)) {
               return illegalValue ("directory "
               + new File (path).getParentFile().getAbsolutePath()
               + " does not exist", errMsg);
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

   /**
    * Returns the JButton used to initiate file browsing on this control.
    * 
    * @return file browsing button for this control
    */

   public JButton getBrowseButton() {
      return myBrowseButton;
   }
}
