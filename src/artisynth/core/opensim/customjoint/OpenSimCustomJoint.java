package artisynth.core.opensim.customjoint;

import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.mechmodels.JointBase;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.opensim.components.Coordinate;
import artisynth.core.opensim.components.CoordinateSet;
import artisynth.core.opensim.components.CustomJoint;
import artisynth.core.opensim.components.SpatialTransform;
import artisynth.core.opensim.components.TransformAxis;
import artisynth.core.opensim.customjoint.OpenSimCustomCoupling.TAxis;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;

public class OpenSimCustomJoint extends JointBase {

   //   private static DoubleInterval DEFAULT_ANGLE_RANGE = new DoubleInterval(
   //      "[-inf,inf])");

   // not static, so I can built it with custom properties
   public static PropertyList myProps = new PropertyList(
      OpenSimCustomJoint.class, JointBase.class);

   //private void initializePropertyList() {
   static {
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      setRenderProps(defaultRenderProps(null));
   }
   
   public OpenSimCustomJoint(CustomJoint cj) {
      this(cj.getSpatialTransform (), cj.getCoordinateArray ());
   }
   
   public OpenSimCustomJoint(SpatialTransform trans, CoordinateSet coords) {
      this(trans.getTransformAxisArray (), 
         coords.objects ().toArray (new Coordinate[coords.size ()]));
   }
   
   public OpenSimCustomJoint(SpatialTransform trans, ArrayList<Coordinate> coords) {
      this(trans.getTransformAxisArray (), 
         coords.toArray (new Coordinate[coords.size ()]));
   }
   
   public OpenSimCustomJoint(TransformAxis[] axes, Coordinate[] coords) {
      setDefaultValues();
      setCoupling (new OpenSimCustomCoupling(axes, coords));
      // initializePropertyList();
   }
   
   public OpenSimCustomJoint (RigidBody bodyA, RigidTransform3d XFA,
      RigidTransform3d XDW, TransformAxis[] axes, Coordinate[] coords) {
      this(axes, coords);
      setBodies(bodyA, XFA, null, XDW);
   }

   public OpenSimCustomJoint (RigidBody bodyA, RigidTransform3d XFA,
      RigidBody bodyB, RigidTransform3d XDB, 
      TransformAxis[] axes, Coordinate[] coords) {
      this(axes, coords);
      setBodies(bodyA, XFA, bodyB, XDB);
   }
   
   public OpenSimCustomJoint (RigidBody bodyA, RigidBody bodyB,
      RigidTransform3d XJointWorld,
      TransformAxis[] axes, Coordinate[] coords) {
      this(axes, coords);
      RigidTransform3d XFA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();

      XFA.mulInverseLeft(bodyA.getPose(), XJointWorld);
      XDB.mulInverseLeft(bodyB.getPose(), XJointWorld);

      setBodies(bodyA, XFA, bodyB, XDB);
   }

   public void updateBounds(Vector3d pmin, Vector3d pmax) {
      RigidTransform3d XFW = getCurrentTCW();
      XFW.p.updateBounds(pmin, pmax);
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps(this);
   }

   //   public void prerender(RenderList list) {
   //   RigidTransform3d XCW = getCurrentTCW();
   //   myRenderFrame.set(XCW);
   //}
   
   private void drawAxes (Renderer renderer, RenderProps props,
      RigidTransform3d X, double length, Color[] colors) {
      
      double l = length;
      renderer.pushModelMatrix();
      renderer.setModelMatrix (new RigidTransform3d());
      
      float[] carray = new float[3];
      Shading savedShading = renderer.setShading (Shading.NONE);
      
      // draw axis
      Vector3d u = new Vector3d();
      renderer.setLineWidth (props.getLineWidth());
     
      renderer.beginDraw (DrawMode.LINES);
      for (int i = 0; i < 3; i++) {
         colors[i].getRGBColorComponents(carray);
         renderer.setColor (carray[0],carray[1],carray[2]);
         renderer.addVertex (X.p.x, X.p.y, X.p.z);
         X.R.getColumn (i, u);
         renderer.addVertex (X.p.x + l * u.x, X.p.y + l * u.y, X.p.z + l * u.z);
      }
      renderer.endDraw();
      renderer.setLineWidth (1);
      renderer.setShading (savedShading);
      
      renderer.popModelMatrix();
   }
   
   public void render(Renderer renderer, int flags) {
      super.render (renderer, flags);
      float[] coords =
         new float[] { (float)myRenderFrameD.p.x, (float)myRenderFrameD.p.y,
                      (float)myRenderFrameD.p.z };
      renderer.drawPoint(myRenderProps, coords, isSelected());
      
      OpenSimCustomCoupling coupling = (OpenSimCustomCoupling)myCoupling;
      TAxis[] transAxes = coupling.transAxes;

      // draw translation axes
      Color[] transColors = {Color.MAGENTA, Color.MAGENTA, Color.MAGENTA};
      RigidTransform3d XDW = getCurrentTDW();
      RigidTransform3d X = new RigidTransform3d();
      for (int i=0; i<3; i++) { 
         X.R.setColumn(i, transAxes[i].getAxis ());
      }
      X.mul(XDW, X);
      drawAxes(renderer, myRenderProps, X, myAxisLength, transColors);
      
      X.p.setZero ();
      TAxis[] rotAxes = coupling.rotAxes;
      Color[] rotColors = {Color.CYAN, Color.CYAN, Color.CYAN};
      for (int i=0; i<3; i++) { 
         X.R.setColumn(i, rotAxes[i].getAxis ());
      }
      X.mul(XDW, X);
      drawAxes(renderer, myRenderProps, X, myAxisLength, rotColors);
      
   }

}
