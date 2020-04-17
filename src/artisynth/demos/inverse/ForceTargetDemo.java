package artisynth.demos.inverse;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import artisynth.core.inverse.ConnectorForceRenderer;
import artisynth.core.inverse.ForceTarget;
import artisynth.core.inverse.ForceTargetTerm;
import artisynth.core.inverse.TargetPoint;
import artisynth.core.inverse.TrackingController;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.PlanarConnector;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.BodyConnector;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Plane;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.Property;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;

public class ForceTargetDemo extends RootModel{
   MechModel mech;
   Particle p1;
   Particle p2;
   Particle p3;
   Particle p4;
   Particle p5;
   Particle p6;
   Muscle muscle;
   Muscle muscle2;
   Muscle muscle3;
   Muscle muscle4;
   Muscle muscle5;
   Muscle muscle6;
   PlanarConnector con;
   PlanarConnector con2;
   Plane pl;
   boolean cons=true;
   boolean two_cons=false;
   boolean force=true;
   
   RigidBody box;
   public void build (String[] args) throws IOException {
      
      // create MechModel and add to RootModel
      mech = new MechModel ("mech");
      addModel (mech);

      // create the components
      p1 = new Particle ("p1", /*mass=*/2, /*x,y,z=*/0, 0, 0.15);
      p2 = new Particle("p2",2,10,0,0.15);
      p3 = new Particle("p3",2,5,5,0);
      p4 = new Particle("p4",2,5,-5,0);
      p5 = new Particle("p5",0,5,0,5);
      p6 = new Particle("p6",0,5,0,-5);
      box =
      RigidBody.createBox ("box", /*wx,wy,wz=*/0.5, 0.3, 0.3, /*density=*/1);
   box.setPose (new RigidTransform3d (/*x,y,z=*/5, 0, -0.15));
   // create marker point and connect it to the box:
   FrameMarker mkr = new FrameMarker (/*x,y,z=*/0, 0, 0.15);
   mkr.setFrame (box);
      // create the muscle:      
      muscle = new Muscle ("mus1", /*restLength=*/5);
      muscle.setPoints (p1, mkr);
      muscle.setMaterial (
         new SimpleAxialMuscle (/*stiffness=*/0, /*damping=*/10, /*maxf=*/1000));
      
      muscle2 = new Muscle ("mus2", /*restLength=*/5);
      muscle2.setPoints (p2, mkr);
      muscle2.setMaterial (
         new SimpleAxialMuscle (/*stiffness=*/0, /*damping=*/10, /*maxf=*/1000));
      
      muscle3 = new Muscle ("mus3", /*restLength=*/5);
      muscle3.setPoints (p3, mkr);
      muscle3.setMaterial (
         new SimpleAxialMuscle (/*stiffness=*/0, /*damping=*/10, /*maxf=*/4000));
      
      muscle4 = new Muscle ("mus4", /*restLength=*/5);
      muscle4.setPoints (p4, mkr);
      muscle4.setMaterial (
         new SimpleAxialMuscle (/*stiffness=*/0, /*damping=*/10, /*maxf=*/4000));
      
      muscle5 = new Muscle ("mus5", /*restLength=*/5);
      muscle5.setPoints (p5, mkr);
      muscle5.setMaterial (
         new SimpleAxialMuscle (/*stiffness=*/0, /*damping=*/10, /*maxf=*/4000));
     
      muscle6 = new Muscle ("mus6", /*restLength=*/5);
      muscle6.setPoints (p6, mkr);
      muscle6.setMaterial (
         new SimpleAxialMuscle (/*stiffness=*/0, /*damping=*/10, /*maxf=*/4000));
    
      
      if(two_cons==true)
      { 
         RigidTransform3d XPW = new RigidTransform3d(5, 0, 0);
         XPW.R.mulAxisAngle(1, 0, 0, Math.toRadians(90));
      // Connection on the corner
      //PlanarConnector connector =
      //   new PlanarConnector (box, new Vector3d (lenx/2, -2.5, 1.5), XPW);
      // Connection in the center
     
      con =
         new PlanarConnector (box, new Vector3d (0, 0, 0.15), XPW);
      con.setUnilateral (false);
      con.setPlaneSize (2);
      
      RenderProps props = con.createRenderProps();
      props.setPointColor (Color.blue);
      props.setPointStyle (Renderer.PointStyle.SPHERE);
      props.setPointRadius (0.06);
      con.setRenderProps (props);
    //  con = new ParticlePlaneConstraint(p5, pl);
     // RenderProps.setDrawEdges (con, true);
     // RenderProps.setVisible (con, true);
      RigidTransform3d XPW2 = new RigidTransform3d(5, 0, 0);
      XPW2.R.mulAxisAngle(1, 0, 0, 0);
      // Connection on the corner
      //PlanarConnector connector =
      //   new PlanarConnector (box, new Vector3d (lenx/2, -2.5, 1.5), XPW);
      // Connection in the center
      con2 =
         new PlanarConnector (box, new Vector3d (0, 0, 0.15), XPW2);
      con2.setUnilateral (false);
      con2.setPlaneSize (2);
      
      
      con2.setRenderProps (props);
      }
      else
      {
         RigidTransform3d XPW = new RigidTransform3d(5, 0, 0);
         XPW.R.mulAxisAngle(1, 0, 0, Math.toRadians(45));
         con =
         new PlanarConnector (box, new Vector3d (0, 0, 0.15), XPW);
         con.setUnilateral (false);
         con.setPlaneSize (2);
      
         RenderProps props = con.createRenderProps();
         props.setPointColor (Color.blue);
         props.setPointStyle (Renderer.PointStyle.SPHERE);
         props.setPointRadius (0.06);
         con.setRenderProps (props);
      }
      
      
      
      
      
     // con.setRenderProps (myRenderProps);
      // add components to the mech model
      mech.addParticle (p1);
      mech.addParticle (p2);
      mech.addParticle (p3);
      mech.addParticle (p4);
      mech.addParticle (p5);
      mech.addParticle (p6);
      
      mech.addRigidBody (box);
      mech.addFrameMarker (mkr);
      //mech.addParticle (p5);
      
      mech.addAxialSpring (muscle);
      mech.addAxialSpring (muscle2);
      mech.addAxialSpring (muscle3);
      mech.addAxialSpring (muscle4);
      mech.addAxialSpring (muscle5);
      mech.addAxialSpring (muscle6);
      
      con.setName("con1");
      if(two_cons==true)
      {con2.setName("con2");}
      if(cons==true)
      { mech.addBodyConnector (con);
      ConnectorForceRenderer rend= new ConnectorForceRenderer(con);
      myRenderProps=rend.createRenderProps ();
      myRenderProps.setLineStyle(LineStyle.CYLINDER);
      myRenderProps.setLineRadius(0.175);
      myRenderProps.setLineColor (Color.BLUE);
      rend.setRenderProps(myRenderProps);
      rend.setArrowSize (0.1);
      addMonitor(rend);
      }
      if(two_cons==true)
      { mech.addBodyConnector (con2);}
     
      
      
      p1.setDynamic (false);               // first particle set to be fixed
      p2.setDynamic (false);
      p3.setDynamic (false);
      p4.setDynamic (false);
      p5.setDynamic (false);
      p6.setDynamic (false);
      // increase model bounding box for the viewer
      mech.setBounds (/*min=*/-1, 0, -1, /*max=*/1, 0, 0);  
      // set render properties for the components
      setPointRenderProps (p1);
      setPointRenderProps (p2);
      setPointRenderProps (p3);
      setPointRenderProps (p4);
      setPointRenderProps (p5);
      setPointRenderProps (p6);
      
      setPointRenderProps (mkr);
      setLineRenderProps (muscle);
      setLineRenderProps (muscle2);
      setLineRenderProps (muscle3);
      setLineRenderProps (muscle4);
      setLineRenderProps (muscle5);
      setLineRenderProps (muscle6);
      
      addTrackingController(mkr);
      
      if(cons=true)
      {addConForceProbe(10,0.1);}
   }

   protected void setPointRenderProps (Renderable r) {
      RenderProps.setPointColor (r, Color.BLUE);
      RenderProps.setPointStyle (r, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (r, 0.06);
   }

   protected void setLineRenderProps (Renderable r) {
      RenderProps.setLineColor (r, Color.RED);
      RenderProps.setLineStyle (r, LineStyle.SPINDLE);
      RenderProps.setLineRadius (r, 0.2);
   }
   
   public void addTrackingController(FrameMarker mkr) throws IOException {
      TrackingController myTrackingController = new TrackingController(mech, "tcon");
      for (AxialSpring s : mech.axialSprings()) {
         if (s instanceof Muscle) {
            myTrackingController.addExciter((Muscle)s);
         }
      }
//      myTrackingController.addTerm(new StiffnessTerm(model, center));
//      StiffnessTerm kTerm = TrackingController.addStiffnessTarget(center, new int[]{0,2});
//      kTerm.setStiffnessTargetType(StiffnessTargetType.DIAG);
      
//      ComplianceTerm2d cterm = new ComplianceTerm2d(TrackingController, center, new int[]{0,2});
//      cterm.setComplianceTargetType(StiffnessTargetType.DIAG);
//      TrackingController.addTerm(cterm);
      
      myTrackingController.addL2RegularizationTerm();
//      myTrackingController.addTerm(new DampingTerm(TrackingController));

//      myTrackingController.addTerm(new StaticMotionTargetTerm(TrackingController));
      myTrackingController.addMotionTarget(mkr);
      setPointRenderProps((TargetPoint) myTrackingController.getMotionTargets ().get (0));
     
//      ForceTargetTerm mft=new ForceTargetTerm(myTrackingController);
      
      // mft.addMotionTarget(mkr);
      //  setPointRenderProps((TargetPoint) mft.getMotionTargets ().get (0));
      double[] lam={-3.5};
      VectorNd tarlam= new VectorNd (lam);
      if (force) {
         ForceTargetTerm mft = myTrackingController.addForceTargetTerm();
         if(cons==true) {
            ForceTarget ft = mft.addForceTarget (con);
            ft.setTargetLambda (tarlam);
         }
         if(two_cons==true) {
            ForceTarget ft = mft.addForceTarget (con2);
            ft.setTargetLambda (tarlam);
         }
      }
      //      myTrackingController.getSolver().setBounds(0.01, 0.99);
      //setWorkingDir();

      //      loadForceInputProbe(mft);
      myTrackingController.setProbeDuration (10.0);
      myTrackingController.createProbesAndPanel (this);
      addController(myTrackingController);

      reloadTargetProbeData();
   }
   
   public void reloadTargetProbeData() throws IOException
   {
      ArtisynthPath.setWorkingDir (new File(
         ArtisynthPath.getSrcRelativePath (this, "data/ForceTargetDemo")));
      
      NumericInputProbe forceTargetProbe = (NumericInputProbe)getInputProbes ().get ("target forces");
      forceTargetProbe.setAttachedFileName ("force_target.txt");
      forceTargetProbe.load ();
      forceTargetProbe.setActive (true);
      
      NumericInputProbe motionTargetProbe = (NumericInputProbe)getInputProbes ().get ("target positions");
      motionTargetProbe.setAttachedFileName ("motion_target.txt");
      motionTargetProbe.load ();
      motionTargetProbe.setActive (true);
   }
   
   public void loadForceInputProbe(ForceTargetTerm mft) throws IOException
   {
      ArtisynthPath.setWorkingDir (new File(
         ArtisynthPath.getSrcRelativePath (this, "data/")));

      Property proparr[]=new Property[mft.getForceTargets ().size()];
      for(int i=0;i<mft.getForceTargets ().size();i++)
      {
      System.out.println(mft.getForceTargets ().get(i).getConnector().getName());
       proparr[i]=mft.getForceTargets().get(i).getProperty ("targetLambda");
      }
      NumericInputProbe forprobe = new NumericInputProbe();
      forprobe.setModel(mech);
      forprobe.setInputProperties (proparr);
      forprobe.setInterval (0, 1);
      forprobe.setName("Force Target");
      if(two_cons==true)
      {forprobe.setAttachedFileName("inputforcetarget3.txt");}
      else
      {forprobe.setAttachedFileName("inputforcetarget2.txt");}
      forprobe.setStartStopTimes (0, 10);
      addInputProbe (forprobe);
      forprobe.load ();
   }
   
   public void addConForceProbe(double duration, double interval) {
      ArrayList<Property> props = new ArrayList<Property>();
      for (BodyConnector rbc : mech.bodyConnectors()) {
         if (rbc.isEnabled() && rbc.getProperty("activation") != null) {
            props.add(rbc.getProperty("activation"));
         }
      }

      Property[] proparray = new Property[props.size()];
      for (int i = 0; i < props.size(); i++) {
         proparray[i] = props.get(i);
      }

      String name = "conforce";

      NumericOutputProbe p = new NumericOutputProbe(proparray, interval);
      p.setStartStopTimes(0, duration);
      p.setName(name);
      p.setAttachedFileName(name + "_output.txt");
      p.setDefaultDisplayRange(-0.1, 0.1);
      addOutputProbe(p);

   }

}
