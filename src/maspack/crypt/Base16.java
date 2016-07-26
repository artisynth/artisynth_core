/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.crypt;

/**
 * Hex-encoded string
 * @author antonio
 *
 */
public class Base16 {
   
   private static String byteToHexChar(byte val) {

      // separate two halves
      int vala = (val>>4)&(0x0F);
      int valb = (val & 0x0F);

      return String.format("%c%c", nibbleToHexChar(vala),nibbleToHexChar(valb));

   }

   private static char nibbleToHexChar(int nib) {
      // A to F
      if (nib >= 10) {
         return (char)(nib -10 + 'A');
      }
      return (char)(nib+'0');
   }

   public static String encode(byte[] byteArray) {

      StringBuilder out = new StringBuilder();
      for (int i = 0; i < byteArray.length; ++i) {
         out.append(byteToHexChar(byteArray[i]));
      }
      return out.toString();
   }

   private static int hexCharToNibble(char a) {

      if (a >= 'a' && a <= 'f') {
         return a - 'a' + 10;
      } else if (a >= 'A' && a <= 'F') {
         return a - 'A' + 10;
      } else if (a >= '0' && a<='9') {
         return a-'0';
      } else {
         return -1;
      }
   }

   /**
    * Converts a Hex String to a byte array
    * @param str hex string to convert
    * @return byte array
    */
   public static byte[] decode(String str) {

      if (str.startsWith("0x") || str.startsWith("0X")) {
         str = str.substring(2);
      }

      int len = str.length()/2;
      byte[] out = new byte[len];
      for (int i = 0; i < str.length()/2; i++) {

         int a = hexCharToNibble(str.charAt(i*2));
         int b = hexCharToNibble(str.charAt(i*2+1));

         if (a < 0 || b < 0) {
            throw new IllegalArgumentException("Invalid hex character in '" + str + "'");
         }
         out[i] = (byte)((a << 4) | b);
      }
      return out;
   }
   
   private static boolean isHexChar(char a) {
      if ( (a >= 'a' && a <= 'f') 
         || (a >= 'A' && a <= 'F') 
         || (a >= '0' && a<='9')) {
         return true;
      }
      return false;
   }
   
   /**
    * True if array looks to be hex coded
    * @param byteArray array to test
    * @return true if appears hex code
    */
   public static boolean isCoded(byte[] byteArray) {
      for (int i=0; i<byteArray.length; i++) {
         if (!isHexChar((char)(byteArray[i]))) {
            return false;
         }
      }
      return true;
   }
   
   /**
    * True if string looks to be hex coded
    * @param str string to test
    * @return true if string looks to be hex coded
    */
   public static boolean isCoded(String str) {
      for (int i=0; i<str.length(); i++) {
         if (!isHexChar(str.charAt(i))) {
            return false;
         }
      }
      return true;
   }   
}
