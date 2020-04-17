package artisynth.demos.inverse;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.inverse.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class FrameTargetDemo extends RootModel {

   void addMarkerAndSprings (
      MechModel mech, RigidBody box, double x, double y, double z) {

      FrameMarker mkr = mech.addFrameMarker (box, new Point3d (x, y, z));
      addParticleAndSpring (mech, mkr, x < 0 ? x-1 : x+1, y, z);
      addParticleAndSpring (mech, mkr, x, y < 0 ? y-1 : y+1, z);
      addParticleAndSpring (mech, mkr, x, y, z < 0 ? z-1 : z+1);
   }

   Particle addParticleAndSpring (
      MechModel mech, FrameMarker mkr, double x, double y, double z) {
      Particle p = new Particle (0, x, y, z);
      mech.addParticle (p);
      p.setDynamic (false);
      Muscle spr = new Muscle();
      spr.setMaterial (new SimpleAxialMuscle (10000.0, 0, 100000.0));
      mech.attachAxialSpring (mkr, p, spr);
      return p;
   }

   public static class FrameController extends ControllerBase {

      Frame myFrame;

      public FrameController () {
      }

      public FrameController (Frame frame) {
         myFrame = frame;
      }

      public void apply (double t0, double t1) {
         double period = 3.0;
         double maxAng = Math.toRadians(30);
         double ang = maxAng*Math.sin (2*Math.PI*t0/period);
         myFrame.setOrientation (new AxisAngle (0, 1, 0, ang));
      }

      protected void writeItems (
         PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
         throws IOException {
         
         if (myFrame != null) {
            pw.println (
               "frame="+ComponentUtils.getWritePathName (ancestor,myFrame));
         }
         super.writeItems (pw, fmt, ancestor);
      }
      
      protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
         throws IOException {
         
         rtok.nextToken();
         if (scanAndStoreReference (rtok, "frame", tokens)) {
            return true;
         }
         rtok.pushBack();
         return super.scanItem (rtok, tokens);
      }
      
      protected boolean postscanItem (
         Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
         
         if (postscanAttributeName (tokens, "frame")) {
            myFrame = postscanReference (tokens, Frame.class, ancestor);
            return true;
         }
         return super.postscanItem (tokens, ancestor);
      }
   }


   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      RigidBody box = RigidBody.createBox ("box", 1.0, 1.0, 1.0, 1000.0);
      mech.setFrameDamping (10000.0);
      mech.addRigidBody (box);
      
      addMarkerAndSprings (mech, box, -0.5,-0.5, 0.5);
      addMarkerAndSprings (mech, box,  0.5,-0.5, 0.5);
      addMarkerAndSprings (mech, box,  0.5, 0.5, 0.5);
      addMarkerAndSprings (mech, box, -0.5, 0.5, 0.5);

      addMarkerAndSprings (mech, box, -0.5,-0.5,-0.5);
      addMarkerAndSprings (mech, box,  0.5,-0.5,-0.5);
      addMarkerAndSprings (mech, box,  0.5, 0.5,-0.5);
      addMarkerAndSprings (mech, box, -0.5, 0.5,-0.5);
      
      RenderProps.setSphericalPoints (mech.frameMarkers(), 0.02, Color.WHITE);
      RenderProps.setSphericalPoints (mech.particles(),    0.02, Color.CYAN);
      RenderProps.setSpindleLines (mech.axialSprings(),    0.02, Color.RED);

      // create the tracking controller and add the force minimization terms
      TrackingController controller = new TrackingController(mech, "tcon");
      for (AxialSpring s : mech.axialSprings()) {
         if (s instanceof Muscle) {
            controller.addExciter((Muscle)s);
         }
      }
      controller.addL2RegularizationTerm();
      MotionTargetComponent target = controller.addMotionTarget(box);
      addController (controller);
      
      addController (new FrameController ((Frame)target));

   }
}
