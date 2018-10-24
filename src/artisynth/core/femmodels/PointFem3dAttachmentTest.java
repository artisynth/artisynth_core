package artisynth.core.femmodels;

import java.util.*;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.geometry.*;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

public class PointFem3dAttachmentTest
   extends DynamicAttachmentTestBase<PointFem3dAttachment> {

   public void computeSlavePos (VectorNd pos, PointFem3dAttachment at) {
      VectorNd coords = at.getCoordinates();
      FemNode[] nodes = at.getNodes();
      Point3d slavePos = new Point3d();      
      for (int i=0; i<nodes.length; i++) {
         slavePos.scaledAdd (coords.get(i), nodes[i].getPosition());
      }
      pos.set (slavePos);
   }
   
   public void computeSlaveVel (VectorNd vel, PointFem3dAttachment at) {
      VectorNd coords = at.getCoordinates();
      FemNode[] nodes = at.getNodes();

      Vector3d slaveVel = new Vector3d();      
      for (int i=0; i<nodes.length; i++) {
         slaveVel.scaledAdd (coords.get(i), nodes[i].getVelocity());
      }
      vel.set (slaveVel);
   }
   
   public void computeMasterForce (
      VectorNd force, int idx, PointFem3dAttachment at) {
      VectorNd coords = at.getCoordinates();

      Vector3d slaveForce = at.getPoint().getForce();
      int nm = at.numMasters();
      if (idx < 0 || idx >= coords.size()) {
         throw new IllegalArgumentException (
            "attachment has "+nm+" masters but idx=" + idx);
      }
      Vector3d nodeForce = new Vector3d();
      nodeForce.scale (coords.get(idx), slaveForce);
      force.set (nodeForce);
   }

   public PointFem3dAttachment createTestAttachment(int idx) {
      Point point = new Point();
      int nnodes = 4;
      FemNode3d[] nodes = new FemNode3d[nnodes];
      for (int i=0; i<nodes.length; i++) {
         nodes[i] = new FemNode3d();
      }
      double[] coords = new double[] { 0.1, 0.2, 0.55, 0.15 };
      return new PointFem3dAttachment (point, nodes, coords);
   }  
   
   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      PointFem3dAttachmentTest tester = new PointFem3dAttachmentTest();
      tester.runtest();
   }   

}
    
