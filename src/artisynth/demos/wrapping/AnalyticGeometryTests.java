package artisynth.demos.wrapping;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;

import javax.swing.JLabel;
import javax.swing.JSeparator;

import artisynth.core.materials.UnidirectionalLinearAxialMaterial;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.MultiPointSpring.WrapSegment;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.Wrappable;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.demos.wrapping.AnalyticGeometryManager.Geometry;
import artisynth.demos.wrapping.AnalyticGeometryManager.WrapMethod;
import artisynth.demos.wrapping.Manager.Creator;
import maspack.geometry.DistanceGrid;
import maspack.geometry.DistanceGrid.DistanceMethod;
import maspack.geometry.DistanceGrid.GridEdge;
import maspack.geometry.DistanceGrid.TetDesc;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.spatialmotion.Wrench;
import maspack.widgets.LabeledComponent;
import maspack.widgets.LabeledComponentBase;

/**
 * Model that tests MultiPointWrapping around objects whose geometry
 * is described by analytic surfaces. The contact and tangent information
 * for such objects can be computed analytically, and compared with the
 * more general solution produced using distance grids. Some of these
 * objects also admit an exact solution for the wrap path.
 * 
 * @author Francois Roewer-Despres, John E. Lloyd
 */
public class AnalyticGeometryTests extends ParametricTestBase {

   boolean specialSphereTest = false;
   boolean sphereOriginInsertion = false;

   // settings for sphere animation frames in paper:
   // sphereOriginInsertion = true;
   // lineRadius = 0.35
   // pointRadius = 0.64

   // settings for cylinder animation frames in paper:
   // lineRadius = 0.45
   // pointRadius = 0.75

   // Model components.
   protected ExactWrappedSpring myExactSolution;
   protected boolean myExactSolutionActive = true;
   protected PointList<Point> myExactABPoints;   
   protected AnalyticGeometryManager<WrappableGeometryPair> myGeometryManager;

   // Other fields.
   //protected ABPointsPanel myExactSolPointsPanel;

   public static PropertyList myProps =
      new PropertyList (AnalyticGeometryTests.class, ParametricTestBase.class);

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   static {
      myProps.addReadOnly (
         "strandLength", "The length of the strand.");
      myProps.addReadOnly (
         "exactStrandLength", "The exact length of the strand.");
      myProps.addReadOnly (
         "strandLengthDot", "The derivative of the strand length.");
      myProps.addReadOnly (
         "exactStrandLengthDot", "The derivative of the exact strand length.");
      myProps.addReadOnly (
         "wrapForce", "Force on the wrappable.");
      myProps.addReadOnly (
         "exactForce", "Exact force on the wrappable.");
      myProps.addReadOnly (
         "wrapForceError", "Force error on the wrappable.");
      // myProps.addReadOnly (
      //    "wrapMomentError", "Moment error on the wrappable.");
      // myProps.addReadOnly (
      //    "wrapTotalError", "Total error on the wrappable.");
      myProps.addReadOnly (
         "ABError", "AB point error.");
      myProps.addReadOnly (
         "lengthError",
         "The difference between the length of the exact and MultiPointSpring solutions.");
      myProps.addReadOnly (
         "lengthDotError",
         "The difference between the length time derivative of the exact and MultiPointSpring solutions.");
      myProps.addReadOnly (
         "forceError",
         "The difference between the force magnitude of the exact and MultiPointSpring solutions.");
   }

   public double getStrandLength() {
      return mySpring.getLength();
   }

   public double getStrandLengthDot() {
      return mySpring.getLengthDot();
   }

   public double getExactStrandLength() {
      if (myExactSolutionActive) {
         return myExactSolution.getLength();
      }
      else {
         return 0;
      }
   }

   public double getExactStrandLengthDot() {
      if (myExactSolutionActive) {
         return myExactSolution.getLengthDot();
      }
      else {
         return 0;
      }
   }

   private Vector3d getABForce (Point3d p0, Point3d p1, double F) {
      Vector3d f = new Vector3d();
      f.sub (p1, p0);
      f.scale (F/f.norm());
      return f;
   }

   private Vector3d getABMoment (RigidTransform3d TWW, Point3d p, Vector3d f) {
      Vector3d ploc = new Vector3d();
      ploc.sub (p, TWW.p);
      Vector3d m = new Vector3d();
      m.cross (ploc, f);
      return m;
   }

   public Wrench getExactForce() {
      Wrench wr = new Wrench();
      if (myExactSolutionActive) {
         ArrayList<Point3d> ABpnts = myExactSolution.getAllABPoints();
         if (ABpnts.size() == 2) {
            double F = myExactSolution.getForce();
            Point3d sprP0 = mySpring.getPoint(0).getPosition();
            Point3d sprPN = mySpring.getPoint(
               mySpring.numPoints()-1).getPosition();
            Vector3d fA = getABForce (ABpnts.get(0), sprP0, F);
            Vector3d fB = getABForce (ABpnts.get(1), sprPN, F);
            wr.f.add (fA, fB);
            RigidTransform3d TWW = ((RigidBody)myWrappable).getPose();
            Vector3d mA = getABMoment (TWW, ABpnts.get(0), fA);
            Vector3d mB = getABMoment (TWW, ABpnts.get(1), fB);
            wr.m.add (mA, mB);
         }
      }
      return wr;
   }

   double computeF (MultiPointSpring spr) {
      return spr.computeF (spr.getActiveLength(), spr.getActiveLengthDot());
   }

   public Wrench getWrapForce() {
      Wrench wr = new Wrench();
      if (mySpring.getSegment(0) instanceof WrapSegment) {
         WrapSegment wseg = (WrapSegment)mySpring.getSegment(0);
         if (wseg.numABPoints() == 2) {
            double F = computeF(mySpring);
            Point3d sprP0 = mySpring.getPoint(0).getPosition();
            Point3d sprPA = wseg.getABPoint(0).getPosition();
            Point3d sprPB = wseg.getABPoint(1).getPosition();
            Point3d sprPN = mySpring.getPoint(1).getPosition();
            Vector3d fA = getABForce (sprPA, sprP0, F);
            Vector3d fB = getABForce (sprPB, sprPN, F);
            wr.f.add (fA, fB);
            RigidTransform3d TWW = ((RigidBody)myWrappable).getPose();
            Vector3d mA = getABMoment (TWW, sprPA, fA);
            Vector3d mB = getABMoment (TWW, sprPB, fB);
            wr.m.add (mA, mB);
         }
      }
      return wr;
   }

   public double getWrapForceError() {
      Wrench wr = getWrapForce();
      Wrench wex = getExactForce();
      Wrench err = new Wrench();
      err.sub (wex, wr);
      return (err.f.norm()/wex.f.norm());
   }

   // public double getWrapMomentError() {
   //    Wrench wr = getWrapForce();
   //    Wrench wex = getExactForce();
   //    Wrench err = new Wrench();
   //    err.sub (wex, wr);
   //    return (err.m.norm()/wex.m.norm());
   // }

   // public double getWrapTotalError() {
   //    Wrench wr = getWrapForce();
   //    Wrench wex = getExactForce();
   //    Wrench err = new Wrench();
   //    err.sub (wex, wr);
   //    return (0.5*(err.m.norm()/wex.m.norm() + err.f.norm()/wex.f.norm()));
   // }

   private double computeError (double a, double b) {
      return Math.abs(a-b)/(0.5*(Math.abs(a)+Math.abs(b)));
   }

   public double getLengthError () {
      if (myExactSolutionActive) {
         return computeError (mySpring.getLength(), myExactSolution.getLength());
      }
      else {
         return 0;
      }
   }

   public double getLengthDotError () {
      if (myExactSolutionActive) {
         return computeError (
            mySpring.getLengthDot(),myExactSolution.getLengthDot());
      }
      else {
         return 0;
      }
   }

   public double getForceError () {
      if (myExactSolutionActive) {
         return computeError (computeF(mySpring), myExactSolution.getForce());
      }
      else {
         return 0;
      }
   }

   private double pointError (Point3d p0, Point3d p1) {
      return p0.distance(p1)/(0.5*(p0.norm()+p1.norm()));
   }

   public double getABError() {
      if (!myExactSolutionActive) {
         return 0;
      }
      myExactSolution.updateWrapSegments();
      ArrayList<Point3d> ABpnts = myExactSolution.getAllABPoints();
      if (ABpnts.size() != 2) {
         return 0;
      }
      Point3d pAExact = ABpnts.get(0);
      Point3d pBExact = ABpnts.get(1);
      
      WrapSegment wseg = null;
      if (mySpring.getSegment(0) instanceof WrapSegment) {
         wseg = (WrapSegment)mySpring.getSegment(0);
      }
      if (wseg == null || wseg.numABPoints() != 2) {
         return 0;
      }
      Point3d pA = wseg.getABPoint(0).getPosition();
      Point3d pB = wseg.getABPoint(1).getPosition();
      return (0.5*(pointError (pA, pAExact)) + pointError (pB, pBExact));
   }

   @Override
   protected void setGridRes (RigidBody body) {
      myGeometryManager.setGridRes (
         body, myDistanceGridDensity, myExplicitGridRes);
   }

   public static class WrappableGeometryPair {
      public Wrappable myWrappable;
      public ExactWrappableGeometry myWrapGeometry;

      public WrappableGeometryPair (Wrappable wrappable,
      ExactWrappableGeometry wrapGeometry) {
         myWrappable = wrappable;
         myWrapGeometry = wrapGeometry;
      }
   }

   NumericOutputProbe addOutputProbe (String name) {
      NumericOutputProbe probe =
         new NumericOutputProbe (this, name, 0, 10, 0.01);
      probe.setName (name);
      addOutputProbe (probe);      
      return probe;
   }

   void addLengthProbes() {
      addOutputProbe ("strandLength");
      addOutputProbe ("exactStrandLength");
      addOutputProbe ("strandLengthDot");
      addOutputProbe ("exactStrandLengthDot");
      addOutputProbe ("wrapForceError");
      //addOutputProbe ("wrapMomentError");
      //addOutputProbe ("wrapTotalError");
      addOutputProbe ("ABError");

      ForceErrorProbe ferrorProbe =
         new ForceErrorProbe (
            (RigidBody)myWrappable, mySpring, myExactSolution,
            "forceError.txt", 0, 10, 0.01);
      ferrorProbe.setName ("total force error");
      addOutputProbe (ferrorProbe);

      ExactForceProbe exactForceProbe =
         new ExactForceProbe (
            (Frame)myWrappable, mySpring, myExactSolution,
            "exactForce.txt", 0, 10, 0.01);
      exactForceProbe.setName ("exactForce");
      addOutputProbe (exactForceProbe);

      ExactMomentProbe exactMomentProbe =
         new ExactMomentProbe (
            (Frame)myWrappable, mySpring, myExactSolution,
            "exactMoment.txt", 0, 10, 0.01);
      exactMomentProbe.setName ("exactMoment");
      addOutputProbe (exactMomentProbe);
   }

   @Override
   public void build (String[] args) throws IOException {
      DistanceGrid.DEFAULT_DISTANCE_METHOD = DistanceMethod.BVH;

      super.build (args);

      myGeometryManager =
         new AnalyticGeometryManager<> (
            "GeometryManager", new Creator<WrappableGeometryPair> () {

               public WrappableGeometryPair create () {
                  RigidBody body = myGeometryManager.createActive ();
                  RenderProps.setFaceColor (
                     body, new Color(238, 232, 170)); // bone color
                  setGridRes (body);
                  setDistanceGridVisible (body, myDistanceGridVisibleP);
                  processNewBody (body);
                  return new WrappableGeometryPair (
                     (Wrappable)body, createWrapGeometry (body));
               }
            }, this);

      add (myGeometryManager);

      myABPanel.addWidget (new JSeparator());
      myExactABPoints = createABPointList (
         "AB exact", "exact", 3, myABPanel, myMechMod);

      addMonitor (
         new ABPointsMonitor (
            "ExactSolABPointsMonitor", myExactABPoints));

      createExactSolution ();
      updateModel (false);

      myController = new ParametricMotionControllerBase (this);
      addController (myController);
      addControlPanel ();

      addLengthProbes();
      setProfileTime (10);

      mySpring.setConvergenceTol (1e-8);
      mySpring.setMaxWrapIterations (20);

      for (int i=1; i<=10; i++) {
         if (sphereOriginInsertion) {
            addWayPoint (0.8*i);
         }
         else {
            addWayPoint (2*i);
         }
      }
      
      if (specialSphereTest) {
         
         RenderProps.setPointRadius (mySpring, 0.05);
         RenderProps.setLineRadius (mySpring, 0.03);
         RenderProps.setPointRadius (myExactABPoints, 0.06);
         RenderProps.setPointColor (myExactABPoints, Color.BLUE);
         RenderProps.setLineRadius (myExactSolution, 0.03);
         RenderProps.setVisible (myABPoints, false);
         mySpring.setDrawKnots (true);

         setExplicitGridRes (new Vector3i (20, 20, 20));
         myGeometryManager.setWrapMethod (WrapMethod.SIGNED_DISTANCE_GRID);
         myGeometryManager.setGeometry (Geometry.SPHERE);
         setNumSegments (50);
         myController.setOriginMotion (
            ParametricMotionControllerBase.PointMotion.PARAMETRIC);
         myController.setInsertionMotion (
            ParametricMotionControllerBase.PointMotion.PARAMETRIC);
         addBreakPoint (4.6);
      }
      mySpring.updateWrapSegments();
   }

   @Override
   protected void redefinePropertiesAndDefaults () {
      DEFAULT_ORIGIN_BASE_POSITION.set (7, -4, 17);
      DEFAULT_INSERTION_BASE_POSITION.set (-5, -1, -12);
      // set for sphere tests:
      if (specialSphereTest || sphereOriginInsertion) {
         DEFAULT_ORIGIN_BASE_POSITION.set (7, -6, 17);
         DEFAULT_INSERTION_BASE_POSITION.set (-5, -4, -12);
      }
      myOriginBasePosition.set (DEFAULT_ORIGIN_BASE_POSITION);
      myInsertionBasePosition.set (DEFAULT_INSERTION_BASE_POSITION);
   }

   protected void createExactSolution () {
      myExactSolution = new ExactWrappedSpring ("exact-solution");
      myExactSolution.setPoints (myOrigin, myInsertion);
      myExactSolution.setRestLength (DEFAULT_REST_LENGTH);
      myExactSolution.setMaterial (
         new UnidirectionalLinearAxialMaterial (
            DEFAULT_STIFFNESS, DEFAULT_DAMPING));
      // add to RootModel so it is rendered. Don't add to MechModel
      // because we don't want it called as a force effector
      addRenderable (myExactSolution);
      RenderProps
         .setCylindricalLines (myExactSolution, 8.0 / 40.0, Color.WHITE);
      myPropertyMonitor.registerComponent (myExactSolution, mySpring);
   }

   @Override
   protected void updateModel (boolean removeOldWrappable) {
      super.updateModel (removeOldWrappable);
      WrappableGeometryPair pair = myGeometryManager.getActive ();
      myWrappable = pair.myWrappable;
      myMechMod.addRigidBody ((RigidBody)myWrappable);
      mySpring.addWrappable (myWrappable);
      // if (myForceProbe != null) {
      //    myForceProbe.setFrame ((Frame)myWrappable);
      // }
      // possible via points for strand initilization
      Point3d[] viaPnts = myGeometryManager.getViaPoints();
      WrapSegment wseg = (WrapSegment)mySpring.getSegment(0);
      wseg.initializeStrand (viaPnts);
      myExactSolutionActive = (pair.myWrapGeometry != null);
      if (pair.myWrapGeometry != null) {
         myExactSolution.setWrapGeometry (pair.myWrapGeometry);
      }
      RenderProps.setVisible (myExactSolution, myExactSolutionActive);
      RenderProps.setVisible (myExactABPoints, myExactSolutionActive);
      updateABPoints ();
   }

   protected ExactWrappableGeometry createWrapGeometry (Frame frame) {
      ExactWrappableGeometry wrap = null;
      switch (myGeometryManager.getGeometry ()) {
         case CYLINDER:
            wrap =
               new ExactWrappableCylinder (
                  frame, Point3d.ZERO, Vector3d.Z_UNIT,
                  myGeometryManager.getCylinderRadius ());
            break;
         case CYLINDER2:
            wrap =
               new ExactWrappableCylinder (
                  frame, Point3d.ZERO, Vector3d.Z_UNIT,
                  myGeometryManager.getCylinderRadius ()/3);
            break;
         case SPHERE:
            wrap =
               new ExactWrappableSphere (
                  frame, Point3d.ZERO, myGeometryManager.getSphereRadius ());
            break;
         default:
            return null;
      }
      return wrap;
   }

   protected void updateExactSolution() {
      if (myExactSolution != null && myExactSolutionActive) {
         myExactSolution.getWrapGeometry();
         myExactSolution.updateWrapSegments();
         //mySpring.updateWrapSegments();
      }
   }
   
   protected void updatePointPositions() {
      super.updatePointPositions();
      if (myExactSolution != null) {
         ArrayList<Point3d> ABpoints;
         if (myExactSolutionActive) {
            ABpoints = myExactSolution.getAllABPoints();
         }
         else {
            ABpoints = new ArrayList<Point3d>();
         }
         int k=0;
         while (k < ABpoints.size()) {
            myExactABPoints.get(k).setPosition (ABpoints.get(k));
            k++;
         }
         while (k < 2) {
            myExactABPoints.get(k).setPosition (Point3d.ZERO);
            k++;
         }
      }
   }
   
   @Override
   protected void updateABPoints () {
      if (myExactSolutionActive) {
         myExactSolution.getWrapGeometry();
         myExactSolution.updateWrapSegments ();
      }
      super.updateABPoints ();
   }

   public AnalyticGeometryManager<WrappableGeometryPair> getGeometryManager() {
      return myGeometryManager;
   }

   @Override
   protected void addPanelWidgets () {
      super.addPanelWidgets ();
      myPanel.addWidget (new JSeparator ());
      myPanel.addWidget (myGeometryManager, "geometry");
      myPanel.addWidget (myGeometryManager, "wrapMethod");
      myPanel.addWidget (myGeometryManager, "resolution");
      myPanel.addWidget (this, "explicitGridRes");
      myPanel.addWidget (new JSeparator ());
      String name0 = "MultiPointSpring length";
      String name1 = "exact solution length       - ";
      String name2 = "length error                = ";
      changeFont (myPanel.addWidget (name0, mySpring, "length"));
      changeFont (myPanel.addWidget (name1, myExactSolution, "length"));
      changeFont (myPanel.addWidget (name2, this, "lengthError"));
      myPanel.addWidget (new JSeparator ());
      name0 = "MultiPointSpring lengthDot";
      name1 = "exact solution lengthDot    - ";
      name2 = "lengthDot error             = ";
      changeFont (myPanel.addWidget (name0, mySpring, "lengthDot"));
      changeFont (myPanel.addWidget (name1, myExactSolution, "lengthDot"));
      changeFont (myPanel.addWidget (name2, this, "lengthDotError"));
      myPanel.addWidget (new JSeparator ());
      name0 = "MultiPointSpring force";
      name1 = "exact solution force        - ";
      name2 = "force error                 = ";
      //changeFont (myPanel.addWidget (name0, mySpring, "force"));
      //changeFont (myPanel.addWidget (name1, myExactSolution, "force"));
      changeFont (myPanel.addWidget (name2, this, "forceError"));
    }

   protected void changeFont (LabeledComponentBase widget) {
      LabeledComponent myWidget = (LabeledComponent)widget;
      JLabel label = myWidget.getLabel ();
      Font font = label.getFont ();
      label.setFont (
         new Font (Font.MONOSPACED, font.getStyle (), font.getSize ()));
   }

   @Override
   public void reWrap () {
      updateABPoints ();
      super.reWrap ();
   }

   public void initialize (double t) {
      updateExactSolution();
      super.initialize (t);
   }

   public void setExplicitGridRes (Vector3i res) {
      super.setExplicitGridRes (res);

      RigidBody body = (RigidBody)getWrappable();
      if (body != null) {
         //processNewBody (body);
      }
   }

   void findSurfaceTetInfo (RigidBody body) {
      DistanceGrid grid = body.getDistanceGrid();
      ArrayList<GridEdge> iedges =
         grid.findSurfaceIntersectingEdges();
      System.out.println ("num edges=" + iedges.size());
      ArrayList<TetDesc> tets =
         grid.findSurfaceEdgeTets (iedges);
      System.out.println ("num tets=" + tets.size());
      HashSet<Integer> allVertices = new LinkedHashSet<>();
      for (TetDesc tdesc : tets) {
         Vector3i[] vertices = tdesc.getVertices();
         for (Vector3i vxyz : vertices) {
            int vi = grid.xyzIndicesToVertex (vxyz);
            allVertices.add (vi);
         }
         Vector3i midVertex = new Vector3i();
         for (int i=0; i<4; i++) {
            Vector3i vxyz0 = vertices[i];
            for (int j=i+1; j<4; j++) {
               Vector3i vxyz1 = vertices[j];
               midVertex.add (vxyz0, vxyz1);
               midVertex.scale (0.5);
               int vi = grid.xyzIndicesToVertex (midVertex);
               allVertices.add (vi);
            }
         }
      }
      System.out.println ("num vertices=" + allVertices.size());    
   }     

   void processNewBody (RigidBody body) {
      if (myGeometryManager.getWrapMethod() ==
          WrapMethod.SIGNED_DISTANCE_GRID) {
         if (myGeometryManager.getGeometry() == Geometry.SPHERE) {
            double r = myGeometryManager.getSphereRadius();
            myGeometryManager.setDistanceUsingFaces (body, r);
            myGeometryManager.setDistanceUsingMesh (body, r);
         }
         //findSurfaceTetInfo (body);
      }
   }
}
