/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.*;

/**
 * Used by CompositeComponents to map between names and numbers and
 * ModelComponents.
 */
public class ComponentMap {
   private HashMap<String,ModelComponent> myNameMap;
   private int[] myNumberMap;
   private int myDefaultNumberCapacity;

   private int myStackSize;
   private int myNumberLimit;
   private int[] myNameStack;
   private boolean myNumberCacheValid = true;

   public int allocNumber() {
      if (!myNumberCacheValid) {
         collectFreeNumbers();
      }
      int num;
      if (myStackSize > 0) {
         num = myNameStack[--myStackSize];
      }
      else {
         num = myNumberLimit;
      }
      if (num >= myNumberLimit) {
         myNumberLimit = num + 1;
      }
      return num;
   }

   public void printNameStack () {
      for (int i=myStackSize-1; i>=0; i--) {
         System.out.println (" " + myNameStack[i]);
      }
   }

   /**
    * Returns the next number that will be allocated, without actually doing the
    * allocation.
    * 
    * @return next number that will be allocated
    */
   public int nextNumber() {
      if (!myNumberCacheValid) {
         collectFreeNumbers();
      }
      if (myStackSize > 0) {
         return myNameStack[myStackSize - 1];
      }
      else {
         return myNumberLimit;
      }
   }

   public void freeNumber (int num) {
      if (!myNumberCacheValid) {
         collectFreeNumbers();
      }
      doFreeNumber (num);
   }

   private void doFreeNumber (int num) {
      if (myStackSize >= myNameStack.length) { // grow the name stack
         int newLength = (3 * myNameStack.length) / 2 + 1;
         int[] newStack = new int[newLength];
         System.arraycopy (myNameStack, 0, newStack, 0, myNameStack.length);
         myNameStack = newStack;
      }
      myNameStack[myStackSize++] = num;
      if (num == myNumberLimit - 1) {
         int k = num - 1;
         while (k >= 0 && myNumberMap[k] == -1) {
            k--;
         }
         myNumberLimit = k + 1;
      }
   }

   public void collectFreeNumbers() {
      int highestNum = myNumberMap.length - 1;
      while (highestNum >= 0 && myNumberMap[highestNum] == -1) {
         highestNum--;
      }
      myNumberLimit = highestNum + 1;
      myStackSize = 0;
      for (int i = highestNum - 1; i >= 0; i--) {
         if (myNumberMap[i] == -1) {
            doFreeNumber (i);
         }
      }
      myNumberCacheValid = true;
   }

   public int mapComponent (ModelComponent comp, int idx) {
      return mapComponent (comp, idx, -1);
   }

   public int mapComponent (ModelComponent comp, int idx, int number) {
      if (number == -1) {
         number = allocNumber();
      }
      else if (number == nextNumber()) {
         allocNumber();
      }
      else {
         // will have to rebuild number cache later since number is being
         // specified outside of the cache
         myNumberCacheValid = false; 
      }
      put (comp.getName(), number, comp, idx);
      if (comp instanceof ComponentList) {
         String shortName = ((ComponentList<?>)comp).getShortName();
         if (shortName != null) {
            put (shortName, comp);
         }
      }
      return number;
   }

   public void unmapComponent (ModelComponent comp) {
      int num = comp.getNumber();
      remove (comp.getName(), num);
      if (comp instanceof ComponentList) {
         String shortName = ((ComponentList<?>)comp).getShortName();
         if (shortName != null) {
            remove (shortName);
         }
      }
      freeNumber (num);
   }

   public void updateNameMap (
      String newName, String oldName, ModelComponent comp) {
      if (oldName != null) {
         remove (oldName);
      }
      if (newName != null) {
         put (newName, comp);
      }
   }

   private void initializeNameMap() {
      myNumberMap = new int[myDefaultNumberCapacity];
      for (int i = 0; i < myDefaultNumberCapacity; i++) {
         myNumberMap[i] = -1;
      }
      myStackSize = 0;
      myNumberLimit = 0;
      myNameStack = new int[myDefaultNumberCapacity];
   }

   public ComponentMap() {
      myDefaultNumberCapacity = 64;
      myNameMap = new HashMap<String,ModelComponent>();
      initializeNameMap();
   }

   public void clear() {
      myNameMap.clear();
      initializeNameMap();
   }

   private void ensureNumberCapacity (int minCap) {
      int oldCap = myNumberMap.length;
      if (minCap > oldCap) {
         int newCap = (3 * oldCap) / 2 + 1;
         if (newCap < minCap) {
            newCap = minCap;
         }
         int[] newNumberMap = new int[newCap];
         System.arraycopy (myNumberMap, 0, newNumberMap, 0, oldCap);
         for (int i = oldCap; i < newCap; i++) {
            newNumberMap[i] = -1;
         }
         myNumberMap = newNumberMap;
      }
   }

   public int getIndex (int num) {
      if (num >= 0 && num < myNumberMap.length) {
         return myNumberMap[num];
      }
      else {
         return -1;
      }
   }

   public int putIndex (int num, int idx) {
      ensureNumberCapacity (num + 1);
      int prev = myNumberMap[num];
      myNumberMap[num] = idx;
      return prev;
   }

   public int removeIndex (int num) {
      if (num < myNumberMap.length) {
         int prev = myNumberMap[num];
         myNumberMap[num] = -1;
         return prev;
      }
      else {
         return -1;
      }
   }

   public ModelComponent get (String name) {
      return myNameMap.get (name);
   }

   public void put (String name, ModelComponent comp) {
      myNameMap.put (name, comp);
   }

   public ModelComponent remove (String name) {
      return myNameMap.remove (name);
   }

   public void put (String name, int num, ModelComponent comp, int idx) {
      if (name != null) {
         put (name, comp);
      }
      putIndex (num, idx);
   }

   public void remove (String name, int num) {
      if (name != null) {
         remove (name);
      }
      removeIndex (num);
   }

   public int getNumberLimit() {
      return myNumberLimit;
   }

   public void resetIndex (ModelComponent comp, int idx) {
      myNumberMap[comp.getNumber()] = idx;
   }

   public ModelComponent getByNameOrNumber (
      String nameOrNumber, IndexedComponentList list) {
      if (Character.isDigit (nameOrNumber.charAt (0))) {
         int num = Integer.parseInt (nameOrNumber);
         return getByNumber (num, list);
      }
      else {
         return myNameMap.get (nameOrNumber);
      }
   }

   public ModelComponent getByNumber (int num, IndexedComponentList list) {
      if (num >= myNumberMap.length) {
         return null;
      }
      int idx = myNumberMap[num];
      if (idx != -1) {
         return list.get (idx);
      }
      else {
         return null;
      }
   }

   void printNumberMap() {
      for (int i = 0; i < myNumberMap.length; i++) {
         System.out.println ("" + i + " " + myNumberMap[i]);
      }
   }

   public void startNumberingAtOne() {
      myNumberLimit = 1;
   }
}
