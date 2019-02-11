package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemFactory.FemElementType;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.Collidable.Collidability;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GLViewer.BlendFactor;
import maspack.render.GL.GL2.GL2Viewer;

public class FemCollisions extends RootModel {
   
   public FemCollisions() {}
   
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);

      // Reduce step size to better resolve collisions
      setMaxStepSize(0.0002);
      
      // Create and add main MechModel
      MechModel mech = new MechModel("mech");
      addModel(mech);
      
      //-------------------------------------------------------------
      // BEAM
      //-------------------------------------------------------------
      
      // Create FEM beam
      FemModel3d beam = new FemModel3d("beam");
      mech.addModel(beam);
      double[] size = {0.003, 0.0015, 0.0015};         // widths
      int[] res = {4, 2, 2};                           // resolution
      FemFactory.createGrid(beam, FemElementType.Hex, 
         size[0], size[1], size[2], 
         res[0], res[1], res[2]);
      
      // Set properties
      beam.setDensity(1000);
      beam.setMaterial(new LinearMaterial(300, 0.33));
      
      //-------------------------------------------------------------
      // ELLIPSOID
      //-------------------------------------------------------------
      
      // Create FEM ellipsoid
      FemModel3d ellipsoid = new FemModel3d("ellipsoid");
      mech.addModel(ellipsoid);
      double[] radii = {0.002, 0.001, 0.001}; // radii (z, x, y)
      int[] eres = {16, 4, 3};                // resolution (theta, phi, r)
      FemFactory.createEllipsoid(ellipsoid, 
         radii[0], radii[1], radii[2], 
         eres[0], eres[1], eres[2]);
      
      // Set properties
      ellipsoid.setDensity(1000);
      ellipsoid.setMaterial(new LinearMaterial(300, 0.33));

      // Transform: rotate 90 degrees about X-Axis
      //            translate up by 0.003
      RigidTransform3d trans = new RigidTransform3d();
      trans.setRotation(new AxisAngle(1, 0, 0, Math.PI/2));
      trans.setTranslation(new Vector3d(0, 0.0005, 0.003));
      ellipsoid.transformGeometry(trans);
      
      //-------------------------------------------------------------
      // BLOCK WITH EMBEDDED SPHERE
      //-------------------------------------------------------------
      
      // Create FEM block
      FemModel3d block = new FemModel3d("block");
      mech.addModel(block);
      FemFactory.createHexGrid(block, 0.002, 0.002, 0.002, 3, 3, 3);

      // Set properties
      block.setDensity(1000);
      block.setMaterial(new LinearMaterial(300, 0.33));

      // Create embedded sphere
      double r = 0.0008;
      int ref = 2;      // level of refinement
      PolygonalMesh sphere = MeshFactory.createOctahedralSphere(r, ref);
      FemMeshComp embeddedSphere = block.addMesh("embedded", sphere);
      // need to explicity set this mesh to be collidable
      embeddedSphere.setCollidable (Collidability.EXTERNAL);
      
      // Transform: rotate 90 degrees about X-Axis
      //            translate left by 0.003
      trans = new RigidTransform3d();
      trans.setTranslation(new Vector3d(0, 0.003, 0));
      block.transformGeometry(trans);
      
      
      //-------------------------------------------------------------
      // TABLE AND COLLISIONS
      //-------------------------------------------------------------
      
      RigidBody table = RigidBody.createBox("table", 0.005, 0.0075, 0.0008, 0);
      table.setDynamic(false);
      table.setPose(
         new RigidTransform3d(
            new Vector3d(0,0.0015,-0.002), AxisAngle.IDENTITY));
      mech.addRigidBody(table);
      
      // Set up collisions
      mech.setCollisionBehavior(ellipsoid, beam, true, 0.1);
      mech.setCollisionBehavior(ellipsoid, table, true, 0.1);
      mech.setCollisionBehavior(embeddedSphere, table, true, 0.1);
      mech.setCollisionBehavior(ellipsoid, embeddedSphere, true, 0.1);
      mech.setCollisionBehavior(table, beam, true, 0.1);
      
      
      //-------------------------------------------------------------
      // RENDER PROPERTIES
      //-------------------------------------------------------------
      
      // Draw beam element widgets
      beam.setElementWidgetSize(0.8);
      RenderProps.setLineWidth(beam.getElements(), 0);
      
      // Make beam blue, and give it a transparent surface
      RenderProps.setFaceColor(beam, Color.BLUE);
      beam.setSurfaceRendering(SurfaceRender.Shaded);
      RenderProps.setAlpha(beam.getMeshComp("surface"), 0.4);
      
      // Make the ellipsoid red
      RenderProps.setFaceColor(ellipsoid, Color.RED);
      RenderProps.setLineColor(ellipsoid, Color.RED.darker());
      ellipsoid.setSurfaceRendering(SurfaceRender.Shaded);
      
      // Make the block green
      RenderProps.setFaceColor(block, Color.GREEN);
      RenderProps.setLineColor(block, Color.GREEN.darker());
   }
   
   @Override
   public void attach(DriverInterface driver) {
      super.attach(driver);
      
      // Enable transparency blending
      GLViewer viewer = getMainViewer();
      if (viewer != null) {
         // viewer will be null if we're in batch mode
         viewer.setBlendDestFactor (BlendFactor.GL_ONE_MINUS_SRC_ALPHA);
      }
   }
}
