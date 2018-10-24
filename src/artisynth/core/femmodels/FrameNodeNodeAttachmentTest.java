package artisynth.core.femmodels;

import java.util.*;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.geometry.*;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

public class FrameNodeNodeAttachmentTest 
   extends DynamicAttachmentTestBase<FrameNodeNodeAttachment> {
   
   public void computeSlavePos (VectorNd pos, FrameNodeNodeAttachment at) {
      Point3d slavePos = new Point3d();
      slavePos.inverseTransform (at.myFrame.getPose(), at.myNode.getPosition());
      pos.set (slavePos);
   }
   
   public void computeSlaveVel (VectorNd vel, FrameNodeNodeAttachment at) {
      Twist frameVel = at.myFrame.getVelocity();
      RotationMatrix3d R = at.myFrame.getPose().R;

      Vector3d lw = new Vector3d(); // frameNode position rotated to world
      lw.transform (R, at.myFrameNode.getPosition());
      
      Vector3d svel = new Vector3d();
      svel.cross (lw, frameVel.w);
      svel.add (at.myNode.getVelocity());
      svel.sub (frameVel.v);
      svel.inverseTransform (R);
      vel.set (svel);
   }
   
   public void computeMasterForce (
      VectorNd force, int idx, FrameNodeNodeAttachment at) {
      RotationMatrix3d R = at.myFrame.getPose().R;
      Vector3d slaveForce = at.myFrameNode.getForce();
      if (idx == 0) {
         Wrench frameForce = new Wrench();
         frameForce.f.set (slaveForce);
         frameForce.m.cross (at.myFrameNode.getPosition(), slaveForce);
         frameForce.negate();
         frameForce.transform (R);
         force.set (frameForce);
      }
      else if (idx == 1) {
         Vector3d nodeForce = new Vector3d();
         nodeForce.transform (R, slaveForce);
         force.set (nodeForce);
      }
      else {
         throw new IllegalArgumentException (
            "attachment has two masters and so idx should be 0 or 1");
      }
   }

   public FrameNodeNodeAttachment createTestAttachment(int idx) {
      Frame frame = new Frame();
      FemNode3d node = new FemNode3d();
      FrameNode3d frameNode = new FrameNode3d (node, frame);
      return new FrameNodeNodeAttachment (frameNode, node);      
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      FrameNodeNodeAttachmentTest tester = new FrameNodeNodeAttachmentTest();
      tester.runtest();
   }

}
