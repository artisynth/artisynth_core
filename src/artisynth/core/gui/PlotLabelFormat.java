package artisynth.core.gui;

import java.text.DecimalFormat;
import java.text.FieldPosition;

/**
 * Class used to format labels when making numeric plots.
 */
public class PlotLabelFormat {

   DecimalFormat myDecFmt = new DecimalFormat ("0.0#######E0");

   public static boolean containsExponent (String str) {
      for (int i=0; i<str.length(); i++) {
         char c = str.charAt (i);
         if (c == 'e' || c == 'E') {
            return true;
         }
      }
      return false;
   }

   private static double displayDigits = 8;

   public double roundDouble (double value) {
      double exp = Math.floor (Math.log10 (Math.abs (value)));
      double base = Math.pow (10, displayDigits - exp);
      
      return Math.round (value * base) / base;
   }

   String createLabel (long m, int exp, boolean forceExponent) {
      if (m == 0) {
        return "0";
      } 
      // ignore sign for now; add it at the end if m < 0
      StringBuilder str = new StringBuilder(Long.toString (Math.abs(m)));

      // find number of trailing zeros:
      int numZeros = 0;
      for (int i=str.length()-1; i>=0; i--) {
         char c = str.charAt(i);
         if (c == '0') {
            numZeros++;
         }
         else {
            break;
         }
      }
      if (exp + numZeros >= 5 || exp + str.length()-1 <= -4 || forceExponent) {
         // convert to scientific notation:
         // delete trailing zeros
         if (numZeros > 0) {
            str.delete (str.length()-numZeros, str.length());
         }
         // get true exponent 
         exp += numZeros + str.length()-1;
         str.insert (1, '.');
         if (str.length() == 2) {
            // add extra zero
            str.append ('0');
         }
         str.append ("E" + exp);
      }
      else {
         if (exp > 0) {
            // add extra trailing zeros
            for (int i=0; i<exp; i++) {
               str.append ('0');
            }
         }
         else if (exp < 0) {
            int expIdx = str.length()+exp; // where to insert decimal point
            if (expIdx > 0) {
               // just insert decimal point
               str.insert (expIdx, '.');
            }
            else {
               while (expIdx < 0) {
                  // add leading zeros
                  str.insert (0, '0');
                  expIdx++;
               }
               str.insert (0, "0.");
            }
         }
      }
      // add sign if negative
      if (m < 0) {
         str.insert (0, '-');
      }
      return str.toString();
   }


   String createLabel (double value, boolean forceExponent) {
      if (value == 0) {
         return "0";
      }
      String str = Double.toString (roundDouble (value));
      if (containsExponent (str)) {
         // has exponent; we are done
         return str;
      }
      int dotIdx = str.indexOf ('.'); // index of dot, if any
      int zeroIdx = str.length(); // index of first trailing zero, if any
      for (int i=str.length()-1; i>=0; i--) {
         char c = str.charAt(i);
         if (c == '0') {
            zeroIdx = i;
         }
         else if (c != '.') {
            break;
         }
      }
      if (zeroIdx != str.length()) {
         // find num zeros to the left of the decimal point
         int numLeftZeros;
         if (dotIdx != -1 && dotIdx >= zeroIdx) {
            numLeftZeros = dotIdx-zeroIdx;
         }
         else {
            numLeftZeros = str.length()-zeroIdx;
         }
         if (numLeftZeros >= 5) {
            forceExponent = true;
         }
      }
      if (forceExponent) {
         FieldPosition fpos =
            new FieldPosition (DecimalFormat.FRACTION_FIELD);

         str = myDecFmt.format (value, new StringBuffer(), fpos).toString();
         return str;
      }
      else if (dotIdx != -1) {
         // strip off trailing zeros right of the decimal point:
         if (zeroIdx > dotIdx+1) {
            if (zeroIdx !=  str.length()) {
               return str.substring (0, zeroIdx);
            }
         }
         else {
            return str.substring (0, dotIdx);
         }
      }
      return str;
   }
}
