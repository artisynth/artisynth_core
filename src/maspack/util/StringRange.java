/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), 
 * Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */


package maspack.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class StringRange extends RangeBase {

   String[] validStrings;

   private boolean isStringAllowed(String str) {
      if (str.contains ("\"")) {
         return false;
      }
      return true;
   }
   
   public StringRange() {
      validStrings = null;
   }

   public StringRange(Collection<String> valid) {
      if (valid == null) {
         validStrings = null;
      } else {
         for (String str : valid) {
            if (!isStringAllowed (str)) {
               throw new IllegalArgumentException ("String cannot contain the symbols: \"");
            }
         }
         validStrings = valid.toArray (new String[valid.size()]);   
      }

   }

   public StringRange(String... valid) {
      if (valid == null) {
         validStrings = null;
      } else {
         for (String str : valid) {
            if (!isStringAllowed (str)) {
               throw new IllegalArgumentException ("String cannot contain the symbols: \"");
            }
         }
         validStrings = Arrays.copyOf (valid, valid.length);
      }
   }

   public String[] getValidStrings() {
      if (validStrings == null) {
         return null;
      }
      return Arrays.copyOf (validStrings, validStrings.length);
   }
   
   public boolean isWildcard() {
      return (validStrings == null);
   }

   @Override
   public boolean isValid (Object obj, StringHolder errMsg) {
      if (!(obj instanceof String)) {
         setError(errMsg, "Object not instance of String");
         return false;
      }
      String str = (String)obj;
      if (validStrings==null) {
         return true;
      }

      for (int i=0; i<validStrings.length; i++) {
         if (validStrings[i].equals (str)) {
            return true;
         }
      }

      setError(errMsg, "String must be one of " + toString());
      return false;
   }

   @Override
   public String toString () {

      if (validStrings == null) {
         return "*";
      }

      StringBuilder sb = new StringBuilder ();
      sb.append ("[ ");
      for (int i=0; i<validStrings.length; i++) {
         sb.append (validStrings[i]);
         sb.append (' ');
      }
      sb.append (']');

      return sb.toString ();

   }

   @Override
   public Object makeValid (Object obj) {
      if (obj instanceof String) {
         String str = (String)obj;

         if (validStrings == null) {
            return str;
         }

         // first check for same case
         for (int i=0; i<validStrings.length; i++) {
            if (validStrings[i].equals(str)) {
               return validStrings[i];
            }
         }

         // then check for different case
         for (int i=0; i<validStrings.length; i++) {
            if (validStrings[i].equalsIgnoreCase (str)) {
               return validStrings[i];
            }
         }
      }

      // cannot find good string
      return Range.IllegalValue;
   }

   @Override
   public boolean isEmpty () {
      if (validStrings != null && validStrings.length == 0 ) {
         return true;
      }
      return false;
   }

   @Override
   public void intersect (Range r) {
      if (!(r instanceof StringRange)) {
         validStrings = new String[0];
      }

      StringRange sr = (StringRange)r;

      // intersect string ranges
      if (validStrings == null) {
         validStrings = sr.getValidStrings();
      } else if (sr.validStrings == null) {
         // keep my strings
      } else {
         // find common strings
         ArrayList<String> common = new ArrayList<String>();
         for (int i=0; i<validStrings.length; i++) {
            boolean found = false;
            for (int j=0; j<sr.validStrings.length; j++) {
               if (validStrings[i].equals (sr.validStrings[j])) {
                  found = true;
                  break;
               }
            }

            if (found) {
               common.add(validStrings[i]);
            }
         }

         validStrings = common.toArray(new String[common.size()]);
      }

   }

   @Override
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.scanToken ('[');
      int nvals = rtok.scanInteger ();
      
      if (nvals < 0) {
         validStrings = null;
      } else {
         validStrings=  new String[nvals];
         for (int i=0; i<nvals; i++) {
            validStrings[i] = rtok.scanQuotedString ('\"');
         }
      }
      rtok.scanToken (']');
   }

   @Override
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
   throws IOException {
      
      int nStrings = -1;
      if (validStrings != null) {
         nStrings = validStrings.length;
      }
      
      pw.print ("[ ");
      pw.print (nStrings);
      pw.print (" ");
      if (validStrings != null) {
         for (int i=0; i<validStrings.length; i++) {
            pw.print("\"" + validStrings[i] + "\"" );
         }
      }
      pw.print("]");
   }

   @Override
   public StringRange clone () {
      StringRange sr = (StringRange)super.clone();
      sr.validStrings = getValidStrings();
      return sr;
   }
   
   public static void main (String[] args) {

      StringRange testRange =
         new StringRange("HELLO", "WORLD", "QUICK", "BROWN", "FOX");

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
