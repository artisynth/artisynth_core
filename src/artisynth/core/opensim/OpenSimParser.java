package artisynth.core.opensim;

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
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.*;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.opensim.components.ModelBase;
import artisynth.core.opensim.components.ModelComponentMap;
import artisynth.core.opensim.components.OpenSimDocument;
import artisynth.core.opensim.components.OpenSimObjectFactory;
import artisynth.core.opensim.components.OpenSimObjectFactoryStore;
import maspack.render.*;

public class OpenSimParser {
   
   public static final File DEFAULT_GEOMETRY_PATH = ArtisynthPath.getSrcRelativeFile (
      OpenSimParser.class, "geometry1/");

   Document myDOM; //The Document Object Model is stored here.
   File myOsimFile;
   OpenSimObjectFactoryStore myFactories;
   OpenSimDocument myDocument;
   File myGeometryPath;
   ModelComponentMap myComponentMap;
   public static boolean myIgnoreFrameGeometry = true;
   public static boolean myFrameGeometryVisible = false;

   /**
    * Creates a new parser
    * @param file file to parse
    * @param geometryPath path to search for geometries
    * @param factories factory storage for generating OpenSim objects from file
    */
   public OpenSimParser(File file, File geometryPath, OpenSimObjectFactoryStore factories) {
      myDOM = null;
      myOsimFile = file;
      if (factories == null) {
         factories = new OpenSimObjectFactoryStore ();
      }
      myFactories = factories;
      myDocument = null;
      myGeometryPath = geometryPath;
   }
   
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

         // Parse using builder to get DOM representation of the XML-based OSim file
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
   
   public void setGeometryPath(File path) {
      myGeometryPath = path;
   }
   
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

   public void setIgnoreGeometryVisible (boolean ignore) {
      myIgnoreFrameGeometry = ignore;
   }

   /**
    * Populates the provided model, creating one if it doesn't exist
    * @param mech mechanical model
    * @return completed model
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
      
      return model.createModel (mech, geometryPath, myComponentMap);

   }
   
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

   public ArrayList<ForceComponent> getForceComponents(MechModel mech) {
      ArrayList<ForceComponent> list = new ArrayList<>();
      recursivelyGetComponents (mech, list, ForceComponent.class);
      return list;
   }
   
   public ArrayList<PointSpringBase> getMusclesAndSprings(MechModel mech) {
      ArrayList<PointSpringBase> list = new ArrayList<>();
      recursivelyGetComponents (mech, list, PointSpringBase.class);
      return list;
   }

   /**
    * Return the list of rigid bodies.
    */
   public RenderableComponentList<RigidBody> getBodySet(MechModel mech) {
      return (RenderableComponentList<RigidBody>)mech.get ("bodyset"); 
   }

   /**
    * Return the force set list.
    */
   public RenderableComponentList<ModelComponent> getForceSet(MechModel mech) {
      return (RenderableComponentList<ModelComponent>)mech.get ("forceset"); 
   }


   /**
    * Return the list of constraints.
    */
   public ComponentList<ConstrainerBase> getConstraintSet(MechModel mech) {
      return (ComponentList<ConstrainerBase>)mech.get ("constraintset"); 
   }

   public void zeroExcitations(MechModel mech) {
      for (PointSpringBase psb : getMusclesAndSprings(mech)) {
         if (psb instanceof ExcitationComponent) {
            ExcitationComponent ec = (ExcitationComponent)psb;
            ec.setExcitation (0);
         }
      }
   }
   
   public ArrayList<RigidBody> getWrapObjects(MechModel mech) {
      ArrayList<RigidBody> list = new ArrayList<>();
      for (RigidBody rb : getBodySet(mech)) {
         RenderableComponentList<RigidBody> wrapobjs =
            (RenderableComponentList<RigidBody>)rb.get("wrapobjectset");
         if (wrapobjs != null && wrapobjs.numComponents() > 0) {
            for (RigidBody wobj : wrapobjs) {
               list.add (wobj);
            }
         }           
      }
      return list;
   }

   public void setWrapObjectsVisible (MechModel mech, boolean visible) {
      for (RigidBody wo : getWrapObjects(mech)) {
         RenderProps.setVisible (wo, visible);
      }
   }
   
}


