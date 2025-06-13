package artisynth.core.opensim;

import java.awt.Color;
import java.io.File;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.ForceComponent;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.JointBase;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.*;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.gui.ControlPanel;
import artisynth.core.opensim.components.ModelBase;
import artisynth.core.opensim.components.ModelComponentMap;
import artisynth.core.opensim.components.OpenSimDocument;
import artisynth.core.opensim.components.OpenSimObjectFactory;
import artisynth.core.opensim.components.OpenSimObjectFactoryStore;
import maspack.render.*;
import maspack.render.Renderer.*;

/**
 * Reads an OpenSim model from a {@code .osim} file and associated geometry
 * folder, and creates the corresponding ArtiSynth components and arranges them
 * in a {@code MechModel}. The OpenSim component hierarchy is preserved, and
 * the resulting MechModel components can be queried after using methods
 * supplied by this parser. Not all OpenSim components are implemented, but
 * those that are include most of the commonly used components found in {@code
 * "bodyset"}, {@code "jointset"}, {@code "forceset"}, {@code "constraintset"},
 * and {@code "markers"}.
 */
public class OpenSimParser {
   
   public static final File DEFAULT_GEOMETRY_PATH =
      ArtisynthPath.getSrcRelativeFile (OpenSimParser.class, "geometry1/");

   Document myDOM; //The Document Object Model is stored here.
   File myOsimFile;
   OpenSimObjectFactoryStore myFactories;
   OpenSimDocument myDocument;
   File myGeometryPath;
   MechModel myMech; // most recently parsed (or set) MechModel
   ModelComponentMap myComponentMap;
   public static boolean myIgnoreFrameGeometry = true;
   public static boolean myFrameGeometryVisible = false;

   /**
    * Creates a new parser
    * @param file file to parse
    * @param geometryPath path to search for geometries
    * @param factories factory storage for generating OpenSim objects from file
    */
   public OpenSimParser (
      File file, File geometryPath, OpenSimObjectFactoryStore factories) {
      myDOM = null;
      myOsimFile = file;
      if (factories == null) {
         factories = new OpenSimObjectFactoryStore ();
      }
      myFactories = factories;
      myDocument = null;
      myGeometryPath = geometryPath;
   }

   /**
    * Creates a new OpenSim parser for a specified OpenSim (.osim) file and
    * geometry folder. Constructing the parser does not load the model; that
    * must be done after by {@link #createModel} or {@link
    * #createModel(MechModel}).
    * 
    * @param osimFile OpenSim file to parse
    * @param geometryPath path to the folder containing the OpenSim geometry
    */
   public OpenSimParser (File osimFile, File geometryPath) {
      this(osimFile, geometryPath, null);
   }

   /**
    * Creates a new OpenSim parser for a specified OpenSim (.osim) file.
    * Constructing the parser does not load the model; that must be done after
    * by {@link #createModel} or {@link #createModel(MechModel}). For model
    * loading to succeed, a geometry folder will most likely need to be
    * specified first using {@link #setGeometryPath}.
    * 
    * @param osimFile OpenSim file to parse
    */
   public OpenSimParser (File osimFile) {
      this(osimFile, null, null);
   }

   public void load() {
      parseOSimFile(); // get DOM
      parseDocument(); // create model
   }

   private void parseOSimFile() {
      // get factory 
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

      try {

         // Using factory get an instance of document builder
         DocumentBuilder db = dbf.newDocumentBuilder();

         // Parse using builder to get DOM representation of the XML-based OSim
         // file
         myDOM = db.parse(myOsimFile);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private void parseDocument() {
      //Get the root element. In this case the root OpenSimDocument
      Element root = myDOM.getDocumentElement();
      
      OpenSimObjectFactory<? extends OpenSimDocument> factory = 
         myFactories.findFactory (OpenSimDocument.class, root);
      
      myDocument = factory.parse (root);
   }

   /**
    * Sets the geometry path to be used in subsequent calls to {@link
    * #createModel} or {@link #createModel(MechModel}).
    *
    * @param geometryPath path to the folder containing the OpenSim geometry
    */
   public void setGeometryPath (File geometryPath) {
      myGeometryPath = geometryPath;
   }
   
   /**
    * Returns the geometry path currently used by this parser.
    *
    * @return path to the folder containing the OpenSim geometry
    */
   public File getGeometryPath() {
      return myGeometryPath;
   }

   public boolean getFrameGeometryVisible() {
      return myFrameGeometryVisible;
   }

   public void setFrameGeometryVisible (boolean enable) {
      myFrameGeometryVisible = enable;
   }

   public boolean getIgnoreFrameGeometry() {
      return myIgnoreFrameGeometry;
   }

   public void setIgnoreFrameGeometry (boolean ignore) {
      myIgnoreFrameGeometry = ignore;
   }

   /**
    * Populates a {@code MechModel} with ArtiSynth implementations of
    * components read from the OpenSim file, preserving the OpenSim names and
    * component hierarchy. Unrecognized or implemented components are indicated
    * by warning messages printed on the console.
    *
    * @param mech MechModel to populate. If {@code null}, then a new
    * model will be created.
    * @return populated model (either {@code mech}, or the newly created model)
    */
   public MechModel createModel(MechModel mech) {
    
      if (myDocument == null) {
         load();
      }
      ModelBase model = myDocument.getModel ();
      myComponentMap = new ModelComponentMap ();
      
      File geometryPath = myGeometryPath;
      if (geometryPath == null) {
         geometryPath = DEFAULT_GEOMETRY_PATH;
      }
      
      mech = model.createModel (mech, geometryPath, myComponentMap);
      myMech = mech;
      setAppropriateDefaults();
      return mech;
   }

   protected void setAppropriateDefaults () {
      // Set muscles and via points to render as dark red, with the muscles as
      // cylinders/spindles and the via points as spheres.
      double msize = RenderableUtils.getRadius (myMech);
      RenderableComponentList<ModelComponent> forceset = getForceSet();
      if (forceset != null) {
         RenderProps.setSpindleLines (
            forceset, 0.0045*msize, Color.RED.darker());
         RenderProps.setSphericalPoints (
            forceset, 0.006*msize, Color.RED.darker());
         RenderProps.setShading (forceset, Shading.SMOOTH);
      }
      RenderableComponentList<RigidBody> bodyset = getBodySet();
      if (bodyset != null) {
         for (RigidBody body : bodyset) {
            body.setAxisLength (0.2*msize);
         }
      }
      for (WrapComponent wobj : getWrapObjects()) {
         wobj.setAxisLength (0.2*msize);
      }
      PointList<FrameMarker> markerset = getMarkerSet();
      if (markerset != null) {
         RenderProps.setSphericalPoints (
            markerset, 0.008*msize, Color.PINK.darker());
      }
   }
   
   /**
    * Creates a {@code MechModel} and populates is with ArtiSynth
    * implementations of components read from the OpenSim file, preserving the
    * OpenSim names and component hierarchy. Unrecognized or implemented
    * components are indicated by warning messages printed on the console.
    *
    * @return created model
    */
   public MechModel createModel() {
      return createModel(null);
   }
   
   /**
    * Returns the ModelComponentMap for the most recently created model,
    * or {#code null} if no model has been created yet.
    * 
    * @return most recent component map.
    */
   public ModelComponentMap getComponentMap() {
      return myComponentMap;
   }

   /**
    * Returns the {@code MechModel} either most recently populated or created
    * by {@link #createModel(MechModel)} or {@link #createModel()}, or set
    * using {@link #setModelModel}.
    *
    * @return most recently populated or set model
    */
   public MechModel getMechModel() {
      return myMech;
   }

   /**
    * Explicitly sets the {@code MechModel} associated with this parser.
    *
    * @param mech MechModel to associated with this parser
    */
   public void setMechModel(MechModel mech) {
      myMech = mech;
   }

   private void checkForMechModel() {
      if (myMech == null) {
         throw new IllegalStateException (
            "No MechModel currently associated with the parser");
      }
   }

   protected <T> void recursivelyGetComponents (
      CompositeComponent comp, List<T> list, Class<T> type) {

      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (type.isAssignableFrom(c.getClass())) {
            T t = type.cast(c);      // checked cast
            list.add (t);
         }
         // sometimes a component can be a of type T
         // with subcomponents also of type T
         if (c instanceof CompositeComponent) {
            recursivelyGetComponents ((CompositeComponent)c, list, type);
         }
      }
   }

   // rigid bodies

   /**
    * Returns the top-level {@code "ground"} rigid body contained in the
    * MecModel associated with this parser.
    *
    * @return "ground" component, or {@code null} if not present.
    */
   public RigidBody getGround() {
      checkForMechModel();
      return (RigidBody)myMech.get ("ground"); 
   }

   /**
    * Returns the top-level {@code "bodyset"} rigid body list component
    * contained in the MecModel associated with this parser.
    *
    * @return "bodyset" component, or {@code null} if not present.
    */
   public RenderableComponentList<RigidBody> getBodySet() {
      checkForMechModel();
      return (RenderableComponentList<RigidBody>)myMech.get ("bodyset"); 
   }

   /**
    * Finds a specific rigid body within the "bodyset" by name.
    *
    * @param name name of the requested body 
    * @return body, or {@code null} if not found
    */
   public RigidBody findBody (String name) {
      RenderableComponentList<RigidBody> bodyset = getBodySet();
      if (bodyset != null) {
         return bodyset.get(name);
      }
      else {
         return null;
      }
   }

   /**
    * Convenience method to set the visibility of the coordinate frames of the
    * rigid bodies located beneath the "bodyset", plus "ground". The visibility
    * is controlled by the enumerated type {@link AxisDrawStyle}, and the
    * default value is {@code OFF}.
    *
    * @param style draw style for the coordinate frame
    * @param axisLength if {@code > 0}, sets the length of the coordinate
    * frames. Otherwise, the length is unchanged.
    */
   public void setBodyFramesVisible (
      AxisDrawStyle style, double axisLength) {

      RenderableComponentList<RigidBody> bodyset = getBodySet();
      if (bodyset != null) {
         for (RigidBody rb : bodyset) {
            rb.setAxisDrawStyle (style);
            if (axisLength != -1) {
               rb.setAxisLength (axisLength);
            }
         }
      }
      RigidBody ground = getGround();
      if (ground != null) {
         ground.setAxisDrawStyle (style);
         if (axisLength != -1) {
            ground.setAxisLength (axisLength);
         }
      }
   }

   // wrap objects

   /**
    * Returns a list of all wrap objects attached to the rigid bodies located
    * under the "bodyset".  In addition to being rigid bodies themselves, all
    * of the returned objects are instances of {@link Wrappable}.
    *
    * @return list of the wrap objects
    */
   public ArrayList<WrapComponent> getWrapObjects() {
      ArrayList<WrapComponent> list = new ArrayList<>();
      RenderableComponentList<RigidBody> bodyset = getBodySet();
      if (bodyset != null) {
         for (RigidBody rb : bodyset) {
            list.addAll (getBodyWrapObjects (rb));
         }
      }
      return list;
   }

   /**
    * Returns a list of the wrap objects attached to a specific rigid body.  An
    * empty list is returned if no wrap objects are attached to the body.  In
    * addition to being rigid bodies themselves, all of the returned objects
    * also implement {@link Wrappable}.
    *
    * @param body body for which wrap objects are requested
    * @return list of the body's wrap objects
    */
   public ArrayList<WrapComponent> getBodyWrapObjects (RigidBody body) {
      RenderableComponentList<WrapComponent> wrapobjs =
         (RenderableComponentList<WrapComponent>)body.get("wrapobjectset");
      ArrayList<WrapComponent> list = new ArrayList<>();
      if (wrapobjs != null && wrapobjs.numComponents() > 0) {
         for (WrapComponent wobj : wrapobjs) {
            list.add (wobj);
         }
      }
      return list;
   }

   /**
    * Convenience method to set the visibility of the all wrap objects attached
    * to the rigid bodies located under the "bodyset".
    *
    * @param visible if {@code true}, wrap objects are set visible.
    */
   public void setWrapObjectsVisible (boolean visible) {
      for (WrapComponent wo : getWrapObjects()) {
         RenderProps.setVisible (wo, visible);
      }
   }

   /**
    * Finds a specific wrap object by name.
    *
    * @param name name of the wrap object
    * @return wrap object, or {@code null} if not found
    */
   public WrapComponent findWrapObject (String name) {
      for (WrapComponent wo : getWrapObjects()) {
         if (name.equals (wo.getName())) {
            return wo;
         }
      }
      return null;
   }

   // joints 

   /**
    * Returns the top-level {@code "jointset"} joint list component contained
    * in the MecModel associated with this parser. Note: this component is only
    * present for OpenSim 4 models. For OpenSim 3 models, one can obtain the
    * joints using {@link #getJoints} instead.
    *
    * @return "jointset" component, or {@code null} if not present.
    */
   public RenderableComponentList<JointBase> getJointSet() {
      checkForMechModel();
      return (RenderableComponentList<JointBase>)myMech.get ("jointset"); 
   }

   /**
    * Returns a list of all the joint components located beneath the {@code
    * "jointset"} (OpenSim 4 models) or beneath the {@code "bodyset"} (OpenSim
    * 3 models). 
    *
    * @return "jointset" component, or {@code null} if not present.
    */
   public ArrayList<JointBase> getJoints() {
      checkForMechModel();
      ArrayList<JointBase> list = new ArrayList<>();
      RenderableComponentList<JointBase> jointset = getJointSet();
      if (jointset != null) {
         // OpenSim 4
         for (JointBase joint : jointset) {
            list.add (joint);
         }
      }
      else {
         // OpenSim 3
         RenderableComponentList<RigidBody> bodyset = getBodySet();
         if (bodyset != null) {
            for (RigidBody rb : bodyset) {
               RenderableComponentList<JointBase> joint =
                  (RenderableComponentList<JointBase>)rb.get("joint");
               if (joint != null && joint.size() > 0) {
                  list.add (joint.get(0));
               }
            }           
         }
      }
      return list;
   }

   /**
    * Finds a specific joint by name.
    *
    * @param name name of the requested joint 
    * @return joint, or {@code null} if not found
    */
   public JointBase findJoint (String name) {
      RenderableComponentList<JointBase> jointset = getJointSet();
      if (jointset != null) {
         return jointset.get(name);
      }
      else {
         for (JointBase joint : getJoints()) {
            if (name.equals (joint.getName())) {
               return joint;
            }
         }
         return null;
      }
   }

   // joint coordinates 

   /**
    * Returns a list of coordinate handles for all the joints returned
    * by {@link #getJoints}.
    *
    * @return list of handles for all coordinates
    */
   public ArrayList<JointCoordinateHandle> getCoordinates() {
      ArrayList<JointCoordinateHandle> list = new ArrayList<>();
      for (JointBase joint : getJoints()) {
         for (int idx=0; idx<joint.numCoordinates(); idx++) {
            JointCoordinateHandle ch = new JointCoordinateHandle (joint, idx);
            list.add (ch);                  
         }
      }
      return list;
   }

   /**
    * Returns a handle for a specific coordinate in one of the joints returned
    * by {@link #getJoints}.
    *
    * @param name name of the requested coordinate
    * @return coordinate handle, or {@code null} if not found
    */
   public JointCoordinateHandle findCoordinate (String name) {
      for (JointBase joint : getJoints()) {
         for (int idx=0; idx<joint.numCoordinates(); idx++) {
            if (name.equals (joint.getCoordinateName(idx))) {
               return new JointCoordinateHandle (joint, idx);
            }
         }
      }
      return null;
   }

   // force effectors

   /**
    * Returns the top-level {@code "forceset"} force effector list component
    * contained in the MecModel associated with this parser. Note: the
    * components in this list are sometimes composite components that contain
    * the actual ForceComponent as a descendent. To obtain an explicit
    * list of the force components, use {@link #getForceCompnents}.
    *
    * @return "forceset" component, or {@code null} if not present.
    */
   public RenderableComponentList<ModelComponent> getForceSet() {
      checkForMechModel();
      return (RenderableComponentList<ModelComponent>)myMech.get ("forceset"); 
   }

   /**
    * Returns a list of the all the {@code ForceComponent} located beneath the
    * {@code "forceset"}. If the forceset is not present, an empty list is
    * returned.
    * 
    * @return list of force components in the "forceset"
    */
   public ArrayList<ForceComponent> getForceComponents() {
      ArrayList<ForceComponent> list = new ArrayList<>();
      RenderableComponentList<ModelComponent> forceset = getForceSet();
      if (forceset != null) {
         forceset.recursivelyFind (list, ForceComponent.class);
      }
      return list;
   }
   
   /**
    * Returns a list of the all the muscle and and line-based spring components
    * located beneath the {@code "forceset"}. If the forceset is not present,
    * an empty list is returned.
    * 
    * @return list of muscle and line-based springs in the "forceset"
    */
   public ArrayList<PointSpringBase> getMusclesAndSprings() {
      ArrayList<PointSpringBase> list = new ArrayList<>();
      RenderableComponentList<ModelComponent> forceset = getForceSet();
      if (forceset != null) {
         forceset.recursivelyFind (list, PointSpringBase.class);
      }
      return list;
   }

   /**
    * Finds a specific force component by name.
    *
    * @param name name of the force component
    * @return force component, or {@code null} if not found
    */
   public ForceComponent findForceComponent (String name) {
      for (ForceComponent comp : getForceComponents()) {
         if (name.equals (comp.getName())) {
            return comp;
         }
      }
      return null;
   }

   /**
    * Finds a specific muscle or line-based spring by name.
    *
    * @param name name of the muscle/spring
    * @return muscle/spring, or {@code null} if not found
    */
   public PointSpringBase findMuscleOrSpring (String name) {
      for (PointSpringBase psb : getMusclesAndSprings()) {
         if (name.equals (psb.getName())) {
            return psb;
         }
      }
      return null;
   }

   /**
    * Convenience method to zero the excitations of all muscle
    * components located beneath the {@code "forceset"}. This is
    * useful when a newly loaded model contains small bias excitations.
    * 
    * @param mech
    */
   public void zeroMuscleExcitations() {
      for (PointSpringBase psb : getMusclesAndSprings()) {
         if (psb instanceof ExcitationComponent) {
            ExcitationComponent ec = (ExcitationComponent)psb;
            ec.setExcitation (0);
         }
      }
   }

   /**
    * Returns a list of all the wrap objects associated with a specific muscle
    * or spring. If there are no wrap objects, or if the muscle is not an
    * instance of {@link MultiPointSpring} or {@link MultiPointMuscle},
    * then an empty list is returned. All of the returned wrap objects
    * 
    * 
    * @param muscle muscle or line-based spring to query
    * @return list of associated wrap objects
    */
   public List<WrapComponent> getMuscleWrapObjects (PointSpringBase muscle) {
      ArrayList<WrapComponent> list = new ArrayList<>();
      if (muscle instanceof MultiPointSpring) {
         MultiPointSpring mspr = (MultiPointSpring)muscle;
         for (int i=0; i<mspr.numWrappables(); i++) {
            if (mspr.getWrappable(i) instanceof WrapComponent) {
               list.add ((WrapComponent)mspr.getWrappable(i));
            }
         }
      }
      return list;
   }
  
   // constraints

   /**
    * Returns the top-level {@code "constraintset"} constrainer list component
    * contained in the MecModel associated with this parser.
    *
    * @return "constraintset" component, or {@code null} if not present.
    */
   public RenderableComponentList<ConstrainerBase> getConstraintSet() {
      checkForMechModel();
      return (RenderableComponentList<ConstrainerBase>)
         myMech.get("constraintset"); 
   }

   /**
    * Finds a specific constraint by name.
    *
    * @param name name of the constraint
    * @return constraint, or {@code null} if not found
    */
   public ConstrainerBase findConstraint (String name) {
      RenderableComponentList<ConstrainerBase> constraintset =
         getConstraintSet();
      if (constraintset != null) {
         return constraintset.get(name);
      }
      else {
         return null;
      }
   }

   // markers

   /**
    * Returns the top-level {@code "markerset"} marker list component
    * contained in the MecModel associated with this parser.
    *
    * @return "markerset" component, or {@code null} if not present.
    */
   public PointList<FrameMarker> getMarkerSet() {
      checkForMechModel();
      return (PointList<FrameMarker>)myMech.get("markerset"); 
   }

   /**
    * Finds a specific marker by name.
    *
    * @param name name of the marker
    * @return marker, or {@code null} if not found
    */
   public FrameMarker findMarker (String name) {
      PointList<FrameMarker> markerset = getMarkerSet();
      if (markerset != null) {
         return markerset.get(name);
      }
      else {
         return null;
      }
   }

   // control panel methods

   /**
    * Creates a control panel for all the muscle excitations.
    *
    * @return created panel
    */
   public ControlPanel createExcitationPanel() {
      ControlPanel panel = new ControlPanel();
      panel.setName ("excitations");
      for (PointSpringBase psb : getMusclesAndSprings()) {
         if (psb instanceof MuscleComponent) {
            MuscleComponent muscle = (MuscleComponent)psb;
            panel.addWidget (muscle.getName(), muscle, "excitation");
         }
      }
      return panel;
   }

   /**
    * Creates a control panel for the excitations of all the names muscles.
    *
    * @param muscleNames names of the muscles for which excitation controls
    * should be created
    * @return created panel
    */
   public ControlPanel createExcitationPanel (String[] muscleNames) {
      HashSet<String> nameSet = new HashSet<>();
      for (String name : muscleNames) {
         nameSet.add (name);
      }
      ControlPanel panel = new ControlPanel();
      panel.setName ("excitations");
      for (PointSpringBase psb : getMusclesAndSprings()) {
         if (psb instanceof MuscleComponent && nameSet.contains(psb.getName())) {
            MuscleComponent muscle = (MuscleComponent)psb;
            panel.addWidget (muscle.getName(), muscle, "excitation");
         }
      }
      return panel;
   }

   /**
    * Update wrap paths for all muscles. This can be called after one or more
    * components are repositioned, in order to ensure that the wrap paths
    * remain consistent.
    */
   public void updateMusclePaths() {
      for (PointSpringBase spr : getMusclesAndSprings()) {
         if (spr instanceof MultiPointMuscle) {
            ((MultiPointMuscle)spr).updateWrapSegments();
         }
      }
   }
   
}


