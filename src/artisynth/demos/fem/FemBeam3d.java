package artisynth.demos.fem;

import java.awt.Point;
import java.util.*;
import java.io.*;

import javax.swing.*;

import java.awt.event.*;

import javax.swing.event.*;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.render.Renderer;
import maspack.util.*;
import maspack.widgets.DoubleFieldSlider;
import maspack.interpolation.Interpolation;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.*;
import artisynth.core.materials.AxialMuscleMaterial;
import artisynth.core.materials.MooneyRivlinMaterial;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.modelbase.*;
import artisynth.core.probes.*;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.render.*;
import artisynth.core.driver.*;

import java.awt.*;
import java.util.*;

public class FemBeam3d extends RootModel {
   public static boolean debug = false;
   private static boolean useFemMuscleModel = false;
   static double LENGTH = 1.0; // 1.0;
   static double WIDTH = 0.2; // .20;
   static double DENSITY = 1000;

   public static int ADD_BLOCKS = 0x01;
   public static int VERTICAL = 0x02;
   public static int ADD_DISPLACEMENT = 0x04;
   public static int ADD_MUSCLES = 0x08;
   public static int NO_FIXED_NODES = 0x10;
   public static int CONSTRAIN_RIGHT_NODES = 0x20;

   // MechFemConnector myConnector;
   protected LinkedList<FemNode3d> myLeftNodes = new LinkedList<FemNode3d>();
   protected LinkedList<FemNode3d> myRightNodes = new LinkedList<FemNode3d>();
   protected FemModel3d myFemMod;   
   protected MechModel myMechMod;   

   public static PropertyList myProps =
      new PropertyList (FemBeam3d.class, RootModel.class);

   static {
      myProps.add ("excitation0", "first muscle excitation", 0);
      myProps.add ("excitation1", "second muscle excitation", 0);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getExcitation0 () {
      AxialSpringList springs =
         (AxialSpringList)findComponent ("models/mech/axialSprings");
      if (springs.size() > 0) {
         return ((Muscle)springs.get(0)).getExcitation();
      }
      else {
         return 0;
      }
   }

   public void setExcitation0 (double e) {
      AxialSpringList springs =
         (AxialSpringList)findComponent ("models/mech/axialSprings");
      int num = springs.size()/2;
      if (num > 0) {
         for (int i=0; i<num; i++) {
            ((Muscle)springs.get(i)).setExcitation(e);
         }
      }
   }

   public double getExcitation1 () {
      AxialSpringList springs =
         (AxialSpringList)findComponent ("models/mech/axialSprings");
      int num = springs.size()/2;
      if (num > 0) {
         return ((Muscle)springs.get(num)).getExcitation();
      }
      else {
         return 0;
      }
   }

   public void setExcitation1 (double e) {
      AxialSpringList springs =
         (AxialSpringList)findComponent ("models/mech/axialSprings");
      int num = springs.size()/2;
      if (num > 0) {
         for (int i=num; i<springs.size(); i++) {
            ((Muscle)springs.get(i)).setExcitation(e);
         }
      }
   }

   public void build (String[] args) {
      build ("hex", 8, 4, /*options=*/0); // ADD_BLOCKS
      //build ("hex", 24, 12, /*options=*/0); // ADD_BLOCKS

      myMechMod.setIntegrator (Integrator.Trapezoidal);
      myFemMod.setMaterial (new MooneyRivlinMaterial());
      myFemMod.setIncompressible (FemModel.IncompMethod.AUTO);
      //myMechMod.getSolver().profileKKTSolveTime = true;
      //myMechMod.setProfiling (true);      
   }

   public void build (
      String type, int nx, int nyz, int options) {
      build (type, LENGTH, WIDTH, nx, nyz, options);
   }      

   public void build (
      String type, double length, double width,
      int nx, int nyz, int options) {
      build (type, length, width, width, nx, nyz, nyz, options);
   }

   public void build (
      String type, double length, double widthy, double widthz,
      int nx, int ny, int nz, int options) {

      if (useFemMuscleModel) {
         myFemMod = new FemMuscleModel ("fem");
      }
      else {
         myFemMod = new FemModel3d ("fem");
      }
      
      myFemMod.setDensity (DENSITY);
      if (type.equals ("hex")) {
         FemFactory.createHexGrid (
            myFemMod, length, widthy, widthz, nx, ny, nz);
      }
//       else if (type.equals ("quadHex")) {
//          FemFactory.createQuadhexGrid (
//             myFemMod, length, width, width, nx, nzz, nzz);
//       }
      else if (type.equals ("tet")) {
         FemFactory.createTetGrid (
            myFemMod, length, widthy, widthz, nx, ny, nz);
      }
      else if (type.equals ("quadtet")) {
         FemFactory.createQuadtetGrid (
            myFemMod, length, widthy, widthz, nx, ny, nz);
      }
      else if (type.equals ("quadhex")) {
         FemFactory.createQuadhexGrid (
            myFemMod, length, widthy, widthz, nx, ny, nz);
      }
      else if (type.equals ("wedge")) {
         FemFactory.createWedgeGrid (
            myFemMod, length, widthy, widthz, nx, ny, nz);
      }
      else if (type.equals ("quadwedge")) {
         FemFactory.createQuadwedgeGrid (
            myFemMod, length, widthy, widthz, nx, ny, nz);
      }
      else if (type.equals ("pyramid")) {
         FemFactory.createPyramidGrid (
            myFemMod, length, widthy, widthz, nx, ny, nz);
      }
      else if (type.equals ("quadpyramid")) {
         FemFactory.createQuadpyramidGrid (
            myFemMod, length, widthy, widthz, nx, ny, nz);
      }
      else {
         throw new IllegalArgumentException ("Unknown element type: "+type);
      }
      
      //myFemMod.setBounds (new Point3d (-0.6, 0, 0), new Point3d (0.6, 0, 0));
      myFemMod.setStiffnessDamping (0.002);
//      myFemMod.setYoungsModulus (100000.0);
//      myFemMod.setPoissonsRatio (0.0);
      myFemMod.setLinearMaterial (100000.0, 0.0, true);
      //      myFemMod.setMaterial (new MooneyRivlinMaterial());

      computeLeftAndRightNodes ();

      if ((options & NO_FIXED_NODES) == 0) {
         for (FemNode3d n : myLeftNodes) {
            n.setDynamic (false);
            RenderProps.setPointColor (n, Color.GRAY);
         }
      }
      myMechMod = addMechModel (myFemMod);
      setRenderProperties (myFemMod, length);

      
      if ((options & ADD_BLOCKS) != 0) {
         
         double wx, wy, wz;

         if (widthy == widthz) {
            wx = widthy/2;
            wy = widthy*1.5;
            wz = widthy*1.5;
         }
         else {
            wx = Math.min(widthy, widthz)/2;
            wy = widthy+wx;
            wz = widthz+wx;
         }

         RigidBody leftBlock =
            RigidBody.createBox ("left", wx, wy, wz, DENSITY);
         leftBlock.setPose (new RigidTransform3d (-(length+wx)/2.0, 0, 0));
         leftBlock.setDynamic (false);
         myMechMod.addRigidBody (leftBlock);

         for (FemNode3d n : myLeftNodes) {
            myMechMod.attachPoint (n, leftBlock);
            n.setDynamic (true);
         }

         RigidBody rightBlock =
            RigidBody.createBox ("right", wx, wy, wz, DENSITY);
         rightBlock.setPose (new RigidTransform3d ((length+wx)/2.0, 0, 0));
         rightBlock.setDynamic (false);

         myMechMod.addRigidBody (rightBlock);

         for (FemNode3d n : myRightNodes) {
            myMechMod.attachPoint (n, rightBlock);
         }
      }

      if ((options & ADD_MUSCLES) != 0) {
         addMuscles (myMechMod, myFemMod, 0.8,  0.06, 3, Color.RED);
         addMuscles (myMechMod, myFemMod, 0.8, -0.06, 3, Color.CYAN);
      }
      

      //      addWayPoint (new WayPoint (TimeBase.secondsToTicks(2.5), true));

      if ((options & VERTICAL) != 0) {
         myMechMod.transformGeometry (
            new RigidTransform3d (0, 0, 0, 0, 1, 0, Math.toRadians(90)));
         //myFemMod.setGravity (0, 0, 0);
         myMechMod.setGravity (0, 0, 0);
      }
      

      if ((options & ADD_DISPLACEMENT) != 0) {
         // old displacement was -.85
         addDisplacementProbes (options, myMechMod, myRightNodes, -.85, 5);
      }          

      if ((options & CONSTRAIN_RIGHT_NODES) != 0) {
         double rightX = myRightNodes.get(0).getPosition().x;
         Plane plane = new Plane (1, 0, 0, rightX);
         ParticlePlaneConstraint c = new ParticlePlaneConstraint (plane);
         c.addParticles (myRightNodes);
         myMechMod.addConstrainer (c);
      }
//          NumericOutputProbe output =
//             new NumericOutputProbe (
//                myFemMod, "elements/31:stress", null, 0.01);
//          output.setStopTime (5);
//          addOutputProbe (output);


      // myMechMod.setProfiling (true);
      // myMechMod.setPrintState ("%g");

      // addModel (myFemMod);
      addControlPanel (myMechMod, myFemMod);
   }


   public void setRenderProperties (FemModel3d mod, double length) {
      mod.setElementWidgetSize (1);
      mod.setSurfaceRendering (SurfaceRender.None);
      RenderProps.setShading (mod, Renderer.Shading.FLAT);
      RenderProps.setFaceColor (mod, new Color (0.7f, 0.7f, 0.9f));
      RenderProps.setLineWidth (mod.getElements(), 2);
      RenderProps.setLineColor (mod.getElements(), Color.blue);
      RenderProps.setPointRadius (myMechMod, 0.01*length);
      RenderProps.setPointStyle (mod, Renderer.PointStyle.SPHERE);
      RenderProps.setPointColor (mod.getNodes(), Color.GREEN);
   }

   public void computeLeftAndRightNodes () {
      
      double EPS = 1e-9;
      double minx = Double.MAX_VALUE;
      double maxx = -Double.MAX_VALUE;
      for (FemNode3d n : myFemMod.getNodes()) {
         Point3d pos = n.getPosition();
         if (pos.x > maxx) {
            maxx = pos.x;
         }
         if (pos.x < minx) {
            minx = pos.x;
         }
      }
      for (FemNode3d n : myFemMod.getNodes()) {
         if (n.getPosition().x < minx + EPS) {
            myLeftNodes.add (n);
         }
         else if (n.getPosition().x > maxx - EPS) {
            myRightNodes.add (n);
         }
      }
      System.out.println ("num right nodes: " + myRightNodes.size());
   }

   public MechModel addMechModel (FemModel3d mod) {
      
      MechModel mechMod = new MechModel ("mech");
      // mechMod.setIntegrator (MechSystemSolver.Integrator.BackwardEuler);
      mechMod.setIntegrator (
         MechSystemSolver.Integrator.ConstrainedBackwardEuler);
      mechMod.setMaxStepSize (0.01);
      mechMod.addModel (mod);
      addModel (mechMod);
      return mechMod;
   }

   private void addMuscles (
      MechModel mech, FemModel3d fem,
      double len, double z, int num, Color color) {
      FemMarker lastMkr = null;

      MuscleBundle bundle = null;
      if (useFemMuscleModel) {
         bundle = new MuscleBundle();
         ((FemMuscleModel)fem).addMuscleBundle(bundle);
         bundle.setFibresActive (true);
      }

      for (int i=0; i<=num; i++) {
         FemMarker mkr = new FemMarker (len*(0.5-i/(double)num), 0, z);
         RenderProps.setPointColor (mkr, Color.MAGENTA);
         fem.addMarker (mkr);
         if (lastMkr != null) {
            Muscle muscle = new Muscle();
            muscle.setConstantMuscleMaterial(2);
            muscle.setFirstPoint (lastMkr);
            muscle.setSecondPoint (mkr);
            RenderProps.setLineRadius (muscle, 0.01);
            RenderProps.setLineStyle (muscle, Renderer.LineStyle.SPINDLE);
            RenderProps.setLineColor (muscle, color);
            if (useFemMuscleModel) {
               bundle.addFibre (muscle);
            }
            else {
               mech.addAxialSpring (muscle);
            }
         }
         lastMkr = mkr;
      }
   }

   public void setDisplacementsFromFile (String fileName) {
      try {
         ReaderTokenizer rtok = ArtisynthIO.newReaderTokenizer (fileName);
         Point3d pos = new Point3d();
         int cnt = 0;
         while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            rtok.pushBack();
            int nnum = rtok.scanInteger();
            double ux = rtok.scanNumber();
            double uy = rtok.scanNumber();
            double uz = rtok.scanNumber();
            FemNode3d node = myFemMod.getNodes().getByNumber (nnum);
            pos.add (node.getRestPosition(), new Vector3d(ux, uy, uz));
            node.setPosition (pos);
            cnt++;
         }
         System.out.println ("read "+cnt+" values");
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
   }


   public void printNodeStresses (String fileName) {
      
      NumberFormat ifmt = new NumberFormat ("%4d");
      NumberFormat ffmt = new NumberFormat ("%10.4f");

      try {
         PrintWriter pw = ArtisynthIO.newIndentingPrintWriter(fileName);
         for (FemNode3d n : myFemMod.getNodes()) {
            pw.println (ifmt.format(n.getNumber())+" "+
                        ffmt.format(n.getVonMisesStress()));
         }
         pw.close();
      }
      catch (Exception e){
         e.printStackTrace();
      }
   }

   public void setNodeStresses (String fileName) {

      try {
         ReaderTokenizer rtok = ArtisynthIO.newReaderTokenizer (fileName);
         int cnt = 0;
         while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            rtok.pushBack();
            int nnum = rtok.scanInteger();
            double vms = rtok.scanNumber();
            myFemMod.getNodes().getByNumber (nnum).setStress (vms);
            cnt++;
         }
         System.out.println ("read "+cnt+" values");
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
   }

   public void writeDisplacementsToFile (String fileName) {
      Vector3d del = new Vector3d();
      try {
         PrintWriter pw = ArtisynthIO.newIndentingPrintWriter (fileName);
         NumberFormat ifmt = new NumberFormat ("%3d");
         for (FemNode3d n : myFemMod.getNodes()) {
            del.sub (n.getPosition(), n.getRestPosition());
            pw.println ("     "+ifmt.format(n.getNumber())+
                        " "+del.toString ("%8.5e"));
         }
         pw.close();
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
      
   }

   public void writeElementJacobiansToFile (String fileName) {
      try {
         PrintWriter pw = ArtisynthIO.newIndentingPrintWriter (fileName);
         NumberFormat ifmt = new NumberFormat ("%3d");
         for (FemElement3d e : myFemMod.getElements()) {
            IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
            IntegrationData3d[] idata = e.getIntegrationData();
            FemNode3d[] nodes = e.getNodes();
            Matrix3d F = new Matrix3d();
            for (int i=0; i<ipnts.length; i++) {
               double detF = 
                  ipnts[i].computeGradient (F, nodes, idata[i].getInvJ0());
               pw.println ("n"+nodes[i].getNumber() + " " + detF);
            }
            pw.println ("");
         }
         pw.close();
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
      
   }

   public void addDisplacementProbes (
      FemNode3d node, MechModel mech, double disp, double time)
      throws IOException {

      String compName = "models/fem/nodes/" + node.getNumber();
      Point3d pos = node.getPosition();
      NumericInputProbe posInput =
         new NumericInputProbe (mech, compName+":position", null);
      posInput.setStopTime (time);
      posInput.loadEmpty();
      posInput.setActive (true);
      addInputProbe (posInput);
      posInput.setData (0);
      posInput.addData (time/2, new Vector3d (pos.x, pos.y, pos.z+disp));
      posInput.setData (time);

      double zvel = disp/(time/2);
      NumericInputProbe velInput =
         new NumericInputProbe (mech, compName+":velocity", null);
      velInput.setStopTime (time);
      velInput.loadEmpty();
      velInput.setActive (true);
      addInputProbe (velInput);
      velInput.setInterpolationOrder (Interpolation.Order.Step);
      velInput.addData (0, new Vector3d (0, 0, zvel));
      velInput.addData (time/2, new Vector3d (0, 0, -zvel));
      velInput.addData (time, new Vector3d (0, 0, 0));

      node.setDynamic (false);
   }

   public void addDisplacementProbes (
      int options, MechModel mech, LinkedList<FemNode3d> nodes,
      double disp, double time) {
      try {
         if ((options & ADD_BLOCKS) != 0) {
            
            RigidBody body = 
               (RigidBody)mech.findComponent ("rigidBodies/right");
            Point3d pos = body.getPosition();
            NumericInputProbe posInput =
               new NumericInputProbe (
                  body, "position", null);
            posInput.setStopTime (time);
            posInput.loadEmpty();
            posInput.setActive (true);
            addInputProbe (posInput);
            posInput.setData (0);
            posInput.addData (time/2, new Vector3d (pos.x, pos.y, pos.z+disp));
            posInput.setData (time);

            double zvel = disp/(time/2);
            NumericInputProbe velInput =
               new NumericInputProbe (
                  body, "velocity", null);
            velInput.setStopTime (5);
            velInput.loadEmpty();
            velInput.setActive (true);
            addInputProbe (velInput);
            velInput.setInterpolationOrder (Interpolation.Order.Step);
            velInput.addData (0, new Twist (0, 0, zvel, 0, 0, 0));
            velInput.addData (time/2, new Twist (0, 0, -zvel, 0, 0, 0));
            velInput.addData (time, new Twist (0, 0, 0, 0, 0, 0));
         }
         else {
            for (FemNode3d n : nodes) {
               addDisplacementProbes (n, mech, disp, time);
            }
         }
         
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }
         
   protected ControlPanel myControlPanel;

   public void addControlPanel (MechModel mechMod, FemModel3d femMod) {

      myControlPanel = new ControlPanel ("options", "LiveUpdate");
      FemControlPanel.addFem3dControls (myControlPanel, femMod, mechMod);
      myControlPanel.addWidget (femMod, "surfaceRendering");
      myControlPanel.addWidget (femMod, "stressPlotRanging");
      myControlPanel.addWidget (femMod, "stressPlotRange");
      if (mechMod.axialSprings().size() > 0) {
         myControlPanel.addWidget (this, "excitation0");
         myControlPanel.addWidget (this, "excitation1");
      }
      else if (femMod instanceof FemMuscleModel) {
         FemMuscleModel tissue = (FemMuscleModel)femMod;
         if (tissue.getMuscleBundles().size() >= 2) {
            myControlPanel.addWidget (femMod, "bundles/0:excitation");
            myControlPanel.addWidget (femMod, "bundles/1:excitation");
         }
      }
            
      addControlPanel (myControlPanel);
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "simple demo of a 3d fem";
   }
}
