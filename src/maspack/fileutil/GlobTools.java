/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

public class GlobTools {

   /**
    * Converts a wildcard glob to regular expression
    * 
    * @param str wildcard glob string
    * @return corresponding regular expression
    */
   public static String globToRegex(String str) {

      str = str.trim();
      int strLen = str.length();

      StringBuilder sb = new StringBuilder(strLen);

      sb.append("^"); // start character
      boolean escaping = false;
      int curls = 0;

      for (char currentChar : str.toCharArray()) {
         switch (currentChar) {
            case '*':
               if (escaping) {
                  sb.append("\\*");
               } else {
                  sb.append(".*");
               }
               escaping = false;
               break;
            case '?':
               if (escaping) {
                  sb.append("\\?");
               } else {
                  sb.append('.');
               }
               escaping = false;
               break;
            case '.':
            case '(':
            case ')':
            case '+':
            case '|':
            case '^':
            case '$':
            case '@':
            case '%':
               sb.append('\\');
               sb.append(currentChar);
               escaping = false;
               break;
            case '\\':
               if (escaping) {
                  sb.append("\\\\");
                  escaping = false;
               } else {
                  escaping = true;
               }
               break;
            case '{':
               if (escaping) {
                  sb.append("\\{");
               } else {
                  sb.append('(');
                  curls++;
               }
               escaping = false;
               break;
            case '}':
               if (curls > 0 && !escaping) {
                  sb.append(')');
                  curls--;
               } else if (escaping) {
                  sb.append("\\}");
               } else {
                  sb.append("}");
               }
               escaping = false;
               break;
            case ',':
               if (curls > 0 && !escaping) {
                  sb.append('|');
               } else if (escaping) {
                  sb.append("\\,");
               } else {
                  sb.append(",");
               }
               break;
            default:
               escaping = false;
               sb.append(currentChar);
         }
      }

      // terminate
      sb.append("$");

      return sb.toString();
   }
   
}
