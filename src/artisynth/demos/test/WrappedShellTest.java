package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.femmodels.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class WrappedShellTest extends RootModel {

   private double DTOR = Math.PI/180.0;  // degress to radians

   /**
    * Adds points, markers and springs to hold the cylinder in place
    * independently of the sheet.
    */
   private void addSprings (
      MechModel mech, RigidBody cylinder, double r, double h) {

      // Create markers at both ends of the cylinder
      FrameMarker mkr0 = mech.addFrameMarkerWorld (
         cylinder, new Point3d (0, h/2, 0));
      FrameMarker mkr1 = mech.addFrameMarkerWorld (
         cylinder, new Point3d (0, -h/2, 0));
      
      // Create 8 points at the indicated positions:
      Point3d[] partPos = new Point3d[] {
         new Point3d ( 3*r,  h/2,  3*r),
         new Point3d ( 3*r,  h/2, -3*r),
         new Point3d (-3*r,  h/2, -3*r),
         new Point3d (-3*r,  h/2,  3*r),
         new Point3d ( 3*r, -h/2,  3*r),
         new Point3d ( 3*r, -h/2, -3*r),
         new Point3d (-3*r, -h/2, -3*r),
         new Point3d (-3*r, -h/2,  3*r)
      };
      for (Point3d p : partPos) {
         Particle part = new Particle (1.0, p);
         part.setDynamic (false);
         mech.addParticle (part);
      }

      // create springs connecting the first four points and the first marker.
      for (int i=0; i<4; i++) {
         Particle part = mech.particles().get(i);
         AxialSpring spr = new AxialSpring();
         spr.setMaterial (new LinearAxialMaterial (1000.0, 0));
         mech.attachAxialSpring (mkr0, part, spr);
      }
      // create springs connecting the next four points and the second marker.
      for (int i=4; i<8; i++) {
         Particle part = mech.particles().get(i);
         AxialSpring spr = new AxialSpring();
         spr.setMaterial (new LinearAxialMaterial (1000.0, 0));
         mech.attachAxialSpring (mkr1, part, spr);
      }
   }


   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      double r = 0.1; // cylinder radius
      double h = 1.0; // cylinder height

      // create the cylinder and add it to the mech model
      RigidBody cylinder = RigidBody.createCylinder (
         "cylinder", r, h, /*density=*/1000, /*nsides=*/36);
      cylinder.setPose (new RigidTransform3d (0, 0, 0,  0, 0, DTOR*90.0));
      cylinder.setFrameDamping (200.0);
      mech.addRigidBody (cylinder);

      // add springs to hold it in place. If these aren't needed, you
      // can comment out this method
      addSprings (mech, cylinder, r, h);

      // create a PolygonalMesh that will be used to define the shell model
      PolygonalMesh mesh = MeshFactory.createQuadRectangle (
         9*r, 6*r, 18, 12);
      // flip the mesh on its side and move it to the left
      mesh.transform (new RigidTransform3d (-r, 0, 0,  0, DTOR*90.0, 0));

      // then adjust the mesh vertex positions so that the mesh wraps around
      // the cylinder and narrows at the top and bottom. Keep track which nodes
      // will be attached to the cylinder, as well as the ones at the top and
      // bottom that need to be fixed. For this, we used the fact that the
      // index values for the vertices will be same as those for the FEM nodes
      // when they are created.
      double EPS = 1e-8;
      ArrayList<Integer> fixedNodeIndices = new ArrayList<Integer>();
      ArrayList<Integer> attachedNodeIndices = new ArrayList<Integer>();
      double sx = 2*(1-Math.cos (DTOR*30));
      double sy = 0.5/(3*r);
      for (Vertex3d vtx : mesh.getVertices()) {
         Point3d pos = vtx.getPosition();
         if (Math.abs (pos.z) <= EPS) {
            attachedNodeIndices.add (vtx.getIndex());
         }
         else if (Math.abs (pos.z-0.05) <= EPS) {
            attachedNodeIndices.add (vtx.getIndex());
         }
         else if (pos.z > 0) {
            if (Math.abs (pos.z-0.3) <= EPS) {
               fixedNodeIndices.add (vtx.getIndex());
            }
         }
         else if (Math.abs (pos.z+0.05) <= EPS) {
            attachedNodeIndices.add (vtx.getIndex());
         }
         else { // (pos.z < 0) 
            if (Math.abs (pos.z+0.3) <= EPS) {
               fixedNodeIndices.add (vtx.getIndex());
            }
         }
         // adjust x and y vertex positions
         pos.x += sx*Math.abs(pos.z);
         pos.y *= (1-sy*Math.abs(pos.z));
      }
      mesh.notifyVertexPositionsModified();
   
      // now create the FEM sheet from the mesh.
      FemModel3d sheet = FemFactory.createShellModel (
         null, mesh, /*thickness=*/0.1*r, /*membrane=*/false);
      sheet.setDensity (1000.0);
      sheet.setMaterial (new NeoHookeanMaterial (5000000.0, 0.45));
      mech.addModel (sheet);
      // attach the middle nodes to the cylinder, and fix the top and bottom
      // nodes:
      for (Integer idx : fixedNodeIndices) {
         sheet.getNode (idx).setDynamic (false);
      }
      for (Integer idx : attachedNodeIndices) {
         mech.attachPoint (sheet.getNode(idx), cylinder);
      }

      //
      // Set rendering properties:
      //
      // Render the sheet surface mesh, front and back. and make it light blue
      sheet.setSurfaceRendering (FemModel.SurfaceRender.Shaded);
      RenderProps.setFaceStyle (sheet, FaceStyle.FRONT_AND_BACK);
      RenderProps.setFaceColor (sheet, new Color (0.6f, 0.6f, 1.0f));

      // Render sheet nodes as green spheres with a radius of 0.05 r,
      // and sheet element edges as blue
      RenderProps.setSphericalPoints (sheet, 0.05*r, Color.GREEN);
      RenderProps.setLineColor (sheet, Color.BLUE);

      // Render the points and markers as white spheres with a radius of 0.2 r,
      // and the axial springs as red cylinders with a radius of 0.1 r
      RenderProps.setSphericalPoints (mech, 0.2*r, Color.WHITE);
      RenderProps.setCylindricalLines (mech, 0.1*r, Color.RED);
   }

}
