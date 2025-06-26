package artisynth.demos.tutorial;

import java.io.File;
import java.io.IOException;

import artisynth.core.probes.IKProbe;
import artisynth.core.probes.TRCReader;
import maspack.matrix.VectorNd;

/**
 * Moves the MultiJointedArm around using target points driven by an IKProbe.
 */
public class IKMultiJointedArm extends MultiJointedArm {

   public void build (String[] args) throws IOException {
      super.build(args); // create MultiJointArm

      // read a TRC file containing tracking data for all the markers.
      File trcfile = new File(getSourceRelativePath ("data/multiJointMkrs.trc"));
      TRCReader reader = new TRCReader (trcfile);
      reader.readData();
      
      // determine probe times from TRC file
      double startTime = reader.getFrameTime (0);
      double stopTime = reader.getFrameTime (reader.numFrames()-1);

      // create an IKProbe to drive the bodies from the marker data
      IKProbe ikprobe = new IKProbe (
         "inverseK", myMech, myMech.frameMarkers(),
         /*wgts*/null, startTime, stopTime);

      // load the probe data directly using the TRC data
      VectorNd mtargs = new VectorNd();
      for (int fidx=0; fidx<reader.numFrames(); fidx++) {
         double t = reader.getFrameTime (fidx);
         reader.getMarkerPositions (mtargs, fidx);
         ikprobe.addData (t, mtargs);
      }
      addInputProbe (ikprobe);
      // ensure bodies are non-dynamic when probe is active
      ikprobe.setBodiesNonDynamicIfActive (true);
   }
}
