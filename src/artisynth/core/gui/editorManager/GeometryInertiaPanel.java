/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.filechooser.*;

import java.io.File;
import java.util.*;

import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.GL.GLViewer;
import maspack.spatialmotion.*;
import maspack.properties.PropertyUtils;
import maspack.util.InternalErrorException;
import maspack.util.StringHolder;
import maspack.widgets.AffineTransformWidget;
import maspack.widgets.DoubleField;
import maspack.widgets.EnumSelector;
import maspack.widgets.FileNameField;
import maspack.widgets.GuiUtils;
import maspack.widgets.IntegerField;
import maspack.widgets.LabeledComponentPanel;
import maspack.widgets.LabeledControl;
import maspack.widgets.SymmetricMatrix3dField;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.widgets.ValueCheckListener;
import maspack.widgets.VectorField;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.gui.widgets.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.RigidBody.InertiaMethod;
import artisynth.core.util.ArtisynthPath;

/**
 * A panel that allows an application to select a specified mesh geometry. The
 * panel also computes (or allows the user to specify) the corresponding spatial
 * inertia.
 */
public class GeometryInertiaPanel extends LabeledComponentPanel implements
ActionListener, ValueChangeListener {
   // if non-null, indicates a body that this panel is editing live
   private RigidBody myAttachedBody;

   // the quantities this panel manages
   private PolygonalMesh myMesh;
   private String myMeshFileName = null;
   private AffineTransform3dBase myMeshFileTransform = null;

   // original copies of said quantities (usually saved from a rigid body)
   private SpatialInertia myOriginalInertia = new SpatialInertia();
   private double myOriginalDensity;
   private PolygonalMesh myOriginalMesh;
   private String myOriginalMeshFileName;
   private AffineTransform3dBase myOriginalMeshFileTransform;
   private InertiaMethod myOriginalInertiaMethod;

   // mesh scanned in directly from a file (before MeshFileTransform is applied)
   private PolygonalMesh myFileMesh;

   // fields for controlling inertia
   private DoubleField myMassField;
   private SymmetricMatrix3dField myInertiaField;
   private VectorField myCOMField;
   DoubleField myDensityField;

   // field for controlling mesh type
   EnumSelector myGeometrySelector;

   // fields for controlling parameters of different geometry types
   FileNameField myMeshFileField;
   AffineTransformWidget myMeshXformWidget;
   JButton myCOMButton;
   JButton myComputeDensityButton;
   DoubleField myPointRadiusField;
   IntegerField myPointSlicesField;
   VectorField myBoxWidthsField;

   ArrayList<LabeledControl> myGeometryWidgets = new ArrayList<LabeledControl>();

   private boolean myValueChangeHandlerMasked = false;
   private EnumSelector myInertiaMethodSelector;

   // parameter settings for creating different geometry types
   public class GeometrySettings {
      GeometryType geometryType;
      Vector3d boxWidths = new Vector3d();
      double pointRadius;
      int pointSlices = 12;
      String meshFileName;
      AffineTransform3dBase meshXform = new RigidTransform3d();
      double density;
      SpatialInertia inertia = new SpatialInertia();
      InertiaMethod inertiaMethod = InertiaMethod.DENSITY;

      public GeometrySettings() {
      }

      public GeometrySettings (GeometrySettings s) {
         this();
         set (s);
      }

      public void set (GeometrySettings s) {
         geometryType = s.geometryType;
         boxWidths.set (s.boxWidths);
         pointRadius = s.pointRadius;
         pointSlices = s.pointSlices;
         density = s.density;
         meshFileName = s.meshFileName;
         meshXform = s.meshXform.copy();
         inertia.set (s.inertia);
         inertiaMethod = s.inertiaMethod;
      }
   }

   enum GeometryType {
      Box, Sphere, Mesh
   }

   private class ObjFileFilter extends FileFilter {
      public boolean accept (File f) {
         return (f.isDirectory() || f.isFile()
         && f.getName().endsWith (".obj"));
      }

      public String getDescription() {
         return ".obj files";
      }
   }

   public GeometrySettings getSettings() {
      GeometrySettings settings = new GeometrySettings();
      getSettings (settings);
      return settings;
   }

   /** 
    * Creates appropriate default values for GeometrySettings. Called when
    * this panel is created if settings have not been explicitly supplied.
    */   
   private GeometrySettings createDefaultSettings (RigidBody body) {
      GeometrySettings settings = new GeometrySettings();
      // may be overridden below .,..
      settings.geometryType = GeometryType.Box;
      
      settings.meshXform = new RigidTransform3d();
      settings.meshFileName = null;
      myFileMesh = null;
      if (body != null) {
         if (myAttachedBody == body) {
            settings.geometryType = GeometryType.Mesh;
            PolygonalMesh mesh;
            if ((mesh = body.getSurfaceMesh()) != null) {
               MeshComponent mcomp = body.getSurfaceMeshComp();
               myFileMesh = mesh.copy();
               if (mcomp.getFileName() != null) {
                  settings.meshFileName = mcomp.getFileName();
               }              
               if (mcomp.getFileTransform() != null) {
                  settings.meshXform = mcomp.getFileTransform().copy();
               }
               if (settings.meshXform != null) {
                  myFileMesh.inverseTransform (settings.meshXform);
               }
            }
         }
         else {
            MeshComponent mcomp = body.getSurfaceMeshComp();
            if (mcomp != null && mcomp.getFileName() != null) {
               myFileMesh = loadMeshFromFile (mcomp.getFileName());
            }
            if (myFileMesh != null) {
               settings.meshFileName = mcomp.getFileName();
               if (mcomp.getFileTransform() != null) {
                  settings.meshXform = mcomp.getFileTransform().copy();
               }
            }
         }
      }
      if (body != null) {
         settings.inertiaMethod = body.getInertiaMethod();
         settings.density = body.getDensity();
         body.getInertia (settings.inertia);
      }
      else {
         settings.density = 1.0;
         // default inertia: unit mass and identity rotational inertia
         settings.inertia.set (1, 1, 1, 1); 
         settings.inertiaMethod = InertiaMethod.DENSITY;
      }
      if (body != null && body.getMesh() != null) {
         PolygonalMesh mesh = body.getMesh();
         OBB obb = new OBB (mesh);
         Vector3d widths = new Vector3d();
         obb.getWidths (widths);
         settings.boxWidths.set (widths);
         settings.pointRadius = mesh.computeAverageRadius();
      }
      else {
         GLViewer viewer = Main.getMain().getViewer();
         double w =
            (viewer.distancePerPixel (viewer.getCenter()) * viewer.getScreenWidth());
         settings.pointRadius = w / 8;
         settings.boxWidths.set (w / 8, w / 8, w / 8);
      }
      return settings;
   }

   private void setWidget (LabeledControl widget, Object value) {
      myValueChangeHandlerMasked = true;
      widget.setValue (value);
      myValueChangeHandlerMasked = false;
   }

   private void setDensity (double d) {
      setWidget (myDensityField, d);
   }

   public double getDensity() {
      return myDensityField.getDoubleValue();
   }
   
   public double getMass() {
      return myMassField.getDoubleValue();
   }

   // returns the geometry parameters settings currently stored in the components
   private void getSettings (GeometrySettings settings) {
      settings.geometryType = (GeometryType) myGeometrySelector.getValue();
      settings.boxWidths.set (myBoxWidthsField.getVectorValue());
      settings.pointRadius = myPointRadiusField.getDoubleValue();
      settings.pointSlices = myPointSlicesField.getIntValue();
      settings.density = myDensityField.getDoubleValue();
      settings.meshFileName = myMeshFileField.getStringValue();
      settings.meshXform = myMeshXformWidget.getTransformValue();
      settings.inertia.set (getInertia());
   }

   // loads geometry parameters into the widgets. Called once when panel is
   // created.
   private void setSettings (GeometrySettings settings) {
      setWidget (myGeometrySelector, settings.geometryType);
      setWidget (myBoxWidthsField, settings.boxWidths);
      setWidget (myPointRadiusField, settings.pointRadius);
      setWidget (myPointSlicesField, settings.pointSlices);
      setDensity (settings.density);
      setWidget (myMeshFileField, settings.meshFileName);

      // XXX do we want to do this?
//       PolygonalMesh mesh = loadMeshFromFile (settings.meshFileName);
      
//       if (mesh != null) {
//          myFileMesh = mesh;
//          myMeshFileName = settings.meshFileName;
//       }
//       else {
//          setWidget (myMeshFileField, myMeshFileName);
//       }
      
      setWidget (myMeshXformWidget, settings.meshXform);
      setInertiaWidgets (settings.inertia);
      setWidget (myInertiaMethodSelector, settings.inertiaMethod);
      setInertiaWidgetEnabling (settings.inertiaMethod);
   }

   private boolean objectsEqual (Object obj1, Object obj2) {
      if (obj1 != null && obj2 != null) {
         return obj1.equals (obj2);
      }
      else {
         return ((obj1 == null) == (obj2 == null));
      }
   }

   // XXX need to make sure myMeshFileTransform is not null, as it can
   // be in rigid bodies

   // Resets myMesh, myMeshFileName, and myMeshFileTransform based on the
   // currently selected Geometry type. For all types, this will involve
   // rebuilding the actual mesh (or for the Mesh type, copying/transforming it
   // from myFileMesh).
   // 
   public void resetMesh() {
      GeometryType type = (GeometryType) myGeometrySelector.getValue();
      
      switch (type) {
         case Box: {
            Vector3d widths = (Vector3d)myBoxWidthsField.getValue();
            myMesh =
               MeshFactory.createBox (
                  widths.get (0), widths.get (1), widths.get (2), 0, 0, 0);
            myMeshFileName = null;
            myMeshFileTransform = null;
            break;
         }
         case Sphere: {
            double radius = myPointRadiusField.getDoubleValue();
            int slices = myPointSlicesField.getIntValue();            
            myMesh = MeshFactory.createSphere (radius, slices);
            myMeshFileName = null;
            myMeshFileTransform = null;
            break;
         }
         case Mesh: {
            String meshFileName = myMeshFileField.getStringValue();
            AffineTransform3dBase xform = myMeshXformWidget.getTransformValue();

//             if (xform instanceof RigidTransform3d) {
//                System.out.println ("xform.p=" + ((RigidTransform3d)xform).p);
//             }

            // set these to null, then override below as appropriate
            myMesh = null;
            myMeshFileName = null;
            myMeshFileTransform = null;

            if (myFileMesh != null) { 
               if (xform.isIdentity()) {
                  myMesh = myFileMesh;
               }
               else {
                  myMesh = myFileMesh.copy();
                  myMesh.transform (xform);
               }
            }
            if (meshFileName != null && meshFileName.length() > 0) {
               myMeshFileName = meshFileName;
               if (!xform.isIdentity()) {
                  myMeshFileTransform = xform;
               }
            }
            break;
         }
         default: {
            throw new InternalErrorException (
               "unimplemented geometry type: " + type);
         }
      }
      updateDensityAndInertia();
      if (myAttachedBody != null && myMesh != null) {
         // don't update the body unless we need to ...

         String bodyMeshFileName = null;
         AffineTransform3dBase bodyMeshFileTransform = null;
         MeshComponent mcomp = myAttachedBody.getSurfaceMeshComp();
         if (mcomp != null) {
            bodyMeshFileName = mcomp.getFileName();
            bodyMeshFileTransform = mcomp.getFileTransform();
         }
         if (myMesh != myAttachedBody.getSurfaceMesh() ||
             !objectsEqual (myMeshFileName, bodyMeshFileName) ||
             !objectsEqual (myMeshFileTransform, bodyMeshFileTransform)) {
            myAttachedBody.setSurfaceMesh (
               myMesh, myMeshFileName, myMeshFileTransform);
            Main.getMain().rerender();
         }
      }
   }

   private void updateDensityAndInertia (){
      if (myMesh != null) {
         if (getInertiaMethod() == InertiaMethod.DENSITY) {
            updateInertiaFromDensity (getDensity());
         }
         else {
            double density = getMass()/myMesh.computeVolume();
            setWidget (myDensityField, density);
            if (getInertiaMethod() == InertiaMethod.MASS) {
               updateInertiaFromDensity (density);
            }
         }
      }
   }

   /** 
    * Sets the inertia of a rigid body based on the settings in this panel.
    */   
   public void setBodyInertia (RigidBody body) {

      switch (getInertiaMethod()) {
         case DENSITY: {
            body.setInertiaFromDensity (getDensity());
            break;            
         }
         case MASS: {
            body.setInertiaFromMass (getMass());
            break;            
         }
         case EXPLICIT: {
            body.setInertia (getInertia());
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented inertia method " + getInertiaMethod());
         }
      }
   }

   /**
    * Method to compute the exact volume of a body, given its parametric
    * representation (or simply the mesh, if no parametric representation
    * exists).
    */
   private double computeExactVolume() {
      switch ((GeometryType) myGeometrySelector.getValue()) {
         case Box: {
            Vector3d widths = (Vector3d)myBoxWidthsField.getValue();
            return widths.x * widths.y * widths.z;
         }
         case Sphere: {
            double radius = myPointRadiusField.getDoubleValue();
            return 4 / 3.0 * Math.PI * radius * radius * radius;
         }
         default: {
            return myMesh.computeVolume();
         }
      }
   }

//   /**
//    * Method to compute the exact spatial inertia of a body, given its
//    * parametric representation (or simply the mesh, if no parametroc
//    * representation exists).
//    */
//   private SpatialInertia computeExactInertia (double density, double vol) {
//      SpatialInertia M = new SpatialInertia();
//      switch ((GeometryType) myGeometrySelector.getValue()) {
//         case Box: {
//            Vector3d widths = (Vector3d)myBoxWidthsField.getValue();
//            M.setBox (density * vol, widths.x, widths.y, widths.z);
//            break;
//         }
//         case Sphere: {
//            double radius = myPointRadiusField.getDoubleValue();
//            M.setSphere (density * vol, radius);
//            break;
//         }
//         default: {
//            myMesh.computeInertia (M, density);
//         }
//      }
//      return M;
//   }

   /**
    * Computes the body inertia from the mesh, given a density
    */
   private void updateInertiaFromDensity (double density) {
      SpatialInertia M = new SpatialInertia();
      myMesh.computeInertia (M, density);
      updateInertia (M);
   }

   /**
    * Updates the inertia settings in the widgets, as well as in the body of one
    * is attached.
    */
   private void updateInertia (SpatialInertia M) {
      setInertiaWidgets (M);
   }

   /**
    * Returns the spatial inertia described by the mass, ineria, and center of
    * mass fields. If one or more of these fields has a void value, then null is
    * returned.
    */
   public SpatialInertia getInertia() {
      double mass = myMassField.getDoubleValue();
      SymmetricMatrix3d J = (SymmetricMatrix3d)myInertiaField.getValue();
      Point3d com = (Point3d)myCOMField.getValue();
      SpatialInertia M = new SpatialInertia();
      M.set (mass, J, com);
      return M;
   }

   public InertiaMethod getInertiaMethod() {
      return (InertiaMethod)myInertiaMethodSelector.getValue();
   }
   
   private void setInertiaWidgets (SpatialInertia M) {
      setWidget (myMassField, M.getMass());
      setWidget (myInertiaField, M.getRotationalInertia());
      setWidget (myCOMField, M.getCenterOfMass());
   }

   public PolygonalMesh getMesh() {
      return myMesh;
   }

   public AffineTransform3dBase getMeshTransform() {
      return myMeshFileTransform;
   }

   public String getMeshFileName() {
      return myMeshFileName;
   }

   /** 
    * Called once at panel creation, and then whenever the geometry type is
    * changed (via the GeometrySelection widget). It rebuilds the panel to
    * include the widgets appropriate to the geometry.
    */   
   private void rebuildGeometryWidgets (GeometryType type) {
      for (LabeledControl w : myGeometryWidgets) {
         removeWidget (w);
      }
      myGeometryWidgets.clear();
      switch (type) {
         case Box: {
            myGeometryWidgets.add (myBoxWidthsField);
            break;
         }
         case Sphere: {
            myGeometryWidgets.add (myPointRadiusField);
            myGeometryWidgets.add (myPointSlicesField);
            break;
         }
         case Mesh: {
            myGeometryWidgets.add (myMeshFileField);
            myGeometryWidgets.add (myMeshXformWidget.getScaleField());
            myGeometryWidgets.add (myMeshXformWidget.getTranslationField());
            myGeometryWidgets.add (myMeshXformWidget.getRotationField());
            break;
         }
         default: {
            throw new InternalErrorException ("unimplemented geometry type: "
            + type);
         }
      }
      
      int idx = GuiUtils.indexOfComponent (this, myGeometrySelector) + 1;
      for (LabeledControl w : myGeometryWidgets) {
         addWidget (w, idx++);
      }
   }

   private void repackWindow () {
      if (isVisible()) { // window will need to repack and repaint itself
         Window win = SwingUtilities.windowForComponent (this);
         if (win != null) {
            win.pack();
            win.repaint();
         }
      }
   }

   protected void createGeometrySelector() {
      myGeometrySelector =
         new EnumSelector ("geometry type", GeometryType.values());

      addWidget (myGeometrySelector);
      myGeometrySelector.addValueChangeListener (this);
   }

   private void initWidget (LabeledControl widget) {
      widget.addValueChangeListener (this);
      widget.setAlignmentX (Component.LEFT_ALIGNMENT);
   }

   // Called once at panel creation time to create all the widgets
   // that may be needed for the Geometry.
   protected void createGeometryWidgets() {
      myBoxWidthsField = new VectorField ("widths", new Vector3d(), "%.4g");
      myBoxWidthsField.setResultHolder (new Vector3d());
      initWidget (myBoxWidthsField);
      myPointRadiusField = new DoubleField ("radius", 0, "%.4g");
      initWidget (myPointRadiusField);
      myPointSlicesField = new IntegerField ("slices", 12);
      initWidget (myPointSlicesField);
      myMeshFileField = new FileNameField ("file name", "", 20);
      initWidget (myMeshFileField);
      myMeshXformWidget =
         new AffineTransformWidget ("", "TRS", new RigidTransform3d());
      initWidget (myMeshXformWidget);
      myMeshXformWidget.unpackFields();
      myMeshXformWidget.getTranslationField().setLabelText ("offset");
      myMeshXformWidget.getScaleField().setLabelText ("scale");
      myMeshXformWidget.getRotationField().setLabelText ("rotation");
      myCOMButton = new JButton ("COM");
      myCOMButton.addActionListener (this);
      myCOMButton.setToolTipText ("puts mesh origin at its center of mass");
      GuiUtils.setFixedSize (
         myCOMButton, myMeshFileField.getBrowseButton().getPreferredSize());
      myMeshXformWidget.getTranslationField().addMajorComponent (myCOMButton);
   }

   private class PositiveValueCheck implements ValueCheckListener {
      public Object validateValue (ValueChangeEvent e, StringHolder errMsg) {
         double dval = ((Double)e.getValue()).doubleValue();
         if (dval <= 0) {
            return PropertyUtils.illegalValue (
               "value must be positive", errMsg);
         }
         else {
            return PropertyUtils.validValue (e.getValue(), errMsg);
         }
      }
   }

   // Called once at panel creation time to create and add the widgets needed
   // for controlling the inertia.
   protected void createInertiaWidgets() {      
      myInertiaMethodSelector = new EnumSelector (
         "set inertia by", InertiaMethod.values());
      myInertiaMethodSelector.addValueChangeListener (this);
      addWidget (myInertiaMethodSelector);

      myDensityField = new DoubleField ("density", 1, "%.6g");
      myDensityField.addValueChangeListener (this);
      myDensityField.setGUIVoidEnabled (true);
      myDensityField.setStretchable (true);
      myDensityField.addValueCheckListener (new PositiveValueCheck());
      addWidget (myDensityField);

      myMassField = new DoubleField ("mass");
      myMassField.addValueChangeListener (this);
      myMassField.setStretchable (true);
      myMassField.addValueCheckListener (new PositiveValueCheck());
      addWidget (myMassField);

      myInertiaField = new SymmetricMatrix3dField ("rotational inertia");
      myInertiaField.setFormat ("%.5g");
      myInertiaField.addValueChangeListener (this);
      myInertiaField.setStretchable (true);
      addWidget (myInertiaField);

      myCOMField = new VectorField ("center of mass", new Point3d(), "%.5g");
      myCOMField.setResultHolder (new Point3d());
      myCOMField.addValueChangeListener (this);
      myCOMField.setStretchable (true);
      addWidget (myCOMField);
      
      // if myOriginalInertia was not set from widgets, then this will
      // default to the identity inertia
      //setInertiaWidgets (myOriginalInertia);
   }

   private void createDisplay() {
      createGeometrySelector();
      createGeometryWidgets();
      addWidget (new JSeparator());
      createInertiaWidgets();
      
      add (Box.createHorizontalGlue ());
   }

   // Called at panel create time, and when we reset
   protected void initializeMeshField() {
      String fileName = myMeshFileField.getStringValue();
      myMeshFileName = fileName;
      JFileChooser chooser = myMeshFileField.getFileChooser();
      File directory = ArtisynthPath.getWorkingDir();
      if (fileName != null) {
         File parentFile = new File (fileName).getParentFile();
         if (parentFile != null && parentFile.isDirectory()) {
            directory = parentFile;
         }
      }
      chooser.setCurrentDirectory (directory);
      chooser.setAcceptAllFileFilterUsed (false);
      chooser.setFileFilter (new ObjFileFilter());
      chooser.addChoosableFileFilter (new ObjFileFilter());
   }

   // XXX investigate
   public GeometryInertiaPanel (
      GeometrySettings settings, RigidBody body, boolean attached) {
      super();

      if (body != null && attached) {
         body.getInertia (myOriginalInertia);
         myOriginalDensity = body.getDensity();
         MeshComponent mcomp = body.getSurfaceMeshComp();
         if (mcomp != null) {
            myOriginalMesh = body.getSurfaceMesh();
            myOriginalMeshFileName = mcomp.getFileName();
            myOriginalMeshFileTransform = mcomp.getFileTransform();
         }
         else {
            myOriginalMesh = null;
            myOriginalMeshFileName = null;
            myOriginalMeshFileTransform = null;
         }
         myOriginalInertiaMethod = body.getInertiaMethod();
         myAttachedBody = body;
      }
      if (settings == null) {
         settings = createDefaultSettings (body);
      }
      createDisplay();
      setSettings (settings);
      initializeMeshField();
      setWidgetsSelectable (false);
      //rebuildInertiaWidgets ((InertiaMethod)myInertiaMethodSelector.getValue());
      rebuildGeometryWidgets ((GeometryType) myGeometrySelector.getValue());
      resetMesh();
   }

   /** 
    * Attaches this geometry panel to a specified RigidBody, so that changes in
    * this panel will directly modify the geometry and inertia settings of that
    * body. If <code>body</code> is null, then any attached body is removed.
    */
   public void setAttachedBody (RigidBody body) {
      if (body != myAttachedBody) {
         myAttachedBody = body;
	 if (body != null) {
	    myOriginalDensity = body.getDensity();
	    body.getInertia (myOriginalInertia);
	    MeshComponent mcomp = body.getSurfaceMeshComp();
	    if (mcomp != null) {
	       myOriginalMesh = body.getSurfaceMesh();
	       myOriginalMeshFileName = mcomp.getFileName();
	       myOriginalMeshFileTransform = mcomp.getFileTransform();
	    }
	    else {
               myOriginalMesh = null;
               myOriginalMeshFileName = null;
               myOriginalMeshFileTransform = null;
	    }
            myOriginalInertiaMethod = body.getInertiaMethod();
            setInertiaWidgets (myOriginalInertia);
            setDensity (myOriginalDensity);
         }
      }
   }

   public RigidBody getAttachedBody() {
      return myAttachedBody;
   }

   private PolygonalMesh loadMeshFromFile (String fileName) {
      PolygonalMesh mesh = null;
      try {
         mesh = new PolygonalMesh (new File (fileName));
      }
      catch (Exception exc) {
         mesh = null;
      }
      return mesh;
   }

   // Called whenever the mesh file specified in the MeshFileField changes
   private void handleMeshChange (ValueChangeEvent e) {
      String fileName = (String)e.getValue();
      PolygonalMesh mesh = null;
      try {
         mesh = new PolygonalMesh (new File (fileName));
      }
      catch (Exception exc) {
         GuiUtils.showError (
            SwingUtilities.windowForComponent (this),
            "Cannot read mesh: "+exc.getMessage());
      }
      if (mesh != null) {
         myFileMesh = mesh;
         myMeshFileName = fileName;
      }
      else {
         setWidget (myMeshFileField, myMeshFileName);
      }
   }

   public void valueChange (ValueChangeEvent e) {
      if (myValueChangeHandlerMasked) {
         return;
      }

      Object source = e.getSource();
      if (source == myDensityField) {
         double density = myDensityField.getDoubleValue();
         if (myMesh != null) {
            double mass = density*myMesh.computeVolume();
            if (getInertiaMethod() == InertiaMethod.MASS ||
                getInertiaMethod() == InertiaMethod.DENSITY) {
               updateInertiaFromDensity (density);
            }
            setWidget (myMassField, mass);
         }
      }
      else if (source == myMassField) {
         double mass = myMassField.getDoubleValue();
         if (myMesh != null) {
            double density = mass/myMesh.computeVolume();
            if (getInertiaMethod() == InertiaMethod.MASS ||
                getInertiaMethod() == InertiaMethod.DENSITY) {
               updateInertiaFromDensity (density);
            }
            setWidget (myDensityField, density);
         }
      }
      else if (source == myInertiaField || source == myCOMField) {

      }
      else if (source == myMeshFileField) {
         handleMeshChange (e);
         resetMesh();
      }
      else if (source == myGeometrySelector) {
         rebuildGeometryWidgets ((GeometryType) myGeometrySelector.getValue());
         resetMesh();
         repackWindow();
      }
      else if (source == myInertiaMethodSelector) {
         setInertiaWidgetEnabling (
            (InertiaMethod)myInertiaMethodSelector.getValue());
         updateDensityAndInertia();
      }
      else { // must be a geometry widget
         if (source == myPointSlicesField && 
             myPointSlicesField.getIntValue() % 2 != 0) {
            setWidget (myPointSlicesField, myPointSlicesField.getIntValue()+1);
         }
         resetMesh();
      }
   }

   private void setInertiaWidgetEnabling (InertiaMethod method) {
      switch (method) {
         case DENSITY: {
            myDensityField.setEnabledAll (true);
            myMassField.setEnabledAll (false);
            myInertiaField.setEnabledAll (false);
            myCOMField.setEnabledAll (false);
            break;
         }
         case MASS: {
            myDensityField.setEnabledAll (false);
            myMassField.setEnabledAll (true);
            myInertiaField.setEnabledAll (false);
            myCOMField.setEnabledAll (false);
            break;
         }
         case EXPLICIT: {
            myDensityField.setEnabledAll (false);
            myMassField.setEnabledAll (true);
            myInertiaField.setEnabledAll (true);
            myCOMField.setEnabledAll (true);
            break;
         }
         default: {
            throw new InternalErrorException (
               "No implementation for InertiaMethod " + method);
         }
      }
   }

   // This is currently called only by RigidBodyGeometryAgent to reset settings
   // for an attachedBody. Hence attachedBody should be != null.
   public void resetGeometryAndInertia() {
      if (myAttachedBody != null) {
         setWidget (myInertiaMethodSelector, myOriginalInertiaMethod);
         setInertiaWidgetEnabling (myOriginalInertiaMethod);
         setWidget (myDensityField, myOriginalDensity);
         //rebuildInertiaWidgets (myOriginalInertiaMethod);
         setInertiaWidgets (myOriginalInertia);
         setWidget (myMeshFileField, myOriginalMeshFileName);
         initializeMeshField();
         if (myOriginalMeshFileTransform != null) {
            setWidget (myMeshXformWidget, myOriginalMeshFileTransform); 
         }
         else {
            setWidget (myMeshXformWidget, RigidTransform3d.IDENTITY);
         }
         // reset the mesh 
         myMesh = myOriginalMesh;
         myMeshFileName = myOriginalMeshFileName;
         myMeshFileTransform = myOriginalMeshFileTransform;
         myFileMesh = myMesh.copy();
         if (myMeshFileTransform != null) {
            myFileMesh.inverseTransform (myMeshFileTransform);
         }
         myAttachedBody.setMesh (
            myOriginalMesh, myOriginalMeshFileName,
            myOriginalMeshFileTransform);

         setWidget (myGeometrySelector, GeometryType.Mesh);
         rebuildGeometryWidgets (GeometryType.Mesh);
         repackWindow();
         Main.getMain().rerender();
      }
   }

   public void actionPerformed (ActionEvent e) {
      Object source = e.getSource();

      if (source == myCOMButton) {
         Vector3d com = (Vector3d)myCOMField.getValue();
         //System.out.println ("com=" + com);
         Vector3d offset =
            (Vector3d)myMeshXformWidget.getTranslationField().getValue();
         Vector3d newOffset = new Vector3d();
         newOffset.sub (offset, com);
         //System.out.println ("newOffset=" + newOffset);
         myMeshXformWidget.getTranslationField().setValue (newOffset);
      }
   }

   public void restoreDefaultValues() {
      myValueChangeHandlerMasked = true;
      
      GLViewer viewer = Main.getMain().getViewer();
      double width = viewer.distancePerPixel (viewer.getCenter()) * 
         viewer.getScreenWidth() / 6;
      
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
      
      myBoxWidthsField.setValue (new Vector3d (scale, scale, scale));
      myPointRadiusField.setValue (scale / 2);
      myPointSlicesField.setValue (12);
      
      myMesh = null;
      myMeshFileName = null;
      myMeshFileTransform = null;

      myMeshFileField.setValue (null);
      myMeshXformWidget.setValue (new RigidTransform3d());
      
      resetMesh();
      myValueChangeHandlerMasked = false;
   }
}
