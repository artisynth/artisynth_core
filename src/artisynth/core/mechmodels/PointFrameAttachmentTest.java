package artisynth.core.mechmodels;

import java.util.*;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.geometry.*;
import maspack.util.*;
import artisynth.core.modelbase.*;

public class PointFrameAttachmentTest
   extends DynamicAttachmentTestBase<PointFrameAttachment> {

   public void computeSlavePos (VectorNd pos, PointFrameAttachment at) {
      Point3d slavePos = new Point3d();
      slavePos.transform (at.getFrame().getPose(), at.getLocation());
      pos.set (slavePos);
   }
   
   public void computeSlaveVel (VectorNd vel, PointFrameAttachment at) {
      Vector3d slaveVel = new Vector3d();
      Twist frameVel = at.getFrame().getVelocity();
      Vector3d locw = new Vector3d();
      locw.transform (at.getFrame().getPose().R, at.getLocation());
      slaveVel.cross (frameVel.w, locw);
      slaveVel.add (frameVel.v);
      vel.set (slaveVel);
   }
   
   public void computeMasterForce (
      VectorNd force, int idx, PointFrameAttachment at) {

      Vector3d pointForce = at.getPoint().getForce();
      Vector3d locw = new Vector3d();
      locw.transform (at.getFrame().getPose().R, at.getLocation());

      Wrench frameForce = new Wrench();
      frameForce.f.set (pointForce);
      frameForce.m.cross (locw, pointForce);
      force.set (frameForce);
   }

   public PointFrameAttachment createTestAttachment(int idx) {
      Point point = new Point();
      Frame frame = new Frame();
      Point3d loc = new Point3d();
      loc.setRandom();
      return new PointFrameAttachment (frame, point, loc);
   }  
   
   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      PointFrameAttachmentTest tester = new PointFrameAttachmentTest();
      tester.runtest();
   }   

}
    
