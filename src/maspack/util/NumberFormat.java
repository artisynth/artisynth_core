/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.text.FieldPosition;
import java.text.DecimalFormat;

/**
 * Object for formatting numbers in the same way similiar to the C
 * <code>printf</code> function.
 * 
 * <p>
 * A <code>printf</code> style format string is specified in the constructor,
 * or can be respecified using the <code>set</code> method. Once instantiated,
 * the <code>format</code> methods of this class may be used to convert
 * numeric types (float, double, int, long) into Strings.
 * 
 * <p>
 * Examples:
 * 
 * <pre>
 * double theta1 = 45.0;
 * double theta2 = 85.0;
 * NumberFormat fmt = new NumberFormat (&quot;%7.2f\n&quot;);
 * 
 * System.out.println (&quot;theta1=&quot; + fmt.format (theta1) + &quot;theta2=&quot;
 * + fmt.format (theta2));
 * </pre>
 * 
 * @author John E. Lloyd, Winter 2004
 */

public class NumberFormat {
   private String prefix = "";
   private String suffix = "";
   private int width = -1;
   private int precision = -1;
   private int type = 'g';

   private static String validTypes = new String ("diouxXeEfgaA");

   private static NumberFormat hex = new NumberFormat ("0x%x");

   private int idx;
   private boolean addSign = false;
   private boolean addBlank = false;
   private boolean leftAdjust = false;
   private boolean alternate = false;
   private boolean zeropad = false;

   private StringBuffer fbuf = new StringBuffer (80);
   private StringBuffer xbuf = new StringBuffer (80);
   private FieldPosition fpos =
      new FieldPosition (DecimalFormat.FRACTION_FIELD);

   private static char[] ddigits =
      { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

   private static char[] xdigits =
      { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
       'e', 'f' };

   private static char[] Xdigits =
      { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
       'E', 'F' };

   private java.text.DecimalFormat decFmt;
   private java.text.DecimalFormat expFmt;

   private String indentString = "";
   private int indentColumn = 0;

   /**
    * Creates a new instance of PrintfFormat with the default format string
    * <code>%g</code>.
    */
   public NumberFormat() {
      set ("%g");
   }

   /**
    * Creates a new NumberFormat, with settings given by the supplied format
    * string. The structure of the format string is described in the
    * documentation for {@link #set set}.
    * 
    * @param fmtStr
    * Format string
    * @throws IllegalArgumentException
    * Malformed format string
    * @see #set(String)
    */
   public NumberFormat (String fmtStr) {
      set (fmtStr);
   }

   /**
    * Creates a new NumberFormat, whose setting are copied from an existing
    * NumberFormat.
    * 
    * @param fmt
    * formatter to be copies
    * @see #set(NumberFormat)
    */
   public NumberFormat (NumberFormat fmt) {
      set (fmt);
   }

   /**
    * Sets the field width associated with this format. The numeric portion of
    * the final formatted string is padded to ensure that it is at least this
    * width.
    * 
    * @param w
    * field width
    * @throws IllegalArgumentException
    * Negative field width
    * @see #getFieldWidth
    * @see #set
    */
   public void setFieldWidth (int w) {
      if (w < 0) {
         throw new IllegalArgumentException ("Negative field width");
      }
      width = w;
   }

   /**
    * Returns the field width associated with this format.
    * 
    * @return field width
    * @see #setFieldWidth
    * @see #set
    */
   public int getFieldWidth() {
      return width;
   }

   /**
    * Enables 'alternate' formatting. This implies the following: For 'o'
    * conversions, the output is always prefixed with a '0'. For 'x' and 'X'
    * conversions, the output is prefixed with "0x" or "0X", respectively. For
    * 'a', 'A', 'e', 'E', and 'f' conversions, the result will always contain a
    * decimal point. There is no effect for other conversions.
    * 
    * @param enable
    * if true, enables 'alternate' formatting
    * @see #isAlternate
    */
   public void setAlternate (boolean enable) {
      alternate = enable;
   }

   /**
    * Returns true if 'alternate' formatting is enabled. For details on what
    * this means, see {@link #setAlternate setAlternate}.
    * 
    * @return true if 'alternate' formatting is enabled.
    * @see #setAlternate
    */
   boolean isAlternate() {
      return alternate;
   }

   /**
    * Sets the precision associated with this format. For a floating point type,
    * the precision generally gives the number of digits that appear after the
    * decimal point. For more precise details, see the documentation for
    * {@link #set set}. A negative value implies either that no precision is
    * set, or a default value should be set.
    * 
    * @param p
    * precision
    * @see #getPrecision
    * @see #set
    */
   public void setPrecision (int p) {
      precision = p;
      if (type == 'f') {
         if (precision < 0) {
            precision = 6;
         }
         // decFmt.setMinimumFractionDigits (precision);
         // decFmt.setMaximumFractionDigits (precision);
      }
      else if (type == 'e' || type == 'E') {
         if (precision < 0) {
            precision = 6;
         }
         createExpFormat (precision);
      }
      else if (type == 'g' || type == 'G') {
         if (precision < 0) {
            precision = -1;
         }
         else if (precision == 0) {
            precision = 1;
         }
         if (precision > 0) {
            createExpFormat (precision - 1);
         }
      }
   }

   /**
    * Returns the precision associated with this format.
    * 
    * @return precision
    * @see #setPrecision
    * @see #set
    */
   public int getPrecision() {
      return precision;
   }

   /**
    * Sets the prefix associated with this format. The prefix is the (possibly
    * empty) string that precedes the numeric part.
    * 
    * @param pre
    * new prefix
    * @see #getPrefix
    */
   public void setPrefix (String pre) {
      prefix = (pre != null ? pre : "");
   }

   /**
    * Gets the prefix associated with this format.
    * 
    * @return prefix
    */
   public String getPrefix() {
      return prefix;
   }

   /**
    * Sets the suffix associated with this format. The suffix is the (possibly
    * empty) string that follows the numeric part.
    * 
    * @param suf
    * new suffix
    * @see #getSuffix
    */
   public void setSuffix (String suf) {
      suffix = (suf != null ? suf : "");
   }

   /**
    * Gets the suffix associated with this format.
    * 
    * @return suffix
    */
   public String getSuffix() {
      return suffix;
   }

   /**
    * Returns the conversion character for this format.
    * 
    * @return conversion character
    */
   public int getConversionChar() {
      return type;
   }

   private void scanRegularChars (StringBuffer buf, String fmt) {
      buf.setLength (0);
      char c;

      for (; idx < fmt.length(); idx++) {
         if ((c = fmt.charAt (idx)) == '%') {
            idx++;
            if (idx == fmt.length()) {
               throw new IllegalArgumentException (
                  "Format string terminates with '%'");
            }
            if ((c = fmt.charAt (idx)) != '%') {
               break;
            }
         }
         buf.append ((char)c);
      }
   }

   private int scanUnsignedInt (String fmt) {
      int value = 0;
      char c;

      for (c = fmt.charAt (idx); Character.isDigit (c); c = fmt.charAt (idx)) {
         value = 10 * value + c - '0';
         if (++idx == fmt.length()) {
            break;
         }
      }
      return value;
   }

   private void createDecFormat() {
      decFmt = new java.text.DecimalFormat();
      decFmt.setGroupingUsed (false);
   }

   private void createExpFormat (int numFracDigits) {
      xbuf.setLength (0);
      if (numFracDigits == 0) {
         xbuf.append ("0E00");
      }
      else {
         xbuf.append ("0.");
         for (int i = 0; i < numFracDigits; i++) {
            xbuf.append ('0');
         }
         xbuf.append ("E00");
      }
      expFmt = new java.text.DecimalFormat();
      expFmt.setGroupingUsed (false);
      expFmt.applyPattern (xbuf.toString());
   }

   /**
    * Sets the format characteristics according to the supplied String.
    * 
    * <p>
    * The format string has a format similar to the one used by the C
    * <code>printf</code> function, except that only one conversion may be
    * specified.
    * 
    * <p>
    * The format string consists of an optional <em>prefix</em> of regular
    * characters, followed by a conversion sequence, followed by an optional
    * <em>suffix</em> of regular characters.
    * 
    * <p>
    * The conversion sequence is introduced by a '%' character, and is followed
    * by any number of optional <em>flag</em> characters, an optional unsigned
    * decimal integer specifying a <em>field
    * width</em>, another optional
    * unsigned decimal integer (preceded by a '.' character) specifying a
    * <em>precision</em>, and finally a <code>conversion character</code>.
    * To incorporate a '%' character into either the prefix or suffix, one
    * should specify the sequence "%%".
    * 
    * The allowed flag characters are:
    * 
    * <dl>
    * <dt> #
    * <dd> The value is converted into an "alternate" form. For 'o' conversions,
    * the output is always prefixed with a '0'. For 'x' and 'X' conversions, the
    * output is prefixed with "0x" or "0X", respectively. For 'a', 'A', 'e',
    * 'E', and 'f' conversions, the result will always contain a decimal point.
    * There is no effect for other conversions.
    * <dt> 0
    * <dd> Use '0' to pad the field on the left, instead of blanks. If the
    * conversion is 'd', 'i', 'o', 'u', 'x', or 'X', and a precision is given,
    * then this flag is ignored.
    * <dt> -
    * <dd> The output is aligned with the left of the field boundary, and padded
    * on the right with blanks. This flag overrides the '0' flag.
    * <dt> ' '
    * <dd> Leave a blank before a positive number produced by a signed
    * conversion.
    * <dt> +
    * <dd> A '+' sign is placed before non-negative numbers produced by a signed
    * conversion. This flag overrides the ' ' flag.
    * </dl>
    * <p>
    * The conversion character is one of:
    *
    * <dl>
    * <dt> d,i
    * <dd> The integer argument is output as a signed decimal number. If a
    * precision is given, it describes the minimum number of digits that must
    * appear (default 1). If the precision exceeds the number of digits that
    * would normally appear, the output is padded on the left with zeros. When 0
    * is printed with precision 0, the result is empty.
    * <dt> o,u,x,X
    * <dd> The integer argument is output as an unsigned number in either octal
    * ('o'), decimal ('u'), or hexadecimal ('x' or 'X'). The digits "abcdef" are
    * used for 'x', and "ABCDEF" are used for 'X'. If a precision is given, it
    * describes the minimum number of digits that must appear (default 1). If
    * the precision exceeds the number of digits that would normally appear, the
    * output is padded on the left with zeros. When 0 is printed with precision
    * 0, the result is empty.
    * <dt> e,E
    * <dd> The floating point argument is output in the exponential form
    * <code>[-]d.ddde+dd</code>, with the number of digits after the decimal
    * point given by the precision. The default precision value is 6. No decimal
    * point is output if the precision is 0. Conversion 'E' causes 'E' to be
    * used as an exponent instead of 'e'. The exponent is always at least two
    * characters.
    * <dt> f
    * <dd> The floating point argument is output in the form
    * <code>[-]ddd.ddd</code>, with the number of digits after the decimal
    * point given by the precision. The default precision value is 6. No decimal
    * point is output if the precision is 0. If a decimal point appears, at
    * least one digit appears before it.
    * <dt> g
    * <dd> If the precision is undefined, the floating point argument is output
    * using the Java <code>Double.toString</code> method. This produces a
    * decimal representation that is accurate enough to reproduce the number
    * exactly if it is read back into a Java application using
    * <code>Double.parseDouble</code>. Note that this is a different behavior
    * from the usual C <code>printf</code> interpretation of <code>%g</code>.
    * <dd> If a precision <i>is</i> defined, then the number is output in
    * decimal format if its value lies between 1e-4 and 10^precision, with the
    * number of significant digits equaling the precision. Otherwise, the number
    * is output in exponential format, again with the number of significant
    * digits equaling the precision. A precision specified as 0 is incremented
    * to 1.
    * <dt> a,A
    * <dd> The floating point argument is output in the hexadecimal floating
    * point form <code>[-]0xh.hhhp+dd</code>. The exponent is a decimal
    * number and describes a power of 2. The 'A' style uses the prefix "0X", the
    * hex digits "ABCDEF", and the exponent character 'P'. The number of digits
    * after the decimal point is given by the precision. The default precision
    * is enough for an exact representation of the value.
    * </dl>
    * 
    * @param fmtStr
    * Format string
    * @throws IllegalArgumentException
    * Malformed format string
    */
   public void set (String fmtStr) throws IllegalArgumentException {

      xbuf.ensureCapacity (fmtStr.length());

      prefix = "";
      suffix = "";
      width = -1;
      precision = -1;
      idx = 0;

      addSign = false;
      addBlank = false;
      leftAdjust = false;
      alternate = false;
      zeropad = false;

      scanRegularChars (xbuf, fmtStr);
      if (xbuf.length() > 0) {
         prefix = xbuf.toString();
      }
      if (idx == fmtStr.length()) {
         throw new IllegalArgumentException ("missing '%' character");
      }

      // parse the flags
      boolean parsingFlags = true;
      do {
         switch (fmtStr.charAt (idx)) {
            case '+': {
               addSign = true;
               break;
            }
            case ' ': {
               addBlank = true;
               break;
            }
            case '-': {
               leftAdjust = true;
               break;
            }
            case '#': {
               alternate = true;
               break;
            }
            case '0': {
               zeropad = true;
               break;
            }
            default: {
               parsingFlags = false;
               break;
            }
         }
         if (parsingFlags) {
            if (++idx == fmtStr.length()) {
               parsingFlags = false;
            }
         }
      }
      while (parsingFlags);

      if (idx < fmtStr.length() && Character.isDigit (fmtStr.charAt (idx))) {
         width = scanUnsignedInt (fmtStr);
      }
      if (idx < fmtStr.length() && fmtStr.charAt (idx) == '.') {
         if (++idx < fmtStr.length() &&
             Character.isDigit (fmtStr.charAt (idx))) {
            precision = scanUnsignedInt (fmtStr);
         }
         else {
            throw new IllegalArgumentException (
               "'.' in conversion spec not followed by precision value");
         }
      }
      if (idx == fmtStr.length()) {
         throw new IllegalArgumentException ("Format string ends prematurely");
      }
      type = fmtStr.charAt (idx++);
      switch (type) {
         case 'd':
         case 'i':
         case 'o':
         case 'u':
         case 'x':
         case 'X': {
            if (precision != -1 && zeropad) {
               zeropad = false;
            }
            break;
         }
         case 'G':
         case 'g': {
            if (precision != -1) {
               if (precision == 0) {
                  precision = 1;
               }
               // createDecFormat();
               createExpFormat (precision - 1);
            }
            break;
         }

         case 'f': {
            if (precision == -1) {
               precision = 6;
            }
            break;
         }
         case 'e':
         case 'E': {
            if (precision == -1) {
               precision = 6;
            }
            createExpFormat (precision);
            break;
         }
         case 'a':
         case 'A': {
            break;
         }
            // case 'c':
            // { break;
            // }
            // case 's':
            // { break;
            // }
         default: {
            if (validTypes.indexOf (type) == -1) {
               throw new IllegalArgumentException ("Conversion character '"
               + (char)type + "' not one of '" + validTypes + "'");
            }
            break;
         }
      }
      scanRegularChars (xbuf, fmtStr);
      if (xbuf.length() > 0) {
         suffix = xbuf.toString();
      }
      if (idx != fmtStr.length()) {
         throw new IllegalArgumentException (
            "Format string has more than one conversion spec");
      }
      if (leftAdjust && zeropad) {
         zeropad = false;
      }
      if (addSign && addBlank) {
         addBlank = false;
      }
   }

   // private void pad (StringBuffer buf, String s, char c, int w)
   // {
   // int len = s.length();
   // if (w != -1)
   // { while (len < w)
   // { buf.append (c);
   // len++;
   // }
   // }
   // buf.append (s);
   // }

   private int indexOf (char c, int base, StringBuffer sbuf) {
      for (int i = base; i < sbuf.length(); i++) {
         if (sbuf.charAt(i) == c) {
            return i;
         }
      }
      return -1;
   }

   /**
    * Formats a double into a string.
    * 
    * @param x
    * value to be formatted
    * @return formatted string
    */
   public String format (double x) {
      fbuf.setLength (0);
      format (x, fbuf);
      return fbuf.toString();
   }

   /**
    * Formats a double and appends it to the end of a <code>StringBuffer</code>.
    *
    * @param x value to be formatted
    * @param sbuf buffer to place the result in
    * @return pointer to the result buffer
    */
   public StringBuffer format (double x, StringBuffer sbuf) {
      int base = sbuf.length();
      char p = '\0';

      if (Double.isNaN (x)) {
         sbuf.append ("NaN");
      }
      else if (x == Double.POSITIVE_INFINITY) {
         sbuf.append ("Inf");
      }
      else if (x == Double.NEGATIVE_INFINITY) {
         sbuf.append ("-Inf");
      }
      else if (type == 'f') {
         if (decFmt == null) {
            createDecFormat();
         }
         decFmt.setMinimumFractionDigits (precision);
         decFmt.setMaximumFractionDigits (precision);
         decFmt.format (x, sbuf, fpos);
         if (alternate && indexOf ('.', base, sbuf) == -1) {
            sbuf.append ('.');
         }
      }
      else if (type == 'e' || type == 'E') {
         expFmt.format (x, sbuf, fpos);
         int i = indexOf ('E', base, sbuf);
         if (i == -1) {
            i = indexOf ('e', base, sbuf);
            if (i == -1) {
               throw new InternalErrorException (
                  "E/e not found in exponential format: x=" + x +
                  " sbuf=" + sbuf);
            }
         }
         else if (type == 'e') {
            sbuf.setCharAt (i, 'e');
         }
         char c = sbuf.charAt(i+1);
         if (c != '-' && c != '+') {
            sbuf.insert (i+1, '+');
         }
      }
      else if (type == 'a' || type == 'A') {
         expHexFormat (sbuf, x, precision);
      }
      else if (type == 'g' || type == 'G') {
         if (precision == -1) {
            sbuf.append (Double.toString (x));
         }
         else if (x == 0) {
            sbuf.append ("0");
         }
         else {
            int numFracDigits = -1;
            double absx = Math.abs (x);
            if (absx >= 1e-4) {
               numFracDigits = 3 + precision;
               double limit = 1e-3;
               while (numFracDigits >= 0) {
                  if (absx < limit) {
                     break;
                  }
                  numFracDigits--;
                  limit *= 10;
               }
            }
            if (numFracDigits == -1) {
               expFmt.format (x, sbuf, fpos);
               int i = indexOf ('E', base, sbuf);
               if (i == -1) {
                  i = indexOf ('e', base, sbuf);
                  if (i == -1) {
                     throw new InternalErrorException (
                        "E/e not found in exponential format: x=" + x +
                        " sbuf=" + sbuf);
                  }
               }
               else if (type == 'g') {
                  sbuf.setCharAt (i, 'e');
               }
               char c = sbuf.charAt(i+1);
               if (c != '-' && c != '+') {
                  sbuf.insert (i+1, '+');
               }
            }
            else {
               if (decFmt == null) {
                  createDecFormat();
               }
               decFmt.setMinimumFractionDigits (numFracDigits);
               decFmt.setMaximumFractionDigits (numFracDigits);
               decFmt.format (x, sbuf, fpos);
               if (width == -1) {
                  trimTrailingZeros (sbuf);
               }
               if (alternate && indexOf ('.', base, sbuf) == -1) {
                  sbuf.append ('.');
               }
            }
         }
      }
      else {
         return format ((long)x, sbuf);
      }
      if (x > 0) {
         if (addSign) {
            p = '+';
         }
         else if (addBlank) {
            p = ' ';
         }
      }
      if (zeropad) {
         int nz = width - sbuf.length() + base;
         if (p != '\0') {
            nz--;
         }
         for (int i = 0; i < nz; i++) {
            sbuf.insert (base, '0');
         }
      }
      if (p != '\0') {
         sbuf.insert (base, p);
      }
      pad (sbuf, sbuf.substring (base));
      return sbuf;
   }

   /**
    * Formats a float into a string.
    * 
    * @param x
    * value to be formatted
    * @return formatted string
    */
   public String format (float x) {
      fbuf.setLength (0);
      format (x, fbuf);
      return fbuf.toString();
   }

   /**
    * Formats a float and appends it to the end of a <code>StringBuffer</code>.
    *
    * @param x value to be formatted
    * @param sbuf buffer to place the result in
    * @return pointer to the result buffer
    */
   public StringBuffer format (float x, StringBuffer sbuf) {

      // XXX This method shares almost identical code with
      // format(double,StringBuffer), with the only difference being 'float'
      // vs. 'double'. There should be a less repetitive way to write this.
      int base = sbuf.length();
      char p = '\0';

      if (Float.isNaN (x)) {
         sbuf.append ("NaN");
      }
      else if (x == Float.POSITIVE_INFINITY) {
         sbuf.append ("Inf");
      }
      else if (x == Float.NEGATIVE_INFINITY) {
         sbuf.append ("-Inf");
      }
      else if (type == 'f') {
         if (decFmt == null) {
            createDecFormat();
         }
         decFmt.setMinimumFractionDigits (precision);
         decFmt.setMaximumFractionDigits (precision);
         decFmt.format (x, sbuf, fpos);
         if (alternate && indexOf ('.', base, sbuf) == -1) {
            sbuf.append ('.');
         }
      }
      else if (type == 'e' || type == 'E') {
         expFmt.format (x, sbuf, fpos);
         int i = indexOf ('E', base, sbuf);
         if (i == -1) {
            i = indexOf ('e', base, sbuf);
            if (i == -1) {
               throw new InternalErrorException (
                  "E/e not found in exponential format: x=" + x +
                  " sbuf=" + sbuf);
            }
         }
         else if (type == 'e') {
            sbuf.setCharAt (i, 'e');
         }
         char c = sbuf.charAt(i+1);
         if (c != '-' && c != '+') {
            sbuf.insert (i+1, '+');
         }
      }
      else if (type == 'a' || type == 'A') {
         expHexFormat (sbuf, x, precision);
      }
      else if (type == 'g' || type == 'G') {
         if (precision == -1) {
            sbuf.append (Float.toString (x));
         }
         else if (x == 0) {
            sbuf.append ("0");
         }
         else {
            int numFracDigits = -1;
            float absx = Math.abs (x);
            if (absx >= 1e-4) {
               numFracDigits = 3 + precision;
               float limit = 1e-3f;
               while (numFracDigits >= 0) {
                  if (absx < limit) {
                     break;
                  }
                  numFracDigits--;
                  limit *= 10;
               }
            }
            if (numFracDigits == -1) {
               expFmt.format (x, sbuf, fpos);
               int i = indexOf ('E', base, sbuf);
               if (i == -1) {
                  i = indexOf ('e', base, sbuf);
                  if (i == -1) {
                     throw new InternalErrorException (
                        "E/e not found in exponential format: x=" + x +
                        " sbuf=" + sbuf);
                  }
               }
               else if (type == 'g') {
                  sbuf.setCharAt (i, 'e');
               }
               char c = sbuf.charAt(i+1);
               if (c != '-' && c != '+') {
                  sbuf.insert (i+1, '+');
               }
            }
            else {
               if (decFmt == null) {
                  createDecFormat();
               }
               decFmt.setMinimumFractionDigits (numFracDigits);
               decFmt.setMaximumFractionDigits (numFracDigits);
               decFmt.format (x, sbuf, fpos);
               if (width == -1) {
                  trimTrailingZeros (sbuf);
               }
               if (alternate && indexOf ('.', base, sbuf) == -1) {
                  sbuf.append ('.');
               }
            }
         }
      }
      else {
         return format ((long)x, sbuf);
      }
      if (x > 0) {
         if (addSign) {
            p = '+';
         }
         else if (addBlank) {
            p = ' ';
         }
      }
      if (zeropad) {
         int nz = width - sbuf.length() + base;
         if (p != '\0') {
            nz--;
         }
         for (int i = 0; i < nz; i++) {
            sbuf.insert (base, '0');
         }
      }
      if (p != '\0') {
         sbuf.insert (base, p);
      }
      pad (sbuf, sbuf.substring (base));
      return sbuf;
   }

   private void trimTrailingZeros (StringBuffer sbuf) {
      if (sbuf.indexOf (".") != -1) {
         int cut = sbuf.length() - 1;
         while (sbuf.charAt (cut) == '0') {
            cut--;
         }
         if (sbuf.charAt (cut) == '.') {
            sbuf.setLength (cut);
         }
         else if (cut < sbuf.length() - 1) {
            sbuf.setLength (cut + 1);
         }
      }
   }

   private void expHexFormat (StringBuffer sbuf, double d, int p) {
      char[] digits = null;
      long bits;
      int e;
      long m;

      bits = Double.doubleToLongBits (d);
      e = (int)((bits >> 52) & 0x7ffL);
      m =
         (e == 0) ? (bits & 0xfffffffffffffL) << 1
            : (bits & 0xfffffffffffffL) | 0x10000000000000L;
      if (m == 0) {
         e = 0;
      }
      else {
         e -= 1023;
      }
      if (m != 0) {
         while ((m & 0x10000000000000L) == 0) {
            m = m << 1;
            e--;
         }
      }
      if (p > 0 && p < 13) { // then round up if necessary
         if ((0xf & (m >> 4 * (12 - p))) >= 8) {
            m += 1L << 4 * (13 - p);
            if ((m & 0x20000000000000L) != 0) {
               m = m >> 1;
               e++;
            }
         }
      }
      if ((bits & 0x8000000000000000L) != 0) {
         sbuf.append ('-');
      }
      sbuf.append ('0');
      if (type == 'A') {
         sbuf.append ('X');
         digits = Xdigits;
      }
      else {
         sbuf.append ('x');
         digits = xdigits;
      }
      sbuf.append (((m & 0x10000000000000L) != 0) ? '1' : '0');
      if (p > 0 || (p == -1 && m != 0) || alternate) {
         sbuf.append ('.');
      }
      while (p > 0 || (p == -1 && (m & 0xfffffffffffffL) != 0)) {
         sbuf.append (digits[(int)((m & 0xf000000000000L) >> 48)]);
         m = m << 4;
         if (p > 0) {
            p--;
         }
      }
      sbuf.append ((type == 'A') ? 'P' : 'p');
      sbuf.append (e >= 0 ? '+' : '-');
      String expStr = Long.toString (Math.abs (e));
      sbuf.append (expStr);
   }

   /**
    * Formats an int into a string.
    * 
    * @param x
    * value to be formatted
    * @return formatted string
    */
   public String format (int x) {
      long lx = x;
      if (type == 'd' || type == 'i') {
         return format (lx);
      }
      else { // unsigned; clear high bits
         return format (lx & 0xffffffffL);
      }
   }

   /**
    * Formats an int and appends it to the end of a <code>StringBuffer</code>.
    *
    * @param x value to be formatted
    * @param sbuf buffer to place the result in
    * @return pointer to the result buffer
    */
   public StringBuffer format (int x, StringBuffer sbuf) {
      long lx = x;
      if (type == 'd' || type == 'i') {
         return format (lx, sbuf);
      }
      else { // unsigned; clear high bits
         return format (lx & 0xffffffffL, sbuf);
      }
   }

   /**
    * Formats a long into a string.
    * 
    * @param x
    * value to be formatted
    * @return formatted string
    */
   public String format (long x) {
      fbuf.setLength (0);
      format (x, fbuf);
      return fbuf.toString();
   }

   /**
    * Formats a long and appends it to the end of a <code>StringBuffer</code>.
    *
    * @param x value to be formatted
    * @param sbuf buffer to place the result in
    * @return pointer to the result buffer
    */
   public StringBuffer format (long x, StringBuffer sbuf) {
      String p = null;

      int base = sbuf.length();

      if (type == 'd' || type == 'i') {
         if (x < 0) {
            x = -x;
            p = "-";
         }
         else {
            if (addSign) {
               p = "+";
            }
            else if (addBlank) {
               p = " ";
            }
         }
         if (precision != 0 || x != 0) {
            sbuf.append (Long.toString (x));
         }
      }
      else if (type == 'u') {
         uconv (sbuf, x, 10, ddigits);
      }
      else if (type == 'o') {
         uconv (sbuf, x, 8, ddigits);
         if (alternate && sbuf.charAt (0) != '0') {
            p = "0";
         }
      }
      else if (type == 'x') {
         uconv (sbuf, x, 16, xdigits);
         if (alternate) {
            p = "0x";
         }
      }
      else if (type == 'X') {
         uconv (sbuf, x, 16, Xdigits);
         if (alternate) {
            p = "0X";
         }
      }
      else {
         return format ((double)x, sbuf);
      }
      int nz = 0;
      if (zeropad) {
         nz = width - sbuf.length() + base;
      }
      else if (precision > 0) {
         nz = precision - sbuf.length() + base;
      }
      if (nz > 0) {
         if (p != null) {
            nz -= p.length();
         }
         for (int i = 0; i < nz; i++) {
            sbuf.insert (base, '0');
         }
      }
      if (p != null) {
         sbuf.insert (base, p);
      }
      pad (sbuf, sbuf.substring (base));
      return sbuf;
   }

   private void pad (StringBuffer sbuf, String str) {
      int padcnt = width - str.length();
      sbuf.setLength (sbuf.length() - str.length());
      sbuf.append (prefix);
      if (leftAdjust) {
         sbuf.append (str);
         for (int i = 0; i < padcnt; i++) {
            sbuf.append (' ');
         }
      }
      else {
         for (int i = 0; i < padcnt; i++) {
            sbuf.append (' ');
         }
         sbuf.append (str);
      }
      sbuf.append (suffix);
   }

   private void uconv (StringBuffer sbuf, long val, int radix, char[] digits) {
      int base = sbuf.length();

      if (val == 0) {
         if (precision != 0) {
            sbuf.append ('0');
         }
         return;
      }
      if (val < 0) { // have to compute the first val/radix and val%radix in a
         // complicated way
         long halfval;
         int mod;

         halfval = val >>> 1;
         mod = (int)(2 * (halfval % radix) + (val & 0x1));
         val = 2 * (halfval / radix);
         if (mod >= radix) {
            mod -= radix;
            val += 1;
         }
         sbuf.insert (base, digits[mod]);
      }
      while (val != 0) {
         sbuf.insert (base, digits[(int)(val % radix)]);
         val /= radix;
      }
   }

   public void setIndentColumn (int col) {
      indentColumn = col;
      indentString = "";
      for (int tabCnt = 0; tabCnt < col / 8; tabCnt++) {
         indentString += "\t";
      }
      for (int spaceCnt = 0; spaceCnt < (col % 8); spaceCnt++) {
         indentString += " ";
      }
   }

   public void incIndentColumn (int inc) {
      setIndentColumn (Math.max (0, indentColumn + inc));
   }

   public int getIndentColumn() {
      return indentColumn;
   }

   public String indent() {
      return indentString;
   }

   /**
    * Returns a string representation of this NumberFormat. If passed as an
    * argument to {@link #set set}, this string should specify a NumberFormat
    * identical to this one.
    * 
    * @return string representation of this format
    */
   public String toString() {
      StringBuffer sbuf = new StringBuffer (16);
      sbuf.append (prefix);
      sbuf.append ('%');
      if (alternate) {
         sbuf.append ('#');
      }
      if (addSign) {
         sbuf.append ('+');
      }
      else if (addBlank) {
         sbuf.append (' ');
      }
      if (leftAdjust) {
         sbuf.append ('-');
      }
      else if (zeropad) {
         sbuf.append ('0');
      }
      if (width != -1) {
         sbuf.append (width);
      }
      if (precision != -1) {
         if (precision != 6 || (type != 'e' && type != 'f')) {
            sbuf.append ('.');
            sbuf.append (precision);
         }
      }
      sbuf.append ((char)type);
      sbuf.append (suffix);
      return sbuf.toString();
   }

   /** 
    * Returns true if this number format corresponds to '%g'.
    * 
    * @return true if format corresponds to '%g'.
    */
   public boolean isFullPrecisionDouble() {
      return ((type == 'g' || type == 'G') && precision == -1);
   }

   /** 
    * Returns true if this number format corresponds to one
    * of the floating point formats ('eEfgaA')
    * 
    * @return true if format corresponds to floating point
    */
   public boolean isFloatingPoint() {
      return (type == 'g' || type == 'G' ||
              type == 'e' || type == 'E' ||
              type == 'a' || type == 'A');
   }

   String toLongString() {
      return "prefix=" + prefix + "\n" + "suffix=" + suffix + "\n" + "width="
      + width + "\n" + "precision=" + precision + "\n" + "type=" + (char)type
      + "\n" + "addSign=" + addSign + "\n" + "addBlank=" + addBlank + "\n"
      + "leftAdjust=" + leftAdjust + "\n" + "alternate=" + alternate + "\n"
      + "zeropad=" + zeropad + "\n";
   }

   /**
    * Sets this formatter to have the same settings as the formatter supplied by
    * the argument.
    * 
    * @param fmt
    * formatter to be copied
    */
   public void set (NumberFormat fmt) {
      prefix = new String (fmt.prefix);
      suffix = new String (fmt.suffix);
      width = fmt.width;
      type = fmt.type;
      addSign = fmt.addSign;
      addBlank = fmt.addBlank;
      leftAdjust = fmt.leftAdjust;
      alternate = fmt.alternate;
      zeropad = fmt.zeropad;
      setPrecision (fmt.precision);
   }

   /**
    * Returns true if this NumberFormat is identical in function (i.e., will
    * produce the same output) as the NumberFormat supplied as an argument.
    * 
    * @param fmt
    * NumberFormat to compare with
    * @return true if this format is identical in function to fmt
    */
   public boolean equals (NumberFormat fmt) {
      if (!prefix.equals (fmt.prefix) ||
          !suffix.equals (fmt.suffix) ||
          width != fmt.width ||
          type != fmt.type ||
          addSign != fmt.addSign ||
          addBlank != fmt.addBlank ||
          leftAdjust != fmt.leftAdjust ||
          alternate != fmt.alternate ||
          zeropad != fmt.zeropad ||
          precision != fmt.precision) {
         return false;
      }
      else {
         return true;
      }
   }

   public static String formatHex (int num) {
      NumberFormat fmt = new NumberFormat(hex);
      return fmt.format (num);
   }
}
