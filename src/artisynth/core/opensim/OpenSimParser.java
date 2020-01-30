package artisynth.core.opensim;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.opensim.components.ModelBase;
import artisynth.core.opensim.components.ModelComponentMap;
import artisynth.core.opensim.components.OpenSimDocument;
import artisynth.core.opensim.components.OpenSimObjectFactory;
import artisynth.core.opensim.components.OpenSimObjectFactoryStore;

public class OpenSimParser {
   
   public static final File DEFAULT_GEOMETRY_PATH = ArtisynthPath.getSrcRelativeFile (
      OpenSimParser.class, "geometry1/");

   Document myDOM; //The Document Object Model is stored here.
   File myOsimFile;
   OpenSimObjectFactoryStore myFactories;
   OpenSimDocument myDocument;
   File myGeometryPath;

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
      ModelComponentMap componentMap = new ModelComponentMap ();
      
      File geometryPath = myGeometryPath;
      if (geometryPath == null) {
         geometryPath = DEFAULT_GEOMETRY_PATH;
      }
      
      return model.createModel (mech, geometryPath, componentMap);

   }
   
   public MechModel createModel() {
      return createModel(null);
   }
}


