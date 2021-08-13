package artisynth.demos.test;

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
 * Tests the code by which a DynamicAttachment's back references to the slave
 * and master components are automatically updated depending on the
 * reachability of these components with repsect to the attaching component.
 */
public class AttachmentReferenceTest extends UnitTest {

   public AttachmentReferenceTest() {
   }

   int numMasterAttachments (DynamicComponent dcomp) {
      List<DynamicAttachment> masters = dcomp.getMasterAttachments();
      return (masters == null ? 0 : masters.size());
   }

   public void test() {
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

   public static void main (String[] args) {
      AttachmentReferenceTest tester = new AttachmentReferenceTest();

      tester.runtest();
   }
}
