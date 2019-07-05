/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class IndentingPrintWriter extends PrintWriter {
   private int myIndent = 0;
   // default tabe size. If 0, then tabs are not used.
   private int myTabSize = 0; // don't use tabs - they aren't portable
   private char[] myIndentBuf = new char[80];
   private int myIndentLen = 0;
   // last character written was a newline, so next write must be
   // padded with indentation:
   private boolean myLastCharWasNewline = false;

   private String myLineSeparator;
   private char myLineSepChar;

   private void init() {
      myLineSeparator =
         (String)java.security.AccessController.doPrivileged (
            new GetPropertyAction ("line.separator"));
      myLineSepChar = myLineSeparator.charAt (myLineSeparator.length() - 1);
      if (myLineSepChar != '\n') {
         System.err.println ("Warning: line sep character not '\n'");
      }
   }

   /**
    * Create a new IndentingPrintWriter, without automatic line flushing.
    * 
    * @param out
    * A character-output stream
    */
   public IndentingPrintWriter (Writer out) {
      super (out);
      init();
   }

   /**
    * Create a new IndentingPrintWriter.
    * 
    * @param out
    * A character-output stream
    * @param autoFlush
    * A boolean; if true, the <tt>println</tt>, <tt>printf</tt>, or
    * <tt>format</tt> methods will flush the output buffer
    */
   public IndentingPrintWriter (Writer out, boolean autoFlush) {
      super (out, autoFlush);
      init();
   }

   /**
    * Create a new IndentingPrintWriter, without automatic line flushing, from
    * an existing OutputStream. This convenience constructor creates the
    * necessary intermediate OutputStreamWriter, which will convert characters
    * into bytes using the default character encoding.
    * 
    * @param out
    * An output stream
    * 
    * @see java.io.OutputStreamWriter#OutputStreamWriter(java.io.OutputStream)
    */
   public IndentingPrintWriter (OutputStream out) {
      super (out);
      init();
   }

   /**
    * Create a new IndentingPrintWriter from an existing OutputStream. This
    * convenience constructor creates the necessary intermediate
    * OutputStreamWriter, which will convert characters into bytes using the
    * default character encoding.
    * 
    * @param out
    * An output stream
    * @param autoFlush
    * A boolean; if true, the <tt>println</tt>, <tt>printf</tt>, or
    * <tt>format</tt> methods will flush the output buffer
    * 
    * @see java.io.OutputStreamWriter#OutputStreamWriter(java.io.OutputStream)
    */
   public IndentingPrintWriter (OutputStream out, boolean autoFlush) {
      super (out, autoFlush);
      init();
   }

   /**
    * Creates a new IndentingPrintWriter, without automatic line flushing, with
    * the specified file name. This convenience constructor creates the
    * necessary intermediate {@link java.io.OutputStreamWriter
    * OutputStreamWriter}, which will encode characters using the
    * {@linkplain java.nio.charset.Charset#defaultCharset default charset} for
    * this instance of the Java virtual machine.
    * 
    * @param fileName
    * The name of the file to use as the destination of this writer. If the file
    * exists then it will be truncated to zero size; otherwise, a new file will
    * be created. The output will be written to the file and is buffered.
    * 
    * @throws FileNotFoundException
    * If the given string does not denote an existing, writable regular file and
    * a new regular file of that name cannot be created, or if some other error
    * occurs while opening or creating the file
    * 
    * @throws SecurityException
    * If a security manager is present and {@link SecurityManager#checkWrite
    * checkWrite(fileName)} denies write access to the file
    * 
    * @since 1.5
    */
   public IndentingPrintWriter (String fileName) throws FileNotFoundException {
      super (fileName);
      init();
   }

   /**
    * Creates a new IndentingPrintWriter, without automatic line flushing, with
    * the specified file name and charset. This convenience constructor creates
    * the necessary intermediate {@link java.io.OutputStreamWriter
    * OutputStreamWriter}, which will encode characters using the provided
    * charset.
    * 
    * @param fileName
    * The name of the file to use as the destination of this writer. If the file
    * exists then it will be truncated to zero size; otherwise, a new file will
    * be created. The output will be written to the file and is buffered.
    * 
    * @param csn
    * The name of a supported {@linkplain java.nio.charset.Charset charset}
    * 
    * @throws FileNotFoundException
    * If the given string does not denote an existing, writable regular file and
    * a new regular file of that name cannot be created, or if some other error
    * occurs while opening or creating the file
    * 
    * @throws SecurityException
    * If a security manager is present and {@link SecurityManager#checkWrite
    * checkWrite(fileName)} denies write access to the file
    * 
    * @throws UnsupportedEncodingException
    * If the named charset is not supported
    * 
    * @since 1.5
    */
   public IndentingPrintWriter (String fileName, String csn)
   throws FileNotFoundException, UnsupportedEncodingException {
      super (fileName, csn);
      init();
   }

   /**
    * Creates a new IndentingPrintWriter, without automatic line flushing, with
    * the specified file. This convenience constructor creates the necessary
    * intermediate {@link java.io.OutputStreamWriter OutputStreamWriter}, which
    * will encode characters using the
    * {@linkplain java.nio.charset.Charset#defaultCharset default charset} for
    * this instance of the Java virtual machine.
    * 
    * @param file
    * The file to use as the destination of this writer. If the file exists then
    * it will be truncated to zero size; otherwise, a new file will be created.
    * The output will be written to the file and is buffered.
    * 
    * @throws FileNotFoundException
    * If the given file object does not denote an existing, writable regular
    * file and a new regular file of that name cannot be created, or if some
    * other error occurs while opening or creating the file
    * 
    * @throws SecurityException
    * If a security manager is present and {@link SecurityManager#checkWrite
    * checkWrite(file.getPath())} denies write access to the file
    * 
    * @since 1.5
    */
   public IndentingPrintWriter (File file) throws FileNotFoundException {
      super (file);
      init();
   }

   /**
    * Creates a new IndentingPrintWriter, without automatic line flushing, with
    * the specified file and charset. This convenience constructor creates the
    * necessary intermediate {@link java.io.OutputStreamWriter
    * OutputStreamWriter}, which will encode characters using the provided
    * charset.
    * 
    * @param file
    * The file to use as the destination of this writer. If the file exists then
    * it will be truncated to zero size; otherwise, a new file will be created.
    * The output will be written to the file and is buffered.
    * 
    * @param csn
    * The name of a supported {@linkplain java.nio.charset.Charset charset}
    * 
    * @throws FileNotFoundException
    * If the given file object does not denote an existing, writable regular
    * file and a new regular file of that name cannot be created, or if some
    * other error occurs while opening or creating the file
    * 
    * @throws SecurityException
    * If a security manager is present and {@link SecurityManager#checkWrite
    * checkWrite(file.getPath())} denies write access to the file
    * 
    * @throws UnsupportedEncodingException
    * If the named charset is not supported
    * 
    * @since 1.5
    */
   public IndentingPrintWriter (File file, String csn)
   throws FileNotFoundException, UnsupportedEncodingException {
      super (file, csn);
      init();
   }

   private void setIndentationString() {
      int ntabs, nspaces;
      if (myTabSize > 0) {
         ntabs = myIndent / myTabSize;
         nspaces = myIndent % myTabSize;
      }
      else {
         ntabs = 0;
         nspaces = myIndent;
      }
      if (nspaces + ntabs > myIndentBuf.length) {
         myIndentBuf = new char[2 * (nspaces + ntabs)];
      }
      int i = 0;
      for (int k = 0; k < ntabs; k++) {
         myIndentBuf[i++] = '\t';
      }
      for (int k = 0; k < nspaces; k++) {
         myIndentBuf[i++] = ' ';
      }
      myIndentLen = ntabs + nspaces;
   }

   public void setIndentation (int col) {
      if (col < 0) {
         col = 0;
      }
      if (col != myIndent) {
         myIndent = col;
         setIndentationString();
      }
   }

   public void addIndentation (int inc) {
      setIndentation (myIndent + inc);
   }
   public void removeIndentation (int inc) {
      setIndentation (myIndent - inc);
   }

   public int getIndentation() {
      return myIndent;
   }

   public String indentationString() {
      return new String (myIndentBuf, 0, myIndentLen);
   }

   public void setTabSize (int size) {
      if (size < 0) {
         size = 0;
      }
      myTabSize = size;
      setIndentationString();
   }

   public int getTabSize() {
      return myTabSize;
   }

   public void write (char[] buf, int off, int len) {
      int end = len + off;
      while (off < end) {
         int idx = off;
         while (idx < end && buf[idx] != myLineSepChar) {
            idx++;
         }
         if (idx < end) {
            idx++;
         }
         if (myLastCharWasNewline && buf[off] != myLineSepChar) {
            writeIndentation();
         }
         super.write (buf, off, idx - off);
         if (buf[idx - 1] == myLineSepChar) {
            myLastCharWasNewline = true;
         }
         else {
            myLastCharWasNewline = false;
         }
         off = idx;
      }
   }

   private void writeIndentation() {
      if (myIndent > 0) {
         super.write (myIndentBuf, 0, myIndentLen);
      }
   }

   public void write (int c) {
      if (c == myLineSepChar) {
         super.write (c);
         myLastCharWasNewline = true;
      }
      else {
         if (myLastCharWasNewline) {
            writeIndentation();
         }
         super.write (c);
         myLastCharWasNewline = false;
      }
   }

   public void println() {
      // writes out a newline by definition
      super.println();
      myLastCharWasNewline = true;
   }

   public void write (String str, int off, int len) {
      int end = off + len;
      while (off < end) {
         int idx = str.indexOf (myLineSepChar, off);
         if (idx != -1 && idx < end) { // new line at idx
            if (myLastCharWasNewline && str.charAt (off) != myLineSepChar) {
               writeIndentation();
            }
            super.write (str, off, idx + 1 - off);
            myLastCharWasNewline = true;
            off = idx + 1;
         }
         else { // no new line
            if (myLastCharWasNewline) {
               writeIndentation();
            }
            super.write (str, off, end - off);
            myLastCharWasNewline = false;
            off = end;
         }

      }
   }

   public static void main (String[] args) {
      IndentingPrintWriter pw = new IndentingPrintWriter (System.out);
      pw.println ("foo bar");
      pw.addIndentation (2);
      pw.println ("more foo bar");
      pw.println ("more foo bar");
      pw.addIndentation (2);
      pw.println ("xxx xxx\nxxx xxx");
      pw.setIndentation (2);
      pw.println ("blah blah");
      pw.close();
   }

   /**
    * Tests a PrintWriter to see if it is an instance of and
    * IndentingPrintWriter, and if it is, add a specified amount of indentation.
    * 
    * @param pw
    * PrintWriter to test
    * @param inc
    * indentation to add
    */
   static public void addIndentation (PrintWriter pw, int inc) {
      if (pw instanceof IndentingPrintWriter) {
         ((IndentingPrintWriter)pw).addIndentation (inc);
      }
   }

   /**
    * Tests a PrintWriter to see if it is an instance of and
    * IndentingPrintWriter, and if it is, add a specified amount of indentation.
    * 
    * @param pw
    * PrintWriter to test
    * @param inc
    * indentation to add
    */
   static public void removeIndentation (PrintWriter pw, int inc) {
      if (pw instanceof IndentingPrintWriter) {
         ((IndentingPrintWriter)pw).removeIndentation (inc);
      }
   }

   /**
    * Tests a PrintWriter to see if it is an instance of IndentingPrintWriter,
    * and if so, returns 1 if a newline was the last character written and 0
    * otherwise. If the PrintWriter is not an IndentingPrintWriter, then -1 is
    * written.
    * 
    * @param pw
    * PrintWriter to test
    * @return true if <code>pw</code> is an indenting print writer and just
    * wrote a newline.
    */
   static public int justWroteNewline (PrintWriter pw) {
      if (pw instanceof IndentingPrintWriter) {
         return ((IndentingPrintWriter)pw).myLastCharWasNewline ? 1 : 0;
      }
      else {
         return -1;
      }
   }

   /**
    * Prints a string to a PrintWriter. This string is then followed by a
    * newline character if (a) the PrintWriter is not an instance of
    * IndentingPrintWriter, or (b) the PrintWriter is an instance of
    * IndentingPrintWriter and the last previously written character was not a
    * newline. The method returns -1 if (a) applies, 1 if (b) applies,
    * and 0 otherwise.
    *
    * <p>The purpose of this method is to facilitate the cuddling of opening
    * delimiters. Suppose we want to write a block of items delimited
    * by '[' and ']'. Then to keep everything aligned, we may want
    * the opening string to be simply "[ " if there was nothing
    * written before it, as in
    * <pre>
    * [ name="joe"
    *   age=23
    * ]
    * </pre>
    * while if something was written before it (such as field name
    * information), then we will want the openning string to be "[ \n", as in
    * <pre>
    * personField=[
    *   name="joe"
    *   age=23
    * ]
    * </pre>
    */
   static public int printOpening (PrintWriter pw, String str) {
      if (pw instanceof IndentingPrintWriter) {
         if (((IndentingPrintWriter)pw).myLastCharWasNewline) {
            pw.print (str);
            return 0;
         }
         else {
            pw.println (str);
            return 1;
         }
      }
      else {
         pw.print (str);
         return -1;
      }
   }
}
