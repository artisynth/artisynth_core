/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.crypt;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

/**
 * A password encryption scheme that uses either a 128-bit,192-bit or 256-bit AES
 * algorithm.  To enable the 256-bit mode, you must install the JCE Unlimited 
 * Strength policy files:
 * "Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files"
 * &lt;http://www.oracle.com/technetwork/java/javase/downloads/index.html&gt;
 * 
 * @author "Antonio Sanchez"
 * Creation date: 24 Oct 2012
 *
 */
public class AESCryptor extends CryptorBase {

   public enum KeySize{
      AES128(16), AES192(24), AES256(32);

      private int val;
      private KeySize(int val) {
         this.val = val;
      }
      public int size() {
         return val;
      }
      public static KeySize find(int val) {
         for (KeySize key : KeySize.values()) {
            if (val == key.val) {
               return key;
            }
         }
         return null;
      }
   
   }

   private static final String ALG = "AES";
   private static final String ALG_OPTS = ""; //"/CBC/PKCS5Padding";

   public static final KeySize AES256 = KeySize.AES256;   // 256-bit encryption
   public static final KeySize AES192 = KeySize.AES192;   // 192-bit encryption
   public static final KeySize AES128 = KeySize.AES128;   // 128-bit encryption

   private KeySize keySize = AES128;

   public AESCryptor () {
      super();
      try {
         setCipher(ALG+ALG_OPTS);
      } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
         throw new RuntimeException("Failed to create cryptor", e);
      }

   }

   public AESCryptor (String hexKey) throws Exception {
      this();
      setHexKey(hexKey);
   }

   public AESCryptor (byte [] key) throws Exception {
      this();
      setKey(key);
   }

   /**
    * Sets the key size, in number of bytes
    * @param keySize either 16 or 32
    * @return false if the method did not succeed
    */
   public boolean setKeySize(KeySize keySize) {

      if (keySize == null) {
         return false;
      }

      // check we are allowed to set key size
      int max = KeySize.AES128.size();
      try{
         max = Cipher.getMaxAllowedKeyLength(ALG+ALG_OPTS);
      } catch(Exception e) {
         return false;
      }
      
      if (keySize.size() <= max) {
         this.keySize = keySize;
      } else {
         return false;
      }
      return true;
   }
   
   public KeySize getKeySize() {
      return keySize;
   }

   public void setKey(byte[] key) {

      if ( key.length != keySize.size() ) {
         // check if we can switch modes
         if (!setKeySize(KeySize.find(key.length))) {
            throw new RuntimeException("Invalid key size: " + key.length*8 + " bits");
         }
      }
      super.setKey(key);
   }


}
