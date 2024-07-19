package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;
import artisynth.core.inverse.*;
import artisynth.core.gui.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.geometry.*;
import maspack.interpolation.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

/**
 * Generates tracking target data for InverseMuscleFem
 */
public class InverseMuscleFemGen extends ToyMuscleFem {

   protected String dataDir = PathFinder.getSourceRelativePath (this, "data/");   

   public void build (String[] args) throws IOException {
      super.build (args);  // build the underlying ToyMuscleFem model

      double startTime = 0; // probe start times
      double stopTime = 5;  // probe stop times

      NumericOutputProbe outprobe;
      NumericInputProbe inprobe;

      Property[] exprops = new Property[] {
         myFem.getMuscleBundles().get(2).getProperty ("excitation"),
         myFem.getMuscleBundles().get(6).getProperty ("excitation"),
      };
      NumericInputProbe exprobe = new NumericInputProbe (exprops, this);
      exprobe.setStartTime (startTime);
      exprobe.setStopTime (stopTime);
      exprobe.setName ("input excitations");
      exprobe.addData (new double[] {
            0,0,0,  1,0.6,0.1, 2,0.8,0.6, 3,0.8,0.75, 4,0.8,0.75, 5,0.8,0.75
         },
         NumericProbeBase.EXPLICIT_TIME);
      exprobe.setInterpolationOrder (Interpolation.Order.Cubic);
      addInputProbe (exprobe);

      NumericOutputProbe framePos =
         new NumericOutputProbe (myFrame,"position", startTime, stopTime, 0.2);
      framePos.setAttachedFileName (dataDir+"inverseFemFramePos.txt");
      framePos.setInterpolationOrder (Interpolation.Order.Cubic);
      framePos.setName ("frame position");
      addOutputProbe (framePos);

      NumericOutputProbe frameRot =
         new NumericOutputProbe (myFrame,"orientation", startTime, stopTime, 0.2);
      frameRot.setName ("frame orientation");
      frameRot.setAttachedFileName (dataDir+"inverseFemFrameRot.txt");
      frameRot.setInterpolationOrder (Interpolation.Order.SphericalLinear);
      addOutputProbe (frameRot);
   }

}
