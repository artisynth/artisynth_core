package artisynth.demos.mech;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver;
import artisynth.core.mechmodels.Muscle; 
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.spatialmotion.SpatialInertia;

public class DoubleArmDemo extends RootModel {
   
   protected MechModel model;
   protected double len = 20;
   Vector3d size = new Vector3d(len/10,len/5,len);
   boolean addCompression = true;
   
   public void build (String[] args) {

      model = new MechModel("Arm");
      addModel(model);
           
      model.setIntegrator(MechSystemSolver.Integrator.RungeKutta4);
      model.setMaxStepSize(0.01);

      setupRenderProps();
      addRigidBodies();
      addJoint();
      addMuscles();
      addEndPoint();
      addPanel();
   }

   public void addRigidBodies()
   {
       RigidTransform3d X;
       
       X= new RigidTransform3d();
       X.p.z = len/2;
       addBody("upper",X, "barm.obj");
       
       X = new RigidTransform3d();
       double angle = Math.toRadians(225);
       X.R.setAxisAngle(0,1,0,angle);
       X.p.set(len/2*Math.sin(angle), 0.0,len/2*Math.cos(angle));
       addBody("lower",X, "barm.obj");        
       
       X.p.set(len*Math.sin(angle),0,len*Math.cos(angle));	// get to end of body
       RigidTransform3d X2 = new RigidTransform3d();
       angle = Math.toRadians(25);
       X2.R.setAxisAngle(0,1,0,angle);
       X2.p.set(len/2*Math.sin(angle),0,len/2*Math.cos(angle));
       X.mul(X2);	// concatenate;
       addBody("third", X, "barm.obj");
       
   }
   
   public void addBody(String name, RigidTransform3d pose, 
               String meshName)
   {
       // add a simple rigid body to the simulation
       
       RigidBody rb = new RigidBody();
       rb.setName(name);
       rb.setPose(pose);

       model.addRigidBody(rb);
       
       PolygonalMesh mesh;
       try
       {
           String meshFilename = ArtisynthPath.getHomeRelativePath(
                       "src/artisynth/demos/mech/geometry/",".") + meshName;
           mesh = new PolygonalMesh();
           mesh.read(
              new BufferedReader(
                   new FileReader(
                        new File(meshFilename))));
           rb.setMesh(mesh, meshFilename);
       }
       catch(IOException e)
       {
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
   }
   
   public void addJoint()
   {
       RigidBody upperArm = model.rigidBodies().get("upper");
       RigidBody lowerArm = model.rigidBodies().get("lower");
       RigidBody thirdArm = model.rigidBodies().get("third");
       if (upperArm==null || lowerArm==null || thirdArm == null) {
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
       
       // add joint between lower and third
       j =new HingeJoint();
       j.setName("wrist");
       TCA = new RigidTransform3d();
       TCA.p.z = -len/2;
       TCA.R.setAxisAngle(1,0,0,Math.PI/2);
       TCW = new RigidTransform3d();
       TCW.p.z =len/2;
       TCW.R.setAxisAngle(1,0,0,Math.PI/2);
       j.setBodies(thirdArm, TCA, lowerArm, TCW);
       j.setShaftLength(len/3);
       RenderProps.setFaceColor (j, Color.BLUE);
       model.addBodyConnector(j);
       
   }
   
   public void addMuscles()
   {
       RigidBody upperArm = model.rigidBodies().get("upper");
       RigidBody lowerArm = model.rigidBodies().get("lower");
       RigidBody thirdArm = model.rigidBodies().get("third");
       if (upperArm==null || lowerArm==null || thirdArm == null)
       {
           return;
       }
       
       Point3d markerBodyPos = new Point3d(-size.x/2,0,(size.z/2.0)/1.2);
       FrameMarker u = new FrameMarker();
       model.addFrameMarker(u, upperArm, markerBodyPos);
       u.setName("upperAttachment");
       
       markerBodyPos = new Point3d(size.x/2,0,(size.z/2.0)/1.2);
       FrameMarker tu = new FrameMarker();
       model.addFrameMarker(tu, thirdArm, markerBodyPos);
       tu.setName("thirdUpperAttachment");
       
       markerBodyPos = new Point3d(size.x/2,0,-(size.z/2.0)/2);
       FrameMarker l = new FrameMarker();
       model.addFrameMarker(l,lowerArm, markerBodyPos);
       l.setName("lowerAttachment");
       
       markerBodyPos = new Point3d(size.x/2, 0, (size.z/2.0)/2 );
       FrameMarker tl = new FrameMarker();
       model.addFrameMarker(tl, lowerArm, markerBodyPos);
       tl.setName("thirdLowerAttachment");
       
       Muscle muscle = new Muscle("muscle");
       muscle.setPeckMuscleMaterial(20.0, 22.0, 30, 0.2, 0.5, 0.1);
       Muscle muscle2 = new Muscle("muscle2");
       muscle2.setPeckMuscleMaterial(8,20,30,0.2, 0.5, 0.1);

       muscle.setFirstPoint(u);
       muscle2.setFirstPoint(tu);
       muscle.setSecondPoint(l);
       muscle2.setSecondPoint(tl);
       
       RenderProps rp = new RenderProps(model.getRenderProps());
       rp.setLineStyle(Renderer.LineStyle.SPINDLE);
       rp.setLineRadius(len/20);
       //rp.setLineSlices(10);
       rp.setShading(Renderer.Shading.SMOOTH);
       rp.setLineColor(Color.RED);
       muscle.setRenderProps(rp);
       muscle2.setRenderProps(rp);
       
       model.addAxialSpring(muscle);
       model.addAxialSpring(muscle2);
       
       if (addCompression)
       {
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
          
          
          // restoring spring
          len = tu.getPosition().distance(tl.getPosition());
          s = new AxialSpring(10,0,2*len);
          s.setFirstPoint(tu);
          s.setSecondPoint(tl);
          model.addAxialSpring(s);
          s.setRenderProps(props);
          
       }
   }
   
   public void addEndPoint()
   {
       RigidBody thirdArm = model.rigidBodies().get("third");
       if (thirdArm==null)
       {
           return;
       }
       
       FrameMarker endPoint = new FrameMarker();
       endPoint.setName("endPoint");
       endPoint.setFrame(thirdArm);
       endPoint.setLocation(new Point3d(0,0,len/2));
       model.addFrameMarker(endPoint);
       //lowerArm.addMarker(endPoint);
       
       RenderProps rp = new RenderProps(model.getRenderProps());
       rp.setShading(Renderer.Shading.SMOOTH);
       rp.setPointColor(Color.ORANGE);
       rp.setPointRadius(len/20);
       endPoint.setRenderProps(rp);
   }
   
   public void setupRenderProps()
   {
      // set render properties for model
      
      RenderProps rp = new RenderProps();
      rp.setPointStyle(Renderer.PointStyle.SPHERE);
      rp.setPointColor(Color.LIGHT_GRAY);
      rp.setPointRadius(0.0);
      rp.setLineStyle(Renderer.LineStyle.SPINDLE);
      rp.setLineColor(Color.WHITE);
      rp.setLineRadius(0.4);
      model.setRenderProps(rp);
   }
   
   protected ControlPanel panel;
   public void addPanel ()
   {

       panel = new ControlPanel("Muscle Control", "");
       panel.addWidget (
          "Upper Muscle", model, "axialSprings/muscle:excitation", 0.0, 1.0);
       panel.addWidget (
             "Lower Muscle", model, "axialSprings/muscle2:excitation", 0.0, 1.0);       
       addControlPanel (panel);
  }
   
}
