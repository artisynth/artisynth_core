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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import maspack.properties.NumericConverter;
import maspack.properties.Property;
import maspack.util.DoubleInterval;
import maspack.util.StringHolder;
import maspack.widgets.GuiUtils;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.widgets.ValueCheckListener;
import artisynth.core.driver.Main;
import artisynth.core.gui.Timeline;
import artisynth.core.gui.editorManager.AddComponentsCommand;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.probes.NumericProbeDriver;
import artisynth.core.probes.NumericProbeVariable;
import artisynth.core.probes.Probe;
import artisynth.core.util.ArtisynthPath;

public class OutputNumericProbeEditor extends NumericProbeEditor implements
ValueChangeListener {
   static final long serialVersionUID = 1L;
   private NumericOutputProbe oldProbe;

   private class OutputFileChecker implements ValueCheckListener {
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

   public OutputNumericProbeEditor() {
      super (); // call superclass constructor
      setupGUI();
      oldProbe = null;
      addBlankOutputProperty();
      addPropertyButton.setEnabled (false);
      refreshAddBtn();
      
      attachedFileField.addValueChangeListener (this);
      attachedFileField.addValueCheckListener (new OutputFileChecker());
      
      originalFilePath = null;
      resizeFrame();
   }

   public OutputNumericProbeEditor (NumericOutputProbe inProbe) {
      super (); // call superclass constructor
      oldProbe = inProbe;

      attachedFile = inProbe.getAttachedFile();
      attachedFileField.setValue (Probe.getPathFromFile (attachedFile));
      attachedFileField.addValueCheckListener (new OutputFileChecker());

      setupGUI();
      setupProbeProperties (inProbe);
      dispLowerField.setValue (inProbe.getDefaultDisplayMin());
      dispUpperField.setValue (inProbe.getDefaultDisplayMax());
      // copy the probe, and then retreive its contents to be placed in the
      // editor
      Property props[] = inProbe.getAttachedProperties();
      for (Property prop : props) {
         if (NumericConverter.isNumeric (prop.get())) {
            // System.out.println(prop.getName()+" found in probe in "+
            // prop.getHost().toString());
            String compPath = getFullPropPath (prop);
            // System.out.println("path of comp ="+compPath);
            addBlankOutputProperty();
            AddPropertyPane propPane = propList.get (propList.size() - 1); 
            propPane.setCompPath (compPath);
            propPane.setEditable (false);
         }
      }
      // at this point, selecting properties would have automatically added
      // output vectors
      // so we delete them first, before loading the ones defined in the probe
      myDrivers.clear();

      for (AddEquationPane pane : eqList) {
         equationPane.remove (pane);
      }
      // for (AddVectorPane pane: probeChannels)
      // {
      // vectorPane.remove(pane);
      // }
      eqList.clear();
      probeChannels.clear();

      myVariables.clear();

      LinkedHashMap<String,NumericProbeVariable> oldVariables =
         inProbe.getVariables();
      // make a deep copy of the variable list
      for (Map.Entry<String,NumericProbeVariable> entry :
              oldVariables.entrySet()) {
         myVariables.put (entry.getKey(),
                          new NumericProbeVariable (entry.getValue()));
      }
      // add the drivers
      for (NumericProbeDriver driver : inProbe.getDrivers()) {
         myDrivers.add (myDrivers.size(), driver);

         AddEquationPane eqPane = createEquationPane (driver.getExpression());

         // eqPane.updateAppearance();

         // AddVectorPane vecPane = new AddVectorPane(this, ""); //name
         // irrevalent
         eqPane.setDimensionLabel (driver.getOutputSize());

         addEquationGUI (eqPane);
         // addVectorGUI(vecPane);
      }
      resizeFrame();
      refreshAddBtn();
   }

   private void setupGUI() {
      this.setTitle ("Output Probe Editor");
      splitPane.add (propPane);
      JPanel EqVecPane = new JPanel();
      equationPane.setMinimumSize (new Dimension (200, 0));

      EqVecPane.setLayout (new BoxLayout (EqVecPane, BoxLayout.X_AXIS));
      EqVecPane.add (equationPane);
      // vectorPane.setBorder(null);
      // EqVecPane.add(vectorPane);
      splitPane.add (EqVecPane);
      splitPane.setDividerSize (10);

      // middlePane.add(Box.createRigidArea(new Dimension(10,0)));
      middlePane.add (splitPane);
      // middlePane.add(vectorPane);
      // middlePane.add(Box.createRigidArea(new Dimension(10,0)));

      infoPaneA.remove (nameLabel);
      infoPaneA.revalidate();

      JPanel btnPane = new JPanel();
      btnPane.setLayout (new BoxLayout (btnPane, BoxLayout.X_AXIS));
      btnPane.add (Box.createRigidArea (new Dimension (8, 0))); // tweakable
                                                                  // value
      btnPane.setMinimumSize (new Dimension (30, 35));
      btnPane.setMaximumSize (new Dimension (Integer.MAX_VALUE, 35));
      btnPane.add (addVectorButton);
      btnPane.setAlignmentX (LEFT_ALIGNMENT);
      equationPane.add (btnPane, 1);

      JPanel btnPane2 = new JPanel();
      btnPane2.setLayout (new BoxLayout (btnPane2, BoxLayout.X_AXIS));
      btnPane2.add (Box.createRigidArea (new Dimension (14, 0))); // tweakable
                                                                  // value
      btnPane2.add (addPropertyButton);
      btnPane2.setMinimumSize (new Dimension (30, 35));
      btnPane2.setMaximumSize (new Dimension (Integer.MAX_VALUE, 35));
      btnPane2.setAlignmentX (LEFT_ALIGNMENT);
      propPane.add (btnPane2, 1);

      rightPropertyPanel.addWidget (rangeField);
      rightPropertyPanel.addWidget (intervalField);

      rightPropertyPanel.addWidget (attachedFileField);
   }

   private void setOutputProbe() {
      ArrayList<String> variableNames = new ArrayList<String>();
      for (Map.Entry<String,NumericProbeVariable> entry :
              myVariables.entrySet()) {
         variableNames.add (entry.getKey());
         System.out.println ("variable: " + entry.getKey() + " added to list");
      }
      for (Property prop : myProperties) {
         System.out.println ("property " + prop.getName() + " found");
      }
      // System.out.println("myProps size ="+myProperties.size());
      // System.out.println("myVars size ="+variableNames.size());
      NumericOutputProbe probeToSet;
      if (oldProbe == null) {
         probeToSet = new NumericOutputProbe();
      }
      else {
         probeToSet = oldProbe;
      }
      probeToSet.set (
         myProperties.toArray (new Property[0]), getDriverExpressions(),
         variableNames.toArray (new String[0]));
      probeToSet.setStartTime (startTimeField.getDoubleValue());
      probeToSet.setStopTime (endTimeField.getDoubleValue());
      probeToSet.setName (probeNameField.getStringValue());
      probeToSet.setUpdateInterval (intervalField.getDoubleValue());
      if (!rangeField.valueIsVoid()) {
         DoubleInterval range = rangeField.getRangeValue();
         probeToSet.setDefaultDisplayRange (
            range.getLowerBound(), range.getUpperBound());
      }
      // probeToSet.setDefaultDisplayRange(
      // dispLowerField.getDoubleValue(), dispUpperField.getDoubleValue());
      probeToSet.setAttachedFileName (attachedFileField.getText());
      if (oldProbe == null) {
         AddComponentsCommand cmd =
            new AddComponentsCommand (
               "add output probe", probeToSet,
               myMain.getRootModel().getOutputProbes());
         myMain.getUndoManager().saveStateAndExecute (cmd);

         // System.out.println("track index: "+probeToSet.getTrack());
         // myMain.getRootModel().addOutputProbe(probeToSet);
         // myMain.getTimeline().addProbe(probeToSet);
      }
   }

   public void valueChange (ValueChangeEvent v) {
      if (v.getSource() == attachedFileField) {
         String newPath = attachedFileField.getStringValue();
         if (newPath == originalFilePath) {
            return;
         }
         File newFile = new File(newPath);
         originalFilePath = newPath;
         
         String fullPath = newPath;
         if (!newFile.isAbsolute()) {
            fullPath =
               ArtisynthPath.getWorkingDirPath() + File.separator + newPath;   
         }
         
         // System.out.println("filepath changed to "+ fullPath);
         File tmpFile = new File (fullPath);
         if (tmpFile.exists()) {
            if (tmpFile.isDirectory()) {
               
            }
            // System.out.println("file exists");
         }
         else {
            // System.out.println("file doesn't exist!");
            if (tmpFile.getParentFile().exists()) {
               // System.out.println("parent dir: "+ tmpFile.getParent());
               try {
                  boolean ok = tmpFile.createNewFile();
                  // System.out.println("new file creation: "+ ok);
               }
               catch (Exception e) {
                  System.out.println (e.getMessage());
               }
            }
            else {
               // System.out.println("ask if directory should be created");
               // JOptionPane pane = new JOptionPane("Create directory for
               // file?", JOptionPane.OK_CANCEL_OPTION);
               if (JOptionPane.showConfirmDialog (
                  this, "Create Directory for file?", "Non-existent directory",
                  JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                  System.out.println ("making directory(s)...");
                  tmpFile.getParentFile().mkdirs();
                  try {
                     tmpFile.createNewFile();
                  }
                  catch (Exception e) {
                     System.out.println (e.getMessage());
                  }

               }
            }
         }
      }
   }

   public void actionPerformed (ActionEvent e) {
      if (e.getActionCommand() == "Add") {
         addBlankOutputProbeVector();
         increaseFrameSize();

      }
      else if (e.getActionCommand() == "Add Property") {
         addBlankOutputProperty();
         increaseFrameSize();
      }
      // else if (e.getActionCommand() == "Browse")
      // {
      // JFileChooser fileDialog = new JFileChooser();
      // System.out.println(ArtisynthPath.getWorkingDir().getPath());
      // fileDialog.setCurrentDirectory(ArtisynthPath.getWorkingDir());

      // int retVal = fileDialog.showOpenDialog(this);
      // if (retVal == JFileChooser.APPROVE_OPTION)
      // {
      // attachedFileField.setValue(fileDialog.getSelectedFile());
      // }
      // }
      else if (e.getActionCommand() == "PropSelected") { // same as input?
         AddPropertyPane propPane = (AddPropertyPane)e.getSource();
         propPane.setPropNameEnabled (true);
         int index = propList.indexOf (propPane);
         System.out.println ("prop selected from prop pane #: " + index);
         String propPath = propPane.getPropPath();
         Property prop = getProp (propPath);
         if (updateProperty (index, prop)) {
            increaseFrameSize();
         }
         updateVariable (propPane.getPropNameFieldText(), prop);
      }
      else if (e.getActionCommand() == "blank") { 
        // happens when we receive a non-selection from the prop select box
         // todo: decide if this method is better (leave prop in list as is)
         // or the one in input editor (where we null the property).
         AddPropertyPane propPane = (AddPropertyPane)e.getSource();
         int index = propList.indexOf (propPane);
         if (index < 0)
            return;

         // go through all drivers and invalidate the ones that use this
         // property

         for (NumericProbeDriver driver : myDrivers) {
            if (driver.usesVariable (propPane.getPropNameFieldText())) {
               int driverIndex = myDrivers.indexOf (driver);
               driver.setInvalid();
               AddEquationPane eqPane = eqList.get (driverIndex);
               eqPane.setEqText ("");
               eqPane.clearDimensionLabel();
               // AddVectorPane vecPane = probeChannels.get(driverIndex);
               // vecPane.clearDimBox();
               System.out.println ("invalidated driver with expr ["
               + driver.getExpression() + "]");
            }
         }
         myVariables.remove (propPane.getPropNameFieldText());
         myProperties.set (index, null);
         propPane.setPropNameEnabled (false);

         // AddEquationPane eqPane = getEqPane(index);
         // if (eqPane != null)
         // {
         // try
         // {
         // myProperties.set(index, null);
         // myDrivers.get(index).setInvalid();
         // }
         // catch (Exception exp)
         // {
         // System.out.println(exp.getMessage());
         // }
         // eqPane.setEnabled(false);
         // eqPane.setEqText("");
         // }
      }
      else if (e.getActionCommand() == "Expression Changed") // delete output
      {
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
            eqPane.clearDimensionLabel();
         }
      }
      else if (e.getActionCommand() == "Delete Channel") // delete output
      {
         System.out.println ("Deleting channel and equation GUI...");
         AddEquationPane pane = (AddEquationPane)e.getSource();
         int index = eqList.indexOf (pane);
         removeEquationGUI (index);
         deleteOutput (index);
      }
      else if (e.getActionCommand() == "Delete Property") // delete a property
      {
         AddPropertyPane pane = (AddPropertyPane)e.getSource();
         int index = propList.indexOf (pane);
         String propname = pane.getPropNameFieldText();
         System.out.println ("Deleting Property ID" + index);
         removePropertyGUI (index);
         deleteProperty (index);
         myVariables.remove (propname);
         revalidateDrivers (propname);
      }

      else if (e.getActionCommand() == "Property Renamed") // rename property
      {
         AddPropertyPane propPane = (AddPropertyPane)e.getSource();
         String newName = propPane.getPropNameFieldText();
         String oldName = propPane.getOldPropName();
         for (AddPropertyPane pane : propList) {
            if (!propPane.equals (pane)) {
               if (newName.compareTo (pane.getPropNameFieldText()) == 0) {
                  // check to make sure there are no duplicates
                  GuiUtils.showError (this, "Name already exists!");
                  newName = oldName;
                  // so in this case, the 'new' name is the same as old, so
                  // effectively we don't make any changes
                  // and when calling setChannelName a few lines later we
                  // restore the old name into the GUI
               }
            }
         }
         // System.out.println("trying to set name");
         propPane.setPropNameField (newName);
         renameProperty (oldName, newName);
      }
      else if (e.getActionCommand() == "MouseEnteredEq") {
         // int index = eqList.indexOf(e.getSource());
         // AddVectorPane vec = probeChannels.get(index);
         // vec.setHighlight(true);
      }
      else if (e.getActionCommand() == "MouseExitedEq") {
         // int index = eqList.indexOf(e.getSource());
         // AddVectorPane vec = probeChannels.get(index);
         // vec.setHighlight(false);
      }
      else if (e.getActionCommand() == "MouseEnteredVec") {
         // int index = probeChannels.indexOf(e.getSource());
         // AddEquationPane eq = eqList.get(index);
         // eq.setHighlight(true);
      }
      else if (e.getActionCommand() == "MouseExitedVec") {
         // int index = probeChannels.indexOf(e.getSource());
         // AddEquationPane eq = eqList.get(index);
         // eq.setHighlight(false);
      }
      else if (e.getActionCommand() == "Done") // done. add the probe!
      {
         setOutputProbe();
//          if (myParent != null) {
//             myParent.actionPerformed (new ActionEvent (this, 0, "ProbeEdited"));
//          }
         
         Timeline timeline = myMain.getTimeline();
         if (timeline != null) {
            timeline.updateProbes(myMain.getRootModel());
         }
         
         dispose();
      }
      else {
         super.actionPerformed (e);
      }
      refreshAddBtn();
   }

   private AddEquationPane createEquationPane (String text) {
      AddEquationPane newEqPane = new AddEquationPane (this);
      newEqPane.setEqText (text);
      newEqPane.createDeletePopup();
      return newEqPane;
   }

   private void addBlankOutputProbeVector() {

      // Data components:
      NumericProbeDriver driver = new NumericProbeDriver();
      driver.setInvalid();
      myDrivers.add (myDrivers.size(), driver);

      // GUI components:
      // String name = getUniqueVariableName(OUTPUT_PREFIX);
      // AddVectorPane newVecPane = new AddVectorPane(this, null);
      // newVecPane.setAsOutput();
      // addVectorGUI(newVecPane);

      AddEquationPane newEqPane = createEquationPane ("");
      addEquationGUI (newEqPane);

   }

   private void addBlankOutputProperty() {
      // GUI components:
      String propname = getUniqueVariableName (OUTPUT_PREFIX);
      AddPropertyPane newPropPane = new AddPropertyPane (this);
      newPropPane.setPropNameField (propname);
      newPropPane.setPropNameEnabled (true);
      addPropertyGUI (newPropPane);

      // Data components:
      myProperties.add (myProperties.size(), null);
      NumericProbeVariable variable = new NumericProbeVariable (1);
      myVariables.put (propname, variable);
   }

   private void deleteOutput (int index) {
      myDrivers.remove (index);
   }

   private boolean updateProperty (int id, Property prop) {
      if (myProperties.get (id) == null) {
         return addProperty (id, prop);
         // AddEquationPane eqPane = getEqPane(id);
         // eqPane.setEqText(myDrivers.get(id).getExpression());
         // getEqPane(id).setEnabled(true);
         // System.out.println("first time... add property");
      }
      else // prop already exists
      {
         changeProperty (prop, id);
         return false;

         // getEqPane(id).setEnabled(true);
         // System.out.println("successive time... just change the property");
      }
   }

   private void updateVariable (String name, Property prop) { 
      // the way we have to check to see the drivers work here is similar to
      // updateProperty in the input probe editor.
      NumericProbeVariable var = myVariables.get (name);
      if (var.getDimension() != getPropDim (prop)) {
         System.out.println (name + " variable changed to size "
         + getPropDim (prop));
         var.setDimension (getPropDim (prop));
         for (NumericProbeDriver driver : myDrivers) {
            System.out.println ("driver expression is : "
            + driver.getExpression());
            if (driver.usesVariable (name)) {
               int index = myDrivers.indexOf (driver);
               System.out.println ("Driver #" + index + "uses this variable");
               AddEquationPane eqPane = eqList.get (index);
               // AddVectorPane vecPane = probeChannels.get(index);
               try {
                  System.out.println ("size used to be: "
                  + driver.getOutputSize());
                  driver.setExpression (driver.getExpression(), myVariables);
                  // vecPane = probeChannels.get(index);
                  System.out.println ("size is now: " + driver.getOutputSize());
                  eqPane.setDimensionLabel (driver.getOutputSize());
               }
               catch (Exception exception) {
                  System.out.println (exception.getMessage());
                  GuiUtils.showError (this, exception.getMessage());
                  invalidateDriverAndGUI (index);
                  eqPane.setEqText (""); // invalidate current driver.
                  eqPane.clearDimensionLabel();
                  System.out.println ("invalidating driver @ index " + index);
               }

            }
         }
      }
   }

   public boolean addProperty (int index, Property prop) {
      myProperties.set (index, prop);
      AddPropertyPane propPane = propList.get (index);
      String varname = propPane.getPropNameFieldText();

      NumericConverter conv = new NumericConverter (prop.get());

      NumericProbeVariable var =
         new NumericProbeVariable (conv.getDimension());
      myVariables.put (varname, var);

      // see if there is an invalid driver at the end of the
      // list. If so, use that:
      if (myDrivers.size() > 0 &&
          !myDrivers.get (myDrivers.size() - 1).isValid()) {
         int id = myDrivers.size() - 1;
         AddEquationPane eqn = eqList.get (id);
         eqn.setEqText (varname);
         changeExpression (id, varname);
         updateGUI();
         return false;
      }
      else { // otherwise, add a new driver
         NumericProbeDriver driver = new NumericProbeDriver();
         // System.out.println("addProperty; var name= "+varname);
         driver.setExpression (varname, myVariables);

         // add it at the end of list
         myDrivers.add (myDrivers.size(), driver);
         // add an equation pane for it
         AddEquationPane newEq = createEquationPane (driver.getExpression());
         newEq.setDimensionLabel (driver.getOutputSize());
         newEq.updateAppearance();
         addEquationGUI (newEq);
         updateGUI();
         return true;
      }

   }

   public void changeProperty (Property newprop, int idx) {
      if (newprop.equals (myProperties.get (idx))) {
         // System.out.println("new and old the same. don't need to replace");
         return;
      }
      System.out.println ("replacing property @ index " + idx);
      Property oldprop = myProperties.remove (idx);
      myProperties.add (idx, newprop);

      if (getPropDim (newprop) == getPropDim (oldprop)) {
         System.out.println ("new prop has same dim as old. keep driver");
      }
      else {
         // the size is different, but there is a chance that it could work as
         // long as
         // the equation is valid- requirement is less stringent than
         // input probe since the size only matters during the operation, and
         // not the output

      }

      updateGUI();
   }

   public void changeExpression (int id, String expr) {
      NumericProbeDriver driver = myDrivers.get (id);
      // AddVectorPane vecPane = probeChannels.get(id);
      String oldExpr = driver.getExpression();
      System.out.println ("old driver expr is [" + oldExpr + "]");
      if (expr == driver.getExpression()) {
         System.out.println ("same as old expression. do nothing.");
         return;
      }
      if (expr == "")// todo: either remove this bit, or remove it in the
                     // function that calls it
      {
         driver.setInvalid();
         System.out.println ("invalidating driver");
         return;
      }
      AddEquationPane eqPane = eqList.get (id);
      try {
         driver.setExpression (expr, myVariables);
         // vecPane = probeChannels.get(id);
         System.out.println ("setting size display to: "
         + driver.getOutputSize());
         eqPane.setDimensionLabel (driver.getOutputSize());
         eqPane.updateAppearance();
      }
      catch (Exception exception) {
         System.out.println (exception.getMessage());
         GuiUtils.showError (this, exception.getMessage());
         eqPane.clearDimensionLabel();
         invalidateDriverAndGUI (id);

         return;
      }

   }

   /**
    * renames the current property by: a.) changing the name in myVariables b.)
    * updating this change in all drivers
    */
   public void renameProperty (String oldname, String newname) {
      // first, rename the property in the variables list.
      try {
         NumericProbeVariable var = myVariables.get (oldname);
         // System.out.println(var.getDimension());
         myVariables.remove (oldname);
         myVariables.put (newname, var);
      }
      catch (Exception e) {
         System.out.println (e.getMessage());
      }
      // then, rename it for each driver.
      for (NumericProbeDriver driver : myDrivers) {
         if (driver.isValid()) {
            driver.renameVariable (oldname, newname);
            System.out.println (oldname + " changed to " + newname);
            int id = myDrivers.indexOf (driver);
            getEqPane (id).setEqText (driver.getExpression());
            getEqPane (id).updateAppearance();
            System.out.println ("setting driver " + id + " expression to "
            + driver.getExpression());
         }
      }
      // System.out.println("Done renaming of Property");
   }

   // 
   // Called when a user deletes a property
   // 
   public void deleteProperty (int idx) {
      try {
         Property prop = myProperties.remove (idx);
         // System.out.println("removing property index "+ idx);

      }
      catch (Exception e) {
         System.out.println (e.getMessage());
      }

      super.updateGUI();
   }
}
