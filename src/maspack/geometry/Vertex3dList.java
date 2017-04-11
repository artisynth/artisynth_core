package maspack.geometry;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

public class Vertex3dList implements Iterable<Vertex3dNode>, Clonable {

   boolean myClosed;
   int mySize;
   Vertex3dNode myHead;
   Vertex3dNode myTail; // for open lists only, points to the last vertex

   public Vertex3dList (boolean closed) {
      myClosed = closed;
      myHead = null;
      myTail = null;
      mySize = 0;
   }

   public Vertex3dList (boolean closed, double[] vals) {
      this (closed);
      set (vals);
   }
   
   public Vertex3dList (boolean closed, Collection<Vector3d> pnts) {
      this (closed);
      set (pnts);
   }
   
   public int set (double[] vals) {
      clear();
      int idx = 0;
      for (int i=0; i<vals.length; i+=3) {
         Vertex3d vtx = new Vertex3d (vals[i], vals[i+1], vals[i+2]);
         vtx.setIndex (idx++);
         add (vtx);
      }
      return idx;
   }

   public int set (Collection<Vector3d> pnts) {
      clear();
      int idx = 0;
      for (Vector3d p : pnts) {
         Vertex3d vtx = new Vertex3d (p.x, p.y, p.z);
         vtx.setIndex (idx++);
         add (vtx);
      }
      return idx;
   }

   public int set (double[] vals, int idx) {
      clear();
      for (int i=0; i<vals.length; i+=3) {
         Vertex3d vtx = new Vertex3d (vals[i], vals[i+1], vals[i+2]);
         vtx.setIndex (idx++);
         add (vtx);
      }
      return idx;
   }

   public int size() {
      return mySize;
   }

   public boolean isClosed() {
      return myClosed;
   }

   private class MyIterator implements Iterator<Vertex3dNode> {
      
      Vertex3dNode myNextNode;

      MyIterator() {
         myNextNode = myHead;
      }

      public boolean hasNext() {
         return myNextNode != null;
      }

      public Vertex3dNode next() {
         if (myNextNode == null) {
            throw new NoSuchElementException();
         }
         Vertex3dNode next = myNextNode;
         myNextNode = next.next;
         if (myClosed && myNextNode == myHead) {
            myNextNode = null;
         }
         return next;
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }
   }

   public Iterator<Vertex3dNode> iterator() {
      return new MyIterator();
   }

   public Vertex3dNode getNode (int idx) {
      Vertex3dNode node = myHead;
      if (myClosed) {
         if (idx >= 0) {
            while (idx-- > 0) {
               node = node.next;
            }
         }
         else {
            while (idx++ < 0) {
               node = node.prev;
            }
         }
      }
      else {
         if (idx < 0 || idx >= mySize) {
            throw new IndexOutOfBoundsException (
               "Requested index "+idx+", size = " + mySize);
         }
         if (idx >= 0) {
            while (idx-- > 0) {
               node = node.next;
            }
         }
      }
      return node;
   }

   public Vertex3d get (int idx) {
      return getNode (idx).vtx;
   }

   private boolean isCoincidentWithLast (Point3d pnt, double dtol) {
      if (myHead == null) {
         return false;
      }
      else {
         Point3d lastp = myClosed ? myHead.prev.vtx.pnt : myTail.vtx.pnt;
         return (lastp.distance (pnt) <= dtol);
      }
   }      

   public Vertex3dNode addNonCoincident (Vertex3d vtx, double dtol) {
      if (!isCoincidentWithLast (vtx.pnt, dtol)) {
         return add (vtx);
      }
      else {
         return null;
      }
   }

   public Vertex3dNode add (Vertex3d vtx) {   
      if (vtx == null) {
         throw new IllegalArgumentException ("vtx is null");
      }
      Vertex3dNode node = new Vertex3dNode (vtx);
      add (node);
      return node;
   }         

   public void addNonCoincident (Vertex3dNode node, double dtol) {
      if (!isCoincidentWithLast (node.vtx.pnt, dtol)) {
         add (node);
      }
   }

   public void add (Vertex3dNode node) {
      if (myClosed) {
         if (myHead == null) {
            myHead = node;
            node.next = node;
            node.prev = node;
         }
         else {
            Vertex3dNode prev = myHead.prev;
            node.next = myHead;
            node.prev = prev;
            prev.next = node;
            myHead.prev = node;
         }
      }
      else {
         if (myHead == null) {
            myHead = node;
            node.next = null;
            node.prev = null;
         }
         else {
            myTail.next = node;
            node.next = null;
            node.prev = myTail;
         }
         myTail = node;
      }
      mySize++;
   }

   public void addAfter (Vertex3dNode node, Vertex3dNode prev) {
      if (myClosed) {
         node.next = prev.next;
         node.prev = prev;
         prev.next = node;
         node.next.prev = node;
      }
      else {
         node.next = prev.next;
         node.prev = prev;
         prev.next = node;
         if (node.next == null) {
            myTail = node;
         }
         else {
            node.next.prev = node;
         }
      }
      mySize++;
   }
      
   public void addBefore (Vertex3dNode node, Vertex3dNode next) {
      if (myClosed) {
         node.prev = next.prev;
         node.next = next;
         next.prev = node;
         node.prev.next = node;
      }
      else {
         node.prev = next.prev;
         node.next = next;
         next.prev = node;
         if (node.prev == null) {
            myHead = node;
         }
         else {
            node.prev.next = node;
         }
      }
      mySize++;
   }
      

   public void remove (Vertex3dNode node) {
      Vertex3dNode next = node.next;
      Vertex3dNode prev = node.prev;

      if (myClosed) {
         if (next == node) {
            // last item in the list
            myHead = null;
         }
         else {
            next.prev = prev;
            prev.next = next;
            if (node == myHead) {
               myHead = next;
            }
         }
      }
      else {
         if (prev == null) {
            // node == myHead
            if (next == null) {
               // last item in the list
               myHead = null;
               myTail = null;
            }
            else {
               next.prev = null;
               myHead = next;
            }
         }
         else if (next == null) {
            // node == myTail
            prev.next = null;
            myTail = prev;
         }
         else {
            next.prev = prev;
            prev.next = next;
         }
      }
      mySize--;
   }

   public Vertex3dNode removeFirst () {
      if (myHead == null) {
         return null;
      }
      Vertex3dNode node = myHead;
      Vertex3dNode next = node.next;

      if (myClosed) {
         if (next == node) {
            // last item in the list
            myHead = null;
         }
         else {
            Vertex3dNode prev = node.prev;
            next.prev = prev;
            prev.next = next;
            myHead = next;
         }
      }
      else {
         // node == myHead
         if (next == null) {
            // last item in the list
            myHead = null;
            myTail = null;
         }
         else {
            next.prev = null;
            myHead = next;
         }
      }
      mySize--;
      return node;
   }

   public boolean isEmpty() {
      return myHead == null;
   }

   public void reverse() {
      if (myHead == null) {
         return;
      }
      Vertex3dNode last = myClosed ? myHead : null;
      Vertex3dNode next = myClosed ? null : myHead;
      Vertex3dNode prev = myHead.prev;
      for (Vertex3dNode node=myHead; next!=last; node=next) {
         next = node.next;
         node.next = prev;
         node.prev = next;
         prev = node;
      }
      if (!myClosed) {
         next = myHead;
         myHead = myTail;
         myTail = next;         
      }
   }

   public void clear() {
      myHead = null;
      myTail = null;
      mySize = 0;
   }

   public void setClosed (boolean closed) {
      if (myClosed != closed) {
         if (!isEmpty()) {
            if (closed) {
               // was open, convert to closed
               myHead.prev = myTail;
               myTail.next = myHead;
               myTail = null;
            }
            else {
               // was closed, convert to open
               myTail = myHead.prev;
               myTail.next = null;
               myHead.prev = null;
            }
         }
         myClosed = closed;
      }
   }

   public Vertex3dList clone() {
      Vertex3dList copy = null;
      try {
         copy = (Vertex3dList)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException (
            "Clone not supported for Vertex3dList");
      }
      copy.clear();
      for (Vertex3dNode node : this) {
         copy.add (node.clone());
      }
      return copy;
   }

   public boolean equals (Vertex3dList list) {
      if (myClosed != list.myClosed) {
         return false;
      }
      if (mySize != list.mySize) {
         return false;
      }
      Vertex3dNode listNode = list.myHead;
      for (Vertex3dNode node : this) {
         if (listNode.vtx != node.vtx) {
            return false;
         }
         listNode = listNode.next;
      }
      return true;
   }
}
