package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.Wrappable;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;

public abstract class WrapObject extends HasVisibleObjectOrAppearance {

   private Point3d xyz_body_rotation;
   private Point3d translation;
   private boolean active;
   private String quadrant;

   public WrapObject() {
      xyz_body_rotation = null;
      translation = null;
      active = true;
      quadrant = "all";
      visibleObject = null;
   }

   public Point3d getXYZBodyRotation () {
      return xyz_body_rotation;
   }
   
   public void setXYZBodyRotation(Point3d xyz) {
      xyz_body_rotation = xyz;
   }
   
   /**
    * Creates a transform representation of the translation and rotation components
    */
   public RigidTransform3d getTransform() {
      RigidTransform3d trans = new RigidTransform3d();
      if (translation != null) {
         trans.setTranslation (translation);
      }
      if (xyz_body_rotation != null) {
         OpenSimObjectFactory.setOrientationXYZ (trans.R, xyz_body_rotation);
      }
      return trans;
   }
   
   public Point3d getTranslation() {
      return translation;
   }
   
   public void setTranslation(Point3d pos) {
      translation = pos;
   }

   public boolean isActive () {
      return active;
   }

   public void setActive (boolean active) {
      this.active = active;
   }
   
   public String getQuadrant () {
      return quadrant;
   }

   public void setQuadrant (String quadrant) {
      this.quadrant = quadrant;
   }
   
   @Override
   public WrapObject clone () {
      WrapObject wo = (WrapObject)super.clone ();
      
      if (xyz_body_rotation != null) {
         wo.setXYZBodyRotation (xyz_body_rotation.clone ());
      }
      if (translation != null) {
         wo.setTranslation (translation.clone ());
      }
      wo.setActive (active);
      wo.setQuadrant (quadrant);
      
      return wo;
   }
   
   public abstract Wrappable createComponent(File geometryPath, ModelComponentMap componentMap);

}
