package artisynth.core.mechmodels;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import maspack.matrix.RigidTransform3d;
import maspack.util.IntHolder;
import maspack.util.InternalErrorException;

/**
 * Models an articulated structure as a bipartite tree consisting of
 * <i>body</i> and <i>joint</i> nodes, with a body node as the root.  Body
 * nodes have joint nodes as their parents and children and joint nodes have
 * body nodes as their parents and children.
 *
 * <p>Each body node corresponds to a single {@link ConnectableBody} which is
 * also a <i>cut vertex</i> of a graph in which the bodies are vertices and the
 * constrainers connecting them are edges. Each joint node corresponds to the
 * sub-graph of bodies and constrainers separated by the body nodes (i.e., cut
 * vertices). This arrangement means that setting coordinates values for the
 * joints within a joint node can be done independently of the constrainers in
 * the other joint nodes, which allows the setting of coordinate values for
 * different joints in the tree to be done in a computationally efficient way.
 *
 * <p>In the simplest case, a joint node contains a single joint or constraint
 * and will then have a single child node and connect the two bodies associated
 * with its parent and child nodes. The joint's coordinates, if present, will
 * uniquely determine the pose of the child body relative to the parent. In
 * more complex cases, a joint node may contain multiple joints and/or
 * constraints and joint coordinates within the node may not be independent. An
 * iterative solve procedure must then be used to determine the coordinate
 * values that result from setting one or more coordinates within the
 * node. This procedure will also determine the poses of the child bodies with
 * respect to the parent.
 *
 * <p>Where appropriate, the tree's root node is set to represent the
 * structures least mobile body. This will be ground if the structure is
 * actually connected to ground; otherwise, other rules are used to determined
 * the root node as specified in the documentation for {@link #findRootNode}.
 */
public class KinematicTree {

   /**
    * Special rigid body instance representing ground.
    */
   public static final RigidBody Ground = new RigidBody("gnd");

   protected BodyNode myRoot;

   public static boolean debug; // debug field - internal use only

   static {
      Ground.setNumber(-1);
   }

   /**
    * Base class for the tree nodes.
    */
   static abstract class TreeNode {

      /**
       * Returns the node's parent, or {@code null} if the node is a root,
       */
      abstract TreeNode getParent();

      /**
       * Returns the node's children.
       */
      abstract List<? extends TreeNode> getChildren();

      /**
       * Returns the number of node children.
       */
      abstract int numChildren();

      /**
       * Traverses ancestors of this node to find the tree's root.
       */
      abstract BodyNode getRoot();

      /**
       * Recursively finds all descendant nodes of this node, including itself.
       */
      void getNodes (List<TreeNode> nodes) {
         nodes.add (this);
         for (TreeNode child : getChildren()) {
            child.getNodes(nodes);
         }
      }
   }

   /**
    * Implements a body node.
    */
   static class BodyNode extends TreeNode {
      ConnectableBody myBody;     // body associated with the node
      JointNode myProximalJoints; // parent joint node, or null
      // all joint nodes connected to this node, *including* the parent
      ArrayList<JointNode> myJointNodes = new ArrayList<>();

      /**
       * Creates a BodyNode for a specific connectable body.
       */
      BodyNode (ConnectableBody cbod) {
         myBody = cbod;
         myJointNodes = new ArrayList<>();
      }

      /**
       * Returns true if the node's body is the 'B' body for all
       * body connectors attached to it.
       */
      boolean allConnectorsAreOutward() {
         for (BodyConnector bcon : myBody.getConnectors()) {
            if (bcon.getBodyA() == myBody) {
               return false;
            }
         }
         return true;
      }

      /**
       * Returns the number of body connectors that are attached to
       * the node's body.
       */
      int numConnectors() {
         return myBody.getConnectors().size();
      }

      /**
       * Returns the number of "outward" connectors attached to the body,
       * defined as a body connector for which the body is the connector's B
       * body.
       */
      int numOutwardConnectors() {
         int num = 0;
         for (BodyConnector bcon : myBody.getConnectors()) {
            if (bcon.getBodyA() != myBody) {
               num++;
            }
         }
         return num;
      }

      /**
       * Returns a string respresentation of the node for testing and
       * debugging.
       */
      public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append ("bodyNode: " + myBody.getName());
         sb.append (" jointNodes:");
         for (JointNode jnode : myJointNodes) {
            sb.append (String.format (" %2d ", jnode.getNumber()));
         }
         if (myProximalJoints != null) {
            sb.append (
               String.format (
                  " proximalJoints: %2d ", myProximalJoints.getNumber()));
         }
         else {
            sb.append (" proximalJoints: null");
         }
         return sb.toString();
      }

      /**
       * Returns a body number for this node, to help identify it. Note that
       * this will not necessarily be unique when both rigid bodies and FE
       * models are present in a kinematic structure.
       */
      public int getNumber() {
         return myBody.getNumber();
      }

      /**
       * Connects a joint node to this body node.
       */
      void addJointNode (JointNode node) {
         int idx;
         // add nodes in order of increasing body number, to try and keep the
         // tree structure unique.
         for (idx=0; idx<myJointNodes.size(); idx++) {
            if (node.getNumber() < myJointNodes.get(idx).getNumber()) {
               break;
            }
         }
         myJointNodes.add (idx, node);
      }

      /**
       * {@InheritDoc}
       */
      JointNode getParent() {
         return myProximalJoints;
      }

      /**
       * {@InheritDoc}
       */
      BodyNode getRoot() {
         BodyNode bnode = this;
         JointNode jnode;
         while ((jnode=bnode.getParent()) != null) {
            if ((bnode=jnode.getParent()) == null) {
               return null;
            }
         }
         return bnode;
      }

      /**
       * {@InheritDoc}
       */
      List<JointNode> getChildren() {
         ArrayList<JointNode> children = new ArrayList<>(myJointNodes.size());
         for (JointNode jnode : myJointNodes) {
            if (jnode != myProximalJoints) {
               children.add (jnode);
            }
         }
         return children;
      }      

      /**
       * {@InheritDoc}
       */
      int numChildren() {
         int numc = myJointNodes.size();
         if (myProximalJoints != null) {
            numc--;
         }
         return numc;
      }

      /**
       * Recursively get all joint nodes which are descendants of this body
       * node.
       */
      void getJointNodes (List<JointNode> nodes) {
         for (JointNode jnode : myJointNodes) {
            if (jnode != myProximalJoints) {
               nodes.add (jnode);
               for (BodyNode bnode : jnode.myBodyNodes) {
                  if (bnode != jnode.myProximalBodyNode) {
                     bnode.getJointNodes (nodes);
                  }
               }
            }
         }        
      }
   }

   /**
    * Returns the outward connector surplus for a body. An outward connector is
    * a body connector for which the body is the B body.
    */
   static int outwardConnectorSurplus (ConnectableBody cbod) {
      int numOutward = 0;
      for (BodyConnector bcon : cbod.getConnectors()) {
         if (bcon.getBodyA() != cbod) {
            numOutward++;
         }
      }
      return 2*numOutward - cbod.getConnectors().size();
   }

   /**
    * Compares two bodies by their component number.
    */
   static class BodyComparator implements Comparator<ConnectableBody> {
      public int compare (ConnectableBody cbod0, ConnectableBody cbod1) {
         int num0 = cbod0.getNumber();
         int num1 = cbod1.getNumber();
         return (num0 < num1 ? -1 : (num0 > num1 ? 1 : 0));
      }
   }

   /**
    * Compares two body constrainers by their component number,
    * with instances of {@link BodyConnector} coming first.
    */
   static class ConstrainerComparator implements Comparator<BodyConstrainer> {
      public int compare (BodyConstrainer bcon0, BodyConstrainer bcon1) {
         boolean isConnector0 = (bcon0 instanceof BodyConnector);
         boolean isConnector1 = (bcon1 instanceof BodyConnector);
         if (isConnector0 != isConnector1) {
            return isConnector0 ? -1 : 1;
         }
         else {
            int num0 = bcon0.getNumber();
            int num1 = bcon1.getNumber();
            return (num0 < num1 ? -1 : (num0 > num1 ? 1 : 0));
         }
      }
   }

   /**
    * Implements a joint node.
    */
   static class JointNode extends TreeNode {
      BodyNode myProximalBodyNode;
      ArrayList<BodyNode> myBodyNodes = new ArrayList<>();
      HashSet<ConnectableBody> myBodies;
      ArrayList<BodyConstrainer> myConstrainers; 
      int myNumJoints = 0;
      int myMinJntNum = -1;
      int myMinConNum = -1;

      /**
       * Creates an empty joint node.
       */
      JointNode() {
         myBodyNodes = new ArrayList<>();
         myBodies = new LinkedHashSet<>();
         myConstrainers = new ArrayList<>();
      }

      /**
       * Connects a body node to this joint node.
       */
      void addBodyNode (BodyNode node) {
         int idx;
         // add nodes in order of increasing body number, to try and keep the
         // tree structure unique.
         for (idx=0; idx<myBodyNodes.size(); idx++) {
            if (node.getNumber() < myBodyNodes.get(idx).getNumber()) {
               break;
            }
         }
         myBodyNodes.add (idx, node);
      }

      /**
       * Adds a constrainer to this joint node.
       */
      void addConstrainer (BodyConstrainer bcon) {
         if (bcon instanceof JointBase) {
            int idx = 0;
            while (idx < myConstrainers.size() &&
                   myConstrainers.get(idx) instanceof JointBase) {
               idx++;
            }
            myConstrainers.add (idx, bcon);
            myNumJoints++;
         }
         else {
            myConstrainers.add ( bcon);
         }
         if (bcon instanceof JointBase) {
            if (myMinJntNum == -1 || bcon.getNumber() < myMinJntNum) {
               myMinJntNum = bcon.getNumber();
            }
         }
         else {
            if (myMinConNum == -1 || bcon.getNumber() < myMinConNum) {
               myMinConNum = bcon.getNumber();
            }
         }
         for (ConnectableBody cbod : bcon.getBodies()) {
            cbod = nullBodyToGround (cbod);
            myBodies.add (cbod);
         }
      }

      /**
       * Sorts the bodies and constrainers of this joint node, using their
       * component numbers, to make it more likely that they will appear in a
       * unique order.
       */
      void sortBodiesAndConstrainers() {
         ArrayList<ConnectableBody> bodies = new ArrayList<>();
         bodies.addAll (myBodies);
         Collections.sort (bodies, new BodyComparator());
         myBodies.clear();
         myBodies.addAll (bodies);
         Collections.sort (myConstrainers, new ConstrainerComparator());
      }

      /**
       * Queries the number of joints (i.e., instances of {@link #JointBase})
       * associated with this joint node.
       */
      int numJoints() {
         return myNumJoints;
      }

      /**
       * Queries the number of constrainers associated with this joint node.
       */
      int numConstrainers() {
         return myConstrainers.size();
      }

      /**
       * Return all rigid bodies associated with this joint node.  This
       * excludes {@link #Ground} and connectable bodies that are not rigid
       * bodies. This is used by the coordinate solver.
       */
      public ArrayList<RigidBody> getRigidBodies() {
         ArrayList<RigidBody> list = new ArrayList<>();
         for (ConnectableBody cbod : myBodies) {
            if (cbod instanceof RigidBody && cbod != Ground) {
               list.add ((RigidBody)cbod);
            }
         }
         return list;
      }
      
      /**
       * Return the index of a rigid body associated with this joint node.
       * This is the index of the body within the list returned by {@link
       * #getRigidBodies()}, or -1 if the body is not associated with the node.
       */
      public int indexOfRigidBody (RigidBody body) {
         int idx = 0;
         for (ConnectableBody cbod : myBodies) {
            if (cbod instanceof RigidBody && cbod != Ground) {
               if (cbod == body) {
                  return idx;
               }
               idx++;
            }
         }
         return -1;
      }

      /**
       * Returns the connectable bodies associated with this node which are
       * <i>not</i> associated with its parent or child nodes.
       */
      ArrayList<ConnectableBody> getOtherBodies() {
         ArrayList<ConnectableBody> bodies = new ArrayList<>();
         for (ConnectableBody body : myBodies) {
            boolean isNode = false;
            for (BodyNode bnode : myBodyNodes) {
               if (bnode.myBody == body) {
                  isNode = true;
                  break;
               }
            }
            if (!isNode) {
               bodies.add (body);
            }
         }
         return bodies;
      }

      /**
       * Recursively find all bodies associated with descendant body
       * nodes of this joint node.
       */
      void recursivelyGetBodies (ArrayList<ConnectableBody> bodies) {
         for (ConnectableBody cbod : myBodies) {
            if (cbod != myProximalBodyNode.myBody) {
               bodies.add (cbod);
            }
         }
         for (BodyNode bnode : myBodyNodes) {
            if (bnode != myProximalBodyNode) {
               for (JointNode jnode : bnode.myJointNodes) {
                  if (jnode != this) {
                     jnode.recursivelyGetBodies (bodies);
                  }
               }
            }
         }
      }

      /**
       * Returns a number for this node, to help identify it. This is the
       * minimum component number of all the constrainers and is not
       * necessarily unique.
       */
      public int getNumber() {
         return Math.max(myMinJntNum, myMinConNum);
      }

      /**
       * Queries whether this node contains a specified body.
       */
      boolean containsBody (ConnectableBody body) {
         return myBodies.contains (body);
      }

      /**
       * If this node contains any bodies marked as ground, return the one with
       * the minimum component number. Otherwise return {@code null}.  Used to
       * help find the tree root.
       */
      ConnectableBody findMinMarkedGroundBody () {
         ConnectableBody minBody = null;
         int minBodyNum = -1;
         for (ConnectableBody body : myBodies) {
            if (body.isGrounded() &&
                (minBodyNum == -1 || body.getNumber() < minBodyNum)) {
               minBodyNum = body.getNumber();
               minBody = body;
            }
         }
         return minBody;
      }

      /**
       * Among the bodies contained in this node, return the one with the
       * minimum component number. Used to help find the tree root.
       */
      ConnectableBody findMinNumberedBody () {
         ConnectableBody minBody = null;
         int minBodyNum = -1;
         for (ConnectableBody body : myBodies) {
            if (minBodyNum == -1 || body.getNumber() < minBodyNum) {
               minBodyNum = body.getNumber();
               minBody = body;
            }
         }
         return minBody;
      }

      /**
       * Among the bodies contained in this node, return the one with the
       * greatest surplus of "outward" connectors (BodyConnectors for which the
       * body is the "B" body). Used to help find the tree root.
       */
      ConnectableBody findMostOutwardConnectedBody (IntHolder surplus) {
         ConnectableBody bestBody = null;
         int maxSurplus = Integer.MIN_VALUE;
         for (ConnectableBody body : myBodies) {
            int surp = outwardConnectorSurplus (body);
            if (surp > maxSurplus) {
               maxSurplus = surp;
               bestBody = body;
            }
         }
         surplus.value = maxSurplus;
         return bestBody;
      }

      /**
       * Queries whether this joint node is <i>simple</i>, meaning that it has
       * a single constrainer that is also a joint.
       */
      boolean isSimple() {
         return numJoints() == 1 && numConstrainers() == 1;
      }

      /**
       * If this node contains any bodies that are non-dynamic rigid bodies,
       * return the one with the minimum component number. Otherwise return
       * {@code null}.  Used to help find the tree root.
       */
      ConnectableBody findMinNonDynamicBody () {
         ConnectableBody minBody = null;
         int minBodyNum = -1;
         for (ConnectableBody body : myBodies) {
            if (body instanceof RigidBody &&
                !((RigidBody)body).isDynamic() &&
                (minBodyNum == -1 || body.getNumber() < minBodyNum)) {
               minBodyNum = body.getNumber();
               minBody = body;
            }
         }
         return minBody;
      }

      /**
       * Returns a string respresentation of the node for testing and
       * debugging.
       */
      public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append (String.format ("jointNode %2d ", getNumber()));
         sb.append (" constrainers:");
         for (BodyConstrainer bcon : myConstrainers) {
            sb.append (" " + bcon.getName());
         }
         sb.append (" bodies:");
         for (ConnectableBody cbod : myBodies) {
            sb.append (" " + cbod.getName());
         }
         sb.append (" bodyNodes:");
         for (BodyNode bnode : myBodyNodes) {
            sb.append (" " + bnode.myBody.getName());
         }
         sb.append (
            " proximalBody: "+
            (myProximalBodyNode != null ?
             myProximalBodyNode.myBody.getName() : "null"));
         return sb.toString();
      }

      /**
       * {@InheritDoc}
       */
      BodyNode getParent() {
         return myProximalBodyNode;
      }

      /**
       * {@InheritDoc}
       */
      BodyNode getRoot() {
         BodyNode bnode;
         JointNode jnode = this;
         while ((bnode=jnode.getParent()) != null) {
            if ((jnode=bnode.getParent()) == null) {
               return bnode;
            }
         }
         return null;
      }

      /**
       * {@InheritDoc}
       */
      List<BodyNode> getChildren() {
         ArrayList<BodyNode> children = new ArrayList<>(myBodyNodes.size());
         for (BodyNode bnode : myBodyNodes) {
            if (bnode != myProximalBodyNode) {
               children.add (bnode);
            }
         }
         return children;
      }

      /**
       * {@InheritDoc}
       */
      int numChildren() {
         int numc = myBodyNodes.size();
         if (myProximalBodyNode != null) {
            numc--;
         }
         return numc;
      }

      /**
       * Returns the list of constrainers associated with this joint node.
       * Should not be modified.
       */
      List<BodyConstrainer> getConstrainers() {
         return myConstrainers;
      }

      ConnectableBody getProximalBody() {
         return myProximalBodyNode != null ? myProximalBodyNode.myBody : null;
      }

      /**
       * If the body associated with this node's parent is a rigid body and is
       * not {@link #Ground}, return the body. Otherwise return {@code null}.
       */
      RigidBody getProximalRigidBody() {
         ConnectableBody cbod = getProximalBody();
         if (cbod instanceof RigidBody && cbod != Ground) {
            return (RigidBody)cbod;
         }
         else {
            return null;
         }
      }

      /**
       * Apply the transform T to the pose of all bodies associated with this
       * joint node, <i>except</i> the body associated with its parent node.
       * and update all body connectors to reflect the new body positions.
       */
      void updateDistalBodiesAndConnectors (RigidTransform3d T) {
         for (ConnectableBody cbod : myBodies) {
            if (cbod != getProximalBody()) {
               cbod.transformPose (T);
            }
         }
         // update connectors
         for (BodyConstrainer bcon : myConstrainers) {
            if (bcon instanceof BodyConnector) {
               ((BodyConnector)bcon).updateAttachments();
            }
         }       
      }   
   }

   /**
    * Contains body connectivity information that is used for computing the
    * kinematic tree. This consists of (a) a list of all constrainers
    * associated with a body, and (b) several attributes that are used for
    * construction of the kinematic tree itself.
    */
   static class BodyInfo {
      ConnectableBody myBody; // the body itself
      // all constrainers associated with the body:
      ArrayList<BodyConstrainer> myConstrainers;

      // attributes for used by {@link #findJointNodes}:
      int myIndex;
      int myLowlink;
      BodyInfo myParent;
      boolean myVisited;
      BodyNode myBodyNode; // body node, once computed 

      BodyInfo (ConnectableBody body) {
         myBody = body;
         myIndex = -1;
         myConstrainers = new ArrayList<>();
      }

      BodyInfo (
         ConnectableBody body, List<? extends BodyConstrainer> constrainers) {
         this (body);
         if (constrainers != null) {
            myConstrainers.addAll (constrainers);
         }
      }
   }

   /**
    * Create an empty kinematic tree.
    */
   public KinematicTree () {
   }

   /**
    * Create a tree based on a specific root node.
    */
   public KinematicTree (BodyNode root) {
      myRoot = root;
      buildTree (root, null);
   }

   /**
    * Queries the root node of this tree.
    */
   public BodyNode getRoot() {
      return myRoot;
   }

   /**
    * Recursively build a kinematic starting from a given body node with a
    * specific parent joint node (which will be null if the body node is the
    * tree root).
    */
   void buildTree (BodyNode bnode, JointNode parentJoints) {
      for (JointNode jnode : bnode.myJointNodes) {
         if (jnode != parentJoints) {
            jnode.myProximalBodyNode = bnode;
            for (BodyNode jchild : jnode.myBodyNodes) {
               if (jchild.myBody != bnode.myBody) {
                  jchild.myProximalJoints = jnode;
                  buildTree (jchild, jnode);
               }
            }
         }
      }
   }

   /**
    * Get a list of all the bodies in this tree.
    */
   public ArrayList<ConnectableBody> getBodies() {
      ArrayList<ConnectableBody> bodies = new ArrayList<>(); 
      bodies.add (myRoot.myBody);
      for (JointNode jnode : myRoot.myJointNodes) {
         jnode.recursivelyGetBodies (bodies);
      }
      return bodies;
   }

   /**
    * Get a list of all the nodes (both body and joint) in this tree.
    */
   ArrayList<TreeNode> getNodes() {
      ArrayList<TreeNode> nodes = new ArrayList<>();
      myRoot.getNodes (nodes);
      return nodes;
   }

   /**
    * Get a list of all the joint nodes in this tree.
    */
   ArrayList<JointNode> getJointNodes() {
      ArrayList<JointNode> nodes = new ArrayList<>();
      myRoot.getJointNodes (nodes);
      return nodes;
   }

   /**
    * Finds all bodies connected to a given body via body connectors or a set
    * of extraConstrainers. Each body is wrapped in a BodyInfo structure, and
    * the BodyInfos are returns as a HashSet.
    */
   static HashMap<ConnectableBody,BodyInfo> findBodyInfoForBody (
      ConnectableBody body0, 
      ArrayList<BodyConstrainer> extraConstrainers) {
      if (debug) {
         System.out.println ("finding for "+body0.getName());
      }
      LinkedHashMap<ConnectableBody,BodyInfo> bodyInfoMap = 
         new LinkedHashMap<>();
      LinkedList<ConnectableBody> queue = new LinkedList<>();
      BodyInfo groundInfo = new BodyInfo (Ground);
      queue.offer (body0);
      while (!queue.isEmpty()) {
         ConnectableBody body = queue.poll();

         BodyInfo binfo;
         if (body == Ground) {
            binfo = groundInfo;
            bodyInfoMap.put (body, groundInfo);
         }
         else {
            binfo = new BodyInfo(body, body.getConnectors());
            bodyInfoMap.put (body, binfo);
            if (body.getConnectors() != null) {
               for (BodyConnector bcon : body.getConnectors()) {
                  ConnectableBody cbod = bcon.getOtherBody (body);
                  if (cbod != null) {
                     if (bodyInfoMap.get(cbod) == null) {
                        queue.offer (cbod);
                     }
                  }
                  else {
                     groundInfo.myConstrainers.add (bcon);
                     if (bodyInfoMap.get(Ground) == null) {
                        queue.offer (Ground);
                     }
                  }
               }
            }
            for (BodyConstrainer bcon : extraConstrainers) {
               boolean referencesBody = false;
               for (ConnectableBody cb : bcon.getBodies()) {               
                  ConnectableBody cbod = nullBodyToGround (cb);
                  if (cbod == body) {
                     binfo.myConstrainers.add (bcon);
                     referencesBody = true;
                     break;
                  }
               }
               if (referencesBody) {
                  for (ConnectableBody cb : bcon.getBodies()) {   
                     ConnectableBody cbod = nullBodyToGround (cb);
                     if (cbod != body) {
                        if (bodyInfoMap.get(cbod) == null) {
                           queue.offer (cbod);
                        }
                        if (cbod == Ground) {
                           if (!groundInfo.myConstrainers.contains (bcon)) {
                              groundInfo.myConstrainers.add (bcon);
                           }
                        }
                     }
                  }
               }
            }
         }           
      }
      if (debug) {
         for (BodyInfo binfo : bodyInfoMap.values()) {
            System.out.println (" body: " + binfo.myBody.getName());
            System.out.print ("   constrainers:");
            for (BodyConstrainer bc : binfo.myConstrainers) {
               System.out.print (" " + bc.getName());
            }
            System.out.println ("");
         }
      }
      return bodyInfoMap;
   }

   /**
    * Search all joint nodes for a body to use at the root of the tree, and
    * create a BodyNode for it if one does not already exist. Choices are, in
    * order:
    * <ol>
    *
    * <li>Ground (which will be unique)
    *
    * <li>The minimum numbered body marked as `grounded'
    *
    * <li>The minumum numbered non-dynamic body
    *
    * <li>A body whose external connectors are all oriented outward (so that
    * the 'B' bodies are all in the node); amoung these, we choose the one with
    * the maximum number of external connectors and then the minimum body
    * number.
    * 
    * </ol>
    */
   static BodyNode findRootNode (
      List<JointNode> jointNodes,
      HashMap<ConnectableBody,BodyInfo> bodies) {

      int markedGroundNum = -1;
      int nonDynamicNum = -1;
      int maxOutward = -1;
      int minOutwardBodyNum = -1;
      ConnectableBody bestBody = null;
      JointNode bestNode = null;

      // first look for bodies which are ground, marked as ground, or
      // non-dynamic
      ConnectableBody body;
      for (JointNode jnode : jointNodes) {
         if (jnode.containsBody (Ground)) {
            bestNode = jnode;
            bestBody = Ground;
            break;
         }
         else if ((body=jnode.findMinMarkedGroundBody()) != null) {
            if (markedGroundNum == -1 || body.getNumber() < markedGroundNum) {
               bestNode = jnode;
               bestBody = body;
               markedGroundNum = body.getNumber();
            }
         }
         else if (markedGroundNum == -1 &&
                  (body=jnode.findMinNonDynamicBody()) != null) {
            if (nonDynamicNum == -1 || body.getNumber() < nonDynamicNum) {
               bestNode = jnode;
               bestBody = body;
               nonDynamicNum = body.getNumber();
            }
         }
         else if (markedGroundNum == -1 && nonDynamicNum == -1) {
            IntHolder surplus = new IntHolder();
            ConnectableBody bod = jnode.findMostOutwardConnectedBody (surplus);
            if (maxOutward == -1 ||
                 (maxOutward < surplus.value ||
                    (maxOutward == surplus.value &&
                     bod.getNumber() < minOutwardBodyNum))) { 
               bestNode = jnode;
               bestBody = bod;
               maxOutward = surplus.value;
               minOutwardBodyNum = bod.getNumber();
            }
         }
      }
      // if root still not found, that means there are no body nodes, or bodies
      // which are ground, marked as ground, or non-dynamic. Search the
      // (single) joint node for the body with the lowest number.
      if (bestBody == null) {
         if (jointNodes.size() != 1) {
            throw new InternalErrorException (
               "Number of joint nodes is " +
               jointNodes.size() + " but no body nodes");
         }
         bestNode = jointNodes.get(0);
         bestBody = bestNode.findMinNumberedBody();
      }
      //System.out.println (" found " + bestBody.getName());
      BodyInfo binfo = bodies.get (bestBody);
      if (binfo.myBodyNode == null) {
         // create body node
         BodyNode bnode = new BodyNode (bestBody);
         bnode.addJointNode (bestNode);
         bestNode.addBodyNode (bnode);
         binfo.myBodyNode = bnode;
      }
      return binfo.myBodyNode;
   }

   /**
    * Finds the kinematic tree for a set of kinematically connected bodies.  It
    * is assumed that all the bodies in the set <i>are</i> kinematically
    * connected, such that each body will be contained in the tree. A suitable
    * <i>proximal</i> body will be chosen as the root of the tree.
    */
    static KinematicTree findKinematicTree (
       HashMap<ConnectableBody,BodyInfo> bodies) {

      if (bodies.size() == 1) {
         // isolated body
         return null;
      }

      List<JointNode> jointNodes = findJointNodes (bodies);
      BodyNode root = findRootNode (jointNodes, bodies);

      KinematicTree tree = new KinematicTree(root);
      for (BodyInfo binfo : bodies.values()) {
         binfo.myIndex = -1;
      }
      return tree;
   }

   /**
    * Convert a {@code null} connectable body (which can indicate ground in
    * ArtiSynth) to {@link #Ground}.
    */
   static ConnectableBody nullBodyToGround (ConnectableBody cbod) {
      if (cbod == null) {
         return Ground;
      }
      else {
         return cbod;
      }
   }

   /**
    * Aggregate all the constrainers connecting a set of bodies into joint
    * nodes. It is assumed that all the bodies in the set <i>are</i>
    * kinematically connected.
    */
   static List<JointNode> findJointNodes (
      HashMap<ConnectableBody,BodyInfo> bodies) {
      ArrayDeque<BodyConstrainer> stack = new ArrayDeque<>();
      HashSet<BodyConstrainer> wasStacked = new HashSet<>();
      ArrayList<JointNode> nodes = new ArrayList<>();
      IntHolder index = new IntHolder();
      for (BodyInfo v : bodies.values()) {
         if (v.myIndex == -1) {
            searchForJointNodes (nodes, v, bodies, stack, wasStacked, index, "");
         }
      }
      return nodes;
   }

   /**
    * Recursively applies the depth-first Hopcroft-Tarjan algorithm to find the
    * joint nodes among a set of constrained bodies. The algorithm works by
    * considering finding the "cut vertices" in a graph in which the bodies are
    * vertices and the constrainers are edges. These cut vertices are then used
    * to form the body nodes of the kinematic tree, while the joint nodes are
    * formed from the subgraph of bodies and constrainers that separate each
    * cut vertex.
    */
   static void searchForJointNodes (
      ArrayList<JointNode> jointNodes,
      BodyInfo v, HashMap<ConnectableBody,BodyInfo> bodyInfo,
      ArrayDeque<BodyConstrainer> stack, HashSet<BodyConstrainer> wasStacked,
      IntHolder index, String pfx) {

      v.myIndex = index.value;
      index.value += 1;
      v.myLowlink = v.myIndex;
      boolean isCutPoint = false;
      int childCnt = 0;
      LinkedList<JointNode> newJointNodes = new LinkedList<>();
      for (BodyConstrainer bcon : v.myConstrainers) {
         if (!wasStacked.contains (bcon)) {
            wasStacked.add (bcon);
            stack.push (bcon);
         }
         for (ConnectableBody bod : bcon.getBodies()) {
            bod = nullBodyToGround (bod);
            if (bod != v.myBody) {
               BodyInfo w = bodyInfo.get(bod);
               if (w.myIndex == -1) {
                  // add (v,w) to T (set of depth first edges)
                  w.myParent = v;
                  childCnt++;
                  searchForJointNodes (
                     jointNodes, w, bodyInfo, stack, wasStacked, index, pfx+"  ");
                  if (w.myLowlink >= v.myIndex) {
                     // found connected
                     isCutPoint = true;
                     JointNode jnode = new JointNode();
                     BodyConstrainer bc = null;
                     do {
                        bc = stack.pop();
                        jnode.addConstrainer (bc);
                     }
                     while (bc != bcon);
                     jnode.sortBodiesAndConstrainers();
                     // add joint node to existing body nodes
                     for (ConnectableBody body : jnode.myBodies) {
                        BodyInfo binfo = bodyInfo.get (body);
                        if (binfo.myBodyNode != null) {
                           binfo.myBodyNode.addJointNode (jnode);
                           jnode.addBodyNode (binfo.myBodyNode);
                        }
                     }
                     newJointNodes.add (jnode);
                     // found biconnected component
                  }
                  v.myLowlink = Math.min (v.myLowlink, w.myLowlink);
               }
               else if (w != v.myParent) {
                  v.myLowlink = Math.min (v.myLowlink, w.myIndex);
               }
            }
         }
      }
      if ((v.myParent != null && isCutPoint) ||
          (v.myParent == null && childCnt > 1)) {
         v.myBodyNode = new BodyNode (v.myBody);
         for (JointNode jnode : newJointNodes) {
            v.myBodyNode.addJointNode (jnode);
            jnode.addBodyNode (v.myBodyNode);
         }
      }
      jointNodes.addAll (newJointNodes);
   }

   /**
    * Filters a list of constrainers to return those which are
    * instances of BodyContrainer but <i>not</i> BodyConnectors.
    */
   static ArrayList<BodyConstrainer> findBodyConstrainers (
      Collection<? extends Constrainer> extraConstrainers) {
      
      ArrayList<BodyConstrainer> list = new ArrayList<>();
      for (Constrainer c : extraConstrainers) {
         if (c instanceof BodyConstrainer && !(c instanceof BodyConnector)) {
            list.add ((BodyConstrainer)c);
         }
      }
      return list;
   }

   /**
    * Finds the kinematic tree associated with a given body. This
    * is formed from all the bodies that it is connected to via
    * both body connectors and extra constrainers.
    */
   public static KinematicTree findTree (
      ConnectableBody body, 
      Collection<? extends Constrainer> extraConstrainers) {
      HashMap<ConnectableBody,BodyInfo> binfos = 
         findBodyInfoForBody (body, findBodyConstrainers(extraConstrainers));
      return findKinematicTree (binfos);
   }         

   /**
    * Finds the kinematic tree associated with a given body connector. This
    * is formed from all the bodies that it is connected to via
    * both body connectors and extra constrainers.
    */
   public static KinematicTree findTree (
      BodyConnector bcon, 
      Collection<? extends Constrainer> extraConstrainers) {
      return findTree (bcon.getBodyA(), findBodyConstrainers(extraConstrainers));
   }

   /**
    * Finds all the kinematic trees associated with a set of bodies.
    */
   public static List<KinematicTree> findTreesForBodies (
      Collection<? extends ConnectableBody> bodies, 
      Collection<ConstrainerBase> extraConstrainers) {
      HashSet<ConnectableBody> bodySet = new HashSet<>();
      ArrayList<KinematicTree> trees = new ArrayList<>();
      ArrayList<BodyConstrainer> bodyConstrainers =
         findBodyConstrainers(extraConstrainers);
      for (ConnectableBody body : bodies) {
         if (!bodySet.contains (body)) {
            KinematicTree tree = findTree (body, bodyConstrainers);
            if (tree != null) {
               trees.add (tree);
               bodySet.addAll (tree.getBodies());
            }
         }
      }
      return trees;
   }         
}
