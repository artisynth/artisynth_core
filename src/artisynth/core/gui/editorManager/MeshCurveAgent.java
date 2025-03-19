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
import maspack.widgets.*;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.renderables.MeshCurve;
import artisynth.core.renderables.MeshMarker;
import artisynth.core.workspace.RootModel;

/**
 * Responsible for adding MeshMarkers to a MeshCurve.
 */
public class MeshCurveAgent extends AddComponentAgent<MeshMarker> {
   protected MeshCurve myCurve;
   protected RenderableComponentList<MeshMarker> myMarkerList; // marker dest
   protected RenderableComponentList<? extends RenderableComponent> myCurveParent; 
   protected MeshComponent myMeshComp; // mesh component containing the curve
   private boolean myRemoveCurveIfEmpty; // remove curve on dispose if no markers

   private static HashMap<Class,ModelComponent> myPrototypeMap;
   private static RootModel myLastRootModel = null;

   /**
    * Controls marker editing actions in the GUI.
    */
   private enum MarkerAction {
      /**
       * Appends a new marker at the end of the curve
       */
      APPEND, 

      /**
       * Inserts a new marker between the nearest two existing markers
       */
      INSERT,
      
      /**
       * Select markers
       */
      SELECT
   };

   private static MarkerAction myDefaultMarkerAction = MarkerAction.APPEND;

   private double getDefaultMarkerRadius() {
      return getDefaultPointRadius()/2;
   }

   private double getDefaultCurveRadius() {
      return getDefaultMarkerRadius()/2;
   }

   EnumSelector myMarkerActionSelector;

   /**
    * Not used because we don't use the default property panel here.
    */
   protected void initializePrototype (
      ModelComponent comp, ComponentList<?> container, Class type) {
   }

   protected void createMarkerActionSelector() {
      myMarkerActionSelector =
         new EnumSelector (
            "marker viewer action:", 
            myDefaultMarkerAction, MarkerAction.values());
      myMarkerActionSelector.addValueChangeListener (this);
      addWidget (myMarkerActionSelector);
   }

   public void initializeProperties (MeshCurve curve) {
      ComponentList<MeshCurve> curves = myMeshComp.getCurves();
      if (curves.size() > 1) {
         MeshCurve prev = curves.get (curves.size()-2);
         // copy explicit properties from prev to curve
         copyExplicitPropertyValues (
            curve.getRenderProps(), prev.getRenderProps());
         copyPropertyValues (
            curve, prev, new String[] {
               "resolution", "normalComputeRadius", "normalLength"});
      }
      else {
         // maybe set point and line properties to ensure we can see the curve
         RenderProps props = curve.getRenderProps();

         if (props.getPointStyle() != PointStyle.SPHERE) {
            RenderProps.setPointStyle (curve, PointStyle.SPHERE);
         }
         if (props.getLineStyle() != LineStyle.CYLINDER) {
            RenderProps.setLineStyle (curve, LineStyle.CYLINDER);
         }
         double defaultRadius = getDefaultMarkerRadius();
         double pr = props.getPointRadius();
         // make sure point radius is not too large or small
         if (pr < defaultRadius/5 || pr > defaultRadius*10) {
            pr = defaultRadius;
            RenderProps.setPointRadius (curve, pr);
         }
         // make sure line radius <= point radius and also not too small
         double lr = props.getLineRadius();
         if (lr < pr/10 || lr > pr) {
            RenderProps.setLineRadius (curve, pr/2);
         }
      }
   }

   protected void setInitialState() {
      setStateForMarkerAction (myDefaultMarkerAction);
   }

   protected void resetState() {
      setStateForMarkerAction (myDefaultMarkerAction);
   }

   private void setStateForMarkerAction (MarkerAction action) {
      switch (action) {
         case INSERT:
         case APPEND: {
            String targetName;
            targetName = myMeshComp.getName();
            if (targetName == null) {
               targetName = "the mesh";
            }
            myInstructionBox.setText ("Pick location on "+targetName);
            installLocationListener();
            break;
         }
         case SELECT: {
            myInstructionBox.setText ("Select markers in the viewer");
            uninstallLocationListener();
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented marker action " + action);
         }
      }
   }

   protected void resetDefaultProperties() {
      //setDefaultProperty ("name", null);
   }

   public MeshCurveAgent (
      Main main, MeshCurve curve, MeshComponent meshComp, 
      boolean removeIfEmpty) {
      this (main, curve, curve.getMarkers(), meshComp, removeIfEmpty);
   }

   public MeshCurveAgent (
      Main main, MeshCurve curve, 
      RenderableComponentList<MeshMarker> markers,
      MeshComponent meshComp, boolean removeIfEmpty) {
      super (main, (ComponentList<MeshMarker>)markers, curve);
      if (curve.getMeshComp() != meshComp) {
         throw new IllegalArgumentException (
            "Mesh curve not associated with body");
      }     
      myCurve = curve;
      myMarkerList = markers;
      myMeshComp = meshComp;
      myRemoveCurveIfEmpty = removeIfEmpty;
   }

   public void dispose() {
      // if we added the curve to the body, remove it if no markers were added
      if (myRemoveCurveIfEmpty && myCurve.numMarkers() == 0) {
         CompositeComponent parent = myCurve.getParent();
         if (parent instanceof ComponentList) { // paranoid
            ((ComponentList)parent).remove (myCurve);
         }
      }
      super.dispose();
   }

   protected void createDisplay() {
      createDisplayFrame ("Add Markers");

      addComponentType (MeshCurve.class, new String[] { 
            "navpanelVisibility" });

      createComponentList (
         "Existing markers:", new ComponentListWidget<MeshMarker> (
            myMarkerList, myCurve));

      createMarkerActionSelector();
      addCurvePropertyPanel();
      createInstructionBox();
      createOptionPanel ("Done");
      mySelectionManager.clearSelections();

      setComponentType (MeshCurve.class);
   }

   protected HashMap<Class,ModelComponent> getPrototypeMap() {
      RootModel root = myMain.getRootModel();
      if (root != null && root != myLastRootModel) {
         myPrototypeMap = new HashMap<Class,ModelComponent>();
         myLastRootModel = root;
      }
      return myPrototypeMap;
   }

   private Point3d rayIntersectsMesh (
      MeshComponent mcomp, MouseRayEvent rayEvent, DoubleHolder minDist) {
      if (mcomp.getMesh() != null && mcomp.getMesh() instanceof PolygonalMesh) {
         Point3d isectPoint = BVFeatureQuery.nearestPointAlongRay (
            (PolygonalMesh)mcomp.getMesh(),
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
      Point3d nearestIntersection = null;
      Point3d isectPoint = rayIntersectsMesh (myMeshComp, rayEvent, minDist);
      if (isectPoint != null) {
         createAndAddMarker (isectPoint, myMeshComp);
      }
   }

   protected void addCurvePropertyPanel() {
      myPropertyPanel = new PropertyPanel ();

      myPropertyPanel.setBorder (
         GuiUtils.createTitledPanelBorder ("curve properties"));

      myPropertyPanel.addWidget (myCurve, "name");
      myPropertyPanel.addWidget (myCurve, "interpolation");
      myPropertyPanel.addWidget (myCurve, "resolution");
      myPropertyPanel.addWidget (myCurve, "normalComputeRadius");
      myPropertyPanel.addWidget (myCurve, "closed");
      myPropertyPanel.addWidget (myCurve, "normalLength");
      myPropertyPanel.addWidget (myCurve, "projectToMesh");

      RenderProps props = myCurve.getRenderProps();
      myPropertyPanel.addWidget (new JSeparator());
      JLabel label = new JLabel ("render properties");
      GuiUtils.setItalicFont (label);
      myPropertyPanel.addWidget (label);
      myPropertyPanel.addWidget (props, "pointColor");
      myPropertyPanel.addWidget (props, "pointStyle");
      myPropertyPanel.addWidget (props, "pointRadius");
      myPropertyPanel.addWidget (props, "pointSize");
      myPropertyPanel.addWidget (props, "lineColor");      
      myPropertyPanel.addWidget (props, "lineStyle");      
      myPropertyPanel.addWidget (props, "lineRadius");
      myPropertyPanel.addWidget (props, "lineWidth");

      myPropertyPanel.updateWidgetValues(/* updateFromSource= */false);
      myPropertyPanel.setAlignmentX (Component.LEFT_ALIGNMENT);
      myPropertyPanel.addGlobalValueChangeListener (this);
      myContentPane.addWidget (myPropertyPanel);
   }

   public void valueChange (ValueChangeEvent e) {
      myMain.rerender();
      myPropertyPanel.updateWidgetValues();
      if (e.getSource() == myMarkerActionSelector) {
         MarkerAction action = (MarkerAction)myMarkerActionSelector.getValue();
         if (action != myDefaultMarkerAction) {
            setStateForMarkerAction (action);
            myDefaultMarkerAction = action;
         }
      }
   }

   protected void createAndAddMarker (Point3d pnt, MeshComponent mcomp) {
      MeshMarker marker = new MeshMarker (mcomp, pnt);

      MarkerAction action = (MarkerAction)myMarkerActionSelector.getValue();
      if (action == MarkerAction.INSERT) {
         // pick the insertion index based on the nearest segment to the point
         int idx = (myCurve.findNearestMarkerInterval(pnt) + 1);
         System.out.println ("idx=" + idx);
         addComponent (new AddComponentsCommand (
                          "add MeshMarker", marker, idx, myMarkerList));
      }
      else {
         addComponent (new AddComponentsCommand (
                          "add MeshMarker", marker, myMarkerList));
      }
      //setState (State.SelectingLocation);
   }

   protected boolean isContextValid() {
      return (ComponentUtils.withinHierarchy (myCurve, myMain.getRootModel()));
   }

}
