/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.*;
import maspack.util.InternalErrorException;

/**
 * Used by CompositeComponents to map between names and numbers and
 * ModelComponents.
 */
public class ComponentMap {
   private HashMap<String,ModelComponent> myNameMap; // maps names to components
   private int[] myNumberMap;    // maps numbers to compnent indices
   private int myDefaultNumberCapacity;

   private int myNumberLimit;    // next number beyond the current highest number
   private int[] myNumberStack;    // stack to implement the free number cache
   private int myNumberStackSize;  // size of free number stack
   private boolean myNumberCacheValid = true;
   
   private boolean myOneBasedNumbering = false;

   // === begin free number cache methods ====

   /**
    * Allocate a number, either from the free cache, or by bumping the number
    * limit.
    */
   private int allocNumber() {
      if (!myNumberCacheValid) {
         rebuildNumberCache();
      }
      int num;
      if (myNumberStackSize > 0) {
         num = myNumberStack[--myNumberStackSize];
      }
      else {
         num = myNumberLimit;
      }
      if (num >= myNumberLimit) {
         myNumberLimit = num + 1;
      }
      return num;
   }

   /**
    * Print the number cache. For debugging.
    */
   void printNumberCache () {
      for (int i=myNumberStackSize-1; i>=0; i--) {
         System.out.println (" " + myNumberStack[i]);
      }
   }

   /**
    * Clear the number cache.
    */
   private void clearNumberCache() {
      myNumberStackSize = 0;
      myNumberCacheValid = true;
   }

   /**
    * Returns the next number that will be allocated, without actually doing
    * the allocation.
    * 
    * @return next number that will be allocated
    */
   int nextNumber() {
      if (!myNumberCacheValid) {
         rebuildNumberCache();
      }
      if (myNumberStackSize > 0) {
         return myNumberStack[myNumberStackSize - 1];
      }
      else {
         return myNumberLimit;
      }
   }

   /**
    * Free a number by placing it on the number cache.
    */
   private void freeNumber (int num) {
      if (!myNumberCacheValid) {
         rebuildNumberCache();
      }
      doFreeNumber (num);
   }

   private void doFreeNumber (int num) {
      if (myNumberStackSize >= myNumberStack.length) { // grow the number cache
         int newLength = (3 * myNumberStack.length) / 2 + 1;
         int[] newStack = new int[newLength];
         System.arraycopy (myNumberStack, 0, newStack, 0, myNumberStack.length);
         myNumberStack = newStack;
      }
      myNumberStack[myNumberStackSize++] = num;
      if (num == myNumberLimit - 1) {
         int k = num - 1;
         while (k >= 0 && myNumberMap[k] == -1) {
            k--;
         }
         myNumberLimit = k + 1;
      }
   }

   /**
    * Rebuild the free number cache.
    */
   void rebuildNumberCache() {
      int base = myOneBasedNumbering ? 1 : 0;
      int highestNum = myNumberMap.length - 1;
      // Iterate backwards from the end of the numberMap and find the first
      // entry != -1. This is the highest currently allocated number.
      while (highestNum >= base && myNumberMap[highestNum] == -1) {
         highestNum--;
      }
      myNumberLimit = highestNum + 1;
      myNumberStackSize = 0;
      // Add any unallocated numbers *below* the highest number to the number
      // stack.
      for (int i = highestNum - 1; i >= base; i--) {
         if (myNumberMap[i] == -1) {
            doFreeNumber (i);
         }
      }
      myNumberCacheValid = true;
   }

   // === end free number cache methods ====

   public int mapComponent (ModelComponent comp, int idx, int number) {

      if (number == 0 && myOneBasedNumbering) {
         throw new InternalErrorException (
            "mapComponent: number=0 with one-based numbering in effect");
      }
      if (number == -1) {
         number = allocNumber();
      }
      else if (myNumberCacheValid && number == nextNumber()) {
         // if number == nextNumber, call allocNumber() to update the number
         // cache. Only do this if the cache is valid, to avoid lots of calls
         // to rebuildNumberCache() when the numbers don't match nextNumber()
         allocNumber();
      }
      else {
         // will have to rebuild number cache later
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

   private void initialize() {
      myNumberMap = new int[myDefaultNumberCapacity];
      for (int i = 0; i < myDefaultNumberCapacity; i++) {
         myNumberMap[i] = -1;
      }
      myNumberStackSize = 0;
      myNumberLimit = myOneBasedNumbering ? 1 : 0;
      myNumberStack = new int[myDefaultNumberCapacity];
   }

   public ComponentMap() {
      myDefaultNumberCapacity = 64;
      myNameMap = new HashMap<String,ModelComponent>();
      initialize();
   }

   public void clear() {
      myNameMap.clear();
      initialize();
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

   private int putIndex (int num, int idx) {
      ensureNumberCapacity (num + 1);
      int prev = myNumberMap[num];
      myNumberMap[num] = idx;
      return prev;
   }

   private int removeIndex (int num) {
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

   private void put (String name, ModelComponent comp) {
      myNameMap.put (name, comp);
   }

   private ModelComponent remove (String name) {
      return myNameMap.remove (name);
   }

   private void put (String name, int num, ModelComponent comp, int idx) {
      if (name != null) {
         put (name, comp);
      }
      putIndex (num, idx);
   }

   private void remove (String name, int num) {
      if (name != null) {
         remove (name);
      }
      removeIndex (num);
   }

   public int getNumberLimit() {
      if (!myNumberCacheValid) {
         rebuildNumberCache();
      }
      return myNumberLimit;
   }

   /**
    * Reset the number map so that numbers and indices match.  The number of
    * components is given by {@code numc}. This also disables one-based
    * numbering, if it was set.
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
      myNumberLimit = numc;
      myOneBasedNumbering = false;
      clearNumberCache();
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
    * Queries whether the numbering in this component map is one-based.
    */
   boolean getOneBasedNumbering() {
      return myOneBasedNumbering;
   }
   
   /**
    * Sets whether the numbering in this component map is one-based.
    */  
   void setOneBasedNumbering (boolean oneBased) {
      if (oneBased != myOneBasedNumbering) {
         if (oneBased) {
            incrementNumbering();
         }
         else {
            decrementNumbering();
         }
         myOneBasedNumbering = oneBased;
      }
   }
   
   /**
    * Increment all number values by one
    */
   void incrementNumbering () {
      ensureNumberCapacity (myNumberLimit+1);
      // shift all entries up by 1
      for (int i=myNumberMap.length-1; i>=1; i--) {
         myNumberMap[i] = myNumberMap[i-1];
      }
      // fill first entry with -1
      myNumberMap[0] = -1;
      // invalidate number cache
      myNumberCacheValid = false; 
   }

   /**
    * Decrement all number values by one
    */
   void decrementNumbering () {
      if (myNumberMap[0] != -1) {
         throw new InternalErrorException (
            "decrement numbering: number 0 is in use");
      }
      // shift all entries down by 1      
      for (int i=0; i<myNumberMap.length-1; i++) {
         myNumberMap[i] = myNumberMap[i+1];
      }
      // fill last entry with -1
      myNumberMap[myNumberMap.length-1] = -1;
      // invalidate number cache
      myNumberCacheValid = false; 
   }

}
