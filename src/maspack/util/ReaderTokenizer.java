/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.*;

/**
 * A tokenizer class that implements the same functionality as
 * java.io.StreamTokenizer, but with enhancements which allow it to read numbers
 * with exponents, read integers formatted in hex, and save and restore
 * settings.
 * 
 * <p>
 * The tokenizer reads characters from a Reader, removes comments and
 * whitespace, and parses the remainder into tokens. One token is parsed for
 * each call to {@link #nextToken nextToken}. The following tokens may be
 * generated:
 * 
 * <dl>
 * <dt><i>numbers</i>
 * <dd>A floating point number, or decimal or hexadecimal integer. Number
 * recognition is enabled by default but can be enabled or disabled using
 * {@link #parseNumbers parseNumbers}.
 * <dt><i>word identifiers</i>
 * <dd>A continuous sequence of word characters not beginning with a digit. By
 * default, word characters consist of alphanumerics, underscores (<code>_</code>),
 * and any character whose unicode value exceeds 0xA0. Other characters can be
 * designated as word characters using {@link #wordChar wordChar} or its sibling
 * methods.
 * <dt><i>strings</i>
 * <dd>Any sequence of characters between a pair of identical quote characters.
 * By default, <code>'</code> and <code>"</code> are enabled as quote
 * characters. Other characters can be designated as quote characters using
 * {@link #quoteChar quoteChar}. Strings include the usual C-style escape
 * sequences beginning with backslash (<code>\</code>). Setting backslash to
 * a quote character is likely to produce strange result.
 * <dt><i>character token</i>
 * <dd>Any character not parsed into one of the above tokens; such characters
 * correspond to <i>ordinary</i> characters. A character can be designated as
 * ordinary using {@link #ordinaryChar ordinaryChar} or its sibling methods. The
 * method {@link #resetSyntax resetSyntax} sets all characters to be ordinary.
 * <dt><i>end of line</i>
 * <dd>An end of line. By default. recognition of this token is disabled, but
 * can be enabled or disabled using {@link #eolIsSignificant eolIsSignificant}.
 * <dt><i>end of file</i>
 * <dd>End of input from the reader.
 * </dl>
 * 
 * Three comment styles are supported:
 * <ul>
 * <li>C-style (<code>/**&#47;</code>) comments, enabled or disabled using
 * {@link #slashStarComments slashStarComments}
 * <li>C++-style (<code>//</code>) comments, enabled or disabled using
 * {@link #slashSlashComments slashSlashComments}
 * <li>Character comments, using specific comment characters designated by
 * {@link #commentChar commentChar}.
 * </ul>
 * By default, <code>#</code> is designated as a comment character and C/C++
 * comments are disabled.
 * 
 * <p>
 * By default, whitespace consists of any character with an ascii value from
 * <code>0x00</code> to <code>0x20</code>. This includes new lines,
 * carriage returns, and spaces. Other characters can be designated as
 * whitespace using {@link #whitespaceChar whitespaceChar} and its sibling
 * routines.
 * 
 * <h3>Basic Usage</h3>
 * 
 * The basic usage paradigm for ReaderTokenizer is:
 * 
 * <ul>
 * <li>Read a token
 * <li>Interpret this token and act accordingly
 * <li>Read another token ...
 * </ul>
 * 
 * <p>
 * We first give a simple example that reads in pairs of words and numbers, such
 * as following:
 * 
 * <pre>
 *     foo 1.67
 *     dayton 678
 *     thadius 1e-4
 * </pre>
 * 
 * This can be parsed with the following code:
 * 
 * <pre>
 * HashMap&lt;String,Double&gt; wordNumberPairs = new HashMap&lt;String,Double&gt;();
 * ReaderTokenizer rtok = new ReaderTokenizer (new FileReader (&quot;foo.txt&quot;));
 * while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
 *    if (rtok.ttype != ReaderTokenizer.TT_WORD) {
 *       throw new IOException (&quot;word expected, line &quot; + rtok.lineno());
 *    }
 *    String word = rtok.sval;
 *    if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER) {
 *       throw new IOException (&quot;number expected, line &quot; + rtok.lineno());
 *    }
 *    Double number = new Double (rtok.nval);
 *    wordNumberPairs.put (word, number);
 * }
 * </pre>
 * 
 * The application uses {@link #nextToken nextToken} to continuously read tokens
 * until the end of the file. Once a token is read, its type is inspected using
 * the {@link #ttype ttype} field. If the type is inappropriate, then an
 * exception is thrown, along with the tokenizer's current line number which is
 * obtained using {@link #lineno lineno}. The numeric value for a number token
 * is stored in the field {@link #nval nval}, while the string value for a word
 * token is stored in the field {@link #sval sval}.
 * 
 * <p>
 * We now give a more complex example that expects input to consist of either
 * vectors of numbers, or file names, as indicated by the keywords
 * <code>vector</code> and <code>file</code>. Keywords should be followed
 * by an equal sign (<code>=</code>), vectors can be any length but should
 * be surrounded by square brackets (<code>[ ]</code>), and file names
 * should be enclosed in double quotes (<code>"</code>), as in the following
 * sample:
 * 
 * <pre>
 *     vector=[ 0.4 0.4 6.7 ]
 *     file=&quot;bar.txt&quot;
 *     vector = [ 1.3 0.4 6.7 1.2 ]
 * </pre>
 * 
 * This can be parsed with the following code:
 * 
 * <pre>
 * ReaderTokenizer rtok = new ReaderTokenizer (new InputStreamReader (System.in));
 * ArrayList&lt;Double&gt; vector = new ArrayList&lt;Double&gt;();
 * String fileName = null;
 * while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
 *    if (rtok.ttype != ReaderTokenizer.TT_WORD) {
 *       throw new IOException (&quot;keyword expected, line &quot; + rtok.lineno());
 *    }
 *    String keyword = rtok.sval;
 *    if (keyword.equals (&quot;vector&quot;)) {
 *       rtok.scanToken ('=');
 *       rtok.scanToken ('[');
 *       vector.clear();
 *       while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
 *          vector.add (rtok.nval);
 *       }
 *       if (rtok.ttype != ']') {
 *          throw new IOException (&quot;']' expected, line &quot; + rtok.lineno());
 *       }
 *       // do something with vector ...
 *    }
 *    else if (keyword.equals (&quot;file&quot;)) {
 *       rtok.scanToken ('=');
 *       rtok.nextToken();
 *       if (!rtok.tokenIsQuotedString ('&quot;')) {
 *          throw new IOException (&quot;quoted string expected, line &quot;
 *          + rtok.lineno());
 *       }
 *       fileName = rtok.sval;
 *       // do something with file ...
 *    }
 *    else {
 *       throw new IOException (&quot;unrecognized keyword, line &quot; + rtok.lineno());
 *    }
 * }
 * </pre>
 * 
 * This code is similar to the first example, except that it also uses the
 * methods {@link #scanToken scanToken} and {@link #tokenIsQuotedString
 * tokenIsQuotedString}. The first is a convenience routine that reads the next
 * token and verifies that it's a specific type of ordinary character token (and
 * throws a diagnostic exception if this is not the case). This facilitates
 * compact code in cases where we know exactly what sort of input is expected.
 * Similar methods exist for other token types, such as
 * {@link #scanNumber scanNumber}, {@link #scanWord scanWord}, or
 * {@link #scanQuotedString scanQuotedString}. The second method,
 * {@link #tokenIsQuotedString tokenIsQuotedString}, verifies that the most
 * recently read token is in fact a string delimited by a specific quote
 * character. Similar methods exist to verify other token types:
 * {@link #tokenIsNumber tokenIsNumber}, {@link #tokenIsWord tokenIsWord}, or
 * {@link #tokenIsBoolean tokenIsBoolean}.
 * 
 * <h3>Token Lookahead</h3>
 * 
 * ReaderTokenizer supports one token of lookahead. That is, the application may
 * read a token, and then, if it is not recognized, return it to the input
 * stream to be read by another part of the application. Tokens are returned
 * using the method {@link #pushBack pushBack}. Push back is implemented very
 * efficiently, and so applications should not be shy about using it.
 * 
 * <p>
 * Here is an example consisting of a routine that reads in a set of numbers and
 * stops when a non-numeric token is read:
 * 
 * <pre>
 * public Double[] readNumbers (ReaderTokenizer rtok) throws IOException {
 *    ArrayList&lt;Double&gt; numbers = new ArrayList&lt;Double&gt;();
 *    while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
 *       numbers.add (rtok.nval);
 *    }
 *    rtok.pushBack();
 *    return numbers.toArray (new Double[0]);
 * }
 * </pre>
 * 
 * The non-numeric token is returned to the input stream so that it is available
 * to whatever parsing code is invoked next.
 * 
 * <h3>Reading Numbers</h3>
 * 
 * A major reason for implementing ReaderTokenizer is that
 * java.io.StreamTokenizer does not properly handle long or integer values, or
 * floating point numbers with exponents. ReaderTokenizer does handle these
 * cases. In particular:
 * 
 * <ul>
 * <li>Floating point exponents denoted by <code>e</code> or <code>E</code>
 * are correctly handled;
 * <li>Numeric values which are also integers (i.e., do not contain a decimal
 * point or exponent) are stored in the field {@link #lval lval} as well as
 * {@link #nval nval}; this permits integer values to be stored exactly with no
 * loss of precision;
 * <li>Hexadecimal integers can be specified by using the prefixes
 * <code>0x</code> or <code>0X</code>.
 * </ul>
 * 
 * <p>
 * One can use {@link #tokenIsInteger tokenIsInteger} to query whether or not a
 * numeric token corresponds to an integer value. Similarly, the convenience
 * routines {@link #scanInteger scanInteger}, {@link #scanLong scanLong}, and
 * {@link #scanShort scanShort} can be used to require that an integer value is
 * scanned and converted to either <code>int</code>, <code>long</code>, or
 * <code>short</code>. Another convenience routine,
 * {@link #scanNumbers scanNumbers}, can be used to read a sequence of numbers
 * and place them in an array.
 * 
 * <h3>Saving and Restoring State</h3>
 * 
 * A deficiency of java.io.StreamTokenizer is that it does not permit its
 * settings to be queried and set arbitrarily. This makes it impossible to
 * transparently save and restore the tokenizer state in different contexts. For
 * example, if a parsing routine requires a specific setting (say, accepting
 * end-of-line as a token), then it has no way of knowing if it should undo this
 * setting upon completion.
 * 
 * <p>
 * ReaderTokenizer allows of its state variables to be queried, so a that
 * parsing routine can set and restore state transparently:
 * 
 * <pre>
 *    void specialParsingRoutine (ReaderTokenizer rtok)
 *     {
 *       boolean eolTokenSave = rtok.getEolIsSignificant(); // save
 *       rtok.eolIsSignificant(true);
 * 
 *       ... do parsing that requires EOL tokens ...
 * 
 *       rtok.eolIsSignificant(eolTokenSave); // restore
 *     }
 * </pre>
 * 
 * <p>
 * This includes the ability to save and restore the type settings for
 * individual characters. In the following example, {@code $},
 * {@code @}, and {@code &} are set to word characters, and then
 * restored to whatever their previous settings were:
 * 
 * <pre>
 * void anotherSpecialParsingRoutine (ReaderTokenizer rtok) {
 *    int[] charSaves = rtok.getCharSettings (&quot;$@&amp;&quot;);
 *    rtok.wordChars (&quot;$@&amp;&quot;);
 *    // do parsing that requires $, @, and &amp; to be word characters
 * 
 *    rtok.setCharSettings (&quot;$@&amp;&quot;, typeSaves); // restore settings
 * }
 * </pre>
 * 
 */
public class ReaderTokenizer {
   private Reader myReader;
   private String myResourceName;
   private byte ctype[] = new byte[256];
   private int myLineNum = 1;

   private boolean myEolIsSignificantP = false;
   private boolean mySlashSlashCommentsP = false;
   private boolean mySlashStarCommentsP = false;
   private boolean myLowerCaseModeP = false;
   private boolean myParseNumbersP = true;
   private int myNumNumericExtensions = 0;

   private static final byte C_ORDINARY = 0;
   private static final byte C_WORD = 0x2;
   private static final byte C_NUMBER_START = 0x1;
   private static final byte C_COMMENT = 0x4;
   private static final byte C_QUOTE = 0x10;
   private static final byte C_WHITESPACE = 0x20;
   private static final byte C_NUMERIC_EXTENSION = 0x40;

   private static final byte C_MASK = C_NUMBER_START;

   private boolean myTokenPushedBack = false;
   private boolean myTokenIsInteger = false;
   private boolean myTokenIsHex = false;

   private String myLastCommentLine = null;

   private void growCtype (int size) {
      byte[] newCtype = new byte[size];
      for (int i = 0; i < ctype.length; i++) {
         newCtype[i] = ctype[i];
      }
      ctype = newCtype;
   }

   // private static final int NO_CHAR = Integer.MAX_VALUE;

   private char[] cbuf = new char[256];
   int cbufIdx = 0;

   private void growCharacterStorage() {
      char[] newbuf = new char[cbuf.length * 2];
      System.arraycopy (cbuf, 0, newbuf, 0, cbuf.length);
      cbuf = newbuf;
   }

   private void clearCharacterStorage() {
      cbufIdx = 0;
   }

   private void trimCharacterStorage() {
      cbufIdx--;
   }

//   private void storeCharacter (int c) {
//      if (cbufIdx == cbuf.length) {
//         growCharacterStorage();
//      }
//      cbuf[cbufIdx++] = (char)c;
//   }

   private String getStoredCharacters() {
      return new String (cbuf, 0, cbufIdx);
   }

   private int storeCharacterAndGetc (int c) throws IOException {
      if (cbufIdx == cbuf.length) {
         growCharacterStorage();
      }
      cbuf[cbufIdx++] = (char)c;
      if (ungetIdx > 0) {
         c = ungetBuf[--ungetIdx];
      }
      else {
         c = myReader.read();
      }
      if (c == '\n') {
         myLineNum++;
      }
      return c;
   }

   /**
    * A constant indicating the end of the input.
    */
   public static final int TT_EOF = StreamTokenizer.TT_EOF;

   /**
    * A constant indicating the end of a line.
    */
   public static final int TT_EOL = StreamTokenizer.TT_EOL;

   /**
    * A constant indicating that the current token is a number.
    */
   public static final int TT_NUMBER = StreamTokenizer.TT_NUMBER;

   /**
    * A constant indicating that the current token is a word.
    */
   public static final int TT_WORD = StreamTokenizer.TT_WORD;

   /**
    * A constant indicating that the current token has no value
    */
   public static final int TT_NOTHING = -10;

   /**
    * Contains the type of the token read after a call to
    * {@link #nextToken nextToken}.
    */
   public int ttype;

   /**
    * If the current token is a number, then this field contains the value of
    * that number.
    */
   public double nval;

   /**
    * If the current token is an integer as well as a number, then this field
    * contains the long value of that integer.
    */
   public long lval;

   /**
    * If the current token is a word or string, then this field contains the
    * value of that word or string.
    */
   public String sval;

   /**
    * Creates a new ReaderTokenizer from the specified Reader.
    * 
    * @param reader
    * Reader that provides the input stream
    */
   public ReaderTokenizer (Reader reader) {
      for (int ch = '0'; ch <= '9'; ch++) {
         ctype[ch] |= C_NUMBER_START;
      }
      ctype['-'] |= C_NUMBER_START;
      ctype['.'] |= C_NUMBER_START;
      ctype['i'] |= C_NUMBER_START;
      ctype['I'] |= C_NUMBER_START;
      ctype['+'] |= C_NUMBER_START;
      for (int ch = 'a'; ch <= 'z'; ch++) {
         ctype[ch] |= C_NUMERIC_EXTENSION;
      }
      for (int ch = 'A'; ch <= 'Z'; ch++) {
         ctype[ch] |= C_NUMERIC_EXTENSION;
      }
      parseNumbers (true);
      wordChars ('a', 'z');
      wordChars ('A', 'Z');
      wordChars ('0', '9');
      wordChar ('_');
      wordChars ('\u00A0', '\u00FF');
      commentChar ('#');
      whitespaceChars ('\u0000', '\u0020');
      quoteChar ('\'');
      quoteChar ('"');
      myReader = reader;
   }

   /**
    * Returns the current line number.
    * 
    * @return current line number
    */
   public int lineno() {
      return myLineNum;
   }

   /**
    * Sets the current line number.
    * 
    * @param num
    * new line number
    */
   public void setLineno (int num) {
      myLineNum = num;
   }

   /**
    * Specifies whether or not end-of-line should be treated as a token. If
    * treated as a token, then end-of-line is recognized as a token indicated by
    * the type {@link #TT_EOL TT_EOL}.
    * 
    * @param enable
    * if true, then end-of-line is treated as a token
    * @see #getEolIsSignificant
    */
   public void eolIsSignificant (boolean enable) {
      myEolIsSignificantP = enable;
   }

   /**
    * Returns true if end-of-line is treated as a token by this tokenizer.
    * 
    * @return true if end-of-line is treated as a token
    * @see #eolIsSignificant
    */
   public boolean getEolIsSignificant() {
      return myEolIsSignificantP;
   }

   /**
    * Enables or disables lower-case mode. In lower-case mode, the values of
    * word tokens that appear in the {@link #sval sval} field are converted to
    * lower case.
    * 
    * @param enable
    * if true, enables lower case mode
    */
   public void lowerCaseMode (boolean enable) {
      myLowerCaseModeP = enable;
   }

   /**
    * Returns true if lower-case mode is enabled for this tokenizer.
    * 
    * @return true if lower-case mode is enabled
    */
   public boolean getLowerCaseMode() {
      return myLowerCaseModeP;
   }

   /**
    * Pushes the current token back into the input, so that it may be read again
    * using {@link #nextToken nextToken}. One token of push-back is supported.
    */
   public void pushBack() {
      if (ttype != TT_NOTHING) {
         myTokenPushedBack = true;
      }
   }

   /**
    * Gets the setting associated with a character. The setting describes the
    * type of a character ( {@link #ordinaryChar ordinary},
    * {@link #wordChar word}, {@link #commentChar comment},
    * {@link #quoteChar quote}, or {@link #whitespaceChar whitespace}), and
    * whether or not the character is a
    * {@link #numericExtensionChar numeric extension}. However, the information
    * is opaque to the user, and is intended mainly to allow the saving and
    * restoring of character settings within a parsing procedure.
    * 
    * @param ch
    * character for which setting is required
    * @return setting associated the character
    * @see #setCharSetting
    * @see #getCharSettings(String)
    * @see #getCharSettings(int,int)
    */
   public int getCharSetting (int ch) {
      if (ch >= ctype.length) {
         return C_WORD;
      }
      return (ctype[ch] & ~C_MASK);
   }

   /**
    * Gets the settings associated with a set of characters specified by a
    * string and returns them in an array. For more information on character
    * settings, see {@link #getCharSetting getCharSetting}.
    * 
    * @param str
    * characters for which setting are required
    * @return settings associated with each character
    * @see #setCharSettings(String,int[])
    * @see #getCharSetting
    * @see #getCharSettings(int,int)
    */
   public int[] getCharSettings (String str) {
      int[] settings = new int[str.length()];
      for (int i = 0; i < settings.length; i++) {
         settings[i] = getCharSetting (str.charAt(i));
      }
      return settings;
   }

   /**
    * Gets the settings associated with a set of characters specified by the
    * range {@code low <= ch <= high}. For more
    * information on character settings, see {@link #getCharSetting
    * getCharSetting}.
    * 
    * @param low
    * lowest character whose setting is desired
    * @param high
    * highest character whose setting is desired
    * @return settings associated with each character
    * @see #setCharSettings(int,int,int[])
    * @see #getCharSetting
    * @see #getCharSettings(String)
    */
   public int[] getCharSettings (int low, int high) {
      int[] settings = new int[high - low + 1];
      for (int ch = low; ch <= high; ch++) {
         settings[ch] = getCharSetting (ch);
      }
      return settings;
   }

   /**
    * Assigns the settings for a character. For more information on character
    * settings, see {@link #getCharSetting getCharSetting}.
    * 
    * @param ch
    * character to be set
    * @param setting
    * setting for the character
    * @see #getCharSetting
    * @see #setCharSettings(String,int[])
    * @see #setCharSettings(int,int,int[])
    */
   public void setCharSetting (int ch, int setting) {
      if (ch >= ctype.length) {
         if (setting == C_WORD) {
            return; // this is the default
         }
         growCtype (ch);
      }
      setCtype (ch, (byte)setting);
   }

   /**
    * Assigns settings for a set of characters specified by a string. The
    * settings are provided in an accompanying array. For more information on
    * character settings, see {@link #getCharSetting getCharSetting}.
    * 
    * @param str
    * characters to be set
    * @param settings
    * setting for each character
    * @see #getCharSettings(String)
    * @see #setCharSetting(int,int)
    * @see #setCharSettings(int,int,int[])
    */
   public void setCharSettings (String str, int[] settings) {
      if (str.length() > settings.length) {
         throw new ArrayIndexOutOfBoundsException (
            "not enough settings for all the supplied characters");
      }
      for (int i = 0; i < str.length(); i++) {
         setCharSetting (str.charAt(i), settings[i]);
      }
   }

   /**
    * Assigns settings for a set of characters specified by the range
    * {@code low <= ch <= high}. The settings are provided in an
    * accompanying array. For more information on character settings, see
    * {@link #getCharSetting getCharSetting}.
    * 
    * @param low
    * lowest character to be set
    * @param high
    * highest character to be set
    * @param settings
    * setting for each character
    * @see #getCharSettings(String)
    * @see #setCharSetting(int,int)
    * @see #setCharSettings(String,int[])
    */
   public void setCharSettings (int low, int high, int[] settings) {
      if (high - low + 1 > settings.length) {
         throw new ArrayIndexOutOfBoundsException (
            "not enough settings for all the supplied characters");
      }
      for (int ch = low; ch <= high; ch++) {
         setCharSetting (ch, settings[ch]);
      }
   }

   private void setCtype (int ch, byte value) {
      ctype[ch] = (byte)((ctype[ch] & C_MASK) | value);
   }

   /**
    * Sets the specified character to be "ordinary", so that it indicates a
    * token whose type is given by the character itself.
    * 
    * <p>
    * Setting the end-of-line character to be ordinary may interfere with
    * ability of this tokenizer to count lines. Setting numeric characters to be
    * ordinary may interfere with the ability of this tokenizer to parse
    * numbers, if numeric parsing is enabled.
    * 
    * @param ch
    * character to be designated as ordinary.
    * @see #isOrdinaryChar
    * @see #ordinaryChars
    */
   public void ordinaryChar (int ch) {
      if (ch >= ctype.length) {
         growCtype (ch);
      }
      setCtype (ch, C_ORDINARY);
   }

   /**
    * Sets all characters in the range {@code low <= ch <= high} to be
    * "ordinary". See {@link #ordinaryChar(int) ordinaryChar} for more
    * information on ordinary characters.
    * 
    * @param low
    * lowest character to be designated as ordinary.
    * @param high
    * highest character to be designated as ordinary.
    * @see #isOrdinaryChar
    * @see #ordinaryChar
    */
   public void ordinaryChars (int low, int high) {
      if (high >= ctype.length) {
         growCtype (high);
      }
      for (int ch = low; ch <= high; ch++) {
         setCtype (ch, C_ORDINARY);
      }
   }

   /**
    * Sets all characters specified by a string to be "ordinary". See
    * {@link #ordinaryChar(int) ordinaryChar} for more information on ordinary
    * characters.
    * 
    * @param str
    * string giving the ordinary characters
    * @see #isOrdinaryChar
    * @see #ordinaryChar
    */
   public void ordinaryChars (String str) {
      for (int i = 0; i < str.length(); i++) {
         setCharSetting (str.charAt(i), C_ORDINARY);
      }
   }

   /**
    * Returns true if the specified character is an ordinary character.
    * 
    * @param ch
    * character to be queried
    * @return true if <code>ch</code>ch is an ordinary character
    * @see #ordinaryChar
    * @see #ordinaryChars
    */
   public final boolean isOrdinaryChar (int ch) {
      if (ch >= ctype.length) {
         return false; // default
      }
      else {
         return (ctype[ch] & C_ORDINARY) != 0;
      }
   }

   /**
    * Sets the specified character to be a "word" character, so that it can form
    * part of a work token.
    * 
    * <p>
    * Setting the end-of-line character to be word may interfere with ability of
    * this tokenizer to count lines. Digits and other characters found in
    * numbers may be specified as word characters, but if numeric parsing is
    * enabled, the formation of numbers will take precedence over the formation
    * of words.
    * 
    * @param ch
    * character to be designated as a word character
    * @see #isWordChar
    * @see #wordChars(int,int)
    * @see #wordChars(String)
    */
   public void wordChar (int ch) {
      if (ch >= ctype.length) {
         return; // no need to do anything; this is the default
      }
      setCtype (ch, C_WORD);
   }

   /**
    * Sets some specified characters to be "word" characters, so that they can
    * form part of a work token. See {@link #wordChar wordChar} for more
    * details.
    * 
    * @param chars
    * characters to be designated as word characters
    * @see #isWordChar
    * @see #wordChar
    */
   public void wordChars (String chars) {
      for (int i = 0; i < chars.length(); i++) {
         wordChar (chars.charAt(i));
      }
   }

   /**
    * Sets all characters in the range {@code low <= ch <= high} to be
    * "word" characters. See {@link #wordChar wordChar} for more information on
    * word characters.
    * 
    * @param low
    * lowest character to be designated as a word character
    * @param high
    * highest character to be designated as a word character
    * @see #isWordChar
    * @see #wordChar
    */
   public void wordChars (int low, int high) {
      for (int ch = low; ch <= high && ch < ctype.length; ch++) {
         setCtype (ch, C_WORD);
      }
   }

   /**
    * Returns true if the specified character is an word character.
    * 
    * @param ch
    * character to be queried
    * @return true if <code>ch</code>ch is an word character
    * @see #wordChar
    * @see #wordChars(int,int)
    * @see #wordChars(String)
    */
   public final boolean isWordChar (int ch) {
      if (ch >= ctype.length) {
         return true; // default
      }
      else {
         return (ctype[ch] & C_WORD) != 0;
      }
   }

//   private void countNumericExtensions() {
//      int cnt = 0;
//      for (int ch = 0; ch < ctype.length; ch++) {
//         if ((ctype[ch] & C_NUMERIC_EXTENSION) != 0) {
//            cnt++;
//         }
//      }
//      myNumNumericExtensions = cnt;
//   }

   /**
    * Sets the specified character to be a numeric extension character. Other
    * settings for the character are unaffected. Numeric extensions are
    * sequences of characters which directly follow a number, without any
    * intervening whitespace. They are generally used to provide qualifying
    * information for numeric tokens, as in <code>100004L</code>,
    * <code>10msec</code>, or <code>2.0f</code>. Any detected numeric
    * extension is placed in the {@link #sval sval} field.
    * 
    * <p>
    * Setting the end-of-line character to be word may interfere with ability of
    * this tokenizer to count lines. Digits and other characters found in
    * numbers may be specified as word characters, but if numeric parsing is
    * enabled, the formation of numbers will take precedence over the formation
    * of words.
    * 
    * @param ch
    * character to be designated for numeric extension
    * @see #isNumericExtensionChar
    * @see #numericExtensionChars(int,int)
    * @see #numericExtensionChars(String)
    */
   public void numericExtensionChar (int ch) {
      if (ch >= ctype.length) {
         growCtype (ch);
      }
      if ((ctype[ch] & C_NUMERIC_EXTENSION) == 0) {
         ctype[ch] |= C_NUMERIC_EXTENSION;
         myNumNumericExtensions++;
      }
   }

   /**
    * Sets some specified characters to be numeric extension characters. Other
    * settings for the characters are unaffected. For more information on
    * numeric extensions, see {@link #numericExtensionChar
    * numericExtensionChar}.
    * 
    * @param chars
    * characters to be designated as numeric extensions
    * @see #isNumericExtensionChar
    * @see #numericExtensionChar
    * @see #numericExtensionChars(int,int)
    */
   public void numericExtensionChars (String chars) {
      for (int i = 0; i < chars.length(); i++) {
         numericExtensionChar (chars.charAt(i));
      }
   }

   /**
    * Sets all characters in the range {@code low <= ch <= high} to be
    * numeric extension characters. Other settings for the characters are
    * unaffected. For more information on numeric extensions, see {@link
    * #numericExtensionChar numericExtensionChar}.
    * 
    * @param low
    * lowest character to be designated as a numeric extension character
    * @param high
    * highest character to be designated as a numeric extension character
    * @see #isNumericExtensionChar
    * @see #numericExtensionChar
    * @see #numericExtensionChars(String)
    */
   public void numericExtensionChars (int low, int high) {
      for (int ch = low; ch <= high && ch < ctype.length; ch++) {
         numericExtensionChar (ch);
      }
   }

   /**
    * Returns true if the specified character is a numeric extension character.
    * For more information on numeric extensions, see {@link
    * #numericExtensionChar numericExtensionChar}.
    * 
    * @param ch
    * character to be queried
    * @return true if <code>ch</code>ch is a numeric extension character
    * @see #numericExtensionChar
    * @see #numericExtensionChars(String)
    * @see #numericExtensionChars(int,int)
    */
   public final boolean isNumericExtensionChar (int ch) {
      if (ch >= ctype.length) {
         return false; // default
      }
      else {
         return (ctype[ch] & C_NUMERIC_EXTENSION) != 0;
      }
   }

   /**
    * Returns a String specifying all characters which are enabled as numeric
    * extensions. For more information on numeric extensions, see
    * {@link #numericExtensionChar numericExtensionChar}.
    * 
    * @return string giving all numeric extension characters
    * @see #isNumericExtensionChar
    * @see #numericExtensionChar
    * @see #numericExtensionChars(String)
    */
   public String getNumericExtensionChars() {
      StringBuffer buf = new StringBuffer (64);
      for (int ch = 0; ch < ctype.length; ch++) {
         if ((ctype[ch] & C_NUMERIC_EXTENSION) != 0) {
            buf.append (ch);
         }
      }
      return buf.toString();
   }

   /**
    * Clears all numeric extension characters. For more information on numeric
    * extensions, see {@link #numericExtensionChar numericExtensionChar}.
    * 
    * @see #isNumericExtensionChar
    * @see #numericExtensionChar
    * @see #numericExtensionChars(String)
    */
   public void clearNumericExtensionChars() {
      for (int ch = 0; ch < ctype.length; ch++) {
         if ((ctype[ch] & C_NUMERIC_EXTENSION) != 0) {
            ctype[ch] &= ~(C_NUMERIC_EXTENSION);
         }
      }
      myNumNumericExtensions = 0;
   }

   /**
    * Sets the specified character to be a "white space" character, so that it
    * delimits tokens and does not otherwise take part in their formation.
    * 
    * @param ch
    * character to be designated as whitespace
    * @see #isWhitespaceChar
    * @see #whitespaceChars
    */
   public void whitespaceChar (int ch) {
      if (ch >= ctype.length) {
         growCtype (ch);
      }
      setCtype (ch, C_WHITESPACE);
   }

   /**
    * Sets all characters in the range {@code low <= ch <= high} to
    * "whitespace" characters. See {@link #whitespaceChar whitespaceChar} for
    * more information on whitespace characters.
    * 
    * @param low
    * lowest character to be designated as whitespace
    * @param high
    * highest character to be designated as whitespace
    * @see #isWhitespaceChar
    * @see #whitespaceChar
    */
   public void whitespaceChars (int low, int high) {
      if (high >= ctype.length) {
         growCtype (high);
      }
      for (int ch = low; ch <= high; ch++) {
         setCtype (ch, C_WHITESPACE);
      }
   }

   /**
    * Returns true if the specified character is an whitespace character.
    * 
    * @param ch
    * character to be queried
    * @return true if <code>ch</code>ch is an whitespace character
    * @see #whitespaceChar
    * @see #whitespaceChars
    */
   public final boolean isWhitespaceChar (int ch) {
      if (ch >= ctype.length) {
         return false; // default
      }
      else {
         return (ctype[ch] & C_WHITESPACE) != 0;
      }
   }

   /**
    * Sets the specified character to be a "quote" character, so that it
    * delimits a quoted string. When a quote character is encountered, a quoted
    * string is formed consisting of all characters following the quote
    * character, up to (but excluding) the next instance of that quote
    * character, or an end-of-line, or the end of input. Usual C-style escape
    * sequences are recognized and may be used to include the quote character
    * within the string.
    * 
    * @param ch
    * character to be designated as a quote character
    * @see #isQuoteChar
    */
   public void quoteChar (int ch) {
      if (ch >= ctype.length) {
         growCtype (ch);
      }
      setCtype (ch, C_QUOTE);
   }

   /**
    * Returns true if the specified character is a quote character.
    * 
    * @param ch
    * character to be queried
    * @return true if <code>ch</code>ch is a quote character
    * @see #quoteChar
    */
   public final boolean isQuoteChar (int ch) {
      if (ch >= ctype.length) {
         return false; // default
      } else if (ch < 0) {
         return false; // default
      }
      else {
         return (ctype[ch] & C_QUOTE) != 0;
      }
   }

   /**
    * Sets all characters to be ordinary characters, so that they are treated as
    * individual tokens, as disabled number parsing.
    * 
    * @see #parseNumbers
    */
   public void resetSyntax() {
      for (int i = 0; i < ctype.length; i++) {
         setCtype (i, C_ORDINARY);
      }
      parseNumbers (false);
   }

   /**
    * Enables parsing of numbers by this tokenizer. If number parsing is
    * enabled, then the following numeric tokens are recognized:
    * 
    * <ul>
    * <li>Floating point decimal numbers, with optional exponents
    * <li>Decimal integers
    * <li>Hexadecimal integers, preceeded by <code>0x</code> or
    * <code>0X</code>
    * </ul>
    * 
    * All numbers may be preceeded by a <code>-</code> sign which negates
    * their value. When a number token is parsed, its numeric value is placed
    * into the {@link #nval nval} field and {@link #ttype ttype} is set to
    * {@link #TT_NUMBER TT_NUMBER}. If the number is also an integer, then it's
    * value is placed into the field {@link #lval lval}.
    * 
    * @param enable
    * if true, enables parsing of numbers
    * @see #getParseNumbers
    */
   public void parseNumbers (boolean enable) {
      myParseNumbersP = enable;
   }

   /**
    * Returns true if number parsing is enabled for this tokenizer.
    * 
    * @return true if number parsing is enabled
    * @see #parseNumbers
    */
   public boolean getParseNumbers() {
      return myParseNumbersP;
   }

   // /**
   // * Enables the parsing of numeric extensions.
   // *
   // * <p>Numeric extensions are disabled by default, and no extensions are
   // * parsed for <code>inf</code>.
   // *
   // * @param if true, enables parsing of numeric extensions
   // * @see #getParseNumbericExtensions
   // */
   // public void parseNumericExtensions (boolean enable)
   // {
   // myNumNumericExtensions = enable;
   // }

   // /**
   // * Returns true if the parsing of numeric extensions is enabled.
   // *
   // * @return true if numeric extension parsing is enabled
   // * @see #parseNumbericExtensions
   // */
   // public boolean getParseNumericExtensions()
   // {
   // return myNumNumericExtensions;
   // }

   /**
    * Enables the handling of C-style slash-star comments, commenting out all
    * characters, inclusing new lines, between <code>/*</code> and
    * <code>*&#47;</code>.
    * 
    * @param enable
    * if true, enables C-style comments
    * @see #getSlashStarComments
    */
   public void slashStarComments (boolean enable) {
      mySlashStarCommentsP = enable;
   }

   /**
    * Returns true if C-style slash-star comments are enabled.
    * 
    * @return true if slash-star comments are enabled
    * @see #slashStarComments
    */
   public boolean getSlashStarComments() {
      return mySlashStarCommentsP;
   }

   /**
    * Enables the handling of C++-style slash-slash comments, commenting out all
    * characters between <code>//</code> and the next line.
    * 
    * @param enable
    * if true, enables slash-slash comments
    * @see #getSlashSlashComments
    */
   public void slashSlashComments (boolean enable) {
      mySlashSlashCommentsP = enable;
   }

   /**
    * Returns true if C++-style slash-slash comments are enabled.
    * 
    * @return true if slash-slash comments are enabled
    * @see #slashSlashComments
    */
   public boolean getSlashSlashComments() {
      return mySlashSlashCommentsP;
   }

   /**
    * Sets the specified character to be a comment character. Occurance of a
    * comment character causes all other characters between it and the next line
    * to be discarded.
    * 
    * @param ch
    * character to be designated as a comment character.
    * @see #isCommentChar
    */
   public void commentChar (int ch) {
      if (ch >= ctype.length) {
         ch = ctype.length - 1;
      }
      setCtype (ch, C_COMMENT);
   }

   /**
    * Returns true if the specified character is a comment character.
    * 
    * @param ch
    * character to be queried
    * @return true if <code>ch</code>ch is a comment character
    * @see #commentChar
    */
   public final boolean isCommentChar (int ch) {
      if (ch >= ctype.length) {
         return false;
      }
      else {
         return (ctype[ch] & C_COMMENT) != 0;
      }
   }
  
   /**
    * Returns a string describing the type and value of the current token, as
    * well as the current line number.
    * 
    * @return string containing token and line information
    */
   public String toString() {
      String str;
      switch (ttype) {
         case TT_NOTHING: {
            str = "NOTHING";
            break;
         }
         case TT_EOL: {
            str = "EOL";
            break;
         }
         case TT_EOF: {
            str = "EOF";
            break;
         }
         case TT_WORD: {
            str = sval;
            break;
         }
         case TT_NUMBER: {
            if (myTokenIsHex) {
               str = "n=0x" + Long.toHexString (lval);
            }
            else if (myTokenIsInteger) {
               str = "n=" + lval;
            }
            else {
               str = "n=" + nval;
            }
            if (sval != null) {
               str += sval;
            }
            break;
         }
         default: {
            if (ttype < ctype.length && ctype[ttype] == C_QUOTE) {
               str = sval;
               break;
            }
            char[] cp = new char[3];
            cp[0] = '\'';
            cp[1] = (char)ttype;
            cp[2] = '\'';
            str = new String (cp);
            break;
         }
      }
      return "Token[" + str + "], line " + myLineNum;
   }

   private int[] ungetBuf = new int[16];
   private int ungetIdx = 0;

   protected final void ungetc (int c) {
      ungetBuf[ungetIdx++] = c;
      if (c == '\n') {
         myLineNum--;
      }
   }

   protected void ungetStr (String str) {
      for (int i = str.length() - 1; i >= 0; i--) {
         ungetc (str.charAt(i));
      }
   }

   protected final int getc() throws IOException {
      int c;
      if (ungetIdx > 0) {
         c = ungetBuf[--ungetIdx];
      }
      else {
         c = myReader.read();
      }
      if (c == '\n') {
         myLineNum++;
      }
      return c;
   }

   private boolean infinityParsed() throws IOException {
      // assumes we have already read an 'i'
      int c;
      if ((c = getc()) != 'n') {
         ungetc (c);
         return false;
      }
      if ((c = getc()) != 'f') {
         ungetc (c);
         ungetc ('n');
         return false;
      }
      if (!isWordChar ((c = getc()))) {
         ungetc (c);
         return true;
      }
      else if (c != 'i' && c != 'I') {
         ungetc (c);
         ungetStr ("nf");
         return false;
      }
      if ((c = getc()) != 'n') {
         ungetc (c);
         ungetStr ("nfi");
         return false;
      }
      if ((c = getc()) != 'i') {
         ungetc (c);
         ungetStr ("nfin");
         return false;
      }
      if ((c = getc()) != 't') {
         ungetc (c);
         ungetStr ("nfini");
         return false;
      }
      if ((c = getc()) != 'y') {
         ungetc (c);
         ungetStr ("nfinit");
         return false;
      }
      if (isWordChar ((c = getc()))) {
         ungetc (c);
         ungetStr ("nfinity");
         return false;
      }
      else {
         ungetc (c);
         return true;
      }
   }

   private int hexDigitValue (int c) {
      if (c >= '0' && c <= '9') {
         return c - '0';
      }
      else if (c >= 'a' && c <= 'f') {
         return c - 'a' + 10;
      }
      else if (c >= 'A' && c <= 'F') {
         return c - 'A' + 10;
      }
      else {
         return -1;
      }
   }

   private int readEscapeSequence() throws IOException {
      int c = getc();
      if (c >= '0' && c <= '7') {
         int firstc = c;
         c -= '0';
         int d = getc();
         if (d >= '0' && d <= '7') {
            c = (c << 3) + d - '0';
            if (firstc <= '3') {
               d = getc();
               if (d >= '0' && d <= '7') {
                  c = (c << 3) + d - '0';
               }
               else {
                  ungetc (d);
               }
            }
         }
         else {
            ungetc (d);
         }
      }
      else {
         switch (c) {
            case 'a':
               c = 0x7;
               break;
            case 'b':
               c = '\b';
               break;
            case 'f':
               c = 0xC;
               break;
            case 'n':
               c = '\n';
               break;
            case 'r':
               c = '\r';
               break;
            case 't':
               c = '\t';
               break;
            case 'v':
               c = 0xB;
               break;
         }
      }
      return c;
   }

//   private static double[] smallExpTab =
//      { 1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12,
//       1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19, 1e20, 1e21, 1e22, 1e23, 1e24,
//       1e25, 1e26, 1e27, 1e28, 1e29, 1e30, 1e31, 1e32, 1e33, 1e34, 1e35, 1e36,
//       1e37, 1e38, 1e39, 1e40, 1e41, 1e42, 1e43, 1e44, 1e45, 1e46, 1e47, 1e48,
//       1e49, 1e50, 1e51, 1e52, 1e53, 1e54, 1e55, 1e56, 1e57, 1e58, 1e59, 1e60,
//       1e61, 1e62, 1e63, 1e64, 1e65, 1e66, 1e67, 1e68, 1e69, 1e70, 1e71, 1e72,
//       1e73, 1e74, 1e75, 1e76, 1e77, 1e78, 1e79, 1e80, 1e81, 1e82, 1e83, 1e84,
//       1e85, 1e86, 1e87, 1e88, 1e89, 1e90, 1e91, 1e92, 1e93, 1e94, 1e95, 1e96,
//       1e97, 1e98, 1e99, };

   private void parseNumericExtension() throws IOException {
      int c = getc();
      if (c >= 0 && isNumericExtensionChar (c)) {
         int nb = 0;
         do {
            if (nb == cbuf.length) {
               growCharacterStorage();
            }
            cbuf[nb++] = (char)c;
            c = getc();
         }
         while (c >= 0 && isNumericExtensionChar (c));
         sval = String.copyValueOf (cbuf, 0, nb);
      }
      else {
         sval = null;
      }
      ungetc (c);
   }

   // private boolean parseNumber (int c)
   // throws IOException
   // {
   // boolean negate = false;
   // int type;

   // int signChar = 0;
   // if (c == '-')
   // { negate = true;
   // signChar = '-';
   // c = getc();
   // }
   // else if (c == '+')
   // { signChar = '+';
   // c = getc();
   // }
   // if (c == 'i' || c == 'I')
   // { if (infinityParsed())
   // { nval = (negate ?
   // Double.NEGATIVE_INFINITY :
   // Double.POSITIVE_INFINITY);
   // sval = null;
   // return true;
   // }
   // if (signChar != 0)
   // { ungetc (c);
   // c = signChar;
   // }
   // return false;
   // }
   // else
   // { long l = 0;
   // int leadDigitCnt = 0;
   // int fracDigitCnt = 0;
   // boolean dotseen = false;
   // if (c == '0')
   // { c = getc();
   // if (c == 'x' || c == 'X')
   // { int d = hexDigitValue(c = getc());
   // if (d >= 0)
   // { l = d;
   // while ((d = hexDigitValue(c = getc())) >= 0)
   // { l = l*16 + d;
   // }
   // // XXX check terminating character?
   // ungetc(c);
   // myTokenIsInteger = true;
   // myTokenIsHex = true;
   // lval = l;
   // nval = (double)lval;
   // if (myNumNumericExtensions > 0)
   // { parseNumericExtension();
   // }
   // else
   // { sval = null;
   // }
   // return true;
   // }
   // else
   // { throw new IOException (
   // "expecting hex digit, got " + this);
   // }
   // }
   // leadDigitCnt = 1;
   // }
   // while (c >= '0' && c <= '9')
   // { l = l*10 + (c - '0');
   // c = getc();
   // leadDigitCnt++;
   // }
   // if (c == '.')
   // { dotseen = true;
   // c = getc();
   // }
   // while (c >= '0' && c <= '9')
   // { l = l*10 + (c - '0');
   // c = getc();
   // fracDigitCnt++;
   // }
   // if (fracDigitCnt == 0 && leadDigitCnt == 0)
   // { // then no number; push the characters back
   // ungetc (c);
   // if (dotseen)
   // { ungetc ('.');
   // }
   // if (signChar != 0)
   // { ungetc (signChar);
   // }
   // c = getc();
   // type = (c < ctype.length ? ctype[c] : C_WORD);
   // return false;
   // }
   // else
   // { int totalExp = -fracDigitCnt;
   // boolean hasExponent = false;
   // if (c == 'e' || c == 'E')
   // { int expSignChar = 0;
   // int echar = c;
   // int exp = 0;
   // c = getc();
   // if (c == '+' || c == '-')
   // { expSignChar = c;
   // c = getc();
   // }
   // if (c >= '0' && c <= '9')
   // { hasExponent = true;
   // while (c >= '0' && c <= '9')
   // { exp = exp*10 + c - '0';
   // c = getc();
   // }
   // }
   // ungetc (c);
   // if (!hasExponent)
   // { if (expSignChar != 0)
   // { ungetc (expSignChar);
   // }
   // ungetc (echar);
   // }
   // else
   // { totalExp += (expSignChar == '-' ? -exp : exp);
   // }
   // }
   // else
   // { ungetc(c);
   // }
   // if (!dotseen && !hasExponent)
   // { // then this number is an integer
   // lval = negate ? -l : l;
   // nval = (double)lval;
   // myTokenIsInteger = true;
   // if (myNumNumericExtensions > 0)
   // { parseNumericExtension();
   // }
   // else
   // { sval = null;
   // }
   // return true;
   // }
   // double v = (double)l;
   // while (totalExp >= 100)
   // { v *= 1e100;
   // totalExp -= 100;
   // }
   // while (totalExp <= -100)
   // { v /= 1e100;
   // totalExp += 100;
   // }
   // if (totalExp < 0)
   // { v /= smallExpTab[-totalExp];
   // }
   // else if (totalExp > 0)
   // { v *= smallExpTab[totalExp];
   // }
   // nval = negate ? -v : v;
   // if (myNumNumericExtensions > 0)
   // { parseNumericExtension();
   // }
   // else
   // { sval = null;
   // }
   // return true;
   // }
   // }
   // }

   private boolean parseNumber (int c) throws IOException {
      boolean negate = false;

      int signChar = 0;
      if (c == '-') {
         negate = true;
         signChar = '-';
         c = getc();
      }
      else if (c == '+') {
         signChar = '+';
         c = getc();
      }
      if (c == 'i' || c == 'I') {
         int startc = c;
         if (infinityParsed()) {
            nval =
               (negate ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
            sval = null;
            return true;
         }
         if (signChar != 0) {
            ungetc (startc);
            c = signChar;
         }
         return false;
      }
      else {
         long l = 0;
         int leadDigitCnt = 0;
         int fracDigitCnt = 0;
         boolean dotseen = false;

         clearCharacterStorage();

         if (c == '0') {
            c = storeCharacterAndGetc (c);
            if (c == 'x' || c == 'X') {
               int d = hexDigitValue (c = getc());
               if (d >= 0) {
                  l = d;
                  while ((d = hexDigitValue (c = getc())) >= 0) {
                     l = l * 16 + d;
                  }
                  // XXX check terminating character?
                  ungetc (c);
                  myTokenIsInteger = true;
                  myTokenIsHex = true;
                  lval = l;
                  nval = (double)lval;
                  if (myNumNumericExtensions > 0) {
                     parseNumericExtension();
                  }
                  else {
                     sval = null;
                  }
                  return true;
               }
               else {
                  throw new IOException ("expecting hex digit, got " + this);
               }
            }
            leadDigitCnt = 1;
         }
         while (c >= '0' && c <= '9') {
            l = l * 10 + (c - '0');
            c = storeCharacterAndGetc (c);
            leadDigitCnt++;
         }
         if (c == '.') {
            dotseen = true;
            c = storeCharacterAndGetc (c);
         }
         while (c >= '0' && c <= '9') {
            l = l * 10 + (c - '0');
            c = storeCharacterAndGetc (c);
            fracDigitCnt++;
         }
         if (fracDigitCnt == 0 && leadDigitCnt == 0) { // then no number; push
                                                         // the characters back
            ungetc (c);
            if (dotseen) {
               ungetc ('.');
            }
            if (signChar != 0) {
               ungetc (signChar);
            }
            c = getc();
            return false;
         }
         else {
            boolean hasExponent = false;
            if (c == 'e' || c == 'E') {
               int expSignChar = 0;
               int echar = c;
               int exp = 0;
               c = storeCharacterAndGetc (c);
               if (c == '+' || c == '-') {
                  expSignChar = c;
                  c = storeCharacterAndGetc (c);
               }
               if (c >= '0' && c <= '9') {
                  hasExponent = true;
                  while (c >= '0' && c <= '9') {
                     exp = exp * 10 + c - '0';
                     c = storeCharacterAndGetc (c);
                  }
               }
               ungetc (c);
               if (!hasExponent) {
                  if (expSignChar != 0) {
                     ungetc (expSignChar);
                     trimCharacterStorage();
                  }
                  ungetc (echar);
                  trimCharacterStorage();
               }
            }
            else {
               ungetc (c);
            }
            if (!dotseen && !hasExponent) { // then this number is an integer
               lval = negate ? -l : l;
               nval = (double)lval;
               myTokenIsInteger = true;
               if (myNumNumericExtensions > 0) {
                  parseNumericExtension();
               }
               else {
                  sval = null;
               }
               return true;
            }
            nval = Double.parseDouble (getStoredCharacters());
            if (negate) {
               nval = -nval;
            }
            if (myNumNumericExtensions > 0) {
               parseNumericExtension();
            }
            else {
               sval = null;
            }
            return true;
         }
      }
   }

   /**
    * Parses the next token from the input and returns its type. The token type
    * is also placed in the field {@link #ttype ttype}. If the token is
    * numeric, then the associated numeric value is placed in the field
    * {@link #nval nval}. If the token is a word or quoted string, then the
    * associated string value is placed in the field {@link #sval sval}.
    * 
    * @return type of the token read
    */
   public int nextToken() throws IOException {
      if (myTokenPushedBack) {
         myTokenPushedBack = false;
         return ttype;
      }

      myTokenIsInteger = false;
      myTokenIsHex = false;

      // skip white space and comments

      int c, type;

      c = getc();

      // skip white space and comments

      while (true) {
         if (c < 0) {
            return ttype = TT_EOF;
         }
         type = (c < ctype.length ? ctype[c] : C_WORD);

         if (c == '/' && (mySlashSlashCommentsP || mySlashStarCommentsP)) {
            int c2 = getc();
            if (c2 == '*' && mySlashStarCommentsP) {
               c2 = getc();
               int c1;
               do {
                  c1 = c2;
                  c2 = getc();
               }
               while (c2 >= 0 && (c1 != '*' || c2 != '/'));
               c = getc();
               continue;
            }
            else if (c2 == '/' && mySlashSlashCommentsP) {
               while ((c = getc()) >= 0 && c != '\n')
                  ;
               continue;
            }
            else {
               ungetc (c2);
            }
         }
         if (c == '\n' && myEolIsSignificantP) {
            return ttype = TT_EOL;
         }
         else if ((type & C_COMMENT) != 0) {
            StringBuilder buf = new StringBuilder();
            while ((c = getc()) >= 0 && c != '\n') {
               buf.append ((char)c);
            }
            myLastCommentLine = buf.toString();
            // skip EOL as well, otherwise we get EOL after EOL after EOL 
            // for a bunch of comments if EOL is significant
            c = getc();
         }
         else if ((type & C_WHITESPACE) != 0) {
            c = getc();
         }
         else {
            break;
         }
      }

      // number parsing

      if ((type & C_NUMBER_START) != 0 && myParseNumbersP) {
         if (parseNumber (c)) {
            return ttype = TT_NUMBER;
         }
      }

      // word parsing

      if ((type & C_WORD) != 0) {
         int nb = 0;
         do {
            if (nb == cbuf.length) {
               growCharacterStorage();
            }
            cbuf[nb++] = (char)c;
            c = getc();
         }
         while (c >= 0 && isWordChar (c));
         ungetc (c);
         sval = String.copyValueOf (cbuf, 0, nb);
         if (myLowerCaseModeP) {
            sval = sval.toLowerCase();
         }
         return ttype = TT_WORD;
      }

      // quoted string parsing

      else if ((type & C_QUOTE) != 0) {
         int nb = 0;
         int quotec = c;
         while ((c = getc()) != quotec && c >= 0 && c != '\n') {
            if (nb == cbuf.length) {
               growCharacterStorage();
            }
            if (c == '\\') {
               c = readEscapeSequence();
            }
            cbuf[nb++] = (char)c;
         }
         sval = String.copyValueOf (cbuf, 0, nb);
         if (c != quotec) {
            ungetc (c);
         }
         return ttype = quotec;
      }
      else {
         return ttype = c;
      }
   }

   /**
    * Sets the Reader which supplies the input for this tokenizer.
    * 
    * @param reader
    * new Reader
    */
   public void setReader (Reader reader) {
      myReader = reader;
      ungetIdx = 0;
      myLineNum = 1;
   }

   /**
    * Returns the Reader which supplies the input for this tokenizer.
    * 
    * @return this tokenizer's reader
    */
   public Reader getReader() {
      return myReader;
   }
   
   /**
    * Returns the name of the resource (e.g., File or URL) associated
    * with this ReaderTokenizer. This can be then be used to provide
    * diagnostic information when an input error occurs. It is up
    * to the application to set the resource name in advance using {@link
    * #setResourceName}. If a resource name has not been set, this
    * method returns <code>null</code>.
    * 
    * @return resource name, if set
    */
   public String getResourceName() {
      return myResourceName;
   }
   
   /**
    * Sets the name of the resource (e.g., File or URL) associated
    * with this ReaderTokenizer. 
    * 
    * @param name name o
    */
   public void setResourceName (String name) {
      myResourceName = name;
   }
   
   /** 
    * Close the underlying reader for this tokenizer.
    */
   public void close() {
      if (myReader != null) {
         try {
            myReader.close();
         }
         catch (Exception e) {
            // ignore
         }
      }
   }

   /**
    * Returns a string identifying the current token.
    * 
    * @return token name string
    */
   public String tokenName() {
      switch (ttype) {
         case TT_NOTHING: {
            return "???";
         }
         case TT_EOL: {
            return "EOL";
         }
         case TT_EOF: {
            return "EOF";
         }
         case TT_WORD: {
            return sval;
         }
         case TT_NUMBER: {
            String name;
            if (myTokenIsHex) {
               name = "0x" + Long.toHexString (lval);
            }
            else if (myTokenIsInteger) {
               name = "" + lval;
            }
            else {
               name = "" + nval;
            }
            if (sval != null) {
               name += sval;
            }
            return name;
         }
         default: {
            if (ttype < ctype.length && ctype[ttype] == C_QUOTE) {
               return "\"" + sval + "\"";
            }
            else {
               return "'" + (char)ttype + "'";
            }
         }
      }
   }

   private String tokenTypeName (int type) {
      switch (type) {
         case TT_NOTHING: {
            return "???";
         }
         case TT_EOL: {
            return "EOL";
         }
         case TT_EOF: {
            return "EOF";
         }
         case TT_WORD: {
            return "WORD";
         }
         case TT_NUMBER: {
            return "NUMBER";
         }
         default: {
            if (type < ctype.length && ctype[type] == C_QUOTE) {
               return (char)type + "<string>" + (char)type;
            }
            else {
               return "'" + (char)type + "'";
            }
         }
      }
   }

   /**
    * Reads the next token and verifies that it matches a specified character.
    * For this to be true, {@link #ttype ttype} must either equal the character
    * directly, or must equal {@link #TT_WORD TT_WORD} with {@link #sval sval}
    * containing a one-character string that matches the character.
    * 
    * @param ch
    * character to match
    * @throws IOException
    * if the character is not matched
    */
   public void scanCharacter (int ch) throws IOException {
      nextToken();
      if (ttype != ch &&
          !(ttype == TT_WORD && sval.length() == 1 &&
            sval.charAt (0) == ch)) {
         throw new IOException ("expected '" + (char)ch + "', got "
         + tokenName() + ", line " + lineno());
      }
   }

   /**
    * Reads the next token and verifies that it is of the specified type.
    * 
    * @param type
    * type of the expected token
    * @throws IOException
    * if the token is not of the expected type
    */
   public void scanToken (int type) throws IOException {
      nextToken();
      if (ttype != type) {
         throw new IOException ("Expecting token type " + tokenTypeName (type)
         + ", got " + this);
      }
   }

   /**
    * Reads the next token and checks that it is a number. If the token is a
    * number, its numeric value is returned. Otherwise, an exception is thrown.
    * 
    * @return numeric value of the next token
    * @throws IOException
    * if the token is not a number
    */
   public double scanNumber() throws IOException {
      nextToken();
      if (ttype != TT_NUMBER) {
         throw new IOException ("expected a number, got " + tokenName()
         + ", line " + lineno());
      }
      else {
         return nval;
      }
   }

   /**
    * Reads the next token and checks that it is an integer. If the token is an
    * integer, its value is returned. Otherwise, an exception is thrown. If the
    * value lies outside the allowed range for an integer, it is truncated in
    * the high-order bits.
    * 
    * @return integer value of the next token
    * @throws IOException
    * if the token is not an integer
    */
   public int scanInteger() throws IOException {
      nextToken();
      if (!myTokenIsInteger) {
         throw new IOException ("expected an integer, got " + this);
      }
      else {
         return (int)lval;
      }
   }

   /**
    * Reads the next token and checks that it is an integer. If the token is an
    * integer, its value is returned as a long. Otherwise, an exception is
    * thrown. If the value lies outside the allowed range for a long, it is
    * truncated in the high-order bits.
    * 
    * @return long integer value of the next token
    * @throws IOException
    * if the token is not an integer
    */
   public long scanLong() throws IOException {
      nextToken();
      if (!myTokenIsInteger) {
         throw new IOException ("expected an integer, got " + this);
      }
      else {
         return lval;
      }
   }

   /**
    * Reads the next token and checks that it is an integer. If the token is an
    * integer, its value is returned as a short. Otherwise, an exception is
    * thrown. If the value lies outside the allowed range for a short, it is
    * truncated in the high-order bits.
    * 
    * @return short integer value of the next token
    * @throws IOException
    * if the token is not an integer
    */
   public short scanShort() throws IOException {
      nextToken();
      if (!myTokenIsInteger) {
         throw new IOException ("expected an integer, got " + this);
      }
      else {
         return (short)lval;
      }
   }

   /**
    * Reads the next token and checks that it represents a boolean. A token
    * represents a boolean if it is a word token whose value equals (ignoring
    * case) either <code>true</code> or <code>false</code>. If the token
    * represents a boolean, its value is returned. Otherwise, an exception is
    * thrown.
    * 
    * @return boolean value of the next token
    * @throws IOException
    * if the token does not represent a boolean
    * @see #tokenIsBoolean
    */
   public boolean scanBoolean() throws IOException {
      nextToken();
      if (ttype != TT_WORD) {
         throw new IOException ("expected a boolean, got " + this);
      }
      else if (sval.equalsIgnoreCase ("true")) {
         return true;
      }
      else if (sval.equalsIgnoreCase ("false")) {
         return false;
      }
      else {
         throw new IOException ("expected a boolean, got " + this);
      }
   }

   /**
    * Reads the next token, check that it represents a specified
    * enumerated type, and return the corrsponding type.
    * 
    * @param enumType type of the expected enum
    * @return instance of enumType
    * @throws IOException
    * if the token does not represent the specified enumerated type
    */
   public <T extends Enum<T>> T scanEnum (Class<T> enumType) throws IOException {
      nextToken();
      if (ttype != TT_WORD) {
         throw new IOException ("expected a word, got " + this);
      }
      try {
         return Enum.valueOf (enumType, sval);
      }
      catch (Exception e) {
         throw new IOException (
            "expected an enum type of "+enumType+", got " + this);
      }
   }

   /**
    * Reads the next token and checks that it is a quoted string delimited by
    * the specified quote character.
    * 
    * @param quoteChar
    * quote character that delimits the string
    * @return value of the quoted string
    * @throws IOException
    * if the token does not represent a quoted string delimited by the specified
    * character
    * @see #tokenIsQuotedString
    */
   public String scanQuotedString (char quoteChar) throws IOException {
      nextToken();
      if (tokenIsWord() && sval.equals ("null")) {
         return null;
      }
      else if (!tokenIsQuotedString (quoteChar)) {
         throw new IOException ("expected a string delimited by '" + quoteChar
         + "', got " + this);
      }
      return sval;
   }

   /**
    * Reads the next token and checks that it is either a word or a quoted
    * string delimited by the specified quote character.
    * 
    * @param quoteChar
    * quote character that delimits strings
    * @return value of the word or quoted string
    * @throws IOException
    * if the token does not represent a word or quoted string delimited by the
    * specified character
    * @see #tokenIsQuotedString
    */
   public String scanWordOrQuotedString (char quoteChar) throws IOException {
      nextToken();
      if (ttype != TT_WORD && !tokenIsQuotedString (quoteChar)) {
         throw new IOException ("expected a word or a string delimited by '"
         + quoteChar + "', got " + this);
      }
      return sval;
   }

   /**
    * Reads the next token and checks that it is a word.
    * 
    * @return value of the word
    * @throws IOException
    * if the token does not represent a word
    */
   public String scanWord() throws IOException {
      nextToken();
      if (ttype != TT_WORD) {
         throw new IOException ("expected a word, got " + this);
      }
      return sval;
   }

   /**
    * Reads the next token and checks that it is a specific word.
    * 
    * @param word
    * expected value of the word to be scanned
    * @return value of the word
    * @throws IOException
    * if the token is not a word with the specified value
    */
   public String scanWord (String word) throws IOException {
      nextToken();
      if (ttype != TT_WORD || !sval.equals (word)) {
         throw new IOException ("expected word '" + word + "', got " + this);
      }
      return sval;
   }

   /**
    * Reads a series of numeric tokens and returns their values. Reading halts
    * when either a non-numeric token is encountered, or <code>max</code>
    * numbers have been read. Note that this token will also be numeric if the
    * input contains more than <i>max</i> consecutive numeric tokens.
    * 
    * @param vals
    * used to return numeric values
    * @param max
    * maximum number of numeric tokens to read
    * @return number of numeric tokens actually read
    */
   public int scanNumbers (double[] vals, int max) throws IOException {
      for (int i = 0; i < max; i++) {
         nextToken();
         if (ttype == TT_NUMBER) {
            vals[i] = nval;
         }
         else {
            return i;
         }
      }
      return max;
   }
   
   /**
    * Reads a series of numeric tokens and returns their values. Reading halts
    * when either a non-integer token is encountered, or <code>max</code>
    * numbers have been read. Note that this token will also be numeric if the
    * input contains more than <i>max</i> consecutive integer tokens.
    * 
    * @param vals
    * used to return integer values
    * @param max
    * maximum number of integer tokens to read
    * @return number of integer tokens actually read
    */
   public int scanIntegers (int[] vals, int max) throws IOException {
      for (int i = 0; i < max; i++) {
         nextToken();
         if (myTokenIsInteger) {
            vals[i] = (int)nval;
         } else {
            return i;
         }
      }
      return max;
   }

   /**
    * Returns true if the current token is a number. This is a convenience
    * routine for checking that {@link #ttype ttype} equals
    * {@link #TT_NUMBER TT_NUMBER}.
    * 
    * @return true if the current token is a number.
    */
   public boolean tokenIsNumber() {
      return ttype == TT_NUMBER;
   }
   
   /**
    * Returns true if the current token marks the end of the file
    * @return true if current token marks end of the file
    */
   public boolean isEOF() {
      return ttype == TT_EOF;
   }

   /**
    * Returns true if the current token is an integer. (This will also imply
    * that the token is a number.)
    * 
    * @return true if the current token is an integer.
    * @see #scanInteger
    */
   public boolean tokenIsInteger() {
      return myTokenIsInteger;
   }
   
   public boolean tokenIsHexInteger() {
      return  tokenIsInteger() && myTokenIsHex;
   }

   /**
    * Returns true if the current token is a word. This is a convenience routine
    * for checking that {@link #ttype ttype} equals {@link #TT_WORD TT_WORD}.
    * 
    * @return true if the current token is a word.
    */
   public boolean tokenIsWord() {
      return ttype == TT_WORD;
   }

   /**
    * Returns true if the current token is a word with a specified value.  This
    * is a convenience routine for checking that {@link #ttype ttype} equals
    * {@link #TT_WORD TT_WORD} and that {@link #sval sval} equals the
    * specified word.
    *
    * @param value required word value 
    * @return true if the current token matches the specified value.
    */
   public boolean tokenIsWord (String value) {
      return ttype == TT_WORD && sval.equals(value);
   }

   /**
    * Returns true if the current token represents a boolean. A token represents
    * a boolean if it is a word token equal (ignoring case) to either
    * <code>true</code> or <code>false</code>.
    * 
    * @return true if the current token is a boolean.
    * @see #scanBoolean
    */
   public boolean tokenIsBoolean() {
      return (ttype == TT_WORD && sval.equalsIgnoreCase ("true") ||
              sval.equalsIgnoreCase ("false"));
   }

   /**
    * Returns true if the current token is a quoted string delimited by the
    * specified quote character.
    * 
    * @param quoteChar
    * quote character used to delimit the string
    * @return true if the current token is a quoted string.
    */
   public boolean tokenIsQuotedString (char quoteChar) {
      if (ttype >= 0) {
         return isQuoteChar (ttype) && ttype == quoteChar;
      }
      else {
         return false;
      }
   }

   /**
    * Returns true if the current token is either a word or a quoted string
    * delimited by the specified quote character.
    * 
    * @param quoteChar
    * quote character used to delimit the string
    * @return true if the current token is a word or a quoted string.
    */
   public boolean tokenIsWordOrQuotedString (char quoteChar) {
      return (ttype == TT_WORD || isQuoteChar (ttype) && ttype == quoteChar);
   }

   /** 
    * Returns the last comment line (excluding the trailing newline) that was
    * read by this tokenizer, or null if no comments have been read yet.
    * 
    * @return last read comment line
    */
   public String lastCommentLine () {
      return myLastCommentLine;      
   }
}
