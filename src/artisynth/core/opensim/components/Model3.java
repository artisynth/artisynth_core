package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import maspack.matrix.Vector3d;

public class Model3 extends ModelBase {
   
   @Override
   public Model3 clone () {
      return (Model3)super.clone ();
   }
   
   @Override
   public MechModel createModel (MechModel mech,
      File geometryPath, ModelComponentMap componentMap) {
      
      if (mech == null) {
         mech = new MechModel(getName ());
      }
      componentMap.put (this, mech);
      
      // bodies
      BodySet bodySet = this.getBodySet ();
      RenderableComponentList<RigidBody> bodies = bodySet.createComponent(geometryPath, componentMap);
      mech.add (bodies);
      
      // force effectors
      ForceSet forceSet = this.getForceSet ();
      RenderableComponentList<ModelComponent> forces = forceSet.createComponent(geometryPath, componentMap);
      mech.add (forces);
      
      // markers
      // MarkerSet markerSet = this.getMarkerSet ();
      
      // set gravity
      Vector3d gravity = this.getGravity ();
      if (gravity != null) {
         mech.setGravity (gravity);
      }
      
      return mech;
      
   }
}
