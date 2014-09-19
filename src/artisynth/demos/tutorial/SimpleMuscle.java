package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import maspack.matrix.*;
import maspack.render.*;

import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.workspace.RootModel;

/**
 * Simple demo of a particle and rigid body connected by an axial muscle.  This
 * is the same as RigidBodySpring except with 
 */
public class SimpleMuscle extends RootModel
{

   MechModel mech;
   Particle p1;
   RigidBody box;
   Muscle muscle;

   public void build (String[] args) throws IOException {

      // create MechModel and add to RootModel
      mech = new MechModel ("mech");
      addModel (mech);

      // create the components
      p1 = new Particle ("p1", /*mass=*/2, /*x,y,z=*/0, 0, 0);
      // create box and set its pose (position/orientation):
      RigidBody box =
         RigidBody.createBox ("box", /*wx,wy,wz=*/0.5, 0.3, 0.3, /*density=*/20);
      box.setPose (new RigidTransform3d (/*x,y,z=*/0.75, 0, 0));
      // create marker point and connect it to the box:
      FrameMarker mkr = new FrameMarker (/*x,y,z=*/-0.25, 0, 0);
      mkr.setFrame (box);

      // create the muscle:      
      muscle = new Muscle ("mus", /*restLength=*/0);
      muscle.setPoints (p1, mkr);
      muscle.setMaterial (
         new SimpleAxialMuscle (/*stiffness=*/20, /*damping=*/10, /*maxf=*/10));

      // add components to the mech model
      mech.addParticle (p1);
      mech.addRigidBody (box);
      mech.addFrameMarker (mkr);
      mech.addAxialSpring (muscle);

      p1.setDynamic (false);               // first particle set to be fixed

      // increase model bounding box for the viewer
      mech.setBounds (/*min=*/-1, 0, -1, /*max=*/1, 0, 0);  
      // set render properties for the components
      setPointRenderProps (p1);
      setPointRenderProps (mkr);
      setLineRenderProps (muscle);
   }

   protected void setPointRenderProps (Renderable r) {
      RenderProps.setPointColor (r, Color.BLUE);
      RenderProps.setPointStyle (r, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (r, 0.06);
   }

   protected void setLineRenderProps (Renderable r) {
      RenderProps.setLineColor (r, Color.RED);
      RenderProps.setLineStyle (r, RenderProps.LineStyle.ELLIPSOID);
      RenderProps.setLineRadius (r, 0.02);
   }

}
