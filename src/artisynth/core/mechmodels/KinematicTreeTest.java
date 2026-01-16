package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import artisynth.core.mechmodels.KinematicTree.BodyNode;
import artisynth.core.mechmodels.KinematicTree.JointNode;
import artisynth.core.mechmodels.KinematicTree.TreeNode;
import artisynth.core.modelbase.ComponentUtils;
import maspack.function.LinearFunctionNx1;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.util.ReaderTokenizer;
import maspack.util.TestException;
import maspack.util.UnitTest;

/**
 * Test class for KinematicTree.
 */
class KinematicTreeTest extends UnitTest {

   static void printTreeNode (TreeNode node) {
      if (node instanceof BodyNode) {
         BodyNode bnode = (BodyNode)node;
         System.out.println ("  bodyNode: " + bnode.myBody.getName());
         System.out.print ("    jointNodes:");
         for (JointNode jnode : bnode.myJointNodes) {
            System.out.print (" " + jnode.myConstrainers.get(0).getName());
         }
         System.out.println ("");
         if (bnode.myProximalJoints != null) {
            System.out.println (
               "    proximalJoints: " +
               bnode.myProximalJoints.myConstrainers.get(0).getName());
         }
         else {
            System.out.println ("    proximalJoints: null");
         }
      }
      else { // joint node
         JointNode jnode = (JointNode)node;
         System.out.println (
            "  jointNode: " + jnode.myConstrainers.get(0).getName() +
            " " + jnode.myMinJntNum + " " + jnode.myMinConNum);
         System.out.print ("    constrainers:");
         for (BodyConstrainer bcon : jnode.myConstrainers) {
            System.out.print (" " + bcon.getName());
         }
         System.out.println ("");
         System.out.print ("    bodyNodes:");
         for (BodyNode bnode : jnode.myBodyNodes) {
            System.out.print (" " + bnode.myBody.getName());
         }
         System.out.println ("");
         System.out.print ("    otherBodies:");
         for (ConnectableBody cbod : jnode.getOtherBodies()) {
            System.out.print (" " + cbod.getName());
         }
         System.out.println ("");
         System.out.println (
            "    proximalBody: "+
            (jnode.myProximalBodyNode != null ?
             jnode.myProximalBodyNode.myBody.getName() : "null"));
      }
   }

   private RigidBody addLink (MechModel mech, String name) {
      RigidBody link = RigidBody.createBox (null, 1.0, 0.2, 0.2, /*density*/1000);
      link.setName(name);
      mech.addRigidBody (link);
      return link;
   }

   private HingeJoint createJoint (
      String name, RigidBody bodyA, RigidBody bodyB) {
      HingeJoint joint =
         new HingeJoint (bodyA, bodyB, new RigidTransform3d ());
      joint.setName(name);
      return joint;
   }

   private PlanarConnector createConnector (
      String name, RigidBody bodyA, RigidBody bodyB) {
      PlanarConnector connector =
         new PlanarConnector (
            bodyA, Vector3d.ZERO, bodyB, new RigidTransform3d ());
      connector.setName(name);
      return connector;
   }

   private HingeJoint addJoint (
      MechModel mech, String name, RigidBody bodyA, RigidBody bodyB) {
      HingeJoint joint = createJoint (name, bodyA, bodyB);
      mech.addBodyConnector (joint);
      return joint;
   }

   private PlanarConnector addConstrainer (
      MechModel mech, String name, RigidBody bodyA, RigidBody bodyB) {
      PlanarConnector connector =
         createConnector (name, bodyA, bodyB);
      mech.addConstrainer (connector);
      return connector;
   }

   private JointCoordinateCoupling addCoupling (
      MechModel mech, String name, BodyConnector... joints) {

      ArrayList<JointCoordinateHandle> coords = new ArrayList<>();

      for (BodyConnector bcon : joints) {
         if (!(bcon instanceof JointBase)) {
            throw new TestException (
               "body connector "+bcon.getName()+" is not a joint");
         }
         coords.add (new JointCoordinateHandle ((JointBase)bcon, 0));
      }
      VectorNd coefs = new VectorNd (joints.length);
      coefs.set (0, 1.0);
      for (int i=1; i<coefs.size(); i++) {
         coefs.set (i, 1.0/(coefs.size()-1));
      }
      JointCoordinateCoupling coupling =
         new JointCoordinateCoupling (name, coords, new LinearFunctionNx1(coefs));
      mech.addConstrainer (coupling);
      return coupling;
   }

   private void clearConstrainers (MechModel mech) {
      mech.clearConstrainers();
   }

   private void clearJointsAndConstrainers (MechModel mech) {
      mech.clearBodyConnectors();
      mech.clearConstrainers();
   }

   HashSet<RigidBody> getBodies (MechModel mech, String bodyNames) {
      HashSet<RigidBody> set = new HashSet<>();
      if (bodyNames != null && bodyNames.length() > 0) {
         String[] names = bodyNames.split(" ");
         for (String name : names) {
            set.add (mech.rigidBodies().get(name));
         }
      }
      return set;
   }

   HashSet<JointBase> getJoints (MechModel mech, String jointNames) {
      HashSet<JointBase> set = new HashSet<>();
      if (jointNames != null && jointNames.length() > 0) {
         String[] names = jointNames.split(" ");
         for (String name : names) {
            set.add ((JointBase)mech.bodyConnectors().get(name));
         }
      }
      return set;
   }

   BodyNode createBodyNode (MechModel mech, String name) {
      RigidBody body;
      if (name.equals ("gnd")) {
         body = KinematicTree.Ground;
      }
      else {
         body = (RigidBody)mech.rigidBodies().get (name);
      }
      if (body == null) {
         throw new TestException (
            "test description: can't find body '"+name+"'");
      }
      return new BodyNode (body);
   }

   JointNode createJointNode (MechModel mech, String compNames) {
      JointNode jnode = new JointNode();
      String[] cnames = compNames.split (",");
      for (String cname : cnames) {
         RigidBody body = mech.rigidBodies().get (cname);
         if (body != null) {
            jnode.myBodies.add (body);
            continue;
         }
         BodyConstrainer bcon = mech.bodyConnectors().get (cname);
         if (bcon == null) {
            bcon = (BodyConstrainer)mech.constrainers().get (cname);
         }
         if (bcon != null) {
            jnode.addConstrainer (bcon);
         }
         else {
            throw new TestException (
               "test description: can't find constrainer '"+cname+"'");
         }
      }
      return jnode;
   }

   ArrayList<KinematicTree> parseTrees (MechModel mech, String treeDesc) {
      ArrayList<KinematicTree> trees = new ArrayList<>();
      try {
         ReaderTokenizer rtok =
            new ReaderTokenizer (new StringReader(treeDesc));
         rtok.wordChar (',');
         TreeNode parent = null;
         TreeNode child = null;
         int depth = -1;
         while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            if (rtok.tokenIsWord()) {
               if (depth == -1) {
                  throw new TestException (
                     "tree description: missing '(' to start tree");
               }
               if (parent instanceof BodyNode) {
                  BodyNode bparent = (BodyNode)parent;
                  JointNode jnode = createJointNode (mech, rtok.sval);
                  bparent.addJointNode (jnode);
                  jnode.addBodyNode (bparent);
                  jnode.myBodies.add (bparent.myBody);
                  jnode.myProximalBodyNode = bparent;
                  child = jnode;
               }
               else {
                  BodyNode bnode = createBodyNode (mech, rtok.sval);
                  if (parent != null) {
                     JointNode jparent = (JointNode)parent;
                     jparent.addBodyNode (bnode);
                     jparent.myBodies.add (bnode.myBody);
                     bnode.addJointNode (jparent);
                     bnode.myProximalJoints = jparent;
                  }
                  child = bnode;
               }
            }
            else if (rtok.ttype == '(') {
               if (parent != null && child == null) {
                   throw new TestException (
                      "tree description: parent has no children");
               }
               parent = child;
               child = null;
               depth++;
            }
            else if (rtok.ttype == ')') {
               if (depth == -1) {
                  throw new TestException (
                     "tree description: unexpected ')'");
               }
               if (child == null) {
                   throw new TestException (
                      "tree description: not active child node before ')'");
               }
               if (parent == null) {
                  if (depth != 0) {
                     throw new TestException (
                        "tree description: depth = "+depth+" at top node");
                  }
                  if (!(child instanceof BodyNode)) {
                     throw new TestException (
                        "tree description: top node is not a BodyNode");
                  }
                  KinematicTree tree = new KinematicTree();
                  tree.myRoot = (BodyNode)child;
                  trees.add (tree);
                  child = null;
                  depth = -1;
               }
               else {
                  child = parent;
                  parent = parent.getParent();
                  depth--;
               }
            }
            else if (rtok.ttype == ']') {
               if (depth == -1) {
                  throw new TestException (
                     "tree description: unexpected ']'");
               }
               if (child == null) {
                   throw new TestException (
                      "tree description: no active child node before ']'");
               }
               while (child.getParent() != null) {
                  child = child.getParent();
               }
               if (!(child instanceof BodyNode)) {
                   throw new TestException (
                      "tree description: top node is not a BodyNode");
               }
               KinematicTree tree = new KinematicTree();
               tree.myRoot = (BodyNode)child;
               trees.add (tree);
               child = null;
               parent = null;
               depth = -1;
            }
            else {
               throw new TestException (
                  "tree description: unrecognized token " + (char)rtok.ttype);
            }
         }
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      return trees;
   }

   boolean nodeContentsEqual (TreeNode node0, TreeNode node1) {
      if (node0 instanceof BodyNode) {
         if (!(node1 instanceof BodyNode)) {
            return false;
         }
         BodyNode bnode0 = (BodyNode)node0;
         BodyNode bnode1 = (BodyNode)node1;
         return bnode0.myBody == bnode1.myBody;
      }
      else if (node0 instanceof JointNode) {
         if (!(node1 instanceof JointNode)) {
            return false;
         }
         JointNode jnode0 = (JointNode)node0;
         JointNode jnode1 = (JointNode)node1;
         if (!jnode0.myBodies.equals (jnode1.myBodies)) {
            return false;
         }
         HashSet<BodyConstrainer> cset0 = new HashSet<>();
         HashSet<BodyConstrainer> cset1 = new HashSet<>();
         cset0.addAll (jnode0.myConstrainers);
         cset1.addAll (jnode1.myConstrainers);
         return cset0.equals (cset1);
      }
      else if (node0 == null) {
         return node1 == null;
      }
      else {
         throw new TestException ("Unknown node type "+node0.getClass());
      }
   }

   boolean nodesEqual (
      List<? extends TreeNode> nodes0, 
      List<? extends TreeNode> nodes1) {
      
      if (nodes0.size() != nodes1.size()) {
         return false;
      }
      for (int k=0; k<nodes0.size(); k++) {
         if (!nodeContentsEqual (nodes0.get(k), nodes1.get(k))) {
            return false;
         }
      }
      return true;
   }

   boolean nodesEqual (TreeNode node0, TreeNode node1) {
      if (!nodeContentsEqual (node0, node1)) {
         return false;
      }
      if (node0 instanceof BodyNode) {
         if (!(node1 instanceof BodyNode)) {
            return false;
         }
         BodyNode bnode0 = (BodyNode)node0;
         BodyNode bnode1 = (BodyNode)node1;
         if (!nodeContentsEqual (
                bnode0.myProximalJoints, bnode1.myProximalJoints)) {
            return false;
         }
         return nodesEqual (bnode0.myJointNodes, bnode1.myJointNodes);
      }
      else {
         if (!(node1 instanceof JointNode)) {
            return false;
         }
         JointNode jnode0 = (JointNode)node0;
         JointNode jnode1 = (JointNode)node1;
         if (!nodeContentsEqual (
                jnode0.myProximalBodyNode, jnode1.myProximalBodyNode)) {
            return false;
         }
         return nodesEqual (jnode0.myBodyNodes, jnode1.myBodyNodes);
      }
   }

   void testTrees (MechModel mech, String treeDesc) {
      ArrayList<KinematicTree> checks = parseTrees (mech, treeDesc);

      ArrayList<RigidBody> allBodies = new ArrayList<>();
      ComponentUtils.recursivelyFindComponents (
         RigidBody.class, mech, allBodies);     
      List<KinematicTree> trees = KinematicTree.findTreesForBodies (
         allBodies, mech.constrainers());
      if (trees.size() != checks.size()) {
         throw new TestException (
            trees.size()+" trees detected; expecting "+checks.size());
      }
      for (int k=0; k<trees.size(); k++) {
         ArrayList<TreeNode> treeNodes = trees.get(k).getNodes();
         ArrayList<TreeNode> checkNodes = checks.get(k).getNodes();


         if (treeNodes.size() != checkNodes.size()) {
               System.out.println ("tree "+k+":");
               for (int j=0; j<treeNodes.size(); j++) {
                  printTreeNode (treeNodes.get(j));
               }
            throw new TestException (
               "Tree "+k+": expected "+checkNodes.size()+
               " nodes, got " + treeNodes.size());
         }
         for (int i=0; i<treeNodes.size(); i++) {
            if (!nodesEqual (treeNodes.get(i), checkNodes.get(i))) {
               System.out.println ("tree "+k+":");
               for (int j=0; j<treeNodes.size(); j++) {
                  printTreeNode (treeNodes.get(j));
               }
               System.out.println ("check "+k+":");
               for (int j=0; j<checkNodes.size(); j++) {
                  printTreeNode (checkNodes.get(j));
               }
               throw new TestException (
                  "tree "+k+", node "+i+": node differs from check");
            }
         }
      }
   }

   private RigidBody[] clearAndCreateLinks (MechModel mech, int num) {
      mech.clearRigidBodies();
      RigidBody[] links = new RigidBody[num];
      for (int i=0; i<num; i++) {
         links[i] = addLink (mech, "b"+i);
      }
      return links;
   }

   public void testJointsForBodies() {
      RigidBody[] links = new RigidBody[10];
      BodyConnector[] joints = new BodyConnector[11];
      MechModel mech = new MechModel();

      links = clearAndCreateLinks (mech, 9);
   
      // test with simple ungrounded chain
      //
      // b0 -j1- b1 -j2- b2 -j3- b3 -j4- b4 
      //
      clearJointsAndConstrainers(mech);
      joints[1] = addJoint (mech, "j1", links[1], links[0]);
      joints[2] = addJoint (mech, "j2", links[2], links[1]);
      joints[3] = addJoint (mech, "j3", links[3], links[2]);
      joints[4] = addJoint (mech, "j4", links[4], links[3]);

      testTrees (mech, "(b0 (j1 (b1 (j2 (b2 (j3 (b3 (j4,b4 ]");

      // add a joint coupling
      //
      // b0 -j1- b1 -j2- b2 -j3- b3 -j4- b4
      //      \     /
      //        jc0
      //
      addCoupling (mech, "jc0", joints[1], joints[2]);

      testTrees (mech, "(b0 (j1,b1,j2,jc0 (b2 (j3 (b3 (j4,b4 ]");

      // add two joint couplings
      //
      // b0 -j1- b1 -j2- b2 -j3- b3 -j4- b4
      //      \     /         \      /
      //        jc0             jc1
      //
      clearConstrainers(mech);
      addCoupling (mech, "jc0", joints[1], joints[2]);
      addCoupling (mech, "jc1", joints[3], joints[4]);

      testTrees (mech, "(b0 (j1,b1,j2,jc0 (b2 (j3,b3,j4,jc1,b4 ]");

      // test one long joint coupling
      //
      // b0 -j1- b1 -j2- b2 -j3- b3 -j4- b4
      //      \              /
      //        ---- jc0 ---
      //
      clearConstrainers(mech);
      addCoupling (mech, "jc0", joints[1], joints[3]);

      testTrees (mech, "(b0 (j1,b1,j2,jc0,b2,j3 (b3 (j4,b4 ]");

      // tree chain:
      //
      //             j3- b3
      //            /
      // b0 -j1- b1          j5- b5 -j6- b6
      //            \       /
      //             j2- b2
      //                    \
      //                     j4- b4
      clearJointsAndConstrainers(mech);
      joints[1] = addJoint (mech, "j1", links[1], links[0]);
      joints[2] = addJoint (mech, "j2", links[2], links[1]);
      joints[3] = addJoint (mech, "j3", links[3], links[1]);
      joints[4] = addJoint (mech, "j4", links[4], links[2]);
      joints[5] = addJoint (mech, "j5", links[5], links[2]);
      joints[6] = addJoint (mech, "j6", links[6], links[5]);

      testTrees (mech, "(b0 (j1 (b1 (j2 (b2 (j4,b4) (j5 (b5 (j6,b6))))) (j3,b3]");
      // add a joint coupling
      //
      //             j3- b3
      //            /
      // b0 -j1- b1          j5- b5 -j6- b6
      //      \     \       /
      //        jc0- j2- b2
      //                    \
      //                     j4- b4
      addCoupling (mech, "jc0", joints[2], joints[1]);

      testTrees (
         mech, "(b0 (j1,j2,jc0 (b2 (j4,b4) (j5 (b5 (j6,b6)))) (b1 (j3,b3))))");

      // add another coupling
      //                 -- jc1 --
      //               /           \
      //             j3- b3         \
      //            /                \
      // b0 -j1- b1          j5- b5 -j6- b6
      //      \     \       /
      //        jc0- j2- b2
      //                    \
      //                     j4- b4
      addCoupling (mech, "jc1", joints[6], joints[3]);

      testTrees (
         mech, "(b0 (j1,j2,j3,j5,j6,jc0,jc1,b1,b3,b5,b6 (b2 (j4,b4 ]");

      // graph with loop:
      //
      //             j3- b3 -c5
      //            /          \
      // b0 -j1- b1              b4 -j7- b6
      //            \          /        
      //             j2- b2 -j4
      //                    \
      //                     j6- b5 -j8- b7
      clearJointsAndConstrainers(mech);
      joints[1] = addJoint (mech, "j1", links[1], links[0]);
      joints[2] = addJoint (mech, "j2", links[2], links[1]);
      joints[3] = addJoint (mech, "j3", links[3], links[1]);
      joints[4] = addJoint (mech, "j4", links[4], links[2]);
      joints[5] = addConstrainer (mech, "c5", links[4], links[3]);
      joints[6] = addJoint (mech, "j6", links[5], links[2]);
      joints[7] = addJoint (mech, "j7", links[6], links[4]);
      joints[8] = addJoint (mech, "j8", links[7], links[5]);

      testTrees (
         mech, "(b0 (j1 (b1 (j3,j4,j2,c5,b3 (b2 (j6 (b5 (j8,b7)))) (b4 (j7,b6]");

      // graph with two loops:
      //
      //             j3- b3 -c5
      //            /          \
      // b0 -j1- b1          j4- b4 -j7
      //            \       /          \
      //             j2- b2              b6 -j9- b7
      //                    \          /
      //                     j6- b5 -c8
      clearJointsAndConstrainers(mech);
      joints[1] = addJoint (mech, "j1", links[1], links[0]);
      joints[2] = addJoint (mech, "j2", links[2], links[1]);
      joints[3] = addJoint (mech, "j3", links[3], links[1]);
      joints[4] = addJoint (mech, "j4", links[4], links[2]);
      joints[5] = addConstrainer (mech, "c5", links[4], links[3]);
      joints[6] = addJoint (mech, "j6", links[5], links[2]);
      joints[7] = addJoint (mech, "j7", links[6], links[4]);
      joints[8] = addConstrainer (mech, "c8", links[6], links[5]);
      joints[9] = addJoint (mech, "j9", links[7], links[6]);

      testTrees (
         mech,
         "(b0 (j1 (b1 (j2,j3,j4,j6,j7,c5,c8,b2,b3,b5 (b6 (j9,b7 ]");

      // graph with two serial loops:
      //
      //             
      //             j3- b3 -c5              j7- b6 -c9
      //            /          \            /          \
      // b0 -j1- b1              b4 -j6- b5              b8
      //            \          /            \          /
      //             j2- b2 -j4              j8- b7 -j10
      clearJointsAndConstrainers(mech);
      joints[1] = addJoint (mech, "j1", links[1], links[0]);
      joints[2] = addJoint (mech, "j2", links[2], links[1]);
      joints[3] = addJoint (mech, "j3", links[3], links[1]);
      joints[4] = addJoint (mech, "j4", links[4], links[2]);
      joints[5] = addConstrainer (mech, "c5", links[4], links[3]);
      joints[6] = addJoint (mech, "j6", links[5], links[4]);
      joints[7] = addJoint (mech, "j7", links[6], links[5]);
      joints[8] = addJoint (mech, "j8", links[7], links[5]);
      joints[9] = addConstrainer (mech, "c9", links[8], links[6]);
      joints[10] = addJoint (mech, "j10", links[8], links[7]);

      testTrees (
         mech,
         "(b0 (j1 (b1 (j2,j3,b2,b3,c5,j4 (b4 (j6 (b5 (j8,j7,b6,b7,j10,c9,b8 ]");

      // add couplings
      //
      //             
      //             j3- b3 -c5        -jc0- j7- b6 -c9
      //            /          \      /     /          \
      // b0 -j1- b1              b4 -j6- b5              b8
      //            \          /      \     \          /
      //             j2- b2 -j4        -jc1- j8- b7 -j10
      //
      addCoupling (mech, "jc0", joints[6], joints[7]);
      addCoupling (mech, "jc1", joints[6], joints[8]);

      testTrees (
         mech,
         "(b0 (j1 (b1 (j2,j3,b2,b3,c5,j4 (b4 "+
            "(j6,b5,j8,j7,b6,b7,j10,c9,b8,jc0,jc1 ]");

      // test with simple grounded chain
      //
      // gnd -j0- b0 -j1- b1 -j2- b2
      //
      clearJointsAndConstrainers(mech);
      joints[0] = addJoint (mech, "j0", links[0], null);
      joints[1] = addJoint (mech, "j1", links[1], links[0]);
      joints[2] = addJoint (mech, "j2", links[2], links[1]);

      testTrees (
         mech,
         "(gnd (j0 (b0 (j1 (b1 (j2,b2 ]");

      // add coupling
      //
      // gnd -j0- b0 -j1- b1 -j2- b2
      //       \      |      /
      //        ---- jc0 ----
      addCoupling (mech, "jc0", joints[0], joints[1], joints[2]);

      testTrees (
         mech,
         "(gnd (j0,j1,j2,jc0,b0,b1,b2 ]");

      // test with chain grounded at both ends
      //
      // gnd -j0- b0 -j1- b1 -j2- b2 -j3- gnd
      //
      clearJointsAndConstrainers(mech);
      joints[0] = addJoint (mech, "j0", links[0], null);
      joints[1] = addJoint (mech, "j1", links[1], links[0]);
      joints[2] = addJoint (mech, "j2", links[2], links[1]);
      joints[3] = addJoint (mech, "j3", links[2], null);

      testTrees (
         mech,
         "(gnd (j0,j1,j2,j3,b0,b1,b2))");

      // add coupling
      //
      // gnd -j0- b0 -j1- b1 -j2- b2 -j3- gnd
      //       \      |       /
      //        ---- jc0 ----
      addCoupling (mech, "jc0", joints[0], joints[1], joints[2]);
      testTrees (
         mech,
         "(gnd (j0,j1,j2,j3,b0,b1,b2,jc0))");

      // multiple bodies connected to ground
      //
      // gnd -j0- b0 -j1- b1
      //
      // gnd -j2- b2
      //
      // gnd -j3- b3 -j4- b4
      clearJointsAndConstrainers(mech);
      joints[0] = addJoint (mech, "j0", links[0], null);
      joints[1] = addJoint (mech, "j1", links[1], links[0]);
      joints[2] = addJoint (mech, "j2", links[2], null);
      joints[3] = addJoint (mech, "j3", links[3], null);
      joints[3] = addJoint (mech, "j4", links[4], links[3]);

      testTrees (
         mech,
         "(gnd (j0 (b0 (j1,b1)))) (gnd (j2,b2)) (gnd (j3 (b3 (j4,b4))))");

      // two bodies connected to ground and a free articulation
      //
      // gnd -j0- b0 -j1- b1
      //
      // gnd -j2- b2
      //
      // b3 -j3- b4 -j4- b5
      clearJointsAndConstrainers(mech);
      joints[0] = addJoint (mech, "j0", links[0], null);
      joints[1] = addJoint (mech, "j1", links[1], links[0]);
      joints[2] = addJoint (mech, "j2", links[2], null);
      joints[3] = addJoint (mech, "j3", links[4], links[3]);
      joints[4] = addJoint (mech, "j4", links[5], links[4]);

      testTrees (
         mech,
         "(gnd (j0 (b0 (j1,b1)))) (gnd (j2,b2)) (b3 (j3 (b4 (j4,b5))))");

      // add coupling
      //
      // gnd -j0- b0 -j1- b1
      //       |
      //     jc0
      //       |
      // gnd -j2- b2
      //
      // b3 -j3- b4 -j4- b5
      addCoupling (mech, "jc0", joints[0], joints[2]);

      testTrees (
         mech,
         "(gnd (j0,j2,jc0,b2 (b0 (j1,b1)))) (b3 (j3 (b4 (j4,b5))))");

      // different coupling
      //
      // gnd -j0- b0 -j1- b1
      //      
      // gnd -j2- b2
      //        \
      //         jcx
      //        /    \
      // b3 -j3- b4 -j4- b5
      clearConstrainers(mech);
      addCoupling (mech, "jcx", joints[2], joints[3], joints[4]);

      testTrees (
         mech,
         "(gnd (j0 (b0 (j1,b1)))) (gnd (j2,j3,j4,jcx,b2,b3,b4,b5))");

      // one body connected to ground and two free articulations
      //
      // gnd -j0- b0 -j1- b1
      //
      // b2 -j2- b3
      //
      // b4 -j3- b5 -j4- b6
      clearJointsAndConstrainers(mech);
      joints[0] = addJoint (mech, "j0", links[0], null);
      joints[1] = addJoint (mech, "j1", links[1], links[0]);
      joints[2] = addJoint (mech, "j2", links[3], links[2]);
      joints[3] = addJoint (mech, "j3", links[5], links[4]);
      joints[4] = addJoint (mech, "j4", links[6], links[5]);

      testTrees (
         mech,
         "(gnd (j0 (b0 (j1,b1)))) (b2 (j2,b3)) (b4 (j3 (b5 (j4,b6))))");

      // add some couplingd
      //
      // gnd -j0- b0 -j1- b1
      //
      // b2 -j2- b3
      //      |
      //     jc0
      //      |
      // b4 -j3- b5 -j4- b6

      addCoupling (mech, "jc0", joints[2], joints[3]);

      testTrees (
         mech,
         "(gnd (j0 (b0 (j1,b1)))) (b2 (j2,j3,jc0,b3,b4 (b5 (j4,b6))))");

      //                      
      //                     j3- b3
      //                    / \
      // b0 -j1- b1 -j2- b2   jc1
      //      \     /       \ /
      //       -jc0-         j4- b4
      clearJointsAndConstrainers(mech);
      joints[1] = addJoint (mech, "j1", links[1], links[0]);
      joints[2] = addJoint (mech, "j2", links[2], links[1]);
      joints[3] = addJoint (mech, "j3", links[3], links[2]);
      joints[4] = addJoint (mech, "j4", links[4], links[2]);
      addCoupling (mech, "jc0", joints[1], joints[2]);      
      addCoupling (mech, "jc1", joints[3], joints[4]);      

      testTrees (
         mech,
         "(b0 (j1,j2,jc0,b1 (b2 (j3,jc1,j4,b3,b4))))");
      
   }

   public void test() {
      testJointsForBodies();
   }

   public static void main (String[] args) {
      KinematicTreeTest tester = new KinematicTreeTest();
      
      tester.runtest();
   }

}
