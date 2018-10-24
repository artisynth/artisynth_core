package artisynth.core.femmodels;

import java.util.*;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.geometry.*;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

public class NodeFrameNodeAttachmentTest
   extends DynamicAttachmentTestBase<NodeFrameNodeAttachment> {
   
   public void computeSlavePos (VectorNd pos, NodeFrameNodeAttachment at) {
      FrameNode3d frameNode = at.myFrameNode;
      Frame frame = frameNode.myFrame;

      Point3d slavePos = new Point3d();
      slavePos.transform (frame.getPose(), frameNode.getPosition());
      pos.set (slavePos);
   }
   
   public void computeSlaveVel (VectorNd vel, NodeFrameNodeAttachment at) {
      FrameNode3d frameNode = at.myFrameNode;
      Frame frame = frameNode.myFrame;

      Twist frameVel = frame.getVelocity();
      RotationMatrix3d R = frame.getPose().R;

      Vector3d lw = new Vector3d(); // frameNode position rotated to world
      lw.transform (R, frameNode.getPosition());
      
      Vector3d svel = new Vector3d();
      Vector3d tmp = new Vector3d();

      svel.cross (frameVel.w, lw);
      svel.add (frameVel.v);
      tmp.transform (R, frameNode.getVelocity());
      svel.add (tmp);
      vel.set (svel);
   }
   
   public void computeMasterForce (
      VectorNd force, int idx, NodeFrameNodeAttachment at) {
      FrameNode3d frameNode = at.myFrameNode;
      Frame frame = frameNode.myFrame;

      RotationMatrix3d R = frame.getPose().R;
      Vector3d slaveForce = at.myNode.getForce();
      if (idx == 0) {
         Vector3d lw = new Vector3d(); // frameNode position rotated to world
         lw.transform (R, frameNode.getPosition());
         Wrench frameForce = new Wrench();
         frameForce.f.set (slaveForce);
         frameForce.m.cross (lw, slaveForce);
         force.set (frameForce);
      }
      else if (idx == 1) {
         Vector3d frameNodeForce = new Vector3d();
         frameNodeForce.inverseTransform (R, slaveForce);
         force.set (frameNodeForce);
      }
      else {
         throw new IllegalArgumentException (
            "attachment has two masters and so idx should be 0 or 1");
      }
   }

   public NodeFrameNodeAttachment createTestAttachment(int idx) {
      Frame frame = new Frame();
      FemNode3d node = new FemNode3d();
      FrameNode3d frameNode = new FrameNode3d (node, frame);
      return new NodeFrameNodeAttachment (node, frameNode);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      NodeFrameNodeAttachmentTest tester = new NodeFrameNodeAttachmentTest();
      tester.runtest();
   }

}
