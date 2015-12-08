/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

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
public class AESCrypter implements Crypter {

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


   private static String ALG = "AES";
   private static String ALG_OPTS = ""; //"/CBC/PKCS5Padding";

   public static final KeySize AES256 = KeySize.AES256;   // 256-bit encryption
   public static final KeySize AES192 = KeySize.AES192;   // 192-bit encryption
   public static final KeySize AES128 = KeySize.AES128;   // 128-bit encryption

   private KeySize keySize = AES128;
   SecretKeySpec keySpec = null;
   Cipher cipher = null;

   public AESCrypter () {
      
      try {
         cipher = Cipher.getInstance(ALG + ALG_OPTS);
      } catch (NoSuchAlgorithmException e) {
         e.printStackTrace();
      } catch (NoSuchPaddingException e) {
         e.printStackTrace();
      }

   }

   public AESCrypter (String hexKey) throws Exception {
      this();
      setKey(hexKey);
   }

   public AESCrypter (byte [] key) throws Exception {
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

   public void setKey(byte[] key) throws Exception {

      if ( key.length != keySize.size() ) {
         // check if we can switch modes
         if (!setKeySize(KeySize.find(key.length))) {
            throw new Exception("Invalid key size: " + key.length*8 + " bits");
         }
      }
      keySpec = new SecretKeySpec(key, ALG);

   }

   public void setKey(String hexKey) throws Exception {
      byte[] hexKeyByte = HexCoder.decode(hexKey);
      setKey(hexKeyByte);
   }

   // create an AES key from a SHA-2 hash of a passphrase
   public void setKeyFromPassphrase(String passphrase) throws Exception {

      byte[] key = generateKeyFromPassphrase(passphrase, keySize);
      setKey(key);

   }

   // create an AES key from a SHA-2 hash of a passphrase
   public static byte[] generateKeyFromPassphrase(String passphrase, KeySize keySize) {

      MessageDigest digest;
      byte[] key = null;
      
      try {
         digest = MessageDigest.getInstance("SHA-256");
         digest.reset();
         digest.update(passphrase.getBytes());

         keySize = KeySize.find(Math.min(keySize.size(), digest.getDigestLength()));
         if (keySize == null) {
            return null;
         }
         
         key = new byte[keySize.size()];
         System.arraycopy(digest.digest(), 0, key, 0, keySize.size());

      } catch (NoSuchAlgorithmException e) {
         e.printStackTrace();
      } catch (Exception e) {
      }

      return key;

   }

   // encrypt
   public String encrypt(String str) throws Exception  {
      
      if (keySpec == null) {
         throw new Exception("You must first set an encryption key");
      }
      byte[] out = encrypt(str.getBytes()); 
      return HexCoder.encode(out);
   }

   public String decrypt(String str) throws Exception {
      if (keySpec == null) {
         throw new Exception("You must first set an encryption key");
      }
      byte [] out = decrypt(HexCoder.decode(str));
      return new String(out);
   }

   public byte[] encrypt(byte[] data) throws Exception {

      cipher.init(Cipher.ENCRYPT_MODE, keySpec); // initialize
      byte[] out = new byte[cipher.getOutputSize(data.length)];
      int outLength = cipher.update(data, 0, data.length, out, 0);
      outLength += cipher.doFinal(out, outLength);

      return out;
   }

   public byte[] decrypt(byte[] data) throws Exception {

      cipher.init(Cipher.DECRYPT_MODE, keySpec);
      byte[] out = new byte[cipher.getOutputSize(data.length)];
      int outLen = cipher.update(data, 0, data.length, out, 0);
      outLen += cipher.doFinal(out, outLen);
      out = Arrays.copyOf(out, outLen);

      return out;
   }

}
