/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.*;

import maspack.matrix.Point3d;

public class HashedPointSet implements Iterable<Point3d> {

   private class Entry {
      Entry myLink;
      Entry myNext;
      Point3d myPnt;

      Entry (Point3d pnt) {
         myPnt = pnt;
         myLink = null;
         myNext = null;
      }
   }

   private Entry[] myTable;
   private int mySizeIndex;
   private int mySize = 0;
   private Entry myFirstEntry = null;
   private Entry myLastEntry = null;

   private class MyIterator implements Iterator<Point3d> {
      
      Entry e;

      MyIterator() {
         e = myFirstEntry;
      }

      public boolean hasNext() {
         return e != null;
      }

      public Point3d next() {
         if (e != null) {
            Point3d pnt = e.myPnt;
            e = e.myLink;
            return pnt;
         }
         else {
            throw new NoSuchElementException();
         }
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }
   }

   private static int[] mySizes = {
      17,
      37,
      79,
      163,
      331,
      673,
      1361,
      2729,
      5471,
      10949,
      21911,
      43858,
      87719,
      175447,
      350899,
      701819,
      1403641,
      2807303,
      5614657,
      11229331,
      22458671,
      44917381,
      89834777
   };   

   public HashedPointSet () {
      this (17);
   }

   public HashedPointSet (int capacity) {
      int realcap = -1;
      for (int i=0; i<mySizes.length; i++) {
         if (mySizes[i] >= capacity) {
            realcap = mySizes[i];
            mySizeIndex = i+1;
            break;
         }
      }
      if (realcap == -1) {
         realcap = mySizes[mySizes.length-1];
         mySizeIndex = mySizes.length;
         while (realcap < capacity) {
            realcap *= 2;
         }
      }
      myTable = new Entry[realcap];
   }

   public int size() {
      return mySize;
   }

   public int capacity() {
      return myTable.length;
   }

   private void resize() {
      int newcap;
      if (mySizeIndex == mySizes.length) {
         newcap = 2*myTable.length;
      }
      else {
         newcap = mySizes[mySizeIndex++];
      }
      Entry[] newtable = new Entry[newcap];
      for (Entry e=myFirstEntry; e!=null; e=e.myLink) {
         int idx = e.myPnt.hashCode()%newcap;
         Entry p = newtable[idx];
         if (p == null) {
            newtable[idx] = e;
         }
         else {
            while (p.myNext != null) {
               p = p.myNext;
            }
            p.myNext = e;
         }
         e.myNext = null;
      }
      myTable = newtable;
   }      

   public boolean add (Point3d pnt) {
      int idx = pnt.hashCode()%myTable.length;
      Entry p = myTable[idx];
      if (p != null) {
         if (p.myPnt == pnt) {
            return false;
         }
         while (p.myNext != null) {
            p = p.myNext;            
            if (p.myPnt == pnt) {
               return false;
            }
         }
         Entry e = new Entry (pnt);
         p.myNext = e;
         myLastEntry.myLink = e;
         myLastEntry = e;
      }
      else {
         Entry e = new Entry (pnt);         
         myTable[idx] = e;
         if (myFirstEntry == null) {
            myFirstEntry = e;
         }
         else {
            myLastEntry.myLink = e;
         }
         myLastEntry = e;
      }
      if (mySize++ > (3*myTable.length/4)) {
         resize();
      }
      return true;
   }

   public Point3d[] getPoints() {
      Point3d[] pnts = new Point3d[mySize];
      int k = 0;
      for (Entry e=myFirstEntry; e!=null; e=e.myLink) {
         pnts[k++] = e.myPnt;
      }
      return pnts;
   }

   public Iterator<Point3d> iterator() {
      return new MyIterator();
   }

   public double[] getPointsAsDoubleArray() {
      double[] pnts = new double[3*mySize];
      int k = 0;
      for (Entry e=myFirstEntry; e!=null; e=e.myLink) {
         pnts[k++] = e.myPnt.x;
         pnts[k++] = e.myPnt.y;
         pnts[k++] = e.myPnt.z;
      }
      return pnts;
   }

}
