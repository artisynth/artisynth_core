package artisynth.core.femmodels;

import java.util.*;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.geometry.*;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

public class DirectorFrameAttachmentTest
   extends DynamicAttachmentTestBase<DirectorFrameAttachment> {
   
   public void computeSlavePos (VectorNd pos, DirectorFrameAttachment at) {
      Frame frame = at.getFrame();

      Point3d pw = new Point3d();
      Vector3d dw = new Vector3d();
      frame.computePointPosition (pw, at.myLoc);
      RigidTransform3d TFL0 = new RigidTransform3d();
      TFL0.p.set (at.myLoc);
      RigidTransform3d TFW = new RigidTransform3d();
      frame.computeFramePosition (TFW, TFL0);
      dw.transform (TFW.R, at.myLocDir);

      pw.sub (dw);
      pos.set (pw);
   }
   
   public void computeSlaveVel (VectorNd vel, DirectorFrameAttachment at) {
      Frame frame = at.getFrame();

      Vector3d pv = new Vector3d();

      RigidTransform3d TFL0 = new RigidTransform3d();
      TFL0.p.set (at.myLoc);
      RigidTransform3d TFW = new RigidTransform3d();
      Twist fvel = new Twist();
      frame.computeFramePosVel (TFW, fvel, null, null, TFL0);
      frame.computePointVelocity (pv, at.myLoc);
      Vector3d dw = new Vector3d();
      dw.transform (TFW.R, at.myLocDir);
      Vector3d svel = new Vector3d();
      svel.crossAdd (dw, fvel.w, pv);
      vel.set (svel);
   }
   
   public int numTestAttachments () {
      return 2;
   }        

   public DirectorFrameAttachment createTestAttachment (int num) {
      Frame frame;
      FemNode3d node = new FemNode3d();
      Point3d loc = new Point3d();
      Vector3d dir = new Vector3d();
      
      loc.setRandom();
      dir.setRandom();
      dir.normalize();
      dir.scale (0.01);
      
      node.setDirectorActive (true);
      node.setPosition (loc);
      node.setDirector (dir);
      
      if (num == 0) {
         frame = new Frame();
      }
      else {
         frame = new EBBeamBody (2.0, /*rad=*/0.1, /*density=*/1.0, 10000);
      }
      return new DirectorFrameAttachment (node, frame);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      DirectorFrameAttachmentTest tester = new DirectorFrameAttachmentTest();
      tester.runtest();
   }

}
