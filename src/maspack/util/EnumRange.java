/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.util;

import java.io.*;
import java.util.*;
import java.lang.reflect.Array;

public class EnumRange<E extends Enum<E>> extends RangeBase {

   private E[] myValidEnums;
   private Class<E> myClass;

   public EnumRange (Class<E> cls) {
      myClass = cls;
      myValidEnums = null;
   }

   private E[] copyArray (E[] array) {
      E[] newArray = createArray (array.length);
      for (int i=0; i<array.length; i++) {
         newArray[i] = array[i];
      }
      return newArray;
   }      

   public EnumRange (Class<E> cls, E[] validEnums) {
      myClass = cls;
      myValidEnums = copyArray(validEnums);
   }

   public E[] getValidEnums() {
      E[] validEnums;
      if (myValidEnums == null) {
         validEnums = copyArray(myClass.getEnumConstants());
      }
      else {
         validEnums = copyArray(myValidEnums);
      }
      return validEnums;
   }

   public String getValidEnumsString() {
      String str = "";
      if (myValidEnums != null) {
         for (int i=0; i<myValidEnums.length; i++) {
            str += myValidEnums[i].toString();
            if (i<myValidEnums.length-1) {
               str += " ";
            }
         }
      }
      return str;
   }

   public boolean isValid (Object obj, StringHolder errMsg) {
      if (myClass.isAssignableFrom (obj.getClass())) {
         if (myValidEnums != null) {
            for (int i=0; i<myValidEnums.length; i++) {
               if (obj == myValidEnums[i]) {
                  return true;
               }
            }
            setError (
               errMsg, "enum must be one of "+getValidEnumsString());
            return false;
         }
         return true;
      }
      else {
         return false;
      }
   }

   @SuppressWarnings("unchecked")
   private E[] createArray (int size) {
      return (E[])Array.newInstance(myClass, size);
   }
   
   /**
    * {@inheritDoc}
    */
   @SuppressWarnings("unchecked")
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myValidEnums = null;

      int[] charSaves = rtok.getCharSettings (".$");
      rtok.wordChars (".$");

      rtok.scanToken ('[');
      String className = rtok.scanWord();
      try {
         myClass = (Class<E>)Class.forName(className);
      }
      catch (Exception e) {
         throw new IOException ("class "+className+" not found");
      }
      if (!myClass.isEnum()) {
         throw new IOException ("class "+className+" is not an Enum");
      }
      ArrayList<E> validList = new ArrayList<E>();
      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsWord()) {
            throw new IOException ("word expected, "+rtok);
         }
         E validEnum = null;
         try {
            validEnum = (E)Enum.valueOf (myClass, rtok.sval);
         }
         catch (Exception e) {
            throw new IOException (
               "Enum "+rtok.sval+" not recognized");
         }
         validList.add (validEnum);
      }
      if (validList.size() > 0) {
         myValidEnums =
            (E[])validList.toArray(createArray(0));
      }

      rtok.setCharSettings (".$", charSaves);
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      pw.print (toString());
   }

   public String toString() {
      return ("[ "+myClass.getName()+" "+getValidEnumsString()+" ]");
   }

   private enum TestEnum { FOO, BAR, BAT };      

   public static void main (String[] args) {

      EnumRange<TestEnum> testRange =
         new EnumRange<TestEnum>(
            TestEnum.class, new TestEnum[] {TestEnum.FOO, TestEnum.BAT});

      try {
         StringWriter sw = new StringWriter();
         testRange.write (new PrintWriter(sw), null, null);
         String str = sw.toString();

         System.out.println ("str=" + str);

         ReaderTokenizer rtok =
            new ReaderTokenizer (new StringReader (str));

         testRange.scan (rtok, null);

         System.out.println ("res=" + testRange);
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
      

   }
}