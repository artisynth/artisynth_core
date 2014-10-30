package artisynth.demos.tutorial;

import java.awt.Color;
import maspack.matrix.*;
import maspack.render.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.workspace.RootModel;

/**
 * Demo of two particles connected by a spring
 */
public class ParticleSpring extends RootModel {

   public void build (String[] args) {

      System.out.println (args.length+" args:");
      for (String a : args) {
         System.out.println (" "+a);
      }

      // create MechModel and add to RootModel
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create the components
      Particle p1 = new Particle ("p1", /*mass=*/2, /*x,y,z=*/0, 0, 0);
      Particle p2 = new Particle ("p2", /*mass=*/2, /*x,y,z=*/1, 0, 0);
      AxialSpring spring = new AxialSpring ("spr", /*restLength=*/0);
      spring.setPoints (p1, p2);
      spring.setMaterial (
         new LinearAxialMaterial (/*stiffness=*/20, /*damping=*/10));

      // add components to the mech model
      mech.addParticle (p1);
      mech.addParticle (p2);
      mech.addAxialSpring (spring);

      p1.setDynamic (false);                // first particle set to be fixed

      // increase model bounding box for the viewer
      mech.setBounds (/*min=*/-1, 0, -1, /*max=*/1, 0, 0);  
      // set render properties for the components
      setPointRenderProps (p1);            
      setPointRenderProps (p2);
      setLineRenderProps (spring);
   }

   protected void setPointRenderProps (Renderable r) {
      RenderProps.setPointColor (r, Color.RED);
      RenderProps.setPointStyle (r, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (r, 0.06);
   }

   protected void setLineRenderProps (Renderable r) {
      RenderProps.setLineColor (r, Color.BLUE);
      RenderProps.setLineStyle (r, RenderProps.LineStyle.CYLINDER);
      RenderProps.setLineRadius (r, 0.02);
   }
}
