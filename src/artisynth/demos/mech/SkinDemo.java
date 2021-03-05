package artisynth.demos.mech;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.PolylineMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.spatialmotion.SpatialInertia;
import artisynth.core.driver.Main;
import artisynth.core.femmodels.SkinMeshBody;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.RootModel;

public class SkinDemo extends RootModel {

   protected MechModel model;
   double len = 20;
   Vector3d size = new Vector3d(len/10,len/5,len);
   boolean addCompression = true;
   protected SkinMeshBody mySkinMesh = null;
   SkinMeshBody myFiberMesh = null;
   RigidBody myLower = null;
   RigidBody myUpper = null;

   double myAlpha = 1;

   public static PropertyList myProps =
      new PropertyList (SkinDemo.class, RootModel.class);

   static {
      myProps.add ("alpha", "alpha for meshes", 1, "[0,1]");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getAlpha() {
      return myAlpha;
   }

   public void setAlpha (double alpha) {
      myAlpha = alpha;
      RenderProps.setAlpha (myFiberMesh, alpha);
      RenderProps.setAlpha (myLower, alpha);
   }
    
   public void build (String[] args) {

      model = new MechModel("Arm");
      addModel(model);
             
      model.setIntegrator(MechSystemSolver.Integrator.RungeKutta4);
      model.setMaxStepSize(0.01);

      setupRenderProps();
        
      addRigidBodies();
      addJoint();
        
      addMuscle();
        
      //        addAntagonist();
      addEndPoint();
      addSkinMesh();
      addFiberMesh();
      addProbes();
      addPanel();
   }
    
   public void addRigidBodies() {
      RigidTransform3d X;
        
      X= new RigidTransform3d();
      X.p.z = len/2;
      myUpper = addBody("upper",X, "barm.obj");
        
      X = new RigidTransform3d();
      double angle = Math.toRadians(225);
      X.R.setAxisAngle(0,1,0,angle);
      X.p.set(len/2*Math.sin(angle), 0.0,len/2*Math.cos(angle));
        
      myLower = addBody("lower",X, "barm.obj");        
   }
    
   public RigidBody addBody(String name, RigidTransform3d pose, 
                       String meshName) {
      // add a simple rigid body to the simulation
        
      RigidBody rb = new RigidBody();
      rb.setName(name);
      rb.setPose(pose);

      model.addRigidBody(rb);
        
      PolygonalMesh mesh;
      try {
         String meshFilename = ArtisynthPath.getHomeRelativePath(
            "src/artisynth/demos/mech/geometry/",".") + meshName;
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
      rb.setRotaryDamping (1000.0);
      return rb;
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
      TCA.p.z = -len/2;
      TCA.R.setAxisAngle(1,0,0,Math.PI/2);
      RigidTransform3d TCW = new RigidTransform3d();
      TCW.R.setAxisAngle(1,0,0,Math.PI/2);

      j.setBodies (lowerArm, TCA, null, TCW);
      j.setShaftLength(len/3);
      RenderProps.setFaceColor (j, Color.BLUE);
      model.addBodyConnector(j);
        
      upperArm.setDynamic(false);
   }
    
   public void addMuscle() {
    
      RigidBody upperArm = model.rigidBodies().get("upper");
      RigidBody lowerArm = model.rigidBodies().get("lower");
      if (upperArm==null || lowerArm==null) {        
         return;
      }
        
      Point3d markerBodyPos = new Point3d(-size.x/2,0,(size.z/2.0)/1.2);
      FrameMarker u = new FrameMarker();
      model.addFrameMarker(u, upperArm, markerBodyPos);
      u.setName("upperAttachment");
        
      markerBodyPos = new Point3d(size.x/2,0,-(size.z/2.0)/2);
      FrameMarker l = new FrameMarker();
      model.addFrameMarker(l,lowerArm, markerBodyPos);
      l.setName("lowerAttachment");
        
      Muscle muscle = new Muscle("muscle");
      muscle.setPeckMuscleMaterial(20.0, 22.0, 30, 0.2, 0.5, 0.1);
      muscle.setFirstPoint(u);
      muscle.setSecondPoint(l);
        
      RenderProps rp = new RenderProps(model.getRenderProps());
      rp.setLineStyle(Renderer.LineStyle.SPINDLE);
      rp.setLineRadius(len/20);
      //rp.setLineSlices(10);
      rp.setShading(Renderer.Shading.SMOOTH);
      rp.setLineColor(Color.RED);
      muscle.setRenderProps(rp);
        
      model.addAxialSpring(muscle);
        
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
    
   public void addAntagonist() {    
      RigidBody lowerArm = model.rigidBodies().get("lower");
      if (lowerArm==null) {        
         return;
      }
        
      Point3d markerBodyPos = new Point3d(-size.x/2,0,0);
      //      Point3d markerBodyPos = new Point3d(-size.x,0,-(size.z/2.0)/1.2);
      FrameMarker marker = new FrameMarker();
      model.addFrameMarker(marker, lowerArm, markerBodyPos);
        
      Particle fixed = new Particle(1.0,new Point3d(-size.z/4,0,-size.z/2.0));
      //        Particle fixed = new Particle(1.0,new Point3d(size.z/4,0,size.z));        
      fixed.setDynamic(false);
      model.addParticle(fixed);

      AxialSpring spring = new AxialSpring(100.0, 2.0, 0.0 );
      spring.setFirstPoint(marker);
      spring.setSecondPoint(fixed);
        
      RenderProps rp = new RenderProps(model.getRenderProps());
      rp.setLineStyle(Renderer.LineStyle.SPINDLE);
      rp.setShading(Renderer.Shading.FLAT);
      rp.setLineColor(Color.WHITE);
      spring.setRenderProps(rp);
        
      model.addAxialSpring(spring);
        
   }
    
   public void addLoad() {
    
      RigidBody lowerArm = model.rigidBodies().get("lower");
      if (lowerArm==null) {        
         return;
      }
        
      double mass = 1.0;
      Particle load = new Particle(mass,new Point3d(-14.14,0,-14.14));
      load.setName("load");
      //        Particle load = new Particle(mass,new Point3d(0,0,0));
        
      RenderProps rp = new RenderProps(model.getRenderProps());
      rp.setShading(Renderer.Shading.SMOOTH);
      rp.setPointColor(Color.ORANGE);
      rp.setPointRadius(len/20);
      load.setRenderProps(rp);
        
      model.addParticle(load);
      model.attachPoint(load, lowerArm, new Point3d(0,0,len/2));
        
   }
    
   public void addEndPoint() {
    
      RigidBody lowerArm = model.rigidBodies().get("lower");
      if (lowerArm==null) {        
         return;
      }
        
      FrameMarker endPoint = new FrameMarker();
      endPoint.setName("endPoint");
      endPoint.setFrame(lowerArm);
      endPoint.setLocation(new Point3d(0,0,len/2));
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
      //rp.setLineStyle(Renderer.LineStyle.ELLIPSOID);
      rp.setLineColor(Color.WHITE);
      rp.setLineRadius(0.4);
      model.setRenderProps(rp);
   }
    
   protected ControlPanel panel;
   public void addPanel () {
      //JFrame frame = driver.getFrame();
      panel = new ControlPanel("Muscle Control", "");
      panel.addWidget (
         "Activation", model, "axialSprings/muscle:excitation", 0.0, 1.0);
      panel.addWidget (
         "alpha", this, "alpha");
      panel.pack();
      panel.setVisible(true);
      // java.awt.Point loc = frame.getLocation();
      // panel.setLocation(loc.x + frame.getWidth(), loc.y);
      addControlPanel (panel);
      Main.getMain().arrangeControlPanels (this);
   }
    
   public void addProbes() {
    
      NumericInputProbe ip;
      NumericOutputProbe op;
      double rate = 0.01;
      try  {        
         String path =
            ArtisynthPath.getHomeRelativePath(
               "src/artisynth/demos/mech/", ".");
         ip = new NumericInputProbe(
            model, "axialSprings/muscle:excitation",
            path+"muscleArmActivation.txt");
         ip.setStartStopTimes (0, 10.0);
         ip.setName("Muscle Activation");
         ip.setActive (false);
         addInputProbe(ip);
      }
      catch (Exception e) { 
         System.out.println ("Error adding probe:");
         e.printStackTrace();
      }
   }

   public void addSkinMesh() {

      PolygonalMesh mesh;
      mesh = MeshFactory.createSphere (10.0, 12, 12);
      mesh.scale (1, 1, 2.5);
      mesh.transform (new RigidTransform3d (-6, 0, 0, 0, Math.toRadians(22.5),0));
      SkinMeshBody skinMesh = new SkinMeshBody (mesh);
      skinMesh.addMasterBody (model.rigidBodies().get(0));
      skinMesh.addMasterBody (model.rigidBodies().get(1));
      skinMesh.computeAllVertexConnections();
      model.addMeshBody (skinMesh);
      RenderProps.setFaceStyle (skinMesh, Renderer.FaceStyle.NONE);
      RenderProps.setDrawEdges (skinMesh, true);
      RenderProps.setLineColor (skinMesh, Color.GRAY);
      mySkinMesh = skinMesh;
   }

   public void addFiberMesh() {

      PolylineMesh mesh = MeshFactory.createSphericalPolyline (8.0, 12, 12);
      mesh.scale (1, 1, 2.5);
      mesh.transform (new RigidTransform3d (-6, 0, 0, 0, Math.toRadians(22.5),0));
      SkinMeshBody fiberMesh = new SkinMeshBody (mesh);
      fiberMesh.addMasterBody (model.rigidBodies().get(0));
      fiberMesh.addMasterBody (model.rigidBodies().get(1));
      fiberMesh.computeWeights();
      model.addMeshBody (fiberMesh);
      RenderProps.setLineColor (fiberMesh, Color.CYAN);
      myFiberMesh = fiberMesh;
      myFiberMesh.addMeshMarker (null, new Point3d (-14.5, 0, 0));
      RenderProps.setSphericalPoints (myFiberMesh, 0.5, Color.WHITE);
   }
    
}
