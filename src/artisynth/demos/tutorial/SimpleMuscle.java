package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import maspack.matrix.*;
import maspack.render.*;

import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.workspace.RootModel;

public class SimpleMuscle extends RootModel
{

   MechModel mech;
   Particle p1;
   RigidBody box;
   Muscle muscle;

   public void build (String[] args) throws IOException {

      mech = new MechModel ("mech");

      p1 = new Particle ("p1", /*mass=*/2, 0, 0, 0);
      box = RigidBody.createBox ("box", 0.5, 0.3, 0.3, /*density=*/20);
      box.setPose (new RigidTransform3d (1.00, 0, 0));
      FrameMarker mkr = new FrameMarker (-0.25, 0, 0);
      mkr.setFrame (box);

      muscle = new Muscle ("mus", /*restLength=*/0);
      muscle.setPoints (p1, mkr);
      muscle.setMaterial (
         new SimpleAxialMuscle (/*stiffness=*/20, /*damping=*/10, /*maxf=*/10));

      mech.addParticle (p1);
      mech.addRigidBody (box);
      mech.addFrameMarker (mkr);
      mech.addAxialSpring (muscle);

      p1.setDynamic (false);
      mech.setBounds (-1, 0, -1, 1, 0, 0);
      setPointRenderProps (p1);
      setPointRenderProps (mkr);
      setSpringRenderProps (muscle);

      addModel (mech);
   }

   protected void setPointRenderProps (Point p) {
      RenderProps.setPointColor (p, Color.BLUE);
      RenderProps.setPointStyle (p, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (p, 0.06);
   }

   protected void setSpringRenderProps (AxialSpring s) {
      RenderProps.setLineColor (s, Color.RED);
      RenderProps.setLineStyle (s, RenderProps.LineStyle.ELLIPSOID);
      RenderProps.setLineRadius (s, 0.02);
   }

}
