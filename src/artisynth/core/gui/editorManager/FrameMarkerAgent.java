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
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

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
import artisynth.core.workspace.RootModel;

/**
 * Responsible for adding FrameMarkers to a MechModel.
 */
public class FrameMarkerAgent extends AddComponentAgent<FrameMarker> {
   private MechModel myModel;

   private static HashMap<Class,ModelComponent> myPrototypeMap;
   private static RootModel myLastRootModel = null;

   protected void initializePrototype (ModelComponent comp, Class type) {
      if (type == FrameMarker.class) {
         FrameMarker mkr = (FrameMarker)comp;
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
            myInstructionBox.setText ("Pick location on a rigid body");
            installLocationListener();
            break;
         }
         default: {
            throw new InternalErrorException ("Unhandled state " + state);
         }
      }
      myState = state;
   }

   protected void resetDefaultProperties() {
      setDefaultProperty ("name", null);
   }

   public FrameMarkerAgent (Main main, MechModel model) {
      super (main, (ComponentList<FrameMarker>)model.frameMarkers(), model);
      myModel = model;
   }

   protected void createDisplay() {
      createDisplayFrame ("Add FrameMarkers");

      addComponentType (FrameMarker.class, new String[] { 
            "position", "velocity", "externalForce", "refPos", 
            "location", "name", "pointDamping", "displacement" });

      createComponentList (
         "Existing frame markers:", new ComponentListWidget<FrameMarker> (
            myModel.frameMarkers(), myModel));
      // createSeparator();
      // createTypeSelector();
      createNameField();
      createPropertyFrame("Default marker properties:");
      // createSeparator();
      // createProgressBox();
      createInstructionBox();
      createOptionPanel ("Done");
   }

   protected HashMap<Class,ModelComponent> getPrototypeMap() {
      RootModel root = myMain.getRootModel();
      if (root != null && root != myLastRootModel) {
         myPrototypeMap = new HashMap<Class,ModelComponent>();
         myLastRootModel = root;
      }
      return myPrototypeMap;
   }

   // void installLocationListener()
   // {
   // super.installLocationListener();
   // myViewerManager.setSelectionEnabled (true);
   // mySelectionManager.setMaximumSelections (1);
   // installSelectionFilter (new BodyFilter());
   // }

   // void uninstallLocationListener()
   // {
   // if (myLocationListener != null)
   // {
   // mySelectionManager.setMaximumSelections (-1);
   // uninstallSelectionFilter();
   // super.uninstallLocationListener();
   // }
   // }

   public void handleLocationEvent (GLViewer viewer, MouseRayEvent rayEvent) {
      double mind = Double.POSITIVE_INFINITY;
      RigidBody nearestBody = null;
      Point3d nearestIntersection = null;
      for (RigidBody body : myModel.rigidBodies()) {
         if (body.getMesh() != null) {
            Point3d isectPoint = BVFeatureQuery.nearestPointAlongRay (
               body.getMesh (),
               rayEvent.getRay().getOrigin(), rayEvent.getRay().getDirection());             
            if (isectPoint != null) {
               double d = isectPoint.distance (rayEvent.getRay().getOrigin());
               if (d < mind) {
                  mind = d;
                  nearestBody = body;
                  nearestIntersection = isectPoint;
               }
            }
         }
      }
      if (nearestBody != null) { // transform to body coordinates
         nearestIntersection.inverseTransform (nearestBody.getPose());
         createAndAddMarker (nearestIntersection, nearestBody);
      }
      // if (myBody.getMesh() != null)
      // {
      // Point3d isectPoint =
      // EditorUtils.intersectWithMesh(myBody.getMesh(), rayEvent);
      // if (isectPoint != null)
      // { createAndAddMarker (isectPoint, myBody);
      // }
      // }
   }

   private void createAndAddMarker (Point3d pnt, Frame frame) {
      FrameMarker marker = new FrameMarker (pnt);
      marker.setFrame (frame);
      marker.setName (getNameFieldValue());
      setProperties (marker, getPrototypeComponent (myComponentType));
      // update properties in the prototype as well ...
      setProperties (myPrototype, myPrototype);

      addComponent (new AddComponentsCommand (
         "add FrameMarker", marker,
         (MutableCompositeComponent<?>)myModel.frameMarkers()));

      setState (State.SelectingLocation);
   }

   protected boolean isContextValid() {
      return (ComponentUtils.withinHierarchy (myModel, myMain.getRootModel()));
   }

}

// class FrameMarkerList extends ComponentListWidget<FrameMarker>
// {
// FrameMarkerList (
// ComponentListView<FrameMarker> list, CompositeComponent ancestor)
// {
// super (list, ancestor);
// }

// @Override
// protected String getName (
// FrameMarker comp, CompositeComponent ancestor)
// {
// Point pointA = comp.getFirstPoint();
// Point pointB = comp.getSecondPoint();
// return
// CompositeUtils.getPathName(ancestor, pointA) + " - " +
// CompositeUtils.getPathName(ancestor, pointB);
// }
// }

