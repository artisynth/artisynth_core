/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL.GL2;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import javax.media.opengl.GL2;

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

   /**
    * Contains the display list Id, as well as information
    * to detect if content has changed via a `fingerprint'.
    */
   public static class DisplayListPassport {
      int list;
      Object fingerPrint;
      
      public DisplayListPassport(int listId, Object fingerPrint) {
         this.list = listId;
         this.fingerPrint = fingerPrint;
      }
      
      public int getList() {
         return list;
      }
      
      public void setFingerPrint(Object fp) {
         this.fingerPrint = fp;
      }
      
      /**
       * Uses the internal 'finger print' to check if
       * it matches the supplied print.  Replaces
       * internal finger print with fp to allow for
       * atomic compare/exchange.
       * @return
       */
      public synchronized boolean compareExchangeFingerPrint(Object fp) {
         if (fingerPrint == null) {
            if (fp == null) {
               return true;
            } else {
               fingerPrint = fp;
               return false;
            }
         }
         boolean match = fingerPrint.equals(fp);
         fingerPrint = fp;
         return match;
      }
   }
   
   protected static class DisplayListInfo {
      Object key;
      int useCnt;
      DisplayListPassport id;

      DisplayListInfo (Object key, int listId, Object fingerPrint) {
         this.key = key;
         this.id = new DisplayListPassport(listId, fingerPrint);
         this.useCnt = 0;
      }
      
      void incrementUseCount() {
         useCnt++;
      }
      
      void resetUseCount() {
         useCnt = 0;
      }

      public int getUseCount() {
         return useCnt;
      }
      
      public DisplayListPassport getId() {
         return id;
      }
      
      public int getList() {
         return id.list;
      }
   }

   private LinkedList<Integer> myFreeLists;
   private HashMap<Object,DisplayListInfo> myDisplayLists;
   private HashMap<Integer,Object> myKeyMap;
   
   public DisplayListManager() {
      myFreeLists = new LinkedList<>();
      myDisplayLists = new HashMap<>();
      myKeyMap = new HashMap<>();
   }
   
   private boolean resetP = true;

   public void requestClear() {
      resetP = true;
   }

   private  void clearIfNeeded (GL2 gl) {
      if (resetP) {
         clear(gl);
      }
   }

   protected DisplayListInfo getListInfo (Object key) {
      synchronized (myDisplayLists) {
         return myDisplayLists.get (key);
      }
   }

   protected void putListInfo (Object key, DisplayListInfo info) {
      synchronized (myDisplayLists) {
         DisplayListInfo oldInfo = myDisplayLists.put (key, info);
         if (oldInfo != null && oldInfo.getList() != info.getList()) {
            freeDisplayList(oldInfo.getList());
         }
         myKeyMap.put(info.getList(), key);
      }
   }

   private  void removeDisplayList (Object key) {
      synchronized (myDisplayLists) {
         DisplayListInfo info = myDisplayLists.remove(key);
         if (info != null) {
            myKeyMap.remove(info.getList());
            freeDisplayList(info.getList());
         }
      }
   }

   private  void removeDisplayList (GL2 gl, Object key) {
      synchronized (myDisplayLists) {
         DisplayListInfo info = myDisplayLists.remove(key);
         if (info != null) {
            myKeyMap.remove(info.getList());
            freeDisplayList(gl, info.getList());
         }
      }
   }

   
   private  void clearFreeLists (GL2 gl) {
      synchronized (myFreeLists) {
         for (int listNum : myFreeLists) {
            gl.glDeleteLists (listNum, 1);
         }
         myFreeLists.clear();
      }
   }

   private  void clearDisplayLists (GL2 gl) {
      synchronized (myDisplayLists) {
         for (DisplayListInfo info : myDisplayLists.values()) {
            gl.glDeleteLists (info.getList(), 1);
         }
         myDisplayLists.clear();
         myKeyMap.clear();
      }
   }

   private void addToFreeList (int num) {
      synchronized (myFreeLists) {
         myFreeLists.add (num);
      }
   }

   public void freeDisplayList (int listNum) {
      addToFreeList (listNum);
   }
   
   public void freeDisplayList(GL2 gl, int listNum) {
      clearFreeLists(gl);
      gl.glDeleteLists(listNum, 1);
   }

   private int allocDisplayList (GL2 gl) {
      int num = gl.glGenLists (1);
      return num;
   }

   protected DisplayListInfo allocDisplayList(GL2 gl, Object key, Object fingerPrint) {
      clearIfNeeded (gl);
      clearFreeLists (gl);
      int listNum = allocDisplayList (gl);
      if (listNum < 0) {
         return null;
      }
      DisplayListInfo info = new DisplayListInfo (key, listNum, fingerPrint);
      putListInfo (key, info);
      info.useCnt++;
      return info;
   }
   
   public DisplayListPassport allocateDisplayList (GL2 gl, Object key, Object fingerPrint) {
      DisplayListInfo li = allocDisplayList(gl, key, fingerPrint);
      if (li == null) {
         return null;
      }
      return li.getId();
   }
   
   public DisplayListPassport allocateDisplayList (GL2 gl, Object key) {
      return allocateDisplayList(gl, key, null);
   }

   public DisplayListPassport getDisplayList (GL2 gl, Object key) {
      clearIfNeeded (gl);
      DisplayListInfo info = getListInfo (key);
      if (info == null) {
         return null;
      }
      info.incrementUseCount();
      return info.getId();
   }

   public void freeDisplayList (Object key) {
      removeDisplayList (key);
   }
   
   public void freeDisplayList (GL2 gl, Object key) {
      removeDisplayList (gl, key);
   }

   public Object createKey (String str, int num) {
      return new StringIntKey (str, num);
   }

   public void clear(GL2 gl) {
      if (myDisplayLists != null) {
         clearDisplayLists (gl);
      } else {
         myDisplayLists = new HashMap<>();
         myKeyMap = new HashMap<>();
      }
      if (myFreeLists != null) {
         clearFreeLists (gl);
      } else {
         myFreeLists = new LinkedList<>();
      }
      resetP = false;
   }
   
   public void dispose(GL2 gl) {
      clear(gl);
   }
   
   public void releaseUnused(GL2 gl) {
      LinkedList<Object> toRemove = new LinkedList<>();
      synchronized(myDisplayLists) {
         for (Entry<Object,DisplayListInfo> entry : myDisplayLists.entrySet()) {
            DisplayListInfo li = entry.getValue();
            if (li.getUseCount() <= 0) {
               toRemove.add(entry.getKey());
            }
         }
      }

      for (Object key : toRemove) {
         removeDisplayList(gl, key);
      }
   }
   
   public void resetUseCounts(GL2 gl) {
      synchronized(myDisplayLists) {
         for (Entry<Object,DisplayListInfo> entry : myDisplayLists.entrySet()) {
            DisplayListInfo li = entry.getValue();
            li.resetUseCount();
         }
      }
   }
   
}
