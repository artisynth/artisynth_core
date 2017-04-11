package maspack.geometry;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

public class Vertex3dListTest extends UnitTest {

   void validate (Vertex3dList list) {
      if (list.isEmpty()) {
         check ("size != 0 when empty", list.size()==0);
         check ("head != null when empty", list.myHead==null);
         check ("tail != null when empty", list.myTail==null);
         return;
      }
      else {
         check ("size == 0 when not empty", list.size()!=0);
         check ("head == null when not empty", list.myHead!=null);
         int size = 0;
         Vertex3dNode node = list.myHead;
         if (list.myClosed) {
            do {
               Vertex3dNode next = node.next;
               Vertex3dNode prev = node.prev;
               check ("node.prev.next != node", prev.next == node);
               check ("node.next.prev != node", next.prev == node);
               node = next;
               size++;
            }
            while (node != list.myHead);
         }
         else {
            while (node != null) {
               Vertex3dNode next = node.next;
               Vertex3dNode prev = node.prev;
               if (node == list.myHead) {
                  check ("node.prev != null at head", prev == null);
               }
               else {
                  check ("node.prev.next != node", prev.next == node);
               }
               if (next == null) {
                  check ("node != tail when next == null", node == list.myTail);
               }
               else {
                  check ("node.next.prev != node", next.prev == node);
               }
               node = node.next;
               size++;
            }
         }
         check ("incorrect size", list.size()==size);
      }
   }

   private void testAdd (Vertex3dList list, Vertex3dNode node) {
      list.add (node);
      validate (list);
   }

   private void testRemove (Vertex3dList list, Vertex3dNode node) {
      list.remove (node);
      validate (list);
   }

   private void testCopyAndReverse (Vertex3dList list) {
      Vertex3dList copy = list.clone();
      list.reverse();
      validate (list);
      list.reverse();
      validate (list);
      if (!copy.equals (list)) {
         throw new TestException (
            "list reversed twice does not equal original list");
      }
   }

   private Vertex3dNode newNode() {
      return new Vertex3dNode (new Vertex3d(0, 0, 0));
   }

   private void testAddRemove (Vertex3dList list) {
      Vertex3dNode n0 = newNode();
      Vertex3dNode n1 = newNode();
      Vertex3dNode n2 = newNode();
      Vertex3dNode n3 = newNode();

      testCopyAndReverse (list);
      testAdd (list, n0);
      testCopyAndReverse (list);
      testAdd (list, n1);
      testCopyAndReverse (list);
      testAdd (list, n2);
      testAdd (list, n3);
      testCopyAndReverse (list);
      testRemove (list, n3);
      testRemove (list, n2);
      testCopyAndReverse (list);
      testRemove (list, n1);
      testRemove (list, n0);
      testCopyAndReverse (list);
      testAdd (list, n0);
      testAdd (list, n1);
      testAdd (list, n2);
      testCopyAndReverse (list);
      testAdd (list, n3);
      testRemove (list, n0);
      testRemove (list, n1);
      testRemove (list, n2);
      testCopyAndReverse (list);
      testRemove (list, n3);
      testAdd (list, n0);
      testAdd (list, n1);
      testCopyAndReverse (list);
      testAdd (list, n2);
      testAdd (list, n3);
      testRemove (list, n2);
      testRemove (list, n1);
      testRemove (list, n0);
      testRemove (list, n3);
      testCopyAndReverse (list);
   }      

   public void test() {
      testAddRemove (new Vertex3dList (/*closed=*/true));
      testAddRemove (new Vertex3dList (/*closed=*/false));
   }

   public static void main (String[] args) {
      Vertex3dListTest tester = new Vertex3dListTest();

      tester.runtest();
   }

}

