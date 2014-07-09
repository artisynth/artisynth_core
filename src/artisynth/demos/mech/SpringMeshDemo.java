package artisynth.demos.mech;

import javax.swing.JFrame;

import maspack.geometry.*;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.util.*;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.SoftPlaneCollider;
import artisynth.core.mechmodels.SolveMatrixTest;
import artisynth.core.modelbase.*;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;

import java.io.*;
import java.awt.Color;

import javax.media.opengl.*;

import maspack.render.*;

//import artisynth.core.mechmodels.DynamicMechComponent.Activity;

public class SpringMeshDemo extends RootModel {
   public static boolean debug = false;

   private boolean pointsAttached = false;

   private boolean collisionEnabled = false;

   private double planeZ = -20;

   private SoftPlaneCollider myCollider;

   public static PropertyList myProps =
      new PropertyList (SpringMeshDemo.class, RootModel.class);

   static {
      myProps.add ("attachment * *", "point constraint enabled", false);
      myProps.add ("collision * *", "plane constraint enabled", false);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setAttachment (boolean enable) {
      if (models().size() > 0) {
         MechModel model = (MechModel)models().getByNumber (0);
         Particle pntA = model.particles().get ("pntA");
         Particle pntB = model.particles().get ("pntB");

         System.out.println ("num particles=" + model.particles().size());
         // make sure model is consistent
         if (pntA.isAttached() && !enable) {
            model.detachPoint (pntA);
         }
         else if (!pntA.isAttached() && enable) {
            model.attachPoint (pntA, pntB);
         }
      }
      pointsAttached = enable;
   }

   public boolean getAttachment() {
      return pointsAttached;
   }

   public void setCollision (boolean enable) {
      if (models().size() > 0) {
         MechModel model = (MechModel)models().getByNumber (0);
         if (model.forceEffectors().size() > 0 && !enable) {
            model.clearForceEffectors();
            rerender();
         }
         else if (model.forceEffectors().size() == 0 && enable) {
            model.addForceEffector (getCollider (model));
            rerender();
         }
      }
   }

   public boolean getCollision() {
      if (models().size() > 0) {
         MechModel model = (MechModel)models().getByNumber (0);
         return model.forceEffectors().size() > 0;
      }
      else {
         return false;
      }
   }

   public SpringMeshDemo() {
      super (null);
   }

   public SoftPlaneCollider getCollider (MechModel model) {
      if (myCollider == null) {
         SoftPlaneCollider collider =
            new SoftPlaneCollider (
               "plane", new Vector3d (0, 0, 1), new Point3d (0, 0, planeZ),
               200, 50);
         collider.addMechModel (model);
         collider.scaleMass (4);
         myCollider = collider;
      }
      return myCollider;
   }

   public SpringMeshDemo (String name) {
      this();
      setName (name);

      MechModel msmod = new MechModel ("msmod");
      // PropertyInfoList list = msmod.getAllPropertyInfo();
      // for (PropertyInfo info : list)
      // { System.out.println (info.getName());
      // }
      msmod.setGravity (0, 0, -9.8);
      //msmod.setGravity (0, 0, 0);
      msmod.setPointDamping (1.0);
      // msmod.setIntegrator (new ForwardEuler());

      // // set view so tha points upwards
      // X.R.setAxisAngle (1, 0, 0, -Math.PI/2);
      // viewer.setTransform (X);

      RenderProps.setPointStyle (msmod, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (msmod, 2);
      RenderProps.setPointColor (msmod, Color.RED);
      RenderProps.setLineRadius (msmod, 0.5);
      RenderProps.setLineStyle (msmod, RenderProps.LineStyle.CYLINDER);

      // PlaneCollider collider =
      // new PlaneCollider("plane",
      // new Plane(0, 0, 1, planeZ), 200, 50);
      // collider.setRenderPosition (new Point3d(0, 0, 0), 25);
      // collider.addMechModel (msmod);

      Material gray = Material.createSpecial (Material.GRAY);
      Material green = Material.createSpecial (Material.GREEN);
      Material red = Material.createSpecial (Material.RED);

      RenderProps props = (new Particle()).createRenderProps();
      props.setPointRadius (2);
      props.setPointStyle (RenderProps.PointStyle.SPHERE);
      props.setPointColor (Color.GREEN);

      Particle p0 = new Particle (5, -10, 0, 20);
      p0.setRenderProps (props);
      p0.setDynamic (false);

      Particle p1 = new Particle (5, 0, 0, 25);
      p1.setRenderProps (props);
      Particle p2 = new Particle (5, 0, 0, 15);
      p2.setRenderProps (props);
      Particle p3 = new Particle (5, 10, 0, 20);
      p3.setRenderProps (props);

      AxialSpring[] springs = new AxialSpring[10];
      for (int i = 0; i < springs.length; i++) {
         springs[i] = new AxialSpring (50, 20, 10);
      }

      msmod.particles().addNumbered (p0, 5);
      msmod.particles().addNumbered (p1, 4);
      msmod.particles().addNumbered (p2, 0);
      msmod.particles().addNumbered (p3, 1);

      // msmod.particles().add (p0);
      // msmod.particles().add (p1);
      // msmod.particles().add (p2);
      // msmod.particles().add (p3);

      msmod.attachAxialSpring (p0, p1, springs[0]);
      msmod.attachAxialSpring (p0, p2, springs[1]);
      msmod.attachAxialSpring (p1, p2, springs[2]);
      msmod.attachAxialSpring (p1, p3, springs[3]);
      msmod.attachAxialSpring (p2, p3, springs[4]);

      Particle p10 = new Particle (5, 10, 0, 20);
      Particle p11 = new Particle (5, 5, 0, 10);
      Particle p12 = new Particle (5, 15, 0, 10);
      Particle p13 = new Particle (5, 10, 0, 0);

      msmod.addParticle (p10);
      msmod.addParticle (p11);
      msmod.addParticle (p12);
      msmod.addParticle (p13);

      // add names to some particles so they can be found for probing and
      // attachment
      p0.setName ("pnt0");
      p3.setName ("pntA");
      p10.setName ("pntB");
      p13.setName ("pnt7");

      msmod.attachAxialSpring (p10, p11, springs[5]);
      msmod.attachAxialSpring (p10, p12, springs[6]);
      msmod.attachAxialSpring (p11, p12, springs[7]);
      msmod.attachAxialSpring (p11, p13, springs[8]);
      msmod.attachAxialSpring (p12, p13, springs[9]);

      // test for bad component insertion:

      // Particle px = new Particle (5, 10, 0, 0);
      // Particle py = new Particle (5, 10, 0, 0);

      // msmod.attachAxialSpring (px, py, new AxialSpring (50, 20, 10));

      msmod.setBounds (new Point3d (0, 0, -25), new Point3d (0, 0, 25));

      addModel (msmod);

      msmod.scaleMass (4);
      setAttachment (true);
      // int numWays = 10;
      // double res = 1;
      // for (int i=0; i<numWays; i++)
      // { Main.getWorkspace().addWayPoint (
      // new WayPoint(TimeBase.secondsToTicks((i+1)*res), true));
      // }
      addControlPanel();
      addProbes (msmod);

      addWayPoint (0.5);
      //addBreakPoint (1.0);
      addWayPoint (1.0);

      addWayPoint (1.5);

      ReferenceList refs = new ReferenceList ("refs");
      refs.addReference (p0);
      refs.addReference (p1);
      refs.addReference (p10);
      refs.addReference (p11);
      refs.addReference (msmod.particles());
      msmod.add (refs);
   }

   private void addControlPanel() {
      myControlPanel = new ControlPanel ("options", "");
      myControlPanel.addWidget (this, "attachment");
      myControlPanel.addWidget (this, "collision");
      myControlPanel.addWidget (this, "models/msmod:integrator");
      myControlPanel.addWidget (this, "models/msmod:maxStepSize");
      addControlPanel (myControlPanel);
   }

   ControlPanel myControlPanel;

   GLViewer myViewer;

   public void addProbes (MechModel mech) {
      try {
         NumericInputProbe inprobe =
            new NumericInputProbe (
               mech, "particles/pnt0:targetPosition",
               ArtisynthPath.getSrcRelativePath (this, "springMeshIn.txt"));
         // inprobe.setDefaultDisplayRange (-10, 10);
         inprobe.setStopTime (10);

//          NumericInputProbe inprobe =
//             new NumericInputProbe (
//                mech, "particles/0" + sep + "targetPosition", 0, 10);
//          inprobe.addData (
//             new double[] { 0, 2, -2, 0, 5, 2, -2, 0, 10, 2, -2, 0 },
//             NumericInputProbe.EXPLICIT_TIME);

         addInputProbe (inprobe);

         NumericOutputProbe collector =
            new NumericOutputProbe (
               mech, "particles/pnt7:position",
               ArtisynthPath.getSrcRelativePath (this, "springMeshOut.txt"),
               0.01);
         collector.setDefaultDisplayRange (-40, 20);
         collector.setStopTime (10);
         addOutputProbe (collector);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   public void attach (DriverInterface driver) {
      // System.out.println();

      // String dir = ArtisynthPath.getSrcPath (this) + File.separator;
      // System.out.println ("path=" + dir);

      //MechModel model = (MechModel)models().getByNumber (0);

      setAttachment (getAttachment());
      setCollision (getCollision());

      //      addTracingProbe (model.particles().get (7), "position", 2, 4);

      if (myControlPanel != null) {
         myControlPanel.pack();
         myControlPanel.setVisible (true);
         java.awt.Point loc = driver.getFrame().getLocation();
         myControlPanel.setLocation (
            loc.x + driver.getFrame().getWidth(), loc.y);
      }

   }

   public void detach (DriverInterface driver) {
      super.detach (driver);
      // if (myControlPanel != null)
      // {
      // myControlPanel.setVisible(false);
      // myControlPanel = null;
      // }

      System.out.println ("Cleaning up Spring mesh");
   }

   // public Property getProperty (String name)
   // {
   // Property prop = super.getProperty (name);
   // if (prop == null && name.indexOf ('/') == -1)
   // { return myProps.getProperty (name, this);
   // }
   // else
   // { return prop;
   // }
   // }

   public StepAdjustment advance (double t0, double t1, int flags) {
      // MechModel model = (MechModel)models().getByNumber (0);
      // SolveMatrixTest tester = new SolveMatrixTest();
      // System.out.println ("error=" + tester.testStiffness (model, 1e-8));

      return super.advance (t0, t1, flags);
   }


   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return artisynth.core.util.TextFromFile.getTextOrError (
         ArtisynthPath.getSrcRelativeFile (this, "SpringMeshDemo.txt"));
   }

}
