/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.dicom;

import maspack.matrix.VectorNd;

/**
 * DICOM header element
 */
public class DicomElement {
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
       * @param c0
       * @param c1
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

}