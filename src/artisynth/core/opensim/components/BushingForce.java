package artisynth.core.opensim.components;

import java.io.File;
import java.util.ArrayList;

import artisynth.core.mechmodels.FrameSpring;
import artisynth.core.mechmodels.Frame;
import artisynth.core.materials.LinearFrameMaterial;
import artisynth.core.materials.FrameMaterial;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.opensim.components.JointBase.BodyAndTransform;
import maspack.matrix.*;

public class BushingForce extends BushingForceBase {

   private Vector3d translational_stiffness;
   private Vector3d rotational_stiffness;

   public BushingForce() {
      super();
      translational_stiffness = null;
      rotational_stiffness = null;
   }
   
   public void setTranslationalStiffness (Vector3d kvec) {
      translational_stiffness = kvec;
   }

   public Vector3d getTranslationalStiffness() {
      return translational_stiffness;
   }

   public void setRotationalStiffness (Vector3d kvec) {
      rotational_stiffness = kvec;
   }

   public Vector3d getRotationalStiffness() {
      return rotational_stiffness;
   }
   
   @Override
   public BushingForce clone () {
      
      BushingForce bushing = (BushingForce)super.clone ();
      
      if (translational_stiffness != null) {
         bushing.setTranslationalStiffness (translational_stiffness.clone());
      }
      if (rotational_stiffness != null) {
         bushing.setRotationalStiffness (rotational_stiffness.clone());
      }
      return bushing;
   }

   protected FrameMaterial createMaterial() {
      LinearFrameMaterial mat = new LinearFrameMaterial();
      mat.setStiffness (translational_stiffness);
      mat.setDamping (translational_damping);
      mat.setRotaryStiffness (rotational_stiffness);
      mat.setRotaryDamping (rotational_damping);
      return mat;
   }

}
