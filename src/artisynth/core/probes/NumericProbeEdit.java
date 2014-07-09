/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.util.*;
import maspack.properties.*;
import artisynth.core.modelbase.*;

public class NumericProbeEdit {
   ArrayList<NumericProbeDriver> myDrivers;
   LinkedHashMap<String,NumericProbeVariable> myVariables;
   ArrayList<Property> myProperties;

   void load (NumericProbeBase probe) {
      // when opening a probe we create local copies of:
      //
      // 1) the properties
      // 2) the drivers
      // 3) the variables
      // 4) the variable dimensions (for input probes)

      NumericProbeDriver[] drivers = probe.getDrivers();
      myDrivers = new ArrayList<NumericProbeDriver>();
      for (int i = 0; i < drivers.length; i++) {
         myDrivers.add (new NumericProbeDriver (drivers[i]));
      }

      myProperties = new ArrayList<Property>();
      Property[] props = probe.getAttachedProperties();
      for (int i = 0; i < props.length; i++) {
         myProperties.add (props[i]);
      }

      myVariables = new LinkedHashMap<String,NumericProbeVariable>();
      for (Map.Entry<String,NumericProbeVariable> entry :
              probe.getVariables().entrySet()) {
         myVariables.put (entry.getKey(),
                          new NumericProbeVariable (entry.getValue()));
      }
   }

   //
   // Called when a user adds an input property
   //
   void addInputProbeProperty (Property prop) {
      myProperties.add (prop);

      NumericConverter conv = new NumericConverter (prop.get());
      String varname = getUniqueVariableName ("V");
      NumericProbeVariable var =
         new NumericProbeVariable (conv.getDimension());
      myVariables.put (varname, var);
      NumericProbeDriver driver = new NumericProbeDriver();
      driver.setExpression (varname, myVariables);
      myDrivers.add (driver);

      updateGUI();
   }

   //
   // Called when a user adds an input variable
   //
   void addInputVariable (String name, int dimen) {
      NumericProbeVariable var = new NumericProbeVariable (dimen);
      myVariables.put (name, var);

      updateGUI();
   }

   // 
   // Called when a user deletes a property
   // 
   void deleteProperty (int idx) {
      Property prop = myProperties.remove (idx);
      myDrivers.remove (idx);

      updateGUI();
   }

   // 
   // Called when a user deletes an input variable
   // 
   void deleteInputVariable (String varname) {
      NumericProbeVariable var = myVariables.get (varname);
      myVariables.remove (var);

      for (NumericProbeDriver driver : myDrivers) {
         if (driver.usesVariable (varname)) {
            driver.setInvalid();
         }
      }

      updateGUI();
   }

   // 
   // Called when a user renames a variable
   // 
   void renameInputVariable (String oldname, String newname) {
      NumericProbeVariable var = myVariables.get (oldname);
      myVariables.remove (var);
      myVariables.put (newname, var);

      for (NumericProbeDriver driver : myDrivers) {
         driver.renameVariable (oldname, newname);
      }
      updateGUI();
   }

   // 
   // Called when a user
   // 
   void changeVariableDimension (String varname, int dimen) {
      NumericProbeVariable var = myVariables.get (varname);
      myVariables.remove (var);
      myVariables.put (varname, new NumericProbeVariable (dimen));

      for (NumericProbeDriver driver : myDrivers) {
         if (driver.usesVariable (varname)) {
            driver.setInvalid();
         }
      }
      updateGUI();
   }

   // 
   // Called when a user changes a property
   // 
   void changeProperty (Property newprop, int idx) {
      myProperties.remove (idx);
      myProperties.add (idx, newprop);

      myDrivers.get (idx).setInvalid();
      updateGUI();
   }

   // 
   // Called when a user changes an expression
   // 
   void changeExpression (String newexpr, int idx) {
      NumericProbeDriver driver = myDrivers.get (idx);
      try {
         driver.setExpression (newexpr, myVariables);
      }
      catch (Exception e) { // handle error
      }
      updateGUI();
   }

   String getUniqueVariableName (String prefix) {
      // look through myVariables to find a unique name;
      for (int i = 0; i < myVariables.size(); i++) {
         if (myVariables.get (prefix + i) == null) {
            return prefix + i;
         }
      }
      return prefix + myVariables.size();
   }

   //
   // Stub routine for updating the GUI
   //
   void updateGUI() {
   }
}
