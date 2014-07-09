/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.io.*;
import java.util.*;

import maspack.properties.*;
import maspack.util.*;

public class FormatRange extends RangeBase {

   private String myValidChars;

   public FormatRange () {
      myValidChars = null;
   }

   public FormatRange (String validChars) {
      myValidChars = validChars;
   }

   public boolean isValid (Object obj, StringHolder errMsg) {
      if (obj instanceof String) {
         String fmtStr = (String)obj;
         NumberFormat fmt = null;
         try {
            fmt = new NumberFormat(fmtStr);
         }
         catch (Exception e){
            setError (errMsg, e.getMessage());
            return false;
         }
         if (myValidChars != null) {
            if (myValidChars.indexOf (fmt.getConversionChar()) == -1) {
               setError (
                  errMsg, "format character must be one of '"+myValidChars+"'");
               return false;
            }
         }
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myValidChars = null;
      rtok.scanToken ('[');
      rtok.nextToken();
      if (rtok.tokenIsQuotedString ('\'')) {
         myValidChars = new String(rtok.sval);
      }
      else {
         rtok.pushBack();
      }
      rtok.scanToken (']');
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      if (myValidChars == null) {
         pw.println ("[ ]");
      }
      else {
         pw.println ("[ '"+myValidChars+"' ]");
      }
   }
   
}