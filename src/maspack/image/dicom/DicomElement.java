/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import maspack.matrix.VectorNd;
import maspack.util.InternalErrorException;

/**
 * DICOM header element
 */
public class DicomElement {
   
   static boolean timeWarning = false;
   
   /**
    * DICOM Value representation
    */
   public static enum VR {
      AE('A', 'E', "Application Entity"), 
      AS('A', 'S', "Age String"),
      AT('A', 'T', "Attribute Tag"), 
      CS('C', 'S', "Code String"), 
      DA('D', 'A', "Date"), 
      DS('D', 'S', "Decimal String"), 
      DT('D', 'T', "Date Time"), 
      DL('D', 'L', "Delimiter"), 
      FL('F', 'L', "Floating Point Single"),
      FD('F', 'D', "Floating Point Double"), 
      IS('I', 'S', "Integer String"), 
      LO('L', 'O', "Long String"), 
      LT('L', 'T', "Long Text"), 
      OB('O', 'B', "Other Byte String"), 
      OF('O', 'F', "Other Float String"), 
      OW('O', 'W', "Other Word String"), 
      PN('P', 'N', "Person Name"), 
      SH('S', 'H', "Short String"),
      SL('S', 'L', "Signed Long"), 
      SQ('S', 'Q', "Sequence of Items"), 
      SS('S', 'S', "Signed Short"), 
      ST('S', 'T', "Short Text"), 
      TM('T', 'M', "Time"), 
      UI('U', 'I', "Unique Identifier"), 
      UL('U', 'L', "Unsigned Long"), 
      UN('U', 'N', "Unknown"), 
      US('U', 'S', "Unsigned Short"), 
      UT('U', 'T', "Unlimited Text"),
      OX('O', 'X', "One of OB, OW, OF"); /*false type*/

      public String desc;
      public char c0;
      public char c1;
      private VR(char c0, char c1, String desc) {
         this.desc = desc;
         this.c0 = c0;
         this.c1 = c1;
      }

      /**
       * Find VR element based on 2-character representation
       * @return the VR entity
       */
      public static VR get(char c0, char c1) {
         for (VR vr : VR.values()) {
            if (vr.c0 == c0 && vr.c1 == c1) {
               return vr;
            }
         }
         return null;
      }

   }

   int tagId;
   VR vr;
   Object value;

   public DicomElement(int tagId, VR type, Object data) {
      this.tagId = tagId;
      this.vr = type;
      this.value = data;
   }
   
   /**
    * @return the DICOM tag id
    */
   public int getTag() {
      return tagId;
   }
   
   /**
    * @return the element's VR
    */
   public VR getVR() {
      return  vr;
   }
   
   /**
    * @return the RAW value extracted from the DICOM file
    */
   public Object getValue() {
      return value;
   }
   
   /**
    * @return the 'best-guess' type of value based on the VR
    */
   public Object getParsedValue() {
      switch (vr) {
         case AE:
         case AS:
         case CS:
         case LT:
         case ST:
         case UI:
         case UN:
         case UT:
            // string
            return (String)value;
         case AT:
            // binary integer
            return (int[])value;
         case DA:
            return parseDate((String)value);
         case DL:
         case SQ:
            // sequence and delimiter have null value
            return null;
         case DS:
            return parseMultiDecimalValue((String)value);
         case DT:
            return parseDateTime((String)value);
         case FD:
            return (double[])value;
         case FL:
            return (float[])value;
         case IS:
            return parseMultiIntValue((String)value);
         case OB:
            return (byte[])value;
         case OF:
            return (float[])value;
         case OW:
            return (short[])value;
         case OX:
            return value;
         case LO:
         case PN:
         case SH:
            String str = ((String)value).trim();
            return str.split("\\\\");
         case SL:
         case UL:
            return (int[])value;
         case SS:
         case US:
            return (short[])value;
         case TM:
            return parseTime((String)value);
         default:
            break;         
      }
      return value;
   }
   
   /**
    * Determines the integer represented by the supplied string
    * @param in string representation
    * @return represented integer
    */
   public static int parseIntString(String in) {
      return convertCharsToInt(in.toCharArray(), 0, in.length());
   }
   
   private static int convertCharsToInt(char[] array, int offset,  int len) {

      int out = 0;
      boolean negative = false;
      boolean hasSign = false;

      for (int i=0; i<len; i++) {
         char c = (char)array[i];

         if ( c >= '0' || c <= '9') {
            hasSign = true;
            out = out * 10 + c -'0';
         } else if ( c == '-' && !hasSign) {
            negative = true;
            hasSign = true;
         } else if ( c == '+' && !hasSign) {
            hasSign = true;
         } else {
            // illegal character
            break;
         }
      }

      if (negative) {
         out = -out;
      }

      return out;

   }

   /**
    * Determines the decimal represented by the supplied string
    * @param in string representation
    * @return represented decimal number
    */
   public static double parseDecimalString(String in) {

      StringBuilder s = new StringBuilder();

      boolean hasSign = false;
      boolean hasExponent = false;
      boolean hasExponentSign = false;
      boolean hasDecimal = false;

      for (int i=0; i<in.length(); i++) {

         char c = in.charAt(i);

         if (c >= '0' && c <= '9') {
            s.append(c);
            if (hasExponent) {
               if (!hasExponentSign) {
                  hasExponentSign = true;
               }
            } else {
               if (!hasSign) {
                  hasSign = true;
               }
            }
         } else if (c == '.' && !hasDecimal && !hasExponent) {
            s.append(c);
            hasDecimal = true;
         } else if ( (c == '-' || c == '+') && !hasSign && !hasExponent ) {
            s.append(c);
            hasSign = true;
         }  else if ( (c == '-' || c == '+') && !hasExponentSign && hasExponent ) {
            s.append(c);
            hasExponentSign = true;
         } else if ( (c == 'e' || c == 'E')  && !hasExponent) {
            s.append(c);
            hasExponent = true;
         } else {
            if (s.length() > 0) {
               return Double.parseDouble(s.toString());
            }
            return 0;
         } // end checking character

      } // end looping through characters

      if (s.length() > 0) {
         return Double.parseDouble(s.toString());
      }
      return 0;

   }

   /**
    * Determines the decimal vector represented by the supplied string
    * @param in string representation
    * @return represented vector
    */
   public static VectorNd parseMultiDecimalString(String in) {

      VectorNd out = new VectorNd();
      StringBuilder s = new StringBuilder();

      boolean hasSign = false;
      boolean hasExponent = false;
      boolean hasExponentSign = false;
      boolean hasDecimal = false;

      for (int i=0; i<in.length(); i++) {

         char c = in.charAt(i);

         if (c >= '0' && c <= '9') {
            s.append(c);
            if (hasExponent) {
               if (!hasExponentSign) {
                  hasExponentSign = true;
               }
            } else {
               if (!hasSign) {
                  hasSign = true;
               }
            }
         } else if (c == '.' && !hasDecimal && !hasExponent) {
            s.append(c);
            hasDecimal = true;
         } else if ( (c == '-' || c == '+') && !hasSign && !hasExponent ) {
            s.append(c);
            hasSign = true;
         }  else if ( (c == '-' || c == '+') && !hasExponentSign && hasExponent ) {
            s.append(c);
            hasExponentSign = true;
         } else if ( (c == 'e' || c == 'E')  && !hasExponent) {
            s.append(c);
            hasExponent = true;
         } else {
            if (s.length() > 0) {
               // add new number
               out.setSize(out.size() + 1);
               out.set(out.size()-1, Double.parseDouble(s.toString()));

               hasSign = false;
               hasExponent = false;
               hasExponentSign = false;
               hasDecimal = false;
               s = new StringBuilder();
            }
         } // end checking character

      } // end looping through characters

      if (s.length() > 0) {
         // add new number
         out.setSize(out.size() + 1);
         out.set(out.size()-1, Double.parseDouble(s.toString()));
      }

      return out;
   }

   private String toTagString(int tag) {
      int groupId = (tagId & 0xFFFF0000) >>> 16;
      int elemId = (tagId & 0x0000FFFF);
      String out = String.format("0x%04X", groupId) + "," + String.format("0x%04X", elemId);
      return out;
   }

   @Override
   public String toString() {
      StringBuilder out = new StringBuilder();

      out.append(toTagString(tagId));
      out.append(" ");
      out.append(DicomTag.getName(tagId));
      out.append(" (");
      out.append(vr);
      out.append("): ");

      switch (vr) {
         case AE:
         case AS: 
         case CS:
         case DA:
         case DS:
         case DT:
         case IS:
         case LO:
         case LT:
         case PN:
         case SH:
         case ST:
         case TM:
         case UI:
         case UT:
         {
            out.append(value.toString());
            break;
         }
         case AT: {
            int[] tags = (int[])value;
            out.append("{ ");
            if (tags.length > 0) {
               out.append('[');
               out.append(tags[0]);
               out.append('[');
               for (int i=1; i<tags.length; i++ ) {
                  out.append(",[");
                  out.append(toTagString(tags[i]));
                  out.append("]");
               }
            }
            
            out.append(" }");

            break;
         }

         case DL: {
            if (value instanceof DicomElement[]) {
               DicomElement[] elems = (DicomElement[])value;
               out.append("{ ");
               if (elems.length > 0) {
                  out.append("[");
                  out.append(elems[0].toString());
                  out.append("]");
                  for (int i=1; i<elems.length; i++) {
                     out.append(",[");
                     out.append(elems[i].toString());
                     out.append("]");
                  }
               }
               out.append(" }");
            }
            break;
         }
         case FD: {
            double[] f = (double[])value;
            if (f.length > 0) {
               out.append(f[0]);
               for (int i=1; i<f.length; i++) {
                  out.append(',');
                  out.append(f[i]);
               }
            }
            break;
         }
         case FL: 
         case OF: {
            float[] f = (float[])value;
            if (f.length > 0) {
               out.append(f[0]);
               for (int i=1; i<f.length; i++) {
                  out.append(',');
                  out.append(f[i]);
               }
            }
            break;
         }
         case OB: 
         case UN: {
            byte[] b = (byte[])value;
            if (b.length > 0) {
               out.append(String.format("0x%02X", b[0]) );
               for (int i=1; i<b.length; i++) {
                  out.append(',');
                  out.append(String.format("0x%02X", b[i]) );
               }
            }
            break;
         }
         case OW: {
            short[] w = (short[])value;
            if (w.length > 0) {
               out.append(String.format("0x%04X", w[0]) );
               for (int i=1; i<w.length; i++) {
                  out.append(',');
                  out.append(String.format("0x%04X", w[i]) );
               }
            }
            break;
         }
         case OX:
            break;
         case SL: {
            int[] vals = (int[])value;
            if (vals.length > 0) {
               out.append(vals[0]);
               for (int i=1; i<vals.length; i++) {
                  out.append(',');
                  out.append(vals[i]);
               }
            }
            break;
         }
         case SQ: {
            DicomElement[] elems = (DicomElement[])value;
            out.append("{ ");
            if (elems.length > 0) {
               out.append("[");
               out.append(elems[0].toString());
               out.append("]");
               for (int i=1; i<elems.length; i++) {
                  out.append(",[");
                  out.append(elems[i].toString());
                  out.append("]");
               }
            }
            out.append(" }");
            break;
         }
         case SS: {
            short[] vals = (short[])value;
            if (vals.length > 0) {
               out.append(vals[0]);
               for (int i=1; i<vals.length; i++) {
                  out.append(',');
                  out.append(vals[i]);
               }
            }
            break;
         }
         case UL: {
            int[] vals = (int[])value;
            if (vals.length > 0) {
               out.append(vals[0] & 0x00000000FFFFFFFFl);
               for (int i=1; i<vals.length; i++) {
                  out.append(',');
                  out.append(vals[i] & 0x00000000FFFFFFFFl);
               }
            }
            break;
         }
         case US: {
            short[] vals = (short[])value;
               if (vals.length > 0) {
               out.append(vals[0] & 0xFFFF);
               for (int i=1; i<vals.length; i++) {
                  out.append(',');
                  out.append(vals[i] & 0xFFFF);
               }
               }
            break;
         }
         default:
            out.append(value.toString());

      }

      return out.toString();
   }
   
   public static DicomDateTime parseDateTime(String in) {
      // YYYYMMDDHHMMSS.FFFFFF&ZZZZ
      String dtStr = in;
      
      // optional offset
      int offsetMinutes = 0;
      int idSign = dtStr.indexOf('+');
      if (idSign < 0) {
         idSign = dtStr.indexOf('-');
      }
      if (idSign >= 0) {
         String strOffset = dtStr.substring(idSign);
         dtStr = dtStr.substring(0, idSign);
         
         boolean minus = (dtStr.charAt(0) == '-');
         strOffset = strOffset.substring(1);
         if (strOffset.length() == 2) {
            offsetMinutes = 60*Integer.parseInt(strOffset);
         } else if (strOffset.length() == 4) {
            offsetMinutes = 60*Integer.parseInt(strOffset.substring(0, 2))
               + Integer.parseInt(strOffset.substring(2,  4));
         } else {
            throw new InternalErrorException(
               "Date offset does not adhere to proper format. (" + strOffset + ")");
         }
         if (minus) {
            offsetMinutes = -offsetMinutes;
         }
      }
      
      // parse actual Date/Time
      int micros = 0;
      int idPeriod = dtStr.indexOf('.');
      if (idPeriod >= 0) {
         String strDecimal = dtStr.substring(idPeriod);
         dtStr = dtStr.substring(0, idPeriod);
         micros = Math.round(Float.parseFloat(strDecimal)*1000000);
      }
      
      // YYYYMMDDHHMMSS
      int year = 1970;
      int month = 1;
      int date = 1;
      int hour = 0;
      int min = 0;
      int sec = 0;
      String substr;
      switch (dtStr.length()) {
         case 14: {
            substr = dtStr.substring(12);
            sec = Integer.parseInt(substr);
            dtStr = dtStr.substring(0, 12);
         }
         case 12: {
            substr = dtStr.substring(10);
            min = Integer.parseInt(substr);
            dtStr = dtStr.substring(0, 10);
         }
         case 10: {
            substr = dtStr.substring(8);
            hour = Integer.parseInt(substr);
            dtStr = dtStr.substring(0, 8);
         }
         case 8: {
            substr = dtStr.substring(6);
            date = Integer.parseInt(substr);
            dtStr = dtStr.substring(0, 6);
         }
         case 6: {
            substr = dtStr.substring(4);
            month = Integer.parseInt(substr);
            dtStr = dtStr.substring(0, 4);
         }
         case 4: {
            year = Integer.parseInt(dtStr);
            break;
         } 
         default: {
            throw new InternalErrorException(
               "Date/Time string in invalid format (" + dtStr + ")");
         }
            
      }
      
      DicomDateTime dt = new DicomDateTime(year, month, date, hour, min, sec, micros); 
      if (offsetMinutes != 0) {
         dt.addTimeMinutes(offsetMinutes);
      }
      return dt;
   }
   
   public static DicomDateTime parseDate(String in) {
      // YYYYMMDD or YYYY.MM.DD
      String dtStr = in;
      // remove periods
      dtStr = dtStr.replace(".", "");
   
      int year = 1970;
      int month = 1;
      int date = 1;
      String substr;
      switch (dtStr.length()) {
         case 8: {
            substr = dtStr.substring(6);
            date = Integer.parseInt(substr);
            dtStr = dtStr.substring(0, 6);
         }
         case 6: {
            substr = dtStr.substring(4);
            month = Integer.parseInt(substr);
            dtStr = dtStr.substring(0, 4);
         }
         case 4: {
            year = Integer.parseInt(dtStr);
            break;
         } 
         default: {
            throw new InternalErrorException(
               "Date/Time string in invalid format (" + dtStr + ")");
         }
            
      }
      
      DicomDateTime dt = new DicomDateTime(year, month, date, 0, 0, 0, 0); 
      return dt;
   }
   
   public static DicomDateTime parseTime(String in) {
      // HHMMSS.FFFFFF or HH:MM:SS.FFFFFF
      String dtStr = in;
      // remove colons
      dtStr = dtStr.replace(":", "");
      
      // fraction
      int micros = 0;
      int idPeriod = dtStr.indexOf('.');
      if (idPeriod >= 0) {
         String strDecimal = dtStr.substring(idPeriod);
         dtStr = dtStr.substring(0, idPeriod);
         micros = Math.round(Float.parseFloat(strDecimal)*1000000);
         if (dtStr.length() < 6) {
            if (!timeWarning) {
               System.err.println("Dicom Time warning: non-stardard time format '" + in + "'");
               timeWarning = true;
            }
            dtStr = "000000".substring(0, 6-dtStr.length()) + dtStr;
         }
      }
      
      // HHMMSS
      int hour = 0;
      int min = 0;
      int sec = 0;
      String substr;
      switch (dtStr.length()) {
         case 5:
         case 6: {
            int start = dtStr.length()-2;
            substr = dtStr.substring(start);
            sec = Integer.parseInt(substr);
            dtStr = dtStr.substring(0, start);
         }
         case 3:
         case 4: {
            int start = dtStr.length()-2;
            substr = dtStr.substring(start);
            min = Integer.parseInt(substr);
            dtStr = dtStr.substring(0, start);
         }
         case 1:
         case 2: {
            hour = Integer.parseInt(dtStr);
         }
         case 0: {
            break;
         }
         default: {
            throw new InternalErrorException(
               "Date/Time string in invalid format (" + dtStr + ")");
         }
            
      }
      
      DicomDateTime dt = new DicomDateTime(1970, 1, 1, hour, min, sec, micros); 
      return dt;
   }

   public static double[] parseMultiDecimalValue(String ds) {
      String[] svals = ds.split("\\\\");
      double[] dvals = new double[svals.length];
      for (int i=0; i<svals.length; i++) {
         dvals[i] = Double.parseDouble(svals[i]);
      }
      return dvals;
   }
   
   public static int[] parseMultiIntValue(String is) {
      String[] svals = is.split("\\\\");
      int[] ivals = new int[svals.length];
      for (int i=0; i<svals.length; i++) {
         ivals[i] = Integer.parseInt(svals[i]);
      }
      return ivals;
   }
   
   public double getDecimalValue() {
      switch (vr) {
         case DS: {
            String ds = (String)value;
            return DicomElement.parseDecimalString(ds);
         }
         case FL:
         {
            float[] f =  (float[])value;
            return f[0];
         }
         case FD:
         {
            double[] d =  (double[])value;
            return d[0];
         }
         default:
      }
      return Double.NaN;
   }
   
   public VectorNd getVectorValue() {
      switch (vr) {
         case DS: 
         case IS: {
            String ds = (String)value;
            String[] svals = ds.split("\\\\");
            VectorNd out = new VectorNd(svals.length);
            for (int i=0; i<svals.length; i++) {
               out.set(i, Double.parseDouble(svals[i]));
            }
            return out;
         }
         case SL: {
            int[] vals = (int[])value;
            VectorNd out = new VectorNd(vals.length);
            for (int i=0; i<vals.length; i++) {
               out.set(i, vals[i]);
            }
            return out;
         }
         case UL: {
            int[] vals = (int[])value;
            VectorNd out = new VectorNd(vals.length);
            for (int i=0; i<vals.length; i++) {
               out.set(i, vals[i] & 0xFFFFFFFFl);
            }
            return out;
         }
         case SS:
         {
            short[] vals = (short[])value;
            VectorNd out = new VectorNd(vals.length);
            for (int i=0; i<vals.length; i++) {
               out.set(i, vals[i]);
            }
            return out;
         }
         case US: {
            short[] vals = (short[])value;
            VectorNd out = new VectorNd(vals.length);
            for (int i=0; i<vals.length; i++) {
               out.set(i, vals[i] & 0xFFFF);
            }
            return out;
         }
         case FL:
         {
            float[] vals =  (float[])value;
            VectorNd out = new VectorNd(vals.length);
            for (int i=0; i<vals.length; i++) {
               out.set(i, vals[i]);
            }
            return out;
         }
         case FD:
         {
            double[] vals =  (double[])value;
            VectorNd out = new VectorNd(vals.length);
            for (int i=0; i<vals.length; i++) {
               out.set(i, vals[i]);
            }
            return out;
         }
         default:
            break;
      }
      
      return null;
   }
   
   public String[] getMultiStringValue() {
      switch(vr) {
         case AE:
            break;
         case AS:
            break;
         case CS:
            break;
         case DA:
            break;
         case DT:
            break;
         case LT:
         case ST:
         case UT:
            return new String[]{(String)value};
         case LO: 
         case PN:
         case SH: {
            String str = ((String)value).trim();
            return str.split("\\\\");
         }
         case TM:
            break;
         case UI:
            break;
         default:
            break;
         
      }
    
      return null;
   }
   
   public DicomElement[] getSequenceItemValue() {
      if (vr == VR.DL) {
         return (DicomElement[])(value);
      }
      return null;
   }
   
   public DicomElement[] getSequenceValue() {
      if (vr == VR.SQ) {
         return (DicomElement[])(value);
      }
      return null;
   }
   
   public DicomDateTime getDateTime() {
      switch(vr) {
         case DT: {
            return DicomElement.parseDateTime((String)value);
         }
         case DA: {
            return DicomElement.parseDate((String)value);
         }
         case TM: {
            return DicomElement.parseTime((String)value);
         }
         default:
            return null;
      }
   }
   
   /**
    * Determines the decimal array value of the header element (if represents valid decimal array)
    * @return the value of the header element if exists and is valid, null otherwise
    */
   public double[] getMultiDecimalValue() {
     
      switch (vr) {
         case DS: {
            return DicomElement.parseMultiDecimalValue((String)value);
         }
         case FL:
         {
            float[] f =  (float[])value;
            double[] dvals = new double[f.length];
            for (int i=0; i<f.length; i++) {
               dvals[i] = f[i];
            }
            return dvals;
         }
         case FD:
         {
            return (double[])value;
         }
         default:
      }
      
      return null;
   }
   
   /**
    * Determines the integer array value of the element (if represents valid integer array)
    * @return the value of the header element if exists and is valid, null otherwise
    */
   public int[] getMultiIntValue() {
      switch (vr) {
         case IS: 
         case DS: {
            String is = (String)value;
            String[] svals = is.split("\\\\");
            int[] ivals = new int[svals.length];
            for (int i=0; i<svals.length; i++) {
               ivals[i] = Integer.parseInt(svals[i]);
            }
            return ivals;
         }
         case SL: 
         case UL: {
            return (int[])value;
         }
         case SS:
         {
            short[] vals = (short[])value;
            int[] ivals = new int[vals.length];
            for (int i=0; i<vals.length; i++) {
               ivals[i] = vals[i];
            }
            return ivals;
         }
         case US: {
            short[] vals = (short[])value;
            int[] ivals = new int[vals.length];
            for (int i=0; i<vals.length; i++) {
               ivals[i] = vals[i] & 0xFFFF;
            }
            return ivals;
         }
         default:
            break;
      }
      
      return null;
   }
   
   /**
    * Determines the string representation of the header element
    * @return string representation
    */
   public String getStringValue() {
      if (value instanceof String) {
         return (String)(value);
      } 
      return value.toString();

   }

   /**
    * Determines the integer value of the header element (if represents valid integer)
    * @return the value of the header element if exists and is valid
    */
   public int getIntValue() {
      switch (vr) {
         case DS:
         case IS: {
            String is = (String)value;
            return DicomElement.parseIntString(is);
         }
         case SL: 
         case UL: {
            int[] vals = (int[])value;
            return vals[0];
         }
         case SS:
         {
            short[] vals = (short[])value;
            return vals[0];
         }
         case US: {
            short[] vals = (short[])value;
            return vals[0] & 0xFFFF;
         }
         default:
            return -1;
      }
   }
}
