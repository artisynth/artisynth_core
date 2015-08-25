package artisynth.demos.tutorial;

import java.awt.Color;
import java.util.HashSet;
import java.util.LinkedHashSet;

import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import artisynth.core.femmodels.*;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;

public class FrameFemAttachment extends RootModel {

   // Collect and return all the nodes of a fem associated with a
   // set of elements specified by an array of element numbers
   private HashSet<FemNode3d> collectNodes (FemModel3d fem, int[] elemNums) {
      HashSet<FemNode3d> nodes = new HashSet<FemNode3d>();
      for (int i=0; i<elemNums.length; i++) {
         FemElement3d e = fem.getElements().getByNumber (elemNums[i]);
         for (FemNode3d n : e.getNodes()) {
            nodes.add (n);
         }
      }
      return nodes;
   }

   public void build (String[] args) {

      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create and add FEM beam 
      FemModel3d fem = FemFactory.createHexGrid (null, 1.0, 0.2, 0.2, 6, 3, 3);
      fem.setMaterial (new LinearMaterial (500000, 0.33));
      RenderProps.setLineColor (fem, Color.BLUE);
      RenderProps.setLineWidth (mech, 2);
      mech.addModel (fem);
      // fix leftmost nodes of the FEM
      for (FemNode3d n : fem.getNodes()) {
         if ((n.getPosition().x-(-0.5)) < 1e-8) {
            n.setDynamic (false);
         }
      }

      // create and add rigid body box
      RigidBody box = RigidBody.createBox (
         "box", 0.25, 0.1, 0.1, /*density=*/1000);
      mech.add (box);

      // create a basic frame and and set its pose and axis length
      Frame frame = new Frame();
      frame.setPose (new RigidTransform3d (0.4, 0, 0, 0, Math.PI/4, 0));
      frame.setAxisLength (0.3);
      mech.addFrame (frame);

      mech.attachFrame (frame, fem); // attach using element-based attachment

      // attach the box to the FEM, using all the nodes of elements 31 and 32
      HashSet<FemNode3d> nodes = collectNodes (fem, new int[] { 22, 31 });
      FrameFem3dAttachment attachment = new FrameFem3dAttachment(box);
      attachment.setFromNodes (box.getPose(), nodes);
      mech.addAttachment (attachment);

      // render the attachment nodes for the box as spheres
      for (FemNode n : attachment.getNodes()) {
         RenderProps.setSphericalPoints (n, 0.007, Color.GREEN);
      }
   }
}
