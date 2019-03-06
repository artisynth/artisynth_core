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
   private HashMap<String,ModelComponent> myNameMap; // maps names to components
   private int[] myNumberMap;    // maps numbers to compnent indices
   private int myDefaultNumberCapacity;

   private int myNumberLimit;    // next number beyond the current highest number
   private int[] myNameStack;    // stack of free numbers
   private int myNameStackSize;  // size of free number stack
   private boolean myNumberCacheValid = true;

   public int allocNumber() {
      if (!myNumberCacheValid) {
         collectFreeNumbers();
      }
      int num;
      if (myNameStackSize > 0) {
         num = myNameStack[--myNameStackSize];
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
      for (int i=myNameStackSize-1; i>=0; i--) {
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
      if (myNameStackSize > 0) {
         return myNameStack[myNameStackSize - 1];
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
      if (myNameStackSize >= myNameStack.length) { // grow the name stack
         int newLength = (3 * myNameStack.length) / 2 + 1;
         int[] newStack = new int[newLength];
         System.arraycopy (myNameStack, 0, newStack, 0, myNameStack.length);
         myNameStack = newStack;
      }
      myNameStack[myNameStackSize++] = num;
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
      myNameStackSize = 0;
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
      myNameStackSize = 0;
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

   /**
    * Reset the number map so that numbers and indices match.  The number of
    * components is given by {@code numc}.
    */
   protected void resetNumbersToIndices (int numc) {
      if (myNumberMap.length < numc) {
         myNumberMap = new int[numc];
      }
      for (int i=0; i<numc; i++) {
         myNumberMap[i] = i;
      }
      for (int i=numc; i<myNumberMap.length; i++) {
         myNumberMap[i] = -1;
      }
      // clear the name stack
      myNumberLimit = numc;
      myNameStackSize = 0;
      myNumberCacheValid = true;
   }

   public void resetIndex (ModelComponent comp, int idx) {
      myNumberMap[comp.getNumber()] = idx;
      //myNumberCacheValid = false;
   }
   
   public void clearIndices() {
      for (int i=0; i<myNumberMap.length; ++i) {
         myNumberMap[i] = -1;
      }
      myNumberCacheValid = false;
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

   /**
    * Adjusts all number values by a given increment. This is used to implement
    * zero or one-based numbering.
    */
   void incrementNumbers (int inc) {
      ensureNumberCapacity (myNumberLimit + inc);
      if (inc > 0) {
         int i = myNumberMap.length-1;
         // shift all entries up by inc
         for ( ; i>=inc; i--) {
            myNumberMap[i] = myNumberMap[i-inc];
         }
         // fill first inc entries with -1
         for ( ; i>=0; i--) {
            myNumberMap[i] = -1;
         }
      }
      else if (inc < 0) {
         int i = 0;
         // shift all entries down by -inc
         for ( ; i<myNumberMap.length+inc; i++) {
            myNumberMap[i] = myNumberMap[i-inc];
         }
         // fill last entries with -1
         for ( ; i<myNumberMap.length; i++) {
            myNumberMap[i] = -1;
         }
      }
      myNumberLimit += inc;
      for (int i=0; i<myNameStackSize; i++) {
         myNameStack[i] += inc;
      }
   }

}
