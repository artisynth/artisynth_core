package artisynth.demos.wrapping;

import java.io.IOException;
import javax.swing.JSeparator;

import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.Wrappable;
import artisynth.core.modelbase.ControllerBase;
import artisynth.core.modelbase.MonitorBase;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;

/**
 * Base class for models that test MultiPointSpring wrapping in a
 * setting where wrappable objects are moved parametrically.
 * 
 * @author Francois Roewer-Despres
 */
public abstract class ParametricTestBase extends WrapTestBase {

   public static class EndpointPenetrationMonitor extends MonitorBase
   implements RequiresReset {
      private ParametricTestBase myRootModel;
      private boolean myHasPenetratedP = false;

      public EndpointPenetrationMonitor (String name,
      ParametricTestBase root) {
         setName (name);
         myRootModel = root;
      }

      @Override
      public void reset () {
         myHasPenetratedP = false;
      }

      public boolean hasPenetrated () {
         return myHasPenetratedP;
      }

      @Override
      public void apply (double t0, double t1) {
         if (!myHasPenetratedP) {
            Wrappable wrappable = myRootModel.getCurrentWrappable ();
            Point3d origin = myRootModel.getOriginPosition ();
            Point3d insertion = myRootModel.getInsertionPosition ();
            Vector3d nrm = new Vector3d ();
            Matrix3d dnrm = new Matrix3d ();
            if (wrappable.penetrationDistance (nrm, dnrm, origin) < 0) {
               myHasPenetratedP = true;
            }
            else {
               if (wrappable.penetrationDistance (nrm, dnrm, insertion) < 0) {
                  myHasPenetratedP = true;
               }
            }
         }
      }
   }

   // Model components.
   protected Wrappable myWrappable; // The currently-active Wrappable.
   
   public Wrappable getWrappable() {
      return myWrappable;
   }

   // Other fields.
   protected ParametricMotionControllerBase myController;

   @Override
   public void setDistanceGridDensity (double density) {
      setDistanceGridDensity (density, myWrappable);
   }

   @Override
   public void setDistanceGridVisible (boolean enable) {
      setDistanceGridVisible (enable, myWrappable);
   }

   public Wrappable getCurrentWrappable () {
      return myWrappable;
   }

   @Override
   protected void updateModel (boolean removeOldWrappable) {
      if (removeOldWrappable) {
         mySpring.removeWrappable (myWrappable);
         myMechMod.removeRigidBody ((RigidBody)myWrappable);
      }
   }
   
   protected void updateExactSolution() {
      // by default does nothing
   }

   @Override
   public void build (String[] args) throws IOException {
      super.build (args);
      addMonitor (
         new EndpointPenetrationMonitor ("EndPointPenetrationMonitor", this));
   }

   public static class ParametricMotionControllerBase extends ControllerBase {

      public static enum PointMotion {
         FIXED, ATTACHED, PARAMETRIC;
      }

      public boolean myEnabled = true;

      public static final boolean DEFAULT_CONTROL_WRAPPABLE = true;
      public static final boolean DEFAULT_CONTROL_POINTS = true;
      public static final PointMotion DEFAULT_ORIGIN_MOTION = PointMotion.FIXED;
      public static final PointMotion DEFAULT_INSERTION_MOTION =
         PointMotion.FIXED;
      public static final double DEFAULT_PERIOD = 10.0;
      public static final double DEFAULT_DISPLACEMENT = 4;
      public static final double DEFAULT_ROTATION_ANGLE = Math.PI / 6;

      protected boolean myControlWrappableP = DEFAULT_CONTROL_WRAPPABLE;
      protected boolean myControlPointsP = DEFAULT_CONTROL_POINTS;
      protected PointMotion myOriginMotion = DEFAULT_ORIGIN_MOTION;
      protected PointMotion myInsertionMotion = DEFAULT_INSERTION_MOTION;
      protected double myPeriod = DEFAULT_PERIOD;
      protected double myDisplacement = DEFAULT_DISPLACEMENT;
      protected double myRotationAngle = DEFAULT_ROTATION_ANGLE;

      protected ParametricTestBase myRoot;
      protected RigidTransform3d myPose;
      protected Point myPoint;

      public static PropertyList myProps =
         new PropertyList (
            ParametricMotionControllerBase.class, ControllerBase.class);

      @Override
      public PropertyList getAllPropertyInfo () {
         return myProps;
      }

      static {
         myProps.add (
            "enabled",
            "Toggles whether this controller is active", true);

         myProps.add (
            "controlWrappable",
            "Toggles whether the motion controller controls the Wrappable's motion.",
            DEFAULT_CONTROL_WRAPPABLE);
         myProps.add (
            "controlPoints",
            "Toggles whether the motion controller controls the points' motion.",
            DEFAULT_CONTROL_POINTS);
         myProps.add (
            "originMotion",
            "Changes how the motion controller controls the origin point's motion.",
            DEFAULT_ORIGIN_MOTION);
         myProps.add (
            "insertionMotion",
            "Changes how the motion controller controls the insertion point's motion.",
            DEFAULT_INSERTION_MOTION);
         myProps.add (
            "period", "The current period of the motion.", DEFAULT_PERIOD);
         myProps.add (
            "displacement",
            "The current displacement of the origin and insertion's motion.",
            DEFAULT_DISPLACEMENT);
         myProps.add (
            "rotationAngle",
            "The current rotation angle of the Wrappable's motion.",
            DEFAULT_ROTATION_ANGLE);
      }

      public boolean getControlWrappable () {
         return myControlWrappableP;
      }

      public void setControlWrappable (boolean enable) {
         if (myControlWrappableP != enable) {
            myControlWrappableP = enable;
            myRoot.myPanel.updateWidgetValues ();
         }
      }

      public boolean getControlPoints () {
         return myControlPointsP;
      }

      public void setControlPoints (boolean enable) {
         if (myControlPointsP != enable) {
            myControlPointsP = enable;
            myRoot.myPanel.updateWidgetValues ();
         }
      }

      public boolean getEnabled() {
         return myEnabled;
      }

      public void setEnabled (boolean enabled) {
         myEnabled = enabled;
      }

      public PointMotion getOriginMotion () {
         return myOriginMotion;
      }

      public void setOriginMotion (PointMotion motion) {
         if (myOriginMotion != motion) {
            Point3d pnt = new Point3d(myRoot.getOriginBasePosition());
            if (motion == PointMotion.PARAMETRIC) {
               pnt.y -= getDisplacement();
            }
            else if (myOriginMotion == PointMotion.PARAMETRIC) {
               pnt.y += getDisplacement();
            }
            myOriginMotion = motion;
            myRoot.setOriginBasePosition(pnt);
            myRoot.myPanel.updateWidgetValues ();
         }
      }

      public PointMotion getInsertionMotion () {
         return myInsertionMotion;
      }

      public void setInsertionMotion (PointMotion motion) {
         if (myInsertionMotion != motion) {

            Point3d pnt = new Point3d(myRoot.getInsertionBasePosition());
            if (motion == PointMotion.PARAMETRIC) {
               pnt.y -= getDisplacement();
            }
            else if (myInsertionMotion == PointMotion.PARAMETRIC) {
               pnt.y += getDisplacement();
            }
            myInsertionMotion = motion;
            myRoot.setInsertionBasePosition(pnt);
            myRoot.myPanel.updateWidgetValues ();
         }
      }

      public double getPeriod () {
         return myPeriod;
      }

      public void setPeriod (double period) {
         if (myPeriod != period) {
            myPeriod = period;
            myRoot.myPanel.updateWidgetValues ();
         }
      }

      public double getDisplacement () {
         return myDisplacement;
      }

      public void setDisplacement (double displacement) {
         if (myDisplacement != displacement) {
            myDisplacement = displacement;
            myRoot.myPanel.updateWidgetValues ();
         }
      }

      public double getRotationAngle () {
         return myRotationAngle;
      }

      public void setRotationAngle (double angle) {
         if (myRotationAngle != angle) {
            myRotationAngle = angle;
            myRoot.myPanel.updateWidgetValues ();
         }
      }

      public ParametricMotionControllerBase (ParametricTestBase root) {
         this (null, root);
      }

      public ParametricMotionControllerBase (String name,
      ParametricTestBase root) {
         setName (name);
         myRoot = root;
         myPose = new RigidTransform3d ();
         myPoint = new Point ();
      }

      protected void computeWrappablePose (double angle) {
         myPose.p.setZero ();
         myPose.R.setAxisAngle (Vector3d.X_UNIT, angle);
      }

      protected void transformOriginAndInsertion (double yDisp, double zDisp) {
         switch (myOriginMotion) {
            case ATTACHED:
               transformAttachedParticle (
                  myRoot.myOrigin, myRoot.myOriginInterpolation);
               break;
            case FIXED:
               transformFixedParticle (
                  myRoot.myOrigin, myRoot.myOriginInterpolation);
               break;
            case PARAMETRIC:
               transformParametricParticle (
                  myRoot.myOrigin, myRoot.myOriginInterpolation, yDisp, zDisp);
               break;
         }
         switch (myInsertionMotion) {
            case ATTACHED:
               transformAttachedParticle (
                  myRoot.myInsertion, myRoot.myInsertionInterpolation);
               break;
            case FIXED:
               transformFixedParticle (
                  myRoot.myInsertion, myRoot.myInsertionInterpolation);
               break;
            case PARAMETRIC:
               transformParametricParticle (
                  myRoot.myInsertion, myRoot.myInsertionInterpolation, yDisp,
                  zDisp);
               break;
         }
      }

      protected void transformAttachedParticle (
         Particle particle, Vector3d interpolation) {
         ((RigidBody)myRoot.myWrappable).getPose (myPose);
         transformParticle (particle, interpolation);
      }

      protected void transformFixedParticle (
         Particle particle, Vector3d interpolation) {
         myPose.p.setZero ();
         myPose.R.setIdentity ();
         transformParticle (particle, interpolation);
      }

      protected void transformParametricParticle (
         Particle particle, Vector3d interpolation, double yDisp,
         double zDisp) {
         myPose.p.set (0, yDisp, zDisp);
         myPose.R.setIdentity ();
         transformParticle (particle, interpolation);
      }

      protected void transformParticle (
         Particle particle, Vector3d interpolation) {
         myRoot.setPosition (myPoint, interpolation);
         myPoint.transformGeometry (myPose);
         particle.setTargetPosition (myPoint.getPosition ());
      }

      @Override
      public void apply (double t0, double t1) {
         if (myEnabled) {
            double x = t0 / myPeriod * 2 * Math.PI;
            double sin = Math.sin (x);
            if (myControlWrappableP) {
               computeWrappablePose (-myRotationAngle * sin);
               ((RigidBody)myRoot.myWrappable).setPose (myPose);
            }
            if (myControlPointsP) {
               double yDisp = myDisplacement * (1-Math.cos (x));
               double zDisp = myDisplacement * sin;
               transformOriginAndInsertion (yDisp, zDisp);
            }
            myRoot.updateExactSolution();
            //myRoot.updateABPoints ();
         }
      }
   }

   @Override
   protected void addPanelWidgets () {
      addMechModWidgets ();
      myPanel.addWidget (new JSeparator ());
      myPanel.addWidget (myController, "controlWrappable");
      myPanel.addWidget (myController, "controlPoints");
      myPanel.addWidget (myController, "originMotion");
      myPanel.addWidget (myController, "insertionMotion");
      myPanel.addWidget (myController, "period");
      myPanel.addWidget (myController, "displacement");
      myPanel.addWidget (myController, "rotationAngle");
      myPanel.addWidget (new JSeparator ());
      addOriginAndInsertionWidgets ();
      myPanel.addWidget (new JSeparator ());
      addSpringWidgets ();
      myPanel.addWidget (new JSeparator ());
      addDistanceGridWidgets ();
   }
}
