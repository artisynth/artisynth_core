package artisynth.demos.mech;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFrame;

import artisynth.core.driver.Main;
import artisynth.core.gui.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.TimeBase;
import artisynth.core.workspace.RootModel;
import artisynth.core.materials.*;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.*;
import maspack.properties.Property;
import maspack.render.*;
import maspack.spatialmotion.SpatialInertia;

public class WrappedMuscleArm extends RootModel {
   
   public static final String meshDir = ArtisynthPath.getHomeRelativePath(
      "src/artisynth/demos/mech/geometry/",".");
   protected MechModel model;
   double len = 20;
   Vector3d size = new Vector3d(len/10,len/5,len);
   boolean addCompression = true;
    
   public void build (String[] args) throws IOException {

      model = new MechModel("Arm");
      addModel(model);
             
      model.setIntegrator(MechSystemSolver.Integrator.RungeKutta4);
      model.setMaxStepSize(0.01);

      setupRenderProps();
        
      addRigidBodies();
      addJoint();
        
      addMuscle();
        
      addEndPoint();
      addControlPanel();
      addProbes();
   }
    
   public void addRigidBodies() {

      RigidTransform3d X;
        
      X= new RigidTransform3d();
      X.p.z = len/2;
      addBody("upper",X, "smoothBeam.obj");
        
      X = new RigidTransform3d();
      double angle = Math.toRadians (32);
      X.R.setAxisAngle(0,1,0,angle);
      X.p.set(-len/2*Math.sin(angle), 0.0,-len/2*Math.cos(angle));

      addBody("lower",X, "smoothBeam.obj");        

      RigidTorus ring = new RigidTorus ("ring", 5.0, 2.0, 1000.0);
      ring.setDynamic (false);
      ring.setPose (new RigidTransform3d (12, 0, 8, 0, -Math.PI/12, 0));
        
      model.addRigidBody (ring);
   }

   public void addBody(String name, RigidTransform3d pose, String meshName) {
      // add a simple rigid body to the simulation
        
      RigidBody rb = new RigidBody();
      rb.setName(name);
      rb.setPose(pose);

      model.addRigidBody(rb);
        
      PolygonalMesh mesh;
      try {
         String meshFilename = meshDir + meshName;
         mesh = new PolygonalMesh();
         mesh.read(
            new BufferedReader(
               new FileReader(
                  new File(meshFilename))));
         rb.setMesh(mesh, meshFilename);
      }
      catch(IOException e) {
         System.out.println(e.getMessage());
         mesh = MeshFactory.createBox(size.x, size.y, size.z);
         rb.setMesh(mesh, null);
      }

      rb.setInertia(SpatialInertia.createBoxInertia(
                       10.0, size.x, size.y, size.z));
        
      RenderProps rp = new RenderProps(model.getRenderProps());
      rp.setFaceColor(Color.GRAY);
      rp.setShading(Renderer.Shading.FLAT);
      rb.setRenderProps(rp);

      rb.setFrameDamping (10);
      rb.setRotaryDamping (10000.0);
   }
    
   public void addJoint() {

      RigidBody upperArm = model.rigidBodies().get("upper");
      RigidBody lowerArm = model.rigidBodies().get("lower");
      if (upperArm==null || lowerArm==null) {
         return;
      }
        
      HingeJoint j = new HingeJoint();
      j.setName("elbow");
        
      RigidTransform3d TCA = new RigidTransform3d();
      TCA.p.z = len/2;
      TCA.R.setAxisAngle(1,0,0,Math.PI/2);
      RigidTransform3d TCW = new RigidTransform3d();
      TCW.R.setAxisAngle(1,0,0,Math.PI/2);

      j.setBodies (lowerArm, TCA, null, TCW);
      j.setShaftLength(len/3);
      RenderProps.setFaceColor (j, Color.BLUE);
      model.addBodyConnector(j);
        
      upperArm.setDynamic(false);
   }
    
   public void addMuscle () {

      RigidBody upperArm = model.rigidBodies().get("upper");
      RigidBody lowerArm = model.rigidBodies().get("lower");
      RigidTorus ring = (RigidTorus)model.rigidBodies().get("ring");
      if (upperArm==null || lowerArm==null) {
         return;
      }
        
      Point3d markerBodyPos = new Point3d(size.x/2,0,(size.z/2.0)/1.2);
      FrameMarker u = new FrameMarker();
      model.addFrameMarker(u, upperArm, markerBodyPos);
      u.setName("upperAttachment");
      RenderProps.setSphericalPoints (u, 0.5, Color.WHITE);
        
      markerBodyPos = new Point3d(size.x/2,0,-(size.z/2.0)/2);
      FrameMarker l = new FrameMarker();
      model.addFrameMarker(l,lowerArm, markerBodyPos);
      l.setName("lowerAttachment");
      RenderProps.setSphericalPoints (l, 0.5, Color.WHITE);
        
      MultiPointMuscle muscle = new MultiPointMuscle("muscle");
      muscle.setMaterial (
         new LinearAxialMuscle (2, 22));

      muscle.addPoint(u);
      muscle.setSegmentWrappable (
         100, new Point3d[] { ring.getPosition() });            
      muscle.addWrappable (ring);
      muscle.addPoint(l);
      muscle.updateWrapSegments();

      RenderProps rp = new RenderProps(model.getRenderProps());
      rp.setLineStyle(Renderer.LineStyle.CYLINDER);
      rp.setLineRadius(len/100);
      //rp.setLineSlices(10);
      rp.setShading(Renderer.Shading.SMOOTH);
      rp.setLineColor(Color.RED);
      muscle.setRenderProps(rp);
        
      model.addMultiPointSpring(muscle);
        
      if (addCompression) {
         markerBodyPos = new Point3d(size.x/2,0,+size.z/2.0);
         FrameMarker l2 = new FrameMarker();
         model.addFrameMarker(l2, lowerArm, markerBodyPos);
         l2.setName("lowerAttachmentCompressor");
           
         double len = u.getPosition().distance(l2.getPosition());
         AxialSpring s = new AxialSpring(10, 0, 50);
         s.setFirstPoint(u);
         s.setSecondPoint(l2);
         model.addAxialSpring(s);
         RenderProps props = new RenderProps();
         props.setLineStyle(Renderer.LineStyle.CYLINDER);
         props.setLineRadius(0.0);
         s.setRenderProps(props);
      }
   }
    
   public void addEndPoint() {

      RigidBody lowerArm = model.rigidBodies().get("lower");
      if (lowerArm==null) {
         return;
      }
        
      FrameMarker endPoint = new FrameMarker();
      endPoint.setName("endPoint");
      endPoint.setFrame(lowerArm);
      endPoint.setLocation(new Point3d(0,0,-len/2));
      model.addFrameMarker(endPoint);
      //lowerArm.addMarker(endPoint);
        
      RenderProps rp = new RenderProps(model.getRenderProps());
      rp.setShading(Renderer.Shading.SMOOTH);
      rp.setPointColor(Color.ORANGE);
      rp.setPointRadius(len/20);
      endPoint.setRenderProps(rp);
   }
    
   public void setupRenderProps() {

      // set render properties for model
       
      RenderProps rp = new RenderProps();
      rp.setPointStyle(Renderer.PointStyle.SPHERE);
      rp.setPointColor(Color.LIGHT_GRAY);
      rp.setPointRadius(0.0);
      rp.setLineStyle(Renderer.LineStyle.CYLINDER);
      rp.setLineColor(Color.WHITE);
      rp.setLineRadius(0.2);
      model.setRenderProps(rp);
   }
    
   protected ControlPanel panel;

   public void addControlPanel () {
      panel = new ControlPanel("Muscle Control", "");
      panel.addWidget (
         "Activation", model, "axialSprings/muscle:excitation", 0.0, 1.0);
      addControlPanel (panel);
   }
    
   public void addProbes() {

      NumericInputProbe ip;
      NumericOutputProbe op;
      double rate = 0.01;
      try {
         String path = ArtisynthPath.getHomeRelativePath(
            "src/artisynth/demos/mech/", ".");
         ip = new NumericInputProbe(model,
                                    "multiPointSprings/muscle:excitation",
                                    path+"muscleArmActivation.txt");
         ip.setStartStopTimes (0, 10.0);
         ip.setName("Muscle Activation");
         //ip.setActive (false);
         addInputProbe(ip);
           
      }
      catch (Exception e) {
         System.out.println(e.getMessage());
      }
   }
}
