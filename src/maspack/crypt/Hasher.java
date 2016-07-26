/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.crypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for creating file hashes
 * @author antonio
 *
 */
public class Hasher {

   public static final String SHA1 = "SHA1";
   public static final String MD5 = "MD5";
   
   public static byte[] hash(File file, String alg) throws IOException, NoSuchAlgorithmException  {

      InputStream fis = new FileInputStream(file);

      // read in chunks
      byte[] buffer = new byte[1024];
      MessageDigest md = null;
      md = MessageDigest.getInstance(alg);

      int numRead;
      do {
         numRead = fis.read(buffer);
         if (numRead > 0) {
            md.update(buffer, 0, numRead);
         }
      } while (numRead != -1);
      fis.close();
      return md.digest();
   }

   public static String md5(File file) throws IOException {
      
      byte[] md5sum = null;
      try {
         md5sum = hash(file, MD5);
      } catch (NoSuchAlgorithmException e) {}
     
      return Base16.encode(md5sum);
   }

   public static String md5(String fileName) throws IOException {
      return md5(new File(fileName));
   }
   
   public static String sha1(File file) throws IOException {
      
      byte[] sha1sum = null;
      try {
         sha1sum = hash(file, SHA1);
      } catch (NoSuchAlgorithmException e) {}
     
      return Base16.encode(sha1sum);
      
   }

   public static String sha1(String fileName) throws IOException {
      return sha1(new File(fileName));
   }   
   
}
