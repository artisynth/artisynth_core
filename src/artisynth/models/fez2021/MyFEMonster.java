package artisynth.models.fez2021;

import java.awt.Color;

import artisynth.core.femmodels.AnsysReader;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.util.PathFinder;

public class MyFEMonster extends RootModel {

   String geodir = PathFinder.getSourceRelativePath (this, "geometry/");

   public void attachFemToBody (
      MechModel mech, FemModel3d fem, RigidBody body, double tol) {

      PolygonalMesh mesh = body.getSurfaceMesh();
      for (FemNode3d node : fem.getNodes()) {
         if (mesh.distanceToPoint (node.getPosition()) < tol ||
             mesh.pointIsInside (node.getPosition()) == 1) {
            mech.attachPoint (node, body);
            RenderProps.setPointColor (node, Color.RED);
         }
      }
   }

   public FemModel3d createHexGridFem () {
      // Create an regular hex grid FEM using FemFactory
      FemModel3d fem = FemFactory.createHexGrid (null, 0.2, 0.2, 0.6, 4, 4, 12);
      fem.transformGeometry (new RigidTransform3d (0, 0, -0.45));
      return fem;
   }

   public FemModel3d createFemFromSurface () {
      // Create an FEM from a surface mesh using TetGen
      PolygonalMesh mesh = null;
      try {
         mesh = new PolygonalMesh (geodir+"humerusLeft1000.ply");
      }
      catch (Exception e) {
         System.out.println ("Can't load humerusLeft1000.ply");
      }
      mesh.scale (0.01, 0.01, 0.0075);
      mesh.transform (new RigidTransform3d (0, 0, -0.17));
      FixedMeshBody meshBody = new FixedMeshBody(mesh);
      FemModel3d fem = FemFactory.createFromMesh (null, mesh, 2.0);
      RenderProps.setLineWidth (fem, 0);
      fem.setMaterial (new LinearMaterial (5000000.0, 0.49));
      return fem;
   }

   public FemModel3d importFemFromAnsys () {
      // Create an FEM by importing Ansys files
      FemModel3d fem = null;
      try {
         fem = AnsysReader.read (
            null, 
            geodir+"humerus.node",
            geodir+"humerus.elem",
            1000.0,
            null,
            0);
      }
      catch (Exception e) {
         System.out.println ("Can't load Ansys files");
      }
      RenderProps.setLineWidth (fem, 0);
      fem.setMaterial (new LinearMaterial (5000000.0, 0.49));
      RenderProps.setVisible (fem.getNodes(), false);
      return fem;
   }

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      mech.setFrameDamping (10.0);
      mech.setRotaryDamping (1.0);

      RigidBody plate = RigidBody.createBox (
         "plate", 1.0, 1.0, 0.1, /*density=*/1000.0);
      plate.setPose (new RigidTransform3d (0, 0, 0.2));
      mech.addRigidBody (plate);
      plate.setDynamic (false);

      PolygonalMesh mesh = MeshFactory.createRoundedBox (
         0.15, 0.2, 0.2, 1, 1, 1, /*nslices=*/20, /*flatBottom=*/true);
      mesh.transform (new RigidTransform3d (0, 0, -.15/2));
      RigidBody top = RigidBody.createFromMesh (
         "top", mesh, /*density=*/1000.0, /*scale=*/1.0);
      mech.addRigidBody (top);

      RigidBody tip = RigidBody.createBox (
         "tip", 0.2, 0.2, 0.15,/*density=*/1000.0);
      mech.addRigidBody (tip);
      tip.setPose (new RigidTransform3d (0, 0, -0.825));
      
      try {
         mesh = new PolygonalMesh (geodir+"flange.obj");
      }
      catch (Exception e) {
      }
      RigidBody flange = RigidBody.createFromMesh ("flange", mesh, 1000.0, 1.0);
      flange.setDynamic (false);
      //mech.addMeshBody (mbody);
      mech.addRigidBody (flange);

      HingeJoint hinge = new HingeJoint (
         top, flange, new Point3d(), new Vector3d (0, 1, 0));

      mech.addBodyConnector (hinge);

      FrameMarker l0 = mech.addFrameMarkerWorld (
         tip, new Point3d(-0.1, 0, -0.8));
      FrameMarker l1 = mech.addFrameMarkerWorld (
         plate, new Point3d(-0.48, 0, 0.15));
      FrameMarker r0 = mech.addFrameMarkerWorld (
         tip, new Point3d(0.1, 0, -0.8));
      FrameMarker r1 = mech.addFrameMarkerWorld (
         plate, new Point3d(0.48, 0, 0.15));

      Muscle muscleL = new Muscle ();
      muscleL.setMaterial (new SimpleAxialMuscle (100, 0, 50));
      mech.attachAxialSpring (l0, l1, muscleL);

      Muscle muscleR = new Muscle ();
      muscleR.setMaterial (new SimpleAxialMuscle (90, 0, 50));
      mech.attachAxialSpring (r0, r1, muscleR);

      hinge.setShaftLength (0.4);
      RenderProps.setFaceColor (hinge, Color.BLUE);
      RenderProps.setSphericalPoints (mech, 0.02, Color.WHITE);
      RenderProps.setSpindleLines (mech, 0.02, Color.RED);

      ControlPanel panel = new ControlPanel ();
      panel.addWidget ("left excitation", muscleL, "excitation");
      panel.addWidget ("right excitation", muscleR, "excitation");      
      addControlPanel (panel);

      NumericInputProbe eprobe =
         new NumericInputProbe (muscleR, "excitation", 0, 10.0);

      eprobe.addData (
         new double[] { 0, 0,
                        1, 1,
                        3, 0,
                        5, 1,
                        6, 0,
                        7, 1,
                        8, 0,
                        9, 1,
                        10, 0},
             NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (eprobe);

      // Code to create and attach an FEM between top and tip

      // FemModel3d fem = createHexGridFem();
      // //FemModel3d fem = createFemFromSurface();
      // //FemModel3d fem = importFemFromAnsys();
      // fem.setSurfaceRendering (SurfaceRender.Shaded);
      // mech.addModel (fem);
      // attachFemToBody (mech, fem, top, 0.001);
      // attachFemToBody (mech, fem, tip, 0.001);
      // // Set rendering properties for the FEM
      // RenderProps.setLineColor (fem, Color.MAGENTA);
      // RenderProps.setFaceColor (fem, new Color (0.8f, 0.8f, 1f));
      // RenderProps.setSphericalPoints (fem, 0.005, new Color (0, 0.6f, 0));
   }
}
