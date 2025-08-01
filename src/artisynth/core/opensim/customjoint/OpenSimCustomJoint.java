package artisynth.core.opensim.customjoint;

import java.awt.Color;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;
import java.util.ArrayList;

import artisynth.core.mechmodels.JointBase;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.util.ScanToken;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.opensim.components.Coordinate;
import artisynth.core.opensim.components.CoordinateSet;
import artisynth.core.opensim.components.CustomJoint;
import artisynth.core.opensim.components.SpatialTransform;
import artisynth.core.opensim.components.TransformAxis;
import artisynth.core.opensim.components.FunctionBase;
import artisynth.core.opensim.customjoint.OpenSimCustomCoupling.TAxis;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.*;
import maspack.render.*;
import maspack.spatialmotion.*;
import maspack.spatialmotion.RigidBodyConstraint.MotionType;
import maspack.util.ReaderTokenizer;
import maspack.util.NumberFormat;

/**
 * Implements an OpenSim custom joint.
 */
public class OpenSimCustomJoint extends JointBase {

   public static AxisDrawStyle DEFAULT_DRAW_ROT_AXES = AxisDrawStyle.OFF;
   private AxisDrawStyle myDrawRotAxes = DEFAULT_DRAW_ROT_AXES;

   public static AxisDrawStyle DEFAULT_DRAW_TRANS_AXES = AxisDrawStyle.OFF;
   private AxisDrawStyle myDrawTransAxes = DEFAULT_DRAW_TRANS_AXES;

   // used to double buffer rotation axis directions for rendering:
   Vector3d[] myRenderRotAxes;

   //   private static DoubleInterval DEFAULT_ANGLE_RANGE = new DoubleInterval(
   //      "[-inf,inf])");

   // not static, so I can built it with custom properties
   public static PropertyList myProps = new PropertyList(
      OpenSimCustomJoint.class, JointBase.class);
   //public LocalPropertyList myLocalProps = new LocalPropertyList (myProps);

   static {
      myProps.remove ("shaftLength");
      myProps.remove ("shaftRadius");
      myProps.add (
         "drawRotAxes",
         "draw rotational axes for this joint", DEFAULT_DRAW_ROT_AXES);
      myProps.add (
         "drawTransAxes",
         "draw translational axes for this joint", DEFAULT_DRAW_TRANS_AXES);
   }

   LocalPropertyList myLocalProps; 

   public PropertyInfoList getAllPropertyInfo() {
      if (myLocalProps == null) {
         myLocalProps = new LocalPropertyList (myProps);
         myLocalProps.setWriteLocalProps (false);
      }
      return myLocalProps;
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      //setRenderProps(defaultRenderProps(null));
   }

   public OpenSimCustomJoint() {
   }
   
   public OpenSimCustomJoint(CustomJoint cj) {
      this(cj.getSpatialTransform (), cj.getCoordinateArray ());
   }
   
   // not used
   public OpenSimCustomJoint(SpatialTransform trans, CoordinateSet coords) {
      this(trans.getTransformAxisArray (), 
         coords.objects ().toArray (new Coordinate[coords.size ()]));
   }
   
   // not used externally
   public OpenSimCustomJoint(SpatialTransform trans, ArrayList<Coordinate> coords) {
      this(trans.getTransformAxisArray (), 
         coords.toArray (new Coordinate[coords.size ()]));
   }
   
   public OpenSimCustomJoint(TransformAxis[] axes, Coordinate[] coords) {
      setDefaultValues();
      setCoupling (new OpenSimCustomCoupling(axes, coords));
      addCoordinateProperties(coords);
   }
   
   // not used
   public OpenSimCustomJoint (
      RigidBody bodyA, RigidTransform3d XFA,
      RigidTransform3d XDW, TransformAxis[] axes, Coordinate[] coords) {
      this(axes, coords);
      setBodies(bodyA, XFA, null, XDW);
   }

   // not used
   public OpenSimCustomJoint (
      RigidBody bodyA, RigidTransform3d XFA,
      RigidBody bodyB, RigidTransform3d XDB, 
      TransformAxis[] axes, Coordinate[] coords) {
      this(axes, coords);
      setBodies(bodyA, XFA, bodyB, XDB);
   }
   
   // not used
   public OpenSimCustomJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d XJointWorld,
      TransformAxis[] axes, Coordinate[] coords) {
      this(axes, coords);
      RigidTransform3d XFA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();

      XFA.mulInverseLeft(bodyA.getPose(), XJointWorld);
      XDB.mulInverseLeft(bodyB.getPose(), XJointWorld);

      setBodies(bodyA, XFA, bodyB, XDB);
   }

   private void addCoordinateProperties (Coordinate[] coords) {
      String degMethods =
         "getCoordinateValueDeg setCoordinateDeg getCoordinateRangeDeg";
      String regMethods =
         "getCoordinateValue setCoordinate getCoordinateRange";

      for (int i=0; i<coords.length; i++) {
         Coordinate c = coords[i];
         String methods;
         if (getCoordinateMotionType(i) == MotionType.ROTARY) {
            methods = degMethods;
         }
         else {
            methods = regMethods;
         }
         myLocalProps.add (
            c.getName(), methods, i, "joint coordinate value", 0);
         setCoordinateName (i, c.getName());
      }

      String lockMethods =
         "isCoordinateLocked setCoordinateLocked";
      for (int i=0; i<coords.length; i++) {
         Coordinate c = coords[i];
         myLocalProps.add (
            c.getName() + "_locked", lockMethods, i,
            "whether or not joint is locked", false);
      }
   }

   public AxisDrawStyle getDrawRotAxes () {
      return myDrawRotAxes;
   }

   public void setDrawRotAxes (AxisDrawStyle style) {
      myDrawRotAxes = style;
   }

   public AxisDrawStyle getDrawTransAxes () {
      return myDrawTransAxes;
   }

   public void setDrawTransAxes (AxisDrawStyle style) {
      myDrawTransAxes = style;
   }

   public void updateBounds(Vector3d pmin, Vector3d pmax) {
      RigidTransform3d XFW = getCurrentTCW();
      XFW.p.updateBounds(pmin, pmax);
   }

   //   public RenderProps createRenderProps() {
   //   return defaultRenderProps(this);
   //}

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

   public void prerender (RenderList list) {
      super.prerender (list);

      // if rotation axes are being rendered, save their locations since
      // they change with the simulation
      if (myDrawRotAxes != AxisDrawStyle.OFF) {
         if (myRenderRotAxes == null) {
            myRenderRotAxes = new Vector3d[3];
            for (int i=0; i<3; i++) {
               myRenderRotAxes[i] = new Vector3d();
            }
         }
         VectorNd coords = new VectorNd();
         getCoordinates (coords);
         RotationMatrix3d R = new RotationMatrix3d (myRenderFrameD.R);
         // rotary axes are the first 3 axes
         for (int i=0; i<3; i++) {
            TAxis raxis = getCoupling().myRotAxes[i];
            myRenderRotAxes[i].transform (R, raxis.getAxis());
            if (i < 5) {
               raxis.applyRotation (R, coords);
            }
         }
      }
   }
   
   public void render(Renderer renderer, int flags) {
      super.render (renderer, flags);

      // float[] coords =
      //    new float[] { (float)myRenderFrameD.p.x, (float)myRenderFrameD.p.y,
      //                 (float)myRenderFrameD.p.z };
      // renderer.drawPoint(myRenderProps, coords, isSelected());

      RigidTransform3d TDW = myRenderFrameD;
      
      if (myDrawTransAxes != AxisDrawStyle.OFF) {
         // draw translation axes
         Vector3d pnt1 = new Vector3d();
         TAxis[] transAxes = getCoupling().myTransAxes;
         renderer.setColor (Color.MAGENTA);
         if (myDrawTransAxes == AxisDrawStyle.LINE) {
            renderer.setLineWidth (myRenderProps.getLineWidth());
            for (int i=0; i<transAxes.length; i++) {
               pnt1.transform (TDW.R, transAxes[i].getAxis());
               pnt1.scaledAdd (myAxisLength, pnt1, TDW.p);
               renderer.drawLine (TDW.p, pnt1);
            }
            renderer.setLineWidth (1);
         }
         else {
            for (int i=0; i<transAxes.length; i++) {
               pnt1.transform (TDW.R, transAxes[i].getAxis());
               pnt1.scaledAdd (myAxisLength, pnt1, TDW.p);
               renderer.drawArrow (
                  TDW.p, pnt1, myAxisLength/60, /*capped=*/true);
            }
         }
      }
      
      if (myDrawRotAxes != AxisDrawStyle.OFF) {
         // draw rotation axes
         Vector3d pnt1 = new Vector3d();
         renderer.setColor (Color.CYAN);
         if (myDrawRotAxes == AxisDrawStyle.LINE) {
            renderer.setLineWidth (myRenderProps.getLineWidth());
            for (int i=0; i<3; i++) {
               pnt1.scaledAdd (myAxisLength, myRenderRotAxes[i], TDW.p);
               renderer.drawLine (TDW.p, pnt1);
            }
            renderer.setLineWidth (1);
         }
         else {
            for (int i=0; i<3; i++) {
               pnt1.scaledAdd (myAxisLength, myRenderRotAxes[i], TDW.p);
               renderer.drawArrow (
                  TDW.p, pnt1, myAxisLength/60, /*capped=*/true);
            }
         }
      }
   }

   public OpenSimCustomCoupling getCoupling() {
      return (OpenSimCustomCoupling)super.getCoupling();
   }

   public Wrench getCoordinateWrenchG (int idx) {
      return ((OpenSimCustomCoupling)myCoupling).getCoordinateWrenchG (idx);
   }

   public double updateConstraints (double t, int flags) {
      return super.updateConstraints (t, flags);
   }   


   /* --- I/O methods */

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.print ("coupling=");
      ((OpenSimCustomCoupling)myCoupling).write (pw, fmt, ancestor);
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "coupling")) {
         OpenSimCustomCoupling coupling = new OpenSimCustomCoupling();
         coupling.scan (rtok, tokens);
         setCoupling (coupling);
         addCoordinateProperties (coupling.myCoords);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   
}
