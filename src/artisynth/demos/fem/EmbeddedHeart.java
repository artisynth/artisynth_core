package artisynth.demos.fem;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.*;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemMuscleModel;
import artisynth.core.femmodels.MuscleBundle;
import artisynth.core.femmodels.*;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.MuscleMaterial;
import artisynth.core.materials.SimpleForceMuscle;
import artisynth.core.mechmodels.Collidable.Collidability;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.renderables.LightComponent;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.gui.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import artisynth.demos.tutorial.FemMuscleHeart;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.io.WavefrontReader;
import maspack.matrix.AxisAngle;
import maspack.matrix.*;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.Property;
import maspack.render.Light;
import maspack.render.RenderProps;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.Shading;
import maspack.render.GL.GLViewer;
import maspack.util.RandomGenerator;

/**
 * Simple demo showing a heart contracting.  Demonstrates the use of generating
 * an encapsulating FEM, and colliding with an embedded surface
 * 
 * @author Antonio, with a changes by John Lloyd
 */
public class EmbeddedHeart extends RootModel {

   public static boolean omitFromMenu = false;

   // Constructor
   public EmbeddedHeart() {}
   
   // Model builder
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      RandomGenerator.setSeed (0x1234);
      setMaxStepSize(0.005);
      
      // Root mechanical model
      MechModel mech = new MechModel("mech");
      mech.setGravity(0, 0, -9.8);
      addModel(mech);
      

      //-------------------------------------------------------------
      // HEART LOAD / ADD GEOMETRY
      //-------------------------------------------------------------
      
      // Heart surface mesh, with texture
      String heartFile = ArtisynthPath.getSrcRelativePath(FemMuscleHeart.class, "data/HumanHeart.obj");
      WavefrontReader wfr = new WavefrontReader(new File(heartFile));
      wfr.parse ();
      PolygonalMesh heartMesh = new PolygonalMesh();
      wfr.setMesh (heartMesh, "(null)");  // read null group
      heartMesh.triangulate();            // triangulate for interaction
      
      // FEM heart:
      FemMuscleModel heart = new FemMuscleModel("heart");
      EmbeddedFem.createVoxelizedFem(heart, heartMesh, RigidTransform3d.IDENTITY, 8, -1, 0);
      FemMeshComp embeddedHeart = heart.addMesh(heartMesh); // add real-looking mesh
      embeddedHeart.setName("embedded");
      
      // convex hull
      PolygonalMesh heartHull = MeshFactory.createConvexHull (heartMesh);
      FemMeshComp collisionHeart = heart.addMesh (heartHull);
      collisionHeart.setName ("collideSurface");
      collisionHeart.setCollidable (Collidability.ALL);  // allow collision with external surfaces
      
      // Allow inverted elements (poor quality mesh)
      heart.setWarnOnInvertedElements(false);
      heart.setAbortOnInvertedElements(false);  
      
      // Convert unites to metres (original was cm)
      heart.scaleDistance(0.01);
      heart.setStiffnessDamping(0.02);
      
      // Set material properties
      heart.setDensity(1000);
      FemMaterial femMat = new LinearMaterial(10000, 0.33, true);
      MuscleMaterial muscleMat = new SimpleForceMuscle(2000.0);    // simple muscle
      heart.setMaterial(femMat);
      
      // Add heart to model
      mech.addModel(heart);
      
      //-------------------------------------------------------------
      // MUSCLE BUNDLES
      //-------------------------------------------------------------
      // One "long" direction muscle bundle
      // One "radial" muscle bundle
      
      // LONG BUNDLE
      
      // Compute the "long" direction of the heart
      PolygonalMesh hull = heart.getSurfaceMesh();
      RigidTransform3d trans = hull.computePrincipalAxes();
      Vector3d longAxis = new Vector3d();
      trans.R.getColumn(0, longAxis);        // first column of rotation
      
      // Create the long axis muscle bundle
      MuscleBundle longBundle = new MuscleBundle("long");
      for (FemElement3d elem : heart.getElements()) {
         longBundle.addElement(elem, longAxis);
      }
      longBundle.setMuscleMaterial(muscleMat);
      heart.addMuscleBundle(longBundle);
      
      // RADIAL BUNDLE
      
      // Compute a plane through centre of heart
      Plane plane = new Plane(longAxis, new Point3d(trans.p));
      Point3d centroid = new Point3d();
      Vector3d radialDir = new Vector3d();
      
      // Create the radial muscle bundle
      MuscleBundle radialBundle = new MuscleBundle("radial");
      for (FemElement3d elem : heart.getElements()) {
         elem.computeCentroid(centroid);
         
         // project to plane and compute radial direction
         plane.project(centroid, centroid);
         radialDir.sub(centroid, trans.p);
         radialDir.normalize();
         
         radialBundle.addElement(elem, radialDir);
      }
      radialBundle.setMuscleMaterial(muscleMat);
      heart.addMuscleBundle(radialBundle);
           
      //-------------------------------------------------------------
      // HEART RENDER PROPERTIES
      //-------------------------------------------------------------

      //RenderProps.setLineColor(heart.getElements(), new Color(0.6f, 0.4f, 0));
      RenderProps.setLineColor(heart.getElements(), new Color(0.6f, 0f, 0.6f));
      RenderProps.setSphericalPoints (heart.getNodes(), 0.0015, Color.WHITE);
      RenderProps.setLineColor(radialBundle, Color.BLUE);
      RenderProps.setLineColor(longBundle, Color.RED);
      RenderProps.setVisible (heart.getNodes(), false);
      RenderProps.setFaceColor (heart, new Color(174f/255, 192f/255, 192f/255));
      radialBundle.setDirectionRenderLen(0.6);

      RenderProps.setLineWidth (heart.getMuscleBundles(), 2);
      longBundle.setDirectionRenderLen(0.6);
      RenderProps.setVisible(heart.getMuscleBundles(), false);
      heart.setSurfaceRendering (SurfaceRender.None);
      embeddedHeart.setSurfaceRendering (SurfaceRender.Shaded);
      collisionHeart.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setVisible (collisionHeart, false);
      
      // adjust heart mesh render properties
      RenderProps rprops = embeddedHeart.getRenderProps();
      rprops.getBumpMap().setScaling(0.01f);
      rprops.getColorMap().setSpecularColoring(false);  // don't modify specular
      rprops.setShading (Shading.SMOOTH);
      rprops.setFaceColor (new Color(0.8f,0.8f,0.8f));
      rprops.getColorMap ().setColorMixing (ColorMixing.MODULATE);
      rprops.setSpecular (new Color(0.4f, 0.4f, 0.4f));
      rprops.setShininess (128);

      //-------------------------------------------------------------
      // INPUT PROBES
      //-------------------------------------------------------------
      
      // Add heart probe
      addHeartProbe(longBundle, radialBundle);
      
      //-------------------------------------------------------------
      // RIGID TABLE AND COLLISION
      //-------------------------------------------------------------
      
      // Create a rigid plate for the heart to fall on
      RigidBody plate = RigidBody.createBox (
         "plate", 0.25, 0.25, 0.02, 0, /*addnormals*/ true);
      plate.setPose (new RigidTransform3d (0,0,-0.2));
      plate.setDynamic(false);
      RenderProps.setFaceColor (plate, new Color (0.9f, 0.68f, 0.45f));
      mech.addRigidBody(plate);
      
      // adjust table render properties
      RenderProps.setShading(plate, Shading.METAL);
      RenderProps.setSpecular(plate, new Color(0.8f,0.8f,0.8f));
      
      // Enable collisions between the heart and table 
      CollisionBehavior cb = mech.setCollisionBehavior(collisionHeart, plate, true);
      cb.setCompliance (1e-5);

      //-------------------------------------------
      // Marker to use with the pull controller
      //-------------------------------------------
      double dist = 0.03;
      Point3d pnt = new Point3d (-0.0015, -0.0552, 0.0040);
      ArrayList<FemNode3d> nodes = heart.findNearestNodes (pnt, dist);
      FemMarker mkr = new FemMarker (pnt);
      RenderProps.setSphericalPoints (mkr, 0.003, new Color (1f, 0.5f, 0.5f));
      mkr.setFromNodes (nodes); 
      // set node colors to reflect weight
      VectorNd wgts = mkr.getCoordinates();
      double max = wgts.infinityNorm();
      double min = wgts.minElement();
      for (int i=0; i<wgts.size(); i++) {
         float c = 1f - (float)((wgts.get(i)-min)/(max-min));
         RenderProps.setPointColor (nodes.get(i), new Color (1f, c, c));
      }
      heart.addMarker (mkr);

      //------------------------------
      // Control panel for excitations
      //------------------------------
      ControlPanel panel = new ControlPanel();
      panel.addWidget ("long excitation", longBundle, "excitation");
      panel.addWidget ("radial excitation", radialBundle, "excitation");
      panel.addWidget ("plate visible", plate.getRenderProps(), "visible");
      addControlPanel (panel);
   }
   
   protected void addHeartProbe(MuscleBundle longBundle, 
      MuscleBundle radialBundle) {
      
      Property[] props = new Property[2];
      props[0] = longBundle.getProperty("excitation");
      props[1] = radialBundle.getProperty("excitation");
      
      NumericInputProbe probe = new NumericInputProbe();
      probe.setInputProperties(props);
      
      double startTime = 1.0;
      double stopTime = 60.0;
      double cycleTime = 0.8;   // seconds per heart beat
      probe.setStartStopTimes(startTime, stopTime);
      
      // beat cycle 
      double [] beat0 = {0, 0};
      double [] beat1 = {0.9, 0};
      double [] beat2 = {0, 0.3};
      for (double t=0; t<stopTime-startTime; t+= cycleTime) {
         // NOTE: times are relative to "startTime"
         probe.addData(t, beat0);
         probe.addData(t+cycleTime*0.15, beat1);
         probe.addData(t+cycleTime*0.3, beat2);
         probe.addData(t+cycleTime*0.4, beat0);
      }
      
      addInputProbe(probe);
      
   }
   
   @Override
   public void attach(DriverInterface driver) {
      super.attach(driver);
      
      // Lloyd, Jul 2025: removed lights because they don't save/load
      // ComponentList<LightComponent> lights = new ComponentList<>(LightComponent.class, "lights");
      // add(lights);
      
      GLViewer viewer = getMainViewer();
      if (viewer != null) {
         // for (int i=0; i<viewer.numLights(); ++i) {
         //    Light light = viewer.getLight(i);
         //    LightComponent lc = new LightComponent(light);
         //    lights.add(lc);
         // }
         viewer.setBackgroundColor(Color.WHITE);
      }
      
   }
   
}
