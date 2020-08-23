package artisynth.demos.test;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.inverse.*;
import artisynth.core.gui.*;
import artisynth.core.materials.*;
import artisynth.demos.inverse.PointModel;
import maspack.render.RenderProps;
import maspack.properties.*;
import maspack.matrix.*;


public class MinimizeAxialSpringForce extends PointModel {

   // need this because PointModel sets omitFromMenu=true
   public static boolean omitFromMenu = false;

   ForceEffectorTerm myForceTerm = null;

   static double DEFAULT_STIFFNESS = 1.0;
   static double DEFAULT_DAMPING = 0.001;

   double myStiffness = DEFAULT_STIFFNESS;
   double myDamping = DEFAULT_DAMPING;
   double myMaxF = 10.0;

   public static PropertyList myProps =
      new PropertyList (MinimizeAxialSpringForce.class, PointModel.class);

   static {
      myProps.addReadOnly ("minForce", "minimizable force");
      myProps.add ("stiffness", "stiffness value", DEFAULT_STIFFNESS);
      myProps.add ("damping", "damping value", DEFAULT_DAMPING);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void reinitMembers() {
      TrackingController tcon =
         (TrackingController)getControllers().get(0);
      myForceTerm = tcon.getForceEffectorTerm();
   }

   public void build(String[] args) throws IOException {
      build (DemoType.Point2d);

      for (AxialSpring spr : model.axialSprings()) {
         spr.setMaterial (new SimpleAxialMuscle (myStiffness, myDamping, myMaxF));
      }

      addWayPoint (0.2);
      TrackingController controller =
         (TrackingController)getControllers().get(0);
      myForceTerm = controller.addForceEffectorTerm();
      myForceTerm.addForce (
         model.axialSprings().get("e"), 0.1, /*staticOnly=*/false);
      myForceTerm.debugHf = false;

      addControlPanel();
   }
   
   public void addTrackingController() {
      super.addTrackingController();
//      addMonitor(new ComplianceReporter(model, center));
//      addController(new QuasistaticController(model));

      for (AxialSpring s : model.axialSprings ()) {
         if (s instanceof Muscle) {
            ((Muscle)s).setExcitationColor (Color.RED);
            RenderProps.setLineColor (s, new Color(0, 0, 219));
         }
      }
      model.setMaxColoredExcitation(0.1);
   }

   public VectorNd getMinForce() {
      if (myForceTerm == null) {
         reinitMembers();
      }
      VectorNd minf = new VectorNd(6);
      if (myForceTerm != null) {
         myForceTerm.getForceError (minf);
      }
      return minf;
   }

   public double getStiffness() {
      return myStiffness;
   }

   public void setStiffness (double k) {
      myStiffness = k;
      for (AxialSpring s : model.axialSprings ()) {
         AxialMaterial mat = s.getMaterial();
         if (mat instanceof SimpleAxialMuscle) {
            ((SimpleAxialMuscle)mat).setStiffness (k);
         }
      }      
   }

   public double getDamping() {
      return myDamping;
   }

   public void setDamping (double d) {
      myDamping = d;
      for (AxialSpring s : model.axialSprings ()) {
         AxialMaterial mat = s.getMaterial();
         if (mat instanceof SimpleAxialMuscle) {
            ((SimpleAxialMuscle)mat).setDamping (d);
         }
      }      
   }

   private void addControlPanel() {
      ControlPanel panel = new ControlPanel ("options", "");
      panel.addWidget (model, "integrator");
      panel.addWidget (this, "minForce");
      panel.addWidget ("controllerEnabled", getControllers().get(0), "active");
      panel.addWidget (this, "stiffness");
      panel.addWidget (this, "damping");
      addControlPanel (panel);
   }

   

}
