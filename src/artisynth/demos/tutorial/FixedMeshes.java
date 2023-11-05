package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.AxisDrawStyle;
import maspack.render.Renderer.FaceStyle;

/**
 * Demonstrates creation of fixed mesh bodies.
 */
public class FixedMeshes extends RootModel {

   public void build (String[] args) {
      // create a MechModel to add the mesh bodies to
      MechModel mech = new MechModel ("mech");
      addModel (mech);
      
      // read torus mesh from a file stored in the folder "data" located under
      // the source folder of this model:
      String filepath = getSourceRelativePath ("data/torus_9_24.obj");
      PolygonalMesh mesh = null;
      try {
         mesh = new PolygonalMesh (filepath);
      }
      catch (Exception e) {
         System.out.println ("Error reading file " + filepath + ": " + e);
         return;
      }
      // create a FixedMeshBody containing the mesh and add it to the MechModel
      FixedMeshBody torus = new FixedMeshBody ("torus", mesh);
      mech.addMeshBody (torus);

      // create a square mesh with a factory method and add it to the MechModel
      mesh = MeshFactory.createRectangle (
         /*width=*/3.0, /*width=*/3.0, /*ndivsx=*/10, /*ndivsy=*/10,
         /*addTextureCoords=*/false);
      FixedMeshBody square = new FixedMeshBody ("square", mesh);
      mech.addMeshBody (square);
      // reposition the square: translate it along y and rotate it about x
      square.setPose (
         new RigidTransform3d (/*xyz=*/0,0.5,0,/*rpy=*/0,0,Math.toRadians(90)));

      // set rendering properties:
      // make torus pale green
      RenderProps.setFaceColor (torus, new Color (0.8f, 1f, 0.8f));
      // show square coordinate frame using solid arrows
      square.setAxisLength (0.75);
      square.setAxisDrawStyle (AxisDrawStyle.ARROW);
      // make square blue gray with mesh edges visible
      RenderProps.setFaceColor (square, new Color (0.8f, 0.8f, 1f));
      RenderProps.setDrawEdges (square, true);
      RenderProps.setEdgeColor (square, new Color (0.5f, 0.5f, 1f));
      RenderProps.setFaceStyle (square, FaceStyle.FRONT_AND_BACK);
   }
}
