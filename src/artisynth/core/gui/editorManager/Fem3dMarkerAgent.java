/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Font;
import java.awt.Cursor;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;

import maspack.render.*;
import maspack.render.GL.GLViewer;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.AmiraLandmarkReader;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.AmiraLandmarkReader.AmiraLandmarkFileFilter;
import artisynth.core.workspace.RootModel;
import artisynth.core.femmodels.*;

/**
 * Responsible for adding FemMarkers to a FemModel3d
 */
public class Fem3dMarkerAgent extends AddComponentAgent<FemMarker> {
   private FemModel3d myModel;

   private static HashMap<Class,ModelComponent> myPrototypeMap;
   private static RootModel myLastRootModel = null;

   private JCheckBox myUsePlaneToggle;

   protected void initializePrototype (ModelComponent comp, Class type) {
      if (type == FemMarker.class) {
         FemMarker mkr = (FemMarker)comp;
         RenderProps.setPointRadius (mkr, getDefaultPointRadius());
      }
      else {
         throw new InternalErrorException ("unimplemented type " + type);
      }
   }

   protected void setInitialState() {
      setState (State.SelectingLocation);
   }

   protected void resetState() {
      setState (State.SelectingLocation);
   }

   private enum State {
      SelectingLocation,
   };

   private State myState = State.SelectingLocation;

   private void setState (State state) {
      switch (state) {
         case SelectingLocation: {
            myInstructionBox.setText ("Pick location in viewer");
            installLocationListener();
            break;
         }
         default: {
            throw new InternalErrorException ("Unhandled state " + state);
         }
      }
      myState = state;
   }

   public Fem3dMarkerAgent (Main main, FemModel3d model) {
      super (main, (ComponentList<FemMarker>)model.markers(), model);
      myModel = model;
   }

   protected void createDisplay() {
      createDisplayFrame ("Add FemMarkers");

      addComponentType (
         FemMarker.class, new String[] {
            "name", "pointDamping", "position", "velocity", "externalForce" });

      createComponentList (
         "Existing markers:",
         new ComponentListWidget<FemMarker> (myModel.markers(), myModel));
      createNameField();
      // createSeparator();
      // createTypeSelector();
      createPropertyFrame("Default marker properties:");
      // createSeparator();
      // createProgressBox();
      createInstructionBox();
      myUsePlaneToggle = new JCheckBox ("constrain to plane");
      addWidget (myUsePlaneToggle);
      createOptionPanel ("Add_Amira_Landmarks Done");
   }

   protected HashMap<Class,ModelComponent> getPrototypeMap() {
      RootModel root = myMain.getRootModel();
      if (root != null && root != myLastRootModel) {
         myPrototypeMap = new HashMap<Class,ModelComponent>();
         myLastRootModel = root;
      }
      return myPrototypeMap;
   }

   /**
    * Trys to find an intersection point between the ray and the fem model. If a
    * point is found, then a marker is inserted there.
    */
   public void handleLocationEvent (GLViewer viewer, MouseRayEvent rayEvent) {
      Point3d isect;

      if (myUsePlaneToggle.isSelected() && viewer.getNumClipPlanes() > 0) {
         isect =
            intersectClipPlane (rayEvent.getRay(), viewer.getClipPlane (0));
      }
      else {
         isect = BVFeatureQuery.nearestPointAlongRay (
            myModel.getSurfaceMesh(), 
            rayEvent.getRay().getOrigin(), rayEvent.getRay().getDirection()); 
      }
      if (isect != null) {
         createAndAddMarker (isect, myModel);
      }
   }

   private void createAndAddMarker (Point3d pnt, FemModel3d femModel) {
      FemMarker marker = new FemMarker();

      // add the marker to the model
      Point3d newLoc = new Point3d();
      FemElement3dBase elem = femModel.findNearestElement (newLoc, pnt);
      pnt.set (newLoc);

      marker.setPosition (pnt);
      marker.setFromElement (elem);
      marker.setName (getNameFieldValue());

      setProperties (marker, getPrototypeComponent (myComponentType));
      // update properties in the prototype as well ...
      setProperties (myPrototype, myPrototype);

      addComponent (new AddComponentsCommand (
         "add FemMarker", marker, (MutableCompositeComponent<?>)myModel.markers()));

      setState (State.SelectingLocation);
   }

   protected boolean isContextValid() {
      return (ComponentUtils.withinHierarchy (myModel, myMain.getRootModel()));
   }
   
   public void actionPerformed (ActionEvent e) {
      if (e.getActionCommand ().compareTo ("Add_Amira_Landmarks")==0) {
         JFileChooser chooser = new JFileChooser ();
         chooser.setCurrentDirectory (ArtisynthPath.getWorkingDir());
         chooser.setAcceptAllFileFilterUsed (false);
         chooser.setFileFilter (new AmiraLandmarkFileFilter ());
         chooser.addChoosableFileFilter (new AmiraLandmarkFileFilter());
         int returnVal = chooser.showOpenDialog (myDisplay);
         if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            try {
                Point3d[] pts = AmiraLandmarkReader.read (path);
                for (int i = 0; i < pts.length; i++)
                   createAndAddMarker (pts[i], myModel);
            }
            catch (IOException ex) {
               ex.printStackTrace ();
            }
         }
      }
      else {
         super.actionPerformed (e);
      }
   }
   
   
}

// class FemMarkerList extends ComponentListWidget<FemMarker>
// {
// FemMarkerList (
// ComponentListView<FemMarker> list, CompositeComponent ancestor)
// {
// super (list, ancestor);
// }

// @Override
// protected String getName (
// FemMarker comp, CompositeComponent ancestor)
// {
// Point pointA = comp.getFirstPoint();
// Point pointB = comp.getSecondPoint();
// return
// CompositeUtils.getPathName(ancestor, pointA) + " - " +
// CompositeUtils.getPathName(ancestor, pointB);
// }
// }

