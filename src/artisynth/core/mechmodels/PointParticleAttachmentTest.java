package artisynth.core.mechmodels;

import java.util.*;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.geometry.*;
import maspack.util.*;
import artisynth.core.modelbase.*;

public class PointParticleAttachmentTest
   extends DynamicAttachmentTestBase<PointParticleAttachment> {

   public void computeSlavePos (VectorNd pos, PointParticleAttachment at) {
      pos.set (at.getParticle().getPosition());
   }
   
   public void computeSlaveVel (VectorNd vel, PointParticleAttachment at) {
      vel.set (at.getParticle().getVelocity());
   }
   
   public void computeMasterForce (
      VectorNd force, int idx, PointParticleAttachment at) {
      force.set (at.getPoint().getForce());
   }

   public PointParticleAttachment createTestAttachment(int idx) {
      Point point = new Point();
      Particle particle = new Particle();
      return new PointParticleAttachment (particle, point);
   }  
   
   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      PointParticleAttachmentTest tester = new PointParticleAttachmentTest();
      tester.runtest();
   }   

}
    
