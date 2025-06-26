package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.AxialMaterial;
import artisynth.core.materials.LinearAxialMaterial;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Point;
import artisynth.core.probes.PositionInputProbe;
import artisynth.core.probes.TRCReader;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;

/**
 * Moves the MultiJointedArm around using target points attached to markers by
 * springs, with the target positions driven by a probe derived from a TRC file
 */
public class TRCMultiJointedArm extends MultiJointedArm {

   public double myTrackingStiffness = 5000; // for tracking springs
   public final double startTime = 0; // probe start time
   public final double stopTime = 3;  // probe stop time

   // define a 'trackingStiffness' property for this model that manages stiffness
   // for all tracking springs at once
   public static PropertyList myProps =
      new PropertyList (TRCMultiJointedArm.class, MultiJointedArm.class);

   static {
      myProps.add (
         "trackingStiffness",
         "stiffness used to track marker targets", 5000);
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   // get() accessor for tracking stiffness property
   public double getTrackingStiffness() {
      return myTrackingStiffness;
   }

   // set() accessor for tracking stiffness property
   public void setTrackingStiffness (double stiffness) {
      myTrackingStiffness = stiffness;
      for (AxialSpring spr : myMech.axialSprings()) {
         AxialMaterial mat = spr.getMaterial();
         if (mat instanceof LinearAxialMaterial) {
            ((LinearAxialMaterial)mat).setStiffness (stiffness);
         }
      }
   }

   public void build (String[] args) throws IOException {
      super.build(args); // create MultiJointArm

      // For each marker, create a target point and attach it to the marker
      // using a spring
      for (FrameMarker mkr : myMech.frameMarkers()) {
         Point point = new Point (/*name*/null, mkr.getPosition());
         myMech.addPoint (point);
         // create a spring between the target point and the marker
         AxialSpring spr = new AxialSpring (
            myTrackingStiffness, /*damping*/0, /*restLength*/0);
         myMech.attachAxialSpring (point, mkr, spr);
      }

      // read a TRC file containing tracking data for all the markers.
      File trcfile = new File(getSourceRelativePath ("data/multiJointMkrs.trc"));
      TRCReader reader = new TRCReader (trcfile);
      reader.readData();
      // use this to create an input probe to drive the target postions
      PositionInputProbe trcprobe = reader.createInputProbe (
         "trc targets", myMech.points(), 
         /*useTargetProps*/true, startTime, stopTime);
      addInputProbe (trcprobe);

      // create a control panel to adjust the tracking stiffness
      ControlPanel panel = new ControlPanel();
      panel.addWidget (this, "trackingStiffness");
      addControlPanel (panel);

      // render properties: target points cyan, springs as red spindles
      RenderProps.setPointColor (myMech.points(), Color.CYAN);
      RenderProps.setSpindleLines (
         myMech.axialSprings(), 0.01, Color.RED);
   }

   public void postscanInitialize() {
      // sets 'myMech' if model is read from a .art file
      myMech = (MechModel)findComponent ("models/mech");
   }
}
