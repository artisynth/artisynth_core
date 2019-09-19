/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.*;
import java.io.*;
import maspack.util.*;
import artisynth.core.util.*;
import artisynth.core.modelbase.ScanWriteUtils.ClassInfo;
import java.lang.reflect.*;

public class ScannableList<C extends Scannable> // extends AbstractList<C>
   implements Scannable, Collection<C> {

   protected Class<C> myComponentType;
   protected C[] myArray;
   protected int mySize = 0;
   protected int modCount = 0;

   private class MyIterator implements Iterator<C> {
      int myExpectedModCount;
      int myIdx;
      int myLastIdx;

      MyIterator() {
         myIdx = 0;
         myLastIdx = -1;
         myExpectedModCount = modCount;
      }

      public boolean hasNext() {
         return myIdx < mySize;
      }

      public C next() {
         if (myExpectedModCount != modCount) {
            throw new ConcurrentModificationException(
               myExpectedModCount + " " + modCount);
         }
         try {
            C value = get (myIdx);
            myLastIdx = myIdx++;
            return value;
         }
         catch (IndexOutOfBoundsException e) {
            if (myExpectedModCount != modCount) {
               throw new ConcurrentModificationException();
            }
            throw new NoSuchElementException();
         }
      }

      public void remove() {
         if (myLastIdx == -1) {
            throw new IllegalStateException();
         }
         if (myExpectedModCount != modCount) {
            throw new ConcurrentModificationException();
         }
         try {
            doRemove (myLastIdx);
            myIdx--;
            myLastIdx = -1;
            myExpectedModCount = modCount;
         }
         catch (IndexOutOfBoundsException e) {
            throw new ConcurrentModificationException();
         }
      }
   }

   public void ensureCapacity (int minCap) {
      modCount++;
      int oldCap = myArray.length;
      if (minCap > oldCap) {
         int newCap = (3 * oldCap) / 2 + 1;
         if (newCap < minCap) {
            newCap = minCap;
         }
         C[] newArray = createArray (newCap);
         System.arraycopy (myArray, 0, newArray, 0, mySize);
         myArray = newArray;
      }
   }

   protected C[] createArray (int length) {
      return (C[])Array.newInstance (myComponentType, length);
   }

   public int indexOf (C obj) {
      if (obj == null) {
         for (int i = 0; i < mySize; i++) {
            if (myArray[i] == null) {
               return i;
            }
         }
      }
      else {
         for (int i = 0; i < mySize; i++) {
            if (obj.equals (myArray[i])) {
               return i;
            }
         }
      }
      return -1;
   }

   public Iterator<C> iterator() {
      return new MyIterator();
   }

   public boolean isEmpty() {
      return mySize == 0;
   }

   public Object[] toArray() {
      Scannable[] newArray = new Scannable[mySize];
      System.arraycopy (myArray, 0, newArray, 0, mySize);
      return newArray;
   }

   public <C> C[] toArray (C[] array) {
      if (array.length < mySize) {
         C[] newArray =
            (C[])Array.newInstance (
               array.getClass().getComponentType(), mySize);
         System.arraycopy (myArray, 0, newArray, 0, mySize);
         return newArray;
      }
      else {
         System.arraycopy (myArray, 0, array, 0, mySize);
         if (array.length > mySize) {
            array[mySize] = null;
         }
         return array;
      }
   }

   public boolean contains (C obj) {
      return indexOf (obj) != -1;
   }

   public C get (int idx) {
      if (idx < 0 || idx >= mySize) {
         throw new IndexOutOfBoundsException ("Index: " + idx + ", Size: "
         + mySize);
      }
      return myArray[idx];
   }

   public C set (int idx, C obj) {
      if (idx < 0 || idx >= mySize) {
         throw new IndexOutOfBoundsException ("Index: " + idx + ", Size: "
         + mySize);
      }
      C prev = myArray[idx];
      myArray[idx] = obj;
      return prev;
   }

   public int size() {
      return mySize;
   }

   public void clear() {
      modCount++;
      for (int i = 0; i < mySize; i++) {
         myArray[i] = null;
      }
      mySize = 0;
   }

   public boolean add (C obj) {
      ensureCapacity (mySize + 1); // modCount incremented here
      myArray[mySize++] = obj;
      return true;
   }

   public void add (int idx, C obj) {
      if (idx > mySize || idx < 0) {
         throw new IndexOutOfBoundsException ("Index: " + idx + ", Size: "
         + (mySize + 1));
      }
      ensureCapacity (mySize + 1); // modCount incremented here
      int numMoved = mySize - idx;
      if (numMoved > 0) {
         System.arraycopy (myArray, idx, myArray, idx + 1, numMoved);
      }
      myArray[idx] = obj;
      mySize++;
   }

   public boolean addAll (Collection<? extends C> c) {
      boolean modified = false;
      ensureCapacity (mySize + c.size());
      Iterator<? extends C> e = c.iterator();
      while (e.hasNext()) {
         if (add(e.next()))
            modified = true;
      }
      return modified;
    }     

   public boolean remove (Object obj) {
      if (obj == null) {
         for (int i = 0; i < mySize; i++) {
            if (myArray[i] == null) {
               doRemove (i);
               return true;
            }
         }
      }
      else {
         for (int i = 0; i < mySize; i++) {
            if (obj.equals (myArray[i])) {
               doRemove (i);
               return true;
            }
         }
      }
      return false;
   }

   public boolean retainAll (Collection<?> c) {
      throw new UnsupportedOperationException();
   }

   public boolean removeAll (Collection<?> c) {
      throw new UnsupportedOperationException();
   }

   private void doRemove (int idx) {
      modCount++;
      int numMoved = mySize - idx - 1;
      if (numMoved > 0) {
         System.arraycopy (myArray, idx + 1, myArray, idx, numMoved);
      }
      myArray[--mySize] = null;
   }

   public C remove (int idx) {
      if (idx >= mySize) {
         throw new IndexOutOfBoundsException ("Index: " + idx + " Size: "
         + mySize);
      }
      C oldValue = myArray[idx];
      doRemove (idx);
      return oldValue;
   }

   public ScannableList (Class<C> type) {
      super();
      myComponentType = type;
      myArray = createArray (10);
   }

   public ScannableList (Class<C> type, int cap) {
      super();
      myComponentType = type;
      myArray = createArray (cap);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isWritable() {
      return true;
   }
   
   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      if (size() == 0) {
         pw.println ("[ ]");
      }
      else {
         pw.print ("\n[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (int i = 0; i < size(); i++) {
            C comp = get (i);
            if (!comp.getClass().isAssignableFrom (myComponentType)) {
               pw.println (ScanWriteUtils.getClassTag(comp));
            }
            comp.write (pw, fmt, ref);
         }        
         //writeItems (pw, fmt, ref);
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   /**
    * Used for scanning: calls createComponent() and throws an appropriate
    * IOException if anything goes wrong. If <code>warnOnly</code> is
    * <code>true</code> and <code>classInfo</code> is non-<code>null</code>,
    * then if the class can't be instantiated, the method prints a warning
    * and returns <code>null</code>.
    */
   protected C newComponent (
      ReaderTokenizer rtok, ClassInfo<C> classInfo, boolean warnOnly) 
      throws IOException {
      C comp = null;
      try {
         comp = createComponent (classInfo);
      }
      catch (Exception e) {
         String errMsg;
         if (classInfo == null) {
            errMsg = "Could not instantiate default type " + getTypeParameter();
         }
         else {
            errMsg = "Could not instantiate type " + classInfo.toString();
            if (warnOnly) {
               System.out.println (
                  "WARNING: " + errMsg + ": " + e.getMessage());
               return null;
            }
         }
         throw new IOException (
            errMsg + ", line " + rtok.lineno(), e);
      }
      if (comp == null) {
         throw new IOException (
            "createComponent() returned null, line " + rtok.lineno());
      }
      return comp;
   }

   /**
    * Looks for and scans a class tag that either consists of a simple class
    * name or alias, such as <code>ModelComponent</code>, or a parameterized
    * class name, such as {@code ModelList<FemModel3d>}, and returns
    * either the associated class information, or <code>null</code> if no class
    * tag is found.
    */
   protected ClassInfo<C> scanClassTagIfPresent (ReaderTokenizer rtok)
      throws IOException {
      if (rtok.ttype == ReaderTokenizer.TT_WORD) {
         rtok.pushBack();
         return ScanWriteUtils.scanClassInfo (rtok, getTypeParameter());
      }
      else {
         rtok.pushBack();
         return null;
      }
   }

   protected C scanComponent (
      ReaderTokenizer rtok, ClassInfo<C> classInfo, Object ref)
      throws IOException {
      C comp = newComponent(rtok, classInfo, /*warnOnly=*/false);
      comp.scan (rtok, ref);
      return comp;
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      clear();
      // scanHeader (rtok);
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         ClassInfo<C> classInfo = scanClassTagIfPresent(rtok);
         C comp = scanComponent (rtok, classInfo, ref);
         add (comp);
      }
   }

   public C createComponent (ClassInfo<C> classInfo)
      throws InstantiationException, IllegalAccessException,
      InvocationTargetException {
      Class<?> baseClass =
         (classInfo != null ? classInfo.compClass : getTypeParameter());
      if (classInfo != null && classInfo.typeParam != null) {
         Constructor ctor = null;
         try {
            ctor = baseClass.getDeclaredConstructor(Class.class);
         }
         catch (Exception e) {
            throw new UnsupportedOperationException (
               "Class "+baseClass+" does not have a public constructor "+
               "that takes the type parameter as an argument");
         }
         return (C)ctor.newInstance (classInfo.typeParam);
      }
      else {
         return (C)baseClass.newInstance();
      }
   }

   public Class<C> getTypeParameter() {
      return myComponentType;
   }

   public ScannableList<C> copy(int flags) {
      ScannableList<C> list = null;
      try {
         list = (ScannableList<C>)clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException (
            "ScannableList cannot be cloned");
      }

      list.myArray = createArray(myArray.length);
      list.mySize = 0;
      return list;
   }

   public boolean containsAll (Collection<?> c) {
      for (Object obj : c) {
         if (!contains (obj)) {
            return false;
         }
      }
      return true;
   }

   public boolean contains (Object obj) {
      if (obj == null) {
         for (int i=0; i<mySize; i++) {
            if (myArray[i] == null) {
               return true;
            }
         }
      }
      else {
         for (int i=0; i<mySize; i++) {
            if (myArray[i] != null && myArray[i].equals (obj)) {
               return true;
            }
         }
      }
      return false;
   }

}
