package artisynth.core.opensim.customjoint;

import java.io.IOException;
import java.awt.Color;

import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidBody.InertiaMethod;
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
import maspack.matrix.*;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.AxisDrawStyle;
import maspack.util.DoubleInterval;

public class OpenSimCustomJointTest extends RootModel {

   private static final double RTOD = 180/Math.PI;
   private static final double DTOR = Math.PI/180;

   MechModel mech;
   Coordinate[] coords;
   TransformAxis[] axes;

   public static class JointMonitor extends MonitorBase {
      BodyConnector joint;
      boolean enabled = true;

      static PropertyList myProps =
         new PropertyList(JointMonitor.class, MonitorBase.class);

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

      //addHingeJoint (/*custom=*/true);

      //addSliderJoint (/*custom=*/true);

      //addCylindricalJoint (/*custom=*/true);

      //addSlottedHingeJoint (/*custom=*/true);

      //addPlanarJoint (/*custom=*/true);

      //addUniversalJoint (/*skewAngle=*/DTOR*30, /*custom=*/true);

      //addGimbalJoint (/*custom=*/true);

      addCoupledHingeJoint();

//      addRollPitchSlideJoint(new Vector3d(0,3.0,0));
//      addSlidingZYXJoint(new Vector3d(0,3.5,0));
//    addRotYSlidingXJoint(new Vector3d(0,4.5,0));
      //add6DOFJoint(new Vector3d(0,5,0));

      mech.setGravity(0, 0, -9.8);
   }

   HingeJoint createHingeJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW, double size) {

      HingeJoint joint = new HingeJoint (bodyA, bodyB, TDW);
      // set the range for theta (in degrees)
      joint.setMaxTheta (135.0);
      joint.setMinTheta (-135.0);
      joint.setShaftLength (0.5*size); // draw shaft
      joint.setShaftRadius (0.05*size);
      return joint;
   }

   OpenSimCustomJoint createCustomHingeJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {

      coords = new Coordinate[1];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue (0);
      coords[0].setLocked (false);
      coords[0].setName("theta");
      coords[0].setRange (-DTOR*135, DTOR*135);
      coords[0].setClamped (true);

      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setCoordinates (new String[] {"theta"});
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
      customJoint.setName("OpenSimCustomHinge");
      customJoint.setBodies(bodyA, bodyB, TDW);
      return customJoint;
   }

   void addHingeJoint (boolean custom) {
      mech.setFrameDamping (50.0);
      mech.setRotaryDamping (1.0);

      double size = 1.0; // size parameter

      // create base - a rounded box flat at the bottom
      PolygonalMesh mesh = MeshFactory.createRoundedBox (
         size, size/2, size/4, 2, 1, 1, 12, /*flatBottom=*/true);
      RigidBody base = RigidBody.createFromMesh (
         "base", mesh, /*density=*/1000.0, 1.0);
      base.setDynamic (false);
      mech.addRigidBody (base);

      // create tip - a box rounded at both ends
      mesh = MeshFactory.createRoundedBox (
         size, size/3, size/3, 2, 1, 1, 12);
      RigidBody tip = RigidBody.createFromMesh (
         "tip", mesh, /*density=*/1000.0, 1.0);
      tip.setPose (new RigidTransform3d (0, 0, size));
      mech.addRigidBody (tip);

      // Add a hinge joint between the tip and base, with frames C and D
      // initially coincident. Frame D is set (in world coordinates) with its
      // origin at (0, 0, size/2) and its z axis pointing along negative
      // (world) y.
      RigidTransform3d TDW =
         new RigidTransform3d (0, 0, size/2, 0, 0, Math.PI/2);
      JointBase joint;
      if (custom) {
         joint = createCustomHingeJoint (tip, base, TDW);
      }
      else {
         joint = createHingeJoint (tip, base, TDW, size);
      }
      mech.addBodyConnector (joint);      
      // set theta to -45 degrees so the tip will fall under gravity
      joint.setCoordinate (0, -DTOR*45);

      // set rendering properties
      joint.setAxisLength (0.6*size); // draw C frame
      joint.setDrawFrameC (AxisDrawStyle.ARROW);
      RenderProps.setFaceColor (joint, Color.BLUE); // set colors
      RenderProps.setFaceColor (tip, new Color (0.5f, 1f, 1f));
   }

   SliderJoint createSliderJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW, double size) {

      SliderJoint joint = new SliderJoint (bodyA, bodyB, TDW);
      joint.setMaxZ (0.4*size);
      joint.setMinZ (-0.4*size);
      return joint;
   }

   OpenSimCustomJoint createCustomSliderJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW, double size) {

      coords = new Coordinate[1];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue (0);
      coords[0].setLocked (false);
      coords[0].setName("z");
      coords[0].setRange (-0.4*size, 0.4*size);
      coords[0].setClamped (true);

      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setFunction(new Constant(0));

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
      axes[5].setCoordinates (new String[] {"z"});
      axes[5].setFunction (new LinearFunction(new double[] {1, 0}));

      OpenSimCustomJoint customJoint = new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSimCustomSlider");
      customJoint.setBodies (bodyA, bodyB, TDW) ;
      return customJoint;
   }

   protected void addSliderJoint (boolean custom) {

      mech.setFrameDamping (50.0);
      mech.setRotaryDamping (1.0);

      double size = 1.0; // size parameter
      double tilt = Math.toRadians(10); // tilt angle for the slider

      // create base - a vertical cylinder
      RigidBody base = RigidBody.createCylinder (
         "base", 0.125*size, size, /*density=*/1000.0, /*nsides=*/25);
      base.setDynamic (false);
      // create sleave - a horizontal tube
      PolygonalMesh sleaveMesh = MeshFactory.createTube (
         0.1*size, 0.2*size, 0.25*size, 25, 1, 1);
      // transform tube to so it lies on top of the cylinder at the angle
      // specified by 'tilt'
      RigidTransform3d TSW =
         new RigidTransform3d (0, 0, 0.65*size, 0, Math.PI/2-tilt, 0);
      sleaveMesh.transform (TSW);
      base.addMesh (sleaveMesh);
      mech.addRigidBody (base);

      // create slider - a cylinder
      RigidBody slider = RigidBody.createCylinder (
         "slider", 0.1*size, size, /*density=*/1000.0, /*nsides=*/25);
      slider.setPose (TSW);
      mech.addRigidBody (slider);

      // Add a slider joint between the slider and the base, with frames C
      // and D coincident. D is set to the same as the slider.
      JointBase joint;
      if (custom) {
         joint = createCustomSliderJoint (slider, base, TSW, size);
      }
      else {
         joint = createSliderJoint (slider, base, TSW, size);
      }
      mech.addBodyConnector (joint);      

      // set rendering properties
      joint.setAxisLength (0.75*size); // draw C frame
      joint.setDrawFrameC (AxisDrawStyle.ARROW);
      RenderProps.setFaceColor (joint, Color.BLUE); // set colors
      RenderProps.setFaceColor (slider, new Color (0.5f, 1f, 1f));
   }

   CylindricalJoint createCylindricalJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {

      CylindricalJoint joint = new CylindricalJoint (bodyA, bodyB, TDW);
      // set coordinate ranges, with max z keeping the slider flange from
      // penetrating the sleeve
      joint.setMaxTheta (135.0);
      joint.setMinTheta (-135.0);
      joint.setMaxZ (0.325);
      joint.setMinZ (-0.4);
      // set theta to 45 degrees so the slider will rotate under gravity
      joint.setTheta (45);
      return joint;
   }

   OpenSimCustomJoint createCustomCylindricalJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {

      coords = new Coordinate[2];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue (0);
      coords[0].setLocked (false);
      coords[0].setName("theta");
      coords[0].setRange (-DTOR*135, DTOR*135);
      coords[0].setClamped (true);

      coords[1] = new Coordinate();
      coords[1].setDefaultValue (0);
      coords[1].setLocked (false);
      coords[1].setName("z");
      coords[1].setRange (-0.4, 0.325);
      coords[1].setClamped (true);

      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setCoordinates (new String[] {"theta"});
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
      axes[5].setCoordinates (new String[] {"z"});
      axes[5].setFunction (new LinearFunction(new double[] {1, 0}));

      OpenSimCustomJoint customJoint = new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSimCustomCylindrical");
      customJoint.setBodies (bodyA, bodyB, TDW) ;
      customJoint.setCoordinate (0, DTOR*45);
      return customJoint;
   }

   protected void addCylindricalJoint (boolean custom) {

      mech.setFrameDamping (50.0);
      mech.setRotaryDamping (1.0);

      double tilt = -Math.toRadians(10); // tilt angle for the slider

      // create base - a vertical cylinder
      RigidBody base = RigidBody.createCylinder (
         "base", 0.125, 1.0, /*density=*/1000.0, /*nsides=*/25);
      base.setDynamic (false);
      // create sleave - a horizontal tube
      PolygonalMesh sleaveMesh = MeshFactory.createTube (
         0.1, 0.2, 0.25, 25, 1, 1);
      // transform tube to so it lies on top of the cylinder at the angle
      // specified by 'tilt'
      RigidTransform3d TSW =
         new RigidTransform3d (0, 0, 0.65, 0, Math.PI/2-tilt, 0);
      sleaveMesh.transform (TSW);
      base.addMesh (sleaveMesh);
      mech.addRigidBody (base);

      // create slider - a cylinder with a flange attached
      RigidBody slider = RigidBody.createCylinder (
         "slider", 0.1, 0.9, /*density=*/1000.0, /*nsides=*/25);
      // createCylinder uses an EXPLICT inertia method. Set to DENSITY so that
      // the inertai will change when we add the flange mesh.
      slider.setInertiaMethod (InertiaMethod.DENSITY);
      PolygonalMesh flangeMesh = MeshFactory.createRoundedBox (
         0.5, 0.2, 0.1, 2, 1, 1, 12);
      flangeMesh.transform (
         new RigidTransform3d (
            -0.25, 0, -0.5, Math.PI/2, 0, Math.PI/2));
      slider.addMesh (flangeMesh, /*hasMass=*/true, /*collidable=*/false);
      slider.setPose (TSW);
      mech.addRigidBody (slider);

      JointBase joint;
      if (custom) {
         joint = createCustomCylindricalJoint (slider, base, TSW);
      }
      else {
         joint = createCylindricalJoint (slider, base, TSW);
      }
      mech.addBodyConnector (joint);

      // set rendering properties
      joint.setAxisLength (0.75); // draw C frame
      joint.setDrawFrameC (AxisDrawStyle.ARROW);
      RenderProps.setFaceColor (joint, Color.BLUE); // set colors
      RenderProps.setFaceColor (slider, new Color (0.5f, 1f, 1f));
   }

   protected void addCustomSliderJoint () {

      mech.setFrameDamping (50.0);
      mech.setRotaryDamping (1.0);

      PolygonalMesh boxMesh1 = MeshFactory.createBox(1, 0.2, 0.2);
      RigidBody rbox1 = new RigidBody("crbox1");
      rbox1.addMesh(boxMesh1);
      rbox1.setPose (new RigidTransform3d(0, 0, 0, 0, DTOR*30, 0));
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
      axes[0].setFunction(new Constant(0));

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
      axes[5].setFunction (new LinearFunction(new double[] {1, 0}));

      OpenSimCustomJoint customJoint = new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSimCustomSlider");
      customJoint.setBodies(
         rbox1, new RigidTransform3d(0, 0, 0, 0, Math.PI/2, 0),
         null, new RigidTransform3d(0, 0, 0, 0, DTOR*120, 0));
      customJoint.setAxisLength(0.4);
      customJoint.setDrawFrameD (AxisDrawStyle.ARROW);
      customJoint.setDrawFrameC (AxisDrawStyle.ARROW);
      mech.addBodyConnector(customJoint);
   }

   OpenSimCustomJoint createCustomSlottedHinge (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {

      coords = new Coordinate[2];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue (0);
      coords[0].setLocked (false);
      coords[0].setName("x");
      coords[0].setRange (-0.5, 0.5);
      coords[0].setClamped (true);

      coords[1] = new Coordinate();
      coords[1].setDefaultValue (0);
      coords[1].setLocked (false);
      coords[1].setName("theta");
      coords[1].setRange (-DTOR*135, DTOR*135.0);
      coords[1].setClamped (true);

      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setCoordinates (new String[] {"theta"});
      axes[0].setFunction (new LinearFunction(new double[] {1, 0}));

      axes[1] = new TransformAxis();
      axes[1].setAxis (new Vector3d(0, 1, 0));
      axes[1].setFunction(new Constant(0));

      axes[2] = new TransformAxis();
      axes[2].setAxis (new Vector3d(1, 0, 0));
      axes[2].setFunction(new Constant(0));

      axes[3] = new TransformAxis();
      axes[3].setAxis (new Vector3d(1, 0, 0));
      axes[3].setCoordinates (new String[] {"x"});
      axes[3].setFunction (new LinearFunction(new double[] {1, 0}));
      
      axes[4] = new TransformAxis();
      axes[4].setAxis (new Vector3d(0, 1, 0));
      axes[4].setFunction (new Constant(0));

      axes[5] = new TransformAxis();
      axes[5].setAxis (new Vector3d(0, 0, 1));
      axes[5].setFunction (new Constant(0));

      OpenSimCustomJoint customJoint = new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSimCustomSlottedHinge");
      customJoint.setBodies (bodyA, bodyB, TDW) ;
      return customJoint;
   }

   SlottedHingeJoint createSlottedHinge (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {

      SlottedHingeJoint joint = new SlottedHingeJoint (bodyA, bodyB, TDW);
      joint.setMaxX (1.0/2);
      joint.setMinX (-1.0/2);
      joint.setMaxTheta (135.0);
      joint.setMinTheta (-135.0);
      joint.setShaftLength (0.5*1.0); // draw shaft
      joint.setShaftRadius (0.05*1.0);
      joint.setSlotDepth (1.10*1.0/4); // draw slot
      joint.setSlotWidth (0.1*1.0);
      return joint;
   }

   protected void addSlottedHingeJoint (boolean custom) {
      mech.setFrameDamping (50.0);
      mech.setRotaryDamping (1.0);

      // create base - a rounded box
      PolygonalMesh mesh = MeshFactory.createRoundedBox (
         1.0, 1.0/2, 1.0/4, 12);
      mesh.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));
      RigidBody base = RigidBody.createFromMesh (
         "base", mesh, /*density=*/1000.0, 1.0);
      base.setDynamic (false);
      mech.addRigidBody (base);

      // create slider - another rounded box
      mesh = MeshFactory.createRoundedBox (
         1.0, 1.0/3, 1.0/3, 2, 1, 1, 12);
      RigidBody slider = RigidBody.createFromMesh (
         "slider", mesh, /*density=*/1000.0, 1.0);
      slider.setPose (new RigidTransform3d (0, 0, 1.0/2));
      mech.addRigidBody (slider);

      // Add a slotted revolute joint between the slider and base, with frames
      // C and D initially coincident. Frame D is set (in world coordinates)
      // with its origin at (0, 0, 0) and its z axis pointing along negative
      // world y.
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.mulRotX (Math.PI/2);
      JointBase joint;
      if (custom) {
         joint = createCustomSlottedHinge (slider, base, TDW);
      }
      else {
         joint = createSlottedHinge (slider, base, TDW);
      }
      mech.addBodyConnector (joint);
      joint.setCoordinate (1, -DTOR*45);
      joint.setAxisLength (0.6*1.0); // draw C frame
      joint.setDrawFrameC (AxisDrawStyle.ARROW);
      RenderProps.setFaceColor (joint, Color.BLUE); // set colors
      RenderProps.setFaceColor (slider, new Color (0.5f, 1f, 1f));
   }

   OpenSimCustomJoint createCustomPlanarJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {

      coords = new Coordinate[3];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue (0);
      coords[0].setLocked (false);
      coords[0].setName("x");
      coords[0].setRange (-0.5, 0.5);
      coords[0].setClamped (true);

      coords[1] = new Coordinate();
      coords[1].setDefaultValue (0);
      coords[1].setLocked (false);
      coords[1].setName("y");
      coords[1].setRange (-0.5, 0.5);
      coords[1].setClamped (true);

      coords[2] = new Coordinate();
      coords[2].setDefaultValue (0);
      coords[2].setLocked (false);
      coords[2].setName("theta");
      coords[2].setRange (-DTOR*90, DTOR*90);
      coords[2].setClamped (true);

      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setCoordinates (new String[] {"theta"});
      axes[0].setFunction (new LinearFunction(new double[] {1, 0}));

      axes[1] = new TransformAxis();
      axes[1].setAxis (new Vector3d(0, 1, 0));
      axes[1].setFunction(new Constant(0));

      axes[2] = new TransformAxis();
      axes[2].setAxis (new Vector3d(1, 0, 0));
      axes[2].setFunction(new Constant(0));

      axes[3] = new TransformAxis();
      axes[3].setAxis (new Vector3d(1, 0, 0));
      axes[3].setCoordinates (new String[] {"x"});
      axes[3].setFunction (new LinearFunction(new double[] {1, 0}));
      
      axes[4] = new TransformAxis();
      axes[4].setAxis (new Vector3d(0, 1, 0));
      axes[4].setCoordinates (new String[] {"y"});
      axes[4].setFunction (new LinearFunction(new double[] {1, 0}));

      axes[5] = new TransformAxis();
      axes[5].setAxis (new Vector3d(0, 0, 1));
      axes[5].setFunction (new Constant(0));

      OpenSimCustomJoint customJoint = new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSimCustomPlanar");
      customJoint.setBodies (bodyA, bodyB, TDW) ;
      return customJoint;
   }

   PlanarJoint createPlanarJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {

      PlanarJoint joint = new PlanarJoint (
         bodyA, bodyB, TDW);

      joint.setMaxX (0.5);
      joint.setMinX (-0.5);
      joint.setMaxY (0.5);
      joint.setMinY (-0.5);
      joint.setMinTheta (-90);
      joint.setMaxTheta (90);
      joint.setShaftLength (0.4); // draw the shaft
      joint.setShaftRadius (0.02);
      return joint;
   }

   protected void addPlanarJoint (boolean custom) {
      mech.setFrameDamping (0.10);
      mech.setRotaryDamping (0.001);

      // create base plane - a flat disk
      RigidBody base = RigidBody.createCylinder (
         "base", 1.0, 0.125, /*density=*/1000.0, /*nsides=*/100);
      base.setDynamic (false);
      mech.addRigidBody (base);

      // create box to slide on the plane
      RigidBody box = RigidBody.createBox (
         "box", 0.5, 0.5, 0.125, /*density=*/1000.0);
      box.setPose (new RigidTransform3d (0, 0, 0.125));
      mech.addRigidBody (box);

      RigidTransform3d TDW = new RigidTransform3d (0, 0, 0.125);
      JointBase joint;
      if (custom) {
         joint = createCustomPlanarJoint (box, base, TDW);
      }
      else {
         joint = createPlanarJoint (box, base, TDW);
      }
      mech.addBodyConnector (joint);      

      // tilt the entire model by 45 degrees about the x axis, so the box will
      // slide under gravity
      mech.transformGeometry (
         new RigidTransform3d (0, 0, 0, 0, 0, Math.PI/4));

      // set rendering properties
      joint.setAxisLength (0.5); // draw frame C
      joint.setDrawFrameC (AxisDrawStyle.ARROW);
      RenderProps.setFaceColor (joint, Color.BLUE); // set colors
      RenderProps.setFaceColor (box, new Color (0.5f, 1f, 1f));
   }

   OpenSimCustomJoint createCustomUniversalJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW, double skewAngle) {

      coords = new Coordinate[2];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue (0);
      coords[0].setLocked (false);
      coords[0].setName("roll");
      coords[0].setRange (-DTOR*90, DTOR*90);
      coords[0].setClamped (true);

      coords[1] = new Coordinate();
      coords[1].setDefaultValue (0);
      coords[1].setLocked (false);
      coords[1].setName("pitch");
      coords[1].setRange (-DTOR*100, DTOR*100);
      coords[1].setClamped (true);

      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setCoordinates (new String[] {"roll"});
      axes[0].setFunction (new LinearFunction(new double[] {1, 0}));

      double ca = Math.cos(skewAngle);
      double sa = Math.sin(skewAngle);

      axes[1] = new TransformAxis();
      axes[1].setAxis (new Vector3d(0, ca, sa));
      axes[1].setCoordinates (new String[] {"pitch"});
      axes[1].setFunction (new LinearFunction(new double[] {1, 0}));

      axes[2] = new TransformAxis();
      axes[2].setAxis (new Vector3d(1, 0, 0));
      axes[2].setFunction(new Constant(0));

      axes[3] = new TransformAxis();
      axes[3].setAxis (new Vector3d(1, 0, 0));
      axes[3].setFunction(new Constant(0));
      
      axes[4] = new TransformAxis();
      axes[4].setAxis (new Vector3d(0, 1, 0));
      axes[4].setFunction(new Constant(0));

      axes[5] = new TransformAxis();
      axes[5].setAxis (new Vector3d(0, 0, 1));
      axes[5].setFunction (new Constant(0));

      OpenSimCustomJoint customJoint = new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSimCustomUniversal");
      customJoint.setBodies (bodyA, bodyB, TDW) ;
      return customJoint;
   }

   UniversalJoint createUniversalJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW,
      double size, double skewAngle) {

      UniversalJoint joint;
      if (skewAngle > 0) {
         joint = new SkewedUniversalJoint (bodyA, bodyB, TDW, skewAngle);
      }
      else {
         joint = new UniversalJoint (bodyA, bodyB, TDW);
      }
      joint.setRollRange (-90, 90);
      joint.setPitchRange (-100, 100);
      joint.setShaftLength (0.9*size); // draw rotational axis
      joint.setShaftRadius (0.05*size);
      return joint;
   }

   protected void addUniversalJoint (double skewAngle, boolean custom) {
      mech.setFrameDamping (0.10);
      mech.setRotaryDamping (0.001);

      double size = 0.5; // size parameter

      // create base - a cylinder rounded at the top
      PolygonalMesh mesh = MeshFactory.createRoundedCylinder (
         size/4, size, /*nslices=*/30, /*negs=*/1, /*flatBottom=*/true);
      RigidBody base = RigidBody.createFromMesh (
         "base", mesh, /*density=*/1000.0, /*scale=*/1.0);
      base.setDynamic (false);
      mech.addRigidBody (base);

      // create tip - a cylinder rounded at both ends
      mesh = MeshFactory.createRoundedCylinder (
         size/6, size, /*nslices=*/30, /*negs=*/1, /*flatBottom=*/false);
      RigidBody tip = RigidBody.createFromMesh (
         "tip", mesh, /*density=*/1000.0, /*scale=*/1.0);
      tip.setPose (new RigidTransform3d (0, 0, 1.45*size));
      mech.addRigidBody (tip);

      // Add a gimbal joint between the tip and the base, with frames C and D
      // initially coincident. A SkewedUniversalJoint is used if the skew angle
      // != 0. Frame D is set (in world coordinates) with its origin at (0, 0,
      // 0.75*size) and its y axis lying along negative world y.
      RigidTransform3d TDW =
         new RigidTransform3d (0, 0, 0.75*size, 0, 0, Math.PI);
      JointBase joint;
      if (custom) {
         joint = createCustomUniversalJoint (tip, base, TDW, skewAngle);
      }
      else {
         joint = createUniversalJoint (tip, base, TDW, size, skewAngle);
      }
      joint.setCoordinate (1, -DTOR*45);
      mech.addBodyConnector (joint);

      // set rendering properties
      joint.setAxisLength (0.75*size); // draw C frame
      joint.setDrawFrameC (AxisDrawStyle.ARROW);
      RenderProps.setFaceColor (joint, Color.BLUE); // set colors
      RenderProps.setFaceColor (tip, new Color (0.5f, 1f, 1f));
   }

   OpenSimCustomJoint createCustomGimbalJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {

      coords = new Coordinate[3];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue (0);
      coords[0].setLocked (false);
      coords[0].setName("roll");
      coords[0].setRange (-DTOR*90, DTOR*90);
      coords[0].setClamped (true);

      coords[1] = new Coordinate();
      coords[1].setDefaultValue (0);
      coords[1].setLocked (false);
      coords[1].setName("pitch");
      coords[1].setRange (-DTOR*45, DTOR*45);
      coords[1].setClamped (true);

      coords[2] = new Coordinate();
      coords[2].setDefaultValue (0);
      coords[2].setLocked (false);
      coords[2].setName("yaw");
      coords[2].setRange (-DTOR*90, DTOR*90);
      coords[2].setClamped (true);

      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setCoordinates (new String[] {"roll"});
      axes[0].setFunction (new LinearFunction(new double[] {1, 0}));

      axes[1] = new TransformAxis();
      axes[1].setAxis (new Vector3d(0, 1, 0));
      axes[1].setCoordinates (new String[] {"pitch"});
      axes[1].setFunction (new LinearFunction(new double[] {1, 0}));

      axes[2] = new TransformAxis();
      axes[2].setAxis (new Vector3d(1, 0, 0));
      axes[2].setCoordinates (new String[] {"yaw"});
      axes[2].setFunction (new LinearFunction(new double[] {1, 0}));

      axes[3] = new TransformAxis();
      axes[3].setAxis (new Vector3d(1, 0, 0));
      axes[3].setFunction(new Constant(0));
      
      axes[4] = new TransformAxis();
      axes[4].setAxis (new Vector3d(0, 1, 0));
      axes[4].setFunction(new Constant(0));

      axes[5] = new TransformAxis();
      axes[5].setAxis (new Vector3d(0, 0, 1));
      axes[5].setFunction (new Constant(0));

      OpenSimCustomJoint customJoint = new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSimCustomGimbal");
      customJoint.setBodies (bodyA, bodyB, TDW) ;
      return customJoint;
   }

   GimbalJoint createGimbalJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW, double size) {

      GimbalJoint joint = new GimbalJoint (bodyA, bodyB, TDW);
      // set coordinate ranges (in degrees)
      joint.setRollRange (-90, 90);
      joint.setPitchRange (-45, 45);
      joint.setYawRange (-90, 90);
      joint.setJointRadius (0.10*size); // draw ball around the joint
      return joint;
   }

   void addGimbalJoint (boolean custom) {
      mech.setFrameDamping (1.0);
      mech.setRotaryDamping (0.1);      

      double size = 0.5; // size parameter

      // create base - a cylinder rounded at the top
      PolygonalMesh mesh = MeshFactory.createRoundedCylinder (
         size/5, size, /*nslices=*/30, /*negs=*/1, /*flatBottom=*/true);
      RigidBody base = RigidBody.createFromMesh (
         "base", mesh, /*density=*/1000.0, /*scale=*/1.0);
      base.setDynamic (false);
      mech.addRigidBody (base);

      // create tip - a flat ellipsoid
      RigidBody tip = RigidBody.createEllipsoid (
         "tip", size/10, size/3, size/2, /*density=*/1000.0, /*nslices=*/30);
      tip.setPose (new RigidTransform3d (0, 0, 1.25*size));
      mech.addRigidBody (tip);

      // Add a gimbal joint between the tip and the base, with frames C and D
      // initially coincident. Frame D is set (in world coordinates) with its
      // origin at (0, 0, 0.75*size) and its z axis lying along world y. This
      // helps accomodate the singularity at pitch = +/-PI/2.
      RigidTransform3d TDW =
         new RigidTransform3d (0, 0, 0.75*size, 0, 0, -Math.PI/2);
      JointBase joint;
      if (custom) {
         joint = createCustomGimbalJoint (tip, base, TDW);
      }
      else {
         joint = createGimbalJoint (tip, base, TDW, size);
      }
      mech.addBodyConnector (joint);
      // set joint coordinates so the tip will fall under gravity
      joint.setCoordinate (0, DTOR*30);
      joint.setCoordinate (2, DTOR*5);

      // set rendering properties
      joint.setAxisLength (0.75*size); // draw frame C
      joint.setDrawFrameC (AxisDrawStyle.ARROW);
      RenderProps.setFaceColor (joint, Color.BLUE); // set colors
      RenderProps.setFaceColor (tip, new Color (0.5f, 1f, 1f));
   }

   OpenSimCustomJoint createCoupledHinge (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {

      coords = new Coordinate[1];
      coords[0] = new Coordinate();
      coords[0].setDefaultValue (0);
      coords[0].setLocked (false);
      coords[0].setName("q");
      coords[0].setRange (-1, 1);
      coords[0].setClamped (true);

      TransformAxis[] axes = new TransformAxis[6];

      axes[0] = new TransformAxis();
      axes[0].setAxis (new Vector3d(0, 0, 1));
      axes[0].setCoordinates (new String[] {"q"});
      axes[0].setFunction (new LinearFunction(new double[] {DTOR*135, 0}));

      axes[1] = new TransformAxis();
      axes[1].setAxis (new Vector3d(0, 1, 0));
      axes[1].setFunction(new Constant(0));

      axes[2] = new TransformAxis();
      axes[2].setAxis (new Vector3d(1, 0, 0));
      axes[2].setFunction(new Constant(0));

      axes[3] = new TransformAxis();
      axes[3].setAxis (new Vector3d(1, 0, 0));
      axes[3].setCoordinates (new String[] {"q"});
      axes[3].setFunction (new LinearFunction(new double[] {0.5, 0}));
      
      axes[4] = new TransformAxis();
      axes[4].setAxis (new Vector3d(0, 1, 0));
      axes[4].setFunction (new Constant(0));

      axes[5] = new TransformAxis();
      axes[5].setAxis (new Vector3d(0, 0, 1));
      axes[5].setFunction (new Constant(0));

      OpenSimCustomJoint customJoint = new OpenSimCustomJoint(axes, coords);
      customJoint.setName("OpenSimCustomSlottedHinge");
      customJoint.setBodies (bodyA, bodyB, TDW) ;
      return customJoint;
   }

   protected void addCoupledHingeJoint () {
      mech.setFrameDamping (50.0);
      mech.setRotaryDamping (1.0);

      // create base - a rounded box
      PolygonalMesh mesh = MeshFactory.createRoundedBox (
         1.0, 1.0/2, 1.0/4, 12);
      mesh.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));
      RigidBody base = RigidBody.createFromMesh (
         "base", mesh, /*density=*/1000.0, 1.0);
      base.setDynamic (false);
      mech.addRigidBody (base);

      // create slider - another rounded box
      mesh = MeshFactory.createRoundedBox (
         1.0, 1.0/3, 1.0/3, 2, 1, 1, 12);
      RigidBody slider = RigidBody.createFromMesh (
         "slider", mesh, /*density=*/1000.0, 1.0);
      slider.setPose (new RigidTransform3d (0, 0, 1.0/2));
      mech.addRigidBody (slider);

      // Add a slotted revolute joint between the slider and base, with frames
      // C and D initially coincident. Frame D is set (in world coordinates)
      // with its origin at (0, 0, 0) and its z axis pointing along negative
      // world y.
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.mulRotX (Math.PI/2);
      JointBase joint = createCoupledHinge (slider, base, TDW);

      mech.addBodyConnector (joint);
      joint.setCoordinate (0, -0.2);
      joint.setAxisLength (0.6*1.0); // draw C frame
      joint.setDrawFrameC (AxisDrawStyle.ARROW);
      RenderProps.setFaceColor (joint, Color.BLUE); // set colors
      RenderProps.setFaceColor (slider, new Color (0.5f, 1f, 1f));
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
