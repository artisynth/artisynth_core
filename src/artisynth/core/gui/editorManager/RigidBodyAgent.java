/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.editorManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;

import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.EditingProperty;
import maspack.properties.HasProperties;
import maspack.properties.HostList;
import maspack.properties.PropTreeCell;
import maspack.properties.Property;
import maspack.render.Dragger3dAdapter;
import maspack.render.Dragger3dEvent;
import maspack.render.MouseRayEvent;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Transrotator3d;
import maspack.render.GL.GLViewer;
import maspack.util.InternalErrorException;
import maspack.widgets.AffineTransformWidget;
import maspack.widgets.AxisAngleField;
import maspack.widgets.BooleanSelector;
import maspack.widgets.DoubleFieldSlider;
import maspack.widgets.EnumSelector;
import maspack.widgets.GuiUtils;
import maspack.widgets.LabeledComponentPanel;
import maspack.widgets.LabeledToggleButton;
import maspack.widgets.PropertyPanel;
import maspack.widgets.ScaleField;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.VectorField;
import artisynth.core.driver.Main;
import artisynth.core.driver.ViewerManager;
import artisynth.core.gui.editorManager.GeometryInertiaPanel.GeometryType;
import artisynth.core.gui.editorManager.GeometryInertiaPanel.GeometrySettings;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.MutableCompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.workspace.RootModel;

public class RigidBodyAgent extends AddComponentAgent<RigidBody> {

   private MechModel myModel;
   private RigidBody body;
   private Transrotator3d rotator;

   private static RootModel myLastRootModel;
   private static HashMap<Class,ModelComponent> myPrototypeMap;
   private static GeometrySettings myGeometrySettings;
   //private static Settings mySettings;
   
   private VectorField positionField;
   private AxisAngleField orientationField;
   private ScaleField scaleField;
   private JCheckBox myUsePlaneToggle;   

   // private HostList basicHostList;
   // private HostList moreHostList; 
   // private PropertyPanel basicPropsPanel;
   // private PropertyPanel morePropsPanel;
   // private PropTreeCell basicPropTree;
   // private LabeledToggleButton togglePropsBtn;
   // private LabeledComponentPanel propPanel;

   private EnumSelector geometrySelector;
   private JButton autoScaleBtn;
   private GeometryInertiaPanel myGeometryPanel;

   private State currentState;
   private boolean fileCheck = true;
   
   // private final String[] EXCLUDED_PROPS = new String[] {
   //    "density", "position", "mass", "inertia", "centerOfMass", 
   //    "orientation", "renderProps", "velocity", "externalForce",
   //    "axisLength"
   // };

   private final Dimension DEFAULT_BUTTON_DIM = new Dimension(100, 26);

   private enum State {
      SelectingLocation, Adding, SelectingFile
   };
   
   // private class Settings {
   //    boolean dynamic;
   //    double frameDamping;
   //    double rotaryDamping;
   // }
   
   // private void setSettings() {
   //    if (mySettings == null) {
   //       mySettings = new Settings();
   //    }
      
   //    mySettings.dynamic = ((BooleanSelector) 
   //       basicPropsPanel.getPropertyWidget ("dynamic")).getBooleanValue();
   //    mySettings.frameDamping = ((DoubleFieldSlider) 
   //       morePropsPanel.getPropertyWidget ("frameDamping")).getDoubleValue();
   //    mySettings.rotaryDamping = ((DoubleFieldSlider) 
   //       morePropsPanel.getPropertyWidget ("rotaryDamping")).getDoubleValue();
   // }

   public RigidBodyAgent (Main main, MechModel model) {
      super (main, (ComponentList<RigidBody>)model.rigidBodies(), model);
      myModel = model;
   }

   protected void setInitialState() {
      setState (State.SelectingLocation);
   }

   protected void resetState() {
      setState (State.SelectingLocation);
   }

   protected void initializePrototype (ModelComponent comp, Class type) {
      if (type == RigidBody.class) {
         RigidBody mkr = (RigidBody)comp;
         RenderProps.setPointRadius (mkr, getDefaultPointRadius());
      }
      else {
         throw new InternalErrorException ("unimplemented type " + type);
      }
   }

   protected HashMap<Class, ModelComponent> getPrototypeMap() {
      RootModel root = myMain.getRootModel();
      if (root != null && root != myLastRootModel) {
         myPrototypeMap = new HashMap<Class, ModelComponent>();
         myLastRootModel = root;
      }
      return myPrototypeMap;
   }
   
   private void setState (State state) {
      if (currentState != state) {
         switch (state) {
            case SelectingLocation: {
               myInstructionBox.setText (
                  "Specify position via the text field or clicking in the viewer");
               myAddButton.setEnabled (false);
               installLocationListener();
               break;
            }
            case Adding: {
               myInstructionBox.setText (
                  "Click 'Add' to finish adding the rigid body");
               myAddButton.setEnabled (true);
               uninstallLocationListener();
               createPreviewBody();
               break;
            }
            case SelectingFile: {
               myInstructionBox.setText ("Specify a valid mesh file");
               myAddButton.setEnabled (false);
               break;
            }
            default: {
               throw new InternalErrorException ("Unhandled state " + state);
            }
         }
         
         currentState = state;
      }
   }
   
   private void createPreviewBody() {
      resetScaling();
      
      body = new RigidBody();
      setProperties (body, getPrototypeComponent (myComponentType));
      setProperties (myPrototype, myPrototype);

      body.setMesh (myGeometryPanel.getMesh(), 
                    myGeometryPanel.getMeshFileName(),
                    myGeometryPanel.getMeshTransform());
      
      RigidTransform3d X = new RigidTransform3d();      
      X.p.set (positionField.getVectorValue());
      X.R.setAxisAngle (orientationField.getAxisAngleValue());
      body.setPose (X);
      
      RenderProps props = body.createRenderProps();
      props.setFaceStyle (Renderer.FaceStyle.NONE);
      props.setDrawEdges (true);
      props.setLineColor (Color.LIGHT_GRAY);
      body.setRenderProps (props);
      myMain.getWorkspace().getViewerManager().addRenderable (body);

      rotator = new Transrotator3d();
      GLViewer viewer = myMain.getMain().getViewer();
      rotator.setDraggerToWorld (X);
      rotator.setSize (
         viewer.distancePerPixel (viewer.getCenter()) * viewer.getScreenWidth() / 6);
      rotator.addListener (new RigidBodyDraggerListener());
      myMain.getWorkspace().getViewerManager().addDragger (rotator);
      
      myGeometryPanel.setAttachedBody (body);
      
      myMain.rerender ();
   }

   private void disposePreviewBody() {
      if (body != null) {
         ViewerManager viewerMan = myMain.getWorkspace().getViewerManager();
         viewerMan.removeRenderable (body);
         viewerMan.removeDragger (rotator);
         
         body = null;
         rotator = null;
         
         myGeometryPanel.setAttachedBody (null);
         
         myMain.rerender();
      }
   }
   
   private void resetPreviewModel() {
      disposePreviewBody();
      
      if (currentState == State.Adding) {
         createPreviewBody();
      }
   }
   
   protected void createDisplay() {
      if (myLastRootModel != myMain.getRootModel()) {
         //mySettings = null;
         myGeometrySettings = null;
      }
      
      createDisplayFrame ("Add RigidBody");

      addComponentType (RigidBody.class, 
         new String[] { "pose", "velocity", "externalForce",
                        "density", "mass", "inertia", "centerOfMass",
                        "position", "orientation", "renderProps",
                        "inertiaMethod"});
      addBasicProps (RigidBody.class, new String[] {"name", "dynamic" });

      createInstructionBox ();
      addWidget (Box.createHorizontalGlue());
      //createGeneralPropPanel();
      createPropertyFrame ("General Properties");
      addWidget (Box.createHorizontalGlue());
      createLocationPanel();
      addWidget (Box.createHorizontalGlue());
      createGeometryPanel();
      
      myUsePlaneToggle = new JCheckBox ("Constrain to plane");
      if (myMain.getWorkspace().getViewerManager().getViewer(0).getNumClipPlanes() > 0) {
         addWidget (myUsePlaneToggle);
      }
      
      createOptionPanel ("Add Clear Cancel");
      myAddButton = myOptionPanel.getButton ("Add");
      myAddButton.setEnabled (false);
      
      myAddButton.setPreferredSize (DEFAULT_BUTTON_DIM);
      myOptionPanel.getButton (
         "Clear").setPreferredSize (DEFAULT_BUTTON_DIM);
      myOptionPanel.getButton (
         "Cancel").setPreferredSize (DEFAULT_BUTTON_DIM);
      
      myDisplay.addWindowListener (new WindowAdapter() {
         public void windowClosed (WindowEvent w) {
            disposePreviewBody();
         }
      });
   }
   
   private void createLocationPanel() {
      positionField = new VectorField ("position", 3);
      positionField.setStretchable (true);
      positionField.addValueChangeListener (this);
      orientationField = 
         new AxisAngleField("orientiation", new AxisAngle());
      orientationField.setStretchable (true);
      orientationField.addValueChangeListener (this);

      LabeledComponentPanel locationPanel = new LabeledComponentPanel();
      locationPanel.addWidget (positionField);
      locationPanel.addWidget (orientationField);
      
      addWidget (locationPanel);
      locationPanel.setBorder (GuiUtils.createTitledPanelBorder ("Location"));
   }
   
   protected void createGeometryPanel() {
      RigidBody protoBody = null;
      int numExistingBodies = myModel.rigidBodies().size();
      if (numExistingBodies > 0) {
         protoBody = myModel.rigidBodies().get (numExistingBodies - 1);
      }
      
      myGeometryPanel = new GeometryInertiaPanel (
         myGeometrySettings, protoBody, /* editing= */false);
      myGeometryPanel.setBorder (
         GuiUtils.createTitledPanelBorder ("Geometry And Inertia"));      

      if (myGeometrySettings == null) {
         GLViewer viewer = myMain.getMain().getViewer();  
         double width = viewer.distancePerPixel (viewer.getCenter()) * 
            viewer.getScreenWidth() / 6;
         
         double boxScale = 1;
         if (width > boxScale) {
            for (int i = -1 ; width > boxScale ; i++) {
               boxScale *= (i % 3 == 0) ? 2.5 : 2.0;
            }
         }
         else {
            for (int i = -1 ; width < boxScale ; i++) {
               boxScale /= (i % 3 == 0) ? 2.5 : 2.0;
            }
         }
         
         myGeometryPanel.myBoxWidthsField.setValue (
            new Vector3d (boxScale, boxScale, boxScale));
      }
      
      geometrySelector = myGeometryPanel.myGeometrySelector;
      
      geometrySelector.addValueChangeListener (this);
      myGeometryPanel.myBoxWidthsField.addValueChangeListener (this);
      myGeometryPanel.myPointRadiusField.addValueChangeListener (this);
      myGeometryPanel.myPointSlicesField.addValueChangeListener (this);
      myGeometryPanel.myMeshFileField.addValueChangeListener (this);
      
      AffineTransformWidget meshXformWidget = 
         myGeometryPanel.myMeshXformWidget;
      meshXformWidget.getTranslationField().addValueChangeListener (this);
      
      myGeometryPanel.myDensityField.setValue (1.0);      

      scaleField = meshXformWidget.getScaleField();
      autoScaleBtn = new JButton ("Auto Scale");
      autoScaleBtn.addActionListener (this);      
      scaleField.addMajorComponent (autoScaleBtn);
      
      addWidget (myGeometryPanel);
   }
   
   private void setPropertyValue (String name, Object value, 
                                  LinkedList<Property> props) {
      for (Property prop : props) {
         if (prop.getName().equals (name)) {
            prop.set (value);
            return;
         }
      }
   }
   
   public void handleLocationEvent (GLViewer viewer, MouseRayEvent rayEvent) {
      Point3d isect = (myUsePlaneToggle.isSelected() &&
                       viewer.getNumClipPlanes() > 0) ?
         intersectClipPlane (rayEvent.getRay(), viewer.getClipPlane (0)) :
         intersectViewPlane (rayEvent.getRay(), getCenter (myModel), viewer);
      
      positionField.setValue (isect);
   }
   
   private void maskAllValueChangeListeners (boolean masked) {
      positionField.maskValueChangeListeners (masked);
      orientationField.maskValueChangeListeners (masked);
   }

   private void updateDisplayGeometry() {
      if (body != null) {
         RigidTransform3d X = new RigidTransform3d();      
         X.p.set (positionField.getVectorValue());
         X.R.setAxisAngle (orientationField.getAxisAngleValue());
         
         body.setPose (X);
         rotator.setDraggerToWorld (X);            
         myMain.rerender();
      }
   }
   
   private void updateScale() {
      resetScaling();
      
      Vector3d minBound = new Vector3d();
      Vector3d maxBound = new Vector3d();
      body.getMesh().getWorldBounds (minBound, maxBound);
      double meshWidth = maxBound.x - minBound.x;
      double meshHeight = maxBound.y - minBound.y;

      GLViewer viewer = myMain.getMain().getViewer();
      double distancePerPixel = viewer.distancePerPixel (viewer.getCenter());
      double width = distancePerPixel * viewer.getScreenWidth() / 4;
      double height = distancePerPixel * viewer.getScreenHeight() / 4;
      
      double meshScale = 1;
      if (meshScale * meshWidth > width || meshScale * meshHeight > height) {
         for (int i = -1; 
              width < meshScale * meshWidth && height < meshScale * meshHeight; 
              i++) {
            meshScale /= (i % 3 == 0) ? 2.5 : 2.0;
         }
      }
      else {
         for (int i = -1; 
              width > meshScale * meshWidth || height > meshScale * meshHeight; 
              i++) {
            double increment = (i % 3 == 0) ? 2.5 : 2.0;
            
            if (width <= meshScale * meshWidth * increment ||
                height <= meshScale * meshHeight * increment) {
               break;
            }
            
            meshScale *= increment;
         }
      }
      
      scaleField.setValue (meshScale);
   }
   
   private void updateState() {
      if (positionField.getValue() == Property.VoidValue) {
         setState (State.SelectingLocation);
      }
      else if (!fileCheck) {
         setState (State.SelectingFile);
      }
      else {
         setState (State.Adding);
      }
   }

   private void resetScaling() {
      scaleField.setValue (1.0);
      myGeometryPanel.resetMesh();
   }
   
   public void dispose() {
      //setSettings();
      myGeometrySettings = myGeometryPanel.getSettings();
      
      super.dispose();
   }
   
   public void valueChange (ValueChangeEvent evt) {
      Object source = evt.getSource();
      
      if (source == positionField) {
         updateState();         
         updateDisplayGeometry();
         
         if (geometrySelector.getValue() == GeometryType.Mesh && body != null) {
            updateScale();
         }
      }
      else if (source == orientationField) {
         updateDisplayGeometry();
      }
      else if (source == geometrySelector) {
         GLViewer viewer = myMain.getMain().getViewer();  
         double width = viewer.distancePerPixel (viewer.getCenter()) * 
            viewer.getScreenWidth() / 6;
         
         if (geometrySelector.getValue() == GeometryType.Mesh) {
            File file =
               new File (myGeometryPanel.myMeshFileField.getStringValue());
            
            if (!file.isFile()) {            
               fileCheck  = false;               
               updateState();
            }

            resetPreviewModel();
            if (body != null) {
               updateScale();
            }
         }
         else {
            fileCheck = true;
            updateState();
            
            double scale = 1;
            if (width > scale) {
               for (int i = -1 ; width > scale ; i++) {
                  scale *= (i % 3 == 0) ? 2.5 : 2.0;
               }
            }
            else {
               for (int i = -1 ; width < scale ; i++) {
                  scale /= (i % 3 == 0) ? 2.5 : 2.0;
               }
            }
            
            if (geometrySelector.getValue() == GeometryType.Box) {
               myGeometryPanel.myBoxWidthsField.setValue (
                  new Vector3d (scale, scale, scale));
            }
            else if (geometrySelector.getValue() == GeometryType.Sphere) {
               myGeometryPanel.myPointRadiusField.setValue (scale / 2);
            }
            
            resetPreviewModel();
            
            myMain.rerender();
         }
      }
      else if (source == myGeometryPanel.myBoxWidthsField ||
               source == myGeometryPanel.myPointRadiusField ||
               source == myGeometryPanel.myPointSlicesField ||
               source == myGeometryPanel.myMeshXformWidget.getTranslationField()){
         resetPreviewModel();
         
         myMain.rerender();
      }
      else if (source == myGeometryPanel.myMeshFileField) {
         File file = new File (myGeometryPanel.myMeshFileField.getStringValue());
         
         if (file.isFile()) {            
            fileCheck  = true;
            updateState();
            
            resetPreviewModel();
            
            if (body != null) {
               updateScale();
            }
         }
         else {
            fileCheck = false;
            
            GuiUtils.showError (
               myDisplay, "File does not exist:" + file.getAbsolutePath()); 
         }
      }
      else {
         super.valueChange (evt);
      }
   }
   
   public void actionPerformed (ActionEvent evt) {
      String cmd = evt.getActionCommand();
      
      if (cmd.equals ("Add")) {
         ViewerManager viewerMan = myMain.getWorkspace().getViewerManager();
         viewerMan.removeRenderable (body);
         viewerMan.removeDragger (rotator);
         rotator = null;
         
         setProperties (body, getPrototypeComponent (myComponentType));
         // basicPropTree.setTreeValuesInHost (
         //    body, getPrototypeComponent (myComponentType));
         // myPropTree.setTreeValuesInHost (
         //    body, getPrototypeComponent (myComponentType));
         setProperties (myPrototype, myPrototype);

         myGeometryPanel.setBodyInertia (body);
         body.setRenderProps (body.createRenderProps());
         addComponent (new AddComponentsCommand (
            "add RigidBody", body, (MutableCompositeComponent<?>)myModel.rigidBodies()));
         
         myDisplay.setVisible (false);
         dispose();         
      }
      else if (cmd.equals ("Clear")) {
         maskAllValueChangeListeners (true);
         
         positionField.setVoidValueEnabled (true);
         positionField.setValue (Property.VoidValue);
         positionField.setVoidValueEnabled (false);
         
         resetScaling();
         myGeometryPanel.restoreDefaultValues();

         myHostList.restoreBackupValues();
         myPropertyPanel.updateWidgetValues();
         
         // boolean setDefaults = myPropTree.setSingleEditDefaultValues();
         // resetDefaultProperties();
         // if (setDefaults && myPropertyPanel != null) {
         //    myPropertyPanel.updateWidgetValues(/* updateFromSource= */false);
         // }


         // moreHostList.restoreBackupValues();
         // basicHostList.restoreBackupValues ();
         // basicPropsPanel.updateWidgetValues();
         // morePropsPanel.updateWidgetValues();
         
         disposePreviewBody();

         if (geometrySelector.getValue() == GeometryType.Mesh) {
            fileCheck = false;
         }
         
         setState (State.SelectingLocation);
         maskAllValueChangeListeners (false);
      } 
      else if (cmd.equals ("Cancel")) {
         disposePreviewBody();
         myDisplay.setVisible (false);
         dispose();
      }
      else if (evt.getSource() == autoScaleBtn) {
         if (body != null) {
            updateScale();
         }
      }
      else {
         super.actionPerformed (evt);
      }
   }

   protected void resetDefaultProperties() {
      setDefaultProperty ("name", null);
   }

   private class RigidBodyDraggerListener extends Dragger3dAdapter {
      public void draggerMove (Dragger3dEvent e) {
         Transrotator3d rotator = (Transrotator3d) e.getSource();
         RigidTransform3d X = rotator.getDraggerToWorld();         
         body.setPose (X);
         
         positionField.maskValueChangeListeners (true);
         positionField.setValue (X.p);
         positionField.maskValueChangeListeners (false);
         
         orientationField.maskValueChangeListeners (true);
         orientationField.setValue (X.R.getAxisAngle());
         orientationField.maskValueChangeListeners (false);
         
         myMain.rerender();
      } 
   }
}
