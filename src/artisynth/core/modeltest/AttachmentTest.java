package artisynth.core.modeltest;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.geometry.GeometryTransformer.UndoState;

import artisynth.core.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.femmodels.*;
import artisynth.core.driver.*;

import artisynth.demos.mech.*;
import artisynth.demos.fem.*;

/**
 * Performs tests related to component attachments.
 */
public class AttachmentTest extends UnitTest {

   public AttachmentTest() {
   }

   int numMasterAttachments (DynamicComponent dcomp) {
      List<DynamicAttachment> masters = dcomp.getMasterAttachments();
      return (masters == null ? 0 : masters.size());
   }

   /**
    * Tests the code by which a DynamicAttachment's back references to the slave
    * and master components are automatically updated depending on the
    * reachability of these components with repsect to the attaching component.
    */
   public void testBackReferences() {
      MechModel mech = new MechModel();

      RigidBody box = RigidBody.createBox ("box", 1.0, 1.0, 1.0, 1000.0);

      FemModel3d fem = FemFactory.createHexGrid (null, 0.5, 0.5, 0.5, 1, 1, 1);
      fem.transformGeometry (new RigidTransform3d (0, 0, -0.75));

      FemNode3d node = fem.getNode(4);

      // attachment added before box or fem model
      
      mech.attachPoint (node, box);
      check ("node.isAttached==true", !node.isAttached());
      check ("box.numMasterAttachments!=0", numMasterAttachments(box)==0);

      // add box
      mech.addRigidBody (box); 
      check ("node.isAttached==true", !node.isAttached());
      check ("box.numMasterAttachments!=1", numMasterAttachments(box)==1);

      // add fem model
      mech.addModel (fem);
      check ("node.isAttached==false", node.isAttached());
      check ("box.numMasterAttachments!=1", numMasterAttachments(box)==1);

      // remove box
      mech.removeRigidBody (box); 
      check ("node.isAttached==false", node.isAttached());
      check ("box.numMasterAttachments!=0", numMasterAttachments(box)==0);

      // remove fem model
      mech.removeModel (fem);
      check ("node.isAttached==true", !node.isAttached());
      check ("box.numMasterAttachments!=0", numMasterAttachments(box)==0);

      // add box
      mech.addRigidBody (box); 
      check ("node.isAttached==true", !node.isAttached());
      check ("box.numMasterAttachments!=1", numMasterAttachments(box)==1);

      // add fem model
      mech.addModel (fem);
      check ("node.isAttached==false", node.isAttached());
      check ("box.numMasterAttachments!=1", numMasterAttachments(box)==1);

      // remove the attachment 
      mech.detachPoint (node);
      check ("node.isAttached==true", !node.isAttached());
      check ("box.numMasterAttachments!=0", numMasterAttachments(box)==0);

      // add the attachment 
      mech.attachPoint (node, box);
      check ("node.isAttached==false", node.isAttached());
      check ("box.numMasterAttachments!=1", numMasterAttachments(box)==1);

      // now do this with a marker

      FrameMarker mkr = new FrameMarker (box, Point3d.ZERO);
      check ("box.numMasterAttachments!=1", numMasterAttachments(box)==1);

      mech.addFrameMarker (mkr);
      check ("box.numMasterAttachments!=2", numMasterAttachments(box)==2);

      // remove box
      mech.removeRigidBody (box); 
      check ("box.numMasterAttachments!=0", numMasterAttachments(box)==0);

      // add box
      mech.addRigidBody (box); 
      check ("box.numMasterAttachments!=2", numMasterAttachments(box)==2);

      // remove and readd the marker

      mech.removeFrameMarker (mkr);
      check ("box.numMasterAttachments!=1", numMasterAttachments(box)==1);
      mech.addFrameMarker (mkr);
      check ("box.numMasterAttachments!=2", numMasterAttachments(box)==2);
      
   }

   MatrixNd getVelocityJacobian (MechModel mech, MotionTargetComponent comp) {
      SparseBlockMatrix J = mech.createVelocityJacobian();
      comp.addTargetJacobian (J, 0);
      mech.reduceVelocityJacobian (J);
      return new MatrixNd (J);
   }

   void checkVelocityJacobian (
      String msg, MechModel mech, MotionTargetComponent comp, MatrixNd Jchk) {
      MatrixNd J = getVelocityJacobian (mech, comp);
      checkEquals (msg, J, Jchk, 1e-14);
   }

   /**
    * Tests the target velocity Jacobians for different components and markers.
    */
   void testVelocityJacobian() {
      MechModel mech = new MechModel();
      MatrixNd J;
      MatrixNd Jchk;

      // 1. FrameMarker, FrameAttachedFrame, and points and frames attached to
      // a box

      // create a box and add a marker at the corner
      RigidBody box = RigidBody.createBox ("box", 3.0, 2.0, 1.0, 1000.0);
      Point3d loc = new Point3d();
      loc.setRandom();
      FrameMarker mkr = mech.addFrameMarkerWorld (box, loc);

      mech.addRigidBody (box);

      // box J should be identity in first block
      Jchk = new MatrixNd (6, 9);
      Jchk.setIdentity();
      checkVelocityJacobian ("box J", mech, box, Jchk);

      // mkr J should be (I -[loc]) in first block
      Jchk = new MatrixNd (3, 9);
      Matrix3d Xloc = new Matrix3d();
      Xloc.setSkewSymmetric (loc);
      Xloc.negate();
      Jchk.setIdentity();
      Jchk.setSubMatrix (0, 3, Xloc);
      checkVelocityJacobian ("frame mkr J", mech, mkr, Jchk);

      // replace the marker with an attached point. J should be the same
      mech.removeFrameMarker (mkr);
      Point pnt = new Point(loc);
      mech.addPoint (pnt);
      mech.attachPoint (pnt, box);
      checkVelocityJacobian ("attaced point J", mech, pnt, Jchk);

      // remove the point, and a Frame that is connected to the body
      mech.detachPoint (pnt);
      mech.removePoint (pnt);
      RigidTransform3d TFW = new RigidTransform3d (loc.x, loc.y, loc.z);
      Frame frame = new Frame (TFW);
      mech.addFrame (frame);
      mech.attachFrame (frame, box);

      // frame J should be (I -[loc] ; 0 I) in first block
      Jchk = new MatrixNd (6, 12);
      Jchk.setIdentity();
      Jchk.setSubMatrix (0, 3, Xloc);
      checkVelocityJacobian ("frame mkr J", mech, frame, Jchk);

      // remove the frame, and add a FrameAttachedFrame.
      mech.detachFrame (frame);
      mech.removeFrame (frame);
      frame = new FrameAttachedFrame (box, TFW);
      mech.addFrame (frame);
      checkVelocityJacobian ("frame attached frame J", mech, frame, Jchk);

      // remove the frame and the box
      mech.removeFrame (frame);
      mech.removeRigidBody (box);

      // 2. FemMarker, FemAttachedFrame, and points and frames attached to a
      // simple hex fem

      // create a one-element hex FEM model and add a marker to it
      FemModel3d fem = FemFactory.createHexGrid (null, 1.0, 1.0, 1.0, 1, 1, 1);
      mech.addModel (fem);
      FemMarker fmkr = fem.addMarker (loc);

      // node i J should be identity in the i-th block
      for (int i=0; i<fem.numNodes(); i++) {
         Matrix3d I = new Matrix3d();
         I.setIdentity();
         // Note:extra 6 columns are for the FEM frame
         Jchk = new MatrixNd (3, 3*(fem.numNodes()+1)+6);
         Jchk.setSubMatrix (0, 3*i, I);
         checkVelocityJacobian ("node i J", mech, fem.getNode(i), Jchk);
      }

      // fmkr J should be identity of the node blocks weight by coordinates
      VectorNd coords = fmkr.getCoordinates();
      Jchk = new MatrixNd (3, 3*(fem.numNodes()+1)+6);
      FemElement3dBase elem = fem.getElement(0);
      FemNode[] nodes = elem.getNodes();
      for (int i=0; i<nodes.length; i++) {
         Matrix3d I = new Matrix3d();
         I.setIdentity();
         I.scale (coords.get(i));
         Jchk.setSubMatrix (0, 3*nodes[i].getSolveIndex(), I);
      }
      checkVelocityJacobian ("fmkr J", mech, fmkr, Jchk);

      // replace the marker with an attached point. J should be the same
      fem.removeMarker (fmkr);
      pnt = new Point(loc);
      mech.addPoint (pnt);
      mech.attachPoint (pnt, fem);
      checkVelocityJacobian ("fem attaced point J", mech, pnt, Jchk);

      // remove the point, and an AttachedFrame to the FEM at loc
      mech.detachPoint (pnt);
      mech.removePoint (pnt);
      TFW = new RigidTransform3d (loc.x, loc.y, loc.z);
      TFW.R.setRandom();
      FemAttachedFrame fframe = new FemAttachedFrame (TFW);
      fem.addAttachedFrame (fframe, fem.getElement(0));

      // fframe J should equal the transpose of the attachment master blocks for
      // the first blocks which correspond to the element nodes.
      Jchk = new MatrixNd (6, 3*(fem.numNodes())+12);
      MatrixBlock[] mblks =
         ((FrameAttachment)fframe.getAttachment()).getMasterBlocks();
      nodes = elem.getNodes();
      for (int i=0; i<nodes.length; i++) {
         Matrix6x3 blk = new Matrix6x3();
         blk.transpose ((Matrix3x6)mblks[i]);
         Jchk.setSubMatrix (0, 3*nodes[i].getSolveIndex(), blk);
      }
      checkVelocityJacobian ("fframe J", mech, fframe, Jchk);

      // remove fframe, and attach a separate frame connected by an
      // attachment. J should be the same.

      fem.removeAttachedFrame (fframe);
      frame = new Frame (TFW);
      mech.addFrame (frame);
      mech.attachFrame (frame, fem);
      mblks = ((FrameAttachment)frame.getAttachment()).getMasterBlocks();

      checkVelocityJacobian ("separate attached frame J", mech, frame, Jchk);

      mech.removeModel (fem);
      mech.detachFrame (frame);
      mech.removeFrame (frame);

      // 3. FemMarker, FemAttachedFrame, and points and frames attached to a
      // quad shell fem

      // create a one-element quad shell model and add a marker to it
      fem = FemFactory.createShellQuadGrid (
         null, 1.0, 1.0, 1, 1, 0.0001, /*membrane*/false);
      loc.z = 0; // make sure we are in the plane
      mech.addModel (fem);
      fmkr = fem.addMarker (loc);

      // node 2*i J should be identity in the i-th block. Even blocks
      // correspond to the back nodes.
      for (int i=0; i<fem.numNodes(); i++) {
         Matrix3d I = new Matrix3d();
         I.setIdentity();
         // Note:extra 6 columns are for the FEM frame
         Jchk = new MatrixNd (3, 3*(2*fem.numNodes()+1)+6);
         Jchk.setSubMatrix (0, 6*i, I);
         checkVelocityJacobian ("shell node i J", mech, fem.getNode(i), Jchk);
      }

      // fmkr J should be identity of the first four even node blocks weight by
      // coordinates
      coords = fmkr.getCoordinates();
      Jchk = new MatrixNd (3, 3*(2*fem.numNodes()+1)+6);
      elem = fem.getShellElement(0);
      nodes = elem.getNodes();
      for (int i=0; i<nodes.length; i++) {
         Matrix3d I = new Matrix3d();
         I.setIdentity();
         I.scale (coords.get(i));
         Jchk.setSubMatrix (0, 3*nodes[i].getSolveIndex(), I);
      }
      checkVelocityJacobian ("fmkr J", mech, fmkr, Jchk);

      // replace the marker with an attached point. J should be the same
      fem.removeMarker (fmkr);
      pnt = new Point(loc);
      mech.addPoint (pnt);
      mech.attachPoint (pnt, fem);
      checkVelocityJacobian ("fem attaced point J", mech, pnt, Jchk);

      // remove the point, and an AttachedFrame to the FEM at loc
      mech.detachPoint (pnt);
      mech.removePoint (pnt);
      TFW = new RigidTransform3d (loc.x, loc.y, 0);
      fframe = new FemAttachedFrame (TFW);
      fem.addAttachedFrame (fframe, fem.getShellElement(0));

      // fframe J should equal the transpose of the attachment master blocks for
      // the first blocks which correspond to the element nodes.
      Jchk = new MatrixNd (6, 6*(fem.numNodes())+12);
      mblks = ((FrameAttachment)fframe.getAttachment()).getMasterBlocks();
      System.out.println ("num blocks=" + mblks.length);
      System.out.println (
         "J=\n" + getVelocityJacobian (mech, fframe).toString("%6.3f"));
      nodes = elem.getNodes();
      for (int i=0; i<nodes.length; i++) {
         Matrix6x3 blk = new Matrix6x3();
         blk.transpose ((Matrix3x6)mblks[i]);
         Jchk.setSubMatrix (0, 3*nodes[i].getSolveIndex(), blk);
      }
      checkVelocityJacobian ("fframe J", mech, fframe, Jchk);

      // remove fframe, and attach a separate frame connected by an
      // attachment. J should be the same.

      fem.removeAttachedFrame (fframe);
      frame = new Frame (TFW);
      mech.addFrame (frame);
      mech.attachFrame (frame, fem);
      mblks = ((FrameAttachment)frame.getAttachment()).getMasterBlocks();

      checkVelocityJacobian ("separate attached frame J", mech, frame, Jchk);
   }

   public void test() {
      testBackReferences();
      testVelocityJacobian();
   }

   public static void main (String[] args) {
      AttachmentTest tester = new AttachmentTest();

      tester.runtest();
   }
}
