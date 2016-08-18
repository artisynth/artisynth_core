package artisynth.demos.mech;

import java.awt.Color;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.probes.*;
import artisynth.core.util.*;
import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer.LineStyle;
import maspack.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

// Layman demo with puppet strings attached

public class PuppetDemo extends LaymanDemo {

   public void build (String[] args) throws IOException {
      super.build (args);

      RigidBody plate = RigidBody.createBox ("plate", 1, 1, 0.1, 1000);

      FrameMarker myRPlateMkr = new FrameMarker();
      FrameMarker myLPlateMkr = new FrameMarker();
      FrameMarker myRHandMkr = new FrameMarker();
      FrameMarker myLHandMkr = new FrameMarker();

      plate.setPose (0, 0, 1, 0, 0, 0);

      myMechMod.addRigidBody (plate);
      myMechMod.addFrameMarker (
         myRPlateMkr, plate, new Point3d (-0.4, 0, -0.05));
      myMechMod.addFrameMarker (myLPlateMkr, plate, new Point3d (0.4, 0, -0.05));
      myMechMod.addFrameMarker (
         myRHandMkr, myLayman.myRHand, new Point3d (0, 0, -0.00));
      myMechMod.addFrameMarker (
         myLHandMkr, myLayman.myLHand, new Point3d (0, 0, -0.00));

      myMechMod.attachAxialSpring (myRPlateMkr, myRHandMkr, new AxialSpring (
         150, 0, 0));
      myMechMod.attachAxialSpring (myLPlateMkr, myLHandMkr, new AxialSpring (
         150, 0, 0));
      
      setRenderProps (myLayman.getRenderProps());
      RenderProps.setPointRadius (this, 0.035);

      plate.setDynamic (false);

      myMechMod.setMaxStepSize (0.002);
      Probe probe = addTracingProbe (myLHandMkr, "position", 0, 10);
      probe.setActive (false);

      NumericInputProbe iprobe =
         new NumericInputProbe (myMechMod, "rigidBodies/plate:position", 0, 10);
      iprobe.addData (
         new double[] { 0, 0, 0, 1, 1, 0, 0, 1, 2.5, 0, 2.5, 1.65, 4, 0, 0,
                       1.2, 5.5, -0.5, -2.5, 1.2, 7, 0, 1, 1, 9, 0, 0, 1 },
         NumericInputProbe.EXPLICIT_TIME);

      iprobe.setActive (true);
      addInputProbe (iprobe);
   }
}
