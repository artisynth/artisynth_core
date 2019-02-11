package artisynth.demos.tutorial;

import java.awt.Color;
import java.util.HashSet;

import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.ElementFilter;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.materials.NeoHookeanMaterial;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;

public class PointFemAttachment extends RootModel {

   // create a zero-mass particle at (x,y,z) and add it to a mech model
   private Particle addParticle (MechModel mech, double x, double y, double z) {
      Particle p = new Particle (null, 0.0, x, y, z);
      RenderProps.setSphericalPoints (p, 0.015, Color.GREEN);
      p.setDynamic (false);
      mech.addParticle (p);
      return p;
   }

   // create a spring connecting a and b and add it to a mech model
   private void addSpring (MechModel mech, double stiffness, Point a, Point b) {
      AxialSpring spring = new AxialSpring (stiffness, 0, 0);
      RenderProps.setSpindleLines (spring, 0.01, Color.RED);
      mech.attachAxialSpring (a, b, spring);
   }

   // create an FEM beam model, translate it to (x,y,z), fix the leftmost
   // nodes, and add it to a mech model
   private FemModel3d addFem (MechModel mech, double x, double y, double z) {
      FemModel3d fem = FemFactory.createHexGrid (null, 1.0, 0.2, 0.2, 10, 3, 3);
      fem.setMaterial (new NeoHookeanMaterial ());
      //RenderProps.setSphericalPoints (fem, 0.005, Color.GREEN);
      RenderProps.setLineColor (fem, Color.BLUE);
      RenderProps.setLineWidth (fem, 2);
      mech.addModel (fem);
      fem.transformGeometry (new RigidTransform3d (x, y, z));

      // find and fix the leftmost elements:
      PointList<FemNode3d> nodes = fem.getNodes();
      for (int i=0; i<nodes.size(); i++) {
         FemNode3d n = nodes.get(i);
         if (Math.abs(n.getPosition().x-(-0.5+x)) < 1e-8) {
            n.setDynamic (false);
         }
      }
      return fem;
   }

   // Sets the nodes associated with a point's attachment to render as white
   // spheres.
   private void setAttachedNodesWhite (Point pnt) {
      // assume attachment is a PointFem3dAttachment
      PointFem3dAttachment ax = (PointFem3dAttachment)pnt.getAttachment();
      for (FemNode n : ax.getNodes()) {
         RenderProps.setSphericalPoints (n, 0.015, Color.WHITE);
      }
   }

   // Filter to select only elements for which the nodes are entirely on the
   // positive side of the x-z plane.
   private class MyFilter implements ElementFilter {

      public boolean elementIsValid (FemElement e) {
         for (FemNode n : e.getNodes()) {
            if (n.getPosition().y < 0) {
               return false;
            }
         }
         return true;         
      }
   }

   // Collect and return all the nodes of a FEM model associated with a
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
      mech.setGravity (0, 0, 0); // turn off gravity

      // create and add two FEM beam models centered at the specified locations
      FemModel3d fem1 = addFem (mech,  0.0, 0.0,  0.25);
      FemModel3d fem2 = addFem (mech,  0.0, 0.0, -0.25);

      // reconstruct the FEM surface meshes so that they show only elements on
      // the positive side of the x-y plane. Also, set surface rendering to
      // show strain values.
      fem1.createSurfaceMesh (new MyFilter());
      fem1.setSurfaceRendering (SurfaceRender.Strain);
      fem2.createSurfaceMesh (new MyFilter());
      fem2.setSurfaceRendering (SurfaceRender.Strain);

      // create and add the particles and markers for point-to-point springs
      // that will apply forces to each FEM.
      Particle p1 = addParticle (mech,  0.9, 0.0,  0.25);
      Particle p2 = addParticle (mech,  0.9, 0.0, -0.25);
      Particle m1 = addParticle (mech,  0.0, 0.0,  0.25);
      Particle m2 = addParticle (mech,  0.0, 0.0, -0.25);

      // attach spring end-point to fem1 using an element-based marker
      mech.attachPoint (m1, fem1);

      // attach spring end-point to fem2 using a larger number of nodes, formed
      // from the node set for elements 22, 31, 40, 49, and 58. This is done by
      // explicitly creating the attachment and then setting it to use the
      // specified nodes
      HashSet<FemNode3d> nodes =
         collectNodes (fem2, new int[] { 22, 31, 40, 49, 58 });

      PointFem3dAttachment ax = new PointFem3dAttachment (m2);
      ax.setFromNodes (m2.getPosition(), nodes);
      mech.addAttachment (ax);

      // finally, create the springs
      addSpring (mech, /*stiffness=*/10000, p1, m1);
      addSpring (mech, /*stiffness=*/10000, p2, m2);

      // set the attachments nodes for m1 and m2 to render as white spheres
      setAttachedNodesWhite (m1);
      setAttachedNodesWhite (m2);
      // set render properties for m1
      RenderProps.setSphericalPoints (m1, 0.015, Color.GREEN);
   }

}
