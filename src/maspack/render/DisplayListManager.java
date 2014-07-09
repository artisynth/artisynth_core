/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import javax.media.opengl.*;
import java.util.*;

public class DisplayListManager {
   public static class StringIntKey {
      String myStr;
      int myNum;

      public StringIntKey (String str, int num) {
         myStr = str;
         myNum = num;
      }

      public boolean equals (Object obj) {
         if (obj instanceof StringIntKey) {
            StringIntKey key = (StringIntKey)obj;
            return myStr.equals (key.myStr) && myNum == key.myNum;
         }
         else {
            return false;
         }
      }

      public int hashCode() {
         return myStr.hashCode() + myNum;
      }
   }

   private static class ListInfo {
      int listNum;
      int refCnt;

      ListInfo (int num) {
         listNum = num;
         refCnt = 0;
      }
   }

   private static LinkedList<Integer> myFreeLists = new LinkedList<Integer>();

   private static HashMap<Object,ListInfo> mySharedLists =
      new HashMap<Object,ListInfo>();

   private static ListInfo getSharedInfo (Object key) {
      synchronized (mySharedLists) {
         return mySharedLists.get (key);
      }
   }

   private static void putSharedInfo (Object key, ListInfo info) {
      synchronized (mySharedLists) {
         mySharedLists.put (key, info);
      }
   }

   private static void removeSharedInfo (Object key) {
      synchronized (mySharedLists) {
         mySharedLists.remove (key);
      }
   }

   private static void clearFreeLists (GL2 gl) {
      synchronized (myFreeLists) {
         for (int listNum : myFreeLists) {
            gl.glDeleteLists (listNum, 1);
         }
         myFreeLists.clear();
      }
   }

   private static void addToFreeList (int num) {
      synchronized (myFreeLists) {
         myFreeLists.add (num);
      }
   }

   public static void freeList (int listNum) {
      addToFreeList (listNum);
   }

   public static int allocList (GL2 gl) {
      clearFreeLists (gl);
      int num = gl.glGenLists (1);
      return num;
   }

   public static int allocSharedList (GL2 gl, Object key) {
      int listNum = allocList (gl);
      if (listNum < 0) {
         return -1;
      }
      ListInfo info = new ListInfo (listNum);
      putSharedInfo (key, info);
      info.refCnt++;
      return listNum;
   }

   public static int getSharedList (GL2 gl, Object key) {
      ListInfo info = getSharedInfo (key);
      if (info == null) {
         return -1;
      }
      info.refCnt++;
      return info.listNum;
   }

   // public static int getOrAllocSharedList (GL2 gl, Object key)
   // {
   // ListInfo info = getSharedInfo (key);
   // if (info == null)
   // { int listNum = allocList (gl);
   // if (listNum <= 0)
   // { return -1;
   // }
   // info = new ListInfo (listNum);
   // putSharedInfo (key, info);
   // }
   // info.refCnt++;
   // return info.listNum;
   // }

   public static void freeSharedList (Object key) {
      ListInfo info = getSharedInfo (key);
      if (info == null) {
         System.out.println ("Warning: no display list found for key " + key);
         return;
      }
      else if (info.refCnt <= 0) {
         System.out.println (
            "Warning: bad refCnt for display list corresponding to key " + key);
         return;
      }
      if (--info.refCnt == 0) {
         addToFreeList (info.listNum);
         removeSharedInfo (key);
      }
   }

   public static Object createKey (String str, int num) {
      return new StringIntKey (str, num);
   }

   public static void main (String[] args) {
      Object key1 = createKey ("sphere", 3);
      Object key2 = createKey ("sphere", 3);
      System.out.println ("key1 == key2: " + (key1.equals (key2)));
      System.out.println ("key1 " + key1.hashCode());
      System.out.println ("key2 " + key2.hashCode());

   }

}
