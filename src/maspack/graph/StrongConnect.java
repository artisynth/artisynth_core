package maspack.graph;

import java.util.*;
import maspack.util.*;

/**
 * Demo code showing how to find the strongly connected components of a graph
 * using Tarjan's algorithm. Note: this code has been morphed from some older
 * working code but has not been explicitly tested in its new form.
 */
public class StrongConnect {

   static class NodeInfo {
      ArrayList<NodeInfo> myAdjacents; // adjacent node info
      int myIndex;
      int myLowlink;
      boolean myOnStack;
      StrongNode myStrongNode;

      NodeInfo () {
         myIndex = -1;
         myAdjacents = new ArrayList<>();
      }
   }

   static class StrongNode {
      ArrayList<NodeInfo> myNodes = new ArrayList<>();

      public StrongNode() {
      }

      public void addNode (NodeInfo ninfo) {
         myNodes.add (ninfo);
      }
   }

   static List<StrongNode> findStrongNodes (
      Collection<NodeInfo> nodes) {
      ArrayDeque<NodeInfo> stack = new ArrayDeque<>();
      ArrayList<StrongNode> snodes = new ArrayList<>();
      IntHolder index = new IntHolder();
      for (NodeInfo v : nodes) {
         if (v.myIndex == -1) {
            strongConnect (snodes, v, stack, index, "  ");
         }
      }
      return snodes;
   }


   static void strongConnect (
      ArrayList<StrongNode> snodes, NodeInfo v,
      ArrayDeque<NodeInfo> stack, IntHolder index, String pfx) {

      v.myIndex = index.value;
      v.myLowlink = index.value;
      index.value += 1;

      stack.push(v);
      v.myOnStack = true;

      for (NodeInfo w : v.myAdjacents) {
         if (w.myIndex == -1) {
            strongConnect (
               snodes, w, stack, index, pfx+"  ");
            v.myLowlink = Math.min(v.myLowlink, w.myLowlink);
         }
         else if (w.myOnStack && w.myIndex < v.myIndex) {
            v.myLowlink = Math.min(v.myLowlink, w.myIndex);
         }
      }
      if (v.myLowlink == v.myIndex) {
         StrongNode snode = new StrongNode();
         NodeInfo w;
         do {
            w = stack.pop();
            w.myOnStack = false;
            snode.addNode (w);
            w.myStrongNode = snode;
         }
         while (w != v);
         snodes.add (snode);
      }
   }  


}
