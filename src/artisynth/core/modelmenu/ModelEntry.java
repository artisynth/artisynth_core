/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

import java.awt.Component;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;

import artisynth.core.driver.ModelScriptInfo;
import artisynth.core.driver.ModelScriptInfo.InfoType;
import artisynth.core.modelmenu.ModelScriptMenuParser.MenuType;
import artisynth.core.workspace.RootModel;

import maspack.util.*;
import maspack.properties.*;

public class ModelEntry extends MenuNode {

   /**
    * Checks that a class name describes an instance of RootModel
    */
   static class ClassNameRange extends RangeBase {

      public boolean isValid (Object obj, StringHolder errMsg) {
         if (obj == null) {
            return true;
         }
         else if (obj instanceof String) {
            String name = (String)obj;
            if (name.length() == 0) {
               return true;
            }
            Class<?> clazz;
            try {
               clazz = Class.forName (name);
            }
            catch (Exception e) {
               if (errMsg != null) {
                  errMsg.value = "'"+name+"' is not a known class";
               }
               return false;
            }
            if (!RootModel.class.isAssignableFrom (clazz)) {
               if (errMsg != null) {
                  errMsg.value = "'"+name+"' is not a RootModel";
               }
               return false;
            }
            return true;
         }
         return false;
      }
   }

   private ModelScriptInfo myModelInfo =
      new ModelScriptInfo (InfoType.CLASS, "", "", new String[0]);
   private ModelScriptActionForwarder myActionForwarder = null;
 
   public static final String DEFAULT_CLASS_NAME = "";
   public static final String DEFAULT_BUILD_ARGS = "";
   protected String myBuildArgs = new String(DEFAULT_BUILD_ARGS);

   private JMenuItem myMenuItem;

   public static PropertyList myProps =
      new PropertyList (ModelEntry.class, MenuNode.class);

   static {
      myProps.add (
         "className", "Classname for the model class",
         DEFAULT_CLASS_NAME);
      myProps.add (
         "buildArgs", "Arguments for the model build() method",
         DEFAULT_BUILD_ARGS);
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

  public ModelEntry() {
      super();
   }

   public ModelEntry (String classname, String name, String[] args) {
      super (name);
      setModel (new ModelScriptInfo (InfoType.CLASS, classname, name, args));
   }

   public ModelScriptInfo getModel() {
      return myModelInfo;
   }

   public void setModel (ModelScriptInfo model) {
      myModelInfo = model;
   }

   public String getTitle() {
      return myModelInfo.getShortName();
   }

   public void setTitle (String title) {
      if (!title.equals (getTitle())) {
         String[] args = ModelScriptInfo.splitArgs (myBuildArgs);
         myModelInfo =
            new ModelScriptInfo (InfoType.CLASS, getClassName(), title, args);
      }
   }

   public String getClassName() {
      return myModelInfo.getClassNameOrFile();
   }

   public void setClassName (String className) {
      if (!className.equals (getClassName())) {
         String[] args = ModelScriptInfo.splitArgs (myBuildArgs);
         myModelInfo =
            new ModelScriptInfo (InfoType.CLASS, className, getTitle(), args);
      }
   }

   public Range getClassNameRange() {
      return new ClassNameRange ();
   }

   public String getBuildArgs() {
      return myBuildArgs;
   }

   public void setBuildArgs (String buildArgs) {
      if (!buildArgs.equals (myBuildArgs)) {
         String[] args = ModelScriptInfo.splitArgs (buildArgs);
         myModelInfo =
            new ModelScriptInfo (InfoType.CLASS, getClassName(), getTitle(), args);
         myBuildArgs = buildArgs;
      }
   }

   @Override
   public MenuType getType() {
      return MenuType.MODEL;
   }

   @Override
   public boolean equals (MenuNode node) {

      if (! (node instanceof ModelEntry)) {
         return false;
      }
      if (!super.equals (node)) {
         return false;
      }
      ModelEntry other = (ModelEntry)node;
      if (! (myModelInfo.equals (other.myModelInfo))) {
         return false;
      }
      return true;
   }   
   
   @Override
   public String toString() {
      return myModelInfo.toString();
   }

   public void set (ModelEntry modelEntry) {
      super.set (modelEntry);
      setClassName (modelEntry.getClassName());
      setBuildArgs (modelEntry.getBuildArgs());
   }

   public JMenuItem getComponent() {
      return myMenuItem;
   }

   public Component updateComponent (ModelScriptMenu modelMenu) {

      // XXX update demoTable
      if (myMenuItem == null) {
         myMenuItem = new JMenuItem (getTitle());
         myActionForwarder = 
            modelMenu.setModelMenuAction (myMenuItem, myModelInfo);
         setLabelAttributes (myMenuItem);
      }
      else {
         if (myActionForwarder.mi != myModelInfo) {
            // model has changed
            myMenuItem.removeActionListener (myActionForwarder);
            myActionForwarder =
               modelMenu.setModelMenuAction (myMenuItem, myModelInfo);
         }
         updateLabelAttributes (myMenuItem);
      }
      return myMenuItem;   
   }

}
