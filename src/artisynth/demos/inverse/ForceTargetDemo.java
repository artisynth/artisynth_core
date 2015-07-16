package artisynth.demos.inverse;

import java.awt.Color;
import java.io.IOException;

import maspack.matrix.Plane;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import artisynth.core.driver.Main;
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
import artisynth.core.workspace.RootModel;

public class ForceTargetDemo extends RootModel{
   MechModel mech;
   Particle p1;
   Particle p2;
   Particle p3;
   Particle p4;
   Muscle muscle;
   Muscle muscle2;
   Muscle muscle3;
   Muscle muscle4;
   PlanarConnector con;
   Plane pl;
   
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
         new SimpleAxialMuscle (/*stiffness=*/0, /*damping=*/10, /*maxf=*/200));
      
      muscle2 = new Muscle ("mus2", /*restLength=*/5);
      muscle2.setPoints (p2, mkr);
      muscle2.setMaterial (
         new SimpleAxialMuscle (/*stiffness=*/0, /*damping=*/10, /*maxf=*/200));
      
      muscle3 = new Muscle ("mus3", /*restLength=*/5);
      muscle3.setPoints (p3, mkr);
      muscle3.setMaterial (
         new SimpleAxialMuscle (/*stiffness=*/0, /*damping=*/10, /*maxf=*/500));
      
      muscle4 = new Muscle ("mus4", /*restLength=*/5);
      muscle4.setPoints (p4, mkr);
      muscle4.setMaterial (
         new SimpleAxialMuscle (/*stiffness=*/0, /*damping=*/10, /*maxf=*/500));
     
      
    
      
      
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
      props.setPointStyle (RenderProps.PointStyle.SPHERE);
      props.setPointRadius (0.06);
      con.setRenderProps (props);
    //  con = new ParticlePlaneConstraint(p5, pl);
     // RenderProps.setDrawEdges (con, true);
     // RenderProps.setVisible (con, true);
      
      
      
      
      
      
      
     // con.setRenderProps (myRenderProps);
      // add components to the mech model
      mech.addParticle (p1);
      mech.addParticle (p2);
      mech.addParticle (p3);
      mech.addParticle (p4);
   
      mech.addRigidBody (box);
      mech.addFrameMarker (mkr);
      //mech.addParticle (p5);
      
      mech.addAxialSpring (muscle);
      mech.addAxialSpring (muscle2);
      mech.addAxialSpring (muscle3);
      mech.addAxialSpring (muscle4);
      
      mech.addRigidBodyConnector (con);
      
     
      
      
      p1.setDynamic (false);               // first particle set to be fixed
      p2.setDynamic (false);
      p3.setDynamic (false);
      p4.setDynamic (false);
      //p5.setDynamic (false);
      // increase model bounding box for the viewer
      mech.setBounds (/*min=*/-1, 0, -1, /*max=*/1, 0, 0);  
      // set render properties for the components
      setPointRenderProps (p1);
      setPointRenderProps (p2);
      setPointRenderProps (p3);
      setPointRenderProps (p4);
     // setPointRenderProps (p5);
      setPointRenderProps (mkr);
      setLineRenderProps (muscle);
      setLineRenderProps (muscle2);
      setLineRenderProps (muscle3);
      setLineRenderProps (muscle4);
      
    
      addTrackingController(mkr);
   }

   protected void setPointRenderProps (Renderable r) {
      RenderProps.setPointColor (r, Color.BLUE);
      RenderProps.setPointStyle (r, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (r, 0.06);
   }

   protected void setLineRenderProps (Renderable r) {
      RenderProps.setLineColor (r, Color.RED);
      RenderProps.setLineStyle (r, RenderProps.LineStyle.ELLIPSOID);
      RenderProps.setLineRadius (r, 0.2);
   }
   
   public void addTrackingController(FrameMarker mkr) {
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
     
      ForceTargetTerm mft=new ForceTargetTerm(myTrackingController);
     // mft.addMotionTarget(mkr);
    //  setPointRenderProps((TargetPoint) mft.getMotionTargets ().get (0));
      double[] lam={-3.5};
      VectorNd tarlam= new VectorNd (lam);
      mft.addForceTarget (con, tarlam);
      myTrackingController.addCostTerm (mft);
//      myTrackingController.getSolver().setBounds(0.01, 0.99);
      myTrackingController.setProbeDuration (10.0);
      myTrackingController.createProbesAndPanel(this);
      addController(myTrackingController);
   }

}
