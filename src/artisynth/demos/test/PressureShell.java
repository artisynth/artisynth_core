package artisynth.demos.test;

import java.awt.Color;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.widgets.*;
import maspack.properties.*;
import maspack.render.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.workspace.*;
import artisynth.core.gui.*;

/**
 * A demo that creates a spherical FEM shell which then expands or contracts in
 * response to a uniform interior pressure. The shell also falls under gravity
 * and collides with a flat table.
 */
public class PressureShell extends RootModel {

   // all units are MKS for this demo

   double density = 1000;      // density for the objects
   double radius = 0.1;        // radius of the sphere
   double thickness = 0.02;    // thickness of the shell

   double length = 0.6;        // table length
   double depth = 0.04;        // table depth
   double width = 0.4;         // table width

   // Faces of the interior part of the shell surface
   ArrayList<Face> myInteriorFaces = new ArrayList<Face>();

   /**
    * This controller sets the forces of the nodes of the interior surface in
    * response to the (uniform) pressure inside the shell.
    */
   private class PressureController extends ControllerBase {

      FemModel3d myFem;

      PressureController (FemModel3d fem) {
         myFem = fem;
      }

      public void apply (double t0, double t1) {
         FemMeshComp meshc = myFem.getSurfaceMeshComp();
         Vector3d f = new Vector3d();
         for (Face face : myInteriorFaces) {
            f.scale (-face.computeArea()*getPressure(), face.getNormal());
            for (Vertex3d vtx : face.getVertices()) {
               FemNode3d n = meshc.getNodeForVertex (vtx);
               n.addForce (f);
            }
         }
      }
   }

   double myPressure = 0;    // interior pressure

   // Functions and static declarations to establish pressure as a property.

   public static PropertyList myProps =
      new PropertyList (PressureShell.class, RootModel.class);

   static {
      myProps.add ("pressure", "interior pressure", 0);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getPressure() {
      return myPressure;
   }

   public void setPressure (double p) {
      myPressure = p;
   }

   // build the actual model
   public void build (String[] args) {

      // create a mech model and add it to the root model
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create an icosahedral spherical mesh.
      PolygonalMesh mesh =
         MeshFactory.createIcosahedralSphere (radius, /*num divisions=*/2);

      // create a FEM shell by extruding the mesh faces
      FemModel3d fem = FemFactory.createExtrusion (
         null, /*num layers=*/1, thickness, 0, mesh);
      fem.setDensity (density);
      fem.setSurfaceRendering (FemModel3d.SurfaceRender.Shaded);
      fem.setMaterial (new LinearMaterial (50000, 0.49));
      fem.setName ("fem");
      mech.addModel (fem);

      // create a table for the shell to collide with
      RigidBody table = RigidBody.createBox (
         "table", length, width, depth, density);
      table.setPose (new RigidTransform3d (0, 0, -1.5*radius));
      table.setDynamic (false);
      mech.addRigidBody (table);

      // enable collision between the shell and the table
      mech.setCollisionBehavior (table, fem, true, /*friction=*/0.1);

      findInteriorFaces (fem);

      // set gravity. (This call is actually redundant since it sets the
      // default gravity value.)
      mech.setGravity (0, 0, -9.8);

      // Set render properties so that FEM nodes are rendered as
      // small green spheres and the shell surface is cyan
      RenderProps.setPointStyle (fem, Renderer.PointStyle.SPHERE);
      RenderProps.setPointColor (fem, Color.GREEN);
      RenderProps.setPointRadius (fem, 0.005);
      RenderProps.setFaceColor (fem, Color.CYAN);

      // add the controller to set node forces to respond to pressure
      addController (new PressureController (fem), mech);

      // add interactive control panel
      addControlPanel();
   }

   /**
    * Test all the faces in the shell's surface mesh, and records those which
    * are on the interior.
    */
   private void findInteriorFaces (FemModel3d fem) {
      PolygonalMesh mesh = fem.getSurfaceMesh();
      Point3d cent = new Point3d();
      for (Face face : mesh.getFaces()) {
         face.computeCentroid (cent);
         if (face.getNormal().dot (cent) < 0) {
            myInteriorFaces.add (face);
         }
      }
   }

   /**
    * Create a control panel for interactively controlling the pressure.
    */
   private void addControlPanel() {
      ControlPanel cp = new ControlPanel ("options", "");
      DoubleFieldSlider w = (DoubleFieldSlider)cp.addWidget (this, "pressure");
      w.setSliderRange (0, 3000);
      addControlPanel (cp);
   }

}
