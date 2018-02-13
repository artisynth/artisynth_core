package artisynth.demos.fem;

import java.awt.Color;
import java.io.IOException;

import javax.swing.JFrame;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.PointToPointMuscle;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.demos.mech.MuscleArm;

public class FemMuscleArm extends MuscleArm
{
    RigidBody upperArm;
    PointToPointMuscle muscle;
        
    public static PropertyList myProps = new PropertyList (
       FemMuscleArm.class, MuscleArm.class);
         
    static
     { myProps.add (
           "collisions * *", "muscle-arm collision enabled", false, "NW");
     }

    public PropertyList getAllPropertyInfo()
     {
       return myProps;
     }

   public void build (String[] args) throws IOException {
        
        model = new MechModel("Arm");
        addModel(model);
             
        model.setIntegrator(MechSystemSolver.Integrator.BackwardEuler);
        model.setMaxStepSize(0.01);
        
        AxialSpring.myIgnoreCoriolisInJacobian = false;

        setupRenderProps();

        addRigidBodies();
        addJoint();
        
//        addFemMuscle(new Point3d(2.4, 0, 20), new Point3d(3, 0, -7));
        addFemMuscle(new Point3d(-1.5,0.0,18),new Point3d(-4, 0.0, -2));

//        addAntagonist();
//        addLoad();
        addEndPoint();
        setCollisions(true);
        addControlPanel();
        addProbes();
    }
    
   public void addFemMuscle(Point3d upper, Point3d lower)   
        throws IOException 
    {
        upperArm = model.rigidBodies().get("upper");
        RigidBody lowerArm = model.rigidBodies().get("lower");
        if (upperArm==null || lowerArm==null)
        {
            return;
        }

        muscle = new PointToPointMuscle("muscle", 1.0,0.85, "muscle", true);
        model.addModel(muscle);
        
        RigidTransform3d X = new RigidTransform3d();
        
//        Point3d upper = new Point3d(2.4, 0, 20), lower = new Point3d(3, 0, -7);
        FemNode3d first = muscle.getFirstNode(), last = muscle.getLastNode();
        
        double scale = (upper.distance(lower)/
                        first.getPosition().distance(last.getPosition()));
        System.out.println(scale);
        AffineTransform3d scaling = new AffineTransform3d();
        scaling.setIdentity();
        scaling.applyScaling(scale, scale, scale);
        //muscle.transformGeometry(scaling);
        
        Vector3d translate = new Vector3d();
        translate.sub(upper, first.getPosition());
        muscle.transformGeometry(
           new RigidTransform3d(translate, new AxisAngle()));
        
        Vector3d offfem = new Vector3d(), offtarget = new Vector3d();
        offfem.sub(last.getPosition(), first.getPosition());
        offtarget.sub(lower, upper);
        double angle = offfem.angle(offtarget);
        offfem.cross(offtarget);
        muscle.transformGeometry(
           new RigidTransform3d(new Vector3d(-upper.x, -upper.y, -upper.z),
                                new AxisAngle()));
        muscle.transformGeometry(
           new RigidTransform3d(upper, new AxisAngle(offfem, angle)));
        
        muscle.getFirstNode().setDynamic(false);

        //ant.getFirstNode().setPosition(new Point3d(2.381, 0.000, 20));
        //model.AttachPoint(ant.getFirstNode(),upperArm,new Point3d(-size.x,0,-(size.z/2.0)/1.2));
        
        model.attachPoint(muscle.getLastNode(),lowerArm);
        
        muscle.setMaxForce(300000);
        muscle.setExcitation (0.0);
        
        RenderProps rp = new RenderProps(model.getRenderProps());
        rp.setFaceColor(Color.RED);
        rp.setLineStyle(Renderer.LineStyle.LINE);
        rp.setPointStyle(Renderer.PointStyle.POINT);
        rp.setShading(Renderer.Shading.SMOOTH);
        rp.setLineColor(Color.WHITE);
        muscle.setRenderProps(rp);

        
        muscle.setSurfaceRendering (SurfaceRender.Shaded);
    }
    
    public void addControlPanel ()
    {
        panel = new ControlPanel("Muscle Control", "");
        panel.addWidget (
           "Activation", model, "models/muscle:excitation", 0.0, 1.0);
        panel.addWidget("Muscle-Arm Collision", this, "collisions");
        addControlPanel (panel);
    }
    
    public void addProbes()
    {
        NumericInputProbe ip;
        NumericOutputProbe op;
        double rate = 0.01;
        try 
        {
           ip = new NumericInputProbe(
              model, "models/muscle:excitation", 0, 2.0);
           ip.addData (new double[] {
                 0, 0, 
                 1, 0.5,
                 2, 0 
              }, NumericInputProbe.EXPLICIT_TIME);
           ip.setName("Muscle Activation");
           addInputProbe (ip);
           
           op = new NumericOutputProbe(
              model, "frameMarkers/endPoint:displacement",
              0, 2, -1);
           op.setName("End Point Displacement");
           addOutputProbe(op);
        }
        catch (Exception e)
        {
           System.out.println(e.getMessage());
        }
        
    }
    
    public void setCollisions(boolean collisions)
    {
       MechModel mech = (MechModel)models().get(0);
       PointToPointMuscle muscle = 
          (PointToPointMuscle)mech.findComponent ("models/0");
       RigidBody upperArm = 
          (RigidBody)mech.findComponent ("rigidBodies/upper");

       if (collisions)
       {
          mech.setCollisionBehavior (muscle, upperArm, true);
       }
       else
       {
          mech.setCollisionBehavior (muscle, upperArm, false);
       }
    }
    
    public boolean getCollisions()
    {
       MechModel mech = (MechModel)models().get(0);
       PointToPointMuscle muscle = 
          (PointToPointMuscle)mech.findComponent ("models/0");
       RigidBody upperArm = 
          (RigidBody)mech.findComponent ("rigidBodies/upper");

       if (muscle == null || upperArm == null) {
          return false;
       }
       CollisionBehavior cb = mech.getCollisionBehavior (muscle, upperArm);
       return cb != null && cb.isEnabled();
    }

}
