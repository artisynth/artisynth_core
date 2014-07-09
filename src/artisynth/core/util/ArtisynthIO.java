/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import maspack.util.*;
import java.io.*;

/**
 * Utility routines for creating writers and tokenizers for I/O operations.
 */
public class ArtisynthIO {
   /**
    * Creates a buffered IndentingPrintWriter for a particular file.
    * 
    * @param file
    * File on which to create the writer
    */
   public static IndentingPrintWriter newIndentingPrintWriter (File file)
      throws IOException {
      return new IndentingPrintWriter (new PrintWriter (new BufferedWriter (
         new FileWriter (file))));
   }

   /**
    * Creates a buffered IndentingPrintWriter for a particular file.
    * 
    * @param fileName
    * Name of the file on which to create the writer
    */
   public static IndentingPrintWriter newIndentingPrintWriter (String fileName)
      throws IOException {
      return new IndentingPrintWriter (new PrintWriter (new BufferedWriter (
         new FileWriter (fileName))));
   }

   /**
    * Creates a ReaderTokenizer for a specified input file. The tokenizer is set
    * to accept <code>.</code>, <code>/</code>, and <code>$</code> as
    * word characters, and the underlying Reader is buffered.
    * 
    * @param file
    * File on which to create the tokenizer
    */
   public static ReaderTokenizer newReaderTokenizer (File file)
      throws IOException {
      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (new FileReader (file)));
      rtok.wordChars ("./$");
      rtok.setResourceName (file.toString());
      return rtok;
   }

   /**
    * Creates a ReaderTokenizer for a specified input file. The tokenizer is set
    * to accept <code>.</code>, <code>/</code>, and <code>$</code> as
    * word characters, and the underlying Reader is buffered.
    * 
    * @param fileName
    * Name of the file on which to create the tokenizer
    */
   public static ReaderTokenizer newReaderTokenizer (String fileName)
      throws IOException {
      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (new FileReader (fileName)));
      rtok.wordChars ("./$");
      rtok.setResourceName (fileName);
      return rtok;
   }

   /**
    * Creates a ReaderTokenizer for a specified Reader. The tokenizer is set to
    * accept <code>.</code>, <code>/</code>, and <code>$</code> as word
    * characters.
    * 
    * @param reader
    * Reader on which to attach the tokenizer
    */
   public static ReaderTokenizer newReaderTokenizer (Reader reader)
      throws IOException {
      ReaderTokenizer rtok = new ReaderTokenizer (reader);
      rtok.wordChars ("./$");
      return rtok;
   }

}
