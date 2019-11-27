package artisynth.demos.wrapping;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JSeparator;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.DistanceGridComp;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.MultiPointSpring.WrapSegment;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.Wrappable;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.MonitorBase;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import artisynth.demos.wrapping.Manager.Updatable;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.widgets.GuiUtils;

/**
 * Base class for models that test MultiPointSpring wrapping.
 * 
 * @author Francois Roewer-Despres
 */
public abstract class WrapTestBase extends RootModel implements Updatable {

   protected double myProfileTime = 0;
   //protected ForceMonitorProbe myForceProbe = null;
   protected boolean myPrintABPoints = false;

   public void setProfileTime (double interval) {
      myProfileTime = interval;
   }

   public double getProfileTime () {
      return myProfileTime;
   }

   public void setDistanceGridVisible (Wrappable wrappable, boolean enable) {
      if (wrappable instanceof RigidBody) {
         DistanceGridComp gcomp = ((RigidBody)wrappable).getDistanceGridComp();
         if (gcomp != null) {
            RenderProps.setVisible (gcomp, enable);
         }
      }
   }

   public static abstract class HasUpdateFileMonitor extends MonitorBase {

      private boolean myWriteHeaderP;
      protected PrintWriter myWriter = null;

      public HasUpdateFileMonitor (boolean writeHeader) {
         myWriteHeaderP = writeHeader;
      }

      public void updateFile (File file) throws IOException {
         updateFile (file, myWriteHeaderP);
      }

      public void updateFile (File file, boolean writeHeader)
         throws IOException {
         if (myWriter != null) {
            myWriter.flush ();
            myWriter.close ();
         }
         if (file != null) {
            myWriter =
               new PrintWriter (
                  new BufferedWriter (new FileWriter (file)), true);
            if (myWriteHeaderP) {
               writeHeader ();
            }
         }
      }

      protected abstract void writeHeader ();
   }

   public static interface RequiresReset {
      void reset ();
   }

   public class ABPointsMonitor extends HasUpdateFileMonitor
   implements RequiresReset {

      PointList<Point> myPoints;
      private int myExpectedNumPoints = 2;
      private int myLargestNumActivePoints = 0;

      public ABPointsMonitor (
         String name, PointList<Point> points) {
         this (name, points, true);
      }

      public ABPointsMonitor (
         String name, PointList<Point> points, boolean writeHeader) {
         super (writeHeader);
         setName (name);
         myPoints = points;
      }

      @Override
      public void reset () {
         myLargestNumActivePoints = 0;
      }

      public int getLargestNumActivePoints () {
         return myLargestNumActivePoints;
      }

      @Override
      protected void writeHeader () {
         myWriter.print ("time,activeNumPoints,expectedNumPoints");
         for (int i = 0; i < myExpectedNumPoints; i++) {
            String name = "p" + i;
            myWriter.print ("," + name + ".x," + name + ".y," + name + ".z");
         }
         myWriter.println ();
      }

      @Override
      public void apply (double t0, double t1) {
         if (myWriter != null) {
            int k=0;
            while (!myPoints.get(k).getPosition().equals (Point3d.ZERO)) {
               k++;
            }
            int numPoints = k;
            if (myLargestNumActivePoints < numPoints) {
               myLargestNumActivePoints = numPoints;
            }
            myWriter.print (t0 + "," + numPoints + "," + myExpectedNumPoints);
            numPoints = Math.min (numPoints, myExpectedNumPoints);
            int i = 0;
            while (i < numPoints) {
               Point3d pos = myPoints.get (i).getPosition ();
               myWriter.print ("," + pos.x + "," + pos.y + "," + pos.z);
               i++;
            }
            while (i < myExpectedNumPoints) {
               myWriter.print (",NA,NA,NA");
               i++;
            }
            myWriter.println ();
         }
      }
   }

   public static class PropertyMonitor extends HasUpdateFileMonitor {

      private LinkedList<ModelComponent> myComps = new LinkedList<> ();
      private HashMap<ModelComponent,String[]> myPropsMap = new HashMap<> ();

      public PropertyMonitor (String name) {
         this (name, true);
      }

      public PropertyMonitor (String name, boolean writeHeader) {
         super (writeHeader);
         setName (name);
      }

      public void registerComponent (ModelComponent comp, ModelComponent host)
         throws IllegalArgumentException {
         registerComponent (comp, myPropsMap.get (host));
      }

      public void registerComponent (ModelComponent comp, String... propNames)
         throws IllegalArgumentException {
         for (String propName : propNames) {
            if (comp.getProperty (propName) == null) {
               String name = comp.getName ();
               if (name == null) {
                  name = "";
               }
               else {
                  name = "(" + name + ")";
               }
               throw new IllegalArgumentException (
                  comp + name + " has no \"" + propName + "\" property.");
            }
         }
         myComps.add (comp);
         myPropsMap.put (comp, propNames);
      }

      @Override
      protected void writeHeader () {
         myWriter.print ("time");
         for (ModelComponent comp : myComps) {
            String name = comp.getName ();
            if (name == null) {
               name = comp.toString ();
            }
            for (String propName : myPropsMap.get (comp)) {
               myWriter.print ("," + name + ":" + propName);
            }
         }
         myWriter.println ();
      }

      @Override
      public void apply (double t0, double t1) {
         if (myWriter != null) {
            myWriter.print (t0);
            for (ModelComponent comp : myComps) {
               for (String propName : myPropsMap.get (comp)) {
                  myWriter.print ("," + comp.getProperty (propName).get ());
               }
            }
            myWriter.println ();
         }
      }
   }

   // Default values for properties to be redefined in subclasses.
   public static final Point3d DEFAULT_ORIGIN_BASE_POSITION = new Point3d ();
   public static final Point3d DEFAULT_INSERTION_BASE_POSITION = new Point3d ();

   // Default values for properties.
   public static final double DEFAULT_DISTANCE_GRID_DENSITY = 1.0;
   public static final boolean DEFAULT_DISTANCE_GRID_VISIBILITY = false;
   public static final Vector3d DEFAULT_ORIGIN_INTERPOLATION =
      new Vector3d (1, 1, 1);
   public static final Vector3d DEFAULT_INSERTION_INTERPOLATION =
      new Vector3d (0, 0, 0);
   public static final Vector3i DEFAULT_EXPLICIT_GRID_RES = 
      new Vector3i (0, 0, 0);

   // Default values for other properties copied from
   // artisynth.models.wrapping.LollyPopDemo.
   public static final int DEFAULT_NUM_SEGMENTS = 100;
   public static final double DEFAULT_STIFFNESS = 300;
   public static final double DEFAULT_DAMPING = 1.0;
   public static final double DEFAULT_REST_LENGTH = 0.0;

   // Properties.
   protected double myDistanceGridDensity = DEFAULT_DISTANCE_GRID_DENSITY;
   protected boolean myDistanceGridVisibleP = DEFAULT_DISTANCE_GRID_VISIBILITY;
   protected Point3d myOriginBasePosition =
      new Point3d (DEFAULT_ORIGIN_BASE_POSITION);
   protected Point3d myInsertionBasePosition =
      new Point3d (DEFAULT_INSERTION_BASE_POSITION);
   protected Vector3d myOriginInterpolation =
      new Vector3d (DEFAULT_ORIGIN_INTERPOLATION);
   protected Vector3d myInsertionInterpolation =
      new Vector3d (DEFAULT_INSERTION_INTERPOLATION);
   protected Vector3i myExplicitGridRes =
      new Vector3i (DEFAULT_EXPLICIT_GRID_RES);

   // Other properties copied from artisynth.models.wrapping.LollyPopDemo.
   protected int myNumSegments = DEFAULT_NUM_SEGMENTS;

   // Model components.
   protected MechModel myMechMod;
   protected PointList<Point> myABPoints;
   protected Particle myOrigin;
   protected Particle myInsertion;
   protected MultiPointSpring mySpring;
   //protected ArrayList<Point> mySpringABPoints = new ArrayList<> ();

   // Other fields.
   protected ControlPanel myPanel;
   protected ControlPanel myABPanel;
   //protected ABPointsPanel mySpringPointsPanel;
   protected PropertyMonitor myPropertyMonitor;
   //protected TimedSpringMonitor myTimedSpringMonitor;

   public static PropertyList myProps =
      new PropertyList (WrapTestBase.class, RootModel.class);

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   static {
      myProps.add (
         "numSegments", "The number of SubSegments of the MultiPointSpring.",
         DEFAULT_NUM_SEGMENTS);
      myProps.add (
         "distanceGridDensity", "The density of the distance grid.",
         DEFAULT_DISTANCE_GRID_DENSITY);
      myProps.add (
         "distanceGridVisible isDistanceGridVisible setDistanceGridVisible",
         "Toggles the visibility of the distance grid.",
         DEFAULT_DISTANCE_GRID_VISIBILITY);
      myProps.add (
         "originBasePosition",
         "The base position of the MultiPointSpring's origin.",
         DEFAULT_ORIGIN_BASE_POSITION);
      myProps.add (
         "insertionBasePosition",
         "The base position of the MultiPointSpring's insertion.",
         DEFAULT_INSERTION_BASE_POSITION);
      myProps.add (
         "originInterpolation",
         "The origin interpolation of the MultiPointSpring.",
         DEFAULT_ORIGIN_INTERPOLATION);
      myProps.add (
         "insertionInterpolation",
         "The insertion interpolation of the MultiPointSpring.",
         DEFAULT_INSERTION_INTERPOLATION);
      myProps.add (
         "explicitGridRes",
         "Explicit distance grid res along x, y, z",
         DEFAULT_EXPLICIT_GRID_RES);
      myProps.addReadOnly (
         "originPosition",
         "The current position of the MultiPointSpring's origin.");
      myProps.addReadOnly (
         "insertionPosition",
         "The current position of the MultiPointSpring's insertion.");
   }

   public int getNumSegments () {
      return myNumSegments;
   }

   public void setNumSegments (int numSegments) {
      if (myNumSegments != numSegments) {
         myNumSegments = numSegments;
         if (mySpring != null) {
            mySpring.removePoint (myInsertion);
            mySpring.removePoint (myOrigin);
            mySpring.addPoint (myOrigin);
            mySpring.setSegmentWrappable (myNumSegments);
            mySpring.addPoint (myInsertion);
            updateABPoints ();
         }
      }
   }

   public double getDistanceGridDensity () {
      return myDistanceGridDensity;
   }
   
   public Vector3i getExplicitGridRes() {
      return myExplicitGridRes;
   }
   
   public void setExplicitGridRes (Vector3i res) {
      Vector3i newRes = new Vector3i(res);
      if (res.x <= 0 || res.y <0 || res.z <= 0) {
         newRes.set(0, 0, 0);
      }
      if (!newRes.equals (myExplicitGridRes)) {
         myExplicitGridRes.set (res);
         RigidBody body = (RigidBody)getWrappable();
         if (body != null) {
            if (newRes.equals (Vector3i.ZERO)) {
               setGridRes (body);
            }
            else {
               body.setDistanceGridRes (newRes);
            }
         }
         if (myPanel != null) {
            myPanel.updateWidgetValues ();
         }
      }
   }
   
   public abstract Wrappable getWrappable();

   public abstract void setDistanceGridDensity (double density);

   protected void setDistanceGridDensity (double density, Wrappable curWrap) {
      if (myDistanceGridDensity != density) {
         myDistanceGridDensity = density;
         setGridRes ((RigidBody)curWrap);
         myPanel.updateWidgetValues ();
      }
   }
   
   protected abstract void setGridRes (RigidBody body);

   public boolean isDistanceGridVisible () {
      return myDistanceGridVisibleP;
   }

   public abstract void setDistanceGridVisible (boolean enable);

   protected void setDistanceGridVisible (boolean enable, Wrappable curWrap) {
      if (myDistanceGridVisibleP != enable) {
         myDistanceGridVisibleP = enable;
         DistanceGridComp gcomp = ((RigidBody)curWrap).getDistanceGridComp();
         RenderProps.setVisible (gcomp, enable);
         myPanel.updateWidgetValues ();
      }
   }

   public Point3d getOriginBasePosition () {
      return myOriginBasePosition;
   }

   public void setOriginBasePosition (Point3d position) {
      if (!myOriginBasePosition.equals (position)) {
         myOriginBasePosition.set (position);
         if (myOrigin != null) {
            setPosition (myOrigin, myOriginInterpolation);
            updateABPoints ();
            resetInitialState();
         }
      }
   }

   public Point3d getInsertionBasePosition () {
      return myInsertionBasePosition;
   }

   public void setInsertionBasePosition (Point3d position) {
      if (!myInsertionBasePosition.equals (position)) {
         myInsertionBasePosition.set (position);
         if (myInsertion != null) {
            setPosition (myInsertion, myInsertionInterpolation);
            updateABPoints ();
            resetInitialState();
         }
      }
   }

   public Vector3d getOriginInterpolation () {
      return myOriginInterpolation;
   }

   public void setOriginInterpolation (Vector3d interpolation) {
      if (!myOriginInterpolation.equals (interpolation)) {
         myOriginInterpolation.set (interpolation);
         setPosition (myOrigin, myOriginInterpolation);
         updateABPoints ();
      }
   }

   public Vector3d getInsertionInterpolation () {
      return myInsertionInterpolation;
   }

   public void setInsertionInterpolation (Vector3d interpolation) {
      if (!myInsertionInterpolation.equals (interpolation)) {
         myInsertionInterpolation.set (interpolation);
         setPosition (myInsertion, myInsertionInterpolation);
         updateABPoints ();
      }
   }

   protected void setPosition (Point p, Vector3d interpolation) {
      Point3d diff = new Point3d ();
      diff.sub (myOriginBasePosition, myInsertionBasePosition);
      diff.scale (interpolation.x, interpolation.y, interpolation.z);
      diff.add (myInsertionBasePosition);
      p.setPosition (diff);
   }

   public Point3d getOriginPosition () {
      Point3d pos = new Point3d ();
      myOrigin.getPosition (pos);
      return pos;
   }

   public Point3d getInsertionPosition () {
      Point3d pos = new Point3d ();
      myInsertion.getPosition (pos);
      return pos;
   }

   @Override
   public void update () {
      updateModel (true);
      if (myPanel != null) {
         myPanel.updateWidgetValues ();
      }
   }

   protected abstract void updateModel (boolean removeOld);

   protected void updateSolution() {
   }
   
   protected void updateABPoints () {
      mySpring.updateWrapSegments ();
      updatePointPositions();
      if (myPanel != null) {
         myPanel.updateWidgetValues ();
      }
   }
   
   // update the positions of A/B points in the control panel
   protected void updatePointPositions() {
      ArrayList<Point> ABpoints = new ArrayList<Point>();
      mySpring.getAllABPoints (ABpoints);
      int k=0;
      while (k < ABpoints.size()) {
         myABPoints.get(k).setPosition (ABpoints.get(k).getPosition());
         k++;
      }
      while (k < 2) {
         myABPoints.get(k).setPosition (Point3d.ZERO);
         k++;
      }
   }

   protected PointList<Point> createABPointList (
      String listName, String widgetName, int npairs,
      ControlPanel panel, MechModel mech) {

      PointList<Point> list = new PointList<Point> (Point.class, listName);
      for (int i=0; i<npairs; i++) {
         list.add (new Point ("A"+i, Point3d.ZERO));
         list.add (new Point ("B"+i, Point3d.ZERO));
      }
      mech.add (list);
      for (int i=0; i<npairs; i++) {
         panel.addWidget ("A "+widgetName, list.get(i*2), "position");
         panel.addWidget ("B "+widgetName, list.get(i*2+1), "position");
      }

      return list;
   }

   @Override
   public void build (String[] args) throws IOException {
      redefinePropertiesAndDefaults ();
      myMechMod = new MechModel ("mechMod");
      addModel (myMechMod);
      createSpringAndParticles ();

      myABPanel = new ControlPanel ("AB points");
      myABPoints = createABPointList (
         "AB points", "position", 3, myABPanel, myMechMod);
      addControlPanel (myABPanel);

      addMonitor (
         new ABPointsMonitor (
            "SpringABPointsMonitor", myABPoints));
      myPropertyMonitor = new PropertyMonitor ("PropertyMonitor");
      myPropertyMonitor.registerComponent (mySpring, "length", "lengthDot");
      addMonitor (myPropertyMonitor);
      // myTimedSpringMonitor =
      //    new TimedSpringMonitor ("TimedSpringMonitor", mySpring);
      // addMonitor (myTimedSpringMonitor);

      //myForceProbe = addForceProbe ((Frame)getWrappable());
   }

   @Override
   public void attach (DriverInterface di) {
      super.attach (di);
      if (myMechMod == null) {
         myMechMod = (MechModel)findComponent ("models/mechMod");
         mySpring = (MultiPointSpring)myMechMod.findComponent (
            "multiPointSprings/spring");
         myOrigin = (Particle)myMechMod.findComponent ("particles/origin");
         myInsertion = (Particle)myMechMod.findComponent ("particles/insertion");
         myPanel = (ControlPanel)getControlPanels().get(0);
         myABPanel = (ControlPanel)getControlPanels().get(1);
      }
   }

   protected abstract void redefinePropertiesAndDefaults ();

   protected void createSpringAndParticles () {
      myOrigin = new Particle (1.0, myOriginBasePosition);
      myOrigin.setName ("origin");
      myOrigin.setDynamic (false);
      myMechMod.addParticle (myOrigin);
      myInsertion = new Particle (1.0, myInsertionBasePosition);
      myInsertion.setName ("insertion");
      myInsertion.setDynamic (false);
      myMechMod.addParticle (myInsertion);
      mySpring =
         new MultiPointSpring (
            "spring", DEFAULT_STIFFNESS, DEFAULT_DAMPING, DEFAULT_REST_LENGTH);
      myMechMod.addMultiPointSpring (mySpring);
      mySpring.addPoint (myOrigin);
      mySpring.setSegmentWrappable (myNumSegments);
      mySpring.addPoint (myInsertion);
      RenderProps.setSphericalPoints (this, 8.0 / 30.0, Color.WHITE);
      RenderProps.setCylindricalLines (
         this, 8.0 / 40.0, Color.RED);
      mySpring.setDrawABPoints (true);
   }

   protected void addMechModWidgets () {
      myPanel.addWidget (myMechMod, "integrator");
      myPanel.addWidget (myMechMod, "maxStepSize");
      myPanel.addWidget (myMechMod, "gravity");
   }

   protected void addOriginAndInsertionWidgets () {
      myPanel.addWidget (this, "originBasePosition");
      myPanel.addWidget (this, "originInterpolation");
      myPanel.addWidget (this, "insertionBasePosition");
      myPanel.addWidget (this, "insertionInterpolation");
      myPanel.addWidget (this, "originPosition");
      myPanel.addWidget (this, "insertionPosition");
   }

   protected void addSpringWidgets () {
      myPanel.addWidget (mySpring, "material.stiffness");
      myPanel.addWidget (mySpring, "material.damping");
      myPanel.addWidget (mySpring, "wrapStiffness");
      myPanel.addWidget (mySpring, "wrapDamping");
      myPanel.addWidget (mySpring, "contactStiffness");
      myPanel.addWidget (mySpring, "contactDamping");
      myPanel.addWidget (mySpring, "convergenceTol");
      myPanel.addWidget (new JSeparator ());
      myPanel.addWidget (mySpring, "maxWrapIterations");
      myPanel.addWidget (this, "numSegments");
      myPanel.addWidget (mySpring, "restLength");
      myPanel.addWidget (mySpring, "sor");
      //myPanel.addWidget (mySpring, "dnrmGain");
      myPanel.addWidget (new JSeparator ());
      myPanel.addWidget (mySpring, "drawKnots");
      myPanel.addWidget (mySpring, "drawABPoints");
      myPanel.addWidget (mySpring, "debugLevel");
   }

   protected void addDistanceGridWidgets () {
      myPanel.addWidget (this, "distanceGridDensity");
      myPanel.addWidget (this, "distanceGridVisible");
   }

   protected abstract void addPanelWidgets ();

   protected void addControlPanel () {
      myPanel = new ControlPanel ("Controls", "");
      addPanelWidgets ();
      addControlPanel (myPanel, 0);
      mergeAllControlPanels (true);
   }

   @Override
   public boolean getMenuItems (List<Object> items) {
      items.add (
         GuiUtils
            .createMenuItem (this, "re-wrap", "update the spring wrapping"));
      return true;
   }

   @Override
   public void actionPerformed (ActionEvent e) {
      if (e.getActionCommand ().equals ("re-wrap")) {
         reWrap ();
      }
   }

   public void reWrap () {
      mySpring.updateWrapSegments ();
      myPanel.updateWidgetValues ();
      rerender ();
   }

   boolean[] inContact = new boolean[0];

   public StepAdjustment advance (double t0, double t1, int flags) {

      if (myProfileTime > 0 && t0 == 0) {
         mySpring.setProfiling (true); // reset profiling info
      }

      StepAdjustment sa = super.advance (t0, t1, flags);
      updatePointPositions();

      if (myPrintABPoints) {
         WrapSegment wseg = (WrapSegment)mySpring.getSegment(0);
         System.out.println ("AB points at t1=" + t1 + " " + wseg.numABPoints());
         for (int i=0; i<wseg.numABPoints(); i++) {
            Point3d p = wseg.getABPoint(i).getPosition();
            System.out.printf ("  %g %g %g\n", p.x, p.y, p.z);
         }
      }
      

      if (myProfileTime > 0 &&
          (((int)(1000*t1))%((int)(1000*myProfileTime))) == 0) {
         double convergedFraction =
            mySpring.getConvergedCount()/(double)mySpring.getContactCount();
         System.out.printf (
            "avgTime %g usec iters= %d updateContacts=%d converged: %g\n",
            mySpring.getProfileTimeUsec(),
            mySpring.getIterationCount(),
            mySpring.getUpdateContactsCount(),
            convergedFraction);
         mySpring.setProfiling (true);
      }
      
      return sa;
   }

   public MultiPointSpring getSpring() {
      return mySpring;
   }

}
