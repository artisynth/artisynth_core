/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.PrintWriter;

//import java.util.*;

/**
 * A set of static methods to help write values to a PrintWriter.
 */
public class Write {

   protected static NumberFormat cfmt = new NumberFormat ("%5.3f");

   public static void writeColor (PrintWriter pw, Color color) {
      writeColor (pw, color, /* newline= */true);
   }

   /**
    * Writes A Color to a PrintWriter. The output consists of three floating
    * point numbers, representing the red, green, and blue color values in the
    * range 0 to 1, plus the alpha value if it is not 1, all surrounded by
    * brackets <code>[ ]</code>.
    * 
    * @param pw
    * PrintWriter to which the output is written
    * @param color
    * color to write
    * @param newline
    * if true, place a newline at the end of the output
    */
   public static void writeColor (PrintWriter pw, Color color, boolean newline) {
      float r = color.getRed() / 255f;
      float g = color.getGreen() / 255f;
      float b = color.getBlue() / 255f;
      float a = color.getAlpha() / 255f;

      NumberFormat fmt = new NumberFormat (cfmt);

      if (a == 1f) {
         pw.print ("[ " + fmt.format (r) + " " + fmt.format (g) + " "
         + fmt.format (b) + " ]");
      }
      else {
         pw.print ("[ " + fmt.format (r) + " " + fmt.format (g) + " "
         + fmt.format (b) + " " + fmt.format (a) + " ]");
      }
      if (newline) {
         pw.println ("");
      }
   }

   // /**
   //  * Writes an AxisAngle to a PrintWriter. The output consists of four floating
   //  * point numbers, representing the x, y, and z values of the axis, and the
   //  * angle in degrees.
   //  * 
   //  * @param pw
   //  * PrintWriter to which the output is written
   //  * @param axisAng
   //  * AxisAngle to write
   //  * @param fmt
   //  * numeric format for the vector elements
   //  */
   // public static void writeAxisAngle (
   //    PrintWriter pw, AxisAngle axisAng, NumberFormat fmt) {
   //    pw.print ("[ ");
   //    pw.print (fmt.format (axisAng.axis.x));
   //    pw.print (' ');
   //    pw.print (fmt.format (axisAng.axis.y));
   //    pw.print (' ');
   //    pw.print (fmt.format (axisAng.axis.z));
   //    pw.print (' ');
   //    pw.print (fmt.format (Math.toDegrees (axisAng.angle)));
   //    pw.println (" ]");
   // }

   /**
    * Writes a set of double values, enclosed in square brackets <code>[
    * ]</code>,
    * to a PrintWriter.
    * 
    * @param pw
    * PrintWriter to which values are written
    * @param vals
    * values to be written
    * @param n
    * number of values to write
    * @param fmt
    * (optional) numeric formatter for each value
    */
   public static void writeDoubles (
      PrintWriter pw, double[] vals, int n, NumberFormat fmt)
      throws IOException {
      pw.print ("[");
      if (fmt == null) {
         for (int i = 0; i < n; i++) {
            pw.print (" " + vals[i]);
         }
      }
      else {
         for (int i = 0; i < n; i++) {
            pw.print (" " + fmt.format (vals[i]));
         }
      }
      pw.println (" ]");
   }

   /**
    * Writes a set of double values, enclosed in square brackets <code>[
    * ]</code>,
    * to a PrintWriter.
    * 
    * @param pw
    * PrintWriter to which values are written
    * @param vals
    * values to be written
    * @param fmt
    * (optional) numeric formatter for each value
    */
   public static void writeDoubles (
      PrintWriter pw, double[] vals, NumberFormat fmt) throws IOException {
      writeDoubles (pw, vals, vals.length, fmt);
   }

   /**
    * Writes a set of float values, enclosed in square brackets <code>[
    * ]</code>,
    * to a PrintWriter.
    * 
    * @param pw
    * PrintWriter to which values are written
    * @param vals
    * values to be written
    * @param n
    * number of values to write
    * @param fmt
    * (optional) numeric formatter for each value
    */
   public static void writeFloats (
      PrintWriter pw, float[] vals, int n, NumberFormat fmt) throws IOException {
      pw.print ("[");
      if (fmt == null) {
         for (int i = 0; i < n; i++) {
            pw.print (" " + vals[i]);
         }
      }
      else {
         for (int i = 0; i < n; i++) {
            pw.print (" " + fmt.format (vals[i]));
         }
      }
      pw.println (" ]");
   }

   /**
    * Writes a set of float values, enclosed in square brackets <code>[
    * ]</code>,
    * to a PrintWriter.
    * 
    * @param pw
    * PrintWriter to which values are written
    * @param vals
    * values to be written
    * @param fmt
    * (optional) numeric formatter for each value
    */
   public static void writeFloats (
      PrintWriter pw, float[] vals, NumberFormat fmt) throws IOException {
      writeFloats (pw, vals, vals.length, fmt);
   }

   /**
    * Writes a set of integer values, enclosed in square brackets <code>[
    * ]</code>,
    * to a PrintWriter.
    * 
    * @param pw
    * PrintWriter to which values are written
    * @param vals
    * values to be written
    * @param n
    * number of values to write
    * @param fmt
    * (optional) numeric formatter for each value
    */
   public static void writeInts (
      PrintWriter pw, int[] vals, int n, NumberFormat fmt) throws IOException {
      pw.print ("[");
      if (fmt == null) {
         for (int i = 0; i < n; i++) {
            pw.print (" " + vals[i]);
         }
      }
      else {
         for (int i = 0; i < n; i++) {
            pw.print (" " + fmt.format (vals[i]));
         }
      }
      pw.println (" ]");
   }

   /**
    * Writes a set of integer values, enclosed in square brackets <code>[
    * ]</code>,
    * to a PrintWriter.
    * 
    * @param pw
    * PrintWriter to which values are written
    * @param vals
    * values to be written
    * @param fmt
    * (optional) numeric formatter for each value
    */
   public static void writeInts (PrintWriter pw, int[] vals, NumberFormat fmt)
      throws IOException {
      writeInts (pw, vals, vals.length, fmt);
   }

   /**
    * Writes a set of short integer values, enclosed in square brackets
    * <code>[ ]</code>, to a PrintWriter.
    * 
    * @param pw
    * PrintWriter to which values are written
    * @param vals
    * values to be written
    * @param n
    * number of values to write
    * @param fmt
    * (optional) numeric formatter for each value
    */
   public static void writeShorts (
      PrintWriter pw, short[] vals, int n, NumberFormat fmt) throws IOException {
      pw.print ("[");
      if (fmt == null) {
         for (int i = 0; i < n; i++) {
            pw.print (" " + vals[i]);
         }
      }
      else {
         for (int i = 0; i < n; i++) {
            pw.print (" " + fmt.format (vals[i]));
         }
      }
      pw.println (" ]");
   }

   /**
    * Writes a set of short integer values, enclosed in square brackets
    * <code>[ ]</code>, to a PrintWriter.
    * 
    * @param pw
    * PrintWriter to which values are written
    * @param vals
    * values to be written
    * @param fmt
    * (optional) numeric formatter for each value
    */
   public static void writeShorts (
      PrintWriter pw, short[] vals, NumberFormat fmt) throws IOException {
      writeShorts (pw, vals, vals.length, fmt);
   }

   /**
    * Writes a set of long integer values, enclosed in square brackets
    * <code>[ ]</code>, to a PrintWriter.
    * 
    * @param pw
    * PrintWriter to which values are written
    * @param vals
    * values to be written
    * @param n
    * number of values to write
    * @param fmt
    * (optional) numeric formatter for each value
    */
   public static void writeLongs (
      PrintWriter pw, long[] vals, int n, NumberFormat fmt) throws IOException {
      pw.print ("[");
      if (fmt == null) {
         for (int i = 0; i < n; i++) {
            pw.print (" " + vals[i]);
         }
      }
      else {
         for (int i = 0; i < n; i++) {
            pw.print (" " + fmt.format (vals[i]));
         }
      }
      pw.println (" ]");
   }

   /**
    * Writes a set of long integer values, enclosed in square brackets
    * <code>[ ]</code>, to a PrintWriter.
    * 
    * @param pw
    * PrintWriter to which values are written
    * @param vals
    * values to be written
    * @param fmt
    * (optional) numeric formatter for each value
    */
   public static void writeLongs (PrintWriter pw, long[] vals, NumberFormat fmt)
      throws IOException {
      writeLongs (pw, vals, vals.length, fmt);
   }

   /**
    * Writes out a String enclosed in double quotes <code>"</code>, formatted
    * using the routine {@link #getQuotedString getQuotedString}.
    * 
    * @param pw
    * PrintWriter to which string is written
    * @param s
    * String to be written
    * @see #getQuotedString
    */
   public static void writeString (PrintWriter pw, String s) {
      pw.println (getQuotedString (s));
   }

   /**
    * Converts a String to a double-quoted format. The String itself is enclosed
    * in double quotes <code>"</code>, and double quotes themselves are
    * escaped using backslash <code>\</code>. The characters <code>\b</code>,
    * <code>\t</code>, <code>\n</code>, <code>\f</code>,
    * <code>\r</code>, and <code>\</code> (corresponding to backspace, tab,
    * linefeed, formfeed, carriage return, and backslash) are written using
    * their canonical escape sequences. Any other character {@code <=}
    * 0x1f or {@code >=} 0x7f
    * is written by a backslashed octal number.
    * 
    * @param s
    * String to be converted to quoted format
    */
   public static String getQuotedString (String s) {
      StringBuffer sbuf = new StringBuffer (2 * s.length());

      sbuf.append ('"');
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         switch (c) {
            case '\b': {
               sbuf.append ("\\b");
               break;
            }
            case '\t': {
               sbuf.append ("\\t");
               break;
            }
            case '\n': {
               sbuf.append ("\\n");
               break;
            }
            case '\f': {
               sbuf.append ("\\f");
               break;
            }
            case '\r': {
               sbuf.append ("\\r");
               break;
            }
            case '\\': {
               sbuf.append ("\\\\");
               break;
            }
            case '"': {
               sbuf.append ("\\\"");
               break;
            }
            default: {
               if (c <= 0x1f || c >= 0x7f) {
                  sbuf.append ("\\" + Integer.toOctalString (c));
               }
               else {
                  sbuf.append (c);
               }
            }
         }
      }
      sbuf.append ('"');
      return sbuf.toString();
   }
   
   public static void writeFont(PrintWriter pw, Font font) {
      pw.print ("[ ");
      pw.print ("fontName=");
      pw.print (getQuotedString (font.getFontName ()));
      pw.print (", size=");
      pw.print (font.getSize2D ());
      pw.print (", style=");
      pw.print (font.getStyle ());
      pw.println (" ]");
   }
   
}
