package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.MechModel;
import maspack.fileutil.uri.URIx;
import maspack.matrix.Vector3d;

public abstract class ModelBase extends OpenSimObject implements ModelComponentGenerator<MechModel> {

   int version;
   
   private String credits;
   private String publications;
   private String length_units;
   private String force_units;
   
   private Vector3d gravity;
   
   private BodySet bodySet;
   private ForceSet forceSet;
   
   // private ConstraintSet constraintSet;
   private MarkerSet markerSet;
   
   // private ContactGeometrySet contactGeometrySet;
   // private ControllerSet controllerSet;
   // private ComponentSet componentSet;
   // private ProbeSet probeSet;

   // defaults
   
   public ModelBase() {
      version = 0;
      credits = null;
      publications = null;
      length_units = "meters";
      force_units = "N";
      gravity = null;
      bodySet = null;
      forceSet = null;
      markerSet = null;
   }
   
   /**
    * Root path "/"
    * @return root path
    */
   public String getPath () {
      return "/";
   }
   
   /**
    * OpenSim model version
    * @return version
    */
   public int getVersion () {
      return version;
   }
   
   /**
    * Sets OpenSim model version
    * @param version version ID
    */
   public void setVersion(int version) {
      this.version = version;
   }

   public String getCredits () {
      return credits;
   }

   public void setCredits (String credits) {
      this.credits = credits;
   }

   public String getPublications () {
      return publications;
   }

   public void setPublications (String publications) {
      this.publications = publications;
   }

   public String getLengthUnits () {
      return length_units;
   }

   public void setLengthUnits (String length_units) {
      this.length_units = length_units;
   }

   public String getForceUnits () {
      return force_units;
   }

   public void setForceUnits (String force_units) {
      this.force_units = force_units;
   }

   public Vector3d getGravity () {
      return gravity;
   }

   public void setGravity (Vector3d gravity) {
      this.gravity = gravity;
   }

   public BodySet getBodySet () {
      return bodySet;
   }

   public void setBodySet (BodySet bodySet) {
      this.bodySet = bodySet;
      this.bodySet.setParent (this);
   }

   public ForceSet getForceSet () {
      return forceSet;
   }

   public void setForceSet (ForceSet forceSet) {
      this.forceSet = forceSet;
      this.forceSet.setParent (this);
   }

   public MarkerSet getMarkerSet () {
      return markerSet;
   }

   public void setMarkerSet (MarkerSet markerSet) {
      this.markerSet = markerSet;
      this.markerSet.setParent (this);
   }

   @Override
   public ModelBase clone () {
      ModelBase model = (ModelBase) super.clone ();
      
      if (bodySet != null) {
         model.setBodySet (bodySet.clone ());
      }
      if (forceSet != null) {
         model.setForceSet (forceSet.clone ());
      }
      if (markerSet != null) {
         model.setMarkerSet (markerSet.clone ());
      }
      
      return model;
   }
   
   @Override
   public MechModel createComponent (File geometryPath, ModelComponentMap componentMap) {
      MechModel mech = new MechModel(getName());
      return createModel(mech, geometryPath, componentMap);
   }

   /**
    * Populates a mech model, creating a new one if necessary
    * @param mech model to populate
    * @param geometryPath path to search for geometry files
    * @param componentMap map of objects to components
    * @return populated or created model
    */
   public abstract MechModel createModel (
      MechModel mech, File geometryPath, ModelComponentMap componentMap);
   
}
