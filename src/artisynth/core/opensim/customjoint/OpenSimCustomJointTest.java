package artisynth.core.opensim.customjoint;

import java.io.IOException;

import artisynth.core.mechmodels.BodyConnector;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RevoluteJoint;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RollPitchJoint;
import artisynth.core.mechmodels.SphericalRpyJoint;
import artisynth.core.modelbase.MonitorBase;
import artisynth.core.workspace.RootModel;
import artisynth.core.opensim.components.Constant;
import artisynth.core.opensim.components.Coordinate;
import artisynth.core.opensim.components.LinearFunction;
import artisynth.core.opensim.components.TransformAxis;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer.LineStyle;
import maspack.util.DoubleInterval;

public class OpenSimCustomJointTest extends RootModel {

   MechModel mech;
   Coordinate[] coords;
   TransformAxis[] axes;

   public static class JointMonitor extends MonitorBase {
      BodyConnector joint;
      boolean enabled = true;

      static PropertyList myProps = new PropertyList(JointMonitor.class, MonitorBase.class);

      static {
         myProps.add("enabled * *", "enabled or not", true);
      }

      @Override
      public PropertyList getAllPropertyInfo() {
         return myProps;
      }

      public JointMonitor(BodyConnector joint) {
         this.joint = joint;
      }

      public boolean getEnabled() {
         return enabled;
      }

      public void setEnabled(boolean set) {
         enabled = set;
      }

      @Override
      public void apply(double t0, double t1) {
         if (enabled) {
            System.out.println(joint.getName() + ":");
            joint.printConstraintInfo();
            System.out.println();
         }
      }
   }

   @Override
   public void build(String[] args) throws IOException {
      super.build(args);

      mech = new MechModel("mech");
      addModel(mech);

//      addCustomRevoluteJoint(new Vector3d(0,0,0)); 
//      addRevoluteJoint(new Vector3d(0,0.5,0));
//      addCustomRollPitchJoint(new Vector3d(0,1,0));
//      addRollPitchJoint(new Vector3d(0,1.5,0));
//      addCustomRollPitchYawJoint(new Vector3d(0, 2, 0));
//      addRollPitchYawJoint(new Vector3d(0, 2.5, 0));
//      addRollPitchSlideJoint(new Vector3d(0,3.0,0));
//      addSlidingZYXJoint(new Vector3d(0,3.5,0));
//      addSlidingZJoint(new Vector3d(0, 4, 0));
//    addRotYSlidingXJoint(new Vector3d(0,4.5,0));
            add6DOFJoint(new Vector3d(0,5,0));


      mech.setGravity(0, 0, -9.8);

      //      for (BodyConnector connector : mech.bodyConnectors()) {
      //         addMonitor(new JointMonitor(connector));
      //      }

   }

   protected void addCustomRevoluteJoint(Vector3d offset) {

      PolygonalMesh boxMesh1 = MeshFactory.createBox(1, 0.2, 0.2);

      RigidBody rbox1 = new RigidBody("crbox1");
      rbox1.addMesh(boxMesh1);
      rbox1.setPose(new RigidTransform3d(new Vector3d(1,0,0), AxisAngle.IDENTITY));
      mech.addRigidBody(rbox1);

      coords = new Coordinate[1];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue (0);
      coords[0].setLocked (false);
      coords[0].setName("1");
      coords[0].setRange (-0.5, 0.5);
      coords[0].setClamped (true);

      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setCoordinates (new String[] {"1"});
      axes[0].setFunction (new LinearFunction(new double[] {1, 0}));

      axes[1] = new TransformAxis();
      axes[1].setAxis (new Vector3d(0, 1, 0));
      axes[1].setFunction(new Constant(0));

      axes[2] = new TransformAxis();
      axes[2].setAxis (new Vector3d(1, 0, 0));
      axes[2].setFunction(new Constant(0));

      axes[3] = new TransformAxis();
      axes[3].setAxis (new Vector3d(1, 0, 0));
      axes[3].setFunction (new Constant(0));

      axes[4] = new TransformAxis();
      axes[4].setAxis (new Vector3d(0, 1, 0));
      axes[4].setFunction (new Constant(0));

      axes[5] = new TransformAxis();
      axes[5].setAxis (new Vector3d(0, 0, 1));
      axes[5].setFunction (new Constant(0));

      OpenSimCustomJoint customJoint = new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSim Revolute Joint");
      customJoint.setBodies(
         rbox1, new RigidTransform3d(-1, 0, 0), null, new RigidTransform3d(offset, AxisAngle.IDENTITY));
      customJoint.setAxisLength(0.2);
      mech.addBodyConnector(customJoint);

      rbox1.transformPose(new RigidTransform3d(offset, AxisAngle.IDENTITY));
   }

   protected void addRevoluteJoint(Vector3d offset) {

      PolygonalMesh boxMesh1 = MeshFactory.createBox(1, 0.2, 0.2);
      RigidBody rbox1 = new RigidBody("rbox1");
      rbox1.addMesh(boxMesh1);
      rbox1.setPose(new RigidTransform3d(new Vector3d(1,0,0), AxisAngle.IDENTITY));
      mech.addRigidBody(rbox1);

      RevoluteJoint joint = new RevoluteJoint();
      joint.setName("Revolute Joint");
      joint.setBodies(
         rbox1, new RigidTransform3d(-1, 0, 0), null, new RigidTransform3d(offset, AxisAngle.IDENTITY));
      joint.setThetaRange(new DoubleInterval(Math.toDegrees(-0.5), Math
         .toDegrees(0.5)));
      joint.setAxisLength(0.2);
      mech.addBodyConnector(joint);
      RenderProps.setLineStyle(joint, LineStyle.LINE);

      rbox1.transformPose(new RigidTransform3d(offset, AxisAngle.IDENTITY));
   }

   protected void addCustomRollPitchJoint(Vector3d offset) {

      PolygonalMesh boxMesh1 = MeshFactory.createBox(1, 0.2, 0.2);
      RigidBody rbox1 = new RigidBody("crpbox1");
      rbox1.addMesh(boxMesh1);
      rbox1.setPose(new RigidTransform3d(new Vector3d(1,0,0), AxisAngle.IDENTITY));
      mech.addRigidBody(rbox1);

      coords = new Coordinate[2];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue (0);
      coords[0].setLocked(false);
      coords[0].setName ("1");
      coords[0].setRange (new DoubleInterval(-0.5, 0.5));
      coords[0].setClamped (true);

      coords[1] = new Coordinate();
      coords[1].setDefaultValue (0);
      coords[1].setLocked (false);
      coords[1].setName("2");
      coords[1].setRange( new DoubleInterval(-0.3, 0.3));
      coords[1].setClamped(true);

      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setCoordinates (new String[] {"1"});
      axes[0].setFunction (new LinearFunction(new double[] {1, 0}));

      axes[1] = new TransformAxis();
      axes[1].setAxis (new Vector3d(0, 1, 0));
      axes[1].setCoordinates (new String[] {"2"});
      axes[1].setFunction(new LinearFunction(new double[]{1,0}));

      axes[2] = new TransformAxis();
      axes[2].setAxis (new Vector3d(1, 0, 0));
      axes[2].setFunction(new Constant(0));

      axes[3] = new TransformAxis();
      axes[3].setAxis (new Vector3d(1, 0, 0));
      axes[3].setFunction (new Constant(0));

      axes[4] = new TransformAxis();
      axes[4].setAxis (new Vector3d(0, 1, 0));
      axes[4].setFunction (new Constant(0));

      axes[5] = new TransformAxis();
      axes[5].setAxis (new Vector3d(0, 0, 1));
      axes[5].setFunction (new Constant(0));

      OpenSimCustomJoint customJoint =
      new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSim Roll-Pitch Joint");
      customJoint.setBodies(
         rbox1, new RigidTransform3d(-1, 0, 0), null, new RigidTransform3d(offset, AxisAngle.IDENTITY));
      customJoint.setAxisLength(0.2);
      mech.addBodyConnector(customJoint);

      rbox1.transformPose(new RigidTransform3d(offset, AxisAngle.IDENTITY));
   }

   protected void addRollPitchSlideJoint(Vector3d offset) {

      PolygonalMesh boxMesh1 = MeshFactory.createBox(1, 0.2, 0.2);
      RigidBody rbox1 = new RigidBody("crpsbox1");
      rbox1.addMesh(boxMesh1);
      rbox1.setPose(new RigidTransform3d(new Vector3d(1,0,0), AxisAngle.IDENTITY));
      mech.addRigidBody(rbox1);

      coords = new Coordinate[3];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue (0);
      coords[0].setLocked(false);
      coords[0].setName ("1");
      coords[0].setRange (new DoubleInterval(-0.5, 0.5));
      coords[0].setClamped (true);

      coords[1] = new Coordinate();
      coords[1].setDefaultValue (0);
      coords[1].setLocked (false);
      coords[1].setName("2");
      coords[1].setRange( new DoubleInterval(-0.3, 0.3));
      coords[1].setClamped(true);

      coords[2] = new Coordinate();
      coords[2].setDefaultValue( 0 );
      coords[2].setLocked( false );
      coords[2].setName("3");
      coords[2].setRange(new DoubleInterval(0, 0.2));
      coords[2].setClamped(true);

      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setCoordinates (new String[] {"1"});
      axes[0].setFunction (new LinearFunction(new double[] {1, 0}));

      axes[1] = new TransformAxis();
      axes[1].setAxis (new Vector3d(0, 1, 0));
      axes[1].setCoordinates (new String[] {"2"});
      axes[1].setFunction(new LinearFunction(new double[]{1,0}));

      axes[2] = new TransformAxis();
      axes[2].setAxis (new Vector3d(1, 0, 0));
      axes[2].setFunction(new Constant(0));

      axes[3] = new TransformAxis();
      axes[3].setAxis (new Vector3d(1, 0, 0));
      axes[3].setCoordinates (new String[] {"3"});
      axes[3].setFunction (new LinearFunction(new double[] {1,0}));

      axes[4] = new TransformAxis();
      axes[4].setAxis (new Vector3d(0, 1, 0));
      axes[4].setFunction (new Constant(0));

      axes[5] = new TransformAxis();
      axes[5].setAxis (new Vector3d(0, 0, 1));
      axes[5].setFunction (new Constant(0));

      OpenSimCustomJoint customJoint =
      new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSim Roll-Pitch Sliding Joint");
      customJoint.setBodies(
         rbox1, new RigidTransform3d(-1, 0, 0), null, new RigidTransform3d(offset, AxisAngle.IDENTITY));
      customJoint.setAxisLength(0.2);
      mech.addBodyConnector(customJoint);

      rbox1.transformPose(new RigidTransform3d(offset, AxisAngle.IDENTITY));
   }


   protected void addRollPitchYawJoint(Vector3d offset) {

      PolygonalMesh boxMesh1 = MeshFactory.createBox(1, 0.2, 0.2);
      RigidBody rbox1 = new RigidBody("rpybox1");
      rbox1.addMesh(boxMesh1);
      rbox1.setPose(new RigidTransform3d(new Vector3d(1,0,0), AxisAngle.IDENTITY));
      mech.addRigidBody(rbox1);

      SphericalRpyJoint joint = new SphericalRpyJoint();
      joint.setName("Roll-Pitch-Yaw Joint");
      joint.setBodies(
         rbox1, new RigidTransform3d(-1, 0, 0), null, new RigidTransform3d(offset, AxisAngle.IDENTITY));

      joint.setRollRange(new DoubleInterval(Math.toDegrees(-0.5), Math
         .toDegrees(0.5)));
      joint.setPitchRange(new DoubleInterval(Math.toDegrees(-0.3), Math
         .toDegrees(0.3)));
      joint.setYawRange(new DoubleInterval(Math.toDegrees(-0.5), Math
         .toDegrees(0.5)));

      joint.setAxisLength(0.2);
      mech.addBodyConnector(joint);
      RenderProps.setLineStyle(joint, LineStyle.LINE);

      rbox1.transformPose(new RigidTransform3d(offset, AxisAngle.IDENTITY));
   }

   protected void addCustomRollPitchYawJoint(Vector3d offset) {

      PolygonalMesh boxMesh1 = MeshFactory.createBox(1, 0.2, 0.2);
      RigidBody rbox1 = new RigidBody("crpybox1");
      rbox1.addMesh(boxMesh1);
      rbox1.setPose(new RigidTransform3d(new Vector3d(1,0,0), AxisAngle.IDENTITY));
      mech.addRigidBody(rbox1);

      coords = new Coordinate[3];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue (0);
      coords[0].setLocked(false);
      coords[0].setName ("1");
      coords[0].setRange (new DoubleInterval(-0.5, 0.5));
      coords[0].setClamped (true);

      coords[1] = new Coordinate();
      coords[1].setDefaultValue (0);
      coords[1].setLocked (false);
      coords[1].setName("2");
      coords[1].setRange( new DoubleInterval(-0.4, 0.4));
      coords[1].setClamped(true);

      coords[2] = new Coordinate();
      coords[2].setDefaultValue( 0 );
      coords[2].setLocked( false );
      coords[2].setName("3");
      coords[2].setRange(new DoubleInterval(-0.3, 0.3));
      coords[2].setClamped(true);

      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setCoordinates (new String[] {"1"});
      axes[0].setFunction (new LinearFunction(new double[] {1, 0}));

      axes[1] = new TransformAxis();
      axes[1].setAxis (new Vector3d(0, 1, 0));
      axes[1].setCoordinates (new String[] {"2"});
      axes[1].setFunction(new LinearFunction(new double[]{1,0}));

      axes[2] = new TransformAxis();
      axes[2].setAxis (new Vector3d(1, 0, 0));
      axes[2].setCoordinates (new String[] {"3"});
      axes[2].setFunction(new LinearFunction(new double[]{1,0}));

      axes[3] = new TransformAxis();
      axes[3].setAxis (new Vector3d(1, 0, 0));
      axes[3].setFunction (new Constant(0));

      axes[4] = new TransformAxis();
      axes[4].setAxis (new Vector3d(0, 1, 0));
      axes[4].setFunction (new Constant(0));

      axes[5] = new TransformAxis();
      axes[5].setAxis (new Vector3d(0, 0, 1));
      axes[5].setFunction (new Constant(0));

      OpenSimCustomJoint customJoint =
      new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSim Roll-Pitch-Yaw Joint");
      customJoint.setBodies(
         rbox1, new RigidTransform3d(-1, 0, 0), null, new RigidTransform3d(offset, AxisAngle.IDENTITY));
      customJoint.setAxisLength(0.2);
      mech.addBodyConnector(customJoint);

      rbox1.transformPose(new RigidTransform3d(offset, AxisAngle.IDENTITY));
   }

   protected void addRollPitchJoint(Vector3d offset) {

      PolygonalMesh boxMesh1 = MeshFactory.createBox(1, 0.2, 0.2);

      RigidBody rbox1 = new RigidBody("rpbox1");
      rbox1.addMesh(boxMesh1);
      rbox1.setPose(new RigidTransform3d(new Vector3d(1,0,0), AxisAngle.IDENTITY));
      mech.addRigidBody(rbox1);

      RollPitchJoint joint = new RollPitchJoint();
      joint.setName("Roll-Pitch Joint");
      joint.setBodies(
         rbox1, new RigidTransform3d(-1, 0, 0), null, new RigidTransform3d(offset, AxisAngle.IDENTITY));
      joint.setRollRange(new DoubleInterval(Math.toDegrees(-0.5), Math
         .toDegrees(0.5)));
      joint.setPitchRange(new DoubleInterval(Math.toDegrees(-0.3), Math
         .toDegrees(0.3)));
      joint.setAxisLength(0.2);
      mech.addBodyConnector(joint);
      RenderProps.setLineStyle(joint, LineStyle.LINE);

      rbox1.transformPose(new RigidTransform3d(offset, AxisAngle.IDENTITY));
   }

   protected void addSlidingZJoint(Vector3d offset) {

      PolygonalMesh boxMesh1 = MeshFactory.createBox(1, 0.2, 0.2);
      RigidBody rbox1 = new RigidBody("tzbox1");
      rbox1.addMesh(boxMesh1);
      rbox1.setPose(new RigidTransform3d(new Vector3d(1,0,0), AxisAngle.IDENTITY));
      mech.addRigidBody(rbox1);

      coords = new Coordinate[1];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue(0);
      coords[0].setLocked(false);
      coords[0].setName("1");
      coords[0].setRange( new DoubleInterval(-0.5, 1));
      coords[0].setClamped (true);

      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setFunction (new Constant(0));

      axes[1] = new TransformAxis();
      axes[1].setAxis (new Vector3d(0, 1, 0));
      axes[1].setFunction(new Constant(0));

      axes[2] = new TransformAxis();
      axes[2].setAxis (new Vector3d(1, 0, 0));
      axes[2].setFunction(new Constant(0));

      axes[3] = new TransformAxis();
      axes[3].setAxis (new Vector3d(1, 0, 0));
      axes[3].setFunction (new Constant(0));

      axes[4] = new TransformAxis();
      axes[4].setAxis (new Vector3d(0, 1, 0));
      axes[4].setFunction (new Constant(0));

      axes[5] = new TransformAxis();
      axes[5].setAxis (new Vector3d(0, 0, 1));
      axes[5].setCoordinates (new String[] {"1"});
      axes[5].setFunction (new LinearFunction(new double[] {1,0}));

      OpenSimCustomJoint customJoint =
      new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSim Sliding-Z Joint");
      customJoint.setBodies(
         rbox1, new RigidTransform3d(-1, 0, 0), null, new RigidTransform3d(offset, AxisAngle.IDENTITY));
      customJoint.setAxisLength(0.2);
      mech.addBodyConnector(customJoint);

      rbox1.transformPose(new RigidTransform3d(offset, AxisAngle.IDENTITY));
   }

   protected void addRotYSlidingXJoint(Vector3d offset) {

      PolygonalMesh boxMesh1 = MeshFactory.createBox(1, 0.2, 0.2);
      RigidBody rbox1 = new RigidBody("rytzbox1");
      rbox1.addMesh(boxMesh1);
      rbox1.setPose(new RigidTransform3d(new Vector3d(1,0,0), AxisAngle.IDENTITY));
      mech.addRigidBody(rbox1);

      coords = new Coordinate[2];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue(0);
      coords[0].setLocked(false);
      coords[0].setName ("1");
      coords[0].setRange (new DoubleInterval(-0.5, 0.5));
      coords[0].setClamped (true);

      coords[1] = new Coordinate();
      coords[1].setDefaultValue(0);
      coords[1].setLocked(false);
      coords[1].setName ("2");
      coords[1].setRange (new DoubleInterval(-0.5, 0.5));
      coords[1].setClamped(false);


      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setFunction (new Constant(0));

      axes[1] = new TransformAxis();
      axes[1].setAxis (new Vector3d(0, 1, 0));
      axes[1].setCoordinates (new String[]{"2"});
      axes[1].setFunction (new LinearFunction(new double[]{1, 0}));

      axes[2] = new TransformAxis();
      axes[2].setAxis (new Vector3d(1, 0, 0));
      axes[2].setFunction (new Constant(0));

      axes[3] = new TransformAxis();
      axes[3].setAxis (new Vector3d(0, 0, 1));
      axes[3].setFunction (new Constant(0));

      axes[4] = new TransformAxis();
      axes[4].setAxis (new Vector3d(0, 1, 0));
      axes[4].setFunction (new Constant(0));

      axes[5] = new TransformAxis();
      axes[5].setAxis (new Vector3d(1, 0, 0));
      axes[5].setCoordinates (new String[]{"1"});
      axes[5].setFunction (new LinearFunction(new double[]{1,0}));

      OpenSimCustomJoint customJoint =
      new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSim Rotating-Y, Sliding-X Joint");
      customJoint.setBodies(
         rbox1, new RigidTransform3d(-1, 0, 0), null, new RigidTransform3d(offset, AxisAngle.IDENTITY));
      customJoint.setAxisLength(0.2);
      mech.addBodyConnector(customJoint);

      rbox1.transformPose(new RigidTransform3d(offset, AxisAngle.IDENTITY));
   }

   protected void addSlidingZYXJoint(Vector3d offset) {

      PolygonalMesh boxMesh1 = MeshFactory.createBox(1, 0.2, 0.2);
      RigidBody rbox1 = new RigidBody("tzyzbox1");
      rbox1.addMesh(boxMesh1);
      rbox1.setPose(new RigidTransform3d(new Vector3d(1,0,0), AxisAngle.IDENTITY));
      mech.addRigidBody(rbox1);

      coords = new Coordinate[3];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue(0);
      coords[0].setLocked(false);
      coords[0].setName ("1");
      coords[0].setRange (new DoubleInterval(-1, 1));
      coords[0].setClamped (true);


      coords[1] = new Coordinate();
      coords[1].setDefaultValue(0);
      coords[1].setLocked(false);
      coords[1].setName ("2");
      coords[1].setRange (new DoubleInterval(-0.5, 0.5));
      coords[1].setClamped (true);


      coords[2] = new Coordinate();
      coords[2].setDefaultValue(0);
      coords[2].setLocked(false);
      coords[2].setName ("3");
      coords[2].setRange (new DoubleInterval(-0.3, 0.3));
      coords[2].setClamped (true);


      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setFunction (new Constant(0));

      axes[1] = new TransformAxis();
      axes[1].setAxis (new Vector3d(0, 1, 0));
      axes[1].setFunction (new Constant(0));

      axes[2] = new TransformAxis();
      axes[2].setAxis (new Vector3d(1, 0, 0));
      axes[2].setFunction (new Constant(0));

      axes[3] = new TransformAxis();
      axes[3].setAxis (new Vector3d(0, 0, 1));
      axes[3].setCoordinates (new String[]{"1"});
      axes[3].setFunction (new LinearFunction(new double[]{1,0}));

      axes[4] = new TransformAxis();
      axes[4].setAxis (new Vector3d(0, 1, 0));
      axes[4].setCoordinates (new String[]{"2"});
      axes[4].setFunction (new LinearFunction(new double[]{1,0}));

      axes[5] = new TransformAxis();
      axes[5].setAxis (new Vector3d(1, 0, 0));
      axes[5].setCoordinates (new String[]{"3"});
      axes[5].setFunction (new LinearFunction(new double[]{1,0}));

      OpenSimCustomJoint customJoint =
      new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSim Sliding-ZYX Joint");
      customJoint.setBodies(
         rbox1, new RigidTransform3d(-1, 0, 0), null, new RigidTransform3d(offset, AxisAngle.IDENTITY));
      customJoint.setAxisLength(0.2);
      mech.addBodyConnector(customJoint);

      rbox1.transformPose(new RigidTransform3d(offset, AxisAngle.IDENTITY));
   }

   protected void add6DOFJoint(Vector3d offset) {

      PolygonalMesh boxMesh1 = MeshFactory.createBox(1, 0.2, 0.2);
      RigidBody rbox1 = new RigidBody("dof6box1");
      rbox1.addMesh(boxMesh1);
      rbox1.setPose(new RigidTransform3d(new Vector3d(1,0,0), AxisAngle.IDENTITY));
      mech.addRigidBody(rbox1);

      coords = new Coordinate[6];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue(0);
      coords[0].setLocked(false);
      coords[0].setName ("1");
      coords[0].setRange (new DoubleInterval(-0.2, 0.2));
      coords[0].setClamped (true);


      coords[1] = new Coordinate();
      coords[1].setDefaultValue(0);
      coords[1].setLocked(false);
      coords[1].setName ("2");
      coords[1].setRange (new DoubleInterval(0, 0.2));
      coords[1].setClamped (true);


      coords[2] = new Coordinate();
      coords[2].setDefaultValue(0);
      coords[2].setLocked(false);
      coords[2].setName ("3");
      coords[2].setRange (new DoubleInterval(0, 0.2));
      coords[2].setClamped (true);


      coords[3] = new Coordinate();
      coords[3].setDefaultValue(0);
      coords[3].setLocked(false);
      coords[3].setName ("4");
      coords[3].setRange (new DoubleInterval(-1, 1));
      coords[3].setClamped (true);


      coords[4] = new Coordinate();
      coords[4].setDefaultValue(0);
      coords[4].setLocked(false);
      coords[4].setName ("5");
      coords[4].setRange (new DoubleInterval(-0.5, 0.5));
      coords[4].setClamped (true);


      coords[5] = new Coordinate();
      coords[5].setDefaultValue(0);
      coords[5].setLocked(false);
      coords[5].setName ("6");
      coords[5].setRange (new DoubleInterval(-0.3, 0.3));
      coords[5].setClamped (true);


      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setCoordinates (new String[]{"4"});
      axes[0].setFunction (new LinearFunction(new double[]{1,0}));

      axes[1] = new TransformAxis();
      axes[1].setAxis (new Vector3d(0, 1, 0));
      axes[1].setCoordinates (new String[]{"5"});
      axes[1].setFunction (new LinearFunction(new double[]{1,0}));

      axes[2] = new TransformAxis();
      axes[2].setAxis (new Vector3d(1, 0, 0));
      axes[2].setCoordinates (new String[]{"6"});
      axes[2].setFunction (new LinearFunction(new double[]{1,0}));

      axes[3] = new TransformAxis();
      axes[3].setAxis (new Vector3d(0, 0, 1));
      axes[3].setCoordinates (new String[]{"1"});
      axes[3].setFunction (new LinearFunction(new double[]{1,0}));

      axes[4] = new TransformAxis();
      axes[4].setAxis (new Vector3d(0, 1, 0));
      axes[4].setCoordinates (new String[]{"2"});
      axes[4].setFunction (new LinearFunction(new double[]{1,0}));

      axes[5] = new TransformAxis();
      axes[5].setAxis (new Vector3d(1, 0, 0));
      axes[5].setCoordinates (new String[]{"3"});
      axes[5].setFunction (new LinearFunction(new double[]{1,0}));


      OpenSimCustomJoint customJoint =
      new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSim 6DOF Joint");
      customJoint.setBodies(
         rbox1, new RigidTransform3d(-1, 0, 0), null, new RigidTransform3d(offset, AxisAngle.IDENTITY));
      customJoint.setAxisLength(0.2);
      mech.addBodyConnector(customJoint);

      rbox1.transformPose(new RigidTransform3d(offset, AxisAngle.IDENTITY));
   }

}
