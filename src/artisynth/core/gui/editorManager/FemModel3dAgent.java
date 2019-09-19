/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import maspack.geometry.PolygonalMesh;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorBase;
import maspack.matrix.VectorNd;
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
import maspack.widgets.AxisAngleField;
import maspack.widgets.DoubleField;
import maspack.widgets.EnumSelector;
import maspack.widgets.FileNameField;
import maspack.widgets.GuiUtils;
import maspack.widgets.IntegerField;
import maspack.widgets.IntegerMultiField;
import maspack.widgets.IntegerSelector;
import maspack.widgets.LabeledComponentPanel;
import maspack.widgets.LabeledControl;
import maspack.widgets.PropertyPanel;
import maspack.widgets.ScaleField;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.VectorField;
import artisynth.core.driver.Main;
import artisynth.core.driver.ViewerManager;
import artisynth.core.femmodels.AnsysReader;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemFactory.FemElementType;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.TetGenReader;
import artisynth.core.femmodels.UCDReader;
import artisynth.core.gui.widgets.*;
import artisynth.core.materials.FemMaterial;
import artisynth.core.mechmodels.MechSystemModel;
import artisynth.core.modelbase.*;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.RootModel;

public class FemModel3dAgent extends AddComponentAgent<FemModel3d> {
   private static RootModel myLastRootModel = null;
   private static HashMap<Class, ModelComponent> myPrototypeMap;
   private static Settings mySettings;

   private FemModel3d fem;
   private Transrotator3d rotator;

   // private HostList basicHostList;
   // private HostList moreHostList;
   // private PropertyPanel basicPropsPanel;
   // private PropertyPanel morePropsPanel;
   // private PropTreeCell basicPropTree;
   // private LabeledControl togglePropsBtn;
   // private LabeledComponentPanel propPanel;

   private VectorField positionField;
   private AxisAngleField orientationField;
   private ScaleField scaleField;
   private LabeledComponentPanel geometryPanel;

   private EnumSelector meshSelector;
   private VectorField gridDimField;
   private IntegerMultiField gridDivField;
   private VectorField tubeDimField;
   private IntegerMultiField tubeDivField;
   private VectorField torusDimField;
   private IntegerMultiField torusDivField;
   private IntegerSelector sphereNodesField;
   private DoubleField extrusDepthField;
   private IntegerField extrusLayersField;
   private FileNameField extrusFileField;
   private FileNameField ansysNodeFileField;
   private FileNameField ansysElemFileField;
   private FileNameField tetgenNodeFileField;
   private FileNameField tetgenEleFileField;
   private FileNameField ucdMeshFileField;
   private FileNameField surfaceMeshFileField;
   private EnumSelector elemSelector;
   private JButton autoScaleBtn;
   private LabeledComponentPanel meshPropPanel;

   private JCheckBox myUsePlaneToggle;

   private State currentState;
   private Object lastScale = 1.0;
   private boolean fileCheck = true;
   
   private enum FemMeshType {
      Grid, Tube, Torus, Sphere,
      Extrusion,
         SurfaceMesh, TetgenMesh, AnsysMesh, UCDMesh
   }
   
   private enum State {
      SelectingLocation, Adding, SelectingFile
   };
   
   private final String[] EXCLUDED_PROPS = new String[] {
      "maxStepSize", "renderProps", "stressPlotRanging", 
      "stressPlotRange", "gravity", "warping", "YoungsModulus", 
      "PoissonsRatio", "profile", "integrator", "matrixSolver", 
      "elementWidgetSize"
   };
   
   private final Vector3d DEFAULT_GRID_DIM = new Vector3d (1, 1, 1);
   private final int[] DEFAULT_GRID_DIV = {3, 3, 3};
   private final Vector3d DEFAULT_TUBE_DIM = new Vector3d (2, 0.5, 1);
   private final int[] DEFAULT_TUBE_DIV = {6, 3, 2};
   private final Vector3d DEFAULT_TORUS_DIM = new Vector3d (4, 0.5, 1);
   private final int[] DEFAULT_TORUS_DIV = {6, 12, 2};
   private final double DEFAULT_EXTRUSION_DEPTH = 1.0;
   private final int DEFAULT_EXTRUSION_LAYERS = 1;
   private final Dimension DEFAULT_BUTTON_DIM = new Dimension(100, 26);
   private final int[] SPHERE_NODE_OPTIONS = {54, 196};
   private final String SPHERE_54_MESH_PATH =
      "src/artisynth/core/femmodels/meshes/sphere2.1";
   private final String SPHERE_196_MESH_PATH =
      "src/artisynth/core/femmodels/meshes/sphere3.1";

   public FemModel3dAgent (Main main, 
      ComponentList<MechSystemModel> list, CompositeComponent ancestor) {
      
      super (main, list, ancestor);
      myAncestor = ancestor;
   }
   
   private class Settings {
      double density;
      SurfaceRender surfaceRendering;
      double particleDamping;
      double stiffnessDamping;
      FemMaterial material;
      boolean incompressible;
      
      FemMeshType meshType;
      VectorNd gridWidths;
      int[] gridDivisions;
      VectorNd tubeWidths;
      int[] tubeDivisions;
      VectorNd torusWidths;
      int[] torusDivisions;
      int sphereNodes;
      double extrusionDepth;
      int extrusionLayers;
      String extrusionFile;
      String ansysNodeFile;
      String ansysElemFile;
      String tetgenNodeFile;
      String tetgenEleFile;
      String ucdFile;
      String surfaceMeshFile;
      FemElementType elemType;
   }
   
   private void setSettings() {
      if (mySettings == null) {
         mySettings = new Settings();
      }

      mySettings.meshType = (FemMeshType) meshSelector.getValue();
      mySettings.gridWidths = gridDimField.getVectorValue();
      mySettings.gridDivisions = gridDivField.getVectorValue();
      mySettings.tubeWidths = tubeDimField.getVectorValue();
      mySettings.tubeDivisions = tubeDivField.getVectorValue();
      mySettings.torusWidths = torusDimField.getVectorValue();
      mySettings.torusDivisions = torusDivField.getVectorValue();
      mySettings.sphereNodes = (Integer) sphereNodesField.getValue();
      mySettings.extrusionDepth = extrusDepthField.getDoubleValue();
      mySettings.extrusionLayers = extrusLayersField.getIntValue();
      mySettings.extrusionFile = extrusFileField.getStringValue();
      mySettings.ansysNodeFile = ansysNodeFileField.getStringValue();
      mySettings.ansysElemFile = ansysElemFileField.getStringValue();
      mySettings.tetgenNodeFile = tetgenNodeFileField.getStringValue();
      mySettings.tetgenEleFile = tetgenEleFileField.getStringValue();
      mySettings.ucdFile = ucdMeshFileField.getStringValue();
      mySettings.surfaceMeshFile = surfaceMeshFileField.getStringValue();

      mySettings.elemType = (FemElementType) (elemSelector.isEnabledAll() ? 
         elemSelector.getValue() : null);
   }

   protected void setInitialState() {
      setState (State.SelectingLocation);
   }

   protected void resetState() {
      setState (State.SelectingLocation);
   }

   protected void initializePrototype (ModelComponent comp, Class type) {
      if (type == FemModel3d.class) {
         FemModel3d mkr = (FemModel3d) comp;
         RenderProps.setPointRadius (mkr, getDefaultPointRadius());
      }
      else {
         throw new InternalErrorException ("Unimplemented type " + type);
      }
   }

   protected Map<Class, ModelComponent> getPrototypeMap () {
      RootModel root = myMain.getRootModel();
      if (root != null && root != myLastRootModel) {
         myPrototypeMap = new HashMap<Class, ModelComponent>();
         myLastRootModel = root;
      }
      
      return myPrototypeMap;
   }
   
   private void setState (State state) {
      if (state != currentState) {
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
                  "Click 'Add' to finish adding FEM model");
               myAddButton.setEnabled (true);
               uninstallLocationListener();
               createPreviewModel();
               break;
            }
            case SelectingFile: {
               myInstructionBox.setText (
                  "Specify a valid mesh file (or node/elem file pair)");
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

   private void createPreviewModel() {
      fem = new FemModel3d();
      setProperties (fem, getPrototypeComponent (myComponentType));
      setProperties (myPrototype, myPrototype);
      
      FemElementType elemType = null; 
      FemMeshType meshType = (FemMeshType) meshSelector.getValue();
      if (elemSelector.isEnabledAll()) {
         elemType = (FemElementType) elemSelector.getValue();
      }

      switch (meshType) {
         case Grid: {
            VectorBase dims = gridDimField.getVectorValue();
            int[] divs = gridDivField.getVectorValue();
            
            FemFactory.createGrid (
               fem, elemType, dims.get (0), dims.get (1), dims.get (2),
               divs[0], divs[1], divs[2]);
            break;
         }
         case Tube: {
            VectorBase dims = tubeDimField.getVectorValue();
            int[] divs = tubeDivField.getVectorValue();
            
            FemFactory.createTube (
               fem, elemType, dims.get (0), dims.get (1), dims.get (2),
               divs[0], divs[1], divs[2]);
            break;
         }
         case Torus: {
            VectorBase dims = torusDimField.getVectorValue();
            int[] divs = torusDivField.getVectorValue();
            
            FemFactory.createTorus (
               fem, elemType, dims.get (0), dims.get (1), dims.get (2),
               divs[0], divs[1], divs[2]);
            break;
         }
         case Sphere: {
            int nodes = (Integer) sphereNodesField.getValue();
            String meshPath;
            
            if (nodes == SPHERE_NODE_OPTIONS[0]) {
               meshPath = ArtisynthPath.getHomeRelativePath (
                  SPHERE_54_MESH_PATH, ".");
            }
            else if (nodes == SPHERE_NODE_OPTIONS[1]) {
               meshPath = ArtisynthPath.getHomeRelativePath (
                  SPHERE_196_MESH_PATH, ".");
            }
            else {
               GuiUtils.showError (
                  myDisplay, "Invalid number of nodes for sphere");
               return;
            }
            
            try {
               TetGenReader.read (fem, 1000, meshPath + ".node",
                  meshPath + ".ele", new Vector3d (1, 1, 1));
            }
            catch (Exception e) {
               GuiUtils.showError (
                  myDisplay, "Error reading file: " + e.getMessage());
               return;
            }
            
            break;
         }
         case Extrusion: {
            double d = extrusDepthField.getDoubleValue();
            int n = extrusLayersField.getIntValue();
            String meshFileName = extrusFileField.getStringValue();
            
            try {
               PolygonalMesh mesh = new PolygonalMesh (new File (meshFileName));
               FemFactory.createExtrusion (fem, elemType, n, d, 0, mesh);
            }
            catch (Exception e) {
               GuiUtils.showError (
                  myDisplay, "Error reading file: " + e.getMessage());
               return;
            }
            
            break;
         }
         case AnsysMesh: {
            String nodeFileName = ansysNodeFileField.getStringValue();
            String elemFileName = ansysElemFileField.getStringValue();
            
            try {
               AnsysReader.read (fem, nodeFileName, elemFileName, 
                  1000, null, /*options=*/0);
            }
            catch (Exception e) {
               GuiUtils.showError (
                  myDisplay, "Error reading file: " + e.getMessage());
               return;
            }
            
            break;
         }
         case TetgenMesh: {
            String nodeFileName = tetgenNodeFileField.getStringValue();
            String eleFileName = tetgenEleFileField.getStringValue();
            
            try {
               TetGenReader.read (fem, 1000, nodeFileName, eleFileName, 
                  new Vector3d (1, 1, 1));
            }
            catch (Exception e) {
               GuiUtils.showError (
                  myDisplay, "Error reading file: " + e.getMessage());
               return;
            }
            
            break;
         }
         case UCDMesh: {
            String ucdFileName = ucdMeshFileField.getStringValue();
            
            try {
               UCDReader.read (fem, ucdFileName, 1000);
            }
            catch (Exception e) {
               GuiUtils.showError (
                  myDisplay, "Error reading file: " + e.getMessage());
               return;
            }
            
            break;
         }
         case SurfaceMesh: {
            String objFileName = surfaceMeshFileField.getStringValue();
            
            PolygonalMesh surfaceMesh = null;

            try {
               surfaceMesh = new PolygonalMesh (new File(objFileName));
            }
            catch (Exception e) {
               GuiUtils.showError (
                  myDisplay, "Error reading file: " + e.getMessage());
               return;
            }
            try {
               FemFactory.createFromMesh (fem, surfaceMesh, /*quality=*/2.0); 
            }
            catch (Exception e) {
               e.printStackTrace(); 
               GuiUtils.showError (
                  myDisplay, "Error tessellating mesh: " + e.getMessage());
               return;
            }
            break;
         }
         default: {
            throw new InternalErrorException ("Unimplemented mesh type");
         }
      }
      
      RigidTransform3d X = new RigidTransform3d();      
      X.p.set (positionField.getVectorValue());
      X.R.setAxisAngle (orientationField.getAxisAngleValue());
      
      PolygonalMesh mesh = fem.getSurfaceMesh();      
      RenderProps props = mesh.createRenderProps();
      props.setFaceStyle (Renderer.FaceStyle.NONE);
      props.setDrawEdges (true);
      props.setLineColor (Color.LIGHT_GRAY);
      mesh.setRenderProps (props);
      mesh.setMeshToWorld (X);
      mesh.setFixed (false);
      mesh.setRenderBuffered (false);
      
      if (meshPropPanel.getComponentIndex (scaleField) != -1) {
         scaleField.maskValueChangeListeners (true);
         scaleField.setValue (1.0);
         scaleField.maskValueChangeListeners (false);
         lastScale = 1.0;
      }
      
      myMain.getWorkspace().getViewerManager().addRenderable (mesh);
      
      rotator = new Transrotator3d();
      GLViewer viewer = myMain.getMain().getViewer();
      rotator.setDraggerToWorld (X);
      rotator.setSize (
         viewer.distancePerPixel (viewer.getCenter()) * viewer.getScreenWidth() / 6);
      rotator.addListener (new FemModelDraggerListener());
      myMain.getWorkspace().getViewerManager().addDragger (rotator);
      
      myMain.rerender ();
   }

   protected void createDisplay() {
      if (myLastRootModel != myMain.getRootModel()) {
         mySettings = null;
      }
      
      createDisplayFrame ("Add FemModel");

      addComponentType (FemModel3d.class, EXCLUDED_PROPS);
      addBasicProps (FemModel3d.class, new String[] {
            "name", "density" });

      createInstructionBox ();
      addWidget (Box.createHorizontalGlue());
      //createGeneralPropPanel();
      createPropertyFrame ("General Properties");
      addWidget (Box.createHorizontalGlue());
      createLocationPanel();
      addWidget (Box.createHorizontalGlue());
      createGeometryPanel();
      
      myUsePlaneToggle = new JCheckBox ("constrain to plane");
      if (myMain.getMain().getViewer().getNumClipPlanes() > 0) {
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
            disposePreviewModel();
         }
      });
      
      updateState();
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

   private void createGeometryPanel() {
      geometryPanel = new LabeledComponentPanel();
      geometryPanel.setLayout (
         new BoxLayout(geometryPanel, BoxLayout.Y_AXIS));
      
      meshSelector = new EnumSelector (
         "mesh type", FemMeshType.Grid, FemMeshType.values());
      meshSelector.addValueChangeListener (this);
      geometryPanel.addWidget (meshSelector);
      
      createMeshPropPanel();      
      geometryPanel.addWidget (meshPropPanel);
      
      elemSelector = new EnumSelector ("element type", 
         FemElementType.Tet, FemElementType.values());      
      elemSelector.addValueChangeListener (this);
      geometryPanel.addWidget (elemSelector);

      updateElemSelector();
      if (mySettings != null && elemSelector.isEnabledAll() && 
          mySettings.elemType != null) {
         elemSelector.maskValueChangeListeners (true);
         elemSelector.setValue (mySettings.elemType);
         elemSelector.maskValueChangeListeners (false);
      }
      
      addWidget (geometryPanel);
      geometryPanel.setBorder (
         GuiUtils.createTitledPanelBorder ("Geometry"));
   }

   private void createMeshPropPanel() {
      meshPropPanel = new LabeledComponentPanel();
      
      gridDimField = new VectorField ("widths", DEFAULT_GRID_DIM, "%8.3f");
      gridDivField = new IntegerMultiField ("divisions", DEFAULT_GRID_DIV, "%d");
      gridDimField.setStretchable (true);
      gridDivField.setStretchable (true);
      
      tubeDimField = new VectorField (
         "widths (L, rin, rout)", DEFAULT_TUBE_DIM, "%8.3f");
      tubeDivField = new IntegerMultiField (
         "divisions (nt, nl, nr)", DEFAULT_TUBE_DIV, "%d");
      tubeDimField.setStretchable (true);
      tubeDivField.setStretchable (true);
      
      torusDimField = new VectorField (
         "widths (R, rin, rout)", DEFAULT_TORUS_DIM, "%8.3f");
      torusDivField = new IntegerMultiField (
         "divisions (nt, nl, nr)", DEFAULT_TORUS_DIV, "%d");
      torusDimField.setStretchable (true);
      torusDivField.setStretchable (true);
      
      sphereNodesField = new IntegerSelector ("node count", SPHERE_NODE_OPTIONS);
      
      extrusDepthField = new DoubleField (
         "depth", DEFAULT_EXTRUSION_DEPTH, "%8.3f");
      extrusLayersField = new IntegerField (
         "layers", DEFAULT_EXTRUSION_LAYERS, "%d");
      extrusFileField = createFileChooser ("obj file", null, "obj");
      extrusFileField.setAlignmentX (Component.LEFT_ALIGNMENT);
      extrusDepthField.setStretchable (true);
      extrusLayersField.setStretchable (true);
      
      ansysNodeFileField = createFileChooser ("node file", null, "node");
      ansysNodeFileField.setAlignmentX (Component.LEFT_ALIGNMENT);
      ansysElemFileField = createFileChooser ("elem file", null, "elem");
      ansysElemFileField.setAlignmentX (Component.LEFT_ALIGNMENT);
      
      tetgenNodeFileField = createFileChooser ("node file", null, "node");
      tetgenNodeFileField.setAlignmentX (Component.LEFT_ALIGNMENT);
      tetgenEleFileField = createFileChooser ("ele file", null, "ele");
      tetgenEleFileField.setAlignmentX (Component.LEFT_ALIGNMENT);
      
      ucdMeshFileField = createFileChooser ("INP file", null, "inp");
      ucdMeshFileField.setAlignmentX (Component.LEFT_ALIGNMENT);
      
      surfaceMeshFileField = createFileChooser ("OBJ file", null, "obj");
      surfaceMeshFileField.setAlignmentX (Component.LEFT_ALIGNMENT);
      
      scaleField = new ScaleField ("scale", 1);
      scaleField.setStretchable (true);

      autoScaleBtn = new JButton ("auto scale");
      autoScaleBtn.addActionListener (this);      
      scaleField.addMajorComponent (autoScaleBtn);
      
      restoreDefaultValues();
      
      if (mySettings != null) {
         meshSelector.maskValueChangeListeners (true);
         meshSelector.setValue (mySettings.meshType);
         meshSelector.maskValueChangeListeners (false);
         
         gridDimField.setValue (mySettings.gridWidths);
         gridDivField.setValue (mySettings.gridDivisions);
         tubeDimField.setValue (mySettings.tubeWidths);
         tubeDivField.setValue (mySettings.tubeDivisions);
         torusDimField.setValue (mySettings.torusWidths);
         torusDivField.setValue (mySettings.torusDivisions);
         sphereNodesField.setValue (mySettings.sphereNodes);
         extrusDepthField.setValue (mySettings.extrusionDepth);
         extrusLayersField.setValue (mySettings.extrusionLayers);
         
         if (new File (mySettings.extrusionFile).isFile()) {
            extrusFileField.setValue (mySettings.extrusionFile);
         }
         
         if (new File (mySettings.ansysNodeFile).isFile()) {
            ansysNodeFileField.setValue (mySettings.ansysNodeFile);
         }
         
         if (new File (mySettings.ansysElemFile).isFile()) {
            ansysElemFileField.setValue (mySettings.ansysElemFile);
         }
         
         if (new File (mySettings.tetgenNodeFile).isFile()) {
            tetgenNodeFileField.setValue (mySettings.tetgenNodeFile);
         }
         
         if (new File (mySettings.tetgenEleFile).isFile()) {
            tetgenEleFileField.setValue (mySettings.tetgenEleFile);
         }
         if (new File (mySettings.ucdFile).isFile()) {
            ucdMeshFileField.setValue (mySettings.ucdFile);
         }
         if (new File (mySettings.surfaceMeshFile).isFile()) {
            surfaceMeshFileField.setValue (mySettings.surfaceMeshFile);
         }
      }      

      updateMeshPanel();
   }
   
   private FileNameField createFileChooser (
      String label, String fileName, String extension) {
      
      FileNameField fileWidget = new FileNameField (label, 20);
      JFileChooser chooser = fileWidget.getFileChooser();
      chooser.setAcceptAllFileFilterUsed (false);
      chooser.setFileFilter (
         new FileExtensionFilter("OBJ file", "obj"));
      
      if (!extension.equals ("obj")) {
         chooser.addChoosableFileFilter (
            new FileExtensionFilter(label, extension));
      }

      if (fileName == null) {
         fileName = "";
      }      
      fileWidget.setValue (fileName);
      
      File directory = ArtisynthPath.getWorkingDir();
      if (fileName != null) {
         File parentFile = new File (fileName).getParentFile();
         if (parentFile != null && parentFile.isDirectory()) {
            directory = parentFile;
         }
      }
      chooser.setCurrentDirectory (directory);
      
      return fileWidget;
   }
   
   private void updateElemSelector() {
      FemMeshType value = (FemMeshType) meshSelector.getValue();
      geometryPanel.removeWidget (elemSelector);
      
      if (value == FemMeshType.AnsysMesh || 
          value == FemMeshType.TetgenMesh || 
          value == FemMeshType.UCDMesh ||
          value == FemMeshType.SurfaceMesh ||
          value == FemMeshType.Sphere) {
         elemSelector.setEnabledAll (false);
      }
      else if (value == FemMeshType.Grid) {
         elemSelector = new EnumSelector (
            "element type", FemElementType.values());
      }
      else if (value == FemMeshType.Tube ||
               value == FemMeshType.Torus ||
               value == FemMeshType.Extrusion ) {
         elemSelector = new EnumSelector ("element type", 
            new FemElementType[] {FemElementType.Tet, FemElementType.Hex,
                               FemElementType.QuadTet, FemElementType.QuadHex});
      }

      elemSelector.addValueChangeListener (this);
      geometryPanel.addWidget (elemSelector);
   }
   
   private void restoreDefaultValues() {
      maskAllValueChangeListeners (true);
      
      lastScale = 1.0;      
      scaleField.setValue (lastScale);
      positionField.setVoidValueEnabled (true);
      positionField.setValue (Property.VoidValue);
      positionField.setVoidValueEnabled (false);
      orientationField.setValue (new AxisAngle());
      
      GLViewer viewer = myMain.getMain().getViewer();      
      double width = viewer.distancePerPixel (viewer.getCenter()) * 
         viewer.getScreenWidth() / 6;
      
      // Grid fields
      double gridWidth = DEFAULT_GRID_DIM.x;
      if (width > gridWidth) {
         for (int i = -1; width > gridWidth; i++) {
            gridWidth *= (i % 3 == 0) ? 2.5 : 2.0;
         }
      }
      else {
         for (int i = -1; width < gridWidth; i++) {
            gridWidth /= (i % 3 == 0) ? 2.5 : 2.0;
         }
      }
      
      gridDimField.setValue (new Vector3d (gridWidth, gridWidth, gridWidth));
      gridDivField.setValue (DEFAULT_GRID_DIV);
      
      // Tube fields
      double tubeScale = 1;
      if (width > tubeScale * DEFAULT_TUBE_DIM.z) {
         for (int i = -1; width > tubeScale * DEFAULT_TUBE_DIM.z; i++) {
            tubeScale *= (i % 3 == 0) ? 2.5 : 2.0;
         }
      }
      else {
         for (int i = -1; width < tubeScale * DEFAULT_TUBE_DIM.z; i++) {
            tubeScale /= (i % 3 == 0) ? 2.5 : 2.0;
         }
      }
      
      tubeScale /= 2.0;

      tubeDimField.setValue (new Vector3d (DEFAULT_TUBE_DIM.x * tubeScale, 
         DEFAULT_TUBE_DIM.y * tubeScale, DEFAULT_TUBE_DIM.z * tubeScale));
      tubeDivField.setValue (DEFAULT_TUBE_DIV);
      
      // Torus fields
      double torusScale = 1;
      if (width > torusScale * (DEFAULT_TORUS_DIM.x + DEFAULT_TORUS_DIM.z)) {
         for (int i = -1; 
              width > torusScale * (DEFAULT_TORUS_DIM.x + DEFAULT_TORUS_DIM.z); 
              i++) {
            torusScale *= (i % 3 == 0) ? 2.5 : 2.0;
         }
      }
      else {
         for (int i = -1; 
              width < torusScale * (DEFAULT_TORUS_DIM.x + DEFAULT_TORUS_DIM.z);
              i++) {
            torusScale /= (i % 3 == 0) ? 2.5 : 2.0;
         }
      }
      
      torusScale /= 2.0;

      torusDimField.setValue ( new Vector3d (DEFAULT_TORUS_DIM.x * torusScale, 
         DEFAULT_TORUS_DIM.y * torusScale, DEFAULT_TORUS_DIM.z * torusScale));
      torusDivField.setValue (DEFAULT_TORUS_DIV);
      
      // Sphere field
      sphereNodesField.setValue (SPHERE_NODE_OPTIONS[0]);
      
      // Extrusion fields
      extrusDepthField.setValue (DEFAULT_EXTRUSION_DEPTH);
      extrusLayersField.setValue (DEFAULT_EXTRUSION_LAYERS);
      extrusFileField.setValue (null);
      
      // Ansys fields
      ansysNodeFileField.setValue (null);
      ansysElemFileField.setValue (null);
      
      // Tetgen fields
      tetgenNodeFileField.setValue (null);
      tetgenEleFileField.setValue (null);
      
      // UCD field
      ucdMeshFileField.setValue (null);
      
      // Surface mesh field
      surfaceMeshFileField.setValue (null);
      
      maskAllValueChangeListeners (false);
   }

   private void updateMeshPanel() {
      for (Component comp : meshPropPanel.getWidgets()) {
         if (comp instanceof LabeledControl) {
            ((LabeledControl) comp).removeValueChangeListener (this);
         }
      }
      
      meshPropPanel.removeAllWidgets ();
      scaleField.setValue (1);
      
      FemMeshType value = (FemMeshType) meshSelector.getValue();
      if (value == FemMeshType.Grid) {
         meshPropPanel.addWidget (gridDimField);
         meshPropPanel.addWidget (gridDivField);
         
         gridDimField.addValueChangeListener (this);
         gridDivField.addValueChangeListener (this);
         
         fileCheck = true;
      }
      else if (value == FemMeshType.Tube) {
         meshPropPanel.addWidget (tubeDimField);
         meshPropPanel.addWidget (tubeDivField);

         tubeDimField.addValueChangeListener (this);
         tubeDivField.addValueChangeListener (this);

         fileCheck = true;
      }
      else if (value == FemMeshType.Torus) {
         meshPropPanel.addWidget (torusDimField);
         meshPropPanel.addWidget (torusDivField);

         torusDimField.addValueChangeListener (this);
         torusDivField.addValueChangeListener (this);

         fileCheck = true;
      }
      else if (value == FemMeshType.Sphere) {
         meshPropPanel.addWidget (sphereNodesField);
         meshPropPanel.addWidget (scaleField);
         
         sphereNodesField.addValueChangeListener (this);
         scaleField.addValueChangeListener (this);
         
         fileCheck = true;
      }
      else if (value == FemMeshType.Extrusion) {
         meshPropPanel.addWidget (extrusDepthField);
         meshPropPanel.addWidget (extrusLayersField);
         meshPropPanel.addWidget (extrusFileField);
         meshPropPanel.addWidget (scaleField);

         extrusDepthField.addValueChangeListener (this);
         extrusLayersField.addValueChangeListener (this);
         extrusFileField.addValueChangeListener (this);
         scaleField.addValueChangeListener (this);
         
         File file = new File (extrusFileField.getStringValue());
         fileCheck = file.isFile();
      }
      else if (value == FemMeshType.AnsysMesh) {
         meshPropPanel.addWidget (ansysNodeFileField);
         meshPropPanel.addWidget (ansysElemFileField);
         meshPropPanel.addWidget (scaleField);

         ansysNodeFileField.addValueChangeListener (this);
         ansysElemFileField.addValueChangeListener (this);
         scaleField.addValueChangeListener (this);
         
         File nodeFile = new File (ansysNodeFileField.getStringValue());
         File elemFile = new File (ansysElemFileField.getStringValue());
         fileCheck = nodeFile.isFile() && elemFile.isFile();
      }
      else if (value == FemMeshType.TetgenMesh) {
         meshPropPanel.addWidget (tetgenNodeFileField);
         meshPropPanel.addWidget (tetgenEleFileField);   
         meshPropPanel.addWidget (scaleField);      

         tetgenNodeFileField.addValueChangeListener (this);
         tetgenEleFileField.addValueChangeListener (this);
         scaleField.addValueChangeListener (this);
         
         File nodeFile = new File (tetgenNodeFileField.getStringValue());
         File eleFile = new File (tetgenEleFileField.getStringValue());
         fileCheck = nodeFile.isFile() && eleFile.isFile();
      }
      else if (value == FemMeshType.UCDMesh) {
         meshPropPanel.addWidget (ucdMeshFileField);
         meshPropPanel.addWidget (scaleField);

         ucdMeshFileField.addValueChangeListener (this);
         scaleField.addValueChangeListener (this);
         
         File file = new File (ucdMeshFileField.getStringValue());
         fileCheck = file.isFile();
      }
      else if (value == FemMeshType.SurfaceMesh) {
         meshPropPanel.addWidget (surfaceMeshFileField);
         meshPropPanel.addWidget (scaleField);

         surfaceMeshFileField.addValueChangeListener (this);
         scaleField.addValueChangeListener (this);
         
         File file = new File (surfaceMeshFileField.getStringValue());
         fileCheck = file.isFile();
      }
      meshPropPanel.repaint();
      myContentPane.repackContainingWindow();
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
   
   public void handleLocationEvent (GLViewer viewer, MouseRayEvent rayEvent) {
      Point3d isect;
      
      if (myUsePlaneToggle.isSelected() && viewer.getNumClipPlanes() > 0) {
         isect = intersectClipPlane (
            rayEvent.getRay(), viewer.getClipPlane (0));
      }
      else {
         isect = intersectViewPlane (
            rayEvent.getRay(), getCenter (new FemModel3d()), viewer);
      }
      
      positionField.setValue (isect);
   }

   private void updateDisplayLocation() {
      if (fem != null) {
         RigidTransform3d X = new RigidTransform3d();      
         X.p.set (positionField.getVectorValue());
         X.R.setAxisAngle (orientationField.getAxisAngleValue());
         
         fem.getSurfaceMesh().setMeshToWorld (X);
         rotator.setDraggerToWorld (X);            
         myMain.rerender();
      }
   }
   
   private void resetPreviewModel() {
      disposePreviewModel();
      
      if (currentState == State.Adding) {
         createPreviewModel();
      }
   }
   
   public void valueChange (ValueChangeEvent evt) {
      Object source = evt.getSource();
      
      if (source == meshSelector) {
         updateMeshPanel();
         updateElemSelector();
         updateState();
         
         resetPreviewModel();
         
         if (meshPropPanel.getComponentIndex (scaleField) != -1 && fem != null) {
            updateScale();
         }
      }
      else if (source == elemSelector || 
               source == gridDimField || source == gridDivField ||
               source == tubeDimField || source == tubeDivField ||
               source == torusDimField || source == torusDivField ||
               source == extrusDepthField || source == extrusLayersField ) {
         resetPreviewModel();
      }
      else if (source == sphereNodesField) {
         resetPreviewModel();
         
         if (fem != null) {
            updateScale();
         }
      }
      else if (source == extrusFileField) {
         File file = new File (extrusFileField.getStringValue());
         
         if (file.exists() && file.isFile()) {            
            fileCheck = true;
            updateState();
            
            resetPreviewModel();
            
            if (fem != null) {
               updateScale();
            }
         }
         else {
            fileCheck = false;
            
            GuiUtils.showError (
               myDisplay, "File does not exist:" + file.getAbsolutePath());            
         }
      }
      else if (source == ansysNodeFileField || source == ansysElemFileField) {
         File file = new File (((source == ansysNodeFileField) ? 
            ansysNodeFileField : ansysElemFileField).getStringValue());
         File otherFile = new File (((source != ansysNodeFileField) ? 
            ansysNodeFileField : ansysElemFileField).getStringValue());
         
         if (file.exists() && file.isFile()) {            
            if (otherFile.exists() && otherFile.isFile()) {
               fileCheck = true;
               updateState();
            }

            resetPreviewModel();
            
            if (fem != null) {
               updateScale();
            }
         }
         else {
            fileCheck = false;
            
            GuiUtils.showError (
               myDisplay, "Invalid file: " + file.getAbsolutePath());            
         }
         
         File currentDir = 
            ((FileNameField) source).getFileChooser().getCurrentDirectory();
         if (source == ansysNodeFileField) {
            ansysElemFileField.getFileChooser().setCurrentDirectory (currentDir);
         }
         else {
            ansysNodeFileField.getFileChooser().setCurrentDirectory (currentDir);
         }
      }
      else if (source == tetgenNodeFileField || source == tetgenEleFileField) {
         File file = new File (((source == tetgenNodeFileField) ? 
            tetgenNodeFileField : tetgenEleFileField).getStringValue());
         File otherFile = new File (((source != tetgenNodeFileField) ? 
            tetgenNodeFileField : tetgenEleFileField).getStringValue());
         
         if (file.exists() && file.isFile()) {            
            if (otherFile.exists() && otherFile.isFile()) {
               fileCheck = true;
               updateState();
            }

            resetPreviewModel();
            
            if (fem != null) {
               updateScale();
            }
         }
         else {
            fileCheck = false;
            
            GuiUtils.showError (
               myDisplay, "Invalid file: " + file.getAbsolutePath());            
         }
         
         File currentDir = 
            ((FileNameField) source).getFileChooser().getCurrentDirectory();
         if (source == tetgenNodeFileField) {
            tetgenEleFileField.getFileChooser().setCurrentDirectory (currentDir);
         }
         else {
            tetgenNodeFileField.getFileChooser().setCurrentDirectory (currentDir);
         }
      }
      else if (source == ucdMeshFileField) {
         File file = new File (ucdMeshFileField.getStringValue());
         
         if (file.exists() && file.isFile()) {            
            fileCheck = true;
            updateState();
            
            resetPreviewModel();
            
            if (fem != null) {
               updateScale();
            }
         }
         else {
            fileCheck = false;
            
            GuiUtils.showError (
               myDisplay, "File does not exist:" + file.getAbsolutePath()); 
         }
      }
      else if (source == surfaceMeshFileField) {
         File file = new File (surfaceMeshFileField.getStringValue());
         
         if (file.exists() && file.isFile()) {            
            fileCheck = true;
            updateState();
            
            resetPreviewModel();
            
            if (fem != null) {
               updateScale();
            }
         }
         else {
            fileCheck = false;
            
            GuiUtils.showError (
               myDisplay, "File does not exist:" + file.getAbsolutePath()); 
         }
      }
      else if (source == positionField) {
         updateState();

         if (meshPropPanel.getComponentIndex (scaleField) != -1 && fem != null) {
            updateScale();
         }

         updateDisplayLocation();
      }
      else if (source == orientationField) {
         updateDisplayLocation();
      }
      else if (source == scaleField) {
         if (fem != null && scaleField.getValue() != lastScale) {
            Object scale = scaleField.getValue();
            
            resetScaling();
            if (scale instanceof Double) {
               fem.getSurfaceMesh().scale ((Double) scale);
            }
            else if (scale instanceof Vector3d) {
               fem.getSurfaceMesh().scale (((Vector3d) scale).x,
                  ((Vector3d) scale).y, ((Vector3d) scale).z);
            }
            
            scaleField.maskValueChangeListeners (true);
            scaleField.setValue (scale);
            scaleField.maskValueChangeListeners (false);
            lastScale = scale;
            
            myMain.rerender();
         }
      }
      else {
         super.valueChange (evt);
      }
   }

   private void resetScaling() {
      if (lastScale instanceof Double && (Double) lastScale != 1.0) { 
         fem.getSurfaceMesh().scale (1 / (Double) lastScale);
      }
      else if (lastScale instanceof Vector3d &&
               lastScale != new Vector3d (1, 1, 1)) {
         fem.getSurfaceMesh().scale (1 / ((Vector3d) lastScale).x,
            1 / ((Vector3d) lastScale).y, 1 / ((Vector3d) lastScale).z);
      }
      
      lastScale = 1.0;      
      scaleField.maskValueChangeListeners (true);
      scaleField.setValue (1.0);
      scaleField.maskValueChangeListeners (false);
   }

   private void updateScale() {
      resetScaling();
      
      Vector3d minBound = new Vector3d();
      Vector3d maxBound = new Vector3d();
      fem.getSurfaceMesh().getWorldBounds (minBound, maxBound);
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
   
   private void disposePreviewModel() {
      if (fem != null) {
         ViewerManager viewerMan = myMain.getWorkspace().getViewerManager();
         viewerMan.removeRenderable (fem.getSurfaceMesh());
         viewerMan.removeDragger (rotator);
         
         fem.dispose();
         fem = null;
         rotator = null;
         
         myMain.rerender();
      }
   }

   private void maskAllValueChangeListeners (boolean masked) {
      positionField.maskValueChangeListeners (masked);
      orientationField.maskValueChangeListeners (masked);
      scaleField.maskValueChangeListeners (masked);
      
      gridDimField.maskValueChangeListeners (masked);
      gridDivField.maskValueChangeListeners (masked);
      tubeDimField.maskValueChangeListeners (masked);
      tubeDivField.maskValueChangeListeners (masked);
      torusDimField.maskValueChangeListeners (masked);
      torusDivField.maskValueChangeListeners (masked);
      sphereNodesField.maskValueChangeListeners (masked);
      extrusDepthField.maskValueChangeListeners (masked);
      extrusLayersField.maskValueChangeListeners (masked);
      extrusFileField.maskValueChangeListeners (masked);
      ansysNodeFileField.maskValueChangeListeners (masked);
      ansysElemFileField.maskValueChangeListeners (masked);
      tetgenNodeFileField.maskValueChangeListeners (masked);
      tetgenEleFileField.maskValueChangeListeners (masked);
      ucdMeshFileField.maskValueChangeListeners (masked);
      surfaceMeshFileField.maskValueChangeListeners (masked);
   }
   
   public void actionPerformed (ActionEvent evt) {
      String cmd = evt.getActionCommand();
      
      if (cmd.equals ("Add")) {
         ViewerManager viewerMan = myMain.getWorkspace().getViewerManager();
         viewerMan.removeRenderable (fem.getSurfaceMesh());
         viewerMan.removeDragger (rotator);
         rotator = null;
         
         setProperties (fem, getPrototypeComponent (myComponentType));
         // basicPropTree.setTreeValuesInHost (
         //    fem, getPrototypeComponent (myComponentType));
         // myPropTree.setTreeValuesInHost (
         //    fem, getPrototypeComponent (myComponentType));
         setProperties (myPrototype, myPrototype);
         
         PolygonalMesh mesh = fem.getSurfaceMesh();
         mesh.setRenderProps (mesh.createRenderProps());
         mesh.setMeshToWorld (RigidTransform3d.IDENTITY);
         mesh.setFixed (false);
         mesh.setColorsFixed (false);
         
         
         RigidTransform3d rigidX = new RigidTransform3d();
         rigidX.p.set (positionField.getVectorValue());
         rigidX.R.setAxisAngle (orientationField.getAxisAngleValue());
         
         AffineTransform3d X = new AffineTransform3d();
         X.set (rigidX);
         
         Object scale = scaleField.getValue();
         resetScaling();
         double sx = 1, sy = 1, sz = 1;
         if (scale instanceof Double) {
            sx = sy = sz = (Double) scale;
         }
         else if (scale instanceof Vector3d) {
            sx = ((Vector3d) scale).x;
            sy = ((Vector3d) scale).y;
            sz = ((Vector3d) scale).z;
         }
         X.applyScaling (sx, sy, sz);     
         
         fem.transformGeometry (X);
         
         addComponent (new AddComponentsCommand (
            "add FemModel3d", fem, myContainer));
         
         myDisplay.setVisible (false);
         dispose();
      }
      else if (cmd.equals ("Clear")) {
         maskAllValueChangeListeners (true);         
         restoreDefaultValues();

         myHostList.restoreBackupValues();
         myPropertyPanel.updateWidgetValues();
         // basicHostList.restoreBackupValues();
         // moreHostList.restoreBackupValues();
         // basicPropsPanel.updateWidgetValues();
         // morePropsPanel.updateWidgetValues();
         
         disposePreviewModel();

         resetState();
         maskAllValueChangeListeners (false);
         
         updateMeshPanel();
         updateState();
      } 
      else if (cmd.equals ("Cancel")) {
         disposePreviewModel();
         myDisplay.setVisible (false);
         dispose();
      }
      else if (evt.getSource() == autoScaleBtn) {
         if (fem != null) {
            updateScale();
         }
      }
      else {
         super.actionPerformed (evt);
      }
   }
   
   public void dispose() {
      setSettings();
      
      super.dispose();
   }
   
   private class FemModelDraggerListener extends Dragger3dAdapter {
      public void draggerMove (Dragger3dEvent e) {
         Transrotator3d rotator = (Transrotator3d) e.getSource();
         RigidTransform3d X = rotator.getDraggerToWorld();         
         fem.getSurfaceMesh().setMeshToWorld (X);
         
         positionField.maskValueChangeListeners (true);
         positionField.setValue (X.p);
         positionField.maskValueChangeListeners (false);
         
         orientationField.maskValueChangeListeners (true);
         orientationField.setValue (X.R.getAxisAngle());
         orientationField.maskValueChangeListeners (false);
         
         myMain.rerender();
      } 
   }
   
   private class FileExtensionFilter extends FileFilter {
      private String description;
      private String extension;

      public FileExtensionFilter (String description, String extension) {
         if (extension == null) {
            throw new IllegalArgumentException (
               "Extensions must be non-null");
         }
         
         this.description = description;
         this.extension = extension;
      }
      
      public boolean accept(File f) {
         if (f != null) {
            if (f.isDirectory()) {
               return true;
            }

            String fileName = f.getName();
            int i = fileName.lastIndexOf('.');
            if (i > 0 && i < fileName.length() - 1) {
               String desiredExtension = fileName.substring(i+1).toLowerCase();
               if (desiredExtension.equals(extension)) {
                  return true;
               }
            }
         }
         
         return false;
      }

      public String getDescription () {
         return description;
      }      
   }
}