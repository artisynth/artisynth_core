package artisynth.core.opensim.components;

import java.awt.Color;
import java.io.File;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.ConstrainerBase;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.ComponentList;
import maspack.matrix.Vector3d;
import maspack.render.RenderableUtils;
import maspack.render.RenderProps;


public class Model3 extends ModelBase {
   
   @Override
   public Model3 clone () {
      return (Model3)super.clone ();
   }
   
   @Override
   public MechModel createModel (
      MechModel mech, File geometryPath, ModelComponentMap componentMap) {
      
      if (mech == null) {
         mech = new MechModel(getName ());
      }
      componentMap.put (this, mech);
      
      // bodies
      BodySet bodySet = this.getBodySet ();
      RenderableComponentList<RigidBody> bodies =
         bodySet.createComponent(geometryPath, componentMap);
      mech.add (bodies);
      
      // force effectors
      ForceSet forceSet = this.getForceSet ();
      RenderableComponentList<ModelComponent> forces =
         forceSet.createComponent(geometryPath, componentMap);
      mech.add (forces);
      
      // constrainers
      ConstraintSet constraintSet = this.getConstraintSet ();
      ComponentList<ConstrainerBase> constraints =
         constraintSet.createComponent(geometryPath, componentMap);
      mech.add (constraints);

      // markers
      MarkerSet markerSet = this.getMarkerSet ();
      PointList<FrameMarker> markers =
         markerSet.createComponent(geometryPath, componentMap);
      mech.add (markers);
      
      // set gravity
      Vector3d gravity = this.getGravity ();
      if (gravity != null) {
         mech.setGravity (gravity);
      }

      // set markers to render as spheres
      double modelRadius = RenderableUtils.getRadius (mech);
      RenderProps.setSphericalPoints (markers, 0.008*modelRadius, Color.CYAN);
      
      return mech;
      
   }
}
