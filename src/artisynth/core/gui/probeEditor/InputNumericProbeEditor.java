/**
 * Copyright (c) 2014, by the Authors: Johnty Wang (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.probeEditor;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import artisynth.core.gui.Displayable;
import artisynth.core.gui.Timeline;
import artisynth.core.gui.editorManager.AddComponentsCommand;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericProbeDriver;
import artisynth.core.probes.NumericProbeVariable;
import artisynth.core.probes.Probe;
import maspack.properties.NumericConverter;
import maspack.properties.Property;
import maspack.util.StringHolder;
import maspack.widgets.GuiUtils;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueCheckListener;

public class InputNumericProbeEditor extends NumericProbeEditor {
   static final long serialVersionUID = 1L;
   private NumericInputProbe oldProbe;

   private class InputFileChecker implements ValueCheckListener {
      public Object validateValue (ValueChangeEvent e, StringHolder errMsg) {
         if (!attachedFileValid ((String)e.getValue(), errMsg)) {
            return Property.IllegalValue;
         }
         if (errMsg != null) {
            errMsg.value = null;
         }
         return e.getValue();
      }
   }

   public InputNumericProbeEditor() {
      super (); // call superclass constructor
      setupGUI();
      addBlankInputProbeProperty();
      addPropertyButton.setEnabled (false);
      resizeFrame();
      refreshAddBtn();
   }

   /**
    * Constructor for an Input Probe Editor based on an existing probe. This
    * method first finds all the attached properties, and adds the corresponding
    * GUI elements for each property.
    */
   public InputNumericProbeEditor (NumericInputProbe inProbe) {
      super ();
      oldProbe = inProbe;

      attachedFile = inProbe.getAttachedFile();
      attachedFileField.setValue (Probe.getPathFromFile (attachedFile));
      attachedFileField.addValueCheckListener (new InputFileChecker());

      setupGUI();
      setupProbeProperties (inProbe);
      Property props[] = inProbe.getAttachedProperties();
      for (Property prop : props) {
         if (NumericConverter.isNumeric (prop.get())) {
            String compPath = getFullPropPath (prop);
            System.out.println ("path of comp =" + compPath);
            addBlankInputProbeProperty();
            AddPropertyPane propPane = propList.get (propList.size() - 1); 
            // get the recently added item
            propPane.setCompPath (compPath);
            propPane.setEditable (false);
         }
      }
      // clear the variables that would have been added automatically upon
      // selection of properties
      for (AddVectorPane pane : probeChannels) {
         vectorPane.remove (pane);
      }
      probeChannels.clear();
      myVariables.clear();

      LinkedHashMap<String,NumericProbeVariable> oldVariables =
         inProbe.getVariables();
      // make a deep copy of the variable list
      for (Map.Entry<String,NumericProbeVariable> entry :
              oldVariables.entrySet()) {
         myVariables.put (entry.getKey(),
                          new NumericProbeVariable (entry.getValue()));
         AddVectorPane vecPane = new AddVectorPane (this, entry.getKey());
         vecPane.setDim (entry.getValue().getDimension());
         addVectorGUI (vecPane);

      }
      myDrivers.clear();
      for (NumericProbeDriver driver : inProbe.getDrivers()) {
         myDrivers.add (myDrivers.size(), driver);
         getEqPane (myDrivers.indexOf (driver)).setEqText (
            driver.getExpression());
         getEqPane (myDrivers.indexOf (driver)).updateAppearance();
      }

      resizeFrame();
      refreshAddBtn();

   }

   private void setupGUI() {
      this.setTitle ("Input Probe Editor");
      // middlePane.add(Box.createRigidArea(new Dimension(10,0)));
      middlePane.add (vectorPane);
      middlePane.add (Box.createRigidArea (new Dimension (5, 0)));
      middlePane.add (splitPane);
      // middlePane.add(Box.createRigidArea(new Dimension(10,0)));

      splitPane.add (equationPane);
      splitPane.add (propPane);

      vectorPane.add (Box.createRigidArea (new Dimension (0, 5)));

      // GuiUtils.setFixedSize(addVectorButton, new Dimension(70, 25));
      JPanel btnPane = new JPanel();
      btnPane.setLayout (new BoxLayout (btnPane, BoxLayout.X_AXIS));
      btnPane.add (Box.createRigidArea (new Dimension (8, 0))); // tweakable
                                                                  // value
      btnPane.setMinimumSize (new Dimension (30, 35));
      btnPane.setMaximumSize (new Dimension (Integer.MAX_VALUE, 35));
      btnPane.add (addVectorButton);
      btnPane.setAlignmentX (LEFT_ALIGNMENT);
      vectorPane.add (btnPane, 1);
      propPane.add (Box.createRigidArea (new Dimension (0, 5)));
      JPanel btnPane2 = new JPanel();
      btnPane2.setLayout (new BoxLayout (btnPane2, BoxLayout.X_AXIS));
      btnPane2.add (Box.createRigidArea (new Dimension (9, 0))); // tweakable
                                                                  // value
      btnPane2.add (addPropertyButton);
      btnPane2.setMinimumSize (new Dimension (30, 35));
      btnPane2.setMaximumSize (new Dimension (Integer.MAX_VALUE, 35));
      btnPane2.setAlignmentX (LEFT_ALIGNMENT);
      equationPane.add (btnPane2, 1);
      rightPropertyPanel.addWidget (scaleField);
      rightPropertyPanel.addWidget (attachedFileField);
   }

   // //
   // // Called when a user adds an input property
   // //
   // public void addInputProbeProperty (int index, Property prop)
   // {
   // myProperties.set (index, prop);
   //
   // NumericConverter conv = new NumericConverter(prop.get());
   // String varname = getUniqueVariableName(INPUT_PREFIX);
   // NumericProbeVariable var = new NumericProbeVariable(conv.getDimension());
   // myVariables.put (varname, var);
   // NumericProbeDriver driver = new NumericProbeDriver();
   // driver.setExpression (varname, myVariables);
   // myDrivers.set (index, driver);
   //      
   // AddVectorPane newVec = new AddVectorPane(this, varname);
   // newVec.setDim(var.getDimension());
   // addVectorGUI(newVec);
   // super.updateGUI();
   // }

   private void setInputProbe() {
      ArrayList<String> variableNames = new ArrayList<String>();
      ArrayList<Integer> variableDims = new ArrayList<Integer>();

      for (Map.Entry<String,NumericProbeVariable> entry :
              myVariables.entrySet()) {
         variableNames.add (entry.getKey());
         variableDims.add (entry.getValue().getDimension());
         System.out.println (entry.getKey() + " -- " + entry.getValue());
      }

      int[] varDimsInt = new int[variableDims.size()];
      for (int i = 0; i < variableDims.size(); i++) {
         varDimsInt[i] = variableDims.get (i);
         // System.out.println("size of V="+variableDims.get(i));
      }
      NumericInputProbe probeToSet;
      if (oldProbe == null) {
         ModelComponent refComp =
            ComponentUtils.getPropertyComponent (myProperties.get (0));
         probeToSet = new NumericInputProbe (refComp);
      }
      else {
         probeToSet = oldProbe;
      }
      probeToSet.set (
         myProperties.toArray (new Property[0]), getDriverExpressions(),
         variableNames.toArray (new String[0]), varDimsInt);
      probeToSet.setStartTime (startTimeField.getDoubleValue());
      probeToSet.setStopTime (endTimeField.getDoubleValue());
      probeToSet.setScale (scaleField.getDoubleValue());
      probeToSet.setName (probeNameField.getStringValue());

      String attachedFilePath = attachedFileField.getStringValue();
      if (attachedFilePath != null) {
         if (attachedFilePath.equals ("") ||
             !attachedFileValid (attachedFilePath, null)) {
            attachedFilePath = null;
         }
      }
      if (attachedFilePath != null) {
         probeToSet.setAttachedFileName (attachedFilePath);
         try {
            probeToSet.load();
            ((Displayable)probeToSet).updateDisplays();
         }
         catch (IOException e) {
            System.err.println ("Couldn't load probe ");
            e.printStackTrace();
            probeToSet.setAttachedFileName (null);
         }
      }
      else {
         probeToSet.setAttachedFileName (null);
         if (probeToSet.getNumericList().isEmpty()) {
            // add knots at beginning and end
            if (probeToSet.isSettable()) {
               probeToSet.setData (probeToSet.getStartTime());
               probeToSet.setData (probeToSet.getStopTime());
            }
            else {
               probeToSet.loadEmpty();
            }
         }
      }
      if (oldProbe == null) {
         AddComponentsCommand cmd =
            new AddComponentsCommand (
               "add input probe", probeToSet,
               myMain.getRootModel().getInputProbes());
         myMain.getUndoManager().saveStateAndExecute (cmd);
      }
   }

   /**
    * Adds a blank input probe property. -adds a (null) Property and (invalid)
    * Driver to lists -adds the gui components corresponding to the proprty and
    * driver
    * 
    */
   public void addBlankInputProbeProperty() {
      // Data components: add a null property, and an empty (invalid) driver
      myProperties.add (myProperties.size(), null);
      NumericProbeDriver driver = new NumericProbeDriver();
      myDrivers.add (myDrivers.size(), driver);

      // GUI components:
      AddPropertyPane newPropPane = new AddPropertyPane (this);
      newPropPane.removePropNameField();
      addPropertyGUI (newPropPane);

      AddEquationPane newEqPane = new AddEquationPane (this);
      newEqPane.setEnabled (false);
      addEquationGUI (newEqPane);
   }

   // 
   // Called when a user renames a variable.
   // first rename the variable in myVariables, and then
   // go through all the drivers and rename
   // 
   public void renameInputVariable (String oldname, String newname) {
      try {
         NumericProbeVariable var = myVariables.get (oldname);
         myVariables.remove (oldname);
         myVariables.put (newname, var);
      }
      catch (Exception e) {
         System.out.println (e.getMessage());
      }

      for (NumericProbeDriver driver : myDrivers) {
         if (driver.renameVariable (oldname, newname)) {
            System.out.println ("new name different... renaming");
         }
         int id = myDrivers.indexOf (driver);
         getEqPane (id).setEqText (driver.getExpression());
         System.out.println ("setting driver expression to "
         + driver.getExpression());
      }
      super.updateGUI();
   }

   // 
   // Called when a user changes the variable's dimension
   // 
   public void changeVariableDimension (String varname, int dimen) {
      NumericProbeVariable var = myVariables.get (varname);
      myVariables.remove (var);
      myVariables.put (varname, new NumericProbeVariable (dimen));

      for (NumericProbeDriver driver : myDrivers) {
         if (driver.usesVariable (varname)) {
            driver.setInvalid();
            AddEquationPane eqPane = eqList.get (myDrivers.indexOf (driver));
            eqPane.setEqText ("");
            System.out.println ("invalidated driver with expr ["
            + driver.getExpression() + "]");
         }
      }
      // super.updateGUI();
   }

   public void changeExpression (int id, String newExpr) {
      NumericProbeDriver driver = myDrivers.get (id);
      AddEquationPane eqPane = eqList.get (id);
      String oldExpr = driver.getExpression();
      System.out.println ("old driver expr is [" + oldExpr + "]");
      if (newExpr == driver.getExpression()) {
         System.out.println ("same as old expression. do nothing.");
         return;
      }
      if (newExpr == "") // todo: either remove this bit, or remove it in the
                           // function that calls it
      {
         driver.setInvalid();
         System.out.println ("invalidating driver");
         return;
      }
      try {
         driver.setExpression (newExpr, myVariables);
      }
      catch (Exception exception) {
         System.out.println (exception.getMessage());
         GuiUtils.showError (this, exception.getMessage());
         invalidateDriverAndGUI (id);
         return;
      }
      // if everything is ok up to this point, check to see if the size matches:
      checkExpressionConsistency (id);
      eqPane.updateAppearance();
      eqPane.revalidate();
      eqPane.repaint();
      // not sure if this is needed, since driver is a shallow copy?
      myDrivers.remove (id);
      myDrivers.add (id, driver);
      System.out.println ("change OK. replacing, [" + oldExpr + "] with ["
      + newExpr + "]");
   }

   public void checkExpressionConsistency (int id) {
      if (getPropDim (myProperties.get (id)) !=
          myDrivers.get(id).getOutputSize()) {
         invalidateDriverAndGUI (id);
         GuiUtils.showError (this, "Driver and Property size mismatch!");
      }
   }

   // 
   // Called when a user deletes a property
   // 
   public void deleteProperty (int idx) {
      try {
         myProperties.remove (idx);
         System.out.println ("removing property index " + idx + " and driver "
         + myDrivers.get (idx).getExpression());
         myDrivers.remove (idx);
      }
      catch (Exception e) {
         System.out.println (e.getMessage());
      }

      super.updateGUI();
   }

   // 
   // Called when a user deletes an input variable
   // 
   public void deleteInputVariable (String varname) {
      myVariables.remove (varname);
      System.out.println ("before remove, size =" + myVariables.size());
      for (NumericProbeDriver driver : myDrivers) {
         System.out.println ("looking at driver expression: ["
         + driver.getExpression() + "]");
         if (driver.usesVariable (varname)) {
            System.out.println (driver.getExpression() + " invalidated");
            driver.setInvalid();
         }
      }
      super.updateGUI();
      System.out.println ("after remove, size =" + myVariables.size());
   }

   /**
    * called when the user adds a new input variable. simple create a new
    * variable and place it into the hash table.
    * 
    * @param name
    * name of variable
    * @param dimen
    * dimension of variable
    */
   public void addInputVariable (String name, int dimen) {
      NumericProbeVariable var = new NumericProbeVariable (dimen);
      myVariables.put (name, var);

      super.updateGUI();
   }

   /**
    * Event handler. Goes through the input probe specific commands first, and
    * if its not specifically for an input probe, send it up to the parent class
    */
   public void actionPerformed (ActionEvent e) {
      if (e.getActionCommand() == "Add") {
         String name = getUniqueVariableName (INPUT_PREFIX);

         AddVectorPane newVec = new AddVectorPane (this, name);
         addVectorGUI (newVec);

         addInputVariable (name, 1);
         increaseFrameSize();
      }
      else if (e.getActionCommand() == "Delete Channel") {
         AddVectorPane vecPane = (AddVectorPane)e.getSource();
         System.out.println ("Deleting Channel GUI ID" + e.getID());
         String name = vecPane.getChannelName();
         revalidateDrivers (name);
         deleteInputVariable (name);
         removeChannelGUI (vecPane);
      }
      else if (e.getActionCommand() == "Add Property") {
         addBlankInputProbeProperty();
         increaseFrameSize();
      }
      else if (e.getActionCommand() == "Delete Property") {
         AddPropertyPane pane = (AddPropertyPane)e.getSource();
         int index = propList.indexOf (pane);
         System.out.println ("Deleting Property ID" + index);
         removePropertyGUI (index);
         removeEquationGUI (index);
         deleteProperty (index);
      }
      else if (e.getActionCommand() == "PropSelected") {
         AddPropertyPane propPane = (AddPropertyPane)e.getSource();
         int index = propList.indexOf (propPane);
         System.out.println ("prop selected from prop pane #: " + index);
         String propPath = propPane.getPropPath();
         Property prop = getProp (propPath);
         updateProperty (index, prop);
         increaseFrameSize();
      }
      else if (e.getActionCommand() == "blank") {
         // happens when we make a non-selection
         AddPropertyPane propPane = (AddPropertyPane)e.getSource();
         int index = propList.indexOf (propPane);
         if (index < 0)
            return;

         System.out.println ("trying to get eq at index " + index);
         AddEquationPane eqPane = getEqPane (index);
         if (eqPane != null) {
            try {
               // myProperties.set(index, null);
               myDrivers.get (index).setInvalid();
            }
            catch (Exception exp) {
               System.out.println (exp.getMessage());
            }
            eqPane.setEnabled (false);
            eqPane.setEqText ("");
         }
      }
      else if (e.getActionCommand() == "Resized") {
         AddVectorPane vecPane = (AddVectorPane)e.getSource();
         NumericProbeVariable var = myVariables.get (vecPane.getChannelName());
         if (var != null) {
            changeVariableDimension (vecPane.getChannelName(),
                                     vecPane.getDim());
            System.out.println ("new dim: " + vecPane.getDim()); // myVariables.get(vecPane.getChannelName()).getDimension());
         }
      }
      else if (e.getActionCommand() == "Renamed") {
         AddVectorPane vecPane = (AddVectorPane)e.getSource();
         String newName = vecPane.getChannelName();
         String oldName = vecPane.getOldName();
         for (AddVectorPane pane : probeChannels) {
            if (!pane.equals (vecPane)) {
               if (newName.compareTo (pane.getChannelName()) == 0) {
                  // check to make sure there are no duplicates

                  GuiUtils.showError (this, "Name already exists!");
                  newName = vecPane.getOldName();
                  // so in this case, the 'new' name is the same as old, so
                  // effectively we don't make any changes
                  // and when calling setChannelName a few lines later we
                  // restore the old name into the GUI
               }
            }
         }
         vecPane.setOldName (newName);
         vecPane.setChannelName (newName);
         renameInputVariable (oldName, newName);
         System.out.println (oldName + " changed to " + newName);
         // if (newName.compareTo(oldName) != 0 )
         // {
         // revalidateDrivers(oldName); //any driver that uses this name should
         // be invalidated
         // }
      }
      else if (e.getActionCommand() == "Expression Changed") {
         AddEquationPane eqPane = (AddEquationPane)e.getSource();
         System.out.println ("new expression entered :" + eqPane.getEqText());
         String newExpr = eqPane.getEqText();
         if (!newExpr.equals ("")) // otherwise, do nothing and driver will
                                    // remain invalid
         {
            changeExpression (eqList.indexOf (eqPane), newExpr);
         }
         else {
            invalidateDriverAndGUI (eqList.indexOf (eqPane));
         }
         // check consistency of size:
      }
      // else if (e.getActionCommand() == "Browse")
      // {
      // JFileChooser fileDialog = new JFileChooser();
      // File attachedFile =
      // if (attachedFile != null)
      // {
      // fileDialog.setSelectedFile(attachedFile);
      // }
      // else
      // {
      // System.out.println(ArtisynthPath.getWorkingDir().getPath());
      // fileDialog.setCurrentDirectory(ArtisynthPath.getWorkingDir());
      // }
      // int retVal = fileDialog.showOpenDialog(this);
      // if (retVal == JFileChooser.APPROVE_OPTION)
      // {
      // attachedFileField.setValue(fileDialog.getSelectedFile());
      // }
      // }
      else if (e.getActionCommand() == "MouseEnteredProp") {
         int index = propList.indexOf (e.getSource());
         AddEquationPane eqPane = eqList.get (index);
         if (eqPane != null) {
            eqPane.setHighlight (true);
         }
      }
      else if (e.getActionCommand() == "MouseExitedProp") {
         int index = propList.indexOf (e.getSource());
         AddEquationPane eqPane = eqList.get (index);
         if (eqPane != null) {
            eqPane.setHighlight (false);
         }
      }
      else if (e.getActionCommand() == "MouseEnteredEq") {
         int index = eqList.indexOf (e.getSource());
         AddPropertyPane propPane = propList.get (index);
         if (propPane != null) {
            propPane.setHighlight (true);
         }
      }
      else if (e.getActionCommand() == "MouseExitedEq") {
         int index = eqList.indexOf (e.getSource());
         AddPropertyPane propPane = propList.get (index);
         if (propPane != null) {
            propPane.setHighlight (false);
         }
      }
      else if (e.getActionCommand() == "Done") {
         setInputProbe();
//          if (myParent != null) {
//             myParent.actionPerformed (new ActionEvent (this, 0, "ProbeEdited"));
//          }
         
         Timeline timeline = myMain.getTimeline();
         if (timeline != null) {
            timeline.updateProbes(myMain.getRootModel());
         }
         
         dispose();
      }
      else { // send it to the superclass
         super.actionPerformed (e);
      }
      refreshAddBtn();
   }

   /**
    * Called when a valid property has been selected for the first time. A
    * property is null before this point. so we set the property at the
    * corresponding index in the ArrayList. Then we automatically create an
    * input variable to go along with this property. The GUI elements are added
    * accordingly for the new input vector. The driver at the current index is
    * also created with the input variable name as its expression.
    * 
    * @param index
    * index of property (also of driver, since they should be in sync)
    * @param prop
    * the selected property.
    */
   public void addProperty (int index, Property prop) {
      myProperties.set (index, prop);

      NumericConverter conv = new NumericConverter (prop.get());
      String varname = getUniqueVariableName (INPUT_PREFIX);
      NumericProbeVariable var =
         new NumericProbeVariable (conv.getDimension());
      myVariables.put (varname, var);

      AddVectorPane newVec = new AddVectorPane (this, varname);
      newVec.setDim (var.getDimension());
      addVectorGUI (newVec);

      NumericProbeDriver driver = new NumericProbeDriver();
      driver.setExpression (varname, myVariables);
      myDrivers.set (index, driver);

      updateGUI();
   }

   /**
    * called when a property at a particular index is changed. if the old
    * property was null, we add one to the list. otherwise call changeProperty.
    * 
    * @param id
    * @param prop
    */
   protected void updateProperty (int id, Property prop) {
      if (myProperties.get (id) == null) {
         addProperty (id, prop);
         AddEquationPane eqPane = getEqPane (id);
         eqPane.setEqText (myDrivers.get (id).getExpression());
         getEqPane (id).setEnabled (true);
         System.out.println ("first time... add property + variable components");
      }
      else // prop already exists
      {
         changeProperty (prop, id);
         getEqPane (id).setEnabled (true);
         System.out.println ("successive time... just change the property");
      }
   }

   /**
    * this occurs when a current property in the list is valid, but a new one is
    * selected to take its place. first we check if the same property has been
    * selected. if this is the case, don't need to do anything. otherwise, if a
    * new one is selected, we replace the old one, and invalidate the driver if
    * and ONLY if the size has changed. (otherwise old driver will still be
    * valid).
    */
   public void changeProperty (Property newprop, int idx) {
      if (newprop.equals (myProperties.get (idx))) {
         System.out.println ("new and old the same. don't need to replace");
         return;
      }
      System.out.println ("replacing property @ index " + idx);
      Property oldprop = myProperties.remove (idx);
      myProperties.add (idx, newprop);

      if (getPropDim (newprop) == getPropDim (oldprop)) {
         System.out.println ("new prop has same dim as old. keep driver");
      }
      else {
         System.out.println (myDrivers.get (idx).getExpression()
         + " invalidated");
         myDrivers.get (idx).setInvalid();
         getEqPane (idx).setEqText ("");
      }

      updateGUI();
   }

}
