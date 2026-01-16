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
import maspack.render.Renderer.*;
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
   protected MechModel myModel;
   protected RenderableComponentList<FrameMarker> myMarkerList; // marker dest
   private RigidBody myRigidBody; // if non-null, body to add markers to

   private static HashMap<Class,ModelComponent> myPrototypeMap;
   private static RootModel myLastRootModel = null;

   protected void initializePrototype (
      ModelComponent comp, ComponentList<?> container, Class type) {
      if (type == FrameMarker.class) {
         if (!hasSphericalPointRendering(container)) {
            FrameMarker mkr = (FrameMarker)comp;
            RenderProps.setPointRadius (mkr, getDefaultPointRadius());
            RenderProps.setPointStyle (mkr, PointStyle.SPHERE);
         }
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
            String targetName;
            if (myRigidBody != null) {
               targetName = myRigidBody.getName();
               if (targetName == null) {
                  targetName = "the rigid body";
               }
            }
            else {
               targetName = "a rigid body";
            }
            myInstructionBox.setText ("Pick location on "+targetName);
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
      this (main, model, model.frameMarkers(), /*rigidBody=*/null);
   }

   public FrameMarkerAgent (
      Main main, MechModel model, 
      RenderableComponentList<FrameMarker> markers, RigidBody body) { 
      super (main, (ComponentList<FrameMarker>)markers, model);
      myModel = model;
      myMarkerList = markers;
      myRigidBody = body;
   }

   protected void createDisplay() {
      createDisplayFrame ("Add FrameMarkers");

      addComponentType (FrameMarker.class, new String[] { 
            "position", "velocity", "externalForce", "refPos", 
            "location", "name", "pointDamping", "displacement",
            "targetPosition", "targetVelocity", "targetActivity" });

      createComponentList (
         "Existing frame markers:", new ComponentListWidget<FrameMarker> (
            myMarkerList, myModel));
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

   private Point3d rayIntersectsBody (
      RigidBody body, MouseRayEvent rayEvent, DoubleHolder minDist) {
      if (body.getSurfaceMesh() != null) {
         Point3d isectPoint = BVFeatureQuery.nearestPointAlongRay (
            body.getSurfaceMesh (),
            rayEvent.getRay().getOrigin(), rayEvent.getRay().getDirection());             
         if (isectPoint != null) {
            double d = isectPoint.distance (rayEvent.getRay().getOrigin());
            if (d < minDist.value) {
               minDist.value = d;
               return isectPoint;
            }
         }
      } 
      return null;
   }
   
   public void handleLocationEvent (GLViewer viewer, MouseRayEvent rayEvent) {
      DoubleHolder minDist = new DoubleHolder(Double.POSITIVE_INFINITY);
      RigidBody nearestBody = null;
      Point3d nearestIntersection = null;
      if (myRigidBody != null) {
         // confine search to single body
         Point3d isectPoint = rayIntersectsBody (myRigidBody, rayEvent, minDist);
         if (isectPoint != null) {
            nearestBody = myRigidBody;
            nearestIntersection = isectPoint;
         }
      }
      else {
         // search among all visible rigid bodies
         for (RigidBody body : myModel.rigidBodies()) {
            if (RenderableComponentBase.isVisible (body)) {
               Point3d isectPoint = rayIntersectsBody (body, rayEvent, minDist);
               if (isectPoint != null) {
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
   }

   protected void createAndAddMarker (Point3d pnt, Frame frame) {
      FrameMarker marker = new FrameMarker (pnt);
      marker.setFrame (frame);
      marker.setName (getNameFieldValue());
      setProperties (marker, getPrototypeComponent (myComponentType));
      // update properties in the prototype as well ...
      setProperties (myPrototype, myPrototype);

      addComponent (new AddComponentsCommand (
         "add FrameMarker", marker, myMarkerList));

      setState (State.SelectingLocation);
   }

   protected boolean isContextValid() {
      return (ComponentUtils.withinHierarchy (myModel, myMain.getRootModel()));
   }

}
