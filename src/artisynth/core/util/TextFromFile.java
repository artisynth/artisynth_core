/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import java.io.*;
import java.net.URL;

/**
 * Reads text in from a file an returns it as a String.
 */
public class TextFromFile {
   private static String getText (InputStream instream, boolean returnError)
      throws IOException {
      // String text;
      Reader reader = new BufferedReader (new InputStreamReader (instream));
      StringBuffer sbuf = new StringBuffer (1024);
      char[] cbuf = new char[1024];
      int n;
      while ((n = reader.read (cbuf)) != -1) {
         sbuf.append (cbuf, 0, n);
      }
      return sbuf.toString();
   }

   // /**
   // * Reads the contents of a file and returns them as a
   // * String. If an error occurs, null is returned.
   // *
   // * @param file File from which to read the text
   // * @return text string
   // */
   // public static String getText (File file)
   // {
   // return getText (file, /*return error=*/false);
   // }

   /**
    * Reads the contents of a file and returns them as a String. If an error
    * occurs, an error message is returned instead.
    * 
    * @param file
    * File from which to read the text
    * @return text string
    */
   public static String getTextOrError (File file) {
      try {
         return getText (new FileInputStream (file), /* return error= */true);
      }
      catch (Exception e) {
         return ("Couldn't open file " + file + ": " + e.getMessage());
      }
   }

   /**
    * Reads the contents of a URL and returns them as a String. If an error
    * occurs, an error message is returned instead.
    * 
    * @param url
    * Url from which to read the text
    * @return text string
    */
   public static String getTextOrError (URL url) {
      try {
         return getText (url.openStream(), /* return error= */true);
      }
      catch (Exception e) {
         return ("Couldn't open URL " + url + ": " + e.getMessage());
      }
   }

   // /**
   // * Reads the contents of a file and returns them as a
   // * String. If an error occurs, null is returned.
   // *
   // * @param fileName Name of the file from which to read the text
   // * @return text string
   // */
   // public static String getText (String fileName)
   // {
   // return getText (new File(fileName));
   // }

   // /**
   // * Reads the contents of a file and returns them as a String. If an
   // * error occurs, an error message is returned instead.
   // *
   // * @param fileName Name of the file from which to read the text
   // * @return text string
   // */
   // public static String getTextOrError (String fileName)
   // {
   // return getTextOrError (new File(fileName));
   // }

}
